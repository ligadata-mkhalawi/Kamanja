package com.ligadata.OutputAdapters

import java.io.{FileOutputStream, File, OutputStream, IOException}

import com.ligadata.AdaptersConfiguration.SmartFileProducerConfiguration
import com.ligadata.Exceptions.{UnsupportedOperationException, FatalAdapterException}
import com.ligadata.KamanjaBase.{ContainerInterface, TransactionContext}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.security.UserGroupInformation
import org.apache.logging.log4j.LogManager

import scala.collection.mutable.ArrayBuffer


class StreamPartitionFile(fc : SmartFileProducerConfiguration, key : String) extends PartitionFile{

  private[this] val LOG = LogManager.getLogger(getClass)
  private val FAIL_WAIT = 2000
  private val MAX_RETRIES = 3

  private var outStream: OutputStream = null
  private var streamBuffer = new ArrayBuffer[Byte]()
  private var records: Long = 0
  private var size: Long = 0
  private var recordsInBuffer: Long = 0
  private var flushBufferSize: Long = 0
  private var filePath : String = ""
  private var ugi : UserGroupInformation = null
  private var compress = false

  val canAppend = true

  def init(filePath: String, flushBufferSize: Long) = {
    this.flushBufferSize = flushBufferSize
    this.filePath = filePath

    compress = fc.compressionString != null

    openStream()
  }

  private def openStream(): Unit ={
    //open output stream
    var os : OutputStream = null
    var originalStream : OutputStream = null
    //os = openFile(fc, fileName)
    val osWriter = new com.ligadata.OutputAdapters.OutputStreamWriter()
    originalStream = osWriter.openFile(fc, filePath, canAppend)

    if (compress)
      os = new CompressorStreamFactory().createCompressorOutputStream(fc.compressionString, originalStream)
    else
      os = originalStream

    outStream = new PartitionStream(os, originalStream)
  }

  def getKey: String = key

  def getFilePath: String = filePath

  def getRecordsInBuffer: Long = recordsInBuffer

  def getSize: Long = size

  def close() : Unit = {
    if(outStream != null)
      outStream.close()
  }

  def getFlushBufferSize: Long = flushBufferSize

  private def trimFileFromLocalFileSystem(fileName: String): String = {
    if (fileName.startsWith("file://"))
      return fileName.substring("file://".length() - 1)
    fileName
  }
  /*private def openFsFile(fc: SmartFileProducerConfiguration, fileName: String, canAppend: Boolean): OutputStream = {
    var os: OutputStream = null
    var numOfRetries = 0
    while (os == null) {
      try {
        val file = new File(trimFileFromLocalFileSystem(fileName))
        file.getParentFile.mkdirs()
        os = new FileOutputStream(file, canAppend)
      } catch {
        case fio: IOException =>
          LOG.warn("Smart File Producer " + fc.Name + ": Unable to create a file destination " + fc.uri + " due to an IOException", fio)
          if (numOfRetries > MAX_RETRIES) {
            LOG.error("Smart File Producer " + fc.Name + ":Unable to create a file destination after " + MAX_RETRIES + " tries.  Aborting.")
            throw FatalAdapterException("Unable to open connection to specified file after " + MAX_RETRIES + " retries", fio)
          }
          numOfRetries += 1
          LOG.warn("Smart File Producer " + fc.Name + ": Retyring " + numOfRetries + "/" + MAX_RETRIES)
          Thread.sleep(FAIL_WAIT)

        case e: Exception =>
          throw FatalAdapterException("Unable to open connection to specified file ", e)

      }
    }
    os
  }*/
  /*private def openHdfsFile(fc: SmartFileProducerConfiguration, fileName: String, canAppend: Boolean): OutputStream = {
    var os: OutputStream = null
    var numOfRetries = 0
    while (os == null) {
      try {
        val hdfsConf: Configuration = new Configuration()
        if (fc.kerberos != null) {
          hdfsConf.set("hadoop.security.authentication", "kerberos")
          UserGroupInformation.setConfiguration(hdfsConf)
          ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(fc.kerberos.principal, fc.kerberos.keytab)
        }

        if (fc.hadoopConfig != null && fc.hadoopConfig.nonEmpty) {
          fc.hadoopConfig.foreach(conf => {
            hdfsConf.set(conf._1, conf._2)
          })
        }

        val uri: URI = URI.create(fileName)
        val path: Path = new Path(uri)
        val fs: FileSystem = FileSystem.get(uri, hdfsConf)

        if (fs.exists(path)) {
          if (!canAppend) {
            throw UnsupportedOperationException("File %s exists but append is not permitted".format(fileName), null)
          }
          LOG.info("Smart File Producer " + fc.Name + "(" + this + ") : Loading existing file " + uri)
          os = fs.append(path)
        } else {
          LOG.info("Smart File Producer " + fc.Name + "(" + this + ") : Creating new file " + uri)
          os = fs.create(path)
        }
      } catch {
        case fio: IOException =>
          LOG.warn("Smart File Producer " + fc.Name + ": Unable to create a file destination " + fc.uri + " due to an IOException", fio)
          if (numOfRetries > MAX_RETRIES) {
            LOG.error("Smart File Producer " + fc.Name + ":Unable to create a file destination after " + MAX_RETRIES + " tries.  Aborting.")
            throw FatalAdapterException("Unable to open connection to specified file after " + MAX_RETRIES + " retries", fio)
          }
          numOfRetries += 1
          LOG.warn("Smart File Producer " + fc.Name + ": Retyring " + numOfRetries + "/" + MAX_RETRIES)
          Thread.sleep(FAIL_WAIT)

        case e: Exception =>
          throw FatalAdapterException("Unable to open connection to specified file ", e)

      }
    }
    os
  }*/


  def reopen(): Unit ={
    openStream()
  }

  private def writeToFs(fc: SmartFileProducerConfiguration, message: Array[Byte]) = {
    outStream.write(message)
    outStream.flush()
  }
  private def writeToHdfs(fc: SmartFileProducerConfiguration, message: Array[Byte]) = {
    try {
      //val stream = os.asInstanceOf[PartitionStream].compressStream
      outStream.write(message)
      outStream.flush()  // this flush will call hsync for HDFS
    } catch {
      case e: Exception =>
        if (fc.kerberos != null) {
          LOG.debug("Smart File Producer " + fc.Name + ": Error writing to HDFS. Will relogin and try.")

          val hdfsConf: Configuration = new Configuration()
          hdfsConf.set("hadoop.security.authentication", "kerberos")
          UserGroupInformation.setConfiguration(hdfsConf)
          ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(fc.kerberos.principal, fc.kerberos.keytab)

          ugi.reloginFromTicketCache()
          outStream.write(message)
          outStream.flush()
        }

    }
  }
  private def writeStream(fc: SmartFileProducerConfiguration, message: Array[Byte]): Unit = {
    if (fc.uri.startsWith("hdfs://")) writeToHdfs(fc, message) else writeToFs(fc, message)
  }

  def send(tnxCtxt: TransactionContext, record: ContainerInterface,
           serializer : SmartFileProducer) : Int = {

    val (outContainers, serializedContainerData, serializerNames) = serializer.serialize(tnxCtxt, Array(record))

    if (1 != serializedContainerData.length || 1 != serializerNames.length) {
      LOG.error("Smart File Producer " + fc.Name + ": Messages, messages serialized data & serializer names should has same number of elements. Messages:%d, Messages Serialized data:%d, serializerNames:%d".format(1, serializedContainerData.length, serializerNames.length))
      return SendStatus.FAILURE
    }
    if (serializedContainerData.length == 0) return SendStatus.NO_DATA

    try {
      // Op is not atomic
      val message = serializedContainerData.head

      var isSuccess = false
      var numOfRetries = 0

      while (!isSuccess) {
        try {
          this.synchronized {
            if (flushBufferSize > 0) {
              LOG.info("Smart File Producer " + fc.Name + ": adding record to buffer for file " + filePath)

                streamBuffer ++= message
                streamBuffer ++= fc.messageSeparator.getBytes

                recordsInBuffer += 1
                if (streamBuffer.size > flushBufferSize) {
                  LOG.info("Smart File Producer " + fc.Name + ": buffer is full writing to file " + filePath)
                  writeStream(fc, streamBuffer.toArray)
                  size += streamBuffer.size
                  records += recordsInBuffer
                  streamBuffer.clear()
                  recordsInBuffer = 0
                }


            } else {
              LOG.info("Smart File Producer " + fc.Name + ": writing record to file " + filePath)
                val data = message ++ fc.messageSeparator.getBytes
                writeStream(fc, data)
                size += data.length
                records += 1

            }
          }
          isSuccess = true
          LOG.info("finished writing message")
          //metrics("MessagesProcessed").asInstanceOf[AtomicLong].incrementAndGet() : TODO : call from producer, based on return val
          return SendStatus.SUCCESS
        } catch {
          case fio: IOException =>
            LOG.warn("Smart File Producer " + fc.Name + ": Unable to write to file " + filePath)
            if (numOfRetries == MAX_RETRIES) {
              LOG.warn("Smart File Producer " + fc.Name + ": Unable to write to file destination after " + MAX_RETRIES + " tries.  Trying to reopen file " + filePath, fio)
              //pf = reopenPartitionFile() //call from producer, based on return val
              return SendStatus.REOPEN
            } else if (numOfRetries > MAX_RETRIES) {
              LOG.error("Smart File Producer " + fc.Name + ": Unable to write to file destination after " + MAX_RETRIES + " tries.  Aborting.", fio)
              throw FatalAdapterException("Unable to write to specified file after " + MAX_RETRIES + " retries", fio)
            }
            numOfRetries += 1
            LOG.warn("Smart File Producer " + fc.Name + ": Retyring " + numOfRetries + "/" + MAX_RETRIES)
            Thread.sleep(FAIL_WAIT)

          case e: Exception =>

            LOG.error("Smart File Producer " + fc.Name + ": Unable to write output message: " + new String(message), e)
            throw e

        }
      }

      return SendStatus.FAILURE

    } catch {
      case e: Exception =>
        LOG.error("Smart File Producer " + fc.Name + ": Failed to send", e)
        throw FatalAdapterException("Unable to send message", e)

    }


  }

  def flush(): Unit ={
    LOG.info("Smart File Producer :" + fc.Name + " : In flushPartitionFile key - [" + key + "]")
    var isSuccess = false
    var numOfRetries = 0
    while (!isSuccess) {
      try {
        this.synchronized {

            if (flushBufferSize > 0 && streamBuffer.size > 0) {
              writeStream(fc, streamBuffer.toArray)
              size += streamBuffer.size
              records += recordsInBuffer
              streamBuffer.clear()
              recordsInBuffer = 0
            }
        }
        isSuccess = true
      } catch {
        case fio: IOException => {
          LOG.warn("Smart File Producer " + fc.Name + ": Unable to flush buffer to file " + filePath)
          if (numOfRetries == MAX_RETRIES) {
              LOG.warn("Smart File Producer " + fc.Name + ": Unable to flush buffer to file destination after " + MAX_RETRIES + " tries.  Trying to reopen file " + filePath, fio)
              //pf = reopenPartitionFile(pf) TODO

          } else if (numOfRetries > MAX_RETRIES) {
            LOG.error("Smart File Producer " + fc.Name + ": Unable to flush buffer to file destination after " + MAX_RETRIES + " tries.  Aborting.", fio)
            throw FatalAdapterException("Unable to flush buffer to specified file after " + MAX_RETRIES + " retries", fio)
          }
          numOfRetries += 1
          LOG.warn("Smart File Producer " + fc.Name + ": Retyring " + numOfRetries + "/" + MAX_RETRIES)
          Thread.sleep(FAIL_WAIT)
        }
        case e: Exception => {
          LOG.error("Smart File Producer " + fc.Name + ": Unable to flush buffer to file " + filePath, e)
          throw e
        }
      }
    }
  }
}
