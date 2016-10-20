package com.ligadata.test.application.data

import java.io.File

case class DataSet(inputDataFile: String,
                   inputDataFormat: String,
                   expectedResultsFile: String,
                   expectedResultsFormat: String,
                   partitionKey: Option[String])
