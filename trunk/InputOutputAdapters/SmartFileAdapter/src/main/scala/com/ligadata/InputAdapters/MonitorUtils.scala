package com.ligadata.InputAdapters

import java.io.{InputStream, IOException, File, FileInputStream}
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
    else parentDir1.compareTo(parentDir2)

  }

  /**
    * compares two files from same location
    * @param fileHandler1
    * @param fileHandler2
    * @param locationInfo
    * @return
    */
  def compareFiles(fileHandler1: SmartFileHandler, fileHandler2: SmartFileHandler, locationInfo : LocationInfo) : Int = {

    val fileComponentsMap1 = getFileComponents(fileHandler1, locationInfo)
    val fileComponentsMap2 = getFileComponents(fileHandler2, locationInfo)

    //var compareResult = 0
    breakable{
      for(orderComponent <- locationInfo.orderBy){
        val fileCompVal1 = if(fileComponentsMap1.contains(orderComponent))
           fileComponentsMap1(orderComponent) else ""
        val fileCompVal2 = if(fileComponentsMap2.contains(orderComponent))
          fileComponentsMap2(orderComponent) else ""

        //println(s"fileCompVal1 $fileCompVal1 , fileCompVal2 $fileCompVal2")

        val tempCompreRes = fileCompVal1.compareTo(fileCompVal2)
        if(tempCompreRes != 0)
          return tempCompreRes
      }
    }

    0
  }

  /**
    * based on file name and ordering config, returns a value representing new file name
    * @param fileHandler
    * @param locationInfo
    * @return
    */
  def getFileComponents(fileHandler: SmartFileHandler, locationInfo : LocationInfo) : Map[String, String] = {

    //TODO BUGBUG : consider predefined components - that start with $

    val filePath = fileHandler.getFullPath
    val fileName = getFileName(filePath)
    val pattern = locationInfo.fileComponents.regex.r

    //println("orderingInfo.fileComponents.regex="+orderingInfo.fileComponents.regex)
    val matchList = pattern.findAllIn(fileName).matchData.toList
    //println("matchList.length="+matchList.length)
    if(matchList.isEmpty)
      throw new Exception(s"File name (${filePath}) does not follow configured pattern ($pattern)")

    val firstMatch = matchList.head
    if(firstMatch.groupCount < locationInfo.fileComponents.components.length)
      throw new Exception(s"File name (${filePath}) does not contain all configured components. " +
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
    println(s"finalFieldVal for file $fileName : " + finalFieldVal)

    componentsMap
  }
}


object SmartFileHandlerFactory{
  def createSmartFileHandler(adapterConfig : SmartFileAdapterConfiguration, fileFullPath : String): SmartFileHandler ={
    val connectionConf = adapterConfig.connectionConfig
    val monitoringConf =adapterConfig.monitoringConfig

    val handler : SmartFileHandler =
      adapterConfig._type.toLowerCase() match {
        case "das/nas" => new PosixFileHandler(fileFullPath)
        case "sftp" => new SftpFileHandler(fileFullPath, connectionConf)
        case "hdfs" => new HdfsFileHandler(fileFullPath, connectionConf)
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
