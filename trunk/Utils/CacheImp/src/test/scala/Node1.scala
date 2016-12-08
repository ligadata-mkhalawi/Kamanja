import com.ligadata.cache.{CacheCallback, CacheCallbackData, DataCache}

import collection.JavaConversions._

/**
  * Created by Saleh on 3/23/2016.
  */


object Node1 {
  def main(args: Array[String]) {


    val aclass = Class.forName("com.ligadata.cache.MemoryDataCacheImp").newInstance
    val node = aclass.asInstanceOf[DataCache]

    node.init("""{"name":"CacheCluster","diskSpoolBufferSizeMB":"20","jgroups.tcpping.initial_hosts":"192.168.1.3[7800],192.168.1.3[7800]","jgroups.port":"7800","replicatePuts":"true","replicateUpdates":"true","replicateUpdatesViaCopy":"true","replicateRemovals":"true","replicateAsynchronously":"true","CacheConfig":{"maxBytesLocalHeap":"20971520","eternal":"false","bootstrapAsynchronously":"false","timeToIdleSeconds":"3000","timeToLiveSeconds":"3000","memoryStoreEvictionPolicy":"LFU","transactionalMode":"off","class":"net.sf.ehcache.distribution.jgroups.JGroupsCacheManagerPeerProviderFactory","separator":"::","peerconfig":"channelName=EH_CACHE::file=jgroups_tcp.xml","enableListener":"true"}}"""
      , new CacheCallback {
        override def call(callbackData: CacheCallbackData): Unit = {println(callbackData.eventType); println("update data")}
      })
    node.start()

    node.put("1","HI ALL".getBytes)

    val map = Map("2" -> "totot")
    node.put(map)

  }
}
