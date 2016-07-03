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

/* Translates a jtm (json) file(s) into scala classes
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

  val template = FileUtils.readFileToString( new File(getClass.getResource("JythonTemplate.scala.txt").getPath), null:String)

  // Generate code
  var code = ""

  private def ModelVersionLong: Long = {
    //MdMgr.ConvertVersionToLong(MdMgr.FormatVersion(root.header.version))
    0
  }

  private def PackageName(): String = {
    //root.header.namespace.trim + ".V" + ModelVersionLong
    ""
  }

  private def ModelName(): String = {
    "Model"
  }

  private def ModelNamespace(): String = {
    ""
  }

  private def FactoryName(): String = {
    "ModelFactory"
  }

  def Code() : String = {
    if(code==null)
      throw new Exception("No code was successful created")

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

  // Controls the code generation
  def Execute(): String = {


    // Process the imports
    //
    var subtitutions = new Substitution

    // Only output generation stamp for production environments
    if(!suppressTimestamps) {
      subtitutions.Add("model.timestamp", Stamp.Generate(this.getClass).mkString("\n"))
    } else {
      subtitutions.Add("model.timestamp", "<supressed>")
    }

    // Namespace
    //
    subtitutions.Add("model.packagename", "package %s\n".format(PackageName))

    //subtitutions.Add("model.name", "%s.%s".format(root.header.namespace, ModelName))
   // subtitutions.Add("model.version", root.header.version)
    subtitutions.Add("factoryclass.name", FactoryName)
    subtitutions.Add("modelclass.name", ModelName)

    //

    if(outputFile!=null && outputFile.nonEmpty) {
      logger.trace("Output to file {}", outputFile)
      FileUtils.writeStringToFile(new File(outputFile), code)
    }

    code
  }
}
