package com.ligadata.InputAdapters

import java.io.{InputStream, IOException, File, FileInputStream}
import java.nio.file.{Paths, Files}

import com.ligadata.AdaptersConfiguration.{ SmartFileAdapterConfiguration, FileAdapterMonitoringConfig, FileAdapterConnectionConfig}
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

  /*def compareFiles(fileHandler1: SmartFileHandler, fileHandler2: SmartFileHandler, orderingInfo : FileOrderingInfo) : Int = {
    if(orderingInfo == null || orderingInfo.orderBy == null)
      throw new Exception("Invalid ordering config")

    orderingInfo.orderBy.toLowerCase() match{
      case "date" => fileHandler1.lastModified().compareTo(fileHandler2.lastModified())
      case "name" => getFileName(fileHandler1.getFullPath).compareTo(getFileName(fileHandler2.getFullPath))
      case "components" =>
        val fileComparisonValue1 = getFileAltNameFromComponents(fileHandler1.getFullPath, orderingInfo)
        val fileComparisonValue2 = getFileAltNameFromComponents(fileHandler2.getFullPath, orderingInfo)
        fileComparisonValue1.compareTo(fileComparisonValue2)

      case _ => throw new Exception(s"Unrecognized ordering type (${orderingInfo.orderBy})")
    }

  }

  /**
    * based on file name and ordering config, returns a value representing new file name
    * @param filePath
    * @param orderingInfo
    * @return
    */
  def getFileAltNameFromComponents(filePath: String, orderingInfo : FileOrderingInfo) : String = {

    val fileName = getFileName(filePath)
    val pattern = orderingInfo.fileComponents.regex.r

    //println("orderingInfo.fileComponents.regex="+orderingInfo.fileComponents.regex)
    val matchList = pattern.findAllIn(fileName).matchData.toList
    //println("matchList.length="+matchList.length)
    if(matchList.isEmpty)
      throw new Exception(s"File name (${filePath}) does not follow configured pattern ($pattern)")

    val firstMatch = matchList.head
    if(firstMatch.groupCount < orderingInfo.fileComponents.components.length)
      throw new Exception(s"File name (${filePath}) does not contain all configured components. " +
      s"components count=${orderingInfo.fileComponents.components.length} while found groups = ${firstMatch.groupCount}")

    val componentsMap = collection.mutable.Map[String, String]()
    for(i <- 0 to firstMatch.groupCount - 1)//group(0) contains whole exp
      componentsMap.put(orderingInfo.fileComponents.components(i), firstMatch.group(i + 1))

    //println("componentsMap="+componentsMap)
   /* var finalFieldVal = orderingInfo.fileComponents.orderFieldValueFromComponents
    componentsMap.foreach(tuple => {
      finalFieldVal = finalFieldVal.replaceAllLiterally(tuple._1, tuple._2)
    })*/

    //get value of field that will be used for file comparison based on the components
    val finalFieldVal =
      componentsMap.foldLeft(orderingInfo.fileComponents.orderFieldValueFromComponents)((value, mapTuple) => {
        value.replaceAllLiterally(mapTuple._1, mapTuple._2)
      })


    finalFieldVal
  }*/
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
