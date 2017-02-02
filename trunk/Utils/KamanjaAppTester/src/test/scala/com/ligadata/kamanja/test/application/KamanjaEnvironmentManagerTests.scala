package com.ligadata.kamanja.test.application

import com.ligadata.kamanja.test.application.logging.KamanjaAppLogger
import org.scalatest._

class KamanjaEnvironmentManagerTests extends FlatSpec with BeforeAndAfterAll {

  override def beforeAll: Unit = {
    KamanjaAppLogger.createKamanjaAppLogger(TestSetup.kamanjaInstallDir)
    KamanjaEnvironmentManager.init(TestSetup.kamanjaInstallDir, "/tmp/Kamanja/Kamanja-1.6.1_2.11/config/MetadataAPIConfig.properties", "/tmp/Kamanja/Kamanja-1.6.1_2.11/config/ClusterConfig_kafka_v10.json")
  }

  "getAllAdapters" should "retrieve a list of adapters that have been uploaded into cluster configuration" in {
    println(KamanjaEnvironmentManager.getAllAdapters)
  }

  "getZookeeperConfig" should "retrieve zookeeper configuration that has been uploaded into cluster configuration" in {
    println(KamanjaEnvironmentManager.getZookeeperConfiguration)
  }

  "getAllTenants" should "retrieve tenant configuration that has been uploaded into cluster configuration" in {
    println(KamanjaEnvironmentManager.getAllTenants)
  }

  "getSystemCatalog" should "retrieve the system catalog that has been uploaded into cluster configuration" in {
    println(KamanjaEnvironmentManager.getSystemCatalog)
  }

  "getAllNodes" should "retrieve a list of nodes that have been uploaded into cluster configuration" in {
    println(KamanjaEnvironmentManager.getAllNodes)
  }

  "getClusterCacheConfiguration" should "retrieve the cache configuration that has been uploaded into cluster configuration" in {
    println(KamanjaEnvironmentManager.getClusterCacheConfiguration)
  }

  "getEnvironmentContextConfiguration" should "retrieve the environment context configuration that has been uploaded into cluster configuration" in {
    println(KamanjaEnvironmentManager.getEnvironmentContextConfiguration)
  }

  "getPythonConfiguration" should "retrieve the python configuration that has been uploaded into cluster configuration" in {
    println(KamanjaEnvironmentManager.getPythonConfiguration)
  }

  "getClusterId" should "retrieve the cluster id that has been uploaded into cluster configuration" in {
    println(KamanjaEnvironmentManager.getClusterId)
  }
}