package com.ligadata.test.application.data

case class DataSet(inputDataFile: String, inputDataFormat: String, expectedResultsFile: String, expectedResultsFormat: String, partitionKey: Option[String])
