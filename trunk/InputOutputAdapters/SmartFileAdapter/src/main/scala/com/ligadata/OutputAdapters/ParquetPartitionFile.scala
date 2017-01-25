package com.ligadata.OutputAdapters

import java.io.{IOException}
import com.ligadata.AdaptersConfiguration.SmartFileProducerConfiguration
import com.ligadata.Exceptions.FatalAdapterException
import com.ligadata.InputAdapters.SmartFileHandlerFactory
import com.ligadata.KamanjaBase.{ContainerInterface, TransactionContext}
import org.apache.hadoop.security.UserGroupInformation
import org.apache.logging.log4j.LogManager
import parquet.hadoop.ParquetWriter
import parquet.hadoop.metadata.CompressionCodecName

import scala.collection.mutable.ArrayBuffer


class ParquetPartitionFile(fc: SmartFileProducerConfiguration, key: String, avroSchemaStr: String, ignoreFlds: Array[String]) extends PartitionFile {

  private[this] val LOG = LogManager.getLogger(getClass)
  private val FAIL_WAIT = 2000
  private val MAX_RETRIES = 3

  //private val parquetBuffer = new ArrayBuffer[ContainerInterface]()
  //private var recordsInBuffer: Long = 0
  private var records: Long = 0
  private var size: Long = 0

  //private var flushBufferSize: Long = 0
  private var finalFilePath: String = ""
  private var ugi: UserGroupInformation = null
  private var parquetCompression: CompressionCodecName = null
  private var ignoreFields = scala.collection.immutable.Set[String]()

  private var tmpDir = ""
  private var tmpFileName = ""

  def tmpFilePath = if (tmpDir == null || tmpDir.length == 0) "" else tmpDir + "/" + tmpFileName

  private var actualActiveFilePath = ""

  def finalFileDir = {
    val idx = finalFilePath.lastIndexOf("/")
    if (idx >= 0) finalFilePath.substring(0, idx) else ""
  }

  private var parquetWriter: ParquetWriter[Array[Any]] = null

  def init(filePath: String, flushBufferSize: Long) = {
    //this.flushBufferSize = flushBufferSize
    this.finalFilePath = filePath
    //tmpFileName = filePath.replaceAll("/", "_")
    val filePathTokens = filePath.split("/")
    tmpDir = filePathTokens.take(filePathTokens.length - 1).mkString("/") //use same folder
    tmpFileName = "." + filePathTokens(filePathTokens.length - 1)

    parquetCompression = if (fc.parquetCompression == null || fc.parquetCompression.length == 0) null else CompressionCodecName.valueOf(fc.parquetCompression)

    /*if(!avroSchemasMap.contains(typeName))
              avroSchemasMap.put(typeName, Utils.getAvroSchema(record))
            val parquetWriter = Utils.createAvroParquetWriter(fc, avroSchemasMap(typeName), fileName, parquetCompression)*/

    if (LOG.isInfoEnabled) LOG.info(">>>>>>>>>>>>>>>>>> Avro schema : " + avroSchemaStr + ", ignoreFields:" + ignoreFlds.mkString(","))
    val parquetSchema = Utils.getParquetSchema(avroSchemaStr, ignoreFlds)
    if (LOG.isInfoEnabled) LOG.info(">>>>>>>>>>>>>>>>>> parquet schema : " + parquetSchema.toString)
    val writeSupport = new ParquetWriteSupport(parquetSchema)

    ignoreFields = if (ignoreFlds != null) ignoreFlds.toSet else scala.collection.immutable.Set[String]()

    actualActiveFilePath = if (tmpFilePath.length > 0) tmpFilePath else filePath
    parquetWriter = Utils.createParquetWriter(fc, actualActiveFilePath, writeSupport, parquetCompression)

    var buffer: ArrayBuffer[ContainerInterface] = null
    if (flushBufferSize > 0)
      buffer = new ArrayBuffer[ContainerInterface]
  }

  def getFilePath: String = finalFilePath

  def getKey: String = key

  def getRecordsInBuffer: Long = 0

  def getSize: Long = 0

  def getFlushBufferSize: Long = 0

  def close(): Unit = {
    if (parquetWriter != null) {
      val isClosedProperly =
        try {
          //LOG.info("closing parquetWriter for file " + actualActiveFilePath)
          LOG.warn("Smart File Producer " + fc.Name + ": closing parquet file  " + actualActiveFilePath)
          parquetWriter.close()
          true
        }
        catch {
          case ex: Throwable =>
            LOG.error("Error while closing file " + actualActiveFilePath, ex)
            false
        }

      if (isClosedProperly && !actualActiveFilePath.equals(finalFilePath)) {
        //move from tmp file to final dest
        try {
          val inputConfig = com.ligadata.AdaptersConfiguration.SmartFileAdapterConfiguration.outputConfigToInputConfig(fc)
          val tmpFileDirNoProtocol = com.ligadata.InputAdapters.MonitorUtils.getFileParentDir(tmpFilePath, inputConfig)
          val tmpFilePathNoProtocol = tmpFileDirNoProtocol + "/" + tmpFileName
          val filePathTokens = finalFilePath.split("/")
          val finalFileBaseName = filePathTokens(filePathTokens.length - 1)
          val finalFullDirNoProtocol = com.ligadata.InputAdapters.MonitorUtils.getFileParentDir(finalFilePath, inputConfig)
          val finalFullPathNoProtocol = finalFullDirNoProtocol + "/" + finalFileBaseName

          val fileHandler = SmartFileHandlerFactory.createSmartFileHandler(inputConfig, tmpFilePathNoProtocol)
          val osWriter = new OutputStreamWriter()
          if (!osWriter.isFileExists(fc, finalFileDir))
            osWriter.mkdirs(fc, finalFileDir)

          LOG.info("parquet file closed. moving file {} to {}", tmpFilePathNoProtocol, finalFullPathNoProtocol)
          val moved = fileHandler.moveTo(finalFullPathNoProtocol)
          if (moved)
            LOG.info("move successful")
          else LOG.error("Move unsuccessful: could not move tmp parquet file {} to {}", tmpFilePathNoProtocol, finalFullPathNoProtocol)

          //fileHandler.disconnect()
        }
        catch {
          case ex: Throwable => LOG.error("Error while moving file " +
            actualActiveFilePath + " to " + finalFilePath, ex)
        }
      }
    }
  }

  def reopen(): Unit = {
    throw new UnsupportedOperationException("Unsupported reopening parquet file")
  }

  val flush_lock = new Object()

  val reflectionUtil = new ReflectionUtil()

  def send(tnxCtxt: TransactionContext, record: ContainerInterface,
           serializer: SmartFileProducer): Int = {

    try {
      // Op is not atomic

      var isSuccess = false
      var numOfRetries = 0
      while (!isSuccess) {
        try {
          this.synchronized {
            //no need to buffer in parquet, writer already acts as a buffer

            LOG.info("Smart File Producer " + fc.Name + ": writing record to file " + actualActiveFilePath)
            val recordData: Array[Any] = record.getAllAttributeValues.filter(attr => !ignoreFields.contains(attr.getValueType().getName())).map(attr => attr.getValue)
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
            LOG.warn("Smart File Producer " + fc.Name + ": Unable to write to file " + actualActiveFilePath)
            if (numOfRetries == MAX_RETRIES) {
              LOG.warn("Smart File Producer " + fc.Name + ": Unable to write to file destination after " + MAX_RETRIES + " tries.  Trying to reopen file " + actualActiveFilePath, fio)
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
              if (record == null) "null"
              else record.getAllAttributeValues.map(attr => if (attr == null || attr.getValue == null) "null" else attr.getValue.toString).mkString(",")
            LOG.error("Smart File Producer " + fc.Name + ": Unable to write output message: " + messageStr, e)

            throw e
          }
        }
      }

      SendStatus.FAILURE

    } catch {
      case e: Exception => {
        val messageStr =
          if (record == null) "null"
          else record.getAllAttributeValues.map(attr => if (attr == null || attr.getValue == null) "null" else attr.getValue.toString).mkString(",")

        LOG.error("Smart File Producer " + fc.Name + ": Failed to send", e)
        throw FatalAdapterException("Smart File Producer " + fc.Name + " is Unable to send message: " + messageStr, e)
      }
    }
  }

  def flush(): Unit = {
    //no manual buffering, so nothing to flush
  }

  /*private def writeParquet(fc: SmartFileProducerConfiguration, messages: Array[ContainerInterface]): Unit = {
    messages.foreach(message => {
      val msgData : Array[Any] = message.getAllAttributeValues.map(attr => attr.getValue)
      parquetWriter.write(msgData)
    })
  }*/
}
