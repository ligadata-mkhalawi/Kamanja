package com.ligadata.InputAdapters

import java.io.OutputStream
import java.util.concurrent.locks.ReentrantReadWriteLock

import com.ligadata.AdaptersConfiguration.{LocationInfo, SmartFileAdapterConfiguration}
import com.ligadata.Exceptions.FatalAdapterException
import com.ligadata.OutputAdapters.PartitionStream
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.logging.log4j.LogManager

import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.ArrayBuffer


case class StreamFile(destDir: String, var destFileName: String, var outStream: OutputStream,
                         var currentFileSize: Long, var streamBuffer: ArrayBuffer[Byte], var flushBufferSize: Long)

class Archiver(adapterConfig: SmartFileAdapterConfiguration, smartFileConsumer: SmartFileConsumer) {

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  val maximumAppendableSize = 10

  private val currentAppendFiles =  scala.collection.mutable.LinkedHashMap[String, (String, Long, Long)]()

  /**

    * @param parentDestDir
    * @param srcFileSize
    * @return file name, size, last mod timestamp
    */
  def getCurrentAppendFile(parentDestDir : String, srcFileSize : Long) : Option[(String, Long, Long)] = {
    this.synchronized {
      //if (currentAppendFiles.contains(parentDir) && currentAppendFiles(parentDir).nonEmpty)
      //  Some(currentAppendFiles(parentDir).head._1, currentAppendFiles(parentDir).head._2, currentAppendFiles(parentDir).head.._3)
      //else None
      if (currentAppendFiles.contains(parentDestDir) )
        Some(currentAppendFiles(parentDestDir))
      else None
    }
  }


  def addToCurrentAppendFiles(parentDir : String, file : String, size : Long, timestamp : Long) = {
    this.synchronized {
      currentAppendFiles.put(parentDir, (file, size, timestamp))
    }
  }

  def removeCurrentAppendFile(parentDir : String) : Unit = {
    if(currentAppendFiles.contains(parentDir))
        currentAppendFiles.remove(parentDir)
  }

  def updateCurrentAppendFile(parentDir : String, file : String, newSize : Long, newTimestamp : Long) = {
    this.synchronized {

      //remove file info if too large to append to
        //TODO : is it better to consider files that are larger than max*ratio (instead of max) unfit for appending?
      if(currentAppendFiles.contains(parentDir)) {
        if (newSize >= adapterConfig.archiveConfig.consolidateThresholdBytes)
          currentAppendFiles.remove(parentDir)
        else currentAppendFiles.put(parentDir, (file, newSize, newTimestamp))
      }
      else
        currentAppendFiles.put(parentDir, (file, newSize, newTimestamp))
    }
  }

  private def trimFileFromLocalFileSystem(fileName: String): String = {
    if (fileName.startsWith("file://"))
      return fileName.substring("file://".length() - 1)
    fileName
  }

  val destFileFormat = "file_%s"
  def getNewArchiveFileName = destFileFormat.format(new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()))


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

  /*def archiveFile(locationInfo: LocationInfo, srcFileDir: String, srcFileBaseName: String, componentsMap: scala.collection.immutable.Map[String, String]): Boolean = {
    if (adapterConfig.archiveConfig == null || adapterConfig.archiveConfig.outputConfig == null)
      return true

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


    logger.warn("dstDirToArchive="+dstDirToArchive)

    //val destArchiveDirHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, dstDirToArchive)
    val osWriter = new com.ligadata.OutputAdapters.OutputStreamWriter()
    //might need to build sub-dirs corresponding to input dir structure
    val destArchiveDirExists =
      if (!osWriter.isFileExists(adapterConfig.archiveConfig.outputConfig, dstDirToArchive))
        osWriter.mkdirs(adapterConfig.archiveConfig.outputConfig, dstDirToArchive)
      else true

    if (!destArchiveDirExists) {
      logger.error("Archiving dest dir {} does not exist and could not be created", dstDirToArchive)
      return false
    }

    val srcFileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, srcFileToArchive, isBinary = false)

    if(adapterConfig.archiveConfig.consolidationMaxSizeGB <= 0){ //no consolidation
      val destFileToArchive = dstDirToArchive + "/" + srcFileBaseName
      val(status, _) = copyFileToArchiveAndDelete(locationInfo, srcFileHandler, destFileToArchive)
      status
    }
    else
      consolidateFile(locationInfo, srcFileHandler, dstDirToArchive)
  }


  def consolidateFile(locationInfo: LocationInfo,
                      fileHandler : SmartFileHandler, dstDirToArchive : String) : Boolean = {

    //var status = false
    var result : (Boolean, Long) = (false, 0)

    val currentFileToArchiveSize = fileHandler.length()
    val currentFileToArchiveTimestamp = fileHandler.lastModified()

    logger.info("currentFileToArchiveSize="+currentFileToArchiveSize.toString)

    val currentAppendFileInfo = getCurrentAppendFile(dstDirToArchive, currentFileToArchiveSize)

    logger.debug("adapterConfig.archiveConfig.consolidationMaxSizeGB={}",adapterConfig.archiveConfig.consolidationMaxSizeGB.toString)
    logger.info("consolidateThresholdBytes={}",adapterConfig.archiveConfig.consolidateThresholdBytes.toString)

    if(currentAppendFileInfo.isEmpty)
      logger.debug("Archiver: no entry for currentAppendFiles")
    else{
      logger.debug("Archiver: currentAppendFile ({}, {}, {})",
        currentAppendFileInfo.get._1, currentAppendFileInfo.get._2.toString, currentAppendFileInfo.get._3.toString)
    }

    if(currentFileToArchiveSize < adapterConfig.archiveConfig.consolidateThresholdBytes){
      if(currentAppendFileInfo.isEmpty){//no files already in dest
        logger.debug("Archiver: path 1")
        //copy from src to new file on dest
        val destFilePath = dstDirToArchive + "/" + getNewArchiveFileName
        result = copyFileToArchiveAndDelete(locationInfo, fileHandler, destFilePath)
        addToCurrentAppendFiles(dstDirToArchive, destFilePath, result._2, currentFileToArchiveTimestamp)//using timestamp of src?
      }
      else{
        val currentAppendFileSize = currentAppendFileInfo.get._2
        val currentAppendFilePath = currentAppendFileInfo.get._1

        logger.debug("currentAppendFileSize="+currentAppendFileSize.toString)
        if(currentAppendFileSize + currentFileToArchiveSize >  adapterConfig.archiveConfig.consolidateThresholdBytes){
          logger.debug("Archiver: path 2")

          //copy from src to new file on dest
          val destFilePath = dstDirToArchive + "/" + getNewArchiveFileName
          result = copyFileToArchiveAndDelete(locationInfo, fileHandler, destFilePath)
          //change currentAppendFilePath and size to new dest
          //removeFromCurrentAppendFiles(dstDirToArchive, currentAppendFilePath)
          updateCurrentAppendFile(dstDirToArchive, currentAppendFilePath, result._2, currentFileToArchiveTimestamp)
          addToCurrentAppendFiles(dstDirToArchive, destFilePath, result._2, currentFileToArchiveTimestamp)//using timestamp of src?
        }
        else{
          logger.debug("Archiver: path 3")
          //append src to currentAppendFilePath
          result = copyFileToArchiveAndDelete(locationInfo, fileHandler, currentAppendFilePath)

          //increase size currentAppendFileSize
          //val currentAppendFileSize = fileHandler.fileLength(currentAppendFilePath)
          logger.debug("path 3 . currentAppendFileSize="+result._2)
          updateCurrentAppendFile(dstDirToArchive, currentAppendFilePath, result._2, currentFileToArchiveTimestamp)
        }
      }
    }
    else{//currentFileToArchiveSize >= consolidateThresholdBytes
      //TODO : might need to split files larger than threshold

      logger.debug("Archiver: path 4")
      //copy from src to new file on dest
      val destFilePath = dstDirToArchive + "/" + getNewArchiveFileName
      result = copyFileToArchiveAndDelete(locationInfo, fileHandler, destFilePath)

      //if(currentAppendFileInfo.isDefined)
        //removeFromCurrentAppendFiles(dstDirToArchive, currentAppendFileInfo.get._1)
    }

    result._1
  }

  //return status and new size of dest file
  def copyFileToArchiveAndDelete(locationInfo: LocationInfo,
                                 srcFileHandler: SmartFileHandler, dstFileToArchive : String): (Boolean, Long) ={

    logger.warn("Archiver: Copying from {} to {}", srcFileHandler.getFullPath, dstFileToArchive)

    val osWriter = new com.ligadata.OutputAdapters.OutputStreamWriter()
    var os: OutputStream = null

    var status = false
    var resultSize : Long = 0

    try {
      //fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, srcFileToArchive, false)
      srcFileHandler.openForRead()

      //TODO: compare input compression to output compression, would this give better performance?
      //might need to do this: if same compression, can simply read and write as binary
      //else must open src to read using proper compression, and open dest for write with proper compression

      val originalOutputStream = osWriter.openFile(adapterConfig.archiveConfig.outputConfig, dstFileToArchive, canAppend = true)
      val compress = adapterConfig.archiveConfig.outputConfig.compressionString != null
      if(compress) {
        validateArchiveDestCompression()
        os = new CompressorStreamFactory().createCompressorOutputStream(adapterConfig.archiveConfig.outputConfig.compressionString, originalOutputStream)
      }
      else
        os = originalOutputStream

      var curReadLen = -1
      val bufferSz = 8 * 1024 * 1024
      val buf = new Array[Byte](bufferSz)

      do {
        curReadLen = srcFileHandler.read(buf, 0, bufferSz)
        if (curReadLen > 0) {
          os.write(buf, 0, curReadLen)
        }
      } while (curReadLen > 0)

      status = true
    } catch {
      case e: Throwable => {
        logger.error("Failed to archive file from " + srcFileHandler.getFullPath + " to " + dstFileToArchive, e)
        status = false
      }
    } finally {
      if (srcFileHandler != null) {
        try {
          srcFileHandler.close()
        } catch {
          case e: Throwable => {
            logger.error("Failed to close InputStream for " + dstFileToArchive, e)
          }
        }
      }

      if (os != null) {
        try {
          os.close()
          resultSize = osWriter.getFileSize(adapterConfig.archiveConfig.outputConfig, dstFileToArchive)
        } catch {
          case e: Throwable => {
            logger.error("Failed to close OutputStream for " + dstFileToArchive, e)
          }
        }
      }

      if (srcFileHandler != null) {
        try {
          srcFileHandler.deleteFile(srcFileHandler.getFullPath) // Deleting file after archive
        }
        catch {
          case e: Throwable => {
            logger.error("Failed to delete file " + dstFileToArchive, e)
          }
        }
      }
    }

    (status, resultSize)
  }
*/
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

    val compress = adapterConfig.archiveConfig.outputConfig.compressionString != null
    if (compress)
      os = new CompressorStreamFactory().createCompressorOutputStream(adapterConfig.archiveConfig.outputConfig.compressionString, originalStream)
    else
      os = originalStream

    os
  }

  private def archiveFilesGroup(dstDirToArchive : String, archInfoGroup: Array[ArchiveFileInfo]) : Boolean = {
    try {
      var streamFile: StreamFile = null
      val osWriter = new com.ligadata.OutputAdapters.OutputStreamWriter()
      val fc = adapterConfig.archiveConfig.outputConfig

      val appendFileOption = getCurrentAppendFile(dstDirToArchive, 0)

      val appendFileInfo =
        if (appendFileOption.isDefined) {
          //TODO : make sure file exists
          //val fileSize = osWriter.getFileSize(fc, dstDirToArchive + "/" + appendFileOption.get._1)
          (appendFileOption.get._1, appendFileOption.get._2)
        }
        else {
          val newFileName = getNewArchiveFileName
          updateCurrentAppendFile(dstDirToArchive, newFileName, 0, 0)
          (newFileName, 0L)
        }

      val appendFileName = appendFileInfo._1
      val appendFileCurrentSize = appendFileInfo._2

      logger.warn("initial appendFileName={}", appendFileName)
      logger.warn("initial appendFileCurrentSize={}", appendFileCurrentSize.toString)

      //var appendFilePath = dstDirToArchive + "/" + appendFileName

      streamFile = StreamFile(dstDirToArchive, appendFileName, null, appendFileCurrentSize, new ArrayBuffer[Byte](),
        adapterConfig.archiveConfig.consolidateThresholdBytes)

      //might need to build sub-dirs corresponding to input dir structure
      val destArchiveDirExists =
        if (!osWriter.isFileExists(adapterConfig.archiveConfig.outputConfig, dstDirToArchive))
          osWriter.mkdirs(adapterConfig.archiveConfig.outputConfig, dstDirToArchive)
        else true

      if (!destArchiveDirExists) {
        logger.error("Archiving dest dir {} does not exist and could not be created", dstDirToArchive)
        return false
      }

      var srcFileHandler : SmartFileHandler = null
      var previousDestFileSize : Long = 0

      var srcFileToArchive : String = null
      var archInfo : ArchiveFileInfo = null
      var archInfoGroupList = archInfoGroup.toList
      if(archInfoGroupList.nonEmpty){
        archInfo = archInfoGroupList.head
        archInfoGroupList = archInfoGroupList.tail
      }

      val archiveIndex = ArrayBuffer[ArchiveFileIndexEntry]()

      while(archInfo != null){

        val dstFileToArchive = streamFile.destDir + "/" + streamFile.destFileName
        srcFileToArchive = archInfo.srcFileDir + "/" + archInfo.srcFileBaseName

        logger.warn("current src file to archive {}", srcFileToArchive)
        logger.warn("current dest  file to archive {}", dstFileToArchive)

        //val currentFileToArchiveSize = srcFileHandler.length()
        //val currentFileToArchiveTimestamp = srcFileHandler.lastModified()

        var status = false
        var srcFileFinished = false
        var destFileFull = false
        var isCloseDestFile = false

        try {
          //fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, srcFileToArchive, false)
          if(srcFileHandler == null) {
            srcFileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, srcFileToArchive, isBinary = false)
            logger.warn("opening src file to read")
            srcFileHandler.openForRead()
          }

          if(streamFile.outStream == null) {
            logger.warn("opening dest file to write")
            streamFile.outStream = openStream(dstFileToArchive)
          }

          //TODO: compare input compression to output compression, would this give better performance?
          //might need to do this: if same compression, can simply read and write as binary
          //else must open src to read using proper compression, and open dest for write with proper compression

          var curReadLen = -1
          val bufferSz = 8 * 1024 * 1024
          val buf = new Array[Byte](bufferSz)

          previousDestFileSize = streamFile.currentFileSize
          do {
            val lengthToRead = Math.min(bufferSz, adapterConfig.archiveConfig.consolidateThresholdBytes - streamFile.currentFileSize).toInt
            logger.warn("lengthToRead={}",lengthToRead.toString)
            if(lengthToRead > 0) {
              curReadLen = srcFileHandler.read(buf, 0, lengthToRead)
              logger.warn("curReadLen={}",curReadLen.toString)
              if (curReadLen > 0) {
                streamFile.outStream.write(buf, 0, curReadLen)
                streamFile.currentFileSize += curReadLen
              }
            }
            else curReadLen = 0

          } while (curReadLen > 0 && streamFile.currentFileSize < adapterConfig.archiveConfig.consolidateThresholdBytes)

          status = true

          srcFileFinished = curReadLen <= 0
          destFileFull = streamFile.currentFileSize >= adapterConfig.archiveConfig.consolidateThresholdBytes
          isCloseDestFile = destFileFull || archInfoGroupList.isEmpty

          logger.warn("srcFileFinished={}", srcFileFinished.toString)
          logger.warn("destFileFull={}",destFileFull.toString )
          logger.warn("isCloseDestFile={}", isCloseDestFile.toString)

        } catch {
          case e: Throwable => {
            logger.error("Failed to archive file from " + srcFileHandler.getFullPath + " to " + dstFileToArchive, e)
            status = false
          }
        } finally {
          if (srcFileHandler != null && srcFileFinished) {
            try {
              srcFileHandler.close()
            } catch {
              case e: Throwable => {
                logger.error("Failed to close InputStream for " + dstFileToArchive, e)
              }
            }
          }

          if(streamFile.outStream != null && isCloseDestFile){
            logger.warn("closing dest file {} ", streamFile.destDir+"/"+streamFile.destFileName)
            streamFile.outStream.close()

            val sizeOnDisk = osWriter.getFileSize(adapterConfig.archiveConfig.outputConfig,
              streamFile.destDir+"/"+streamFile.destFileName)

            logger.warn("dest file{}. sizeOnDisk={}, estimated size={}",
              streamFile.destDir+"/"+streamFile.destFileName, sizeOnDisk.toString, streamFile.currentFileSize.toString)

            val entry = ArchiveFileIndexEntry(srcFileToArchive, streamFile.destDir+"/"+streamFile.destFileName,
              previousDestFileSize, streamFile.currentFileSize - 1)
            archiveIndex.append(entry)

            updateCurrentAppendFile(streamFile.destDir, streamFile.destFileName, streamFile.currentFileSize, 0)
            streamFile.outStream = null
          }
          if (destFileFull) {
            try {
              logger.warn("file {} is full", streamFile.destDir+"/"+streamFile.destFileName)
              //dest file is full, open a new one
              streamFile.destFileName = getNewArchiveFileName
              streamFile.currentFileSize = 0

              updateCurrentAppendFile(streamFile.destDir, streamFile.destFileName, streamFile.currentFileSize, 0)

            } catch {
              case e: Throwable => {
                logger.error("Failed to close OutputStream for " + dstFileToArchive, e)
              }
            }
          }

          if (srcFileHandler != null && srcFileFinished) {
            try {
              logger.warn("file {} is finished, deleting", srcFileHandler.getFullPath)
              srcFileHandler.deleteFile(srcFileHandler.getFullPath) // Deleting file after archive
              srcFileHandler = null

              if(archInfoGroupList.nonEmpty) {
                archInfo = archInfoGroupList.head
                archInfoGroupList = archInfoGroupList.tail
              }
              else{
                logger.warn("finished group")
                archInfo = null
              }
            }
            catch {
              case e: Throwable => {
                logger.error("Failed to delete file " + dstFileToArchive, e)
              }
            }
          }
        }
      }

      archiveIndex.foreach(entry => println(entry))
      true
    }
    catch{
      case ex : Exception =>
        logger.error("Error while archiving", ex)
        false
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
                                 var destFileStartOffset : Long, var destFileEndOffset : Long)
