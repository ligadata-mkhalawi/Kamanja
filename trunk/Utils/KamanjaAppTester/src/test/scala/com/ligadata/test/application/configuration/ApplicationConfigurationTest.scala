package com.ligadata.test.application.configuration

import com.ligadata.test.application.metadata.{PmmlModelElement, _}
import org.scalatest._

class ApplicationConfigurationTest extends FlatSpec {
  "ApplicationConfiguration" should "read in a configuration file and generate a List of MetadataElement" in {
    val configFile: String = getClass.getResource("/ApplicationConfigurationTest/TestConfig.json").getPath
    val appConfig = new ApplicationConfiguration(configFile)

    assert(!appConfig.metadataElements.isEmpty)

    appConfig.metadataElements.foreach(md => {
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
}
