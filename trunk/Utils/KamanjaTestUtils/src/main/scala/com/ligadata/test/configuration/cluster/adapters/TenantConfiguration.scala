package com.ligadata.test.configuration.cluster.adapters

import com.ligadata.test.configuration.cluster.adapters.interfaces.StorageAdapter

case class TenantConfiguration(tenantId: String,
                               description: String,
                               cacheConfig: TenantCacheConfig,
                               primaryDataStore: StorageAdapter) {
  override def toString: String = {
    s"""{"TenantId": "$tenantId",""" + "\n" +
      s""""Description": "$description",""" + "\n" +
      s""""PrimaryDataStore": ${primaryDataStore.toString},""" + "\n" +
      s""""CacheConfig": ${cacheConfig.toString}}"""
  }
}
