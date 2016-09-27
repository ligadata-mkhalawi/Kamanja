package com.ligadata.cache.infinispan

/**
  * Created by Saleh on 6/9/2016.
  */

import com.ligadata.cache.{CacheCustomConfig, Config}
import org.infinispan.configuration.cache.{CacheMode, ConfigurationBuilder}
import org.infinispan.configuration.global.GlobalConfigurationBuilder
import org.infinispan.eviction.EvictionStrategy
import org.infinispan.manager.DefaultCacheManager
import org.infinispan.transaction.{LockingMode, TransactionMode}

object CacheCustomConfigInfinispan {
  private var cacheManager: DefaultCacheManager = null

  def getDefaultCacheManager(configurationFile: String): DefaultCacheManager = {
    if (cacheManager != null) {
      return cacheManager
    }
    classOf[DefaultCacheManager] synchronized {
      cacheManager = new DefaultCacheManager(GlobalConfigurationBuilder.defaultClusteredBuilder()
        .transport()
        .addProperty("configurationFile", configurationFile)
        .globalJmxStatistics().allowDuplicateDomains(true).enable()
        .build(),
        null)

      return cacheManager
    }
  }
}

class CacheCustomConfigInfinispan(val jsonconfig: Config) {
  private val some = jsonconfig.getvalue(Config.CACHECONFIG)
  private val values = some.get.asInstanceOf[Map[String, String]]
  private val cacheName = jsonconfig.getvalue(Config.NAME).getOrElse("Ligadata").toString

  System.setProperty(CacheCustomConfig.PREFERIPV4STACK, "true")
  System.setProperty(CacheCustomConfig.SKIPUPDATECHECK, "true")
  System.setProperty(Config.INITIALHOSTS, jsonconfig.getvalue(Config.INITIALHOSTS).getOrElse("localhost[7800]").toString)
  System.setProperty(Config.UDPADD, jsonconfig.getvalue(Config.UDPADD).getOrElse("231.12.21.132").toString)
  System.setProperty(Config.PORT, jsonconfig.getvalue(Config.PORT).getOrElse("45566").toString)

  def getcacheName(): String = cacheName;

  def defineConfiguration(): DefaultCacheManager = {
    val cacheManager = CacheCustomConfigInfinispan.getDefaultCacheManager(values.getOrElse(CacheCustomConfig.PEERCONFIG, "jgroups_udp.xml"))
    cacheManager.defineConfiguration(cacheName,
      new ConfigurationBuilder().expiration
        .lifespan(values.getOrElse(CacheCustomConfig.TIMETOLIVESECONDS, "10000000").toLong)
        .maxIdle(values.getOrElse(CacheCustomConfig.TIMETOIDLESECONDS, "10000000").toLong)
        .transaction().transactionManagerLookup(org.infinispan.transaction.lookup.GenericTransactionManagerLookup.INSTANCE).lockingMode(LockingMode.OPTIMISTIC).transactionMode(TransactionMode.TRANSACTIONAL)
        .eviction().strategy(EvictionStrategy.LIRS).maxEntries(jsonconfig.getvalue(CacheCustomConfig.MAXENTRIES).getOrElse("300000").toLong)
        .clustering
        .cacheMode(CacheMode.DIST_SYNC)
        .hash.numOwners(jsonconfig.getvalue(Config.NUMBEROFKETOWNERS).getOrElse("1").toInt)
        .invocationBatching().enable()
        .build);
    return cacheManager
  }
}
