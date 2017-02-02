package com.ligadata.kamanja.test.application

import com.ligadata.kamanja.test.application.TestExecutor
import org.scalatest._

import scala.io.Source

class TestExecutorTests extends FlatSpec with BeforeAndAfterAll {

  //TODO: This will be verified by sight until I can finish end to end operations of the test executor
  "Test Executor" should "take the Kamanja install directory and execute all tests within" in {

    //TestExecutor.main(Array("--kamanja-dir", TestSetup.kamanjaInstallDir))
    TestExecutor.main(Array("--SOMETHING", "else"))

    //TODO: Need to produce output so that I may assert correct results
  }
}