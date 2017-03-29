package com.ligadata.AdaptersConfiguration

import com.ligadata.InputOutputAdapterInfo.{AdapterConfiguration, PartitionUniqueRecordKey, PartitionUniqueRecordValue}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.apache.logging.log4j.LogManager

class DbAdapterConfiguration extends AdapterConfiguration {
  //Name of the driver class
  var DriverName: String = _

  //JDBC Connectivity URL
  var URLString: String = _

  //Db User Name for connectivity
  var UserId: String = _

  //Db User Password for connectivity
  var Password: String = _

  //Partition Column for splitting the extraction work into partitions
  //Number of partitions (will be defaulted to 1)
  var Consumers: Int = 1

  //Timeout
  var Timeout: Long = 86400000L

  // <= 0 means run once, any greater value, will run continuously
  var RefreshInterval: Long = 0L

  //Serialize
  var Serialize: String = _

  //Queries
  var queries: String = _

  override def toString(): String = {
    "(DriverName " + DriverName + "," + "UserId " + UserId + "," +
      "Password " + Password + "," + "URLString " + URLString + "," +
      "Consumers " + Consumers + "," + "RefreshInterval " + RefreshInterval + ","
      "dependencyJars " + dependencyJars + "," + "jarName " + jarName + ")";
  }

}

object DbAdapterConfiguration {
  private[this] val LOG = LogManager.getLogger(getClass);

  def getAdapterConfig(inputConfig: AdapterConfiguration): DbAdapterConfiguration = {
    if (inputConfig.adapterSpecificCfg == null || inputConfig.adapterSpecificCfg.size == 0) {
      val err = "Not found Db Adapter Config:" + inputConfig.Name
      throw new Exception(err)
    }

    val dbAdpt = new DbAdapterConfiguration

    dbAdpt.Name = inputConfig.Name
    dbAdpt.className = inputConfig.className
    dbAdpt.jarName = inputConfig.jarName
    dbAdpt.dependencyJars = inputConfig.dependencyJars
    dbAdpt.adapterSpecificCfg = inputConfig.adapterSpecificCfg
    dbAdpt.tenantId = inputConfig.tenantId

    val adapCfg = parse(inputConfig.adapterSpecificCfg)
    if (adapCfg == null || adapCfg.values == null) {
      val err = "Not found DB Adapter Config:" + inputConfig.Name
      throw new Exception(err)
    }

    val values = adapCfg.values.asInstanceOf[Map[String, String]]

    values.foreach(kv => {
      if (kv._1.compareToIgnoreCase("DriverName") == 0) {
        dbAdpt.DriverName = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("UserId") == 0) {
        dbAdpt.UserId = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("Password") == 0) {
        dbAdpt.Password = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("URLString") == 0) {
        dbAdpt.URLString = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("Consumers") == 0) {
        dbAdpt.Consumers = kv._2.trim.toInt
      } else if (kv._1.compareToIgnoreCase("RefreshInterval") == 0) {
        dbAdpt.RefreshInterval = kv._2.trim.toLong
      }
    })

    dbAdpt
  }
}

case class DbKeyData(Version: Int, Type: String, Name: String, QueryUniqStr: String, PartitionId: Int)

class DbPartitionUniqueRecordKey extends PartitionUniqueRecordKey {
  val Version: Int = 1
  val Type: String = "JdbcDatabase"
  var Name: String = _  // Name
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
    val keyData = parse(key).extract[DbKeyData]
    if (keyData.Version == Version && keyData.Type.compareTo(Type) == 0) {
      Name = keyData.Name
      PartitionId = keyData.PartitionId
    }
    // else { } // Not yet handling other versions
  }
}

case class DbRecData(Version: Int, QueryId2Primary: Map[String, String])

class DbPartitionUniqueRecordValue extends PartitionUniqueRecordValue {
  val Version: Int = 1

  // Primary Key Column Value where each value is unique for each query
  var QueryId2Primary = scala.collection.mutable.Map[String, String]()

  def Serialize: String = {
    // Making String from Value
    val json =
    ("Version" -> Version) ~
      ("QueryId2Primary" -> QueryId2Primary.toMap.map(kv => (kv._1 -> kv._2) ))

    compact(render(json))
  }

  def Deserialize(key: String): Unit = {
    // Making Value from Serialized String
    implicit val jsonFormats: Formats = DefaultFormats
    val recData = parse(key).extract[DbRecData]
    if (recData.Version == Version) {
      QueryId2Primary ++= recData.QueryId2Primary
    }
    // else { } // Not yet handling other versions
  }
}