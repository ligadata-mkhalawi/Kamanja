package com.ligadata.throttler

import com.ligadata.cache.DataCache
import com.ligadata.cache.{ CacheCallbackData, CacheCallback, DataCache }

class ThrottleController(finalhostsstr: String, port: String, enableListener: Boolean) {

  var Node: DataCache = null;
  private val eh_cache_ConfigTemplate =
    """
      |
      |{
      |  "name": "%s",
      |  "diskSpoolBufferSizeMB": "20",
      |  "jgroups.tcpping.initial_hosts": "%s[%d]",
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

  def Init() = {
    val cacheClass = "com.ligadata.cache.MemoryDataCacheImp";
    println(this.finalhostsstr + " " + this.port + "  " + this.enableListener)
    val cacheCfg = this.eh_cache_ConfigTemplate.format("name", this.finalhostsstr, this.port.toInt, this.port.toInt, this.enableListener);
    println("cacheCfg  " + cacheCfg)
    val aclass = Class.forName(cacheClass).newInstance;
    this.Node = aclass.asInstanceOf[DataCache];
    this.Node.init(cacheCfg, new CacheCallback {
      override def call(callbackData: CacheCallbackData): Unit = {} //println("EventType:" + callbackData.eventType + ", Key:" + callbackData.key + ", Value:" + callbackData.value)
    })
    this.Node.start()
  }

  def AddKey(key: String, value: Any) = {
    if (this.Node == null) throw new Exception("Start the Cache by calling Init()");
    if (key == null || key.trim() == "") throw new Exception("Please provide a proper Key");
    this.Node.put(key, value)
  }

  def RemoveKey(key: String) = {
    if (this.Node == null) throw new Exception("Start the Cache by calling Init()");
    if (key == null || key.trim() == "") throw new Exception("Please provide a proper Key");
    if (!this.Node.isKeyInCache(key)) throw new Exception("Key does not exists in Cache");
   this.Node.del(key);
  }

  def Size(): Int = {
    if (this.Node == null) throw new Exception("Start the Cache by calling Init()");
    return this.Node.getKeys.size;
  }
}




