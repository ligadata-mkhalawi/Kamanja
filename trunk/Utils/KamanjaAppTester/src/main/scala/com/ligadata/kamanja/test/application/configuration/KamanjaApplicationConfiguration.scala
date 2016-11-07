package com.ligadata.kamanja.test.application.configuration

import java.io.File

import com.ligadata.kamanja.test.application.KamanjaApplication
import com.ligadata.kamanja.test.application.data.DataSet
import com.ligadata.kamanja.test.application.metadata._
import com.ligadata.kamanja.test.application.metadata.interfaces.MetadataElement
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source

class KamanjaApplicationConfiguration {

  def initializeApplication(applicationDirectory: String, applicationConfiguration: String): KamanjaApplication = {
    val config: File = new File(applicationConfiguration)
    if(!config.exists()) {
      throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Configuration File: '" + config + "' does not exist")
    }

    val source = Source.fromFile(config)
    val jsonStr: String = source.getLines().mkString
    source.close()

    val json = parse(jsonStr)

    try {
      val appName = (json \ "Application" \ "Name").values.toString
      println("[Kamanja Application Tester]: Initializing Application '" + appName + "'")
      return new KamanjaApplication(appName, applicationDirectory, parseMdElements(applicationDirectory, json), parseDataSets(applicationDirectory, json))
    }
    catch {
      case e: KamanjaApplicationConfigurationException => {
        println("[Kamanja Application Tester]: ***ERROR*** Failed to initialize Kamanja Application", e)
        throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Failed to initialize Kamanja Application", e)
      }
      case e: Exception => {
        println("[Kamanja Application Tester]: ***ERROR*** Unexpected exception encountered. Failed to initialize Kamanja Application.", e)
        throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Unexpected exception encountered. Failed to initialize Kamanja Application.", e)
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
            println("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Container' requires 'Filename' to be defined.")
            throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Container' requires 'Filename' to be defined.")
          }
          val kvFile: Option[String] = if (elem.keySet.exists(_ == "KVFile")) Some(s"$appDir/data/${elem("KVFile").toString}") else None
          metadataElements = metadataElements :+ new ContainerElement(appDir + "/metadata/container/" + elem("Filename").toString, kvFile)
        }
        case "message" => {
          if (!elem.keySet.exists(_ == "Filename")) {
            println("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Message' requires 'Filename' to be defined.")
            throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Message' requires 'Filename' to be defined.")
          }

          metadataElements = metadataElements :+ new MessageElement(appDir + "/metadata/message/" + elem("Filename").toString)
        }
        case "model" => {
          elem("ModelType").toString.toLowerCase match {
            case "java" =>
              if (!elem.keySet.exists(_ == "Filename")) {
                println("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'Java' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'Java' requires 'Filename' to be defined.")
              }

              if(!elem.keySet.exists(_ == "ModelConfiguration")) {
                println("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'Java' requires 'ModelConfiguration' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'Java' requires 'ModelConfiguration' to be defined.")
              }

              metadataElements = metadataElements :+ new JavaModelElement(appDir + "/metadata/model/" + elem("Filename").toString, elem("ModelConfiguration").toString)
            case "scala" => {
              if (!elem.keySet.exists(_ == "Filename")) {
                println("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'Scala' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'Scala' requires 'Filename' to be defined.")
              }
              if(!elem.keySet.exists(_ == "ModelConfiguration")) {
                println("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'Scala' requires 'ModelConfiguration' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'Scala' requires 'ModelConfiguration' to be defined.")
              }
              metadataElements = metadataElements :+ new ScalaModelElement(appDir + "/metadata/model/" + elem("Filename").toString, elem("ModelConfiguration").toString)
            }
            case "kpmml" => {
              if (!elem.keySet.exists(_ == "Filename")) {
                println("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'KPMML' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'KPMML' requires 'Filename' to be defined.")
              }
              metadataElements = metadataElements :+ new KPmmlModelElement(appDir + "/metadata/model/" + elem("Filename").toString)
            }
            case "pmml" => {
              if (!elem.keySet.exists(_ == "Filename")) {
                println("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'PMML' requires 'Filename' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'PMML' requires 'Filename' to be defined.")
              }
              if(!elem.keySet.exists(_ == "MessageConsumed")) {
                println("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'PMML' requires 'MessageConsumed' to be defined.")
                throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'Model' with ModelType 'PMML' requires 'MessageConsumed' to be defined.")
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
            println("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'ModelConfiguration' requires 'Filename' to be defined.")
            throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'ModelConfiguration' requires 'Filename' to be defined.")
          }
          metadataElements = metadataElements :+ new ModelConfigurationElement(appDir + "/metadata/configuration/" + elem("Filename").toString)
        }
        case "adaptermessagebindings" => {
          if (!elem.keySet.exists(_ == "Filename")) {
            println("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'AdapterMessageBindings' requires 'Filename' to be defined.")
            throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Metadata Element Type 'AdapterMessageBindings' requires 'Filename' to be defined.")
          }
          metadataElements = metadataElements :+ new AdapterMessageBindingElement(appDir + "/metadata/configuration/" + elem("Filename").toString)
        }
        case _ => println(s"[Kamanja Application Tester]: ***WARN*** Unknown Metadata Element '${elem("Type")}' found. Ignoring.")
      }
    })
    return metadataElements
  }

  private def parseDataSets(appDir: String, configStr: JValue): List[DataSet] = {
    var dataSets: List[DataSet] = List()
    val data: List[Map[String, Any]] = (configStr \\ "DataSets").values.asInstanceOf[List[Map[String, Any]]]
    data.foreach(set => {
      if (!set.keySet.exists(_ == "InputDataFile")) {
        println("[Kamanja Application Tester]: ***ERROR*** Data Set requires 'InputDataFile' to be defined.")
        throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Data Set requires 'InputDataFile' to be defined.")
      }
      if (!set.keySet.exists(_ == "InputDataFormat")) {
        println("[Kamanja Application Tester]: ***ERROR*** Data Set requires 'InputDataFormat' to be defined.")
        throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Data Set requires 'InputDataFormat' to be defined.")
      }
      else {
        if(set("InputDataFormat").toString.toLowerCase != "csv" && set("InputDataFormat").toString.toLowerCase != "json"){
          throw new KamanjaApplicationConfigurationException(s"[Kamanja Application Tester]: Invalid InputDataFormat '${set("InputDataFormat")}' found. Accepted formats are CSV and JSON.")
        }
      }
      if (!set.keySet.exists(_ == "ExpectedResultsFile")) {
        println("[Kamanja Application Tester]: ***ERROR*** Data Set requires 'ExpectedResultsFile' to be defined.")
        throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Data Set requires 'ExpectedResultsFile' to be defined.")
      }
      if(!set.keySet.exists(_ == "ExpectedResultsFormat")) {
        println("[Kamanja Application Tester]: ***ERROR*** Data Set requires 'ExpectedResultsFormat' to be defined.")
        throw new KamanjaApplicationConfigurationException("[Kamanja Application Tester]: ***ERROR*** Data Set requires 'ExpectedResultsFormat' to be defined.")
      }

      var partitionKey: Option[String] = None
      if(set.keySet.exists(_ == "PartitionKey")) {
        if(set("InputDataFormat").toString.toLowerCase == "csv" && !isNumeric(set("PartitionKey").toString))
          throw new KamanjaApplicationConfigurationException(s"[Kamanja Application Tester]: ***ERROR*** Input Data Format is defined as CSV but the partition key ${set("PartitionKey").toString} is a String. It must be an integer.")
        else if(set("InputDataFormat").toString.toLowerCase == "json" && isNumeric(set("PartitionKey").toString))
          throw new KamanjaApplicationConfigurationException(s"[Kamanja Application Tester]: ***ERROR*** Input Data Format is defined as JSON but the partition key ${set("PartitionKey").toString} is an Integer. It must be a string in the format 'namespace.message:partitionKey'")
        else
          partitionKey = Some(set("PartitionKey").toString)
      }

      dataSets = dataSets :+ new DataSet(appDir + "/data/" + set("InputDataFile").toString, set("InputDataFormat").toString, appDir + "/data/" + set("ExpectedResultsFile").toString, set("ExpectedResultsFormat").toString, partitionKey)
    })
    return dataSets
  }

  private def isNumeric(input: String): Boolean = {
    return input.forall(_.isDigit)
  }
}
