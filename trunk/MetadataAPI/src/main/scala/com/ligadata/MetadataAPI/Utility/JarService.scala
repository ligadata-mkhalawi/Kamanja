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

import com.ligadata.MetadataAPI.{ApiResult, ErrorCodeConstants, MetadataAPIImpl}

import scala.io.Source
import org.apache.logging.log4j._

import scala.io._

/**
 * Created by dhaval on 8/13/15.
 */
object JarService {
  private val userid: Option[String] = Some("kamanja")
  // 646 - 676 Change begins - replase MetadataAPIImpl
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Chagne ends
  val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

def uploadJar(input: String): String ={
  var response = ""
  var jarFileDir: String = ""

  if (input == "") {
    try{
      jarFileDir = getMetadataAPI.GetMetadataAPIConfig.getProperty("JAR_TARGET_DIR")
    }catch {
      case e: Exception => {
        // logger.info("", e)
        //response=e.getStackTrace.toString
        response = (new ApiResult(ErrorCodeConstants.Failure, "activateModel", null, e.getStackTrace.toString)).toString
      }
    }

    if (jarFileDir == null) {
      //response = "JAR_TARGET_DIR property missing in the metadata API configuration"
      response= (new ApiResult(ErrorCodeConstants.Failure, "uploadJar",null,"JAR_TARGET_DIR property missing in the metadata API configuration")).toString
    } else {
      //verify the directory where messages can be present
      IsValidDir(jarFileDir) match {
        case true => {
          //get all files with json extension
          val jars: Array[File] = new java.io.File(jarFileDir).listFiles.filter(_.getName.endsWith(".jar"))
          jars.length match {
            case 0 => {
              //println("Jars not found at " + jarFileDir)
              //response="Jars not found at " + jarFileDir
              response= (new ApiResult(ErrorCodeConstants.Failure, "uploadJar",null,"Jars not found at " + jarFileDir)).toString
            }
            case option => {
              response = uploadJars(jars)
            }
          }
        }
        case false => {
          //response = "JAR directory is invalid."
          response= (new ApiResult(ErrorCodeConstants.Failure, "uploadJar ",null,"JAR directory is invalid.")).toString
        }
      }
    }
  } else {
    //input provided
    var jarFile = new File(input.toString)
    response = getMetadataAPI.UploadJar(jarFile.getPath)
  }
  response
}

  //utility  ???? move this to a utility directory, Dhaval!
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

  def   uploadJars(files: Array[File]): String = {
    var srNo = 0
    var results: String = ""
    println("\nPick a Jar Definition file(s) from below choices\n")
    for (file <- files) {
      srNo += 1
      println("[" + srNo + "]" + file)
    }

    print("\nEnter your choice(If more than 1 choice, please use commas to seperate them): \n")
    val userOptions: List[Int] = readLine().filter(_ != '\n').split(',').filter(ch => (ch != null && ch != "")).map(_.trim.toInt).toList

    //check if user input valid. If not exit
    //for (userOption <- userOptions) {
    userOptions.foreach(userOption =>  {
      if ((1 to srNo).contains(userOption)) {
         var file = files(userOption - 1)
         println("Uploading "+file.getPath)
         results = results + "\n" +getMetadataAPI.UploadJar(file.getPath)

      } else {
         println("Unknown option: " + userOption)
      }
    })
    results
  }
}
