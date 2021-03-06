package com.ligadata.smartfileadapter

import com.ligadata.InputOutputAdapterInfo.AdapterConfiguration
import org.scalatest._
import com.ligadata.AdaptersConfiguration._
import com.ligadata.InputAdapters._
import org.apache.commons.lang.StringUtils

/**
  * Created by Yasser on 7/21/2016.
  */
class TestOrdering extends FunSpec with BeforeAndAfter with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen {

  val file1 = "/data/input/20160715.INTEC_DIGICELY_EMM_JA.3"
  val file2 = "/data/input/20160715.INTEC_DIGICELY_EMM_JA.10"

  val invalidFileName = "/data/input/INTEC_DIGICELY_EMM_JA.10"

  describe("Test file name pattern matching"){
    it("Should"){
      val conf = getAdapterCfg
      val loc1 = conf.monitoringConfig.detailedLocations(0)

      val file1Match = MonitorUtils.isPatternMatch(MonitorUtils.getFileName(file1), loc1.fileComponents.regex)
      val invalidMatch = MonitorUtils.isPatternMatch(MonitorUtils.getFileName(invalidFileName), loc1.fileComponents.regex)

      file1Match shouldEqual true
      invalidMatch shouldEqual false
    }
  }

  describe("Test order by file name components"){
    it("Should find file1 preceeding file2"){

      val conf = getAdapterCfg
      val loc1 = conf.monitoringConfig.detailedLocations(0)

      val fileHandler1 = SmartFileHandlerFactory.createSmartFileHandler(conf, file1)
      val fileHandler2 = SmartFileHandlerFactory.createSmartFileHandler(conf, file2)
      val res = MonitorUtils.compareFiles(fileHandler1, fileHandler2, loc1)
      assert(res < 0)

    }
  }

  def getAdapterCfg : SmartFileAdapterConfiguration = {
    //just read and parse adapter config
    val inputConfig = new AdapterConfiguration()
    inputConfig.Name = "TestInput_2"
    inputConfig.className = "com.ligadata.InputAdapters.SamrtFileInputAdapter$"
    inputConfig.jarName = "smartfileinputoutputadapters_2.10-1.0.jar"
    //inputConfig.dependencyJars = new Set()
    inputConfig.adapterSpecificCfg = adapterSpecificCfgJson
    val conf = SmartFileAdapterConfiguration.getAdapterConfig(inputConfig)
    conf
  }

  val adapterSpecificCfgJson =
    """
      |{
      |  "Type": "das/nas",
      |  "ConnectionConfig": {

      |  },
      |  "MonitoringConfig": {
      |    "MaxTimeWait": "10000",
      |	  "WorkerBufferSize": "4",
      |	  "ConsumersCount": "2",
      |
      |	  "OrderBy": ["date" , "region", "serial"],
      |	  "MessageSeparator": "10",
      |
      |	  "DetailedLocations":[
      |	     {
      |		   "srcDir": "/data/input",
      |		   "targetDir": "/data/processed",
      |		   "FileComponents": {
      |			  "Components" : ["date", "source_type", "phy_switch", "proc_source", "region", "serial"],
      |			  "Regex": "^([0-9]{8})\\.([A-Za-z]+)_([A-Z]+)_([A-Z]+)_([A-Z]+)\\.([0-9]+)$",
      |			  "Paddings" : {
      |			    "serial": ["left", "5", "0"]
      |			  }
      |	       },
      |		   "MsgTags": ["emm", "$FileName"],
      |		   "TagDelimiter":"^",
      |
      |		   "MessageSeparator": "10",
      |		   "OrderBy": ["date" , "region", "serial"]
      |		  }
      |	   ]
      |  }
      |}
    """.stripMargin
}
