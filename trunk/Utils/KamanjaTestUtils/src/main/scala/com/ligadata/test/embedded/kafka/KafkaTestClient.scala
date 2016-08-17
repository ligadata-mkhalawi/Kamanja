package com.ligadata.test.embedded.kafka

import java.util.Properties

import com.ligadata.test.utils.KamanjaTestLogger

import kafka.admin.AdminUtils
import kafka.common.TopicExistsException
import kafka.utils._

import org.I0Itec.zkclient.{ZkClient, ZkConnection}

class KafkaTestClient(zookeeperConn: String) extends KamanjaTestLogger {

  val sessionTimeoutMs = 10000
  val connectionTimeoutMs = 10000

  def createTopics(topicNames: Seq[String], numPartitions: Int, replicationFactor: Int): Unit = {
    topicNames.foreach(topic => {
      createTopic(topic, numPartitions, replicationFactor)
    })
  }

  def createTopic(topicName: String, numPartitions: Int, replicationFactor: Int): Unit = {
    //val zkClient: ZkClient = new ZkClient(zookeeperConn, sessionTimeoutMs, connectionTimeoutMs, ZKStringSerializer)
//    val zkUtils: ZkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperConn), false)
    val zkClient = ZkUtils.createZkClient(zookeeperConn, sessionTimeoutMs, connectionTimeoutMs)
    val zkUtils: ZkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperConn), false)

    val topicConfig = new Properties()

    try {
      if (!AdminUtils.topicExists(zkUtils, topicName)) {
        logger.info("KAFKA-TEST-CLIENT: Creating Topic: " + topicName)
        AdminUtils.createTopic(zkUtils, topicName, numPartitions, replicationFactor, topicConfig)
        Thread sleep 3000
        logger.info("KAFKA-TEST-CLIENT: Topic Created: " + topicName)
      }
      else {
        logger.info("AUTOMAT ION-KAFKA: Topic '" + topicName + "' already exists")
      }
    }
    catch {
      case e: TopicExistsException => logger.info("KAFKA-TEST-CLIENT: Topic " + topicName + " already created. Continuing...")
      case e: Exception => throw new KafkaTestClientException("KAFKA-TEST-CLIENT: Failed to create topic '" + topicName + "' with error:\n" + e)
    }
    finally {
      if (zkClient != null) {
        zkClient.close()
      }
    }
  }

  /*
    This is commented out for now since a particular test library for kafka isn't being assembled properly with sbt assembly.
    Additionally, there are no usages for this code at the moment.

  def deleteTopic(topicName: String, kafkaBrokers: Seq[KafkaBroker]): Unit = {
    val zkClient = ZkUtils.createZkClient(zookeeperConn, sessionTimeoutMs, connectionTimeoutMs)
    val zkUtils: ZkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperConn), false)
    if (AdminUtils.topicExists(zkUtils, topicName)) {
      try {
        logger.info("KAFKA-TEST-CLIENT: Deleting topic {}", topicName)
        AdminUtils.deleteTopic(zkUtils, topicName)
        val servers = kafkaBrokers.map(broker => broker.kafkaServer)
        kafka.utils.TestUtils.waitUntilMetadataIsPropagated(servers, topicName, 0, 10000L)
      }
      catch {
        case e: Exception => throw new KafkaTestClientException(s"KAFKA-TEST-CLIENT: Failed to delete topic $topicName")
      }
    }
  }
  */
}
