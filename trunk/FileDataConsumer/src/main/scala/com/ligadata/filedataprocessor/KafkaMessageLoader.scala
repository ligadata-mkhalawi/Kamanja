package com.ligadata.filedataprocessor

import java.io.{IOException, File, PrintWriter}
import java.nio.file.StandardCopyOption._
import java.nio.file.{Paths, Files}
import java.text.SimpleDateFormat
import java.util.concurrent.{TimeUnit, Future}
import java.util.{TimeZone, Properties, Date, Arrays}
import com.ligadata.Exceptions._
import com.ligadata.KamanjaBase._
import com.ligadata.MetadataAPI.MetadataAPIImpl
import com.ligadata.Utils.{Utils, KamanjaLoaderInfo}
import com.ligadata.ZooKeeper.CreateClient
import com.ligadata.kamanja.metadata.MdMgr._
import com.ligadata.kamanja.metadata.MessageDef
import kafka.common.{QueueFullException, FailedToSendMessageException}
import kafka.producer.{KeyedMessage, Producer, Partitioner}
import org.apache.curator.framework.CuratorFramework
import org.apache.logging.log4j.{Logger, LogManager}
import kafka.utils.VerifiableProperties
import org.apache.kafka.clients.producer.{Callback, RecordMetadata, KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Formats}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Promise
import java.util.regex.Pattern


/**
  * Created by danielkozin on 9/24/15.
  */
class KafkaMessageLoader(partIdx: Int, inConfiguration: scala.collection.mutable.Map[String, String]) extends ObjectResolver {
  var fileBeingProcessed: String = ""
  var numberOfMessagesProcessedInFile: Int = 0
  var currentOffset: Int = 0
  var startFileProcessingTimeStamp: Long = 0
  var numberOfValidEvents: Int = 0
  var endFileProcessingTimeStamp: Long = 0
  val RC_RETRY: Int = 3
  var retryCount = 0
  var recPartitionMap: scala.collection.mutable.Map[Int, Int] = scala.collection.mutable.Map[Int, Int]()

  //Log the File Name and Offset when a parsing exception occurs
  var exception_metadata = inConfiguration.getOrElse(SmartFileAdapterConstants.EXCEPTION_METADATA, "false").toBoolean

  //Append File ID/Name and Offset to each message
  var message_metadata = inConfiguration.getOrElse(SmartFileAdapterConstants.ADD_METADATA_TO_MESSAGE, "false").toBoolean

  val MAX_RETRY = 1
  val INIT_KAFKA_UNAVAILABLE_WAIT_VALUE = 1000
  val MAX_WAIT = 60000
  val allowedSize: Int = inConfiguration.getOrElse(SmartFileAdapterConstants.MAX_MESSAGE_SIZE, "65536").toInt

  var currentSleepValue = INIT_KAFKA_UNAVAILABLE_WAIT_VALUE

  var lastOffsetProcessed: Int = 0
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
  dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  // Setting the UTC timezone.

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
  val producer = new KafkaProducer[Array[Byte], Array[Byte]](props)
  var numPartitionsForMainTopic = producer.partitionsFor(inConfiguration(SmartFileAdapterConstants.KAFKA_TOPIC))

  //  var delimiters = new DataDelimiters
  //  var msgFormatType = inConfiguration.getOrElse(SmartFileAdapterConstants.MSG_FORMAT,null)
  //  if (msgFormatType == null) {
  //    shutdown
  //    throw MissingPropertyException("Missing Paramter: " + SmartFileAdapterConstants.MSG_FORMAT, null)
  //  }
  //
  //  delimiters.keyAndValueDelimiter = inConfiguration.getOrElse(SmartFileAdapterConstants.KV_SEPARATOR, ":")
  //  delimiters.fieldDelimiter = inConfiguration.getOrElse(SmartFileAdapterConstants.FIELD_SEPARATOR, ":")
  //  delimiters.valueDelimiter = inConfiguration.getOrElse(SmartFileAdapterConstants.VALUE_SEPARATOR, "~")

  val keyAndValueDelimiter = inConfiguration.getOrElse(SmartFileAdapterConstants.KV_SEPARATOR, ":")
  val fieldDelimiter = inConfiguration.getOrElse(SmartFileAdapterConstants.FIELD_SEPARATOR, ":")
  val valueDelimiter = inConfiguration.getOrElse(SmartFileAdapterConstants.VALUE_SEPARATOR, "~")

  val deserializerName = inConfiguration.getOrElse(SmartFileAdapterConstants.DESERIALIZERNAME, "").trim.toLowerCase()
  val deserializerOptionsJson = inConfiguration.getOrElse(SmartFileAdapterConstants.DESERIALIZEROPTIONSJSON, "")

  if (deserializerName == null || deserializerName.size == 0) {
    shutdown
    throw MissingPropertyException("Missing Paramter: " + SmartFileAdapterConstants.DESERIALIZERNAME, null)
  }

  var debug_IgnoreKafka = inConfiguration.getOrElse("READ_TEST_ONLY", "FALSE")
  var status_frequency: Int = inConfiguration.getOrElse(SmartFileAdapterConstants.STATUS_FREQUENCY, "100000").toInt
  var isZKIgnore: Boolean = inConfiguration.getOrElse(SmartFileAdapterConstants.ZOOKEEPER_IGNORE, "FALSE").toBoolean
  var errorTopic = inConfiguration.getOrElse(SmartFileAdapterConstants.KAFKA_ERROR_TOPIC, null)
  var statusTopic = inConfiguration.getOrElse(SmartFileAdapterConstants.KAFKA_STATUS_TOPIC, null)

  val zkcConnectString = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("ZOOKEEPER_CONNECT_STRING")
  logger.debug(partIdx + " SMART FILE CONSUMER Using zookeeper " + zkcConnectString)
  var (objInst, objFullName) = configureMessageDef
  if (objInst == null) {
    shutdown
    throw UnsupportedObjectException("Unknown message definition " + inConfiguration(SmartFileAdapterConstants.MESSAGE_NAME), null)
  }

  val deserMsgBindingInfo = ResolveDeserializer(deserializerName, deserializerOptionsJson)

  if (deserMsgBindingInfo == null || deserMsgBindingInfo.serInstance == null) {
    throw new KamanjaException("Unable to resolve deserializer:" + deserializerName, null)
  }

  override def getInstance(MsgContainerType: String): ContainerInterface = {
    if (MsgContainerType.compareToIgnoreCase(objFullName) != 0)
      return null
    // Simply creating new object and returning. Not checking for MsgContainerType. This is issue if the child level messages ask for the type
    return objInst.createInstance
  }


  override def getInstance(schemaId: Long): ContainerInterface = {
    //BUGBUG:: For now we are getting latest class. But we need to get the old one too.
    if (mdMgr == null)
      throw new KamanjaException("Metadata Not found", null)

    val contOpt = mdMgr.ContainerForSchemaId(schemaId.toInt)

    if (contOpt == None)
      throw new KamanjaException("Container Not found for schemaid:" + schemaId, null)

    getInstance(contOpt.get.FullName)
  }

  override def getMdMgr = mdMgr

  private def ResolveDeserializer(deserializer: String, optionsjson: String): MsgBindingInfo = {
    val serInfo = mdMgr.GetSerializer(deserializer)
    if (serInfo == null) {
      throw new KamanjaException(s"Not found Serializer/Deserializer for ${deserializer}", null)
    }

    val phyName = serInfo.PhysicalName
    if (phyName == null) {
      throw new KamanjaException(s"Not found Physical name for Serializer/Deserializer for ${deserializer}", null)
    }

    try {
      val aclass = Class.forName(phyName).newInstance
      val ser = aclass.asInstanceOf[SerializeDeserialize]

      val map = new java.util.HashMap[String, String] //BUGBUG:: we should not convert the 2nd param to String. But still need to see how can we convert scala map to java map
      var options: collection.immutable.Map[String, Any] = null
      if (optionsjson != null) {
        implicit val jsonFormats: Formats = DefaultFormats
        val validJson = parse(optionsjson)

        options = validJson.values.asInstanceOf[collection.immutable.Map[String, Any]]
        if (options != null) {
          options.foreach(o => {
            map.put(o._1, o._2.toString)
          })
        }
      }
      ser.configure(this, map)
      ser.setObjectResolver(this)
      return MsgBindingInfo(deserializer, options, optionsjson, ser)
    } catch {
      case e: Throwable => {
        throw new KamanjaException(s"Failed to resolve Physical name ${phyName} in Serializer/Deserializer for ${deserializer}", e)
      }
    }

    return null // Should not come here
  }

  /**
    *
    * @param messages
    */
  def pushData(messages: Array[KafkaMessage]): Unit = {
    // First, if we are handling failover, then the messages could be of size 0.
    logger.info("SMART FILE CONSUMER **** processing chunk of " + messages.size + " messages")
    if (messages.size == 0) return

    // If we start processing a new file, then mark so in the zk.
    if (fileBeingProcessed.compareToIgnoreCase(messages(0).relatedFileName) != 0) {
      numberOfMessagesProcessedInFile = 0
      currentOffset = 0
      numberOfValidEvents = 0
      startFileProcessingTimeStamp = 0 //scala.compat.Platform.currentTime
      fileBeingProcessed = messages(0).relatedFileName
      recPartitionMap = messages(0).partMap

      //val fileTokens = fileBeingProcessed.split("/")
      //FileProcessor.addToZK(fileTokens(fileTokens.size - 1), 0)
      FileProcessor.addToZK(fileBeingProcessed, 0)
    }

    if (startFileProcessingTimeStamp == 0)
      startFileProcessingTimeStamp = scala.compat.Platform.currentTime

    val keyMessages = new ArrayBuffer[ProducerRecord[Array[Byte], Array[Byte]]](messages.size)

    var isLast = false
    messages.foreach(msg => {
      if (msg.offsetInFile == FileProcessor.BROKEN_FILE) {
        logger.error("SMART FILE ADAPTER " + partIdx + ": aborting kafka data push for " + msg.relatedFileName + " last successful offset for this file is " + numberOfValidEvents)

        //val tokenName = msg.relatedFileName.split("/")
        //FileProcessor.setFileOffset(tokenName(tokenName.size - 1), numberOfValidEvents)
        FileProcessor.setFileOffset(msg.relatedFileName, numberOfValidEvents)

        fileBeingProcessed = ""
        return
      }

      if (msg.offsetInFile == FileProcessor.CORRUPT_FILE) {
        logger.error("SMART FILE ADAPTER " + partIdx + ": aborting kafka data push for " + msg.relatedFileName + " Unrecoverable file corruption detected")

        //val tokenName = msg.relatedFileName.split("/")
        //FileProcessor.markFileProcessingEnd(tokenName(tokenName.size - 1))
        FileProcessor.markFileProcessingEnd(msg.relatedFileName)

        writeGenericMsg("Corrupt file detected", msg.relatedFileName, inConfiguration(SmartFileAdapterConstants.KAFKA_STATUS_TOPIC))
        closeOutFile(msg.relatedFileName, true)
        fileBeingProcessed = ""
        return
      }

      if (!msg.isLastDummy) {
        numberOfValidEvents += 1
        //        var inputData: InputData = null
        var msgStr: String = null
        try {

          //Pass in the complete message instead of just the message string
          //          inputData = CreateKafkaInput(msg, SmartFileAdapterConstants.MESSAGE_NAME, delimiters)
          msgStr = new String(msg.msg)

          if (msgStr.size > allowedSize) throw new KVMessageFormatingException("Message size exceeds the maximum alloweable size ", null)

          if (message_metadata && !msgStr.startsWith("fileId")) {
            msgStr = "fileId" + keyAndValueDelimiter + FileProcessor.getIDFromFileCache(msg.relatedFileName) +
              fieldDelimiter +
              "fileOffset" + keyAndValueDelimiter + msg.msgOffset.toString() +
              fieldDelimiter + msgStr
          }

          currentOffset += 1

          // val partitionKey = objInst.asInstanceOf[MessageContainerObjBase].PartitionKeyData(inputData).mkString(",")

          // BUGBUG:: Previously we were using msg.msg. Should we use msgStr or msg.msg.
          val container = deserMsgBindingInfo.serInstance.deserialize(msgStr.getBytes(), objFullName)
          val partitionKey = container.getPartitionKey.mkString(",")

          // By far the most common path..  just add the message
          if (msg.offsetInFile == FileProcessor.NOT_RECOVERY_SITUATION) {
            keyMessages += new ProducerRecord[Array[Byte], Array[Byte]](inConfiguration(SmartFileAdapterConstants.KAFKA_TOPIC),
              getPartition(partitionKey.getBytes("UTF8"), numPartitionsForMainTopic.size),
              partitionKey.getBytes("UTF8"),
              msgStr.getBytes("UTF8"))
            numberOfMessagesProcessedInFile += 1
          } else {
            // Recovery... do checking for the message
            // Only add those messages that we have not previously processed....
            if (msg.offsetInFile >= 0 && msg.offsetInFile < currentOffset) {
              // We may be part of in doubt batch.. gotta check partition recovery info
              var partition = getPartition(partitionKey.getBytes("UTF8"), numPartitionsForMainTopic.size)
              if ((!recPartitionMap.contains(partition) ||
                recPartitionMap.contains(partition) && recPartitionMap(partition) == 0)) {
                keyMessages += new ProducerRecord[Array[Byte], Array[Byte]](inConfiguration(SmartFileAdapterConstants.KAFKA_TOPIC),
                  getPartition(partitionKey.getBytes("UTF8"), numPartitionsForMainTopic.size),
                  partitionKey.getBytes("UTF8"),
                  msgStr.getBytes("UTF8"))
                numberOfMessagesProcessedInFile += 1
              } else {
                if (recPartitionMap.contains(partition)) {
                  // The partition key exists and its > 0...  meaning we need to ignore the messagem but need to
                  // decrement the counter
                  recPartitionMap(partition) = recPartitionMap(partition) - 1
                }
              }
            } else {
              // Ignoring shit...
            }
          }
        } catch {
          case mfe: KVMessageFormatingException => {
            logger.warn("Unknown message format in partition " + partIdx, mfe)
            writeErrorMsg(msg)
          }
          case e: Exception => {
            logger.warn("Unknown message format in partition " + partIdx, e)
            writeErrorMsg(msg)
          }
          case e: Throwable => {
            logger.warn("Unknown message format in partition " + partIdx, e)
            writeErrorMsg(msg)
          }
        }

        if (msg.isLast) {
          isLast = true
        }
      } else {
        isLast = true
      }
    })

    // Write to kafka
    logger.info("KafkaMessageLoader: pushData: Sending " + numberOfValidEvents)
    if (isLast)
      sendToKafka(keyMessages, "msgPush1", numberOfValidEvents)
    else
      sendToKafka(keyMessages, "msgPush2", (numberOfValidEvents - messages.size), fileBeingProcessed)

    // Make sure you dont write extra for DummyLast
    if (!isLast) {
      writeStatusMsg(fileBeingProcessed)

      //val fileTokens = fileBeingProcessed.split("/")
      //FileProcessor.addToZK(fileTokens(fileTokens.size - 1), numberOfValidEvents)
      FileProcessor.addToZK(fileBeingProcessed, numberOfValidEvents)
    } else {
      // output the status message to the KAFAKA_STATUS_TOPIC
      writeStatusMsg(fileBeingProcessed, true)
      closeOutFile(fileBeingProcessed)
      numberOfMessagesProcessedInFile = 0
      currentOffset = 0
      startFileProcessingTimeStamp = 0
      numberOfValidEvents = 0
    }
  }

  /**
    *
    * @param messages
    * @return
    */
  //private def sendToKafka(messages: ArrayBuffer[KeyedMessage[Array[Byte], Array[Byte]]]): Int = {
  private def sendToKafka(messages: ArrayBuffer[ProducerRecord[Array[Byte], Array[Byte]]], sentFrom: String, fullSuccessOffset: Int = 0, fileToUpdate: String = null): Int = {
    try {
      var partitionsStats = scala.collection.mutable.Map[Int, Int]()
      logger.info("SMART FILE CONSUMER (" + partIdx + ") Sending " + messages.size + " to kafka from " + sentFrom)
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


      while (!isFullySent) {
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
                  logger.warn("SMART FILE CONSUMER (" + partIdx + ") has detected a problem with pushing a message into the " + msg.topic + " will retry " + exception.getMessage)
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

        var sleepTmRemainingInMs = 30000
        // Make sure all messages have been successfuly sent, and resend them if we detected bad messages
        isFullySent = true
        var i = messages.size - 1
        while (i >= 0) {
          if (!successVector(i) && !gotResponse(i)) {
            val tmConsumed = System.nanoTime
            val (rc, partitionId) = checkMessage(respFutures, i, sleepTmRemainingInMs)
            var tmElapsed = System.nanoTime - tmConsumed
            if (tmElapsed < 0) tmElapsed = 0
            sleepTmRemainingInMs -= (tmElapsed / 1000000).toInt
            if (rc > 0) {
              isFullySent = false
              isRetry = true
            } else {
              if (partitionsStats.contains(partitionId)) {
                partitionsStats(partitionId) = partitionsStats(partitionId) + 1
              } else {
                partitionsStats(partitionId) = 1
              }
              successVector(i) = true
            }
          }
          i -= 1
        }

        // We can now fail for some messages, so, we need to update the recovery area in ZK, to make sure the retry does not
        // process these messages.
        if (fileToUpdate != null) {
          FileProcessor.addToZK(fileBeingProcessed, fullSuccessOffset, partitionsStats)
        }
      }
      resetSleepTimer
    } catch {
      case e: Exception =>
        logger.error("SMART FILE CONSUMER (" + partIdx + ") Could not add to the queue due to an Exception " + e.getMessage, e)
      case e: Throwable => {
        logger.error("SMART FILE CONSUMER (" + partIdx + ") Could not add to the queue due to an Exception " + e.getMessage, e)
      }
    }
    FileProcessor.KAFKA_SEND_SUCCESS
  }

  private def checkMessage(mapF: scala.collection.mutable.Map[Int, Future[RecordMetadata]], i: Int, sleepTmRemainingInMs: Int): (Int, Int) = {
    try {
      val md = mapF(i).get(if (sleepTmRemainingInMs <= 0) 1 else sleepTmRemainingInMs, TimeUnit.MILLISECONDS)
      mapF(i) = null
      return (FileProcessor.KAFKA_SEND_SUCCESS, md.partition)
    } catch {
      case e1: java.util.concurrent.ExecutionException => {
        return (FileProcessor.KAFKA_SEND_DEAD_PRODUCER, -1)
      }
      case e: java.util.concurrent.TimeoutException => {
        return (FileProcessor.KAFKA_SEND_DEAD_PRODUCER, -1)
      }
      // case ftsme: FailedToSendMessageException => {
      case ftsme: java.lang.InterruptedException => {
        return (FileProcessor.KAFKA_SEND_DEAD_PRODUCER, -1)
      }
      case e: Exception => {
        logger.error("CHECK_MESSAGE: Unknown error from Kafka ", e);
        throw e
      }
      case e: Throwable => {
        logger.error("CHECK_MESSAGE: Unknown error from Kafka ", e);
        throw e
      }
    }
  }

  private def getCurrentSleepTimer: Int = {
    currentSleepValue = currentSleepValue * 2
    logger.error("SMART FILE CONSUMER (" + partIdx + ") detected a problem with Kafka... Retry in " + scala.math.min(currentSleepValue, MAX_WAIT) / 1000 + " seconds")
    currentSleepValue = scala.math.min(currentSleepValue, MAX_WAIT)
    currentSleepValue
  }

  private def resetSleepTimer: Unit = {
    currentSleepValue = INIT_KAFKA_UNAVAILABLE_WAIT_VALUE
  }

  /**
    *
    * @param fileName
    */
  private def closeOutFile(fileName: String, isResultOfCorruption: Boolean = false): Unit = {
    try {
      logger.info("SMART FILE CONSUMER (" + partIdx + ") - cleaning up after " + fileName)
      // Either move or rename the file.

      val fileStruct = fileName.split("/")

      if (isResultOfCorruption) writeStatusMsg(fileName, true)

      //Take care of multiple directories
      logger.info("SMART FILE CONSUMER (" + partIdx + ") Moving File" + fileName + " to " + inConfiguration(SmartFileAdapterConstants.DIRECTORY_TO_MOVE_TO))
      FileProcessor.executeCallWithElapsed(Files.move(Paths.get(fileName), Paths.get(inConfiguration(SmartFileAdapterConstants.DIRECTORY_TO_MOVE_TO) + "/" + fileStruct(fileStruct.size - 1)), REPLACE_EXISTING), "Moving file " + fileName)

      //Use the full filename
      FileProcessor.removeFromZK(fileName)
      FileProcessor.markFileProcessingEnd(fileName)
      FileProcessor.fileCacheRemove(fileName)

    } catch {
      case ioe: IOException => {
        logger.error("Exception moving the file ", ioe)
        //var tokenName = fileName.split("/")
        //FileProcessor.setFileState(tokenName(tokenName.size - 1),FileProcessor.FINISHED_FAILED_TO_COPY)
        FileProcessor.setFileState(fileName, FileProcessor.FINISHED_FAILED_TO_COPY)
      }
      case e: Throwable => {
        logger.error("Exception moving the file ", e)
        //var tokenName = fileName.split("/")
        //FileProcessor.setFileState(tokenName(tokenName.size - 1),FileProcessor.FINISHED_FAILED_TO_COPY)
        FileProcessor.setFileState(fileName, FileProcessor.FINISHED_FAILED_TO_COPY)
      }
    }
  }

  /**
    *
    * @param fileName
    */
  private def writeStatusMsg(fileName: String, isTotal: Boolean = false): Unit = {

    if (statusTopic == null) return

    var fileId = FileProcessor.getIDFromFileCache(fileName)

    try {
      val cdate: Date = new Date
      if (inConfiguration.getOrElse(SmartFileAdapterConstants.KAFKA_STATUS_TOPIC, "").length > 0) {
        endFileProcessingTimeStamp = scala.compat.Platform.currentTime
        var statusMsg: String = null
        //val nameToken = fileName.split("/")
        if (!isTotal) {
          if (message_metadata) {
            statusMsg = SmartFileAdapterConstants.KAFKA_LOAD_STATUS + dateFormat.format(cdate) + "," + fileName + "," + fileId + "," + numberOfMessagesProcessedInFile + "," + (endFileProcessingTimeStamp - startFileProcessingTimeStamp)
          } else {
            statusMsg = SmartFileAdapterConstants.KAFKA_LOAD_STATUS + dateFormat.format(cdate) + "," + fileName + "," + numberOfMessagesProcessedInFile + "," + (endFileProcessingTimeStamp - startFileProcessingTimeStamp)

          }
        } else {
          if (message_metadata) {
            statusMsg = SmartFileAdapterConstants.TOTAL_FILE_STATUS + dateFormat.format(cdate) + "," + fileName + "," + fileId + "," + numberOfMessagesProcessedInFile + "," + (endFileProcessingTimeStamp - FileProcessor.getTimingFromFileCache(fileName))
          } else {
            //statusMsg = SmartFileAdapterConstants.TOTAL_FILE_STATUS + dateFormat.format(cdate) + "," + fileName + "," + numberOfMessagesProcessedInFile + "," + (endFileProcessingTimeStamp - FileProcessor.getTimingFromFileCache(nameToken(nameToken.size - 1)))
            statusMsg = SmartFileAdapterConstants.TOTAL_FILE_STATUS + dateFormat.format(cdate) + "," + fileName + "," + numberOfMessagesProcessedInFile + "," + (endFileProcessingTimeStamp - FileProcessor.getTimingFromFileCache(fileName))
          }
        }

        val statusPartitionId = "it does not matter"

        // Write a Status Message
        val keyMessages = new ArrayBuffer[ProducerRecord[Array[Byte], Array[Byte]]](1)
        keyMessages += new ProducerRecord(inConfiguration(SmartFileAdapterConstants.KAFKA_STATUS_TOPIC), statusPartitionId.getBytes("UTF8"), new String(statusMsg).getBytes("UTF8"))

        sendToKafka(keyMessages, "Status")

        logger.debug("Status pushed ->" + statusMsg)
      } else {
        logger.debug("NO STATUS Q SPECIFIED")
      }
    } catch {
      case e: Exception => {
        logger.warn(partIdx + " SMART FILE CONSUMER: Unable to externalize status message", e)
      }
      case e: Throwable => {
        logger.warn(partIdx + " SMART FILE CONSUMER: Unable to externalize status message", e)
      }
    }
  }

  /**
    *
    * @param msg
    */
  private def writeErrorMsg(msg: KafkaMessage): Unit = {

    if (errorTopic == null) return

    val cdate: Date = new Date

    // if the message is corrupted and results in a message larger then kafka can accept trucate it to a reasonable size\
    val org_error_msg: String = new String(msg.msg)
    var error_msg = org_error_msg
    if (error_msg.size > allowedSize) {
      error_msg = error_msg.substring(0, allowedSize - 1)
    }

    logger.warn(" SMART FILE CONSUMER (" + partIdx + "): invalid message in file " + msg.relatedFileName)

    val keyMessages = new ArrayBuffer[ProducerRecord[Array[Byte], Array[Byte]]](1)

    if (exception_metadata) {
      //Add message offset
      val org_errorMsg2 = dateFormat.format(cdate) + "," + msg.relatedFileName + "," + msg.msgOffset + "," + org_error_msg
      logger.warn(org_errorMsg2)
      val errorMsg2 = dateFormat.format(cdate) + "," + msg.relatedFileName + "," + msg.msgOffset + "," + error_msg
      keyMessages += new ProducerRecord(inConfiguration(SmartFileAdapterConstants.KAFKA_ERROR_TOPIC),
        "rare event".getBytes("UTF8"), errorMsg2.getBytes("UTF8"))
    } else {
      val org_errorMsg1 = dateFormat.format(cdate) + "," + msg.relatedFileName + "," + org_error_msg
      logger.warn(org_errorMsg1)
      val errorMsg1 = dateFormat.format(cdate) + "," + msg.relatedFileName + "," + error_msg
      keyMessages += new ProducerRecord(inConfiguration(SmartFileAdapterConstants.KAFKA_ERROR_TOPIC),
        "rare event".getBytes("UTF8"), errorMsg1.getBytes("UTF8"))
    }
    // Write a Error Message
    sendToKafka(keyMessages, "Error")
  }

  private def writeGenericMsg(msg: String, fileName: String, topicName: String): Unit = {

    if (statusTopic == null) return

    val cdate: Date = new Date
    // Corrupted_File_Detected,Date-XXXXXX,FileName,-1
    val genMsg = SmartFileAdapterConstants.CORRUPTED_FILE + dateFormat.format(cdate) + "," + fileName + ",-1"
    logger.warn(" SMART FILE CONSUMER (" + partIdx + "): problem in file " + fileName)
    logger.warn(genMsg)

    // Write a Error Message
    //    val keyMessages = new ArrayBuffer[KeyedMessage[Array[Byte], Array[Byte]]](1)
    //   keyMessages += new KeyedMessage(topicName, "rare event".getBytes("UTF8"), genMsg.getBytes("UTF8"))
    val keyMessages = new ArrayBuffer[ProducerRecord[Array[Byte], Array[Byte]]](1)
    keyMessages += new ProducerRecord(topicName, "rare event".getBytes("UTF8"), genMsg.getBytes("UTF8"))

    sendToKafka(keyMessages, "Generic - Corrupt")

  }

  /**
    *
    * //   * @param inputData
    * //   * @param associatedMsg
    * //   * @param delimiters
    * //   * @return
    */
  //  private def CreateKafkaInput(inputData: KafkaMessage, associatedMsg: String, delimiters: DataDelimiters): InputData = {
  //
  //    val msgStr = new String(inputData.msg)
  //
  //    if (associatedMsg == null || associatedMsg.size == 0) {
  //      throw new Exception("KV data expecting Associated messages as input.")
  //    }
  //
  //    if (delimiters.fieldDelimiter == null) delimiters.fieldDelimiter = ","
  //    if (delimiters.valueDelimiter == null) delimiters.valueDelimiter = "~"
  //    if (delimiters.keyAndValueDelimiter == null) delimiters.keyAndValueDelimiter = "\\x01"
  //
  //    //Add Patterns
  //    val fieldPattern = Pattern.quote(delimiters.fieldDelimiter)
  //    val kvPattern = Pattern.quote(delimiters.keyAndValueDelimiter)
  //    val valuePattern = Pattern.quote(delimiters.valueDelimiter)
  //
  //    val str_arr = msgStr.split(fieldPattern, -1)
  //
  //    val inpData = new KvData(msgStr, delimiters)
  //    val dataMap = scala.collection.mutable.Map[String, String]()
  //
  //    if (delimiters.fieldDelimiter.compareTo(delimiters.keyAndValueDelimiter) == 0) {
  //      if (str_arr.size % 2 != 0) {
  //
  //        val errStr1 = "Expecting Key & Value pairs are even number of tokens when FieldDelimiter & KeyAndValueDelimiter are matched. We got %d tokens from input string %s".format(str_arr.size, msgStr)
  //        val errStr2 = "Expecting Key & Value pairs to be even number of tokens when FieldDelimiter & KeyAndValueDelimiter are matched. We got %d tokens from input string %s, reading file %s at offset %d".format(str_arr.size, msgStr, inputData.relatedFileName, inputData.msgOffset)
  //
  //        //Flag to handle logging the exception metadata
  //        if(exception_metadata){
  //          logger.error(errStr2)
  //          throw KVMessageFormatingException(errStr2, null)
  //        }else{
  //          logger.error(errStr1)
  //          throw KVMessageFormatingException(errStr1, null)
  //        }
  //
  //      }
  //      for (i <- 0 until str_arr.size by 2) {
  //        dataMap(str_arr(i).trim) = str_arr(i + 1)
  //      }
  //    } else {
  //      str_arr.foreach(kv => {
  //        val kvpair = kv.split(kvPattern)
  //        if (kvpair.size != 2) {
  //          throw KVMessageFormatingException("Expecting Key & Value pair only ", null)
  //        }
  //        dataMap(kvpair(0).trim) = kvpair(1)
  //      })
  //    }
  //
  //    inpData.dataMap = dataMap.toMap
  //    inpData
  //
  //  }

  /**
    *
    * @return
    */
  private def configureMessageDef(): (com.ligadata.KamanjaBase.MessageFactoryInterface, String) = {
    val loaderInfo = new KamanjaLoaderInfo()
    var msgDef: MessageDef = null
    var baseMsgObj: com.ligadata.KamanjaBase.MessageFactoryInterface = null
    var objFullName: String = null
    try {
      val (typNameSpace, typName) = com.ligadata.kamanja.metadata.Utils.parseNameTokenNoVersion(inConfiguration(SmartFileAdapterConstants.MESSAGE_NAME))
      //  msgDef =  FileProcessor.getMsgDef(typNameSpace,typName) //
      msgDef = mdMgr.ActiveMessage(typNameSpace, typName)
    } catch {
      case e: Exception => {
        shutdown
        logger.error("Unable to to parse message defintion", e)
        throw new UnsupportedObjectException("Unknown message definition " + inConfiguration(SmartFileAdapterConstants.MESSAGE_NAME), null)
      }
      case e: Throwable => {
        shutdown
        logger.error("Unable to to parse message defintion", e)
        throw new UnsupportedObjectException("Unknown message definition " + inConfiguration(SmartFileAdapterConstants.MESSAGE_NAME), null)
      }
    }

    if (msgDef == null) {
      shutdown
      logger.error("Unable to to retrieve message defintion")
      throw UnsupportedObjectException("Unknown message definition " + inConfiguration(SmartFileAdapterConstants.MESSAGE_NAME), null)
    }
    // Just in case we want this to deal with more then 1 MSG_DEF in a future.  - msgName paramter will probably have to
    // be an array inthat case.. but for now......
    var allJars = collection.mutable.Set[String]()
    allJars = allJars + msgDef.jarName

    var jarPaths0 = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_PATHS").split(",").toSet
    jarPaths0 = jarPaths0 + MetadataAPIImpl.GetMetadataAPIConfig.getProperty("COMPILER_WORK_DIR")

    Utils.LoadJars(allJars.map(j => Utils.GetValidJarFile(jarPaths0, j)).toArray, loaderInfo.loadedJars, loaderInfo.loader)
    val jarName0 = Utils.GetValidJarFile(jarPaths0, msgDef.jarName)
    var classNames = Utils.getClasseNamesInJar(jarName0)

    var tempCurClass: Class[_] = null
    classNames.foreach(clsName => {
      try {
        Class.forName(clsName, true, loaderInfo.loader)
      } catch {
        case e: Exception => {
          logger.error("Failed to load Model class " + clsName, e)
          throw e // Rethrow
        }
        case e: Throwable => {
          logger.error("Failed to load Model class " + clsName, e)
          throw e // Rethrow
        }
      }

      var curClz = Class.forName(clsName, true, loaderInfo.loader)
      tempCurClass = curClz

      var isMsg = false
      while (curClz != null && isMsg == false) {
        isMsg = Utils.isDerivedFrom(curClz, "com.ligadata.KamanjaBase.MessageFactoryInterface")
        if (isMsg == false)
          curClz = curClz.getSuperclass()
      }

      if (isMsg) {
        try {
          // Trying Singleton Object
          val module = loaderInfo.mirror.staticModule(clsName)
          val obj = loaderInfo.mirror.reflectModule(module)
          val objInst = obj.instance
          objFullName = msgDef.FullName
          baseMsgObj = objInst.asInstanceOf[com.ligadata.KamanjaBase.MessageFactoryInterface]
        } catch {
          case e: java.lang.NoClassDefFoundError => {
            logger.error("SMART FILE CONSUMER:  ERROR", e)
            throw e
          }
          case e: Exception => {
            try {
              val objInst = tempCurClass.newInstance
              objFullName = msgDef.FullName
              baseMsgObj = objInst.asInstanceOf[com.ligadata.KamanjaBase.MessageFactoryInterface]
            } catch {
              case e: Throwable => {
                logger.error("SMART FILE CONSUMER:  ERROR", e)
                throw e
              }
            }
          }
          case e: Throwable => {
            logger.warn("SMART FILE CONSUMER:  ERROR", e)
            throw e
          }
        }
      }
    })
    return (baseMsgObj, objFullName)
  }

  private def getPartition(key: Any, numPartitions: Int): Int = {
    val random = new java.util.Random
    if (key != null) {
      try {
        if (key.isInstanceOf[Array[Byte]]) {
          return (scala.math.abs(Arrays.hashCode(key.asInstanceOf[Array[Byte]])) % numPartitions)
        } else if (key.isInstanceOf[String]) {
          return (key.asInstanceOf[String].hashCode() % numPartitions)
        }
      } catch {
        case e: Exception => {
          logger.warn("SMART FILE CONSUMER:  ERROR in getPartition", e)
        }
        case e: Throwable => {
          logger.warn("SMART FILE CONSUMER:  ERROR in getPartition", e)
        }
      }
    }
    return random.nextInt(numPartitions)
  }

  /**
    *
    */
  private def shutdown: Unit = {
    MetadataAPIImpl.shutdown
    if (producer != null)
      producer.close

    Thread.sleep(2000)
  }
}
