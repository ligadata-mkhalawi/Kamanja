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
package com.ligadata.jython.test

import java.io.File
import com.ligadata.kamanja.metadata.{MdMgr, MiningModelType, ModelRepresentation, ModelDef}
import com.ligadata.runtime._
import com.ligadata.jython.GeneratorBuilder
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager
import org.scalatest.{BeforeAndAfter, FunSuite}


/**
  *
  */
class JythonSimpleTest extends FunSuite with BeforeAndAfter {

  val logger = LogManager.getLogger(this.getClass.getName())

  test("test01") {
    val fileInput = getClass.getResource("/JythonSimple/JythonSample.python").getPath
    val fileOutput = getClass.getResource("/JythonSimple/").getPath + "/JythonSample.scala.actual"
    val fileExpected = getClass.getResource("/JythonSimple/JythonSample.scala.expected").getPath
    val metadataLocation = getClass.getResource("/metadata").getPath

    // Create model metadata
    //inMsgsAndAttrsSets.map(s => s._2).toArray
    // out
    var model = new ModelDef(ModelRepresentation.JAR, MiningModelType.JYTHON, null, Array("com.ligadata.kamanja.samples.messages.msg1", "com.ligadata.kamanja.samples.messages.outmsg1"), false, false)

    // Append addtional attributes
    model.nameSpace = "com.ligadata.samples.models"
    model.name = "HelloWorldModel"
    model.description = "HelloWorldModel"
    model.ver = MdMgr.ConvertVersionToLong(MdMgr.FormatVersion("1.0.0"))
    model.physicalName = model.nameSpace + ".V" + model.ver + "." + model.name + "Factory"

    // JythonBuilder
    val generator = GeneratorBuilder.create().
      setSuppressTimestamps().
      setInputFile(fileInput).
      setOutputFile(fileOutput).
      setMetadataLocation(metadataLocation).
      setModelDef(model).
      build()

    generator.Execute()

    val expected = FileUtils.readFileToString(new File(fileExpected))
    val actual = FileUtils.readFileToString(new File(fileOutput))
    logger.info("actual path={}", fileOutput)
    logger.info("expected path={}", fileExpected)

    assert(actual == expected)

    DeleteFile(fileOutput)

  }

}
