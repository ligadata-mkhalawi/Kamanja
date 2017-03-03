package com.ligadata.OutputAdapters

import com.ligadata.AdaptersConfiguration.SmartFileProducerConfiguration
import com.ligadata.KamanjaBase.{AttributeValue, ContainerInterface}
import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation
import org.apache.logging.log4j.LogManager
import parquet.hadoop.ParquetWriter
import parquet.hadoop.metadata.CompressionCodecName
import org.apache.hadoop.fs.Path
import org.apache.avro.generic.{GenericRecordBuilder, GenericRecord}
import org.apache.avro.file.DataFileWriter;
import parquet.avro.AvroParquetWriter;

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization

/**
  * Created by Yasser on 9/11/2016.
  */
object Utils {

  private val LOG = LogManager.getLogger(getClass)

  def createHdfsConfig(fc: SmartFileProducerConfiguration): Configuration = {
    val hdfsConf: Configuration = new Configuration()
    if (fc.kerberos != null) {
      hdfsConf.set("hadoop.security.authentication", "kerberos")
      UserGroupInformation.setConfiguration(hdfsConf)
      val ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(fc.kerberos.principal, fc.kerberos.keytab);
    }

    if (fc.hadoopConfig != null && !fc.hadoopConfig.isEmpty) {
      fc.hadoopConfig.foreach(conf => {
        hdfsConf.set(conf._1, conf._2)
      })
    }
    hdfsConf
  }

  def getAvroSchema(avroSchemaStr: String, ignoreFields: Array[String]): org.apache.avro.Schema = {
    //replace basic types with Union type to allow nulls => optional in parquet,
    // otherwise generated parquet fields will be required
    val regex = "\"type\"\\s*:\\s*\"(int|double|float|boolean|long|string)\"".r
    val modifiedAvroSchemaStr = regex.replaceAllIn(avroSchemaStr, "\"type\" : [\"$1\",\"null\"]")

    val arrayRegex = "\\{\"type\"\\s*:\\s*\"array\"(.)*\"\\}".r
    val modAvroSchemaStr = arrayRegex.replaceAllIn(modifiedAvroSchemaStr, "[$0,\"null\"]")

    if (LOG.isInfoEnabled) LOG.info("final avro schema: \n" + modAvroSchemaStr)

    val finalAvroSchemaStr =
      if (ignoreFields != null && ignoreFields.size > 0) {
        val ignoreKeys = ignoreFields.toSet
        val avroSchemaMap = parse(modifiedAvroSchemaStr).values.asInstanceOf[scala.collection.immutable.Map[String, Any]]
        val modAvroSchemaMap =
          avroSchemaMap.map(mfld =>
            if (mfld._1.equals("fields")) {
              val arrflds = mfld._2.asInstanceOf[List[Any]]
              val filterdArrFlds = arrflds.filter(fld =>
                if (fld.isInstanceOf[scala.collection.immutable.Map[String, Any]]) {
                  val mp = fld.asInstanceOf[scala.collection.immutable.Map[String, Any]]
                  val nm = mp.getOrElse("name", "").toString.trim
                  (!ignoreKeys.contains(nm))
                } else {
                  true
                }
              )
              (mfld._1, filterdArrFlds)
            } else {
              mfld
            }
          )

        implicit val jsonFormats: Formats = DefaultFormats
        val lst = List(modAvroSchemaMap)
        val str = Serialization.write(lst)
        val filteredAvroSchemaStr =
          if (str.size > 2) {
            str.substring(1, str.size - 1)
          } else {
            ""
          }
        if (LOG.isInfoEnabled) LOG.info("filteredAvroSchemaStr:" + filteredAvroSchemaStr)
        filteredAvroSchemaStr
      } else {
        modAvroSchemaStr
      }

    val avroSchema = new org.apache.avro.Schema.Parser().parse(finalAvroSchemaStr)
    avroSchema
  }

  /** **************************************************************/
  /** ******Avro Parquet Writer *********/
  def createAvroParquetWriter(fc: SmartFileProducerConfiguration, schema: Schema,
                              filePath: String, compression: CompressionCodecName): AvroParquetWriter[GenericRecord] = {

    val writeToHdfs = fc.uri.startsWith("hdfs://")
    val path =
      if (writeToHdfs) new Path(filePath)
      else {
        val outputParquetFile = new java.io.File(filePath)
        new Path(outputParquetFile.toURI)
      }

    val parquetBlockSize = if (fc.parquetBlockSize > 0) fc.parquetBlockSize else ParquetWriter.DEFAULT_BLOCK_SIZE
    val parquetPageSize = if (fc.parquetPageSize > 0) fc.parquetPageSize else ParquetWriter.DEFAULT_PAGE_SIZE

    val parquetWriter =
      if (writeToHdfs) {
        val hadoopConf = createHdfsConfig(fc)
        new AvroParquetWriter[GenericRecord](path, schema, compression,
          parquetBlockSize, parquetPageSize,
          ParquetWriter.DEFAULT_IS_DICTIONARY_ENABLED,
          hadoopConf)
      }
      else
        new AvroParquetWriter[GenericRecord](path, schema, compression,
          parquetBlockSize, parquetPageSize)

    parquetWriter
  }

  /*def writeAvroParquet(fc: SmartFileProducerConfiguration, pf : PartitionFile, messages: Array[ContainerInterface],
                         schema : org.apache.avro.Schema): Unit = {

    messages.foreach(message => {

      val nullFlags = {
        val nullFlagsAny = message.getOrElse(SmartFileProducer.nullFlagsFieldName, null)
        try {
          if (nullFlagsAny == null) null
          else nullFlagsAny.asInstanceOf[Array[Boolean]]
        }
        catch {
          case ex: Exception => null
        }
      }


      val builder = message.getAllAttributeValues.foldLeft(new GenericRecordBuilder(schema)) ((recordBuilder, attr) => {

        if(SmartFileProducer.systemFields.contains(attr.getValueType.getName.toLowerCase))
          setRecordValue(attr, recordBuilder, schema, null)
        else {
          if (nullFlags != null && nullFlags.length > attr.getValueType.getIndex && nullFlags(attr.getValueType.getIndex))
            setRecordValue(attr, recordBuilder, schema, null)
          else
            setRecordValue(attr, recordBuilder, schema, attr.getValue)
        }
      })
      val record = builder.build()

      pf.parquetWriter.write(record)
    })
  }*/

  def setRecordValue(attr: AttributeValue, recordBuilder: GenericRecordBuilder,
                     schema: org.apache.avro.Schema, actualValue: Any): GenericRecordBuilder = {


    val fieldName = attr.getValueType.getName
    if (actualValue != null && attr.getValueType.IsArray()) {
      //for arrays, writer is expecting a java list not array, need to find elements type to convert
      if (schema.getField(fieldName).schema().getName == "union") {
        //all fields are supposed to be unions to support null
        val types = schema.getField(fieldName).schema().getTypes
        val elementType =
          if (types.get(0).getName == "array")
            types.get(0).getElementType.getName
          else if (types.get(1).getName == "array")
            types.get(1).getElementType.getName
          else null //not expected

        if (elementType != null)
          recordBuilder.set(attr.getValueType.getName, toList(actualValue, elementType))
        else recordBuilder
      }
      else recordBuilder
    }
    else recordBuilder.set(attr.getValueType.getName, actualValue)
  }

  def toList[T](ar: Array[T]): java.util.List[T] = {
    val list = new java.util.ArrayList[T]()
    ar.foreach(item => list.add(item))
    list
  }

  def toList(arAny: Any, typ: String): Any = {

    typ.toLowerCase() match {
      case "boolean" => toList[Boolean](arAny.asInstanceOf[Array[Boolean]])
      case "int" => toList[Int](arAny.asInstanceOf[Array[Int]])
      case "long" => toList[Long](arAny.asInstanceOf[Array[Long]])
      case "float" => toList[Float](arAny.asInstanceOf[Array[Float]])
      case "double" => toList[Double](arAny.asInstanceOf[Array[Double]])
      case "string" => toList[String](arAny.asInstanceOf[Array[String]])
      case _ => throw new Exception("Unsopported type: Array  of " + typ)
    }

  }


  /** *************************************************************/
  /** ******Parquet Writer *********/


  def getParquetSchema(avroSchemaStr: String, ignoreFields: Array[String]): parquet.schema.MessageType = {
    val avroSchema: org.apache.avro.Schema = getAvroSchema(avroSchemaStr, ignoreFields)
    val avroSchemaConverter = new parquet.avro.AvroSchemaConverter()
    val parquetSchema = avroSchemaConverter.convert(avroSchema)
    parquetSchema
  }

  def createParquetWriter(fc: SmartFileProducerConfiguration,
                          filePath: String, writeSupport: ParquetWriteSupport,
                          compression: CompressionCodecName): ParquetWriter[Array[Any]] = {

    val writeToHdfs = fc.uri.startsWith("hdfs://")
    val path =
      if (writeToHdfs) new Path(filePath)
      else {
        val outputParquetFile = new java.io.File(filePath)
        new Path(outputParquetFile.toURI)
      }

    val parquetBlockSize = if (fc.parquetBlockSize > 0) fc.parquetBlockSize else ParquetWriter.DEFAULT_BLOCK_SIZE
    val parquetPageSize = if (fc.parquetPageSize > 0) fc.parquetPageSize else ParquetWriter.DEFAULT_PAGE_SIZE

    LOG.debug(">>>>>>>>>>>>>>>>>> parquetBlockSize=" + parquetBlockSize)

    val parquetWriter =
      if (writeToHdfs) {
        val hadoopConf = createHdfsConfig(fc)
        new ParquetWriter[Array[Any]](path, writeSupport, compression,
          parquetBlockSize, parquetPageSize, parquetPageSize, // third one is dictionaryPageSize
          ParquetWriter.DEFAULT_IS_DICTIONARY_ENABLED,
          ParquetWriter.DEFAULT_IS_VALIDATING_ENABLED,
          ParquetWriter.DEFAULT_WRITER_VERSION,
          hadoopConf)
      }
      else
        new ParquetWriter[Array[Any]](path, writeSupport, compression,
          parquetBlockSize, parquetPageSize)

    //reflectionUtil.setParquetRecCount(parquetWriter, 1)

    parquetWriter
  }

  /*def writeParquet(fc: SmartFileProducerConfiguration, pf : PartitionFile, messages: Array[ContainerInterface]): Unit = {
    messages.foreach(message => {
      val msgData : Array[Any] = message.getAllAttributeValues.map(attr => attr.getValue)
      pf.parquetWriter.write(msgData)
    })

  }*/
}
