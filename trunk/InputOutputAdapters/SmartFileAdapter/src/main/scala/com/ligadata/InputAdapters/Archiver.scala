package com.ligadata.InputAdapters

import java.io.OutputStream

import com.ligadata.AdaptersConfiguration.{LocationInfo, SmartFileAdapterConfiguration}
import com.ligadata.Exceptions.FatalAdapterException
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.logging.log4j.LogManager

import scala.collection.mutable.ArrayBuffer


class Archiver(adapterConfig: SmartFileAdapterConfiguration) {

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  val maximumAppendableSize = 10

  private val currentAppendFiles =  scala.collection.mutable.LinkedHashMap[String, //scala.collection.mutable.LinkedHashMap[String, (Long, Long)]]()
    ArrayBuffer[(String, Long, Long)]]()

  /**
    * search for appendable archived file compared with src file size
    * @param parentDir
    * @param srcFileSize
    * @return file name, size, last mod timestamp
    */
  def getCurrentAppendFile(parentDir : String, srcFileSize : Long) : Option[(String, Long, Long)] = {
    this.synchronized {
      //if (currentAppendFiles.contains(parentDir) && currentAppendFiles(parentDir).nonEmpty)
      //  Some(currentAppendFiles(parentDir).head._1, currentAppendFiles(parentDir).head._2, currentAppendFiles(parentDir).head.._3)
      //else None

      if (currentAppendFiles.contains(parentDir) && currentAppendFiles(parentDir).nonEmpty){
        currentAppendFiles(parentDir).
          find(fileInfo => fileInfo._2 + srcFileSize <= adapterConfig.archiveConfig.consolidateThresholdBytes) match {
          case Some(fileInfo) => Some(fileInfo)
          case None => None
        }
      }
      else None
    }
  }

  /*def removeFromCurrentAppendFiles(parentDir : String, file : String) = {
    this.synchronized {
      if (currentAppendFiles.contains(parentDir))
        currentAppendFiles(parentDir).remove(file)
    }
  }*/

  def addToCurrentAppendFiles(parentDir : String, file : String, size : Long, timestamp : Long) = {
    this.synchronized {

      val buffer =
        if (currentAppendFiles.contains(parentDir)) currentAppendFiles(parentDir)
        else ArrayBuffer[(String, Long, Long)]()
      buffer.append((file, size, timestamp))
      if(buffer.length > maximumAppendableSize) buffer.remove(0)

      currentAppendFiles.put(parentDir, buffer)

    }
  }
  def updateCurrentAppendFile(parentDir : String, file : String, newSize : Long, newTimestamp : Long) = {
    this.synchronized {
      val buffer = currentAppendFiles(parentDir)

      val idx = buffer.indexWhere(tuple => tuple._1.equals(file))
      if(idx >= 0) {//remove file info if too large to append to
        //TODO : is it better to consider files that are larger than max*ratio (instead of max) unfit for appending?
        if(newSize >= adapterConfig.archiveConfig.consolidateThresholdBytes)
          buffer.remove(idx)
        else buffer(idx) = (file, newSize, newTimestamp)
      }
      //else ??
    }
  }

  private def trimFileFromLocalFileSystem(fileName: String): String = {
    if (fileName.startsWith("file://"))
      return fileName.substring("file://".length() - 1)
    fileName
  }

  val destFileFormat = "file_%s"
  def getNewArchiveFileName = destFileFormat.format(new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()))



  def archiveFile(locationInfo: LocationInfo, srcFileDir: String, srcFileBaseName: String, componentsMap: scala.collection.immutable.Map[String, String]): Boolean = {
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
}
