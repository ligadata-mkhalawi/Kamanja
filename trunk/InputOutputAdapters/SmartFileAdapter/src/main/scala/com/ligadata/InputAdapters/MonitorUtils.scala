package com.ligadata.InputAdapters

import java.io.{InputStream, IOException, File, FileInputStream}
import java.nio.file.{Paths, Files}

import com.ligadata.AdaptersConfiguration.{SmartFileAdapterConfiguration, FileAdapterMonitoringConfig, FileAdapterConnectionConfig}
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
