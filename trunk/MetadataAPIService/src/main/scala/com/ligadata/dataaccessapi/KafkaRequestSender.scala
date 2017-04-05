package com.ligadata.dataaccessapi

import java.util.Properties
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future
import org.apache.logging.log4j.{ Logger, LogManager }
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata }

object KafkaRequestSender {
  lazy val log = LogManager.getLogger(this.getClass.getName)

  val maxRetries = 3
  val retryInterval = 1000
  val kafkaTimeout = 4000

  def get(config: Map[String, String]): KafkaRequestSender = {
      return new KafkaRequestSender(config)
  }

}

class KafkaRequestSender(config: Map[String, String]) {
  lazy val log = LogManager.getLogger(this.getClass.getName)
  private val lock = new Object()

  val topic = config("topicName")
  var producer: KafkaProducer[Array[Byte], Array[Byte]] = null
  var numPartitions: Int = 0
  val randomPartitionCntr = new java.util.Random

  def start() = lock.synchronized {
    try {
      connect()
    } catch {
      case e: Throwable => {
        if (producer != null) producer.close()
        log.warn("Error connecting to kafka: ", e);
        throw e
      }
    }
  }
  
  def stop() = lock.synchronized {
    if(producer != null) producer.close()
  }
  
  def connect() = {
    // Set up some properties for the Kafka Producer
    val props = new Properties()
    props.put("bootstrap.servers", config("kafkaHost"))
    props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    props.put("max.block.ms", KafkaRequestSender.kafkaTimeout.toString)
    props.put("retries", "0")

    for((k,v) <- config) if(!k.equals("topicName") && !k.equals("kafkaHost")) props.put(k, v)

    // create the producer object
    producer = new KafkaProducer[Array[Byte], Array[Byte]](props)
    log.debug("Kafka producer created for "+topic)
    numPartitions = producer.partitionsFor(topic).size()
    log.debug("Number of partitions = "+numPartitions)
  }

  def send(keys: Array[String], messages: Array[Array[Byte]]): Unit = {

    log.debug("Sending " + messages.length + " message(s) to kafka topic " + topic)
    val records = new Array[ProducerRecord[Array[Byte],Array[Byte]]](messages.length)
    for(i <- 0 to messages.length-1) {
      records(i) = new ProducerRecord[Array[Byte],Array[Byte]](topic, getPartition(keys(i)), keys(i).getBytes("UTF8"), messages(i))
    }

    val responses = new Array[Future[RecordMetadata]](messages.length)
    var isSent = Array.fill[Boolean](messages.size)(false)

    var retries = 0
    while (retries < KafkaRequestSender.maxRetries) {
      for (i <- 0 to records.length - 1) {
        if (!isSent(i))
          responses(i) = producer.send(records(i))
      }

      var error: Throwable = null
      for (i <- 0 to responses.length - 1) {
        try {
          responses(i).get(KafkaRequestSender.kafkaTimeout, java.util.concurrent.TimeUnit.MILLISECONDS)
          isSent(i) = true
        } catch {
          case e: Throwable => {
            log.warn("Kafka error: " + e.getMessage, e)
            error = e
          }
        }
      }

      retries += 1
      if(error == null)
        return
      else if(retries >= KafkaRequestSender.maxRetries)
        throw error

      log.debug("Could not send " + messages.length + " message(s) to kafka topic will retry in " + retries * KafkaRequestSender.retryInterval + " millisecs" )
      Thread.sleep(retries * KafkaRequestSender.retryInterval)
    }

  }

  def getPartition(key: String): Int = {
    if (numPartitions == 0) return 0
    if (key != null) {
      try {
        return scala.math.abs(key.hashCode() % numPartitions)
      } catch {
        case e: Throwable => { }
      }
    }
    return randomPartitionCntr.nextInt(numPartitions)
  }
}