
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

import akka.routing.RoundRobinPool
import org.apache.logging.log4j.{LogManager, Logger}
//import java.util.concurrent._
import akka.actor._

case class LogicalPartitionLocalExec(txnId: Long) extends KamanjaManagerAkkaWorklet

case class LogicalPartitionRemoteExec(txnId: Long, cacheQueueEntry: KamanjaCacheQueueEntry) extends KamanjaManagerAkkaWorklet

class LogicalPartitionAkkaExecution(val cacheBaseName: String, val threadId: Int, val startPartRange: Int, val endPartitionRange: Int, val host: String, val port: Int, val createLocalExecutor: Boolean) {
  private[this] val LOG = LogManager.getLogger(getClass);
  private[this] var localExecutor: LeanringEngineRemoteExecution = null
  private[this] val workerName = cacheBaseName + "_km_actor_worker_thread_" + threadId
  private[this] val actorSystem = AkkaActorSystem.getActorSystem(host, port)
  private[this] var worker: ActorRef = _

  if (createLocalExecutor) {
    localExecutor = new LeanringEngineRemoteExecution(threadId.toShort, startPartRange, endPartitionRange)
    worker = actorSystem.actorOf(Props(new ActorWorker), name = workerName)
    if (LOG.isInfoEnabled) LOG.info("Creating local executor for threadId:%d, startPartRange:%d, endPartitionRange:%d, host:%d, port:%d, workerName:%s, createLocalExecutor:%s".format(threadId, startPartRange, endPartitionRange, host, port, workerName, createLocalExecutor.toString))

    class ActorWorker extends Actor {
      def receive = {
/*
        case Terminated(r) => {
          // Killing local worker
          worker ! Kill
        }
*/
        case LogicalPartitionLocalExec(txnId) => {
          try {
            if (LOG.isTraceEnabled) LOG.trace("Executing Local Logical partition for transactionid:" + txnId)
            val dqKamanjaCacheQueueEntry = KamanjaLeader.GetFromLocalCacheQueueEntriesForCache(txnId)
            localExecutor.executeModelsForTxnId(dqKamanjaCacheQueueEntry)
          } catch {
            case e: ActorKilledException => {
              if (LOG.isWarnEnabled) LOG.warn("Killing actor:" + worker.path, e)
            }
            case e: Throwable => {
              throw e
            }
          }
        }
        case LogicalPartitionRemoteExec(txnId, cacheQueueEntry) => {
          try {
            if (LOG.isTraceEnabled) LOG.trace("Executing Remote Logical partition for transactionid:" + txnId)
            localExecutor.executeModelsForTxnId(cacheQueueEntry)
          } catch {
            case e: ActorKilledException => {
              if (LOG.isWarnEnabled) LOG.warn("Killing actor:" + worker.path, e)
            }
            case e: Throwable => {
              throw e
            }
          }
        }
        case _ => {
          if (LOG.isWarnEnabled) LOG.warn("Not found any match")
        }
      }

      /*
            def shuttingDown: Receive = {
              case "job" => sender() ! "service unavailable, shutting down"
              case Terminated(`worker`) =>
                // context stop self
            }
      */

      override def postStop() {
        // clean up some resources ...
      }
    }
  }

  var actorSelection: ActorSelection = _

  def shutDown: Unit = {
    if (worker != null)
      worker ! Kill
    // if (actorSelection != null)
    //  actorSelection ! Kill
  }

  private def getActorSelector: ActorSelection = {
    if (actorSelection != null)
      return actorSelection

    synchronized {
      if (actorSelection != null)
        return actorSelection
      val nm = "/user/" + workerName
      val hostport = host + ":" + port
      actorSelection = actorSystem.actorSelection("akka.tcp://KamanjaManagerActorSystem@" + hostport + nm)
      if (LOG.isTraceEnabled) LOG.trace("Actor Selection for hostport:" + hostport + ", workerName:" + workerName + ", nm:" + nm + ", actorSelection:" + actorSelection)
    }

    actorSelection
  }

  def process(txnId: Long, cacheQueueEntry: KamanjaCacheQueueEntry): Unit = {
    val runner = getActorSelector
    if (LOG.isTraceEnabled) LOG.trace("processing txnId:" + txnId + ", createLocalExecutor:" + createLocalExecutor + ", runner:" + runner)
    if (createLocalExecutor)
      runner ! LogicalPartitionLocalExec(txnId)
    else
      runner ! LogicalPartitionRemoteExec(txnId, cacheQueueEntry)
  }
}

