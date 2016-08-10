package com.ligadata.test.application.metadata

import com.ligadata.test.application.metadata.interfaces.{MetadataElement, ModelElement}

case class ContainerElement(filename: String, tenantId: String) extends MetadataElement

case class MessageElement(filename: String, tenantId: String) extends MetadataElement

case class JavaModelElement(filename: String, tenantId: String, modelCfg: String) extends ModelElement

case class ScalaModelElement(filename: String, tenantId: String,  modelCfg: String) extends ModelElement

case class PmmlModelElement(filename: String, tenantId: String, msgConsumed: String, msgProduced: Option[String]) extends ModelElement

case class KPmmlModelElement(filename: String, tenantId: String) extends ModelElement

case class ModelConfigurationElement(filename: String) extends MetadataElement

case class AdapterMessageBindingElement(filename: String) extends MetadataElement