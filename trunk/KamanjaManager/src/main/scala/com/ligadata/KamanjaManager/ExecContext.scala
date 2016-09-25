
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
class ExecContextImpl(val input: InputAdapter, val nodeContext: NodeContext) extends ExecContext {
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

  def CommitPartitionOffsetIfNeeded: Unit = {
    if (false && lastEventOrigin.key != null && lastEventOrigin.value != null && lastEventOrigin.key.trim.size > 0 && lastEventOrigin.value.trim.size > 0 && !nodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0 && ((lastTimeCommitOffsets + KamanjaConfiguration.commitOffsetsTimeInterval) <= System.currentTimeMillis)) {
      WriteLock(execCtxt_reent_lock)
      try {
        if (lastEventOrigin.key != null && lastEventOrigin.value != null && lastEventOrigin.key.trim.size > 0 && lastEventOrigin.value.trim.size > 0 && !nodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0 && ((lastTimeCommitOffsets + KamanjaConfiguration.commitOffsetsTimeInterval) <= System.currentTimeMillis)) {
          val prevTimeCommitOffsets = lastTimeCommitOffsets
          val prevEventsCntr = eventsCntr
          val prevEventOrigin = lastEventOrigin
          try {
            lastEventOrigin = nullEventOrigin
            lastTimeCommitOffsets = System.currentTimeMillis
            eventsCntr = 0
            nodeContext.getEnvCtxt().setAdapterUniqueKeyValue(lastEventOrigin.key, lastEventOrigin.value)
          } catch {
            case e: Throwable => {
              lastEventOrigin = lastEventOrigin
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

  private val engine = new LearningEngine
  private var previousLoader: com.ligadata.Utils.KamanjaClassLoader = null

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
    throw new Exception("Not implemented")
  }
}

object ExecContextFactoryImpl extends ExecContextFactory {
  def CreateExecContext(input: InputAdapter, nodeContext: NodeContext): ExecContext = {
    new ExecContextImpl(input, nodeContext)
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

    //def StartProcessing(partitionInfo: Array[StartProcPartInfo], ignoreFirstMsg: Boolean): Unit = {}
    
    def StartProcessing(partitionInfo: Array[ThreadPartitions], ignoreFirstMsg: Boolean): Unit = {}

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
    execCtxt = ExecContextFactoryImpl.CreateExecContext(input, nodeCtxt)
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
