package com.ligadata.InputAdapters

import scala.actors.threadpool.{TimeUnit => STimeUnit}
import java.io._
import java.nio.file.{Paths, Files}

import com.ligadata.AdaptersConfiguration.{LocationInfo, SmartFileAdapterConfiguration, FileAdapterMonitoringConfig, FileAdapterConnectionConfig}
import com.ligadata.InputAdapters.hdfs._
import com.ligadata.InputAdapters.sftp._
import com.ligadata.Exceptions.{FatalAdapterException, KamanjaException}
import net.sf.jmimemagic._
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.logging.log4j.LogManager
import org.apache.tika.Tika
import org.apache.tika.detect.DefaultDetector
import scala.actors.threadpool.ExecutorService
import scala.actors.threadpool.TimeUnit
import FileType._
import org.apache.commons.lang.StringUtils
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._

/**
  * Created by Yasser on 3/15/2016.
  */
object MonitorUtils {
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)


  var adaptersSessions = scala.collection.mutable.Map[String, scala.collection.mutable.Map[Int, String]]()
  var adaptersChannels = scala.collection.mutable.Map[String, scala.collection.mutable.Map[Int, String]]()

  //Default allowed content types -
  val validContentTypes  = Set(PLAIN, GZIP, BZIP2, LZO) //might change to get that from some configuration

  def isValidFile(adapterName : String, genericFileHandler: SmartFileHandler, filePath : String,
                  locationInfo : LocationInfo, ignoredFilesMap : scala.collection.mutable.Map[String, Long],
                  checkExistence : Boolean, checkFileTypes: Boolean): Boolean = {
    try {
      val filePathParts = filePath.split("/")
      val fileName = filePathParts(filePathParts.length - 1)
      if (fileName.startsWith(".")) {
        logger.debug("Adapter {} - file {} starts with (.)", adapterName, filePath)
        return false
      }

      if(locationInfo == null) {
        logger.debug("Adapter {} - file {} has no location config", adapterName, filePath)
        return false
      }

      if(locationInfo.fileComponents != null && locationInfo.fileComponents.regex != null &&
        locationInfo.fileComponents.regex.trim.length > 0) {
        val pattern = locationInfo.fileComponents.regex.r
        val matchList = pattern.findAllIn(fileName).matchData.toList
        if (matchList.isEmpty) {
          if (!ignoredFilesMap.contains(filePath)) {
            logger.warn("Adapter {} - File name ({}) does not follow configured pattern ({})",
              adapterName, filePath, pattern)
            ignoredFilesMap.put(filePath, System.currentTimeMillis())
          }
          return false
        }
      }

      //val fileSize = fileHandler.length
      //Check if the File exists
      if (!checkExistence || genericFileHandler.exists(filePath)) {

        if (!checkFileTypes) return true
        else {
          val contentType = CompressionUtil.getFileType(genericFileHandler, filePath, "")
          if((validContentTypes contains contentType)) return true
          else {
            if (!ignoredFilesMap.contains(filePath)) {
              //Log error for invalid content type
              logger.warn("Adapter " + adapterName + " -  Invalid content type " + contentType + " for file " + filePath)
              ignoredFilesMap.put(filePath, System.currentTimeMillis())
            }
          }
        }
      }
      else {
        //File does not exists - could be already processed
        logger.warn("Adapter " + adapterName + " - File does not exist anymore " + filePath)
      }
      return false
    }
    catch{
      case e : Throwable =>
        logger.debug("SMART FILE CONSUMER (MonitorUtils): Error while checking validity of file "+filePath, e)
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
    try {
      if(!pool.isShutdown){
        pool.shutdown() // Disable new tasks from being submitted
        pool.awaitTermination(1, STimeUnit.DAYS) //giving a very short time may cuz the thread to interrupt
      }
    } catch {
      case ie: InterruptedException => {
        logger.error("InterruptedException for " + id, ie)
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
    * @return
    */
  def compareFiles(fileHandler1: EnqueuedFileHandler,
                   fileHandler2: EnqueuedFileHandler) : Int = {
    //TODO : for now if different folders, compare based on parent folders
    val parentDir1 = simpleDirPath(fileHandler1.fileHandler.getParentDir)
    val parentDir2 = simpleDirPath(fileHandler2.fileHandler.getParentDir)
    if(parentDir1.compareTo(parentDir2) == 0)
      compareFilesSameLoc(fileHandler1, fileHandler2)
    else fileHandler1.lastModifiedDate.compareTo(fileHandler2.lastModifiedDate)

  }

  /**
    * compares two files from same location
    * @param fileHandler1
    * @param fileHandler2
    * @return
    */
  def compareFilesSameLoc(fileHandler1: EnqueuedFileHandler, fileHandler2: EnqueuedFileHandler) : Int = {

    val fileComponentsMap1 = fileHandler1.componentsMap//getFileComponents(fileHandler1.fileHandler.getFullPath, locationInfo)
    val fileComponentsMap2 = fileHandler2.componentsMap//getFileComponents(fileHandler2.fileHandler.getFullPath, locationInfo)

    val locationInfo = fileHandler1.locationInfo

    if (locationInfo != null) {
      breakable{
        //loop order components, until values for one of them are not equal
        for(orderComponent <- locationInfo.orderBy){

          //predefined components
          if(orderComponent.startsWith("$")){
            orderComponent match{
              case "$File_Name" =>
                val tempCompreRes = getFileName(fileHandler1.fileHandler.getFullPath).compareTo(getFileName(fileHandler2.fileHandler.getFullPath))
                if (tempCompreRes != 0)  return tempCompreRes
              case "$File_Full_Path" =>
                val tempCompreRes =  fileHandler1.fileHandler.getFullPath.compareTo(fileHandler2.fileHandler.getFullPath)
                if (tempCompreRes != 0)  return tempCompreRes
              case "$FILE_MOD_TIME" =>
                val tempCompreRes = fileHandler1.lastModifiedDate.compareTo(fileHandler2.lastModifiedDate)
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
    if(locationInfo == null || locationInfo.fileComponents == null)
      return Map[String, String]()

    val fileName = getFileName(fileFullPath)
    val pattern = locationInfo.fileComponents.regex.r

    //println("orderingInfo.fileComponents.regex="+orderingInfo.fileComponents.regex)
    val matchList = pattern.findAllIn(fileName).matchData.toList
    //println("matchList.length="+matchList.length)
    if(matchList.isEmpty)//file name does not conform with regex, should not get here
      return Map[String, String]()

    val firstMatch = matchList.head
    if(firstMatch.groupCount < locationInfo.fileComponents.components.length)
      throw new Exception(s"File name (${fileFullPath}) does not contain all configured components. " +
      s"components count=${locationInfo.fileComponents.components.length} while found groups = ${firstMatch.groupCount}")

    //check if padding is needed for any component. then put components into map (component name -> value)
    var componentsMap = Map[String, String]()
    for(i <- 0 to firstMatch.groupCount - 1){
      val componentName = locationInfo.fileComponents.components(i)
      //group(0) contains whole exp
      val componentValNoPad =
        try {
          firstMatch.group(i + 1)
        }
        catch{
          case ex : Throwable => ""
        }

      val componentVal =
        if(componentValNoPad == null) ""
        else if(locationInfo.fileComponents.paddings != null && locationInfo.fileComponents.paddings.contains(componentName)){
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

  def getCallStack() : String = {
    if(showStackTraceWithLogs) {
      val callstack = Thread.currentThread().getStackTrace().drop(2).take(5).
        map(s => s.getClassName + "." + s.getMethodName + "(" + s.getLineNumber + ")").mkString("\n")
      " Callstack is: " + callstack
    }
    else ""
  }
  val showStackTraceWithLogs = true

  //(array of files, array of dirs)
  def separateFilesFromDirs (allFiles : Array[MonitoredFile]) : (Array[MonitoredFile], Array[MonitoredFile]) = {
    val files = ArrayBuffer[MonitoredFile]()
    val dirs = ArrayBuffer[MonitoredFile]()
    allFiles.foreach(f => if(f.isDirectory) dirs.append(f) else files.append(f) )
    (files.toArray, dirs.toArray)
  }
}

object SmartFileHandlerFactory{
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  /*
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
        srcFileStruct.take( srcFileStruct.length-1).mkString("/").replace(locationInfo.targetDir, dstDirToArchiveBase)
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

    val dstFileToArchive =  dstDirToArchive + "/" + srcFileBaseName

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


      fileHandler = SmartFileHandlerFactory.createSmartFileHandler(adapterConfig, srcFileToArchive, false)
      fileHandler.openForRead()

      //TODO: compare input compression to output compression, would this give better performance?
      //might need to do this: if same compression, can simply read and write as binary
      //else must open src to read using proper compression, and open dest for write with proper compression

      val originalOutputStream = osWriter.openFile(adapterConfig.archiveConfig.outputConfig, dstFileToArchive, false)
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

*/


  def createSmartFileHandler(adapterConfig : SmartFileAdapterConfiguration, fileFullPath : String, isBinary: Boolean = false): SmartFileHandler ={
    val connectionConf = adapterConfig.connectionConfig
    val monitoringConf =adapterConfig.monitoringConfig

    val handler : SmartFileHandler =
      adapterConfig._type.toLowerCase() match {
        case "das/nas" => new PosixFileHandler(fileFullPath, monitoringConf, isBinary)
        case "sftp" => new SftpFileHandler(adapterConfig.Name, fileFullPath, connectionConf, monitoringConf, isBinary)
        case "hdfs" => new HdfsFileHandler(fileFullPath, connectionConf, monitoringConf, isBinary)
        case _ => throw new KamanjaException("Unsupported Smart file adapter type", null)
      }

    handler
  }
}
