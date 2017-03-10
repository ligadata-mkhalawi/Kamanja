package com.ligadata.kamanja.test.application.metadata

import java.io.{File, FileNotFoundException}

import com.ligadata.MetadataAPI.MetadataAPI.ModelType
import com.ligadata.kamanja.test.application.metadata.interfaces.{MetadataElement, ModelElement}
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source

/**
  * ContainerElement, unlike the other elements, will parse the given file for its own package and name.
  * This is used if a kvFile is given so that we know the proper type we are uploading.
  *
  * @param filename The name of the file containing the container definition
  * @param kvInitOptions Some class that contains the filename and serializer options for kvinit
  */
case class ContainerElement(filename: String, kvInitOptions: Option[KVInitOptions]) extends MetadataElement {
  val elementType: String = "container"
}

case class KVInitOptions(filename: String,
                         ignoreRecords: Option[String] = Some("1"),
                         deserializer: Option[String] = Some("com.ligadata.kamanja.serializer.csvserdeser"),
                         alwaysQuoteFields: Option[Boolean] = Some(false),
                         fieldDelimiter: Option[String] = Some(","),
                         valueDelimiter: Option[String] = Some("~")
                        )

case class MessageElement(filename: String) extends MetadataElement {
  val elementType = "message"
}

case class JavaModelElement(filename: String, modelCfg: String) extends ModelElement {
  val elementType = "model"
  val modelType = ModelType.JAVA
}

case class ScalaModelElement(filename: String,  modelCfg: String) extends ModelElement {
  val elementType = "model"
  val modelType = ModelType.SCALA
}

case class PmmlModelElement(filename: String, modelName: String, msgConsumed: String, msgProduced: Option[String]) extends ModelElement {
  val elementType = "model"
  val modelType = ModelType.PMML
}

case class PythonModelElement(filename: String, modelName: String, modelOptions:String, msgConsumed: String, msgProduced: Option[String]) extends ModelElement {
  val elementType = "model"
  val modelType = ModelType.PYTHON
}

case class KPmmlModelElement(filename: String) extends ModelElement {
  val elementType = "model"
  val modelType = ModelType.KPMML
}

case class ModelConfigurationElement(filename: String) extends MetadataElement {
  val elementType = "compileconfig"
}

case class AdapterMessageBindingElement(filename: String) extends MetadataElement {
  val elementType = "adaptermessagebinding"
}
