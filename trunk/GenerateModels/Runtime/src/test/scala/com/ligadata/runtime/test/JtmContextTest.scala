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
package com.ligadata.runtime.test


import java.io.File

import com.ligadata.runtime._
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.LogManager

import org.scalatest.{BeforeAndAfter, FunSuite}

/**
  *
  */
class JtmContextTest extends FunSuite with BeforeAndAfter {

  val logger = LogManager.getLogger(this.getClass.getName())

  test("test") {
    val context = new JtmContext
    context.SetSection("a")
    context.AddError("aa")
    context.AddError("ab")
    context.SetScope("z")
    context.AddError("za")
    context.AddError("zb")

    val r1 = context.CurrentErrorList()
    val r2 = context.CurrentErrors()
    assert(r1.deep == Array("aa", "ab", "za", "zb").deep)
    assert(r2 == 4)

    val r3 = context.ErrorList("a", "z")
    val r4 = context.Errors("a", "z")
    assert(r3.deep == Array("aa", "ab", "za", "zb").deep)
    assert(r4 == 4)

    val r5 = context.ErrorList("a", "")
    val r6 = context.Errors("a", "")
    assert(r5.deep == Array("aa", "ab").deep)
    assert(r6 == 2)
  }

  test("test 1") {
    val context = new JtmContext
    context.SetSection("a")
    context.AddError("aa")
    context.AddError("ab")
    context.SetScope("z")

    val r1 = context.CurrentErrorList()
    val r2 = context.CurrentErrors()
    assert(r1.deep == Array("aa", "ab").deep)
    assert(r2 == 2)

    val r3 = context.ErrorList("a", "z")
    val r4 = context.Errors("a", "z")
    assert(r3.deep == Array("aa", "ab").deep)
    assert(r4 == 2)

    val r5 = context.ScopeErrorList("z")
    val r6 = context.ScopeErrors("z")
    assert(r5.deep == Array("aa", "ab").deep)
    assert(r6 == 2)

  }

 }
