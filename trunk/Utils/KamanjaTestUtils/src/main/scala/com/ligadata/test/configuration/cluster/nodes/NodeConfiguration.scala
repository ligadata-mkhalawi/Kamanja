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

package com.ligadata.test.configuration.cluster.nodes

case class NodeConfiguration(nodeId: String,
                             nodePort: Int,
                             nodeIpAddr: String,
                             jarPaths: Array[String],
                             scalaHome:String,
                             javaHome:String,
                             classPath:String
                 ) {
  override def toString: String = {
    val builder = new StringBuilder
    builder.append("{\n")
    builder.append(s"""  "NodeId": "$nodeId",""")
    builder.append(s"""  "NodePort": $nodePort,""")
    builder.append(s"""  "NodeIpAddr": "$nodeIpAddr",""")
    builder.append(s"""  "JarPaths": [ """)
    for(i <- 0 to jarPaths.length - 1) {
      if(i == jarPaths.length - 1)
        builder.append(s""""${jarPaths(i)}"""")
      else
        builder.append(s""""${jarPaths(i)}",""")
    }
    builder.append("],\n")
    builder.append(s"""   "Scala_home": "${scalaHome}",""")
    builder.append(s"""   "Java_home": "${javaHome}",""")
    builder.append("      \"Roles\": [\n")
    builder.append(s"""      "RestAPI",""" + "\n")
    builder.append(s"""      "ProcessingEngine"""" + "\n")
    builder.append("],\n")
    builder.append(s"""   "Classpath": "${classPath}"""")
    builder.append(s"""}""")
    builder.toString()
  }
}

class NodeBuilder {
  private var nodeId: String = _
  private var nodePort: Int = _
  private var nodeIpAddr: String = _
  private var jarPaths: Array[String] = _
  private var scalaHome: String = _
  private var javaHome: String = _
  private var classPath: String = _

  def withNodeId(nodeId: String): NodeBuilder = {
    this.nodeId = nodeId
    this
  }

  def withNodePort(port: Int): NodeBuilder = {
    this.nodePort = port
    this
  }

  def withNodeIpAddr(ip: String): NodeBuilder = {
    this.nodeIpAddr = ip
    this
  }

  def withJarPaths(paths: Array[String]): NodeBuilder = {
    this.jarPaths = paths
    this
  }

  def withScalaHome(home: String): NodeBuilder = {
    this.scalaHome = home
    this
  }

  def withJavaHome(home: String): NodeBuilder = {
    this.javaHome = home
    this
  }

  def withClassPath(classPath: String): NodeBuilder = {
    this.classPath = classPath
    this
  }

  def build: NodeConfiguration = {
    return new NodeConfiguration(nodeId, nodePort, nodeIpAddr, jarPaths, scalaHome, javaHome, classPath)
  }
}