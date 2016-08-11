package com.ligadata.test.application.configuration

import com.ligadata.test.application.metadata.{PmmlModelElement, _}
import com.ligadata.test.application.KamanjaApplication

import org.scalatest._

class KamanjaApplicationConfigurationTests extends FlatSpec with BeforeAndAfterAll {
  var configFile: String = ""
  var appConfig: KamanjaApplicationConfiguration = _
  var app: KamanjaApplication = _

  override def beforeAll {
    configFile = getClass.getResource("/KamanjaApplicationConfigurationTest/TestConfig.json").getPath
    appConfig = new KamanjaApplicationConfiguration
    app = appConfig.initializeApplication("/TestApp", configFile)
  }

  "ApplicationConfiguration" should "read in a configuration file and generate a List of MetadataElement" in {
    assert(!app.metadataElements.isEmpty)

    app.metadataElements.foreach(md => {
      md match {
        case e: ContainerElement =>
          assert(e.filename == "/TestApp/metadata/container/container.json")
          assert(e.tenantId == "tenant1")
        case e: MessageElement =>
          e.filename match {
            case "/TestApp/metadata/message/inputMessage.json" =>
              assert(e.tenantId == "tenant1")
            case "/TestApp/metadata/message/outputMessage.json" =>
              assert(e.tenantId == "tenant1")
            case _ => fail(s"Unrecognized filename: " + e.filename)
          }
        case e: JavaModelElement =>
          assert(e.filename == "/TestApp/metadata/model/model.java")
          assert(e.tenantId == "tenant1")
          assert(e.modelCfg == "modelCfg")
        case e: ScalaModelElement =>
          assert(e.filename == "/TestApp/metadata/model/model.scala")
          assert(e.tenantId == "tenant1")
          assert(e.modelCfg == "modelCfg")
        case e: KPmmlModelElement =>
          assert(e.filename == "/TestApp/metadata/model/kpmmlModel.xml")
          assert(e.tenantId == "tenant1")
        case e: PmmlModelElement =>
          if(e.filename == "/TestApp/metadata/model/pmmlModel.xml") {
            assert(e.tenantId == "tenant1")
            assert(e.msgConsumed == "com.ligadata.test.message.InputMessage")
            assert(e.msgProduced == Some("com.ligadata.test.message.OutputMessage"))
          }
          else if(e.filename == "/TestApp/metadata/model/pmmlModel2.xml") {
            assert(e.tenantId == "tenant1")
            assert(e.msgConsumed == "com.ligadata.test.message.InputMessage")
            assert(e.msgProduced == None)
          }
          else fail("Unexpected filename: " + e.filename)
        case e: ModelConfigurationElement =>
          assert(e.filename == "/TestApp/metadata/configuration/modelCfg.json")
        case e: AdapterMessageBindingElement =>
          assert(e.filename == "/TestApp/metadata/configuration/adapterMsgBindings.json")
      }
    })
  }

  it should "read in a configuration file and generate a List of DataSets" in {
    assert(!app.dataSets.isEmpty)

    val ds1 = app.dataSets(0)
    val ds2 = app.dataSets(1)

    assert(ds1.inputDataFile == "/TestApp/data/inputFile1.csv")
    assert(ds1.inputDataFormat == "CSV")
    assert(ds1.expectedResultsFile == "/TestApp/data/expectedResults1.csv")
    assert(ds1.expectedResultsFormat == "CSV")

    assert(ds2.inputDataFile == "/TestApp/data/inputFile2.json")
    assert(ds2.inputDataFormat == "JSON")
    assert(ds2.expectedResultsFile == "/TestApp/data/expectedResults2.json")
    assert(ds2.expectedResultsFormat == "JSON")
  }

  it should "read in a configuration file and produce an Application name" in {
    assert(app.name == "TestApp")
  }


}
