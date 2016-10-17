package com.ligadata.InputAdapters

import java.io.OutputStream

import com.ligadata.AdaptersConfiguration.{LocationInfo, SmartFileAdapterConfiguration}
import com.ligadata.Exceptions.FatalAdapterException
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.logging.log4j.LogManager


class Archiver {

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  private val currentAppendFiles =  scala.collection.mutable.LinkedHashMap[String, (Long, Long)]()
  //for now only keep one file
  def getCurrentAppendFile : Option[(String, Long, Long)] = {//file name, size, last mod timestamp
    if(currentAppendFiles.nonEmpty)
      Some(currentAppendFiles.head._1, currentAppendFiles.head._2._1, currentAppendFiles.head._2._2)
    else None
  }
  def removeFromCurrentAppendFiles(file : String) = currentAppendFiles.remove(file)
  def addToCurrentAppendFiles(file : String, size : Long, timestamp : Long) =
    currentAppendFiles.put(file, (size, timestamp))
  def updateCurrentAppendFile(file : String, newSize : Long, newTimestamp : Long) = {
    currentAppendFiles.put(file, (newSize, newTimestamp))
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
    val dstDirToArchiveBase = partitionFormatString.format(values: _*)
    val srcFileStruct = srcFileToArchive.split("/")
    val dstDirToArchive =
      if (locationInfo != null && adapterConfig.monitoringConfig.createInputStructureInTargetDirs) {
        srcFileStruct.take(srcFileStruct.length - 1).mkString("/").replace(locationInfo.targetDir, dstDirToArchiveBase)
      }
      else dstDirToArchiveBase


    val destArchiveDirHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, dstDirToArchive)
    //might need to build sub-dirs corresponding to input dir structure
    val destArchiveDirExists =
      if (!destArchiveDirHandler.exists())
        destArchiveDirHandler.mkdirs()
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

    var status = false

    val currentFileToArchiveSize = fileHandler.length()
    val currentFileToArchiveTimestamp = fileHandler.lastModified()

    val consolidateThresholdBytes : Long = (adapterConfig.archiveConfig.consolidationMaxSizeGB * 1024 * 1024 * 1024).toLong

    val currentAppendFileInfo = getCurrentAppendFile

    if(currentAppendFileInfo.isEmpty)
      logger.warn("Archiver: no entry for currentAppendFiles")
    else{
      logger.warn("Archiver: currentAppendFile ({}, {}, {})",
        currentAppendFileInfo.get._1, currentAppendFileInfo.get._2.toString, currentAppendFileInfo.get._3.toString)
    }

    if(currentFileToArchiveSize < consolidateThresholdBytes){
      if(currentAppendFileInfo.isEmpty){//no files already in dest
        //copy from src to new file on dest
        val destFilePath = dstDirToArchive + getNewArchiveFileName
        status = copyFileToArchiveAndDelete(adapterConfig, locationInfo, fileHandler, destFilePath)
        addToCurrentAppendFiles(destFilePath, fileHandler.fileLength(destFilePath), currentFileToArchiveTimestamp)//using timestamp of src?
      }
      else{
        val currentAppendFileSize = currentAppendFileInfo.get._2
        val currentAppendFilePath = currentAppendFileInfo.get._1

        if(currentAppendFileSize + currentFileToArchiveSize >  consolidateThresholdBytes){
          //copy from src to new file on dest
          val destFilePath = dstDirToArchive + getNewArchiveFileName
          status = copyFileToArchiveAndDelete(adapterConfig, locationInfo, fileHandler, destFilePath)
          //change currentAppendFilePath and size to new dest
          removeFromCurrentAppendFiles(currentAppendFilePath)
          addToCurrentAppendFiles(destFilePath, fileHandler.fileLength(destFilePath), currentFileToArchiveTimestamp)//using timestamp of src?
        }
        else{
          //append src to currentAppendFilePath
          status = copyFileToArchiveAndDelete(adapterConfig, locationInfo, fileHandler, currentAppendFilePath)

          //increase size currentAppendFileSize
          updateCurrentAppendFile(currentAppendFilePath, fileHandler.fileLength(currentAppendFilePath), currentFileToArchiveTimestamp)
        }
      }
    }
    else{
      //TODO : might need to split files larger than thresold

      //copy from src to new file on dest
      val destFilePath = dstDirToArchive + "/" + getNewArchiveFileName
      status = copyFileToArchiveAndDelete(adapterConfig, locationInfo, fileHandler, destFilePath)

      //for now, assuming order matters, remove current append because it got old
      if(currentAppendFileInfo.isDefined)
        removeFromCurrentAppendFiles(currentAppendFileInfo.get._1)
    }

    status
  }

  def copyFileToArchiveAndDelete(adapterConfig: SmartFileAdapterConfiguration, locationInfo: LocationInfo,
                                 srcFileHandler: SmartFileHandler, dstFileToArchive : String): Boolean ={

    logger.warn("Archive: Copying from {} to {}", srcFileHandler.getFullPath, dstFileToArchive)

    val osWriter = new com.ligadata.OutputAdapters.OutputStreamWriter()
    var os: OutputStream = null

    var status = false

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
        } catch {
          case e: Throwable => {
            logger.error("Failed to close OutputStream for " + dstFileToArchive, e)
          }
        }
      }
    }
    status
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
