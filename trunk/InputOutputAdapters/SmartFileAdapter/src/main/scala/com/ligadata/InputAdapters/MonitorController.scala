package com.ligadata.InputAdapters

import java.io.IOException

import com.ligadata.AdaptersConfiguration.{LocationInfo, SmartFileAdapterConfiguration}
import com.ligadata.Exceptions.KamanjaException
import org.apache.logging.log4j.LogManager
import org.json4s.native.JsonMethods._

import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.ArrayBuffer

case class PatternInfo(patternString: String, pattern: scala.util.matching.Regex, patternMatchGroupNumber: Int)

case class GroupsInfoList(onlyBaseFile: Boolean, pathSeparator: String, patterns: Array[PatternInfo])

case class MonitoredFile(path: String, parent: String, lastModificationTime: Long,
                         lastReportedSize: Long, isDirectory: Boolean, isFile: Boolean)

/**
  *
  * adapterConfig
  * newFileDetectedCallback callback to notify leader whenever a file is detected
  */
class MonitorController {

  def this(adapterConfig: SmartFileAdapterConfiguration, parentSmartFileConsumer: SmartFileConsumer,
           newFileDetectedCallback: () => Unit) {
    this()

    this.adapterConfig = adapterConfig
    this.newFileDetectedCallback = newFileDetectedCallback
    this.parentSmartFileConsumer = parentSmartFileConsumer

    commonFileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, "/")
  }

  val NOT_RECOVERY_SITUATION = -1

  private var adapterConfig: SmartFileAdapterConfiguration = null
  private var newFileDetectedCallback: () => Unit = null
  private var parentSmartFileConsumer: SmartFileConsumer = null

  private val bufferingQ_map: scala.collection.mutable.Map[String, (MonitoredFile, Int, Boolean)] = scala.collection.mutable.Map[String, (MonitoredFile, Int, Boolean)]()
  private val bufferingQLock = new Object

  private var commonFileHandler: SmartFileHandler = null
  private var monitoringThreadsFileHandlers: Array[SmartFileHandler] = null

  implicit def orderedEnqueuedFileHandler(f: EnqueuedFileHandler): Ordered[EnqueuedFileHandler] = new Ordered[EnqueuedFileHandler] {
    def compare(other: EnqueuedFileHandler) = {
      // val locationInfo1 = f.locationInfo //parentSmartFileConsumer.getDirLocationInfo(MonitorUtils.simpleDirPath(f.fileHandler.getParentDir))
      // val locationInfo2 = other.locationInfo // parentSmartFileConsumer.getDirLocationInfo(MonitorUtils.simpleDirPath(other.fileHandler.getParentDir))
      //not sure why but had to invert sign
      (MonitorUtils.compareFiles(f, other)) * -1
    }
  }

  implicit def orderedEnqueuedGroupHandler(f: EnqueuedGroupHandler): Ordered[EnqueuedGroupHandler] = new Ordered[EnqueuedGroupHandler] {
    def compare(other: EnqueuedGroupHandler) = {
      //BUGBUG:: For now we are using first one
      (MonitorUtils.compareFiles(f.fileHandlers(0), other.fileHandlers(0))) * -1
    }
  }

  private val ignoredFiles = scala.collection.mutable.LinkedHashMap[String, Long]()
  private val ignoredFilesMaxSize = 100000

  // private var fileQ: scala.collection.mutable.PriorityQueue[EnqueuedFileHandler] =
  //   new scala.collection.mutable.PriorityQueue[EnqueuedFileHandler]()(Ordering.by(fileComparisonField))
  //   new scala.collection.mutable.PriorityQueue[EnqueuedFileHandler]() //use above implicit compare function

  private var groupQ: scala.collection.mutable.PriorityQueue[EnqueuedGroupHandler] =
    new scala.collection.mutable.PriorityQueue[EnqueuedGroupHandler]() //use above implicit compare function

  // private val fileQLock = new Object
  private val groupQLock = new Object

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

  def markFileAsProcessed(files: List[String]): Unit = {
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
    if (logger.isInfoEnabled) logger.info("Smart File Monitor - running {} threads to monitor {} dirs",
      monitoringConf.monitoringThreadsCount.toString, monitoringConf.detailedLocations.length.toString)

    monitoringThreadsFileHandlers = new Array[SmartFileHandler](maxThreadCount)
    for (currentThreadId <- 0 until maxThreadCount) {
      if (!isShutdown)
        monitoringThreadsFileHandlers(currentThreadId) = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, "/")
    }

    var groupsInfo: GroupsInfoList = null
    if (monitoringConf.filesGroupsInfoJsonString != null) {
      if (logger.isDebugEnabled) logger.debug("filesGroupsInfoJsonString:" + monitoringConf.filesGroupsInfoJsonString)
      try {
        val grpsInfo = parse(monitoringConf.filesGroupsInfoJsonString)
        if (grpsInfo == null || grpsInfo.values == null) {
          logger.error("Failed to parse string %s for GroupsInfo".format(monitoringConf.filesGroupsInfoJsonString))
        } else {
          val grpsInfoMap = grpsInfo.values.asInstanceOf[Map[String, Any]]

          val onlyBaseFile: Boolean = grpsInfoMap.getOrElse("ConsiderOnlyBaseFileName", "false").toString.toBoolean
          var pathSeparator: String = grpsInfoMap.getOrElse("PathSeparator", "/").toString
          var patternInfo = grpsInfoMap.getOrElse("PatternInfo", null)

          var patLst: List[Any] = List[Any]()
          if (patternInfo.isInstanceOf[List[Any]]) {
            patLst = patternInfo.asInstanceOf[List[Any]]
          } else if (grpsInfo.isInstanceOf[Map[String, Any]]) {
            patLst = List[Any](patternInfo)
          } else {
            logger.error("Ignoring PatternInfo, because we did not found valid list of groups in PatternInfo %s".format(monitoringConf.filesGroupsInfoJsonString))
          }

          val finalGroups = ArrayBuffer[PatternInfo]()
          var idx = 0
          patLst.foreach(patInfo => {
            try {
              if (patInfo.isInstanceOf[Map[String, Any]]) {
                val grpCfgValues = patInfo.asInstanceOf[Map[String, Any]]
                val patternString: String = grpCfgValues.getOrElse("PatternString", "").toString
                if (patternString.length == 0) {
                  logger.error("Not found PatternString in %s for PatternInfo at index %d. So, ignoring PatternInfo".format(monitoringConf.filesGroupsInfoJsonString, idx))
                } else {
                  var patternMatchGroupNumber: Int = grpCfgValues.getOrElse("PatternMatchGroupNumber", "0").toString.toInt
                  if (patternMatchGroupNumber < 0) {
                    logger.error("Not found valid patternMatchGroupNumber (%s) in %s for PatternInfo at index %d. So, ignoring PatternInfo".format(patternMatchGroupNumber, monitoringConf.filesGroupsInfoJsonString, idx))
                  } else {
                    val pattern: scala.util.matching.Regex = patternString.r
                    finalGroups += PatternInfo(patternString, pattern, patternMatchGroupNumber)
                  }
                }
              } else {
                logger.error("PatternInfo is not valid Map at index %d in %s for PatternInfo. So, ignoring PatternInfo".format(idx, monitoringConf.filesGroupsInfoJsonString))
              }
            } catch {
              case e: Throwable => {
                logger.error("PatternInfo got exception at index %d in %s for PatternInfo. So, ignoring PatternInfo".format(idx, monitoringConf.filesGroupsInfoJsonString), e)
              }
            }
            idx += 1
          })
          groupsInfo = GroupsInfoList(onlyBaseFile, pathSeparator, finalGroups.toArray)
          if (logger.isDebugEnabled) logger.debug("GroupsInfoList:" + groupsInfo)
        }
      } catch {
        case e: Throwable => {
          logger.error("Failed to parse string %s for GroupsInfo".format(monitoringConf.filesGroupsInfoJsonString))
        }
      }
    }

    val monitoredDirsQueue = new MonitoredDirsQueue()
    monitoredDirsQueue.init(monitoringConf.detailedLocations, monitoringConf.waitingTimeMS)
    for (currentThreadId <- 0 until maxThreadCount) {
      val dirMonitorThread = new Runnable() {
        override def run() = {
          try {

            while (keepMontoringBufferingFiles && !isShutdown) {
              if (logger.isDebugEnabled) logger.debug("waitingGroupsToProcessCount={}, dirCheckThreshold={}",
                waitingGroupsToProcessCount.toString, adapterConfig.monitoringConfig.dirCheckThreshold.toString)

              //start/stop listing folders contents based on current number of waiting files compared to a threshold
              if (adapterConfig.monitoringConfig.dirCheckThreshold > 0 &&
                waitingGroupsToProcessCount > adapterConfig.monitoringConfig.dirCheckThreshold) {

                if (logger.isInfoEnabled) logger.info("Smart File Monitor - too many files already in process queue. monitoring thread {} is sleeping for {} ms", currentThreadId.toString, monitoringConf.waitingTimeMS.toString)
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
                    monitorBufferingFiles(currentThreadId, srcDir, location, isFirstScan, groupsInfo)

                  val updateDirQueuedInfo = (dirQueuedInfo._1, dirQueuedInfo._2, false) //not first scan anymore
                  monitoredDirsQueue.reEnqueue(updateDirQueuedInfo) // so the folder gets monitored again

                  /*if (keepMontoringBufferingFiles && !isShutdown) {
                    if (logger.isDebugEnabled) logger.debug("Smart File Monitor - finished checking dir sleeping", currentThreadId.toString, monitoringConf.waitingTimeMS.toString)
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
                  if (logger.isInfoEnabled) logger.info("Smart File Monitor - no folders to monitor for now. Thread {} is sleeping for {} ms", currentThreadId.toString, monitoringConf.waitingTimeMS.toString)
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

    if (logger.isWarnEnabled) logger.warn("Adapter {} - MonitorController - shutting down called", adapterConfig.Name)


    keepMontoringBufferingFiles = false
    if (monitorsExecutorService != null)
      monitorsExecutorService.shutdownNow()
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

    if (logger.isWarnEnabled) logger.warn("Adapter {} - MonitorController - shutting down finished", adapterConfig.Name)
  }

  private def enQBufferedFile(file: MonitoredFile, initiallyExists: Boolean): Unit = {
    bufferingQLock.synchronized {
      if (logger.isDebugEnabled) logger.debug("adapter {} - enqueuing file {} into tmp buffering queue", adapterConfig.Name, file.path)
      //val fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, file.path)
      bufferingQ_map(file.path) = (file, 0, initiallyExists)
    }
  }

  private def enQBufferedFiles(files: Array[MonitoredFile], initiallyExists: Boolean): Unit = {
    bufferingQLock.synchronized {
      //BUGBUG:: Not considering failed count
      files.foreach(file => {
        bufferingQ_map(file.path) = (file, 0, initiallyExists)
      })
    }
  }

  private def getAllEnqueuedBufferedFiles: Map[String, (MonitoredFile, Int, Boolean)] = {
    bufferingQLock.synchronized {
      bufferingQ_map.toMap
    }
  }

  private def removeFilesFromBufferingQ(removedEntries: Array[String]): Unit = {
    bufferingQLock.synchronized {
      bufferingQ_map --= removedEntries
    }
  }

  private def getBaseFileName(fullPath: String, pathSeparator: String): String = {
    val endIndex = fullPath.lastIndexOf(pathSeparator)
    var retStr = "empty"
    if (endIndex != -1) {
      fullPath.substring(endIndex + 1, fullPath.size)
    } else {
      fullPath
    }
  }

  private def extractFileFromPattern(grpInfoLst: GroupsInfoList, flName: String): String = {
    var retVal = flName
    try {
      var lstIdx = 0
      var foundVal = false
      while (!foundVal && lstIdx < grpInfoLst.patterns.size) {
        val patInfo = grpInfoLst.patterns(lstIdx)
        try {
          val allGrps = patInfo.pattern.findFirstMatchIn(flName)
          // BUGBUG:: May be it is better to check whether we have given MatchGroupNumber or not
          if (allGrps != None) {
            retVal = allGrps.map(_ group patInfo.patternMatchGroupNumber).get
            if (logger.isTraceEnabled) logger.trace("FileName:" + flName + ", matched patterns:" + allGrps + ", found value:" + retVal + " for pattern :" + patInfo.patternString)
            foundVal = true
          } else {
            if (logger.isTraceEnabled) logger.trace("FileName:" + flName + ", matched patterns:" + allGrps + ", not found value for pattern :" + patInfo.patternString)
          }
        } catch {
          case ex: Throwable => {
            if (logger.isDebugEnabled) logger.debug("Failed to extract matched value from fileName:%s for pattern:%s of group:%d.".format(flName, patInfo.patternString, patInfo.patternMatchGroupNumber), ex)
            // retVal = flName
          }
        }
        lstIdx += 1
      }
    } catch {
      case ex: Throwable => {
        logger.error("Failed to extract matched value from fileName. Using filename:%s as base as it is".format(flName), ex)
        retVal = flName
      }
    }
    
    retVal
  }

  private def ProcessValidGroups(enqueuedBufferedFiles: Map[String, (MonitoredFile, Int, Boolean)], validFiles: Map[String, (MonitoredFile, LocationInfo)],
                               removedEntries: ArrayBuffer[String], isFirstScan: Boolean, fileGroups: Array[Array[MonitoredFile]]): Boolean = {
    val yetToValidateFileGroups = ArrayBuffer[Array[MonitoredFile]]()
    val finalValidFileGroups = ArrayBuffer[Array[MonitoredFile]]()
    val curTm = System.currentTimeMillis
    val zeroLenFls = ArrayBuffer[(String, Boolean)]()
    val move0LenFls = ArrayBuffer[String]()

    if (logger.isDebugEnabled) logger.debug("ValidFiles:%s".format(validFiles.keys.mkString(",")))

    fileGroups.foreach(grp => {
      var allFlsValidInGrp = true
      val grpSz = grp.length
      var idx = 0
      zeroLenFls.clear
      while (allFlsValidInGrp && idx < grpSz) {
        try {
          val fl = grp(idx)
          if (logger.isDebugEnabled) logger.debug("SMART FILE CONSUMER (MonitorController):  File {} size has not changed", fl.path)
          val validFl = validFiles.getOrElse(fl.path, null)
          if (validFl == null && allFlsValidInGrp)
            allFlsValidInGrp = false
          if (allFlsValidInGrp) {
            val thisFileNewLength = fl.lastReportedSize
            val previousMonitoredFile = enqueuedBufferedFiles(fl.path)
            val thisFilePreviousLength = previousMonitoredFile._1.lastReportedSize
            val thisFileFailures = previousMonitoredFile._2
            if (thisFilePreviousLength != thisFileNewLength)
              allFlsValidInGrp = false
            if (allFlsValidInGrp && thisFilePreviousLength == 0) {
              if ((curTm - validFl._1.lastModificationTime) <= bufferTimeout)
                allFlsValidInGrp = false
              else
                zeroLenFls += ((fl.path, (validFl._2 != null && validFl._2.isMovingEnabled)))
            }
          }
        } catch {
          case e: Throwable => {
            logger.error("Got Exception while validating group", e)
            allFlsValidInGrp = false
          }
        }
        idx += 1
      }
      if (allFlsValidInGrp) {
        if (zeroLenFls.length > 0) {
          if (zeroLenFls.length == grp.length) {
            move0LenFls ++= zeroLenFls.filter(f => f._2).map(f => f._1)
            removedEntries ++= zeroLenFls.map(f => f._1)
          } else {
            move0LenFls ++= zeroLenFls.filter(f => f._2).map(f => f._1)
            removedEntries ++= zeroLenFls.map(f => f._1)
            val excludeFlsSet = zeroLenFls.map(f => f._1).toSet
            finalValidFileGroups += grp.filter(fl => !excludeFlsSet(fl.path))
          }
        }
        else {
          finalValidFileGroups += grp
        }
      } else {
        yetToValidateFileGroups += grp
      }
    })

    if (!isShutdown) {
      if (logger.isDebugEnabled) logger.debug("yetToValidateFileGroups:%s".format(
        yetToValidateFileGroups.map(grp => grp.map(f => f.path).mkString("~")).mkString(",")))
      yetToValidateFileGroups.foreach(grp => {
        if (!isShutdown) {
          enQBufferedFiles(grp, isFirstScan)
        }
      })
    }
    
    if (!isShutdown && !move0LenFls.isEmpty) {
      if (logger.isDebugEnabled) logger.debug("move0LenFls:%s".format(
        move0LenFls.mkString(",")))
      move0LenFls.foreach(fl => {
        if (!isShutdown) {
          try {
            parentSmartFileConsumer.moveFile(fl)
          }
          catch {
            case e: Exception => {
              logger.error("Failed to move file:" + fl, e)
            }
          }
        }
      })
    }

    // Now we have valid groups finalValidFileGroups. We can add them to process. And also add them  in processing queue. So, we will ignore them in next search
    if (!isShutdown && !finalValidFileGroups.isEmpty) {
      val grps = finalValidFileGroups.map(grp => {
        new EnqueuedGroupHandler(grp.map(fl => {
          val fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, fl.path, false)
          val locationInfo = parentSmartFileConsumer.getDirLocationInfo(MonitorUtils.simpleDirPath(fileHandler.getParentDir))
          val components = MonitorUtils.getFileComponents(fileHandler.getFullPath, locationInfo)
          new EnqueuedFileHandler(fileHandler, NOT_RECOVERY_SITUATION, fl.lastModificationTime, locationInfo, components)
        }))
      }).toArray
      if (!isShutdown) {
        logger.warn("enq groups:%s".format(
          grps.map(grp => grp.fileHandlers.map(f => f.fileHandler.getFullPath).mkString("~")).mkString(",")))
        enQGroups(grps)
      }
    }
    
    (!finalValidFileGroups.isEmpty)
  }

  /**
    * Look at the files on the DEFERRED QUEUE... if we see that it stops growing, then move the file onto the READY
    * to process QUEUE.
    */
  private def monitorBufferingFiles(currentThreadId: Int, dir: String, locationInfo: LocationInfo, isFirstScan: Boolean, groupsInfo: GroupsInfoList): Unit = {
    // This guys will keep track of when to exgernalize a WARNING Message.  Since this loop really runs every second,
    // we want to throttle the warning messages.
    if (logger.isDebugEnabled) logger.debug("SMART FILE CONSUMER (MonitorController):  monitorBufferingFiles. dir = " + dir)

    var specialWarnCounter: Int = 1

    if (!keepMontoringBufferingFiles || isShutdown)
      return

    // Scan all the files that we are buffering, if there is not difference in their file size.. move them onto
    // the FileQ, they are ready to process.

    val removedEntries = ArrayBuffer[String]()

    val currentAllNonFilteredChilds =
      try {
        if (!isShutdown)
          monitoringThreadsFileHandlers(currentThreadId).listFiles(dir, adapterConfig.monitoringConfig.dirMonitoringDepth)
        else
          Array[MonitoredFile]()
      }
      catch {
        case ex: Throwable =>
          logger.error("", ex)
          Array[MonitoredFile]()
      }

    if (currentAllNonFilteredChilds.size == 0) return

    val excludeFlsSet = scala.collection.mutable.Set[String]()

    val allEnqueuedGrps = getAllEnqueuedGroups
    allEnqueuedGrps.foreach(g => {
      excludeFlsSet ++= g.fileHandlers.map(f => f.fileHandler.getFullPath)
    })
    /*
    //BUGBUG:: Get all processing files and add them in exclude list
        val allProcessingGrps = getAllEnqueuedGroups
        allProcessingGrps.foreach(g => {
          excludeFlsSet ++= g.fileHandlers.map(f => f.fileHandler.getFullPath)
        })
  */

    if (isFirstScan && initialFiles != null) {
      excludeFlsSet ++= initialFiles
    }

    val currentAllChilds = currentAllNonFilteredChilds.filter(currentMonitoredFile => {
      (!isShutdown && keepMontoringBufferingFiles && currentMonitoredFile != null && currentMonitoredFile.isFile && (!excludeFlsSet.contains(currentMonitoredFile.path)))
    })

    val fileGroups =
      if (groupsInfo != null) {
        if (logger.isDebugEnabled) logger.debug("Found groupInfo")
        currentAllChilds.groupBy(fl => {
          val flName = if (groupsInfo.onlyBaseFile) getBaseFileName(fl.path, groupsInfo.pathSeparator) else fl.path
          val parent = if (fl.parent != null) fl.parent else ""
          (parent, extractFileFromPattern(groupsInfo, flName))
        }).values.toArray
      }
      else {
        if (logger.isDebugEnabled) logger.debug("Not found groupInfo")
        currentAllChilds.map(fl => Array(fl))
      }

    if (logger.isDebugEnabled) logger.debug("All Files:\n\tcurrentAllNonFilteredChilds:%s\n\tfileGroups:%s".format(
      currentAllNonFilteredChilds.map(f => f.path).mkString(","), fileGroups.map(grp => grp.map(f => f.path).mkString("~")).mkString(",")))

    val validFiles = scala.collection.mutable.Map[String, (MonitoredFile, LocationInfo)]()
    val enqueuedBufferedFiles = getAllEnqueuedBufferedFiles
    val allProcessingFiles = parentSmartFileConsumer.getAllFilesInProcessingQueue

    currentAllChilds.foreach(currentMonitoredFile => {
      if (!isShutdown && keepMontoringBufferingFiles && currentMonitoredFile != null && currentMonitoredFile.isFile) {
        val filePath = currentMonitoredFile.path
        try {
          val currentFileParentDir = currentMonitoredFile.parent
          val currentFileLocationInfo = parentSmartFileConsumer.getDirLocationInfo(currentFileParentDir)
/*
          if (isEnqueued(filePath)) {
            if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (MonitorController):  File already enqueued " + filePath)
          }
          else 
*/
          if (allProcessingFiles.contains(filePath)) {
            if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (MonitorController):  File already in processing queue " + filePath)
          }
          else {
            // val fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, filePath, false)
            // val locationInfo = parentSmartFileConsumer.getDirLocationInfo(MonitorUtils.simpleDirPath(fileHandler.getParentDir))
            if (currentFileLocationInfo != null) {
              if (!enqueuedBufferedFiles.contains(filePath)) {
                val isValid = MonitorUtils.isValidFile(adapterConfig.Name,
                  monitoringThreadsFileHandlers(currentThreadId), filePath, currentFileLocationInfo, ignoredFiles,
                  false, adapterConfig.monitoringConfig.checkFileTypes)
                fixIgnoredFiles()

                if (isValid)
                  enQBufferedFile(currentMonitoredFile, isFirstScan)
                else {
                  if (logger.isDebugEnabled) logger.debug("Adapter {} - file {} is invalid to be queued", adapterConfig.Name, filePath)
                }
              }
              else {
                validFiles(filePath) = ((currentMonitoredFile, currentFileLocationInfo))
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

    if (isFirstScan && initialFiles != null) {
      validFiles --= initialFiles
    }

    val addedNewGroups = ProcessValidGroups(enqueuedBufferedFiles, validFiles.toMap, removedEntries, isFirstScan, fileGroups)

    if (!isShutdown && addedNewGroups && newFileDetectedCallback != null) {
      newFileDetectedCallback()
    }

    try {
      removeFilesFromBufferingQ(removedEntries.toArray)
    } catch {
      case e: Throwable => {
        logger.error("Smart File Adapter (MonitorController) - Failed to remove entries from bufferingQ_map", e)
      }
    }

  }

  def updateMonitoredFile(file: MonitoredFile, newSize: Long, newModTime: Long): MonitoredFile = {
    MonitoredFile(file.path, file.parent, newModTime, newSize, file.isDirectory, file.isFile)
  }

  def fixIgnoredFiles(): Unit = {
    if (ignoredFiles == null) return
    if (ignoredFiles.size > ignoredFilesMaxSize) {
      ignoredFiles.drop(ignoredFiles.size - ignoredFilesMaxSize)
    }
  }

/*
  private def enQFile(file: String, offset: Int, createDate: Long): Unit = {
    if (isShutdown) return
    try {
      fileQLock.synchronized {
        if (logger.isInfoEnabled) logger.info("SMART FILE CONSUMER (MonitorController):  enq file " + file + " with priority " + createDate + " --- curretnly " + fileQ.size + " files on a QUEUE")
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
    catch {
      case ex: Throwable =>
        logger.error("Adapter {} - Error: ", ex)
        throw ex //todo : remove throwing exception, but how to keep error from showing repeatedly
    }
  }
*/

  
  private def enQGroup(grp: EnqueuedGroupHandler): Unit = {
    if (grp == null) return
    groupQLock.synchronized {
      groupQ += grp
      parentSmartFileConsumer.addFilesToProcessingQueue(grp.fileHandlers.map(f => f.fileHandler.getFullPath()))
    }
  }

  private def enQGroups(grps: Array[EnqueuedGroupHandler]): Unit = {
    if (grps == null || grps.size == 0) return
    groupQLock.synchronized {
      groupQ ++= grps
      grps.foreach(grp => {
        parentSmartFileConsumer.addFilesToProcessingQueue(grp.fileHandlers.map(f => f.fileHandler.getFullPath()))
      })
    }
  }

  private def deQGroup: EnqueuedGroupHandler = {
    if (groupQ.isEmpty) {
      if (logger.isDebugEnabled) logger.debug("deQGroup is not returning anything")
      return null
    }
    groupQLock.synchronized {
      if (groupQ.isEmpty) {
        if (logger.isDebugEnabled) logger.debug("deQGroup is not returning anything")
        return null
      }
      val enqueuedgroupHandler = groupQ.dequeue()
      if (enqueuedgroupHandler != null && logger.isDebugEnabled) {
        logger.debug("SMART FILE CONSUMER (MonitorController):  deq group:" + enqueuedgroupHandler.fileHandlers.map(x => {
          x.fileHandler.getFullPath
        }).mkString(","))
      } else {
        if (logger.isDebugEnabled) logger.debug("deQGroup is not returning anything")
      }
      return enqueuedgroupHandler
    }
  }

  private def getAllEnqueuedGroups: Array[EnqueuedGroupHandler] = {
    if (groupQ.isEmpty) {
      return Array[EnqueuedGroupHandler]()
    }
    groupQLock.synchronized {
      groupQ.toArray
    }
  }

  private def waitingGroupsToProcessCount: Int = {
    groupQLock.synchronized {
      groupQ.length
    }
  }

  def getNextGroupToProcess: EnqueuedGroupHandler = {
    deQGroup
  }
}

