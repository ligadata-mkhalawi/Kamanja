
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

import com.ligadata.KamanjaBase._
import com.ligadata.InputOutputAdapterInfo.{ InputAdapter, OutputAdapter, PartitionUniqueRecordKey, PartitionUniqueRecordValue, StartProcPartInfo }
import com.ligadata.StorageBase.DataStore
import com.ligadata.Utils.ClusterStatus
import com.ligadata.kamanja.metadata.{ BaseElem, MappedMsgTypeDef, BaseAttributeDef, StructTypeDef, EntityType, AttributeDef, MessageDef, ContainerDef, ModelDef }
import com.ligadata.kamanja.metadata._
import com.ligadata.kamanja.metadata.MdMgr._

import com.ligadata.kamanja.metadataload.MetadataLoad
import scala.collection.mutable.TreeSet
import com.ligadata.KamanjaBase.{ ContainerFactoryInterface, MessageFactoryInterface, ContainerInterface, EnvContext }
import scala.collection.mutable.HashMap
import org.apache.logging.log4j.{ Logger, LogManager }
import scala.collection.mutable.ArrayBuffer
import com.ligadata.Serialize._
import com.ligadata.ZooKeeper._
import org.apache.curator.framework._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.apache.curator.utils.ZKPaths
import scala.actors.threadpool.{ Executors, ExecutorService }
import com.ligadata.Exceptions.{ KamanjaException, FatalAdapterException }
import scala.collection.JavaConversions._
import com.ligadata.KvBase.{ Key }
import java.io.{ PrintWriter, File, PrintStream, BufferedReader, InputStreamReader }
import java.util.concurrent.locks.ReentrantReadWriteLock

case class AdapMaxPartitions(Adap: String, MaxParts: Int)
/*
case class NodeDistMap(Adap: String, Parts: List[String])
case class DistributionMap(Node: String, Adaps: List[NodeDistMap])
case class FoundKeysInValidation(K: String, V1: String, V2: Int, V3: Int, V4: Long)
case class ActionOnAdaptersMap(action: String, adaptermaxpartitions: Option[List[AdapMaxPartitions]], distributionmap: Option[List[DistributionMap]])
*/

case class PartitionKeyVal(Key: String, KeyValue: String, UUID: String, NodeStartTime: Long, Counter: Long)
case class NodeDistMap(Adap: String, Parts: List[PartitionKeyVal])
case class DistributionMap(Node: String, Adaps: List[NodeDistMap])
case class ActionOnAdaptersMap(action: String, adaptermaxpartitions: Option[List[AdapMaxPartitions]], distributionmap: Option[List[DistributionMap]])

case class KeyValue(var keyValue: String, var nodeid: String, var uuid: String, var nodestarttime: Long, var counter: Long)

object KamanjaLeader {
  private[this] val LOG = LogManager.getLogger(getClass);
  private[this] val lock = new Object()
  private[this] val lock1 = new Object()
  private[this] var newClusterStatus = ClusterStatus("", false, "", null)
  private[this] var newClusterStatusChangedTime = 0L
  private[this] var newClusterStatusCopiedTime = 0L
  private[this] var clusterStatus = ClusterStatus("", false, "", null)
  //  private[this] var zkLeaderLatch: ZkLeaderLatch = _
  private[this] var nodeId: String = _
  private[this] var zkConnectString: String = _
  private[this] var engineLeaderZkNodePath: String = _
  private[this] var engineDistributionZkNodePath: String = _
  private[this] var dataChangeZkNodePath: String = _
  private[this] var adaptersStatusPath: String = _
  private[this] var zkSessionTimeoutMs: Int = _
  private[this] var zkConnectionTimeoutMs: Int = _
  //  private[this] var zkEngineDistributionNodeListener: ZooKeeperListener = _
  //  private[this] var zkAdapterStatusNodeListener: ZooKeeperListener = _
  //  private[this] var zkDataChangeNodeListener: ZooKeeperListener = _
  private[this] var zkcForSetData: CuratorFramework = null
  private[this] val setDataLockObj = new Object()
  private[this] var distributionMap = scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, ArrayBuffer[String]]]() // Nodeid & Unique Keys (adapter unique name & unique key)
  //  private[this] var foundKeysInValidation: scala.collection.immutable.Map[String, (String, Int, Int, Long)] = _
  private[this] var adapterMaxPartitions = scala.collection.mutable.Map[String, Int]() // Adapters & Max Partitions
  private[this] var allPartitionsToValidate = scala.collection.mutable.Map[String, Set[String]]()
  private[this] var nodesStatus = scala.collection.mutable.Set[String]() // NodeId
  private[this] var expectedNodesAction: String = _
  private[this] var nodesActionIssuedTime: Long = 0
  private[this] var curParticipents = Set[String]() // Derived from clusterStatus.participants
  private[this] var canRedistribute = false
  private[this] var inputAdapters: ArrayBuffer[InputAdapter] = _
  private[this] var outputAdapters: ArrayBuffer[OutputAdapter] = _
  private[this] var storageAdapters: ArrayBuffer[DataStore] = _
  private[this] var envCtxt: EnvContext = _
  private[this] var updatePartitionsFlag = false
  private[this] var saveEndOffsets = false
  private[this] var distributionExecutor = Executors.newFixedThreadPool(1)

  private var partitionsInfoMap = scala.collection.mutable.Map[String, String]()
  private var execCtxtsAdapterInfoPool: ExecutorService = null // 
  var updatePartitionInfoPath = "data/node"
  var consolidatedPartitionInfo = "/adapterinfo/consolidate/"
  var adapterJson: String = ""
  //key is node id, value is (partition id, file name, offset, ignoreFirstMsg)
  val allPartitions = scala.collection.mutable.Map[String, (String, String, String, Long, Long)]()
  private val consolidatedPartitions = scala.collection.mutable.Map[String, String]()
  var condolidatedAdapterInfoMap = Map[String, Array[(String, String)]]()

  val versionStr = "value embedded in node execution information"
  val UUID = AdapterPartitionInfoUtil.setGuid(System.currentTimeMillis())
  val nodeStartTime = System.currentTimeMillis()

  var counter: Long = 0
  def increment = {
    counter = counter + 1;
    counter
  }

  private val MAX_ZK_RETRIES = 1

  def Reset: Unit = {
    newClusterStatus = ClusterStatus("", false, "", null)
    clusterStatus = ClusterStatus("", false, "", null)
    newClusterStatusChangedTime = 0L
    newClusterStatusCopiedTime = 0L
    //    zkLeaderLatch = null
    nodeId = null
    zkConnectString = null
    engineLeaderZkNodePath = null
    engineDistributionZkNodePath = null
    dataChangeZkNodePath = null
    adaptersStatusPath = null
    zkSessionTimeoutMs = 0
    zkConnectionTimeoutMs = 0
    //    zkEngineDistributionNodeListener = null
    //    zkAdapterStatusNodeListener = null
    //    zkDataChangeNodeListener = null
    zkcForSetData = null
    distributionMap = scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, ArrayBuffer[String]]]() // Nodeid & Unique Keys (adapter unique name & unique key)
    //    foundKeysInValidation = null
    adapterMaxPartitions = scala.collection.mutable.Map[String, Int]() // Adapters & Max Partitions
    allPartitionsToValidate = scala.collection.mutable.Map[String, Set[String]]()
    nodesStatus = scala.collection.mutable.Set[String]() // NodeId
    expectedNodesAction = null
    curParticipents = Set[String]() // Derived from clusterStatus.participants
    canRedistribute = false
    inputAdapters = null
    outputAdapters = null
    envCtxt = null
    updatePartitionsFlag = false
    saveEndOffsets = false
    distributionExecutor = Executors.newFixedThreadPool(1)
  }

  private def SetCanRedistribute(redistFlag: Boolean): Unit = lock.synchronized {
    canRedistribute = redistFlag
  }

  private def SendUnSentInfoToOutputAdapters: Unit = lock.synchronized {
    /*
    // LOG.info("SendUnSentInfoToOutputAdapters -- envCtxt:" + envCtxt + ", outputAdapters:" + outputAdapters)
    if (envCtxt != null && outputAdapters != null) {
      // Information found in Committing list
      val committingInfo = envCtxt.getAllIntermediateCommittingInfo // Array[(String, (Long, String, List[(String, String)]))]

      LOG.info("Information found in Committing Info. table:" + committingInfo.map(info => (info._1, (info._2._1, info._2._2, info._2._3.mkString(";")))).mkString(","))

      if (committingInfo != null && committingInfo.size > 0) {
        // For current key, we need to hold the values of Committing, What ever we have in main table, Validate Adapter Info
        val currentValues = scala.collection.mutable.Map[String, ((Long, String, List[(String, String, String)]), (Long, String), (String, Int, Int, Long))]()

        committingInfo.foreach(ci => {
          currentValues(ci._1.toLowerCase) = (ci._2, null, null)
        })

        val keys = committingInfo.map(info => { info._1 })

        // Get AdapterUniqKvDataInfo 
        val allAdapterUniqKvDataInfo = envCtxt.getAllAdapterUniqKvDataInfo(keys)

        if (allAdapterUniqKvDataInfo != null) {
          LOG.debug("Information found in data table:" + allAdapterUniqKvDataInfo.mkString(","))
          allAdapterUniqKvDataInfo.foreach(ai => {
            val key = ai._1.toLowerCase
            val fndVal = currentValues.getOrElse(key, null)
            if (fndVal != null) {
              currentValues(key) = (fndVal._1, ai._2, null)
            }
          })
        }

        // Get recent information from output validators
        val validateFndKeysAndVals = getValidateAdaptersInfo
        if (validateFndKeysAndVals != null) {
          LOG.debug("Information found in Validate Adapters:" + validateFndKeysAndVals.mkString(","))
          validateFndKeysAndVals.foreach(validatekv => {
            val key = validatekv._1.toLowerCase
            val fndVal = currentValues.getOrElse(key, null)
            if (fndVal != null) {
              currentValues(key) = (fndVal._1, fndVal._2, validatekv._2)
            }
          })
        }

        val allOuAdapters = if (outputAdapters != null && currentValues.size > 0) outputAdapters.map(o => (o.inputConfig.Name.toLowerCase, o)).toMap else Map[String, OutputAdapter]()

        // Now find which one we need to resend
        // BUGBUG:: Not yet handling M/N Sub Messages for a Message
        currentValues.foreach(possibleKeyToSend => {
          // We should not have possibleKeyToSend._2._1._1 < possibleKeyToSend._2._2._1 and if it is possibleKeyToSend._2._1._1 > possibleKeyToSend._2._2._1, the data is not yet committed to main table.
          if (possibleKeyToSend._2._1 != null && possibleKeyToSend._2._2 != null && possibleKeyToSend._2._1._1 == possibleKeyToSend._2._2._1) { // This is just committing record and is written in main table.
            if (possibleKeyToSend._2._3 == null || possibleKeyToSend._2._1._1 != possibleKeyToSend._2._3._4) { // Not yet written in. possibleKeyToSend._2._1._1 < possibleKeyToSend._2._3._4 should not happen and possibleKeyToSend._2._1._1 == possibleKeyToSend._2._3._4 is already written.
              val outputs = possibleKeyToSend._2._1._3.groupBy(_._1)
              outputs.foreach(output => {
                val oadap = allOuAdapters.getOrElse(output._1, null)
                if (oadap != null) {
                  oadap.send(output._2.map(out => out._3.getBytes("UTF8")).toArray, output._2.map(out => out._2.getBytes("UTF8")).toArray)
                }
              })
            }
          }
        })
        // Remove after sending again 
        // envCtxt.removeCommittedKeys(keys)
      }
    }
*/
  }

  private def UpdatePartitionsNodeData(eventType: String, eventPath: String, eventPathData: Array[Byte]): Unit = lock.synchronized {
    try {
      val evntPthData = if (eventPathData != null) (new String(eventPathData)) else "{}"
      val extractedNode = ZKPaths.getNodeFromPath(eventPath)
      LOG.warn("UpdatePartitionsNodeData => eventType: %s, eventPath: %s, eventPathData: %s, Extracted Node:%s".format(eventType, eventPath, evntPthData, extractedNode))

      if (eventType.compareToIgnoreCase("CHILD_UPDATED") == 0) {
        if (curParticipents(extractedNode)) { // If this node is one of the participent, then work on this, otherwise ignore
          try {
            val json = parse(evntPthData)
            if (json == null || json.values == null) // Not doing any action if not found valid json
              return
            val values = json.values.asInstanceOf[Map[String, Any]]
            val action = values.getOrElse("action", "").toString.toLowerCase

            if (expectedNodesAction.compareToIgnoreCase(action) == 0) {
              nodesStatus += extractedNode

              // val nodeJson = parse(extractedNode)

              // if (nodeJson == null || nodeJson.values == null) // Not doing any action if not found valid json
              //  return

              //   val nodevalues = nodeJson.values.asInstanceOf[Map[String, String]]

              //  val nodeid = nodevalues.getOrElse("NodeId", "").toString.toLowerCase

              val (nodeid, uuid) = extractNodeIdAndUUId(extractedNode)

              var partKeyValueMap = scala.collection.mutable.Map[String, (String, String, String, Long, Long)]()

              //scala.collection.mutable.Map[null, (null, UUID, nodeId)]()
              if (distributionMap != null && distributionMap.size > 0) {
                distributionMap.foreach(distInfo => {
                  distInfo._2.foreach(parts => {
                    val partitions = parts._2
                    if (partitions != null && partitions.size > 0) {
                      for (i <- 0 until partitions.size) {
                        partKeyValueMap(partitions(i)) = (null, nodeid, UUID, nodeStartTime, counter)
                      }
                    }
                  })
                })
              }
              //get the key values from storage
              var nodeKeysMap = scala.collection.mutable.Map[String, Array[String]]()
              distributionMap.foreach(distMap => {
                distMap._2.foreach(distr => {
                  var keys = new scala.collection.mutable.ArrayBuffer[String]
                  if (nodeKeysMap.contains(distr._1)) {
                    val previousKeys = nodeKeysMap(distr._1)
                    if (previousKeys != null && previousKeys.length > 0) {
                      for (i <- 0 until previousKeys.length) {
                        keys += previousKeys(i)
                      }
                    }
                  }
                  keys = keys ++ distr._2
                  nodeKeysMap(distr._1) = keys.toArray
                })
              })

              partKeyValueMap = getPartKeyValues(partKeyValueMap, nodeKeysMap.toMap)

              if (LOG.isDebugEnabled()) {
                nodeKeysMap.foreach(kv => {
                  LOG.debug("nodeKeysMap kv " + kv._1)
                  for (i <- 0 until kv._2.length) {
                    LOG.debug("kv._2 " + kv._2(i))
                  }
                })
                if (partKeyValueMap != null && partKeyValueMap.keys.size > 0) {
                  partKeyValueMap.foreach(kv => {
                    LOG.debug(kv._1 + " : " + kv._2._1 + " : " + kv._2._2 + " : " + kv._2._3 + " : " + kv._2._4 + " : " + kv._2._5)
                  })
                }
              }

              if (expectedNodesAction == "stopped") {
                // extractedNode
                val parittionsInfo = values.getOrElse("PartitionsInfo", "")

                if (parittionsInfo != null)
                  partitionsInfoMap(nodeId) = parittionsInfo.toString
              }
              val finalConsolidatedPartitionMap = consolidatePartitionsMap(partitionsInfoMap.toMap, partKeyValueMap)

              if (nodesStatus.size == curParticipents.size && expectedNodesAction == "stopped" && (nodesStatus -- curParticipents).isEmpty) {
                // Send the data to output queues in case if anything not sent before
                SendUnSentInfoToOutputAdapters
                // envCtxt.PersistRemainingStateEntriesOnLeader
                nodesStatus.clear
                expectedNodesAction = "distributed"

                // val fndKeyInVal = if (foundKeysInValidation == null) scala.collection.immutable.Map[String, (String, Int, Int, Long)]() else foundKeysInValidation
                val fndKeyInVal = scala.collection.immutable.Map[String, (String, Int, Int, Long)]()

                // Set DISTRIBUTE Action on engineDistributionZkNodePath
                // Send all Unique keys to corresponding nodes 
                val distribute =
                  ("action" -> "distribute") ~
                    ("adaptermaxpartitions" -> adapterMaxPartitions.map(kv =>
                      ("Adap" -> kv._1) ~
                        ("MaxParts" -> kv._2))) ~
                    ("distributionmap" -> distributionMap.map(kv =>
                      ("Node" -> kv._1) ~
                        ("Adaps" -> kv._2.map(kv1 => ("Adap" -> kv1._1) ~
                          ("Parts" -> kv1._2.toList.map(k => {
                            val value = finalConsolidatedPartitionMap(k);
                            ("Key" -> k) ~
                              ("KeyValue" -> value._1) ~
                              ("UUID" -> value._3) ~
                              ("NodeStartTime" -> value._4) ~
                              ("Counter" -> value._5)
                          }))))))

                val sendJson = compact(render(distribute))
                LOG.warn("Partition Distribution: " + sendJson)
                nodesActionIssuedTime = System.currentTimeMillis
                SetNewDataToZkc(engineDistributionZkNodePath, sendJson.getBytes("UTF8"))
              }

              /*
                if (nodesStatus.size == curParticipents.size && expectedNodesAction == "stopped" && (nodesStatus -- curParticipents).isEmpty) {
                  // Send the data to output queues in case if anything not sent before
                  SendUnSentInfoToOutputAdapters
                  // envCtxt.PersistRemainingStateEntriesOnLeader
                  nodesStatus.clear
                  expectedNodesAction = "distributed"

                  // val fndKeyInVal = if (foundKeysInValidation == null) scala.collection.immutable.Map[String, (String, Int, Int, Long)]() else foundKeysInValidation
                  val fndKeyInVal = scala.collection.immutable.Map[String, (String, Int, Int, Long)]()

                  // Set DISTRIBUTE Action on engineDistributionZkNodePath
                  // Send all Unique keys to corresponding nodes 
                  val distribute =
                    ("action" -> "distribute") ~
                      ("adaptermaxpartitions" -> adapterMaxPartitions.map(kv =>
                        ("Adap" -> kv._1) ~
                          ("MaxParts" -> kv._2))) ~
                      ("distributionmap" -> distributionMap.map(kv =>
                        ("Node" -> kv._1) ~
                          ("Adaps" -> kv._2.map(kv1 => ("Adap" -> kv1._1) ~
                            ("Parts" -> kv1._2.toList)))))
                  val sendJson = compact(render(distribute))
                  LOG.warn("Partition Distribution: " + sendJson)
                  nodesActionIssuedTime = System.currentTimeMillis
                  SetNewDataToZkc(engineDistributionZkNodePath, sendJson.getBytes("UTF8"))
                }
              } */
            } else {
              val redStr = if (canRedistribute) "canRedistribute is true, Redistributing" else "canRedistribute is false, waiting until next call"
              // Got different action. May be re-distribute. For now any non-expected action we will redistribute
              LOG.info("UpdatePartitionsNodeData => eventType: %s, eventPath: %s, eventPathData: %s, Extracted Node:%s. Expected Action:%s, Recieved Action:%s %s.".format(eventType, eventPath, evntPthData, extractedNode, expectedNodesAction, action, redStr))
              if (canRedistribute) {
                LOG.warn("Got different action (%s) than expected(%s). Going to redistribute the work".format(action, expectedNodesAction))
                SetUpdatePartitionsFlag
              }
            }
          } catch {
            case e: Exception => {
              LOG.error("UpdatePartitionsNodeData => Failed eventType: %s, eventPath: %s, eventPathData: %s".format(eventType, eventPath, evntPthData), e)
            }
          }
        }

      } else if (eventType.compareToIgnoreCase("CHILD_REMOVED") == 0) {
        // Not expected this. Need to check what is going on
        LOG.error("UpdatePartitionsNodeData => eventType: %s, eventPath: %s, eventPathData: %s".format(eventType, eventPath, evntPthData))
      } else if (eventType.compareToIgnoreCase("CHILD_ADDED") == 0) {
        // Not doing anything here
      }
    } catch {
      case e: Exception => {
        LOG.error("Exception while UpdatePartitionsNodeData", e)
      }
    }
  }

  private def getAllPartitionsToValidate: (Array[(String, String)], scala.collection.immutable.Map[String, Set[String]]) = lock.synchronized {
    val allPartitionUniqueRecordKeys = ArrayBuffer[(String, String)]()
    val allPartsToValidate = scala.collection.mutable.Map[String, Set[String]]()

    // Get all PartitionUniqueRecordKey for all Input Adapters

    inputAdapters.foreach(ia => {
      try {
        val uk = ia.GetAllPartitionUniqueRecordKey
        val name = ia.UniqueName
        val ukCnt = if (uk != null) uk.size else 0
        adapterMaxPartitions(name) = ukCnt
        if (ukCnt > 0) {
          val serUK = uk.map(k => {
            val kstr = k.Serialize
            LOG.debug("Unique Key in %s => %s".format(name, kstr))
            (name, kstr)
          })
          allPartitionUniqueRecordKeys ++= serUK
          allPartsToValidate(name) = serUK.map(k => k._2).toSet
        } else {
          allPartsToValidate(name) = Set[String]()
        }
      } catch {
        case fae: FatalAdapterException => {
          // Adapter could not get partition information and can't reconver.
          LOG.error("Failed to get partitions from validate adapter " + ia.UniqueName, fae)
        }
        case e: Exception => {
          // Adapter could not get partition information and can't reconver.
          LOG.error("Failed to get partitions from validate adapter " + ia.UniqueName, e)
        }
        case e: Throwable => {
          // Adapter could not get partition information and can't reconver.
          LOG.error("Failed to get partitions from validate adapter " + ia.UniqueName, e)
        }
      }
    })
    return (allPartitionUniqueRecordKeys.toArray, allPartsToValidate.toMap)
  }

  /*
  private def getValidateAdaptersInfo: scala.collection.immutable.Map[String, (String, Int, Int, Long)] = lock.synchronized {

    val savedValidatedAdaptInfo = envCtxt.GetValidateAdapterInformation
    val map = scala.collection.mutable.Map[String, String]()

    LOG.debug("savedValidatedAdaptInfo: " + savedValidatedAdaptInfo.mkString(","))

    savedValidatedAdaptInfo.foreach(kv => {
      map(kv._1.toLowerCase) = kv._2
    })

    CollectKeyValsFromValidation.clear
    validateInputAdapters.foreach(via => {
      try {
        // Get all Begin values for Unique Keys
        val begVals = via.getAllPartitionBeginValues
        val finalVals = new ArrayBuffer[StartProcPartInfo](begVals.size)

        // Replace the newly found ones
        val nParts = begVals.size
        for (i <- 0 until nParts) {
          val key = begVals(i)._1.Serialize.toLowerCase
          val foundVal = map.getOrElse(key, null)

          val info = new StartProcPartInfo

          if (foundVal != null) {
            val desVal = via.DeserializeValue(foundVal)
            info._validateInfoVal = desVal
            info._key = begVals(i)._1
            info._val = desVal
          } else {
            info._validateInfoVal = begVals(i)._2
            info._key = begVals(i)._1
            info._val = begVals(i)._2
          }

          finalVals += info
        }
        LOG.debug("Trying to read data from ValidatedAdapter: " + via.UniqueName)
        via.StartProcessing(finalVals.toArray, false)
      } catch {
        case e: FatalAdapterException => {
          // If validate adapter is not able to connect, just ignoring it for now
          LOG.error("Validate Adapter " + via.UniqueName + " failed to start processing", e)
        }
        case e: Exception => {
          // If validate adapter is not able to connect, just ignoring it for now
          LOG.error("Validate Adapter " + via.UniqueName + " failed to start processing", e)
        }
        case e: Throwable => {
          // If validate adapter is not able to connect, just ignoring it for now
          LOG.error("Validate Adapter " + via.UniqueName + " failed to start processing", e)
        }
      }
    })

    var stillWait = true
    while (stillWait) {
      try {
        Thread.sleep(1000) // sleep 1000 ms and then check
      } catch {
        case e: Exception => {
          LOG.debug("Failed to sleep thread", e)
        }
      }
      if ((System.nanoTime - CollectKeyValsFromValidation.getLastUpdateTime) < 1000 * 1000000) // 1000ms
        stillWait = true
      else
        stillWait = false
    }

    // Stopping the Adapters
    validateInputAdapters.foreach(via => {
      try {
        via.StopProcessing
      } catch {
        case fae: FatalAdapterException => {
          LOG.error("Validate adapter " + via.UniqueName + "failed to stop processing", fae)
        }
        case e: Exception => {
          LOG.error("Validate adapter " + via.UniqueName + "failed to stop processing", e)
        }
        case e: Throwable => {
          LOG.error("Validate adapter " + via.UniqueName + "failed to stop processing", e)
        }
      }
    })

    if (distributionExecutor.isShutdown) {
      LOG.debug("Distribution Executor is shutting down")
      return scala.collection.immutable.Map[String, (String, Int, Int, Long)]()
    } else {
      val validateFndKeysAndVals = CollectKeyValsFromValidation.get
      LOG.debug("foundKeysAndValuesInValidation: " + validateFndKeysAndVals.map(v => { (v._1.toString, v._2.toString) }).mkString(","))
      return validateFndKeysAndVals
    }
  }
*/

  private def UpdatePartitionsIfNeededOnLeader: Unit = lock.synchronized {
    val cs = GetClusterStatus
    if (cs.isLeader == false || cs.leaderNodeId != cs.nodeId) return // This is not leader, just return from here. This is same as (cs.leader != cs.nodeId)

    LOG.warn("Distribution NodeId:%s, IsLeader:%s, Leader:%s, AllParticipents:{%s}".format(cs.nodeId, cs.isLeader.toString, cs.leaderNodeId, cs.participantsNodeIds.mkString(",")))

    // Clear Previous Distribution Map
    distributionMap.clear
    adapterMaxPartitions.clear
    nodesStatus.clear
    expectedNodesAction = ""
    nodesActionIssuedTime = System.currentTimeMillis
    curParticipents = if (cs.participantsNodeIds != null) cs.participantsNodeIds.toSet else Set[String]()

    try {
      var tmpDistMap = ArrayBuffer[(String, scala.collection.mutable.Map[String, ArrayBuffer[String]])]()

      if (cs.participantsNodeIds != null) {

        // Create ArrayBuffer for each node participating at this moment
        cs.participantsNodeIds.foreach(p => {
          tmpDistMap += ((p, scala.collection.mutable.Map[String, ArrayBuffer[String]]()))
        })

        val (allPartitionUniqueRecordKeys, allPartsToValidate) = getAllPartitionsToValidate
        //        val validateFndKeysAndVals = getValidateAdaptersInfo

        allPartsToValidate.foreach(kv => {
          AddPartitionsToValidate(kv._1, kv._2)
        })

        //        foundKeysInValidation = validateFndKeysAndVals

        // Update New partitions for all nodes and Set the text
        val totalParticipents: Int = cs.participantsNodeIds.size
        if (allPartitionUniqueRecordKeys != null && allPartitionUniqueRecordKeys.size > 0) {
          LOG.debug("allPartitionUniqueRecordKeys: %d".format(allPartitionUniqueRecordKeys.size))
          var cntr: Int = 0
          allPartitionUniqueRecordKeys.foreach(k => {
            //            val fnd = foundKeysInValidation.getOrElse(k._2.toLowerCase, null)
            val af = tmpDistMap(cntr % totalParticipents)._2.getOrElse(k._1, null)
            if (af == null) {
              val af1 = new ArrayBuffer[String]
              af1 += (k._2)
              tmpDistMap(cntr % totalParticipents)._2(k._1) = af1
            } else {
              af += (k._2)
            }
            cntr += 1
          })
        }

        tmpDistMap.foreach(tup => {
          distributionMap(tup._1) = tup._2
        })
      }

      expectedNodesAction = "stopped"
      partitionsInfoMap.clear
      // Set STOP Action on engineDistributionZkNodePath
      val act = ("action" -> "stop")
      val sendJson = compact(render(act))
      nodesActionIssuedTime = System.currentTimeMillis
      SetNewDataToZkc(engineDistributionZkNodePath, sendJson.getBytes("UTF8"))
    } catch {
      case e: Exception => {
        LOG.debug("", e)
      }
    }
  }

  private def AddPartitionsToValidate(adapName: String, partitions: Set[String]): Unit = lock1.synchronized {
    allPartitionsToValidate(adapName) = partitions
  }

  private def GetPartitionsToValidate(adapName: String): Set[String] = lock1.synchronized {
    allPartitionsToValidate.getOrElse(adapName, null)
  }

  private def GetAllAdaptersPartitionsToValidate: Map[String, Set[String]] = lock1.synchronized {
    allPartitionsToValidate.toMap
  }

  private def SetModifiedClusterStatus(cs: ClusterStatus): Unit = lock1.synchronized {
    newClusterStatus = cs
    newClusterStatusChangedTime = System.currentTimeMillis
    logger.warn("SetModifiedClusterStatus - Got updatePartitionsFlag. Waiting for distribution")
  }

  private val waitTmBeforeRedistribute = 15000
  private def copyClusterStatusIfNeeded: Unit = lock1.synchronized {
    val newDistribution =
      if (newClusterStatusCopiedTime == 0 && newClusterStatusChangedTime > 0) {
        logger.warn("First time - Copy ClusterStatus")
        true
      } else {
        val curTm = System.currentTimeMillis
        var canCopyClusterStatus = (newClusterStatusCopiedTime < newClusterStatusChangedTime && ((newClusterStatusChangedTime + waitTmBeforeRedistribute) < curTm))
        if (canCopyClusterStatus) {
          logger.warn("Found canCopyClusterStatus as true. newClusterStatusCopiedTime:%d < newClusterStatusChangedTime:%d, CurrentTime:%d".format(newClusterStatusCopiedTime, newClusterStatusChangedTime, curTm))
          // Check whether ClusterStatus is really changed?
          val curParticipents = if (clusterStatus.participantsNodeIds != null) clusterStatus.participantsNodeIds.toArray else Array[String]()
          val newParticipents = if (newClusterStatus.participantsNodeIds != null) newClusterStatus.participantsNodeIds.toArray else Array[String]()
          val curParticipentsSet = curParticipents.toSet
          val newParticipentsSet = newParticipents.toSet
          val diff1 = curParticipentsSet -- newParticipentsSet
          val diff2 = newParticipentsSet -- curParticipentsSet
          canCopyClusterStatus =
            (clusterStatus.leaderNodeId != newClusterStatus.leaderNodeId ||
              // clusterStatus.nodeId != newClusterStatus.nodeId ||
              curParticipents.size != newParticipents.size ||
              diff1.size > 0 || diff2.size > 0)
          if (!canCopyClusterStatus && newClusterStatusCopiedTime != newClusterStatusChangedTime) {
            // Resetting newClusterStatusCopiedTime from newClusterStatusChangedTime. Because no need to change ClusterStatus/ClusterDistribution
            logger.warn("Found canCopyClusterStatus as true, but there is no real change in ClusterStatus.")
            newClusterStatusCopiedTime = newClusterStatusChangedTime
          }
        }
        canCopyClusterStatus
      }

    if (newDistribution) {
      logger.warn("Copying ClusterStatus to Distribute.")
      clusterStatus = newClusterStatus
      newClusterStatusCopiedTime = newClusterStatusChangedTime
      updatePartitionsFlag = true
      logger.warn("copyClusterStatusIfNeeded - Got updatePartitionsFlag. Waiting for distribution")
    }
  }

  private def SetClusterStatus(cs: ClusterStatus): Unit = lock1.synchronized {
    clusterStatus = cs
    newClusterStatusCopiedTime = newClusterStatusChangedTime
    updatePartitionsFlag = true
    logger.warn("SetClusterStatus - Got updatePartitionsFlag. Waiting for distribution")
  }

  def GetClusterStatus: ClusterStatus = lock1.synchronized {
    return clusterStatus
  }

  private def IsLeaderNode: Boolean = lock1.synchronized {
    return (clusterStatus.isLeader && clusterStatus.leaderNodeId == clusterStatus.nodeId)
  }

  private def IsLeaderNodeAndUpdatePartitionsFlagSet: Boolean = lock1.synchronized {
    if (clusterStatus.isLeader && clusterStatus.leaderNodeId == clusterStatus.nodeId)
      return updatePartitionsFlag
    else
      return false
  }

  private def SetUpdatePartitionsFlag: Unit = lock1.synchronized {
    logger.warn("SetUpdatePartitionsFlag - Got updatePartitionsFlag. Waiting for distribution")
    updatePartitionsFlag = true
  }

  private def GetUpdatePartitionsFlag: Boolean = lock1.synchronized {
    return updatePartitionsFlag
  }

  private def GetUpdatePartitionsFlagAndReset: Boolean = lock1.synchronized {
    val retVal = updatePartitionsFlag
    updatePartitionsFlag = false
    retVal
  }

  // Here Leader can change or Participants can change
  private def EventChangeCallback(cs: ClusterStatus): Unit = {
    logger.warn("DistributionCheck:EventChangeCallback got new ClusterStatus")
    LOG.debug("EventChangeCallback => Enter")
    KamanjaConfiguration.participentsChangedCntr += 1
    SetModifiedClusterStatus(cs)
    LOG.warn("DistributionCheck:NodeId:%s, IsLeader:%s, Leader:%s, AllParticipents:{%s}, participentsChangedCntr:%d".format(cs.nodeId, cs.isLeader.toString, cs.leaderNodeId, cs.participantsNodeIds.mkString(","), KamanjaConfiguration.participentsChangedCntr))
    LOG.debug("EventChangeCallback => Exit")
  }

  /*
  private def GetUniqueKeyValue(uk: String): String = {
    envCtxt.getAdapterUniqueKeyValue(uk)
  }
*/

  private def GetUniqueKeyValue(uk: String): KeyValue = {
    var keyvalue: KeyValue = null
    var keyValueStr = envCtxt.getAdapterUniqueKeyValue(uk)
    try {
      val json = parse(keyValueStr)
      if (json != null && json.values != null) {
        val values = json.values.asInstanceOf[Map[String, String]]
        if (values.contains("versionstr")) {
          val versionStr = values.getOrElse("versionstr", "").toString.toLowerCase
          if (versionStr != null && versionStr.trim().size > 0 && versionStr.equals(versionStr)) {
            val keyValue = values.getOrElse("keyvalue", "").toString
            val nodeid = values.getOrElse("nodeid", nodeId).toString
            val uuid = values.getOrElse("uuid", UUID).toString
            val nodestarttime = values.getOrElse("nodestarttime", nodeStartTime).toString.toLong
            val countr = values.getOrElse("counter", counter).toString.toLong
            keyvalue = new KeyValue(keyValue, nodeid, uuid, nodestarttime, countr)
          } else {
            keyvalue = new KeyValue(keyValueStr, nodeId, UUID, nodeStartTime, counter)
          }
        } else {
          keyvalue = new KeyValue(keyValueStr, nodeId, UUID, nodeStartTime, counter)
        }
      }
    } catch {
      case e: Exception => {
        LOG.warn("uuid, nodeid, nodestarttime do not exists in database. Considering local one")
        keyvalue = new KeyValue(keyValueStr, nodeId, UUID, nodeStartTime, counter)
      }
    }
    if (LOG.isDebugEnabled()) LOG.debug("Kamanja Leader - GetUniqueKeyValue - keyvalue: " + keyvalue.keyValue + " : " + keyvalue.nodeid + " : " + keyvalue.uuid + " : " + keyvalue.nodestarttime + " : " + keyvalue.counter)
    keyvalue
  }

  private def getPartKeyValues(keyValueMap: scala.collection.mutable.Map[String, (String, String, String, Long, Long)], nodeKeysMap: scala.collection.immutable.Map[String, Array[String]]): scala.collection.mutable.Map[String, (String, String, String, Long, Long)] = {
    if (keyValueMap == null || keyValueMap.size == 0) {
      return keyValueMap
    }

    var remainingInpAdapters = inputAdapters.toArray
    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs

    //BUGBUG:: We may be blocking here for long time. Which will not give any updates from zookeeper for this node.
    while (remainingInpAdapters.size > 0) {
      var failedInpAdapters = ArrayBuffer[InputAdapter]()
      remainingInpAdapters.foreach(ia => {
        val name = ia.UniqueName
        try {
          val uAK = nodeKeysMap.getOrElse(name, null)
          if (uAK != null) {
            val uKV = uAK.map(uk => { GetUniqueKeyValue(uk) })
            val keys = uAK.map(k => ia.DeserializeKey(k))
            val vals = uKV.map(v => (ia.DeserializeValue(if (v.keyValue != null) v.keyValue else null), v))
            for (i <- 0 until keys.size) {
              val key = keys(i).Serialize
              keyValueMap(key) = (vals(i)._1.Serialize, vals(i)._2.nodeid, vals(i)._2.uuid, vals(i)._2.nodestarttime, vals(i)._2.counter)
            }
          }
        } catch {
          case fae: FatalAdapterException => {
            LOG.error("Failed to start processing input adapter:" + name, fae)
            failedInpAdapters += ia
          }
          case e: Exception => {
            LOG.error("Failed to start processing input adapter:" + name, e)
            failedInpAdapters += ia
          }
          case t: Throwable => {
            LOG.error("Failed to start processing input adapter:" + name, t)
            failedInpAdapters += ia
          }
        }
      })

      remainingInpAdapters = failedInpAdapters.toArray

      if (remainingInpAdapters.size > 0) {
        try {
          LOG.error("Failed to start processing %d input adapters while distributing. Waiting for another %d milli seconds and going to start them again.".format(remainingInpAdapters.size, failedWaitTime))
          Thread.sleep(failedWaitTime)
        } catch {
          case e: Exception => { LOG.warn("", e) }
        }
        // Adjust time for next time
        if (failedWaitTime < maxFailedWaitTime) {
          failedWaitTime = failedWaitTime * 2
          if (failedWaitTime > maxFailedWaitTime)
            failedWaitTime = maxFailedWaitTime
        }
      }
    }
    LOG.debug("keyValueMap: " + keyValueMap)
    return keyValueMap
  }

  /*
  
  private def StartNodeKeysMap(nodeKeysMap: scala.collection.immutable.Map[String, Array[String]], receivedJsonStr: String, adapMaxPartsMap: Map[String, Int], foundKeysInVald: Map[String, (String, Int, Int, Long)]): Boolean = {
    if (nodeKeysMap == null || nodeKeysMap.size == 0) {
      return true
    }

    var remainingInpAdapters = inputAdapters.toArray
    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs

    //BUGBUG:: We may be blocking here for long time. Which will not give any updates from zookeeper for this node.
    while (remainingInpAdapters.size > 0) {
      var failedInpAdapters = ArrayBuffer[InputAdapter]()
      remainingInpAdapters.foreach(ia => {
        val name = ia.UniqueName
        try {
          val uAK = nodeKeysMap.getOrElse(name, null)
          if (uAK != null) {
            val uKV = uAK.map(uk => { GetUniqueKeyValue(uk) })

            val maxParts = adapMaxPartsMap.getOrElse(name, 0)
            LOG.info("DistributionCheck:On Node %s for Adapter %s with Max Partitions %d UniqueKeys %s, UniqueValues %s".format(nodeId, name, maxParts, uAK.mkString(","), uKV.mkString(",")))

            LOG.debug("Deserializing Keys")
            val keys = uAK.map(k => ia.DeserializeKey(k))

            LOG.debug("Deserializing Values")
            val vals = uKV.map(v => ia.DeserializeValue(if (v != null) v else null))

            LOG.debug("Deserializing Keys & Values done")

            val quads = new ArrayBuffer[StartProcPartInfo](keys.size)

            for (i <- 0 until keys.size) {
              val key = keys(i)

              val info = new StartProcPartInfo
              info._key = key
              info._val = vals(i)
              info._validateInfoVal = vals(i)
              quads += info
            }

            LOG.info(ia.UniqueName + " ==> Processing Keys & values: " + quads.map(q => { (q._key.Serialize, q._val.Serialize, q._validateInfoVal.Serialize) }).mkString(","))
            ia.StartProcessing(quads.toArray, true)
          }
        } catch {
          case fae: FatalAdapterException => {
            LOG.error("Failed to start processing input adapter:" + name, fae)
            failedInpAdapters += ia
          }
          case e: Exception => {
            LOG.error("Failed to start processing input adapter:" + name, e)
            failedInpAdapters += ia
          }
          case t: Throwable => {
            LOG.error("Failed to start processing input adapter:" + name, t)
            failedInpAdapters += ia
          }
        }
      })

      remainingInpAdapters = failedInpAdapters.toArray

      if (remainingInpAdapters.size > 0) {
        try {
          LOG.error("Failed to start processing %d input adapters while distributing. Waiting for another %d milli seconds and going to start them again.".format(remainingInpAdapters.size, failedWaitTime))
          Thread.sleep(failedWaitTime)
        } catch {
          case e: Exception => { LOG.warn("", e) }
        }
        // Adjust time for next time
        if (failedWaitTime < maxFailedWaitTime) {
          failedWaitTime = failedWaitTime * 2
          if (failedWaitTime > maxFailedWaitTime)
            failedWaitTime = maxFailedWaitTime
        }
      }
    }

    return true
  }
*/
  private def StartNodeKeysMap(nodeKeysMap: scala.collection.immutable.Map[String, Array[(String, String)]], receivedJsonStr: String, adapMaxPartsMap: Map[String, Int], foundKeysInVald: Map[String, (String, Int, Int, Long)]): Boolean = {
    if (nodeKeysMap == null || nodeKeysMap.size == 0) {
      return true
    }

    var remainingInpAdapters = inputAdapters.toArray
    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs

    //BUGBUG:: We may be blocking here for long time. Which will not give any updates from zookeeper for this node.
    while (remainingInpAdapters.size > 0) {
      var failedInpAdapters = ArrayBuffer[InputAdapter]()
      remainingInpAdapters.foreach(ia => {
        val name = ia.UniqueName
        try {
          val uAK = nodeKeysMap.getOrElse(name, null)
          if (uAK != null) {
            val uKV = uAK.map(uk => { uk._2 })

            val maxParts = adapMaxPartsMap.getOrElse(name, 0)
            LOG.info("DistributionCheck:On Node %s for Adapter %s with Max Partitions %d UniqueKeys %s, UniqueValues %s".format(nodeId, name, maxParts, uAK.mkString(","), uKV.mkString(",")))

            LOG.debug("Deserializing Keys")
            val keys = uAK.map(k => ia.DeserializeKey(k._1))

            LOG.debug("Deserializing Values")
            val vals = uKV.map(v => ia.DeserializeValue(if (v != null) v else null))

            LOG.debug("Deserializing Keys & Values done")

            val quads = new ArrayBuffer[StartProcPartInfo](keys.size)

            for (i <- 0 until keys.size) {
              val key = keys(i)

              val info = new StartProcPartInfo
              info._key = key
              info._val = vals(i)
              info._validateInfoVal = vals(i)
              quads += info
            }

            LOG.info(ia.UniqueName + " ==> Processing Keys & values: " + quads.map(q => { (q._key.Serialize, q._val.Serialize, q._validateInfoVal.Serialize) }).mkString(","))
            ia.StartProcessing(quads.toArray, true)
          }
        } catch {
          case fae: FatalAdapterException => {
            LOG.error("Failed to start processing input adapter:" + name, fae)
            failedInpAdapters += ia
          }
          case e: Exception => {
            LOG.error("Failed to start processing input adapter:" + name, e)
            failedInpAdapters += ia
          }
          case t: Throwable => {
            LOG.error("Failed to start processing input adapter:" + name, t)
            failedInpAdapters += ia
          }
        }
      })

      remainingInpAdapters = failedInpAdapters.toArray

      if (remainingInpAdapters.size > 0) {
        try {
          LOG.error("Failed to start processing %d input adapters while distributing. Waiting for another %d milli seconds and going to start them again.".format(remainingInpAdapters.size, failedWaitTime))
          Thread.sleep(failedWaitTime)
        } catch {
          case e: Exception => { LOG.warn("", e) }
        }
        // Adjust time for next time
        if (failedWaitTime < maxFailedWaitTime) {
          failedWaitTime = failedWaitTime * 2
          if (failedWaitTime > maxFailedWaitTime)
            failedWaitTime = maxFailedWaitTime
        }
      }
    }

    return true
  }

  def GetAdaptersMaxPartitioinsMap(adaptermaxpartitions: Option[List[AdapMaxPartitions]]): Map[String, Int] = {
    val adapterMax = scala.collection.mutable.Map[String, Int]() // Adapters & Max Partitions
    if (adaptermaxpartitions != None && adaptermaxpartitions != null) {
      adaptermaxpartitions.get.foreach(adapMaxParts => {
        adapterMax(adapMaxParts.Adap) = adapMaxParts.MaxParts
      })
    }

    adapterMax.toMap
  }
  /*
  private def GetDistMapForNodeId(distributionmap: Option[List[DistributionMap]], nodeId: String): scala.collection.immutable.Map[String, Array[String]] = {
    val retVals = scala.collection.mutable.Map[String, Array[String]]()
    if (distributionmap != None && distributionmap != null) {
      val nodeDistMap = distributionmap.get.filter(ndistmap => { (ndistmap.Node.compareToIgnoreCase(nodeId) == 0) })
      if (nodeDistMap != None && nodeDistMap != null) { // At most 1 value, but not enforcing
        nodeDistMap.foreach(ndistmap => {
          ndistmap.Adaps.map(ndm => {
            retVals(ndm.Adap) = ndm.Parts.toArray
          })
        })
      }
    }

    retVals.toMap
  }
*/

  private def GetDistMapForNodeId(distributionmap: Option[List[DistributionMap]], nodeId: String): scala.collection.immutable.Map[String, Array[(String, String)]] = {
    val retVals = scala.collection.mutable.Map[String, Array[(String, String)]]()
    if (distributionmap != None && distributionmap != null) {
      val nodeDistMap = distributionmap.get.filter(ndistmap => { (ndistmap.Node.compareToIgnoreCase(nodeId) == 0) })
      if (nodeDistMap != None && nodeDistMap != null) { // At most 1 value, but not enforcing
        nodeDistMap.foreach(ndistmap => {
          ndistmap.Adaps.map(ndm => {
            retVals(ndm.Adap) = ndm.Parts.map(a => (a.Key, a.KeyValue)).toArray
          })
        })
      }
    }

    retVals.toMap
  }

  // Using canRedistribute as startup mechanism here, because until we do bootstap ignore all the messages from this 
  private def ActionOnAdaptersDistImpl(receivedJsonStr: String): Unit = lock.synchronized {
    if (LOG.isDebugEnabled)
      LOG.debug("ActionOnAdaptersDistImpl => receivedJsonStr: " + receivedJsonStr)

    if (receivedJsonStr == null || receivedJsonStr.size == 0 || canRedistribute == false) {
      // nothing to do
      if (LOG.isDebugEnabled)
        LOG.debug("ActionOnAdaptersDistImpl => Exit. receivedJsonStr: " + receivedJsonStr)
      return
    }

    if (IsLeaderNodeAndUpdatePartitionsFlagSet) {
      if (LOG.isDebugEnabled)
        LOG.debug("Already got Re-distribution request. Ignoring any actions from ActionOnAdaptersDistImpl") // Atleast this happens on main node
      return
    }

    if (LOG.isInfoEnabled)
      LOG.info("ActionOnAdaptersDistImpl => receivedJsonStr: " + receivedJsonStr)

    ActionOnDistCommit(receivedJsonStr)

    if (LOG.isDebugEnabled)
      LOG.debug("ActionOnAdaptersDistImpl => Exit. receivedJsonStr: " + receivedJsonStr)
  }

  /*
  
  private def ActionOnDist(receivedJsonStr: String): Unit = {
    try {
      // Perform the action here (STOP or DISTRIBUTE for now)
      val json = parse(receivedJsonStr)
      if (json == null || json.values == null) { // Not doing any action if not found valid json
        LOG.error("ActionOnAdaptersDistImpl => Exit. receivedJsonStr: " + receivedJsonStr)
        return
      }

      implicit val jsonFormats: Formats = DefaultFormats
      val actionOnAdaptersMap = json.extract[ActionOnAdaptersMap]

      actionOnAdaptersMap.action match {
        case "stop" => {
          try {
            var remInputAdaps = inputAdapters.toArray
            var failedWaitTime = 15000 // Wait time starts at 15 secs
            val maxFailedWaitTime = 60000 // Max Wait time 60 secs
            val maxTries = 5
            var tryNo = 0

            //    ===============  writeAllPartitions

            while (remInputAdaps.size > 0 && tryNo < maxTries) { // maximum trying only 5 times
              tryNo += 1
              var failedInputAdaps = ArrayBuffer[InputAdapter]()

              // STOP all Input Adapters on local node
              remInputAdaps.foreach(ia => {
                try {
                  LOG.warn("Stopping adapter " + ia.UniqueName)
                  ia.StopProcessing
                  LOG.warn("Stopped adapter " + ia.UniqueName)
                } catch {
                  case fae: FatalAdapterException => {
                    LOG.error("Input adapter " + ia.UniqueName + "failed to stop processing", fae)
                    failedInputAdaps += ia
                  }
                  case e: Exception => {
                    LOG.error("Input adapter " + ia.UniqueName + "failed to stop processing", e)
                    failedInputAdaps += ia
                  }
                  case e: Throwable => {
                    LOG.error("Input adapter " + ia.UniqueName + "failed to stop processing", e)
                    failedInputAdaps += ia
                  }
                }
              })

              remInputAdaps = failedInputAdaps.toArray
              if (remInputAdaps.size > 0 && distributionExecutor.isShutdown == false) {
                try {
                  LOG.error("Failed to stop %d input adapters. Waiting for another %d milli seconds and going to start them again.".format(remInputAdaps.size, failedWaitTime))
                  Thread.sleep(failedWaitTime)
                } catch {
                  case e: Exception => { LOG.warn("", e) }
                }
                // Adjust time for next time
                if (failedWaitTime < maxFailedWaitTime) {
                  failedWaitTime = failedWaitTime * 2
                  if (failedWaitTime > maxFailedWaitTime)
                    failedWaitTime = maxFailedWaitTime
                }
              }
              if (distributionExecutor.isShutdown) // If it is shutting down, no more retries
                tryNo = maxTries
            }

            // Sleep for a sec
            try {
              Thread.sleep(1000)
            } catch {
              case e: Exception => {
                // Not doing anything
                LOG.debug("", e)
              }
            }

            if (KamanjaManager.instance != null) {
              if (KamanjaMetadata.gNodeContext != null && !KamanjaMetadata.gNodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0) {
                val envCtxts = KamanjaManager.instance.GetEnvCtxts
                LOG.warn("Running CommitPartitionOffsetIfNeeded for " + envCtxts.length + " envCtxts while doing stop")
                envCtxts.map(ctxt => {
                  try {
                    if (ctxt != null)
                      ctxt.CommitPartitionOffsetIfNeeded(true)
                  } catch {
                    case e: Throwable => {
                      LOG.error("Failed to commit partitions offsets", e)
                    }
                  }
                })
              }
              KamanjaManager.instance.ClearExecContext()
              KamanjaManager.instance.RecreateExecCtxtsCommitPartitionOffsetPool()
            }

            // Write all adapters end points in case if requested
            if (saveEndOffsets) {
              saveEndOffsets = false
              inputAdapters.foreach(ia => {
                try {
                  LOG.warn("Start Writing End Partition Values for adapter " + ia.UniqueName)
                  val endVals = ia.getAllPartitionEndValues
                  if (endVals != null) {
                    endVals.foreach(kv => {
                      var key: String = null
                      var value: String = null
                      if (kv != null && kv._1 != null && kv._2 != null) {
                        try {
                          key = kv._1.Serialize
                          value = kv._2.Serialize
                          if (key != null && value != null && key.size > 0 && value.size > 0) {
                            envCtxt.setAdapterUniqueKeyValue(key, value)
                            // LOG.warn("===============> Partition Key:%s, Partition Value:%s".format(key, value))
                          }
                        } catch {
                          case e: Throwable => {}
                        }
                      }
                    })
                  }
                  LOG.warn("Done Writing End Partition Values for adapter " + ia.UniqueName)
                } catch {
                  case e: Throwable => {
                    LOG.error("Input adapter " + ia.UniqueName + "failed to write end partition values", e)
                  }
                }
              })
            }

            if (distributionExecutor.isShutdown == false) {
              // Save the state and Clear the maps
              //              ProcessedAdaptersInfo.CommitAdapterValues
              ProcessedAdaptersInfo.clearInstances
              // envCtxt.PersistLocalNodeStateEntries
              //              envCtxt.clearIntermediateResults

              // Set STOPPED action in adaptersStatusPath + "/" + nodeId path
              val adaptrStatusPathForNode = adaptersStatusPath + "/" + envCtxt.getNodeIdAndUUID()
              var sendJson: String = ""
              if (KamanjaMetadata.gNodeContext != null && !KamanjaMetadata.gNodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0) {
                val nodeId: String = KamanjaMetadata.gNodeContext.getEnvCtxt().getNodeId()
                val writeLocation = KamanjaConfiguration.adapterInfoWriteLocation
               
                val localData = AdapterPartitionInfoUtil.readfromFile(nodeId, writeLocation)
                val act = ("action" -> "stopped") ~ ("PartitionsInfo" -> localData)
                sendJson = compact(render(act))
              } else {
                val act = ("action" -> "stopped")
                sendJson = compact(render(act))
              }
              LOG.warn("New Action Stopped set to " + adaptrStatusPathForNode)
              SetNewDataToZkc(adaptrStatusPathForNode, sendJson.getBytes("UTF8"))
            }
          } catch {
            case e: Exception => {
              LOG.error("Failed to get Input Adapters partitions.", e)
            }
          }
        }
        case "distribute" => {
          //          envCtxt.clearIntermediateResults // We may not need it here. But anyway safe side
          // Clear the maps
          ProcessedAdaptersInfo.clearInstances
          var distributed = true
          try {
            // get Unique Keys for this nodeId
            // Distribution Map 
            if (actionOnAdaptersMap.distributionmap != None && actionOnAdaptersMap.distributionmap != null) {
              val adapMaxPartsMap = GetAdaptersMaxPartitioinsMap(actionOnAdaptersMap.adaptermaxpartitions)
              val nodeDistMap = GetDistMapForNodeId(actionOnAdaptersMap.distributionmap, envCtxt.getNodeIdAndUUID())

              var foundKeysInVald = scala.collection.mutable.Map[String, (String, Int, Int, Long)]()

              //              if (actionOnAdaptersMap.foundKeysInValidation != None && actionOnAdaptersMap.foundKeysInValidation != null) {
              //                actionOnAdaptersMap.foundKeysInValidation.get.foreach(ks => {
              //                  foundKeysInVald(ks.K.toLowerCase) = (ks.V1, ks.V2, ks.V3, ks.V4)
              //                })
              //
              //              }
              StartNodeKeysMap(nodeDistMap, receivedJsonStr, adapMaxPartsMap, foundKeysInVald.toMap)
            }

          } catch {
            case e: Exception => {
              LOG.error("distribute action failed", e)
              distributed = false
            }
          }

          val adaptrStatusPathForNode = adaptersStatusPath + "/" + envCtxt.getNodeIdAndUUID()
          var sentDistributed = false
          if (distributed) {
            try {
              // Set DISTRIBUTED action in adaptersStatusPath + "/" + nodeId path
              val act = ("action" -> "distributed")
              val sendJson = compact(render(act))
              SetNewDataToZkc(adaptrStatusPathForNode, sendJson.getBytes("UTF8"))
              sentDistributed = true
            } catch {
              case e: Exception => {
                LOG.error("distribute action failed", e)
              }
            }
          }

          if (sentDistributed == false) {
            // Set RE-DISTRIBUTED action in adaptersStatusPath + "/" + nodeId path
            val act = ("action" -> "re-distribute")
            val sendJson = compact(render(act))
            SetNewDataToZkc(adaptrStatusPathForNode, sendJson.getBytes("UTF8"))
          }
        }
        case _ => {
          LOG.debug("No action performed, because of invalid action %s in json %s".format(actionOnAdaptersMap.action, receivedJsonStr))
        }
      }
    } catch {
      case e: Exception => {
        LOG.error("Found invalid JSON: %s".format(receivedJsonStr), e)
      }
    }
  }
  */

  private def ActionOnDistCommit(receivedJsonStr: String): Unit = {

    try {
      // Perform the action here (STOP or DISTRIBUTE for now)
      val json = parse(receivedJsonStr)
      if (json == null || json.values == null) { // Not doing any action if not found valid json
        LOG.error("ActionOnAdaptersDistImpl => Exit. receivedJsonStr: " + receivedJsonStr)
        return
      }

      implicit val jsonFormats: Formats = DefaultFormats
      val actionOnAdaptersMap = json.extract[ActionOnAdaptersMap]

      actionOnAdaptersMap.action match {
        case "stop" => {
          try {
            var remInputAdaps = inputAdapters.toArray
            var failedWaitTime = 15000 // Wait time starts at 15 secs
            val maxFailedWaitTime = 60000 // Max Wait time 60 secs
            val maxTries = 5
            var tryNo = 0

            //     writeAllPartitions to local drive
            writeAdapPartitionInfoToNodes(allPartitions)

            while (remInputAdaps.size > 0 && tryNo < maxTries) { // maximum trying only 5 times
              tryNo += 1
              var failedInputAdaps = ArrayBuffer[InputAdapter]()

              // STOP all Input Adapters on local node
              remInputAdaps.foreach(ia => {
                try {
                  LOG.warn("Stopping adapter " + ia.UniqueName)
                  ia.StopProcessing
                  LOG.warn("Stopped adapter " + ia.UniqueName)
                } catch {
                  case fae: FatalAdapterException => {
                    LOG.error("Input adapter " + ia.UniqueName + "failed to stop processing", fae)
                    failedInputAdaps += ia
                  }
                  case e: Exception => {
                    LOG.error("Input adapter " + ia.UniqueName + "failed to stop processing", e)
                    failedInputAdaps += ia
                  }
                  case e: Throwable => {
                    LOG.error("Input adapter " + ia.UniqueName + "failed to stop processing", e)
                    failedInputAdaps += ia
                  }
                }
              })

              remInputAdaps = failedInputAdaps.toArray
              if (remInputAdaps.size > 0 && distributionExecutor.isShutdown == false) {
                try {
                  LOG.error("Failed to stop %d input adapters. Waiting for another %d milli seconds and going to start them again.".format(remInputAdaps.size, failedWaitTime))
                  Thread.sleep(failedWaitTime)
                } catch {
                  case e: Exception => { LOG.warn("", e) }
                }
                // Adjust time for next time
                if (failedWaitTime < maxFailedWaitTime) {
                  failedWaitTime = failedWaitTime * 2
                  if (failedWaitTime > maxFailedWaitTime)
                    failedWaitTime = maxFailedWaitTime
                }
              }
              if (distributionExecutor.isShutdown) // If it is shutting down, no more retries
                tryNo = maxTries
            }

            // Sleep for a sec
            try {
              Thread.sleep(1000)
            } catch {
              case e: Exception => {
                // Not doing anything
                LOG.debug("", e)
              }
            }

            if (KamanjaManager.instance != null) {
              if (KamanjaMetadata.gNodeContext != null && !KamanjaMetadata.gNodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0) {
                val envCtxts = KamanjaManager.instance.GetEnvCtxts
                LOG.warn("Running CommitPartitionOffsetIfNeeded for " + envCtxts.length + " envCtxts while doing stop")
                envCtxts.map(ctxt => {
                  try {
                    if (ctxt != null)
                      ctxt.CommitPartitionOffsetIfNeeded(true)
                  } catch {
                    case e: Throwable => {
                      LOG.error("Failed to commit partitions offsets", e)
                    }
                  }
                })
              }
              KamanjaManager.instance.ClearExecContext()
              KamanjaManager.instance.RecreateExecCtxtsCommitPartitionOffsetPool()
            }

            // Write all adapters end points in case if requested
            if (saveEndOffsets) {
              saveEndOffsets = false
              inputAdapters.foreach(ia => {
                try {
                  LOG.warn("Start Writing End Partition Values for adapter " + ia.UniqueName)
                  val endVals = ia.getAllPartitionEndValues
                  if (endVals != null) {
                    endVals.foreach(kv => {
                      var key: String = null
                      var value: String = null
                      if (kv != null && kv._1 != null && kv._2 != null) {
                        try {
                          key = kv._1.Serialize
                          value = kv._2.Serialize
                          if (key != null && value != null && key.size > 0 && value.size > 0) {
                            envCtxt.setAdapterUniqueKeyValue(key, value)
                            // LOG.warn("===============> Partition Key:%s, Partition Value:%s".format(key, value))
                          }
                        } catch {
                          case e: Throwable => {}
                        }
                      }
                    })
                  }
                  LOG.warn("Done Writing End Partition Values for adapter " + ia.UniqueName)
                } catch {
                  case e: Throwable => {
                    LOG.error("Input adapter " + ia.UniqueName + "failed to write end partition values", e)
                  }
                }
              })
            }

            if (distributionExecutor.isShutdown == false) {
              // Save the state and Clear the maps
              //              ProcessedAdaptersInfo.CommitAdapterValues
              ProcessedAdaptersInfo.clearInstances
              // envCtxt.PersistLocalNodeStateEntries
              //              envCtxt.clearIntermediateResults

              // Set STOPPED action in adaptersStatusPath + "/" + nodeId path
              val adaptrStatusPathForNode = adaptersStatusPath + "/" + envCtxt.getNodeIdAndUUID()
              var sendJson: String = ""
              if (KamanjaMetadata.gNodeContext != null && !KamanjaMetadata.gNodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0) {
                val nodeId: String = KamanjaMetadata.gNodeContext.getEnvCtxt().getNodeId()
                val writeLocation = KamanjaConfiguration.adapterInfoWriteLocation

                val localData = AdapterPartitionInfoUtil.readfromFile(nodeId, writeLocation)
                val act = ("action" -> "stopped") ~ ("PartitionsInfo" -> localData)
                sendJson = compact(render(act))
              } else {
                val act = ("action" -> "stopped")
                sendJson = compact(render(act))
              }
              LOG.warn("New Action Stopped set to " + adaptrStatusPathForNode)
              SetNewDataToZkc(adaptrStatusPathForNode, sendJson.getBytes("UTF8"))
            }
          } catch {
            case e: Exception => {
              LOG.error("Failed to get Input Adapters partitions.", e)
            }
          }
        }
        case "distribute" => {
          //          envCtxt.clearIntermediateResults // We may not need it here. But anyway safe side
          // Clear the maps
          ProcessedAdaptersInfo.clearInstances
          var distributed = true
          try {
            // get Unique Keys for this nodeId
            // Distribution Map 
            if (actionOnAdaptersMap.distributionmap != None && actionOnAdaptersMap.distributionmap != null) {
              val adapMaxPartsMap = GetAdaptersMaxPartitioinsMap(actionOnAdaptersMap.adaptermaxpartitions)
              val nodeDistMap = GetDistMapForNodeId(actionOnAdaptersMap.distributionmap, envCtxt.getNodeIdAndUUID())

              var foundKeysInVald = scala.collection.mutable.Map[String, (String, Int, Int, Long)]()

              //              if (actionOnAdaptersMap.foundKeysInValidation != None && actionOnAdaptersMap.foundKeysInValidation != null) {
              //                actionOnAdaptersMap.foundKeysInValidation.get.foreach(ks => {
              //                  foundKeysInVald(ks.K.toLowerCase) = (ks.V1, ks.V2, ks.V3, ks.V4)
              //                })
              //
              //              }

              //populate all paritions with distributemap and write to local drive
              actionOnAdaptersMap.distributionmap.foreach(distList => {
                if (distList != null && distList.size > 0) {
                  distList.foreach(dist => {
                    if (dist.Node != null && dist.Node.size > 0) {
                      // val nodeJson = parse(dist.Node)
                      // if (nodeJson == null || nodeJson.values == null) // Not doing any action if not found valid json
                      //   return

                      //  val nodevalues = nodeJson.values.asInstanceOf[Map[String, String]]
                      //  val nodeid = nodevalues.getOrElse("NodeId", "").toString.toLowerCase

                      val (nodeid, uuid) = extractNodeIdAndUUId(dist.Node)
                      if (dist.Adaps != null && dist.Adaps.size > 0) {
                        dist.Adaps.foreach(adap => {
                          val parts = adap.Parts
                          if (parts != null && parts.size > 0) {
                            parts.foreach(part => {
                              allPartitions(part.Key) = (part.KeyValue, nodeid, part.UUID, part.NodeStartTime, part.Counter)
                            })
                          }
                        })
                      }
                    }
                  })
                }
              })
              if (LOG.isDebugEnabled()) LOG.debug("All Paritions" + allPartitions)
              writeAdapPartitionInfoToNodes(allPartitions)
              StartNodeKeysMap(nodeDistMap, receivedJsonStr, adapMaxPartsMap, foundKeysInVald.toMap)
            }

          } catch {
            case e: Exception => {
              LOG.error("distribute action failed", e)
              distributed = false
            }
          }

          val adaptrStatusPathForNode = adaptersStatusPath + "/" + envCtxt.getNodeIdAndUUID()
          var sentDistributed = false
          if (distributed) {
            try {
              // Set DISTRIBUTED action in adaptersStatusPath + "/" + nodeId path
              val act = ("action" -> "distributed")
              val sendJson = compact(render(act))
              SetNewDataToZkc(adaptrStatusPathForNode, sendJson.getBytes("UTF8"))
              sentDistributed = true
            } catch {
              case e: Exception => {
                LOG.error("distribute action failed", e)
              }
            }
          }

          if (sentDistributed == false) {
            // Set RE-DISTRIBUTED action in adaptersStatusPath + "/" + nodeId path
            val act = ("action" -> "re-distribute")
            val sendJson = compact(render(act))
            SetNewDataToZkc(adaptrStatusPathForNode, sendJson.getBytes("UTF8"))
          }
        }
        case _ => {
          LOG.debug("No action performed, because of invalid action %s in json %s".format(actionOnAdaptersMap.action, receivedJsonStr))
        }
      }
    } catch {
      case e: Exception => {
        LOG.error("Found invalid JSON: %s".format(receivedJsonStr), e)
      }
    }

    // 

  }

  private def ActionOnAdaptersDistribution(receivedJsonStr: String): Unit = {
    ActionOnAdaptersDistImpl(receivedJsonStr)
  }

  private def ActionOnDataChngImpl(receivedJsonStr: String): Unit = lock.synchronized {
    if (LOG.isDebugEnabled)
      LOG.debug("ActionOnDataChngImpl => receivedJsonStr: " + receivedJsonStr)
    if (receivedJsonStr == null || receivedJsonStr.size == 0) {
      // nothing to do
      if (LOG.isDebugEnabled)
        LOG.debug("ActionOnDataChngImpl => Exit. receivedJsonStr: " + receivedJsonStr)
      return
    }

    try {
      // Perform the action here
      val json = parse(receivedJsonStr)
      if (json == null || json.values == null) { // Not doing any action if not found valid json
        LOG.error("ActionOnAdaptersDistImpl => Exit. receivedJsonStr: " + receivedJsonStr)
        return
      }

      val values = json.values.asInstanceOf[Map[String, Any]]
      //      val changedMsgsContainers = values.getOrElse("changeddata", null)
      val tmpChngdContainersAndKeys = values.getOrElse("changeddatakeys", null)

      //      if (changedMsgsContainers != null) {
      //        // Expecting List/Array of String here
      //        var changedVals: Array[String] = null
      //        if (changedMsgsContainers.isInstanceOf[List[_]]) {
      //          try {
      //            changedVals = changedMsgsContainers.asInstanceOf[List[String]].toArray
      //          } catch {
      //            case e: Exception => { LOG.warn("", e) }
      //          }
      //        } else if (changedMsgsContainers.isInstanceOf[Array[_]]) {
      //          try {
      //            changedVals = changedMsgsContainers.asInstanceOf[Array[String]]
      //          } catch {
      //            case e: Exception => { LOG.warn("", e) }
      //          }
      //        }
      //
      //        if (changedVals != null) {
      //          envCtxt.clearIntermediateResults(changedVals)
      //        }
      //      }
      //
      if (tmpChngdContainersAndKeys != null) {
        val changedContainersAndKeys = if (tmpChngdContainersAndKeys.isInstanceOf[List[_]]) tmpChngdContainersAndKeys.asInstanceOf[List[_]] else if (tmpChngdContainersAndKeys.isInstanceOf[Array[_]]) tmpChngdContainersAndKeys.asInstanceOf[Array[_]].toList else null
        if (changedContainersAndKeys != null && changedContainersAndKeys.size > 0) {
          val txnid = values.getOrElse("txnid", "0").toString.trim.toLong // txnid is 0, if it is not passed
          changedContainersAndKeys.foreach(CK => {
            if (CK != null && CK.isInstanceOf[Map[_, _]]) {
              val contAndKeys = CK.asInstanceOf[Map[String, Any]]
              val contName = contAndKeys.getOrElse("C", "").toString.trim

              var tenatId: String = ""
              val msg = mdMgr.Message(contName, -1, true)
              if (msg == None) {
                val container = mdMgr.Container(contName, -1, true)
                if (container != None)
                  tenatId = container.get.tenantId
              } else {
                tenatId = msg.get.tenantId
              }

              val tmpKeys = contAndKeys.getOrElse("K", null)
              if (contName.size > 0 && tmpKeys != null && tenatId != null && tenatId.size > 0) {
                // Expecting List/Array of Keys
                var keys: List[Any] = null
                if (tmpKeys.isInstanceOf[List[_]]) {
                  try {
                    keys = tmpKeys.asInstanceOf[List[Any]]
                  } catch {
                    case e: Exception => { LOG.warn("", e) }
                  }
                } else if (tmpKeys.isInstanceOf[Array[_]]) {
                  try {
                    keys = tmpKeys.asInstanceOf[Array[Any]].toList
                  } catch {
                    case e: Exception => { LOG.warn("", e) }
                  }
                } else if (tmpKeys.isInstanceOf[Map[_, _]]) {
                  try {
                    keys = tmpKeys.asInstanceOf[Map[String, Any]].toList
                  } catch {
                    case e: Exception => { LOG.warn("", e) }
                  }
                } else if (tmpKeys.isInstanceOf[scala.collection.mutable.Map[_, _]]) {
                  try {
                    keys = tmpKeys.asInstanceOf[scala.collection.mutable.Map[String, Any]].toList
                  } catch {
                    case e: Exception => { LOG.warn("", e) }
                  }
                }

                if (keys != null && keys.size > 0) {
                  var loadableKeys = ArrayBuffer[Key]()
                  val ks = keys.map(k => {
                    var oneKey: Map[String, Any] = null
                    if (k.isInstanceOf[List[_]]) {
                      try {
                        oneKey = k.asInstanceOf[List[(String, Any)]].toMap
                      } catch {
                        case e: Exception => { LOG.warn("", e) }
                      }
                    } else if (k.isInstanceOf[Array[_]]) {
                      try {
                        oneKey = k.asInstanceOf[Array[(String, Any)]].toMap
                      } catch {
                        case e: Exception => { LOG.warn("", e) }
                      }
                    } else if (k.isInstanceOf[Map[_, _]]) {
                      try {
                        oneKey = k.asInstanceOf[Map[String, Any]]
                      } catch {
                        case e: Exception => { LOG.warn("", e) }
                      }
                    } else if (k.isInstanceOf[scala.collection.mutable.Map[_, _]]) {
                      try {
                        oneKey = k.asInstanceOf[scala.collection.mutable.Map[String, Any]].toMap
                      } catch {
                        case e: Exception => { LOG.warn("", e) }
                      }
                    }

                    if (oneKey != null) {
                      val bk = oneKey.getOrElse("bk", null)
                      if (bk != null) {
                        val tm = oneKey.getOrElse("tm", "0").toString().toLong
                        val tx = oneKey.getOrElse("tx", "0").toString().toLong
                        val rid = oneKey.getOrElse("rid", "0").toString().toInt
                        loadableKeys += Key(tm, bk.asInstanceOf[List[String]].toArray, tx, rid)
                      }
                    }
                  })

                  if (loadableKeys.size > 0) {
                    try {
                      logger.debug("Loading Keys => Txnid:%d, ContainerName:%s, Keys:%s".format(txnid, contName, loadableKeys.map(k => (k.timePartition, k.bucketKey.mkString("="), k.transactionId, k.rowId)).mkString(",")))
                      envCtxt.ReloadKeys(txnid, tenatId, contName, loadableKeys.toList)
                    } catch {
                      case e: Exception => {
                        logger.error("Failed to reload keys for container:" + contName, e)
                      }
                      case t: Throwable => {
                        logger.error("Failed to reload keys for container:" + contName, t)
                      }
                    }
                  }
                }
              }
            } // else // not handling              if (contName.size > 0 && tmpKeys != null) {
            // Expecting List/Array of Keys
            //            var keys: List[Any] = null
            //            if (tmpKeys.isInstanceOf[List[_]]) {
            //              try {
            //                keys = tmpKeys.asInstanceOf[List[Any]]
            //              } catch {
            //                case e: Exception => { LOG.warn("", e) }
            //              }
            //            } else if (tmpKeys.isInstanceOf[Array[_]]) {
            //              try {
            //                keys = tmpKeys.asInstanceOf[Array[Any]].toList
            //              } catch {
            //                case e: Exception => { LOG.warn("", e) }
            //              }
            //            } else if (tmpKeys.isInstanceOf[Map[_, _]]) {
            //              try {
            //                keys = tmpKeys.asInstanceOf[Map[String, Any]].toList
            //              } catch {
            //                case e: Exception => { LOG.warn("", e) }
            //              }
            //            } else if (tmpKeys.isInstanceOf[scala.collection.mutable.Map[_, _]]) {
            //              try {
            //                keys = tmpKeys.asInstanceOf[scala.collection.mutable.Map[String, Any]].toList
            //              } catch {
            //                case e: Exception => { LOG.warn("", e) }
            //              }
            //            }
            //
            //            if (keys != null && keys.size > 0) {
            //              var loadableKeys = ArrayBuffer[Key]()
            //              val ks = keys.map(k => {
            //                var oneKey: Map[String, Any] = null
            //                if (k.isInstanceOf[List[_]]) {
            //                  try {
            //                    oneKey = k.asInstanceOf[List[(String, Any)]].toMap
            //                  } catch {
            //                    case e: Exception => { LOG.warn("", e) }
            //                  }
            //                } else if (k.isInstanceOf[Array[_]]) {
            //                  try {
            //                    oneKey = k.asInstanceOf[Array[(String, Any)]].toMap
            //                  } catch {
            //                    case e: Exception => { LOG.warn("", e) }
            //                  }
            //                } else if (k.isInstanceOf[Map[_, _]]) {
            //                  try {
            //                    oneKey = k.asInstanceOf[Map[String, Any]]
            //                  } catch {
            //                    case e: Exception => { LOG.warn("", e) }
            //                  }
            //                } else if (k.isInstanceOf[scala.collection.mutable.Map[_, _]]) {
            //                  try {
            //                    oneKey = k.asInstanceOf[scala.collection.mutable.Map[String, Any]].toMap
            //                  } catch {
            //                    case e: Exception => { LOG.warn("", e) }
            //                  }
            //                }
            //
            //                if (oneKey != null) {
            //                  val bk = oneKey.getOrElse("bk", null)
            //                  if (bk != null) {
            //                    val tm = oneKey.getOrElse("tm", "0").toString().toLong
            //                    val tx = oneKey.getOrElse("tx", "0").toString().toLong
            //                    val rid = oneKey.getOrElse("rid", "0").toString().toInt
            //                    loadableKeys += Key(tm, bk.asInstanceOf[List[String]].toArray, tx, rid)
            //                  }
            //                }
            //              })
            //
            //              if (loadableKeys.size > 0) {
            //                try {
            //                  logger.debug("Loading Keys => Txnid:%d, ContainerName:%s, Keys:%s".format(txnid, contName, loadableKeys.map(k => (k.timePartition, k.bucketKey.mkString("="), k.transactionId, k.rowId)).mkString(",")))
            //                  envCtxt.ReloadKeys(txnid, contName, loadableKeys.toList)
            //                } catch {
            //                  case e: Exception => {
            //                    logger.error("Failed to reload keys for container:" + contName, e)
            //                  }
            //                  case t: Throwable => {
            //                    logger.error("Failed to reload keys for container:" + contName, t)
            //                  }
            //                }
            //              }
            //            }
            //
            //
            //          }
            //        } // else // not handling
            //
            //
          })
        }
      }

      // 
    } catch {
      case e: Exception => {
        LOG.error("Found invalid JSON: %s".format(receivedJsonStr), e)
      }
    }

    if (LOG.isDebugEnabled)
      LOG.debug("ActionOnDataChngImpl => Exit. receivedJsonStr: " + receivedJsonStr)
  }

  private def ActionOnDataChange(receivedJsonStr: String): Unit = {
    ActionOnDataChngImpl(receivedJsonStr)
  }

  private def ParticipentsAdaptersStatus(eventType: String, eventPath: String, eventPathData: Array[Byte], childs: Array[(String, Array[Byte])]): Unit = {
    if (LOG.isDebugEnabled)
      LOG.debug("ParticipentsAdaptersStatus => Enter, eventType:%s, eventPath:%s ".format(eventType, eventPath))
    if (IsLeaderNode == false) { // Not Leader node
      if (LOG.isDebugEnabled)
        LOG.debug("ParticipentsAdaptersStatus => Exit, eventType:%s, eventPath:%s ".format(eventType, eventPath))
      return
    }

    if (IsLeaderNodeAndUpdatePartitionsFlagSet) {
      if (LOG.isDebugEnabled)
        LOG.debug("Already got Re-distribution request. Ignoring any actions from ParticipentsAdaptersStatus")
      return
    }

    UpdatePartitionsNodeData(eventType, eventPath, eventPathData)
    if (LOG.isDebugEnabled)
      LOG.debug("ParticipentsAdaptersStatus => Exit, eventType:%s, eventPath:%s ".format(eventType, eventPath))
  }

  private def CheckForPartitionsChange: Unit = {
    if (inputAdapters != null) {
      var updFlag = false
      var i = 0
      try {
        while (!updFlag && i < inputAdapters.size) {
          val ia = inputAdapters(i)
          i += 1
          try {
            val uk = ia.GetAllPartitionUniqueRecordKey
            val name = ia.UniqueName
            val ukCnt = if (uk != null) uk.size else 0
            val prevParts = GetPartitionsToValidate(name)
            val prevCnt = if (prevParts != null) prevParts.size else 0
            if (prevCnt != ukCnt) {
              // Number of partitions does not match
              LOG.warn("Number of partitions changed from %d to %d for %s. Going to redistribute the work".format(prevCnt, ukCnt, ia.UniqueName))
              SetUpdatePartitionsFlag
              updFlag = true
            }
            if (ukCnt > 0 && !updFlag) {
              // Check the real content
              val serUKSet = uk.map(k => {
                k.Serialize
              }).toSet
              if ((serUKSet -- prevParts).isEmpty == false) {
                LOG.warn("Partitions changed from for %s. Going to redistribute the work".format(ia.UniqueName))
                // Partition keys does not match
                SetUpdatePartitionsFlag
                updFlag = true
              }
              if (ukCnt > 0 && !updFlag) {
                // Check the real content
                val serUKSet = uk.map(k => {
                  k.Serialize
                }).toSet
                if ((serUKSet -- prevParts).isEmpty == false) {
                  // Partition keys does not match
                  SetUpdatePartitionsFlag
                  updFlag = true
                }
              }
            }
          } catch {
            case fae: FatalAdapterException => {
              // Adapter could not get partition information and can't reconver.
              LOG.error("Failed to get partitions from input adapter " + ia.UniqueName + ". We are not going to change work load for now", fae)
            }
            case e: Exception => {
              // Adapter could not get partition information and can't reconver.
              LOG.error("Failed to get partitions from input adapter " + ia.UniqueName + ". We are not going to change work load for now", e)
            }
            case e: Throwable => {
              // Adapter could not get partition information and can't reconver.
              LOG.error("Failed to get partitions from input adapter " + ia.UniqueName + ". We are not going to change work load for now", e)
            }
          }
        }
      } catch {
        case e: Exception => {
          LOG.error("Failed to get Input Adapters partitions.", e)
        }
      }
    }
  }
  /*
  private def GetEndPartitionsValuesForValidateAdapters: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = {
    val uniqPartKeysValues = ArrayBuffer[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]()

    if (validateInputAdapters != null) {
      var lastAdapterException: Throwable = null
      try {
        validateInputAdapters.foreach(ia => {
          try {
            val uKeysVals = ia.getAllPartitionEndValues
            uniqPartKeysValues ++= uKeysVals
          } catch {

            case e: FatalAdapterException => {
              lastAdapterException = e // Adapter could not partition information and can't recover.
              // If validate adapter is not able to connect, just ignoring it for now
              LOG.error("Failed to get partition values for Validate Adapter " + ia.UniqueName, e)
            }
            case e: Exception => {
              lastAdapterException = e // Adapter could not partition information and can't recover.
              // If validate adapter is not able to connect, just ignoring it for now
              LOG.error("Failed to get partition values for Validate Adapter " + ia.UniqueName, e)
            }
            case e: Throwable => {
              lastAdapterException = e // Adapter could not partition information and can't recover.
              // If validate adapter is not able to connect, just ignoring it for now
              LOG.error("Failed to get partition values for Validate Adapter " + ia.UniqueName, e)
            }
          }
        })
        // Weird, but we want to record an FatalAdapterException for each consumer and handle all other unexpected ones.
        if (lastAdapterException != null) throw lastAdapterException
      } catch {
        case e: Exception => {
          LOG.error("Failed to get Validate Input Adapters partitions.", e)
        }
      }
    }

    uniqPartKeysValues.toArray
  }
  */

  def forceAdapterRebalance: Unit = {
    SetUpdatePartitionsFlag
  }

  def forceAdapterRebalanceAndSetEndOffsets: Unit = {
    saveEndOffsets = true
    SetUpdatePartitionsFlag
  }

  private def extractNodeIdAndUUId(nodeIdAndUUIdStr: String): (String, String) = {
    var nodeId: String = null
    var uuId: String = null

    if (nodeIdAndUUIdStr.startsWith("NodeId-")) {
      val uuidKeyWordIdx = nodeIdAndUUIdStr.indexOf("-UUID-")
      nodeId = nodeIdAndUUIdStr.substring("NodeId-".length, uuidKeyWordIdx)
      uuId = nodeIdAndUUIdStr.substring(uuidKeyWordIdx + "-UUID-".length)
    }

    (nodeId, uuId)
  }

  def Init(nodeId1: String, zkConnectString1: String, engineLeaderZkNodePath1: String, engineDistributionZkNodePath1: String, adaptersStatusPath1: String, inputAdap: ArrayBuffer[InputAdapter], outputAdap: ArrayBuffer[OutputAdapter],
           storageAdap: ArrayBuffer[DataStore], enviCxt: EnvContext, zkSessionTimeoutMs1: Int, zkConnectionTimeoutMs1: Int, dataChangeZkNodePath1: String): Unit = {
    nodeId = nodeId1.toLowerCase
    zkConnectString = zkConnectString1
    engineLeaderZkNodePath = engineLeaderZkNodePath1
    engineDistributionZkNodePath = engineDistributionZkNodePath1
    dataChangeZkNodePath = dataChangeZkNodePath1
    adaptersStatusPath = adaptersStatusPath1
    zkSessionTimeoutMs = zkSessionTimeoutMs1
    zkConnectionTimeoutMs = zkConnectionTimeoutMs1
    inputAdapters = inputAdap
    outputAdapters = outputAdap
    storageAdapters = storageAdap
    envCtxt = enviCxt

    if (zkConnectString != null && zkConnectString.isEmpty() == false && engineLeaderZkNodePath != null && engineLeaderZkNodePath.isEmpty() == false && engineDistributionZkNodePath != null && engineDistributionZkNodePath.isEmpty() == false && dataChangeZkNodePath != null && dataChangeZkNodePath.isEmpty() == false) {
      try {
        val adaptrStatusPathForNode = adaptersStatusPath + "/" + envCtxt.getNodeIdAndUUID()
        LOG.info("ZK Connecting. adaptrStatusPathForNode:%s, zkConnectString:%s, engineLeaderZkNodePath:%s, engineDistributionZkNodePath:%s, dataChangeZkNodePath:%s".format(adaptrStatusPathForNode, zkConnectString, engineLeaderZkNodePath, engineDistributionZkNodePath, dataChangeZkNodePath))
        CreateClient.CreateNodeIfNotExists(zkConnectString, engineDistributionZkNodePath) // Creating 
        CreateClient.CreateNodeIfNotExists(zkConnectString, adaptrStatusPathForNode) // Creating path for Adapter Statues
        CreateClient.CreateNodeIfNotExists(zkConnectString, dataChangeZkNodePath) // Creating 
        zkcForSetData = CreateClient.createSimple(zkConnectString, zkSessionTimeoutMs, zkConnectionTimeoutMs)
        if (KamanjaMetadata.gNodeContext != null && !KamanjaMetadata.gNodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0) {

          // "Create Listener for PartitionsInfo
          KamanjaMetadata.gNodeContext.getEnvCtxt().createListenerForCacheChildern(updatePartitionInfoPath, updatePartitionsFn) // listen to start info
        }

        envCtxt.createZkPathChildrenCacheListener(adaptersStatusPath, false, ParticipentsAdaptersStatus)
        envCtxt.createZkPathListener(engineDistributionZkNodePath, ActionOnAdaptersDistribution)
        envCtxt.createZkPathListener(dataChangeZkNodePath, ActionOnDataChange)
        //        zkAdapterStatusNodeListener = new ZooKeeperListener
        //        zkAdapterStatusNodeListener.CreatePathChildrenCacheListener(zkConnectString, adaptersStatusPath, false, ParticipentsAdaptersStatus, zkSessionTimeoutMs, zkConnectionTimeoutMs)
        //        zkEngineDistributionNodeListener = new ZooKeeperListener
        //        zkEngineDistributionNodeListener.CreateListener(zkConnectString, engineDistributionZkNodePath, ActionOnAdaptersDistribution, zkSessionTimeoutMs, zkConnectionTimeoutMs)
        //        zkDataChangeNodeListener = new ZooKeeperListener
        //        zkDataChangeNodeListener.CreateListener(zkConnectString, dataChangeZkNodePath, ActionOnDataChange, zkSessionTimeoutMs, zkConnectionTimeoutMs)
        try {
          Thread.sleep(500)
        } catch {
          case e: Exception => {
            // Not doing anything
            LOG.debug("", e)
          }
        }

        distributionExecutor.execute(new Runnable() {
          override def run() = {
            var updatePartsCntr = 0
            var getValidateAdapCntr = 0
            var wait4ValidateCheck = 0
            var validateUniqVals: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = null

            var lastParticipentChngCntr: Long = 0
            var lastParticipentChngDistTime: Long = 0

            while (distributionExecutor.isShutdown == false) {
              try {
                Thread.sleep(1000) // Waiting for 1000 milli secs
              } catch {
                case e: Exception => {
                  LOG.debug("", e)
                }
              }

              copyClusterStatusIfNeeded

              if (LOG.isDebugEnabled())
                LOG.debug("DistributionCheck:Checking for Distribution information. IsLeaderNode:%s, GetUpdatePartitionsFlag:%s, distributionExecutor.isShutdown:%s".format(
                  IsLeaderNode.toString, GetUpdatePartitionsFlag.toString, distributionExecutor.isShutdown.toString))

              var execDefaultPath = true
              if (IsLeaderNode && GetUpdatePartitionsFlag && distributionExecutor.isShutdown == false) {
                val curParticipentChngCntr = KamanjaConfiguration.participentsChangedCntr
                LOG.warn("DistributionCheck:Found some change. IsLeaderNode:%s, GetUpdatePartitionsFlag:%s, distributionExecutor.isShutdown:%s, lastParticipentChngCntr:%d, curParticipentChngCntr:%d".format(
                  IsLeaderNode.toString, GetUpdatePartitionsFlag.toString, distributionExecutor.isShutdown.toString, lastParticipentChngCntr, curParticipentChngCntr))
                if (lastParticipentChngCntr != curParticipentChngCntr) {
                  lastParticipentChngCntr = curParticipentChngCntr
                  val cs = GetClusterStatus
                  var mxTm = 0

                  // Make sure we check the number of nodes participating in the node start (get number of nodes from metadata manager and if all of them are in participents, no need to wait more than 4-5 secs, other wait more time)
                  val mdMgr = GetMdMgr
                  var allNodesUp = false

                  if (mdMgr == null) {
                    LOG.warn("Got Redistribution request and not able to get metadata manager. Not going to check whether all nodes came up or not in participents {%s}.".format(cs.participantsNodeIds.mkString(",")))
                  } else {
                    val nodes = mdMgr.NodesForCluster(KamanjaConfiguration.clusterId)
                    if (nodes == null) {
                      LOG.warn("Got Redistribution request and not able to get nodes from metadata manager for cluster %s. Not going to check whether all nodes came up or not in participents {%s}.".format(KamanjaConfiguration.clusterId, cs.participantsNodeIds.mkString(",")))
                    } else {
                      val allParticipentNodeIds = cs.participantsNodeIds.map(nd => {
                        val (nodeId, uuId) = extractNodeIdAndUUId(nd)
                        nodeId
                      }).filter(nd => nd.size > 0)
                      val participentNodeIds = allParticipentNodeIds.toSet
                      // Check for nodes in participents now
                      allNodesUp = true
                      var i = 0
                      while (i < nodes.size && allNodesUp) {
                        if (participentNodeIds.contains(nodes(i).nodeId) == false)
                          allNodesUp = false
                        i += 1
                      }

                      val dupNodes = allParticipentNodeIds.groupBy(x => x).filter(kv => (kv._2.size > 1)).map(kv => kv._1)
                      if (dupNodes.size > 0) {
                        LOG.warn("Found duplicate %s in %s.".format(dupNodes.mkString(","), cs.participantsNodeIds.mkString(" ~ ")))
                      }

                      if (allNodesUp) {
                        // Check for duplicates if we have any in participents
                        // Just do group by and do get duplicates if we have any. If we have duplicates just make allNodesUp as false, so it will wait long time and by that time the duplicate node may go down.
                        // allNodesUp = (cs.participantsNodeIds.groupBy(x => x).mapValues(lst => lst.size).filter(kv => kv._2 > 1).size == 0)
                        allNodesUp = (dupNodes.size == 0)
                      }
                    }
                  }

                  if (allNodesUp == false) { // If all nodes are not up then wait for long time
                    // (/* zkConnectString, zkSessionTimeoutMs, zkConnectionTimeoutMs */) = envCtxt.getZookeeperInfo()
                    mxTm = if (zkSessionTimeoutMs > zkConnectionTimeoutMs) zkSessionTimeoutMs else zkConnectionTimeoutMs
                    if (mxTm < 5000) // if the value is < 5secs, we are taking 5 secs
                      mxTm = 5000
                    LOG.warn("Got Redistribution request. Participents are {%s}. Looks like all nodes are not yet up. Waiting for %d milli seconds to see whether there are any more changes in participents".format(cs.participantsNodeIds.mkString(","), mxTm))
                    lastParticipentChngDistTime = System.currentTimeMillis + mxTm + 5000 // waiting another 5secs
                    execDefaultPath = false
                  } else { // if all nodes are up, no need to wait any more
                    LOG.warn("All Participents are {%s} up. Going to distribute the work now".format(cs.participantsNodeIds.mkString(",")))
                  }
                } else if (lastParticipentChngDistTime > System.currentTimeMillis) {
                  // Still waiting to distribute
                  execDefaultPath = false
                }
              }

              if (execDefaultPath && distributionExecutor.isShutdown == false) {
                lastParticipentChngCntr = 0
                if (GetUpdatePartitionsFlagAndReset) {
                  UpdatePartitionsIfNeededOnLeader
                  wait4ValidateCheck = 180 // When ever rebalancing it should be 180 secs
                  updatePartsCntr = 0
                  getValidateAdapCntr = 0
                } else if (IsLeaderNode) {
                  if (GetUpdatePartitionsFlag == false) {
                    // Get Partitions for every N secs and see whether any partitions changes from previous get
                    if (updatePartsCntr >= 30) { // for every 30 secs
                      CheckForPartitionsChange
                      updatePartsCntr = 0
                    } else {
                      updatePartsCntr += 1
                    }
                  }
                } else {
                  wait4ValidateCheck = 0 // Not leader node, don't check for it until we set it in redistribute
                  getValidateAdapCntr = 0
                }
              }

              // Waiting for a two mins after issuing stopped to redo the same actions again
              val maxWaitForStop = 60000
              if (GetUpdatePartitionsFlag == false && expectedNodesAction == "stopped" && (nodesActionIssuedTime + maxWaitForStop) < System.currentTimeMillis) {
                // Redistribute. We did not get expected stopped message
                logger.warn(s"From past ${maxWaitForStop} milli second we are waiting for adapters redistribution. Did not get the information so far. Going to stop & distribute again.")
                SetUpdatePartitionsFlag
              }
            }
          }
        })

        SetCanRedistribute(true)
        logger.warn("DistributionCheck:Going to do registerNodesChangeNotification in KamanjaLeader")
        envCtxt.registerNodesChangeNotification(EventChangeCallback)
        logger.warn("DistributionCheck:Done registerNodesChangeNotification in KamanjaLeader")

        PostAdapterParitionInfoToLocalDrive()

        // Forcing to distribute
        // SetUpdatePartitionsFlag

        //          zkLeaderLatch = new ZkLeaderLatch(zkConnectString, engineLeaderZkNodePath, nodeId, EventChangeCallback, zkSessionTimeoutMs, zkConnectionTimeoutMs)
        //        zkLeaderLatch.SelectLeader
        /*
        // Set RE-DISTRIBUTED action in adaptersStatusPath + "/" + nodeId path
        val act = ("action" -> "re-distribute")
        val sendJson = compact(render(act))
        SetNewDataToZkc(adaptrStatusPathForNode, sendJson.getBytes("UTF8"))
        */
      } catch {
        case e: Exception => {
          LOG.error("Failed to initialize ZooKeeper Connection.", e)
          throw e
        }
      }
    } else {
      LOG.error("Not connected to elect Leader and not distributing data between nodes.")
    }
  }

  private def CloseSetDataZkc: Unit = {
    setDataLockObj.synchronized {
      if (zkcForSetData != null) {
        try {
          zkcForSetData.close
        } catch {
          case e: Throwable => {
            LOG.warn("KamanjaLeader: unable to close zk connection due to", e)
          }
        }
      }
      zkcForSetData = null
    }
  }

  private def ReconnectToSetDataZkc: Unit = {
    CloseSetDataZkc
    setDataLockObj.synchronized {
      try {
        zkcForSetData = CreateClient.createSimple(zkConnectString, zkSessionTimeoutMs, zkConnectionTimeoutMs)
      } catch {
        case e: Throwable => {
          LOG.warn("KamanjaLeader: unable to create new zk connection due to", e)
        }
      }
    }
  }

  def SetNewDataToZkc(zkNodePath: String, data: Array[Byte]): Unit = {

    var retriesAttempted = 0
    while (retriesAttempted <= MAX_ZK_RETRIES) {
      try {
        setDataLockObj.synchronized {

          if (retriesAttempted > 0) {
            LOG.warn("KamanjaLeader: retrying zk call (SetNewDataToZkc)")
          }

          if ((zkcForSetData != null)) {
            zkcForSetData.setData().forPath(zkNodePath, data)
            return
          } else
            throw new KamanjaException("Connection to ZK does not exists", null)
        }
      } catch {
        case e: Throwable => {
          retriesAttempted += 1
          LOG.warn("KamanjaLeader: Connection to Zookeeper is temporarily unavailable (SetNewDataToZkc).due to ", e)
          ReconnectToSetDataZkc
        }
      }
    }
    LOG.warn("KamanjaLeader: failed to conntect to Zookeeper after retry")
  }

  def GetDataFromZkc(zkNodePath: String): Array[Byte] = {

    var retriesAttempted = 0
    while (retriesAttempted <= MAX_ZK_RETRIES) {
      try {
        setDataLockObj.synchronized {
          if (retriesAttempted > 0) {
            LOG.warn("KamanjaLeader: retrying zk call (GetDataFromZkc)")
          }
          if (zkcForSetData != null)
            return zkcForSetData.getData().forPath(zkNodePath)
          // Bad juju here...
          throw new KamanjaException("Connection to ZK does not exists", null)
        }
      } catch {
        case e: Throwable => {
          retriesAttempted += 1
          LOG.warn("KamanjaLeader: Connection to Zookeeper is temporarily unavailable (GetDataFromZkc)..due to ", e)
          ReconnectToSetDataZkc
        }
      }
    }
    // The only way to get here is by catching an exceptions, retyring and then failing again.
    LOG.warn("KamanjaLeader: failed to conntect to Zookeeper after retry")
    return Array[Byte]()
  }

  def GetChildrenFromZkc(zkNodePath: String): List[String] = {
    var retriesAttempted = 0
    while (retriesAttempted <= MAX_ZK_RETRIES) {
      try {
        setDataLockObj.synchronized {
          if (retriesAttempted > 0) {
            LOG.warn("KamanjaLeader: retrying zk call (GetChildrenFromZkc)")
          }
          if (zkcForSetData != null)
            return zkcForSetData.getChildren().forPath(zkNodePath).toList
          // bad juju here
          throw new KamanjaException("Connection to ZK does not exists", null)
        }
      } catch {
        case e: Throwable => {
          retriesAttempted += 1
          LOG.warn("KamanjaLeader: Connection to Zookeeper is temporarily unavailable (GetChildrenFromZkc) due to ", e)
          ReconnectToSetDataZkc
        }
      }
    }
    // The only way to get here is by catching an exceptions, retyring and then failing again.
    LOG.warn("KamanjaLeader: failed to conntect to Zookeeper after retry")
    return List[String]()
  }

  def GetChildrenDataFromZkc(zkNodePath: String): List[(String, Array[Byte])] = {

    var retriesAttempted = 0
    while (retriesAttempted <= MAX_ZK_RETRIES) {
      try {
        setDataLockObj.synchronized {
          if (retriesAttempted > 0) {
            LOG.warn("KamanjaLeader: retrying zk call (GetChildrenDataFromZkc)")
          }
          if (zkcForSetData != null) {
            val children = zkcForSetData.getChildren().forPath(zkNodePath)
            return children.map(child => {
              val path = zkNodePath + "/" + child
              val chldData = zkcForSetData.getData().forPath(path)
              (child, chldData)
            }).toList
          }
          // bad juju here
          throw new KamanjaException("Connection to ZK does not exists", null)

        }
      } catch {
        case e: Throwable => {
          retriesAttempted += 1
          LOG.warn("KamanjaLeader: Connection to Zookeeper is temporarily unavailable (GetChildrenDataFromZkc).due to ", e)
          ReconnectToSetDataZkc
        }
      }
    }
    // The only way to get here is by catching an exceptions, retyring and then failing again.
    LOG.warn("KamanjaLeader: failed to conntect to Zookeeper after retry")
    return List[(String, Array[Byte])]()
  }

  def Shutdown: Unit = {
    execCtxtsAdapterInfoPool.shutdown()
    distributionExecutor.shutdown

    //    if (zkLeaderLatch != null)
    //      zkLeaderLatch.Shutdown
    //    zkLeaderLatch = null
    //    if (zkEngineDistributionNodeListener != null)
    //      zkEngineDistributionNodeListener.Shutdown
    //    zkEngineDistributionNodeListener = null
    //    if (zkDataChangeNodeListener != null)
    //      zkDataChangeNodeListener.Shutdown
    //    zkDataChangeNodeListener = null
    //    if (zkAdapterStatusNodeListener != null)
    //      zkAdapterStatusNodeListener.Shutdown
    //    zkAdapterStatusNodeListener = null
    CloseSetDataZkc
  }

  private def consolidatePartitionsMap(partitionInfoMap: Map[String, String], keyValueMap: scala.collection.mutable.Map[String, (String, String, String, Long, Long)]): scala.collection.mutable.Map[String, (String, String, String, Long, Long)] = {
    var consolidatedMap = scala.collection.mutable.Map[String, (String, String, String, Long, Long)]()

    try {

      if (LOG.isDebugEnabled()) LOG.debug("consolidatePartitionsMap - partitionInfoMap " + partitionInfoMap)
      if (LOG.isDebugEnabled()) LOG.debug("consolidatePartitionsMap - keyValueMap " + keyValueMap)

      val initialConsolidatedMap = getConsolidatedInitialMap(partitionInfoMap)
      if (LOG.isDebugEnabled()) LOG.debug("consolidatePartitionsMap - initialConsolidatedMap " + initialConsolidatedMap)

      val consolidateAllPartitions = getConsolidatedMap(initialConsolidatedMap, allPartitions)
      if (LOG.isDebugEnabled()) LOG.debug("consolidatePartitionsMap - consolidateAllPartitions " + consolidateAllPartitions)

      consolidatedMap = getConsolidatedMap(consolidateAllPartitions, keyValueMap)
      if (LOG.isDebugEnabled()) LOG.debug("consolidatePartitionsMap - consolidatedMap " + consolidatedMap)

    } catch {
      case e: Exception => LOG.error("Consolidated Map" + e.getMessage)
    }
    if (LOG.isDebugEnabled()) LOG.debug("consolidatePartitionsMap - consolidatedMap " + consolidatedMap)

    consolidatedMap
  }

  private def getConsolidatedMap(initialConsolidatedMap: scala.collection.mutable.Map[String, (String, String, String, Long, Long)], keyValueMap: scala.collection.mutable.Map[String, (String, String, String, Long, Long)]): scala.collection.mutable.Map[String, (String, String, String, Long, Long)] = {
    val consolidatedMap = scala.collection.mutable.Map[String, (String, String, String, Long, Long)]()
    try {

      if (keyValueMap != null && keyValueMap.keys.size > 0) {

        keyValueMap.foreach(keyVal => {
          if (initialConsolidatedMap.contains(keyVal._1)) {
            if (initialConsolidatedMap(keyVal._1) != null) {
              val keyval = initialConsolidatedMap(keyVal._1)._1
              val nodeid = initialConsolidatedMap(keyVal._1)._2
              val uuid = initialConsolidatedMap(keyVal._1)._3
              val nodestarttime = initialConsolidatedMap(keyVal._1)._4
              val counter = initialConsolidatedMap(keyVal._1)._5

              // val compUUID = AdapterPartitionInfoUtil.compareUUID(keyVal._2._3, uuid)
              if (keyVal._2._3.equals(uuid)) {
                if (LOG.isDebugEnabled()) LOG.debug("getConsolidatedMap - same UUID")
                if (keyVal._2._5 > counter)
                  consolidatedMap(keyVal._1) = (keyVal._2._1, keyVal._2._2, keyVal._2._3, keyVal._2._4, keyVal._2._5)
                else
                  consolidatedMap(keyVal._1) = (keyval, nodeid, uuid, nodestarttime, counter)
              } else {
                if (keyVal._2._4 > nodestarttime)
                  consolidatedMap(keyVal._1) = (keyVal._2._1, keyVal._2._2, keyVal._2._3, keyVal._2._4, keyVal._2._5)
                else consolidatedMap(keyVal._1) = (keyval, nodeid, uuid, nodestarttime, counter)
              }
            }
          } else {
            consolidatedMap(keyVal._1) = (keyVal._2._1, keyVal._2._2, keyVal._2._3, keyVal._2._4, keyVal._2._5)
          }
        })
      } else return initialConsolidatedMap

    } catch {
      case e: Exception => LOG.error("Consolidated Map" + e.getMessage)
    }
    if (LOG.isDebugEnabled()) LOG.debug("getConsolidatedMap - consolidatedMap " + consolidatedMap)
    consolidatedMap
  }

  private def getConsolidatedInitialMap(partitionInfoMap: Map[String, String]): scala.collection.mutable.Map[String, (String, String, String, Long, Long)] = {
    val consolidatedMap = scala.collection.mutable.Map[String, (String, String, String, Long, Long)]()
    try {
      if (partitionInfoMap != null && partitionInfoMap.keys.size > 0) {
        partitionInfoMap.foreach(partitionInfo => {
          if (partitionInfo._2 != null && partitionInfo._2.trim().size > 0) {
            val json = parse(partitionInfo._2)
            implicit val jsonFormats: Formats = DefaultFormats

            val partitionKeyValues = json.extract[PartKeyValues]
            val keyvalues = partitionKeyValues.keyvalues
            if (keyvalues != null && keyvalues.length > 0) {
              for (i <- 0 until keyvalues.length) {
                if (consolidatedMap.contains(keyvalues(i).key)) {
                  val consolidate_keyval = consolidatedMap(keyvalues(i).key)._1
                  val consolidate_nodeid = consolidatedMap(keyvalues(i).key)._2
                  val consolidate_uuid = consolidatedMap(keyvalues(i).key)._3
                  val consolidate_nodestarttime = consolidatedMap(keyvalues(i).key)._4
                  val consolidate_counter = consolidatedMap(keyvalues(i).key)._5
                  // val compUUID = AdapterPartitionInfoUtil.compareUUID(keyvalues(i).uuid, consolidate_uuid)

                  if (keyvalues(i).uuid.equals(consolidate_uuid)) {
                    if (keyvalues(i).uniquecounter > consolidate_counter)
                      consolidatedMap(keyvalues(i).key) = (keyvalues(i).keyvalue, keyvalues(i).nodeid, keyvalues(i).uuid, keyvalues(i).nodestarttime, keyvalues(i).uniquecounter)
                    else consolidatedMap(keyvalues(i).key) = (consolidate_keyval, consolidate_nodeid, consolidate_uuid, consolidate_nodestarttime, consolidate_counter)
                  } else {
                    if (keyvalues(i).nodestarttime > consolidate_nodestarttime)
                      consolidatedMap(keyvalues(i).key) = (keyvalues(i).keyvalue, keyvalues(i).nodeid, keyvalues(i).uuid, keyvalues(i).nodestarttime, keyvalues(i).uniquecounter)
                    else
                      consolidatedMap(keyvalues(i).key) = (consolidate_keyval, consolidate_nodeid, consolidate_uuid, consolidate_nodestarttime, consolidate_counter)
                  }
                } else {
                  consolidatedMap(keyvalues(i).key) = (keyvalues(i).keyvalue, keyvalues(i).nodeid, keyvalues(i).uuid, keyvalues(i).nodestarttime, keyvalues(i).uniquecounter)
                }
              }
            }
          }
        })
      }
    } catch {
      case e: Exception => LOG.error("Consolidated Map" + e.getMessage)
    }
    consolidatedMap
  }

  def PostAdapterParitionInfoToLocalDrive(): Unit = synchronized {
    if (LOG.isDebugEnabled()) LOG.debug("Called PostAdapterParitionInfoToLocalDrive")

    if (execCtxtsAdapterInfoPool != null) {
      execCtxtsAdapterInfoPool.shutdownNow()
      // Not really waiting for termination
    }
    var adapInfoMaplocal = scala.collection.mutable.Map[String, scala.collection.mutable.ArrayBuffer[AdapterPartKeyValues]]()

    execCtxtsAdapterInfoPool = scala.actors.threadpool.Executors.newFixedThreadPool(2)

    // We are checking for EnableEachTransactionCommit & KamanjaConfiguration.commitOffsetsTimeInterval to add thsi task
    if (KamanjaMetadata.gNodeContext != null && !KamanjaMetadata.gNodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0) {

      val postAdapterInfoSvc = new Runnable() {
        override def run() {
          val tp = execCtxtsAdapterInfoPool
          val commitOffsetsTimeInterval = KamanjaConfiguration.commitOffsetsTimeInterval
          while (!KamanjaConfiguration.shutdown && !tp.isShutdown && !tp.isTerminated) {
            try {
              Thread.sleep(1000) // Sleeping 1000ms more than given interval
            } catch {
              case e: Throwable => {}
            }
            if (!KamanjaConfiguration.shutdown && KamanjaMetadata.gNodeContext != null && !KamanjaMetadata.gNodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0) {
              val envCtxts = KamanjaManager.instance.GetEnvCtxts
              if (LOG.isDebugEnabled()) LOG.debug("Running execCtxtsAdapterInfo for " + envCtxts.length + " envCtxts")
              var idx = 0
              //keyvalues.clear()
              while (idx < envCtxts.length && !tp.isShutdown) {
                try {
                  if (idx == 0) {
                    adapInfoMaplocal.clear()
                  }
                  var keyvalues = scala.collection.mutable.ArrayBuffer[AdapterPartKeyValues]()

                  val (adapterName, key, keyValue) = envCtxts(idx).GetAdapterPartitionKVInfo
                  if (adapterName != null && key != null && keyValue != null) {

                    if (adapInfoMaplocal.contains(adapterName.trim)) {
                      val kv = adapInfoMaplocal(adapterName)
                      if (kv != null && kv.size > 0) {
                        kv.foreach(a => {
                          if (a != null && a.isInstanceOf[AdapterPartKeyValues]) {
                            keyvalues += a
                          }
                        })
                      }
                    }
                    keyvalues += new AdapterPartKeyValues(key, keyValue)
                    adapInfoMaplocal(adapterName.trim) = keyvalues
                  }
                } catch {
                  case e: Throwable => {
                    LOG.error("Failed to commit partitions offsets", e)
                  }
                }
                idx += 1
              }
              postAdapterPartitionsInfoToNodes(adapInfoMaplocal)
            }
          }
        }
      }

      execCtxtsAdapterInfoPool.execute(postAdapterInfoSvc)
      val writeAdapterInfoTime = KamanjaConfiguration.writeAdapterInfoTime

      val writeAdapterInfoSvc = new Runnable() {
        override def run() {
          val tp = execCtxtsAdapterInfoPool
          val commitOffsetsTimeInterval = KamanjaConfiguration.commitOffsetsTimeInterval
          while (!tp.isShutdown) {
            try {
              Thread.sleep(1000) // Sleeping 1000ms more than given interval
            } catch {
              case e: Throwable => {}
            }
            if (!KamanjaConfiguration.shutdown && KamanjaMetadata.gNodeContext != null && !KamanjaMetadata.gNodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0) {
              while (!tp.isShutdown) {
                try {
                  val waitSecs = writeAdapterInfoTime / 1000
                  val waitMilliSecs = writeAdapterInfoTime % 1000
                  var idx = 0
                  while (!KamanjaConfiguration.shutdown && KamanjaMetadata.gNodeContext != null && idx < waitSecs) {
                    idx += 1
                    try {
                      Thread.sleep(1000)
                    } catch {
                      case e: Throwable => {}
                    }
                  }
                  if (!KamanjaConfiguration.shutdown && KamanjaMetadata.gNodeContext != null && waitMilliSecs > 0) {
                    try {
                      Thread.sleep(waitMilliSecs)
                    } catch {
                      case e: Throwable => {}
                    }
                  }
                  writeAdapPartitionInfoToNodes(allPartitions)
                } catch {
                  case e: Throwable => {
                    LOG.error("Failed to commit partitions offsets", e)
                  }
                }
              }
            }
          }
        }
      }
      execCtxtsAdapterInfoPool.execute(writeAdapterInfoSvc)
    }
  }

  def updatePartitionsFn(eventType: String, eventPath: String, eventPathData: String): Unit = {
    LOG.debug("Kamanja Leader - " + eventPathData)
    val pathTokens = eventPath.split("/")
    val sendingNodeId = pathTokens(pathTokens.length - 1)

    if (eventPathData != null && eventPathData.trim().size > 0) {
      implicit val jsonFormats: Formats = DefaultFormats
      val json = parse(eventPathData)
      val partitionKeyValues = json.extract[PartitionKeyValuesInfo]
      val partKeyValuesSize = partitionKeyValues.keyvalues.size
      val partKeyValues = partitionKeyValues.keyvalues

      if (partKeyValuesSize > 0) {
        try {
          lock.writeLock().lock()

          if (allPartitions != null && allPartitions.keys.size == 0) {
            for (i <- 0 until partKeyValuesSize) {
              if (partKeyValues(i) != null && partKeyValues(i).key != null && partKeyValues(i).key.size > 0 && partKeyValues(i).keyvalue != null && partKeyValues(i).keyvalue.size > 0) {
                val keyvalue = (partKeyValues(i).keyvalue, partitionKeyValues.nodeid, partitionKeyValues.uuid, partitionKeyValues.nodestarttime, partitionKeyValues.uniquecounter)
                allPartitions(partKeyValues(i).key) = keyvalue
              }
            }
          } else {
            allPartitions.foreach(partitionsInfo => {

              if (partitionsInfo != null) {
                if (partitionKeyValues.nodeid.toLowerCase().equals(partitionsInfo._2._2)) {
                  //get the highest counter
                  val appPartcounter = partitionsInfo._2._5
                  val localCounter = partitionKeyValues.uniquecounter
                  if (partitionKeyValues.uuid.equals(partitionsInfo._2._3)) {
                    if (partitionKeyValues.uniquecounter > partitionsInfo._2._5) {
                      val keyValArray = partitionKeyValues.keyvalues
                      if (keyValArray != null && keyValArray.length > 0) {
                        for (i <- 0 until keyValArray.length) {
                          val allPartKey: String = partitionsInfo._1
                          if (allPartKey != null && allPartKey.length() > 0) {
                            if (allPartKey.equals(keyValArray(i).key)) {
                              val keyvalue = (keyValArray(i).keyvalue, partitionKeyValues.nodeid, partitionKeyValues.uuid, partitionKeyValues.nodestarttime, partitionKeyValues.uniquecounter)
                              allPartitions(partitionsInfo._1) = keyvalue
                            }
                          }
                        }
                      }
                    }
                  } else {
                    if (partitionKeyValues.nodestarttime > partitionsInfo._2._5) {
                      val keyValArray = partitionKeyValues.keyvalues
                      if (keyValArray != null && keyValArray.length > 0) {
                        for (i <- 0 until keyValArray.length) {
                          val allPartKey: String = partitionsInfo._1
                          if (allPartKey != null && allPartKey.length() > 0) {
                            if (allPartKey.equals(keyValArray(i).key)) {
                              val keyvalue = (keyValArray(i).keyvalue, partitionKeyValues.nodeid, partitionKeyValues.uuid, partitionKeyValues.nodestarttime, partitionKeyValues.uniquecounter)
                              allPartitions(partitionsInfo._1) = keyvalue
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            })
          }
        } finally {
          lock.writeLock().unlock()
        }
      }
    }

    if (LOG.isDebugEnabled()) {
      if (allPartitions != null && allPartitions.size > 0) {
        allPartitions.foreach(kv => {
          LOG.debug("collectAdapterPartitionInfo- key " + kv._1 + " value " + kv._2)
        })
      } else LOG.debug("collectAdapterPartitionInfo - allPartitions " + allPartitions)
    }

  }

  private var allPartitionsInfo = scala.collection.mutable.Map[String, (String, String, Long, String, Long)]()

  def postAdapterPartitionsInfoToNodes(adapterInfoMap: scala.collection.mutable.Map[String, ArrayBuffer[AdapterPartKeyValues]]): Unit = {
    val nodeId = KamanjaMetadata.gNodeContext.getEnvCtxt().getNodeId()

    if (LOG.isDebugEnabled) LOG.debug("Kamanja Leader -postAdapterPartitionsInfoToNodes - nodeId " + nodeId)
    if (LOG.isDebugEnabled) {
      if (adapterInfoMap != null && adapterInfoMap.size > 0) {
        adapterInfoMap.foreach(adapInfo => {
          LOG.debug(adapInfo._1)
          adapInfo._2.map(a => LOG.debug("key values: " + a.key + " : " + a.keyvalue))
        })
      }
    }

    val nodeStartTime = System.currentTimeMillis()
    var keyvalues = new scala.collection.mutable.ArrayBuffer[AdapterPartKeyValues]
    if (adapterInfoMap != null && adapterInfoMap.keys.size > 0) {
      adapterInfoMap.foreach(adapKeyValues => {
        if (adapKeyValues != null && adapKeyValues._2 != null && adapKeyValues._2.size > 0) {
          for (i <- 0 until adapKeyValues._2.size) {
            val keyval = new AdapterPartKeyValues(adapKeyValues._2(i).key, adapKeyValues._2(i).keyvalue)
            keyvalues += keyval
          }
        }
      })
    }
    if (keyvalues != null && keyvalues.size > 0)
      counter = increment
    val localPartitionInfo = new PartitionKeyValuesInfo(nodeId, UUID, nodeStartTime, counter, keyvalues.toArray)
    adapterJson = AdapterPartitionInfoUtil.generateAdapterInfoJson(localPartitionInfo)

    if (LOG.isDebugEnabled) LOG.debug("Kamanja Leader -postAdapterPartitionsInfoToNodes - adapterJson " + adapterJson)
    // do this very x millisecs , Get the value from CLusterConfig
    val postAdapterInfoTime = KamanjaConfiguration.postAdapterInfoTime
    Thread.sleep(postAdapterInfoTime)

    val UpdatePartitionNodeInfo = updatePartitionInfoPath + "/" + nodeId
    if (LOG.isDebugEnabled) LOG.debug("Kamanja Leader -postAdapterPartitionsInfoToNodes - SendAdapterInfo " + UpdatePartitionNodeInfo)

    KamanjaMetadata.gNodeContext.getEnvCtxt().setListenerCacheKey(UpdatePartitionNodeInfo, adapterJson) // => Goes to Leader

    if (LOG.isDebugEnabled) {
      if (allPartitions != null && allPartitions.size > 0) {
        allPartitions.foreach(kv => {
          LOG.debug("Kamanja Leader -postAdapterPartitionsInfoToNodes -allPartitions- key " + kv._1)
          LOG.debug("Kamanja Leader -postAdapterPartitionsInfoToNodes - allPartitions- value " + kv._2)
        })
      } else LOG.debug("Kamanja Leader -postAdapterPartitionsInfoToNodes - allPartitions " + allPartitions)
    }
  }

  def writeAdapPartitionInfoToNodes(allPartitions: scala.collection.mutable.Map[String, (String, String, String, Long, Long)]) = {
    try {
      if (allPartitions != null && allPartitions.size > 0) {
        //get the node local drive location
        //do this every y millisecs
        val nodeId = KamanjaMetadata.gNodeContext.getEnvCtxt().getNodeId()
        var nodepath = KamanjaConfiguration.adapterInfoWriteLocation
        if (nodepath != null && nodepath.length() > 0) {
          if (!nodepath.endsWith("/"))
            nodepath = nodepath + "/"
          val nodeadapterInfoPath = nodepath + nodeId //Get this info from cluster config

          val nodepathsplit = nodeadapterInfoPath.split("/")
          var dirpath: String = ""
          if (nodepathsplit != null && nodepathsplit.length > 0) {
            for (i <- 0 until nodepathsplit.length) {
              if (nodepathsplit(i) != null && nodepathsplit(i).length() > 0) {
                dirpath = dirpath + "/" + nodepathsplit(i)
                val folder = new File(dirpath)
                if (!folder.exists()) {
                  folder.mkdir();
                }
              }
            }
          }
          AdapterPartitionInfoUtil.takeBackUpAndWriteToFile(allPartitions, nodeadapterInfoPath)
        }
      }
    } catch {
      case e: Exception => LOG.error("Kamanja Leader - Write AdapterPartitionInfo to File" + e.getMessage)
    }
  }

  def ReadFromLocalDrive(): String = {
    var adapterJsonFromlocalDrive: String = ""
    try {
      if (KamanjaMetadata.gNodeContext != null && !KamanjaMetadata.gNodeContext.getEnvCtxt().EnableEachTransactionCommit && KamanjaConfiguration.commitOffsetsTimeInterval > 0) {
        //read to local map from local drive
        val nodeid = KamanjaMetadata.gNodeContext.getEnvCtxt().getNodeId()
        val localDriveLocation = KamanjaConfiguration.adapterInfoWriteLocation
        adapterJsonFromlocalDrive = AdapterPartitionInfoUtil.readfromFile(nodeid, localDriveLocation) //allPartitions)
      }
    } catch {
      case e: Exception => LOG.debug("KamanjaLeader - ReadFromLocalDrive " + e.getMessage)
    }
    adapterJsonFromlocalDrive
  }
}


