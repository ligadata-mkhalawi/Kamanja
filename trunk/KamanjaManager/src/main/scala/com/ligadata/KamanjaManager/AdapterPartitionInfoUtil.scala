package com.ligadata.KamanjaManager

import org.apache.logging.log4j.{ Logger, LogManager }
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import java.util.UUID
import java.io.{ PrintWriter, File, PrintStream, BufferedReader, InputStreamReader }
import java.util.concurrent.locks.ReentrantReadWriteLock
import scala.io.Source
import scala.io.Source._

case class AdapterPartKeyValues(var key: String, var keyValue: String)

object AdapterPartitionInfoUtil {
  private[this] val LOG = LogManager.getLogger(getClass);
  private var guid: String = ""
  private var lock: ReentrantReadWriteLock = new ReentrantReadWriteLock(true);

  private var counter = 0
  private def increment = {
    counter = counter + 1;
    counter
  }

  val file = "/adapterinfo.json"
  val backupfile1 = "/adapterinfo_bkup1.json"
  val backupfile2 = "/adapterinfo_bkup2.json"
  val backupfile3 = "/adapterinfo_bkup3.json"
  val backupfile4 = "/adapterinfo_bkup4.json"
  val backupfile5 = "/adapterinfo_bkup5.json"

  def generateAdapterInfoJson(adapterinfoMap: scala.collection.mutable.Map[String, scala.collection.mutable.ArrayBuffer[AdapterPartKeyValues]], nodeId: String, nodestarttime: Long): String = {
    var adapterInfoString: String = ""
    var counter = 0
    setGuid(System.currentTimeMillis())

    val json = ("uuid" -> guid) ~
      ("uniquecounter" -> increment) ~
      ("nodestarttime" -> nodestarttime) ~
      ("nodeid" -> nodeId) ~
      ("adapterinfo" -> adapterinfoMap.map(adapinfo =>
        ("adaptername" -> adapinfo._1) ~
          ("keyvalues" -> adapinfo._2.toList.map(kv =>
            ("key" -> kv.key) ~
              ("value" -> kv.keyValue)))));

    adapterInfoString = compact(render(json))

    if (adapterinfoMap != null && adapterinfoMap.size > 0) {
      adapterinfoMap.foreach(adapInfo => {
        println("2 adapInfo._1" + adapInfo._1)
        println(adapInfo._2.map(a => println("2key values: " + a.key + " : " + a.keyValue)))
      })
    }
    adapterInfoString
  }

  private def setGuid(curTime: Long) {
    guid = UUID.randomUUID().toString

  }

  def writeToFile(allPartitions: scala.collection.mutable.ArrayBuffer[JValue], nodeadapterInfoPath: String): Unit = {
    if (nodeadapterInfoPath.equalsIgnoreCase(null)) {
      LOG.error("nodeadapterInfoPath should not be null for select operation")
    } else {
      if (!allPartitions.isEmpty) {
        val filename = nodeadapterInfoPath + file
        val finaljson = allPartitions.map { key =>
          (key)
        }
        new PrintWriter(filename) {
          write(pretty(render(finaljson)));
          close
        }
      } else {
        LOG.error("no data retrieved")
      }
    }
  }

  /*  read to local map from local drive 
  def readfromFile(nodeId: String, localDriveLocation: String, adapPartitionMap: scala.collection.mutable.Map[String, String]) = {
    //  get the file from local drive
    //  add to the map
    var location: String = ""
    var adapterpartitioninfo: String = ""
    if (!localDriveLocation.endsWith("/"))
      location = localDriveLocation + "/"
    else
      location = localDriveLocation
    val fileStr = location + nodeId + file
    println("fileStr" + fileStr)
    if (fileExist(fileStr)) {
      adapterpartitioninfo = readFile(fileStr)
      //  val mapOriginal = parse(adapterpartitioninfo).values.asInstanceOf[scala.collection.immutable.List[Any]]
      val mapOriginal = parse(adapterpartitioninfo)
      val children = mapOriginal.children
      if (children != null) {
        children.foreach(child => {
          println("child" + child)
          val nodeidJStr = child \ "nodeid"
          val nodeid = nodeidJStr.values.toString()
          println("nodeid " + nodeid)
          val json = pretty(render(child))
          if (nodeid != null && nodeid.trim().length() > 0 && json != null) {
            adapPartitionMap(nodeid) = json
          }
        })
      }
    } else println("file do not exists")   
  }*/

  /* read to local map from local drive */
  def readfromFile(nodeId: String, localDriveLocation: String): String = {
    //  get the file from local drive
    //  add to the map
    var location: String = ""
    var adapterpartitioninfo: String = ""
    if (!localDriveLocation.endsWith("/"))
      location = localDriveLocation + "/"
    else
      location = localDriveLocation
    val fileStr = location + nodeId + file
    println("fileStr" + fileStr)
    if (fileExist(fileStr)) {
      adapterpartitioninfo = readFile(fileStr)
    }
    return adapterpartitioninfo
  }

  def readFile(filePath: String): String = { //This method used to read a whole file (from header && pmml)
    return fromFile(filePath).mkString
  }

  def fileExist(filePath: String): Boolean = { //This method used to check if file exists or not (return true if exists and false otherwise)
    return new java.io.File(filePath).exists
  }

  def takeBackUpAndWriteToFile(allPartitions: scala.collection.mutable.ArrayBuffer[JValue], nodeadapterInfoPath: String): Unit = {
    try {
      lock.writeLock().lock();
      {
        if (fileExist(nodeadapterInfoPath + backupfile5))
          deleteFile(nodeadapterInfoPath + backupfile5)
        if (fileExist(nodeadapterInfoPath + backupfile4)) {
          val str = Source.fromFile(nodeadapterInfoPath + backupfile4).mkString;
          val pw = new PrintWriter(new File(nodeadapterInfoPath + backupfile5))
          pw.write(str)
          pw.close
        }
      };
      {
        if (fileExist(nodeadapterInfoPath + backupfile4))
          deleteFile(nodeadapterInfoPath + backupfile4)
        if (fileExist(nodeadapterInfoPath + backupfile3)) {
          val str = Source.fromFile(nodeadapterInfoPath + backupfile3).mkString;
          val pw = new PrintWriter(new File(nodeadapterInfoPath + backupfile4))
          pw.write(str)
          pw.close
        }
      };
      {
        if (fileExist(nodeadapterInfoPath + backupfile3))
          deleteFile(nodeadapterInfoPath + backupfile3)
        if (fileExist(nodeadapterInfoPath + backupfile2)) {
          val str = Source.fromFile(nodeadapterInfoPath + backupfile2).mkString;
          val pw = new PrintWriter(new File(nodeadapterInfoPath + backupfile3))
          pw.write(str)
          pw.close
        }
      };
      {
        if (fileExist(nodeadapterInfoPath + backupfile2))
          deleteFile(nodeadapterInfoPath + backupfile2)
        if (fileExist(nodeadapterInfoPath + backupfile1)) {
          val str = Source.fromFile(nodeadapterInfoPath + backupfile1).mkString;
          val pw = new PrintWriter(new File(nodeadapterInfoPath + backupfile2))
          pw.write(str)
          pw.close
        }
      };
      {
        if (fileExist(nodeadapterInfoPath + backupfile1))
          deleteFile(nodeadapterInfoPath + backupfile1)
        if (fileExist(nodeadapterInfoPath + file)) {
          val str = Source.fromFile(nodeadapterInfoPath + file).mkString;
          val pw = new PrintWriter(new File(nodeadapterInfoPath + backupfile1))
          pw.write(str)
          pw.close
        }
      };
      writeToFile(allPartitions, nodeadapterInfoPath)

    } catch {
      case e: Exception => LOG.error("taking backup of adapter file" + e.getMessage)
    } finally {
      lock.writeLock().unlock()
    }

  }

  private def deleteFile(path: String) = {
    val fileTemp = new File(path)
    if (fileTemp.exists) {
      fileTemp.delete()
    }
  }

  def consolidatingAdapterInfo(allPartitions: scala.collection.mutable.Map[String, String]): String = { /////Map[String, Array[(String, String)]] = {
    var condolidatedAdapterInfoMap = Map[String, Array[(String, String)]]()
    var consolidatedAdapterInfoJsoStr: String = ""
    try {
      var nodeAdapParitionInfo = scala.collection.mutable.Map[String, scala.collection.mutable.ArrayBuffer[JValue]]()
      var arrJValue = scala.collection.mutable.ArrayBuffer[JValue]()
      var finalAdapterInfo: JValue = null

      if (allPartitions.size > 1) {
        println("11111 ")
        var arrJValue = new scala.collection.mutable.ArrayBuffer[JValue]
        allPartitions.foreach(adapInfo => {
          val jVal = parse(adapInfo._2)
          arrJValue += jVal
          println("adapInfo._2 " + parse(adapInfo._2))
        })
        println("adapInfo._2 " + arrJValue)
        println("2222 " + arrJValue.size)

        val uuidStr = "uuid"
        val temp = getHighestUUIDAdapInfo(arrJValue, uuidStr)
        var filteredNodeJValue = new scala.collection.mutable.ArrayBuffer[JValue];
        for (i <- 0 until arrJValue.size) {
          val guid = arrJValue(i) \ uuidStr
          val uuid = guid.values.toString()
          if (uuid.equals(temp)) {
            filteredNodeJValue += arrJValue(i)
          }
        }
        val nodestrttimeStr = "nodestarttime"
        var filtereCounterJValue = new scala.collection.mutable.ArrayBuffer[JValue];
        val nodeStTimeLst = getHighestCounterAdapInfo(filteredNodeJValue, nodestrttimeStr)
        for (i <- 0 until filteredNodeJValue.size) {
          val nodestrttime = filteredNodeJValue(i) \ nodestrttimeStr
          val nodestarttime = nodestrttime.values.toString().asInstanceOf[Long]
          if (nodestrttime.equals(nodeStTimeLst)) {
            filtereCounterJValue += filteredNodeJValue(i)
          }
        }
        val counterStr = "counter"
        val cntr_highest = getHighestCounterAdapInfo(filtereCounterJValue, counterStr)
        for (i <- 0 until filtereCounterJValue.size) {
          val cntr = filtereCounterJValue(i) \ counterStr
          val counter = cntr.values.toString().asInstanceOf[Long]
          if (counter.equals(cntr_highest)) {
            finalAdapterInfo = filtereCounterJValue(i)
          }
        }
      } else if (allPartitions.size == 1) {
        allPartitions.foreach(adapInfo => {
          finalAdapterInfo = parse(adapInfo._2)
        })
      }

      finalAdapterInfo.values

    } catch {
      case e: Exception => LOG.error("Consolidating the adapter parition info " + e.getMessage)
    }
    return consolidatedAdapterInfoJsoStr
  }

  def getHighestUUIDAdapInfo(arrJValue: scala.collection.mutable.ArrayBuffer[JValue], key: String): String = {
    var temp: String = ""
    for (i <- 0 until arrJValue.size) {
      val guid = arrJValue(i) \ key
      val uuid = guid.values.toString()
      if (i == 0) temp = uuid
      if (i + 1 < arrJValue.size) {
        val nextguid = arrJValue(i + 1) \ key
        val nextuuid = nextguid.values.toString()
        val comp = compareUUID(temp, nextuuid) //UUID.fromString(temp).compareTo(UUID.fromString(nextuuid))
        if (comp > 0)
          temp = nextuuid
        println("i" + i)
        println("temp " + temp)
      }
    }
    return temp
  }

  def compareUUID(from: String, to: String): Int = {
    UUID.fromString(from).compareTo(UUID.fromString(to))
  }

  def getHighestCounterAdapInfo(filteredNodeJValue: scala.collection.mutable.ArrayBuffer[JValue], key: String): Long = {

    var tempNodeStrtTime: Long = 0L
    for (i <- 0 until filteredNodeJValue.size) {
      val nodestrttime = filteredNodeJValue(i) \ key
      val nodestarttime = nodestrttime.values.toString().asInstanceOf[Long]
      if (i == 0) tempNodeStrtTime = nodestarttime
      if (i + 1 < filteredNodeJValue.size) {
        val nextnodestrttime = filteredNodeJValue(i + 1) \ key
        val nexnodestrttime = nextnodestrttime.values.toString().asInstanceOf[Long]
        val comp = tempNodeStrtTime.compareTo(nexnodestrttime)
        if (comp > 0)
          tempNodeStrtTime = nexnodestrttime
        println("i" + i)
        println("tempNodeStrtTime " + tempNodeStrtTime)
      }
    }
    return tempNodeStrtTime
  }

}
//compare 100 uuids

    //======

    //get the all nodes adapter json, 
    //take the same node adapter partition information,. put it on arraybuffer

    //compare all the uuid of all the nodes json string,
    //if uuid matches compare the node start time
    //take the latest nodestart time for that node, repeat above for all nodes
    //Build the final Map[String, Array[String, String]]  (adaptername, Array(key, values)) for all nodes..


  //   addlistenerwithconsoliodateString

  //add distribution mechanism
