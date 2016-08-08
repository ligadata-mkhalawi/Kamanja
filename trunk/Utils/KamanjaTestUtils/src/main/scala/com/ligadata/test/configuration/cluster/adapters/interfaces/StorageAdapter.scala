package com.ligadata.test.configuration.cluster.adapters.interfaces

/**
  * Created by william on 4/11/16.
  */

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
  def connectionMode: String = "embedded"
}

trait StorageAdapter extends Adapter {
  def name: String
  def storeType: StoreType
  def schemaName: String
  def hostname: String
  def tenantId: String
}
