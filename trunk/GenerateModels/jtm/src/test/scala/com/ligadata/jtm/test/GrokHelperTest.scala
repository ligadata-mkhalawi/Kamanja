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
import com.ligadata.jtm.eval.GrokHelper
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager

import org.scalatest.{BeforeAndAfter, FunSuite}

/**
  *
  */
class GrokHelperTest extends FunSuite with BeforeAndAfter {

  test("test1") {

    val actual = GrokHelper.ExtractDictionaryKeys("%{EMAIL: email}")
    assert(actual == Set("email"))
  }

  test("test2") {

    val actual = GrokHelper.ExtractDictionaryKeys("%{EMAIL: email}%{EMAIL: email1}")
    assert(actual == Set("email", "email1"))
  }

  test("test3") {

    val actual = GrokHelper.ExtractDictionaryKeys("%{ EMAIL : email} %{EMAIL: email1}")
    assert(actual == Set("email", "email1"))
  }

  test("test4") {

    val actual = GrokHelper.ConvertToGrokPattern("%{EMAIL:email} %{EMAIL:email1}")
    assert(actual == "%{EMAIL:email} %{EMAIL:email1}")
  }

  test("defect1416") {

    val actual = GrokHelper.ConvertToGrokPattern("{USER:auth} \\[%{HTTPDATE:timestamp}\\]")
    assert(actual == "{USER:auth} \\[%{HTTPDATE:timestamp}\\]")
  }
}
