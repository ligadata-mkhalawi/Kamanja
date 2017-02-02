package com.ligadata.test.configuration.cluster.adapters.interfaces

trait StoreType
{
  def name: String
}

class DataStore extends StoreType
{
  def name: String = "DataStore"
}

class HashMapStore extends StoreType {
  def name: String = "hashmap"
}

class CassandraStore extends StoreType {
  def name: String = "Cassandra"
}

class H2DBStore extends StoreType {
  def name: String = "h2db"
  var connectionMode: String = "embedded"
}

class HBaseStore extends StoreType {
  def name: String = "hbase"
}

trait StorageAdapter extends Adapter {
  def name: String
  def storeType: StoreType
  def schemaName: String
  def hostname: String
  def tenantId: String
}

