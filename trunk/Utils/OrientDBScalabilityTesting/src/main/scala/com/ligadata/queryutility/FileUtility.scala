package com.ligadata.scalabilityutility

import java.io.PrintWriter
import java.text.SimpleDateFormat

import com.ligadata.Exceptions.KamanjaException
import org.apache.commons.io.FilenameUtils
import org.json4s
import org.json4s.{JsonAST, DefaultFormats}
import org.json4s.JsonDSL._
//import org.json4s.jackson.JsonMethods._
import org.json4s.native.JsonMethods._
import scala.collection.immutable.Map
import scala.io.Source._

/**
  * Created by Yousef on 6/16/2016.
  */
class FileUtility extends LogTrait{
  case class databaseConfigFile(username: Option[String], password: Option[String], url: Option[String], numberOfVertices: Option[Int])

  def ParseFile(filePath: String): json4s.JValue ={//This method used to parse a config file (JSON format)
    try{
      val parsedFile = parse(filePath)
      return parsedFile
    } catch{
      case e: Exception => throw new KamanjaException(s"There is an error in the format of file \n ErrorMsg : ", e)
    }
  }

  def extractInfo(parsedValue: json4s.JValue): databaseConfigFile={ //This method used to extract data from config file
  implicit val formats = DefaultFormats
    val extractedObj = parsedValue.extract[databaseConfigFile]
    return extractedObj
  }

  def ReadFile(filePath: String): String ={//This method used to read a whole file (from header && pmml)
    return fromFile(filePath).mkString
  }

  def FileExist(filePath: String): Boolean={//This method used to check if file exists or not (return true if exists and false otherwise)
    return new java.io.File(filePath).exists
  }

  def createConfigBeanObj(configInfo: databaseConfigFile): ConfigBean={ //This method used to create a configObj
  var configBeanObj:ConfigBean = new ConfigBean()
    if(configInfo.username.get == None){
      logger.error("you should pass username in database config file")
      sys.exit(1)
    } else if(configInfo.username.get.trim == ""){
      logger.error("username cannot be null in config file")
      sys.exit(1)
    } else {
      configBeanObj.username_=(configInfo.username.get)
    }


    if(configInfo.password.get == None){
      logger.error("you should pass password in database config file")
      sys.exit(1)
    } else if(configInfo.password.get.trim == ""){
      logger.error("password cannot be null in config file")
      sys.exit(1)
    } else {
      configBeanObj.password_=(configInfo.password.get)
    }


    if(configInfo.url.get == None){
      logger.error("you should pass url in database config file")
      sys.exit(1)
    } else if(configInfo.url.get.trim == ""){
      logger.error("url cannot be null in config file")
      sys.exit(1)
    } else {
      configBeanObj.url_=(configInfo.url.get)
    }

    if(configInfo.numberOfVertices.get == None){
      logger.info("The defult value for numberOfVertices is 1000")
      logger.info("[RESULT] - The defult value for numberOfVertices is 1000")
      configBeanObj.numberOfVertices_=(1000)
    } else {
      configBeanObj.numberOfVertices_=(configInfo.numberOfVertices.get)
    }


    return configBeanObj
  }

}
