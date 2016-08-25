package com.ligadata.test.application

import org.scalatest._

class TestExecutorTests extends FlatSpec with BeforeAndAfterAll {
  val installDir = this.getClass.getResource("/kamanjaInstall").getPath

  override def beforeAll: Unit = {
    EmbeddedServicesManager.startServices(installDir)
  }

  override def afterAll: Unit = {
    EmbeddedServicesManager.stopServices
  }

  //TODO: This will be verified by sight until I can finish end to end operations of the test executor
  "Test Executor" should "take the Kamanja install directory and execute all tests within" in {

    TestExecutor.main(Array("--kamanja-dir", installDir))

    //TODO: Need to produce output so that I may assert correct results
  }

  it should "push data into Kafka given as a DataSet and return an Array of output messages" in {

  }

}
