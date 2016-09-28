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
package com.ligadata.keyvaluestore

import java.net.{InetAddress, URL, URLClassLoader}
import java.sql.{Connection, Driver, DriverPropertyInfo, ResultSet}
import java.text.SimpleDateFormat
import java.util.{Properties, TimeZone}

import com.ligadata.Exceptions._
import com.ligadata.KamanjaBase.NodeContext
import com.ligadata.KvBase.{Key, TimeRange, Value}
import com.ligadata.StorageBase.{DataStore, StorageAdapterFactory, Transaction}
import com.ligadata.Utils.KamanjaLoaderInfo
import com.ligadata.kamanja.metadata.AdapterInfo
import org.apache.commons.codec.binary.Base64
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.{XContentBuilder, XContentFactory}
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.{SortOrder, SortParseElement}
import org.elasticsearch.search.{SearchHit, SearchHits}
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.collection.mutable.{ArrayBuffer, TreeSet}
import scala.util.control.Breaks._


class JdbcClassLoader(urls: Array[URL], parent: ClassLoader) extends URLClassLoader(urls, parent) {
  override def addURL(url: URL) {
    super.addURL(url)
  }
}

class DriverShim(d: Driver) extends Driver {
  private var driver: Driver = d

  def connect(u: String, p: Properties): Connection = this.driver.connect(u, p)

  def acceptsURL(u: String): Boolean = this.driver.acceptsURL(u)

  def getPropertyInfo(u: String, p: Properties): Array[DriverPropertyInfo] = this.driver.getPropertyInfo(u, p)

  def getMajorVersion(): Int = this.driver.getMajorVersion

  def getMinorVersion(): Int = this.driver.getMinorVersion

  def jdbcCompliant(): Boolean = this.driver.jdbcCompliant()

  def getParentLogger(): java.util.logging.Logger = this.driver.getParentLogger()
}

class ElasticsearchAdapter(val kvManagerLoader: KamanjaLoaderInfo, val datastoreConfig: String, val nodeCtxt: NodeContext, val adapterInfo: AdapterInfo) extends DataStore {

  private[this] val lock = new Object

  val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  val adapterConfig = if (datastoreConfig != null) datastoreConfig.trim else ""
  //  val loggerName = this.getClass.getName
  //  val logger = LogManager.getLogger(loggerName)

  private val loadedJars: TreeSet[String] = new TreeSet[String];
  private val clsLoader = new JdbcClassLoader(ClassLoader.getSystemClassLoader().asInstanceOf[URLClassLoader].getURLs(), getClass().getClassLoader())

  private var containerList: scala.collection.mutable.Set[String] = scala.collection.mutable.Set[String]()
  private var msg: String = ""

  private def CreateConnectionException(msg: String, ie: Exception): StorageConnectionException = {
    logger.error(msg, ie)
    val ex = new StorageConnectionException("Failed to connect to Database", ie)
    ex
  }

  private def CreateDMLException(msg: String, ie: Exception): StorageDMLException = {
    logger.error(msg, ie)
    val ex = new StorageDMLException("Failed to execute select/insert/delete/update operation on Database", ie)
    ex
  }

  private def CreateDDLException(msg: String, ie: Exception): StorageDDLException = {
    logger.error(msg, ie)
    val ex = new StorageDDLException("Failed to execute create/drop operations on Database", ie)
    ex
  }

  if (adapterConfig.size == 0) {
    msg = "Invalid Elasticsearch Json Configuration string:" + adapterConfig
    throw CreateConnectionException(msg, new Exception("Invalid Configuration"))
  }

  logger.debug("Elasticsearch configuration:" + adapterConfig)
  var parsed_json: Map[String, Any] = null
  try {
    val json = parse(adapterConfig)
    if (json == null || json.values == null) {
      msg = "Failed to parse Elasticsearch JSON configuration string:" + adapterConfig
      throw CreateConnectionException(msg, new Exception("Invalid Configuration"))
    }
    parsed_json = json.values.asInstanceOf[Map[String, Any]]
  } catch {
    case e: Exception => {
      var msg = "Failed to parse Elasticsearch JSON configuration string:%s.".format(adapterConfig)
      throw CreateConnectionException(msg, e)
    }
  }

  // Getting AdapterSpecificConfig if it has
  var adapterSpecificConfig_json: Map[String, Any] = null

  if (parsed_json.contains("AdapterSpecificConfig")) {
    val adapterSpecificStr = parsed_json.getOrElse("AdapterSpecificConfig", "").toString.trim
    if (adapterSpecificStr.size > 0) {
      try {
        val json = parse(adapterSpecificStr)
        if (json == null || json.values == null) {
          msg = "Failed to parse Elasticsearch Adapter Specific JSON configuration string:" + adapterSpecificStr
          throw CreateConnectionException(msg, new Exception("Invalid Configuration"))
        }
        adapterSpecificConfig_json = json.values.asInstanceOf[Map[String, Any]]
      } catch {
        case e: Exception => {
          msg = "Failed to parse Elasticsearch Adapter Specific JSON configuration string:%s.".format(adapterSpecificStr)
          throw CreateConnectionException(msg, e)
        }
      }
    }
  }

  // Read all Elasticsearch parameters

  var location: String = null;
  if (parsed_json.contains("Location")) {
    location = parsed_json.get("Location").get.toString.trim
  } else {
    throw CreateConnectionException("Unable to find Location in adapterConfig ", new Exception("Invalid adapterConfig"))
  }

  var portNumber: String = null;
  if (parsed_json.contains("portnumber")) {
    portNumber = parsed_json.get("portnumber").get.toString.trim
  } else {
    logger.info("The portnumber is not supplied in adapterConfig, defaults to " + "9300")
    portNumber = "9300"
  }

  var clusterName: String = null;
  if (parsed_json.contains("clusterName")) {
    clusterName = parsed_json.get("clusterName").get.toString.trim
  } else {
    logger.info("The clusterName is not supplied in adapterConfig, defaults to " + "elasticsearch")
    clusterName = "elasticsearch"
  }

  var SchemaName: String = null;
  if (parsed_json.contains("SchemaName")) {
    SchemaName = parsed_json.get("SchemaName").get.toString.trim
  } else {
    logger.info("The SchemaName is not supplied in adapterConfig, defaults to " + "default")
    SchemaName = "default"
  }

  // The following three properties are used for connection pooling
  var maxActiveConnections = 1000
  if (parsed_json.contains("maxActiveConnections")) {
    maxActiveConnections = parsed_json.get("maxActiveConnections").get.toString.trim.toInt
  }

  var maxIdleConnections = 10
  if (parsed_json.contains("maxIdleConnections")) {
    maxIdleConnections = parsed_json.get("maxIdleConnections").get.toString.trim.toInt
  }

  var initialSize = 10
  if (parsed_json.contains("initialSize")) {
    initialSize = parsed_json.get("initialSize").get.toString.trim.toInt
  }

  var maxWaitMillis = 10000
  if (parsed_json.contains("maxWaitMillis")) {
    maxWaitMillis = parsed_json.get("maxWaitMillis").get.toString.trim.toInt
  }

  // some misc optional parameters
  var autoCreateTables = "YES"
  if (parsed_json.contains("autoCreateTables")) {
    autoCreateTables = parsed_json.get("autoCreateTables").get.toString.trim
  }

  logger.info("SchemaName => " + SchemaName)
  logger.info("autoCreateTables  => " + autoCreateTables)


  // Connection
  //  var client = TransportClient.builder().build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(location), portNumber.toInt));

  // set the timezone to UTC for all time values
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

  // verify whether schema exists, It can throw an exception due to authorization issues
  logger.info(" SKIP : Check whether schema exists ..")
  //  var schemaExists: Boolean = false
  //  try {
  //    schemaExists = IsSchemaExists(SchemaName)
  //  } catch {
  //    case e: Exception => {
  //      msg = "Message:%s".format(e.getMessage)
  //      throw CreateDMLException(msg, e)
  //    }
  //  }

  // if the schema doesn't exist, we try to create one. It can still fail due to authorization issues
  //  if (!schemaExists) {
  //    logger.info("Unable to find the schema " + SchemaName + " in the database, attempt to create one ")
  //    try {
  //      CreateSchema(SchemaName)
  //    } catch {
  //      case e: Exception => {
  //        msg = "Message:%s".format(e.getMessage)
  //        throw CreateDDLException(msg, e)
  //      }
  //    }
  //  }
  logger.info("SKIP : Done with schema checking ..")

  // return the date time in the format yyyy-MM-dd HH:mm:ss.SSS
  private def GetCurDtTmStr: String = {
    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis))
  }

  private def getKeySize(k: Key): Int = {
    var bucketKeySize = 0
    k.bucketKey.foreach(bk => {
      bucketKeySize = bucketKeySize + bk.length
    })
    8 + bucketKeySize + 8 + 4
  }

  private def getValueSize(v: Value): Int = {
    v.serializedInfo.length
  }

  private def updateOpStats(operation: String, tableName: String, opCount: Int): Unit = lock.synchronized {
    operation match {
      case "get" => {
        if (_getOps.get(tableName) != None) {
          _getOps(tableName) = _getOps(tableName) + opCount
        }
        else {
          _getOps(tableName) = +opCount
        }
      }
      case "put" => {
        if (_putOps.get(tableName) != None) {
          _putOps(tableName) = _putOps(tableName) + opCount
        }
        else {
          _putOps(tableName) = opCount
        }
      }
      case _ => {
        throw CreateDMLException("Internal Error: Failed to Update Op-stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

  private def updateObjStats(operation: String, tableName: String, objCount: Int): Unit = lock.synchronized {
    operation match {
      case "get" => {
        if (_getObjs.get(tableName) != None) {
          _getObjs(tableName) = _getObjs(tableName) + objCount
        }
        else {
          _getObjs(tableName) = +objCount
        }
      }
      case "put" => {
        if (_putObjs.get(tableName) != None) {
          _putObjs(tableName) = _putObjs(tableName) + objCount
        }
        else {
          _putObjs(tableName) = objCount
        }
      }
      case _ => {
        throw CreateDMLException("Internal Error: Failed to Update Obj-stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

  private def updateByteStats(operation: String, tableName: String, byteCount: Int): Unit = lock.synchronized {
    operation match {
      case "get" => {
        if (_getBytes.get(tableName) != None) {
          _getBytes(tableName) = _getBytes(tableName) + byteCount
        }
        else {
          _getBytes(tableName) = byteCount
        }
      }
      case "put" => {
        if (_putBytes.get(tableName) != None) {
          _putBytes(tableName) = _putBytes(tableName) + byteCount
        }
        else {
          _putBytes(tableName) = byteCount
        }
      }
      case _ => {
        throw CreateDMLException("Internal Error: Failed to Update Byte Stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

  private def getConnection: TransportClient = {
    try {
      val settings = Settings.settingsBuilder().put("cluster.name", clusterName).build()
      var client = TransportClient.builder().settings(settings).build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(location), portNumber.toInt))
      client
    } catch {
      case e: Exception => {
        var msg = "Message:%s".format(e.getMessage)
        throw CreateConnectionException(msg, e)
      }
    }
  }

  //  private def IsSchemaExists(schemaName: String): Boolean = {
  //    var con: TransportClient = null
  //    var pstmt: PreparedStatement = null
  //    var rs: ResultSet = null
  //    var rowCount = 0
  //    var query = ""
  //    try {
  //      con = getConnection
  //      //      query = "SELECT count(*) FROM sys.schemas WHERE name = ?"
  //      //      query = "SELECT count(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE CATALOG_NAME = ?"
  //      //      query = "select count(*) from INFORMATION_SCHEMA.CATALOGS where CATALOG_NAME= UPPER(?)"
  //      query = "select count(*) from INFORMATION_SCHEMA.SCHEMATA where SCHEMA_NAME=UPPER(?)"
  //      pstmt = con.prepareStatement(query)
  //      pstmt.setString(1, schemaName)
  //      rs = pstmt.executeQuery();
  //      while (rs.next()) {
  //        rowCount = rs.getInt(1)
  //      }
  //      if (rowCount > 0) {
  //        return true
  //      } else {
  //        return false
  //      }
  //    } catch {
  //      case e: StorageConnectionException => {
  //        throw e
  //      }
  //      case e: Exception => {
  //        throw new Exception("Failed to verify schema existence for the schema " + schemaName + ":" + "query => " + query, e)
  //      }
  //    } finally {
  //      if (rs != null) {
  //        rs.close
  //      }
  //      if (pstmt != null) {
  //        pstmt.close
  //      }
  //      if (con != null) {
  //        con.close
  //      }
  //    }
  //  }

  //  private def CreateSchema(schemaName: String): Unit = {
  //    var con: Connection = null
  //    var stmt: Statement = null
  //    try {
  //      con = dataSource.getConnection
  //      var query = "create schema " + schemaName
  //      stmt = con.createStatement()
  //      stmt.executeUpdate(query);
  //    } catch {
  //      case e: Exception => {
  //        throw CreateDDLException("Failed to create schema  " + schemaName, e)
  //      }
  //    } finally {
  //      if (stmt != null) {
  //        stmt.close
  //      }
  //      if (con != null) {
  //        con.close
  //      }
  //    }
  //  }

  private def CheckTableExists(containerName: String, apiType: String = "dml"): Unit = {
    try {
      if (containerList.contains(containerName)) {
        return
      } else {
        CreateContainer(containerName.toLowerCase(), apiType)
        containerList.add(containerName)
      }
    } catch {
      case e: Exception => {
        throw new Exception("Failed to create table  " + toFullTableName(containerName), e)
      }
    }
  }

  /**
    * loadJar - load the specified jar into the classLoader
    */

  //  private def LoadJars(jars: Array[String]): Unit = {
  //    // Loading all jars
  //    for (j <- jars) {
  //      val jarNm = j.trim
  //      logger.debug("%s:Processing Jar: %s".format(GetCurDtTmStr, jarNm))
  //      val fl = new File(jarNm)
  //      if (fl.exists) {
  //        try {
  //          if (loadedJars(fl.getPath())) {
  //            logger.info("%s:Jar %s already loaded to class path.".format(GetCurDtTmStr, jarNm))
  //          } else {
  //            clsLoader.addURL(fl.toURI().toURL())
  //            logger.info("%s:Jar %s added to class path.".format(GetCurDtTmStr, jarNm))
  //            loadedJars += fl.getPath()
  //          }
  //        } catch {
  //          case e: Exception => {
  //            val errMsg = "Jar " + jarNm + " failed added to class path."
  //            throw CreateConnectionException(errMsg, e)
  //          }
  //        }
  //      } else {
  //        val errMsg = "Jar " + jarNm + " not found"
  //        throw new Exception(errMsg)
  //      }
  //    }
  //  }

  //  private def toTableName(containerName: String): String = {
  //    // Ideally, we need to check for all restrictions for naming a table
  //    // such as length of the table, special characters etc
  //    // containerName.replace('.','_')
  //    containerName.toLowerCase.replace('.', '_').replace('-', '_')
  //  }

  // made the following function public to make it available to scala test
  // components.
  def toFullTableName(containerName: String): String = {
    (SchemaName + "." + containerName).toLowerCase()
  }

  // accessor used for testing
  override def getTableName(containerName: String): String = {
    toFullTableName(containerName)
  }


  def putJson(containerName: String, data_list: ArrayBuffer[(ArrayBuffer[(String)])]): Unit = {
    var client: TransportClient = null
    val tableName = toFullTableName(containerName)
    //    CheckTableExists(tableName)
    try {
      client = getConnection
      var bulkRequest = client.prepareBulk()
      data_list.foreach({ x =>
        x.foreach({ jsonData =>
          // insert x to table tableName
          println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + jsonData)
          bulkRequest.add(client.prepareIndex(tableName, "type1").setSource(jsonData))
        })
      })
      logger.debug("Executing bulk insert...")
      val bulkResponse = bulkRequest.execute().actionGet()
      println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + bulkRequest.numberOfActions())

    }
    catch {
      case e: Exception => {
        throw CreateDMLException("Failed to save an object in the table " + tableName + ":", e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }


  override def put(containerName: String, key: Key, value: Value): Unit = {
    var client: TransportClient = null
    var tableName = toFullTableName(containerName)
    try {
      CheckTableExists(containerName)
      client = getConnection
      // put is sematically an upsert. An upsert is being implemented using a transact-sql update
      var newBuffer: Array[Byte] = new Array[Byte](value.serializedInfo.length)
      var c: Int = 0
      c = new java.io.ByteArrayInputStream(value.serializedInfo).read(newBuffer, 0, value.serializedInfo.length)
      // for elasticsearch we need the id to update the doc

      // get the Document unique id
      val response = client
        .prepareSearch(tableName)
        .setTypes("type1")
        .setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("timePartition", key.timePartition))
          .must(QueryBuilders.termQuery("bucketKey", key.bucketKey.mkString(",")))
          .must(QueryBuilders.termQuery("transactionId", key.transactionId))
          .must(QueryBuilders.termQuery("rowId", key.rowId))
        ).execute().actionGet()

      var hits: SearchHits = response.getHits()
      var id = ""

      hits.totalHits() match {
        case 0 => id = tableName + System.currentTimeMillis() + System.nanoTime()
        case 1 => id = hits.getAt(0).id()
        case x => System.err.println(" found " + hits.totalHits() + " hits, NOT VALID")
      }


      // index the data
      val builder: XContentBuilder =
      XContentFactory.jsonBuilder().startObject()
        .field("timePartition", key.timePartition)
        .field("bucketKey", key.bucketKey.mkString(","))
        .field("transactionId", key.transactionId)
        .field("rowId", key.rowId)
        .field("schemaId", value.schemaId)
        .field("serializerType", value.serializerType)
        .field("serializedInfo", Base64.encodeBase64String(newBuffer))
        .endObject()

      val indexResponse = client.prepareIndex(tableName, "type1", id)
        .setSource(builder).get()

      client.admin().indices().prepareRefresh(tableName).get()

      logger.info("data indexed into " + tableName)

      updateOpStats("put", tableName, 1)
      updateObjStats("put", tableName, 1)
      updateByteStats("put", tableName, getKeySize(key) + getValueSize(value))
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to save an object in the table " + tableName + ":", e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  private def IsSingleRowPut(data_list: Array[(String, Array[(Key, Value)])]): Boolean = {
    if (data_list.length == 1) {
      if (data_list(0)._2.length == 1) {
        return true
      }
    }
    return false
  }

  override def put(data_list: Array[(String, Array[(Key, Value)])]): Unit = {
    var client: TransportClient = null
    var totalRowsUpdated = 0;
    try {
      if (IsSingleRowPut(data_list)) {
        var containerName = data_list(0)._1
        var isMetadataContainer = data_list(0)._2
        var keyValuePairs = data_list(0)._2
        var key = keyValuePairs(0)._1
        var value = keyValuePairs(0)._2
        put(containerName, key, value)
      } else {
        logger.debug("Get a new connection...")
        client = getConnection

        var bulkRequest = client.prepareBulk()

        var byteCount = 0
        data_list.foreach(f = li => {
          var containerName = li._1
          var tableName = toFullTableName(containerName)
          var keyValuePairs = li._2

          keyValuePairs.foreach(f = keyValuePair => {
            var key = keyValuePair._1
            var value = keyValuePair._2
            // get the Document unique id
            val response = client
              .prepareSearch(tableName)
              .setTypes("type1")
              .setQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("timePartition", key.timePartition))
                .must(QueryBuilders.termQuery("bucketKey", key.bucketKey.mkString(",")))
                .must(QueryBuilders.termQuery("transactionId", key.transactionId))
                .must(QueryBuilders.termQuery("rowId", key.rowId))
              ).execute().actionGet()

            val hits: SearchHits = response.getHits()
            var id = ""

            hits.totalHits() match {
              case 0 => id = tableName + System.currentTimeMillis() + System.nanoTime()
              case 1 => id = hits.getAt(0).id()
              case x => System.err.println(" found " + hits.totalHits() + " hits, NOT VALID")
            }

            // index the data
            val newBuffer: Array[Byte] = new Array[Byte](value.serializedInfo.length)
            var c: Int = 0;
            c = new java.io.ByteArrayInputStream(value.serializedInfo).read(newBuffer, 0, value.serializedInfo.length)

            val builder: XContentBuilder =
              XContentFactory.jsonBuilder().startObject()
                .field("timePartition", key.timePartition)
                .field("bucketKey", key.bucketKey.mkString(","))
                .field("transactionId", key.transactionId)
                .field("rowId", key.rowId)
                .field("schemaId", value.schemaId)
                .field("serializerType", value.serializerType)
                .field("serializedInfo", Base64.encodeBase64String(newBuffer))
                .endObject()

            bulkRequest.add(client.prepareIndex(tableName, "type1", id).setSource(builder))

            byteCount = byteCount + getKeySize(key) + getValueSize(value)
          })
          logger.debug("Executing bulk upsert...")
          totalRowsUpdated = bulkRequest.numberOfActions()
          val bulkResponse = bulkRequest.execute().actionGet()
          client.admin().indices().prepareRefresh(tableName).get()


          if (bulkResponse.hasFailures()) {
            System.err.println(bulkResponse.buildFailureMessage())
          } else {
            logger.info("Inserted/Updated " + totalRowsUpdated + " rows for " + tableName)
          }

          updateOpStats("put", tableName, 1)
          updateObjStats("put", tableName, totalRowsUpdated)
          updateByteStats("put", tableName, byteCount)
        })
      }
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to save a batch of objects into indexes ", e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  // delete operations
  override def del(containerName: String, keys: Array[Key]): Unit = {
    var client: TransportClient = null
    val tableName = toFullTableName(containerName)
    var deleteCount = 0
    try {
      CheckTableExists(containerName)
      client = getConnection
      var bulkRequest = client.prepareBulk()
      keys.foreach(key => {
        val response = client
          .prepareSearch(tableName)
          .setTypes("type1")
          .setQuery(QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery("timePartition", key.timePartition))
            .must(QueryBuilders.termQuery("bucketKey", key.bucketKey.mkString(",").toLowerCase()))
            .must(QueryBuilders.termQuery("transactionid", key.transactionId))
            .must(QueryBuilders.termQuery("rowId", key.rowId))
          ).execute().actionGet()

        var hits: SearchHits = response.getHits()
        var id = ""

        hits.totalHits() match {
          case 0 => id = "noRecords"
          case 1 => id = hits.getAt(0).id()
          case x => System.err.println(" found " + hits.totalHits() + " hits, NOT VALID")
        }
        // create and add delete statement to bulk
        bulkRequest.add(client.prepareDelete(tableName, "type1", id))
      })
      deleteCount = bulkRequest.numberOfActions()
      val bulkResponse = bulkRequest.execute().actionGet()

      if (bulkResponse.hasFailures()) {
        System.err.println(bulkResponse.buildFailureMessage())
      } else {
        logger.info("Deleted " + deleteCount + " rows from " + tableName)
      }

    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to delete object(s) from the table " + tableName + ":", e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  override def del(containerName: String, time: TimeRange, keys: Array[Array[String]]): Unit = {
    var client: TransportClient = null
    var bulkRequest = client.prepareBulk()
    var deleteCount = 0
    var tableName = toFullTableName(containerName)
    try {
      logger.info("begin time => " + dateFormat.format(time.beginTime))
      logger.info("end time => " + dateFormat.format(time.endTime))
      CheckTableExists(containerName)

      client = getConnection

      keys.foreach(keyList => {
        var keyStr = keyList.mkString(",").toLowerCase
        val response = client
          .prepareSearch(tableName)
          .setTypes("type1")
          .setQuery(QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery("bucketKey", keyStr))
            .must(QueryBuilders.rangeQuery("timePartition").gte(time.beginTime))
            .must(QueryBuilders.rangeQuery("timePartition").lte(time.endTime))
          )

          .execute().actionGet()

        var hits: SearchHits = response.getHits()
        var id = ""

        hits.totalHits() match {
          case 0 => id = "noRecords"
          case 1 => id = hits.getAt(0).id()
          case x => System.err.println(" found " + hits.totalHits() + " hits, NOT VALID")
        }
        // create and add delete statement to bulk
        bulkRequest.add(client.prepareDelete(tableName, "type1", id))
      })

      deleteCount = bulkRequest.numberOfActions()
      val bulkResponse = bulkRequest.execute().actionGet()

      if (bulkResponse.hasFailures()) {
        System.err.println(bulkResponse.buildFailureMessage())
      } else {
        logger.info("Deleted " + deleteCount + " rows from " + tableName)
      }

      //      sql = "delete from " + tableName + " where timePartition >= ?  and timePartition <= ? and bucketKey = ?"
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to delete object(s) from the table " + tableName, e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  // Added by Yousef Abu Elbeh at 2016-03-13 from here
  override def del(containerName: String, time: TimeRange /*, keys: Array[Array[String]]*/): Unit = {
    var client: TransportClient = null
    var bulkRequest = client.prepareBulk()
    var deleteCount = 0
    var tableName = toFullTableName(containerName)
    try {
      logger.info("begin time => " + dateFormat.format(time.beginTime))
      logger.info("end time => " + dateFormat.format(time.endTime))
      CheckTableExists(containerName)

      client = getConnection

      val response = client
        .prepareSearch(tableName)
        .setTypes("type1")
        .setQuery(QueryBuilders.boolQuery()
          .must(QueryBuilders.rangeQuery("timePartition").gte(time.beginTime))
          .must(QueryBuilders.rangeQuery("timePartition").lte(time.endTime)))
        //        .setQuery(
        //          QueryBuilders.andQuery(
        //            QueryBuilders.rangeQuery("timePartition").gte(time.beginTime),
        //            QueryBuilders.rangeQuery("timePartition").lte(time.endTime)))
        .execute().actionGet()

      var hits: SearchHits = response.getHits()
      var id = ""

      hits.totalHits() match {
        case 0 => id = "noRecords"
        case 1 => id = hits.getAt(0).id()
        case x => System.err.println(" found " + hits.totalHits() + " hits, NOT VALID")
      }

      bulkRequest.add(client.prepareDelete(tableName, "type1", id))

      deleteCount = bulkRequest.numberOfActions()
      val bulkResponse = bulkRequest.execute().actionGet()

      if (bulkResponse.hasFailures()) {
        System.err.println(bulkResponse.buildFailureMessage())
      } else {
        logger.info("Deleted " + deleteCount + " rows from " + tableName)
      }
    }
    //      sql = "delete from " + tableName + " where timePartition >= ?  and timePartition <= ?"
    catch {
      case e: Exception => {
        throw CreateDMLException("Failed to delete object(s) from the table " + tableName, e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  // to here


  // get operations
  //  def getRowCount(containerName: String, whereClause: String): Int = {
  //    var con: Connection = null
  //    var stmt: Statement = null
  //    var rs: ResultSet = null
  //    var rowCount = 0
  //    var tableName = ""
  //    var query = ""
  //    try {
  //      con = getConnection
  //      CheckTableExists(containerName)
  //
  //      tableName = toFullTableName(containerName)
  //      query = "select count(*) from " + tableName
  //      if (whereClause != null) {
  //        query = query + whereClause
  //      }
  //      stmt = con.createStatement()
  //      rs = stmt.executeQuery(query);
  //      while (rs.next()) {
  //        rowCount = rs.getInt(1)
  //      }
  //      rowCount
  //    } catch {
  //      case e: Exception => {
  //        throw CreateDMLException("Failed to fetch data from the table " + tableName + ":" + "query => " + query, e)
  //      }
  //    } finally {
  //      if (rs != null) {
  //        rs.close
  //      }
  //      if (stmt != null) {
  //        stmt.close
  //      }
  //      if (con != null) {
  //        con.close
  //      }
  //    }
  //  }

  //  private def getData(tableName: String, query: String, callbackFunction: (Key, Value) => Unit): Unit = {
  //    var client: TransportClient = null
  //    var stmt: Statement = null
  //    var rs: ResultSet = null
  //    logger.info("Fetch the results of " + query)
  //    try {
  //      client = getConnection
  //
  //      con.createStatement()
  //      rs = stmt.executeQuery(query);
  //      var recCount = 0
  //      var byteCount = 0
  //      updateOpStats("get", tableName, 1)
  //      while (rs.next()) {
  //        var timePartition = rs.getLong(1)
  //        var keyStr = rs.getString(2)
  //        var tId = rs.getLong(3)
  //        var rId = rs.getInt(4)
  //        var schemaId = rs.getInt(5)
  //        var st = rs.getString(6)
  //        var ba = rs.getBytes(7)
  //        val bucketKey = if (keyStr != null) keyStr.split(",").toArray else new Array[String](0)
  //        var key = new Key(timePartition, bucketKey, tId, rId)
  //        // yet to understand how split serializerType and serializedInfo from ba
  //        // so hard coding serializerType to "kryo" for now
  //        var value = new Value(schemaId, st, ba)
  //        recCount = recCount + 1
  //        byteCount = byteCount + getKeySize(key) + getValueSize(value)
  //        if (callbackFunction != null)
  //          (callbackFunction) (key, value)
  //      }
  //      updateByteStats("get", tableName, byteCount)
  //      updateObjStats("get", tableName, recCount)
  //    } catch {
  //      case e: Exception => {
  //        throw CreateDMLException("Failed to fetch data from the table " + tableName + ":" + "query => " + query, e)
  //      }
  //    } finally {
  //      if (rs != null) {
  //        rs.close
  //      }
  //      if (stmt != null) {
  //        stmt.close
  //      }
  //      if (con != null) {
  //        con.close
  //      }
  //    }
  //  }

  override def get(containerName: String, callbackFunction: (Key, Value) => Unit): Unit = {
    var tableName = toFullTableName(containerName)
    var client = getConnection
    try {
      CheckTableExists(containerName)
      var recCount = 0
      var byteCount = 0

      var timeToLive = "1m"
      var response = client.prepareSearch(tableName)
        .setTypes("type1")
        .setFetchSource(Array("timePartition", "bucketKey", "transactionId", "rowId", "schemaId", "serializerType", "serializedInfo"),
          null)
        .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
        .setScroll(timeToLive)
        .setSize(5)
        .execute().actionGet()

      val results: SearchHits = response.getHits
      val hit: SearchHit = null
      breakable {
        while (true) {
          response.getHits.getHits.foreach((hit: SearchHit) => {
            var timePartition = hit.getSource.get("timePartition").toString.toLong
            var keyStr: String = hit.getSource.get("bucketKey").toString
            var tId = hit.getSource.get("transactionId").toString.toLong
            var rId = hit.getSource.get("rowId").toString.toInt
            var schemaId = hit.getSource.get("schemaId").toString.toInt
            var st = hit.getSource.get("serializerType").toString
            //        var ba: Array[Byte] = hit.getSource.get("serializedInfo").toString.getBytes()
            var ba = Base64.decodeBase64(hit.getSource.get("serializedInfo").toString.getBytes)

            val bucketKey = if (keyStr != null) keyStr.split(",").toArray else new Array[String](0)
            var key = new Key(timePartition, bucketKey, tId, rId)
            // yet to understand how split serializerType and serializedInfo from ba
            // so hard coding serializerType to "kryo" for now
            var value = new Value(schemaId, st, ba)
            recCount = recCount + 1
            byteCount = byteCount + getKeySize(key) + getValueSize(value)
            if (callbackFunction != null) {
              (callbackFunction) (key, value)
            }
          })


          response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeToLive).execute().actionGet()
          //Break condition: No hits are returned
          if (response.getHits().getHits().length == 0) {
            break
          }
        }
      }



      //      client.close()
      //    var query = "select timePartition,bucketKey,transactionId,rowId,schemaId,serializerType,serializedInfo from " + tableName
      //    getData(tableName, query, callbackFunction)
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to fetch data from the table " + tableName, e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  //  private def getKeys(tableName: String, query: String, callbackFunction: (Key) => Unit): Unit = {
  //    var con: Connection = null
  //    var stmt: Statement = null
  //    var rs: ResultSet = null
  //    try {
  //      con = getConnection
  //
  //      stmt = con.createStatement()
  //      rs = stmt.executeQuery(query);
  //      var recCount = 0
  //      var byteCount = 0
  //      updateOpStats("get", tableName, 1)
  //      while (rs.next()) {
  //        var timePartition = rs.getLong(1)
  //        var keyStr = rs.getString(2)
  //        var tId = rs.getLong(3)
  //        var rId = rs.getInt(4)
  //        val bucketKey = if (keyStr != null) keyStr.split(",").toArray else new Array[String](0)
  //        var key = new Key(timePartition, bucketKey, tId, rId)
  //        recCount = recCount + 1
  //        byteCount = byteCount + getKeySize(key)
  //        if (callbackFunction != null)
  //          (callbackFunction) (key)
  //      }
  //      updateByteStats("get", tableName, byteCount)
  //      updateObjStats("get", tableName, recCount)
  //    } catch {
  //      case e: Exception => {
  //        throw CreateDMLException("Failed to fetch data from the table " + tableName + ":" + "query => " + query, e)
  //      }
  //    } finally {
  //      if (rs != null) {
  //        rs.close
  //      }
  //      if (stmt != null) {
  //        stmt.close
  //      }
  //      if (con != null) {
  //        con.close
  //      }
  //    }
  //  }

  override def getKeys(containerName: String, callbackFunction: (Key) => Unit): Unit = {
    CheckTableExists(containerName)
    var tableName = toFullTableName(containerName)
    var client = getConnection
    var recCount = 0
    var byteCount = 0
    val timeToLive = "1m"

    // var query = "select timePartition,bucketKey,transactionId,rowId from " + tableName

    var response = client
      .prepareSearch(tableName)
      .setTypes("type1")
      .setFetchSource(Array("timePartition", "bucketKey", "transactionId", "rowId"), null)
      .execute().actionGet()

    updateOpStats("get", tableName, 1)

    val results: SearchHits = response.getHits
    val hit: SearchHit = null

    breakable {
      while (true) {
        results.getHits.foreach((hit: SearchHit) => {
          var timePartition = hit.getSource.get("timePartition").toString.toLong
          var keyStr = hit.getSource.get("bucketKey").toString
          var tId = hit.getSource.get("transactionId").toString.toLong
          var rId = hit.getSource.get("rowId").toString.toInt
          val bucketKey = if (keyStr != null) keyStr.split(",").toArray else new Array[String](0)
          var key = new Key(timePartition, bucketKey, tId, rId)
          recCount = recCount + 1
          byteCount = byteCount + getKeySize(key)
          if (callbackFunction != null)
            (callbackFunction) (key)
        })
        response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeToLive).execute().actionGet()
        //Break condition: No hits are returned
        if (response.getHits().getHits().length == 0) {
          break
        }
      }
    }
    updateByteStats("get", tableName, byteCount)
    updateObjStats("get", tableName, recCount)

    client.close()
    //  getKeys(tableName, query, callbackFunction)
  }

  override def getKeys(containerName: String, keys: Array[Key], callbackFunction: (Key) => Unit): Unit = {
    var client: TransportClient = null
    var tableName = toFullTableName(containerName)

    try {
      CheckTableExists(containerName)
      client = getConnection
      var recCount = 0
      var byteCount = 0
      val timeToLive = "1m"

      keys.foreach(key => {
        var response = client
          .prepareSearch(tableName)
          .setTypes("type1")
          .setQuery(QueryBuilders
            .boolQuery()
            .must(QueryBuilders.termQuery("timePartition", key.timePartition))
            .must(QueryBuilders.termQuery("bucketKey", key.bucketKey.mkString(",")))
            .must(QueryBuilders.termQuery("transactionid", key.transactionId))
            .must(QueryBuilders.termQuery("rowId", key.rowId))
          )
          .setFetchSource(Array("timePartition", "bucketKey", "transactionId", "rowId"), null)
          .execute().actionGet()

        updateOpStats("get", tableName, 1)

        val results: SearchHits = response.getHits
        val hit: SearchHit = null
        breakable {
          while (true) {
            results.getHits.foreach((hit: SearchHit) => {
              var timePartition = hit.getSource.get("timePartition").toString.toLong
              var keyStr = hit.getSource.get("bucketKey").toString
              var tId = hit.getSource.get("transactionId").toString.toLong
              var rId = hit.getSource.get("rowId").toString.toInt
              val bucketKey = if (keyStr != null) keyStr.split(",").toArray else new Array[String](0)
              var key = new Key(timePartition, bucketKey, tId, rId)
              recCount = recCount + 1
              byteCount = byteCount + getKeySize(key)
              if (callbackFunction != null)
                (callbackFunction) (key)
            })

            response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeToLive).execute().actionGet()
            //Break condition: No hits are returned
            if (response.getHits().getHits().length == 0) {
              break
            }
          }
        }
      })
      updateByteStats("get", tableName, byteCount)
      updateObjStats("get", tableName, recCount)

      //      query = "select timePartition,bucketKey,transactionId,rowId from " + tableName + " where timePartition = ? and bucketKey = ? and transactionid = ? and rowId = ?"

    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to fetch data from the table " + tableName, e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  override def get(containerName: String, keys: Array[Key], callbackFunction: (Key, Value) => Unit): Unit = {
    var client: TransportClient = null
    var tableName = toFullTableName(containerName)
    val timeToLive = "1m"
    try {
      CheckTableExists(containerName)
      client = getConnection
      var recCount = 0
      var byteCount = 0
      keys.foreach(key => {
        var response = client
          .prepareSearch(tableName)
          .setTypes("type1")
          .setQuery(QueryBuilders
            .boolQuery()
            .must(QueryBuilders.termQuery("timePartition", key.timePartition))
            .must(QueryBuilders.termQuery("bucketKey", key.bucketKey.mkString(",")))
            .must(QueryBuilders.termQuery("transactionid", key.transactionId))
            .must(QueryBuilders.termQuery("rowId", key.rowId))
          )
          .setFetchSource(Array("schemaId", "serializerType", "serializedInfo"), null)
          .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
          .setScroll(timeToLive)
          .setSize(5)
          .execute().actionGet()

        updateOpStats("get", tableName, 1)

        val results: SearchHits = response.getHits
        val hit: SearchHit = null

        breakable {
          while (true) {

            response.getHits.getHits.foreach((hit: SearchHit) => {
              val schemaId = hit.getSource.get("schemaId").toString.toInt
              val st = hit.getSource.get("serializerType").toString
              val ba: Array[Byte] = hit.getSource.get("serializedInfo").toString.getBytes()
              val value = new Value(schemaId, st, ba)
              recCount = recCount + 1
              byteCount = byteCount + getKeySize(key) + getValueSize(value)
              if (callbackFunction != null) {
                (callbackFunction) (key, value)
              }
            })

            response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeToLive).execute().actionGet()
            //Break condition: No hits are returned
            if (response.getHits().getHits().length == 0) {
              break
            }
          }
        }
      })

      updateByteStats("get", tableName, byteCount)
      updateObjStats("get", tableName, recCount)


      // query = "select schemaId,serializerType,serializedInfo from " + tableName + " where timePartition = ? and bucketKey = ? and transactionid = ? and rowId = ?"
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to fetch data from the table " + tableName + ": ", e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  override def get(containerName: String, time_ranges: Array[TimeRange], callbackFunction: (Key, Value) => Unit): Unit = {
    var client: TransportClient = null
    var tableName = toFullTableName(containerName)
    try {
      var recCount = 0
      var byteCount = 0
      CheckTableExists(containerName)
      client = getConnection
      var timeToLive = "1m"
      time_ranges.foreach(time_range => {
        var response = client
          .prepareSearch(tableName)
          .setTypes("type1")
          .setQuery(QueryBuilders.boolQuery()
            .must(QueryBuilders.rangeQuery("timePartition").gte(time_range.beginTime))
            .must(QueryBuilders.rangeQuery("timePartition").lte(time_range.endTime)))
          .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
          .setScroll(timeToLive)
          .setSize(5)
          .execute().actionGet()

        val results: SearchHits = response.getHits
        val hit: SearchHit = null
        breakable {
          while (true) {
            response.getHits.getHits.foreach((hit: SearchHit) => {
              var timePartition = hit.getSource.get("timePartition").toString.toLong
              var keyStr: String = hit.getSource.get("bucketKey").toString
              var tId = hit.getSource.get("transactionId").toString.toLong
              var rId = hit.getSource.get("rowId").toString.toInt
              var schemaId = hit.getSource.get("schemaId").toString.toInt
              var st = hit.getSource.get("serializerType").toString
              //          var ba: Array[Byte] = hit.getSource.get("serializedInfo").toString.getBytes()
              var ba = Base64.decodeBase64(hit.getSource.get("serializedInfo").toString.getBytes)

              val bucketKey = if (keyStr != null) keyStr.split(",").toArray else new Array[String](0)
              var key = new Key(timePartition, bucketKey, tId, rId)
              // yet to understand how split serializerType and serializedInfo from ba
              // so hard coding serializerType to "kryo" for now
              var value = new Value(schemaId, st, ba)
              recCount = recCount + 1
              byteCount = byteCount + getKeySize(key) + getValueSize(value)
              if (callbackFunction != null) {
                (callbackFunction) (key, value)
              }
            })
            response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeToLive).execute().actionGet()
            //Break condition: No hits are returned
            if (response.getHits().getHits().length == 0) {
              break
            }
          }
        }

        //      var query = "select timePartition,bucketKey,transactionId,rowId,schemaId,serializerType,serializedInfo from " + tableName + " where timePartition >= " + time_range.beginTime + " and timePartition <= " + time_range.endTime
        //      logger.debug("query => " + query)
        //      getData(tableName, query, callbackFunction)
      })
      //    client.close()
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to fetch data from the table " + tableName, e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }

  }

  override def getKeys(containerName: String, time_ranges: Array[TimeRange], callbackFunction: (Key) => Unit): Unit = {
    CheckTableExists(containerName)
    var tableName = toFullTableName(containerName)
    var client = getConnection
    var recCount = 0
    var byteCount = 0
    val timeToLive = "1m"

    time_ranges.foreach(time_range => {
      var response = client
        .prepareSearch(tableName)
        .setTypes("type1")
        .setQuery(QueryBuilders.boolQuery()
          .must(QueryBuilders.rangeQuery("timePartition").gte(time_range.beginTime))
          .must(QueryBuilders.rangeQuery("timePartition").lte(time_range.endTime)))
        //        .setQuery(
        //          QueryBuilders.andQuery(
        //            QueryBuilders.rangeQuery("timePartition").gte(time_range.beginTime),
        //            QueryBuilders.rangeQuery("timePartition").lte(time_range.endTime)))
        .setFetchSource(Array("timePartition", "bucketKey", "transactionId", "rowId"), null)
        .execute().actionGet()

      updateOpStats("get", tableName, 1)

      val results: SearchHits = response.getHits
      val hit: SearchHit = null


      breakable {
        while (true) {
          results.getHits.foreach((hit: SearchHit) => {
            var timePartition = hit.getSource.get("timePartition").toString.toLong
            var keyStr = hit.getSource.get("bucketKey").toString
            var tId = hit.getSource.get("transactionId").toString.toLong
            var rId = hit.getSource.get("rowId").toString.toInt
            val bucketKey = if (keyStr != null) keyStr.split(",").toArray else new Array[String](0)
            var key = new Key(timePartition, bucketKey, tId, rId)
            recCount = recCount + 1
            byteCount = byteCount + getKeySize(key)
            if (callbackFunction != null)
              (callbackFunction) (key)
          })
          response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeToLive).execute().actionGet()
          //Break condition: No hits are returned
          if (response.getHits().getHits().length == 0) {
            break
          }
        }
      }
      updateByteStats("get", tableName, byteCount)
      updateObjStats("get", tableName, recCount)
      //      var query = "select timePartition,bucketKey,transactionId,rowId from " + tableName + " where timePartition >= " + time_range.beginTime + " and timePartition <= " + time_range.endTime
      //      getKeys(tableName, query, callbackFunction)
    })
    client.close
  }

  override def get(containerName: String, time_ranges: Array[TimeRange], bucketKeys: Array[Array[String]], callbackFunction: (Key, Value) => Unit): Unit = {
    var client: TransportClient = null
    var tableName = toFullTableName(containerName)
    try {
      CheckTableExists(containerName)
      //con = DriverManager.getConnection(jdbcUrl);
      client = getConnection
      var recCount = 0
      var byteCount = 0
      var timeToLive = "1m"
      time_ranges.foreach(time_range => {
        bucketKeys.foreach(bucketKey => {
          var response = client
            .prepareSearch(tableName)
            .setTypes("type1")
            .setFetchSource(Array("timePartition", "bucketKey", "transactionId", "rowId", "schemaId", "serializerType", "serializedInfo"), null)
            //            .setQuery(
            //              QueryBuilders.andQuery(
            //                QueryBuilders.rangeQuery("timePartition").gte(time_range.beginTime),
            //                QueryBuilders.rangeQuery("timePartition").lte(time_range.endTime)))
            .setQuery(QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery("bucketKey", bucketKey.mkString(",")))
            .must(QueryBuilders.rangeQuery("timePartition").gte(time_range.beginTime))
            .must(QueryBuilders.rangeQuery("timePartition").lte(time_range.endTime)))
            .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
            .setScroll(timeToLive)
            .setSize(5)
            .execute().actionGet()

          val results: SearchHits = response.getHits
          val hit: SearchHit = null
          breakable {
            while (true) {
              response.getHits.getHits.foreach((hit: SearchHit) => {
                var timePartition = hit.getSource.get("timePartition").toString.toLong
                var keyStr = hit.getSource.get("bucketKey").toString
                var tId = hit.getSource.get("transactionId").toString.toLong
                var rId = hit.getSource.get("rowId").toString.toInt
                val schemaId = hit.getSource.get("schemaId").toString.toInt
                var st = hit.getSource.get("serializerType").toString
                //            var ba: Array[Byte] = hit.getSource.get("serializedInfo").toString.getBytes()
                var ba = Base64.decodeBase64(hit.getSource.get("serializedInfo").toString.getBytes)

                val bucketKey = if (keyStr != null) keyStr.split(",").toArray else new Array[String](0)
                var key = new Key(timePartition, bucketKey, tId, rId)
                var value = new Value(schemaId, st, ba)
                recCount = recCount + 1
                byteCount = byteCount + getKeySize(key) + getValueSize(value)
                if (callbackFunction != null) {
                  (callbackFunction) (key, value)
                }
              })

              response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeToLive).execute().actionGet()
              //Break condition: No hits are returned
              if (response.getHits().getHits().length == 0) {
                break
              }
            }
          }

        })
        // query = "select timePartition,bucketKey,transactionId,rowId,schemaId,serializerType,serializedInfo from " + tableName + " where timePartition >= " + time_range.beginTime + " and timePartition <= " + time_range.endTime + " and bucketKey = ? "
      })
      updateByteStats("get", tableName, byteCount)
      updateObjStats("get", tableName, recCount)
    }

    catch {
      case e: Exception => {
        throw CreateDMLException("Failed to fetch data from the table " + tableName + ": ", e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  override def getKeys(containerName: String, time_ranges: Array[TimeRange], bucketKeys: Array[Array[String]], callbackFunction: (Key) => Unit): Unit = {
    var client: TransportClient = null
    var tableName = toFullTableName(containerName)
    try {
      CheckTableExists(containerName)
      client = getConnection
      var recCount = 0
      var byteCount = 0
      val timeToLive = "1m"


      time_ranges.foreach(time_range => {
        bucketKeys.foreach(bucketKey => {
          var response = client
            .prepareSearch(tableName)
            .setTypes("type1")
            .setFetchSource(Array("timePartition", "bucketKey", "transactionId", "rowId"), null)
            .setQuery(QueryBuilders.boolQuery()
              .must(QueryBuilders.termQuery("bucketKey", bucketKey.mkString(",")))
              .must(QueryBuilders.rangeQuery("timePartition").gte(time_range.beginTime))
              .must(QueryBuilders.rangeQuery("timePartition").lte(time_range.endTime))
            )
            //            .setQuery(
            //              QueryBuilders.andQuery(
            //                QueryBuilders.rangeQuery("timePartition").gte(time_range.beginTime),
            //                QueryBuilders.rangeQuery("timePartition").lte(time_range.endTime)))
            .execute().actionGet()
          updateOpStats("get", tableName, 1)

          val results: SearchHits = response.getHits
          val hit: SearchHit = null

          breakable {
            while (true) {
              results.getHits.foreach((hit: SearchHit) => {
                val timePartition = hit.getSource.get("timePartition").toString.toLong
                val keyStr = hit.getSource.get("bucketKey").toString
                val tId = hit.getSource.get("transactionId").toString.toLong
                val rId = hit.getSource.get("transactionId").toString.toInt
                val bucketKey = if (keyStr != null) keyStr.split(",").toArray else new Array[String](0)
                var key = new Key(timePartition, bucketKey, tId, rId)
                recCount = recCount + 1
                byteCount = byteCount + getKeySize(key)
                if (callbackFunction != null)
                  (callbackFunction) (key)
              })
              response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeToLive).execute().actionGet()
              //Break condition: No hits are returned
              if (response.getHits().getHits().length == 0) {
                break
              }
            }
          }
        })
      })
      updateByteStats("get", tableName, byteCount)
      updateObjStats("get", tableName, recCount)
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to fetch data from the table " + tableName, e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  override def get(containerName: String, bucketKeys: Array[Array[String]], callbackFunction: (Key, Value) => Unit): Unit = {
    var client: TransportClient = null
    var tableName = toFullTableName(containerName)
    try {
      CheckTableExists(containerName)
      client = getConnection
      var recCount = 0
      var byteCount = 0
      var timeToLive = "1m"

      bucketKeys.foreach(bucketKey => {
        var response = client
          .prepareSearch(tableName)
          .setTypes("type1")
          .setQuery(QueryBuilders
            .boolQuery()
            .must(QueryBuilders.termQuery("bucketKey", bucketKey.mkString(","))))
          .setFetchSource(Array("timePartition", "bucketKey", "transactionId", "rowId", "schemaId", "serializerType", "serializedInfo"), null)
          .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
          .setScroll(timeToLive)
          .setSize(5)
          .execute().actionGet()

        updateOpStats("get", tableName, 1)

        val results: SearchHits = response.getHits
        val hit: SearchHit = null
        breakable {
          while (true) {
            response.getHits.getHits.foreach((hit: SearchHit) => {
              var timePartition = hit.getSource.get("timePartition").toString.toLong
              var keyStr = hit.getSource.get("bucketKey").toString
              var tId = hit.getSource.get("transactionId").toString.toLong
              var rId = hit.getSource.get("rowId").toString.toInt
              val schemaId = hit.getSource.get("schemaId").toString.toInt
              var st = hit.getSource.get("serializerType").toString
              //          var ba: Array[Byte] = hit.getSource.get("serializedInfo").toString.getBytes()
              var ba = Base64.decodeBase64(hit.getSource.get("serializedInfo").toString.getBytes)

              val bucketKey = if (keyStr != null) keyStr.split(",").toArray else new Array[String](0)
              var key = new Key(timePartition, bucketKey, tId, rId)
              var value = new Value(schemaId, st, ba)
              recCount = recCount + 1
              byteCount = byteCount + getKeySize(key)
              if (callbackFunction != null) {
                (callbackFunction) (key, value)
              }
            })

            response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeToLive).execute().actionGet()
            //Break condition: No hits are returned
            if (response.getHits().getHits().length == 0) {
              break
            }
          }
        }
      })
      updateByteStats("get", tableName, byteCount)
      updateObjStats("get", tableName, recCount)

      // query = "select timePartition,bucketKey,transactionId,rowId,schemaId,serializerType,serializedInfo from " + tableName + " where  bucketKey = ? "

    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to fetch data from the table " + tableName, e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  override def getKeys(containerName: String, bucketKeys: Array[Array[String]], callbackFunction: (Key) => Unit): Unit = {
    var client: TransportClient = null
    var tableName = toFullTableName(containerName)
    try {
      CheckTableExists(containerName)
      client = getConnection
      var recCount = 0
      var byteCount = 0
      val timeToLive = "1m"

      bucketKeys.foreach(bucketKey => {
        var response = client
          .prepareSearch(tableName)
          .setTypes("type1")
          .setQuery(QueryBuilders
            .boolQuery().must(QueryBuilders.termQuery("timePartition", bucketKey.mkString(","))))
          .setFetchSource(Array("timePartition", "bucketKey", "transactionId", "rowId"), null)
          .execute().actionGet()

        updateOpStats("get", tableName, 1)

        val results: SearchHits = response.getHits
        val hit: SearchHit = null

        breakable {
          while (true) {
            results.getHits.foreach((hit: SearchHit) => {
              val schemaId = hit.getSource.get("schemaId").toString.toInt
              val st = hit.getSource.get("serializerType").toString
              val ba: Array[Byte] = hit.getSource.get("serializedInfo").toString.getBytes()
              val value = new Value(schemaId, st, ba)
              var timePartition = hit.getSource.get("timePartition").toString.toLong
              var keyStr = hit.getSource.get("bucketKey").toString
              var tId = hit.getSource.get("transactionId").toString.toLong
              var rId = hit.getSource.get("rowId").toString.toInt
              val bucketKey = if (keyStr != null) keyStr.split(",").toArray else new Array[String](0)
              var key = new Key(timePartition, bucketKey, tId, rId)
              recCount = recCount + 1
              byteCount = byteCount + getKeySize(key)
              if (callbackFunction != null)
                (callbackFunction) (key)

            })
            response = client.prepareSearchScroll(response.getScrollId()).setScroll(timeToLive).execute().actionGet()
            //Break condition: No hits are returned
            if (response.getHits().getHits().length == 0) {
              break
            }
          }
        }
      })
      updateByteStats("get", tableName, byteCount)
      updateObjStats("get", tableName, recCount)
      //query = "select timePartition,bucketKey,transactionId,rowId from " + tableName + " where  bucketKey = ? "
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to fetch data from the table " + tableName, e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  override def beginTx(): Transaction = {
    new ElasticsearchAdapterTx(this)
  }

  override def endTx(tx: Transaction): Unit = {
  }

  override def commitTx(tx: Transaction): Unit = {
  }

  override def rollbackTx(tx: Transaction): Unit = {
  }

  override def Shutdown(): Unit = {
    logger.info("close the connection pool")
  }

  private def TruncateContainer(containerName: String): Unit = {
    var client: TransportClient = null
    var tableName = toFullTableName(containerName)
    try {
      CheckTableExists(containerName)
      client = getConnection
      logger.info("delete the index/container ")
      var response = client.admin().indices()
        .delete(new DeleteIndexRequest(tableName)).actionGet()
      logger.info("create the index/container again ")
      CreateContainer(containerName.toLowerCase(), "ddl")
    } catch {
      case e: Exception => {
        throw CreateDDLException("Failed to fetch data from the table " + tableName, e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  override def TruncateContainer(containerNames: Array[String]): Unit = {
    logger.info("truncate the container tables")
    containerNames.foreach(cont => {
      logger.info("truncate the container " + cont)
      TruncateContainer(cont)
    })
  }

  private def dropTable(tableName: String): Unit = lock.synchronized {
    var client: TransportClient = null
    //    var fullTableName = SchemaName + "." + tableName
    try {
      client = getConnection
      val indicies = client.admin().cluster()
        .prepareState().execute()
        .actionGet().getState()
        .getMetaData().concreteAllIndices()

      if (indicies.contains(tableName) == true) {
        var response = client.admin().indices()
          .delete(new DeleteIndexRequest(tableName)).actionGet()
      } else {
        logger.info("The Index " + tableName + " doesn't exist, it may have beem dropped already ")
      }

    } catch {
      case e: Exception => {
        throw CreateDDLException("Failed to drop the Index " + tableName, e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  private def DropContainer(containerName: String): Unit = lock.synchronized {
    var tableName = toFullTableName(containerName)
    dropTable(tableName)
  }

  override def DropContainer(containerNames: Array[String]): Unit = {
    logger.info("drop the container tables")
    containerNames.foreach(cont => {
      logger.info("drop the container " + cont)
      DropContainer(cont)
    })
  }

  private def CreateContainer(containerName: String, apiType: String): Unit = lock.synchronized {
    var client: TransportClient = null
    var tableName = toFullTableName(containerName)
    try {
      client = getConnection

      val indicies = client.admin().cluster()
        .prepareState().execute()
        .actionGet().getState()
        .getMetaData().concreteAllIndices()


      if (indicies.contains(tableName) == true) {
        logger.debug("The Index " + tableName + " already exists ")
      } else {

        if (autoCreateTables.equalsIgnoreCase("NO")) {
          apiType match {
            case "dml" => {
              throw new Exception("The option autoCreateTables is set to NO, So Can't create non-existent table automatically to support the requested DML operation")
            }
            case _ => {
              logger.info("proceed with creating table..")
            }
          }
        }

        var putMappingResponse = client.admin().indices().prepareCreate(tableName)
          //          .preparePutMapping(tableName)
          //          .setType("type1")
          .setSource("{\"mappings\": {\"type1\": {\"properties\": {\"serializedInfo\": {\"type\": \"binary\",\"store\": \"true\"},\"bucketKey\": {\"type\": \"string\",\"index\": \"not_analyzed\"}}}}}")
          .execute().actionGet()


        //        val mapping = XContentFactory.jsonBuilder()
        //          .field("serializedInfo")
        //          .startObject()
        //          .field("store", "true")
        //          .field("type", "binary")
        //          .endObject()
        //
        //
        //        var putMappingResponse = client.admin().indices()
        //          .preparePutMapping(tableName)
        ////          .setIndices(tableName)
        //          .setType("type1")
        //          .setSource(mapping)
        //          .execute().actionGet()

      }


    }
    //query = "create table " + fullTableName + "(timePartition bigint,bucketKey varchar(1024), transactionId bigint, rowId Int, schemaId Int, serializerType varchar(128), serializedInfo varbinary(max))"
    catch {
      case e: Exception => {
        throw CreateDDLException("Failed to create Index " + tableName, e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  override def CreateContainer(containerNames: Array[String]): Unit = {
    logger.info("create the container tables")
    containerNames.foreach(cont => {
      logger.info("create the container " + cont)
      CreateContainer(cont.toLowerCase(), "ddl")
    })
  }

  override def CreateMetadataContainer(containerNames: Array[String]): Unit = {
    CreateContainer(containerNames)
  }

  override def isTableExists(tableName: String): Boolean = {
    // check whether corresponding table exists
    var client: TransportClient = null
    var rs: ResultSet = null
    logger.info("Checking the existence of the Index " + tableName)
    try {
      client = getConnection

      val indicies = client.admin().cluster()
        .prepareState().execute()
        .actionGet().getState()
        .getMetaData().concreteAllIndices()

      if (indicies.contains(tableName) == true) {
        return true
      } else {
        return false
      }
    } catch {
      case e: Exception => {
        throw CreateDMLException("Unable to verify table existence of table " + tableName, e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  override def isTableExists(tableNamespace: String, tableName: String): Boolean = {
    isTableExists(tableNamespace + "." + tableName)
  }

  // not implemented yet
  //  def renameTable(srcTableName: String, destTableName: String, forceCopy: Boolean = false): Unit = lock.synchronized {
  //    var client: TransportClient = null
  //    var stmt: Statement = null
  //    var rs: ResultSet = null
  //    logger.info("renaming " + srcTableName + " to " + destTableName);
  //    var query = ""
  //    try {
  //      // check whether source table exists
  //      var exists = isTableExists(srcTableName)
  //      if (!exists) {
  //        throw CreateDDLException("Failed to rename the table " + srcTableName + ":", new Exception("Source Table doesn't exist"))
  //      }
  //      // check if the destination table already exists
  //      exists = isTableExists(destTableName)
  //      if (exists) {
  //        logger.info("The table " + destTableName + " exists.. ")
  //        if (forceCopy) {
  //          dropTable(destTableName)
  //        } else {
  //          throw CreateDDLException("Failed to rename the table " + srcTableName + ":", new Exception("Destination Table already exist"))
  //        }
  //      }
  //      client = getConnection
  //
  //      var indicies: util.Map[String, IndexStats] = client.admin().indices().prepareStats().clear().get().getIndices()
  //
  //      if (indicies.get(srcTableName) != null) {
  //        val response = client.admin().indices().prepareAliases()
  //          .addAlias(toFullTableName(srcTableName), toFullTableName(destTableName))
  //          .execute().actionGet()
  //      } else {
  //
  //        val response = client.admin().indices().prepareAliases()
  //          .removeAlias("my_old_index", "my_alias")
  //          .addAlias("my_index", "my_alias")
  //          .execute().actionGet();
  //
  //      }
  //
  //
  //      query = "sp_rename '" + SchemaName + "." + srcTableName + "' , '" + destTableName + "'"
  //      stmt = con.createStatement()
  //      stmt.executeUpdate(query);
  //    }
  //  catch
  //  {
  //    case e: Exception => {
  //      throw CreateDDLException("Failed to rename the table " + srcTableName + ":" + "query => " + query, e)
  //    }
  //  } finally
  //  {
  //    if (rs != null) {
  //      rs.close
  //    }
  //    if (stmt != null) {
  //      stmt.close
  //    }
  //    if (con != null) {
  //      con.close
  //    }
  //  }
  //}


  //  def getIndicesFromAliasName(aliasName: String, client: TransportClient): Set[String] = {
  //
  //    val iac: IndicesAdminClient = client.admin().indices();
  //
  //
  //    val map: ImmutableOpenMap[String, util.List[AliasMetaData]] = iac.getAliases(new GetAliasesRequest(aliasName))
  //      .actionGet().getAliases()
  //
  //    val allIndices: Set[String] = null
  //    val key: String = null
  //
  //    map.keysIt().forEachRemaining((key1:String) => {
  //
  //
  //
  //
  //    })
  //
  ////    map.keysIt().forEachRemaining(allIndices::add);
  ////    return allIndices;
  //  }

  // not implemented yet
  def backupContainer(containerName: String): Unit = lock.synchronized {
    var tableName = toFullTableName(containerName)
    var oldTableName = tableName
    var newTableName = tableName + "_bak"
    //    renameTable(oldTableName, newTableName)
  }

  // not implemented yet
  def restoreContainer(containerName: String): Unit = lock.synchronized {
    var tableName = toFullTableName(containerName)
    var oldTableName = tableName + "_bak"
    var newTableName = tableName
    //    renameTable(oldTableName, newTableName)
  }

  override def isContainerExists(containerName: String): Boolean = {
    // check whether corresponding table exists
    var tableName = toFullTableName(containerName)
    isTableExists(tableName)
  }

  // code to be implemented
  override def copyContainer(srcContainerName: String, destContainerName: String, forceCopy: Boolean): Unit = lock.synchronized {
    if (srcContainerName.equalsIgnoreCase(destContainerName)) {
      throw CreateDDLException("Failed to copy the container " + srcContainerName, new Exception("Source Container Name can't be same as destination container name"))
    }
    var srcTableName = toFullTableName(srcContainerName)
    var destTableName = toFullTableName(destContainerName)
    try {
      //      renameTable(srcTableName, destTableName, forceCopy)
      logger.info("code  not implemented yet")
    } catch {
      case e: Exception => {
        throw CreateDDLException("Failed to copy the container " + srcContainerName, e)
      }
    }
  }

  override def getAllTables: Array[String] = {
    var tbls = new Array[String](0)
    // check whether corresponding table exists
    var client: TransportClient = null
    var rs: ResultSet = null
    try {
      client = getConnection

      val indicies: Array[String] = client.admin().cluster()
        .prepareState().execute()
        .actionGet().getState()
        .getMetaData().concreteAllIndices()

      indicies
    } catch {
      case e: Exception => {
        throw CreateDMLException("Unable to fetch the list of tables Elasticsearch ", e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  override def dropTables(tbls: Array[String]): Unit = {
    try {
      tbls.foreach(t => {
        dropTable(t)
      })
    } catch {
      case e: Exception => {
        throw CreateDDLException("Failed to drop table list  ", e)
      }
    }
  }

  override def dropTables(tbls: Array[(String, String)]): Unit = {
    dropTables(tbls.map(t => t._1 + ':' + t._2))
  }

  // not implemented yet
  override def copyTable(srcTableName: String, destTableName: String, forceCopy: Boolean): Unit = {
    //    renameTable(srcTableName, destTableName, forceCopy)
  }

  // not implemented yet
  override def copyTable(namespace: String, srcTableName: String, destTableName: String, forceCopy: Boolean): Unit = {
    //    copyTable(namespace + '.' + srcTableName, namespace + '.' + destTableName, forceCopy)
  }
}

class ElasticsearchAdapterTx(val parent: DataStore) extends Transaction {

  //  val loggerName = this.getClass.getName
  //  val logger = LogManager.getLogger(loggerName)
  def putJson(data_list: Array[(String, Array[(String)])]): Unit = {
    putJson(data_list)
  }

  override def put(containerName: String, key: Key, value: Value): Unit = {
    parent.put(containerName, key, value)
  }

  override def put(data_list: Array[(String, Array[(Key, Value)])]): Unit = {
    parent.put(data_list)
  }

  // delete operations
  override def del(containerName: String, keys: Array[Key]): Unit = {
    parent.del(containerName, keys)
  }

  override def del(containerName: String, time: TimeRange, keys: Array[Array[String]]): Unit = {
    parent.del(containerName, time, keys)
  }

  //Added by Yousef Abu Elbeh at 2016-3-13 from here
  override def del(containerName: String, time: TimeRange): Unit = {
    parent.del(containerName, time)
  }

  // to here
  // get operations
  override def get(containerName: String, callbackFunction: (Key, Value) => Unit): Unit = {
    parent.get(containerName, callbackFunction)
  }

  override def get(containerName: String, keys: Array[Key], callbackFunction: (Key, Value) => Unit): Unit = {
    parent.get(containerName, keys, callbackFunction)
  }

  override def get(containerName: String, time_ranges: Array[TimeRange], callbackFunction: (Key, Value) => Unit): Unit = {
    parent.get(containerName, time_ranges, callbackFunction)
  }

  override def get(containerName: String, time_ranges: Array[TimeRange], bucketKeys: Array[Array[String]], callbackFunction: (Key, Value) => Unit): Unit = {
    parent.get(containerName, time_ranges, bucketKeys, callbackFunction)
  }

  override def get(containerName: String, bucketKeys: Array[Array[String]], callbackFunction: (Key, Value) => Unit): Unit = {
    parent.get(containerName, bucketKeys, callbackFunction)
  }

  def getKeys(containerName: String, callbackFunction: (Key) => Unit): Unit = {
    parent.getKeys(containerName, callbackFunction)
  }

  def getKeys(containerName: String, keys: Array[Key], callbackFunction: (Key) => Unit): Unit = {
    parent.getKeys(containerName, keys, callbackFunction)
  }

  def getKeys(containerName: String, timeRanges: Array[TimeRange], callbackFunction: (Key) => Unit): Unit = {
    parent.getKeys(containerName, timeRanges, callbackFunction)
  }

  def getKeys(containerName: String, timeRanges: Array[TimeRange], bucketKeys: Array[Array[String]], callbackFunction: (Key) => Unit): Unit = {
    parent.getKeys(containerName, timeRanges, bucketKeys, callbackFunction)
  }

  def getKeys(containerName: String, bucketKeys: Array[Array[String]], callbackFunction: (Key) => Unit): Unit = {
    parent.getKeys(containerName, bucketKeys, callbackFunction)
  }

  def backupContainer(containerName: String): Unit = {
    parent.backupContainer(containerName: String)
  }

  def restoreContainer(containerName: String): Unit = {
    parent.restoreContainer(containerName: String)
  }

  override def isContainerExists(containerName: String): Boolean = {
    parent.isContainerExists(containerName)
  }

  override def copyContainer(srcContainerName: String, destContainerName: String, forceCopy: Boolean): Unit = {
    parent.copyContainer(srcContainerName, destContainerName, forceCopy)
  }

  override def getAllTables: Array[String] = {
    parent.getAllTables
  }

  override def dropTables(tbls: Array[String]): Unit = {
    parent.dropTables(tbls)
  }

  override def copyTable(srcTableName: String, destTableName: String, forceCopy: Boolean): Unit = {
    parent.copyTable(srcTableName, destTableName, forceCopy)
  }

  override def isTableExists(tableName: String): Boolean = {
    parent.isTableExists(tableName)
  }

  override def isTableExists(tableNamespace: String, tableName: String): Boolean = {
    isTableExists(tableNamespace, tableName)
  }

  // accessor used for testing
  override def getTableName(containerName: String): String = {
    parent.getTableName(containerName)
  }

  override def dropTables(tbls: Array[(String, String)]): Unit = {
    dropTables(tbls)
  }

  override def copyTable(namespace: String, srcTableName: String, destTableName: String, forceCopy: Boolean): Unit = {
    copyTable(namespace, srcTableName, destTableName, forceCopy)
  }

}

// To create Elasticsearch Datastore instance
object ElasticsearchAdapter extends StorageAdapterFactory {
  override def CreateStorageAdapter(kvManagerLoader: KamanjaLoaderInfo, datastoreConfig: String, nodeCtxt: NodeContext, adapterInfo: AdapterInfo): DataStore = new ElasticsearchAdapter(kvManagerLoader, datastoreConfig, nodeCtxt, adapterInfo)
}
