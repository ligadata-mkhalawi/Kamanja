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

trait LogTrait {
  val loggerName = this.getClass.getName()
  val logger = LogManager.getLogger(loggerName)
}


object QueryGenerator extends App with LogTrait{

  def usage: String = {
    """
Usage:  bash $KAMANJA_HOME/bin/QueryGenerator.sh --metadataconfig $KAMANJA_HOME/config/Engine1Config.properties --databaseconfig $KAMANJA_HOME/config/file.json
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

     /* Step 1
     *  1- check all existing classes in graphDB
     *  2- add missing classes to GraphDB
      */

     var dataQuery = queryObj.getAllExistDataQuery(elementType = "class", extendClass = option("V"))
     var data = queryObj.getAllClasses(conn, dataQuery)
     var classesName = Array("KamanjaVertex", "Model", "Input", "Output", "Storage", "Container", "Message", "Inputs", "Stores", "Outputs", "Engine")
     var extendsClass = "KamanjaVertex"
     for (className <- classesName) {
      // if (!data.contains(className)) {
         if (className.equalsIgnoreCase("KamanjaVertex")) extendsClass = "V" else extendsClass = "KamanjaVertex"
         val createClassQuery = queryObj.createQuery(elementType = "class", className = className, setQuery = "", extendsClass = Option(extendsClass))
         val existFlag = queryObj.createclassInDB(conn, createClassQuery)
       if(existFlag == false){
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
     classesName = Array("KamanjaEdge", "MessageE", "Containers", "Messages", "Produces", "ConsumedBy", "StoredBy", "Retrieves", "SentTo")
     extendsClass = "KamanjaEdge"
     for (className <- classesName) {
       //if (!data.contains(className)) {
         if (className.equalsIgnoreCase("KamanjaEdge")) extendsClass = "E" else extendsClass = "KamanjaEdge"
         val createClassQuery = queryObj.createQuery(elementType = "class", className = className, setQuery = "", extendsClass = Option(extendsClass))
         val existFlag = queryObj.createclassInDB(conn, createClassQuery)
         if(existFlag == false){
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
     val verticesData = queryObj.getAllVertices(conn, dataQuery)

     if (adapterDefs.isEmpty) {
       logger.debug("There are no adapter in metadata")
       println("There are no adapter in metadata")
     } else {
       for (adapter <- adapterDefs) {
         var adapterType: String = ""
         val setQuery = queryObj.createSetCommand(adapter = Option(adapter._2))
         if (adapter._2.typeString.equalsIgnoreCase("input"))
           adapterType = "Input"
         else if(adapter._2.typeString.equalsIgnoreCase("output"))
           adapterType = "Output"
         else
           adapterType = "Storage"

         val query: String = queryObj.createQuery(elementType = "vertex", className = adapterType, setQuery = setQuery)
         // if(queryObj.checkObjexsist(conn,queryObj.checkQuery(elementType = "vertex", objName = adapter._2.Name, className = adapterType)) == false) {
         if (!verticesData.exists(_._2 == adapter._2.Name)) {
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

     if (containerDefs.isEmpty) {
       logger.debug("There are no container in metadata")
       println("There are no container in metadata")
     } else {
       for (container <- containerDefs.get) {
         val setQuery = queryObj.createSetCommand(contianer = Option(container))
         val query: String = queryObj.createQuery(elementType = "vertex", className = "Container", setQuery = setQuery)
         //if(queryObj.checkObjexsist(conn,queryObj.checkQuery(elementType = "vertex", objName = container.Name, className = "container")) == false) {
         if (!verticesData.exists(_._2 == container.FullName)) {
           queryObj.executeQuery(conn, query)
           logger.debug(query)
           println(query)
         } else {
           logger.debug("This container %s exsist in".format(container.Name))
           println("This container %s exsist in database".format(container.name))
         }
       }
     }

     if (ModelDefs.isEmpty) {
       logger.debug("There are no model in metadata")
       println("There are no model in metadata")
     } else {
       for (model <- ModelDefs.get) {
         val setQuery = queryObj.createSetCommand(model = Option(model))
         val query: String = queryObj.createQuery(elementType = "vertex", className = "Model", setQuery = setQuery)
         //if(queryObj.checkObjexsist(conn,queryObj.checkQuery(elementType = "vertex", objName = model.Name, className = "model")) == false) {
         if (!verticesData.exists(_._2 == model.FullName)) {
           queryObj.executeQuery(conn, query)
           logger.debug(query)
           println(query)
         } else {
           logger.debug("This model %s exsist in database".format(model.Name))
           println("This model %s exsist in database".format(model.name))
         }
       }
     }

     if (msgDefs.isEmpty) {
       logger.debug("There are no messages in metadata")
       println("There are no messages in metadata")
     } else {
       for (message <- msgDefs.get) {
         val setQuery = queryObj.createSetCommand(message = Option(message))
         val query: String = queryObj.createQuery(elementType = "vertex", className = "Message", setQuery = setQuery)
         //if(queryObj.checkObjexsist(conn,queryObj.checkQuery(elementType = "vertex", objName = message.Name, className = "Message")) == false) {
         if (!verticesData.exists(_._2 == message.FullName)) {
           queryObj.executeQuery(conn, query)
           logger.debug(query)
           println(query)
         } else {
           logger.debug("This message %s exsist in database".format(message.Name))
           println("This message %s exsist in database".format(message.name))
         }
         // create an edge
       }
     }

     val tenatInfo = mdMgr.GetAllTenantInfos
     if (tenatInfo.isEmpty){
       logger.debug("There are no tenatInfo in metadata")
       println("There are no tenantInfo in metadata")
     } else{
       for(tenant <- tenatInfo){
         val json = parse(tenant.primaryDataStore)
         val adapCfgValues = json.values.asInstanceOf[Map[String, Any]]
         val primaryStroage: String = if(adapCfgValues.get("StoreType").get.toString == null) "" else adapCfgValues.get("StoreType").get.toString
         val tenantName = tenant.tenantId + "_" + primaryStroage
         val setQuery = queryObj.createSetCommand(tenant = option(tenant))
         val query: String = queryObj.createQuery(elementType = "vertex", className = "Storage", setQuery = setQuery)
         if (!verticesData.exists(_._2 == tenantName)) {
           queryObj.executeQuery(conn, query)
           logger.debug(query)
           println(query)
         } else{
           logger.debug("This storage %s exsist in database".format(tenantName))
           println("This storage %s exsist in database".format(tenantName))
         }
       }
     }

     /* Step 3
      *  1- check all vertices
      *  2- check all existing Edges in graphDB
      *  3- add missing Edges to GraphDB
     */

     dataQuery = queryObj.getAllExistDataQuery(elementType = "vertex", extendClass = option("V"))
     val verticesDataNew = queryObj.getAllVertices(conn, dataQuery)
     queryObj.PrintAllResult(verticesDataNew, "Vertices")
     dataQuery = queryObj.getAllExistDataQuery(elementType = "edge", extendClass = option("e"))
     val edgeData = queryObj.getAllEdges(conn, dataQuery)
     queryObj.PrintAllResult(edgeData, "Edges")
     val adapterMessageMap: Map[String, AdapterMessageBinding] = mdMgr.AllAdapterMessageBindings //this includes all adapter and message for it

     if(!ModelDefs.isEmpty){
       for(model <- ModelDefs.get){ // add link between storage and model ( storage ===messageE===> model  && model ===messageE===> storage) //// DAG scenario/////
         for(tenant <- tenatInfo){
           var vertexId = ""
           var storageId = ""
           val modelTenant = if(model.TenantId.isEmpty) "" else model.TenantId
           val storageTenant = if(tenant.tenantId.isEmpty) "" else tenant.tenantId
           val json = parse(tenant.primaryDataStore)
           val adapCfgValues = json.values.asInstanceOf[Map[String, Any]]
           val primaryStroage: String = if(adapCfgValues.get("StoreType").get.toString == null) "" else adapCfgValues.get("StoreType").get.toString
           val tenantName = tenant.tenantId + "_" + primaryStroage
        //   if(modelTenant.equalsIgnoreCase(storageTenant)) {
             for (vertex <- verticesDataNew) {
               if (vertex._2.equalsIgnoreCase(model.FullName)) {
                 vertexId = vertex._1
               } //id of model
               if (vertex._2.equalsIgnoreCase(tenantName)) {
                 storageId = vertex._1
               } //id of storage
             }
             if (storageId.length != 0 && vertexId.length != 0) {
               var linkKey = storageId + "," + vertexId
               var fromlink = storageId
               var tolink = vertexId
               if (!edgeData.contains(linkKey)) {
                   edgeData += (linkKey -> "Message")
                   val setQuery = "set Name = \"%s\"".format("Message")
                   val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkFrom = Option(fromlink), linkTo = Option(tolink))
                   queryObj.executeQuery(conn, query)
                   logger.debug(query)
                   println(query)
               } else{
                 logger.debug("The edge exist between this two nodes %s , %s".format(fromlink, tolink))
                 println("The edge exist between this two nodes %s, %s".format(fromlink, tolink))
               }
               linkKey = vertexId + "," + storageId
               fromlink = vertexId
               tolink = storageId
               if (!edgeData.contains(linkKey)) {
                 edgeData += (linkKey -> "Message")
                 val setQuery = "set Name = \"%s\"".format("Message")
                 val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkFrom = Option(fromlink), linkTo = Option(tolink))
                 queryObj.executeQuery(conn, query)
                 logger.debug(query)
                 println(query)
               } else{
                 logger.debug("The edge exist between this two nodes %s , %s".format(fromlink, tolink))
                 println("The edge exist between this two nodes %s, %s".format(fromlink, tolink))
               }
             }
    //       }
         }

         for(adapterMessage <- adapterMessageMap){
           var vertexId = ""
           var adapterId = ""
           val inputMessages = model.inputMsgSets
           for(inputmsg <- inputMessages) { //add link between adapter and model (input ===messageE===> model ===messageE===> output) //// DAG scenario/////
             for(msg <- inputmsg){
               if(adapterMessage._2.messageName.equalsIgnoreCase(msg.message)){
                 for (vertex <- verticesDataNew) {
                   if (vertex._2.equalsIgnoreCase(model.FullName)) {
                     vertexId = vertex._1
                   } //id of model
                   if (vertex._2.equalsIgnoreCase(adapterMessage._2.adapterName)) {
                     adapterId = vertex._1
                   } //id of adapter
                 }
                 if (adapterId.length != 0 && vertexId.length != 0) {
                   val linkKey = adapterId + "," + vertexId
                   if (!edgeData.contains(linkKey)) {
//                     if (!msgDefs.isEmpty) {
//                       for (message <- msgDefs.get) {
//                         if (message.FullName.equalsIgnoreCase(adapterMessage._2.messageName)) {
//                           if (!edgeData.contains(linkKey)) {
//                             edgeData += (linkKey -> message.FullName)
//                             val setQuery = queryObj.createSetCommand(message = Option(message))
//                             val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkTo = Option(vertexId), linkFrom = Option(adapterId))
//                             queryObj.executeQuery(conn, query)
//                             logger.debug(query)
//                             println(query)
//                           }
//                         }
//                       }
//                     }
                     edgeData += (linkKey -> msg.message)
                     val setQuery = "set Name = \"%s\"".format(msg.message)
                     val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkFrom = Option(adapterId), linkTo = Option(vertexId))
                     queryObj.executeQuery(conn, query)
                     logger.debug(query)
                     println(query)
                   } else {
                     logger.debug("The edge exist between this two nodes %s , %s".format(adapterId, vertexId))
                     println("The edge exist between this two nodes %s, %s".format(adapterId, vertexId))
                   }
                 }
               }
             }
           }
           val outputName = model.outputMsgs
           for (item <- outputName) { // add link between model and output adapter (model ===messageE===> output) //// DAG scenario/////
             if (adapterMessage._2.messageName.equalsIgnoreCase(item)) {
               for (vertex <- verticesDataNew) {
                 if (vertex._2.equalsIgnoreCase(adapterMessage._2.adapterName)) {
                   adapterId = vertex._1
                 } //id of adpater
                 if (vertex._2.equalsIgnoreCase(model.FullName)) {
                   vertexId = vertex._1
                 } //id of vertex
               }
               if (adapterId.length != 0 && vertexId.length != 0) {
                 val linkKey = vertexId + "," + adapterId
                 if (!edgeData.contains(linkKey)) {
//                   if (!msgDefs.isEmpty) {
//                     for (message <- msgDefs.get) {
//                       if (message.FullName.equalsIgnoreCase(item)) {
//                         if (!edgeData.contains(linkKey)) {
//                           edgeData += (linkKey -> message.FullName)
//                           val setQuery = queryObj.createSetCommand(message = Option(message))
//                           val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkFrom = Option(vertexId), linkTo = Option(adapterId))
//                           queryObj.executeQuery(conn, query)
//                           logger.debug(query)
//                           println(query)
//                         }
//                       }
//                     }
//                   }
                   edgeData += (linkKey -> item)
                   val setQuery = "set Name = \"%s\"".format(item)
                   val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkFrom = Option(vertexId), linkTo = Option(adapterId))
                   queryObj.executeQuery(conn, query)
                   logger.debug(query)
                   println(query)
                 } else {
                   logger.debug("The edge exist between this two nodes %s , %s".format(vertexId, adapterId))
                   println("The edge exist between this two nodes %s, %s".format(vertexId, adapterId))
                 }
               }
             }
           }
         }
      //   if(!msgDefs.isEmpty){  /// add link between message and model (input message ===consumedby===> model && model ===produces===> output message)
      //     for (msg <- msgDefs.get){
             var messageId = ""
             var vertexId = ""
             val inputName = model.inputMsgSets
             for (msgin <- inputName)  /// add link between message and model (input message ===consumedby===> model)
               for (msg1 <- msgin) {
               //  if(msg1.message.equalsIgnoreCase(msg.FullName)) {
                   for (vertex <- verticesDataNew) {
                     if (vertex._2.equalsIgnoreCase(msg1.message)) {
                       messageId = vertex._1
                     } //id of adpater
                     if (vertex._2.equalsIgnoreCase(model.FullName)) {
                       vertexId = vertex._1
                     } //id of vertex
                   }
                   if (messageId.length != 0 && vertexId.length != 0) {
                     val linkKey = messageId + "," + vertexId
                     if (!edgeData.contains(linkKey)) {
                       //                     if (!msgDefs.isEmpty) {
                       //                       for (message <- msgDefs.get) {
                       //                         if (message.FullName.equalsIgnoreCase(msg1.message)) {
                       //                           if (!edgeData.contains(linkKey)) {
                       //                             edgeData += (linkKey -> msg1.message)
                       //                             val setQuery = "set Name = \"%s\"".format("ConsumedBy")
                       //                             val query: String = queryObj.createQuery(elementType = "edge", className = "ConsumedBy", setQuery = setQuery, linkTo = Option(vertexId), linkFrom = Option(messageId))
                       //                             queryObj.executeQuery(conn, query)
                       //                             logger.debug(query)
                       //                             println(query)
                       //                           }
                       //                         }
                       //                       }
                       //                     }
                       edgeData += (linkKey -> msg1.message)
                       val setQuery = "set Name = \"%s\"".format("ConsumedBy")
                       val query: String = queryObj.createQuery(elementType = "edge", className = "ConsumedBy", setQuery = setQuery, linkFrom = Option(messageId), linkTo = Option(vertexId))
                       queryObj.executeQuery(conn, query)
                       logger.debug(query)
                       println(query)
                     } else {
                       logger.debug("The edge exist between this two nodes %s , %s".format(messageId, vertexId))
                       println("The edge exist between this two nodes %s, %s".format(messageId, vertexId))
                     }
                   }
             //    }
               }
             val outputName = model.outputMsgs
             for (item <- outputName) {  /// add link between message and model (model ===produces===> output message)
               for (vertex <- verticesDataNew) {
                 if (vertex._2.equalsIgnoreCase(item)) {
                   messageId = vertex._1
                 } //id of adpater
                 if (vertex._2.equalsIgnoreCase(model.FullName)) {
                   vertexId = vertex._1
                 } //id of vertex
               }
               if (messageId.length != 0 && vertexId.length != 0) {
                 val linkKey = vertexId + "," + messageId
                 if (!edgeData.contains(linkKey)) {
//                   if (!msgDefs.isEmpty) {
//                     for (message <- msgDefs.get) {
//                       if (message.FullName.equalsIgnoreCase(item)) {
//                         if (!edgeData.contains(linkKey)) {
//                           edgeData += (linkKey -> item)
//                           val setQuery = "set Name = \"%s\"".format("Produces")
//                           val query: String = queryObj.createQuery(elementType = "edge", className = "Produces", setQuery = setQuery, linkTo = Option(messageId), linkFrom = Option(vertexId))
//                           queryObj.executeQuery(conn, query)
//                           logger.debug(query)
//                           println(query)
//                         }
//                       }
//                     }
//                   }
                   edgeData += (linkKey -> item)
                   val setQuery = "set Name = \"%s\"".format("Produces")
                   val query: String = queryObj.createQuery(elementType = "edge", className = "Produces", setQuery = setQuery, linkTo = Option(messageId), linkFrom = Option(vertexId))
                   queryObj.executeQuery(conn, query)
                   logger.debug(query)
                   println(query)
                 } else {
                   logger.debug("The edge exist between this two nodes %s , %s".format(vertexId, messageId))
                   println("The edge exist between this two nodes %s, %s".format(vertexId, messageId))
                 }
               }
             }
      //     }
      //   }
       }
     }

     if(!msgDefs.isEmpty){
       for(inputMessage <- msgDefs.get){ // add link between message and storage, input adapter, output adapter
         for (tenant <-tenatInfo){ // add link between message and storage (message ===StoredBy===> storage ===retrieves===> message)
         var vertexId = ""
           var storageId = ""
           val json = parse(tenant.primaryDataStore)
           val adapCfgValues = json.values.asInstanceOf[Map[String, Any]]
           val primaryStroage: String = if(adapCfgValues.get("StoreType").get.toString == null) "" else adapCfgValues.get("StoreType").get.toString
           val tenantName = tenant.tenantId + "_" + primaryStroage
           for (vertex <- verticesDataNew) {
             if (vertex._2.equalsIgnoreCase(inputMessage.FullName)) {
               vertexId = vertex._1
             } //id of message
             if (vertex._2.equalsIgnoreCase(tenantName)) {
               storageId = vertex._1
             } //id of storage
           }
           if (storageId.length != 0 && vertexId.length != 0) {
             var linkKey = vertexId + "," + storageId
             if (!edgeData.contains(linkKey)) {
               edgeData += (linkKey -> "StoredBy")
               val setQuery = "set Name = \"%s\"".format("StoredBy")
               val query: String = queryObj.createQuery(elementType = "edge", className = "StoredBy", setQuery = setQuery, linkFrom = Option(vertexId), linkTo = Option(storageId))
               queryObj.executeQuery(conn, query)
               logger.debug(query)
               println(query)
             } else{
               logger.debug("The edge exist between this two nodes %s , %s".format(vertexId, storageId))
               println("The edge exist between this two nodes %s, %s".format(vertexId, storageId))
             }
             linkKey = storageId + "," + vertexId
             if (!edgeData.contains(linkKey)) {
               edgeData += (linkKey -> "Retrieves")
               val setQuery = "set Name = \"%s\"".format("Retrieves")
               val query: String = queryObj.createQuery(elementType = "edge", className = "StoredBy", setQuery = setQuery, linkFrom = Option(storageId), linkTo = Option(vertexId))
               queryObj.executeQuery(conn, query)
               logger.debug(query)
               println(query)
             } else{
               logger.debug("The edge exist between this two nodes %s , %s".format(vertexId, storageId))
               println("The edge exist between this two nodes %s, %s".format(vertexId, storageId))
             }
           }
         }
         var linkKey = ""
         var fromlink = ""
         var tolink = ""
         var messageId = ""
         var adapterId = ""
         for (adapterMessage <- adapterMessageMap) { // add link between message and input adapter, output adapter (input adapter ===produces===> message ===sendto===> output adapter)
           if (!adapterDefs.isEmpty) {
             for (adapter <- adapterDefs) {
               if (adapterMessage._2.adapterName.equalsIgnoreCase(adapter._2.Name)) {
                 for (vertex <- verticesDataNew) {
                   if (vertex._2.equalsIgnoreCase(inputMessage.FullName)) {
                     messageId = vertex._1
                   } //id of message
                   if (vertex._2.equalsIgnoreCase(adapter._2.Name)) {
                     adapterId = vertex._1
                   } //id of storage
                 }
                 var adapterType: String = ""
                 if (adapter._2.typeString.equalsIgnoreCase("input")) {
                   adapterType = "Produces"
                   linkKey = adapterId + "," + messageId
                   fromlink = adapterId
                   tolink = messageId
                 } else if (adapter._2.typeString.equalsIgnoreCase("output")) {
                   adapterType = "SentTo"
                   linkKey = messageId + "," + adapterId
                   fromlink = messageId
                   tolink = adapterId
                 } else {
                   adapterType = "StoredBy"
                   linkKey = messageId + "," + adapterId
                   fromlink = messageId
                   tolink = adapterId
                 }
                 if (!edgeData.contains(linkKey)) {
                    if (inputMessage.FullName.equalsIgnoreCase(adapterMessage._2.adapterName)) {
                      if (!edgeData.contains(linkKey)) {
                        edgeData += (linkKey -> adapterType)
                        val setQuery = "set Name = \"%s\"".format(adapterType)
                        val query: String = queryObj.createQuery(elementType = "edge", className = adapterType, setQuery = setQuery, linkFrom = Option(fromlink), linkTo = Option(tolink))
                        queryObj.executeQuery(conn, query)
                        logger.debug(query)
                        println(query)
                      }
                    }
                 } else {
                   logger.debug("The edge exist between this two nodes %s , %s".format(fromlink, tolink))
                   println("The edge exist between this two nodes %s, %s".format(fromlink, tolink))
                 }
               }
             }
           }
         }
       }
     }


//     for(adapterMessage <- adapterMessageMap) {
//       var adapterId = ""
//       var vertexId = ""
//       if (!ModelDefs.isEmpty) { //add link between adapter and model (input ===messageE===> model ===messageE===> output)
//         for (model <- ModelDefs.get) {
//           val inputName = model.inputMsgSets
//           for (msg <- inputName)
//             for (msg1 <- msg) { // add link between model and input adapter (input ====messageE===> model)
//               if (adapterMessage._2.messageName.equalsIgnoreCase(msg1.message)) {
//                 for (vertex <- verticesDataNew) {
//                   if (vertex._2.equalsIgnoreCase(adapterMessage._2.adapterName)) {
//                     adapterId = vertex._1
//                   } //id of adpater
//                   if (vertex._2.equalsIgnoreCase(model.FullName)) {
//                     vertexId = vertex._1
//                   } //id of vertex
//                 }
//                 //   } here
//                 if (adapterId.length != 0 && vertexId.length != 0) {
//                   val linkKey = adapterId + "," + vertexId
//                   if (!edgeData.contains(linkKey)) {
//                     if (!msgDefs.isEmpty) {
//                       for (message <- msgDefs.get) {
//                         if (message.FullName.equalsIgnoreCase(adapterMessage._2.messageName)) {
//                           if (!edgeData.contains(linkKey)) {
//                             edgeData += (linkKey -> message.FullName)
//                             val setQuery = queryObj.createSetCommand(message = Option(message))
//                             val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkTo = Option(vertexId), linkFrom = Option(adapterId))
//                             queryObj.executeQuery(conn, query)
//                             logger.debug(query)
//                             println(query)
//                           }
//                         }
//                       }
//                     }
//                   } else {
//                     logger.debug("The edge exist between this two nodes %s , %s".format(adapterId, vertexId))
//                     println("The edge exist between this two nodes %s, %s".format(adapterId, vertexId))
//                   }
//                 }
//               } //here
//             }
//
//           val outputName = model.outputMsgs
//           for (item <- outputName) { // add link between model and output adapter (model ===messageE===> output)
//             if (adapterMessage._2.messageName.equalsIgnoreCase(item)) {
//               for (vertex <- verticesDataNew) {
//                 if (vertex._2.equalsIgnoreCase(adapterMessage._2.adapterName)) {
//                   adapterId = vertex._1
//                 } //id of adpater
//                 if (vertex._2.equalsIgnoreCase(model.FullName)) {
//                   vertexId = vertex._1
//                 } //id of vertex
//               }
//               // } here
//               if (adapterId.length != 0 && vertexId.length != 0) {
//                 val linkKey = vertexId + "," + adapterId
//                 if (!edgeData.contains(linkKey)) {
//                   if (!msgDefs.isEmpty) {
//                     for (message <- msgDefs.get) {
//                       if (message.FullName.equalsIgnoreCase(item)) {
//                         if (!edgeData.contains(linkKey)) {
//                           edgeData += (linkKey -> message.FullName)
//                           val setQuery = queryObj.createSetCommand(message = Option(message))
//                           val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkFrom = Option(vertexId), linkTo = Option(adapterId))
//                           queryObj.executeQuery(conn, query)
//                           logger.debug(query)
//                           println(query)
//                         }
//                       }
//                     }
//                   }
//                 } else {
//                   logger.debug("The edge exist between this two nodes %s , %s".format(vertexId, adapterId))
//                   println("The edge exist between this two nodes %s, %s".format(vertexId, adapterId))
//                 }
//               }
//             }
//           }
//         }
//       }
//       ////// high level ////////
//       var messageid = ""
//       for (vertex <- verticesDataNew) {
//         if (vertex._2.equalsIgnoreCase(adapterMessage._2.adapterName)) {
//           adapterId = vertex._1
//         } //id of adpater
//         if (vertex._2.equalsIgnoreCase(adapterMessage._2.messageName)) {
//           messageid = vertex._1
//         } //id of message
//       }
//
//       if (adapterId.length != 0 && messageid.length != 0) {
//         var linkKey = "" //adapterId + "," + vertexId
//         var fromlink = ""
//         var tolink = ""
//    //     if (!edgeData.contains(linkKey)) {
//           if (!adapterDefs.isEmpty) {
//             for (adapter <- adapterDefs) {
//               var adapterType: String = ""
//               if (adapter._2.typeString.equalsIgnoreCase("input")) {
//                 adapterType = "Produces"
//                 linkKey = adapterId + "," + messageid
//                 fromlink = adapterId
//                 tolink = messageid
//               } else if (adapter._2.typeString.equalsIgnoreCase("output")) {
//                 adapterType = "SentTo"
//                 linkKey = messageid + "," + adapterId
//                 fromlink = messageid
//                 tolink = adapterId
//               } else {
//                 adapterType = "StoredBy"
//                 linkKey = messageid + "," + adapterId
//                 fromlink = messageid
//                 tolink = adapterId
//               }
//
//               if (!edgeData.contains(linkKey)) {
//                 if (adapter._2.Name.equalsIgnoreCase(adapterMessage._2.adapterName)) {
//                   if (!edgeData.contains(linkKey)) {
//                     edgeData += (linkKey -> adapterType)
//                     val setQuery = "set Name = \"%s\"".format(adapterType)
//                     val query: String = queryObj.createQuery(elementType = "edge", className = adapterType, setQuery = setQuery, linkFrom = Option(fromlink), linkTo = Option(tolink))
//                     queryObj.executeQuery(conn, query)
//                     logger.debug(query)
//                     println(query)
//                   }
//                 }
//               } else {
//                 logger.debug("The edge exist between this two nodes %s , %s".format(fromlink, tolink))
//                 println("The edge exist between this two nodes %s, %s".format(fromlink, tolink))
//               }
//             }
//           }
// //        }
//       }
//     }

//     if(!ModelDefs.isEmpty) { ////// from message to model (input message ===consumedby===> model &&& model ===produced===> output message)
//       for (model <- ModelDefs.get) {
//         var messageId = ""
//         var vertexId = ""
//         val inputName = model.inputMsgSets
//         for (msg <- inputName)
//           for (msg1 <- msg) {
//               for (vertex <- verticesDataNew) {
//                 if (vertex._2.equalsIgnoreCase(msg1.message)) {
//                   messageId = vertex._1
//                 } //id of adpater
//                 if (vertex._2.equalsIgnoreCase(model.FullName)) {
//                   vertexId = vertex._1
//                 } //id of vertex
//               }
//             if (messageId.length != 0 && vertexId.length != 0) {
//               val linkKey = messageId + "," + vertexId
//               if (!edgeData.contains(linkKey)) {
//                 if (!msgDefs.isEmpty) {
//                   for (message <- msgDefs.get) {
//                     if (message.FullName.equalsIgnoreCase(msg1.message)) {
//                       if (!edgeData.contains(linkKey)) {
//                         edgeData += (linkKey -> msg1.message)
//                         val setQuery = "set Name = \"%s\"".format("ConsumedBy")
//                         val query: String = queryObj.createQuery(elementType = "edge", className = "ConsumedBy", setQuery = setQuery, linkTo = Option(vertexId), linkFrom = Option(messageId))
//                         queryObj.executeQuery(conn, query)
//                         logger.debug(query)
//                         println(query)
//                       }
//                     }
//                   }
//                 }
//               } else {
//                 logger.debug("The edge exist between this two nodes %s , %s".format(messageId, vertexId))
//                 println("The edge exist between this two nodes %s, %s".format(messageId, vertexId))
//               }
//             }
//           }
//
//         val outputName = model.outputMsgs
//         for (item <- outputName) {
//             for (vertex <- verticesDataNew) {
//               if (vertex._2.equalsIgnoreCase(item)) {
//                 messageId = vertex._1
//               } //id of adpater
//             }
//           if (messageId.length != 0 && vertexId.length != 0) {
//             val linkKey = vertexId + "," + messageId
//             if (!edgeData.contains(linkKey)) {
//               if (!msgDefs.isEmpty) {
//                 for (message <- msgDefs.get) {
//                   if (message.FullName.equalsIgnoreCase(item)) {
//                     if (!edgeData.contains(linkKey)) {
//                       edgeData += (linkKey -> item)
//                       val setQuery = "set Name = \"%s\"".format("Produces")
//                       val query: String = queryObj.createQuery(elementType = "edge", className = "Produces", setQuery = setQuery, linkTo = Option(messageId), linkFrom = Option(vertexId))
//                       queryObj.executeQuery(conn, query)
//                       logger.debug(query)
//                       println(query)
//                     }
//                   }
//                 }
//               }
//             } else {
//               logger.debug("The edge exist between this two nodes %s , %s".format(vertexId, messageId))
//               println("The edge exist between this two nodes %s, %s".format(vertexId, messageId))
//             }
//           }
//         }
//       }
//
//       if (!adapterDefs.isEmpty) {
//         var messageId = ""
//         var storage = ""
//         for(adapter <- adapterDefs){
//           if(adapter._2.TypeString.equalsIgnoreCase("storage")){
//             if(!msgDefs.isEmpty){
//               for (message <- msgDefs.get){
//                 for (vertex <- verticesDataNew) {
//                   if (vertex._2.equalsIgnoreCase(message.FullName)) {
//                      messageId = vertex._1
//                   } //id of message
//                   else if(vertex._2.equalsIgnoreCase(adapter._2.Name)){
//                      storage = vertex._1
//                   }
//                 }
//                 val fromlink = messageId
//                 val tolink = storage
//                 val linkKey = messageId + "," + storage
//                 if (!edgeData.contains(linkKey)) {
//                   edgeData += (linkKey -> adapter._2.Name)
//                   val setQuery = "set Name = \"%s\"".format("StoredBy")
//                   val query: String = queryObj.createQuery(elementType = "edge", className = "StoredBy", setQuery = setQuery, linkFrom = Option(fromlink), linkTo = Option(tolink))
//                   queryObj.executeQuery(conn, query)
//                   logger.debug(query)
//                   println(query)
//                 } else {
//                   logger.debug("The edge exist between this two nodes %s , %s".format(fromlink, tolink))
//                   println("The edge exist between this two nodes %s, %s".format(fromlink, tolink))
//                 }
//               }
//             }
//           }
//         }
//       }
//     }

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



