package com.ligadata.Distribution

import scala.collection.mutable.ArrayBuffer
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.Json
import org.json4s.jackson.JsonMethods._
import org.apache.logging.log4j.{ LogManager, Logger }
import com.ligadata.KamanjaManager.NodeDistInfo
import com.ligadata.KamanjaManager.ClusterDistributionInfo

case class Node(var Name: String, var ProcessThreads: Int, var ReaderThreads: Int)
case class NodeDistributionMap(Node: String, PhysicalPartitions: ArrayBuffer[PhysicalPartitionsDist], LogicalPartitionsDist: ArrayBuffer[LogicalPartitionsDist])
case class PhysicalPartitionsDist(var ThreadId: String, AdapterPartitions: scala.collection.mutable.Map[String, ArrayBuffer[String]])
case class LogicalPartitionsDist(var ThreadId: String, var LowerRange: Int, var UpperRange: Int)
case class AdapterDist(var Name: String, var ReaderPatitions: ArrayBuffer[String])
case class ClusterDistributionMap(var Action: String, var LogicalPartitions: Int, var GlobalProcessThreads: Int, var GlobalReaderThreads: Int, var TotalReaderThreads: Int, var TotalProcessThreads: Int, var NodeDistributionMap: ArrayBuffer[NodeDistributionMap])

object Distribution {
  private val LOG = LogManager.getLogger(getClass);

  def createDistribution(clusterDistInfo: ClusterDistributionInfo, allPartitionUniqueRecordKeys: Array[(String, String)]): ClusterDistributionMap = {

    var logicalPartitions: Int = clusterDistInfo.LogicalPartitions
    var globalProcessingThreads: Int = clusterDistInfo.GlobalProcessThreads
    var globalReaderThreads: Int = clusterDistInfo.GlobalReaderThreads

    val participantsNodes = clusterDistInfo.NodesDist

    if (participantsNodes == null) throw new Exception("Participant Nodes is Null")
    if (allPartitionUniqueRecordKeys == null) throw new Exception("Partition Unique Record Keys is Null")
    if (logicalPartitions == 0) throw new Exception("The Logical Partitions given is 0")
    var nodesArray = ArrayBuffer[Node]()

    participantsNodes.foreach(n => {
      if (n.Nodeid == null || n.Nodeid.trim() == "") throw new Exception("Node Name should be provided")
      if ((n.ProcessThreads == null || n.ProcessThreads == 0) && (globalProcessingThreads == null || globalProcessingThreads == 0)) throw new Exception("Global Processing Threads or Node Process Threads should be provided")
      if ((n.ReaderThreads == null || n.ReaderThreads == 0) && (globalReaderThreads == null || globalReaderThreads == 0)) throw new Exception("Global Reader Threads or Node Reader Threads should be provided")

      var processThreads: Int = 0
      var readerThreads: Int = 0

      if (n.ProcessThreads == null || n.ProcessThreads == 0) processThreads = globalProcessingThreads else processThreads = n.ProcessThreads
      if (n.ReaderThreads == null || n.ReaderThreads == 0) readerThreads = globalReaderThreads else readerThreads = n.ReaderThreads
      var node = new Node(n.Nodeid, processThreads, readerThreads)
      nodesArray += node
    })

    val totalReaderThreads = computeTotalReaderThreads(nodesArray)
    var totalProcessingThreads = computeTotalProcessThreads(nodesArray)

    val nodedist = processDistribution(nodesArray, allPartitionUniqueRecordKeys, logicalPartitions, totalReaderThreads, totalProcessingThreads)

    val clusterDistributionMap = new ClusterDistributionMap("distribute", logicalPartitions, globalProcessingThreads, globalReaderThreads, totalReaderThreads, totalProcessingThreads, nodedist)

    /* val allPartsToValidate = scala.collection.mutable.Map[String, Set[String]]()
    allPartitionUniqueRecordKeys.foreach(key => {
      LOG.info(key._1)
      if (!allPartsToValidate.contains(key._1))
        allPartsToValidate(key._1) = Set(key._2)
      else {
        allPartsToValidate(key._1) = allPartsToValidate(key._1) + key._2
      }
    })
    var adapterMaxPartitions = scala.collection.mutable.Map[String, Int]()
    allPartsToValidate.foreach(p => { adapterMaxPartitions(p._1) = p._2.size })
    val distributeJson = createDistributionJson(adapterMaxPartitions, clusterDistributionMap)
*/
    clusterDistributionMap
  }

  private def processDistribution(participantsNodes: ArrayBuffer[Node], allPartitionUniqueRecordKeys: Array[(String, String)], logicalPartitions: Int, totalReaderThreads: Int, totalProcessingThreads: Int): ArrayBuffer[NodeDistributionMap] = { //scala.collection.mutable.Map[String, (ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])], scala.collection.mutable.Map[String, (Int, Int)])] = {

    LOG.info("totalReaderThreads" + totalReaderThreads)

    val nodePPartsDist = ComputePhysicalPartitions(allPartitionUniqueRecordKeys, participantsNodes, totalReaderThreads)
    nodePPartsDist.foreach(n => { LOG.info(n._1 + " " + n._2) })

    val nodeLogicalPartDist = ComputeLogicalPartitions(participantsNodes, logicalPartitions, totalProcessingThreads)

    LOG.info(nodeLogicalPartDist)

    val nodeDistMap = NodePhysicalLogicalDist(nodeLogicalPartDist, nodePPartsDist)
    LOG.info(nodeDistMap)
    return nodeDistMap
  }

  private def NodePhysicalLogicalDist(nodeLogicalPartDist: scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, (Int, Int)]], nodePPartsDist: scala.collection.mutable.Map[String, ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])]]): ArrayBuffer[NodeDistributionMap] = { //scala.collection.mutable.Map[String, (ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])], scala.collection.mutable.Map[String, (Int, Int)])] = {

    var nodeDist = ArrayBuffer[NodeDistributionMap]()
    var nodeDistMap = scala.collection.mutable.Map[String, (ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])], scala.collection.mutable.Map[String, (Int, Int)])]()
    nodeLogicalPartDist.foreach(node => {
      nodeDistMap(node._1) = (nodePPartsDist(node._1), node._2)
    })

    nodeDistMap.foreach(nDist => {
      var ppartDist = ArrayBuffer[PhysicalPartitionsDist]()

      nDist._2._1.foreach(physicalParts => {
        var pp = new PhysicalPartitionsDist(physicalParts._1, physicalParts._2)
        ppartDist += pp
      })
      var lpartDist = ArrayBuffer[LogicalPartitionsDist]()

      nDist._2._2.foreach(logicalParts => {
        var lp = new LogicalPartitionsDist(logicalParts._1, logicalParts._2._1, logicalParts._2._2)
        lpartDist += lp
      })

      var nd = new NodeDistributionMap(nDist._1, ppartDist, lpartDist)
      nodeDist += nd

    })

    nodeDist

  }

  private def ComputePhysicalPartitions(allPartitionUniqueRecordKeys: Array[(String, String)], participantsNodes: ArrayBuffer[Node], totalReaderThreads: Int): scala.collection.mutable.Map[String, ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])]] = {
    var distributionMap = scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, ArrayBuffer[String]]]()
    var nodePPartsDist = scala.collection.mutable.Map[String, ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])]]()
    var tmpDistMap = ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])]()
    for (i <- 0 until totalReaderThreads) {
      tmpDistMap += ((i.toString(), scala.collection.mutable.Map[String, ArrayBuffer[String]]()))
    }

    val totalParticipents: Int = totalReaderThreads
    if (allPartitionUniqueRecordKeys != null && allPartitionUniqueRecordKeys.size > 0) {
      //  LOG.debug("allPartitionUniqueRecordKeys: %d".format(allPartitionUniqueRecordKeys.size))
      var cntr: Int = 0
      allPartitionUniqueRecordKeys.foreach(k => {
        val af = tmpDistMap(cntr % totalParticipents)._2.getOrElse(k._1, null)
        if (af == null) {
          val af1 = new ArrayBuffer[String]
          af1 += (k._2)
          tmpDistMap(cntr % totalParticipents)._2(k._1) = af1
        } else {
          af += (k._2)
        }
        cntr += 1
      })
    }

    LOG.info(tmpDistMap.toList)
    tmpDistMap.foreach(tup => {
      distributionMap(tup._1) = tup._2
    })
    val nodeThreadMap = NodeThreadDistribution(participantsNodes, totalReaderThreads)
    nodePPartsDist = NodePhysicalPartsMap(nodeThreadMap, distributionMap)
    nodePPartsDist
  }

  private def NodeThreadDistribution(participantsNodes: ArrayBuffer[Node], totalReaderThreads: Int): ArrayBuffer[(String, ArrayBuffer[String])] = {
    var nodeThreadMap = ArrayBuffer[(String, ArrayBuffer[String])]()
    var limitreachedcount: Int = 0

    for (i <- 0 until participantsNodes.size) {
      val value = (participantsNodes(i).Name, ArrayBuffer[String]())
      nodeThreadMap += value
    }

    nodeThreadMap.foreach(f => LOG.info(f._1))

    for (t <- 0 until totalReaderThreads) {
      var nodeNum: Int = (t + limitreachedcount) % participantsNodes.size
      while (participantsNodes(nodeNum).ReaderThreads == nodeThreadMap(nodeNum)._2.size) {
        // if (participantsNodes(nodeNum)._3 == nodeThreadMap(nodeNum)._2.size)
        limitreachedcount = limitreachedcount + 1
        nodeNum = nodeNum + 1
        if (nodeNum >= participantsNodes.size) nodeNum = nodeNum - participantsNodes.size
      }
      var abuf = nodeThreadMap(nodeNum)._2
      abuf += t.toString
      nodeThreadMap(nodeNum) = (nodeThreadMap(nodeNum)._1, abuf)
    }
    nodeThreadMap.foreach(f => { LOG.info(f._1 + "  " + f._2) })
    nodeThreadMap
  }

  private def NodePhysicalPartsMap(nodeThreadMap: ArrayBuffer[(String, ArrayBuffer[String])], tmpDistMap: scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, ArrayBuffer[String]]]): scala.collection.mutable.Map[String, ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])]] = {
    var nodePPartsDist = scala.collection.mutable.Map[String, ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])]]()

    nodeThreadMap.foreach(node => {
      val nodemap = (node._1, ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])]())
      nodePPartsDist += nodemap
    })

    nodeThreadMap.foreach(node => {
      var tmp = ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])]()
      node._2.foreach { t =>
        {
          val tmap = (t, tmpDistMap.getOrElse(t, null))
          tmp += tmap
        }
        nodePPartsDist(node._1) = tmp
      }
    })

    nodePPartsDist
  }

  private def ComputeLogicalPartitions(participantsNodes: ArrayBuffer[Node], logicalPartitions: Int, totalProcessingThreads: Int): scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, (Int, Int)]] = {
    val logicalthreads = ThreadLogicalPartsDist(totalProcessingThreads, logicalPartitions)
    logicalthreads.foreach(f => LOG.info(f._1 + " " + f._2._1 + " " + f._2._2))

    val nodeThreadMap = NodeLogicalThreadDistribution(participantsNodes, totalProcessingThreads)
    nodeThreadMap.foreach(f => { LOG.info(f._1 + "  " + f._2) })
    val nodeLogicalPartDist = NodeLogicalPartsMap(nodeThreadMap, logicalthreads)
    nodeLogicalPartDist
  }

  private def NodeLogicalPartsMap(nodeThreadMap: ArrayBuffer[(String, ArrayBuffer[String])], logicalthreads: scala.collection.mutable.Map[String, (Int, Int)]): scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, (Int, Int)]] = {
    var nodeLogicalPartMap = scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, (Int, Int)]]()
    nodeThreadMap.foreach(n => {
      val nmap = (n._1, scala.collection.mutable.Map[String, (Int, Int)]())
      nodeLogicalPartMap += nmap
    })
    nodeThreadMap.foreach(node => {
      var tmp = scala.collection.mutable.Map[String, (Int, Int)]()
      node._2.foreach { thread =>
        {
          val tmap = logicalthreads.getOrElse(thread, null)
          tmp(thread) = tmap
        }
      }
      nodeLogicalPartMap(node._1) = tmp
    })
    nodeLogicalPartMap

  }

  private def NodeLogicalThreadDistribution(participantsNodes: ArrayBuffer[Node], totalReaderThreads: Int): ArrayBuffer[(String, ArrayBuffer[String])] = {
    var nodeThreadMap = ArrayBuffer[(String, ArrayBuffer[String])]()
    var limitreachedcount: Int = 0

    for (i <- 0 until participantsNodes.size) {
      val value = (participantsNodes(i).Name, ArrayBuffer[String]())
      nodeThreadMap += value
    }

    nodeThreadMap.foreach(f => LOG.info(f._1))

    for (t <- 0 until totalReaderThreads) {
      var nodeNum: Int = (t + limitreachedcount) % participantsNodes.size
      if (nodeNum >= participantsNodes.size) nodeNum = nodeNum - participantsNodes.size
      while (participantsNodes(nodeNum).ProcessThreads == nodeThreadMap(nodeNum)._2.size) {
        nodeNum = nodeNum + 1
        limitreachedcount = limitreachedcount + 1
        if (nodeNum >= participantsNodes.size) nodeNum = nodeNum - participantsNodes.size
      }
      var abuf = nodeThreadMap(nodeNum)._2
      abuf += t.toString
      nodeThreadMap(nodeNum) = (nodeThreadMap(nodeNum)._1, abuf)
    }
    nodeThreadMap
  }

  private def computeTotalReaderThreads(nodes: ArrayBuffer[Node]): Int = {
    var totalReadThreads: Int = 0
    nodes.foreach { node => totalReadThreads = totalReadThreads + node.ReaderThreads }
    return totalReadThreads
  }

  private def computeTotalProcessThreads(participantsNodes: ArrayBuffer[Node]): Int = {
    var totalProcessThreads: Int = 0
    participantsNodes.foreach(node => {
      totalProcessThreads = totalProcessThreads + node.ProcessThreads
    })
    return totalProcessThreads;
  }

  private def computeMaxPhysicalPartitions(adapterMaxPartitions: scala.collection.mutable.Map[String, Int]): Int = {
    var totalPartitions: Int = 0
    if ((adapterMaxPartitions == null) || (adapterMaxPartitions.empty == true)) throw new Exception("Adapter details do not exist to compute max read parittions");
    adapterMaxPartitions.foreach(a => { if (a._2 >= 0) totalPartitions = totalPartitions + a._2 })
    return totalPartitions;
  }

  private def computeMaxProcessThreads(adapterMaxPartitions: scala.collection.mutable.Map[String, Int]): Int = {
    var totalPartitions: Int = 0
    if ((adapterMaxPartitions == null) || (adapterMaxPartitions.empty == true)) throw new Exception("Adapter details do not exist to compute max read parittions");
    adapterMaxPartitions.foreach(a => { if (a._2 >= 0) totalPartitions = totalPartitions + a._2 })
    return totalPartitions;
  }
  //

  private def allocatePartsToThreads(totalThreads: Int, totalPartitions: Int): scala.collection.mutable.Map[String, ArrayBuffer[String]] = {
    var PartsToThreads = scala.collection.mutable.Map[String, ArrayBuffer[String]]()
    for (t <- 0 until totalThreads) {
      var partitions = ArrayBuffer[String]()
      for (p <- 0 until totalPartitions) {
        val parts = (p + totalThreads) % totalThreads
        if (t == parts) {
          partitions += p.toString().trim()
        }
      }
      PartsToThreads(t.toString()) = partitions

    }
    PartsToThreads
  }

  def createDistributionJson(adapterMaxPartitions: scala.collection.mutable.Map[String, Int], clusterDistributionMap: ClusterDistributionMap): String = {
    if (clusterDistributionMap == null) throw new Exception("The Cluster Distribution Map is null")

    val distributionMap: ArrayBuffer[NodeDistributionMap] = clusterDistributionMap.NodeDistributionMap
    if (distributionMap == null) throw new Exception("The Node Distribution Map ArrayBuffer is null")

    val json = ("action" -> clusterDistributionMap.Action) ~
      ("LogicalPartitions" -> clusterDistributionMap.LogicalPartitions) ~
      ("GlobalProcessingThreads" -> clusterDistributionMap.GlobalProcessThreads) ~
      ("GlobalReaderThreads" -> clusterDistributionMap.GlobalReaderThreads) ~
      ("TotalProcessThreads" -> clusterDistributionMap.TotalProcessThreads) ~
      ("TotalReadThreads" -> clusterDistributionMap.TotalReaderThreads) ~
      ("adaptermaxpartitions" -> adapterMaxPartitions.map(kv =>
        ("Adap" -> kv._1) ~
          ("MaxParts" -> kv._2))) ~
      ("distributionmap" -> distributionMap.map(kv =>
        ("Node" -> kv.Node) ~
          ("PhysicalPartitions" -> kv.PhysicalPartitions.map(t =>
            ("ThreadId" -> t.ThreadId) ~
              ("Adaps" -> t.AdapterPartitions.map(a =>
                ("Adap" -> a._1) ~
                  ("ReadPartitions" -> a._2.toList))))) ~
          ("LogicalPartitions" -> kv.LogicalPartitionsDist.map(lp =>
            ("ThreadId" -> lp.ThreadId) ~
              ("Range" -> (lp.LowerRange + "," + lp.UpperRange))))))

    var outputJson: String = compact(render(json))
    LOG.info(outputJson)
    outputJson
  }

  private def ThreadLogicalPartsDist(totalProcessingThreads: Int, logicalPartitions: Int): scala.collection.mutable.Map[String, (Int, Int)] = {
    var lowerlimit: Int = 0
    var upperlimit: Int = 0
    var logicalthreads = scala.collection.mutable.Map[String, (Int, Int)]()
    val logicalPartRange = logicalPartitions / totalProcessingThreads
    val extraLogicalParts: Int = logicalPartitions - (logicalPartRange * totalProcessingThreads)
    var extra: Int = extraLogicalParts

    for (t <- 0 until totalProcessingThreads) {
      if (t == 0)
        lowerlimit = 0
      else lowerlimit = upperlimit + 1
      if (extra == 0) {
        upperlimit = lowerlimit + logicalPartRange - 1
      } else if (extra > 0) {
        upperlimit = lowerlimit + logicalPartRange
        extra = extra - 1
      }
      val lt = (lowerlimit, upperlimit)
      logicalthreads(t.toString()) = lt
    }

    logicalthreads
  }

}






