package com.ligadata.kamanja.test.application

import com.ligadata.kamanja.test.application.configuration.EmbeddedConfiguration
import com.ligadata.kamanja.test.application.data.DataSet
import com.ligadata.kamanja.test.application.{MatchResult, ResultsManager}
import com.ligadata.test.utils.TestUtils
import org.scalatest._

class ResultsManagerTests extends FlatSpec with BeforeAndAfterAll {

  override def beforeAll: Unit = {
    KamanjaEnvironmentManager.init(TestSetup.kamanjaInstallDir)
  }

  override def afterAll: Unit = {
    KamanjaEnvironmentManager.stopServices
    TestUtils.deleteFile(EmbeddedConfiguration.storageDir)
  }

  "ResultsManager" should "compare an Array of actual results with expected results read from a file that are equal and in csv format and return a List of MatchResults that are all true" in {
    val resultsManager = new ResultsManager
    val testDataSet = new DataSet(TestSetup.kamanjaInstallDir + "/test/TestApp1/data/testApp1InputFile.csv", "CSV", TestSetup.kamanjaInstallDir + "/test/TestApp1/data/testApp1ExpectedResults.csv", "CSV", None)
    val actualResults: List[String] = List("1,John Clark,food,39.95",
      "2,James Brown,games,59.99",
      "3,Clark Gable,music,14.80",
      "1,John Clark,food~games,99.94",
      "2,James Brown,games~music,74.79",
      "3,Clark Gable,music~food,54.75",
      "1,John Clark,food~games~music,114.74",
      "2,James Brown,games~music~food,114.74",
      "3,Clark Gable,music~food~games,114.74"
    )

    val matchResults: List[MatchResult] = resultsManager.compareResults(testDataSet, actualResults)
    var count = 1
    matchResults.foreach(result => {
      info("message number: " + result.messageNumber)
      assert(result.messageNumber == count)
      count += 1
      info("actual result => " + result.actualResult)
      assert(result.actualResult != "")
      info("expected result => " + result.expectedResult)
      assert(result.expectedResult != "")
      info("matched: " + result.matched)
      assert(result.matched)
    })
  }

  it should "compare an Array of actual results with expected results read from a file that are equal in json format and return a List of MatchResults that are all true" in {
    val resultsManager = new ResultsManager
    val testDataSet = new DataSet(TestSetup.kamanjaInstallDir + "/test/TestApp1/data/testApp1InputFile.csv", "CSV", TestSetup.kamanjaInstallDir + "/test/TestApp1/data/testApp1ExpectedResults.json", "JSON", None)
    var actualResults: List[String] = List(
      """{"ID":1,"Name":"John Clark","Shopping List":["food"],"Total Cost":39.95}""",
      """{"ID":2,"Name":"James Brown","Shopping List":["games"],"Total Cost":59.99}""",
      """{"ID":3,"Name":"Clark Gable","Shopping List":["music"],"Total Cost":14.80}""",
      """{"ID":1,"Name":"John Clark","Shopping List":["food","games"],"Total Cost":99.94}""",
      """{"ID":2,"Name":"James Brown","Shopping List":["games","music"],"Total Cost":74.79}""",
      """{"ID":3,"Name":"Clark Gable","Shopping List":["music","food"],"Total Cost":54.75}""",
      """{"ID":1,"Name":"John Clark","Shopping List":["food","games","music"],"Total Cost":114.74}""",
      """{"ID":2,"Name":"James Brown","Shopping List":["games","music","food"],"Total Cost":114.74}""",
      """{"ID":3,"Name":"Clark Gable","Shopping List":["music","food","games"],"Total Cost":114.74}"""
    )
    //val expectedResults: List[String] = resultsManager.parseExpectedResults(testDataSet)
    val matchResults: List[MatchResult] = resultsManager.compareResults(testDataSet, actualResults)
    var count = 1
    matchResults.foreach(result => {
      info("message number: " + result.messageNumber)
      assert(result.messageNumber == count)
      count += 1
      info("actual result => " + result.actualResult)
      assert(result.actualResult != "")
      info("expected result => " + result.expectedResult)
      assert(result.expectedResult != "")
      info("matched: " + result.matched)
      assert(result.matched)
    })
  }
}
