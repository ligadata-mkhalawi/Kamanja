package com.ligadata.tool.generatemessage

import org.json4s.native.JsonMethods._
import org.scalatest._
import Matchers._

/**
  * Created by Yousef on 5/26/2016.
  */
class PMMLUtilityTest extends FeatureSpec with GivenWhenThen {
  private def getResourceFullPath(resourcePath : String): String ={
    val os = System.getProperty("os.name")
    val isWidnows = os.toLowerCase.contains("windows")
    val path = getClass.getResource(resourcePath).getPath
    val finalPath = if(isWidnows) path.substring(1) else path
    finalPath
  }

  info("This an example to test PMMLUtility class")
  info("I want to be able to create an object")
  info("So i can access the variables")
  info("And get all variables when I need it using functions")

  feature("JsonUtility object") {
    val fileBean: FileUtility = new FileUtility
    val pmmlBean: PMMLUtility = new PMMLUtility
    scenario("Unit Tests for all PMMLUtility functions") {
      Given("Initiate variables")
      val filePath = getResourceFullPath("/configFile.json")
      val filePathEmpty = getResourceFullPath("/configFileEmpty.json")
      //val inputFile = getResourceFullPath("/DecisionTreeIris.pmml")
      //val inputFile = getResourceFullPath("/DecisionTreeEnsembleIris.pmml")
      val inputFile = getResourceFullPath("/SupportVectorMachineIris.pmml")
      val fileContent = fileBean.ReadFile(inputFile)

      When("xml includes data")
      val model = pmmlBean.XMLReader(fileContent)
      Then("variable should be set")
      model should not be (null)

      When("try to get active fields")
      val ActiveFieldsArray = pmmlBean.ActiveFields(model)
      Then("activeFields array should includes data")
      println("=================Active fields============")
      println(ActiveFieldsArray.length)
      for (item <- ActiveFieldsArray) {
        println("fields: " + item._1 + ", value: " + item._2)
      }


      When("try to get output fields")
      val outputFieldsArray = pmmlBean.OutputFields(model)
      Then("outputFieldsArray array should includes data")
      println("=================output fields============")
      println(outputFieldsArray.length)
      for (item <- outputFieldsArray) {
        println("fields: " + item._1 + ", value: " + item._2)
      }

      When("try to get target fields")
      val targetFieldsArray = pmmlBean.TargetFields(model)
      Then("targetFieldsArray array should includes data")
      println("=================Target fields============")
      println(targetFieldsArray.length)
      for (item <- targetFieldsArray) {
        println("fields: " + item._1 + ", value: " + item._2)
      }
      println("==========================================")
//      When("try to get output message fields")
//      val outputFieldsmap = pmmlBean.OutputMessageFields(outputFieldsArray, targetFieldsArray)
//      Then("outputFieldsmap array should includes data")
//      println("===================================================")
//      println(outputFieldsmap.size)
//      for (item <- outputFieldsmap) {
//        println("fields: " + item._1 + ", value: " + item._2)
//      }
//      println("===================================================")
    }
  }
}
