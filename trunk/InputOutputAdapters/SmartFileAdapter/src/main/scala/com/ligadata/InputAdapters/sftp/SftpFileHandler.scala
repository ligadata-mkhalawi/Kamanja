package com.ligadata.InputAdapters.sftp

import java.io._
import com.ligadata.AdaptersConfiguration.{SmartFileAdapterConfiguration, FileAdapterMonitoringConfig, FileAdapterConnectionConfig}
import com.ligadata.Exceptions.KamanjaException
import com.ligadata.InputAdapters.FileChangeType.FileChangeType
import com.ligadata.InputAdapters.FileChangeType._

import scala.collection.mutable.{ArrayBuffer, Map}
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.impl.StandardFileSystemManager
import com.ligadata.InputAdapters._
import org.apache.logging.log4j.LogManager
import SftpUtility._
import scala.actors.threadpool.{Executors, ExecutorService}
import com.jcraft.jsch._


class SftpFileEntry {
  var name : String = ""
  var lastReportedSize : Long = 0
  var lastModificationTime : Long = 0
  var parent : String = ""
  var isDirectory : Boolean = false
}

class IteratorWrapper[A](iter:java.util.Iterator[A])
{
  def foreach(f: A => Unit): Unit = {
    while(iter.hasNext){
      f(iter.next)
    }
  }
}

class SftpFileHandler extends SmartFileHandler{
  private var remoteFullPath = ""

  private var connectionConfig : FileAdapterConnectionConfig = null
  //private var manager : StandardFileSystemManager = null
  private var in : InputStream = null
  //private var bufferedReader : BufferedReader = null

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  def opts = createDefaultOptions(connectionConfig)
  def sftpEncodedUri = SftpUtility.createConnectionString(connectionConfig, getFullPath)

  private var passphrase : String = null

  private var host : String = _
  private var port : Int = _
  private var channelSftp : ChannelSftp = null
  private var session : Session = null
  private var jsch : JSch = null

  private var ui : SftpUserInfo = null

  private var isBinary: Boolean = false

  private var fileType : String = null

  def this(path : String, config : FileAdapterConnectionConfig){
    this()
    this.remoteFullPath = path
    connectionConfig = config

    passphrase = if (connectionConfig.keyFile != null && connectionConfig.keyFile.length > 0)
      connectionConfig.passphrase else null

    val hostTokens = connectionConfig.hostsList(0).split(":")
    host = hostTokens(0)
    port = if(hostTokens.length ==2 && hostTokens(1) != null && hostTokens(1).length >0 ) hostTokens(1).toInt else 22 //default

    jsch = new JSch()
    if (connectionConfig.keyFile != null && connectionConfig.keyFile.length > 0) {
      jsch.addIdentity(connectionConfig.keyFile)
    }

    ui = new SftpUserInfo(connectionConfig.password, passphrase)
  }

  def this(fullPath : String, config : FileAdapterConnectionConfig, isBin: Boolean) {
    this(fullPath, config)
    isBinary = isBin
  }

  private def getNewSession() : Unit = {
    if(session == null || !session.isConnected) {
      session = jsch.getSession(connectionConfig.userId, host, port)
      session.setUserInfo(ui)
      session.connect()


      logger.info("new sftp session is opened." + MonitorUtils.getCallStack())
    }

    if(channelSftp == null || !channelSftp.isConnected || channelSftp.isClosed){
      val channel = session.openChannel("sftp")
      channel.connect()
      channelSftp = channel.asInstanceOf[ChannelSftp]
    }
  }

  def getParentDir : String = {
    val filePath = getPathOnly(getFullPath)
    val idx = filePath.lastIndexOf("/")
    MonitorUtils.simpleDirPath(filePath.substring(0, idx))
  }

  def getFullPath = remoteFullPath

  def getSimplifiedFullPath : String = {
    SftpUtility.getPathOnly(remoteFullPath)
  }

  //gets the input stream according to file system type - SFTP here
  def getDefaultInputStream : InputStream = {
    getDefaultInputStream(getFullPath)
  }
  def getDefaultInputStream(file : String) : InputStream = {

    logger.info("Sftp File Handler - opening file " + file)
    val inputStream : InputStream =
      try {
        val startTm = System.nanoTime

        getNewSession()
        val is = channelSftp.get(file)

        val endTm = System.nanoTime
        val elapsedTm = endTm - startTm

        logger.info("Sftp File Handler - finished openeing stream from file %s. Operation took %fms. StartTime:%d, EndTime:%d.".
          format(file, elapsedTm/1000000.0,elapsedTm, endTm) + MonitorUtils.getCallStack())

        is
      }
      catch {
        case e: Exception =>
          logger.error("Error getting input stream for file " + hashPath(file), e)
          null

        case e: Throwable =>
          logger.error("Error getting input stream for file " + hashPath(file), e)
          null

      }
      finally {

      }

    inputStream
  }

  @throws(classOf[KamanjaException])
  def openForRead(): InputStream = {
    logger.debug(s"Opening SFTP file ($getFullPath) to read")

    try {
      /*manager = new StandardFileSystemManager()
      manager.init()*/
      getNewSession()

      val is = getDefaultInputStream()
      if (!isBinary) {
        fileType = CompressionUtil.getFileType(this, getFullPath, null)
        in = CompressionUtil.getProperInputStream(is, fileType)
      } else {
        fileType = FileType.UNKNOWN
        in = is
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
      if (in == null) {
        logger.warn(s"Trying to read from SFTP file ($getFullPath) but input stream is null")
        return -1
      }
      logger.debug(s"Reading from SFTP file ($getFullPath)")
      in.read(buf, offset, length)
    }
    catch{
      case e : Exception => throw new KamanjaException (e.getMessage, e)
      case e : Throwable => throw new KamanjaException (e.getMessage, e)
    }
  }

  @throws(classOf[Exception])
  def moveTo(remoteNewFilePath : String) : Boolean = {

    logger.info(s"Sftp File Handler - moving file (${hashPath(getFullPath)}) to (${hashPath(remoteNewFilePath)}")

    if(getFullPath.equals(remoteNewFilePath)){
      logger.warn(s"Trying to move file ($getFullPath) but source and destination are the same")
      return false
    }

    val ui = new SftpUserInfo(connectionConfig.password, passphrase)
    try {
      getNewSession()

      if(!fileExists(channelSftp,getFullPath )) {
        logger.warn("Source file {} does not exists", getFullPath)
        return false
      }
      else

      //checking if dest file already exists
      if(fileExists(channelSftp, remoteNewFilePath)) {
        logger.info("File {} already exists. It will be deleted first", remoteNewFilePath)
        channelSftp.rm(remoteNewFilePath)
      }

      channelSftp.rename(getFullPath, remoteNewFilePath)

      true
    }
    catch {
      case ex: Exception =>
        logger.error("Sftp File Handler - Error while trying to moving sftp file " +
          getFullPath + " to " + remoteNewFilePath, ex)
        false

      case ex: Throwable =>
        logger.error("Sftp File Handler - Error while trying to moving sftp file " +
          getFullPath + " to " + remoteNewFilePath, ex)
        false
    }
    finally{
      disconnect()
    }

  }

  @throws(classOf[Exception])
  def delete() : Boolean = {
    logger.info(s"Sftp File Handler - Deleting file (${hashPath(getFullPath)}")

    val ui = new SftpUserInfo(connectionConfig.password, passphrase)

    try {
      getNewSession()
      channelSftp.rm(getFullPath)
      true
    }
    catch {
      case ex: Exception =>
        logger.error("Sftp File Handler - Error while trying to delete sftp file " + getFullPath, ex)
         false

      case ex: Throwable =>
        logger.error("Sftp File Handler - Error while trying to delete sftp file " + getFullPath, ex)
         false

    }
    finally{
      disconnect()
    }
  }

  @throws(classOf[Exception])
  override def deleteFile(fileName: String) : Boolean = {
    logger.info(s"Sftp File Handler - Deleting file (${hashPath(fileName)}")

    val ui = new SftpUserInfo(connectionConfig.password, passphrase)

    try {
      getNewSession
      channelSftp.rm(fileName)
      true
    }
    catch {
      case ex: Exception =>
        logger.error("Sftp File Handler - Error while trying to delete sftp file " + fileName, ex)
        false

      case ex: Throwable =>
        logger.error("Sftp File Handler - Error while trying to delete sftp file " + fileName, ex)
        false

    }
    finally{
      disconnect()
    }
  }

  @throws(classOf[Exception])
  def close(): Unit = {
    logger.info("Sftp File Handler - Closing file " + hashPath(getFullPath))
    /*if(bufferedReader != null)
      bufferedReader.close()*/
    if(in != null) {
      try {
        in.close()
      }
      catch{
        case ex : Exception => logger.warn("Error while closing sftp file " + getFullPath, ex)
        case ex : Throwable => logger.warn("Error while closing sftp file " + getFullPath, ex)
      }
      in = null
    }

    disconnect()
  }

  @throws(classOf[Exception])
  def length(file : String) : Long = {
    logger.info("Sftp File Handler - checking length for file " + hashPath(file))
    val startTm = System.nanoTime
    val attrs = getRemoteFileAttrs()
    val result = if (attrs == null) 0 else attrs.getSize

    val endTm = System.nanoTime
    val elapsedTm = endTm - startTm

    logger.info("Sftp File Handler - finished checking length of file %s. Operation took %fms. StartTime:%d, EndTime:%d.".
      format(file, elapsedTm/1000000.0,elapsedTm, endTm) + MonitorUtils.getCallStack())

    result
  }

  @throws(classOf[KamanjaException])
  def length : Long = {
    val result = length(getFullPath)
    disconnect()
    result
  }

  @throws(classOf[Exception])
  def lastModified : Long = {
    val result = lastModified(getFullPath)
    disconnect()
    result
  }

  @throws(classOf[Exception])
  def lastModified(file : String) : Long = {
    logger.info("Sftp File Handler - checking modification time for file " + hashPath(file))
    val startTm = System.nanoTime
    val attrs = getRemoteFileAttrs(file, true)
    val result = if (attrs == null) 0 else attrs.getMTime

    val endTm = System.nanoTime
    val elapsedTm = endTm - startTm

    logger.info("Sftp File Handler - finished checking lastModified of file %s. Operation took %fms. StartTime:%d, EndTime:%d.".
      format(file, elapsedTm/1000000.0,elapsedTm, endTm) + MonitorUtils.getCallStack())

    result
  }

  @throws(classOf[Exception])
  def exists(): Boolean = {
    val result = exists(getFullPath)
    disconnect()
    result
  }

  @throws(classOf[Exception])
  def exists(file : String): Boolean = {
    try {
      logger.info("Sftp File Handler - checking existence for file " + hashPath(file))
      val startTm = System.nanoTime
      val att = getRemoteFileAttrs(logError = false)
      val exists = att != null

      val endTm = System.nanoTime
      val elapsedTm = endTm - startTm

      logger.info("Sftp File Handler - finished checking existing of file %s. Operation took %fms. StartTime:%d, EndTime:%d.".
        format(file, elapsedTm/1000000.0,elapsedTm, endTm) + MonitorUtils.getCallStack())

      exists
    }
    catch{
      case ex : Exception => false
      case ex : Throwable => false
    }
  }

  private def fileExists(channel : ChannelSftp, file : String) : Boolean = {
    logger.info("Sftp File Handler - checking length for file " + file)
      try{
        channelSftp.lstat(file)
         true
      }
      catch{//source file does not exist, nothing to do
        case ee  : Exception =>
          //no need to log, file does not exist, calling threads will report
           false

        case ee  : Throwable =>
          false

      }
  }


  @throws(classOf[Exception])
  override def isFile: Boolean = {
    logger.info("Sftp File Handler - checking (isFile) for file " + hashPath(getFullPath))
    val startTm = System.nanoTime
    val attrs = getRemoteFileAttrs()
    val result = if (attrs == null) false else !attrs.isDir

    val endTm = System.nanoTime
    val elapsedTm = endTm - startTm

    logger.info("Sftp File Handler - finished checking isFile of file %s. Operation took %fms. StartTime:%d, EndTime:%d.".
      format(getFullPath, elapsedTm/1000000.0,elapsedTm, endTm) + MonitorUtils.getCallStack())

    disconnect()

    result
  }

  @throws(classOf[Exception])
  override def isDirectory: Boolean = {
    logger.info("Sftp File Handler - checking (isDir) for file " + hashPath(getFullPath))
    val startTm = System.nanoTime
    val attrs = getRemoteFileAttrs()
    val result =  if (attrs == null) false else attrs.isDir

    val endTm = System.nanoTime
    val elapsedTm = endTm - startTm

    logger.info("Sftp File Handler - finished checking isDirectory of file %s. Operation took %fms. StartTime:%d, EndTime:%d.".
      format(getFullPath, elapsedTm/1000000.0,elapsedTm, endTm) + MonitorUtils.getCallStack())

    disconnect()

    result
  }

  private def getRemoteFileAttrs(logError : Boolean = true) :  SftpATTRS = {
    getRemoteFileAttrs(getFullPath, logError)
  }

  private def getRemoteFileAttrs(filePath : String, logError : Boolean) :  SftpATTRS = {
    try {
      getNewSession
      channelSftp.lstat(filePath)
    }
    catch {
      case ex : Exception =>
        if(logError)
          logger.warn("Error while getting file attrs for file " + getFullPath)
        null

      case ex : Throwable =>
        if(logError)
          logger.warn("Error while getting file attrs for file " + getFullPath)
        null

    } finally {
      /*logger.debug("Closing SFTP session from getRemoteFileAttrs()")
      if(channelSftp != null) channelSftp.exit()
      if(session != null) session.disconnect()*/
    }
  }

  //no accurate way to make sure a file/folder is readable or writable by current user
  //api can only tell the unix rep. of file permissions but cannot find user name or group name of that file
  //so for now return true if exists
  override def isAccessible : Boolean = true//exists()     assuming exists is called before   isAccessible()

  override def mkdirs() : Boolean = {
    logger.info("Sftp File Handler - mkdirs for path " + getFullPath)
    try {

      getNewSession

      //no direct method to create nonexistent parent dirs, so create one by one whenever necessary
      val tokens = getFullPath.split("/")
      var currentPath = ""
      var idx = 0
      var fail = false
      while(idx < tokens.length && !fail) {
        currentPath += tokens(idx) + "/"
        if(tokens(idx).length() >0){
          if(!fileExists(channelSftp, currentPath)){
            logger.error("Sftp File Handler - Error while creating path " + currentPath)
            try{
              channelSftp.mkdir(currentPath)
            }
            catch{
              case ex : Throwable =>
                logger.error("", ex)
                fail = true
            }
          }
        }
        idx += 1
      }

      !fail
    }
    catch {
      case ex : Throwable =>
        logger.error("Sftp File Handler - Error while creating path " + getFullPath, ex)
        //ex.printStackTrace()
        false

    } finally {
      disconnect()
    }
  }

  implicit def iteratorToWrapper[T](iter:java.util.Iterator[T]):IteratorWrapper[T] = new IteratorWrapper[T](iter)
  def listDirectFiles(path : String) : (Array[MonitoredFile], Array[MonitoredFile]) = {

    val currentDirectFiles = ArrayBuffer[MonitoredFile]()
    val currentDirectDirs = ArrayBuffer[MonitoredFile]()

    try {
      getNewSession()

      val startTm = System.nanoTime
      val children = channelSftp.ls(path).iterator()

      for (child <- children) {
        val monitoredFile = makeFileEntry(child, path)
        if(monitoredFile != null) {
          if(monitoredFile.isDirectory) currentDirectDirs.append(monitoredFile)
          else currentDirectFiles.append(monitoredFile)
        }
      }

      val endTm = System.nanoTime
      val elapsedTm = endTm - startTm

      logger.debug("Sftp File Handler - finished listing dir %s. Operation took %fms. StartTime:%d, EndTime:%d.".
        format(path, elapsedTm / 1000000.0, elapsedTm, endTm))

    }
    catch {
      case ex : Throwable =>
        logger.error("Sftp File Handler - Error while listing files of dir " + path, ex)
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

  private def makeFileEntry(child : Any, parent : String) : MonitoredFile = {

    val aclass = Class.forName("com.jcraft.jsch.ChannelSftp$LsEntry")
    val method = aclass.getDeclaredMethod("getFilename")
    val filename = method.invoke(child).toString
    if (!filename.equals(".") && !filename.equals("..")) {
      val getAttrsMethod = aclass.getDeclaredMethod("getAttrs")
      val anySftpATTRS = getAttrsMethod.invoke(child)
      val sftpATTRS = anySftpATTRS.asInstanceOf[com.jcraft.jsch.SftpATTRS]
      val lastModificationTime = sftpATTRS.getMTime();
      val lastReportedSize = sftpATTRS.getSize()
      val isDir = sftpATTRS.isDir
      MonitoredFile(parent + "/" + filename, parent, lastModificationTime, lastReportedSize, isDir, !isDir)
    }
    else null

  }

  def disconnect() : Unit = {
    try{

      logger.info("Closing SFTP session from disconnect(). original file path is %s.".format(getFullPath) + MonitorUtils.getCallStack())

      if(channelSftp != null) channelSftp.exit()
      if(session != null) session.disconnect()

      channelSftp = null
      session = null
    }
    catch{
      case ex : Exception =>
        logger.error("",ex)
      case ex : Throwable =>
        logger.error("",ex)
    }
  }
}