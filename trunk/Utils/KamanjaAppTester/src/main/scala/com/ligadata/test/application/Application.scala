package com.ligadata.test.application

import com.ligadata.test.application.metadata.interfaces.MetadataElement

case class Application(name: String, metadataElements: List[MetadataElement], inputOutputData: Map[String, String])