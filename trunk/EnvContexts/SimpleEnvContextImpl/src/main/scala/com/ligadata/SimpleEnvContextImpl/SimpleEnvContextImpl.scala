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

package com.ligadata.SimpleEnvContextImpl

import com.ligadata.Utils.{CacheConfig, KamanjaLoaderInfo, ClusterStatus}
import com.ligadata.ZooKeeper.{ZkLeaderLatch, ZooKeeperListener, CreateClient}
import org.apache.curator.framework.CuratorFramework
import org.apache.curator.framework.recipes.locks.InterProcessMutex
import org.json4s.jackson.Serialization

import scala.actors.threadpool.{Executors, ExecutorService}
import scala.collection.immutable.Map
import scala.collection.mutable
import scala.collection.mutable._
import scala.util.control.Breaks._
import scala.reflect.runtime.{universe => ru}
import org.apache.logging.log4j.{Logger, LogManager}
import com.ligadata.KvBase.{Key, TimeRange, KvBaseDefalts, KeyWithBucketIdAndPrimaryKey, KeyWithBucketIdAndPrimaryKeyCompHelper}
import com.ligadata.StorageBase.{DataStore, Transaction, DataStoreOperations}
import com.ligadata.KamanjaBase._

// import com.ligadata.KamanjaBase.{ EnvContext, ContainerInterface }
import com.ligadata.kamanja.metadata._
import com.ligadata.Exceptions._
import java.net.URLClassLoader
import com.ligadata.Serialize._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import com.ligadata.Exceptions._
import com.ligadata.keyvaluestore.KeyValueManager
import java.io.{ByteArrayInputStream, DataInputStream, DataOutputStream, ByteArrayOutputStream}
import java.util.{TreeMap, Date}
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.ligadata.cache.{CacheCallback, DataCache, CacheCallbackData}
import scala.collection.JavaConverters._

trait LogTrait {
  val loggerName = this.getClass.getName()
  val logger = LogManager.getLogger(loggerName)
}

/**
  * The SimpleEnvContextImpl supports kv stores that are based upon MapDb hash tables.
  */
object SimpleEnvContextImpl extends EnvContext with LogTrait {

  case class CacheInfo(HostList: String, CachePort: Int, CacheSizePerNode: Long, ReplicateFactor: Int, TimeToIdleSeconds: Long, EvictionPolicy: String)

  case class TenantEnvCtxtInfo(tenantInfo: TenantInfo, datastore: DataStore, cachedContainers: scala.collection.mutable.Map[String, MsgContainerInfo], containersNames: scala.collection.mutable.Set[String])

  val CLASSNAME = "com.ligadata.SimpleEnvContextImpl.SimpleEnvContextImpl$"
  private var hbExecutor: ExecutorService = Executors.newFixedThreadPool(1)
  private var isShutdown = false
  private var metrics: collection.mutable.Map[String, Long] = collection.mutable.Map[String, Long]()
  private var startTime: String = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var lastSeen: String = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private val STORAGE_READ_COUNT = "READS"
  private val STORAGE_WRITE_COUNT = "WRITES"

  private var _nodeId: String = _
  private var _clusterId: String = _
  private val _setZk_reent_lock = new ReentrantReadWriteLock(true)
  private var _setZkData: CuratorFramework = _
  private var _zkConnectString = ""
  private var _zkBasePath = ""
  private var _zkleaderNodePath = ""
  private var _zkSessionTimeoutMs = 30000
  private var _zkConnectionTimeoutMs = 30000
  private val _zkListeners = ArrayBuffer[ZooKeeperListener]()
  private val _zkListeners_reent_lock = new ReentrantReadWriteLock(true)
  private val _zkLeader_reent_lock = new ReentrantReadWriteLock(true)
  private val _cacheListener_reent_lock = new ReentrantReadWriteLock(true)
  private var _zkLeaderLatch: ZkLeaderLatch = _
  private val _zkLeaderListeners = ArrayBuffer[LeaderListenerCallback]()
  private val _cacheListeners = ArrayBuffer[ReturnCacheListenerCallback]()
  private var _clusterStatusInfo: ClusterStatus = _
  private val _nodeCacheMap = collection.mutable.Map[String, Any]()
  private val _nodeCache_reent_lock = new ReentrantReadWriteLock(true)
  private var txnIdsRangeForNode: Int = 100000
  // Each time get txnIdsRange of transaction ids for each Node
  private var txnIdsRangeForPartition: Int = 10000
  // Each time get txnIdsRange of transaction ids for each partition
  private var _sysCatalogDsString: String = _
  private val _tenantIdMap = scala.collection.mutable.Map[String, TenantEnvCtxtInfo]()
  private var _postMsgListenerCallback: (Array[ContainerInterface]) => Unit = null
  private var _listenerCache: DataCache = null
  private var _listenerConfigClusterCache: DataCache = null
  private var _cacheConfig: CacheConfig = null
  private val _cacheConfigTemplate =
    """
      |
      |{
      |  "name": "%s",
      |  "diskSpoolBufferSizeMB": "20",
      |  "jgroups.tcpping.initial_hosts": "%s",
      |  "jgroups.port": "%d",
      |  "replicatePuts": "true",
      |  "replicateUpdates": "true",
      |  "replicateUpdatesViaCopy": "true",
      |  "replicateRemovals": "true",
      |  "replicateAsynchronously": "true",
      |  "CacheConfig": {
      |    "maxBytesLocalHeap": "%d",
      |    "eternal": "false",
      |    "bootstrapAsynchronously": "false",
      |    "timeToIdleSeconds": "%d",
      |    "timeToLiveSeconds": "%d",
      |    "memoryStoreEvictionPolicy": "%s",
      |    "transactionalMode": "off",
      |    "class": "net.sf.ehcache.distribution.jgroups.JGroupsCacheManagerPeerProviderFactory",
      |    "separator": "::",
      |    "peerconfig": "channelName=EH_CACHE::file=jgroups_tcp.xml",
      |    "enableListener": "%s"
      |  }
      |}
      |
      | """.stripMargin

  private def addCacheListener(listenPath: String, ListenCallback: (String, String, String) => Unit, childCache: Boolean): Unit = {
    if (ListenCallback == null || listenPath == null || listenPath.size == 0 /* || listenPath.trim.size == 0 */ ) {
      return
    }

    WriteLock(_cacheListener_reent_lock)
    var prevListener: ReturnCacheListenerCallback = null
    try {
      if (_listenerCache != null) {
        var i = 0
        while (i < _cacheListeners.size && prevListener == null) {
          if (_cacheListeners(i).listenPath.compareTo(listenPath) == 0) {
            prevListener = _cacheListeners.remove(i)
          }
          i += 1
        }
        _cacheListeners += ReturnCacheListenerCallback(listenPath, ListenCallback, childCache)
        prevListener = null
      }
    } catch {
      case e: Throwable => {
        if (prevListener != null)
          _cacheListeners += prevListener
        throw e
      }
    } finally {
      WriteUnlock(_cacheListener_reent_lock)
    }

    try {
      if (_listenerCache != null) {
        if (!childCache && _listenerCache.isKeyInCache(listenPath)) {
          val value = _listenerCache.get(listenPath);
          ListenCallback("Put", listenPath, value.toString)
        } else if (childCache) {
          val keys = _listenerCache.getKeys().filter(k => k.startsWith(listenPath))
          if (keys.size > 0) {
            val map = _listenerCache.get(keys)
            map.asScala.foreach(kv => {
              ListenCallback("Put", kv._1, kv._2.toString)
            })
          }
        }
      }
    } catch {
      case e: Throwable => {
        throw e
      }
    }
  }

  private def getMatchedCacheListeners(key: String): Array[ReturnCacheListenerCallback] = {
    var matchedListerners = Array[ReturnCacheListenerCallback]()
    ReadLock(_cacheListener_reent_lock)
    try {
      matchedListerners = _cacheListeners.filter(l => (l.childCache && key.startsWith(l.listenPath)) || key == l.listenPath).toArray
    } catch {
      case e: Throwable => {
        throw e
      }
    } finally {
      ReadUnlock(_cacheListener_reent_lock)
    }

    matchedListerners
  }

  case class LeaderListenerCallback(val EventChangeCallback: (ClusterStatus) => Unit)

  case class ReturnCacheListenerCallback(val listenPath: String, val ListenCallback: (String, String, String) => Unit, val childCache: Boolean)

  //  case class PostMessageListenerCallback(val callback: (Array[ContainerInterface]) => Unit)

  override def hasZkConnectionString: Boolean = (_zkConnectString != null && _zkConnectString.size > 0 && _zkBasePath != null && _zkBasePath.size > 0)

  override def getTransactionRanges(): (Int, Int) = (txnIdsRangeForPartition, txnIdsRangeForNode)

  def ReadLock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.readLock().lock()
  }

  def ReadUnlock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.readLock().unlock()
  }

  def WriteLock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.writeLock().lock()
  }

  def WriteUnlock(reent_lock: ReentrantReadWriteLock): Unit = {
    if (reent_lock != null)
      reent_lock.writeLock().unlock()
  }

  // Init begin monitor values
  metrics(STORAGE_READ_COUNT) = 0
  metrics(STORAGE_WRITE_COUNT) = 0

  // Start the heartbeat.
  hbExecutor.execute(new Runnable() {
    override def run(): Unit = {
      while (!isShutdown) {
        try {
          lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
          Thread.sleep(5000)
        } catch {
          case e: Exception => {
            if (!isShutdown)
              logger.warn("SimpleEnvContext heartbeat interrupted.", e)
          }
        }
      }
    }
  })

  /*
    override def getComponentStatusAndMetrics: com.ligadata.HeartBeat.MonitorComponentInfo = {
      implicit val formats = org.json4s.DefaultFormats
      return new com.ligadata.HeartBeat.MonitorComponentInfo("STORAGE_ADAPTER", "SimpleEnvContext", "v1.3", startTime, lastSeen, Serialization.write(metrics).toString)
    }
  */

  private def ResolveEnableEachTransactionCommit: Unit = {
    if (_mgr != null) {
      var foundIt = false
      val clusters = _mgr.Clusters
      clusters.foreach(c => {
        if (foundIt == false) {
          val tmp1 = _mgr.GetUserProperty(c._1, "EnableEachTransactionCommit")
          if (tmp1 != null && tmp1.trim().size > 0) {
            try {
              _enableEachTransactionCommit = tmp1.trim().toBoolean
              foundIt = true
            } catch {
              case e: Exception => {
                logger.warn("", e)
              }
            }
          }

        }
      })
    }
  }

  override def setMdMgr(inMgr: MdMgr): Unit = {
    _mgr = inMgr
    ResolveEnableEachTransactionCommit
  }

  override def NewMessageOrContainer(fqclassname: String): ContainerInterface = {
    var curClass: Class[_] = null
    try {
      if (_metadataLoader != null) {
        curClass = Class.forName(fqclassname, true, _metadataLoader.loader)
      } else {
        curClass = Class.forName(fqclassname)
      }
    } catch {
      case e: Exception => {
        logger.error("Failed to load Message/Container class %s".format(fqclassname), e)
        throw e // Rethrow
      }
    }
    val msgOrContainer: ContainerInterface = curClass.newInstance().asInstanceOf[ContainerInterface]
    msgOrContainer
  }

  //  class TxnCtxtKey {
  //    var containerName: String = _
  //    var key: String = _
  //  }

  class MsgContainerInfo(createLock: Boolean) {
    // By time, BucketKey, then PrimaryKey/{transactionid & rowid}. This is little cheaper if we are going to get exact match, because we compare time & then bucketid
    val dataByTmPart = new TreeMap[KeyWithBucketIdAndPrimaryKey, ContainerInterface](KvBaseDefalts.defualtTimePartComp)
    // By BucketKey, time, then PrimaryKey/{Transactionid & Rowid}
    val dataByBucketKey = new TreeMap[KeyWithBucketIdAndPrimaryKey, ContainerInterface](KvBaseDefalts.defualtBucketKeyComp)
    var isContainer: Boolean = false
    val reent_lock: ReentrantReadWriteLock = if (createLock) new ReentrantReadWriteLock(true) else null
  }

  object TxnContextCommonFunctions {
    def ReadLockContainer(container: MsgContainerInfo): Unit = {
      if (container != null)
        ReadLock(container.reent_lock)
    }

    def ReadUnlockContainer(container: MsgContainerInfo): Unit = {
      if (container != null)
        ReadUnlock(container.reent_lock)
    }

    def WriteLockContainer(container: MsgContainerInfo): Unit = {
      if (container != null)
        WriteLock(container.reent_lock)
    }

    def WriteUnlockContainer(container: MsgContainerInfo): Unit = {
      if (container != null)
        WriteUnlock(container.reent_lock)
    }

    //BUGBUG:: we are handling primaryKey only when partKey
    def getRecent(container: MsgContainerInfo, partKey: List[String], tmRange: TimeRange, primaryKey: List[String], f: ContainerInterface => Boolean): (ContainerInterface, Boolean) = {
      //BUGBUG:: Taking last record from the search. it may not be the most recent
      if (container != null) {
        TxnContextCommonFunctions.ReadLockContainer(container)
        try {
          if (TxnContextCommonFunctions.IsEmptyKey(partKey) == false) {
            val tmRng =
              if (tmRange == null)
                TimeRange(Long.MinValue, Long.MaxValue)
              else
                tmRange
            val partKeyAsArray = partKey.toArray
            val primKeyAsArray = if (primaryKey != null && primaryKey.size > 0) primaryKey.toArray else null
            val fromKey = KeyWithBucketIdAndPrimaryKey(KeyWithBucketIdAndPrimaryKeyCompHelper.BucketIdForBucketKey(partKeyAsArray), Key(tmRng.beginTime, partKeyAsArray, 0, 0), primKeyAsArray != null, primKeyAsArray)
            val toKey = KeyWithBucketIdAndPrimaryKey(KeyWithBucketIdAndPrimaryKeyCompHelper.BucketIdForBucketKey(partKeyAsArray), Key(tmRng.endTime, partKeyAsArray, Long.MaxValue, Int.MaxValue), primKeyAsArray != null, primKeyAsArray)
            val tmpDataByTmPart = new TreeMap[KeyWithBucketIdAndPrimaryKey, ContainerInterface](KvBaseDefalts.defualtTimePartComp) // By time, BucketKey, then PrimaryKey/{transactionid & rowid}. This is little cheaper if we are going to get exact match, because we compare time & then bucketid
            tmpDataByTmPart.putAll(container.dataByBucketKey.subMap(fromKey, true, toKey, true))
            val tmFilterMap = tmpDataByTmPart.subMap(fromKey, true, toKey, true)

            if (f != null) {
              var it1 = tmFilterMap.descendingMap().entrySet().iterator()
              while (it1.hasNext()) {
                val entry = it1.next();
                val value = entry.getValue();
                if (primKeyAsArray != null) {
                  if (primKeyAsArray.sameElements(value.getPrimaryKey) && f(value)) {
                    return (value, true);
                  }
                } else {
                  if (f(value)) {
                    return (value, true);
                  }
                }
              }
            } else {
              if (primKeyAsArray != null) {
                var it1 = tmFilterMap.descendingMap().entrySet().iterator()
                while (it1.hasNext()) {
                  val entry = it1.next();
                  val value = entry.getValue();
                  if (primKeyAsArray.sameElements(value.getPrimaryKey))
                    return (value, true);
                }
              } else {
                val data = tmFilterMap.lastEntry()
                if (data != null)
                  return (data.getValue(), true)
              }
            }
          } else if (tmRange != null) {
            val fromKey = KeyWithBucketIdAndPrimaryKey(Int.MinValue, Key(tmRange.beginTime, null, 0, 0), false, null)
            val toKey = KeyWithBucketIdAndPrimaryKey(Int.MaxValue, Key(tmRange.endTime, null, Long.MaxValue, Int.MaxValue), false, null)
            val tmFilterMap = container.dataByTmPart.subMap(fromKey, true, toKey, true)

            if (f != null) {
              var it1 = tmFilterMap.descendingMap().entrySet().iterator()
              while (it1.hasNext()) {
                val entry = it1.next();
                val value = entry.getValue();
                if (f(value)) {
                  return (value, true);
                }
              }
            } else {
              val data = tmFilterMap.lastEntry()
              if (data != null)
                return (data.getValue(), true)
            }
          } else {
            val data = container.dataByTmPart.lastEntry()
            if (data != null)
              return (data.getValue(), true)
          }
        } catch {
          case e: Exception => {
            throw e
          }
        } finally {
          TxnContextCommonFunctions.ReadUnlockContainer(container)
        }
      }
      (null, false)
    }

    def IsEmptyKey(key: List[String]): Boolean = {
      (key == null || key.size == 0 /* || key.filter(k => k != null).size == 0 */)
    }

    def IsEmptyKey(key: Array[String]): Boolean = {
      (key == null || key.size == 0 /* || key.filter(k => k != null).size == 0 */)
    }

    def IsSameKey(key1: List[String], key2: List[String]): Boolean = {
      if (key1.size != key2.size)
        return false

      for (i <- 0 until key1.size) {
        if (key1(i).compareTo(key2(i)) != 0)
          return false
      }

      return true
    }

    def IsSameKey(key1: Array[String], key2: Array[String]): Boolean = {
      if (key1.size != key2.size)
        return false

      for (i <- 0 until key1.size) {
        if (key1(i).compareTo(key2(i)) != 0)
          return false
      }

      return true
    }

    def getRddData(container: MsgContainerInfo, partKey: List[String], tmRange: TimeRange, primaryKey: List[String], f: ContainerInterface => Boolean): Array[(KeyWithBucketIdAndPrimaryKey, ContainerInterface)] = {
      val retResult = ArrayBuffer[(KeyWithBucketIdAndPrimaryKey, ContainerInterface)]()
      if (container != null) {
        TxnContextCommonFunctions.ReadLockContainer(container)
        try {
          if (TxnContextCommonFunctions.IsEmptyKey(partKey) == false) {
            val tmRng =
              if (tmRange == null)
                TimeRange(Long.MinValue, Long.MaxValue)
              else
                tmRange
            val partKeyAsArray = partKey.toArray
            val primKeyAsArray = if (primaryKey != null && primaryKey.size > 0) primaryKey.toArray else null
            val fromKey = KeyWithBucketIdAndPrimaryKey(KeyWithBucketIdAndPrimaryKeyCompHelper.BucketIdForBucketKey(partKeyAsArray), Key(tmRng.beginTime, partKeyAsArray, 0, 0), false, null)
            val toKey = KeyWithBucketIdAndPrimaryKey(KeyWithBucketIdAndPrimaryKeyCompHelper.BucketIdForBucketKey(partKeyAsArray), Key(tmRng.endTime, partKeyAsArray, Long.MaxValue, Int.MaxValue), false, null)
            val tmpDataByTmPart = new TreeMap[KeyWithBucketIdAndPrimaryKey, ContainerInterface](KvBaseDefalts.defualtTimePartComp) // By time, BucketKey, then PrimaryKey/{transactionid & rowid}. This is little cheaper if we are going to get exact match, because we compare time & then bucketid
            tmpDataByTmPart.putAll(container.dataByBucketKey.subMap(fromKey, true, toKey, true))
            val tmFilterMap = tmpDataByTmPart.subMap(fromKey, true, toKey, true)

            if (f != null) {
              var it1 = tmFilterMap.entrySet().iterator()
              while (it1.hasNext()) {
                val entry = it1.next();
                val value = entry.getValue();
                if (f(value)) {
                  if (primKeyAsArray == null || IsSameKey(primKeyAsArray, value.getPrimaryKey))
                    retResult += ((entry.getKey(), value))
                }
              }
            } else {
              var it1 = tmFilterMap.entrySet().iterator()
              while (it1.hasNext()) {
                val entry = it1.next();
                val value = entry.getValue();
                if (primKeyAsArray == null || IsSameKey(primKeyAsArray, value.getPrimaryKey))
                  retResult += ((entry.getKey(), value))
              }
            }
          } else if (tmRange != null) {
            val fromKey = KeyWithBucketIdAndPrimaryKey(Int.MinValue, Key(tmRange.beginTime, null, 0, 0), false, null)
            val toKey = KeyWithBucketIdAndPrimaryKey(Int.MaxValue, Key(tmRange.endTime, null, Long.MaxValue, Int.MaxValue), false, null)
            val tmFilterMap = container.dataByTmPart.subMap(fromKey, true, toKey, true)

            if (f != null) {
              var it1 = tmFilterMap.entrySet().iterator()
              while (it1.hasNext()) {
                val entry = it1.next();
                val value = entry.getValue();
                if (f(value)) {
                  retResult += ((entry.getKey(), value))
                }
              }
            } else {
              var it1 = tmFilterMap.entrySet().iterator()
              while (it1.hasNext()) {
                val entry = it1.next();
                retResult += ((entry.getKey(), entry.getValue()))
              }
            }
          } else {
            var it1 = container.dataByTmPart.entrySet().iterator()
            if (f != null) {
              while (it1.hasNext()) {
                val entry = it1.next();
                val value = entry.getValue();
                if (f(value))
                  retResult += ((entry.getKey(), value))
              }
            } else {
              while (it1.hasNext()) {
                val entry = it1.next();
                retResult += ((entry.getKey(), entry.getValue()))
              }
            }
          }
        } catch {
          case e: Exception => {
            throw e
          }
        } finally {
          TxnContextCommonFunctions.ReadUnlockContainer(container)
        }
      }
      retResult.toArray
    }
  }

  // Adapters Keys & values
  override def setAdapterUniqueKeyValue(key: String, value: String): Unit = {
    val bktIdx = getParallelBucketIdx(key)
    _adapterUniqKeyValBktlocks(bktIdx).writeLock().lock()
    try {
      _adapterUniqKeyValBuckets(bktIdx)(key) = value
    } catch {
      case e: Exception => {
        throw e
      }
    } finally {
      _adapterUniqKeyValBktlocks(bktIdx).writeLock().unlock()
    }
    val oneContData = Array(("AdapterUniqKvData", Array((Key(KvBaseDefalts.defaultTime, Array(key), 0, 0), "", value.getBytes().asInstanceOf[Any]))))
    if (logger.isDebugEnabled)
      logger.debug(s"Saving AdapterUniqKvData key:${key}, value:${value}")
    callSaveData(_sysCatalogDatastore, oneContData)
  }

  // Get Status information from Final table
  override def getAllAdapterUniqKvDataInfo(keys: Array[String]): Array[(String, String)] = {
    localGetAdapterUniqueKeyValue(keys).toArray
  }

  override def getAdapterUniqueKeyValue(key: String): String = {
    val res = localGetAdapterUniqueKeyValue(Array(key))
    if (res != null && res.size > 0)
      return res(0)._2
    null
  }

  private def getParallelBucketIdx(key: String): Int = {
    if (key == null) return 0
    return (math.abs(key.hashCode()) % _parallelBuciets)
  }

  private def localGetAdapterUniqueKeyValue(keys: Array[String]): Array[(String, String)] = {
    var retVal = ArrayBuffer[(String, String)]()
    var notFoundKeys = ArrayBuffer[Array[String]]()

    val bktIdxsAndKeys = keys.map(k => (getParallelBucketIdx(k), k))

    val bktIdxs = bktIdxsAndKeys.map(IdxAndKey => IdxAndKey._1).toSet

    bktIdxs.foreach(bktIdx => {
      _adapterUniqKeyValBktlocks(bktIdx).readLock().lock()
      try {
        bktIdxsAndKeys.foreach(IdxAndKey => {
          if (IdxAndKey._1 == bktIdx) {
            val v = _adapterUniqKeyValBuckets(bktIdx).getOrElse(IdxAndKey._2, null)
            if (v != null) {
              retVal += ((IdxAndKey._2, v))
            } else {
              notFoundKeys += Array(IdxAndKey._2)
            }
          }
        })
      } catch {
        case e: Exception => {
          throw e
        }
      } finally {
        _adapterUniqKeyValBktlocks(bktIdx).readLock().unlock()
      }
    })

    if (notFoundKeys.size == 0) return retVal.toArray
    val results = new ArrayBuffer[(String, String)]()
    val buildAdapOne = (k: Key, v: Any, serType: String, t: String, ver: Int) => {
      buildAdapterUniqueValue(k, v, results)
    }
    try {
      callGetData(_sysCatalogDatastore, "AdapterUniqKvData", Array(TimeRange(KvBaseDefalts.defaultTime, KvBaseDefalts.defaultTime)), notFoundKeys.toArray, buildAdapOne)
    } catch {
      case e: Exception => {
        logger.debug("Data not found for keys:" + notFoundKeys.map(key => key.mkString(",")).mkString(","), e)
      }
    }
    if (results.size > 0) {
      retVal ++= results
      val groupedVals = results.groupBy(kv => getParallelBucketIdx(kv._1))
      groupedVals.foreach(idxAndValues => {
        _adapterUniqKeyValBktlocks(idxAndValues._1).writeLock().lock()
        try {
          idxAndValues._2.foreach(kv => {
            _adapterUniqKeyValBuckets(idxAndValues._1)(kv._1) = kv._2
          })
        } catch {
          case e: Exception => {
            throw e
          }
        } finally {
          _adapterUniqKeyValBktlocks(idxAndValues._1).writeLock().unlock()
        }
      })
    }
    return retVal.toArray
  }

  // Prime number
  //  private[this] val _buckets = 257
  // Prime number
  private[this] val _parallelBuciets = 11

  private[this] val _adapterUniqKeyValBuckets = new Array[scala.collection.mutable.Map[String, String]](_parallelBuciets)
  private[this] val _adapterUniqKeyValBktlocks = new Array[ReentrantReadWriteLock](_parallelBuciets)

  for (i <- 0 until _parallelBuciets) {
    //    _modelsRsltBuckets(i) = scala.collection.mutable.Map[String, scala.collection.mutable.Map[String, SavedMdlResult]]()
    //    _modelsRsltBktlocks(i) = new ReentrantReadWriteLock(true);
    _adapterUniqKeyValBuckets(i) = scala.collection.mutable.Map[String, String]()
    _adapterUniqKeyValBktlocks(i) = new ReentrantReadWriteLock(true);
  }

  private[this] var _metadataLoader: KamanjaLoaderInfo = null
  private[this] var _adaptersAndEnvCtxtLoader: KamanjaLoaderInfo = null
  private[this] var _sysCatalogDatastore: DataStore = null
  private[this] var _objectResolver: ObjectResolver = null
  private[this] var _enableEachTransactionCommit = true
  private[this] var _jarPaths: collection.immutable.Set[String] = null // Jar paths where we can resolve all jars (including dependency jars).

  private def GetDataStoreHandle(jarPaths: collection.immutable.Set[String], dataStoreInfo: String): DataStore = {
    try {
      logger.debug("Getting DB Connection for dataStoreInfo:%s".format(dataStoreInfo))
      return KeyValueManager.Get(jarPaths, dataStoreInfo, null, null)
    } catch {
      case e: Exception => throw e
      case e: Throwable => throw e
    }
  }

  val results = new ArrayBuffer[(String, (Long, String, List[(String, String)]))]()

  private def buildAdapterUniqueValue(k: Key, v: Any, results: ArrayBuffer[(String, String)]) {
    if (v != null && v.isInstanceOf[Array[Byte]]) {
      results += ((k.bucketKey(0), new String(v.asInstanceOf[Array[Byte]]))) // taking 1st key, that is what we are expecting
    }
  }

  private def collectKeyAndValues(k: Key, v: Any, loadedData: ArrayBuffer[(KeyWithBucketIdAndPrimaryKey, ContainerInterface)]): Unit = {
    if (v != null && v.isInstanceOf[ContainerInterface]) {
      if (logger.isDebugEnabled)
        logger.debug("Key:(%d, %s, %d, %d)".format(k.timePartition, k.bucketKey.mkString(","), k.transactionId, k.rowId))
      val value = v.asInstanceOf[ContainerInterface]
      val primarykey = value.getPrimaryKey
      val key = KeyWithBucketIdAndPrimaryKey(KeyWithBucketIdAndPrimaryKeyCompHelper.BucketIdForBucketKey(k.bucketKey), k, primarykey != null && primarykey.size > 0, primarykey)

      try {
        loadedData += ((key, value))
      } catch {
        case e: Exception => {
          throw e
        }
      }
    }
  }

  private def getData(tenantId: String, contName: String, tmRangeValues1: Array[TimeRange], partKeys: Array[Array[String]], f: ContainerInterface => Boolean): Array[(KeyWithBucketIdAndPrimaryKey, ContainerInterface)] = {
    val loadedData = ArrayBuffer[(KeyWithBucketIdAndPrimaryKey, ContainerInterface)]()
    val tmRangeValues = {
      if (partKeys.size > 0 && tmRangeValues1.size == 0) {
        partKeys.map(k => TimeRange(Long.MinValue, Long.MaxValue))
      } else {
        tmRangeValues1
      }
    }


    val tenantInfo: TenantEnvCtxtInfo = if (tmRangeValues.size == partKeys.size) _tenantIdMap.getOrElse(tenantId.toLowerCase(), null) else null
    if (tenantInfo != null && tenantInfo.datastore != null && tmRangeValues.size == partKeys.size) {
      val tmpDatastore = tenantInfo.datastore
      val containerName = contName.toLowerCase
      val buildOne = (k: Key, v: Any, serType: String, typ: String, ver: Int) => {
        if (f != null) {
          if (v != null && v.isInstanceOf[ContainerInterface] && f(v.asInstanceOf[ContainerInterface])) {
            collectKeyAndValues(k, v, loadedData)
          }
        } else {
          if (v != null && v.isInstanceOf[ContainerInterface]) {
            collectKeyAndValues(k, v, loadedData)
          }
        }
      }

      // Check whether this table is in Cache or not
      var cacheContainer = tenantInfo.cachedContainers.getOrElse(containerName, null)

      if (tmRangeValues.size > 0) {
        for (i <- 0 until tmRangeValues.size) {
          val bk = partKeys(i)
          val tr = tmRangeValues(i)
          if (TxnContextCommonFunctions.IsEmptyKey(bk) == false) {
            // println("1. containerName:" + containerName)
            val tr1 = if (tr != null) tr else TimeRange(Long.MinValue, Long.MaxValue)

            try {
              logger.debug("Table %s Key %s for timerange: (%d,%d)".format(containerName, bk.mkString(","), tr1.beginTime, tr1.endTime))
              if (cacheContainer != null) {
                TxnContextCommonFunctions.ReadLockContainer(cacheContainer)
                logger.debug("Going to cached contaienr (%s) to get the data for bk".format(containerName))
                try {
                  val fndValuesAndKeys = TxnContextCommonFunctions.getRddData(cacheContainer, bk.toList, tr, null, f)
                  if (fndValuesAndKeys != null && fndValuesAndKeys.size > 0)
                    loadedData ++= fndValuesAndKeys.map(kv => (kv._1, Clone(kv._2)))
                } catch {
                  case e: Exception => {
                    throw e
                  }
                } finally {
                  TxnContextCommonFunctions.ReadUnlockContainer(cacheContainer)
                }
              } else {
                if (tr != null)
                  callGetData(tmpDatastore, containerName, Array(tr), Array(bk), buildOne)
                else
                  callGetData(tmpDatastore, containerName, Array(bk), buildOne)
              }
            } catch {
              case e: ObjectNotFoundException => {
                logger.debug("Table %s Key %s Not found for timerange: (%d,%d)".format(containerName, bk.mkString(","), tr1.beginTime, tr1.endTime), e)
              }
              case e: Exception => {
                logger.error("Table %s Key %s Not found for timerange: (%d,%d)".format(containerName, bk.mkString(","), tr1.beginTime, tr1.endTime), e)
              }
            }
          } else if (tr != null) {
            // println("2. containerName:" + containerName)
            try {
              logger.debug("Table %s Key %s for timerange: (%d,%d)".format(containerName, bk.mkString(","), tr.beginTime, tr.endTime))
              if (cacheContainer != null) {
                TxnContextCommonFunctions.ReadLockContainer(cacheContainer)
                try {
                  logger.debug("Going to cached contaienr (%s) to get the data for tr".format(containerName))
                  val fndValuesAndKeys = TxnContextCommonFunctions.getRddData(cacheContainer, null, tr, null, f)
                  if (fndValuesAndKeys != null && fndValuesAndKeys.size > 0)
                    loadedData ++= fndValuesAndKeys.map(kv => (kv._1, Clone(kv._2)))
                } catch {
                  case e: Exception => {
                    throw e
                  }
                } finally {
                  TxnContextCommonFunctions.ReadUnlockContainer(cacheContainer)
                }
              } else {
                callGetData(tmpDatastore, containerName, Array(tr), buildOne)
              }
            } catch {
              case e: ObjectNotFoundException => {
                logger.debug("Table %s Key %s Not found for timerange: (%d,%d)".format(containerName, bk.mkString(","), tr.beginTime, tr.endTime), e)
              }
              case e: Exception => {
                logger.error("Table %s Key %s Not found for timerange: (%d,%d)".format(containerName, bk.mkString(","), tr.beginTime, tr.endTime), e)
              }
            }
          } else {
            // println("3. containerName:" + containerName)
            try {
              logger.debug("Table %s".format(containerName))
              if (cacheContainer != null) {
                TxnContextCommonFunctions.ReadLockContainer(cacheContainer)
                try {
                  logger.debug("Going to cached contaienr (%s) to get the data".format(containerName))
                  val fndValuesAndKeys = TxnContextCommonFunctions.getRddData(cacheContainer, null, tr, null, f)
                  if (fndValuesAndKeys != null && fndValuesAndKeys.size > 0)
                    loadedData ++= fndValuesAndKeys.map(kv => (kv._1, Clone(kv._2)))
                } catch {
                  case e: Exception => {
                    throw e
                  }
                } finally {
                  TxnContextCommonFunctions.ReadUnlockContainer(cacheContainer)
                }
              } else {
                callGetData(tmpDatastore, containerName, buildOne)
              }
            } catch {
              case e: ObjectNotFoundException => {
                logger.debug("Table %s".format(containerName), e)
              }
              case e: Exception => {
                logger.error("Table %s".format(containerName), e)
              }
            }
          }
        }
      } else {
        // println("containerName:" + containerName)
        try {
          logger.debug("Table %s".format(containerName))
          if (cacheContainer != null) {
            TxnContextCommonFunctions.ReadLockContainer(cacheContainer)
            try {
              logger.debug("Going to cached contaienr (%s) to get the data".format(containerName))
              val fndValuesAndKeys = TxnContextCommonFunctions.getRddData(cacheContainer, null, null, null, f)
              if (fndValuesAndKeys != null && fndValuesAndKeys.size > 0)
                loadedData ++= fndValuesAndKeys.map(kv => (kv._1, Clone(kv._2)))
            } catch {
              case e: Exception => {
                throw e
              }
            } finally {
              TxnContextCommonFunctions.ReadUnlockContainer(cacheContainer)
            }
          } else {
            callGetData(tmpDatastore, containerName, buildOne)
          }
        } catch {
          case e: ObjectNotFoundException => {
            logger.debug("Table %s".format(containerName), e)
          }
          case e: Exception => {
            logger.error("Table %s".format(containerName), e)
          }
        }
      }
    } else if (tmRangeValues.size != partKeys.size) {
      logger.error("Table %s getData request tmRangeValues.size(%d) != partKeys.size(%d)".format(contName, tmRangeValues.size, partKeys.size))
    } else if (tenantInfo == null || tenantInfo.datastore == null) {
      logger.warn("Table %s getData request does not find tenantinfo with datastore".format(contName))
    }
    loadedData.toArray
  }

  override def setMetadataLoader(metadataLoader: KamanjaLoaderInfo): Unit = {
    _metadataLoader = metadataLoader
  }

  override def getMetadataLoader: KamanjaLoaderInfo = _metadataLoader

  override def setAdaptersAndEnvCtxtLoader(adaptersAndEnvCtxtLoader: KamanjaLoaderInfo): Unit = {
    _adaptersAndEnvCtxtLoader = adaptersAndEnvCtxtLoader
  }

  override def getAdaptersAndEnvCtxtLoader: KamanjaLoaderInfo = _adaptersAndEnvCtxtLoader


  override def setObjectResolver(objResolver: ObjectResolver): Unit = {
    _objectResolver = objResolver
  }

  //BUGBUG:: May be we need to lock before we do anything here
  override def Shutdown: Unit = {

    isShutdown = true

    if (_listenerCache != null) {
      try {
        _listenerCache.shutdown()
      } catch {
        case e: Throwable => {
          logger.error("Failed to shutdown EnvCtxtListenersCache", e)
        }
      }
      _listenerCache = null
    }

    if (_listenerConfigClusterCache != null) {
      try {
        _listenerConfigClusterCache.shutdown()
      } catch {
        case e: Throwable => {
          logger.error("Failed to shutdown EnvCtxtConfigClusterCache", e)
        }
      }
      _listenerConfigClusterCache = null
    }

    if (_adapterUniqKeyValBuckets != null) {
      for (i <- 0 until _parallelBuciets) {
        _adapterUniqKeyValBktlocks(i).writeLock().lock()
        try {
          _adapterUniqKeyValBuckets(i).clear()
        } catch {
          case e: Exception => {
            logger.warn("", e)
            // throw e
          }
        } finally {
          _adapterUniqKeyValBktlocks(i).writeLock().unlock()
        }
      }
    }

    _tenantIdMap.foreach(tInfo => {
      if (tInfo != null && tInfo._2 != null && tInfo._2.datastore != null) {
        if (tInfo._2.datastore != null)
          tInfo._2.datastore.Shutdown()
        tInfo._2.cachedContainers.clear()
        tInfo._2.containersNames.clear()
      }
    })

    _tenantIdMap.clear()

    if (_sysCatalogDatastore != null)
      _sysCatalogDatastore.Shutdown
    _sysCatalogDatastore = null
    // _messagesOrContainers.clear

    hbExecutor.shutdownNow
    while (!hbExecutor.isTerminated) {
      Thread.sleep(100) // sleep 100ms and then check
    }

    CloseSetDataZkc


    WriteLock(_zkListeners_reent_lock)
    try {
      _zkListeners.foreach(l => {
        if (l != null)
          l.Shutdown
      })
      _zkListeners.clear()
    } catch {
      case e: Throwable => {}
    } finally {
      WriteUnlock(_zkListeners_reent_lock)
    }

    WriteLock(_zkLeader_reent_lock)
    try {
      if (_zkLeaderLatch != null)
        _zkLeaderLatch.Shutdown
      _zkLeaderLatch = null
      _zkLeaderListeners.clear()
    } catch {
      case e: Throwable => {}
    } finally {
      WriteUnlock(_zkLeader_reent_lock)
    }
  }

  override def getPropertyValue(clusterId: String, key: String): String = {
    _mgr.GetUserProperty(clusterId, key)
  }

  override def setJarPaths(jarPaths: collection.immutable.Set[String]): Unit = {
    if (jarPaths != null) {
      logger.debug("JarPaths:%s".format(jarPaths.mkString(",")))
    }
    _jarPaths = jarPaths
  }

  override def getJarPaths(): collection.immutable.Set[String] = _jarPaths

  override def openTenantsPrimaryDatastores(): Unit = {
    val allTenants = _mgr.GetAllTenantInfos

    if (allTenants == null) return

    allTenants.foreach(tenantInfo => {
      if (tenantInfo != null && tenantInfo.primaryDataStore != null && tenantInfo.primaryDataStore.trim.size > 0) {
        try {
          val tenantPrimaryDatastore = GetDataStoreHandle(_jarPaths, tenantInfo.primaryDataStore)
          if (tenantPrimaryDatastore != null) {
            tenantPrimaryDatastore.setObjectResolver(_objectResolver)
            tenantPrimaryDatastore.setDefaultSerializerDeserializer("com.ligadata.kamanja.serializer.kbinaryserdeser", Map[String, Any]())
          }
          _tenantIdMap(tenantInfo.tenantId.toLowerCase()) = TenantEnvCtxtInfo(tenantInfo, tenantPrimaryDatastore, scala.collection.mutable.Map[String, MsgContainerInfo](), scala.collection.mutable.Set[String]())
        } catch {
          case e: Throwable => {
            logger.error("Failed to connect to datastore for tenantId:%swith configuration:%s".format(tenantInfo.tenantId, tenantInfo.primaryDataStore), e)
            throw e
          }
        }
      } else {
        _tenantIdMap(tenantInfo.tenantId.toLowerCase()) = TenantEnvCtxtInfo(tenantInfo, null, scala.collection.mutable.Map[String, MsgContainerInfo](), scala.collection.mutable.Set[String]())
      }
    })
  }

  override def cacheContainers(clusterId: String): Unit = {
    val tmpContainersStr = _mgr.GetUserProperty(clusterId, "containers2cache")
    val containersNames = if (tmpContainersStr != null) tmpContainersStr.trim.toLowerCase.split(",").map(s => s.trim).filter(s => s.size > 0) else Array[String]()
    if (containersNames.size > 0) {
      val tenantAndContainers = containersNames.map(c => {
        var tenatId: String = ""
        val msg = _objectResolver.getMdMgr.Message(c, -1, true)
        if (msg == None) {
          val container = _objectResolver.getMdMgr.Container(c, -1, true)
          if (container != None)
            tenatId = container.get.tenantId
        } else {
          tenatId = msg.get.tenantId
        }
        (tenatId, c)
      }).filter(tc => (tc._1 != null && tc._1.trim.size > 0)).groupBy(_._1)

      tenantAndContainers.foreach(tcKv => {
        val tenantInfo = _tenantIdMap.getOrElse(tcKv._1.toLowerCase(), null)
        if (tenantInfo != null) {
          tcKv._2.foreach(tc => {
            val c = tc._2
            var cacheContainer = tenantInfo.cachedContainers.getOrElse(c, null)
            if (cacheContainer == null) {
              // Load the container data into cache
              cacheContainer = new MsgContainerInfo(true)
              val loadedData = ArrayBuffer[(KeyWithBucketIdAndPrimaryKey, ContainerInterface)]()

              val buildOne = (k: Key, v: Any, serType: String, typ: String, ver: Int) => {
                if (v != null && v.isInstanceOf[ContainerInterface]) {
                  collectKeyAndValues(k, v, loadedData)
                }
              }
              if (tenantInfo.datastore != null)
                callGetData(tenantInfo.datastore, c, buildOne)
              TxnContextCommonFunctions.WriteLockContainer(cacheContainer)
              try {
                loadedData.foreach(kv => {
                  cacheContainer.dataByBucketKey.put(kv._1, kv._2)
                  cacheContainer.dataByTmPart.put(kv._1, kv._2)
                })
              } catch {
                case e: Exception => {
                  throw e
                }
              } finally {
                TxnContextCommonFunctions.WriteUnlockContainer(cacheContainer)
              }
              tenantInfo.cachedContainers(c) = cacheContainer
            }
          })
        }
      })
    }
  }

  private def Clone(vals: Array[ContainerInterface]): Array[ContainerInterface] = {
    if (vals == null) return null
    return vals.map(v => {
      if (v == null) null else v.Clone().asInstanceOf[ContainerInterface]
    })
  }

  private def Clone(v: ContainerInterface): ContainerInterface = {
    if (v == null) return null
    return v.Clone().asInstanceOf[ContainerInterface]
  }

  private def Clone(ov: Option[ContainerInterface]): Option[ContainerInterface] = {
    if (ov == None) return ov
    Some(ov.get.Clone().asInstanceOf[ContainerInterface])
  }

  override def getAllObjects(tenantId: String, containerName: String): Array[ContainerInterface] = {
    val data = getData(tenantId, containerName, Array[TimeRange](), Array[Array[String]](), null)
    data.map(d => d._2)
  }

  override def getObject(tenantId: String, containerName: String, partKey: List[String], primaryKey: List[String]): ContainerInterface = {
    val data = getData(tenantId, containerName, Array[TimeRange](), if (partKey != null) Array(partKey.toArray) else Array[Array[String]](), null)
    if (primaryKey != null) {
      val primKeyAsArray = primaryKey.toArray
      data.foreach(d => {
        if (primKeyAsArray.sameElements(d._2.getPrimaryKey)) {
          return d._2;
        }
      })
    } else {
      if (data.size > 0)
        return data(0)._2
    }
    null
  }

  override def getHistoryObjects(tenantId: String, containerName: String, partKey: List[String], appendCurrentChanges: Boolean): Array[ContainerInterface] = {
    val data = getData(tenantId, containerName, Array[TimeRange](), if (partKey != null) Array(partKey.toArray) else Array[Array[String]](), null)
    data.map(d => d._2)
  }

  /**
    * Does the supplied key exist in a container with the supplied name?
    */
  override def contains(tenantId: String, containerName: String, partKey: List[String], primaryKey: List[String]): Boolean = {
    val obj = getObject(tenantId, containerName, partKey, primaryKey)
    (obj != null)
  }

  /**
    * Does at least one of the supplied keys exist in a container with the supplied name?
    */
  override def containsAny(tenantId: String, containerName: String, partKeys: Array[List[String]], primaryKeys: Array[List[String]]): Boolean = {
    if (partKeys != null && primaryKeys != null) {
      if (partKeys.size > 0 && primaryKeys.size > 0) {
        for (i <- 0 until partKeys.size) {
          val obj = getObject(tenantId, containerName, partKeys(i), primaryKeys(i))
          if (obj != null)
            return true
        }
      } else {
        throw new KamanjaException("Primary Keys %d and Partition Keys %d does not match".format(primaryKeys.size, partKeys.size), null)
      }
    }
    false
  }

  /**
    * Do all of the supplied keys exist in a container with the supplied name?
    */
  override def containsAll(tenantId: String, containerName: String, partKeys: Array[List[String]], primaryKeys: Array[List[String]]): Boolean = {
    if (partKeys != null && primaryKeys != null) {
      if (partKeys.size > 0 && primaryKeys.size > 0) {
        for (i <- 0 until partKeys.size) {
          val obj = getObject(tenantId, containerName, partKeys(i), primaryKeys(i))
          if (obj == null)
            return false
        }
        return true
      } else {
        throw new KamanjaException("Primary Keys %d and Partition Keys %d does not match".format(primaryKeys.size, partKeys.size), null)
      }
    }
    false
  }

  override def setObject(tenantId: String, containerName: String, partKey: List[String], value: ContainerInterface): Unit = {
    //      if (value != null && TxnContextCommonFunctions.IsEmptyKey(partKey) == false)
    //        localSetObject(transId, containerName, Array(value.getTimePartitionData), Array(partKey.toArray), Array(value))
    throw new KamanjaException("Function call is not implemented", null)
  }

  override def rollbackData(): Unit = {
    //    removeTransactionContext(transId)
    // throw new KamanjaException("Function call is not implemented", null)
  }

  override def commitData(tenantId: String, txnCtxt: TransactionContext, data: Array[(String, Array[ContainerInterface])], forceCommit: Boolean): Unit = {
    val tenantInfo: TenantEnvCtxtInfo = _tenantIdMap.getOrElse(tenantId.toLowerCase(), null)
    if (tenantInfo == null) return

    if (tenantInfo.cachedContainers.size > 0) {
      data.foreach(oneContainerData => {
        val cacheContainer = tenantInfo.cachedContainers.getOrElse(oneContainerData._1, null)
        if (cacheContainer != null) {
          oneContainerData._2.foreach(containerInst => {
            val tmpPartitionkey = containerInst.getPartitionKey
            val tmpPrimarykey = containerInst.getPrimaryKey
            val partitionkey = if (tmpPartitionkey != null) tmpPartitionkey else Array[String]()
            val primarykey = if (tmpPrimarykey != null) tmpPrimarykey else Array[String]();
            val tmRng = containerInst.TimePartitionData()
            val key = KeyWithBucketIdAndPrimaryKey(KeyWithBucketIdAndPrimaryKeyCompHelper.BucketIdForBucketKey(partitionkey),
              Key(containerInst.TimePartitionData(), containerInst.PartitionKeyData(), containerInst.TransactionId(), containerInst.RowNumber()),
              primarykey != null && primarykey.size > 0, primarykey)
            cacheContainer.dataByBucketKey.put(key, containerInst)
          })
        }
      })
    }

    if (forceCommit || EnableEachTransactionCommit) {
      tenantInfo.datastore.putContainers(txnCtxt, data)
    }
  }

  override def ReloadKeys(tempTransId: Long, tenatId: String, containerName: String, keys: List[Key]): Unit = {
    if (tenatId == null || tenatId.trim.size == 0 || containerName == null || keys == null || keys.size == 0) return;
    val contName = containerName.toLowerCase

    val tenantInfo: TenantEnvCtxtInfo = _tenantIdMap.getOrElse(tenatId.toLowerCase(), null)
    if (tenantInfo == null) return None

    if (tenantInfo.cachedContainers != null) {
      var cacheContainer = tenantInfo.cachedContainers.getOrElse(containerName, null)
      if (cacheContainer != null) {
        val loadedData = ArrayBuffer[(KeyWithBucketIdAndPrimaryKey, ContainerInterface)]()

        val buildOne = (k: Key, v: Any, serType: String, typ: String, ver: Int) => {
          if (v != null && v.isInstanceOf[ContainerInterface]) {
            collectKeyAndValues(k, v, loadedData)
          }
        }
        if (tenantInfo.datastore != null)
          callGetData(tenantInfo.datastore, containerName, keys.toArray, buildOne)

        if (loadedData.size > 0) {
          TxnContextCommonFunctions.WriteLockContainer(cacheContainer)
          try {
            loadedData.foreach(kv => {
              cacheContainer.dataByBucketKey.put(kv._1, kv._2)
              cacheContainer.dataByTmPart.put(kv._1, kv._2)
            })
          } catch {
            case e: Exception => {
              throw e
            }
          } finally {
            TxnContextCommonFunctions.WriteUnlockContainer(cacheContainer)
          }
        }
      }
    }
  }

  override def getRecent(tenantId: String, containerName: String, partKey: List[String], tmRange: TimeRange, f: ContainerInterface => Boolean): Option[ContainerInterface] = {
    val tenantInfo: TenantEnvCtxtInfo = _tenantIdMap.getOrElse(tenantId.toLowerCase(), null)
    if (tenantInfo == null) return None

    if (tenantInfo.cachedContainers != null) {
      var cacheContainer = tenantInfo.cachedContainers.getOrElse(containerName, null)
      if (cacheContainer != null) {
        val inMemoryRecent = TxnContextCommonFunctions.getRecent(cacheContainer, partKey, tmRange, null, f)
        if (inMemoryRecent != null && inMemoryRecent._1 != null)
          Some(inMemoryRecent._1)
        else
          None
      }
    }

    val data = getData(tenantId, containerName, if (tmRange != null) Array(tmRange) else Array[TimeRange](), if (partKey != null) Array(partKey.toArray) else Array[Array[String]](), f)
    if (data.size > 0) {
      Some(data(0)._2)
    } else {
      None
    }
  }

  override def getRDD(tenantId: String, containerName: String, partKey: List[String], tmRange: TimeRange, f: ContainerInterface => Boolean): Array[ContainerInterface] = {
    val data = getData(tenantId, containerName, if (tmRange != null) Array(tmRange) else Array[TimeRange](), if (partKey != null) Array(partKey.toArray) else Array[Array[String]](), f)
    data.map(d => d._2)
  }

  private def callSaveData(dataStore: DataStoreOperations, data_list: Array[(String, Array[(Key, String, Any)])]): Unit = {
    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs
    var doneSave = false

    while (!doneSave) {
      try {
        dataStore.put(null, data_list)
        incrementWriteCount
        doneSave = true
      } catch {
        case e: FatalAdapterException => {
          logger.error("Failed to save data into datastore", e)
        }
        case e: StorageDMLException => {
          logger.error("Failed to save data into datastore", e)
        }
        case e: StorageDDLException => {
          logger.error("Failed to save data into datastore", e)
        }
        case e: Exception => {
          logger.error("Failed to save data into datastore", e)
        }
        case e: Throwable => {
          logger.error("Failed to save data into datastore", e)
        }
      }

      if (!doneSave) {
        try {
          logger.error("Failed to save data into datastore. Waiting for another %d milli seconds and going to start them again.".format(failedWaitTime))
          Thread.sleep(failedWaitTime)
        } catch {
          case e: Exception => {
            logger.warn("", e)

          }
        }
        // Adjust time for next time
        if (failedWaitTime < maxFailedWaitTime) {
          failedWaitTime = failedWaitTime * 2
          if (failedWaitTime > maxFailedWaitTime)
            failedWaitTime = maxFailedWaitTime
        }
      }
    }
  }

  // BUGBUG:: We can make all gets as simple template for exceptions handling and call that.
  private def callGetData(dataStore: DataStoreOperations, containerName: String, callbackFunction: (Key, Any, String, String, Int) => Unit): Unit = {
    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs
    var doneGet = false

    while (!doneGet) {
      try {
        dataStore.get(containerName, callbackFunction)
        incrementReadCount
        doneGet = true
      } catch {
        case e@(_: ObjectNotFoundException | _: KeyNotFoundException) => {
          logger.debug("Failed to get data from container:%s".format(containerName), e)
          doneGet = true
        }
        case e: FatalAdapterException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: StorageDMLException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: StorageDDLException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: Exception => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: Throwable => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
      }

      if (!doneGet) {
        try {
          logger.error("Failed to get data from datastore. Waiting for another %d milli seconds and going to start them again.".format(failedWaitTime))
          Thread.sleep(failedWaitTime)
        } catch {
          case e: Exception => {
            logger.warn("", e)
          }
        }
        // Adjust time for next time
        if (failedWaitTime < maxFailedWaitTime) {
          failedWaitTime = failedWaitTime * 2
          if (failedWaitTime > maxFailedWaitTime)
            failedWaitTime = maxFailedWaitTime
        }
      }
    }
  }

  private def callGetData(dataStore: DataStoreOperations, containerName: String, keys: Array[Key], callbackFunction: (Key, Any, String, String, Int) => Unit): Unit = {
    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs
    var doneGet = false

    while (!doneGet) {
      try {
        dataStore.get(containerName, keys, callbackFunction)
        incrementReadCount
        doneGet = true
      } catch {
        case e@(_: ObjectNotFoundException | _: KeyNotFoundException) => {
          logger.debug("Failed to get data from container:%s".format(containerName), e)
          doneGet = true
        }
        case e: FatalAdapterException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: StorageDMLException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: StorageDDLException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: Exception => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: Throwable => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
      }

      if (!doneGet) {
        try {
          logger.error("Failed to get data from datastore. Waiting for another %d milli seconds and going to start them again.".format(failedWaitTime))
          Thread.sleep(failedWaitTime)
        } catch {
          case e: Exception => {
            logger.warn("", e)
          }
        }
        // Adjust time for next time
        if (failedWaitTime < maxFailedWaitTime) {
          failedWaitTime = failedWaitTime * 2
          if (failedWaitTime > maxFailedWaitTime)
            failedWaitTime = maxFailedWaitTime
        }
      }
    }
  }

  private def callGetData(dataStore: DataStoreOperations, containerName: String, timeRanges: Array[TimeRange], callbackFunction: (Key, Any, String, String, Int) => Unit): Unit = {
    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs
    var doneGet = false

    while (!doneGet) {
      try {
        dataStore.get(containerName, timeRanges, callbackFunction)
        incrementReadCount
        doneGet = true
      } catch {
        case e@(_: ObjectNotFoundException | _: KeyNotFoundException) => {
          logger.debug("Failed to get data from container:%s".format(containerName), e)
          doneGet = true
        }
        case e: FatalAdapterException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: StorageDMLException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: StorageDDLException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: Exception => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: Throwable => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
      }

      if (!doneGet) {
        try {
          logger.error("Failed to get data from datastore. Waiting for another %d milli seconds and going to start them again.".format(failedWaitTime))
          Thread.sleep(failedWaitTime)
        } catch {
          case e: Exception => {
            logger.warn("", e)
          }
        }
        // Adjust time for next time
        if (failedWaitTime < maxFailedWaitTime) {
          failedWaitTime = failedWaitTime * 2
          if (failedWaitTime > maxFailedWaitTime)
            failedWaitTime = maxFailedWaitTime
        }
      }
    }
  }

  private def callGetData(dataStore: DataStoreOperations, containerName: String, timeRanges: Array[TimeRange], bucketKeys: Array[Array[String]], callbackFunction: (Key, Any, String, String, Int) => Unit): Unit = {
    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs
    var doneGet = false

    while (!doneGet) {
      try {
        dataStore.get(containerName, timeRanges, bucketKeys, callbackFunction)
        incrementReadCount
        doneGet = true
      } catch {
        case e@(_: ObjectNotFoundException | _: KeyNotFoundException) => {
          logger.debug("Failed to get data from container:%s".format(containerName), e)
          doneGet = true
        }
        case e: FatalAdapterException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: StorageDMLException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: StorageDDLException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: Exception => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: Throwable => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
      }

      if (!doneGet) {
        try {
          logger.error("Failed to get data from datastore. Waiting for another %d milli seconds and going to start them again.".format(failedWaitTime))
          Thread.sleep(failedWaitTime)
        } catch {
          case e: Exception => {
            logger.warn("", e)
          }
        }
        // Adjust time for next time
        if (failedWaitTime < maxFailedWaitTime) {
          failedWaitTime = failedWaitTime * 2
          if (failedWaitTime > maxFailedWaitTime)
            failedWaitTime = maxFailedWaitTime
        }
      }
    }
  }

  private def callGetData(dataStore: DataStoreOperations, containerName: String, bucketKeys: Array[Array[String]], callbackFunction: (Key, Any, String, String, Int) => Unit): Unit = {
    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs
    var doneGet = false

    while (!doneGet) {
      try {
        dataStore.get(containerName, bucketKeys, callbackFunction)
        incrementReadCount
        doneGet = true
      } catch {
        case e@(_: ObjectNotFoundException | _: KeyNotFoundException) => {
          logger.debug("Failed to get data from container:%s".format(containerName), e)
          doneGet = true
        }
        case e: FatalAdapterException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: StorageDMLException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: StorageDDLException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: Exception => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: Throwable => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
      }

      if (!doneGet) {
        try {
          logger.error("Failed to get data from datastore. Waiting for another %d milli seconds and going to start them again.".format(failedWaitTime))
          Thread.sleep(failedWaitTime)
        } catch {
          case e: Exception => {
            logger.warn("", e)
          }
        }
        // Adjust time for next time
        if (failedWaitTime < maxFailedWaitTime) {
          failedWaitTime = failedWaitTime * 2
          if (failedWaitTime > maxFailedWaitTime)
            failedWaitTime = maxFailedWaitTime
        }
      }
    }
  }

  override def EnableEachTransactionCommit: Boolean = _enableEachTransactionCommit

  private def incrementReadCount: Unit = {
    val curr: Long = metrics.getOrElse(SimpleEnvContextImpl.STORAGE_READ_COUNT, 0)
    if (curr == 0) {
      metrics(SimpleEnvContextImpl.STORAGE_READ_COUNT) = 1
      return
    }
    metrics(SimpleEnvContextImpl.STORAGE_READ_COUNT) = curr + 1
  }

  private def incrementWriteCount: Unit = {
    val curr: Long = metrics.getOrElse(SimpleEnvContextImpl.STORAGE_WRITE_COUNT, 0)
    if (curr == 0) {
      metrics(SimpleEnvContextImpl.STORAGE_WRITE_COUNT) = 1
      return
    }
    metrics(SimpleEnvContextImpl.STORAGE_WRITE_COUNT) = curr + 1
  }

  override def saveObjectInNodeCache(key: String, value: Any): Unit = {
    WriteLock(_nodeCache_reent_lock)
    try {
      _nodeCacheMap(key) = value;
    } catch {
      case e: Throwable => {
        throw e
      }
    }
    finally {
      WriteUnlock(_nodeCache_reent_lock)
    }
  }

  override def getObjectFromNodeCache(key: String): Any = {
    var retVal: Any = null
    ReadLock(_nodeCache_reent_lock)
    try {
      retVal = _nodeCacheMap.getOrElse(key, null)
    } catch {
      case e: Throwable => {
        throw e
      }
    }
    finally {
      ReadUnlock(_nodeCache_reent_lock)
    }

    retVal
  }

  override def getAllKeysFromNodeCache(): Array[String] = {
    var retVal = Array[String]()
    ReadLock(_nodeCache_reent_lock)
    try {
      retVal = _nodeCacheMap.keySet.toArray
    } catch {
      case e: Throwable => {
        throw e
      }
    }
    finally {
      ReadUnlock(_nodeCache_reent_lock)
    }

    retVal
  }

  override def getAllObjectsFromNodeCache(): Array[KeyValuePair] = {
    var retVal = Array[KeyValuePair]()
    ReadLock(_nodeCache_reent_lock)
    try {
      retVal = _nodeCacheMap.map(kv => KeyValuePair(kv._1, kv._2)).toArray
    } catch {
      case e: Throwable => {
        throw e
      }
    }
    finally {
      ReadUnlock(_nodeCache_reent_lock)
    }

    retVal
  }

  // Saving & getting data
  override def saveDataInPersistentStore(containerName: String, key: String, serializerType: String, value: Array[Byte]): Unit = {
    val oneContData = Array((containerName, Array((Key(0, Array(key), 0, 0), serializerType, value.asInstanceOf[Any]))))
    callSaveData(_sysCatalogDatastore, oneContData)
  }

  override def getDataFromPersistentStore(containerName: String, key: String): SerializerTypeValuePair = {
    var retVal = SerializerTypeValuePair(null, null)
    val buildOne = (k: Key, v: Any, serType: String, typ: String, ver: Int) => {
      retVal = SerializerTypeValuePair(serType, v.asInstanceOf[Array[Byte]])
    }
    callGetData(_sysCatalogDatastore, containerName, Array(Key(0, Array(key), 0, 0)), buildOne)
    retVal
  }

  // Zookeeper functions
  private def CloseSetDataZkc: Unit = {
    WriteLock(_setZk_reent_lock)
    try {
      if (_setZkData != null)
        _setZkData.close
      _setZkData = null
    } catch {
      case e: Throwable => {
        logger.warn("KamanjaLeader: unable to close zk connection due to", e)
      }
    }
    finally {
      WriteUnlock(_setZk_reent_lock)
    }
    _setZkData = null
  }

  private def ReconnectToSetDataZkc: Unit = {
    CloseSetDataZkc
    WriteLock(_setZk_reent_lock)
    try {
      _setZkData = CreateClient.createSimple(_zkConnectString, _zkSessionTimeoutMs, _zkConnectionTimeoutMs)
    } catch {
      case e: Throwable => {
        logger.warn("KamanjaLeader: unable to create new zk connection due to", e)
      }
    }
    finally {
      WriteUnlock(_setZk_reent_lock)

    }
  }

  private def setData2ZNode(zNodePath: String, value: Array[Byte]): Unit = {
    if (!hasZkConnectionString)
      throw new KamanjaException("Zookeeper information is not yet set", null)
    val MAX_ZK_RETRIES = 1
    var finalException: Throwable = null
    WriteLock(_setZk_reent_lock)
    try {
      var retriesAttempted = 0
      var run = true

      while (run) {
        run = false
        finalException = null
        try {
          if (retriesAttempted > 0) {
            logger.warn("retrying zk call (setDataToZNode)")
          }
          if (_setZkData != null) {
            _setZkData.setData().forPath(zNodePath, value)
          } else {
            throw new KamanjaException("Connection to ZK does not exists", null)
          }
        } catch {
          case e: org.apache.zookeeper.KeeperException.NoNodeException => {
            finalException = e
            // No need to rerun. Just exit
          }
          case e: Throwable => {
            finalException = e
            retriesAttempted += 1
            if (_setZkData != null) {
              logger.warn("Connection to Zookeeper is temporarily unavailable (setDataToZNode).due to ", e)
            }
            ReconnectToSetDataZkc
            run = (retriesAttempted <= MAX_ZK_RETRIES)
          }
        }
      }
    } catch {
      case e: Throwable => {
        finalException = e
      }
    } finally {
      WriteUnlock(_setZk_reent_lock)
    }

    if (finalException != null) {
      throw finalException
    }
  }

  private def ValidateCacheListener(conf: CacheConfig): Unit = {
    if (conf == null)
      throw new KamanjaException("Got null CacheConfig", null)

    val sb = new scala.StringBuilder()
    if (conf.HostList == null)
      sb.append("Got null HostList in CacheConfig\n")

    var foundLocalNodeId = false

    conf.HostList.foreach(h => {
      if (h.NodeId == null || h.NodeIp == null || h.Port <= 0)
        sb.append("Got invalid HostConfig in CacheConfig. NodeId:" + h.NodeId + ", NodeIp:" + h.NodeIp + ", Port:" + h.Port)
      else if (!foundLocalNodeId && h.NodeId != null && _nodeId != null) {
        foundLocalNodeId = h.NodeId.equalsIgnoreCase(_nodeId)
        //        _cachePort = h.Port
      }
    })

    if (!foundLocalNodeId)
      sb.append("Not found local NodeId:" + _nodeId + " in CacheConfig HostList")

    if (conf.CacheStartPort <= 0)
      sb.append("Got invalid CacheStartPort (%d) in CacheConfig\n".format(conf.CacheStartPort))
    if (conf.CacheSizePerNode <= 0)
      sb.append("Got invalid CacheSizePerNode (%d) in CacheConfig\n".format(conf.CacheSizePerNode))
    if (conf.ReplicateFactor <= 0)
      sb.append("Got invalid ReplicateFactor (%d) in CacheConfig\n".format(conf.ReplicateFactor))
    if (conf.TimeToIdleSeconds <= 0)
      sb.append("Got invalid TimeToIdleSeconds (%d) in CacheConfig\n".format(conf.TimeToIdleSeconds))
    if ((!conf.EvictionPolicy.equalsIgnoreCase("LFU")) && (!conf.EvictionPolicy.equalsIgnoreCase("LRU")) && (!conf.EvictionPolicy.equalsIgnoreCase("FIFO")))
      sb.append("Got invalid EvictionPolicy (%s) in CacheConfig\n".format(conf.EvictionPolicy))

    if (sb.length > 0) {
      throw new KamanjaException(sb.toString(), null)
    }
  }

  class DummyCacheListenerCallback extends CacheCallback {
    @throws(classOf[Exception])
    override def call(callbackData: CacheCallbackData): Unit = {
      // Not doing anythin
      if (logger.isDebugEnabled && callbackData != null) {
        logger.debug("DummyCacheListenerCallback callback called with eventtype:" + callbackData.eventType + ", key:" + callbackData.key + ", value:" + callbackData.value)
      }
    }
  }

  class CacheListenerCallback extends CacheCallback {
    @throws(classOf[Exception])
    override def call(callbackData: CacheCallbackData): Unit = {
      val matchedListerners = getMatchedCacheListeners(callbackData.key)
      matchedListerners.foreach(l => {
        if (l.ListenCallback != null) {
          l.ListenCallback(callbackData.eventType, callbackData.key, callbackData.value)
        }
      })
    }
  }

  private def CreateCacheListener(conf: CacheConfig): Unit = {
    val hosts = conf.HostList.map(h => {
      "%s[%d]".format(h.NodeIp, conf.CacheStartPort)
    }).mkString(",")
    val cacheCfg = _cacheConfigTemplate.format("EnvCtxtListenersCache", hosts, conf.CacheStartPort, conf.CacheSizePerNode / 2, conf.TimeToIdleSeconds, conf.TimeToIdleSeconds, conf.EvictionPolicy.toUpperCase, "true")

    val listenCallback = new CacheListenerCallback()

    val aclass = Class.forName("com.ligadata.cache.MemoryDataCacheImp").newInstance
    _listenerCache = aclass.asInstanceOf[DataCache]
    _listenerCache.init(cacheCfg, listenCallback)
    _listenerCache.start()
  }

  private def CreateConfigClusterCache(conf: CacheConfig): Unit = {
    val hosts = conf.HostList.map(h => {
      "%s[%d]".format(h.NodeIp, conf.CacheStartPort)
    }).mkString(",")
    val cacheCfg = _cacheConfigTemplate.format("EnvCtxtConfigClusterCache", hosts, conf.CacheStartPort, conf.CacheSizePerNode / 2, conf.TimeToIdleSeconds, conf.TimeToIdleSeconds, conf.EvictionPolicy.toUpperCase, "true")

    val listenCallback = new DummyCacheListenerCallback()
    val aclass = Class.forName("com.ligadata.cache.MemoryDataCacheImp").newInstance
    _listenerConfigClusterCache = aclass.asInstanceOf[DataCache]
    _listenerConfigClusterCache.init(cacheCfg, listenCallback)
    _listenerConfigClusterCache.start()
  }

  override def setDataToZNode(zNodePath: String, value: Array[Byte]): Unit = {
    var tryAgain = true
    var retriesAttempted = 0
    var finalException: Throwable = null
    while (tryAgain && retriesAttempted <= 1) {
      tryAgain = false
      finalException = null
      try {
        setData2ZNode(zNodePath, value)
      } catch {
        case e: org.apache.zookeeper.KeeperException.NoNodeException => {
          finalException = e
          try {
            CreateClient.CreateNodeIfNotExists(_zkConnectString, zNodePath)
            tryAgain = true
            retriesAttempted += 1
          } catch {
            case e: Throwable => {
              finalException = e
            }
          }
        }
        case e: Throwable => {
          finalException = e
        }
      }
    }

    if (finalException != null)
      throw finalException
  }

  override def getDataFromZNode(zNodePath: String): Array[Byte] = {
    if (!hasZkConnectionString)
      throw new KamanjaException("Zookeeper information is not yet set", null)
    val MAX_ZK_RETRIES = 1
    var readVal: Array[Byte] = Array[Byte]()
    var finalException: Throwable = null
    ReadLock(_setZk_reent_lock)
    try {
      var retriesAttempted = 0
      var run = true

      while (run) {
        run = false
        finalException = null
        try {
          if (retriesAttempted > 0) {
            logger.warn("retrying zk call (getDataFromZNode)")
          }
          if (_setZkData != null) {
            readVal = _setZkData.getData().forPath(zNodePath)
          } else {
            throw new KamanjaException("Connection to ZK does not exists", null)
          }
        } catch {
          case e: Throwable => {
            finalException = e
            retriesAttempted += 1
            if (_setZkData != null) {
              logger.warn("Connection to Zookeeper is temporarily unavailable (getDataFromZNode).due to ", e)
            }
            ReconnectToSetDataZkc
            run = (retriesAttempted <= MAX_ZK_RETRIES)
          }
        }
      }
    } catch {
      case e: Throwable => {
        finalException = e
      }
    } finally {
      ReadUnlock(_setZk_reent_lock)
    }

    if (finalException != null) {
      throw finalException
    }

    return readVal
  }

  override def setZookeeperInfo(zkConnectString: String, zkBasePath: String, zkSessionTimeoutMs: Int, zkConnectionTimeoutMs: Int): Unit = {
    _zkConnectString = zkConnectString
    _zkBasePath = zkBasePath
    _zkleaderNodePath = zkBasePath + "/EnvCtxtLeader"
    _zkSessionTimeoutMs = zkSessionTimeoutMs
    _zkConnectionTimeoutMs = zkConnectionTimeoutMs
  }

  override def getZookeeperInfo(): (String, String, Int, Int) = (_zkConnectString, _zkBasePath, _zkSessionTimeoutMs, _zkConnectionTimeoutMs)

  override def createZkPathListener(znodePath: String, ListenCallback: (String) => Unit): Unit = {
    if (!hasZkConnectionString)
      throw new KamanjaException("Zookeeper information is not yet set", null)

    WriteLock(_zkListeners_reent_lock)
    try {
      logger.warn("Creating ZkPathListener _zkConnectString:%s, znodePath:%s, _zkSessionTimeoutMs:%d, _zkConnectionTimeoutMs:%d".format(_zkConnectString, znodePath, _zkSessionTimeoutMs, _zkConnectionTimeoutMs))
      val zkDataChangeNodeListener = new ZooKeeperListener
      zkDataChangeNodeListener.CreateListener(_zkConnectString, znodePath, ListenCallback, _zkSessionTimeoutMs, _zkConnectionTimeoutMs)
      _zkListeners += zkDataChangeNodeListener
    } catch {
      case e: Throwable => {
        throw e
      }
    } finally {
      WriteUnlock(_zkListeners_reent_lock)
    }
  }

  override def createZkPathChildrenCacheListener(znodePath: String, getAllChildsData: Boolean, ListenCallback: (String, String, Array[Byte], Array[(String, Array[Byte])]) => Unit): Unit = {
    if (!hasZkConnectionString)
      throw new KamanjaException("Zookeeper information is not yet set", null)

    WriteLock(_zkListeners_reent_lock)
    try {
      logger.warn("Creating ZkPathChildrenCacheListener _zkConnectString:%s, znodePath:%s, _zkSessionTimeoutMs:%d, _zkConnectionTimeoutMs:%d".format(_zkConnectString, znodePath, _zkSessionTimeoutMs, _zkConnectionTimeoutMs))
      val zkDataChangeNodeListener = new ZooKeeperListener
      zkDataChangeNodeListener.CreatePathChildrenCacheListener(_zkConnectString, znodePath, false, ListenCallback, _zkSessionTimeoutMs, _zkConnectionTimeoutMs)
      _zkListeners += zkDataChangeNodeListener
    } catch {
      case e: Throwable => {
        throw e
      }
    } finally {
      WriteUnlock(_zkListeners_reent_lock)
    }
  }

  // Cache Listeners
  override def setListenerCacheKey(key: String, value: String): Unit = {
    if (_listenerCache != null)
      _listenerCache.put(key, value)
    //    setDataToZNode(key: String, value.getBytes())
  }

  // listenPath is the Path where it has to listen. Ex: /kamanja/notification/node1
  // ListenCallback is the call back called when there is any change in listenPath. The return value is has 3 components. 1 st is eventType, 2 is eventPath and 3rd is eventPathData
  // eventType is PUT, UPDATE, REMOVE etc
  // eventPath is the Path where it changed the data
  // eventPathData is the data of that path
  override def createListenerForCacheKey(listenPath: String, ListenCallback: (String, String, String) => Unit): Unit = {
    addCacheListener(listenPath, ListenCallback, false)
    /*
        class Test(path: String, ListenCallback: (String, String, String) => Unit) {
          def Callback(data: String): Unit = {
            if (ListenCallback != null)
              ListenCallback("UPDATE", path, data)
          }
        }

        val tst = new Test(listenPath, ListenCallback)

        createZkPathListener(listenPath: String, tst.Callback)
    */
  }

  // listenPath is the Path where it has to listen and its children
  //    Ex: If we start watching /kamanja/nodification/ all the following puts/updates/removes/etc will notify callback
  //    /kamanja/nodification/node1/1 or /kamanja/nodification/node1/2 or /kamanja/nodification/node1 or /kamanja/nodification/node2 or /kamanja/nodification/node3 or /kamanja/nodification/node4
  // ListenCallback is the call back called when there is any change in listenPath and or its children. The return value is has 3 components. 1 st is eventType, 2 is eventPath and 3rd is eventPathData
  // eventType is PUT, UPDATE, REMOVE etc
  // eventPath is the Path where it changed the data
  // eventPathData is the data of that path
  // eventType: String, eventPath: String, eventPathData: Array[Byte]
  override def createListenerForCacheChildern(listenPath: String, ListenCallback: (String, String, String) => Unit): Unit = {
    addCacheListener(listenPath, ListenCallback, true)
    /*
        val sendOne = (eventType: String, eventPath: String, eventPathData: Array[Byte], childs: Array[(String, Array[Byte])]) => {
          ListenCallback(eventType, eventPath, if (eventPathData != null) new String(eventPathData) else null)
        }
        createZkPathChildrenCacheListener(listenPath, false, sendOne)
    */
  }

  override def getClusterInfo(): ClusterStatus = _clusterStatusInfo

  private def EnvCtxtEventChangeCallback(cs: ClusterStatus): Unit = {
    logger.warn("DistributionCheck:EnvCtxtEventChangeCallback. Entered from EnvCtxtEventChangeCallback. Got new ClusterStatus. NodeId:%s, LeaderNodeId:%s, Participents:%s, isLeader:%s".format(cs.nodeId,
      cs.leaderNodeId, if (cs.participantsNodeIds != null) cs.participantsNodeIds.mkString(",") else "", cs.isLeader.toString))
    WriteLock(_zkLeader_reent_lock)
    try {
      _clusterStatusInfo = cs
    } catch {
      case e: Throwable => {}
    } finally {
      WriteUnlock(_zkLeader_reent_lock)
    }

    ReadLock(_zkLeader_reent_lock)
    try {
      _zkLeaderListeners.foreach(fn => {
        try {
          if (fn != null && fn.EventChangeCallback != null) {
            logger.warn("DistributionCheck:EnvCtxtEventChangeCallback. Sending ClusterStatus to funcitons. NodeId:%s, LeaderNodeId:%s, Participents:%s, isLeader:%s".format(cs.nodeId,
              cs.leaderNodeId, if (cs.participantsNodeIds != null) cs.participantsNodeIds.mkString(",") else "", cs.isLeader.toString))
            fn.EventChangeCallback(cs)
          } else {
            logger.warn("DistributionCheck:EnvCtxtEventChangeCallback. Can not send ClusterStatus (function is null). NodeId:%s, LeaderNodeId:%s, Participents:%s, isLeader:%s".format(cs.nodeId,
              cs.leaderNodeId, if (cs.participantsNodeIds != null) cs.participantsNodeIds.mkString(",") else "", cs.isLeader.toString))
          }
        } catch {
          case e: Throwable => {
            logger.error("DistributionCheck:Failed to send ClusterStatus. NodeId:%s, LeaderNodeId:%s, Participents:%s, isLeader:%s".format(cs.nodeId,
              cs.leaderNodeId, if (cs.participantsNodeIds != null) cs.participantsNodeIds.mkString(",") else "", cs.isLeader.toString), e)
          }
        }
      })
    } catch {
      case e: Throwable => {}
    } finally {
      ReadUnlock(_zkLeader_reent_lock)
    }
    logger.warn("DistributionCheck:EnvCtxtEventChangeCallback. Exiting from EnvCtxtEventChangeCallback. NodeId:%s, LeaderNodeId:%s, Participents:%s, isLeader:%s".format(cs.nodeId,
      cs.leaderNodeId, if (cs.participantsNodeIds != null) cs.participantsNodeIds.mkString(",") else "", cs.isLeader.toString))
  }

  // This will give either any node change or leader change
  override def registerNodesChangeNotification(EventChangeCallback: (ClusterStatus) => Unit): Unit = {
    if (!hasZkConnectionString)
      throw new KamanjaException("Zookeeper information is not yet set", null)

    WriteLock(_zkLeader_reent_lock)
    try {
      if (_zkLeaderLatch == null) {
        logger.warn("DistributionCheck:registerNodesChangeNotification. Registering as new listener and starting leader")
        _zkLeaderLatch = new ZkLeaderLatch(_zkConnectString, _zkleaderNodePath, _nodeId, EnvCtxtEventChangeCallback, _zkSessionTimeoutMs, _zkConnectionTimeoutMs)
        _zkLeaderListeners += LeaderListenerCallback(EventChangeCallback)
        _zkLeaderLatch.SelectLeader
      } else {
        logger.warn("DistributionCheck:registerNodesChangeNotification. Registering as new listener with existing leader")
        _zkLeaderListeners += LeaderListenerCallback(EventChangeCallback)
        if (_clusterStatusInfo != null) {
          EventChangeCallback(_clusterStatusInfo)
          logger.warn("DistributionCheck:registerNodesChangeNotification. Sending existing ClusterStatus. NodeId:%s, LeaderNodeId:%s, Participents:%s, isLeader:%s".format(_clusterStatusInfo.nodeId,
            _clusterStatusInfo.leaderNodeId, if (_clusterStatusInfo.participantsNodeIds != null) _clusterStatusInfo.participantsNodeIds.mkString(",") else "", _clusterStatusInfo.isLeader.toString))
        }
      }
    } catch {
      case e: Throwable => {
        throw e
      }
    } finally {
      WriteUnlock(_zkLeader_reent_lock)
    }
  }

  //  def unregisterNodesChangeNotification(EventChangeCallback: (ClusterStatus) => Unit): Unit = {}

  override def getNodeId(): String = _nodeId

  override def getClusterId(): String = _clusterId

  override def setNodeInfo(nodeId: String, clusterId: String): Unit = {
    _nodeId = nodeId
    _clusterId = clusterId
  }

  // This post the message into where ever these messages are associated immediately
  // Later this will be posted to logical queue where it can execute on logical partition.
  override def postMessages(msgs: Array[ContainerInterface]): Unit = {
    if (_postMsgListenerCallback != null)
      _postMsgListenerCallback(msgs)
  }

  override def postMessagesListener(postMsgListenerCallback: (Array[ContainerInterface]) => Unit): Unit = {
    _postMsgListenerCallback = postMsgListenerCallback
  }

  override def getContainerInstance(containerName: String): ContainerInterface = {
    if (_objectResolver != null)
      _objectResolver.getInstance(containerName)
    else
      null
  }

  override def getPrimaryDatastoreForTenantId(tenantId: String): String = {
    val tntInfo = _mgr.GetTenantInfo(tenantId)
    if (tntInfo == null)
      throw new KamanjaException("Not found tenantId:%s in metadata".format(tenantId), null)
    tntInfo.primaryDataStore
  }

  override def setSystemCatalogDatastore(sysCatalog: String): Unit = {
    _sysCatalogDsString = sysCatalog
    if (_sysCatalogDsString != null)
      logger.debug("SystemCatalog Information" + _sysCatalogDsString)
    // Doing it only once
    if (_sysCatalogDatastore != null)
      _sysCatalogDatastore.Shutdown()
    _sysCatalogDatastore = null
    _sysCatalogDatastore = GetDataStoreHandle(_jarPaths, _sysCatalogDsString)
    if (_sysCatalogDatastore != null) {
      _sysCatalogDatastore.setObjectResolver(_objectResolver)
      _sysCatalogDatastore.setDefaultSerializerDeserializer("com.ligadata.kamanja.serializer.kbinaryserdeser", Map[String, Any]())
    }
  }

  override def getSystemCatalogDatastore(): String = _sysCatalogDsString

  override def startCache(conf: CacheConfig): Unit = {
    ValidateCacheListener(conf)
    CreateCacheListener(conf)
    CreateConfigClusterCache(conf)
    _cacheConfig = conf
  }

  override def getCacheConfig(): CacheConfig = _cacheConfig

  // Saving & getting temporary objects in cache
  override def saveConfigInClusterCache(key: String, value: Array[Byte]): Unit = {
    if (_listenerConfigClusterCache != null)
      _listenerConfigClusterCache.put(key, value)
  }

  override def getConfigFromClusterCache(key: String): Array[Byte] = {
    if (_listenerConfigClusterCache != null && _listenerConfigClusterCache.isKeyInCache(key)) {
      val v = _listenerConfigClusterCache.get(key)
      if (v != null)
        v.asInstanceOf[Array[Byte]]
      else
        null
    }
    else
      null
  }

  override def getAllKeysFromClusterCache(): Array[String] = {
    if (_listenerConfigClusterCache != null)
      _listenerConfigClusterCache.getKeys
    else
      Array[String]()
  }

  override def getAllConfigFromClusterCache(): Array[KeyValuePair] = {
    if (_listenerConfigClusterCache != null) {
      val allValues = _listenerConfigClusterCache.getAll
      if (allValues != null && allValues.size() > 0)
        allValues.asScala.map(kv => KeyValuePair(kv._1, if (kv._2 != null) kv._2.asInstanceOf[Array[Byte]] else null)).toArray
      else
        Array[KeyValuePair]()
    }
    else {
      Array[KeyValuePair]()
    }
  }
}
