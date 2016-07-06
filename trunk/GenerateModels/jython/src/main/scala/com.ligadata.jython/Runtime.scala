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
package com.ligadata.jython

import com.ligadata.Utils._
import com.ligadata.kamanja.metadata.{MdMgr, ModelDef, BaseElem}
import com.ligadata.kamanja.metadataload.MetadataLoad
import com.ligadata.msgcompiler._
import org.aicer.grok.dictionary.GrokDictionary
import org.apache.logging.log4j.{LogManager, Logger}
import org.json4s.jackson.JsonMethods._
import org.rogach.scallop._
import org.apache.commons.io.FileUtils
import java.io.{StringReader, File}
import com.ligadata.runtime.Stamp
import com.ligadata.runtime.Substitution

import scala.collection.mutable.ArrayBuffer

import org.python.core.Py
import org.python.core.PyObject
import org.python.core.PyString
import org.python.core.PySystemState
import org.python.util.PythonInterpreter
import java.util.Properties
import com.ligadata.KamanjaBase._
import java.util.Properties
import java.lang.ClassLoader

/**
  * An implementation of the FactoryOfModelInstanceFactory trait that supports all of the Jython models.
  *
  * copy of JpmmlFactoryOfModelInstanceFactory
  */

object Runtime extends FactoryOfModelInstanceFactory {

  private[this] val loggerName = this.getClass.getName
  private[this] val LOG = LogManager.getLogger(loggerName)

  /**
    * As part of the creation of the model instance factory, see to it that there any jars that it needs (either its
    * own or those upon which it depends) are loaded.
    *
    * @param metadataLoader the engine's custom loader to use if/when instances of the model and the dependent jars are to be loaded
    * @param jarPaths  a Set of Paths that contain the dependency jars required by this factory instance to be created
    * @param elem a BaseElem (actually the ModelDef in this case) with an implementation jar and possible dependent jars
    * @return true if all the jar loading was successful.
    */
  private[this] def LoadJarIfNeeded(metadataLoader: KamanjaLoaderInfo, jarPaths: collection.immutable.Set[String], elem: BaseElem): Boolean = {
    val allJars = GetAllJarsFromElem(jarPaths, elem)
    val loadAcceptable : Boolean = if (allJars.nonEmpty) {
      Utils.LoadJars(allJars.toArray, metadataLoader.loadedJars, metadataLoader.loader)
    } else {
      true
    }
    loadAcceptable
  }

  /**
    * Answer a set of jars that contain the implementation jar and its dependent jars for the supplied BaseElem.
    * The list is checked for valid jar paths before returning.
    *
    * @param jarPaths where jars are located in the cluster node.
    * @param elem the model definition that has a jar implementation and a list of dependency jars
    * @return an Array[String] containing the valid jar paths for the supplied element.
    */
  private[this] def GetAllJarsFromElem(jarPaths: collection.immutable.Set[String], elem: BaseElem): Set[String] = {
    var allJars: Array[String] = null

    val jarname = if (elem.JarName == null) "" else elem.JarName.trim

    if (elem.DependencyJarNames != null && elem.DependencyJarNames.nonEmpty && jarname.nonEmpty) {
      allJars = elem.DependencyJarNames :+ jarname
    } else if (elem.DependencyJarNames != null && elem.DependencyJarNames.nonEmpty) {
      allJars = elem.DependencyJarNames
    } else if (jarname.nonEmpty) {
      allJars = Array(jarname)
    } else {
      return Set[String]()
    }

    allJars.map(j => Utils.GetValidJarFile(jarPaths, j)).toSet
  }

  /**
    * Instantiate a model instance factory for the supplied ''modelDef''.  The model instance factory can instantiate
    * instances of the model described by the ModelDef.
    *
    * @param modelDef the metadatata object for which a model factory instance will be created.
    * @param nodeContext the NodeContext that provides access to model state and kamanja engine services.
    * @param loaderInfo the engine's custom loader to use if/when instances of the model and the dependent jars are to be loaded
    * @param jarPaths a Set of Paths that contain the dependency jars required by this factory instance to be created
    * @return a ModelInstanceFactory or null if bogus information has been supplied.
    *
    * Fixme: Passing the jarPaths as a Set suggests that there is no search order in the path.  Should we have an ordered list
    * instead to allow for alternate implementations where the lib/application directory is first followed by the lib/system
    * directory?  This could be fruitfully used to drop in a fixed implementation ahead of a broken one... this all assumes
    * there is a command to force reload of jars for a given model.
    */
  override def getModelInstanceFactory(modelDef: ModelDef
                                       , nodeContext: NodeContext
                                       , loaderInfo: KamanjaLoaderInfo
                                       , jarPaths: collection.immutable.Set[String]): ModelInstanceFactory = {

    LoadJarIfNeeded(loaderInfo, jarPaths, modelDef)

    val isReasonable : Boolean = (modelDef != null && modelDef.FullNameWithVer != null && modelDef.FullNameWithVer.nonEmpty)
    val mdlInstanceFactory : ModelInstanceFactory = if (isReasonable) {
      val factory: RuntimeFactory = new RuntimeFactory(modelDef, nodeContext)

      if (factory == null) {
        LOG.error(s"Failed to instantiate ModelInstanceFactory... name = $modelDef.FullNameWithVer")
      }
      factory
    }  else {
      null
    }

    mdlInstanceFactory
  }

  /**
    * Answer a model definition for the supplied model string, input message, output message and jarPaths.
    *
    * NOTE: Currently not used.
    *
    * @param nodeContext the NodeContext that provides access to model state and kamanja engine services.
    * @param modelString the model source (for those models that supply source)
    * @param inputMessage the namespace.name.version of the input message this model consumes
    * @param outputMessage the namespace.name.version of the output message this model produces (if any)
    * @param loaderInfo the engine's custom loader to use if/when instances of the model and the dependent jars are to be loaded
    * @param jarPaths a Set of Paths that contain the dependency jars required by this factory instance to be created
    * @return a ModelDef instance
    */
  override def prepareModel(nodeContext: NodeContext
                            , modelString: String
                            , inputMessage: String
                            , outputMessage: String
                            , loaderInfo: KamanjaLoaderInfo
                            , jarPaths: collection.immutable.Set[String]): ModelDef = {
    null
  }
}

/** Factory of jython objects
  *
  */
class RuntimeFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {

  val logger = LogManager.getLogger(this.getModelName())

  /** Split a fully qualified object name into namespace and class
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

  val inputPythonData = modelDef.objectDefinition
  val classLoader: ClassLoader = this.getClass.getClassLoader
  if(logger.isDebugEnabled)
  {
    val urls2 = urlses(classLoader)
    logger.debug("CLASSPATH_JYTHON=" + urls2.mkString(":"))
  }

  var preprops: Properties = System.getProperties()

  var props: Properties = new Properties()
  props.put("python.console.encoding", "UTF-8")
  props.put("python.security.respectJavaAccessibility", "false")
  props.put("python.import.site", "false")

  PySystemState.initialize(preprops, props, Array.empty[String], classLoader)

  val interpreter = new PythonInterpreter

  val modelClass: PyObject = try {

    val mgr: MdMgr = MdMgr.GetMdMgr
    val inputPythonDataAdjusted = Generator.PythonReplaceModuleVersions(mgr, modelDef, inputPythonData)
    interpreter.exec(inputPythonDataAdjusted)
    interpreter.get(modelDef.Name)

  } catch {
    case e: Exception => logger.error(e.toString)
      throw e
  }

  override def getModelName(): String = {
    val name : String = if (getModelDef() != null) {
      getModelDef().FullName
    } else {
      val msg : String =  "JythonAdapterFactory: model has no name and no version"
      logger.error(msg)
      msg
    }
    name
  }

  override def getVersion(): String = {
    val withDots: Boolean = true
    if (modelDef != null) {
      MdMgr.ConvertLongVersionToString(modelDef.Version, withDots)
    } else {
      if (withDots) "000000.000001.000000" else "000000000001000000"
    }
  }

  override def createModelInstance(txnCtxt: TransactionContext): ModelInstance = {
    return new Runtime(this, modelClass.__call__())
  }
}

class Runtime(factory: ModelInstanceFactory, modelObject: PyObject) extends ModelInstance(factory) {

  val logger = LogManager.getLogger(this.getModelName())

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
      case e: Exception =>  logger.error(e.toString)
        throw e
    }
  }
}
