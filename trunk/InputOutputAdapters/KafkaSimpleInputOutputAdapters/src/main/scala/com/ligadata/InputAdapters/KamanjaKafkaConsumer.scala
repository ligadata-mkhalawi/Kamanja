/*package com.ligadata.InputAdapters

import java.util
import java.util.Properties

import com.ligadata.AdaptersConfiguration.{KafkaQueueAdapterConfiguration, KafkaPartitionUniqueRecordValue, KafkaPartitionUniqueRecordKey}
import com.ligadata.Exceptions.KamanjaException
import com.ligadata.HeartBeat.MonitorComponentInfo
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.KamanjaBase.{NodeContext, DataDelimiters}
//import kafka.api.{FetchResponse, FetchRequestBuilder}
//import kafka.consumer.SimpleConsumer
import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.{TopicPartition, PartitionInfo}
import org.apache.logging.log4j.LogManager
import org.json4s.jackson.Serialization
import com.ligadata.Utils.Utils

import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._

/**
  *
  */
object KamanjaKafkaConsumer extends InputAdapterFactory {

  val INITIAL_SLEEP = 500
  val MAX_SLEEP = 30000
  val POLLING_INTERVAL = 100

  // Statistics Keys
  val ADAPTER_DESCRIPTION = "Kafka 0.10.0.0 Client"
  val PARTITION_COUNT_KEYS = "Partition Counts"
  val PARTITION_DEPTH_KEYS = "Partition Depths"
  val EXCEPTION_SUMMARY = "Exception Summary"

  def CreateInputAdapter(inputConfig: AdapterConfiguration, execCtxtObj: ExecContextFactory, nodeContext: NodeContext): InputAdapter = new KamanjaKafkaConsumer(inputConfig, execCtxtObj, nodeContext)
}


/**
  * Class that handles reading from Kafka Topics
  *
  * @param inputConfig
  * @param execCtxtObj
  * @param nodeContext
  */
class KamanjaKafkaConsumer(val inputConfig: AdapterConfiguration, val execCtxtObj: ExecContextFactory, val nodeContext: NodeContext) extends InputAdapter {
  val input = this
  private val qc = KafkaQueueAdapterConfiguration.GetAdapterConfig(inputConfig)
  private val LOG = LogManager.getLogger(getClass)
  LOG.info("Staring Kafka Consumer (0.9+) with the following paramters:\n" + qc.toString)
  private var isShutdown = false
  private val lock = new Object()
  private var msgCount: Long = 0
  private var sleepDuration = 500
  private var isQuiese = false
  private val kvs = scala.collection.mutable.Map[Int, (KafkaPartitionUniqueRecordKey, KafkaPartitionUniqueRecordValue, KafkaPartitionUniqueRecordValue)]()

  private var readExecutor: ExecutorService = _

  // Gotta have metric collection
  private var metrics: collection.mutable.Map[String,Any] = collection.mutable.Map[String,Any]()
  private var partitonCounts: collection.mutable.Map[String,Long] = collection.mutable.Map[String,Long]()
  private var partitonDepths: collection.mutable.Map[String,Long] = collection.mutable.Map[String,Long]()
  private var partitionExceptions: collection.mutable.Map[String,ExceptionInfo] = collection.mutable.Map[String,ExceptionInfo]()
  private var startHeartBeat: String = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var lastSeen: String = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  metrics(KafkaSimpleConsumer.PARTITION_COUNT_KEYS) = partitonCounts
  metrics(KafkaSimpleConsumer.EXCEPTION_SUMMARY) = partitionExceptions
  metrics(KafkaSimpleConsumer.PARTITION_DEPTH_KEYS) = partitonDepths
  var localReadOffsets: collection.mutable.Map[Int,Long] = collection.mutable.Map[Int,Long]()

  var props = new Properties()
  props.put("bootstrap.servers", qc.hosts.mkString(","))

  // We handle offsets ourselves...
  props.put("enable.auto.commit", "false")

  //auto.offset.reset is always Latest for Kamanja
  props.put("auto.offset.reset", "latest")

  // Default to somve values..
  props.put("session.timeout.ms", "30000")
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

  if (qc.group_id != null)
    props.put("group.id", qc.group_id)
  else
    props.put("group.id", "kamanja-kafka-group")

  // Verify the Secuirty Paramters...
  if (qc.security_protocol != null && (qc.security_protocol.trim.equalsIgnoreCase("sasl_plaintext") || qc.security_protocol.trim.equalsIgnoreCase("sasl_ssl") || qc.security_protocol.trim.equalsIgnoreCase("ssl"))) {
    if (qc.security_protocol.trim.equalsIgnoreCase("sasl_plaintext")) {
      // Add all the required SASL parameters.
      props.put("security.protocol", qc.security_protocol.trim)
      if (qc.sasl_mechanism != null) props.put("sasl.mechanism", qc.sasl_mechanism)

      // THROW A WARNING, if PLAIN is chosen with unencrypted communication.
      if (qc.sasl_mechanism != null && qc.sasl_mechanism.equalsIgnoreCase("plain")) {
        LOG.warn("\n\nKamanjaKafkaConsumer is instantiated with security protocol of SASL_PLAIN and security mechanism of PLAIN. This Will result in unecrypted passwords to be sent across the wire\n")
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
      props.put("security.protocol", qc.security_protocol.trim)
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
      props.put("security.protocol", qc.security_protocol.trim)
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


  var isConsumerInitialized: Boolean = false

  // factory for KafkaConsumers.. for now it doesn't do anything, but it may have to in the future.
  private def createConsumerWithInputProperties(): org.apache.kafka.clients.consumer.KafkaConsumer[String,String] = {
    // Create the new consumer Objects
    new org.apache.kafka.clients.consumer.KafkaConsumer[String,String] (props)
  }

  /**
    * Start processing - will start a number of threads to read the Kafka queues for a topic.  The list of Hosts servicing a
    * given topic, and the topic have been set when this KafkaConsumer_V2 Adapter was instantiated.  The partitionIds should be
    * obtained via a prior call to the adapter.  One of the hosts will be a chosen as a leader to service the requests by the
    * spawned threads.
    *
    * @param ignoreFirstMsg Boolean - if true, ignore the first message sending to engine
    * @param partitionIds Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue, Long, PartitionUniqueRecordValue)] - an Array of partition ids
    */
  override def StartProcessing(partitionIds: Array[StartProcPartInfo], ignoreFirstMsg: Boolean): Unit = lock.synchronized {

    LOG.info("Start processing called on KamanjaKafkaAdapter for topic " + qc.topic)
    // This is the number of executors we will run - Heuristic, but will go with it
   // var numberOfThreads = availableThreads
    var maxPartNumber = -1
    //TODO: The engine should tell us how many thread to use.. for now default to the old behaiviour... 1 Threads per partition
    var numberOfThreads = partitionIds.size
    readExecutor = Executors.newFixedThreadPool(numberOfThreads, Utils.GetScalaThreadFactory("Adapter:" + inputConfig.Name + "-readExecutor-%d"))

    // Get the data about the request and set the instancePartition list.
    val partitionInfo = partitionIds.map(quad => {
      (quad._key.asInstanceOf[KafkaPartitionUniqueRecordKey],
        quad._val.asInstanceOf[KafkaPartitionUniqueRecordValue],
        quad._validateInfoVal.asInstanceOf[KafkaPartitionUniqueRecordValue])
    })

    qc.instancePartitions = partitionInfo.map(partQuad => { partQuad._1.PartitionId }).toSet

    qc.instancePartitions.foreach(partitionId => {
      maxPartNumber = scala.math.max(partitionId, maxPartNumber)
    })

    val partitionGroups: scala.collection.mutable.Map[Int, scala.collection.mutable.Set[(KafkaPartitionUniqueRecordKey, KafkaPartitionUniqueRecordValue, KafkaPartitionUniqueRecordValue )]]
                         = scala.collection.mutable.Map[Int, scala.collection.mutable.Set[(KafkaPartitionUniqueRecordKey, KafkaPartitionUniqueRecordValue, KafkaPartitionUniqueRecordValue )]]()

    // Create a Map of all the partiotion Ids.  We use a MOD for all the partition numbers to allocate each partition into
    // a bucket.
    var bCount = 0
    partitionInfo.foreach(quad => {
      if (partitionGroups.contains(bCount)) {
        partitionGroups(bCount) = partitionGroups(bCount) + quad
      } else {
        partitionGroups(bCount) =  scala.collection.mutable.Set[(KafkaPartitionUniqueRecordKey, KafkaPartitionUniqueRecordValue, KafkaPartitionUniqueRecordValue )](quad)
      }
      bCount = (bCount + 1 ) % numberOfThreads
    })


    // So, now that we have our partition buckets, we start a thread for each buckets.  This involves seeking to the desired location for each partition
    // in the bucket, then starting to POLL.
    partitionGroups.foreach(group => {

      var partitionListDisplay = group._2.map(x => {x._1.asInstanceOf[KafkaPartitionUniqueRecordKey].PartitionId}).mkString(",")
      readExecutor.execute(new Runnable() {
        var intSleepTimer = KamanjaKafkaConsumer.INITIAL_SLEEP

        // One consumer to service this group
        var kafkaConsumer: org.apache.kafka.clients.consumer.KafkaConsumer[String,String] = null

        // A Map of Execution Contexts to notify.. Assuming that The engine is still going to be running a Learning Engine
        // per partition.
        var execContexts : Array[ExecContext] = new Array[ExecContext](maxPartNumber + 1)
        var uniqueVals : Array[KafkaPartitionUniqueRecordKey] = new Array[KafkaPartitionUniqueRecordKey](maxPartNumber + 1)
        var topicPartitions: Array[org.apache.kafka.common.TopicPartition] = new Array[org.apache.kafka.common.TopicPartition](maxPartNumber + 1)
        var initialOffsets: Array[Long] = new Array[Long](maxPartNumber + 1)
        var ignoreUntilOffsets: Array[Long] = new Array[Long](maxPartNumber + 1)


        var isSeekSuccessful = false
        private def internalGetSleep : Int = {
          var thisSleep = sleepDuration
          sleepDuration = scala.math.max(KamanjaKafkaConsumer.MAX_SLEEP, thisSleep *2)
          return intSleepTimer
        }

        private def resetSleepTimer : Unit = {
          intSleepTimer =  KamanjaKafkaConsumer.INITIAL_SLEEP
        }

        // Create the connection for this thread to use and SEEK the appropriate offsets.
        while (!isSeekSuccessful && !isQuiese) {
          try {
            kafkaConsumer = createConsumerWithInputProperties
            var partitionsToMonitor: java.util.List[TopicPartition] = new util.ArrayList[TopicPartition]()

            // Initialize the monitoring status - NOTE:  we will not know which partition the exception occurred, so we
            // keep track of these for a partition group as a whole.
            partitionExceptions("Partitions-"+group._2.map(p => {p._1.asInstanceOf[KafkaPartitionUniqueRecordKey].PartitionId.toString}).toArray[String].mkString(",")) = new ExceptionInfo("n/a","n/a")
            //intitialize and seek each partition
            group._2.foreach(partitionInfo => {
              var thisPartitionNumber: Int = partitionInfo._1.asInstanceOf[KafkaPartitionUniqueRecordKey].PartitionId
              var thisTopicPartition: org.apache.kafka.common.TopicPartition = null
              LOG.debug("Seeking to requested point for topic " + qc.topic + " for partition " + thisPartitionNumber)

              // Initialize the monitoring status
              partitonCounts(thisPartitionNumber.toString) = 0
              partitonDepths(thisPartitionNumber.toString) = 0

              // Create an execution context for this partition
              if (execContexts(thisPartitionNumber) != null) {
                LOG.warn("KamanjaKafkaConsumer is creating a duplicate ExecutionContext for partition " + KamanjaKafkaConsumer)
              } else {
                val uniqueKey = new KafkaPartitionUniqueRecordKey
                thisTopicPartition = new TopicPartition(qc.topic, thisPartitionNumber)
                uniqueKey.Name = qc.Name
                uniqueKey.TopicName = qc.topic
                uniqueKey.PartitionId = thisPartitionNumber

                uniqueVals(thisPartitionNumber) = uniqueKey
                execContexts(thisPartitionNumber) = execCtxtObj.CreateExecContext(input, uniqueKey, nodeContext)
                topicPartitions(thisPartitionNumber) = thisTopicPartition
                initialOffsets(thisPartitionNumber) = partitionInfo._2.asInstanceOf[KafkaPartitionUniqueRecordValue].Offset.toLong
              }

              // Seek to the desired offset for this partition
              if (thisTopicPartition != null) partitionsToMonitor.add(thisTopicPartition)
            })
            kafkaConsumer.assign(partitionsToMonitor)

            // Ok, the partitions have been assigned, seek all the right offsets
            partitionsToMonitor.toArray.foreach(thisPartition => {

              var thisPid = thisPartition.asInstanceOf[TopicPartition].partition

              // So, initialize local offsets here.
              localReadOffsets(thisPid) = initialOffsets(thisPid)
              if (initialOffsets(thisPid) > 0) {
                localReadOffsets(thisPid) = initialOffsets(thisPid)
                ignoreUntilOffsets(thisPid) =  if (ignoreFirstMsg) initialOffsets(thisPid) else initialOffsets(thisPid) - 1
                kafkaConsumer.seek(topicPartitions(thisPid), initialOffsets(thisPid))
              }
              else {
                localReadOffsets(thisPid) = 0
                ignoreUntilOffsets(thisPid) =  if (ignoreFirstMsg) initialOffsets(thisPid) else initialOffsets(thisPid) - 1
                // kafka 0.9 api requires collections.. will optimize in a future release
                var tempCollection: java.util.ArrayList[org.apache.kafka.common.TopicPartition] = new java.util.ArrayList[org.apache.kafka.common.TopicPartition]()
                tempCollection.add(topicPartitions(thisPid))
                kafkaConsumer.seekToBeginning(topicPartitions(thisPid))
              }
            })
            isSeekSuccessful = true
            resetSleepTimer
          } catch {
            case e: Throwable => {
              externalizeExceptionEvent(e)
              LOG.warn("KamanjaKafkaConsumer Exception initializing consumer",e)
              if (kafkaConsumer != null) kafkaConsumer.close
              kafkaConsumer = null
              try {
                Thread.sleep(internalGetSleep)
              } catch {
                case ie: InterruptedException => {
                  externalizeExceptionEvent(ie)
                  Shutdown()
                  LOG.warn("KamanjaKafkaConsumer - sleep interrupted, shutting down ")
                  throw ie
                }
                case t: Throwable => {
                  externalizeExceptionEvent(t)
                  LOG.warn("KamanjaKafkaConsumer - sleep interrupted (UNKNOWN CAUSE), shutting down ",t)
                  Shutdown()
                  throw t
                }
              }

            }
          }
        }

        // This is teh guy that keeps running processing each group of partitions.
        override def run(): Unit = {
          LOG.debug("Starting to POLL ")
          while (!isQuiese) {
            try {
              var records = (kafkaConsumer.poll(KamanjaKafkaConsumer.POLLING_INTERVAL)).iterator
              while (records.hasNext && !isQuiese) {
                val readTmMs = System.currentTimeMillis

                // Process this record.
                var record: ConsumerRecord[String, String] = records.next
                var isRecordSentToKamanja = false

                // Process this record (keep doing it until success
                while (!isRecordSentToKamanja && !isQuiese) {
                  try {
                    val message: Array[Byte] = record.value.getBytes()

                    // The Engine could have told us to ignore the first offsets that we received.
                    if (record.offset() > ignoreUntilOffsets(record.partition())) {
                      val uniqueVal = new KafkaPartitionUniqueRecordValue
                      uniqueVal.Offset = record.offset
                      LOG.debug("MESSAGE-> at @partition " + record.partition + " @offset " + record.offset + " " + new String(message))

                      msgCount += 1
                      incrementCountForPartition(record.partition)
                      execContexts(record.partition).execute(message, uniqueVals(record.partition), uniqueVal, readTmMs)
                      localReadOffsets(record.partition) = (record.offset)
                      resetSleepTimer
                    }

                    isRecordSentToKamanja = true
                  } catch {
                    // This covers errors for each record... it must succeed!
                    case e: Throwable => {
                      externalizeExceptionEvent(e)
                      LOG.warn("KamanjaKafkaConsumer Exception during Kafka Queue processing " + qc.topic + ", record " + record.offset + " from partition " + record.partition + " cause: ", e)
                      var pGroup_value = "Partitions-" + group._2.map(p => {p._1.asInstanceOf[KafkaPartitionUniqueRecordKey].PartitionId.toString}).toArray[String].mkString(",")
                      partitionExceptions(pGroup_value) = new ExceptionInfo(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis)), "n/a")
                      try {
                        Thread.sleep(internalGetSleep)
                      } catch {
                        case ie: InterruptedException => {
                          externalizeExceptionEvent(ie)
                          LOG.warn("KamanjaKafkaConsumer - sleep interrupted, shutting donw ")
                          if (kafkaConsumer != null) kafkaConsumer.close
                          kafkaConsumer = null
                          throw ie
                        }
                        case t: Throwable => {
                          externalizeExceptionEvent(e)
                          LOG.warn("KamanjaKafkaConsumer - sleep interrupted (UNKNOWN CAUSE), shutting down ", t)
                          if (kafkaConsumer != null) kafkaConsumer.close
                          kafkaConsumer = null
                          throw new InterruptedException("KamanjaKafkaConsumer - sleep interrupted (UNKNOWN CAUSE), shutting down ")
                        }
                      }
                    }
                  }
                } // While still sending a record to Kamanja
              } // While there are still records in the iterator
            } catch {
              // This will cover the case when we get an exception from the POLL call.... just turn around and do another POLL.
              case ie: InterruptedException => {
                // We are catching a cotrolC or similar interrup exception from the inner try-catch... we dont want to retry that!
                LOG.warn("KamanjaKafkaConsumer - terminating reading for " + qc.topic + " partition list ["+ partitionListDisplay+"]")
                throw ie
              }
              case e: Throwable => {
                externalizeExceptionEvent(e)
                LOG.warn("KamanjaKafkaConsumer Exception during Kafka Queue processing " + qc.topic + ", cause: ",e)
                var pGroup_value = "Partitions-"+group._2.map(p => {p._1.asInstanceOf[KafkaPartitionUniqueRecordKey].PartitionId.toString}).toArray[String].mkString(",")
                partitionExceptions(pGroup_value) = new ExceptionInfo(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis)),"n/a")
                try {
                  Thread.sleep(internalGetSleep)
                } catch {
                  case ie: InterruptedException => {
                    externalizeExceptionEvent(ie)
                    LOG.warn("KamanjaKafkaConsumer - sleep interrupted, shutting down ", e)
                    if (kafkaConsumer != null) kafkaConsumer.close
                    kafkaConsumer = null
                    StopProcessing
                    throw ie
                  }
                  case t: Throwable => {
                    externalizeExceptionEvent(e)
                    LOG.warn("KamanjaKafkaConsumer - sleep interrupted (UNKNOWN CAUSE), shutting down ",t)
                    if (kafkaConsumer != null) kafkaConsumer.close
                    kafkaConsumer = null
                    StopProcessing
                    throw t
                  }
                }
              }
            }
          }  // While the consumption is going on for this topic/partition(s)

          // Clean up all the existing connections
          try {
            if (kafkaConsumer != null) kafkaConsumer.close
            kafkaConsumer = null
          } catch {
            case e: Throwable => {
              externalizeExceptionEvent(e)
              LOG.warn("KamanjaKafkaConsumer Exception trying to close kafka connections ", e)
            }
          }
        }
      })
    })

  }

  /**
    * Called by the engine to determine all the partitions avaliable for this Topic.
    *
    * @return Array[PartitionUniqueRecordKey]
    */
  override def GetAllPartitionUniqueRecordKey: Array[PartitionUniqueRecordKey] = lock.synchronized {
    LOG.debug("Getting all partionas for " + qc.Name)
    var results: java.util.List[PartitionInfo] = null
    var partitionNames: scala.collection.mutable.ListBuffer[KafkaPartitionUniqueRecordKey] = scala.collection.mutable.ListBuffer()
    var kafkaConsumer: org.apache.kafka.clients.consumer.KafkaConsumer[String,String] = null
    var isSuccessfulConnection = false

    // Create Consumer object and issue a request for known partitions.
    // per Kamanja design, we will continue trying until we success.  The retry will throttle back to 60 sec
    while (!isSuccessfulConnection && !isQuiese) {
      try {
        kafkaConsumer = createConsumerWithInputProperties()
        results = kafkaConsumer.partitionsFor(qc.topic)
        isSuccessfulConnection = true
        kafkaConsumer.close()
      } catch {
        case e: Throwable => {
          externalizeExceptionEvent(e)
          LOG.error ("Exception processing PARTITIONSFOR request..  Retrying ",e)
          try {
            Thread.sleep(getSleepTimer)
          } catch {
            case ie: InterruptedException => {
              externalizeExceptionEvent(ie)
              LOG.warn("KamanjaKafkaConsumer - sleep interrupted, shutting donw ")
              Shutdown()
              kafkaConsumer.close()
              kafkaConsumer = null
              throw ie
            }
            case t: Throwable => {
              externalizeExceptionEvent(t)
              LOG.warn("KamanjaKafkaConsumer - sleep interrupted (UNKNOWN CAUSE), shutting down ",t)
              Shutdown()
              kafkaConsumer.close()
              kafkaConsumer = null
              throw t
            }
          }
        }
      }
    }

    resetSleepTimer
    if (isQuiese) {
      // return the info back to the Engine.  if quiesing
      LOG.warn("Quiese request is receive during GetAllPartitionUniqueRecordKey for topic " + qc.topic)
      partitionNames.toArray
    }

    if (results == null)  {
      // return the info back to the Engine.  Just in case we end up with null result
      LOG.warn("Kafka broker returned a null during GetAllPartitionUniqueRecordKey for topic " + qc.topic)
      partitionNames.toArray
    }

    // Successful fetch of metadata.. return the values to the engine.
    var iter = results.iterator
    while(iter.hasNext && !isQuiese) {
      var thisRes = iter.next()
      var newVal = new KafkaPartitionUniqueRecordKey
      newVal.TopicName = thisRes.topic
      newVal.Name = qc.Name
      newVal.PartitionId = thisRes.partition
      partitionNames += newVal
      LOG.debug(" GetAllPartitions returned " +thisRes.partition + "  for topic " + thisRes.topic)
    }
    partitionNames.toArray
  }

  /**
    * Return an array of PartitionUniqueKey/PartitionUniqueRecordValues whre key is the partion and value is the offset
    * within the kafka queue where it begins.
    *
    * @return Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]
    */
  override def getAllPartitionBeginValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = lock.synchronized {
    LOG.warn("Unimplemented method getAllPartitionBeginValues is called on the KamanjaKafkaAdapter for topic " + qc.topic)
    null
  }

  /**
    * Return an array of PartitionUniqueKey/PartitionUniqueRecordValues whre key is the partion and value is the offset
    * within the kafka queue where it eds.
    *
    * @return Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]
    */
  override def getAllPartitionEndValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = lock.synchronized {

    var kafkaConsumer: KafkaConsumer[String,String] = null
    var partitionsToMonitor: java.util.List[TopicPartition] = new util.ArrayList[TopicPartition]()
    var infoList = List[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]()

    // The way we find the End offset is the following:
    //   1. Find the list of all the Partitions on this topic
    //   2. Seek to the End of each of these partitions
    //   3. call 'position' method to determine where the end is.
    try {
      kafkaConsumer = createConsumerWithInputProperties()
      var results = kafkaConsumer.partitionsFor(qc.topic)
      var iter = results.iterator
      var partitionNames: scala.collection.mutable.ListBuffer[Int] = scala.collection.mutable.ListBuffer()
      while(iter.hasNext && !isQuiese) {
        var thisRes = iter.next()
       // var newVal = new KafkaPartitionUniqueRecordKey
       // newVal.TopicName = thisRes.topic
       // newVal.PartitionId = thisRes.partition
        partitionNames += thisRes.partition
        val tp = new TopicPartition(qc.topic, thisRes.partition)
        partitionsToMonitor.add(tp)
        LOG.debug(" GetAllPartitions returned " +thisRes.partition + "  for topic " + thisRes.topic)
      }

      // Assign the partitions to monitor
      kafkaConsumer.assign(partitionsToMonitor)

      // Figure out what the end values are
      partitionNames.foreach(part => {
        val tp = new TopicPartition(qc.topic, part)
        var tempCollection: java.util.ArrayList[org.apache.kafka.common.TopicPartition] = new java.util.ArrayList[org.apache.kafka.common.TopicPartition]()
        tempCollection.add(tp)
        kafkaConsumer.seekToEnd(tp)
        var end = kafkaConsumer.position(tp)
        val rKey = new KafkaPartitionUniqueRecordKey
        val rValue = new KafkaPartitionUniqueRecordValue
        rKey.PartitionId = part
        rKey.Name = qc.Name
        rKey.TopicName = qc.topic
        rValue.Offset = end
        infoList = (rKey, rValue) :: infoList
      })

    } catch {
      case t: Throwable => {
        externalizeExceptionEvent(t)
        LOG.warn("KamanjaKafkaConsumer error encountered during HealthCheck-getDepths call", t)
        return List[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]().toArray
      }
    } finally {
      kafkaConsumer.close()
    }

    return infoList.toArray
  }

  /**
    *
    */
  override def Shutdown(): Unit = lock.synchronized {
    isQuiese = true
    StopProcessing
  }


  /**
    *
    * @param k
    * @return
    */
  override def DeserializeKey(k: String): PartitionUniqueRecordKey = {
    val key = new KafkaPartitionUniqueRecordKey
    try {
      LOG.debug("Deserializing Key:" + k)
      key.Deserialize(k)
    } catch {
      case e: Exception => {
        LOG.error("Failed to deserialize Key:%s.".format(k), e)
        throw e
      }
    }
    key
  }

  /**
    *
    * @param v
    * @return
    */
  override def DeserializeValue(v: String): PartitionUniqueRecordValue = {
    val vl = new KafkaPartitionUniqueRecordValue
    if (v != null) {
      try {
        LOG.debug("Deserializing Value:" + v)
        vl.Deserialize(v)
      } catch {
        case e: Exception => {
          LOG.error("Failed to deserialize Value:%s.".format(v), e)
          throw e
        }
      }
    }
    vl
  }

  /**
    * Will stop all the running read threads only - a call to StartProcessing will restart the reading process
    */
  override def StopProcessing(): Unit = {
    isShutdown = true
    terminateReaderTasks
  }

  /**
    *
    * @return String - Simple Stats format TYPE/NAME/envCnt->COUNT
    */
  override def getComponentSimpleStats: String = {
    return "Input/"+qc.topic+"/evtCnt" + "->" + msgCount
  }

  /**
    *
    * @return MonitorComponentInfo
    */
  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    implicit val formats = org.json4s.DefaultFormats

    var depths:  Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = null

    try {
      depths = getAllPartitionEndValues
    } catch {
      case e: KamanjaException => {
         externalizeExceptionEvent(e)
        return new MonitorComponentInfo(AdapterConfiguration.TYPE_INPUT, qc.Name, KamanjaKafkaConsumer.ADAPTER_DESCRIPTION, "0", "0", Serialization.write(metrics).toString)
      }
      case e: Throwable => {
        externalizeExceptionEvent(e)
        LOG.error ("KAFKA-ADAPTER: Unexpected exception determining kafka queue depths for " + qc.topic, e)
        return new MonitorComponentInfo(AdapterConfiguration.TYPE_INPUT, qc.Name, KamanjaKafkaConsumer.ADAPTER_DESCRIPTION, "0",  "0", Serialization.write(metrics).toString)
      }
    }

    partitonDepths.clear

    depths.foreach(t => {
      try {
        val partId = t._1.asInstanceOf[KafkaPartitionUniqueRecordKey]
        val localPart = localReadOffsets.getOrElse(partId.PartitionId,null)
        if (localPart != null) {
          val partVal = t._2.asInstanceOf[KafkaPartitionUniqueRecordValue]
          var thisDepth: Long = 0
          if(localReadOffsets.contains(partId.PartitionId)) {
            thisDepth = localReadOffsets(partId.PartitionId)
          }
          partitonDepths(partId.PartitionId.toString) = partVal.Offset - thisDepth
        }

      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          LOG.warn("KAFKA-ADAPTER: Broker:  error trying to determine kafka queue depths for "+qc.topic,e)
        }
      }
    })

    return new MonitorComponentInfo( AdapterConfiguration.TYPE_INPUT, qc.Name, KamanjaKafkaConsumer.ADAPTER_DESCRIPTION, "0",  "0",  Serialization.write(metrics).toString)
  }

  /**
    *  terminateReaderTasks - well, just what it says
    */
  private def terminateReaderTasks(): Unit = {
    if (readExecutor == null) return

    // Give the threads to gracefully stop their reading cycles, and then execute them with extreme prejudice.
    Thread.sleep(qc.noDataSleepTimeInMs + 1)
    if (readExecutor != null) readExecutor.shutdownNow
    while (readExecutor != null && readExecutor.isTerminated == false) {
      Thread.sleep(100)
    }

    LOG.debug("KAFKA_ADAPTER - Shutdown Complete")
    readExecutor = null
  }

   // Return current sleep timer and double it for the next all.  Max out at MAX_SLEEP (30 secs)...
  // resetSleepTimer will reset this to the original value
  private def getSleepTimer() : Int = {
    var thisSleep = sleepDuration
    sleepDuration = scala.math.max(KamanjaKafkaConsumer.MAX_SLEEP, thisSleep *2)
    return thisSleep
  }

  // Rest the sleep value to the INITIAL_SLEEP
  private def resetSleepTimer(): Unit = {
    sleepDuration = KamanjaKafkaConsumer.INITIAL_SLEEP
  }

  private def incrementCountForPartition(pid: Int): Unit = {
    var cVal: Long = partitonCounts.getOrElse(pid.toString, 0)
    partitonCounts(pid.toString) = cVal + 1
  }
}*/