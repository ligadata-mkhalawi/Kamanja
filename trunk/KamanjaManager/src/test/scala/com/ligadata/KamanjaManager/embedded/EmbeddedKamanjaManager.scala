package com.ligadata.KamanjaManager.embedded

import java.io.FileInputStream
import java.util.Properties

import com.ligadata.test.utils.KamanjaTestLogger
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.util.control.Breaks._
import com.ligadata.KamanjaManager.{KamanjaConfiguration, KamanjaManager}
import com.ligadata.test.configuration.cluster.zookeeper.ZookeeperConfig
import com.ligadata.test.embedded.zookeeper.{EmbeddedZookeeperException, ZookeeperClient}
import com.ligadata.kamanja.metadata.MdMgr


/**
  * Created by william on 8/8/16.
  */
class EmbeddedKamanjaManager extends KamanjaTestLogger {
  var isRunning: Boolean = false
  var engThread: Thread = null
  var nodeId: String = ""

  def startup(configFile: String, zkConfig: ZookeeperConfig, zkc: ZookeeperClient): Int = {
    logger.info("AUTOMATION-KAMANJA-MANAGER: Starting Kamanja Manager...")
    val confProperties: Properties = new Properties()
    confProperties.load(new FileInputStream(configFile))
    nodeId = confProperties.getProperty("NODEID")

    if (!isRunning(zkConfig, zkc)) {
      val engProc = new Runnable {
        def run(): Unit = {
          try {
            val argArray = Array("--config", configFile)
            KamanjaManager.main(argArray)
            //if (returnCode != 0) {
              //throw new EmbeddedKamanjaManagerException("AUTOMATION-KAMANJA-MANAGER: Kamanja Manager failed to start with return code " + returnCode)
            //}
          }
          catch {
            case e: Exception => throw new EmbeddedKamanjaManagerException("AUTOMATION-KAMANJA-MANAGER: Failed to start the engine with the following exception:\n" + e)
            case e: Throwable => throw new EmbeddedKamanjaManagerException("AUTOMATION-KAMANJA-MANAGER: Failed to start the engine with the following exception:\n" + e)
          }
        }
      }

      engThread = new Thread(engProc)
      engThread.start()

      breakable {
        for (i <- 0 to 30) {
          if (isRunning(zkConfig, zkc)) {
            logger.info("AUTOMATION-KAMANJA-MANAGER: Kamanja Manager started")
            return 0
          }
          Thread sleep 1000
        }
      }

      engThread = null
      logger.error("AUTOMATION-KAMANJA-MANAGER: Failed to start Kamanja Manager")
      return 1
    }
    else {
      logger.info("AUTOMATION-KAMANJA-MANAGER: Kamanja Manager is already running. Continuing...")
      return 0
    }
  }

  def shutdown(zkConfig: ZookeeperConfig, zkc: ZookeeperClient): Int = {
    logger.info("AUTOMATION-KAMANJA-MANAGER: Shutting down Kamanja Manager...")
    KamanjaConfiguration.shutdown = true

    for (i <- 0 to 30) {
      if (!isRunning(zkConfig, zkc)) {
        logger.info("AUTOMATION-KAMANJA-MANAGER: Kamanja Manager successfully shut down")
        return 0
      }
      Thread sleep 1000
    }
    logger.error("AUTOMATION-KAMANJA-MANAGER: Failed to shut down Kamanja Manager")
    return 1
  }

  def restart(configFile: String, zkConfig: ZookeeperConfig, zkc: ZookeeperClient): Int = {
    logger.info("AUTOMATION-KAMANJA-MANAGER: Restarting Kamanja Manager...")

    val shutdownStatus = shutdown(zkConfig,zkc)
    if (shutdownStatus != 0) {
      logger.error("AUTOMATION-KAMANJA-MANAGER: Failed to restart Kamanja Manager")
      return shutdownStatus
    }

    val startupStatus = startup(configFile, zkConfig, zkc)
    if (startupStatus != 0) {
      logger.error("AUTOMATION-KAMANJA-MANAGER: Failed to restart Kamanja Manager")
      return startupStatus
    }

    logger.info("AUTOMATION-KAMANJA-MANAGER: Kamanja Manager has been restarted")

    return 0
  }

  private def isRunning(zkConfig: ZookeeperConfig, zkc: ZookeeperClient): Boolean = {
    val formatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    var heartbeatJson: String = ""
    try {
      heartbeatJson = zkc.getNodeData(zkConfig.zkNodeBasePath + s"/monitor/engine/$nodeId")
      if (heartbeatJson == "") {
        logger.info("AUTOMATION-KAMANJA-MANAGER: Kamanja Manager is not running")
        return false
      }
      else {
        val firstSeen: DateTime = formatter.parseDateTime((parse(heartbeatJson) \ "LastSeen").values.toString)
        for (i <- 0 to 5) {
          val lastSeenHBJson: String = zkc.getNodeData(zkConfig.zkNodeBasePath + s"/monitor/engine/$nodeId")
          val lastSeen: DateTime = formatter.parseDateTime((parse(lastSeenHBJson) \ "LastSeen").values.toString)
          if (firstSeen.isBefore(lastSeen)) {
            logger.info("AUTOMATION-KAMANJA-MANAGER: Kamanja Manager is running")
            return true
          }
          else {
            Thread sleep 1000
          }
        }
        logger.info("AUTOMATION-KAMANJA-MANAGER: Kamanja Manager is not running")
        return false
      }
    }
    catch {
      case e: EmbeddedZookeeperException =>
        logger.info("AUTOMATION-KAMANJA-MANAGER: Kamanja Manager is not running")
        return false
      case e: Exception =>
        logger.error("AUTOMATION-KAMANJA-MANAGER: Unexpected exception caught...", e)
        return false
    }
  }
}
