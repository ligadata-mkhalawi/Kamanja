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
import parquet.io.api.Binary

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
  private var ignoreFieldsNames = scala.collection.immutable.Set[String]()
  private var ignoreFieldsIdxs = scala.collection.immutable.Set[Int]()

  class BinFldInputVal {
    var outputIdx: Int = -1
    var binType: Int = -1
  }

  private var binaryFieldIdxs = scala.collection.mutable.Map[Int, Int]()
  private var binaryFieldInputIdxs = scala.collection.mutable.Map[Int, BinFldInputVal]() // This is mapped to direct input idxs. So that way we can ignore then in conversion. value is outputidx & bintype

  // We are considering only fixed & only one type of message for now
  private var recordElementId: Long = 0
  private var recordSchemaId: Int = 0

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

    if (LOG.isInfoEnabled) LOG.info(">>>>>>>>>>>>>>>>>> Avro schema : " + avroSchemaStr + ", ignoreFieldsNames:" + ignoreFlds.mkString(","))
    val parquetSchema = Utils.getParquetSchema(avroSchemaStr, ignoreFlds)
    if (LOG.isInfoEnabled) LOG.info(">>>>>>>>>>>>>>>>>> parquet schema : " + parquetSchema.toString)

    val ignoreNullFlags =
       if (fc.otherConfig.contains("NullFlagsFieldName"))
         fc.otherConfig.getOrElse("NullFlagsFieldName", SmartFileProducer.nullFlagsFieldDefaultName).toString.trim
       else SmartFileProducer.nullFlagsFieldDefaultName

    val writeSupport = new ParquetWriteSupport(parquetSchema, ignoreNullFlags)

    actualActiveFilePath = if (tmpFilePath.length > 0) tmpFilePath else filePath
    parquetWriter = Utils.createParquetWriter(fc, actualActiveFilePath, writeSupport, parquetCompression)

    ignoreFieldsNames = (if (ignoreFlds != null) ignoreFlds.toSet else scala.collection.immutable.Set[String]() ++ writeSupport.getSystemFields)

    writeSupport.getBinaryFieldIdxs.foreach(idx => {
      binaryFieldIdxs(idx) = 0 // This is simple Binary
    })

    writeSupport.getArrayBinaryFieldIdxs.foreach(idx => {
      binaryFieldIdxs(idx) = 1 // This is Array Binary
    })

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

          if (LOG.isInfoEnabled) LOG.info("parquet file closed. moving file {} to {}", tmpFilePathNoProtocol, finalFullPathNoProtocol)
          val moved = fileHandler.moveTo(finalFullPathNoProtocol)
          if (moved) {
            if (LOG.isInfoEnabled) LOG.info("move successful")
          }
          else {
            LOG.error("Move unsuccessful: could not move tmp parquet file {} to {}", tmpFilePathNoProtocol, finalFullPathNoProtocol)
          }
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

  private def resolveElemntIdAndIgnoreFlds(record: ContainerInterface, serializer: SmartFileProducer): Unit = synchronized {
    if (recordSchemaId == 0) {
      recordSchemaId = record.getSchemaId
      recordElementId = serializer.nodeContext.getEnvCtxt()._mgr.ElementIdForSchemaId(recordSchemaId)
      // Here it may go wrong only when this record get partial fields
      // Here we can use getAttributeTypes instead of getAllAttributeValues. But this is called only once. I think this is fine.

      val recVals = record.getAllAttributeValues

      ignoreFieldsIdxs = recVals.filter(attr => ignoreFieldsNames.contains(attr.getValueType().getName())).map(attr => attr.getValueType().getIndex).toSet

      var outIdx = 0
      recVals.filter(attr => !ignoreFieldsIdxs.contains(attr.getValueType().getIndex)).map(attr => {
        if (binaryFieldIdxs.contains(outIdx)) {
          val binFldInputVal = new BinFldInputVal
          binFldInputVal.binType = binaryFieldIdxs.getOrElse(outIdx, -1)
          binFldInputVal.outputIdx = outIdx
          binaryFieldInputIdxs(attr.getValueType().getIndex) = binFldInputVal
        }
        outIdx += 1
      })
    }
  }

  private def getRecordValues(record: ContainerInterface): Array[Any] = {
    record.getAllAttributeValues.filter(attr => !ignoreFieldsIdxs.contains(attr.getValueType().getIndex)).map(attr => {
      val binFldInputVal = binaryFieldInputIdxs.getOrElse(attr.getValueType().getIndex, null)
      val value = attr.getValue
      if (binFldInputVal != null) {
        if (binFldInputVal.binType == 0) {
          Binary.fromString(value.toString)
        } else {
          val valueAr = value.asInstanceOf[Array[String]]
          valueAr.map(v => {
            if (v != null) {
              Binary.fromString(v)
            } else {
              null
            }
          })
        }
      } else {
        value
      }
    })
  }

  def send(tnxCtxt: TransactionContext, recordsArr: Array[ContainerInterface], serializer: SmartFileProducer): Array[Int] = {
    if (recordsArr.size == 0) return Array[Int]()

    val resultSize = recordsArr.size
    var retResults = new Array[Int](resultSize)

    for (i <- 0 until resultSize) {
      retResults(i) = SendStatus.FAILURE
    }

    if (recordSchemaId == 0) {
      resolveElemntIdAndIgnoreFlds(recordsArr(0), serializer)
    }

    val recordsData = recordsArr.par.map(record => getRecordValues(record))

    var writingRecIdx = 0
    while (writingRecIdx < resultSize) {
      val curRec = recordsData(writingRecIdx)
      retResults(writingRecIdx) = writeRecordData(curRec)
      if (retResults(writingRecIdx) == SendStatus.SUCCESS)
        records += 1
      writingRecIdx += 1
    }

    /*
        var i = 0
        recordsData.toArray.foreach(curRec => {
          retResults(i) = writeRecordData(curRec)
          if (retResults(i) == SendStatus.SUCCESS)
            records += 1
          i += 1
        })
    */

    retResults
  }

  def send(tnxCtxt: TransactionContext, record: ContainerInterface, serializer: SmartFileProducer): Int = {

    try {
      if (!record.isFixed) {
        throw new Exception("Support only fixed messages for Parquet format")
      }

      if (recordSchemaId == 0) {
        resolveElemntIdAndIgnoreFlds(record, serializer)
      }

      val recordData: Array[Any] = getRecordValues(record)

      val retCode = writeRecordData(recordData)
      if (retCode == SendStatus.SUCCESS)
        records += 1

      return retCode
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

  private def writeRecordData(recordData: Array[Any]): Int = {
    try {
      var isSuccess = false
      var numOfRetries = 0
      while (!isSuccess) {
        try {
          //no need to buffer in parquet, writer already acts as a buffer
          if (LOG.isInfoEnabled) LOG.info("Smart File Producer " + fc.Name + ": writing record to file " + actualActiveFilePath)
          //avroSchemasMap(record.getFullTypeName)
          // flush_lock.synchronized {
          parquetWriter.write(recordData)
          //reflectionUtil.callFlush(parquetWriter)
          //reflectionUtil.callInitStore(parquetWriter)
          // }

          size += recordData.length // TODO : do we need to get actual size ?
          isSuccess = true
          if (LOG.isInfoEnabled) LOG.info("finished writing message")
          return SendStatus.SUCCESS
        } catch {
          case fio: IOException => {
            LOG.error("Smart File Producer " + fc.Name + ": Unable to write to file " + actualActiveFilePath, fio)
            throw FatalAdapterException("Unable to write to specified file " + actualActiveFilePath, fio)
          }
          case e: Throwable => {
            LOG.error("Smart File Producer " + fc.Name + ": Unable to write output message", e)
            throw e
          }
        }
      }
      SendStatus.FAILURE
    } catch {
      case e: Throwable => {
        LOG.error("Smart File Producer " + fc.Name + ": Failed to send", e)
        throw FatalAdapterException("Smart File Producer " + fc.Name + " is Unable to send message", e)
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
