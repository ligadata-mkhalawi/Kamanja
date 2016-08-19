package com.ligadata.test.application

import org.scalatest._

class EmbeddedServicesManagerTests extends FlatSpec {

  "Embedded Services Manager" should "start zookeeper and kafka" in {
    assert(EmbeddedServicesManager.startServices(getClass.getResource("/kamanjaInstall").getPath))
  }

  it should "stop zookeeper and kafka" in {
    assert(EmbeddedServicesManager.stopServices)
  }

}
