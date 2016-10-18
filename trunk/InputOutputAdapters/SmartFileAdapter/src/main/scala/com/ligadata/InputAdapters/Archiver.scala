package com.ligadata.InputAdapters

import java.io.OutputStream

import com.ligadata.AdaptersConfiguration.{LocationInfo, SmartFileAdapterConfiguration}
import com.ligadata.Exceptions.FatalAdapterException
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.logging.log4j.LogManager


class Archiver {

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  private val currentAppendFiles =  scala.collection.mutable.LinkedHashMap[String, scala.collection.mutable.LinkedHashMap[String, (Long, Long)]]()
  //for now only keep one file
  def getCurrentAppendFile(parentDir : String) : Option[(String, Long, Long)] = {//file name, size, last mod timestamp
    this.synchronized {
      if (currentAppendFiles.contains(parentDir) && currentAppendFiles(parentDir).nonEmpty)
        Some(currentAppendFiles(parentDir).head._1, currentAppendFiles(parentDir).head._2._1, currentAppendFiles(parentDir).head._2._2)
      else None
    }
  }
  def removeFromCurrentAppendFiles(parentDir : String, file : String) = {
    this.synchronized {
      if (currentAppendFiles.contains(parentDir))
        currentAppendFiles.remove(file)
    }
  }

  def addToCurrentAppendFiles(parentDir : String, file : String, size : Long, timestamp : Long) = {
    this.synchronized {
      if (currentAppendFiles.contains(parentDir))
        currentAppendFiles(parentDir).put(file, (size, timestamp))
      else {
        val map = scala.collection.mutable.LinkedHashMap[String, (Long, Long)](file ->(size, timestamp))
        currentAppendFiles.put(parentDir, map)
      }
    }
  }
  def updateCurrentAppendFile(parentDir : String, file : String, newSize : Long, newTimestamp : Long) = {
    this.synchronized {
      if (currentAppendFiles.contains(parentDir))
        currentAppendFiles(parentDir).put(file, (newSize, newTimestamp))
    }
  }

  private def trimFileFromLocalFileSystem(fileName: String): String = {
    if (fileName.startsWith("file://"))
      return fileName.substring("file://".length() - 1)
    fileName
  }

  val destFileFormat = "file_%s"
  def getNewArchiveFileName = destFileFormat.format(new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()))



  def archiveFile(adapterConfig: SmartFileAdapterConfiguration, locationInfo: LocationInfo, srcFileDir: String, srcFileBaseName: String, componentsMap: scala.collection.immutable.Map[String, String]): Boolean = {
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

    val status = consolidateFile(adapterConfig, locationInfo, srcFileHandler, dstDirToArchive)

    status
  }


  def consolidateFile(adapterConfig: SmartFileAdapterConfiguration, locationInfo: LocationInfo,
                      fileHandler : SmartFileHandler, dstDirToArchive : String) : Boolean = {

    //var status = false
    var result : (Boolean, Long) = (false, 0)

    val currentFileToArchiveSize = fileHandler.length()
    val currentFileToArchiveTimestamp = fileHandler.lastModified()

    logger.info("currentFileToArchiveSize="+currentFileToArchiveSize.toString)

    val consolidateThresholdBytes : Long = (adapterConfig.archiveConfig.consolidationMaxSizeGB * 1024 * 1024 * 1024).toLong

    val currentAppendFileInfo = getCurrentAppendFile(dstDirToArchive)

    logger.debug("adapterConfig.archiveConfig.consolidationMaxSizeGB={}",adapterConfig.archiveConfig.consolidationMaxSizeGB.toString)
    logger.info("consolidateThresholdBytes={}",consolidateThresholdBytes.toString)

    if(currentAppendFileInfo.isEmpty)
      logger.debug("Archiver: no entry for currentAppendFiles")
    else{
      logger.debug("Archiver: currentAppendFile ({}, {}, {})",
        currentAppendFileInfo.get._1, currentAppendFileInfo.get._2.toString, currentAppendFileInfo.get._3.toString)
    }

    if(currentFileToArchiveSize < consolidateThresholdBytes){
      if(currentAppendFileInfo.isEmpty){//no files already in dest
        logger.debug("Archiver: path 1")
        //copy from src to new file on dest
        val destFilePath = dstDirToArchive + "/" + getNewArchiveFileName
        result = copyFileToArchiveAndDelete(adapterConfig, locationInfo, fileHandler, destFilePath)
        addToCurrentAppendFiles(dstDirToArchive, destFilePath, result._2, currentFileToArchiveTimestamp)//using timestamp of src?
      }
      else{
        val currentAppendFileSize = currentAppendFileInfo.get._2
        val currentAppendFilePath = currentAppendFileInfo.get._1

        logger.debug("currentAppendFileSize="+currentAppendFileSize.toString)
        if(currentAppendFileSize + currentFileToArchiveSize >  consolidateThresholdBytes){
          logger.debug("Archiver: path 2")

          //copy from src to new file on dest
          val destFilePath = dstDirToArchive + "/" + getNewArchiveFileName
          result = copyFileToArchiveAndDelete(adapterConfig, locationInfo, fileHandler, destFilePath)
          //change currentAppendFilePath and size to new dest
          removeFromCurrentAppendFiles(dstDirToArchive, currentAppendFilePath)
          addToCurrentAppendFiles(dstDirToArchive, destFilePath, result._2, currentFileToArchiveTimestamp)//using timestamp of src?
        }
        else{
          logger.debug("Archiver: path 3")
          //append src to currentAppendFilePath
          result = copyFileToArchiveAndDelete(adapterConfig, locationInfo, fileHandler, currentAppendFilePath)

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
      result = copyFileToArchiveAndDelete(adapterConfig, locationInfo, fileHandler, destFilePath)

      //for now, assuming order matters, keep only one appendable dest file, remove current append because it got old
      if(currentAppendFileInfo.isDefined)
        removeFromCurrentAppendFiles(dstDirToArchive, currentAppendFileInfo.get._1)
    }

    result._1
  }

  //return status and new size of dest file
  def copyFileToArchiveAndDelete(adapterConfig: SmartFileAdapterConfiguration, locationInfo: LocationInfo,
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
        validateArchiveDestCompression(adapterConfig)
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
      srcFileHandler.deleteFile(srcFileHandler.getFullPath) // Deleting file after archive
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
    }

    (status, resultSize)
  }

  def validateArchiveDestCompression(adapterConfig : SmartFileAdapterConfiguration) : Boolean = {
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
