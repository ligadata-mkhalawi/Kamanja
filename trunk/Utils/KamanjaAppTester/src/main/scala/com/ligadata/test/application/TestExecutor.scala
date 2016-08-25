package com.ligadata.test.application

import com.ligadata.MetadataAPI.test._
import com.ligadata.test.application.data.DataSet
import com.ligadata.test.application.metadata._
import com.ligadata.test.application.metadata.interfaces.ModelElement
import com.ligadata.test.configuration.cluster.adapters.KafkaAdapterConfig
import com.ligadata.test.utils.Globals
import com.ligadata.tools.test._

case class TestExecutorException(message: String, cause: Throwable = null) extends Exception(message, cause)

object TestExecutor {

  private val mdMan: MetadataManager = new MetadataManager

  private type OptionMap = Map[Symbol, Any]

  private def addApplicationMetadata(kamanjaApp: KamanjaApplication): Boolean = {
    var result = 0
    kamanjaApp.metadataElements.foreach(element => {
      element match {
        case e: MessageElement => {
          println(s"[Kamanja Application Tester] ---> Adding message from file '${e.filename}'")
          result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant))
        }
        case e: ContainerElement => {
          println(s"[Kamanja Application Tester] ---> Adding container from file '${e.filename}'")
          result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant))
        }
        case e: JavaModelElement => {
          println(s"[Kamanja Application Tester] ---> Adding java model from file '${e.filename}' with model configuration '${e.modelCfg}'")
          result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType), Some(e.modelCfg))
        }
        case e: ScalaModelElement => {
          println(s"[Kamanja Application Tester] ---> Adding scala model from file '${e.filename}' with model configuration '${e.modelCfg}'")
          result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType), Some(e.modelCfg))
        }
        case e: KPmmlModelElement => {
          println(s"[Kamanja Application Tester] ---> Adding KPMML model from file '${e.filename}'")
          result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType))
        }
        case e: PmmlModelElement => {
          println(s"[Kamanja Application Tester] ---> Adding PMML model from file '${e.filename}' with message consumed '${e.msgConsumed}'")
          result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType), None, Some("0.0.1"), Some(e.msgConsumed), None, e.msgProduced)
        }
        case e: AdapterMessageBindingElement => {
          println(s"[Kamanja Application Tester] ---> Adding adapter message bindings from file '${e.filename}'")
          result = mdMan.addBindings(e.filename)
        }
        case e: ModelConfigurationElement => {
          println(s"[Kamanja Application Tester] ---> Adding model configuration from file '${e.filename}'")
          result = mdMan.add(e.elementType, e.filename)
        }
        case _ => throw new TestExecutorException("[Kamanja Application Tester] - ***ERROR*** Unknown element type: '" + element.elementType)
      }

      if(result != 0) {
        println(s"[Kamanja Application Tester] ---> ***ERROR*** Failed too add '${element.elementType}' from file '${element.filename}' with result '$result'")
        return false
      }
      else
        println(s"[Kamanja Application Tester] ---> '${element.elementType}' successfully added")
    })
    return true
  }

  //TODO: Need to determine if we want the user to specify the kamanja install directory.
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("[Kamanja Application Tester] -> ***ERROR*** Kamanja installation directory must be specified. --kamanja-dir /path/to/Kamanja/install/directory")
      return
    } else {
      val options = nextOption(Map(), args.toList)
      if (options == null) {
        return
      }
      val installDir: String = options('kamanjadir).asInstanceOf[String]
      val appManager = new KamanjaApplicationManager(installDir + "/test")
      var mdAddResult = true
      appManager.kamanjaApplications.foreach(app => {
        println(s"[Kamanja Application Tester] -> Adding metadata for application '${app.name}'")
        if (!addApplicationMetadata(app)) {
          println(s"[Kamanja Application Tester] -> ***ERROR*** Failed to add metadata for application '${app.name}'")
          throw new Exception(s"[Kamanja Application Tester] -> ***ERROR*** Failed to add metadata for application '${app.name}'")
        }
        println(s"[Kamanja Application Tester] -> Metadata added for application '${app.name}'")
      })

      println("[Kamanja Application Tester] -> Starting Embedded Services...")
      if (!EmbeddedServicesManager.startServices(installDir)) {
        println(s"[Kamanja Application Tester] -> ***ERROR*** Failed to start embedded services")
        throw new Exception(s"[Kamanja Application Tester] -> ***ERROR*** Failed to start embedded services")
      }
      else {
        var testResult = true
        appManager.kamanjaApplications.foreach(app => {
          println(s"[Kamanja Application Tester] -> Processing Data Sets...")
          app.dataSets.foreach(set => {
            if(!processDataSet(set, EmbeddedServicesManager.getInputKafkaAdapterConfig)){
              testResult = false
            }
          })
          if(!testResult)
            println(s"[Kamanja Application Tester] -> ***ERROR*** Application '${app.name}' testing failed")
          else
            println(s"[Kamanja Application Tester] -> Application '${app.name}' testing passed")
        })
      }
    }
  }

  private def processDataSet(dataSet: DataSet, kafkaAdapterConfig: KafkaAdapterConfig): Boolean = {
    println("[Kamanja Application Tester] ---> Processing test data...")
    // Determine number of messages in expected results file
    var messageCount = 0
    var expectedResults: List[String] = List()
    var nonMatchingMessages: Map[String, Map[String, String]] = Map()
    var actualResults: List[String] = List()

    if(dataSet.expectedResultsFormat.toLowerCase == "csv") {
      val source = scala.io.Source.fromFile(dataSet.expectedResultsFile)
      expectedResults = source.getLines().toList
      messageCount = expectedResults.length
      source.close()
    }
    else if(dataSet.expectedResultsFormat.toLowerCase == "json") {
      throw new NotImplementedError("JSON is not yet supported")
    }

    if(messageCount == 0) {
      println(s"[Kamanja Application Tester] -----> ***ERROR*** No messages were found in expected results file '${dataSet.expectedResultsFile}'")
      return false
    }
    println(s"[Kamanja Application Tester] -----> $messageCount expected results read in from '${dataSet.expectedResultsFile}'")

    val producer = new TestKafkaProducer
    println(s"[Kamanja Application Tester] -----> Pushing data from '${dataSet.inputDataFile}' to Kafka...")
    try {
      producer.inputMessages(kafkaAdapterConfig, dataSet.inputDataFile, dataSet.partitionKey.getOrElse("1"))
      println(s"[Kamanja Application Tester] -----> Successfully pushed data")
    }
    catch {
      case e: Exception => println(s"[Kamanja Application Tester] -----> ***ERROR*** Failed to push data from '${dataSet.inputDataFile}'")
    }

    actualResults = Globals.waitForOutputResults(kafkaAdapterConfig, 30, messageCount).getOrElse(null)
    if(actualResults != null) {
      var count = 0
      expectedResults.foreach(expectedResult => {
        if(expectedResult != actualResults(count)){
          nonMatchingMessages += ("Message Number: " + count -> Map(expectedResult -> actualResults(count)))
        }
      })
    }
    else {
      println("[Kamanja Application Tester] ---> ***ERROR*** Failed to retrieve results from Kafka")
      return false
    }
    if(nonMatchingMessages.size > 0) {
      println("[Kamanja Application Tester] ---> ***ERROR*** Some output messages did not match the expected output results")
      nonMatchingMessages.foreach(messageMap => {
        println(s"[Kamanja Application Tester] -----> Nonmatching result")
        println(s"[Kamanja Application Tester] -------> $messageMap")
      })
      return false
    }
    else return true
  }

  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    def isSwitch(s: String) = (s(0) == '-')
    list match {
      case Nil => map
      case "--kamanja-dir" :: value :: tail =>
        nextOption(map ++ Map('kamanjadir -> value), tail)
      case option :: tail => {
        println("[Kamanja Application Tester] - ***ERROR*** Unknown option " + option)
        return null
      }
    }
  }
}