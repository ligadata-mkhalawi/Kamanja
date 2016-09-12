package com.ligadata.OutputAdapters

import com.ligadata.AdaptersConfiguration.SmartFileProducerConfiguration
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation
import parquet.hadoop.ParquetWriter
import parquet.hadoop.metadata.CompressionCodecName
import org.apache.hadoop.fs.Path

/**
  * Created by Yasser on 9/11/2016.
  */
object Utils {

  def createHdfsConfig(fc: SmartFileProducerConfiguration) : Configuration = {
    var hdfsConf: Configuration = new Configuration();
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

  def createParquetWriter(fc: SmartFileProducerConfiguration,
                          filePath : String, writeSupport : ParquetWriteSupport,
                          compression : CompressionCodecName) : ParquetWriter[Array[Any]] = {

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
        new ParquetWriter[Array[Any]](path, writeSupport, compression,
          parquetBlockSize, parquetPageSize, parquetPageSize,  // third one is dictionaryPageSize
          ParquetWriter.DEFAULT_IS_DICTIONARY_ENABLED,
          ParquetWriter.DEFAULT_IS_VALIDATING_ENABLED,
          ParquetWriter.DEFAULT_WRITER_VERSION,
          hadoopConf)
      }
      else
        new ParquetWriter[Array[Any]](path, writeSupport, compression,
          parquetBlockSize, parquetPageSize)

    parquetWriter
  }
}
