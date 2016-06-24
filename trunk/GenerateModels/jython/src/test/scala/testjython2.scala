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
class TestJython2 extends FunSuite with BeforeAndAfter {

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
        |from com.ligadata.runtime import Log
        |from com.ligadata.KamanjaBase import ContainerOrConcept
        |from com.ligadata.kamanja.samples.messages import msg1
        |from com.ligadata.kamanja.samples.messages import outmsg1
        |
        |class Model():
        |   def __init__(self):
        |       self.logger = Log("Model")
        |       self.logger.Info('Model.__init__')
        |
        |   def execute(self, txnCtxt, execMsgsSet, matchedInputSetIndex, outputDefault):
        |       self.logger.Info('Model.execute')
        |       inMsg = execMsgsSet[0]
        |       self.logger.Info('jython>>> 21')
        |       if inMsg.id()!=111:
        |           self.logger.Info('jython>>> 22')
        |           v = inMsg.in1()
        |           self.logger.Info('Early exit')
        |           return None
        |
        |       self.logger.Info('jython>>> 23')
        |       output = outmsg1.createInstance()
        |       self.logger.Info('jython>>> 24')
        |       output.set(0, inMsg.id())
        |       self.logger.Info('jython>>> 25')
        |       output.set(1, inMsg.name())
        |       self.logger.Info('jython>>> 26')
        |
        |       return output
      """.stripMargin

    val cp_application="/home/joerg/app2/Kamanja-1.4.1_2.10/lib/application/com.ligadata.kamanja.samples.messages_msg1_1000000_1465412866388.jar:/home/joerg/app2/Kamanja-1.4.1_2.10/lib/application/com.ligadata.kamanja.samples.messages_msg1.jar:/home/joerg/app2/Kamanja-1.4.1_2.10/lib/application/com.ligadata.kamanja.samples.messages_outmsg1_1000000_1465412910148.jar:/home/joerg/app2/Kamanja-1.4.1_2.10/lib/application/com.ligadata.kamanja.samples.messages_outmsg1.jar".split(':')

    val cp = "".split(':')

    def urlses(cl: ClassLoader): Array[java.net.URL] = cl match {
      case null => Array()
      case u: java.net.URLClassLoader => u.getURLs() // ++ urlses(cl.getParent)
      case _ => urlses(cl.getParent)
    }

    class PythonInterpreterClassLoader(url: Array[URL], parent : ClassLoader) extends URLClassLoader(url, parent) {
    }

    try {

      var classLoader: URLClassLoader = this.getClass.getClassLoader.asInstanceOf[URLClassLoader]
      var method: Method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL]);
      method.setAccessible(true);
      var cl1 = cp_application.map(c => method.invoke(classLoader, new File(c).toURI().toURL))

      var props: Properties = new Properties();
      props.put("python.home", "/home/joerg/bin/jython/Lib")
      props.put("python.console.encoding", "UTF-8")
      props.put("python.security.respectJavaAccessibility", "false")
      props.put("python.import.site", "false")

      var preprops: Properties = System.getProperties()

      PythonInterpreter.initialize(preprops, props, Array.empty[String])

      val interpreter = new org.python.util.PythonInterpreter

      {
        val cl2 = interpreter.getClass.getClassLoader
        val urls2 = urlses(cl2)
        logger.Info("CLASSPATH-JYTHON-2:=" + urls2.mkString(":"))
      }

      // val modelObject: PyObject =
      interpreter.exec(code)
    } catch {
      case e: Exception =>  println(e.toString)
        throw e
    }
  }
}
/*
    logger.Info("<<< 12")
    //interpreter.exec("import Model")
    logger.Info("<<< 13")
    //val modelClass = interpreter.get("Model")
    logger.Info("<<< 14")
    //val modelObject: PyObject = modelClass.__call__()
    logger.Info("<<< 15")
 */
