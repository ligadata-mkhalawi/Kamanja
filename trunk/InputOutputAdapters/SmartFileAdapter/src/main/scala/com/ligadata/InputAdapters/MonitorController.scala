package com.ligadata.InputAdapters

import scala.actors.threadpool.{TimeUnit => STimeUnit}
import java.io.IOException
import java.util.concurrent.TimeUnit

import com.ligadata.AdaptersConfiguration.{LocationInfo, SmartFileAdapterConfiguration}
import com.ligadata.Exceptions.KamanjaException
import com.ligadata.Utils.Utils
import org.apache.logging.log4j.LogManager

import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.ArrayBuffer


case class MonitoredFile(path: String, parent: String, lastModificationTime: Long,
                         lastReportedSize: Long, isDirectory: Boolean, isFile: Boolean)

/**
  *
  * adapterConfig
  * newFileDetectedCallback callback to notify leader whenever a file is detected
  */
class MonitorController {

  def this(adapterConfig: SmartFileAdapterConfiguration, parentSmartFileConsumer: SmartFileConsumer,
           newFileDetectedCallback: (String) => Unit) {
    this()

    this.adapterConfig = adapterConfig
    this.newFileDetectedCallback = newFileDetectedCallback
    this.parentSmartFileConsumer = parentSmartFileConsumer

    commonFileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, "/")
  }

  val NOT_RECOVERY_SITUATION = -1

  private var adapterConfig: SmartFileAdapterConfiguration = null
  private var newFileDetectedCallback: (String) => Unit = null
  private var parentSmartFileConsumer: SmartFileConsumer = null

  private val bufferingQ_map: scala.collection.mutable.Map[String, (MonitoredFile, Int, Boolean)] = scala.collection.mutable.Map[String, (MonitoredFile, Int, Boolean)]()
  private val bufferingQLock = new Object

  private var commonFileHandler: SmartFileHandler = null
  private var monitoringThreadsFileHandlers: Array[SmartFileHandler] = null

  implicit def orderedEnqueuedFileHandler(f: EnqueuedFileHandler): Ordered[EnqueuedFileHandler] = new Ordered[EnqueuedFileHandler] {
    def compare(other: EnqueuedFileHandler) = {
      val locationInfo1 = f.locationInfo //parentSmartFileConsumer.getDirLocationInfo(MonitorUtils.simpleDirPath(f.fileHandler.getParentDir))
      val locationInfo2 = other.locationInfo // parentSmartFileConsumer.getDirLocationInfo(MonitorUtils.simpleDirPath(other.fileHandler.getParentDir))
      //not sure why but had to invert sign
      (MonitorUtils.compareFiles(f, other)) * -1
    }
  }

  private val ignoredFiles = scala.collection.mutable.LinkedHashMap[String, Long]()
  private val ignoredFilesMaxSize = 100000

  private var fileQ: scala.collection.mutable.PriorityQueue[EnqueuedFileHandler] =
  //new scala.collection.mutable.PriorityQueue[EnqueuedFileHandler]()(Ordering.by(fileComparisonField))
    new scala.collection.mutable.PriorityQueue[EnqueuedFileHandler]() //use above implicit compare function

  private val fileQLock = new Object

  private var refreshRate: Int = 1000
  //Refresh rate for monitorBufferingFiles
  private var bufferTimeout: Int = 300000
  // Default to 5 minutes
  private var maxTimeFileAllowedToLive: Int = 3000
  // default to 50 minutes.. will be multiplied by 1000 later
  private var maxBufferErrors = 5

  private var keepMontoringBufferingFiles = false
  //var globalFileMonitorService: ExecutorService = Executors.newFixedThreadPool(2)

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  private var initialFiles: List[String] = null

  private var isShutdown = false

  def init(files: List[String]): Unit = {
    initialFiles = files

  }

  def checkConfigDirsAccessibility(): Unit = {

    adapterConfig.monitoringConfig.detailedLocations.foreach(location => {
      //val srcHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, location.srcDir)
      if (!commonFileHandler.exists(location.srcDir))
        throw new KamanjaException("Smart File Consumer - Dir to watch (" + location.srcDir + ") does not exist", null)
      /*else if(!srcHandler.isAccessible)
        throw new KamanjaException("Smart File Consumer - Dir to watch (" + location.srcDir + ") is not accessible. It must be readable and writable", null)
      */

      if (location.isMovingEnabled) {
        //val targetHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, location.targetDir)
        if (!commonFileHandler.exists(location.targetDir))
          throw new KamanjaException("Smart File Consumer - Target Dir (" + location.targetDir + ") does not exist", null)
        /*else if (!targetHandler.isAccessible)
          throw new KamanjaException("Smart File Consumer - Target Dir (" + location.targetDir + ") is not accessible. It must be readable and writable", null)
          */
      }
    })

  }

  def markFileAsProcessed(filePath: String): Unit = {
  }

  def startMonitoring(): Unit = {
    keepMontoringBufferingFiles = true
    monitor()
  }

  private var monitorsExecutorService: ExecutorService = null

  def monitor(): Unit = {
    val monitoringConf = adapterConfig.monitoringConfig
    val maxThreadCount = Math.min(monitoringConf.monitoringThreadsCount, monitoringConf.detailedLocations.length)
    monitorsExecutorService = Executors.newFixedThreadPool(maxThreadCount)
    logger.info("Smart File Monitor - running {} threads to monitor {} dirs",
      monitoringConf.monitoringThreadsCount.toString, monitoringConf.detailedLocations.length.toString)

    monitoringThreadsFileHandlers = new Array[SmartFileHandler](maxThreadCount)
    for (currentThreadId <- 0 until maxThreadCount) {
      if (! isShutdown)
        monitoringThreadsFileHandlers(currentThreadId) = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, "/")
    }

    val monitoredDirsQueue = new MonitoredDirsQueue()
    monitoredDirsQueue.init(monitoringConf.detailedLocations, monitoringConf.waitingTimeMS)
    for (currentThreadId <- 0 until maxThreadCount) {
      val dirMonitorThread = new Runnable() {
        override def run() = {
          try {

            while (keepMontoringBufferingFiles && !isShutdown) {
              logger.debug("waitingFilesToProcessCount={}, dirCheckThreshold={}",
                waitingFilesToProcessCount.toString, adapterConfig.monitoringConfig.dirCheckThreshold.toString)

              //start/stop listing folders contents based on current number of waiting files compared to a threshold
              if (adapterConfig.monitoringConfig.dirCheckThreshold > 0 &&
                waitingFilesToProcessCount > adapterConfig.monitoringConfig.dirCheckThreshold) {

                logger.info("Smart File Monitor - too many files already in process queue. monitoring thread {} is sleeping for {} ms", currentThreadId.toString, monitoringConf.waitingTimeMS.toString)
                try {
                  Thread.sleep(monitoringConf.waitingTimeMS)
                }
                catch {
                  case ex: Throwable =>
                }
              }
              else {
                val dirQueuedInfo = monitoredDirsQueue.getNextDir()
                if (dirQueuedInfo != null) {
                  val location = dirQueuedInfo._1
                  val isFirstScan = dirQueuedInfo._3
                  val srcDir = location.srcDir

                  if (keepMontoringBufferingFiles && !isShutdown)
                    monitorBufferingFiles(currentThreadId, srcDir, location, isFirstScan)

                  val updateDirQueuedInfo = (dirQueuedInfo._1, dirQueuedInfo._2, false) //not first scan anymore
                  monitoredDirsQueue.reEnqueue(updateDirQueuedInfo) // so the folder gets monitored again

                  /*if (keepMontoringBufferingFiles && !isShutdown) {
                    logger.debug("Smart File Monitor - finished checking dir sleeping", currentThreadId.toString, monitoringConf.waitingTimeMS.toString)
                    try {
                      Thread.sleep(monitoringConf.waitingTimeMS)
                    }
                    catch {
                      case ex: Throwable =>
                    }
                  }*/
                }
                else {
                  //happens if last time queue head dir was monitored was less than waiting time
                  logger.info("Smart File Monitor - no folders to monitor for now. Thread {} is sleeping for {} ms", currentThreadId.toString, monitoringConf.waitingTimeMS.toString)
                  try {
                    Thread.sleep(monitoringConf.waitingTimeMS)
                  }
                  catch {
                    case ex: Throwable =>
                  }
                }
              }
            }
          }
          catch {
            case ex: Exception =>
              logger.error("Smart File Monitor - Error", ex)

            case ex: Throwable =>
              logger.error("Smart File Monitor - Error", ex)

          }
        }
      }
      monitorsExecutorService.execute(dirMonitorThread)
    }
  }

  def listFiles(path: String): Array[String] = {
    val files = commonFileHandler.listFiles(path, adapterConfig.monitoringConfig.dirMonitoringDepth)
    if (files != null) files.map(file => file.path)
    else Array[String]()
  }

  def stopMonitoring(): Unit = {
    isShutdown = true

    logger.warn("Adapter {} - MonitorController - shutting down called", adapterConfig.Name)


    keepMontoringBufferingFiles = false
    if (monitorsExecutorService != null){
      Utils.shutdownAndAwaitTermination(monitorsExecutorService,"MonitorController thread",10000)
    }

    monitorsExecutorService = null

    if (monitoringThreadsFileHandlers != null) {
      monitoringThreadsFileHandlers.foreach(handler => {
        try {
          handler.shutdown()
          handler.disconnect()
        }
        catch {
          case ex: Throwable =>
        }
      })
    }

    if (commonFileHandler != null) {
      commonFileHandler.shutdown()
      commonFileHandler.disconnect()
    }
    commonFileHandler = null

    logger.warn("Adapter {} - MonitorController - shutting down finished", adapterConfig.Name)
  }

  private def enQBufferedFile(file: MonitoredFile, initiallyExists: Boolean): Unit = {
    bufferingQLock.synchronized {
      logger.debug("adapter {} - enqueuing file {} into tmp buffering queue", adapterConfig.Name, file.path)
      //val fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, file.path)
      bufferingQ_map(file.path) = (file, 0, initiallyExists)
    }
  }

  /**
    * Look at the files on the DEFERRED QUEUE... if we see that it stops growing, then move the file onto the READY
    * to process QUEUE.
    */
  private def monitorBufferingFiles(currentThreadId: Int, dir: String, locationInfo: LocationInfo, isFirstScan: Boolean): Unit = {
    // This guys will keep track of when to exgernalize a WARNING Message.  Since this loop really runs every second,
    // we want to throttle the warning messages.
    logger.debug("SMART FILE CONSUMER (MonitorController):  monitorBufferingFiles. dir = " + dir)

    var specialWarnCounter: Int = 1

    if (!keepMontoringBufferingFiles || isShutdown)
      return

    // Scan all the files that we are buffering, if there is not difference in their file size.. move them onto
    // the FileQ, they are ready to process.

    {
      val newlyAdded = ArrayBuffer[String]()
      val removedEntries = ArrayBuffer[String]()

      val currentAllChilds =
        try {
          if(!isShutdown)
            monitoringThreadsFileHandlers(currentThreadId).listFiles(dir, adapterConfig.monitoringConfig.dirMonitoringDepth)
          else Array[MonitoredFile]()
        }
        catch{
          case ex : Throwable =>
            logger.error("", ex)
            Array[MonitoredFile]()
        }
      //val (currentDirectFiles, currentDirectDirs) = separateFilesFromDirs(currentAllChilds)

      currentAllChilds.foreach(currentMonitoredFile => {
        if (!isShutdown && keepMontoringBufferingFiles && currentMonitoredFile != null && currentMonitoredFile.isFile) {
          val filePath = currentMonitoredFile.path
          try {
            var thisFileNewLength: Long = 0
            var thisFilePreviousLength: Long = 0
            var thisFileFailures: Int = 0
            val thisFileStarttime = currentMonitoredFile.lastModificationTime //todo

            val currentFileParentDir = currentMonitoredFile.parent
            val currentFileLocationInfo = parentSmartFileConsumer.getDirLocationInfo(currentFileParentDir)

            if (isEnqueued(filePath)) {
              logger.info("SMART FILE CONSUMER (MonitorController):  File already enqueued " + filePath)
            }
            else if (parentSmartFileConsumer.isInProcessingQueue(filePath)) {
              logger.info("SMART FILE CONSUMER (MonitorController):  File already in processing queue " + filePath)
            }
            else {
              if (!bufferingQ_map.contains(filePath)) {
                val isValid = MonitorUtils.isValidFile(adapterConfig.Name,
                  monitoringThreadsFileHandlers(currentThreadId), filePath, currentFileLocationInfo, ignoredFiles,
                  false, adapterConfig.monitoringConfig.checkFileTypes)
                fixIgnoredFiles()

                if(isValid)
                  enQBufferedFile(currentMonitoredFile, isFirstScan)
                else{
                  logger.debug("Adapter {} - file {} is invalid to be queued", adapterConfig.Name, filePath)
                }
              }
              else {
                try {

                  logger.debug("SMART FILE CONSUMER (MonitorController):  monitorBufferingFiles - file " + currentMonitoredFile.path)

                  if (isFirstScan && initialFiles != null && initialFiles.contains(filePath)) {
                    logger.debug("SMART FILE CONSUMER (MonitorController): file {} is already in initial files", filePath)
                    removedEntries += filePath
                    logger.debug("SMART FILE CONSUMER (MonitorController): now initialFiles = {}", initialFiles)
                  }
                  else {
                    if (!isShutdown) {
                      thisFileNewLength = currentMonitoredFile.lastReportedSize
                      val previousMonitoredFile = bufferingQ_map(filePath)
                      val thisFilePreviousLength = previousMonitoredFile._1.lastReportedSize
                      thisFileFailures = previousMonitoredFile._2

                      // If file hasn't grown in the past  seconds - either a delay OR a completed transfer.
                      if (thisFilePreviousLength == thisFileNewLength) {
                        logger.debug("SMART FILE CONSUMER (MonitorController):  File {} size has not changed", filePath)

                        if (thisFilePreviousLength > 0) {
                          if (isEnqueued(filePath)) {
                            logger.debug("SMART FILE CONSUMER (MonitorController):  File already enqueued " + filePath)
                          } else {
                            logger.info("SMART FILE CONSUMER (MonitorController):  File READY TO PROCESS " + filePath)
                            enQFile(filePath, NOT_RECOVERY_SITUATION, currentMonitoredFile.lastModificationTime)
                            newlyAdded.append(filePath)
                          }
                          // bufferingQ_map.remove(fileTuple._1)
                          removedEntries += filePath
                        } else {
                          // Here becayse either the file is sitll of len 0,or its deemed to be invalid.
                          if (thisFilePreviousLength == 0) {
                            if (!isShutdown) {
                              val diff = System.currentTimeMillis - thisFileStarttime //d.lastModified
                              if (diff > bufferTimeout) {
                                logger.warn("SMART FILE CONSUMER (MonitorController): Detected that " + filePath + " has been on the buffering queue longer then " + bufferTimeout / 1000 + " seconds - Cleaning up")

                                if (!isShutdown && (currentFileLocationInfo != null && currentFileLocationInfo.isMovingEnabled)) {
                                  try {
                                    parentSmartFileConsumer.moveFile(filePath)
                                  }
                                  catch {
                                    case e: Exception =>
                                  }
                                }
                                else
                                  logger.info("SMART FILE CONSUMER (MonitorController): File {} will not be moved since moving is disabled for folder {} - Adapter {}",
                                    filePath, currentFileParentDir, adapterConfig.Name)

                                // bufferingQ_map.remove(fileTuple._1)
                                removedEntries += filePath
                              }
                            }
                          } else {
                            //Invalid File - due to content type
                            if (!isShutdown && (currentFileLocationInfo != null && currentFileLocationInfo.isMovingEnabled)) {
                              logger.error("SMART FILE CONSUMER (MonitorController): Moving out " + filePath + " with invalid file type ")
                              try {
                                parentSmartFileConsumer.moveFile(filePath)
                              }
                              catch {
                                case e: Exception =>
                              }
                            }
                            else {
                              logger.info("SMART FILE CONSUMER (MonitorController): File {} has invalid file type but will not be moved since moving is disabled for folder {} - Adapter {}",
                                filePath, currentFileParentDir, adapterConfig.Name)
                            }
                            // bufferingQ_map.remove(fileTuple._1)
                            removedEntries += filePath
                          }
                        }
                      } else {
                        logger.debug("SMART FILE CONSUMER (MonitorController):  File {} size changed from {} to {}",
                          filePath, thisFilePreviousLength.toString, thisFileNewLength.toString)

                        bufferingQ_map(filePath) = (currentMonitoredFile, thisFileFailures, isFirstScan)
                      }
                    }
                  }
                } catch {
                  case fnfe: java.io.FileNotFoundException => {
                    logger.warn("SMART FILE CONSUMER (MonitorController): Detected that file " + filePath + " no longer exists")
                    removedEntries += filePath
                  }
                  case ioe: IOException => {
                    if (!isShutdown) {
                      thisFileFailures += 1
                      if (((System.currentTimeMillis - thisFileStarttime) > maxTimeFileAllowedToLive && thisFileFailures > maxBufferErrors)) {
                        logger.warn("SMART FILE CONSUMER (MonitorController): Detected that a stuck file " + filePath + " on the buffering queue", ioe)
                        try {
                          removedEntries += filePath

                          if (currentFileLocationInfo != null && currentFileLocationInfo.isMovingEnabled)
                            parentSmartFileConsumer.moveFile(filePath)
                          // bufferingQ_map.remove(fileTuple._1)
                        } catch {
                          case e: Throwable => {
                            logger.error("SMART_FILE_CONSUMER: Failed to move file, retyring", e)
                          }
                        }
                      } else {
                        //bufferingQ_map(fileTuple._1) = (thisFileOrigLength, thisFileStarttime, thisFileFailures, initiallyExists)
                        bufferingQ_map(filePath) = (currentMonitoredFile, thisFileFailures, isFirstScan)
                        logger.warn("SMART_FILE_CONSUMER: IOException trying to monitor the buffering queue ", ioe)
                      }
                    }
                  }
                  case e: Throwable => {
                    if (!isShutdown) {
                      thisFileFailures += 1
                      if (((System.currentTimeMillis - thisFileStarttime) > maxTimeFileAllowedToLive && thisFileFailures > maxBufferErrors)) {
                        logger.error("SMART FILE CONSUMER (MonitorController): Detected that a stuck file " + filePath + " on the buffering queue", e)
                        try {
                          removedEntries += filePath

                          if (currentFileLocationInfo != null && currentFileLocationInfo.isMovingEnabled)
                            parentSmartFileConsumer.moveFile(filePath)

                        } catch {
                          case ee: Throwable => {
                            logger.error("SMART_FILE_CONSUMER (MonitorController): Failed to move file, retyring", ee)
                          }
                        }
                      } else {
                        //bufferingQ_map(fileTuple._1) = (thisFileOrigLength, thisFileStarttime, thisFileFailures, initiallyExists)
                        bufferingQ_map(filePath) = (currentMonitoredFile, thisFileFailures, isFirstScan)
                        logger.error("SMART_FILE_CONSUMER: IOException trying to monitor the buffering queue ", e)
                      }
                    }
                  }
                }
              }
            }
          }
          catch {
            case e: Throwable => {
              logger.error("Smart File Adapter (MonitorController) - Failed to check for entry in bufferingQ_map", e)
            }
          }
        }
      })

      newlyAdded.foreach(filePath => {
        if (!isShutdown) {
          //notify leader about the new files
          if (newFileDetectedCallback != null) {
            logger.debug("Smart File Adapter (MonitorController) - New file is enqueued in monitor controller queue ({})", filePath)
            newFileDetectedCallback(filePath)
          }
        }
      })

      try {
        bufferingQ_map --= removedEntries
      } catch {
        case e: Throwable => {
          logger.error("Smart File Adapter (MonitorController) - Failed to remove entries from bufferingQ_map", e)
        }
      }
    }


  }

  def updateMonitoredFile(file: MonitoredFile, newSize: Long, newModTime: Long): MonitoredFile = {
    MonitoredFile(file.path, file.parent, newModTime, newSize, file.isDirectory, file.isFile)
  }

  def fixIgnoredFiles(): Unit ={
    if(ignoredFiles == null) return
    if(ignoredFiles.size > ignoredFilesMaxSize){
      ignoredFiles.drop(ignoredFiles.size - ignoredFilesMaxSize)
    }
  }

  private def enQFile(file: String, offset: Int, createDate: Long): Unit = {
    if (isShutdown) return
    try {
      fileQLock.synchronized {
        logger.info("SMART FILE CONSUMER (MonitorController):  enq file " + file + " with priority " + createDate + " --- curretnly " + fileQ.size + " files on a QUEUE")
        if (!isShutdown) {
          val fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, file, false)
          val locationInfo = parentSmartFileConsumer.getDirLocationInfo(MonitorUtils.simpleDirPath(fileHandler.getParentDir))
          if (locationInfo == null)
            logger.error("Adapter {} - file {} has no location config. not going to enqueue in file queue",
              adapterConfig.Name, file)
          else {
            val components = MonitorUtils.getFileComponents(fileHandler.getFullPath, locationInfo)
            if (!isShutdown)
              fileQ += new EnqueuedFileHandler(fileHandler, offset, createDate, locationInfo, components)
          }
        }
      }
    }
    catch{
      case ex : Throwable =>
        logger.error("Adapter {} - Error: ", ex)
        throw  ex //todo : remove throwing exception, but how to keep error from showing repeatedly
    }
  }

  private def isEnqueued(file: String): Boolean = {
    if (isShutdown) return false
    fileQLock.synchronized {
      if (fileQ.isEmpty) {
        return false
      }
      fileQ.exists(f => f.fileHandler.getFullPath.equals(file))
    }
  }

  private def deQFile: EnqueuedFileHandler = {
    if (isShutdown) return null
    fileQLock.synchronized {
      if (fileQ.isEmpty) {
        return null
      }
      val ef = fileQ.dequeue()
      logger.info("SMART FILE CONSUMER (MonitorController):  deq file " + ef.fileHandler.getFullPath + " with priority " + ef.lastModifiedDate + " --- curretnly " + fileQ.size + " files left on a QUEUE")
      return ef

    }
  }

  private def waitingFilesToProcessCount: Int = {
    fileQLock.synchronized {
      fileQ.length
    }
  }

  //get file name only for now
  def getNextFileToProcess: String = {
    val f = deQFile
    if (f == null) null else f.fileHandler.getFullPath
  }

}
