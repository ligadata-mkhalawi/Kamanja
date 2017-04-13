package com.ligadata.filedataprocessor

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Executors, TimeUnit}
import java.util.zip.{GZIPInputStream, ZipException}

import com.ligadata.Exceptions.{MissingPropertyException, StackTrace}
import com.ligadata.MetadataAPI.MetadataAPIImpl
import com.ligadata.ZooKeeper.CreateClient
import com.ligadata.kamanja.metadata.MessageDef
import org.apache.curator.framework.CuratorFramework
import org.apache.logging.log4j.{LogManager, Logger}
import org.json4s.DefaultFormats
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.JsonMethods._
import com.ligadata.kamanja.metadata.MdMgr._

import scala.collection.mutable.HashMap
import scala.collection.JavaConverters._
import util.control.Breaks._
import java.io._
import java.nio.file._

import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.PriorityQueue
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.Files.copy
import java.nio.file.Paths.get

import scala.collection.mutable.ArrayBuffer
import org.apache.commons.lang3.RandomStringUtils
import java.net.URLEncoder

import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.Metadata
import org.apache.tika.detect.Detector
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.mime.MimeTypes

import scala.collection.mutable.Map
import java.net.URLDecoder
import java.util.Calendar

import org.apache.tika.Tika
import net.sf.jmimemagic.Magic
import net.sf.jmimemagic.MagicMatch
import net.sf.jmimemagic.MagicParseException
import net.sf.jmimemagic.MagicMatchNotFoundException
import net.sf.jmimemagic.MagicException
import com.ligadata.ZooKeeper.ProcessComponentByWeight;

case class BufferLeftoversArea(workerNumber: Int, leftovers: Array[Char], relatedChunk: Int)

case class BufferToChunk(len: Int, payload: Array[Char], chunkNumber: Int, relatedFileName: String, firstValidOffset: Int, isEof: Boolean, partMap: scala.collection.mutable.Map[Int, Int])

case class KafkaMessage(msg: Array[Char], offsetInFile: Int, isLast: Boolean, isLastDummy: Boolean, relatedFileName: String, partMap: scala.collection.mutable.Map[Int, Int], msgOffset: Long)

case class EnqueuedFile(name: String, offset: Int, createDate: Long, partMap: scala.collection.mutable.Map[Int, Int])

case class FileStatus(status: Int, offset: Long, createDate: Long)

case class OffsetValue(lastGoodOffset: Int, partitionOffsets: Map[Int, Int])


/**
  * This is the global area for the File Processor.  It basically handles File Access and Distribution !!!!!
  * the Individual File Processors ask here for what files it they should be processed.
  */
object FileProcessor {
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  private var initRecoveryLock = new Object
  private var numberOfReadyConsumers = 0
  private var isBufferMonitorRunning = false
  private var props: scala.collection.mutable.Map[String, String] = null
  var zkc: CuratorFramework = null
  var znodePath: String = ""
  var znodePath_Status: String = ""
  var localMetadataConfig: String = ""
  var zkcConnectString: String = ""

  //private var path: Path = null
  //Create multiple path watcher
  private var path: ArrayBuffer[Path] = null
  private val contentTypes = new scala.collection.mutable.HashMap[String, String]

  var dirToWatch: String = _
  var targetMoveDir: String = _
  var readyToProcessKey: String = _

  var globalFileMonitorService: ExecutorService = scala.actors.threadpool.Executors.newFixedThreadPool(3)
  val DEBUG_MAIN_CONSUMER_THREAD_ACTION = 1000
  val NOT_RECOVERY_SITUATION = -1
  val BROKEN_FILE = -100
  val CORRUPT_FILE = -200
  val REFRESH_RATE = 2000
  val MAX_WAIT_TIME = 60000
  var errorWaitTime = 1000
  val MAX_ZK_RETRY_MS = 5000
  val MAX_RETRY = 10
  val RECOVERY_SLEEP_TIME = 1000
  var WARNING_THRESHTHOLD: Long = 5000 * 1000000L
  var FILE_Q_FULL_CONDITION = 300

  var reset_watcher = false

  val KAFKA_SEND_SUCCESS = 0
  val KAFKA_SEND_Q_FULL = 1
  val KAFKA_SEND_DEAD_PRODUCER = 2
  val RECOVERY_DUMMY_START_TIME = 100

  // Possible statuses of files being processed
  val ACTIVE = 0
  val MISSING = 1
  val IN_PROCESS_FAILED = 2
  val FINISHED_FAILED_TO_COPY = 3
  val BUFFERING_FAILED = 4
  var bufferTimeout: Int = 300000
  // Default to 5 minutes
  var maxTimeFileAllowedToLive: Int = 3000
  // default to 50 minutes.. will be multiplied by 1000 later
  var refreshRate: Int = 2000
  //Refresh rate for monitorBufferingFiles and runFileWatcher methods
  var maxBufferErrors = 5

  val HEALTHCHECK_TIMEOUT = 30000
  val maxRetry = 3

  private var fileCache: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map[String, String]()
  private var fileCacheLock = new Object
  // Files in Progress! and their statuses.
  //   Files can be in the following states:
  //    ACTIVE_PROCESSING, BUFFERING, FAILED_TO_MOVE, FAILED_TO_FINISH...
  private var activeFiles: scala.collection.mutable.Map[String, FileStatus] = scala.collection.mutable.Map[String, FileStatus]()
  private val activeFilesLock = new Object
  // READY TO PROCESS FILE QUEUES
  var fileQ: scala.collection.mutable.PriorityQueue[EnqueuedFile] = new scala.collection.mutable.PriorityQueue[EnqueuedFile]()(Ordering.by(OldestFile))
  val fileQLock = new Object
  private val bufferingQ_map: scala.collection.mutable.Map[String, (Long, Long, Int)] = scala.collection.mutable.Map[String, (Long, Long, Int)]()
  private val bufferingQLock = new Object
  private val zkRecoveryLock = new Object
  private var maxFormatValidationArea = 1048576
  private var status_interval = 0
  // default to a minute
  private var randomFailureThreshHold = 0

  val scheduledThreadPool = java.util.concurrent.Executors.newScheduledThreadPool(1)

  val testRand = scala.util.Random
  private var isMontoringDirectories = true

  var prevIsThisNodeToProcess = false;
  var pcbw: ProcessComponentByWeight = null;
  var isLockAcquired = false;
  var isThisNodeReadyToProcess = false

  private var fileAge: String = ""
  private var fileCount: Int = 0
  private var fileAgeInt: Int = 0
  private val ONE_SECOND_IN_MILLIS = 1000
  private val ONE_MINUTE_IN_MILLIS = 60000
  private val ONE_HOUR_IN_MILLIS = 3600000

  private def testFailure(thisCause: String) = {
    var bigCheck = FileProcessor.testRand.nextInt(100)
    if (FileProcessor.randomFailureThreshHold > bigCheck) throw new Exception(thisCause + " (" + bigCheck + "/" + FileProcessor.randomFailureThreshHold + ")")
  }

  def shutdownInputWatchers: Unit = {
    isMontoringDirectories = false
  }

  def executeCallWithElapsed[A](f: => A, reasonToTrace: String): (A) = {
    val startTS: Long = System.nanoTime()
    if (logger.isTraceEnabled) logger.trace(reasonToTrace + " start call: " + startTS)

    val ret = f

    val endTS: Long = System.nanoTime()

    if (logger.isTraceEnabled) logger.trace(reasonToTrace + " end call: " + endTS)
    if ((endTS - startTS) > WARNING_THRESHTHOLD && logger.isWarnEnabled) logger.warn("DELAY WARNING: " + reasonToTrace + " is taking(" + (endTS - startTS) / 1000000.0 + "ms) greater then " + FileProcessor.WARNING_THRESHTHOLD + "ns")
    if (logger.isDebugEnabled)
      logger.debug("Reason:" + reasonToTrace + ". Timetaken:" + (endTS - startTS) / 1000000.0 + " ms")
    else if (logger.isInfoEnabled && ((endTS - startTS) / 1000000) > 10)
      logger.info("Reason:" + reasonToTrace + ". Timetaken:" + (endTS - startTS) / 1000000.0 + " ms. Taken more than 10ms")
    ret

  }

  /**
    *
    */
  def initZookeeper = {
    try {
      zkcConnectString = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("ZOOKEEPER_CONNECT_STRING")
      if (logger.isInfoEnabled) logger.info("SMART_FILE_CONSUMER (global) Using zookeeper " + zkcConnectString)
      znodePath = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("ZNODE_PATH") + "/smartFileConsumer"
      znodePath_Status = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("ZNODE_PATH") + "/smartFileConsumer_status"
      createNode(zkcConnectString, znodePath) // CreateClient.CreateNodeIfNotExists(zkcConnectString, znodePath)
      createNode(zkcConnectString, znodePath_Status)
      zkc = getZkc(zkcConnectString) // CreateClient.createSimple(zkcConnectString)
    } catch {
      case e: Exception => {
        logger.error("SMART FILE CONSUMER (global): unable to connect to zookeeper using " + zkcConnectString, e)
        throw new Exception("Failed to start a zookeeper session with(" + zkcConnectString + "): " + e.getMessage(), e)
      }
      case e: Throwable => {
        logger.error("SMART FILE CONSUMER (global): unable to connect to zookeeper using " + zkcConnectString, e)
        throw new Exception("Failed to start a zookeeper session with(" + zkcConnectString + "): " + e.getMessage(), e)
      }
    }
  }

  //
  def addToZK(fileName: String, offset: Int, partitions: scala.collection.mutable.Map[Int, Int] = null): Unit = {
    var zkValue: String = ""
    if (logger.isInfoEnabled) logger.info("SMART_FILE_CONSUMER (global): Getting zookeeper info for " + znodePath)

    if (logger.isInfoEnabled) logger.info("SMART_FILE_CONSUMER (MI): addToZK " + fileName)
    createNode(zkcConnectString, znodePath + "/" + URLEncoder.encode(fileName, "UTF-8"))
    zkValue = zkValue + offset.toString

    // Set up Partition data
    if (partitions == null) {
      zkValue = zkValue + ",[]"
    } else {
      zkValue = zkValue + ",["
      var isFirst = true
      partitions.keySet.foreach(key => {
        if (!isFirst) zkValue = zkValue + ";"
        var mapVal = partitions(key)
        zkValue = zkValue + key.toString + ":" + mapVal.toString
        isFirst = false
      })
      zkValue = zkValue + "]"
    }
    setData(znodePath + "/" + URLEncoder.encode(fileName, "UTF-8"), zkValue.getBytes)
  }

  def removeFromZK(fileName: String): Unit = {
    try {
      if (logger.isInfoEnabled) logger.info("SMART_FILE_CONSUMER (global): Removing file " + fileName + " from zookeeper")
      deleteData(znodePath + "/" + URLEncoder.encode(fileName, "UTF-8"))
    } catch {
      case e: Exception => if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Failure", e)
      case e: Throwable => if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Failure", e)
    }
  }

  private def closeZKClient(): Unit = {
    zkRecoveryLock.synchronized {
      try {
        if (zkc != null) {
          executeCallWithElapsed(zkc.close, "Close ZK Connection")
        }
      } catch {
        case e: Throwable => {
          if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Failure closing ZKC.")
        }
      } finally {
        zkc = null
      }

    }
  }

  private def getZkc(zkcConnectString: String): CuratorFramework = {
    closeZKClient
    var client: CuratorFramework = null
    zkRecoveryLock.synchronized {
      var isSuccess = false
      while (!LocationWatcher.shutdown && !isSuccess) {
        try {
          client = executeCallWithElapsed(CreateClient.createSimple(zkcConnectString), "Create ZK Client Connection")
          isSuccess = true
          //return client
        } catch {
          case e: Throwable => {
            if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Failure creating a new zookeeper connection, retrying.", e)
            try {
              Thread.sleep(MAX_ZK_RETRY_MS)
            } catch {
              case e: InterruptedException => {
                if (!LocationWatcher.shutdown)
                  throw e
              }
            }
          }
        }
      }
    }
    return client
  }

  private def deleteData(zkPath: String): Unit = {
    var isSuccess = false
    while (!LocationWatcher.shutdown && !isSuccess) {
      try {
        if (doNodesExist(zkPath)) {
          executeCallWithElapsed(zkc.delete.forPath(zkPath), "Delete Data for path " + zkPath)
        }
        isSuccess = true
      } catch {
        case e: Throwable => {
          if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Failure deleting data from zookeeper, reinitializing connection and retrying.")
          try {
            Thread.sleep(MAX_ZK_RETRY_MS)
            zkc = getZkc(zkcConnectString)
          } catch {
            case e: InterruptedException => {
              if (!LocationWatcher.shutdown)
                throw e
            }
          }
        }
      }
    }
  }

  private def getData(zkPath: String): Array[Byte] = {
    var isSuccess = false
    var data: Array[Byte] = Array[Byte]()
    while (!LocationWatcher.shutdown && !isSuccess) {
      try {
        if (doNodesExist(zkPath)) {
          data = executeCallWithElapsed(zkc.getData.forPath(zkPath), "Get ZK Data for path " + zkPath)
        }
        isSuccess = true
      } catch {
        case e: Throwable => {
          if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Failure adding data to zookeeper reinitializing and retrying.")
          try {
            Thread.sleep(MAX_ZK_RETRY_MS)
            zkc = getZkc(zkcConnectString)
          } catch {
            case e: InterruptedException => {
              if (!LocationWatcher.shutdown)
                throw e
            }
          }
        }
      }
    }
    return data
  }

  private def setData(zkPath: String, data: Array[Byte]): Unit = {
    var isSuccess = false
    while (!LocationWatcher.shutdown && !isSuccess) {
      try {
        executeCallWithElapsed(zkc.setData().forPath(zkPath, data), "Set ZK Data for path" + zkPath)
        isSuccess = true
      } catch {
        case e: Throwable => {
          if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Failure adding data to zookeeper reinitializing and retrying.")
          try {
            Thread.sleep(MAX_ZK_RETRY_MS)
            zkc = getZkc(zkcConnectString)
          } catch {
            case e: InterruptedException => {
              if (!LocationWatcher.shutdown)
                throw e
            }
          }
        }
      }
    }
  }

  private def getNodes(zkPath: String): java.util.List[String] = {
    var data: java.util.List[String] = new java.util.ArrayList();
    var isSuccess = false
    while (!LocationWatcher.shutdown && !isSuccess) {
      try {
        if (doNodesExist(zkPath)) {
          data = executeCallWithElapsed(zkc.getChildren.forPath(zkPath), "Get ZK Children nodes for " + zkPath)
        }
        isSuccess = true
      } catch {
        case e: Throwable => {
          if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Failure adding data to zookeeper reinitializing and retrying.")
          try {
            Thread.sleep(MAX_ZK_RETRY_MS)
            zkc = getZkc(zkcConnectString)
          } catch {
            case e: InterruptedException => {
              if (!LocationWatcher.shutdown)
                throw e
            }
          }
        }
      }
    }
    return data
  }

  private def doNodesExist(zkPath: String): Boolean = {
    var isData: Boolean = true
    var isSuccess = false
    while (!LocationWatcher.shutdown && !isSuccess) {
      try {
        isData = true // just incae
        var retData = executeCallWithElapsed(zkc.checkExists().forPath(zkPath), "Check if ZK Node exists " + zkPath)
        if (retData == null) isData = false
        isSuccess = true
      } catch {
        case e: Throwable => {
          if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Failure adding data to zookeeper reinitializing and retrying.")
          try {
            Thread.sleep(MAX_ZK_RETRY_MS)
            zkc = getZkc(zkcConnectString)
          } catch {
            case e: InterruptedException => {
              if (!LocationWatcher.shutdown)
                throw e
            }
          }
        }
      }
    }
    return isData
  }

  private def createNode(zkConnectString: String, zkPath: String): Unit = {
    var isSuccess = false
    while (!LocationWatcher.shutdown && !isSuccess) {
      try {
        executeCallWithElapsed(CreateClient.CreateNodeIfNotExists(zkcConnectString, zkPath), "Creating a ZK Node " + zkPath)
        isSuccess = true
      } catch {
        case e: Throwable => {
          if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Failure creating a new node in zookeeper, retrying.")
          try {
            Thread.sleep(MAX_ZK_RETRY_MS)
          } catch {
            case e: InterruptedException => {
              if (!LocationWatcher.shutdown)
                throw e
            }
          }
        }
      }
    }
  }

  /**
    * checkIfFileBeingProcessed - if for some reason a file name is queued twice... this will prevent it
    *
    * @param file
    * @return
    */
  def checkIfFileBeingProcessed(file: String): Boolean = {
    fileCacheLock.synchronized {
      if (fileCache.contains(file)) {
        return true
      }
      else {
        fileCache(file) = scala.compat.Platform.currentTime + "::" + RandomStringUtils.randomAlphanumeric(10)
        return false
      }
    }
  }

  def fileCacheRemove(file: String): Unit = {
    fileCacheLock.synchronized {
      try {
        fileCache.remove(file)
      } catch {
        case e: Throwable => {
          logger.error("SMART FILE CONSUMER: failed to remove file " + file + " from cache files list")
        }
      }
    }
  }

  def bufferingQRemove(file: String): Unit = {
    bufferingQLock.synchronized {
      try {
        bufferingQ_map.remove(file)
      } catch {
        case e: Throwable => {
          logger.error("SMART FILE CONSUMER: failed to remove file " + file + " from bufferingQ files list")
        }
      }
    }
  }

  def getTimingFromFileCache(file: String): Long = {
    fileCacheLock.synchronized {
      fileCache(file).split("::")(0).toLong
    }
  }

  def getIDFromFileCache(file: String): String = {
    fileCacheLock.synchronized {
      fileCache(file)
    }
  }

  //def setProperties(inprops: scala.collection.mutable.Map[String, String], inPath: Path): Unit = {
  def setProperties(inprops: scala.collection.mutable.Map[String, String], inPath: ArrayBuffer[Path]): Unit = {
    props = inprops
    path = inPath
    dirToWatch = props.getOrElse(SmartFileAdapterConstants.DIRECTORY_TO_WATCH, null)
    targetMoveDir = props.getOrElse(SmartFileAdapterConstants.DIRECTORY_TO_MOVE_TO, null)
    readyToProcessKey = props.getOrElse(SmartFileAdapterConstants.READY_MESSAGE_MASK, ".gzip")
    maxTimeFileAllowedToLive = (1000 * props.getOrElse(SmartFileAdapterConstants.MAX_TIME_ALLOWED_TO_BUFFER, "3000").toInt)
    refreshRate = props.getOrElse(SmartFileAdapterConstants.REFRESH_RATE, "2000").toInt
    maxFormatValidationArea = props.getOrElse(SmartFileAdapterConstants.MAX_SIZE_FOR_FILE_CONTENT_VALIDATION, "1048576").toInt
    status_interval = props.getOrElse(SmartFileAdapterConstants.STATUS_QUEUES_FREQUENCY, "0").toInt
    randomFailureThreshHold = props.getOrElse(SmartFileAdapterConstants.TEST_FAILURE_THRESHOLD, "0").toInt
    fileAge = props.getOrElse(SmartFileAdapterConstants.FILE_AGE,"30m")
    fileAgeInt =
      if(fileAge.takeRight(1) == "s"){
        fileAge.substring(0, fileAge.length-1).toInt * ONE_SECOND_IN_MILLIS
      } else if(fileAge.takeRight(1) == "m"){
        fileAge.substring(0, fileAge.length-1).toInt * ONE_MINUTE_IN_MILLIS
      }  else if(fileAge.takeRight(1) == "h"){
        fileAge.substring(0, fileAge.length-1).toInt * ONE_HOUR_IN_MILLIS
      } else{
        30 * ONE_MINUTE_IN_MILLIS // default value for file age is 30 minute
      }

    fileCount = props.getOrElse(SmartFileAdapterConstants.FILE_COUNT,"100").toInt
  }

  def markFileProcessing(fileName: String, offset: Int, createDate: Long): Unit = {
    activeFilesLock.synchronized {
      if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (global): begin tracking the processing of a file " + fileName + "  begin at offset " + offset)
      var fs = new FileStatus(ACTIVE, offset, createDate)
      activeFiles(fileName) = fs
    }
  }

  def markFileProcessingEnd(fileName: String): Unit = {
    activeFilesLock.synchronized {
      if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER: stop tracking the processing of a file " + fileName)
      try {
        activeFiles.remove(fileName)
      } catch {
        case e: Throwable => {
          logger.error("SMART FILE CONSUMER: failed to remove file " + fileName + " from active files list")
        }
      }
    }
  }

  def setFileState(fileName: String, state: Int): Unit = {
    activeFilesLock.synchronized {
      if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (global): set file state of file " + fileName + " to " + state)
      if (!activeFiles.contains(fileName)) {
        if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): Trying to set state for an unknown file " + fileName)
      } else {
        val fs = activeFiles(fileName)
        val fs_new = new FileStatus(state, fs.offset, fs.createDate)
        activeFiles(fileName) = fs_new
      }
    }
  }


  // This only applies to the IN_PROCESS_FAILED
  def setFileOffset(fileName: String, offset: Long): Unit = {
    activeFilesLock.synchronized {
      if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (global): changing intermal offset of a file " + fileName + " to " + offset)
      if (!activeFiles.contains(fileName)) {
        if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): Trying to set state for an unknown file " + fileName)
      } else {
        val fs = activeFiles(fileName)
        val fs_new = new FileStatus(IN_PROCESS_FAILED, scala.math.max(fs.offset, offset), fs.createDate)
        activeFiles(fileName) = fs_new
      }
    }
  }

  def getFileStatus(fileName: String): FileStatus = {
    activeFilesLock.synchronized {
      if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (global): checking file in a list of active files " + fileName)
      if (!activeFiles.contains(fileName)) {
        if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): Trying to get status on unknown file " + fileName)
        return null
      }
      return activeFiles(fileName)
    }
  }


  // Stuff used by the File Priority Queue.
  def OldestFile(file: EnqueuedFile): Long = {
    file.createDate * -1
  }

  private def getFileQSize: Long = {
    fileQLock.synchronized {
      return fileQ.size
    }
  }

  def getFileInDirectory(dir: String, time: Long): Int ={
    val files = new File(dir)
    var size = 0
    if(files.exists() && files.isDirectory) {
      val filesList = files.listFiles.filter(_.isFile).toList
      for(file <- filesList){
        if (file.lastModified < time)
          size = size + 1
      }
      size
    } else {
      logger.warn("%s does not exists or it is not a directory".format(dir))
      0
    }
  }
  private def enQFile(file: String, offset: Int, createDate: Long, partMap: scala.collection.mutable.Map[Int, Int] = scala.collection.mutable.Map[Int, Int]()): Unit = {
    fileQLock.synchronized {
      logger.info("SMART FILE CONSUMER (global):  enq file " + file + " with priority " + createDate + " --- curretnly " + fileQ.size + " files on a QUEUE")
      if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (global):  enq file " + file + " with priority " + createDate + " --- curretnly " + fileQ.size + " files on a QUEUE")
      //////////////////////// new code from here
//      val processedPath = file.substring(0,file.lastIndexOf('/'))
//      val MovedPath = props.getOrElse(SmartFileAdapterConstants.DIRECTORY_TO_MOVE_TO, "")
//      if(processedPath.equals(MovedPath)){
//        val timeDiff = System.currentTimeMillis/*Calendar.getInstance().getTimeInMillis*/ - fileAgeInt
//        if (createDate > timeDiff){
//          if(getFileInDirectory(processedPath, timeDiff) <= fileCount){
//            fileQ += new EnqueuedFile(file, offset, createDate, partMap)
//          }
//        }
//      } else {
//        fileQ += new EnqueuedFile(file, offset, createDate, partMap)
//      }
      //////////////////////// to here
      fileQ += new EnqueuedFile(file, offset, createDate, partMap)
    }
  }

  private def deQFile: EnqueuedFile = {
    fileQLock.synchronized {
      if (fileQ.isEmpty) {
        return null
      }
      val ef = fileQ.dequeue()
      logger.info("SMART FILE CONSUMER (global):  deq file " + ef.name + " with priority " + ef.createDate + " --- curretnly " + fileQ.size + " files left on a QUEUE")
      if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (global):  deq file " + ef.name + " with priority " + ef.createDate + " --- curretnly " + fileQ.size + " files left on a QUEUE")
      return ef

    }
  }

  // Code that deals with Buffering Files Queue is below..
  def startGlobalFileMonitor: Unit = {
    // If already started by a different File Consumer - return
    if (isBufferMonitorRunning) return
    isBufferMonitorRunning = true

    if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (global): Initializing global queues")

    // Default to 5 minutes (value given in secopnds
    bufferTimeout = 1000 * props.getOrElse(SmartFileAdapterConstants.FILE_BUFFERING_TIMEOUT, "300").toInt
    localMetadataConfig = props(SmartFileAdapterConstants.METADATA_CONFIG_FILE)
    MetadataAPIImpl.InitMdMgrFromBootStrap(localMetadataConfig, false)
    initZookeeper

    isBufferMonitorRunning = true
    globalFileMonitorService.execute(new Runnable() {
      override def run() = {
        try {
          monitorBufferingFiles
        } catch {
          case e: Exception => {
            logger.error("Failure", e)
          }
          case e: Throwable => {
            logger.error("Failure", e)
          }
        }
      }
    })

    globalFileMonitorService.execute(new Runnable() {
      override def run() = {
        try {
          runFileWatcher
        } catch {
          case e: Exception => {
            logger.error("Failure", e)
          }
          case e: Throwable => {
            logger.error("Failure", e)
          }
        }
      }
    })

    globalFileMonitorService.execute(new Runnable() {
      override def run() = {
        try {
          monitorActiveFiles
        } catch {
          case e: Exception => {
            logger.error("Failure", e)
          }
          case e: Throwable => {
            logger.error("Failure", e)
          }
        }
      }
    })
    if (status_interval > 0) {
      scheduledThreadPool.scheduleWithFixedDelay(externalizeStats, 0, status_interval * 1000, TimeUnit.MILLISECONDS);
    }
  }

  def removeBufferedFilesAndEnqedFiles(): Unit = {
    fileCacheLock.synchronized {
      fileCache --= fileQ.map(v => v.name)
      fileCache --= bufferingQ_map.toArray.map(kv => kv._1)
    }
    bufferingQLock.synchronized {
      bufferingQ_map.clear
    }
    fileQLock.synchronized {
      fileQ.clear
    }
  }

  private def enQBufferedFile(file: String): Unit = {
    bufferingQLock.synchronized {
      bufferingQ_map(file) = (0L, System.currentTimeMillis(), 0) // Initially, always set to 0.. this way we will ensure that it has time to be processed
    }
  }

  /**
    * Look at the files on the DEFERRED QUEUE... if we see that it stops growing, then move the file onto the READY
    * to process QUEUE.
    */
  private def monitorBufferingFiles: Unit = {
    // This guys will keep track of when to exgernalize a WARNING Message.  Since this loop really runs every second,
    // we want to throttle the warning messages.
    var specialWarnCounter: Int = 1
    while (!LocationWatcher.shutdown) {
      try {
        // Scan all the files that we are buffering, if there is not difference in their file size.. move them onto
        // the FileQ, they are ready to process.
        if (FileProcessor.isThisNodeReadyToProcess) {
          bufferingQLock.synchronized {
            val iter = bufferingQ_map.iterator
            iter.foreach(fileTuple => {

              //TODO C&S - changes
              var thisFileFailures: Int = fileTuple._2._3
              var thisFileStarttime: Long = fileTuple._2._2
              var thisFileOrigLength: Long = fileTuple._2._1


              try {
                val file = new File(fileTuple._1)
                val d = executeCallWithElapsed(file, "Check file for buffering " + fileTuple._1)
                // If the filesystem is accessible
                if (d.exists) {
                  logger.info("FileProcessor: monitorBufferingFiles: Processing the file " + fileTuple._1)
                  //TODO C&S - Changes
                  thisFileOrigLength = d.length

                  // If file hasn't grown in the past 2 seconds - either a delay OR a completed transfer.
                  if (fileTuple._2._1 == thisFileOrigLength) {
                    // If the length is > 0, we assume that the file completed transfer... (very problematic), but unless
                    // told otherwise by BofA, not sure what else we can do here.
                    if (thisFileOrigLength > 0 && FileProcessor.isValidFile(fileTuple._1)) {
                      logger.info("SMART FILE CONSUMER (global):  File READY TO PROCESS " + d.toString)
                      if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (global):  File READY TO PROCESS " + d.toString)
                      ////////////////new code
                      val processedPath = fileTuple._1.substring(0,fileTuple._1.lastIndexOf('/'))
                      val MovedPath = props.getOrElse(SmartFileAdapterConstants.DIRECTORY_TO_MOVE_TO, "")
                      if(processedPath.equals(MovedPath)){
                        val timeDiff = System.currentTimeMillis/*Calendar.getInstance().getTimeInMillis*/ - fileAgeInt
                        if (d.lastModified < timeDiff){
                          if(getFileInDirectory(processedPath, timeDiff) <= fileCount){
                            enQFile(fileTuple._1, FileProcessor.NOT_RECOVERY_SITUATION, d.lastModified)
                            bufferingQRemove(fileTuple._1)
                            file.setLastModified(Calendar.getInstance().getTimeInMillis);
                          }
                        }
                      } else {
                        enQFile(fileTuple._1, FileProcessor.NOT_RECOVERY_SITUATION, d.lastModified)
                        bufferingQRemove(fileTuple._1)
                      }
                      /////////////// to here
//                      enQFile(fileTuple._1, FileProcessor.NOT_RECOVERY_SITUATION, d.lastModified)
//                      bufferingQRemove(fileTuple._1)
                    } else {
                      // Here becayse either the file is sitll of len 0,or its deemed to be invalid.
                      if (thisFileOrigLength == 0) {
                        val diff = System.currentTimeMillis - thisFileStarttime //d.lastModified
                        if (diff > bufferTimeout) {
                          if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): Detected that " + d.toString + " has been on the buffering queue longer then " + bufferTimeout / 1000 + " seconds - Cleaning up")
                          moveFile(fileTuple._1) // This internally call fileCacheRemove
                          bufferingQRemove(fileTuple._1)
                        }
                      } else {
                        //Invalid File - due to content type
                        logger.error("SMART FILE CONSUMER (global): Moving out " + fileTuple._1 + " with invalid file type ")
                        moveFile(fileTuple._1) // This internally call fileCacheRemove
                        bufferingQRemove(fileTuple._1)
                      }
                    }
                  } else {
                    bufferingQLock.synchronized {
                      bufferingQ_map(fileTuple._1) = (thisFileOrigLength, thisFileStarttime, thisFileFailures)
                    }
                  }
                } else {
                  // File System is not accessible.. issue a warning and go on to the next file.
                  if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): File on the buffering Q is not found " + fileTuple._1 + ", Removing from the buffering queue")
                  fileCacheRemove(fileTuple._1) // No moveFile. So, we must need this.
                  bufferingQRemove(fileTuple._1)
                }
              } catch {
                case ioe: IOException => {
                  thisFileFailures += 1
                  if ((System.currentTimeMillis - thisFileStarttime) > maxTimeFileAllowedToLive && thisFileFailures > maxBufferErrors) {
                    if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): Detected that a stuck file " + fileTuple._1 + " on the buffering queue", ioe)
                    try {
                      moveFile(fileTuple._1)
                      bufferingQRemove(fileTuple._1)
                    } catch {
                      case e: Throwable => {
                        logger.error("SMART_FILE_CONSUMER: Failed to move file, retyring", e)
                      }
                    }
                  } else {
                    // Lock this before update
                    bufferingQLock.synchronized {
                      bufferingQ_map(fileTuple._1) = (thisFileOrigLength, thisFileStarttime, thisFileFailures)
                    }
                    if (logger.isWarnEnabled) logger.warn("SMART_FILE_CONSUMER: IOException trying to monitor the buffering queue for file " + fileTuple._1, ioe)
                  }
                }
                case e: Throwable => {
                  thisFileFailures += 1
                  if ((System.currentTimeMillis - thisFileStarttime) > maxTimeFileAllowedToLive && thisFileFailures > maxBufferErrors) {
                    logger.error("SMART FILE CONSUMER (global): Detected that a stuck file " + fileTuple._1 + " on the buffering queue", e)
                    try {
                      moveFile(fileTuple._1)
                      bufferingQRemove(fileTuple._1)
                    } catch {
                      case e: Throwable => {
                        logger.error("SMART_FILE_CONSUMER: Failed to move file, retyring", e)
                      }
                    }
                  } else {
                    bufferingQLock.synchronized {
                      bufferingQ_map(fileTuple._1) = (thisFileOrigLength, thisFileStarttime, thisFileFailures)
                    }
                    logger.error("SMART_FILE_CONSUMER: IOException trying to monitor the buffering queue ", e)
                  }
                }
              }

            })
          }
        }
        // Give all the files a 1 second to add a few bytes to the contents
        //TODO C&S - make it to parameter
        try {
          Thread.sleep(refreshRate)
        } catch {
          case e: Throwable => {
            logger.warn("SMART_FILE_CONSUMER: Thread sleep exception", e)
          }
        }
      } catch {
        case e: Throwable => {
          logger.error("Error", e)
        }
      }
    }
  }

  private def processExistingFiles(d: File): Unit = {

    if (d.exists && d.isDirectory) {
      FileProcessor.testFailure("TEST EXCEPTION... Processing Existing Files")

      val files = d.listFiles.filter(_.isFile)
        .filter(!_.getName.startsWith("."))
        .sortWith(_.lastModified < _.lastModified).toList
      files.foreach(file => {
        breakable {
          if (!Files.exists(file.toPath())) {
            if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): " + file.toString() + " does not exist in the  " + d.toString)
            break
          }
          if (!checkIfFileBeingProcessed(file.toString)) {
            FileProcessor.enQBufferedFile(file.toString)
          }
        }
      })
    }
  }

  private def isValidFile(fileName: String): Boolean = {
    //Check if the File exists
    var contentType: String = ""
    if (Files.exists(Paths.get(fileName)) && (Paths.get(fileName).toFile().length() > 0)) {
      //Sniff only text/plain and application/gzip for now
      var detector = new DefaultDetector()
      var tika = new Tika(detector)
      var fis = new FileInputStream(new File(fileName))

      try {
        contentType = tika.detect(fis)
      } catch {
        case e: IOException => {
          if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Tika unable to read from InputStream - " + e.getMessage, e)
          throw e
        }
        case e: Exception => {
          if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Tika processing generic exception - " + e.getMessage, e)
          throw e
        }
        case e: Throwable => {
          if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - Tika processing runtime exception - " + e.getMessage, e)
          throw e
        }
      } finally {
        fis.close()
      }

      if (contentType != null && !contentType.isEmpty() && contentType.equalsIgnoreCase("application/octet-stream")) {
        var magicMatcher: MagicMatch = null;
        var is: FileInputStream = null
        try {
          is = new FileInputStream(fileName)
          // BUGBUG:: Take NMB from config.
          // Get Max N MB to detect contentType
          val buffSzToTestContextType = maxFormatValidationArea;
          // Default is 1 * 1024 * 1024
          val tmpbuffer = new Array[Byte](buffSzToTestContextType)

          testFailure("TEST EXCEPTION... Checking File Content Type")

          val readlen = is.read(tmpbuffer, 0, buffSzToTestContextType)
          val buffer =
            if (readlen < buffSzToTestContextType)
              java.util.Arrays.copyOf(tmpbuffer, readlen);
            else
              tmpbuffer
          try {
            magicMatcher = Magic.getMagicMatch(buffer)
            if (magicMatcher != null)
              contentType = magicMatcher.getMimeType
          } catch {
            case e: MagicParseException => {
              if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - MimeMagic caught a parsing exception - " + e.getMessage, e)
              throw e
            }
            case e: MagicMatchNotFoundException => {
              if (logger.isWarnEnabled) logger.warn("SmartFileConsumer -MimeMagic Mime Not Found -" + e.getMessage, e)
              throw e
            }
            case e: MagicException => {
              if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - MimeMagic generic exception - " + e.getMessage, e)
              throw e
            }
            case e: Exception => {
              if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - MimeMagic processing generic exception - " + e.getMessage, e)
              throw e
            }
            case e: Throwable => {
              if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - MimeMagic processing runtime exception - " + e.getMessage, e)
              throw e
            }
          }
        } catch {
          case e: Exception =>
            if (logger.isWarnEnabled) logger.warn("SmartFileConsumer - File read exception - " + e.getMessage, e)
            throw e
        } finally {
          if (is != null)
            is.close()
        }
      }

      //Currently handling only text/plain and application/gzip contents
      //Need to bubble this property out into the Constants and Configuration
      if (contentTypes contains contentType) {
        return true
      } else {
        //Log error for invalid content type
        logger.error("SMART FILE CONSUMER (global): Invalid content type " + contentType + " for file " + fileName)
      }
    } else if (!Files.exists(Paths.get(fileName))) {
      //File doesnot exists - it is already processed
      if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): File aready processed " + fileName)
    } else if (Paths.get(fileName).toFile().length() == 0) {
      if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): File " + fileName + " is valid and content type is" + contentType)
      return true
    }
    if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): File " + fileName + " is not valid for " + contentType)
    return false
  }

  /**
    *
    */
  private def runFileWatcher(): Unit = {
    var fileDirectoryWatchers = scala.actors.threadpool.Executors.newFixedThreadPool(path.size)
    try {
      // Lets see if we have failed previously on this partition Id, and need to replay some messages first.
      if (logger.isInfoEnabled) logger.info(" SMART FILE CONSUMER (global): Recovery operations, checking  => " + MetadataAPIImpl.GetMetadataAPIConfig.getProperty("ZNODE_PATH") + "/smartFileConsumer")
      //if (zkc.checkExists().forPath(MetadataAPIImpl.GetMetadataAPIConfig.getProperty("ZNODE_PATH") + "/smartFileConsumer") != null) {
      if (doNodesExist(MetadataAPIImpl.GetMetadataAPIConfig.getProperty("ZNODE_PATH") + "/smartFileConsumer")) {
        var priorFailures = getNodes(MetadataAPIImpl.GetMetadataAPIConfig.getProperty("ZNODE_PATH") + "/smartFileConsumer") //zkc.getChildren.forPath(MetadataAPIImpl.GetMetadataAPIConfig.getProperty("ZNODE_PATH") + "/smartFileConsumer")
        if (priorFailures != null) {
          var map = priorFailures.toArray
          //var map = parse(new String(priorFailures)).values.asInstanceOf[Map[String, Any]]
          if (map != null) map.foreach(fileToReprocess => {
            var fileToRecover = ""
            try {
              if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (global): Consumer  recovery of file " + URLDecoder.decode(fileToReprocess.asInstanceOf[String], "UTF-8"))

              fileToRecover = URLDecoder.decode(fileToReprocess.asInstanceOf[String], "UTF-8")
              var isFailedFileReprocessed = false
              while (!isFailedFileReprocessed && !LocationWatcher.shutdown)
                try {
                  if (Files.exists(Paths.get(fileToRecover)) &&
                    !checkIfFileBeingProcessed(fileToRecover)) {

                    val offset = getData(znodePath + "/" + fileToReprocess.asInstanceOf[String]) // zkc.getData.forPath(znodePath + "/" + fileToReprocess.asInstanceOf[String])

                    var recoveryInfo = new String(offset)
                    if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (global): " + fileToRecover + " from offset " + recoveryInfo)

                    // There will always be 2 parts here.
                    var partMap = scala.collection.mutable.Map[Int, Int]()
                    var recoveryTokens = recoveryInfo.split(",")
                    var parts = recoveryTokens(1).substring(1, recoveryTokens(1).size - 1)
                    if (parts.size != 0) {
                      var kvs = parts.split(";")
                      kvs.foreach(kv => {
                        var pair = kv.split(":")
                        partMap(pair(0).toInt) = pair(1).toInt
                      })
                    }
                    FileProcessor.enQFile(fileToRecover, recoveryTokens(0).toInt, FileProcessor.RECOVERY_DUMMY_START_TIME, partMap)

                  } else if (!Files.exists(Paths.get(fileToRecover))) {
                    //Check if file is already moved, if yes remove from ZK
                    val tokenName = fileToRecover.split("/")
                    if (Files.exists(Paths.get(targetMoveDir + "/" + tokenName(tokenName.size - 1)))) {
                      if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): Found file " + fileToRecover + " processed ")
                      removeFromZK(fileToRecover)
                    } else {
                      if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): File possibly moved out manually " + fileToRecover)
                      removeFromZK(fileToRecover)
                    }
                  } else {
                    if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global): " + fileToRecover + " already being processed ")
                  }
                  isFailedFileReprocessed = true
                } catch {
                  case e: Throwable => {
                    if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (global) - recovering failed files, Error accesisng disk, retrying")
                    try {
                      Thread.sleep(500)
                    } catch {
                      case ie: InterruptedException => {
                        if (!LocationWatcher.shutdown)
                          throw ie
                      }
                    }

                  }
                }
            }
            catch {
              case e: Exception => {
                logger.error("SMART FILE CONSUMER (global): Failed to recover file:" + fileToRecover, e)
              }
              case e: Throwable => {
                logger.error("SMART FILE CONSUMER (global): Failed to recover file:" + fileToRecover, e)
              }
            }
          })
        }
      }

      if (logger.isInfoEnabled) logger.info("SMART_FILE_CONSUMER partition Initialization complete  Monitoring specified directory for new files")
      val dirNumber = new AtomicInteger()
      val dirArray = path.toArray
      for (dir <- path) {
        fileDirectoryWatchers.execute(new Runnable() {
          override def run() = {
            var errorWaitTime = 1000
            val dirThreadNumber: Int = dirNumber.getAndIncrement()
            val directorToWatch = dirArray(dirThreadNumber)
            while (isMontoringDirectories && !LocationWatcher.shutdown) {
              try {
                if (FileProcessor.isThisNodeReadyToProcess) {
                  if (logger.isDebugEnabled) logger.debug("\n " + dirThreadNumber + " = Watching directory " + directorToWatch + "\n")

                  // This is a throttle point.. will sleep in here until the number of file on an active queue drop below
                  // a treshhold..
                  var isfileProcessorBusy = if (FileProcessor.getFileQSize < FILE_Q_FULL_CONDITION) false else true
                  while (isfileProcessorBusy && !LocationWatcher.shutdown) {
                    try {
                      if (logger.isDebugEnabled) logger.debug("Too many files on the active queue... throttling to let File Processor threads catch up.  Sleep for " + (refreshRate / 3) + " ms")
                      Thread.sleep(refreshRate / 3)
                    } catch {
                      case e: InterruptedException => {
                        isMontoringDirectories = false
                        logger.error("Reading of " + directorToWatch + " has been interrupted")
                        if (!LocationWatcher.shutdown)
                          throw e
                      }
                    }
                    isfileProcessorBusy = if (FileProcessor.getFileQSize < FILE_Q_FULL_CONDITION) false else true
                  }

                  if (isMontoringDirectories) executeCallWithElapsed(processExistingFiles(directorToWatch.toFile()), "Quering directory " + directorToWatch.toFile())
                } else {
                  // No need to do anything
                }
                errorWaitTime = 1000
              } catch {
                case ie: InterruptedException =>
                case e: Exception => {
                  if (logger.isWarnEnabled) logger.warn("Unable to access Directory, Retrying after " + errorWaitTime + " seconds", e)
                  Thread.sleep(errorWaitTime)
                  errorWaitTime = scala.math.min((errorWaitTime * 2), FileProcessor.MAX_WAIT_TIME)
                }
                case e: Throwable => {
                  if (logger.isWarnEnabled) logger.warn("Unable to access Directory, Retrying after " + errorWaitTime + " seconds", e)
                  Thread.sleep(errorWaitTime)
                  errorWaitTime = scala.math.min((errorWaitTime * 2), FileProcessor.MAX_WAIT_TIME)
                }
              }
              try {
                if (isMontoringDirectories) Thread.sleep(refreshRate)
              } catch {
                case e: InterruptedException => {
                  isMontoringDirectories = false
                  logger.error("Reading of " + directorToWatch + " has been interrupted")
                  if (!LocationWatcher.shutdown)
                    throw e
                }
              }
            }
          }
        })
      }

      // Suspend and wait for the shutdown
      while (isMontoringDirectories && !LocationWatcher.shutdown) {
        try {
          Thread.sleep(refreshRate)
        } catch {
          case e: Exception => {

          }
        }
      }

      // On the way out...  clean up.
      fileDirectoryWatchers.shutdownNow()
      globalFileMonitorService.shutdownNow()
      if (zkc != null)
        zkc.close
      zkc = null

    } catch {
      case ie: InterruptedException => {
        isMontoringDirectories = false
        logger.error("Shutting down due to InterruptedException: " + ie)
        fileDirectoryWatchers.shutdownNow()
        globalFileMonitorService.shutdownNow()
      }
      case e: Exception => {
        logger.error("Shutting down due to Exception: ", e)
        isMontoringDirectories = false
        fileDirectoryWatchers.shutdownNow()
        globalFileMonitorService.shutdownNow()
      }
      case e: Throwable => {
        logger.error("Shutting down due to Throwable: ", e)
        isMontoringDirectories = false
        fileDirectoryWatchers.shutdownNow()
        globalFileMonitorService.shutdownNow()
      }
    } finally {
      isMontoringDirectories = false
      fileDirectoryWatchers.shutdownNow()
      globalFileMonitorService.shutdownNow()
      if (zkc != null)
        zkc.close
      zkc = null
    }
  }

  private def monitorActiveFiles: Unit = {

    var afterErrorConditions = false

    while (!LocationWatcher.shutdown) {
      // Do we need to introduct try ... catch for this while loop? What happens to throw errors?
      var isWatchedFileSystemAccesible = true
      var isTargetFileSystemAccesible = true
      var d1: File = null
      if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (global): Scanning for problem files")

      // Watched Directory must be available! NO EXCEPTIONS.. if not, then we need to recreate a file watcher.
      try {
        // DirToWatch is required, TargetMoveDir is  not...
        //d1 = new File(dirToWatch)
        //isWatchedFileSystemAccesible = (d1.canRead && d1.canWrite)
        for (dirName <- dirToWatch.split(System.getProperty("path.separator"))) {
          d1 = new File(dirName)
          isWatchedFileSystemAccesible = isWatchedFileSystemAccesible && (d1.canRead && d1.canWrite)
        }
      } catch {
        case fio: IOException => {
          if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER: ERROR", fio)
          isWatchedFileSystemAccesible = false
        }
        case e: Throwable => {
          if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER: ERROR", e)
          isTargetFileSystemAccesible = false
        }
      }

      // Target Directory must be available if it is used (it may not be). If it is not, we dont need to restart the watcher
      try {
        var d2: File = null
        if (targetMoveDir != null) {
          d2 = new File(targetMoveDir)
          isTargetFileSystemAccesible = (d2.canRead && d2.canWrite)
        }
      } catch {
        case fio: IOException => {
          if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER: ERROR", fio)
          isTargetFileSystemAccesible = false
        }
        case e: Throwable => {
          if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER: ERROR", e)
          isTargetFileSystemAccesible = false
        }
      }


      if (isWatchedFileSystemAccesible && isTargetFileSystemAccesible) {
        if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (global): File system is accessible, perform cleanup for problem files")
        /*  if (afterErrorConditions) {
             try {
              for (dirName <- dirToWatch.split(System.getProperty("path.separator"))) {
                d1 = new File(dirName)
                fileDirectoryWatchers.execute(new Runnable() {
                  override def run() = {
                    processExistingFiles(d1)
                  }
                })
                processExistingFiles(d1)
              }
              afterErrorConditions = false
            } catch {
              case e: IOException => {
                // Interesting,  we have just died, and need to move the newly added files to the queue
                if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (gloabal): Unable to connect to watch directory, will try to shut it down and recreate again", e)
                isWatchedFileSystemAccesible = false
                afterErrorConditions = true
              }
              case e: Throwable => {
                // Interesting,  we have just died, and need to move the newly added files to the queue
                if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (gloabal): Unable to connect to watch directory, will try to shut it down and recreate again", e)
                isWatchedFileSystemAccesible = false
                afterErrorConditions = true
              }
            }
          } */

        if (isWatchedFileSystemAccesible) {
          val missingFiles = getFailedFiles(MISSING)
          missingFiles.foreach(file => FileProcessor.enQFile(file._1, file._2.asInstanceOf[FileStatus].offset.asInstanceOf[Int], file._2.asInstanceOf[FileStatus].createDate))

          val failedFiles = getFailedFiles(IN_PROCESS_FAILED)
          failedFiles.foreach(file => FileProcessor.enQFile(file._1, file._2.asInstanceOf[FileStatus].offset.asInstanceOf[Int], FileProcessor.RECOVERY_DUMMY_START_TIME))

          val unmovedFiles = getFailedFiles(FINISHED_FAILED_TO_COPY)
          unmovedFiles.foreach(file => {
            completeFile(file._1)
          })
        }
      } else {
        if (!isWatchedFileSystemAccesible) {
          afterErrorConditions = true
          if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (gloabal): Unable to access a watched directory, will try to shut it down and recreate")
        }
        if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (gloabal): File system is not accessible. targetMoveDir:" + targetMoveDir)
      }
      try {
        Thread.sleep(HEALTHCHECK_TIMEOUT)
      } catch {
        case e: InterruptedException => {
          if (!LocationWatcher.shutdown)
            throw e
        }
      }
    }
  }


  private def getFailedFiles(fileType: Int): Map[String, FileStatus] = {
    var returnMap: Map[String, FileStatus] = Map[String, FileStatus]()
    activeFilesLock.synchronized {
      val iter = activeFiles.iterator
      while (!LocationWatcher.shutdown && iter.hasNext) {
        var file = iter.next
        val cStatus = file._2
        if (cStatus.status == fileType) {
          returnMap += file
        }
      }
    }
    if (returnMap.size > 0) {
      if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (gloabal): Tracking issue for file type " + fileType)
      if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (gloabal): " + returnMap.toString)
      returnMap.foreach(item => {
        setFileState(item._1, FileProcessor.ACTIVE)
      })
    }
    return returnMap
  }

  // This gets called inthe error case by the recovery logic
  // in normal cases, the KafkaMessafeLoader will handle the completing the file.
  private def completeFile(fileName: String): Unit = {
    try {
      if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER {global): - cleaning up after " + fileName)
      // Either move or rename the file.
      moveFile(fileName)
      markFileProcessingEnd(fileName)
      removeFromZK(fileName)
      fileCacheRemove(fileName)
    } catch {
      case ioe: IOException => {
        logger.error("Exception moving the file ", ioe)
        FileProcessor.setFileState(fileName, FileProcessor.FINISHED_FAILED_TO_COPY)
      }
      case e: Throwable => {
        logger.error("Exception moving the file ", e)
        FileProcessor.setFileState(fileName, FileProcessor.FINISHED_FAILED_TO_COPY)
      }
    }
  }

  private def moveFile(fileName: String): Unit = {
    val fileStruct = fileName.split("/")
    if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER Moving File" + fileName + " to " + targetMoveDir)
    if (Paths.get(fileName).toFile().exists()) {

      testFailure("TEST EXCEPTION... Failing to Move File")

      executeCallWithElapsed(Files.move(Paths.get(fileName), Paths.get(targetMoveDir + "/" + fileStruct(fileStruct.size - 1)), REPLACE_EXISTING), "Moving File " + fileName)
      fileCacheRemove(fileName)
    } else {
      if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER File has been deleted" + fileName);
    }
  }

  def PriorityNodeSetup(config: scala.collection.mutable.Map[String, String]): Unit = {
    logger.info("Setup for node failure events..");
    val zkConnectStr = config.getOrElse(SmartFileAdapterConstants.ZOOKEEPER_CONNECT, null);
    if (zkConnectStr == null || zkConnectStr.length() == 0) {
      logger.error("SMART_FILE_CONSUMER: ZOOKEEPER_CONNECT must be specified")
      throw new MissingPropertyException("Missing Paramter: " + SmartFileAdapterConstants.ZOOKEEPER_CONNECT, null)
    }
    val zkSessionTimeOut = config.getOrElse(SmartFileAdapterConstants.ZOOKEEPER_SESSION_TIMEOUT_MS, "30000").toInt;
    val zkConnectionTimeOut = config.getOrElse(SmartFileAdapterConstants.ZOOKEEPER_CONNECTION_TIMEOUT_MS, "30000").toInt;
    val zkLeaderPath = config.getOrElse(SmartFileAdapterConstants.ZOOKEEPER_LEADER_PATH, "/kamanja/adapters/leader");
    val component_name = config.getOrElse(SmartFileAdapterConstants.COMPONENT_NAME, "FileConsumer");
    val node_id_prefix = config.getOrElse(SmartFileAdapterConstants.NODE_ID_PREFIX, "Node1");
    val adapter_weight = config.getOrElse(SmartFileAdapterConstants.ADAPTER_WEIGHT, "10").toInt;

    logger.info("Create an instnce of ProcessComponentByWeight ...");
    try {
      pcbw = new ProcessComponentByWeight(component_name, node_id_prefix, adapter_weight);
    } catch {
      case e: Throwable => {
        logger.error("Error : " + e.getMessage() + e);
      }
    }

    logger.info("Call ProcessComponentByWeight.Init..");
    pcbw.Init(zkConnectStr, zkLeaderPath, zkSessionTimeOut, zkConnectionTimeOut);
  }

  def AcquireLock(): Unit = {
    try {
      if (!isLockAcquired) {
        logger.info("Acquiring the lock ..");
        pcbw.Acquire();
        isLockAcquired = true;
        logger.info("Acquired the lock ..");
      }
    } catch {
      case e: Exception => {
        logger.error("Error : " + e.getMessage() + e);
        throw e;
      }
    }
  }

  def ReleaseLock(): Unit = {
    if (isLockAcquired) {
      logger.info("Releasing the lock ..");
      pcbw.Release();
      isLockAcquired = false;
      logger.info("Released the lock ..");
    }
  }


  val externalizeStats = new Runnable {
    def run(): Unit = {
      try {
        val flCacheMap = fileCacheLock.synchronized {
          fileCache.toMap
        }
        val flQMap = fileQLock.synchronized {
          fileQ.map(file => (file.name -> file.createDate)).toMap
        }
        val actFlMap = activeFilesLock.synchronized {
          activeFiles.toMap
        }
        val bufQMap = bufferingQLock.synchronized {
          bufferingQ_map.toMap
        }

        val flOnlyInCacheMap = flCacheMap -- flQMap.map(fl => fl._1) -- bufQMap.map(fl => fl._1)
        val flOnlyInBufQ = bufQMap -- flCacheMap.map(fl => fl._1)
        val flOnlyInFlQ = flQMap -- flCacheMap.map(fl => fl._1)
        val flCacheAndBufQ = flCacheMap -- (flCacheMap -- bufQMap.map(fl => fl._1)).map(fl => fl._1) -- flOnlyInBufQ.map(fl => fl._1)
        val flCacheAndFlQ = flCacheMap -- (flCacheMap -- flQMap.map(fl => fl._1)).map(fl => fl._1) -- flOnlyInFlQ.map(fl => fl._1)

        implicit val formats = DefaultFormats
        val statusJson = (("OnlyCachedFiles" -> (flOnlyInCacheMap)) ~
          ("OnlyBufferingFiles" -> flOnlyInBufQ.map(file => {
            ("fileName" -> file._1) ~
              ("fileInfo" -> (file._2._1 + ":" + file._2._2 + ":" + file._2._3))
          })) ~
          ("OnlyEnqueuedFiles" -> (flOnlyInFlQ)) ~
          ("CachedAndBufferingFiles" -> (flCacheAndBufQ)) ~
          ("CachedAndEnqueuedFiles" -> (flCacheAndFlQ)) ~
          ("ActiveFiles" -> actFlMap.map { file =>
            ("fileName" -> file._1) ~
              ("fileInfo" -> ("status" -> file._2.asInstanceOf[FileStatus].status) ~
                ("offset" -> file._2.asInstanceOf[FileStatus].offset) ~
                ("createDate" -> file._2.asInstanceOf[FileStatus].createDate))
          }))

        var status_data_string = pretty(render(statusJson))
        setData(znodePath_Status, status_data_string.getBytes())
        if (logger.isWarnEnabled) logger.warn(status_data_string)
      } catch {
        case e: Throwable => {
          if (logger.isWarnEnabled) logger.warn("Unable to produce STATS due to ", e)
          try {
            Thread.sleep(FileProcessor.RECOVERY_SLEEP_TIME)
          } catch {
            case e: InterruptedException => {
              if (!LocationWatcher.shutdown)
                throw e
            }
          }
        }
      }
    }
  }
}

class FileProcessorThread(val flProcessor: FileProcessor) extends Runnable {
  override def run() = {
    try {
      if (flProcessor != null)
        flProcessor.run()
    } catch {
      case e: Throwable => {

      }
    }
  }
}

/**
  *
  * @param path
  * @param partitionId
  */
class FileProcessor(val path: ArrayBuffer[Path], val partitionId: Int) {

  private var kml: KafkaMessageLoader = null
  private var zkc: CuratorFramework = null
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  var fileConsumers: ExecutorService = scala.actors.threadpool.Executors.newFixedThreadPool(3)

  //val inMemoryBuffersCntr = new java.util.concurrent.atomic.AtomicLong()

  var stillConsuming = true
  var isConsuming = true
  var isProducing = true

  var isContentParsable = true

  private var workerBees: ExecutorService = null
  private var tempKMS = false
  // QUEUES used in file processing... will be synchronized.s
  private var msgQ: scala.collection.mutable.Queue[Array[KafkaMessage]] = scala.collection.mutable.Queue[Array[KafkaMessage]]()
  private var bufferQ: scala.collection.mutable.Queue[BufferToChunk] = scala.collection.mutable.Queue[BufferToChunk]()
  private var blg = new BufferLeftoversArea(-1, null, -1)

  private val msgQLock = new Object
  private val bufferQLock = new Object
  private val beeLock = new Object

  private var msgCount = 0

  // Confugurable Properties
  private var dirToWatch: String = ""
  private var dirToMoveTo: String = ""
  private var message_separator: Char = _
  private var field_separator: Char = _
  private var kv_separator: Char = _
  private var NUMBER_OF_BEES: Int = 2
  private var maxlen: Int = _
  private var partitionSelectionNumber: Int = _
  private var kafkaTopic = ""
  private var readyToProcessKey = ""
  private var maxBufAllowed: Long = 0
  private var throttleTime: Int = 0
  private var isRecoveryOps = true
  private var bufferLimit = 1
  private var dirToError: String = ""
  private var fileAge: String = ""
  private var fileCount: Int = 0
  private var fileAgeInt: Int = 0
  private val ONE_SECOND_IN_MILLIS = 1000
  private val ONE_MINUTE_IN_MILLIS = 60000
  private val ONE_HOUR_IN_MILLIS = 3600000
  def setContentParsableFlag(isParsable: Boolean): Unit = synchronized {
    isContentParsable = isParsable
  }

  /**
    * Called by the Directory Listener to initialize
    *
    * @param props
    */
  def init(props: scala.collection.mutable.Map[String, String]): Unit = {
    try {
      message_separator = props.getOrElse(SmartFileAdapterConstants.MSG_SEPARATOR, "10").toInt.toChar
      dirToWatch = props.getOrElse(SmartFileAdapterConstants.DIRECTORY_TO_WATCH, null)
      dirToMoveTo = props.getOrElse(SmartFileAdapterConstants.DIRECTORY_TO_MOVE_TO, null)
      NUMBER_OF_BEES = props.getOrElse(SmartFileAdapterConstants.PAR_DEGREE_OF_FILE_CONSUMER, "1").toInt
      maxlen = props.getOrElse(SmartFileAdapterConstants.WORKER_BUFFER_SIZE, "4").toInt * 1024 * 1024
      partitionSelectionNumber = props(SmartFileAdapterConstants.NUMBER_OF_FILE_CONSUMERS).toInt
      bufferLimit = props.getOrElse(SmartFileAdapterConstants.THREAD_BUFFER_LIMIT, "1").toInt
      FileProcessor.WARNING_THRESHTHOLD = props.getOrElse(SmartFileAdapterConstants.DELAY_WARNING_THRESHOLD, "5000").toLong * 1000000
      FileProcessor.FILE_Q_FULL_CONDITION = props.getOrElse(SmartFileAdapterConstants.FILE_Q_FULL_CONDITION, "300").toInt
      //Code commented
      readyToProcessKey = props.getOrElse(SmartFileAdapterConstants.READY_MESSAGE_MASK, ".gzip")

      maxBufAllowed = props.getOrElse(SmartFileAdapterConstants.MAX_MEM, "512").toLong * 1024L * 1024L
      throttleTime = props.getOrElse(SmartFileAdapterConstants.THROTTLE_TIME, "100").toInt
      var mdConfig = props.getOrElse(SmartFileAdapterConstants.METADATA_CONFIG_FILE, null)
      var msgName = props.getOrElse(SmartFileAdapterConstants.MESSAGE_NAME, null)
      var kafkaBroker = props.getOrElse(SmartFileAdapterConstants.KAFKA_BROKER, null)
      dirToError = props.getOrElse(SmartFileAdapterConstants.ERROR_DIR, null)
      fileAge = props.getOrElse(SmartFileAdapterConstants.FILE_AGE, null)
      fileCount = props.getOrElse(SmartFileAdapterConstants.FILE_COUNT, "0").toInt

      //Default allowed content types -
      var cTypes = props.getOrElse(SmartFileAdapterConstants.VALID_CONTENT_TYPES, "text/plain;application/gzip")

      for (cType <- cTypes.split(";")) {
        //if (logger.isInfoEnabled) logger.info("SMART_FILE_CONSUMER Putting "+cType+" into allowed content types")
        if (!FileProcessor.contentTypes.contains(cType))
          FileProcessor.contentTypes.put(cType, cType)
      }

      if(dirToError == null){
        logger.error("SMART_FILE_CONSUMER (" + partitionId + ") Destination directory for error must be specified")
        shutdown
        throw MissingPropertyException("Missing Paramter: " + SmartFileAdapterConstants.ERROR_DIR, null)
      }

      if(fileAge == null || fileAge.length == 0){
        logger.warn("SMART_FILE_CONSUMER (" + partitionId + ") file age did not specified. The default value is 30 minute")
        fileAge = "30m"
      }

      fileAgeInt =
        if(fileAge.takeRight(1) == "s"){
        fileAge.substring(0, fileAge.length-1).toInt * ONE_SECOND_IN_MILLIS
      } else if(fileAge.takeRight(1) == "m"){
        fileAge.substring(0, fileAge.length-1).toInt * ONE_MINUTE_IN_MILLIS
      }  else if(fileAge.takeRight(1) == "h"){
        fileAge.substring(0, fileAge.length-1).toInt * ONE_HOUR_IN_MILLIS
      } else{
        30 * ONE_MINUTE_IN_MILLIS // default value for file age is 30 minute
      }

      if(fileCount == 0){
        logger.warn("SMART_FILE_CONSUMER (" + partitionId + ") file count did not specified. Th default value is 100")
      }
      kafkaTopic = props.getOrElse(SmartFileAdapterConstants.KAFKA_TOPIC, null)

      // Bail out if dirToWatch, Topic are not set
      if (kafkaTopic == null) {
        logger.error("SMART_FILE_CONSUMER (" + partitionId + ") Kafka Topic to populate must be specified")
        shutdown
        throw MissingPropertyException("Missing Paramter: " + SmartFileAdapterConstants.KAFKA_TOPIC, null)
      }

      if (dirToWatch == null) {
        logger.error("SMART_FILE_CONSUMER (" + partitionId + ") Directory to watch must be specified")
        shutdown
        throw MissingPropertyException("Missing Paramter: " + SmartFileAdapterConstants.DIRECTORY_TO_WATCH, null)
      }

      if (dirToMoveTo == null) {
        logger.error("SMART_FILE_CONSUMER (" + partitionId + ") Destination directory must be specified")
        shutdown
        throw MissingPropertyException("Missing Paramter: " + SmartFileAdapterConstants.DIRECTORY_TO_MOVE_TO, null)
      }

      if (mdConfig == null) {
        logger.error("SMART_FILE_CONSUMER (" + partitionId + ") Directory to watch must be specified")
        shutdown
        throw new MissingPropertyException("Missing Paramter: " + SmartFileAdapterConstants.METADATA_CONFIG_FILE, null)
      }

      if (msgName == null) {
        logger.error("SMART_FILE_CONSUMER (" + partitionId + ") Message name must be specified")
        shutdown
        throw new MissingPropertyException("Missing Paramter: " + SmartFileAdapterConstants.MESSAGE_NAME, null)
      }

      if (kafkaBroker == null) {
        logger.error("SMART_FILE_CONSUMER (" + partitionId + ") Kafka Broker details must be specified")
        shutdown
        throw new MissingPropertyException("Missing Paramter: " + SmartFileAdapterConstants.KAFKA_BROKER, null)
      }

      FileProcessor.setProperties(props, path)
      FileProcessor.startGlobalFileMonitor

      if (logger.isInfoEnabled) logger.info("SMART_FILE_CONSUMER (" + partitionId + ") Initializing Kafka loading process")
      // Initialize threads
      try {
        kml = new KafkaMessageLoader(partitionId, props)
      } catch {
        case e: Exception => {
          logger.error("SMART_FILE_CONSUMER: ERROR", e)
          shutdown
          throw e
        }
        case e: Throwable => {
          logger.error("SMART_FILE_CONSUMER: ERROR", e)
          shutdown
          throw e
        }
      }
    } catch {
      case e: Exception => {
        logger.error("SMART_FILE_CONSUMER: ERROR", e)
        shutdown
        throw e
      }
      case e: Throwable => {
        logger.error("SMART_FILE_CONSUMER: ERROR", e)
        shutdown
        throw e
      }
    }
  }

  private def enQMsg(buffer: Array[KafkaMessage], bee: Int): Unit = {
    msgQLock.synchronized {
      msgQ += buffer
    }
  }

  private def deQMsg(): Array[KafkaMessage] = {
    msgQLock.synchronized {
      if (msgQ.isEmpty) {
        return null
      }
      // inMemoryBuffersCntr.decrementAndGet() // Incrementing when we enQBuffer and Decrementing when we deQMsg
      return msgQ.dequeue
    }
  }

  private def enQBuffer(buffer: BufferToChunk): Unit = {
    bufferQLock.synchronized {
      bufferQ += buffer
    }
  }

  private def deQBuffer(bee: Int): BufferToChunk = {
    bufferQLock.synchronized {
      if (bufferQ.isEmpty) {
        return null
      }
      return bufferQ.dequeue
    }
  }

  private def getLeftovers(code: Int): BufferLeftoversArea = {
    beeLock.synchronized {
      return blg
    }
  }

  private def setLeftovers(in: BufferLeftoversArea, code: Int) = {
    beeLock.synchronized {
      blg = in
    }
  }


  /**
    * Each worker bee will run this code... looking for work to do.
    *
    * @param beeNumber
    */
  private def processBuffers(beeNumber: Int) = {

    var msgNum: Int = 0
    var myLeftovers: BufferLeftoversArea = null
    var buffer: BufferToChunk = null;
    var fileNameToProcess: String = ""
    var isEofBuffer = false
    var messages: scala.collection.mutable.LinkedHashSet[KafkaMessage] = null
    var leftOvers: Array[Char] = new Array[Char](0)


    // basically, keep running until shutdown.
    while (isConsuming && !LocationWatcher.shutdown) {
      // Try to get a new file to process.
      buffer = deQBuffer(beeNumber)

      // If the buffer is there to process, do it
      if (buffer != null) {
        // If the new file being processed,  offsets to messages in this file to 0.
        if (!fileNameToProcess.equalsIgnoreCase(buffer.relatedFileName)) {
          msgNum = 0
          fileNameToProcess = buffer.relatedFileName
          isEofBuffer = false
        }

        // In this state, we need to ignore the rest of the incoming buffers, until a new file is encountered.
        if (!isContentParsable) {
          if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (" + partitionId + "): Ignoring buffers due to earlier corruption in the file.")

          // Tell whomever waiting for the leftovers of this buffer that it ain't coming...
          var newLeftovers = BufferLeftoversArea(beeNumber, leftOvers, buffer.chunkNumber)
          setLeftovers(newLeftovers, beeNumber)
        } else {
          // need a ordered structure to keep the messages.
          messages = scala.collection.mutable.LinkedHashSet[KafkaMessage]()

          var indx = 0
          var prevIndx = indx
          var isTmpContentParsable = true

          // Record if this is the last 'dummy' buffer
          isEofBuffer = buffer.isEof

          // if we got an exception while reading the file and filling buffers, we are going to bail.
          if (buffer.firstValidOffset <= FileProcessor.BROKEN_FILE) {
            // Broken File is recoverable, CORRUPTED FILE ISNT!!!!!
            if (buffer.firstValidOffset == FileProcessor.BROKEN_FILE) {
              logger.error("SMART FILE CONSUMER (" + partitionId + "): Detected a broken file")
              messages.add(new KafkaMessage(Array[Char](), FileProcessor.BROKEN_FILE, true, true, buffer.relatedFileName, buffer.partMap, FileProcessor.BROKEN_FILE))
            } else {
              logger.error("SMART FILE CONSUMER (" + partitionId + "): Detected a broken file")
              messages.add(new KafkaMessage(Array[Char](), FileProcessor.CORRUPT_FILE, true, true, buffer.relatedFileName, buffer.partMap, FileProcessor.CORRUPT_FILE))
            }
          } else {
            // Look for messages - this buffer is going to have some
            if (!buffer.isEof) {
              isTmpContentParsable = (buffer.payload.size < maxlen) // false only if the buffer is completely full
              // if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (" + partitionId + "): isTmpContentParsable:" + isTmpContentParsable + ", isContentParsable:" + isContentParsable + ", maxlen:" + maxlen + ", buffer.payload.size:" + buffer.payload.size)
              buffer.payload.foreach(x => {
                if (isContentParsable) {
                  if (x.asInstanceOf[Char] == message_separator) {
                    isTmpContentParsable = true
                    var newMsg: Array[Char] = buffer.payload.slice(prevIndx, indx)
                    msgNum += 1
                    if (logger.isDebugEnabled) logger.debug("SMART_FILE_CONSUMER (" + partitionId + ") Message offset " + msgNum + ", and the buffer offset is " + buffer.firstValidOffset)

                    // Ok, we could be in recovery, so we have to ignore some messages, but these ignoraable messages must still
                    // appear in the leftover areas
                    messages.add(new KafkaMessage(newMsg, buffer.firstValidOffset, false, false, buffer.relatedFileName, buffer.partMap, prevIndx))

                    prevIndx = indx + 1
                  }
                  if ((indx - prevIndx) >= maxlen) {
                    if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (" + partitionId + "): Found message size more than " + maxlen + ". isTmpContentParsable:" + isTmpContentParsable + ", isContentParsable:" + isContentParsable + ", buffer.payload.size:" + buffer.payload.size)
                    // Don't want to take any more from this buffer. We move beyond the maxlen
                    isTmpContentParsable = false
                    //                    setContentParsableFlag(false)
                  }
                }
                indx = indx + 1
              })
            }
          }

          // We are here if our sanity check notifies us that we cannot separate data into distinct lines... ERROR out as CORRUPT
          if (isContentParsable && !isTmpContentParsable) {
            // Basically, the first time we fail on the file, we go here...
            setContentParsableFlag(false)
            logger.error("SMART FILE CONSUMER (" + partitionId + "): This maybe a corrupt file, The max length of the line must be the size of a processing buffer")
            messages = scala.collection.mutable.LinkedHashSet[KafkaMessage]()
            messages.add(new KafkaMessage(Array[Char](), FileProcessor.CORRUPT_FILE, true, true, buffer.relatedFileName, buffer.partMap, FileProcessor.CORRUPT_FILE))
            enQMsg(messages.toArray, beeNumber)
          } else if (isContentParsable) {

            // Normal execution flow.
            // Wait for a previous worker be to finish so that we can get the leftovers.,, If we are the first buffer, then
            // just publish
            if (buffer.chunkNumber == 0) {
              enQMsg(messages.toArray, beeNumber)
            }

            var tstContentParsable = true
            var foundRelatedLeftovers = false
            var isIncompleteLefovers = false

            // wait for the previous buffer to be processed..  first buffer in line will skip this.
            while (!foundRelatedLeftovers && buffer.chunkNumber != 0 && !LocationWatcher.shutdown) {
              myLeftovers = getLeftovers(beeNumber)
              if (myLeftovers.relatedChunk == (buffer.chunkNumber - 1)) {
                // Leftovers from a prevous buffers, incorporate it to into the these messages
                tstContentParsable = isContentParsable
                if (!tstContentParsable) {
                  //TODO can this be here..
                  if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER (" + partitionId + "): Ignoring buffers due to earlier corruption in the file detection.")
                  var newLeftovers = BufferLeftoversArea(beeNumber, leftOvers, buffer.chunkNumber)
                  setLeftovers(newLeftovers, beeNumber)
                } else {
                  leftOvers = myLeftovers.leftovers
                  foundRelatedLeftovers = true

                  //NOTE:: First Message + previous leftover should not exceed maxlen
                  // Prepend the leftovers to the first element of the array of messages
                  val msgArray = messages.toArray
                  var firstMsgWithLefovers: KafkaMessage = null
                  if (isEofBuffer) {
                    // This is the last 'dummy' buffer. the leftovers here are a message.  If the leftover are > maxlen
                    // then we are corrupted
                    if (leftOvers.size > maxlen) {
                      setContentParsableFlag(false)
                      logger.error("SMART FILE CONSUMER (" + partitionId + "): This maybe a corrupt file from leftover, The max length of the line must be the size of a processing buffer")
                      messages = scala.collection.mutable.LinkedHashSet[KafkaMessage]()
                      messages.add(new KafkaMessage(Array[Char](), FileProcessor.CORRUPT_FILE, true, true, buffer.relatedFileName, buffer.partMap, FileProcessor.CORRUPT_FILE))
                      enQMsg(messages.toArray, beeNumber)
                      isIncompleteLefovers = false //BUGBUG:: DO we need this?????
                      var newLeftovers = BufferLeftoversArea(beeNumber, Array[Char](), buffer.chunkNumber)
                      setLeftovers(newLeftovers, beeNumber)
                    } else if (leftOvers.size > 0) {
                      firstMsgWithLefovers = new KafkaMessage(leftOvers, buffer.firstValidOffset, false, false, buffer.relatedFileName, buffer.partMap, buffer.firstValidOffset)
                      messages.add(firstMsgWithLefovers)
                      enQMsg(messages.toArray, beeNumber)
                      isIncompleteLefovers = false
                    }
                  } else {
                    // there is valuable data in this here buffer.
                    if (messages.size > 0) {
                      // if the first message (leftover, plus the firlst messages) are greater then max, we are corrupt
                      if ((leftOvers.size + msgArray(0).msg.size) > maxlen) {
                        setContentParsableFlag(false)
                        logger.error("SMART FILE CONSUMER (" + partitionId + "): This maybe a corrupt file from leftover & first message, The max length of the line must be the size of a processing buffer")
                        messages = scala.collection.mutable.LinkedHashSet[KafkaMessage]()
                        messages.add(new KafkaMessage(Array[Char](), FileProcessor.CORRUPT_FILE, true, true, buffer.relatedFileName, buffer.partMap, FileProcessor.CORRUPT_FILE))
                        enQMsg(messages.toArray, beeNumber)
                        var newLeftovers = BufferLeftoversArea(beeNumber, Array[Char](), buffer.chunkNumber)
                        setLeftovers(newLeftovers, beeNumber)
                      } else {
                        //BUGBUG:: Do we need to care about the first message size > maxlen
                        firstMsgWithLefovers = new KafkaMessage(leftOvers ++ msgArray(0).msg, msgArray(0).offsetInFile, false, false, buffer.relatedFileName, msgArray(0).partMap, msgArray(0).offsetInFile)
                        msgArray(0) = firstMsgWithLefovers
                        enQMsg(msgArray, beeNumber)
                        isIncompleteLefovers = false
                      }
                    } else {
                      // there are no messages and this is not the last buffer... the only scenario is if we read
                      // a tiny string from stream that does not contain message separator.
                      if ((leftOvers.size + buffer.payload.size) > maxlen) {
                        setContentParsableFlag(false)
                        logger.error("SMART FILE CONSUMER (" + partitionId + "): This maybe a corrupt file from leftover & payload, The max length of the line must be the size of a processing buffer")
                        messages = scala.collection.mutable.LinkedHashSet[KafkaMessage]()
                        messages.add(new KafkaMessage(Array[Char](), FileProcessor.CORRUPT_FILE, true, true, buffer.relatedFileName, buffer.partMap, FileProcessor.CORRUPT_FILE))
                        enQMsg(messages.toArray, beeNumber)
                        var newLeftovers = BufferLeftoversArea(beeNumber, Array[Char](), buffer.chunkNumber)
                        setLeftovers(newLeftovers, beeNumber)
                      } else {
                        // combine the tiny buffer portion with the existing leftovers
                        var newLeftovers = BufferLeftoversArea(beeNumber, leftOvers ++ buffer.payload, buffer.chunkNumber)
                        setLeftovers(newLeftovers, beeNumber)
                        // inMemoryBuffersCntr.decrementAndGet()
                        isIncompleteLefovers = true
                      }
                    }
                  }
                }
              } else {
                Thread.sleep(100)
              }
            }

            // Handle the lefovers from this buffer
            if (!isIncompleteLefovers && tstContentParsable) {
              if (isContentParsable) {
                // We may have some leftovers from this latest buffer...whatever is left is the leftover we need to pass to another thread.
                indx = scala.math.min(indx, buffer.len)

                if (indx != prevIndx) {
                  if (!isEofBuffer) {
                    if ((indx - prevIndx) > maxlen) {
                      setContentParsableFlag(false)
                      logger.error("SMART FILE CONSUMER (" + partitionId + "): This maybe a corrupt file from final leftover, The max length of the line must be the size of a processing buffer")
                      messages = scala.collection.mutable.LinkedHashSet[KafkaMessage]()
                      messages.add(new KafkaMessage(Array[Char](), FileProcessor.CORRUPT_FILE, true, true, buffer.relatedFileName, buffer.partMap, FileProcessor.CORRUPT_FILE))
                      enQMsg(messages.toArray, beeNumber)
                      var newLeftovers = BufferLeftoversArea(beeNumber, Array[Char](), buffer.chunkNumber)
                      setLeftovers(newLeftovers, beeNumber)
                    } else {

                      // slice the remains from the buffer...
                      val newFileLeftOvers = BufferLeftoversArea(beeNumber, buffer.payload.slice(prevIndx, indx), buffer.chunkNumber)
                      setLeftovers(newFileLeftOvers, beeNumber)
                    }
                  } else {
                    // This is the last dummy buffer... it has no leftovers
                    val newFileLeftOvers = BufferLeftoversArea(beeNumber, new Array[Char](0), buffer.chunkNumber)
                    setLeftovers(newFileLeftOvers, beeNumber)
                  }
                } else {
                  // there are no leftovers.. we pegged it and the last character in the buffer has been put into a
                  // message....
                  val newFileLeftOvers = BufferLeftoversArea(beeNumber, new Array[Char](0), buffer.chunkNumber)
                  setLeftovers(newFileLeftOvers, beeNumber)
                }
              } else {
                val newFileLeftOvers = BufferLeftoversArea(beeNumber, new Array[Char](0), buffer.chunkNumber)
                setLeftovers(newFileLeftOvers, beeNumber)
              }
            }
          }
        }
      } else {
        // Ok, we did not find a buffer to process on the BufferQ.. wait.
        Thread.sleep(100)
      }
    }
  }

  /**
    * This will be run under a CONSUMER THREAD.
    *
    * @param file
    */
  private def readBytesChunksFromFile(file: EnqueuedFile): Unit = {

    val buffer = new Array[Char](maxlen)
    var readlen = 0
    var len: Int = 0
    var totalLen = 0
    var chunkNumber = 0

    val fileName = file.name
    val offset = file.offset
    val partMap = file.partMap

    setContentParsableFlag(true)

    val r = scala.util.Random

    // Start the worker bees... should only be started the first time..
    if (workerBees == null) {
      workerBees = scala.actors.threadpool.Executors.newFixedThreadPool(NUMBER_OF_BEES)
      for (i <- 1 to NUMBER_OF_BEES) {
        workerBees.execute(new Runnable() {
          override def run() = {
            try {
              processBuffers(i)
            } catch {
              case e: Exception => {
                logger.error("Failure", e)
              }
              case e: Throwable => {
                logger.error("Failure", e)
              }
            }
          }
        })
      }
    }

    // Grab the InputStream from the file and start processing it.  Enqueue the chunks onto the BufferQ for the
    // worker bees to pick them up.
    //var bis: InputStream = new ByteArrayInputStream(Files.readAllBytes(Paths.get(fileName)))
    var bis: BufferedReader = null
    try {
      FileProcessor.testFailure("TEST EXCEPTION... Doing a BIS")

      if (isCompressed(fileName)) {
        bis = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))))
      } else {
        bis = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)))
      }
    } catch {
      // Ok, sooo if the file is not Found, either someone moved the file manually, or this specific destination is not reachable..
      // We just drop the file, if it is still in the directory, then it will get picked up and reprocessed the next tick.
      case fio: java.io.FileNotFoundException => {
        logger.error("SMART_FILE_CONSUMER (" + partitionId + ") Exception accessing the file for processing the file - File is missing", fio)
        FileProcessor.removeFromZK(fileName)
        FileProcessor.markFileProcessingEnd(fileName)
        FileProcessor.fileCacheRemove(fileName)
        if (bis != null) bis.close
        return
      }
      case fio: IOException => {
        logger.error("SMART_FILE_CONSUMER (" + partitionId + ") Exception accessing the file for processing the file ", fio)
        FileProcessor.setFileState(fileName, FileProcessor.MISSING)
        if (bis != null) bis.close
        return
      }
      case e: Throwable => {
        logger.error("SMART_FILE_CONSUMER (" + partitionId + ") Exception accessing the file for processing the file ", e)
        FileProcessor.setFileState(fileName, FileProcessor.MISSING)
        if (bis != null) bis.close
        return
      }
    }

    // Intitialize the leftover area for this file reading.
    var newFileLeftOvers = BufferLeftoversArea(0, Array[Char](), -1)
    setLeftovers(newFileLeftOvers, 0)

    var waitedCntr = 0
    var tempFailure = 0
    var foundIssue = false
    do {
      waitedCntr = 0
      val st = System.currentTimeMillis
      //while ((BufferCounters.inMemoryBuffersCntr.get * 2 + partitionSelectionNumber + 2) * maxlen * 2 > maxBufAllowed) 
      // One counter for bufferQ and one for msgQ and also taken concurrentKafkaJobsRunning and 2 extra in memory
      while (!LocationWatcher.shutdown && (bufferQ.size + msgQ.size) >= bufferLimit) {
        if (waitedCntr == 0) {
          if (logger.isWarnEnabled) logger.warn("SMART FILE ADDAPTER (" + partitionId + ") : current size:%d (bufferQ:%d + msgQ:%d) exceed the MAX number of %d buffers. Halting for a free slot".format(bufferQ.size + msgQ.size, bufferQ.size, msgQ.size, bufferLimit))
        }
        waitedCntr += 1
        Thread.sleep(throttleTime)
      }

      if (waitedCntr > 0) {
        val timeDiff = System.currentTimeMillis - st
        if (logger.isWarnEnabled) logger.warn("%d:Got a slot after waiting %dms".format(partitionId, timeDiff))
      }

      if (!isContentParsable) {
        // THis means that we have found corruption while looking at a previous buffer.... we should not push any more
        // crap into the buffer queue.. its all garbage somehow.
        logger.error("ContentParsable failure detected for file " + fileName + " we are skipping chunk #" + chunkNumber)
        val BufferToChunk = new BufferToChunk(0, Array[Char](message_separator), chunkNumber, fileName, FileProcessor.CORRUPT_FILE, true, partMap)
        enQBuffer(BufferToChunk) // ????????? TODO, is this needed?????
        chunkNumber += 1
        foundIssue = true
      } else if (!LocationWatcher.shutdown) {
        var isLastChunk = false // Initialize
        try {
          readlen = 0
          var curReadLen = bis.read(buffer, readlen, maxlen - readlen - 1)

          FileProcessor.testFailure("TEST EXCEPTION... Doing a READ")

          if (curReadLen > 0)
            readlen += curReadLen
          else // First time reading into buffer triggered end of file (< 0)
            readlen = curReadLen
          val minBuf = maxlen / 3; // We are expecting at least 1/3 of the buffer need to fill before

          while (!LocationWatcher.shutdown && readlen < minBuf && curReadLen > 0) {
            // Re-reading some more data
            curReadLen = bis.read(buffer, readlen, maxlen - readlen - 1)
            if (curReadLen > 0)
              readlen += curReadLen
          }
        } catch {
          case e: Throwable => {
            logger.error("Failed to read file, file corrupted " + fileName, e)
            val BufferToChunk = new BufferToChunk(readlen, buffer.slice(0, readlen), chunkNumber, fileName, FileProcessor.CORRUPT_FILE, isLastChunk, partMap)
            enQBuffer(BufferToChunk)
            chunkNumber += 1
            foundIssue = true
          }
        }

        if (!foundIssue) {
          if (readlen > 0) {
            // Still have data, create a new buffer with it
            totalLen += readlen
            len += readlen
            val BufferToChunk = new BufferToChunk(readlen, buffer.slice(0, readlen), chunkNumber, fileName, offset, isLastChunk, partMap)
            enQBuffer(BufferToChunk)
            chunkNumber += 1
          } else {
            // add a buffer with only the Message separator- this will make sure we pick up the last leftovers
            val BufferToChunk = new BufferToChunk(readlen, Array[Char](message_separator), chunkNumber, fileName, offset, true, partMap)
            enQBuffer(BufferToChunk)
            chunkNumber += 1
          }
        }
      }
    } while (readlen > 0 && !foundIssue && !LocationWatcher.shutdown)

    // Pass the leftovers..  - some may have been left by the last chunkBuffer... nothing else will pick it up...
    // make it a KamfkaMessage buffer.
    var myLeftovers: BufferLeftoversArea = null
    var foundRelatedLeftovers = false
    while (!foundRelatedLeftovers && !LocationWatcher.shutdown) {
      myLeftovers = getLeftovers(FileProcessor.DEBUG_MAIN_CONSUMER_THREAD_ACTION)
      // if this is for the last chunk written...
      if (myLeftovers.relatedChunk == (chunkNumber - 1)) {
        // EnqMsg here.. but only if there is something in there.
        if (!foundIssue) {
          if (myLeftovers.leftovers.size > 0) {
            // Not sure how we got there... this should not happen.
            if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER: partition " + partitionId + ": NON-EMPTY final leftovers, this really should not happend... check the file ")
          } else {
            // This is the case where we find the last 0 length leftovers.. this means that we can add a message that will
            // close the file
            if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER: partition \" + partitionId + \", " + fileName + " is finished processing")
            //  if (isContentParsable) {
            val messages: scala.collection.mutable.LinkedHashSet[KafkaMessage] = scala.collection.mutable.LinkedHashSet[KafkaMessage]()
            messages.add(new KafkaMessage(null, 0, true, true, fileName, scala.collection.mutable.Map[Int, Int](), 0))
            enQMsg(messages.toArray, 1000)
            // }
          }
        }
        foundRelatedLeftovers = true
      } else {
        Thread.sleep(100)
      }
    }
    // Done with this file... mark is as closed
    try {
      // markFileAsFinished(fileName)
      if (bis != null) bis.close
      bis = null
    } catch {
      case ioe: IOException => {
        if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER: partition " + partitionId + " Unable to detect file as being processed " + fileName)
        if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER: Check to make sure the input directory does not still contain this file " + ioe)
      }
      case e: Throwable => {
        if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER: partition " + partitionId + " Unable to detect file as being processed " + fileName)
        if (logger.isWarnEnabled) logger.warn("SMART FILE CONSUMER: Check to make sure the input directory does not still contain this file " + e)
      }
    }
  }

  /**
    * This is the "FILE CONSUMER"
    */
  private def doSomeConsuming(): Unit = {
    while (isConsuming && stillConsuming && !LocationWatcher.shutdown) {

      var curTimeStart: Long = 0
      var curTimeEnd: Long = 0

      val fileToProcess = FileProcessor.deQFile
      if (fileToProcess == null) {
        Thread.sleep(500)
      } else {
        if (logger.isInfoEnabled) logger.info("SMART_FILE_CONSUMER partition " + partitionId + " Processing file " + fileToProcess)

        //val tokenName = fileToProcess.name.split("/")
        //FileProcessor.markFileProcessing(tokenName(tokenName.size - 1), fileToProcess.offset, fileToProcess.createDate)
        FileProcessor.markFileProcessing(fileToProcess.name, fileToProcess.offset, fileToProcess.createDate)

        curTimeStart = System.currentTimeMillis


        try {
          readBytesChunksFromFile(fileToProcess)
        } catch {
          case fnfe: Exception => {
            logger.error("Exception Encountered, check the logs.", fnfe)
          }
          case e: Throwable => {
            logger.error("Exception Encountered, check the logs.", e)
          }
        }
        curTimeEnd = System.currentTimeMillis
      }
      stillConsuming = FileProcessor.isThisNodeReadyToProcess && FileProcessor.pcbw != null && FileProcessor.pcbw.IsThisNodeToProcess()
    }
  }

  /**
    * This is a "PUSHER" file.
    */
  private def doSomePushing(): Unit = {
    var contunueToWork = true
    while (isProducing && contunueToWork && !LocationWatcher.shutdown) {
      var msg = deQMsg
      if (msg == null) {
        contunueToWork = stillConsuming
        if (contunueToWork)
          Thread.sleep(250)
      } else {
        kml.pushData(msg)
        msg = null
      }
    }
  }


  /**
    * The main directory watching thread
    */
  def run(): Unit = {
    stillConsuming = true
    isConsuming = true
    isProducing = true
    // Initialize and launch the File Processor thread(s), and kafka producers
    fileConsumers.execute(new Runnable() {
      override def run() = {
        try {
          doSomeConsuming
        } catch {
          case e: Exception => {
            logger.error("Failure", e)
          }
          case e: Throwable => {
            logger.error("Failure", e)
          }
        }
      }
    })

    fileConsumers.execute(new Runnable() {
      override def run() = {
        try {
          doSomePushing
        } catch {
          case e: Exception => {
            logger.error("Failure", e)
          }
          case e: Throwable => {
            logger.error("Failure", e)
          }
        }
      }
    })
  }

  /**
    *
    * @param inputfile
    * @return
    */
  private def isCompressed(inputfile: String): Boolean = {
    var is: FileInputStream = null
    try {
      FileProcessor.testFailure("TEST EXCEPTION... checking for Compression")
      is = new FileInputStream(inputfile)

      val maxlen = 2
      val buffer = new Array[Byte](maxlen)
      val readlen = is.read(buffer, 0, maxlen)

      if (readlen < 2)
        return false;

      val b0: Int = buffer(0)
      val b1: Int = buffer(1)

      val head = (b0 & 0xff) | ((b1 << 8) & 0xff00)

      return (head == GZIPInputStream.GZIP_MAGIC);
    } catch {
      case e: Throwable => {
        logger.error("Access to file failed during Compression check", e)
        throw e;
      }
    } finally {
      try {
        if (is != null)
          is.close
        is = null
      } catch {
        case e1: Throwable => {
          if (logger.isWarnEnabled) logger.warn("Error while closing the file", e1)
          is = null
        }
      }
    }

    return false;
  }

  /**
    *
    */
  def shutdown: Unit = {
    FileProcessor.shutdownInputWatchers
    FileProcessor.scheduledThreadPool.shutdownNow()
    isConsuming = false
    isProducing = false
    stillConsuming = false

    if (fileConsumers != null) {
      fileConsumers.shutdown()
    }
    fileConsumers = null

    if (workerBees != null)
      workerBees.shutdownNow()
    workerBees = null

    MetadataAPIImpl.shutdown
    if (zkc != null)
      zkc.close
    zkc = null
    if (kml != null)
      kml.shutdown
    kml = null
    Thread.sleep(2000)
  }
}
