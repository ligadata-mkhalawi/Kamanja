package com.ligadata.test.configuration.cluster.adapters

case class TenantCacheConfig(maxSize: Int = 256) {
  override def toString: String = s"""{ "MaxSizeInMB": $maxSize}"""
}

case class ClusterCacheConfig(cacheStartPort: Int = 7800, cacheSizePerNodeInMB: Int = 256, replicationFactor: Int = 1, timeToIdleSeconds: Long = 31622400, evictionPolicy: String = "LFU") {
  override def toString: String = {
    val builder = new StringBuilder
    builder.append(""""Cache": {""")
    builder.append(s"""   "CacheStartPort": $cacheStartPort,""")
    builder.append(s"""   "CacheSizePerNodeInMB": $cacheSizePerNodeInMB,""")
    builder.append(s"""   "ReplicateFactor": $replicationFactor,""")
    builder.append(s"""   "TimeToIdleSeconds": $timeToIdleSeconds,""")
    builder.append(s"""   "EvictionPolicy": "$evictionPolicy"""")
    builder.append(s"""}""")
    return builder.toString()
  }
}
