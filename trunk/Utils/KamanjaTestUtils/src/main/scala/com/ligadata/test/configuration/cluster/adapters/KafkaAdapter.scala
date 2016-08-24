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

package com.ligadata.test.configuration.cluster.adapters

import com.ligadata.test.configuration.cluster.KamanjaConfigurationException
import com.ligadata.test.configuration.cluster.adapters.interfaces.{AdapterSpecificConfig, AdapterType, KafkaIOAdapter}
import com.ligadata.test.utils.TestUtils

case class KafkaAdapterSpecificConfig(hostList: String = "localhost:9092", topicName: String) extends AdapterSpecificConfig {
  override def toString = s"""{"HostList": "$hostList", "TopicName": "$topicName"}"""
}

case class KafkaAdapterConfig(
                             name: String,
                             adapterType: AdapterType,
                             override val associatedMessage: String,
                             override val keyValueDelimiter: String,
                             override val fieldDelimiter: String,
                             override val valueDelimiter: String,
                             className: String,
                             jarName: String,
                             dependencyJars: List[String],
                             adapterSpecificConfig: KafkaAdapterSpecificConfig,
                             tenantId: String
                             ) extends KafkaIOAdapter {
  override def toString: String = {
    val builder = StringBuilder.newBuilder
    builder.append("{\n")
    builder.append(s"""  "Name": "$name",""" + "\n")
    builder.append(s"""  "TypeString": "${adapterType.name}",""" + "\n")
    builder.append(s"""  "TenantId": "$tenantId",""" + "\n")
    builder.append(s"""  "JarName": "$jarName",""" + "\n")
    if(associatedMessage != "" && associatedMessage != null)
      builder.append(s"""  "AssociatedMessage": "$associatedMessage",""" + "\n")
    if(keyValueDelimiter != "" && keyValueDelimiter != null)
      builder.append(s"""  "KeyValueDelimiter": "$keyValueDelimiter",""" + "\n")
    if(fieldDelimiter != "" && fieldDelimiter != null)
      builder.append(s"""  "FieldDelimiter": "$fieldDelimiter",""" + "\n")
    builder.append(s"""  "ClassName": "$className",""" + "\n")
    builder.append(s"""  "DependencyJars": [ "${dependencyJars.mkString("\", \"")}"],""" + "\n")
    builder.append(s"""  "AdapterSpecificCfg": ${adapterSpecificConfig.toString}""" + "\n")
    builder.append("}")
    return builder.toString()
  }
}

class KafkaAdapterBuilder {
  private var name: String = _
  private var adapterType: AdapterType = _
  private var associatedMessage: String = _
  private var keyValueDelimiter: String = _
  private var fieldDelimiter: String = _
  private var valueDelimiter: String = _
  private var className: String = _
  private var jarName: String = s"kamanjakafkaadapters_0_10_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
  private var dependencyJars: List[String] =
    List(s"kafka-clients-0.10.0.0.jar",
      s"KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      s"ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      s"ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar")
  private var adapterSpecificConfig: KafkaAdapterSpecificConfig = _
  private var tenantId: String = _

  def withName(name: String): KafkaAdapterBuilder = {
    this.name = name
    this
  }

  def withAdapterType(adapterType: AdapterType): KafkaAdapterBuilder = {
    this.adapterType = adapterType
    // If the classname hasn't already been set, it should default to one of two classes based on the adapterType given.
    if(className == "" || className == null) {
      className = if (adapterType.name.toLowerCase == "input") "com.ligadata.kafkaInputOutputAdapters_v10.KamanjaKafkaConsumer$" else "com.ligadata.kafkaInputOutputAdapters_v10.KafkaProducer$"
    }
    this
  }

  def withAssociatedMessage(msg: String): KafkaAdapterBuilder = {
    this.associatedMessage = msg
    this
  }

  def withKeyValueDelimiter(delim: String): KafkaAdapterBuilder = {
    this.keyValueDelimiter = delim
    this
  }

  def withFieldDelimiter(delim: String): KafkaAdapterBuilder = {
    this.fieldDelimiter = delim
    this
  }

  def withValueDelimiter(delim: String): KafkaAdapterBuilder = {
    this.valueDelimiter = delim
    this
  }

  def withClassName(className: String): KafkaAdapterBuilder = {
    this.className = className
    this
  }

  def withJarName(jarName: String): KafkaAdapterBuilder = {
    this.jarName = jarName
    this
  }

  def withDependencyJar(jar: String): KafkaAdapterBuilder = {
    this.dependencyJars :+= jar
    this
  }

  def withDependencyJars(jars: List[String]): KafkaAdapterBuilder = {
    this.dependencyJars ++= jars
    this
  }

  def withAdapterSpecificConfig(config: KafkaAdapterSpecificConfig): KafkaAdapterBuilder = {
    this.adapterSpecificConfig = config
    this
  }

  def withTenantId(tenantId: String): KafkaAdapterBuilder = {
    this.tenantId = tenantId
    this
  }

  def build(): KafkaAdapterConfig = {
    try {
      new KafkaAdapterConfig(name, adapterType, associatedMessage, keyValueDelimiter, fieldDelimiter, valueDelimiter, className, jarName, dependencyJars, adapterSpecificConfig, tenantId)
    }
    catch {
      case e: Exception => throw new KamanjaConfigurationException("[Embedded Kafka]: Failed to create KafkaAdapterConfig", e)
    }
  }
}