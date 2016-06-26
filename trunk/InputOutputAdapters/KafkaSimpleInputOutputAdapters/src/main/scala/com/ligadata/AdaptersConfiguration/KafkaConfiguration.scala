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

package com.ligadata.AdaptersConfiguration

import com.ligadata.InputOutputAdapterInfo.{ AdapterConfiguration, PartitionUniqueRecordKey, PartitionUniqueRecordValue }
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

class KafkaQueueAdapterConfiguration extends AdapterConfiguration {
  var hosts: Array[String] = _ // each host is HOST:PORT
  var topic: String = _ // topic name
  var noDataSleepTimeInMs: Int = 300 // No Data Sleep Time in milli secs. Default is 300 ms
  var instancePartitions: Set[Int] = _ // Valid only for Input Queues. These are the partitions we handle for this Queue. For now we are treating Partitions as Ints. (Kafka handle it as ints)
  var otherconfigs = scala.collection.mutable.Map[String, String]() // Making the key is lowercase always

  // Security Realted stuff
  var security_protocol: String = _ // SASL, SSL, or None (default)

  // SSL stuff
  var ssl_key_password: String = _
  var ssl_keystore_password: String = _
  var ssl_keystore_location: String = _
  var ssl_keystore_type: String = _
  var ssl_truststore_password: String = _
  var ssl_truststore_location: String = _
  var ssl_truststore_type: String = _
  var ssl_enabled_protocols: String = _
  var ssl_protocol: String = _
  var ssl_provider: String = _
  var ssl_cipher_suites: String = _
  var ssl_endpoint_identification_algorithm : String = _
  var ssl_keymanager_algorithm: String = _
  var ssl_trust_manager_algorithm: String = _

  //SADL stuff
  var sasl_kerberos_service_name : String = _
  var sasl_mechanism : String = _
  var sasl_kerberos_kinit_cmd : String = _
  var sasl_kerberos_min_time_before_relogic: String = _
  var sasl_kerberos_ticket_renew_jiter: String = _
  var sasl_kerberos_ticket_renew_window_factor : String = _

  override  def toString: String = {
    var outString: String = ""

    if (hosts != null) {
      outString = outString + "  host: " + hosts.mkString(":") + "/n"
    }
    if (topic != null) {
      outString = outString + "  topic: " + topic + "/n"
    }
    if (instancePartitions != null) {
      outString = outString + "  instancePartitions: " + instancePartitions.mkString(":") + "/n"
    }
    if (otherconfigs != null) {
      outString = outString + "  otherconfigs: " + otherconfigs.mkString(":") + "/n"
    }
    if (security_protocol != null) {
      outString = outString + "  security_protocol: " + security_protocol + "/n"
    }
    if (sasl_kerberos_service_name != null) {
      outString = outString + "  sasl_kerberos_service_name: " + sasl_kerberos_service_name + "/n"
    }
    if (sasl_mechanism != null) {
      outString = outString + "  sasl_mechanism: " + sasl_mechanism + "/n"
    }
    if (sasl_kerberos_kinit_cmd != null) {
      outString = outString + "  sasl_kerberos_kinit_cmd: " + sasl_kerberos_kinit_cmd + "/n"
    }
    if (sasl_kerberos_min_time_before_relogic != null) {
      outString = outString + "  sasl_kerberos_min_time_before_relogic: " + sasl_kerberos_min_time_before_relogic + "/n"
    }
    if (sasl_kerberos_ticket_renew_jiter != null) {
      outString = outString + "  sasl_kerberos_ticket_renew_jiter: " + sasl_kerberos_ticket_renew_jiter + "/n"
    }
    if (sasl_kerberos_ticket_renew_window_factor != null) {
      outString = outString + "  sasl_kerberos_ticket_renew_window_factor: " + sasl_kerberos_ticket_renew_window_factor + "/n"
    }


    if (ssl_key_password != null) {
      outString = outString + "  ssl_key_password:  ------ /n"
    }
    if (ssl_keystore_password != null) {
      outString = outString + "  ssl_keystore_password:  ------ /n"
    }
    if (ssl_keystore_location != null) {
      outString = outString + "  ssl_keystore_location: " + ssl_keystore_location + "/n"
    }
    if (ssl_keystore_type != null) {
      outString = outString + "  ssl_keystore_type: " + ssl_keystore_type + "/n"
    }
    if (ssl_truststore_password != null) {
      outString = outString + "  ssl_truststore_password:  ------ /n"
    }
    if (ssl_truststore_location != null) {
      outString = outString + "  ssl_truststore_location: " + ssl_truststore_location + "/n"
    }
    if (ssl_truststore_type != null) {
      outString = outString + "  ssl_truststore_type: " + ssl_truststore_type + "/n"
    }
    if (ssl_enabled_protocols != null) {
      outString = outString + "  ssl_enabled_protocols: " + ssl_enabled_protocols + "/n"
    }
    if (ssl_protocol != null) {
      outString = outString + "  ssl_protocol: " + ssl_protocol + "/n"
    }
    if (ssl_provider != null) {
      outString = outString + "  ssl_provider: " + ssl_provider + "/n"
    }
    if (ssl_cipher_suites != null) {
      outString = outString + "  ssl_cipher_suites: " + ssl_cipher_suites + "/n"
    }
    if (ssl_endpoint_identification_algorithm != null) {
      outString = outString + "  ssl_endpoint_identification_algorithm: " + ssl_endpoint_identification_algorithm + "/n"
    }
    if (ssl_keymanager_algorithm != null) {
      outString = outString + "  ssl_keymanager_algorithm: " + ssl_keymanager_algorithm + "/n"
    }
    if (ssl_trust_manager_algorithm != null) {
      outString = outString + "  ssl_trust_manager_algorithm: " + ssl_trust_manager_algorithm + "/n"
    }

    return outString
  }

}

object KafkaConstants {
  val KAFKA_SEND_SUCCESS = 0
  val KAFKA_SEND_Q_FULL = 1
  val KAFKA_SEND_DEAD_PRODUCER = 2
  val KAFKA_NOT_SEND = 3
}

object KafkaQueueAdapterConfiguration {
  def GetAdapterConfig(inputConfig: AdapterConfiguration): KafkaQueueAdapterConfiguration = {
    if (inputConfig.adapterSpecificCfg == null || inputConfig.adapterSpecificCfg.size == 0) {
      val err = "Not found Host/Brokers and topicname for Kafka Adapter Config:" + inputConfig.Name
      throw new Exception(err)
    }

    val qc = new KafkaQueueAdapterConfiguration
    qc.Name = inputConfig.Name
    qc.className = inputConfig.className
    qc.jarName = inputConfig.jarName
    qc.dependencyJars = inputConfig.dependencyJars

    val adapCfg = parse(inputConfig.adapterSpecificCfg)
    if (adapCfg == null || adapCfg.values == null) {
      val err = "Not found Host/Brokers and topicname for Kafka Adapter Config:" + inputConfig.Name
      throw new Exception(err)
    }
    val values = adapCfg.values.asInstanceOf[Map[String, String]]

    values.foreach(kv => {
      if (kv._1.compareToIgnoreCase("bootstrap.servers") == 0) {
        qc.hosts = kv._2.split(",").map(str => str.trim).filter(str => str.size > 0)
      } else  if (kv._1.compareToIgnoreCase("HostList") == 0) {
        qc.hosts = kv._2.split(",").map(str => str.trim).filter(str => str.size > 0)
      } else if (kv._1.compareToIgnoreCase("TopicName") == 0) {
        qc.topic = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("NoDataSleepTimeInMs") == 0) {
        qc.noDataSleepTimeInMs = kv._2.trim.toInt
        if (qc.noDataSleepTimeInMs < 0)
          qc.noDataSleepTimeInMs = 0
      } else if (kv._1.compareToIgnoreCase("security.protocol") == 0) {
        qc.security_protocol = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.key.password") == 0) {
        qc.ssl_key_password = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.keystore.password") == 0) {
        qc.ssl_keystore_password = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.keystore.location") == 0) {
        qc.ssl_keystore_location = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.keystore.type") == 0) {
        qc.ssl_keystore_type = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.truststore.password") == 0) {
        qc.ssl_truststore_password = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.truststore.location") == 0) {
        qc.ssl_truststore_location = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.truststore.type") == 0) {
        qc.ssl_truststore_type = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.enabled.protocols") == 0) {
        qc.ssl_enabled_protocols = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.protocol") == 0) {
        qc.ssl_protocol = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.provider") == 0) {
        qc.ssl_provider = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.cipher.suites") == 0) {
        qc.ssl_cipher_suites = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.endpoint.identification.algorithm") == 0) {
        qc.ssl_endpoint_identification_algorithm = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.keymanager.algorithm") == 0) {
        qc.ssl_keymanager_algorithm = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("ssl.trust.manager.algorithm") == 0) {
        qc.ssl_trust_manager_algorithm = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("sasl.kerberos.service.name") == 0) {
        qc.sasl_kerberos_service_name = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("sasl.mechanism") == 0) {
        qc.sasl_mechanism = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("sasl.kerberos.kinit.cmd") == 0) {
        qc.sasl_kerberos_kinit_cmd = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("sasl.min.time.before.relogic") == 0) {
        qc.sasl_kerberos_min_time_before_relogic = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("sasl.kerberos.ticket.renew.jiter") == 0) {
        qc.sasl_kerberos_ticket_renew_jiter = kv._2.trim
      } else if (kv._1.compareToIgnoreCase("sasl.kerberos.ticket.renew.window.factor") == 0) {
        qc.sasl_kerberos_ticket_renew_window_factor = kv._2.trim
      }
      else {
        qc.otherconfigs(kv._1.toLowerCase()) = kv._2;
      }
    })

    qc.instancePartitions = Set[Int]()

    qc
  }
}

case class KafkaKeyData(Version: Int, Type: String, Name: String, TopicName: Option[String], PartitionId: Option[Int]) // Using most of the values as optional values. Just thinking about future changes. Don't know the performance issues.

class KafkaPartitionUniqueRecordKey extends PartitionUniqueRecordKey {
  val Version: Int = 1
  val Type: String = "Kafka"
  var Name: String = _ // Name
  var TopicName: String = _ // Topic Name
  var PartitionId: Int = _ // Partition Id

  override def Serialize: String = { // Making String from key
    val json =
      ("Version" -> Version) ~
        ("Type" -> Type) ~
        ("Name" -> Name) ~
        ("TopicName" -> TopicName) ~
        ("PartitionId" -> PartitionId)
    compact(render(json))
  }

  override def Deserialize(key: String): Unit = { // Making Key from Serialized String
    implicit val jsonFormats: Formats = DefaultFormats
    val keyData = parse(key).extract[KafkaKeyData]
    if (keyData.Version == Version && keyData.Type.compareTo(Type) == 0) {
      Name = keyData.Name
      TopicName = keyData.TopicName.get
      PartitionId = keyData.PartitionId.get
    }
    // else { } // Not yet handling other versions
  }
}

case class KafkaRecData(Version: Int, Offset: Option[Long]) // Using most of the values as optional values. Just thinking about future changes. Don't know the performance issues.

class KafkaPartitionUniqueRecordValue extends PartitionUniqueRecordValue {
  val Version: Int = 1
  var Offset: Long = -1 // Offset 

  override def Serialize: String = { // Making String from Value
    val json =
      ("Version" -> Version) ~
        ("Offset" -> Offset)
    compact(render(json))
  }

  override def Deserialize(key: String): Unit = { // Making Value from Serialized String
    implicit val jsonFormats: Formats = DefaultFormats
    val recData = parse(key).extract[KafkaRecData]
    if (recData.Version == Version) {
      Offset = recData.Offset.get
    }
    // else { } // Not yet handling other versions
  }
}

