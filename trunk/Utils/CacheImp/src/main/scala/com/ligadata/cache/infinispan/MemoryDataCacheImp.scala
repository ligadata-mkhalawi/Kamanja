package com.ligadata.cache.infinispan

import java.util
import java.util.concurrent.TimeUnit

import com.ligadata.cache._
import net.sf.ehcache.config.Configuration
import org.infinispan.configuration.cache.{CacheMode, ConfigurationBuilder}
import org.infinispan.manager.DefaultCacheManager
import org.infinispan.Cache
import org.infinispan.cache.impl.CacheImpl;

/**
  * Created by Saleh on 6/9/2016.
  */
class MemoryDataCacheImp extends DataCache {
  private var cacheManager: DefaultCacheManager = null
  private var config: CacheCustomConfigInfinispan = null
  private var cache: Cache[String, Any] = null
  private var listenCallback: CacheCallback = null

  final def getCache(): Cache[String, Any] = cache

  final def getCacheManager(): DefaultCacheManager = cacheManager

  final def getCacheConfig(): CacheCustomConfigInfinispan = config

  override def init(jsonString: String, listenCallback: CacheCallback): Unit = {
    config = new CacheCustomConfigInfinispan(new Config(jsonString), cacheManager)
    cacheManager = config.getDefaultCacheManager()
    this.listenCallback = listenCallback
  }

  override def start(): Unit = {
    cache = cacheManager.getCache(config.getcacheName());
    if (listenCallback != null) {
      cache.addListener(new EventCacheListener(listenCallback))
    }
  }

  override def shutdown(): Unit = {
    cacheManager.stop()
  }

  override def put(key: String, value: scala.Any): Unit = {
    cache.put(key, value)
  }

  override def get(key: String): AnyRef = {
    val obj = cache.get(key).asInstanceOf[AnyRef]
    return if (obj != null) obj else ""
  }

  override def remove(key: String): Unit = {
    cache.remove(key)
/*
    if (cache.containsKey(key)) {
      cache.remove(key)
    }
*/
  }

  override def clear(): Unit = {
    cache.clear()
  }

  override def isKeyInCache(key: String): Boolean = (cache != null && cache.containsKey(key))

  override def get(keys: Array[String]): java.util.Map[String, AnyRef] = {
    val map = new java.util.HashMap[String, AnyRef]
    keys.foreach(str => map.put(str, get(str)))

    map
  }

  override def put(map: java.util.Map[String, AnyRef]): Unit = {
    cache.putAll(map, -1, TimeUnit.HOURS)
  }

  override def getAll(): java.util.Map[String, AnyRef] = {
    val map = new java.util.HashMap[String, AnyRef]
    var it = cache.entrySet.iterator()
    while (it.hasNext) {
      val thisEntry = it.next()
      map.put(thisEntry.getKey, thisEntry.getValue().asInstanceOf[AnyRef])
    }
    map
  }

  override def getKeys(): Array[String] = {
    cache.keySet().toArray.map(k => k.asInstanceOf[String])
  }

  override def size: Int = {
    // This may include expried elements also
    cache.size
  }

  override def put(containerName: String, timestamp: String, key: String, value: scala.Any): Unit = {}

  override def get(containerName: String, map: java.util.Map[String, java.util.Map[String, AnyRef]]): Unit = {}

  override def get(containerName: String, timestamp: String): util.Map[String, AnyRef] = {
    null
  }

  override def get(containerName: String, timestamp: String, key: String): AnyRef = {
    null
  }

  override def del(containerName: String): Unit = {}

  override def del(containerName: String, timestamp: String): Unit = {}

  override def del(containerName: String, timestamp: String, key: String): Unit = {}

  override def put(containerName: String, key: String, value: scala.Any): Unit = {}

  override def getFromRoot(rootNode: String, key: String): java.util.Map[String, AnyRef] = {
    null
  }

  override def beginTransaction(): Transaction = {
    new MemoryDataCacheTxnImp(this)
  }
}

class MemoryDataCacheTxnImp(cache: DataCache) extends Transaction(cache) {
  var tm = cache.asInstanceOf[MemoryDataCacheImp].getCache().asInstanceOf[CacheImpl[String, Any]].getAdvancedCache.getTransactionManager
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
