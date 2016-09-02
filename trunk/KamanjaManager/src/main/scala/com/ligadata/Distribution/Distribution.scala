package com.ligadata.Distribution

import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.Json
import org.json4s.jackson.JsonMethods._

case class Node(var Name: String, var Adapter: AdapterMap)
case class DistributionMap(Node: String, Adaps: List[AdapterMap])
case class AdapterMap(var Name: String, var ProcessThreads: Int, var ReadThreads: Int, var LogicalPartitions: Int, var PhysicalThreads: ArrayBuffer[PhysicalPartitions], var LogicalThreads: ArrayBuffer[LogicalPartitions], var limitReached: Boolean, var ReaderThreadsStrList: scala.collection.mutable.Map[String, ArrayBuffer[String]])
case class PhysicalPartitions(var ThreadId: String, ReadPartitions: Array[String])
case class LogicalPartitions(var ThreadId: String, var LowerRange: Int, var UpperRange: Int)
case class Distribute(var LogicalPartitions: Int, var GlobalProcessThreadsPerNode: Int, var GlobalReadThreadsPerNode: Int, var TotalProcessThreads: Int, var TotalReadThreads: Int)

class Distribution {

  private def AdapterMapDistribution(nodes: Array[String], adapters: Array[AdapterMap], adapterMaxPartitions: scala.collection.mutable.Map[String, Int]): scala.collection.mutable.Map[String, AdapterMap] = {
    var distibutionMap = scala.collection.mutable.Map[String, AdapterMap]()
    var adapterList = List[AdapterMap]()
    val totalPartitions = computeMaxPhysicalPartitions(adapterMaxPartitions)
    //println("1 totalPartitions " + totalPartitions)
    val totalProcessingThreads = TotalProcessingThreads(adapters, nodes.size)
    //println("2 totalProcessingThreads " + totalProcessingThreads)
    var adapinfo = scala.collection.mutable.Map[String, AdapterMap]()
    for (i <- 0 until adapters.length) {
      var amap: scala.collection.mutable.Map[String, AdapterMap] = ComputePhysicalPartitions(nodes, adapters(i), totalPartitions)

      amap.foreach(f => {
        var readThreadMap = scala.collection.mutable.Map[String, ArrayBuffer[String]]()
        if (readThreadMap != null) readThreadMap.clear()
        for (i <- 0 until f._2.PhysicalThreads.length) {
          var readThrdBuf = ArrayBuffer[String]()
          val pt = f._2.PhysicalThreads(i).ReadPartitions
          for (j <- 0 until pt.length) {
            var str = "{\"Version\":1,\"Type\":\"Kafka\",\"Name\":\"%s\",\"TopicName\":\"%s\",\"PartitionId\":%s}".format(f._2.Name, f._2.Name, pt(j))
            readThrdBuf += str
          }
          readThreadMap(f._2.PhysicalThreads(i).ThreadId) = readThrdBuf
        }
        f._2.ReaderThreadsStrList = readThreadMap
      })
      //println("amap1 " + amap)

      adapinfo = ComputeLogicalPartitionRanges(totalProcessingThreads, adapters(i).LogicalPartitions, nodes, adapters(i), amap)

      /* println("adapterInfo  " + adapinfo.size)
      adapinfo.foreach(f => {
         for (i <- 0 until f._2.PhysicalThreads.length) {
         val pt = f._2.PhysicalThreads(i).ReadPartitions
          for (j <- 0 until pt.length)
            print(" " + pt(j))
          println()
        }
        println("===LP====")
        for (j <- 0 until f._2.LogicalThreads.length) {
          println("  " + f._2.LogicalThreads(j).ThreadId + " " + f._2.LogicalThreads(j).LowerRange + " " + f._2.LogicalThreads(j).UpperRange)
        }
      })*/

    }
    adapinfo
  }

  private def TotalProcessingThreads(adapters: Array[AdapterMap], nodeSize: Int): Int = {
    var totalProcessingThreads: Int = 0
    if (adapters == null) throw new Exception("Nodes is null");
    for (i <- 0 until adapters.length) {
      totalProcessingThreads = totalProcessingThreads + adapters(i).ProcessThreads
    }
    return totalProcessingThreads * nodeSize;
  }

  private def ComputeLogicalPartitionRanges(totalProcessingThreads: Int, logicalPartitions: Int, nodes: Array[String], adapterMap: AdapterMap, logicalparts: scala.collection.mutable.Map[String, AdapterMap]): scala.collection.mutable.Map[String, AdapterMap] = {

    if (totalProcessingThreads.equals(null) || totalProcessingThreads == 0 || logicalPartitions.equals(null) || logicalPartitions == 0) throw new Exception("Check either totalProcessingthreads or logical patitions")

    val logicalPartRange = logicalPartitions / totalProcessingThreads
    //println("logicalPartRange  " + logicalPartRange)
    val extraLogicalParts: Int = logicalPartitions - (logicalPartRange * totalProcessingThreads)
    //println("extraLogicalParts  " + extraLogicalParts)
    var extra: Int = extraLogicalParts
    var j = 0
    var logicalthreads: ArrayBuffer[LogicalPartitions] = ArrayBuffer[LogicalPartitions]()
    var limitreachedcount: Int = 0
    var lowerlimit: Int = 0
    var upperlimit: Int = 0

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

      //println("lowLimit:" + lowerlimit + " upperLimit:" + upperlimit)
      val lthread = new LogicalPartitions(t.toString(), lowerlimit, upperlimit)
      logicalthreads += lthread
    }
    //println("logicalthreads " + logicalthreads.size)
    //println("logicalthreads" + logicalthreads.toList)
    var indexcount: Array[Int] = Array.fill[Int](nodes.size)(0) //Array[Int](nodes.size)
    //println("size" + indexcount.length)
    for (t <- 0 until logicalthreads.size) {
      //println("thread value: " + t)
      breakable {
        var nodeNum: Int = (t + nodes.size) % nodes.size + limitreachedcount
        if (nodeNum >= nodes.size)
          nodeNum = nodeNum % nodes.size
        //println("nodeNum: " + nodeNum)

        for (n <- 0 until nodes.size) {
          //println("node value: " + n)
          if (nodeNum == n) {
            if (logicalparts(nodes(n)).LogicalThreads.size == null) logicalparts(nodes(n)).LogicalThreads = ArrayBuffer[LogicalPartitions]()
            if (logicalparts(nodes(n)).limitReached == false) {
              //println("Node" + n + "  " + logicalthreads(t).ThreadId + "  " + logicalthreads(t).LowerRange + " " + logicalthreads(t).UpperRange + "   " + indexcount(n) + "   " + logicalparts(nodes(n)).LogicalThreads.size)

              if (indexcount(n) >= logicalparts(nodes(n)).LogicalThreads.size) { //if (indexcount(n) >= nodes(n).LogicalThreads.size) {
                logicalparts(nodes(n)).LogicalThreads = logicalparts(nodes(n)).LogicalThreads :+ logicalthreads(t)

                if (logicalparts(nodes(n)).LogicalThreads.size == logicalparts(nodes(n)).ProcessThreads)
                  logicalparts(nodes(n)).limitReached = true
                indexcount(n) = indexcount(n) + 1
                break;
              } else break;

            } else {
              if (n + 1 < nodes.size) {
                var it = n + 1
                if (logicalparts(nodes(it)).LogicalThreads.size == null) logicalparts(nodes(n)).LogicalThreads = ArrayBuffer[LogicalPartitions]()
                do {
                  if (it > nodes.size) it = it - nodes.size
                  if (logicalparts(nodes(it)).limitReached == true) { it = it + 1; limitreachedcount = limitreachedcount + 1; }

                } while (logicalparts(nodes(it)).limitReached == false)

                if ((logicalparts(nodes(it)).limitReached == false)) {
                  //println("*Node" + it + "  " + logicalthreads(t).ThreadId + "  " + logicalthreads(t).LowerRange + " " + logicalthreads(t).UpperRange + "   " + indexcount(it) + "   " + logicalparts(nodes(it)).LogicalThreads.size)
                  logicalparts(nodes(it)).LogicalThreads = logicalparts(nodes(it)).LogicalThreads :+ logicalthreads(t)
                  indexcount(it) = indexcount(it) + 1
                  limitreachedcount = limitreachedcount + 1
                  if (logicalparts(nodes(it)).LogicalThreads.size == logicalparts(nodes(it)).ProcessThreads)
                    logicalparts(nodes(it)).limitReached = true
                  break
                }
              } else {
                val it = n + 1 - nodes.size
                if (logicalparts(nodes(it)).LogicalThreads.size == null) logicalparts(nodes(n)).LogicalThreads = ArrayBuffer[LogicalPartitions]()

                //println("^Node" + it + "  " + logicalthreads(t).ThreadId + "  " + logicalthreads(t).LowerRange + " " + logicalthreads(t).UpperRange + "   " + indexcount(it) + "   " + logicalparts(nodes(it)).LogicalThreads.size)
                logicalparts(nodes(it)).LogicalThreads = logicalparts(nodes(it)).LogicalThreads :+ logicalthreads(t)
                limitreachedcount = 1
                indexcount(it) = indexcount(it) + 1
                if (logicalparts(nodes(it)).LogicalThreads.size == logicalparts(nodes(it)).ProcessThreads)
                  logicalparts(nodes(it)).limitReached = true
                break
              }
            }
          }
        }
      }
    }

    logicalparts
  }

  private def computeTotalReadThreads(nodes: Array[String], adapterMap: AdapterMap): Int = {
    var totalReadThreads: Int = 0
    //  for (n <- 0 until nodes.size) {
    totalReadThreads = totalReadThreads + adapterMap.ReadThreads

    // }
    return totalReadThreads * nodes.size
  }

  private def computeMaxPhysicalPartitions(adapterMaxPartitions: scala.collection.mutable.Map[String, Int]): Int = {
    var totalPartitions: Int = 0
    if ((adapterMaxPartitions == null) || (adapterMaxPartitions.empty == true)) throw new Exception("Adapter details do not exist to compute max read parittions");
    adapterMaxPartitions.foreach(a => { if (a._2 >= 0) totalPartitions = totalPartitions + a._2 })
    return totalPartitions;
  }
  //

  private def ComputePhysicalPartitions(nodes: Array[String], adapterMap: AdapterMap, totalPartitions: Int): scala.collection.mutable.Map[String, AdapterMap] = {
    val totalReadThreads = computeTotalReadThreads(nodes, adapterMap)
    //val totalReadThreads = adapterMap.ReadThreads
    //println("1 totalReadThreads " + totalReadThreads)
    // println("totalPartitions " + totalPartitions)
    if (totalReadThreads == 0) throw new Exception("Check Read Threads in adapters")
    if (totalPartitions == 0) throw new Exception("Check max Partitions in adapters")
    val PartsToThreads = allocatePartsToThreads(totalReadThreads, totalPartitions)
    //PartsToThreads.foreach(p => { println("^^^" + p._1 + "  " + p._2.toList) })
    val ThreadsToNodes = allocateThreadsToNodes(nodes, adapterMap, PartsToThreads)
    ThreadsToNodes
  }

  private def allocatePartsToThreads(totalThreads: Int, totalPartitions: Int): scala.collection.mutable.Map[String, ArrayBuffer[String]] = {
    var PartsToThreads = scala.collection.mutable.Map[String, ArrayBuffer[String]]()
    for (t <- 0 until totalThreads) {
      var partitions = ArrayBuffer[String]()
      for (p <- 0 until totalPartitions) {
        val parts = (p + totalThreads) % totalThreads
        if (t == parts) {
          //  println("=========" + parts)
          partitions += p.toString().trim()
          //   println("******************" + partitions.toList)
        }
      }
      PartsToThreads(t.toString()) = partitions

    }
    PartsToThreads
  }

  private def allocateThreadsToNodes(nodes: Array[String], adapterMap: AdapterMap, partsToThreads: scala.collection.mutable.Map[String, ArrayBuffer[String]]): scala.collection.mutable.Map[String, AdapterMap] = {
    // var physicalPartitions = scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, ArrayBuffer[Int]]]()
    var physicalPartitions = scala.collection.mutable.Map[String, AdapterMap]()
    //println(" nodes.size " + nodes.size);
    //println(" partsToThreads.size " + partsToThreads.size);
    for (n <- 0 until nodes.size) {
      val pparraybuf = ArrayBuffer[PhysicalPartitions]()
      for (t <- 0 until partsToThreads.size) {
        val nodeNum = (t + nodes.size) % nodes.size
        //println("nodeNum " + nodeNum + "- n   " + n + "- T   " + t);
        if (nodeNum == n) {
          //println(" n " + n);
          pparraybuf += new PhysicalPartitions(t.toString(), partsToThreads(t.toString()).toArray)
        }
      }
      //println("nodes(n)" + nodes(n))
      physicalPartitions(nodes(n)) = new AdapterMap(adapterMap.Name, adapterMap.ProcessThreads, adapterMap.ReadThreads, adapterMap.LogicalPartitions, pparraybuf, adapterMap.LogicalThreads, adapterMap.limitReached, null)
    }
    //physicalPartitions.foreach(f => { print("Node!!!:  " + f._1 + " " + f._2.PhysicalThreads.size); f._2.PhysicalThreads.foreach(p => { println("Thread" + p.ThreadId + " : " + p.ReadPartitions.toList) }) })
    physicalPartitions
  }

  private def createDistributionJson(adapterMaxPartitions: scala.collection.mutable.Map[String, Int], distributionMap: scala.collection.mutable.Map[String, AdapterMap], dist: Distribute): Unit = {

    val json = ("action" -> "distribute") ~
      ("LogicalPartitions" -> dist.LogicalPartitions) ~
      ("GlobalProcessingThreads" -> dist.GlobalProcessThreadsPerNode.toString()) ~
      ("TotalProcessThreads" -> dist.TotalProcessThreads.toString()) ~
      ("TotalReadThreads" -> dist.TotalReadThreads.toString()) ~
      ("adaptermaxpartitions" -> adapterMaxPartitions.map(kv =>
        ("Adap" -> kv._1) ~
          ("MaxParts" -> kv._2))) ~
      ("distributionmap" -> distributionMap.map(kv =>
        ("Node" -> kv._1) ~
          //("Adaps" -> kv._2.map(kv1 => ("Adap" -> kv1.Name) ~
          ("Adaps" -> ("Adap" -> kv._2.Name) ~
            ("ReadThreads" -> kv._2.ReadThreads) ~
            ("ProcessThreads" -> kv._2.ProcessThreads) ~
            ("Parts" ->
              ("PhysicalPartitions" -> kv._2.ReaderThreadsStrList.map(pp => { ("ThreadId" -> pp._1) ~ ("ReadPartitions" -> pp._2) })) ~
              ("LogicalPartitions" -> kv._2.LogicalThreads.toList.map(lp => { ("ThreadId" -> lp.ThreadId) ~ ("Range" -> (lp.LowerRange.toString() + "," + lp.UpperRange.toString())) }))))))

    var outputJson = compact(render(json))
    println(outputJson)
  }
}

object Distribution {

  def main(args: Array[String]) = {

    val nodesCount = 2
    val globarProcessThreads = 10
    val GlobalReadThreads = 2
    val processThreads = 10
    val readThreads = 2

    var adapter1: AdapterMap = AdapterMap("testing_1", 8, 2, 8192, null, ArrayBuffer[LogicalPartitions](), false, null)
    //  var adapter2: AdapterMap = AdapterMap("finance", 8, 2, 8192, null, ArrayBuffer[LogicalPartitions](), false)
    //  var adapter3: AdapterMap = AdapterMap("medical", 10, 1, 8192, null, ArrayBuffer[LogicalPartitions](), false)

    var distribution = new Distribution
    //computelogical threads 
    var nodes = Array[String]("1", "2")
    var adapters: Array[AdapterMap] = Array[AdapterMap](adapter1)

    var adapterMaxPartitions = scala.collection.mutable.Map[String, Int](("{\"Name\" : \"testin_1\"}", 10))
    var distributionMap = distribution.AdapterMapDistribution(nodes, adapters, adapterMaxPartitions)
    var dist = new Distribute(8192, 8, 2, 10, 10)
    distribution.createDistributionJson(adapterMaxPartitions, distributionMap, dist)

  }

  /*for (n <- 0 until nodes.size) {
          println("node value: " + n)
          if (nodeNum == n) {
            //if ((nodes(n).limitReached == false)) {
             if (adapterMap.LogicalThreads.size == adapterMap.ProcessThreads)
              println("Node" + n + "  " + logicalthreads(t).ThreadId + "  " + logicalthreads(t).LowerRange + " " + logicalthreads(t).UpperRange + "   " + indexcount(n) + "   " + nodes(n).LogicalThreads.size)
              if (indexcount(n) >= nodes(n).LogicalThreads.size) {
                nodes(n).LogicalThreads = nodes(n).LogicalThreads :+ logicalthreads(t)
                if (nodes(n).LogicalThreads.size == nodes(n).ProcessThreads)
                  nodes(n).limitReached = true
                indexcount(n) = indexcount(n) + 1
                break;
              } else break;

            } else {
              if (n + 1 < nodes.size) {
                var it = n + 1
                if (nodes(it).limitReached == true) { it = it + 1; limitreachedcount = limitreachedcount + 1; }
                if ((nodes(it).limitReached == false)) {
                  println("*Node" + it + "  " + logicalthreads(t).ThreadId + "  " + logicalthreads(t).LowerRange + " " + logicalthreads(t).UpperRange + "   " + indexcount(it) + "   " + nodes(n).LogicalThreads.size)
                  nodes(it).LogicalThreads = nodes(it).LogicalThreads :+ logicalthreads(t)
                  indexcount(it) = indexcount(it) + 1
                  limitreachedcount = limitreachedcount + 1
                  if (nodes(it).LogicalThreads.size == nodes(it).ProcessThreads)
                    nodes(it).limitReached = true
                  break
                }
              } else {
                val it = n + 1 - nodes.size
                println("^Node" + it + "  " + logicalthreads(t).ThreadId + "  " + logicalthreads(t).LowerRange + " " + logicalthreads(t).UpperRange + "   " + indexcount(it) + "   " + nodes(n).LogicalThreads.size)
                nodes(it).LogicalThreads = nodes(it).LogicalThreads :+ logicalthreads(t)
                limitreachedcount = 1
                indexcount(it) = indexcount(it) + 1
                if (nodes(it).LogicalThreads.size == nodes(it).ProcessThreads)
                  nodes(it).limitReached = true
                break
              }
            }
          }
        }
*/

}





