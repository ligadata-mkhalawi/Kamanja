package com.ligadata.kamanja.test.application.data

case class InputSet(file: String, format: String, adapterName: String, fileAdapterDir: Option[String], partitionKey: Option[String])

case class ExpectedResultsSet(file: String, format: String, adapterName: String, fileAdapterDir: Option[String])

case class DataSet(inputSet: InputSet, expectedResultsSet: ExpectedResultsSet)
