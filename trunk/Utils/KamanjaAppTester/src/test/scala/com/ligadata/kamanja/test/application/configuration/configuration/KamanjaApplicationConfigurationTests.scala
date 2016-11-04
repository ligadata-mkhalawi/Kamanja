package com.ligadata.kamanja.test.application.configuration.configuration

import com.ligadata.kamanja.test.application.KamanjaApplication
import com.ligadata.kamanja.test.application.configuration.KamanjaApplicationConfiguration
import com.ligadata.kamanja.test.application.metadata._
import org.scalatest._

class KamanjaApplicationConfigurationTests extends FlatSpec with BeforeAndAfterAll {
  var configFile: String = ""
  var appConfig: KamanjaApplicationConfiguration = _
  var app: KamanjaApplication = _
  val testAppDir: String = getClass.getResource("/kamanjaInstall/test/TestApp1").getPath

  override def beforeAll {
    configFile = getClass.getResource("/KamanjaApplicationConfigurationTest/TestConfig.json").getPath
    appConfig = new KamanjaApplicationConfiguration
    app = appConfig.initializeApplication(testAppDir, configFile)
  }

  "ApplicationConfiguration" should "read in a configuration file and generate a List of MetadataElement" in {
    assert(app.metadataElements.nonEmpty)

    app.metadataElements.foreach {
      case e: ContainerElement =>
        assert(e.filename == s"$testAppDir/metadata/container/testApp1Container.json")
        assert(e.name == "com.ligadata.kamanja.test.containers.TestApp1Container")
      case e: MessageElement =>
        val inputMsg = s"$testAppDir/metadata/message/inputMessage.json"
        val outputMsg = s"$testAppDir/metadata/message/outputMessage.json"
        e.filename match {
          case `inputMsg` =>
          case `outputMsg` =>
          case _ => fail(s"Unrecognized filename: " + e.filename)
        }
      case e: JavaModelElement =>
        assert(e.filename == s"$testAppDir/metadata/model/model.java")
        assert(e.modelCfg == "modelCfg")
      case e: ScalaModelElement =>
        assert(e.filename == s"$testAppDir/metadata/model/model.scala")
        assert(e.modelCfg == "modelCfg")
      case e: KPmmlModelElement =>
        assert(e.filename == s"$testAppDir/metadata/model/kpmmlModel.xml")
      case e: PmmlModelElement =>
        if (e.filename == s"$testAppDir/metadata/model/pmmlModel.xml") {
          assert(e.msgConsumed == "com.ligadata.test.message.InputMessage")
          assert(e.msgProduced == Some("com.ligadata.test.message.OutputMessage"))
        }
        else if (e.filename == s"$testAppDir/metadata/model/pmmlModel2.xml") {
          assert(e.msgConsumed == "com.ligadata.test.message.InputMessage")
          assert(e.msgProduced.isEmpty)
        }
        else fail("Unexpected filename: " + e.filename)
      case e: ModelConfigurationElement =>
        assert(e.filename == s"$testAppDir/metadata/configuration/modelCfg.json")
      case e: AdapterMessageBindingElement =>
        assert(e.filename == s"$testAppDir/metadata/configuration/adapterMsgBindings.json")
    }
  }

  it should "read in a configuration file and generate a List of DataSets" in {
    assert(app.dataSets.nonEmpty)

    val ds1 = app.dataSets.head
    val ds2 = app.dataSets(1)

    assert(ds1.inputDataFile == s"$testAppDir/data/inputFile1.csv")
    assert(ds1.inputDataFormat == "CSV")
    assert(ds1.expectedResultsFile == s"$testAppDir/data/expectedResults1.csv")
    assert(ds1.expectedResultsFormat == "CSV")
    assert(ds1.partitionKey == Some("3"))

    assert(ds2.inputDataFile == s"$testAppDir/data/inputFile2.json")
    assert(ds2.inputDataFormat == "JSON")
    assert(ds2.expectedResultsFile == s"$testAppDir/data/expectedResults2.json")
    assert(ds2.expectedResultsFormat == "JSON")
    assert(ds2.partitionKey == Some("com.ligadata.test.message.InputMessage:TestKey"))
  }

  it should "read in a configuration file and produce an Application name" in {
    assert(app.name == "TestApp")
  }


}
