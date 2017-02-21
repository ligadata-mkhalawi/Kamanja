package com.ligadata.kamanja.test.application.metadata.interfaces

trait MetadataElement {
  def elementType: String
  def filename: String
  var name: String = ""
  var namespace: String = ""
  var version: String = ""
}