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

package com.ligadata.tools.kvinit

import scala.collection.mutable._
import scala.io.Source
import scala.util.control.Breaks._
import java.io.BufferedWriter
import java.io.FileWriter
import sys.process._
import java.io.PrintWriter
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import org.apache.logging.log4j.{Logger, LogManager}
import com.ligadata.keyvaluestore._
import com.ligadata.KamanjaBase._
import com.ligadata.kamanja.metadataload.MetadataLoad
import com.ligadata.Utils.{Utils, KamanjaClassLoader, KamanjaLoaderInfo}
import java.util.Properties
import com.ligadata.kamanja.metadata.MdMgr._
import com.ligadata.kamanja.metadata._
import scala.reflect.runtime.{universe => ru}
import com.ligadata.Serialize._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import java.io.FileInputStream
import java.util.zip.GZIPInputStream
import java.io.BufferedReader
import java.io.InputStreamReader

//import scala.util.control.Breaks._
import scala.collection.mutable.ArrayBuffer
import com.ligadata.ZooKeeper._
import org.apache.curator.framework._
import com.ligadata.Serialize.{JDataStore, JZKInfo, JEnvCtxtJsonStr}
import com.ligadata.KvBase.{Key, TimeRange, KvBaseDefalts, KeyWithBucketIdAndPrimaryKey, KeyWithBucketIdAndPrimaryKeyCompHelper, LoadKeyWithBucketId}
import com.ligadata.StorageBase.{DataStore, Transaction}
import java.util.{Collection, Iterator, TreeMap}
import com.ligadata.Exceptions._
import com.ligadata.KamanjaVersion.KamanjaVersion

trait LogTrait {
  val loggerName = this.getClass.getName()
  val logger = LogManager.getLogger(loggerName)
}

object KVInit extends App with LogTrait {

  // 646 - 676 Change begins - replace MetadataAPIImpl
  val getMetadataAPI = MetadataAPI.getMetadataApiInterface()
  // 646 - 676 Change ends

  def usage: String = {
    """
Usage: scala com.ligadata.kvinit.KVInit
    --version
    --config <config file while has jarpaths, metadata store information & data store information>
    --typename <full package qualified name of a Container or Message>
    --datafiles <input to load>
    --keyfieldname  <name of one of the fields in the first line of the datafiles file>

Nothing fancy here.  Mapdb kv store is created from arguments... style is hash map. Support
for other styles of input (e.g., JSON, XML) are not supported.

The name of the kvstore will be the classname(without it path).

It is expected that the first row of the csv file will be the column names.  One of the names
must be specified as the key field name.  Failure to find this name causes termination and
no kv store creation.

Sample uses:
      java -jar /tmp/KamanjaInstall/KVInit-1.0 --typename System.TestContainer --config /tmp/KamanjaInstall/EngineConfig.cfg --datafiles /tmp/KamanjaInstall/sampledata/TestContainer.csv --keyfieldname Id

    """
  }

  override def main(args: Array[String]) {

    logger.debug("KVInit.main begins")

    if (args.length == 0) logger.error(usage)
    val arglist = args.toList
    type OptionMap = Map[Symbol, String]
    logger.debug(arglist)
    def nextOption(map: OptionMap, list: List[String]): OptionMap = {
      list match {
        case Nil => map
        case "--config" :: value :: tail =>
          nextOption(map ++ Map('config -> value), tail)
        case "--kvname" :: value :: tail => // Deprecated, use typename instead
          nextOption(map ++ Map('kvname -> value), tail) // Deprecated, use typename instead
        case "--typename" :: value :: tail =>
          nextOption(map ++ Map('typename -> value), tail)
        case "--csvpath" :: value :: tail => // Deprecated, use datafiles instead
          nextOption(map ++ Map('csvpath -> value), tail) // Deprecated, use datafiles instead
        case "--datafiles" :: value :: tail =>
          nextOption(map ++ Map('datafiles -> value), tail)
        case "--keyfieldname" :: value :: tail => // Deprecated, use keyfields instead
          nextOption(map ++ Map('keyfieldname -> value), tail) // Deprecated, use keyfields instead
        case "--keyfields" :: value :: tail =>
          nextOption(map ++ Map('keyfields -> value), tail)
        case "--delimiter" :: value :: tail =>
          nextOption(map ++ Map('delimiter -> value), tail) // Deprecated, use deserializer & optionsjson
        case "--keyandvaluedelimiter" :: value :: tail =>
          nextOption(map ++ Map('keyandvaluedelimiter -> value), tail) // Deprecated, use deserializer & optionsjson
        case "--fielddelimiter" :: value :: tail =>
          nextOption(map ++ Map('fielddelimiter -> value), tail) // Deprecated, use deserializer & optionsjson
        case "--valuedelimiter" :: value :: tail =>
          nextOption(map ++ Map('valuedelimiter -> value), tail) // Deprecated, use deserializer & optionsjson
        case "--ignoreerrors" :: value :: tail =>
          nextOption(map ++ Map('ignoreerrors -> value), tail)
        case "--ignorerecords" :: value :: tail =>
          nextOption(map ++ Map('ignorerecords -> value), tail)
        case "--format" :: value :: tail =>
          nextOption(map ++ Map('format -> value), tail)
        case "--deserializer" :: value :: tail =>
          nextOption(map ++ Map('deserializer -> value), tail)
        case "--optionsjson" :: value :: tail =>
          nextOption(map ++ Map('optionsjson -> value), tail)
        case "--onlyappend" :: tail =>
          nextOption(map ++ Map('onlyappend -> "true"), tail)
        case "--version" :: tail =>
          nextOption(map ++ Map('version -> "true"), tail)
        case option :: tail =>
          logger.error("Unknown option " + option)
          sys.exit(1)
      }
    }

    val options = nextOption(Map(), arglist)
    val version = options.getOrElse('version, "false").toString
    if (version.equalsIgnoreCase("true")) {
      KamanjaVersion.print
      return
    }

    var cfgfile = if (options.contains('config)) options.apply('config) else null
    var typename = if (options.contains('typename)) options.apply('typename) else if (options.contains('kvname)) options.apply('kvname) else null
    var tmpdatafiles = if (options.contains('datafiles)) options.apply('datafiles) else if (options.contains('csvpath)) options.apply('csvpath) else null
    val tmpkeyfieldnames = if (options.contains('keyfields)) options.apply('keyfields) else if (options.contains('keyfieldname)) options.apply('keyfieldname) else null
    var ignoreerrors = (if (options.contains('ignoreerrors)) options.apply('ignoreerrors) else "0").trim
    var commitBatchSize = (if (options.contains('commitbatchsize)) options.apply('commitbatchsize) else "10000").trim.toInt
    var deserializer = if (options.contains('deserializer)) options.apply('deserializer) else null
    var optionsjson = if (options.contains('optionsjson)) options.apply('optionsjson) else null

    val isOnlyAppend = options.getOrElse('onlyappend, "false").toString.trim.toBoolean

    if (commitBatchSize <= 0) {
      logger.error("commitbatchsize must be greater than 0")
      return
    }

    if (deserializer != null) {
      val delimiterString = if (options.contains('delimiter)) options.apply('delimiter) else null
      val format = (if (options.contains('format)) options.apply('format) else null)
      val keyAndValueDelimiter = if (options.contains('keyandvaluedelimiter)) options.apply('keyandvaluedelimiter) else null
      val fieldDelimiter1 = if (options.contains('fielddelimiter)) options.apply('fielddelimiter) else null
      val valueDelimiter = if (options.contains('valuedelimiter)) options.apply('valuedelimiter) else null

      if (delimiterString != null || format != null || keyAndValueDelimiter != null || fieldDelimiter1 != null && valueDelimiter != null)
        logger.warn("deserializer:%s is specified. Ignoring deprecated options: delimiter, format, keyandvaluedelimiter, fielddelimiter, valuedelimiter")
    } else {
      logger.warn("recommented to use deserializer & optionsjson")

      val delimiterString = if (options.contains('delimiter)) options.apply('delimiter) else null
      val format = (if (options.contains('format)) options.apply('format) else "delimited").trim.toLowerCase()
      val keyAndValueDelimiter = if (options.contains('keyandvaluedelimiter)) options.apply('keyandvaluedelimiter) else null
      val fieldDelimiter1 = if (options.contains('fielddelimiter)) options.apply('fielddelimiter) else null
      val valueDelimiter = if (options.contains('valuedelimiter)) options.apply('valuedelimiter) else null

      val fieldDelimiter = if (fieldDelimiter1 != null) fieldDelimiter1 else delimiterString

      if (format.equals("delimited")) {
        deserializer = "com.ligadata.kamanja.serializer.csvserdeser"
        optionsjson = """{"alwaysQuoteFields":false,"fieldDelimiter":"%s","valueDelimiter":"%s"}""".format(fieldDelimiter, valueDelimiter)
      }
      else if (format.equals("json")) {
        deserializer = "com.ligadata.kamanja.serializer.jsonserdeser"
        optionsjson = """{"alwaysQuoteFields":false,"fieldDelimiter":"%s","valueDelimiter":"%s"}""".format(fieldDelimiter, valueDelimiter)
      } else {
        logger.error("Supported formats are only delimited & json")
        return
      }
    }

    val keyfieldnames = if (tmpkeyfieldnames != null && tmpkeyfieldnames.trim.size > 0) tmpkeyfieldnames.trim.split(",").map(_.trim).filter(_.length() > 0) else Array[String]()

    val ignoreRecords = (if (options.contains('ignorerecords)) options.apply('ignorerecords) else "0".toString).trim.toInt

    if (ignoreerrors.size == 0) ignoreerrors = "0"

    if (options.contains('keyfieldname) && keyfieldnames.size == 0) {
      logger.error("keyfieldname does not have valid strings to take header")
      return
    }

    val dataFiles = if (tmpdatafiles == null || tmpdatafiles.trim.size == 0) Array[String]() else tmpdatafiles.trim.split(",").map(_.trim).filter(_.length() > 0)

    var valid: Boolean = (cfgfile != null && dataFiles.size > 0 && typename != null)

    if (valid) {
      cfgfile = cfgfile.trim
      typename = typename.trim
      valid = (cfgfile.size != 0 && dataFiles.size > 0 && typename.size != 0)
    }

    if (valid) {
      val (loadConfigs, failStr) = Utils.loadConfiguration(cfgfile.toString, true)
      if (failStr != null && failStr.size > 0) {
        logger.error(failStr)
        return
      }
      if (loadConfigs == null) {
        logger.error("Failed to load configurations from configuration file")
        return
      }

      KvInitConfiguration.configFile = cfgfile.toString

      val kvmaker: KVInit = new KVInit(loadConfigs, typename.toLowerCase, dataFiles, keyfieldnames, deserializer, optionsjson, ignoreerrors, ignoreRecords, commitBatchSize, isOnlyAppend)
      if (kvmaker.isOk) {
        var dstore: DataStore = null;
        try {
          dstore = kvmaker.GetDataStoreHandle(KvInitConfiguration.jarPaths, kvmaker.dataDataStoreInfo)
          if (dstore != null) {
            try {
              kvmaker.buildContainerOrMessage(dstore)
            } catch {
              case e: Exception => {
                logger.error("Failed to build Container or Message.", e)
              }
            } finally {
              if (dstore != null)
                dstore.Shutdown()
              dstore = null
              com.ligadata.transactions.NodeLevelTransService.Shutdown
              if (kvmaker.zkcForSetData != null)
                kvmaker.zkcForSetData.close()
            }
          } else {
            logger.error("Failed to connect to datastore:" + kvmaker.dataDataStoreInfo)
          }
        } catch {
          case e: FatalAdapterException => {
            logger.error("Failed to connect to Datastore.", e)
          }
          case e: StorageConnectionException => {
            logger.error("Failed to connect to Datastore.", e)
          }
          case e: StorageFetchException => {
            logger.error("Failed to connect to Datastore.", e)
          }
          case e: StorageDMLException => {
            logger.error("Failed to connect to Datastore.", e)
          }
          case e: StorageDDLException => {
            logger.error("Failed to connect to Datastore.", e)
          }
          case e: Exception => {
            logger.error("Failed to connect to Datastore.", e)
          }
          case e: Throwable => {
            logger.error("Failed to connect to Datastore.", e)
          }
        } finally {
          if (dstore != null)
            dstore.Shutdown()
          dstore = null
        }
      }
      MetadataAPI.getMetadataApiInterface().CloseDbStore

    } else {
      logger.error("Illegal and/or missing arguments")
      logger.error(usage)
    }
  }
}

object KvInitConfiguration {
  var nodeId: Int = _
  var configFile: String = _
  var jarPaths: collection.immutable.Set[String] = _
}

class KVInit(val loadConfigs: Properties, val typename: String, val dataFiles: Array[String], val keyfieldnames: Array[String], val deserializer: String, val optionsjson: String,
             ignoreerrors: String, ignoreRecords: Int, commitBatchSize: Int, val isOnlyAppend: Boolean) extends LogTrait with ObjectResolver {
  var ignoreErrsCount = if (ignoreerrors != null && ignoreerrors.size > 0) ignoreerrors.toInt else 0
  if (ignoreErrsCount < 0) ignoreErrsCount = 0
  var isOk: Boolean = true
  var zkcForSetData: CuratorFramework = null
  var totalCommittedMsgs: Int = 0
  // 646 - 676 Change begins - replace MetadataAPIImpl
  val getMetadataAPI = MetadataAPI.getMetadataApiInterface()
    // 646 - 676 Change ends
  val kvInitLoader = new KamanjaLoaderInfo

  KvInitConfiguration.nodeId = loadConfigs.getProperty("nodeId".toLowerCase, "0").replace("\"", "").trim.toInt
  if (KvInitConfiguration.nodeId <= 0) {
    logger.error("Not found valid nodeId. It should be greater than 0")
    isOk = false
  }

  var nodeInfo: NodeInfo = _

  if (isOk) {
    getMetadataAPI.InitMdMgrFromBootStrap(KvInitConfiguration.configFile, false)

    nodeInfo = mdMgr.Nodes.getOrElse(KvInitConfiguration.nodeId.toString, null)
    if (nodeInfo == null) {
      // 660 Change begins - bug fix for proper cluster config upload message
      logger.error("Node %d not found in metadata. Please ensure cluster configuration has been uploaded.".format(KvInitConfiguration.nodeId))
      // 660 Change ends
      isOk = false
    }
  }

  if (isOk) {
    KvInitConfiguration.jarPaths = if (nodeInfo.JarPaths == null) Array[String]().toSet else nodeInfo.JarPaths.map(str => str.replace("\"", "").trim).filter(str => str.size > 0).toSet
    if (KvInitConfiguration.jarPaths.size == 0) {
      logger.error("Not found valid JarPaths.")
      isOk = false
    }
  }

  val cluster = if (isOk) mdMgr.ClusterCfgs.getOrElse(nodeInfo.ClusterId, null) else null
  if (isOk && cluster == null) {
    logger.error("Cluster not found for Node %d  & ClusterId : %s".format(KvInitConfiguration.nodeId, nodeInfo.ClusterId))
    isOk = false
  }

  val zooKeeperInfo = if (isOk) cluster.cfgMap.getOrElse("ZooKeeperInfo", null) else null
  if (isOk && zooKeeperInfo == null) {
    logger.error("ZooKeeperInfo not found for Node %d  & ClusterId : %s".format(KvInitConfiguration.nodeId, nodeInfo.ClusterId))
    isOk = false
  }

  var sysCatalogDataStoreInfo: String = null
  var dataDataStoreInfo: String = null
  var zkConnectString: String = null
  var zkNodeBasePath: String = null
  var zkSessionTimeoutMs: Int = 0
  var zkConnectionTimeoutMs: Int = 0

  if (isOk) {
    sysCatalogDataStoreInfo = if (isOk) cluster.cfgMap.getOrElse("SystemCatalog", null) else null
    if (isOk && sysCatalogDataStoreInfo == null) {
      logger.error("SystemCatalog not found for Node %d  & ClusterId : %s".format(KvInitConfiguration.nodeId, nodeInfo.ClusterId))
      isOk = false
    }
  }

  if (isOk) {
    implicit val jsonFormats: Formats = DefaultFormats
    // val dataStoreInfo = parse(dataStore).extract[JDataStore]
    val zKInfo = parse(zooKeeperInfo).extract[JZKInfo]

    if (isOk) {
      zkConnectString = zKInfo.ZooKeeperConnectString.replace("\"", "").trim
      zkNodeBasePath = zKInfo.ZooKeeperNodeBasePath.replace("\"", "").trim
      zkSessionTimeoutMs = if (zKInfo.ZooKeeperSessionTimeoutMs == None || zKInfo.ZooKeeperSessionTimeoutMs == null) 0 else zKInfo.ZooKeeperSessionTimeoutMs.get.toString.toInt
      zkConnectionTimeoutMs = if (zKInfo.ZooKeeperConnectionTimeoutMs == None || zKInfo.ZooKeeperConnectionTimeoutMs == null) 0 else zKInfo.ZooKeeperConnectionTimeoutMs.get.toString.toInt

      // Taking minimum values in case if needed
      if (zkSessionTimeoutMs <= 0)
        zkSessionTimeoutMs = 30000
      if (zkConnectionTimeoutMs <= 0)
        zkConnectionTimeoutMs = 30000
    }

    if (zkConnectString.size == 0) {
      logger.warn("Not found valid Zookeeper connection string.")
    }

    if (zkConnectString.size > 0 && zkNodeBasePath.size == 0) {
      logger.warn("Not found valid Zookeeper ZNode Base Path.")
    }
  }

  var typeNameCorrType: BaseTypeDef = _
  var tenantId: String = ""
  var kvTableName: String = _
  var messageObj: MessageFactoryInterface = _
  var containerObj: ContainerFactoryInterface = _
  var objFullName: String = _
  // var kryoSer: com.ligadata.Serialize.Serializer = null

  if (isOk) {
    typeNameCorrType = mdMgr.ActiveType(typename.toLowerCase)
    if (typeNameCorrType == null || typeNameCorrType == None) {
      logger.error("Not found valid type for " + typename.toLowerCase)
      isOk = false
    } else {
      val msg = mdMgr.Message(typename.toLowerCase, -1, false)
      val cnt = mdMgr.Container(typename.toLowerCase, -1, false)
      tenantId = if (msg != None) msg.get.TenantId else if (cnt != None) cnt.get.TenantId else ""
      objFullName = typeNameCorrType.FullName.toLowerCase
      kvTableName = objFullName.replace('.', '_')
    }
  }

  if (isOk && tenantId.trim.size == 0) {
    logger.error("Not found valid tenantId for " + typename)
    isOk = false
  } else {
    val tenatInfo = mdMgr.GetTenantInfo(tenantId.toLowerCase)
    if (tenatInfo == null) {
      logger.error("Not found tenantInfo for tenantId " + tenantId)
      isOk = false
    } else {
      if (tenatInfo.primaryDataStore == null || tenatInfo.primaryDataStore.trim.size == 0) {
        logger.error("Not found valid Primary Datastore for tenantId " + tenantId)
        isOk = false
      } else {
        dataDataStoreInfo = tenatInfo.primaryDataStore
      }
    }
  }

  var isMsg = false
  var isContainer = false

  if (isOk) {
    isOk = LoadJarIfNeeded(typeNameCorrType, kvInitLoader.loadedJars, kvInitLoader.loader)
  }

  if (isOk) {
    var clsName = typeNameCorrType.PhysicalName.trim
    if (clsName.size > 0 && clsName.charAt(clsName.size - 1) != '$') // if no $ at the end we are taking $
      clsName = clsName + "$"

    if (isMsg == false) {
      // Checking for Message
      try {
        // Convert class name into a class
        var curClz = Class.forName(clsName, true, kvInitLoader.loader)

        while (curClz != null && isContainer == false) {
          isContainer = Utils.isDerivedFrom(curClz, "com.ligadata.KamanjaBase.ContainerFactoryInterface")
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
        var curClz = Class.forName(clsName, true, kvInitLoader.loader)

        while (curClz != null && isMsg == false) {
          isMsg = Utils.isDerivedFrom(curClz, "com.ligadata.KamanjaBase.MessageFactoryInterface")
          if (isMsg == false)
            curClz = curClz.getSuperclass()
        }
      } catch {
        case e: Exception => {
          logger.error("Failed to load container class %s".format(clsName), e)
        }
      }
    }

    if (isMsg || isContainer) {
      try {
        val module = kvInitLoader.mirror.staticModule(clsName)
        val obj = kvInitLoader.mirror.reflectModule(module)
        val objinst = obj.instance
        if (objinst.isInstanceOf[MessageFactoryInterface]) {
          messageObj = objinst.asInstanceOf[MessageFactoryInterface]
          logger.debug("Created Message Object")
        } else if (objinst.isInstanceOf[ContainerFactoryInterface]) {
          containerObj = objinst.asInstanceOf[ContainerFactoryInterface]
          logger.debug("Created Container Object")
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
    /*
    if (isOk) {
      kryoSer = SerializerManager.GetSerializer("kryo")
      if (kryoSer != null)
        kryoSer.SetClassLoader(kvInitLoader.loader)
    }
*/
  }

  private def LoadJarIfNeeded(elem: BaseElem, loadedJars: TreeSet[String], loader: KamanjaClassLoader): Boolean = {
    if (KvInitConfiguration.jarPaths == null) return false

    var retVal: Boolean = true
    var allJars: Array[String] = null

    val jarname = if (elem.JarName == null) "" else elem.JarName.trim

    if (elem.DependencyJarNames != null && elem.DependencyJarNames.size > 0 && jarname.size > 0) {
      allJars = elem.DependencyJarNames :+ jarname
    } else if (elem.DependencyJarNames != null && elem.DependencyJarNames.size > 0) {
      allJars = elem.DependencyJarNames
    } else if (jarname.size > 0) {
      allJars = Array(jarname)
    } else {
      return retVal
    }

    val jars = allJars.map(j => Utils.GetValidJarFile(KvInitConfiguration.jarPaths, j))

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

  private def GetDataStoreHandle(jarPaths: collection.immutable.Set[String], dataStoreInfo: String): DataStore = {
    try {
      logger.debug("Getting DB Connection for dataStoreInfo:%s".format(dataStoreInfo))
      return KeyValueManager.Get(jarPaths, dataStoreInfo, null, null, null)
    } catch {
      case e: Exception => throw e
      case e: Throwable => throw e
    }
  }

  override def getInstance(MsgContainerType: String): ContainerInterface = {
    if (MsgContainerType.compareToIgnoreCase(objFullName) != 0)
      return null
    // Simply creating new object and returning. Not checking for MsgContainerType. This is issue if the child level messages ask for the type
    if (isMsg)
      return messageObj.createInstance.asInstanceOf[ContainerInterface]
    if (isContainer)
      return containerObj.createInstance.asInstanceOf[ContainerInterface]
    return null
  }

  override def getInstance(schemaId: Long): ContainerInterface = {
    //BUGBUG:: For now we are getting latest class. But we need to get the old one too.
    if (mdMgr == null)
      throw new KamanjaException("Metadata Not found", null)

    val contOpt = mdMgr.ContainerForSchemaId(schemaId.toInt)

    if (contOpt == None)
      throw new KamanjaException("Container Not found for schemaid:" + schemaId, null)

    getInstance(contOpt.get.FullName)
  }

  override def getMdMgr: MdMgr = mdMgr

  private def collectKeyAndValues(k: Key, v: Any, dataByBucketKeyPart: TreeMap[KeyWithBucketIdAndPrimaryKey, ContainerInterface], loadedKeys: java.util.TreeSet[LoadKeyWithBucketId]): Unit = {
    val value: ContainerInterface = v.asInstanceOf[ContainerInterface]
    val primarykey = value.getPrimaryKey
    val key = KeyWithBucketIdAndPrimaryKey(KeyWithBucketIdAndPrimaryKeyCompHelper.BucketIdForBucketKey(k.bucketKey), k, primarykey != null && primarykey.size > 0, primarykey)
    dataByBucketKeyPart.put(key, value)

    val bucketId = KeyWithBucketIdAndPrimaryKeyCompHelper.BucketIdForBucketKey(k.bucketKey)
    val loadKey = LoadKeyWithBucketId(bucketId, TimeRange(k.timePartition, k.timePartition), k.bucketKey)
    loadedKeys.add(loadKey)
  }

  private def LoadDataIfNeeded(loadKey: LoadKeyWithBucketId, loadedKeys: java.util.TreeSet[LoadKeyWithBucketId], dataByBucketKeyPart: TreeMap[KeyWithBucketIdAndPrimaryKey, ContainerInterface], kvstore: DataStore): Unit = {
    if (loadedKeys.contains(loadKey))
      return
    val buildOne = (k: Key, v: Any, serType: String, typ: String, ver: Int) => {
      collectKeyAndValues(k, v, dataByBucketKeyPart, loadedKeys)
    }

    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs
    var doneGet = false

    while (!doneGet) {
      try {
        kvstore.get(objFullName, Array(loadKey.tmRange), Array(loadKey.bucketKey), buildOne)
        loadedKeys.add(loadKey)
        doneGet = true
      } catch {
        case e@(_: ObjectNotFoundException | _: KeyNotFoundException) => {
          logger.debug("In Container %s Key %s Not found for timerange: %d-%d".format(objFullName, loadKey.bucketKey.mkString(","), loadKey.tmRange.beginTime, loadKey.tmRange.endTime), e)
          doneGet = true
        }
        case e: FatalAdapterException => {
          logger.error("In Container %s Key %s Not found for timerange: %d-%d.".format(objFullName, loadKey.bucketKey.mkString(","), loadKey.tmRange.beginTime, loadKey.tmRange.endTime), e)
        }
        case e: StorageDMLException => {
          logger.error("In Container %s Key %s Not found for timerange: %d-%d.".format(objFullName, loadKey.bucketKey.mkString(","), loadKey.tmRange.beginTime, loadKey.tmRange.endTime), e)
        }
        case e: StorageDDLException => {
          logger.error("In Container %s Key %s Not found for timerange: %d-%d.".format(objFullName, loadKey.bucketKey.mkString(","), loadKey.tmRange.beginTime, loadKey.tmRange.endTime), e)
        }
        case e: Exception => {
          logger.error("In Container %s Key %s Not found for timerange: %d-%d.".format(objFullName, loadKey.bucketKey.mkString(","), loadKey.tmRange.beginTime, loadKey.tmRange.endTime), e)
        }
        case e: Throwable => {
          logger.error("In Container %s Key %s Not found for timerange: %d-%d.".format(objFullName, loadKey.bucketKey.mkString(","), loadKey.tmRange.beginTime, loadKey.tmRange.endTime), e)
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

  private def commitData(transId: Long, kvstore: DataStore, dataByBucketKeyPart: TreeMap[KeyWithBucketIdAndPrimaryKey, ContainerInterface], commitBatchSize: Int, processedRows: Int): Unit = {
    val storeObjects = new ArrayBuffer[(Key, String, Any)](dataByBucketKeyPart.size())
    var it1 = dataByBucketKeyPart.entrySet().iterator()
    while (it1.hasNext()) {
      val entry = it1.next();

      val value = entry.getValue();
      if (true /* value.modified */) {
        val key = entry.getKey();
        try {
          val k = entry.getKey().key
          storeObjects += ((k, "", value))
        } catch {
          case e: Exception => {
            logger.error("Failed to serialize/write data.", e)
            throw e
          }
        }
      }
    }

    var failedWaitTime = 15000 // Wait time starts at 15 secs
    val maxFailedWaitTime = 60000 // Max Wait time 60 secs
    var doneSave = false

    while (!doneSave) {
      try {
        kvstore.put(null, Array((objFullName, storeObjects.toArray)))
        doneSave = true
      } catch {
        case e: FatalAdapterException => {
          logger.error("Failed to save data into datastore", e)
        }
        case e: StorageDMLException => {
          logger.error("Failed to save data into datastore", e)
        }
        case e: StorageDDLException => {
          logger.error("Failed to save data into datastore", e)
        }
        case e: Exception => {
          logger.error("Failed to save data into datastore", e)
        }
        case e: Throwable => {
          logger.error("Failed to save data into datastore", e)
        }
      }

      if (!doneSave) {
        try {
          logger.error("Failed to save data into datastore. Waiting for another %d milli seconds and going to start them again.".format(failedWaitTime))
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

    val savedKeys = storeObjects.map(kv => kv._1)

    totalCommittedMsgs += storeObjects.size

    val msgStr = "%s: Inserted %d keys in this commit. Totals (Processed:%d, inserted:%d) so far".format(GetCurDtTmStr, storeObjects.size, processedRows, totalCommittedMsgs)
    logger.info(msgStr)
    println(msgStr)

    if (zkcForSetData != null) {
      logger.info("Notifying Engines after updating is done through Zookeeper.")
      try {
        val dataChangeZkNodePath = zkNodeBasePath + "/datachange"
        val changedContainersData = Map[String, List[Key]]()
        changedContainersData(typename) = savedKeys.toList
        val datachangedata = ("txnid" -> transId.toString) ~
          ("changeddatakeys" -> changedContainersData.map(kv =>
            ("C" -> kv._1) ~
              ("K" -> kv._2.map(k =>
                ("tm" -> k.timePartition) ~
                  ("bk" -> k.bucketKey.toList) ~
                  ("tx" -> k.transactionId) ~
                  ("rid" -> k.rowId)))))
        val sendJson = compact(render(datachangedata))
        zkcForSetData.setData().forPath(dataChangeZkNodePath, sendJson.getBytes("UTF8"))
      } catch {
        case e: Exception => {
          logger.error("Failed to send update notification to engine.", e)
          throw e
        }
      }
    } else {
      logger.error("Failed to send update notification to engine.")
    }
  }

  private def ResolveDeserializer(deserializer: String, optionsjson: String): MsgBindingInfo = {
    val serInfo = getMdMgr.GetSerializer(deserializer)
    if (serInfo == null) {
      throw new KamanjaException(s"Not found Serializer/Deserializer for ${deserializer}", null)
    }

    val phyName = serInfo.PhysicalName
    if (phyName == null) {
      throw new KamanjaException(s"Not found Physical name for Serializer/Deserializer for ${deserializer}", null)
    }

    try {
      val aclass = Class.forName(phyName).newInstance
      val ser = aclass.asInstanceOf[SerializeDeserialize]

      val map = new java.util.HashMap[String, String] //BUGBUG:: we should not convert the 2nd param to String. But still need to see how can we convert scala map to java map
      var options: collection.immutable.Map[String, Any] = null
      if (optionsjson != null) {
        implicit val jsonFormats: Formats = DefaultFormats
        val validJson = parse(optionsjson)

        options = validJson.values.asInstanceOf[collection.immutable.Map[String, Any]]
        if (options != null) {
          options.foreach(o => {
            map.put(o._1, o._2.toString)
          })
        }
      }
      ser.configure(this, map)
      ser.setObjectResolver(this)
      return MsgBindingInfo(deserializer, options, optionsjson, ser)
    } catch {
      case e: Throwable => {
        throw new KamanjaException(s"Failed to resolve Physical name ${phyName} in Serializer/Deserializer for ${deserializer}", e)
      }
    }

    return null // Should not come here
  }


  // If we have keyfieldnames.size > 0
  private def buildContainerOrMessage(kvstore: DataStore): Unit = {
    if (!isOk)
      return

    // kvstore.TruncateStore

    var processedRows: Int = 0
    var errsCnt: Int = 0
    var transId: Long = 0

    logger.debug("KeyFields:" + keyfieldnames.mkString(","))

    // The value for this is Boolean & ContainerInterface. Here Boolean represents it is changed in this transaction or loaded from previous file
    var dataByBucketKeyPart = new TreeMap[KeyWithBucketIdAndPrimaryKey, ContainerInterface](KvBaseDefalts.defualtBucketKeyComp) // By time, BucketKey, then PrimaryKey/{transactionid & rowid}. This is little cheaper if we are going to get exact match, because we compare time & then bucketid
    var loadedKeys = new java.util.TreeSet[LoadKeyWithBucketId](KvBaseDefalts.defaultLoadKeyComp) // By BucketId, BucketKey, Time Range

    var hasPrimaryKey = false
    var firstTime = true
    var transService: com.ligadata.transactions.SimpleTransService = null

    if (zkConnectString != null && zkNodeBasePath != null && zkConnectString.size > 0 && zkNodeBasePath.size > 0) {
      try {
        // TransactionId
        com.ligadata.transactions.NodeLevelTransService.init(zkConnectString, zkSessionTimeoutMs, zkConnectionTimeoutMs, zkNodeBasePath, 1, sysCatalogDataStoreInfo, KvInitConfiguration.jarPaths)
        transService = new com.ligadata.transactions.SimpleTransService
        transService.init(1)
        transId = transService.getNextTransId // Getting first transaction. It may get wasted if we don't have any lines to save.

        // ZK notifications
        val dataChangeZkNodePath = zkNodeBasePath + "/datachange"
        CreateClient.CreateNodeIfNotExists(zkConnectString, dataChangeZkNodePath) // Creating path if missing
        zkcForSetData = CreateClient.createSimple(zkConnectString, zkSessionTimeoutMs, zkConnectionTimeoutMs)
      } catch {
        case e: Exception => throw e
      }
    }

    val deserMsgBindingInfo = ResolveDeserializer(deserializer, optionsjson)

    if (deserMsgBindingInfo == null || deserMsgBindingInfo.serInstance == null) {
      throw new KamanjaException("Unable to resolve deserializer", null)
    }

    kvstore.setObjectResolver(this)
    kvstore.setDefaultSerializerDeserializer("com.ligadata.kamanja.serializer.kbinaryserdeser", scala.collection.immutable.Map[String, Any]())
    // kvstore.addMessageBinding(typename, deserMsgBindingInfo.serName, deserMsgBindingInfo.options)

    dataFiles.foreach(fl => {
      logger.info("%s: File:%s => About to process".format(GetCurDtTmStr, fl))
      val alldata: List[String] = fileData(fl, deserializer)
      logger.info("%s: File:%s => Lines in file:%d".format(GetCurDtTmStr, fl, alldata.size))

      var lnNo = 0

      alldata.foreach(inputStr => {
        lnNo += 1
        if (lnNo > ignoreRecords && inputStr.size > 0) {
          logger.debug("Record:" + inputStr)

          val container = deserMsgBindingInfo.serInstance.deserialize(inputStr.getBytes, typename)
          if (container != null) {

            if (firstTime) {
              firstTime = false
              hasPrimaryKey = container.hasPrimaryKey
            }

            container.setTransactionId(transId)

            try {
              val keyData = container.getPartitionKey

              val timeVal = container.getTimePartitionData
              container.setRowNumber(processedRows)

              val bucketId = KeyWithBucketIdAndPrimaryKeyCompHelper.BucketIdForBucketKey(keyData)
              val k = KeyWithBucketIdAndPrimaryKey(bucketId, Key(timeVal, keyData, transId, processedRows), hasPrimaryKey, if (hasPrimaryKey) container.getPrimaryKey else null)
              if (!isOnlyAppend && hasPrimaryKey) {
                // Get the record(s) for this partition key, time value & primary key
                val loadKey = LoadKeyWithBucketId(bucketId, TimeRange(timeVal, timeVal), keyData)
                LoadDataIfNeeded(loadKey, loadedKeys, dataByBucketKeyPart, kvstore)
              }

              dataByBucketKeyPart.put(k, container)
              processedRows += 1
              if (processedRows % commitBatchSize == 0) {
                logger.info("%s: Collected batch (%d) of values. About to insert".format(GetCurDtTmStr, commitBatchSize))
                // Get transaction and commit them
                commitData(transId, kvstore, dataByBucketKeyPart, commitBatchSize, processedRows)
                dataByBucketKeyPart.clear()
                loadedKeys.clear()
                // Getting new transactionid for next batch
                transId = transService.getNextTransId
                logger.info("%s: Inserted batch (%d) of values. Total processed so far:%d".format(GetCurDtTmStr, commitBatchSize, processedRows))

              }
            } catch {
              case e: Exception => {
                logger.debug("Failed to serialize/write data.", e)
                errsCnt += 1
              }
            }
          } else {
            logger.error("Deserialize returned null container.")
            errsCnt += 1
          }

          if (errsCnt > ignoreErrsCount) {
            val errStr = "Populate/Serialize errors (%d) exceed the given count(%d)." format(errsCnt, ignoreErrsCount)
            logger.error(errStr)
            throw new Exception(errStr)
          }
        }
      })
    })

    if (dataByBucketKeyPart.size > 0) {
      commitData(transId, kvstore, dataByBucketKeyPart, commitBatchSize, processedRows)
      dataByBucketKeyPart.clear()
      loadedKeys.clear()
    }

    val msgStr = "%s: Processed %d records and Inserted %d keys total for Type %s".format(GetCurDtTmStr, processedRows, totalCommittedMsgs, typename)
    logger.info(msgStr)
    println(msgStr)
  }

  private def fileData(inputeventfile: String, deserializer: String): List[String] = {
    var br: BufferedReader = null
    try {
      if (isCompressed(inputeventfile)) {
        br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(inputeventfile))))
      } else {
        br = new BufferedReader(new InputStreamReader(new FileInputStream(inputeventfile)))
      }
    } catch {
      case e: Exception => {
        logger.error("Failed to open Input File %s.".format(inputeventfile), e)
        throw e
      }
    }
    var fileContentsArray = ArrayBuffer[String]()

    try {
      if (deserializer.equalsIgnoreCase("com.ligadata.kamanja.serializer.csvserdeser")) {
        var line: String = ""
        while ( {
          line = br.readLine(); line != null
        }) {
          fileContentsArray += line
        }
      } else if (deserializer.equalsIgnoreCase("com.ligadata.kamanja.serializer.jsonserdeser")) {
        var buf = new ArrayBuffer[Int]()
        var ch = br.read()

        while (ch != -1) {
          buf.clear() // Reset
          // Finding start "{" char
          var foundOpen = false
          while (ch != -1 && foundOpen == false) {
            if (ch == '{')
              foundOpen = true;
            else {
              buf += ch
              ch = br.read()
            }
          }

          if (buf.size > 0) {
            val str = new String(buf.toArray, 0, buf.size)
            if (str.trim.size > 0)
              logger.error("Found invalid string in JSON file, which is not json String:" + str)
          }

          buf.clear() // Reset

          while (foundOpen && ch != -1) {
            if (ch == '}') {
              // Try the string now
              buf += ch
              ch = br.read()

              val possibleFullJsonStr = new String(buf.toArray, 0, buf.size)
              try {
                implicit val jsonFormats: Formats = DefaultFormats
                val validJson = parse(possibleFullJsonStr)
                // If we find valid json, that means we will take this json
                if (validJson != null) {
                  fileContentsArray += possibleFullJsonStr
                  buf.clear() // Reset
                  foundOpen = false
                } // else // Did not match the correct json even if we have one open brace. Tring for the next open one
              } catch {
                case e: Exception => {
                  logger.warn("", e)
                } // Not yet valid json
              }
            } else {
              buf += ch
              ch = br.read()
            }
          }
        }

        if (buf.size > 0) {
          val str = new String(buf.toArray, 0, buf.size)
          if (str.trim.size > 0)
            logger.error("Found invalid string in JSON file, which is not json String:" + str)
        }
      } else {
        // Un-handled format
        logger.error("Got unhandled deserializer :" + deserializer)
        throw new Exception("Got unhandled deserializer :" + deserializer)
      }
    } catch {
      case e: Exception => {
        logger.error("Failed to open Input File %s.".format(inputeventfile), e)
        throw e
      }
    }

    br.close();

    return fileContentsArray.toList
  }

  private def isCompressed(inputfile: String): Boolean = {
    var is: FileInputStream = null
    try {
      is = new FileInputStream(inputfile)
    } catch {
      case e: Exception =>
        logger.debug("", e)
        return false
    }

    val maxlen = 2
    val buffer = new Array[Byte](maxlen)
    val readlen = is.read(buffer, 0, maxlen)

    is.close() // Close before we really check and return the data

    if (readlen < 2)
      return false;

    val b0: Int = buffer(0)
    val b1: Int = buffer(1)

    val head = (b0 & 0xff) | ((b1 << 8) & 0xff00)

    return (head == GZIPInputStream.GZIP_MAGIC);
  }

  private val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

  private def SimpDateFmtTimeFromMs(tmMs: Long): String = {
    formatter.format(new java.util.Date(tmMs))
  }

  private def GetCurDtTmStr: String = {
    SimpDateFmtTimeFromMs(System.currentTimeMillis)
  }
}
