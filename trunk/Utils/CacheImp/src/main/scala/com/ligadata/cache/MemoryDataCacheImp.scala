package com.ligadata.cache

import java.util

import net.sf.ehcache.bootstrap.{ BootstrapCacheLoader, BootstrapCacheLoaderFactory }
import net.sf.ehcache.distribution.jgroups.{ JGroupsBootstrapCacheLoaderFactory, JGroupsCacheReplicatorFactory }
import net.sf.ehcache.event.CacheEventListener
import net.sf.ehcache.{ Element, Cache, CacheManager }

import scala.collection.JavaConverters._

/**
 * Created by Saleh on 3/15/2016.
 */

class MemoryDataCacheImp extends DataCache {

  var cm: CacheManager = null
  var cache: Cache = null
  var cacheConfig: CacheCustomConfig = null

  override def init(jsonString: String, listenCallback: CacheCallback): Unit = {
    cacheConfig = new CacheCustomConfig(new Config(jsonString), listenCallback)
    println("cacheConfig.getConfiguration() " + cacheConfig.getConfiguration())
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
    if (cache.isKeyInCache(key)) {

      val ele: Element = cache.get(key)
      //val obj:Object = ele.getValue

      return ele.getObjectValue
    } else {
      //System.out.println("get data from SSD");
      cache.load(key)

      return ""
    }
  }

  override def isKeyInCache(key: String): Boolean = (cache != null && cache.isKeyInCache(key))

  override def get(keys: Array[String]): java.util.Map[String, AnyRef] = {
    val map = new java.util.HashMap[String, AnyRef]
    keys.foreach(str => map.put(str, get(str)))

    map
  }

  override def put(map: java.util.Map[_, _]): Unit = {
    val scalaMap = map.asScala
    scalaMap.foreach { keyVal => cache.put(new Element(keyVal._1, keyVal._2)) }
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
    cache.getKeys().asScala.map(k => k.toString).toArray
  }

  override def put(containerName: String, timestamp: String, key: String, value: scala.Any): Unit = {}

  override def get(containerName: String, map: java.util.Map[String, java.util.Map[String, AnyRef]]): Unit = {}

  override def get(containerName: String, timestamp: String): java.util.Map[String, AnyRef] = {
    null
  }

  override def get(containerName: String, timestamp: String, key: String): AnyRef = {
    null
  }

  override def del(key: String): Unit = {
    if (cache.isKeyInCache(key)) {
      cache.remove(key)
    }
  }

  override def remove(key: String): Unit = {
    if (cache.isKeyInCache(key)) {
      println("key exixst in cache" + key)
      cache.remove(key)
    }
  }

  override def del(containerName: String, timestamp: String): Unit = {}

  override def del(containerName: String, timestamp: String, key: String): Unit = {}

  override def put(containerName: String, key: String, value: scala.Any): Unit = {}

  override def getFromRoot(rootNode: String, key: String): java.util.Map[String, AnyRef] = {
    null
  }

  override def beginTransaction(): Transaction = {
    throw new NotImplementedError("beginTransaction is not yet implemented")
    return null;
  }

  override def beginTx(): Transaction = {
    throw new NotImplementedError("beginTx is not yet implemented")
    return null;
  }

  override def endTx(tx: Transaction): Unit = {
    throw new NotImplementedError("endTx is not yet implemented")
  }

  override def commitTx(tx: Transaction): Unit = {
    throw new NotImplementedError("commitTx is not yet implemented")
  }

  override def rollbackTx(tx: Transaction): Unit = {
    throw new NotImplementedError("rollbackTx is not yet implemented")
  }
}
