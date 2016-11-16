package com.ligadata.InputAdapters

import java.io.OutputStream
import java.util.concurrent.locks.ReentrantReadWriteLock

import com.ligadata.AdaptersConfiguration.{LocationInfo, SmartFileAdapterConfiguration}
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

  val archiveRolloverInterval : Long = 60 //in minutes
  val archiveRolloverCheckSleepMS = 1 * 60 * 1000

  private val currentAppendFiles =  scala.collection.mutable.LinkedHashMap[String, (String, Long, Long, Long)]()

  /**

    * @param parentDestDir
    * @param srcFileSize
    * @return file name, size, byte offset, last mod timestamp
    */
  def getCurrentAppendFile(parentDestDir : String, srcFileSize : Long) : Option[(String, Long, Long, Long)] = {
    this.synchronized {
      //if (currentAppendFiles.contains(parentDir) && currentAppendFiles(parentDir).nonEmpty)
      //  Some(currentAppendFiles(parentDir).head._1, currentAppendFiles(parentDir).head._2, currentAppendFiles(parentDir).head.._3)
      //else None
      if (currentAppendFiles.contains(parentDestDir) )
        Some(currentAppendFiles(parentDestDir))
      else None
    }
  }


  def addToCurrentAppendFiles(parentDir : String, file : String, size : Long, byteOffset : Long, timestamp : Long) = {
    this.synchronized {
      currentAppendFiles.put(parentDir, (file, size, byteOffset, timestamp))
    }
  }

  def removeCurrentAppendFile(parentDir : String) : Unit = {
    if(currentAppendFiles.contains(parentDir))
        currentAppendFiles.remove(parentDir)
  }

  def updateCurrentAppendFile(parentDir : String, file : String, newSize : Long, newByteOffset : Long, newTimestamp : Long) = {
    this.synchronized {

      val callstack = Thread.currentThread().getStackTrace().drop(1).take(2).
        map(s => s.getClassName + "." + s.getMethodName + "(" + s.getLineNumber + ")").mkString("\n")
      logger.debug("updating CurrentAppendFile with newByteOffset={}", newByteOffset.toString + ". "  + callstack)

      //remove file info if too large to append to
        //TODO : is it better to consider files that are larger than max*ratio (instead of max) unfit for appending?
      if(currentAppendFiles.contains(parentDir)) {
        if (newSize >= adapterConfig.archiveConfig.consolidateThresholdBytes)
          currentAppendFiles.remove(parentDir)
        else currentAppendFiles.put(parentDir, (file, newSize, newByteOffset, newTimestamp))
      }
      else
        currentAppendFiles.put(parentDir, (file, newSize, newByteOffset, newTimestamp))
    }
  }

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
      if(adapterConfig.archiveConfig.createDirPerLocation) {
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

  def clear() = {
    currentAppendFiles.clear()
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
      //get corresponding archive dir here
      val destArchiveDir = getDestArchiveDir(archInfo.locationInfo, archInfo.srcFileDir, archInfo.srcFileBaseName, archInfo.componentsMap)
      archInfo.destArchiveDir = destArchiveDir

      archiveInfoList :+= archInfo
    } finally {
      WriteUnlock(archiveInfoList_reent_lock)
    }
  }



  val archiveParallelism = 1// TODO
  val archiveSleepTimeInMs = 2000//100
  private var archiveExecutor : ExecutorService = null


  def startArchiving(): Unit ={
    archiveExecutor = Executors.newFixedThreadPool(archiveParallelism + 1)
    for (i <- 0 until archiveParallelism) {
      val archiveThread = new Runnable() {
        override def run(): Unit = {
          var interruptedVal = false
          while (!interruptedVal) {
            try {
              if (hasNextArchiveFileInfo) {
                val archInfo = getNextArchiveFileInfo
                if (archInfo != null && archInfo._1 != null && archInfo._2 != null) {
                  logger.debug("got file to archive from queue: {}", archInfo._1.srcFileBaseName)
                  archiveFile(archInfo._1, archInfo._2)
                }
                else {
                  logger.debug("no files in archive queue")
                  interruptedVal = smartFileConsumer.sleepMs(archiveSleepTimeInMs)
                }
              }
              else {
                logger.debug("no files in archive queue")
                if (archiveSleepTimeInMs > 0)
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
        }
      }
      archiveExecutor.execute(archiveThread)
    }

    val rolloverCheckThread = new Runnable() {
      override def run(): Unit = {
        var interruptedVal = false
        while (!interruptedVal) {
          try {
            checkRolloverDueFiles()
            interruptedVal = smartFileConsumer.sleepMs(archiveRolloverCheckSleepMS)
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
    logger.debug("Archiver: checking rollover intervals")
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
            tuple._2.isActive = true // lock the dir
          }
        })
      }

      dirsToFlush.foreach(dir => {
        logger.info("Archiver: Flushing data for dir {} since rollover is due", dir)
        val dirStatus = archiveDirsStatusMap.getOrElse(dir, null)
        if(dirStatus != null){
          //dir is already locked, no need to lock the map

          flushArchiveDirStatus(dirStatus)

          dirStatus.nextRolloverTime = System.currentTimeMillis + archiveRolloverInterval * 60 * 1000
          dirStatus.isActive = false
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
  private def flushArchiveDirStatus(archiveDirStatus : ArchiveDirStatus) : Unit = {

    val lastSrcFilePath = archiveDirStatus.lastSrcFilePath
    if(lastSrcFilePath != null && lastSrcFilePath.length > 0) {

      val osWriter = new com.ligadata.OutputAdapters.OutputStreamWriter()
      val archiveIndex = archiveDirStatus.archiveIndex

      //update and dump index first
      val previousEntry = if (archiveIndex.nonEmpty) archiveIndex.last else null
      if (previousEntry != null && previousEntry.srcFile.equals(lastSrcFilePath)) {
        previousEntry.destFileEndOffset = archiveDirStatus.destFileCurrentOffset
        previousEntry.ArchiveTimestamp = getCurrentTimestamp
      }
      else {
        logger.warn("file {} should already have an entry in archive index", lastSrcFilePath)
      }
      dumpArchiveIndex(osWriter, archiveDirStatus.dir, archiveIndex)

      //dump archive
      val diskOutputStream = openStream(archiveDirStatus.destFileFullPath)
      archiveDirStatus.originalMemoryStream.writeTo(diskOutputStream)
      diskOutputStream.close()
      archiveDirStatus.originalMemoryStream.reset()

      archiveDirStatus.destFileName = getNewArchiveFileName
      logger.debug("new file name is " + archiveDirStatus.destFileName)
      logger.debug("resetting currentFileSize to 0")
      archiveDirStatus.destFileCurrentSize = 0
      val newEntry = ArchiveFileIndexEntry(lastSrcFilePath, archiveDirStatus.destFileFullPath, 0, -1, "")
      archiveIndex.append(newEntry)

      archiveDirStatus.destFileCurrentOffset = -1
      archiveDirStatus.nextRolloverTime = System.currentTimeMillis + archiveRolloverInterval * 60 * 1000

      //archiveDirStatus.archiveIndex.clear() ????

      //todo : need to update status map?


      //now delete src files
      archiveDirStatus.srcFiles.foreach(file => {
        try {
          val srcFileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, file)
          logger.info("file {} is finished, deleting", srcFileHandler.getFullPath)
          srcFileHandler.deleteFile(srcFileHandler.getFullPath) // Deleting file after archive
        }
        catch {
          case e: Throwable => {
            logger.error("Failed to delete file " + archiveDirStatus.destFileFullPath, e)
          }
        }
      })

    }
  }

  private def archiveFile(archInfo: ArchiveFileInfo, archiveDirStatus : ArchiveDirStatus) : Boolean = {

    val srcFileToArchive = archInfo.srcFileDir + "/" + archInfo.srcFileBaseName
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

        val entry = ArchiveFileIndexEntry(srcFileToArchive, archiveDirStatus.destFileFullPath,
          archiveDirStatus.destFileCurrentOffset + 1, -1, "")
        archiveIndex.append(entry)

        var lastReadLen = -1
        var actualBufferLen = 0
        val bufferSz = 8 * 1024 * 1024
        val byteBuffer = new Array[Byte](bufferSz)

        val message_separator: Char =
          if (archInfo.locationInfo == null) adapterConfig.monitoringConfig.messageSeparator
          else archInfo.locationInfo.messageSeparator

        do {
          val currentInMemorySize = archiveDirStatus.originalMemoryStream.size()
          logger.debug("reading destFileName=" + archiveDirStatus.destFileName)
          logger.debug("streamFile.currentFileSize=" + archiveDirStatus.destFileCurrentSize)
          logger.debug("currentInMemorySize=" + currentInMemorySize)
          logger.debug("actualBufferLen=" + actualBufferLen)

          val lengthToRead = Math.min(bufferSz,
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
              lastReadLen = srcFileHandler.read(byteBuffer, actualBufferLen, lengthToRead)
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
                logger.debug("old in memory size = "+archiveDirStatus.originalMemoryStream.size)
                archiveDirStatus.actualMemoryStream = getProperStream(archiveDirStatus.originalMemoryStream)
                archiveDirStatus.actualMemoryStream.write(byteBuffer, 0, actualLenToWrite)
                archiveDirStatus.actualMemoryStream.close()
                archiveDirStatus.destFileCurrentOffset += actualLenToWrite

                logger.debug("new in memory size = "+archiveDirStatus.originalMemoryStream.size)

                //logger.debug("current buffer:" + new String(byteBuffer.slice(0, actualBufferLen)))
                //logger.debug("written:" + new String(byteBuffer.slice(0, actualLenToWrite)))
                if(archiveDirStatus.originalMemoryStream.size >= adapterConfig.archiveConfig.consolidateThresholdBytes){
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


        } while (lastReadLen > 0)

        archiveDirStatus.srcFiles.append(srcFileToArchive)//to be deleted when archive is dumped to disk
      }
    }
    catch {
      case e: Throwable => {
        logger.error("Failed to archive file from " + srcFileHandler.getFullPath + " to " + archiveDirStatus.destFileFullPath, e)
        //status = false
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
        }
        else{
          logger.warn("file {} does not have an entry in archive index", srcFileHandler.getFullPath)
        }

        /*try {
          logger.info("file {} is finished, deleting", srcFileHandler.getFullPath)
          srcFileHandler.deleteFile(srcFileHandler.getFullPath) // Deleting file after archive
          srcFileHandler = null

        }
        catch {
          case e: Throwable => {
            logger.error("Failed to delete file " + archiveDirStatus.destFileFullPath, e)
          }
        }*/

      }

      releaseArchiveDirLock(archiveDirStatus.dir)
    }

    archiveIndex.foreach(a => logger.debug(a.toString))

    true
  }

  def dumpArchiveIndex(osWriter : OutputStreamWriter,
                       parentFolder : String, archiveIndex: ArrayBuffer[ArchiveFileIndexEntry]) : Unit = {
    try {
      if (archiveIndex == null || archiveIndex.isEmpty)
        return
      val path = parentFolder + "/" + archiveIndexFileName
      logger.info("dumping current archive index to file " + path)
      val fc = adapterConfig.archiveConfig.outputConfig
      val os = osWriter.openFile(fc, path, canAppend = true)
      archiveIndex.foreach(ai => {
        val lineAr = Array(ai.srcFile, ai.destFile, ai.destFileStartOffset, ai.destFileEndOffset, ai.ArchiveTimestamp)
        os.write((lineAr.mkString(",") + "\n").getBytes())
      })
      os.close()

      archiveIndex.clear()
    }
    catch{
      case ex : Throwable => //todo - retry?
        logger.error("Error while dumping archive index for dir " + parentFolder, ex)
    }
  }

  def forceFlushAllArchiveDirs(): Unit ={
    archiveDirsStatusMap.foreach(dirTuple =>{

      val archiveDirStatus = dirTuple._2
      try {
        if(archiveDirStatus.originalMemoryStream.size() > 0) {
          flushArchiveDirStatus(archiveDirStatus)
        }

      }
      catch{
        case ex : Throwable => logger.error("", ex)
      }

    })
  }

  def shutdown(): Unit ={

    if (archiveExecutor != null)
      archiveExecutor.shutdownNow()

    forceFlushAllArchiveDirs()

    archiveExecutor = null
    archiveInfoList.clear()
    archiveDirsStatusMap.clear()
  }

}

case class ArchiveFileIndexEntry(var srcFile : String, var destFile : String,
                                 var destFileStartOffset : Long, var destFileEndOffset : Long, var ArchiveTimestamp : String)


//TODO : add last flush time (for rollover)
//actualMemroyStream could be originalMemoryStream or compressed stream created using originalMemoryStream
case class ArchiveDirStatus(var dir : String, var originalMemoryStream : ByteArrayOutputStream, var actualMemoryStream : OutputStream,
                            var destFileName : String, var destFileCurrentSize : Long, var destFileCurrentOffset : Long,
                            var nextRolloverTime : Long, var srcFiles : ArrayBuffer[String], var lastSrcFilePath : String,
                            var archiveIndex : ArrayBuffer[ArchiveFileIndexEntry], var isActive : Boolean){

  def destFileFullPath = dir + "/" + destFileName
}