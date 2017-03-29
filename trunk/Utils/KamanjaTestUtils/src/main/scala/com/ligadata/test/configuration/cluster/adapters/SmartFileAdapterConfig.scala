package com.ligadata.test.configuration.cluster.adapters

import com.ligadata.test.configuration.cluster.adapters.interfaces.{AdapterSpecificConfig, AdapterType, SmartFileIOAdapter}

case class SmartFileAdapterConfig(
                                 name: String,
                                 adapterType: AdapterType,
                                 className: String = "",
                                 jarName: String = "",
                                 dependencyJars: List[String] = List(),
                                 adapterSpecificConfig: AdapterSpecificConfig = null,
                                 tenantId: String = "tenant1"
                                 ) extends SmartFileIOAdapter
