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

import com.ligadata.test.utils.TestUtils

case class EnvironmentContextConfig(className: String = "com.ligadata.SimpleEnvContextImpl.SimpleEnvContextImpl$",
                                    jarName: String = s"KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
                                    dependencyJars: List[String] = List(s"""ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar""", s"ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar")) {
  override def toString = {
    s""""EnvironmentContext":""" +
      s"""{"classname": "$className",""" +
      s""""jarname": "$jarName",""" +
      s""""dependencyjars": [ "${dependencyJars.mkString("\", \"")}" ]}"""
  }
}
