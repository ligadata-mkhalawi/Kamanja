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
package com.ligadata.samples.models

import org.python.core.Py
import org.python.core.PyObject
import org.python.core.PyString
import org.python.util.PythonInterpreter
import com.ligadata.kamanja.metadata.{MdMgr, ModelDef}
import com.ligadata.KamanjaBase._
import com.ligadata.kamanja.samples.messages._
import java.util.Properties
import com.ligadata.runtime.Log
import org.python.core.PySystemState

class HelloWorldJythonFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {

  override def getModelName(): String = {
    "com.ligadata.kamanja.samples.models.HelloWorldJythonModel" // ToDo: get from metadata
  }

  override def getVersion(): String = {
    "0.0.8" // ToDo: get from metadata
  }

  override def createModelInstance(): ModelInstance = {
    return new HelloWorldJythonModel(this)
  }
}

class HelloWorldJythonModel(factory: ModelInstanceFactory) extends ModelInstance(factory) {

  /** Split a fully qualified object name into namspace and class
    *
    * @param name is a fully qualified class name
    * @return tuple with namespace and class name
    */
  def splitNamespaceClass(name: String): (String, String) = {
    val elements = name.split('.')
    (elements.dropRight(1).mkString("."), elements.last)
  }

  def urlses(cl: ClassLoader): Array[java.net.URL] = cl match {
    case null => Array()
    case u: java.net.URLClassLoader => u.getURLs() ++ urlses(cl.getParent)
    case _ => urlses(cl.getParent)
  }

  val logger = new Log("com.ligadata.samples.models.HelloWorldJythonModel")

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
      |from com.ligadata.runtime import Log
      |
      |class Model():
      |    def __init__(self):
      |        self.logger = Log('Model')
      |        self.logger.Info('Model.__init__')
      |
      |    def execute(self, txnCtxt, execMsgsSet, matchedInputSetIndex, outputDefault):
      |        self.logger.Info('Model.execute')
      |        inMsg = execMsgsSet[0]
      |        if inMsg.score()!=1:
      |            return None
      |
      |        output=[]
      |        output[0] = outmsg1.createInstance()
      |        output[0].set(0, inMsg.id())
      |        output[0].set(1, inMsg.name())
      |        return output
    """.stripMargin

  var props: Properties = new Properties()

  props.put("python.console.encoding", "UTF-8")
  props.put("python.security.respectJavaAccessibility", "false")
  props.put("python.import.site", "false")

  var preprops: Properties = System.getProperties()

  PySystemState.initialize(preprops, props, Array.empty[String], getModelInstanceFactory().getClass.getClassLoader)

  {
    val urls2 = urlses(getModelInstanceFactory().getClass.getClassLoader)
    logger.Debug("CLASSPATH-JYTHON-1:=" + urls2.mkString(":"))
  }

  val interpreter = new org.python.util.PythonInterpreter

  val mgr : MdMgr = MdMgr.GetMdMgr

  val modelObject: PyObject = try {

  // Go through all possible output messages and addt them as imports incl. version
  val ouputmessages = getModelInstanceFactory().getModelDef().outputMsgs

  // Load output messages into the interpreter
  ouputmessages.foreach( m => {
    val classname = m
    val classMd = mgr.Message(classname, 0, true)
    if(classMd.isEmpty) {
      throw new Exception("Metadata: unable to find class %s".format(classname))
    }
    val name: String = classMd.get.physicalName
    val (packagename, class1) = splitNamespaceClass(name)
    val importCommand = "from %s import %s".format(packagename, class1)
    logger.Debug(importCommand)
    interpreter.exec(importCommand)
  })

    // Load the code
    interpreter.exec(code)

    // Create the model class
    val modelClass = interpreter.get("Model")

    // Create the model object form the class
    modelClass.__call__()
  } catch {
    case e: Exception => logger.Error(e.toString)
      throw e
  }

  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {

    try {
      val execMsgsSetPy: Array[PyObject] = execMsgsSet.map(m => Py.java2py(m))
      val args: Array[PyObject] = Array[PyObject](Py.java2py(txnCtxt), Py.java2py(execMsgsSetPy), Py.java2py(triggerdSetIndex), Py.java2py(outputDefault))
      val result: Array[PyObject] = modelObject.invoke("execute", args).__tojava__(classOf[Object]).asInstanceOf[Array[PyObject]]

      if(result==null)
        return null

      val outmsgs: Array[ContainerOrConcept] = result.map( m => m.__tojava__(classOf[ContainerOrConcept]).asInstanceOf[ContainerOrConcept])
      outmsgs
    } catch {
      case e: Exception =>  logger.Error(e.toString)
        throw e
    }
  }

}
