package com.ligadata.pluginsutility

import com.ligadata.MetadataAPI.MetadataAPI.ModelType
import com.ligadata.Exceptions.KamanjaException
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.LogManager
import org.json4s.native.JsonMethods._
import com.ligadata.Utils.{KamanjaLoaderInfo, Utils}

import scala.io.Source._
import com.ligadata.MetadataAPI.{MetadataAPI, MetadataAPIImpl}
import java.io.File

import scala.io.Source
/**
  * Created by Yousef on 8/2/2016.
  */

trait LogTrait {
  val loggerName = this.getClass.getName()
  val logger = LogManager.getLogger(loggerName)
}


object PluginPrerequisites extends App with LogTrait {

  def usage: String = {
    """
Usage:  bash $KAMANJA_HOME/bin/JsonChecker.sh --inputfile $KAMANJA_HOME/config/ClusterConfig.json
    """
  }

  private type OptionMap = Map[Symbol, Any]

  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    def isSwitch(s: String) = (s.charAt(0) == '-')
    list match {
      case Nil => map
      case "--kamanjapath" :: value :: tail =>
        nextOption(map ++ Map('kamanajapath -> value), tail)
      case "--inputfile" :: value :: tail =>
        nextOption(map ++ Map('inputfile -> value), tail)
      case "--outputpath" :: value :: tail =>
        nextOption(map ++ Map('outputpath -> value), tail)
      case "--dependencyfile" :: value :: tail =>
        nextOption(map ++ Map('dependencyfile -> value), tail)
      case "--clusterconfig" :: value :: tail =>
        nextOption(map ++ Map('clusterconfig -> value), tail)
      case option :: tail => {
        logger.error("Unknown option " + option)
        logger.warn(usage)
        sys.exit(1)
      }
    }
  }

  override def main(args: Array[String]) {

    logger.debug("PluginPrerequisites.main begins")

    if (args.length == 0) {
      logger.error("Please pass the input file after --inputfile option")
      logger.warn(usage)
      sys.exit(1)
    }
    val options = nextOption(Map(), args.toList)

    val inputFile = options.getOrElse('inputfile, null).toString.trim
    val outputPath = options.getOrElse('outputpath, null).toString.trim
    val dependencyFile = options.getOrElse('dependencyfile, null).toString.trim
    val kamanjaPath = options.getOrElse('kamanjapath, null).toString.trim
    val clusterConfig = options.getOrElse('clusterconfig, null).toString.trim

    val fileBean: FileUtility = new FileUtility()
    fileBean.FileExist(inputFile) // check if inputFile path exists
    fileBean.FileExist(outputPath) // check if outputPath path exists
    fileBean.FileExist(dependencyFile) // check if dependencyFile path exists
    fileBean.FileExist(kamanjaPath) // check if kamanjaPath path exists
    fileBean.FileExist(clusterConfig) // check if kamanjaPath path exists
    fileBean.CheckFileContent(inputFile, "input") //check if file includes data
    //fileBean.CheckFileContent(outputPath, "output") //check if file includes data
    fileBean.CheckFileContent(dependencyFile, "dependency") //check if file includes data
    fileBean.CheckFileContent(clusterConfig, "clusterconfig") //check if file includes data


    val parsedConfig = fileBean.ParseFile(dependencyFile) //Parse config file
    val extractedInfo = fileBean.extractInfo(parsedConfig) //Extract information from parsed file
    val configBeanObj = fileBean.createConfigBeanObj(extractedInfo)// create a config object that store the result from extracting config file
    if (configBeanObj.hasMessages == true) configBeanObj.messagesArray = fileBean.SplitStringToArray(configBeanObj.messages)
    if (configBeanObj.hasMessageBinding == true) configBeanObj.messageBindingArray = fileBean.SplitStringToArray(configBeanObj.messageBinding)
    if (configBeanObj.hasModelConfig == true) configBeanObj.modelConfigArray = fileBean.SplitStringToArray(configBeanObj.modelConfig)
    if (configBeanObj.hasContainers == true) configBeanObj.containersArray = fileBean.SplitStringToArray(configBeanObj.containers)
    if (configBeanObj.hasModels == true) configBeanObj.modelsArray = fileBean.SplitStringToArray(configBeanObj.models)
    if (configBeanObj.hasJtms == true) configBeanObj.jtmsArray = fileBean.SplitStringToArray(configBeanObj.jtms)

    val (allConfigs, failStr) = Utils.loadConfiguration(clusterConfig.toString, true)
    if (failStr != null && failStr.size > 0) {
      logger.error(failStr)
      sys.exit(1)
    }
    if (allConfigs == null) {
      logger.error("Failed to load configurations from configuration file")
      sys.exit(1)
    }

    MetadataAPIImpl.InitMdMgrFromBootStrap(clusterConfig.toString, false)

    var response =""
    var filePath = new File(kamanjaPath)
    val pluginpre: PluginPrerequisites = new PluginPrerequisites()
    if(configBeanObj.hasMessages) {
      response = pluginpre.AddMessageDefinition(configBeanObj, kamanjaPath) //add message to kamanja
    } else response = "no message added in config file"
    println(response)
    if(configBeanObj.hasContainers) {
      response = pluginpre.AddContainerDefinition(configBeanObj, kamanjaPath) //add container to kamanja
    }  else response = "no container added in config file"
    println(response)
    if(configBeanObj.hasModelConfig) {
      response = pluginpre.AddModelConfig(configBeanObj, kamanjaPath)//add modelConfig to kamanja
    } else response = "no modelConfig added in config file"
    println(response)
    if(configBeanObj.hasJtms) {
      response = pluginpre.AddJTMModel(configBeanObj, kamanjaPath) // add jtm to kamanaja
    }  else response = "no jtm added in config file"
    println(response)
    if(configBeanObj.hasModels) {
      response = pluginpre.AddCurrentModel(configBeanObj, kamanjaPath) // add model to kamanja
    } else response = "no model added in config file"
    println(response)
    if(configBeanObj.hasMessageBinding) {
      response = pluginpre.AddMessageBinding(configBeanObj, kamanjaPath)// add messageBinding to kamanja
    } else response = "no messageBinding added in config file"
    println(response)
  }
}

class PluginPrerequisites extends LogTrait{

  def AddMessageDefinition(configBeanObj: ConfigBean, kamanjaPath: String): String ={
    var response  = ""
    for (message <- configBeanObj.messagesArray) {
      val filePath = new File(kamanjaPath + "/" + message)
      val messageDef = Source.fromFile(filePath).mkString
      response = MetadataAPIImpl.AddMessage(messageText = messageDef, format = "JSON", pStr = None)
    }
    return response
  }

  def AddContainerDefinition(configBeanObj: ConfigBean, kamanjaPath: String): String ={
    var response  = ""
    for (container <- configBeanObj.containersArray) {
      val filePath = new File(kamanjaPath + "/" + container)
      val containerDef = Source.fromFile(filePath).mkString
      response = MetadataAPIImpl.AddContainer(containerText = containerDef, format = "JSON", pStr = None)
    }
    return response
  }

  def AddModelConfig(configBeanObj: ConfigBean, kamanjaPath: String): String ={
    var response = ""
    for (modelConfig <- configBeanObj.modelConfigArray) {
      val filePath = new File(kamanjaPath + "/" + modelConfig)
      val modelConfigDef = Source.fromFile(filePath).mkString
      response = MetadataAPIImpl.UploadModelsConfig(cfgStr = modelConfigDef, objectList = "configuration", userid = None)
    }
    return response
  }

  def AddJTMModel(configBeanObj: ConfigBean, kamanjaPath: String): String ={
    var response = ""
    for (jtm <- configBeanObj.jtmsArray) {
      val filePath = new File(kamanjaPath + "/" + jtm)
      val jtmDef = Source.fromFile(filePath).mkString
      response = MetadataAPIImpl.AddModel(modelType = ModelType.JTM, input = jtmDef, pStr = None)
    }
    return response
  }

  def AddCurrentModel(configBeanObj: ConfigBean, kamanjaPath: String): String ={
    var response = ""
      for (model <- configBeanObj.modelsArray) {
        val filePath = new File(kamanjaPath + "/" + model) // ad path for message
        val modelDef = Source.fromFile(filePath).mkString //////save model in file then add the model to kamanaja
        response = MetadataAPIImpl.getMetadataAPI.AddModel(ModelType.JAVA, modelDef, pStr = None)
      }
    return response
  }

  def AddMessageBinding(configBeanObj: ConfigBean, kamanjaPath: String): String ={
    var response = ""
    for (messageBinding <- configBeanObj.messageBindingArray) {
      val filePath = new File(kamanjaPath + "/" + messageBinding)
      val messageBindingDef = Source.fromFile(filePath).mkString
      response = com.ligadata.MetadataAPI.Utility.AdapterMessageBindingService.addFromFileAnAdapterMessageBinding(messageBindingDef, None)
    }
    return response
  }
}


