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
  var manuallyCreateIndexMapping = false
  var dateFiledNameInOutputMessage: String = ""
  var dateFiledFormat: String = ""
  var indexMapping: String = ""
  var columnDelimiter: String = ""
  var rowkeyIncluded: Boolean = false
  var numberOfThread: Int = 1
  var kerberos: KerberosConfig = null
  var instancePartitions: Set[Int] = _
  var noDataSleepTimeInMs: Int = 300
  var timeToWriteRecs = 60000
  var writeRecsBatch = 1000
}

class KerberosConfig {
  var principal: String = null
  var keytab: String = null
  var masterPrincipal: String = null
  var regionServer: String = null
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
        //      } else if (kv._1.compareToIgnoreCase("serializerName") == 0) {
        //        adapterConfig.serializerName = kv._2.toString
      } else if (kv._1.compareToIgnoreCase("Kerberos") == 0) {
        adapterConfig.kerberos = new KerberosConfig()
        val kerbConf = kv._2.asInstanceOf[Map[String, String]]
        adapterConfig.kerberos.principal = kerbConf.getOrElse("Principal", null)
        adapterConfig.kerberos.keytab = kerbConf.getOrElse("Keytab", null)
        adapterConfig.kerberos.masterPrincipal = kerbConf.getOrElse("masterprincipal", null)
        adapterConfig.kerberos.regionServer = kerbConf.getOrElse("regionserve", null)
      } else if (kv._1.compareToIgnoreCase("numberOfThread") == 0) {
        adapterConfig.numberOfThread = kv._2.toString.trim.toInt
      } else if (kv._1.compareToIgnoreCase("columnDelimiter") == 0) {
        adapterConfig.columnDelimiter = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("IndexMapping") == 0) {
        adapterConfig.indexMapping = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("DateFiledNameInOutputMessage") == 0) {
        adapterConfig.dateFiledNameInOutputMessage = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("DateFiledFormat") == 0) {
        adapterConfig.dateFiledFormat = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("rowkeyIncluded") == 0) {
        adapterConfig.rowkeyIncluded = kv._2.toString.trim.toBoolean
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
    //
    //    if (adapterConfig.serializerName == null || adapterConfig.serializerName.size == 0)
    //      throw new KamanjaException("serializerName should not be NULL or empty for Elasticsearch Producer" + adapterConfig.Name, null)

    if (adapterConfig.kerberos != null) {
      if (adapterConfig.kerberos.principal == null || adapterConfig.kerberos.principal.size == 0)
        throw new KamanjaException("Principal should be specified for Kerberos authentication for Elasticsearch Producer: " + adapterConfig.Name, null)

      if (adapterConfig.kerberos.keytab == null || adapterConfig.kerberos.keytab.size == 0)
        throw new KamanjaException("Keytab should be specified for Kerberos authentication for Elasticsearch Producer: " + adapterConfig.Name, null)
    }

    if (adapterConfig.scehmaName == null)
      throw new KamanjaException("schemaName should be specified to read/write data from Elasticsearch storage for Elasticsearch Producer: " + adapterConfig.Name, null)

    if (inputType.equalsIgnoreCase("input")) {

      if (adapterConfig.numberOfThread == 0)
        throw new KamanjaException("numberOfThread should be more than 0 for Elasticsearch Producer: " + adapterConfig.Name, null)

      if (adapterConfig.TableName == null || adapterConfig.TableName.size == 0)
        throw new KamanjaException("tableName should be specified for Elasticsearch Producer: " + adapterConfig.Name, null)

      if (!adapterConfig.rowkeyIncluded.equals(true) && !adapterConfig.rowkeyIncluded.equals(false)) {
        //throw new KamanjaException("rowkeyIncluded should be specified for Elasticsearch Producer: " + adapterConfig.Name, null)
        logger.info("rowkeyIncluded did not pass, the default value is false.")
        adapterConfig.rowkeyIncluded = false
      }

      if (adapterConfig.columnDelimiter == null || adapterConfig.columnDelimiter.size == 0) {
        // throw new KamanjaException("columnDelimiter should be specified for Elasticsearch Producer: " + adapterConfig.Name, null)
        logger.info("""columnDelimiter did not pass, the default value is ",". """)
        adapterConfig.columnDelimiter = ","
      }
      //      if (adapterConfig.columnName == null || adapterConfig.columnName.size == 0)
      //        throw new KamanjaException("columnName should be specified for Elasticsearch Producer: " + adapterConfig.Name, null)
      //
      //      if (adapterConfig.familyName == null || adapterConfig.familyName.size == 0)
      //        throw new KamanjaException("familyName should be specified for Elasticsearch Producer: " + adapterConfig.Name, null)
    }

    adapterConfig
  }
}

case class ElasticsearchKeyData(Version: Int, Type: String, Name: String, PartitionId: Int)

class ElasticsearchPartitionUniqueRecordKey extends PartitionUniqueRecordKey {
  val Version: Int = 1
  var Name: String = _
  // Name
  val Type: String = "Elasticsearch"
  var PartitionId: Int = _ // Partition Id

  override def Serialize: String = {
    // Making String from key
    val json =
    ("Version" -> Version) ~
      ("Type" -> Type) ~
      ("Name" -> Name) ~
      ("PartitionId" -> PartitionId)
    compact(render(json))
  }

  override def Deserialize(key: String): Unit = {
    // Making Key from Serialized String
    implicit val jsonFormats: Formats = DefaultFormats
    val keyData = parse(key).extract[ElasticsearchKeyData]
    if (keyData.Version == Version && keyData.Type.compareTo(Type) == 0) {
      Name = keyData.Name
      PartitionId = keyData.PartitionId
    }
  }
}

case class ElasticsearchRecData(Version: Int, TableName: String, Key: Option[Long], TimeStamp: Long)

class ElasticsearchPartitionUniqueRecordValue extends PartitionUniqueRecordValue {
  val Version: Int = 1
  var TableName: String = _
  var Key: Long = -1
  // key of next message in the file
  var TimeStamp: Long = -1 // timestamp of next message in the file

  override def Serialize: String = {
    // Making String from Value
    val json =
    ("Version" -> Version) ~
      ("Key" -> Key) ~
      ("TableName" -> TableName) ~
      ("TimeStamp" -> TimeStamp)
    compact(render(json))
  }

  override def Deserialize(key: String): Unit = {
    // Making Value from Serialized String
    implicit val jsonFormats: Formats = DefaultFormats
    val recData = parse(key).extract[ElasticsearchRecData]
    if (recData.Version == Version) {
      Key = recData.Key.get
      TableName = recData.TableName
      TimeStamp = recData.TimeStamp
    }
    // else { } // Not yet handling other versions
  }
}

object KamanjaElasticsearchAdapterConstants {
  // Statistics Keys
  val PARTITION_COUNT_KEYS = "Partition Counts"
  val PARTITION_DEPTH_KEYS = "Partition Depths"
  val EXCEPTION_SUMMARY = "Exception Summary"
}

object ElasticsearchConstants {
  val ELASTICSEARCH_SEND_SUCCESS = 0
  val ELASTICSEARCH_SEND_Q_FULL = 1
  val ELASTICSEARCH_SEND_DEAD_PRODUCER = 2
  val ELASTICSEARCH_NOT_SEND = 3
}