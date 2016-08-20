package com.ligadata.test.application.metadata

import com.ligadata.MetadataAPI.MetadataAPI.ModelType
import com.ligadata.test.application.metadata.interfaces.{MetadataElement, ModelElement}

case class ContainerElement(filename: String) extends MetadataElement {
  val elementType = "container"
}

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

case class PmmlModelElement(filename: String, msgConsumed: String, msgProduced: Option[String]) extends ModelElement {
  val elementType = "model"
  val modelType = ModelType.PMML
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