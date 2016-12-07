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

package com.ligadata.MetadataAPI

import java.util.{Date, Properties}

import com.ligadata.AuditAdapterInfo.AuditAdapter
import com.ligadata.MetadataAPI.MetadataAPI.ModelType
import com.ligadata.StorageBase.DataStore
import com.ligadata.kamanja.metadata.{BaseElemDef, MdMgr, MessageDef, ModelDef}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import com.ligadata.kamanja.metadata.MdMgr._

case class ZkNotification(ObjectType: String, Operation: String, NameSpace: String, Name: String, Version: String, PhysicalName: String, JarName: String, DependantJars: List[String], ConfigContnent: Option[String])
case class ZkTransaction(Notifications: List[ZkNotification], transactionId: Option[String])

/** A class that defines the result any of the API function uniformly
 * @constructor creates a new ApiResult with a statusCode,functionName,statusDescription,resultData
 * @param statusCode status of the API call, 0 => success, non-zero => failure.
 * @param description relevant in case of non-zero status code
 * @param resultData A string value representing string, either XML or JSON format
 */
class ApiResult(var statusCode:Int, var functionName: String, var resultData: String, var description: String){
/**
 * Override toString to return ApiResult as a String
 */
  override def toString: String = {

    val json = ("APIResults" -> ("Status Code" -> statusCode) ~
                                ("Function Name" -> functionName) ~
                                ("Result Data"  -> resultData) ~
                                ("Result Description" -> description))

    pretty(render(json))
  }
}

object MetadataAPI {

  object ModelType extends Enumeration {
      type ModelType = Value
      val JAVA = Value("java")
      val SCALA = Value("scala")
      val KPMML = Value("kpmml")
      val PMML = Value("pmml")
      val PYTHON = Value("python") 
      val JYTHON = Value("jython") 
      val JTM = Value("jtm")
      val BINARY = Value("binary")
      val UNKNOWN = Value("unknown")

      def fromString(typstr : String) : ModelType = {
          val typ : ModelType.Value = typstr.toLowerCase match {
              case "java" => JAVA
              case "scala" => SCALA
              case "pmml" => PMML
              case "python" => PYTHON
              case "jython" => JYTHON
              case "kpmml" => KPMML
              case "jtm" => JTM
              case "binary" => BINARY
              case _ => UNKNOWN
          }
          typ
      }
  }

  def getMetadataApiInterface() : MetadataAPI = {
  	return null;
  }

  def SerializeMapToJsonString(map: Map[String, Any]): String = {
    implicit val formats = org.json4s.DefaultFormats
    Serialization.write(map)
  }

}

/**
 * A class that defines the result any of the API function - The ResultData reuturns a Complex Array of Values.
 * @param statusCode
 * @param functionName
 * @param resultData
 * @param description
 */
class ApiResultComplex (var statusCode:Int, var functionName: String, var resultData: String, var description: String){
  /**
   * Override toString to return ApiResult as a String
   */
  override def toString: String = {

    val resultArray = parse(resultData).values.asInstanceOf[List[Map[String,Any]]]

    var json = ("APIResults" -> ("Status Code" -> statusCode) ~
                                ("Function Name" -> functionName) ~
                                ("Result Data"  -> resultArray.map {nodeInfo => MetadataAPI.SerializeMapToJsonString(nodeInfo)}) ~
                                ("Result Description" -> description))

    pretty(render(json))
  }
}


// 646 - 674 Change begins

trait MetadataAPI {
  import ModelType._
  /** MetadataAPI defines the CRUD (create, read, update, delete) operations on metadata objects supported
   * by this system. The metadata objects includes Types, Functions, Concepts, Derived Concepts,
   * MessageDefinitions, Model Definitions. All functions take String values as input in XML or JSON Format
   * returns JSON string of ApiResult object.
   */

  /** Add new types
    * @param typesText an input String of types in a format defined by the next parameter formatType
    * @param formatType format of typesText ( JSON or XML)
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    *
    * Example:
    *
    * {{{
    * val sampleScalarTypeStr = """
    * {
    * "MetadataType" : "ScalarTypeDef",
    * "NameSpace" : "system",
    * "Name" : "my_char",
    * "TypeTypeName" : "tScalar",
    * "TypeNameSpace" : "System",
    * "TypeName" : "Char",
    * "PhysicalName" : "Char",
    * "Version" : 100,
    * "JarName" : "basetypes_2.10-0.1.0.jar",
    * "DependencyJars" : [ "metadata_2.10-1.0.jar" ],
    * "Implementation" : "com.ligadata.BaseTypes.CharImpl"
    * }
    * """
    * var apiResult = MetadataAPIImpl.AddType(sampleScalarTypeStr,"JSON")
    * var result = MetadataAPIImpl.getApiResult(apiResult)
    * println("Result as Json String => \n" + result._2)
    * }}}
    *
   */
  def AddType(typesText:String, formatType:String, userid: Option[String] = None): String

  /** Update existing types
    * @param typesText an input String of types in a format defined by the next parameter formatType
    * @param formatType format of typesText ( JSON or XML)
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    *
    * Example:
    *
    * {{{
    * val sampleScalarTypeStr = """
    * {
    * "MetadataType" : "ScalarTypeDef",
    * "NameSpace" : "system",
    * "Name" : "my_char",
    * "TypeTypeName" : "tScalar",
    * "TypeNameSpace" : "System",
    * "TypeName" : "Char",
    * "PhysicalName" : "Char",
    * "Version" : 101,
    * "JarName" : "basetypes_2.10-0.1.0.jar",
    * "DependencyJars" : [ "metadata_2.10-1.0.jar" ],
    * "Implementation" : "com.ligadata.BaseTypes.CharImpl"
    * }
    * """
    * var apiResult = MetadataAPIImpl.UpdateType(sampleScalarTypeStr,"JSON")
    * var result = MetadataAPIImpl.getApiResult(apiResult)
    * println("Result as Json String => \n" + result._2)
    * }}}
    *
    */
  def UpdateType(typesText:String, formatType:String, userid: Option[String] = None): String
  /** Remove Type for given typeName and version
    * @param typeName name of the Type
    * @version version of the Type
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    *
    * Example:
    *
    * {{{
    * val apiResult = MetadataAPIImpl.RemoveType(MdMgr.sysNS,"my_char",100)
    * val (statusCode,resultData) = MetadataAPIImpl.getApiResult(apiResult)
    * println("Result as Json String => \n" + resultData)
    * }}}
    *
    */
  def RemoveType(typeNameSpace:String, typeName:String, version:Long, userid: Option[String] = None): String

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  /** Upload Jars into system. Dependency jars may need to upload first. Once we upload the jar,
    * if we retry to upload it will throw an exception.
    * @param jarPath fullPathName of the jar file
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    *
   */
  def UploadJar(jarPath:String, userid: Option[String] = None): String

  /** Add new functions
    * @param functionsText an input String of functions in a format defined by the next parameter formatType
    * @param formatType format of functionsText ( JSON or XML)
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    *
    * Example:
    * {{{
    *   val sampleFunctionStr = """
    *  {
    *  "NameSpace" : "pmml",
    *  "Name" : "my_min",
    *  "PhysicalName" : "com.ligadata.pmml.udfs.Udfs.Min",
    *  "ReturnTypeNameSpace" : "system",
    *  "ReturnTypeName" : "double",
    *  "Arguments" : [ {
    *  "ArgName" : "expr1",
    *  "ArgTypeNameSpace" : "system",
    *  "ArgTypeName" : "int"
    *  }, {
    *  "ArgName" : "expr2",
    *  "ArgTypeNameSpace" : "system",
    *  "ArgTypeName" : "double"
    *  } ],
    *  "Version" : 1,
    *  "JarName" : null,
    *  "DependantJars" : [ "basetypes_2.10-0.1.0.jar", "metadata_2.10-1.0.jar" ]
    *  }
    *"""
    *    var apiResult = MetadataAPIImpl.AddFunction(sampleFunctionStr,"JSON")
    *    var result = MetadataAPIImpl.getApiResult(apiResult)
    *    println("Result as Json String => \n" + result._2)
    *}}}
    */
  def AddFunctions(functionsText:String, formatType:String, userid: Option[String] = None): String

  /** Update existing functions
    * @param functionsText an input String of functions in a format defined by the next parameter formatType
    * @param formatType format of functionsText ( JSON or XML)
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    *
    * Example:
    * {{{
    *   val sampleFunctionStr = """
    *  {
    *  "NameSpace" : "pmml",
    *  "Name" : "my_min",
    *  "PhysicalName" : "com.ligadata.pmml.udfs.Udfs.Min",
    *  "ReturnTypeNameSpace" : "system",
    *  "ReturnTypeName" : "double",
    *  "Arguments" : [ {
    *  "ArgName" : "expr1",
    *  "ArgTypeNameSpace" : "system",
    *  "ArgTypeName" : "int"
    *  }, {
    *  "ArgName" : "expr2",
    *  "ArgTypeNameSpace" : "system",
    *  "ArgTypeName" : "double"
    *  } ],
    *  "Version" : 1,
    *  "JarName" : null,
    *  "DependantJars" : [ "basetypes_2.10-0.1.0.jar", "metadata_2.10-1.0.jar" ]
    *  }
    *"""
    *    var apiResult = MetadataAPIImpl.UpdateFunction(sampleFunctionStr,"JSON")
    *    var result = MetadataAPIImpl.getApiResult(apiResult)
    *    println("Result as Json String => \n" + result._2)
    * }}}
    *
    */
  def UpdateFunctions(functionsText:String, formatType:String, userid: Option[String] = None): String

  /** Remove function for given FunctionName and Version
    * @param nameSpace the function's namespace
    * @param functionName name of the function
    * @version version of the function
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    *
    * Example:
    * {{{
    * val apiResult = MetadataAPIImpl.RemoveFunction(MdMgr.sysNS,"my_min",100)
    * val (statusCode,resultData) = MetadataAPIImpl.getApiResult(apiResult)
    * println("Result as Json String => \n" + resultData)
    *}}}
    *
    */
  def RemoveFunction(nameSpace:String, functionName:String, version:Long, userid: Option[String] = None): String

  /** Add new concepts
    * @param conceptsText an input String of concepts in a format defined by the next parameter formatType
    * @param formatType format of conceptsText ( JSON or XML)
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    *
    * Example:
    *
    * {{{
    *   val sampleConceptStr = """
    *  {"Concepts" : [
    *  "NameSpace":"Ligadata",
    *  "Name":"ProviderId",
    *  "TypeNameSpace":"System",
    *  "TypeName" : "String",
    *  "Version"  : 100 ]
    *  }
    *"""
    *    var apiResult = MetadataAPIImpl.AddConcepts(sampleConceptStr,"JSON")
    *    var result = MetadataAPIImpl.getApiResult(apiResult)
    *    println("Result as Json String => \n" + result._2)
    *}}}
    *
    */
  def AddConcepts(conceptsText:String, formatType:String, userid: Option[String] = None): String // Supported format is JSON/XML

  /** Update existing concepts
    * @param conceptsText an input String of concepts in a format defined by the next parameter formatType
    * @param formatType format of conceptsText ( JSON or XML)
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    *
    * Example:
    *
    * {{{
    *   val sampleConceptStr = """
    *  {"Concepts" : [
    *  "NameSpace":"Ligadata",
    *  "Name":"ProviderId",
    *  "TypeNameSpace":"System",
    *  "TypeName" : "String",
    *  "Version"  : 101 ]
    *  }
    *"""
    *    var apiResult = MetadataAPIImpl.UpdateConcepts(sampleConceptStr,"JSON")
    *    var result = MetadataAPIImpl.getApiResult(apiResult)
    *    println("Result as Json String => \n" + result._2)
    *
    *}}}
    *
    */
  def UpdateConcepts(conceptsText:String, formatType:String, userid: Option[String] = None): String

  /** RemoveConcepts take all concepts names to be removed as an Array
    * @param concepts array of Strings where each string is name of the concept
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    *
    * Example:
    * {{{
    * val apiResult = MetadataAPIImpl.RemoveConcepts(Array("Ligadata.ProviderId.100"))
    * val (statusCode,resultData) = MetadataAPIImpl.getApiResult(apiResult)
    * println("Result as Json String => \n" + resultData)
    *}}}
    *
    */
  def RemoveConcepts(concepts:Array[String], userid: Option[String] = None): String

  /** Add message given messageText
    *
    * @param messageText text of the message (as JSON/XML string as defined by next parameter formatType)
    * @param formatType format of messageText ( JSON or XML)
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    *
    * Example
    *
    * {{{
    * var apiResult = MetadataAPIImpl.AddMessage(msgStr,"JSON"))
    * var result = MetadataAPIImpl.getApiResult(apiResult)
    * println("Result as Json String => \n" + result._2)
    * }}}
    */
  def AddMessage(messageText:String, formatType:String, userid: Option[String] = None, tid: Option[String] = None, pStr: Option[String]): String

  /** Update message given messageText
    *
    * @param messageText text of the message (as JSON/XML string as defined by next parameter formatType)
    * @param formatType format of messageText (as JSON/XML string)
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param pStr  json string that contains extra parameters to be added to BaseElem (description, comment, tag)
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    */
  def UpdateMessage(messageText:String, formatType:String, userid: Option[String] = None, tid: Option[String] = None, pStr : Option[String]): String

  /** Remove message with MessageName and Vesion Number
    *
    * @param messageName Name of the given message
    * @param version   Version of the given message
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    */
  def RemoveMessage(messageName:String, version:Long, userid: Option[String]): String

  /**
    * Remove message with Message Name and Version Number
    *
    * @param nameSpace namespace of the object
    * @param name
    * @param version  Version of the object
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param zkNotify
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    */

  def  RemoveMessage(nameSpace: String, name: String, version: Long, userid: Option[String], zkNotify: Boolean = true): String

  /** Add container given containerText
    *
    * @param containerText text of the container (as JSON/XML string as defined by next parameter formatType)
    * @param formatType format of containerText ( JSON or XML)
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param pStr  json string that contains extra parameters to be added to BaseElem (description, comment, tag)
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    *
    * Example
    *
    * {{{
    * var apiResult = MetadataAPIImpl.AddContainer(msgStr,"JSON"))
    * var result = MetadataAPIImpl.getApiResult(apiResult)
    * println("Result as Json String => \n" + result._2)
    * }}}
    */
  def AddContainer(containerText:String, formatType:String, userid: Option[String] = None, tenantId: Option[String] = None, pStr: Option[String]): String

  /** Update container given containerText
    *
    * @param containerText text of the container (as JSON/XML string as defined by next parameter formatType)
    * @param formatType format of containerText (as JSON/XML string)
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param pStr  json string that contains extra parameters to be added to BaseElem (description, comment, tag)
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    */
  def UpdateContainer(containerText:String, formatType:String, userid: Option[String] = None, tenantid: Option[String] = None, pStr : Option[String]): String

  /** Remove container with ContainerName and Vesion Number
    *
    * @param containerName Name of the given container
    * @param version   Version of the given container
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    */
  def RemoveContainer(containerName:String, version:Long, userid: Option[String]): String

  /** Add model. Several model types are currently supported.  They describe the content of the ''input'' argument:
    *
    *   - SCALA - a Scala source string
    *   - JAVA - a Java source string
    *   - PMML - a PMML source string
    *   - KPMML - a Kamanja Pmml source string
    *   - BINARY - the path to a jar containing the model
    *
    * The remaining arguments, while noted as optional, are required for some model types.  In particular,
    * the ''modelName'', ''version'', and ''msgConsumed'' must be specified for the PMML model type.  The ''userid'' is
    * required for systems that have been configured with a SecurityAdapter or AuditAdapter.
    * @see [[http://kamanja.org/security/ security wiki]] for more information. The audit adapter, if configured,
    *       will also be invoked to take note of this user's action.
    * @see [[http://kamanja.org/auditing/ auditing wiki]] for more information about auditing.
    * NOTE: The BINARY model is not supported at this time.  The model submitted for this type will be via a jar file.
    *
    * @param modelType the type of the model submission (any {SCALA,JAVA,PMML,KPMML,BINARY}
    * @param input the text element to be added dependent upon the modelType specified.
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param modelName the namespace.name of the PMML model to be added to the Kamanja metadata
    * @param version the model version to be used to describe this PMML model
    * @param msgConsumed the namespace.name of the message to be consumed by a PMML model
    * @param optMsgProduced the namespace.name of the message to be produced by a PMML model
    * @param modelOptions model specific properties
    * @param pStr global properties
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    */

  def AddModel( modelType: ModelType
                , input: String
                , userid: Option[String] = None
                , tenantid: Option[String] = None
                , modelName: Option[String] = None
                , version: Option[String] = None
                , msgConsumed: Option[String] = None
                , msgVer : Option[String] = Some("-1")
                , optMsgProduced: Option[String] = None
                , pStr : Option[String]
                , modelOptions : Option[String] = None
              ): String

  /** Update model given the supplied input.  Like the Add model, the ''modelType'' controls the processing and describes the
    * sort of content that has been supplied in the ''input'' argument.  The current ''modelType'' values are:
    *
    *   - SCALA - a Scala source string
    *   - JAVA - a Java source string
    *   - PMML - a PMML source string
    *   - KPMML - a Kamanja Pmml source string
    *   - BINARY - the path to a jar containing the model
    *
    * The remaining arguments, while noted as optional, are required for some model types.  In particular,
    * the ''modelName'' and ''version'' must be specified for the PMML model type.  The ''userid'' is
    * required for systems that have been configured with a SecurityAdapter or AuditAdapter.
    * @see [[http://kamanja.org/security/ security wiki]] for more information. The audit adapter, if configured,
    *       will also be invoked to take note of this user's action.
    * @see [[http://kamanja.org/auditing/ auditing wiki]] for more information about auditing.
    *
    * @param modelType the type of the model submission (any {SCALA,JAVA,PMML,KPMML,BINARY}
    * @param input the text element to be added dependent upon the modelType specified.
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None
    * @param modelName appropriate for PMML, the namespace.name of the PMML model to be added to the Kamanja metadata
    * @param version appropriate for PMML, the model version to be assigned. This version ''must'' be greater than the
    *                version in use and unique for models with the modelName
    * @param optVersionBeingUpdated not used .. reserved for future release where explicit modelnamespace.modelname.modelversion
    *                               can be updated (not just the latest version)
    * @param optMsgProduced the namespace.name of the message to be produced by a PMML model
    * @param modelOptions model specific properties
    * @param pStr global properties
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
   */
  def UpdateModel(modelType: ModelType
                  , input: String
                  , userid: Option[String] = None
                  , optTenantid: Option[String] = None
                  , modelName: Option[String] = None
                  , version: Option[String] = None
                  , optVersionBeingUpdated : Option[String] = None
                  , optMsgProduced: Option[String] = None
                  , pStr : Option[String]
                  , modelOptions : Option[String]): String

  /** Remove model with the supplied ''modelName'' and ''version''.  If the SecurityAdapter and/or AuditAdapter have
    * been configured, the ''userid'' must also be supplied.
    *
    * @param modelName the Namespace.Name of the given model to be removed
    * @param version   Version of the given model.  The version should comply with the Kamanja version format.  For example,
    *                  a value of 1000001000001 is the value for 1.000001.000001. Helper functions for constructing this
    *                  Long from a string can be found in the MdMgr object,
    *                  @see com.ligadata.kamanja.metadata.ConvertVersionToLong(String):Long for details.
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None
    * @return the result as a JSON String of object ApiResult where ApiResult.statusCode
    * indicates success or failure of operation: 0 for success, Non-zero for failure. The Value of
    * ApiResult.statusDescription and ApiResult.resultData indicate the nature of the error in case of failure
    */
  def RemoveModel(modelName :String, version : String, userid : Option[String] = None): String

  /** Retrieve All available ModelDefs from Metadata Store
   *
   * @param formatType format of the return value, either JSON or XML
   * @return the ModelDef(s) either as a JSON or XML string depending on the parameter formatType
   */
  def GetAllModelDefs(formatType: String, userid: Option[String] = None) : String

  /** Retrieve specific ModelDef(s) from Metadata Store
   *
   * @param objectName Name of the ModelDef
   * @param formatType format of the return value, either JSON or XML
   * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
   * ModelDef(s) either as a JSON or XML string depending on the parameter formatType
   */
  def GetModelDef(objectName:String,formatType: String, userid: Option[String] = None) : String

  /** Retrieve a specific ModelDef from Metadata Store
    *
    * @param objectName Name of the ModelDef
    * @param version  Version of the ModelDef
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the ModelDef either as a JSON or XML string depending on the parameter formatType
   */
  def GetModelDef( objectName:String,version:String, formatType: String, userid: Option[String]) : String

  /**
    * GetDependentModels
    *
    * @param msgNameSpace
    * @param msgName
    * @param msgVer
    * @return
    */
  def GetDependentModels(msgNameSpace: String, msgName: String, msgVer: Long): Array[ModelDef]

  /** Retrieve All available MessageDefs from Metadata Store
    *
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the MessageDef(s) either as a JSON or XML string depending on the parameter formatType
    */
  def GetAllMessageDefs(formatType: String, userid: Option[String] = None) : String

  /**
    * GetAllMessagesFromCache
    *
    * @param active
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def GetAllMessagesFromCache(active: Boolean, userid: Option[String] = None, tid: Option[String] = None): Array[String]

  /** Retrieve specific MessageDef(s) from Metadata Store
    *
    * @param objectName Name of the MessageDef
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param tid tenantID filter
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the MessageDef(s) either as a JSON or XML string depending on the parameter formatType
    */
  def GetMessageDef(objectName:String,formatType: String, userid: Option[String] = None, tid : Option[String] = None) : String

  /** Retrieve a specific MessageDef from Metadata Store
    *
    * @param objectName Name of the MessageDef
    * @param version  Version of the MessageDef
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None
    * @param tid tenantID filter
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the MessageDef either as a JSON or XML string depending on the parameter formatType
    */
   def GetMessageDef( objectName:String,version:String, formatType: String, userid: Option[String], tid : Option[String]) : String


  /** Retrieve a specific MessageDef from Metadata Store
    *
    * @param objectNameSpace NameSpace of the MessageDef
    * @param objectName Name of the MessageDef
    * @param version  Version of the MessageDef
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None
    * @param tid tenantId filter
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the MessageDef either as a JSON or XML string depending on the parameter formatType
    */
  def GetMessageDef(objectNameSpace:String,objectName:String,version:String, formatType: String, userid: Option[String], tid : Option[String]) : String

  /** Retrieve specific ContainerDef(s) from Metadata Store
    *
    * @param objectName Name of the ContainerDef
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param tenantId helps to filter by tenantId
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the ContainerDef(s) either as a JSON or XML string depending on the parameter formatType
    */
  def GetContainerDef(objectName:String,formatType: String, userid: Option[String] = None, tenantId: Option[String] = None) : String

  /** Retrieve a specific ContainerDef from Metadata Store
    *
    * @param objectName Name of the ContainerDef
    * @param version  Version of the ContainerDef
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param tenantId helps to filter by tenantId
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the ContainerDef either as a JSON or XML string depending on the parameter formatType
    */
  def GetContainerDef( objectName:String,version:String, formatType: String, userid: Option[String], tenantId : Option[String]) : String

  /** Retrieve a specific ContainerDef from Metadata Store
    *
    * @param objectNameSpace NameSpace of the ContainerDef
    * @param objectName Name of the ContainerDef
    * @param version  Version of the ContainerDef
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    *  @param tenantId helps to filter by tenantId
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the ContainerDef either as a JSON or XML string depending on the parameter formatType
    */
  def GetContainerDef(objectNameSpace:String,objectName:String,version:String, formatType: String, userid: Option[String], tenantId : Option[String]) : String

   /** Retrieve All available FunctionDefs from Metadata Store. Answer the count and a string representation
    *  of them.
    *
    * param formatType format of the return value, either JSON or XML
    * param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * return the function count and the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the FunctionDef(s) either as a JSON or XML string depending on the parameter formatType as a Tuple2[Int,String]
    */

  /**
    * GetAllContainersFromCache
    *
    * @param active
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param tid helps to filter by tenantId
    * @return
    */
  // 646 - 672 Changes begin - filter by tenantId
  def GetAllContainersFromCache(active: Boolean, userid: Option[String] = None, tid: Option[String] = None): Array[String]

  /**
    * Remove container with Container Name and Version Number
    *
    * @param nameSpace namespace of the object
    * @param name
    * @param version  Version of the object
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param zkNotify
    * @return
    */
    def RemoveContainer(nameSpace: String, name: String, version: Long, userid: Option[String], zkNotify: Boolean = true): String

  /**
    * RemoveContainerFromCache
    *
    * @param zkMessage
    * @return
    */
  def RemoveContainerFromCache(zkMessage: ZkNotification)


  def GetAllFunctionDefs(formatType: String, userid: Option[String] = None) : (Int,String)

  /** Retrieve specific FunctionDef(s) from Metadata Store
    *
    * @param objectName Name of the FunctionDef
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the FunctionDef(s) either as a JSON or XML string depending on the parameter formatType
    */
  def GetFunctionDef(objectName:String,formatType: String, userid: Option[String]) : String

  /** Retrieve a specific FunctionDef from Metadata Store
    *
    * @param objectName Name of the FunctionDef
    * @param version  Version of the FunctionDef
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None..
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the FunctionDef either as a JSON or XML string depending on the parameter formatType
    */
  def GetFunctionDef( objectName:String,version:String, formatType: String, userid: Option[String]) : String

  /** Retrieve All available Concepts from Metadata Store
    *
    * param formatType format of the return value, either JSON or XML
    * param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the Concept(s) either as a JSON or XML string depending on the parameter formatType
    */

  /**
    * GetFunctionDef
    *
    * @param nameSpace namespace of the object
    * @param objectName name of the desired object, possibly namespace qualified
    * @param formatType format of the return value, either JSON or XML
    * @param version  Version of the object
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def GetFunctionDef(nameSpace: String, objectName: String, formatType: String, version: String, userid: Option[String]): String

  def GetAllConcepts(formatType: String, userid: Option[String] = None) : String

  /** Retrieve specific Concept(s) from Metadata Store
    *
    * @param objectName Name of the Concept
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None..
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the Concept(s) either as a JSON or XML string depending on the parameter formatType
    */
  def GetConcept(objectName:String, formatType: String, userid: Option[String] = None) : String

  /** Retrieve a specific Concept from Metadata Store
    *
    * @param objectName Name of the Concept
    * @param version  Version of the Concept
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the Concept either as a JSON or XML string depending on the parameter formatType
    */
  def GetConcept(objectName:String,version: String, formatType: String, userid: Option[String]) : String


  /** Retrieve All available derived concepts from Metadata Store
    *
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the Derived Concept(s) either as a JSON or XML string depending on the parameter formatType
    */
  def GetAllDerivedConcepts(formatType: String, userid: Option[String] = None) : String


  /** Retrieve specific Derived Concept(s) from Metadata Store
    *
    * @param objectName Name of the Derived Concept
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the Derived Concept(s) either as a JSON or XML string depending on the parameter formatType
    */
  def GetDerivedConcept(objectName:String, formatType: String, userid: Option[String] = None) : String

  /** Retrieve a specific Derived Concept from Metadata Store
    *
    * @param objectName Name of the Derived Concept
    * @param version  Version of the Derived Concept
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the Derived Concept either as a JSON or XML string depending on the parameter formatType
    */
  def GetDerivedConcept(objectName:String, version:String, formatType: String, userid: Option[String]) : String

  /** Retrieves all available Types from Metadata Store
    *
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the available types as a JSON or XML string depending on the parameter formatType
    */
  def GetAllTypes(formatType: String, userid: Option[String] = None) : String

  /** Retrieve a specific Type  from Metadata Store
    *
    * @param objectName Name of the Type
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    * the Type object either as a JSON or XML string depending on the parameter formatType
    */
  def GetType(objectName:String, formatType: String, userid: Option[String] = None) : String

   /**
    * getHealthCheck - will return all the health-check information for the nodeId specified.
    *
    * @param nodeId: String - if no parameter specified, return health-check for all nodes
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return status string
    */
  def getHealthCheck(nodeId: String, userid: Option[String] = None): String

  /**
    *  getHealthCheckNodesOnly - will return node info from the health-check information for the nodeId specified.
    *  @param nodeId a cluster node: String - if no parameter specified, return health-check for all nodes
    *  @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    */
  def getHealthCheckNodesOnly(nodeId: String = "", userid: Option[String] = None): String

  /**
    *  getHealthCheckComponentNames - will return partial components info from the health-check information for the nodeId specified.
    *  @param nodeId a cluster node: String - if no parameter specified, return health-check for all nodes
    *  @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    */
  def getHealthCheckComponentNames(nodeId: String = "", userid: Option[String] = None): String

  /**
    *  getHealthCheckComponentDetailsByNames - will return specific components info from the health-check information for the nodeId specified.
    *  @param componentNames names of components required
    *  @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    */
  def getHealthCheckComponentDetailsByNames(componentNames: String = "", userid: Option[String] = None): String


  /**
    * GetMetadataAPIConfig
    *
    * @return properties as key value pair from the file.
    */
  def GetMetadataAPIConfig: Properties

  def GetAllTenants(uid: Option[String] = None): Array[String]

  def GetContainerDefFromCache(nameSpace: String, name: String, formatType: String, version: String, userid: Option[String], tid: Option[String]): String

  /**
    * Get the model config keys
    *
    * @return
    */
  def getModelConfigNames(): Array[String]

  /**
    * Get a specific model (format JSON or XML) as a String using modelName(with version) as the key
    *
    * @param nameSpace namespace of the object
    * @param name
    * @param formatType format of the return value, either JSON or XML
    * @param version  Version of the object
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def GetModelDefFromCache(nameSpace: String, name: String, formatType: String, version: String, userid: Option[String] = None, tid : Option[String] = None): String

  /**
    * Deactivate the model that presumably is active and waiting for input in the working set of the cluster engines.
    *
    * @param nameSpace namespace of the object
    * @param name
    * @param version  Version of the object
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def DeactivateModel(nameSpace: String, name: String, version: Long, userid: Option[String] = None): String

  /**
    * Activate the model with the supplied keys. The engine is notified and the model factory is loaded.
    *
    * @param nameSpace namespace of the object
    * @param name
    * @param version  Version of the object
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def ActivateModel(nameSpace: String, name: String, version: Long, userid: Option[String] = None): String

  /**
    * GetAllModelsFromCache
    *
    * @param active
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def GetAllModelsFromCache(active: Boolean, userid: Option[String] = None, tid: Option[String] = None): Array[String]

  /**
    * GetAllFunctionsFromCache
    *
    * @param active
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def GetAllFunctionsFromCache(active: Boolean, userid: Option[String] = None): Array[String]

  /**
    * RemoveConcept
    *
    * @param key
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def RemoveConcept(key: String, userid: Option[String] = None): String

  /**
    * RemoveConcept
    *
    * @param nameSpace namespace of the object
    * @param name
    * @param version  Version of the object
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def RemoveConcept(nameSpace: String, name: String, version: Long, userid: Option[String]): String

  /**
    * AddTypes
    *
    * @param typesText
    * @param format
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def AddTypes(typesText: String, format: String, userid: Option[String] = None): String

  /**
    * GetAllKeys
    *
    * @param objectType
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def GetAllKeys(objectType: String, userid: Option[String] = None): Array[String]

  /**
    * GetAllTypesByObjType - All available types(format JSON or XML) as a String
    *
    * @param formatType format of the return value, either JSON or XML
    * @param objType
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def GetAllTypesByObjType(formatType: String, objType: String, userid: Option[String] = None): String

  /**
    * Get a specific messsage/container using schemaId as the key
    *
    * @param schemaId
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *        method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    *        the Message/Container either as a JSON or XML string depending on the parameter formatType
    */
  def GetTypeBySchemaId(schemaId: Int, userid: Option[String]): String

  /**
    * Get a specific messsage/container/model using elementId as the key
    *
    * @param elementId
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *        method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return the result as a JSON String of object ApiResult where ApiResult.resultData contains
    *      the ContainerDef/MessageDef/ModelDef either as a JSON or XML string depending on the parameter formatType
    */
  def GetTypeByElementId(elementId: Long, userid: Option[String]): String


  /**
    * Accept a config specification (a JSON str)
    *
    * @param cfgStr the json file to be interpted
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param objectList note on the objects in the configuration to be logged to audit adapter
    * @return
    */
  def UploadConfig(cfgStr: String, userid: Option[String], objectList: String): String

  /**
    * Upload a model config.  These are for native models written in Scala or Java
    *
    * @param cfgStr
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param objectList
    * @param isFromNotify
    * @return
    */
  def UploadModelsConfig(cfgStr: String, userid: Option[String], objectList: String, isFromNotify: Boolean = false): String

  /**
    * All available config objects(format JSON) as a String
    *
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. The default is None, but if Security and/or Audit are configured, this value is of little practical use.
    *               Supply one.
    * @return
    */
  def GetAllCfgObjects(formatType: String, userid: Option[String] = None): String

  /**
    * Remove a cluster configuration
    *
    * @param cfgStr
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @param cobjects
    * @return results string
    */
  def RemoveConfig(cfgStr: String, userid: Option[String], cobjects: String): String

  /**
    * Get the nodes as json.
    *
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. The default is None, but if Security and/or Audit are configured, this value is of little practical use.
    *               Supply one.
    * @return
    */
  def GetAllNodes(formatType: String, userid: Option[String] = None): String

  /**
    * All available clusters(format JSON) as a String
    *
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. The default is None, but if Security and/or Audit are configured, this value is of little practical use.
    *               Supply one.
    * @return
    */
  def GetAllClusters(formatType: String, userid: Option[String] = None): String

  /**
    *
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. The default is None, but if Security and/or Audit are configured, this value is of little practical use.
    *               Supply one.
    * @return
    */
  def GetAllClusterCfgs(formatType: String, userid: Option[String] = None): String

  /**
    * All available adapters(format JSON) as a String
    *
    * @param formatType format of the return value, either JSON or XML
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. The default is None, but if Security and/or Audit are configured, this value is of little practical use.
    *               Supply one.
    * @return
    */
  def GetAllAdapters(formatType: String, userid: Option[String] = None): String


  /**
    * NotifyEngine
    *
    * @param objList <description please>
    * @param operations <description please>
    */
  def NotifyEngine(objList: Array[BaseElemDef], operations: Array[String])

  /**
    * SaveObject
    *
    * @param obj <description please>
    * @param mdMgr the metadata manager receiver
    * @return <description please>
    */
  def SaveObject(obj: BaseElemDef, mdMgr: MdMgr): Boolean

  /**
    * SaveObject
    *
    * @param bucketKeyStr
    * @param value
    * @param typeName
    * @param serializerTyp
    */
  def SaveObject(bucketKeyStr: String, value: Array[Byte], typeName: String, serializerTyp: String)

  def GetUniqueId: Long

  def GetMdElementId: Long

  /**
    * getModelMessagesContainers
    *
    * @param modelConfigName
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def getModelMessagesContainers(modelConfigName: String, userid: Option[String] = None): List[String]

  def getModelInputTypesSets(modelConfigName: String, userid: Option[String] = None): List[List[String]]

  def getModelOutputTypes(modelConfigName: String, userid: Option[String] = None): List[String]

  /**
    * Answer the model compilation dependencies
    * FIXME: Which ones? input or output?
    *
    * @param modelConfigName
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def getModelDependencies(modelConfigName: String, userid: Option[String] = None): List[String]


  def GetSchemaId: Int

  def UpdateTranId (objList:Array[BaseElemDef] ): Unit

  /**
    * logAuditRec - Record an Audit event using the audit adapter
    *
    * @param userOrRole the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. The default is None, but if Security and/or Audit are configured, this value is of little practical use.
    *               Supply one.
    * @param userPrivilege <description please>
    * @param action <description please>
    * @param objectText <description please>
    * @param success <description please>
    * @param transactionId <description please>
    * @param objName <description please>
    */
  def logAuditRec(userOrRole: Option[String], userPrivilege: Option[String], action: String, objectText: String, success: String, transactionId: String, objName: String)

  /**
    * GetNewTranId
    *
    * @return <description please>
    */
  def GetNewTranId: Long

  /**
    * AddObjectToCache
    *
    * @param o <description please>
    *  @param mdMgr the metadata manager receiver
    */
  def AddObjectToCache(o: Object, mdMgr: MdMgr, ignoreExistingObjectsOnStartup: Boolean = false)

  /**
    * DeleteObject
    *
    * @param obj
    */
  def DeleteObject(obj: BaseElemDef)

  def setCurrentTranLevel(tranLevel: Long)

  /**
    * SaveObjectList
    *
    * The following batch function is useful when we store data in single table
    * If we use Storage component library, note that table itself is associated with a single
    * database connection( which itself can be mean different things depending on the type
    * of datastore, such as cassandra, hbase, etc..)
    *
    * @param objList
    * @param typeName
    */
  def SaveObjectList(objList: Array[BaseElemDef], typeName: String)

  /**
    * SaveObjectList
    *
    * @param keyList
    * @param valueList
    * @param typeName
    * @param serializerTyp
    */
  def SaveObjectList(keyList: Array[String], valueList: Array[Array[Byte]], typeName: String, serializerTyp: String)

  /**
    * DeleteObject
    *
    * @param bucketKeyStr
    * @param typeName
    */
  def DeleteObject(bucketKeyStr: String, typeName: String)

  /**
    * AddConfigObjToCache
    *
    * @param tid <description please>
    * @param key <description please>
    * @param mdlConfig <description please>
    *  @param mdMgr the metadata manager receiver
    */
  def AddConfigObjToCache(tid: Long, key: String, mdlConfig: Map[String, List[String]], mdMgr: MdMgr)


  /**
    * Remove all of the elements with the supplied keys in the list from the supplied DataStore
    *
    * @param keyList
    * @param typeName
    */
  def RemoveObjectList(keyList: Array[String], typeName: String)

  /**
    * getSSLCertificatePasswd
    */
  def getSSLCertificatePasswd: String

  /**
    * setSSLCertificatePasswd
    *
    * @param pw <description please>
    */
  def setSSLCertificatePasswd(pw: String)

  /**
    * GetDependantJars of some base element (e.g., model, type, message, container, etc)
    *
    * @param obj <description please>
    * @return <description please>
    */
  def GetDependantJars(obj: BaseElemDef): Array[String]

  /**
    * UploadJarToDB
    *
    * @param jarName <description please>
    */
  def UploadJarToDB(jarName: String)

  /**
    * UploadJarToDB
    *
    * @param jarName <description please>
    * @param byteArray <description please>
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return <description please>
    */
  def UploadJarToDB(jarName: String, byteArray: Array[Byte], userid: Option[String] = None): String

    /**
    * UploadJarsToDB
    *
    * @param obj <description please>
    * @param forceUploadMainJar <description please>
    * @param alreadyCheckedJars <description please>
    */
  def UploadJarsToDB(obj: BaseElemDef, forceUploadMainJar: Boolean = true, alreadyCheckedJars: scala.collection.mutable.Set[String] = null): Unit

  /**
    * PutTranId
    *
    * @param tId <description please>
    */
  def PutTranId(tId: Long)

  /**
    * GetObject
    *
    * @param bucketKeyStr
    * @param typeName
    */
  def GetObject(bucketKeyStr: String, typeName: String): (String, Any)

  /**
    * getObjectType
    *
    * @param obj <description please>
    * @return <description please>
    */
  def getObjectType(obj: BaseElemDef): String

  /**
    * Recompile the supplied model. Optionally the message definition is supplied that was just built.
    *
    * @param mod the model definition that possibly needs to be reconstructed.
    * @param userid the user id that has invoked this command
    * @param optMsgDef the MessageDef constructed, assuming it was a message def. If a container def has been rebuilt,
    *               this field will have a value of None.  This is only meaningful at this point when the model to
    *               be rebuilt is a PMML model.
    * @return the result string reflecting what happened with this operation.
    */
  def RecompileModel(mod: ModelDef, userid : Option[String], optMsgDef : Option[MessageDef]): String

  /**
    * DownloadJarFromDB
    *
    * @param obj <description please>
    */
  def DownloadJarFromDB(obj: BaseElemDef)

  /**
    * IsValidVersion
    *
    * @param oldObj
    * @param newObj
    * @return
    */
  def IsValidVersion(oldObj: BaseElemDef, newObj: BaseElemDef): Boolean

  /**
    * DeactivateObject
    *
    * @param obj
    */
  def DeactivateObject(obj: BaseElemDef)

  /**
    * ActivateObject
    *
    * @param obj
    */
  def ActivateObject(obj: BaseElemDef)

  def getCurrentTranLevel() : Long

  /**
    * GetJarAsArrayOfBytes
    *
    * @param jarName <description please>
    * @return <description please>
    */
  def GetJarAsArrayOfBytes(jarName: String): Array[Byte]

  /**
    * IsDownloadNeeded
    *
    * @param jar <description please>
    * @param obj <description please>
    * @return <description please>
    */
  def IsDownloadNeeded(jar: String, obj: BaseElemDef): Boolean

  /**
    * PutArrayOfBytesToJar
    *
    * @param ba <description please>
    * @param jarName <description please>
    */
  def PutArrayOfBytesToJar(ba: Array[Byte], jarName: String)

  def GetAuditObj: AuditAdapter

  /**
    * Release various resources including heartbeat, dbstore, zk listener, and audit adapter
    * FIXME: What about Security adapter? Should there be a 'release' call on the SecurityAdapter trait?
    */
  def shutdown: Unit

  /**
    * InitMdMgr
    *
    * @param mgr the metadata manager instance
    * @param jarPathsInfo
    * @param databaseInfo
    */
  def InitMdMgr(mgr: MdMgr, jarPathsInfo: String, databaseInfo: String)

  /**
    * Initialize the metadata from the bootstrap, establish zookeeper listeners, load the cached information from
    * persistent storage, set up heartbeat and authorization implementations.
    * FIXME: Is there a difference between this function and InitMdMgr?
    *
    * @see InitMdMgr(String,Boolean)
    * @param configFile the MetadataAPI configuration file
    * @param startHB
    */
  def InitMdMgrFromBootStrap(configFile: String, startHB: Boolean)

  /**
    * CloseDbStore
    */
  def CloseDbStore: Unit

  /**
    * checkAuth
    *
    * @param usrid a
    * @param password a
    * @param role a
    * @param privilige a
    * @return <description please>
    */
  def checkAuth(usrid: Option[String], password: Option[String], role: Option[String], privilige: String): Boolean

  /**
    * getPrivilegeName
    *
    * @param op <description please>
    * @param objName <description please>
    * @return <description please>
    */
  def getPrivilegeName(op: String, objName: String): String

  /**
    * GetAllTypesFromCache
    *
    * @param active <description please>
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return <description please>
    */
  def GetAllTypesFromCache(active: Boolean, userid: Option[String] = None): Array[String]

  /**
    * GetAllConceptsFromCache
    *
    * @param active
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def GetAllConceptsFromCache(active: Boolean, userid: Option[String] = None): Array[String]

  /**
    * Get an audit record from the audit adapter.
    *
    * @param startTime <description please>
    * @param endTime <description please>
    * @param userOrRole the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value should be supplied.
    * @param action <description please>
    * @param objectAccessed <description please>
    * @return <description please>
    */
  def getAuditRec(startTime: Date, endTime: Date, userOrRole: String, action: String, objectAccessed: String): String

  /**
    * getLeaderHost
    *
    * @param leaderNode <description please>
    * @return <description please>
    */
  def getLeaderHost(leaderNode: String): String

  /**
    * getAuditRec
    *
    * @param filterParameters <description please>
    * @return <description please>
    */
  def getAuditRec(filterParameters: Array[String]): String

  /**
    *
    * @param nameSpace namespace of the object
    * @param objectName name of the desired object, possibly namespace qualified
    * @param formatType format of the return value, either JSON or XML
    * @param version  Version of the object
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def GetModelDef(nameSpace: String, objectName: String, formatType: String, version: String, userid: Option[String]): String

  /**
    * Get a single concept as a string using name and version as the key
    *
    * @param nameSpace namespace of the object
    * @param objectName name of the desired object, possibly namespace qualified
    * @param formatType format of the return value, either JSON or XML
    * @param version  Version of the object
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def GetConceptDef(nameSpace: String, objectName: String, formatType: String,
    version: String, userid: Option[String]): String

  /**
    * GetTypeDef
    *
    * @param nameSpace namespace of the object
    * @param objectName name of the desired object, possibly namespace qualified
    * @param formatType format of the return value, either JSON or XML
    * @param version  Version of the object
    * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
    *               method. If Security and/or Audit are configured, this value must be a value other than None.
    * @return
    */
  def GetTypeDef(nameSpace: String, objectName: String, formatType: String, version: String, userid: Option[String] = None): String

  /**
    * getSSLCertificatePath
    */
  def getSSLCertificatePath: String

  /**
    * Read metadata api configuration properties
    *
    * @param configFile the MetadataAPI configuration file
    */
  def readMetadataAPIConfigFromPropertiesFile(configFile: String): Unit

  /**
    * OpenDbStore
    *
    * @param jarPaths Set of paths where jars are located
    * @param dataStoreInfo information needed to access the data store (kv store dependent)
    */
  def OpenDbStore(jarPaths: collection.immutable.Set[String], dataStoreInfo: String)

  /**
    * UpdateObject
    *
    * @param key
    * @param value
    * @param typeName
    * @param serializerTyp
    */
  def UpdateObject(key: String, value: Array[Byte], typeName: String, serializerTyp: String)

  /**
    * UpdateObjectInDB
    *
    * @param obj <description please>
    */
  def UpdateObjectInDB(obj: BaseElemDef)

  def UpdateMdMgr(zkTransaction: ZkTransaction): Unit

  def setSslEnabled(enable: Boolean): Unit

  def isSslEnabled: Boolean

  def GetMainDS(): DataStore
}
// 646 - 674 Change ends
