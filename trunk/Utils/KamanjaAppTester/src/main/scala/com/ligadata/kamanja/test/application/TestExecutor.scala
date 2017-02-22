package com.ligadata.kamanja.test.application

import com.ligadata.MetadataAPI.MetadataAPIImpl
import com.ligadata.kamanja.test.application.metadata._
import com.ligadata.test.embedded.kafka._
import com.ligadata.test.utils.{Globals, TestUtils}
import com.ligadata.MetadataAPI.test._
import com.ligadata.kamanja.test.application.configuration.EmbeddedConfiguration
import com.ligadata.tools.kvinit.KVInit
import com.ligadata.tools.test._
import com.ligadata.kamanja.test.application.logging.KamanjaAppLogger
import scala.util.control.Breaks._

case class TestExecutorException(message: String, cause: Throwable = null) extends Exception(message, cause)

object TestExecutor {

  private val mdMan: MetadataManager = new MetadataManager
  private var logger: KamanjaAppLogger = _
  private val usage: String = "The following arguments are accepted:\n\t" +
    "--kamanja-dir (Required: the install directory of Kamanja)\n\t" +
    "--metadata-config (Optional: the metadata configuration file you'd like to use. If not given, an internal configuration will be generated automatically.)\n\t" +
    "--cluster-config (Optional: the cluster configuration file you'd like to use. If not given, an internal configuration will be generated automatically.)\n\t" +
    "--app-name (Optional: the application you would like to test.)\n\t" +
    "--skip-metadata (Optional: skip adding metadata for an application. Primarily used if not cleaning up after tests)\n\t" +
    "--help (Optional: Displays help text)"

  private type OptionMap = Map[Symbol, Any]
  private var runEmbedded: Boolean = false

  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("***ERROR*** Kamanja installation directory must be specified. --kamanja-dir /path/to/Kamanja/install/directory")
      println(usage)
      return
    }
    else if (args.length == 1) {
      val options = optionMap(Map(), args.toList)
      if (options == null) {
        println(usage)
        return
      }
      val help = options.getOrElse('help, null)
      if (help != null) {
        println(usage)
        return
      }
    }
    else {
      val options = optionMap(Map(), args.toList)
      if (options == null) {
        println(usage)
        return
      }
      val installDir: String = options('kamanjadir).asInstanceOf[String]
      val metadataConfigFile: String = options.getOrElse('metadataconfig, null).asInstanceOf[String]
      val clusterConfigFile: String = options.getOrElse('clusterconfig, null).asInstanceOf[String]
      val appName: String = options.getOrElse('appname, null).asInstanceOf[String]
      val skipMetadata: Boolean = options.getOrElse('skipmetadata, false).asInstanceOf[Boolean]
      logger = KamanjaAppLogger.createKamanjaAppLogger(installDir)
      val appManager = new KamanjaApplicationManager(installDir + "/test")

      appManager.kamanjaApplications.foreach(app => {
        breakable {
          if (appName != null && !appName.equalsIgnoreCase(app.name)) {
            logger.info(s"Ignore Kamanja Application '${app.name}'")
            break
          }
          logger.info(s"Beginning test for Kamanja Application '${app.name}'")
          // Initializing Kamanja Environment Manager, which will deal with setting up metadata manager and
          /// generating config files and start embedded services if user doesn't provide said files.
          KamanjaEnvironmentManager.init(installDir, metadataConfigFile, clusterConfigFile)

          if(!skipMetadata) {
            logger.info(s"Adding metadata...")
            if (!appManager.addApplicationMetadata(app)) {
              logger.error(s"***ERROR*** Failed to add metadata for application '${app.name}'")
              throw new Exception(s"***ERROR*** Failed to add metadata for application '${app.name}'")
            }
            logger.info(s"All metadata successfully added")
          }
          else {
            logger.warn(s"***WARN*** Skip Metadata option set to true. Not adding metadata.")
          }
          var testResult = true

          val consumer = new TestKafkaConsumer(KamanjaEnvironmentManager.getOutputKafkaAdapterConfig)
          val consumerThread = new Thread(consumer)

          val errorConsumer = new TestKafkaConsumer(KamanjaEnvironmentManager.getErrorKafkaAdapterConfig)
          val errorConsumerThread = new Thread(errorConsumer)

          val eventConsumer = new TestKafkaConsumer(KamanjaEnvironmentManager.getEventKafkaAdapterConfig)
          val eventConsumerThread = new Thread(eventConsumer)

          consumerThread.start()
          errorConsumerThread.start()
          eventConsumerThread.start()

          logger.info(s"Processing data sets...")
          app.dataSets.foreach(set => {
            val resultsManager = new ResultsManager

            //TODO: For some reason, if we don't sleep, the consumer doesn't fully start until after the messages are pushed and the consumer won't pick up the messages that are already in kafka
            Thread sleep 1000

            val producer = new TestKafkaProducer
            logger.info(s"Pushing data from file ${set.inputDataFile} in format ${set.inputDataFormat}")
            producer.inputMessages(KamanjaEnvironmentManager.getInputKafkaAdapterConfig, set.inputDataFile, set.inputDataFormat, set.partitionKey.getOrElse("1"))

            //Reading in the expected results from the given data set into a List[String]. This will be used to count the number of results to expect in kafka as well.
            val expectedResults = resultsManager.parseExpectedResults(set)


            logger.info("Waiting for output results...")
            val results = Globals.waitForOutputResults(KamanjaEnvironmentManager.getOutputKafkaAdapterConfig, msgCount = expectedResults.length).getOrElse(null)
            if (results == null) {
              testResult = false
              logger.error(s"***ERROR*** Failed to retrieve results. Checking error queue...")
              val errors = Globals.waitForOutputResults(KamanjaEnvironmentManager.getErrorKafkaAdapterConfig, msgCount = expectedResults.length).getOrElse(null)
              if (errors != null) {
                errors.foreach(error => {
                  logger.info(s"Error Message: " + error)
                })
              }
              else {
                logger.warn(s"***WARN*** Failed to discover messages in error queue")
                logger.warn(s"Checking message event queue")

                val events = Globals.waitForOutputResults(KamanjaEnvironmentManager.getEventKafkaAdapterConfig, msgCount = expectedResults.length).getOrElse(null)
                if (events != null) {
                  events.foreach(event => {
                    logger.info(s"Event Message: $event")
                  })
                }
                else {
                  logger.error(s"***ERROR*** No event messages found. Kamanja did not process any input messages")
                }
              }
            }
            else if (results.length > expectedResults.length) {
              logger.error(s"***ERROR*** Found more output results than exist in expected results file ${set.expectedResultsFile}")
              logger.error(s"Results Found: ")
              results.foreach(result => {
                logger.error(s"Result: $result")
              })
              testResult = false
            }
            else {
              val matchResults = resultsManager.compareResults(set, results)
              var matchFailureCount = 0
              // If results don't match, display an error and increment the matchFailure count
              matchResults.foreach(matchResult => {
                if (!matchResult.matched) {
                  logger.error(s"***ERROR*** Actual result and expected result do not match")
                  logger.error(s"Expected Result: ${matchResult.expectedResult}")
                  logger.error(s"Actual Result:   ${matchResult.actualResult}")
                  logger.error(s"Message Number:  ${matchResult.messageNumber}")
                  matchFailureCount += 1
                }
              })

              if (matchFailureCount > 0) {
                testResult = false
                logger.error(s"***ERROR*** Data Set actual results differ from expected results. Data Set failed.")
                logger.error(s"Expected Results File:   ${set.expectedResultsFile}")
                logger.error(s"Expected Results Format: ${set.expectedResultsFormat}")
                logger.error(s"Input Data File:         ${set.inputDataFile}")
                logger.error(s"Input Data Format:       ${set.inputDataFormat}")
                logger.error(s"Partition Key:           ${set.partitionKey.getOrElse("None")}")
              }
              else {
                logger.info(s"Actual results match expected results")
              }
            }
          })

          if (!testResult)
            logger.error(s"***ERROR*** Application '${app.name}' testing failed")
          else
            logger.info(s"Application '${app.name}' testing passed")

          consumer.shutdown
          eventConsumer.shutdown
          errorConsumer.shutdown

          if (!appManager.removeApplicationMetadata(app)) {
            logger.error(s"*** Failed to remove metadata from application '${app.name}'")
            throw new Exception(s"*** Failed to remove metadata from application '${app.name}'")
          }

          if (KamanjaEnvironmentManager.isEmbedded) {
            KamanjaEnvironmentManager.stopServices
            TestUtils.deleteFile(EmbeddedConfiguration.storageDir)
          }

          logger.close
        }
      })
    }
  }

  private def optionMap(map: OptionMap, list: List[String]): OptionMap = {
    def isSwitch(s: String) = (s(0) == '-')

    list match {
      case Nil => map
      case "--kamanja-dir" :: value :: tail =>
        optionMap(map ++ Map('kamanjadir -> value), tail)
      case "--metadata-config" :: value :: tail =>
        optionMap(map ++ Map('metadataconfig -> value), tail)
      case "--cluster-config" :: value :: tail =>
        optionMap(map ++ Map('clusterconfig -> value), tail)
      case "--app-name" :: value :: tail =>
        optionMap(map ++ Map('appname -> value), tail)
      case "--skip-metadata" :: tail =>
        optionMap(map ++ Map('skipmetadata -> true), tail)
      case "--help" :: tail =>
        optionMap(map ++ Map('help -> true), tail)
      case opt => {
        println("***ERROR*** Unknown option " + opt)
        return null
      }
    }
  }
}