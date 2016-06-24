/*
 * Copyright 2016 ligaDATA
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
package com.ligadata.jython.test

import java.io.File
import java.lang.reflect.Method

import com.ligadata.jython._
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager

import org.scalatest.{BeforeAndAfter, FunSuite}

import org.python.core.Py
import org.python.core.PyObject
import org.python.core.PyString
import org.python.util.PythonInterpreter
import com.ligadata.kamanja.metadata.ModelDef
import com.ligadata.KamanjaBase._

//import com.ligadata.kamanja.samples.messages._
import java.util.Properties
import com.ligadata.runtime.Log
import java.net.{URL, URLClassLoader}
import org.python.core.PySystemState
import org.python.core.packagecache.PackageManager

import com.ligadata.KamanjaBase.ContainerOrConcept
/**
  *
  */
class TestJython1 extends FunSuite with BeforeAndAfter {

  val logger = new com.ligadata.runtime.Log(this.getClass.getName())

  // Simple jtm
  test("test1") {
    val code =
      """
        |#
        |# Copyright 2016 ligaDATA
        |#
        |# Licensed under the Apache License, Version 2.0 (the "License");
        |# you may not use this file except in compliance with the License.
        |# You may obtain a copy of the License at
        |#
        |#     http://www.apache.org/licenses/LICENSE-2.0
        |#
        |# Unless required by applicable law or agreed to in writing, software
        |# distributed under the License is distributed on an "AS IS" BASIS,
        |# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        |# See the License for the specific language governing permissions and
        |# limitations under the License.
        |#
        |import sys
      """.stripMargin

    val cp = "".split(':')
    val cp1 = "".split(':')

    def urlses(cl: ClassLoader): Array[java.net.URL] = cl match {
      case null => Array()
      case u: java.net.URLClassLoader => u.getURLs() ++ urlses(cl.getParent)
      case _ => urlses(cl.getParent)
    }

    class PythonInterpreterClassLoader(url: Array[URL], parent : ClassLoader) extends URLClassLoader(url, parent) {
    }

    try {

      var cl1 = new PythonInterpreterClassLoader(cp.map(c => new File(c).toURI().toURL), this.getClass.getClassLoader)

      val urls2 = urlses(cl1)
      logger.Info("CLASSPATH-JYTHON-1:=" + urls2.mkString(":") +  "\n\n")

      var props: Properties = new Properties();
      props.put("python.home", "/home/joerg/bin/jython/Lib")
      props.put("python.console.encoding", "UTF-8")
      props.put("python.security.respectJavaAccessibility", "false")
      props.put("python.import.site", "false")
      var preprops: Properties = System.getProperties()

      PythonInterpreter.initialize(preprops, props, Array.empty[String])

      val interpreter1 = cl1.loadClass("org.python.util.PythonInterpreter").newInstance()

      val interpreter = interpreter1.asInstanceOf[org.python.util.PythonInterpreter]

      {
        val cl2 = interpreter.getClass.getClassLoader
        val urls2 = urlses(cl2)
        logger.Info("CLASSPATH-JYTHON-2:=" + urls2.mkString(":") +  "\n\n")
      }

      //val modelObject: PyObject =
      interpreter.exec(code)

    } catch {
      case e: Exception =>  println(e.toString)
        throw e
    }

  }
}
