package com.ligadata.KamanjaManager.embedded

import java.io.FileInputStream
import java.util.Properties

import com.ligadata.KamanjaManager.{KamanjaConfiguration, KamanjaManager}
import com.ligadata.test.configuration.cluster.zookeeper._
import com.ligadata.test.embedded.zookeeper._

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.util.control.Breaks._
import org.apache.logging.log4j.{LogManager, Logger}

class EmbeddedKamanjaManager {
  private val logger: Logger = LogManager.getLogger(this.getClass)
  var isRunning: Boolean = false
  var engThread: Thread = null
  var nodeId: String = ""

  def startup(configFile: String, zkConfig: ZookeeperConfig, zkc: ZookeeperClient, timeout: Int = 30): Int = {
    logger.info(s"[Embedded Kamanja Manager]: Starting Kamanja Manager with configuration file $configFile...")
    val confProperties: Properties = new Properties()
    confProperties.load(new FileInputStream(configFile))
    nodeId = confProperties.getProperty("NODEID")
    zkc.init

    if (!isRunning(zkConfig, zkc)) {
      val engProc = new Runnable {
        def run(): Unit = {
          try {
            val argArray = Array("--config", configFile)
            KamanjaManager.main(argArray)
            //if (returnCode != 0) {
              //throw new EmbeddedKamanjaManagerException("[Embedded Kamanja Manager]: Kamanja Manager failed to start with return code " + returnCode)
            //}
          }
          catch {
            case e: Exception => throw new EmbeddedKamanjaManagerException("[Embedded Kamanja Manager]: Failed to start the engine with the following exception:\n" + e)
            case e: Throwable => throw new EmbeddedKamanjaManagerException("[Embedded Kamanja Manager]: Failed to start the engine with the following exception:\n" + e)
          }
        }
      }

      engThread = new Thread(engProc)
      engThread.start()

      breakable {
        for (i <- 0 to timeout) {
          if (isRunning(zkConfig, zkc)) {
            logger.info("[Embedded Kamanja Manager]: Kamanja Manager started")
            return 0
          }
          Thread sleep 1000
        }
      }

      engThread = null
      logger.error("[Embedded Kamanja Manager]: Failed to start Kamanja Manager")
      return 1
    }
    else {
      logger.info("[Embedded Kamanja Manager]: Kamanja Manager is already running. Continuing...")
      return 0
    }
  }

  def shutdown(zkConfig: ZookeeperConfig, zkc: ZookeeperClient): Int = {
    logger.info("[Embedded Kamanja Manager]: Shutting down Kamanja Manager...")
    KamanjaConfiguration.shutdown = true

    for (i <- 0 to 30) {
      if (!isRunning(zkConfig, zkc)) {
        logger.info("[Embedded Kamanja Manager]: Kamanja Manager successfully shut down")
        zkc.stop
        return 0
      }
      Thread sleep 1000
    }
    logger.error("[Embedded Kamanja Manager]: Failed to shut down Kamanja Manager")
    return 1
  }

  def restart(configFile: String, zkConfig: ZookeeperConfig, zkc: ZookeeperClient): Int = {
    logger.info("[Embedded Kamanja Manager]: Restarting Kamanja Manager...")

    val shutdownStatus = shutdown(zkConfig,zkc)
    if (shutdownStatus != 0) {
      logger.error("[Embedded Kamanja Manager]: Failed to restart Kamanja Manager")
      return shutdownStatus
    }

    val startupStatus = startup(configFile, zkConfig, zkc)
    if (startupStatus != 0) {
      logger.error("[Embedded Kamanja Manager]: Failed to restart Kamanja Manager")
      return startupStatus
    }

    logger.info("[Embedded Kamanja Manager]: Kamanja Manager has been restarted")

    return 0
  }

  private def isRunning(zkConfig: ZookeeperConfig, zkc: ZookeeperClient): Boolean = {
    val formatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    var heartbeatJson: String = ""

    try {
      heartbeatJson = zkc.getNodeData(zkConfig.zkNodeBasePath + s"/monitor/engine/$nodeId")
      if (heartbeatJson == "") {
        logger.info("[Embedded Kamanja Manager]: Kamanja Manager is not running... no heartbeat found")
        return false
      }
      else {
        val firstSeen: DateTime = formatter.parseDateTime((parse(heartbeatJson) \ "LastSeen").values.toString)
        for (i <- 0 to 5) {
          val lastSeenHBJson: String = zkc.getNodeData(zkConfig.zkNodeBasePath + s"/monitor/engine/$nodeId")
          val lastSeen: DateTime = formatter.parseDateTime((parse(lastSeenHBJson) \ "LastSeen").values.toString)
          if (firstSeen.isBefore(lastSeen)) {
            logger.info("[Embedded Kamanja Manager]: Kamanja Manager is running")
            return true
          }
          else {
            Thread sleep 1000
          }
        }
        logger.info("[Embedded Kamanja Manager]: Kamanja Manager is not running. Heartbeat found but StartTime and LastSeen are the same.")
        logger.debug("[Embedded Kamanja Manager]: HeartBeat:\n" + heartbeatJson)
        return false
      }
    }
    catch {
      case e: EmbeddedZookeeperException =>
        logger.warn("[Embedded Kamanja Manager]: Kamanja Manager is not running due to an EmbeddedZookeeperException")
        logger.debug("[Embedded Kamanja Manager]: EmbeddedZookeeperException", e)
        return false
      case e: Exception =>
        logger.error("[Embedded Kamanja Manager]: Unexpected exception caught...", e)
        return false
    }
  }
}
