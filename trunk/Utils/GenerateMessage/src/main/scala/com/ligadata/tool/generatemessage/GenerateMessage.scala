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

package com.ligadata.tool.generatemessage

import org.apache.commons.io.FilenameUtils
import org.json4s._
import org.json4s.JsonDSL._
import com.ligadata.Exceptions._
import org.json4s.native.JsonMethods._
import scala.collection.immutable.HashMap
import java.io.File
import java.io.PrintWriter
import org.apache.logging.log4j._
import scala.collection.mutable
import scala.io.Source.fromFile

trait LogTrait {
  val loggerName = this.getClass.getName()
  val logger = LogManager.getLogger(loggerName)
}


object GenerateMessage extends App with LogTrait{

  def usage: String = { //This method used to tell user how he can use the tool
    """
Usage:  bash $KAMANJA_HOME/bin/GenerateMessage.sh --inputfile $KAMANJA_HOME/input/SampleApplication/data/file.csv --config $KAMANJA_HOME/config/file.json
    """
  }

  private type OptionMap = Map[Symbol, Any]

  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {//This method used to parse the input parameter for tool
    def isSwitch(s: String) = (s.charAt(0) == '-')
    list match {
      case Nil => map
      case "--inputfile" :: value :: tail =>
        nextOption(map ++ Map('inputfile -> value), tail)
      case "--config" :: value :: tail =>
        nextOption(map ++ Map('config -> value), tail)
      case "--fieldnames" :: value :: tail =>
        nextOption(map ++ Map('fieldnames -> value), tail)
      case "--header" :: value :: tail =>
        nextOption(map ++ Map('header -> value), tail)
      case option :: tail => {
        logger.error("Unknown option " + option)
        logger.warn(usage)
        sys.exit(1)
      }
    }
  }

   override def main(args: Array[String]) {

    logger.debug("GenerateMessage.main begins")

    if (args.length == 0) { //Check if user did not pass parameters
      logger.error("Please pass the input file after --inputfile option and config file after --config operation")
      logger.warn(usage)
      sys.exit(1)
    }
    val options = nextOption(Map(), args.toList)

    val inputFile = options.getOrElse('inputfile, null).toString.trim //Read inputFile value from parsed parameters
    if (inputFile == null || inputFile.toString().trim() == "") { //check if inputFile passed or not
      logger.error("Please pass the input file after --inputfile option")
      logger.warn(usage)
      sys.exit(1)
    }

     val configFile = options.getOrElse('config, null).toString.trim //Raad config value from parsed parameters
     if (configFile == null || configFile.toString().trim() == "") { //check if config passed or not
       logger.error("Please pass the config file after --config option")
       logger.warn(usage)
       sys.exit(1)
     }

     val fileBean: FileUtility = new FileUtility()
     val dataTypeObj: DataTypeUtility = new DataTypeUtility()

     val inputFileExistFlag = fileBean.FileExist(inputFile) // check if inputFile path exists
     val configFileExistFlag = fileBean.FileExist(configFile) // check if config file path exists
     if (inputFileExistFlag == false){
       logger.error("This file %s does not exists".format(inputFile))
       logger.warn(usage)
       sys.exit(1)
     }
     if (configFileExistFlag == false){
       logger.error("This file %s does not exists".format(configFile))
       logger.warn(usage)
       sys.exit(1)
     }

     val inputFileContent = fileBean.ReadFile(inputFile) // read inputFile contents
     val configFileContent = fileBean.ReadFile(configFile) // read config file contents
     if (inputFileContent == null  || inputFileContent.size == 0) { //check if inputFile includes data
        logger.error("This file %s does not include data. Check your file please.".format(inputFile))
       logger.warn(usage)
        sys.exit(1)
      }
     if (configFileContent == null  || configFileContent.size == 0) { // check if config file includes data
       logger.error("This file %s does not include data. Check your file please.".format(configFile))
       logger.warn(usage)
       sys.exit(1)
     }

     val parsedConfig = fileBean.ParseFile(configFileContent) //Parse config file
     val extractedInfo = fileBean.extractInfo(parsedConfig) //Extract information from parsed file
     val configBeanObj = fileBean.createConfigBeanObj(extractedInfo)// create a config object that store the result from extracting config file
     var feildsString = mutable.LinkedHashMap[String, String]()
     var feildsTemp = mutable.LinkedHashMap[String, String]()
     if(configBeanObj.createMessageFrom.equalsIgnoreCase("header")){
       var headerFlagFile = false
       val inputFileFields = options.getOrElse('fieldnames, "notpassed").toString.trim //Read fieldnames value from parsed parameters
       if (!inputFileFields.equalsIgnoreCase("notpassed") && !inputFileFields.equalsIgnoreCase("")) { //check if fieldnames passed or not
         val inputFileFieldsExistsFlag = fileBean.FileExist(inputFileFields) // check if  fieldnames path exists
         if (inputFileFieldsExistsFlag == false){
           logger.error("This file %s does not exists".format(inputFileFields))
           logger.warn(usage)
           sys.exit(1)
         }

         val inputFileFieldsContent = fileBean.ReadFile(inputFileFields) // readfieldnames contents
         if (inputFileFieldsContent == null  || inputFileFieldsContent.size == 0) { //check if fieldnames includes data
           logger.error("This file %s does not include data. Check your file please.".format(inputFileFields))
           logger.warn(usage)
           sys.exit(1)
         }

          headerFlagFile = true // change the flag to true if there is geader file in command

       }


       var headerFlag = options.getOrElse('header, "notpassed").toString.trim //Read inputFile value from parsed parameters
       if (headerFlag.equalsIgnoreCase("notpassed")) { //check if inputFile passed or not
         logger.info("You are not pass --header option and the default value is true.")
         println("[RESULT] - You are not pass --header option and the default value is true")
         headerFlag = "true"
       } else if(!headerFlag.equalsIgnoreCase("true") && !headerFlag.equalsIgnoreCase("false")){
         logger.error("The value for --header should be true or false")
         logger.warn(usage)
         sys.exit(1)
       }

       val fileSize = fileBean.Countlines(inputFile) // Find number of lines in file
       var headerString = ""
       var headerFields = Array.empty[String]
       if(headerFlagFile){
         headerString = fileBean.ReadHeaderFile(inputFileFields, 0) //read the header line for inputFile
         if(headerFlag.equalsIgnoreCase("true") || headerFlag.equalsIgnoreCase("false")){
           println("[Result] - The --fieldnames passed so the --header parameter ignored")
           logger.info("The --fieldnames passed so the --header parameter ignored")
         }
       } else if(headerFlag.equalsIgnoreCase("true")){
         headerString = fileBean.ReadHeaderFile(inputFile, 0) //read the header line for inputFile
         println("[Result] - The header read from input file because --fieldnames not passed")
         logger.info("The header read from input file because --fieldnames not passed")
       } else {
         logger.error("At least one of these option should pass correctly --header --fieldnames")
         logger.warn(usage)
         sys.exit(1)
       }

        headerFields = fileBean.SplitFile(headerString, configBeanObj.delimiter) //split the header line based on delimiter
       // check if partitionkey,primarykey,timepartioninfo value in file header
       if(configBeanObj.detectDatatypeFrom > fileSize) {
         logger.error("you pass %d in detectdatatypeFrom and the file size equal to %d records, please pass a number greater than 1 and less than the file size"
           .format(configBeanObj.detectDatatypeFrom, fileSize))
         sys.exit(1)
       }
       if (configBeanObj.hasPartitionKey == true) configBeanObj.partitionKeyArray = dataTypeObj.CheckKeys(headerFields,configBeanObj.partitionKey)
       if (configBeanObj.hasPrimaryKey == true) configBeanObj.primaryKeyArray = dataTypeObj.CheckKeys(headerFields, configBeanObj.primaryKey)
       if (configBeanObj.hasTimePartition == true) {
         val check = dataTypeObj.CheckKeys(headerFields, configBeanObj.timePartition)
       }

      // var invalidFlag: Boolean = false
       for(itemIndex <- 0 to headerFields.length-1) {
         var previousType = dataTypeObj.FindFinalType(fileSize, itemIndex, inputFile,configBeanObj.delimiter, configBeanObj.detectDatatypeFrom)
         feildsString += (headerFields(itemIndex) -> previousType)
       }

     } else if(configBeanObj.createMessageFrom.equalsIgnoreCase("pmml")){
       val pmmlObj: PMMLUtility = new PMMLUtility
       val modelEvaluator = pmmlObj.XMLReader(inputFileContent)
       if(configBeanObj.messageType.equalsIgnoreCase("input")){
         val activeFields = pmmlObj.ActiveFields(modelEvaluator)
         for(item <- activeFields){
           feildsString += (item._1 -> item._2)
         }
         if(feildsString.size == 0){
           logger.info("no input message produced from file")
           println("[RESULT] - no input message produced from file")
           sys.exit(1)
         }
       } else if(configBeanObj.messageType.equalsIgnoreCase("output")){
         val outputFields = pmmlObj.OutputFields(modelEvaluator)
         val targetFields = pmmlObj.TargetFields(modelEvaluator)
         feildsString = pmmlObj.OutputMessageFields(outputFields, targetFields)
         if(feildsString.size == 0){
           logger.info("no output message produced from file")
           println("[RESULT] - no output message produced from file")
           sys.exit(1)
         }
       }
       var keyArray = Array[String]()
       feildsString.keys.map { key =>
         keyArray = keyArray :+ key
       }
       if (configBeanObj.hasPartitionKey == true) configBeanObj.partitionKeyArray = dataTypeObj.CheckKeys(keyArray,configBeanObj.partitionKey)
       if (configBeanObj.hasPrimaryKey == true) configBeanObj.primaryKeyArray = dataTypeObj.CheckKeys(keyArray, configBeanObj.primaryKey)
       if (configBeanObj.hasTimePartition == true) {
         val check2  = dataTypeObj.CheckKeys(keyArray, configBeanObj.timePartition).toString
       }
     }
     val jsonBean: JsonUtility = new JsonUtility()
     val fileName = fileBean.CreateFileName(configBeanObj.outputPath) // create name for output file
     var mappedMessage: Boolean = false
     var ignoredFields: List[String]=Nil
     for(key <- feildsString.keySet){
      if(!dataTypeObj.validateVariableName(key)){
        feildsString.remove(key)
        ignoredFields = ignoredFields ++ Array(key)
        mappedMessage = true
      }
     }
     if (mappedMessage == true){
       var ignoredString = "("
       configBeanObj.messageStructure_=(false) //false means mapped and true means fixed
       for(item <- ignoredFields){
         ignoredString += item + ","
       }
       ignoredString = ignoredString.substring(0,ignoredString.length-1) + ")"
       logger.info("The message changed to mapped because there are some ignored fields %s".format(ignoredString))
       println("[RESULT] - The message changed to mapped because there are some ignored fields %s".format(ignoredString))
     }
     val json = jsonBean.FinalJsonString(feildsString,configBeanObj) // create json string
     fileBean.writeToFile(json,fileName) // write json string to output file
     logger.info("message created successfully")
     logger.info("you can find the file in this path %s".format(fileName))
     println("[RESULT] - message created successfully")
     println("[RESULT] - you can find the file in this path %s".format(fileName))
   }
}

