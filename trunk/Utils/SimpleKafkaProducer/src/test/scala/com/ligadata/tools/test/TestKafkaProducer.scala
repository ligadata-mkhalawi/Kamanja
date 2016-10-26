package com.ligadata.tools.test

import java.util.Properties

import com.ligadata.InputOutputAdapterInfo.AdapterConfiguration
import com.ligadata.test.configuration.cluster.adapters.KafkaAdapterSpecificConfig
import com.ligadata.tools.SimpleKafkaProducer
import com.ligadata.test.configuration.cluster.zookeeper._
import com.ligadata.test.configuration.cluster.adapters.interfaces._

class TestKafkaProducer {
  /**
    * Designed to leverage the SimpleKafkaProducer tool to input messages into a running kafka queue.
    */
  def inputMessages(iOAdapterConfig: IOAdapter, file: String, dataFormat: String, partitionKeyIdxs: String = "1"): Unit = {
    if (!dataFormat.equalsIgnoreCase("CSV") && !dataFormat.equalsIgnoreCase("JSON"))
      throw new Exception("[Test Kafka Producer]: Invalid data format '" + dataFormat + "' provided. Accepted formats: CSV | JSON")

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

    val props = new Properties() {
      put("bootstrap.servers", hostList)
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
      //props.put("partitioner.class", "com.ligadata.tools.CustPartitioner")
      put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
      put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer")
    }

    //val producer = new Producer[Array[Byte], Array[Byte]](new ProducerConfig(props))
    //val producer = new org.apache.kafka.clients.producer.KafkaProducer[Array[Byte], Array[Byte]](props)
    //val oAdapter = com.ligadata.kafkaInputOutputAdapters_v10.KafkaProducer.CreateOutputAdapter(props, null)

    val adapterConfiguration = new AdapterConfiguration{
      Name = iOAdapterConfig.name
      className = iOAdapterConfig.className
      jarName = iOAdapterConfig.jarName
      dependencyJars = iOAdapterConfig.dependencyJars.toSet
      adapterSpecificCfg = iOAdapterConfig.adapterSpecificConfig.toString
      tenantId = iOAdapterConfig.tenantId
    }

    val oAdapter = com.ligadata.kafkaInputOutputAdapters_v10.KafkaProducer.CreateOutputAdapter(adapterConfiguration, null)

    try {
      //ProcessFile(producer, topics, threadNo, fl, msg, sleeptm, partitionkeyidxs, st, ignorelines, format, isGzip, topicpartitions, isVerbose.equalsIgnoreCase("true"))
      println(s"Pushing data to topic $topicName")
      println(s"Adapter: " + oAdapter.toString)
      SimpleKafkaProducer.ProcessFile(oAdapter, Array(topicName), 1, file, "", 0, partKeys, new SimpleKafkaProducer.Stats, 0, dataFormat, false, 8, true)

      //SimpleKafkaProducer.main(Array("--gz", "false", "--format", dataFormat, "--topics", topicName, "--brokerlist", hostList,
      //  "--files", file, "--partitionkeyidxs", partitionKeyIdxs, "--threads", "1", "--topicpartitions", "8"))
    }
    catch {
      case e: Exception => throw new Exception("[Test Kafka Producer]: Failed to send message to kafka", e)
    }
    finally {
      oAdapter.Shutdown
    }
  }
}
