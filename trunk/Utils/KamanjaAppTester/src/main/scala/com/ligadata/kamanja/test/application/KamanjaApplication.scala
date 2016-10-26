package com.ligadata.kamanja.test.application

import com.ligadata.kamanja.test.application.data.DataSet
import com.ligadata.kamanja.test.application.metadata.interfaces.MetadataElement

case class KamanjaApplication(name: String, applicationDirectory: String, metadataElements: List[MetadataElement], dataSets: List[DataSet])