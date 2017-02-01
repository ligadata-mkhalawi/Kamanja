package com.ligadata.test.configuration.cluster.adapters.interfaces

trait StoreType
{
  def name: String
}

object DataStore extends StoreType
{
  def name: String = "DataStore"
}

object HashMapStore extends StoreType {
  def name: String = "hashmap"
}

object CassandraStore extends StoreType {
  def name: String = "Cassandra"
}

object H2DBStore extends StoreType {
  def name: String = "h2db"
  var connectionMode: String = "embedded"
}

object HBaseStore extends StoreType {
  def name: String = "hbase"
}

trait StorageAdapter extends Adapter {
  def name: String
  def storeType: StoreType
  def schemaName: String
  def hostname: String
  def tenantId: String
}

