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

package com.ligadata.queryutility

import java.util.Properties

import com.ligadata.MetadataAPI.MetadataAPIImpl
import com.ligadata.Utils.{KamanjaLoaderInfo, Utils}
import com.ligadata.kamanja.metadata.MdMgr._
import com.ligadata.kamanja.metadata._
import org.apache.logging.log4j._
import java.sql.Connection

import org.json4s.native.JsonMethods._
import shapeless.option

import scala.collection.mutable.ArrayBuffer

trait LogTrait {
  val loggerName = this.getClass.getName()
  val logger = LogManager.getLogger(loggerName)
}


object QueryGenerator extends App with LogTrait {

  def usage: String = {
    """
Usage:  bash $KAMANJA_HOME/bin/QueryGenerator.sh --metadataconfig $KAMANJA_HOME/config/Metadataapi.properties --databaseconfig $KAMANJA_HOME/config/file.json
    """
  }

  private type OptionMap = Map[Symbol, Any]

  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    def isSwitch(s: String) = (s.charAt(0) == '-')
    list match {
      case Nil => map
      case "--metadataconfig" :: value :: tail =>
        nextOption(map ++ Map('metadataconfig -> value), tail)
      case "--databaseconfig" :: value :: tail =>
        nextOption(map ++ Map('databaseconfig -> value), tail)
      case "--recreate" :: tail =>
        nextOption(map ++ Map('recreate -> "true"), tail)
      case option :: tail => {
        logger.error("Unknown option " + option)
        logger.warn(usage)
        sys.exit(1)
      }
    }
  }

  override def main(args: Array[String]) {

    logger.debug("QueryGenerator.main begins")

    if (args.length == 0) {
      logger.error("Please pass the Engine1Config.properties file after --metadataconfig option and database config file after --databaseconfig")
      logger.warn(usage)
      sys.exit(1)
    }
    val options = nextOption(Map(), args.toList)

    val metadataConfig = options.getOrElse('metadataconfig, null).toString.trim
    if (metadataConfig == null || metadataConfig.toString().trim() == "") {
      logger.error("Please pass the Engine1Config.properties file after --metadataconfig option")
      logger.warn(usage)
      sys.exit(1)
    }

    val databaseConfig = options.getOrElse('databaseconfig, null).toString.trim
    if (databaseConfig == null || databaseConfig.toString().trim() == "") {
      logger.error("Please pass the database config file file after --databaseconfig option")
      logger.warn(usage)
      sys.exit(1)
    }

    KamanjaConfiguration.configFile = metadataConfig.toString

    val (loadConfigs, failStr) = Utils.loadConfiguration(KamanjaConfiguration.configFile, true)

    if (failStr != null && failStr.size > 0) {
      logger.error(failStr)
      sys.exit(1)
    }
    if (loadConfigs == null) {
      logger.error("Failed to load configurations from configuration file")
      sys.exit(1)
    }

    // KamanjaConfiguration.allConfigs = loadConfigs

    MetadataAPIImpl.InitMdMgrFromBootStrap(KamanjaConfiguration.configFile, false)

    val mdMgr = GetMdMgr

    val msgDefs: Option[scala.collection.immutable.Set[MessageDef]] = mdMgr.Messages(true, true)
    val containerDefs: Option[scala.collection.immutable.Set[ContainerDef]] = mdMgr.Containers(true, true)
    val ModelDefs: Option[scala.collection.immutable.Set[ModelDef]] = mdMgr.Models(true, true)
    val adapterDefs: Map[String, AdapterInfo] = mdMgr.Adapters

    val fileObj: FileUtility = new FileUtility
    if (fileObj.FileExist(databaseConfig) == false) {
      logger.error("This file %s does not exists".format(databaseConfig))
      logger.warn(usage)
      sys.exit(1)
    }

    val databasefileContent = fileObj.ReadFile(databaseConfig)

    if (databasefileContent == null || databasefileContent.size == 0) {
      // check if config file includes data
      logger.error("This file %s does not include data. Check your file please.".format(databaseConfig))
      logger.warn(usage)
      sys.exit(1)
    }

    val parsedConfig = fileObj.ParseFile(databasefileContent) //Parse config file
    val extractedInfo = fileObj.extractInfo(parsedConfig) //Extract information from parsed file
    val configBeanObj = fileObj.createConfigBeanObj(extractedInfo) // create a config object that store the result from extracting config file

    val queryObj: QueryBuilder = new QueryBuilder

    val conn: Connection = queryObj.getDBConnection(configBeanObj) //this used to fet a connection for orientDB

    val recreateFlag = options.getOrElse('recreate, "false").toString.trim.toBoolean

    /* Step 1
    *  1- check all existing classes in graphDB
    *  2- add missing classes to GraphDB
     */

    val verticesClassesName = Array("KamanjaVertex", "System", "Model", "Input", "Output", "Storage", "Container", "Message", "Inputs", "Stores", "Outputs", "Engine")
    val edgesClassesName = Array("KamanjaEdge", "MessageE", "Containers", "Messages", "Produces", "ConsumedBy", "StoredBy", "Retrieves", "SentTo")

    if (recreateFlag) {
      var idx = edgesClassesName.size - 1
      while (idx >= 0) {
        val classnm = edgesClassesName(idx)
        idx = idx - 1
        var commandsta = "delete edge " + classnm
        queryObj.executeQuery(conn, commandsta)
        logger.debug(commandsta)
        println(commandsta)
        commandsta = "drop class " + classnm
        queryObj.executeQuery(conn, commandsta)
        logger.debug(commandsta)
        println(commandsta)
      }

      idx = verticesClassesName.size - 1
      while (idx >= 0) {
        val classnm = verticesClassesName(idx)
        idx = idx - 1
        var commandsta = "delete vertex " + classnm
        queryObj.executeQuery(conn, commandsta)
        logger.debug(commandsta)
        println(commandsta)
        commandsta = "drop class " + classnm
        queryObj.executeQuery(conn, commandsta)
        logger.debug(commandsta)
        println(commandsta)
      }
    }

    var dataQuery = queryObj.getAllExistDataQuery(elementType = "class", extendClass = option("V"))
    var data = queryObj.getAllClasses(conn, dataQuery)
    var extendsClass = "KamanjaVertex"
    //    if(recreateFlag) {
    //      for (classnm <- verticesClassesName) {
    //        var commandsta = "delete vertex " + classnm
    //        queryObj.executeQuery(conn, commandsta)
    //        logger.debug(commandsta)
    //        println(commandsta)
    //        commandsta = "drop class " + classnm
    //        queryObj.executeQuery(conn, commandsta)
    //        logger.debug(commandsta)
    //        println(commandsta)
    //      }
    //    }
    for (className <- verticesClassesName) {
      // if (!data.contains(className)) {
      if (className.equalsIgnoreCase("KamanjaVertex")) extendsClass = "V" else extendsClass = "KamanjaVertex"
      val createClassQuery = queryObj.createQuery(elementType = "class", className = className, setQuery = "", extendsClass = Option(extendsClass))
      val existFlag = queryObj.createclassInDB(conn, createClassQuery)
      if (existFlag == false) {
        logger.debug(createClassQuery)
        println(createClassQuery)
        if (className.equalsIgnoreCase("KamanjaVertex")) {
          val propertyList = queryObj.getAllProperty("KamanjaVertex")
          for (prop <- propertyList) {
            queryObj.executeQuery(conn, prop)
            logger.debug(prop)
            println(prop)
          }
        }
      } else {
        logger.debug("The %s class exsists".format(className))
        println("The %s class exsists".format(className))
      }
    }

    dataQuery = queryObj.getAllExistDataQuery(elementType = "class", extendClass = option("E"))
    data = queryObj.getAllClasses(conn, dataQuery)
    //    if(recreateFlag) {
    //      for (classnm <- edgesClassesName) {
    //        var commandsta = "delete edge " + classnm
    //        queryObj.executeQuery(conn, commandsta)
    //        logger.debug(commandsta)
    //        println(commandsta)
    //        commandsta = "drop class " + classnm
    //        queryObj.executeQuery(conn, commandsta)
    //        logger.debug(commandsta)
    //        println(commandsta)
    //      }
    //    }
    extendsClass = "KamanjaEdge"
    for (className <- edgesClassesName) {
      //if (!data.contains(className)) {
      if (className.equalsIgnoreCase("KamanjaEdge")) extendsClass = "E" else extendsClass = "KamanjaEdge"
      val createClassQuery = queryObj.createQuery(elementType = "class", className = className, setQuery = "", extendsClass = Option(extendsClass))
      val existFlag = queryObj.createclassInDB(conn, createClassQuery)
      if (existFlag == false) {
        logger.debug(createClassQuery)
        println(createClassQuery)
        if (className.equalsIgnoreCase("KamanjaEdge")) {
          val propertyList = queryObj.getAllProperty("KamanjaEdge")
          for (prop <- propertyList) {
            queryObj.executeQuery(conn, prop)
            logger.debug(prop)
            println(prop)
          }
        }
      } else {
        logger.debug("The %s class exsists".format(className))
        println("The %s class exsists".format(className))
      }
    }

    /* Step 2
     *  1- check all existing Vertices in graphDB
     *  2- add missing Vertices to GraphDB
    */
    dataQuery = queryObj.getAllExistDataQuery(elementType = "vertex", extendClass = option("V"))
    val verticesByTypAndFullName = queryObj.getAllVertices(conn, dataQuery)
    val currentVerticesSet = scala.collection.mutable.Set[String]();
    val currentEdgesSet = scala.collection.mutable.Set[String]();
    val adapterTypes = scala.collection.mutable.Map[String, String]();

    {
      var systemVertex: String = ""
      val nm = ("System,System").toLowerCase
      currentVerticesSet += nm
      if (!verticesByTypAndFullName.contains(nm)) {
        val setQuery = "set Name = \"System\", FullName = \"System\", Tenant = \"System\""
        val query: String = queryObj.createQuery(elementType = "vertex", className = "System", setQuery = setQuery)
        queryObj.executeQuery(conn, query)
        logger.debug(query)
        println(query)
      }
      else {
        logger.debug("This adapter %s exsist in database".format("System"))
        println("This adapter %s exsist in database".format("System"))
      }
    }

    if (adapterDefs.isEmpty) {
      logger.debug("There are no adapter in metadata")
      println("There are no adapter in metadata")
    } else {
      for (adapter <- adapterDefs) {
        var adapterType: String = ""
        if (adapter._2.typeString.equalsIgnoreCase("input"))
          adapterType = "Input"
        else if (adapter._2.typeString.equalsIgnoreCase("output"))
          adapterType = "Output"
        else
          adapterType = "Storage"

        adapterTypes(adapter._2.Name.toLowerCase) = adapterType;

        val nm = (adapterType + "," + adapter._2.Name).toLowerCase
        currentVerticesSet += nm
        // if(queryObj.checkObjexsist(conn,queryObj.checkQuery(elementType = "vertex", objName = adapter._2.Name, className = adapterType)) == false) {
        if (!verticesByTypAndFullName.contains(nm)) {
          val setQuery = queryObj.createSetCommand(adapter = Option(adapter._2))
          val query: String = queryObj.createQuery(elementType = "vertex", className = adapterType, setQuery = setQuery)
          queryObj.executeQuery(conn, query)
          logger.debug(query)
          println(query)
        }
        else {
          logger.debug("This adapter %s exsist in database".format(adapter._2.Name))
          println("This adapter %s exsist in database".format(adapter._2.Name))
        }
      }
    }

    /*
        if (containerDefs.isEmpty) {
          logger.debug("There are no container in metadata")
          println("There are no container in metadata")
        } else {
          for (container <- containerDefs.get) {
            val nm = ("Container," + container.FullName).toLowerCase
            currentVerticesSet += nm
            //if(queryObj.checkObjexsist(conn,queryObj.checkQuery(elementType = "vertex", objName = container.Name, className = "container")) == false) {
            if (!verticesByTypAndFullName.contains(nm)) {
              val setQuery = queryObj.createSetCommand(baseElem = Option(container))
              val query: String = queryObj.createQuery(elementType = "vertex", className = "Container", setQuery = setQuery)
              queryObj.executeQuery(conn, query)
              logger.debug(query)
              println(query)
            } else {
              logger.debug("This container %s exsist in".format(container.FullName))
              println("This container %s exsist in database".format(container.FullName))
            }
          }
        }
    */

    if (ModelDefs.isEmpty) {
      logger.debug("There are no model in metadata")
      println("There are no model in metadata")
    } else {
      for (model <- ModelDefs.get) {
        val nm = ("Model," + model.FullName).toLowerCase
        currentVerticesSet += nm
        //if(queryObj.checkObjexsist(conn,queryObj.checkQuery(elementType = "vertex", objName = model.Name, className = "model")) == false) {
        if (!verticesByTypAndFullName.contains(nm)) {
          val setQuery = queryObj.createSetCommand(baseElem = Option(model))
          val query: String = queryObj.createQuery(elementType = "vertex", className = "Model", setQuery = setQuery)
          queryObj.executeQuery(conn, query)
          logger.debug(query)
          println(query)
        } else {
          logger.debug("This model %s exsist in database".format(model.FullName))
          println("This model %s exsist in database".format(model.FullName))
        }
      }
    }

    if (msgDefs.isEmpty) {
      logger.debug("There are no messages in metadata")
      println("There are no messages in metadata")
    } else {
      for (message <- msgDefs.get) {
        val nm = ("Message," + message.FullName).toLowerCase
        currentVerticesSet += nm
        //if(queryObj.checkObjexsist(conn,queryObj.checkQuery(elementType = "vertex", objName = message.Name, className = "Message")) == false) {
        if (!verticesByTypAndFullName.contains(nm)) {
          val setQuery = queryObj.createSetCommand(baseElem = Option(message))
          val query: String = queryObj.createQuery(elementType = "vertex", className = "Message", setQuery = setQuery)
          queryObj.executeQuery(conn, query)
          logger.debug(query)
          println(query)
        } else {
          logger.debug("This message %s exsist in database".format(message.FullName))
          println("This message %s exsist in database".format(message.FullName))
        }
        // create an edge
      }
    }

    val tenantId2FullName = scala.collection.mutable.Map[String, String]()

    val tenatInfo = mdMgr.GetAllTenantInfos
    if (tenatInfo.isEmpty) {
      logger.debug("There are no tenatInfo in metadata")
      println("There are no tenantInfo in metadata")
    } else {
      for (tenant <- tenatInfo) {
        var primaryStroage: String = ""
        if (tenant.primaryDataStore != null) {
          val json = parse(tenant.primaryDataStore)
          val adapCfgValues = json.values.asInstanceOf[Map[String, Any]]
          primaryStroage = if (adapCfgValues.get("StoreType").get.toString == null) "" else adapCfgValues.get("StoreType").get.toString
          val tenantName = tenant.tenantId + "_" + primaryStroage + "_primaryStroage"
          val nm = ("Storage," + tenantName).toLowerCase
          currentVerticesSet += nm
          tenantId2FullName += ((tenant.tenantId.toLowerCase -> tenantName))
          if (!verticesByTypAndFullName.contains(nm)) {
            val setQuery = queryObj.createSetCommand(tenant = option(tenant))
            val query: String = queryObj.createQuery(elementType = "vertex", className = "Storage", setQuery = setQuery)
            queryObj.executeQuery(conn, query)
            logger.debug(query)
            println(query)
          } else {
            logger.debug("This storage %s exsist in database".format(tenantName))
            println("This storage %s exsist in database".format(tenantName))
          }
        }
      }
    }

    /* Step 3
     *  1- check all vertices
     *  2- check all existing Edges in graphDB
     *  3- add missing Edges to GraphDB
    */

    dataQuery = queryObj.getAllExistDataQuery(elementType = "vertex", extendClass = option("V"))
    val verticesNewByTypAndFullName = queryObj.getAllVertices(conn, dataQuery)
    queryObj.PrintAllResult(verticesNewByTypAndFullName, "Vertices")
    dataQuery = queryObj.getAllExistDataQuery(elementType = "edge", extendClass = option("e"))
    val edgeData = queryObj.getAllEdges(conn, dataQuery)
    queryObj.PrintAllResult(edgeData, "Edges")
    val adapterMessageMap: Map[String, AdapterMessageBinding] = mdMgr.AllAdapterMessageBindings //this includes all adapter and message for it
    val messageProducers = scala.collection.mutable.Map[String, ArrayBuffer[String]]()
    val messageProperties = scala.collection.mutable.Map[String, String]()

    // First collect all messages produced from Message Producers. For now it is Models & Input Adatpers
    if (!ModelDefs.isEmpty) {
      for (model <- ModelDefs.get) {
        val mdlnm = ("Model," + model.FullName).toLowerCase
        val mdlVertexId = verticesNewByTypAndFullName.getOrElse(mdlnm, null)
        for (outmsgnm <- model.outputMsgs) {
          /// add link between message and model (model ===produces===> output message)
          if (outmsgnm != null) {
            val msgnm = ("Message," + outmsgnm).toLowerCase
            val msgVertexId = verticesNewByTypAndFullName.getOrElse(msgnm, null)
            if (msgVertexId != null && !msgVertexId.isEmpty) {
              val ab = messageProducers.getOrElse(msgVertexId, ArrayBuffer[String]())
              ab += mdlVertexId
              messageProducers(msgVertexId) = ab
            }
          }
        }
      }
    }

    for (adapterMessage <- adapterMessageMap) {
      // add link between message and input adapter, output adapter (input adapter ===produces===> message ===sendto===> output adapter)
      if (!adapterDefs.isEmpty) {
        val adapTyp = adapterTypes.getOrElse(adapterMessage._2.adapterName.toLowerCase, "")

        if (adapTyp.equalsIgnoreCase("input")) {
          val adapnm = (adapTyp + "," + adapterMessage._2.adapterName).toLowerCase
          val adapVertexId = verticesNewByTypAndFullName.getOrElse(adapnm, null)

          val msgnm = ("Message," + adapterMessage._2.messageName).toLowerCase
          val msgVertexId = verticesNewByTypAndFullName.getOrElse(msgnm, null)
          if (msgVertexId != null && !msgVertexId.isEmpty && adapVertexId != null && !adapVertexId.isEmpty) {
            val ab = messageProducers.getOrElse(msgVertexId, ArrayBuffer[String]())
            ab += adapVertexId
            messageProducers(msgVertexId) = ab
          }
        }
      }
    }

    if (!msgDefs.isEmpty) {
      // Get all system messages (generated from System)
      val sysnm = ("System,System").toLowerCase
      val sysVertexId = verticesNewByTypAndFullName.getOrElse(sysnm, null)
      if (sysVertexId != null && !sysVertexId.isEmpty) {
        for (messageDefObj <- msgDefs.get) {
          val msgTenant = if (messageDefObj.TenantId == null || messageDefObj.TenantId.isEmpty) "" else messageDefObj.TenantId
          if (msgTenant.isEmpty || msgTenant.equalsIgnoreCase("System")) {
            // This is making as System message
            val msgnm = ("Message," + messageDefObj.FullName).toLowerCase
            val msgVertexId = verticesNewByTypAndFullName.getOrElse(msgnm, null)
            if (msgVertexId != null && !msgVertexId.isEmpty) {
              val ab = messageProducers.getOrElse(msgVertexId, ArrayBuffer[String]())
              ab += sysVertexId
              messageProducers(msgVertexId) = ab

              // Adding edges for Universal view
              val linkKey = sysVertexId + "," + msgVertexId + ",Produces"
              currentEdgesSet += linkKey.toLowerCase
              if (!edgeData.contains(linkKey.toLowerCase)) {
                val setQuery = "set Name = \"Produces\""
                val query: String = queryObj.createQuery(elementType = "edge", className = "Produces", setQuery = setQuery, linkTo = Option(msgVertexId), linkFrom = Option(sysVertexId))
                queryObj.executeQuery(conn, query)
                logger.debug(query)
                println(query)
              } else {
                logger.debug("The edge exist between this two nodes %s , %s".format(sysVertexId, msgVertexId))
                println("The edge exist between this two nodes %s, %s".format(sysVertexId, msgVertexId))
              }
            }
          }
        }
      }

      for (messageDefObj <- msgDefs.get) {
        // add link between message and storage, input adapter, output adapter
        // add link between message and storage (message ===StoredBy===> storage ===retrieves===> message)
        val msgnm = ("Message," + messageDefObj.FullName).toLowerCase
        val msgVertexId = verticesNewByTypAndFullName.getOrElse(msgnm, null)
        if (msgVertexId != null && !msgVertexId.isEmpty && messageDefObj != null && messageDefObj.cType != null && messageDefObj.cType.persist) {
          val msgTenant = if (messageDefObj.TenantId.isEmpty) "" else messageDefObj.TenantId
          val tenantStoreName = tenantId2FullName.getOrElse(msgTenant.toLowerCase(), "")

          val storenm = ("Storage," + tenantStoreName).toLowerCase
          val storeVertexId = verticesNewByTypAndFullName.getOrElse(storenm, null)

          if (msgVertexId != null && !msgVertexId.isEmpty) {
            val setQuery = queryObj.createSetCommand(baseElem = Option(messageDefObj))
            messageProperties(msgVertexId) = setQuery;
          }

          if (storeVertexId != null &&
            !storeVertexId.isEmpty) {
            var linkKey = msgVertexId + "," + storeVertexId + ",StoredBy"
            currentEdgesSet += linkKey.toLowerCase
            if (!edgeData.contains(linkKey.toLowerCase)) {
              val setQuery = "set Name = \"%s\"".format("StoredBy")
              val query: String = queryObj.createQuery(elementType = "edge", className = "StoredBy", setQuery = setQuery, linkFrom = Option(msgVertexId), linkTo = Option(storeVertexId))
              queryObj.executeQuery(conn, query)
              logger.debug(query)
              println(query)
            } else {
              logger.debug("The edge exist between this two nodes %s , %s".format(msgVertexId, storeVertexId))
              println("The edge exist between this two nodes %s, %s".format(msgVertexId, storeVertexId))
            }
            /*
                        linkKey = storeVertexId + "," + msgVertexId + ",Retrieves"
                        currentEdgesSet += linkKey.toLowerCase
                        if (!edgeData.contains(linkKey.toLowerCase)) {
                          val setQuery = "set Name = \"%s\"".format("Retrieves")
                          val query: String = queryObj.createQuery(elementType = "edge", className = "Retrieves", setQuery = setQuery, linkFrom = Option(storeVertexId), linkTo = Option(msgVertexId))
                          queryObj.executeQuery(conn, query)
                          logger.debug(query)
                          println(query)
                        } else {
                          logger.debug("The edge exist between this two nodes %s , %s".format(msgVertexId, storeVertexId))
                          println("The edge exist between this two nodes %s, %s".format(msgVertexId, storeVertexId))
                        }
            */

            // DAG view
            // Get all Generators for this message
            val ab = messageProducers.getOrElse(msgVertexId, ArrayBuffer[String]())
            ab.foreach(inVertexId => {
              if (inVertexId != null && !inVertexId.isEmpty) {
                val linkKey = inVertexId + "," + storeVertexId + "," + messageDefObj.FullName
                currentEdgesSet += linkKey.toLowerCase
                if (!edgeData.contains(linkKey.toLowerCase)) {
                  val setQuery = messageProperties.getOrElse(msgVertexId, "set Name = \"%s\"".format(messageDefObj.FullName))
                  val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkFrom = Option(inVertexId), linkTo = Option(storeVertexId))
                  queryObj.executeQuery(conn, query)
                  logger.debug(query)
                  println(query)
                } else {
                  logger.debug("The edge exist between this two nodes %s , %s".format(inVertexId, storeVertexId))
                  println("The edge exist between this two nodes %s, %s".format(inVertexId, storeVertexId))
                }
              }
            })
          }
        } else {
          if (msgVertexId != null && !msgVertexId.isEmpty) {
            val setQuery = queryObj.createSetCommand(baseElem = Option(messageDefObj))
            messageProperties(msgVertexId) = setQuery;
          }
        }
      }
    }

    if (!ModelDefs.isEmpty) {
      for (model <- ModelDefs.get) {
        // add link between storage and model ( storage ===messageE===> model  && model ===messageE===> storage) //// DAG scenario/////
        // val modelTenant = if (model.TenantId.isEmpty) "" else model.TenantId
        // val tenantStoreName = tenantId2FullName.getOrElse(modelTenant.toLowerCase(), "")
        val mdlnm = ("Model," + model.FullName).toLowerCase
        val mdlVertexId = verticesNewByTypAndFullName.getOrElse(mdlnm, null)

        // For DAG
        if (model.inputMsgSets != null) {
          for (msgin <- model.inputMsgSets) {
            if (msgin != null) {
              /// add link between message and model (input message ===consumedby===> model)
              for (msg1 <- msgin) {
                if (msg1 != null) {
                  val msgnm = ("Message," + msg1.message).toLowerCase
                  val msgVertexId = verticesNewByTypAndFullName.getOrElse(msgnm, null)

                  // Get all Generators for this message
                  val ab = messageProducers.getOrElse(msgVertexId, ArrayBuffer[String]())
                  ab.foreach(inVertexId => {
                    if (inVertexId != null && !inVertexId.isEmpty) {
                      val linkKey = inVertexId + "," + mdlVertexId + "," + msg1.message
                      currentEdgesSet += linkKey.toLowerCase
                      if (!edgeData.contains(linkKey.toLowerCase)) {
                        val setQuery = messageProperties.getOrElse(msgVertexId, "set Name = \"%s\"".format(msg1.message))
                        val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkFrom = Option(inVertexId), linkTo = Option(mdlVertexId))
                        queryObj.executeQuery(conn, query)
                        logger.debug(query)
                        println(query)
                      } else {
                        logger.debug("The edge exist between this two nodes %s , %s".format(inVertexId, mdlVertexId))
                        println("The edge exist between this two nodes %s, %s".format(inVertexId, mdlVertexId))
                      }
                    }
                  })
                }
              }
            }
          }
        }

        if (model.inputMsgSets != null) {
          for (msgin <- model.inputMsgSets) {
            if (msgin != null) {
              /// add link between message and model (input message ===consumedby===> model)
              for (msg1 <- msgin) {
                if (msg1 != null) {
                  val msgnm = ("Message," + msg1.message).toLowerCase
                  val msgVertexId = verticesNewByTypAndFullName.getOrElse(msgnm, null)

                  if (msgVertexId != null && !msgVertexId.isEmpty) {
                    val linkKey = msgVertexId + "," + mdlVertexId + ",ConsumedBy"
                    currentEdgesSet += linkKey.toLowerCase
                    if (!edgeData.contains(linkKey.toLowerCase)) {
                      val setQuery = "set Name = \"ConsumedBy\""
                      val query: String = queryObj.createQuery(elementType = "edge", className = "ConsumedBy", setQuery = setQuery, linkFrom = Option(msgVertexId), linkTo = Option(mdlVertexId))
                      queryObj.executeQuery(conn, query)
                      logger.debug(query)
                      println(query)
                    } else {
                      logger.debug("The edge exist between this two nodes %s , %s".format(msgVertexId, mdlVertexId))
                      println("The edge exist between this two nodes %s, %s".format(msgVertexId, mdlVertexId))
                    }
                  }
                }
              }
            }
          }
        }

        val outputName = model.outputMsgs
        for (outmsgnm <- outputName) {
          /// add link between message and model (model ===produces===> output message)
          if (outmsgnm != null) {
            val msgnm = ("Message," + outmsgnm).toLowerCase
            val msgVertexId = verticesNewByTypAndFullName.getOrElse(msgnm, null)
            if (msgVertexId != null && !msgVertexId.isEmpty) {
              val linkKey = mdlVertexId + "," + msgVertexId + ",Produces"
              currentEdgesSet += linkKey.toLowerCase
              if (!edgeData.contains(linkKey.toLowerCase)) {
                val setQuery = "set Name = \"Produces\""
                val query: String = queryObj.createQuery(elementType = "edge", className = "Produces", setQuery = setQuery, linkTo = Option(msgVertexId), linkFrom = Option(mdlVertexId))
                queryObj.executeQuery(conn, query)
                logger.debug(query)
                println(query)
              } else {
                logger.debug("The edge exist between this two nodes %s , %s".format(mdlVertexId, msgVertexId))
                println("The edge exist between this two nodes %s, %s".format(mdlVertexId, msgVertexId))
              }
            }
          }
        }
      }
    }

    for (adapterMessage <- adapterMessageMap) {
      // add link between message and input adapter, output adapter (input adapter ===produces===> message ===sendto===> output adapter)
      if (!adapterDefs.isEmpty) {
        val adapTyp = adapterTypes.getOrElse(adapterMessage._2.adapterName.toLowerCase, "")
        val adapnm = (adapTyp + "," + adapterMessage._2.adapterName).toLowerCase
        val adapVertexId = verticesNewByTypAndFullName.getOrElse(adapnm, null)

        val msgnm = ("Message," + adapterMessage._2.messageName).toLowerCase
        val msgVertexId = verticesNewByTypAndFullName.getOrElse(msgnm, null)

        // println("AdapterName:%s, messageName:%s, adapTyp:%s, adapnm:%s, adapVertexId:%s, msgnm:%s, msgVertexId:%s".format(adapterMessage._2.adapterName, adapterMessage._2.messageName, adapTyp, adapnm, adapVertexId, msgnm, msgVertexId))

        if (adapVertexId != null && msgVertexId != null && !adapVertexId.isEmpty && !msgVertexId.isEmpty) {
          var linkKey = ""
          var fromlink = ""
          var tolink = ""

          var adapterType: String = ""
          if (adapTyp.equalsIgnoreCase("input")) {
            adapterType = "Produces"
            linkKey = adapVertexId + "," + msgVertexId + "," + adapterType
            fromlink = adapVertexId
            tolink = msgVertexId
          } else if (adapTyp.equalsIgnoreCase("output")) {
            adapterType = "SentTo"
            linkKey = msgVertexId + "," + adapVertexId + "," + adapterType
            fromlink = msgVertexId
            tolink = adapVertexId
          } else if (adapTyp.equalsIgnoreCase("Storage")) {
            adapterType = "StoredBy"
            linkKey = msgVertexId + "," + adapVertexId + "," + adapterType
            fromlink = msgVertexId
            tolink = adapVertexId
          }

          currentEdgesSet += linkKey.toLowerCase
          if (!edgeData.contains(linkKey.toLowerCase)) {
            val setQuery = "set Name = \"%s\"".format(adapterType)
            val query: String = queryObj.createQuery(elementType = "edge", className = adapterType, setQuery = setQuery, linkFrom = Option(fromlink), linkTo = Option(tolink))
            queryObj.executeQuery(conn, query)
            logger.debug(query)
            println(query)
          } else {
            logger.debug("The edge exist between this two nodes %s , %s".format(fromlink, tolink))
            println("The edge exist between this two nodes %s, %s".format(fromlink, tolink))
          }

          // For DAG
          if (!adapTyp.equalsIgnoreCase("input")) {
            // Get all Generators for this message
            val ab = messageProducers.getOrElse(msgVertexId, ArrayBuffer[String]())
            ab.foreach(inVertexId => {
              if (inVertexId != null && !inVertexId.isEmpty) {
                val linkKey = inVertexId + "," + adapVertexId + "," + adapterMessage._2.messageName
                currentEdgesSet += linkKey.toLowerCase
                if (!edgeData.contains(linkKey.toLowerCase)) {
                  val setQuery = messageProperties.getOrElse(msgVertexId, "set Name = \"%s\"".format(adapterMessage._2.messageName))
                  val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkFrom = Option(inVertexId), linkTo = Option(adapVertexId))
                  queryObj.executeQuery(conn, query)
                  logger.debug(query)
                  println(query)
                } else {
                  logger.debug("The edge exist between this two nodes %s , %s".format(inVertexId, adapVertexId))
                  println("The edge exist between this two nodes %s, %s".format(inVertexId, adapVertexId))
                }
              }
            })
          }
        }
      }
    }


    // Invalid Edges (Which are removed from previous run)
    val removedEdges = edgeData -- currentEdgesSet
    logger.debug("delete invalid edges...")
    println("delete invalid edges...")
    removedEdges.foreach(kv => {
      val rid = kv._2
      val queryCommand = "delete edge " + rid
      queryObj.executeQuery(conn, queryCommand)
      logger.debug(queryCommand)
      println(queryCommand)
      // Generate & Execute Delete Edge #rid
    })


    // Invalid Vertices (Which are removed from previous run)
    val removedVertices = verticesNewByTypAndFullName -- currentVerticesSet
    logger.debug("delete invalid vertices...")
    println("delete invalid vertices...")
    removedVertices.foreach(kv => {
      val rid = kv._2
      val queryCommand = "delete vertex " + rid
      queryObj.executeQuery(conn, queryCommand)
      logger.debug(queryCommand)
      println(queryCommand)
      // Generate & Execute Delete Vertex #rid
    })

    conn.close()
  }
}


object KamanjaConfiguration {
  var configFile: String = _
  //  var allConfigs: Properties = _
  //  var nodeId: Int = _
  //  var clusterId: String = _
  //  var nodePort: Int = _
  //  // Debugging info configs -- Begin
  //  var waitProcessingSteps = collection.immutable.Set[Int]()
  //  var waitProcessingTime = 0
  //  // Debugging info configs -- End
  //
  //  var shutdown = false
  //  var participentsChangedCntr: Long = 0
  //  var baseLoader = new KamanjaLoaderInfo
  //
  //  def Reset: Unit = {
  //    configFile = null
  //    allConfigs = null
  //    nodeId = 0
  //    clusterId = null
  //    nodePort = 0
  //    // Debugging info configs -- Begin
  //    waitProcessingSteps = collection.immutable.Set[Int]()
  //    waitProcessingTime = 0
  //    // Debugging info configs -- End
  //
  //    shutdown = false
  //    participentsChangedCntr = 0
  //  }
}



