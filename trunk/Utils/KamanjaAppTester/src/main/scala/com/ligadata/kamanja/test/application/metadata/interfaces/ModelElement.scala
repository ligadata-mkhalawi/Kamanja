package com.ligadata.kamanja.test.application.metadata.interfaces

import com.ligadata.MetadataAPI.MetadataAPI.ModelType.ModelType

trait ModelElement extends MetadataElement {
  def modelType: ModelType
}