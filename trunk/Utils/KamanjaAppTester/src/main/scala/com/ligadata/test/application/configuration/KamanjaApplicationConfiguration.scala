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
        case "container" =>
          metadataElements = metadataElements :+ new ContainerElement(elem("Filename").toString, elem("Tenant").toString)
        case "message" =>
          metadataElements = metadataElements :+ new MessageElement(elem("Filename").toString, elem("Tenant").toString)
        case "model" =>
          elem("ModelType").toString.toLowerCase match {
            case "java" =>
              metadataElements = metadataElements :+ new JavaModelElement(elem("Filename").toString, elem("Tenant").toString, elem("ModelConfiguration").toString)
            case "scala" =>
              metadataElements = metadataElements :+ new ScalaModelElement(elem("Filename").toString, elem("Tenant").toString, elem("ModelConfiguration").toString)
            case "kpmml" =>
              metadataElements = metadataElements :+ new KPmmlModelElement(elem("Filename").toString, elem("Tenant").toString)
            case "pmml" =>
              if(elem.keySet.exists(_ == "MessageProduced")) {
                if (elem("MessageProduced") != null && elem("MessageProduced") != "") {
                  metadataElements = metadataElements :+ new PmmlModelElement(elem("Filename").toString, elem("Tenant").toString, elem("MessageConsumed").toString, Some(elem("MessageProduced").toString))
                }
                else {
                  metadataElements = metadataElements :+ new PmmlModelElement(elem("Filename").toString, elem("Tenant").toString, elem("MessageConsumed").toString, None)
                }
              }
          }
        case "modelconfiguration" =>
          metadataElements = metadataElements :+ new ModelConfigurationElement(elem("Filename").toString)
        case "adaptermessagebindingelement" => {
          metadataElements = metadataElements :+ new AdapterMessageBindingElement(elem("Filename").toString)
        }
        case _ => println("Unknown Metadata Type: " + elem("Type"))
      }
    })
    return metadataElements
  }

  private def parseDataSets: List[DataSet] = {
    var dataSets: List[DataSet] = List()
    val data: List[Map[String, Any]] = (json \\ "DataSets").values.asInstanceOf[List[Map[String, Any]]]
    data.foreach(set => {
      dataSets = dataSets :+ new DataSet(set("InputDataFile").toString, set("InputDataFormat").toString, set("ExpectedResultsFile").toString, set("ExpectedResultsFormat").toString)
    })
    return dataSets
  }

  def initializeApplication: KamanjaApplication = {
    return new KamanjaApplication((json \ "Application" \ "Name").values.toString, parseMdElements, parseDataSets)
  }
}
