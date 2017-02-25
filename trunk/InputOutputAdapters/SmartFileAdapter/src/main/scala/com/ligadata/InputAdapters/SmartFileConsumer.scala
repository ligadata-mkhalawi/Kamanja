package com.ligadata.InputAdapters

import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import com.ligadata.Exceptions.KamanjaException


import scala.actors.threadpool.TimeUnit
import java.util.zip.ZipException

import com.ligadata.HeartBeat.MonitorComponentInfo
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.AdaptersConfiguration._
import com.ligadata.KamanjaBase._
import com.ligadata.Utils.{Utils, ClusterStatus}
import org.apache.logging.log4j.LogManager
import org.json4s.jackson.Serialization
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import com.ligadata.Serialize._
import com.ligadata.VelocityMetrics._

import scala.actors.threadpool.{Executors, ExecutorService}
import scala.collection.mutable.{Map, MultiMap, HashMap, ArrayBuffer}

case class BufferLeftoversArea(workerNumber: Int, leftovers: Array[Byte], relatedChunk: Int)

//case class BufferToChunk(len: Int, payload: Array[Byte], chunkNumber: Int, relatedFileHandler: SmartFileHandler, firstValidOffset: Int, isEof: Boolean, partMap: scala.collection.mutable.Map[Int,Int])
case class SmartFileMessage(msg: Array[Byte], offsetInFile: Long, relatedFileHandler: SmartFileHandler, msgNumber: Long, msgStartOffset: Long)

case class FileStatus(status: Int, offset: Long, createDate: Long)

//case class OffsetValue (lastGoodOffset: Int, partitionOffsets: Map[Int,Int])
case class EnqueuedFileHandler(fileHandler: SmartFileHandler, offset: Long, lastModifiedDate: Long,
                               locationInfo: LocationInfo, componentsMap: scala.collection.immutable.Map[String, String])

case class EnqueuedGroupHandler(fileHandlers: Array[EnqueuedFileHandler])

case class FileProcessingData(Fl: String, Off: Long)

case class GroupProcessingData(Files: Option[List[FileProcessingData]])

case class GroupProcessingItem(N: Int, T: Int, Files: Option[List[String]])

case class GroupProcessingItmWithStatus(item: GroupProcessingItem, status: String)

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
  var inUse: Boolean = false
}

case class InputAdapterStatus(var fileName: String, var recordsCount: Long, var startTs: String, var endTs: String,
                              var bytesRead: Long, var nodeId: String, var status: String)

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

// typ == 0 is LeaderCallbackRequest, typ == 1 is FileProcessingCallbackRequest
case class LeaderCallbackRequest(typ: Int, eventType: String, eventPath: String, eventPathData: String)

case class FileAssignmentsCallbackRequest(eventType: String, eventPath: String, eventPathData: String)

case class ArchiveFileInfo(adapterConfig: SmartFileAdapterConfiguration, locationInfo: LocationInfo,
                           srcFileDir: String, srcFileBaseName: String,
                           componentsMap: scala.collection.immutable.Map[String, String],
                           var previousAttemptsCount: Int,
                           var destArchiveDir: String = "",
                           var srcFileStartOffset: Long = 0)

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
  private val msgCount = new AtomicLong(0)

  private val smartFileContextMap_lock = new Object()
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

  private val filesInProcessingQueue = scala.collection.mutable.Set[String]()
  private val filesProcessingQueueLock = new Object

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

  //  val File_Requests_Cache_Key = "Smart_File_Adapter/" + adapterConfig.Name + "/" + "FileRequests"

  private val fileRequestsQueue = ArrayBuffer[String]()

  //maintained by leader, stores only files being processed (as list under one key). so that if leader changes, new leader can get the processing status
  //  val File_Processing_Cache_Key = "Smart_File_Adapter/" + adapterConfig.Name + "/" + "FileProcessing"

  private val processingFilesQueue = ArrayBuffer[String]()
  private val processingFilesQStartTime = scala.collection.mutable.Map[String, Long]()

  private var nextQueueDumptime = 0L

  private var envContext: EnvContext = nodeContext.getEnvCtxt()
  private var clusterStatus: ClusterStatus = null
  private var archiveExecutor: ExecutorService = null
  private var archiveInfo = new scala.collection.mutable.Queue[ArchiveFileInfo]()
  private var participantExecutor: ExecutorService = null
  private var participantsFilesAssignmentExecutor: ExecutorService = null
  private var leaderExecutor: ExecutorService = null
  private var filesParallelism: Int = -1
  private var monitorController: MonitorController = null
  private var archiver: Archiver = null
  private val assignedFileProcessingAssignementKeyPaths = ArrayBuffer[String]()

  private var initialized = false
  private var filesParallelismCallback_initialized = false
  private var prevRegLeader = ""

  private var prevRegParticipantPartitions: List[Int] = List()

  private var _ignoreFirstMsg: Boolean = _

  private val _leaderCallbackRequests = ArrayBuffer[LeaderCallbackRequest]()
  private val _leaderCallbackReqsLock = new ReentrantReadWriteLock(true)

  private val _fileAssignmentsCallbackRequests = ArrayBuffer[FileAssignmentsCallbackRequest]()
  private val _fileAssignmentsCallbackReqsLock = new ReentrantReadWriteLock(true)

  val statusUpdateInterval = 5000
  //ms
  //val maxWaitingTimeForNodeStatus = 10 * 1000000000L;//10 seconds, value is in nanoseconds.
  val maxFailedCheckCounts = 721

  private var keepCheckingStatus = false

  //key is node id, value is (partition id, file name, offset, ignoreFirstMsg)
  private val allNodesStartInfo = scala.collection.mutable.Map[String, List[(Int, String, Long, Boolean)]]()

  envContext.registerNodesChangeNotification(nodeChangeCallback)

  //calling the velocity metrics instances
  getVelocityInstances = vm.getFileVelocityInstances(VMFactory, Category, inputConfig.Name, inputConfig.fullAdapterConfig, nodeContext)

  private val _reent_lock = new ReentrantReadWriteLock(true)

  def isConsumerShutdown = isShutdown

  def isFileInProcessingQueue(file: String): Boolean = {
    filesProcessingQueueLock.synchronized {
      filesInProcessingQueue.contains(file)
    }
  }

  def getAllFilesInProcessingQueue: Set[String] = {
    filesProcessingQueueLock.synchronized {
      filesInProcessingQueue.toSet
    }
  }

  def removeFilesFromProcessingQueue(fls: Array[String]): Unit = {
    filesProcessingQueueLock.synchronized {
      filesInProcessingQueue --= fls
    }
  }

  def addFilesToProcessingQueue(fls: Array[String]): Unit = {
    filesProcessingQueueLock.synchronized {
      filesInProcessingQueue ++= fls
    }
  }

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

  /*def hasNextArchiveFileInfo: Boolean = {
    (archiveInfo.size > 0)
  }

  def getNextArchiveFileInfo: ArchiveFileInfo = {
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
  }*/

  private def IsLeaderNode: Boolean = (clusterStatus != null && clusterStatus.isLeader && clusterStatus.leaderNodeId.equals(clusterStatus.nodeId))

  private def getLeaderCallbackRequests: Array[LeaderCallbackRequest] = {
    var retVals: Array[LeaderCallbackRequest] = null
    ReadLock(_leaderCallbackReqsLock)
    try {
      retVals = _leaderCallbackRequests.toArray
    } finally {
      ReadUnlock(_leaderCallbackReqsLock)
    }
    retVals
  }

  private def getLeaderCallbackRequestsAndClear: Array[LeaderCallbackRequest] = {
    var retVals: Array[LeaderCallbackRequest] = null
    WriteLock(_leaderCallbackReqsLock)
    try {
      retVals = _leaderCallbackRequests.toArray
      _leaderCallbackRequests.clear
    } finally {
      WriteUnlock(_leaderCallbackReqsLock)
    }
    retVals
  }

  private def addLeaderCallbackRequest(typ: Int, eventType: String, eventPath: String, eventPathData: String): Unit = {
    WriteLock(_leaderCallbackReqsLock)
    try {
      _leaderCallbackRequests += LeaderCallbackRequest(typ, eventType, eventPath, eventPathData)
    } finally {
      WriteUnlock(_leaderCallbackReqsLock)
    }
  }

  private def getFileAssignmentsCallbackRequests: Array[FileAssignmentsCallbackRequest] = {
    var retVals: Array[FileAssignmentsCallbackRequest] = null
    ReadLock(_fileAssignmentsCallbackReqsLock)
    try {
      retVals = _fileAssignmentsCallbackRequests.toArray
    } finally {
      ReadUnlock(_fileAssignmentsCallbackReqsLock)
    }
    retVals
  }

  private def getFileAssignmentsCallbackRequestsAndClear: Array[FileAssignmentsCallbackRequest] = {
    var retVals: Array[FileAssignmentsCallbackRequest] = null
    WriteLock(_fileAssignmentsCallbackReqsLock)
    try {
      retVals = _fileAssignmentsCallbackRequests.toArray
      _fileAssignmentsCallbackRequests.clear
    } finally {
      WriteUnlock(_fileAssignmentsCallbackReqsLock)
    }
    retVals
  }

  private def addFileAssignmentsCallbackRequest(eventType: String, eventPath: String, eventPathData: String): Unit = {
    if (isShutdown) return
    WriteLock(_fileAssignmentsCallbackReqsLock)
    try {
      _fileAssignmentsCallbackRequests += FileAssignmentsCallbackRequest(eventType, eventPath, eventPathData)
    } finally {
      WriteUnlock(_fileAssignmentsCallbackReqsLock)
    }
  }

  def isFileExists(originalFilePath: String): Boolean = {
    try {
      val fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, originalFilePath)
      return fileHandler.exists()
    } catch {
      case e: Throwable => {
        LOG.error("Failed to check Exists for file:" + originalFilePath, e)
      }
    }

    false
  }

  //add the node callback
  private def initializeNode: Unit = synchronized {
    if (LOG.isDebugEnabled) LOG.debug("Max memeory = " + Runtime.getRuntime().maxMemory())

    if (nodeContext == null && LOG.isDebugEnabled)
      LOG.debug("Smart File Consumer - nodeContext = null")

    if (nodeContext.getEnvCtxt() == null && LOG.isDebugEnabled)
      LOG.debug("Smart File Consumer - nodeContext.getEnvCtxt() = null")

    if (LOG.isDebugEnabled) {
      if (envContext == null)
        LOG.debug("Smart File Consumer - envContext = null")
      else {
        if (envContext.getClusterInfo() == null)
          LOG.debug("Smart File Consumer - envContext.getClusterInfo() = null")
      }
    }

    if (LOG.isDebugEnabled) {
      if (clusterStatus == null)
        LOG.debug("Smart File Consumer - clusterStatus = null")
      else {
        LOG.debug("Smart File Consumer - clusterStatus.nodeId = " + clusterStatus.nodeId)
        LOG.debug("Smart File Consumer - clusterStatus.leaderNodeId = " + clusterStatus.leaderNodeId)
      }
    }

    if (initialized == false) {
      if (!filesParallelismCallback_initialized) {
        val fileParallelismPath = filesParallelismParentPath + "/" + clusterStatus.nodeId
        if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - participant {} is listening to path {}", clusterStatus.nodeId, fileParallelismPath)
        envContext.createListenerForCacheKey(fileParallelismPath, filesParallelismCallback)
        filesParallelismCallback_initialized = true
      }
    }

    if (IsLeaderNode) {
      // Clearing Previous cache on the new leader start
      clearRequestQueue()
      clearProcessingQueue()
      if (allNodesStartInfo != null)
        allNodesStartInfo.clear()

      val newfilesParallelism = (adapterConfig.monitoringConfig.consumersCount.toDouble / clusterStatus.participantsNodeIds.size).ceil.toInt
      if (filesParallelism != -1) {
        // BUGBUG:: FIXME:- Unregister stuff
        // It is already distributes. Need to Re-Register stuff????
      }

      //action for the leader node

      if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - Leader is running on node " + clusterStatus.nodeId)

      if (adapterConfig.archiveConfig != null) {
        if (logger.isWarnEnabled) logger.warn("Adapter {} {} is creating new instance of archiver", adapterConfig.Name, this)
        archiver = new Archiver(adapterConfig, this)
      }

      monitorController = new MonitorController(adapterConfig, this, newFileDetectedCallback)
      monitorController.checkConfigDirsAccessibility //throw an exception if a folder is not accissible

      envContext.createListenerForCacheChildern(sendStartInfoToLeaderParentPath, collectStartInfo) // listen to start info

      leaderExecutor = Executors.newFixedThreadPool(2)
      val statusCheckerThread = new Runnable() {
        var lastStatus: scala.collection.mutable.Map[String, (Long, Int)] = null

        override def run(): Unit = {
          while (keepCheckingStatus) {
            try {
              Thread.sleep(statusUpdateInterval)
              lastStatus = checkParticipantsStatus(lastStatus)
            }
            catch {
              case ie: InterruptedException => {
                if (LOG.isWarnEnabled) LOG.warn("Smart File Consumer - interrupted " + ie)
              }
              case e: Throwable => {
                LOG.error("Smart File Consumer - unkown exception " + e)
              }
            }
          }
        }
      }
      keepCheckingStatus = true
      leaderExecutor.execute(statusCheckerThread)

      val assignFilesToRequestThread = new Runnable() {
        override def run(): Unit = {
          // First we process Req. Type 1 and then 0
          val reqTypesProcessingOrder = Array[Int](1, 0)
          while (!isShutdown) {
            try {
              val leaderCallbackRequests = getLeaderCallbackRequestsAndClear
              var appliedReq = 0

              var moveExecutor = Executors.newFixedThreadPool(32)

              val startTime = System.currentTimeMillis
              var moveWaitingTime: Long = 0
              try {
                reqTypesProcessingOrder.foreach(reqTypeId => {
                  leaderCallbackRequests.foreach(req => {
                    if (reqTypeId == req.typ && req.typ == 1) {
                      val eventType = req.eventType
                      val eventPath = req.eventPath
                      val eventPathData = req.eventPathData
                      if ((!isShutdown) && (eventType.equalsIgnoreCase("put") || eventType.equalsIgnoreCase("update") ||
                        eventType.equalsIgnoreCase("CHILD_UPDATED") || eventType.equalsIgnoreCase("CHILD_ADDED"))) {
                        val keyTokens = eventPath.split("/")
                        val processingThreadId = keyTokens(keyTokens.length - 1)
                        val processingNodeId = keyTokens(keyTokens.length - 2)
                        //value for file processing has the format <file-name>|<status>
                        val dataObj = createGroupWithStatusFromProcessingItemJson(eventPathData)
                        if (dataObj != null) {
                          val processingFiles = if (dataObj != null && dataObj.item != null && dataObj.item.Files != None) dataObj.item.Files.get else List[String]()
                          val status = dataObj.status
                          if (LOG.isWarnEnabled) {
                            LOG.warn("Smart File Consumer (Leader) - File ({}) processing finished by node {} , partition {} , status={}",
                              processingFiles, processingNodeId, processingThreadId.toString, status)
                          }

                          var handledInnMoveThread = false
                          if ((status == File_Processing_Status_Finished || status == File_Processing_Status_Corrupted) && !processingFiles.isEmpty) {
                            val procFileParentDir = MonitorUtils.getFileParentDir(processingFiles.head, adapterConfig)
                            val procFileLocationInfo = getDirLocationInfo(procFileParentDir)
                            if (LOG.isTraceEnabled) LOG.trace("procFileParentDir:" + procFileParentDir)
                            if (procFileLocationInfo.isMovingEnabled) {
                              val moveThread = new Runnable() {
                                override def run(): Unit = {
                                  val data = dataObj
                                  val processingFls = processingFiles
                                  var doneMove = false
                                  var tryNo = 0
                                  val maxTrys = 10

                                  var flsToMove = processingFiles.filter(flPath => isFileExists(flPath))
                                  if (LOG.isDebugEnabled) LOG.debug("Files to move:" + flsToMove.mkString(","))
                                  while (!doneMove && !isShutdown && tryNo < maxTrys) {
                                    var lastFlPath = ""
                                    try {
                                      tryNo += 1
                                      if (!doneMove) {
                                        var moved = true
                                        flsToMove.foreach(flPath => {
                                          lastFlPath = flPath
                                          if (LOG.isDebugEnabled) LOG.debug("Moving file:" + flPath)
                                          val moved1 = moveFile(flPath)
                                          if (!moved1)
                                            moved = moved1
                                        })
                                        if (moved && !isShutdown) {
                                          monitorController.markFileAsProcessed(processingFls)
                                          //remove the file from processing queue
                                          val valueInProcessingQueue = createProcessingItemJsonFromGroup(processingNodeId.toInt, processingThreadId.toInt, processingFls)
                                          if (!isShutdown) {
                                            removeFromProcessingQueue(valueInProcessingQueue)
                                            if (LOG.isInfoEnabled) LOG.info("Smart File Consumer (Leader) - removing from processing queue: " + valueInProcessingQueue)
                                          }
                                        }
                                        doneMove = moved
                                      }
                                    } catch {
                                      case e: Throwable => {
                                        // BUGBUG:: What happens if file failed to move. We are keep on retrying
                                        LOG.error("Failed to move file:" + lastFlPath)
                                      }
                                    }
                                    if (!doneMove && !isShutdown) {
                                      try {
                                        if (tryNo == maxTrys) {
                                          LOG.error("Failed to move files:" + flsToMove.mkString(",") + ", Try:" + tryNo + ", giving up")
                                        } else {
                                          LOG.error("Failed to move files:" + flsToMove.mkString(",") + ", Try:" + tryNo + ", waiting for 1 secs and retry")
                                          Thread.sleep(1000)
                                        }
                                      } catch {
                                        case e: Throwable => {}
                                      }
                                    }
                                    val flsToMove1 = flsToMove.filter(flPath => !isFileExists(flPath))
                                    flsToMove = flsToMove1
                                  }
                                }
                              }
                              moveExecutor.execute(moveThread)
                              handledInnMoveThread = true
                            }
                            else if (!processingFiles.isEmpty) {
                              if (logger.isInfoEnabled) logger.info("File {} will not be moved since moving is disabled for folder {} - Adapter {}",
                                processingFiles.mkString(","), procFileParentDir, adapterConfig.Name)

                              if (!isShutdown)
                                monitorController.markFileAsProcessed(processingFiles)
                            }
                          }
                          else if (status == File_Processing_Status_NotFound && !isShutdown)
                            monitorController.markFileAsProcessed(processingFiles)

                          if (!handledInnMoveThread) {
                            //remove the file from processing queue
                            val valueInProcessingQueue = createProcessingItemJsonFromGroup(processingNodeId.toInt, processingThreadId.toInt, processingFiles)
                            if (!isShutdown) {
                              removeFromProcessingQueue(valueInProcessingQueue)
                              if (LOG.isWarnEnabled) LOG.warn("Smart File Consumer (Leader) - removing from processing queue: " + valueInProcessingQueue)
                            }
                          }
                          appliedReq += 1
                        }
                      }
                    } else if (reqTypeId == req.typ && req.typ == 0) {
                      val eventType = req.eventType
                      val eventPath = req.eventPath
                      val eventPathData = req.eventPathData
                      if ((!isShutdown) && (eventType.equalsIgnoreCase("put") || eventType.equalsIgnoreCase("update") ||
                        eventType.equalsIgnoreCase("CHILD_UPDATED") || eventType.equalsIgnoreCase("CHILD_ADDED"))) {
                        val keyTokens = eventPath.split("/")
                        val requestingNodeId = keyTokens(keyTokens.length - 2)
                        val requestingThreadId = keyTokens(keyTokens.length - 1)
                        val fileToProcessKeyPath = eventPathData //from leader

                        if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - Leader has received a request from Node {}, Thread {}", requestingNodeId, requestingThreadId)

                        //just add to request queue
                        val newRequest = requestingNodeId + "/" + requestingThreadId + ":" + fileToProcessKeyPath
                        if (!isShutdown) {
                          if (isPartitionInProcessingQueue(requestingThreadId.toInt)) {
                            if (logger.isWarnEnabled) logger.warn("Partition {} is requesting to process file {} but already in processing queue",
                              requestingThreadId, fileToProcessKeyPath)
                          }
                          else
                            addToRequestQueue(newRequest)
                        }
                        appliedReq += 1
                      }
                    }
                  })

                  if (reqTypeId == 1) {
                    moveWaitingTime = System.currentTimeMillis
                    if (moveExecutor != null) {
                      if (logger.isInfoEnabled) logger.info("Waiting for files to move");
                      var cntr = 0
                      moveExecutor.shutdown()
                      val tm = System.currentTimeMillis
                      while (!moveExecutor.isTerminated && cntr < 17280000) {
                        Thread.sleep(5) // sleep 5ms and then check
                        cntr += 1
                        //                        if ((cntr % 2) == 0) {
                        //                          assignFileProcessingIfPossible()
                        //                        }
                        if ((cntr % 12000) == 0) {
                          if (logger.isWarnEnabled()) logger.warn("Waiting for files to move from past %d ms".format(System.currentTimeMillis - tm));
                        }
                      }
                    }
                    moveExecutor = null
                  }
                })
              }
              catch {
                case ie: InterruptedException => {
                  LOG.error("Smart File Consumer - interrupted " + ie)
                }
                case e: Throwable => {
                  LOG.error("Smart File Consumer - unkown exception ", e)
                }
              } finally {
                if (moveExecutor != null) {
                  moveWaitingTime = System.currentTimeMillis
                  if (logger.isInfoEnabled()) logger.info("Waiting for files to move");
                  var cntr = 0
                  moveExecutor.shutdown()
                  val tm = System.currentTimeMillis
                  while (!moveExecutor.isTerminated && cntr < 17280000) {
                    Thread.sleep(5) // sleep 5ms and then check
                    cntr += 1
                    //                    if ((cntr % 2) == 0) {
                    //                      assignFileProcessingIfPossible()
                    //                    }
                    if ((cntr % 12000) == 0) {
                      if (logger.isWarnEnabled()) logger.warn("Waiting for files to move from past %d ms".format(System.currentTimeMillis - tm));
                    }
                  }
                }
                moveExecutor = null
              }
              if (logger.isDebugEnabled) logger.debug("assignFilesToRequestThread: leaderCallbackRequests:%d, ReqQ:%d, ProcessingQ:%d".format(leaderCallbackRequests.size, fileRequestsQueue.size, processingFilesQueue.size));

              val beforeAssignFilesTime = System.currentTimeMillis
              assignFileProcessingIfPossible()
              val afrterAssignFilesTime = System.currentTimeMillis

              if (appliedReq > 0 && (afrterAssignFilesTime - startTime) > 30000) {
                if (logger.isWarnEnabled) logger.warn("Time Assignments. Process Requests:%d ms, Move took:%d ms, AssignFiles:%d ms, ReqQ:%d, appliedReq:%d".format(
                  moveWaitingTime - startTime, beforeAssignFilesTime - moveWaitingTime, afrterAssignFilesTime - beforeAssignFilesTime, leaderCallbackRequests.size, appliedReq));
              }
              else if (appliedReq > 0 && logger.isInfoEnabled()) {
                logger.info("Time Assignments. Process Requests:%d ms, Move took:%d ms, AssignFiles:%d ms, ReqQ:%d, appliedReq:%d".format(
                  moveWaitingTime - startTime, beforeAssignFilesTime - moveWaitingTime, afrterAssignFilesTime - beforeAssignFilesTime, leaderCallbackRequests.size, appliedReq));
              }

              if (appliedReq == 0) {
                try {
                  Thread.sleep(5)
                } catch {
                  case e: Throwable => {
                  }
                }
              }
            }
            catch {
              case ie: InterruptedException => {
                if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - interrupted " + ie)
              }
              case e: Throwable => {
                LOG.error("Smart File Consumer - unkown exception ", e)
              }
            }
          }
          if (logger.isWarnEnabled) logger.warn("Done assignFilesToRequestThread");
        }
      }
      keepCheckingStatus = true
      leaderExecutor.execute(assignFilesToRequestThread)

      //now register listeners for new requests (other than initial ones)
      if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - Leader is listening to children of path " + requestFilePath)
      envContext.createListenerForCacheChildern(requestFilePath, requestFileLeaderCallback) // listen to file requests
      if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - Leader is listening to children of path " + fileProcessingPath)
      envContext.createListenerForCacheChildern(fileProcessingPath, fileProcessingLeaderCallback) // listen to file processing status

      prevRegLeader = clusterStatus.leaderNodeId

      //FIXME : recalculate partitions for each node and send partition ids to nodes ???
      //set parallelism
      filesParallelism = newfilesParallelism
      //envContext.setListenerCacheKey(filesParallelismPath, filesParallelism.toString)
    }

    //
    initialized = true
  }

  //after leader collects start info, it must pass initial files to monitor and, and assign partitions to participants
  private def handleStartInfo(): Unit = {
    if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - handleStartInfo()")
    //Thread.sleep(10000) need to keep track of Partitions we got from nodes (whether we got all the participants we sent to engine or not)
    val maximumTrials = 10
    var trialCounter = 1
    while (trialCounter <= maximumTrials && allNodesStartInfo.size < clusterStatus.participantsNodeIds.size) {
      Thread.sleep(1000)
      trialCounter += 1
    }

    if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - allNodesStartInfo = " + allNodesStartInfo)

    //send to each node what partitions to handle (as received from engine)
    allNodesStartInfo.foreach(nodeStartInfo => {
      val path = filesParallelismParentPath + "/" + nodeStartInfo._1
      val data = nodeStartInfo._2.map(nodePartitionInfo => nodePartitionInfo._1).mkString(",")
      if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - Leader is sending parallelism info. key is {}. value is {}", path, data)
      envContext.setListenerCacheKey(path, data)
    })

    //now since we have start info for all participants
    //collect file names and offsets
    val initialFileNames = ArrayBuffer[String]()

    val initialFilesToProcess = ArrayBuffer[(String, Int, String, Long)]()
    allNodesStartInfo.foreach(nodeStartInfo => nodeStartInfo._2.foreach(nodePartitionInfo => {
      val fileName = nodePartitionInfo._2.trim
      if (LOG.isDebugEnabled) LOG.debug("nodePartitionInfo._2 = " + (if (nodePartitionInfo._2 == null) "actual null" else nodePartitionInfo._2))
      if (fileName.length > 0 && !fileName.equals("null")) {
        if (!initialFileNames.contains(fileName.trim)) {
          val fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, fileName)
          val locConf = getFileLocationConfig(fileHandler)
          if (locConf == null)
            logger.error("Adapter {} - Ignoring initial file {} since it has no location config",
              adapterConfig.Name, fileName)
          else {
            if (fileHandler.exists()) {
              //no need to assign deleted files or files with size less than initial offset

              initialFilesToProcess.append((nodeStartInfo._1, nodePartitionInfo._1, fileName, nodePartitionInfo._3))
              initialFileNames.append(fileName)
            }
            else {
              if (LOG.isWarnEnabled) LOG.warn("Smart File Consumer - File ({}) does not exist", fileName)
            }
          }
        }
      }
      //(node, partitionId, file name, offset)
    }))

    // (First we need to process what ever files we get here), if we have file names
    if (initialFilesToProcess.size > 0)
      assignInitialFiles(initialFilesToProcess.toArray)
    else {
      initialFilesHandled = true //nothing to handle
    }

    //now run the monitor
    if (!isShutdown) {
      if (logger.isWarnEnabled) logger.warn("adapter {} is starting monitor", adapterConfig.Name)
      monitorController.init(initialFileNames.toList)
      monitorController.startMonitoring()
    }
  }

  def nodeChangeCallback(newClusterStatus: ClusterStatus): Unit = {
    //action for participant nodes:
    clusterStatus = newClusterStatus
  }

  //will be useful when leader has requests from all nodes but no more files are available. then leader should be notified when new files are detected
  private def newFileDetectedCallback(): Unit = {
    assignFileProcessingIfPossible()
  }

  //leader constantly checks processing participants to make sure they are still working
  private def checkParticipantsStatus(previousStatusMap: scala.collection.mutable.Map[String, (Long, Int)]): scala.collection.mutable.Map[String, (Long, Int)] = {
    //if previousStatusMap==null means this is first run of checking, no errors

    if (isShutdown)
      return previousStatusMap

    if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - checking participants status")

    if (LOG.isDebugEnabled) {
      if (previousStatusMap == null)
        LOG.debug("Smart File Consumer - previousStatusMap is " + null)
      else
        LOG.debug("Smart File Consumer - previousStatusMap is {}", previousStatusMap)
    }

    val currentStatusMap = scala.collection.mutable.Map[String, (Long, Int)]()

    val processingQueue = getFileProcessingQueue

    val curTime = System.currentTimeMillis

    if (nextQueueDumptime < curTime) {
      if (nextQueueDumptime > 0) {
        if (LOG.isWarnEnabled) LOG.warn("Current Processing Queue:" + processingQueue.mkString("\\|") + ", Requests Queue:" + getFileRequestsQueue.mkString("\\|"))
      }
      nextQueueDumptime = curTime + 5 * 60 * 1000
    }

    processingQChangeLock.synchronized {
      val queuesToRemove = ArrayBuffer[(String, String, String, String, String)]()
      processingQueue.foreach(processStr => {
        if (!isShutdown) {
          val processingItem = createGroupFromProcessingItemJson(processStr)
          val processingFiles = if (processingItem != null && processingItem.Files != null && processingItem.Files != None) processingItem.Files.get else List[String]()
          val nodeId = processingItem.N
          val partitionId = processingItem.T
          val filesListInProcess = processingFiles.mkString(",")

          var statusData: Array[Byte] = null
          val cacheKey = Status_Check_Cache_KeyParent + "/" + nodeId + "/" + partitionId
          try {
            statusData = envContext.getConfigFromClusterCache(cacheKey) // filename~offset~timestamp~donestatus
          }
          catch {
            case ex: Throwable => logger.error("", ex)
          }
          val statusDataStr: String = if (statusData == null) null else new String(statusData)

          var failedCheckCount = 0
          var previousTimestamp = 0L
          var fileInStatus: String = null

          if (previousStatusMap != null && previousStatusMap.contains(filesListInProcess)) {
            val tmp = previousStatusMap(filesListInProcess)
            val previousTimestamp = tmp._1
            failedCheckCount = tmp._2
          }

          if (!isShutdown && previousStatusMap != null && (statusDataStr == null || statusDataStr.trim.length == 0)) {
            if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - current participants status in cache key {} is {}", cacheKey, statusDataStr)
            val errStr = "Smart File Consumer - file " + filesListInProcess + " is supposed to be processed by partition " + partitionId + " on node " + nodeId + " but not found in node updated status. used key is " + cacheKey
            LOG.error(errStr)

            //participant hasn't sent any status yet. store current time, to delete from processing queue if case is still the same after limit
            failedCheckCount += 1
            currentStatusMap.put(filesListInProcess, (System.nanoTime, failedCheckCount))
          }
          else if (!isShutdown && (statusDataStr == null || statusDataStr.trim.length == 0)) {
            if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - current participants status in cache is {}", statusDataStr)
            val errStr = "Smart File Consumer - file " + filesListInProcess + " is supposed to be processed by partition " + partitionId + " on node " + nodeId + " but not found in node updated status"
            LOG.error(errStr)
            failedCheckCount += 1
            currentStatusMap.put(filesListInProcess, (System.nanoTime, failedCheckCount))
          }
          else if (!isShutdown) {
            if (LOG.isInfoEnabled) LOG.info("Smart File Consumer - current participants status in cache is {} , key is {}", statusDataStr, cacheKey)

            // BUGBUG:: Can we move this to JSON string
            val statusDataTokens = statusDataStr.split("~", -1)
            fileInStatus = statusDataTokens(0)
            val currentTimeStamp = statusDataTokens(2).toLong
            val doneStatus = if (statusDataTokens.size >= 4) statusDataTokens(3) else ""

            if (!doneStatus.equals("done")) {
              if (previousStatusMap != null && !fileInStatus.equals(filesListInProcess)) {
                if (LOG.isWarnEnabled) {
                  //could this happen?
                  val warnStr = "Smart File Consumer - file " + filesListInProcess + " is supposed to be processed by partition " + partitionId + " on node " + nodeId + " but found this file " + fileInStatus + " in node updated status"
                  LOG.warn(warnStr)
                }
              }
              else {
                if (previousStatusMap != null && previousStatusMap.contains(fileInStatus)) {
                  val tmp = previousStatusMap(fileInStatus)
                  previousTimestamp = tmp._1
                  failedCheckCount = tmp._2

                  if (currentTimeStamp > previousTimestamp) {
                    //no problems here
                    failedCheckCount = 0
                    currentStatusMap.put(fileInStatus, (currentTimeStamp, failedCheckCount))
                  }
                  else if (currentTimeStamp == previousTimestamp) {
                    val errStr = "Smart File Consumer - file " + fileInStatus + " is being processed by partition " + partitionId + " on node " + nodeId + ", but status hasn't been updated for " + ((currentTimeStamp - previousTimestamp) / 1000) + " ms. where currentTimeStamp=" + currentTimeStamp + " - previousTimestamp=" + previousTimestamp + ", failedCheckCount=" + failedCheckCount + " , key is " + cacheKey
                    LOG.error(errStr)
                    failedCheckCount += 1
                    currentStatusMap.put(fileInStatus, (currentTimeStamp, failedCheckCount))
                  }
                }
                else {
                  failedCheckCount += 1
                  currentStatusMap.put(fileInStatus, (currentTimeStamp, failedCheckCount))
                }
              }
            } else {
              if (previousStatusMap != null && !fileInStatus.equals(filesListInProcess)) {
                if (LOG.isWarnEnabled) {
                  //could this happen?
                  val warnStr = "Smart File Consumer - file " + filesListInProcess + " is supposed to be processed by partition " + partitionId + " on node " + nodeId + " but found this file " + fileInStatus + " in node updated status"
                  LOG.warn(warnStr)
                }
              }
              else {
                // Keeping the previous one as it is.
                currentStatusMap.put(fileInStatus, (currentTimeStamp, failedCheckCount))
              }
            }
          }

          if (failedCheckCount > maxFailedCheckCounts) {
            //if (currentTimeStamp - previousTimestamp > maxWaitingTimeForNodeStatus) {
            if (LOG.isDebugEnabled) {
              val debugStr = "Smart File Consumer - file processing item (" + processStr + ") has faile count = " + failedCheckCount + ". should be removed from processing queue"
              LOG.debug(debugStr)
            }
            queuesToRemove.append((processStr, filesListInProcess, fileInStatus, nodeId.toString, partitionId.toString))
          }
        }
      })

      queuesToRemove.foreach(removeInfo => {
        if (!isShutdown) {
          if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - removing the following from processing queue: {}", removeInfo._1)

          removeFromProcessingQueue(removeInfo._1)
          assignFileProcessingIfPossible()

          if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - removing the following entry from currentStatusMap: {}", removeInfo._3)
          if (removeInfo._3 != null && removeInfo._3.length > 0)
            currentStatusMap.remove(removeInfo._3)
          //TODO : should be removed from cache status ?
        }
      })
    }
    if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - currentStatusMap is {}", currentStatusMap)

    currentStatusMap
  }

  //value in cache has the format <node1>/<thread1>:<path to receive files>|<node2>/<thread1>:<path to receive files>
  def hasPendingFileRequestsInQueue: Boolean = {
    requestQLock.synchronized {
      (!fileRequestsQueue.isEmpty)
    }
  }

  //value in cache has the format <node1>/<thread1>:<path to receive files>|<node2>/<thread1>:<path to receive files>
  def getFileRequestsQueue: Array[String] = {
    requestQLock.synchronized {
      fileRequestsQueue.toArray
    }
  }

  def getNextFileRequestFromQueue: String = {
    requestQLock.synchronized {
      if (fileRequestsQueue.size == 0) return null
      val retVal = fileRequestsQueue(0)
      fileRequestsQueue.remove(0)
      if (LOG.isDebugEnabled) LOG.debug("getNextFileRequestFromQueue. Size:" + fileRequestsQueue.size)
      retVal
    }
  }

  def addToRequestQueue(request: String, addToHead: Boolean = false): Unit = {
    requestQLock.synchronized {
      if (addToHead)
        fileRequestsQueue.prepend(request)
      else
        fileRequestsQueue.append(request)
      if (LOG.isDebugEnabled) LOG.debug("addToRequestQueue. Size:" + fileRequestsQueue.size)
    }
  }

  def removeFromRequestQueue(request: String): Unit = {
    requestQLock.synchronized {
      fileRequestsQueue -= request
      if (LOG.isDebugEnabled) LOG.debug("removeFromRequestQueue. Size:" + fileRequestsQueue.size)
    }
  }

  def clearRequestQueue(): Unit = {
    requestQLock.synchronized {
      fileRequestsQueue.clear
      if (LOG.isDebugEnabled) LOG.debug("clearRequestQueue. Size:" + fileRequestsQueue.size)
    }
  }

  //value for file processing queue in cache has the format <node1>/<thread1>:<filename>|<node2>/<thread1>:<filename>
  def getFileProcessingQueue: Array[String] = {
    processingQLock.synchronized {
      return processingFilesQueue.toArray
    }
  }

  //value for file processing queue in cache has the format <node1>/<thread1>:<filename>|<node2>/<thread1>:<filename>
  def getFileProcessingQSize: Int = {
    processingQLock.synchronized {
      processingFilesQueue.size
    }
  }

  def isPartitionInProcessingQueue(partitionId: Int): Boolean = {
    processingQLock.synchronized {
      processingFilesQueue.exists(item => {
        val processingItem = createGroupFromProcessingItemJson(item)
        processingItem.T == partitionId
      })
    }
  }

  def addToProcessingQueue(processingItem: String, addToHead: Boolean = false): Unit = {
    if (LOG.isTraceEnabled) LOG.trace("addToProcessingQueue => processingItem:" + processingItem)
    processingQLock.synchronized {
      if (addToHead)
        processingFilesQueue.prepend(processingItem)
      else
        processingFilesQueue.append(processingItem)
    }
  }

  def removeFromProcessingQueue(processingItem: String): Unit = {
    if (LOG.isTraceEnabled) LOG.trace("removeFromProcessingQueue:" + processingItem)
    processingQLock.synchronized {
      processingFilesQueue -= processingItem
    }
  }

  def clearProcessingQueue(): Unit = {
    processingQLock.synchronized {
      processingFilesQueue.clear()
      processingFilesQStartTime.clear
      /*
      val cacheData = ""
      envContext.saveConfigInClusterCache(File_Processing_Cache_Key, cacheData.getBytes)
      */
    }
  }

  private def createProcessingItemJsonFromGroup(requestingNodeId: Int, requestingThreadId: Int, fls: List[String]): String = {
    if (fls == null || fls.isEmpty) {
      """{"N":%d,"T":%d,"Files":[]}""".format(requestingNodeId, requestingThreadId)
    } else {
      """{"N":%d,"T":%d,"Files":[%s]}""".format(requestingNodeId, requestingThreadId, fls.map(fl => {
        "\"%s\"".format(fl)
      }).mkString(","))
    }
  }

  private def createProcessingItemJsonFromGroup(requestingNodeId: Int, requestingThreadId: Int, grp: EnqueuedGroupHandler): String = {
    if (grp == null || grp.fileHandlers == null) {
      createProcessingItemJsonFromGroup(requestingNodeId, requestingThreadId, List[String]())
    } else {
      createProcessingItemJsonFromGroup(requestingNodeId, requestingThreadId, grp.fileHandlers.map(fl => fl.fileHandler.getFullPath()).toList)
    }
  }

  private def createProcessingItemWithStatusJsonFromGroup(requestingNodeId: Int, requestingThreadId: Int, grp: EnqueuedGroupHandler, status: String): String = {
    """{"item":%s,"status":"%s"}""".format(createProcessingItemJsonFromGroup(requestingNodeId, requestingThreadId, grp), status)
  }

  private def createProcessingItemWithStatusJsonFromGroup(requestingNodeId: Int, requestingThreadId: Int, fls: List[String], status: String): String = {
    """{"item":%s,"status":"%s"}""".format(createProcessingItemJsonFromGroup(requestingNodeId, requestingThreadId, fls), status)
  }

  private def createProcessingJsonDataFromGroup(grp: EnqueuedGroupHandler): String = {
    if (grp == null || grp.fileHandlers == null) {
      """{"Files":[]}"""
    } else {
      //leave offset management to engine, usually this will be other than zero when calling startProcessing
      """{"Files":[""" + grp.fileHandlers.map(fl => { """{"Fl":"%s","Off":0}""".format(fl.fileHandler.getFullPath())}).mkString(",") + "]}"
    }
  }

  private def createProcessingJsonDataFromFileAndOffset(file: String, offset: Long): String = {
    """{"Files":[""" + """{"Fl":"%s","Off":%d}""".format(file, offset) + "]}"
  }

  private def createGroupFromProcessingItemJson(data: String): GroupProcessingItem = {
    var retVal: GroupProcessingItem = null
    try {
      implicit val jsonFormats = DefaultFormats
      retVal = parse(data).extract[GroupProcessingItem]
    } catch {
      case e: Throwable => {
        LOG.error("Failed to prepare GroupProcessingItem", e)
      }
    }

    return retVal
  }

  private def createGroupWithStatusFromProcessingItemJson(data: String): GroupProcessingItmWithStatus = {
    var retVal: GroupProcessingItmWithStatus = null
    try {
      implicit val jsonFormats = DefaultFormats
      retVal = parse(data).extract[GroupProcessingItmWithStatus]
    } catch {
      case e: Throwable => {
        LOG.error("Failed to prepare GroupProcessingItmWithStatus", e)
      }
    }

    return retVal
  }

  private def createGroupFromProcessingJsonData(data: String): GroupProcessingData = {
    var retVal: GroupProcessingData = null
    try {
      implicit val jsonFormats = DefaultFormats
      retVal = parse(data).extract[GroupProcessingData]
    } catch {
      case e: Throwable => {
        LOG.error("Failed to prepare GroupProcessingData", e)
      }
    }

    return retVal
  }

  //what a leader should do when recieving file processing request
  def requestFileLeaderCallback(eventType: String, eventPath: String, eventPathData: String): Unit = {
    if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - requestFileLeaderCallback: eventType={}, eventPath={}, eventPathData={}",
      eventType, eventPath, eventPathData)

    if (eventPathData != null && eventPathData.length > 0 && IsLeaderNode && !isShutdown) {
      val startTm = System.currentTimeMillis
      envContext.setListenerCacheKey(eventPath, "") //clear it. TODO : find a better way
      addLeaderCallbackRequest(0, eventType, eventPath, eventPathData)
      if (LOG.isInfoEnabled) LOG.info("requestFileLeaderCallback took:%d ms for eventType:%s, eventPath:%s, eventPathData:%s".format(System.currentTimeMillis - startTm, eventType, eventPath, eventPathData))
      //should do anything for remove?
    }
  }

  //this is to be called whenever we have some changes in requests/new files
  //checks if there is a request ready, if parallelism degree allows new processing
  //   and if there is file needs processing
  //if all conditions met then assign a file to first request in the queue
  private def assignFileProcessingIfPossible(): Unit = {
    if (isShutdown || !IsLeaderNode)
      return
    if (initialFilesHandled) {
      if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - Leader is checking if it is possible to assign a new file to process")
      if (hasPendingFileRequestsInQueue) {
        processingQLock.synchronized {
          var canAssignedReq = true
          var loopCntr = 0
          while (canAssignedReq && hasPendingFileRequestsInQueue && getFileProcessingQSize < adapterConfig.monitoringConfig.consumersCount && loopCntr < 128) {
            loopCntr += 1
            val request = getNextFileRequestFromQueue //take first request
            if (request != null) {
              if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - finished call to saveFileRequestsQueue, from assignFileProcessingIfPossible")
              var requestAssigned = false
              var duplicateRequest = false

              //since a request in cache has the format <node1>/<thread1>:<path to receive files>|<node2>/<thread1>:<path to receive files>
              val requestTokens = request.split(":")
              val fileToProcessKeyPath = requestTokens(1) //something like SmartFileCommunication/FromLeader/<NodeId>/<thread id>
              val requestNodeInfoTokens = requestTokens(0).split("/")
              val requestingNodeId = requestNodeInfoTokens(0)
              val requestingThreadId = requestNodeInfoTokens(1)

              if (LOG.isInfoEnabled) LOG.info("Smart File Consumer - currently " + getFileProcessingQSize + " File(s) are being processed")
              if (LOG.isInfoEnabled) LOG.info("Smart File Consumer - Maximum processing ops is " + adapterConfig.monitoringConfig.consumersCount)
              //check if it is allowed to process one more file

              if (getFileProcessingQSize < adapterConfig.monitoringConfig.consumersCount) {
                if (isPartitionInProcessingQueue(requestingThreadId.toInt)) {
                  duplicateRequest = true
                  if (logger.isInfoEnabled) logger.info("assignFileProcessingIfPossible : Partition {} is requesting to process file {} but already in processing queue",
                    requestingThreadId, fileToProcessKeyPath)
                }
                else {
                  val groupToProcess = if (monitorController == null) null
                  else monitorController.getNextGroupToProcess
                  if (groupToProcess != null) {
                    //leave offset management to engine, usually this will be other than zero when calling startProcessing
                    val data = createProcessingJsonDataFromGroup(groupToProcess)
                    val newProcessingItem = createProcessingItemJsonFromGroup(requestingNodeId.toInt, requestingThreadId.toInt, groupToProcess)
                    if (LOG.isWarnEnabled) LOG.warn("Smart File Consumer - Adding Processing group + " + data +
                      " to Node " + requestingNodeId + ", thread Id=" + requestingThreadId +
                      ". fileToProcessKeyPath=" + fileToProcessKeyPath + ", newProcessingItem:" + newProcessingItem)
                    //there are files that need to process
                    addToProcessingQueue(newProcessingItem)
                    try {
                      //logger.warn("")
                      val cacheKey = Status_Check_Cache_KeyParent + "/" + requestingNodeId + "/" + requestingThreadId
                      envContext.saveConfigInClusterCache(cacheKey, "".getBytes)
                      envContext.setListenerCacheKey(fileToProcessKeyPath, data)
                    } catch {
                      case e: Throwable => {
                        removeFromProcessingQueue(newProcessingItem)
                      }
                    }

                    requestAssigned = true
                  }
                  else {
                    if (LOG.isInfoEnabled) LOG.info("Smart File Consumer - No more files currently to process")
                  }

                }
                canAssignedReq = true
              }
              else {
                canAssignedReq = false
                if (LOG.isInfoEnabled) LOG.info("Smart File Consumer - Cannot assign anymore files to process")
              }

              //if request was not handled, must get it back to request queue
              //FIXME : find a better way to sync instead of removing and adding back
              if (!requestAssigned && !duplicateRequest)
                addToRequestQueue(request, true)
            } else {
              canAssignedReq = false
            }
          }
        }
      }
      else {
        if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - request queue is empty, no participants are available for new processes")
      }
    }
    else {
      if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - initial files have not been handled yet")
    }
  }

  private var initialFilesHandled = false

  private def assignInitialFiles(initialFilesToProcess: Array[(String, Int, String, Long)]): Unit = {
    if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - handling initial assignment ")

    if (initialFilesToProcess == null || initialFilesToProcess.length == 0) {
      if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - no initial files to process")
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
          if (LOG.isWarnEnabled) LOG.warn("Smart File Consumer - Initial files : file ({}) was already assigned", fileInfo._3)
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
            if (LOG.isInfoEnabled) LOG.info("Smart File Consumer - has not received a request from Node {}, Partition {} to handle initial file {}. trying to find another partition ",
              fileInfo._1, fileInfo._2.toString, fileInfo._3)
            val requestQueue = getFileRequestsQueue
            if (requestQueue.length > 0) {
              requestToAssign = requestQueue.head
              removeFromRequestQueue(requestToAssign)
            }
            else {
              if (LOG.isWarnEnabled) LOG.warn("Smart File Consumer - could not find any partition ready to handle initial file {}.",
                fileInfo._3)
            }
          }

          if (requestToAssign != null && requestToAssign.length > 0) {
            //LOG.debug("Smart File Consumer - finished call to saveFileRequestsQueue, from assignInitialFiles")
            val fileToProcessFullPath = fileInfo._3

            removeFromRequestQueue(requestToAssign) //remove the current request

            val reqTokens = requestToAssign.split(":")
            if (reqTokens.length >= 2) {
              val fileAssignmentKeyPath = reqTokens(1)
              val participantPathTokens = reqTokens(0).split("/")
              if (participantPathTokens.length >= 2) {
                val nodeId = participantPathTokens(0)
                val partitionId = participantPathTokens(1).toInt

                assignedFilesList.append(fileToProcessFullPath)

                val newProcessingItem = nodeId + "/" + partitionId + ":" + fileToProcessFullPath
                addToProcessingQueue(newProcessingItem) //add to processing queue

                val offset = fileInfo._4
                val data = createProcessingJsonDataFromFileAndOffset(fileToProcessFullPath, offset)
                if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - Initial files : Adding a file processing assignment of file (" + fileToProcessFullPath +
                  ") to Node " + nodeId + ", partition Id=" + partitionId + ", data:" + data + ", newProcessingItem:" + newProcessingItem)
                envContext.setListenerCacheKey(fileAssignmentKeyPath, data)
              }
            }
          }
        }
      })
      initialFilesHandled = true
    }
  }

  //leader
  def collectStartInfo(eventType: String, eventPath: String, eventPathData: String): Unit = {
    if (!IsLeaderNode || isShutdown) {
      return
    }

    if (eventPathData == null || eventPathData.length == 0 || eventPathData.trim.length == 0) {
      return
    }

    val pathTokens = eventPath.split("/")
    val sendingNodeId = pathTokens(pathTokens.length - 1)

    if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - leader got start info. path is {}, value is {} ", eventPath, eventPathData)

    // BUGBUG:: For now we are handling single file here. Groups does not handle here
    //(1,file1,0,true)~(2,file2,0,true)~(3,file3,1000,true)
    val dataAr = eventPathData.split("~").filter(d => !(d.trim.isEmpty))
    val sendingNodeStartInfo = dataAr.map(dataItem => {
      val trimmedItem = dataItem.substring(1, dataItem.length - 1) // remove parenthesis
      val itemTokens = trimmedItem.split(",")
      (itemTokens(0).toInt, itemTokens(1), itemTokens(2).toLong, itemTokens(3).toBoolean)
    }).toList

    allNodesStartInfo.put(sendingNodeId, sendingNodeStartInfo)
  }

  //what a leader should do when recieving file processing status update
  def fileProcessingLeaderCallback(eventType: String, eventPath: String, eventPathData: String): Unit = {
    if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - fileProcessingLeaderCallback: eventType={}, eventPath={}, eventPathData={}",
      eventType, eventPath, eventPathData)
    if (eventPathData != null && !isShutdown && IsLeaderNode) {
      val startTm = System.currentTimeMillis
      addLeaderCallbackRequest(1, eventType, eventPath, eventPathData)
      if (LOG.isInfoEnabled) LOG.info("fileProcessingLeaderCallback took:%d ms for eventType:%s, eventPath:%s, eventPathData:%s".format(System.currentTimeMillis - startTm, eventType, eventPath, eventPathData))
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

  def fileAssignmentFromLeaderFn(eventType: String, eventPath: String, eventPathData: String): Unit = {
    // val startTm = System.currentTimeMillis
    envContext.setListenerCacheKey(eventPath, "") //TODO : so that it will not be read again. find a better way
    if (LOG.isInfoEnabled) LOG.info("Smart File Consumer - eventType={}, eventPath={}, eventPathData={}",
      eventType, eventPath, eventPathData)
    //data has format <file name>|offset

    if (eventPathData != null && eventPath != null) {
      val processingData = createGroupFromProcessingJsonData(eventPathData)
      if (processingData.Files != None) {
        val filesGrp = processingData.Files.get
        val keyTokens = eventPath.split("/")
        if (keyTokens.length >= 2) {
          val processingThreadId = keyTokens(keyTokens.length - 1).toInt
          val processingNodeId = keyTokens(keyTokens.length - 2)

          if (isShutdown) {
            if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - Node Id = {}, Thread Id = {} had been assigned files ({}), but shutdown already called. so ignore the assignment",
              processingNodeId, processingThreadId.toString, filesGrp.map(f => f.Fl).mkString(","))
            return
          }

          if (LOG.isInfoEnabled) LOG.info("Smart File Consumer - Node Id = {}, Thread Id = {}, Files ({}) was assigned",
            processingNodeId, processingThreadId.toString, filesGrp.map(f => f.Fl).mkString(","))
          val partitionId = processingThreadId.toInt
          var smartFileContext: SmartFileConsumerContext = null

          smartFileContextMap_lock.synchronized {
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

              smartFileContextMap.put(partitionId, smartFileContext)
            }
          }
          val actualThreadId = Thread.currentThread().getThreadGroup.getName + ">" + Thread.currentThread().getId
          if (!smartFileContext.inUse) {
            if (logger.isDebugEnabled) logger.debug("SmartFileConsumer : context {} ready to use, partition id={}, actualThreadId={}, files={}",
              smartFileContext, partitionId.toString, actualThreadId, filesGrp.map(f => f.Fl).mkString(","))
            smartFileContext.inUse = true
          }
          else {
            if (logger.isDebugEnabled) logger.debug("SmartFileConsumer : context {} already in use, partition id={}, actualThreadId={},files={}",
              smartFileContext, partitionId.toString, actualThreadId, filesGrp.map(f => f.Fl).mkString(","))
          }

          //logger.error("partitionId={}, smartFileContext={}", partitionId.toString, smartFileContext)

          //start processing the file
          val fileHanders = new Array[SmartFileHandler](filesGrp.size)
          val fileOffsets = new Array[Long](filesGrp.size)

          var idx = 0
          filesGrp.foreach(f => {
            fileHanders(idx) = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, f.Fl)
            fileOffsets(idx) = f.Off
            idx += 1
          })

          //now read the file and call sendSmartFileMessageToEngin for each message, and when finished call fileMessagesExtractionFinished_Callback to update status
          val fileMessageExtractor = new FileMessageExtractor(this, participantExecutor, adapterConfig, fileHanders, fileOffsets, smartFileContext,
            sendSmartFileMessageToEngin, fileMessagesExtractionFinished_Callback)
          fileMessageExtractor.extractMessages()

          /**Get VelocityMetrics for SmartFileName - fileToProcessName ***/
          val nodeId = nodeContext.getEnvCtxt().getNodeId()
          filesGrp.foreach(f => {
            getSmartFileVelocityMetrics(this, f.Fl, true)
          })
        }
      }
    }

    /*
    if (LOG.isWarnEnabled) LOG.warn("fileAssignmentFromLeaderFn took:%d ms for eventType:%s, eventPath:%s, eventPathData:%s".format(System.currentTimeMillis - startTm,
      if (eventType == null) "" else eventType,
      if (eventPath == null) "" else eventPath,
      if (eventPathData == null) "" else eventPathData))
    */
  }

  //what a participant should do when receiving file to process (from leader)
  def fileAssignmentFromLeaderCallback(eventType: String, eventPath: String, eventPathData: String): Unit = {
    if (eventPathData == null || eventPathData.length == 0)
      return

    val startTm = System.currentTimeMillis
    addFileAssignmentsCallbackRequest(eventType, eventPath, eventPathData)
    if (LOG.isInfoEnabled) LOG.info("fileAssignmentFromLeaderCallback took:%d ms for eventType:%s, eventPath:%s, eventPathData:%s".format(System.currentTimeMillis - startTm,
      if (eventType == null) "" else eventType,
      if (eventPath == null) "" else eventPath,
      if (eventPathData == null) "" else eventPathData))
  }

  //key: SmartFileCommunication/FileProcessing/<node>/<threadId>
  //val: file|status
  def fileMessagesExtractionFinished_Callback(fileHandlers: Array[SmartFileHandler], context: SmartFileConsumerContext,
                                              status: Int, stats: InputAdapterStatus): Unit = {
    val fileHandler = fileHandlers(0)
    val actualThreadId = Thread.currentThread().getThreadGroup.getName + ">" + Thread.currentThread().getId
    if (logger.isWarnEnabled) logger.warn("SmartFileConsumer : context {} ready to free, partition id={}, actualThreadId={}, file={}",
      context, context.partitionId.toString, actualThreadId, fileHandler.getFullPath)
    if (context.inUse) {
      if (logger.isDebugEnabled) logger.debug("SmartFileConsumer : context {} ready to free, partition id={}, actualThreadId={}, file={}",
        context, context.partitionId.toString, actualThreadId, fileHandler.getFullPath)
      context.inUse = false
    }
    else {
      if (logger.isDebugEnabled) logger.debug("SmartFileConsumer : context {} already free, partition id={}, actualThreadId={}, file={}",
        context, context.partitionId.toString, actualThreadId, fileHandler.getFullPath)
    }


    var statusToSendToLeader = ""

    if (isShutdown) {
      if (LOG.isInfoEnabled) LOG.info("SMART FILE CONSUMER - participant node ({}), partition ({}) finished reading file ({}) with status {}, but adapter already shutdown",
        context.nodeId, context.partitionId.toString, fileHandler.getFullPath, status.toString)
      statusToSendToLeader = File_Processing_Status_Interrupted
    }
    else if (status == SmartFileConsumer.FILE_STATUS_FINISHED) {
      if (LOG.isInfoEnabled) LOG.info("SMART FILE CONSUMER - participant node ({}), partition ({}) finished reading file ({})",
        context.nodeId, context.partitionId.toString, fileHandler.getFullPath)
      statusToSendToLeader = File_Processing_Status_Finished
    }
    else if (status == SmartFileConsumer.FILE_STATUS_ProcessingInterrupted) {
      if (LOG.isInfoEnabled) LOG.info("SMART FILE CONSUMER - participant node ({}), partition ({}) interrupted while reading file ({})",
        context.nodeId, context.partitionId.toString, fileHandler.getFullPath)
      statusToSendToLeader = File_Processing_Status_Interrupted
    }
    else if (status == SmartFileConsumer.FILE_STATUS_CORRUPT) {
      LOG.error("SMART FILE CONSUMER - participant node ({}), partition ({}) file ({}) is corrupt",
        context.nodeId, context.partitionId.toString, fileHandler.getFullPath)
      statusToSendToLeader = File_Processing_Status_Corrupted
    }
    else {
      if (LOG.isWarnEnabled) LOG.warn("SMART FILE CONSUMER - participant node ({}), partition ({}) reports file not found ({})",
        context.nodeId, context.partitionId.toString, fileHandler.getFullPath)
      statusToSendToLeader = File_Processing_Status_NotFound
    }

    //set file status as finished
    val pathKey = fileProcessingPath + "/" + context.nodeId + "/" + context.partitionId
    val data = createProcessingItemWithStatusJsonFromGroup(context.nodeId.toInt, context.partitionId.toInt, fileHandlers.map(fh => fh.getFullPath()).toList, statusToSendToLeader)
    envContext.setListenerCacheKey(pathKey, data)

    //send a new file request to leader
    if (!isShutdown) {
      //shutdown will clear all queues
      val requestData = smartFileFromLeaderPath + "/" + context.nodeId + "/" + context.partitionId //listen to this SmartFileCommunication/FromLeader/<NodeId>/<partitionId id>
      val requestPathKey = requestFilePath + "/" + context.nodeId + "/" + context.partitionId
      if (LOG.isInfoEnabled) LOG.info("SMART FILE CONSUMER - participant ({}) - sending a file request to leader on partition ({}) after finishing file {}",
        context.nodeId, context.partitionId.toString, fileHandler.getFullPath)
      if (LOG.isDebugEnabled) LOG.debug("SMART FILE CONSUMER - sending the request using path ({}) using value ({}) ", requestPathKey, requestData)
      envContext.setListenerCacheKey(requestPathKey, requestData);
    }
    //send status
    if (logger.isInfoEnabled) logger.info("sending stat msg")
    val statMsgStr = adapterConfig.statusMsgTypeName //"com.ligadata.messages.InputAdapterStatsMsg"
    if (statMsgStr != null && statMsgStr.trim.size > 0) {
      try {
        val statMsg = envContext.getContainerInstance(statMsgStr)
        if (statMsg != null) {
          // Set all my values
          statMsg.set("msgtype", "FileInputAdapterStatusMsg")
          statMsg.set("source", adapterConfig._type)
          statMsg.set("filename", stats.fileName)
          statMsg.set("recordscount", stats.recordsCount)
          statMsg.set("starttime", stats.startTs)
          statMsg.set("endtime", stats.endTs)
          statMsg.set("bytesRead", stats.bytesRead)
          statMsg.set("nodeId", stats.nodeId)
          statMsg.set("status", stats.status)

          // Post the messgae
          envContext.postMessages(Array[ContainerInterface](statMsg))
        }
        else {
          logger.error("Msg {} is not found in metadata", statMsgStr)
        }
      }
      catch {
        case e: Exception => {
          logger.error("", e)
        }
      }
    }

  }

  //what a participant should do parallelism value changes
  def filesParallelismCallback(eventType: String, eventPath: String, eventPathData: String): Unit = {
    //data is comma separated partition ids
    val currentNodePartitions = eventPathData.split(",").map(idStr => idStr.toInt).toList

    //val newFilesParallelism = eventPathData.toInt

    val nodeId = clusterStatus.nodeId

    if (LOG.isInfoEnabled) LOG.info("Smart File Consumer - Node Id = {}, files parallelism changed. partitions to handle are {}", nodeId, eventPathData)
    if (LOG.isInfoEnabled) LOG.info("Smart File Consumer - Old File Parallelism is {}", filesParallelism.toString)

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

    if (LOG.isDebugEnabled) LOG.debug("filesParallelism:" + filesParallelism + ", prevRegParticipantPartitions:" + prevRegParticipantPartitions.mkString(","))

    //create the threads only if no threads created yet or number of threads changed
    if (parallelismStatus == "Uninitialized" || parallelismStatus == "Changed") {
      if (LOG.isInfoEnabled) LOG.info("SMART FILE CONSUMER - participant ({}) - creating {} thread(s) to handle partitions ({})",
        nodeId, filesParallelism.toString, eventPathData)
      participantExecutor = Executors.newFixedThreadPool(filesParallelism)
      currentNodePartitions.foreach(partitionId => {

        val executorThread = new Runnable() {
          private var partitionId: Int = _

          def init(id: Int) = partitionId = id

          override def run(): Unit = {

            val actualThreadId = Thread.currentThread().getThreadGroup.getName + ">" + Thread.currentThread().getId
            if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER - running thread with partitionId={}, actualThreadId={}",
              partitionId.toString, actualThreadId)

            val fileProcessingAssignementKeyPath = smartFileFromLeaderPath + "/" + nodeId + "/" + partitionId //listen to this SmartFileCommunication/FromLeader/<NodeId>/<partitionId id>
            //listen to file assignment from leader
            assignedFileProcessingAssignementKeyPaths += fileProcessingAssignementKeyPath
            envContext.createListenerForCacheKey(fileProcessingAssignementKeyPath, fileAssignmentFromLeaderCallback) //e.g.   SmartFileCommunication/FromLeader/RequestFile/<nodeid>/<partitionId id>

            //send a file request to leader
            val fileRequestKeyPath = requestFilePath + "/" + nodeId + "/" + partitionId
            if (LOG.isDebugEnabled) {
              LOG.debug("SMART FILE CONSUMER - participant ({}) - sending a file request to leader (initial) on partition ({}) -- sending the request using path ({}) using value ({})", nodeId, partitionId.toString, fileRequestKeyPath, fileProcessingAssignementKeyPath)
            } else if (LOG.isInfoEnabled) {
              LOG.info("SMART FILE CONSUMER - participant ({}) - sending a file request to leader (initial) on partition ({})", nodeId, partitionId.toString)
            }
            envContext.setListenerCacheKey(fileRequestKeyPath, fileProcessingAssignementKeyPath)
          }
        }
        executorThread.init(partitionId)
        if (LOG.isDebugEnabled) LOG.debug("added req for partitionId:" + partitionId)
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
      val parentDir = smartFileHandler.getParentDir
      val locationInfo = getDirLocationInfo(parentDir)
      if (adapterConfig.archiveConfig != null && adapterConfig.archiveConfig.outputConfig != null) {
        //val locationInfo = locationsMap(parentDir)
        if (locationInfo != null && locationInfo.fileComponents != null)
          componentsMap = MonitorUtils.getFileComponents(originalFilePath, locationInfo)

        val (targetMoveDir1, flBaseName1) = getTargetFile(smartFileHandler)
        targetMoveDir = targetMoveDir1
        flBaseName = flBaseName1
      }

      isFileMoved = moveFile(smartFileHandler)
      if (!isShutdown && archiver != null && isFileMoved && adapterConfig.archiveConfig != null &&
        adapterConfig.archiveConfig.outputConfig != null) {
        archiver.addArchiveFileInfo(ArchiveFileInfo(adapterConfig, locationInfo, targetMoveDir, flBaseName, componentsMap, 0))
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
    val originalFilePath = fileHandler.getSimplifiedFullPath

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

      if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER Moving File" + originalFilePath + " to " + targetMoveDir)
      //if (fileHandler.exists()) {
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
        if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER - Target dir not found and could not be created:" + targetMoveDir)
        false
      }
      /*} else {
        if (LOG.isWarnEnabled) LOG.warn("SMART FILE CONSUMER File has been deleted " + originalFilePath)
        true
      }*/
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
      if (LOG.isDebugEnabled) LOG.debug("Deserializing Key:" + k)
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
        if (LOG.isDebugEnabled) LOG.debug("Deserializing Value:" + v)
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
    // Don't look for Shutdown variable. It may go wrong if we start looking at it in case of StopProcessing & StartProcessing
    getKeyValuePairs()
  }

  override def getAllPartitionEndValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = lock.synchronized {
    // Don't look for Shutdown variable. It may go wrong if we start looking at it in case of StopProcessing & StartProcessing
    getKeyValuePairs()
  }

  private def getKeyValuePairs(): Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = {
    // Don't look for Shutdown variable. It may go wrong if we start looking at it in case of StopProcessing & StartProcessing
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
    // Don't look for Shutdown variable. It may go wrong if we start looking at it in case of StopProcessing & StartProcessing
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

  def sleepMs(sleepTimeInMs: Int): Boolean = {
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
    if (MonitorUtils.adaptersChannels.contains(adapterConfig.Name)) {
      MonitorUtils.adaptersChannels.clear()
    }
    if (MonitorUtils.adaptersSessions.contains(adapterConfig.Name)) {
      MonitorUtils.adaptersSessions.clear()
    }

    if (participantsFilesAssignmentExecutor != null)
      participantsFilesAssignmentExecutor.shutdownNow()
    participantsFilesAssignmentExecutor = null

    isShutdown = false
    _leaderCallbackRequests.clear
    _fileAssignmentsCallbackRequests.clear

    participantsFilesAssignmentExecutor = Executors.newFixedThreadPool(1)

    val fileAssignmentFromLeaderThread = new Runnable() {
      val exec = participantsFilesAssignmentExecutor

      override def run(): Unit = {
        while (!isShutdown && exec != null && !exec.isShutdown && !exec.isTerminated) {
          try {
            val requests = getFileAssignmentsCallbackRequestsAndClear
            var i = 0
            while (i < requests.size && !isShutdown && exec != null && !exec.isShutdown && !exec.isTerminated) {
              val req = requests(i)
              i += 1
              fileAssignmentFromLeaderFn(req.eventType, req.eventPath, req.eventPathData)
            }
            // Wait only if we don't have any requests to process
            if (requests.size == 0 && !isShutdown && exec != null && !exec.isShutdown && !exec.isTerminated) {
              try {
                Thread.sleep(5)
              } catch {
                case e: Throwable => {
                }
              }
            }
          }
          catch {
            case ie: InterruptedException => {
              if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - interrupted " + ie)
            }
            case e: Throwable => {
              LOG.error("Smart File Consumer - unkown exception ", e)
            }
          }

        }
      }
    }

    participantsFilesAssignmentExecutor.execute(fileAssignmentFromLeaderThread)

    _ignoreFirstMsg = ignoreFirstMsg
    var lastHb: Long = 0
    startHeartBeat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))

    if (LOG.isWarnEnabled) LOG.warn("SMART_FILE_ADAPTER {} - START_PROCESSING CALLED", adapterConfig.Name)

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

    if (adapterConfig.archiveConfig != null && adapterConfig.archiveConfig.outputConfig != null && IsLeaderNode) {
      val archiveParallelism = if (adapterConfig.archiveConfig.archiveParallelism <= 0) 1 else adapterConfig.archiveConfig.archiveParallelism
      val archiveSleepTimeInMs = if (adapterConfig.archiveConfig.archiveSleepTimeInMs < 0) 1 else adapterConfig.archiveConfig.archiveSleepTimeInMs
      if (logger.isInfoEnabled) logger.info("Archival Init. archiveParallelism:" + archiveParallelism + ", archiveSleepTimeInMs:" + archiveSleepTimeInMs)
      archiveExecutor = Executors.newFixedThreadPool(archiveParallelism)

      val maxArchiveAttemptsCount = 3
      if (archiver != null && adapterConfig.archiveConfig != null) {
        if (logger.isWarnEnabled) logger.warn("adapter {} is starting archiver", adapterConfig.Name)
        archiver.startArchiving()
      }
    }

    // BUGBUG:: For now we are handling only non entire file as one msg
    val myPartitionInfo =
    if (adapterConfig.monitoringConfig.entireFileAsOneMessage) {
      val nullFlName: String = "" // or do we need to take null
      partitionIds.map(pid => (pid._key.asInstanceOf[SmartFilePartitionUniqueRecordKey].PartitionId,
        nullFlName, -1, ignoreFirstMsg)).mkString("~")
    } else {
      // BUGBUG:: For now we are handling single file here. Groups does not handle here
      //(1,file1,0,true)~(2,file2,0,true)~(3,file3,1000,true)
      partitionIds.map(pid => (pid._key.asInstanceOf[SmartFilePartitionUniqueRecordKey].PartitionId,
        pid._val.asInstanceOf[SmartFilePartitionUniqueRecordValue].FileName,
        pid._val.asInstanceOf[SmartFilePartitionUniqueRecordValue].Offset, ignoreFirstMsg)).mkString("~")
    }

    if (LOG.isDebugEnabled) LOG.debug("partitionInfo:" + myPartitionInfo)

    val SendStartInfoToLeaderPath = sendStartInfoToLeaderParentPath + "/" + clusterStatus.nodeId // Should be different for each Nodes
    if (LOG.isWarnEnabled) LOG.warn("Smart File Consumer - Node {} is sending start info to leader. path is {}, value is {} ",
      clusterStatus.nodeId, SendStartInfoToLeaderPath, myPartitionInfo)
    envContext.setListenerCacheKey(SendStartInfoToLeaderPath, myPartitionInfo) // => Goes to Leader

    if (IsLeaderNode)
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
    if (message == null)
      return


    // Create a new EngineMessage and call the engine.
    if (smartFileConsumerContext.execThread == null) {
      smartFileConsumerContext.execThread = execCtxtObj.CreateExecContext(input, uniqueKey, nodeContext)
    }

    incrementCountForPartition(partitionId)

    uniqueVal.Offset = offset
    uniqueVal.FileName = fileName
    //val dontSendOutputToOutputAdap = uniqueVal.Offset <= uniqueRecordValue

    if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - Node {} is sending a msg to engine. partition id= {}. msg={}. file={}. offset={}. uniqueKey={}, uniqueVal={}, smartFileConsumerContext.execThread={},smartFileConsumerContext={}",
      smartFileConsumerContext.nodeId, smartFileConsumerContext.partitionId.toString, new String(message), fileName, offset.toString, uniqueKey, uniqueVal, smartFileConsumerContext.execThread, smartFileConsumerContext)
    msgCount.incrementAndGet()
    smartFileConsumerContext.execThread.execute(message, uniqueKey, uniqueVal, readTmMs)

  }


  /**
    * add tags if needed
    */
  def getFinalMsg(smartMessage: SmartFileMessage): Array[Byte] = {

    val parentDirLocationConfig = getFileLocationConfig(smartMessage.relatedFileHandler)
    if (parentDirLocationConfig == null) {
      logger.error("Adapter {} - file {} has no entry in adapter config location section. ignoring msg",
        adapterConfig.Name, smartMessage.relatedFileHandler.getFullPath)
      return null
    }
    if (parentDirLocationConfig.msgTagsKV == null || parentDirLocationConfig.msgTagsKV.size == 0)
      return smartMessage.msg

    val msgStr = new String(smartMessage.msg)

    val tagDelimiter = parentDirLocationConfig.tagDelimiter
    val fileName = MonitorUtils.getFileName(smartMessage.relatedFileHandler.getFullPath)
    val parentDirName = MonitorUtils.getFileName(smartMessage.relatedFileHandler.getParentDir)

    val prefix = parentDirLocationConfig.msgTagsKV.foldLeft("")((pre, tagTuple) => {
      val tagValue =
        if (tagTuple._2.startsWith("$")) {
          //predefined tags
          tagTuple._2 match {
            //assuming msg type is defined by parent folder name
            case "$Dir_Name" | "$DirName" => parentDirName
            case "$File_Name" | "$FileName" => fileName
            case "$File_Full_Path" | "$FileFullPath" => smartMessage.relatedFileHandler.getFullPath
            case "$Line_Number" | "$LineNumber" => smartMessage.msgNumber.toString
            case "$Msg_Number" | "$MsgNumber" => smartMessage.msgNumber.toString
            case "$Msg_Start_Offset" | "$MsgStartOffset" => smartMessage.msgStartOffset.toString
            case _ => ""
          }
        }
        else {
          //if not predefined just add it as is
          tagTuple._2
        }
      //send tags as key:value
      pre + tagTuple._1 + ":" + tagValue + tagDelimiter
    })

    val finalMsg = prefix + msgStr
    finalMsg.getBytes
  }

  override def StopProcessing: Unit = {
    if (LOG.isWarnEnabled) LOG.warn("shutting down adapter - {}", adapterConfig.Name)

    initialized = false
    isShutdown = true

    if (participantsFilesAssignmentExecutor != null)
      participantsFilesAssignmentExecutor.shutdownNow()
    participantsFilesAssignmentExecutor = null

    _leaderCallbackRequests.clear
    _fileAssignmentsCallbackRequests.clear

    /*if (archiveExecutor != null)
      archiveExecutor.shutdownNow()
    archiveExecutor = null
    archiveInfo.clear()*/

    if (leaderExecutor != null) {
      if (LOG.isWarnEnabled) LOG.warn("shutting down adapter - {} . stopping leaderExecutor", adapterConfig.Name)
      keepCheckingStatus = false
      MonitorUtils.shutdownAndAwaitTermination(leaderExecutor, "Leader executor")
      leaderExecutor = null
    }

    // Unregister
    // For now we have uniq register key value for this. So, we are find with unregistering for a path
    envContext.removeListenerForCacheChildern(sendStartInfoToLeaderParentPath) // listen to start info
    envContext.removeListenerForCacheChildern(requestFilePath) // listen to file requests
    envContext.removeListenerForCacheChildern(fileProcessingPath) // listen to file processing status

    assignedFileProcessingAssignementKeyPaths.foreach(fileProcessingAssignementKeyPath => {
      envContext.removeListenerForCacheKey(fileProcessingAssignementKeyPath)
    })

    assignedFileProcessingAssignementKeyPaths.clear()

    clearProcessingQueue()
    clearRequestQueue()

    if (monitorController != null) {
      if (LOG.isWarnEnabled) LOG.warn("shutting down adapter - {} . stopping monitorController", adapterConfig.Name)
      monitorController.stopMonitoring
      monitorController = null
    }

    if (LOG.isWarnEnabled) LOG.warn("shutting down adapter - {} . stopping terminateReaderTasks", adapterConfig.Name)
    terminateReaderTasks

    if (archiver != null) {
      if (LOG.isWarnEnabled) LOG.warn("shutting down adapter - {} . stopping archiver", adapterConfig.Name)
      archiver.shutdown()
      archiver = null
    }

    if (LOG.isWarnEnabled) LOG.warn("shutting down adapter - {} . clearing cache", adapterConfig.Name)
    clearCache

    prevRegParticipantPartitions = List()
    prevRegLeader = ""
    filesParallelism = -1

    if (LOG.isWarnEnabled) LOG.warn("finished shutting down adapter - {}", adapterConfig.Name)

    /*if(MonitorUtils.adaptersChannels.contains(adapterConfig.Name)){
      MonitorUtils.adaptersChannels(adapterConfig.Name).foreach(tuple => {
        logger.error("adapter {} still have channel with hashcode {} and call stack {}",
          adapterConfig.Name, tuple._1.toString, tuple._2)
      })
    }
    if(MonitorUtils.adaptersSessions.contains(adapterConfig.Name)){
      MonitorUtils.adaptersSessions(adapterConfig.Name).foreach(tuple => {
        logger.error("adapter {} still have session with hashcode {} and call stack {}",
          adapterConfig.Name, tuple._1.toString, tuple._2)
      })
    }*/
  }

  private def clearCache(): Unit = {
    //TODO:- make sure to Clear listeners & queues all stuff related to leader or non-leader. So, Next SartProcessing should start the whole stuff again.
    if (prevRegParticipantPartitions != null) {
      prevRegParticipantPartitions.foreach(partitionId => {

        //clear file processing listener path
        val fileProcessListenerPathKey = fileProcessingPath + "/" + clusterStatus.nodeId + "/" + partitionId
        val emptyProcessingData = "|"
        if (LOG.isWarnEnabled) LOG.warn("clearing process cache key {}", fileProcessListenerPathKey)
        envContext.setListenerCacheKey(fileProcessListenerPathKey, emptyProcessingData)

        //clear status check cache
        val cacheKey = Status_Check_Cache_KeyParent + "/" + clusterStatus.nodeId + "/" + partitionId
        envContext.saveConfigInClusterCache(cacheKey, "".getBytes())
      })
    }

    //clear start info path
    val SendStartInfoToLeaderPath = sendStartInfoToLeaderParentPath + "/" + clusterStatus.nodeId
    envContext.setListenerCacheKey(SendStartInfoToLeaderPath, "")

    if (allNodesStartInfo != null)
      allNodesStartInfo.clear()

    if (clusterStatus != null) {
      if (IsLeaderNode) {
        if (LOG.isDebugEnabled) LOG.debug("Smart File Consumer - Cleaning queues and cache stuff by leader")
        clearProcessingQueue()
        clearRequestQueue()
      }
      else {
        //clean participant-related stuff

      }
    }
  }

  private def terminateReaderTasks(): Unit = {

    // Tell all thread to stop processing on the next interval, and shutdown the Excecutor.
    quiesce

    // Give the threads to gracefully stop their reading cycles, and then execute them with extreme prejudice.
    //Thread.sleep(adapterConfig.monitoringConfig.waitingTimeMS)

    if (participantExecutor != null)
      MonitorUtils.shutdownAndAwaitTermination(participantExecutor, "Participant Executor")

    if (LOG.isDebugEnabled) LOG.debug("Smart File Adapter - Shutdown Complete")
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
        case e: Exception => { if (LOG.isWarnEnabled) LOG.warn("SMART-FILE-ADAPTER: error trying to determine depths for smart file input adapter "+adapterConfig.Name,e) }
      }
    })*/

    return new MonitorComponentInfo(AdapterConfiguration.TYPE_INPUT, adapterConfig.Name, SmartFileConsumer.ADAPTER_DESCRIPTION,
      startHeartBeat, lastSeen, Serialization.write(metrics).toString)
  }

  override def getComponentSimpleStats: String = {
    return "Input/" + adapterConfig.Name + "/evtCnt" + "->" + msgCount.get()
  }

  private def incrementCountForPartition(pid: Int): Unit = {
    val cVal: Long = partitonCounts.getOrElse(pid.toString, 0)
    partitonCounts(pid.toString) = cVal + 1
  }

  def getDirLocationInfo(srcDir: String): LocationInfo = {
    if (logger.isDebugEnabled) logger.debug("getDirLocationInfo for file " + srcDir)
    //if (logger.isDebugEnabled) logger.debug(locationsMap)

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
    if (logger.isDebugEnabled) logger.debug("getFileLocationConfig for file " + fileHandler.getFullPath)
    val parentDir = fileHandler.getParentDir
    getDirLocationInfo(parentDir)

  }


  /* Get Velocity Metrics for Output Adapter   */
  // private def getSmartFileVelocityMetrics(VMFactory: VelocityMetricsFactoryInterface, componentName: String, fileName: String, nodeId: String, adapConfig: AdapterConfiguration) = {
  //   var vm = new VelocityMetricsInfo
  //   vm.incrementFileVelocityMetrics(VMFactory, componentName, fileName, nodeId, adapConfig)
  // }

  /* Get Velocity Metrics for Output Adapter   */
  private def getSmartFileVelocityMetrics(input: InputAdapter, fileName: String, processed: Boolean) = {
    val vmInstances = input.getVelocityInstances
    if (vmInstances != null && vmInstances.length > 0) {
      for (i <- 0 until vmInstances.length) {
        input.vm.incrementFileVMetrics(vmInstances(i), fileName, processed)
      }
    }
  }
}
