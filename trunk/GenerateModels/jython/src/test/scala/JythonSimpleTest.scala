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
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfter, FunSuite}


/**
  *
  */
class SimpleJythonTest extends FunSuite with BeforeAndAfter {

  test("test01") {
    val fileInput = getClass.getResource("/JythonSimple/JythonSample.python").getPath

    val actual = "" //getClass.getResource("/JythonSimple/JythonSample.scala.expected").getPath

    // JythonBuilder

    // Generate scala
    val expected = ""

    assert(expected == actual)
  }

}
