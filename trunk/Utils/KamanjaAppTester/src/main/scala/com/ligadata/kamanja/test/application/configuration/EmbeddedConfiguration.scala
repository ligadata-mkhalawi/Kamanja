package com.ligadata.kamanja.test.application.configuration

import java.io.{File, PrintWriter}
import java.util.NoSuchElementException

import com.ligadata.test.utils._

import scala.io.Source

object EmbeddedConfiguration {
  val storageDir = TestUtils.constructTempDir("/h2db").getAbsolutePath + "/storage"
  val h2dbPortNumber = TestUtils.getAvailablePort
  val pythonPortNumber = TestUtils.getAvailablePort
  val clusterCacheStartPort = TestUtils.getAvailablePort
  val kafkaPort = TestUtils.getAvailablePort
  private var pythonHome: String = ""

  try {
    pythonHome = sys.env("PYTHON_HOME")
  }
  catch {
    case e: NoSuchElementException =>
      throw new Exception("***ERROR*** Failed to discover environmental variable PYTHON_HOME. " +
        "Please set it before running.\n" +
        "EX: export PYTHON_HOME=/usr")
  }

  def getSystemAdapterBindings: String = {
      """
        |[
        |  {
        |    "AdapterName": "TestStatus_1",
        |    "MessageNames": [
        |      "com.ligadata.KamanjaBase.KamanjaStatusEvent"
        |    ],
        |    "Serializer": "com.ligadata.kamanja.serializer.csvserdeser",
        |    "Options": {
        |      "alwaysQuoteFields": false,
        |      "fieldDelimiter": ","
        |    }
        |  },
        |  {
        |    "AdapterName": "TestFailedEvents_1",
        |    "MessageNames": [
        |      "com.ligadata.KamanjaBase.KamanjaExecutionFailureEvent"
        |    ],
        |    "Serializer": "com.ligadata.kamanja.serializer.jsonserdeser",
        |    "Options": {
        |    }
        |  },
        |  {
        |    "AdapterName": "TestMessageEvents_1",
        |    "MessageNames": [
        |      "com.ligadata.KamanjaBase.KamanjaMessageEvent"
        |    ],
        |    "Serializer": "com.ligadata.kamanja.serializer.jsonserdeser",
        |    "Options": {
        |    }
        |  }
        |]
      """.stripMargin
  }

  def generateClusterConfigFile(kamanjaInstallDir: String, kafkaBrokerList: String, zkConnectString: String): String = {
    val clusterCfgStr = s"""
      |{
      |  "Clusters": [
      |    {
      |      "ClusterId": "ligadata1",
      |      "SystemCatalog": {
      |        "StoreType": "h2db",
      |        "connectionMode": "embedded",
      |        "SchemaName": "kamanja",
      |        "Location": "$storageDir/syscatalog",
      |        "portnumber": "",
      |        "user": "test",
      |        "password": "test"
      |      },
      |      "Tenants": [
      |        {
      |          "TenantId": "tenant1",
      |          "Description": "tenant1",
      |          "PrimaryDataStore": {
      |            "StoreType": "h2db",
      |            "connectionMode": "embedded",
      |            "SchemaName": "testdata",
      |            "Location": "$storageDir/tenant1_default",
      |            "portnumber": "$h2dbPortNumber",
      |            "user": "test",
      |            "password": "test"
      |          },
      |          "CacheConfig": {
      |            "MaxSizeInMB": 256
      |          }
      |        }
      |      ],
      |      "ZooKeeperInfo": {
      |        "ZooKeeperNodeBasePath": "/kamanja",
      |        "ZooKeeperConnectString": "$zkConnectString",
      |        "ZooKeeperSessionTimeoutMs": "30000",
      |        "ZooKeeperConnectionTimeoutMs": "30000"
      |      },
      |      "PYTHON_CONFIG" : {
      |        "PYTHON_PATH": "$kamanjaInstallDir/python",
      |        "SERVER_BASE_PORT": $pythonPortNumber,
      |        "SERVER_PORT_LIMIT": 40,
      |        "SERVER_HOST": "localhost",
      |        "PYTHON_LOG_CONFIG_PATH": "$kamanjaInstallDir/python/bin/pythonlog4j.cfg",
      |        "PYTHON_LOG_PATH": "$kamanjaInstallDir/python/logs/pythonserver.log",
      |        "PYTHON_BIN_DIR" : "$pythonHome/bin"
      |      },
      |      "EnvironmentContext": {
      |        "classname": "com.ligadata.SimpleEnvContextImpl.SimpleEnvContextImpl$$",
      |        "jarname": "KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |        "dependencyjars": [
      |          "ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |          "ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
      |        ]
      |      },
      |      "Cache": {
      |        "CacheStartPort": $clusterCacheStartPort,
      |        "CacheSizePerNodeInMB": 256,
      |        "ReplicateFactor": 1,
      |        "TimeToIdleSeconds": 31622400,
      |        "EvictionPolicy": "LFU"
      |      },
      |      "Nodes": [
      |        {
      |          "NodeId": "1",
      |          "NodePort": 6541,
      |          "NodeIpAddr": "localhost",
      |          "JarPaths": [
      |            "$kamanjaInstallDir/lib/system",
      |            "$kamanjaInstallDir/lib/application"
      |          ],
      |          "Scala_home": "/Users/william/.svm/current/rt",
      |          "Java_home": "/Library/Java/JavaVirtualMachines/jdk1.8.0_101.jdk/Contents/Home",
      |          "Roles": [
      |            "RestAPI",
      |            "ProcessingEngine"
      |          ],
      |          "Classpath": ".:$kamanjaInstallDir/lib/system/ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar:$kamanjaInstallDir/lib/system/KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar:$kamanjaInstallDir/lib/system/ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
      |        }
      |      ],
      |      "Adapters": [
      |        {
      |          "Name": "TestIn_1",
      |          "TypeString": "Input",
      |          "TenantId": "tenant1",
      |          "ClassName": "com.ligadata.kafkaInputOutputAdapters_v10.KamanjaKafkaConsumer$$",
      |          "JarName": "kamanjakafkaadapters_0_10_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |          "DependencyJars": [
      |            "kafka-clients-0.10.0.1.jar",
      |            "KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |            "ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |            "ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
      |          ],
      |          "AdapterSpecificCfg": {
      |            "HostList": "$kafkaBrokerList",
      |            "TopicName": "testin_1"
      |          }
      |        },
      |        {
      |          "Name": "TestOut_1",
      |          "TypeString": "Output",
      |          "TenantId": "tenant1",
      |          "ClassName": "com.ligadata.kafkaInputOutputAdapters_v10.KafkaProducer$$",
      |          "JarName": "kamanjakafkaadapters_0_10_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |          "DependencyJars": [
      |            "kafka-clients-0.10.0.1.jar",
      |            "KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |            "ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |            "ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
      |          ],
      |          "AdapterSpecificCfg": {
      |            "HostList": "$kafkaBrokerList",
      |            "TopicName": "testout_1"
      |          }
      |        },
      |        {
      |          "Name": "TestFailedEvents_1",
      |          "TypeString": "Output",
      |          "TenantId": "tenant1",
      |          "ClassName": "com.ligadata.kafkaInputOutputAdapters_v10.KafkaProducer$$",
      |          "JarName": "kamanjakafkaadapters_0_10_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |          "DependencyJars": [
      |            "kafka-clients-0.10.0.1.jar",
      |            "KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |            "ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |            "ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
      |          ],
      |          "AdapterSpecificCfg": {
      |            "HostList": "$kafkaBrokerList",
      |            "TopicName": "testfailedevents_1"
      |          }
      |        },
      |        {
      |          "Name": "TestMessageEvents_1",
      |          "TypeString": "Output",
      |          "TenantId": "tenant1",
      |          "ClassName": "com.ligadata.kafkaInputOutputAdapters_v10.KafkaProducer$$",
      |          "JarName": "kamanjakafkaadapters_0_10_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |          "DependencyJars": [
      |            "kafka-clients-0.10.0.1.jar",
      |            "KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |            "ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |            "ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
      |          ],
      |          "AdapterSpecificCfg": {
      |            "HostList": "$kafkaBrokerList",
      |            "TopicName": "testmessageevents_1"
      |          }
      |        },
      |        {
      |          "Name": "TestStatus_1",
      |          "TypeString": "Output",
      |          "TenantId": "System",
      |          "ClassName": "com.ligadata.kafkaInputOutputAdapters_v10.KafkaProducer$$",
      |          "JarName": "kamanjakafkaadapters_0_10_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |          "DependencyJars": [
      |            "kafka-clients-0.10.0.1.jar",
      |            "KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |            "ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      |            "ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
      |          ],
      |          "AdapterSpecificCfg": {
      |            "HostList": "$kafkaBrokerList",
      |            "TopicName": "teststatus_1"
      |          }
      |        }
      |      ]
      |    }
      |  ]
      |}
    """.stripMargin
    val clusterConfigFile = TestUtils.constructTempDir("/cluster-tmp-config").getAbsolutePath + "/ClusterConfig.json"
    new PrintWriter(clusterConfigFile) {
      write(clusterCfgStr)
      close()
    }

    return clusterConfigFile
  }

  def generateKamanjaConfigFile: String = {
    val kamanjaConfigFile = TestUtils.constructTempDir("/kamanja-tmp-config").getAbsolutePath + "/kamanja.conf"
    new PrintWriter(kamanjaConfigFile) {
      write("NODEID=1\n")
      write(s"""MetadataDataStore={"StoreType": "h2db", "connectionMode": "embedded", "SchemaName": "kamanja", "Location": "$storageDir", "portnumber": "$h2dbPortNumber", "user": "test", "password": "test"}""")
      close()
    }
    return kamanjaConfigFile
  }

  def generateMetadataAPIConfigFile(kamanjaInstallDir: String, zkConnectString: String): String = {
    val metadataAPIConfigFile = TestUtils.constructTempDir("/metadata-tmp-config").getAbsolutePath + "MetadataAPIConfig.properties"
    new PrintWriter(metadataAPIConfigFile) {
      write("NODEID=1\n")
      write(s"""MetadataDataStore={"StoreType": "h2db", "connectionMode": "embedded", "SchemaName": "kamanja", "Location": "$storageDir", "portnumber": "$h2dbPortNumber", "user": "test", "password": "test"}""" + "\n")
      write(s"ROOT_DIR=$kamanjaInstallDir\n")
      write(s"ZOOKEEPER_CONNECT_STRING=$zkConnectString")
      close()
    }
    return metadataAPIConfigFile
  }
}