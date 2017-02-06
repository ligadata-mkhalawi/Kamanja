package com.ligadata.kamanja.test.application

import java.io.{File, PrintWriter}
import java.util.NoSuchElementException

import com.ligadata.MetadataAPI.ConfigUtils
import com.ligadata.KamanjaManager.embedded._
import com.ligadata.test.configuration.cluster._
import com.ligadata.test.configuration.cluster.adapters.{KafkaAdapterSpecificConfig, TenantCacheConfig, _}
import com.ligadata.test.configuration.cluster.adapters.interfaces._
import com.ligadata.test.configuration.cluster.nodes._
import com.ligadata.test.configuration.cluster.python.PythonConfiguration
import com.ligadata.test.configuration.cluster.zookeeper._
import com.ligadata.test.embedded.zookeeper._
import com.ligadata.kafkaInputOutputAdapters_v10.embedded._
import com.ligadata.test.utils._
import com.ligadata.MetadataAPI.test._
import com.ligadata.Serialize.JsonSerializer
import com.ligadata.kamanja.metadata.MdMgr
import com.ligadata.kamanja.test.application.logging.{KamanjaAppLogger, KamanjaAppLoggerException}
import com.ligadata.test.embedded.kafka._
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.io.Source

case class KamanjaEnvironmentManagerException(message: String, cause: Throwable = null) extends Exception(message, cause)

object KamanjaEnvironmentManager {
  private var isInitialized: Boolean = false
  private var logger: KamanjaAppLogger = _
  private var kamanjaInstallDir: String = _
  private var mdMan: MetadataManager = _
  private implicit val formats = org.json4s.DefaultFormats

  def init(kamanjaInstallDir: String, metadataConfigFile: String, clusterConfigFile: String): Unit = {
    isInitialized = true

    // Initializing Kamanja Application Test Tool Logger
    try {
      logger = KamanjaAppLogger.getKamanjaAppLogger
    }
    catch {
      case e: KamanjaAppLoggerException => logger = KamanjaAppLogger.createKamanjaAppLogger(kamanjaInstallDir)
    }

    try {
      sys.env("PYTHON_HOME")
    }
    catch {
      case e: NoSuchElementException =>
        throw new EmbeddedServicesException("***ERROR*** Failed to discover environmental variable PYTHON_HOME. " +
          "Please set it before running.\n" +
          "EX: export PYTHON_HOME=/usr")
    }

    this.kamanjaInstallDir = kamanjaInstallDir

    mdMan = new MetadataManager
    mdMan.setSSLPassword("")
    mdMan.initMetadataCfg(metadataConfigFile)

    val addConfigResult = mdMan.addConfig(clusterConfigFile)
    if (addConfigResult != 0) {
      logger.error("***ERROR*** Attempted to upload cluster configuration but failed")
      throw new KamanjaEnvironmentManagerException("***ERROR*** Attempted to upload cluster configuration but failed")
    }
    logger.info("Cluster configuration successfully uploaded")
  }

  def getAllAdapters: List[Adapter] = {
    if(!isInitialized) {
      throw new KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    val adaptersAPIResult = ConfigUtils.GetAllAdapters("JSON", Some("kamanja"))
    val adaptersResultData = mdMan.parseApiResult(adaptersAPIResult).resultData
    val adaptersListMap = (parse(adaptersResultData) \\ "Adapters").extract[List[Map[String, Any]]]

    var adapterList: List[Adapter] = List()

    adaptersListMap.foreach(adapter => {
      val typeString: String = adapter("TypeString").toString
      val tenantId: String = adapter("TenantId").toString
      val name: String = adapter("Name").toString

      typeString.toLowerCase match {
        case "input" | "output" => {
          val dependencyJars: List[String] = adapter("DependencyJars").asInstanceOf[List[String]]
          val className: String = adapter("ClassName").toString
          val adapterSpecificCfg = parse(adapter("AdapterSpecificCfg").toString).extract[Map[String, String]]
          val jarName = adapter("JarName").toString
          var adapterType: AdapterType = null

          // Converting the adapter type to an object for use in the configuration api under the KamanjaTestUtils project.
          if(typeString.toLowerCase == "input")
            adapterType = InputAdapter
          else if(typeString.toLowerCase == "output")
            adapterType = OutputAdapter

          //Optional Fields
          val associatedMessage = adapter.getOrElse("AssociatedMessage", "").toString
          val keyValueDelimiter = adapter.getOrElse("KeyValueDelimiter", "").toString
          val fieldDelimiter = adapter.getOrElse("FieldDelimiter", "").toString
          val valueDelimiter = adapter.getOrElse("ValueDelimiter", "").toString

          if(className.toLowerCase.contains("kafka")) {
            val hostList = adapterSpecificCfg("HostList").toString
            val topicName = adapterSpecificCfg("TopicName").toString
            adapterList :+= new KafkaAdapterConfig(name, adapterType, associatedMessage, keyValueDelimiter,
              fieldDelimiter, valueDelimiter, className, jarName, dependencyJars,
              new KafkaAdapterSpecificConfig(hostList, topicName), tenantId)
          }
        }
        case "storage" => {
          throw new KamanjaEnvironmentManagerException("Storage Adapters are currently unsupported.")
        }
        case _ => throw new KamanjaEnvironmentManagerException(s"Unrecognized Type String $typeString found.")
      }
    })

    return adapterList
  }

  def getZookeeperConfiguration: ZookeeperConfig = {
    if(!isInitialized) {
      throw new KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    val clusterCfgs = MdMgr.GetMdMgr.ClusterCfgs.values.toArray
    val zookeeperInfoJsonStr = (parse(JsonSerializer.SerializeCfgObjectListToJson("ClusterCfgs", clusterCfgs)) \\ "CfgMap" \\ "ZooKeeperInfo").extract[String]
    val zookeeperInfoMap = parse(zookeeperInfoJsonStr).extract[Map[String, String]]

    val zkNodeBasePath = zookeeperInfoMap("ZooKeeperNodeBasePath").toString
    val zkConnStr = zookeeperInfoMap("ZooKeeperConnectString").toString
    val zkSessionTimeoutMs = zookeeperInfoMap("ZooKeeperSessionTimeoutMs").toInt
    val zkConnectionTimeoutMs = zookeeperInfoMap("ZooKeeperConnectionTimeoutMs").toInt

    return new ZookeeperConfig(zkNodeBasePath, zkConnStr, zkSessionTimeoutMs, zkConnectionTimeoutMs)
  }

  def getAllTenants: List[TenantConfiguration] = {
    if(!isInitialized) {
      throw new KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    var tenantList: List[TenantConfiguration] = List()
    val tenantInfoArr = MdMgr.GetMdMgr.GetAllTenantInfos
    tenantInfoArr.foreach(tenantInfo => {
      val tenantId = tenantInfo.tenantId
      if(tenantId != "System") {
        val tenantDescription = tenantInfo.description
        val tenantPrimaryDataStoreConfig = createStorageAdapter(tenantInfo.primaryDataStore)

        // parse json and convert into TenantCacheConfig object from KamanjaTestUtils project
        val tenantInfoCacheConfig = tenantInfo.cacheConfig
        val tenantCacheConfigMap = parse(tenantInfoCacheConfig).extract[Map[String, Int]]
        val maxSizeInMB = tenantCacheConfigMap("MaxSizeInMB")

        val tenantCacheConfig = new TenantCacheConfig(maxSizeInMB)

        tenantList :+= new TenantConfiguration(tenantId, tenantDescription, tenantCacheConfig, tenantPrimaryDataStoreConfig)
      }
    })
    return tenantList
  }

  def getSystemCatalog: StorageAdapter = {
    if(!isInitialized) {
      throw new KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    val clusterCfgs = MdMgr.GetMdMgr.ClusterCfgs.values.toArray
    val sysCatalogJsonStr = (parse(JsonSerializer.SerializeCfgObjectListToJson("ClusterCfgs", clusterCfgs)) \\ "CfgMap" \\ "SystemCatalog").extract[String]
    return createStorageAdapter(sysCatalogJsonStr)
  }

  def getAllNodes: List[NodeConfiguration] = {
    if(!isInitialized) {
      throw new KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    val nodesApiResultStr = ConfigUtils.GetAllNodes("JSON", Some("kamanja"))
    var nodeList: List[NodeConfiguration] = List()
    val nodesResultData = mdMan.parseApiResult(nodesApiResultStr).resultData
    val adaptersListMap = (parse(nodesResultData) \\ "Nodes").extract[List[Map[String, Any]]]

    adaptersListMap.foreach(node => {
      val nodeId = node("NodeId").toString
      val scalaHome = node("Scala_home").toString
      val nodePort = node("NodePort").toString.toInt
      val jarPaths: Array[String] = node("JarPaths").asInstanceOf[List[String]].toArray
      val nodeIP = node("NodeIpAddr").toString
      val classpath = node("Classpath").toString
      val roles: Array[String] = node("Roles").asInstanceOf[List[String]].toArray
      val javaHome = node("Java_home").toString

      nodeList :+= new NodeConfiguration(nodeId, nodePort, nodeIP, jarPaths.toArray, scalaHome, javaHome, classpath)
    })

    return nodeList
  }

  def getClusterCacheConfiguration: ClusterCacheConfig = {
    if(!isInitialized) {
      throw new KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    val clusterCfgs = MdMgr.GetMdMgr.ClusterCfgs.values.toArray
    val clusterCacheJsonStr = (parse(JsonSerializer.SerializeCfgObjectListToJson("ClusterCfgs", clusterCfgs)) \\ "CfgMap" \\ "Cache").extract[String]
    val clusterCacheMap = parse(clusterCacheJsonStr).extract[Map[String, Any]]
    val timeToIdleSeconds = clusterCacheMap("TimeToIdleSeconds").toString.toInt
    val cacheSizePerNodeInMB = clusterCacheMap("CacheSizePerNodeInMB").toString.toInt
    val evictionPolicy: String = clusterCacheMap("EvictionPolicy").toString
    val replicateFactor = clusterCacheMap("ReplicateFactor").toString.toInt
    val cacheStartPort = clusterCacheMap("CacheStartPort").toString.toInt

    return new ClusterCacheConfig(cacheStartPort, cacheSizePerNodeInMB, replicateFactor, timeToIdleSeconds, evictionPolicy)
  }

  def getEnvironmentContextConfiguration: EnvironmentContextConfig = {
    if(!isInitialized) {
      throw new KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    val clusterCfgs = MdMgr.GetMdMgr.ClusterCfgs.values.toArray
    val envContextJsonStr = (parse(JsonSerializer.SerializeCfgObjectListToJson("ClusterCfgs", clusterCfgs)) \\ "CfgMap" \\ "EnvironmentContext").extract[String]
    val envContextMap = parse(envContextJsonStr).extract[Map[String, Any]]
    val classname = envContextMap("classname").toString
    val jarname = envContextMap("jarname").toString
    val dependencyJars = envContextMap("dependencyjars").asInstanceOf[List[String]]
    return new EnvironmentContextConfig(classname, jarname, dependencyJars)
  }

  def getPythonConfiguration: PythonConfiguration = {
    if(!isInitialized) {
      throw new KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    val clusterCfgs = MdMgr.GetMdMgr.ClusterCfgs.values.toArray
    val pythonCfgJsonStr = (parse(JsonSerializer.SerializeCfgObjectListToJson("ClusterCfgs", clusterCfgs)) \\ "CfgMap" \\ "PYTHON_CONFIG").extract[String]
    val pythonCfgMap = parse(pythonCfgJsonStr).extract[Map[String,String]]
    val pythonPath = pythonCfgMap("PYTHON_PATH")
    val serverHost = pythonCfgMap("SERVER_HOST")
    val pythonBinDir = pythonCfgMap("PYTHON_BIN_DIR")
    val serverBasePort = pythonCfgMap("SERVER_BASE_PORT").toInt
    val pythonLogConfigPath = pythonCfgMap("PYTHON_LOG_CONFIG_PATH")
    val serverPortLimit = pythonCfgMap("SERVER_PORT_LIMIT").toInt
    val pythonLogPath = pythonCfgMap("PYTHON_LOG_PATH")

    return new PythonConfiguration(serverBasePort, serverPortLimit, serverHost, pythonPath, pythonBinDir, pythonLogConfigPath, pythonLogPath)
  }

  def getClusterId: String = {
    if(!isInitialized) {
      throw new KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    val clusters = MdMgr.GetMdMgr.Clusters.values.toArray
    val clusterId = (parse(JsonSerializer.SerializeCfgObjectListToJson("Clusters", clusters)) \\ "ClusterId").extract[String]
    return clusterId
  }

  def getMetadataManager: MetadataManager = {
    if(!isInitialized) {
      throw new KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    return mdMan
  }

  private def createStorageAdapter(storageJsonStr: String): StorageAdapter = {
    val storageMap = parse(storageJsonStr).extract[Map[String, String]]
    val hostname = storageMap("Location")
    val stType = storageMap("StoreType")
    val schemaName = storageMap("SchemaName")

    //Optional for H2DB storage
    val connectionMode = storageMap.getOrElse("connectionMode", "")
    val portnumber = storageMap.getOrElse("portnumber", "")
    val user = storageMap.getOrElse("user", "")
    val password = storageMap.getOrElse("password", "")

    //TODO: Extra parameters for HBase need to be searched for

    var storeType: StoreType = null

    stType match {
      case "h2db" =>
        storeType = new H2DBStore
        storeType.asInstanceOf[H2DBStore].connectionMode = connectionMode
      case "hbase" =>
        storeType = new HBaseStore
      case "cassandra" =>
        storeType = new CassandraStore
    }

    return new StorageConfiguration(storeType, schemaName, hostname)
  }
}
