package com.ligadata.OutputAdapters

import java.io.{IOException}
import com.ligadata.AdaptersConfiguration.SmartFileProducerConfiguration
import com.ligadata.Exceptions.FatalAdapterException
import com.ligadata.KamanjaBase.{ContainerInterface, TransactionContext}
import org.apache.hadoop.security.UserGroupInformation
import org.apache.logging.log4j.LogManager
import parquet.hadoop.ParquetWriter
import parquet.hadoop.metadata.CompressionCodecName

import scala.collection.mutable.ArrayBuffer


class ParquetPartitionFile(fc : SmartFileProducerConfiguration, key : String, avroSchemaStr : String) extends PartitionFile {

  private[this] val LOG = LogManager.getLogger(getClass)
  private val FAIL_WAIT = 2000
  private val MAX_RETRIES = 3

  //private val parquetBuffer = new ArrayBuffer[ContainerInterface]()
  //private var recordsInBuffer: Long = 0
  private var records: Long = 0
  private var size: Long = 0

  //private var flushBufferSize: Long = 0
  private var filePath : String = ""
  private var ugi : UserGroupInformation = null
  private var parquetCompression : CompressionCodecName = null


  private var parquetWriter : ParquetWriter[Array[Any]] = null

  def init(filePath: String, flushBufferSize: Long) = {
    //this.flushBufferSize = flushBufferSize
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

  def getRecordsInBuffer: Long = 0

  def getSize: Long = 0

  def getFlushBufferSize: Long = 0

  def close() : Unit = {
    if(parquetWriter != null)
      parquetWriter.close()
  }

  def reopen(): Unit ={
    throw new UnsupportedOperationException("Unsupported reopening parquet file")
  }

  val flush_lock = new Object()

  val reflectionUtil = new ReflectionUtil()
  def send(tnxCtxt: TransactionContext, record: ContainerInterface,
           serializer : SmartFileProducer) : Int = {

    try {
      // Op is not atomic

        var isSuccess = false
        var numOfRetries = 0
        while (!isSuccess) {
          try {
            this.synchronized {//no need to buffer in parquet, writer already acts as a buffer

              LOG.info("Smart File Producer " + fc.Name + ": writing record to file " + filePath)
              val recordData : Array[Any] = record.getAllAttributeValues.map(attr => attr.getValue)
              //avroSchemasMap(record.getFullTypeName)
              flush_lock.synchronized {
                parquetWriter.write(recordData)
                //reflectionUtil.callFlush(parquetWriter)
                //reflectionUtil.callInitStore(parquetWriter)
              }

              size += recordData.length // TODO : do we need to get actual size ?
              records += 1
            }
            isSuccess = true
            LOG.info("finished writing message")
            //metrics("MessagesProcessed").asInstanceOf[AtomicLong].incrementAndGet()
            return SendStatus.SUCCESS
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

      SendStatus.FAILURE

    } catch {
      case e: Exception => {
        LOG.error("Smart File Producer " + fc.Name + ": Failed to send", e)
        throw FatalAdapterException("Unable to send message", e)
      }
    }
  }

  def flush(): Unit ={
    //no manual buffering, so nothing to flush
  }

  /*private def writeParquet(fc: SmartFileProducerConfiguration, messages: Array[ContainerInterface]): Unit = {
    messages.foreach(message => {
      val msgData : Array[Any] = message.getAllAttributeValues.map(attr => attr.getValue)
      parquetWriter.write(msgData)
    })
  }*/
}
