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

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import scala.io.Source
import com.ligadata.Exceptions._
import org.apache.logging.log4j.{ Logger, LogManager }

import com.ligadata.MetadataAPI.MetadataAPI.ModelType
import com.ligadata.MetadataAPI.Utility._
import com.ligadata.kamanja.metadata.MdMgr


import scala.collection.mutable
import scala.collection.immutable
import com.ligadata.KamanjaVersion.KamanjaVersion
import scala.io.Source
import java.nio.file.{ Files, FileSystems }

/**
 * Created by dhaval Kolapkar on 7/24/15.
 */

object StartMetadataAPI {

  var response = ""
  //get default config
  val defaultConfig = scala.util.Properties.envOrElse("KAMANJA_HOME", scala.util.Properties.envOrElse("HOME", "~")) + "/config/MetadataAPIConfig.properties"
  val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  var action = ""
  var location = ""
  var config = ""
  val WITHDEP = "dependsOn"
  val REMOVE = "remove"
  val GET = "get"
  val ACTIVATE = "activate"
  val OUTPUTMSG = "outmessage"
  val DEACTIVATE = "deactivate"
  val UPDATE = "update"
  val MODELS = "models"
  val MESSAGES = "messages"
  val CONTAINERS = "containers"
  val TENANTID = "tenantid"
  val INPUTLOC = "inputlocation"
  var expectDep = false
  var expectRemoveParm = false
  var depName: String = ""
  var parmName: String = ""
  val MODELNAME = "MODELNAME"
  val MODELVERSION = "MODELVERSION"
  val MESSAGENAME = "MESSAGENAME"
  val CONTAINERNAME = "CONTAINERNAME"
  val TYPENAME = "TYPENAME"
  val FUNCTIONNAME = "FUNCTIONNAME"

  val SCHEMAID = "SCHEMAID"
  val ELEMENTID = "ELEMENTID"

  /** AdapterMessageBinding tags */
  val FROMFILE = "FROMFILE"
  val FROMSTRING = "FROMSTRING"
  val KEY = "KEY"

  val ADAPTERMESSAGEBINDING = "ADAPTERMESSAGEBINDING"

  /** List AdapterMessageBinding filters */
  val ADAPTERFILTER = "ADAPTERFILTER"
  val MESSAGEFILTER = "MESSAGEFILTER"
  val SERIALIZERFILTER = "SERIALIZERFILTER"
  val MODELOPTIONS = "MODELOPTIONS"

  var expectModelName = false
  var expectModelVer = false
  var expectMessageName = false
  var foundModelName = false
  var foundModelVer = false
  var foundMessageName = false
  var expectBindingFromFile = false
  var expectBindingFromString = false
  var expectModelOpts = false
  var removeCmdFound = false
  var expectRemoveBindingKey = false
  var expectListBindingFilter = false
  var expectAdapterFilter = false
  var expectMessageFilter = false
  var expectSerializerFilter = false
  var expectOutputMsg = false
  var varmap: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map[String, String]()
  var expectTid: Boolean = false
  var expectMDep: Boolean = false
  var expectSchemaId = false
  var expectElementId = false

  // 646 - 675, 676 Change begins - replace MetadataAPIImpl, addition of new meta data
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  val PROPERTYFILE = "PropertiesFile"
  val PROPERTY = "Properties"
  var expectPropFile = false
  var expectPropStr = false
  // 646 - 675,676 Change ends

  val extraCmdArgs = mutable.Map[String, String]()

  def main(args: Array[String]) {
    if (args.length > 0 && args(0).equalsIgnoreCase("--version")) {
      KamanjaVersion.print
      return
    }

    /** FIXME: the user id should be discovered in the parse of the args array */
    val userId: Option[String] = Some("kamanja")
    try {
      val jsonBuffer: StringBuilder = new StringBuilder

      extraCmdArgs(PROPERTYFILE) = ""
      extraCmdArgs(PROPERTY) = ""

      args.foreach(arg => {

        if ((arg.endsWith(".json")
          || arg.endsWith(".jtm")
          || arg.endsWith(".xml")
          || arg.endsWith(".pmml")
          || arg.endsWith(".scala")
          || arg.endsWith(".java")
          || arg.endsWith(".py")
          || arg.endsWith(".jar")) && (expectPropFile == false)) {
         extraCmdArgs(INPUTLOC) = arg
          if (expectBindingFromFile) {
            /** the json test above can prevent the ordinary catch of the name below */
            extraCmdArgs(FROMFILE) = extraCmdArgs.getOrElse(INPUTLOC, null)
            expectBindingFromFile = false
          }

        } else if (arg.endsWith(".properties")) {
          config = arg
        } else {
          if (arg != "debug") {
            /** ignore the debug tag */
            if (arg.equalsIgnoreCase(TENANTID)) {
              expectTid = true
              extraCmdArgs(TENANTID) = ""
            } else if (arg.equalsIgnoreCase(WITHDEP)) {
              expectDep = true
              extraCmdArgs(WITHDEP) = ""
            } else if (arg.equalsIgnoreCase(MODELNAME)) {
              expectModelName = true
            } else if (arg.equalsIgnoreCase(MODELVERSION)) {
              expectModelVer = true
            } else if (arg.equalsIgnoreCase(MESSAGENAME)) {
              expectMessageName = true
            } else if (arg.equalsIgnoreCase(OUTPUTMSG)) {
              expectOutputMsg = true
            } else if (arg.equalsIgnoreCase(KEY)) {
              expectRemoveBindingKey = true
            } else if (arg.equalsIgnoreCase(FROMFILE)) {
              expectBindingFromFile = true
            } else if (arg.equalsIgnoreCase(FROMSTRING)) {
                expectBindingFromString = true
            } else if (arg.equalsIgnoreCase(MODELOPTIONS)) {
                expectModelOpts = true
            } else if (arg.equalsIgnoreCase(ADAPTERFILTER)) {
              expectAdapterFilter = true
            } else if (arg.equalsIgnoreCase(MESSAGEFILTER)) {
              expectMessageFilter = true
            } else if (arg.equalsIgnoreCase(SERIALIZERFILTER)) {
              expectSerializerFilter = true
            } else if (arg.equalsIgnoreCase(SCHEMAID)) {
              expectSchemaId = true
              extraCmdArgs(SCHEMAID) = ""
            } else if (arg.equalsIgnoreCase(ELEMENTID)) {
              expectElementId = true
              extraCmdArgs(ELEMENTID) = ""
            } else if (arg.equalsIgnoreCase(PROPERTYFILE)) {
              logger.warn("getting pfile " + arg)
              expectPropFile = true
            }
            else if (arg.equalsIgnoreCase(PROPERTY)) {
              logger.warn ("getting property " + arg)
              expectPropStr = true
            } else {
              var argVar = arg
              if (expectTid) {
                extraCmdArgs(TENANTID) = arg
                expectTid = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectDep) {
                extraCmdArgs(WITHDEP) = arg
                expectDep = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectModelName) {
                extraCmdArgs(MODELNAME) = arg
                expectModelName = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectModelVer) {
                extraCmdArgs(MODELVERSION) = arg
                expectModelVer = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectMessageName) {
                extraCmdArgs(MESSAGENAME) = arg
                expectMessageName = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectOutputMsg) {
                extraCmdArgs(OUTPUTMSG) = arg
                expectOutputMsg = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectRemoveBindingKey) {
                expectRemoveBindingKey = false
                extraCmdArgs(Action.REMOVEADAPTERMESSAGEBINDING.toString) = arg
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectBindingFromString) {
                extraCmdArgs(FROMSTRING) = arg
                expectBindingFromString = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectModelOpts) {
                extraCmdArgs(MODELOPTIONS) = arg
                expectModelOpts = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectBindingFromFile) {
                extraCmdArgs(FROMFILE) = arg
                expectBindingFromFile = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectAdapterFilter) {
                extraCmdArgs(ADAPTERFILTER) = arg
                expectAdapterFilter = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectMessageFilter) {
                extraCmdArgs(MESSAGEFILTER) = arg
                expectMessageFilter = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectSerializerFilter) {
                extraCmdArgs(SERIALIZERFILTER) = arg
                expectSerializerFilter = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectSchemaId) {
                extraCmdArgs(SCHEMAID) = arg
                expectSchemaId = false
                argVar = "" // Make sure we don't add to the routing command
              }
              if (expectElementId) {
                extraCmdArgs(ELEMENTID) = arg
                expectElementId = false
                argVar = "" // Make sure we don't add to the routing command

              }
              if (expectPropFile) {
                extraCmdArgs(PROPERTYFILE) = arg
                expectPropFile = false
                argVar = ""
              }
              if (expectPropStr) {
                extraCmdArgs(PROPERTY) = arg
                expectPropStr = false
                argVar = ""
              }

              /**
               * FIXME:
               * FIXME: The removes have positional keys... right after the command.  Downside is that
               * the tenant id collection uses a named style, which means that it MUST follow the
               * object name to be removed.  When this thing gets reworked, the object key should also
               * have a name like "key" as is used with the REMOVEADAPTERMESSAGEBINDING. The name/value pairs can
               * then be expressed in any order to the liking of the user.
               */
              if (action.equalsIgnoreCase("getmodel") || action.equalsIgnoreCase("removemodel")) {
                /** only take the first one */
                if (!extraCmdArgs.contains(MODELNAME)) extraCmdArgs(MODELNAME) = arg
                argVar = "" // Make sure we don't add to the routing command
              }
              if (action.equalsIgnoreCase("activatemodel") || action.equalsIgnoreCase("deactivatemodel")) {
                /** only take the first one */
                if (!extraCmdArgs.contains(MODELNAME)) extraCmdArgs(MODELNAME) = arg
                argVar = "" // Make sure we don't add to the routing command
              }
              if (action.equalsIgnoreCase("getmessage") || action.equalsIgnoreCase("removemessage")) {
                /** only take the first one */
                if (!extraCmdArgs.contains(MESSAGENAME)) extraCmdArgs(MESSAGENAME) = arg
                argVar = "" // Make sure we don't add to the routing command
              }
              if (action.equalsIgnoreCase("removecontainer") || action.equalsIgnoreCase("getcontainer")) {
                /** only take the first one */
                if (!extraCmdArgs.contains(CONTAINERNAME)) extraCmdArgs(CONTAINERNAME) = arg
                argVar = "" // Make sure we don't add to the routing command
              }
              if (action.equalsIgnoreCase("removetype") || action.equalsIgnoreCase("gettype")) {
                /** only take the first one */
                if (!extraCmdArgs.contains(TYPENAME)) extraCmdArgs(TYPENAME) = arg
                argVar = "" // Make sure we don't add to the routing command
              }
              if (action.equalsIgnoreCase("removefunction") || action.equalsIgnoreCase("getfunction")) {
                /** only take the first one */
                if (!extraCmdArgs.contains(FUNCTIONNAME)) extraCmdArgs(FUNCTIONNAME) = arg
                argVar = "" // Make sure we don't add to the routing command
              }

              action += argVar
            }
          }
        }
      })
      //add configuration
      if (config == "") {
        println("Using default configuration " + defaultConfig)
        config = defaultConfig
      }

      // logger.warn(extraCmdArgs(PROPERTYFILE)  + " name of propertty file")
      //              var paramValues : scala.collection.mutable.Map [String, Any]
      var paramJsonStr : String =  extraCmdArgs(PROPERTY)
      val paramConfig = scala.util.Properties.envOrElse("KAMANJA_HOME", scala.util.Properties.envOrElse("HOME", "~")) + "/config/" + (extraCmdArgs getOrElse (PROPERTYFILE, None))


      if (extraCmdArgs(PROPERTYFILE) != "") {
        if (FileExists(extraCmdArgs(PROPERTYFILE))) {
          paramJsonStr = Source.fromFile(extraCmdArgs(PROPERTYFILE)).getLines.mkString
          logger.warn(extraCmdArgs(PROPERTYFILE) + " name of propertty file contents " + paramJsonStr)
          //          val mapOriginal  =   parse(paramJsonStr).values.asInstanceOf[scala.collection.mutable.Map[String, Any]]
        }
      }



      getMetadataAPI.InitMdMgrFromBootStrap(config, false)
      val tenantId = extraCmdArgs.getOrElse(TENANTID, "")
      if (tenantId != "") {
        if (isValidTenantId(extraCmdArgs.getOrElse(TENANTID, "")) == false) {
          throw TenantIdNotFoundException("Tennat Id not found, please use valid tenantid or add tenantid in ClusterConfig", null)
        }
      }
      if (action == "")
        TestMetadataAPI.StartTest
      else {
        response = route(Action.withName(action.trim), extraCmdArgs.getOrElse(INPUTLOC, ""),
          extraCmdArgs.getOrElse(WITHDEP, ""), extraCmdArgs.getOrElse(TENANTID, ""), args, userId, extraCmdArgs.toMap, paramJsonStr)
        println("Result: " + response)
      }
    }
    catch {
      case e: TenantIdNotFoundException => {
        logger.error("Unable to get tenantid info, please add it to ClusterConfig before using. ", e.getMessage)
        response = new ApiResult(-1, "StartMetadataAPI", null, e.getMessage).toString
        println("Result: " + response)
      }
      case e: NoSuchElementException => {
        println("action trim" + action.trim());
        logger.error("Route not found", e.getMessage)

        /** preserve the original response ... */
        response = new ApiResult(-1, "StartMetadataAPI", null, e.getMessage).toString
        println("Result: " + response)

        /** one more try ... going the alternate route */
        // do we still need this ??
        /* val altResponse: String = AltRoute(args)
         if (altResponse != null) {
           //response = altResponse
           println(response)
           usage
         } else {
           /* if the AltRoute doesn't produce a valid result, we will complain with the original failure */
           println(response)
           usage
         }*/
      }
      case e: java.io.FileNotFoundException => {
        logger.error("Unable to read a file, the file either does not exist or is inaccessible ", e.getMessage)
        response = new ApiResult(-1, "StartMetadataAPI", null, e.getMessage).toString
        println("Result: " + response)
      }
      case e: Throwable => {
        logger.error("Error, due to an unknown exception", e)
        response = new ApiResult(-1, "StartMetadataAPI", null, e.getMessage).toString
        println("Result: " + response)
      }
      case e: Exception => {
        logger.error("Error, due to an unknown exception", e)
        response = new ApiResult(-1, "StartMetadataAPI", null, e.getMessage).toString
        println("Result: " + response)
      }
      case e: RuntimeException => {
        logger.error("Error, due to an unknown exception", e)
        response = new ApiResult(-1, "StartMetadataAPI", null, e.getMessage).toString
        println("Result: " + response)
      }
    } finally {
      getMetadataAPI.shutdown
    }
  }

  //  def extraParam (fileName : String) : Map[String, String] = {

  //  }

  def FileExists(filename: String): Boolean = {

    val defaultFS = FileSystems.getDefault()

    val path = defaultFS.getPath(filename)

    if (Files.exists(path))
      return true
    else
      return false

  }

  def isValidTenantId(tenantId: String): Boolean = {

    val tenatInfo = MdMgr.GetMdMgr.GetTenantInfo(tenantId.toLowerCase)
    if (tenatInfo == null) {
      logger.error("Not found tenantInfo for tenantId " + tenantId)
      return false
    }
    return true
  }

  // main
  def usage: Unit = {
    println(s"Usage:\n  kamanja <action> <optional input> \n e.g. kamanja add message ${'$'}HOME/msg.json")
  }

  def route(action: Action.Value, input: String, param: String = "", tenantid: String, originalArgs: Array[String], userId: Option[String], extraCmdArgs: immutable.Map[String, String], paramJsonStr: String): String = {
    var response = ""
    var fileinquesiton = input
    var optMsgProduced: Option[String] = None
    var tid = if (tenantid.size > 0) Some(tenantid) else None
    var paramStr = if (paramJsonStr != "") Some(paramJsonStr) else None

    val outputMsgName = extraCmdArgs.getOrElse(OUTPUTMSG, null)

    if (outputMsgName != null) {
      logger.debug("The value of argument optMsgProduced will be " + outputMsgName)
      optMsgProduced = Some(outputMsgName)
    }
    try {
      action match {
        //message management
        case Action.ADDMESSAGE => response = MessageService.addMessage(input, tid, paramStr)
        case Action.UPDATEMESSAGE => response = MessageService.updateMessage(input, tid, paramStr)
        case Action.REMOVEMESSAGE => {
          val msgName: String = extraCmdArgs.getOrElse(MESSAGENAME, "")
          if (msgName.isEmpty)
            response = MessageService.removeMessage()
          else
            response = MessageService.removeMessage(msgName)
        }

        // 672 Change beigns, adds an extra argument for tenantID tid to enable filtering by tenantid
        case Action.GETALLMESSAGES => response = MessageService.getAllMessages(tid)
        case Action.GETMESSAGE => {
          val msgName: String = extraCmdArgs.getOrElse(MESSAGENAME, "")
          if (msgName.isEmpty)
            response = MessageService.getMessage("", tid)
          else
            response = MessageService.getMessage(msgName, tid)
        }
        // 672 Change ends

        //model management
        case Action.ADDMODELKPMML => response = ModelService.addModelKPmml(input, userId, optMsgProduced, tid, paramStr)
        case Action.ADDMODELJTM   => response = ModelService.addModelJTM(input, userId, tid, if (param == null || param.trim.size == 0) None else Some(param.trim), paramStr)
        case Action.ADDMODELPMML => {
          val modelName: Option[String] = extraCmdArgs.get(MODELNAME)
          val modelVer = extraCmdArgs.getOrElse(MODELVERSION, null)
          val msgName: Option[String] = extraCmdArgs.get(MESSAGENAME)
          val validatedModelVersion = if (modelVer != null) MdMgr.FormatVersion(modelVer) else null
          val optModelVer = Option(validatedModelVersion)
          val optMsgVer = Option(null)
          response = ModelService.addModelPmml(ModelType.PMML
                                            , input
                                            , userId
                                            , modelName
                                            , optModelVer
                                            , msgName
                                            , optMsgVer
                                            , tid
                                            , paramStr
					    , optMsgProduced)
        }

        case Action.ADDMODELPYTHON => {
          val modelName : String = extraCmdArgs.getOrElse(MODELNAME,"there was no modelName supplied")
          /** assumption here is that the file name endswith(".py")... we want the stem of the file name for moduleName */
          val moduleName : String = input.split('/').last.split('.').dropRight(1).last
          val pkgQualifiedName : Option[String] = Some(s"$moduleName.$modelName")
          val modelVer = extraCmdArgs.getOrElse(MODELVERSION, null)
          val modelOptions : String = extraCmdArgs.getOrElse(MODELOPTIONS, "{}")
          val optMsgName: Option[String] = extraCmdArgs.get(MESSAGENAME)
          val validatedModelVersion = if (modelVer != null) MdMgr.FormatVersion(modelVer) else MdMgr.FormatVersion("0.1.0")
          val optModelVer = Option(validatedModelVersion)
          val optMsgVer = Option(null)
          response = ModelService.addModelPython(ModelType.PYTHON, input, userId, pkgQualifiedName, optModelVer, optMsgName, optMsgVer, optMsgProduced, tid, paramStr, Some(modelOptions))
        }

        case Action.ADDMODELJYTHON => {
            val modelName : String = extraCmdArgs.getOrElse(MODELNAME,"there was no modelName supplied")
            /** assumption here is that the file name endswith(".py")... we want the stem of the file name for moduleName */
            val moduleName : String = input.split('.').dropRight(1).last
            val pkgQualifiedName : Option[String] = Some(s"$moduleName.$modelName")
            val modelVer = extraCmdArgs.getOrElse(MODELVERSION, null)
            val modelOptions : String = extraCmdArgs.getOrElse(MODELOPTIONS, "{}")
            val optMsgName: Option[String] = extraCmdArgs.get(MESSAGENAME)
            val validatedModelVersion = if (modelVer != null) MdMgr.FormatVersion(modelVer) else MdMgr.FormatVersion("0.1.0")
            val optModelVer = Option(validatedModelVersion)
            val optMsgVer = Option(null)
            response = ModelService.addModelJython(ModelType.JYTHON, input, userId, pkgQualifiedName, optModelVer, optMsgName, optMsgVer, optMsgProduced, tid, paramStr, Some(modelOptions))
        }

        case Action.ADDMODELSCALA => {
          if (param.length == 0)
            response = ModelService.addModelScala(input, "", userId, optMsgProduced, tid, paramStr)
          else
            response = ModelService.addModelScala(input, param, userId, optMsgProduced, tid, paramStr)
        }

        case Action.ADDMODELJAVA => {
          if (param.length == 0)
            response = ModelService.addModelJava(input, "", userId, optMsgProduced, tid, paramStr)
          else
            response = ModelService.addModelJava(input, param, userId, optMsgProduced, tid, paramStr)
        }

        case Action.REMOVEMODEL => {
          val modelName: String = extraCmdArgs.getOrElse(MODELNAME, "")
          if (modelName.isEmpty)
            response = ModelService.removeModel("", userId)
          else
            response = ModelService.removeModel(modelName)
        }

        /*case Action.ACTIVATEMODEL =>
          response = {
            if (param.length == 0)
              ModelService.activateModel("", userId)
            else
              ModelService.activateModel(param, userId)
          }

        case Action.DEACTIVATEMODEL => response = {
          if (param.length == 0)
            ModelService.deactivateModel("", userId)
          else
            ModelService.deactivateModel(param, userId)
        }*/
        case Action.ACTIVATEMODEL => {
          val modelName: String = extraCmdArgs.getOrElse(MODELNAME, "")

          response = {
            if (modelName.isEmpty)
              ModelService.activateModel("", userId)
            else
              ModelService.activateModel(modelName, userId)
          }
        }

        case Action.DEACTIVATEMODEL => {
          val modelName: String = extraCmdArgs.getOrElse(MODELNAME, "")

          response = {
            if (modelName.isEmpty)
              ModelService.deactivateModel("", userId)
            else
              ModelService.deactivateModel(modelName, userId)
          }
        }

        case Action.UPDATEMODELKPMML => response = ModelService.updateModelKPmml(input, userId, tid, paramStr)
        case Action.UPDATEMODELJTM => response = ModelService.updateModelJTM(input, userId, tid, if (param == null || param.trim.size == 0) None else Some(param.trim), paramStr)

        case Action.UPDATEMODELPMML => {
          val modelName = extraCmdArgs.getOrElse(MODELNAME, "")
          val modelVer = extraCmdArgs.getOrElse(MODELVERSION, null)
          var validatedNewVersion: String = if (modelVer != null) MdMgr.FormatVersion(modelVer) else null
          response = ModelService.updateModelPmml(input, userId, modelName, validatedNewVersion, tid, paramStr)
        }

        case Action.UPDATEMODELPYTHON => {
            val modelName : String = extraCmdArgs.getOrElse(MODELNAME,"there was no modelName supplied")
            /** assumption here is that the file name endswith(".py")... we want the stem of the file name for moduleName */
            val moduleName : String = input.split('.').dropRight(1).last
            val pkgQualifiedName : String = s"$moduleName.$modelName"
            val modelOptions : String = extraCmdArgs.getOrElse(MODELOPTIONS, "{}")
            val modelVer = extraCmdArgs.getOrElse(MODELVERSION, null)
            var validatedNewVersion: String = if (modelVer != null) MdMgr.FormatVersion(modelVer) else null
            response = ModelService.updateModelPython(input, userId, pkgQualifiedName, validatedNewVersion, tid, paramStr, Some(modelOptions))
        }
        case Action.UPDATEMODELJYTHON => {
            val modelName : String = extraCmdArgs.getOrElse(MODELNAME,"there was no modelName supplied")
            /** assumption here is that the file name endswith(".py")... we want the stem of the file name for moduleName */
            val moduleName : String = input.split('.').dropRight(1).last
            val pkgQualifiedName : String = s"$moduleName.$modelName"
            val modelVer = extraCmdArgs.getOrElse(MODELVERSION, null)
            val modelOptions : String = extraCmdArgs.getOrElse(MODELOPTIONS, "{}")
            var validatedNewVersion: String = if (modelVer != null) MdMgr.FormatVersion(modelVer) else null
            response = ModelService.updateModelJython(input, userId, pkgQualifiedName, validatedNewVersion, tid, paramStr, Some(modelOptions))
        }

        case Action.UPDATEMODELSCALA => {
          if (param.length == 0)
            response = ModelService.updateModelscala(input, "", userId, tid, paramStr)
          else
            response = ModelService.updateModelscala(input, param, userId, tid, paramStr)
        }

        case Action.UPDATEMODELJAVA => {
          if (param.length == 0)
            response = ModelService.updateModeljava(input, "", userId, tid, paramStr)
          else
            response = ModelService.updateModeljava(input, param, userId, tid, paramStr)
        }

        case Action.GETALLMODELS => response = ModelService.getAllModels(userId, tid)
        case Action.GETMODEL => response = {
          val modelName: String = extraCmdArgs.getOrElse(MODELNAME, "")
          if (modelName.isEmpty)
            ModelService.getModel("", userId, tid)
          else
            ModelService.getModel(modelName, userId, tid)
        }

        //container management
        case Action.ADDCONTAINER => response = ContainerService.addContainer(input, tid, paramStr)
        case Action.UPDATECONTAINER => response = ContainerService.updateContainer(input, tid, paramStr)
        case Action.GETCONTAINER => response = {
          val containerName: String = extraCmdArgs.getOrElse(CONTAINERNAME, "")
          if (containerName.isEmpty)
            ContainerService.getContainer("", tid)
          else
            ContainerService.getContainer(containerName, tid)
        }

        case Action.GETALLCONTAINERS => response = ContainerService.getAllContainers(tid)
        case Action.REMOVECONTAINER => {
          val containerName: String = extraCmdArgs.getOrElse(CONTAINERNAME, "")
          if (containerName.isEmpty)
            response = ContainerService.removeContainer()
          else
            response = ContainerService.removeContainer(containerName)
        }

        //Type management
        // 1116 - Changes begin
        case Action.ADDTYPE => {
          response = new ApiResult(ErrorCodeConstants.Success, "StartMetadataAPI/route", null, s"The action = $action is no longer supported").toString
        }
        // 1116 - Changes end - The above line is commented since the TYPE actions is deprecated
        case Action.GETTYPE => response = {
          val typeName: String = extraCmdArgs.getOrElse(TYPENAME, "")
          if (typeName.isEmpty)
            TypeService.getType()
          else
            TypeService.getType(typeName)
        }

        case Action.GETALLTYPES => response = TypeService.getAllTypes
        // 1116 - Changes begin
        case Action.REMOVETYPE => {
          response = new ApiResult(ErrorCodeConstants.Success, "StartMetadataAPI/route", null, s"The action = $action is no longer supported").toString
        }
        case Action.LOADTYPESFROMAFILE => {
          response = new ApiResult(ErrorCodeConstants.Success, "StartMetadataAPI/route", null, s"The action = $action is no longer supported").toString
        }
        // 1116 - Changes end - The above line is commented since the TYPE actions is deprecated
        case Action.DUMPALLTYPESBYOBJTYPEASJSON => response = TypeService.dumpAllTypesByObjTypeAsJson

        //function management
        case Action.ADDFUNCTION => response = FunctionService.addFunction(input)
        case Action.GETFUNCTION => response = {
          val fcnName: String = extraCmdArgs.getOrElse(FUNCTIONNAME, "")
          if (fcnName.isEmpty)
            FunctionService.getFunction()
          else
            FunctionService.getFunction(fcnName)

        }
        case Action.REMOVEFUNCTION => response = {
          val fcnName: String = extraCmdArgs.getOrElse(FUNCTIONNAME, "")
          if (fcnName.isEmpty)
            FunctionService.removeFunction()
          else
            FunctionService.removeFunction(fcnName)
        }

        case Action.UPDATEFUNCTION => response = FunctionService.updateFunction(input)
        // 1295 Changes begin
        case Action.LOADFUNCTIONSFROMAFILE => {
          response = new ApiResult(ErrorCodeConstants.Success, "StartMetadataAPI/route", null, s"The action = $action is no longer supported, please use add function").toString
        }
          //response = FunctionService.loadFunctionsFromAFile(input)
          // 1295 Changes end
        case Action.DUMPALLFUNCTIONSASJSON => response = FunctionService.dumpAllFunctionsAsJson

        //config
        case Action.UPLOADCLUSTERCONFIG => response = ConfigService.uploadClusterConfig(input)
        case Action.UPLOADCOMPILECONFIG => response = ConfigService.uploadCompileConfig(input)
        case Action.DUMPALLCFGOBJECTS => response = ConfigService.dumpAllCfgObjects
        case Action.REMOVEENGINECONFIG => response = ConfigService.removeEngineConfig(input)

        // adapter message bindings
        case Action.ADDADAPTERMESSAGEBINDING => {
          val bindingString: String = extraCmdArgs.getOrElse(FROMSTRING, "")
          val bindingFilePath: String = extraCmdArgs.getOrElse(FROMFILE, "")
          fileinquesiton = bindingFilePath
          if (bindingString.nonEmpty && bindingFilePath.nonEmpty) {
            println("Currently only a file specification OR a string specification for the bindings are permitted, not both.")
            throw new RuntimeException(s"Currently only a file specification or a string specification for the bindings are permitted, not both.")
          } else if (bindingString.nonEmpty) {
            response = AdapterMessageBindingService.addFromInlineAdapterMessageBinding(bindingString, userId)
          } else if (bindingFilePath.nonEmpty) {
            response = AdapterMessageBindingService.addFromFileAnAdapterMessageBinding(bindingFilePath, userId)
          } else {
            println(s"Add Adapter Message Binding invocation is fouled up.  Check the syntax: \nkamanja <apiconfig> add adaptermessagebinding <binding spec from file path with json content | binding spec from inline json string> <file path|string>")
            throw new RuntimeException(s"Add Adapter Message Binding invocation is fouled up.  Check the syntax: \nkamanja <apiconfig> add adaptermessagebinding <bindingfromfile|bindingfromstring> <file path|string>")
          }

        }
        case Action.UPDATEADAPTERMESSAGEBINDING => {
          response = AdapterMessageBindingService.updateAdapterMessageBinding(input, userId)
        }
        case Action.REMOVEADAPTERMESSAGEBINDING => {
          val bindingKey: String = extraCmdArgs.getOrElse(Action.REMOVEADAPTERMESSAGEBINDING.toString, "")
          if (bindingKey.nonEmpty) {
            response = AdapterMessageBindingService.removeAdapterMessageBinding(bindingKey, userId)
          } else {

            val bindingString: String = extraCmdArgs.getOrElse(FROMSTRING, "")
            val bindingFilePath: String = extraCmdArgs.getOrElse(FROMFILE, "")
            fileinquesiton = bindingFilePath
            val invalidArgs: Boolean = (bindingString.nonEmpty && bindingFilePath.nonEmpty) || (bindingString.isEmpty && bindingFilePath.isEmpty)
            if (invalidArgs) {
              println(s"Remove Adapter Message Binding - invalid arguments supplied.  Check the syntax.  One of three ways are permissible:\nkamanja <apiconfig> remove adaptermessagebinding FROMFILE <file path with json content for binding key(s)>\nkamanja <apiconfig> remove adaptermessagebinding FROMSTRING '[ <json array item or items of the form adapterName,namespace.messageName,namespace.serializerName> ]'\nkamanja <apiconfig> remove adaptermessagebinding KEY '<adapterName,namespace.messageName,namespace.serializerName>'")
              throw new RuntimeException(s"Remove Adapter Message Binding - invalid arguments supplied.  Check the syntax.  One of three ways are permissible:\nkamanja <apiconfig> remove adaptermessagebinding FROMFILE <file path with json content for binding key(s)>\nkamanja <apiconfig> remove adaptermessagebinding FROMSTRING '[ <json array item or items of the form adapterName,namespace.messageName,namespace.serializerName> ]'\nkamanja <apiconfig> remove adaptermessagebinding KEY '<adapterName,namespace.messageName,namespace.serializerName>'")
            } else if (bindingString.nonEmpty) {
              response = AdapterMessageBindingService.removeFromInlineAdapterMessageBinding(bindingString, userId)
            } else if (bindingFilePath.nonEmpty) {
              response = AdapterMessageBindingService.removeFromFileAnAdapterMessageBinding(bindingFilePath, userId)
            }
          }
        }



        case Action.LISTADAPTERMESSAGEBINDINGS => {
          val adapterfilter: String = extraCmdArgs.getOrElse(ADAPTERFILTER, "")
          val messagefilter: String = extraCmdArgs.getOrElse(MESSAGEFILTER, "")
          val serializerfilter: String = extraCmdArgs.getOrElse(SERIALIZERFILTER, "")

          val filterCnt: Int = (if (adapterfilter.nonEmpty) 1 else 0) +
            (if (messagefilter.nonEmpty) 1 else 0) +
            (if (serializerfilter.nonEmpty) 1 else 0)
          response = if (filterCnt == 0) {
            AdapterMessageBindingService.ListAllAdapterMessageBindings
          } else {
            if (filterCnt == 1) {
              if (adapterfilter.nonEmpty) {
                AdapterMessageBindingService.ListBindingsForAdapter(adapterfilter)
              } else if (messagefilter.nonEmpty) {
                AdapterMessageBindingService.ListBindingsForMessage(messagefilter)
              } else {
                AdapterMessageBindingService.ListBindingsUsingSerializer(serializerfilter)
              }
            } else {
              println("Currently only one filter is permitted for the ListAdapterMessageBindings cmd")
              throw new RuntimeException(s"Currently only one filter is permitted for the ListAdapterMessageBindings cmd")
            }
          }
        }

        //concept
        case Action.ADDCONCEPT => response = ConceptService.addConcept(input)
        case Action.REMOVECONCEPT => response = {
          if (param.length == 0)
            ConceptService.removeConcept("", userId)
          else
            ConceptService.removeConcept(param, userId)

        }

        case Action.LOADCONCEPTSFROMAFILE => response = ConceptService.loadConceptsFromAFile
        case Action.DUMPALLCONCEPTSASJSON => response = ConceptService.dumpAllConceptsAsJson

        //jar
        case Action.UPLOADJAR => response = JarService.uploadJar(input)

        //dumps
        case Action.DUMPMETADATA => response = DumpService.dumpMetadata
        case Action.DUMPALLNODES => response = DumpService.dumpAllNodes
        case Action.DUMPALLCLUSTERS => response = DumpService.dumpAllClusters
        case Action.DUMPALLCLUSTERCFGS => response = DumpService.dumpAllClusterCfgs
        case Action.DUMPALLADAPTERS => response = DumpService.dumpAllAdapters
        case Action.GETTYPEBYSCHEMAID => response = {
          val schemaId: String = extraCmdArgs.getOrElse(SCHEMAID, "")
          if (schemaId.isEmpty) throw new Exception("Please provide the SchemaId");
          TypeService.getTypeBySchemaId(schemaId)
        }
        case Action.GETTYPEBYELEMENTID => response = {
          val elementId: String = extraCmdArgs.getOrElse(ELEMENTID, "")
          if (elementId.isEmpty) throw new Exception("Please provide the ElementId");
          TypeService.getTypeByElementId(elementId)
        }

        case _ => {
          println(s"Unexpected action! action=$action")
          throw new RuntimeException(s"Unexpected action! action=$action")
        }
      }
    } catch {
      case e: java.util.NoSuchElementException => {
        logger.error("Unable to access route: " + fileinquesiton)
        response = new ApiResult(-1, "StartMetadataAPI/route", null, s"Unable to execute command for action = $action").toString
      }
      case fio: java.io.FileNotFoundException => {
        logger.error("Unable to access file: " + fileinquesiton)
        response = new ApiResult(-1, "StartMetadataAPI/route", null, s"Unable to execute command for action = $action").toString
      }

      case e: Exception => {
        logger.warn("", e)

        /** tentative answer of unidentified command type failure. */
        response = new ApiResult(-1, "StartMetadataAPI/route", null, s"Unexpected action! action = $action").toString

        /**
          * one more try ... going the alternate route.
          *
          * ''Do we still need this ?'' Let's keep it for now.
          */
        /*
        val altResponse: String = AltRoute(originalArgs)
        if (altResponse != null) {
            //response = altResponse  ... typically a parse error that is only meaningful for AltRoute processing
            println(response)
            sys.exit(1)
         } else {
          /* if the AltRoute doesn't produce a valid result, we will complain with the original failure */
          printf(response)
          sys.exit(1)
        }
        */
      }

    }
    response
  }

  /** NOT USED
    * AltRoute is invoked only if the 'Action.withName(action.trim)' method fails to discern the appropriate
    * MetadataAPI method to invoke.  The command argument array is reconsidered with the AlternateCmdParser
    * If it produces valid command arguments (a command name and Map[String,String] of arg name/values) **and**
    * it is a command that we currently support with this mechanism (JPMML related commands are currently supported),
    * the service module is invoked.
    *
    * @param origArgs an Array[String] containing all of the arguments (sans debug if present) originally submitted
    * @return the response from successfully recognized commands (good or bad) or null if this mechanism couldn't
    *         make a determination of which command to invoke.  In that case a null is returned and the original
    *         complaint is returned to the caller.
    *
    */
  /* def AltRoute(origArgs: Array[String]): String = {

     /** trim off the config argument and if debugging the "debug" argument as well */
     val argsSansConfig: Array[String] = if (origArgs != null && origArgs.size > 0 && origArgs(0).toLowerCase == "debug") {
       origArgs.tail.tail
     } else {
       origArgs.tail
     }

     /** Put the command back together */
     val buffer: StringBuilder = new StringBuilder
     argsSansConfig.addString(buffer, " ")
     val originalCmd: String = buffer.toString

     var response: String = ""
     try {
       /** Feed the command string to the alternate parser. If successful, the cmdName will be valid string. */
       val (optCmdName, argMap): (Option[String], Map[String, String]) = AlternateCmdParser.parse(originalCmd)
       val cmdName: String = optCmdName.orNull
       response = if (cmdName != null) {
         /** See if it is one of the **supported** alternate commands */
         val cmd: String = cmdName.toLowerCase

         val resp: String = cmd match {
           case "addmodel" => {
             val modelTypeToBeAdded: String = if (argMap.contains("type")) argMap("type").toLowerCase else null
             if (modelTypeToBeAdded != null && modelTypeToBeAdded == "pmml") {

               val modelName: Option[String] = if (argMap.contains("name")) Some(argMap("name")) else None
               val modelVer: String = if (argMap.contains("modelversion")) argMap("modelversion") else null
               val msgName: Option[String] = if (argMap.contains("message")) Some(argMap("message")) else None
               /** it is permissable to not supply the messageversion... the latest version is assumed in that case */
               val msgVer: String = if (argMap.contains("messageversion")) argMap("messageversion") else MdMgr.LatestVersion
               val pmmlSrc: Option[String] = if (argMap.contains("pmml")) Some(argMap("pmml")) else None
               val pmmlPath: String = pmmlSrc.orNull
               val tid: Option[String] = if (argMap.contains("tenantid")) Some(argMap("tenantid")) else None

               var validatedModelVersion: String = null
               var validatedMsgVersion: String = null
               try {
                 validatedModelVersion = if (modelVer != null) MdMgr.FormatVersion(modelVer) else null
                 validatedMsgVersion = if (msgVer != null) MdMgr.FormatVersion(msgVer) else null
               } catch {
                 case e: Exception => throw (new RuntimeException(s"The version parameter is invalid... either not numeric or out of range...modelversion=$modelVer, messageversion=$msgVer", e))
               }
               val optModelVer: Option[String] = Option(validatedModelVersion)
               val optMsgVer: Option[String] = Option(validatedMsgVersion)

               ModelService.addModelPmml(ModelType.PMML, pmmlPath, Some("kamanja"), modelName, optModelVer, msgName, optMsgVer, tid)

             } else {
               null
             }
           }
           case "updatemodel" => {
             // updateModel type(jpmml) name(com.anotherCo.jpmml.DahliaRandomForest) newVersion(000000.000001.000002) oldVersion(000000.000001.000001) pmml(/anotherpath/prettierDahliaRandomForest.xml)  <<< NOT AVAILABLE (YET) update an explicit model version... doesn't have to be latest
             // updateModel type(jpmml) name(com.anotherCo.jpmml.DahliaRandomForest) newVersion(000000.000001.000002) pmml(/anotherpath/prettierDahliaRandomForest.xml)  <<< default to the updating the latest model version there.

             val modelTypeToBeUpdated: String = if (argMap.contains("type")) argMap("type").toLowerCase else null
             if (modelTypeToBeUpdated != null && modelTypeToBeUpdated == "pmml") {

               val optModelName: Option[String] = if (argMap.contains("name")) Some(argMap("name")) else None
               val newVer: String = if (argMap.contains("newversion")) argMap("newversion") else null
               /** it is permissable to not supply the old version... we just ask for update of the latest version in that case */
               val oldVer: String = if (argMap.contains("oldversion")) argMap("oldversion") else MdMgr.LatestVersion
               if (oldVer != MdMgr.LatestVersion) {
                 val warningMsg: String = "Specific version replacement is not currently supported.  Only the latest version of a model may be updated........"
                 logger.warn(warningMsg)
                 warningMsg
               } else {

                 val pmmlSrc: Option[String] = if (argMap.contains("pmml")) Some(argMap("pmml")) else None
                 val pmmlPath: String = pmmlSrc.orNull

                 /**
                  * NOTE: Despite the presence of the oldVer, it is currently not supported.  The metadata
                  * manager is not supporting specific version replacement with update.  Only the "latest"
                  * version of the model can be changed.  That said, we leave this in place for now until
                  * it has been determined if the verion will become an active part of the metadata
                  * key that manages models (and messages, containers, and the rest)
                  */

                 /** Use FormatVersion to normalize the string representation ... padding with appropriate 0's etc. */
                 var validatedOldVersion: String = null
                 var validatedNewVersion: String = null
                 try {
                   validatedOldVersion = if (oldVer != null && oldVer != MdMgr.LatestVersion) MdMgr.FormatVersion(oldVer)
                   else {
                     if (oldVer == MdMgr.LatestVersion) {
                       MdMgr.LatestVersion
                     } else {
                       null
                     }
                   }
                   validatedNewVersion = if (newVer != null) MdMgr.FormatVersion(newVer) else null
                 } catch {
                   case e: Exception => throw (new RuntimeException(s"One or more version parameters are invalid... oldVer=$oldVer, newVer=$newVer", e))
                 }
                 val optOldVer: Option[String] = Option(validatedOldVersion)

                 /** modelnamespace.modelname expected for modelName value */
                 val modelName: String = optModelName.orNull
                 var tid: Option[String] = if (argMap.contains("tenantid")) Some(argMap("tenantid")) else None
                 ModelService.updateModelPmml(pmmlPath, Some("kamanja"), modelName, validatedNewVersion, tid)
               }
             } else {
               null
             }
           }

         }
         resp
       } else {
         null
       }
     } catch {
       case e: Exception =>
         logger.debug(s"Exception seen ... e=${e.toString}", e)
         response = ""
     }

     response
   } */
}
