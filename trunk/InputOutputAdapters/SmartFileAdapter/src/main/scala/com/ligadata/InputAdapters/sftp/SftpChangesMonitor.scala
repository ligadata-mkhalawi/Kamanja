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
        logger.error("Sftp File Handler - Error while creating path " + getFullPath)
        //ex.printStackTrace()
        false

    } finally {
      disconnect()
    }
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

class SftpChangesMonitor (adapterName : String, modifiedFileCallback:(MonitoredFile, Boolean) => Unit) extends SmartFileMonitor{

  private var isMonitoring = false
  private var checkFolders = true

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  private var connectionConf : FileAdapterConnectionConfig = null
  private var monitoringConf :  FileAdapterMonitoringConfig = null

  private var monitorsExecutorService: ExecutorService = null

  private var host : String = _
  private var port : Int = _

  private val filesStatusMap = Map[String, MonitoredFile]()
  private val filesStatusMapLock = new Object()

  private val processedFilesMap : scala.collection.mutable.LinkedHashMap[String, Long] = scala.collection.mutable.LinkedHashMap[String, Long]()

  def init(adapterSpecificCfgJson: String): Unit ={
    val(_, c, m, a) =  SmartFileAdapterConfiguration.parseSmartFileAdapterSpecificConfig(adapterName, adapterSpecificCfgJson)
    connectionConf = c
    monitoringConf = m

    if(connectionConf.hostsList == null || connectionConf.hostsList.length == 0){
      val err = "Invalid host for Smart SFTP File Adapter Config:" + adapterName
      throw new KamanjaException(err, null)
    }

    val hostTokens = connectionConf.hostsList(0).split(":")
    host = hostTokens(0)
    port = if(hostTokens.length > 1 && hostTokens(1).length >0 ) hostTokens(1).toInt else 22 //default
  }

  def markFileAsProcessed(filePath : String) : Unit = {
    filesStatusMapLock.synchronized {
      logger.info("Smart File Consumer (SFTP Monitor) - removing file {} from map {} as it is processed", filePath, filesStatusMap)
      filesStatusMap.remove(filePath)
    }
    //MonitorUtils.addProcessedFileToMap(filePath, processedFilesMap) //TODO : uncomment later
  }

  def setMonitoringStatus(status : Boolean): Unit ={
    checkFolders = status
  }

  def monitor(): Unit = {
    val validModifiedFiles = ArrayBuffer[(MonitoredFile, FileChangeType)]()
    val manager: StandardFileSystemManager = new StandardFileSystemManager()

    isMonitoring = true
    //Initializes the file manager
    manager.init()

    val maxThreadCount = Math.min(monitoringConf.monitoringThreadsCount, monitoringConf.detailedLocations.length)
    monitorsExecutorService = Executors.newFixedThreadPool(maxThreadCount)
    logger.info("Smart File Monitor - running {} threads to monitor {} dirs",
      monitoringConf.monitoringThreadsCount.toString, monitoringConf.detailedLocations.length.toString)

    val monitoredDirsQueue = new MonitoredDirsQueue()
    monitoredDirsQueue.init(monitoringConf.detailedLocations, monitoringConf.waitingTimeMS)

    for (currentThreadId <- 1 to maxThreadCount) {

      val dirMonitorthread = new Runnable() {

        override def run() = {
          try {

            //var firstCheck = true

            while (isMonitoring) {

              if (checkFolders) {

                val dirQueuedInfo = monitoredDirsQueue.getNextDir()
                if (dirQueuedInfo != null) {
                  val location = dirQueuedInfo._1
                  val isFirstScan = dirQueuedInfo._3
                  val targetRemoteFolder = location.srcDir

                  try {
                    val sftpEncodedUri = createConnectionString(connectionConf, targetRemoteFolder)
                    logger.info(s"Checking configured SFTP directory ($targetRemoteFolder)...")

                    val modifiedDirs = new ArrayBuffer[(String, Int)]()//dir name, dir depth
                    modifiedDirs.append((sftpEncodedUri, 1))
                    while (modifiedDirs.nonEmpty) {
                      //each time checking only updated folders: first find direct children of target folder that were modified
                      // then for each folder of these search for modified files and folders, repeat for the modified folders

                      val aFolder = modifiedDirs.head
                      val modifiedFiles = Map[MonitoredFile, FileChangeType]() // these are the modified files found in folder $aFolder

                      modifiedDirs.remove(0)
                      findDirModifiedDirectChilds(aFolder._1, aFolder._2, manager, modifiedDirs, modifiedFiles, isFirstScan)
                      logger.debug("modifiedFiles map is {}", modifiedFiles)

                      //check for file names pattern
                      validModifiedFiles.clear()
                      if (location.fileComponents != null) {
                        modifiedFiles.foreach(tuple => {
                          if (MonitorUtils.isPatternMatch(MonitorUtils.getFileName(tuple._1.path), location.fileComponents.regex))
                            validModifiedFiles.append(tuple)
                          else
                            logger.warn("Smart File Consumer (SFTP) : File {}, does not follow configured name pattern ({}), so it will be ignored - Adapter {}",
                              tuple._1.path, location.fileComponents.regex, adapterName)
                        })
                      }
                      else
                        validModifiedFiles.appendAll(modifiedFiles)

                      logger.debug("validModifiedFiles size is {}", validModifiedFiles.length.toString)
                      val orderedModifiedFiles = validModifiedFiles/*.map(tuple => (tuple._1, tuple._2)).toList.
                        sortWith((tuple1, tuple2) => MonitorUtils.compareFiles(tuple1._1, tuple2._1, location) < 0)*/

                      if (orderedModifiedFiles.nonEmpty)
                        orderedModifiedFiles.foreach(tuple => {

                          logger.debug("calling sftp monitor is calling file callback for MonitorController for file {}, initial = {}",
                            tuple._1.path, (tuple._2 == AlreadyExisting).toString)
                          try {
                            /*val fileInfo = MonitoredFile(getPathOnly(tuple._1.path), tuple._1.parent, tuple._1.lastModificationTime,
                              tuple._1.lastReportedSize, false, true)*/
                            modifiedFileCallback(tuple._1, tuple._2 == AlreadyExisting)
                          }
                          catch {
                            case e: Throwable =>
                              logger.error("Smart File Consumer (Sftp) : Error while notifying Monitor about new file", e)
                          }

                        }
                        )
                    }

                  }
                  catch {
                    case ex: Exception => logger.error("Smart File Consumer (sftp Monitor) - Error while checking folder " + targetRemoteFolder, ex)
                    case ex: Throwable => logger.error("Smart File Consumer (sftp Monitor) - Error while checking folder " + targetRemoteFolder, ex)
                  }

                  val updateDirQueuedInfo = (dirQueuedInfo._1, dirQueuedInfo._2, false)//not first scan anymore

                  monitoredDirsQueue.reEnqueue(updateDirQueuedInfo) // so the folder gets monitored again
                }
                else {
                  //happens if last time queue head dir was monitored was less than waiting time
                  logger.info("Smart File Monitor - no folders to monitor for now. Thread {} is sleeping for {} ms", currentThreadId.toString, monitoringConf.waitingTimeMS.toString)
                  Thread.sleep(monitoringConf.waitingTimeMS)
                }

              }
              else {
                logger.info("Smart File Monitor - too many files already in process queue. monitoring thread {} is sleeping for {} ms", currentThreadId.toString, monitoringConf.waitingTimeMS.toString)
                Thread.sleep(monitoringConf.waitingTimeMS)
              }
            }

            //if(!isMonitoring)
            //globalFileMonitorCallbackService.shutdown()
          }
          catch {
            case ex: Exception =>
              logger.error("Smart File Monitor - Error", ex)

            case ex: Throwable =>
              logger.error("Smart File Monitor - Error", ex)

          }
          finally {
            manager.close()
          }
        }
      }
      monitorsExecutorService.execute(dirMonitorthread)
    }

  }

  def shutdown(): Unit ={
    isMonitoring = false
    processedFilesMap.clear()
    monitorsExecutorService.shutdown()
  }


  private def filterQueuedFiles(files : Array[FileObject]) : Array[FileObject] = {
    if(files == null) return Array[FileObject]()
    files.filter(f => !filesStatusMap.contains(getPathOnly(f.getURL.toString)))
  }

  private def findDirModifiedDirectChilds(parentFolder : String, parentFolderDepth : Int, manager : StandardFileSystemManager,
                                          modifiedDirs : ArrayBuffer[(String, Int)], modifiedFiles : Map[MonitoredFile, FileChangeType], isFirstCheck : Boolean){
    val parentFolderHashed = hashPath(parentFolder)//used for logging since path contains user and password
    logger.info("SFTP Changes Monitor - listing dir " + parentFolderHashed)

    val startTm = System.nanoTime
    val allDirectChildren = getRemoteFolderContents(parentFolder, manager)
    val getRemoteFolderContentsEndTm = System.nanoTime
    val getRemoteFolderContentsElapsedTm = getRemoteFolderContentsEndTm - startTm
    logger.info("Sftp File Handler - finished call for getRemoteFolderContents() %s. Operation took %fms. StartTime:%d, EndTime:%d.".
      format(parentFolder, getRemoteFolderContentsElapsedTm/1000000.0,getRemoteFolderContentsElapsedTm, getRemoteFolderContentsEndTm))


    val filteredFiles = filterQueuedFiles(allDirectChildren)//.sortWith(_.getContent.getLastModifiedTime < _.getContent.getLastModifiedTime)
    logger.debug("filteredFiles: "+filteredFiles.map(f=>f.getURL.toString).mkString(","))

    var changeType : FileChangeType = null //new, modified

    logger.debug("filesStatusMap=" + filesStatusMap)

    //logger.debug("got the following children for checked folder " + directChildren.map(c => c.getURL.toString).mkString(", "))
    //process each file reported by FS cache.
    filteredFiles.foreach(child => {
      var isChanged = false
      val uniquePath = child.getURL.toString
      val uniquePathOnly = getPathOnly(uniquePath)

      val currentChildEntry = makeFileEntry(child, uniquePathOnly, parentFolder)
      if(currentChildEntry != null) {

        if (processedFilesMap.contains(uniquePathOnly)) //TODO what if file was moved then copied again?
          logger.info("Smart File Consumer (Sftp) - File {} already processed, ignoring - Adapter {}", uniquePathOnly, adapterName)
        else {
          //if (!filesStatusMap.contains(uniquePath))
          {
            //path is new
            isChanged = true
            changeType = if (isFirstCheck) AlreadyExisting else New

            logger.debug("SftpChangesMonitor - file {} is {}", uniquePathOnly, changeType.toString)

            filesStatusMap.put(uniquePathOnly, currentChildEntry)
            /*if (currentChildEntry.isDirectory)
            modifiedDirs += uniquePath*/
          }


          //TODO : this method to find changed folders is not working as expected. so for now check all dirs
          if (currentChildEntry.isDirectory &&
            (monitoringConf.dirMonitoringDepth == 0 || parentFolderDepth < monitoringConf.dirMonitoringDepth)) {
            logger.debug("SftpChangesMonitor - file {} is directory", uniquePathOnly)
            modifiedDirs.append((uniquePath, parentFolderDepth + 1))
          }

          if (isChanged) {
            if (currentChildEntry.isDirectory) {
              //logger.debug("file {} is directory", uniquePath)
            }
            else {
              if (changeType == New || changeType == AlreadyExisting) {
                logger.debug("file {} will be added to modifiedFiles map", uniquePathOnly)
                //val fileHandler = new SftpFileHandler(getPathOnly(uniquePath), connectionConf)
                modifiedFiles.put(currentChildEntry, changeType)
              }
            }
          }
        }
      }
    }
    )


    val deletedFiles = new ArrayBuffer[String]()

    filesStatusMap.values.foreach(fileEntry =>{
      if(isDirectParentDir(fileEntry, parentFolder)){
        if(!allDirectChildren.exists(fileStatus => getPathOnly(fileStatus.getURL.toString).equals(fileEntry.path))) {
          //key that is no more in the folder => file/folder deleted
          logger.debug("file {} is no more under folder  {}, will be deleted from map", fileEntry.path, fileEntry.parent)
          deletedFiles += fileEntry.path
        }
        else {
          //logger.debug("file {} is still under folder  {}", fileEntry.name, fileEntry.parent)
        }
      }
    })

    //logger.debug("files to be deleted from map are : ", deletedFiles)
    deletedFiles.foreach(f => filesStatusMap.remove(f))

    val allFuncEndTm = System.nanoTime
    val allFuncElapsedTm = allFuncEndTm - startTm
    logger.info("Sftp File Handler - finished call for findDirModifiedDirectChilds() %s. Operation took %fms. StartTime:%d, EndTime:%d.".
      format(parentFolder, allFuncElapsedTm/1000000.0,allFuncElapsedTm, allFuncEndTm))
  }

  private def getRemoteFolderContents(parentRemoteFolderUri : String, manager : StandardFileSystemManager) : Array[FileObject] = {
    val remoteDir : FileObject = manager.resolveFile(parentRemoteFolderUri )
    val children = remoteDir.getChildren
    children
  }

  private def makeFileEntry(fileObject : FileObject, uniquePathOnly : String, parent : String) : MonitoredFile = {
    val startTm = System.nanoTime
    val stillExists = fileObject.exists() //operations of file object take very little time - make sure file was not deleted
    val endTm = System.nanoTime
    val elapsedTm = endTm - startTm
    //logger.debug("Sftp File Monitor - finished checking existence of fileObject %s. Operation took %fms. StartTime:%d, EndTime:%d.".
      //format(uniquePathOnly, elapsedTm/1000000.0,elapsedTm, endTm) + MonitorUtils.getCallStack())

    if(stillExists) {
      val path = uniquePathOnly
      //val parent = fileObject.getParent.getURL.toString
      val isDirectory = fileObject.getType.getName.equalsIgnoreCase("folder")
      val lastModificationTime = if (isDirectory) 0 else fileObject.getContent.getLastModifiedTime
      val lastReportedSize = if (isDirectory) -1 else fileObject.getContent.getSize //size is not defined for folders
      MonitoredFile(path, parent, lastModificationTime, lastReportedSize, isDirectory, !isDirectory)
    }
    else null
  }

  /*private def isDirectParentDir(fileObj : FileObject, dir : String) : Boolean = {
    fileObj.getParent.getURL.toString().equals(dir)
  }*/

  private def isDirectParentDir(fileObj : MonitoredFile, dirUrl : String) : Boolean = {
    logger.debug("comparing folders {} and {}", getPathOnly(fileObj.parent), getPathOnly(dirUrl))
    getPathOnly(fileObj.parent).equals(getPathOnly(dirUrl))
  }

  //retrieve only path and remove connection ino
  private def getPathOnly(url : String) : String = {
    val hostIndex = url.indexOf(host)
    if(hostIndex < 0 )
      return url

    val afterHostUrl = url.substring(hostIndex + host.length)
    afterHostUrl.substring(afterHostUrl.indexOf("/"))
  }

  override def listFiles(path: String): Array[String] ={
    val sftpEncodedUri = createConnectionString(connectionConf, path)
    val manager : StandardFileSystemManager  = new StandardFileSystemManager()
    manager.init()
    val remoteDir : FileObject = manager.resolveFile(sftpEncodedUri)
    remoteDir.getChildren.filter(x => x.getType.getName.equalsIgnoreCase("folder") == false).map(x => x.getName.getBaseName)
  }
}