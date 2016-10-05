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

package com.ligadata.MetadataAPI

import java.util.Properties
import java.io._
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.text.ParseException
import com.ligadata.MetadataAPI.MetadataAPI.ModelType
import com.ligadata.MetadataAPI.MetadataAPI.ModelType.ModelType

import scala.Enumeration
import scala.io._
import scala.collection.mutable.ArrayBuffer

import scala.collection.mutable._
import scala.reflect.runtime.{ universe => ru }

import com.ligadata.kamanja.metadata.ObjType._
import com.ligadata.kamanja.metadata._
import com.ligadata.kamanja.metadata.MdMgr._

import com.ligadata.kamanja.metadataload.MetadataLoad

// import com.ligadata.keyvaluestore._
import com.ligadata.HeartBeat.{MonitoringContext, HeartBeatUtil}
import com.ligadata.StorageBase.{ DataStore, Transaction }
import com.ligadata.KvBase.{ Key, TimeRange }

import scala.util.parsing.json.JSON
import scala.util.parsing.json.{ JSONObject, JSONArray }
import scala.collection.immutable.Map
import scala.collection.immutable.HashMap
import scala.collection.mutable.HashMap

import com.google.common.base.Throwables

import com.ligadata.msgcompiler._
import com.ligadata.Exceptions._

import scala.xml.XML
import org.apache.logging.log4j._

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import com.ligadata.ZooKeeper._
import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.CreateMode

import com.ligadata.keyvaluestore._
import com.ligadata.Serialize._
import com.ligadata.Utils.{Utils, KamanjaClassLoader, KamanjaLoaderInfo}
import scala.util.control.Breaks._
import com.ligadata.AuditAdapterInfo._
import com.ligadata.SecurityAdapterInfo.SecurityAdapter
import com.ligadata.keyvaluestore.KeyValueManager
import com.ligadata.Exceptions.StackTrace

import java.util.Date
import org.json4s.jackson.Serialization
import sys.process._

// The implementation class
object ConfigUtils {
  lazy val serializerType = "json4s"//"kryo"
  // 646 - 676 Change begins - replace MetadataAPIImpl
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Change ends

  //lazy val serializer = SerializerManager.GetSerializer(serializerType)
  /**
   *
   */
  private var cfgmap: Map[String, Any] = null

  // For future debugging  purposes, we want to know which properties were not set - so create a set
  // of values that can be set via our config files
  var pList: Set[String] = Set("ZK_SESSION_TIMEOUT_MS", "ZK_CONNECTION_TIMEOUT_MS", "DATABASE_SCHEMA", "DATABASE", "DATABASE_LOCATION", "DATABASE_HOST", "API_LEADER_SELECTION_ZK_NODE",
    "JAR_PATHS", "JAR_TARGET_DIR", "ROOT_DIR", "GIT_ROOT", "SCALA_HOME", "JAVA_HOME", "MANIFEST_PATH", "CLASSPATH", "NOTIFY_ENGINE", "SERVICE_HOST",
    "ZNODE_PATH", "ZOOKEEPER_CONNECT_STRING", "COMPILER_WORK_DIR", "SERVICE_PORT", "MODEL_FILES_DIR", "TYPE_FILES_DIR", "FUNCTION_FILES_DIR",
    "CONCEPT_FILES_DIR", "MESSAGE_FILES_DIR", "CONTAINER_FILES_DIR", "CONFIG_FILES_DIR", "MODEL_EXEC_LOG", "NODE_ID", "SSL_CERTIFICATE", "SSL_PASSWD", "DO_AUTH", "SECURITY_IMPL_CLASS",
    "SECURITY_IMPL_JAR", "AUDIT_IMPL_CLASS", "AUDIT_IMPL_JAR", "DO_AUDIT", "AUDIT_PARMS", "ADAPTER_SPECIFIC_CONFIG", "METADATA_DATASTORE")


  // This is used to exclude all non-engine related configs from Uplodad Config method
  private val excludeList: Set[String] = Set[String]("ClusterId", "Nodes", "Config", "Adapters", "SystemCatalog", "ZooKeeperInfo", "EnvironmentContext", "Cache")

    /**
     * AddNode
      *
      * @param nodeId a cluster node
     * @param nodePort
     * @param nodeIpAddr
     * @param jarPaths Set of paths where jars are located
     * @param scala_home
     * @param java_home
     * @param classpath
     * @param clusterId
     * @param power
     * @param roles
     * @param description
     * @return
     */
  def AddNode(nodeId: String, nodePort: Int, nodeIpAddr: String,
    jarPaths: List[String], scala_home: String,
    java_home: String, classpath: String,
    clusterId: String, power: Int,
    roles: Array[String], description: String): String = {
    try {
      // save in memory
      val ni = MdMgr.GetMdMgr.MakeNode(nodeId, nodePort, nodeIpAddr, jarPaths, scala_home,
        java_home, classpath, clusterId, power, roles, description)
      MdMgr.GetMdMgr.AddNode(ni)
      // save in database
      val key = "NodeInfo." + nodeId
      val value = MetadataAPISerialization.serializeObjectToJson(ni).getBytes//serializer.SerializeObjectToByteArray(ni)
      getMetadataAPI.SaveObject(key.toLowerCase, value, "config_objects", serializerType)
      var apiResult = new ApiResult(ErrorCodeConstants.Success, "AddNode", null, ErrorCodeConstants.Add_Node_Successful + ":" + nodeId)
      apiResult.toString()
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "AddNode", null, "Error :" + e.toString() + ErrorCodeConstants.Add_Node_Failed + ":" + nodeId)
        apiResult.toString()
      }
    }
  }

    /**
     * UpdateNode
      *
      * @param nodeId a cluster node
     * @param nodePort
     * @param nodeIpAddr
     * @param jarPaths Set of paths where jars are located
     * @param scala_home
     * @param java_home
     * @param classpath
     * @param clusterId
     * @param power
     * @param roles
     * @param description
     * @return
     */
  def UpdateNode(nodeId: String, nodePort: Int, nodeIpAddr: String,
    jarPaths: List[String], scala_home: String,
    java_home: String, classpath: String,
    clusterId: String, power: Int,
    roles: Array[String], description: String): String = {
    AddNode(nodeId, nodePort, nodeIpAddr, jarPaths, scala_home,
      java_home, classpath,
      clusterId, power, roles, description)
  }

    /**
     * RemoveNode
      *
      * @param nodeId a cluster node
     * @return
     */
  def RemoveNode(nodeId: String): String = {
    try {
      MdMgr.GetMdMgr.RemoveNode(nodeId)
      val key = "NodeInfo." + nodeId
      getMetadataAPI.DeleteObject(key.toLowerCase, "config_objects")
      var apiResult = new ApiResult(ErrorCodeConstants.Success, "RemoveNode", null, ErrorCodeConstants.Remove_Node_Successful + ":" + nodeId)
      apiResult.toString()
    } catch {
      case e: Exception => {
        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "RemoveNode", null, "Error :" + e.toString() + ErrorCodeConstants.Remove_Node_Failed + ":" + nodeId)
        apiResult.toString()
      }
    }
  }

  def AddTenant(tenantId: String, description: String, primaryDataStore: String, cacheConfig: String): String = {
    try {
      // save in memory
      val ti = MdMgr.GetMdMgr.MakeTenantInfo(tenantId, description, primaryDataStore, cacheConfig)
      MdMgr.GetMdMgr.AddTenantInfo(ti)
      // save in database
      val key = "TenantInfo." + tenantId.trim.toLowerCase()
      val value = MetadataAPISerialization.serializeObjectToJson(ti).getBytes //serializer.SerializeObjectToByteArray(ti)
      getMetadataAPI.SaveObject(key.toLowerCase, value, "config_objects", serializerType)
      var apiResult = new ApiResult(ErrorCodeConstants.Success, "AddTenant", null, ErrorCodeConstants.Add_Tenant_Successful + ":" + tenantId)
      apiResult.toString()
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "AddTenant", null, "Error :" + e.toString() + ErrorCodeConstants.Add_Tenant_Failed + ":" + tenantId)
        apiResult.toString()
      }
    }
  }

  def UpdateTenant(tenantId: String, description: String, primaryDataStore: String, cacheConfig: String): String = {
    AddTenant(tenantId, description, primaryDataStore, cacheConfig)
  }

  /**
    * RemoveNode
    *
    * @param tenantId a cluster node
    * @return
    */
  def RemoveTenant(tenantId: String): String = {
    try {
      MdMgr.GetMdMgr.RemoveTenantInfo(tenantId)
      val key = "TenantInfo." + tenantId.trim.toLowerCase()
      getMetadataAPI.DeleteObject(key.toLowerCase, "config_objects")
      var apiResult = new ApiResult(ErrorCodeConstants.Success, "RemoveNode", null, ErrorCodeConstants.Remove_Tenant_Successful + ":" + tenantId)
      apiResult.toString()
    } catch {
      case e: Exception => {
        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "RemoveNode", null, "Error :" + e.toString() + ErrorCodeConstants.Remove_Tenant_Failed + ":" + tenantId)
        apiResult.toString()
      }
    }
  }

  /**
     * AddAdapter
    *
    * @param name
     * @param typeString
     * @param className
     * @param jarName
     * @param dependencyJars
     * @param adapterSpecificCfg
     * @return
     */
  def AddAdapter(name: String, typeString: String, className: String,
                 jarName: String, dependencyJars: List[String],
                 adapterSpecificCfg: String, tenantId: String, fullAdapterConfig: String): String = {
    try {
      // save in memory
      val ai = MdMgr.GetMdMgr.MakeAdapter(name, typeString, className, jarName, dependencyJars, adapterSpecificCfg, tenantId, fullAdapterConfig)
      MdMgr.GetMdMgr.AddAdapter(ai)
      // save in database
      val key = "AdapterInfo." + name
      val value = MetadataAPISerialization.serializeObjectToJson(ai).getBytes//serializer.SerializeObjectToByteArray(ai)
      getMetadataAPI.SaveObject(key.toLowerCase, value, "config_objects", serializerType)
      var apiResult = new ApiResult(ErrorCodeConstants.Success, "AddAdapter", null, ErrorCodeConstants.Add_Adapter_Successful + ":" + name)
      apiResult.toString()
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "AddAdapter", null, "Error :" + e.toString() + ErrorCodeConstants.Add_Adapter_Failed + ":" + name)
        apiResult.toString()
      }
    }
  }

    /**
     * RemoveAdapter
      *
      * @param name
     * @param typeString
     * @param className
     * @param jarName
     * @param dependencyJars
     * @param adapterSpecificCfg
     * @return
     */
  def UpdateAdapter(name: String, typeString: String, className: String,
                    jarName: String, dependencyJars: List[String],
                    adapterSpecificCfg: String, tenantId: String, fullAdapterConfig: String): String = {
    AddAdapter(name, typeString, className, jarName, dependencyJars, adapterSpecificCfg, tenantId, fullAdapterConfig)
  }

    /**
     * RemoveAdapter
      *
      * @param name
     * @return
     */
  def RemoveAdapter(name: String): String = {
    try {
      MdMgr.GetMdMgr.RemoveAdapter(name)
      val key = "AdapterInfo." + name
      getMetadataAPI.DeleteObject(key.toLowerCase, "config_objects")
      var apiResult = new ApiResult(ErrorCodeConstants.Success, "RemoveAdapter", null, ErrorCodeConstants.Remove_Adapter_Successful + ":" + name)
      apiResult.toString()
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "RemoveAdapter", null, "Error :" + e.toString() + ErrorCodeConstants.Remove_Adapter_Failed + ":" + name)
        apiResult.toString()
      }
    }
  }

    /**
     * AddCluster
      *
      * @param clusterId
     * @param description
     * @param privileges
     * @return
     */
  def AddCluster(clusterId: String, description: String, privileges: String): String = {
    try {
      // save in memory
      val ci = MdMgr.GetMdMgr.MakeCluster(clusterId, description, privileges)
      MdMgr.GetMdMgr.AddCluster(ci)
      // save in database
      val key = "ClusterInfo." + clusterId
      val value = MetadataAPISerialization.serializeObjectToJson(ci).getBytes//serializer.SerializeObjectToByteArray(ci)
      getMetadataAPI.SaveObject(key.toLowerCase, value, "config_objects", serializerType)
      var apiResult = new ApiResult(ErrorCodeConstants.Success, "AddCluster", null, ErrorCodeConstants.Add_Cluster_Successful + ":" + clusterId)
      apiResult.toString()
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "AddCluster", null, "Error :" + e.toString() + ErrorCodeConstants.Add_Cluster_Failed + ":" + clusterId)
        apiResult.toString()
      }
    }
  }

    /**
     * UpdateCluster
      *
      * @param clusterId
     * @param description
     * @param privileges
     * @return
     */
  def UpdateCluster(clusterId: String, description: String, privileges: String): String = {
    AddCluster(clusterId, description, privileges)
  }

    /**
     * RemoveCluster
      *
      * @param clusterId
     * @return
     */
  def RemoveCluster(clusterId: String): String = {
    try {
      MdMgr.GetMdMgr.RemoveCluster(clusterId)
      val key = "ClusterInfo." + clusterId
      getMetadataAPI.DeleteObject(key.toLowerCase, "config_objects")
      var apiResult = new ApiResult(ErrorCodeConstants.Success, "RemoveCluster", null, ErrorCodeConstants.Remove_Cluster_Successful + ":" + clusterId)
      apiResult.toString()
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "RemoveCluster", null, "Error :" + e.toString() + ErrorCodeConstants.Remove_Cluster_Failed + ":" + clusterId)
        apiResult.toString()
      }
    }
  }

    /**
     * Add a cluster configuration from the supplied map with the supplied identifer key
      *
      * @param clusterCfgId cluster id to add
     * @param cfgMap the configuration map
     * @param modifiedTime when modified
     * @param createdTime when created
     * @return results string
     */
  def AddClusterCfg(clusterCfgId: String, cfgMap: scala.collection.mutable.HashMap[String, String],
    modifiedTime: Date, createdTime: Date): String = {
    try {
      // save in memory
      val ci = MdMgr.GetMdMgr.MakeClusterCfg(clusterCfgId, cfgMap, modifiedTime, createdTime)
      MdMgr.GetMdMgr.AddClusterCfg(ci)
      // save in database
      val key = "ClusterCfgInfo." + clusterCfgId
      val value = MetadataAPISerialization.serializeObjectToJson(ci).getBytes//serializer.SerializeObjectToByteArray(ci)
      getMetadataAPI.SaveObject(key.toLowerCase, value, "config_objects", serializerType)
      var apiResult = new ApiResult(ErrorCodeConstants.Success, "AddClusterCfg", null, ErrorCodeConstants.Add_Cluster_Config_Successful + ":" + clusterCfgId)
      apiResult.toString()
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "AddClusterCfg", null, "Error :" + e.toString() + ErrorCodeConstants.Add_Cluster_Config_Failed + ":" + clusterCfgId)
        apiResult.toString()
      }
    }
  }

    /**
     * Update te configuration for the cluster with the supplied id
      *
      * @param clusterCfgId
     * @param cfgMap
     * @param modifiedTime
     * @param createdTime
     * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
     *               method. If Security and/or Audit are configured, this value must be a value other than None.
     * @return
     */
  def UpdateClusterCfg(clusterCfgId: String, cfgMap: scala.collection.mutable.HashMap[String, String],
    modifiedTime: Date, createdTime: Date, userid: Option[String] = None): String = {
    AddClusterCfg(clusterCfgId, cfgMap, modifiedTime, createdTime)
  }

    /**
     * Remove a cluster configuration with the suppplied id
     *
     * @param clusterCfgId
     * @return results string
     */
  def RemoveClusterCfg(clusterCfgId: String, userid: Option[String] = None): String = {
    try {
      MdMgr.GetMdMgr.RemoveClusterCfg(clusterCfgId)
      val key = "ClusterCfgInfo." + clusterCfgId
      getMetadataAPI.DeleteObject(key.toLowerCase, "config_objects")
      var apiResult = new ApiResult(ErrorCodeConstants.Success, "RemoveCLusterCfg", null, ErrorCodeConstants.Remove_Cluster_Config_Successful + ":" + clusterCfgId)
      apiResult.toString()
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "RemoveCLusterCfg", null, "Error :" + e.toString() + ErrorCodeConstants.Remove_Cluster_Config_Failed + ":" + clusterCfgId)
        apiResult.toString()
      }
    }
  }

    /**
     * Remove a cluster configuration
      *
      * @param cfgStr
     * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
     *               method. If Security and/or Audit are configured, this value must be a value other than None.
     * @param cobjects
     * @return results string
     */
  def RemoveConfig(cfgStr: String, userid: Option[String], cobjects: String): String = {
    var keyList = new Array[String](0)
      var clusterNotifications: ArrayBuffer[BaseElemDef] = new ArrayBuffer[BaseElemDef]
      var clusterNotifyActions: ArrayBuffer[String] =  new ArrayBuffer[String]
      //BOOOYA
    getMetadataAPI.logAuditRec(userid, Some(AuditConstants.WRITE), AuditConstants.REMOVECONFIG, cfgStr, AuditConstants.SUCCESS, "", cobjects)
    try {
      // extract config objects
      val map = JsonSerializer.parseEngineConfig(cfgStr)
      // process clusterInfo object if it exists
      if (map.contains("Clusters")) {
        var globalAdaptersCollected = false // to support previous versions
        val clustersList = map.get("Clusters").get.asInstanceOf[List[_]] //BUGBUG:: Do we need to check the type before converting
        logger.debug("Found " + clustersList.length + " cluster objects ")
        clustersList.foreach(clustny => {
          val cluster = clustny.asInstanceOf[Map[String, Any]] //BUGBUG:: Do we need to check the type before converting
          val ClusterId = cluster.getOrElse("ClusterId", "").toString.trim.toLowerCase

          MdMgr.GetMdMgr.RemoveCluster(ClusterId)
          var key = "ClusterInfo." + ClusterId
          keyList = keyList :+ key.toLowerCase

          if (ClusterId.length > 0) {
            var clusterDef: ClusterConfigDef = new ClusterConfigDef
            clusterDef.clusterId = ClusterId
            clusterDef.elementType = "clusterDef"
            clusterDef.nameSpace = "cluster"
            clusterDef.name = ClusterId
            clusterDef.tranId = getMetadataAPI.GetNewTranId
            clusterNotifications.append(clusterDef)
            clusterNotifyActions.append("Remove")
          }

          MdMgr.GetMdMgr.RemoveClusterCfg(ClusterId)
          key = "ClusterCfgInfo." + ClusterId
          keyList = keyList :+ key.toLowerCase

          if (ClusterId.length > 0) {
            var clusterInfoDef: ClusterConfigDef = new ClusterConfigDef
            clusterInfoDef.clusterId = ClusterId
            clusterInfoDef.elementType = "clusterInfoDef"
            clusterInfoDef.name = ClusterId
            clusterInfoDef.nameSpace = "clusterInfo"
            clusterInfoDef.tranId = getMetadataAPI.GetNewTranId
            clusterNotifications.append(clusterInfoDef)
            clusterNotifyActions.append("Remove")
          }

          if (cluster.contains("Nodes")) {
            val nodes = cluster.get("Nodes").get.asInstanceOf[List[_]]
            nodes.foreach(n => {
              val node = n.asInstanceOf[Map[String, Any]]
              val nodeId = node.getOrElse("NodeId", "").toString.trim.toLowerCase
              if (nodeId.size > 0) {
                MdMgr.GetMdMgr.RemoveNode(nodeId.toLowerCase)
                key = "NodeInfo." + nodeId
                keyList = keyList :+ key.toLowerCase

                var nodeDef: ClusterConfigDef = new ClusterConfigDef
                nodeDef.name = nodeId
                nodeDef.tranId = getMetadataAPI.GetNewTranId
                nodeDef.nameSpace = "nodeIds"
                nodeDef.clusterId = nodeId
                nodeDef.elementType = "nodeDef"
                clusterNotifications.append(nodeDef)
                clusterNotifyActions.append("Remove")
              }
            })
          }

          if (cluster.contains("Tenants")) {
            val tenants = cluster.get("Tenants").get.asInstanceOf[List[_]]
            tenants.foreach(t => {
              val tenant = t.asInstanceOf[Map[String, Any]]
              val tenantId = tenant.getOrElse("TenantId", "").toString.trim
              if (tenantId.trim.size > 0) {
                MdMgr.GetMdMgr.RemoveTenantInfo(tenantId)
                key = "TenantInfo." + tenantId.trim.toLowerCase()
                keyList = keyList :+ key.toLowerCase

                if (tenantId.length > 0) {
                  val tenantDef: ClusterConfigDef = new ClusterConfigDef
                  tenantDef.name =  tenantId.trim.toLowerCase()
                  tenantDef.tranId = getMetadataAPI.GetNewTranId
                  tenantDef.nameSpace = "Tenants"
                  tenantDef.clusterId = ClusterId
                  tenantDef.elementType = "TenantDef"
                  clusterNotifications.append(tenantDef)
                  clusterNotifyActions.append("Remove")
                }
              }
            })
          }

          if (cluster.contains("Adapters") || (globalAdaptersCollected == false && map.contains("Adapters"))) {
            val adapters = if (cluster.contains("Adapters") && (globalAdaptersCollected == false && map.contains("Adapters"))) {
              map.get("Adapters").get.asInstanceOf[List[_]] ++ cluster.get("Adapters").get.asInstanceOf[List[_]]
            } else if (cluster.contains("Adapters")) {
              cluster.get("Adapters").get.asInstanceOf[List[_]]
            } else if (globalAdaptersCollected == false && map.contains("Adapters")) {
              map.get("Adapters").get.asInstanceOf[List[_]]
            } else {
              List[Any]()
            }

            globalAdaptersCollected = true // to support previous versions

            adapters.foreach(a => {
              val adap = a.asInstanceOf[Map[String, Any]]
              val nm = adap.getOrElse("Name", "").toString.trim.toLowerCase
              if (nm.size > 0) {
                MdMgr.GetMdMgr.RemoveAdapter(nm)
                val key = "AdapterInfo." + nm
                keyList = keyList :+ key.toLowerCase

                if (nm.length > 0) {
                  var adapterDef: ClusterConfigDef = new ClusterConfigDef
                  adapterDef.name = nm
                  adapterDef.nameSpace = ClusterId
                  adapterDef.tranId = getMetadataAPI.GetNewTranId
                  adapterDef.clusterId = ClusterId
                  adapterDef.elementType = "adapterDef"
                  clusterNotifications.append(adapterDef)
                  clusterNotifyActions.append("Remove")
                }
              }
            })
          }
        })
      }
      if (keyList.size > 0) {
        getMetadataAPI.RemoveObjectList(keyList, "config_objects")
        getMetadataAPI.NotifyEngine(clusterNotifications.toArray, clusterNotifyActions.toArray)
      }
      var apiResult = new ApiResult(ErrorCodeConstants.Success, "RemoveConfig", null, ErrorCodeConstants.Remove_Config_Successful + ":" + cfgStr)
      apiResult.toString()
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "RemoveConfig", null, "Error :" + e.toString() + ErrorCodeConstants.Remove_Config_Failed + ":" + cfgStr)
        apiResult.toString()
      }
    }
  }


    /**
     * Upload a model config.  These are for native models written in Scala or Java
      *
      * @param cfgStr
     * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
     *               method. If Security and/or Audit are configured, this value must be a value other than None.
     * @param objectList
     * @param isFromNotify
     * @return
     */
  def UploadModelsConfig(cfgStr: String, userid: Option[String], objectList: String, isFromNotify: Boolean = false): String = {
    try{
      var keyList = new Array[String](0)
      var valueList = new Array[Array[Byte]](0)
      val tranId = getMetadataAPI.GetNewTranId
      logger.debug("Parsing ModelConfig : " + cfgStr)
      cfgmap = parse(cfgStr).values.asInstanceOf[Map[String, Any]]
      logger.debug("Count of objects in cfgmap : " + cfgmap.keys.size)

      var i = 0
      // var objectsAdded: scala.collection.mutable.MutableList[Map[String, List[String]]] = scala.collection.mutable.MutableList[Map[String, List[String]]]()
      var baseElems: Array[BaseElemDef] = new Array[BaseElemDef](cfgmap.keys.size)
      cfgmap.keys.foreach(key => {
	logger.debug("Model Config Key => " + key)
	var mdl = cfgmap(key).asInstanceOf[Map[String, List[String]]]

	// wrap the config objet in Element Def
	var confElem: ConfigDef = new ConfigDef
	confElem.tranId = tranId
	confElem.nameSpace = if(userid != None) userid.get else null
	confElem.contents = JsonSerializer.SerializeMapToJsonString(mdl)
	confElem.name = key
	baseElems(i) = confElem
	i = i + 1

	// Prepare KEY/VALUE for persistent insertion
	var modelKey = userid.getOrElse("_") + "." + key
	var value = confElem.contents.getBytes //serializer.SerializeObjectToByteArray(mdl)
	keyList = keyList :+ modelKey.toLowerCase
	valueList = valueList :+ value
	// Save in memory
	getMetadataAPI.AddConfigObjToCache(tranId, modelKey, mdl, MdMgr.GetMdMgr)
      })
      // Save in Database
      getMetadataAPI.SaveObjectList(keyList, valueList, "model_config_objects", serializerType)
      if (!isFromNotify) {
	val operations = for (op <- baseElems) yield "Add"
	getMetadataAPI.NotifyEngine(baseElems, operations)
      }

      // return reuslts
      val apiResult = new ApiResult(ErrorCodeConstants.Success, "UploadModelsConfig", null, "Upload of model config successful")
      apiResult.toString()
    } catch {
      case e: Exception => {
        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "UploadModelsConfig", null, "Error :" + e.toString() + ErrorCodeConstants.Upload_Config_Failed + ":" + cfgStr)
        apiResult.toString()
      }
    }
  }

    /**
     * getStringFromJsonNode
      *
      * @param v just any old thing
     * @return a string representation
     */
  private def getStringFromJsonNode(v: Any): String = {
    if (v == null) return ""

    if (v.isInstanceOf[String]) return v.asInstanceOf[String]

    implicit val jsonFormats: Formats = DefaultFormats
    val lst = List(v)
    val str = Serialization.write(lst)
    if (str.size > 2) {
      return str.substring(1, str.size - 1)
    }
    return ""
  }

  /*
  private def getJsonNodeFromString(s: String): Any = {
    if (s.size == 0) return s

    val s1 = "[" + s + "]"

    implicit val jsonFormats: Formats = DefaultFormats
    val list = Serialization.read[List[_]](s1)

    return list(0)
  }
*/

    /**
     * Accept a config specification (a JSON str)
      *
      * @param cfgStr the json file to be interpted
     * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
     *               method. If Security and/or Audit are configured, this value must be a value other than None.
     * @param objectList note on the objects in the configuration to be logged to audit adapter
     * @return
     */
  def UploadConfig(cfgStr: String, userid: Option[String], objectList: String): String = {
    var keyList = new Array[String](0)
    var valueList = new Array[Array[Byte]](0)

    //TODO: BOOOYA
    var clusterNotifications: ArrayBuffer[BaseElemDef] = new ArrayBuffer[BaseElemDef]
    var clusterNotifyActions: ArrayBuffer[String] =  new ArrayBuffer[String]

    getMetadataAPI.logAuditRec(userid, Some(AuditConstants.WRITE), AuditConstants.INSERTCONFIG, cfgStr, AuditConstants.SUCCESS, "", objectList)

    try {
      // extract config objects
      val map = JsonSerializer.parseEngineConfig(cfgStr)
      // process clusterInfo object if it exists
      if (map.contains("Clusters") == false) {
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "UploadConfig", null, ErrorCodeConstants.Upload_Config_Failed + ":" + cfgStr)
        apiResult.toString()
      } else {
        if (map.contains("Clusters")) {
          var globalAdaptersCollected = false // to support previous versions
          val clustersList = map.get("Clusters").get.asInstanceOf[List[_]] //BUGBUG:: Do we need to check the type before converting
          logger.debug("Found " + clustersList.length + " cluster objects ")
          clustersList.foreach(clustny => {
            val cluster = clustny.asInstanceOf[Map[String, Any]] //BUGBUG:: Do we need to check the type before converting
            val ClusterId = cluster.getOrElse("ClusterId", "").toString.trim.toLowerCase
            if (ClusterId.length == 0) {
              val apiResult = new ApiResult(ErrorCodeConstants.Failure, "UploadConfig", cfgStr, "Error : ClusterId Must be present to upload Cluster Config " + ErrorCodeConstants.Upload_Config_Failed)
              return apiResult.toString()
            }
            logger.debug("Processing the cluster => " + ClusterId)
            // save in memory
            val ci = MdMgr.GetMdMgr.MakeCluster(ClusterId, "", "")
            val addCluserReuslt = MdMgr.GetMdMgr.AddCluster(ci)

            if (addCluserReuslt != None) {
              var clusterDef: ClusterConfigDef = new ClusterConfigDef
              clusterDef.clusterId = ci.clusterId
              clusterDef.elementType = "clusterDef"
              clusterDef.nameSpace = "cluster"
              clusterDef.name = ci.clusterId
              clusterDef.tranId = getMetadataAPI.GetNewTranId
              clusterNotifications.append(clusterDef)
              if (addCluserReuslt.get.equalsIgnoreCase("add"))
                clusterNotifyActions.append("Add")
              else
                clusterNotifyActions.append("Update")
            }

            var key = "ClusterInfo." + ci.clusterId
            var value = MetadataAPISerialization.serializeObjectToJson(ci).getBytes //serializer.SerializeObjectToByteArray(ci)
            keyList = keyList :+ key.toLowerCase
            valueList = valueList :+ value

            // gather config name-value pairs
            var cfgMap: scala.collection.mutable.HashMap[String,String] = null

            // Upload the latest and see if any are new updates - do it like this to make sure
            // incremental update does not remove existing values
            val currentCic = MdMgr.GetMdMgr.GetClusterCfg(ClusterId.toLowerCase.trim)
            if (currentCic == null) {
              cfgMap = new scala.collection.mutable.HashMap[String, String]
            } else {
              cfgMap = currentCic.CfgMap.map(elem =>{elem._1 -> elem._2})
            }

            if (cluster.contains("SystemCatalog"))
              cfgMap("SystemCatalog") = getStringFromJsonNode(cluster.getOrElse("SystemCatalog", null))
            if (cluster.contains("ZooKeeperInfo"))
              cfgMap("ZooKeeperInfo") = getStringFromJsonNode(cluster.getOrElse("ZooKeeperInfo", null))
            if (cluster.contains("EnvironmentContext"))
              cfgMap("EnvironmentContext") = getStringFromJsonNode(cluster.getOrElse("EnvironmentContext", null))
            if (cluster.contains("Cache"))
              cfgMap("Cache") = getStringFromJsonNode(cluster.getOrElse("Cache", null))
           if (cluster.contains("PYTHON_CONFIG"))
                cfgMap("PYTHON_CONFIG") = getStringFromJsonNode(cluster.get("PYTHON_CONFIG"))
            if (cluster.contains("Config")) {
              val config = cluster.get("Config").get.asInstanceOf[Map[String, Any]] //BUGBUG:: Do we need to check the type before converting
              if (config.contains("SystemCatalog"))
                cfgMap("SystemCatalog") = getStringFromJsonNode(config.get("SystemCatalog"))
              if (config.contains("ZooKeeperInfo"))
                cfgMap("ZooKeeperInfo") = getStringFromJsonNode(config.get("ZooKeeperInfo"))
              if (config.contains("EnvironmentContext"))
                cfgMap("EnvironmentContext") = getStringFromJsonNode(config.get("EnvironmentContext"))
            }

	    if (logger.isDebugEnabled()) {
	       logger.debug( "The value of python config while uploading is  " + cfgMap.getOrElse("PYTHON_CONFIG", "") ) 
	    }

	    getMetadataAPI.GetMetadataAPIConfig.setProperty("PYTHON_CONFIG", cfgMap.getOrElse("PYTHON_CONFIG", "")) 

	    if (logger.isDebugEnabled()) {
	       logger.debug( "The value of python config from meta config while uploading is  " + getMetadataAPI.GetMetadataAPIConfig.getProperty("PYTHON_CONFIG") ) 
	    }
            // save in memory
            val cic = MdMgr.GetMdMgr.MakeClusterCfg(ClusterId, cfgMap, null, null)
            val addClusterResult = MdMgr.GetMdMgr.AddClusterCfg(cic)

            if (addClusterResult != None) {
              var clusterInfoDef: ClusterConfigDef = new ClusterConfigDef
              clusterInfoDef.clusterId = cic.clusterId
              clusterInfoDef.elementType = "clusterInfoDef"
              clusterInfoDef.name = cic.clusterId
              clusterInfoDef.nameSpace = "clusterInfo"
              clusterInfoDef.tranId = getMetadataAPI.GetNewTranId
              clusterNotifications.append(clusterInfoDef)
              logger.debug("cic Change for " + clusterInfoDef.name )
              if (addClusterResult.get.equalsIgnoreCase("add"))
                clusterNotifyActions.append("Add")
              else
                clusterNotifyActions.append("Update")
            }

            key = "ClusterCfgInfo." + cic.clusterId
            value = MetadataAPISerialization.serializeObjectToJson(cic).getBytes//serializer.SerializeObjectToByteArray(cic)
            keyList = keyList :+ key.toLowerCase
            valueList = valueList :+ value


            if (cluster.contains("Nodes")) {
              val nodes = cluster.get("Nodes").get.asInstanceOf[List[_]]
              nodes.foreach(n => {
                val node = n.asInstanceOf[Map[String, Any]]
                val nodeId = node.getOrElse("NodeId", "").toString.trim.toLowerCase
                val nodePort = node.getOrElse("NodePort", "0").toString.trim.toInt
                val nodeIpAddr = node.getOrElse("NodeIpAddr", "").toString.trim
                val scala_home = node.getOrElse("Scala_home", "").toString.trim
                val java_home = node.getOrElse("Java_home", "").toString.trim
                val classpath = node.getOrElse("Classpath", "").toString.trim
                val jarPaths = if (node.contains("JarPaths")) node.get("JarPaths").get.asInstanceOf[List[String]] else List[String]()
                val roles = if (node.contains("Roles")) node.get("Roles").get.asInstanceOf[List[String]] else List[String]()

                val validRoles = NodeRole.ValidRoles.map(r => r.toLowerCase).toSet
                val givenRoles = roles
                var foundRoles = ArrayBuffer[String]()
                var notfoundRoles = ArrayBuffer[String]()
                if (givenRoles != null) {
                  val gvnRoles = givenRoles.foreach(r => {
                    if (validRoles.contains(r.toLowerCase))
                      foundRoles += r
                    else
                      notfoundRoles += r
                  })
                  if (notfoundRoles.size > 0) {
                    logger.error("Found invalid node roles:%s for nodeid: %d".format(notfoundRoles.mkString(","), nodeId))
                  }
                }

                val ni = MdMgr.GetMdMgr.MakeNode(nodeId, nodePort, nodeIpAddr, jarPaths,
                                                 scala_home, java_home, classpath, ClusterId, 0, foundRoles.toArray, "")

                val addNodeResult = MdMgr.GetMdMgr.AddNode(ni)
                if (addNodeResult != None) {
                  var nodeDef: ClusterConfigDef = new ClusterConfigDef
                  nodeDef.name = ni.nodeId
                  nodeDef.tranId = getMetadataAPI.GetNewTranId
                  nodeDef.nameSpace = "nodeIds"
                  nodeDef.clusterId = ci.clusterId
                  nodeDef.elementType = "nodeDef"
                  clusterNotifications.append(nodeDef)
                  logger.debug("node Change for " + nodeDef.name )
                  if (addNodeResult.get.equalsIgnoreCase("add"))
                    clusterNotifyActions.append("Add")
                  else
                    clusterNotifyActions.append("Update")
                }


                val key = "NodeInfo." + ni.nodeId
                val value = MetadataAPISerialization.serializeObjectToJson(ni).getBytes//serializer.SerializeObjectToByteArray(ni)
                keyList = keyList :+ key.toLowerCase
                valueList = valueList :+ value
              })
            }

            if (cluster.contains("Tenants")) {
              val tenants = cluster.get("Tenants").get.asInstanceOf[List[_]]
              tenants.foreach(t => {
                val tenant = t.asInstanceOf[Map[String, Any]]
                val tenantId = tenant.getOrElse("TenantId", "").toString.trim
                val description = tenant.getOrElse("Description", "").toString.trim
                var primaryDataStore = getStringFromJsonNode(tenant.getOrElse("PrimaryDataStore", null))
                var cacheConfig = getStringFromJsonNode(tenant.getOrElse("CacheConfig", null))

                val ti = MdMgr.GetMdMgr.MakeTenantInfo(tenantId, description, primaryDataStore, cacheConfig)
                val addTenantResult = MdMgr.GetMdMgr.AddTenantInfo(ti)
                if (addTenantResult != None) {
                  val tenantDef: ClusterConfigDef = new ClusterConfigDef
                  tenantDef.name = ti.tenantId.trim.toLowerCase()
                  tenantDef.tranId = getMetadataAPI.GetNewTranId
                  tenantDef.nameSpace = "Tenants"
                  tenantDef.clusterId = ci.clusterId
                  tenantDef.elementType = "TenantDef"
                  clusterNotifications.append(tenantDef)
                  logger.debug("tenant Change for " + tenantDef.name )
                  if (addTenantResult.get.equalsIgnoreCase("add"))
                    clusterNotifyActions.append("Add")
                  else
                    clusterNotifyActions.append("Update")
                }

                val key = "TenantInfo." + ti.tenantId.trim.toLowerCase()
                val value = MetadataAPISerialization.serializeObjectToJson(ti).getBytes//serializer.SerializeObjectToByteArray(ti)
                keyList = keyList :+ key.toLowerCase
                valueList = valueList :+ value

              })
            }

            if (cluster.contains("Adapters") || (globalAdaptersCollected == false && map.contains("Adapters"))) {
              val adapters = if (cluster.contains("Adapters") && (globalAdaptersCollected == false && map.contains("Adapters"))) {
                map.get("Adapters").get.asInstanceOf[List[_]] ++ cluster.get("Adapters").get.asInstanceOf[List[_]]
              } else if (cluster.contains("Adapters")) {
                cluster.get("Adapters").get.asInstanceOf[List[_]]
              } else if (globalAdaptersCollected == false && map.contains("Adapters")) {
                map.get("Adapters").get.asInstanceOf[List[_]]
              } else {
                List[Any]()
              }

              globalAdaptersCollected = true // to support previous versions

              adapters.foreach(a => {
                val adap = a.asInstanceOf[Map[String, Any]]
                val nm = adap.getOrElse("Name", "").toString.trim
                val typStr = adap.getOrElse("TypeString", "").toString.trim
                val tenantId = adap.getOrElse("TenantId", "").toString.trim
                val clsNm = adap.getOrElse("ClassName", "").toString.trim
                val jarnm = adap.getOrElse("JarName", "").toString.trim

                if (nm.trim.length == 0) {
                  val apiResult = new ApiResult(ErrorCodeConstants.Failure, "UploadConfig", cfgStr, "Error : Name must be set in the adapter for cluster " + ClusterId + ", " + ErrorCodeConstants.Upload_Config_Failed)
                  return apiResult.toString()
                }
                if (typStr.trim.length == 0) {
                  val apiResult = new ApiResult(ErrorCodeConstants.Failure, "UploadConfig", cfgStr, "Error : Type String must be set in the adapter for cluster " + ClusterId + ", " + ErrorCodeConstants.Upload_Config_Failed)
                  return apiResult.toString()
                }
                if (tenantId.trim.length == 0) {
                  val apiResult = new ApiResult(ErrorCodeConstants.Failure, "UploadConfig", cfgStr, "Error : Tenant ID must be set in the adapter for cluster " + ClusterId + ", " + ErrorCodeConstants.Upload_Config_Failed)
                  return apiResult.toString()
                }

                var depJars: List[String] = null
                if (adap.contains("DependencyJars")) {
                  depJars = adap.get("DependencyJars").get.asInstanceOf[List[String]]
                }
                var ascfg: String = ""
                if (adap.contains("AdapterSpecificCfg")) {
                  ascfg = getStringFromJsonNode(adap.get("AdapterSpecificCfg"))
                }
                val fullAdapterConfig = getStringFromJsonNode(adap) // Saving the full config here in case if we want to use it later. In case of storage we use it

                // save in memory
                val ai = MdMgr.GetMdMgr.MakeAdapter(nm, typStr, clsNm, jarnm, depJars, ascfg, tenantId, fullAdapterConfig)
                val addAdapterResult = MdMgr.GetMdMgr.AddAdapter(ai)
                if (addAdapterResult != None) {
                  var adapterDef: ClusterConfigDef = new ClusterConfigDef
                  adapterDef.name = nm
                  adapterDef.nameSpace = typStr
                  adapterDef.tranId = getMetadataAPI.GetNewTranId
                  adapterDef.clusterId = ClusterId
                  adapterDef.elementType = "adapterDef"
                  clusterNotifications.append(adapterDef)
                  logger.debug("adapter Change for " + adapterDef.name )
                  if (addAdapterResult.get.equalsIgnoreCase("add"))
                    clusterNotifyActions.append("Add")
                  else
                    clusterNotifyActions.append("Update")
                }
                val key = "AdapterInfo." + ai.name
                val value = MetadataAPISerialization.serializeObjectToJson(ai).getBytes//serializer.SerializeObjectToByteArray(ai)
                keyList = keyList :+ key.toLowerCase
                valueList = valueList :+ value
              })
            } else {
              logger.debug("Found no adapater objects in the config file")
            }

            // Now see if there are any other User Defined Properties in this cluster, if there are any, create a container
            // like we did for adapters and noteds, etc....
            var userDefinedProps: Map[String, Any] = cluster.filter(x => { !excludeList.contains(x._1) })
            if (userDefinedProps.size > 0) {
              val upProps: UserPropertiesInfo = MdMgr.GetMdMgr.MakeUPProps(ClusterId)
              userDefinedProps.keys.foreach(key => {
                upProps.Props(key) = userDefinedProps(key).toString
              })

              val upAddResults = MdMgr.GetMdMgr.AddUserProperty(upProps)
              if (upAddResults != None) {
                var upDef: ClusterConfigDef = new ClusterConfigDef
                upDef.name = upProps.clusterId
                upDef.nameSpace = "userProperties"
                upDef.tranId = getMetadataAPI.GetNewTranId
                upDef.clusterId = ClusterId
                upDef.elementType = "upDef"
                clusterNotifications.append(upDef)

                logger.debug("UP Change for " + upDef.clusterId )
                if (upAddResults.get.equalsIgnoreCase("add"))
                  clusterNotifyActions.append("Add")
                else
                  clusterNotifyActions.append("Update")
              }

              val upKey = "userProperties." + upProps.clusterId
              val upValue = MetadataAPISerialization.serializeObjectToJson(upProps).getBytes//serializer.SerializeObjectToByteArray(upProps)
              keyList = keyList :+ upKey.toLowerCase
              valueList = valueList :+ upValue
            }
          })

        } else {
          logger.debug("Found no adapater objects in the config file")
        }


        getMetadataAPI.SaveObjectList(keyList, valueList, "config_objects", serializerType)
        getMetadataAPI.NotifyEngine(clusterNotifications.toArray, clusterNotifyActions.toArray)
        var apiResult = new ApiResult(ErrorCodeConstants.Success, "UploadConfig", cfgStr, ErrorCodeConstants.Upload_Config_Successful)
        apiResult.toString()
      }
    } catch {
      case e: Exception => {
        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "UploadConfig", cfgStr, "Error :" + e.toString() + ErrorCodeConstants.Upload_Config_Failed)
        apiResult.toString()
      }
    }
  }

    /**
     * Get a property value
      *
      * @param ci
     * @param key
     * @return
     */
  def getUP(ci: String, key: String): String = {
    MdMgr.GetMdMgr.GetUserProperty(ci, key)
  }

    /**
     * Answer nodes as an array.
      *
      * @return
     */
  def getNodeList1: Array[NodeInfo] = { MdMgr.GetMdMgr.Nodes.values.toArray }
  // All available nodes(format JSON) as a String
    /**
     * Get the nodes as json.
      *
      * @param formatType format of the return value, either JSON or XML
     * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
     *               method. The default is None, but if Security and/or Audit are configured, this value is of little practical use.
     *               Supply one.
     * @return
     */
  def GetAllNodes(formatType: String, userid: Option[String] = None): String = {
    try {
      val nodes = MdMgr.GetMdMgr.Nodes.values.toArray
      getMetadataAPI.logAuditRec(userid, Some(AuditConstants.READ), AuditConstants.GETCONFIG, AuditConstants.CONFIG, AuditConstants.SUCCESS, "", "nodes")
      if (nodes.length == 0) {
        logger.debug("No Nodes found ")
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetAllNodes", null, ErrorCodeConstants.Get_All_Nodes_Failed_Not_Available)
        apiResult.toString()
      } else {
        val apiResult = new ApiResult(ErrorCodeConstants.Success, "GetAllNodes", JsonSerializer.SerializeCfgObjectListToJson("Nodes", nodes), ErrorCodeConstants.Get_All_Nodes_Successful)
        apiResult.toString()
      }
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetAllNodes", null, "Error :" + e.toString() + ErrorCodeConstants.Get_All_Nodes_Failed)
        apiResult.toString()
      }
    }
  }

    /**
     * All available adapters(format JSON) as a String
      *
      * @param formatType format of the return value, either JSON or XML
     * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
     *               method. The default is None, but if Security and/or Audit are configured, this value is of little practical use.
     *               Supply one.
     * @return
     */
  def GetAllAdapters(formatType: String, userid: Option[String] = None): String = {
    try {
      val adapters = MdMgr.GetMdMgr.Adapters.values.toArray
      getMetadataAPI.logAuditRec(userid, Some(AuditConstants.READ), AuditConstants.GETCONFIG, AuditConstants.CONFIG, AuditConstants.SUCCESS, "", "adapters")
      if (adapters.length == 0) {
        logger.debug("No Adapters found ")
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetAllAdapters", null, ErrorCodeConstants.Get_All_Adapters_Failed_Not_Available)
        apiResult.toString()
      } else {
        val apiResult = new ApiResult(ErrorCodeConstants.Success, "GetAllAdapters", JsonSerializer.SerializeCfgObjectListToJson("Adapters", adapters), ErrorCodeConstants.Get_All_Adapters_Successful)
        apiResult.toString()
      }
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetAllAdapters", null, "Error :" + e.toString() + ErrorCodeConstants.Get_All_Adapters_Failed)

        apiResult.toString()
      }
    }
  }


  def GetAllTenants(userid: Option[String] = None): Array[String] = {
    try{
      getMetadataAPI.logAuditRec(userid, Some(AuditConstants.READ), AuditConstants.GETCONFIG, AuditConstants.CONFIG, AuditConstants.SUCCESS, "", "tenants")
      var tenants = MdMgr.GetMdMgr.GetAllTenantInfos
      var tenantIds: ArrayBuffer[String] = ArrayBuffer[String]()
      tenants.foreach(x=> tenantIds.append(x.tenantId))
      return tenantIds.toArray[String]
    } catch {
      case e: Exception => {
        logger.error("Unable to retreive tenant ids")
        return new Array[String](0)
      }
    }
  }

    /**
     * All available clusters(format JSON) as a String
      *
      * @param formatType format of the return value, either JSON or XML
     * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
     *               method. The default is None, but if Security and/or Audit are configured, this value is of little practical use.
     *               Supply one.
     * @return
     */
  def GetAllClusters(formatType: String, userid: Option[String] = None): String = {
    try {
      val clusters = MdMgr.GetMdMgr.Clusters.values.toArray
      getMetadataAPI.logAuditRec(userid, Some(AuditConstants.READ), AuditConstants.GETCONFIG, AuditConstants.CONFIG, AuditConstants.SUCCESS, "", "Clusters")
      if (clusters.length == 0) {
        logger.debug("No Clusters found ")
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetAllClusters", null, ErrorCodeConstants.Get_All_Clusters_Failed_Not_Available)
        apiResult.toString()
      } else {
        val apiResult = new ApiResult(ErrorCodeConstants.Success, "GetAllClusters", JsonSerializer.SerializeCfgObjectListToJson("Clusters", clusters), ErrorCodeConstants.Get_All_Clusters_Successful)
        apiResult.toString()
      }
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetAllClusters", null, "Error :" + e.toString() + ErrorCodeConstants.Get_All_Clusters_Failed)
        apiResult.toString()
      }
    }
  }

  // All available clusterCfgs(format JSON) as a String
    /**
     *
     * @param formatType format of the return value, either JSON or XML
     * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
     *               method. The default is None, but if Security and/or Audit are configured, this value is of little practical use.
     *               Supply one.
     * @return
     */
  def GetAllClusterCfgs(formatType: String, userid: Option[String] = None): String = {
    try {
      getMetadataAPI.logAuditRec(userid, Some(AuditConstants.READ), AuditConstants.GETCONFIG, AuditConstants.CONFIG, AuditConstants.SUCCESS, "", "ClusterCfg")
      val clusterCfgs = MdMgr.GetMdMgr.ClusterCfgs.values.toArray
      if (clusterCfgs.length == 0) {
        logger.debug("No ClusterCfgs found ")
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetAllClusterCfgs", null, ErrorCodeConstants.Get_All_Cluster_Configs_Failed_Not_Available)
        apiResult.toString()
      } else {
        val apiResult = new ApiResult(ErrorCodeConstants.Success, "GetAllClusterCfgs", JsonSerializer.SerializeCfgObjectListToJson("ClusterCfgs", clusterCfgs), ErrorCodeConstants.Get_All_Cluster_Configs_Successful)

        apiResult.toString()
      }
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetAllClusterCfgs", null, "Error :" + e.toString() + ErrorCodeConstants.Get_All_Cluster_Configs_Failed)

        apiResult.toString()
      }
    }
  }

    /**
     * All available config objects(format JSON) as a String
      *
      * @param formatType format of the return value, either JSON or XML
     * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
     *               method. The default is None, but if Security and/or Audit are configured, this value is of little practical use.
     *               Supply one.
     * @return
     */
  def GetAllCfgObjects(formatType: String, userid: Option[String] = None): String = {
    var cfgObjList = new Array[Object](0)
    getMetadataAPI.logAuditRec(userid, Some(AuditConstants.READ), AuditConstants.GETCONFIG, AuditConstants.CONFIG, AuditConstants.SUCCESS, "", "all")
    var jsonStr: String = ""
    var jsonStr1: String = ""
    try {
      val clusters = MdMgr.GetMdMgr.Clusters.values.toArray
      if (clusters.length != 0) {
        cfgObjList = cfgObjList :+ clusters
        jsonStr1 = JsonSerializer.SerializeCfgObjectListToJson("Clusters", clusters)
        jsonStr1 = jsonStr1.substring(1)
        jsonStr1 = JsonSerializer.replaceLast(jsonStr1, "}", ",")
        jsonStr = jsonStr + jsonStr1
      }
      val clusterCfgs = MdMgr.GetMdMgr.ClusterCfgs.values.toArray
      if (clusterCfgs.length != 0) {
        cfgObjList = cfgObjList :+ clusterCfgs
        jsonStr1 = JsonSerializer.SerializeCfgObjectListToJson("ClusterCfgs", clusterCfgs)
        jsonStr1 = jsonStr1.substring(1)
        jsonStr1 = JsonSerializer.replaceLast(jsonStr1, "}", ",")
        jsonStr = jsonStr + jsonStr1
      }
      val tenants = MdMgr.GetMdMgr.GetAllTenantInfos
      if (tenants.length != 0) {
        cfgObjList = cfgObjList :+ tenants
        jsonStr1 = JsonSerializer.SerializeCfgObjectListToJson("Tenants", tenants)
        jsonStr1 = jsonStr1.substring(1)
        jsonStr1 = JsonSerializer.replaceLast(jsonStr1, "}", ",")
        jsonStr = jsonStr + jsonStr1
      }
      val nodes = MdMgr.GetMdMgr.Nodes.values.toArray
      if (nodes.length != 0) {
        cfgObjList = cfgObjList :+ nodes
        jsonStr1 = JsonSerializer.SerializeCfgObjectListToJson("Nodes", nodes)
        jsonStr1 = jsonStr1.substring(1)
        jsonStr1 = JsonSerializer.replaceLast(jsonStr1, "}", ",")
        jsonStr = jsonStr + jsonStr1
      }
      val adapters = MdMgr.GetMdMgr.Adapters.values.toArray
      if (adapters.length != 0) {
        cfgObjList = cfgObjList :+ adapters
        jsonStr1 = JsonSerializer.SerializeCfgObjectListToJson("Adapters", adapters)
        jsonStr1 = jsonStr1.substring(1)
        jsonStr1 = JsonSerializer.replaceLast(jsonStr1, "}", ",")
        jsonStr = jsonStr + jsonStr1
      }

      jsonStr = "{" + JsonSerializer.replaceLast(jsonStr, ",", "") + "}"

      if (cfgObjList.length == 0) {
        logger.debug("No Config Objects found ")
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetAllCfgObjects", null, ErrorCodeConstants.Get_All_Configs_Failed_Not_Available)
        apiResult.toString()
      } else {
        val apiResult = new ApiResult(ErrorCodeConstants.Success, "GetAllCfgObjects", jsonStr, ErrorCodeConstants.Get_All_Configs_Successful)
        apiResult.toString()
      }
    } catch {
      case e: Exception => {

        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetAllCfgObjects", null, "Error :" + e.toString() + ErrorCodeConstants.Get_All_Configs_Failed)
        apiResult.toString()
      }
    }
  }

    /**
     * Dump the configuration file to the log
     */
  def  dumpMetadataAPIConfig {
    val e = getMetadataAPI.GetMetadataAPIConfig.propertyNames()
    while (e.hasMoreElements()) {
      val key = e.nextElement().asInstanceOf[String]
      val value = getMetadataAPI.GetMetadataAPIConfig.getProperty(key)
      logger.debug("Key : " + key + ", Value : " + value)
      System.out.println(key + " => " + value)
    }
  }

    /**
     * setPropertyFromConfigFile - convert a specific KEY:VALUE pair in the config file into the
     * KEY:VALUE pair in the  Properties object
      *
      * @param key a property key
     * @param value a value
     */
  private def setPropertyFromConfigFile(key: String, value: String) {
    var finalKey = key
    var finalValue = value

    // JAR_PATHs need to be trimmed
    if (key.equalsIgnoreCase("JarPaths") || key.equalsIgnoreCase("JAR_PATHS")) {
      val jp = value
      val j_paths = jp.split(",").map(s => s.trim).filter(s => s.size > 0)
      finalValue = j_paths.mkString(",")
      finalKey = "JAR_PATHS"
    }

    // Special case 1. for config.  if JAR_PATHS is never set, then it should default to JAR_TARGET_DIR..
    // so we set the JAR_PATH if it was never set.. no worries, if JAR_PATH comes later, it willsimply
    // overwrite the value.
    if (key.equalsIgnoreCase("JAR_TARGET_DIR") && (getMetadataAPI.GetMetadataAPIConfig.getProperty("JAR_PATHS") == null)) {
      getMetadataAPI.GetMetadataAPIConfig.setProperty("JAR_PATHS", finalValue)
      logger.debug("JAR_PATHS = " + finalValue)
      pList = pList - "JAR_PATHS"
    }

    // Special case 2.. MetadataLocation must set 2 properties in the config object.. 1. prop set by DATABASE_HOST,
    // 2. prop set by DATABASE_LOCATION.  MetadataLocation will overwrite those values, but not the other way around.
    if (key.equalsIgnoreCase("MetadataLocation")) {
      getMetadataAPI.GetMetadataAPIConfig.setProperty("DATABASE_LOCATION", finalValue)
      getMetadataAPI.GetMetadataAPIConfig.setProperty("DATABASE_HOST", finalValue)
      logger.debug("DATABASE_LOCATION  = " + finalValue)
      pList = pList - "DATABASE_LOCATION"
      logger.debug("DATABASE_HOST  = " + finalValue)
      pList = pList - "DATABASE_HOST"
      return
    }

    // SSL_PASSWORD will not be saved in the Config object, since that object is printed out for debugging purposes.
    if (key.equalsIgnoreCase("SSL_PASSWD")) {
      getMetadataAPI.setSSLCertificatePasswd(value)
      return
    }

    // Special case 2a.. DATABASE_HOST should not override METADATA_LOCATION
    if (key.equalsIgnoreCase("DATABASE_HOST") && (getMetadataAPI.GetMetadataAPIConfig.getProperty(key.toUpperCase) != null)) {
      return
    }
    // Special case 2b.. DATABASE_LOCATION should not override METADATA_LOCATION
    if (key.equalsIgnoreCase("DATABASE_LOCATION") && (getMetadataAPI.GetMetadataAPIConfig.getProperty(key.toUpperCase) != null)) {
      return
    }

    // Special case 3: SCHEMA_NAME can come it under several keys, but we store it as DATABASE SCHEMA
    if (key.equalsIgnoreCase("MetadataSchemaName")) {
      finalKey = "DATABASE_SCHEMA"
    }

    if (key.equalsIgnoreCase("MetadataAdapterSpecificConfig")) {
      finalKey = "ADAPTER_SPECIFIC_CONFIG"
    }

    // Special case 4: DATABASE can come under DATABASE or MetaDataStoreType
    if (key.equalsIgnoreCase("DATABASE") || key.equalsIgnoreCase("MetadataStoreType")) {
      finalKey = "DATABASE"
    }

    // Special case 5: NodeId or Node_ID is possible
    if (key.equalsIgnoreCase("NODE_ID") || key.equalsIgnoreCase("NODEID")) {
      finalKey = "NODE_ID"
    }

    if (key.equalsIgnoreCase("MetadataDataStore")) {
      finalKey = "METADATA_DATASTORE"
    }

    // Store the Key/Value pair
    getMetadataAPI.GetMetadataAPIConfig.setProperty(finalKey.toUpperCase, finalValue)
    logger.debug(finalKey.toUpperCase + " = " + finalValue)
    pList = pList - finalKey.toUpperCase
  }

    /**
     * Refresh the ClusterConfiguration for the specified node
      *
      * @param nodeId a cluster node
     * @return
     */
  def RefreshApiConfigForGivenNode(nodeId: String): Boolean = {

    val nd = mdMgr.Nodes.getOrElse(nodeId, null)
    if (nd == null) {
      logger.error("In Refresh API Config - Node %s not found in metadata".format(nodeId))
      return false
    }

    val clusterId = nd.ClusterId

    val cluster = mdMgr.ClusterCfgs.getOrElse(nd.ClusterId, null)
    if (cluster == null) {
      logger.error("Cluster not found for Node %s  & ClusterId : %s".format(nodeId, nd.ClusterId))
      return false
    }

    logger.debug("Configurations for the clusterId:" + clusterId)
    cluster.cfgMap.foreach(kv => {
      logger.debug("Key: %s, Value: %s".format(kv._1, kv._2))
    })

    val zooKeeperInfo = cluster.cfgMap.getOrElse("ZooKeeperInfo", null)
    if (zooKeeperInfo == null) {
      logger.error("ZooKeeperInfo not found for Node %s  & ClusterId : %s".format(nodeId, nd.ClusterId))
      return false
    }

    val pythonConfigs = cluster.cfgMap.getOrElse("PYTHON_CONFIG", null)
    
    if (pythonConfigs != null && pythonConfigs.isInstanceOf[String]
      && pythonConfigs.asInstanceOf[String].trim().size > 0) {
      if (logger.isDebugEnabled()) {
      	 logger.debug(" The value of Pythonconfigs are " + pythonConfigs) ;
      }
      getMetadataAPI.GetMetadataAPIConfig.setProperty("PYTHON_CONFIG", pythonConfigs) 

      implicit val jsonFormats: Formats = DefaultFormats

      val pyInfo = parse(pythonConfigs).extract[PythonInfo]

      val pythonPath  = pyInfo.PYTHON_PATH.replace("\"", "").trim
      getMetadataAPI.GetMetadataAPIConfig.setProperty("PYTHON_PATH", pythonPath)
      val pythonBinDir  = pyInfo.PYTHON_BIN_DIR.replace("\"", "").trim
      getMetadataAPI.GetMetadataAPIConfig.setProperty("PYTHON_BIN_DIR", pythonBinDir)
      val pythonLogConfigPath  = pyInfo.PYTHON_LOG_CONFIG_PATH.replace("\"", "").trim
      getMetadataAPI.GetMetadataAPIConfig.setProperty("PYTHON_LOG_CONFIG_PATH", pythonLogConfigPath)
      val pythonLogPath  = pyInfo.PYTHON_LOG_PATH.replace("\"", "").trim
      getMetadataAPI.GetMetadataAPIConfig.setProperty("PYTHON_LOG_PATH", pythonLogPath)
      val serverBasePort  = pyInfo.SERVER_BASE_PORT.replace("\"", "").trim
      getMetadataAPI.GetMetadataAPIConfig.setProperty("SERVER_BASE_PORT", serverBasePort)
      val serverPortLimit  = pyInfo.SERVER_PORT_LIMIT.replace("\"", "").trim
      getMetadataAPI.GetMetadataAPIConfig.setProperty("SERVER_PORT_LIMIT", serverPortLimit)
      val serverHost  = pyInfo.SERVER_HOST.replace("\"", "").trim
      getMetadataAPI.GetMetadataAPIConfig.setProperty("SERVER_HOST", serverHost)
      if (logger.isDebugEnabled()) {
         logger.debug("PYTHON_PATH(based on PYTHON_CONFIG) => " + pythonPath)
         logger.debug("PYTHON_BIN_DIR(based on PYTHON_CONFIG) => " + pythonBinDir)
         logger.debug("PYTHON_LOG_CONFIG_PATH(based on PYTHON_CONFIG) => " + pythonLogConfigPath)
         logger.debug("PYTHON_LOG_PATH(based on PYTHON_CONFIG) => " + pythonLogPath)
         logger.debug("SERVER_BASE_PORT(based on PYTHON_CONFIG) => " + serverBasePort)
         logger.debug("SERVER_PORT_LIMIT(based on PYTHON_CONFIG) => " + serverPortLimit)
          logger.debug("SERVER_HOST(based on PYTHON_CONFIG) => " + serverHost)
      }

    }


    val jarPaths = if (nd.JarPaths == null) Set[String]() else nd.JarPaths.map(str => str.replace("\"", "").trim).filter(str => str.size > 0).toSet
      implicit val jsonFormats: Formats = DefaultFormats
    if (jarPaths.size == 0) {
      logger.error("Not found valid JarPaths.")
      return false
    } else {
      getMetadataAPI.GetMetadataAPIConfig.setProperty("JAR_PATHS", jarPaths.mkString(","))
      logger.debug("JarPaths Based on node(%s) => %s".format(nodeId, jarPaths.mkString(",")))
      val jarDir = compact(render(jarPaths.head)).replace("\"", "").trim

      // If JAR_TARGET_DIR is unset.. set it ot the first value of the the JAR_PATH.. whatever it is... ????? I think we should error on start up.. this seems like wrong
      // user behaviour not to set a variable vital to MODEL compilation.
      if (getMetadataAPI.GetMetadataAPIConfig.getProperty("JAR_TARGET_DIR") == null || (getMetadataAPI.GetMetadataAPIConfig.getProperty("JAR_TARGET_DIR") != null && getMetadataAPI.GetMetadataAPIConfig.getProperty("JAR_TARGET_DIR").length == 0))
        getMetadataAPI.GetMetadataAPIConfig.setProperty("JAR_TARGET_DIR", jarDir)
      logger.debug("Jar_target_dir Based on node(%s) => %s".format(nodeId, jarDir))
    }

    val zKInfo = parse(zooKeeperInfo).extract[JZKInfo]

    val zkConnectString = zKInfo.ZooKeeperConnectString.replace("\"", "").trim
    getMetadataAPI.GetMetadataAPIConfig.setProperty("ZOOKEEPER_CONNECT_STRING", zkConnectString)
    logger.debug("ZOOKEEPER_CONNECT_STRING(based on nodeId) => " + zkConnectString)

    val zkNodeBasePath = zKInfo.ZooKeeperNodeBasePath.replace("\"", "").trim
    getMetadataAPI.GetMetadataAPIConfig.setProperty("ZNODE_PATH", zkNodeBasePath)
    logger.debug("ZNODE_PATH(based on nodeid) => " + zkNodeBasePath)

    val zkSessionTimeoutMs1 = if (zKInfo.ZooKeeperSessionTimeoutMs == None || zKInfo.ZooKeeperSessionTimeoutMs == null) 0 else zKInfo.ZooKeeperSessionTimeoutMs.get.toString.toInt
    // Taking minimum values in case if needed
    val zkSessionTimeoutMs = if (zkSessionTimeoutMs1 <= 0) 1000 else zkSessionTimeoutMs1
    getMetadataAPI.GetMetadataAPIConfig.setProperty("ZK_SESSION_TIMEOUT_MS", zkSessionTimeoutMs.toString)
    logger.debug("ZK_SESSION_TIMEOUT_MS(based on nodeId) => " + zkSessionTimeoutMs)

    val zkConnectionTimeoutMs1 = if (zKInfo.ZooKeeperConnectionTimeoutMs == None || zKInfo.ZooKeeperConnectionTimeoutMs == null) 0 else zKInfo.ZooKeeperConnectionTimeoutMs.get.toString.toInt
    // Taking minimum values in case if needed
    val zkConnectionTimeoutMs = if (zkConnectionTimeoutMs1 <= 0) 30000 else zkConnectionTimeoutMs1
    getMetadataAPI.GetMetadataAPIConfig.setProperty("ZK_CONNECTION_TIMEOUT_MS", zkConnectionTimeoutMs.toString)
    logger.debug("ZK_CONNECTION_TIMEOUT_MS(based on nodeId) => " + zkConnectionTimeoutMs)
    true
  }

  private def fileExists(fileName: String): Boolean = {
    val iFile = new File(fileName)
    if (!iFile.exists) {
      logger.error("The File  (" + fileName + ") is not found: ")
      false
    } else
      true
  }

    /**
     * Read metadata api configuration properties
      *
      * @param configFile the MetadataAPI configuration file
     */
  @throws(classOf[MissingPropertyException])
  @throws(classOf[InvalidPropertyException])
  def readMetadataAPIConfigFromPropertiesFile(configFile: String): Unit = {
    try {
      if (MetadataAPIImpl.propertiesAlreadyLoaded) {
        logger.debug("Configuratin properties already loaded, skipping the load configuration step")
        return ;
      }

      val (prop, failStr) = com.ligadata.Utils.Utils.loadConfiguration(configFile.toString, true)
      if (failStr != null && failStr.size > 0) {
        logger.error(failStr)
        return
      }
      if (prop == null) {
        logger.error("Failed to load configuration")
        return
      }

      // some zookeper vals can be safely defaulted to.
      setPropertyFromConfigFile("NODE_ID", "Undefined")
      setPropertyFromConfigFile("API_LEADER_SELECTION_ZK_NODE", "/ligadata")
      setPropertyFromConfigFile("ZK_SESSION_TIMEOUT_MS", "3000")
      setPropertyFromConfigFile("ZK_CONNECTION_TIMEOUT_MS", "3000")


      // Loop through and set the rest of the values.
      val eProps1 = prop.propertyNames()
      while (eProps1.hasMoreElements()) {
        val key = eProps1.nextElement().asInstanceOf[String]
        val value = prop.getProperty(key)
        setPropertyFromConfigFile(key, value)
      }

      val nodeId = getMetadataAPI.GetMetadataAPIConfig.getProperty("NODE_ID")
      if( nodeId == null ){
	throw new Exception("NodeId must be defined in the config file " + configFile)
      }
      setPropertyFromConfigFile("NODE_ID", nodeId)

      val mdDataStore = getMetadataAPI.GetMetadataAPIConfig.getProperty("METADATA_DATASTORE")
      if( mdDataStore == null ){
	throw new Exception("MetadataDataStore must be defined in the config file " + configFile)
      }
      setPropertyFromConfigFile("METADATA_DATASTORE", mdDataStore)

      var notifyEngine = getMetadataAPI.GetMetadataAPIConfig.getProperty("NOTIFY_ENGINE")
      if( notifyEngine == null ){
	notifyEngine = "YES"
        setPropertyFromConfigFile("NOTIFY_ENGINE", notifyEngine)
      }

      var znodePath = getMetadataAPI.GetMetadataAPIConfig.getProperty("ZNODE_PATH")
      if( znodePath == null ){
	znodePath = "/kamanja"
        setPropertyFromConfigFile("ZNODE_PATH", znodePath)
      }

      var apiLeaderSelectionZkNode = getMetadataAPI.GetMetadataAPIConfig.getProperty("API_LEADER_SELECTION_NODE_PATH")
      if( apiLeaderSelectionZkNode == null ){
	apiLeaderSelectionZkNode = "/kamanja"
        setPropertyFromConfigFile("API_LEADER_SELECTION_NODE_PATH", apiLeaderSelectionZkNode)
      }

      var zkConnectString = getMetadataAPI.GetMetadataAPIConfig.getProperty("ZOOKEEPER_CONNECT_STRING")
      if( zkConnectString == null ){
	zkConnectString = "localhost:2181"
        setPropertyFromConfigFile("ZOOKEEPER_CONNECT_STRING", zkConnectString)
      }

      var serviceHost = getMetadataAPI.GetMetadataAPIConfig.getProperty("SERVICE_HOST")
      if( serviceHost == null ){
	serviceHost = "localhost"
        setPropertyFromConfigFile("SERVICE_HOST", serviceHost)
      }

      var servicePort = getMetadataAPI.GetMetadataAPIConfig.getProperty("SERVICE_PORT")
      if( servicePort == null ){
	servicePort = "8081"
        setPropertyFromConfigFile("SERVICE_PORT", servicePort)
      }

      var modelExecFlag = getMetadataAPI.GetMetadataAPIConfig.getProperty("MODEL_EXEC_FLAG")
      if( modelExecFlag == null ){
	modelExecFlag = "false"
        setPropertyFromConfigFile("MODEL_EXEC_FLAG", modelExecFlag)
      }

      var securityImplClass = getMetadataAPI.GetMetadataAPIConfig.getProperty("SECURITY_IMPL_CLASS")
      if( securityImplClass == null ){
	securityImplClass = "com.ligadata.Security.SimpleApacheShiroAdapter"
        setPropertyFromConfigFile("SECURITY_IMPL_CLASS", securityImplClass)
      }

      var doAuth = getMetadataAPI.GetMetadataAPIConfig.getProperty("DO_AUTH")
      if( doAuth == null ){
	doAuth = "NO"
        setPropertyFromConfigFile("DO_AUTH", doAuth)
      }

      var auditImplClass = getMetadataAPI.GetMetadataAPIConfig.getProperty("AUDIT_IMPL_CLASS")
      if( auditImplClass == null ){
	auditImplClass = "com.ligadata.audit.adapters.AuditCassandraAdapter"
        setPropertyFromConfigFile("AUDIT_IMPL_CLASS", auditImplClass)
      }

      var doAudit = getMetadataAPI.GetMetadataAPIConfig.getProperty("DO_AUDIT")
      if( doAudit == null ){
	doAudit = "NO"
        setPropertyFromConfigFile("DO_AUDIT", doAudit)
      }

      var rootDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("ROOT_DIR")
      if( rootDir == null ){
	logger.warn("The property ROOT_DIR is not defined in MetadataAPI properties file")
	logger.warn("This property is required if metadata api operation is being performed")
	logger.warn("It is not required for engine startup")
      }

      var scalaHome = getMetadataAPI.GetMetadataAPIConfig.getProperty("SCALA_HOME")
      if( scalaHome == null ){
	try{
	  scalaHome = Process("which scala").!!
	} catch {
	  case e: Exception =>
	    logger.error("Failed to locate SCALA_HOME")
	    throw e
	}
	scalaHome = scalaHome.replaceAll("/bin/scala\n","")
	setPropertyFromConfigFile("SCALA_HOME", scalaHome)
      }

      var javaHome = getMetadataAPI.GetMetadataAPIConfig.getProperty("JAVA_HOME")
      if( javaHome == null ){
	try{
	  javaHome = Process("which java").!!
	} catch {
	  case e: Exception =>
	    logger.error("Failed to locate JAVA_HOME")
	    throw e
	}
	javaHome = javaHome.replaceAll("/bin/java\n","")
	setPropertyFromConfigFile("JAVA_HOME", javaHome)
      }


      if( rootDir != null ){
	val libSystemPath = rootDir + "/lib/system"
	val libApplicationPath = rootDir + "/lib/application"

	logger.debug("libSystemPath => " + libSystemPath)
	logger.debug("libApplicationPath => " + libApplicationPath)

	val defaultJarPathStr = libSystemPath + "," + libApplicationPath
	setPropertyFromConfigFile("JarPaths", defaultJarPathStr)

	var manifestPath = getMetadataAPI.GetMetadataAPIConfig.getProperty("MANIFEST_PATH")
	if( manifestPath == null ){
	  manifestPath = rootDir + "/config/manifest.mf"
          setPropertyFromConfigFile("MANIFEST_PATH", manifestPath)
	}

	var compilerWorkDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("COMPILER_WORK_DIR")
	if( compilerWorkDir == null ){
	  compilerWorkDir = rootDir + "/workingdir"
          setPropertyFromConfigFile("COMPILER_WORK_DIR", compilerWorkDir)
	}

	var modelFilesDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("MODEL_FILES_DIR")
	if( modelFilesDir == null ){
	  modelFilesDir = rootDir + "/input/SampleApplications/metadata/model/"
          setPropertyFromConfigFile("MODEL_FILES_DIR", modelFilesDir)
	}

	var typeFilesDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("TYPE_FILES_DIR")
	if( typeFilesDir == null ){
	  typeFilesDir = rootDir + "/input/SampleApplications/metadata/type/"
          setPropertyFromConfigFile("TYPE_FILES_DIR", typeFilesDir)
	}

	var functionFilesDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("FUNCTION_FILES_DIR")
	if( functionFilesDir == null ){
	  functionFilesDir = rootDir + "/input/SampleApplications/metadata/function/"
          setPropertyFromConfigFile("FUNCTION_FILES_DIR", functionFilesDir)
	}

	var conceptFilesDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("CONCEPT_FILES_DIR")
	if( conceptFilesDir == null ){
	  conceptFilesDir = rootDir + "/input/SampleApplications/metadata/concept/"
          setPropertyFromConfigFile("CONCEPT_FILES_DIR", conceptFilesDir)
	}

	var messageFilesDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("MESSAGE_FILES_DIR")
	if( messageFilesDir == null ){
	  messageFilesDir = rootDir + "/input/SampleApplications/metadata/message/"
          setPropertyFromConfigFile("MESSAGE_FILES_DIR", messageFilesDir)
	}

	var containerFilesDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("CONTAINER_FILES_DIR")
	if( containerFilesDir == null ){
	  containerFilesDir = rootDir + "/input/SampleApplications/metadata/container/"
          setPropertyFromConfigFile("CONTAINER_FILES_DIR", containerFilesDir)
	}

	var configFilesDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("CONFIG_FILES_DIR")
	if( configFilesDir == null ){
	  configFilesDir = rootDir + "/input/SampleApplications/metadata/config/"
          setPropertyFromConfigFile("CONFIG_FILES_DIR", configFilesDir)
	}


	var securityImplJar = getMetadataAPI.GetMetadataAPIConfig.getProperty("SECURITY_IMPL_JAR")
	if( securityImplJar == null ){
	  securityImplJar = libSystemPath + "/simpleapacheshiroadapter_2.11-1.0.jar"
          setPropertyFromConfigFile("SECURITY_IMPL_JAR", securityImplJar)
	}

	var auditImplJar = getMetadataAPI.GetMetadataAPIConfig.getProperty("AUDIT_IMPL_JAR")
	if( auditImplJar == null ){
	  auditImplJar = libSystemPath + "/auditadapters_2.11-1.0.jar"
          setPropertyFromConfigFile("AUDIT_IMPL_JAR", auditImplJar)
	}

	var sslCertificate = getMetadataAPI.GetMetadataAPIConfig.getProperty("SSL_CERTIFICATE")
	if( sslCertificate == null ){
	  sslCertificate = rootDir + "/config/keystore.jks"
          setPropertyFromConfigFile("SSL_CERTIFICATE", sslCertificate)
	}

	var jarPaths = getMetadataAPI.GetMetadataAPIConfig.getProperty("JAR_PATHS")
	logger.debug("jarPaths => " + jarPaths)
	if( jarPaths == null ){
	  jarPaths = libSystemPath + "," + libApplicationPath
          setPropertyFromConfigFile("JAR_PATHS", jarPaths)
	}

	var jarTargetDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("JAR_TARGET_DIR")
	logger.debug("jarTargetDir => " + jarTargetDir)
	if( jarTargetDir == null ){
	  jarTargetDir = libApplicationPath
          setPropertyFromConfigFile("JAR_TARGET_DIR", jarTargetDir)
	}

	val classPath =  getMetadataAPI.GetMetadataAPIConfig.getProperty("CLASSPATH")
	logger.debug("classPath => " + classPath)
	val jarPathSet = getMetadataAPI.GetMetadataAPIConfig.getProperty("JAR_PATHS").split(",").toSet
	jarPathSet.foreach(j => { logger.debug("jarPath Element => " + j) })

	val libraryFile = rootDir + "/config/library_list"
	if( ! fileExists(libraryFile) ){
	  throw new Exception("Possible deployment error: The file " + libraryFile + " that lists the default libraries is not found")
	}

	var libList = new scala.collection.mutable.ListBuffer[String]()
	for (line <- Source.fromFile(libraryFile).getLines()) {
	  libList += line
	}

	if ( libList.length == 0 ){
	  throw new Exception("Possible deployment error: The file " + libraryFile + " is empty, It must contain default libraries")
	}

	val defaultClassPath = libList.map(j => com.ligadata.Utils.Utils.GetValidJarFile(jarPathSet, j)).mkString(":")

	logger.info("defaultClassPath => " + defaultClassPath)
	var finalClassPath = defaultClassPath
	if( classPath != null ){
	  finalClassPath = classPath + ":" + finalClassPath
	}

	setPropertyFromConfigFile("CLASSPATH", finalClassPath)
      }

      pList.map(v => logger.warn(v + " remains unset"))
      dumpMetadataAPIConfig
      MetadataAPIImpl.propertiesAlreadyLoaded = true;

    } catch {
      case e: Exception =>
        logger.error("Failed to load configuration", e)
        sys.exit(1)
    }
  }

    /**
     * Read the default configuration property values from config file.
      *
      * @param cfgFile
     */
  @throws(classOf[MissingPropertyException])
  @throws(classOf[LoadAPIConfigException])
  def readMetadataAPIConfigFromJsonFile(cfgFile: String): Unit = {
    val msg = "The MetadataAPI properties from a json file are nolonger supported"
    logger.error(msg)
    throw LoadAPIConfigException("Failed to load configuration", new Exception(msg))
  }

    /**
     * LoadAllConfigObjectsIntoCache
      *
      * @return
     */
  def LoadAllConfigObjectsIntoCache: Boolean = {
    try {
      var processed: Long = 0L
      val storeInfo = PersistenceUtils.GetContainerNameAndDataStore("config_objects")
      storeInfo._2.get(storeInfo._1, { (k: Key, v: Any, serType: String, typ: String, ver:Int) =>
        {
          val strKey = k.bucketKey.mkString(".")
          val i = strKey.indexOf(".")
          val objType = strKey.substring(0, i)
          val typeName = strKey.substring(i + 1)
          processed += 1
          objType match {
            case "nodeinfo" => {
              val ni = MetadataAPISerialization.deserializeMetadata(new String(v.asInstanceOf[Array[Byte]])).asInstanceOf[NodeInfo]//serializer.DeserializeObjectFromByteArray(v.asInstanceOf[Array[Byte]]).asInstanceOf[NodeInfo]
              MdMgr.GetMdMgr.AddNode(ni)

            }
            case "adapterinfo" => {
              val ai = MetadataAPISerialization.deserializeMetadata(new String(v.asInstanceOf[Array[Byte]])).asInstanceOf[AdapterInfo]//serializer.DeserializeObjectFromByteArray(v.asInstanceOf[Array[Byte]]).asInstanceOf[AdapterInfo]
              MdMgr.GetMdMgr.AddAdapter(ai)
            }
            case "clusterinfo" => {
              val ci = MetadataAPISerialization.deserializeMetadata(new String(v.asInstanceOf[Array[Byte]])).asInstanceOf[ClusterInfo]//serializer.DeserializeObjectFromByteArray(v.asInstanceOf[Array[Byte]]).asInstanceOf[ClusterInfo]
              MdMgr.GetMdMgr.AddCluster(ci)
            }
            case "clustercfginfo" => {
              val ci = MetadataAPISerialization.deserializeMetadata(new String(v.asInstanceOf[Array[Byte]])).asInstanceOf[ClusterCfgInfo]//serializer.DeserializeObjectFromByteArray(v.asInstanceOf[Array[Byte]]).asInstanceOf[ClusterCfgInfo]
              MdMgr.GetMdMgr.AddClusterCfg(ci)
            }
            case "userproperties" => {
              val up = MetadataAPISerialization.deserializeMetadata(new String(v.asInstanceOf[Array[Byte]])).asInstanceOf[UserPropertiesInfo]//serializer.DeserializeObjectFromByteArray(v.asInstanceOf[Array[Byte]]).asInstanceOf[UserPropertiesInfo]
              MdMgr.GetMdMgr.AddUserProperty(up)
            }
            case "tenantinfo" => {
                val ti = MetadataAPISerialization.deserializeMetadata(new String(v.asInstanceOf[Array[Byte]])).asInstanceOf[TenantInfo] //serializer.DeserializeObjectFromByteArray(v.asInstanceOf[Array[Byte]]).asInstanceOf[TenantInfo]
                MdMgr.GetMdMgr.AddTenantInfo(ti)
            }
            case "adaptermessagebinding" => {
                val binding = MetadataAPISerialization.deserializeMetadata(new String(v.asInstanceOf[Array[Byte]])).asInstanceOf[AdapterMessageBinding] //serializer.DeserializeObjectFromByteArray(v.asInstanceOf[Array[Byte]]).asInstanceOf[TenantInfo]
                MdMgr.GetMdMgr.AddAdapterMessageBinding(binding)
            }
            case _ => {
              throw InternalErrorException("LoadAllConfigObjectsIntoCache: Unknown objectType " + objType, null)
            }
          }
        }
      })

      if (processed == 0) {
        logger.debug("No config objects available in the Database")
        return false
      }

      return true
    } catch {
      case e: Exception => {

        logger.debug("", e)
        return false
      }
    }
  }

    /**
     * LoadAllModelConfigsIntoChache
     */
  def LoadAllModelConfigsIntoCache: Unit = {
    val maxTranId = PersistenceUtils.GetTranId
    getMetadataAPI.setCurrentTranLevel(maxTranId)
    logger.debug("Max Transaction Id => " + maxTranId)

    var processed: Long = 0L
    val storeInfo = PersistenceUtils.GetContainerNameAndDataStore("model_config_objects")
    storeInfo._2.get(storeInfo._1, { (k: Key, v: Any, serType: String, typ: String, ver:Int) =>
      {
        processed += 1
        val json = parse(new String(v.asInstanceOf[Array[Byte]]))
        //val conf = serializer.DeserializeObjectFromByteArray(v.asInstanceOf[Array[Byte]]).asInstanceOf[Map[String, List[String]]]
        val conf = json.values.asInstanceOf[Map[String, Any]]
        MdMgr.GetMdMgr.AddModelConfig(k.bucketKey.mkString("."), conf)
      }
    })
    if (processed == 0) {
      logger.debug("No model config objects available in the Database")
      return
    }
    MdMgr.GetMdMgr.DumpModelConfigs
  }

    /**
      * LoadAdapterMessageBindingIntoCache
      *
      * @param key string of the form "s"${zkMessage.ObjectType}.${zkMessage.Name}" where the object type is the
      *            "AdapterMessageBinding" and the Name is the FullBindingName of the object to fetch
      */
    def LoadAdapterMessageBindingIntoCache(key: String) {
        try {
            logger.debug("Fetch the AdapterMessageBinding with key: " + key + " from persistent store ")
            val (objtype, jsonBytes) : (String, Any) = PersistenceUtils.GetObject(key.toLowerCase, "adapter_message_bindings")
            logger.debug("Deserialize the binding " + key)
            val binding : AdapterMessageBinding =
                    MetadataAPISerialization.deserializeMetadata(new String(jsonBytes.asInstanceOf[Array[Byte]])).asInstanceOf[AdapterMessageBinding]
            if (binding != null) {
                if (! binding.IsDeleted) {
                    logger.debug("Add the binding " + key + " to the metadata cache ")
                    MdMgr.GetMdMgr.AddAdapterMessageBinding(binding)
                }
            }
        } catch {
            case e: Exception => {
                logger.warn("Unable to load the object " + key + " into cache ", e)
            }
        }
    }

    /**
      * Remove the supplied binding the the supplied zookeeper object and binding specific key.
      *
      * @param bindingKey "<adapter name>,<namespace.msgname>,<namespace.serializername>"
      */
    def RemoveAdapterMessageBindingFromCache(bindingKey : String): Unit = {
      try {
          val binding: AdapterMessageBinding  = mdMgr.RemoveAdapterMessageBinding(bindingKey)

          /** Note that even if it the binding is not in the mdMgr cache, we will proceed to remove it if possible
            * from the Storage. The MetadataAPI can delete it (it doesn't necessarily notify the engine... NOTIFY_ENGINE = NO... and get it
            * deleted on the back side during call back.  */
          val key = s"AdapterMessageBinding.$bindingKey"
          getMetadataAPI.DeleteObject(key.toLowerCase, "adapter_message_bindings")
          val apiResult = new ApiResult(ErrorCodeConstants.Success, "RemoveAdapterMessageBindingFromCache", null, ErrorCodeConstants.Remove_AdapterMessageBinding_Successful + ":" + bindingKey)
          apiResult.toString()

      } catch {
          case e: Exception => {
              /**
                * This is not necessarily catastrophic.  The binding could have been removed earlier depending upon the cluster
                * configuration. It will attemtp to delete twice when Notify_Engine = yes
                */
              logger.debug("", e)
              val apiResult = new ApiResult(ErrorCodeConstants.Failure, "RemoveAdapterMessageBindingFromCache", null, "Error :" + e.toString() + ErrorCodeConstants.Remove_AdapterMessageBinding_Failed + ":" + s"AdapterMessageBinding.$bindingKey")
              apiResult.toString()
          }
      }
    }


}
