package com.ligadata.adapterconfiguration

import com.ligadata.Exceptions.{FatalAdapterException, KamanjaException}
import com.ligadata.HbaseInputOutputAdapters.LogTrait
import com.ligadata.InputOutputAdapterInfo.{AdapterConfiguration, PartitionUniqueRecordKey, PartitionUniqueRecordValue}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

/**
  * Created by Yousef on 8/13/2016.
  */
class HbaseAdapterConfiguration extends AdapterConfiguration{
  var hostList: String = null //folder to write files
  var scehmaName: String = "" // prefix for the file names
  var TableName: String = "" // optional separator inserted between messages
//  var serializerName: String =""
  var columnName: String = ""
  var familyName: String = ""
  var columnDelimiter: String = ""
  var rowkeyIncluded: Boolean = false
  var numberOfThread: Int = 1
  var kerberos: KerberosConfig = null
  var instancePartitions: Set[Int] = _
  var noDataSleepTimeInMs: Int = 300
}

class KerberosConfig {
  var principal: String = null
  var keytab: String = null
  var masterPrincipal: String = null
  var regionServer: String = null
}

object HbaseAdapterConfiguration extends LogTrait{

  def getAdapterConfig(inputConfig: AdapterConfiguration, inputType: String): HbaseAdapterConfiguration = {

    if (inputConfig.adapterSpecificCfg == null || inputConfig.adapterSpecificCfg.size == 0) {
      val err = "Not found Type and Connection info for Hbase Adapter Config:" + inputConfig.Name
      throw new KamanjaException(err, null)
    }

    val adapterConfig = new HbaseAdapterConfiguration()
    adapterConfig.Name = inputConfig.Name
    adapterConfig.className = inputConfig.className
    adapterConfig.jarName = inputConfig.jarName
    adapterConfig.dependencyJars = inputConfig.dependencyJars
    adapterConfig.tenantId = inputConfig.tenantId

    val adapCfg = parse(inputConfig.adapterSpecificCfg)
    if (adapCfg == null || adapCfg.values == null) {
      val err = "Hbase File Producer configuration must be specified for " + adapterConfig.Name
      throw new KamanjaException(err, null)
    }

    val adapCfgValues = adapCfg.values.asInstanceOf[Map[String, Any]]
    adapCfgValues.foreach(kv => {
      if (kv._1.compareToIgnoreCase("hostList") == 0) {
        adapterConfig.hostList = kv._2.toString.trim
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
      } else if (kv._1.compareToIgnoreCase("columnName") == 0) {
        adapterConfig.columnName = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("numberOfThread") == 0) {
        adapterConfig.numberOfThread = kv._2.toString.trim.toInt
      } else if (kv._1.compareToIgnoreCase("familyName") == 0) {
        adapterConfig.familyName = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("columnDelimiter") == 0) {
        adapterConfig.columnDelimiter = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("rowkeyIncluded") == 0) {
        adapterConfig.rowkeyIncluded = kv._2.toString.trim.toBoolean
      }
    })

    adapterConfig.instancePartitions = Set[Int]()

    if (adapterConfig.hostList == null || adapterConfig.hostList.size == 0)
      throw new KamanjaException("hostList should not be NULL or empty for Hbase Producer" + adapterConfig.Name, null)
//
//    if (adapterConfig.serializerName == null || adapterConfig.serializerName.size == 0)
//      throw new KamanjaException("serializerName should not be NULL or empty for Hbase Producer" + adapterConfig.Name, null)

    if (adapterConfig.kerberos != null) {
      if (adapterConfig.kerberos.principal == null || adapterConfig.kerberos.principal.size == 0)
        throw new KamanjaException("Principal should be specified for Kerberos authentication for Hbase Producer: " + adapterConfig.Name, null)

      if (adapterConfig.kerberos.keytab == null || adapterConfig.kerberos.keytab.size == 0)
        throw new KamanjaException("Keytab should be specified for Kerberos authentication for Hbase Producer: " + adapterConfig.Name, null)
    }

    if (adapterConfig.scehmaName == null)
      throw new KamanjaException("schemaName should be specified to read/write data from Hbase storage for Hbase Producer: " + adapterConfig.Name, null)

    if(inputType.equalsIgnoreCase("input")){

      if (adapterConfig.numberOfThread == 0)
        throw new KamanjaException("numberOfThread should be more than 0 for Hbase Producer: " + adapterConfig.Name, null)

      if (adapterConfig.TableName == null || adapterConfig.TableName.size == 0)
        throw new KamanjaException("tableName should be specified for Hbase Producer: " + adapterConfig.Name, null)

      if (!adapterConfig.rowkeyIncluded.equals(true) && !adapterConfig.rowkeyIncluded.equals(false)) {
        //throw new KamanjaException("rowkeyIncluded should be specified for Hbase Producer: " + adapterConfig.Name, null)
        logger.info("rowkeyIncluded did not pass, the default value is false.")
        adapterConfig.rowkeyIncluded = false
      }

      if (adapterConfig.columnDelimiter == null || adapterConfig.columnDelimiter.size == 0) {
       // throw new KamanjaException("columnDelimiter should be specified for Hbase Producer: " + adapterConfig.Name, null)
        logger.info("""columnDelimiter did not pass, the default value is ",". """)
        adapterConfig.columnDelimiter = ","
      }
//      if (adapterConfig.columnName == null || adapterConfig.columnName.size == 0)
//        throw new KamanjaException("columnName should be specified for Hbase Producer: " + adapterConfig.Name, null)
//
//      if (adapterConfig.familyName == null || adapterConfig.familyName.size == 0)
//        throw new KamanjaException("familyName should be specified for Hbase Producer: " + adapterConfig.Name, null)
    }

    adapterConfig
  }
}

case class HbaseKeyData(Version: Int, Type: String, Name: String, PartitionId: Int)

class HbasePartitionUniqueRecordKey extends PartitionUniqueRecordKey {
  val Version: Int = 1
  var Name: String = _ // Name
  val Type: String = "Hbase"
  var PartitionId: Int = _ // Partition Id

  override def Serialize: String = { // Making String from key
  val json =
    ("Version" -> Version) ~
      ("Type" -> Type) ~
      ("Name" -> Name) ~
      ("PartitionId" -> PartitionId)
    compact(render(json))
  }

  override def Deserialize(key: String): Unit = { // Making Key from Serialized String
  implicit val jsonFormats: Formats = DefaultFormats
    val keyData = parse(key).extract[HbaseKeyData]
    if (keyData.Version == Version && keyData.Type.compareTo(Type) == 0) {
      Name = keyData.Name
      PartitionId = keyData.PartitionId
    }
  }
}

case class HbaseRecData(Version: Int, TableName : String, Key: Option[Long], TimeStamp: Long)

class HbasePartitionUniqueRecordValue extends PartitionUniqueRecordValue {
  val Version: Int = 1
  var TableName : String = _
  var Key: Long = -1 // key of next message in the file
  var TimeStamp: Long = -1 // timestamp of next message in the file

  override def Serialize: String = {
    // Making String from Value
    val json =
      ("Version" -> Version) ~
        ("Key" -> Key) ~
        ("TableName" -> TableName)~
        ("TimeStamp" -> TimeStamp)
    compact(render(json))
  }

  override def Deserialize(key: String): Unit = {
    // Making Value from Serialized String
    implicit val jsonFormats: Formats = DefaultFormats
    val recData = parse(key).extract[HbaseRecData]
    if (recData.Version == Version) {
      Key = recData.Key.get
      TableName = recData.TableName
      TimeStamp = recData.TimeStamp
    }
    // else { } // Not yet handling other versions
  }
}

object KamanjaHbaseAdapterConstants {
  // Statistics Keys
  val PARTITION_COUNT_KEYS = "Partition Counts"
  val PARTITION_DEPTH_KEYS = "Partition Depths"
  val EXCEPTION_SUMMARY = "Exception Summary"
}

object HbaseConstants {
  val HBASE_SEND_SUCCESS = 0
  val HBASE_SEND_Q_FULL = 1
  val HBASE_SEND_DEAD_PRODUCER = 2
  val HBASE_NOT_SEND = 3
}