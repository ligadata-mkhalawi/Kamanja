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

import java.io.File

import com.ligadata.Exceptions.InvalidArgumentException
import com.ligadata.MetadataAPI.{MetadataAPIImpl,ApiResult,ErrorCodeConstants}

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import org.apache.logging.log4j._

import scala.io._

/**
 * Created by dhaval on 8/7/15.
 */

object MessageService {
  private val userid: Option[String] = Some("kamanja")
  val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  // 646 - 676 Change begins - replase MetadataAPIImpl
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Chagne ends

  def addMessage(input: String, tid: Option[String], paramStr : Option[String]): String = {
    var response = ""
    var msgFileDir: String = ""


    //val gitMsgFile = "https://raw.githubusercontent.com/ligadata-dhaval/Kamanja/master/HelloWorld_Msg_Def.json"
    var chosen: String = ""
    var finalTid: Option[String] = None
    try{
    if (tid == None) {
        chosen = getTenantId
        finalTid = Some(chosen)
    } else {
      finalTid = tid
    }
    }catch {
      case e: InvalidArgumentException => {
        logger.error("Invalid choice")
        return (new ApiResult(ErrorCodeConstants.Failure, "addMessage",null, "Invalid choice")).toString
      }
    }


    if (input == "") {
      msgFileDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("MESSAGE_FILES_DIR")
      if (msgFileDir == null) {
        response = new ApiResult(ErrorCodeConstants.Failure,"addMessage",null,"MESSAGE_FILES_DIR property missing in the metadata API configuration").toString
        //response = "MESSAGE_FILES_DIR property missing in the metadata API configuration"
      } else {
        //verify the directory where messages can be present
        IsValidDir(msgFileDir) match {
          case true => {
            //get all files with json extension
            val messages: Array[File] = new java.io.File(msgFileDir).listFiles.filter(_.getName.endsWith(".json"))
            messages.length match {
              case 0 => {
                //println("Messages not found at " + msgFileDir)
                //"Messages not found at " + msgFileDir
                response=new ApiResult(ErrorCodeConstants.Failure,"addMessage",null,"Messages not found at "+msgFileDir).toString
              }
              case option => {
                val messageDefs = getUserInputFromMainMenu(messages)
                for (messageDef <- messageDefs) {
                  response += getMetadataAPI.AddMessage(messageDef.toString, "JSON", userid, finalTid, paramStr)
                }
              }
            }
          }
          case false => {
            //println("Message directory is invalid.")
            //response = "Message directory is invalid."
            response=new ApiResult(ErrorCodeConstants.Failure,"addMessage",null,"Message directory is invalid").toString
          }
        }
      }
    } else {
      //input provided
      var message = new File(input.toString)
      if(message.exists()){
        val messageDef = Source.fromFile(message).mkString
        response = getMetadataAPI.AddMessage(messageDef, "JSON", userid, finalTid, paramStr)
      }else{
        //response="Message defintion file does not exist"
        response=new ApiResult(ErrorCodeConstants.Failure,"addMessage",null,"Message defintion file does not exist").toString
      }
    }
    //Got the message. Now add them
    response
  }

  // 646 - Sub task 672, 676 Changes begin
  def getAllMessages (tid: Option[String] ) : String = {
    var response = ""
    var messageKeysList =""
    try {
      val messageKeys: Array[String] = getMetadataAPI.GetAllMessagesFromCache(true, userid, tid)
      if (messageKeys.length == 0) {
       var emptyAlert="Sorry, No messages are available in the Metadata"
        response =  (new ApiResult(ErrorCodeConstants.Success, "getAllMessages",null, emptyAlert)).toString
      } else {
       response= (new ApiResult(ErrorCodeConstants.Success, "getAllMessages", messageKeys.mkString(", ") , "Successfully retrieved all the messages")).toString
      }
    } catch {
      case e: Exception => {
        //logger.warn("", e)
        response= (new ApiResult(ErrorCodeConstants.Failure, "getAllMessages",null, e.getStackTrace.toString)).toString
      }
    }
    response
  }

  def updateMessage(input: String, tid: Option[String], pStr : Option[String]): String = {
    var response = ""
    //val gitMsgFile = "https://raw.githubusercontent.com/ligadata-dhaval/Kamanja/master/HelloWorld_Msg_Def.json"

    //val gitMsgFile = "https://raw.githubusercontent.com/ligadata-dhaval/Kamanja/master/HelloWorld_Msg_Def.json"
    var chosen: String = ""
    var finalTid: Option[String] = None
    try {
      if (tid == None) {
        chosen = getTenantId
        finalTid = Some(chosen)
      } else {
        finalTid = tid
      }
    }catch {
      case e: InvalidArgumentException => {
        logger.error("Invalid choice")
        return (new ApiResult(ErrorCodeConstants.Failure, "addMessage",null, "Invalid choice")).toString
      }
    }

    if (input == "") {
      val msgFileDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("MESSAGE_FILES_DIR")
      if (msgFileDir == null) {
        //response = "MESSAGE_FILES_DIR property missing in the metadata API configuration"
        response = new ApiResult(ErrorCodeConstants.Failure,"updateMessage",null,"MESSAGE_FILES_DIR property missing in the metadata API configuration").toString
      } else {
        //verify the directory where messages can be present
        IsValidDir(msgFileDir) match {
          case true => {
            //get all files with json extension
            val messages: Array[File] = new java.io.File(msgFileDir).listFiles.filter(_.getName.endsWith(".json"))
            messages.length match {
              case 0 => {
               // println("Messages not found at " + msgFileDir)
                //"Messages not found at " + msgFileDir
                response=new ApiResult(ErrorCodeConstants.Failure,"updateMessage",null,"Messages not found at "+msgFileDir).toString
              }
              case option => {
                val messageDefs = getUserInputFromMainMenu(messages)
                for (messageDef <- messageDefs) {
                  response += getMetadataAPI.UpdateMessage(messageDef.toString, "JSON", userid, finalTid, pStr)
                }
              }
            }
          }
          case false => {
            //println("Message directory is invalid.")
           // response = "Message directory is invalid."
            response=new ApiResult(ErrorCodeConstants.Failure,"updateMessage",null,"Message directory is invalid").toString
          }
        }
      }
    } else {
      //input provided
      var message = new File(input.toString)
      if(message.exists()){
        val messageDef = Source.fromFile(message).mkString
        response = getMetadataAPI.UpdateMessage(messageDef, "JSON", userid, finalTid, pStr)
      }else{
        //response="Message defintion file does not exist"
        response=new ApiResult(ErrorCodeConstants.Failure,"updateMessage",null,"Message defintion file does not exist").toString
      }
    }
    response
  }

  def removeMessage(parm: String = ""): String = {
    var response = ""
    try {
      if (parm.length > 0) {
         val(ns, name, ver) = com.ligadata.kamanja.metadata.Utils.parseNameToken(parm)
         try {
           return getMetadataAPI.RemoveMessage(ns, name, ver.toInt, userid)
         } catch {
           case e: Exception =>
             //logger.error("", e)
             response= (new ApiResult(ErrorCodeConstants.Failure, "removeMessage",null, e.getStackTrace.toString)).toString
         }
      }

      val messageKeys = getMetadataAPI.GetAllMessagesFromCache(true, None)

      if (messageKeys.length == 0) {
        //val errorMsg = "Sorry, No messages available, in the Metadata, to delete!"
        //response = errorMsg
        response = new ApiResult(ErrorCodeConstants.Failure, "removeMessage", null, "No messages in the metadata").toString
      }
      else {
        println("\nPick the message to be deleted from the following list: ")
        var srno = 0
        var count =0
        for (messageKey <- messageKeys) {
          count +=1
          srno += 1
          println("[" + srno + "] " + messageKey)
        }
        println("Enter your choice: ")
        val choice: Int = readInt()

        if (choice < 1 || choice > count) {
          //val errormsg = "Invalid choice " + choice + ". Start with the main menu."
          //response = errormsg
          response = new ApiResult(ErrorCodeConstants.Failure, "removeMessage", null, "Invalid choice").toString

        }
        else{
          val msgKey = messageKeys(choice - 1)
          val(msgNameSpace, msgName, msgVersion) = com.ligadata.kamanja.metadata.Utils.parseNameToken(msgKey)
          val apiResult = getMetadataAPI.RemoveMessage(msgNameSpace, msgName, msgVersion.toLong, userid).toString
          response = apiResult
        }

      }
    } catch {
      case e: Exception => {
        logger.warn("", e)
        response = e.getStackTrace.toString
      }
    }
    response
  }

  def getMessage(param: String= "", tid : Option[String] = None) : String = {
    try {
      var response=""
      if (param.length > 0) {
        val(ns, name, ver) = com.ligadata.kamanja.metadata.Utils.parseNameToken(param)
        try {
          return getMetadataAPI.GetMessageDef(ns, name, "JSON", ver,  userid, tid)
        } catch {
          case e: Exception =>
            //logger.error("", e)
            response= (new ApiResult(ErrorCodeConstants.Failure, "getMessage",null, e.getStackTrace.toString)).toString
        }
      }

      //    logger.setLevel(Level.TRACE); //check again

      //val msgKeys = getMetadataAPI.GetAllKeys("MessageDef", None)
      val msgKeys = getMetadataAPI.GetAllMessagesFromCache(true, None)
      if (msgKeys.length == 0) {
        //response="Sorry, No messages available in the Metadata"
        response = new ApiResult(ErrorCodeConstants.Failure, "getMessage", null, "No messages in the metadata").toString
      }else{
        println("\nPick the message to be presented from the following list: ")

        var seq = 0
        msgKeys.foreach(key => { seq += 1; println("[" + seq + "] " + key) })

        print("\nEnter your choice: ")
        val choice: Int = readInt()

        if (choice < 1 || choice > msgKeys.length) {
         // response = "Invalid choice " + choice + ",start with main menu..."
          response = new ApiResult(ErrorCodeConstants.Failure, "getMessage", null, "Invalid choice").toString
        }
        else{
          val msgKey = msgKeys(choice - 1)
          val(msgNameSpace, msgName, msgVersion) = com.ligadata.kamanja.metadata.Utils.parseNameToken(msgKey)
          val depModels = getMetadataAPI.GetDependentModels(msgNameSpace, msgName, msgVersion.toLong)
          logger.debug("DependentModels => " + depModels)

          logger.debug("DependentModels => " + depModels)

          val apiResult = getMetadataAPI.GetMessageDef(msgNameSpace, msgName, "JSON", msgVersion, userid, tid)

          //     val apiResultStr = getMetadataAPI.getApiResult(apiResult)
          response=apiResult
        }
      }
      response

    } catch {
      case e: Exception => {
        logger.warn("", e)
        e.getStackTrace.toString
      }
    }
  }

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

  @throws(classOf[InvalidArgumentException])
  private def getTenantId: String = {
    println("Select a tenant id:")
    var tenatns = getMetadataAPI.GetAllTenants(userid)
     getUserInputFromMainMenu(tenatns)
  }

  def getUserInputFromMainMenu(tenants: Array[String]) : String = {
    logger.debug("getUserInputFromMainMenu for tenant ids")
    var srNo = 0
    for(tenant <- tenants) {
      srNo += 1
      println("[" + srNo + "]" + tenant)
     }
     print("\nEnter your choice(If more than 1 choice, please use commas to seperate them): \n")
    val userOption: Int = readLine().trim.toInt
    if(userOption<1 || userOption > srNo){
      //(new ApiResult(ErrorCodeConstants.Failure, "getUserInputFromMainMenu(tenantid)",null, "Invalid choice")).toString
      logger.debug("Invalid choice")
        throw new InvalidArgumentException("Invalid choice",null)
    }
    else{
logger.debug("User option is: "+(userOption-1))
      tenants(userOption - 1)

    }
  }

  def   getUserInputFromMainMenu(messages: Array[File]): Array[String] = {
    var listOfMsgDef: Array[String] = Array[String]()
    var srNo = 0
    println("\nPick a Message Definition file(s) from below choices\n")
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
}
