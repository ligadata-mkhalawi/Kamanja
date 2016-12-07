package com.ligadata.kamanja.test.application

import java.io.{File, PrintWriter}
import java.util.NoSuchElementException

import com.ligadata.KamanjaManager.embedded._
import com.ligadata.test.configuration.cluster._
import com.ligadata.test.configuration.cluster.adapters._
import com.ligadata.test.configuration.cluster.adapters.interfaces._
import com.ligadata.test.configuration.cluster.nodes._
import com.ligadata.test.configuration.cluster.python.PythonConfiguration
import com.ligadata.test.configuration.cluster.zookeeper._
import com.ligadata.test.embedded.zookeeper._
import com.ligadata.kafkaInputOutputAdapters_v10.embedded._
import com.ligadata.test.utils._
import com.ligadata.MetadataAPI.test._
import com.ligadata.kamanja.test.application.logging.{KamanjaAppLogger, KamanjaAppLoggerException}
import com.ligadata.test.embedded.kafka._

import org.json4s._
import org.json4s.native.JsonMethods._

import scala.io.Source

case class EmbeddedServicesException(message: String, cause: Throwable = null) extends Exception(message, cause)

object EmbeddedServicesManager {
  private var embeddedKamanjaManager: EmbeddedKamanjaManager = _
  private var embeddedZookeeper: EmbeddedZookeeper = _
  private var kafkaCluster: EmbeddedKafkaCluster = _
  private var zkClient: ZookeeperClient = _
  private var clusterConfig: Cluster = _
  private var kafkaConsumer: TestKafkaConsumer = _
  private var kamanjaInstallDir: String = _
  private var isInitialized: Boolean = false
  private var mdMan = new MetadataManager
  private var logger: KamanjaAppLogger = _
  var kamanjaConfigFile: String = _
  var clusterConfigFile: String = _
  var metadataConfigFile: String = _
  var storageDir: String = _

  def getInputKafkaAdapterConfig: KafkaAdapterConfig = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** EmbeddedServicesManager has not been initialized. Please call def init first.")
    }
    return clusterConfig.adapters.filter(_.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName.toLowerCase == "testin_1")(0).asInstanceOf[KafkaAdapterConfig]
  }

  def getOutputKafkaAdapterConfig: KafkaAdapterConfig = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** EmbeddedServicesManager has not been initialized. Please call def init first.")
    }
    return clusterConfig.adapters.filter(_.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName.toLowerCase == "testout_1")(0).asInstanceOf[KafkaAdapterConfig]
  }

  def getErrorKafkaAdapterConfig: KafkaAdapterConfig = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** EmbeddedServicesManager has not been initialized. Please call def init first.")
    }
    return clusterConfig.adapters.filter(_.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName.toLowerCase == "testfailedevents_1")(0).asInstanceOf[KafkaAdapterConfig]
  }

  def getEventKafkaAdapterConfig: KafkaAdapterConfig = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** EmbeddedServicesManager has not been initialized. Please call def init first.")
    }
    return clusterConfig.adapters.filter(_.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName.toLowerCase == "testmessageevents_1")(0).asInstanceOf[KafkaAdapterConfig]
  }

  def getCluster: Cluster = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** EmbeddedServicesManager has not been initialized. Please call def init first.")
    }
    return clusterConfig
  }

  def init(kamanjaInstallDir: String, metadataConfigFile: String = null, clusterConfigFile: String = null): Unit = {
    isInitialized = true

    if(metadataConfigFile != null && clusterConfigFile == null)
      throw new EmbeddedServicesException("***ERROR*** A MetadataAPIConfig file has been provided but a Cluster Configuration file has not. Please pass in a Cluster Configuration file.")
    else if(metadataConfigFile == null && clusterConfigFile != null)
      throw new EmbeddedServicesException("***ERROR*** A Cluster Configuration file has been provided but a MetadataAPIConfig file has not. Please pass in a MetadataAPIConfig file.")

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
    kamanjaConfigFile = TestUtils.constructTempDir("/kamanja-tmp-config").getAbsolutePath + "/kamanja.conf"
    embeddedZookeeper = new EmbeddedZookeeper
    kafkaCluster = new EmbeddedKafkaCluster().
      withBroker(new KafkaBroker(1, embeddedZookeeper.getConnection))
    if (clusterConfigFile == null) {
      clusterConfig = generateClusterConfiguration
    }
    else {
      clusterConfig = generateClusterConfigFromFile(clusterConfigFile)
    }
    kafkaConsumer = new TestKafkaConsumer(getOutputKafkaAdapterConfig)
  }

  def startServices: Boolean = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** EmbeddedServicesManager has not been initialized. Please call def init first.")
    }

    val classPath: String = {
      List(
        s"ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
        s"ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
        s"KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
      ).mkString(s"$kamanjaInstallDir/lib/system/", s":$kamanjaInstallDir/lib/system/", "")
    }

    try {
      val zkStartCode = startZookeeper

      // Sleeping for 1 second to give zookeeper time to fully start up so kafka doesn't spit out a bunch of errors
      Thread sleep 1000
      val kafkaStartCode = startKafka

      mdMan = new MetadataManager
      mdMan.setSSLPassword("")
      mdMan.initMetadataCfg(new MetadataAPIProperties(H2DBStore.name, H2DBStore.connectionMode, storageDir, kamanjaInstallDir, "kamanja", classPath, zkConnStr = embeddedZookeeper.getConnection, systemJarPath = s"$kamanjaInstallDir/lib/system", appJarPath = s"$kamanjaInstallDir/lib/application"))

      val addConfigResult = mdMan.addConfig(clusterConfig)
      if (addConfigResult != 0) {
        logger.error("***ERROR*** Attempted to upload cluster configuration but failed")
        return false
      }
      logger.info("Cluster configuration successfully uploaded")

      //val addSystemBindingsResult = mdMan.addBindings(this.getClass.getResource("/SystemMsgs_Adapter_Bindings.json").getPath)
      //val addSystemBindingsResult = mdMan.addBindings(kamanjaInstallDir + "/config/SystemMsgs_Adapter_Bindings.json")
      val systemAdapterBindings: String =
        """
          |[
          |  {
          |    "AdapterName": "TestStatus_1",
          |    "MessageNames": [
          |      "com.ligadata.KamanjaBase.KamanjaStatusEvent"
          |    ],
          |    "Serializer": "com.ligadata.kamanja.serializer.csvserdeser",
          |    "Options": {
          |      "alwaysQuoteFields": false,
          |      "fieldDelimiter": ","
          |    }
          |  },
          |  {
          |    "AdapterName": "TestFailedEvents_1",
          |    "MessageNames": [
          |      "com.ligadata.KamanjaBase.KamanjaExecutionFailureEvent"
          |    ],
          |    "Serializer": "com.ligadata.kamanja.serializer.jsonserdeser",
          |    "Options": {
          |    }
          |  },
          |  {
          |    "AdapterName": "TestMessageEvents_1",
          |    "MessageNames": [
          |      "com.ligadata.KamanjaBase.KamanjaMessageEvent"
          |    ],
          |    "Serializer": "com.ligadata.kamanja.serializer.jsonserdeser",
          |    "Options": {
          |    }
          |  }
          |]
        """.stripMargin

      mdMan.addBindingsFromString(systemAdapterBindings)

      //Creating topics from the cluster config adapters
      val kafkaTestClient = new KafkaTestClient(embeddedZookeeper.getConnection)
      clusterConfig.adapters.foreach(adapter => {
        kafkaTestClient.createTopic(adapter.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName, 1, 1)
      })

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

    // Generating a Kamanja Configuration file in a temporary directory
    new PrintWriter(kamanjaConfigFile) {
      write("NODEID=1\n")
      write(s"""MetadataDataStore={"StoreType": "h2db", "connectionMode": "embedded", "SchemaName": "kamanja", "Location": "$storageDir", "portnumber": "9100", "user": "test", "password": "test"}""")
      close()
    }

    zkClient = new ZookeeperClient(embeddedZookeeper.getConnection)
    try {
      logger.info(s"Starting Kamanja with configuration file $kamanjaConfigFile...")
      val startCode = embeddedKamanjaManager.startup(kamanjaConfigFile, clusterConfig.zookeeperConfig, zkClient)
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
      val shutdownCode = embeddedKamanjaManager.shutdown(clusterConfig.zookeeperConfig, zkClient)
      if (shutdownCode != 0) {
        logger.error("***ERROR*** Failed to stop Kamanja. Return code: " + shutdownCode)
        return false
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
      throw new Exception("***ERROR*** EmbeddedServicesManager has not been initialized. Please call def init first.")
    }
    try {
      logger.info("Starting Zookeeper...")
      embeddedZookeeper.startup
      logger.info("Zookeeper started")
      return true
    }
    catch {
      case e: Exception => {
        logger.error("***ERROR*** Failed to start Zookeeper\n" + logger.getStackTraceAsString(e))
        return false
      }
    }
  }

  private def stopZookeeper: Boolean = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** EmbeddedServicesManager has not been initialized. Please call def init first.")
    }
    try {
      logger.info("Stopping Zookeeper...")
      embeddedZookeeper.shutdown
      logger.info("Zookeeper stopped")
      return true
    }
    catch {
      case e: Exception => {
        logger.error("***ERROR* Failed to stop Zookeeper\n" + logger.getStackTraceAsString(e))
        return false
      }
    }
  }

  private def startKafka: Boolean = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** EmbeddedServicesManager has not been initialized. Please call def init first.")
    }
    try {
      logger.info("Starting Kafka...")
      kafkaCluster.startCluster
      logger.info("Kafka started")
      return true
    }
    catch {
      case e: Exception => {
        logger.error("***ERROR*** Failed to start Kafka\n" + logger.getStackTraceAsString(e))
        return false
      }
    }
  }

  private def stopKafka: Boolean = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** EmbeddedServicesManager has not been initialized. Please call def init first.")
    }
    try {
      logger.info("Stopping Kafka...")
      kafkaCluster.stopCluster
      logger.info("Kafka stopped")
      return true
    }
    catch {
      case e: Exception => {
        logger.error("***ERROR*** Failed to stop Kafka\n" + logger.getStackTraceAsString(e))
        return false
      }
    }
  }

  private def startKafkaConsumer: Boolean = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** EmbeddedServicesManager has not been initialized. Please call def init first.")
    }
    try {
      logger.info(s"Starting Kafka consumer against topic '${getOutputKafkaAdapterConfig.adapterSpecificConfig.topicName}'...")
      val kafkaConsumerThread = new Thread(kafkaConsumer)
      kafkaConsumerThread.start()
      logger.info(s"Kafka consumer started against topic '${getOutputKafkaAdapterConfig.adapterSpecificConfig.topicName}'")
      return true
    }
    catch {
      case e: Exception => {
        logger.error(s"***ERROR*** Failed to start kafka consumer against topic '${getOutputKafkaAdapterConfig.adapterSpecificConfig.topicName}'\n${logger.getStackTraceAsString(e)}")
        throw new Exception(s"***ERROR*** Failed to start kafka consumer against topic '${getOutputKafkaAdapterConfig.adapterSpecificConfig.topicName}'", e)
      }
    }
  }

  private def stopKafkaConsumer: Boolean = {
    if (!isInitialized) {
      throw new Exception("***ERROR*** EmbeddedServicesManager has not been initialized. Please call def init first.")
    }
    if (kafkaConsumer != null) {
      try {
        logger.info("Stopping Kafka consumer...")
        kafkaConsumer.shutdown

        logger.info("Kafka consumer stopped")
        return true
      }
      catch {
        case e: Exception => {
          logger.error("***ERROR*** Failed to stop Kafka consumer\n" + logger.getStackTraceAsString(e))
          throw new Exception("***ERROR*** Failed to stop Kafka consumer", e)
        }
      }
    }
    return true
  }

  private def generateClusterConfigFromFile(clusterConfigFile: String) : Cluster = {
    val file = new File(clusterConfigFile)
    if (!file.exists())
      throw new EmbeddedServicesException(s"Cluster Configuration file $clusterConfigFile does not exist")
    val source = Source.fromFile(file)
    val clusterCfgStr = source.getLines().mkString
    source.close()

    val json = parse(clusterCfgStr)
    implicit val defaults = org.json4s.DefaultFormats
    val expectedResultsList = json.extract[List[Map[String, Any]]]
    expectedResultsList.foreach(map => {
      map.keySet.foreach(key => {
        println(s"Key => $key")
        println(s"Value => ${map(key)}")
      })
    })
    return null
  }

  private def generateClusterConfiguration(): Cluster = {
    val zkConfig: ZookeeperConfig = new ZookeeperConfig(zookeeperConnStr = embeddedZookeeper.getConnection)

    val pythonConfig: PythonConfiguration = new PythonConfiguration(kamanjaInstallDir = kamanjaInstallDir, pythonBinDir = sys.env("PYTHON_HOME") + "/bin")

    val inputAdapter: KafkaAdapterConfig = new KafkaAdapterBuilder()
      .withAdapterSpecificConfig(new KafkaAdapterSpecificConfig(kafkaCluster.getBrokerList, "testin_1"))
      .withAdapterType(InputAdapter)
      .withName("TestIn_1")
      .withTenantId(Globals.kamanjaTestTenant)
      .build()

    val outputAdapter: KafkaAdapterConfig = new KafkaAdapterBuilder().
      withAdapterSpecificConfig(new KafkaAdapterSpecificConfig(kafkaCluster.getBrokerList, "testout_1")).
      withAdapterType(OutputAdapter).
      withName("TestOut_1").
      withTenantId(Globals.kamanjaTestTenant).
      build()

    val statusAdapter: KafkaAdapterConfig = new KafkaAdapterBuilder().
      withAdapterSpecificConfig(new KafkaAdapterSpecificConfig(kafkaCluster.getBrokerList, "teststatus_1")).
      withAdapterType(OutputAdapter).
      withName("TestStatus_1").
      withTenantId("System").
      build()

    val exceptionAdapter: KafkaAdapterConfig = new KafkaAdapterBuilder()
      .withAdapterSpecificConfig(new KafkaAdapterSpecificConfig(kafkaCluster.getBrokerList, "testfailedevents_1"))
      .withAdapterType(OutputAdapter)
      .withName("TestFailedEvents_1")
      .withTenantId("System")
      .build()

    val messageEventAdapter: KafkaAdapterConfig = new KafkaAdapterBuilder()
      .withAdapterSpecificConfig(new KafkaAdapterSpecificConfig(kafkaCluster.getBrokerList, "testmessageevents_1"))
      .withAdapterType(OutputAdapter)
      .withName("TestMessageEvents_1")
      .withTenantId("System")
      .build()

    storageDir = TestUtils.constructTempDir("/h2db").getAbsolutePath + "/storage"

    val clusterDataStore: StorageAdapter = new StorageConfiguration(H2DBStore, "kamanja", storageDir)

    val tenant1DataStore: StorageAdapter = new StorageConfiguration(H2DBStore, Globals.kamanjaTestTenant, storageDir)

    val classPath: String = {
      List(
        s"ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
        s"ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
        s"KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
      ).mkString(s".:$kamanjaInstallDir/lib/system/", s":$kamanjaInstallDir/lib/application/", "")
    }

    val node1 = new NodeBuilder()
      .withNodeId("1")
      .withNodePort(6541)
      .withNodeIpAddr("localhost")
      .withJarPaths(Array(s"$kamanjaInstallDir/lib/system", s"$kamanjaInstallDir/lib/application"))
      .withScalaHome(System.getenv("SCALA_HOME"))
      .withJavaHome(System.getenv("JAVA_HOME"))
      .withClassPath(classPath)
      .build

    val envContext = new EnvironmentContextConfig()

    val cluster = new ClusterBuilder()
      .withId("testcluster")
      .withAdapter(inputAdapter)
      .withAdapter(outputAdapter)
      .withAdapter(statusAdapter)
      .withAdapter(exceptionAdapter)
      .withAdapter(messageEventAdapter)
      .withEnvContext(envContext)
      .withNode(node1)
      .withSystemCatalog(clusterDataStore)
      .withClusterCacheConfig(new ClusterCacheConfig())
      .withTenant(new TenantConfiguration(Globals.kamanjaTestTenant, "Kamanja Test Tenant", new TenantCacheConfig(), tenant1DataStore))
      .withZkInfo(zkConfig)
      .withPythonConfig(pythonConfig)
      .build()

    return cluster
  }
}
