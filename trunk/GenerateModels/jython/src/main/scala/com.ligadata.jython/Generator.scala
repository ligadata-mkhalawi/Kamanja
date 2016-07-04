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

import com.ligadata.kamanja.metadata._
import com.ligadata.kamanja.metadataload.MetadataLoad
import com.ligadata.msgcompiler._
import org.aicer.grok.dictionary.GrokDictionary
import org.apache.logging.log4j.{ Logger, LogManager }
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

object jythonGlobalLogger {
  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)
}

trait LogTrait {
  val logger = jythonGlobalLogger.logger
}

object GeneratorBuilder {
  def create() = { new GeneratorBuilder }
}

/** Class to collect all the parameter to build a compiler instance
  *
  */
class GeneratorBuilder {

  def setSuppressTimestamps(switch: Boolean = true) = { suppressTimestamps = switch; this }
  def setInputFile(filename: String) = { inputFile = filename; this }
  def setInputPythonString(jsonData: String) = { inputPythonData = jsonData; this }
  def setOutputFile(filename: String) = { outputFile = filename; this }
  def setMetadataLocation(filename: String) = { metadataLocation = filename; this }
  def setMetadata(md: MdMgr) = { metadataMgr = md; this }
  def setModelDef(md: ModelDef) = { modelDef = md; this }
  def setPhyton(jtm: String) = { pythonData = jtm; this }

  var pythonData: String = null
  var inputFile: String = null
  var outputFile: String = null
  var metadataLocation: String = null
  var suppressTimestamps: Boolean = false
  var metadataMgr: MdMgr = null
  var modelDef: ModelDef = null
  var inputPythonData: String = null

  def build() : Generator = {
    new Generator(this)
  }
}

/* Helper functions
 *
 */
object Generator extends LogTrait {
  def PythonReplaceModuleVersions(md: MdMgr, modelDef: ModelDef, pythonCode: String) : String = {

    var code = pythonCode

    val inputMessages = if(modelDef.inputMsgSets!=null) {
      modelDef.inputMsgSets.foldLeft(Array.empty[String])((a, e1) => {
        e1.foldLeft(a)((a, e1) => {
          a :+ e1.message
        })
      })
    } else {
      Array.empty[String]
    }

    val outputMessages = if(modelDef.outputMsgs!=null) {
      modelDef.outputMsgs
    } else {
      Array.empty[String]
    }

    val message = outputMessages ++ inputMessages

    // Subsitute version in the code
    //import module - can't be patched, you would  need to use the fully qualified name include the version
    //import module as alias - can be patched
    //from module import submodule - can be patched
    //from .. import module - deprecated and can't be pacthed
    //
    // Load output messages into the interpreter
    message.foreach( m => {
      val classname = m
      val classMd = md.Message(classname, 0, true)
      if(classMd.isEmpty) {
        throw new Exception("Metadata: unable to find class %s".format(classname))
      }
      val name: String = classMd.get.physicalName
      val packagenameUnversioned = classMd.get.NameSpace
      val (packagename, class1) = splitNamespaceClass(name)

      logger.info(s"Process import: $name")

      code = code.replaceAll(s"import\\s+$packagenameUnversioned\\.$class1\\s+as", s"import $name as")
      code = code.replaceAll(s"import\\s+$packagenameUnversioned\\.[Vv][\\d.]+.$class1\\s+as", s"import $name as")

      code = code.replaceAll(s"import\\s+$packagenameUnversioned\\s+as", s"import $packagename as")
      code = code.replaceAll(s"import\\s+$packagenameUnversioned\\.[Vv][\\d.]+\\s+as", s"import $packagename as")

      code = code.replaceAll(s"from\\s++$packagenameUnversioned\\.[Vv][\\d.]+\\s+import", "from %s import".format(packagename))
      code = code.replaceAll(s"from\\s++$packagenameUnversioned\\s+import", "from %s import".format(packagename))
    })

    code
  }

  /** Split a fully qualified object name into namspace and class
    *
    * @param name is a fully qualified class name
    * @return tuple with namespace and class name
    */
  def splitNamespaceClass(name: String): (String, String) = {
    val elements = name.split('.')
    (elements.dropRight(1).mkString("."), elements.last)
  }

  /** Escape string as literal
    *
    * @param raw
    * @return
    */
  def escape(raw: String): String = {
    import scala.reflect.runtime.universe._
    Literal(Constant(raw)).toString
  }

}

/* Generates a jython (python) file into scala classes
 *
 */
class Generator(params: GeneratorBuilder) extends LogTrait {

  /** Initialize from parameter block
    *
    */
  val md = if(params.metadataMgr==null) {
    com.ligadata.runtime.loadMetadata(params.metadataLocation) // Load metadata if not passed in
  } else {
    params.metadataMgr
  }

  val modelDef = params.modelDef
  val suppressTimestamps: Boolean = params.suppressTimestamps // Suppress timestamps

  val inputFile: String = params.inputFile // Input file to compile
  val outputFile: String = params.outputFile // Output file to write
  val inputPythonData: String  = if (params.inputPythonData != null) {
      params.inputPythonData // Process given Json
    } else if (inputFile != null) {
      FileUtils.readFileToString(new File(inputFile), null:String)
    } else {
      throw new Exception("Input not found")
    }

  try {

    // Syntax check for python code
    var props: Properties = new Properties()

    props.put("python.console.encoding", "UTF-8")
    props.put("python.security.respectJavaAccessibility", "false")
    props.put("python.import.site", "false")

    var preprops: Properties = System.getProperties()

    PySystemState.initialize(preprops, props, Array.empty[String], this.getClass.getClassLoader)
    val interpreter = new PythonInterpreter

    interpreter.compile(inputPythonData)

  } catch {
    case e: Exception =>  logger.error(e.toString)
      throw e
  }

  val template = FileUtils.readFileToString( new File(getClass.getResource("/JythonTemplate.scala.txt").getPath), null:String)

  // Generate code
  var code = ""

  def Code() : String = {
    if(code==null)
      throw new Exception("No code was successful created")

    code
  }

  // Controls the code generation
  def Execute(): String = {

    // Process the imports
    //
    var subtitutions = new Substitution

    // Only output generation stamp for production environments
    if(!suppressTimestamps) {
      subtitutions.Add("model.timestamp", Stamp.Generate(this.getClass).mkString("\n"))
    } else {
      subtitutions.Add("model.timestamp", "// <supressed>")
    }

    // Gather information from the model
    //
    val name = modelDef.typeString
    subtitutions.Add("model.name",  "%s.%s".format(modelDef.NameSpace, modelDef.Name))
    subtitutions.Add("model.version", MdMgr.ConvertLongVersionToString(modelDef.ver, true))

    val (packagename, modelname) = Generator.splitNamespaceClass(name)
    subtitutions.Add("model.packagename", "package %s\n".format(packagename))

    // Replace all imports with proper versions before adding to substiution map
    val inputPythonDataAdjusted = Generator.PythonReplaceModuleVersions(md, modelDef, inputPythonData)
    // Check for doc strings, we do not handle it currently
    if(inputPythonDataAdjusted.indexOf("\"\"\"") >= 0) {
      throw new Exception("Docstring format is not allowd for jython models") // Create a specific exception
    }
    subtitutions.Add("model.jythoncode", inputPythonDataAdjusted)

    subtitutions.Add("model.factoryclassname", modelname)
    subtitutions.Add("model.classname", modelDef.Name)

    code = subtitutions.Replace(template)

    if(outputFile!=null && outputFile.nonEmpty) {
      logger.trace("Output to file {}", outputFile)
      FileUtils.writeStringToFile(new File(outputFile), code)
    }

    code
  }
}
