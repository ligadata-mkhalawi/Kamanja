package com.ligadata.InputAdapters.hdfs

/**
  * Created by Yasser on 12/6/2015.
  */

import com.ligadata.AdaptersConfiguration.{SmartFileAdapterConfiguration, FileAdapterMonitoringConfig, FileAdapterConnectionConfig}
import com.ligadata.Exceptions.KamanjaException
import com.ligadata.InputAdapters.FileChangeType.FileChangeType
import com.ligadata.InputAdapters.FileChangeType.FileChangeType
import com.ligadata.InputAdapters.FileChangeType._
import org.apache.hadoop.fs.FileStatus
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.conf.Configuration
import scala.collection.mutable.{ArrayBuffer, Map}
import scala.actors.threadpool.{Executors, ExecutorService}
import java.io.{InputStream}
import org.apache.logging.log4j.{Logger, LogManager}
import com.ligadata.InputAdapters._
import scala.actors.threadpool.{Executors, ExecutorService}

class HdfsFileHandler extends SmartFileHandler {

  private var fileFullPath = ""

  private var in: InputStream = null
  private var hdFileSystem: FileSystem = null
  private var hdfsConfig: Configuration = null

  private var monitoringConfig: FileAdapterMonitoringConfig = null

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  private var isBinary: Boolean = false

  private var fileType : String = null

  private var isArchFile: Boolean = false
  private var archFileType: String = null

  private def resolveArchiveFileInfo: Unit = {
    if (monitoringConfig.hasHandleArchiveFileExtensions) {
      val typ = SmartFileHandlerFactory.getArchiveFileType(fileFullPath, monitoringConfig.handleArchiveFileExtensions)
      isArchFile = (typ != null && !typ.isEmpty)
      archFileType = typ
    }
  }

  override def isArchiveFile(): Boolean = isArchFile

  override def getArchiveFileType(): String = archFileType

  def this(fullPath: String, connectionConf: FileAdapterConnectionConfig, monitoringConfig: FileAdapterMonitoringConfig) {
    this()

    this.monitoringConfig = monitoringConfig

    fileFullPath = fullPath
    hdfsConfig = HdfsUtility.createConfig(connectionConf)
    hdFileSystem = FileSystem.newInstance(hdfsConfig)
    resolveArchiveFileInfo
  }

  def this(fullPath: String, connectionConf: FileAdapterConnectionConfig, monitoringConfig: FileAdapterMonitoringConfig, isBin: Boolean) {
    this(fullPath, connectionConf, monitoringConfig)
    isBinary = isBin
    resolveArchiveFileInfo
  }

  /*def this(fullPath : String, fs : FileSystem){
    this(fullPath)

	hdFileSystem = fs
	closeFileSystem = false
  }*/

  def getFullPath = fileFullPath

  def getSimplifiedFullPath : String = {
    HdfsUtility.getFilePathNoProtocol(getFullPath)
  }

  def getParentDir: String = {

    val simpleFilePath = HdfsUtility.getFilePathNoProtocol(getFullPath)

    val idx = simpleFilePath.lastIndexOf("/")
    MonitorUtils.simpleDirPath(simpleFilePath.substring(0, idx))
  }

  //gets the input stream according to file system type - HDFS here
  def getDefaultInputStream() : InputStream = getDefaultInputStream(getFullPath)

  def getDefaultInputStream(file : String): InputStream = {

    hdFileSystem = FileSystem.newInstance(hdfsConfig)
    val inputStream: FSDataInputStream =
      try {
        val inFile: Path = new Path(file)
        logger.info("Hdfs File Handler - opening file " + file)
        hdFileSystem.open(inFile)
      }
      catch {
        case e: Exception => {
          logger.error(e)
          null
        }
        case e: Throwable => {
          logger.error(e)
          null
        }
      }

    inputStream
  }

  @throws(classOf[KamanjaException])
  def openForRead(): InputStream = {
    try {
      val is = getDefaultInputStream()
      if (!isBinary && !isArchFile) {
        fileType = CompressionUtil.getFileType(this, getFullPath, null)
        val asIs = if(monitoringConfig == null) true else monitoringConfig.considerUnknownFileTypesAsIs
        in = CompressionUtil.getProperInputStream(is, fileType, asIs)
      } else {
        fileType = FileType.UNKNOWN
        in = is
      }
      in
    }
    catch {
      case e: Exception => throw new KamanjaException(e.getMessage, e)
      case e: Throwable => throw new KamanjaException(e.getMessage, e)
    }
  }

  def getOpenedStreamFileType() : String = {
    fileType
  }

  @throws(classOf[KamanjaException])
  def read(buf: Array[Byte], length: Int): Int = {
    read(buf, 0, length)
  }

  @throws(classOf[KamanjaException])
  def read(buf: Array[Byte], offset: Int, length: Int): Int = {

    try {
      logger.debug("Reading from hdfs file " + fileFullPath)

      if (in == null)
        return -1
      val readLength = in.read(buf, offset, length)
      logger.debug("readLength= " + readLength)
      readLength
    }
    catch {
      case e: Exception => {
        logger.warn("Error while reading from hdfs file [" + fileFullPath + "]", e)
        throw e
      }
      case e: Throwable => {
        logger.warn("Error while reading from hdfs file [" + fileFullPath + "]", e)
        throw e
      }
    }
  }

  @throws(classOf[KamanjaException])
  def moveTo(newFilePath: String): Boolean = {
    logger.info(s"Hdfs File Handler - moving file ($getFullPath) to ($newFilePath)")

    if (getFullPath.equals(newFilePath)) {
      logger.warn(s"Trying to move file ($getFullPath) but source and destination are the same")
      return false
    }

    try {
      hdFileSystem = FileSystem.get(hdfsConfig)
      val srcPath = new Path(getFullPath)
      val destPath = new Path(newFilePath)

      if (hdFileSystem.exists(srcPath)) {

        if (hdFileSystem.exists(destPath)) {
          logger.info("File {} already exists. It will be deleted first", destPath)
          hdFileSystem.delete(destPath, true)
        }

        hdFileSystem.rename(srcPath, destPath)
        logger.debug("Moved file success")
        fileFullPath = newFilePath
        true
      }
      else {
        logger.warn("Source file was not found")
        false
      }
    }
    catch {
      case ex: Exception => {
        logger.error("", ex)
        false
      }
      case ex: Throwable => {
        logger.error("", ex)
        false
      }

    } finally {

    }
  }

  @throws(classOf[KamanjaException])
  def delete(): Boolean = {
    logger.info(s"Hdfs File Handler - Deleting file ($getFullPath)")
    try {
      hdFileSystem = FileSystem.get(hdfsConfig)
      hdFileSystem.delete(new Path(getFullPath), true)
      logger.debug("Successfully deleted")
      true
    }
    catch {
      case ex: Exception => {
        logger.error("Hdfs File Handler - Error while trying to delete file " + getFullPath, ex)
        false
      }
      case ex: Throwable => {
        logger.error("Hdfs File Handler - Error while trying to delete file " + getFullPath, ex)
        false
      }

    } finally {

    }
  }

  @throws(classOf[KamanjaException])
  override def deleteFile(fileName: String): Boolean = {
    logger.info(s"Hdfs File Handler - Deleting file ($fileName)")
    try {
      hdFileSystem = FileSystem.get(hdfsConfig)
      hdFileSystem.delete(new Path(fileName), true)
      logger.debug("Successfully deleted")
      true
    }
    catch {
      case ex: Exception => {
        logger.error("Hdfs File Handler - Error while trying to delete file " + fileName, ex)
        false
      }
      case ex: Throwable => {
        logger.error("Hdfs File Handler - Error while trying to delete file " + fileName, ex)
        false
      }

    } finally {

    }
  }

  @throws(classOf[KamanjaException])
  def close(): Unit = {
    if (in != null) {
      logger.info("Hdfs File Handler - Closing file " + getFullPath)
      in.close()
    }
    if (hdFileSystem != null) {
      logger.debug("Closing Hd File System object hdFileSystem")
      //hdFileSystem.close()
    }
  }

  @throws(classOf[KamanjaException])
  def length: Long = length(getFullPath)

  @throws(classOf[KamanjaException])
  def length(file : String): Long = getHdFileSystem("get length").getFileStatus(new Path(file)).getLen

  @throws(classOf[KamanjaException])
  def lastModified: Long = lastModified(getFullPath)

  @throws(classOf[KamanjaException])
  def lastModified(file : String): Long = getHdFileSystem("get modification time").getFileStatus(new Path(file)).getModificationTime

  @throws(classOf[KamanjaException])
  override def exists(): Boolean = exists(getFullPath)

  override def exists(file : String): Boolean = getHdFileSystem("check existence").exists(new Path(file))

  @throws(classOf[KamanjaException])
  override def isFile: Boolean = getHdFileSystem("check if file").isFile(new Path(getFullPath))

  @throws(classOf[KamanjaException])
  override def isDirectory: Boolean = getHdFileSystem("check if dir").isDirectory(new Path(getFullPath))

  /**
    *
    * @param op for logging purposes
    * @return
    */
  private def getHdFileSystem(op: String): FileSystem = {
    try {
      if (op != null)
        logger.info(s"Hdfs File Handler - accessing file ($getFullPath) to " + op)

      hdFileSystem = FileSystem.get(hdfsConfig)
      hdFileSystem
    }
    catch {
      case ex: Exception => {
        throw new KamanjaException("", ex)
      }
      case ex: Throwable => {
        throw new KamanjaException("", ex)
      }

    } finally {
    }
  }

  //TODO : see if can check whether current user can read and write
  override def isAccessible: Boolean = exists()

  override def mkdirs() : Boolean = {
    logger.info("Hdfs File Handler - mkdirs for path " + getFullPath)
    try {
      getHdFileSystem("mkdirs").mkdirs(new Path(getFullPath))
    }
    catch{
      case e : Throwable =>
        logger.error("Hdfs File Handler - Error while creating path " + fileFullPath, e)
        false
    }
  }

  private def makeFileEntry(fileStatus: FileStatus, parentfolder: String): MonitoredFile = {

    val lastReportedSize = fileStatus.getLen
    val path = fileStatus.getPath.toString
    val lastModificationTime = fileStatus.getModificationTime
    val isDirectory = fileStatus.isDirectory
    MonitoredFile(path, parentfolder, lastModificationTime, lastReportedSize, isDirectory, !isDirectory)
  }

  def listDirectFiles(path : String) :  (Array[MonitoredFile], Array[MonitoredFile]) = {
    val currentDirectFiles = ArrayBuffer[MonitoredFile]()
    val currentDirectDirs = ArrayBuffer[MonitoredFile]()

    val startTm = System.nanoTime

    try {
      val files = getHdFileSystem("listFiles").listStatus(new Path(path))
      if(files != null) {
        files.foreach(file => {
          val monitoredFile = makeFileEntry(file, path)
          if (monitoredFile != null) {
            if (monitoredFile.isDirectory) currentDirectDirs.append(monitoredFile)
            else currentDirectFiles.append(monitoredFile)
          }
        })
      }

      val endTm = System.nanoTime
      val elapsedTm = endTm - startTm
      logger.debug("Sftp File Handler - finished listing dir %s. Operation took %fms. StartTime:%d, EndTime:%d.".
        format(path, elapsedTm / 1000000.0, elapsedTm, endTm))
    }
    catch {
      case ex: Exception => {
        logger.error(ex)
        Array[MonitoredFile]()
      }
      case ex: Throwable => {
        logger.error(ex)
        Array[MonitoredFile]()
      }
    }

    (currentDirectFiles.toArray, currentDirectDirs.toArray)
  }
  def listFiles(srcDir : String, maxDepth : Int) : Array[MonitoredFile] = {
    val files = ArrayBuffer[MonitoredFile]()

    val dirsToCheck = new ArrayBuffer[(String, Int)]()
    dirsToCheck.append((srcDir, 1))

    while (dirsToCheck.nonEmpty) {
      val currentDirInfo = dirsToCheck.head
      val currentDir = currentDirInfo._1
      val currentDirDepth = currentDirInfo._2
      dirsToCheck.remove(0)

      val (currentDirectFiles, currentDirectDirs) = listDirectFiles(currentDir)
      //val (currentDirectFiles, currentDirectDirs) = MonitorUtils.separateFilesFromDirs(currentAllChilds)
      files.appendAll(currentDirectFiles)

      if(maxDepth <= 0 || currentDirDepth < maxDepth)
        dirsToCheck.appendAll(currentDirectDirs.map(dir => (dir.path, currentDirDepth + 1)))

    }

    files.toArray
  }

  def disconnect() : Unit = {
    if (hdFileSystem != null) {
      try {
        logger.debug("Closing Hd File System object hdFileSystem")
        //hdFileSystem.close()
      }
      catch{
        case ex: Throwable =>
          logger.error("", ex)
      }
    }
  }

  def shutdown(): Unit = {
  }
}