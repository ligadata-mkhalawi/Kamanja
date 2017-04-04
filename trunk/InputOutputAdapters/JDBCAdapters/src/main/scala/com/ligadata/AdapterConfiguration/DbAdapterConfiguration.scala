package com.ligadata.AdaptersConfiguration

import com.ligadata.InputOutputAdapterInfo.{AdapterConfiguration, PartitionUniqueRecordKey, PartitionUniqueRecordValue}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.apache.logging.log4j.LogManager

case class QueryInfo(Query: String, PrimaryKeyColumn: String, PartitionExpression: String, PrimaryKeyExpression: String)

object DbSerializeFormat extends Enumeration {
  type DbSerializeFormatType = Value
  val none, kv, json, delimited  = Value

  def asString(typ: DbSerializeFormatType): String = {
    typ.toString
  }
  def fromString(typeStr: String): DbSerializeFormatType = {
    val typ: DbSerializeFormatType = typeStr.toLowerCase match {
      case "none" => none
      case "kv" => kv
      case "json" => json
      case "delimited" => delimited
      case _ => none
    }
    typ
  }

}

class DbAdapterConfiguration extends AdapterConfiguration {
  //Name of the driver class
  var DriverName: String = _

  //JDBC Connectivity URL
  var URLString: String = _

  //Db User Name for connectivity
  var UserId: String = _

  //Db User Password for connectivity
  var Password: String = _

  var validationQuery = ""

  //Partition Column for splitting the extraction work into partitions
  //Number of partitions (will be defaulted to 1)
  var Consumers: Int = 1

  //Timeout
  var Timeout: Long = 86400000L

  // <= 0 means run once, any greater value, will run continuously
  var RefreshInterval: Long = 30000L

  //Queries
  var queriesInfo = scala.collection.mutable.Map[String, QueryInfo]()

  // For now supporting only KV, JSON & delimited.
  var format = DbSerializeFormat.kv

  // fieldDelimiter is only for kv & delimited. default fieldDelimiter is , for both kv and delimited
  var fieldDelimiter = ","

  // For now this is only for kv & delimited.
  var alwaysQuoteFields = false

  // For now this is only for kv. default value is :
  var keyDelimiter = ":"

  override def toString(): String = {
    "(DriverName " + DriverName + "," + "URLString " + URLString + "," + "UserId " + UserId + "," +
      "Password " + Password + "," + "ValidationQuery " + validationQuery + "," + "Consumers " + Consumers + "," + "Timeout " + Timeout + "," +
      "RefreshInterval " + RefreshInterval + "," + "format " + format.toString + "," + "fieldDelimiter " + fieldDelimiter + "," +
      "alwaysQuoteFields " + alwaysQuoteFields + "," + "keyDelimiter " + keyDelimiter + "," + "queriesInfo " + queriesInfo.mkString("~") + "," +
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

    val values = adapCfg.values.asInstanceOf[Map[String, Any]]

    values.foreach(kv => {
      if (kv._1.compareToIgnoreCase("DriverName") == 0) {
        dbAdpt.DriverName = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("UserId") == 0) {
        dbAdpt.UserId = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("Password") == 0) {
        dbAdpt.Password = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("URLString") == 0) {
        dbAdpt.URLString = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("ValidationQuery") == 0) {
        dbAdpt.validationQuery = kv._2.toString.trim
      } else if (kv._1.compareToIgnoreCase("Consumers") == 0) {
        dbAdpt.Consumers = kv._2.toString.trim.toInt
        if (dbAdpt.Consumers <= 0)
          dbAdpt.Consumers = 1
      } else if (kv._1.compareToIgnoreCase("Timeout") == 0) {
        val timeout = kv._2.toString.trim.toLong
        if (timeout > 0)
          dbAdpt.Timeout = timeout
        else
          LOG.error("Gave Timeout <= 0. Taking default value as " + dbAdpt.Timeout)
      } else if (kv._1.compareToIgnoreCase("RefreshInterval") == 0) {
        val refreshInterval = kv._2.toString.trim.toLong
        if (refreshInterval > 0)
          dbAdpt.RefreshInterval = refreshInterval
        else
          LOG.error("Gave RefreshInterval <= 0. Taking default value as " + dbAdpt.RefreshInterval)
      } else if (kv._1.compareToIgnoreCase("Queries") == 0) {
        val qs = if (kv._2.isInstanceOf[Map[String, Any]]) kv._2.asInstanceOf[Map[String, Any]] else null
        if (qs != null) {
          qs.foreach(kv => {
            val qryUniqId = kv._1.trim.toLowerCase
            if (!qryUniqId.isEmpty) {
              if (kv._2.isInstanceOf[Map[String, String]]) {
                val qryInfo = kv._2.asInstanceOf[Map[String, String]]
                var qry = qryInfo.getOrElse("Query", "")
                var primaryKeyColumn = qryInfo.getOrElse("PrimaryKeyColumn", "")
                var partitionExpression = qryInfo.getOrElse("PartitionExpression", "")
                var primaryKeyExpression = qryInfo.getOrElse("PrimaryKeyExpression", "")
                qry = if (qry == null) "" else qry.toString.trim
                primaryKeyColumn = if (primaryKeyColumn == null) "" else primaryKeyColumn.toString.trim
                partitionExpression = if (partitionExpression == null) "" else partitionExpression.toString.trim
                primaryKeyExpression = if (primaryKeyExpression == null) "" else primaryKeyExpression.toString.trim
                if (qry.isEmpty || primaryKeyColumn.isEmpty || partitionExpression.isEmpty || primaryKeyExpression.isEmpty) {
                  LOG.error("Query information (Query:%s, PrimaryKeyColumn:%s, PartitionExpression:%s, PrimaryKeyExpression:%s) for QueryUniqueKey:%s is incomplete. Not going to use it for quering information.".format(qry, primaryKeyColumn, partitionExpression, primaryKeyExpression, qryUniqId))
                } else {
                  if (! qry.contains("${PrimaryAndPartitionSubstitution}")) {
                    LOG.error("Query:%s for QueryUniqueKey:%s does not contain ${PrimaryAndPartitionSubstitution}. Adapter may not work properly.".format(qry, qryUniqId))
                  }
                  if ((! partitionExpression.contains("${MaxPartitions}")) || (! partitionExpression.contains("${PartitionId}"))) {
                    LOG.error("PartitionExpression:%s for QueryUniqueKey:%s does not contain either ${MaxPartitions} or ${PartitionId}. Adapter may not work properly.".format(partitionExpression, qryUniqId))
                  }
                  if (! primaryKeyExpression.contains("${PreviousPrimaryKeyValue}")) {
                    LOG.error("PrimaryKeyExpression:%s for QueryUniqueKey:%s does not contain ${PreviousPrimaryKeyValue}. Adapter may not work properly.".format(primaryKeyExpression, qryUniqId))
                  }
                  dbAdpt.queriesInfo(qryUniqId) = QueryInfo(qry, primaryKeyColumn, partitionExpression, primaryKeyExpression)
                }
              }
            } else {
              LOG.error("Found empty QueryUniqueKey. Not going to use it for quering information.")
            }
          })
        }
      } else if (kv._1.compareToIgnoreCase("Serialize") == 0) {
        val serInfo = if (kv._2.isInstanceOf[Map[String, Any]]) kv._2.asInstanceOf[Map[String, Any]] else null
        if (serInfo != null) {
          val formatVal = serInfo.getOrElse("Format", null)
          val options = serInfo.getOrElse("Options", null)

          if (formatVal != null) {
            val fmt = formatVal.toString.trim.toLowerCase
            dbAdpt.format = DbSerializeFormat.fromString(fmt)
            if (dbAdpt.format != DbSerializeFormat.none) {
              if (dbAdpt.format == DbSerializeFormat.kv || dbAdpt.format == DbSerializeFormat.delimited) {
                var fdSet = false
                var kdSet = false
                var qfSet = false
                if (options != null && options.isInstanceOf[Map[String, String]]) {
                  val opts = options.asInstanceOf[Map[String, String]]
                  val qteFlds = opts.getOrElse("AlwaysQuoteFields", null)
                  val fldDelim = opts.getOrElse("FieldDelimiter", null)
                  val keyDelim = if (dbAdpt.format == DbSerializeFormat.kv) opts.getOrElse("KeyDelimiter", null) else null
                  if (qteFlds != null) {
                    qfSet = true
                    dbAdpt.alwaysQuoteFields = qteFlds.toString.toBoolean
                  }
                  if (fldDelim != null) {
                    fdSet = true
                    dbAdpt.fieldDelimiter = fldDelim.toString
                  }
                  if (keyDelim != null) {
                    kdSet = true
                    dbAdpt.keyDelimiter = keyDelim.toString
                  }
                }
                if (! qfSet)
                  dbAdpt.alwaysQuoteFields = false
                if (! fdSet)
                  dbAdpt.fieldDelimiter = ","
                if (dbAdpt.format == DbSerializeFormat.kv && ! kdSet)
                  dbAdpt.keyDelimiter = ":"
              }
            } else {
              LOG.error("Found Format as %s. We support only kv, delimited or json. Taking default Format as delimited and FieldDelimiter as \",\"".format(fmt))
            }
          }
        }
      }
    })

    if (dbAdpt.queriesInfo.isEmpty) {
      LOG.error("Not found any query information for adapter:%s. Not going to query any data for this consumer.".format(inputConfig.Name))
    }

    dbAdpt
  }
}

case class DbKeyData(Version: Int, Type: String, Name: String, PartitionId: Int)

class DbPartitionUniqueRecordKey extends PartitionUniqueRecordKey {
  val Version: Int = 1
  val Type: String = "Database"
  // Name
  var Name: String = _
  // Partition Id
  var PartitionId: Int = _

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
      ("QueryId2Primary" -> QueryId2Primary.toMap.map(kv => (kv._1 -> kv._2)))

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