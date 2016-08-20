package com.ligadata.test.application.metadata.interfaces

trait MetadataElement {
  def elementType: String
  def filename: String
}

/*
case class MetadataElement(mdType: String,
                           filename: String,
                           tenantId: Option[String],
                           modelType: Option[String],
                           messageConsumed: Option[String],
                           messageProduced: Option[String]
                          )
*/