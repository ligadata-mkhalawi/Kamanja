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

package com.ligadata.MetadataAPI.Utility

import java.io.{ FileNotFoundException, File }

import com.ligadata.Exceptions.{ AlreadyExistsException }
import com.ligadata.MetadataAPI.{ TypeUtils, ErrorCodeConstants, ApiResult, MetadataAPIImpl }
import com.ligadata.kamanja.metadata.BaseTypeDef

import scala.io.Source

import org.apache.logging.log4j._

import scala.io._

/**
 * Created by dhaval on 8/12/15.
 */
object TypeService {
  private val userid: Option[String] = Some("kamanja")
  val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  // 646 - 676 Change begins - replase MetadataAPIImpl
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Chagne ends

  def addType(input: String): String = {
    var response = ""
    var typeFileDir: String = ""
    //val gitMsgFile = "https://raw.githubusercontent.com/ligadata-dhaval/Kamanja/master/HelloWorld_Msg_Def.json"
    if (input == "") {
      typeFileDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("TYPE_FILES_DIR")
      if (typeFileDir == null) {
        response = "TYPE_FILES_DIR property missing in the metadata API configuration"
      } else {
        //verify the directory where messages can be present
        IsValidDir(typeFileDir) match {
          case true => {
            //get all files with json extension
            val types: Array[File] = new java.io.File(typeFileDir).listFiles.filter(_.getName.endsWith(".json"))
            types.length match {
              case 0 => {
                println("Types not found at " + typeFileDir)
                "Types not found at " + typeFileDir
              }
              case option => {
                val typeDefs = getUserInputFromMainMenu(types)
                for (typeDef <- typeDefs) {
                  response += getMetadataAPI.AddTypes(typeDef.toString, "JSON", userid)
                }
              }
            }
          }
          case false => {
            //println("Message directory is invalid.")
            response = "Message directory is invalid."
          }
        }
      }
    } else {
      //input provided
      var message = new File(input.toString)
      if (message.exists()) {
        val typeDef = Source.fromFile(message).mkString
        response = getMetadataAPI.AddTypes(typeDef.toString, "JSON", userid)

      } else {
        response = "File does not exist"
      }
    }
    response
  }

  /**
   * Retrieve the type info for the namespace.name.version supplied
   * @param param namespace.name.version
   * @return string result
   */
  def getType(param: String = ""): String = {
    val response: String = if (param.length > 0) {
      val (ns, name, ver) = com.ligadata.kamanja.metadata.Utils.parseNameToken(param)
      val (typeString, ok): (String, Boolean) = try {
        val optTypeInfo: Option[BaseTypeDef] = TypeUtils.GetType(ns, name, ver, "JSON", userid)
        val typeInfo: BaseTypeDef = optTypeInfo match {
          case Some(optTypeInfo) => optTypeInfo
          case _                 => null
        }
        val (typeStr, succeeded): (String, Boolean) = if (typeInfo != null) {
          val result = s"${typeInfo.FullNameWithVer}, ${typeInfo.PhysicalName}"
          (result, true)
        } else {
          (s"no type named $param", false)
        }
        (typeStr, succeeded)
      } catch {
        case e: Exception => {
          logger.error("", e)
          (s"no type named $param", false)
        }
      }
      val isItOk: Int = if (ok) ErrorCodeConstants.Success else ErrorCodeConstants.Failure
      val resultStr: String = new ApiResult(isItOk, "getType", null, typeString).toString
      resultStr
    } else {
      val result: String = try {
        val typeKeys = getMetadataAPI.GetAllKeys("TypeDef", None)
        val (msg, ok): (String, Boolean) = if (typeKeys.length == 0) {
          val errorMsg = "Sorry, No types available, in the Metadata, to display!"
          (errorMsg, false)
        } else {
          println("\nPick the type to be displayed from the following list: ")
          var srno = 0
          for (typeKey <- typeKeys) {
            srno += 1
            println("[" + srno + "] " + typeKey)
          }
          println("Enter your choice: ")
          val choice: Int = readInt()

          val (resp, ok): (String, Boolean) = if (choice < 1 || choice > typeKeys.length) {
            val errormsg = "Invalid choice " + choice + ". Start with the main menu."
            (errormsg, false)
          } else {
            val typeKey = typeKeys(choice - 1)
            val typeKeyTokens = typeKey.split("\\.")
            val typeNameSpace = typeKeyTokens(0)
            val typeName = typeKeyTokens(1)
            val typeVersion = typeKeyTokens(2)
            val optTypeInfo: Option[BaseTypeDef] = TypeUtils.GetType(typeNameSpace, typeName, typeVersion, "JSON", userid)
            val typeInfo: BaseTypeDef = optTypeInfo match {
              case Some(optTypeInfo) => optTypeInfo
              case _                 => null
            }
            val (typeStr, ok): (String, Boolean) = if (typeInfo != null) {
              (s"${typeInfo.FullNameWithVer}, ${typeInfo.PhysicalName}", true)
            } else {
              (s"no type named $param", false)
            }
            (typeStr, ok)
          }
          (resp, ok)
        }
        val isItOk: Int = if (ok) ErrorCodeConstants.Success else ErrorCodeConstants.Failure
        val resultStr: String = new ApiResult(isItOk, "getType", null, msg).toString
        resultStr
      } catch {
        case e: Exception => {
          logger.info("", e)
          e.getStackTrace.toString
        }
      }
      result
    }
    response
  }

  def getAllTypes: String = {
    getMetadataAPI.GetAllTypes("JSON", userid)
  }

  def removeType(param: String = ""): String = {
    var response = ""
    try {
      if (param.length > 0) {
        val (ns, name, ver) = com.ligadata.kamanja.metadata.Utils.parseNameToken(param)
        try {
          return getMetadataAPI.RemoveType(ns, name, ver.toLong, userid).toString
        } catch {
          case e: Exception => logger.error("", e)
        }
      }
      val typeKeys = getMetadataAPI.GetAllKeys("TypeDef", None)

      if (typeKeys.length == 0) {
        val errorMsg = "Sorry, No types available, in the Metadata, to delete!"
        //println(errorMsg)
        response = errorMsg
      } else {
        println("\nPick the type to be deleted from the following list: ")
        var srno = 0
        for (modelKey <- typeKeys) {
          srno += 1
          println("[" + srno + "] " + modelKey)
        }
        println("Enter your choice: ")
        val choice: Int = readInt()

        if (choice < 1 || choice > typeKeys.length) {
          val errormsg = "Invalid choice " + choice + ". Start with the main menu."
          //println(errormsg)
          response = errormsg
        }
        val typeKey = typeKeys(choice - 1)
        val (typeNameSpace, typeName, typeVersion) = com.ligadata.kamanja.metadata.Utils.parseNameToken(typeKey)
        response = getMetadataAPI.RemoveType(typeNameSpace, typeName, typeVersion.toLong, userid).toString
      }

    } catch {
      case e: Exception => {
        //logger.error("", e)
        logger.info("", e)
        response = e.getStackTrace.toString
      }
    }
    response
  }

  /**
   * loadTypesFromAFile is used to load type information to the Metadata store for use principally by
   * the kamanja pmml models.
   *
   * @param input path of the file containing the json function definitions
   * @param userid optional user id needed for authentication and logging
   * @return api results as a string
   */
  def loadTypesFromAFile(input: String, userid: Option[String] = None): String = {
    val response: String = try {
      val jsonTypeStr: String = Source.fromFile(input).mkString
      val apiResult = getMetadataAPI.AddTypes(jsonTypeStr, "JSON", userid)

      val resultMsg: String = s"Result as Json String => \n$apiResult"
      println(resultMsg)
      resultMsg
    } catch {
      case fnf: FileNotFoundException => {
        val filePath: String = if (input != null && input.nonEmpty) input else "bad file path ... blank or null"
        val errorMsg: String = "file supplied to loadTypesFromAFile ($filePath) does not exist...."
        logger.error(errorMsg, fnf)
        errorMsg
      }
      case e: Exception => {
        val errorMsg: String = s"Exception $e encountered ..."
        logger.debug(errorMsg, e)
        errorMsg
      }
    }
    response
  }

  def dumpAllTypesByObjTypeAsJson: String = {
    var response = ""
    try {
      val typeMenu = Map(1 -> "ScalarTypeDef",
        2 -> "ArrayTypeDef",
        3 -> "ArrayBufTypeDef",
        4 -> "SetTypeDef",
        5 -> "TreeSetTypeDef",
        6 -> "AnyTypeDef",
        7 -> "SortedSetTypeDef",
        8 -> "MapTypeDef",
        9 -> "HashMapTypeDef",
        10 -> "ImmutableMapTypeDef",
        11 -> "ListTypeDef",
        12 -> "QueueTypeDef",
        13 -> "TupleTypeDef")
      var selectedType = "com.ligadata.kamanja.metadata.ScalarTypeDef"
      var done = false
      while (done == false) {
        println("\n\nPick a Type ")
        var seq = 0
        typeMenu.foreach(key => { seq += 1; println("[" + seq + "] " + typeMenu(seq)) })
        seq += 1
        println("[" + seq + "] Main Menu")
        print("\nEnter your choice: ")
        val choice: Int = readInt()
        if (choice <= typeMenu.size) {
          selectedType = "com.ligadata.kamanja.metadata." + typeMenu(choice)
          done = true
        } else if (choice == typeMenu.size + 1) {
          done = true
        } else {
          logger.error("Invalid Choice : " + choice)
        }
      }

      response = getMetadataAPI.GetAllTypesByObjType("JSON", selectedType)
    } catch {
      case e: Exception => {
        logger.info("", e)
        response = e.getStackTrace.toString
      }
    }
    response
  }

  //utility
  def IsValidDir(dirName: String): Boolean = {
    val iFile = new File(dirName)
    if (!iFile.exists) {
      println("The File Path (" + dirName + ") is not found: ")
      false
    } else if (!iFile.isDirectory) {
      println("The File Path (" + dirName + ") is not a directory: ")
      false
    } else
      true
  }

  def getUserInputFromMainMenu(messages: Array[File]): Array[String] = {
    var listOfMsgDef: Array[String] = Array[String]()
    var srNo = 0
    println("\nPick a Type Definition file(s) from below choices\n")
    for (message <- messages) {
      srNo += 1
      println("[" + srNo + "]" + message)
    }
    print("\nEnter your choice(If more than 1 choice, please use commas to seperate them): \n")
    val userOptions: List[Int] = readLine().filter(_ != '\n').split(',').filter(ch => (ch != null && ch != "")).map(_.trim.toInt).toList
    //check if user input valid. If not exit
    for (userOption <- userOptions) {
      userOption match {
        case userOption if (1 to srNo).contains(userOption) => {
          //find the file location corresponding to the message
          var message = messages(userOption - 1)
          //process message
          val messageDef = Source.fromFile(message).mkString
          listOfMsgDef = listOfMsgDef :+ messageDef
        }
        case _ => {
          println("Unknown option: ")
        }
      }
    }
    listOfMsgDef
  }

  def getTypeBySchemaId(schemaId: String): String = {

    if (!scala.util.Try(schemaId.toInt).isSuccess) {
      val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetTypeBySchemaId", null, "Please provide proper schema id :" + schemaId)
      return apiResult.toString()
    }
    val response: String = getMetadataAPI.GetTypeBySchemaId(schemaId.toInt, userid)
    response
  }

  def getTypeByElementId(elementId: String): String = {
    if (!scala.util.Try(elementId.toLong).isSuccess) {
      val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetTypeByElementId", null, "Please provide proper element id :" + elementId)
      return apiResult.toString()
    }
    val response: String = getMetadataAPI.GetTypeByElementId(elementId.toLong, userid)
    response
  }
}
