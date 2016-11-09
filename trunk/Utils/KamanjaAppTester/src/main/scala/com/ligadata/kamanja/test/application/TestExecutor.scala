package com.ligadata.kamanja.test.application

import com.ligadata.MetadataAPI.MetadataAPIImpl
import com.ligadata.kamanja.test.application.metadata._
import com.ligadata.test.embedded.kafka._
import com.ligadata.test.utils.{Globals, TestUtils}
import com.ligadata.MetadataAPI.test._
import com.ligadata.tools.kvinit.KVInit
import com.ligadata.tools.test._
import com.ligadata.kamanja.test.application.logging.KamanjaAppLogger

case class TestExecutorException(message: String, cause: Throwable = null) extends Exception(message, cause)

object TestExecutor {

  private val mdMan: MetadataManager = new MetadataManager

  private type OptionMap = Map[Symbol, Any]

  private def addApplicationMetadata(kamanjaApp: KamanjaApplication): Boolean = {
    var result = 1
    kamanjaApp.metadataElements.foreach(element => {
      try {
        element match {
          case e: MessageElement => {
            KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> Adding message from file '${e.filename}'")
            result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant))
          }
          case e: ContainerElement => {
            KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> Adding container from file '${e.filename}'")
            result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant))
            e.kvFilename match {
              case Some(file) =>
                KamanjaAppLogger.log(s"[Kamanja Application Tester] -----> Key-Value filename associated with container ${e.name} found. Adding data from $file")
                if(KVInit.run(Array("--typename", s"${e.name}",
                  "--config", EmbeddedServicesManager.kamanjaConfigFile,
                  "--datafiles", file,
                  "--ignorerecords", "1",
                  "--deserializer", "com.ligadata.kamanja.serializer.csvserdeser",
                  "--optionsjson", """{"alwaysQuoteFields": false, "fieldDelimiter":",", "valueDelimiter":"~"}"""
                )) != 0)
                  throw new TestExecutorException(s"[Kamanja Application Tester] ---> ***ERROR*** Failed to upload data from Key-Value file")
                else {
                  KamanjaAppLogger.log(s"[Kamanja Application Tester] -----> Successfully added Key-Value data")
                  // KVInit calls MetadataAPIImpl.CloseDB after it loads container data.
                  // Therefore, we need to call OpenDBStore in order to reopen it to avoid a nullpointer expceiont on future MetadataAPI calls.
                  MetadataAPIImpl.OpenDbStore(MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_PATHS").split(",").toSet, MetadataAPIImpl.GetMetadataAPIConfig.getProperty("METADATA_DATASTORE"))
                }
              case None =>
            }
          }
          case e: JavaModelElement => {
            KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> Adding java model from file '${e.filename}' with model configuration '${e.modelCfg}'")
            result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType), Some(e.modelCfg))
          }
          case e: ScalaModelElement => {
            KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> Adding scala model from file '${e.filename}' with model configuration '${e.modelCfg}'")
            result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType), Some(e.modelCfg))
          }
          case e: KPmmlModelElement => {
            KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> Adding KPMML model from file '${e.filename}'")
            result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType))
          }
          case e: PmmlModelElement => {
            KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> Adding PMML model from file '${e.filename}' with message consumed '${e.msgConsumed}'")
            result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType), None, Some("0.0.1"), Some(e.msgConsumed), None, e.msgProduced)
          }
          case e: AdapterMessageBindingElement => {
            KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> Adding adapter message bindings from file '${e.filename}'")
            result = mdMan.addBindings(e.filename)
          }
          case e: ModelConfigurationElement => {
            KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> Adding model configuration from file '${e.filename}'")
            result = mdMan.add(e.elementType, e.filename)
          }
          case _ => throw new TestExecutorException("[Kamanja Application Tester] - ***ERROR*** Unknown element type: '" + element.elementType)
        }
      }
      catch {
        case e: MetadataManagerException =>
          KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> ***ERROR*** Failed to add '${element.elementType}' from file '${element.filename}' with result '${result}' and exception:\n$e")
          return false
        case e: Exception =>
          KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> ***ERROR*** Failed to add '${element.elementType}' from file '${element.filename}' with result '${result}' and exception:\n$e")
          return false
      }

      if (result != 0) {
        KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> ***ERROR*** Failed to add '${element.elementType}' from file '${element.filename}' with result '$result'")
        return false
      }
      else
        KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> '${element.elementType}' successfully added")
    })
    return true
  }

  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      KamanjaAppLogger.log("[Kamanja Application Tester] -> ***ERROR*** Kamanja installation directory must be specified. --kamanja-dir /path/to/Kamanja/install/directory")
      return
    } else {
      val options = nextOption(Map(), args.toList)
      if (options == null) {
        return
      }
      val installDir: String = options('kamanjadir).asInstanceOf[String]
      val appManager = new KamanjaApplicationManager(installDir + "/test")

      appManager.kamanjaApplications.foreach(app => {
        KamanjaAppLogger.log(s"[Kamanja Application Tester] -> Beginning test for Kamanja Application '${app.name}'")
        KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> Starting Embedded Services...")
        EmbeddedServicesManager.init(installDir)
        if (!EmbeddedServicesManager.startServices) {
          KamanjaAppLogger.log(s"[Kamanja APplication Tester] ---> ***ERROR*** Failed to start embedded services")
          EmbeddedServicesManager.stopServices
          TestUtils.deleteFile(EmbeddedServicesManager.storageDir)
          throw new Exception(s"[Kamanja Application Tester] ---> ***ERROR*** Failed to start embedded services")
        }
        else {
          KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> Adding metadata...")
          if (!addApplicationMetadata(app)) {
            KamanjaAppLogger.log(s"[Kamanja Application Tester] -> ***ERROR*** Failed to add metadata for application '${app.name}'")
            throw new Exception(s"[Kamanja Application Tester] ---> ***ERROR*** Failed to add metadata for application '${app.name}'")
          }
          KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> All metadata successfully added")
        }
        var testResult = true

        val consumer = new TestKafkaConsumer(EmbeddedServicesManager.getOutputKafkaAdapterConfig)
        val consumerThread = new Thread(consumer)

        val errorConsumer = new TestKafkaConsumer(EmbeddedServicesManager.getErrorKafkaAdapterConfig)
        val errorConsumerThread = new Thread(errorConsumer)

        val eventConsumer = new TestKafkaConsumer(EmbeddedServicesManager.getEventKafkaAdapterConfig)
        val eventConsumerThread = new Thread(eventConsumer)

        consumerThread.start()
        errorConsumerThread.start()
        eventConsumerThread.start()

        KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> Processing data sets...")
        app.dataSets.foreach(set => {
          val resultsManager = new ResultsManager(EmbeddedServicesManager.getCluster)

          //TODO: For some reason, if we don't sleep, the consumer doesn't fully start until after the messages are pushed and the consumer won't pick up the messages that are already in kafka
          Thread sleep 1000

          val producer = new TestKafkaProducer
          KamanjaAppLogger.log(s"s[Kamanja Application Tester] -----> Pushing data from file ${set.inputDataFile} in format ${set.inputDataFormat}")
          producer.inputMessages(EmbeddedServicesManager.getInputKafkaAdapterConfig, set.inputDataFile, set.inputDataFormat, set.partitionKey.getOrElse("1"))

          //Reading in the expected results from the given data set into a List[String]. This will be used to count the number of results to expect in kafka as well.
          val expectedResults = resultsManager.parseExpectedResults(set)

          KamanjaAppLogger.log("[Kamanja Application Tester] ----->  Waiting for output results...")
          val results = Globals.waitForOutputResults(EmbeddedServicesManager.getOutputKafkaAdapterConfig, msgCount = expectedResults.length).getOrElse(null)
          if (results == null) {
            testResult = false
            KamanjaAppLogger.log(s"[Kamanja Application Tester] -----> ***ERROR*** Failed to retrieve results. Checking error queue...")
            val errors = Globals.waitForOutputResults(EmbeddedServicesManager.getErrorKafkaAdapterConfig, msgCount = expectedResults.length).getOrElse(null)
            if (errors != null) {
              errors.foreach(error => {
                KamanjaAppLogger.log(s"[Kamanja Application Tester] -----> Error Message: " + error)
              })
            }
            else {
              KamanjaAppLogger.log(s"[Kamanja Application Tester] -----> ***WARN*** Failed to discover messages in error queue")
              KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> Checking message event queue")

              val events = Globals.waitForOutputResults(EmbeddedServicesManager.getEventKafkaAdapterConfig, msgCount = expectedResults.length).getOrElse(null)
              if (events != null) {
                events.foreach(event => {
                  KamanjaAppLogger.log(s"[Kamanja Application Tester] -----> Event Message: $event")
                })
              }
              else {
                KamanjaAppLogger.log(s"[Kamanja Application Tester] -----> ***ERROR*** No event messages found. Kamanja did not process any input messages")
              }
            }
          }
          else if (results.length > expectedResults.length) {
            KamanjaAppLogger.log(s"[Kamanja Application Tester] ---> ***ERROR*** Found more output results than exist in expected results file ${set.expectedResultsFile}")
            KamanjaAppLogger.log(s"[Kamanja Application Tester] -----> Results Found: ")
            results.foreach(result => {
              KamanjaAppLogger.log(s"[Kamanja Application Tester] -------> Result: $result")
            })
            testResult = false
          }
          else {
            val matchResults = resultsManager.compareResults(set, results)
            var matchFailureCount = 0
            // If results don't match, display an error and increment the matchFailure count
            matchResults.foreach(matchResult => {
              if (!matchResult.matched) {
                KamanjaAppLogger.log(s"[Kamanja Application Tester] -----> ***ERROR*** Actual result and expected result do not match")
                KamanjaAppLogger.log(s"[Kamanja Application Tester] -------> Expected Result: ${matchResult.expectedResult}")
                KamanjaAppLogger.log(s"[Kamanja Application Tester] -------> Actual Result:   ${matchResult.actualResult}")
                KamanjaAppLogger.log(s"[Kamanja Application Tester] -------> Message Number:  ${matchResult.messageNumber}")
                matchFailureCount += 1
              }
            })

            if (matchFailureCount > 0) {
              testResult = false
              KamanjaAppLogger.log(s"[Kamanja Application Tester] -----> ***ERROR*** Data Set actual results differ from expected results. Data Set failed.")
              KamanjaAppLogger.log(s"[Kamanja Application Tester] -------> Expected Results File:   ${set.expectedResultsFile}")
              KamanjaAppLogger.log(s"[Kamanja Application Tester] -------> Expected Results Format: ${set.expectedResultsFormat}")
              KamanjaAppLogger.log(s"[Kamanja Application Tester] -------> Input Data File:         ${set.inputDataFile}")
              KamanjaAppLogger.log(s"[Kamanja Application Tester] -------> Input Data Format:       ${set.inputDataFormat}")
              KamanjaAppLogger.log(s"[Kamanja Application Tester] -------> Partition Key:           ${set.partitionKey.getOrElse("None")}")
            }
            else {
              KamanjaAppLogger.log(s"[Kamanja Application Tester] -----> Actual results match expected results")
            }
          }
        })

        if (!testResult)
          KamanjaAppLogger.log(s"[Kamanja Application Tester] -> ***ERROR*** Application '${app.name}' testing failed")
        else
          KamanjaAppLogger.log(s"[Kamanja Application Tester] -> Application '${app.name}' testing passed")

        consumer.shutdown
        eventConsumer.shutdown
        errorConsumer.shutdown

        EmbeddedServicesManager.stopServices
        KamanjaAppLogger.close
        TestUtils.deleteFile(EmbeddedServicesManager.storageDir)

      })
    }
  }

  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    def isSwitch(s: String) = (s(0) == '-')
    list match {
      case Nil => map
      case "--kamanja-dir" :: value :: tail =>
        nextOption(map ++ Map('kamanjadir -> value), tail)
      case option :: tail => {
        KamanjaAppLogger.log("[Kamanja Application Tester] - ***ERROR*** Unknown option " + option)
        return null
      }
    }
  }
}