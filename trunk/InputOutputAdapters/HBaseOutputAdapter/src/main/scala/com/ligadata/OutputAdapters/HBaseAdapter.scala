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

package com.ligadata.OutputAdapters;

// Hbase core
import com.ligadata.KamanjaBase.NodeContext
import com.ligadata.kamanja.metadata.AdapterInfo
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.NamespaceDescriptor;
// hbase client
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
// hbase filters
import org.apache.hadoop.hbase.filter.{ Filter, SingleColumnValueFilter, FirstKeyOnlyFilter, FilterList, CompareFilter, RowFilter }
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp
import org.apache.hadoop.hbase.filter.BinaryComparator
// hadoop security model
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.hbase.client.{ BufferedMutator, BufferedMutatorParams, Connection, ConnectionFactory }
import org.apache.hadoop.hbase._
import org.apache.logging.log4j.{Logger, LogManager}

//import org.apache.logging.log4j._
//import java.nio.ByteBuffer
//import java.io.IOException
import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.ligadata.Exceptions._
import com.ligadata.Utils.{ KamanjaLoaderInfo }
import com.ligadata.KvBase.{ Key, TimeRange, Value }
import com.ligadata.StorageBase.{ DataStore, Transaction, StorageAdapterFactory }

import scala.collection.mutable.ArrayBuffer

import scala.collection.JavaConversions._

class HBaseAdapter(val kvManagerLoader: KamanjaLoaderInfo, val datastoreConfig: String, val nodeCtxt: NodeContext, adapterInfo: AdapterInfo) {
  private[this] val logger = LogManager.getLogger(getClass.getName);
  val adapterConfig = if (datastoreConfig != null) datastoreConfig.trim else ""
  private[this] val lock = new Object
  private var containerList: scala.collection.mutable.Set[String] = scala.collection.mutable.Set[String]()
  private var msg: String = ""

  private val baseStrBytes = "base".getBytes()
  private val baseStrBytesLen = baseStrBytes.length
  private var keySize = 0

  var _getOps:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _getObjs:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _getBytes:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putObjs:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putOps:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putBytes:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()

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
    msg = "Invalid HBase Json Configuration string:" + adapterConfig
    throw CreateConnectionException(msg, new Exception("Invalid Configuration"))
  }

  logger.debug("HBase configuration:" + adapterConfig)
  var parsed_json: Map[String, Any] = null
  try {
    val json = parse(adapterConfig)
    if (json == null || json.values == null) {
      var msg = "Failed to parse HBase JSON configuration string:" + adapterConfig
      throw CreateConnectionException(msg, new Exception("Invalid Configuration"))
    }
    parsed_json = json.values.asInstanceOf[Map[String, Any]]
  } catch {
    case e: Exception => {
      var msg = "Failed to parse HBase JSON configuration string:%s.".format(adapterConfig)
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
          msg = "Failed to parse HBase JSON configuration string:" + adapterSpecificStr
          throw CreateConnectionException(msg, new Exception("Invalid Configuration"))
        }
        adapterSpecificConfig_json = json.values.asInstanceOf[Map[String, Any]]
      } catch {
        case e: Exception => {
          msg = "Failed to parse HBase Adapter Specific JSON configuration string:%s.".format(adapterSpecificStr)
          throw CreateConnectionException(msg, e)
        }
      }
    }
  }

  private def getOptionalField(key: String, main_json: Map[String, Any], adapterSpecific_json: Map[String, Any], default: Any): Any = {
    if (main_json != null) {
      val mainVal = main_json.getOrElse(key, null)
      if (mainVal != null)
        return mainVal
    }
    if (adapterSpecific_json != null) {
      val mainVal1 = adapterSpecific_json.getOrElse(key, null)
      if (mainVal1 != null)
        return mainVal1
    }
    return default
  }

  def CreateNameSpace(nameSpace: String): Unit = {
    relogin
    try {
      val nsd = admin.getNamespaceDescriptor(nameSpace)
      return
    } catch {
      case e: Exception => {
        logger.info("Namespace " + nameSpace + " doesn't exist, create it", e)
      }
    }
    try {
      admin.createNamespace(NamespaceDescriptor.create(nameSpace).build)
    } catch {
      case e: Exception => {
        throw CreateConnectionException("Unable to create hbase name space " + nameSpace, e)
      }
    }
  }

  val hostnames = if (parsed_json.contains("hostlist")) parsed_json.getOrElse("hostlist", "localhost").toString.trim else parsed_json.getOrElse("Location", "localhost").toString.trim
  val namespace = if (parsed_json.contains("SchemaName")) parsed_json.getOrElse("SchemaName", "default").toString.trim else parsed_json.getOrElse("SchemaName", "default").toString.trim

  val config = HBaseConfiguration.create();

  config.setInt("zookeeper.session.timeout", getOptionalField("zookeeper_session_timeout", parsed_json, adapterSpecificConfig_json, "5000").toString.trim.toInt);
  config.setInt("zookeeper.recovery.retry", getOptionalField("zookeeper_recovery_retry", parsed_json, adapterSpecificConfig_json, "1").toString.trim.toInt);
  config.setInt("hbase.client.retries.number", getOptionalField("hbase_client_retries_number", parsed_json, adapterSpecificConfig_json, "3").toString.trim.toInt);
  config.setInt("hbase.client.pause", getOptionalField("hbase_client_pause", parsed_json, adapterSpecificConfig_json, "5000").toString.trim.toInt);
  config.set("hbase.zookeeper.quorum", hostnames);

  val keyMaxSz = getOptionalField("hbase_client_keyvalue_maxsize", parsed_json, adapterSpecificConfig_json, "104857600").toString.trim.toInt
  var clntWrtBufSz = getOptionalField("hbase_client_write_buffer", parsed_json, adapterSpecificConfig_json, "104857600").toString.trim.toInt

  if (clntWrtBufSz < keyMaxSz)
    clntWrtBufSz = keyMaxSz + 1024 // 1K Extra

  config.setInt("hbase.client.keyvalue.maxsize", keyMaxSz);
  config.setInt("hbase.client.write.buffer", clntWrtBufSz);

  var isKerberos: Boolean = false
  var ugi: UserGroupInformation = null

  val auth = getOptionalField("authentication", parsed_json, adapterSpecificConfig_json, "").toString.trim
  if (auth.size > 0) {
    isKerberos = auth.compareToIgnoreCase("kerberos") == 0
    if (isKerberos) {
      try {
        val regionserver_principal = getOptionalField("regionserver_principal", parsed_json, adapterSpecificConfig_json, "").toString.trim
        val master_principal = getOptionalField("master_principal", parsed_json, adapterSpecificConfig_json, "").toString.trim
        val principal = getOptionalField("principal", parsed_json, adapterSpecificConfig_json, "").toString.trim
        val keytab = getOptionalField("keytab", parsed_json, adapterSpecificConfig_json, "").toString.trim

        logger.debug("HBase info => Hosts:" + hostnames + ", Namespace:" + namespace + ", Principal:" + principal + ", Keytab:" + keytab + ", hbase.regionserver.kerberos.principal:" + regionserver_principal + ", hbase.master.kerberos.principal:" + master_principal)

        config.set("hadoop.proxyuser.hdfs.groups", "*")
        config.set("hadoop.security.authorization", "true")
        config.set("hbase.security.authentication", "kerberos")
        config.set("hadoop.security.authentication", "kerberos")
        config.set("hbase.regionserver.kerberos.principal", regionserver_principal)
        config.set("hbase.master.kerberos.principal", master_principal)

        org.apache.hadoop.security.UserGroupInformation.setConfiguration(config);

        UserGroupInformation.loginUserFromKeytab(principal, keytab);

        ugi = UserGroupInformation.getLoginUser
      } catch {
        case e: Exception => {
          throw CreateConnectionException("HBase issue from JSON configuration string:%s.".format(adapterConfig), e)
        }
      }
    } else {
      throw CreateConnectionException("Not handling any authentication other than KERBEROS. AdapterSpecificConfig:" + adapterConfig, new Exception("Authentication Exception"))
    }
  }

  var autoCreateTables = "YES"
  if (parsed_json.contains("autoCreateTables")) {
    autoCreateTables = parsed_json.get("autoCreateTables").get.toString.trim
  }

  logger.info("HBase info => Hosts:" + hostnames + ", Namespace:" + namespace + ",autoCreateTables:" + autoCreateTables)

  var conn: Connection = _
  try {
    conn = ConnectionFactory.createConnection(config);
  } catch {
    case e: Exception => {
      throw CreateConnectionException("Unable to connect to hbase at " + hostnames, e)
    }
  }

  val admin = conn.getAdmin
  CreateNameSpace(namespace)

  private def relogin: Unit = {
    try {
      if (ugi != null)
        ugi.checkTGTAndReloginFromKeytab
    } catch {
      case e: Exception => {
        logger.error("Failed to relogin into HBase.", e)
        // Not throwing exception from here
      }
    }
  }

  private def createTableFromDescriptor(tableDesc: HTableDescriptor): Unit = {
    try {
      relogin
      admin.createTable(tableDesc);
    } catch {
      case e: Exception => {
        throw CreateDDLException("Failed to create table", e)
      }
    }
  }

  def createAnyTable(containerName: String, columnList: Array[String], apiType:String): Unit = {
    var tableName = toFullTableName(containerName)
    try {
      relogin
      if (!admin.tableExists(TableName.valueOf(tableName))) {
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
        val tableDesc = new HTableDescriptor(TableName.valueOf(tableName));
	columnList.foreach(f => {
          val colDesc = new HColumnDescriptor(f.getBytes())
          tableDesc.addFamily(colDesc)
	})
        createTableFromDescriptor(tableDesc)
	// update local cache
        containerList.add(containerName)
      }
      else{
	// update local cache
        containerList.add(containerName)
      }	
    } catch {
      case e: Exception => {
        throw CreateDDLException("Failed to create table " + tableName, e)
      }
    }
  }

  private def dropTable(tableName: String): Unit = {
    try {
      relogin
      val tblNm = TableName.valueOf(tableName)
      if (admin.tableExists(tblNm)) {
        if (admin.isTableEnabled(tblNm)) {
          admin.disableTable(tblNm)
        }
        admin.deleteTable(tblNm)
      }
    } catch {
      case e: Exception => {
        throw CreateDDLException("Failed to drop table " + tableName, e)
      }
    }
  }

  def DropNameSpace(namespace: String): Unit = lock.synchronized {
    logger.info("Drop namespace " + namespace)
    relogin
    try {
      logger.info("Check whether namespace exists " + namespace)
      val nsd = admin.getNamespaceDescriptor(namespace)
    } catch {
      case e: Exception => {
        logger.info("Namespace " + namespace + " doesn't exist, nothing to delete", e)
        return
      }
    }
    try {
      logger.info("delete namespace: " + namespace)
      admin.deleteNamespace(namespace)
    } catch {
      case e: Exception => {
        throw CreateDDLException("Unable to delete hbase name space " + namespace, e)
      }
    }
  }

  def toTableName(containerName: String): String = {
    // we need to check for other restrictions as well
    // such as length of the table, special characters etc
    namespace + ':' + containerName.toLowerCase.replace('.', '_').replace('-', '_').replace(' ', '_')
  }

  private def toFullTableName(containerName: String): String = {
    // we need to check for other restrictions as well
    // such as length of the table, special characters etc
    toTableName(containerName)
  }

  // accessor used for testing
  def getTableName(containerName: String): String = {
    val t = toTableName(containerName)
    // Remove namespace and send only tableName
    t.stripPrefix(namespace + ":")
  }

  private def getTableFromConnection(tableName: String): Table = {
    try {
      relogin
      return conn.getTable(TableName.valueOf(tableName))
    } catch {
      case e: Exception => {
        throw ConnectionFailedException("Failed to get table " + tableName, e)
      }
    }
    return null
  }

  private def getKeySize(r: Result) : Int = {
    r.getRow().length
  }

  private def updateOpStats(operation: String, tableName: String, opCount: Int) : Unit = lock.synchronized{
    operation match {
      case "get" => {
	if( _getOps.get(tableName) != None ){
	  _getOps(tableName) = _getOps(tableName) + opCount
	}
	else{
	  _getOps(tableName) = + opCount
	}
      }
      case "put" => {
	if( _putOps.get(tableName) != None ){
	  _putOps(tableName) = _putOps(tableName) + opCount
	}
	else{
	  _putOps(tableName) = opCount
	}	  
      }
      case _ => {
        throw CreateDMLException("Internal Error: Failed to Update Op-stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

  private def updateObjStats(operation: String, tableName: String, objCount: Int) : Unit = lock.synchronized{
    operation match {
      case "get" => {
	if( _getObjs.get(tableName) != None ){
	  _getObjs(tableName) = _getObjs(tableName) + objCount
	}
	else{
	  _getObjs(tableName) = + objCount
	}
      }
      case "put" => {
	if( _putObjs.get(tableName) != None ){
	  _putObjs(tableName) = _putObjs(tableName) + objCount
	}
	else{
	  _putObjs(tableName) = objCount
	}	  
      }
      case _ => {
        throw CreateDMLException("Internal Error: Failed to Update Obj-stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

  private def updateByteStats(operation: String, tableName: String, byteCount: Int) : Unit = lock.synchronized{
    operation match {
      case "get" => {
	if( _getBytes.get(tableName) != None ){
	  _getBytes(tableName) = _getBytes(tableName) + byteCount
	}
	else{
	  _getBytes(tableName) = byteCount
	}
      }
      case "put" => {
	if( _putBytes.get(tableName) != None ){
	  _putBytes(tableName) = _putBytes(tableName) + byteCount
	}
	else{
	  _putBytes(tableName) = byteCount
	}
      }
      case _ => {
        throw CreateDMLException("Internal Error: Failed to Update Byte Stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }


  private def getCompositeKey(keyColumns: Array[String], columnValues: Array[(String,String,String)]): String = {
    var keyValue = "";
    var keyColumnValues = columnValues
    keyColumns.foreach(x => {
      keyColumnValues = keyColumnValues.filter(c => c._1.equals(x))
    });
    keyColumnValues.foreach( x => {
      keyValue = keyValue + x._3 + ".";
    })
    return keyValue.dropRight(1);
  }


  def put(containerName: String, keyColumns: Array[String], 
	  columnValues: Array[(String,String,String)]): Unit = {
    var tableName = toFullTableName(containerName)
    var tableHBase: Table = null
    try {
      relogin
      tableHBase = getTableFromConnection(tableName);
      var keyValue = getCompositeKey(keyColumns,columnValues);

      // filter key values from given input map of values
      var nonkeyColumnValues = columnValues
      keyColumns.foreach(x => {
	nonkeyColumnValues = nonkeyColumnValues.filter(c => ! c._1.equals(x))
      });

      var kba = keyValue.getBytes()
      var p = new Put(kba)
      nonkeyColumnValues.foreach( x => {
	p.addColumn(Bytes.toBytes(x._1), Bytes.toBytes(x._2), Bytes.toBytes(x._3))
      })
      tableHBase.put(p)
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to save an object in table " + tableName, e)
      }
    } finally {
      if (tableHBase != null) {
        tableHBase.close()
      }
    }
  }

  private def processCell(k: Array[Byte], columnFamily:String, columnName: String, columnValue: String,callbackFunction: (String, String, String, String) => Unit) {
    try {
      var key = new String(k);
      if (callbackFunction != null)
        (callbackFunction)(key, columnFamily, columnName,columnValue)
    } catch {
      case e: Exception => {
        throw e
      }
    }
  }

  private def isKeyColumn(columnName:String,keyColumns:Array[String]): Boolean = {
    keyColumns.foreach(x => {
      if( x.equalsIgnoreCase(columnName) ){
	return true;
      }
    })
    return false;
  }

  def get(containerName: String, selectList: Array[(String,String)], filterColumns:Array[(String,String,String)], keyColumns: Array[String], callbackFunction: (String, String, String, String) => Unit): Unit = {
    var tableName = toFullTableName(containerName)
    var tableHBase: Table = null
    var valueList: scala.collection.mutable.Map[(String,String), String] = new scala.collection.mutable.HashMap()
    try {
      relogin
      tableHBase = getTableFromConnection(tableName);

      var scan = new Scan();
      // construct filter maps
      val filters = new java.util.ArrayList[Filter]()
      if ( filterColumns.size > 0 ){
	filterColumns.foreach(x => {
	  logger.info("Adding the column %s:%s to the filter".format(x._1,x._2));
	  if( isKeyColumn(x._1,keyColumns) ){
	    logger.info("Creating a row filter for the key column %s to the filter".format(x._1));
	    val filter = new RowFilter(CompareOp.EQUAL,new BinaryComparator(Bytes.toBytes(x._3)));
	    filters.add(filter);
	  }
	  else{
	    logger.info("Creating a  filter for the non-key column %s:%s to the filter".format(x._1,x._2));
	    val filter = new SingleColumnValueFilter(Bytes.toBytes(x._1), Bytes.toBytes(x._2),
						CompareOp.EQUAL, Bytes.toBytes(x._3))
	    filters.add(filter);
	  }
	})
	val filterList = new FilterList(filters);  
	scan.setFilter(filterList)
      }
      var rs = tableHBase.getScanner(scan);
      updateOpStats("get",tableName,1)
      val it = rs.iterator()
      var byteCount = 0
      var recCount = 0
      val selectListByFamily = selectList.groupBy(f => f._1)
      while (it.hasNext()) {
        val r = it.next()
	byteCount = byteCount + getKeySize(r)
	recCount = recCount + 1
	val kvit = r.list().iterator()
	while( kvit.hasNext() ){
	  val kv = kvit.next()
	  val q = Bytes.toString(kv.getFamily())
	  if( selectListByFamily.contains(q) ){
	    val selectList = selectListByFamily(q)
	    selectList.foreach(c => {
	      val colName = c._1
	      val colValue = r.getValue(q.getBytes(),colName.getBytes())
	      if( colValue != null ){
		valueList((q,colName)) = Bytes.toString(r.getValue(q.getBytes(),colName.getBytes()))
		byteCount = byteCount + q.length + colName.length + valueList((q,colName)).length
	      }
	      else{
		logger.info("The value of column (%s,%s) is null".format(q,colName))
	      }
	    })
	  }
	}
	valueList.foreach(x => {
	  val (columnFamily,columnName) = x._1
	  val columnValue = x._2
          processCell(r.getRow(), columnFamily, columnName, columnValue, callbackFunction)
	})
      }
      updateByteStats("get",tableName,byteCount)
      updateObjStats("get",tableName,recCount)
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to fetch data from the table " + tableName, e)
      }
    } finally {
      if (tableHBase != null) {
        tableHBase.close()
      }
    }
  }

  private def TruncateContainer(containerName: String): Unit = {
    var tableName = toFullTableName(containerName)
    var tableHBase: Table = null
    try {
      relogin
      tableHBase = getTableFromConnection(tableName);
      var dels = new ArrayBuffer[Delete]()
      var scan = new Scan()
      val rs = tableHBase.getScanner(scan);
      updateOpStats("get",tableName,1)
      val it = rs.iterator()
      var byteCount = 0
      var recCount = 0
      while (it.hasNext()) {
        val r = it.next()
	byteCount = byteCount + getKeySize(r)
	recCount = recCount + 1
        dels += new Delete(r.getRow())
      }
      updateByteStats("get",tableName,byteCount)
      updateObjStats("get",tableName,recCount)
      if (dels.size > 0)
        tableHBase.delete(new java.util.ArrayList(dels.toList)) // callling tableHBase.delete(dels.toList) results java.lang.UnsupportedOperationException
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to truncate the table " + tableName, e)
      }
    } finally {
      if (tableHBase != null) {
        tableHBase.close()
      }
    }
  }

  def TruncateContainer(containerNames: Array[String]): Unit = {
    logger.info("truncate the container tables")
    containerNames.foreach(cont => {
      logger.info("truncate the container " + cont)
      TruncateContainer(cont)
    })
  }

  private def DropContainer(containerName: String): Unit = lock.synchronized {
    var fullTableName = toFullTableName(containerName)
    try {
      relogin
      dropTable(fullTableName)
    } catch {
      case e: Exception => {
        throw CreateDDLException("Failed to drop the table " + fullTableName, e)
      }
    }
  }

  def DropContainer(containerNames: Array[String]): Unit = {
    logger.info("drop the container tables")
    containerNames.foreach(cont => {
      logger.info("drop the container " + cont)
      DropContainer(cont)
    })
  }

  def dropTables(tbls: Array[String]): Unit = {
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

  def dropTables(tbls: Array[(String, String)]): Unit = {
    dropTables(tbls.map(t => t._1 + ':' + t._2))
  }

  def Shutdown(): Unit = {
    logger.info("close the session and connection pool")
    if (conn != null) {
      conn.close()
      conn = null
    }
  }
}
