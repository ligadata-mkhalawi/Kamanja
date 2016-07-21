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
      |{
      |  "Type": "Hdfs",
      |  "ConnectionConfig": {
      |    "HostLists": "10.20.30.40:2000,10.20.30.40:2001",
      |    "UserId": "uid",
      |    "Password": "pwd"
      |  },
      |  "MonitoringConfig": {
      |    "MaxTimeWait": "10000",
      |	  "WorkerBufferSize": "4",
      |	  "ConsumersCount": "2",
      |
      |	  "OrderBy": ["date" , "region", "Serial"],
      |	  "MessageSeparator": "10",
      |
      |	  "Locations":[
      |	     {
      |		   "srcDir": "/data/input",
      |		   "targetDir": "/data/processed",
      |		   "FileComponents": {
      |			  "Components" : "date, source_type, phy_switch, proc_source, region, serial",
      |			  "Regex": "^([0-9]{4})([0-9]{2})([0-9]{2})\\.([A-Za-z]+)_([A-Z]+)_([A-Z]+)_([A-Z]+)\\.([0-9]+)$",
      |			  "Paddings" : {
      |			    "serial": ["right", "3", "-"]
      |			  }
      |	       },
      |		   "MsgTags": ["emm", "$FileName"],
      |		   "TagDelimiter":"^",
      |
      |		   "MessageSeparator": "10",
      |		   "OrderBy": ["date" , "region", "Serial"]
      |		  }
      |	   ]
      |  }
      |}
    """.stripMargin

  val adapterSpecificCfgJsonKerberos =
  """{
    |    "Type": "Hdfs",
    |    "ConnectionConfig": {
    |      "HostLists": "10.20.30.40:2000,10.20.30.40:2001",
    |      "Authentication": "kerberos",
    |	     "Principal": "user@domain.com",
    |	     "Keytab": "/tmp/user.keytab"
    |    },
    |    "MonitoringConfig": {
    |	  "Locations": "/data/input,/tmp/input",
    |   "TargetMoveDir": "/data/processed",
    |      "MaxTimeWait": "5000"
    |    }
    |}""".stripMargin

  describe("Test smart file adapter adapterSpecificCfg json parsing") {

    it("should get right values for attributes (type, userid, password, hostslist, location and waitingTimeMS)") {

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
      conf.monitoringConfig.orderBy.length shouldEqual 3

      val loc1 = conf.monitoringConfig.locations(0)
      println("loc1 info")
      println("srcDir=" + loc1.srcDir)
      println("targetDir=" + loc1.targetDir)
      println("msgTags=" + loc1.msgTags.mkString(","))
      println("tagDelimiter=" + loc1.tagDelimiter)
      println("messageSeparator=" + loc1.messageSeparator.toInt)
      println("orderBy=" + loc1.orderBy.mkString(","))

      println("components" + loc1.fileComponents.components.mkString(","))
      println("regex" + loc1.fileComponents.regex)

      loc1.fileComponents.paddings.foreach(kv =>{
        println("padding component: " + kv._1 + s", pos= ${kv._2.padPos} , size= ${kv._2.padSize} , padstr= ${kv._2.padStr}")
      })

    }
  }


  /*describe("Test smart file adapter adapterSpecificCfg json parsing with kerberos auth") {

    it("should get right values for attributes (type, principal, password, keytab, location and waitingTimeMS") {

      val inputConfig = new AdapterConfiguration()
      inputConfig.Name = "TestInput_2"
      inputConfig.className = "com.ligadata.InputAdapters.SamrtFileInputAdapter$"
      inputConfig.jarName = "smartfileinputoutputadapters_2.10-1.0.jar"
      //inputConfig.dependencyJars = new Set()
      inputConfig.adapterSpecificCfg = adapterSpecificCfgJsonKerberos

      val conf = SmartFileAdapterConfiguration.getAdapterConfig(inputConfig)

      conf._type shouldEqual  "Hdfs"
      conf.connectionConfig.authentication shouldEqual "kerberos"
      conf.connectionConfig.principal shouldEqual "user@domain.com"
      conf.connectionConfig.keytab shouldEqual "/tmp/user.keytab"
      conf.connectionConfig.hostsList.mkString(",") shouldEqual "10.20.30.40:2000,10.20.30.40:2001"
      conf.monitoringConfig.locations.mkString(",") shouldEqual "/data/input,/tmp/input"
      conf.monitoringConfig.waitingTimeMS shouldEqual 5000
    }
  }*/
}
