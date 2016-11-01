package com.ligadata.InputAdapters


import java.io._
import java.nio.file.Path
import java.nio.file.Path
import java.nio.file._
import java.util
import java.util.zip.GZIPInputStream
import com.ligadata.Exceptions.{KamanjaException}
import com.ligadata.InputOutputAdapterInfo.AdapterConfiguration

import org.apache.logging.log4j.{ Logger, LogManager }

import scala.actors.threadpool.{Executors, ExecutorService}
import scala.collection.mutable.{MultiMap, ArrayBuffer, HashMap}
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

  private var isBinary: Boolean = false

  private var fileType : String = null

  def this(fullPath : String){
    this()
    fileFullPath = fullPath
  }

  def this(fullPath : String, isBin: Boolean) {
    this(fullPath)
    isBinary = isBin
  }

  def getSimplifiedFullPath : String = {
    if (fileFullPath.startsWith("file://"))
      fileFullPath.substring("file://".length() - 1)
    else fileFullPath
  }

  def getParentDir : String = MonitorUtils.simpleDirPath(fileObject.getParent.trim)

  //gets the input stream according to file system type - POSIX here
  def getDefaultInputStream(file : String) : InputStream = {
    logger.info("Posix File Handler - opening file " + getFullPath)
    val inputStream : InputStream =
      try {
        new FileInputStream(file)
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

  def getDefaultInputStream() : InputStream = getDefaultInputStream(getFullPath)

  @throws(classOf[KamanjaException])
  def openForRead(): InputStream = {
    /*try {
      val is = getDefaultInputStream()
      if (!isBinary) {
        fileType = CompressionUtil.getFileType(this, null)
        in = CompressionUtil.getProperInputStream(is, fileType)
        //bufferedReader = new BufferedReader(in)
      } else {
        fileType = FileType.UNKNOWN
        in = is
      }
      in
    }
    catch{
      case e : Exception => throw new KamanjaException (e.getMessage, e)
      case e : Throwable => throw new KamanjaException (e.getMessage, e)
    }*/
    openForRead(null)
  }

  /**
    *
    * @param ftype when file type is already known, no need to check again,
    *              if null or empty string is passed, file type will be checked
    *              if Plain or Unknown is passed, default stream will be opened
    *              else will try to open compressed stream based on value of ftype
    * @throws com.ligadata.Exceptions.KamanjaException
    * @return
    */
  @throws(classOf[KamanjaException])
  def openForRead(ftype : String): InputStream = {
    try {
      val is = getDefaultInputStream()

      if(ftype == null || ftype.length == 0){
        //get file type
        if (!isBinary) {
          fileType = CompressionUtil.getFileType(this, getFullPath, null)
          in = CompressionUtil.getProperInputStream(is, fileType)
        } else {
          fileType = FileType.UNKNOWN
          in = is
        }
      }
      else {//already have file type
        fileType = ftype
        if(ftype == FileType.UNKNOWN || ftype == FileType.PLAIN) in = is
        else in = CompressionUtil.getProperInputStream(is, fileType)
      }

      in
    }
    catch{
      case e : Exception => throw new KamanjaException (e.getMessage, e)
      case e : Throwable => throw new KamanjaException (e.getMessage, e)
    }
  }

  def getOpenedStreamFileType() : String = {
    fileType
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
      logger.debug(s"Posix File Handler - Moving file ${fileObject.toString} to ${newFilePath}")
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
    logger.info(s"Posix File Handler - Deleting file ($getFullPath)")
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
  override def deleteFile(fileName: String) : Boolean = {
    logger.info(s"Posix File Handler - Deleting file ($fileName)")
    try {
      val flObj = new File(fileName)
      flObj.delete
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
  def close(): Unit = {
    logger.info("Posix File Handler - Closing file " + getFullPath)
    try {
      if (in != null)
        in.close()
    }
    catch{
      case e : Exception => throw new KamanjaException (e.getMessage, e)
      case e : Throwable => throw new KamanjaException (e.getMessage, e)
    }
  }

  @throws(classOf[KamanjaException])
  def length : Long = {
    logger.info("Posix File Handler - checking length for file " + getFullPath)
    fileObject.length
  }

  @throws(classOf[KamanjaException])
  def length(file : String) : Long = {
    logger.info("Posix File Handler - checking length for file " + file)
    new File(file).length
  }

  def lastModified : Long = lastModified(getFullPath)

  def lastModified(file : String) : Long = {
    logger.info("Posix File Handler - checking modification time for file " + file)
    new File(file).lastModified
  }

  override def exists(): Boolean = exists(getFullPath)

  override def exists(file : String): Boolean = {
    logger.info("Posix File Handler - checking existence for file " + file)
    new File(file).exists
  }

  override def isFile: Boolean = {
    logger.info("Posix File Handler - checking (isFile) for file " + getFullPath)
    new File(fileFullPath).isFile
  }

  override def isDirectory: Boolean = {
    logger.info("Posix File Handler - checking (isDir) for file " + getFullPath)
    new File(fileFullPath).isDirectory
  }

  override def isAccessible : Boolean = {
    val file = new File(fileFullPath)
    file.exists() && file.canRead && file.canWrite
  }

  override def mkdirs() : Boolean = {
    logger.info("Posix File Handler - mkdirs for path " + getFullPath)
    try {
        new File(fileFullPath).mkdirs()
    }
    catch{
      case e : Throwable =>
        logger.error("Posix File Handler - Error while creating path " + fileFullPath, e)
        false
    }
  }

  private def makeFileEntry(file: File, parentFolder: String): MonitoredFile = {
    val lastReportedSize = file.length()
    val path = file.toString
    val lastModificationTime = file.lastModified()
    val isDirectory = file.isDirectory
    MonitoredFile(path, parentFolder, lastModificationTime, lastReportedSize, isDirectory, !isDirectory)
  }
  def listFiles(path : String) : Array[MonitoredFile] = {
    try {
      new File(path).listFiles.map(file => makeFileEntry(file, path))
    }
    catch{
      case ex : Throwable =>
        logger.error("Error while listing contents of dir " + path, ex)
        Array[MonitoredFile]()
    }
  }

  def disconnect() : Unit = {

  }
}


