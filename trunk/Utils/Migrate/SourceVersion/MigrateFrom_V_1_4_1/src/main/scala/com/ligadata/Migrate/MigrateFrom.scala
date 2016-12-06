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

package com.ligadata.Migrate

import com.ligadata.Utils._
import com.ligadata.MigrateBase._
import java.io.{DataOutputStream, ByteArrayOutputStream, File, PrintWriter}
import org.apache.logging.log4j._
import scala.collection.mutable.ArrayBuffer
import com.ligadata.KvBase.{Key, Value, TimeRange}
import com.ligadata.StorageBase._
import com.ligadata.keyvaluestore.KeyValueManager
import com.ligadata.Serialize._
import com.ligadata.kamanja.metadata._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import com.ligadata.KamanjaBase._
import scala.util.control.Breaks._
import scala.reflect.runtime.{universe => ru}
import com.ligadata.Exceptions._

import com.ligadata.MetadataAPI._

// import scala.collection.JavaConversions._

class MigrateFrom_V_1_4_1 extends MigratableFrom {

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  private var _sourceInstallPath: String = _
  private var _metadataStoreInfo: String = _
  private var _dataStoreInfo: String = _
  private var _statusStoreInfo: String = _
  private var _sourceReadFailuresFilePath: String = _
  private var _modelConfigStore: DataStore = _
  private var _metadataStore: DataStore = _
  private var _dataStore: DataStore = _
  private var _counterStore: DataStore = _
  private var _flReadFailures: PrintWriter = _
  private var _bInit = false
  private val _kRecordsSeparator = "======================================================================================================================================================="


  object MdResolve extends MdBaseResolveInfo {
    val _messagesAndContainers = scala.collection.mutable.Map[String, MessageContainerObjBase]()
    val _kamanjaLoader = new KamanjaLoaderInfo

    private val _dataFoundButNoMetadata = scala.collection.mutable.Set[String]()

    def DataFoundButNoMetadata = _dataFoundButNoMetadata.toArray

    def AddMessageOrContianer(objType: String, jsonObjMap: Map[String, Any], jarPaths: collection.immutable.Set[String]): Unit = {
      var isOk = true

      try {
        val objNameSpace = jsonObjMap.getOrElse("NameSpace", "").toString.trim()
        val objName = jsonObjMap.getOrElse("Name", "").toString.trim()
        val objVer = jsonObjMap.getOrElse("Version", "").toString.trim()

        val objFullName = (objNameSpace + "." + objName).toLowerCase
        val physicalName = jsonObjMap.getOrElse("PhysicalName", "").toString.trim()

        var isMsg = false
        var isContainer = false

        if (isOk) {
          isOk = LoadJarIfNeeded(jsonObjMap, _kamanjaLoader.loadedJars, _kamanjaLoader.loader, jarPaths)
        }

        if (isOk) {
          var clsName = physicalName
          if (clsName.size > 0 && clsName.charAt(clsName.size - 1) != '$') // if no $ at the end we are taking $
            clsName = clsName + "$"

          if (isMsg == false) {
            // Checking for Message
            try {
              // Convert class name into a class
              var curClz = Class.forName(clsName, true, _kamanjaLoader.loader)

              while (curClz != null && isContainer == false) {
                isContainer = isDerivedFrom(curClz, "com.ligadata.KamanjaBase.BaseContainerObj")
                if (isContainer == false)
                  curClz = curClz.getSuperclass()
              }
            } catch {
              case e: Exception => {
                logger.error("Failed to load message class %s".format(clsName), e)
              }
            }
          }

          if (isContainer == false) {
            // Checking for container
            try {
              // If required we need to enable this test
              // Convert class name into a class
              var curClz = Class.forName(clsName, true, _kamanjaLoader.loader)

              while (curClz != null && isMsg == false) {
                isMsg = isDerivedFrom(curClz, "com.ligadata.KamanjaBase.BaseMsgObj")
                if (isMsg == false)
                  curClz = curClz.getSuperclass()
              }
            } catch {
              case e: Exception => {
                logger.error("Failed to load container class %s".format(clsName), e)
              }
            }
          }

          logger.debug("isMsg:%s, isContainer:%s".format(isMsg, isContainer))

          if (isMsg || isContainer) {
            try {
              val mirror = ru.runtimeMirror(_kamanjaLoader.loader)
              val module = mirror.staticModule(clsName)
              val obj = mirror.reflectModule(module)
              val objinst = obj.instance

              if (isMsg) {
                // objinst
              } else {

              }

              if (objinst.isInstanceOf[BaseMsgObj]) {
                val messageObj = objinst.asInstanceOf[BaseMsgObj]
                logger.debug("Created Message Object")
                _messagesAndContainers(objFullName) = messageObj
              } else if (objinst.isInstanceOf[BaseContainerObj]) {
                val containerObj = objinst.asInstanceOf[BaseContainerObj]
                logger.debug("Created Container Object")
                _messagesAndContainers(objFullName) = containerObj
              } else {
                logger.error("Failed to instantiate message or conatiner object :" + clsName)
                isOk = false
              }
            } catch {
              case e: Exception => {
                logger.error("Failed to instantiate message or conatiner object:" + clsName, e)
                isOk = false
              }
            }
          } else {
            logger.error("Failed to instantiate message or conatiner object :" + clsName)
            isOk = false
          }
        }
        if (isOk == false) {
          logger.error("Failed to add message or conatiner object. NameSpace:%s, Name:%s, Version:%s".format(objNameSpace, objName, objVer))
        }
      } catch {
        case e: Exception => {
          logger.error("Failed to Add Message/Contianer", e)
          throw e
        }
      }
    }

    def AddMessageOrContianer(objType: String, objJson: String, jarPaths: collection.immutable.Set[String]): Unit = {
      try {
        implicit val jsonFormats = DefaultFormats
        val json = parse(objJson)
        val jsonObjMap = json.values.asInstanceOf[Map[String, Any]]
        AddMessageOrContianer(objType, jsonObjMap, jarPaths)
      } catch {
        case e: Exception => {
          logger.error("Failed to Add Message/Contianer", e)
          throw e
        }
      }
    }

    override def getMessgeOrContainerInstance(msgContainerType: String): ContainerInterface = {
      return null
    }
  }

  private def dumpKeyValueFailures(k: Key, v: Value): Unit = {
    val keyStr = "%d~%s~%d~%d".format(k.timePartition, k.bucketKey.mkString(","), k.transactionId, k.rowId)
    val msgStr = ("Failed to deserialize key: %s. Written the data related to this key in file:%s".format(keyStr, _sourceReadFailuresFilePath))
    logger.error(msgStr)
    _flReadFailures.println("Key:\n" + keyStr)
    _flReadFailures.println("Record:")
    _flReadFailures.println("SerializeType:\n" + v.serializerType + "\nSerializedInfo:")
    _flReadFailures.println(v.serializedInfo)
    _flReadFailures.flush()
  }

  private def isValidPath(path: String, checkForDir: Boolean = false, checkForFile: Boolean = false, str: String = "path"): Unit = {
    val fl = new File(path)
    if (fl.exists() == false) {
      val szMsg = "Given %s:%s does not exists".format(str, path)
      logger.error(szMsg)
      throw new Exception(szMsg)
    }

    if (checkForDir && fl.isDirectory() == false) {
      val szMsg = "Given %s:%s is not directory".format(str, path)
      logger.error(szMsg)
      throw new Exception(szMsg)
    }

    if (checkForFile && fl.isFile() == false) {
      val szMsg = "Given %s:%s is not file".format(str, path)
      logger.error(szMsg)
      throw new Exception(szMsg)
    }
  }

  private def GetDataStoreHandle(jarPaths: collection.immutable.Set[String], dataStoreInfo: String): DataStore = {
    try {
      logger.debug("Getting DB Connection for dataStoreInfo:%s".format(dataStoreInfo))
      return KeyValueManager.Get(jarPaths, dataStoreInfo,null,null,null)
    } catch {
      case e: Exception => throw e
      case e: Throwable => throw e
    }
  }

  private def LoadFqJarsIfNeeded(jars: Array[String], loadedJars: scala.collection.mutable.TreeSet[String], loader: KamanjaClassLoader): Boolean = {
    // Loading all jars
    for (j <- jars) {
      logger.debug("Processing Jar " + j.trim)
      val fl = new File(j.trim)
      if (fl.exists) {
        try {
          if (loadedJars(fl.getPath())) {
            logger.debug("Jar " + j.trim + " already loaded to class path.")
          } else {
            loader.addURL(fl.toURI().toURL())
            logger.debug("Jar " + j.trim + " added to class path.")
            loadedJars += fl.getPath()
          }
        } catch {
          case e: Exception => {
            logger.error("Jar " + j.trim + " failed added to class path.", e)
            return false
          }
        }
      } else {
        logger.error("Jar " + j.trim + " not found")
        return false
      }
    }

    true
  }

  private def GetValidJarFile(jarPaths: collection.immutable.Set[String], jarName: String): String = {
    if (jarPaths == null) return jarName // Returning base jarName if no jarpaths found
    jarPaths.foreach(jPath => {
      val fl = new File(jPath + "/" + jarName)
      if (fl.exists) {
        return fl.getPath
      }
    })
    return jarName // Returning base jarName if not found in jar paths
  }

  private def LoadJarIfNeeded(jsonObjMap: Map[String, Any], loadedJars: scala.collection.mutable.TreeSet[String], loader: KamanjaClassLoader, jarPaths: collection.immutable.Set[String]): Boolean = {
    if (jarPaths == null) return false

    var retVal: Boolean = true
    var allJars = ArrayBuffer[String]()

    try {
      val jarName = jsonObjMap.getOrElse("JarName", "").toString.trim()
      val dependantJars = jsonObjMap.getOrElse("DependantJars", null)

      if (dependantJars != null) {
        val depJars = dependantJars.asInstanceOf[List[String]]
        if (depJars.size > 0)
          allJars ++= depJars
      }

      if (jarName.size > 0) {
        allJars += jarName
      }

      if (allJars.size > 0) {
        val jars = allJars.map(j => GetValidJarFile(jarPaths, j)).toArray
        return LoadFqJarsIfNeeded(jars, loadedJars, loader)
      }

      return true
    } catch {
      case e: Exception => {
        logger.error("Failed to collect jars", e)
        throw e
      }
    }
    return true
  }

  private def LoadJarIfNeeded(objJson: String, loadedJars: scala.collection.mutable.TreeSet[String], loader: KamanjaClassLoader, jarPaths: collection.immutable.Set[String]): Boolean = {
    if (jarPaths == null) return false

    var retVal: Boolean = true
    var allJars = ArrayBuffer[String]()

    try {
      implicit val jsonFormats = DefaultFormats
      val json = parse(objJson)
      val jsonObjMap = json.values.asInstanceOf[Map[String, Any]]

      return LoadJarIfNeeded(jsonObjMap, loadedJars, loader, jarPaths)
    } catch {
      case e: Exception => {
        logger.error("Failed to parse JSON" + objJson, e)
        throw e
      }
    }
    return true
  }

  private def getEmptyIfNull(jarName: String): String = {
    if (jarName != null) jarName else ""
  }

  private def serializeObjectToJson(mdObj: BaseElem): (String, String) = {
    val ver = MdMgr.ConvertLongVersionToString(mdObj.Version)
    try {
      mdObj match {
        // Assuming that zookeeper transaction will be different based on type of object
        case o: ModelDef => {
          val json = (("ObjectType" -> "ModelDef") ~
            ("IsActive" -> o.IsActive.toString) ~
            ("IsDeleted" -> o.IsDeleted.toString) ~
            ("TransId" -> o.TranId.toString) ~
            ("OrigDef" -> o.OrigDef) ~
            ("ObjectDefinition" -> o.ObjectDefinition) ~
            ("ObjectFormat" -> ObjFormatType.asString(o.ObjectFormat)) ~
            ("ModelType" -> o.miningModelType.toString) ~
            ("NameSpace" -> o.nameSpace) ~
            ("Name" -> o.name) ~
            ("Version" -> ver) ~
            ("PhysicalName" -> o.physicalName) ~
            ("JarName" -> getEmptyIfNull(o.jarName)) ~
            ("DependantJars" -> o.CheckAndGetDependencyJarNames.toList))
          ("ModelDef", compact(render(json)))
        }
        case o: MessageDef => {
          val json = (("ObjectType" -> "MessageDef") ~
            ("IsActive" -> o.IsActive.toString) ~
            ("IsDeleted" -> o.IsDeleted.toString) ~
            ("TransId" -> o.TranId.toString) ~
            ("OrigDef" -> o.OrigDef) ~
            ("ObjectDefinition" -> o.ObjectDefinition) ~
            ("ObjectFormat" -> ObjFormatType.asString(o.ObjectFormat)) ~
            ("NameSpace" -> o.nameSpace) ~
            ("Name" -> o.name) ~
            ("Version" -> ver) ~
            ("PhysicalName" -> o.physicalName) ~
            ("JarName" -> getEmptyIfNull(o.jarName)) ~
            ("DependantJars" -> o.CheckAndGetDependencyJarNames.toList))
          ("MessageDef", compact(render(json)))
        }
        /*
        case o: MappedMsgTypeDef => {
          val json = (("ObjectType" -> "MappedMsgTypeDef") ~
            ("IsActive" -> o.IsActive.toString) ~
            ("IsDeleted" -> o.IsDeleted.toString) ~
            ("TransId" -> o.TranId.toString) ~
            ("OrigDef" -> o.OrigDef) ~
            ("ObjectDefinition" -> o.ObjectDefinition) ~
            ("ObjectFormat" -> ObjFormatType.asString(o.ObjectFormat)) ~
            ("NameSpace" -> o.nameSpace) ~
            ("Name" -> o.name) ~
            ("Version" -> ver) ~
            ("PhysicalName" -> o.physicalName) ~
            ("JarName" -> getEmptyIfNull(o.jarName)) ~
            ("DependantJars" -> o.CheckAndGetDependencyJarNames.toList))
          ("MappedMsgTypeDef", compact(render(json)))
        }
        case o: StructTypeDef => {
          val json = (("ObjectType" -> "StructTypeDef") ~
            ("IsActive" -> o.IsActive.toString) ~
            ("IsDeleted" -> o.IsDeleted.toString) ~
            ("TransId" -> o.TranId.toString) ~
            ("OrigDef" -> o.OrigDef) ~
            ("ObjectDefinition" -> o.ObjectDefinition) ~
            ("ObjectFormat" -> ObjFormatType.asString(o.ObjectFormat)) ~
            ("NameSpace" -> o.nameSpace) ~
            ("Name" -> o.name) ~
            ("Version" -> ver) ~
            ("PhysicalName" -> o.physicalName) ~
            ("JarName" -> getEmptyIfNull(o.jarName)) ~
            ("DependantJars" -> o.CheckAndGetDependencyJarNames.toList))
          ("StructTypeDef", compact(render(json)))
        }
        */
        case o: ContainerDef => {
          val json = (("ObjectType" -> "ContainerDef") ~
            ("IsActive" -> o.IsActive.toString) ~
            ("IsDeleted" -> o.IsDeleted.toString) ~
            ("TransId" -> o.TranId.toString) ~
            ("OrigDef" -> o.OrigDef) ~
            ("ObjectDefinition" -> o.ObjectDefinition) ~
            ("ObjectFormat" -> ObjFormatType.asString(o.ObjectFormat)) ~
            ("NameSpace" -> o.nameSpace) ~
            ("Name" -> o.name) ~
            ("PhysicalName" -> o.physicalName) ~
            ("JarName" -> getEmptyIfNull(o.jarName)) ~
            ("DependantJars" -> o.CheckAndGetDependencyJarNames.toList))
          ("ContainerDef", compact(render(json)))
        }
        case o: FunctionDef => {
          val args = if (o.args == null || o.args.size == 0) List[(String, String, String)]() else o.args.map(arg => (arg.name, arg.aType.NameSpace, arg.aType.Name)).toList
          val features = if (o.features == null || o.features.size == 0) List[String]() else o.features.map(f => f.toString()).toList

          val fnDefJson =
            ("Functions" ->
              List(("NameSpace" -> o.nameSpace) ~
                ("Name" -> o.name) ~
                ("PhysicalName" -> o.physicalName) ~
                ("ReturnTypeNameSpace" -> o.retType.nameSpace) ~
                ("ReturnTypeName" -> o.retType.name) ~
                ("Arguments" -> args.map(arg => ("ArgName" -> arg._1) ~ ("ArgTypeNameSpace" -> arg._2) ~ ("ArgTypeName" -> arg._3))) ~
                ("Features" -> features) ~
                ("Version" -> ver) ~
                ("JarName" -> getEmptyIfNull(o.jarName)) ~
                ("DependantJars" -> o.CheckAndGetDependencyJarNames.toList)))

          val fnDefStr = compact(render(fnDefJson))

          val json = (("ObjectType" -> "FunctionDef") ~
            ("IsActive" -> o.IsActive.toString) ~
            ("IsDeleted" -> o.IsDeleted.toString) ~
            ("TransId" -> o.TranId.toString) ~
            ("OrigDef" -> o.OrigDef) ~
            ("ObjectDefinition" -> fnDefStr) ~
            ("ObjectFormat" -> ObjFormatType.asString(o.ObjectFormat)) ~
            ("NameSpace" -> o.nameSpace) ~
            ("Name" -> o.name) ~
            ("Version" -> ver) ~
            ("PhysicalName" -> o.physicalName) ~
            ("JarName" -> getEmptyIfNull(o.jarName)) ~
            ("DependantJars" -> o.CheckAndGetDependencyJarNames.toList))
          ("FunctionDef", compact(render(json)))
        }
        case o: ArrayTypeDef => {
          val json = (("ObjectType" -> "ArrayTypeDef") ~
            ("IsActive" -> o.IsActive.toString) ~
            ("IsDeleted" -> o.IsDeleted.toString) ~
            ("TransId" -> o.TranId.toString) ~
            ("OrigDef" -> o.OrigDef) ~
            ("ObjectDefinition" -> o.ObjectDefinition) ~
            ("ObjectFormat" -> ObjFormatType.asString(o.ObjectFormat)) ~
            ("NameSpace" -> o.nameSpace) ~
            ("Name" -> o.name) ~
            ("Version" -> ver) ~
            ("PhysicalName" -> o.physicalName) ~
            ("JarName" -> getEmptyIfNull(o.jarName)) ~
            ("DependantJars" -> o.CheckAndGetDependencyJarNames.toList))
          ("ArrayTypeDef", compact(render(json)))
        }
        case o: MapTypeDef => {
          val json = (("ObjectType" -> "MapTypeDef") ~
            ("IsActive" -> o.IsActive.toString) ~
            ("IsDeleted" -> o.IsDeleted.toString) ~
            ("TransId" -> o.TranId.toString) ~
            ("OrigDef" -> o.OrigDef) ~
            ("ObjectDefinition" -> o.ObjectDefinition) ~
            ("ObjectFormat" -> ObjFormatType.asString(o.ObjectFormat)) ~
            ("NameSpace" -> o.nameSpace) ~
            ("Name" -> o.name) ~
            ("Version" -> ver) ~
            ("PhysicalName" -> o.physicalName) ~
            ("JarName" -> getEmptyIfNull(o.jarName)) ~
            ("DependantJars" -> o.CheckAndGetDependencyJarNames.toList))
          ("MapTypeDef", compact(render(json)))
        }
        case o: JarDef => {
          val json = (("ObjectType" -> "JarDef") ~
            ("IsActive" -> o.IsActive.toString) ~
            ("IsDeleted" -> o.IsDeleted.toString) ~
            ("TransId" -> o.TranId.toString) ~
            ("OrigDef" -> o.OrigDef) ~
            ("ObjectDefinition" -> o.ObjectDefinition) ~
            ("ObjectFormat" -> ObjFormatType.asString(o.ObjectFormat)) ~
            ("NameSpace" -> o.nameSpace) ~
            ("Name" -> o.name) ~
            ("Version" -> ver) ~
            ("PhysicalName" -> o.physicalName) ~
            ("JarName" -> getEmptyIfNull(o.jarName)) ~
            ("DependantJars" -> o.CheckAndGetDependencyJarNames.toList))
          ("JarDef", compact(render(json)))
        }
        case _ => {
          throw new Exception("serializeObjectToJson doesn't support the objects of type objectType of " + mdObj.getClass().getName() + " yet.")
        }
      }
    } catch {
      case e: Exception => {
        logger.debug("Failed to serialize", e)
        throw e
      }
    }
  }

  private def isDerivedFrom(clz: Class[_], clsName: String): Boolean = {
    var isIt: Boolean = false

    val interfecs = clz.getInterfaces()
    logger.debug("Interfaces => " + interfecs.length + ",isDerivedFrom: Class=>" + clsName)

    breakable {
      for (intf <- interfecs) {
        val intfName = intf.getName()
        logger.debug("Interface:" + intfName)
        if (intfName.equals(clsName)) {
          isIt = true
          break
        }
      }
    }

    if (isIt == false) {
      val superclass = clz.getSuperclass
      if (superclass != null) {
        val scName = superclass.getName()
        logger.debug("SuperClass => " + scName)
        if (scName.equals(clsName)) {
          isIt = true
        }
      }
    }

    isIt
  }

  private def AddActiveMessageOrContianer(metadataElemsJson: Array[MetadataFormat], jarPaths: collection.immutable.Set[String]): Unit = {
    try {
      implicit val jsonFormats = DefaultFormats
      metadataElemsJson.foreach(mdElem => {
        if (mdElem.objType.compareToIgnoreCase("MessageDef") == 0 || mdElem.objType.compareToIgnoreCase("MappedMsgTypeDef") == 0 ||
          mdElem.objType.compareToIgnoreCase("StructTypeDef") == 0 || mdElem.objType.compareToIgnoreCase("ContainerDef") == 0) {
          val json = parse(mdElem.objDataInJson)
          val jsonObjMap = json.values.asInstanceOf[Map[String, Any]]
          val isActiveStr = jsonObjMap.getOrElse("IsActive", "").toString.trim()
          if (isActiveStr.size > 0) {
            val isActive = jsonObjMap.getOrElse("IsActive", "").toString.trim().toBoolean
            if (isActive)
              MdResolve.AddMessageOrContianer(mdElem.objType, jsonObjMap, jarPaths)
          } else {
            val objNameSpace = jsonObjMap.getOrElse("NameSpace", "").toString.trim()
            val objName = jsonObjMap.getOrElse("Name", "").toString.trim()
            val objVer = jsonObjMap.getOrElse("Version", "").toString.trim()
            logger.warn("message or conatiner of this version is not active. So, ignoring to migrate data for this. NameSpace:%s, Name:%s, Version:%s".format(objNameSpace, objName, objVer))
          }
        } else if (mdElem.objType.compareToIgnoreCase("ModelDef") == 0) {
          val json = parse(mdElem.objDataInJson)
          val jsonObjMap = json.values.asInstanceOf[Map[String, Any]]
          val isActiveStr = jsonObjMap.getOrElse("IsActive", "").toString.trim()
          if (isActiveStr.size > 0) {
            val isActive = jsonObjMap.getOrElse("IsActive", "").toString.trim().toBoolean
            if (isActive)
              LoadJarIfNeeded(jsonObjMap, MdResolve._kamanjaLoader.loadedJars, MdResolve._kamanjaLoader.loader, jarPaths)
          } else {
            val objNameSpace = jsonObjMap.getOrElse("NameSpace", "").toString.trim()
            val objName = jsonObjMap.getOrElse("Name", "").toString.trim()
            val objVer = jsonObjMap.getOrElse("Version", "").toString.trim()
            logger.warn("message or conatiner of this version is not active. So, ignoring to migrate data for this. NameSpace:%s, Name:%s, Version:%s".format(objNameSpace, objName, objVer))
          }
        }
      })
    } catch {
      case e: Exception => {
        logger.error("Failed to Add Message/Contianer", e)
        throw e
      }
    }
  }

  override def init(sourceInstallPath: String, metadataStoreInfo: String, dataStoreInfo: String, statusStoreInfo: String, sourceReadFailuresFilePath: String): Unit = {
    isValidPath(sourceInstallPath, true, false, "sourceInstallPath")
    isValidPath(sourceInstallPath + "/bin", true, false, "bin folder in sourceInstallPath")
    isValidPath(sourceInstallPath + "/lib/system", true, false, "/lib/system folder in sourceInstallPath")
    isValidPath(sourceInstallPath + "/lib/application", true, false, "/lib/application folder in sourceInstallPath")

    _sourceInstallPath = sourceInstallPath
    _metadataStoreInfo = metadataStoreInfo
    _dataStoreInfo = dataStoreInfo
    _statusStoreInfo = statusStoreInfo
    _sourceReadFailuresFilePath = sourceReadFailuresFilePath

    _flReadFailures = new PrintWriter(_sourceReadFailuresFilePath, "UTF-8")

    _bInit = true
  }

  override def isInitialized: Boolean = _bInit

  override def getAllMetadataTableNames: Array[TableName] = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")

    if (_metadataStoreInfo.trim.size == 0)
      return Array[TableName]()

    var parsed_json: Map[String, Any] = null
    try {
      val json = parse(_metadataStoreInfo)
      if (json == null || json.values == null) {
        val msg = "Failed to parse JSON configuration string:" + _metadataStoreInfo
        throw new Exception(msg)
      }
      parsed_json = json.values.asInstanceOf[Map[String, Any]]
    } catch {
      case e: Exception => {
        throw new Exception("Failed to parse JSON configuration string:" + _metadataStoreInfo, e)
      }
    }

    val namespace = if (parsed_json.contains("SchemaName")) parsed_json.getOrElse("SchemaName", "default").toString.trim else parsed_json.getOrElse("SchemaName", "default").toString.trim
    Array(new TableName(namespace, "config_objects"), new TableName(namespace, "jar_store"), new TableName(namespace, "metadata_objects"),
      new TableName(namespace, "model_config_objects"), new TableName(namespace, "transaction_id"))
  }

  override def getAllDataTableNames: Array[TableName] = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    Array[TableName]()
  }

  override def getAllStatusTableNames: Array[TableName] = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")
    Array[TableName]()
  }

  // BUGBUG:: We can make all gets as simple template for exceptions handling and call that.
  private def callGetData(dataStore: DataStoreOperations, containerName: String, callbackFunction: (Key, Value) => Unit): Unit = {
    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs
    var doneGet = false

    while (!doneGet) {
      try {
        dataStore.get(containerName, callbackFunction)
        doneGet = true
      } catch {
        case e@(_: ObjectNotFoundException | _: KeyNotFoundException) => {
          logger.debug("Failed to get data from container:%s".format(containerName), e)
          doneGet = true
        }
        case e: FatalAdapterException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: StorageDMLException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: StorageDDLException => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: Exception => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
        case e: Throwable => {
          logger.error("Failed to get data from container:%s.".format(containerName), e)
        }
      }

      if (!doneGet) {
        try {
          logger.error("Failed to get data from datastore. Waiting for another %d milli seconds and going to start them again.".format(failedWaitTime))
          Thread.sleep(failedWaitTime)
        } catch {
          case e: Exception => {
            logger.warn("", e)
          }
        }
        // Adjust time for next time
        if (failedWaitTime < maxFailedWaitTime) {
          failedWaitTime = failedWaitTime * 2
          if (failedWaitTime > maxFailedWaitTime)
            failedWaitTime = maxFailedWaitTime
        }
      }
    }
  }

  private def SplitFullName(mdName: String): (String, String) = {
    val buffer: StringBuilder = new StringBuilder
    val nameNodes: Array[String] = mdName.split('.')
    val name: String = nameNodes.last
    if (nameNodes.size > 1)
      nameNodes.take(nameNodes.size - 1).addString(buffer, ".")
    val namespace: String = buffer.toString
    (namespace, name)
  }

  // Callback function calls with metadata Object Type & metadata information in JSON string
  override def getAllMetadataObjs(backupTblSufix: String, callbackFunction: MetadataObjectCallBack, excludeMetadata: Array[String]): Unit = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")

    val excludedMetadataTypes = if (excludeMetadata != null && excludeMetadata.length > 0) excludeMetadata.map(t => t.toLowerCase.trim).toSet else Set[String]()

    val installPath = new File(_sourceInstallPath)

    var fromVersionInstallationPath = installPath.getAbsolutePath

    val sysPath = new File(_sourceInstallPath + "/lib/system")
    val appPath = new File(_sourceInstallPath + "/lib/application")

    val fromVersionJarPaths = collection.immutable.Set[String](sysPath.getAbsolutePath, appPath.getAbsolutePath)

    logger.info("fromVersionInstallationPath:%s, fromVersionJarPaths:%s".format(fromVersionInstallationPath, fromVersionJarPaths.mkString(",")))

    val kamanjaLoader = new KamanjaLoaderInfo

    _metadataStore = GetDataStoreHandle(fromVersionJarPaths, _metadataStoreInfo)

    try {
      // Load all metadata objects
      val buildMdlOne = (k: Key, v: Value) => {
        try {
          val strKey = k.bucketKey.mkString(".")
          val i = strKey.indexOf(".")
          val objType = strKey.substring(0, i)
          val typeName = strKey.substring(i + 1)
	  try{
            val (typ, jsonStr) = (objType,new String(v.serializedInfo))
	    logger.info("strKey => " + strKey + ",objType => " + objType + ",typeName => " + typeName + ",object json => " + jsonStr)
            if (excludedMetadataTypes.contains(typ.toLowerCase()) == false) {
              if (callbackFunction != null) {
                val retVal = callbackFunction.call(new MetadataFormat(typ, jsonStr))
                if (retVal == false) {
                  return
                }
              }
            }
          } catch {
            case e: Exception => {
              dumpKeyValueFailures(k, v); logger.warn("", e);
            }
            case e: Throwable => {
              dumpKeyValueFailures(k, v); logger.warn("", e);
            }
          }
        } catch {
          case e: Exception => {
            dumpKeyValueFailures(k, v); logger.warn("", e);
          }
          case e: Throwable => {
            dumpKeyValueFailures(k, v); logger.warn("", e);
          }
        }
      }

      callGetData(_metadataStore, "metadata_objects" + backupTblSufix, buildMdlOne)

      logger.debug("Collected all metadata objects from " + "metadata_objects" + backupTblSufix)

      if (excludedMetadataTypes.contains("ConfigDef".toLowerCase()) == false) {
        logger.debug("Collecting all model configuration objects from " + "model_config_objects" + backupTblSufix)
        val buildMdlCfglOne = (k: Key, v: Value) => {
          try {
            val strKey = k.bucketKey.mkString(".")
            val i = strKey.indexOf(".")
            val objType = strKey.substring(0, i)
            val typeName = strKey.substring(i + 1)
            val js = new String(v.serializedInfo)
	    logger.info("strKey => " + strKey + ",objType => " + objType + ",typeName => " + typeName + ",object json => " + js)
            val conf = js.asInstanceOf[Map[String, Any]]
            val (nameSpace, name) = SplitFullName(k.bucketKey(0))

            implicit val jsonFormats: Formats = DefaultFormats
            val str = "{\"" + name + "\" :" + Serialization.write(conf) + "}"

            val json = (("ObjectType" -> "ConfigDef") ~
              ("IsActive" -> "true") ~
              ("IsDeleted" -> "false") ~
              ("TransId" -> "0") ~
              ("OrigDef" -> "") ~
              ("ObjectDefinition" -> str) ~
              ("ObjectFormat" -> "JSON") ~
              ("NameSpace" -> nameSpace) ~
              ("Name" -> name) ~
              ("Version" -> "0") ~
              ("PhysicalName" -> "") ~
              ("JarName" -> "") ~
              ("DependantJars" -> List[String]()))

            val mdlCfg = compact(render(json))
            if (callbackFunction != null) {
              val retVal = callbackFunction.call(new MetadataFormat("ConfigDef", mdlCfg))
              if (retVal == false) {
                return
              }
            }
          } catch {
            case e: Exception => {
              dumpKeyValueFailures(k, v); logger.warn("", e);
            }
            case e: Throwable => {
              dumpKeyValueFailures(k, v); logger.warn("", e);
            }
          }
        }
        callGetData(_metadataStore, "model_config_objects" + backupTblSufix, buildMdlCfglOne)
      }

    } catch {
      case e: Exception => {
        throw new Exception("Failed to load metadata objects", e)
      }
    }
  }

  def GetKeys(containerName: String, store: DataStore): scala.collection.mutable.Set[Key] = {
    var objs = scala.collection.mutable.Set[Key]()
    val getObjFn = (k: Key) => {
      val a = objs.add(k)
    }
    try {
      store.getKeys(containerName, getObjFn)
      objs
    } catch {
      case e: ObjectNotFoundException => {
        logger.debug("ObjectNotFound Exception", e)
        throw e
      }
      case e: Exception => {
        logger.debug("General Exception", e)
        throw ObjectNotFoundException(e.getMessage(), e)
      }
    }
  }

  def GetValue(containerName: String, key: Key, store: DataStore): Value = {
    var value: Value = null
    val getObjFn = (k: Key, v: Value) => {
      value = v
    }
    try {
      store.get(containerName, Array(key), getObjFn)
      value
    } catch {
      case e: ObjectNotFoundException => {
        logger.debug("ObjectNotFound Exception", e)
        throw e
      }
      case e: Exception => {
        logger.debug("General Exception", e)
        throw ObjectNotFoundException(e.getMessage(), e)
      }
    }
  }

  def GetAllTableData(containerName: String, store: DataStore, callbackFunction: (Key, Value) => Unit): Unit = {
    try {
      store.get(containerName, callbackFunction)
    } catch {
      case e: ObjectNotFoundException => {
        logger.debug("ObjectNotFound Exception", e)
        throw e
      }
      case e: Exception => {
        logger.debug("General Exception", e)
        throw ObjectNotFoundException(e.getMessage(), e)
      }
    }
  }

  // metadataElemsJson are used for dependency load
  // Callback function calls with container name, timepartition value, bucketkey, transactionid, rowid, serializername & data in Gson (JSON) format.
  override def getAllDataObjs(backupTblSufix: String, metadataElemsJson: Array[MetadataFormat], msgsAndContainers: java.util.List[String], catalogTables: java.util.List[String], callbackFunction: DataObjectCallBack): Unit = {
    if (_bInit == false)
      throw new Exception("Not yet Initialized")

    val installPath = new File(_sourceInstallPath)

    var fromVersionInstallationPath = installPath.getAbsolutePath

    val sysPath = new File(_sourceInstallPath + "/lib/system")
    val appPath = new File(_sourceInstallPath + "/lib/application")

    val fromVersionJarPaths = collection.immutable.Set[String](sysPath.getAbsolutePath, appPath.getAbsolutePath)

    logger.info("fromVersionInstallationPath:%s, fromVersionJarPaths:%s".format(fromVersionInstallationPath, fromVersionJarPaths.mkString(",")))

    val dir = new File(fromVersionInstallationPath + "/bin");

    val mdapiFls = dir.listFiles.filter(_.isFile).filter(_.getName.startsWith("MetadataAPI-")).toList

    var baseFileToLoadFromPrevVer = ""
    if (mdapiFls.size == 0) {
      val kmFls = dir.listFiles.filter(_.isFile).filter(_.getName.startsWith("KamanjaManager-")).toList
      if (kmFls.size == 0) {
        val szMsg = "Not found %s/bin/MetadataAPI-* and %s/bin/KamanjaManager-*".format(fromVersionInstallationPath, fromVersionInstallationPath)
        logger.error(szMsg)
        throw new Exception(szMsg)
      } else {
        baseFileToLoadFromPrevVer = kmFls(0).getAbsolutePath
      }
    } else {
      baseFileToLoadFromPrevVer = mdapiFls(0).getAbsolutePath
    }
    // Loading the base file where we have all the base classes like classes from KamanjaBase, metadata, MetadataAPI, etc
    if (baseFileToLoadFromPrevVer != null && baseFileToLoadFromPrevVer.size > 0)
      LoadFqJarsIfNeeded(Array(baseFileToLoadFromPrevVer), MdResolve._kamanjaLoader.loadedJars, MdResolve._kamanjaLoader.loader)

    AddActiveMessageOrContianer(metadataElemsJson, fromVersionJarPaths)

    _dataStore = GetDataStoreHandle(fromVersionJarPaths, _dataStoreInfo)

    logger.debug("All Messages and Containers:%s".format(MdResolve._messagesAndContainers.map(kv => kv._1).mkString(",")))

    try {
      // Load all data objects
      breakable {
        msgsAndContainers.toArray.foreach(msg => {
          val msgName = msg.asInstanceOf[String]
          val getObjFn = (k: Key, v: Value) => {
            if (callbackFunction != null) {
              val serType = v.serializerType
              val arr = v.serializedInfo
              if (callbackFunction.call(Array(new DataFormat(msgName, k.timePartition, k.bucketKey, k.transactionId, k.rowId, serType, arr))) == false)
                throw new Exception("Data failed to consume")
            }
          }
          GetAllTableData(msgName + backupTblSufix, _dataStore, getObjFn)
        })

        // Collecting all data from catalog tables at once
        catalogTables.toArray.foreach(msg => {
          val tblName = msg.asInstanceOf[String]
          val getObjFn = (k: Key, v: Value) => {
            if (callbackFunction != null) {
              val serType = v.serializerType
              val arr = v.serializedInfo
              if (callbackFunction.call(Array(new DataFormat(tblName, k.timePartition, k.bucketKey, k.transactionId, k.rowId, serType, arr))) == false)
                throw new Exception("Data failed to consume")
            }
          }
          GetAllTableData(tblName + backupTblSufix, _dataStore, getObjFn)
        })
      }
    } catch {
      case e: Exception => {
        throw new Exception("Failed to get data", e)
      }
    }
  }

  override def shutdown: Unit = {
    if (_metadataStore != null)
      _metadataStore.Shutdown()
    _metadataStore = null
    if (_flReadFailures != null)
      _flReadFailures.close()
    _flReadFailures = null
  }
}

