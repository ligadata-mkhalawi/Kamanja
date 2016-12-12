package com.ligadata.InputAdapters

import java.io.OutputStream
import java.util.concurrent.locks.ReentrantReadWriteLock

import com.ligadata.AdaptersConfiguration._
import com.ligadata.Exceptions.FatalAdapterException
import com.ligadata.OutputAdapters.{OutputStreamWriter, PartitionStream}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.logging.log4j.LogManager

import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.{ListBuffer, ArrayBuffer}

import java.io.ByteArrayOutputStream


case class StreamFile(destDir: String, var destFileName: String, var outStream: OutputStream,
                      var currentFileSize: Long, var streamBuffer: ArrayBuffer[Byte], var flushBufferSize: Long,
                      var currentActualOffset : Long){
  def destFileFullPath = destDir + "/" + destFileName
}

class Archiver(adapterConfig: SmartFileAdapterConfiguration, smartFileConsumer: SmartFileConsumer) {

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  val archiveRolloverInterval : Long =
    if(adapterConfig.archiveConfig == null || adapterConfig.archiveConfig.outputConfig == null) 60
    else adapterConfig.archiveConfig.outputConfig.rolloverInterval //in minutes
  val archiveRolloverCheckSleepMS = 1 * 60 * 1000

  private def trimFileFromLocalFileSystem(fileName: String): String = {
    if (fileName.startsWith("file://"))
      return fileName.substring("file://".length() - 1)
    fileName
  }

  val destFileFormat = "file_%s"
  def getNewArchiveFileName = {
    val callstack = Thread.currentThread().getStackTrace().drop(2).take(5).
      map(s => s.getClassName + "." + s.getMethodName + "(" + s.getLineNumber + ")").mkString("\n")
    logger.debug("getting new archive file. " + callstack)
    destFileFormat.format(new java.text.SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new java.util.Date()) )
  }

  //daily
  val archiveIndexFileFormat = "ArchiveIndex_%s.info"
  def archiveIndexFileName = archiveIndexFileFormat.format(new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date()) )

  def getCurrentTimestamp : String = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new java.util.Date())

  def getDestArchiveDir(locationInfo: LocationInfo, srcFileDir: String, srcFileBaseName: String, componentsMap: scala.collection.immutable.Map[String, String]) : String = {
    val partitionVariable = "\\$\\{([^\\}]+)\\}".r
    val partitionFormats = partitionVariable.findAllMatchIn(adapterConfig.archiveConfig.outputConfig.uri).map(x => x.group(1)).toList
    val partitionFormatString = partitionVariable.replaceAllIn(adapterConfig.archiveConfig.outputConfig.uri, "%s")

    val values = partitionFormats.map(fmt => {
      componentsMap.getOrElse(fmt, "default").trim
    })

    val srcFileToArchive = srcFileDir + "/" + srcFileBaseName
    //val dstFileToArchive =  partitionFormatString.format(values: _*) + "/" + srcFileBaseName

    val dstDirToArchiveBase =
      if(locationInfo != null && locationInfo.archiveRelativePath != null && locationInfo.archiveRelativePath.length > 0){
        adapterConfig.archiveConfig.outputConfig.uri + "/" + locationInfo.archiveRelativePath
      }
      else if(adapterConfig.archiveConfig.createDirPerLocation) {
        val srcDirStruct = locationInfo.srcDir.split("/")
        val srcDirOnly = srcDirStruct(srcDirStruct.length - 1) //last part of src dir
        partitionFormatString.format(values: _*) + "/" +  srcDirOnly
      }
      else partitionFormatString.format(values: _*)

    val srcFileStruct = srcFileToArchive.split("/")
    val dstDirToArchive =
      if (locationInfo != null && adapterConfig.monitoringConfig.createInputStructureInTargetDirs) {
        val dir = srcFileStruct.take(srcFileStruct.length - 1).mkString("/").replace(locationInfo.targetDir, dstDirToArchiveBase)
        trimFileFromLocalFileSystem(dir)
      }
      else trimFileFromLocalFileSystem(dstDirToArchiveBase)

    dstDirToArchive
  }

  def validateArchiveDestCompression() : Boolean = {
    if (CompressorStreamFactory.BZIP2.equalsIgnoreCase(adapterConfig.archiveConfig.outputConfig.compressionString) ||
      CompressorStreamFactory.GZIP.equalsIgnoreCase(adapterConfig.archiveConfig.outputConfig.compressionString) ||
      CompressorStreamFactory.XZ.equalsIgnoreCase(adapterConfig.archiveConfig.outputConfig.compressionString)
    ) {
      logger.info("Smart File Consumer " + adapterConfig.Name + " Archiving is using compression: " + adapterConfig.archiveConfig.outputConfig.compressionString)
      true
    }
    else
      throw FatalAdapterException("Unsupported compression type " + adapterConfig.archiveConfig.outputConfig.compressionString + " for Smart File Producer: " + adapterConfig.archiveConfig.outputConfig.Name, new Exception("Invalid Parameters"))

  }

  /*************************************************************************************************************/
  //key is dest archive dir
  private var archiveDirsStatusMap = new scala.collection.mutable.LinkedHashMap[String, ArchiveDirStatus]()

  //key is dest archive dir
  //private var archiveInfo = new scala.collection.mutable.LinkedHashMap[String, ArrayBuffer[ArchiveFileInfo]]()
  private var archiveInfoList = ListBuffer[ArchiveFileInfo]()
  private val archiveInfoList_reent_lock = new ReentrantReadWriteLock(true)
  private val archiveDirsStatusMap_reent_lock = new ReentrantReadWriteLock(true)
  private val archiveDirsStatusMap_lock = new Object

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

  def hasNextArchiveFileInfo: Boolean = {
    archiveInfoList.nonEmpty
  }

  def releaseArchiveDirLock(dir : String) : Unit = {
    logger.debug("releasing map lock for dir {}", dir)
    archiveDirsStatusMap_lock.synchronized {
      try {
        if(archiveDirsStatusMap != null && archiveDirsStatusMap.contains(dir))
          archiveDirsStatusMap(dir).isActive = false
      }
      catch {
        case ex: Throwable => logger.error("", ex)
      }

    }
  }

  def getNextArchiveFileInfo: (ArchiveFileInfo, ArchiveDirStatus) = {
    var archInfo: ArchiveFileInfo = null
    var archiveDirStatus : ArchiveDirStatus = null
    WriteLock(archiveInfoList_reent_lock)
    try {
      if (archiveInfoList.nonEmpty) {

        var idx = 0
        var found = false
        //loop the files queue
        while(idx < archiveInfoList.length && !found){
          archInfo = archiveInfoList(idx)
          logger.debug("getNextArchiveFileInfo - checking status corresponding to file {} ", archInfo.srcFileBaseName)

          archiveDirStatus = getArchiveDirStatus(archInfo.destArchiveDir)
          if(archiveDirStatus == null) {
            logger.debug("getNextArchiveFileInfo - dir {} is processed by another thread", archInfo.destArchiveDir)
            //another file from same dir is already being processed by another thread
            archInfo = null
            idx += 1
          }
          else found = true

        }
        if(archInfo == null)
          logger.debug("getNextArchiveFileInfo - no files")
        else logger.debug("getNextArchiveFileInfo - got file to archive: {}", archInfo.srcFileDir + "/" + archInfo.srcFileBaseName)

        logger.debug("getNextArchiveFileInfo - idx="+idx)
        if(archInfo != null && idx < archiveInfoList.length)
          archiveInfoList.remove(idx)
      }

    }
    catch{
      case ex : Throwable => logger.error("", ex)
    }
    finally {
      WriteUnlock(archiveInfoList_reent_lock)
    }

    logger.debug("archiveInfoList len="+archiveInfoList.length)
    (archInfo, archiveDirStatus)
  }

  def addArchiveFileInfo(archInfo: ArchiveFileInfo): Unit = {

    WriteLock(archiveInfoList_reent_lock)
    try {
      if(archInfo.destArchiveDir == null || archInfo.destArchiveDir.length == 0) {
        //get corresponding archive dir here
        val destArchiveDir = getDestArchiveDir(archInfo.locationInfo, archInfo.srcFileDir, archInfo.srcFileBaseName, archInfo.componentsMap)
        archInfo.destArchiveDir = destArchiveDir
      }

      archiveInfoList :+= archInfo
    } finally {
      WriteUnlock(archiveInfoList_reent_lock)
    }
  }

  //add these into head of the queue
  def addFailoverArchiveFilesInfo(archInfos: Array[ArchiveFileInfo]): Unit = {

    WriteLock(archiveInfoList_reent_lock)
    try {
      archInfos.foreach(archInfo => {
        if(archInfo.destArchiveDir == null || archInfo.destArchiveDir.length == 0) {
          //get corresponding archive dir here
          val destArchiveDir = getDestArchiveDir(archInfo.locationInfo, archInfo.srcFileDir, archInfo.srcFileBaseName, archInfo.componentsMap)
          archInfo.destArchiveDir = destArchiveDir
        }
      })


      archiveInfoList.insertAll(0, archInfos)
    } finally {
      WriteUnlock(archiveInfoList_reent_lock)
    }
  }

  val archiveParallelism = if (adapterConfig.archiveConfig == null || adapterConfig.archiveConfig.archiveParallelism <= 0) 1 else adapterConfig.archiveConfig.archiveParallelism
  val archiveSleepTimeInMs = if (adapterConfig.archiveConfig == null || adapterConfig.archiveConfig.archiveSleepTimeInMs < 0) 500 else adapterConfig.archiveConfig.archiveSleepTimeInMs

  private var archiveExecutor : ExecutorService = null


  def isInterrupted : Boolean = {
    isShutdown || Thread.currentThread().isInterrupted || archiveExecutor == null || archiveExecutor.isShutdown || archiveExecutor.isTerminated
  }

  var initialTargetDirsCheckingDone = false

  def startArchiving(): Unit ={
    if(adapterConfig.archiveConfig == null)
      return

    archiveExecutor = Executors.newFixedThreadPool(archiveParallelism + 2)

    val initialTargetDirsChecker = new Runnable() {
      override def run(): Unit = {
        //check all available target dirs
        //find already existing files and push to head of archive queue
        checkTargetDirs()
        initialTargetDirsCheckingDone = true
        logger.warn("adapter {} finished checkTargetDirs()", adapterConfig.Name)
      }
    }
    archiveExecutor.execute(initialTargetDirsChecker)


    for (i <- 0 until archiveParallelism) {
      val archiveThread = new Runnable() {
        override def run(): Unit = {
          var interruptedVal = false
          while (!interruptedVal && !isInterrupted) {
            if(initialTargetDirsCheckingDone) {
              try {
                if (!isInterrupted && hasNextArchiveFileInfo) {
                  val archInfo = getNextArchiveFileInfo
                  if (!isInterrupted && archInfo != null && archInfo._1 != null && archInfo._2 != null) {
                    logger.debug("got file to archive from queue: {}", archInfo._1.srcFileBaseName)
                    try {
                      archiveFile(archInfo._1, archInfo._2)
                    }
                    catch {
                      case e: Throwable => logger.error("Error while archiving file " +
                        archInfo._1.srcFileDir + "/" + archInfo._1.srcFileBaseName, e)
                    }
                  }
                  else {
                    logger.debug("no files in archive queue")
                    interruptedVal = smartFileConsumer.sleepMs(archiveSleepTimeInMs)
                  }
                }
                else {
                  logger.debug("no files in archive queue")
                  if (archiveSleepTimeInMs > 0 && !isInterrupted)
                    interruptedVal = smartFileConsumer.sleepMs(archiveSleepTimeInMs)
                }
              } catch {
                case e: InterruptedException => {
                  interruptedVal = true
                }
                case e: Throwable => {
                }
              }
            }
            else {
              logger.debug("initial Target Dirs Checking not Done yet, waiting")
              interruptedVal = smartFileConsumer.sleepMs(archiveSleepTimeInMs)
            }
          }
        }
      }
      archiveExecutor.execute(archiveThread)
    }

    val rolloverCheckThread = new Runnable() {
      override def run(): Unit = {
        // Trying to sleep maximum 1sec intervals
        var interruptedVal = false
        val nSleepSecs = archiveRolloverCheckSleepMS / 1000
        val nSleepMilliSecs = archiveRolloverCheckSleepMS % 1000
        while (!interruptedVal && !isInterrupted && !isShutdown) {
          try {
            checkRolloverDueFiles()
            var cntr = 0
            while (cntr < nSleepSecs && !interruptedVal && !isShutdown) {
              cntr += 1
              interruptedVal = smartFileConsumer.sleepMs(1000)
            }
            if (nSleepMilliSecs > 0 && !interruptedVal && !isShutdown) {
              interruptedVal = smartFileConsumer.sleepMs(nSleepMilliSecs)
            }
          } catch {
            case e: InterruptedException => {
              interruptedVal = true
            }
            case e: Throwable => {
            }
          }

        }
      }
    }
    logger.debug("Archiver: running thread to check files rollover intervals")
    archiveExecutor.execute(rolloverCheckThread)

  }

  private def checkRolloverDueFiles(): Unit ={
    logger.debug("Archiver: checking for files which need to rollover")
    try{
      val dirsToFlush = ArrayBuffer[String]()
      val currentTs = System.currentTimeMillis
      archiveDirsStatusMap_lock.synchronized {
        archiveDirsStatusMap.foreach(tuple =>  {
          logger.debug("checking rollover interval for dir "+tuple._2.dir)
          logger.debug("  active="+tuple._2.isActive)
          logger.debug("  nextRolloverTime="+tuple._2.nextRolloverTime)
          logger.debug("  currentTs="+currentTs)
          if(!tuple._2.isActive && currentTs >= tuple._2.nextRolloverTime){
            dirsToFlush.append(tuple._1)
            tuple._2.isActive = true // lock the dir to be flushed next instead of locking map until all dirs are done
          }
        })
      }

      dirsToFlush.foreach(dir => {
        logger.info("Archiver: Flushing data for dir {} since rollover is due", dir)
        archiveDirsStatusMap_lock.synchronized {
          val dirStatus = archiveDirsStatusMap.getOrElse(dir, null)
          if (dirStatus != null) {
            //dir is already locked, no need to lock the map

            flushArchiveDirStatus(dirStatus)

            dirStatus.nextRolloverTime = System.currentTimeMillis + archiveRolloverInterval * 60 * 1000
            dirStatus.isActive = false
          }
        }

      })
    }
    catch{
      case ex : Throwable => logger.error("", ex)
    }

  }

  private def openStream(filePath : String): OutputStream ={
    //open output stream
    var os : OutputStream = null
    var originalStream : OutputStream = null
    //os = openFile(fc, fileName)
    val osWriter = new com.ligadata.OutputAdapters.OutputStreamWriter()
    originalStream = osWriter.openFile(adapterConfig.archiveConfig.outputConfig, filePath, true)
    originalStream
    /*val compress = adapterConfig.archiveConfig.outputConfig.compressionString != null
    if (compress)
      os = new CompressorStreamFactory().createCompressorOutputStream(adapterConfig.archiveConfig.outputConfig.compressionString, originalStream)
    else
      os = originalStream

    os*/
  }


  private def getArchiveDirStatus(dir : String) : ArchiveDirStatus = {
    logger.debug("getArchiveDirStatus - dir {}", dir)

    archiveDirsStatusMap_lock.synchronized {
      var currentFileDirStatus = archiveDirsStatusMap.getOrElse(dir, null)
      if (currentFileDirStatus == null) {
        logger.debug("no entry in dir status map for dir {}. adding new entry", dir)
        val nextRolloverTime = System.currentTimeMillis + archiveRolloverInterval * 60 * 1000
        currentFileDirStatus = ArchiveDirStatus(dir, new ByteArrayOutputStream(), null,
          getNewArchiveFileName, 0, -1, nextRolloverTime, ArrayBuffer[String](), "", ArrayBuffer[ArchiveFileIndexEntry](), true)
        archiveDirsStatusMap.put(dir, currentFileDirStatus)
        currentFileDirStatus
      }
      else{
        if(currentFileDirStatus.isActive){
          logger.debug("dir {} has an entry in dir status map but already active", dir)
          null
        }
        else {
          logger.debug("dir {} has an entry in dir status map but not active, changing to active by current thread", dir)
          currentFileDirStatus.isActive = true
          archiveDirsStatusMap.put(dir, currentFileDirStatus)
          currentFileDirStatus
        }
      }
    }
  }

  private def getProperStream(originalMemroyStream : ByteArrayOutputStream): OutputStream ={
    val compress = adapterConfig.archiveConfig.outputConfig.compressionString != null
    if (compress)
      new CompressorStreamFactory().createCompressorOutputStream(adapterConfig.archiveConfig.outputConfig.compressionString, originalMemroyStream)
    else
      originalMemroyStream
  }

  /**
    * flushes in memory stream into disk and updates dir status
    * @param archiveDirStatus
    */
  private def flushArchiveDirStatus(archiveDirStatus : ArchiveDirStatus, isForced : Boolean = false) : Unit = {
    //files were already read into memory, flush into disk after shutdown signal
    if(isShutdown && !isForced)
      return

    var targetDirFileHandler : SmartFileHandler = null
    try {
      if(archiveDirStatus == null || archiveDirStatus.destFileCurrentOffset <= 0 ||
        archiveDirStatus.originalMemoryStream == null || archiveDirStatus.originalMemoryStream.size() <= 0)
        return //nothing to flush
      else
        logger.debug("archive dir {} has nothing to flush", archiveDirStatus.dir)

      val lastSrcFilePath = archiveDirStatus.lastSrcFilePath
      if (lastSrcFilePath != null && lastSrcFilePath.length > 0) {

        val osWriter = new com.ligadata.OutputAdapters.OutputStreamWriter()
        val archiveIndex = archiveDirStatus.archiveIndex

        //update and dump index first
        val previousEntry = if (archiveIndex.nonEmpty) archiveIndex.last else null
        if (previousEntry != null && previousEntry.srcFile.equals(lastSrcFilePath)) {
          previousEntry.destFileEndOffset = archiveDirStatus.destFileCurrentOffset
          previousEntry.ArchiveTimestamp = getCurrentTimestamp

          previousEntry.srcFileEndOffset = previousEntry.destFileEndOffset - previousEntry.destFileStartOffset + previousEntry.srcFileStartOffset

          dumpArchiveIndex(osWriter, archiveDirStatus.dir, archiveIndex)
          /*logger.warn("archiveIndex len = {}", archiveIndex.length.toString)
          logger.warn("archiveDirStatus.archiveIndex len = {}", archiveDirStatus.archiveIndex.length.toString)
          logger.warn("archiveDirsStatusMap = {}", archiveDirsStatusMap)*/

          val expectedFileSize = archiveDirStatus.originalMemoryStream.size()
          //dump archive
          logger.info("Archiver - dumping in memory data from dir {} to file {}", archiveDirStatus.dir, archiveDirStatus.destFileFullPath)
          val diskOutputStream = openStream(archiveDirStatus.destFileFullPath)
          archiveDirStatus.originalMemoryStream.writeTo(diskOutputStream)
          diskOutputStream.close()
          archiveDirStatus.originalMemoryStream.reset()

          val lastWrittenDestFile = archiveDirStatus.destFileFullPath

          archiveDirStatus.destFileName = getNewArchiveFileName
          logger.debug("new file name is " + archiveDirStatus.destFileName)
          logger.debug("resetting currentFileSize to 0")
          archiveDirStatus.destFileCurrentSize = 0
          val srcStartOffset = if(previousEntry != null) previousEntry.srcFileEndOffset + 1 else 0
          val newEntry = ArchiveFileIndexEntry(lastSrcFilePath, archiveDirStatus.destFileFullPath, srcStartOffset, -1, 0, -1, "")
          archiveIndex.append(newEntry)

          archiveDirStatus.destFileCurrentOffset = -1
          archiveDirStatus.nextRolloverTime = System.currentTimeMillis + archiveRolloverInterval * 60 * 1000

          //archiveDirStatus.archiveIndex.clear() ????

          //todo : need to update status map?

          targetDirFileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, "/")

          if(osWriter.isFileExists(adapterConfig.archiveConfig.outputConfig, lastWrittenDestFile)){
            val actualSize = osWriter.getFileSize(adapterConfig.archiveConfig.outputConfig, lastWrittenDestFile)
            logger.debug("disk file {} size on disk is {} . expected file size {}",
              lastWrittenDestFile, actualSize.toString, expectedFileSize.toString)

            if(actualSize == expectedFileSize){
              logger.debug("disk and in memory sizes match")

              //now delete src files
              archiveDirStatus.srcFiles.foreach(file => {
                try {
                  logger.info("file {} is finished, deleting", file)
                  targetDirFileHandler.deleteFile(file) // Deleting file after archive
                }
                catch {
                  case e: Throwable => {
                    logger.error("Failed to delete file " + archiveDirStatus.destFileFullPath, e)
                  }
                }
              })
              archiveDirStatus.srcFiles.clear()
            }
            else
              logger.warn("Archiver: archive file {} was supposed to have size {}. but actual size is {}",
                lastWrittenDestFile, actualSize.toString, expectedFileSize.toString)

          }
          else
            logger.warn("Archiver: archive file {} was successfully written. but does not exist anymore", lastWrittenDestFile)

        }
        else {
          logger.warn("file {} should already have an entry in archive index", lastSrcFilePath)
          //TODO : how can we get here
        }

      }
    }
    catch{
      case ex : Throwable =>
        val msg = "Error while saving archive file into dir (%s) corresponding to src files (%s)".format(
          archiveDirStatus.destFileFullPath, archiveDirStatus.srcFiles.mkString(","))
        logger.error(msg, ex)
    }
    finally{
      try{
        if(targetDirFileHandler != null)
          targetDirFileHandler.disconnect()
      }
      catch{
        case ex : Exception =>
      }
    }
  }

  val bufferSz = 8 * 1024 * 1024
  private def archiveFile(archInfo: ArchiveFileInfo, archiveDirStatus : ArchiveDirStatus) : Boolean = {

    if(isShutdown)
      return  false

    if(archInfo == null)
      return false

    val srcFileToArchive = archInfo.srcFileDir + "/" + archInfo.srcFileBaseName
    logger.warn("start archiving file {}", srcFileToArchive)

    val byteBuffer = new Array[Byte](bufferSz)


    archiveDirStatus.lastSrcFilePath = srcFileToArchive

    logger.debug("srcFileToArchive={}", srcFileToArchive)
    logger.debug("appendFileName={}", archiveDirStatus.destFileFullPath)
    logger.debug("appendFileCurrentSize={}", archiveDirStatus.destFileCurrentSize.toString)
    logger.debug("appendFileCurrentActualOffset={}", archiveDirStatus.destFileCurrentOffset.toString)

    if (archiveDirStatus.originalMemoryStream == null)
      archiveDirStatus.originalMemoryStream = new ByteArrayOutputStream()

    val archiveIndex = archiveDirStatus.archiveIndex

    val osWriter = new com.ligadata.OutputAdapters.OutputStreamWriter()

    var srcFileHandler: SmartFileHandler = null
    try {

      srcFileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, srcFileToArchive, isBinary = false)
      if (srcFileHandler.exists()) {
        //might need to build sub-dirs corresponding to input dir structure
        val destArchiveDirExists =
          if (!osWriter.isFileExists(adapterConfig.archiveConfig.outputConfig, archiveDirStatus.dir))
            osWriter.mkdirs(adapterConfig.archiveConfig.outputConfig, archiveDirStatus.dir)
          else true
        if (!destArchiveDirExists) {
          logger.error("Archiving dest dir {} does not exist and could not be created", archiveDirStatus.dir)
          return false
        }

        logger.debug("opening src file to read {}", srcFileHandler.getFullPath)
        srcFileHandler.openForRead()


        /*********skip until offset*********/
        if(archInfo.srcFileStartOffset > 0) {
          var totalReadLen = 0
          var curReadLen = 0
          var lengthToRead: Int = 0
          do {
            lengthToRead = Math.min(bufferSz, archInfo.srcFileStartOffset - totalReadLen).toInt
            curReadLen = srcFileHandler.read(byteBuffer, 0, lengthToRead)
            totalReadLen += curReadLen
            logger.debug("SMART FILE CONSUMER - skipping {} bytes from file {} but got only {} bytes",
              lengthToRead.toString, srcFileHandler.getFullPath, curReadLen.toString)
          } while (totalReadLen < archInfo.srcFileStartOffset && curReadLen > 0 && !isInterrupted)
          logger.debug("SMART FILE CONSUMER - totalReadLen from file {} is {}", srcFileHandler.getFullPath, totalReadLen.toString)
          //globalOffset = totalReadLen
          curReadLen = 0
        }
        /******************/

        if(!isInterrupted) {
          val entry = ArchiveFileIndexEntry(srcFileToArchive, archiveDirStatus.destFileFullPath,
            archInfo.srcFileStartOffset, -1,
            archiveDirStatus.destFileCurrentOffset + 1, -1, "")
          archiveIndex.append(entry)

          var lastReadLen = -1
          var actualBufferLen = 0


          val message_separator: Char =
            if (archInfo.locationInfo == null) adapterConfig.monitoringConfig.messageSeparator
            else archInfo.locationInfo.messageSeparator

          do {
            val currentInMemorySize = archiveDirStatus.originalMemoryStream.size()
            logger.debug("reading destFileName=" + archiveDirStatus.destFileName)
            logger.debug("streamFile.currentFileSize=" + archiveDirStatus.destFileCurrentSize)
            logger.debug("currentInMemorySize=" + currentInMemorySize)
            logger.debug("actualBufferLen=" + actualBufferLen)

            val lengthToRead = Math.min(bufferSz - actualBufferLen,
              adapterConfig.archiveConfig.consolidateThresholdBytes - currentInMemorySize - actualBufferLen).toInt
            logger.debug("lengthToRead={}", lengthToRead.toString)
            if (lengthToRead > 0) {
              if (lengthToRead <= actualBufferLen) {
                logger.debug("memory stream is almost full. cannot write leftover from last iteration of src file {}. saving to file {} and opening a new one",
                  srcFileHandler.getFullPath, archiveDirStatus.destFileName)

                flushArchiveDirStatus(archiveDirStatus)
              }

              else {
                logger.debug("reading {} to buffer with offset {}", lengthToRead.toString, actualBufferLen.toString)
                try {
                  lastReadLen = srcFileHandler.read(byteBuffer, actualBufferLen, lengthToRead)
                }
                catch {
                  case e: Throwable =>
                    lastReadLen = -1
                }
                logger.debug("curReadLen={}", lastReadLen.toString)

                var lastSeparatorIdx = -1
                var actualLenToWrite: Int = actualBufferLen //initially
                if (lastReadLen > 0) {
                  actualBufferLen += lastReadLen

                  var idx = actualBufferLen - 1
                  while (idx >= 0 && lastSeparatorIdx < 0) {
                    if (byteBuffer(idx).asInstanceOf[Char] == message_separator) {
                      lastSeparatorIdx = idx
                    }
                    idx -= 1
                  }
                  if (lastSeparatorIdx >= 0)
                    actualLenToWrite = lastSeparatorIdx + 1
                  else actualLenToWrite = actualBufferLen
                }

                if (actualLenToWrite > 0) {
                  logger.debug("lastSeparatorIdx={}", lastSeparatorIdx.toString)

                  //how much can we write to dest file max
                  val remainingArchiveSpace = adapterConfig.archiveConfig.consolidateThresholdBytes - currentInMemorySize
                  logger.debug("streamFile.currentFileSize=" + archiveDirStatus.destFileCurrentSize)
                  logger.debug("remainingArchiveSpace=" + remainingArchiveSpace)
                  logger.debug("currentInMemorySize=" + currentInMemorySize)
                  logger.debug("actualLenToWrite=" + actualLenToWrite)

                  if (remainingArchiveSpace < actualLenToWrite) {
                    logger.debug("file {} is full", archiveDirStatus.destFileFullPath)
                    //dest file is full
                    flushArchiveDirStatus(archiveDirStatus)

                  }

                  logger.debug("writing to memory stream actualLenToWrite ={}", actualLenToWrite.toString)
                  logger.debug("old in memory size = " + archiveDirStatus.originalMemoryStream.size)
                  archiveDirStatus.actualMemoryStream = getProperStream(archiveDirStatus.originalMemoryStream)
                  archiveDirStatus.actualMemoryStream.write(byteBuffer, 0, actualLenToWrite)
                  archiveDirStatus.actualMemoryStream.close()
                  archiveDirStatus.destFileCurrentOffset += actualLenToWrite

                  logger.debug("new in memory size = " + archiveDirStatus.originalMemoryStream.size)

                  //logger.debug("current buffer:" + new String(byteBuffer.slice(0, actualBufferLen)))
                  //logger.debug("written:" + new String(byteBuffer.slice(0, actualLenToWrite)))
                  if (archiveDirStatus.originalMemoryStream.size >= adapterConfig.archiveConfig.consolidateThresholdBytes) {
                    logger.debug("writing to disk since memory stream is full")

                    flushArchiveDirStatus(archiveDirStatus)
                  }

                  //fix buffer and index for next reading
                  if (actualBufferLen >= actualLenToWrite) {
                    // copy reaming bytes to head of buffer
                    for (i <- 0 to actualBufferLen - actualLenToWrite) {
                      byteBuffer(i) = byteBuffer(actualLenToWrite + i)
                    }
                    actualBufferLen = actualBufferLen - actualLenToWrite
                  }
                  else actualBufferLen = 0

                  logger.debug("next offset is {}", actualBufferLen.toString)
                }
                else
                  logger.debug("actualLenToWrite is 0, nothing to write")

              }
            }
            else lastReadLen = 0

            logger.debug("lastReadLen=" + lastReadLen)


          } while (lastReadLen > 0 && !isInterrupted)

          if (!isInterrupted) {
            archiveDirStatus.srcFiles.append(srcFileToArchive) //to be deleted when archive is dumped to disk
          }
        }
      }
    }
    catch {
      case e: Throwable => {
        logger.error("Failed to archive file from " + srcFileHandler.getFullPath + " to " + archiveDirStatus.destFileFullPath, e)
        return false
      }
    } finally {
      if (srcFileHandler != null) {
        try {
          srcFileHandler.close()
        } catch {
          case e: Throwable => {
            logger.error("Failed to close InputStream for " + archiveDirStatus.destFileFullPath, e)
          }
        }

        val entry = if(archiveIndex.nonEmpty) archiveIndex.last else null
        if(entry != null && entry.srcFile.equals(srcFileHandler.getFullPath)) {
          entry.destFileEndOffset = archiveDirStatus.destFileCurrentOffset
          entry.ArchiveTimestamp = getCurrentTimestamp
          entry.srcFileEndOffset = entry.destFileEndOffset - entry.destFileStartOffset + entry.srcFileStartOffset
        }
        else{
          logger.warn("file {} does not have an entry in archive index", srcFileHandler.getFullPath)
        }

        logger.warn("finished archiving file {} into memory", srcFileHandler.getFullPath)
      }

      releaseArchiveDirLock(archiveDirStatus.dir)
    }

    archiveIndex.foreach(a => logger.debug(a.toString))

    true
  }

  def dumpArchiveIndex(osWriter : OutputStreamWriter,
                       parentFolder : String, archiveIndex: ArrayBuffer[ArchiveFileIndexEntry]) : Unit = {
    var os : OutputStream = null
    try {
      if (archiveIndex == null || archiveIndex.isEmpty)
        return
      val path = parentFolder + "/" + archiveIndexFileName
      logger.info("dumping current archive index to file " + path)
      val fc = adapterConfig.archiveConfig.outputConfig
      os = osWriter.openFile(fc, path, canAppend = true)
      archiveIndex.foreach(ai => {
        val lineAr = Array(ai.srcFile, ai.destFile, ai.srcFileStartOffset, ai.srcFileEndOffset, ai.destFileStartOffset, ai.destFileEndOffset, ai.ArchiveTimestamp)
        os.write((lineAr.mkString(",") + "\n").getBytes())
      })

      archiveIndex.clear()
    }
    catch{
      case ex : Throwable => //todo - retry?
        logger.error("Error while dumping archive index for dir " + parentFolder, ex)
    }
    finally{
      if(os != null){
        try{
          os.close()
        }
        catch{case ex : Throwable => }
      }
    }
  }

  def forceFlushAllArchiveDirs(): Unit ={
    archiveDirsStatusMap.foreach(dirTuple =>{

      val archiveDirStatus = dirTuple._2
      try {

        if(archiveDirStatus.originalMemoryStream.size() > 0) {
          flushArchiveDirStatus(archiveDirStatus, isForced = true)
        }

      }
      catch{
        case ex : Throwable => logger.error("", ex)
      }

    })
  }

  private def checkTargetDirs(): Unit ={
    var srcFileHandler : SmartFileHandler = null
    try {
      srcFileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, "/")
      adapterConfig.monitoringConfig.detailedLocations.foreach(location => {
        val targetFiles = srcFileHandler.listFiles(location.targetDir, adapterConfig.monitoringConfig.dirMonitoringDepth)
        val filesPerTargetDirMap = scala.collection.mutable.Map[String, ArrayBuffer[String]]()
        targetFiles.foreach(file => {
          if (!filesPerTargetDirMap.contains(file.parent)) filesPerTargetDirMap.put(file.parent, ArrayBuffer[String]())
          filesPerTargetDirMap(file.parent).append(file.path)
        })

        if (filesPerTargetDirMap.size == 0)
          logger.info("no files found under target dir {}. nothing to failover", location.targetDir)
        filesPerTargetDirMap.foreach(tuple => {
          checkAndFixTargetDirArchiveStatus(tuple._1, tuple._2.toArray, location)
        })

      })
    }
    catch{
      case ex : Throwable =>
        logger.error("", ex)

    }
    finally{
      if(srcFileHandler != null){
        try{
          srcFileHandler.disconnect()
        }
        catch{ case ex : Throwable => }
      }
    }
  }

  private def getDirLatestIndexFile(archiveDir : String, fileHandler: SmartFileHandler) : Option[String] = {

    try {
      val files = fileHandler.listFiles(archiveDir, 1)
      val indexFiles = files.filter(file => MonitorUtils.getFileName(file.path).toLowerCase.startsWith("archiveindex")
        && file.path.endsWith(".info"))
      if (indexFiles.isEmpty) return None
      else {
        Some(indexFiles.maxBy(file => file.lastModificationTime).path)
      }
    }
    catch{
      case ex : Throwable =>
        logger.error("", ex)
        None
    }
  }

  val archiveIndexSeparator = '\n'
  private def parseIndexFile(archiveInputConfig : SmartFileAdapterConfiguration, indexFilePath : String, fileSize : Int):
  (String, Array[ArchiveFileIndexEntry], Int) ={

    var lastIndexEntrySetStartOffset : Int = 0
    val lastIndexEntrySet = ArrayBuffer[ArchiveFileIndexEntry]()
    var lastDestArchiveFile = ""
    var archiveDirFileHandler : SmartFileHandler = null

    try {
      val buffer = new Array[Byte](fileSize)
      archiveDirFileHandler = SmartFileHandlerFactory.createSmartFileHandler(archiveInputConfig, indexFilePath)
      archiveDirFileHandler.openForRead()
      val readLen = archiveDirFileHandler.read(buffer, 0, fileSize)
      var currentEntryIdx = 0
      logger.debug("parseIndexFile - readLen={}", readLen.toString)

      for (idx <- 0 until readLen) {
        if (buffer(idx).asInstanceOf[Char] == archiveIndexSeparator || idx == readLen - 1) {
          val entryStr = new String(buffer, currentEntryIdx, idx - currentEntryIdx)
          logger.debug("parseIndexFile - record={}", entryStr)
          if (entryStr != null && entryStr.trim.length > 0) {
            val entryOpt = createIndexEntryFromString(entryStr)
            if (entryOpt.isDefined) {
              val entry = entryOpt.get
              if (entry.destFile.equals(lastDestArchiveFile))
                lastIndexEntrySet.append(entry)
              else {
                lastIndexEntrySet.clear()
                lastDestArchiveFile = entry.destFile
                lastIndexEntrySet.append(entry)
                lastIndexEntrySetStartOffset = currentEntryIdx
              }
            }
          }
          currentEntryIdx = idx + 1
        }

      }
    }
    catch{
      case ex : Throwable => logger.error("", ex)
    }

    try{
      if(archiveDirFileHandler != null)
        archiveDirFileHandler.close()
    }
    catch{ case ex: Throwable => }

    (lastDestArchiveFile, lastIndexEntrySet.toArray, lastIndexEntrySetStartOffset)
  }

  private def removeLastIndexEntrySet(archiveInputConfig : SmartFileAdapterConfiguration,
                                      indexFilePath : String, lastIndexEntrySetStartOffset : Int) : Boolean = {

    var indexFileHandler : SmartFileHandler = null
    var tmpIndexFileHandler : SmartFileHandler = null
    try {
      var readLen = 0
      indexFileHandler = SmartFileHandlerFactory.createSmartFileHandler(archiveInputConfig, indexFilePath)
      if (lastIndexEntrySetStartOffset <= 0)
        indexFileHandler.delete()
      else {
        val buffer = new Array[Byte](lastIndexEntrySetStartOffset - 1)
        try {
          indexFileHandler.openForRead()
          readLen = indexFileHandler.read(buffer, 0, lastIndexEntrySetStartOffset - 1)
        }
        catch{
          case ex : Throwable =>
            logger.error("", ex)
        }

        if (readLen > 0) {
          val osWriter = new com.ligadata.OutputAdapters.OutputStreamWriter()
          val tmpIndexFilePath = indexFilePath + "_tmp"
          logger.debug("Archiver - writing from index file {} to file {} : from offset zero to offset {}",
            indexFilePath, tmpIndexFilePath, readLen.toString)
          val os = osWriter.openFile(adapterConfig.archiveConfig.outputConfig, tmpIndexFilePath, false)
          os.write(buffer, 0, readLen)
          os.write(archiveIndexSeparator)
          os.close()

          tmpIndexFileHandler = SmartFileHandlerFactory.createSmartFileHandler(archiveInputConfig, tmpIndexFilePath)
          logger.debug("Archiver - renaming file {} to file {}", indexFilePath, indexFilePath + "_tmp_del")
          indexFileHandler.moveTo(indexFilePath + "_tmp_del")
          logger.debug("Archiver - renaming file {} to file {}", tmpIndexFileHandler.getFullPath, indexFilePath)
          tmpIndexFileHandler.moveTo(indexFilePath)
          logger.debug("Archiver - deleting file {}", indexFileHandler.getFullPath)
          indexFileHandler.delete()
        }
      }
      true
    }
    catch{
      case ex : Throwable =>
        logger.error("", ex)
        false
    }
    finally{
      try{
        if(indexFileHandler != null)
          indexFileHandler.close()
      }
      catch{case ex : Throwable => }

      try{
        if(tmpIndexFileHandler != null)
          tmpIndexFileHandler.close()
      }
      catch{case ex : Throwable => }
    }

  }

  private def checkAndFixTargetDirArchiveStatus(targetDir : String, targetFiles : Array[String], locationInfo : LocationInfo) : Unit = {

    logger.info("checkAndFixTargetDirArchiveStatus: check target dir {}", targetDir)

    var archiveDirFileHandler : SmartFileHandler = null
    var targetDirFileHandler : SmartFileHandler = null
    try {
      val failoverArchiveFilesInfo = ArrayBuffer[ArchiveFileInfo]()

      val archiveInputConfig = SmartFileAdapterConfiguration.outputConfigToInputConfig(adapterConfig.archiveConfig.outputConfig)
      archiveInputConfig.monitoringConfig = adapterConfig.monitoringConfig
      archiveDirFileHandler = SmartFileHandlerFactory.createSmartFileHandler(archiveInputConfig, "/")
      targetDirFileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, "/")

      val lastIndexEntrySet = ArrayBuffer[ArchiveFileIndexEntry]()

      val archiveDirPath = getDestArchiveDir(locationInfo, targetDir, "dummyFileBaseName", scala.collection.immutable.Map[String, String]())
      logger.info("archive dir corresponding to target dir {} is {}", targetDir, archiveDirPath)

      val latestIndexFileOption = getDirLatestIndexFile(archiveDirPath, archiveDirFileHandler)
      if (latestIndexFileOption.isDefined) {

        val indexFilePath = latestIndexFileOption.get

        logger.info("found latest archive index: {}", indexFilePath)

        val indexFileSize = archiveDirFileHandler.length(indexFilePath).toInt
        val (lastDestArchiveFile, lastIndexEntrySetTmp, lastIndexEntrySetStartOffset) = parseIndexFile(archiveInputConfig, indexFilePath, indexFileSize)
        lastIndexEntrySet.appendAll(lastIndexEntrySetTmp)
        if (lastIndexEntrySet.nonEmpty) {
          val firstEntry = lastIndexEntrySet.head
          val lastEntry = lastIndexEntrySet.last

          val archiveFileExists = archiveDirFileHandler.exists(lastDestArchiveFile)

          //check if dest file exists and has lastEntry.destFileEndOffset
          val isIndexValid = archiveFileExists && hasOffset(archiveInputConfig, lastDestArchiveFile, lastEntry.destFileEndOffset)
          if (isIndexValid) {
            logger.debug("archive file {} is valid", lastDestArchiveFile)

            //delete any existing src files that were successfully archived

            lastIndexEntrySet.foreach(entry => {
              try {
                if(targetDirFileHandler.exists(entry.srcFile)) {
                  logger.warn("deleting file {} since it was already successfully archived and still exists", entry.srcFile)
                  targetDirFileHandler.deleteFile(entry.srcFile)
                }
              }
              catch {
                case ex: Throwable => logger.error("", ex)
              }
            })

          }

          if (!isIndexValid) {

            if (!archiveFileExists)
              logger.info("archive file {}, as in index file {} does not exist", lastDestArchiveFile, indexFilePath)
            else
              logger.info("archive file {}, as in index file {} is invalid", lastDestArchiveFile, indexFilePath)

            //if dest file exists, delete
            try {
              if (archiveFileExists) {
                logger.info("deleting archive file {}", lastDestArchiveFile)
                archiveDirFileHandler.deleteFile(lastDestArchiveFile)
              }
            }
            catch {
              case ex: Throwable => logger.error("", ex)
            }

            if (lastIndexEntrySet.nonEmpty) {
              logger.info("removing last entry set from index file {}, since it is not saved into disk", indexFilePath)
              removeLastIndexEntrySet(archiveInputConfig, indexFilePath, lastIndexEntrySetStartOffset)
            }

            //archive src files in lastIndexEntrySet,
            // but check first one, need to skip destFileStartOffset -1 bytes

            lastIndexEntrySet.foreach(entry => {

              val srcFileTokens = entry.srcFile.split("/")
              val flBaseName = srcFileTokens(srcFileTokens.length - 1)
              val offset = if (entry.srcFile.equals(firstEntry.srcFile)) entry.srcFileStartOffset else 0
              val componentsMap =
                if (locationInfo != null && locationInfo.fileComponents != null)
                  MonitorUtils.getFileComponents(targetDir + "/" + flBaseName, locationInfo)
                else null

              val archiveFileInfo = ArchiveFileInfo(adapterConfig,
                locationInfo, targetDir, flBaseName, componentsMap, 0, archiveDirPath, offset)

              //archiveFile(archiveFileInfo, archiveFileDirStatus)
              logger.info("Adding failover archive info to archive queue for file {}, with offset {}",
                targetDir + "/" + flBaseName, offset.toString)

              failoverArchiveFilesInfo.append(archiveFileInfo)
            })
            //flushArchiveDirStatus(archiveFileDirStatus)

          }
        }

      }
      else
        logger.info("no archive index file in dir {}", archiveDirPath)

      //get files in target folder that are not even in index file
      //smart file handlers since sorting files functionality requires that. these objects will not do anything
      //  so no need to disconnect
      val nonIndexedFiles = targetFiles.filter(file => !lastIndexEntrySet.exists(entry => entry.srcFile.equals(file))).
        map(filePath => SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, filePath)).
        map(handler => EnqueuedFileHandler(handler, 0, targetDirFileHandler.lastModified(handler.getFullPath),
          locationInfo, MonitorUtils.getFileComponents(handler.getFullPath, locationInfo)))


      //val nextRolloverTime = System.currentTimeMillis + archiveRolloverInterval * 60 * 1000
      //val archiveFileDirStatus = ArchiveDirStatus(archiveDirPath, new ByteArrayOutputStream(), null,
      //getNewArchiveFileName, 0, -1, nextRolloverTime, ArrayBuffer[String](), "", ArrayBuffer[ArchiveFileIndexEntry](), true)
      val orderedFiles = nonIndexedFiles.sortWith(MonitorUtils.compareFiles(_, _) < 0)
      orderedFiles.foreach(file => {
        val archiveFileInfo = ArchiveFileInfo(adapterConfig,
          locationInfo, targetDir, MonitorUtils.getFileName(file.fileHandler.getFullPath), file.componentsMap, 0, archiveDirPath, 0)

        //archiveFile(archiveFileInfo, archiveFileDirStatus)
        logger.info("Adding failover archive info to archive queue for file {}, with offset {}",
          file.fileHandler.getFullPath, "0")
        failoverArchiveFilesInfo.append(archiveFileInfo)
      })
      //flushArchiveDirStatus(archiveFileDirStatus)

      if (failoverArchiveFilesInfo.length > 0)
        addFailoverArchiveFilesInfo(failoverArchiveFilesInfo.toArray)
      else logger.info("no failover files need to archive from target dir {}", targetDir)
    }
    catch{
      case ex : Throwable =>
        logger.error("Error while checking failover status for dir " + targetDir, ex)
    }
    finally{
      try {
        if(archiveDirFileHandler != null)
          archiveDirFileHandler.disconnect()
      }
      catch {
        case ex: Throwable => logger.error("", ex)
      }
      try {
        if(targetDirFileHandler != null)
          targetDirFileHandler.disconnect()
      }
      catch {
        case ex: Throwable => logger.error("", ex)
      }

    }
  }

  //check if a file can be opened to read and has the provided offset
  //since file could be compressed, checking file size does not help
  private def hasOffset(adapterConfiguration: SmartFileAdapterConfiguration, filePath : String, offset : Long) : Boolean = {
    var fileHandler : SmartFileHandler = null
    try {
      logger.debug("hasOffset - checking for file {} , offset= {}", filePath, offset.toString)

      fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfiguration, filePath)
      fileHandler.openForRead()

      val byteBuffer = new Array[Byte](bufferSz)
      var totalReadLen = 0
      var curReadLen = 0
      var lengthToRead: Int = 0
      do {
        lengthToRead = Math.min(bufferSz, offset - totalReadLen).toInt
        curReadLen = fileHandler.read(byteBuffer, 0, lengthToRead)
        totalReadLen += curReadLen
        //logger.debug("SMART FILE CONSUMER - skipping {} bytes from file {} but got only {} bytes",
        //lengthToRead.toString, filePath, curReadLen.toString)
      } while (totalReadLen < offset && curReadLen > 0)
      //logger.debug("SMART FILE CONSUMER - totalReadLen from file {} is {}", filePath, totalReadLen.toString)

      logger.debug("hasOffset - final read offset is {}", totalReadLen.toString)

      totalReadLen >= offset

    }
    catch{
      case ex : Throwable => false
    }
    finally {
      if (fileHandler != null) {
        try {
          fileHandler.close()
        }
        catch {
          case ex: Throwable =>
        }
      }
    }
  }

  private def createIndexEntryFromString(line : String) : Option[ArchiveFileIndexEntry] = {
    val indexFieldsSeparator = ","
    val indexFieldsCount = 5
    try {
      val fields = line.split(indexFieldsSeparator, -1)
      if (fields.length >= indexFieldsCount) {
        Some(ArchiveFileIndexEntry(fields(0), fields(1), fields(2).toLong, fields(3).toLong,
          fields(4).toLong, fields(5).toLong, fields(6)))
      }
      else None
    }
    catch{
      case ex : Throwable => None
    }
  }

  var isShutdown = false

  def shutdown(): Unit ={

    isShutdown = true

    if(adapterConfig.archiveConfig == null)
      return

    if (archiveExecutor != null)
      archiveExecutor.shutdownNow()

    archiveExecutor = null

    forceFlushAllArchiveDirs()

    archiveInfoList.clear()
    archiveDirsStatusMap_lock.synchronized {
      if(archiveDirsStatusMap != null)
        archiveDirsStatusMap.clear()
    }
  }

}

case class ArchiveFileIndexEntry(var srcFile : String, var destFile : String,
                                 var srcFileStartOffset : Long, var srcFileEndOffset : Long,
                                 var destFileStartOffset : Long, var destFileEndOffset : Long, var ArchiveTimestamp : String)



//actualMemroyStream could be originalMemoryStream or compressed stream created using originalMemoryStream
case class ArchiveDirStatus(var dir : String, var originalMemoryStream : ByteArrayOutputStream, var actualMemoryStream : OutputStream,
                            var destFileName : String, var destFileCurrentSize : Long, var destFileCurrentOffset : Long,
                            var nextRolloverTime : Long, var srcFiles : ArrayBuffer[String], var lastSrcFilePath : String,
                            var archiveIndex : ArrayBuffer[ArchiveFileIndexEntry], var isActive : Boolean){

  def destFileFullPath = dir + "/" + destFileName
}