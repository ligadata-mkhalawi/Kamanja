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

package com.ligadata.Migrate

import com.ligadata.KamanjaBase.AttributeTypeInfo.TypeCategory._
import com.ligadata.KamanjaBase.{ContainerFactoryInterface, MessageFactoryInterface, ContainerInterface, ObjectResolver}
import com.ligadata.KamanjaManager.KamanjaConfiguration
import com.ligadata.MigrateBase._
import com.ligadata.Utils.{KamanjaLoaderInfo, Utils}
import org.apache.logging.log4j._
import java.io.{DataInputStream, ByteArrayInputStream, File, PrintWriter}

import com.ligadata.kamanja.metadata._
import com.ligadata.kamanja.metadataload.MetadataLoad
import com.ligadata.MetadataAPI.MetadataAPIImpl
import com.ligadata.MetadataAPI.MetadataAPISerialization
import com.ligadata.MetadataAPI.MessageAndContainerUtils
import com.ligadata.MetadataAPI.PersistenceUtils
import com.ligadata.MetadataAPI.AdapterMessageBindingUtils
import com.ligadata.MetadataAPI.MetadataAPI
import com.ligadata.Serialize.JsonSerializer
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import scala.io.Source

import com.ligadata.KvBase.{KvBaseDefalts, TimeRange, Key}

import com.ligadata.StorageBase.{DataStore, DataStoreOperations}
import com.ligadata.keyvaluestore.KeyValueManager
import scala.collection.mutable.{HashMap, ArrayBuffer}
import com.ligadata.kamanja.metadata.ModelCompilationConstants
import com.ligadata.Exceptions._

import java.util.concurrent.atomic._

case class AdapterUniqueValueDes_1_3(T: Long, V: String, Out: Option[List[List[String]]]) // TransactionId, Value, Queues & Result Strings. Adapter Name, Key and Result Strings

import scala.actors.threadpool.{Executors, ExecutorService, TimeUnit}

case class adapterMessageBinding(var AdapterName: String, var MessageNames: List[String], var Options: Map[String, String], var Serializer: String)

class MigrateTo_V_1_5_0 extends MigratableTo {
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  // 646 - 676 Change begins - replace MetadataAPIImpl
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Change ends

  class MdObjectRes(val mdMgr: MdMgr, val jarPaths: Set[String]) extends ObjectResolver {
    private[this] var messageContainerObjects = new HashMap[String, ContainerFactoryInterface]
    private[this] val metadataLoader = new KamanjaLoaderInfo

    def GetAllJarsFromElem(elem: BaseElem): Set[String] = {
      var allJars: Array[String] = null

      val jarname = if (elem.JarName == null) "" else elem.JarName.trim

      if (elem.DependencyJarNames != null && elem.DependencyJarNames.size > 0 && jarname.size > 0) {
        allJars = elem.DependencyJarNames :+ jarname
      } else if (elem.DependencyJarNames != null && elem.DependencyJarNames.size > 0) {
        allJars = elem.DependencyJarNames
      } else if (jarname.size > 0) {
        allJars = Array(jarname)
      } else {
        return Set[String]()
      }

      return allJars.map(j => Utils.GetValidJarFile(jarPaths, j)).toSet
    }

    def LoadJarIfNeeded(elem: BaseElem): Boolean = {
      val allJars = GetAllJarsFromElem(elem)
      if (allJars.size > 0) {
        return Utils.LoadJars(allJars.toArray, metadataLoader.loadedJars, metadataLoader.loader)
      } else {
        return true
      }
    }

    private def PrepareMessages(tmpMsgDefs: Option[scala.collection.immutable.Set[MessageDef]]): Unit = {
      if (tmpMsgDefs == None) // Not found any messages
        return

      val msgDefs = tmpMsgDefs.get

      // Load all jars first
      msgDefs.foreach(msg => {
        LoadJarIfNeeded(msg)
      })

      msgDefs.foreach(msg => {
        PrepareMessage(msg, false) // Already Loaded required dependency jars before calling this
      })
    }


    private def PrepareContainers(tmpContainerDefs: Option[scala.collection.immutable.Set[ContainerDef]]): Unit = {
      if (tmpContainerDefs == None) // Not found any containers
        return

      val containerDefs = tmpContainerDefs.get

      // Load all jars first
      containerDefs.foreach(container => {
        LoadJarIfNeeded(container)
      })

      val baseContainersPhyName = scala.collection.mutable.Set[String]()
      val baseContainerInfo = MetadataLoad.ContainerInterfacesInfo
      baseContainerInfo.foreach(bc => {
        baseContainersPhyName += bc._3
      })

      containerDefs.foreach(container => {
        PrepareContainer(container, false, (container.PhysicalName.equalsIgnoreCase("com.ligadata.KamanjaBase.KamanjaModelEvent$") == false) && baseContainersPhyName.contains(container.PhysicalName.trim)) // Already Loaded required dependency jars before calling this
      })

    }

    private[this] def CheckAndPrepMessage(clsName: String, msg: MessageDef): Boolean = {
      var isMsg = true
      var curClass: Class[_] = null

      try {
        // If required we need to enable this test
        // Convert class name into a class
        var curClz = Class.forName(clsName, true, metadataLoader.loader)
        curClass = curClz

        isMsg = false

        while (curClz != null && isMsg == false) {
          isMsg = Utils.isDerivedFrom(curClz, "com.ligadata.KamanjaBase.MessageFactoryInterface")
          if (isMsg == false)
            curClz = curClz.getSuperclass()
        }
      } catch {
        case e: Exception => {
          logger.debug("Failed to get message classname :" + clsName, e)
          return false
        }
      }

      if (isMsg) {
        try {
          var objinst: Any = null
          try {
            // Trying Singleton Object
            val module = metadataLoader.mirror.staticModule(clsName)
            val obj = metadataLoader.mirror.reflectModule(module)
            objinst = obj.instance
          } catch {
            case e: Exception => {
              // Trying Regular Object instantiation
              logger.debug("", e)
              objinst = curClass.newInstance
            }
          }
          if (objinst.isInstanceOf[MessageFactoryInterface]) {
            val messageobj = objinst.asInstanceOf[MessageFactoryInterface]
            val msgName = msg.FullName.toLowerCase
            messageContainerObjects(msgName) = messageobj

            logger.info("Created Message:" + msgName)
            return true
          } else {
            logger.debug("Failed to instantiate message object :" + clsName)
            return false
          }
        } catch {
          case e: Exception => {
            logger.debug("Failed to instantiate message object:" + clsName, e)
            return false
          }
        }
      }
      return false
    }

    def PrepareMessage(msg: MessageDef, loadJars: Boolean): Unit = {
      if (loadJars)
        LoadJarIfNeeded(msg)
      // else Assuming we are already loaded all the required jars

      var clsName = msg.PhysicalName.trim
      var orgClsName = clsName

      var foundFlg = CheckAndPrepMessage(clsName, msg)

      if (foundFlg == false) {
        // if no $ at the end we are taking $
        if (clsName.size > 0 && clsName.charAt(clsName.size - 1) != '$') {
          clsName = clsName + "$"
          foundFlg = CheckAndPrepMessage(clsName, msg)
        }
      }
      if (foundFlg == false) {
        logger.error("Failed to instantiate message object:%s, class name:%s".format(msg.FullName, orgClsName))
      }
    }

    private[this] def CheckAndPrepContainer(clsName: String, container: ContainerDef): Boolean = {
      var isContainer = true
      var curClass: Class[_] = null

      try {
        // If required we need to enable this test
        // Convert class name into a class
        var curClz = Class.forName(clsName, true, metadataLoader.loader)
        curClass = curClz

        isContainer = false

        while (curClz != null && isContainer == false) {
          isContainer = Utils.isDerivedFrom(curClz, "com.ligadata.KamanjaBase.ContainerFactoryInterface")
          if (isContainer == false)
            curClz = curClz.getSuperclass()
        }
      } catch {
        case e: Exception => {
          logger.debug("Failed to get container classname: " + clsName, e)
          return false
        }
      }

      if (isContainer) {
        try {
          var objinst: Any = null
          try {
            // Trying Singleton Object
            val module = metadataLoader.mirror.staticModule(clsName)
            val obj = metadataLoader.mirror.reflectModule(module)
            objinst = obj.instance
          } catch {
            case e: Exception => {
              logger.error("", e)
              // Trying Regular Object instantiation
              objinst = curClass.newInstance
            }
          }

          if (objinst.isInstanceOf[ContainerFactoryInterface]) {
            val containerobj = objinst.asInstanceOf[ContainerFactoryInterface]
            val contName = container.FullName.toLowerCase
            messageContainerObjects(contName) = containerobj

            logger.info("Created Container:" + contName)
            return true
          } else {
            logger.debug("Failed to instantiate container object :" + clsName)
            return false
          }
        } catch {
          case e: Exception => {
            logger.debug("Failed to instantiate containerObjects object:" + clsName, e)
            return false
          }
        }
      }
      return false
    }

    def PrepareContainer(container: ContainerDef, loadJars: Boolean, ignoreClassLoad: Boolean): Unit = {
      if (loadJars)
        LoadJarIfNeeded(container)
      // else Assuming we are already loaded all the required jars

      if (ignoreClassLoad) {
        val contName = container.FullName.toLowerCase
        messageContainerObjects(contName) = null
        logger.debug("Added Base Container:" + contName)
        return
      }

      var clsName = container.PhysicalName.trim
      var orgClsName = clsName

      var foundFlg = CheckAndPrepContainer(clsName, container)
      if (foundFlg == false) {
        // if no $ at the end we are taking $
        if (clsName.size > 0 && clsName.charAt(clsName.size - 1) != '$') {
          clsName = clsName + "$"
          foundFlg = CheckAndPrepContainer(clsName, container)
        }
      }
      if (foundFlg == false) {
        logger.error("Failed to instantiate container object:%s, class name:%s".format(container.FullName, orgClsName))
      }
    }

    def ResolveMessageAndContainers(): Unit = {
      val tmpMsgDefs = mdMgr.Messages(true, true)
      val tmpContainerDefs = mdMgr.Containers(true, true)
      PrepareMessages(tmpMsgDefs)
      PrepareContainers(tmpContainerDefs)
    }

    def isMsgOrContainer(msgOrContainerName: String): Boolean = {
      if (messageContainerObjects == null) return false
      val v = messageContainerObjects.getOrElse(msgOrContainerName.toLowerCase, null)
      (v != null)
    }

    override def getInstance(msgOrContainerName: String): ContainerInterface = {
      if (messageContainerObjects == null) return null
      val v = messageContainerObjects.getOrElse(msgOrContainerName.toLowerCase, null)
      if (v != null && v.isInstanceOf[MessageFactoryInterface]) {
        return v.createInstance.asInstanceOf[ContainerInterface]
      } else if (v != null && v.isInstanceOf[ContainerFactoryInterface]) {
        // NOTENOTE: Not considering Base containers here
        return v.createInstance.asInstanceOf[ContainerInterface]
      }
      return null
    }

    override def getInstance(schemaId: Long): ContainerInterface = {
      //BUGBUG:: For now we are getting latest class. But we need to get the old one too.
      val md = mdMgr

      if (md == null)
        throw new KamanjaException("Metadata Not found", null)

      val contOpt = getMdMgr.ContainerForSchemaId(schemaId.toInt)

      if (contOpt == None)
        throw new KamanjaException("Container Not found for schemaid:" + schemaId, null)

      getInstance(contOpt.get.FullName)
    }

    override def getMdMgr: MdMgr = mdMgr
  }

  private var _unhandledMetadataDumpDir: String = _
  private var _curMigrationSummaryFlPath: String = _
  private var _sourceVersion: String = _
  private var _fromScalaVersion: String = _
  private var _toScalaVersion: String = _
  private var _destInstallPath: String = _
  private var _apiConfigFile: String = _
  private var _clusterConfigFile: String = _
  private var _metaDataStoreInfo: String = _
  private var _dataStoreInfo: String = _
  private var _statusStoreInfo: String = _
  private var _tenantDatastoreInfo: String = _
  private var _metaDataStoreDb: DataStore = _
  private var _dataStoreDb: DataStore = _
  private var _statusStoreDb: DataStore = _
  private var _tenantDsDb: DataStore = _
  private var _jarPaths: collection.immutable.Set[String] = collection.immutable.Set[String]()
  private var _bInit = false
  private var _flCurMigrationSummary: PrintWriter = _
  private val defaultUserId: Option[String] = Some("kamanja")
  private var _parallelDegree = 0
  private var _mergeContainerAndMessages = true
  private var _tenantId: Option[String] = None
  private var _dataConversionInitLock = new Object()
  private var _mdObjectRes: MdObjectRes = null
  private var _adapterMessageBindings: Option[String] = None

  private val globalExceptions = ArrayBuffer[(String, Throwable)]()
  private var _uniqueIdGenerator:AtomicInteger = null

  private def AddToGlobalException(failedMsg: String, e: Throwable): Unit = {
    globalExceptions += ((failedMsg, e))
  }

  private def AddMdObjToGlobalException(mdObj: (String, Map[String, Any]), failedMsg: String, e: Throwable): Unit = {
    val namespace = mdObj._2.getOrElse("NameSpace", "").toString.trim()
    val name = mdObj._2.getOrElse("Name", "").toString.trim()
    AddToGlobalException(failedMsg + ":" + namespace + "." + name, e)
  }

  private def LogGlobalException: Boolean = {
    if (globalExceptions.size > 0) {
      globalExceptions.foreach(expTup => {
        if (expTup._2 != null)
          logger.error(if (expTup._1 != null) expTup._1 else "", expTup._2)
        else
          logger.error(if (expTup._1 != null) expTup._1 else "")
      })
      return true
    }

    return false
  }


  private def isValidPath(path: String, checkForDir: Boolean = false, checkForFile: Boolean = false, str: String = "path"): Unit = {
    val fl = new File(path)
    if (fl.exists() == false) {
      val szMsg = "Given %s:%s does not exists".format(str, path)
      logger.error(szMsg)
      throw new Exception(szMsg)
    }

    if (checkForDir && fl.isDirectory() == false) {
      val szMsg = "Given %s:%s is not directory".format(str, path)
      logger.error(szMsg)
      throw new Exception(szMsg)
    }

    if (checkForFile && fl.isFile() == false) {
      val szMsg = "Given %s:%s is not file".format(str, path)
      logger.error(szMsg)
      throw new Exception(szMsg)
    }
  }

  private def getStringFromJsonNode(v: Any): String = {
    if (v == null) return ""

    if (v.isInstanceOf[String]) return v.asInstanceOf[String]

    implicit val jsonFormats: Formats = DefaultFormats
    val lst = List(v)
    val str = org.json4s.jackson.Serialization.write(lst)
    if (str.size > 2) {
      return str.substring(1, str.size - 1)
    }
    return ""
  }

  private def GetDataStoreStatusStoreInfo(cfgStr: String): (String, String, String) = {
    var tenantDsStr: String = null
    var dsStr: String = null
    var ssStr: String = ""

    try {
      // extract config objects
      val map = JsonSerializer.parseEngineConfig(cfgStr)
      // process clusterInfo object if it exists
      if (map.contains("Clusters")) {
        val clustersList = map.get("Clusters").get.asInstanceOf[List[_]]
        val clusters = clustersList.length
        logger.debug("Found " + clusters + " cluster objects ")
        clustersList.foreach(clustny => {
          if (dsStr == null || dsStr.size == 0) {
            val cluster = clustny.asInstanceOf[Map[String, Any]]
            val ClusterId = cluster.getOrElse("ClusterId", "").toString.trim.toLowerCase
            logger.debug("Processing the cluster => " + ClusterId)
            if (ClusterId.size > 0 && cluster.contains("SystemCatalog"))
              dsStr = getStringFromJsonNode(cluster.getOrElse("SystemCatalog", null))
          }
          if (ssStr == null || ssStr.size == 0) {
            val cluster = clustny.asInstanceOf[Map[String, Any]]
            val ClusterId = cluster.getOrElse("ClusterId", "").toString.trim.toLowerCase
            logger.debug("Processing the cluster => " + ClusterId)
            if (ClusterId.size > 0 && cluster.contains("StatusInfo"))
              ssStr = getStringFromJsonNode(cluster.getOrElse("StatusInfo", null))
          }
          if ((tenantDsStr == null || tenantDsStr.size == 0) && _tenantId != None) {
            val givenTenantId = _tenantId.get
            val cluster = clustny.asInstanceOf[Map[String, Any]]
            val ClusterId = cluster.getOrElse("ClusterId", "").toString.trim.toLowerCase
            logger.debug("Processing the cluster => " + ClusterId)
            if (ClusterId.size > 0 && cluster.contains("Tenants")) {
              val tmpTenants = cluster.getOrElse("Tenants", null)
              if (tmpTenants != null) {
                val tenants = tmpTenants.asInstanceOf[List[Map[String, Any]]]
                tenants.foreach(tInfo => {
                  if ((tenantDsStr == null || tenantDsStr.size == 0) && tInfo != null) {
                    val tName = tInfo.getOrElse("TenantId", "").asInstanceOf[String].trim.toLowerCase
                    if (tName.equalsIgnoreCase(givenTenantId)) {
                      tenantDsStr = getStringFromJsonNode(tInfo.getOrElse("PrimaryDataStore", null))
                    }
                  }
                })
              }
            }
          }
        })
      }
      logger.debug("Found Datastore String:%s and Statusstore String:%s".format(dsStr, ssStr));
      (dsStr, ssStr, tenantDsStr)
    } catch {
      case e: Exception => {
        throw new Exception("Failed to parse clusterconfig", e)
      }
      case e: Throwable => {
        throw new Exception("Failed to parse clusterconfig", e)
      }
    }
  }

  private def GetDataStoreHandle(jarPaths: collection.immutable.Set[String], dataStoreInfo: String): DataStore = {
    try {
      logger.debug("Getting DB Connection for dataStoreInfo:%s".format(dataStoreInfo))
      return KeyValueManager.Get(jarPaths, dataStoreInfo, null, null)
    } catch {
      case e: Exception => throw e
      case e: Throwable => throw e
    }
  }

  override def init(destInstallPath: String, apiConfigFile: String, clusterConfigFile: String, sourceVersion: String, unhandledMetadataDumpDir: String, curMigrationSummaryFlPath: String, parallelDegree: Int, mergeContainerAndMessages: Boolean, fromScalaVersion: String, toScalaVersion: String, tenantId: String, adapterMessageBindings: String): Unit = {
    isValidPath(apiConfigFile, false, true, "apiConfigFile")
    isValidPath(clusterConfigFile, false, true, "clusterConfigFile")

    isValidPath(destInstallPath, true, false, "destInstallPath")
    isValidPath(destInstallPath + "/bin", true, false, "bin folder in destInstallPath")
    isValidPath(destInstallPath + "/lib/system", true, false, "/lib/system folder in destInstallPath")
    isValidPath(destInstallPath + "/lib/application", true, false, "/lib/application folder in destInstallPath")

    isValidPath(unhandledMetadataDumpDir, true, false, "unhandledMetadataDumpDir")

    MdMgr.GetMdMgr.truncate
    val mdLoader = new MetadataLoad(MdMgr.mdMgr, "", "", "", "")
    mdLoader.initialize
    getMetadataAPI.readMetadataAPIConfigFromPropertiesFile(apiConfigFile)

    if (tenantId != null && tenantId.size > 0) {
      _tenantId = Some(tenantId)
    }
    else {
      throw new Exception("tenantId can't be null")
    }

    if (adapterMessageBindings != null && adapterMessageBindings.size > 0) {
      _adapterMessageBindings = Some(adapterMessageBindings)
    }
    else {
      if( _sourceVersion == "1.1" || _sourceVersion == "1.2" || _sourceVersion == "1.3" ){
	throw new Exception("adapterMessageBindings can't be null")
      }
    }

    if( _sourceVersion == "1.1" || _sourceVersion == "1.2" || _sourceVersion == "1.3" ){
      isValidPath(adapterMessageBindings);
    }

    val tmpJarPaths = getMetadataAPI.GetMetadataAPIConfig.getProperty("JAR_PATHS")
    val jarPaths = if (tmpJarPaths != null) tmpJarPaths.split(",").toSet else scala.collection.immutable.Set[String]()
    val metaDataStoreInfo = getMetadataAPI.GetMetadataAPIConfig.getProperty("METADATA_DATASTORE");
    val cfgStr = Source.fromFile(clusterConfigFile).mkString
    val (dataStoreInfo, statusStoreInfo, tenantDatastoreInfo) = GetDataStoreStatusStoreInfo(cfgStr)

    if (metaDataStoreInfo == null || metaDataStoreInfo.size == 0) {
      throw new Exception("Not found valid MetadataStore info in " + apiConfigFile)
    }

    if (dataStoreInfo == null || dataStoreInfo.size == 0) {
      throw new Exception("Not found valid DataStore info in " + clusterConfigFile)
    }

    if (tenantDatastoreInfo == null || tenantDatastoreInfo.trim.size == 0) {
      throw new Exception("Not found valid PrimaryDataStore for given tenantId:" + _tenantId.get)
    }

    val sysPath = new File(destInstallPath + "/lib/system")
    val appPath = new File(destInstallPath + "/lib/application")

    val toVersionJarPaths = collection.immutable.Set[String](sysPath.getAbsolutePath, appPath.getAbsolutePath) ++ jarPaths

    _destInstallPath = destInstallPath
    _apiConfigFile = apiConfigFile
    _clusterConfigFile = clusterConfigFile

    _metaDataStoreInfo = metaDataStoreInfo
    _dataStoreInfo = dataStoreInfo
    _statusStoreInfo = if (statusStoreInfo == null) "" else statusStoreInfo
    _tenantDatastoreInfo = if (tenantDatastoreInfo == null) "" else tenantDatastoreInfo
    _jarPaths = toVersionJarPaths
    _sourceVersion = sourceVersion
    _fromScalaVersion = fromScalaVersion
    _toScalaVersion = toScalaVersion

    _unhandledMetadataDumpDir = unhandledMetadataDumpDir
    _curMigrationSummaryFlPath = curMigrationSummaryFlPath

    _flCurMigrationSummary = new PrintWriter(_curMigrationSummaryFlPath, "UTF-8")

    // Open the database here
    _metaDataStoreDb = GetDataStoreHandle(toVersionJarPaths, metaDataStoreInfo)
    _dataStoreDb = GetDataStoreHandle(toVersionJarPaths, dataStoreInfo)

    if (_statusStoreInfo.size > 0) {
      _statusStoreDb = GetDataStoreHandle(toVersionJarPaths, _statusStoreInfo)
    }

    _parallelDegree = if (parallelDegree <= 1) 1 else parallelDegree
    _mergeContainerAndMessages = mergeContainerAndMessages

    _tenantDsDb = GetDataStoreHandle(toVersionJarPaths, tenantDatastoreInfo)

    _uniqueIdGenerator = new AtomicInteger((System.currentTimeMillis()/1000).toInt)

    _bInit = true
  }

  override def isInitialized: Boolean = _bInit

  override def getMetadataStoreInfo: String = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    _metaDataStoreInfo
  }

  override def getDataStoreInfo: String = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    _dataStoreInfo
  }

  override def getStatusStoreInfo: String = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    _statusStoreInfo
  }

  override def getMetadataTableName(containerName: String): String = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    _metaDataStoreDb.getTableName(containerName)
  }

  override def getDataTableName(containerName: String): String = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    //BUGBUG:: If this table is realted to _tenantDsDb, it may have different name
    _dataStoreDb.getTableName(containerName)
  }

  override def isMetadataTableExists(tblInfo: TableName): Boolean = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    logger.debug("Check whether table exists or not, namespace => " + tblInfo.namespace + ",name => " + tblInfo.name)
    _metaDataStoreDb.isTableExists(tblInfo.namespace, tblInfo.name)
  }

  override def isDataTableExists(tblInfo: TableName): Boolean = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    logger.info("Checking whether table for the container " + tblInfo.name + " exists ")
    _dataStoreDb.isTableExists(tblInfo.namespace, tblInfo.name) || _tenantDsDb.isTableExists(tblInfo.namespace, tblInfo.name)
    //_dataStoreDb.isContainerExists(tblInfo.name)
  }

  override def isStatusTableExists(tblInfo: TableName): Boolean = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    if (_statusStoreDb != null)
      return _statusStoreDb.isTableExists(tblInfo.namespace, tblInfo.name)
    //return _statusStoreDb.isContainerExists(tblInfo.name)
    false
  }


  override def getDataTableSchemaName: String = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")

    if (_dataStoreInfo.trim.size == 0)
      return null

    var parsed_json: Map[String, Any] = null
    try {
      val json = parse(_dataStoreInfo)
      if (json == null || json.values == null) {
        val msg = "Failed to parse JSON configuration string:" + _dataStoreInfo
        throw new Exception(msg)
      }
      parsed_json = json.values.asInstanceOf[Map[String, Any]]
    } catch {
      case e: Exception => {
        throw new Exception("Failed to parse JSON configuration string:" + _dataStoreInfo, e)
      }
    }

    val namespace = if (parsed_json.contains("SchemaName")) parsed_json.getOrElse("SchemaName", "default").toString.trim else parsed_json.getOrElse("SchemaName", "default").toString.trim
    namespace
  }

  override def getTenantTableSchemaName: String = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")

    if (_tenantDatastoreInfo.trim.size == 0)
      return null

    var parsed_json: Map[String, Any] = null
    try {
      val json = parse(_tenantDatastoreInfo)
      if (json == null || json.values == null) {
        val msg = "Failed to parse JSON configuration string:" + _tenantDatastoreInfo
        throw new Exception(msg)
      }
      parsed_json = json.values.asInstanceOf[Map[String, Any]]
    } catch {
      case e: Exception => {
        throw new Exception("Failed to parse JSON configuration string:" + _tenantDatastoreInfo, e)
      }
    }

    val namespace = if (parsed_json.contains("SchemaName")) parsed_json.getOrElse("SchemaName", "default").toString.trim else parsed_json.getOrElse("SchemaName", "default").toString.trim
    namespace
  }

  override def createMetadataTables(): Unit = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    // Create metadataTables
    val metadataTables = Array("metadata_objects", "jar_store", "config_objects", "model_config_objects", "transaction_id", "metadatacounters", "avroschemainfo", "elementinfo")
    _metaDataStoreDb.CreateMetadataContainer(metadataTables)
  }


  private def getUniqueId: Long = {
    _uniqueIdGenerator.incrementAndGet
  }

  private def addBackupTablesToExecutor(executor: ExecutorService, storeDb: DataStore, tblsToBackedUp: Array[BackupTableInfo], errMsgTemplate: String, force: Boolean): Unit = {
    tblsToBackedUp.foreach(backupTblInfo => {
      executor.execute(new Runnable() {
        override def run() = {
          try {
            logger.info("Copy the table " + backupTblInfo.srcTable + " to " + backupTblInfo.dstTable)
            storeDb.copyTable(backupTblInfo.namespace, backupTblInfo.srcTable, backupTblInfo.dstTable, force)
            //storeDb.copyContainer(backupTblInfo.srcTable, backupTblInfo.dstTable, force)
          } catch {
            case e: Exception => AddToGlobalException(errMsgTemplate + "(" + backupTblInfo.namespace + "," + backupTblInfo.srcTable + " => " + backupTblInfo.namespace + "," + backupTblInfo.dstTable + ")", e)
            case e: Throwable => AddToGlobalException(errMsgTemplate + "(" + backupTblInfo.namespace + "," + backupTblInfo.srcTable + " => " + backupTblInfo.namespace + "," + backupTblInfo.dstTable + ")", e)
          }
        }
      })
    })
  }

  override def backupAllTables(metadataTblsToBackedUp: Array[BackupTableInfo], dataTblsToBackedUp: Array[BackupTableInfo], statusTblsToBackedUp: Array[BackupTableInfo], force: Boolean): Unit = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    logger.debug("Backup tables => (Metadata Tables:{" + metadataTblsToBackedUp.map(t => "((" + t.namespace + "," + t.srcTable + ") => (" + t.namespace + "," + t.dstTable + "))").mkString(",") + "}, Data Tables:{"
      + dataTblsToBackedUp.map(t => "((" + t.namespace + "," + t.srcTable + ") => (" + t.namespace + "," + t.dstTable + "))").mkString(",") + "}, Status Tables:{"
      + statusTblsToBackedUp.map(t => "((" + t.namespace + "," + t.srcTable + ") => (" + t.namespace + "," + t.dstTable + "))").mkString(",") + "})")
    var executor: ExecutorService = null
    try {
      executor = Executors.newFixedThreadPool(if (_parallelDegree <= 1) 1 else _parallelDegree)

      if (statusTblsToBackedUp.size > 0) {
        if (_statusStoreDb == null)
          throw new Exception("Does not have Status store information")
        addBackupTablesToExecutor(executor, _statusStoreDb, statusTblsToBackedUp, "Failed to backup status table:", force)
      }

      addBackupTablesToExecutor(executor, _metaDataStoreDb, metadataTblsToBackedUp, "Failed to backup metadata table:", force)
      addBackupTablesToExecutor(executor, _dataStoreDb, dataTblsToBackedUp, "Failed to backup data table:", force)
      // addBackupTablesToExecutor(executor, _tenantDsDb, dataTblsToBackedUp, "Failed to backup data table:", force)
      executor.shutdown();
      try {
        executor.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS);
      } catch {
        case e: Exception => AddToGlobalException("Failed to backup tables", e)
        case e: Throwable => AddToGlobalException("Failed to backup tables", e)
      }
    } catch {
      case e: Exception => AddToGlobalException("Failed to backup tables", e)
      case e: Throwable => AddToGlobalException("Failed to backup tables", e)
    }

    if (executor != null) {
      executor.shutdown();
      try {
        executor.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS);
      } catch {
        case e: Exception => AddToGlobalException("Failed to backup tables", e)
        case e: Throwable => AddToGlobalException("Failed to backup tables", e)
      }
    }

    if (LogGlobalException) {
      throw new Exception("Failed to Backup tables")
    }
  }

  private def addDropTablesToExecutor(executor: ExecutorService, storeDb: DataStore, tblsToDrop: Array[(String, String)], errMsgTemplate: String): Unit = {
    if (tblsToDrop.size > 0) {
      tblsToDrop.foreach(dropTblTuple => {
        executor.execute(new Runnable() {
          override def run() = {
            try {
              storeDb.dropTables(Array(dropTblTuple))
            } catch {
              case e: Exception => AddToGlobalException(errMsgTemplate + dropTblTuple.toString(), e)
              case e: Throwable => AddToGlobalException(errMsgTemplate + dropTblTuple.toString(), e)
            }
          }
        })
      })
    }
  }

  def dropAllTables(metadataTblsToDrop: Array[TableName], dataTblsToDrop: Array[TableName], statusTblsToDrop: Array[TableName]): Unit = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    var executor: ExecutorService = null
    try {
      executor = Executors.newFixedThreadPool(if (_parallelDegree <= 1) 1 else _parallelDegree)

      val metadataTblsTuples = metadataTblsToDrop.map(t => (t.namespace, t.name))
      val dataTblsTuples = dataTblsToDrop.map(t => (t.namespace, t.name))
      val statusTblsTuples = statusTblsToDrop.map(t => (t.namespace, t.name))
      logger.debug("Drop tables => (Metadata Tables:{" + metadataTblsTuples.mkString(",") + "}, Data Tables:" + dataTblsTuples.mkString(",") + "}, Status Tables:" + statusTblsTuples.mkString(",") + "})")

      if (statusTblsTuples.size > 0) {
        if (_statusStoreDb == null)
          throw new Exception("Does not have Status store information")
        addDropTablesToExecutor(executor, _statusStoreDb, statusTblsTuples, "Failed to drop status table:")
      }

      addDropTablesToExecutor(executor, _metaDataStoreDb, metadataTblsTuples, "Failed to drop data table:")
      addDropTablesToExecutor(executor, _dataStoreDb, dataTblsTuples, "Failed to drop metadata table:")

      executor.shutdown();
      try {
        executor.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS);
      } catch {
        case e: Exception => AddToGlobalException("Failed to drop tables", e)
        case e: Throwable => AddToGlobalException("Failed to drop tables", e)
      }
    } catch {
      case e: Exception => AddToGlobalException("Failed to drop tables", e)
      case e: Throwable => AddToGlobalException("Failed to drop tables", e)
    }

    if (executor != null) {
      executor.shutdown();
      try {
        executor.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS);
      } catch {
        case e: Exception => AddToGlobalException("Failed to drop tables", e)
        case e: Throwable => AddToGlobalException("Failed to drop tables", e)
      }
    }

    if (LogGlobalException) {
      throw new Exception("Failed to Drop tables")
    }
  }

  override def dropMessageContainerTablesFromMetadata(allMetadataElemsJson: Array[MetadataFormat]): Unit = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")

    val messagesAndContainers = scala.collection.mutable.Set[String]()

    allMetadataElemsJson.foreach(mdf => {
      val json = parse(mdf.objDataInJson)
      val jsonObjMap = json.values.asInstanceOf[Map[String, Any]]

      val isActiveStr = jsonObjMap.getOrElse("IsActive", "").toString.trim()
      if (isActiveStr.size > 0) {
        val isActive = jsonObjMap.getOrElse("IsActive", "").toString.trim().toBoolean
        if (isActive) {
          if ((mdf.objType == "MessageDef") || (mdf.objType == "ContainerDef")) {
            val namespace = jsonObjMap.getOrElse("NameSpace", "").toString.trim()
            val name = jsonObjMap.getOrElse("Name", "").toString.trim()
            messagesAndContainers += (namespace + "." + name).toLowerCase()
          }
        }
      }
    })

    if (messagesAndContainers.size > 0) {
      logger.debug("Dropping messages/containers:" + messagesAndContainers.mkString(","))
      var executor: ExecutorService = null
      try {
        executor = Executors.newFixedThreadPool(if (_parallelDegree <= 1) 1 else _parallelDegree)
        messagesAndContainers.foreach(dropContainer => {
          executor.execute(new Runnable() {
            override def run() = {
              try {
                // Dropping both Syscatalog & tenant datastore
                _dataStoreDb.DropContainer(Array(dropContainer))
                _tenantDsDb.DropContainer(Array(dropContainer))
              } catch {
                case e: Exception => AddToGlobalException("Failed to drop data message/container:" + dropContainer, e)
                case e: Throwable => AddToGlobalException("Failed to drop data message/container:" + dropContainer, e)
              }
            }
          })
        })

        executor.shutdown();
        try {
          executor.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS);
        } catch {
          case e: Exception => AddToGlobalException("Failed to drop messsages/containers", e)
          case e: Throwable => AddToGlobalException("Failed to drop messsages/containers", e)
        }
      } catch {
        case e: Exception => AddToGlobalException("Failed to drop messsages/containers", e)
        case e: Throwable => AddToGlobalException("Failed to drop messsages/containers", e)
      }

      if (executor != null) {
        executor.shutdown();
        try {
          executor.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS);
        } catch {
          case e: Exception => AddToGlobalException("Failed to drop messsages/containers", e)
          case e: Throwable => AddToGlobalException("Failed to drop messsages/containers", e)
        }
      }

      if (LogGlobalException) {
        throw new Exception("Failed to Drop messsages/containers")
      }
    }
  }

  private def WriteStringToFile(flName: String, str: String): Unit = {
    val out = new PrintWriter(flName, "UTF-8")
    try {
      out.print(str)
    } catch {
      case e: Exception => throw e;
      case e: Throwable => throw e;
    } finally {
      out.close
    }
  }

  private def isFailedStatus(retRes: String): Boolean = {
    implicit val formats = org.json4s.DefaultFormats
    val json = org.json4s.jackson.JsonMethods.parse(retRes)
    val statusCodeAny = (json \\ "Status Code").values
    var statusCode = 0
    if (statusCodeAny.isInstanceOf[List[_]]) {
      val t = statusCodeAny.asInstanceOf[List[_]].map(v => v.toString.toInt)
      statusCode = if (t.size > 0) t(0) else -1
    } else if (statusCodeAny.isInstanceOf[Array[_]]) {
      val t = statusCodeAny.asInstanceOf[Array[_]].map(v => v.toString.toInt)
      statusCode = if (t.size > 0) t(0) else -1
    } else {
      statusCode = statusCodeAny.toString.toInt
    }
    /*
    val functionName = (json \\ "Function Name").values.toString
    val resultData = (json \\ "Results Data").values.toString
    val description = (json \\ "Result Description").values.toString
*/
    return (statusCode != 0)
  }

  val curVersion = "1.5.4"

  private def DepJars(depJars1: List[String]): List[String] = {

    // Removing jars which are not valid any more, All these jars are consolidated(or assembled) into three fat jars: "ExtDependencyLibs_2.11-${curVersion}.jar","KamanjaInternalDeps_2.11-${curVersion}.jar","ExtDependencyLibs2_2.11-${curVersion}.jar".
    val jarsToBeExcluded = List("spray-httpx_2.10-1.3.3.jar","messagedef_2.11-1.0.jar","joni-2.1.2.jar","jets3t-0.9.0.jar","utilityservice_2.10-1.0.jar","scala-reflect-2.10.4.jar","servlet-api-2.5.jar","serialize_2.10-1.0.jar","jdom-1.1.jar","messagedef_2.10-1.0.jar","hamcrest-core-1.3.jar","commons-collections4-4.0.jar","curator-test-2.8.0.jar","commons-beanutils-core-1.8.0.jar","jsondatagen_2.10-0.1.0.jar","jline-0.9.94.jar","hadoop-common-2.7.1.jar","metrics-core-3.0.2.jar","snappy-java-1.0.4.1.jar","commons-lang3-3.1.jar","lz4-1.2.0.jar","commons-configuration-1.6.jar","datadelimiters_2.11-1.0.jar","jdbcdatacollector_2.10-1.0.jar","commons-httpclient-3.1.jar","customudflib_2.10-1.0.jar","exceptions_2.10-1.0.jar","curator-client-2.7.1.jar","chill_2.10-0.5.0.jar","curator-recipes-2.6.0.jar","jarfactoryofmodelinstancefactory_2.10-1.0.jar","commons-configuration-1.7.jar","simplekafkaproducer_2.10-0.1.0.jar","spray-can_2.10-1.3.3.jar","zookeeperleaderlatch_2.10-1.0.jar","compress-lzf-0.9.1.jar","metadata_2.11-1.0.jar","chill-java-0.5.0.jar","scala-compiler-2.10.0.jar","methodextractor_2.11-1.0.jar","javassist-3.18.1-GA.jar","commons-logging-1.1.1.jar","hbase-protocol-1.0.2.jar","json4s-jackson_2.10-3.2.9.jar","jackson-core-asl-1.9.13.jar","cassandra-thrift-2.0.3.jar","bootstrap_2.10-1.0.jar","cassandra_2.10-0.1.0.jar","commons-beanutils-1.7.0.jar","jersey-server-1.9.jar","securityadapterbase_2.10-1.0.jar","pmmlruntime_2.11-1.0.jar","kafkasimpleinputoutputadapters_2.10-1.0.jar","slf4j-log4j12-1.7.10.jar","xmlenc-0.52.jar","bootstrap_2.11-1.0.jar","pmmludfs_2.11-1.0.jar","kamanjabase_2.11-1.0.jar","pmmlruntime_2.10-1.0.jar","basefunctions_2.11-0.1.0.jar","commons-codec-1.10.jar","treemap_2.10-0.1.0.jar","kafka_2.10-0.8.2.2.jar","kryo-2.21.jar","zkclient-0.3.jar","avro-1.7.4.jar","jetty-embedded-6.1.26-sources.jar","objenesis-1.2.jar","parboiled-core-1.1.7.jar","java-xmlbuilder-0.4.jar","bee-client_2.10-0.28.0.jar","config-1.2.1.jar","reflectasm-1.07-shaded.jar","kamanjautils_2.10-1.0.jar","migratefrom_v_1_2_2.10-1.0.jar","commons-logging-1.2.jar","migratebase-1.0.jar","commons-digester-1.8.jar","migrateto_v_1_3_2.10-1.0.jar","gson-2.3.1.jar","paranamer-2.6.jar","guava-16.0.1.jar","sqlserver_2.10-0.1.0.jar","installdriver_2.10-1.0.jar","slf4j-api-1.7.10.jar","clusterinstallerdriver-1.0.jar","asm-tree-4.0.jar","jetty-6.1.26.jar","commons-dbcp2-2.1.jar","hbase_2.10-0.1.0.jar","simpleenvcontextimpl_2.10-1.0.jar","jsp-api-2.1.jar","netty-3.7.0.Final.jar","json4s-native_2.11-3.2.9.jar","spray-client_2.10-1.3.3.jar","commons-dbcp-1.4.jar","htrace-core-3.1.0-incubating.jar","jaxb-api-2.2.2.jar","commons-io-2.4.jar","commons-math3-3.6.jar","guava-18.0.jar","metadata_2.10-1.0.jar","httpcore-4.2.4.jar","jpmmlfactoryofmodelinstancefactory_2.10-1.0.jar","spray-routing_2.10-1.3.3.jar","curator-framework-2.6.0.jar","scalap-2.11.0.jar","zkclient-0.6.jar","logback-classic-1.0.13.jar","heartbeat_2.10-0.1.0.jar","basetypes_2.10-0.1.0.jar","asm-4.0.jar","commons-net-3.1.jar","jackson-databind-2.3.1.jar","scala-actors-2.10.4.jar","scala-compiler-2.10.4.jar","kamanjabase_2.10-1.0.jar","jsr305-1.3.9.jar","basefunctions_2.10-0.1.0.jar","nodeinfoextract_2.10-1.0.jar","hbase-common-1.0.2.jar","transactionservice_2.10-0.1.0.jar","commons-compress-1.4.1.jar","shapeless_2.10-1.2.4.jar","storagebase_2.10-1.0.jar","scala-reflect-2.11.7.jar","migratefrom_v_1_1_2.10-1.0.jar","kamanjautils_2.11-1.0.jar","parboiled-scala_2.10-1.1.7.jar","commons-codec-1.4.jar","pmml-evaluator-1.2.9.jar","jsr305-3.0.0.jar","log4j-1.2.16.jar","jaxb-impl-2.2.3-1.jar","asm-commons-4.0.jar","netty-all-4.0.23.Final.jar","json4s-ast_2.11-3.2.9.jar","spray-io_2.10-1.3.3.jar","curator-client-2.6.0.jar","joda-convert-1.7.jar","json4s-ast_2.10-3.2.9.jar","log4j-core-2.4.1.jar","log4j-1.2.17.jar","kvinit_2.10-1.0.jar","metadataapi_2.10-1.0.jar","commons-codec-1.9.jar","netty-3.9.0.Final.jar","cassandra-driver-core-2.1.2.jar","protobuf-java-2.6.0.jar","shiro-core-1.2.3.jar","spray-testkit_2.10-1.3.3.jar","customudflib_2.11-1.0.jar","exceptions_2.11-1.0.jar","guava-19.0.jar","logback-classic-1.0.12.jar","getcomponent_2.10-1.0.jar","antlr-2.7.7.jar","jersey-json-1.9.jar","scala-library-2.10.4.jar","zookeeper-3.4.6.jar","httpclient-4.2.5.jar","pmml-model-1.2.9.jar","slf4j-log4j12-1.6.1.jar","utilsformodels_2.10-1.0.jar","servlet-api-2.5.20110712-sources.jar","camel-core-2.9.2.jar","json4s-core_2.11-3.2.9.jar","jettison-1.1.jar","voldemort-0.96.jar","cleanutil_2.10-1.0.jar","pmmlcompiler_2.10-1.0.jar","jackson-jaxrs-1.8.3.jar","scalap-2.10.0.jar","jcodings-1.0.8.jar","commons-beanutils-1.8.3.jar","commons-collections-3.2.1.jar","akka-testkit_2.10-2.3.9.jar","apacheds-kerberos-codec-2.0.0-M15.jar","kvbase_2.11-0.1.0.jar","spray-http_2.10-1.3.3.jar","simpleapacheshiroadapter_2.10-1.0.jar","jopt-simple-3.2.jar","heartbeat_2.11-0.1.0.jar","hashmap_2.10-0.1.0.jar","joda-time-2.8.2.jar","basetypes_2.11-0.1.0.jar","json4s-jackson_2.11-3.2.9.jar","scala-compiler-2.11.0.jar","commons-math3-3.1.1.jar","installdriverbase-1.0.jar","log4j-api-2.4.1.jar","scalatest_2.10-2.2.6.jar","jsontools-core-1.7-sources.jar","junit-4.12.jar","xz-1.0.jar","java-stub-server-0.12-sources.jar","jackson-core-2.3.1.jar","redisclient_2.10-2.13.jar","jersey-core-1.9.jar","auditadapters_2.10-1.0.jar","logback-core-1.0.13.jar","hadoop-annotations-2.7.1.jar","api-util-1.0.0-M20.jar","auditadapterbase_2.10-1.0.jar","commons-math-2.2.jar","jsch-0.1.42.jar","metrics-core-2.2.0.jar","hadoop-auth-2.7.1.jar","snappy-java-1.1.1.7.jar","scalatest_2.10-2.2.0.jar","metadataapiserviceclient_2.10-0.1.jar","google-collections-1.0.jar","joda-convert-1.6.jar","httpcore-4.1.2.jar","filesimpleinputoutputadapters_2.10-1.0.jar","interfacessamples_2.10-1.0.jar","pmml-schema-1.2.9.jar","jetty-util-6.1.26.jar","kafka-clients-0.8.2.2.jar","storagemanager_2.10-0.1.0.jar","stax-api-1.0-2.jar","akka-actor_2.10-2.3.2.jar","akka-actor_2.10-2.3.9.jar","commons-pool2-2.3.jar","jackson-xc-1.8.3.jar","hbase-annotations-1.0.2.jar","jackson-mapper-asl-1.9.13.jar","scalatest_2.10-2.2.4.jar","activation-1.1.jar","inputoutputadapterbase_2.10-1.0.jar","filedataconsumer_2.10-0.1.0.jar","json4s-native_2.10-3.2.9.jar","joda-time-2.9.1.jar","hbase-client-1.0.2.jar","log4j-1.2-api-2.4.1.jar","scala-2.10.0.jar","scala-library-2.11.7.jar","pmmlcompiler_2.11-1.0.jar","guava-16.0.1.jar","junit-3.8.1.jar","commons-lang-2.6.jar","mimepull-1.9.5.jar","json-simple-1.1.jar","kamanjamanager_2.10-1.0.jar","datadelimiters_2.10-1.0.jar","commons-digester-1.8.1.jar","controller_2.10-1.0.jar","spray-util_2.10-1.3.3.jar","spray-json_2.10-1.3.2.jar","zookeeperclient_2.10-1.0.jar","kvbase_2.10-0.1.0.jar","outputmsgdef_2.10-1.0.jar","commons-cli-1.2.jar","logback-core-1.0.12.jar","metadataapiservice_2.10-1.0.jar","api-asn1-api-1.0.0-M20.jar","config-1.2.0.jar","json4s-core_2.10-3.2.9.jar","zookeeperlistener_2.10-1.0.jar","apacheds-i18n-2.0.0-M15.jar","httpclient-4.1.2.jar","libthrift-0.9.2.jar","methodextractor_2.10-1.0.jar","commons-pool-1.6.jar","je-4.0.92.jar","asm-3.1.jar","jna-3.2.7.jar","curator-recipes-2.7.1.jar","servlet-api-2.5.20110712.jar","pmml-agent-1.2.9.jar","jackson-annotations-2.3.0.jar","commons-pool-1.5.4.jar","curator-framework-2.7.1.jar","pmmludfs_2.10-1.0.jar","mapdb-1.0.6.jar","gson-2.2.4.jar","minlog-1.2.jar","findbugs-annotations-1.3.9-1.jar","jetty-sslengine-6.1.26.jar","guava-14.0.1.jar","ExtDependencyLibs_2.11-1.4.0.jar", "KamanjaInternalDeps_2.11-1.4.0.jar", "ExtDependencyLibs2_2.11-1.4.0.jar")

    val depJars = (depJars1 diff jarsToBeExcluded)
    // If source is 2.10 and destination is 2.11, then only tranform this. otherwise just leave them as it is.
    if (_fromScalaVersion.equalsIgnoreCase("2.10") && _toScalaVersion.equalsIgnoreCase("2.11")) {
      val depJarsMap = Map("scalap-2.10.0.jar" -> "scalap-2.11.0.jar", "kvbase_2.10-0.1.0.jar" -> "kvbase_2.11-0.1.0.jar", "kamanjautils_2.10-1.0.jar" -> "kamanjautils_2.11-1.0.jar",
        "kamanjabase_2.10-1.0.jar" -> "kamanjabase_2.11-1.0.jar", "customudflib_2.10-1.0.jar" -> "customudflib_2.11-1.0.jar", "pmmlcompiler_2.10-1.0.jar" -> "pmmlcompiler_2.11-1.0.jar",
        "basetypes_2.10-0.1.0.jar" -> "basetypes_2.11-0.1.0.jar", "basefunctions_2.10-0.1.0.jar" -> "basefunctions_2.11-0.1.0.jar", "json4s-core_2.10-3.2.9.jar" -> "json4s-core_2.11-3.2.9.jar",
        "json4s-jackson_2.10-3.2.9.jar" -> "json4s-jackson_2.11-3.2.9.jar", "pmmlruntime_2.10-1.0.jar" -> "pmmlruntime_2.11-1.0.jar", "pmmludfs_2.10-1.0.jar" -> "pmmludfs_2.11-1.0.jar",
        "datadelimiters_2.10-1.0.jar" -> "datadelimiters_2.11-1.0.jar", "metadata_2.10-1.0.jar" -> "metadata_2.11-1.0.jar", "exceptions_2.10-1.0.jar" -> "exceptions_2.11-1.0.jar",
        "json4s-ast_2.10-3.2.9.jar" -> "json4s-ast_2.11-3.2.9.jar", "json4s-native_2.10-3.2.9.jar" -> "json4s-native_2.11-3.2.9.jar", "bootstrap_2.10-1.0.jar" -> "bootstrap_2.11-1.0.jar",
        "messagedef_2.10-1.0.jar" -> "messagedef_2.11-1.0.jar", "guava-16.0.1.jar" -> "guava-16.0.1.jar", "guava-18.0.jar" -> "guava-16.0.1.jar", "guava-19.0.jar" -> "guava-16.0.1.jar")

      var newDeps = depJars.map(d => {
        if (d.startsWith("scala-reflect-2.10")) {
          "scala-reflect-2.11.7.jar"
        } else if (d.startsWith("scala-library-2.10")) {
          "scala-library-2.11.7.jar"
        } else if (d.startsWith("scala-compiler-2.10")) {
          "scala-compiler-2.11.7.jar"
        } else {
          depJarsMap.getOrElse(d, d)
        }
      })

      newDeps = List(s"ExtDependencyLibs_2.11-${curVersion}.jar", s"KamanjaInternalDeps_2.11-${curVersion}.jar", s"ExtDependencyLibs2_2.11-${curVersion}.jar") ::: newDeps

      newDeps
    } else {
      val depJarsMap = Map("guava-16.0.1.jar" -> "guava-16.0.1.jar", "guava-18.0.jar" -> "guava-16.0.1.jar", "guava-19.0.jar" -> "guava-16.0.1.jar")

      var newDeps = depJars.map(d => {
        depJarsMap.getOrElse(d, d)
      })

      if( _toScalaVersion.equalsIgnoreCase("2.11") ){
	newDeps = List(s"ExtDependencyLibs_2.11-${curVersion}.jar", s"KamanjaInternalDeps_2.11-${curVersion}.jar", s"ExtDependencyLibs2_2.11-${curVersion}.jar") ::: newDeps
      }
      else{
	newDeps = List(s"ExtDependencyLibs_2.10-${curVersion}.jar", s"KamanjaInternalDeps_2.10-${curVersion}.jar", s"ExtDependencyLibs2_2.10-${curVersion}.jar") ::: newDeps
      }
      newDeps
    }
  }

  private def ProcessObject(mdObjs: ArrayBuffer[(String, Map[String, Any])]): Unit = {
    try {
      mdObjs.foreach(mdObj => {
        val objType = mdObj._1
        var dispkey = ""
        var ver = ""

        try {
          val namespace = mdObj._2.getOrElse("NameSpace", "").toString.trim()
          val name = mdObj._2.getOrElse("Name", "").toString.trim()
          dispkey = (namespace + "." + name).toLowerCase
          ver = mdObj._2.getOrElse("Version", "0.0.1").toString
          val objFormat = mdObj._2.getOrElse("ObjectFormat", "").toString

          objType match {
            case "ModelDef" => {
              val mdlType = mdObj._2.getOrElse("ModelType", "").toString
              val mdlDefStr = mdObj._2.getOrElse("ObjectDefinition", "").toString

              logger.info("Adding model:" + dispkey + ", ModelType:" + mdlType + ", ObjectFormat:" + objFormat)

              if (_sourceVersion.equalsIgnoreCase("1.1") ||
                _sourceVersion.equalsIgnoreCase("1.2") ||
                _sourceVersion.equalsIgnoreCase("1.3")) {
                if ((objFormat.equalsIgnoreCase("JAVA")) || (objFormat.equalsIgnoreCase("scala"))) {
                  val mdlInfo = parse(mdlDefStr).values.asInstanceOf[Map[String, Any]]
                  val defStr = mdlInfo.getOrElse(ModelCompilationConstants.SOURCECODE, "").asInstanceOf[String]
                  // val phyName = mdlInfo.getOrElse(ModelCompilationConstants.PHYSICALNAME, "").asInstanceOf[String]
                  val deps = DepJars(mdlInfo.getOrElse(ModelCompilationConstants.DEPENDENCIES, List[String]()).asInstanceOf[List[String]])
                  val typs = mdlInfo.getOrElse(ModelCompilationConstants.TYPES_DEPENDENCIES, List[String]()).asInstanceOf[List[String]]
                  //returns current time as a unique number
                  val uniqueId = getUniqueId
                  val cfgnm = "migrationmodelconfig_from_" + _sourceVersion.replace('.', '_') + "_to_1_5_0_" + uniqueId.toString;

                  val mdlConfig = (cfgnm ->
                    ("Dependencies" -> deps) ~
                      ("MessageAndContainers" -> typs))

                  var failed = false

                  try {
                    val mdlCfgStr = compact(render(mdlConfig))
                    logger.debug("Temporary Model Config:" + mdlCfgStr)
                    val retRes = getMetadataAPI.UploadModelsConfig(mdlCfgStr, defaultUserId, "configuration", true)
                    failed = isFailedStatus(retRes)

                    if (failed == false) {
                      val retRes1 = getMetadataAPI.AddModel(MetadataAPI.ModelType.fromString(objFormat), defStr, defaultUserId, _tenantId, Some((defaultUserId.get + "." + cfgnm).toLowerCase), Some(ver), None, None, None, None)
                      failed = isFailedStatus(retRes1)
                    }
                  } catch {
                    case e: Exception => {
                      logger.error("Failed to add model:" + dispkey, e)
                      failed = true
                    }
                  }

                  if (failed) {
                    var defFl = _unhandledMetadataDumpDir + "/" + objFormat + "_mdldef_" + dispkey + "." + ver + "." + objFormat.toLowerCase()
                    var jsonFl = _unhandledMetadataDumpDir + "/" + objFormat + "_mdlinfo_" + dispkey + "." + ver + ".json"

                    val dumpMdlInfoStr = ("ModelInfo" ->
                      ("Dependencies" -> deps) ~
                        ("MessageAndContainers" -> typs) ~
                        ("ModelType" -> mdlType) ~
                        ("ObjectFormat" -> objFormat) ~
                        ("ModelDefinition" -> defStr) ~
                        ("NameSpace" -> namespace) ~
                        ("Name" -> name) ~
                        ("Version" -> ver))

                    WriteStringToFile(defFl, defStr)
                    WriteStringToFile(jsonFl, compact(render(dumpMdlInfoStr)))

                    val msgStr = ("%s type model failed to migrate. Model %s definition is dumped into %s, more model information dumped to %s.".format(objFormat, dispkey, defFl, jsonFl))
                    logger.error(msgStr)
                    _flCurMigrationSummary.println(msgStr)
                    _flCurMigrationSummary.flush()
                    AddedFailedMetadataKey(objType, dispkey, ver)
                  }
                } else if (objFormat.equalsIgnoreCase("XML")) {
                  var defFl = _unhandledMetadataDumpDir + "/kPMML_mdldef_" + dispkey + "." + ver + "." + objFormat.toLowerCase()
                  var failed = false

                  try {
                    val retRes = getMetadataAPI.AddModel(MetadataAPI.ModelType.fromString("kpmml"), mdlDefStr, defaultUserId, _tenantId, Some(dispkey), Some(ver), None, None, None, None)
                    logger.info("AddModel: Response => " + retRes)
                    failed = isFailedStatus(retRes)
                  } catch {
                    case e: Exception => {
                      logger.error("Failed to add model:" + dispkey, e)
                      failed = true
                    }
                  }
                  if (failed) {
                    WriteStringToFile(defFl, mdlDefStr)
                    val msgStr = ("kPMML type model failed to migrate. Model %s definition is dumped into %s.".format(dispkey, defFl))
                    logger.error(msgStr)
                    _flCurMigrationSummary.println(msgStr)
                    _flCurMigrationSummary.flush()
                    AddedFailedMetadataKey(objType, dispkey, ver)
                  }
                }
              } else {
                logger.error("Not supported any other source migration version other than 1.1 and 1.2")
              }
            }
            case "MessageDef" => {
              val msgDefStr = mdObj._2.getOrElse("ObjectDefinition", "").toString
              if (msgDefStr != null && msgDefStr.size > 0) {
                logger.info("Adding the message:" + dispkey)
                var defFl = _unhandledMetadataDumpDir + "/message_" + dispkey + "." + ver + "." + objFormat.toLowerCase()
                var failed = false

                try {
                  val retRes = getMetadataAPI.AddMessage(msgDefStr, "JSON", defaultUserId, _tenantId, None)
                  logger.info("AddMessage: Response => " + retRes)
                  failed = isFailedStatus(retRes)
                } catch {
                  case e: Exception => {
                    logger.error("Failed to add message:" + dispkey, e)
                    failed = true
                  }
                }
                if (failed) {
                  WriteStringToFile(defFl, msgDefStr)
                  val msgStr = ("Message failed to migrate. Message %s definition is dumped into %s.".format(dispkey, defFl))
                  logger.error(msgStr)
                  _flCurMigrationSummary.println(msgStr)
                  _flCurMigrationSummary.flush()
                  AddedFailedMessageOrContainer(dispkey)
                  AddedFailedMetadataKey(objType, dispkey, ver)
                }
              } else {
                logger.debug("Bootstrap object. Ignore it")
              }
            }
            case "ContainerDef" => {
              logger.debug("Adding the container:" + dispkey)
              val contDefStr = mdObj._2.getOrElse("ObjectDefinition", "").toString
              if (contDefStr != null && contDefStr.size > 0) {
                logger.info("Adding the message: name of the object =>  " + dispkey)
                var defFl = _unhandledMetadataDumpDir + "/container_" + dispkey + "." + ver + "." + objFormat.toLowerCase()
                var failed = false

                try {
                  val retRes = getMetadataAPI.AddContainer(contDefStr, "JSON", defaultUserId, _tenantId, None)
                  logger.info("AddContainer: Response => " + retRes)

                  failed = isFailedStatus(retRes)
                } catch {
                  case e: Exception => {
                    logger.error("Failed to add container:" + dispkey, e)
                    failed = true
                  }
                }
                if (failed) {
                  WriteStringToFile(defFl, contDefStr)
                  val msgStr = ("Container failed to migrate. Container %s definition is dumped into %s.".format(dispkey, defFl))
                  logger.error(msgStr)
                  _flCurMigrationSummary.println(msgStr)
                  _flCurMigrationSummary.flush()
                  AddedFailedMessageOrContainer(dispkey)
                  AddedFailedMetadataKey(objType, dispkey, ver)
                }
              } else {
                logger.debug("Bootstrap object. Ignore it")
              }
            }
            case "ConfigDef" => {
              val mdlCfg = mdObj._2.getOrElse("ObjectDefinition", "").toString
              if (mdlCfg != null && mdlCfg.size > 0) {
                logger.debug("Adding model config:" + dispkey)
                var defFl = _unhandledMetadataDumpDir + "/ModelConfig_" + dispkey + "." + ver + "." + objFormat.toLowerCase()
                var failed = false

                try {

                  val cfgmap = parse(mdlCfg).values.asInstanceOf[Map[String, Any]]
                  val changedCfg = cfgmap.map(kv => {
                    val key = kv._1
                    val mdl = kv._2.asInstanceOf[Map[String, Any]]
                    val deps1 = mdl.getOrElse(ModelCompilationConstants.DEPENDENCIES, null)
                    val typs1 = mdl.getOrElse(ModelCompilationConstants.TYPES_DEPENDENCIES, null)
                    val phyNm = mdl.getOrElse(ModelCompilationConstants.PHYSICALNAME, "").toString()

                    val deps = if (deps1 != null) DepJars(deps1.asInstanceOf[List[String]]) else List[String]()
                    val typs = if (typs1 != null) typs1.asInstanceOf[List[String]] else List[String]()

                    (key, Map(ModelCompilationConstants.DEPENDENCIES -> deps, ModelCompilationConstants.TYPES_DEPENDENCIES -> typs, ModelCompilationConstants.PHYSICALNAME -> phyNm))
                  })

                  implicit val jsonFormats: Formats = DefaultFormats
                  val newMdlCfgStr = org.json4s.jackson.Serialization.write(changedCfg)
                  val retRes = getMetadataAPI.UploadModelsConfig(newMdlCfgStr, Some[String](namespace), null) // Considering namespace as userid
                  logger.info("AddConfig: Response => " + retRes)
                  failed = isFailedStatus(retRes)
                } catch {
                  case e: Exception => {
                    logger.error("Failed to add model config:" + dispkey, e)
                    failed = true
                  }
                }
                if (failed) {
                  WriteStringToFile(defFl, mdlCfg)
                  val msgStr = ("Model Config failed to migrate. Model Config %s definition is dumped into %s.".format(dispkey, defFl))
                  logger.error(msgStr)
                  _flCurMigrationSummary.println(msgStr)
                  _flCurMigrationSummary.flush()
                  AddedFailedMetadataKey(objType, dispkey, ver)
                }
              }
            }
            case "FunctionDef" => {
              val fnCfg = mdObj._2.getOrElse("ObjectDefinition", "").toString
              if (fnCfg != null && fnCfg.size > 0) {
                logger.debug("Adding model config:" + dispkey)
                var defFl = _unhandledMetadataDumpDir + "/Function_" + dispkey + "." + ver + "." + objFormat.toLowerCase()
                var failed = false

                try {
                  val retRes = getMetadataAPI.AddFunctions(fnCfg, "JSON", defaultUserId)
                  logger.info("AddFunction: Response => " + retRes)
                  failed = isFailedStatus(retRes)
                } catch {
                  case e: Exception => {
                    logger.error("Failed to add function:" + dispkey, e)
                    failed = true
                  }
                }
                if (failed) {
                  WriteStringToFile(defFl, fnCfg)
                  val msgStr = ("Function failed to migrate. Function %s definition is dumped into %s.".format(dispkey, defFl))
                  logger.error(msgStr)
                  _flCurMigrationSummary.println(msgStr)
                  _flCurMigrationSummary.flush()
                  AddedFailedMetadataKey(objType, dispkey, ver)
                }
              }
            }
            /*
            case "JarDef" => {
              logger.debug("Jar")
              logger.debug("Adding Jar:" + dispkey)
              //FIXME:: Yet to handle
              logger.error("Not yet handled migrating JarDef " + objType)
            }
            */
            /*
            case "AttributeDef" => {
              logger.debug("Adding the attribute: name of the object =>  " + dispkey)
            }
            case "ScalarTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
            case "ArrayTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
            case "ArrayBufTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
            case "ListTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
            case "QueueTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
            case "SetTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
            case "TreeSetTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
            case "SortedSetTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
            case "MapTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
            case "ImmutableMapTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
            case "HashMapTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
            case "TupleTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
            case "ContainerTypeDef" => {
              logger.debug("Adding the Type: name of the object =>  " + dispkey)
            }
  */
            case _ => {
              val msgStr = ("Object type %s of key %s did not handle in migrate. Not captured any information related to this.".format(objType, dispkey))
              logger.error(msgStr)
              _flCurMigrationSummary.println(msgStr)
              _flCurMigrationSummary.flush()
              AddedFailedMetadataKey(objType, dispkey, ver)
            }
          }
        } catch {
          case e: Exception => AddMdObjToGlobalException(mdObj, "Failed to add metadata of type " + objType, e); AddedFailedMetadataKey(objType, dispkey, ver)
          case e: Throwable => AddMdObjToGlobalException(mdObj, "Failed to add metadata of type " + objType, e); AddedFailedMetadataKey(objType, dispkey, ver)
        }
      })
    } catch {
      case e: Exception => throw e
      case e: Throwable => throw e
    }
  }

  private var msgsContainersFailed = ArrayBuffer[String]()
  private var msgsContainersFailLock = new Object
  private var mdFailed = ArrayBuffer[FailedMetadataKey]()
  private var mdFailLock = new Object

  private def AddedFailedMessageOrContainer(con: String): Unit = msgsContainersFailLock.synchronized {
    if (con != null)
      msgsContainersFailed += con
  }

  private def AddedFailedMetadataKey(objType: String, mdKey: String, ver: String): Unit = mdFailLock.synchronized {
    if (objType != null && mdKey != null && ver != null)
      mdFailed += new FailedMetadataKey(objType, mdKey, ver)
  }

  private def GetFailedMessageOrContainer: Array[String] = msgsContainersFailLock.synchronized {
    return msgsContainersFailed.toArray
  }

  override def getFailedMetadataKeys(): Array[FailedMetadataKey] = mdFailLock.synchronized {
    mdFailed.toArray
  }

  def getCCParams(cc: Product): scala.collection.mutable.Map[String, Any] = {
    val values = cc.productIterator
    val m = cc.getClass.getDeclaredFields.map(_.getName -> values.next).toMap
    scala.collection.mutable.Map(m.toSeq: _*)
  }

  private def BuildHierarchyAndProcessObjectsParallel(mdObjs: ArrayBuffer[(String, Map[String, Any])], errorStr: String): Unit = {
    val objsToRunNow = ArrayBuffer[(String, Map[String, Any])]()
    val objsToRunLater = ArrayBuffer[(String, Map[String, Any])]()
    val yetToRun = scala.collection.mutable.Map[String, Int]() // Jarname to Position. Not expecting same jar for more than one element.

    // Take which does not have any more dependencies
    for (i <- 0 until mdObjs.size) {
      val o = mdObjs(i)
      val jarName = o._2.getOrElse("JarName", "").toString.trim
      if (jarName.isEmpty) {
        objsToRunNow += o
      } else {
        yetToRun(jarName) = i
        objsToRunLater += o
      }
    }

    do {
      // Take which does not have any more dependencies
      val remList = objsToRunLater.toArray
      objsToRunLater.clear

      // Remaining list to run
      remList.foreach(o => {
        val dependantJars = o._2.getOrElse("DependantJars", List[String]()).asInstanceOf[List[String]].toArray.map(j => j.trim).filter(j => (j.isEmpty == false))

        var idx = dependantJars.size - 1
        var yetToBuildDeps = false
        while (idx >= 0 && ! yetToBuildDeps) {
          yetToBuildDeps = yetToRun.contains(dependantJars(idx))
          idx = idx - 1
        }

        if (yetToBuildDeps) {
          objsToRunLater += o
        } else {
          objsToRunNow += o
        }
      })

      if (objsToRunLater.size > 0 && objsToRunNow.size == 0) {
        // Nothing found to be run in this run, but we have some more to run
        objsToRunNow ++= objsToRunLater
        objsToRunLater.clear
      }

      // Process all objs to run now
      ProcessMdObjectsParallel(objsToRunNow, errorStr)

      // Remove all jars executed now
      objsToRunNow.foreach(o => {
        val jarName = o._2.getOrElse("JarName", "").toString.trim
        yetToRun.remove(jarName)
      })

      // Clear objects ran now
      objsToRunNow.clear
    } while (objsToRunLater.size > 0)
  }

  private def ProcessMdObjectsParallel(mdObjs: ArrayBuffer[(String, Map[String, Any])], errorStr: String): Unit = {
    if (mdObjs.length > 0) {
      var executor: ExecutorService = null
      try {
        executor = Executors.newFixedThreadPool(if (_parallelDegree <= 1) 1 else _parallelDegree)
        mdObjs.foreach(obj => {
          executor.execute(new Runnable() {
            override def run() = {
              try {
                ProcessObject(ArrayBuffer(obj))
              } catch {
                case e: Exception => AddMdObjToGlobalException(obj, errorStr, e)
                case e: Throwable => AddMdObjToGlobalException(obj, errorStr, e)
              }
            }
          })
        })
        executor.shutdown();
        try {
          executor.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS);
        } catch {
          case e: Exception => {
            logger.debug("Failed", e)
          }
        }
      } catch {
        case e: Exception => AddToGlobalException("Failed to add metadata", e)
        case e: Throwable => AddToGlobalException("Failed to add metadata", e)
      }

      if (executor != null) {
        executor.shutdown();
        try {
          executor.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS);
        } catch {
          case e: Exception => AddToGlobalException("Failed to add metadata", e)
          case e: Throwable => AddToGlobalException("Failed to add metadata", e)
        }
      }

      if (LogGlobalException) {
        throw new Exception("Failed to add metadata")
      }
    }
  }

  private def AddMsgBindings: Unit = {
    // handle adapterMessageBindings here
    try {
      implicit val jsonFormats: Formats = DefaultFormats
      var ambsAsJson = Source.fromFile(_adapterMessageBindings.get).mkString
      val ambs1 = parse(ambsAsJson).extract[Array[adapterMessageBinding]]
      val ambsMap: Array[scala.collection.mutable.Map[String, Any]] = ambs1.map(amb => {
        val ambMap = getCCParams(amb); ambMap
      })
      val retRes = AdapterMessageBindingUtils.AddAdapterMessageBinding(ambsMap.toList, defaultUserId)
      logger.info(retRes)
      val failed = isFailedStatus(retRes)
      if (failed == true) {
        logger.error("Failed to add adapter-message-bindings" + retRes)
        logger.error("******************************************************Make sure you add message bindings before you start clustesr******************************************************")
//        throw new Exception("Failed to add adapter-message-bindings")
      }
    } catch {
      case e: Throwable => {
        logger.error("Failed to add adapter-message-bindings", e)
        logger.error("******************************************************Make sure you add message bindings before you start clustesr******************************************************")
//        throw new Exception("Failed to add adapter-message-bindings")
      }
    }
  }

  override def getMessagesAndContainers(allMetadataElemsJson: Array[MetadataFormat], uploadClusterConfig: Boolean, excludeMetadata: Array[String]): java.util.List[String] = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")

    val excludedMetadataTypes = if (excludeMetadata != null && excludeMetadata.length > 0) excludeMetadata.map(t => t.toLowerCase.trim).toSet else Set[String]()

    // Order metadata to add in the given order.
    // First get all the message & containers And also the excluded types we automatically add when we add messages & containers
    val allTemp = ArrayBuffer[(String, Map[String, Any])]()
    val types = ArrayBuffer[(String, Map[String, Any])]()
    val messages = ArrayBuffer[(String, Map[String, Any])]()
    val containers = ArrayBuffer[(String, Map[String, Any])]()
    val functions = ArrayBuffer[(String, Map[String, Any])]()
    val mdlConfig = ArrayBuffer[(String, Map[String, Any])]()
    val models = ArrayBuffer[(String, Map[String, Any])]()
    val jarDef = ArrayBuffer[(String, Map[String, Any])]()
    val configDef = ArrayBuffer[(String, Map[String, Any])]()
    val typesToIgnore = scala.collection.mutable.Set[String]()

    val addedMessagesContainers: java.util.List[String] = new java.util.ArrayList[String]()

    allMetadataElemsJson.foreach(mdf => {
      val json = parse(mdf.objDataInJson)
      val jsonObjMap = json.values.asInstanceOf[Map[String, Any]]

      val isActiveStr = jsonObjMap.getOrElse("IsActive", "").toString.trim()
      if (isActiveStr.size > 0) {
        val isActive = jsonObjMap.getOrElse("IsActive", "").toString.trim().toBoolean
        if (isActive) {
          if (mdf.objType == "MessageDef") {

            val namespace = jsonObjMap.getOrElse("NameSpace", "").toString.trim()
            val name = jsonObjMap.getOrElse("Name", "").toString.trim()

            typesToIgnore += (namespace + ".arrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".arraybufferof" + name).toLowerCase
            typesToIgnore += (namespace + ".sortedsetof" + name).toLowerCase
            typesToIgnore += (namespace + ".immutablemapofintarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".immutablemapofstringarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".arrayofarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".mapofstringarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".mapofintarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".setof" + name).toLowerCase
            typesToIgnore += (namespace + ".treesetof" + name).toLowerCase

            if (excludedMetadataTypes.contains(mdf.objType.toLowerCase()) == false) {
              messages += ((mdf.objType, jsonObjMap))
              addedMessagesContainers.add(namespace + "." + name)
            }
          } else if (mdf.objType == "ContainerDef") {

            val namespace = jsonObjMap.getOrElse("NameSpace", "").toString.trim()
            val name = jsonObjMap.getOrElse("Name", "").toString.trim()

            typesToIgnore += (namespace + ".arrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".arraybufferof" + name).toLowerCase
            typesToIgnore += (namespace + ".sortedsetof" + name).toLowerCase
            typesToIgnore += (namespace + ".immutablemapofintarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".immutablemapofstringarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".arrayofarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".mapofstringarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".mapofintarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".setof" + name).toLowerCase
            typesToIgnore += (namespace + ".treesetof" + name).toLowerCase

            if (excludedMetadataTypes.contains(mdf.objType.toLowerCase()) == false) {
              containers += ((mdf.objType, jsonObjMap))
              addedMessagesContainers.add(namespace + "." + name)
            }
          } else {
            if (excludedMetadataTypes.contains(mdf.objType.toLowerCase()) == false) {
              allTemp += ((mdf.objType, jsonObjMap))
            }
          }
        }
      }
    })
    addedMessagesContainers
  }

  private def getObjectName(tag: String, jsonObjMap: Map[String, Any]): String = {
    val cont = jsonObjMap.getOrElse(tag, null).asInstanceOf[Map[String,Any]]
    var objectName = ""
    if( cont != null ){
      val namespace = cont.getOrElse("NameSpace", "").toString.trim()
      val name = cont.getOrElse("Name", "").toString.trim()
      objectName = (namespace + "." + name).toLowerCase
    }
    else{
      logger.debug("Failed to grab " + tag + " from " + jsonObjMap)
    }
    objectName
  }    

  private def getObjectVersion(tag: String, jsonObjMap: Map[String, Any]): String = {
    val cont = jsonObjMap.getOrElse(tag, null).asInstanceOf[Map[String,Any]]
    var version = ""
    if( cont != null ){
      val nTypes = cont.getOrElse("NumericTypes", null).asInstanceOf[Map[String,Any]]
      if( nTypes != null ){
	version = nTypes.getOrElse("Version", "").toString.trim()
      }
      else{
	logger.debug("Failed to grab " + tag + " from the map " + cont)
      }
    }
    else{
      logger.debug("Failed to grab " + tag + " from the map " + jsonObjMap)
    }
    version
  }

  def getContainersFromModelConfig(cfgmap: Map[String,Any],
				   containerSet: scala.collection.mutable.Set[String]): Array[String] = {
    var containerList = List[String]()
    var msgsAndContainers = MessageAndContainerUtils.getMessagesAndContainers(cfgmap)
    if( msgsAndContainers.length > 0 ){
      msgsAndContainers.foreach(msg => {
	logger.debug("checking the message/container " + msg)
	if( containerSet.contains(msg) ){
	  logger.debug("The " + msg + " is a container")
	  containerList = msg :: containerList
	}
	else{
	  logger.debug("The " + msg + " is not a container")
	}
      })
    }
    else{
      logger.debug("getMessagesAndContainers: No types for the model config " + cfgmap)
    }
    containerList.toArray
  }

  private def update140ModelsInPlace(mdObjs: ArrayBuffer[(String,String,Map[String, Any])],
				     containerSet: scala.collection.mutable.Set[String] ): Unit = {
    try {
      logger.info("Models to be Updated => " + mdObjs.length)
      mdObjs.foreach(mdObj => {
        val objType = mdObj._1
	val jsonObjMap = mdObj._3

	val mObj = (objType,jsonObjMap)

	logger.debug("Model Json => " + jsonObjMap)
	val modelName = getObjectName("Model",jsonObjMap)
	val modelVer  = getObjectVersion("Model",jsonObjMap)
	logger.info("Updating model, modelName => " + modelName + ",modelVer => " + modelVer)

        try {
	  var mdStrBefore:String = mdObj._2
	  logger.debug("Model Json Before Update => " + mdStrBefore)
          val md = MetadataAPISerialization.deserializeMetadata(mdStrBefore).asInstanceOf[ModelDef]
	  // Add depContainers to the model first
	  logger.debug("Parsing ModelConfig : " + md.modelConfig)
	  var cfgmap = parse(md.modelConfig).values.asInstanceOf[Map[String, Any]]
	  logger.debug("Count of objects in cfgmap : " + cfgmap.keys.size)

	  var depContainers = List[String]()
	  var containers = getContainersFromModelConfig(cfgmap,containerSet)
	  logger.debug("containers => " + containers)
	  depContainers = depContainers ::: containers.toList
	  logger.debug("depContainers => " + depContainers)
	  md.depContainers = depContainers.toArray

	  // Filter 1.4.0 fat jars now
	  val depJars1 = md.DependencyJarNames
	  val jarsToBeExcluded = List("ExtDependencyLibs_2.11-1.4.0.jar", "KamanjaInternalDeps_2.11-1.4.0.jar", "ExtDependencyLibs2_2.11-1.4.0.jar")
	  var depJars = (depJars1 diff jarsToBeExcluded)
	  md.dependencyJarNames = depJars.toArray
	  var mdStrAfter = MetadataAPISerialization.serializeObjectToJson(md)
	  mdStrAfter = mdStrAfter.replaceAll("ExtDependencyLibs_2.11-1.4.0.jar",s"ExtDependencyLibs_2.11-${curVersion}.jar")
	  mdStrAfter = mdStrAfter.replaceAll("ExtDependencyLibs2_2.11-1.4.0.jar",s"ExtDependencyLibs2_2.11-${curVersion}.jar")
	  mdStrAfter = mdStrAfter.replaceAll("KamanjaInternalDeps_2.11-1.4.0.jar",s"KamanjaInternalDeps_2.11-${curVersion}.jar")
	  mdStrAfter = mdStrAfter.replaceAll("ExtDependencyLibs_2.11-1.4.1.jar",s"ExtDependencyLibs_2.11-${curVersion}.jar")
	  mdStrAfter = mdStrAfter.replaceAll("ExtDependencyLibs2_2.11-1.4.1.jar",s"ExtDependencyLibs2_2.11-${curVersion}.jar")
	  mdStrAfter = mdStrAfter.replaceAll("KamanjaInternalDeps_2.11-1.4.1.jar",s"KamanjaInternalDeps_2.11-${curVersion}.jar")
	  logger.debug("Model Json Afer Update => " + mdStrAfter)
	  // save updated mdStr
          val md1 = MetadataAPISerialization.deserializeMetadata(mdStrAfter).asInstanceOf[ModelDef]
	  getMetadataAPI.UpdateObjectInDB(md1)
        } catch {
          case e: Exception => AddMdObjToGlobalException(mObj, "Failed to add metadata of type " + objType, e); AddedFailedMetadataKey(objType, modelName, modelVer)
          case e: Throwable => AddMdObjToGlobalException(mObj, "Failed to add metadata of type " + objType, e); AddedFailedMetadataKey(objType, modelName, modelVer)
        }
      })
    } catch {
      case e: Exception => throw e
      case e: Throwable => throw e
    }
  }

  def update14ObjectsInPlace(allMetadataElemsJson: Array[MetadataFormat]): java.util.List[String] = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")

    // Order metadata to add in the given order.
    // First get all the message & containers And also the excluded types we automatically add when we add messages & containers
    val models = ArrayBuffer[(String, String,Map[String, Any])]()
    val addedMessagesContainers: java.util.List[String] = new java.util.ArrayList[String]()
    var containerSet: scala.collection.mutable.Set[String] = scala.collection.mutable.Set[String]()

    logger.info("AllMetadataElemJson row count => " + allMetadataElemsJson.length);
    allMetadataElemsJson.foreach(mdf => {
      val json = parse(mdf.objDataInJson)
      val jsonObjMap = json.values.asInstanceOf[Map[String, Any]]
      logger.debug("objType => " + mdf.objType)
      if (mdf.objType.equalsIgnoreCase("ModelDef")) {
	logger.debug("Found a model object to be updated")
        models += ((mdf.objType,mdf.objDataInJson,jsonObjMap))
      }
      else if (mdf.objType.equalsIgnoreCase("ContainerDef")) {
	logger.debug("Container json => " + jsonObjMap)
	val contName = getObjectName("Container",jsonObjMap)
	logger.debug("Found a container object " + contName)
        containerSet.add(contName)
      }
    })

    // Open OpenDbStore
    getMetadataAPI.OpenDbStore(_jarPaths, _metaDataStoreInfo)
    val cfgStr = Source.fromFile(_clusterConfigFile).mkString
    logger.info("Uploading configuration:" + cfgStr)
    getMetadataAPI.UploadConfig(cfgStr, defaultUserId, "ClusterConfig")
    logger.info("Updating Models")
    update140ModelsInPlace(models,containerSet)
    return addedMessagesContainers
  }

  override def addMetadata(allMetadataElemsJson: Array[MetadataFormat], uploadClusterConfig: Boolean, excludeMetadata: Array[String]): java.util.List[String] = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")

    // If the source version is 1.4 (1.4.0), then we take a different route here
    // In this route, we just update model objects and config objects
    // In case of model objects we remove the dependency of the 1.4.0 fat jars
    // In case of config objects, make adapters depend on ${curVersion} fat jars
    // instead of 1.4.0 or 1.4.1 fat jars.
    if( _sourceVersion == "1.4" ||  _sourceVersion == "1.4.1" ){
      var msgs = update14ObjectsInPlace(allMetadataElemsJson)
      return msgs
    }

    val excludedMetadataTypes = if (excludeMetadata != null && excludeMetadata.length > 0) excludeMetadata.map(t => t.toLowerCase.trim).toSet else Set[String]()

    // Order metadata to add in the given order.
    // First get all the message & containers And also the excluded types we automatically add when we add messages & containers
    val allTemp = ArrayBuffer[(String, Map[String, Any])]()
    val types = ArrayBuffer[(String, Map[String, Any])]()
    val messages = ArrayBuffer[(String, Map[String, Any])]()
    val containers = ArrayBuffer[(String, Map[String, Any])]()
    val functions = ArrayBuffer[(String, Map[String, Any])]()
    val mdlConfig = ArrayBuffer[(String, Map[String, Any])]()
    val models = ArrayBuffer[(String, Map[String, Any])]()
    val jarDef = ArrayBuffer[(String, Map[String, Any])]()
    val configDef = ArrayBuffer[(String, Map[String, Any])]()
    val typesToIgnore = scala.collection.mutable.Set[String]()

    val addedMessagesContainers: java.util.List[String] = new java.util.ArrayList[String]()

    logger.info("AllMetadataElemJson row count => " + allMetadataElemsJson.length);
    allMetadataElemsJson.foreach(mdf => {
      val json = parse(mdf.objDataInJson)
      val jsonObjMap = json.values.asInstanceOf[Map[String, Any]]

      val isActiveStr = jsonObjMap.getOrElse("IsActive", "").toString.trim()
      if (isActiveStr.size > 0) {
        val isActive = jsonObjMap.getOrElse("IsActive", "").toString.trim().toBoolean
        if (isActive) {
          if (mdf.objType == "MessageDef") {

            val namespace = jsonObjMap.getOrElse("NameSpace", "").toString.trim()
            val name = jsonObjMap.getOrElse("Name", "").toString.trim()

            typesToIgnore += (namespace + ".arrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".arraybufferof" + name).toLowerCase
            typesToIgnore += (namespace + ".sortedsetof" + name).toLowerCase
            typesToIgnore += (namespace + ".immutablemapofintarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".immutablemapofstringarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".arrayofarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".mapofstringarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".mapofintarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".setof" + name).toLowerCase
            typesToIgnore += (namespace + ".treesetof" + name).toLowerCase

            if (excludedMetadataTypes.contains(mdf.objType.toLowerCase()) == false) {
              messages += ((mdf.objType, jsonObjMap))
              addedMessagesContainers.add(namespace + "." + name)
            }
          } else if (mdf.objType == "ContainerDef") {

            val namespace = jsonObjMap.getOrElse("NameSpace", "").toString.trim()
            val name = jsonObjMap.getOrElse("Name", "").toString.trim()

            typesToIgnore += (namespace + ".arrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".arraybufferof" + name).toLowerCase
            typesToIgnore += (namespace + ".sortedsetof" + name).toLowerCase
            typesToIgnore += (namespace + ".immutablemapofintarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".immutablemapofstringarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".arrayofarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".mapofstringarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".mapofintarrayof" + name).toLowerCase
            typesToIgnore += (namespace + ".setof" + name).toLowerCase
            typesToIgnore += (namespace + ".treesetof" + name).toLowerCase

            if (excludedMetadataTypes.contains(mdf.objType.toLowerCase()) == false) {
              containers += ((mdf.objType, jsonObjMap))
              addedMessagesContainers.add(namespace + "." + name)
            }
          } else {
            if (excludedMetadataTypes.contains(mdf.objType.toLowerCase()) == false) {
              allTemp += ((mdf.objType, jsonObjMap))
            }
          }
        }
      }
    })

    allTemp.foreach(jsonObjMap => {
      val objType = jsonObjMap._1
      if (objType == "ModelDef") {
        models += jsonObjMap
      } else if (objType == "ArrayTypeDef" ||
        objType == "ArrayBufTypeDef" ||
        objType == "SortedSetTypeDef" ||
        objType == "ImmutableMapTypeDef" ||
        objType == "MapTypeDef" ||
        objType == "HashMapTypeDef" ||
        objType == "SetTypeDef" ||
        objType == "ImmutableSetTypeDef" ||
        objType == "TreeSetTypeDef") {
        val namespace = jsonObjMap._2.getOrElse("NameSpace", "").toString.trim()
        val name = jsonObjMap._2.getOrElse("Name", "").toString.trim()

        val typ = (namespace + "." + name).toLowerCase
        if (typesToIgnore.contains(typ) == false)
          types += jsonObjMap
      } else if (objType == "FunctionDef") {
        functions += jsonObjMap
      } else if (objType == "JarDef") {
        jarDef += jsonObjMap
      } else if (objType == "ConfigDef") {
        configDef += jsonObjMap
      } else {
        logger.error("ObjectType:%s is not handled".format(objType))
      }
    })

    // Open OpenDbStore
    getMetadataAPI.OpenDbStore(_jarPaths, _metaDataStoreInfo)

    val cfgStr = Source.fromFile(_clusterConfigFile).mkString
    logger.debug("Uploading configuration")
    getMetadataAPI.UploadConfig(cfgStr, defaultUserId, "ClusterConfig")

    // We need to add the metadata in the following order
    // Jars
    // Types
    // Containers
    // Messages
    // Functions
    // Model configuration
    // Models
    // OutputMessageDef

    ProcessObject(jarDef)
    ProcessObject(types)


    if (_mergeContainerAndMessages) {
      val msgsAndContainers = ArrayBuffer[(String, Map[String, Any])]()
      if (containers.length > 0)
        msgsAndContainers ++= containers
      if (messages.length > 0)
        msgsAndContainers ++= messages
      BuildHierarchyAndProcessObjectsParallel(msgsAndContainers, "Failed to add messages/container")
    } else {
      BuildHierarchyAndProcessObjectsParallel(containers, "Failed to add container")
      BuildHierarchyAndProcessObjectsParallel(messages, "Failed to add message")
    }

    if (_adapterMessageBindings != None)
      AddMsgBindings

    val failedMsgsContainer = GetFailedMessageOrContainer

    if (failedMsgsContainer.size > 0) {
      // Report the errors and Stop migration
      val errMsg = "Migrating Messages/Containers (%s) failed".format(failedMsgsContainer.mkString(","))
      logger.error(errMsg)
      throw new Exception(errMsg)
    }

    ProcessObject(functions)
    ProcessObject(configDef)
    //ProcessObject(models)
    ProcessMdObjectsParallel(models, "Failed to add model")

    return addedMessagesContainers
  }

  private def callSaveData(dataStore: DataStoreOperations, data_list: Array[(String, Array[(Key, String, Any)])]): Unit = {
    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs
    var doneSave = false

    while (!doneSave) {
      try {
        dataStore.put(null, data_list)
        doneSave = true
      } catch {
        case e: FatalAdapterException => {
          logger.error("Failed to save data into datastore", e)
        }
        case e: StorageDMLException => {
          logger.error("Failed to save data into datastore", e)
        }
        case e: StorageDDLException => {
          logger.error("Failed to save data into datastore", e)
        }
        case e: Exception => {
          logger.error("Failed to save data into datastore", e)
        }
        case e: Throwable => {
          logger.error("Failed to save data into datastore", e)
        }
      }

      if (!doneSave) {
        try {
          logger.error("Failed to save data into datastore. Waiting for another %d milli seconds and going to start them again.".format(failedWaitTime))
          Thread.sleep(failedWaitTime)
        } catch {
          case e: Exception => {
            logger.warn("", e)
          }
        }
        // Adjust time for next time
        if (failedWaitTime < maxFailedWaitTime) {
          failedWaitTime = failedWaitTime * 2
          if (failedWaitTime > maxFailedWaitTime)
            failedWaitTime = maxFailedWaitTime
        }
      }
    }
  }

  private def callGetData(dataStore: DataStoreOperations, containerName: String, keys: Array[Key], callbackFunction: (Key, Any, String, String, Int) => Unit): Unit = {
    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs
    var doneGet = false

    while (!doneGet) {
      try {
        dataStore.get(containerName, keys, callbackFunction)
        doneGet = true
      } catch {
        case e@(_: ObjectNotFoundException | _: KeyNotFoundException) => {
          logger.debug("Failed to get data from container:%s".format(containerName))
          doneGet = true
        }
        case e: FatalAdapterException => {
          val stackTrace = StackTrace.ThrowableTraceString(e.cause)
          logger.error("Failed to get data from container:%s.\nStackTrace:%s".format(containerName, stackTrace))
        }
        case e: StorageDMLException => {
          val stackTrace = StackTrace.ThrowableTraceString(e.cause)
          logger.error("Failed to get data from container:%s.\nStackTrace:%s".format(containerName, stackTrace))
        }
        case e: StorageDDLException => {
          val stackTrace = StackTrace.ThrowableTraceString(e.cause)
          logger.error("Failed to get data from container:%s.\nStackTrace:%s".format(containerName, stackTrace))
        }
        case e: Exception => {
          val stackTrace = StackTrace.ThrowableTraceString(e)
          logger.error("Failed to get data from container:%s.\nStackTrace:%s".format(containerName, stackTrace))
        }
        case e: Throwable => {
          val stackTrace = StackTrace.ThrowableTraceString(e)
          logger.error("Failed to get data from container:%s.\nStackTrace:%s".format(containerName, stackTrace))
        }
      }

      if (!doneGet) {
        try {
          logger.error("Failed to get data from datastore. Waiting for another %d milli seconds and going to start them again.".format(failedWaitTime))
          Thread.sleep(failedWaitTime)
        } catch {
          case e: Exception => {}
        }
        // Adjust time for next time
        if (failedWaitTime < maxFailedWaitTime) {
          failedWaitTime = failedWaitTime * 2
          if (failedWaitTime > maxFailedWaitTime)
            failedWaitTime = maxFailedWaitTime
        }
      }
    }
  }

  // Array of tuples has container name, timepartition value, bucketkey, transactionid, rowid, serializername & data in Gson (JSON) format
  override def populateAndSaveData(data: Array[DataFormat]): Unit = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    initDataConversion
    val containersData = data.groupBy(_.containerName.toLowerCase)
    val data_list = containersData.map(kv => (kv._1, kv._2.map(d => {
      val sendVal =
        if (_mdObjectRes.isMsgOrContainer(d.containerName)) {
          val container = convertDataTo1_5_x(d.containerName, d.serializername, d.data, d.timePartition, d.transactionid, d.rowid)
          container.asInstanceOf[Any]
        } else if (d.containerName.equalsIgnoreCase("AdapterUniqKvData")) {
          containersData.synchronized {
            implicit val jsonFormats: Formats = DefaultFormats
            val uniqVal = parse(new String(d.data)).extract[AdapterUniqueValueDes_1_3]
            if (logger.isDebugEnabled)
              logger.debug("AdapterUniqKvData. Key:%s, Value:%s. Original Data:%s".format(d.bucketKey.mkString(","), uniqVal.V, new String(d.data)))
            uniqVal.V.getBytes().asInstanceOf[Any]
          }
        } else {
          d.data.asInstanceOf[Any]
        }

      (Key(d.timePartition, d.bucketKey, d.transactionid, d.rowid), d.serializername, sendVal)
    }))).toArray

    callSaveData(_tenantDsDb, data_list);
  }

  override def shutdown: Unit = {
    if (_metaDataStoreDb != null)
      _metaDataStoreDb.Shutdown()
    if (_tenantDsDb != null)
      _tenantDsDb.Shutdown()
    if (_dataStoreDb != null)
      _dataStoreDb.Shutdown()
    if (_statusStoreDb != null)
      _statusStoreDb.Shutdown()
    if (_flCurMigrationSummary != null)
      _flCurMigrationSummary.close()
    _metaDataStoreDb = null
    _tenantDsDb = null
    _dataStoreDb = null
    _statusStoreDb = null
    _flCurMigrationSummary = null
    getMetadataAPI.shutdown
  }

  override def getStatusFromDataStore(key: String): String = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    if (_dataStoreDb == null)
      throw new Exception("Not found valid Datastore DB connection")

    var ret = ""
    val buildAdapOne = (k: Key, v: Any, serType: String, typ: String, ver: Int) => {
      ret = new String(v.asInstanceOf[Array[Byte]])
    }

    callGetData(_dataStoreDb, "MigrateStatusInformation", Array(Key(KvBaseDefalts.defaultTime, Array(key.toLowerCase), 0, 0)), buildAdapOne)

    ret
  }

  override def setStatusFromDataStore(key: String, value: String) = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    if (_dataStoreDb == null)
      throw new Exception("Not found valid Datastore DB connection")

    callSaveData(_dataStoreDb, Array(("MigrateStatusInformation", Array((Key(KvBaseDefalts.defaultTime, Array(key.toLowerCase), 0, 0), "txt", value.getBytes().asInstanceOf[Any])))))
  }

  private def initDataConversion: Unit = {
    if (_mdObjectRes != null) return
    _dataConversionInitLock.synchronized {
      if (_mdObjectRes != null) return
      val mdObjectRes = new MdObjectRes(MdMgr.GetMdMgr, _jarPaths)
      mdObjectRes.ResolveMessageAndContainers()
      _mdObjectRes = mdObjectRes
      _tenantDsDb.setObjectResolver(_mdObjectRes)
      _tenantDsDb.setDefaultSerializerDeserializer("com.ligadata.kamanja.serializer.kbinaryserdeser", scala.collection.immutable.Map[String, Any]())
    }
  }

  private def deserializeContainer(serType: String, serInfo: Array[Byte]): ContainerInterface = {
    var dis = new DataInputStream(new ByteArrayInputStream(serInfo));

    val typName = dis.readUTF
    val version = dis.readUTF
    val classname = dis.readUTF

    try {
      val container = _mdObjectRes.getInstance(typName)
      if (container == null) {
        throw new Exception("Failed to get instance of " + typName)
      }
      if (container.isFixed) {
        val attribs = container.getAttributeTypes
        attribs.foreach(at => {
          val valType = at.getTypeCategory
          val fld = valType match {
            case LONG => container.set(at.getIndex, com.ligadata.BaseTypes.LongImpl.DeserializeFromDataInputStream(dis))
            case INT => container.set(at.getIndex, com.ligadata.BaseTypes.IntImpl.DeserializeFromDataInputStream(dis))
            case BYTE => container.set(at.getIndex, com.ligadata.BaseTypes.CharImpl.DeserializeFromDataInputStream(dis).toByte)
            case BOOLEAN => container.set(at.getIndex, com.ligadata.BaseTypes.BoolImpl.DeserializeFromDataInputStream(dis))
            case DOUBLE => container.set(at.getIndex, com.ligadata.BaseTypes.DoubleImpl.DeserializeFromDataInputStream(dis))
            case FLOAT => container.set(at.getIndex, com.ligadata.BaseTypes.FloatImpl.DeserializeFromDataInputStream(dis))
            case STRING => container.set(at.getIndex, com.ligadata.BaseTypes.StringImpl.DeserializeFromDataInputStream(dis))
            case CHAR => container.set(at.getIndex, com.ligadata.BaseTypes.CharImpl.DeserializeFromDataInputStream(dis))
            //              case MAP => { // BUGBUG:: Not really handled old maps }
            case (CONTAINER | MESSAGE) => {
              val length = com.ligadata.BaseTypes.IntImpl.DeserializeFromDataInputStream(dis)
              if (length > 0) {
                var bytes = new Array[Byte](length);
                dis.read(bytes);
                val inst = deserializeContainer(serType, bytes)
                container.set(at.getIndex, inst)
              }
            }
            case ARRAY => {
              var arraySize = com.ligadata.BaseTypes.IntImpl.DeserializeFromDataInputStream(dis);
              val itmType = at.getValTypeCategory
              val fld = itmType match {
                case LONG => {
                  val arr = new Array[Long](arraySize)
                  for (i <- 0 until arraySize)
                    arr(i) = com.ligadata.BaseTypes.LongImpl.DeserializeFromDataInputStream(dis)
                  container.set(at.getIndex, arr)
                }
                case INT => {
                  val arr = new Array[Int](arraySize)
                  for (i <- 0 until arraySize)
                    arr(i) = com.ligadata.BaseTypes.IntImpl.DeserializeFromDataInputStream(dis)
                  container.set(at.getIndex, arr)
                }
                case BYTE => {
                  val arr = new Array[Byte](arraySize)
                  for (i <- 0 until arraySize)
                    arr(i) = com.ligadata.BaseTypes.CharImpl.DeserializeFromDataInputStream(dis).toByte
                  container.set(at.getIndex, arr)
                }
                case BOOLEAN => {
                  val arr = new Array[Boolean](arraySize)
                  for (i <- 0 until arraySize)
                    arr(i) = com.ligadata.BaseTypes.BoolImpl.DeserializeFromDataInputStream(dis)
                  container.set(at.getIndex, arr)
                }
                case DOUBLE => {
                  val arr = new Array[Double](arraySize)
                  for (i <- 0 until arraySize)
                    arr(i) = com.ligadata.BaseTypes.DoubleImpl.DeserializeFromDataInputStream(dis)
                  container.set(at.getIndex, arr)
                }
                case FLOAT => {
                  val arr = new Array[Float](arraySize)
                  for (i <- 0 until arraySize)
                    arr(i) = com.ligadata.BaseTypes.FloatImpl.DeserializeFromDataInputStream(dis)
                  container.set(at.getIndex, arr)
                }
                case STRING => {
                  val arr = new Array[String](arraySize)
                  for (i <- 0 until arraySize)
                    arr(i) = com.ligadata.BaseTypes.StringImpl.DeserializeFromDataInputStream(dis)
                  container.set(at.getIndex, arr)
                }
                case CHAR => {
                  val arr = new Array[Long](arraySize)
                  for (i <- 0 until arraySize)
                    arr(i) = com.ligadata.BaseTypes.CharImpl.DeserializeFromDataInputStream(dis)
                  container.set(at.getIndex, arr)
                }
                //                  case MAP => { // BUGBUG:: Not really handled old maps }
                case (CONTAINER | MESSAGE) => {
                  val arr = new Array[ContainerInterface](arraySize)
                  for (i <- 0 until arraySize) {
                    val length = com.ligadata.BaseTypes.IntImpl.DeserializeFromDataInputStream(dis)
                    if (length > 0) {
                      var bytes = new Array[Byte](length);
                      dis.read(bytes);
                      arr(i) = deserializeContainer(serType, bytes)
                    }
                  }
                  container.set(at.getIndex, arr)
                }
                //                  case ARRAY => { BUGBUG:- Not handling array in array }
                case _ => throw new ObjectNotFoundException("Array: invalid value type: ${itmType.getValue}, fldName: ${itmType.name} could not be resolved", null)
              }
            }
            case _ => throw new UnsupportedObjectException("Not yet handled valType:" + valType, null)
          }
        })
      } else {
        // Mapped is yet to handle
        throw new Exception("Not yet handled data migration for mapped message:" + typName)
      }
      dis.close
      return container
    } catch {
      case e: Exception => {
        // LOG.error("Failed to get classname :" + clsName, e)
        logger.debug("Failed to Deserialize", e)
        dis.close
        throw e
      }
    }
    null
  }

  private def convertDataTo1_5_x(containerName: String, serType: String, serInfo: Array[Byte], timePartition: Long, transactionid: Long, rowid: Int): ContainerInterface = {
    val container = deserializeContainer(serType, serInfo)
    if (container == null) {
      throw new Exception("Failed to get instance of " + containerName)
    }

    container.setTransactionId(transactionid)
    container.setRowNumber(rowid)
    container.setTimePartitionData(timePartition)
    container
  }
}
