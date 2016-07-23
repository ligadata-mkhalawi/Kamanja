package com.ligadata.utils

import java.util
import java.util.Properties

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.{ProducerRecord, KafkaProducer}
import org.apache.kafka.common.{TopicPartition, PartitionInfo}

import scala.actors.threadpool._



object KafkaDriver {

  var pprops = new Properties()
  pprops.put("bootstrap.servers", "localhost:9092")
  pprops.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
  pprops.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

  var cprops = new Properties()
  cprops.put("bootstrap.servers", "localhost:9092")
  cprops.put("enable.auto.commit", "false")
  cprops.put("auto.offset.reset", "latest")
  cprops.put("session.timeout.ms", "30000")
  cprops.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  cprops.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

  def main(args: Array[String]): Unit = {

    var testPool: ExecutorService = scala.actors.threadpool.Executors.newFixedThreadPool(2)

    var topic = "dummyTopic"
    var message = "Sent data"

    println("Starting a Producer...")
    testPool.execute(new Runnable() {
      override def run() = {
        println("Creating Producer")
        var producer: KafkaProducer[Array[Byte],Array[Byte]] = new KafkaProducer[Array[Byte],Array[Byte]](pprops);
        println("Producer created")
        try {
          var count = 0L
          while (true) {
            producer.send(new ProducerRecord[Array[Byte], Array[Byte]](topic, (message + "<" + count + ">").getBytes))
            println("message sent -> " + (message + "<" + count + ">"))
            Thread.sleep(4000)
            count += 1
          }

        } catch {
          case e: Exception => println(e.printStackTrace())
        } finally {
          producer.close
        }
      }
    })

    println("Starting Consumer....")
    testPool.execute(new Runnable() {
      override def run() = {
        println("Creating Consumer")
        var consumer = new org.apache.kafka.clients.consumer.KafkaConsumer[String,String] (cprops)
        println("Consumer created, quering Partitions")
        var results: java.util.List[PartitionInfo] = consumer.partitionsFor(topic)
        println("This topic has " + results.size + " Partitions")

        var partitions: util.ArrayList[TopicPartition] = new util.ArrayList[TopicPartition]()
        var iter = results.iterator()
        while (iter.hasNext) {
          var tp = iter.next.asInstanceOf[PartitionInfo]
          println(tp.topic + " --->" + tp.partition)
          var thisTopicPartition = new TopicPartition(topic, tp.partition)
          partitions.add(thisTopicPartition)
        }

        // Assign and see to the beginning.
        consumer.assign(partitions)
        consumer.seekToBeginning(partitions)

        println("Consumer beginning poling")
        try {
          while (true) {
            var records = (consumer.poll(25)).iterator
            while (records.hasNext) {
              var record: ConsumerRecord[String, String] = records.next
              val message: Array[Byte] = record.value.getBytes()
              println(("MESSAGE Consumed -> at @partition " + record.partition + " @offset " + record.offset + " " + new String(message)))
            }
          }
        } catch {
          case e: Exception =>  println(e.printStackTrace())
        } finally {
          consumer.close
        }
      }
    })
  }

}
