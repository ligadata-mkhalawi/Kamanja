package com.ligadata.Distribution

import scala.collection.mutable.ArrayBuffer
import com.ligadata.KamanjaManager.NodeDistInfo
import com.ligadata.KamanjaManager.{ ClusterDistributionInfo }
//import com.ligadata.KamanjaManager.ActionOnAdaptersMap
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s._

//case class DistributionMap(var action: String, var globalprocessthreads: Int, var globalreaderthreads: Int, var logicalpartitions: Int, var totalreaderthreads: Int, var totalprocessthreads: Int, adaptermaxpartitions: Option[List[AdapMaxPartitions1]], var distributionmap: List[NodeDistMap])
case class DistributionMap(var action: String, var totalreaderthreads: Option[Int], var totalprocessthreads: Option[Int], adaptermaxpartitions: Option[List[AdapMaxPartitions1]], var distributionmap: List[NodeDistMap])

case class AdapMaxPartitions1(Adap: String, MaxParts: Int)
case class NodeDistMap(Node: String, PhysicalPartitions: List[PhysicalPartitions], LogicalPartitions: List[LogicalPartitions])
case class PhysicalPartitions(var ThreadId: String, Adaps: List[Adaps])
case class Adaps(var Adap: String, var ReadPartitions: List[String])
case class LogicalPartitions(var ThreadId: Short, var SRange: Int, var ERange: Int)

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

    val distributeJson = Distribution.createDistributionJson(nodeDist)
    //val distributeJson = "{\"action\":\"stop\"}"
    println("distJson " + distributeJson)
    // println("distJson 1 " + distributeJson)
    val json = parse(distributeJson)
    if (json == null || json.values == null) { // Not doing any action if not found valid json
      println("ActionOnAdaptersDistImpl => Exit. receivedJsonStr: " + distributeJson)
    }

    implicit val jsonFormats: Formats = DefaultFormats
    val actionOnAdaptersMap = json.extract[DistributionMap]
    println("totalprocessthreads====> " + actionOnAdaptersMap.totalprocessthreads)
    println("totalreaderthreads====> " + actionOnAdaptersMap.totalreaderthreads)
    println(actionOnAdaptersMap.adaptermaxpartitions)

    println(actionOnAdaptersMap.distributionmap)
    val nodeId = "node1"
    val nodeDistMap = GetNodeDistMapForNodeId(actionOnAdaptersMap.distributionmap, nodeId)

    GetNodeDistLogicalPartsMapForNodeId(actionOnAdaptersMap.distributionmap, nodeId)

  }

  private def GetNodeDistMapForNodeId(distributionmap: List[NodeDistMap], nodeId: String): scala.collection.mutable.Map[String, ArrayBuffer[(String, List[String])]] = {
    var threadsPartitionMap = scala.collection.mutable.Map[String, ArrayBuffer[(String, List[String])]]()
    distributionmap.foreach { nodedist =>
      {
        if (nodedist.Node == nodeId) {
          nodedist.PhysicalPartitions.foreach { physicalPart =>
            {
              physicalPart.Adaps.foreach { adap =>
                {
                  val threadPartsMap = (physicalPart.ThreadId, adap.ReadPartitions)
                  println(physicalPart.ThreadId + "  " + adap.ReadPartitions)
                  if (threadsPartitionMap.contains(adap.Adap))
                    threadsPartitionMap(adap.Adap) += threadPartsMap
                  else threadsPartitionMap(adap.Adap) = ArrayBuffer(threadPartsMap)
                }
              }
            }
          }
        }
      }
    }
    println(threadsPartitionMap)
    threadsPartitionMap
  }

  private def GetNodeDistLogicalPartsMapForNodeId(distributionmap: List[NodeDistMap], nodeId: String): scala.collection.mutable.Map[Int, (Int, Int)] = {
    var threadsPartitionMap = scala.collection.mutable.Map[Int, (Int, Int)]()
    distributionmap.foreach { nodedist =>
      {
        if (nodedist.Node == nodeId) {
          if (nodedist.LogicalPartitions != null && nodedist.LogicalPartitions.size > 0) {
            nodedist.LogicalPartitions.foreach { logicalPart =>
              {
                var low: Int = 0
                var high: Int = 0
                if (logicalPart != null) {
                  low = logicalPart.SRange
                  high = logicalPart.ERange
                  val range = (low.toInt, high.toInt)
                  threadsPartitionMap(logicalPart.ThreadId.toInt) = range
                }
              }
            }
          }
          println("Logical APrts " + threadsPartitionMap)
        }
      }
    }
    threadsPartitionMap
  }

  def main(args: Array[String]): Unit = {
    DistributionTest.test
  }
}