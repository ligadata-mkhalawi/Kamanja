/*
 * Copyright 2015 ligaDATA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ligadata.automation.unittests.api

import com.ligadata.MetadataAPI.MetadataAPI.ModelType
import com.ligadata.automation.unittests.api.setup._
import org.scalatest._
import Matchers._

import com.ligadata.MetadataAPI._
import com.ligadata.kamanja.metadata._
import com.ligadata.kamanja.metadata.MdMgr._

import com.ligadata.Utils._
import util.control.Breaks._
import scala.io._
import java.util.Date
import java.io._

import sys.process._
import org.apache.logging.log4j._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import com.ligadata.Serialize._

import org.json4s.native.Serialization
import org.json4s.native.Serialization.{ read, write, writePretty }

import com.ligadata.kamanja.metadataload.MetadataLoad
import com.ligadata.MetadataAPI.Utility._

class AddHelloWorldModelSpec extends FunSpec with LocalTestFixtures with BeforeAndAfter with BeforeAndAfterAll with GivenWhenThen {
  var res: String = null;
  var statusCode: Int = -1;
  var apiResKey: String = "\"Status Code\" : 0"
  var objName: String = null
  var contStr: String = null
  var version: String = null
  var o: Option[ContainerDef] = None
  var dirName: String = null
  var iFile: File = null
  var fileList: List[String] = null
  var newVersion: String = null
  val userid: Option[String] = Some("kamanja")
  val tenantId: Option[String] = Some("kamanja")

  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)

  private var containerList: Array[String] = Array("config_objects", "jar_store", "model_config_objects", "metadata_objects", "transaction_id","avroschemainfo","element_info","elementinfo","ismetadata","metadatacounters")

  private def TruncateDbStore = {
    val db = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("DATABASE")
    assert(null != db)
    var ds = MetadataAPIImpl.GetMainDS
    ds.TruncateContainer(containerList)
  }

  private def DropDbStore = {
    val db = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("DATABASE")
    assert(null != db)
    var ds = MetadataAPIImpl.GetMainDS
    ds.DropContainer(containerList)
  }


  override def beforeAll = {
    try {

      logger.info("starting...");

      logger.info("resource dir => " + getClass.getResource("/").getPath)

      logger.info("Initialize MetadataManager")
      mdMan.config.classPath = ConfigDefaults.metadataClasspath
      mdMan.initMetadataCfg

      logger.info("Initialize MdMgr")
      MdMgr.GetMdMgr.truncate
      val mdLoader = new MetadataLoad(MdMgr.mdMgr, "", "", "", "")
      mdLoader.initialize

      logger.info("Startup embedded zooKeeper ")
      val zkServer = EmbeddedZookeeper
      zkServer.instance.startup

      logger.info("Initialize zooKeeper connection")
      MetadataAPIImpl.initZkListeners(false)

      logger.info("Initialize datastore")
      var tmpJarPaths = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_PATHS")
      logger.info("jarPaths => " + tmpJarPaths)
      val jarPaths = if (tmpJarPaths != null) tmpJarPaths.split(",").toSet else scala.collection.immutable.Set[String]()
      MetadataAPIImpl.OpenDbStore(jarPaths, MetadataAPIImpl.GetMetadataAPIConfig.getProperty("METADATA_DATASTORE"))

      var jp = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_PATHS")
      logger.info("jarPaths => " + jp)


      logger.info("Truncating dbstore")
      TruncateDbStore

      And("PutTranId updates the tranId")
      noException should be thrownBy {
	MetadataAPIImpl.PutTranId(0)
      }

      logger.info("Load All objects into cache")
      MetadataAPIImpl.LoadAllObjectsIntoCache(false)

      // The above call is resetting JAR_PATHS based on nodeId( node-specific configuration)
      // This is causing unit-tests to fail
      // restore JAR_PATHS value
      MetadataAPIImpl.GetMetadataAPIConfig.setProperty("JAR_PATHS", tmpJarPaths)
      jp = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_PATHS")
      logger.info("jarPaths => " + jp)

      logger.info("Initialize security adapter")
      val tempAuditParamsFile = getClass.getResource("/").getPath + this.getClass.getSimpleName
      MetadataAPIImpl.GetMetadataAPIConfig.setProperty("AUDIT_PARMS", TestUtils.createAuditParamsFile(tempAuditParamsFile))
      MetadataAPIImpl.GetMetadataAPIConfig.setProperty("AUDIT_PARMS", TestUtils.createAuditParamsFile(this.getClass.getSimpleName))
      MetadataAPIImpl.InitSecImpl

      //MetadataAPIImpl.TruncateAuditStore
      MetadataAPIImpl.isInitilized = true
      logger.info(MetadataAPIImpl.GetMetadataAPIConfig)
    }
    catch {
      case e: EmbeddedZookeeperException => {
	throw new EmbeddedZookeeperException("EmbeddedZookeeperException detected")
      }
      case e: Exception => throw new Exception("Failed to execute set up properly", e)
    }
  }

  /**
   * extractNameFromPMML - pull the Application name="xxx" from the PMML doc and construct
   * a name  string from it, cloned from APIService.scala
   */
  def extractNameFromPMML(pmmlObj: String): String = {
    var firstOccurence: String = "unknownModel"
    val pattern = """Application[ ]*name="([^ ]*)"""".r
    val allMatches = pattern.findAllMatchIn(pmmlObj)
    allMatches.foreach(m => {
      if (firstOccurence.equalsIgnoreCase("unknownModel")) {
	firstOccurence = (m.group(1))
      }
    })
    return firstOccurence
  }

  describe("Unit Tests for all MetadataAPI operations") {

    // validate property setup
    it("Validate properties for MetadataAPI") {
      And("MetadataAPIImpl.GetMetadataAPIConfig should have been initialized")
      val cfg = MetadataAPIImpl.GetMetadataAPIConfig
      assert(null != cfg)

      And("The property DATABASE must have been defined")
      val db = cfg.getProperty("DATABASE")
      assert(null != db)
      if (db == "cassandra") {
	And("The property MetadataLocation must have been defined for store type " + db)
	val loc = cfg.getProperty("DATABASE_LOCATION")
	assert(null != loc)
	And("The property MetadataSchemaName must have been defined for store type " + db)
	val schema = cfg.getProperty("DATABASE_SCHEMA")
	assert(null != schema)
      }
      And("The property NODE_ID must have been defined")
      assert(null != cfg.getProperty("NODE_ID"))


      And("The property JAR_TRAGET_DIR must have been defined")
      val d = cfg.getProperty("JAR_TARGET_DIR")
      assert(null != d)

      And("Make sure the Directory " + d + " exists")
      val f = new File(d)
      assert(null != f)

      And("The property SCALA_HOME must have been defined")
      val sh = cfg.getProperty("SCALA_HOME")
      assert(null != sh)

      And("The property JAVA_HOME must have been defined")
      val jh = cfg.getProperty("SCALA_HOME")
      assert(null != jh)

      And("The property CLASSPATH must have been defined")
      val cp = cfg.getProperty("CLASSPATH")
      assert(null != cp)

      And("The property ZNODE_PATH must have been defined")
      val zkPath = cfg.getProperty("ZNODE_PATH")
      assert(null != zkPath)

      And("The property ZOOKEEPER_CONNECT_STRING must have been defined")
      val zkConnStr = cfg.getProperty("ZOOKEEPER_CONNECT_STRING")
      assert(null != zkConnStr)

      And("The property SERVICE_HOST must have been defined")
      val shost = cfg.getProperty("SERVICE_HOST")
      assert(null != shost)

      And("The property SERVICE_PORT must have been defined")
      val sport = cfg.getProperty("SERVICE_PORT")
      assert(null != sport)

      And("The property JAR_PATHS must have been defined")
      val jp = cfg.getProperty("JAR_PATHS")
      assert(null != jp)
      logger.info("jar_paths => " + jp)

      And("The property SECURITY_IMPL_JAR  must have been defined")
      val sij = cfg.getProperty("SECURITY_IMPL_JAR")
      assert(null != sij)

      And("The property SECURITY_IMPL_CLASS  must have been defined")
      val sic = cfg.getProperty("SECURITY_IMPL_CLASS")
      assert(null != sic)

      And("The property DO_AUTH  must have been defined")
      val da = cfg.getProperty("DO_AUTH")
      assert(null != da)

      And("The property AUDIT_IMPL_JAR  must have been defined")
      val aij = cfg.getProperty("AUDIT_IMPL_JAR")
      assert(null != sij)

      And("The property AUDIT_IMPL_CLASS  must have been defined")
      val aic = cfg.getProperty("AUDIT_IMPL_CLASS")
      assert(null != sic)

      And("The property DO_AUDIT  must have been defined")
      val dau = cfg.getProperty("DO_AUDIT")
      assert(null != dau)

      And("The property SSL_CERTIFICATE  must have been defined")
      val sc = cfg.getProperty("SSL_CERTIFICATE")
      assert(null != sc)
    }

    it("Add Cluster Config ") {

      And("Check whether CONFIG_FILES_DIR defined as property")
      dirName = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("CONFIG_FILES_DIR")
      assert(null != dirName)

      And("Check Directory Path")
      iFile = new File(dirName)
      assert(true == iFile.exists)

      And("Check whether " + dirName + " is a directory ")
      assert(true == iFile.isDirectory)

      And("Make sure there are few JSON config files in " + dirName);
      val cfgFiles = new java.io.File(dirName).listFiles.filter(_.getName.endsWith(".json"))
      assert(0 != cfgFiles.length)

      fileList = List("ClusterConfig_kafka_v10.json")
      fileList.foreach(f1 => {
	And("Add the Config From " + f1)
	And("Make Sure " + f1 + " exist")
	var exists = false
	var file: java.io.File = null
	breakable {
	  cfgFiles.foreach(f2 => {
	    if (f2.getName() == f1) {
	      exists = true
	      file = f2
	      break
	    }
	  })
	}
	assert(true == exists)

	And("AddConfig first time from " + file.getPath)
	var cfgStr = Source.fromFile(file).mkString
	res = MetadataAPIImpl.UploadConfig(cfgStr, None, "testConfig")
	res should include regex ("\"Status Code\" : 0")

	And("GetAllCfgObjects to fetch all config objects")
	res = MetadataAPIImpl.GetAllCfgObjects("JSON", None)
	res should include regex ("\"Status Code\" : 0")

	And("GetAllNodes to fetch the nodes")
	res = MetadataAPIImpl.GetAllNodes("JSON", None)
	res should include regex ("\"Status Code\" : 0")
	logger.info(res)

	And("GetAllAdapters to fetch the adapters")
	res = MetadataAPIImpl.GetAllAdapters("JSON", None)
	res should include regex ("\"Status Code\" : 0")

	And("GetAllClusters to fetch the clusters")
	res = MetadataAPIImpl.GetAllClusters("JSON", None)
	res should include regex ("\"Status Code\" : 0")

	And("Check number of the nodes")
	var nodes = MdMgr.GetMdMgr.Nodes
	assert(nodes.size == 1)

	And("Check number of the adapters")
	var adapters = MdMgr.GetMdMgr.Adapters
	assert(adapters.size == 11)

	And("RemoveConfig API for the config that was just added")
	res = MetadataAPIImpl.RemoveConfig(cfgStr, None, "testConfig")
	res should include regex ("\"Status Code\" : 0")

	And("Check number of the nodes after removing config")
	nodes = MdMgr.GetMdMgr.Nodes
	assert(nodes.size == 0)

	And("Check number of the adapters after removing config")
	adapters = MdMgr.GetMdMgr.Adapters
	assert(adapters.size == 0)

	And("AddConfig second time from " + file.getPath)
	cfgStr = Source.fromFile(file).mkString
	res = MetadataAPIImpl.UploadConfig(cfgStr, None, "testConfig")
	res should include regex ("\"Status Code\" : 0")
      })
    }

    def extractNameFromMsgDef(msgDef: String): String = {
      var firstOccurence: String = "unknownMsg"
      val pattern = """"Name": "([^ ]*)"""".r
      val allMatches = pattern.findAllMatchIn(msgDef)
      allMatches.foreach(m => {
	if (firstOccurence.equalsIgnoreCase("unknownMsg")) {
	  firstOccurence = (m.group(1))
	}
      })
      return firstOccurence
    }


    it("Add HelloWorld Model Messages") {
      And("Check whether MESSAGE_FILES_DIR defined as property")
      dirName = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("MESSAGE_FILES_DIR")
      assert(null != dirName)

      And("Check Directory Path")
      iFile = new File(dirName)
      assert(true == iFile.exists)

      And("Check whether " + dirName + " is a directory ")
      assert(true == iFile.isDirectory)

      And("Make sure there are few JSON message files in " + dirName);
      val msgFiles = new java.io.File(dirName).listFiles.filter(_.getName.endsWith(".json"))
      assert(0 != msgFiles.length)

      fileList = List("Message_Definition_HelloWorld.json","OutMessage_Definition_HelloWorld.json")
      fileList.foreach(f1 => {
	And("Add the Message From " + f1)
	And("Make Sure " + f1 + " exist")
	var exists = false
	var file: java.io.File = null
	breakable {
	  msgFiles.foreach(f2 => {
	    if (f2.getName() == f1) {
	      exists = true
	      file = f2
	      break
	    }
	  })
	}
	assert(true == exists)

	And("AddMessage first time from " + file.getPath)
	var msgStr = Source.fromFile(file).mkString
	res = MetadataAPIImpl.AddMessage(msgStr, "JSON", None,tenantId,None)
	res should include regex ("\"Status Code\" : 0")

	And("GetMessageDef API to fetch the message that was just added")
	var objName = extractNameFromMsgDef(msgStr)
	var version = "0000000000001000000"
	res = MetadataAPIImpl.GetMessageDef("com.ligadata.kamanja.samples.messages", objName, "JSON", version, None,None)
	res should include regex ("\"Status Code\" : 0")
      })
    }

    it("Add HelloWorld Model Config Object in preparation for adding scala source models") {
      And("Check whether CONFIG_FILES_DIR defined as property")
      dirName = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("CONFIG_FILES_DIR")
      assert(null != dirName)

      And("Check Directory Path")
      iFile = new File(dirName)
      assert(true == iFile.exists)

      And("Check whether " + dirName + " is a directory ")
      assert(true == iFile.isDirectory)

      And("Make sure there are few scala model files in " + dirName);
      val modFiles = new java.io.File(dirName).listFiles.filter(_.getName.endsWith(".json"))
      assert(0 != modFiles.length)

      //  Model Config Object
      fileList = List("Model_Config_HelloWorld.json")
      fileList.foreach(f1 => {
	And("Add the Model Config From " + f1)
	And("Make Sure " + f1 + " exist")
	var exists = false
	var file: java.io.File = null
	breakable {
	  modFiles.foreach(f2 => {
	    if (f2.getName() == f1) {
	      exists = true
	      file = f2
	      break
	    }
	  })
	}
	assert(true == exists)

	And("Call UploadModelConfig MetadataAPI Function to add Model from " + file.getPath)
	var modStr = Source.fromFile(file).mkString
	res = MetadataAPIImpl.UploadModelsConfig(modStr,
						 userid,   // userid
						 null,   // objectList
						 true   // isFromNotify
					       )
	res should include regex ("\"Status Code\" : 0")

	And("Dump  modelConfig that was just added");
	MdMgr.GetMdMgr.DumpModelConfigs
	And("GetModelDependencies to fetch the modelConfig that was just added")
	val cfgName = "HelloWorldModel";
	var msgsAndContainers = MetadataAPIImpl.getModelMessagesContainers(userid.get + "." + cfgName,userid);
	assert(msgsAndContainers.length == 2);
      })
    }

    it("Add com.ligadata.kamanja.samples.models.HelloWorld Java Models") {
      And("Check whether MODEL_FILES_DIR defined as property")
      dirName = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("MODEL_FILES_DIR")
      assert(null != dirName)

      And("Check Directory Path")
      iFile = new File(dirName)
      assert(true == iFile.exists)

      And("Check whether " + dirName + " is a directory ")
      assert(true == iFile.isDirectory)

      And("Make sure there are few java model files in " + dirName);
      val modFiles = new java.io.File(dirName).listFiles.filter(_.getName.endsWith(".java"))
      assert(0 != modFiles.length)

      // Scala Models
      fileList = List("HelloWorld.java")
      fileList.foreach(f1 => {
	And("Add the Model From " + f1)
	And("Make Sure " + f1 + " exist")
	var exists = false
	var file: java.io.File = null
	breakable {
	  modFiles.foreach(f2 => {
	    if (f2.getName() == f1) {
	      exists = true
	      file = f2
	      break
	    }
	  })
	}
	assert(true == exists)

	And("Call AddModel MetadataAPI Function to add Model from " + file.getPath)
	var modStr = Source.fromFile(file).mkString
	logger.info("scala model => " + modStr)
	res = MetadataAPIImpl.AddModel(ModelType.JAVA, // modelType
				       modStr, // input
				       userid,   // optUserid
				       Some("testTenantId"),  // tenantId
				       Some("kamanja.helloworldmodel"),   // optModelName
				       None,   // optVersion
				       None,   // optMsgConsumed
				       None,   // optMsgVersion
				       //Some("system.helloworld_msg_output_def") // optMsgProduced
				       None,
				       None
				     )
	res should include regex ("\"Status Code\" : 0")

	And("GetModelDef API to fetch the model that was just added")
	// Unable to use fileName to identify the name of the object
	// Use this function to extract the name of the model
	var nameSpace = "com.ligadata.kamanja.samples.models"
	var objName = "HelloWorldModel"
	logger.info("ModelName => " + objName)
	var version = "0000000000000000001"
	res = MetadataAPIImpl.GetModelDef(nameSpace, objName, "XML", version, userid)
	res should include regex ("\"Status Code\" : 0")

	modStr = modStr.replaceFirst("0.0.1", "0.0.2")
	assert(modStr.indexOf("0.0.2") >= 0)

	logger.info("modStr => " + modStr);

	// Update the model and catch the exception
	res = MetadataAPIImpl.UpdateModel(ModelType.JAVA, // modelType
					 modStr, // input
					 userid,   // optUserid
					 Some("testTenantId"),  // tenantId
					 Some("kamanja.helloworldmodel"),   // optModelName
					 None,   // optVersion
					 None,   // optMsgConsumed
					 None,   // optMsgVersion
					 None,
					 None
				       )
	res should include regex ("\"Status Code\" : 0")

	nameSpace = "com.ligadata.kamanja.samples.models"
	objName = "HelloWorldModel"
	logger.info("ModelName => " + objName)
	version = "0000000000000000002"
	res = MetadataAPIImpl.GetModelDef(nameSpace, objName, "XML", version, userid)
	res should include regex ("\"Status Code\" : 0")
      })
    }

    it("Add com.ligadata.kamanja.samples.models.HelloWorld Java Model where packageName is different from className") {
      And("Check whether MODEL_FILES_DIR defined as property")
      dirName = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("MODEL_FILES_DIR")
      assert(null != dirName)

      And("Check Directory Path")
      iFile = new File(dirName)
      assert(true == iFile.exists)

      And("Check whether " + dirName + " is a directory ")
      assert(true == iFile.isDirectory)

      And("Make sure there are few java model files in " + dirName);
      val modFiles = new java.io.File(dirName).listFiles.filter(_.getName.endsWith(".java"))
      assert(0 != modFiles.length)

      // Scala Models
      fileList = List("HelloWorld.java")
      fileList.foreach(f1 => {
	And("Add the Model From " + f1)
	And("Make Sure " + f1 + " exist")
	var exists = false
	var file: java.io.File = null
	breakable {
	  modFiles.foreach(f2 => {
	    if (f2.getName() == f1) {
	      exists = true
	      file = f2
	      break
	    }
	  })
	}
	assert(true == exists)

	And("Call AddModel MetadataAPI Function to add Model from " + file.getPath)
	var modStr = Source.fromFile(file).mkString
	logger.info("scala model => " + modStr)

	// Change the packageName so it doesn't contain the nameSpace of
	// modelName(as defined by getModelName() ) function of java model

	modStr = modStr.replaceFirst("com.ligadata.kamanja.samples.models", "com.ligadata1.kamanja.samples.models")
	assert(modStr.indexOf("com.ligadata1.kamanja.samples.models") >= 0)

	logger.info("modStr => " + modStr);

	// Add the model and catch the exception
	res = MetadataAPIImpl.AddModel(ModelType.JAVA, // modelType
					 modStr, // input
					 userid,   // optUserid
					 Some("testTenantId"),  // tenantId
					 Some("kamanja.helloworldmodel"),   // optModelName
					 None,   // optVersion
					 None,   // optMsgConsumed
					 None,   // optMsgVersion
					 None,
					 None
				       )
	res should include regex ("\"Status Code\" : -1")
      })
    }


    it("Update com.ligadata.kamanja.samples.models.HelloWorld Java Model where packageName is different from className") {
      And("Check whether MODEL_FILES_DIR defined as property")
      dirName = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("MODEL_FILES_DIR")
      assert(null != dirName)

      And("Check Directory Path")
      iFile = new File(dirName)
      assert(true == iFile.exists)

      And("Check whether " + dirName + " is a directory ")
      assert(true == iFile.isDirectory)

      And("Make sure there are few java model files in " + dirName);
      val modFiles = new java.io.File(dirName).listFiles.filter(_.getName.endsWith(".java"))
      assert(0 != modFiles.length)

      // Scala Models
      fileList = List("HelloWorld.java")
      fileList.foreach(f1 => {
	And("Add the Model From " + f1)
	And("Make Sure " + f1 + " exist")
	var exists = false
	var file: java.io.File = null
	breakable {
	  modFiles.foreach(f2 => {
	    if (f2.getName() == f1) {
	      exists = true
	      file = f2
	      break
	    }
	  })
	}
	assert(true == exists)

	And("Call AddModel MetadataAPI Function to add Model from " + file.getPath)
	var modStr = Source.fromFile(file).mkString
	logger.info("scala model => " + modStr)

	// Change the packageName so it doesn't contain the nameSpace of
	// modelName(as defined by getModelName() ) function of java model

	modStr = modStr.replaceFirst("com.ligadata.kamanja.samples.models", "com.ligadata1.kamanja.samples.models")
	assert(modStr.indexOf("com.ligadata1.kamanja.samples.models") >= 0)

	modStr = modStr.replaceFirst("0.0.1", "0.0.2")
	assert(modStr.indexOf("0.0.2") >= 0)

	logger.info("modStr => " + modStr);

	// Update the model and catch the exception
	res = MetadataAPIImpl.UpdateModel(ModelType.JAVA, // modelType
					 modStr, // input
					 userid,   // optUserid
					 Some("testTenantId"),  // tenantId
					 Some("kamanja.helloworldmodel"),   // optModelName
					 None,   // optVersion
					 None,   // optMsgConsumed
					 None,   // optMsgVersion
					 None,
					 None
				       )
	res should include regex ("\"Status Code\" : -1")
      })
    }

    it("Add System.HelloWorld Java Models") {
      And("Check whether MODEL_FILES_DIR defined as property")
      dirName = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("MODEL_FILES_DIR")
      assert(null != dirName)

      And("Check Directory Path")
      iFile = new File(dirName)
      assert(true == iFile.exists)

      And("Check whether " + dirName + " is a directory ")
      assert(true == iFile.isDirectory)

      And("Make sure there are few java model files in " + dirName);
      val modFiles = new java.io.File(dirName).listFiles.filter(_.getName.endsWith(".java"))
      assert(0 != modFiles.length)

      // Scala Models
      fileList = List("System.HelloWorld.java")
      fileList.foreach(f1 => {
	And("Add the Model From " + f1)
	And("Make Sure " + f1 + " exist")
	var exists = false
	var file: java.io.File = null
	breakable {
	  modFiles.foreach(f2 => {
	    if (f2.getName() == f1) {
	      exists = true
	      file = f2
	      break
	    }
	  })
	}
	assert(true == exists)

	And("Call AddModel MetadataAPI Function to add Model from " + file.getPath)
	var modStr = Source.fromFile(file).mkString
	logger.info("scala model => " + modStr)
	res = MetadataAPIImpl.AddModel(ModelType.JAVA, // modelType
				       modStr, // input
				       userid,   // optUserid
				       Some("testTenantId"),  // tenantId
				       Some("kamanja.helloworldmodel"),   // optModelName
				       None,   // optVersion
				       None,   // optMsgConsumed
				       None,   // optMsgVersion
				       //Some("system.helloworld_msg_output_def") // optMsgProduced
				       None,
				       None
				     )
	res should include regex ("\"Status Code\" : 0")

	And("GetModelDef API to fetch the model that was just added")
	// Unable to use fileName to identify the name of the object
	// Use this function to extract the name of the model
	var nameSpace = "System"
	var objName = "HelloWorldModel"
	logger.info("ModelName => " + objName)
	var version = "0000000000000000001"
	res = MetadataAPIImpl.GetModelDef(nameSpace, objName, "XML", version, userid)
	res should include regex ("\"Status Code\" : 0")
      })
    }

    it("Add System.HelloWorld Java Model where packageName is different from className") {
      And("Check whether MODEL_FILES_DIR defined as property")
      dirName = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("MODEL_FILES_DIR")
      assert(null != dirName)

      And("Check Directory Path")
      iFile = new File(dirName)
      assert(true == iFile.exists)

      And("Check whether " + dirName + " is a directory ")
      assert(true == iFile.isDirectory)

      And("Make sure there are few java model files in " + dirName);
      val modFiles = new java.io.File(dirName).listFiles.filter(_.getName.endsWith(".java"))
      assert(0 != modFiles.length)

      // Scala Models
      fileList = List("System.HelloWorld.java")
      fileList.foreach(f1 => {
	And("Add the Model From " + f1)
	And("Make Sure " + f1 + " exist")
	var exists = false
	var file: java.io.File = null
	breakable {
	  modFiles.foreach(f2 => {
	    if (f2.getName() == f1) {
	      exists = true
	      file = f2
	      break
	    }
	  })
	}
	assert(true == exists)

	And("Call AddModel MetadataAPI Function to add Model from " + file.getPath)
	var modStr = Source.fromFile(file).mkString
	logger.info("scala model => " + modStr)

	// Change the packageName so it doesn't contain the nameSpace of
	// modelName(as defined by getModelName() ) function of java model

	modStr = modStr.replaceFirst("System", "system1")
	assert(modStr.indexOf("system1") >= 0)

	logger.info("modStr => " + modStr);

	// Add the model and catch the exception
	res = MetadataAPIImpl.AddModel(ModelType.JAVA, // modelType
					 modStr, // input
					 userid,   // optUserid
					 Some("testTenantId"),  // tenantId
					 Some("kamanja.helloworldmodel"),   // optModelName
					 None,   // optVersion
					 None,   // optMsgConsumed
					 None,   // optMsgVersion
					 None,
					 None
				       )
	res should include regex ("\"Status Code\" : -1")
      })
    }

    def getCCParams(cc: Product): scala.collection.mutable.Map[String, Any] = {
      val values = cc.productIterator
      val m = cc.getClass.getDeclaredFields.map(_.getName -> values.next).toMap
      scala.collection.mutable.Map(m.toSeq: _*)
    }

    it("Load Adapter Message Bindings") {

      And("Check whether CONFIG_FILES_DIR defined as property")
      dirName = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("CONFIG_FILES_DIR")
      assert(null != dirName)

      And("Check Directory Path")
      iFile = new File(dirName)
      assert(true == iFile.exists)

      And("Check whether " + dirName + " is a directory ")
      assert(true == iFile.isDirectory)

      And("Make sure there are few JSON config files in " + dirName);
      val cfgFiles = new java.io.File(dirName).listFiles.filter(_.getName.endsWith(".json"))
      assert(0 != cfgFiles.length)

      fileList = List("HelloWorld_Adapter_Binding.json","SystemMsgs_Adapter_Binding.json")

      fileList.foreach(f1 => {
        And("Add the Config From " + f1)
        And("Make Sure " + f1 + " exist")
        var exists = false
        var file: java.io.File = null
        breakable {
          cfgFiles.foreach(f2 => {
            if (f2.getName() == f1) {
              exists = true
              file = f2
              break
            }
          })
        }
        assert(true == exists)
        And("AddConfig  from " + file.getPath)

        res = AdapterMessageBindingService.addFromInlineAdapterMessageBinding(file.getPath, userid)
        res should include regex ("\"Status Code\" : 0")

      })
    }
  }

  override def afterAll = {
    var file = new java.io.File("logs")
    if (file.exists()) {
      TestUtils.deleteFile(file)
    }

    logger.info("Drop dbstore")
    val db = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("DATABASE")
    assert(null != db)
    DropDbStore
    if( MetadataAPIImpl.GetAuditObj != null ){
      MetadataAPIImpl.GetAuditObj.dropStore
      MetadataAPIImpl.SetAuditObj(null)
      val pFile = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("AUDIT_PARMS")
      if( pFile != null ){
	TestUtils.deleteFile(pFile)
      }
    }
    MetadataAPIImpl.shutdown
  }

  if (zkServer != null) {
    zkServer.instance.shutdown
  }
}
