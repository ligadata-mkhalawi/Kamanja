package com.ligadata.test.embedded.kafka

import java.util.Properties

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
//import org.apache.kafka.messages.NoCompressionCodec
import kafka.message.NoCompressionCodec

import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.InputAdapters.KamanjaKafkaConsumer

import com.ligadata.test.configuration.cluster.adapters._
import com.ligadata.test.utils._
import com.ligadata.KamanjaBase._
//import com.ligadata.KamanjaManager.KamanjaMetadata
import com.ligadata.test.configuration.cluster.adapters.interfaces._
import com.ligadata.tools._

import scala.collection.mutable.ListBuffer
import scala.sys.process.Process

/*
object AutoExecContextFactory extends ExecContextFactory {
  def CreateExecContext(input: InputAdapter, curPartitionKey: PartitionUniqueRecordKey, nodeContext: NodeContext): ExecContext = {
    new AutoExecContextImpl(input, curPartitionKey, nodeContext)
  }
}

class AutoExecContextImpl(val input: InputAdapter, val curPartitionKey: PartitionUniqueRecordKey, val nodeContext: NodeContext) extends ExecContext with KamanjaTestLogger {
  override def executeMessage(txnCtxt: TransactionContext): Unit = {
    try {
      val outputBuffer: ListBuffer[String] = new ListBuffer[String]
      if (Globals.modelOutputResult.exists((_._1 == input.inputConfig.Name))) {
        outputBuffer ++= Globals.modelOutputResult(input.inputConfig.Name)
      }

      outputBuffer += new String(txnCtxt.msgData)
      Globals.modelOutputResult(input.inputConfig.Name) = outputBuffer.toList
      logger.info("Output Message Added:\n\tTopic Name: " + input.inputConfig.Name + "\n\tMessage: " + new String(txnCtxt.msgData))
      logger.info(s"Total Number of output messages is: ${Globals.modelOutputResult(input.inputConfig.Name).length}")
    }
    catch {
      case e: Exception => throw new EmbeddedKafkaException("AUTOMATION-EMBEDDED-KAFKA: Exception thrown in executeMessage: ", e)
    }
  }

  override def commitData(txnCtxt: TransactionContext): Unit = println("COMMIT DATA CALLED!!!!")
}
*/

class KafkaMsgHandler extends KamanjaTestLogger {
  type OptionMap = Map[Symbol, Any]
  var isStarted: Boolean = false
  var zkNodeBasePath: String = null
  var zkConnectString: String = null
  var dataLoadCommand: String = null
  var zkDataPath: String = null
  var zkActionPath: String = null
  var seed: Long = 0
  var prc: Process = null
  var isMonitorRunning = false

  val conf = new AdapterConfiguration()
  var adapter: InputAdapter = _
  var adapterList: List[InputAdapter] = List()

  /**
    * Begins processing on the output queue as defined in com.ligadata.automation.utils.Configuration.KafkaConfiguration.
    */
  //def monitorOutputQueues(kafkaOutputConfigs: KafkaOutputAdapterConfig*): Unit = {

  /*
  def monitorOutputQueues(kafkaOutputConfigs: KafkaAdapterConfig*): Unit = {
    kafkaOutputConfigs.foreach(config => {
      try {
        val adapterConfiguration = new AdapterConfiguration
        adapterConfiguration.adapterSpecificCfg = config.adapterSpecificConfig.toString
        adapterConfiguration.className = config.className
        adapterConfiguration.dependencyJars = config.dependencyJars.toSet
        //adapterConfiguration.formatName = config.adapterSpecificConfig.topicName
        adapterConfiguration.adapterSpecificCfg = config.adapterSpecificConfig.toString
        adapterConfiguration.jarName = config.jarName
        adapterConfiguration.Name = config.name

        //val adapter = KafkaSimpleConsumer.CreateInputAdapter(adapterConfiguration, ExecContextFactoryImpl, KamanjaMetadata.gNodeContext)
        val adapter = KamanjaKafkaConsumer.CreateInputAdapter(adapterConfiguration, AutoExecContextFactory, KamanjaMetadata.gNodeContext)
        adapterList = adapterList :+ adapter
        val adapterMeta: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = adapter.getAllPartitionEndValues

        val inputMeta = adapterMeta.map(partMeta => {
          val info = new StartProcPartInfo
          info._key = partMeta._1
          info._val = partMeta._2
          info._validateInfoVal = partMeta._2
          info
        })

        if (inputMeta.length <= 0 || adapterMeta.length <= 0)
          throw new KafkaMsgHandlerException("Failed to retrieve input or adapter metadata for the topic '" +
            config.adapterSpecificConfig.topicName + "'")

        adapter.StartProcessing(inputMeta, false)
        isMonitorRunning = true
        logger.info("AUTOMATION-KAFKA-MESSAGE-HANDLER: Monitoring output topic:")
        logger.info("\tAUTOMATION-KAFKA-MESSAGE-HANDLER: " + config.adapterSpecificConfig.topicName)
        logger.info("\tAUTOMATION-KAFKA-MESSAGE-HANDLER: Broker List: " + config.adapterSpecificConfig.hostList)
      }
      catch {
        case e: Exception => logger.error("AUTOMATION-KAFKA-MESSAGE-HANDLER: Failed to monitor output queue '" + config.adapterSpecificConfig.topicName + "'")
      }
    })
  }
*/

  // This will wait for the map to be populated with the adapter config's topic name. At that point,
  // it will return the first String in the list. Use waitForOutputMessages to return the whole list.
  def waitForOutputMessage(adapterConfig: KafkaAdapterConfig, timeout: Int = 15): Option[String] = {
    logger.info("AUTOMATION-KAFKA-MESSAGE-HANDLER: Waiting for output...")
    logger.debug(s"AUTOMATION-KAFKA-MESSAGE-HANDLER: Kafka Adapter Configuration:\n ${adapterConfig.toString}")
    val topicName = adapterConfig.adapterSpecificConfig.topicName
    var count = 0

    var currentOutput: Option[List[String]] = None
    while (count < timeout * 100) {
      currentOutput = Globals.modelOutputResult.get(topicName)
      currentOutput match {
        case Some(output) => {
          logger.info("AUTOMATION-KAFKA-MESSAGE-HANDLER: Found Output:\n" + output)
          logger.debug("AUTOMATION-KAFKA-MESSAGE-HANDLER: Output found:\n" + output)
          var count = 0
          while (Globals.modelOutputResult.exists(_._1 == topicName)) {
            count += 1
            Globals.modelOutputResult = Globals.modelOutputResult - topicName
            if (count >= 30) {
              throw new KafkaMsgHandlerException("AUTOMATION-KAFKA-MESSAGE-HANDLER: Failed to remove model output results")
            }
          }
          return currentOutput.get.headOption
        }
        case None => {
          Thread sleep 10
          count += 1
        }
      }
    }
    logger.warn("Unable to retrieve output after " + timeout + " seconds.\nCurrent output reads\n" + Globals.modelOutputResult.getOrElse(topicName, ""))
    //return currentOutput.get.headOption
    return None
  }

  // Returns a list of output messages. Optional count will wait until that number of messages exists in the List.
  def waitForOutputMessages(adapterConfig: KafkaAdapterConfig, timeout: Int = 15, msgCount: Int = 1): Option[List[String]] = {
    logger.info(s"AUTOMATION-KAFKA-MESSAGE-HANDLER: Waiting for $msgCount output messages...")

    val topicName = adapterConfig.adapterSpecificConfig.topicName
    var timeoutCnt = 0
    var currentOutput: Option[List[String]] = None

    while (timeoutCnt < timeout * 100) {
      currentOutput = Globals.modelOutputResult.get(topicName)
      currentOutput match {
        case Some(output) => {
          val outputCnt: Int = output.length
          var innerCnt = 0
          if (outputCnt < msgCount) {
            logger.info(s"AUTOMATION-KAFKA-MESSAGE-HANDLER: Found $outputCnt out of $msgCount messages. Waiting for more messages...")
            Thread sleep 10
            timeoutCnt += 1
          }
          else if (outputCnt > msgCount) {
            Globals.modelOutputResult -= topicName
            throw new KafkaMsgHandlerException(s"AUTOMATION-KAFKA-MESSAGE-HANDLER: Found $outputCnt output messages but was only expecting $msgCount messages")
          }
          else {
            logger.info(s"AUTOMATION-KAFKA-MESSAGE-HANDLER: Found all $outputCnt output messages")
            while (Globals.modelOutputResult.exists(_._1 == topicName)) {
              innerCnt += 1
              Globals.modelOutputResult = Globals.modelOutputResult - topicName
              if (innerCnt >= 30) {
                throw new KafkaMsgHandlerException("AUTOMATION-KAFKA-MESSAGE-HANDLER: Failed to remove model output results")
              }
            }
            return currentOutput
          }
        }
        case None => {
          logger.warn("AUTOMATION-KAFKA-MESSAGE-HANDLER: No output found. Trying again...")
          Thread sleep 10
          timeoutCnt += 1
        }
      }
    }
    throw new KafkaMsgHandlerException("AUTOMATION-KAFKA-MESSAGE-HANDLER: Failed to retrieve output.")
  }

  def truncateOutputMessages = Globals.modelOutputResult.clear()

  /**
    * Halts monitoring of the output queue as defined in com.ligadata.automation.utils.Configuration.KafkaConfiguration.
    */
  /*
  def stopMonitoringOutputQueues(): Unit = {
    adapterList.foreach(adapter => {
      try {
        logger.info("AUTOMATION-KAFKA-MESSAGE-HANDLER: Stopping input adapter...")
        adapter.StopProcessing
        adapter.Shutdown
        isMonitorRunning = false
        zkNodeBasePath = null
        zkConnectString = null
        dataLoadCommand = null
        logger.info("AUTOMATION-KAFKA-MESSAGE-HANDLER: Input adapter stopped")
      }
      catch {
        case e: NullPointerException => logger.info("AUTOMATION-KAFKA-MESSAGE-HANDLER: No adapter to stop.")
        case e: Exception => throw new KafkaMsgHandlerException("AUTOMATION-KAFKA-MESSAGE-HANDLER: Failed to stop the adapter with the following exception:\n" + e)
      }
    })
  }
*/
  /**
    * Designed to leverage the SimpleKafkaProducer tool to input messages into a running kafka queue.
    */
  def inputMessages(iOAdapterConfig: IOAdapter, file: String, dataFormat: String, partitionKeyIdxs: String = "1"): Unit = {
    if (!dataFormat.equalsIgnoreCase("CSV") && !dataFormat.equalsIgnoreCase("JSON"))
      throw new KafkaMsgHandlerException("AUTOMATION-KAFKA-MESSAGE-HANDLER: Invalid data format '" + dataFormat + "' provided. Accepted formats: CSV | JSON")

    val validJsonMap = scala.collection.mutable.Map[String, List[String]]()
    var partKeys: Any = null
    if (dataFormat.equalsIgnoreCase("json")) {
      val msgTypeKeys = partitionKeyIdxs.replace("\"", "").trim.split(",").filter(part => part.size > 0)

      msgTypeKeys.foreach(msgType => {
        val keyStructure = msgType.split(":")
        var keyList: List[String] = List()
        keyStructure(1).split("~").foreach(key => {
          keyList = List(key) ::: keyList
        })

        validJsonMap(keyStructure(0)) = keyList
      })
      partKeys = validJsonMap
    } else {
      // This is the CSV path, partitionkeyidx is in the Array[Int] format
      // If this is old path... keep as before for now....
      partKeys = partitionKeyIdxs.replace("\"", "").trim.split(",").map(part => part.trim).filter(part => part.size > 0).map(part => part.toInt)
    }

    val (topicName, hostList) = {
      iOAdapterConfig.adapterSpecificConfig match {
        case cfg: KafkaAdapterSpecificConfig =>
          (cfg.topicName, cfg.hostList)
      }
    }

    val props = new Properties()
    props.put("bootstrap.servers", hostList)
    props.put("producer.type", "async")
    props.put("batch.num.messages", "1024")
    props.put("batch.size", "1024")
    props.put("queue.time", "50")
    props.put("queue.size", (16 * 1024 * 1024).toString)
    props.put("message.send.max.retries", "3")
    props.put("request.required.acks", "1")
    val bufferMemory: Integer = 64 * 1024 * 1024
    props.put("buffer.memory", bufferMemory.toString)
    props.put("buffer.size", bufferMemory.toString)
    props.put("socket.send.buffer", bufferMemory.toString)
    props.put("socket.receive.buffer", bufferMemory.toString)
    props.put("client.id", "Client1")
    //props.put("partitioner.class", "com.ligadata.tools.CustPartitioner")
    props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
    props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

    //val producer = new Producer[Array[Byte], Array[Byte]](new ProducerConfig(props))
    val producer = new org.apache.kafka.clients.producer.KafkaProducer[Array[Byte], Array[Byte]](props)

    try {
      //ProcessFile(producer, topics, threadNo, fl, msg, sleeptm, partitionkeyidxs, st, ignorelines, format, isGzip, topicpartitions, isVerbose.equalsIgnoreCase("true"))
      SimpleKafkaProducer.ProcessFile(producer, Array(topicName), 1, file, "", 0, partKeys, new SimpleKafkaProducer.Stats, 0, dataFormat, false, 8, true)

      //SimpleKafkaProducer.main(Array("--gz", "false", "--format", dataFormat, "--topics", topicName, "--brokerlist", hostList,
      //  "--files", file, "--partitionkeyidxs", partitionKeyIdxs, "--threads", "1", "--topicpartitions", "8"))
    }
    catch {
      case e: Exception => throw new KafkaMsgHandlerException("AUTOMATION-KAFKA-MESSAGE-HANDLER: Failed to send message to kafka", e)
    }
  }
}