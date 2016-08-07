/**
  * Created by Saleh on 6/12/2016.
  */

import java.util

import com.ligadata.cache.DataCache
import com.ligadata.cache.infinispan.EventCacheListener

/**
  * Created by Saleh on 6/12/2016.
  */
object InfiniNode2 {
  def main(args: Array[String]) {
    val aclass = Class.forName("com.ligadata.cache.infinispan.MemoryDataTreeCacheImp").newInstance
    val node = aclass.asInstanceOf[DataCache]

    node.init("""{"name":"CacheCluster","jgroups.tcpping.initial_hosts":"192.168.1.129[7800],192.168.1.129[7801],192.168.1.129[7802]","jgroups.port":"7801","numOfKeyOwners":"2","maxEntries":"300000","CacheConfig":{"timeToIdleSeconds":"300000","timeToLiveSeconds":"300000","peerconfig":"jgroups_tcp.xml"}}""",null)
    node.start()

    val test = node.get("1").asInstanceOf[Array[Byte]]
    test.foreach(k=>System.out.println(k.toChar))

    node.getKeys.foreach(k=>println( node.get(k).toString))

    println(node.getAll.entrySet().size())


    var a: Array[String] = new Array[String](1)
    a(0) = "1"
    a.foreach(k=>println(k))
    println(node.get(a).get("1").asInstanceOf[Array[Byte]])

    val map = new java.util.HashMap[String, java.util.Map[String, AnyRef]]
    node.get("test",map)
    println(map.get("/test/20160312").get("1").toString)

    println((node.get("test","20160311").get("1")).asInstanceOf[Array[Byte]].foreach(k=>System.out.print(k.toChar)))

//    node.del("test2")
//    node.del("test","20160311")
//    node.del("test","20160311","2")

    node.get("test","20160311","1").asInstanceOf[Array[Byte]].foreach(k=>System.out.print(k.toChar))
    println
    node.get("test","20160311","2").asInstanceOf[Array[Byte]].foreach(k=>System.out.print(k.toChar))
    println
    println(node.get("test","20160312","1").toString)

    node.get("test","20160312","2").asInstanceOf[Array[Byte]].foreach(k=>System.out.print(k.toChar))
    println(node.getFromRoot("test1","1").get("1").toString)
    println(node.getFromRoot("test2","2").get("1").toString)
  }
}