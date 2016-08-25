package com.ligadata.test.embedded.kafka

import java.util.Properties

import com.ligadata.test.configuration.cluster.adapters.KafkaAdapterConfig
import com.ligadata.test.utils.{Globals, KamanjaTestLogger}
import org.apache.kafka.clients.consumer._

import collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class TestKafkaConsumer(config: KafkaAdapterConfig) extends Runnable with KamanjaTestLogger {
  private var consumer: KafkaConsumer[String, String] = null

  val props = new Properties() {
    put("bootstrap.servers", config.adapterSpecificConfig.hostList)
    put("producer.type", "async")
    put("batch.num.messages", "1024")
    put("batch.size", "1024")
    put("queue.time", "50")
    put("queue.size", (16 * 1024 * 1024).toString)
    put("message.send.max.retries", "3")
    put("request.required.acks", "1")
    val bufferMemory: Integer = 64 * 1024 * 1024
    put("buffer.memory", bufferMemory.toString)
    put("buffer.size", bufferMemory.toString)
    put("socket.send.buffer", bufferMemory.toString)
    put("socket.receive.buffer", bufferMemory.toString)
    put("client.id", "Client1")
    put("group.id", "kamanja-auto-group")
    //props.put("partitioner.class", "com.ligadata.tools.CustPartitioner")
    put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  }

  consumer = new KafkaConsumer[String, String](props)

  def run: Unit = {
    try {
      consumer.subscribe(List(config.adapterSpecificConfig.topicName))
      while(true) {
        val records = consumer.poll(Long.MaxValue).iterator

        while(records.hasNext) {
          var record: ConsumerRecord[String, String] = records.next
          println("partition: " + record.partition)
          println("offset: " + record.offset)
          println("value: " + record.value)
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
        logger.error("AUTOMATION-KAFKA-CONSUMER: Failed to start consumer", e)
        throw new Exception("AUTOMATION-KAFKA-CONSUMER: Failed to start consumer", e)
    }
    finally {
      consumer.close()
    }
  }

  def shutdown: Unit = {
    consumer.wakeup
  }

}
