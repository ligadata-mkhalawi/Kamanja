package com.ligadata.kamanja.test.application

import java.io.File

import com.ligadata.kamanja.test.application.data.DataSet
import com.ligadata.test.configuration.cluster._
import com.ligadata.test.configuration.cluster.adapters._
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.io.Source

case class MatchResult(messageNumber: Int, expectedResult: String, actualResult: String, matched: Boolean)

/*
  ResultsManager will take a DataSet and Cluster configuration and push the input data, parse the expected results data, and read in output results processed by the engine.
  This is expecting EmbeddedServicesManager to have already started the services. Otherwise, ResultsManager will fail to connect to Kafka.
 */
class ResultsManager(cluster: Cluster) {

  def compareResults(dataSet: DataSet, actualResults: List[String]): List[MatchResult] = {
    if(dataSet.expectedResultsFormat.toLowerCase == "csv") compareCsvResults(dataSet, actualResults)
    else compareJsonResults(dataSet, actualResults)
  }

  def parseExpectedResults(dataSet: DataSet): List[String] = {
    var source: Source = null
    if(!(new File(dataSet.expectedResultsFile).exists))
      throw new Exception(s"[Kamanja Application Tester] ---> ***ERROR*** Expected results file '${dataSet.expectedResultsFile}' does not exist.")
    if(dataSet.expectedResultsFormat.toLowerCase == "csv") {
      try {
        source = Source.fromFile(dataSet.expectedResultsFile)
        source.getLines().toList
      }
      catch {
        case e: Exception => throw new Exception(s"[Kamanja Application Tester] ---> ***ERROR*** Failed to read file '${dataSet.expectedResultsFile}'", e)
      }
      finally {
        source.close()
      }
    }
    else {
      try {
        val sb = new StringBuilder
        source = Source.fromFile(dataSet.expectedResultsFile)
        source.getLines().toList.foreach(line => {
          sb ++= line
        })
        val jsonStr = sb.mkString
        val json = parse(jsonStr)

        implicit val formats = org.json4s.DefaultFormats
        val expectedResultsList = json.extract[List[Map[String,Any]]]
        var stringResultsList: List[String] = List()
        expectedResultsList.foreach(map => {
          stringResultsList :+= org.json4s.jackson.Serialization.write(map)
        })

        return stringResultsList
      }
      catch {
        case e: MappingException => throw new Exception(s"[Kamanja Application Tester] ---> ***ERROR*** Failed to parse json", e)
        case e: Exception => throw new Exception(s"[Kamanja Application Tester] ---> ***ERROR*** Failed to read file '${dataSet.expectedResultsFile}'", e)
      }
      finally {
        source.close()
      }
    }
  }

  private def expectedResultsCount(dataSet: DataSet): Int = parseExpectedResults(dataSet).length

  private val inputAdapterConfig: KafkaAdapterConfig = cluster.adapters.filter(_.asInstanceOf[KafkaAdapterConfig].adapterSpecificConfig.topicName.toLowerCase == "testin_1")(0).asInstanceOf[KafkaAdapterConfig]

  private def compareCsvResults(dataSet: DataSet, actualResults: List[String]): List[MatchResult] = {
    val expectedResults = parseExpectedResults(dataSet)
    var matchResults: List[MatchResult] = List[MatchResult]()
    if(actualResults != null && actualResults.length > 0) {
      var count = 0
      expectedResults.foreach(expectedResult => {
        if(expectedResult != actualResults(count)) {
          matchResults = matchResults :+ new MatchResult(count + 1, expectedResult, actualResults(count), false)
          count += 1
        }
        else {
          matchResults = matchResults :+ new MatchResult(count + 1, expectedResult, actualResults(count), true)
          count += 1
        }
      })
      return matchResults
    }
    else {
      throw new Exception("[Kamanja Application Tester] -----> ***ERROR*** Failed to retrieve output results. Please ensure messages were processed correctly.")
    }
  }

  private def compareJsonResults(dataSet: DataSet, actualResults: List[String]): List[MatchResult] = {
    val expectedResults = parseExpectedResults(dataSet)
    var matchResults: List[MatchResult] = List[MatchResult]()
    if(actualResults != null && actualResults.length > 0) {
      var count = 0
      expectedResults.foreach(expectedResult => {
        val actualResult = compact(render(parse(actualResults(count))))
        if(expectedResult != actualResult){
          matchResults = matchResults :+ new MatchResult(count + 1, expectedResult, actualResult, false)
          count += 1
        }
        else {
          matchResults = matchResults :+ new MatchResult(count + 1, expectedResult, actualResult, true)
          count += 1
        }
      })
      return matchResults
    }
    else {
      throw new Exception("[Kamanja Application Tester] -----> ***ERROR*** Failed to retrieve output results. please ensure messages were processed correctly.")
    }
  }
}