package com.ligadata.test.application.configuration

import com.ligadata.test.utils.KamanjaTestLogger
import com.ligadata.test.application.KamanjaApplication
import java.io.File

import com.ligadata.test.application.data.DataSet
import com.ligadata.test.application.metadata._
import com.ligadata.test.application.metadata.interfaces.MetadataElement
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.io.Source

class KamanjaApplicationConfiguration(configFile: String) extends KamanjaTestLogger {

  private val config: File = new File(configFile)
  if(!config.exists()) {
    throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Configuration File: '" + configFile + "' does not exist")
  }

  private val source = Source.fromFile(config)
  private val jsonStr: String = source.getLines().mkString
  source.close()

  private val json = parse(jsonStr)
  logger.debug(s"Config JSON:\n${pretty(render(json))}")

  parseMdElements
  parseDataSets

  private def parseMdElements: List[MetadataElement] = {
    var metadataElements: List[MetadataElement] = List()
    val mdElems: List[Map[String, Any]] = (json \\ "MetadataElements").values.asInstanceOf[List[Map[String, Any]]]
    mdElems.foreach(elem => {
      elem("Type").toString.toLowerCase match {
        case "container" => {
          if (!elem.keySet.exists(_ == "Filename")) {
            logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Container' requires 'Filename' to be defined.")
            throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Container' requires 'Filename' to be defined.")
          }

          if (!elem.keySet.exists(_ == "Tenant")) {
            logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Container' requires 'Tenant' to be defined.")
            throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Container' requires 'Tenant' to be defined.")
          }

          metadataElements = metadataElements :+ new ContainerElement(elem("Filename").toString, elem("Tenant").toString)
        }
        case "message" => {
          if (!elem.keySet.exists(_ == "Filename")) {
            logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Message' requires 'Filename' to be defined.")
            throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Message' requires 'Filename' to be defined.")
          }

          if (!elem.keySet.exists(_ == "Tenant")) {
            logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Message' requires 'Tenant' to be defined.")
            throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Message' requires 'Tenant' to be defined.")
          }

          metadataElements = metadataElements :+ new MessageElement(elem("Filename").toString, elem("Tenant").toString)
        }
        case "model" => {
          elem("ModelType").toString.toLowerCase match {
            case "java" =>
              if (!elem.keySet.exists(_ == "Filename")) {
                logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'Java' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'Java' requires 'Filename' to be defined.")
              }

              if (!elem.keySet.exists(_ == "Tenant")) {
                logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'Java' requires 'Tenant' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'Java' requires 'Tenant' to be defined.")
              }

              if(!elem.keySet.exists(_ == "ModelConfiguration")) {
                logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'Java' requires 'ModelConfiguration' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'Java' requires 'ModelConfiguration' to be defined.")
              }

              metadataElements = metadataElements :+ new JavaModelElement(elem("Filename").toString, elem("Tenant").toString, elem("ModelConfiguration").toString)
            case "scala" => {
              if (!elem.keySet.exists(_ == "Filename")) {
                logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'Scala' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'Scala' requires 'Filename' to be defined.")
              }
              if (!elem.keySet.exists(_ == "Tenant")) {
                logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'Scala' requires 'Tenant' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'Scala' requires 'Tenant' to be defined.")
              }
              if(!elem.keySet.exists(_ == "ModelConfiguration")) {
                logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'Scala' requires 'ModelConfiguration' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'Scala' requires 'ModelConfiguration' to be defined.")
              }
              metadataElements = metadataElements :+ new ScalaModelElement(elem("Filename").toString, elem("Tenant").toString, elem("ModelConfiguration").toString)
            }
            case "kpmml" => {
              if (!elem.keySet.exists(_ == "Filename")) {
                logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'KPMML' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'KPMML' requires 'Filename' to be defined.")
              }
              if (!elem.keySet.exists(_ == "Tenant")) {
                logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'KPMML' requires 'Tenant' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'KPMML' requires 'Tenant' to be defined.")
              }
              metadataElements = metadataElements :+ new KPmmlModelElement(elem("Filename").toString, elem("Tenant").toString)
            }
            case "pmml" => {
              if (!elem.keySet.exists(_ == "Filename")) {
                logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'PMML' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'PMML' requires 'Filename' to be defined.")
              }
              if (!elem.keySet.exists(_ == "Tenant")) {
                logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'PMML' requires 'Tenant' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'PMML' requires 'Tenant' to be defined.")
              }
              if(!elem.keySet.exists(_ == "MessageConsumed")) {
                logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'PMML' requires 'MessageConsumed' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'Model' with ModelType 'PMML' requires 'MessageConsumed' to be defined.")
              }
              if (elem.keySet.exists(_ == "MessageProduced")) {
                if (elem("MessageProduced") != null && elem("MessageProduced") != "") {
                  metadataElements = metadataElements :+ new PmmlModelElement(elem("Filename").toString, elem("Tenant").toString, elem("MessageConsumed").toString, Some(elem("MessageProduced").toString))
                }
                else {
                  metadataElements = metadataElements :+ new PmmlModelElement(elem("Filename").toString, elem("Tenant").toString, elem("MessageConsumed").toString, None)
                }
              }
            }
          }
        }
        case "modelconfiguration" => {
          if (!elem.keySet.exists(_ == "Filename")) {
            logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'ModelConfiguration' requires 'Filename' to be defined.")
            throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'ModelConfiguration' requires 'Filename' to be defined.")
          }
          metadataElements = metadataElements :+ new ModelConfigurationElement(elem("Filename").toString)
        }
        case "adaptermessagebindings" => {
          if (!elem.keySet.exists(_ == "Filename")) {
            logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'AdapterMessageBindings' requires 'Filename' to be defined.")
            throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Metadata Element Type 'AdapterMessageBindings' requires 'Filename' to be defined.")
          }
          metadataElements = metadataElements :+ new AdapterMessageBindingElement(elem("Filename").toString)
        }
        case _ => logger.warn(s"[Kamanja Application Tester - ApplicationConfiguration]: Unknown Metadata Element '${elem("Type")}' found. Ignoring.")
      }
    })
    return metadataElements
  }

  private def parseDataSets: List[DataSet] = {
    var dataSets: List[DataSet] = List()
    val data: List[Map[String, Any]] = (json \\ "DataSets").values.asInstanceOf[List[Map[String, Any]]]
    data.foreach(set => {
      if (!set.keySet.exists(_ == "InputDataFile")) {
        logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Data Set requires 'InputDataFile' to be defined.")
        throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Data Set requires 'InputDataFile' to be defined.")
      }
      if (!set.keySet.exists(_ == "InputDataFormat")) {
        logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Data Set requires 'InputDataFormat' to be defined.")
        throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Data Set requires 'InputDataFormat' to be defined.")
      }
      if (!set.keySet.exists(_ == "ExpectedResultsFile")) {
        logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Data Set requires 'ExpectedResultsFile' to be defined.")
        throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Data Set requires 'ExpectedResultsFile' to be defined.")
      }
      if(!set.keySet.exists(_ == "ExpectedResultsFormat")) {
        logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Data Set requires 'ExpectedResultsFormat' to be defined.")
        throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Data Set requires 'ExpectedResultsFormat' to be defined.")
      }
      dataSets = dataSets :+ new DataSet(set("InputDataFile").toString, set("InputDataFormat").toString, set("ExpectedResultsFile").toString, set("ExpectedResultsFormat").toString)
    })
    return dataSets
  }

  def initializeApplication: KamanjaApplication = {
    try {
      val appName = (json \ "Application" \ "Name").values.toString
      logger.info("[Kamanja Application Tester - ApplicationConfiguration]: Initializing Application '" + appName + "'")
      return new KamanjaApplication((json \ "Application" \ "Name").values.toString, parseMdElements, parseDataSets)
    }
    catch {
      case e: KamanjaApplicationConfigurationException => {
        logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Failed to initialize Kamanja Application", e)
        throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Failed to initialize Kamanja Application", e)
      }
      case e: Exception => {
        logger.error("[Kamanja Application Tester - ApplicationConfiguration]: Unexpected exception encountered. Failed to initialize Kamanja Application.", e)
        throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester - ApplicationConfiguration]: Unexpected exception encountered. Failed to initialize Kamanja Application.", e)
      }
    }
  }
}
