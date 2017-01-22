package com.ligadata.ElasticsearchInputOutputAdapters

import com.ligadata.AdapterConfiguration.ElasticsearchAdapterConfiguration
import com.ligadata.StorageBase.DataStore
import com.ligadata.Utils.KamanjaLoaderInfo
import com.ligadata.kamanja.metadata.MdMgr._
import com.ligadata.keyvaluestore.ElasticsearchAdapter
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.security.UserGroupInformation


class ElasticsearchUtility /*extends LogTrait*/ {
  var dataDataStoreInfo: String = null
  private val kvManagerLoader = new KamanjaLoaderInfo
  val elasticsearchConfig = new Configuration()
  var ugi: UserGroupInformation = null
  var conn: Connection = _
  var _getOps: scala.collection.mutable.Map[String, Long] = new scala.collection.mutable.HashMap()
  var _getObjs: scala.collection.mutable.Map[String, Long] = new scala.collection.mutable.HashMap()
  var _getBytes: scala.collection.mutable.Map[String, Long] = new scala.collection.mutable.HashMap()
  var _putObjs: scala.collection.mutable.Map[String, Long] = new scala.collection.mutable.HashMap()
  var _putOps: scala.collection.mutable.Map[String, Long] = new scala.collection.mutable.HashMap()
  var _putBytes: scala.collection.mutable.Map[String, Long] = new scala.collection.mutable.HashMap()
  private val siStrBytes = "serializedInfo".getBytes()
  private val baseStrBytes = "base".getBytes()
  private val siStrBytesLen = siStrBytes.length
  private val stStrBytes = "serializerType".getBytes()
  private val stStrBytesLen = stStrBytes.length
  private val schemaIdStrBytes = "schemaId".getBytes()
  private val schemaIdStrBytesLen = schemaIdStrBytes.length
  private[this] val lock = new Object
  var tableName: String = _
  var tenantId: String = _
  var namespace: String = _
  var fulltableName: String = _
  var serName: String = _

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
  def createConnection(adapterConfig: ElasticsearchAdapterConfiguration): Configuration = {
    elasticsearchConfig.setInt("zookeeper.session.timeout", 10000)
    elasticsearchConfig.setInt("zookeeper.recovery.retry", 1)
    elasticsearchConfig.setInt("elasticsearch.client.retries.number", 1)
    elasticsearchConfig.setInt("elasticsearch.client.pause", 10000)
    elasticsearchConfig.set("elasticsearch.zookeeper.quorum", adapterConfig.hostList)
//    if (adapterConfig.kerberos != null) {
      //      elasticsearchConfig.set("hadoop.security.authorization", "true")
      //      elasticsearchConfig.set("hadoop.proxyuser.hdfs.groups", "*")
      //      elasticsearchConfig.set("hadoop.security.authentication", "kerberos")
      //      elasticsearchConfig.set("hbase.security.authentication", "kerberos")
      //      elasticsearchConfig.set("hbase.master.kerberos.principal", adapterConfig.kerberos.masterPrincipal)
      //      elasticsearchConfig.set("hbase.regionserver.kerberos.principal", adapterConfig.kerberos.regionServer)
      //      org.apache.hadoop.security.UserGroupInformation.setConfiguration(elasticsearchConfig)
      //      UserGroupInformation.loginUserFromKeytab(adapterConfig.kerberos.principal, adapterConfig.kerberos.keytab)
      //      ugi = UserGroupInformation.getLoginUser()
 //   }
    elasticsearchConfig
  }

  def toTableName(): String = {
    // we need to check for other restrictions as well
    // such as length of the table, special characters etc
    namespace + ':' + tableName.toLowerCase.replace('.', '_').replace('-', '_').replace(' ', '_')
  }

  def GetDataStoreHandle(dataStoreInfo: String): DataStore = {
    try {
      logger.debug("Getting DB Connection for dataStoreInfo:%s".format(dataStoreInfo))
      return ElasticsearchAdapter.CreateStorageAdapter(kvManagerLoader, dataDataStoreInfo, null, null)
    } catch {
      case e: Exception => throw e
      case e: Throwable => throw e
    }
  }

  def initilizeVariable(adapterConfig: ElasticsearchAdapterConfiguration): Unit = {
    tableName = adapterConfig.TableName
    namespace = adapterConfig.scehmaName
    fulltableName = toTableName
    tenantId = adapterConfig.tenantId
  }

  def createDataStorageInfo(adapterConfig: ElasticsearchAdapterConfiguration): String = {
//    if (adapterConfig.kerberos != null) {
//      dataDataStoreInfo = """{"StoreType": "elasticsearch","SchemaName": "%s","Location":"%s","clusterName":"%s", "authentication": "kerberos", "regionserver_principal": %s", "master_principal": "%s", "principal": "%s", "keytab": "%s"}""".format(adapterConfig.scehmaName, adapterConfig.hostList, adapterConfig.clusterName, adapterConfig.kerberos.regionServer, adapterConfig.kerberos.masterPrincipal, adapterConfig.kerberos.principal, adapterConfig.kerberos.keytab)
//    } else {
//      dataDataStoreInfo = """{"StoreType": "elasticsearch","SchemaName": "%s","Location":"%s","clusterName":"%s"}""".format(adapterConfig.scehmaName, adapterConfig.hostList, adapterConfig.clusterName)
//    }
    dataDataStoreInfo
  }
}