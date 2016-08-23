package com.ligadata.MetadataAPI.test


import java.io.File

import com.ligadata.MetadataAPI.MetadataAPI.ModelType
import com.ligadata.MetadataAPI.MetadataAPI.ModelType._
import com.ligadata.MetadataAPI.{AdapterMessageBindingUtils, ApiResult, MetadataAPIImpl}
import com.ligadata.Serialize.SerializerManager
import com.ligadata.Exceptions.AlreadyExistsException
import com.ligadata.MetadataAPI.Utility.AdapterMessageBindingService
import com.ligadata.test.configuration.cluster.Cluster
import com.ligadata.test.configuration.cluster.adapters.StorageConfiguration
import com.ligadata.test.configuration.cluster.adapters.interfaces.KafkaIOAdapter
import com.ligadata.test.utils.{Globals, KamanjaTestLogger, TestUtils}
import com.ligadata.kamanja.metadata.{AdapterInfo, MdMgr, ModelDef}
import com.ligadata.keyvaluestore.KeyValueManager

import scala.collection.mutable
import scala.io.Source
import org.apache.commons.io.FilenameUtils._
import org.json4s._
import org.json4s.native.JsonMethods._

case class MetadataAPIProperties(var database: String,
                                 var connectionMode:String,
                                 var databaseHost: String,
                                 var databaseSchema: String = "kamanja",
                                 var classPath: String = MetadataDefaults.metadataClasspath,
                                 var systemJarPath: String = "",
                                 var appJarPath: String = "",
                                 var znodeBasePath: String = "/kamanja",
                                 var zkConnStr: String = "localhost:2181",
                                 var modelExecLog: String = "true",
                                 var adapterSpecificConfig: String = "",
                                 var serviceHost: String = "localhost",
                                 var servicePort: String = "8081"
                                )

case class MetadataManagerException(message: String) extends Exception(message)

class MetadataDataStore(storeType: String, connectionMode: String, schemaName: String, location: String) {
  override def toString = "{\"StoreType\": \"" + storeType + "\", \"connectionMode\": \"" + connectionMode + "\", \"SchemaName\": \"" + schemaName + "\", \"Location\": \"" + location + "\"}"
}

class MetadataManager extends KamanjaTestLogger {
  private lazy val metadataDir = new File(getClass.getResource("/Metadata").getPath)
  private val scalaHome = System.getenv("SCALA_HOME")
  private val javaHome = System.getenv("JAVA_HOME")
  private val userId = "metadataapi"

  if(scalaHome == null || javaHome == null) {
    throw new MetadataManagerException("Environment Variable SCALA_HOME or JAVA_HOME not set")
  }

  /// Init is kept separate rather than as a part of the construction of the class in order to prevent execution of certain operations too soon.
  def initMetadataCfg(config: MetadataAPIProperties): Unit = {
    if(scalaHome != null)
      MetadataAPIImpl.GetMetadataAPIConfig.setProperty("SCALA_HOME", scalaHome)
    else {
      throw new MetadataManagerException("Failed to retrieve environmental variable 'SCALA_HOME'. You must set this variable in order to run tests.")
    }

    if(javaHome != null)
      MetadataAPIImpl.GetMetadataAPIConfig.setProperty("JAVA_HOME", javaHome)
    else {
      throw new MetadataManagerException("Failed to retrieve environmental variable 'JAVA_HOME'. You must set this variable in order to run tests.")
    }

    if(config.systemJarPath == "") {
      config.systemJarPath = this.getClass.getResource("/jars/lib/system").getPath
    }

    if(config.appJarPath == "") {
      config.appJarPath = this.getClass.getResource("/jars/lib/aplication").getPath
    }

    val mdDataStore = new MetadataDataStore(config.database, config.connectionMode, config.databaseSchema, config.databaseHost)

    MetadataAPIImpl.metadataAPIConfig.setProperty("JAR_PATHS",  config.systemJarPath + "," + config.appJarPath)
    MetadataAPIImpl.metadataAPIConfig.setProperty("METADATA_DATASTORE", mdDataStore.toString)

    try {
      logger.info("[Metadata Manager]: Initializing Metadata Configuration...")

      //MetadataAPIImpl.InitMdMgr(MdMgr.mdMgr, config.database, config.databaseHost, config.databaseSchema, config.databaseHost, config.adapterSpecificConfig)
      //When restarting in-proc, MdMgr somehow causes a NullPointerException when InitMdMgr is called again. Creating a new MdMgr before initializing to avoid that.
      MdMgr.mdMgr = new MdMgr
      MetadataAPIImpl.InitMdMgr(MdMgr.mdMgr, MetadataAPIImpl.metadataAPIConfig.get("JAR_PATHS").toString, MetadataAPIImpl.metadataAPIConfig.get("METADATA_DATASTORE").toString)

    }
    catch {
      case e: AlreadyExistsException =>
      case e: Exception => throw new MetadataManagerException("[Metadata Manager]: Failed to initialize MetadataAPI with the following exception:\n" + e)
    }
    MetadataAPIImpl.metadataAPIConfig.setProperty("NODE_ID", "1")
    MetadataAPIImpl.metadataAPIConfig.setProperty("ROOT_DIR", "")
    MetadataAPIImpl.metadataAPIConfig.setProperty("GIT_ROOT", "")
    MetadataAPIImpl.metadataAPIConfig.setProperty("DATABASE", config.database)
    MetadataAPIImpl.metadataAPIConfig.setProperty("JAR_TARGET_DIR", config.appJarPath)
    MetadataAPIImpl.metadataAPIConfig.setProperty("MANIFEST_PATH", metadataDir.getAbsoluteFile + "/manifest.mf")
    MetadataAPIImpl.metadataAPIConfig.setProperty("CLASSPATH", config.classPath)
    MetadataAPIImpl.metadataAPIConfig.setProperty("NOTIFY_ENGINE", "NO")
    MetadataAPIImpl.metadataAPIConfig.setProperty("ZNODE_PATH", config.znodeBasePath)
    MetadataAPIImpl.metadataAPIConfig.setProperty("ZOOKEEPER_CONNECT_STRING", config.zkConnStr)
    MetadataAPIImpl.metadataAPIConfig.setProperty("COMPILER_WORK_DIR", TestUtils.constructTempDir("workingdir").getPath)//getClass.getResource("/jars/lib/workingdir").getPath)
    MetadataAPIImpl.metadataAPIConfig.setProperty("API_LEADER_SELECTION_ZK_NODE", config.znodeBasePath)
    MetadataAPIImpl.metadataAPIConfig.setProperty("MODEL_EXEC_LOG", config.modelExecLog)
    MetadataAPIImpl.metadataAPIConfig.setProperty("SECURITY_IMPL_JAR", config.systemJarPath + "/simpleapacheshiroadapter_2.11-1.0.jar")
    MetadataAPIImpl.metadataAPIConfig.setProperty("SECURITY_IMPL_CLASS", "com.ligadata.Security.SimpleApacheShiroAdapter")
    MetadataAPIImpl.metadataAPIConfig.setProperty("DO_AUTH", "NO")
    MetadataAPIImpl.metadataAPIConfig.setProperty("AUDIT_IMPL_JAR", config.systemJarPath + "/auditadapters_2.11-1.0.jar")
    MetadataAPIImpl.metadataAPIConfig.setProperty("DO_AUDIT", "NO")
    MetadataAPIImpl.metadataAPIConfig.setProperty("ADAPTER_SPECIFIC_CONFIG", config.adapterSpecificConfig)
    MetadataAPIImpl.metadataAPIConfig.setProperty("SERVICE_HOST", config.serviceHost)
    MetadataAPIImpl.metadataAPIConfig.setProperty("SERVICE_PORT", config.servicePort)

    //MetadataAPIImpl.metadataAPIConfig.setProperty("SSL_PASSWD", config.sslPassword)

    // Metadata Directory Properties. These are primarily used for certain configuration tests to determine that the property has been set.
    config.database match {
      case "cassandra" => {
        MetadataAPIImpl.metadataAPIConfig.setProperty("AUDIT_IMPL_CLASS", "com.ligadata.audit.adapters.AuditCassandraAdapter")
      }
      case "hbase" => {
        MetadataAPIImpl.metadataAPIConfig.setProperty("AUDIT_IMPL_CLASS", "com.ligadata.audit.adapters.AuditHBaseAdapter")
      }
      case "hashmap" => {
        MetadataAPIImpl.metadataAPIConfig.setProperty("AUDIT_IMPL_CLASS", "com.ligadata.audit.adapters.AuditHashMapAdapter")
      }
      case "treemap" => {
        MetadataAPIImpl.metadataAPIConfig.setProperty("AUDIT_IMPL_CLASS", "com.ligadata.audit.adapters.AuditHashMapAdapter")
      }
      case "sqlserver" | "mysql" | "h2db" => {
        MetadataAPIImpl.metadataAPIConfig.setProperty("AUDIT_IMPL_CLASS", "com.ligadata.audit.adapters.AuditCassandraAdapter")
      }
      case _ => {
        throw new MetadataManagerException("Unknown DataStoreType: " + config.database)
      }
    }
    MetadataAPIImpl.metadataAPIConfig.setProperty("SSL_CERTIFICATE",metadataDir.getAbsoluteFile + "/certs/keystore.jks")
    MetadataAPIImpl.metadataAPIConfig.setProperty("CONTAINER_FILES_DIR",metadataDir.getAbsoluteFile + "/container")
    MetadataAPIImpl.metadataAPIConfig.setProperty("MESSAGE_FILES_DIR",metadataDir.getAbsoluteFile + "/message")
    MetadataAPIImpl.metadataAPIConfig.setProperty("MODEL_FILES_DIR",metadataDir.getAbsoluteFile + "/model")
    MetadataAPIImpl.metadataAPIConfig.setProperty("FUNCTION_FILES_DIR",metadataDir.getAbsoluteFile + "/function")
    MetadataAPIImpl.metadataAPIConfig.setProperty("CONCEPT_FILES_DIR",metadataDir.getAbsoluteFile + "/concept")
    MetadataAPIImpl.metadataAPIConfig.setProperty("TYPE_FILES_DIR",metadataDir.getAbsoluteFile + "/type")
    MetadataAPIImpl.metadataAPIConfig.setProperty("CONFIG_FILES_DIR",metadataDir.getAbsoluteFile + "/config")
  }

  def addBindings(filepath: String): Int = {
    val file = new File(filepath)

    logger.info(s"[Metadata Manager]: Adding message bindings from file $filepath")
    if(!file.exists())
      throw new MetadataManagerException(s"[Metadata Manager]: The file ${file.getAbsolutePath} does not exist")

    val source = Source.fromFile(file)
    val mdString = source.mkString
    source.close()

    return validateApiResults(AdapterMessageBindingService.addFromInlineAdapterMessageBinding(mdString, Some(userId)))
  }

  def add(mdType: String,
          filepath: String,
          tenantId: Option[String] = None,
          modelType: Option[ModelType] = None,
          modelCfg: Option[String] = None,
          modelName: Option[String] = None,
          modelVersion: Option[String] = None,
          msgConsumed: Option[String] = None,
          msgVersion: Option[String] = None,
          msgProduced: Option[String] = None): Int = {

    var result: ApiResult = null
    val file = new File(filepath)

    logger.info(s"[Metadata Manager]: Adding $mdType from file $filepath")
    if(!file.exists())
      throw new MetadataManagerException(s"[Metadata Manager]: The file '${file.getAbsoluteFile}' does not exist")

    val source = Source.fromFile(file)
    val mdString = source.mkString
    source.close()
    mdType.toLowerCase match {
      case "message" => result = parseApiResult(MetadataAPIImpl.AddMessage(mdString, "JSON", Some(userId), tenantId, None))
      case "container" => result = parseApiResult(MetadataAPIImpl.AddContainer(mdString, "JSON", Some(userId), tenantId, None))
      case "model" => {
        modelType match {
          case None =>
            throw new MetadataManagerException("[Metadata Manager]: Adding a model requires the model type to be specified")
          case Some(ModelType.SCALA) | Some(ModelType.JAVA) =>
            result = parseApiResult(MetadataAPIImpl.AddModel(modelType.get, mdString, Some(userId), tenantId, Some(userId + "." + modelCfg.get), modelVersion, msgConsumed, msgVersion, msgProduced, None))
          case Some(_) =>
            result = parseApiResult(MetadataAPIImpl.AddModel(modelType.get, mdString, Some(userId), tenantId, modelName, modelVersion, msgConsumed, msgVersion, msgProduced, None))
            val model: Option[ModelDef] = MdMgr.GetMdMgr.Model(s"$modelName", MdMgr.ConvertVersionToLong(modelVersion.getOrElse("-1")), true)
        }
      }
      case "function" => result = parseApiResult(MetadataAPIImpl.AddFunctions(mdString, "json", Some(userId)))
      case "jar" => result = parseApiResult(MetadataAPIImpl.UploadJar(filepath))
      case "compileconfig" => result = parseApiResult(MetadataAPIImpl.UploadModelsConfig(mdString, Some(userId), "", false))
    }
    logger.info(s"[Metadata Manager]: API Result =>\n${result.toString}")
    result.statusCode
  }

  def remove(mdType: String, namespace: String, name: String, version: String): Int = {
    var result: ApiResult = null
    logger.info("[Metadata Manager]: Removing " + mdType + s" $namespace.$name.$version")
    mdType.toLowerCase match {
      case "message" => result = parseApiResult(MetadataAPIImpl.RemoveMessage(namespace, name, version.toLong, Some(userId)))
      case "container" => result = parseApiResult(MetadataAPIImpl.RemoveContainer(namespace, name, version.toLong, Some(userId)))
      case "model" => {
        result = parseApiResult(MetadataAPIImpl.RemoveModel(s"$namespace.$name", version, Some(userId)))
        val model: Option[ModelDef] = MdMgr.GetMdMgr.Model(s"$namespace.$name", MdMgr.ConvertVersionToLong(version), true)
      }
      case "function" => result = parseApiResult(MetadataAPIImpl.RemoveFunction(namespace, name, version.toLong, Some(userId)))
      case _ => throw new MetadataManagerException(s"Metadata Type $mdType is an invalid type.")
    }
    logger.info("[Metadata Manager]: API Result =>")
    logger.info(result.toString)
    result.statusCode
  }

  def get(mdType: String, namespace: String, name: String, version: String): Int = {
    var result: ApiResult = null
    logger.info(s"[Metadata Manager]: Getting $mdType $namespace.$name.$version")
    mdType.toLowerCase match {
      case "message" => result = parseApiResult(MetadataAPIImpl.GetMessageDef(namespace + "." + name, "JSON", version, Some(userId), None))
      case "container" => result = parseApiResult(MetadataAPIImpl.GetContainerDef(namespace + "." + name, "JSON", version, Some(userId), None))
      case "model" => result = parseApiResult(MetadataAPIImpl.GetModelDef(namespace + "." + name, "JSON", version, Some(userId)))
      case "function" => result = parseApiResult(MetadataAPIImpl.GetFunctionDef(namespace, name, "JSON", version, Some(userId)))
      case _ => throw new MetadataManagerException(s"Metadata Type $mdType is an invalid type.")
    }
    logger.info("[Metadata Manager]: API Result =>")
    logger.info(result.toString)
    result.statusCode
  }

  def update(mdType: String,
             filepath: String,
             tenantId: Option[String] = None,
             modelName: Option[String] = None,
             modelType: Option[ModelType] = None,
             modelVersion: Option[String] = None,
             modelVersionBeingUpdated: Option[String] = None,
             msgProduced: Option[String] = None
            ): Int = {

    var result: ApiResult = null
    val file: File = new File(filepath)

    logger.info(s"[Metadata Manager]: Updating $mdType from file '$filepath'")
    if (!file.exists())
      throw new MetadataManagerException("[Metadata Manager]: The file " + file.getAbsoluteFile + " does not exist")

    val source = Source.fromFile(file)
    val mdString = source.mkString
    source.close()
    mdType.toLowerCase match {
      case "message" => result = parseApiResult(MetadataAPIImpl.UpdateMessage(mdString, "JSON", Some(userId), tenantId, None))
      case "container" => result = parseApiResult(MetadataAPIImpl.UpdateContainer(mdString, "JSON", Some(userId), tenantId, None))
      case "model" => {
        modelType match {
          case None =>
            throw new MetadataManagerException("[Metadata Manager]: Adding a model requires the model type to be specified")
          case Some(_) =>
            result = parseApiResult(MetadataAPIImpl.UpdateModel(modelType.get, mdString, Some(userId), tenantId, modelName, modelVersion, modelVersionBeingUpdated, msgProduced, None))
        }
      }
      case "function" => result = parseApiResult(MetadataAPIImpl.UpdateFunctions(mdString, "JSON", Some(userId)))
      case _ => throw new MetadataManagerException(s"Metadata Type $mdType is an invalid type.")
    }
    logger.info("[Metadata Manager]: API Result =>")
    logger.info(result.toString)
    result.statusCode
  }

  def addConfig(cluster: Cluster): Int = {
    try {
      val result = parseApiResult(MetadataAPIImpl.UploadConfig(cluster.toString, Some(userId), ""))
      logger.info("[Metadata Manager]: Upload Cluster Configuration API Result =>\n")
      logger.info(result.toString)
      result.statusCode
    }
    catch {
      case e: Exception => {
        logger.error("[Metadata Manager]: Failed to add Cluster Configuration", e)
        e.printStackTrace()
        -1
      }
    }
  }

  def parseApiResult(apiResult: String): ApiResult = {
    implicit val formats = org.json4s.DefaultFormats
    val json = parse(apiResult)
    val statusCode = (json \\ "Status Code").values.toString.toInt
    val functionName = (json \\ "Function Name").values.toString
    val resultData = (json \\ "Results Data").values.toString
    val description = (json \\ "Result Description").values.toString

    new ApiResult(statusCode, functionName, resultData, description)
  }

  /// Returns 0 if all results are 0, otherwise returns the non-zero code
  def validateApiResults(apiResults: String): Int = {
    logger.info("[Metadata Manager]: Validating API Results =>\n" + apiResults)
    val json = parse(apiResults)
    val results = (json \\ "Status Code")

    val codes: List[BigInt] = for {
      JObject(result) <- json
      JField("Status Code", JInt(statusCode)) <- result
    } yield statusCode

    codes map { code =>
      if (code != 0) {
        logger.error(s"[Metadata Manager]: An APIResult returned with Status Code $code")
        return code.toInt
      }
    }
    return 0
  }

  def setSSLPassword(password:String): Unit ={
    MetadataAPIImpl.setSSLCertificatePasswd(password)
  }

  def shutdown: Unit = {
    MetadataAPIImpl.CloseZKSession
    MetadataAPIImpl.shutdown
    MetadataAPIImpl.isInitilized = false
  }
}
