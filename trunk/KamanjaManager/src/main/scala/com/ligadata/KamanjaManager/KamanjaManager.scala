
package com.ligadata.KamanjaManager

import com.ligadata.HeartBeat.MonitoringContext
import com.ligadata.KamanjaBase._
import com.ligadata.InputOutputAdapterInfo.{ ExecContext, InputAdapter, OutputAdapter, ExecContextFactory, PartitionUniqueRecordKey, PartitionUniqueRecordValue }
import com.ligadata.StorageBase.{ DataStore }
import com.ligadata.ZooKeeper.CreateClient
import com.ligadata.kamanja.metadata.MdMgr._
import com.ligadata.kamanja.metadata.{ ContainerDef, MessageDef, AdapterMessageBinding, AdapterInfo }
import org.json4s.jackson.JsonMethods._

import scala.reflect.runtime.{ universe => ru }
import scala.util.control.Breaks._
import scala.collection.mutable.ArrayBuffer
import collection.mutable.{ MultiMap, Set }
import java.io.{ PrintWriter, File, PrintStream, BufferedReader, InputStreamReader }
import scala.util.Random
import scala.Array.canBuildFrom
import java.util.{ Properties, Observer, Observable }
import java.sql.Connection
import scala.collection.mutable.TreeSet
import java.net.{ Socket, ServerSocket }
import java.util.concurrent.{ Executors, ScheduledExecutorService, TimeUnit }
import com.ligadata.Utils.{ Utils, KamanjaClassLoader, KamanjaLoaderInfo }
import org.apache.logging.log4j.{ Logger, LogManager }
import com.ligadata.Exceptions.{ FatalAdapterException }
import scala.actors.threadpool.{ ExecutorService }
import com.ligadata.KamanjaVersion.KamanjaVersion
import com.ligadata.InputOutputAdapterInfo.VelocityMetricsInfo
import com.ligadata.VelocityMetrics.VelocityMetricsFactoryInterface
import com.ligadata.VelocityMetrics.VelocityMetricsCallback
import com.ligadata.VelocityMetrics.VelocityMetricsInstanceInterface
import com.ligadata.VelocityMetrics.Metrics

class KamanjaServer(port: Int) extends Runnable {
  private val LOG = LogManager.getLogger(getClass);
  private val serverSocket = new ServerSocket(port)
  private var exec: ExecutorService = scala.actors.threadpool.Executors.newFixedThreadPool(5)

  LOG.warn("KamanjaServer started for port:" + port)

  def run() {
    try {
      while (KamanjaConfiguration.shutdown == false) {
        // This will block until a connection comes in.
        val socket = serverSocket.accept()
        exec.execute(new ConnHandler(socket))
      }
    } catch {

      case e: java.net.SocketException => {
        if (serverSocket != null && serverSocket.isClosed())
          LOG.warn("Socket Error. May be closed", e)
        else
          LOG.error("Socket Error", e)
      }
      case e: Exception => {
        LOG.error("Socket Error", e)
      }
    } finally {
      exec.shutdownNow()
      if (serverSocket != null && serverSocket.isClosed() == false)
        serverSocket.close
    }
  }

  def shutdown() {
    if (serverSocket != null && serverSocket.isClosed() == false)
      serverSocket.close
  }
}

private class ConnHandler(var socket: Socket) extends Runnable {
  private val LOG = LogManager.getLogger(getClass);
  private val out = new PrintStream(socket.getOutputStream)
  private val in = new BufferedReader(new InputStreamReader(socket.getInputStream))

  socket.setKeepAlive(true)

  LOG.warn("Created a connection to socket. HostAddress:%s, Port:%d, LocalPort:%d".format(socket.getLocalAddress.getHostAddress, socket.getPort, socket.getLocalPort))

  def run() {
    val vt = 0
    try {
      breakable {
        while (KamanjaConfiguration.shutdown == false) {
          val strLine = in.readLine()
          if (strLine == null)
            break
          LOG.warn("Current Command:%s. HostAddress:%s, Port:%d, LocalPort:%d".format(strLine, socket.getLocalAddress.getHostAddress, socket.getPort, socket.getLocalPort))
          KamanjaManager.instance.execCmd(strLine)
        }
      }
    } catch {
      case e: Exception => {
        LOG.error("", e)
      }
    } finally {
      socket.close;
    }
  }
}

object KamanjaManangerMonitorContext {
  val monitorCount = new java.util.concurrent.atomic.AtomicLong()
}

case class InitConfigs(val dataDataStoreInfo: String, val jarPaths: collection.immutable.Set[String], val zkConnectString: String,
                       val zkNodeBasePath: String, val zkSessionTimeoutMs: Int, val zkConnectionTimeoutMs: Int)

object KamanjaConfiguration {
  var configFile: String = _
  var allConfigs: Properties = _
  //  var metadataDataStoreInfo: String = _
  //  var dataDataStoreInfo: String = _
  //  var jarPaths: collection.immutable.Set[String] = _
  var nodeId: Int = _
  var clusterId: String = _
  var nodePort: Int = _
  //  var zkConnectString: String = _
  //  var zkNodeBasePath: String = _
  //  var zkSessionTimeoutMs: Int = _
  //  var zkConnectionTimeoutMs: Int = _

  //  var txnIdsRangeForNode: Int = 100000 // Each time get txnIdsRange of transaction ids for each Node
  //  var txnIdsRangeForPartition: Int = 10000 // Each time get txnIdsRange of transaction ids for each partition

  // Debugging info configs -- Begin
  var waitProcessingSteps = collection.immutable.Set[Int]()
  var waitProcessingTime = 0
  // Debugging info configs -- End

  var commitOffsetsMsgCnt = 0
  var commitOffsetsTimeInterval = 0

  var shutdown = false
  var participentsChangedCntr: Long = 0
  var baseLoader = new KamanjaLoaderInfo
  //  var adaptersAndEnvCtxtLoader = new KamanjaLoaderInfo(baseLoader, true, true)
  //  var metadataLoader = new KamanjaLoaderInfo(baseLoader, true, true)

  //  var adapterInfoCommitTime = 5000 // Default 5 secs

  def Reset: Unit = {
    configFile = null
    allConfigs = null
    //    metadataDataStoreInfo = null
    //    dataDataStoreInfo = null
    //    jarPaths = null
    nodeId = 0
    clusterId = null
    nodePort = 0
    //    zkConnectString = null
    //    zkNodeBasePath = null
    //    zkSessionTimeoutMs = 0
    //    zkConnectionTimeoutMs = 0

    // Debugging info configs -- Begin
    waitProcessingSteps = collection.immutable.Set[Int]()
    waitProcessingTime = 0
    // Debugging info configs -- End

    commitOffsetsMsgCnt = 0
    commitOffsetsTimeInterval = 0

    shutdown = false
    participentsChangedCntr = 0
  }
}

object ProcessedAdaptersInfo {
  private val LOG = LogManager.getLogger(getClass);
  private val lock = new Object
  private val instances = scala.collection.mutable.Map[Int, scala.collection.mutable.Map[String, String]]()
  private var prevAdapterCommittedValues = Map[String, String]()

  private def getAllValues: Map[String, String] = {
    var maps: List[scala.collection.mutable.Map[String, String]] = null
    lock.synchronized {
      maps = instances.map(kv => kv._2).toList
    }

    var retVals = scala.collection.mutable.Map[String, String]()
    maps.foreach(m => retVals ++= m)
    retVals.toMap
  }

  def getOneInstance(hashCode: Int, createIfNotExists: Boolean): scala.collection.mutable.Map[String, String] = {
    lock.synchronized {
      val inst = instances.getOrElse(hashCode, null)
      if (inst != null) {
        return inst
      }
      if (createIfNotExists == false)
        return null

      val newInst = scala.collection.mutable.Map[String, String]()
      instances(hashCode) = newInst
      return newInst
    }
  }

  def clearInstances: Unit = {
    var maps: List[scala.collection.mutable.Map[String, String]] = null
    lock.synchronized {
      instances.clear()
      prevAdapterCommittedValues
    }
  }

  //  def CommitAdapterValues: Boolean = {
  //    LOG.debug("CommitAdapterValues. AdapterCommitTime: " + KamanjaConfiguration.adapterInfoCommitTime)
  //    var committed = false
  //    if (KamanjaMetadata.envCtxt != null) {
  //      // Try to commit now
  //      var changedValues: List[(String, String)] = null
  //      val newValues = getAllValues
  //      if (prevAdapterCommittedValues.size == 0) {
  //        changedValues = newValues.toList
  //      } else {
  //        var changedArr = ArrayBuffer[(String, String)]()
  //        newValues.foreach(v1 => {
  //          val oldVal = prevAdapterCommittedValues.getOrElse(v1._1, null)
  //          if (oldVal == null || v1._2.equals(oldVal) == false) { // It is not found or changed, simple take it
  //            changedArr += v1
  //          }
  //        })
  //        changedValues = changedArr.toList
  //      }
  //      // Commit here
  //      try {
  //        if (changedValues.size > 0)
  //          KamanjaMetadata.envCtxt.setAdapterUniqKeyAndValues(changedValues)
  //        prevAdapterCommittedValues = newValues
  //        committed = true
  //      } catch {
  //        case e: Exception => {
  //          LOG.error("Failed to commit adapter changes. if we can not save this we will reprocess the information when service restarts.", e)
  //        }
  //        case e: Throwable => {
  //          LOG.error("Failed to commit adapter changes. if we can not save this we will reprocess the information when service restarts.", e)
  //        }
  //      }
  //
  //    }
  //    committed
  //  }
}

class KamanjaManager extends Observer {
  private val LOG = LogManager.getLogger(getClass);

  // KamanjaServer Object
  private var serviceObj: KamanjaServer = null

  private val inputAdapters = new ArrayBuffer[InputAdapter]
  private val outputAdapters = new ArrayBuffer[OutputAdapter]
  private val storageAdapters = new ArrayBuffer[DataStore]
  //  private val adapterChangedCntr = new java.util.concurrent.atomic.AtomicLong(0)
  private var adapterChangedCntr: Long = 0
  //  private val statusAdapters = new ArrayBuffer[OutputAdapter]
  //  private val validateInputAdapters = new ArrayBuffer[InputAdapter]

  private var thisEngineInfo: MainInfo = null
  private var adapterMetricInfo: scala.collection.mutable.MutableList[com.ligadata.HeartBeat.MonitorComponentInfo] = null
  //  private val failedEventsAdapters = new ArrayBuffer[OutputAdapter]

  private var metricsService: ExecutorService = scala.actors.threadpool.Executors.newFixedThreadPool(1)
  private var isTimerRunning = false
  private var isTimerStarted = false
  private type OptionMap = Map[Symbol, Any]

  private var outputAdaptersBindings = Array[(OutputAdapter, Array[AdapterMessageBinding])]()
  private var storageAdaptersBindings = Array[(DataStore, Array[AdapterMessageBinding])]()
  private var adapterChangedResolvedCntr: Long = -1
  private val adapterChangedResolveLock = new Object
  private var msgChangedCntr: Long = 0
  private var msg2TenantId = scala.collection.immutable.Map[String, String]()
  private var msg2TentIdResolvedCntr: Long = -1

  def incrAdapterChangedCntr(): Unit = {
    adapterChangedCntr += 1
  }

  def incrMsgChangedCntr(): Unit = {
    msgChangedCntr += 1
  }

  def getAdapterChangedCntr: Long = adapterChangedCntr

  def getMsgChangedCntr: Long = msgChangedCntr

  def getAllAdaptersInfo: (Array[InputAdapter], Array[OutputAdapter], Array[DataStore], Long) = {
    (inputAdapters.toArray, outputAdapters.toArray, storageAdapters.toArray, getAdapterChangedCntr)
  }

  def resolveAdapterBindins: (Array[(OutputAdapter, Array[AdapterMessageBinding])], Array[(DataStore, Array[AdapterMessageBinding])], Long) = {
    if (adapterChangedResolvedCntr == getAdapterChangedCntr)
      return (outputAdaptersBindings, storageAdaptersBindings, adapterChangedResolvedCntr)

    adapterChangedResolveLock.synchronized {
      if (adapterChangedResolvedCntr != getAdapterChangedCntr) {
        val (ins, outs, storages, cntr) = KamanjaManager.instance.getAllAdaptersInfo
        LOG.warn("AdapterChangedCntr. New adapterChngCntr:%d, Old adapterChangedCntr:%d".format(cntr, adapterChangedResolvedCntr))
        val mdMgr = GetMdMgr

        val newOuts = outs.map(out => {
          val ret = (out, mdMgr.BindingsForAdapter(out.inputConfig.Name).map(bind => bind._2).toArray)
          LOG.info("Out Adapter:%s, Bound Msgs:%s".format(out.inputConfig.Name, ret._2.map(b => b.messageName).mkString(",")))
          ret
        }).filter(adap => adap._2.size > 0)

        val newStorages = storages.map(storage => {
          val name = if (storage != null && storage.adapterInfo != null) storage.adapterInfo.Name else ""
          val ret = (storage, mdMgr.BindingsForAdapter(name).map(bind => bind._2).toArray)
          LOG.info("Storage Adapter:%s, Bound Msgs:%s".format(name, ret._2.map(b => b.messageName).mkString(",")))
          ret
        }).filter(adap => adap._2.size > 0)

        outputAdaptersBindings = newOuts
        storageAdaptersBindings = newStorages
        adapterChangedResolvedCntr = cntr
      }
    }

    (outputAdaptersBindings, storageAdaptersBindings, adapterChangedResolvedCntr)
  }

  def resolveMsg2TenantId: (scala.collection.immutable.Map[String, String], Long) = {
    if (msg2TentIdResolvedCntr == getMsgChangedCntr)
      return (msg2TenantId, msg2TentIdResolvedCntr)

    adapterChangedResolveLock.synchronized {
      if (msg2TentIdResolvedCntr != getMsgChangedCntr) {
        val tmpMsg2TenantId = scala.collection.mutable.Map[String, String]()
        val cntr = getMsgChangedCntr
        val msgDefs: Option[scala.collection.immutable.Set[MessageDef]] = mdMgr.Messages(true, true)
        if (msgDefs != None) {
          msgDefs.get.foreach(m => {
            tmpMsg2TenantId(m.FullName.toLowerCase()) = m.TenantId.toLowerCase()
          })
        }

        val containerDefs: Option[scala.collection.immutable.Set[ContainerDef]] = mdMgr.Containers(true, true)
        if (containerDefs != None) {
          containerDefs.get.foreach(c => {
            tmpMsg2TenantId(c.FullName.toLowerCase()) = c.TenantId.toLowerCase()
          })
        }
        msg2TenantId = tmpMsg2TenantId.toMap
        msg2TentIdResolvedCntr = cntr
      }
    }

    (msg2TenantId, msg2TentIdResolvedCntr)
  }

  private val execCtxts = ArrayBuffer[ExecContextImpl]()
  private var execCtxtsCommitPartitionOffsetPool: ExecutorService = null

  def AddExecContext(execCtxt: ExecContextImpl): Unit = synchronized {
    if (KamanjaMetadata.gNodeContext != null && !KamanjaMetadata.gNodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0) {
      if (LOG.isDebugEnabled()) LOG.debug("Adding execCtxt:" + execCtxt + " to commit offset after every " + KamanjaConfiguration.commitOffsetsTimeInterval + "ms")
      execCtxts += execCtxt
    }
  }

  def ClearExecContext(): Unit = synchronized {
    if (LOG.isDebugEnabled()) LOG.debug("Called ClearExecContext")
    execCtxts.clear
  }

  def GetEnvCtxts(): Array[ExecContextImpl] = {
    execCtxts.toArray
  }

  def RecreateExecCtxtsCommitPartitionOffsetPool(): Unit = synchronized {
    if (LOG.isDebugEnabled()) LOG.debug("Called RecreateExecCtxtsCommitPartitionOffsetPool")
    if (execCtxtsCommitPartitionOffsetPool != null) {
      execCtxtsCommitPartitionOffsetPool.shutdownNow()
      // Not really waiting for termination
    }

    execCtxtsCommitPartitionOffsetPool = scala.actors.threadpool.Executors.newFixedThreadPool(1)

    // We are checking for EnableEachTransactionCommit & KamanjaConfiguration.commitOffsetsTimeInterval to add thsi task
    if (KamanjaMetadata.gNodeContext != null && !KamanjaMetadata.gNodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0) {
      execCtxtsCommitPartitionOffsetPool.execute(new Runnable() {
        override def run() {
          val tp = execCtxtsCommitPartitionOffsetPool
          val commitOffsetsTimeInterval = KamanjaConfiguration.commitOffsetsTimeInterval
          while (!tp.isShutdown) {
            try {
              Thread.sleep(commitOffsetsTimeInterval + 1000) // Sleeping 1000ms more than given interval
            } catch {
              case e: Throwable => {}
            }
            if (KamanjaMetadata.gNodeContext != null && !KamanjaMetadata.gNodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0) {
              val envCtxts = GetEnvCtxts
              if (LOG.isDebugEnabled()) LOG.debug("Running CommitPartitionOffsetIfNeeded for " + envCtxts.length + " envCtxts")
              var idx = 0
              while (idx < envCtxts.length && !tp.isShutdown) {
                try {
                  envCtxts(idx).CommitPartitionOffsetIfNeeded
                } catch {
                  case e: Throwable => {
                    LOG.error("Failed to commit partitions offsets", e)
                  }
                }
                idx += 1
              }
            }
          }
        }
      })
    }
  }

  private def PrintUsage(): Unit = {
    LOG.warn("Available commands:")
    LOG.warn("    Quit")
    LOG.warn("    Help")
    LOG.warn("    --version")
    LOG.warn("    --config <configfilename>")
  }

  private def Shutdown(exitCode: Int): Int = {
    /*
    if (KamanjaMetadata.envCtxt != null)
      KamanjaMetadata.envCtxt.PersistRemainingStateEntriesOnLeader
*/
    if (execCtxtsCommitPartitionOffsetPool != null)
      execCtxtsCommitPartitionOffsetPool.shutdownNow()
    ClearExecContext
    KamanjaLeader.Shutdown
    KamanjaMetadata.Shutdown
    ShutdownAdapters
    PostMessageExecutionQueue.shutdown
    if (KamanjaMetadata.envCtxt != null)
      KamanjaMetadata.envCtxt.Shutdown
    if (serviceObj != null)
      serviceObj.shutdown
    com.ligadata.transactions.NodeLevelTransService.Shutdown
    return exitCode
  }

  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    def isSwitch(s: String) = (s(0) == '-')
    list match {
      case Nil => map
      case "--config" :: value :: tail =>
        nextOption(map ++ Map('config -> value), tail)
      case "--version" :: tail =>
        nextOption(map ++ Map('version -> "true"), tail)
      case option :: tail => {
        LOG.error("Unknown option " + option)
        throw new Exception("Unknown option " + option)
      }
    }
  }

  private def LoadDynamicJarsIfRequired(loadConfigs: Properties): Boolean = {
    val dynamicjars: String = loadConfigs.getProperty("dynamicjars".toLowerCase, "").trim

    if (dynamicjars != null && dynamicjars.length() > 0) {
      val jars = dynamicjars.split(",").map(_.trim).filter(_.length() > 0)
      if (jars.length > 0) {
        val qualJars = jars.map(j => Utils.GetValidJarFile(KamanjaMetadata.envCtxt.getJarPaths(), j))
        val nonExistsJars = Utils.CheckForNonExistanceJars(qualJars.toSet)
        if (nonExistsJars.size > 0) {
          LOG.error("Not found jars in given Dynamic Jars List : {" + nonExistsJars.mkString(", ") + "}")
          return false
        }
        return Utils.LoadJars(qualJars.toArray, KamanjaConfiguration.baseLoader.loadedJars, KamanjaConfiguration.baseLoader.loader)
      }
    }

    true
  }

  private def ShutdownAdapters: Boolean = {
    LOG.debug("Shutdown Adapters started @ " + Utils.GetCurDtTmStr)
    val s0 = System.nanoTime
    //
    //    validateInputAdapters.foreach(ia => {
    //      try {
    //        ia.Shutdown
    //      } catch {
    //        case fae: FatalAdapterException => {
    //          LOG.error("Validate adapter " + ia.UniqueName + "failed to shutdown", fae)
    //        }
    //        case e: Exception => {
    //          LOG.error("Validate adapter " + ia.UniqueName + "failed to shutdown", e)
    //        }
    //        case e: Throwable => {
    //          LOG.error("Validate adapter " + ia.UniqueName + "failed to shutdown", e)
    //        }
    //      }
    //    })
    //
    //    validateInputAdapters.clear

    inputAdapters.foreach(ia => {
      try {
        ia.Shutdown
      } catch {
        case fae: FatalAdapterException => {
          LOG.error("Input adapter " + ia.UniqueName + "failed to shutdown", fae)
        }
        case e: Exception => {
          LOG.error("Input adapter " + ia.UniqueName + "failed to shutdown", e)
        }
        case e: Throwable => {
          LOG.error("Input adapter " + ia.UniqueName + "failed to shutdown", e)
        }
      }
    })

    inputAdapters.clear

    outputAdapters.foreach(oa => {
      try {
        oa.Shutdown
      } catch {
        case fae: FatalAdapterException => {
          LOG.error("Output adapter failed to shutdown", fae)
        }
        case e: Exception => {
          LOG.error("Output adapter failed to shutdown", e)
        }
        case e: Throwable => {
          LOG.error("Output adapter failed to shutdown", e)
        }
      }
    })

    outputAdapters.clear

    storageAdapters.foreach(sa => {
      try {
        sa.Shutdown
      } catch {
        case fae: FatalAdapterException => {
          LOG.error("Storage adapter failed to shutdown", fae)
        }
        case e: Exception => {
          LOG.error("Storage adapter failed to shutdown", e)
        }
        case e: Throwable => {
          LOG.error("Storage adapter failed to shutdown", e)
        }
      }
    })

    storageAdapters.clear

    //
    //    statusAdapters.foreach(oa => {
    //      try {
    //        oa.Shutdown
    //      } catch {
    //        case fae: FatalAdapterException => {
    //          LOG.error("Status adapter failed to shutdown", fae)
    //        }
    //        case e: Exception => {
    //          LOG.error("Status adapter failed to shutdown", e)
    //        }
    //        case e: Throwable => {
    //          LOG.error("Status adapter failed to shutdown", e)
    //        }
    //      }
    //    })
    //
    //    statusAdapters.clear
    //
    //    failedEventsAdapters.foreach(oa => {
    //      try {
    //        oa.Shutdown
    //      } catch {
    //        case fae: FatalAdapterException => {
    //          LOG.error("FailedEvents adapter failed to shutdown, cause", fae)
    //        }
    //        case e: Exception => {
    //          LOG.error("FailedEvents adapter failed to shutdown, cause", e)
    //        }
    //        case e: Throwable => {
    //          LOG.error("FailedEvents adapter failed to shutdown", e)
    //        }
    //      }
    //    })
    //
    //    failedEventsAdapters.clear

    val totaltm = "TimeConsumed:%.02fms".format((System.nanoTime - s0) / 1000000.0);
    LOG.debug("Shutdown Adapters done @ " + Utils.GetCurDtTmStr + ". " + totaltm)

    true
  }

  private def initialize: Boolean = {
    var retval: Boolean = true

    val loadConfigs = KamanjaConfiguration.allConfigs

    try {
      KamanjaConfiguration.nodeId = loadConfigs.getProperty("nodeId".toLowerCase, "0").replace("\"", "").trim.toInt
      if (KamanjaConfiguration.nodeId <= 0) {
        LOG.error("Not found valid nodeId. It should be greater than 0")
        return false
      }

      try {
        val commitOffsetsMsgCnt = loadConfigs.getProperty("CommitOffsetsMsgCnt".toLowerCase, "0").replace("\"", "").trim.toInt
        if (commitOffsetsMsgCnt > 0) {
          KamanjaConfiguration.commitOffsetsMsgCnt = commitOffsetsMsgCnt
        }
      } catch {
        case e: Exception => {
          LOG.warn("", e)
        }
      }

      try {
        val commitOffsetsTmInterval = loadConfigs.getProperty("CommitOffsetsTimeInterval".toLowerCase, "0").replace("\"", "").trim.toInt
        if (commitOffsetsTmInterval > 0) {
          KamanjaConfiguration.commitOffsetsTimeInterval = commitOffsetsTmInterval
        }
      } catch {
        case e: Exception => {
          LOG.warn("", e)
        }
      }

      //      try {
      //        val adapterCommitTime = loadConfigs.getProperty("AdapterCommitTime".toLowerCase, "0").replace("\"", "").trim.toInt
      //        if (adapterCommitTime > 0) {
      //          KamanjaConfiguration.adapterInfoCommitTime = adapterCommitTime
      //        }
      //      } catch {
      //        case e: Exception => { LOG.warn("", e) }
      //      }

      try {
        KamanjaConfiguration.waitProcessingTime = loadConfigs.getProperty("waitProcessingTime".toLowerCase, "0").replace("\"", "").trim.toInt
        if (KamanjaConfiguration.waitProcessingTime > 0) {
          val setps = loadConfigs.getProperty("waitProcessingSteps".toLowerCase, "").replace("\"", "").split(",").map(_.trim).filter(_.length() > 0)
          if (setps.size > 0)
            KamanjaConfiguration.waitProcessingSteps = setps.map(_.toInt).toSet
        }
      } catch {
        case e: Exception => {
          LOG.warn("", e)
        }
      }

      LOG.debug("Initializing metadata bootstrap")
      KamanjaMetadata.InitBootstrap
      var intiConfigs: InitConfigs = null

      try {
        intiConfigs = KamanjaMdCfg.InitConfigInfo
      } catch {
        case e: Exception => {
          return false
        }
      }

      if (KamanjaConfiguration.commitOffsetsMsgCnt == 0) {
        try {
          val commitOffsetsMsgCntStr = GetMdMgr.GetUserProperty(KamanjaConfiguration.clusterId, "CommitOffsetsMsgCnt").replace("\"", "").trim
          if (!commitOffsetsMsgCntStr.isEmpty) {
            val commitOffsetsMsgCnt = commitOffsetsMsgCntStr.toInt
            if (commitOffsetsMsgCnt > 0)
              KamanjaConfiguration.commitOffsetsMsgCnt = commitOffsetsMsgCnt
          }
        } catch {
          case e: Exception => {
            LOG.warn("", e)
          }
        }
      }

      if (KamanjaConfiguration.commitOffsetsMsgCnt == 0) {
        try {
          val commitOffsetsMsgCntStr = GetMdMgr.GetUserProperty(KamanjaConfiguration.clusterId, "CommitOffsetsMsgCnt".toLowerCase).replace("\"", "").trim
          if (!commitOffsetsMsgCntStr.isEmpty) {
            val commitOffsetsMsgCnt = commitOffsetsMsgCntStr.toInt
            if (commitOffsetsMsgCnt > 0)
              KamanjaConfiguration.commitOffsetsMsgCnt = commitOffsetsMsgCnt
          }
        } catch {
          case e: Exception => {
            LOG.warn("", e)
          }
        }
      }

      if (KamanjaConfiguration.commitOffsetsTimeInterval == 0) {
        try {
          val commitOffsetsTimeIntervalStr = GetMdMgr.GetUserProperty(KamanjaConfiguration.clusterId, "CommitOffsetsTimeInterval").replace("\"", "").trim
          if (!commitOffsetsTimeIntervalStr.isEmpty) {
            val commitOffsetsTimeInterval = commitOffsetsTimeIntervalStr.toInt
            if (commitOffsetsTimeInterval > 0)
              KamanjaConfiguration.commitOffsetsTimeInterval = commitOffsetsTimeInterval
          }
        } catch {
          case e: Exception => {
            LOG.warn("", e)
          }
        }
      }

      if (KamanjaConfiguration.commitOffsetsTimeInterval == 0) {
        try {
          val commitOffsetsTimeIntervalStr = GetMdMgr.GetUserProperty(KamanjaConfiguration.clusterId, "CommitOffsetsTimeInterval".toLowerCase).replace("\"", "").trim
          if (!commitOffsetsTimeIntervalStr.isEmpty) {
            val commitOffsetsTimeInterval = commitOffsetsTimeIntervalStr.toInt
            if (commitOffsetsTimeInterval > 0)
              KamanjaConfiguration.commitOffsetsTimeInterval = commitOffsetsTimeInterval
          }
        } catch {
          case e: Exception => {
            LOG.warn("", e)
          }
        }
      }

      LOG.debug("Validating required jars")
      KamanjaMdCfg.ValidateAllRequiredJars(intiConfigs.jarPaths)
      LOG.debug("Load Environment Context")

      KamanjaMetadata.envCtxt = KamanjaMdCfg.LoadEnvCtxt(intiConfigs)
      if (KamanjaMetadata.envCtxt == null)
        return false

      val (zkConnectString, zkNodeBasePath, zkSessionTimeoutMs, zkConnectionTimeoutMs) = KamanjaMetadata.envCtxt.getZookeeperInfo

      var engineLeaderZkNodePath = ""
      var engineDistributionZkNodePath = ""
      var metadataUpdatesZkNodePath = ""
      var adaptersStatusPath = ""
      var dataChangeZkNodePath = ""
      var zkHeartBeatNodePath = ""

      if (zkNodeBasePath.size > 0) {
        engineLeaderZkNodePath = zkNodeBasePath + "/engineleader"
        engineDistributionZkNodePath = zkNodeBasePath + "/enginedistribution"
        metadataUpdatesZkNodePath = zkNodeBasePath + "/metadataupdate"
        adaptersStatusPath = zkNodeBasePath + "/adaptersstatus"
        dataChangeZkNodePath = zkNodeBasePath + "/datachange"
        zkHeartBeatNodePath = zkNodeBasePath + "/monitor/engine/" + KamanjaConfiguration.nodeId.toString
      }

      KamanjaMetadata.gNodeContext = new NodeContext(KamanjaMetadata.envCtxt)

      PostMessageExecutionQueue.init(KamanjaMetadata.gNodeContext)

      LOG.debug("Loading Adapters")
      // Loading Adapters (Do this after loading metadata manager & models & Dimensions (if we are loading them into memory))

      retval = KamanjaMdCfg.LoadAdapters(inputAdapters, outputAdapters, storageAdapters)

      if (retval) {
        LOG.debug("Initialize Metadata Manager")
        KamanjaMetadata.InitMdMgr(zkConnectString, metadataUpdatesZkNodePath, zkSessionTimeoutMs, zkConnectionTimeoutMs, inputAdapters, outputAdapters, storageAdapters)
        KamanjaMetadata.envCtxt.cacheContainers(KamanjaConfiguration.clusterId) // Load data for Caching
        LOG.debug("Initializing Leader")

        var txnCtxt: TransactionContext = null
        var txnId = KamanjaConfiguration.nodeId.toString.hashCode()
        if (txnId > 0)
          txnId = -1 * txnId
        // Finally we are taking -ve txnid for this
        try {
          txnCtxt = new TransactionContext(txnId, KamanjaMetadata.gNodeContext, Array[Byte](), EventOriginInfo("", ""), 0, null)
          ThreadLocalStorage.txnContextInfo.set(txnCtxt)

          val (tmpMdls, tMdlsChangedCntr) = KamanjaMetadata.getAllModels
          val tModels = if (tmpMdls != null) tmpMdls else Array[(String, MdlInfo)]()

          tModels.foreach(tup => {
            tup._2.mdl.init(txnCtxt)
          })
        } catch {
          case e: Exception => throw e
          case e: Throwable => throw e
        } finally {
          ThreadLocalStorage.txnContextInfo.remove
          if (txnCtxt != null) {
            KamanjaMetadata.gNodeContext.getEnvCtxt.rollbackData()
          }
        }

        KamanjaLeader.Init(KamanjaConfiguration.nodeId.toString, zkConnectString, engineLeaderZkNodePath, engineDistributionZkNodePath, adaptersStatusPath, inputAdapters, outputAdapters, storageAdapters, KamanjaMetadata.envCtxt, zkSessionTimeoutMs, zkConnectionTimeoutMs, dataChangeZkNodePath)
      }

      if (retval) {
        try {
          serviceObj = new KamanjaServer(KamanjaConfiguration.nodePort)
          (new Thread(serviceObj)).start()
        } catch {
          case e: Exception => {
            LOG.error("Failed to create server to accept connection on port:" + KamanjaConfiguration.nodePort, e)
            retval = false
          }
        }
        if (retval)
          RecreateExecCtxtsCommitPartitionOffsetPool()
      }
    } catch {
      case e: Exception => {
        LOG.error("Failed to initialize.", e)
        retval = false
      }
    } finally {

    }

    return retval
  }

  def execCmd(ln: String): Boolean = {
    if (ln.length() > 0) {
      val trmln = ln.trim
      if (trmln.length() > 0) {
        if (trmln.compareToIgnoreCase("Quit") == 0 || trmln.compareToIgnoreCase("Exit") == 0)
          return true
        if (trmln.compareToIgnoreCase("forceAdapterRebalance") == 0) {
          KamanjaLeader.forceAdapterRebalance
        } else if (trmln.compareToIgnoreCase("forceAdapterRebalanceAndSetEndOffsets") == 0) {
          KamanjaLeader.forceAdapterRebalanceAndSetEndOffsets
        }
      }
    }
    return false;
  }

  def update(o: Observable, arg: AnyRef): Unit = {
    val sig = arg.toString
    LOG.debug("Received signal: " + sig)
    if (sig.compareToIgnoreCase("SIGTERM") == 0 || sig.compareToIgnoreCase("SIGINT") == 0 || sig.compareToIgnoreCase("SIGABRT") == 0) {
      LOG.warn("Got " + sig + " signal. Shutting down the process")
      KamanjaConfiguration.shutdown = true
    }
  }

  private def run(args: Array[String]): Int = {
    KamanjaConfiguration.Reset
    KamanjaLeader.Reset
    if (args.length == 0) {
      PrintUsage()
      return Shutdown(1)
    }

    val options = nextOption(Map(), args.toList)
    val version = options.getOrElse('version, "false").toString
    if (version.equalsIgnoreCase("true")) {
      KamanjaVersion.print
      return Shutdown(0)
    }
    val cfgfile = options.getOrElse('config, null)
    if (cfgfile == null) {
      LOG.error("Need configuration file as parameter")
      return Shutdown(1)
    }

    KamanjaConfiguration.configFile = cfgfile.toString
    val (loadConfigs, failStr) = Utils.loadConfiguration(KamanjaConfiguration.configFile, true)
    if (failStr != null && failStr.size > 0) {
      LOG.error(failStr)
      return Shutdown(1)
    }
    if (loadConfigs == null) {
      return Shutdown(1)
    }

    KamanjaConfiguration.allConfigs = loadConfigs

    {
      // Printing all configuration
      LOG.info("Configurations:")
      val it = loadConfigs.entrySet().iterator()
      val lowercaseconfigs = new Properties()
      while (it.hasNext()) {
        val entry = it.next();
        LOG.info("\t" + entry.getKey().asInstanceOf[String] + " -> " + entry.getValue().asInstanceOf[String])
      }
      LOG.info("\n")
    }

    if (LoadDynamicJarsIfRequired(loadConfigs) == false) {
      return Shutdown(1)
    }

    if (initialize == false) {
      return Shutdown(1)
    }

    // Jars loaded, create the status factory
    //val statusEventFactory =  KamanjaMetadata.envCtxt.getContainerInstance("com.ligadata.KamanjaBase.KamanjaStatusEvent") //KamanjaMetadata.getMessgeInfo("com.ligadata.KamanjaBase.KamanjaStatusEvent").contmsgobj.asInstanceOf[MessageFactoryInterface]

    val exceptionStatusAdaps = scala.collection.mutable.Set[String]()
    var curCntr = 0
    val maxFailureCnt = 30

    val statusPrint_PD = new Runnable {
      def run(): Unit = {
        //var stats: scala.collection.immutable.Map[String, Long] = Map[String, Long]() // SimpleStats.copyMap
        var stats: ArrayBuffer[String] = new ArrayBuffer[String]()
        outputAdapters.foreach(x => {
          stats.append(x.getComponentSimpleStats)
        })
        inputAdapters.foreach(x => {
          stats.append(x.getComponentSimpleStats)
        })
        storageAdapters.foreach(x => {
          stats.append(x.getComponentSimpleStats)
        })
        val statsStr = stats.mkString("~")
        val statusMsg: com.ligadata.KamanjaBase.KamanjaStatusEvent = KamanjaMetadata.envCtxt.getContainerInstance("com.ligadata.KamanjaBase.KamanjaStatusEvent").asInstanceOf[KamanjaStatusEvent]
        statusMsg.statustype = "PD"
        statusMsg.nodeid = KamanjaConfiguration.nodeId.toString
        statusMsg.statusstring = statsStr
        statusMsg.eventtime = Utils.GetCurDtTmStrWithTZ // GetCurDtTmStr
        KamanjaMetadata.envCtxt.postMessages(Array[ContainerInterface](statusMsg))
      }
    }
    /*      val stats: scala.collection.immutable.Map[String, Long] = SimpleStats.copyMap
        val statsStr = stats.mkString("~")
        val dispStr = "PD,%d,%s,%s".format(KamanjaConfiguration.nodeId, Utils.GetCurDtTmStr, statsStr)
        var statusMsg = KamanjaMetadata.envCtxt.getContainerInstance("com.ligadata.KamanjaBase.KamanjaStatusEvent")
        statusMsg.nodeid = KamanjaConfiguration.nodeId.toString
        statusMsg.statusstring = statsStr
        //statusMsg.eventtime = Utils.GetCurDtTmStr

        if (statusAdapters != null) {
          curCntr += 1
          statusAdapters.foreach(sa => {
            val adapNm = sa.inputConfig.Name
            val alreadyFailed = (exceptionStatusAdaps.size > 0 && exceptionStatusAdaps.contains(adapNm))
            try {
              if (alreadyFailed == false || curCntr >= maxFailureCnt) {
                sa.send(dispStr, "1")
                if (alreadyFailed)
                  exceptionStatusAdaps -= adapNm
              }
            } catch {
              case fae: FatalAdapterException => {
                LOG.error("Failed to send data to status adapter:" + adapNm, fae)
                if (alreadyFailed == false)
                  exceptionStatusAdaps += adapNm
              }
              case e: Exception => {
                LOG.error("Failed to send data to status adapter:" + adapNm, e)
                if (alreadyFailed == false)
                  exceptionStatusAdaps += adapNm
              }
              case t: Throwable => {
                LOG.error("Failed to send data to status adapter:" + adapNm, t)
                if (alreadyFailed == false)
                  exceptionStatusAdaps += adapNm
              }
            }
          })
          if (curCntr >= maxFailureCnt)
            curCntr = 0
        } else {
          LOG.info(dispStr)
        } */

    val metricsCollector = new Runnable {
      def run(): Unit = {
        try {
          validateAndExternalizeMetrics
        } catch {
          case e: Throwable => {
            LOG.warn("KamanjaManager " + KamanjaConfiguration.nodeId.toString + " unable to externalize statistics due to internal error. Check ZK connection", e)
          }
        }
      }
    }
    var velocityMetricsOutput = new VelocityMetricsOutput

    VelocityMetricsInfo.getVMFactory(KamanjaMetadata.gNodeContext).addEmitListener(velocityMetricsOutput)

    /* inputAdapters.foreach(ia => {
      ia.VMFactory.addEmitListener(velocityMetricsOutput)
    })
    outputAdapters.foreach(oa => {
      oa.VMFactory.addEmitListener(velocityMetricsOutput)
    })*/

    val scheduledThreadPool = Executors.newScheduledThreadPool(3);

    scheduledThreadPool.scheduleWithFixedDelay(statusPrint_PD, 0, 1000, TimeUnit.MILLISECONDS);

    /**
     * print("=> ")
     * breakable {
     * for (ln <- io.Source.stdin.getLines) {
     * val rv = execCmd(ln)
     * if (rv)
     * break;
     * print("=> ")
     * }
     * }
     */

    var timeOutEndTime: Long = 0
    var participentsChangedCntr: Long = 0
    var lookingForDups = false
    var cntr: Long = 0
    var prevParticipents = ""

    val nodeNameToSetZk = KamanjaConfiguration.nodeId.toString

    var sh: SignalHandler = null
    try {
      sh = new SignalHandler()
      sh.addObserver(this)
      sh.handleSignal("TERM")
      sh.handleSignal("INT")
      sh.handleSignal("ABRT")
    } catch {
      case e: Throwable => {
        LOG.error("Failed to add signal handler.", e)
      }
    }

    //    var nextAdapterValuesCommit = System.currentTimeMillis + KamanjaConfiguration.adapterInfoCommitTime

    LOG.warn("KamanjaManager is running now. Waiting for user to terminate with SIGTERM, SIGINT or SIGABRT signals")
    while (KamanjaConfiguration.shutdown == false) {
      // Infinite wait for now
      //      if (KamanjaMetadata.envCtxt != null && nextAdapterValuesCommit < System.currentTimeMillis) {
      //        if (ProcessedAdaptersInfo.CommitAdapterValues)
      //          nextAdapterValuesCommit = System.currentTimeMillis + KamanjaConfiguration.adapterInfoCommitTime
      //      }
      cntr = cntr + 1
      if (participentsChangedCntr != KamanjaConfiguration.participentsChangedCntr) {
        val dispWarn = (lookingForDups && timeOutEndTime > 0)
        lookingForDups = false
        timeOutEndTime = 0
        participentsChangedCntr = KamanjaConfiguration.participentsChangedCntr
        val cs = KamanjaLeader.GetClusterStatus
        if (cs.leaderNodeId != null && cs.participantsNodeIds != null && cs.participantsNodeIds.size > 0) {
          if (dispWarn) {
            LOG.warn("Got new participents. Trying to see whether the node still has duplicates participents. Previous Participents:{%s} Current Participents:{%s}".format(prevParticipents, cs.participantsNodeIds.mkString(",")))
          }
          prevParticipents = ""
          val isNotLeader = (cs.isLeader == false || cs.leaderNodeId != cs.nodeId)
          if (isNotLeader) {
            val sameNodeIds = cs.participantsNodeIds.filter(p => p == cs.nodeId)
            if (sameNodeIds.size > 1) {
              lookingForDups = true
              val (zkConnectString, zkNodeBasePath, zkSessionTimeoutMs, zkConnectionTimeoutMs) = KamanjaMetadata.envCtxt.getZookeeperInfo
              var mxTm = if (zkSessionTimeoutMs > zkConnectionTimeoutMs) zkSessionTimeoutMs else zkConnectionTimeoutMs
              if (mxTm < 5000) // if the value is < 5secs, we are taking 5 secs
                mxTm = 5000
              timeOutEndTime = System.currentTimeMillis + mxTm + 2000 // waiting another 2secs
              LOG.error("Found more than one of NodeId:%s in Participents:{%s}. Waiting for %d milli seconds to check whether it is real duplicate or not.".format(cs.nodeId, cs.participantsNodeIds.mkString(","), mxTm))
              prevParticipents = cs.participantsNodeIds.mkString(",")
            }
          }
        }
      }

      if (lookingForDups && timeOutEndTime > 0) {
        if (timeOutEndTime < System.currentTimeMillis) {
          lookingForDups = false
          timeOutEndTime = 0
          val cs = KamanjaLeader.GetClusterStatus
          if (cs.leaderNodeId != null && cs.participantsNodeIds != null && cs.participantsNodeIds.size > 0) {
            val isNotLeader = (cs.isLeader == false || cs.leaderNodeId != cs.nodeId)
            if (isNotLeader) {
              val sameNodeIds = cs.participantsNodeIds.filter(p => p == cs.nodeId)
              if (sameNodeIds.size > 1) {
                LOG.error("Found more than one of NodeId:%s in Participents:{%s} for ever. Shutting down this node.".format(cs.nodeId, cs.participantsNodeIds.mkString(",")))
                KamanjaConfiguration.shutdown = true
              }
            }
          }
        }
      }

      try {
        Thread.sleep(500) // Waiting for 500 milli secs
      } catch {
        case e: Exception => {
          LOG.debug("", e)
        }
      }

      // See if we have to extenrnalize stats, every 5000ms..
      if (LOG.isTraceEnabled)
        LOG.trace("KamanjaManager " + KamanjaConfiguration.nodeId.toString + " running iteration " + cntr)

      if (!isTimerStarted) {
        scheduledThreadPool.scheduleWithFixedDelay(metricsCollector, 0, 5000, TimeUnit.MILLISECONDS);
        isTimerStarted = true
      }
    }
    inputAdapters.foreach(ia => {
      ia.VMFactory.shutdown()
    })
    outputAdapters.foreach(oa => {
      oa.VMFactory.shutdown()
    })

    scheduledThreadPool.shutdownNow()
    sh = null
    return Shutdown(0)
  }

  /**
   *
   */
  private def validateAndExternalizeMetrics: Unit = {
    val (zkConnectString, zkNodeBasePath, zkSessionTimeoutMs, zkConnectionTimeoutMs) = KamanjaMetadata.envCtxt.getZookeeperInfo
    val zkHeartBeatNodePath = zkNodeBasePath + "/monitor/engine/" + KamanjaConfiguration.nodeId.toString
    val isLogDebugEnabled = LOG.isDebugEnabled
    var isChangeApplicable = false

    if (isLogDebugEnabled)
      LOG.debug("KamanjaManager " + KamanjaConfiguration.nodeId.toString + " is externalizing metrics to " + zkNodeBasePath)

    // As part of the metrics externaization, look for changes to the configuration that can be affected
    // while the manager is running
    var changes: Array[(String, Any)] = KamanjaMetadata.getConfigChanges
    if (!changes.isEmpty) {
      changes.foreach(changes => {
        val cTokens = changes._1.split('.')
        if (cTokens.size == 3) {
          val tmp = processConfigChange(cTokens(0), cTokens(1), cTokens(2), changes._2)
          if (tmp)
            isChangeApplicable = tmp
        }
      })
    }
    if (isChangeApplicable) {
      // force Kamanja Mananger to take the changes
      KamanjaLeader.forceAdapterRebalance
      isChangeApplicable = false
    }

    if (thisEngineInfo == null) {
      thisEngineInfo = new MainInfo
      thisEngineInfo.startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
      thisEngineInfo.name = KamanjaConfiguration.nodeId.toString
      thisEngineInfo.uniqueId = MonitoringContext.monitorCount.incrementAndGet
      CreateClient.CreateNodeIfNotExists(zkConnectString, zkHeartBeatNodePath) // Creating the path if missing
    }

    thisEngineInfo.memory = memoryMetricsString(processmemoryUsage)

    thisEngineInfo.lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
    thisEngineInfo.uniqueId = MonitoringContext.monitorCount.incrementAndGet

    // run through all adapters.
    if (adapterMetricInfo == null) {
      adapterMetricInfo = scala.collection.mutable.MutableList[com.ligadata.HeartBeat.MonitorComponentInfo]()
    }
    adapterMetricInfo.clear

    inputAdapters.foreach(ad => {
      adapterMetricInfo += ad.getComponentStatusAndMetrics
    })
    outputAdapters.foreach(ad => {
      adapterMetricInfo += ad.getComponentStatusAndMetrics
    })
    storageAdapters.foreach(ad => {
      adapterMetricInfo += ad.getComponentStatusAndMetrics
    })

    // Combine all the junk into a single JSON String
    import org.json4s.JsonDSL._
    val allMetrics =
      ("Name" -> thisEngineInfo.name) ~
        ("Version" -> (KamanjaVersion.getMajorVersion.toString + "." + KamanjaVersion.getMinorVersion.toString + "." + KamanjaVersion.getMicroVersion + "." + KamanjaVersion.getBuildNumber)) ~
        ("UniqueId" -> thisEngineInfo.uniqueId) ~
        ("Metrics" -> thisEngineInfo.memory) ~
        ("LastSeen" -> thisEngineInfo.lastSeen) ~
        ("StartTime" -> thisEngineInfo.startTime) ~
        ("Components" -> adapterMetricInfo.map(mci =>
          ("Type" -> mci.typ) ~
            ("Name" -> mci.name) ~
            ("Description" -> mci.description) ~
            ("LastSeen" -> mci.lastSeen) ~
            ("StartTime" -> mci.startTime) ~
            ("Metrics" -> mci.metricsJsonString)))

    val statEvent: com.ligadata.KamanjaBase.KamanjaStatisticsEvent = KamanjaMetadata.envCtxt.getContainerInstance("com.ligadata.KamanjaBase.KamanjaStatisticsEvent").asInstanceOf[KamanjaStatisticsEvent]
    statEvent.statistics = compact(render(allMetrics))
    KamanjaMetadata.envCtxt.postMessages(Array[ContainerInterface](statEvent))
    // get the envContext.
    KamanjaLeader.SetNewDataToZkc(zkHeartBeatNodePath, compact(render(allMetrics)).getBytes)
    if (isLogDebugEnabled)
      LOG.debug("KamanjaManager " + KamanjaConfiguration.nodeId.toString + " externalized metrics for UID: " + thisEngineInfo.uniqueId)
  }

  private def processConfigChange(objType: String, action: String, objectName: String, adapter: Any): Boolean = {
    // For now we are only handling adapters.
    if (objType.equalsIgnoreCase("adapterdef")) {
      var cia: InputAdapter = null
      var coa: OutputAdapter = null
      var csa: DataStore = null

      // If this is an add - just call updateAdapter, he will figure out if its input or output
      if (action.equalsIgnoreCase("remove")) {
        // SetUpdatePartitionsFlag
        inputAdapters.foreach(ad => {
          if (ad.inputConfig.Name.equalsIgnoreCase(objectName)) ad.Shutdown
        })
        outputAdapters.foreach(ad => {
          if (ad.inputConfig.Name.equalsIgnoreCase(objectName)) ad.Shutdown
        })
        storageAdapters.foreach(ad => {
          if (ad != null && ad.adapterInfo != null && ad.adapterInfo.Name.equalsIgnoreCase(objectName)) ad.Shutdown
        })
      }

      // If this is an add - just call updateAdapter, he will figure out if its input or output
      if (action.equalsIgnoreCase("add")) {
        KamanjaMdCfg.updateAdapter(adapter.asInstanceOf[AdapterInfo], true, inputAdapters, outputAdapters, storageAdapters)
        return true
      }

      // Updating requires that we Stop the adapter first.
      if (action.equalsIgnoreCase("update")) {
        // SetUpdatePartitionsFlag
        inputAdapters.foreach(ad => {
          if (ad.inputConfig.Name.equalsIgnoreCase(objectName)) cia = ad
        })
        outputAdapters.foreach(ad => {
          if (ad.inputConfig.Name.equalsIgnoreCase(objectName)) coa = ad
        })
        storageAdapters.foreach(ad => {
          if (ad != null && ad.adapterInfo != null && ad.adapterInfo.Name.equalsIgnoreCase(objectName)) csa = ad
        })

        if (cia != null) {
          cia.Shutdown
          KamanjaMdCfg.updateAdapter(adapter.asInstanceOf[AdapterInfo], false, inputAdapters, outputAdapters, storageAdapters)
        }
        if (coa != null) {
          coa.Shutdown
          KamanjaMdCfg.updateAdapter(adapter.asInstanceOf[AdapterInfo], false, inputAdapters, outputAdapters, storageAdapters)
        }
        if (csa != null) {
          csa.Shutdown
          KamanjaMdCfg.updateAdapter(adapter.asInstanceOf[AdapterInfo], false, inputAdapters, outputAdapters, storageAdapters)
        }
        return true
      }

      if (action.equalsIgnoreCase("remove")) {
        // Implement this
      }

    }
    return false
  }

  private def processmemoryUsage(): Memory = {
    var mem: Memory = new Memory
    val mb = 1024 * 1024
    val runtime = Runtime.getRuntime
    mem.usedMemory = (runtime.totalMemory - runtime.freeMemory) / mb
    mem.totalMemory = runtime.totalMemory / mb
    mem.freeMemory = runtime.freeMemory / mb
    mem.maxMemory = runtime.maxMemory / mb

    LOG.info("** Used Memory:  " + (runtime.totalMemory - runtime.freeMemory) / mb + " MB")
    LOG.info("** Free Memory:  " + runtime.freeMemory / mb + " MB")
    LOG.info("** Total Memory: " + runtime.totalMemory / mb + " MB")
    LOG.info("** Max Memory:   " + runtime.maxMemory / mb + " MB")
    return mem
  }

  private def memoryMetricsString(mem: Memory): String = {
    if (mem == null) return "{\"UsedMemory\":\"0 MB\",\"FreeMomory\":\"0 MB\",\"TotalMemory\":\"0 MB\",\"MaxMemory\":\"0 MB\"}\", "
    return "{\"UsedMemory\":\"" + mem.usedMemory + " MB\",\"FreeMomory\":\"" + mem.freeMemory + " MB\",\"TotalMemory\":\"" + mem.totalMemory + " MB\",\"MaxMemory\":\"" + mem.maxMemory + " MB\"}\", "
  }
  private class SignalHandler extends Observable with sun.misc.SignalHandler {
    def handleSignal(signalName: String) {
      sun.misc.Signal.handle(new sun.misc.Signal(signalName), this)
    }

    def handle(signal: sun.misc.Signal) {
      setChanged()
      notifyObservers(signal)
    }
  }

}

class VelocityMetricsOutput extends VelocityMetricsCallback {
  private val LOG = LogManager.getLogger(getClass);
  @throws[Exception]
  def call(metrics: Metrics): Unit = {

    LOG.info("Start Velocity Metrics")
    val velocityMetricsArrBuf: ArrayBuffer[com.ligadata.KamanjaBase.KamanjaVelocityMetrics] = new ArrayBuffer[com.ligadata.KamanjaBase.KamanjaVelocityMetrics]

    val metricsArrBuf: ArrayBuffer[com.ligadata.KamanjaBase.MetricsValue] = new ArrayBuffer[com.ligadata.KamanjaBase.MetricsValue]
    val componentKeyMetricsBuf: ArrayBuffer[com.ligadata.KamanjaBase.ComponentKeyMetrics] = new ArrayBuffer[com.ligadata.KamanjaBase.ComponentKeyMetrics]

    val componentMetrics = metrics.compMetrics
    if (componentMetrics != null && componentMetrics.length > 0) {
      for (i <- 0 until componentMetrics.length) {
        //get uuid, get componentKey, get nodeid
        val compKeyMetrics = componentMetrics(i).keyMetrics

        if (compKeyMetrics != null && compKeyMetrics.length > 0) {
          for (j <- 0 until compKeyMetrics.length) {
            val metricsValues = compKeyMetrics(j).metricValues

            if (metricsValues != null && metricsValues.length > 0) {
              for (k <- 0 until metricsValues.length) {
                val MetricsInst = KamanjaMetadata.envCtxt.getContainerInstance("com.ligadata.KamanjaBase.MetricsValue")

                if (MetricsInst == null) {
                  LOG.warn("Not able to get com.ligadata.KamanjaBase.MetricsValue")
                } else {
                  val MetricsVal = MetricsInst.asInstanceOf[MetricsValue]

                  MetricsVal.metrickey = metricsValues(k).Key()
                  MetricsVal.metricskeyvalue = metricsValues(k).Value()
                  metricsArrBuf += MetricsVal
                }
              }
            }
            val ComponentInst = KamanjaMetadata.envCtxt.getContainerInstance("com.ligadata.KamanjaBase.ComponentKeyMetrics")
            if (ComponentInst == null) {
              LOG.warn("Not able to get com.ligadata.KamanjaBase.ComponentKeyMetrics")
            } else {
              val Component = ComponentInst.asInstanceOf[ComponentKeyMetrics]
              Component.key = compKeyMetrics(j).key
              Component.metricstime = compKeyMetrics(j).metricsTime
              Component.roundintervaltimeinsec = compKeyMetrics(j).roundIntervalTimeInSec
              Component.firstoccured = compKeyMetrics(j).firstOccured
              Component.lastoccured = compKeyMetrics(j).lastOccured
              Component.metricsvalue = metricsArrBuf.toArray
              componentKeyMetricsBuf += Component
              metricsArrBuf.clear()
            }
          }
        }
        val VelocityMetricsInst = KamanjaMetadata.envCtxt.getContainerInstance("com.ligadata.KamanjaBase.KamanjaVelocityMetrics")
        if (VelocityMetricsInst == null) {
          LOG.warn("Not able to get com.ligadata.KamanjaBase.VelocityMetrics")
        } else {
          val VelocityMetrics = VelocityMetricsInst.asInstanceOf[KamanjaVelocityMetrics]
          VelocityMetrics.uuid = metrics.uuid
          VelocityMetrics.componentkey = componentMetrics(i).componentKey
          VelocityMetrics.nodeid = componentMetrics(i).nodeId
          VelocityMetrics.componentkeymetrics = componentKeyMetricsBuf.toArray
          velocityMetricsArrBuf += VelocityMetrics
          componentKeyMetricsBuf.clear()
        }
      }
    }
    val isLogDebugEnabled = LOG.isDebugEnabled
    if (isLogDebugEnabled) {
      val vmArray = velocityMetricsArrBuf.toArray
      if (vmArray != null && vmArray.length > 0)
        for (i <- 0 until vmArray.length) {
          LOG.info("vmArray  " + vmArray(i).uuid)
          LOG.info("vmArray componentkey " + vmArray(i).componentkey)
          LOG.info("vmArray  nodeId " + vmArray(i).nodeid)
          val componentkey = vmArray(i).componentkeymetrics
          if (componentkey != null && componentkey.length > 0)
            for (j <- 0 until componentkey.length) {

              LOG.info("componentkey key " + componentkey(j).key)
              LOG.info("componentkey metricstime " + componentkey(j).metricstime)
              LOG.info("componentkey roundintervaltimeinsec " + componentkey(j).roundintervaltimeinsec)
              LOG.info("componentkey firstoccured " + componentkey(j).firstoccured)
              LOG.info("componentkey lastoccured " + componentkey(j).lastoccured)
              val metricsValue = componentkey(i).metricsvalue
              if (metricsValue != null && metricsValue.length > 0)
                for (k <- 0 until metricsValue.length) {
                  LOG.info("metricsValue key " + metricsValue(k).metrickey)
                  LOG.info("metricsValue metricstime " + metricsValue(k).metricskeyvalue)
                }
            }
        }
    }

    LOG.info("End Velocity Metrics")

    val vmstats = velocityMetricsArrBuf.toArray.asInstanceOf[Array[ContainerInterface]]
    KamanjaMetadata.envCtxt.postMessages(vmstats)

  }
}

class MainInfo {
  var name: String = null
  var uniqueId: Long = 0
  var lastSeen: String = null
  var startTime: String = null
  var memory: String = null
}

class Memory {
  var usedMemory: Long = 0
  var freeMemory: Long = 0
  var maxMemory: Long = 0
  var totalMemory: Long = 0
}

class ComponentInfo {
  var typ: String = null
  var name: String = null
  var desc: String = null
  var uniqueId: Long = 0
  var lastSeen: String = null
  var startTime: String = null
  var metrics: collection.mutable.Map[String, Any] = null
}

object KamanjaManager {
  private val LOG = LogManager.getLogger(getClass)
  private var km: KamanjaManager = _

  val instance: KamanjaManager = {
    if (km == null) {
      km = new KamanjaManager
    }
    km
  }

  def main(args: Array[String]): Unit = {
    scala.sys.addShutdownHook({
      if (KamanjaConfiguration.shutdown == false) {
        LOG.warn("KAMANJA-MANAGER: Received shutdown request")
        KamanjaConfiguration.shutdown = true // Setting the global shutdown
      }
    })
    val kmResult = instance.run(args)
    if (kmResult != 0) {
      LOG.error(s"KAMANJA-MANAGER: Kamanja shutdown with error code $kmResult")
    }
  }
}

