package com.ligadata.ZooKeeper

import org.apache.logging.log4j.LogManager
import com.ligadata.ZooKeeper._
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.locks.InterProcessMutex
import com.ligadata.Utils.ClusterStatus

class ProcessComponentByWeight(val componentName: String, val nodeIdPrefix: String, val weight: Int) {
  private[this] val _LOG = LogManager.getLogger(getClass);
  private[this] var _clusterStatus = ClusterStatus("", false, "", null)
  private[this] var _zkLeaderLatch: ZkLeaderLatch = _
  private[this] var _zkConnectString: String = _
  private[this] var _zkNodePath: String = _
  private[this] var _zkSessionTimeoutMs: Int = _
  private[this] var _zkConnectionTimeoutMs: Int = _
  private[this] var _zkcForAcquire: CuratorFramework = null
  private[this] var _lock: InterProcessMutex = null
  private[this] var _comp_weight_str = "~~~~~COMP~~~~~WEIGHT~~~~~"
  private[this] var _changedCluster = true
  private[this] var _isThisNodeToProcess = false
  private[this] var _objlock = new Object

  // Here Leader can change or Participants can change
  private def EventChangeCallback(cs: ClusterStatus): Unit = {
    try {
      _clusterStatus = cs
      _changedCluster = true
      _LOG.warn("NodeId:%s, IsLeader:%s, Leader:%s, AllParticipents:{%s}".format(cs.nodeId, if (cs.isLeader) "true" else "false", cs.leaderNodeId, cs.participantsNodeIds.mkString(",")))
    } catch {
      case e: Exception => {
        _LOG.debug("EventChangeCallback => Found exception.", e)
      }
    }
  }

  private def ComputeNodeToProcess: Unit = {
    if (!_changedCluster) return
    _objlock.synchronized {
      if (!_changedCluster) return
      _changedCluster = false
      val cs = _clusterStatus
      if (cs.nodeId != null && cs.participantsNodeIds != null) {
        var maxWeightNode = ""
        var maxWeight = -2
        cs.participantsNodeIds.foreach(nd => {
          val parts = nd.split(_comp_weight_str)
          val weight = if (parts.size == 2 && parts(1).trim.size > 0) parts(1).trim.toInt else -1
          if (parts.size == 2 && (maxWeightNode.size == 0 || weight > maxWeight)) {
            maxWeight = weight
            maxWeightNode = nd
          }
        })
        _isThisNodeToProcess = maxWeightNode.equals(cs.nodeId)
        _LOG.warn("Triggered ComputeNodeToProcess. _isThisNodeToProcess:%s, NodeId:%s, maxWeightNode:%s, maxWeight:%d".format(_isThisNodeToProcess.toString, cs.nodeId, maxWeightNode, maxWeight))
      }
    }
  }

  def Init(zkConnectString: String, zkNodePath: String, zkSessionTimeoutMs: Int, zkConnectionTimeoutMs: Int): Unit = {
    _zkConnectString = zkConnectString
    _zkNodePath = zkNodePath
    _zkSessionTimeoutMs = zkSessionTimeoutMs
    _zkConnectionTimeoutMs = zkConnectionTimeoutMs

    val nodeId = nodeIdPrefix + _comp_weight_str + weight
    _LOG.warn("Initializing with ZK info. zkConnectString:%s, zkNodePath:%s, zkSessionTimeoutMs:%d, zkConnectionTimeoutMs:%d, nodeId:%s".format(zkConnectString, zkNodePath, zkSessionTimeoutMs, zkConnectionTimeoutMs, nodeId))

    if (_zkConnectString != null && _zkConnectString.isEmpty() == false && _zkNodePath != null && _zkNodePath.isEmpty() == false) {
      try {
        CreateClient.CreateNodeIfNotExists(_zkConnectString, _zkNodePath)
        _zkLeaderLatch = new ZkLeaderLatch(_zkConnectString, _zkNodePath, nodeId, EventChangeCallback, _zkSessionTimeoutMs, _zkConnectionTimeoutMs)
        _zkLeaderLatch.SelectLeader
      } catch {
        case e: Exception => {
          _LOG.error("Failed to initialize ZooKeeper Connection.", e)
          throw e
        }
      }
    } else {
      _LOG.error("Not connected to elect Leader and not distributing data between nodes.")
    }
    _LOG.warn("Initialized ZK Leaderlatch.")
  }

  def Acquire(): Unit = {
    // Do Distributed zookeeper lock here
    if (_zkcForAcquire == null) {
      CreateClient.CreateNodeIfNotExists(_zkConnectString, _zkNodePath)
      _zkcForAcquire = CreateClient.createSimple(_zkConnectString, _zkSessionTimeoutMs, _zkConnectionTimeoutMs)
    }
    if (_zkcForAcquire == null)
      throw new Exception("Failed to connect to Zookeeper with connection string:" + _zkConnectString)
    val lockPath = _zkNodePath + "/distributed-aquire-lock-for-" + componentName.toLowerCase

    _LOG.warn("Acquiring lock for lockPath:%s".format(lockPath))
    _lock = new InterProcessMutex(_zkcForAcquire, lockPath);
    try {
      _lock.acquire();
      _LOG.warn("Acquired lock for lockPath:%s".format(lockPath))
    } catch {
      case e: Exception => {
        _LOG.warn("Failed to acquire lock for lockPath:%s".format(lockPath), e)
        try {
          if (_lock != null)
            _lock.release();
        } catch {
          case e: Exception => {}
        }
        _lock = null
        throw e
      }
    }
  }

  def Release(): Unit = {
    if (_lock == null)
      throw new Exception("Lock is not really aquired")

    _LOG.warn("Releasing acquired lock")

    try {
      if (_lock != null)
        _lock.release();
      _lock = null
    } catch {
      case e: Exception => {
        _LOG.error("Failed to release lock", e)
        throw e
      }
    }
  }

  def IsThisNodeToProcess(): Boolean = {
    ComputeNodeToProcess
    _isThisNodeToProcess
  }

  def Shutdown: Unit = {
    _LOG.warn("Shutdown called")
    if (_zkLeaderLatch != null)
      _zkLeaderLatch.Shutdown
    _zkLeaderLatch = null

    try {
      if (_lock != null)
        _lock.release();
      _lock = null
    } catch {
      case e: Exception => _LOG.error("Failed to release lock", e)
    }
    if (_zkcForAcquire != null)
      _zkcForAcquire.close()
    _zkcForAcquire = null

  }
}
