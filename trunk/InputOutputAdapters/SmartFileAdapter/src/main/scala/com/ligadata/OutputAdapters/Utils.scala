package com.ligadata.OutputAdapters

import com.ligadata.AdaptersConfiguration.SmartFileProducerConfiguration
import com.ligadata.KamanjaBase.ContainerInterface
import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation
import parquet.hadoop.ParquetWriter
import parquet.hadoop.metadata.CompressionCodecName
import org.apache.hadoop.fs.Path
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.file.DataFileWriter;
import parquet.avro.AvroParquetWriter;

/**
  * Created by Yasser on 9/11/2016.
  */
object Utils {

  def createHdfsConfig(fc: SmartFileProducerConfiguration) : Configuration = {
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
  
  def getAvroSchema(record: ContainerInterface): org.apache.avro.Schema ={

    //replace basic types with Union type to allow nulls => optional in parquet,
    // otherwise generated parquet fields will be required
    val regex = "\"type\"\\s*:\\s*\"(int|double|float|boolean|long|string)\"".r
    val modifiedAvroSchemaStr = regex.replaceAllIn(record.getAvroSchema, "\"type\" : [\"$1\",\"null\"]")

    val avroSchema = new org.apache.avro.Schema.Parser().parse(modifiedAvroSchemaStr)
    avroSchema
  }

  def createAvroParquetWriter(fc: SmartFileProducerConfiguration, schema : Schema,
                          filePath : String, compression : CompressionCodecName) : AvroParquetWriter[GenericRecord] = {

    val writeToHdfs = fc.uri.startsWith("hdfs://")
    val path =
      if (writeToHdfs) new Path(filePath)
      else {
        val outputParquetFile = new java.io.File(filePath)
        new Path(outputParquetFile.toURI)
      }

    val parquetBlockSize = if(fc.parquetBlockSize > 0) fc.parquetBlockSize else  ParquetWriter.DEFAULT_BLOCK_SIZE
    val parquetPageSize = if(fc.parquetPageSize > 0) fc.parquetPageSize else  ParquetWriter.DEFAULT_PAGE_SIZE

    val parquetWriter =
      if (writeToHdfs){
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
}
