package com.ligadata.InputAdapters

import java.util.concurrent.locks.ReentrantReadWriteLock

import com.ligadata.AdaptersConfiguration._
import com.ligadata.Exceptions.KamanjaException
import com.ligadata.HeartBeat.MonitorComponentInfo
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.KamanjaBase.{DataDelimiters, EnvContext, NodeContext}
import com.ligadata.Utils.ClusterStatus
import org.json4s.jackson.Serialization

import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.{ArrayBuffer, HashMap, MultiMap}

case class BufferLeftoversArea(workerNumber: Int, leftovers: Array[Byte], relatedChunk: Int)

//case class BufferToChunk(len: Int, payload: Array[Byte], chunkNumber: Int, relatedFileHandler: SmartFileHandler, firstValidOffset: Int, isEof: Boolean, partMap: scala.collection.mutable.Map[Int,Int])
case class SmartFileMessage(msg: Array[Byte], offsetInFile: Long, relatedFileHandler: SmartFileHandler, msgNumber: Long)

case class FileStatus(status: Int, offset: Long, createDate: Long)

//case class OffsetValue (lastGoodOffset: Int, partitionOffsets: Map[Int,Int])
case class EnqueuedFileHandler(fileHandler: SmartFileHandler, offset: Long, createDate: Long, partMap: scala.collection.mutable.Map[Int, Int])

case class EnqueuedGroupHandler(fileHandlers: Array[SmartFileHandler], offset: Long, createDates: Array[Long], partMap: scala.collection.mutable.Map[Int, Int])


class SmartFileConsumerContext {
  var adapterName: String = _
  var partitionId: Int = _
  var ignoreFirstMsg: Boolean = _
  var nodeId: String = _
  var envContext: EnvContext = null
  //var fileOffsetCacheKey : String = _
  var statusUpdateCacheKey: String = _
  var statusUpdateInterval: Int = _
  var execThread: ExecContext = null
}

/**
  * Counter of buffers used by the FileProcessors... there is a limit on how much memory File Consumer can use up.
  */
object BufferCounters {
  val inMemoryBuffersCntr = new java.util.concurrent.atomic.AtomicLong()
}

/**
  * Created by Yasser on 3/13/2016.
  */
object SmartFileConsumer extends InputAdapterFactory {
  val MONITOR_FREQUENCY = 10000
  // Monitor Topic queues every 20 seconds
  val SLEEP_DURATION = 1000
  // Allow 1 sec between unsucessful fetched
  var CURRENT_BROKER: String = _
  val FETCHSIZE = 64 * 1024
  val ZOOKEEPER_CONNECTION_TIMEOUT_MS = 3000
  val MAX_TIMEOUT = 60000
  val INIT_TIMEOUT = 250
  val ADAPTER_DESCRIPTION = "Smart File Consumer"

  val FILE_STATUS_FINISHED = 0
  val FILE_STATUS_NOT_FOUND = -1
  val FILE_STATUS_CORRUPT = -2
  val FILE_STATUS_ProcessingInterrupted = -3

  val PARTITION_COUNT_KEYS = "Partition Counts"
  val PARTITION_DEPTH_KEYS = "Partition Depths"
  val EXCEPTION_SUMMARY = "Exception Summary"

  def CreateInputAdapter(inputConfig: AdapterConfiguration, execCtxtObj: ExecContextFactory, nodeContext: NodeContext): InputAdapter = new SmartFileConsumer(inputConfig, execCtxtObj, nodeContext)

}

case class SmartFileExceptionInfo(Last_Failure: String, Last_Recovery: String)

case class ArchiveFileInfo(adapterConfig: SmartFileAdapterConfiguration, srcFileDir: String, srcFileBaseName: String, componentsMap: scala.collection.immutable.Map[String, String])

class SmartFileConsumer(val inputConfig: AdapterConfiguration, val execCtxtObj: ExecContextFactory, val nodeContext: NodeContext) extends InputAdapter {

  val input = this
  //  lazy val loggerName = this.getClass.getName
  lazy val LOG = logger

  private val lock = new Object()
  private var readExecutor: ExecutorService = _

  private val adapterConfig = SmartFileAdapterConfiguration.getAdapterConfig(inputConfig)

  private val locationTargetMoveDirsMap =
    adapterConfig.monitoringConfig.detailedLocations.map(loc => loc.srcDir -> loc.targetDir).toMap


  private var isShutdown = false
  private var isQuiesced = false
  private var startTime: Long = 0
  private var msgCount = 0

  private val smartFileContextMap: collection.mutable.Map[Int, SmartFileConsumerContext] = collection.mutable.Map[Int, SmartFileConsumerContext]()
  private val locationsMap: collection.mutable.Map[String, LocationInfo] = collection.mutable.Map[String, LocationInfo]()

  private val partitionKVs = scala.collection.mutable.Map[Int, (SmartFilePartitionUniqueRecordKey, SmartFilePartitionUniqueRecordValue, SmartFilePartitionUniqueRecordValue)]()

  private var partitonCounts: collection.mutable.Map[String, Long] = collection.mutable.Map[String, Long]()
  private var partitonDepths: collection.mutable.Map[String, Long] = collection.mutable.Map[String, Long]()
  private var partitionExceptions: collection.mutable.Map[String, SmartFileExceptionInfo] = collection.mutable.Map[String, SmartFileExceptionInfo]()
  private var metrics: collection.mutable.Map[String, Any] = collection.mutable.Map[String, Any]()
  private var startHeartBeat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var lastSeen: String = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  metrics(SmartFileConsumer.PARTITION_COUNT_KEYS) = partitonCounts
  metrics(SmartFileConsumer.EXCEPTION_SUMMARY) = partitionExceptions
  metrics(SmartFileConsumer.PARTITION_DEPTH_KEYS) = partitonDepths

  val delimiters: DataDelimiters = new DataDelimiters()

  private val requestQLock = new Object
  private val processingQLock = new Object
  private val processingQChangeLock = new Object

  //******************************************************************************************************
  //***************************node sync related code**********
  val communicationBasePath = "SmartFileInputAdapter/" + adapterConfig.Name
  val smartFileCommunicationPath = if (communicationBasePath.length > 0) (communicationBasePath + "/" + "SmartFileCommunication") else ("/" + "SmartFileCommunication")
  val smartFileFromLeaderPath = smartFileCommunicationPath + "/FromLeader"
  val smartFileToLeaderPath = smartFileCommunicationPath + "/ToLeader"
  val requestFilePath = smartFileToLeaderPath + "/RequestFile"
  val fileProcessingPath = smartFileToLeaderPath + "/FileProcessing"

  val File_Processing_Status_Finished = "finished"
  val File_Processing_Status_Interrupted = "interrupted"
  val File_Processing_Status_Corrupted = "corrupted"
  val File_Processing_Status_NotFound = "notfound"

  val filesParallelismParentPath = smartFileCommunicationPath + "/FilesParallelism"
  val sendStartInfoToLeaderParentPath = smartFileToLeaderPath + "/StartInfo"

  val Status_Check_Cache_KeyParent = "Smart_File_Adapter/" + adapterConfig.Name + "/" + "Status"

  val File_Requests_Cache_Key = "Smart_File_Adapter/" + adapterConfig.Name + "/" + "FileRequests"

  //maintained by leader, stores only files being processed (as list under one key). so that if leader changes, new leader can get the processing status
  val File_Processing_Cache_Key = "Smart_File_Adapter/" + adapterConfig.Name + "/" + "FileProcessing"

  private var envContext: EnvContext = nodeContext.getEnvCtxt()
  private var clusterStatus: ClusterStatus = null
  private var archiveExecutor: ExecutorService = null
  private var archiveInfo = new scala.collection.mutable.Queue[ArchiveFileInfo]()
  private var participantExecutor: ExecutorService = null
  private var leaderExecutor: ExecutorService = null
  private var filesParallelism: Int = -1
  private var monitorController: MonitorController = null
  private var initialized = false
  private var filesParallelismCallback_initialized = false
  private var prevRegLeader = ""

  private var prevRegParticipantPartitions: List[Int] = List()

  private var _ignoreFirstMsg: Boolean = _

  val statusUpdateInterval = 2000
  //ms
  //val maxWaitingTimeForNodeStatus = 10 * 1000000000L;//10 seconds, value is in nanoseconds.
  val maxFailedCheckCounts = 3

  private var keepCheckingStatus = false

  //key is node id, value is (partition id, file name, offset, ignoreFirstMsg)
  private val allNodesStartInfo = scala.collection.mutable.Map[String, List[(Int, String, Long, Boolean)]]()

  envContext.registerNodesChangeNotification(nodeChangeCallback)

  private val _reent_lock = new ReentrantReadWriteLock(true)

  private def ReadLock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.readLock().lock()
  }

  private def ReadUnlock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.readLock().unlock()
  }

  private def WriteLock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.writeLock().lock()
  }

  private def WriteUnlock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.writeLock().unlock()
  }

  private def hasNextArchiveFileInfo: Boolean = {
    (archiveInfo.size > 0)
  }

  private def getNextArchiveFileInfo: ArchiveFileInfo = {
    var archInfo: ArchiveFileInfo = null
    ReadLock(_reent_lock)
    try {
      if (archiveInfo.size > 0)
        archInfo = archiveInfo.dequeue()
    } finally {
      ReadUnlock(_reent_lock)
    }
    archInfo
  }

  private def addArchiveFileInfo(archInfo: ArchiveFileInfo): Unit = {
    WriteLock(_reent_lock)
    try {
      archiveInfo += archInfo
    } finally {
      WriteUnlock(_reent_lock)
    }
  }

  //add the node callback
  private def initializeNode: Unit = {
    LOG.debug("Max memeory = " + Runtime.getRuntime().maxMemory())

    if (nodeContext == null)
      LOG.debug("Smart File Consumer - nodeContext = null")

    if (nodeContext.getEnvCtxt() == null)
      LOG.debug("Smart File Consumer - nodeContext.getEnvCtxt() = null")

    if (envContext == null)
      LOG.debug("Smart File Consumer - envContext = null")
    else {
      if (envContext.getClusterInfo() == null)
        LOG.debug("Smart File Consumer - envContext.getClusterInfo() = null")
    }

    if (clusterStatus == null)
      LOG.debug("Smart File Consumer - clusterStatus = null")
    else {
      LOG.debug("Smart File Consumer - clusterStatus.nodeId = " + clusterStatus.nodeId)
      LOG.debug("Smart File Consumer - clusterStatus.leaderNodeId = " + clusterStatus.leaderNodeId)
    }

    if (initialized == false) {
      if (!filesParallelismCallback_initialized) {
        val fileParallelismPath = filesParallelismParentPath + "/" + clusterStatus.nodeId
        LOG.debug("Smart File Consumer - participant {} is listening to path {}", clusterStatus.nodeId, fileParallelismPath)
        envContext.createListenerForCacheKey(fileParallelismPath, filesParallelismCallback)
        filesParallelismCallback_initialized = true
      }
    }

    if (clusterStatus.isLeader && clusterStatus.leaderNodeId.equals(clusterStatus.nodeId)) {
      val newfilesParallelism = (adapterConfig.monitoringConfig.consumersCount.toDouble / clusterStatus.participantsNodeIds.size).ceil.toInt
      if (filesParallelism != -1) {
        // BUGBUG:: FIXME:- Unregister stuff
        // It is already distributes. Need to Re-Register stuff????
      }

      //action for the leader node

      //node wasn't a leader before
      if (initialized == false || !clusterStatus.leaderNodeId.equals(prevRegLeader)) {
        LOG.debug("Smart File Consumer - Leader is running on node " + clusterStatus.nodeId)

        monitorController = new MonitorController(adapterConfig, this, newFileDetectedCallback)
        monitorController.checkConfigDirsAccessibility //throw an exception if a folder is not accissible

        allNodesStartInfo.clear()
        envContext.createListenerForCacheChildern(sendStartInfoToLeaderParentPath, collectStartInfo) // listen to start info

        leaderExecutor = Executors.newFixedThreadPool(1)
        val statusCheckerThread = new Runnable() {
          var lastStatus: scala.collection.mutable.Map[String, (Long, Int)] = null

          override def run(): Unit = {
            while (keepCheckingStatus) {
              lastStatus = checkParticipantsStatus(lastStatus)
              try {
                Thread.sleep(statusUpdateInterval)
              }
              catch {
                case ie: InterruptedException => {
                  LOG.debug("Smart File Consumer - interrupted " + ie)
                  throw ie
                }
                case e: Throwable => {
                  externalizeExceptionEvent(e)
                  LOG.debug("Smart File Consumer - unkown exception " + e)
                }
              }
            }
          }
        }
        keepCheckingStatus = true
        leaderExecutor.execute(statusCheckerThread)

        //now register listeners for new requests (other than initial ones)
        LOG.debug("Smart File Consumer - Leader is listening to children of path " + requestFilePath)
        envContext.createListenerForCacheChildern(requestFilePath, requestFileLeaderCallback) // listen to file requests
        LOG.debug("Smart File Consumer - Leader is listening to children of path " + fileProcessingPath)
        envContext.createListenerForCacheChildern(fileProcessingPath, fileProcessingLeaderCallback) // listen to file processing status
      }
      else {
        //node was already leader

      }

      prevRegLeader = clusterStatus.leaderNodeId

      //FIXME : recalculate partitions for each node and send partition ids to nodes ???
      //set parallelism
      filesParallelism = newfilesParallelism
      //envContext.setListenerCacheKey(filesParallelismPath, filesParallelism.toString)
    }

    initialized = true
  }

  //after leader collects start info, it must pass initial files to monitor and, and assign partitions to participants
  private def handleStartInfo(): Unit = {
    LOG.debug("Smart File Consumer - handleStartInfo()")
    //Thread.sleep(10000) need to keep track of Partitions we got from nodes (whether we got all the participants we sent to engine or not)
    val maximumTrials = 10
    var trialCounter = 1
    while (trialCounter <= maximumTrials && allNodesStartInfo.size < clusterStatus.participantsNodeIds.size) {
      Thread.sleep(1000)
      trialCounter += 1
    }

    LOG.debug("Smart File Consumer - allNodesStartInfo = " + allNodesStartInfo)

    //send to each node what partitions to handle (as received from engine)
    allNodesStartInfo.foreach(nodeStartInfo => {
      val path = filesParallelismParentPath + "/" + nodeStartInfo._1
      val data = nodeStartInfo._2.map(nodePartitionInfo => nodePartitionInfo._1).mkString(",")
      LOG.debug("Smart File Consumer - Leader is sending parallelism info. key is {}. value is {}", path, data)
      envContext.setListenerCacheKey(path, data)
    })

    //now since we have start info for all participants
    //collect file names and offsets
    val initialFileNames = ArrayBuffer[String]()

    val initialFilesToProcess = ArrayBuffer[(String, Int, String, Long)]()
    allNodesStartInfo.foreach(nodeStartInfo => nodeStartInfo._2.foreach(nodePartitionInfo => {
      val fileName = nodePartitionInfo._2.trim
      LOG.debug("nodePartitionInfo._2 = " + (if (nodePartitionInfo._2 == null) "actual null" else nodePartitionInfo._2))
      if (fileName.length > 0 && !fileName.equals("null")) {
        if (!initialFileNames.contains(fileName.trim)) {
          val fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, fileName)
          if (fileHandler.exists()) {
            //no need to assign deleted files or files with size less than initial offset

            initialFilesToProcess.append((nodeStartInfo._1, nodePartitionInfo._1, fileName, nodePartitionInfo._3))
            initialFileNames.append(fileName)
          }
          else {
            LOG.warn("Smart File Consumer - File ({}) does not exist", fileName)
          }

        }
      }
      //(node, partitionId, file name, offset)
    }))

    // (First we need to process what ever files we get here), if we have file names
    if (initialFilesToProcess.size > 0)
      assignInitialFiles(initialFilesToProcess.toArray)
    else
      initialFilesHandled = true //nothing to handle

    //now run the monitor
    if (!isShutdown) {
      monitorController.init(initialFileNames.toList)
      monitorController.startMonitoring()
    }

  }

  def nodeChangeCallback(newClusterStatus: ClusterStatus): Unit = {
    //action for participant nodes:
    clusterStatus = newClusterStatus

    if (initialized)
      initializeNode // Immediately Changing if participents change. Do we need to wait for engine to do it?

  }

  //will be useful when leader has requests from all nodes but no more files are available. then leader should be notified when new files are detected
  private def newFileDetectedCallback(fileName: String): Unit = {
    LOG.debug("Smart File Consumer - a new file was sent to leader ({}).", fileName)
    //    logger.error("==============> HaithamLog => inside newFileDetectedCallback : before assignFileProcessingIfPossible ")
    assignFileProcessingIfPossible()
  }

  //leader constantly checks processing participants to make sure they are still working
  private def checkParticipantsStatus(previousStatusMap: scala.collection.mutable.Map[String, (Long, Int)]): scala.collection.mutable.Map[String, (Long, Int)] = {
    //if previousStatusMap==null means this is first run of checking, no errors

    //    logger.error("==============> HaithamLog => inside checkParticipantsStatus :")

    if (isShutdown)
      return previousStatusMap

    LOG.debug("Smart File Consumer - checking participants status")

    if (previousStatusMap == null)
      LOG.debug("Smart File Consumer - previousStatusMap is " + null)
    else
      LOG.debug("Smart File Consumer - previousStatusMap is {}", previousStatusMap)

    val currentStatusMap = scala.collection.mutable.Map[String, (Long, Int)]()

    processingQChangeLock.synchronized {
      val processingQueue = getFileProcessingQueue

      val queuesToRemove = ArrayBuffer[(String, String, String, String, String)]()
      processingQueue.foreach(processStr => {
        val processStrTokens = processStr.split(":")
        val pathTokens = processStrTokens(0).split("/")
        val fileInProcess = processStrTokens(1)
        val nodeId = pathTokens(0)
        val partitionId = pathTokens(1)

        val cacheKey = Status_Check_Cache_KeyParent + "/" + nodeId + "/" + partitionId
        val statusData = envContext.getConfigFromClusterCache(cacheKey) // filename~offset~timestamp
        val statusDataStr: String = if (statusData == null) null else new String(statusData)

        var failedCheckCount = 0
        var previousTimestamp = 0L
        var fileInStatus: String = null

        if (previousStatusMap != null && previousStatusMap.contains(fileInProcess)) {
          val previousTimestamp = previousStatusMap(fileInProcess)._1
          failedCheckCount = previousStatusMap(fileInProcess)._2
        }

        if (previousStatusMap != null && statusDataStr == null || statusDataStr.trim.length == 0) {
          LOG.debug("Smart File Consumer - current participants status in cache is {}", statusDataStr)

          LOG.error("Smart File Consumer - file {} is supposed to be processed by partition {} on node {} but not found in node updated status",
            fileInProcess, partitionId, nodeId)

          //participant hasn't sent any status yet. store current time, to delete from processing queue if case is still the same after limit
          failedCheckCount += 1
          currentStatusMap.put(fileInProcess, (System.nanoTime, failedCheckCount))
        }
        else if (statusDataStr == null || statusDataStr.trim.length == 0) {
          LOG.debug("Smart File Consumer - current participants status in cache is {}", statusDataStr)

          LOG.debug("Smart File Consumer - file {} is supposed to be processed by partition {} on node {} but not found in node updated status",
            fileInProcess, partitionId, nodeId)

          failedCheckCount += 1
          currentStatusMap.put(fileInProcess, (System.nanoTime, failedCheckCount))
        }
        else {
          LOG.debug("Smart File Consumer - current participants status in cache is {}", statusDataStr)

          val statusDataTokens = statusDataStr.split("~")
          fileInStatus = statusDataTokens(0)
          val currentTimeStamp = statusDataTokens(2).toLong

          if (previousStatusMap != null && !fileInStatus.equals(fileInProcess)) {
            LOG.info("Smart File Consumer - file {} is supposed to be processed by partition {} on node {} but found this file {} in node updated status",
              fileInProcess, partitionId, nodeId, fileInStatus) //could this happen?
          }
          else {

            if (previousStatusMap != null && previousStatusMap.contains(fileInStatus)) {
              previousTimestamp = previousStatusMap(fileInStatus)._1
              failedCheckCount = previousStatusMap(fileInStatus)._2

              if (currentTimeStamp > previousTimestamp) {
                //no problems here
                failedCheckCount = 0
                currentStatusMap.put(fileInStatus, (currentTimeStamp, failedCheckCount))
              }
              else if (currentTimeStamp == previousTimestamp) {
                LOG.error("Smart File Consumer - file {} is being processed by partition {} on node {}, but status hasn't been updated for {} ms. where currentTimeStamp={} - previousTimestamp={}, failedCheckCount={}",
                  fileInStatus, partitionId, nodeId, ((currentTimeStamp - previousTimestamp) / 1000).toString, currentTimeStamp.toString, previousTimestamp.toString, failedCheckCount.toString)

                failedCheckCount += 1
                currentStatusMap.put(fileInStatus, (currentTimeStamp, failedCheckCount))
              }
            }
            else {
              failedCheckCount += 1
              currentStatusMap.put(fileInStatus, (currentTimeStamp, failedCheckCount))
            }
          }
        }

        if (failedCheckCount > maxFailedCheckCounts) {
          //if (currentTimeStamp - previousTimestamp > maxWaitingTimeForNodeStatus) {
          LOG.debug("Smart File Consumer - file processing item ({}) has faile count = {}. should be removed from processing queue", processStr, failedCheckCount.toString)
          queuesToRemove.append((processStr, fileInProcess, fileInStatus, nodeId, partitionId))
        }
      })

      queuesToRemove.foreach(removeInfo => {
        LOG.debug("Smart File Consumer - removing the following from processing queue: {}", removeInfo._1)

        removeFromProcessingQueue(removeInfo._1)
        //        logger.error("==============> HaithamLog => inside checkParticipantsStatus : before assignFileProcessingIfPossible ")
        assignFileProcessingIfPossible()

        LOG.debug("Smart File Consumer - removing the following entry from currentStatusMap: {}", removeInfo._3)
        if (removeInfo._3 != null && removeInfo._3.length > 0)
          currentStatusMap.remove(removeInfo._3)
        //TODO : should be removed from cache status ?
      })
    }
    LOG.debug("Smart File Consumer - currentStatusMap is {}", currentStatusMap)

    currentStatusMap
  }

  //value in cache has the format <node1>/<thread1>:<path to receive files>|<node2>/<thread1>:<path to receive files>
  def hasPendingFileRequestsInQueue: Boolean = {
    requestQLock.synchronized {
      val cacheData = envContext.getConfigFromClusterCache(File_Requests_Cache_Key)
      if (cacheData != null) {
        val cacheDataStr = new String(cacheData)
        if (cacheDataStr.trim.length == 0) {
          false
        }
        else {
          true
        }
      }
      else {
        LOG.debug("Smart File Consumer - file request queue from cache is null")
        false
      }
    }
  }

  //value in cache has the format <node1>/<thread1>:<path to receive files>|<node2>/<thread1>:<path to receive files>
  def getFileRequestsQueue: List[String] = {
    requestQLock.synchronized {
      val cacheData = envContext.getConfigFromClusterCache(File_Requests_Cache_Key)
      if (cacheData != null) {
        val cacheDataStr = new String(cacheData)
        if (cacheDataStr.trim.length == 0) {
          LOG.debug("Smart File Consumer - file request queue from cache is empty")
          List()
        }
        else {
          LOG.debug("Smart File Consumer - file request queue from cache is {}", cacheDataStr)
          val tokens = cacheDataStr.split("\\|")
          tokens.toList
        }
      }
      else {
        LOG.debug("Smart File Consumer - file request queue from cache is null")
        List()
      }
    }
  }

  /*def saveFileRequestsQueue(requestQueue : List[String]) : Unit = {
    requestQLock.synchronized {

      val currentRequests = getFileRequestsQueue

      val cacheData = requestQueue.mkString("|")
      LOG.debug("Smart File Consumer - saving request queue. key={}, value={}", File_Requests_Cache_Key,
        if (cacheData.length == 0) "(empty)" else cacheData)

      envContext.saveConfigInClusterCache(File_Requests_Cache_Key, cacheData.getBytes)
    }
  }*/

  def addToRequestQueue(request: String, addToHead: Boolean = false): Unit = {
    requestQLock.synchronized {

      val currentRequests = getFileRequestsQueue

      val cacheData =
        if (addToHead)
          (List(request) ::: currentRequests).mkString("|")
        else
          (currentRequests ::: List(request)).mkString("|")

      LOG.debug("Smart File Consumer - adding ({}) to request queue", request)
      LOG.debug("Smart File Consumer - saving request queue. key={}, value={}", File_Requests_Cache_Key,
        if (cacheData.length == 0) "(empty)" else cacheData)

      envContext.saveConfigInClusterCache(File_Requests_Cache_Key, cacheData.getBytes)

    }
  }

  def removeFromRequestQueue(request: String): Unit = {
    requestQLock.synchronized {

      val currentRequests = getFileRequestsQueue
      val cacheData = (currentRequests diff List(request)).mkString("|")

      LOG.debug("Smart File Consumer - removing ({}) from request queue", request)
      LOG.debug("Smart File Consumer - saving request queue. key={}, value={}", File_Requests_Cache_Key,
        if (cacheData.length == 0) "(empty)" else cacheData)

      envContext.saveConfigInClusterCache(File_Requests_Cache_Key, cacheData.getBytes)

    }
  }

  def clearRequestQueue(): Unit = {
    requestQLock.synchronized {
      val cacheData = ""
      envContext.saveConfigInClusterCache(File_Requests_Cache_Key, cacheData.getBytes)
    }
  }

  //value for file processing queue in cache has the format <node1>/<thread1>:<filename>|<node2>/<thread1>:<filename>
  def getFileProcessingQueue: List[String] = {
    processingQLock.synchronized {
      val cacheData = envContext.getConfigFromClusterCache(File_Processing_Cache_Key)

      if (cacheData != null) {
        val cacheDataStr = new String(cacheData)
        //LOG.debug("Smart File Consumer - processing queue str len = "+cacheDataStr.length)
        //LOG.debug("Smart File Consumer - processing queue characters: "+ cacheDataStr.map(c=> (c.toInt)).toArray.mkString(","))

        if (cacheDataStr.trim.length == 0) {
          LOG.debug("Smart File Consumer - file processing queue from cache is empty")
          List()
        }
        else {
          LOG.debug("Smart File Consumer - file processing queue from cache is {}", cacheDataStr)
          val tokens = cacheDataStr.split("\\|")
          tokens.toList
        }
      }
      else {
        LOG.debug("Smart File Consumer - file processing queue from cache is null")
        List()
      }
    }
  }

  /*def saveFileProcessingQueue(requestQueue : List[String]) : Unit = {
    //processingQChangeLock.synchronized {
      processingQLock.synchronized {
        val cacheData = requestQueue.mkString("|")
        LOG.debug("Smart File Consumer - saving following value to processing queue: " + cacheData)
        envContext.saveConfigInClusterCache(File_Processing_Cache_Key, cacheData.getBytes)
      }
  }*/

  //value for file processing queue in cache has the format <node1>/<thread1>:<groupOfFileNamesSeperatedBy"~~">|<node2>/<thread1>:<groupOfFileNamesSeperatedBy"~~">
  def addToProcessingQueue(processingItem: String, addToHead: Boolean = false): Unit = {
    processingQLock.synchronized {

      val currentProcesses = getFileProcessingQueue

      val cacheData =
        if (addToHead)
          (List(processingItem) ::: currentProcesses).mkString("|")
        else
          (currentProcesses ::: List(processingItem)).mkString("|")

      LOG.debug("Smart File Consumer - adding ({}) to processing queue", processingItem)
      LOG.debug("Smart File Consumer - saving processing queue. key={}, value={}", File_Requests_Cache_Key,
        if (cacheData.length == 0) "(empty)" else cacheData)

      envContext.saveConfigInClusterCache(File_Processing_Cache_Key, cacheData.getBytes)

    }
  }

  //value for file processing queue in cache has the format <node1>/<thread1>:<groupOfFileNamesSeperatedBy"~~">|<node2>/<thread1>:<groupOfFileNamesSeperatedBy"~~">
  def removeFromProcessingQueue(processingItem: String): Unit = {
    processingQLock.synchronized {

      val currentProcesses = getFileProcessingQueue
      val cacheData = (currentProcesses diff List(processingItem)).mkString("|")

      LOG.debug("Smart File Consumer - removing ({}) from processing queue", processingItem)
      LOG.debug("Smart File Consumer - saving processing queue. key={}, value={}", File_Requests_Cache_Key,
        if (cacheData.length == 0) "(empty)" else cacheData)

      envContext.saveConfigInClusterCache(File_Processing_Cache_Key, cacheData.getBytes)

    }
  }

  def clearProcessingQueue(): Unit = {
    processingQLock.synchronized {
      val cacheData = ""
      envContext.saveConfigInClusterCache(File_Processing_Cache_Key, cacheData.getBytes)
    }
  }

  //what a leader should do when recieving file processing request
  def requestFileLeaderCallback(eventType: String, eventPath: String, eventPathData: String): Unit = {
    LOG.debug("Smart File Consumer - requestFileLeaderCallback: eventType={}, eventPath={}, eventPathData={}",
      eventType, eventPath, eventPathData)
    //    logger.error("==============> HaithamLog => inside requestFileLeaderCallback :")


    if (eventPathData != null && eventPathData.length > 0) {
      envContext.setListenerCacheKey(eventPath, "") //clear it. TODO : find a better way

      var addRequestToQueue = false
      if (eventType.equalsIgnoreCase("put") || eventType.equalsIgnoreCase("update") ||
        eventType.equalsIgnoreCase("CHILD_UPDATED") || eventType.equalsIgnoreCase("CHILD_ADDED")) {
        val keyTokens = eventPath.split("/")
        val requestingNodeId = keyTokens(keyTokens.length - 2)
        val requestingThreadId = keyTokens(keyTokens.length - 1)
        val fileToProcessKeyPath = eventPathData //from leader

        LOG.info("Smart File Consumer - Leader has received a request from Node {}, Thread {}", requestingNodeId, requestingThreadId)

        //just add to request queue
        val newRequest = requestingNodeId + "/" + requestingThreadId + ":" + fileToProcessKeyPath
        if (!isShutdown)
          addToRequestQueue(newRequest)

        //        logger.error("==============> HaithamLog => inside requestFileLeaderCallback : before assignFileProcessingIfPossible ")
        assignFileProcessingIfPossible()
      }
      //should do anything for remove?
    }
  }

  //this is to be called whenever we have some changes in requests/new files
  //checks if there is a request ready, if parallelism degree allows new processing
  //   and if there is file needs processing
  //if all conditions met then assign a file to first request in the queue
  private def assignFileProcessingIfPossible(): Unit = {
    //    logger.error("==============> HaithamLog => inside assignFileProcessingIfPossible : initialFilesHandled = " + initialFilesHandled + ", hasPendingFileRequestsInQueue = " + hasPendingFileRequestsInQueue)

    if (isShutdown)
      return

    if (initialFilesHandled) {
      LOG.debug("Smart File Consumer - Leader is checking if it is possible to assign a new file to process")
      if (hasPendingFileRequestsInQueue) {
        requestQLock.synchronized {
          if (hasPendingFileRequestsInQueue) {
            var processingQueue = getFileProcessingQueue
            val requestQueue = getFileRequestsQueue

            //there are ndoes/threads ready to process
            val request = requestQueue.head //take first request
            removeFromRequestQueue(request)
            LOG.debug("Smart File Consumer - finished call to saveFileRequestsQueue, from assignFileProcessingIfPossible")
            var requestAssigned = false

            //since a request in cache has the format <node1>/<thread1>:<path to receive files>|<node2>/<thread1>:<path to receive files>
            val requestTokens = request.split(":")
            val fileToProcessKeyPath = requestTokens(1) //something like SmartFileCommunication/FromLeader/<NodeId>/<thread id>
            val requestNodeInfoTokens = requestTokens(0).split("/")
            val requestingNodeId = requestNodeInfoTokens(0)
            val requestingThreadId = requestNodeInfoTokens(1)

            LOG.info("Smart File Consumer - currently " + processingQueue.length + " File(s) are being processed")
            LOG.info("Smart File Consumer - Maximum processing ops is " + adapterConfig.monitoringConfig.consumersCount)

            //check if it is allowed to process one more file
            if (processingQueue.length < adapterConfig.monitoringConfig.consumersCount) {

              val groupToProcessFullPath = if (monitorController == null) null
              else monitorController.getNextGroupToProcess

              var data: ArrayBuffer[String] = new ArrayBuffer()
              if (groupToProcessFullPath != null) {

                LOG.debug("Smart File Consumer - Adding a group processing assignment of group + " + groupToProcessFullPath +
                  " to Node " + requestingNodeId + ", thread Id=" + requestingThreadId)

                //leave offset management to engine, usually this will be other than zero when calling startProcessing
                val offset = 0L //getFileOffsetFromCache(fileToProcessFullPath)

                // iterate to add ("|" + offset) to each file path
                groupToProcessFullPath.split("~~").foreach(fileToProcessFullPath => {
                  data += fileToProcessFullPath + "|" + offset
                })

                //there are groups that need to process
                //val groupOfFiles = groupToProcessFullPath.mkString("~~")
                val newProcessingItem = requestingNodeId + "/" + requestingThreadId + ":" + groupToProcessFullPath
                addToProcessingQueue(newProcessingItem)

                try {
                  // one setListenerCacheKey for all keys
                  val groupOfFilesData = data.mkString("~~")
                  //                  logger.error("==============> HaithamLog => inside assignFileProcessingIfPossible2 : fileToProcessKeyPath = " + fileToProcessKeyPath + ", groupOfFilesData = " + groupOfFilesData)
                  envContext.setListenerCacheKey(fileToProcessKeyPath, groupOfFilesData)
                } catch {
                  case e: Throwable => {
                    removeFromProcessingQueue(newProcessingItem)
                  }
                }

                requestAssigned = true
              }
              else {
                LOG.info("Smart File Consumer - No more files currently to process")
              }
            }
            else {
              LOG.info("Smart File Consumer - Cannot assign anymore files to process")
            }

            //if request was not handled, must get it back to request queue
            //FIXME : find a better way to sync instead of removing and adding back
            if (!requestAssigned)
              addToRequestQueue(request, true)
          }
        }
      }
      else {
        LOG.debug("Smart File Consumer - request queue is empty, no participants are available for new processes")
      }
    }
    else {
      LOG.debug("Smart File Consumer - initial files have not been handled yet")
    }
  }

  private var initialFilesHandled = false

  //used to assign files to participants right after calling start processing, if the engine has files to be processed
  //initialFilesToProcess: list of (node, partitionId, file name, offset)
  private def assignInitialFiles_old(initialFilesToProcess: Array[(String, Int, String, Long)]): Unit = {
    LOG.debug("Smart File Consumer - handling initial assignment ")

    if (initialFilesToProcess == null || initialFilesToProcess.length == 0) {
      LOG.debug("Smart File Consumer - no initial files to process")
      return
    }
    var processingQueue = getFileProcessingQueue
    var requestQueue = getFileRequestsQueue

    //wait to get requests from all threads
    val maxTrials = 5
    var trialsCounter = 1
    while (trialsCounter <= maxTrials && requestQueue.size < adapterConfig.monitoringConfig.consumersCount) {
      Thread.sleep(1000)
      requestQueue = getFileRequestsQueue
      trialsCounter += 1
    }

    val assignedFilesList = ArrayBuffer[String]()
    //<node1>/<thread1>:<path to receive files>|<node2>/<thread1>:<path to receive files>
    requestQueue.foreach(requestStr => {
      val reqTokens = requestStr.split(":")
      val fileAssignmentKeyPath = reqTokens(1)
      val participantPathTokens = reqTokens(0).split("/")
      val nodeId = participantPathTokens(0)
      val partitionId = participantPathTokens(1).toInt
      initialFilesToProcess.find(fileInfo =>
        fileInfo._1.equals(nodeId) && fileInfo._2 == partitionId) match {
        case None => {}
        case Some(fileInfo) => {
          removeFromRequestQueue(requestStr) //remove the current request
          //LOG.debug("Smart File Consumer - finished call to saveFileRequestsQueue, from assignInitialFiles")

          val fileToProcessFullPath = fileInfo._3

          if (assignedFilesList.contains(fileToProcessFullPath)) {
            LOG.warn("Smart File Consumer - Initial files : file ({}) was already assigned", fileToProcessFullPath)
          }
          else {
            assignedFilesList.append(fileToProcessFullPath)

            val newProcessingItem = nodeId + "/" + partitionId + ":" + fileToProcessFullPath
            addToProcessingQueue(newProcessingItem) //add to processing queue

            LOG.debug("Smart File Consumer - Initial files : Adding a file processing assignment of file (" + fileToProcessFullPath +
              ") to Node " + nodeId + ", partition Id=" + partitionId)
            val offset = fileInfo._4
            val data = fileToProcessFullPath + "|" + offset
            envContext.setListenerCacheKey(fileAssignmentKeyPath, data)
          }
        }
      }
    })


    initialFilesHandled = true
  }

  private def assignInitialFiles(initialFilesToProcess: Array[(String, Int, String, Long)]): Unit = {
    //    logger.error("==============> HaithamLog => inside assignInitialFiles ")

    LOG.debug("Smart File Consumer - handling initial assignment ")

    if (initialFilesToProcess == null || initialFilesToProcess.length == 0) {
      LOG.debug("Smart File Consumer - no initial files to process")
      //      logger.error("==============> HaithamLog => inside assignInitialFiles : no initial files to process")

      return
    }
    requestQLock.synchronized {

      var processingQueue = getFileProcessingQueue
      var requestQueue = getFileRequestsQueue

      //wait to get requests from all threads
      val maxTrials = 5
      var trialsCounter = 1
      while (trialsCounter <= maxTrials && requestQueue.size < adapterConfig.monitoringConfig.consumersCount) {
        Thread.sleep(1000)
        requestQueue = getFileRequestsQueue
        trialsCounter += 1
      }

      val assignedFilesList = ArrayBuffer[String]()
      //<node1>/<thread1>:<path to receive files>|<node2>/<thread1>:<path to receive files>
      initialFilesToProcess.foreach(fileInfo => {

        if (assignedFilesList.contains(fileInfo._3)) {
          LOG.warn("Smart File Consumer - Initial files : file ({}) was already assigned", fileInfo._3)
          //          logger.error("==============> HaithamLog => inside assignInitialFiles : file ({" + fileInfo._3 + "}) was already assigned")
        }

        else {

          var requestToAssign: String = ""

          requestQueue.find(requestStr => {
            val reqTokens = requestStr.split(":")
            val participantPathTokens = reqTokens(0).split("/")
            val nodeId = participantPathTokens(0)
            val partitionId = participantPathTokens(1).toInt

            fileInfo._1.equals(nodeId) && fileInfo._2 == partitionId
          }) match {
            case None => {}
            case Some(requestStr) => {
              requestToAssign = requestStr
            }
          }

          if (requestToAssign == null || requestToAssign.length == 0) {
            LOG.info("Smart File Consumer - has not received a request from Node {}, Partition {} to handle initial file {}. trying to find another partition ",
              fileInfo._1, fileInfo._2.toString, fileInfo._3)
            val requestQueue = getFileRequestsQueue
            if (requestQueue.length > 0) {
              requestToAssign = requestQueue.head
              removeFromRequestQueue(requestToAssign)
            }
            else {
              LOG.warn("Smart File Consumer - could not find any partition ready to handle initial file {}.",
                fileInfo._3)
            }
          }

          if (requestToAssign != null) {
            //LOG.debug("Smart File Consumer - finished call to saveFileRequestsQueue, from assignInitialFiles")
            val fileToProcessFullPath = fileInfo._3


            removeFromRequestQueue(requestToAssign) //remove the current request

            val reqTokens = requestToAssign.split(":")
            val fileAssignmentKeyPath = reqTokens(1)
            val participantPathTokens = reqTokens(0).split("/")
            val nodeId = participantPathTokens(0)
            val partitionId = participantPathTokens(1).toInt

            assignedFilesList.append(fileToProcessFullPath)

            val newProcessingItem = nodeId + "/" + partitionId + ":" + fileToProcessFullPath
            //            logger.error("==============> HaithamLog => inside assignInitialFiles : newProcessingItem =" + newProcessingItem)

            addToProcessingQueue(newProcessingItem) //add to processing queue

            LOG.debug("Smart File Consumer - Initial files : Adding a file processing assignment of file (" + fileToProcessFullPath +
              ") to Node " + nodeId + ", partition Id=" + partitionId)
            val offset = fileInfo._4
            val data = fileToProcessFullPath + "|" + offset
            //            logger.error("==============> HaithamLog => inside assignInitialFiles : fileAssignmentKeyPath =" + fileAssignmentKeyPath + ", data" + data)

            envContext.setListenerCacheKey(fileAssignmentKeyPath, data)
          }
        }
      })
      initialFilesHandled = true
    }
  }

  //leader
  def collectStartInfo(eventType: String, eventPath: String, eventPathData: String): Unit = {

    if (isShutdown || eventPathData == null || eventPathData.length == 0) {
      return
    }

    LOG.debug("Smart File Consumer - leader got start info. path is {}, value is {} ", eventPath, eventPathData)

    val pathTokens = eventPath.split("/")
    val sendingNodeId = pathTokens(pathTokens.length - 1)

    //(1,file1,0,true)~(2,file2,0,true)~(3,file3,1000,true)
    val dataAr = eventPathData.split("~")
    val sendingNodeStartInfo = dataAr.map(dataItem => {
      val trimmedItem = dataItem.substring(1, dataItem.length - 1) // remove parenthesis
      val itemTokens = trimmedItem.split(",")
      (itemTokens(0).toInt, itemTokens(1), itemTokens(2).toLong, itemTokens(3).toBoolean)
    }).toList

    allNodesStartInfo.put(sendingNodeId, sendingNodeStartInfo)

  }

  //what a leader should do when recieving file processing status update
  def fileProcessingLeaderCallback(eventType: String, eventPath: String, eventPathData: String): Unit = {
    LOG.debug("Smart File Consumer - fileProcessingLeaderCallback: eventType={}, eventPath={}, eventPathData={}",
      eventType, eventPath, eventPathData)

    //    logger.error("==============> HaithamLog => inside fileProcessingLeaderCallback :")


    if (eventPathData != null) {
      if (eventType.equalsIgnoreCase("put") || eventType.equalsIgnoreCase("update") ||
        eventType.equalsIgnoreCase("CHILD_UPDATED") || eventType.equalsIgnoreCase("CHILD_ADDED")) {
        val keyTokens = eventPath.split("/")
        val processingThreadId = keyTokens(keyTokens.length - 1)
        val processingNodeId = keyTokens(keyTokens.length - 2)
        //value for file processing has the format <file-name>|<status>
        val valueTokens = eventPathData.split("\\|")
        if (valueTokens.length >= 2) {
          val processingFilePath = valueTokens(0)
          val status = valueTokens(1)
          //if (status == File_Processing_Status_Finished) {
          LOG.info("Smart File Consumer (Leader) - File ({}) processing finished", processingFilePath)

          val correspondingRequestFileKeyPath = requestFilePath + "/" + processingNodeId //e.g. SmartFileCommunication/ToLeader/ProcessedFile/<nodeid>

          //remove the file from processing queue
          var processingQueue = getFileProcessingQueue
          val valueInProcessingQueue = processingNodeId + "/" + processingThreadId + ":" + processingFilePath //// ???????
          LOG.debug("Smart File Consumer (Leader) - removing from processing queue: " + valueInProcessingQueue)
          if (!isShutdown)
            removeFromProcessingQueue(valueInProcessingQueue)

          //since a file just got finished, a new one can be processed
          //          logger.error("==============> HaithamLog => inside fileProcessingLeaderCallback : before assignFileProcessingIfPossible ")
          assignFileProcessingIfPossible()

          if (status == File_Processing_Status_Finished || status == File_Processing_Status_Corrupted) {
            val procFileParentDir = MonitorUtils.getFileParentDir(processingFilePath, adapterConfig)

            val procFileLocationInfo = getDirLocationInfo(procFileParentDir)
            if (procFileLocationInfo.isMovingEnabled) {
              val moved = moveFile(processingFilePath)
              if (moved)
                monitorController.markFileAsProcessed(processingFilePath)
            }
            else {
              logger.info("File {} will not be moved since moving is disabled for folder {} - Adapter {}",
                processingFilePath, procFileParentDir, adapterConfig.Name)

              monitorController.markFileAsProcessed(processingFilePath)
            }
          }
          else if (status == File_Processing_Status_NotFound)
            monitorController.markFileAsProcessed(processingFilePath)
        }
      }
    }
  }

  //to be used by leader in case changes happened to nodes
  private def shufflePartitionsToNodes(nodes: List[String], partitionsCount: Int): HashMap[String, scala.collection.mutable.Set[Int]] with MultiMap[String, Int] = {
    val nodesPartitionsMap = new HashMap[String, scala.collection.mutable.Set[Int]] with MultiMap[String, Int]
    var counter = 0
    for (partition <- 1 to partitionsCount) {
      val mod = partition % nodes.size
      val correspondingNodeIndex = if (mod == 0) nodes.size - 1 else mod - 1

      nodesPartitionsMap.addBinding(nodes(correspondingNodeIndex), partition)

      counter += 1
    }

    nodesPartitionsMap
  }

  //what a participant should do when receiving file to process (from leader)
  def fileAssignmentFromLeaderCallback(eventType: String, eventPath: String, eventPathData: String): Unit = {

    //    logger.error("==============> HaithamLog => inside fileAssignmentFromLeaderCallback ")

    if (eventPathData == null || eventPathData.length == 0)
      return
    envContext.setListenerCacheKey(eventPath, "") //TODO : so that it will not be read again. find a better way

    //data has format <file name>|offset~~<file name2>|offset2
    // how to do the split "~~" , then  filename and offset "|"

    var filesToProcessNames: ArrayBuffer[String] = ArrayBuffer() // All all files list here
    var offsets: ArrayBuffer[Long] = ArrayBuffer() // All all files list here
    var smartFileContexts: ArrayBuffer[SmartFileConsumerContext] = ArrayBuffer() // All all files list here


    val arraytOfDataTokens = eventPathData.split("~~")
    logger.error("==============> HaithamLog => arraytOfDataTokens =  " + arraytOfDataTokens.mkString(",,"))
    arraytOfDataTokens.foreach(listItem => {
      val dataTokens = listItem.split("\\|")
      logger.error("==============> HaithamLog => dataTokens =  " + dataTokens.mkString(",,"))
      if (dataTokens.length >= 2) {
        val fileToProcessName = dataTokens(0)
        val offset = dataTokens(1).toLong

        filesToProcessNames += fileToProcessName
        offsets += offset

        val keyTokens = eventPath.split("/")
        if (keyTokens.length >= 2) {
          val processingThreadId = keyTokens(keyTokens.length - 1).toInt
          val processingNodeId = keyTokens(keyTokens.length - 2)

          if (isShutdown) {
            LOG.debug("Smart File Consumer - Node Id = {}, Thread Id = {} had been assigned a new file ({}), but shutdown already called. so ignore the assignment",
              processingNodeId, processingThreadId.toString, fileToProcessName)
            return
          }

          LOG.info("Smart File Consumer - Node Id = {}, Thread Id = {}, File ({}) was assigned",
            processingNodeId, processingThreadId.toString, fileToProcessName)


          val partitionId = processingThreadId.toInt
          var smartFileContext: SmartFileConsumerContext = null
          if (smartFileContextMap.contains(partitionId)) {
            val context = smartFileContextMap(partitionId)
            if (context.nodeId.equals(processingNodeId)) //maybe this could take place if re-shuffling happened
              smartFileContext = context
          }
          if (smartFileContext == null) {
            smartFileContext = new SmartFileConsumerContext()
            smartFileContext.adapterName = inputConfig.Name
            smartFileContext.partitionId = partitionId
            smartFileContext.ignoreFirstMsg = _ignoreFirstMsg
            smartFileContext.nodeId = processingNodeId
            smartFileContext.envContext = envContext
            //context.fileOffsetCacheKey = getFileOffsetCacheKey(fileToProcessName)
            smartFileContext.statusUpdateCacheKey = Status_Check_Cache_KeyParent + "/" + processingNodeId + "/" + processingThreadId
            smartFileContext.statusUpdateInterval = statusUpdateInterval

            smartFileContexts += smartFileContext
            //            logger.error("==============> HaithamLog => fileToProcessName =  " + fileToProcessName)
            //            logger.error("==============> HaithamLog => offset =  " + offset)
            //            logger.error("==============> HaithamLog => smartFileContext =  " + smartFileContext)

            smartFileContextMap.put(partitionId, smartFileContext)
          }
          //        val filesToProcessName = Array(fileToProcessName) // All all files list here
        }
      }
    })

    //start processing the group
    val fileHandlers = filesToProcessNames.map(fileToProcessName => SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, fileToProcessName))
    //now read the file and call sendSmartFileMessageToEngin for each message, and when finished call fileMessagesExtractionFinished_Callback to update status
    val fileMessageExtractor = new FileMessageExtractor(this, participantExecutor, adapterConfig, fileHandlers.toArray, offsets.toArray, smartFileContexts.toArray,
      sendSmartFileMessageToEngin, fileMessagesExtractionFinished_Callback)
    fileMessageExtractor.extractMessages()

  }

  //key: SmartFileCommunication/FileProcessing/<node>/<threadId>
  //val: file|status
  def fileMessagesExtractionFinished_Callback(fileHandler: SmartFileHandler, context: SmartFileConsumerContext,
                                              status: Int): Unit = {

    var statusToSendToLeader = ""

    if (isShutdown) {
      LOG.info("SMART FILE CONSUMER - participant node ({}), partition ({}) finished reading file ({}) with status {}, but adapter already shutdown",
        context.nodeId, context.partitionId.toString, fileHandler.getFullPath, status.toString)
      statusToSendToLeader = File_Processing_Status_Interrupted
    }
    else if (status == SmartFileConsumer.FILE_STATUS_FINISHED) {
      LOG.info("SMART FILE CONSUMER - participant node ({}), partition ({}) finished reading file ({})",
        context.nodeId, context.partitionId.toString, fileHandler.getFullPath)
      statusToSendToLeader = File_Processing_Status_Finished
    }
    else if (status == SmartFileConsumer.FILE_STATUS_ProcessingInterrupted) {
      LOG.info("SMART FILE CONSUMER - participant node ({}), partition ({}) interrupted while reading file ({})",
        context.nodeId, context.partitionId.toString, fileHandler.getFullPath)
      statusToSendToLeader = File_Processing_Status_Interrupted
    }
    else if (status == SmartFileConsumer.FILE_STATUS_CORRUPT) {
      LOG.error("SMART FILE CONSUMER - participant node ({}), partition ({}) file ({}) is corrupt",
        context.nodeId, context.partitionId.toString, fileHandler.getFullPath)
      statusToSendToLeader = File_Processing_Status_Corrupted
    }
    else {
      LOG.warn("SMART FILE CONSUMER - participant node ({}), partition ({}) reports file not found ({})",
        context.nodeId, context.partitionId.toString, fileHandler.getFullPath)
      statusToSendToLeader = File_Processing_Status_NotFound
    }

    //set file status as finished
    val pathKey = fileProcessingPath + "/" + context.nodeId + "/" + context.partitionId
    val data = fileHandler.getFullPath + "|" + statusToSendToLeader
    envContext.setListenerCacheKey(pathKey, data)

    //send a new file request to leader
    if (!isShutdown) {
      //shutdown will clear all queues
      val requestData = smartFileFromLeaderPath + "/" + context.nodeId + "/" + context.partitionId //listen to this SmartFileCommunication/FromLeader/<NodeId>/<partitionId id>
      val requestPathKey = requestFilePath + "/" + context.nodeId + "/" + context.partitionId
      LOG.info("SMART FILE CONSUMER - participant ({}) - sending a file request to leader on partition ({})", context.nodeId, context.partitionId.toString)
      LOG.debug("SMART FILE CONSUMER - sending the request using path ({}) using value ({})", requestPathKey, requestData)
      envContext.setListenerCacheKey(requestPathKey, requestData);
    }
  }

  //what a participant should do parallelism value changes
  def filesParallelismCallback(eventType: String, eventPath: String, eventPathData: String): Unit = {

    //data is comma separated partition ids
    val currentNodePartitions = eventPathData.split(",").map(idStr => idStr.toInt).toList

    //val newFilesParallelism = eventPathData.toInt

    val nodeId = clusterStatus.nodeId

    LOG.info("Smart File Consumer - Node Id = {}, files parallelism changed. partitions to handle are {}", nodeId, eventPathData)
    LOG.info("Smart File Consumer - Old File Parallelism is {}", filesParallelism.toString)

    var parallelismStatus = ""
    if (filesParallelism == -1)
      parallelismStatus = "Uninitialized"
    else if (currentNodePartitions.size == prevRegParticipantPartitions.size)
      parallelismStatus = "Intact"
    else parallelismStatus = "Changed"

    //filesParallelism changed
    //wait until current threads finish then re-initialize threads
    if (parallelismStatus == "Changed") {
      if (participantExecutor != null) {
        participantExecutor.shutdown() //tell the executor service to shutdown after threads finish tasks in hand
        //if(!participantExecutor.awaitTermination(5, TimeUnit.MINUTES))
        //participantExecutor.shutdownNow()
      }
    }

    filesParallelism = currentNodePartitions.size
    prevRegParticipantPartitions = currentNodePartitions


    //create the threads only if no threads created yet or number of threads changed
    if (parallelismStatus == "Uninitialized" || parallelismStatus == "Changed") {
      LOG.info("SMART FILE CONSUMER - participant ({}) - creating {} thread(s) to handle partitions ({})",
        nodeId, filesParallelism.toString, eventPathData)
      participantExecutor = Executors.newFixedThreadPool(filesParallelism)
      currentNodePartitions.foreach(partitionId => {

        val executorThread = new Runnable() {
          private var partitionId: Int = _

          def init(id: Int) = partitionId = id

          override def run(): Unit = {
            val fileProcessingAssignementKeyPath = smartFileFromLeaderPath + "/" + nodeId + "/" + partitionId //listen to this SmartFileCommunication/FromLeader/<NodeId>/<partitionId id>
            //listen to file assignment from leader
            envContext.createListenerForCacheKey(fileProcessingAssignementKeyPath, fileAssignmentFromLeaderCallback) //e.g.   SmartFileCommunication/FromLeader/RequestFile/<nodeid>/<partitionId id>

            //send a file request to leader
            val fileRequestKeyPath = requestFilePath + "/" + nodeId + "/" + partitionId
            LOG.info("SMART FILE CONSUMER - participant ({}) - sending a file request to leader on partition ({})", nodeId, partitionId.toString)
            LOG.debug("SMART FILE CONSUMER - sending the request using path ({}) using value ({})", fileRequestKeyPath, fileProcessingAssignementKeyPath)
            envContext.setListenerCacheKey(fileRequestKeyPath, fileProcessingAssignementKeyPath)
          }
        }
        executorThread.init(partitionId)
        participantExecutor.execute(executorThread)
      })
    }
  }

  //after a file is changed, move it into targetMoveDir
  def moveFile(originalFilePath: String): Boolean = {
    var isFileMoved = false
    try {
      val smartFileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, originalFilePath)
      var componentsMap = scala.collection.immutable.Map[String, String]()
      var targetMoveDir: String = null
      var flBaseName: String = null
      if (adapterConfig.archiveConfig != null && adapterConfig.archiveConfig.outputConfig != null) {
        val parentDir = smartFileHandler.getParentDir
        if (locationsMap.contains(parentDir)) {
          val locationInfo = locationsMap(parentDir)
          if (locationInfo != null && locationInfo.fileComponents != null)
            componentsMap = MonitorUtils.getFileComponents(originalFilePath, locationInfo)
        }
        val (targetMoveDir1, flBaseName1) = getTargetFile(smartFileHandler)
        targetMoveDir = targetMoveDir1
        flBaseName = flBaseName1
      }

      isFileMoved = moveFile(smartFileHandler)
      if (isFileMoved && adapterConfig.archiveConfig != null && adapterConfig.archiveConfig.outputConfig != null) {
        addArchiveFileInfo(ArchiveFileInfo(adapterConfig, targetMoveDir, flBaseName, componentsMap))
      }
    } catch {
      case e: Throwable => {
        LOG.error("Failed to move/archive file", e)
      }
    }
    isFileMoved
  }

  // Returns target
  private def getTargetFile(fileHandler: SmartFileHandler): (String, String) = {
    val originalFilePath = fileHandler.getFullPath

    /*if(!locationTargetMoveDirsMap.contains(parentDir))
      throw new Exception("No target move dir for directory " + parentDir)

    val targetMoveDir = locationTargetMoveDirsMap(parentDir)
    val fileStruct = originalFilePath.split("/")
    (targetMoveDir, fileStruct(fileStruct.size - 1))*/

    val locationInfo = getFileLocationConfig(fileHandler)
    if (locationInfo == null)
      throw new Exception("No target move dir for file " + originalFilePath)

    val targetMoveDirBase = locationInfo.targetDir
    val fileStruct = originalFilePath.split("/")
    val targetMoveDir =
      if (adapterConfig.monitoringConfig.createInputStructureInTargetDirs) {
        fileStruct.take(fileStruct.length - 1).mkString("/").replace(locationInfo.srcDir, targetMoveDirBase)
      }
      else targetMoveDirBase

    (targetMoveDir, fileStruct(fileStruct.size - 1))
  }

  def moveFile(fileHandler: SmartFileHandler): Boolean = {
    val originalFilePath = fileHandler.getFullPath
    val (targetMoveDir, flBaseName) = getTargetFile(fileHandler)

    try {

      logger.info("SMART FILE CONSUMER Moving File" + originalFilePath + " to " + targetMoveDir)
      if (fileHandler.exists()) {
        val targetDirHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, targetMoveDir)

        //base target dir already exists, but might need to build sub-dirs corresponding to input dir structure
        val targetDirExists =
        if (!targetDirHandler.exists())
          targetDirHandler.mkdirs()
        else true

        if (targetDirExists)
          fileHandler.moveTo(targetMoveDir + "/" + flBaseName)
        //fileCacheRemove(fileHandler.getFullPath)
        else {
          logger.warn("SMART FILE CONSUMER - Target dir not found and could not be created:" + targetMoveDir)
          false
        }
      } else {
        LOG.warn("SMART FILE CONSUMER File has been deleted " + originalFilePath)
        true
      }
    }
    catch {
      case e: Exception => {
        externalizeExceptionEvent(e)
        LOG.error(s"SMART FILE CONSUMER - Failed to move file ($originalFilePath) into directory ($targetMoveDir)")
        false
      }
      case e: Throwable => {
        externalizeExceptionEvent(e)
        LOG.error(s"SMART FILE CONSUMER - Failed to move file ($originalFilePath) into directory ($targetMoveDir)")
        false
      }
    }
  }

  //******************************************************************************************************

  override def Shutdown: Unit = lock.synchronized {
    StopProcessing
  }

  override def DeserializeKey(k: String): PartitionUniqueRecordKey = {
    val key = new SmartFilePartitionUniqueRecordKey
    try {
      LOG.debug("Deserializing Key:" + k)
      key.Deserialize(k)
    } catch {
      case e: Exception => {
        externalizeExceptionEvent(e)
        LOG.error("Failed to deserialize Key:%s.".format(k), e)
        throw e
      }
      case e: Throwable => {
        externalizeExceptionEvent(e)
        LOG.error("Failed to deserialize Key:%s.".format(k), e)
        throw e
      }
    }
    key
  }

  override def DeserializeValue(v: String): PartitionUniqueRecordValue = {
    val vl = new SmartFilePartitionUniqueRecordValue
    if (v != null) {
      try {
        LOG.debug("Deserializing Value:" + v)
        vl.Deserialize(v)
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          LOG.error("Failed to deserialize Value:%s.".format(v), e)
          throw e
        }
        case e: Throwable => {
          externalizeExceptionEvent(e)
          LOG.error("Failed to deserialize Value:%s.".format(v), e)
          throw e
        }
      }
    }
    vl
  }

  override def getAllPartitionBeginValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = lock.synchronized {
    getKeyValuePairs()
  }

  override def getAllPartitionEndValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = lock.synchronized {
    getKeyValuePairs()
  }

  private def getKeyValuePairs(): Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = {
    val infoBuffer = ArrayBuffer[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]()

    for (partitionId <- 1 to adapterConfig.monitoringConfig.consumersCount) {
      val rKey = new SmartFilePartitionUniqueRecordKey
      val rValue = new SmartFilePartitionUniqueRecordValue

      rKey.PartitionId = partitionId
      rKey.Name = adapterConfig.Name

      rValue.Offset = -1
      rValue.FileName = ""

      infoBuffer.append((rKey, rValue))
    }

    infoBuffer.toArray
  }

  // each value in partitionInfo is (PartitionUniqueRecordKey, PartitionUniqueRecordValue, Long, PartitionUniqueRecordValue). // key, processed value, Start transactionid, Ignore Output Till given Value (Which is written into Output Adapter) & processing Transformed messages (processing & total)
  override def GetAllPartitionUniqueRecordKey: Array[PartitionUniqueRecordKey] = lock.synchronized {

    val infoBuffer = ArrayBuffer[PartitionUniqueRecordKey]()

    for (partitionId <- 1 to adapterConfig.monitoringConfig.consumersCount) {
      val rKey = new SmartFilePartitionUniqueRecordKey
      val rValue = new SmartFilePartitionUniqueRecordValue

      rKey.PartitionId = partitionId
      rKey.Name = adapterConfig.Name

      infoBuffer.append(rKey)
    }

    infoBuffer.toArray
  }

  private def sleepMs(sleepTimeInMs: Int): Boolean = {
    var interruptedVal = false
    try {
      Thread.sleep(sleepTimeInMs)
    } catch {
      case e: InterruptedException => {
        interruptedVal = true
      }
      case e: Throwable => {
        ///
      }
    }
    interruptedVal
  }

  private def DummyCallback(fileName: String): Unit = {
  }

  override def StartProcessing(partitionIds: Array[StartProcPartInfo], ignoreFirstMsg: Boolean): Unit = {
    isShutdown = false

    _ignoreFirstMsg = ignoreFirstMsg
    var lastHb: Long = 0
    startHeartBeat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))

    LOG.info("SMART_FILE_ADAPTER - START_PROCESSING CALLED")

    // Check to see if this already started
    if (startTime > 0) {
      LOG.error("SMART_FILE_ADAPTER: already started, or in the process of shutting down")
    }
    startTime = System.nanoTime

    if (partitionIds == null || partitionIds.size == 0) {
      LOG.error("SMART_FILE_ADAPTER: Cannot process the file adapter request, invalid parameters - number")
      return
    }

    //mapping each src folder for its config
    adapterConfig.monitoringConfig.detailedLocations.foreach(location => {
      val srcDir = MonitorUtils.simpleDirPath(location.srcDir)
      locationsMap.put(srcDir, location)
    })
    partitionIds.foreach(part => {
      val partitionId = part._key.asInstanceOf[SmartFilePartitionUniqueRecordKey].PartitionId
      // Initialize the monitoring status
      partitonCounts(partitionId.toString) = 0
      partitonDepths(partitionId.toString) = 0
      partitionExceptions(partitionId.toString) = new SmartFileExceptionInfo("n/a", "n/a")
    })

    initialFilesHandled = false

    initializeNode //register the callbacks

    if (adapterConfig.archiveConfig != null && adapterConfig.archiveConfig.outputConfig != null && clusterStatus != null && clusterStatus.isLeader && clusterStatus.leaderNodeId.equals(clusterStatus.nodeId)) {
      val archiveParallelism = if (adapterConfig.archiveConfig.archiveParallelism <= 0) 1 else adapterConfig.archiveConfig.archiveParallelism
      val archiveSleepTimeInMs = if (adapterConfig.archiveConfig.archiveSleepTimeInMs < 0) 1 else adapterConfig.archiveConfig.archiveSleepTimeInMs
      logger.info("Archival Init. archiveParallelism:" + archiveParallelism + ", archiveSleepTimeInMs:" + archiveSleepTimeInMs)
      archiveExecutor = Executors.newFixedThreadPool(archiveParallelism)

      for (i <- 0 until archiveParallelism) {
        val archiveThread = new Runnable() {
          override def run(): Unit = {
            var interruptedVal = false
            while (!interruptedVal) {
              try {
                if (hasNextArchiveFileInfo) {
                  val archInfo = getNextArchiveFileInfo
                  if (archInfo != null) {
                    try {
                      val status = SmartFileHandlerFactory.archiveFile(archInfo.adapterConfig, archInfo.srcFileDir, archInfo.srcFileBaseName, archInfo.componentsMap)
                      if (!status)
                        addArchiveFileInfo(archInfo)
                    } catch {
                      case e: Throwable => {
                        addArchiveFileInfo(archInfo)
                        logger.error("Failed to archive file:" + archInfo.srcFileDir + "/" + archInfo.srcFileBaseName, e)
                      }
                    }
                  } else {
                    if (archiveSleepTimeInMs > 0)
                      interruptedVal = sleepMs(archiveSleepTimeInMs)
                  }
                } else {
                  if (archiveSleepTimeInMs > 0)
                    interruptedVal = sleepMs(archiveSleepTimeInMs)
                }
              } catch {
                case e: InterruptedException => {
                  interruptedVal = true
                }
                case e: Throwable => {
                  ///
                }
              }
            }
          }
        }
        archiveExecutor.execute(archiveThread)
      }

      if (locationTargetMoveDirsMap != null) {
        val processedDirs = scala.collection.mutable.Set[String]()
        val tmpMonitorController = new MonitorController(adapterConfig, this, DummyCallback)
        locationTargetMoveDirsMap.foreach(kv => {
          val srcDir = kv._1.trim
          val tgtDir = kv._2.trim
          logger.info("Collecting files to archive from : " + tgtDir)
          if (!processedDirs.contains(srcDir)) {
            val flsLst = tmpMonitorController.listFiles(tgtDir)
            if (flsLst != null) {
              var smartFileHandler: SmartFileHandler = null
              logger.info("Files list from: " + flsLst.mkString(","))
              flsLst.foreach(f => {
                if (smartFileHandler != null)
                  smartFileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, tgtDir + "/" + f)
                var componentsMap = scala.collection.immutable.Map[String, String]()
                if (locationsMap.contains(srcDir)) {
                  val locationInfo = locationsMap(srcDir)
                  if (locationInfo != null && locationInfo.fileComponents != null)
                    componentsMap = MonitorUtils.getFileComponents(tgtDir + "/" + f, locationInfo)
                }
                addArchiveFileInfo(ArchiveFileInfo(adapterConfig, tgtDir, f, componentsMap))
              })
            }
            processedDirs += srcDir
          }
        })
      }
    }

    //(1,file1,0,true)~(2,file2,0,true)~(3,file3,1000,true)
    val myPartitionInfo = partitionIds.map(pid => (pid._key.asInstanceOf[SmartFilePartitionUniqueRecordKey].PartitionId,
      pid._val.asInstanceOf[SmartFilePartitionUniqueRecordValue].FileName,
      pid._val.asInstanceOf[SmartFilePartitionUniqueRecordValue].Offset, ignoreFirstMsg)).mkString("~")

    val SendStartInfoToLeaderPath = sendStartInfoToLeaderParentPath + "/" + clusterStatus.nodeId // Should be different for each Nodes
    LOG.warn("Smart File Consumer - Node {} is sending start info to leader. path is {}, value is {} ",
      clusterStatus.nodeId, SendStartInfoToLeaderPath, myPartitionInfo)
    envContext.setListenerCacheKey(SendStartInfoToLeaderPath, myPartitionInfo) // => Goes to Leader

    if (clusterStatus.isLeader)
      handleStartInfo()
  }

  private def sendSmartFileMessageToEngin(smartMessage: SmartFileMessage,
                                          smartFileConsumerContext: SmartFileConsumerContext): Unit = {

    //in case the msg extracotr still had some msgs to send but some parts of the engine were already shutdown, just ignore
    if (isShutdown)
      return

    //LOG.debug("Smart File Consumer - Node {} will send a msg to engine. partition id= {}. msg={}",
    //smartFileConsumerContext.nodeId, smartFileConsumerContext.partitionId.toString, new String(smartMessage.msg))

    val partitionId = smartFileConsumerContext.partitionId
    //val partition = partitionKVs(partitionId)

    val ignoreFirstMsg = smartFileConsumerContext.ignoreFirstMsg

    // if the offset is -1, then the server wants to start from the begining, else, it means that the server
    // knows what its doing and we start from that offset.
    var readOffset: Long = -1
    //val uniqueRecordValue = if (ignoreFirstMsg) partition._3.Offset else partition._3.Offset - 1


    val uniqueKey = new SmartFilePartitionUniqueRecordKey
    val uniqueVal = new SmartFilePartitionUniqueRecordValue

    uniqueKey.Name = adapterConfig.Name
    uniqueKey.PartitionId = partitionId

    val readTmMs = System.currentTimeMillis


    val fileName = smartMessage.relatedFileHandler.getFullPath
    val offset = smartMessage.offsetInFile
    val message = getFinalMsg(smartMessage)


    // Create a new EngineMessage and call the engine.
    if (smartFileConsumerContext.execThread == null) {
      smartFileConsumerContext.execThread = execCtxtObj.CreateExecContext(input, uniqueKey, nodeContext)
    }

    incrementCountForPartition(partitionId)

    uniqueVal.Offset = offset
    uniqueVal.FileName = fileName
    //val dontSendOutputToOutputAdap = uniqueVal.Offset <= uniqueRecordValue

    LOG.debug("Smart File Consumer - Node {} is sending a msg to engine. partition id= {}. msg={}. file={}. offset={}",
      smartFileConsumerContext.nodeId, smartFileConsumerContext.partitionId.toString, new String(message), fileName, offset.toString)
    msgCount += 1
    smartFileConsumerContext.execThread.execute(message, uniqueKey, uniqueVal, readTmMs)

  }


  /**
    * add tags if needed
    */
  def getFinalMsg(smartMessage: SmartFileMessage): Array[Byte] = {

    val parentDir = smartMessage.relatedFileHandler.getParentDir
    val parentDirLocationConfig = getFileLocationConfig(smartMessage.relatedFileHandler)
    if (parentDirLocationConfig == null)
      throw new Exception(s"Dir ${parentDir} has no entry in adapter config location section")

    if (parentDirLocationConfig.msgTags == null || parentDirLocationConfig.msgTags.length == 0)
      return smartMessage.msg

    val msgStr = new String(smartMessage.msg)

    val tagDelimiter = parentDirLocationConfig.tagDelimiter
    val fileName = MonitorUtils.getFileName(smartMessage.relatedFileHandler.getFullPath)
    val parentDirName = MonitorUtils.getFileName(smartMessage.relatedFileHandler.getParentDir)

    val prefix = parentDirLocationConfig.msgTags.foldLeft("")((pre, tag) => {
      val tagValue =
        if (tag.startsWith("$")) {
          //predefined tags
          tag match {
            //assuming msg type is defined by parent folder name
            case "$Dir_Name" | "$DirName" => parentDirName
            case "$File_Name" | "$FileName" => fileName
            case "$File_Full_Path" | "$FileFullPath" => smartMessage.relatedFileHandler.getFullPath
            case "$Line_Number" | "$LineNumber" => smartMessage.msgNumber.toString
            case "$Msg_Number" | "$MsgNumber" => smartMessage.msgNumber.toString
            case _ => ""
          }
        }
        else {
          //if not predefined just add it as is
          tag
        }
      pre + tagValue + tagDelimiter
    })

    val finalMsg = prefix + msgStr
    finalMsg.getBytes
  }

  override def StopProcessing: Unit = {
    LOG.debug("Smart File Consumer - shutting down the adapter")

    initialized = false
    isShutdown = true

    if (archiveExecutor != null)
      archiveExecutor.shutdownNow()
    archiveExecutor = null
    archiveInfo.clear()

    if (monitorController != null)
      monitorController.stopMonitoring
    monitorController = null

    if (leaderExecutor != null) {
      LOG.debug("Smart File Consumer - shutting down leader executor service")
      keepCheckingStatus = false
      MonitorUtils.shutdownAndAwaitTermination(leaderExecutor, "Leader executor")
    }

    terminateReaderTasks

    clearCache

    prevRegParticipantPartitions = List()
    prevRegLeader = ""
    filesParallelism = -1

  }

  private def clearCache(): Unit = {

    //TODO:- make sure to Clear listeners & queues all stuff related to leader or non-leader. So, Next SartProcessing should start the whole stuff again.


    if (prevRegParticipantPartitions != null) {
      prevRegParticipantPartitions.foreach(partitionId => {

        //clear file processing listener path
        val fileProcessListenerPathKey = fileProcessingPath + "/" + clusterStatus.nodeId + "/" + partitionId
        val emptyProcessingData = "|"
        LOG.debug("clearing process cache key {}", fileProcessListenerPathKey)
        envContext.setListenerCacheKey(fileProcessListenerPathKey, emptyProcessingData)

        //clear status check cache
        val cacheKey = Status_Check_Cache_KeyParent + "/" + clusterStatus.nodeId + "/" + partitionId
        envContext.saveConfigInClusterCache(cacheKey, "".getBytes())
      })
    }

    //clear start info path
    val SendStartInfoToLeaderPath = sendStartInfoToLeaderParentPath + "/" + clusterStatus.nodeId
    envContext.setListenerCacheKey(SendStartInfoToLeaderPath, "")


    if (clusterStatus != null) {
      if (clusterStatus.isLeader) {
        LOG.debug("Smart File Consumer - Cleaning queues and cache stuff by leader")
        clearProcessingQueue()
        clearRequestQueue()

        if (allNodesStartInfo != null)
          allNodesStartInfo.clear()
      }
      else {
        //clean participant-related stuff

      }
    }
  }

  /*private def clearProcessingAssignmentListenerKeys(): Unit ={
    if(allNodesStartInfo == null)
      return
    allNodesStartInfo.foreach(tuple => {
      val nodeId = tuple._1
      tuple._2.foreach(item => {
        val partitionId = item._1
        val path = smartFileFromLeaderPath + "/" + nodeId + "/" + partitionId
        envContext.setListenerCacheKey(path, "")
      })
    })
  }*/

  private def terminateReaderTasks(): Unit = {

    // Tell all thread to stop processing on the next interval, and shutdown the Excecutor.
    quiesce

    // Give the threads to gracefully stop their reading cycles, and then execute them with extreme prejudice.
    //Thread.sleep(adapterConfig.monitoringConfig.waitingTimeMS)

    if (participantExecutor != null)
      MonitorUtils.shutdownAndAwaitTermination(participantExecutor, "Participant Executor")

    LOG.debug("Smart File Adapter - Shutdown Complete")
    //participantExecutor = null
    startTime = 0

    smartFileContextMap.clear()
    locationsMap.clear()
    prevRegParticipantPartitions = List()
    prevRegLeader = ""
    filesParallelism = -1
  }

  /* no need for any synchronization here... it can only go one way.. worst case scenario, a reader thread gets to try to
*  read  one extra time (100ms lost)
 */
  private def quiesce: Unit = {
    isQuiesced = true
  }

  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    implicit val formats = org.json4s.DefaultFormats

    var depths: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = null

    try {
      depths = getAllPartitionEndValues
    } catch {
      case e: KamanjaException => {
        return new MonitorComponentInfo(AdapterConfiguration.TYPE_INPUT, adapterConfig.Name, SmartFileConsumer.ADAPTER_DESCRIPTION, startHeartBeat, lastSeen, Serialization.write(metrics).toString)
      }
      case e: Exception => {
        LOG.error("SMART-FILE-ADAPTER: Unexpected exception determining depths for smart file input adapter " + adapterConfig.Name, e)
        return new MonitorComponentInfo(AdapterConfiguration.TYPE_INPUT, adapterConfig.Name, SmartFileConsumer.ADAPTER_DESCRIPTION, startHeartBeat, lastSeen, Serialization.write(metrics).toString)
      }
    }

    /*partitonDepths.clear
    depths.foreach(t => {
      try {
        val partId = t._1.asInstanceOf[SmartFilePartitionUniqueRecordKey]
        val localPart = kvs.getOrElse(partId.PartitionId,null)
        if (localPart != null) {
          val partVal = t._2.asInstanceOf[SmartFilePartitionUniqueRecordValue]
          var thisDepth: Long = 0
          if(localReadOffsets.contains(partId.PartitionId)) {
            thisDepth = localReadOffsets(partId.PartitionId)
          }
          partitonDepths(partId.PartitionId.toString) = partVal.Offset - thisDepth
        }

      } catch {
        case e: Exception => LOG.warn("SMART-FILE-ADAPTER: error trying to determine depths for smart file input adapter "+adapterConfig.Name,e)
      }
    })*/

    return new MonitorComponentInfo(AdapterConfiguration.TYPE_INPUT, adapterConfig.Name, SmartFileConsumer.ADAPTER_DESCRIPTION,
      startHeartBeat, lastSeen, Serialization.write(metrics).toString)
  }

  override def getComponentSimpleStats: String = {
    return "Input/" + adapterConfig.Name + "/evtCnt" + "->" + msgCount
  }

  private def incrementCountForPartition(pid: Int): Unit = {
    val cVal: Long = partitonCounts.getOrElse(pid.toString, 0)
    partitonCounts(pid.toString) = cVal + 1
  }

  def getDirLocationInfo(srcDir: String): LocationInfo = {
    logger.debug("getDirLocationInfo for file " + srcDir)
    logger.debug(locationsMap)

    if (locationsMap.contains(srcDir))
      locationsMap(srcDir)
    else {
      val search = locationsMap.find(tuple => (srcDir + "/").startsWith(tuple._1 + "/"))
      search match {
        case None => null
        case Some(tuple) => tuple._2
      }
    }
  }

  def getFileLocationConfig(fileHandler: SmartFileHandler): LocationInfo = {
    logger.debug("getFileLocationConfig for file " + fileHandler.getFullPath)
    val parentDir = fileHandler.getParentDir
    getDirLocationInfo(parentDir)
  }
}
