/*
 * Copyright 2015 ligaDATA
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

package com.ligadata.test.configuration.cluster

import com.ligadata.test.configuration.cluster.adapters.{ClusterCacheConfig, TenantConfiguration}
import com.ligadata.test.configuration.cluster.adapters.interfaces.{Adapter, H2DBStore, StorageAdapter}
import com.ligadata.test.configuration.cluster.nodes.NodeConfiguration
import com.ligadata.test.configuration.cluster.python.PythonConfiguration
import com.ligadata.test.configuration.cluster.zookeeper.ZookeeperConfig

import scala.collection.mutable.HashMap

case class Cluster(var id: String,
                   var systemCatalog: StorageAdapter,
                   var tenants: Array[TenantConfiguration],
                   var zookeeperConfig: ZookeeperConfig,
                   var pythonConfig: PythonConfiguration,
                   var envContext: EnvironmentContextConfig,
                   var clusterCacheConfig: ClusterCacheConfig,
                   var nodes: Array[NodeConfiguration],
                   var adapters: Array[Adapter],
                   var customCfg: Option[HashMap[String, String]]) {

  def toCluster(clusterConfigString: String): Cluster = {

    return this
  }

  override def toString: String = {
    try {
      val builder = new StringBuilder
      builder.append(s"{\n")
      builder.append(s"""  "Clusters": [""" + "\n")
      builder.append(s"""    {""" + "\n")
      customCfg match {
        case Some(configMap) => {
          configMap.foreach(config => {
            builder.append(s""""${config._1}": "${config._2}",""" + "\n")
          })
        }
        case None =>
      }
      builder.append(s"""      "ClusterId": "$id", """ + "\n")
      builder.append(s"""      "SystemCatalog": { """ + "\n")
      builder.append(s"""        "StoreType": "${systemCatalog.storeType.name}",""" + "\n")
      systemCatalog.storeType match {
        case s @ H2DBStore => {
          builder.append(s""""connectionMode": "${s.connectionMode}",""" + "\n")
          builder.append(s""""portnumber": "9100",""" + "\n")
          builder.append(s""""user": "test",""" + "\n")
          builder.append(s""""password": "test",""" + "\n")
        }
      }
      builder.append(s"""        "SchemaName": "${systemCatalog.schemaName}",""" + "\n")
      builder.append(s"""        "Location": "${systemCatalog.hostname}"""" + "\n")
      builder.append(s"""      },""" + "\n")
      builder.append(s"""      "Tenants": [""" + "\n")
      for (i <- 0 to tenants.length - 1) {
        if (i == tenants.length - 1)
          builder.append(tenants(i).toString + "\n")
        else
          builder.append(tenants(i).toString + ",")
      }
      builder.append(s"""      ],""" + "\n")
      builder.append(s"""      "ZooKeeperInfo": """ + "\n")
      builder.append(s"""        ${zookeeperConfig.toString},""" + "\n")
      builder.append(s"""        ${envContext.toString}, """ + "\n")
      builder.append(s"""        ${clusterCacheConfig.toString},""" + "\n")
      builder.append(s"""      ${pythonConfig.toString},""" + "\n")
      builder.append(s"""      "Nodes": [""" + "\n")
      for (i <- 0 to nodes.length - 1) {
        if (i == nodes.length - 1)
          builder.append(nodes(i).toString + "\n")
        else
          builder.append(nodes(i).toString + ",\n")
      }
      builder.append("]," + "\n")
      builder.append(s""""Adapters": [ """ + "\n")
      for (i <- 0 to adapters.length - 1) {
        if (i == adapters.length - 1)
          builder.append(adapters(i).toString)
        else
          builder.append(adapters(i).toString + ",")
      }
      builder.append("]\n}\n]\n}\n")
      builder.toString()
    }
    catch {
      case e: Exception => throw new KamanjaConfigurationException("[Kamanja Test Cluster Configuration]: Failed to convert cluster configuration to string", e)
    }
  }
}


class ClusterBuilder {
  private var id: String = _
  private var systemCatalog: StorageAdapter = _
  private var adapters: Array[Adapter] = Array()
  private var envContext: EnvironmentContextConfig = _
  private var nodes: Array[NodeConfiguration] = Array()
  private var tenants: Array[TenantConfiguration] = Array()
  private var zkInfo: ZookeeperConfig = _
  private var pythonConfig: PythonConfiguration = _
  private var clusterCacheConfig: ClusterCacheConfig = _
  private val customCfg: scala.collection.mutable.HashMap[String, String] = new HashMap[String, String]()

  def withZkInfo(zookeeperConfig: ZookeeperConfig): ClusterBuilder = {
    this.zkInfo = zookeeperConfig
    this
  }

  def withPythonConfig(pythonConfig: PythonConfiguration): ClusterBuilder = {
    this.pythonConfig = pythonConfig
    this
  }

  def withId(id: String): ClusterBuilder = {
    this.id = id
    this
  }

  def withEnvContext(environmentContextConfig: EnvironmentContextConfig): ClusterBuilder = {
    this.envContext = environmentContextConfig
    this
  }

  def withTenant(tenantConfiguration: TenantConfiguration): ClusterBuilder ={
    this.tenants = this.tenants :+ tenantConfiguration
    this
  }

  def withNode(node: NodeConfiguration): ClusterBuilder = {
    this.nodes = this.nodes :+ node
    this
  }

  def withAdapter(adapter: Adapter): ClusterBuilder = {
    this.adapters = this.adapters :+ adapter
    this
  }

  def withAdapters(adapters: List[Adapter]): ClusterBuilder = {
    adapters.foreach(adapter => {
      this.adapters = this.adapters :+ adapter
    })
    this
  }

  def withSystemCatalog(storageAdapter: StorageAdapter): ClusterBuilder = {
    this.systemCatalog = storageAdapter
    this
  }

  def withCustomCfg(key: String, value: String): ClusterBuilder = {
    this.customCfg(key) = value
    this
  }

  def withClusterCacheConfig(clusterCacheConfig: ClusterCacheConfig): ClusterBuilder = {
    this.clusterCacheConfig = clusterCacheConfig
    this
  }

  def build(): Cluster = {
    var cluster: Cluster = null
    if (customCfg.isEmpty) {
      cluster = new Cluster(id, systemCatalog, tenants, zkInfo, pythonConfig, envContext, clusterCacheConfig, nodes, adapters, None)
    }
    else
     cluster = new Cluster(id, systemCatalog, tenants, zkInfo, pythonConfig, envContext, clusterCacheConfig, nodes, adapters, Some(customCfg))
    return cluster
  }
}