/**
  * Created by Saleh on 6/12/2016.
  */
import com.ligadata.cache.DataCache
import com.ligadata.cache.infinispan.EventCacheListener

/**
  * Created by Saleh on 6/12/2016.
  */
object InfiniNode2 {
  def main(args: Array[String]) {
    val aclass = Class.forName("com.ligadata.cache.infinispan.MemoryDataCacheImp").newInstance
    val node = aclass.asInstanceOf[DataCache]

    node.init("""{"name":"CacheCluster","jgroups.tcpping.initial_hosts":"192.168.1.122[7800],192.168.1.122[7801],192.168.1.122[7802]","jgroups.port":"7801","numOfKeyOwners":"2","CacheConfig":{"timeToIdleSeconds":"300000","timeToLiveSeconds":"300000","peerconfig":"jgroups_tcp.xml"}}""", new EventCacheListener)
    node.start()

    val test = node.get("1").asInstanceOf[Array[Byte]]
    test.foreach(k=>System.out.println(k.toChar))

    node.getKeys.foreach(k=>println( node.get(k).toString))

    println(node.getAll.entrySet().size())


    var a: Array[String] = new Array[String](1)
    a(0) = "1"
    a.foreach(k=>println(k))
    println(node.get(a).get("1").asInstanceOf[Array[Byte]])
  }
}