
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
import com.ligadata.cache.{CacheCallback, CacheQueue, CacheQueueElement, DataCache}

class LogicalPartitionQueue(val cacheBaseName: String, val threadId: Int, val hostListStr: String, val port: Int,
                            val listenCallback: CacheCallback,
                            val deserializeCacheQueueElement: Array[Byte] => CacheQueueElement) {
  private[this] val LOG = LogManager.getLogger(getClass);

  private val infini_span_ConfigTemplate =
    """
      |
      |{
      |    "name": "%s",
      |    "jgroups.tcpping.initial_hosts": "%s",
      |    "jgroups.port": "%d",
      |    "numOfKeyOwners": "2",
      |    "maxEntries": "10000000",
      |    "CacheConfig": {
      |      "timeToIdleSeconds": "300000",
      |      "timeToLiveSeconds": "300000",
      |      "peerconfig": "jgroups_tcp.xml"
      |    }
      |  }
      |
      | """.stripMargin

  val name = cacheBaseName + "_" + threadId
  val cacheCfg = infini_span_ConfigTemplate.format(name, hostListStr, port)
  val cacheClass = "com.ligadata.cache.infinispan.MemoryDataCacheImp"
  var logicalPartCache: DataCache = null
  var logicalPartQueue: CacheQueue = null

  try {
    if (LOG.isInfoEnabled())
      LOG.info("Starting Logical Partition Queue for Thread:%d with name:%s, hostList:%s and port:%d".format(threadId, name, hostListStr, port))
    val aclass = Class.forName(cacheClass).newInstance
    val tmpLogPartCache = aclass.asInstanceOf[DataCache]
    tmpLogPartCache.init(cacheCfg, listenCallback)
    tmpLogPartCache.start()
    logicalPartCache = tmpLogPartCache

    logicalPartQueue = new CacheQueue(logicalPartCache, "-1", "-2", deserializeCacheQueueElement)
  }

  def shutDown: Unit = {
    logicalPartQueue = null
    if (logicalPartCache != null) {
      try {
        logicalPartCache.shutdown()
      } catch {
        case e: Throwable => {
          LOG.error("Failed to shutdown Logical Partition Queue", e)
        }
      }
      logicalPartCache = null
    }
  }

  final def enQ(uniqKey: String, cacheQueueEntry: KamanjaCacheQueueEntry): Unit = {
    if (logicalPartQueue != null)
      logicalPartQueue.enQ(uniqKey, cacheQueueEntry)
  }

  final def deQ(): KamanjaCacheQueueEntry = {
    var entry: KamanjaCacheQueueEntry = null
    if (logicalPartQueue != null) {
      val tmpEntry = logicalPartQueue.deQ()
      if (tmpEntry != null)
        entry = tmpEntry.asInstanceOf[KamanjaCacheQueueEntry]
    }
    entry
  }

  final def lookAheadQ(): KamanjaCacheQueueEntry = {
    var entry: KamanjaCacheQueueEntry = null
    if (logicalPartQueue != null) {
      val tmpEntry = logicalPartQueue.lookAheadQ()
      if (tmpEntry != null)
        entry = tmpEntry.asInstanceOf[KamanjaCacheQueueEntry]
    }
    entry
  }
}

