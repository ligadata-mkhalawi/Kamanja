package com.ligadata.cache


import java.util

import net.sf.ehcache.bootstrap.{BootstrapCacheLoader, BootstrapCacheLoaderFactory}
import net.sf.ehcache.distribution.jgroups.{JGroupsBootstrapCacheLoaderFactory, JGroupsCacheReplicatorFactory}
import net.sf.ehcache.event.CacheEventListener
import net.sf.ehcache.{Element, Cache, CacheManager}
import collection.JavaConverters._

import scala.collection.JavaConverters._

/**
  * Created by Saleh on 3/15/2016.
  */

class MemoryDataCacheImp extends DataCache {
  private var cm: CacheManager = null
  private var cache: Cache = null
  private var cacheConfig: CacheCustomConfig = null

  final def getCache(): Cache = cache

  final def getCacheManager(): CacheManager = cm

  final def getCacheConfig(): CacheCustomConfig = cacheConfig

  override def init(jsonString: String, listenCallback: CacheCallback): Unit = {
    cacheConfig = new CacheCustomConfig(new Config(jsonString), listenCallback)
    cm = CacheManager.create(cacheConfig.getConfiguration())
    val cache = new Cache(cacheConfig)
    cache.setBootstrapCacheLoader(cacheConfig.getBootStrap())
    cm.addCache(cache)
  }

  override def start(): Unit = {
    cache = cm.getCache(cacheConfig.getName)
    cacheConfig.addListeners(cache)
  }

  override def shutdown(): Unit = {
    cm.shutdown()
  }

  override def put(key: String, value: scala.Any): Unit = {
    cache.put(new Element(key, value))
  }

  override def get(key: String): AnyRef = {
    val ele: Element = cache.get(key)
    if (ele != null) ele.getObjectValue else ""
  }

  override def remove(key: String): Unit = {
    cache.remove(key)
  }

  override def clear(): Unit = {
    cache.removeAll()
  }

  override def isKeyInCache(key: String): Boolean = (cache != null && cache.isKeyInCache(key))

  override def get(keys: Array[String]): java.util.Map[String, AnyRef] = {
    val allVals = cache.getAll(keys.toList.asJava)
    val map = new java.util.HashMap[String, AnyRef]
    allVals.asScala.foreach(kv => {
      map.put(kv._2.getObjectKey.asInstanceOf[String], kv._2.getObjectValue)
    })
    map
  }

  override def put(map: java.util.Map[String, AnyRef]): Unit = {
    val scalaValues = map.asScala.map(keyVal => (new Element(keyVal._1, keyVal._2))).toList
    cache.putAll(scalaValues.asJava)
  }

  override def getAll(): java.util.Map[String, AnyRef] = {
    get(getKeys())
  }

  override def getKeys(): Array[String] = {
    cache.getKeys().asScala.map(k => k.toString).toArray
  }

  override def size: Int = {
    // This may include expried elements also
    cache.getSize
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
  private def CheckForValidTxn: Unit = {
    if (getDataCache == null)
      throw new Exception("Not found valid DataCache in transaction")
  }

  override def commit(): Unit = {
    CheckForValidTxn

    throw new NotImplementedError("commit is not yet implemented")
  }

  override def rollback(): Unit = {
    CheckForValidTxn

    throw new NotImplementedError("rollback is not yet implemented")

    resetDataCache
  }
}
