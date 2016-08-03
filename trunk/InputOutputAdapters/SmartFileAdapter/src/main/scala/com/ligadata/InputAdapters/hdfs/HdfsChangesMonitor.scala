package com.ligadata.InputAdapters.hdfs

/**
 * Created by Yasser on 12/6/2015.
 */
import com.ligadata.AdaptersConfiguration.{SmartFileAdapterConfiguration, FileAdapterMonitoringConfig, FileAdapterConnectionConfig}
import com.ligadata.Exceptions.KamanjaException
import com.ligadata.InputAdapters.FileChangeType.FileChangeType
import com.ligadata.InputAdapters.FileChangeType._
import org.apache.hadoop.fs.FileStatus
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.conf.Configuration
import scala.collection.mutable.{ArrayBuffer, Map}
import scala.actors.threadpool.{ Executors, ExecutorService }
import java.io.{InputStream}
import org.apache.logging.log4j.{ Logger, LogManager }
import com.ligadata.InputAdapters.{MonitorUtils, CompressionUtil, SmartFileHandler, SmartFileMonitor}
import scala.actors.threadpool.{Executors, ExecutorService}

class HdfsFileEntry {
  var name : String = ""
  var lastReportedSize : Long = 0
  var lastModificationTime : Long = 0
  var parent : String = ""
  //boolean processed
}

class MofifiedFileCallbackHandler(fileHandler : SmartFileHandler, modifiedFileCallback:(SmartFileHandler) => Unit) extends Runnable{
  def run() {
    modifiedFileCallback(fileHandler)
  }
}

class HdfsFileHandler extends SmartFileHandler{

  private var fileFullPath = ""
  
  private var in : InputStream = null
  private var hdFileSystem : FileSystem = null
  private var hdfsConfig : Configuration = null

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  private var isBinary: Boolean = false

  def this(fullPath : String, connectionConf : FileAdapterConnectionConfig){
    this()

    fileFullPath = fullPath
    hdfsConfig = HdfsUtility.createConfig(connectionConf)
    hdFileSystem = FileSystem.newInstance(hdfsConfig)
  }

  def this(fullPath : String, connectionConf : FileAdapterConnectionConfig, isBin: Boolean) {
    this(fullPath, connectionConf)
    isBinary = isBin
  }

  /*def this(fullPath : String, fs : FileSystem){
    this(fullPath)

	hdFileSystem = fs
	closeFileSystem = false
  }*/

  def getFullPath = fileFullPath
  def getParentDir : String = {

    val simpleFilePath = HdfsUtility.getFilePathNoProtocol(getFullPath)

    val idx = simpleFilePath.lastIndexOf("/")
    MonitorUtils.simpleDirPath(simpleFilePath.substring(0, idx))
  }

  //gets the input stream according to file system type - HDFS here
  def getDefaultInputStream() : InputStream = {

    hdFileSystem = FileSystem.newInstance(hdfsConfig)
    val inputStream : FSDataInputStream =
      try {
        val inFile : Path = new Path(getFullPath)
        logger.info("Hdfs File Handler - opening file " + getFullPath)
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
      if (!isBinary) {
        val compressionType = CompressionUtil.getFileType(this, null)
        in = CompressionUtil.getProperInputStream(is, compressionType)
      } else {
        is
      }
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
      logger.debug("Reading from hdfs file " + fileFullPath)

      if (in == null)
        return -1
      val readLength = in.read(buf, offset, length)
      logger.debug("readLength= " + readLength)
      readLength
    }
    catch{
      case e : Exception => {
        logger.warn("Error while reading from hdfs file [" + fileFullPath + "]",e)
        throw e
      }
      case e : Throwable => {
        logger.warn("Error while reading from hdfs file [" + fileFullPath + "]",e)
        throw e
      }
    }
  }

  @throws(classOf[KamanjaException])
  def moveTo(newFilePath : String) : Boolean = {
    logger.info(s"Hdfs File Handler - moving file ($getFullPath) to ($newFilePath)")

     if(getFullPath.equals(newFilePath)){
      logger.warn(s"Trying to move file ($getFullPath) but source and destination are the same")
      return false
     }

     try {
       hdFileSystem = FileSystem.get(hdfsConfig)
       val srcPath = new Path(getFullPath)
       val destPath = new Path(newFilePath)

        if (hdFileSystem.exists(srcPath)) {

          if(hdFileSystem.exists(destPath)){
            logger.info("File {} already exists. It will be deleted first", destPath)
            hdFileSystem.delete(destPath, true)
          }

          hdFileSystem.rename(srcPath, destPath)
          logger.debug("Moved file success")
          fileFullPath = newFilePath
            true
        }
        else{
            logger.warn("Source file was not found")
            false
        }
     } 
     catch {
       case ex : Exception => {
         logger.error("", ex)
         false
       }
       case ex : Throwable => {
         logger.error("", ex)
         false
       }

     } finally {

     }
  }
  
  @throws(classOf[KamanjaException])
  def delete() : Boolean = {
    logger.info(s"Hdfs File Handler - Deleting file ($getFullPath)")
     try {
       hdFileSystem = FileSystem.get(hdfsConfig)
       hdFileSystem.delete(new Path(getFullPath), true)
        logger.debug("Successfully deleted")
        true
     } 
     catch {
       case ex : Exception => {
        logger.error("Hdfs File Handler - Error while trying to delete file " + getFullPath, ex)
        false
       }
       case ex : Throwable => {
         logger.error("Hdfs File Handler - Error while trying to delete file " + getFullPath, ex)
         false
       }
        
     } finally {

     }
  }

  @throws(classOf[KamanjaException])
  def close(): Unit = {
    if(in != null){
      logger.info("Hdfs File Handler - Closing file " + getFullPath)
      in.close()
    }
    if(hdFileSystem != null) {
      logger.debug("Closing Hd File System object hdFileSystem")
      hdFileSystem.close()
    }
  }

  @throws(classOf[KamanjaException])
  def length : Long = getHdFileSystem("get length").getFileStatus(new Path(getFullPath)).getLen

  @throws(classOf[KamanjaException])
  def lastModified : Long = getHdFileSystem("get modification time").getFileStatus(new Path(getFullPath)).getModificationTime

  @throws(classOf[KamanjaException])
  override def exists(): Boolean = getHdFileSystem("check existence").exists(new Path(getFullPath))

  @throws(classOf[KamanjaException])
  override def isFile: Boolean = getHdFileSystem("check if file").isFile(new Path(getFullPath))

  @throws(classOf[KamanjaException])
  override def isDirectory: Boolean = getHdFileSystem("check if dir").isDirectory(new Path(getFullPath))

  /**
    *
    * @param op for logging purposes
    * @return
    */
  private def getHdFileSystem(op : String) : FileSystem = {
    try {
      if(op != null)
        logger.info(s"Hdfs File Handler - accessing file ($getFullPath) to " + op)

      hdFileSystem = FileSystem.get(hdfsConfig)
      hdFileSystem
    }
    catch {
      case ex : Exception => {
        throw new KamanjaException("", ex)
      }
      case ex : Throwable => {
        throw new KamanjaException("", ex)
      }

    } finally {
    }
  }

  //TODO : see if can check whether current user can read and write
  override def isAccessible : Boolean = exists()
}

/**
 * callback is the function to call when finding a modified file, currently has one parameter which is the file path
 */
class HdfsChangesMonitor (adapterName : String, modifiedFileCallback:(SmartFileHandler, Boolean) => Unit) extends SmartFileMonitor{

  private var isMonitoring = false
  private var checkFolders = true
  
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  val poolSize = 5
  private val globalFileMonitorCallbackService: ExecutorService = Executors.newFixedThreadPool(poolSize)

  private var connectionConf : FileAdapterConnectionConfig = null
  private var monitoringConf :  FileAdapterMonitoringConfig = null
  private var monitorsExecutorService: ExecutorService = null
  private var hdfsConfig : Configuration = null
  private val filesStatusMap = Map[String, HdfsFileEntry]()
  private val processedFilesMap : scala.collection.mutable.LinkedHashMap[String, Long] = scala.collection.mutable.LinkedHashMap[String, Long]()

  def init(adapterSpecificCfgJson: String): Unit ={
    val(_type, c, m) =  SmartFileAdapterConfiguration.parseSmartFileAdapterSpecificConfig(adapterName, adapterSpecificCfgJson)
    connectionConf = c
    monitoringConf = m

    if(connectionConf.hostsList == null || connectionConf.hostsList.length == 0){
      val err = "HostsList is missing or invalid for Smart HDFS File Adapter Config:" + adapterName
      throw new KamanjaException(err, null)
    }
    if(connectionConf.authentication.equalsIgnoreCase("kerberos")){
      if(connectionConf.principal == null || connectionConf.principal.length == 0 ||
        connectionConf.keytab == null || connectionConf.keytab.length == 0){
        val err = "Principal and Keytab cannot be empty for Kerberos authentication for Smart HDFS File Adapter Config:" + adapterName
        throw new KamanjaException(err, null)
      }
    }

    hdfsConfig = HdfsUtility.createConfig(connectionConf)
  }

  def markFileAsProcessed(filePath : String) : Unit = {
    logger.info("Smart File Consumer (SFTP Monitor) - removing file {} from map {} as it is processed", filePath, filesStatusMap)
    filesStatusMap.remove(filePath)

    //MonitorUtils.addProcessedFileToMap(filePath, processedFilesMap) //TODO : uncomment later
  }

  def setMonitoringStatus(status : Boolean): Unit ={
    checkFolders = status
  }

  def shutdown: Unit ={

    isMonitoring = false
    processedFilesMap.clear()
    monitorsExecutorService.shutdown()
  }

  def getFolderContents(parentfolder : String, hdFileSystem : FileSystem) : Array[FileStatus] = {
    try {
      val files = hdFileSystem.listStatus(new Path(parentfolder))
      files
    }
    catch{
      case ex : Exception => {
        logger.error(ex)
        new Array[FileStatus](0)
      }
      case ex : Throwable => {
        logger.error(ex)
        new Array[FileStatus](0)
      }
    }
  }

  def monitor(){
    val validModifiedFiles = ArrayBuffer[(SmartFileHandler, FileChangeType)]()
    isMonitoring = true
    monitorsExecutorService = Executors.newFixedThreadPool(monitoringConf.detailedLocations.length)

    monitoringConf.detailedLocations.foreach(location => {
      val folderToWatch = location.srcDir
      val dirMonitorthread = new Runnable() {
        private var targetFolder: String = _
        def init(dir: String) = targetFolder = dir

        override def run() = {

          var firstCheck = true

          while (isMonitoring) {

            if (checkFolders) {
              try {
                logger.info(s"Checking configured HDFS directory (targetFolder)...")


                val modifiedDirs = new ArrayBuffer[String]()
                modifiedDirs += targetFolder
                while (modifiedDirs.nonEmpty) {
                  //each time checking only updated folders: first find direct children of target folder that were modified
                  // then for each folder of these search for modified files and folders, repeat for the modified folders

                  val aFolder = modifiedDirs.head
                  val modifiedFiles = Map[SmartFileHandler, FileChangeType]() // these are the modified files found in folder $aFolder

                  modifiedDirs.remove(0)
                  val fs = FileSystem.get(hdfsConfig)
                  findDirModifiedDirectChilds(aFolder, fs, modifiedDirs, modifiedFiles, firstCheck)

                  //logger.debug("Closing Hd File System object fs in monitorDirChanges()")
                  //fs.close()

                  //check for file names pattern
                  validModifiedFiles.clear()
                  if(location.fileComponents != null){
                    modifiedFiles.foreach(tuple => {
                      if(MonitorUtils.isPatternMatch(MonitorUtils.getFileName(tuple._1.getFullPath), location.fileComponents.regex))
                        validModifiedFiles.append(tuple)
                      else
                        logger.warn("Smart File Consumer (Hdfs) : File {}, does not follow configured name pattern ({}), so it will be ignored - Adapter {}",
                          tuple._1.getFullPath, location.fileComponents.regex, adapterName)
                    })
                  }
                  else
                    validModifiedFiles.appendAll(modifiedFiles)

                  val orderedModifiedFiles = validModifiedFiles.map(tuple => (tuple._1, tuple._2)).toList.
                    sortWith((tuple1, tuple2) => MonitorUtils.compareFiles(tuple1._1,tuple2._1,location) < 0)

                  if (orderedModifiedFiles.nonEmpty)
                    orderedModifiedFiles.foreach(tuple => {

                      try {
                        modifiedFileCallback(tuple._1, tuple._2 == AlreadyExisting)
                      }
                      catch {
                        case e: Throwable =>
                          logger.error("Smart File Consumer (Hdfs) : Error while notifying Monitor about new file", e)
                      }

                    }
                    )

                }

              }
              catch {
                case ex: Exception => {
                  logger.error("Smart File Consumer (Hdfs Monitor) - Error while checking the folder", ex)
                }
                case ex: Throwable => {
                  logger.error("Smart File Consumer (Hdfs Monitor) - Error while checking the folder", ex)
                }
              }

              firstCheck = false

              logger.info(s"Sleepng for ${monitoringConf.waitingTimeMS} milliseconds...............................")
              Thread.sleep(monitoringConf.waitingTimeMS)
            }
          }
        }
      }
      dirMonitorthread.init(folderToWatch)
      monitorsExecutorService.execute(dirMonitorthread)
    })


  }

  private def findDirModifiedDirectChilds(parentfolder : String, hdFileSystem : FileSystem,
                                          modifiedDirs : ArrayBuffer[String], modifiedFiles : Map[SmartFileHandler, FileChangeType], isFirstCheck : Boolean){

    logger.info("HDFS Changes Monitor - listing dir " + parentfolder)
    val directChildren = getFolderContents(parentfolder, hdFileSystem).sortWith(_.getModificationTime < _.getModificationTime)
    var changeType : FileChangeType = null //new, modified

    //process each file reported by FS cache.
    directChildren.foreach(fileStatus => {
      var isChanged = false
      val uniquePath = fileStatus.getPath.toString
      if (processedFilesMap.contains(uniquePath))
        logger.info("Smart File Consumer (Sftp) - File {} already processed, ignoring - Adapter {}", uniquePath, adapterName)
      else {
        if (!filesStatusMap.contains(uniquePath)) {
          //path is new
          isChanged = true
          changeType = if (isFirstCheck) AlreadyExisting else New

          val fileEntry = makeFileEntry(fileStatus, parentfolder)
          filesStatusMap.put(uniquePath, fileEntry)
          if (fileStatus.isDirectory)
            modifiedDirs += uniquePath
        }
        else {
          val storedEntry = filesStatusMap.get(uniquePath).get
          if (fileStatus.getModificationTime > storedEntry.lastModificationTime) {
            //file has been modified
            storedEntry.lastModificationTime = fileStatus.getModificationTime
            isChanged = true

            changeType = Modified
          }
        }

        //TODO : this method to find changed folders is not working as expected. so for now check all dirs
        if (fileStatus.isDirectory)
          modifiedDirs += uniquePath

        if (isChanged) {
          if (fileStatus.isDirectory) {

          }
          else {
            if (changeType == New || changeType == AlreadyExisting) {
              val fileHandler = new HdfsFileHandler(uniquePath, connectionConf)
              modifiedFiles.put(fileHandler, changeType)
            }
          }
        }
      }
    }
    )


    val deletedFiles = new ArrayBuffer[String]()
    /*filesStatusMap.keys.filter(filePath => isDirectParentDir(filePath, parentfolder)).foreach(pathKey =>
      if(!directChildren.exists(fileStatus => fileStatus.getPath.toString.equals(pathKey))){ //key that is no more in the folder => file/folder deleted
        deletedFiles += pathKey
      }
    )*/
    filesStatusMap.values.foreach(fileEntry =>{
      //logger.debug("checking if file {} is deleted, parent is {}. comparing to folder {}",
        //fileEntry.name, fileEntry.parent, parentfolder)
      if(isDirectParentDir(fileEntry, parentfolder)){
        if(!directChildren.exists(fileStatus => fileStatus.getPath.toString.equals(fileEntry.name))) {
          //key that is no more in the folder => file/folder deleted
          logger.debug("file {} is no more under folder  {}, will be deleted from map", fileEntry.name, parentfolder)
          deletedFiles += fileEntry.name
        }
        else {
          //logger.debug("file {} is still under folder  {}", fileEntry.name, fileEntry.parent)
        }
      }
    })
    deletedFiles.foreach(f => filesStatusMap.remove(f))
  }


  private def isDirectParentDir(fileEntry : HdfsFileEntry, dir : String) : Boolean = {
    try{

      //logger.debug("isDirectParentDir - comparing {} to {}", fileEntry.parent, dir)
      fileEntry.parent.toString.equals(dir)
    }
    catch{
      case ex : Exception => false
      case ex : Throwable => false
    }
  }

  private def makeFileEntry(fileStatus : FileStatus, parentfolder : String) : HdfsFileEntry = {

    val newFile = new HdfsFileEntry()
    newFile.lastReportedSize = fileStatus.getLen
    newFile.name = fileStatus.getPath.toString
    newFile.lastModificationTime = fileStatus.getModificationTime
    newFile.parent = parentfolder
    newFile
  }

  def stopMonitoring(){
    isMonitoring = false
  }
}