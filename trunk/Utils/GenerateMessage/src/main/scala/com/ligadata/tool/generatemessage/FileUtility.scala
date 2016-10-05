package com.ligadata.tool.generatemessage

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
  * Created by Yousef on 5/12/2016.
  */
case class configFile(delimiter: Option[String], outputPath: Option[String], saveMessage: Option[String], nameSpace: Option[String],
                      partitionKey: Option[String], primaryKey: Option[String], timePartition: Option[String], messageType: Option[String],
                      messageName: Option[String], createMessageFrom: Option[String], messageStructure: Option[String], detectDatatypeFrom: Option[Int])

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

  def FileExist(filePath: String): Boolean={//This method used to check if file exists or not (return true if exists and false otherwise)
    return new java.io.File(filePath).exists
  }

  def ReadHeaderFile(filePath: String, ignoreLines: Int): String ={//This method used to read first line in file
    if (ignoreLines == 0) {
      val line = fromFile(filePath).getLines
      return line.next()
    } else {
      val line = fromFile(filePath).getLines.drop(ignoreLines)
      return line.next()
    }
  }

  def Countlines(filePath: String): Integer={ //return number of lines in file
    return fromFile(filePath).getLines.size
  }

  def SplitFile (filePath: String, delimiter: String): Array[String] = {//This method used to split file based on delimiter
    if(!filePath.contains(delimiter)){
      logger.error("This delimiter %s does not exist in file".format(delimiter))
      sys.exit(1)
    }
    return filePath.split(delimiter)
  }

  def createConfigBeanObj(configInfo: configFile): ConfigBean={ //This method used to create a configObj
    var configBeanObj:ConfigBean = new ConfigBean()
    val dataTypeObj: DataTypeUtility = new DataTypeUtility()
    if(configInfo.outputPath.get == None){
      logger.error("you should pass outputpath in config file")
      sys.exit(1)
    } else if(configInfo.outputPath.get.trim == ""){
      logger.error("outputpath cannot be null in config file")
      sys.exit(1)
    } else {
      configBeanObj.outputPath_=(configInfo.outputPath.get)
    }

    if(configInfo.createMessageFrom == None){
      logger.error("you should pass createMessageFrom in config file")
      sys.exit(1)
    } else if(configInfo.createMessageFrom.get.equalsIgnoreCase("header")){
      configBeanObj.createMessageFrom_=("header")
      if(configInfo.delimiter == None){
        logger.error("you should pass delimiter in config file when you need to create a message from header")
        sys.exit(1)
      } else if(configInfo.delimiter.get.trim.equalsIgnoreCase("")){
        logger.error("delimiter cannot be null in config file")
        sys.exit(1)
      } else{
        configBeanObj.delimiter_=(configInfo.delimiter.get)
      }
    } else if(configInfo.createMessageFrom.get.equalsIgnoreCase("pmml")){
      configBeanObj.createMessageFrom_=("pmml")
    } else {
      logger.error("the value for createMessageFrom should be header or pmml")
      sys.exit(1)
    }

      if(dataTypeObj.isBoolean(configInfo.saveMessage.getOrElse("false"))){
        configBeanObj.saveMessage_=(configInfo.saveMessage.get.toBoolean)
      } else{
        logger.error("the value for saveMessage should be true or false")
        sys.exit(1)
      }

    if(dataTypeObj.validateMessageName(configInfo.nameSpace.getOrElse("com.message"))){ //////// check the . character
      configBeanObj.nameSpace_=(configInfo.nameSpace.getOrElse("com.message"))
    } else {
      logger.error("The namespace is invalid. please check the namespace in config file.")
      sys.exit(1)
    }

    if(dataTypeObj.validateMessageName(configInfo.messageName.getOrElse("testmessage"))){
      configBeanObj.messageName_=(configInfo.messageName.getOrElse("testmessage"))
    } else {
      logger.error("The messageName is invalid. Please check the messageName in config file.")
      sys.exit(1)
    }

      configBeanObj.partitionKey_=(configInfo.partitionKey.getOrElse(""))
      if(!configBeanObj.partitionKey.trim.equalsIgnoreCase("")) configBeanObj.hasPartitionKey_=(true) else configBeanObj.hasPartitionKey_=(false)

      configBeanObj.primaryKey_=(configInfo.primaryKey.getOrElse(""))
      if(!configBeanObj.primaryKey.trim.equalsIgnoreCase("")) configBeanObj.hasPrimaryKey_=(true) else configBeanObj.hasPrimaryKey_=(false)

      configBeanObj.timePartition_=(configInfo.timePartition.getOrElse(""))
      if(!configBeanObj.timePartition.trim.equalsIgnoreCase("")) configBeanObj.hasTimePartition_=(true) else configBeanObj.hasTimePartition_=(false)

        if (configInfo.messageType.getOrElse("input").equalsIgnoreCase("input")) {
          configBeanObj.messageType_=("input")
        } else if (configInfo.messageType.getOrElse("input").equalsIgnoreCase("output")) {
          configBeanObj.messageType_=("output")
          if(configInfo.createMessageFrom.get.equalsIgnoreCase("header")){
            logger.error("create output message from header file not supported yet")
            sys.exit(1)
          }
        } else {
          logger.error("the value of massegeType should be input or output")
          sys.exit(1)
        }

      if(configInfo.messageStructure.getOrElse("fixed").equalsIgnoreCase("fixed")){
        configBeanObj.messageStructure_=(true)
      } else if(configInfo.messageStructure.getOrElse("fixed").equalsIgnoreCase("mapped")){
        configBeanObj.messageStructure_=(false)
      } else {
        logger.error("the value of massegeStructure should be fixed or mapped")
        sys.exit(1)
      }

    configBeanObj.detecDatatypeFrom_=(configInfo.detectDatatypeFrom.getOrElse(4))

      return configBeanObj
    }

  def writeToFile(json:JsonAST.JValue, filename: String): Unit = {
        new PrintWriter(filename) {
          write(pretty(render(json)));
          close
        }
  }

  def CreateFileName(outputPath: String): String={
    var filename = ""
    if (outputPath == null) {
      logger.error("output path should not be null")
      sys.exit(1)
    } else if(!FileExist(outputPath)) {
      logger.error("the output path does not exists")
      sys.exit(1)
    } else {
      val dateFormat = new SimpleDateFormat("yyyyMMddhhmmss")
      var messageName = "/message_" + dateFormat.format(new java.util.Date()) + ".json"
      if(outputPath.charAt(outputPath.length -1 ).equals('/'))
        messageName = "message_" + dateFormat.format(new java.util.Date()) + ".json"
       filename = outputPath + messageName
    }
    return filename
  }
}
