package com.ligadata.test.application

import com.ligadata.test.application.data.DataSet
import com.ligadata.test.application.metadata.interfaces.MetadataElement

case class KamanjaApplication(name: String, applicationDirectory: String, metadataElements: List[MetadataElement], dataSets: List[DataSet])