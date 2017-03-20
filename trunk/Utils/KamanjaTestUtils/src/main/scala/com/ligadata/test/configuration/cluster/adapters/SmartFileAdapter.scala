package com.ligadata.test.configuration.cluster.adapters

import com.ligadata.test.configuration.cluster.adapters.interfaces.{AdapterSpecificConfig, AdapterType, SmartFileIOAdapter}

//case class KafkaAdapterSpecificConfig(hostList: String = "localhost:9092", topicName: String) extends AdapterSpecificConfig {
//  override def toString = s"""{"HostList": "$hostList", "TopicName": "$topicName"}"""
//}

case class SmartFileAdapterSpecificConfig(typ: String,
                                          connConfig: ConnectionConfig,
                                          monitoringConfig: MonitoringConfig,
                                          archiveConfig: ArchiveConfig
                                         )

case class ConnectionConfig()

case class DetailedLocation(sourceDirectory: String, targetDirectory: String, archiveRelativePath: String) {
  override def toString = {
    s"""{
          "srcDir": "$sourceDirectory",
          "targetDir": "$targetDirectory",
          "ArchiveRelativePath": "$archiveRelativePath"
        }
    """.stripMargin
  }
}

case class ArchiveConfig(uri: String, consolidationMaxSizeInMB: String, archiveParallelism: String, compression: String, rolloverInterval: String)

case class MonitoringConfig(
                           maxTimeWait: Int,
                           workerBufferSize: Int,
                           consumersCount: Int,
                           messageSeparator: String,
                           monitoringThreadsCount: String,
                           checkFilesType: Boolean,
                           enableDelete: String,
                           enableMoving: String,
                           detailedLocations: List[DetailedLocation]
                           )

case class SmartFileAdapterConfig(
                                 name: String,
                                 adapterType: AdapterType,
                                 override val associatedMessage: String,
                                 override val keyValueDelimiter: String,
                                 override val fieldDelimiter: String,
                                 override val valueDelimiter: String,
                                 className: String,
                                 jarName: String,
                                 dependencyJars: List[String],
                                 adapterSpecificConfig: SmartFileAdapterSpecificConfig,
                                 tenantId: String
                                 ) extends SmartFileIOAdapter {
  override def toString =
    """{
      |
    """.stripMargin
}
