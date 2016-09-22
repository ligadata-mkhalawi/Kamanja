
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

import java.io.{ByteArrayOutputStream, DataOutputStream}
import java.util.concurrent.locks.ReentrantReadWriteLock

import com.ligadata.KamanjaBase._
import com.ligadata.cache.{CacheQueue, CacheQueueElement}
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.Json
import org.json4s.jackson.JsonMethods._

import scala.actors.threadpool.ExecutorService

// import com.ligadata.Utils.Utils
// import java.util.Map
import com.ligadata.utils.dag.{ReadyNode, EdgeId, DagRuntime}
import org.apache.logging.log4j.{Logger, LogManager}

//import java.io.{PrintWriter, File}
//import scala.xml.XML
//import scala.xml.Elem
import scala.collection.mutable.ArrayBuffer

//import org.json4s._
//import org.json4s.JsonDSL._
//import org.json4s.jackson.JsonMethods._
import com.ligadata.InputOutputAdapterInfo.{ExecContext, InputAdapter, PartitionUniqueRecordKey, PartitionUniqueRecordValue}
import com.ligadata.Exceptions.{KamanjaException, StackTrace, MessagePopulationException}

object LeanringEngine {
  // There are 3 types of error that we can create an ExceptionMessage for
  //  val invalidMessage: String = "Invalid_message"
  //  val invalidResult: String = "Invalid_result"
  val modelExecutionException: String = "Model_Excecution_Exception"

  val engineComponent: String = "Kamanja_Manager"

  private val LOG = LogManager.getLogger(getClass);

  def hasValidOrigin(txnCtxt: TransactionContext): Boolean = {
    return (txnCtxt.origin != null && txnCtxt.origin.key != null && txnCtxt.origin.value != null && txnCtxt.origin.key.length > 0 && txnCtxt.origin.value.length > 0)
  }

  // This returns Partition Key String & BucketId
  def GetQueuePartitionInfo(nodeIdModlsObj: scala.collection.mutable.Map[Long, (MdlInfo, ModelInstance)],
                            execNode: ReadyNode, txnCtxt: TransactionContext, partitionIdForNoKey: Int): (String, Int) = {

    val execMdl = nodeIdModlsObj.getOrElse(execNode.nodeId, null)
    if (execMdl != null) {
      if (LOG.isDebugEnabled)
        LOG.debug("LearningEngine:Executing Model:%s,NodeId:%d, iesPos:%d".format(execMdl._1.mdl.getModelName(), execNode.nodeId, execNode.iesPos))

      val allPartitionKeysWithDelim = ArrayBuffer[String]()
      var foundPartitionKey = false

      execMdl._1.inputs(execNode.iesPos).foreach(eid => {
        val tmpElem = KamanjaMetadata.getMdMgr.ElementForElementId(eid.edgeTypeId)
        var partKeyStr = ""

        if (tmpElem != None) {
          val lst =
            if (eid.nodeId > 0) {
              val origin =
                if (eid.nodeId > 0) {
                  val mdlObj = nodeIdModlsObj.getOrElse(eid.nodeId, null)
                  if (mdlObj != null) {
                    mdlObj._1.mdl.getModelName()
                  } else {
                    LOG.debug("Not found any node for eid.nodeId:" + eid.nodeId)
                    ""
                  }
                } else {
                  LOG.debug("Origin nodeid is not valid")
                  ""
                }
              LOG.debug("Input message:" + tmpElem.get.FullName)
              txnCtxt.getContainersOrConcepts(origin, tmpElem.get.FullName)
            } else {
              LOG.debug("Input message:" + tmpElem.get.FullName)
              txnCtxt.getContainersOrConcepts(tmpElem.get.FullName)
            }

          if (lst != null && lst.size > 0) {
            partKeyStr = lst(0)._2.asInstanceOf[MessageContainerBase].getPartitionKey.mkString(",")
            lst(0)._2
          } else {
            LOG.warn("Not found any message for Msg:" + tmpElem.get.FullName)
            null
          }
        }
        if (partKeyStr.length > 0)
          foundPartitionKey = true
        allPartitionKeysWithDelim += partKeyStr
      })

      if (!foundPartitionKey) return ("", partitionIdForNoKey)

      // Decide the Queue bucket number  .here after collecting partition keys
      val partKeyStr = allPartitionKeysWithDelim.mkString("-")
      val partitionIdx = Math.abs(partKeyStr.hashCode % KamanjaConfiguration.totalPartitionCount)

      return (partKeyStr, partitionIdx)
    }
    return ("", partitionIdForNoKey)
  }
}

class KamanjaCacheQueueEntry(val exeQueue: ArrayBuffer[ReadyNode], var execPos: Int, val dagRuntime: DagRuntime, val txnCtxt: TransactionContext, val thisMsgEvent: KamanjaMessageEvent, val modelsForMessage: ArrayBuffer[KamanjaModelEvent], val msgProcessingStartTime: Long) extends CacheQueueElement {
  override def serialize(): Array[Byte] = {
    KamanjaCacheQueueEntry.serialize(this)
  }
}

object KamanjaCacheQueueEntry {
  final def serialize(cacheEntry: KamanjaCacheQueueEntry): Array[Byte] = {
    // BUGBUG:: Finish rest of the serialization
    // throw new NotImplementedError("KamanjaCacheQueueEntry.serialize is not yet implemented")
    val linkBuf = CacheQueue.linkValueToSerializeCacheQueueElementInfo(cacheEntry.link)
/*
    val bos: ByteArrayOutputStream = new ByteArrayOutputStream(1024 * 1024)
    val dos = new DataOutputStream(bos)
    try {
      dos.writeInt(cacheEntry.exeQueue.size)
      cacheEntry.exeQueue.foreach(rn => {
        dos.writeInt(rn.iesPos)
        dos.writeLong(rn.nodeId)
      })
      dos.writeInt(cacheEntry.execPos)

      val dagRuntime: DagRuntime, val txnCtxt: TransactionContext, val thisMsgEvent: KamanjaMessageEvent, val modelsForMessage: ArrayBuffer[KamanjaModelEvent], val msgProcessingStartTime: Long

      dos.writeUTF(inst.Version)
      dos.writeUTF(inst.getClass.getName)
      inst.Serialize(dos)
      val arr = bos.toByteArray
      dos.close
      bos.close
      return arr

    } catch {
      case e: Exception => {
        //LOG.error("Failed to get classname :" + clsName, e)
        logger.debug("Failed to Serialize", e)
        dos.close
        bos.close
        throw e
      }
    }
    null
*/
    linkBuf
  }

  final def deserialize(buf: Array[Byte]): KamanjaCacheQueueEntry = {
    // BUGBUG:: Finish rest of the deserialization
    // throw new NotImplementedError("KamanjaCacheQueueEntry.deserialize is not yet implemented")
    val ent = new KamanjaCacheQueueEntry(null, 0, null, null, null, null, 0)
    ent.link = CacheQueue.extractLinkValueFromSerializedCacheQueueElementInfo(buf);
    ent
  }
}

// BUGBUG:: Do we need to get nodeIdModlsObj from KamanjaCacheQueueEntry?????
class LeanringEngineRemoteExecution(val threadId: Short, val startPartitionId: Int, val endPartitionId: Int) extends Runnable {
  private var waitTimeForNoMdlsExecInMs = 1
  private val LOG = LogManager.getLogger(getClass);
  private var mdlsChangedCntr: Long = -1
  private var nodeIdModlsObj = scala.collection.mutable.Map[Long, (MdlInfo, ModelInstance)]()

  def executeModel(dqKamanjaCacheQueueEntry: KamanjaCacheQueueEntry): Unit = {
    if (dqKamanjaCacheQueueEntry == null)
      return
    val mdlChngCntr = KamanjaMetadata.GetModelsChangedCounter
    if (mdlChngCntr != mdlsChangedCntr) {
      val (newDag, tmpNodeIdModlsObj, tMdlsChangedCntr) = KamanjaMetadata.getExecutionDag
      mdlsChangedCntr = tMdlsChangedCntr
      val newNodeIdModlsObj = scala.collection.mutable.Map[Long, (MdlInfo, ModelInstance)]()
      tmpNodeIdModlsObj.foreach(info => {
        val oldMdlInfo = nodeIdModlsObj.getOrElse(info._1, null)
        if (oldMdlInfo != null && info._2.mdl.getModelDef().UniqId == oldMdlInfo._1.mdl.getModelDef().UniqId) {
          // If it is same version and same factory
          newNodeIdModlsObj(info._1) = (info._2, oldMdlInfo._2)
        } else {
          newNodeIdModlsObj(info._1) = (info._2, null)
        }
      })
      nodeIdModlsObj = newNodeIdModlsObj
    }

    val execNode = dqKamanjaCacheQueueEntry.exeQueue(dqKamanjaCacheQueueEntry.execPos)
    dqKamanjaCacheQueueEntry.execPos += 1

    val execMdl = nodeIdModlsObj.getOrElse(execNode.nodeId, null)
    if (execMdl != null) {
      if (LOG.isDebugEnabled)
        LOG.debug("LearningEngine:Executing Model:%s,NodeId:%d, iesPos:%d".format(execMdl._1.mdl.getModelName(), execNode.nodeId, execNode.iesPos))
      val curMd =
        if (execMdl._1.mdl.isModelInstanceReusable()) {
          if (execMdl._2 == null) {
            // First time initialize this
            val tInst = execMdl._1.mdl.createModelInstance()
            tInst.init(dqKamanjaCacheQueueEntry.txnCtxt.origin.key)
            nodeIdModlsObj(execNode.nodeId) = (execMdl._1, tInst)
            tInst
          } else {
            execMdl._2
          }
        } else {
          // Not reusable instance. So, create it every time
          val tInst = execMdl._1.mdl.createModelInstance()
          tInst.init(dqKamanjaCacheQueueEntry.txnCtxt.origin.key)
          tInst
        }

      if (curMd != null) {
        var modelEvent: KamanjaModelEvent = if (dqKamanjaCacheQueueEntry.thisMsgEvent != null) {
          val tmpMdlEvent: KamanjaModelEvent = dqKamanjaCacheQueueEntry.txnCtxt.getNodeCtxt.getEnvCtxt.getContainerInstance("com.ligadata.KamanjaBase.KamanjaModelEvent").asInstanceOf[KamanjaModelEvent]
          if (tmpMdlEvent == null) {
            LOG.warn("Not able to get com.ligadata.KamanjaBase.KamanjaModelEvent")
          }
          tmpMdlEvent
        } else {
          val tmpMdlEvent: KamanjaModelEvent = null
          tmpMdlEvent
        }
        if (modelEvent != null) {
          modelEvent.isresultproduced = false
          modelEvent.error = ""
          modelEvent.producedmessages = Array[Long]()
        }
        val modelStartTime = System.nanoTime

        try {
          val execMsgsSet: Array[ContainerOrConcept] = execMdl._1.inputs(execNode.iesPos).map(eid => {
            if (LOG.isDebugEnabled)
              LOG.debug("MsgInfo: nodeId:" + eid.nodeId + ", edgeTypeId:" + eid.edgeTypeId)
            val tmpElem = KamanjaMetadata.getMdMgr.ElementForElementId(eid.edgeTypeId)

            val finalEntry =
              if (tmpElem != None) {
                val lst =
                  if (eid.nodeId > 0) {
                    val origin =
                      if (eid.nodeId > 0) {
                        val mdlObj = nodeIdModlsObj.getOrElse(eid.nodeId, null)
                        if (mdlObj != null) {
                          mdlObj._1.mdl.getModelName()
                        } else {
                          LOG.debug("Not found any node for eid.nodeId:" + eid.nodeId)
                          ""
                        }
                      } else {
                        LOG.debug("Origin nodeid is not valid")
                        ""
                      }
                    LOG.debug("Input message:" + tmpElem.get.FullName)
                    dqKamanjaCacheQueueEntry.txnCtxt.getContainersOrConcepts(origin, tmpElem.get.FullName)
                  } else {
                    LOG.debug("Input message:" + tmpElem.get.FullName)
                    dqKamanjaCacheQueueEntry.txnCtxt.getContainersOrConcepts(tmpElem.get.FullName)
                  }

                if (lst != null && lst.size > 0) {
                  lst(0)._2
                } else {
                  LOG.warn("Not found any message for Msg:" + tmpElem.get.FullName)
                  null
                }
              } else {
                LOG.warn("Not found any message for EdgeTypeId:" + eid.edgeTypeId)
                null
              }
            finalEntry
          })

          val outputDefault: Boolean = false;
          val res = curMd.execute(dqKamanjaCacheQueueEntry.txnCtxt, execMsgsSet, execNode.iesPos, outputDefault)
          if (modelEvent != null)
            modelEvent.consumedmessages = execMsgsSet.map(msg => KamanjaMetadata.getMdMgr.ElementIdForSchemaId(msg.asInstanceOf[ContainerInterface].getSchemaId))
          if (res != null && res.size > 0) {
            if (modelEvent != null) {
              modelEvent.isresultproduced = true
              modelEvent.producedmessages = res.map(msg => KamanjaMetadata.getMdMgr.ElementIdForSchemaId(msg.asInstanceOf[ContainerInterface].getSchemaId))
            }
            dqKamanjaCacheQueueEntry.txnCtxt.addContainerOrConcepts(execMdl._1.mdl.getModelName(), res)
            val newEges = res.map(msg => EdgeId(execMdl._1.nodeId, KamanjaMetadata.getMdMgr.ElementIdForSchemaId(msg.asInstanceOf[ContainerInterface].getSchemaId)))
            val readyNodes = dqKamanjaCacheQueueEntry.dagRuntime.FireEdges(newEges)
            dqKamanjaCacheQueueEntry.exeQueue ++= readyNodes

            if (LOG.isDebugEnabled) {
              val inputMsgs = execMsgsSet.map(msg => msg.getFullTypeName).mkString(",")
              val outputMsgs = res.map(msg => msg.getFullTypeName).mkString(",")
              val inputEges = newEges.map(edge => "(NodeId:%d,edgeTypeId:%d)".format(edge.nodeId, edge.edgeTypeId)).mkString(",")
              val producedNodeIds = if (readyNodes != null) readyNodes.map(nd => "(NodeId:%d,iesPos:%d)".format(nd.nodeId, nd.iesPos)).mkString(",") else ""
              val msg = "LearningEngine:Executed Model:%s with NodeId:%d Using messages:%s, which produced %s (with Edges:%s). Adding those message into DAG produced:%s".format(execMdl._1.mdl.getModelName(), execNode.nodeId, inputMsgs, outputMsgs, inputEges, producedNodeIds)
              LOG.debug(msg)
            }
          } else {
            if (LOG.isDebugEnabled) {
              val inputMsgs = execMsgsSet.map(msg => msg.getFullTypeName).mkString(",")
              val msg = "LearningEngine:Executed Model:%s with NodeId:%d Using messages:%s, which did not produce any result".format(execMdl._1.mdl.getModelName(), execNode.nodeId, inputMsgs)
              LOG.debug(msg)
            }
          }
        } catch {
          case e: Throwable => {
            if (modelEvent != null) {
              modelEvent.error = StackTrace.ThrowableTraceString(e)
            }
            LOG.error("Failed to execute model:" + execMdl._1.mdl.getModelName(), e)
          }
        }

        if (modelEvent != null) {
          // Model finished executing, add the stats to the modeleventmsg
          //var mdlDefs = KamanjaMetadata.getMdMgr.Models(md.mdl.getModelDef().FullName,true, false).getOrElse(null)
          modelEvent.modelid = execNode.nodeId
          modelEvent.eventepochtime = System.currentTimeMillis()
          modelEvent.elapsedtimeinms = ((System.nanoTime - modelStartTime) / 1000000.0).toFloat
          dqKamanjaCacheQueueEntry.modelsForMessage.append(modelEvent)
        }
      } else {
        val errorTxt = "Failed to create model " + execMdl._1.mdl.getModelName()
        LOG.error(errorTxt)
        if (dqKamanjaCacheQueueEntry.thisMsgEvent != null) {
          dqKamanjaCacheQueueEntry.thisMsgEvent.eventepochtime = System.currentTimeMillis()
          dqKamanjaCacheQueueEntry.thisMsgEvent.error = "Failed to create model " + execMdl._1.mdl.getModelName()
          dqKamanjaCacheQueueEntry.thisMsgEvent.elapsedtimeinms = ((System.nanoTime - dqKamanjaCacheQueueEntry.msgProcessingStartTime) / 1000000.0).toFloat
          dqKamanjaCacheQueueEntry.thisMsgEvent.modelinfo = dqKamanjaCacheQueueEntry.modelsForMessage.toArray[KamanjaModelEvent]
        }
      }
    }
  }

  private def isNotShuttingDown: Boolean = {
    (KamanjaConfiguration.shutdown == false && KamanjaManager.instance != null && KamanjaManager.instance.GetEnvCtxts.length > 0)
  }

  private def executeModels(): Unit = {
    while (isNotShuttingDown) {
      val dqKamanjaCacheQueueEntry: KamanjaCacheQueueEntry = null
      if (dqKamanjaCacheQueueEntry == null) {
        // Sleep some time or until we get listener or sleep done for this queue
        try {
          Thread.sleep(waitTimeForNoMdlsExecInMs)
        } catch {
          case e: InterruptedException => {
            // Interrupted exception
          }
          case e: Throwable => {
            LOG.error("Failed to sleep", e)
          }
        }
      } else /* if (dqKamanjaCacheQueueEntry != null) */ {
        var execNextMdl = true
        while (execNextMdl && isNotShuttingDown) {
          execNextMdl = false
          executeModel(dqKamanjaCacheQueueEntry)
          if (dqKamanjaCacheQueueEntry.execPos >= dqKamanjaCacheQueueEntry.exeQueue.size) {
            try {
              CommitDataComponent.commitData(dqKamanjaCacheQueueEntry.txnCtxt)
              if (LeanringEngine.hasValidOrigin(dqKamanjaCacheQueueEntry.txnCtxt)) {
                KamanjaLeader.getThrottleControllerCache.remove("TXN-" + dqKamanjaCacheQueueEntry.txnCtxt.getTransactionId())
              }
            } catch {
              case e: Throwable => {
                LOG.error("Failed to commit data for transactionId:" + dqKamanjaCacheQueueEntry.txnCtxt.getTransactionId(), e)
              }
            }
          } else {
            val execNode = dqKamanjaCacheQueueEntry.exeQueue(dqKamanjaCacheQueueEntry.execPos)
            val (partKeyStr, partitionIdx) = LeanringEngine.GetQueuePartitionInfo(nodeIdModlsObj, execNode, dqKamanjaCacheQueueEntry.txnCtxt, startPartitionId)

            if (partitionIdx < startPartitionId || partitionIdx > endPartitionId) {
              KamanjaLeader.AddToRemoteProcessingBucket(partitionIdx, dqKamanjaCacheQueueEntry)
            } else {
              execNextMdl = true
            }
          }
        }
      }
    }
  }

  override def run(): Unit = {
    executeModels
  }
}

object CommitDataComponent {
  private val LOG = LogManager.getLogger(getClass);
  private var eventsCntr: Long = 0
  private var lastTimeCommitOffsets: Long = System.currentTimeMillis
  private val nullEventOrigin = EventOriginInfo(null, null)
  private var lastEventOrigin = nullEventOrigin
  private val execCtxt_reent_lock = new ReentrantReadWriteLock(true)

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

  def commitData(txnCtxt: TransactionContext): Unit = {
    try {
      val (outputAdapters, storageAdapters, cntr) = KamanjaManager.instance.resolveAdapterBindins

      outputAdapters.foreach(adap => {
        try {
          val sendContainers = ArrayBuffer[ContainerInterface]();
          adap._2.foreach(bind => (bind, txnCtxt.getContainersOrConcepts(bind.messageName).foreach(orginAndmsg => {
            sendContainers += orginAndmsg._2.asInstanceOf[ContainerInterface];
          })))
          if (sendContainers.size > 0) {
            adap._1.send(txnCtxt, sendContainers.toArray)
          }
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

      val nodeContext = if (txnCtxt != null) txnCtxt.getNodeCtxt() else null

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
        if (txnCtxt.origin.key != null && txnCtxt.origin.value != null && txnCtxt.origin.key.trim.size > 0 && txnCtxt.origin.value.trim.size > 0) {
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
            lastEventOrigin = txnCtxt.origin
          }
        }
      }
      else {
        if (LOG.isDebugEnabled) {
          val key = if (txnCtxt != null && txnCtxt.origin != null && txnCtxt.origin.key != null) txnCtxt.origin.key else ""
          val value = if (txnCtxt != null && txnCtxt.origin != null && txnCtxt.origin.value != null) txnCtxt.origin.value else ""
          LOG.debug(s"Not Saving AdapterUniqKvData key:${key}, value:${value}. txnCtxt: ${txnCtxt}, nodeContext: ${nodeContext}")
        }
      }
    } catch {
      case e: Throwable => throw e
    }
  }
}

class LearningEngine {
  private val LOG = LogManager.getLogger(getClass);
  private var cntr: Long = 0
  private var mdlsChangedCntr: Long = -1
  private val isLocalInlineExecution = true
  // inlineExecution is used for local inline execution
  private var inlineExecution: LeanringEngineRemoteExecution = new LeanringEngineRemoteExecution(0, 0, 0)
  private var nodeIdModlsObj = scala.collection.mutable.Map[Long, (MdlInfo, ModelInstance)]()
  // Key is Nodeid (Model ElementId), Value is ModelInfo & Previously Initialized model instance in case of Reuse instances
  // ModelName, ModelInfo, IsModelInstanceReusable, Global ModelInstance if the model is IsModelInstanceReusable == true. The last boolean is to check whether we tested message type or not (thi is to check Reusable flag)
  // var models = Array[(String, MdlInfo, Boolean, ModelInstance, Boolean)]()
  // var validateMsgsForMdls = scala.collection.mutable.Set[String]() // Message Names for creating models inst val results = RunAllModels(transId, iances

  private var dagRuntime = new DagRuntime()

  def execute(txnCtxt: TransactionContext): Unit = {
    // List of ModelIds that we ran.
    var outMsgIds: ArrayBuffer[Long] = new ArrayBuffer[Long]()
    var modelsForMessage: ArrayBuffer[KamanjaModelEvent] = new ArrayBuffer[KamanjaModelEvent]()

    var msgProcessingEndTime: Long = -1L
    var thisMsgEvent: KamanjaMessageEvent = txnCtxt.getMessageEvent.asInstanceOf[KamanjaMessageEvent]
    var msgProcessingStartTime: Long = System.nanoTime

    try {
      val mdlChngCntr = KamanjaMetadata.GetModelsChangedCounter
      if (mdlChngCntr != mdlsChangedCntr) {
        val (newDag, tmpNodeIdModlsObj, tMdlsChangedCntr) = KamanjaMetadata.getExecutionDag
        dagRuntime.SetDag(newDag)
        mdlsChangedCntr = tMdlsChangedCntr
        val newNodeIdModlsObj = scala.collection.mutable.Map[Long, (MdlInfo, ModelInstance)]()
        tmpNodeIdModlsObj.foreach(info => {
          val oldMdlInfo = nodeIdModlsObj.getOrElse(info._1, null)
          if (oldMdlInfo != null && info._2.mdl.getModelDef().UniqId == oldMdlInfo._1.mdl.getModelDef().UniqId) {
            // If it is same version and same factory
            newNodeIdModlsObj(info._1) = (info._2, oldMdlInfo._2)
          } else {
            newNodeIdModlsObj(info._1) = (info._2, null)
          }
        })
        nodeIdModlsObj = newNodeIdModlsObj
      }

      dagRuntime.ReInit()

      val outputDefault: Boolean = false;
      val (origin, orgMsg) = txnCtxt.getInitialMessage
      val elemId = KamanjaMetadata.getMdMgr.ElementIdForSchemaId(orgMsg.asInstanceOf[ContainerInterface].getSchemaId)
      val readyNodes = dagRuntime.FireEdge(EdgeId(0, elemId))
      if (LOG.isDebugEnabled) {
        val msgTyp = if (orgMsg == null) "null" else orgMsg.getFullTypeName
        val msgElemId = if (orgMsg == null) "0" else KamanjaMetadata.getMdMgr.ElementIdForSchemaId(orgMsg.asInstanceOf[ContainerInterface].getSchemaId)
        val producedNodeIds = if (readyNodes != null) readyNodes.map(nd => "(NodeId:%d,iesPos:%d)".format(nd.nodeId, nd.iesPos)).mkString(",") else ""
        val msg = "LearningEngine:InputMessageFrom:%s with ElementId:%d produced:%s from DAG".format(msgTyp, msgElemId, producedNodeIds)
        LOG.debug(msg)
      }
      if (thisMsgEvent != null)
        thisMsgEvent.messageid = elemId

      val exeQueue = ArrayBuffer[ReadyNode]()
      var execPos = 0

      exeQueue ++= readyNodes

      if (exeQueue.size > execPos) {
        if (inlineExecution != null && KamanjaLeader.isLocalExecution) {
          while (execPos < exeQueue.size) {
            val execNode = exeQueue(execPos)
            inlineExecution.executeModel(new KamanjaCacheQueueEntry(exeQueue, execPos, dagRuntime, txnCtxt, thisMsgEvent, modelsForMessage, msgProcessingStartTime))
            execPos += 1
          }
          // all models executed. Send output
          try {
            CommitDataComponent.commitData(txnCtxt)
            if (LeanringEngine.hasValidOrigin(txnCtxt)) {
              KamanjaLeader.getThrottleControllerCache.put("ADAP-KEY-COMPLETED-MAX-" + txnCtxt.origin.key, txnCtxt.origin.value)
            }
          } catch {
            case e: Throwable => {
              LOG.error("Failed to commit data for transactionId:" + txnCtxt.getTransactionId(), e)
            }
          }
        } else {
          // Push the following into Cache as one object for TransactionId as Key
          val execNode = exeQueue(execPos)

          val partitionIdForNoKey = 0 // BUGBUG:: Collect this value

          val (partKeyStr, partitionIdx) = LeanringEngine.GetQueuePartitionInfo(nodeIdModlsObj, execNode, txnCtxt, partitionIdForNoKey)
          if (LeanringEngine.hasValidOrigin(txnCtxt)) {
            val map = new java.util.HashMap[String, AnyRef]
            map.put("ADAP-KEY-MAX-" + txnCtxt.origin.key, txnCtxt.origin.value)

            val json = "Jar" ->
              ("K" -> txnCtxt.origin.key) ~
                ("V" -> txnCtxt.origin.value)
            val outputJson = compact(render(json))

            map.put("TXN-" + txnCtxt.getTransactionId(), outputJson)

            KamanjaLeader.getThrottleControllerCache.put(map)
          }
          KamanjaLeader.AddToRemoteProcessingBucket(partitionIdx, new KamanjaCacheQueueEntry(exeQueue, execPos, dagRuntime, txnCtxt, thisMsgEvent, modelsForMessage, msgProcessingStartTime))
        }
      } else {
        // No models found to execute. Send output
        try {
          CommitDataComponent.commitData(txnCtxt)
          if (LeanringEngine.hasValidOrigin(txnCtxt)) {
            KamanjaLeader.getThrottleControllerCache.put("ADAP-KEY-COMPLETED-MAX-" + txnCtxt.origin.key, txnCtxt.origin.value)
          }
        } catch {
          case e: Throwable => {
            LOG.error("Failed to commit data for transactionId:" + txnCtxt.getTransactionId(), e)
          }
        }
      }
    } catch {
      case e: Exception => {
        // Generate an exception event
        LOG.error("Failed to execute message", e)
        val st = StackTrace.ThrowableTraceString(e)
        if (thisMsgEvent != null) {
          thisMsgEvent.eventepochtime = System.currentTimeMillis()
          thisMsgEvent.error = st
          thisMsgEvent.elapsedtimeinms = ((System.nanoTime - msgProcessingStartTime) / 1000000.0).toFloat
          thisMsgEvent.modelinfo = modelsForMessage.toArray[KamanjaModelEvent]
        }
        // val exeptionEvent = createExceptionEvent(LeanringEngine.modelExecutionException, LeanringEngine.engineComponent, st, txnCtxt)
        // txnCtxt.getNodeCtxt.getEnvCtxt.postMessages(Array(exeptionEvent))
        // throw e
      }
    }
    if (thisMsgEvent != null) {
      thisMsgEvent.eventepochtime = System.currentTimeMillis()
      thisMsgEvent.elapsedtimeinms = ((System.nanoTime - msgProcessingStartTime) / 1000000.0).toFloat
      thisMsgEvent.modelinfo = modelsForMessage.toArray[KamanjaModelEvent]
    }
  }

  private def createExceptionEvent(errorType: String, compName: String, errorString: String, txnCtxt: TransactionContext): KamanjaExceptionEvent = {
    // ExceptionEventFactory is guaranteed to be here....
    var exceptionEvnt = txnCtxt.getNodeCtxt.getEnvCtxt.getContainerInstance("com.ligadata.KamanjaBase.KamanjaExceptionEvent")
    if (exceptionEvnt == null)
      throw new KamanjaException("Unable to create message/container com.ligadata.KamanjaBase.KamanjaExceptionEvent", null)
    var exceptionEvent = exceptionEvnt.asInstanceOf[KamanjaExceptionEvent]
    exceptionEvent.errortype = errorType
    exceptionEvent.timeoferrorepochms = System.currentTimeMillis
    exceptionEvent.componentname = compName
    exceptionEvent.errorstring = errorString
    exceptionEvent
  }
}
