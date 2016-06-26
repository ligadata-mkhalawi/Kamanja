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

import com.ligadata.kamanja.metadataload.MetadataLoad
import com.ligadata.Exceptions._

class UpdateModelContainersSpec extends FunSpec with LocalTestFixtures with BeforeAndAfter with BeforeAndAfterAll with GivenWhenThen {
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

      logger.info("Load All objects into cache")
      MetadataAPIImpl.LoadAllObjectsIntoCache(false)

      // The above call is resetting JAR_PATHS based on nodeId( node-specific configuration)
      // This is causing unit-tests to fail
      // restore JAR_PATHS value
      MetadataAPIImpl.GetMetadataAPIConfig.setProperty("JAR_PATHS", tmpJarPaths)
      jp = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_PATHS")
      logger.info("jarPaths => " + jp)

      logger.info("Initialize security adapter")
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


  describe("Sanity checks on environment") {

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

    it("Manipulate Existing Models") {

      And("GetModelDef API to fetch the model that was just added")
      // Unable to use fileName to identify the name of the object
      // Use this function to extract the name of the model
      var nameSpace = "com.ligadata.kamanja.samples.models"
      var objName = "COPDRiskAssessment"
      logger.info("ModelName => " + objName)
      var version = "0000000000000000001"
      res = MetadataAPIImpl.GetModelDef(nameSpace, objName, "XML", version, userid)
      res should include regex ("\"Status Code\" : 0")

      val modDefs = MdMgr.GetMdMgr.Models(nameSpace, objName, true, true)
      assert(modDefs != None)
      
      val models = modDefs.get.toArray
      assert(models.length == 1)
      
      // there should be two sets in this test
      And("Validate contents of inputMsgSets")
      var imsgs = models(0).inputMsgSets
      assert(imsgs.length == 1) // only beneficiary is added

      // The order of msgSets in ModelDef.inputMsgSets may not exactly correspond to the order in model config definition
      imsgs.foreach(msgAttrArrays => {
	//var msgAttrArrays = imsgs(0)
	//assert(msgAttrArrays.length == 1 )
	msgAttrArrays.foreach(msgAttr => {
	  //var msgAttr = msgAttrArrays(0)
	  //assert(msgAttr != null)
	  assert(msgAttr.message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.beneficiary") || 
		 msgAttr.message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.inpatientclaim") || 
		 msgAttr.message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.outpatientclaim") || 
		 msgAttr.message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.hl7") || 
		 msgAttr.message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.coughcodes") || 
		 msgAttr.message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.sputumcodes") || 
		 msgAttr.message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.envcodes") || 	    
		 msgAttr.message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.dyspnoeacodes") || 
		 msgAttr.message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.smokecodes") )
	})
      })
      
      var cfgName = "COPDRiskAssessment"

      And("Validate contents of outputMsgSets")
      val omsgs = models(0).outputMsgs
      assert(omsgs.length == 1)

      var omsg = omsgs(0)
      assert(omsg != null)
      assert(omsg.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.COPDOutputMessage"))

      And("Validate the ModelDef.modelConfig")

      And("Validate dependencies and typeDependencies of modelConfig object")
      var dependencies = MetadataAPIImpl.getModelDependencies(userid.get + "." + cfgName,userid)
      assert(dependencies.length == 0) 

      var msgsAndContainers = MetadataAPIImpl.getModelMessagesContainers(userid.get + "." + cfgName,userid)
      assert(msgsAndContainers.length == 8)

      msgsAndContainers.foreach(message => {
	logger.info("message =>  " + message)
	assert(message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.beneficiary") || 
	       message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.inpatientclaim") || 
	       message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.outpatientclaim") || 
	       message.equalsIgnoreCase("com.ligadata.kamanja.samples.messages.hl7") || 
	       message.equalsIgnoreCase("com.ligadata.kamanja.samples.containers.coughcodes") || 
	       message.equalsIgnoreCase("com.ligadata.kamanja.samples.containers.sputumcodes") || 
	       message.equalsIgnoreCase("com.ligadata.kamanja.samples.containers.envcodes") || 	    
	       message.equalsIgnoreCase("com.ligadata.kamanja.samples.containers.dyspnoeacodes") || 
	       message.equalsIgnoreCase("com.ligadata.kamanja.samples.containers.smokecodes") )

      })
      And("Validate contents of DepContainers")
      val deps = models(0).depContainers
      assert(deps.length == 5)
      deps.foreach(dep => {
	assert(MessageAndContainerUtils.IsContainer(dep))
      })

      var md = models(0)
      var cfgStr = MetadataAPISerialization.serializeObjectToJson(md)
      logger.debug("Parsing ModelConfig : " + md.modelConfig)
      var cfgmap = parse(md.modelConfig).values.asInstanceOf[Map[String, Any]]
      logger.debug("Count of objects in cfgmap : " + cfgmap.keys.size)
      var depContainers = List[String]()
      cfgmap.keys.foreach( key => {
	cfgName = key
	var containers = MessageAndContainerUtils.getContainersFromModelConfig(None,cfgName)
	logger.debug("containers => " + containers)
	depContainers = depContainers ::: containers.toList
      })
      logger.debug("depContainers => " + depContainers)
      md.depContainers = depContainers.toArray
      MetadataAPIImpl.UpdateObjectInDB(md)      
    }
  }

  override def afterAll = {
    logger.info("Truncating dbstore")
    var file = new java.io.File("logs")
    if (file.exists()) {
      TestUtils.deleteFile(file)
    }
    MetadataAPIImpl.shutdown
  }

  if (zkServer != null) {
    zkServer.instance.shutdown
  }
}
