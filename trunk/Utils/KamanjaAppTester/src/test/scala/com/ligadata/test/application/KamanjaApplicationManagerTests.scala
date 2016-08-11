package com.ligadata.test.application

import com.ligadata.test.application.metadata.interfaces.ModelElement
import com.ligadata.test.application.metadata._
import org.scalatest._

class KamanjaApplicationManagerTests extends FlatSpec {
  private val kamanjaAppManager: KamanjaApplicationManager = new KamanjaApplicationManager(getClass.getResource("/KamanjaApplicationTest").getPath)
  private val app1MdElems = kamanjaAppManager.kamanjaApplications(0).metadataElements
  private val app1DataSets = kamanjaAppManager.kamanjaApplications(0).dataSets
  private val app1Dir = kamanjaAppManager.kamanjaApplications(0).applicationDirectory
  private val app1Name = kamanjaAppManager.kamanjaApplications(0).name

  "Kamanja Application Manager" should "read in two applications given the resource directory 'KamanjaApplicationTest'" in {
    assert(kamanjaAppManager.kamanjaApplications.length == 2)
  }

  it should "have it's application configuration properly set" in {
    val testAppDir = getClass.getResource("/KamanjaApplicationTest/TestApp1").getPath
    assert(app1Dir == testAppDir)
    assert(app1Name == "TestApp1")

    // The order elements are stored in memory should be the same as written in configuration

    assert(app1MdElems.length == 6)

    // Verifying adapter message binding configuration
    assert(app1MdElems(0).isInstanceOf[AdapterMessageBindingElement])
    assert(app1MdElems(0).asInstanceOf[AdapterMessageBindingElement].filename == testAppDir + "/metadata/configuration/testApp1AdapterMessageBindings.json")

    // Container
    assert(app1MdElems(1).isInstanceOf[ContainerElement])
    assert(app1MdElems(1).asInstanceOf[ContainerElement].filename == s"$testAppDir/metadata/container/testApp1Container.json")
    assert(app1MdElems(1).asInstanceOf[ContainerElement].tenantId == "tenant1")

    // Input Message
    assert(app1MdElems(2).isInstanceOf[MessageElement])
      assert(app1MdElems(2).asInstanceOf[MessageElement].filename == s"$testAppDir/metadata/message/testApp1Message.json")
    assert(app1MdElems(2).asInstanceOf[MessageElement].tenantId == "tenant1")

    // Output Message
    assert(app1MdElems(3).isInstanceOf[MessageElement])
    assert(app1MdElems(3).asInstanceOf[MessageElement].filename == s"$testAppDir/metadata/message/testApp1OutputMessage.json")
    assert(app1MdElems(3).asInstanceOf[MessageElement].tenantId == "tenant1")

    // Model Configuration
    assert(app1MdElems(4).isInstanceOf[ModelConfigurationElement])
    assert(app1MdElems(4).asInstanceOf[ModelConfigurationElement].filename == s"$testAppDir/metadata/configuration/testApp1ModelConfiguration.json")

    // Model
    assert(app1MdElems(5).isInstanceOf[ModelElement])
    assert(app1MdElems(5).isInstanceOf[ScalaModelElement])
    assert(app1MdElems(5).asInstanceOf[ScalaModelElement].filename == s"$testAppDir/metadata/model/testApp1Model.scala")
    assert(app1MdElems(5).asInstanceOf[ScalaModelElement].tenantId == "tenant1")
    assert(app1MdElems(5).asInstanceOf[ScalaModelElement].modelCfg == "testApp1ModelConfiguration")


    // Verifying Data Sets read in
    assert(app1DataSets.length == 1)
    assert(app1DataSets(0).inputDataFile == s"$testAppDir/data/testApp1InputFile.csv")
    assert(app1DataSets(0).inputDataFormat == "CSV")
    assert(app1DataSets(0).expectedResultsFile == s"$testAppDir/data/testApp1ExpectedResults.csv")
    assert(app1DataSets(0).expectedResultsFormat == "CSV")
  }


}
