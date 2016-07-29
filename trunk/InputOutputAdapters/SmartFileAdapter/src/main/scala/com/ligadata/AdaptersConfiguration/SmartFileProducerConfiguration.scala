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
import com.ligadata.InputOutputAdapterInfo._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

class SmartFileProducerConfiguration extends AdapterConfiguration {
  var uri: String = null //folder to write files
  var fileNamePrefix: String = "" // prefix for the file names
  var messageSeparator: String = "" // optional separator inserted between messages
  var compressionString: String = null // If it is null or empty we treat it as TEXT file
  var rolloverInterval: Int = 0 // in minutes. create new output file every rolloverInterval mins 
  var partitionFormat: String = null // folder structure for partitions
  var partitionBuckets: Int = 0 //  number of files to create within a partition

  var kerberos: KerberosConfig = null
}

class KerberosConfig {
  var principal: String = null
  var keytab: String = null
}

object SmartFileProducerConfiguration {

  def getAdapterConfig(inputConfig: AdapterConfiguration): SmartFileProducerConfiguration = {

    if (inputConfig.adapterSpecificCfg == null || inputConfig.adapterSpecificCfg.size == 0) {
      val err = "Not found Type and Connection info for Smart File Adapter Config:" + inputConfig.Name
      throw new KamanjaException(err, null)
    }

    val adapterConfig = new SmartFileProducerConfiguration()
    adapterConfig.Name = inputConfig.Name
    adapterConfig.className = inputConfig.className
    adapterConfig.jarName = inputConfig.jarName
    adapterConfig.dependencyJars = inputConfig.dependencyJars

    val adapCfg = parse(inputConfig.adapterSpecificCfg)
    if (adapCfg == null || adapCfg.values == null) {
      val err = "Smart File Producer configuration must be specified for " + adapterConfig.Name
      throw new KamanjaException(err, null)
    }

    val adapCfgValues = adapCfg.values.asInstanceOf[Map[String, Any]]
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
        adapterConfig.partitionFormat = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("PartitionBuckets") == 0) {
        adapterConfig.partitionBuckets = kv._2.toString.toInt
      } else if (kv._1.compareToIgnoreCase("Kerberos") == 0) {
        adapterConfig.kerberos = new KerberosConfig()
        val kerbConf = kv._2.asInstanceOf[Map[String, String]]
        adapterConfig.kerberos.principal = kerbConf.getOrElse("Principal", null)
        adapterConfig.kerberos.keytab = kerbConf.getOrElse("Keytab", null)
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

    adapterConfig
  }
}


