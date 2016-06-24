package com.ligadata.cache.infinispan

import java.util

import com.ligadata.cache.{Config, CacheCustomConfig, CacheCallback, DataCache}
import net.sf.ehcache.config.Configuration
import org.infinispan.configuration.cache.{CacheMode, ConfigurationBuilder}
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.Cache;

/**
  * Created by Saleh on 6/9/2016.
  */
class MemoryDataCacheImp extends DataCache {

  var cacheManager: DefaultCacheManager = null
  var config: CacheCustomConfig = null
  var cache: Cache[String, Any] = null
  var listenCallback: CacheCallback = null

  override def init(jsonString: String, listenCallback: CacheCallback): Unit = {
    config = new CacheCustomConfig(new Config(jsonString), cacheManager)
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
    if (cache.containsKey(key)) {

      val obj = cache.get(key).asInstanceOf[AnyRef]

      return obj
    } else {
      return ""
    }
  }

  override def isKeyInCache(key: String): Boolean = (cache != null && cache.containsKey(key))

  override def get(keys: Array[String]): java.util.Map[String, AnyRef] = {
    val map = new java.util.HashMap[String, AnyRef]
    keys.foreach(str => map.put(str, get(str)))

    map
  }

  override def put(map: java.util.Map[_, _]): Unit = {
    val map = new java.util.HashMap[String, AnyRef]
    val keys = getKeys()
    if (keys != null) {
      keys.foreach(str => map.put(str, get(str)))
    }
    map
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
    val size: Int = cache.keySet().size()
    val array = new Array[String](size)
    cache.keySet().toArray[String](array)

    array
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
