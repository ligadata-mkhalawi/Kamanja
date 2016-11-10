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
            KamanjaAppLogger.info(s"Adding message from file '${e.filename}'")
            result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant))
          }
          case e: ContainerElement => {
            KamanjaAppLogger.info(s"Adding container from file '${e.filename}'")
            result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant))
            e.kvFilename match {
              case Some(file) =>
                KamanjaAppLogger.info(s"Key-Value filename associated with container ${e.name} found. Adding data from $file")
                if(KVInit.run(Array("--typename", s"${e.name}",
                  "--config", EmbeddedServicesManager.kamanjaConfigFile,
                  "--datafiles", file,
                  "--ignorerecords", "1",
                  "--deserializer", "com.ligadata.kamanja.serializer.csvserdeser",
                  "--optionsjson", """{"alwaysQuoteFields": false, "fieldDelimiter":",", "valueDelimiter":"~"}"""
                )) != 0)
                  throw new TestExecutorException(s"***ERROR*** Failed to upload data from Key-Value file")
                else {
                  KamanjaAppLogger.info(s"Successfully added Key-Value data")
                  // KVInit calls MetadataAPIImpl.CloseDB after it loads container data.
                  // Therefore, we need to call OpenDBStore in order to reopen it to avoid a nullpointer expceiont on future MetadataAPI calls.
                  MetadataAPIImpl.OpenDbStore(MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_PATHS").split(",").toSet, MetadataAPIImpl.GetMetadataAPIConfig.getProperty("METADATA_DATASTORE"))
                }
              case None =>
            }
          }
          case e: JavaModelElement => {
            KamanjaAppLogger.info(s"Adding java model from file '${e.filename}' with model configuration '${e.modelCfg}'")
            result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType), Some(e.modelCfg))
          }
          case e: ScalaModelElement => {
            KamanjaAppLogger.info(s"Adding scala model from file '${e.filename}' with model configuration '${e.modelCfg}'")
            result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType), Some(e.modelCfg))
          }
          case e: KPmmlModelElement => {
            KamanjaAppLogger.info(s"Adding KPMML model from file '${e.filename}'")
            result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType))
          }
          case e: PmmlModelElement => {
            KamanjaAppLogger.info(s"Adding PMML model from file '${e.filename}' with message consumed '${e.msgConsumed}'")
            result = mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType), None, Some("0.0.1"), Some(e.msgConsumed), None, e.msgProduced)
          }
          case e: AdapterMessageBindingElement => {
            KamanjaAppLogger.info(s"Adding adapter message bindings from file '${e.filename}'")
            result = mdMan.addBindings(e.filename)
          }
          case e: ModelConfigurationElement => {
            KamanjaAppLogger.info(s"Adding model configuration from file '${e.filename}'")
            result = mdMan.add(e.elementType, e.filename)
          }
          case _ => throw new TestExecutorException("***ERROR*** Unknown element type: '" + element.elementType)
        }
      }
      catch {
        case e: MetadataManagerException =>
          KamanjaAppLogger.error(s"Failed to add '${element.elementType}' from file '${element.filename}' with result '${result}' and exception:\n$e")
          return false
        case e: Exception =>
          KamanjaAppLogger.error(s"***ERROR*** Failed to add '${element.elementType}' from file '${element.filename}' with result '${result}' and exception:\n$e")
          return false
      }

      if (result != 0) {
        KamanjaAppLogger.error(s"***ERROR*** Failed to add '${element.elementType}' from file '${element.filename}' with result '$result'")
        return false
      }
      else
        KamanjaAppLogger.info(s"'${element.elementType}' successfully added")
    })
    return true
  }

  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      KamanjaAppLogger.error("***ERROR*** Kamanja installation directory must be specified. --kamanja-dir /path/to/Kamanja/install/directory")
      return
    } else {
      val options = nextOption(Map(), args.toList)
      if (options == null) {
        return
      }
      val installDir: String = options('kamanjadir).asInstanceOf[String]
      val appManager = new KamanjaApplicationManager(installDir + "/test")

      appManager.kamanjaApplications.foreach(app => {
        KamanjaAppLogger.info(s"Beginning test for Kamanja Application '${app.name}'")
        KamanjaAppLogger.info(s"Starting Embedded Services...")
        EmbeddedServicesManager.init(installDir)
        if (!EmbeddedServicesManager.startServices) {
          KamanjaAppLogger.error(s"***ERROR*** Failed to start embedded services")
          EmbeddedServicesManager.stopServices
          TestUtils.deleteFile(EmbeddedServicesManager.storageDir)
          throw new Exception(s"***ERROR*** Failed to start embedded services")
        }
        else {
          KamanjaAppLogger.info(s"Adding metadata...")
          if (!addApplicationMetadata(app)) {
            KamanjaAppLogger.error(s"***ERROR*** Failed to add metadata for application '${app.name}'")
            throw new Exception(s"***ERROR*** Failed to add metadata for application '${app.name}'")
          }
          KamanjaAppLogger.info(s"All metadata successfully added")
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

        KamanjaAppLogger.info(s"Processing data sets...")
        app.dataSets.foreach(set => {
          val resultsManager = new ResultsManager(EmbeddedServicesManager.getCluster)

          //TODO: For some reason, if we don't sleep, the consumer doesn't fully start until after the messages are pushed and the consumer won't pick up the messages that are already in kafka
          Thread sleep 1000

          val producer = new TestKafkaProducer
          KamanjaAppLogger.info(s"sPushing data from file ${set.inputDataFile} in format ${set.inputDataFormat}")
          producer.inputMessages(EmbeddedServicesManager.getInputKafkaAdapterConfig, set.inputDataFile, set.inputDataFormat, set.partitionKey.getOrElse("1"))

          //Reading in the expected results from the given data set into a List[String]. This will be used to count the number of results to expect in kafka as well.
          val expectedResults = resultsManager.parseExpectedResults(set)

          KamanjaAppLogger.info("Waiting for output results...")
          val results = Globals.waitForOutputResults(EmbeddedServicesManager.getOutputKafkaAdapterConfig, msgCount = expectedResults.length).getOrElse(null)
          if (results == null) {
            testResult = false
            KamanjaAppLogger.error(s"***ERROR*** Failed to retrieve results. Checking error queue...")
            val errors = Globals.waitForOutputResults(EmbeddedServicesManager.getErrorKafkaAdapterConfig, msgCount = expectedResults.length).getOrElse(null)
            if (errors != null) {
              errors.foreach(error => {
                KamanjaAppLogger.info(s"Error Message: " + error)
              })
            }
            else {
              KamanjaAppLogger.warn(s"***WARN*** Failed to discover messages in error queue")
              KamanjaAppLogger.warn(s"Checking message event queue")

              val events = Globals.waitForOutputResults(EmbeddedServicesManager.getEventKafkaAdapterConfig, msgCount = expectedResults.length).getOrElse(null)
              if (events != null) {
                events.foreach(event => {
                  KamanjaAppLogger.info(s"Event Message: $event")
                })
              }
              else {
                KamanjaAppLogger.error(s"***ERROR*** No event messages found. Kamanja did not process any input messages")
              }
            }
          }
          else if (results.length > expectedResults.length) {
            KamanjaAppLogger.error(s"***ERROR*** Found more output results than exist in expected results file ${set.expectedResultsFile}")
            KamanjaAppLogger.error(s"Results Found: ")
            results.foreach(result => {
              KamanjaAppLogger.error(s"Result: $result")
            })
            testResult = false
          }
          else {
            val matchResults = resultsManager.compareResults(set, results)
            var matchFailureCount = 0
            // If results don't match, display an error and increment the matchFailure count
            matchResults.foreach(matchResult => {
              if (!matchResult.matched) {
                KamanjaAppLogger.error(s"***ERROR*** Actual result and expected result do not match")
                KamanjaAppLogger.error(s"Expected Result: ${matchResult.expectedResult}")
                KamanjaAppLogger.error(s"Actual Result:   ${matchResult.actualResult}")
                KamanjaAppLogger.error(s"Message Number:  ${matchResult.messageNumber}")
                matchFailureCount += 1
              }
            })

            if (matchFailureCount > 0) {
              testResult = false
              KamanjaAppLogger.error(s"***ERROR*** Data Set actual results differ from expected results. Data Set failed.")
              KamanjaAppLogger.error(s"Expected Results File:   ${set.expectedResultsFile}")
              KamanjaAppLogger.error(s"Expected Results Format: ${set.expectedResultsFormat}")
              KamanjaAppLogger.error(s"Input Data File:         ${set.inputDataFile}")
              KamanjaAppLogger.error(s"Input Data Format:       ${set.inputDataFormat}")
              KamanjaAppLogger.error(s"Partition Key:           ${set.partitionKey.getOrElse("None")}")
            }
            else {
              KamanjaAppLogger.info(s"Actual results match expected results")
            }
          }
        })

        if (!testResult)
          KamanjaAppLogger.error(s"***ERROR*** Application '${app.name}' testing failed")
        else
          KamanjaAppLogger.info(s"Application '${app.name}' testing passed")

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
        KamanjaAppLogger.info("***ERROR*** Unknown option " + option)
        return null
      }
    }
  }
}