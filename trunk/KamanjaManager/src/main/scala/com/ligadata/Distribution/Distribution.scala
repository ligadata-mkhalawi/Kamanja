package com.ligadata.Distribution

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.Json
import org.json4s.jackson.JsonMethods._
import org.apache.logging.log4j.{ LogManager, Logger }
import com.ligadata.KamanjaManager.NodeDistInfo
import com.ligadata.KamanjaManager.ClusterDistributionInfo

case class AdapMaxPartitions(Adap: String, MaxParts: Int)
case class Node(var Name: String, var ProcessThreads: Int, var ReaderThreads: Int)
case class NodeDistributionMap(Node: String, PhysicalPartitions: List[PhysicalPartitionsDist], LogicalPartitionsDist: List[LogicalPartitionsDist])
case class PhysicalPartitionsDist(var ThreadId: Short, Adaps: scala.collection.mutable.Map[String, ArrayBuffer[String]])
case class LogicalPartitionsDist(var ThreadId: Short, var LowerRange: Int, var UpperRange: Int)
case class AdapterDist(var Name: String, var ReaderPatitions: ArrayBuffer[String])
case class ClusterDistributionMap(var action: String, var GlobalProcessThreads: Int, var GlobalReaderThreads: Int, var LogicalPartitions: Int, var TotalReaderThreads: Int, var TotalProcessThreads: Int, AdapterMaxPartitions: Option[List[AdapMaxPartitions]], var DistributionMap: List[NodeDistributionMap])

object Distribution {
  private val LOG = LogManager.getLogger(getClass);

  def createDistribution(clusterDistInfo: ClusterDistributionInfo, allPartitionUniqueRecordKeys: Array[(String, String)]): ClusterDistributionMap = {
    var clusterDistributionMap: ClusterDistributionMap = null

    if (clusterDistInfo == null) throw new Exception("ClusterDistributionInfo is null")
    if (clusterDistInfo.LogicalPartitions == 0 || clusterDistInfo.GlobalProcessThreads == 0 || clusterDistInfo.GlobalReaderThreads == 0) throw new Exception("ClusterDistributionInfo LogicalPartitions or GlobalProcessThreads or GlobalReaderThreads is 0")

    if (allPartitionUniqueRecordKeys == null || (allPartitionUniqueRecordKeys != null && allPartitionUniqueRecordKeys.size == 0)) throw new Exception("Partition Unique Record Keys is Null or records size is 0")

    try {
      var logicalPartitions: Int = clusterDistInfo.LogicalPartitions
      var globalProcessingThreads: Int = clusterDistInfo.GlobalProcessThreads
      var globalReaderThreads: Int = clusterDistInfo.GlobalReaderThreads

      val participantsNodes = clusterDistInfo.NodesDist
      if (participantsNodes == null || participantsNodes.size == 0) throw new Exception("Participant Nodes is Null")
      var nodesArray = ArrayBuffer[Node]()

      participantsNodes.foreach(n => {
        if (n.Nodeid == null || n.Nodeid.trim() == "") throw new Exception("Node Name should be provided")
        // if ((n.ProcessThreads == null || n.ProcessThreads == 0) && (globalProcessingThreads == null || globalProcessingThreads == 0)) throw new Exception("Global Processing Threads or Node Process Threads should be provided")
        // if ((n.ReaderThreads == null || n.ReaderThreads == 0) && (globalReaderThreads == null || globalReaderThreads == 0)) throw new Exception("Global Reader Threads or Node Reader Threads should be provided")

        var processThreads: Int = 0
        var readerThreads: Int = 0

        if (n.ProcessThreads == null || n.ProcessThreads == 0) processThreads = globalProcessingThreads else processThreads = n.ProcessThreads
        if (n.ReaderThreads == null || n.ReaderThreads == 0) readerThreads = globalReaderThreads else readerThreads = n.ReaderThreads
        var node = new Node(n.Nodeid, processThreads, readerThreads)
        nodesArray += node
      })

      val totalReaderThreads = computeTotalReaderThreads(nodesArray)
      var totalProcessingThreads = computeTotalProcessThreads(nodesArray)

      if (totalReaderThreads == 0) throw new Exception("Total Reader Threads is 0, please check")
      if (totalProcessingThreads == 0) throw new Exception("Total Processing Threads is 0, please check")

      val nodedist = processDistribution(nodesArray, allPartitionUniqueRecordKeys, logicalPartitions, totalReaderThreads, totalProcessingThreads)
      val adapterMaxPartitions = getAadapterMaxPartitions(allPartitionUniqueRecordKeys)

      clusterDistributionMap = new ClusterDistributionMap("distribute", globalProcessingThreads, globalReaderThreads, logicalPartitions, totalReaderThreads, totalProcessingThreads, adapterMaxPartitions, nodedist.toList)

    } catch {
      case e: Exception => {
        LOG.debug("", e)
      }
    }
    clusterDistributionMap
  }

  private def processDistribution(participantsNodes: ArrayBuffer[Node], allPartitionUniqueRecordKeys: Array[(String, String)], logicalPartitions: Int, totalReaderThreads: Int, totalProcessingThreads: Int): ArrayBuffer[NodeDistributionMap] = { //scala.collection.mutable.Map[String, (ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])], scala.collection.mutable.Map[String, (Int, Int)])] = {

    LOG.debug("totalReaderThreads" + totalReaderThreads)

    val nodePPartsDist = ComputePhysicalPartitions(allPartitionUniqueRecordKeys, participantsNodes, totalReaderThreads)
    if (nodePPartsDist != null && nodePPartsDist.size > 0) nodePPartsDist.foreach(n => { LOG.debug(n._1 + " " + n._2) })

    val nodeLogicalPartDist = ComputeLogicalPartitions(participantsNodes, logicalPartitions, totalProcessingThreads)

    LOG.debug("nodeLogicalPartDist " + nodeLogicalPartDist)

    val nodeDistMap = NodePhysicalLogicalDist(nodeLogicalPartDist, nodePPartsDist)
    LOG.debug("nodeDistMap "+nodeDistMap)
    return nodeDistMap
  }

  private def getAadapterMaxPartitions(allPartitionUniqueRecordKeys: Array[(String, String)]): Option[List[AdapMaxPartitions]] = {
    var adapterMaxPartitions = ListBuffer[AdapMaxPartitions]()
    val allPartsToValidate = scala.collection.mutable.Map[String, Set[String]]()

    try {
      if (allPartitionUniqueRecordKeys != null) {
        allPartitionUniqueRecordKeys.foreach(key => {
          LOG.info(key._1)
          if (!allPartsToValidate.contains(key._1))
            allPartsToValidate(key._1) = Set(key._2)
          else {
            allPartsToValidate(key._1) = allPartsToValidate(key._1) + key._2
          }
        })
        allPartsToValidate.foreach(p => {
          val adapPart = new AdapMaxPartitions(p._1, p._2.size)
          adapterMaxPartitions += adapPart
        })
      }
      LOG.debug("adapterMaxPartitions " + adapterMaxPartitions)
    } catch {
      case e: Exception => {
        LOG.debug("", e)
      }
    }
    Some(adapterMaxPartitions.toList)
  }

  private def NodePhysicalLogicalDist(nodeLogicalPartDist: scala.collection.mutable.Map[String, scala.collection.mutable.Map[Int, (Int, Int)]], nodePPartsDist: scala.collection.mutable.Map[String, ArrayBuffer[(Short, scala.collection.mutable.Map[String, ArrayBuffer[String]])]]): ArrayBuffer[NodeDistributionMap] = { //scala.collection.mutable.Map[String, (ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])], scala.collection.mutable.Map[String, (Int, Int)])] = {

    var nodeDist = ArrayBuffer[NodeDistributionMap]()
    var nodeDistMap = scala.collection.mutable.Map[String, (ArrayBuffer[(Short, scala.collection.mutable.Map[String, ArrayBuffer[String]])], scala.collection.mutable.Map[Int, (Int, Int)])]()
    try {
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
          var lp = new LogicalPartitionsDist(logicalParts._1.toShort, logicalParts._2._1, logicalParts._2._2)
          lpartDist += lp
        })

        var nd = new NodeDistributionMap(nDist._1, ppartDist.toList, lpartDist.toList)
        nodeDist += nd

      })
    } catch {
      case e: Exception => {
        LOG.debug("", e)
      }
    }

    nodeDist

  }

  private def ComputePhysicalPartitions(allPartitionUniqueRecordKeys: Array[(String, String)], participantsNodes: ArrayBuffer[Node], totalReaderThreads: Int): scala.collection.mutable.Map[String, ArrayBuffer[(Short, scala.collection.mutable.Map[String, ArrayBuffer[String]])]] = {
    var distributionMap = scala.collection.mutable.Map[Short, scala.collection.mutable.Map[String, ArrayBuffer[String]]]()
    var nodePPartsDist = scala.collection.mutable.Map[String, ArrayBuffer[(Short, scala.collection.mutable.Map[String, ArrayBuffer[String]])]]()
    var tmpDistMap = ArrayBuffer[(Short, scala.collection.mutable.Map[String, ArrayBuffer[String]])]()
    try {
      for (i <- 0 until totalReaderThreads) {
        tmpDistMap += ((i.toShort, scala.collection.mutable.Map[String, ArrayBuffer[String]]()))
      }

      val totalParticipents: Int = totalReaderThreads
      if (allPartitionUniqueRecordKeys != null && allPartitionUniqueRecordKeys.size > 0) {
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

      LOG.debug(tmpDistMap.toList)
      tmpDistMap.foreach(tup => {
        distributionMap(tup._1) = tup._2
      })
      val nodeThreadMap = NodeThreadDistribution(participantsNodes, totalReaderThreads)
      nodePPartsDist = NodePhysicalPartsMap(nodeThreadMap, distributionMap)
    } catch {
      case e: Exception => {
        LOG.debug("", e)
      }
    }
    nodePPartsDist
  }

  private def NodeThreadDistribution(participantsNodes: ArrayBuffer[Node], totalReaderThreads: Int): ArrayBuffer[(String, ArrayBuffer[Short])] = {
    var nodeThreadMap = ArrayBuffer[(String, ArrayBuffer[Short])]()
    var limitreachedcount: Int = 0
    try {
      for (i <- 0 until participantsNodes.size) {
        val value = (participantsNodes(i).Name, ArrayBuffer[Short]())
        nodeThreadMap += value
      }

      nodeThreadMap.foreach(f => LOG.debug(f._1))

      for (t <- 0 until totalReaderThreads) {
        var nodeNum: Int = (t + limitreachedcount) % participantsNodes.size
        while (participantsNodes(nodeNum).ReaderThreads == nodeThreadMap(nodeNum)._2.size) {
          limitreachedcount = limitreachedcount + 1
          nodeNum = nodeNum + 1
          if (nodeNum >= participantsNodes.size) nodeNum = nodeNum - participantsNodes.size
        }
        var abuf = nodeThreadMap(nodeNum)._2
        abuf += t.toShort
        nodeThreadMap(nodeNum) = (nodeThreadMap(nodeNum)._1, abuf)
      }
      nodeThreadMap.foreach(f => { LOG.debug(f._1 + "  " + f._2) })
    } catch {
      case e: Exception => {
        LOG.debug("", e)
      }
    }
    nodeThreadMap
  }

  private def NodePhysicalPartsMap(nodeThreadMap: ArrayBuffer[(String, ArrayBuffer[Short])], tmpDistMap: scala.collection.mutable.Map[Short, scala.collection.mutable.Map[String, ArrayBuffer[String]]]): scala.collection.mutable.Map[String, ArrayBuffer[(Short, scala.collection.mutable.Map[String, ArrayBuffer[String]])]] = {
    var nodePPartsDist = scala.collection.mutable.Map[String, ArrayBuffer[(Short, scala.collection.mutable.Map[String, ArrayBuffer[String]])]]()
    if (nodeThreadMap == null || (nodeThreadMap != null && nodeThreadMap.size == 0)) throw new Exception("Node Thread Map is null or no data")

    try {
      nodeThreadMap.foreach(node => {
        val nodemap = (node._1, ArrayBuffer[(Short, scala.collection.mutable.Map[String, ArrayBuffer[String]])]())
        nodePPartsDist += nodemap
      })

      nodeThreadMap.foreach(node => {
        var tmp = ArrayBuffer[(Short, scala.collection.mutable.Map[String, ArrayBuffer[String]])]()
        node._2.foreach { t =>
          {
            val tmap = (t, tmpDistMap.getOrElse(t, null))
            tmp += tmap
          }
          nodePPartsDist(node._1) = tmp
        }
      })
    } catch {
      case e: Exception => {
        LOG.debug("", e)
      }
    }
    nodePPartsDist
  }

  private def ComputeLogicalPartitions(participantsNodes: ArrayBuffer[Node], logicalPartitions: Int, totalProcessingThreads: Int): scala.collection.mutable.Map[String, scala.collection.mutable.Map[Int, (Int, Int)]] = {
    var nodeLogicalPartDist = scala.collection.mutable.Map[String, scala.collection.mutable.Map[Int, (Int, Int)]]()
    if (logicalPartitions == 0) throw new Exception("logical paritions is 0")
    try {

      val logicalthreads = ThreadLogicalPartsDist(totalProcessingThreads, logicalPartitions)

      if (logicalthreads != null && logicalthreads.size > 0)
        logicalthreads.foreach(f => LOG.debug(f._1 + " " + f._2._1 + " " + f._2._2))

      val nodeThreadMap = NodeLogicalThreadDistribution(participantsNodes, totalProcessingThreads)
      if (nodeThreadMap != null && nodeThreadMap.size > 0) nodeThreadMap.foreach(f => { LOG.debug(f._1 + "  " + f._2) })
      nodeLogicalPartDist = NodeLogicalPartsMap(nodeThreadMap, logicalthreads)
    } catch {
      case e: Exception => {
        LOG.debug("", e)
      }
    }
    nodeLogicalPartDist
  }

  private def NodeLogicalPartsMap(nodeThreadMap: ArrayBuffer[(String, ArrayBuffer[Int])], logicalthreads: scala.collection.mutable.Map[Int, (Int, Int)]): scala.collection.mutable.Map[String, scala.collection.mutable.Map[Int, (Int, Int)]] = {
    var nodeLogicalPartMap = scala.collection.mutable.Map[String, scala.collection.mutable.Map[Int, (Int, Int)]]()
    if (nodeThreadMap == null || (nodeThreadMap != null && nodeThreadMap.size == 0)) throw new Exception("Node Thread Map is null or size is 0")
    if (logicalthreads == null || (logicalthreads != null && logicalthreads.size == 0)) throw new Exception("Logical Thread Map is null or size is 0")

    try {
      nodeThreadMap.foreach(n => {
        val nmap = (n._1, scala.collection.mutable.Map[Int, (Int, Int)]())
        nodeLogicalPartMap += nmap
      })
      nodeThreadMap.foreach(node => {
        var tmp = scala.collection.mutable.Map[Int, (Int, Int)]()
        node._2.foreach { thread =>
          {
            val tmap = logicalthreads.getOrElse(thread, null)
            tmp(thread) = tmap
          }
        }
        nodeLogicalPartMap(node._1) = tmp
      })
    } catch {
      case e: Exception => {
        LOG.debug("", e)
      }
    }
    nodeLogicalPartMap

  }

  private def NodeLogicalThreadDistribution(participantsNodes: ArrayBuffer[Node], totalReaderThreads: Int): ArrayBuffer[(String, ArrayBuffer[Int])] = {
    var nodeThreadMap = ArrayBuffer[(String, ArrayBuffer[Int])]()
    var limitreachedcount: Int = 0
    try {
      for (i <- 0 until participantsNodes.size) {
        val value = (participantsNodes(i).Name, ArrayBuffer[Int]())
        nodeThreadMap += value
      }

      nodeThreadMap.foreach(f => LOG.debug(f._1))

      for (t <- 0 until totalReaderThreads) {
        var nodeNum: Int = (t + limitreachedcount) % participantsNodes.size
        if (nodeNum >= participantsNodes.size) nodeNum = nodeNum - participantsNodes.size
        while (participantsNodes(nodeNum).ProcessThreads == nodeThreadMap(nodeNum)._2.size) {
          nodeNum = nodeNum + 1
          limitreachedcount = limitreachedcount + 1
          if (nodeNum >= participantsNodes.size) nodeNum = nodeNum - participantsNodes.size
        }
        var abuf = nodeThreadMap(nodeNum)._2
        abuf += t
        nodeThreadMap(nodeNum) = (nodeThreadMap(nodeNum)._1, abuf)
      }
    } catch {
      case e: Exception => {
        LOG.debug("", e)
      }
    }
    nodeThreadMap
  }

  private def computeTotalReaderThreads(participantsNodes: ArrayBuffer[Node]): Int = {
    var totalReadThreads: Int = 0
    if (participantsNodes == null || (participantsNodes != null && participantsNodes.size == 0)) throw new Exception("Node information is needed")
    participantsNodes.foreach { node => totalReadThreads = totalReadThreads + node.ReaderThreads }
    return totalReadThreads
  }

  private def computeTotalProcessThreads(participantsNodes: ArrayBuffer[Node]): Int = {
    var totalProcessThreads: Int = 0
    if (participantsNodes == null || (participantsNodes != null && participantsNodes.size == 0)) throw new Exception("Node information is needed")
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

  def createDistributionJson(clusterDistributionMap: ClusterDistributionMap): String = {
    if (clusterDistributionMap == null) throw new Exception("The Cluster Distribution Map is null")
    val adaptermaxpartitions = clusterDistributionMap.AdapterMaxPartitions.get
    val DistributionMap: List[NodeDistributionMap] = clusterDistributionMap.DistributionMap
    if (DistributionMap == null) throw new Exception("The Node Distribution Map ArrayBuffer is null")

    val json = ("action" -> clusterDistributionMap.action) ~
      ("globalprocessthreads" -> clusterDistributionMap.GlobalProcessThreads) ~
      ("globalreaderthreads" -> clusterDistributionMap.GlobalReaderThreads) ~
      ("logicalpartitions" -> clusterDistributionMap.LogicalPartitions) ~
      ("totalprocessthreads" -> clusterDistributionMap.TotalProcessThreads) ~
      ("totalreaderthreads" -> clusterDistributionMap.TotalReaderThreads) ~
      ("adaptermaxpartitions" -> adaptermaxpartitions.map(kv =>
        ("Adap" -> kv.Adap) ~
          ("MaxParts" -> kv.MaxParts))) ~
      ("distributionmap" -> DistributionMap.map(kv =>
        ("Node" -> kv.Node) ~
          ("PhysicalPartitions" -> kv.PhysicalPartitions.map(t =>
            ("ThreadId" -> t.ThreadId.toInt) ~
              ("Adaps" -> t.Adaps.map(a =>
                ("Adap" -> a._1) ~
                  ("ReadPartitions" -> a._2.toList))))) ~
          ("LogicalPartitions" -> kv.LogicalPartitionsDist.map(lp =>
            ("ThreadId" -> lp.ThreadId.toInt) ~
              ("SRange" -> lp.LowerRange) ~
              ("ERange" -> lp.UpperRange)))))

    var outputJson: String = compact(render(json))
    LOG.info(outputJson)
    outputJson
  }

  private def ThreadLogicalPartsDist(totalProcessingThreads: Int, logicalPartitions: Int): scala.collection.mutable.Map[Int, (Int, Int)] = {
    var lowerlimit: Int = 0
    var upperlimit: Int = 0
    var logicalthreads = scala.collection.mutable.Map[Int, (Int, Int)]()
    try {
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
        logicalthreads(t) = lt
      }
    } catch {
      case e: Exception => {
        LOG.debug("", e)
      }
    }

    logicalthreads
  }

}






