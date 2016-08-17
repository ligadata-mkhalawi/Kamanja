package com.ligadata.test.application

import com.ligadata.KamanjaManager.embedded._
import com.ligadata.test.configuration.cluster.zookeeper.ZookeeperConfig
import com.ligadata.test.configuration.cluster._
import com.ligadata.test.configuration.cluster.adapters._
import com.ligadata.test.configuration.cluster.adapters.interfaces._
import com.ligadata.test.embedded.zookeeper._
import com.ligadata.kafkaInputOutputAdapters_v10.embedded._

object EmbeddedServicesManager {
  private var embeddedKamanjaManager: EmbeddedKamanjaManager = _
  private var embeddedZookeeper: EmbeddedZookeeper = _
  private var kafkaCluster: EmbeddedKafkaCluster = _

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

  private def generateClusterConfiguration: Cluster = {
    val zkConfig: ZookeeperConfig = new ZookeeperConfig(zookeeperConnStr = embeddedZookeeper.getConnection)



    return null
  }

  def startServices: Boolean = {
    return startZookeeper && startKafka
  }

  def stopServices: Boolean = {
    return stopKafka && stopZookeeper
  }
}
