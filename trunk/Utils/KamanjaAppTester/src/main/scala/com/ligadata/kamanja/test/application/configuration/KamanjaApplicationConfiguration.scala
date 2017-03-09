package com.ligadata.kamanja.test.application.configuration

import java.io.File

import com.ligadata.kamanja.test.application.KamanjaApplication
import com.ligadata.kamanja.test.application.data._
import com.ligadata.kamanja.test.application.logging.KamanjaAppLogger
import com.ligadata.kamanja.test.application.metadata._
import com.ligadata.kamanja.test.application.metadata.interfaces.MetadataElement
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source

class KamanjaApplicationConfiguration {

  private var logger: KamanjaAppLogger = _

  def initializeApplication(applicationDirectory: String, applicationConfiguration: String): KamanjaApplication = {
    try {
      logger = KamanjaAppLogger.getKamanjaAppLogger
    }
    catch {
      case e: Exception => throw new KamanjaApplicationConfigurationException("Kamanja Application Logger has not be created. Please call createKamanjaAppLogger first.")
    }
    val config: File = new File(applicationConfiguration)
    if (!config.exists()) {
      throw new KamanjaApplicationConfigurationException("***ERROR*** Configuration File: '" + config + "' does not exist")
    }

    val source = Source.fromFile(config)
    val jsonStr: String = source.getLines().mkString
    source.close()

    val json = parse(jsonStr)

    try {
      val appName = (json \ "Application" \ "Name").values.toString
      logger.info("Initializing Application '" + appName + "'")
      return new KamanjaApplication(appName, applicationDirectory, parseMdElements(applicationDirectory, json), parseDataSets(applicationDirectory, json))
    }
    catch {
      case e: KamanjaApplicationConfigurationException => {
        logger.error("***ERROR*** Failed to initialize Kamanja Application\n" + logger.getStackTraceAsString(e))
        throw new KamanjaApplicationConfigurationException("***ERROR*** Failed to initialize Kamanja Application", e)
      }
      case e: Exception => {
        logger.error("***ERROR*** Unexpected exception encountered. Failed to initialize Kamanja Application.\n" + logger.getStackTraceAsString(e))
        throw new KamanjaApplicationConfigurationException("***ERROR*** Unexpected exception encountered. Failed to initialize Kamanja Application.", e)
      }
    }
  }

  private def parseMdElements(appDir: String, configStr: JValue): List[MetadataElement] = {
    var metadataElements: List[MetadataElement] = List()
    val mdElems: List[Map[String, Any]] = (configStr \\ "MetadataElements").values.asInstanceOf[List[Map[String, Any]]]

    mdElems.foreach(elem => {
      elem("Type").toString.toLowerCase match {
        case "container" => {
          if (!elem.keySet.exists(_ == "Filename")) {
            logger.error("***ERROR*** Metadata Element Type 'Container' requires 'Filename' to be defined.")
            throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Container' requires 'Filename' to be defined.")
          }
          val kvFile: Option[String] = if (elem.keySet.exists(_ == "KVFile")) Some(s"$appDir/data/${elem("KVFile").toString}") else None
          metadataElements = metadataElements :+ new ContainerElement(appDir + "/metadata/container/" + elem("Filename").toString, kvFile)
        }
        case "message" => {
          if (!elem.keySet.exists(_ == "Filename")) {
            logger.error("***ERROR*** Metadata Element Type 'Message' requires 'Filename' to be defined.")
            throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Message' requires 'Filename' to be defined.")
          }

          metadataElements = metadataElements :+ new MessageElement(appDir + "/metadata/message/" + elem("Filename").toString)
        }
        case "model" => {
          elem("ModelType").toString.toLowerCase match {
            case "java" =>
              if (!elem.keySet.exists(_ == "Filename")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'Java' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'Java' requires 'Filename' to be defined.")
              }

              if (!elem.keySet.exists(_ == "ModelConfiguration")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'Java' requires 'ModelConfiguration' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'Java' requires 'ModelConfiguration' to be defined.")
              }

              metadataElements = metadataElements :+ new JavaModelElement(appDir + "/metadata/model/" + elem("Filename").toString, elem("ModelConfiguration").toString)
            case "scala" => {
              if (!elem.keySet.exists(_ == "Filename")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'Scala' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'Scala' requires 'Filename' to be defined.")
              }
              if (!elem.keySet.exists(_ == "ModelConfiguration")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'Scala' requires 'ModelConfiguration' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'Scala' requires 'ModelConfiguration' to be defined.")
              }
              metadataElements = metadataElements :+ new ScalaModelElement(appDir + "/metadata/model/" + elem("Filename").toString, elem("ModelConfiguration").toString)
            }
            case "kpmml" => {
              if (!elem.keySet.exists(_ == "Filename")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'KPMML' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'KPMML' requires 'Filename' to be defined.")
              }
              metadataElements = metadataElements :+ new KPmmlModelElement(appDir + "/metadata/model/" + elem("Filename").toString)
            }
            case "pmml" => {
              if (!elem.keySet.exists(_ == "Filename")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'pmml' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'pmml' requires 'Filename' to be defined.")
              }
              if (!elem.keySet.exists(_ == "ModelName")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'pmml' requires 'ModelName' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'pmml' requires 'ModelName' to be defined.")
              }
              if (!elem.keySet.exists(_ == "MessageConsumed")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'pmml' requires 'MessageConsumed' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'pmml' requires 'MessageConsumed' to be defined.")
              }
              if (elem.keySet.exists(_ == "MessageProduced")) {
                if (elem("MessageProduced") != null && elem("MessageProduced") != "") {
                  metadataElements = metadataElements :+ new PmmlModelElement(appDir + "/metadata/model/" + elem("Filename").toString, elem("ModelName").toString, elem("MessageConsumed").toString, Some(elem("MessageProduced").toString))
                }
                else {
                  metadataElements = metadataElements :+ new PmmlModelElement(appDir + "/metadata/model/" + elem("Filename").toString, elem("ModelName").toString, elem("MessageConsumed").toString, None)
                }
              }
            }
            case "python" => {
              if (!elem.keySet.exists(_ == "Filename")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'python' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'python' requires 'Filename' to be defined.")
              }
              if (!elem.keySet.exists(_ == "ModelName")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'python' requires 'ModelName' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'python' requires 'ModelName' to be defined.")
              }
              if (!elem.keySet.exists(_ == "ModelOptions")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'python' requires 'ModelOptions' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'python' requires 'ModelOptions' to be defined.")
              }
              if (!elem.keySet.exists(_ == "MessageConsumed")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'python' requires 'MessageConsumed' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'python' requires 'MessageConsumed' to be defined.")
              }
              if (elem.keySet.exists(_ == "MessageProduced")) {
                if (elem("MessageProduced") != null && elem("MessageProduced") != "") {
                  metadataElements = metadataElements :+ new PythonModelElement(appDir + "/metadata/model/" + elem("Filename").toString, elem("ModelName").toString, elem("ModelOptions").toString, elem("MessageConsumed").toString, Some(elem("MessageProduced").toString))
                }
                else {
                  metadataElements = metadataElements :+ new PythonModelElement(appDir + "/metadata/model/" + elem("Filename").toString, elem("ModelName").toString, elem("ModelOptions").toString, elem("MessageConsumed").toString, None)
                }
              }
            }
          }
        }
        case "modelconfiguration" => {
          if (!elem.keySet.exists(_ == "Filename")) {
            logger.error("***ERROR*** Metadata Element Type 'ModelConfiguration' requires 'Filename' to be defined.")
            throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'ModelConfiguration' requires 'Filename' to be defined.")
          }
          metadataElements = metadataElements :+ new ModelConfigurationElement(appDir + "/metadata/configuration/" + elem("Filename").toString)
        }
        case "adaptermessagebindings" => {
          if (!elem.keySet.exists(_ == "Filename")) {
            logger.error("***ERROR*** Metadata Element Type 'AdapterMessageBindings' requires 'Filename' to be defined.")
            throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'AdapterMessageBindings' requires 'Filename' to be defined.")
          }
          metadataElements = metadataElements :+ new AdapterMessageBindingElement(appDir + "/metadata/configuration/" + elem("Filename").toString)
        }
        case _ => logger.warn(s"***WARN*** Unknown Metadata Element '${elem("Type")}' found. Ignoring.")
      }
    })
    return metadataElements
  }

  private def parseDataSets(appDir: String, configStr: JValue): List[DataSet] = {
    var dataSets: List[DataSet] = List()
    val dataSetMap: List[Map[String, Any]] = (configStr \\ "DataSets").values.asInstanceOf[List[Map[String, Any]]]
    var inputDataFile: String = null;
    var inputDataFormat: String = null;
    var partitionKey: String = null;
    var resultsDataFile: String = null;
    var resultsDataFormat: String = null;

    def checkFormat(format: String): String = {
      format.toLowerCase match {
        case "csv" | "json" => format
        case _ => {
          logger.error(s"***ERROR*** Format $format is not supported. Supported formats are 'CSV' and 'JSON'.")
          throw new KamanjaApplicationConfigurationException(s"***ERROR*** Format $format is not supported. Supported formats are 'CSV' and 'JSON'.")
        }
      }
    }

    dataSetMap.foreach(dataSetConfig => {
      // Get the input configuration and convert to map or throw exception
      val inputSetConfig = dataSetConfig.getOrElse("Input", {
        logger.error("***ERROR*** DataSet Element Type 'Input' must be defined.")
        throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'Input' must be defined.")
      }).asInstanceOf[Map[String, Any]]

      val inputFilename = inputSetConfig.getOrElse("Filename", {
        logger.error("***ERROR*** DataSet Element Type 'Input' requires 'Filename' to be defined.")
        throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'Input' requires 'Filename' to be defined.")
      }).asInstanceOf[String]

      val inputFormat = checkFormat(inputSetConfig.getOrElse("Format", {
        logger.error("***ERROR*** DataSet Element Type 'Input' requires 'Format' to be defined.")
        throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'Input' requires 'Format' to be defined.")
      }).asInstanceOf[String])

      val inputAdapterName = inputSetConfig.getOrElse("AdapterName", {
        logger.error("***ERROR*** DataSet Element Type 'Input' Requires 'AdapterName' to be defined.")
        throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'Input' Requires 'AdapterName' to be defined.")
      }).asInstanceOf[String]

      var partitionKey = if (inputSetConfig.getOrElse("PartitionKey", null) == null) None else Some(inputSetConfig("PartitionKey").asInstanceOf[String])

      partitionKey match {
        case Some(key) =>
          if (inputFormat.toLowerCase == "csv" && !isNumeric(key)) {
            logger.error(s"***ERROR*** Input Data Format is defined as CSV but the partition key $key is a String. It must be an integer.")
            throw new KamanjaApplicationConfigurationException(s"***ERROR*** Input Data Format is defined as CSV but the partition key ${partitionKey} is a String. It must be an integer.")
          }
          else if (inputFormat.toLowerCase == "json" && isNumeric(key)) {
            logger.error(s"***ERROR*** Input Data Format is defined as JSON but the partition key ${partitionKey} is an Integer. It must be a string in the format 'namespace.message:partitionKey'")
            throw new KamanjaApplicationConfigurationException(s"***ERROR*** Input Data Format is defined as JSON but the partition key ${partitionKey} is an Integer. It must be a string in the format 'namespace.message:partitionKey'")
          }
      }

      val inputSet: InputSet = new InputSet(s"$appDir/data/$inputFilename", inputFormat, inputAdapterName, partitionKey)

      // Get the output configuration and convert to map or throw exception
      val expectedResultsSetConfig = dataSetConfig.getOrElse("ExpectedResults", {
        logger.error("***ERROR*** DataSet Element Type 'ExpectedResults' must be defined.")
        throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'ExpectedResults' must be defined.")
      }).asInstanceOf[Map[String, Any]]

      val expectedResultsFilename = expectedResultsSetConfig.getOrElse("Filename", {
        logger.error("***ERROR*** DataSet Element type 'ExpectedResults' requires 'Filename' to be defined.")
        throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element type 'ExpectedResults' requires 'Filename' to be defined.")
      }).asInstanceOf[String]

      val expectedResultsFormat = checkFormat(expectedResultsSetConfig.getOrElse("Format", {
        logger.error("***ERROR*** DataSet Element Type 'ExpectedResults' requires 'Format' to be defined.")
        throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'ExpectedResults' requires 'Format' to be defined.")
      }).asInstanceOf[String])

      val expectedResultsAdapterName = expectedResultsSetConfig.getOrElse("AdapterName", {
        logger.error("***ERROR*** DataSet Element Type 'ExpectedResults' requires 'AdapterName' to be defined.")
        throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'ExpectedResults' requires 'AdapterName' to be defined.")
      }).asInstanceOf[String]

      val expectedResultsSet: ExpectedResultsSet = new ExpectedResultsSet(s"$appDir/data/$expectedResultsFilename", expectedResultsFormat, expectedResultsAdapterName)

      dataSets :+= new DataSet(inputSet, expectedResultsSet)
    })
    return dataSets
  }

  private def isNumeric(input: String): Boolean = {
    return input.forall(_.isDigit)
  }
}
