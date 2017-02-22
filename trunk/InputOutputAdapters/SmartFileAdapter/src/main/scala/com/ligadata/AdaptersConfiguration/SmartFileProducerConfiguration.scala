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

package com.ligadata.AdaptersConfiguration

import com.ligadata.Exceptions.{ KamanjaException, FatalAdapterException }
import com.ligadata.KamanjaBase.{NodeContext}
import com.ligadata.InputOutputAdapterInfo._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import scala.io.Source

class TypeLevelConfiguration {
  var flushBufferSize: Long = 0
  var partitionFormat: String = null
  var partitionFormatString: String = null
  var partitionFormatObjects: List[Any] = null
  var subDirName : String = null
}

class SmartFileProducerConfiguration extends AdapterConfiguration {
  var uri: String = null //folder to write files
  var fileNamePrefix: String = "" // prefix for the file names
  var messageSeparator: String = "" // optional separator inserted between messages
  var compressionString: String = null // If it is null or empty we treat it as TEXT file
  var rolloverInterval: Int = 0 // in minutes. create new output file every rolloverInterval mins 
  var timePartitionFormat: String = null // folder structure for partitions - deprecated
  var partitionFormat: String = null // folder structure for partitions, will support time partition and other fields
  var partitionBuckets: Int = 0 //  number of files to create within a partition
  var useTypeFullNameForPartition: Boolean = true // when true uses full type name to create partition directory
  var replaceSeparator: Boolean = false
  var separatorCharForTypeName: String = "." // this character will be used to replace "." in type full name before creating partition directory 
  var flushBufferSize: Long = 0 // in bytes. writes the buffer after flushBufferSize bytes.
  var flushBufferInterval: Long = 0 // in msecs. writes the buffer every flushBufferInterval msecs
  var typeLevelConfigKey: String = null
  var typeLevelConfigFile: String = null // file name that contains type level override configuration. Will override inline type level config
  var typeLevelConfig: collection.mutable.Map[String, TypeLevelConfiguration] = collection.mutable.Map[String, TypeLevelConfiguration]() // inline type level override configuration
  var parquetBlockSize : Int = 0 //in bytes, when 0 use default : 134217728=(128 * 1024 * 1024) - used for parquet only
  var parquetPageSize : Int = 0  //in bytes, when 0 use default :   1048576=(  1 * 1024 * 1024).- used for parquet only
  val otherConfig = scala.collection.mutable.Map[String, Any]()

  var kerberos: KerberosConfig = null

  var hadoopConfig  : List[(String,String)]=null

  def isParquet = (compressionString != null && compressionString.toLowerCase.startsWith("parquet"))
  def parquetCompression =
    if(!isParquet) ""
    else{
      val tokens = compressionString.split("/")
      if(tokens.length == 1) "UNCOMPRESSED"
      else{
        tokens(1).toLowerCase match{
          case "snappy" => "SNAPPY"
          case "gzip" => "GZIP"
          case "lzo" => "LZO"
          case "uncompressed" => "UNCOMPRESSED"
          case _ => throw new Exception("Unsopported parquet compression " + tokens(1))
        }
      }
    }

}


class KerberosConfig {
  var principal: String = null
  var keytab: String = null
}

object SmartFileProducerConfiguration {
  def getAdapterConfig(nodeContext: NodeContext, config: AdapterConfiguration): SmartFileProducerConfiguration = {
    if (config.adapterSpecificCfg == null || config.adapterSpecificCfg.size == 0) {
      val err = "Not found Type and Connection info for Smart File Adapter Config:" + config.Name
      throw new KamanjaException(err, null)
    }

    val adapCfg = parse(config.adapterSpecificCfg)
    if (adapCfg == null || adapCfg.values == null) {
      val err = "Smart File Producer configuration must be specified for " + config.Name
      throw new KamanjaException(err, null)
    }
    val adapCfgValues = adapCfg.values.asInstanceOf[Map[String, Any]]

    val adapterConfig = getAdapterConfigFromMap(nodeContext, adapCfgValues)

    adapterConfig.Name = config.Name
    adapterConfig.className = config.className
    adapterConfig.jarName = config.jarName
    adapterConfig.dependencyJars = config.dependencyJars
    adapterConfig.fullAdapterConfig = config.fullAdapterConfig
    
    adapterConfig
  }

  def getAdapterConfigFromMap(nodeContext: NodeContext, adapCfgValues: Map[String, Any]): SmartFileProducerConfiguration = {
    val adapterConfig = new SmartFileProducerConfiguration()
/*
    if (adapCfgValues.contains("Name"))
      adapterConfig.Name = adapCfgValues.get("Name").toString.trim
    if (adapCfgValues.contains("ClassName"))
      adapterConfig.className = adapCfgValues.get("ClassName").toString.trim
    if (adapCfgValues.contains("JarName"))
      adapterConfig.jarName = adapCfgValues.get("JarName").toString.trim
    if (adapCfgValues.contains("DependencyJars"))
      adapterConfig.dependencyJars =
*/

    adapCfgValues.foreach(kv => {
      if (kv._1.compareToIgnoreCase("Uri") == 0) {
        adapterConfig.uri = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("FileNamePrefix") == 0) {
        adapterConfig.fileNamePrefix = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("MessageSeparator") == 0) {
        adapterConfig.messageSeparator = kv._2.toString
      } else if (kv._1.compareToIgnoreCase("Compression") == 0) {
        adapterConfig.compressionString = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("RolloverInterval") == 0) {
        adapterConfig.rolloverInterval = kv._2.toString.toInt
      } else if (kv._1.compareToIgnoreCase("TimePartitionFormat") == 0) {
        adapterConfig.timePartitionFormat = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("PartitionFormat") == 0) {
        adapterConfig.partitionFormat = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("PartitionBuckets") == 0) {
        adapterConfig.partitionBuckets = kv._2.toString.toInt
      } else if (kv._1.compareToIgnoreCase("UseTypeFullNameForPartition") == 0) {
        adapterConfig.useTypeFullNameForPartition = kv._2.toString.trim.equalsIgnoreCase("true")
      } else if (kv._1.compareToIgnoreCase("SeparatorCharForTypeName") == 0) {
        adapterConfig.separatorCharForTypeName = kv._2.toString.trim
        adapterConfig.replaceSeparator = !adapterConfig.separatorCharForTypeName.equalsIgnoreCase(".")
      } else if (kv._1.compareToIgnoreCase("flushBufferSize") == 0) {
        adapterConfig.flushBufferSize = kv._2.toString.toLong
      } else if (kv._1.compareToIgnoreCase("flushBufferInterval") == 0) {
        adapterConfig.flushBufferInterval = kv._2.toString.toLong
      } else if (kv._1.compareToIgnoreCase("typeLevelConfigKey") == 0) {
        adapterConfig.typeLevelConfigKey = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("typeLevelConfigFile") == 0) {
        adapterConfig.typeLevelConfigFile = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("typeLevelConfig") == 0) {
        val configs = kv._2.asInstanceOf[List[Map[String, String]]]
        loadTypeLevelConfig(adapterConfig, configs)
      } else if (kv._1.compareToIgnoreCase("Kerberos") == 0) {
        adapterConfig.kerberos = new KerberosConfig()
        val kerbConf = kv._2.asInstanceOf[Map[String, String]]
        adapterConfig.kerberos.principal = kerbConf.getOrElse("Principal", null)
        adapterConfig.kerberos.keytab = kerbConf.getOrElse("Keytab", null)
      }
      else if (kv._1.compareToIgnoreCase("hadoopConfig")==0){
        val hadoopConfig = kv._2.asInstanceOf[Map[String,String]]
        adapterConfig.hadoopConfig= List[(String,String)]()
        hadoopConfig.foreach(hconf =>{
          adapterConfig.hadoopConfig ::=(hconf._1, hconf._2)
        })
      }
      else if (kv._1.compareToIgnoreCase("ParquetBlockSize") == 0) {
        adapterConfig.parquetBlockSize = kv._2.toString.toInt
      }
      else if (kv._1.compareToIgnoreCase("ParquetPageSize") == 0) {
        adapterConfig.parquetPageSize = kv._2.toString.toInt
      } else {
        adapterConfig.otherConfig(kv._1) = kv._2
      }
    })

    if (adapterConfig.uri == null || adapterConfig.uri.size == 0)
      throw FatalAdapterException("Uri should not be NULL or empty for Smart File Producer" + adapterConfig.Name, new Exception("Invalid Parameters"))

    if (!adapterConfig.uri.startsWith("file://") && !adapterConfig.uri.startsWith("hdfs://"))
      throw FatalAdapterException("Uri should start with file:// or hdfs:// for Smart File Producer: " + adapterConfig.Name, new Exception("Invalid Parameters"))
    
    if (adapterConfig.kerberos != null) {
      if (adapterConfig.kerberos.principal == null || adapterConfig.kerberos.principal.size == 0)
        throw FatalAdapterException("Principal should be specified for Kerberos authentication for Smart File Producer: " + adapterConfig.Name, new Exception("Invalid Parameters"))

      if (adapterConfig.kerberos.keytab == null || adapterConfig.kerberos.keytab.size == 0)
        throw FatalAdapterException("Keytab should be specified for Kerberos authentication for Smart File Producer: " + adapterConfig.Name, new Exception("Invalid Parameters"))
    }

    var jsonStr: String = null;
    if (adapterConfig.typeLevelConfigFile != null) {
      var source: Source = null
      try {
        source = Source.fromFile(adapterConfig.typeLevelConfigFile)
        jsonStr = source.mkString
      } catch {
        case e: Throwable => {
          throw FatalAdapterException("Smart File Producer:" + adapterConfig.Name + " - Error parsing config file " + adapterConfig.typeLevelConfigFile, e)
        }
      } finally {
        if (source != null) source.close()
      }
    } else if(adapterConfig.typeLevelConfigKey != null && nodeContext != null && nodeContext.getEnvCtxt() != null && nodeContext.getEnvCtxt()._mgr != null) {
      jsonStr = nodeContext.getEnvCtxt()._mgr.GetUserProperty(nodeContext.getEnvCtxt().getClusterId(), adapterConfig.typeLevelConfigKey)
    }
    
    if(jsonStr != null && !jsonStr.trim.isEmpty()) {
      val tlConfigs = parse(jsonStr)
      if (tlConfigs == null)
        throw new FatalAdapterException("Smart File Producer:" + adapterConfig.Name + " - Invalid JSON in config file " + adapterConfig.typeLevelConfigFile, new Exception("Invalid Parameters"))
      loadTypeLevelConfig(adapterConfig, tlConfigs.values.asInstanceOf[List[Map[String, String]]])
    }
 
    adapterConfig
  }

  private def loadTypeLevelConfig(adapterCfg: SmartFileProducerConfiguration, tlConfigs: List[Map[String, String]]) = {
    tlConfigs.foreach(cfg => {
      //val cfg = x.asInstanceOf[Map[String, String]]
      var typeStr = cfg.getOrElse("type", null)
      if (typeStr != null) {
        //typeStr = typeStr.toLowerCase
        val tlcfg = new TypeLevelConfiguration
        tlcfg.flushBufferSize = cfg.getOrElse("flushBufferSize", "0").toLong
        tlcfg.partitionFormat = cfg.getOrElse("PartitionFormat", null)
        tlcfg.subDirName = cfg.getOrElse("SubDirName", null)

        adapterCfg.typeLevelConfig(typeStr) = tlcfg
      }
    })
  }
}


