package com.ligadata.kamanja.test.application

import org.scalatest._

class TestExecutorTests extends FlatSpec with BeforeAndAfterAll {

  //TODO: This will be verified by sight until I can finish end to end operations of the test executor
  "Test Executor" should "take the Kamanja install directory and execute all tests within" in {

    TestExecutor.main(Array("--kamanja-dir", TestSetup.kamanjaInstallDir))
    //TODO: Need to produce output so that I may assert correct results
  }
}
