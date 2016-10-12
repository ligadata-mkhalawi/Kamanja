package com.ligadata.InputAdapters

import java.io._
import java.nio.file.{Paths, Files}

import com.ligadata.AdaptersConfiguration.{LocationInfo, SmartFileAdapterConfiguration, FileAdapterMonitoringConfig, FileAdapterConnectionConfig}
import com.ligadata.InputAdapters.hdfs._
import com.ligadata.InputAdapters.sftp._
import com.ligadata.Exceptions.KamanjaException
import net.sf.jmimemagic._
import org.apache.logging.log4j.LogManager
import org.apache.tika.Tika
import org.apache.tika.detect.DefaultDetector
import scala.actors.threadpool.ExecutorService
import scala.actors.threadpool.TimeUnit
import FileType._
import org.apache.commons.lang.StringUtils
import scala.util.control.Breaks._

/**
  * Created by Yasser on 3/15/2016.
  */
object MonitorUtils {
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)


  //Default allowed content types -
  val validContentTypes  = Set(PLAIN, GZIP, BZIP2, LZO) //might change to get that from some configuration

  def isValidFile(fileHandler: SmartFileHandler): Boolean = {
    try {
      val filepathParts = fileHandler.getFullPath.split("/")
      val fileName = filepathParts(filepathParts.length - 1)
      if (fileName.startsWith("."))
        return false

      val fileSize = fileHandler.length
      //Check if the File exists
      if (fileHandler.exists && fileSize > 0) {

        val contentType = CompressionUtil.getFileType(fileHandler, "")
        if (validContentTypes contains contentType) {
          return true
        } else {
          //Log error for invalid content type
          logger.error("SMART FILE CONSUMER (MonitorUtils): Invalid content type " + contentType + " for file " + fileHandler.getFullPath)
        }
      } else if (fileSize == 0) {
        return true
      } else {
        //File doesnot exists - could be already processed
        logger.warn("SMART FILE CONSUMER (MonitorUtils): File does not exist anymore " + fileHandler.getFullPath)
      }
      return false
    }
    catch{
      case e : Throwable =>
        logger.debug("SMART FILE CONSUMER (MonitorUtils): Error while checking validity of file "+fileHandler.getDefaultInputStream, e)
        false
    }
  }

  def toCharArray(bytes : Array[Byte]) : Array[Char] = {
    if(bytes == null)
      return null

    bytes.map(b => b.toChar)
  }

  def simpleDirPath(path : String) : String = {
    if(path.endsWith("/"))
      path.substring(0, path.length - 1)
    else
      path
  }

  def getFileName(path : String) : String = {
    val idx = path.lastIndexOf("/")
    if(idx < 0 )
      return path
    path.substring(idx + 1)
  }

  def getFileParentDir(fileFullPath : String, adapterConfig: SmartFileAdapterConfiguration) : String = {
    val parentDir = simpleDirPath(fileFullPath.substring(0, fileFullPath.lastIndexOf("/")))

    val finalParentDir =
      adapterConfig._type.toLowerCase() match {
        case "das/nas" => parentDir
        case "hdfs" => HdfsUtility.getFilePathNoProtocol(parentDir)
        case "sftp" => SftpUtility.getPathOnly(parentDir)
        case _ => throw new KamanjaException("Unsupported Smart file adapter type", null)
      }

    finalParentDir
  }

  def shutdownAndAwaitTermination(pool : ExecutorService, id : String) : Unit = {
    pool.shutdown(); // Disable new tasks from being submitted
    try {
      // Wait a while for existing tasks to terminate
      if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
        pool.shutdownNow(); // Cancel currently executing tasks
        // Wait a while for tasks to respond to being cancelled
        if (!pool.awaitTermination(2, TimeUnit.SECONDS)) {
          logger.warn("Pool did not terminate " + id);
          Thread.currentThread().interrupt()
        }
      }
    } catch  {
      case ie : InterruptedException => {
        logger.info("InterruptedException for " + id, ie)
        // (Re-)Cancel if current thread also interrupted
        pool.shutdownNow();
        // Preserve interrupt status
        Thread.currentThread().interrupt()
      }
    }
  }

  def isPatternMatch(name : String, regex : String): Boolean ={
    val pattern = regex.r
    val matchList = pattern.findAllIn(name).matchData.toList
    matchList.nonEmpty
  }

  /**
    * compares two files not necessarily from same src folder
    * @param fileHandler1
    * @param fileHandler2
    * @param locationInfo1
    * @param locationInfo2
    * @return
    */
  def compareFiles(fileHandler1: SmartFileHandler, locationInfo1 : LocationInfo, fileHandler2: SmartFileHandler, locationInfo2 : LocationInfo) : Int = {
    //TODO : for now if different folders, compare based on parent folders
    val parentDir1 = simpleDirPath(fileHandler1.getParentDir)
    val parentDir2 = simpleDirPath(fileHandler2.getParentDir)
    if(parentDir1.compareTo(parentDir2) == 0)
      compareFiles(fileHandler1, fileHandler2, locationInfo1)
    else fileHandler1.lastModified().compareTo(fileHandler2.lastModified())

  }

  /**
    * compares two files from same location
    * @param fileHandler1
    * @param fileHandler2
    * @param locationInfo
    * @return
    */
  def compareFiles(fileHandler1: SmartFileHandler, fileHandler2: SmartFileHandler, locationInfo : LocationInfo) : Int = {

    val fileComponentsMap1 = getFileComponents(fileHandler1.getFullPath, locationInfo)
    val fileComponentsMap2 = getFileComponents(fileHandler2.getFullPath, locationInfo)


    breakable{
      //loop order components, until values for one of them are not equal
      for(orderComponent <- locationInfo.orderBy){

        //predefined components
        if(orderComponent.startsWith("$")){
          orderComponent match{
            case "$File_Name" =>
              val tempCompreRes = getFileName(fileHandler1.getFullPath).compareTo(getFileName(fileHandler2.getFullPath))
              if (tempCompreRes != 0)  return tempCompreRes
            case "$File_Full_Path" =>
              val tempCompreRes =  fileHandler1.getFullPath.compareTo(fileHandler2.getFullPath)
              if (tempCompreRes != 0)  return tempCompreRes
            case "$FILE_MOD_TIME" =>
              val tempCompreRes = fileHandler1.lastModified().compareTo(fileHandler2.lastModified())
              if (tempCompreRes != 0)  return tempCompreRes
              //TODO : check for other predefined components
            case "_" => throw new Exception("Unsopported predefined file order component - " + orderComponent)
          }
        }
        else {
          val fileCompVal1 = if (fileComponentsMap1.contains(orderComponent))
            fileComponentsMap1(orderComponent)
          else "" //TODO : check if this can happen
          val fileCompVal2 = if (fileComponentsMap2.contains(orderComponent))
            fileComponentsMap2(orderComponent)
          else ""

          //println(s"fileCompVal1 $fileCompVal1 , fileCompVal2 $fileCompVal2")

          val tempCompreRes = fileCompVal1.compareTo(fileCompVal2)
          if (tempCompreRes != 0)
            return tempCompreRes
        }
      }
    }

    0
  }

  /**
    * based on file name and ordering config, returns a value representing new file name
    * @param fileFullPath
    * @param locationInfo
    * @return
    */
  def getFileComponents(fileFullPath: String, locationInfo : LocationInfo) : Map[String, String] = {
    if(locationInfo.fileComponents == null)
      return Map[String, String]()

    val fileName = getFileName(fileFullPath)
    val pattern = locationInfo.fileComponents.regex.r

    //println("orderingInfo.fileComponents.regex="+orderingInfo.fileComponents.regex)
    val matchList = pattern.findAllIn(fileName).matchData.toList
    //println("matchList.length="+matchList.length)
    if(matchList.isEmpty)
      throw new Exception(s"File name (${fileFullPath}) does not follow configured pattern ($pattern)")

    val firstMatch = matchList.head
    if(firstMatch.groupCount < locationInfo.fileComponents.components.length)
      throw new Exception(s"File name (${fileFullPath}) does not contain all configured components. " +
      s"components count=${locationInfo.fileComponents.components.length} while found groups = ${firstMatch.groupCount}")

    //check if padding is needed for any component. then put components into map (component name -> value)
    var componentsMap = Map[String, String]()
    for(i <- 0 to firstMatch.groupCount - 1){
      val componentName = locationInfo.fileComponents.components(i)
      //group(0) contains whole exp
      val componentValNoPad = firstMatch.group(i + 1)

      val componentVal =
      if(locationInfo.fileComponents.paddings != null && locationInfo.fileComponents.paddings.contains(componentName)){
        val padInfo = locationInfo.fileComponents.paddings(componentName)
        padInfo.padPos.toLowerCase match{
          case "left" => StringUtils.leftPad(componentValNoPad, padInfo.padSize, padInfo.padStr)
          case "right" => StringUtils.rightPad(componentValNoPad, padInfo.padSize, padInfo.padStr)
          case _ => throw new Exception("Unsopported padding position config - " + padInfo.padPos)
        }
      }
      else componentValNoPad

      componentsMap += (componentName -> componentVal)
    }

    //println(s"componentsMap for file $fileName is " + componentsMap)

    //for testing only
    val orderFieldValueTemplate = locationInfo.orderBy.mkString("-")
    val finalFieldVal =
      componentsMap.foldLeft(orderFieldValueTemplate)((value, mapTuple) => {
        value.replaceAllLiterally(mapTuple._1, mapTuple._2)
      })
    logger.debug(s"finalFieldVal for file $fileName : " + finalFieldVal)

    componentsMap
  }

  def addProcessedFileToMap(filePath : String, processedFilesMap : scala.collection.mutable.LinkedHashMap[String, Long]) : Unit = {
    //map is sorted the way items were appended
    val time = java.util.Calendar.getInstance().getTime()
    val timeAsLong = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(time).toLong

    if(processedFilesMap.size >= 1000000) //Todo : make value as config ?
      processedFilesMap.remove(processedFilesMap.head._1)//remove first item to make place

    processedFilesMap.put(filePath, timeAsLong)
  }
}

object SmartFileHandlerFactory{
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  def archiveFile(adapterConfig: SmartFileAdapterConfiguration, locationInfo: LocationInfo, srcFileDir: String, srcFileBaseName: String, componentsMap: scala.collection.immutable.Map[String, String]): Boolean = {
    if (adapterConfig.archiveConfig == null || adapterConfig.archiveConfig.outputConfig == null)
      return true

    var status = false
    val partitionVariable = "\\$\\{([^\\}]+)\\}".r
    val partitionFormats = partitionVariable.findAllMatchIn(adapterConfig.archiveConfig.outputConfig.uri).map(x => x.group(1)).toList
    val partitionFormatString = partitionVariable.replaceAllIn(adapterConfig.archiveConfig.outputConfig.uri, "%s")

    val values = partitionFormats.map(fmt => { componentsMap.getOrElse(fmt, "default").toString.trim })

    val srcFileToArchive = srcFileDir + "/" + srcFileBaseName
    //val dstFileToArchive =  partitionFormatString.format(values: _*) + "/" + srcFileBaseName
    val dstDirToArchiveBase =  partitionFormatString.format(values: _*)
    val srcFileStruct = srcFileToArchive.split("/")
    val dstDirToArchive =
      if(locationInfo != null && adapterConfig.monitoringConfig.createInputStructureInTargetDirs) {
        srcFileStruct.take( srcFileStruct.length-1).mkString("/").replace(locationInfo.srcDir, dstDirToArchiveBase)
      }
      else dstDirToArchiveBase


    val destArchiveDirHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, dstDirToArchive)
    //might need to build sub-dirs corresponding to input dir structure
    val destArchiveDirExists =
      if(!destArchiveDirHandler.exists())
        destArchiveDirHandler.mkdirs()
      else true

    if(!destArchiveDirExists) {
      logger.error("Archiving dest dir {} does not exist and could not be created", dstDirToArchive)
      return false
    }

    val dstFileToArchive =  dstDirToArchiveBase + "/" + srcFileBaseName

    logger.warn("Archiving file from " + srcFileToArchive + " to " + dstFileToArchive)

    var fileHandler: SmartFileHandler = null
    val osWriter = new com.ligadata.OutputAdapters.OutputStreamWriter()
    var os: OutputStream = null

    try {
      if (osWriter.isFileExists(adapterConfig.archiveConfig.outputConfig, dstFileToArchive)) {
        // Delete existing file in Archive folder
        logger.error("Deleting previously archived/partially archived file " + dstFileToArchive)
        osWriter.removeFile(adapterConfig.archiveConfig.outputConfig, dstFileToArchive)
      }

      fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, srcFileToArchive, true)
      fileHandler.openForRead()
      os = osWriter.openFile(adapterConfig.archiveConfig.outputConfig, dstFileToArchive, false)

      var curReadLen = -1
      val bufferSz = 8 * 1024 * 1024
      val buf = new Array[Byte](bufferSz)

      do {
        curReadLen = fileHandler.read(buf, 0, bufferSz)
        if (curReadLen > 0) {
          os.write(buf, 0, curReadLen)
        }
      } while (curReadLen > 0)
      fileHandler.deleteFile(srcFileToArchive) // Deleting file after archive
      status = true
    } catch {
      case e: Throwable => {
        logger.error("Failed to archive file from " + srcFileToArchive + " to " + dstFileToArchive, e)
        status = false
      }
    } finally {
      if (fileHandler != null) {
        try {
          fileHandler.close()
        } catch {
          case e: Throwable => {
            logger.error("Failed to close InputStream for " + srcFileToArchive, e)
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

  def createSmartFileHandler(adapterConfig : SmartFileAdapterConfiguration, fileFullPath : String, isBinary: Boolean = false): SmartFileHandler ={
    val connectionConf = adapterConfig.connectionConfig
    val monitoringConf =adapterConfig.monitoringConfig

    val handler : SmartFileHandler =
      adapterConfig._type.toLowerCase() match {
        case "das/nas" => new PosixFileHandler(fileFullPath, isBinary)
        case "sftp" => new SftpFileHandler(fileFullPath, connectionConf, isBinary)
        case "hdfs" => new HdfsFileHandler(fileFullPath, connectionConf, isBinary)
        case _ => throw new KamanjaException("Unsupported Smart file adapter type", null)
      }

    handler
  }
}

object SmartFileMonitorFactory{
  def createSmartFileMonitor(adapterName : String, adapterType : String, modifiedFileCallback:(SmartFileHandler, Boolean) => Unit) : SmartFileMonitor = {

    val monitor : SmartFileMonitor =
      adapterType.toLowerCase() match {
        case "das/nas" => new PosixChangesMonitor(adapterName, modifiedFileCallback)
        case "sftp" => new SftpChangesMonitor(adapterName, modifiedFileCallback)
        case "hdfs" => new HdfsChangesMonitor(adapterName, modifiedFileCallback)
        case _ => throw new KamanjaException("Unsupported Smart file adapter type", null)
      }

    monitor
  }
}
