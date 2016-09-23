package com.ligadata.throttler

import com.ligadata.cache.DataCache
import com.ligadata.cache.{CacheCallbackData, CacheCallback, DataCache}

class ThrottleControllerCache(finalhostsstr: String, port: Int, enableListener: Boolean) {
  private var Node: DataCache = null;
  private val eh_cache_ConfigTemplate =
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
      |    "maxBytesLocalHeap": "20971520",
      |    "eternal": "false",
      |    "bootstrapAsynchronously": "false",
      |    "timeToIdleSeconds": "3000",
      |    "timeToLiveSeconds": "3000",
      |    "memoryStoreEvictionPolicy": "LFU",
      |    "transactionalMode": "off",
      |    "class": "net.sf.ehcache.distribution.jgroups.JGroupsCacheManagerPeerProviderFactory",
      |    "separator": "::",
      |    "peerconfig": "channelName=EH_CACHE::file=jgroups_tcp.xml",
      |    "enableListener": "%s"
      |  }
      |}
      |
      | """.stripMargin

  def Init(): Unit = {
    val cacheClass = "com.ligadata.cache.MemoryDataCacheImp";
    println(this.finalhostsstr + " " + this.port + "  " + this.enableListener)
    val cacheCfg = this.eh_cache_ConfigTemplate.format("NodeThrottleControllerCache", this.finalhostsstr, this.port, this.enableListener);
    println("cacheCfg  " + cacheCfg)
    val aclass = Class.forName(cacheClass).newInstance;
    this.Node = aclass.asInstanceOf[DataCache];
    this.Node.init(cacheCfg, null)
    this.Node.start()
  }

  def put(key: String, value: Any): Unit = {
    if (this.Node == null) throw new Exception("Start the Cache by calling Init()");
    if (key == null || key.trim() == "") throw new Exception("Please provide a proper Key");
    this.Node.put(key, value)
  }

  def put(map: java.util.HashMap[String, AnyRef]): Unit = {
    if (this.Node == null) throw new Exception("Start the Cache by calling Init()");
    if (map.size() > 0)
      this.Node.put(map)
  }

  def remove(key: String): Unit = {
    if (this.Node == null) throw new Exception("Start the Cache by calling Init()");
    if (key == null || key.trim() == "") throw new Exception("Please provide a proper Key");
    this.Node.remove(key);
  }

  def get(key: String): AnyRef = {
    if (this.Node == null) throw new Exception("Start the Cache by calling Init()");
    return this.Node.get(key);
  }

  def getKeys(): Array[String] = {
    if (this.Node == null) throw new Exception("Start the Cache by calling Init()");
    return this.Node.getKeys;
  }

  def getAll(): java.util.Map[String, AnyRef] = {
    if (this.Node == null) throw new Exception("Start the Cache by calling Init()");
    return this.Node.getAll;
  }

  def size(): Int = {
    if (this.Node == null) throw new Exception("Start the Cache by calling Init()");
    return this.Node.size;
  }

  def clear(): Unit = {
    if (this.Node == null) throw new Exception("Start the Cache by calling Init()");
    return this.Node.clear();
  }
}




