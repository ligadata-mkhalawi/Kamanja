package com.ligadata.InputAdapters

import java.io.OutputStream
import java.util.concurrent.locks.ReentrantReadWriteLock

import com.ligadata.AdaptersConfiguration.{LocationInfo, SmartFileAdapterConfiguration}
import com.ligadata.Exceptions.FatalAdapterException
import com.ligadata.OutputAdapters.{OutputStreamWriter, PartitionStream}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.logging.log4j.LogManager

import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.ArrayBuffer

import java.io.ByteArrayOutputStream


case class StreamFile(destDir: String, var destFileName: String, var outStream: OutputStream,
                         var currentFileSize: Long, var streamBuffer: ArrayBuffer[Byte], var flushBufferSize: Long,
                         var currentActualOffset : Long){
  def destFileFullPath = destDir + "/" + destFileName
}

class Archiver(adapterConfig: SmartFileAdapterConfiguration, smartFileConsumer: SmartFileConsumer) {

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  val maximumAppendableSize = 10

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
    val callstack = Thread.currentThread().getStackTrace().drop(1).take(2).
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
  private var archiveInfo = new scala.collection.mutable.LinkedHashMap[String, ArrayBuffer[ArchiveFileInfo]]()
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

  def hasNextArchiveFileInfo: Boolean = {
    (archiveInfo.size > 0)
  }

  def getNextArchiveFileInfoGroup(): (String, Array[ArchiveFileInfo]) = {
    var archInfo: Array[ArchiveFileInfo] = null
    var dir : String = null
    ReadLock(_reent_lock)
    try {
      if (archiveInfo.size > 0) {
        val archInfoTuple = archiveInfo.head
        archInfo = archInfoTuple._2.toArray
        archiveInfo.remove(archInfoTuple._1)
        dir = archInfoTuple._1
      }
    } finally {
      ReadUnlock(_reent_lock)
    }
    (dir, archInfo)
  }

  def addArchiveFileInfo(archInfo: ArchiveFileInfo): Unit = {
    WriteLock(_reent_lock)
    try {
      val destArchiveDir = getDestArchiveDir(archInfo.locationInfo, archInfo.srcFileDir, archInfo.srcFileBaseName, archInfo.componentsMap)
      archInfo.destArchiveDir = destArchiveDir

      val archInfoGroup =
        if(archiveInfo.contains(destArchiveDir)) archiveInfo(destArchiveDir)
        else ArrayBuffer[ArchiveFileInfo]()

      archInfoGroup.append(archInfo)
      archiveInfo.put(destArchiveDir, archInfoGroup)

    } finally {
      WriteUnlock(_reent_lock)
    }
  }

  val archiveParallelism = 3// TODO
  val archiveSleepTimeInMs = 100
  private var archiveExecutor : ExecutorService = null


  //key is parent dir of src file
  private val streamFiles: collection.mutable.Map[String, StreamFile] = collection.mutable.Map[String, StreamFile]()

  def startArchiving(): Unit ={
    archiveExecutor = Executors.newFixedThreadPool(archiveParallelism)
    for (i <- 0 until archiveParallelism) {
      val archiveThread = new Runnable() {
        override def run(): Unit = {
          var interruptedVal = false
          while (!interruptedVal) {
            try {
              if (hasNextArchiveFileInfo) {
                val archInfoGroupTuple = getNextArchiveFileInfoGroup()
                if (archInfoGroupTuple != null && archInfoGroupTuple._2 != null && archInfoGroupTuple._2.length > 0) {
                  archiveFilesGroup(archInfoGroupTuple._1, archInfoGroupTuple._2)
                }
              } else {
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

  private def archiveFilesGroup(dstDirToArchive : String, archInfoGroup: Array[ArchiveFileInfo]) : Boolean = {
    try {
      var streamFile: StreamFile = null
      val osWriter = new com.ligadata.OutputAdapters.OutputStreamWriter()
      val fc = adapterConfig.archiveConfig.outputConfig

      logger.info("archiving following files "+archInfoGroup.map(a=>a.srcFileBaseName).mkString(","))
      logger.info("consolidateThresholdBytes="+adapterConfig.archiveConfig.consolidateThresholdBytes)

      val appendFileOption = getCurrentAppendFile(dstDirToArchive, 0)

      val appendFileInfo =
        if (appendFileOption.isDefined) {
          //TODO : make sure file exists
          //val fileSize = osWriter.getFileSize(fc, dstDirToArchive + "/" + appendFileOption.get._1)
          (appendFileOption.get._1, appendFileOption.get._2, appendFileOption.get._3)
        }
        else {
          val newFileName = getNewArchiveFileName
          updateCurrentAppendFile(dstDirToArchive, newFileName, 0, -1, 0)
          (newFileName, 0L, -1L)
        }

      val appendFileName = appendFileInfo._1
      val appendFileCurrentSize = appendFileInfo._2
      val appendFileCurrentActualOffset = appendFileInfo._3

      logger.debug("initial appendFileName={}", appendFileName)
      logger.debug("initial appendFileCurrentSize={}", appendFileCurrentSize.toString)
      logger.debug("initial appendFileCurrentActualOffset={}", appendFileCurrentActualOffset.toString)

      //var appendFilePath = dstDirToArchive + "/" + appendFileName

      var originalMemroyStream = new ByteArrayOutputStream
      val compress = adapterConfig.archiveConfig.outputConfig.compressionString != null
      var memroyStream : OutputStream = null

      streamFile = StreamFile(dstDirToArchive, appendFileName, null, appendFileCurrentSize, new ArrayBuffer[Byte](),
        adapterConfig.archiveConfig.consolidateThresholdBytes, appendFileCurrentActualOffset)

      //might need to build sub-dirs corresponding to input dir structure
      val destArchiveDirExists =
        if (!osWriter.isFileExists(adapterConfig.archiveConfig.outputConfig, dstDirToArchive))
          osWriter.mkdirs(adapterConfig.archiveConfig.outputConfig, dstDirToArchive)
        else true

      if (!destArchiveDirExists) {
        logger.error("Archiving dest dir {} does not exist and could not be created", dstDirToArchive)
        return false
      }

      var srcFileHandler: SmartFileHandler = null
      var previousDestFileSize: Long = 0
      var archInfoGroupList = archInfoGroup
      var srcFileToArchive: String = null


      val archiveIndex = ArrayBuffer[ArchiveFileIndexEntry]()

      while (archInfoGroupList.nonEmpty) {

        val archInfo = archInfoGroupList.head
        archInfoGroupList = archInfoGroupList.tail

        //val dstFileToArchive = streamFile.destDir + "/" + streamFile.destFileName
        srcFileToArchive = archInfo.srcFileDir + "/" + archInfo.srcFileBaseName
        logger.debug("current src file to archive {}", srcFileToArchive)
        logger.debug("current dest  file to archive {}", streamFile.destFileFullPath)

        try {
          srcFileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, srcFileToArchive, isBinary = false)
          logger.debug("opening src file to read {}", srcFileHandler.getFullPath)
          srcFileHandler.openForRead()

          val entry = ArchiveFileIndexEntry(srcFileToArchive, streamFile.destFileFullPath,
            streamFile.currentActualOffset + 1, -1, "")
          archiveIndex.append(entry)

          //TODO: compare input compression to output compression, would this give better performance?
          //might need to do this: if same compression, can simply read and write as binary
          //else must open src to read using proper compression, and open dest for write with proper compression

          var lastReadLen = -1
          var actualBufferLen = 0
          val bufferSz = 8 * 1024 * 1024
          val byteBuffer = new Array[Byte](bufferSz)

          val message_separator: Char =
            if (archInfo.locationInfo == null) adapterConfig.monitoringConfig.messageSeparator
            else archInfo.locationInfo.messageSeparator

          do {
            logger.debug("destFileName=" + streamFile.destFileName)
            logger.debug("streamFile.currentFileSize=" + streamFile.currentFileSize)


            val lengthToRead = Math.min(bufferSz, adapterConfig.archiveConfig.consolidateThresholdBytes - streamFile.currentFileSize).toInt
            logger.debug("lengthToRead={}", lengthToRead.toString)
            if (lengthToRead > 0) {
              if (lengthToRead <= actualBufferLen) {
                logger.debug("memory stream is almost full. cannot write leftover from last iteration of src file {}. saving to file {} and opening a new one",
                  srcFileHandler.getFullPath, streamFile.destFileName)

                //copy from memory to dest archive file
                val diskOutputStream = openStream(streamFile.destFileFullPath)
                originalMemroyStream.writeTo(diskOutputStream)
                diskOutputStream.close()
                originalMemroyStream.reset()

                streamFile.destFileName = getNewArchiveFileName
                logger.debug("new file name is "+streamFile.destFileName)
                logger.debug("resetting currentFileSize to 0")
                streamFile.currentFileSize = 0

                val previousEntry = if(archiveIndex.nonEmpty) archiveIndex.last else null
                if(previousEntry != null && previousEntry.srcFile.equals(srcFileHandler.getFullPath)) {
                  previousEntry.destFileEndOffset = streamFile.currentActualOffset
                  previousEntry.ArchiveTimestamp = getCurrentTimestamp
                }
                else{
                  logger.warn("file {} should already have an entry in archive index", srcFileHandler.getFullPath)
                }
                val newEntry = ArchiveFileIndexEntry(srcFileToArchive, streamFile.destFileFullPath,
                  0, -1, "")
                archiveIndex.append(newEntry)


                streamFile.currentActualOffset = -1
                updateCurrentAppendFile(streamFile.destDir, streamFile.destFileName,
                  streamFile.currentFileSize, streamFile.currentActualOffset, 0)

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
                  val remainingArchiveSpace = adapterConfig.archiveConfig.consolidateThresholdBytes - streamFile.currentFileSize
                  logger.debug("streamFile.currentFileSize=" + streamFile.currentFileSize)
                  logger.debug("remainingArchiveSpace=" + remainingArchiveSpace)
                  logger.debug("actualLenToWrite=" + actualLenToWrite)

                  if (remainingArchiveSpace < actualLenToWrite) {
                    logger.debug("file {} is full", streamFile.destDir + "/" + streamFile.destFileName)
                    //dest file is full
                    //copy from memory to dest archive file
                    val diskOutputStream = openStream(streamFile.destFileFullPath)
                    originalMemroyStream.writeTo(diskOutputStream)
                    diskOutputStream.close()
                    originalMemroyStream.reset()

                    streamFile.destFileName = getNewArchiveFileName
                    logger.debug("resetting currentFileSize to 0")
                    streamFile.currentFileSize = 0

                    val previousEntry = if(archiveIndex.nonEmpty) archiveIndex.last else null
                    if(previousEntry != null && previousEntry.srcFile.equals(srcFileHandler.getFullPath)) {
                      previousEntry.destFileEndOffset = streamFile.currentActualOffset
                      previousEntry.ArchiveTimestamp = getCurrentTimestamp
                    }
                    else{
                      logger.warn("file {} should already have an entry in archive index", srcFileHandler.getFullPath)
                    }
                    val newEntry = ArchiveFileIndexEntry(srcFileToArchive, streamFile.destFileFullPath,
                      0, -1, "")
                    archiveIndex.append(newEntry)

                    streamFile.currentActualOffset = -1
                    updateCurrentAppendFile(streamFile.destDir, streamFile.destFileName, streamFile.currentFileSize,
                      streamFile.currentActualOffset, 0)

                  }

                  logger.debug("writing to memory stream actualLenToWrite ={}", actualLenToWrite.toString)
                  if (compress)
                    streamFile.outStream = new CompressorStreamFactory().createCompressorOutputStream(adapterConfig.archiveConfig.outputConfig.compressionString, originalMemroyStream)
                  else
                    streamFile.outStream = originalMemroyStream
                  streamFile.outStream.write(byteBuffer, 0, actualLenToWrite)
                  streamFile.outStream.close()
                  streamFile.currentActualOffset += actualLenToWrite

                  logger.debug("current buffer:" + new String(byteBuffer.slice(0, actualBufferLen)))
                  logger.debug("written:" + new String(byteBuffer.slice(0, actualLenToWrite)))

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

            logger.debug("lastReadLen="+lastReadLen)
          } while (lastReadLen > 0)

        } catch {
          case e: Throwable => {
            logger.error("Failed to archive file from " + srcFileHandler.getFullPath + " to " + streamFile.destFileFullPath, e)
            //status = false
          }
        } finally {
          if (srcFileHandler != null) {
            try {
              srcFileHandler.close()
            } catch {
              case e: Throwable => {
                logger.error("Failed to close InputStream for " + streamFile.destFileFullPath, e)
              }
            }

            val entry = if(archiveIndex.nonEmpty) archiveIndex.last else null
            if(entry != null && entry.srcFile.equals(srcFileHandler.getFullPath)) {
              entry.destFileEndOffset = streamFile.currentActualOffset
              entry.ArchiveTimestamp = getCurrentTimestamp
            }
            else{
              logger.warn("file {} does not have an entry in archive index", srcFileHandler.getFullPath)
            }

            try {
              logger.info("file {} is finished, deleting", srcFileHandler.getFullPath)
              srcFileHandler.deleteFile(srcFileHandler.getFullPath) // Deleting file after archive
              srcFileHandler = null

            }
            catch {
              case e: Throwable => {
                logger.error("Failed to delete file " + streamFile.destFileFullPath, e)
              }
            }

          }

        }

      }

      if (originalMemroyStream.size() > 0) {
        logger.info("finished group, writing final leftovers to file {} ", streamFile.destFileFullPath)
        val compressedSize = originalMemroyStream.size()

        val diskOutputStream = openStream(streamFile.destFileFullPath)
        originalMemroyStream.writeTo(diskOutputStream)
        diskOutputStream.close()
        logger.debug("old file size is {}, new size is {}",
          streamFile.currentFileSize.toString, (streamFile.currentFileSize + compressedSize).toString)
        streamFile.currentFileSize += compressedSize// += actualLenToWrite
        updateCurrentAppendFile(streamFile.destDir, streamFile.destFileName, streamFile.currentFileSize,
          streamFile.currentActualOffset, 0)
        originalMemroyStream.reset()

        //TODO - archive index needs to be updated
      }

      updateCurrentAppendFile(streamFile.destDir, streamFile.destFileName, streamFile.currentFileSize,
        streamFile.currentActualOffset, 0)

      archiveIndex.foreach(a => logger.debug(a.toString))
      dumpArchiveIndex(osWriter, dstDirToArchive, archiveIndex.toArray)

      true
    }
    catch {
      case ex: Exception =>
        logger.error("Error while archiving", ex)
        false
    }

  }

  def dumpArchiveIndex(osWriter : OutputStreamWriter,
                       parentFolder : String, archiveIndex: Array[ArchiveFileIndexEntry]) : Unit = {
    try {
      if (archiveIndex == null || archiveIndex.length == 0)
        return
      val path = parentFolder + "/" + archiveIndexFileName
      logger.info("dumping current archive index to file " + path)
      val fc = adapterConfig.archiveConfig.outputConfig
      val os = osWriter.openFile(fc, path, true)
      archiveIndex.foreach(ai => {
        val lineAr = Array(ai.srcFile, ai.destFile, ai.destFileStartOffset, ai.destFileEndOffset, ai.ArchiveTimestamp)
        os.write((lineAr.mkString(",") + "\n").getBytes())
      })
      os.close()
    }
    catch{
      case ex : Throwable => //todo - retry?
        logger.error("Error while dumping archive index for dir " + parentFolder, ex)
    }
  }

  def shutdown(): Unit ={
    if (archiveExecutor != null)
      archiveExecutor.shutdownNow()
    archiveExecutor = null
    archiveInfo.clear()
  }
}

case class ArchiveFileIndexEntry(var srcFile : String, var destFile : String,
                                 var destFileStartOffset : Long, var destFileEndOffset : Long, var ArchiveTimestamp : String)
