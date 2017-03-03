
/*
 * Copyright 2015 ligaDATA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ligadata.KamanjaManager

import com.ligadata.HeartBeat.MonitorComponentInfo
import com.ligadata.KamanjaBase._
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.KvBase.Key
import com.ligadata.StorageBase.DataStore
import com.ligadata.kamanja.metadata.{AdapterMessageBinding, ContainerDef, MessageDef}
import com.ligadata.kamanja.metadata.MdMgr._
import org.apache.logging.log4j.{LogManager, Logger}

//import org.json4s._
//import org.json4s.JsonDSL._
//import org.json4s.jackson.JsonMethods._
import scala.collection.mutable.ArrayBuffer
import com.ligadata.Exceptions.{FatalAdapterException, MessagePopulationException, StackTrace}
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ligadata.transactions._

import com.ligadata.transactions._
import scala.actors.threadpool.{ExecutorService}

// There are no locks at this moment. Make sure we don't call this with multiple threads for same object
class ExecContextImpl(val input: InputAdapter, val curPartitionKey: PartitionUniqueRecordKey, val nodeContext: NodeContext) extends ExecContext {
  private val LOG = LogManager.getLogger(getClass);
  private var eventsCntr: Long = 0
  private var lastTimeCommitOffsets: Long = System.currentTimeMillis
  private val nullEventOrigin = EventOriginInfo(null, null)
  private var lastEventOrigin = nullEventOrigin
  private val execCtxt_reent_lock = new ReentrantReadWriteLock(true)

  if (KamanjaManager.instance != null)
    KamanjaManager.instance.AddExecContext(this)

  private def ReadLock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.readLock().lock()
  }

  private def ReadUnlock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.readLock().unlock()
  }

  private def WriteLock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.writeLock().lock()
  }

  private def WriteUnlock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.writeLock().unlock()
  }

  def CommitPartitionOffsetIfNeeded(forceTime: Boolean): Unit = {
    if (lastEventOrigin != null && lastEventOrigin.key != null && lastEventOrigin.value != null && lastEventOrigin.key.trim.size > 0 && lastEventOrigin.value.trim.size > 0 && nodeContext != null && nodeContext.getEnvCtxt() != null && !nodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0 && (forceTime || ((lastTimeCommitOffsets + KamanjaConfiguration.commitOffsetsTimeInterval) <= System.currentTimeMillis))) {
      WriteLock(execCtxt_reent_lock)
      try {
        if (lastEventOrigin.key != null && lastEventOrigin.value != null && lastEventOrigin.key.trim.size > 0 && lastEventOrigin.value.trim.size > 0 && !nodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0 && (forceTime || ((lastTimeCommitOffsets + KamanjaConfiguration.commitOffsetsTimeInterval) <= System.currentTimeMillis))) {
          val prevTimeCommitOffsets = lastTimeCommitOffsets
          val prevEventsCntr = eventsCntr
          val prevEventOrigin = lastEventOrigin
          try {
            lastEventOrigin = nullEventOrigin
            lastTimeCommitOffsets = System.currentTimeMillis
            eventsCntr = 0
            nodeContext.getEnvCtxt().setAdapterUniqueKeyValue(prevEventOrigin.key, prevEventOrigin.value)
          } catch {
            case e: Throwable => {
              lastEventOrigin = prevEventOrigin
              lastTimeCommitOffsets = prevTimeCommitOffsets
              eventsCntr = prevEventsCntr
            }
          }
        }
      } finally {
        WriteUnlock(execCtxt_reent_lock)
      }
    }
  }

  private def MsgsInPostList: Set[String] = {
    if (nodeContext != null && nodeContext.getEnvCtxt() != null && nodeContext.getEnvCtxt()._mgr != null) {
      try {
        val postMsgsStr = nodeContext.getEnvCtxt()._mgr.GetUserProperty(KamanjaConfiguration.clusterId, "PostSystemMessageList").replace("\"", "").trim
        if (postMsgsStr != null && !postMsgsStr.isEmpty) {
          return postMsgsStr.split(",").map(s => s.trim.toLowerCase).filter(s => !s.isEmpty).toSet
        }
      } catch {
        case e: Exception => {
          LOG.warn("Failed to get PostSystemMessageList", e)
        }
      }
    }

    Set[String]()
  }

  private val msgsInPostList = MsgsInPostList

  private def IsMsgInPostList(msgName: String): Boolean = {
    msgsInPostList.contains(msgName.toLowerCase)
  }

  private val engine = new LearningEngine
  private var previousLoader: com.ligadata.Utils.KamanjaClassLoader = null
  private val canPostMessageEvent = IsMsgInPostList("com.ligadata.KamanjaBase.KamanjaMessageEvent")

  protected override def executeMessage(txnCtxt: TransactionContext): Unit = {
    try {
      val curLoader = txnCtxt.getNodeCtxt().getEnvCtxt().getMetadataLoader.loader // Protecting from changing it between below statements
      if (curLoader != null && previousLoader != curLoader) {
        // Checking for every messages and setting when changed. We can set for every message, but the problem is it tries to do SecurityManager check every time.
        Thread.currentThread().setContextClassLoader(curLoader);
        previousLoader = curLoader
      }
    } catch {
      case e: Throwable => {
        LOG.error("Failed to setContextClassLoader.", e)
        throw e
      }
    }

    try {
      engine.execute(txnCtxt)
    } catch {
      case e: Throwable => throw e
    }
  }

  protected override def commitData(txnCtxt: TransactionContext): Unit = {
    try {
      val (outputAdapters, storageAdapters, cntr) = KamanjaManager.instance.resolveAdapterBindins

      outputAdapters.foreach(adap => {
        try {
          val sendContainers = ArrayBuffer[ContainerInterface]();
          adap._2.foreach(bind => (bind, txnCtxt.getContainersOrConcepts(bind.messageName).foreach(orginAndmsg => {
            sendContainers += orginAndmsg._2.asInstanceOf[ContainerInterface];
          })))
          adap._1.send(txnCtxt, sendContainers.toArray)
        } catch {
          case e: Exception => {
            LOG.error("Failed to save data in output adapter:" + adap._1.getAdapterName, e)
          }
          case e: Throwable => {
            LOG.error("Failed to save data in output adapter:" + adap._1.getAdapterName, e)
          }
        }
      })

      storageAdapters.foreach(adap => {
        try {
          val sendContainers = ArrayBuffer[ContainerInterface]();
          adap._2.foreach(bind => (bind, txnCtxt.getContainersOrConcepts(bind.messageName).foreach(orginAndmsg => {
            sendContainers += orginAndmsg._2.asInstanceOf[ContainerInterface];
          })))
          adap._1.putContainers(txnCtxt, sendContainers.toArray)
        } catch {
          case e: Exception => {
            LOG.error("Failed to save data in storage adapter:" + adap._1.getAdapterName, e)
          }
          case e: Throwable => {
            LOG.error("Failed to save data in storage adapter:" + adap._1.getAdapterName, e)
          }
        }
      })

      val allData = txnCtxt.getAllContainersOrConcepts()

      val (msg2TenantId, cntr1) = KamanjaManager.instance.resolveMsg2TenantId

      val forceCommitVal = txnCtxt.getValue("forcecommit")
      val forceCommitFlag = forceCommitVal != null

      if (allData != null && nodeContext != null && nodeContext.getEnvCtxt() != null) {

        if (LOG.isDebugEnabled) {
          allData.foreach(containerAndData => {
            val nm = if (containerAndData != null && containerAndData._1 != null) containerAndData._1.toLowerCase() else ""
            var tenatId = if (containerAndData != null && containerAndData._2 != null && containerAndData._2.size > 0) msg2TenantId.getOrElse(containerAndData._2(0).container.getFullTypeName.toLowerCase(), "tenant1") else "tenant1"
            val sz = if (containerAndData != null && containerAndData._2 != null) containerAndData._2.size else 0
            val canPersist = if (containerAndData != null && containerAndData._2 != null && containerAndData._2.size > 0) containerAndData._2(0).container.asInstanceOf[ContainerInterface].CanPersist().toString else "false"
            LOG.debug("Save Data Before Filter: Container:%s with TenatId %s has %d values. canPersist:%s".format(nm, tenatId, sz, canPersist))
          })
        }

        // All containers data for Tenant
        val validDataToCommit = scala.collection.mutable.Map[String, ArrayBuffer[(String, Array[ContainerInterface])]]()
        allData.filter(containerAndData => (containerAndData != null && containerAndData._2 != null && containerAndData._2.size > 0 &&
          containerAndData._2(0).container.isInstanceOf[ContainerInterface] && containerAndData._2(0).container.asInstanceOf[ContainerInterface].CanPersist())).foreach(containerAndData => {
          val tenatId = if (containerAndData._2.size > 0) msg2TenantId.getOrElse(containerAndData._2(0).container.getFullTypeName.toLowerCase(), "") else ""
          val cData = containerAndData._2.filter(cInfo => (cInfo != null && cInfo.container.isInstanceOf[ContainerInterface])).map(cInfo => cInfo.container.asInstanceOf[ContainerInterface])
          if (cData.size > 0) {
            val modData =
              if (cData(0).hasPrimaryKey /* && cData(0).hasPartitionKey */ ) {
                cData.map(d => {
                  // Get RDD and replace the transactionId & rowNumber from old one
                  val partKey = d.PartitionKeyData().toList
                  val tmRange =
                    if (d.hasTimePartitionInfo)
                      com.ligadata.KvBase.TimeRange(d.getTimePartitionData, d.getTimePartitionData)
                    else
                      com.ligadata.KvBase.TimeRange(com.ligadata.KvBase.KvBaseDefalts.defaultTime, com.ligadata.KvBase.KvBaseDefalts.defaultTime)
                  val tmpData = nodeContext.getEnvCtxt.getRDD(tenatId.toLowerCase(), containerAndData._1.toLowerCase(), partKey, tmRange, null)
                  var oldData: ContainerInterface = null
                  if (tmpData != null && tmpData.size > 0) {
                    if (LOG.isTraceEnabled())
                      LOG.trace("Got RDD for TxnId:%d, RowNumber:%d, PartitionKey:%s and PrimaryKey:%s.".format(d.getTransactionId, d.getRowNumber, d.PartitionKeyData().mkString(","), d.PrimaryKeyData().mkString(",")))
                    val newPrimaryKey = d.getPrimaryKey
                    var mIdx = 0

                    while (oldData == null && newPrimaryKey.size > 0 && mIdx < tmpData.size) {
                      val m = tmpData(mIdx)
                      mIdx += 1
                      val oldPrimaryKey = m.getPrimaryKey
                      if (oldData == null && newPrimaryKey.size == oldPrimaryKey.size) {
                        var idx = 0
                        var cmp = 0
                        while (cmp == 0 && idx < newPrimaryKey.size) {
                          cmp = newPrimaryKey(idx).compareTo(oldPrimaryKey(idx))
                          idx += 1
                        }
                        if (cmp == 0 && idx >= newPrimaryKey.size)
                          oldData = m
                      }
                    }
                  } else {
                    if (LOG.isTraceEnabled())
                      LOG.trace("Did not get RDD for TxnId:%d, RowNumber:%d, PartitionKey:%s and PrimaryKey:%s.".format(d.getTransactionId, d.getRowNumber, d.PartitionKeyData().mkString(","), d.PrimaryKeyData().mkString(",")))
                  }
                  if (oldData != null) {
                    d.setRowNumber(oldData.getRowNumber)
                    d.setTransactionId(oldData.getTransactionId)
                    if (LOG.isTraceEnabled())
                      LOG.trace("TxnId:%d, RowNumber:%d, PartitionKey:%s and PrimaryKey:%s found.".format(d.getTransactionId, d.getRowNumber, d.PartitionKeyData().mkString(","), d.PrimaryKeyData().mkString(",")))
                  }
                  d
                })
              } else {
                cData
              }
            val arrContainersData = validDataToCommit.getOrElse(tenatId.toLowerCase(), ArrayBuffer[(String, Array[ContainerInterface])]())
            arrContainersData += ((containerAndData._1.toLowerCase(), modData))
            validDataToCommit(tenatId.toLowerCase()) = arrContainersData
          }
        })

        validDataToCommit.foreach(kv => {
          if (LOG.isDebugEnabled) {
            LOG.debug("Save Data AfterFilter: Tenant:%s has %d values.".format(kv._1, kv._2.size))
          }
          try {
            nodeContext.getEnvCtxt().commitData(kv._1, txnCtxt, kv._2.toArray, forceCommitFlag)
          } catch {
            case e: Exception => {
              LOG.error("Failed to commit data into primary datastore for tenantid:" + kv._1, e)
            }
            case e: Throwable => {
              LOG.error("Failed to commit data into primary datastore for tenantid:" + kv._1, e)
            }
          }
        })
      }

      if (txnCtxt != null && nodeContext != null && nodeContext.getEnvCtxt() != null) {
        if (txnCtxt.origin != null && txnCtxt.origin.key != null && txnCtxt.origin.value != null && txnCtxt.origin.key.trim.size > 0 && txnCtxt.origin.value.trim.size > 0) {
          eventsCntr += 1
          if (forceCommitFlag ||
            (KamanjaConfiguration.commitOffsetsMsgCnt > 0 && eventsCntr >= KamanjaConfiguration.commitOffsetsMsgCnt) ||
            (KamanjaConfiguration.commitOffsetsTimeInterval > 0 && ((lastTimeCommitOffsets + KamanjaConfiguration.commitOffsetsTimeInterval) <= System.currentTimeMillis)) ||
            nodeContext.getEnvCtxt().EnableEachTransactionCommit) {
            var prevTimeCommitOffsets = lastTimeCommitOffsets
            var prevEventsCntr = eventsCntr
            var prevEventOrigin = lastEventOrigin

            lastEventOrigin = nullEventOrigin
            eventsCntr = 0
            lastTimeCommitOffsets = System.currentTimeMillis

            try {
              if (!nodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0 && ((prevTimeCommitOffsets + KamanjaConfiguration.commitOffsetsTimeInterval) <= System.currentTimeMillis)) {
                // ReadLock
                WriteLock(execCtxt_reent_lock)
                try {
                  lastEventOrigin = nullEventOrigin
                  lastTimeCommitOffsets = System.currentTimeMillis
                  eventsCntr = 0
                  nodeContext.getEnvCtxt().setAdapterUniqueKeyValue(txnCtxt.origin.key, txnCtxt.origin.value)
                  prevTimeCommitOffsets = lastTimeCommitOffsets
                  prevEventsCntr = eventsCntr
                  prevEventOrigin = lastEventOrigin
                } catch {
                  case e: Throwable => {
                    lastEventOrigin = prevEventOrigin
                    lastTimeCommitOffsets = prevTimeCommitOffsets
                    eventsCntr = prevEventsCntr
                  }
                } finally {
                  WriteUnlock(execCtxt_reent_lock)
                }
              } else {
                lastEventOrigin = nullEventOrigin
                eventsCntr = 0
                lastTimeCommitOffsets = System.currentTimeMillis
                nodeContext.getEnvCtxt().setAdapterUniqueKeyValue(txnCtxt.origin.key, txnCtxt.origin.value)
              }
            } catch {
              case e: Exception => {
                lastEventOrigin = prevEventOrigin
                lastTimeCommitOffsets = prevTimeCommitOffsets
                eventsCntr = prevEventsCntr
                LOG.error("Failed to setAdapterUniqueKeyValue", e)
              }
              case e: Throwable => {
                lastEventOrigin = prevEventOrigin
                lastTimeCommitOffsets = prevTimeCommitOffsets
                eventsCntr = prevEventsCntr
                LOG.error("Failed to setAdapterUniqueKeyValue", e)
              }
            }
          } else {
            lastEventOrigin = EventOriginInfo(txnCtxt.origin.key, txnCtxt.origin.value)
          }
        }
        if (canPostMessageEvent && txnCtxt.getMessageEvent != null) {
          nodeContext.getEnvCtxt().postMessages(Array[ContainerInterface](txnCtxt.getMessageEvent))
        }
      }
      else {
        if (logger.isDebugEnabled) {
          val key = if (txnCtxt != null && txnCtxt.origin != null && txnCtxt.origin.key != null) txnCtxt.origin.key else ""
          val value = if (txnCtxt != null && txnCtxt.origin != null && txnCtxt.origin.value != null) txnCtxt.origin.value else ""
          logger.debug(s"Not Saving AdapterUniqKvData key:${key}, value:${value}. txnCtxt: ${txnCtxt}, nodeContext: ${nodeContext}")
        }
      }
    } catch {
      case e: Throwable => throw e
    }
  }
}

object ExecContextFactoryImpl extends ExecContextFactory {
  def CreateExecContext(input: InputAdapter, curPartitionKey: PartitionUniqueRecordKey, nodeContext: NodeContext): ExecContext = {
    new ExecContextImpl(input, curPartitionKey, nodeContext)
  }
}

object PostMessageExecutionQueue {
  private val LOG = LogManager.getLogger(getClass);
  //  private val engine = new LearningEngine
  private var previousLoader: com.ligadata.Utils.KamanjaClassLoader = null
  private var nodeContext: NodeContext = _
  //  private var transService: SimpleTransService = _
  private var isInit = false
  private val msgsQueue = scala.collection.mutable.Queue[ContainerInterface]()
  private val msgQLock = new Object()
  private var processMsgs: ExecutorService = null
  private var execCtxt: ExecContext = null
  private var isShutdown = false

  // Passing empty values
  private val emptyData = Array[Byte]()
  private val uk = ""
  private val uv = ""

  private def enQMsg(msg: ContainerInterface): Unit = {
    msgQLock.synchronized {
      msgsQueue += msg
    }
  }

  private def enQMsg(msg: Array[ContainerInterface]): Unit = {
    msgQLock.synchronized {
      msgsQueue ++= msg
    }
  }

  private def deQMsg: ContainerInterface = {
    if (msgsQueue.isEmpty) {
      return null
    }
    msgQLock.synchronized {
      if (msgsQueue.isEmpty) {
        return null
      }
      return msgsQueue.dequeue
    }
  }

  class PostMsgUniqRecKey extends PartitionUniqueRecordKey {
    val Type: String = "PostMessageExecutionQueue"

    override def Serialize: String = "PostMessageExecutionQueue"

    override def Deserialize(key: String): Unit = {}
  }

  class PostMsgIA(val nodeContext: NodeContext, val inputConfig: AdapterConfiguration) extends InputAdapter {
    override def UniqueName: String = "PostMessageExecutionQueue"

    override def Category = "Input"

    private val startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))

    def Shutdown: Unit = {}

    def StopProcessing: Unit = {}

    def StartProcessing(partitionInfo: Array[StartProcPartInfo], ignoreFirstMsg: Boolean): Unit = {}

    def GetAllPartitionUniqueRecordKey: Array[PartitionUniqueRecordKey] = Array[PartitionUniqueRecordKey]()

    def DeserializeKey(k: String): PartitionUniqueRecordKey = null

    def DeserializeValue(v: String): PartitionUniqueRecordValue = null

    def getAllPartitionBeginValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]()

    def getAllPartitionEndValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]()

    override def getComponentStatusAndMetrics: MonitorComponentInfo = {
      val lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
      MonitorComponentInfo("PostMessageExecutionQueue", "PostMessageExecutionQueue", "PostMessageExecutionQueue", startTime, lastSeen, "")
    }

    override def getComponentSimpleStats: String = ""
  }

  def init(nodeCtxt: NodeContext): Unit = {
    val ac = new AdapterConfiguration
    ac.Name = "PostMessageExecutionQueue"
    ac.tenantId = "System"
    val input: InputAdapter = new PostMsgIA(nodeCtxt, ac)
    val curPartitionKey: PartitionUniqueRecordKey = new PostMsgUniqRecKey
    execCtxt = ExecContextFactoryImpl.CreateExecContext(input, curPartitionKey, nodeCtxt)
    nodeContext = nodeCtxt
    processMsgs = scala.actors.threadpool.Executors.newFixedThreadPool(1)
    isShutdown = false

    processMsgs.execute(new Runnable() {
      val emptyStrBytes = "".getBytes()

      override def run() = {
        while (!isShutdown && processMsgs != null && processMsgs.isShutdown == false) {
          val msg = deQMsg
          if (msg != null) {
            execCtxt.execute(msg, emptyStrBytes, null, null, System.currentTimeMillis)
          }
          else {
            // If no messages found in the queue, simply sleep for sometime
            try {
              if (!isShutdown)
                Thread.sleep(100) // Sleeping for 100ms
            } catch {
              case e: Throwable => {
                // Not yet handled this
              }
            }
          }
        }
      }
    })

    nodeContext.getEnvCtxt().postMessagesListener(postMsgListenerCallback)

    isInit = true
  }

  def postMsgListenerCallback(msgs: Array[ContainerInterface]): Unit = {
    if (msgs == null || msgs.size == 0) return
    if (isInit == false) {
      throw new Exception("PostMessageExecutionQueue is not yet initialized")
    }
    enQMsg(msgs)
  }

  def shutdown(): Unit = {
    isShutdown = true
    if (processMsgs != null)
      processMsgs.shutdownNow()
    // processMsgs = null, for now not setting to null. Anyway it will reset new executor when it reinitialized in case if it does
    //BUGBUG:: Instead of stutdown now we can call shutdown and wait for termination to shutdown the thread(s)
    //FIXME:: Instead of stutdown now we can call shutdown and wait for termination to shutdown the thread(s)
  }
}
