package com.ligadata.kamanja.test.application

import java.io.File
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
import com.ligadata.kafkaInputOutputAdapters_v9.embedded.{EmbeddedKafkaCluster, KafkaBroker}
import com.ligadata.test.utils._
import com.ligadata.MetadataAPI.test._
import com.ligadata.Serialize.JsonSerializer
import com.ligadata.kamanja.metadata.MdMgr
import com.ligadata.kamanja.test.application.configuration.EmbeddedConfiguration
import com.ligadata.kamanja.test.application.logging.{KamanjaAppLogger, KamanjaAppLoggerException}
import com.ligadata.test.embedded.kafka._
import org.json4s._
import org.json4s.native.JsonMethods._

case class KamanjaEnvironmentManagerException(message: String, cause: Throwable = null) extends Exception(message, cause)

object KamanjaEnvironmentManager {
  private var isInitialized: Boolean = false
  private var logger: KamanjaAppLogger = _
  private var kamanjaInstallDir: String = _
  var mdMan: MetadataManager = _
  private var embeddedKamanjaManager: EmbeddedKamanjaManager = _
  private var embeddedZookeeper: EmbeddedZookeeper = _
  private var kafkaCluster: EmbeddedKafkaCluster = _
  private var zkClient: ZookeeperClient = _
  private var kafkaConsumer: TestKafkaConsumer = _
  var kamanjaConfigFile: String = _
  var clusterConfigFile: String = _
  var metadataConfigFile: String = _
  var h2dbStore: H2DBStore = new H2DBStore
  var isEmbedded: Boolean = false
  private implicit val formats = org.json4s.DefaultFormats

  def init(kamanjaInstallDir: String, metadataConfigFile: String = null, clusterConfigFile: String = null): Unit = {
    isInitialized = true

    try {
      logger = KamanjaAppLogger.getKamanjaAppLogger
    }
    catch {
      case e: KamanjaAppLoggerException => logger = KamanjaAppLogger.createKamanjaAppLogger(kamanjaInstallDir)
    }

    this.kamanjaInstallDir = kamanjaInstallDir

    if(clusterConfigFile == null || metadataConfigFile == null) {
      isEmbedded = true
      logger.info("Cluster Configuration file or Metadata API Configuration file not provided. Initializing embedded services.")
      try {
        sys.env("PYTHON_HOME")
      }
      catch {
        case e: NoSuchElementException =>
          throw KamanjaEnvironmentManagerException("***ERROR*** Failed to discover environmental variable PYTHON_HOME. " +
            "Please set it before running Kamanja Application Tester using embedded services.\n" +
            "EX: export PYTHON_HOME=/usr")
      }

      embeddedZookeeper = new EmbeddedZookeeper
      kafkaCluster = new EmbeddedKafkaCluster().
        withBroker(new KafkaBroker(1, embeddedZookeeper.getConnection))

      //clusterConfig = generateClusterConfiguration
      kamanjaConfigFile = TestUtils.constructTempDir("/kamanja-tmp-config").getAbsolutePath + "/kamanja.conf"
      embeddedZookeeper = new EmbeddedZookeeper
      kafkaCluster = new EmbeddedKafkaCluster().
        withBroker(new KafkaBroker(1, embeddedZookeeper.getConnection))
      this.metadataConfigFile = EmbeddedConfiguration.generateMetadataAPIConfigFile(kamanjaInstallDir, embeddedZookeeper.getConnection)
      this.clusterConfigFile = EmbeddedConfiguration.generateClusterConfigFile(kamanjaInstallDir, kafkaCluster.getBrokerList, embeddedZookeeper.getConnection)
      this.kamanjaConfigFile = EmbeddedConfiguration.generateKamanjaConfigFile
      logger.info(s"Starting Embedded Services...")
      if (!KamanjaEnvironmentManager.startServices) {
        logger.error(s"***ERROR*** Failed to start embedded services")
        KamanjaEnvironmentManager.stopServices
        TestUtils.deleteFile(EmbeddedConfiguration.storageDir)
        throw KamanjaEnvironmentManagerException(s"***ERROR*** Failed to start embedded services")
      }
    }
    else {
      this.metadataConfigFile = metadataConfigFile
      this.clusterConfigFile = clusterConfigFile
      mdMan = new MetadataManager
      mdMan.setSSLPassword("")
      mdMan.initMetadataCfg(this.metadataConfigFile)
      val addConfigResult = mdMan.addConfig(new File(this.clusterConfigFile))
      if (addConfigResult != 0) {
        logger.error("***ERROR*** Attempted to upload cluster configuration but failed")
        throw KamanjaEnvironmentManagerException("***ERROR*** Attempted to upload cluster configuration but failed")
      }
      logger.info("Cluster configuration successfully uploaded")
      kafkaConsumer = new TestKafkaConsumer(getOutputKafkaAdapterConfig)
    }
  }

  def getAllAdapters: List[Adapter] = {
    if (!isInitialized) {
      throw KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    val adaptersAPIResult = ConfigUtils.GetAllAdapters("JSON", Some("kamanja"))
    val adaptersResultData = mdMan.parseApiResult(adaptersAPIResult).resultData
    val adaptersListMap = (parse(adaptersResultData) \\ "Adapters" \ "Adapter").extract[List[Map[String, Any]]]
    var adapterList: List[Adapter] = List()

    adaptersListMap.foreach(adapter => {
      val typeString: String = adapter("TypeString").toString
      val tenantId: String = adapter("TenantId").toString
      val name: String = adapter("Name").toString

      typeString.toLowerCase match {
        case "input" | "output" => {
          val className: String = adapter("ClassName").toString
          val dependencyJars: List[String] = adapter("DependencyJars").asInstanceOf[List[String]]
          val adapterSpecificCfg = parse(adapter("AdapterSpecificCfg").toString).extract[Map[String, String]]
          val jarName = adapter("JarName").toString
          var adapterType: AdapterType = null

          // Converting the adapter type to an object for use in the configuration api under the KamanjaTestUtils project.
          if (typeString.toLowerCase == "input")
            adapterType = InputAdapter
          else if (typeString.toLowerCase == "output")
            adapterType = OutputAdapter

          //Optional Fields
          val associatedMessage = adapter.getOrElse("AssociatedMessage", "").toString
          val keyValueDelimiter = adapter.getOrElse("KeyValueDelimiter", "").toString
          val fieldDelimiter = adapter.getOrElse("FieldDelimiter", "").toString
          val valueDelimiter = adapter.getOrElse("ValueDelimiter", "").toString

          if (className.toLowerCase.contains("kamanjakafkaconsumer") || className.toLowerCase.contains("kafkaproducer")) {
            val hostList = adapterSpecificCfg("HostList").toString
            val topicName = adapterSpecificCfg("TopicName").toString
            adapterList = adapterList :+ KafkaAdapterConfig(name, adapterType, associatedMessage, keyValueDelimiter,
              fieldDelimiter, valueDelimiter, className, jarName, dependencyJars,
              KafkaAdapterSpecificConfig(hostList, topicName), tenantId)
          }
        }
        case "storage" => throw KamanjaEnvironmentManagerException("Storage Adapters are currently unsupported.")
        case _ => throw KamanjaEnvironmentManagerException(s"Unrecognized Type String $typeString found.")
      }
    })

    adapterList
  }

  def getZookeeperConfiguration: ZookeeperConfig = {
    if(!isInitialized) {
      throw KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
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
      throw KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
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
      throw KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    val clusterCfgs = MdMgr.GetMdMgr.ClusterCfgs.values.toArray
    val sysCatalogJsonStr = (parse(JsonSerializer.SerializeCfgObjectListToJson("ClusterCfgs", clusterCfgs)) \\ "CfgMap" \\ "SystemCatalog").extract[String]
    return createStorageAdapter(sysCatalogJsonStr)
  }

  def getAllNodes: List[NodeConfiguration] = {
    if(!isInitialized) {
      throw KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    val nodesApiResultStr = ConfigUtils.GetAllNodes("JSON", Some("kamanja"))
    var nodeList: List[NodeConfiguration] = List()
    val nodesResultData = mdMan.parseApiResult(nodesApiResultStr).resultData
    val adaptersListMap = (parse(nodesResultData) \\ "Nodes" \ "Node").extract[List[Map[String, Any]]]

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
      throw KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
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
      throw KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
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
      throw KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
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
      throw KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    val clusters = MdMgr.GetMdMgr.Clusters.values.toArray
    val clusterId = (parse(JsonSerializer.SerializeCfgObjectListToJson("Clusters", clusters)) \\ "ClusterId").extract[String]
    return clusterId
  }

  def getMetadataManager: MetadataManager = {
    if(!isInitialized) {
      throw KamanjaEnvironmentManagerException("Kamanja Environment Manager has not been initialized. Please run def init first.")
    }
    return mdMan
  }

  def getInputKafkaAdapterConfig: KafkaAdapterConfig = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** KamanjaEnvironmentManager has not been initialized. Please call def init first.")
    }
    return getAllAdapters.filter(_.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName.toLowerCase == "testin_1")(0).asInstanceOf[KafkaAdapterConfig]
  }

  def getOutputKafkaAdapterConfig: KafkaAdapterConfig = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** KamanjaEnvironmentManager has not been initialized. Please call def init first.")
    }
    return getAllAdapters.filter(_.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName.toLowerCase == "testout_1")(0).asInstanceOf[KafkaAdapterConfig]
  }

  def getErrorKafkaAdapterConfig: KafkaAdapterConfig = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** KamanjaEnvironmentManager has not been initialized. Please call def init first.")
    }
    return getAllAdapters.filter(_.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName.toLowerCase == "testfailedevents_1")(0).asInstanceOf[KafkaAdapterConfig]
  }

  def getEventKafkaAdapterConfig: KafkaAdapterConfig = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** KamanjaEnvironmentManager has not been initialized. Please call def init first.")
    }
    return getAllAdapters.filter(_.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName.toLowerCase == "testmessageevents_1")(0).asInstanceOf[KafkaAdapterConfig]
  }

  //////////////////////////////////////////
  //EMBEDDED ENVIRONMENT RELATED FUNCTIONS//
  //////////////////////////////////////////

  private def startServices: Boolean = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** KamanjaEnvironmentManager has not been initialized. Please call def init first.")
    }

    try {
      val zkStartCode = startZookeeper

      // Sleeping for 1 second to give zookeeper time to fully start up so kafka doesn't spit out a bunch of errors
      Thread sleep 1000
      val kafkaStartCode = startKafka

      mdMan = new MetadataManager
      mdMan.setSSLPassword("")
      mdMan.initMetadataCfg(this.metadataConfigFile)

      val addConfigResult = mdMan.addConfig(new File(EmbeddedConfiguration.generateClusterConfigFile(kamanjaInstallDir, kafkaCluster.getBrokerList, embeddedZookeeper.getConnection)))
      if (addConfigResult != 0) {
        logger.error("***ERROR*** Attempted to upload cluster configuration but failed")
        return false
      }
      logger.info("Cluster configuration successfully uploaded")

      //val addSystemBindingsResult = mdMan.addBindings(this.getClass.getResource("/SystemMsgs_Adapter_Bindings.json").getPath)
      //val addSystemBindingsResult = mdMan.addBindings(kamanjaInstallDir + "/config/SystemMsgs_Adapter_Bindings.json")

      //Creating topics from the cluster config adapters
      //val kafkaTestClient = new KafkaTestClient(embeddedZookeeper.getConnection)
      //clusterConfig.adapters.foreach(adapter => {
      //  kafkaTestClient.createTopic(adapter.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName, 1, 1)
      //})

      return zkStartCode && kafkaStartCode && startKamanja //&& startKafkaConsumer
    }
    catch {
      case e: Exception => throw new Exception("***ERROR*** Failed to start services", e)
    }
  }

  def stopServices: Boolean = {
    // Sleeping between each to give each one time to properly shut down to avoid errors
    val stopKamanjaCode = stopKamanja
    //val stopKafkaConsumerCode = stopKafkaConsumer
    //Thread sleep 1000
    val stopKafkaCode = stopKafka
    val stopZookeeperCode = stopZookeeper

    mdMan.shutdown

    if (stopKamanjaCode && stopKafkaCode && stopZookeeperCode /*&& stopKafkaConsumerCode*/ ) {
      isInitialized = false
      return true
    }
    return false
  }

  private def startKamanja: Boolean = {
    embeddedKamanjaManager = new EmbeddedKamanjaManager

    zkClient = new ZookeeperClient(embeddedZookeeper.getConnection)
    try {
      logger.info(s"Starting Kamanja with configuration file $kamanjaConfigFile...")
      val startCode = embeddedKamanjaManager.startup(this.kamanjaConfigFile, getZookeeperConfiguration, zkClient, 60)
      if (startCode != 0) {
        logger.error("***ERROR*** Failed to start Kamanja")
        return false
      }
      else {
        logger.info("Kamanja started")
      }
      return true
    }
    catch {
      case e: Exception => {
        logger.error("***ERROR*** Failed to start Kamanja\n" + logger.getStackTraceAsString(e))
        return false
      }
    }
  }

  private def stopKamanja: Boolean = {
    try {
      logger.info("Stopping Kamanja...")
      val shutdownCode = embeddedKamanjaManager.shutdown(getZookeeperConfiguration, zkClient)
      if (shutdownCode != 0) {
        logger.error("***ERROR*** Failed to stop Kamanja. Return code: " + shutdownCode)
        false
      }
      else {
        logger.info("Kamanja stopped")
        return true
      }
    }
    catch {
      case e: Exception => {
        logger.error("***ERROR*** Failed to stop Kamanja\n" + logger.getStackTraceAsString(e))
        return false
      }
    }
  }

  private def startZookeeper: Boolean = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** KamanjaEnvironmentManager has not been initialized. Please call def init first.")
    }
    try {
      logger.info("Starting Zookeeper...")
      embeddedZookeeper.startup
      logger.info("Zookeeper started")
      true
    }
    catch {
      case e: Exception => {
        logger.error("***ERROR*** Failed to start Zookeeper\n" + logger.getStackTraceAsString(e))
        false
      }
    }
  }

  private def stopZookeeper: Boolean = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** KamanjaEnvironmentManager has not been initialized. Please call def init first.")
    }
    try {
      logger.info("Stopping Zookeeper...")
      embeddedZookeeper.shutdown
      logger.info("Zookeeper stopped")
      true
    }
    catch {
      case e: Exception => {
        logger.error("***ERROR* Failed to stop Zookeeper\n" + logger.getStackTraceAsString(e))
        false
      }
    }
  }

  private def startKafka: Boolean = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** KamanjaEnvironmentManager has not been initialized. Please call def init first.")
    }
    try {
      logger.info("Starting Kafka...")
      kafkaCluster.startCluster
      logger.info("Kafka started")
      true
    }
    catch {
      case e: Exception => {
        logger.error("***ERROR*** Failed to start Kafka\n" + logger.getStackTraceAsString(e))
        false
      }
    }
  }

  private def stopKafka: Boolean = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** KamanjaEnvironmentManager has not been initialized. Please call def init first.")
    }
    try {
      logger.info("Stopping Kafka...")
      kafkaCluster.stopCluster
      logger.info("Kafka stopped")
      true
    }
    catch {
      case e: Exception => {
        logger.error("***ERROR*** Failed to stop Kafka\n" + logger.getStackTraceAsString(e))
        false
      }
    }
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

    StorageConfiguration(storeType, schemaName, hostname)
  }
}
