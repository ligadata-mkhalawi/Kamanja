package com.ligadata.kamanja.test.application

import com.ligadata.kamanja.test.application.KamanjaApplicationManager
import com.ligadata.kamanja.test.application.logging.KamanjaAppLogger
import com.ligadata.kamanja.test.application.metadata._
import com.ligadata.kamanja.test.application.metadata.interfaces.ModelElement
import org.scalatest._

class KamanjaApplicationManagerTests extends FlatSpec {
  val installDir: String = getClass.getResource("/kamanjaInstall").getPath
  KamanjaAppLogger.createKamanjaAppLogger(installDir)
  private val kamanjaAppManager: KamanjaApplicationManager = new KamanjaApplicationManager(getClass.getResource("/kamanjaInstall/test").getPath)
  private val app1MdElems = kamanjaAppManager.kamanjaApplications(0).metadataElements
  private val app1DataSets = kamanjaAppManager.kamanjaApplications(0).dataSets
  private val app1Dir = kamanjaAppManager.kamanjaApplications(0).applicationDirectory
  private val app1Name = kamanjaAppManager.kamanjaApplications(0).name

  "Kamanja Application Manager" should "read in one application given the resource directory '/kamanjaInstall/test'" in {
    assert(kamanjaAppManager.kamanjaApplications.length == 1)
  }

  it should "have it's application configuration properly set" in {
    val testAppDir = getClass.getResource("/kamanjaInstall/test/TestApp1").getPath
    assert(app1Dir == testAppDir)
    assert(app1Name == "TestApp1")

    // The order elements are stored in memory should be the same as written in configuration

    assert(app1MdElems.length == 6)

    // Container
    assert(app1MdElems(0).isInstanceOf[ContainerElement])
    assert(app1MdElems(0).asInstanceOf[ContainerElement].filename == s"$testAppDir/metadata/container/testApp1Container.json")

    // Input Message
    assert(app1MdElems(1).isInstanceOf[MessageElement])
    assert(app1MdElems(1).asInstanceOf[MessageElement].filename == s"$testAppDir/metadata/message/testApp1Message.json")

    // Output Message
    assert(app1MdElems(2).isInstanceOf[MessageElement])
    assert(app1MdElems(2).asInstanceOf[MessageElement].filename == s"$testAppDir/metadata/message/testApp1OutputMessage.json")

    // Model Configuration
    assert(app1MdElems(3).isInstanceOf[ModelConfigurationElement])
    assert(app1MdElems(3).asInstanceOf[ModelConfigurationElement].filename == s"$testAppDir/metadata/configuration/testApp1ModelConfiguration.json")

    // Model
    assert(app1MdElems(4).isInstanceOf[ModelElement])
    assert(app1MdElems(4).isInstanceOf[ScalaModelElement])
    assert(app1MdElems(4).asInstanceOf[ScalaModelElement].filename == s"$testAppDir/metadata/model/testApp1Model.scala")
    assert(app1MdElems(4).asInstanceOf[ScalaModelElement].modelCfg == "TestApp1ModelConfiguration")

    // Verifying adapter message binding configuration
    assert(app1MdElems(5).isInstanceOf[AdapterMessageBindingElement])
    assert(app1MdElems(5).asInstanceOf[AdapterMessageBindingElement].filename == testAppDir + "/metadata/configuration/testApp1AdapterMessageBindings.json")

    // Verifying Data Sets read in
    assert(app1DataSets.length == 1)
    assert(app1DataSets(0).inputSet.file == s"$testAppDir/data/testApp1InputFile.csv")
    assert(app1DataSets(0).inputSet.format == "CSV")
    assert(app1DataSets(0).inputSet.adapterName == "TestIn_1")
    assert(app1DataSets(0).expectedResultsSet.file == s"$testAppDir/data/testApp1ExpectedResults.csv")
    assert(app1DataSets(0).expectedResultsSet.format == "CSV")
    assert(app1DataSets(0).expectedResultsSet.adapterName == "TestOut_1")
  }


}
