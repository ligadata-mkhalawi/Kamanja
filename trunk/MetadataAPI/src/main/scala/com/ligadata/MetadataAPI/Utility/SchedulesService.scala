package com.ligadata.MetadataAPI.Utility

import java.io.File

import com.ligadata.MetadataAPI.MetadataAPIImpl
import org.apache.logging.log4j.LogManager

import scala.io.Source

/**
  * Created by Saleh on 8/25/2016.
  */
object SchedulesService {
  private val userid: Option[String] = Some("kamanja")
  val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI

  def addSchedule(input: String, tid: Option[String], paramStr: Option[String]): String = {
    var response = ""
    var schdualeFileDir: String = ""

    var chosen: String = ""
    var finalTid: Option[String] = None
//    if (tid == None) {
//      chosen = getTenantId
//      finalTid = Some(chosen)
//    } else {
//      finalTid = tid
//    }


    if (input == "") {
      schdualeFileDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("SCHEDULE_FILES_DIR")
      if (schdualeFileDir == null) {
        response = "SCHEDULE_FILES_DIR property missing in the metadata API configuration"
      } else {
        IsValidDir(schdualeFileDir) match {
          case true => {
            val schduales: Array[File] = new java.io.File(schdualeFileDir).listFiles.filter(_.getName.endsWith(".json"))
            schduales.length match {
              case 0 => {
                println("Schedules not found at " + schdualeFileDir)
                "Schedules not found at " + schdualeFileDir
              }
              case option => {
                val schdualeDefs = getUserInputFromMainMenu(schduales)
                for (schdualeDef <- schdualeDefs) {
                  response += getMetadataAPI.addSchedule(schdualeDef.toString, "JSON", userid, finalTid, paramStr)
                }
              }
            }
          }
          case false => {
            response = "Schedules directory is invalid."
          }
        }
      }
    } else {
      //input provided
      var message = new File(input.toString)
      if (message.exists()) {
        val messageDef = Source.fromFile(message).mkString
        response = getMetadataAPI.AddMessage(messageDef, "JSON", userid, finalTid, paramStr)
      } else {
        response = "Schedule defintion file does not exist"
      }
    }
    //Got the Schedule. Now add them
    response
  }

//  private def getTenantId: String = {
//    var tenatns = getMetadataAPI.GetAllTenants(userid)
//    return getUserInputFromMainMenu(tenatns)
//  }
//
//  def getUserInputFromMainMenu(tenants: Array[String]): String = {
//    var srNo = 0
//    for (tenant <- tenants) {
//      srNo += 1
//      println("[" + srNo + "]" + tenant)
//    }
//    print("\nEnter your choice(If more than 1 choice, please use commas to seperate them): \n")
//    val userOption: Int = readLine().trim.toInt
//    return tenants(userOption - 1)
//  }


  def getUserInputFromMainMenu(schduales: Array[File]): Array[String] = {
    var listOfMsgDef: Array[String] = Array[String]()
    var srNo = 0
    println("\nPick a Job Definition file(s) from below choices\n")
    for (message <- schduales) {
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
          var schduale = schduales(userOption - 1)
          //process message
          val schdualeDef = Source.fromFile(schduale).mkString
          listOfMsgDef = listOfMsgDef :+ schdualeDef
        }
        case _ => {
          println("Unknown option: ")
        }
      }
    }
    listOfMsgDef
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
}
