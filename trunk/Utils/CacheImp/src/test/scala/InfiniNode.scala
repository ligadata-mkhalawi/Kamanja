import com.ligadata.cache.{CacheCallbackData, CacheCallback, DataCache}
import com.ligadata.cache.infinispan.EventCacheListener

/**
  * Created by Saleh on 6/12/2016.
  */
object InfiniNode {
  def main(args: Array[String]) {
    val aclass = Class.forName("com.ligadata.cache.infinispan.MemoryDataCacheImp").newInstance
    val node = aclass.asInstanceOf[DataCache]

//    node.init(
//      """{"name":"CacheCluster","jgroups.tcpping.initial_hosts":"192.168.1.2[7800],192.168.1.2[7801],192.168.1.2[7802]","jgroups.port":"7800","numOfKeyOwners":"2","maxEntries":"300000","CacheConfig":{"timeToIdleSeconds":"300000","timeToLiveSeconds":"300000","peerconfig":"jgroups_tcp.xml"}}""", new CacheCallback {
//        override def call(callbackData: CacheCallbackData): Unit = println(callbackData.key + ">>>" + callbackData.eventType + ">>>>>" + callbackData.value)
//      })
    node.init("""{"name":"CacheCluster","jgroups.tcpping.initial_hosts":"192.168.1.129[7800],192.168.1.129[7801],192.168.1.129[7802]","jgroups.port":"7800","numOfKeyOwners":"2","maxEntries":"300000","CacheConfig":{"timeToIdleSeconds":"300000","timeToLiveSeconds":"300000","peerconfig":"jgroups_tcp.xml"}}""", null)
    node.start()

    node.put("1", "HI ALL".getBytes)
    node.put("2", "2")
    node.put("test1", "1", "1")
    node.put("test2", "1", "2")
    node.put("test", "20160311", "1", "1".getBytes)
    node.put("test", "20160311", "2", "2".getBytes)
    node.put("test", "20160312", "1", "1test_20160312")
    node.put("test", "20160312", "1", "xxxx")
    node.put("test", "20160312", "3", "2222xxxx")
    node.put("test", "20160312", "2", "1111xxxx")
    node.put("test", "20160312", "2", "1test_20160312")
    node.put("test", "20160312", "3", "1test_20160312")
    node.put("test", "20160312", "2", "2test_20160312".getBytes)

    node.put("test2", "20160312", "1", "test1".getBytes)
    node.put("test2", "20160312", "2", "test2".getBytes)


  }
}
