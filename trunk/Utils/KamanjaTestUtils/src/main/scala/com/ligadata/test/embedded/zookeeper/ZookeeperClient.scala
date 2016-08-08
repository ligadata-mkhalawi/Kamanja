package com.ligadata.test.embedded.zookeeper

import com.ligadata.test.utils.KamanjaTestLogger

import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.retry.ExponentialBackoffRetry
import org.joda.time._

class ZookeeperClient(zkcConnectString:String, sessionTimeoutMs:Int = 30000, connectionTimeoutMs:Int = 30000) extends KamanjaTestLogger {
  private val retryPolicy = new ExponentialBackoffRetry(1000, 3)
  private var isRunning: Boolean = false
  private var isInit: Boolean = false
  private var zkc: CuratorFramework = _

  def init(): Unit = {
    zkc = CuratorFrameworkFactory.newClient(zkcConnectString, sessionTimeoutMs, connectionTimeoutMs, retryPolicy)
    isInit = true
  }

  def start(): Unit = {
    if(!isInit) {
      throw new EmbeddedZookeeperException("AUTOMATION-ZOOKEEPER-CLIENT: You must call def init first")
    }
    if(!isRunning) {
      try {
        logger.info("AUTOMATION-ZOOKEEPER-CLIENT: Starting Curator Framework")
        zkc.start()
        isRunning = true
      }
      catch {
        case e: Exception => {
          logger.error("AUTOMATION-ZOOKEEPER-CLIENT: Failed to start Curator Framework")
          throw new EmbeddedZookeeperException("AUTOMATION-ZOOKEEPER-CLIENT: Failed to start Curator Framework", e)
        }
      }
    }
  }

  def stop: Unit = {
    if(isRunning) {
      try {
        logger.info("AUTOMATION-ZOOKEEPER-CLIENT: Stopping Curator Framework")
        zkc.close()
        isRunning = false
      }
      catch {
        case e: Exception => {
          logger.error("AUTOMATION-ZOOKEEPER-CLIENT: Failed to stop Curator Framework")
          throw new EmbeddedZookeeperException("AUTOMATION-ZOOKEEPER-CLIENT: Failed to stop Curator Framework")
        }
      }
    }
  }

  def doesNodeExist(znodePath:String): Boolean = {
    if(!isInit) {
      throw new EmbeddedZookeeperException("AUTOMATION-ZOOKEEPER-CLIENT: You must call def init first")
    }
    try {
      logger.debug("AUTOMATION-ZOOKEEPER-CLIENT: Checking if zookeeper node path '" + znodePath + "' exists")
      if (zkc.checkExists().forPath(znodePath) == null) {
        logger.debug("AUTOMATION-ZOOKEEPER-CLIENT: Zookeeper node path '" + znodePath + "' doesn't exist")
        return false
      }
      else {
        //logger.debug("AUTOMATION-ZOOKEEPER-CLIENT: Zookeeper node path found with data: " + zkc.checkExists().forPath(znodePath))
        return true
      }
    }
    catch {
      case e: Exception => {
        logger.error(s"AUTOMATION-ZOOKEEPER-CLIENT: Failed to verify node '$znodePath' exists", e)
        throw new EmbeddedZookeeperException(s"AUTOMATION-ZOOKEEPER-CLIENT: Failed to verify node '$znodePath' exists", e)
      }
    }
  }

  def waitForNodeToExist(znodePath:String, timeout:Int):Boolean = {
    if(!isInit) {
      throw new EmbeddedZookeeperException("AUTOMATION-ZOOKEEPER-CLIENT: You must call def init first")
    }
    var count = 0
    while(count < timeout) {
      count += 1
      try {
        if (doesNodeExist(znodePath))
          return true
      }
      catch {
        case e: EmbeddedZookeeperException => {
          logger.error("AUTOMATION-ZOOKEEPER-CLIENT: Caught exception\n" + e)
          throw new EmbeddedZookeeperException("AUTOMATION-ZOOKEEPER-CLIENT: Caught exception\n" + e)
        }
        case e: Exception => throw new EmbeddedZookeeperException("AUTOMATION-ZOOKEEPER-CLIENT: Caught exception\n" + e)
      }
      Thread sleep 1000
    }
    logger.warn("AUTOMATION-ZOOKEEPER-CLIENT: Node '" + znodePath + "' doesn't exist after " + timeout + " seconds")
    return false
  }

  def getNodeData(znodePath: String): String = {
    if(!isInit) {
      throw new EmbeddedZookeeperException("AUTOMATION-ZOOKEEPER-CLIENT: You must call def init first")
    }
    try {
      logger.debug(s"AUTOMATION-ZOOKEEPER-CLIENT: Retrieving data from node path '$znodePath'")
      if(doesNodeExist(znodePath)) {
        return new String(zkc.getData().forPath(znodePath))
      }
      else {
        return ""
      }
    }
    catch {
      case e: Exception => throw new EmbeddedZookeeperException(s"AUTOMATION-ZOOKEEPER-CLIENT: Caught exception while attempting to get data from znode path '$znodePath'", e)
    }
  }
}