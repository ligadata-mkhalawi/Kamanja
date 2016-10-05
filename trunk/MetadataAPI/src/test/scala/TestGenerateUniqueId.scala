/*
 * Copyright 2015 ligaDATA
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

package com.ligadata.automation.unittests.api
import org.scalatest._
import java.util.concurrent._
import java.util.concurrent.atomic._
import org.apache.logging.log4j._

object TestGenerateUniqueId  {
  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)

  var at:AtomicInteger = new AtomicInteger(0)
  var uniqueIntList: scala.collection.mutable.Set[Int] = scala.collection.mutable.Set[Int]()

  def generateUniqueNumbers(n:Int) = {
    var i = 0
    for(i <- 1 to n ){
      var myCounter = at.incrementAndGet
      logger.info("myCounter => " + myCounter)
      uniqueIntList.add(myCounter)
    }
  }
}

// To execute this test in parallel, set the following in MetadataAPI/build.sbt 
// parallelExecution in Test := true (By default this is turned off)

class TestGenerateUniqueId extends FunSuite with  ParallelTestExecution {
  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)

  test("thread1") {
    TestGenerateUniqueId.generateUniqueNumbers(10)
  }

  test("thread2") {
    TestGenerateUniqueId.generateUniqueNumbers(10)
  }

  test("thread3") {
    TestGenerateUniqueId.generateUniqueNumbers(10)
  }

  test("thread4") {
    TestGenerateUniqueId.generateUniqueNumbers(10)
  }

  test("thread5") {
    TestGenerateUniqueId.generateUniqueNumbers(10)
  }

  test("thread6") {
    TestGenerateUniqueId.generateUniqueNumbers(10)
  }

  test("thread7") {
    TestGenerateUniqueId.generateUniqueNumbers(10)
  }

  test("thread8") {
    TestGenerateUniqueId.generateUniqueNumbers(10)
  }

  test("thread9") {
    TestGenerateUniqueId.generateUniqueNumbers(10)
  }

  test("thread10") {
    TestGenerateUniqueId.generateUniqueNumbers(10)
  }

}
