import com.ligadata.cache.{CacheCallbackData, CacheCallback, DataCache}
import com.ligadata.cache.infinispan.EventCacheListener

/**
  * Created by Saleh on 6/12/2016.
  */
object InfiniNodeWithoutTreeCache {
  def main(args: Array[String]) {
    val aclass = Class.forName("com.ligadata.cache.infinispan.MemoryDataWithoutTreeCacheImp").newInstance
    val node = aclass.asInstanceOf[DataCache]

    node.init(
      """{"name":"WithoutTreeCacheCluster","jgroups.tcpping.initial_hosts":"192.168.1.129[7802]","jgroups.port":"7802","numOfKeyOwners":"2","maxEntries":"300000","CacheConfig":{"timeToIdleSeconds":"300000","timeToLiveSeconds":"300000","peerconfig":"jgroups_tcp.xml"}}""", new CacheCallback {
        override def call(callbackData: CacheCallbackData): Unit = println(callbackData.key + ">>>" + callbackData.eventType + ">>>>>" + callbackData.value)
      })

    node.start()

    node.put("1", "HI ALL")
    node.put("2", "2")
    node.put("3", "3")
    node.put("4", "4")
    node.put("1", "1")
    node.put("2", "12")

  }
}
