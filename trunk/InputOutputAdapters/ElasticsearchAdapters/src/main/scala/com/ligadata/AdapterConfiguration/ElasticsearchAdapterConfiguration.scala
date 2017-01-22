package com.ligadata.AdapterConfiguration

import com.ligadata.Exceptions.KamanjaException
import com.ligadata.InputOutputAdapterInfo.{AdapterConfiguration, PartitionUniqueRecordKey, PartitionUniqueRecordValue}
import com.ligadata.kamanja.metadata.LogTrait
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._


class ElasticsearchAdapterConfiguration extends AdapterConfiguration {
  var hostList: String = null
  //folder to write files
  var scehmaName: String = ""
  // prefix for the file names
  var TableName: String = ""
  // optional separator inserted between messages
  var clusterName: String = ""
  var portNumber: String = ""
  var location: String = ""
  var properties = Map[String, Any]()
  // optional
  var manuallyCreateIndexMapping = false
  var indexMapping: String = ""
  var instancePartitions: Set[Int] = _
  var noDataSleepTimeInMs: Int = 300
  var timeToWriteRecs = 60000
  var writeRecsBatch = 1000
}

object ElasticsearchAdapterConfiguration extends LogTrait {

  def getAdapterConfig(inputConfig: AdapterConfiguration, inputType: String): ElasticsearchAdapterConfiguration = {

    if (inputConfig.adapterSpecificCfg == null || inputConfig.adapterSpecificCfg.size == 0) {
      val err = "Not found Type and Connection info for Elasticsearch Adapter Config:" + inputConfig.Name
      throw new KamanjaException(err, null)
    }

    val adapterConfig = new ElasticsearchAdapterConfiguration()
    adapterConfig.Name = inputConfig.Name
    adapterConfig.className = inputConfig.className
    adapterConfig.jarName = inputConfig.jarName
    adapterConfig.dependencyJars = inputConfig.dependencyJars
    adapterConfig.tenantId = inputConfig.tenantId

    val adapCfg = parse(inputConfig.adapterSpecificCfg)
    if (adapCfg == null || adapCfg.values == null) {
      val err = "Elasticsearch File Producer configuration must be specified for " + adapterConfig.Name
      throw new KamanjaException(err, null)
    }

    val adapCfgValues = adapCfg.values.asInstanceOf[Map[String, Any]]
    adapCfgValues.foreach(kv => {
      if (kv._1.compareToIgnoreCase("hostList") == 0) {
        adapterConfig.hostList = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("clustername") == 0) {
        adapterConfig.clusterName = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("properties") == 0) {
        adapterConfig.properties = kv._2.asInstanceOf[Map[String, Any]]
      } else if (kv._1.compareToIgnoreCase("location") == 0) {
        adapterConfig.location = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("portnumber") == 0) {
        adapterConfig.portNumber = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("scehmaName") == 0) {
        adapterConfig.scehmaName = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("tableName") == 0) {
        adapterConfig.TableName = kv._2.toString
      }  else if (kv._1.compareToIgnoreCase("IndexMapping") == 0) {
        adapterConfig.indexMapping = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("ManuallyCreateIndexMapping") == 0) {
        adapterConfig.manuallyCreateIndexMapping = kv._2.toString.trim.toBoolean
      }else if (kv._1.compareToIgnoreCase("timeToWriteRecs") == 0) {
        adapterConfig.timeToWriteRecs = kv._2.toString.trim.toInt
      }else if (kv._1.compareToIgnoreCase("writeRecsBatch") == 0) {
        adapterConfig.writeRecsBatch = kv._2.toString.trim.toInt
      }
    })

    adapterConfig.instancePartitions = Set[Int]()

    if (adapterConfig.hostList == null || adapterConfig.hostList.size == 0)
      throw new KamanjaException("hostList should not be NULL or empty for Elasticsearch Producer" + adapterConfig.Name, null)


    if (adapterConfig.scehmaName == null)
      throw new KamanjaException("schemaName should be specified to read/write data from Elasticsearch storage for Elasticsearch Producer: " + adapterConfig.Name, null)

    if (inputType.equalsIgnoreCase("input")) {

      if (adapterConfig.TableName == null || adapterConfig.TableName.size == 0)
        throw new KamanjaException("tableName should be specified for Elasticsearch Producer: " + adapterConfig.Name, null)

    }

    adapterConfig
  }
}

