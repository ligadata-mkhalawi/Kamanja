package com.ligadata.smartfileadapter

import org.scalatest._
import com.ligadata.AdaptersConfiguration.SmartFileProducerConfiguration
import com.ligadata.InputOutputAdapterInfo.AdapterConfiguration
import com.ligadata.Exceptions.{ KamanjaException, FatalAdapterException }

class TestProducerConfigs extends FunSpec with BeforeAndAfter with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen {

  describe("Test Smart File Producer configuration") {

    val inputConfig = new AdapterConfiguration()
    inputConfig.Name = "TestOutput"
    inputConfig.className = "com.ligadata.InputAdapters.SamrtFileProducer$"
    inputConfig.jarName = "smartfileinputoutputadapters_2.10-1.0.jar"

    it("should read configuration correctly from a valid JSON") {

      inputConfig.adapterSpecificCfg =
        """
  		  |{
  		  |  "Uri": "hdfs://nameservice/folder/to/save",
	  	  |  "FileNamePrefix": "Data",
        |  "MessageSeparator": "\n",
		    |  "Compression": "gz",
  		  |  "RolloverInterval": "3600",
	  	  |  "TimePartitionFormat": "year=${yyyy}/month=${MM}/day=${dd}",
		    |  "PartitionBuckets": "10",
		    |  "Kerberos": {
	  	  |	     "Principal": "user@domain.com",
	  	  |	     "Keytab": "/path/to/keytab/user.keytab"
	  	  |  }
	  	  |}
		    """.stripMargin

      val conf = SmartFileProducerConfiguration.getAdapterConfig(inputConfig)

      conf.uri shouldEqual  "hdfs://nameservice/folder/to/save"
      conf.fileNamePrefix shouldEqual "Data"
      conf.messageSeparator shouldEqual "\n"
      conf.compressionString shouldEqual "gz"
      conf.rolloverInterval shouldEqual 3600
      conf.partitionFormat shouldEqual "year=${yyyy}/month=${MM}/day=${dd}"
      conf.partitionBuckets shouldEqual 10
      conf.kerberos.principal shouldEqual "user@domain.com"
      conf.kerberos.keytab shouldEqual "/path/to/keytab/user.keytab"
    }
    
    it("should throw FatalAdapterException if uri is missing") {

      inputConfig.adapterSpecificCfg = 
        """
        |{
        |  "FileNamePrefix": "Data"
        |}
        """.stripMargin

      a [FatalAdapterException] should be thrownBy {
        val conf = SmartFileProducerConfiguration.getAdapterConfig(inputConfig)
      }
    }

    it("should throw FatalAdapterException if uri is invalid") {

      inputConfig.adapterSpecificCfg = 
        """
        |{
  		  |  "Uri": "/nameservice/folder/to/save",
        |  "FileNamePrefix": "Data"
        |}
        """.stripMargin

      a [FatalAdapterException] should be thrownBy {
        val conf = SmartFileProducerConfiguration.getAdapterConfig(inputConfig)
      }
    }

    it("should throw FatalAdapterException if Principal is missing for Kerberos") {

      inputConfig.adapterSpecificCfg = 
        """
        |{
  		  |  "Uri": "/nameservice/folder/to/save",
        |  "FileNamePrefix": "Data",
  		  |  "Kerberos": {
		    |	     "Keytab": "/path/to/keytab/user.keytab"
	      |  }
        |}
        """.stripMargin

      a [FatalAdapterException] should be thrownBy {
        val conf = SmartFileProducerConfiguration.getAdapterConfig(inputConfig)
      }
    }
    
    it("should throw FatalAdapterException if Keytab is missing for Kerberos") {

      inputConfig.adapterSpecificCfg = 
        """
        |{
   	    |  "Uri": "/nameservice/folder/to/save",
        |  "FileNamePrefix": "Data",
    	  |  "Kerberos": {
		    |	     "Principal": "user@domain.com",
		    |  }
        |}
        """.stripMargin

      a [FatalAdapterException] should be thrownBy {
        val conf = SmartFileProducerConfiguration.getAdapterConfig(inputConfig)
      }
    }

    
  }

}
