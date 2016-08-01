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

package com.ligadata.scalabilityutility


import org.apache.logging.log4j._
import java.sql.Connection
import shapeless.option

trait LogTrait {
  val loggerName = this.getClass.getName()
  val logger = LogManager.getLogger(loggerName)
}


object ScalabilityTesting extends App with LogTrait {

  def usage: String = {
    """
Usage:  bash $KAMANJA_HOME/bin/ScalabilityTesting.sh --databaseconfig $KAMANJA_HOME/config/file.json
    """
  }

  private type OptionMap = Map[Symbol, Any]

  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    def isSwitch(s: String) = (s.charAt(0) == '-')
    list match {
      case Nil => map
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

    logger.debug("ScalabilityTesting.main begins")

    if (args.length == 0) {
      logger.error("Please pass database config file after --databaseconfig")
      logger.warn(usage)
      sys.exit(1)
    }
    val options = nextOption(Map(), args.toList)

    val databaseConfig = options.getOrElse('databaseconfig, null).toString.trim
    if (databaseConfig == null || databaseConfig.toString().trim() == "") {
      logger.error("Please pass the database config file file after --databaseconfig option")
      logger.warn(usage)
      sys.exit(1)
    }

    val fileObj: FileUtility = new FileUtility
    if (fileObj.FileExist(databaseConfig) == false) {
      logger.error("This file %s does not exists".format(databaseConfig))
      logger.warn(usage)
      sys.exit(1)
    }

    val databasefileContent = fileObj.ReadFile(databaseConfig)

    if (databasefileContent == null || databasefileContent.size == 0) {// check if config file includes data
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

    if (recreateFlag) {
      var commandsta = "delete edge " + "testedge"
      queryObj.executeQuery(conn, commandsta)
      logger.debug(commandsta)
      println(commandsta)
      commandsta = "drop class " + "testedge"
      queryObj.executeQuery(conn, commandsta)
      logger.debug(commandsta)
      println(commandsta)
      commandsta = "delete vertex " + "testvertex"
      queryObj.executeQuery(conn, commandsta)
      logger.debug(commandsta)
      println(commandsta)
      commandsta = "drop class " + "testvertex"
      queryObj.executeQuery(conn, commandsta)
      logger.debug(commandsta)
      println(commandsta)
    }

    var className = "testvertex"
    var dataQuery = queryObj.getAllExistDataQuery(elementType = "class", extendClass = option("V"))
    var data = queryObj.getAllClasses(conn, dataQuery)
    var createClassQuery = queryObj.createQuery(elementType = "class", className = className , setQuery = "", extendsClass = Option("V"))
    var existFlag = queryObj.createclassInDB(conn, createClassQuery)
    if (existFlag == false) {
      logger.debug(createClassQuery)
      println(createClassQuery)
      val propertyList = queryObj.getAllProperty(className)
      for (prop <- propertyList) {
        queryObj.executeQuery(conn, prop)
        logger.debug(prop)
        println(prop)
        }
    } else {
      logger.debug("The %s class exsists".format(className))
      println("The %s class exsists".format(className))
    }

    className = "testedge"
    dataQuery = queryObj.getAllExistDataQuery(elementType = "class", extendClass = option("E"))
    data = queryObj.getAllClasses(conn, dataQuery)
    createClassQuery = queryObj.createQuery(elementType = "class", className = className, setQuery = "", extendsClass = Option("E"))
    existFlag = queryObj.createclassInDB(conn, createClassQuery)
    if (existFlag == false) {
      logger.debug(createClassQuery)
      println(createClassQuery)
      val propertyList = queryObj.getAllProperty(className)
      for (prop <- propertyList) {
        queryObj.executeQuery(conn, prop)
        logger.debug(prop)
        println(prop)
        }
    } else {
      logger.debug("The %s class exsists".format(className))
      println("The %s class exsists".format(className))
    }

    /* Step 2
     *  1- check all existing Vertices in graphDB
     *  2- add missing Vertices to GraphDB
    */
    dataQuery = queryObj.getAllExistDataQuery(elementType = "vertex", extendClass = option("V"))
    val verticesByTypAndFullName = queryObj.getAllVertices(conn, dataQuery)
    val currentVerticesSet = scala.collection.mutable.Set[String]();
    val currentEdgesSet = scala.collection.mutable.Set[String]();

    for (item <- 1 to configBeanObj.numberOfVertices){
      val nm = ("testvertex" + "," + "v"+item).toLowerCase
      currentVerticesSet += nm
      if (!verticesByTypAndFullName.contains(nm)) {
        val setQuery = queryObj.createSetCommand(item, "v"+item, "v"+item)
        val query: String = queryObj.createQuery(elementType = "vertex", className = "testvertex", setQuery = setQuery)
        queryObj.executeQuery(conn, query)
        logger.debug(query)
        println(query)
      }
      else {
        logger.debug("This adapter %s exsist in database".format("v"+item))
        println("This adapter %s exsist in database".format("v"+item))
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

    for (item <- 1 to configBeanObj.numberOfVertices) {
      for(index <- 1 to configBeanObj.numberOfEdges) {
        val fromVer = ("testvertex," + "v" + item).toLowerCase
        val toVer = ("testvertex," + "v" + (item+index)).toLowerCase
        val fromVertexId = verticesNewByTypAndFullName.getOrElse(fromVer, null)
        val toVertexId = verticesNewByTypAndFullName.getOrElse(toVer, null)
        var linkKey = fromVertexId + "," + toVertexId + ",like"
        currentEdgesSet += linkKey.toLowerCase
        if (!edgeData.contains(linkKey.toLowerCase) && fromVertexId != null && toVertexId != null) {
          val setQuery = "set Name = \"like\""
          val query: String = queryObj.createQuery(elementType = "edge", className = "testedge", setQuery = setQuery, linkTo = Option(toVertexId), linkFrom = Option(fromVertexId))
          queryObj.executeQuery(conn, query)
          logger.debug(query)
          println(query)
        } else if(fromVertexId != null && toVertexId != null){
          logger.debug("The edge exist between this two nodes %s , %s".format(fromVertexId, toVertexId))
          println("The edge exist between this two nodes %s, %s".format(fromVertexId, toVertexId))
        }
        linkKey = toVertexId + "," + fromVertexId  + ",share"
        currentEdgesSet += linkKey.toLowerCase
        if (!edgeData.contains(linkKey.toLowerCase) && fromVertexId != null && toVertexId != null) {
          val setQuery = "set Name = \"share\""
          val query: String = queryObj.createQuery(elementType = "edge", className = "testedge", setQuery = setQuery, linkTo = Option(fromVertexId), linkFrom = Option(toVertexId))
          queryObj.executeQuery(conn, query)
          logger.debug(query)
          println(query)
        } else if(fromVertexId != null && toVertexId != null){
          logger.debug("The edge exist between this two nodes %s , %s".format(fromVertexId, toVertexId))
          println("The edge exist between this two nodes %s, %s".format(fromVertexId, toVertexId))
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



