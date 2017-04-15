package com.ligadata.kafkaInputOutputAdapters_v9.embedded

import java.io.File
import java.io.FileNotFoundException
import java.util.Properties

//import kafka.server.{KafkaConfig, KafkaServer}
import kafka.server.KafkaConfig
import kafka.server.KafkaServer
import com.ligadata.test.utils._
import org.apache.logging.log4j.{LogManager, Logger}

case class EmbeddedKafkaException(message: String, cause: Throwable = null) extends Exception(message, cause)

class KafkaBroker(hostname: String, port: Int, private var brokerId: Int, zkConnStr: String) {
  private val logger: Logger = LogManager.getLogger(this.getClass)

  private var kafkaConfig: KafkaConfig = _
  private var isRunning = false
  private var isInitialized = false
  private val logDir = TestUtils.constructTempDir("kafka-logs-")
  private val _port = if(port == -1) TestUtils.getAvailablePort else port
  final var kafkaServer: KafkaServer = _

  def getHostname: String = hostname
  def getPort: Int = _port
  def getBrokerId: Int = brokerId
  def getLogDir: File = logDir
  def getZkConnStr: String = zkConnStr
  def getKafkaConfig: KafkaConfig = kafkaConfig

  def setBrokerId(id: Int) = {
    brokerId = id
  }

  def this(port: Int, brokerId: Int, zkConnStr: String){
    this("127.0.0.1", port, brokerId, zkConnStr)
  }

  def this(brokerId: Int, zkConnStr: String) {
    this("127.0.0.1", -1, brokerId, zkConnStr)
  }

  private def init(): Unit = {
    logger.info("[Embedded Kafka Broker]: Initializing Kafka Broker")
    val props = new Properties()

    props.put("host.name", "127.0.0.1")
    props.put("port", _port.toString)
    props.put("broker.id", brokerId.toString)
    props.put("log.dir", logDir.getAbsolutePath)
    props.put("zookeeper.connect", zkConnStr)
    props.put("controlled.shutdown.enable", "true")
    props.put("controlled.shutdown.max.retries", "0")
    props.put("delete.topic.enable", "true")
    kafkaConfig = new KafkaConfig(props)
    kafkaServer = new KafkaServer(kafkaConfig)
    isInitialized = true
  }

  def start(): Unit = {
    if (!isInitialized) {
      init()
    }
    if (!isRunning) {
      logger.info(s"[Embedded Kafka Broker]: Starting Kafka Broker:\n\tHost Name: $hostname\n\tID: $brokerId\n\tPort: ${_port}\n\tLog Directory: $logDir\n")
      try {
        TestUtils.retry(3) {
          kafkaServer.startup()
        }
      }
      catch {
        case e: Exception =>
          logger.error(s"[Embedded Kafka Broker]: Failed to start Kafka Broker:\n\tHost Name: $hostname\n\tID: $brokerId\n\tPort: ${_port}\n\tLog Directory: $logDir\n", e)
          throw EmbeddedKafkaException(s"[Embedded Kafka Broker]: Failed to start Kafka Broker:\n\tHost Name: $hostname\n\tID: $brokerId\n\tPort: ${_port}\n\tLog Directory: $logDir\n", e)
      }
      isRunning = true
    }
  }

  def stop(cleanup:Boolean = true): Unit = {
    if(isRunning) {
      logger.info(s"[Embedded Kafka Broker]: Stopping Kafka Broker:\n\tHost Name: $hostname\n\tID: $brokerId\n\tPort: ${_port}\n\tLog Directory: $logDir\n")
      kafkaServer.shutdown()
      kafkaServer.awaitShutdown()
      isRunning = false

      if(cleanup) {
        try {
          cleanLogDir()
        }
        catch {
          case e: FileNotFoundException => throw new EmbeddedKafkaException("[Embedded Kafka Cluster]: Unable to find file '" + this.logDir + "'", e)
        }
      }
    }
  }

  override def toString = {
    hostname + ":" + port
  }

  private def cleanLogDir(timeout: Int = 30): Unit = {
    var count = 0
    while(logDir.exists() && count < timeout) {
      if(count >= timeout) {
        throw new EmbeddedKafkaException(s"[Embedded Kafka Broker]: Failed to delete log directory $logDir")
      }
      count += 1
      if(TestUtils.deleteFile(logDir))
        return
      else
        Thread sleep 1000
    }
  }
}

class EmbeddedKafkaCluster {
  private val logger: Logger = LogManager.getLogger(this.getClass)
  private var isRunning = false
  private var brokers: List[KafkaBroker] = List()

  def getBrokers: List[KafkaBroker] = brokers

  def startCluster(): Unit = {
    if(brokers.isEmpty){
      throw new Exception("[Embedded Kafka Cluster]: No brokers found to start")
    }
    brokers.foreach(broker => {
      broker.start
    })
    isRunning = true
  }

  def stopCluster(): Unit = {
    if (brokers.isEmpty) {
      throw new Exception("[Embedded Kafka Cluster]: No brokers found to stop")
    }
    brokers.foreach(broker => broker.stop())
    brokers = List()
    isRunning = false
  }

  def withBroker(kafkaBroker: KafkaBroker): EmbeddedKafkaCluster = {
    logger.debug("[Embedded Kafka Cluster]: Adding Kafka Broker:")
    logger.debug("\tBroker ID: " + kafkaBroker.getBrokerId)
    logger.debug("\tBroker Hostname: " + kafkaBroker.getHostname)
    brokers = brokers :+ kafkaBroker
    if(isRunning) {
      kafkaBroker.start
    }
    return this
  }

  def getBrokerList: String = {
    val sb = new StringBuilder
    brokers.foreach(broker => {
      if(sb.nonEmpty) sb.append(",")
      sb.append(broker.getHostname).
        append(":").
        append(broker.getPort)
    })
    return sb.toString()
  }
}