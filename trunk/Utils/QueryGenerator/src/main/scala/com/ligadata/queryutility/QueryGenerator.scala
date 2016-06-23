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
         if (className.equalsIgnoreCase("KamanjaVertex")) extendsClass = "V"
         val createClassQuery = queryObj.createQuery(elementType = "class", className = className, setQuery = "", extendsClass = Option(extendsClass))
         val existFlag = queryObj.createclassInDB(conn, createClassQuery)
       if(existFlag == false){
         logger.info(createClassQuery)
         println(createClassQuery)
         if (className.equalsIgnoreCase("KamanjaVertex")) {
           val propertyList = queryObj.getAllProperty("KamanjaVertex")
           for (prop <- propertyList) {
             queryObj.executeQuery(conn, prop)
             logger.info(prop)
             println(prop)
           }
         }
       } else {
         logger.info("The %s class exsists".format(className))
         println("The %s class exsists".format(className))
       }
     }

     dataQuery = queryObj.getAllExistDataQuery(elementType = "class", extendClass = option("E"))
     data = queryObj.getAllClasses(conn, dataQuery)
     classesName = Array("KamanjaEdge", "MessageE", "Containers", "Messages", "Produces", "ConsumedBy", "StoredBy", "Retrieves", "SentTo")
     extendsClass = "KamanjaEdge"
     for (className <- classesName) {
       //if (!data.contains(className)) {
         if (className.equalsIgnoreCase("KamanjaEdge")) extendsClass = "E"
         val createClassQuery = queryObj.createQuery(elementType = "class", className = className, setQuery = "", extendsClass = Option(extendsClass))
         val existFlag = queryObj.createclassInDB(conn, createClassQuery)
         if(existFlag == false){
         logger.info(createClassQuery)
         println(createClassQuery)
         if (className.equalsIgnoreCase("KamanjaEdge")) {
           val propertyList = queryObj.getAllProperty("KamanjaEdge")
           for (prop <- propertyList) {
             queryObj.executeQuery(conn, prop)
             logger.info(prop)
             println(prop)
           }
         }
       } else {
         logger.info("The %s class exsists".format(className))
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
       logger.info("There are no adapter in metadata")
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
           logger.info(query)
           println(query)
         }
         else {
           logger.info("This adapter %s exsist in database".format(adapter._2.Name))
           println("This adapter %s exsist in database".format(adapter._2.Name))
         }
       }
     }

     if (containerDefs.isEmpty) {
       logger.info("There are no container in metadata")
       println("There are no container in metadata")
     } else {
       for (container <- containerDefs.get) {
         val setQuery = queryObj.createSetCommand(contianer = Option(container))
         val query: String = queryObj.createQuery(elementType = "vertex", className = "Container", setQuery = setQuery)
         //if(queryObj.checkObjexsist(conn,queryObj.checkQuery(elementType = "vertex", objName = container.Name, className = "container")) == false) {
         if (!verticesData.exists(_._2 == container.FullName)) {
           queryObj.executeQuery(conn, query)
           logger.info(query)
           println(query)
         } else {
           logger.info("This container %s exsist in".format(container.Name))
           println("This container %s exsist in database".format(container.name))
         }
       }
     }

     if (ModelDefs.isEmpty) {
       logger.info("There are no model in metadata")
       println("There are no model in metadata")
     } else {
       for (model <- ModelDefs.get) {
         val setQuery = queryObj.createSetCommand(model = Option(model))
         val query: String = queryObj.createQuery(elementType = "vertex", className = "Model", setQuery = setQuery)
         //if(queryObj.checkObjexsist(conn,queryObj.checkQuery(elementType = "vertex", objName = model.Name, className = "model")) == false) {
         if (!verticesData.exists(_._2 == model.FullName)) {
           queryObj.executeQuery(conn, query)
           logger.info(query)
           println(query)
         } else {
           logger.info("This model %s exsist in database".format(model.Name))
           println("This model %s exsist in database".format(model.name))
         }
//         val inputName = model.inputMsgSets
//         for (msg <- inputName)
//           for (msg1 <- msg) {
//             msg1.message.substring(msg1.message.lastIndexOf('.') + 1)
//             //println(" input message : " + msg1.message)
//           }
//
//         val outputName = model.outputMsgs
//         for (item <- outputName)
//           item.substring(item.lastIndexOf('.') + 1)
//         //println("output message : " + item)
       }
     }

     if (msgDefs.isEmpty) {
       logger.info("There are no messages in metadata")
       println("There are no messages in metadata")
     } else {
       for (message <- msgDefs.get) {
         val setQuery = queryObj.createSetCommand(message = Option(message))
         val query: String = queryObj.createQuery(elementType = "vertex", className = "Message", setQuery = setQuery)
         //if(queryObj.checkObjexsist(conn,queryObj.checkQuery(elementType = "vertex", objName = message.Name, className = "Message")) == false) {
         if (!verticesData.exists(_._2 == message.FullName)) {
           queryObj.executeQuery(conn, query)
           logger.info(query)
           println(query)
         } else {
           logger.info("This message %s exsist in database".format(message.Name))
           println("This message %s exsist in database".format(message.name))
         }
         // create an edge
       }
     }

     /* Step 3
      *  1- check all vertices
      *  2- check all existing Edges in graphDB
      *  3- add missing Edges to GraphDB
     */

     dataQuery = queryObj.getAllExistDataQuery(elementType = "vertex", extendClass = option("V"))
     val verticesDataNew = queryObj.getAllVertices(conn, dataQuery)
     dataQuery = queryObj.getAllExistDataQuery(elementType = "edge", extendClass = option("e"))
     val edgeData = queryObj.getAllEdges(conn, dataQuery)
     val adapterMessageMap: Map[String, AdapterMessageBinding] = mdMgr.AllAdapterMessageBindings //this includes all adapter and message for it
     for(adapterMessage <- adapterMessageMap) {
       var adapterId = ""
       var vertexId = ""
       //// DAG /////
       if (!ModelDefs.isEmpty) {
         for (model <- ModelDefs.get) {
           val inputName = model.inputMsgSets
           for (msg <- inputName)
             for (msg1 <- msg) {
               if (adapterMessage._2.messageName.equalsIgnoreCase(msg1.message)) {
                 for (vertex <- verticesDataNew) {
                   if (vertex._2.equalsIgnoreCase(adapterMessage._2.adapterName)) {
                     adapterId = vertex._1
                     //adapterId = adapterId.substring(adapterId.indexOf("#"), adapterId.indexOf("{"))
                   } //id of adpater
                   if (vertex._2.equalsIgnoreCase(model.FullName)) {
                     vertexId = vertex._1
                     // vertexId = vertexId.substring(vertexId.indexOf("#"), vertexId.indexOf("{"))
                   } //id of vertex
                 }
                 //   } here
                 if (adapterId.length != 0 && vertexId.length != 0) {
                   val linkKey = adapterId + "," + vertexId
                   if (!edgeData.contains(linkKey)) {
                     if (!msgDefs.isEmpty) {
                       for (message <- msgDefs.get) {
                         if (message.FullName.equalsIgnoreCase(adapterMessage._2.messageName)) {
                           if (!edgeData.contains(linkKey)) {
                             edgeData += (linkKey -> message.FullName)
                             val setQuery = queryObj.createSetCommand(message = Option(message))
                             val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkTo = Option(vertexId), linkFrom = Option(adapterId))
                             queryObj.executeQuery(conn, query)
                             logger.info(query)
                             println(query)
                           }
                         }
                       }
                     }
                   } else {
                     logger.info("The edge exist between this two nodes %s , %s".format(adapterId, vertexId))
                     println("The edge exist between this two nodes %s, %s".format(adapterId, vertexId))
                   }
                 }
               } //here

               //             msg1.message.substring(msg1.message.lastIndexOf('.') + 1)
               //println(" input message : " + msg1.message)
             }

           val outputName = model.outputMsgs
           for (item <- outputName) {
             //item.substring(item.lastIndexOf('.') + 1)
             //println("output message : " + item)
             //             var adapterId = ""
             //             var vertexId = ""
             if (adapterMessage._2.messageName.equalsIgnoreCase(item)) {
               for (vertex <- verticesDataNew) {
                 if (vertex._2.equalsIgnoreCase(adapterMessage._2.adapterName)) {
                   adapterId = vertex._1
                   // adapterId = adapterId.substring(adapterId.indexOf("#"), adapterId.indexOf("{"))
                 } //id of adpater
                 if (vertex._2.equalsIgnoreCase(model.FullName)) {
                   vertexId = vertex._1
                   //vertexId = vertexId.substring(vertexId.indexOf("#"),vertexId.indexOf("{"))
                 } //id of vertex
               }
               // } here
               if (adapterId.length != 0 && vertexId.length != 0) {
                 val linkKey = vertexId + "," + adapterId
                 if (!edgeData.contains(linkKey)) {
                   if (!msgDefs.isEmpty) {
                     for (message <- msgDefs.get) {
                       if (message.FullName.equalsIgnoreCase(item)) {
                         if (!edgeData.contains(linkKey)) {
                           edgeData += (linkKey -> message.FullName)
                           val setQuery = queryObj.createSetCommand(message = Option(message))
                           val query: String = queryObj.createQuery(elementType = "edge", className = "MessageE", setQuery = setQuery, linkFrom = Option(vertexId), linkTo = Option(adapterId))
                           queryObj.executeQuery(conn, query)
                           logger.info(query)
                           println(query)
                         }
                       }
                     }
                   }
                 } else {
                   logger.info("The edge exist between this two nodes %s , %s".format(vertexId, adapterId))
                   println("The edge exist between this two nodes %s, %s".format(vertexId, adapterId))
                 }
               }
             }
           }
         }
       }
       ////// high level ////////
       var messageid = ""
       for (vertex <- verticesDataNew) {
         if (vertex._2.equalsIgnoreCase(adapterMessage._2.adapterName)) {
           adapterId = vertex._1
          // adapterId = adapterId.substring(adapterId.indexOf("#"), adapterId.indexOf("{"))
         } //id of adpater
         if (vertex._2.equalsIgnoreCase(adapterMessage._2.messageName)) {
           messageid = vertex._1
         //  messageid = messageid.substring(messageid.indexOf("#"), messageid.indexOf("{"))
         } //id of message
       }

       if (adapterId.length != 0 && messageid.length != 0) {
         var linkKey = "" //adapterId + "," + vertexId
         var fromlink = ""
         var tolink = ""
    //     if (!edgeData.contains(linkKey)) {
           if (!adapterDefs.isEmpty) {
             for (adapter <- adapterDefs) {
               var adapterType: String = ""
               if (adapter._2.typeString.equalsIgnoreCase("input")) {
                 adapterType = "Produces"
                 linkKey = adapterId + "," + messageid
                 fromlink = adapterId
                 tolink = messageid
               } else if (adapter._2.typeString.equalsIgnoreCase("output")) {
                 adapterType = "SentTo"
                 linkKey = messageid + "," + adapterId
                 fromlink = messageid
                 tolink = adapterId
               } else {
                 adapterType = "StoredBy"
                 linkKey = messageid + "," + adapterId
                 fromlink = messageid
                 tolink = adapterId
               }

               if (!edgeData.contains(linkKey)) {
                 if (adapter._2.Name.equalsIgnoreCase(adapterMessage._2.adapterName)) {
                   if (!edgeData.contains(linkKey)) {
                     edgeData += (linkKey -> adapterType)
                     val setQuery = "set Name = \"%s\"".format(adapterType)
                     val query: String = queryObj.createQuery(elementType = "edge", className = adapterType, setQuery = setQuery, linkFrom = Option(fromlink), linkTo = Option(tolink))
                     queryObj.executeQuery(conn, query)
                     logger.info(query)
                     println(query)
                   }
                 }
               } else {
                 logger.info("The edge exist between this two nodes %s , %s".format(fromlink, tolink))
                 println("The edge exist between this two nodes %s, %s".format(fromlink, tolink))
               }
             }
           }
 //        }
       }
     }

     if(!ModelDefs.isEmpty) {
       for (model <- ModelDefs.get) {
         var messageId = ""
         var vertexId = ""
         val inputName = model.inputMsgSets
         for (msg <- inputName)
           for (msg1 <- msg) {
               for (vertex <- verticesDataNew) {
                 if (vertex._2.equalsIgnoreCase(msg1.message)) {
                   messageId = vertex._1
                //   messageId = messageId.substring(messageId.indexOf("#"), messageId.indexOf("{"))
                 } //id of adpater
                 if (vertex._2.equalsIgnoreCase(model.FullName)) {
                   vertexId = vertex._1
                //   vertexId = vertexId.substring(vertexId.indexOf("#"),vertexId.indexOf("{"))
                 } //id of vertex
               }
             if (messageId.length != 0 && vertexId.length != 0) {
               val linkKey = messageId + "," + vertexId
               if (!edgeData.contains(linkKey)) {
                 if (!msgDefs.isEmpty) {
                   for (message <- msgDefs.get) {
                     if (message.FullName.equalsIgnoreCase(msg1.message)) {
                       if (!edgeData.contains(linkKey)) {
                         edgeData += (linkKey -> msg1.message)
                         val setQuery = "set Name = \"%s\"".format("ConsumedBy")
                         val query: String = queryObj.createQuery(elementType = "edge", className = "ConsumedBy", setQuery = setQuery, linkTo = Option(vertexId), linkFrom = Option(messageId))
                         queryObj.executeQuery(conn, query)
                         logger.info(query)
                         println(query)
                       }
                     }
                   }
                 }
               } else {
                 logger.info("The edge exist between this two nodes %s , %s".format(messageId, vertexId))
                 println("The edge exist between this two nodes %s, %s".format(messageId, vertexId))
               }
             }

             //             msg1.message.substring(msg1.message.lastIndexOf('.') + 1)
             //println(" input message : " + msg1.message)
           }

         val outputName = model.outputMsgs
         for (item <- outputName) {
             for (vertex <- verticesDataNew) {
               if (vertex._2.equalsIgnoreCase(item)) {
                 messageId = vertex._1
               //  messageId = messageId.substring(messageId.indexOf("#"), messageId.indexOf("{"))
               } //id of adpater
             }
           if (messageId.length != 0 && vertexId.length != 0) {
             val linkKey = vertexId + "," + messageId
             if (!edgeData.contains(linkKey)) {
               if (!msgDefs.isEmpty) {
                 for (message <- msgDefs.get) {
                   if (message.FullName.equalsIgnoreCase(item)) {
                     if (!edgeData.contains(linkKey)) {
                       edgeData += (linkKey -> item)
                       val setQuery = "set Name = \"%s\"".format("Produces")
                       val query: String = queryObj.createQuery(elementType = "edge", className = "Produces", setQuery = setQuery, linkTo = Option(messageId), linkFrom = Option(vertexId))
                       queryObj.executeQuery(conn, query)
                       logger.info(query)
                       println(query)
                     }
                   }
                 }
               }
             } else {
               logger.info("The edge exist between this two nodes %s , %s".format(vertexId, messageId))
               println("The edge exist between this two nodes %s, %s".format(vertexId, messageId))
             }
           }
         }
       }

       if (!adapterDefs.isEmpty) {
         var messageId = ""
         var storage = ""
         for(adapter <- adapterDefs){
           if(adapter._2.TypeString.equalsIgnoreCase("storage")){
             if(!msgDefs.isEmpty){
               for (message <- msgDefs.get){
                 for (vertex <- verticesDataNew) {
                   if (vertex._2.equalsIgnoreCase(message.FullName)) {
                      messageId = vertex._1
                   } //id of message
                   else if(vertex._2.equalsIgnoreCase(adapter._2.Name)){
                      storage = vertex._1
                   }
                 }
                 val fromlink = messageId
                 val tolink = storage
                 val linkKey = messageId + "," + storage
                 if (!edgeData.contains(linkKey)) {
                   edgeData += (linkKey -> adapter._2.Name)
                   val setQuery = "set Name = \"%s\"".format("StoredBy")
                   val query: String = queryObj.createQuery(elementType = "edge", className = "StoredBy", setQuery = setQuery, linkFrom = Option(fromlink), linkTo = Option(tolink))
                   queryObj.executeQuery(conn, query)
                   logger.info(query)
                   println(query)
                 } else {
                   logger.info("The edge exist between this two nodes %s , %s".format(fromlink, tolink))
                   println("The edge exist between this two nodes %s, %s".format(fromlink, tolink))
                 }
               }
             }
           }
         }
       }
     }

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



