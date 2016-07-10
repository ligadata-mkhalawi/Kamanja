package com.ligadata.tool.generatemessage

import org.json4s.native.JsonMethods._
import org.scalatest._
import Matchers._
/**
  * Created by Yousef on 5/16/2016.
  */
class FileUtilityTest extends FeatureSpec with GivenWhenThen {

  private def getResourceFullPath(resourcePath : String): String ={
    val os = System.getProperty("os.name")
    val isWidnows = os.toLowerCase.contains("windows")
    val path = getClass.getResource(resourcePath).getPath
    val finalPath = if(isWidnows) path.substring(1) else path
    finalPath
  }

  info("This an example to test FileUtility class")
  info("I want to be able to create an object")
  info("So i can access the variables")
  info("And get all variables when I need it using functions")

  feature("FileUtility object") {
    val fileBean: FileUtility = new FileUtility()
    scenario("Unit Tests for all fileUtility function") {
      Given("Initiate variables")
      val filePath = getResourceFullPath("/configFile.json")
      val filePathEmpty = getResourceFullPath("/configFileEmpty.json")
      val inputFile = getResourceFullPath("/inputFile.txt")
      val datatypeObj = new DataTypeUtility
      Given("Test readFile function")

      When("The file includes data")
      val fileContent = fileBean.ReadFile(filePath)
      Then("The fileContent value should be set and FileContent length should not equal 0")
      fileContent should not be (null)
      fileContent.length should not equal (0)

      When("file does not include data")
      val fileContentEmpty = fileBean.ReadFile(filePathEmpty)
      Then("The fileContent length should be 0")
      fileContentEmpty.length should be(0)

      Given("Test FileExist function")

      When("The file in path")
      val fileExitFlag = fileBean.FileExist(filePath)
      Then("The fileExist flag should be true")
      fileExitFlag should be(true)

//      When("The file not in path")
//      Then("a NullPointException should be raise")
//      intercept[NullPointerException] {
//        val fileExitFlag = fileBean.FileExist(getResourceFullPath("/Config.json"))
//      }

      Given("Test ReadFileHeader function")

      When("The file in path")
      val fileHeader = fileBean.ReadHeaderFile(inputFile,0)
      Then("The fileHeader size should not be 0")
      fileHeader.length should not be(0)
      println(fileHeader)

      Given("Test SplitHeader function")

      When("The file in path")
      val headerFeilds = fileBean.SplitFile(fileHeader,",")
      Then("The size of array should not be 4")
      headerFeilds.length should be(4)
      for(item <- headerFeilds)
        println(item)

      Given("Test CountLines function")

      When("The file in path")
      val count = fileBean.Countlines(inputFile)
      Then("The number of lines should be 4")
      count should be(4)

      Given("Test Parse and Extract functions")

      When("The file includes data")
      val configFileContent = fileBean.ReadFile(filePath)
      val parsedFile = fileBean.ParseFile(configFileContent)
      val extractedFile = fileBean.extractInfo(parsedFile)
      Then("The delimiter variable should be set")
      extractedFile.delimiter should not be(None)

      Given("Test CreateConfigObj function")

      When("The file includes data")
      val configObj = fileBean.createConfigBeanObj(extractedFile)
      Then("The delimiter variable should be set")
      configObj.delimiter should be(",")

      if("yousef".matches("^[a-zA-Z][a-zA-Z0-9]*?$"))
        println("valid")
      else println("invalid")

      val variableName = "Kamanja.com"
//      println(variableName.length)
//      println(variableName.charAt(variableName.length-1))
      if(datatypeObj.validateMessageName(variableName))
        println(variableName + " is valid")
      else println(variableName + " is invalid")
//      Given("Test CreateFileName function")
//      val filename = fileBean.CreateFileName(configObj.outputPath)
//      When("The file includes data")
//      println(filename)

//      Given("Test WriteToFile function")
//      val json = parse(""" { "numbers" : [1, 2, 3, 4] } """)
//     fileBean.writeToFile(json,filename)
    }
  }
}
