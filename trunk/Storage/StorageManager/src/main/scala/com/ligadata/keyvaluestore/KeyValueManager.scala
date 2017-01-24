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

import scala.collection.mutable.ArrayBuffer

object KeyValueManager {
  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)
  private val kvManagerLoaders = ArrayBuffer[KamanjaLoaderInfo]()

  private def hasFlagToPrependJarsBeforeSystemJars(parsed_json: Map[String, Any]): Boolean = {
    try {
      var prependJarsBeforeSystemJars = parsed_json.getOrElse("PrependJarsBeforeSystemJars", null)
      if (prependJarsBeforeSystemJars == null)
        prependJarsBeforeSystemJars = parsed_json.getOrElse("prependJarsBeforeSystemJars", null)
      if (prependJarsBeforeSystemJars == null)
        prependJarsBeforeSystemJars = parsed_json.getOrElse("prependjarsbeforesystemjars", null)

      if (prependJarsBeforeSystemJars != null) {
        return prependJarsBeforeSystemJars.toString.trim.equalsIgnoreCase("true")
      }
    } catch {
      case e: Exception => {}
      case e: Throwable => {}
    }

    return false
  }

  // We will add more implementations here
  // so we can test  the system characteristics
  //
  def Get(jarPaths: collection.immutable.Set[String], datastoreConfig: String, nodeCtxt: NodeContext, adapterInfo: AdapterInfo): DataStore = {
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

    val storeType = parsed_json.getOrElse("StoreType", "").toString.trim.toLowerCase

    val (className, jarName, dependencyJars) = getClassNameJarNameDepJarsFromJson(parsed_json)

    val prependJarsBeforeSystemJars = hasFlagToPrependJarsBeforeSystemJars(parsed_json)

    if (logger.isDebugEnabled) logger.debug("className:%s, jarName:%s, dependencyJars:%s".format(if (className != null) className else "", if (jarName != null) jarName else "", if (dependencyJars != null) dependencyJars.mkString(",") else ""))
    var allJars: collection.immutable.Set[String] = null
    if (dependencyJars != null && jarName != null && jarName.trim.size > 0) {
      allJars = dependencyJars.toSet + jarName
    } else if (dependencyJars != null) {
      allJars = dependencyJars.toSet
    } else if (jarName != null) {
      allJars = collection.immutable.Set(jarName)
    }

    val allJarsToBeValidated = scala.collection.mutable.Set[String]();

    if (allJars != null && allJars.size > 0) {
      allJarsToBeValidated ++= allJars.map(j => GetValidJarFile(jarPaths, j))
      val nonExistsJars = CheckForNonExistanceJars(allJarsToBeValidated.toSet)
      if (nonExistsJars.size > 0) {
        logger.error("Not found jars in Storage Adapters Jars List : {" + nonExistsJars.mkString(", ") + "}")
        return null
      }
    }

    val kvManagerLoader =
      if (prependJarsBeforeSystemJars) {
        val preprendedJars = if (allJars != null) allJars.map(j => GetValidJarFile(jarPaths, j)).toArray else Array[String]()
        new KamanjaLoaderInfo(null, false, prependJarsBeforeSystemJars, prependJarsBeforeSystemJars, preprendedJars)
      } else {
        val tmpKvLoader = new KamanjaLoaderInfo
        if (allJars != null) {
          if (LoadJars(allJars.map(j => GetValidJarFile(jarPaths, j)).toArray, tmpKvLoader.loadedJars, tmpKvLoader.loader) == false)
            throw new Exception("Failed to add Jars")
        }
        tmpKvLoader
      }

    kvManagerLoaders += kvManagerLoader

    storeType match {

      // Other KV stores
      case "cassandra" => return CassandraAdapter.CreateStorageAdapter(kvManagerLoader, datastoreConfig, nodeCtxt, adapterInfo)
      case "hbase" => return HBaseAdapter.CreateStorageAdapter(kvManagerLoader, datastoreConfig, nodeCtxt, adapterInfo)
      /*
      // Simple file base implementations
      case "redis" => return KeyValueRedis.CreateStorageAdapter(kvManagerLoader, datastoreConfig, tableName)
      */
      case "hashmap" => return HashMapAdapter.CreateStorageAdapter(kvManagerLoader, datastoreConfig, nodeCtxt, adapterInfo)
      case "treemap" => return TreeMapAdapter.CreateStorageAdapter(kvManagerLoader, datastoreConfig, nodeCtxt, adapterInfo)
      // Other relational stores such as sqlserver, mysql
      case "sqlserver" => return SqlServerAdapter.CreateStorageAdapter(kvManagerLoader, datastoreConfig, nodeCtxt, adapterInfo)
      case "h2db" => return H2dbAdapter.CreateStorageAdapter(kvManagerLoader, datastoreConfig, nodeCtxt, adapterInfo)
      // case "elasticsearch" => { // Fallthru instead of using this return ElasticsearchAdapter.CreateStorageAdapter(kvManagerLoader, datastoreConfig, nodeCtxt, adapterInfo) }
      // case "mysql" => return MySqlAdapter.CreateStorageAdapter(kvManagerLoader, datastoreConfig)

      // Default, Load it from Class
      case _ => {
        if (className != null && className.size > 0 && jarName != null && jarName.size > 0) {
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
                logger.error("Failed to instantiate Storage Adapter with configuration:" + adapterConfig)
                return null
              }

            } catch {
              case e: Exception => {
                logger.error("Failed to instantiate Storage Adapter with configuration:" + adapterConfig, e)
                return null
              }
            }
          } else {
            logger.error("Failed to instantiate Storage Adapter with configuration:" + adapterConfig)
            return null
          }
        }
      }
    }
    val errMsg = "Failed to instantiate Storage Adapter with configuration:" + adapterConfig
    logger.error(errMsg)
    throw new Exception(errMsg)
  }
}
