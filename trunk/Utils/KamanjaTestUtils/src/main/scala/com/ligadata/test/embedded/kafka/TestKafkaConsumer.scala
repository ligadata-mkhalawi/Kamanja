package com.ligadata.test.embedded.kafka

import java.util.Properties

import com.ligadata.test.configuration.cluster.adapters.KafkaAdapterConfig
import com.ligadata.test.utils.{Globals, KamanjaTestLogger}
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.TopicPartition

import collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

class TestKafkaConsumer(config: KafkaAdapterConfig) extends Runnable with KamanjaTestLogger {
  private var consumer: KafkaConsumer[String, String] = null

  val props = new Properties() {
    put("bootstrap.servers", config.adapterSpecificConfig.hostList)
    put("group.id", "kamanja-test-group")
    put("enable.auto.commit", "true")
    put("auto.commit.interval.ms", "1000")
    put("session.timeout.ms", "30000")
    put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    //put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
    //put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
  }

  consumer = new KafkaConsumer[String, String](props)

  /**
    * def run will subscribe to the topic specified in the KafkaAdapterConfig passed in at class construction
    * and seek to the end of the topic prior to retrieving records.
    */
  def run: Unit = {
    try {
      consumer.subscribe(List(config.adapterSpecificConfig.topicName))
      val partitions = consumer.assignment()
      consumer.seekToEnd(partitions)
      while(true) {
        val records = consumer.poll(100).iterator()
        while(records.hasNext) {
          val record: ConsumerRecord[String, String] = records.next
          logger.debug("[Test Kafka Consumer]: Record: partition: " + record.partition)
          logger.debug("[Test Kafka Consumer]: Offset: " + record.offset)
          logger.debug("[Test Kafka Consumer]: Value: " + record.value)
          val outputBuffer: ListBuffer[String] = new ListBuffer[String]
          if (Globals.modelOutputResult.exists((_._1 == config.adapterSpecificConfig.topicName))) {
            outputBuffer ++= Globals.modelOutputResult(config.adapterSpecificConfig.topicName)
          }

          outputBuffer += record.value
          Globals.modelOutputResult(config.adapterSpecificConfig.topicName) = outputBuffer.toList
          logger.info("Output Message Added:\n\tTopic Name: " + config.adapterSpecificConfig.topicName + "\n\tMessage: " + record.value)
          logger.info(s"Total Number of output messages is: ${Globals.modelOutputResult(config.adapterSpecificConfig.topicName).length}")
        }
      }
    }
    catch {
      case e: org.apache.kafka.common.errors.WakeupException =>
      case e: Exception =>
        logger.error("[Test Kafka Consumer]: Failed to start consumer", e)
        throw new Exception("[Test Kafka Consumer]: Failed to start consumer", e)
    }
    finally {
      consumer.close()
    }
  }

  def shutdown: Unit = {
    consumer.wakeup
  }

}
