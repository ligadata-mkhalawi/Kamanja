
/*
 * Copyright 2015 ligaDATA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ligadata.kafkaInputOutputAdapters_v10

import java.util.{ Properties, Arrays }
import com.ligadata.KamanjaBase.{ContainerInterface, TransactionContext, NodeContext}
//import kafka.common.{ QueueFullException, FailedToSendMessageException }
import org.apache.logging.log4j.{ Logger, LogManager }
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.AdaptersConfiguration.{ KafkaConstants, KafkaQueueAdapterConfiguration }
import com.ligadata.Exceptions.{KamanjaException, FatalAdapterException}
import com.ligadata.HeartBeat.{Monitorable, MonitorComponentInfo}
import org.json4s.jackson.Serialization
import scala.collection.mutable.ArrayBuffer
import org.apache.kafka.clients.producer.{ Callback, RecordMetadata, ProducerRecord }
import org.apache.kafka.common.serialization.{ ByteArraySerializer /*, StringSerializer */ }
import java.util.concurrent.{ TimeUnit, Future }
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong
import scala.actors.threadpool.{ TimeUnit, ExecutorService, Executors }
import java.util.concurrent.locks.ReentrantReadWriteLock;


object KafkaProducer extends OutputAdapterFactory {
  def CreateOutputAdapter(inputConfig: AdapterConfiguration, nodeContext: NodeContext): OutputAdapter = new KafkaProducer(inputConfig, nodeContext)
  val HB_PERIOD = 5000

  // Statistics Keys
  val ADAPTER_DESCRIPTION = "Kafka 0.10.0.0 Producer Client"
  val SEND_MESSAGE_COUNT_KEY = "Messages Sent"
  val SEND_CALL_COUNT_KEY = "Send Call Count"
  val LAST_FAILURE_TIME = "Last_Failure"
  val LAST_RECOVERY_TIME = "Last_Recovery"
}

// http://kafka.apache.org/documentation.html
// New Producer configs are found @ http://kafka.apache.org/082/documentation.html#newproducerconfigs
// We still have ordering issues with Kafka. Once case is, if Kafka goes down and comes back and if we have list of new messages to send before it trigger failure, the new messages may go first

class KafkaProducer(val inputConfig: AdapterConfiguration, val nodeContext: NodeContext) extends OutputAdapter {
  private[this] val LOG = LogManager.getLogger(getClass);

  //BUGBUG:: Not Checking whether inputConfig is really QueueAdapterConfiguration or not.
  private[this] var qc: com.ligadata.AdaptersConfiguration.KafkaQueueAdapterConfiguration = null
  if(!inputConfig.isInstanceOf[com.ligadata.AdaptersConfiguration.KafkaQueueAdapterConfiguration])
    qc = KafkaQueueAdapterConfiguration.GetAdapterConfig(inputConfig)
  else
    qc = inputConfig.asInstanceOf[com.ligadata.AdaptersConfiguration.KafkaQueueAdapterConfiguration]

  val default_compression_type = "none" // Valida values at this moment are none, gzip, or snappy.
  val default_value_serializer = "org.apache.kafka.common.serialization.ByteArraySerializer"
  val default_key_serializer = "org.apache.kafka.common.serialization.ByteArraySerializer"
  val default_batch_size = "1024"
  val default_linger_ms = "50" // 50ms
  // val default_retries = "0"
  val default_block_on_buffer_full = "true" // true or false
  val default_buffer_memory = "16777216" // 16MB
  val default_client_id = qc.Name + "_" + hashCode.toString
  val default_request_timeout_ms = "10000"
  val default_timeout_ms = "10000"
  val default_metadata_fetch_timeout_ms = "10000"
  val defrault_metadata_max_age_ms = "20000"
  val default_max_block_ms = "20000"
  val default_max_buffer_full_block_ms = "100"
  val default_network_request_timeout_ms = "20000"
  val default_outstanding_messages = "2048"

  val linger_ms = qc.otherconfigs.getOrElse("linger.ms", default_linger_ms).toString.trim()
  val timeout_ms = qc.otherconfigs.getOrElse("timeout.ms", default_timeout_ms).toString.trim()
  val metadata_fetch_timeout_ms = qc.otherconfigs.getOrElse("metadata.fetch.timeout.ms", default_metadata_fetch_timeout_ms).toString.trim()
  private var msgCount = 0
  val counterLock = new Object

  private var metrics: collection.mutable.Map[String,Any] = collection.mutable.Map[String,Any]()
  private var startTime: String = "n/a"
  private var lastSeen: String = "n/a"

  // Set up some properties for the Kafka Producer
  val props = new Properties()
  props.put("bootstrap.servers", qc.hosts.mkString(",")); // ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
  props.put("compression.type", qc.otherconfigs.getOrElse("compression.type", default_compression_type).toString.trim()); // ProducerConfig.COMPRESSION_TYPE_CONFIG
  props.put("value.serializer", qc.otherconfigs.getOrElse("value.serializer", default_value_serializer).toString.trim()); // ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
  props.put("key.serializer", qc.otherconfigs.getOrElse("key.serializer", default_key_serializer).toString.trim()); // ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
  props.put("batch.size", qc.otherconfigs.getOrElse("batch.size", default_batch_size).toString.trim()); // ProducerConfig.BATCH_SIZE_CONFIG
  props.put("linger.ms", linger_ms) // ProducerConfig.LINGER_MS_CONFIG
  // props.put("retries", qc.otherconfigs.getOrElse("retries", default_retries).toString.trim()) // ProducerConfig.RETRIES_CONFIG
  props.put("block.on.buffer.full", qc.otherconfigs.getOrElse("block.on.buffer.full", default_block_on_buffer_full).toString.trim()) // ProducerConfig.BLOCK_ON_BUFFER_FULL_CONFIG
  props.put("buffer.memory", qc.otherconfigs.getOrElse("buffer.memory", default_buffer_memory).toString.trim()) // ProducerConfig.BUFFER_MEMORY_CONFIG
  props.put("client.id", qc.otherconfigs.getOrElse("client.id", default_client_id).toString.trim()) // ProducerConfig.CLIENT_ID_CONFIG
  props.put("request.timeout.ms", qc.otherconfigs.getOrElse("request.timeout.ms", default_request_timeout_ms).toString.trim())
  props.put("timeout.ms", timeout_ms)
  props.put("metadata.fetch.timeout.ms", metadata_fetch_timeout_ms)
  props.put("metadata.max.age.ms", qc.otherconfigs.getOrElse("metadata.max.age.ms", defrault_metadata_max_age_ms).toString.trim())
  props.put("max.block.ms", qc.otherconfigs.getOrElse("max.block.ms", default_max_block_ms).toString.trim())
  props.put("max.buffer.full.block.ms", qc.otherconfigs.getOrElse("max.buffer.full.block.ms", default_max_buffer_full_block_ms).toString.trim())
  props.put("network.request.timeout.ms", qc.otherconfigs.getOrElse("network.request.timeout.ms", default_network_request_timeout_ms).toString.trim())

  if (qc.group_id != null)
    props.put("group.id", qc.group_id)
  else
    props.put("group.id", "kamanja-kafka-group")

  // Verify the Secuirty Paramters...
  if (qc.security_protocol != null && (qc.security_protocol.trim.equalsIgnoreCase("sasl_plaintext") || qc.security_protocol.trim.equalsIgnoreCase("sasl_ssl") || qc.security_protocol.trim.equalsIgnoreCase("ssl"))) {
    if (qc.security_protocol.trim.equalsIgnoreCase("sasl_plaintext")) {

      // Add all the required SASL parameters.
      props.put("security.protocol", qc.security_protocol)
      if (qc.sasl_mechanism != null) props.put("sasl.mechanism", qc.sasl_mechanism)

      // THROW A WARNING, if PLAIN is chosen with unencrypted communication.
      if (qc.sasl_mechanism != null && qc.sasl_mechanism.equalsIgnoreCase("plaintext")) {
        LOG.warn("\n\nKafkaProducer is instantiated with security protocol of SASL_PLAINTEXT and security mechanism of PLAINTEXT. This Will result in unecrypted passwords to be sent across the wire\n")
      }

      if (qc.sasl_kerberos_service_name != null)
        props.put("sasl.kerberos.service.name", qc.sasl_kerberos_service_name)
      else
        throw new KamanjaException("KamanjaKafkaCosnumer properties must specify SASL.KERBEROS.SERVICE.NAME if SASL is specified as Security Protocol", null)
      if (qc.sasl_kerberos_kinit_cmd != null) props.put("sasl.kerberos.kinit.cmd", qc.sasl_kerberos_kinit_cmd)
      if (qc.sasl_kerberos_min_time_before_relogic != null) props.put("sasl.kerberos.min.time.before.relogin", qc.sasl_kerberos_min_time_before_relogic)
      if (qc.sasl_kerberos_ticket_renew_jiter != null) props.put("sasl.kerberos.ticket.renew.jitter", qc.sasl_kerberos_ticket_renew_jiter)
      if (qc.sasl_kerberos_ticket_renew_window_factor != null) props.put("sasl.kerberos.ticket.renew.window.factor", qc.sasl_kerberos_ticket_renew_window_factor)
    }

    if (qc.security_protocol.trim.equalsIgnoreCase("sasl_ssl")) {
      // Add all the required SASL parameters.
      props.put("security.protocol", qc.security_protocol)
      if (qc.sasl_mechanism != null) props.put("sasl.mechanism", qc.sasl_mechanism)
      if (qc.sasl_kerberos_service_name != null)
        props.put("sasl.kerberos.service.name", qc.sasl_kerberos_service_name)
      else
        throw new KamanjaException("KamanjaKafkaCosnumer properties must specify SASL.KERBEROS.SERVICE.NAME if SASL is specified as Security Protocol", null)
      if (qc.sasl_kerberos_kinit_cmd != null) props.put("sasl.kerberos.kinit.cmd", qc.sasl_kerberos_kinit_cmd)
      if (qc.sasl_kerberos_min_time_before_relogic != null) props.put("sasl.kerberos.min.time.before.relogin", qc.sasl_kerberos_min_time_before_relogic)
      if (qc.sasl_kerberos_ticket_renew_jiter != null) props.put("sasl.kerberos.ticket.renew.jitter", qc.sasl_kerberos_ticket_renew_jiter)
      if (qc.sasl_kerberos_ticket_renew_window_factor != null) props.put("sasl.kerberos.ticket.renew.window.factor", qc.sasl_kerberos_ticket_renew_window_factor)

      // Add all the SSL stuff now
      if (qc.ssl_key_password != null) props.put("ssl.key.password", qc.ssl_key_password)
      if (qc.ssl_keystore_location != null) props.put("ssl.keystore.location", qc.ssl_keystore_location)
      if (qc.ssl_keystore_password != null) props.put("ssl.keystore.password", qc.ssl_keystore_password)
      if (qc.ssl_truststore_location != null) props.put("ssl.truststore.location",qc.ssl_truststore_location)
      if (qc.ssl_truststore_password != null) props.put("ssl.truststore.password", qc.ssl_truststore_password)
      if (qc.ssl_enabled_protocols != null) props.put("ssl.enabled.protocols", qc.ssl_enabled_protocols)
      if (qc.ssl_keystore_type != null) props.put("ssl.keystore.type", qc.ssl_keystore_type)
      if (qc.ssl_protocol != null) props.put("ssl.protocol", qc.ssl_protocol)
      if (qc.ssl_provider != null) props.put("ssl.provider", qc.ssl_provider)
      if (qc.ssl_truststore_type != null) props.put("ssl.truststore.type", qc.ssl_truststore_type)
      if (qc.ssl_cipher_suites != null) props.put("ssl.cipher.suites", qc.ssl_cipher_suites)
      if (qc.ssl_endpoint_identification_algorithm != null) props.put("ssl.endpoint.identification.algorithm", qc.ssl_endpoint_identification_algorithm)
      if (qc.ssl_keymanager_algorithm != null) props.put("ssl.keymanager.algorithm", qc.ssl_keymanager_algorithm)
      if (qc.ssl_trust_manager_algorithm != null) props.put("ssl.trustmanager.algorithm", qc.ssl_trust_manager_algorithm)
    }

    if (qc.security_protocol.trim.equalsIgnoreCase("ssl")) {
      //All SSL parameters
      props.put("security.protocol", qc.security_protocol)
      if (qc.ssl_key_password != null) props.put("ssl.key.password", qc.ssl_key_password)
      if (qc.ssl_keystore_location != null) props.put("ssl.keystore.location", qc.ssl_keystore_location)
      if (qc.ssl_keystore_password != null) props.put("ssl.keystore.password", qc.ssl_keystore_password)
      if (qc.ssl_truststore_location != null) props.put("ssl.truststore.location",qc.ssl_truststore_location)
      if (qc.ssl_truststore_password != null) props.put("ssl.truststore.password", qc.ssl_truststore_password)
      if (qc.ssl_enabled_protocols != null) props.put("ssl.enabled.protocols", qc.ssl_enabled_protocols)
      if (qc.ssl_keystore_type != null) props.put("ssl.keystore.type", qc.ssl_keystore_type)
      if (qc.ssl_protocol != null) props.put("ssl.protocol", qc.ssl_protocol)
      if (qc.ssl_provider != null) props.put("ssl.provider", qc.ssl_provider)
      if (qc.ssl_truststore_type != null) props.put("ssl.truststore.type", qc.ssl_truststore_type)
      if (qc.ssl_cipher_suites != null) props.put("ssl.cipher.suites", qc.ssl_cipher_suites)
      if (qc.ssl_endpoint_identification_algorithm != null) props.put("ssl.endpoint.identification.algorithm", qc.ssl_endpoint_identification_algorithm)
      if (qc.ssl_keymanager_algorithm != null) props.put("ssl.keymanager.algorithm", qc.ssl_keymanager_algorithm)
      if (qc.ssl_trust_manager_algorithm != null) props.put("ssl.trustmanager.algorithm", qc.ssl_trust_manager_algorithm)
    }
  }
  // Add the rest of the properties.  max.outstanding.messages is a special thing ????
  qc.otherconfigs.foreach(p => {
    if (!props.containsKey(p._1) && !"max.outstanding.messages".equalsIgnoreCase(p._1)) {
      props.put(p._1, p._2)
    }
  })

  val max_outstanding_messages = qc.otherconfigs.getOrElse("max.outstanding.messages", default_outstanding_messages).toString.trim().toInt

  case class MsgDataRecievedCnt(cntrToOrder: Long, msg: ProducerRecord[Array[Byte], Array[Byte]])

  val partitionsMap = new ConcurrentHashMap[Int, ConcurrentHashMap[Long, MsgDataRecievedCnt]](128);
  val failedMsgsMap = new ConcurrentHashMap[Int, ConcurrentHashMap[Long, MsgDataRecievedCnt]](128); // We just need Array Buffer as Innser value. But the only issue is we need to make sure we handle it for multiple threads.

  var reqCntr: Int = 0
  var msgInOrder = new AtomicLong


  // Create the producer object...
  LOG.info("Staring Kafka Producer with the following paramters: \n" + qc.toString)
  //workaround for Kafka bug with class loader
  var tempContext = Thread.currentThread().getContextClassLoader
  Thread.currentThread().setContextClassLoader(null);
  val producer = new org.apache.kafka.clients.producer.KafkaProducer[Array[Byte], Array[Byte]](props)
  Thread.currentThread().setContextClassLoader(tempContext);

  var topicPartitionsCount = producer.partitionsFor(qc.topic).size()

  var partitionsGetTm = System.currentTimeMillis
  val refreshPartitionTime = 60 * 1000 // 60 secs

  val randomPartitionCntr = new java.util.Random

  val key = Category + "/" + qc.Name + "/evtCnt"

  private var isShutdown = false
  private var isHeartBeating = false
  private var isInError = false

  private var retryExecutor: ExecutorService = Executors.newFixedThreadPool(1)
  private var heartBeatThread: ExecutorService = Executors.newFixedThreadPool(1)

  LOG.info(qc.Name + " Initializing Statistics")
  startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  metrics (KafkaProducer.SEND_CALL_COUNT_KEY) = 0
  metrics (KafkaProducer.SEND_MESSAGE_COUNT_KEY) = 0
  metrics (KafkaProducer.LAST_FAILURE_TIME) = "n/a"
  metrics (KafkaProducer.LAST_RECOVERY_TIME) = "n/a"

  retryExecutor.execute(new RetryFailedMessages())

  class RetryFailedMessages extends Runnable {
    def run() {
      val statusPrintTm = 60000 // for every 1 min
      var nextPrintTimeCheck = System.currentTimeMillis + statusPrintTm
      while (isShutdown == false) {
        try {
          Thread.sleep(5000) // Sleeping for 5Sec
        } catch {
          case e: Exception => {
            externalizeExceptionEvent(e)
            if (! isShutdown) LOG.warn("", e)
          }
          case e: Throwable => {
            externalizeExceptionEvent(e)
            if (! isShutdown) LOG.warn("", e)
          }
        }
        if (isShutdown == false) {
          var outstandingMsgs = outstandingMsgCount
          var allFailedMsgs = failedMsgCount
          if (outstandingMsgs > 0 || allFailedMsgs > 0 || nextPrintTimeCheck < System.currentTimeMillis) {
            LOG.warn("KAFKA PRODUCER: Topic: %s - current outstanding messages:%d & failed messages:%d".format(qc.topic, outstandingMsgs, allFailedMsgs))
            nextPrintTimeCheck = System.currentTimeMillis + statusPrintTm
          }
          // Get all failed records and resend for each partitions
          val keysIt = failedMsgsMap.keySet().iterator()

          while (keysIt.hasNext() && isShutdown == false) {
            val partId = keysIt.next();

            val failedMsgs = failedMsgsMap.get(partId)
            val sz = failedMsgs.size()
            if (sz > 0) {
              val keyMessages = new ArrayBuffer[MsgDataRecievedCnt](sz)

              val allmsgsit = failedMsgs.entrySet().iterator()
              while (allmsgsit.hasNext() && isShutdown == false) {
                val ent = allmsgsit.next();
                keyMessages += ent.getValue
              }
              if (isShutdown == false) {
                val km = keyMessages.sortWith(_.cntrToOrder < _.cntrToOrder) // Sending in the same order as inserted before.
                sendInfinitely(km, true)
              }
            }
          }
        }
      }
    }
  }

  private def failedMsgCount: Int = {
    var failedMsgs = 0

    val allFailedPartitions = failedMsgsMap.elements()
    while (allFailedPartitions.hasMoreElements()) {
      val nxt = allFailedPartitions.nextElement();
      failedMsgs += nxt.size()
    }
    failedMsgs
  }

  private def outstandingMsgCount: Int = {
    var outstandingMsgs = 0
    val allPartitions = partitionsMap.elements()
    while (allPartitions.hasMoreElements()) {
      val nxt = allPartitions.nextElement();
      outstandingMsgs += nxt.size()
    }
    outstandingMsgs
  }

  /*
  private def addMsgToMap(partId: Int, msgAndCntr: MsgDataRecievedCnt): Unit = {
    var msgMap = partitionsMap.get(partId)
    if (msgMap == null) {
      partitionsMap.synchronized {
        msgMap = partitionsMap.get(partId)
        if (msgMap == null) {
          val tmpMsgMap = new ConcurrentHashMap[Long, MsgDataRecievedCnt](1024);
          partitionsMap.put(partId, tmpMsgMap)
          msgMap = tmpMsgMap
        }
      }
    }

    if (msgMap != null) {
      try {
        msgMap.put(msgAndCntr.cntrToOrder, msgAndCntr)
      } catch {
        case e: Exception => {
          // Failed to insert into Map
          throw e
        }
      }
    }
  }
*/

  private def addMsgsToMap(partId: Int, keyMessages: ArrayBuffer[MsgDataRecievedCnt]): Unit = {
    var msgMap = partitionsMap.get(partId)
    if (msgMap == null) {
      partitionsMap.synchronized {
        msgMap = partitionsMap.get(partId)
        if (msgMap == null) {
          val tmpMsgMap = new ConcurrentHashMap[Long, MsgDataRecievedCnt](1024);
          partitionsMap.put(partId, tmpMsgMap)
          msgMap = tmpMsgMap
        }
      }
    }

    if (msgMap != null) {
      try {
        val allKeys = new java.util.HashMap[Long, MsgDataRecievedCnt]()
        keyMessages.foreach(m => {
          allKeys.put(m.cntrToOrder, m)
        })
        msgMap.putAll(allKeys)
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          // Failed to insert into Map
          throw e
        }
      }
    }
  }

  private def removeMsgFromMap(msgAndCntr: MsgDataRecievedCnt): Unit = {
    if (msgAndCntr == null) return
    val partId = msgAndCntr.msg.partition()
    val msgMap = partitionsMap.get(partId)
    if (msgMap != null) {
      try {
        msgMap.remove(msgAndCntr.cntrToOrder) // This must present. Because we are adding the records into partitionsMap before we send messages. If it does not present we simply ignore it.
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          LOG.warn("", e)
        }
        case e: Throwable => {
          externalizeExceptionEvent(e)
          LOG.warn("", e)
        }
      }
    }
  }

  private def addToFailedMap(msgAndCntr: MsgDataRecievedCnt): Unit = {
    if (msgAndCntr == null) return
    val partId = msgAndCntr.msg.partition()
    var msgMap = failedMsgsMap.get(partId)
    if (msgMap == null) {
      failedMsgsMap.synchronized {
        msgMap = failedMsgsMap.get(partId)
        if (msgMap == null) {
          val tmpMsgMap = new ConcurrentHashMap[Long, MsgDataRecievedCnt](1024);
          failedMsgsMap.put(partId, tmpMsgMap)
          msgMap = tmpMsgMap
        }
      }
    }

    if (msgMap != null) {
      try {
        msgMap.put(msgAndCntr.cntrToOrder, msgAndCntr)
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          // Failed to insert into Map
          throw e
        }
      }
    }
  }

  private def removeMsgFromFailedMap(msgAndCntr: MsgDataRecievedCnt): Unit = {
    if (msgAndCntr == null) return
    val partId = msgAndCntr.msg.partition()
    val msgMap = failedMsgsMap.get(partId)
    if (msgMap != null) {
      try {
        msgMap.remove(msgAndCntr.cntrToOrder)
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          LOG.warn("", e)
        }
        case e: Throwable => {
          externalizeExceptionEvent(e)
          LOG.warn("", e)
        }
      }
    }
  }

  private def getPartition(key: Array[Byte], numPartitions: Int): Int = {
    if (numPartitions == 0) return 0
    if (key != null) {
      try {
        return (scala.math.abs(Arrays.hashCode(key)) % numPartitions)
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          throw e
        }
        case e: Throwable => {
          externalizeExceptionEvent(e)
          throw e
        }
      }
    }
    return randomPartitionCntr.nextInt(numPartitions)
  }


  /**
    *
    *
    * @return
    */
  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    implicit val formats = org.json4s.DefaultFormats
    return new MonitorComponentInfo( AdapterConfiguration.TYPE_OUTPUT, qc.Name, KafkaProducer.ADAPTER_DESCRIPTION, startTime, lastSeen,  Serialization.write(metrics).toString)
  }

  override def getComponentSimpleStats: String = {
    return key + "->" + msgCount
  }


  /**
    *
    * @param tnxCtxt
    * @param outputContainers
    */
  override def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): Unit = {
    if (outputContainers.size == 0) return

    // Sanity checks
    if (isShutdown) {
      val szMsg = qc.Name + " KAFKA PRODUCER: Producer is not available for processing"
      LOG.error(szMsg)
      throw new Exception(szMsg)
    }

    val (outContainers, serializedContainerData, serializerNames) = serialize(tnxCtxt, outputContainers)

    if (outContainers.size != serializedContainerData.size || outContainers.size != serializerNames.size) {
      val szMsg = qc.Name + " KAFKA PRODUCER: Messages, messages serialized data & serializer names should has same number of elements. Messages:%d, Messages Serialized data:%d, serializerNames:%d".format(outContainers.size, serializedContainerData.size, serializerNames.size)
      LOG.error(szMsg)
      throw new Exception(szMsg)
    }

    if (serializedContainerData.size == 0) return

    val partitionKeys = ArrayBuffer[Array[Byte]]()

    for (i <- 0 until serializedContainerData.size) {
      partitionKeys += outContainers(i).getPartitionKey.mkString(",").getBytes()
    }

    send(serializedContainerData, partitionKeys.toArray)
  }


  override def send(messages: Array[Array[Byte]], partitionKeys: Array[Array[Byte]]): Unit = {
    if (!isHeartBeating) runHeartBeat

    // Refreshing Partitions for every refreshPartitionTime.
    // BUGBUG:: This may execute multiple times from multiple threads. For now it does not hard too much.
    if ((System.currentTimeMillis - partitionsGetTm) > refreshPartitionTime) {
      topicPartitionsCount = producer.partitionsFor(qc.topic).size()
      partitionsGetTm = System.currentTimeMillis
    }

    try {
      var partitionsMsgMap = scala.collection.mutable.Map[Int, ArrayBuffer[MsgDataRecievedCnt]]();

      for (i <- 0 until messages.size) {

        val partId = getPartition(partitionKeys(i), topicPartitionsCount)
        var ab = partitionsMsgMap.getOrElse(partId, null)
        if (ab == null) {
          ab = new ArrayBuffer[MsgDataRecievedCnt](256)
          partitionsMsgMap(partId) = ab
        }
        val pr = new ProducerRecord(qc.topic, partId, partitionKeys(i), messages(i))
        ab += MsgDataRecievedCnt(msgInOrder.getAndIncrement, pr)
      }

      var outstandingMsgs = outstandingMsgCount
      // LOG.debug("KAFKA PRODUCER: current outstanding messages for topic %s are %d".format(qc.topic, outstandingMsgs))

      var osRetryCount = 0
      var osWaitTm = 5000
      while (outstandingMsgs > max_outstanding_messages) {
        LOG.warn(qc.Name + " KAFKA PRODUCER: %d outstanding messages in queue to write. Waiting for them to flush before we write new messages. Retrying after %dms. Retry count:%d".format(outstandingMsgs, osWaitTm, osRetryCount))
        try {
          Thread.sleep(osWaitTm)
        } catch {
          case e: Exception => {
            externalizeExceptionEvent(e)
            throw e
          }
          case e: Throwable => {
            externalizeExceptionEvent(e)
            throw e
          }
        }
        outstandingMsgs = outstandingMsgCount
      }

      partitionsMsgMap.foreach(partIdAndRecs => {
        val partId = partIdAndRecs._1
        val keyMessages = partIdAndRecs._2

        // first push all messages to partitionsMap before we really send. So that callback is guaranteed to find the message in partitionsMap
        addMsgsToMap(partId, keyMessages)
        sendInfinitely(keyMessages, false)
      })

    } catch {
      case e: java.lang.InterruptedException => {
        // Not doing anythign for now
        LOG.warn(qc.Name + " KAFKA PRODUCER: Got java.lang.InterruptedException. isShutdown:" + isShutdown)
      }
      case fae: FatalAdapterException => {
        externalizeExceptionEvent(fae)
        throw fae
      }
      case e: Exception               => {
        externalizeExceptionEvent(e)
        throw FatalAdapterException("Unknown exception", e)
      }
      case e: Throwable               => {
        externalizeExceptionEvent(e)
        throw FatalAdapterException("Unknown exception", e)
      }
    }
  }

  private def sendInfinitely(keyMessages: ArrayBuffer[MsgDataRecievedCnt], removeFromFailedMap: Boolean): Unit = {
    var sendStatus = KafkaConstants.KAFKA_NOT_SEND
    var retryCount = 0
    var waitTm = 15000

    // We keep on retry until we succeed on this thread
    while (sendStatus != KafkaConstants.KAFKA_SEND_SUCCESS && isShutdown == false) {
      try {
        sendStatus = doSend(keyMessages, removeFromFailedMap)
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          LOG.error(qc.Name + " KAFKA PRODUCER: Error sending to kafka, Retrying after %dms. Retry count:%d".format(waitTm, retryCount), e)
          try {
            Thread.sleep(waitTm)
          } catch {
            case e: Exception =>  {
              externalizeExceptionEvent(e)
              throw e
            }
            case e: Throwable => {
              externalizeExceptionEvent(e)
              throw e
            }
          }
          if (waitTm < 60000) {
            waitTm = waitTm * 2
            if (waitTm > 60000)
              waitTm = 60000
          }
        }
      }
    }
  }

  private def addBackFailedToSendRec(lastAccessRec: MsgDataRecievedCnt): Unit = {
    if (lastAccessRec != null)
      addToFailedMap(lastAccessRec)
  }

  private def doSend(keyMessages: ArrayBuffer[MsgDataRecievedCnt], removeFromFailedMap: Boolean): Int = {
    var sentMsgsCntr = 0
    var lastAccessRec: MsgDataRecievedCnt = null
    try {
      updateMetricValue(KafkaProducer.SEND_MESSAGE_COUNT_KEY,keyMessages.size)
      updateMetricValue(KafkaProducer.SEND_CALL_COUNT_KEY,1)

      // We already populated partitionsMap before we really send. So that callback is guaranteed to find the message in partitionsMap
      keyMessages.map(msgAndCntr => {
        if (isShutdown)
          throw new Exception(qc.Name + " is shutting down")
        lastAccessRec = msgAndCntr
        if (removeFromFailedMap)
          removeMsgFromFailedMap(lastAccessRec)
        // Send the request to Kafka
        producer.send(msgAndCntr.msg, new Callback {
          override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
            val localMsgAndCntr = msgAndCntr
            msgCount += 1
            if (exception != null) {
              LOG.warn(qc.Name + " Failed to send message into " + localMsgAndCntr.msg.topic, exception)
              addToFailedMap(localMsgAndCntr)
              if (!isInError) updateMetricValue(KafkaProducer.LAST_FAILURE_TIME, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis)))
              isInError = true
            } else {
              // Succeed - also click the heartbeat here... just to be more accurate.
              if (isInError) updateMetricValue(KafkaProducer.LAST_RECOVERY_TIME, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis)))
              lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
              removeMsgFromMap(localMsgAndCntr)
              isInError = false

            }
          }
        })
        lastAccessRec = null
        sentMsgsCntr += 1
        // cntrAdapter.addCntr(key, 1)
      })

      keyMessages.clear()
    } catch {
      //  case ftsme: FailedToSendMessageException => {
      //    externalizeExceptionEvent(ftsme)
      //    if (sentMsgsCntr > 0) keyMessages.remove(0, sentMsgsCntr); addBackFailedToSendRec(lastAccessRec); throw new FatalAdapterException("Kafka sending to Dead producer", ftsme)
      //  }
      //  case qfe: QueueFullException => {
      //    externalizeExceptionEvent(qfe)
      //    if (sentMsgsCntr > 0) keyMessages.remove(0, sentMsgsCntr); addBackFailedToSendRec(lastAccessRec); throw new FatalAdapterException("Kafka queue full", qfe)
      //  }
      case e: Exception  => {
        externalizeExceptionEvent(e)
        if (sentMsgsCntr > 0) keyMessages.remove(0, sentMsgsCntr)
        addBackFailedToSendRec(lastAccessRec)
        LOG.warn(qc.Name + " unknown exception encountered ", e)
        throw new FatalAdapterException("Unknown exception", e)
      }
      case e: Throwable => {
        externalizeExceptionEvent(e)
        if (sentMsgsCntr > 0) keyMessages.remove(0, sentMsgsCntr)
        addBackFailedToSendRec(lastAccessRec)
        LOG.warn(qc.Name + " unknown exception encountered ", e)
        throw new FatalAdapterException("Unknown exception", e) }
    }
    return KafkaConstants.KAFKA_SEND_SUCCESS
  }

  override def Shutdown(): Unit = {

    LOG.info(qc.Name + " Shutdown detected")

    // Shutdown HB
    isShutdown = true

    heartBeatThread.shutdownNow
    while (!heartBeatThread.isTerminated) {
      try {
        Thread.sleep(100)
      } catch {
        case e: Exception => {
          // Don't do anything, because it is shutting down
        }
        case e: Throwable => {
          // Don't do anything, because it is shutting down
        }
      }
    }

    // First shutdown retry executor
    if (retryExecutor != null) {
      retryExecutor.shutdownNow
      while (!retryExecutor.isTerminated) {
        try {
          Thread.sleep(100)
        } catch {
          case e: Exception => {
            // Don't do anything, because it is shutting down
          }
          case e: Throwable => {
            // Don't do anything, because it is shutting down
          }
        }
      }
    }

    if (producer != null)
      producer.close
  }


  // Accumulate the metrics.. simple for now
  private def updateMetricValue(key: String, value: Any): Unit = {
    counterLock.synchronized {
      if (key.equalsIgnoreCase(KafkaProducer.LAST_FAILURE_TIME) ||
        key.equalsIgnoreCase(KafkaProducer.LAST_RECOVERY_TIME)) {
        metrics(key) = value.toString
      } else {
        // This is an aggregated Long value
        val cur = metrics.getOrElse(key,"0").toString
        val longCur = cur.toLong
        metrics(key) = longCur + value.toString.toLong
      }
    }
  }

  private def runHeartBeat: Unit = {
    heartBeatThread.execute(new Runnable() {
      override def run(): Unit = {
        try {
          isHeartBeating = true
          while (!isShutdown) {
            if (!isInError) {
              lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
            }
            Thread.sleep(KafkaProducer.HB_PERIOD)
          }
          isHeartBeating = false
        } catch {
          case e: Exception => {
            externalizeExceptionEvent(e)
            isHeartBeating = false
            if (isShutdown == false)
              LOG.warn(qc.Name + " Heartbeat Interrupt detected", e)
          }
        }
        LOG.info(qc.Name + " Heartbeat is shutting down")
      }
    })
  }
}
