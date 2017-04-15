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
      throw EmbeddedZookeeperException("[Embedded Zookeeper Client]: You must call def init first")
    }
    if(!isRunning) {
      try {
        logger.info("[Embedded Zookeeper Client]: Starting Curator Framework")
        zkc.start()
        isRunning = true
      }
      catch {
        case e: Exception => {
          logger.error("[Embedded Zookeeper Client]: Failed to start Curator Framework")
          throw EmbeddedZookeeperException("[Embedded Zookeeper Client]: Failed to start Curator Framework", e)
        }
      }
    }
  }

  def stop(): Unit = {
    if(isRunning) {
      try {
        logger.info("[Embedded Zookeeper Client]: Stopping Curator Framework")
        zkc.close()
        isRunning = false
      }
      catch {
        case e: Exception => {
          logger.error("[Embedded Zookeeper Client]: Failed to stop Curator Framework")
          throw EmbeddedZookeeperException("[Embedded Zookeeper Client]: Failed to stop Curator Framework")
        }
      }
    }
  }

  def doesNodeExist(znodePath:String): Boolean = {
    if(!isInit) {
      throw EmbeddedZookeeperException("[Embedded Zookeeper Client]: You must call def init first")
    }
    if(!isRunning){
      this.start()
    }
    try {
      logger.debug("[Embedded Zookeeper Client]: Checking if zookeeper node path '" + znodePath + "' exists")
      if (zkc.checkExists().forPath(znodePath) == null) {
        logger.debug("[Embedded Zookeeper Client]: Zookeeper node path '" + znodePath + "' doesn't exist")
        false
      }
      else {
        //logger.debug("[Embedded Zookeeper Client]: Zookeeper node path found with data: " + zkc.checkExists().forPath(znodePath))
        true
      }
    }
    catch {
      case e: Exception => {
        logger.error(s"[Embedded Zookeeper Client]: Failed to verify node '$znodePath' exists", e)
        throw EmbeddedZookeeperException(s"[Embedded Zookeeper Client]: Failed to verify node '$znodePath' exists", e)
      }
    }
  }

  def waitForNodeToExist(znodePath:String, timeout:Int):Boolean = {
    if(!isInit) {
      throw new EmbeddedZookeeperException("[Embedded Zookeeper Client]: You must call def init first")
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
          logger.error("[Embedded Zookeeper Client]: Caught exception\n" + e)
          throw new EmbeddedZookeeperException("[Embedded Zookeeper Client]: Caught exception\n" + e)
        }
        case e: Exception => throw new EmbeddedZookeeperException("[Embedded Zookeeper Client]: Caught exception\n" + e)
      }
      Thread sleep 1000
    }
    logger.warn("[Embedded Zookeeper Client]: Node '" + znodePath + "' doesn't exist after " + timeout + " seconds")
    return false
  }

  def getNodeData(znodePath: String): String = {
    if(!isInit) {
      throw new EmbeddedZookeeperException("[Embedded Zookeeper Client]: You must call def init first")
    }
    if(!isRunning) {
      this.start()
    }
    try {
      logger.debug(s"[Embedded Zookeeper Client]: Retrieving data from node path '$znodePath'")
      if(doesNodeExist(znodePath)) {
        return new String(zkc.getData().forPath(znodePath))
      }
      else {
        return ""
      }
    }
    catch {
      case e: Exception => throw new EmbeddedZookeeperException(s"[Embedded Zookeeper Client]: Caught exception while attempting to get data from znode path '$znodePath'", e)
    }
  }
}