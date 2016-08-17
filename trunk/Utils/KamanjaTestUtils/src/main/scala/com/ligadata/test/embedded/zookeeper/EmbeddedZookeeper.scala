package com.ligadata.test.embedded.zookeeper

import java.io.{File, FileNotFoundException}
import java.net.InetSocketAddress
import org.apache.zookeeper.server.{ZooKeeperServer, ServerCnxnFactory}
import com.ligadata.test.utils.{KamanjaTestLogger, TestUtils}

class EmbeddedZookeeper extends KamanjaTestLogger {
  private var factory: ServerCnxnFactory = _
  private var snapshotDir: File = _
  private var logDir: File = _
  private var isRunning: Boolean = false
  private var port: Int = _

  def startup: Unit = {
    // This should get a new port at startup and set EmbeddedZookeeper.port at the new port
    if (!isRunning) {
      logger.info("[Embedded-Zookeeper]: Starting Zookeeper...")
      this.factory = ServerCnxnFactory.createFactory(new InetSocketAddress("localhost", getPort), 1024)
      this.snapshotDir = TestUtils.constructTempDir("zookeeper/snapshot")
      this.logDir = TestUtils.constructTempDir("zookeeper/data")
      try {
        factory.startup(new ZooKeeperServer(snapshotDir, logDir, 500))
        logger.info("[Embedded-Zookeeper]: Zookeeper server launched. Connection String='" + getConnection + "'")
        isRunning = true
      }
      catch {
        case e: InterruptedException => throw new EmbeddedZookeeperException("[Embedded-Zookeeper]: Failed to start embedded zookeeper instance with exception:\n" + e)
      }
    }
  }

  def shutdown: Unit = {
    if (isRunning) {
      logger.info("[Embedded-Zookeeper]: Shutting down zookeeper")
      factory.shutdown()
      logger.info("[Embedded-Zookeeper]: Zookeeper shutdown")
      isRunning = false
      try {
        logger.info(s"[Embedded-Zookeeper]: Deleting zookeeper snapshot directory $snapshotDir")
        TestUtils.deleteFile(snapshotDir)
        logger.info(s"[Embedded-Zookeeper]: Deleting zookeeper snapshot directory $logDir")
        TestUtils.deleteFile(logDir)
        logger.info("[Embedded-Zookeeper]: Zookeeper files deleted")
      }
      catch {
        case e: FileNotFoundException => throw new EmbeddedZookeeperException(s"[Embedded-Zookeeper]: Unable to find file $logDir or $snapshotDir", e)
      }
    }
  }

  def getConnection: String = {
    s"localhost:$getPort"
  }

  def getPort: Int = {
    if(this.port <= 0) {
      this.port = TestUtils.getAvailablePort
    }
    return this.port
  }

  def setPort(port: Int): Unit = {
    this.port = port
  }

  override def toString: String = {
    val sb: StringBuilder = new StringBuilder("EmbeddedZookeeper{")
    sb.append("connection=").append(getConnection)
    sb.append('}')
    sb.toString
  }
}