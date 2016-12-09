package com.ligadata.cache

import scala.util.control.Breaks._

object ListenerInstance {
  private val cacheConfigTemplate =
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
      |    "enableListener": "true"
      |  }
      |}
      |
      | """.stripMargin

  private type OptionMap = Map[Symbol, Any]

  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    def isSwitch(s: String) = (s(0) == '-')
    list match {
      case Nil => map
      case "--name" :: value :: tail =>
        nextOption(map ++ Map('name -> value), tail)
      case "--hosts" :: value :: tail =>
        nextOption(map ++ Map('hosts -> value), tail)
      case "--port" :: value :: tail =>
        nextOption(map ++ Map('port -> value), tail)
      case option :: tail => {
        println("Unknown option " + option)
        throw new Exception("Unknown option " + option)
      }
    }
  }

  def main(args: Array[String]) {
    val options = nextOption(Map(), args.toList)

    val name = options.getOrElse('name, "").toString.trim
    if (name.size == 0) {
      println("Need name parameter to initialize cache with that name")
      return
    }

    val hostnamesstr = options.getOrElse('hosts, "").toString.trim
    if (hostnamesstr.size == 0) {
      println("Need hosts parameter to initialize cache with that hosts. Ex: 10.20.10.20,10.20.10.30,10.20.10.40")
      return
    }

    val port = options.getOrElse('port, "0").toString.trim.toInt
    if (port < 0) {
      println("Expecting port > 0. Ex: 7800")
      return
    }

    val hosts = hostnamesstr.split(",").map(h => {
      "%s[%d]".format(h, port)
    }).mkString(",")
    
    val cacheCfg = cacheConfigTemplate.format(name, hosts, port)

    val aclass = Class.forName("com.ligadata.cache.MemoryDataCacheImp").newInstance
    val node = aclass.asInstanceOf[DataCache]

    node.init(cacheCfg, new CacheCallback {
        override def call(callbackData: CacheCallbackData): Unit = println("EventType:" + callbackData.eventType + ", Key:" + callbackData.key + ", Value:" + callbackData.value)
      })
    node.start()
    var counter = 0
    try {
      breakable {
        while (true) {
          counter += 1
          node.put(""+counter,System.currentTimeMillis())
          if(counter == 50 ){
            Thread.sleep(50000)
            counter = 0
          }
//          val strLine = readLine()
//          if (strLine != null) {
//            val cmdArgs = strLine.trim.split(",", -1).map(s => s.trim).filter(s => s.size > 0)
//            if (cmdArgs.size > 0) {
//              if (cmdArgs(0).equalsIgnoreCase("put")) {
//                if (cmdArgs.size > 2) {
//                  println("Executing Command:%s from input:%s".format(cmdArgs(0), strLine))
//                  node.put(cmdArgs(1), cmdArgs(2))
//                } else {
//                  println("Command:%s from input:%s need 3 arguments".format(cmdArgs(0), strLine))
//                }
  /*
              } else if (cmdArgs(0).equalsIgnoreCase("update")) {
                if (cmdArgs.size > 2) {
                  println("Executing Command:%s from input:%s".format(cmdArgs(0), strLine))
                  node.update(cmdArgs(1), cmdArgs(2))
                } else {
                  println("Command:%s from input:%s need 3 arguments".format(cmdArgs(0), strLine))
                }
              } else if (cmdArgs(0).equalsIgnoreCase("remove")) {
                if (cmdArgs.size > 1) {
                  println("Executing Command:%s from input:%s".format(cmdArgs(0), strLine))
                  node.remove(cmdArgs(1))
                } else {
                  println("Command:%s from input:%s need 2 arguments".format(cmdArgs(0), strLine))
                }
*/
//              } else if (cmdArgs(0).equalsIgnoreCase("get")) {
//                if (cmdArgs.size > 1) {
//                  println("Executing Command:%s from input:%s".format(cmdArgs(0), strLine))
//                  println("Result:" + node.get(cmdArgs(1)))
//                } else {
//                  println("Command:%s from input:%s need 2 arguments".format(cmdArgs(0), strLine))
//                }
//              } else {
//                println("Unknown command:%s from input:%s".format(cmdArgs(0), strLine))
//              }
//            }
//          }
        }
      }
    } catch {
      case e: Exception => {
        println("", e)
      }
    }
  }
}
