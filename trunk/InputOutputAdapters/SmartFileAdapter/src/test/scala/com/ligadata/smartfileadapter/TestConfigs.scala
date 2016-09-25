package com.ligadata.smartfileadapter

import org.scalatest._
import com.ligadata.AdaptersConfiguration.SmartFileAdapterConfiguration
import com.ligadata.InputOutputAdapterInfo.AdapterConfiguration
/**
  * Created by Yasser on 3/10/2016.
  */
class TestConfigs extends FunSpec with BeforeAndAfter with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen {

  val adapterSpecificCfgJson =
    """
{
      |  "Type": "Hdfs",
      |  "ConnectionConfig": {
      |    "HostLists": "10.20.30.40:2000,10.20.30.40:2001",
      |    "UserId": "uid",
      |    "Password": "pwd"
      |  },
      |  "MonitoringConfig": {
      |    "MaxTimeWait": "10000",
      |    "WorkerBufferSize": "4",
      |    "ConsumersCount": "2",
      |    "targetMoveDir": "/data/processed",
      |    "FileComponents": {
      |          "Components": ["date", "region", "serial"],
      |          "Regex": "^([0-9]{4})\\.([A-Z]+)\\.([0-9]+)$",
      |          "Paddings": {
      |            "serial": ["left", "5", "0" ]
      |          }
      |      },
      |    "OrderBy": ["$FILE_MOD_TIME", "serial"],
      |    "MessageSeparator": "10",
      |    "MsgTags": ["$FileName"],
      |    "TagDelimiter": "$$",
      |    "DetailedLocations": [
      |      {
      |        "srcDir": "/data/input",
      |        "targetDir": "/data/processed1",
      |        "enableMoving":"off",
      |        "FileComponents": {
      |          "Components": ["date", "source_type", "phy_switch", "proc_source", "region", "serial"],
      |          "Regex": "^([0-9]{4})([0-9]{2})([0-9]{2})\\.([A-Za-z]+)_([A-Z]+)_([A-Z]+)_([A-Z]+)\\.([0-9]+)$",
      |          "Paddings": {
      |            "serial": ["right", "3", "-" ]
      |          }
      |        },
      |        "MsgTags": [ "emm", "$FileName"],
      |        "TagDelimiter": "^",
      |        "MessageSeparator": "10",
      |        "OrderBy": ["date", "region", "serial"]
      |      },
      |      {
      |        "srcDir": "/data/input2"
      |      }
      |    ]
      |  }
      |}
    """.stripMargin

  val adapterSpecificCfgJsonKerberosOldConfig =
  """{
    |    "Type": "Hdfs",
    |    "ConnectionConfig": {
    |      "HostLists": "10.20.30.40:2000,10.20.30.40:2001",
    |      "Authentication": "kerberos",
    |	     "Principal": "user@domain.com",
    |	     "Keytab": "/tmp/user.keytab"
    |    },
    |    "MonitoringConfig": {
    |	     "Locations": "/data/input,/tmp/input",
    |      "TargetMoveDir": "/data/processed",
    |      "MaxTimeWait": "5000"
    |    }
    |}""".stripMargin

  describe("Test smart file adapter adapterSpecificCfg json parsing") {

    it("should get right attribute values for adapter") {

      val inputConfig = new AdapterConfiguration()
      inputConfig.Name = "TestInput_2"
      inputConfig.className = "com.ligadata.InputAdapters.SamrtFileInputAdapter$"
      inputConfig.jarName = "smartfileinputoutputadapters_2.10-1.0.jar"
      //inputConfig.dependencyJars = new Set()
      inputConfig.adapterSpecificCfg = adapterSpecificCfgJson

      val conf = SmartFileAdapterConfiguration.getAdapterConfig(inputConfig)

      conf._type shouldEqual  "Hdfs"
      conf.connectionConfig.userId shouldEqual "uid"
      conf.connectionConfig.password shouldEqual "pwd"
      conf.connectionConfig.hostsList.mkString(",") shouldEqual "10.20.30.40:2000,10.20.30.40:2001"
      //conf.monitoringConfig.locations.mkString(",") shouldEqual "/data/input,/tmp/input"
      conf.monitoringConfig.waitingTimeMS shouldEqual 10000
      conf.monitoringConfig.orderBy.length shouldEqual 2

      conf.monitoringConfig.detailedLocations.length shouldEqual 2

      val loc1 = conf.monitoringConfig.detailedLocations(0)
      loc1.srcDir shouldEqual "/data/input"
      loc1.targetDir shouldEqual "/data/processed1"
      loc1.orderBy.mkString(",") shouldEqual "date,region,serial"
      loc1.fileComponents.components.mkString(",") shouldEqual "date,source_type,phy_switch,proc_source,region,serial"
      loc1.fileComponents.regex shouldEqual "^([0-9]{4})([0-9]{2})([0-9]{2})\\.([A-Za-z]+)_([A-Z]+)_([A-Z]+)_([A-Z]+)\\.([0-9]+)$"
      loc1.fileComponents.paddings("serial").padSize shouldEqual 3
      loc1.messageSeparator shouldEqual 10
      loc1.msgTags.mkString(",") shouldEqual "emm,$FileName"
      loc1.tagDelimiter shouldEqual "^"
      loc1.isMovingEnabled shouldEqual false

      val loc2 = conf.monitoringConfig.detailedLocations(1)
      loc2.srcDir shouldEqual "/data/input2"
      loc2.targetDir shouldEqual "/data/processed"
      loc2.orderBy.mkString(",") shouldEqual "$FILE_MOD_TIME,serial"
      loc2.fileComponents.components.mkString(",") shouldEqual "date,region,serial"
      loc2.fileComponents.regex shouldEqual "^([0-9]{4})\\.([A-Z]+)\\.([0-9]+)$"
      loc2.fileComponents.paddings("serial").padSize shouldEqual 5
      loc2.messageSeparator shouldEqual 10
      loc2.msgTags.mkString(",") shouldEqual "$FileName"
      loc2.tagDelimiter shouldEqual "$$"
      loc2.isMovingEnabled shouldEqual true
    }
  }


  describe("Test smart file adapter config with old style parsing") {

    it("should get old locations/targetMoveDir values") {

      val inputConfig = new AdapterConfiguration()
      inputConfig.Name = "TestInput_2"
      inputConfig.className = "com.ligadata.InputAdapters.SamrtFileInputAdapter$"
      inputConfig.jarName = "smartfileinputoutputadapters_2.10-1.0.jar"
      //inputConfig.dependencyJars = new Set()
      inputConfig.adapterSpecificCfg = adapterSpecificCfgJsonKerberosOldConfig

      val conf = SmartFileAdapterConfiguration.getAdapterConfig(inputConfig)

      conf._type shouldEqual  "Hdfs"
      conf.connectionConfig.authentication shouldEqual "kerberos"
      conf.connectionConfig.principal shouldEqual "user@domain.com"
      conf.connectionConfig.keytab shouldEqual "/tmp/user.keytab"
      conf.connectionConfig.hostsList.mkString(",") shouldEqual "10.20.30.40:2000,10.20.30.40:2001"
      conf.monitoringConfig.detailedLocations.length shouldEqual 2

      conf.monitoringConfig.detailedLocations(0).srcDir shouldEqual "/data/input"
      conf.monitoringConfig.detailedLocations(0).targetDir shouldEqual "/data/processed"
      conf.monitoringConfig.detailedLocations(1).srcDir shouldEqual "/tmp/input"
      conf.monitoringConfig.detailedLocations(1).targetDir shouldEqual "/data/processed"
    }
  }
}
