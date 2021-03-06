/*
 * Copyright 2015 ligaDATA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ligadata.OutputAdapters

import org.apache.avro.generic.GenericRecord
import org.apache.logging.log4j.{ Logger, LogManager }
import java.io._
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.zip.{ ZipException, GZIPOutputStream }
import java.nio.file.{ Paths, Files }
import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import com.ligadata.KamanjaBase.{ ContainerInterface, TransactionContext, NodeContext }
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.AdaptersConfiguration.SmartFileProducerConfiguration
import com.ligadata.Exceptions.{ UnsupportedOperationException, FatalAdapterException }
import com.ligadata.HeartBeat.{ Monitorable, MonitorComponentInfo }
import org.json4s.jackson.Serialization
import org.apache.hadoop.hdfs.DFSOutputStream
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream.SyncFlag
import org.apache.hadoop.fs.{ FileSystem, FSDataOutputStream, Path }
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation
import org.apache.commons.lang3.time.FastDateFormat
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.compressors.CompressorOutputStream
import parquet.avro.AvroParquetWriter
import org.apache.avro.generic.GenericRecordBuilder
import parquet.hadoop.metadata.CompressionCodecName
import parquet.schema.MessageTypeParser
import scala.collection.mutable.ArrayBuffer
import scala.actors.threadpool.{ ExecutorService, Executors }
import com.ligadata.VelocityMetrics._

import parquet.hadoop._
import parquet.hadoop.api.WriteSupport

object SmartFileProducer extends OutputAdapterFactory {
  val ADAPTER_DESCRIPTION = "Smart File Output Adapter"

  val nullFlagsFieldDefaultName = "kamanja_system_null_flags"

  def CreateOutputAdapter(inputConfig: AdapterConfiguration, nodeContext: NodeContext): OutputAdapter = new SmartFileProducer(inputConfig, nodeContext)
}

object PartitionFileFactory {
  def createPartitionFile(fc: SmartFileProducerConfiguration, key: String, avroSchema: Option[String], ignoreFields: Array[String]): PartitionFile = {
    if (fc.isParquet) new ParquetPartitionFile(fc, key, avroSchema.get, ignoreFields)
    else new StreamPartitionFile(fc, key)

  }

}

object SendStatus {
  val SUCCESS = 0
  //val REOPEN = 1
  val FAILURE = 2
  val NO_DATA = 3
}

/*case class PartitionFile(key: String, name: String, outStream: OutputStream, parquetWriter : ParquetWriter[Array[Any]],
                         var records: Long, var size: Long, var streamBuffer: ArrayBuffer[Byte], var parquetBuffer: ArrayBuffer[ContainerInterface],
                         var recordsInBuffer: Long, var flushBufferSize: Long)*/

case class PartitionStream(val compressStream: OutputStream, val originalStream: Any) extends OutputStream {
  override def close() = {
    compressStream.close();
  }

  override def flush() = {
    compressStream.flush();
    if (originalStream.isInstanceOf[FSDataOutputStream]) {
      val hdfsOs = originalStream.asInstanceOf[FSDataOutputStream]
      hdfsOs.getWrappedStream.asInstanceOf[DFSOutputStream].hsync(java.util.EnumSet.of(SyncFlag.UPDATE_LENGTH))
    }
  }

  override def write(b: Array[Byte]) = {
    compressStream.write(b)
  }

  override def write(b: Array[Byte], off: Int, len: Int) = {
    compressStream.write(b, off, len)
  }

  override def write(b: Int) = {
    compressStream.write(b)
  }
}

class OutputStreamWriter {
  private[this] val LOG = LogManager.getLogger(getClass);
  private var ugi: UserGroupInformation = null

  private var MAX_RETRIES = 3
  private val FAIL_WAIT = 2000

  private def trimFileFromLocalFileSystem(fileName: String): String = {
    if (fileName.startsWith("file://"))
      return fileName.substring("file://".length() - 1)
    fileName
  }

  private def isFsFileExists(fc: SmartFileProducerConfiguration, fileName: String): Boolean = {
    try {
      val file = new File(trimFileFromLocalFileSystem(fileName))
      return (file.exists())
    } catch {
      case e: Throwable => {
        LOG.warn("Failed to check file exists for file " + fileName, e)
      }
    }
    return false
  }

  private def isHdfsFileExists(fc: SmartFileProducerConfiguration, fileName: String): Boolean = {
    try {
      var hdfsConf: Configuration = new Configuration();
      if (fc.kerberos != null) {
        hdfsConf.set("hadoop.security.authentication", "kerberos")
        UserGroupInformation.setConfiguration(hdfsConf)
        ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(fc.kerberos.principal, fc.kerberos.keytab);
      }

      if (fc.hadoopConfig != null && !fc.hadoopConfig.isEmpty) {
        fc.hadoopConfig.foreach(conf => {
          hdfsConf.set(conf._1, conf._2)
        })
      }

      var uri: URI = URI.create(fileName)
      var path: Path = new Path(uri)
      var fs: FileSystem = FileSystem.get(uri, hdfsConf);

      return (fs.exists(path))
    } catch {
      case e: Throwable => {
        LOG.warn("Failed to check file exists for file " + fileName, e)
      }
    }
    return false
  }

  private def removeFsFile(fc: SmartFileProducerConfiguration, fileName: String): Unit = {
    try {
      val file = new File(trimFileFromLocalFileSystem(fileName))
      if (file.exists()) {
        file.delete()
      }
    } catch {
      case e: Throwable => {
        LOG.error("Failed to remove file " + fileName, e)
      }
    }
  }

  private def removeHdfsFile(fc: SmartFileProducerConfiguration, fileName: String): Unit = {
    try {
      var hdfsConf: Configuration = new Configuration();
      if (fc.kerberos != null) {
        hdfsConf.set("hadoop.security.authentication", "kerberos")
        UserGroupInformation.setConfiguration(hdfsConf)
        ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(fc.kerberos.principal, fc.kerberos.keytab);
      }

      if (fc.hadoopConfig != null && !fc.hadoopConfig.isEmpty) {
        fc.hadoopConfig.foreach(conf => {
          hdfsConf.set(conf._1, conf._2)
        })
      }

      var uri: URI = URI.create(fileName)
      var path: Path = new Path(uri)
      var fs: FileSystem = FileSystem.get(uri, hdfsConf);

      if (fs.exists(path)) {
        fs.delete(path, true)
      }
    } catch {
      case e: Throwable => {
        LOG.error("Failed to remove file " + fileName, e)
      }
    }
  }

  final def isFileExists(fc: SmartFileProducerConfiguration, fileName: String): Boolean = if (fc.uri.startsWith("hdfs://")) isHdfsFileExists(fc, fileName) else isFsFileExists(fc, fileName)

  final def removeFile(fc: SmartFileProducerConfiguration, fileName: String): Unit = if (fc.uri.startsWith("hdfs://")) removeHdfsFile(fc, fileName) else removeFsFile(fc, fileName)

  private def openFsFile(fc: SmartFileProducerConfiguration, fileName: String, canAppend: Boolean): OutputStream = {
    var os: OutputStream = null
    var numOfRetries = 0
    while (os == null) {
      try {
        val file = new File(trimFileFromLocalFileSystem(fileName))
        file.getParentFile().mkdirs();
        os = new FileOutputStream(file, canAppend)
      } catch {
        case fio: IOException => {
          LOG.warn("Smart File Producer " + fc.Name + ": Unable to create a file destination " + fc.uri + " due to an IOException", fio)
          if (numOfRetries > MAX_RETRIES) {
            LOG.error("Smart File Producer " + fc.Name + ":Unable to create a file destination after " + MAX_RETRIES + " tries.  Aborting.")
            throw FatalAdapterException("Unable to open connection to specified file after " + MAX_RETRIES + " retries", fio)
          }
          numOfRetries += 1
          LOG.warn("Smart File Producer " + fc.Name + ": Retyring " + numOfRetries + "/" + MAX_RETRIES)
          Thread.sleep(FAIL_WAIT)
        }
        case e: Exception => {
          throw FatalAdapterException("Unable to open connection to specified file ", e)
        }
      }
    }
    return os;
  }

  private def openHdfsFile(fc: SmartFileProducerConfiguration, fileName: String, canAppend: Boolean): OutputStream = {
    var os: OutputStream = null
    var numOfRetries = 0
    while (os == null) {
      try {
        var hdfsConf: Configuration = new Configuration();
        if (fc.kerberos != null) {
          hdfsConf.set("hadoop.security.authentication", "kerberos")
          UserGroupInformation.setConfiguration(hdfsConf)
          ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(fc.kerberos.principal, fc.kerberos.keytab);
        }

        if (fc.hadoopConfig != null && !fc.hadoopConfig.isEmpty) {
          fc.hadoopConfig.foreach(conf => {
            hdfsConf.set(conf._1, conf._2)
          })
        }

        var uri: URI = URI.create(fileName)
        var path: Path = new Path(uri)
        var fs: FileSystem = FileSystem.get(uri, hdfsConf);

        if (fs.exists(path)) {
          if (!canAppend) {
            throw UnsupportedOperationException("File %s exists but append is not permitted".format(fileName), null)
          }
          LOG.info("Smart File Producer " + fc.Name + "(" + this + ") : Loading existing file " + uri)
          os = fs.append(path)
        } else {
          LOG.info("Smart File Producer " + fc.Name + "(" + this + ") : Creating new file " + uri);
          os = fs.create(path);
        }
      } catch {
        case fio: IOException => {
          LOG.warn("Smart File Producer " + fc.Name + ": Unable to create a file destination " + fc.uri + " due to an IOException", fio)
          if (numOfRetries > MAX_RETRIES) {
            LOG.error("Smart File Producer " + fc.Name + ":Unable to create a file destination after " + MAX_RETRIES + " tries.  Aborting.")
            throw FatalAdapterException("Unable to open connection to specified file after " + MAX_RETRIES + " retries", fio)
          }
          numOfRetries += 1
          LOG.warn("Smart File Producer " + fc.Name + ": Retyring " + numOfRetries + "/" + MAX_RETRIES)
          Thread.sleep(FAIL_WAIT)
        }
        case e: Exception => {
          throw FatalAdapterException("Unable to open connection to specified file ", e)
        }
      }
    }
    return os;
  }

  final def openFile(fc: SmartFileProducerConfiguration, fileName: String, canAppend: Boolean = true): OutputStream = if (fc.uri.startsWith("hdfs://")) openHdfsFile(fc, fileName, canAppend) else openFsFile(fc, fileName, canAppend)

  def getFileSize(fc: SmartFileProducerConfiguration, fileName: String): Long = if (fc.uri.startsWith("hdfs://")) getHdfsFileSize(fc, fileName) else getFSFileSize(fc, fileName)

  def getHdfsFileSize(fc: SmartFileProducerConfiguration, fileName: String): Long = {
    try {
      val hdfsConf: Configuration = new Configuration()
      if (fc.kerberos != null) {
        hdfsConf.set("hadoop.security.authentication", "kerberos")
        UserGroupInformation.setConfiguration(hdfsConf)
        ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(fc.kerberos.principal, fc.kerberos.keytab);
      }

      if (fc.hadoopConfig != null && !fc.hadoopConfig.isEmpty) {
        fc.hadoopConfig.foreach(conf => {
          hdfsConf.set(conf._1, conf._2)
        })
      }

      val uri: URI = URI.create(fileName)
      val path: Path = new Path(uri)
      val fs: FileSystem = FileSystem.get(uri, hdfsConf)
      fs.getFileStatus(path).getLen
    } catch {
      case ex: Throwable =>
        LOG.warn("", ex)
        0
    }
  }

  def getFSFileSize(fc: SmartFileProducerConfiguration, fileName: String): Long = {
    val file = new File(trimFileFromLocalFileSystem(fileName))
    file.length()
  }

  def mkdirs(fc: SmartFileProducerConfiguration, dirPath: String): Boolean = {
    if (fc.uri.startsWith("hdfs://")) mkdirsHDFS(fc, dirPath)
    else mkdirsFS(fc, dirPath)
  }

  def mkdirsFS(fc: SmartFileProducerConfiguration, dirPath: String): Boolean = {
    LOG.info("OutputStreamWriter - mkdirs for path " + dirPath)
    try {
      new File(dirPath).mkdirs()
    } catch {
      case e: Throwable =>
        LOG.error("OutputStreamWriter - Error while creating fs path " + dirPath, e)
        false
    }
  }

  def mkdirsHDFS(fc: SmartFileProducerConfiguration, dirPath: String): Boolean = {
    LOG.info("OutputStreamWriter - mkdirs for path " + dirPath)
    try {
      val hdfsConf: Configuration = new Configuration()
      if (fc.kerberos != null) {
        hdfsConf.set("hadoop.security.authentication", "kerberos")
        UserGroupInformation.setConfiguration(hdfsConf)
        ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(fc.kerberos.principal, fc.kerberos.keytab);
      }

      if (fc.hadoopConfig != null && !fc.hadoopConfig.isEmpty) {
        fc.hadoopConfig.foreach(conf => {
          hdfsConf.set(conf._1, conf._2)
        })
      }

      val uri: URI = URI.create(dirPath)
      val path: Path = new Path(uri)
      val fs: FileSystem = FileSystem.get(uri, hdfsConf)
      fs.mkdirs(path)
    } catch {
      case ex: Throwable =>
        LOG.warn("", ex)
        false
    }
  }

  /*
    def hasCompressedFlag(fc: SmartFileProducerConfiguration): Boolean = {
      return (fc.compressionString != null)
    }

    def hasValidCompression(fc: SmartFileProducerConfiguration): Boolean = {
      val compress = (fc.compressionString != null)
      if (compress) {
        if (CompressorStreamFactory.BZIP2.equalsIgnoreCase(fc.compressionString) ||
          CompressorStreamFactory.GZIP.equalsIgnoreCase(fc.compressionString) ||
          CompressorStreamFactory.XZ.equalsIgnoreCase(fc.compressionString))
          return true
      }

      return false
    }
  */
}

class SmartFileProducer(val inputConfig: AdapterConfiguration, val nodeContext: NodeContext) extends OutputStreamWriter with OutputAdapter {

  //new ReflectionUtil().setParquetMinRecCount(1)

  private[this] val LOG = LogManager.getLogger(getClass);

  private val _reent_lock = new ReentrantReadWriteLock(true)

  private def ReadLock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.readLock().lock()
  }

  private def ReadUnlock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.readLock().unlock()
  }

  private def WriteLock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.writeLock().lock()
  }

  private def WriteUnlock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.writeLock().unlock()
  }

  private def parsePartitionFormat(formatStr: String): (String, List[Any]) = {
    var partitionFormatString: String = null
    var partitionFormatObjects: List[Any] = null
    if (formatStr != null) {
      val partitionVariable = "\\$\\{([^\\}]+)\\}".r
      partitionFormatObjects = partitionVariable.findAllMatchIn(formatStr).map(x => try {
        val spec = x.group(1).split(":");
        if (spec.length > 1 && spec(0).equalsIgnoreCase("time")) {
          val fmt = FastDateFormat.getInstance(spec(1), TimeZone.getTimeZone("UTC"))
          fmt
        } else if (spec.length > 1) {
          spec(1)
        } else {
          spec(0)
        }
      } catch {
        case e: Exception => {
          throw FatalAdapterException(x.group(1) + " is not a valid date format string.", e)
        }
      }).toList
      partitionFormatString = partitionVariable.replaceAllIn(formatStr, "%s")
    }

    (partitionFormatString, partitionFormatObjects)
  }

  private[this] val fc = SmartFileProducerConfiguration.getAdapterConfig(nodeContext, inputConfig)
  private val partitionStreams: collection.mutable.Map[String, PartitionFile] = collection.mutable.Map[String, PartitionFile]()
  private val extensions: scala.collection.immutable.Map[String, String] = Map(
    (CompressorStreamFactory.BZIP2, ".bz2"),
    (CompressorStreamFactory.GZIP, ".gz"),
    (CompressorStreamFactory.XZ, ".xz"))

  private var shutDown: Boolean = false
  private val nodeId = if (nodeContext == null || nodeContext.getEnvCtxt() == null) "1" else nodeContext.getEnvCtxt().getNodeId()
  private val FAIL_WAIT = 2000
  private var MAX_RETRIES = 3
  private var startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var metrics: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map[String, Any]()
  metrics("MessagesProcessed") = new AtomicLong(0)

  //calling the velocity metrics instances
  getVelocityInstances = vm.getMsgVelocityInstances(VMFactory, Category, inputConfig.Name, inputConfig.fullAdapterConfig, nodeContext)

  var getFileVelocityInstances = vm.getFileVelocityInstances(VMFactory, Category, inputConfig.Name, inputConfig.fullAdapterConfig, nodeContext)

  //private val avroSchemasMap = collection.mutable.Map[String, org.apache.avro.Schema]() //message -> schema
  private val writeSupportsMap = collection.mutable.Map[String, ParquetWriteSupport]() //message -> support

  if (fc.uri.startsWith("file://"))
    fc.uri = fc.uri.substring("file://".length() - 1)

  val isParquet = fc.isParquet
  val parquetCompression = if (fc.parquetCompression == null || fc.parquetCompression.length == 0) null else CompressionCodecName.valueOf(fc.parquetCompression)
  if (isParquet)
    LOG.info(">>>>>>>>> using parquet with compression: " + parquetCompression)
  else LOG.info(">>>>>>>>> compression: " + fc.compressionString)

  val defaultExtension = if (isParquet) "" else fc.compressionString

  val compress = (fc.compressionString != null && !isParquet)
  if (compress) {
    if (CompressorStreamFactory.BZIP2.equalsIgnoreCase(fc.compressionString) ||
      CompressorStreamFactory.GZIP.equalsIgnoreCase(fc.compressionString) ||
      CompressorStreamFactory.XZ.equalsIgnoreCase(fc.compressionString) ||
      isParquet)
      LOG.debug("Smart File Producer " + fc.Name + " Using compression: " + fc.compressionString)
    else
      throw FatalAdapterException("Unsupported compression type " + fc.compressionString + " for Smart File Producer: " + fc.Name, new Exception("Invalid Parameters"))
  }

  if (fc.partitionFormat == null && fc.timePartitionFormat != null) {
    // for backward compatibility if timePartitionFormat given and not partitionFormat then use it
    fc.partitionFormat = fc.timePartitionFormat.replace("${", "${time:")
  }

  val (glbPartitionFormatString, glbPartitionFormatObjects) = parsePartitionFormat(fc.partitionFormat)
  for ((typeName, tlcfg) <- fc.typeLevelConfig) {
    if (tlcfg != null) {
      val (pfs, pfo) = parsePartitionFormat(tlcfg.partitionFormat)
      tlcfg.partitionFormatString = pfs
      tlcfg.partitionFormatObjects = pfo
    }
  }

  private var rolloverExecutor: ExecutorService = Executors.newFixedThreadPool(1)
  private val producerLock = this

  private def isTimeToRollover(dt: Long): Boolean = {
    (nextRolloverTime > 0 && dt > nextRolloverTime)
  }

  var nextRolloverTime: Long = 0
  if (fc.rolloverInterval > 0) {
    LOG.info("Smart File Producer " + fc.Name + ": File rollover is configured. Will rollover files every " + fc.rolloverInterval + " minutes.")
    val dt = System.currentTimeMillis()
    nextRolloverTime = (dt - (dt % (fc.rolloverInterval * 60 * 1000))) + (fc.rolloverInterval * 60 * 1000)

    rolloverExecutor.execute(new Runnable() {
      override def run(): Unit = {
        while (!shutDown) {
          try {
            val dt = System.currentTimeMillis
            if (isTimeToRollover(dt)) {
              producerLock.synchronized {
                if (isTimeToRollover(dt)) {
                  rolloverFiles()
                  nextRolloverTime = (dt - (dt % (fc.rolloverInterval * 60 * 1000))) + (fc.rolloverInterval * 60 * 1000)
                }
              }
            }
          } catch {
            case e: Throwable => {
              if (!shutDown)
                logger.warn("Failed to rollover.", e)
            }
          }

          try {
            Thread.sleep(1000)
          } catch {
            case e: Throwable => {}
          }
        }
      }
    })
  }

  var bufferFlusher: Thread = null
  if (fc.flushBufferInterval > 0) {
    LOG.info("Smart File Producer " + fc.Name + ": File buffer is configured. Will flush buffer every " + fc.flushBufferInterval + " milli seconds.")
    bufferFlusher = new Thread {
      override def run {
        LOG.info("Smart File Producer " + fc.Name + ": writing all buffers.")
        try {
          Thread.sleep(fc.flushBufferInterval)
        } catch {
          case e: Exception => {}
        }

        while (!shutDown) {
          WriteLock(_reent_lock)
          try {
            for ((name, pf) <- partitionStreams) {
              if (pf != null) {
                LOG.debug("Smart File Producer " + fc.Name + ": writing buffer for file at " + name)
                try {
                  pf.synchronized {
                    pf.flush()
                  }
                } catch {
                  case e: Exception => LOG.debug("Smart File Producer " + fc.Name + ": Error closing file: ", e)
                }
              }
            }
          } finally {
            WriteUnlock(_reent_lock)
          }

          try {
            Thread.sleep(fc.flushBufferInterval)
          } catch {
            case e: Exception => {}
          }
        }
      }
    }

    bufferFlusher.start
  }

  private def rolloverFiles() = {
    LOG.info("Smart File Producer " + fc.Name + ": Rolling over files.")

    WriteLock(_reent_lock)
    try {
      for ((name, pf) <- partitionStreams) {
        if (pf != null) {
          LOG.info("Smart File Producer " + fc.Name + ": Rolling file at " + name)
          try {
            pf.synchronized {
              pf.flush()
              pf.close()
            }
          } catch {
            case e: Exception => LOG.debug("Smart File Producer " + fc.Name + ": Error closing file: ", e)
          }
        }
      }
      partitionStreams.clear()
    } finally {
      WriteUnlock(_reent_lock)
    }
  }

  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    implicit val formats = org.json4s.DefaultFormats
    return new MonitorComponentInfo(AdapterConfiguration.TYPE_OUTPUT, fc.Name, SmartFileProducer.ADAPTER_DESCRIPTION, startTime, lastSeen, Serialization.write(metrics).toString)
  }

  override def getComponentSimpleStats: String = {
    Category + "/" + getAdapterName + "/evtCnt->" + metrics("MessagesProcessed").asInstanceOf[AtomicLong].get
  }

  private def getPartionFile(record: ContainerInterface): PartitionFile = {
    var typeName = record.getFullTypeName();
    val dateTime = record.getTimePartitionData()
    val tlcfg = fc.typeLevelConfig.getOrElse(typeName, null);
    var fileBufferSize = fc.flushBufferSize;
    var partitionFormatString = glbPartitionFormatString
    var partitionFormatObjects = glbPartitionFormatObjects
    if (tlcfg != null) {
      fileBufferSize = tlcfg.flushBufferSize
      partitionFormatString = tlcfg.partitionFormatString
      partitionFormatObjects = tlcfg.partitionFormatObjects
    }

    var key = ""
    //if we have a subdir name in type level config correspnding to this msg, use it
    //else name of the msg will be used as subdir
    if (tlcfg != null && tlcfg.subDirName != null && tlcfg.subDirName.trim.length > 0) {
      key = if (tlcfg.subDirName.trim.endsWith("/"))
        tlcfg.subDirName.trim.substring(0, tlcfg.subDirName.trim.length - 1)
      else tlcfg.subDirName
    } else {
      key = record.getTypeName()
      if (fc.useTypeFullNameForPartition) {
        key = if (fc.replaceSeparator) typeName.replace(".", fc.separatorCharForTypeName) else typeName
      }
    }

    val pk = record.getPartitionKey()
    var bucket: Int = 0
    if (pk != null && pk.length > 0 && fc.partitionBuckets > 1) {
      LOG.info("Smart File Producer :" + fc.Name + " : In getPartionFile pk for the record - [" + pk.mkString(",") + "]")
      bucket = pk.mkString("").hashCode() % fc.partitionBuckets
    }

    if (dateTime > 0 && partitionFormatString != null && partitionFormatObjects != null) {
      LOG.info("Smart File Producer :" + fc.Name + " : In getPartionFile time partion data for the record - [" + dateTime + "]")
      val dtTm = new java.util.Date(dateTime)
      val values = partitionFormatObjects.map(fmt => {
        if (fmt.isInstanceOf[FastDateFormat])
          fmt.asInstanceOf[FastDateFormat].format(dtTm)
        else
          record.getOrElse(fmt.asInstanceOf[String], "").toString
      })
      key = key + "/" + partitionFormatString.format(values: _*)
    }

    val path = key
    key = key + bucket

    if (LOG.isInfoEnabled()) {
      LOG.info("Smart File Producer :" + fc.Name + " : In getPartionFile key for the record - [" + key + "]")
      LOG.info("Smart File Producer :" + fc.Name + " : In getPartionFile key in partitionStreams - [" + partitionStreams.keys.mkString(",") + "]")
    }

    var partKey: PartitionFile = null
    ReadLock(_reent_lock)
    partKey = partitionStreams.getOrElse(key, null)
    ReadUnlock(_reent_lock)

    if (partKey == null) {
      WriteLock(_reent_lock)
      try {
        LOG.info("Smart File Producer :" + fc.Name + " : In getPartionFile key[" + key + "] not found")
        // need to check again to make sure other threads did not update
        partKey = partitionStreams.getOrElse(key, null)
        if (partKey == null) {
          LOG.info("Smart File Producer :" + fc.Name + " : In getPartionFile key[" + key + "] still not found will add")
          // need to check again
          val dt = if (nextRolloverTime > 0) nextRolloverTime - (fc.rolloverInterval * 60 * 1000) else System.currentTimeMillis
          val ts = new java.text.SimpleDateFormat("yyyyMMdd'T'HHmm").format(new java.util.Date(dt))
          val initialFileName = "%s/%s/%s%s-%d-%s.dat%s".format(fc.uri, path, fc.fileNamePrefix, nodeId, bucket, ts, extensions.getOrElse(defaultExtension, ""))
          val fileName =
            if (isParquet) {
              //cannot use already existing parquet file
              val filePathTokens = initialFileName.split("/")
              val tmpFileDir = filePathTokens.take(filePathTokens.length - 1).mkString("/")
              val tmpFileName = tmpFileDir + "/." + filePathTokens(filePathTokens.length - 1)

              logger.info("checking if archive files already exists: {} or {}", initialFileName, tmpFileName)
              if (isFileExists(fc, initialFileName) || isFileExists(fc, tmpFileName)) {
                val newDt = System.currentTimeMillis
                nextRolloverTime = newDt + (fc.rolloverInterval * 60 * 1000)
                val newTs = new java.text.SimpleDateFormat("yyyyMMdd'T'HHmm").format(new java.util.Date(newDt))
                "%s/%s/%s%s-%d-%s.dat%s".format(fc.uri, path, fc.fileNamePrefix, nodeId, bucket, newTs, extensions.getOrElse(defaultExtension, ""))
              } else initialFileName
            } else initialFileName

          if (isParquet) {
            //TODO : is it better to cache parsed schemas in a map ?
            /*if(!writeSupportsMap.contains(record.getFullTypeName)){
              LOG.info(">>>>>>>>>>>>>>>>>> Avro schema : " + record.getAvroSchema)
              val parquetSchema = Utils.getParquetSchema(record.getAvroSchema)
              LOG.info(">>>>>>>>>>>>>>>>>> parquet schema : " + parquetSchema.toString)

              val writeSupport = new ParquetWriteSupport(parquetSchema)
              writeSupportsMap.put(record.getFullTypeName, writeSupport)
            }*/
          }

          // By default we are ignoring partition key & null flags
          var ignorePartitionKey = false;
          var ignoreNullFlags = true;

          val ignoreFields = ArrayBuffer[String]()

          if (fc.otherConfig.contains("IgnorePartitionKey"))
            ignorePartitionKey = fc.otherConfig.getOrElse("IgnorePartitionKey", "true").toString.trim.toBoolean

          if (ignorePartitionKey && record != null)
            ignoreFields ++= record.getPartitionKeyNames

          if (fc.otherConfig.contains("IgnoreNullFlags"))
            ignoreNullFlags = fc.otherConfig.getOrElse("IgnoreNullFlags", "true").toString.trim.toBoolean

          if (ignoreNullFlags) {
            val ignoreNullFlags =
              if (fc.otherConfig.contains("NullFlagsFieldName"))
                fc.otherConfig.getOrElse("NullFlagsFieldName", SmartFileProducer.nullFlagsFieldDefaultName).toString.trim
              else SmartFileProducer.nullFlagsFieldDefaultName

            ignoreFields += ignoreNullFlags
          }

          if (fc.otherConfig.contains("IgnoreFields")) {
            ignoreFields ++= fc.otherConfig.getOrElse("IgnoreFields", "").toString.trim.split(",").map(fld => fld.trim).filter(fld => fld.size > 0)
          }

          partKey = //new PartitionFile(key, fileName, new PartitionStream(os, originalStream), parquetWriter, 0, 0, null, buffer, 0, fileBufferSize)
            PartitionFileFactory.createPartitionFile(fc, key, Some(record.getAvroSchema), ignoreFields.toSet.toArray)
          partKey.init(fileName, fileBufferSize)

          LOG.info("Smart File Producer :" + fc.Name + " : In getPartionFile adding key - [" + key + "]")
          partitionStreams(key) = partKey
        }
      } finally {
        WriteUnlock(_reent_lock) // release write lock
      }
    }

    return partKey
  }

  /*private def reopenPartitionFile(pf: PartitionFile): PartitionFile = {//only for stream, not applicable for parquet
    LOG.info("Smart File Producer :" + fc.Name + " : In PartitionFile key - [" + pf.getKey + "]")
    WriteLock(_reent_lock)
    try {
      partitionStreams.remove(pf.getKey)

      val newPf = PartitionFileFactory.createPartitionFile(fc, pf.getKey)
      newPf.init(pf.getFilePath, pf.getFlushBufferSize)
      partitionStreams(pf.getKey) = new PartitionFile(pf.key, pf.name, new PartitionStream(os, originalStream), null, pf.records, pf.size, pf.streamBuffer, null, pf.recordsInBuffer, pf.flushBufferSize)

      return partitionStreams(pf.getKey)
    } finally {
      WriteUnlock(_reent_lock)
    }
  }*/

  override def send(message: Array[Array[Byte]], partitionKey: Array[Array[Byte]]): Unit = {
    // Not implemented yet
  }

  // Locking before we write into file
  // To send an array of messages. messages.size should be same as partKeys.size
  override def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): Unit = {
    if (outputContainers.size == 0) return

    val dt = System.currentTimeMillis
    lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(dt))
    if (isTimeToRollover(dt)) {
      this.synchronized {
        if (isTimeToRollover(dt)) {
          rolloverFiles()
          nextRolloverTime = (dt - (dt % (fc.rolloverInterval * 60 * 1000))) + (fc.rolloverInterval * 60 * 1000)
        }
      }
    }
    var filename: String = ""
    try {
      // Op is not atomic
      var idx = 0
      val nodeId = nodeContext.getEnvCtxt().getNodeId()
      outputContainers.foreach(record => {

        val pf = getPartionFile(record);
        filename = pf.getFilePath
        pf.synchronized {
          val status = pf.send(tnxCtxt, record, this)

          if (status == SendStatus.SUCCESS) {
            metrics("MessagesProcessed").asInstanceOf[AtomicLong].incrementAndGet()
            /**VelocityMetrics****/
            if (record != null) {
              getOAVelocityMetrics(this, record, true)
            }
            getFileVelocityMetrics(this, filename, true)
          } else {
            
            /**VelocityMetrics****/
            if (record != null) {
              getOAVelocityMetrics(this, record, false)
            }

            getFileVelocityMetrics(this, filename, false)
          }
        }

      })
    } catch {
      case e: Exception => {
        LOG.error("Smart File Producer " + fc.Name + ": Failed to send", e)
        getFileVelocityMetrics(this, filename, false)
        throw FatalAdapterException("Unable to send message", e)
      }
    }
  }

  override def Shutdown(): Unit = {
    WriteLock(_reent_lock)
    try {
      shutDown = true

      if (rolloverExecutor != null)
        rolloverExecutor.shutdownNow

      if (bufferFlusher != null) {
        bufferFlusher.interrupt
        bufferFlusher = null
      }

      for ((name, pf) <- partitionStreams) {
        if (pf != null) {
          LOG.info("Smart File Producer " + fc.Name + ": closing file at " + name)
          pf.synchronized {
            pf.flush()
            pf.close()
          }
        }
      }
      partitionStreams.clear()
    } finally {
      WriteUnlock(_reent_lock)
    }
  }

  private def getOAVelocityMetrics(output: OutputAdapter, message: ContainerInterface, processed: Boolean) = {
    val vmInstances = output.getVelocityInstances
    if (vmInstances != null && vmInstances.length > 0) {
      for (i <- 0 until vmInstances.length) {
        output.vm.incrementIAVelocityMetrics(vmInstances(i), message, processed)
      }
    }
  }

  /* Get Velocity Metrics for Output Adapter   */
  private def getFileVelocityMetrics(output: OutputAdapter, fileName: String, processed: Boolean) = {
    val vmInstances = this.getFileVelocityInstances
    if (vmInstances != null && vmInstances.length > 0) {
      for (i <- 0 until vmInstances.length) {
        output.vm.incrementFileVMetrics(vmInstances(i), fileName, processed)
      }
    }
  }
}

