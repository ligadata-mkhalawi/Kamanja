package com.ligadata.test.application

import java.io.PrintWriter

import com.ligadata.KamanjaManager.embedded._
import com.ligadata.test.configuration.cluster._
import com.ligadata.test.configuration.cluster.adapters._
import com.ligadata.test.configuration.cluster.adapters.interfaces._
import com.ligadata.test.configuration.cluster.nodes._
import com.ligadata.test.configuration.cluster.zookeeper._
import com.ligadata.test.embedded.zookeeper._
import com.ligadata.kafkaInputOutputAdapters_v10.embedded._
import com.ligadata.test.utils._
import com.ligadata.MetadataAPI.test._
import com.ligadata.test.embedded.kafka._

object EmbeddedServicesManager {
  private var embeddedKamanjaManager: EmbeddedKamanjaManager = _
  private var kamanjaConfigFile: String = _
  private var storageDir: String = _
  private var embeddedZookeeper: EmbeddedZookeeper = _
  private var kafkaCluster: EmbeddedKafkaCluster = _
  private var zkClient: ZookeeperClient = _
  private var clusterConfig: Cluster = _
  private var kafkaConsumer: TestKafkaConsumer = _

  def getInputKafkaAdapterConfig: KafkaAdapterConfig = {
    if(clusterConfig != null) {
      throw new Exception("[Kamanja Application Tester] ---> ***ERROR*** Cluster Configuration has not been generated. Please run startServices first.")
    }
    else {
      val inputConfig = clusterConfig.adapters.filter(_.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName.toLowerCase == "testin_1")(0).asInstanceOf[KafkaAdapterConfig]
      return inputConfig
    }
  }

  def startServices(kamanjaInstallDir: String): Boolean = {
    val classPath: String = {
        List(
          s"ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
          s"ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
          s"KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
        ).mkString(s"$kamanjaInstallDir/lib/system/", s":$kamanjaInstallDir/lib/system/", "")
      }

    try {
      val zkStartCode = startZookeeper
      val kafkaStartCode = startKafka
      clusterConfig = generateClusterConfiguration(kamanjaInstallDir)

      val outputAdapterConfig: KafkaAdapterConfig = clusterConfig.adapters.filter(_.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName.toLowerCase == "testout_1")(0).asInstanceOf[KafkaAdapterConfig]

      val mdMan = new MetadataManager

      mdMan.setSSLPassword("")
      mdMan.initMetadataCfg(new MetadataAPIProperties(H2DBStore.name, H2DBStore.connectionMode, storageDir, "kamanja", classPath, zkConnStr = embeddedZookeeper.getConnection, systemJarPath = s"$kamanjaInstallDir/lib/system", appJarPath = s"$kamanjaInstallDir/lib/application"))

      val addConfigResult = mdMan.addConfig(clusterConfig)

      if (addConfigResult != 0) {
        println("[Kamanja Application Tester] ---> ***ERROR*** Attempted to upload cluster configuration but failed")
        return false
      }

      val addSystemBindingsResult = mdMan.addBindings(this.getClass.getResource("/SystemMsgs_Adapter_Bindings.json").getPath)

      //Creating topics from the cluster config adapters
      val kafkaTestClient = new KafkaTestClient(embeddedZookeeper.getConnection)
      clusterConfig.adapters.foreach(adapter => {
        kafkaTestClient.createTopic(adapter.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName, 1, 1)
      })


      return zkStartCode && kafkaStartCode && startKamanja && startKafkaConsumer(outputAdapterConfig)
    }
    catch {
      case e:Exception => throw new Exception("[Kamanja Application Tester] ---> ***ERROR*** Failed to start services", e)
    }
  }

  def stopServices: Boolean = {
    return stopKafkaConsumer && stopKafka && stopZookeeper && stopKamanja
  }

  private def startKamanja: Boolean = {
    embeddedKamanjaManager = new EmbeddedKamanjaManager

    // Generating a Kamanja Configuration file in a temporary directory
    val kamanjaConfigFile: String = TestUtils.constructTempDir("/kamanja-tmp-config").getAbsolutePath + "/kamanja.conf"
    new PrintWriter(kamanjaConfigFile) {
      write("NODEID=1\n")
      write(s"""MetadataDataStore={"StoreType": "h2db", "connectionMode": "embedded", "SchemaName": "kamanja", "Location": "$storageDir", "portnumber": "9100", "user": "test", "password": "test"}""")
      close()
    }

    zkClient = new ZookeeperClient(embeddedZookeeper.getConnection)
    try {
      println("[Kamanja Application Tester] ---> Starting Kamanja...")
      val startCode = embeddedKamanjaManager.startup(kamanjaConfigFile, clusterConfig.zookeeperConfig, zkClient)
      if (startCode != 0) {
        println("[Kamanja Application Tester] ---> ***ERROR*** Failed to start Kamanja")
        return false
      }
      else {
        println("[Kamanja Application Tester] ---> Kamanja started")
      }
      return true
    }
    catch {
      case e: Exception => {
        println("[Kamanja Application Tester] ---> ***ERROR*** Failed to start Kamanja\nCause: " + e)
        return false
      }
    }
  }

  private def stopKamanja: Boolean = {
    try {
      println("[Kamanja Application Tester] ---> Stopping Kamanja...")
      val shutdownCode = embeddedKamanjaManager.shutdown(clusterConfig.zookeeperConfig, zkClient)
      if(shutdownCode != 0){
        println("[Kamanja Application Tester] ---> ***ERROR*** Failed to stop Kamanja. Return code: " + shutdownCode)
        return false
      }
      else {
        println("[Kamanja Application Tester] ---> Kamanja stopped")
        return true
      }
    }
    catch {
      case e: Exception => {
        println("[Kamanja Application Tester] ---> ***ERROR*** Failed to stop Kamanja\nCause: " + e)
        return false
      }
    }
  }

  private def startZookeeper: Boolean = {
    embeddedZookeeper = new EmbeddedZookeeper
    try {
      println("[Kamanja Application Tester] ---> Starting Zookeeper...")
      embeddedZookeeper.startup
      println("[Kamanja Application Tester] ---> Zookeeper started")
      return true
    }
    catch {
      case e: Exception => {
        println("[Kamanja Application Tester] ---> ***ERROR*** Failed to start Zookeeper\nCause: " + e)
        return false
      }
    }
  }

  private def stopZookeeper: Boolean = {
    try {
      println("[Kamanja Application Tester] ---> Stopping Zookeeper...")
      embeddedZookeeper.shutdown
      println("[Kamanja Application Tester] ---> Zookeeper stopped")
      return true
    }
    catch {
      case e: Exception => {
        println("[Kamanja Application Tester] ---> ***ERROR* Failed to stop Zookeeper\nCause: " + e)
        return false
      }
    }
  }

  private def startKafka: Boolean = {
    try {
      println("[Kamanja Application Tester] ---> Starting Kafka...")
      kafkaCluster = new EmbeddedKafkaCluster().
        withBroker(new KafkaBroker(1, embeddedZookeeper.getConnection))
      kafkaCluster.startCluster
      println("[Kamanja Application Tester] ---> Kafka started")
      return true
    }
    catch {
      case e: Exception => {
        println("[Kamanja Application Tester] ---> ***ERROR*** Failed to start Kafka\nCause: " + e)
        return false
      }
    }
  }

  private def stopKafka: Boolean = {
    try {
      println("[Kamanja Application Tester] ---> Stopping Kafka...")
      kafkaCluster.stopCluster
      println("[Kamanja Application Tester] ---> Kafka stopped")
      return true
    }
    catch {
      case e: Exception => {
        println("[Kamanja Application Tester] ---> ***ERROR*** Failed to stop Kafka\nCause: " + e)
        return false
      }
    }
  }

  private def startKafkaConsumer(adapterConfig: KafkaAdapterConfig): Boolean = {
    kafkaConsumer = new TestKafkaConsumer(adapterConfig)
    try {
      println(s"[Kamanja Application Tester] ---> Starting Kafka consumer against topic '${adapterConfig.adapterSpecificConfig.topicName}'...")
      kafkaConsumer.run
      println(s"[Kamanja Application Tester] ---> Kafka consumer started against topic '${adapterConfig.adapterSpecificConfig.topicName}'")
      return true
    }
    catch {
      case e: Exception => {
       throw new Exception(s"[Kamanja Application Tester] ---> ***ERROR*** Failed to start kafka consumer against topic '${adapterConfig.adapterSpecificConfig.topicName}'", e)
      }
    }
  }

  private def stopKafkaConsumer: Boolean = {
    if (kafkaConsumer != null) {
      try {
        println("[Kamanja Application Tester] ---> Stopping Kafka consumer...")
        kafkaConsumer.shutdown
        println("[Kamanja Application Tester] ---> Kafka consumer stopped")
        return true
      }
      catch {
        case e: Exception => throw new Exception("[Kamanja Application Tester] ---> ***ERROR*** Failed to stop Kafka consumer", e)
      }
    }
    return true
  }

  private def generateClusterConfiguration(kamanjaInstallDir: String): Cluster = {
    val zkConfig: ZookeeperConfig = new ZookeeperConfig(zookeeperConnStr = embeddedZookeeper.getConnection)

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
      .build()

    return cluster
  }
}
