package com.ligadata.kamanja.test.application.configuration

import com.ligadata.kamanja.test.application.EmbeddedServicesManager
import com.ligadata.test.utils.TestUtils
import org.scalatest._

class EmbeddedServicesManagerTests extends FlatSpec with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestUtils.deleteFile(EmbeddedServicesManager.storageDir)
  }

  "Embedded Services Manager" should "start zookeeper and kafka" in {
    EmbeddedServicesManager.init(TestSetup.kamanjaInstallDir)
    assert(EmbeddedServicesManager.startServices)
  }

  it should "stop zookeeper and kafka" in {
    assert(EmbeddedServicesManager.stopServices)
  }

}
