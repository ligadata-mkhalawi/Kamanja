package com.ligadata.cache


import java.util

import net.sf.ehcache.bootstrap.{BootstrapCacheLoader, BootstrapCacheLoaderFactory}
import net.sf.ehcache.distribution.jgroups.{JGroupsBootstrapCacheLoaderFactory, JGroupsCacheReplicatorFactory}
import net.sf.ehcache.event.CacheEventListener
import net.sf.ehcache.{Element, Cache, CacheManager}
import collection.JavaConversions._


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

    return ele.getObjectValue
  }

  override def isKeyInCache(key: String): Boolean = (cache != null && cache.isKeyInCache(key))

  override def get(keys: Array[String]): java.util.Map[String, AnyRef] = {
    val keysColl: java.util.Collection[_] = keys.toSeq
    var map = new java.util.HashMap[String, AnyRef]
    cache.getAll(keysColl).foreach(value => map.put(value._1.toString,value._2.getObjectValue))

    map
  }

  override def put(map: java.util.Map[_, _]): Unit = {
    val scalaMap = map.asScala
    val list = scalaMap.map(keyVal => new Element(keyVal._1, keyVal._2))
    cache.putAll(list.asJavaCollection)
  }

  override def getAll(): java.util.Map[String, AnyRef] = {
    var map = new java.util.HashMap[String, AnyRef]
    cache.getAll(cache.getKeys()).foreach(value => map.put(value._1.toString,value._2.getObjectValue))
    map
  }

  override def getKeys(): Array[String] = {
    cache.getKeys().asScala.map(k => k.toString).toArray
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
}
