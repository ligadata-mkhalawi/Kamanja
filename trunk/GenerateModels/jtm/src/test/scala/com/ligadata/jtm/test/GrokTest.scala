/*
 * Copyright 2016 ligaDATA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ligadata.jtm.test

import java.io.File

import com.ligadata.jtm._
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager

import org.scalatest.{BeforeAndAfter, FunSuite}

/**
  *
  */
class GrokTest extends FunSuite with BeforeAndAfter {

  val logger = LogManager.getLogger(this.getClass.getName())

  // Simple jtm
  test("test1") {

    val fileInput = getClass.getResource("/grok/grok001.jtm").getPath
    val fileOutput = getClass.getResource("/grok").getPath + "/grok001.scala.actual"
    val fileExpected = getClass.getResource("/grok/grok001.scala.expected").getPath
    val metadataLocation = getClass.getResource("/metadata").getPath

    val compiler = CompilerBuilder.create().
      setSuppressTimestamps().
      setInputFile(fileInput).
      setOutputFile(fileOutput).
      setMetadataLocation(metadataLocation).
      build()

    compiler.Execute()

    val expected = FileUtils.readFileToString(new File(fileExpected), null:String)
    val actual = FileUtils.readFileToString(new File(fileOutput), null:String)
    logger.info("actual path={}", fileOutput)
    logger.info("expected path={}", fileExpected)

    assert(actual == expected)

    DeleteFile(fileOutput)
  }

  // Simple jtm
  test("defect4016") {

    val fileInput = getClass.getResource("/grok/defect1416.jtm").getPath
    val fileOutput = getClass.getResource("/grok").getPath + "/defect1416.scala.actual"
    val fileExpected = getClass.getResource("/grok/defect1416.scala.expected").getPath
    val metadataLocation = getClass.getResource("/metadata").getPath

    val compiler = CompilerBuilder.create().
      setSuppressTimestamps().
      setInputFile(fileInput).
      setOutputFile(fileOutput).
      setMetadataLocation(metadataLocation).
      build()

    compiler.Execute()

    val expected = FileUtils.readFileToString(new File(fileExpected), null:String)
    val actual = FileUtils.readFileToString(new File(fileOutput), null:String)
    logger.info("actual path={}", fileOutput)
    logger.info("expected path={}", fileExpected)

    assert(actual == expected)

    DeleteFile(fileOutput)
  }

}
