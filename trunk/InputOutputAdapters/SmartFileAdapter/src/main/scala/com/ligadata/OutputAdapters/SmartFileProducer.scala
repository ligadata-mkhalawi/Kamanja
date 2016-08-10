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

import org.apache.logging.log4j.{ Logger, LogManager }
import java.io._
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.zip.{ ZipException, GZIPOutputStream }
import java.nio.file.{ Paths, Files }
import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import com.ligadata.KamanjaBase.{ContainerInterface, TransactionContext, NodeContext}
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.AdaptersConfiguration.SmartFileProducerConfiguration
import com.ligadata.Exceptions.{ FatalAdapterException }
import com.ligadata.HeartBeat.{ Monitorable, MonitorComponentInfo }
import org.json4s.jackson.Serialization
import org.apache.hadoop.fs.{ FileSystem, FSDataOutputStream, Path }
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation
import org.apache.commons.compress.compressors.CompressorStreamFactory

object SmartFileProducer extends OutputAdapterFactory {
  val ADAPTER_DESCRIPTION = "Smart File Output Adapter"
  def CreateOutputAdapter(inputConfig: AdapterConfiguration, nodeContext: NodeContext): OutputAdapter = new SmartFileProducer(inputConfig, nodeContext)
}

case class PartitionFile(key: String, name: String, outStream: OutputStream, var records: Long, var size: Long)

class SmartFileProducer(val inputConfig: AdapterConfiguration, val nodeContext: NodeContext) extends OutputAdapter {
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

  private[this] val fc = SmartFileProducerConfiguration.getAdapterConfig(inputConfig)
  private var ugi: UserGroupInformation = null
  private var partitionStreams: collection.mutable.Map[String, PartitionFile] = collection.mutable.Map[String,PartitionFile]()
  private var extensions: scala.collection.immutable.Map[String, String] = Map(
      (CompressorStreamFactory.BZIP2, ".bz2"),
      (CompressorStreamFactory.GZIP, ".gz"),
      (CompressorStreamFactory.XZ, ".xz"))
      
  
  private var shutDown:Boolean = false
  private val nodeId = if(nodeContext==null || nodeContext.getEnvCtxt()==null) "1" else nodeContext.getEnvCtxt().getNodeId()
  private val FAIL_WAIT = 2000
  private var numOfRetries = 0
  private var MAX_RETRIES = 3
  private var startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var metrics: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map[String, Any]()
  metrics("MessagesProcessed") = new AtomicLong(0)

  if(fc.uri.startsWith("file://"))
    fc.uri = fc.uri.substring("file://".length()-1)
    
  val compress = (fc.compressionString != null)
  if (compress) {
    if (CompressorStreamFactory.BZIP2.equalsIgnoreCase(fc.compressionString) ||
      CompressorStreamFactory.GZIP.equalsIgnoreCase(fc.compressionString) ||
      CompressorStreamFactory.XZ.equalsIgnoreCase(fc.compressionString))
      LOG.debug("Smart File Producer " + fc.Name + " Using compression: " + fc.compressionString)
    else
      throw FatalAdapterException("Unsupported compression type " + fc.compressionString + " for Smart File Producer: " + fc.Name, new Exception("Invalid Parameters"))
  }

  var partitionFormatString:String = null
  var partitionDateFormats:List[SimpleDateFormat] = null
  if (fc.partitionFormat != null) {
    val partitionVariable = "\\$\\{([^\\}]+)\\}".r
    partitionDateFormats = partitionVariable.findAllMatchIn(fc.partitionFormat).map(x => try {
      val fmt = new SimpleDateFormat(x.group(1))
      fmt.setTimeZone(TimeZone.getTimeZone("UTC"))
      fmt
    } catch {
      case e: Exception => { throw FatalAdapterException(x.group(1) + " is not a valid date format string.", e) }
    }).toList
    partitionFormatString = partitionVariable.replaceAllIn(fc.partitionFormat, "%s")
  }
  
  var nextRolloverTime: Long = 0 
  if (fc.rolloverInterval > 0) {
    LOG.info("Smart File Producer " + fc.Name + ": File rollover is configured. Will rollover files every " + fc.rolloverInterval + " minutes.")
    val dt = System.currentTimeMillis()
    nextRolloverTime = (dt - (dt % (fc.rolloverInterval * 60 * 1000))) + fc.rolloverInterval * 60 * 1000
  }

  //  var fileRoller: Thread = null
  //  if (fc.rolloverInterval > 0) {
  //    LOG.info("Smart File Producer " + fc.Name + ": File rollover is configured. Will rollover files every " + fc.rolloverInterval + " seconds.")
  //    fileRoller = new Thread {
  //      override def run {
  //        LOG.info("Smart File Producer " + fc.Name + ": Rolling over files.")
  //        try { Thread.sleep(fc.rolloverInterval * 1000) } catch { case e: Exception => {} }
  //
  //        while (!shutDown) {
  //          WriteLock(_reent_lock) 
  //          try {
  //            for ((name, pf) <- partitionStreams) {
  //              if (pf != null) {
  //                LOG.debug("Smart File Producer " + fc.Name + ": Rolling file at " + name)
  //                try {
  //                  pf.synchronized {
  //                    pf.outStream.close
  //                  }
  //                } catch {
  //                  case e: Exception => LOG.debug("Smart File Producer " + fc.Name + ": Error closing file: ", e)
  //                }
  //              }
  //            }
  //            partitionStreams.clear()
  //          } finally {
  //            WriteUnlock(_reent_lock)
  //          }
  //
  //          try { Thread.sleep(fc.rolloverInterval * 1000) } catch { case e: Exception => {} }
  //        }
  //      }
  //    }
  //
  //    fileRoller.start
  //  }

  private def rolloverFiles() = {
    LOG.info("Smart File Producer " + fc.Name + ": Rolling over files.")

    WriteLock(_reent_lock)
    try {
      for ((name, pf) <- partitionStreams) {
        if (pf != null) {
          LOG.debug("Smart File Producer " + fc.Name + ": Rolling file at " + name)
          try {
            pf.synchronized {
              pf.outStream.close
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

  private def openFile(fileName: String) = if (fc.uri.startsWith("hdfs://")) openHdfsFile(fileName) else openFsFile(fileName)
  private def write(os: OutputStream, message: Array[Byte]) = if(fc.uri.startsWith("hdfs://")) writeToHdfs(os, message) else writeToFs(os, message)

  private def openFsFile(fileName: String): OutputStream = {
    var os: OutputStream = null
    while (os == null) {
      try {
        val file = new File(fileName)
        file.getParentFile().mkdirs();
        os = new FileOutputStream(file, true) 
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
        case e: Exception => { throw FatalAdapterException("Unable to open connection to specified file ", e) }
      }
    }
    return os;
  }

  private def openHdfsFile(fileName: String): OutputStream = {
    var os: OutputStream = null
    while (os == null) {
      try {
        var hdfsConf: Configuration = new Configuration();
        if (fc.kerberos != null) {
          hdfsConf.set("hadoop.security.authentication", "kerberos")
          UserGroupInformation.setConfiguration(hdfsConf)
          ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(fc.kerberos.principal, fc.kerberos.keytab);
        }

        var uri: URI = URI.create(fileName)
        var path: Path = new Path(uri)
        var fs: FileSystem = FileSystem.get(uri, hdfsConf);

        if (fs.exists(path)) {
          LOG.info("Loading existing file " + uri)
          os = fs.append(path)
        } else {
          LOG.info("Creating new file " + uri);
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
        case e: Exception => { throw FatalAdapterException("Unable to open connection to specified file ", e) }
      }
    }
    return os;
  }
  
  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    implicit val formats = org.json4s.DefaultFormats
    return new MonitorComponentInfo(AdapterConfiguration.TYPE_OUTPUT, fc.Name, SmartFileProducer.ADAPTER_DESCRIPTION, startTime, lastSeen, Serialization.write(metrics).toString)
  }
  
  override def getComponentSimpleStats: String = {
    Category + "/" + getAdapterName + "/evtCnt->" + metrics("MessagesProcessed").asInstanceOf[AtomicLong].get
  }

  private def getPartionFile(record: ContainerInterface) : PartitionFile = {
    var key = record.getTypeName();
    val dateTime = record.getTimePartitionData()
    
    val pk = record.getPartitionKey()
    var bucket:Int = 0
    if(pk != null && pk.length > 0 && fc.partitionBuckets > 1) {
      bucket = pk.mkString("").hashCode() % fc.partitionBuckets
    }
  
    if(dateTime > 0 && partitionFormatString != null && partitionDateFormats != null) {
      val dtTm = new java.util.Date(dateTime)
      val values = partitionDateFormats.map(fmt => fmt.format(dtTm))
      key = record.getTypeName() + "/" + partitionFormatString.format(values: _*)
    }
    
    val path = key
    key = key + bucket

    ReadLock(_reent_lock)
    if (!partitionStreams.contains(key)) {
      //upgrade to write lock
      ReadUnlock(_reent_lock)
      WriteLock(_reent_lock)
      try {
        // need to check again to make sure other threads did not update
        if (!partitionStreams.contains(key)) { // need to check again
          val dt = if(nextRolloverTime > 0) nextRolloverTime - (fc.rolloverInterval * 60 * 1000) else System.currentTimeMillis
          val ts = new java.text.SimpleDateFormat("yyyyMMdd'T'HHmm").format(new java.util.Date(dt))
          val fileName = "%s/%s/%s%s-%d-%s.dat%s".format(fc.uri, path, fc.fileNamePrefix, nodeId, bucket, ts, extensions.getOrElse(fc.compressionString, ""))
          var os = openFile(fileName)
          if (compress)
            os = new CompressorStreamFactory().createCompressorOutputStream(fc.compressionString, os)

          partitionStreams(key) = new PartitionFile(key, fileName, os, 0, 0)
          ReadLock(_reent_lock) // downgrade to original readlock
        }
      } finally {
        WriteUnlock(_reent_lock) // release write lock
      }
    }

    try {
      return partitionStreams(key)
    } finally {
      ReadUnlock(_reent_lock)
    }
  }
  
  private def reopenPartitionFile(pf: PartitionFile): PartitionFile = {
    WriteLock(_reent_lock)
    try {
      partitionStreams.remove(pf.key)
      var os = openFile(pf.name)
      if (compress)
        os = new CompressorStreamFactory().createCompressorOutputStream(fc.compressionString, os)

      partitionStreams(pf.key) = new PartitionFile(pf.key, pf.name, os, 0, 0)

      return partitionStreams(pf.key)
    } finally {
      WriteUnlock(_reent_lock)
    }
  }
  
  private def writeToFs(os: OutputStream, message: Array[Byte]) = {
    os.write(message);
    os.flush()
  }

  private def writeToHdfs(os: OutputStream, message: Array[Byte]) = {
    try {
      os.write(message);
      os.flush()
    } catch {
      case e: Exception => {
        if (fc.kerberos != null) {
          LOG.debug("Smart File Producer " + fc.Name + ": Error writing to HDFS. Will relogin and try.")
          ugi.reloginFromTicketCache()
          os.write(message);
        }
      }
    }
  }

  override def send(message: Array[Array[Byte]], partitionKey: Array[Array[Byte]]): Unit = {
    // Not implemented yet
  }

  // Locking before we write into file
  // To send an array of messages. messages.size should be same as partKeys.size
  override def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): Unit = {
    if (outputContainers.size == 0) return

    val dt = System.currentTimeMillis
    lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(dt))
    if(nextRolloverTime > 0 && dt > nextRolloverTime) {
      rolloverFiles()
      nextRolloverTime += (fc.rolloverInterval * 60 * 1000)
    }
    
    val (outContainers, serializedContainerData, serializerNames) = serialize(tnxCtxt, outputContainers)

    if (outputContainers.size != serializedContainerData.size || outputContainers.size != serializerNames.size) {
      LOG.error("Smart File Producer " + fc.Name + ": Messages, messages serialized data & serializer names should has same number of elements. Messages:%d, Messages Serialized data:%d, serializerNames:%d".format(outputContainers.size, serializedContainerData.size, serializerNames.size))
      return
    }
    if (serializedContainerData.size == 0) return

    try {
      // Op is not atomic
      (serializedContainerData, outputContainers).zipped.foreach((message, record) => {
        var isSuccess = false
        numOfRetries = 0
        var pf = getPartionFile(record);
        while (!isSuccess) {
          try {
            pf.synchronized {
              val data = message++fc.messageSeparator.getBytes
              write(pf.outStream, data)
              pf.records += 1
              pf.size += data.length
            }
            isSuccess = true
            LOG.debug("finished writing message")
            metrics("MessagesProcessed").asInstanceOf[AtomicLong].incrementAndGet()
          } catch {
            case fio: IOException => {
              LOG.warn("Smart File Producer " + fc.Name + ": Unable to write to file " + pf.name)
              if (numOfRetries == MAX_RETRIES) {
                LOG.warn("Smart File Producer " + fc.Name + ": Unable to write to file destination after " + MAX_RETRIES + " tries.  Trying to reopen file " + pf.name, fio)
                pf = reopenPartitionFile(pf)              
              } else if (numOfRetries > MAX_RETRIES) {
                LOG.error("Smart File Producer " + fc.Name + ": Unable to write to file destination after " + MAX_RETRIES + " tries.  Aborting.", fio)
                throw FatalAdapterException("Unable to write to specified file after " + MAX_RETRIES + " retries", fio)
              }
              numOfRetries += 1
              LOG.warn("Smart File Producer " + fc.Name + ": Retyring " + numOfRetries + "/" + MAX_RETRIES)
              Thread.sleep(FAIL_WAIT)
            }
            case e: Exception => {
              LOG.error("Smart File Producer " + fc.Name + ": Unable to write output message: " + new String(message), e)
              throw e
            }
          }
        }
      })
    } catch {
      case e: Exception => {
        LOG.error("Smart File Producer " + fc.Name + ": Failed to send", e)
        throw FatalAdapterException("Unable to send message", e)
      }
    }
  }

  override def Shutdown(): Unit = {
    WriteLock(_reent_lock)
    try {
      shutDown = true

//      if (fileRoller != null) {
//        fileRoller.interrupt
//        fileRoller = null
//      }

      for ((name, pf) <- partitionStreams) {
        if (pf != null) {
          LOG.debug("Smart File Producer " + fc.Name + ": closing file at " + name)
          pf.synchronized {
            pf.outStream.close
          }
        }
      }
      partitionStreams.clear()
    } finally {
      WriteUnlock(_reent_lock)
    }
  }

}

