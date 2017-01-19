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

package com.ligadata.keyvaluestore

import java.io.File

import com.ligadata.KamanjaBase.NodeContext
import com.ligadata._
import com.ligadata.kamanja.metadata.AdapterInfo
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import com.ligadata.StorageBase.DataStore
import org.apache.logging.log4j._
import com.ligadata.keyvaluestore._
import com.ligadata.Utils.Utils._
import com.ligadata.Utils.{KamanjaClassLoader, KamanjaLoaderInfo}
import com.ligadata.StorageBase.StorageAdapterFactory

object KeyValueManager {
  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)
  private val kvMgrLoader = new KamanjaLoaderInfo

  // We will add more implementations here
  // so we can test  the system characteristics
  //
  def Get(jarPaths: collection.immutable.Set[String], datastoreConfig: String, nodeCtxt: NodeContext, adapterInfo: AdapterInfo, localStorageLoader: KamanjaLoaderInfo /* = null */): DataStore = {
    val adapterConfig = if (datastoreConfig != null) datastoreConfig.trim else ""

    if (adapterConfig.size == 0) {
      throw new Exception("Not found valid Storage Configuration.")
    }

    logger.debug("Storage configuration:" + adapterConfig)
    var parsed_json: Map[String, Any] = null
    try {
      val json = parse(adapterConfig)
      if (json == null || json.values == null) {
        logger.error("Failed to parse Storage JSON configuration string:" + adapterConfig)
        throw new Exception("Failed to parse Storage JSON configuration string:" + adapterConfig)
      }
      parsed_json = json.values.asInstanceOf[Map[String, Any]]
    } catch {
      case e: Exception => {
        logger.error("Failed to parse Storage JSON configuration string:%s.".format(adapterConfig), e)
        throw e
      }
    }

    val kvManagerLoader = if (localStorageLoader != null) localStorageLoader else kvMgrLoader

    val (className1, jarName, dependencyJars) = getClassNameJarNameDepJarsFromJson(parsed_json)
    if (logger.isDebugEnabled) logger.debug("className:%s, jarName:%s, dependencyJars:%s".format(className1, jarName, dependencyJars.mkString(",")))
    var allJars: collection.immutable.Set[String] = null
    if (dependencyJars != null && jarName != null && jarName.trim.size > 0) {
      allJars = dependencyJars.toSet + jarName
    } else if (dependencyJars != null && dependencyJars.size > 0) {
      allJars = dependencyJars.toSet
    } else if (jarName != null && jarName.trim.size > 0) {
      allJars = collection.immutable.Set(jarName)
    }

    val allJarsToBeValidated = scala.collection.mutable.Set[String]();

    if (allJars != null && allJars.size > 0) {
      allJarsToBeValidated ++= allJars.map(j => GetValidJarFile(jarPaths, j))
    }

    case class ResolveFileInfo(flPrefix: String, var resolvedFile: String, var resolvedFullFilePath: String, var isResolved: Boolean)
    val resolveFiles = collection.mutable.ArrayBuffer[ResolveFileInfo]()

    val storeType = parsed_json.getOrElse("StoreType", "").toString.trim.toLowerCase
    var className = ""

    storeType match {
      case "cassandra" => {
        className = "com.ligadata.keyvaluestore.CassandraAdapter$"
        resolveFiles += ResolveFileInfo("StorageDeps_", "", "", false)
        resolveFiles += ResolveFileInfo("KamanjaInternalDeps_", "", "", false)
      }
      case "hbase" => {
        className = "com.ligadata.keyvaluestore.HBaseAdapter$"
        resolveFiles += ResolveFileInfo("StorageDeps_", "", "", false)
        resolveFiles += ResolveFileInfo("HBaseExtDependencyLibs_", "", "", false)
        resolveFiles += ResolveFileInfo("KamanjaInternalDeps_", "", "", false)
      }
      case "hashmap" => {
        className = "com.ligadata.keyvaluestore.HashMapAdapter$"
        resolveFiles += ResolveFileInfo("StorageDeps_", "", "", false)
        resolveFiles += ResolveFileInfo("KamanjaInternalDeps_", "", "", false)
      }
      case "treemap" => {
        className = "com.ligadata.keyvaluestore.TreeMapAdapter$"
        resolveFiles += ResolveFileInfo("StorageDeps_", "", "", false)
        resolveFiles += ResolveFileInfo("KamanjaInternalDeps_", "", "", false)
      }
      case "sqlserver" => {
        className = "com.ligadata.keyvaluestore.SqlServerAdapter$"
        resolveFiles += ResolveFileInfo("StorageDeps_", "", "", false)
        resolveFiles += ResolveFileInfo("KamanjaInternalDeps_", "", "", false)
      }
      case "h2db" => {
        className = "com.ligadata.keyvaluestore.H2dbAdapter$"
        resolveFiles += ResolveFileInfo("StorageDeps_", "", "", false)
        resolveFiles += ResolveFileInfo("KamanjaInternalDeps_", "", "", false)
      }
      case "elasticsearch" => {
        className = "com.ligadata.keyvaluestore.ElasticsearchAdapter$"
        resolveFiles += ResolveFileInfo("StorageDeps_", "", "", false)
        resolveFiles += ResolveFileInfo("KamanjaInternalDeps_", "", "", false)
      }
      case _ => {
        className = className1
      }
    }

    var yetToBeResolved = resolveFiles.size
    var dirsIdx = 0

    jarPaths.foreach(dirName => {
      if (yetToBeResolved > 0) {
        val dir = new File(dirName)
        val files = dir.listFiles.filter(fl => fl.isFile)

        var flIdx = 0

        while (flIdx < files.size && yetToBeResolved > 0) {
          var i = 0
          var resolvedFl: String = null
          var recvFlIdx = -1
          val recvdFl = files(flIdx).getName
          val recvdFlPath = files(flIdx).getCanonicalPath
          while (resolvedFl == null && i < resolveFiles.size) {
            if (!resolveFiles(i).isResolved && recvdFl.startsWith(resolveFiles(i).flPrefix)) {
              resolvedFl = recvdFl
              recvFlIdx = i
            }
            i += 1
          }

          if (recvFlIdx >= 0) {
            resolveFiles(recvFlIdx).resolvedFile = resolvedFl
            resolveFiles(recvFlIdx).resolvedFullFilePath = recvdFlPath
            resolveFiles(recvFlIdx).isResolved = true
            yetToBeResolved -= 1
          }

          flIdx += 1
        }
      }
    })

    val resolvedFiles = resolveFiles.filter(info => info.isResolved).map(info => info.resolvedFullFilePath).toArray
    allJarsToBeValidated ++= resolvedFiles

    val nonExistsJars = CheckForNonExistanceJars(allJarsToBeValidated.toSet)
    if (nonExistsJars.size > 0) {
      logger.error("Not found jars in Storage Adapters Jars List : {" + nonExistsJars.mkString(", ") + "}")
      return null
    }

    if (allJarsToBeValidated != null && allJarsToBeValidated.size > 0) {
      if (LoadJars(allJarsToBeValidated.toArray, kvManagerLoader.loadedJars, kvManagerLoader.loader) == false)
        throw new Exception("Failed to add Jars")
    }

    if (logger.isDebugEnabled) logger.debug("Jars Loaded:" + allJarsToBeValidated.mkString(", "))

    try {
      Class.forName(className, true, kvManagerLoader.loader)
    } catch {
      case e: Exception => {
        logger.error("Failed to load Storage Adapter class %s".format(className), e)
        throw e // Rethrow
      }
    }

    // Convert class name into a class
    val clz = Class.forName(className, true, kvManagerLoader.loader)

    var isDs = false
    var curClz = clz

    while (curClz != null && isDs == false) {
      isDs = isDerivedFrom(curClz, "com.ligadata.StorageBase.StorageAdapterFactory")
      if (isDs == false)
        curClz = curClz.getSuperclass()
    }

    if (isDs) {
      try {
        val module = kvManagerLoader.mirror.staticModule(className)
        val obj = kvManagerLoader.mirror.reflectModule(module)

        val objinst = obj.instance
        if (objinst.isInstanceOf[StorageAdapterFactory]) {
          val storageAdapterObj = objinst.asInstanceOf[StorageAdapterFactory]
          return storageAdapterObj.CreateStorageAdapter(kvManagerLoader, datastoreConfig, nodeCtxt, adapterInfo)
        } else {
          logger.error("Failed to instantiate Storage Adapter with configuration:" + adapterConfig + ", objinst:" + objinst + " is not type of StorageAdapterFactory")
          return null
        }

      } catch {
        case e: Exception => {
          logger.error("Failed to instantiate Storage Adapter with configuration:" + adapterConfig, e)
          return null
        }
      }
    } else {
      logger.error("Failed to instantiate Storage Adapter with configuration:" + adapterConfig + ". " + className + " not found as derived from com.ligadata.StorageBase.StorageAdapterFactory")
      return null
    }

    val errMsg = "Failed to instantiate Storage Adapter with configuration:" + adapterConfig
    logger.error(errMsg)
    throw new Exception(errMsg)
  }

}
