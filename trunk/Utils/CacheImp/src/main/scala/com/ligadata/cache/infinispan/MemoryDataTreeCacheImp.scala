package com.ligadata.cache.infinispan

import java.util

import com.ligadata.cache._
import net.sf.ehcache.config.Configuration
import org.infinispan.configuration.cache.{CacheMode, ConfigurationBuilder}
import org.infinispan.manager.DefaultCacheManager
import org.infinispan.Cache
import org.infinispan.cache.impl.CacheImpl
import org.infinispan.tree.{Fqn, Node, TreeCache, TreeCacheFactory}

/**
  * Created by Saleh on 6/9/2016.
  */
class MemoryDataTreeCacheImp extends DataCache {

  var cacheManager: DefaultCacheManager = null
  var config: CacheCustomConfigInfinispan = null
  var cache: Cache[String, Any] = null
  var listenCallback: CacheCallback = null
  var treeCache: TreeCache[String, Any] = null
  val root: Fqn = Fqn.fromString("/")

  final def getCache(): Cache[String, Any] = cache

  final def getCacheManager(): DefaultCacheManager = cacheManager

  final def getCacheConfig(): CacheCustomConfigInfinispan = config

  override def init(jsonString: String, listenCallback: CacheCallback): Unit = {
    config = new CacheCustomConfigInfinispan(new Config(jsonString))
    cacheManager = config.defineConfiguration()
    this.listenCallback = listenCallback
  }

  override def start(): Unit = {
    cache = cacheManager.getCache(config.getcacheName());
    if (listenCallback != null) {
      cache.addListener(new EventCacheListener(listenCallback))
    }
    treeCache = new TreeCacheFactory().createTreeCache(cache);
  }

  override def shutdown(): Unit = {
    cacheManager.stop()
  }

  override def put(key: String, value: scala.Any): Unit = {
    if (!key.equals(null) && !"".equals(key)) {
      treeCache.put(root, key, value)
    }
  }

  override def get(key: String): AnyRef = {
    if (treeCache.getKeys(root).contains(key)) {

      val obj = treeCache.get(root, key).asInstanceOf[AnyRef]

      return obj
    } else {
      return null
    }
  }

  override def remove(key: String): Unit = {
    treeCache.remove(root, key)
  }

  override def clear(): Unit = {
    cache.clear()
  }

  override def getFromRoot(rootNode: String, key: String): java.util.Map[String, AnyRef] = {
    val map = new java.util.HashMap[String, AnyRef]
    val fqn: Fqn = Fqn.fromString(rootNode)
    val keys: util.Iterator[String] = treeCache.getKeys(fqn).iterator()
    if (treeCache.getKeys(fqn).size() != 0) {
      while (keys.hasNext) {
        val key: String = keys.next()
        map.put(key, treeCache.get(fqn, key).asInstanceOf[AnyRef])
      }
    }
    return map
  }

  override def isKeyInCache(key: String): Boolean = (treeCache != null && treeCache.getKeys(root).contains(key))

  override def get(keys: Array[String]): java.util.Map[String, AnyRef] = {
    val map = new java.util.HashMap[String, AnyRef]
    keys.foreach(str => map.put(str, get(str)))

    map
  }

  override def put(map: java.util.Map[String, AnyRef]): Unit = {
    val map = new java.util.HashMap[String, AnyRef]
    val keys = getKeys()
    if (keys != null) {
      keys.foreach(str => map.put(str, get(str)))
    }
  }

  override def getAll(): java.util.Map[String, AnyRef] = {
    val map = new java.util.HashMap[String, AnyRef]
    val keys = getKeys()
    if (keys != null) {
      keys.foreach(str => map.put(str, get(str)))
    }
    map
  }

  override def getKeys(): Array[String] = {
    val size: Int = treeCache.getKeys(root).size()
    val array = new Array[String](size)
    treeCache.getKeys(root).toArray[String](array)

    array
  }

  override def size: Int = {
    // This may include expried elements also
    cache.size
  }
  override def put(containerName: String, timestamp: String, key: String, value: scala.Any): Unit = {
    if (!containerName.equals(null) && !"".equals(containerName)) {
      if (!timestamp.equals(null) && !"".equals(timestamp)) {
        if (!key.equals(null) && !"".equals(key)) {
          val fqn: Fqn = Fqn.fromElements(containerName, timestamp);
          treeCache.put(fqn, key, value);
        }

      }
    }
  }

  override def put(containerName: String, key: String, value: scala.Any): Unit = {
    if (!containerName.equals(null) && !"".equals(containerName)) {
      if (!key.equals(null) && !"".equals(key)) {
        val fqn: Fqn = Fqn.fromElements(containerName);
        treeCache.put(fqn, key, value);
      }
    }
  }

  override def get(containerName: String, map: java.util.Map[String, java.util.Map[String, AnyRef]]): Unit = {
    if (!containerName.equals(null) && !"".equals(containerName)) {
      val containerNameCheck: Fqn = Fqn.fromElements(containerName)
      if (treeCache.exists(containerNameCheck)) {
        val allTimeStamp: Node[String, Any] = treeCache.getRoot().getChild(containerNameCheck);
        val nodes = allTimeStamp.getChildren.iterator()
        while (nodes.hasNext) {
          val timeStamp: Node[String, Any] = nodes.next()
          map.put(timeStamp.getFqn.toString, timeStamp.getData().asInstanceOf[java.util.Map[String, AnyRef]])
        }
      }
    }
  }

  override def get(containerName: String, timestamp: String): util.Map[String, AnyRef] = {
    if (!containerName.equals(null) && !"".equals(containerName)) {
      if (!timestamp.equals(null) && !"".equals(timestamp)) {
        val timestampCheck: Fqn = Fqn.fromElements(containerName, timestamp);
        if (treeCache.exists(timestampCheck)) {
          return treeCache.getData(timestampCheck).asInstanceOf[util.Map[String, AnyRef]]
        }
      }
    }
    null
  }

  override def get(containerName: String, timestamp: String, key: String): AnyRef = {
    if (!containerName.equals(null) && !"".equals(containerName)) {
      if (!timestamp.equals(null) && !"".equals(timestamp)) {
        if (!key.equals(null) && !"".equals(key)) {
          val keyCheck: Fqn = Fqn.fromElements(containerName, timestamp);
          if (treeCache.exists(keyCheck)) {
            return treeCache.get(keyCheck, key).asInstanceOf[AnyRef];
          }
        }
      }
    }
    null
  }

  override def del(containerName: String): Unit = {
    if (!containerName.equals(null) && !"".equals(containerName)) {
      val fqn: Fqn = Fqn.fromElements(containerName);
      treeCache.removeNode(fqn)
    }
  }

  override def del(containerName: String, timestamp: String): Unit = {
    if (!containerName.equals(null) && !"".equals(containerName)) {
      if (!timestamp.equals(null) && !"".equals(timestamp)) {
        val fqn: Fqn = Fqn.fromElements(containerName, timestamp);
        treeCache.removeNode(fqn)
      }
    }
  }

  override def del(containerName: String, timestamp: String, key: String): Unit = {
    if (!containerName.equals(null) && !"".equals(containerName)) {
      if (!timestamp.equals(null) && !"".equals(timestamp)) {
        if (!key.equals(null) && !"".equals(key)) {
          val fqn: Fqn = Fqn.fromElements(containerName, timestamp);
          treeCache.remove(fqn, key)
        }

      }
    }
  }

  override def beginTransaction(): Transaction = {
    new MemoryDataTreeCacheTxnImp(this)
  }
}

class MemoryDataTreeCacheTxnImp(cache: DataCache) extends Transaction(cache) {
  var tm = cache.asInstanceOf[MemoryDataTreeCacheImp].getCache().asInstanceOf[CacheImpl[String, Any]].getAdvancedCache.getTransactionManager
  tm.begin()

  private def CheckForValidTxn: Unit = {
    if (getDataCache == null)
      throw new Exception("Not found valid DataCache in transaction")
  }

  // Exceptions are thrown to caller
  @throws(classOf[Exception])
  @throws(classOf[Throwable])
  override def commit(): Unit = {
    CheckForValidTxn
    tm.commit()
    resetDataCache
    tm = null
  }

  // Exceptions are thrown to caller
  @throws(classOf[Exception])
  @throws(classOf[Throwable])
  override def rollback(): Unit = {
    CheckForValidTxn
    tm.rollback()
    resetDataCache
    tm = null
  }
}
