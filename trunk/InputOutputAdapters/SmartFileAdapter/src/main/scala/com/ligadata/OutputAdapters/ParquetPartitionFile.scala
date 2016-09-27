package com.ligadata.OutputAdapters

import java.io.{IOException, OutputStream}
import java.util.concurrent.atomic.AtomicLong

import com.ligadata.AdaptersConfiguration.SmartFileProducerConfiguration
import com.ligadata.Exceptions.FatalAdapterException
import com.ligadata.KamanjaBase.{ContainerInterface, TransactionContext}
import org.apache.hadoop.security.UserGroupInformation
import org.apache.logging.log4j.LogManager
import parquet.hadoop.ParquetWriter
import parquet.hadoop.metadata.CompressionCodecName

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Yasser on 9/27/2016.
  */
class ParquetPartitionFile(fc : SmartFileProducerConfiguration, key : String, avroSchemaStr : String) extends PartitionFile {

  private[this] val LOG = LogManager.getLogger(getClass)
  private val FAIL_WAIT = 2000
  private val MAX_RETRIES = 3

  private var outStream: OutputStream = null
  private var parquetBuffer = new ArrayBuffer[ContainerInterface]()
  private var records: Long = 0
  private var size: Long = 0
  private var recordsInBuffer: Long = 0
  private var flushBufferSize: Long = 0 // get as param
  private var filePath : String = ""
  private var ugi : UserGroupInformation = null
  private var parquetCompression : CompressionCodecName = null


  private var parquetWriter : ParquetWriter[Array[Any]] = null

  def init(filePath: String, flushBufferSize: Long) = {
    this.flushBufferSize = flushBufferSize
    this.filePath = filePath

    parquetCompression = if(fc.parquetCompression == null || fc.parquetCompression.length == 0) null else CompressionCodecName.valueOf(fc.parquetCompression)

    /*if(!avroSchemasMap.contains(typeName))
              avroSchemasMap.put(typeName, Utils.getAvroSchema(record))
            val parquetWriter = Utils.createAvroParquetWriter(fc, avroSchemasMap(typeName), fileName, parquetCompression)*/

      LOG.info(">>>>>>>>>>>>>>>>>> Avro schema : " + avroSchemaStr)
      val parquetSchema = Utils.getParquetSchema(avroSchemaStr)
      LOG.info(">>>>>>>>>>>>>>>>>> parquet schema : " + parquetSchema.toString)
      val writeSupport = new ParquetWriteSupport(parquetSchema)

    parquetWriter = Utils.createParquetWriter(fc, filePath, writeSupport, parquetCompression)

    var buffer: ArrayBuffer[ContainerInterface] = null
    if (flushBufferSize > 0)
      buffer = new ArrayBuffer[ContainerInterface]
  }

  def getFilePath: String = filePath

  def getKey: String = key

  def getRecordsInBuffer: Long = recordsInBuffer

  def getSize: Long = size

  def getFlushBufferSize: Long = flushBufferSize

  def close() : Unit = {
    if(outStream != null)
      outStream.close()
  }

  def reopen(): Unit ={
    throw new UnsupportedOperationException("Unsupported reopening parquet file")
  }

  def send(tnxCtxt: TransactionContext, record: ContainerInterface,
           serializer : SmartFileProducer) : Unit = {

    try {
      // Op is not atomic

        var isSuccess = false
        var numOfRetries = 0
        while (!isSuccess) {
          try {
            this.synchronized {
              if (flushBufferSize > 0) {
                LOG.info("Smart File Producer " + fc.Name + ": adding record to buffer for file " + filePath)

                parquetBuffer.append(record)

                recordsInBuffer += 1
                if (parquetBuffer.size > flushBufferSize) {
                  LOG.info("Smart File Producer " + fc.Name + ": buffer is full writing to file " + filePath)
                  //avroSchemasMap(pf.parquetBuffer(0).getFullTypeName)
                  writeParquet(fc, parquetBuffer.toArray)
                  size += parquetBuffer.size
                  records += recordsInBuffer
                  parquetBuffer.clear()
                  recordsInBuffer = 0
                }



              } else {
                LOG.info("Smart File Producer " + fc.Name + ": writing record to file " + filePath)
                val data = Array(record)
                //avroSchemasMap(record.getFullTypeName)
                writeParquet(fc, data)
                size += data.length

                records += 1

              }
            }
            isSuccess = true
            LOG.info("finished writing message")
            //metrics("MessagesProcessed").asInstanceOf[AtomicLong].incrementAndGet() //TODO
          } catch {
            case fio: IOException => {
              LOG.warn("Smart File Producer " + fc.Name + ": Unable to write to file " + filePath)
              if (numOfRetries == MAX_RETRIES) {
                LOG.warn("Smart File Producer " + fc.Name + ": Unable to write to file destination after " + MAX_RETRIES + " tries.  Trying to reopen file " + filePath, fio)
                //TODO : what to do for parquet
              } else if (numOfRetries > MAX_RETRIES) {
                LOG.error("Smart File Producer " + fc.Name + ": Unable to write to file destination after " + MAX_RETRIES + " tries.  Aborting.", fio)
                throw FatalAdapterException("Unable to write to specified file after " + MAX_RETRIES + " retries", fio)
              }
              numOfRetries += 1
              LOG.warn("Smart File Producer " + fc.Name + ": Retyring " + numOfRetries + "/" + MAX_RETRIES)
              Thread.sleep(FAIL_WAIT)
            }
            case e: Exception => {

                val messageStr =
                  if(record == null) "null"
                  else record.getAllAttributeValues.map(attr => if(attr==null || attr.getValue == null) "null" else attr.getValue.toString).mkString(fc.messageSeparator)
                LOG.error("Smart File Producer " + fc.Name + ": Unable to write output message: " + messageStr, e)

              throw e
            }
          }
        }

    } catch {
      case e: Exception => {
        LOG.error("Smart File Producer " + fc.Name + ": Failed to send", e)
        throw FatalAdapterException("Unable to send message", e)
      }
    }
  }

  def flush(): Unit ={
    LOG.info("Smart File Producer :" + fc.Name + " : In flushPartitionFile key - [" + key + "]")
    var isSuccess = false
    var numOfRetries = 0
    while (!isSuccess) {
      try {
        this.synchronized {
          if (flushBufferSize > 0 && parquetBuffer.size > 0) {
            writeParquet(fc, parquetBuffer.toArray)
            size += parquetBuffer.size
            records += recordsInBuffer
            parquetBuffer.clear()
            recordsInBuffer = 0
          }

        }
        isSuccess = true
      } catch {
        case fio: IOException => {
          LOG.warn("Smart File Producer " + fc.Name + ": Unable to flush buffer to file " + filePath)
          if (numOfRetries == MAX_RETRIES) {
            //TODO : what to be done?
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

  private def writeParquet(fc: SmartFileProducerConfiguration, messages: Array[ContainerInterface]): Unit = {

    messages.foreach(message => {
      val msgData : Array[Any] = message.getAllAttributeValues.map(attr => attr.getValue)
      parquetWriter.write(msgData)
    })
  }
}
