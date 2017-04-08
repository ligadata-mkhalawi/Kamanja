package com.ligadata.dataaccessapi

import java.util
import java.util.Properties

import org.apache.kafka.clients.consumer.{ ConsumerRecord, ConsumerRecords, KafkaConsumer }
import org.apache.kafka.common.{ TopicPartition, PartitionInfo }
import org.apache.logging.log4j.LogManager
import org.json4s.jackson.Serialization

import scala.actors.threadpool.{ ExecutorService, Executors }
import scala.collection.mutable.ArrayBuffer
import java.util.concurrent.atomic.AtomicLong
import com.ligadata.Utils.Utils

object KafkaResponseReader {

  val INITIAL_SLEEP = 500
  val MAX_SLEEP = 30000
  val POLLING_INTERVAL = 100

  def get(config: Map[String, String]): KafkaResponseReader = {
    return new KafkaResponseReader(config)
  }
}

class KafkaResponseReader(config: Map[String, String]) {
  private val LOG = LogManager.getLogger(getClass)
  private val lock = new Object()
  private var msgCount = new AtomicLong(0)
  private var sleepDuration = 500
  private var isShutdown = false

  val topic = config("topicName")
  //val partitions = config("topicPartitions").split(",").map(x => new TopicPartition(topic, x.toInt))
  val numberOfThreads = config.getOrElse("numberOfThreads", "1").toInt

  val props = new Properties()
  props.put("bootstrap.servers", config("kafkaHost"))

  // Default to some values..
  props.put("auto.offset.reset", "latest")
  props.put("session.timeout.ms", "30000")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("group.id", config.getOrElse("group.id", "flare-daas-api"))

  // Add the rest of the properties.
  for ((k, v) <- config) if (!k.equals("topicName") && !k.equals("kafkaHost") && !k.equals("numberOfThreads") && !k.equals("group.id")) props.put(k, v)

  var consumers = ArrayBuffer[KafkaConsumer[String, String]]()
  var readExecutor: ExecutorService = _

  def addConsumers(kafkaConsumer: org.apache.kafka.clients.consumer.KafkaConsumer[String, String]): Unit = synchronized {
    if (kafkaConsumer != null)
      consumers += kafkaConsumer
  }

  def getConsumers: Array[org.apache.kafka.clients.consumer.KafkaConsumer[String, String]] = synchronized {
    consumers.toArray
  }

  def getConsumersAndClear: Array[org.apache.kafka.clients.consumer.KafkaConsumer[String, String]] = synchronized {
    val retVal = consumers.toArray
    consumers.clear
    retVal
  }

  private def createConsumer(): KafkaConsumer[String, String] = {

    var intSleepTimer = KafkaResponseReader.INITIAL_SLEEP
    var done = false
    var kc: KafkaConsumer[String, String] = null
    while (!done && !isShutdown) {
      try {
        kc = new KafkaConsumer[String, String](props)
        kc.seekToEnd()
        done = true
      } catch {
        case e: Throwable => {
          LOG.warn("KafkaResponseReader Exception initializing consumer", e)
          if (kc != null) kc.close
          kc = null
          try {
            Thread.sleep(intSleepTimer)
            intSleepTimer = scala.math.max(KafkaResponseReader.MAX_SLEEP, intSleepTimer * 2)
          } catch {
            case ie: InterruptedException => {
              stop()
              LOG.warn("KafkaResponseReader - sleep interrupted, shutting down ")
              throw ie
            }
            case t: Throwable => {
              LOG.warn("KafkaResponseReader - sleep interrupted (UNKNOWN CAUSE), shutting down ", t)
              stop()
              throw t
            }
          }

        }
      }
    }

    return kc
  }

  def start(process: (String) => Unit): Unit = lock.synchronized {

    println("Started processing for topic " + topic)
    readExecutor = Executors.newFixedThreadPool(numberOfThreads, Utils.GetScalaThreadFactory(getClass.getName + "-readExecutor-%d"))

    // Start a KafkaConsumer for each thread
    (1 to numberOfThreads).foreach(i => {

      readExecutor.execute(new Runnable() {
        // Create the connection for this thread to use and SEEK to the end.
        var kafkaConsumer = createConsumer
        addConsumers(kafkaConsumer)
        
        kafkaConsumer.subscribe(java.util.Arrays.asList(topic))

        // This is the guy that keeps running processing messages.
        override def run(): Unit = {
          println("Starting to POLL ")
          var intSleepTimer = KafkaResponseReader.INITIAL_SLEEP
          while (!isShutdown) {
            try {
              //LOG.debug("next POLL ")
              var poll_records = (kafkaConsumer.poll(KafkaResponseReader.POLLING_INTERVAL))
              var records = poll_records.iterator
              //LOG.debug("Got messages from POLL :" + records.hasNext())
              while (records.hasNext && !isShutdown) {
                // Process this record
                var record: ConsumerRecord[String, String] = records.next
                println("MESSAGE-> at @partition " + record.partition + " @offset " + record.offset + " " + record.value)
                msgCount.incrementAndGet()
                process(record.value)
              }
              intSleepTimer = KafkaResponseReader.INITIAL_SLEEP
            } catch {
              case ie: org.apache.kafka.common.errors.WakeupException => {}
              case ie: InterruptedException => {
                LOG.warn("KafkaResponseReader - terminating reading for " + topic)
                throw ie
              }
              case e: Throwable => {
                LOG.warn("KafkaResponseReader Exception during Kafka Queue processing " + topic + ", cause: ", e)
                try {
                  Thread.sleep(intSleepTimer)
                  intSleepTimer = scala.math.max(KafkaResponseReader.MAX_SLEEP, intSleepTimer * 2)
                } catch {
                  case ie: org.apache.kafka.common.errors.WakeupException => {
                    throw ie
                  }
                  case ie: InterruptedException => {
                    LOG.warn("KafkaResponseReader - sleep interrupted, shutting down ", e)
                    if (kafkaConsumer != null) kafkaConsumer.close
                    kafkaConsumer = null
                    StopProcessing
                    throw ie
                  }
                  case t: Throwable => {
                    LOG.warn("KafkaResponseReader - sleep interrupted (UNKNOWN CAUSE), shutting down ", t)
                    if (kafkaConsumer != null) kafkaConsumer.close
                    kafkaConsumer = null
                    StopProcessing
                    throw t
                  }
                }
              }
            }
          }

          // Clean up all the existing connections
          try {
            if (kafkaConsumer != null) kafkaConsumer.close
            kafkaConsumer = null
          } catch {
            case e: Throwable => {
              LOG.warn("KamanjaKafkaConsumer Exception trying to close kafka connections ", e)
            }
          }
        }
      })
    })

  }

  /**
   *
   */
  def stop(): Unit = lock.synchronized {
    isShutdown = true
    StopProcessing
  }

  /**
   * Will stop all the running read threads only - a call to StartProcessing will restart the reading process
   */
  def StopProcessing(): Unit = {
    LOG.warn("StopProcessing")
    terminateReaderTasks
  }

  /**
   *  terminateReaderTasks - well, just what it says
   */
  private def terminateReaderTasks(): Unit = {
    val kafkaConsumers = getConsumersAndClear
    kafkaConsumers.foreach(kConsumer => {
      try {
        if (kConsumer != null)
          kConsumer.wakeup();
      } catch {
        case e: Throwable => {
        }
      }
    })

    if (readExecutor == null) return

    // Give the threads to gracefully stop their reading cycles, and then execute them with extreme prejudice.
    try {
      Thread.sleep(30000)
    } catch {
      case e: Throwable => {}
    }

    if (readExecutor != null) readExecutor.shutdownNow
    var cntr = 0
    while (readExecutor != null && readExecutor.isTerminated == false && cntr < 1001) {
      cntr += 1
      if (cntr % 1000 == 0) {
        throw new Exception("KAFKA_ADAPTER - Failed to terminate reader executor from Kafka. Waited for 100 secs.")
      }
      try {
        Thread.sleep(100)
      } catch {
        case e: Throwable => {}
      }
    }

    LOG.debug("KAFKA_ADAPTER - Shutdown Complete")
    readExecutor = null
  }

}