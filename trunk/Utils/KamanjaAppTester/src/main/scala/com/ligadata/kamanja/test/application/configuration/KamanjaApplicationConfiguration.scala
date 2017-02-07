package com.ligadata.kamanja.test.application.configuration

import java.io.File

import com.ligadata.kamanja.test.application.KamanjaApplication
import com.ligadata.kamanja.test.application.data.DataSet
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
    if(!config.exists()) {
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

              if(!elem.keySet.exists(_ == "ModelConfiguration")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'Java' requires 'ModelConfiguration' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'Java' requires 'ModelConfiguration' to be defined.")
              }

              metadataElements = metadataElements :+ new JavaModelElement(appDir + "/metadata/model/" + elem("Filename").toString, elem("ModelConfiguration").toString)
            case "scala" => {
              if (!elem.keySet.exists(_ == "Filename")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'Scala' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'Scala' requires 'Filename' to be defined.")
              }
              if(!elem.keySet.exists(_ == "ModelConfiguration")) {
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
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'PMML' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'PMML' requires 'Filename' to be defined.")
              }
              if(!elem.keySet.exists(_ == "MessageConsumed")) {
                logger.error("***ERROR*** Metadata Element Type 'Model' with ModelType 'PMML' requires 'MessageConsumed' to be defined.")
                throw new KamanjaApplicationConfigurationException("***ERROR*** Metadata Element Type 'Model' with ModelType 'PMML' requires 'MessageConsumed' to be defined.")
              }
              if (elem.keySet.exists(_ == "MessageProduced")) {
                if (elem("MessageProduced") != null && elem("MessageProduced") != "") {
                  metadataElements = metadataElements :+ new PmmlModelElement(appDir + "/metadata/model/" + elem("Filename").toString, elem("MessageConsumed").toString, Some(elem("MessageProduced").toString))
                }
                else {
                  metadataElements = metadataElements :+ new PmmlModelElement(appDir + "/metadata/model/" + elem("Filename").toString, elem("MessageConsumed").toString, None)
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
    var inputDataFile:String = null;
    var inputDataFormat:String = null;
    var partitionKey:String = null;
    var resultsDataFile:String = null;
    var resultsDataFormat:String = null;

    dataSetMap.foreach(dataSet => {
      val inputElem = dataSet.getOrElse("Input",null)
      if ( inputElem != null ){
	val inputFileProperties: Map[String,Any] = inputElem.asInstanceOf[scala.collection.immutable.Map[String, Any]]
	val fn = inputFileProperties.getOrElse("Filename",null)
	if( fn == null ){
	  logger.error("***ERROR*** DataSet Element Type 'Input' requires 'Filename' to be defined.")
	  throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'Input' requires 'Filename' to be defined.")
        }
	else{
	  inputDataFile = fn.asInstanceOf[String]
	}
	
	val fmt = inputFileProperties.getOrElse("Format",null)
	if( fmt == null ){
	  logger.error("***ERROR*** DataSet Element Type 'Input' requires 'Format' to be defined.")
	  throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'Input' requires 'Format' to be defined.")
        }
	else{
	  inputDataFormat = fmt.asInstanceOf[String]
	}

	val pkey = inputFileProperties.getOrElse("PartitionKey",null)
	if( pkey == null ){
	  logger.error("***ERROR*** DataSet Element Type 'Input' requires 'PartitionKey' to be defined.")
	  throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'Input' requires 'PartitionKey' to be defined.")
        }
	else{
	  partitionKey = pkey.asInstanceOf[String]
	}
      }
      else{
	  logger.error("***ERROR*** DataSet Element Type 'Input' must be defined.")
	  throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'Input' must be defined.")
      }

      val resultsElem = dataSet.getOrElse("ExpectedResults",null)
      if ( resultsElem != null ){
	val resultsFileProperties: Map[String,Any] = resultsElem.asInstanceOf[scala.collection.immutable.Map[String, Any]]
	val fn = resultsFileProperties.getOrElse("Filename",null)
	if( fn == null ){
	  logger.error("***ERROR*** DataSet Element Type 'ExpectedResults' requires 'Filename' to be defined.")
	  throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'ExpectedResults' requires 'Filename' to be defined.")
        }
	else{
	  resultsDataFile = fn.asInstanceOf[String]
	}
	val fmt = resultsFileProperties.getOrElse("Format",null)
	if( fmt == null ){
	  logger.error("***ERROR*** DataSet Element Type 'ExpectedResults' requires 'Format' to be defined.")
	  throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'ExpectedResults' requires 'Format' to be defined.")
        }
	else{
	  resultsDataFormat = fmt.asInstanceOf[String]
	}
      }
      else{
	  logger.error("***ERROR*** DataSet Element Type 'ExpectedResults' must be defined.")
	  throw new KamanjaApplicationConfigurationException("***ERROR*** DataSet Element Type 'ExpectedResults' must be defined.")
      }

      if( !inputDataFormat.equalsIgnoreCase("csv") && ! inputDataFormat.equalsIgnoreCase("json") ) {
         throw new KamanjaApplicationConfigurationException(s"Invalid InputDataFormat '${inputDataFormat}' found. Accepted formats are CSV and JSON.")
      }

      if( ! resultsDataFormat.equalsIgnoreCase("csv") && ! resultsDataFormat.equalsIgnoreCase("json") ) {
         throw new KamanjaApplicationConfigurationException(s"Invalid Format '${resultsDataFormat}' in ExpectedResults found. Accepted formats are CSV and JSON.")
      }

      var partKey: Option[String] = None
      if(inputDataFormat.equalsIgnoreCase("csv") && !isNumeric(partitionKey) ){
          throw new KamanjaApplicationConfigurationException(s"***ERROR*** Input Data Format is defined as CSV but the partition key ${partitionKey} is a String. It must be an integer.")
      }
      else if(inputDataFormat.equalsIgnoreCase("json") && isNumeric(partitionKey) ) {
          throw new KamanjaApplicationConfigurationException(s"***ERROR*** Input Data Format is defined as JSON but the partition key ${partitionKey} is an Integer. It must be a string in the format 'namespace.message:partitionKey'")
      }
      else {
        partKey = Some(partitionKey)
      }
      dataSets = dataSets :+ new DataSet(appDir + "/data/" + inputDataFile, inputDataFormat, appDir + "/data/" + resultsDataFile, resultsDataFormat, partKey)
    })
    return dataSets
  }

  private def isNumeric(input: String): Boolean = {
    return input.forall(_.isDigit)
  }
}
