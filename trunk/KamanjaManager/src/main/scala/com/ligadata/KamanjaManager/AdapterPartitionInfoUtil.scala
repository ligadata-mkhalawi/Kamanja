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

case class AdapterPartKeyValues(var key: String, var keyvalue: String)

case class PartitionKeyValuesInfo(var nodeid: String, var uuid: String, var nodestarttime: Long, var uniquecounter: Long, var keyvalues: Array[AdapterPartKeyValues])

case class PartitionKeyValues(var key: String, var keyvalue: String, var nodeid: String, var uuid: String, var nodestarttime: Long, var uniquecounter: Long)

case class PartKeyValues(var keyvalues: Array[PartitionKeyValues])

case class NodeInfo(var NodeId: String, var UUID: String)

object AdapterPartitionInfoUtil {
  private[this] val LOG = LogManager.getLogger(getClass);
  private var guid: String = ""
  private var lock: ReentrantReadWriteLock = new ReentrantReadWriteLock(true);

  val file = "/adapterinfo.json"
  val backupfile1 = "/adapterinfo_bkup1.json"
  val backupfile2 = "/adapterinfo_bkup2.json"
  val backupfile3 = "/adapterinfo_bkup3.json"
  val backupfile4 = "/adapterinfo_bkup4.json"
  val backupfile5 = "/adapterinfo_bkup5.json"

  def generateAdapterInfoJson_Old(uuid: String, nodeId: String, nodestarttime: Long, adapterinfoMap: scala.collection.mutable.Map[String, scala.collection.mutable.ArrayBuffer[AdapterPartKeyValues]], increment: Long): String = {
    var adapterInfoString: String = ""

    val json = ("uuid" -> uuid) ~
      ("uniquecounter" -> increment) ~
      ("nodestarttime" -> nodestarttime) ~
      ("nodeid" -> nodeId) ~
      ("adapterinfo" -> adapterinfoMap.map(adapinfo =>
        ("adaptername" -> adapinfo._1) ~
          ("keyvalues" -> adapinfo._2.toList.map(kv =>
            ("key" -> kv.key) ~
              ("value" -> kv.keyvalue)))));

    adapterInfoString = compact(render(json))
    adapterInfoString
  }

  def generateAdapterInfoJson(paritionKeyValues: PartitionKeyValuesInfo): String = {
    var adapterInfoString: String = ""
    val guid = setGuid(System.currentTimeMillis())

    val json = ("uuid" -> paritionKeyValues.uuid) ~
      ("uniquecounter" -> paritionKeyValues.uniquecounter) ~
      ("nodestarttime" -> paritionKeyValues.nodestarttime) ~
      ("nodeid" -> paritionKeyValues.nodeid) ~
      ("keyvalues" -> paritionKeyValues.keyvalues.toList.map(kv =>
        ("key" -> kv.key) ~
          ("keyvalue" -> kv.keyvalue)));

    adapterInfoString = compact(render(json))
    adapterInfoString
  }

  def setGuid(curTime: Long): String = {
    guid = UUID.randomUUID().toString
    return guid
  }

  def writeToFile(allPartitions: scala.collection.mutable.Map[String, (String, String, String, Long, Long)], nodeadapterInfoPath: String): Unit = {
    try {
      if (nodeadapterInfoPath.equalsIgnoreCase(null)) {
        LOG.error("nodeadapterInfoPath should not be null for select operation")
      } else {
        if (!allPartitions.isEmpty) {
          val filename = nodeadapterInfoPath + file
          val finaljson = generatePartitionInfoJson(allPartitions)
          new PrintWriter(filename) {
            write(finaljson);
            close
          }
        } else {
          LOG.error("no data retrieved")
        }
      }
    } catch {
      case e: Exception => LOG.debug("AdapterPartitionInfoUtil - writeToFile " + e.getMessage)
    }
  }

  private def generatePartitionInfoJson(allPartitions: scala.collection.mutable.Map[String, (String, String, String, Long, Long)]): String = {

    var adapterInfoString: String = ""
    val guid = setGuid(System.currentTimeMillis())

    val json = ("keyvalues" -> allPartitions.map(kv =>
      ("key" -> kv._1) ~
        ("keyvalue" -> kv._2._1) ~
        ("nodeid" -> kv._2._2) ~
        ("uuid" -> kv._2._3) ~
        ("nodestarttime" -> kv._2._4) ~
        ("uniquecounter" -> kv._2._5)));

    adapterInfoString = compact(render(json))
    adapterInfoString
  }

  /* read to local map from local drive */
  def readfromFile(nodeId: String, localDriveLocation: String): String = {
    //  get the file from local drive
    //  add to the map

    var location: String = ""
    var adapterpartitioninfo: String = ""
    try {
      if (!localDriveLocation.endsWith("/"))
        location = localDriveLocation + "/"
      else
        location = localDriveLocation
      val fileStr = location + nodeId + file
      if (fileExist(fileStr)) {
        adapterpartitioninfo = readFile(fileStr)
      }
    } catch {
      case e: Exception => LOG.debug("AdapterPartitionInfoUtil - readfromFile " + e.getMessage)
    }
    return adapterpartitioninfo
  }

  def readFile(filePath: String): String = {
    return fromFile(filePath).mkString
  }

  def fileExist(filePath: String): Boolean = { //This method used to check if file exists or not (return true if exists and false otherwise)
    return new java.io.File(filePath).exists
  }

  def takeBackUpAndWriteToFile(allPartitions: scala.collection.mutable.Map[String, (String, String, String, Long, Long)], nodeadapterInfoPath: String): Unit = {
    var localAllPartitions = scala.collection.mutable.Map[String, (String, String, String, Long, Long)]()
    try {
      lock.writeLock().lock();
      deleteOldFileAndWriteToFile(nodeadapterInfoPath, backupfile5, backupfile4)
      deleteOldFileAndWriteToFile(nodeadapterInfoPath, backupfile4, backupfile3)
      deleteOldFileAndWriteToFile(nodeadapterInfoPath, backupfile3, backupfile2)
      deleteOldFileAndWriteToFile(nodeadapterInfoPath, backupfile2, backupfile1)
      deleteOldFileAndWriteToFile(nodeadapterInfoPath, backupfile1, file)
      localAllPartitions = allPartitions
    } catch {
      case e: Exception => LOG.error("taking backup of adapter file" + e.getMessage)
    } finally {
      lock.writeLock().unlock()
    }
    writeToFile(localAllPartitions, nodeadapterInfoPath)
  }

  private def deleteOldFileAndWriteToFile(nodeadapterInfoPath: String, oldFile: String, newFile: String) = {
    try {
      if (fileExist(nodeadapterInfoPath + oldFile))
        deleteFile(nodeadapterInfoPath + oldFile)
      if (fileExist(nodeadapterInfoPath + newFile)) {
        val str = Source.fromFile(nodeadapterInfoPath + newFile).mkString;
        val pw = new PrintWriter(new File(nodeadapterInfoPath + oldFile))
        pw.write(str)
        pw.close
      }
    } catch {
      case e: Exception => LOG.error("deleteOldFileAndWriteToFile " + e.getMessage)
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
        var arrJValue = new scala.collection.mutable.ArrayBuffer[JValue]
        allPartitions.foreach(adapInfo => {
          val jVal = parse(adapInfo._2)
          arrJValue += jVal
        })

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
      }
    }
    return tempNodeStrtTime
  }

}

