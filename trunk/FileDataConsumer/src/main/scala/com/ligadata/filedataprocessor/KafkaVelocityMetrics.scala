package com.ligadata.filedataprocessor

import com.ligadata.VelocityMetrics._
import java.util.{ TimeZone, Properties, Date, Arrays }
import org.apache.kafka.clients.producer.{ Callback, RecordMetadata, KafkaProducer, ProducerRecord }
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.apache.logging.log4j.{ Logger, LogManager }
import scala.collection.mutable.ArrayBuffer
import java.util.concurrent.{ TimeUnit, Future }

class KafkaVelocityMetrics(inConfiguration: scala.collection.mutable.Map[String, String]) extends VelocityMetricsCallback {

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  var velocitymetricsTopic: String = null;
  // private JSONObject currentStatus = new JSONObject();
  var componentName: String = "unknown";
  val MAX_RETRY = 1
  val INIT_KAFKA_UNAVAILABLE_WAIT_VALUE = 1000
  val MAX_WAIT = 60000
  val allowedSize: Int = inConfiguration.getOrElse(SmartFileAdapterConstants.MAX_MESSAGE_SIZE, "65536").toInt

  var currentSleepValue = INIT_KAFKA_UNAVAILABLE_WAIT_VALUE
  // Set up some properties for the Kafka Producer
  val props = new Properties()
  props.put(org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, inConfiguration.get(SmartFileAdapterConstants.KAFKA_BROKER).get)
  props.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
  props.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
  props.put("request.required.acks", inConfiguration.getOrElse(SmartFileAdapterConstants.KAFKA_ACK, "0"))

  // Add all the new security paramterst
  val secProt = inConfiguration.getOrElse(SmartFileAdapterConstants.SEC_PROTOCOL, "plaintext").toString
  if (secProt.length > 0 &&
    !secProt.equalsIgnoreCase("plaintext")) {

    props.put(SmartFileAdapterConstants.SEC_PROTOCOL, secProt)

    // SSL_KEY_PASSWORD
    val sslkeypassword = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_KEY_PASSWORD, "").toString
    if (sslkeypassword.length > 0)
      props.put(SmartFileAdapterConstants.SSL_KEY_PASSWORD, sslkeypassword)

    // SSL_KEYSTORE_PASSWORD
    val sslkeystorepassword = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_KEYSTORE_PASSWORD, "").toString
    if (sslkeystorepassword.length > 0)
      props.put(SmartFileAdapterConstants.SSL_KEYSTORE_PASSWORD, sslkeystorepassword)

    // SSL_KEYSTORE_LOCATION
    val sslkeystorelocation = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_KEYSTORE_LOCATION, "").toString
    if (sslkeystorelocation.length > 0)
      props.put(SmartFileAdapterConstants.SSL_KEYSTORE_LOCATION, sslkeystorelocation)

    // SSL_KEYSTORE_TYPE
    val sslkeystoretype = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_KEYSTORE_TYPE, "").toString
    if (sslkeystoretype.length > 0)
      props.put(SmartFileAdapterConstants.SSL_KEYSTORE_TYPE, sslkeystoretype)

    //SSL_TRUSTSTORE_PASSWORD
    val ssltruststorepassword = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_TRUSTSTORE_PASSWORD, "").toString
    if (ssltruststorepassword.length > 0)
      props.put(SmartFileAdapterConstants.SSL_TRUSTSTORE_PASSWORD, ssltruststorepassword)

    //SSL_TRUSTSTORE_LOCATION
    val ssltruststorelocation = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_TRUSTSTORE_LOCATION, "").toString
    if (ssltruststorelocation.length > 0)
      props.put(SmartFileAdapterConstants.SSL_TRUSTSTORE_LOCATION, ssltruststorelocation)

    //SSL_TRUSTSTORE_TYPE
    val ssltruststoretype = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_TRUSTSTORE_TYPE, "").toString
    if (ssltruststoretype.length > 0)
      props.put(SmartFileAdapterConstants.SSL_TRUSTSTORE_TYPE, ssltruststoretype)

    //SSL_ENABLED_PROTOCOLS
    val sslenabledprotocols = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_ENABLED_PROTOCOLS, "").toString
    if (sslenabledprotocols.length > 0)
      props.put(SmartFileAdapterConstants.SSL_ENABLED_PROTOCOLS, sslenabledprotocols)

    //SSL_PROTOCOL
    val sslprotocol = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_PROTOCOL, "").toString
    if (sslprotocol.length > 0)
      props.put(SmartFileAdapterConstants.SSL_PROTOCOL, sslprotocol)

    // SSL_PROVIDER
    val sslprovider = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_PROVIDER, "").toString
    if (sslprovider.length > 0)
      props.put(SmartFileAdapterConstants.SSL_PROVIDER, sslprovider)

    // SSL_CIPHER_SUITES
    val sslciphersuites = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_CIPHER_SUITES, "").toString
    if (sslciphersuites.length > 0)
      props.put(SmartFileAdapterConstants.SSL_CIPHER_SUITES, sslciphersuites)

    //SSL_ENDPOINT_IDENTIFICATION_ALGORITHM
    val sslendpointidentificationalgorithm = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM, "").toString
    if (sslendpointidentificationalgorithm.length > 0)
      props.put(SmartFileAdapterConstants.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM, sslendpointidentificationalgorithm)

    // SSL_KEYMANAGER_ALGORITHM
    val sslkeymanageralgorithm = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_KEYMANAGER_ALGORITHM, "").toString
    if (sslkeymanageralgorithm.length > 0)
      props.put(SmartFileAdapterConstants.SSL_KEYMANAGER_ALGORITHM, sslkeymanageralgorithm)

    // SSL_TRUSTMANAGER_ALGORITHM
    val ssltrustmanageralgorithm = inConfiguration.getOrElse(SmartFileAdapterConstants.SSL_TRUSTMANAGER_ALGORITHM, "").toString
    if (ssltrustmanageralgorithm.length > 0)
      props.put(SmartFileAdapterConstants.SSL_TRUSTMANAGER_ALGORITHM, ssltrustmanageralgorithm)

    //SASL_KERBEROS_SERVICE_NAME
    val saslkerberosservicename = inConfiguration.getOrElse(SmartFileAdapterConstants.SASL_KERBEROS_SERVICE_NAME, "").toString
    if (saslkerberosservicename.length > 0)
      props.put(SmartFileAdapterConstants.SASL_KERBEROS_SERVICE_NAME, saslkerberosservicename)

    //SASL_KERBEROS_KINIT_CMD
    val saslkerberoskinitcmd = inConfiguration.getOrElse(SmartFileAdapterConstants.SASL_KERBEROS_KINIT_CMD, "").toString
    if (saslkerberoskinitcmd.length > 0)
      props.put(SmartFileAdapterConstants.SASL_KERBEROS_KINIT_CMD, saslkerberoskinitcmd)

    // SASL_MIN_TIME_BEFORE_RELOGIN
    val saslmintimebeforerelogic = inConfiguration.getOrElse(SmartFileAdapterConstants.SASL_MIN_TIME_BEFORE_RELOGIN, "").toString
    if (saslmintimebeforerelogic.length > 0)
      props.put(SmartFileAdapterConstants.SASL_MIN_TIME_BEFORE_RELOGIN, saslmintimebeforerelogic)

    // SASL_MECHANISM
    val saslmechanism = inConfiguration.getOrElse(SmartFileAdapterConstants.SASL_MECHANISM, "").toString
    if (saslmechanism.length > 0)
      props.put(SmartFileAdapterConstants.SASL_MECHANISM, saslmechanism)

    // SASL_KERBEROS_TICKET_RENEW_JITER
    val saslkerberosticketrenewjiter = inConfiguration.getOrElse(SmartFileAdapterConstants.SASL_KERBEROS_TICKET_RENEW_JITER, "").toString
    if (saslkerberosticketrenewjiter.length > 0)
      props.put(SmartFileAdapterConstants.SASL_KERBEROS_TICKET_RENEW_JITER, saslkerberosticketrenewjiter)

    //SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR
    val saslkerberosticketrenewwindowfactor = inConfiguration.getOrElse(SmartFileAdapterConstants.SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR, "").toString
    if (saslkerberosticketrenewwindowfactor.length > 0)
      props.put(SmartFileAdapterConstants.SASL_KERBEROS_TICKET_RENEW_WINDOW_FACTOR, saslkerberosticketrenewwindowfactor)
  }

  // create the producer object
  // val producer = new KafkaProducer[Array[Byte], Array[Byte]](new ProducerConfig(props))
  var producer = new KafkaProducer[Array[Byte], Array[Byte]](props)

  /**
   * Create an instance of KafkaStatsRecorder
   *
   * @throws Exception
   */

  def call(metrics: Metrics): Unit = {
    println("metrics " + metrics.metricsGeneratedTimeInMs)
    val json = ("uuid" -> metrics.uuid) ~
      ("metricsgeneratedtime" -> metrics.metricsGeneratedTimeInMs) ~
      ("metrics" -> metrics.compMetrics.toList.map(componentMetrics =>
        ("componentkey" -> componentMetrics.componentKey) ~
          ("nodeid" -> componentMetrics.nodeId) ~
          ("componentmetrics" -> componentMetrics.keyMetrics.toList.map(keymetrics =>
            ("key" -> keymetrics.key) ~
              ("metricstime" -> keymetrics.metricsTime) ~
              ("roundintervaltimeinsec" -> keymetrics.roundIntervalTimeInSec) ~
              ("firstoccured" -> keymetrics.firstOccured) ~
              ("lastoccured" -> keymetrics.lastOccured) ~
              ("metricsvalue" -> keymetrics.metricValues.toList.map(mValues =>
                ("metricskey" -> mValues.Key()) ~
                  ("metricskeyvalue" -> mValues.Value())))))));

    var outputJson: String = compact(render(json))
    logger.info("velocity metrics json.values " + json.values)
    logger.info("velocity metrics outputJson " + outputJson)
    val statusPartitionId = "velocity metrics"
    val keyMessages = new scala.collection.mutable.ArrayBuffer[ProducerRecord[Array[Byte], Array[Byte]]](1)
    keyMessages += new ProducerRecord(inConfiguration(SmartFileAdapterConstants.VM_KAFKA_TOPIC), statusPartitionId.getBytes("UTF8"), new String(outputJson).getBytes("UTF8"))
    logger.info("velocity metrics topic " + SmartFileAdapterConstants.VM_KAFKA_TOPIC)
    logger.info("velocity metrics topic " + inConfiguration(SmartFileAdapterConstants.VM_KAFKA_TOPIC))
    sendToKafka(keyMessages, "VelocityMetrics")

  }

  //private def sendToKafka(messages: ArrayBuffer[KeyedMessage[Array[Byte], Array[Byte]]]): Int = {
  private def sendToKafka(messages: ArrayBuffer[ProducerRecord[Array[Byte], Array[Byte]]], sentFrom: String, fullSuccessOffset: Int = 0, fileToUpdate: String = null): Int = {
    try {
      var partitionsStats = scala.collection.mutable.Map[Int, Int]()
      logger.info("File Data Consumer - Sending " + messages.size + " to kafka from " + sentFrom)
      if (messages.size == 0) return FileProcessor.KAFKA_SEND_SUCCESS

      var currentMessage = 0
      // Set up a map of messages to be used to verify if a message has been sent succssfully on not.
      val respFutures: scala.collection.mutable.Map[Int, Future[RecordMetadata]] = scala.collection.mutable.Map[Int, Future[RecordMetadata]]()
      messages.foreach(msg => {
        respFutures(currentMessage) = null
        currentMessage += 1
      })
      var successVector = Array.fill[Boolean](messages.size)(false) //Array[Boolean](messages.size)
      var gotResponse = Array.fill[Boolean](messages.size)(false) //Array[Boolean](messages.size)
      var isFullySent = false
      var isRetry = false
      var failedPush = 0

      logger.info("Sending " + messages.size + " messages to kafka ...")

      while (!LocationWatcher.shutdown && !isFullySent) {
        if (isRetry) {
          Thread.sleep(getCurrentSleepTimer)
        }

        currentMessage = 0
        messages.foreach(msg => {
          if ( //respFutures.contains(currentMessage) &&
          !successVector(currentMessage)) {
            // Send the request to Kafka
            val response = FileProcessor.executeCallWithElapsed(producer.send(msg, new Callback {
              override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
                val msgIdx = currentMessage
                if (exception != null) {
                  isFullySent = false
                  isRetry = true
                  failedPush += 1
                  logger.warn("File Data Consumer has detected a problem with pushing a message into the " + msg.topic + " will retry " + exception.getMessage)
                } else {
                  successVector(msgIdx) = true
                }
                gotResponse(msgIdx) = true
              }
            }), " Sending data to kafka")
            respFutures(currentMessage) = response
          }
          currentMessage += 1
        })
      }

    } catch {
      case e: Exception =>
        logger.error("File Data Consumer -  Could not add to the queue due to an Exception " + e.getMessage, e)
      case e: Throwable => {
        logger.error("File Data Consumer -  Could not add to the queue due to an Exception " + e.getMessage, e)
      }
    }
    FileProcessor.KAFKA_SEND_SUCCESS
  }
  private def getCurrentSleepTimer: Int = {
    currentSleepValue = currentSleepValue * 2
    logger.error("File Data Consumer detected a problem with Kafka... Retry in " + scala.math.min(currentSleepValue, MAX_WAIT) / 1000 + " seconds")
    currentSleepValue = scala.math.min(currentSleepValue, MAX_WAIT)
    currentSleepValue
  }

}