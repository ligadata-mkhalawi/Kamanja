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
import com.ligadata.MetadataAPI._
import com.ligadata.kamanja.metadata._
import com.ligadata.kamanja.metadata.MdMgr._
import com.ligadata.Utils._

import util.control.Breaks._
import scala.io._
import java.util.Date
import java.io._

import com.ligadata.MetadataAPI.test.{MetadataAPIProperties, MetadataDefaults}

import sys.process._
import org.apache.logging.log4j._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import com.ligadata.Serialize._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{read, write, writePretty}
import com.ligadata.kamanja.metadataload.MetadataLoad
import com.ligadata.test.utils._
import com.ligadata.test.configuration.cluster.adapters.interfaces._
import com.ligadata.test.embedded.zookeeper._

class AddAutoMPGPmmlModelSpec extends FunSpec with LocalTestFixtures with BeforeAndAfter with BeforeAndAfterAll with GivenWhenThen with Matchers {
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

  implicit val formats = Serialization.formats(
    ShortTypeHints(
      List(
        classOf[adapterMessageBinding]
      )
    )
  )

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

      //logger.info("Startup embedded zooKeeper ")
      zkServer.startup

      logger.info("Initialize MetadataManager")
      val h2dbStore = new H2DBStore
      mdMan.initMetadataCfg(new MetadataAPIProperties(h2dbStore.name, h2dbStore.connectionMode, ConfigDefaults.storageDirectory, zkConnStr = zkServer.getConnection))
      //mdMan.config.classPath = ConfigDefaults.metadataClasspath

      logger.info("Initialize MdMgr")
      MdMgr.GetMdMgr.truncate
      val mdLoader = new MetadataLoad(MdMgr.mdMgr, "", "", "", "")
      mdLoader.initialize

      logger.info("Initialize zooKeeper connection")
      MetadataAPIImpl.initZkListeners(false)

      logger.info("Initialize datastore")
      var tmpJarPaths = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_PATHS")
      logger.info("jarPaths => " + tmpJarPaths)
      val jarPaths = if (tmpJarPaths != null) tmpJarPaths.split(",").toSet else scala.collection.immutable.Set[String]()
      MetadataAPIImpl.OpenDbStore(jarPaths, MetadataAPIImpl.GetMetadataAPIConfig.getProperty("METADATA_DATASTORE"))

      var jp = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_PATHS")
      logger.info("jarPaths => " + jp)

      logger.info("Create Metadata Tables..")
      MetadataAPIImpl.CreateMetadataTables

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

  def getCCParams(cc: Product): scala.collection.mutable.Map[String, Any] = {
    val values = cc.productIterator
    val m = cc.getClass.getDeclaredFields.map(_.getName -> values.next).toMap
    scala.collection.mutable.Map(m.toSeq: _*)
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

    // CRUD operations on message objects
    it("Add Messages ..") {
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

      fileList = List("AutoMPG.json", "AutoMPG_OutputMsg.json")
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
        res = MetadataAPIImpl.AddMessage(msgStr, "JSON", None, tenantId,None)
        res should include regex ("\"Status Code\" : 0")

      })
    }

    it("Add Cluster Config") {
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

      fileList = List("ClusterConfig.json")
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
        assert(adapters.size == 10)
      })
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

      fileList = List("AutoMPG_Adapter_Binding.json")

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
        var ambsAsJson = Source.fromFile(file).mkString

        // parse adapter bindings json
        val ambs1 = parse(ambsAsJson).extract[Array[adapterMessageBinding]]
        val ambsAsJson1 = writePretty(ambs1)
        logger.info(ambsAsJson1)

        val ambsMap: Array[scala.collection.mutable.Map[String, Any]] = ambs1.map(amb => {
          val ambMap = getCCParams(amb); ambMap
        })
        ambsMap.toList.foreach(ambMap => {
          logger.info("ambMap => " + ambMap)
        })

        val cnt = ambsMap.size

        res = AdapterMessageBindingUtils.AddAdapterMessageBinding(ambsMap.toList, userid)
        res should include regex ("\"Status Code\" : 0")

        val bindings = AdapterMessageBindingUtils.ListAllAdapterMessageBindings
        assert(bindings.size == cnt)

      })
    }

    // CRUD operations on Model objects
    ignore("Add PMML Models") {
      And("Check whether MODEL_FILES_DIR defined as property")
      dirName = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("MODEL_FILES_DIR")
      assert(null != dirName)

      And("Check Directory Path")
      iFile = new File(dirName)
      assert(true == iFile.exists)

      And("Check whether " + dirName + " is a directory ")
      assert(true == iFile.isDirectory)

      And("Make sure there are few pmml model files in " + dirName);
      val modFiles = new java.io.File(dirName).listFiles.filter(_.getName.endsWith(".xml"))
      assert(0 != modFiles.length)

      // KPMML Models
      fileList = List("AutoMPG_rpart_PMML.xml")
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

        And("AddModel  to add PMML Model with a output message from " + file.getPath)
        var modStr = Source.fromFile(file).mkString
        res = MetadataAPIImpl.AddModel(ModelType.PMML, // modelType
          modStr, // input
          userid, // optUserid
          Some("testTenantId"), // tenantId
          None, // optModelName
          None, // optVersion
          Some("com.ligadata.kamanja.samples.messages.AutoMPGData"), // optMsgConsumed
          None, // optMsgVersion
          Some("com.ligadata.kamanja.samples.messages.OutAutoMPGData"), // optMsgProduced
	  None // pStr
        )
        res should include regex ("\"Status Code\" : 0")


        And("GetModelDef API to fetch the model that was just added")
        // Unable to use fileName to identify the name of the object
        // Use this function to extract the name of the model
        var nameSpace = "system"
        var objName = extractNameFromPMML(modStr).toLowerCase
        logger.info("ModelName => " + objName)
        assert(objName != "unknownModel")

        var version = "0000000000001000000"
        res = MetadataAPIImpl.GetModelDef(nameSpace, objName, "XML", version, None)
        res should include regex ("\"Status Code\" : 0")

        val msgName = objName + "_outputmsg"
        version = "000000000000000001"
        res = MetadataAPIImpl.GetMessageDef(nameSpace, msgName, "JSON", version, None, None)
        res should include regex ("\"Status Code\" : 0")

        val modDefs = MdMgr.GetMdMgr.Models(nameSpace, objName, true, true)
        assert(modDefs != None)

        val models = modDefs.get.toArray
        assert(models.length == 1)

        var omsgs = models(0).outputMsgs
        assert(omsgs.length == 1)
        val msgFullName = nameSpace + "." + msgName
        assert(omsgs(0) == msgFullName)
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
      if( pFile != null ) {
        TestUtils.deleteFile(pFile)
      }
    }
    MetadataAPIImpl.shutdown
  }

  if (zkServer != null) {
    zkServer.shutdown
  }
}
