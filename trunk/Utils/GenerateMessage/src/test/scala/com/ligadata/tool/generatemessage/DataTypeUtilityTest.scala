package com.ligadata.tool.generatemessage


import org.scalatest._
import Matchers._
import java.lang._
/**
  * Created by Yousef on 5/17/2016.
  */
class DataTypeUtilityTest extends FeatureSpec with GivenWhenThen {

  private def getResourceFullPath(resourcePath : String): String ={
    val os = System.getProperty("os.name")
    val isWidnows = os.toLowerCase.contains("windows")
    val path = getClass.getResource(resourcePath).getPath
    val finalPath = if(isWidnows) path.substring(1) else path
    finalPath
  }

  info("This an example to test DataTypeUtility class")
  info("I want to be able to create an object")
  info("So i can access the variables")
  info("And get all variables when I need it using functions")

  feature("DataTypeUtility object") {
    scenario("Unit Tests for all DataTypeUtility function") {
      Given("Initiate variables")
      val datatypeBean: DataTypeUtility = new DataTypeUtility()

      Given("Test isAllDigits function (return true if all characters are number and false otherwise)")

      When("pass a string value")
      var isDigitFlag = datatypeBean.isAllDigits("Kamanja")
      Then("The isDigitFlag value should be false")
      isDigitFlag should equal (false)

      When("pass a numeric value")
      isDigitFlag = datatypeBean.isAllDigits("11")
      Then("The isDigitFlag value should be true")
      isDigitFlag should equal (true)

      Given("Test isInteger function (return true if variable is integer and false otherwise)")

      When("pass a string value")
      var isIntegerFlag = datatypeBean.isInteger("Kamanja")
      Then("The isIntegerFlag value should be false")
      isIntegerFlag should equal (false)

      When("pass an Integer value")
      isIntegerFlag = datatypeBean.isInteger("11")
      Then("The isIntegerFlag value should be true")
      isIntegerFlag should equal (true)

      When("pass an double value")
      isIntegerFlag = datatypeBean.isInteger("1.1")
      Then("The isIntegerFlag value should be false")
      isIntegerFlag should equal (false)

      Given("Test isDouble function (return true if variable is double and false otherwise)")

      When("pass a string value")
      var isDoubleFlag = datatypeBean.isDouble("Kamanja")
      Then("The isDoubleFlag value should be false")
      isDoubleFlag should equal (false)

      When("pass an Integer value")
      isDoubleFlag = datatypeBean.isDouble("11")
      Then("The isDoubleFlag value should be fasle")
      isDoubleFlag should equal (false)

      When("pass an double value")
      isDoubleFlag = datatypeBean.isDouble("1.1")
      Then("The isDoubleFlag value should be true")
      isDoubleFlag should equal (true)

      Given("Test isBoolean function (return true if variable is boolean and false otherwise)")

      When("pass a string value")
      var isBooleanFlag = datatypeBean.isBoolean("Kamanja")
      Then("The isBooleanFlag value should be false")
      isBooleanFlag should equal (false)

      When("pass an Integer value")
      isBooleanFlag = datatypeBean.isBoolean("11")
      Then("The isBooleanFlag value should be fasle")
      isBooleanFlag should equal (false)

      When("pass an double value")
      isBooleanFlag = datatypeBean.isBoolean("1.1")
      Then("The isBooleanFlag value should be false")
      isBooleanFlag should equal (false)

      When("pass an Boolean value")
      isBooleanFlag = datatypeBean.isBoolean("false")
      Then("The isBooleanFlag value should be true")
      isBooleanFlag should equal (true)

      Given("Test FindFeildType function")

      When("pass a string value")
      var feildType = datatypeBean.FindFeildType("Kamanja")
      Then("The feildType value should be String")
      feildType should be ("String")

      When("pass an Integer value")
      feildType = datatypeBean.FindFeildType("11")
      Then("The feildType value should be Int")
      feildType should be ("Int")

      When("pass an Float value")
      feildType = datatypeBean.FindFeildType("1.1")
      Then("The feildType value should be Float")
      feildType should be ("Float")

      When("pass an Double value")
      feildType = datatypeBean.FindFeildType("3.4028236E38") // Float.MAX_VALUE = 3.4028235E38
      Then("The feildType value should be Double")
      feildType should be ("Double")

      When("pass an Boolean value")
      feildType = datatypeBean.FindFeildType("false")
      Then("The feildType value should be Boolean")
      feildType should be ("Boolean")

      When("pass an Long value")
      feildType = datatypeBean.FindFeildType("2147483648") //Integer.MAX_VALUE = 2147483647
      Then("The feildType value should be Long")
      feildType should be ("Long")

      Given("test CheckKeys function")

      When("pass value in messgae fields")
      val messageFields = Array("id","name","company","work")
      val partitionkeyKeys = "id,name"
      datatypeBean.CheckKeys(messageFields,partitionkeyKeys)
      Then("no error shuold be raised")
    }
  }
}
