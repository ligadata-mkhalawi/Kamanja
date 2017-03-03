
/*
 * Copyright 2016 ligaDATA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ligadata.KamanjaManager

import org.apache.logging.log4j.{LogManager, Logger}
import akka.actor._
import com.typesafe.config.ConfigFactory

trait KamanjaManagerAkkaWorklet

object AkkaActorSystem {
  private val LOG = LogManager.getLogger(getClass)
  private val actorySystems = scala.collection.mutable.Map[String, ActorSystem]()
  private val akkaConfigFormat =
    """
      |
      |akka
      |{
      |    log-dead-letters = 0
      |    log-dead-letters-during-shutdown = off
      |
      |    loglevel = "INFO"
      |    actor
      |    {
      |        provider = "akka.remote.RemoteActorRefProvider"
      |        serialize-messages = on
      |
      |        serializers
      |        {
      |            java = "akka.serialization.JavaSerializer"
      |            proto = "akka.remote.serialization.ProtobufSerializer"
      |        }
      |
      |        serialization-bindings
      |        {
      |            "com.ligadata.KamanjaManager.LogicalPartitionLocalExec" = java
      |            "com.ligadata.KamanjaManager.LogicalPartitionRemoteExec" = java
      |        }
      |
      |    }
      |
      |    remote
      |    {
      |        enabled-transports = ["akka.remote.netty.tcp"]
      |        netty.tcp
      |        {
      |            hostname = "%s"
      |            port = %d
      |        }
      |
      |        log-sent-messages = on
      |        log-received-messages = on
      |    }
      |}
      |
      |LocalSystem
      |{
      |    include "common"
      |    akka
      |    {
      |        log-dead-letters = 0
      |        log-dead-letters-during-shutdown = off
      |    }
      |}
      |
    """.stripMargin

  private val root = ConfigFactory.load()

  def getActorSystem(host: String, port: Int): ActorSystem = synchronized {
    val searchKey = host.trim + "," + port
    val tmpActorSystem = actorySystems.getOrElse(searchKey, null)
    if (tmpActorSystem != null)
      return tmpActorSystem
    val akkaConfigStr = akkaConfigFormat.format(host.trim, port)
    val actorconf = ConfigFactory.parseString(akkaConfigStr).withFallback(root);
    val actorSystem = ActorSystem("KamanjaManagerActorSystem", actorconf)
    actorySystems(searchKey) = actorSystem
    if (LOG.isInfoEnabled) LOG.info("Creating actorySystems for searchKey:" + searchKey + "\nakkaConfigStr:" + akkaConfigStr + "\nactorSystem:" + actorSystem)
    actorSystem
  }

  def shutDown: Unit = synchronized {
    actorySystems.foreach(kv => kv._2.shutdown())
    actorySystems.clear()
  }
}

