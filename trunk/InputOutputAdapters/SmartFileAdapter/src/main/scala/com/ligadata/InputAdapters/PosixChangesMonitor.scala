package com.ligadata.InputAdapters


import java.io._
import java.nio.file.Path
import java.nio.file._
import java.util.zip.GZIPInputStream
import com.ligadata.Exceptions.{KamanjaException}

import org.apache.logging.log4j.{ Logger, LogManager }

import scala.actors.threadpool.{Executors, ExecutorService}
import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.util.control.Breaks._
import com.ligadata.AdaptersConfiguration._
import CompressionUtil._

/**
  * Created by Yasser on 1/14/2016.
  *
  * POSIX (DAS/NAS) file systems directory monitor and necessary classes
  * based on Dan's code
  */


class PosixFileHandler extends SmartFileHandler{

  private var fileFullPath = ""
  def getFullPath = fileFullPath

  def fileObject = new File(fileFullPath)
  private var bufferedReader: BufferedReader = null
  //private var in: InputStreamReader = null
  private var in: InputStream = null

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  def this(fullPath : String){
    this()

    fileFullPath = fullPath
  }

  def getParentDir : String = fileObject.getParent.trim

  //gets the input stream according to file system type - POSIX here
  def getDefaultInputStream() : InputStream = {
    val inputStream : InputStream =
      try {
        new FileInputStream(fileFullPath)
      }
      catch {
        case e: Exception =>
          logger.error("", e)
          null
        case e: Throwable =>
          logger.error("", e)
          null
      }
    inputStream
  }

  @throws(classOf[KamanjaException])
  def openForRead(): InputStream = {
    try {
      val fileType = CompressionUtil.getFileType(this, null)
      in = CompressionUtil.getProperInputStream(getDefaultInputStream, fileType)
      //bufferedReader = new BufferedReader(in)
      in
    }
    catch{
      case e : Exception => throw new KamanjaException (e.getMessage, e)
      case e : Throwable => throw new KamanjaException (e.getMessage, e)
    }
  }

  @throws(classOf[KamanjaException])
  def read(buf : Array[Byte], length : Int) : Int = {
    read(buf, 0, length)
  }

  @throws(classOf[KamanjaException])
  def read(buf : Array[Byte], offset : Int, length : Int) : Int = {

    try {
      if (in == null)
        return -1

      in.read(buf, offset, length)
    }
    catch{
      case e : Exception => throw new KamanjaException (e.getMessage, e)
      case e : Throwable => throw new KamanjaException (e.getMessage, e)
    }
  }

  @throws(classOf[KamanjaException])
  def moveTo(newFilePath : String) : Boolean = {
    if(getFullPath.equals(newFilePath)){
      logger.warn(s"Trying to move file ($getFullPath) but source and destination are the same")
      return false
    }
    try {
      logger.debug(s"PosixFileHandler - Moving file ${fileObject.toString} to ${newFilePath}")
      val destFileObj = new File(newFilePath)

      if (fileObject.exists()) {
        fileObject.renameTo(destFileObj)
        logger.debug("Moved file success")
        fileFullPath = newFilePath
        return true
      }
      else{
        logger.warn("Source file was not found")
        return false
      }
    }
    catch {
      case ex : Exception => {
        logger.error("", ex)
        return false
      }
      case ex : Throwable => {
        logger.error("", ex)
        return false
      }
    }
  }

  @throws(classOf[KamanjaException])
  def delete() : Boolean = {
    logger.debug(s"Deleting file ($getFullPath)")
    try {
      fileObject.delete
      logger.debug("Successfully deleted")
      return true
    }
    catch {
      case ex : Exception => {
        logger.error("", ex)
        return false
      }
      case ex : Throwable => {
        logger.error("", ex)
        return false
      }

    }
  }

  @throws(classOf[KamanjaException])
  def length : Long = fileObject.length

  def lastModified : Long = fileObject.lastModified

  @throws(classOf[KamanjaException])
  def close(): Unit = {
    try {
      if (in != null)
        in.close()
    }
    catch{
      case e : Exception => throw new KamanjaException (e.getMessage, e)
      case e : Throwable => throw new KamanjaException (e.getMessage, e)
    }
  }

  override def exists(): Boolean = new File(fileFullPath).exists

  override def isFile: Boolean = new File(fileFullPath).isFile

  override def isDirectory: Boolean = new File(fileFullPath).isDirectory

  override def isAccessible : Boolean = {
    val file = new File(fileFullPath)
    file.exists() && file.canRead && file.canWrite
  }
}


/**
  *  adapterName might be used for error messages
  * @param adapterName
  * @param modifiedFileCallback
  */
class PosixChangesMonitor(adapterName : String, modifiedFileCallback:(SmartFileHandler, Boolean) => Unit) extends SmartFileMonitor {

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)


  private var watchService: WatchService = null
  private var keys = new HashMap[WatchKey, Path]

  private var errorWaitTime = 1000
  val MAX_WAIT_TIME = 60000

  private var fileCache: scala.collection.mutable.Map[String, Long] = scala.collection.mutable.Map[String, Long]()
  private var fileCacheLock = new Object
  private var connectionConf : FileAdapterConnectionConfig = null
  private var monitoringConf :  FileAdapterMonitoringConfig = null

  private var monitorsExecutorService: ExecutorService = null

  private var isMonitoring = false
  private var checkFolders = true

  def init(adapterSpecificCfgJson: String): Unit ={
    logger.debug("PosixChangesMonitor (init)- adapterSpecificCfgJson==null is "+
      (adapterSpecificCfgJson == null))

    val(_type, c, m) =  SmartFileAdapterConfiguration.parseSmartFileAdapterSpecificConfig(adapterName, adapterSpecificCfgJson)
    connectionConf = c
    monitoringConf = m
  }

  def markFileAsProcessed(filePath : String) : Unit = {
    fileCacheLock.synchronized {
      logger.info("Smart File Consumer (Posix Monitor) - removing file {} from map {} as it is processed", filePath, fileCache)
      fileCache -= filePath
    }
  }

  def setMonitoringStatus(status : Boolean): Unit ={
    checkFolders = status
  }

  def monitor: Unit ={

    logger.info("Posix Changes Monitor - start monitoring")
    //TODO : consider running each folder monitoring in a separate thread
    isMonitoring = true

    monitorsExecutorService = Executors.newFixedThreadPool(monitoringConf.locations.length)

    monitoringConf.locations.foreach(folderToWatch => {
      val dirMonitorthread = new Runnable() {
        private var targetFolder: String = _
        def init(dir: String) = targetFolder = dir

        override def run() = {
          try{
            breakable {
              var isFirstScan = true
              while (isMonitoring) {

                if(checkFolders) {
                  try {
                    logger.info(s"Watching directory $targetFolder")

                    val dirsToCheck = new ArrayBuffer[String]()
                    dirsToCheck += targetFolder

                    while (dirsToCheck.nonEmpty) {
                      val dirToCheck = dirsToCheck.head
                      dirsToCheck.remove(0)

                      val dir = new File(dirToCheck)
                      checkExistingFiles(dir, isFirstScan)
                      //dir.listFiles.filter(_.isDirectory).foreach(d => dirsToCheck += d.toString)


                      errorWaitTime = 1000
                    }

                    isFirstScan = false

                  } catch {
                    case e: Exception => {
                      logger.warn("Unable to access Directory, Retrying after " + errorWaitTime + " seconds", e)
                      errorWaitTime = scala.math.min((errorWaitTime * 2), MAX_WAIT_TIME)
                    }
                    case e: Throwable => {
                      logger.warn("Unable to access Directory, Retrying after " + errorWaitTime + " seconds", e)
                      errorWaitTime = scala.math.min((errorWaitTime * 2), MAX_WAIT_TIME)
                    }
                  }
                  Thread.sleep(monitoringConf.waitingTimeMS)
                }
              }
            }
          }  catch {
            case ie: InterruptedException => logger.error("InterruptedException: ", ie)
            case ioe: IOException         => logger.error("Unable to find the directory to watch, Shutting down File Consumer", ioe)
            case e: Exception             => logger.error("Exception: ", e)
            case e: Throwable             => logger.error("Throwable: ", e)
          }
        }
      }

      dirMonitorthread.init(folderToWatch)
      monitorsExecutorService.execute(dirMonitorthread)

    })

  }

  def shutdown: Unit ={
    logger.debug("Shutting down PosixChangesMonitor")
    isMonitoring = false

    monitorsExecutorService.shutdown()
  }


  private def checkExistingFiles(parentDir: File, isFirstScan : Boolean): Unit = {
    // Process all the existing files in the directory that are not marked complete.
    if (parentDir.exists && parentDir.isDirectory) {
      val files = parentDir.listFiles.filter(_.isFile).sortWith(_.lastModified < _.lastModified).toList
      files.foreach(file => {
        val tokenName = file.toString.split("/")
        if (!checkIfFileHandled(file.toString)) {
          //logger.info("SMART FILE CONSUMER (global)  Processing " + file.toString)
          //FileProcessor.enQBufferedFile(file.toString)
          val fileHandler = new PosixFileHandler(file.toString)
          //call the callback for new files
          logger.info(s"Posix Changes Monitor - A new file found ${fileHandler.getFullPath}. initial = $isFirstScan")
          try {
            modifiedFileCallback(fileHandler, isFirstScan)
          }
          catch{
            case e : Throwable =>
              logger.error("Smart File Consumer (Sftp) : Error while notifying Monitor about new file", e)
          }
        }
      })

      //remove files that are no longer in the folder
      val deletedFiles = new ArrayBuffer[String]()
      fileCacheLock.synchronized {
        fileCache.foreach(fileCacheEntry => {
          //logger.debug("file in map is {}, its parent is {}, the folder being checked is {}",
            //fileCacheEntry._1,  new File(fileCacheEntry._1).getParent, parentDir.toString.trim)

          if (new File(fileCacheEntry._1).getParent.trim.equals(parentDir.toString.trim)) {
            //logger.debug("file in map is direct child of checked folder")
            //logger.debug("checked folder's children are {}", files)
            if (!files.exists(file => file.toString.equals(fileCacheEntry._1))) {
              //key that is no more in the folder => file/folder deleted
              //logger.debug("file in map is not currenlty in children list of the folder, adding it to deleted files list")
              deletedFiles += fileCacheEntry._1
            }
          }
        })

        //logger.debug("deleted files list is {}", deletedFiles)
        deletedFiles.foreach(f => {
          logger.debug("checkExistingFiles - removing file {} from map {}", f, fileCache)
          fileCache -= f
        })//remove from file cache
      }


    }
    else{
      logger.warn(parentDir.toString + " is not a directory or does not exist")
    }
  }

  /**
    * checkIfFileHandled: previously checkIfFileBeingProcessed - if for some reason a file name is queued twice... this will prevent it
    * @param file
    * @return
    */
  def checkIfFileHandled(file: String): Boolean = {
    fileCacheLock.synchronized {
      if (fileCache.contains(file)) {
        return true
      }
      else {
        fileCache.put(file, scala.compat.Platform.currentTime)
        return false
      }
    }
  }

}


