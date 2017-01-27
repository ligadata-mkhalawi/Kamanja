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

package com.ligadata.dataaccessapi

import org.apache.logging.log4j._

// Hbase core
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.NamespaceDescriptor;
// hbase client
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
// hbase filters
import org.apache.hadoop.hbase.filter.{ Filter, SingleColumnValueFilter, FirstKeyOnlyFilter, FilterList, CompareFilter }
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp
// hadoop security model
import org.apache.hadoop.security.UserGroupInformation

import org.apache.hadoop.hbase.client.{ BufferedMutator, BufferedMutatorParams, Connection, ConnectionFactory }

import org.apache.hadoop.hbase._

import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.ligadata.Exceptions._
import com.ligadata.Utils.{ KamanjaLoaderInfo }
import com.ligadata.KvBase.{ Key, TimeRange, Value }
import com.ligadata.StorageBase.{ DataStore, Transaction, StorageAdapterFactory }

import scala.collection.mutable.ArrayBuffer

import scala.collection.JavaConversions._

class HBaseDataAccessAdapter(val configJson: String) extends DataAccessAPI {
  val adapterConfig = if (configJson != null) configJson.trim else ""
  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)

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
//      externalizeExceptionEvent(e)
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
//          externalizeExceptionEvent(e)
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
//          externalizeExceptionEvent(e)
          throw CreateConnectionException("HBase issue from JSON configuration string:%s.".format(adapterConfig), e)
        }
      }
    } else {
      throw CreateConnectionException("Not handling any authentication other than KERBEROS. AdapterSpecificConfig:" + adapterConfig, new Exception("Authentication Exception"))
    }
  }

  var conn: Connection = _
  try {
    conn = ConnectionFactory.createConnection(config);
  } catch {
    case e: Exception => {
//      externalizeExceptionEvent(e)
      throw CreateConnectionException("Unable to connect to hbase at " + hostnames, e)
    }
  }

  val admin = conn.getAdmin

  private def relogin: Unit = {
    try {
      if (ugi != null)
        ugi.checkTGTAndReloginFromKeytab
    } catch {
      case e: Exception => {
//        externalizeExceptionEvent(e)
        logger.error("Failed to relogin into HBase.", e)
        // Not throwing exception from here
      }
    }
  }

  def toTableName(containerName: String): String = {
    // we need to check for other restrictions as well
    // such as length of the table, special characters etc
    namespace + ':' + containerName.toLowerCase.replace('.', '_').replace('-', '_').replace(' ', '_')
  }

  private def getTableFromConnection(tableName: String): Table = {
    try {
      relogin
      return conn.getTable(TableName.valueOf(tableName))
    } catch {
      case e: Exception => {
//        externalizeExceptionEvent(e)
        throw ConnectionFailedException("Failed to get table " + tableName, e)
      }
    }

    return null
  }

  override def get(fullContainerName: String, select: Array[AttributeDef], keys: Array[String]): Array[Map[String, Any]] = {
    var tableName = toTableName(fullContainerName)
    var tableHBase: Table = null
    try {

      relogin
      tableHBase = getTableFromConnection(tableName);

      val keyGets = new java.util.ArrayList[Get]()
      keys.foreach(key => {
        val f = new Get(key.getBytes)
        keyGets.add(f);
      })
      
      val rs = tableHBase.get(keyGets);
      var byteCount = 0
      val results = new ArrayBuffer[Map[String, Any]](rs.length)
      rs.foreach ( r => {
	      //byteCount = byteCount + getRowSize(r)
//        val cells = r.rawCells()
        val attrs = scala.collection.mutable.Map[String, Any]() //(cells.length)
        select.foreach( a => {
          val c = r.getColumnLatestCell("default".getBytes, a.name.getBytes)
          if(c != null) {
            val name = new String(CellUtil.cloneQualifier(c))
            val value = new String(CellUtil.cloneValue(c))
            attrs.put(name, value)
          }
        })
        if(attrs.size > 0)
          results.add(attrs.toMap)
      })
      results.toArray
    } catch {
      case e: Exception => {
//        externalizeExceptionEvent(e)
        throw CreateDMLException("Failed to fetch data from the table " + tableName, e)
      }
    } finally {
      if (tableHBase != null) {
        tableHBase.close()
      }
    }
  }
  
  def getDataContainerDefinitionsV1(): Map[String, DataContainerDef] = {
    var tableName = toTableName("DataContainer")
    var tableHBase: Table = null
    try {

      relogin
      tableHBase = getTableFromConnection(tableName);

      val scan = new Scan()      
      val rs = tableHBase.getScanner(scan);
      val results = scala.collection.mutable.Map[String, DataContainerDef]()
      rs.foreach ( r => {
        
        val name = new String(r.getRow)
        
        var c = r.getColumnLatestCell("base".getBytes, "definition".getBytes)        
        val defStr = new String(CellUtil.cloneValue(c))
        val containerDef = parse(defStr).values.asInstanceOf[Map[String, Any]]
        
        val fullName = containerDef.getOrElse("FullName", "null").toString
        
        val attributes = new ArrayBuffer[AttributeDef]
        val fields = containerDef.get("Fields").get.asInstanceOf[List[Any]]
        fields.foreach( f => {
          val field = f.asInstanceOf[Map[String, String]]
          attributes.add(new AttributeDef(field.getOrElse("Name", "null").toString, field.getOrElse("Type", "null").toString))
        })

        val attributeGroups = scala.collection.mutable.Map[String, AttributeGroupDef]()
        val groups = containerDef.get("FieldGroups").get.asInstanceOf[Map[String, Any]]
        groups.keys.foreach( grpName => {
          val attrs = groups.get(grpName).get.asInstanceOf[List[String]]
          val grpAttr = new ArrayBuffer[AttributeDef]
          attrs.foreach(a => {
              grpAttr.add(new AttributeDef(a, ""))            
          })
          attributeGroups.put(grpName, new AttributeGroupDef(grpName, grpAttr.toArray))
        })

        results.put(name, new DataContainerDef(name, fullName, attributes.toArray, attributeGroups.toMap))
      })
      results.toMap
    } catch {
      case e: Exception => {
//        externalizeExceptionEvent(e)
        throw CreateDMLException("Failed to fetch data from the table " + tableName, e)
      }
    } finally {
      if (tableHBase != null) {
        tableHBase.close()
      }
    }      
  }
  
  def getDataContainerDefinitionsV2(): Map[String, DataContainerDef] = {
    var tableName = toTableName("DataContainer")
    var tableHBase: Table = null
    try {

      relogin
      tableHBase = getTableFromConnection(tableName);

      val scan = new Scan()      
      val rs = tableHBase.getScanner(scan);
      val results = scala.collection.mutable.Map[String, DataContainerDef]()
      rs.foreach ( r => {
        var c = r.getColumnLatestCell("base".getBytes, "name".getBytes)
        val name = new String(CellUtil.cloneValue(c))
        c = r.getColumnLatestCell("base".getBytes, "fullcontainername".getBytes)
        val fullName = new String(CellUtil.cloneValue(c))
        val attributeGroups = scala.collection.mutable.Map[String, AttributeGroupDef]()
        val attributes = new ArrayBuffer[AttributeDef]
        
        val families = r.getNoVersionMap
        families.entrySet.foreach( f => {
          val fam = new String(f.getKey)
          if(fam.equalsIgnoreCase("attributes")) {
            f.getValue.entrySet.foreach(a => {
              attributes.add(new AttributeDef(new String(a.getKey), new String(a.getValue)))
            })
          } else if(!fam.equalsIgnoreCase("base")) {
            val grpAttr = new ArrayBuffer[AttributeDef]
            f.getValue.entrySet.foreach(a => {
              grpAttr.add(new AttributeDef(new String(a.getKey), new String(a.getValue)))            
            })
            attributeGroups.put(fam, new AttributeGroupDef(fam, grpAttr.toArray))
          }
        })

        results.put(name, new DataContainerDef(name, fullName, attributes.toArray, attributeGroups.toMap))
      })
      results.toMap
    } catch {
      case e: Exception => {
//        externalizeExceptionEvent(e)
        throw CreateDMLException("Failed to fetch data from the table " + tableName, e)
      }
    } finally {
      if (tableHBase != null) {
        tableHBase.close()
      }
    }    
  }

  def Shutdown(): Unit = {
    logger.info("close the session and connection pool")
    if (conn != null) {
      conn.close()
      conn = null
    }
  }
}

object HBaseDataAccessAdapter {
  var api: HBaseDataAccessAdapter = _

  def Init(configJson: String) = {
    api = new HBaseDataAccessAdapter(configJson)
  }
  
  def Shutdown() = api.Shutdown() 
}
