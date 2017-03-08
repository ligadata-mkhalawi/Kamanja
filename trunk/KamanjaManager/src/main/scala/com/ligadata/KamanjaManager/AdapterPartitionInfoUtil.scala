package com.ligadata.KamanjaManager

import org.apache.logging.log4j.{ Logger, LogManager }
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import java.util.UUID
import java.io.{ PrintWriter, File, PrintStream, BufferedReader, InputStreamReader }

case class AdapterPartKeyValues(var key: String, var keyValue: String)

object AdapterPartitionInfoUtil {
  private[this] val LOG = LogManager.getLogger(getClass);
  private var guid: String = ""

  private var counter = 0
  private def increment = {
    counter = counter + 1;
    counter
  }
  def generateAdapterInfoJson(adapterinfoMap: scala.collection.mutable.Map[String, scala.collection.mutable.ArrayBuffer[AdapterPartKeyValues]], nodeId: String): String = {
    var adapterInfoString: String = ""
    var counter = 0
    setGuid(System.currentTimeMillis())

    val json = ("uuid" -> guid) ~
      ("uniquecounter" -> increment) ~
      ("nodestarttime" -> System.currentTimeMillis()) ~
      ("nodeid" -> nodeId) ~
      ("adapterinfo" -> adapterinfoMap.map(adapinfo =>
        ("adaptername" -> adapinfo._1) ~
          ("keyvalues" -> adapinfo._2.toList.map(kv =>
            ("key" -> kv.key) ~
              ("value" -> kv.keyValue)))));

    adapterInfoString = compact(render(json))

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
        val filename = nodeadapterInfoPath + "/adapterinfo.json"
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
}