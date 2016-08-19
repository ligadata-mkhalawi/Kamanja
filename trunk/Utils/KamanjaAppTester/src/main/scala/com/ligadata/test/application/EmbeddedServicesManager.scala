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

object EmbeddedServicesManager {
  private var embeddedKamanjaManager: EmbeddedKamanjaManager = _
  private var kamanjaConfigFile: String = _
  private var storageDir: String = _
  private var embeddedZookeeper: EmbeddedZookeeper = _
  private var kafkaCluster: EmbeddedKafkaCluster = _
  private var zkClient: ZookeeperClient = _
  private var clusterConfig: Cluster = _

  def startServices(kamanjaInstallDir: String): Boolean = {
    try {
      val zkStartCode = startZookeeper
      val kafkaStartCode = startKafka
      clusterConfig = generateClusterConfiguration(kamanjaInstallDir)
      val mdMan = new MetadataManager

      mdMan.setSSLPassword("")
      mdMan.initMetadataCfg(new MetadataAPIProperties(H2DBStore.name, H2DBStore.connectionMode, storageDir, zkConnStr = embeddedZookeeper.getConnection, systemJarPath = s"$kamanjaInstallDir/lib/system", appJarPath = s"$kamanjaInstallDir/lib/application"))

      val result = mdMan.addConfig(clusterConfig)
      if (result != 0) {
        println("[Kamanja Application Tester] - ***ERROR*** Attempted to upload cluster configuration but failed\n\t")
        return false
      }
      return zkStartCode && kafkaStartCode && startKamanja
    }
    catch {
      case e:Exception => throw new Exception("[Kamanja Application Tester] - ***ERROR*** Failed to start services", e)
    }
  }

  def stopServices: Boolean = {
    return stopKafka && stopZookeeper && stopKamanja
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
      println("[Kamanja Application Tester] - Starting Kamanja...")
      val startCode = embeddedKamanjaManager.startup(kamanjaConfigFile, clusterConfig.zookeeperConfig, zkClient)
      if (startCode != 0) {
        println("[Kamanja Application Tester] - ***ERROR*** Failed to start Kamanja")
      }
      else {
        println("[Kamanja Application Tester] - Kamanja started")
      }
      return true
    }
    catch {
      case e: Exception => {
        println("[Kamanja Application Tester] - ***ERROR*** Failed to start Kamanja\nCause: " + e)
        return false
      }
    }
  }

  private def stopKamanja: Boolean = {
    try {
      println("[Kamanja Application Tester] - Stopping Kamanja...")
      val shutdownCode = embeddedKamanjaManager.shutdown(clusterConfig.zookeeperConfig, zkClient)
      if(shutdownCode != 0){
        println("[Kamanja Application Tester] - ***ERROR*** Failed to stop Kamanja. Return code: " + shutdownCode)
        return false
      }
      else {
        println("[Kamanja Application Tester] - Kamanja stopped")
        return true
      }
    }
    catch {
      case e: Exception => {
        println("[Kamanja Application Tester] - ***ERROR*** Failed to stop Kamanja\nCause: " + e)
        return false
      }
    }
  }

  private def startZookeeper: Boolean = {
    embeddedZookeeper = new EmbeddedZookeeper
    try {
      println("[Kamanja Application Tester] - Starting Zookeeper...")
      embeddedZookeeper.startup
      println("[Kamanja Application Tester] - Zookeeper started")
      return true
    }
    catch {
      case e: Exception => {
        println("[Kamanja Application Tester] - ***ERROR*** Failed to start Zookeeper\nCause: " + e)
        return false
      }
    }
  }

  private def stopZookeeper: Boolean = {
    try {
      println("[Kamanja Application Tester] - Stopping Zookeeper...")
      embeddedZookeeper.shutdown
      println("[Kamanja Application Tester] - Zookeeper stopped")
      return true
    }
    catch {
      case e: Exception => {
        println("[Kamanja Application Tester - ***ERROR* Failed to stop Zookeeper\nCause: " + e)
        return false
      }
    }
  }

  private def startKafka: Boolean = {
    try {
      println("[Kamanja Application Tester] - Starting Kafka...")
      kafkaCluster = new EmbeddedKafkaCluster().
        withBroker(new KafkaBroker(1, embeddedZookeeper.getConnection))
      kafkaCluster.startCluster
      println("[Kamanja Application Tester] - Kafka started")
      return true
    }
    catch {
      case e: Exception => {
        println("[Kamanja Application Tester] - ***ERROR*** Failed to start Kafka\nCause: " + e)
        return false
      }
    }
  }

  private def stopKafka: Boolean = {
    try {
      println("[Kamanja Application Tester] - Stopping Kafka...")
      kafkaCluster.stopCluster
      println("[Kamanja Application Tester] - Kafka stopped")
      return true
    }
    catch {
      case e: Exception => {
        println("[Kamanja Application Tester] - ***ERROR*** Failed to stop Kafka\nCause: " + e)
        return false
      }
    }
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

    val cluster = new ClusterBuilder()
      .withId("testcluster")
      .withAdapter(inputAdapter)
      .withAdapter(outputAdapter)
      .withAdapter(statusAdapter)
      .withAdapter(exceptionAdapter)
      .withAdapter(messageEventAdapter)
      .withEnvContext(new EnvironmentContextConfig)
      .withNode(node1)
      .withSystemCatalog(clusterDataStore)
      .withClusterCacheConfig(new ClusterCacheConfig())
      .withTenant(new TenantConfiguration(Globals.kamanjaTestTenant, "Kamanja Test Tenant", new TenantCacheConfig(), tenant1DataStore))
      .withZkInfo(zkConfig)
      .build()

    return cluster
  }
}
