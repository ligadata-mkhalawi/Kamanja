package com.ligadata.pluginsutility

import java.io.PrintWriter
import java.text.SimpleDateFormat

import com.ligadata.Exceptions.KamanjaException
import org.apache.commons.io.FilenameUtils
import org.json4s
import org.json4s.{JsonAST, DefaultFormats}
import org.json4s.native.JsonMethods._
import scala.io.Source._
/**
  * Created by Yousef on 8/02/2016.
  */
case class configFile(messages: Option[String], containers: Option[String], modelConfig: Option[String], models: Option[String],
                      messageBinding: Option[String], jtms: Option[String])

class FileUtility  extends LogTrait{

  def FindFileExtension (filePath: String) : Boolean = {//This method used to check if the extension of file is json or not (return true if json and false otherwise)
    val ext = FilenameUtils.getExtension(filePath);
    if (ext.equalsIgnoreCase("json")){
      return true
    } else {
      return false
    }
  }

  def ParseFile(filePath: String): json4s.JValue ={//This method used to parse a config file (JSON format)
    try{
      val parsedFile = parse(filePath)
      return parsedFile
    } catch{
      case e: Exception => throw new KamanjaException(s"There is an error in the format of file \n ErrorMsg : ", e)
    }
  }

  def extractInfo(parsedValue: json4s.JValue): configFile={ //This method used to extract data from config file
  implicit val formats = DefaultFormats
    val extractedObj = parsedValue.extract[configFile]
    return extractedObj
  }

  def ReadFile(filePath: String): String ={//This method used to read a whole file (from header && pmml)
    return fromFile(filePath).mkString
  }

  def CheckFileContent(fileContent: String, fileType: String): Unit = {
    if (fileContent == null || fileContent.toString().trim() == "") {
      logger.error("Please pass the %s file after --%s option".format(fileType, fileContent.toLowerCase))
      logger.warn(PluginPrerequisites.usage)
      sys.exit(1)
    }
  }

  def FileExist(filePath: String): Unit={//This method used to check if file exists or not (return true if exists and false otherwise)
    val fileExistsFlag = new java.io.File(filePath).exists
    if(fileExistsFlag == false){
      logger.error("This file %s does not exists".format(filePath))
      logger.warn(PluginPrerequisites.usage)
      sys.exit(1)
    }
  }

  def SplitStringToArray(stringArray: String): Array[String] ={
    val keysArray = stringArray.split(",")
    return keysArray
  }

  def createConfigBeanObj(configInfo: configFile): ConfigBean={ //This method used to create a configObj
    var configBeanObj:ConfigBean = new ConfigBean()
    if(configInfo.messages.isEmpty){
      logger.error("you should pass messages in config file")
      sys.exit(1)
    } else if(configInfo.messages.get.trim == ""){
      logger.error("messages cannot be null in config file")
      sys.exit(1)
    } else {
      configBeanObj.hasMessages_=(true)
      configBeanObj.messages_=(configInfo.messages.get)
    }

//    if(configInfo.containers.isEmpty){
//      logger.error("you should pass containers in config file")
//      sys.exit(1)
//    } else if(configInfo.containers.get.trim == ""){
//      logger.error("containers cannot be null in config file")
//      sys.exit(1)
//    } else {
//      configBeanObj.hasContainers_=(true)
//      configBeanObj.containers_=(configInfo.containers.get)
//    }

    if(!configInfo.containers.isEmpty){
      configBeanObj.hasContainers_=(true)
      configBeanObj.containers_=(configInfo.containers.get)
    }

//    if(configInfo.models.isEmpty){
//      logger.error("you should pass containers in config file")
//      sys.exit(1)
//    } else if(configInfo.models.get.trim == ""){
//      logger.error("models cannot be null in config file")
//      sys.exit(1)
//    } else {
//      configBeanObj.hasModels_=(true)
//      configBeanObj.models_=(configInfo.models.get)
//    }

    if(!configInfo.models.isEmpty){
      configBeanObj.hasModels_=(true)
      configBeanObj.models_=(configInfo.models.get)
    }
//    if(configInfo.jtms.isEmpty){
//      logger.error("you should pass jtms in config file")
//      sys.exit(1)
//    } else if(configInfo.jtms.get.trim == ""){
//      logger.error("jtms cannot be null in config file")
//      sys.exit(1)
//    } else {
//      configBeanObj.hasJtms_=(true)
//      configBeanObj.jtms_=(configInfo.jtms.get)
//    }

    if(!configInfo.jtms.isEmpty){
      configBeanObj.hasJtms_=(true)
      configBeanObj.jtms_=(configInfo.jtms.get)
    }

//    if(configInfo.messageBinding.isEmpty){
//      logger.error("you should pass messageBinding in config file")
//      sys.exit(1)
//    } else if(configInfo.messageBinding.get.trim == ""){
//      logger.error("messageBinding cannot be null in config file")
//      sys.exit(1)
//    } else {
//      configBeanObj.hasMessageBinding_=(true)
//      configBeanObj.messageBinding_=(configInfo.messageBinding.get)
//    }

    if(!configInfo.messageBinding.isEmpty){
      configBeanObj.hasMessageBinding_=(true)
      configBeanObj.messageBinding_=(configInfo.messageBinding.get)
    }

    if(configInfo.modelConfig.isEmpty){
      logger.error("you should pass modelConfig in config file")
      sys.exit(1)
    } else if(configInfo.modelConfig.get.trim == ""){
      logger.error("modelConfig cannot be null in config file")
      sys.exit(1)
    } else {
      configBeanObj.hasModelConfig_=(true)
      configBeanObj.modelConfig_=(configInfo.modelConfig.get)
    }

      return configBeanObj
    }

  def writeToFile(fileContent: String, filename: String): Unit = {
        new PrintWriter(filename) {
          write(fileContent);
          close
        }
  }
}
