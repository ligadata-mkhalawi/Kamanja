package com.ligadata.test.application.metadata.interfaces

import com.ligadata.MetadataAPI.MetadataAPI.ModelType
import com.ligadata.MetadataAPI.MetadataAPI.ModelType.ModelType

trait ModelElement extends MetadataElement {
  def modelType: ModelType
}