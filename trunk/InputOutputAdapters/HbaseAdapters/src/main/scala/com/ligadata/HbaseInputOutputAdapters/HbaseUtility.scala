package com.ligadata.HbaseInputOutputAdapters

import java.util

import com.ligadata.Exceptions.{ConnectionFailedException, FatalAdapterException, KamanjaException}
import com.ligadata.KamanjaBase.{ContainerInterface, SerializeDeserialize}
import com.ligadata.KvBase.{Key, TimeRange, Value}
import com.ligadata.adapterconfiguration.HbaseAdapterConfiguration
import com.ligadata.kamanja.metadata.MdMgr
import com.ligadata.kamanja.metadata.MdMgr._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase._
import org.apache.hadoop.hbase.util.Bytes
import org.apache.logging.log4j.LogManager
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Formats}
import com.ligadata.KamanjaBase._
import com.ligadata.StorageBase.DataStore
import com.ligadata.Utils.KamanjaLoaderInfo
import com.ligadata.keyvaluestore.{HBaseAdapter}
import scala.collection.mutable.ArrayBuffer

/**
  * Created by Yousef on 8/13/2016.
  */
trait LogTrait {
  val loggerName = this.getClass.getName()
  val logger = LogManager.getLogger(loggerName)
}

class HbaseUtility /*extends LogTrait*/{
  var dataDataStoreInfo: String = null
  private val kvManagerLoader = new KamanjaLoaderInfo
  val hbaseConfig = new Configuration()
  var ugi: UserGroupInformation= null
  var conn: Connection=_
  var _getOps:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _getObjs:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _getBytes:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putObjs:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putOps:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putBytes:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  private val siStrBytes = "serializedInfo".getBytes()
  private val baseStrBytes = "base".getBytes()
  private val siStrBytesLen = siStrBytes.length
  private val stStrBytes = "serializerType".getBytes()
  private val stStrBytesLen = stStrBytes.length
  private val schemaIdStrBytes = "schemaId".getBytes()
  private val schemaIdStrBytesLen = schemaIdStrBytes.length
  private[this] val lock = new Object
  var tableName: String=_
  var tenantId: String=_
  var namespace: String=_
  var fulltableName: String=_
  var serName: String=_
//  val tenatInfo = mdMgr.GetTenantInfo(tenantId.toLowerCase)
//  if (tenatInfo == null) {
//    logger.error("Not found tenantInfo for tenantId " + tenantId)
//  } else {
//    if (tenatInfo.primaryDataStore == null || tenatInfo.primaryDataStore.trim.size == 0) {
//      logger.error("Not found valid Primary Datastore for tenantId " + tenantId)
//    } else {
//      dataDataStoreInfo = tenatInfo.primaryDataStore
//    }
//  }
  def createConnection(adapterConfig: HbaseAdapterConfiguration): Configuration = {
    hbaseConfig.setInt("zookeeper.session.timeout", 10000)
    hbaseConfig.setInt("zookeeper.recovery.retry", 1)
    hbaseConfig.setInt("hbase.client.retries.number", 1)
    hbaseConfig.setInt("hbase.client.pause", 10000)
    hbaseConfig.set("hbase.zookeeper.quorum", adapterConfig.hostList)
    if (adapterConfig.kerberos != null) {
      hbaseConfig.set("hadoop.security.authorization", "true")
      hbaseConfig.set("hadoop.proxyuser.hdfs.groups", "*")
      hbaseConfig.set("hadoop.security.authentication", "kerberos")
      hbaseConfig.set("hbase.security.authentication", "kerberos")
      hbaseConfig.set("hbase.master.kerberos.principal", adapterConfig.kerberos.masterPrincipal)
      hbaseConfig.set("hbase.regionserver.kerberos.principal", adapterConfig.kerberos.regionServer)
      org.apache.hadoop.security.UserGroupInformation.setConfiguration(hbaseConfig)
      UserGroupInformation.loginUserFromKeytab(adapterConfig.kerberos.principal, adapterConfig.kerberos.keytab)
      ugi = UserGroupInformation.getLoginUser()
    }
    hbaseConfig
  }

  def toTableName(): String = {
    // we need to check for other restrictions as well
    // such as length of the table, special characters etc
    namespace + ':' + tableName.toLowerCase.replace('.', '_').replace('-', '_').replace(' ', '_')
  }

  def GetDataStoreHandle(dataStoreInfo: String): DataStore = {
    try {
      logger.debug("Getting DB Connection for dataStoreInfo:%s".format(dataStoreInfo))
      return HBaseAdapter.CreateStorageAdapter(kvManagerLoader, dataDataStoreInfo, null, null)
    } catch {
      case e: Exception => throw e
      case e: Throwable => throw e
    }
  }

  def initilizeVariable(adapterConfig: HbaseAdapterConfiguration): Unit={
    tableName = adapterConfig.TableName
    namespace = adapterConfig.scehmaName
    fulltableName = toTableName
    tenantId = adapterConfig.tenantId
  }

  def createDataStorageInfo(adapterConfig: HbaseAdapterConfiguration): String ={
    if (adapterConfig.kerberos != null) {
      dataDataStoreInfo = """{"StoreType": "hbase","SchemaName": "%s","Location":"%s","authentication": "kerberos", "regionserver_principal": %s", "master_principal": "%s", "principal": "%s", "keytab": "%s"}""".format(adapterConfig.scehmaName, adapterConfig.hostList, adapterConfig.kerberos.regionServer, adapterConfig.kerberos.masterPrincipal, adapterConfig.kerberos.principal, adapterConfig.kerberos.keytab)
    } else {
      dataDataStoreInfo = """{"StoreType": "hbase","SchemaName": "%s","Location":"%s"}""".format(adapterConfig.scehmaName, adapterConfig.hostList)
    }
     dataDataStoreInfo
  }
}
