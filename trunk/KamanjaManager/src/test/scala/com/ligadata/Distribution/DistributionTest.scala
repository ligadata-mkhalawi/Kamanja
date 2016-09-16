package com.ligadata.Distribution

import scala.collection.mutable.ArrayBuffer
import com.ligadata.KamanjaManager.NodeDistInfo
import com.ligadata.KamanjaManager.ClusterDistributionInfo

object DistributionTest {

  def test = {
    
    var tmpDistMap = ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])]()
    var allPartitionUniqueRecordKeys = Array[(String, String)]()
    allPartitionUniqueRecordKeys = Array(
      ("{\"Name\" : \"financeinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"financeinput\",\"TopicName\":\"financeinput\",\"PartitionId\":2}"),
      ("{\"Name\" : \"financeinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"financeinput\",\"TopicName\":\"financeinput\",\"PartitionId\":5}"),
      ("{\"Name\" : \"financeinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"financeinput\",\"TopicName\":\"financeinput\",\"PartitionId\":4}"),
      ("{\"Name\" : \"financeinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"financeinput\",\"TopicName\":\"financeinput\",\"PartitionId\":1}"),
      ("{\"Name\" : \"financeinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"financeinput\",\"TopicName\":\"financeinput\",\"PartitionId\":3}"),
      ("{\"Name\" : \"financeinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"financeinput\",\"TopicName\":\"financeinput\",\"PartitionId\":0}"),
      ("{\"Name\" : \"testin_1\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"testin_1\",\"TopicName\":\"testin_1\",\"PartitionId\":2}"),
      ("{\"Name\" : \"testin_1\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"testin_1\",\"TopicName\":\"testin_1\",\"PartitionId\":5}"),
      ("{\"Name\" : \"testin_1\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"testin_1\",\"TopicName\":\"testin_1\",\"PartitionId\":4}"),
      ("{\"Name\" : \"testin_1\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"testin_1\",\"TopicName\":\"testin_1\",\"PartitionId\":7}"),
      ("{\"Name\" : \"testin_1\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"testin_1\",\"TopicName\":\"testin_1\",\"PartitionId\":1}"),
      ("{\"Name\" : \"testin_1\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"testin_1\",\"TopicName\":\"testin_1\",\"PartitionId\":3}"),
      ("{\"Name\" : \"testin_1\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"testin_1\",\"TopicName\":\"testin_1\",\"PartitionId\":6}"),
      ("{\"Name\" : \"testin_1\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"testin_1\",\"TopicName\":\"testin_1\",\"PartitionId\":0}"),
      ("{\"Name\" : \"telecominput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"telecominput\",\"TopicName\":\"telecominput\",\"PartitionId\":2}"),
      ("{\"Name\" : \"telecominput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"telecominput\",\"TopicName\":\"telecominput\",\"PartitionId\":5}"),
      ("{\"Name\" : \"telecominput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"telecominput\",\"TopicName\":\"telecominput\",\"PartitionId\":4}"),
      ("{\"Name\" : \"telecominput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"telecominput\",\"TopicName\":\"telecominput\",\"PartitionId\":7}"),
      ("{\"Name\" : \"telecominput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"telecominput\",\"TopicName\":\"telecominput\",\"PartitionId\":1}"),
      ("{\"Name\" : \"telecominput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"telecominput\",\"TopicName\":\"telecominput\",\"PartitionId\":3}"),
      ("{\"Name\" : \"telecominput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"telecominput\",\"TopicName\":\"telecominput\",\"PartitionId\":6}"),
      ("{\"Name\" : \"telecominput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"telecominput\",\"TopicName\":\"telecominput\",\"PartitionId\":0}"),
      ("{\"Name\" : \"helloworldinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",\"TopicName\":\"helloworldinput\",\"PartitionId\":2}"),
      ("{\"Name\" : \"helloworldinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",\"TopicName\":\"helloworldinput\",\"PartitionId\":5}"),
      ("{\"Name\" : \"helloworldinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",\"TopicName\":\"helloworldinput\",\"PartitionId\":4}"),
      ("{\"Name\" : \"helloworldinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",\"TopicName\":\"helloworldinput\",\"PartitionId\":7}"),
      ("{\"Name\" : \"helloworldinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",\"TopicName\":\"helloworldinput\",\"PartitionId\":1}"),
      ("{\"Name\" : \"helloworldinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",\"TopicName\":\"helloworldinput\",\"PartitionId\":3}"),
      ("{\"Name\" : \"helloworldinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",\"TopicName\":\"helloworldinput\",\"PartitionId\":6}"),
      ("{\"Name\" : \"helloworldinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"helloworldinput\",\"TopicName\":\"helloworldinput\",\"PartitionId\":0}"),
      ("{\"Name\" : \"medicalinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"medicalinput\",\"TopicName\":\"medicalinput\",\"PartitionId\":2}"),
      ("{\"Name\" : \"medicalinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"medicalinput\",\"TopicName\":\"medicalinput\",\"PartitionId\":5}"),
      ("{\"Name\" : \"medicalinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"medicalinput\",\"TopicName\":\"medicalinput\",\"PartitionId\":4}"),
      ("{\"Name\" : \"medicalinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"medicalinput\",\"TopicName\":\"medicalinput\",\"PartitionId\":7}"),
      ("{\"Name\" : \"medicalinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"medicalinput\",\"TopicName\":\"medicalinput\",\"PartitionId\":1}"),
      ("{\"Name\" : \"medicalinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"medicalinput\",\"TopicName\":\"medicalinput\",\"PartitionId\":3}"),
      ("{\"Name\" : \"medicalinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"medicalinput\",\"TopicName\":\"medicalinput\",\"PartitionId\":6}"),
      ("{\"Name\" : \"medicalinput\"}", "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"medicalinput\",\"TopicName\":\"medicalinput\",\"PartitionId\":0}"))

    //   case class ClusterDistributionInfo(ClusterId: String, GlobalProcessThreads: Int, GlobalReaderThreads: Int, LogicalPartitions: Int, NodesDist: ArrayBuffer[NodeDistInfo])
    //case class NodeDistInfo(Nodeid: String, ProcessThreads: Int, ReaderThreads: Int)

    var nodeDistInfo = ArrayBuffer[NodeDistInfo]((new NodeDistInfo("node1", 8, 2)), (new NodeDistInfo("node2", 8, 2)))
    var clusterDistInfo = new ClusterDistributionInfo("cluster1", 8, 2, 8192, nodeDistInfo)
    var participantsNodes = ArrayBuffer(("node1", 10, 2), ("node2", 5, 2)) //
    var participantsNodeIds: Iterable[String] = Iterable("Thread 0", "Thread 1") //, "Thread 2", "Thread 3", "Thread 4", "Thread 5", "Thread 6", "Thread 7")
    var logicalPartitions: Int = 8192
    var globalProcessingThreads: Int = 8
    var globalReaderThreads: Int = 2

    val nodeDist = Distribution.createDistribution(clusterDistInfo, allPartitionUniqueRecordKeys)
    println("nodeDist " + nodeDist)

    val allPartsToValidate = scala.collection.mutable.Map[String, Set[String]]()
    allPartitionUniqueRecordKeys.foreach(key => {
      if (!allPartsToValidate.contains(key._1))
        allPartsToValidate(key._1) = Set(key._2)
      else {
        allPartsToValidate(key._1) = allPartsToValidate(key._1) + key._2
      }
    })
    var adapterMaxPartitions = scala.collection.mutable.Map[String, Int]()
    allPartsToValidate.foreach(p => { adapterMaxPartitions(p._1) = p._2.size })

    val distributeJson = Distribution.createDistributionJson(adapterMaxPartitions, nodeDist)
    println("distJson " + distributeJson)

  }
  def main(args: Array[String]): Unit = {
    DistributionTest.test
  }
}