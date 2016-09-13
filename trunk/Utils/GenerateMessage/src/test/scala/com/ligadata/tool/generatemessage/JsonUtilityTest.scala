package com.ligadata.tool.generatemessage

import org.json4s.native.JsonMethods._
import org.scalatest._
import Matchers._

import scala.collection.mutable

/**
  * Created by Yousef on 5/18/2016.
  */
class JsonUtilityTest extends FeatureSpec with GivenWhenThen {
  private def getResourceFullPath(resourcePath : String): String ={
    val os = System.getProperty("os.name")
    val isWidnows = os.toLowerCase.contains("windows")
    val path = getClass.getResource(resourcePath).getPath
    val finalPath = if(isWidnows) path.substring(1) else path
    finalPath
  }

  info("This an example to test JsonUtility class")
  info("I want to be able to create an object")
  info("So i can access the variables")
  info("And get all variables when I need it using functions")

  feature("JsonUtility object") {
    scenario("Unit Tests for all JsonUtility functions") {
      Given("Initiate variables")
      val jsonObj: JsonUtility = new JsonUtility()
      val jsonUtilityBean: JsonUtility = new JsonUtility()
      val fileBean: FileUtility = new FileUtility()
      val filePath = getResourceFullPath("/configFile.json")
      val configFileContent = fileBean.ReadFile(filePath)
      val parsedFile = fileBean.ParseFile(configFileContent)
      val extractedFile = fileBean.extractInfo(parsedFile)
      var feildsString =  mutable.LinkedHashMap[String, String]()
      //val inputFile = getResourceFullPath("/inputFile.txt")
      val inputFile = getResourceFullPath("/SubscriberInfo_Telecom.dat")
      val inputFileContent = fileBean.ReadFile(inputFile)
      val fileSize = fileBean.Countlines(inputFile)
      val configBeanObj = fileBean.createConfigBeanObj(extractedFile)
      val headerString = fileBean.ReadHeaderFile(inputFile, 0)
      val headerFields = fileBean.SplitFile(headerString, configBeanObj.delimiter)
      val dataTypeObj: DataTypeUtility = new DataTypeUtility()
      for(itemIndex <- 0 to headerFields.length-1) {
        var previousType = ""
        for(size <- 2 to 4){
          if(fileSize >= size) {
         //   println( "itemindex = "+ itemIndex + " size = " + size+ " previousType = "+ previousType)
              val fieldLines = fileBean.ReadHeaderFile(inputFile, size - 1)
              val linesfeild = fileBean.SplitFile(fieldLines, configBeanObj.delimiter)
              val currentType = dataTypeObj.FindFeildType(linesfeild(itemIndex))
          //  println( "itemindex = "+ itemIndex + " size = " + size+ " currentType = "+ currentType)
            if(previousType.equalsIgnoreCase("string") || (previousType.equalsIgnoreCase("boolean") && !currentType.equalsIgnoreCase("boolean"))
              || (!previousType.equalsIgnoreCase("boolean") && !previousType.equalsIgnoreCase("") && (currentType.equalsIgnoreCase("string") || currentType.equalsIgnoreCase("boolean")))
              || (previousType.equalsIgnoreCase("Int")&& currentType.equalsIgnoreCase("boolean"))){
              previousType = "String"
            } else if (previousType.equalsIgnoreCase("boolean") && currentType.equalsIgnoreCase("boolean")){
              previousType = "Boolean"
            } else if((previousType.equalsIgnoreCase("double") && (!currentType.equalsIgnoreCase("string") && !currentType.equalsIgnoreCase("boolean")))
              || (previousType.equalsIgnoreCase("long") && (currentType.equalsIgnoreCase("double") || currentType.equalsIgnoreCase("float")))
              || (previousType.equalsIgnoreCase("float") && (currentType.equalsIgnoreCase("long") || currentType.equalsIgnoreCase("double")))){
              previousType = "Double"
            } else if(previousType.equalsIgnoreCase("long") && (currentType.equalsIgnoreCase("long") || currentType.equalsIgnoreCase("int"))){
              previousType = "Long"
            } else if(previousType.equalsIgnoreCase("float") && (currentType.equalsIgnoreCase("float") || currentType.equalsIgnoreCase("int"))){
              previousType = "Float"
            } else if(previousType.equalsIgnoreCase("") ||(previousType.equalsIgnoreCase("int") && !currentType.equalsIgnoreCase("boolean"))){
              previousType = currentType
            } else if(previousType.equalsIgnoreCase("int") && currentType.equalsIgnoreCase("boolean")){
              previousType = "Boolean"
            }
          }
        }
        feildsString += (headerFields(itemIndex) -> previousType)
      }
      Given("Test CreateMainJsonString function")

      When("pass a map with data")
      var json = jsonObj.CreateMainJsonString(feildsString, configBeanObj)
      Then("The isDigitFlag value should be false")
      //isDigitFlag should be (false)
      println(pretty(render(json)))

      configBeanObj.hasPartitionKey_=(true)
      configBeanObj.primaryKeyArray_=(Array("id","name"))
      configBeanObj.partitionKeyArray_=(Array("id"))
     // println("List ->" + configBeanObj.partitionKeyArray.toList)
      val jsonPatitionKey = jsonObj.CreateJsonString("PartitionKey", configBeanObj, configBeanObj.partitionKeyArray)
      json = json merge jsonPatitionKey
      println("=======================================================")
      println(pretty(render(json)))

      configBeanObj.hasPrimaryKey_=(true)
      val jsonPrimaryKey = jsonObj.CreateJsonString("PrimaryKey", configBeanObj, configBeanObj.primaryKeyArray)
      json = json merge jsonPrimaryKey
      println("=======================================================")
      println(pretty(render(json)))

      configBeanObj.hasTimePartition_=(true)
      val jsonTimePartition = jsonObj.CreateJsonString("TimePartitionInfo", configBeanObj, Array(configBeanObj.timePartition))
      json = json merge jsonTimePartition
      println("=======================================================")
      println(pretty(render(json)))

      configBeanObj.timePartition_= ("id")
      val keyArray = Array("id","name","company","work")
      if (configBeanObj.hasTimePartition == true) {
        val x = dataTypeObj.CheckKeys(keyArray, configBeanObj.timePartition).toString
        println(configBeanObj.timePartition)
      }
    }
  }
}
