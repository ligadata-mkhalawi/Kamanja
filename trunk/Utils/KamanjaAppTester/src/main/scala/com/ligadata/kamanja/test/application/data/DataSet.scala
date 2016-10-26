package com.ligadata.kamanja.test.application.data

case class DataSet(inputDataFile: String,
                   inputDataFormat: String,
                   expectedResultsFile: String,
                   expectedResultsFormat: String,
                   partitionKey: Option[String])
