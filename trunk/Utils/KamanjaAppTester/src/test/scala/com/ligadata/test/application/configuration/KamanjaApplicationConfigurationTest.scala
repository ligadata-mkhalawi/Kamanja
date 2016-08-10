package com.ligadata.test.application.configuration

import com.ligadata.test.application.metadata.{PmmlModelElement, _}
import com.ligadata.test.application.KamanjaApplication

import org.scalatest._

class KamanjaApplicationConfigurationTest extends FlatSpec with BeforeAndAfterAll {
  var configFile: String = ""
  var appConfig: KamanjaApplicationConfiguration = _
  var app: KamanjaApplication = _

  override def beforeAll {
    configFile = getClass.getResource("/ApplicationConfigurationTest/TestConfig.json").getPath
    appConfig = new KamanjaApplicationConfiguration(configFile)
    app = appConfig.initializeApplication
  }

  "ApplicationConfiguration" should "read in a configuration file and generate a List of MetadataElement" in {
    assert(!app.metadataElements.isEmpty)

    app.metadataElements.foreach(md => {
      md match {
        case e: ContainerElement =>
          assert(e.filename == "container.json")
          assert(e.tenantId == "tenant1")
        case e: MessageElement =>
          e.filename match {
            case "inputMessage.json" =>
              assert(e.tenantId == "tenant1")
            case "outputMessage.json" =>
              assert(e.tenantId == "tenant1")
            case _ => fail(s"Unrecognized filename: " + e.filename)
          }
        case e: JavaModelElement =>
          assert(e.filename == "model.java")
          assert(e.tenantId == "tenant1")
          assert(e.modelCfg == "modelCfg")
        case e: ScalaModelElement =>
          assert(e.filename == "model.scala")
          assert(e.tenantId == "tenant1")
          assert(e.modelCfg == "modelCfg")
        case e: KPmmlModelElement =>
          assert(e.filename == "kpmmlModel.xml")
          assert(e.tenantId == "tenant1")
        case e: PmmlModelElement =>
          if(e.filename == "pmmlModel.xml") {
            assert(e.tenantId == "tenant1")
            assert(e.msgConsumed == "com.ligadata.test.message.InputMessage")
            assert(e.msgProduced == Some("com.ligadata.test.message.OutputMessage"))
          }
          else if(e.filename == "pmmlModel2.xml") {
            assert(e.tenantId == "tenant1")
            assert(e.msgConsumed == "com.ligadata.test.message.InputMessage")
            assert(e.msgProduced == None)
          }
          else fail("Unexpected filename: " + e.filename)
        case e: ModelConfigurationElement =>
          assert(e.filename == "modelCfg.json")
        case e: AdapterMessageBindingElement =>
          assert(e.filename == "adapterMsgBindings.json")
      }
    })
  }

  it should "read in a configuration file and generate a List of DataSets" in {
    assert(!app.dataSets.isEmpty)

    val ds1 = app.dataSets(0)
    val ds2 = app.dataSets(1)

    assert(ds1.inputDataFile == "inputFile1.csv")
    assert(ds1.inputDataFormat == "CSV")
    assert(ds1.expectedResultsFile == "expectedResults1.csv")
    assert(ds1.expectedResultsFormat == "CSV")

    assert(ds2.inputDataFile == "inputFile2.json")
    assert(ds2.inputDataFormat == "JSON")
    assert(ds2.expectedResultsFile == "expectedResults2.json")
    assert(ds2.expectedResultsFormat == "JSON")
  }

  it should "read in a configuration file and produce an Application name" in {
    assert(app.name == "TestApp")
  }
}
