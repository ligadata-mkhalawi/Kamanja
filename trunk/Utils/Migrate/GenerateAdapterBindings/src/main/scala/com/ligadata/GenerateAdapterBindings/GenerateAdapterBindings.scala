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

import scala.io._
import java.util.Date
import java.io._

import sys.process._
import org.apache.logging.log4j._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import org.json4s.native.Serialization
import org.json4s.native.Serialization.{ read, write, writePretty }

case class adapterMessageBinding(var AdapterName: String, var MessageNames: List[String], var Options: Map[String,String], var Serializer: String)

class GenerateAdapterBindings {

  private val logger = LogManager.getLogger(getClass);
  private type OptionMap = Map[Symbol, Any]

  private def PrintUsage(): Unit = {
    logger.warn("Available commands:")
    logger.warn("    Help")
    logger.warn("    --config <ClusterConfigFileName>")
    logger.warn("    --outfile <AdapterBindingsFileName>")
  }


  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    list match {
      case Nil => map
      case "--config" :: value :: tail =>
        nextOption(map ++ Map('config -> value), tail)
      case "--outfile" :: value :: tail =>
        nextOption(map ++ Map('outfile -> value), tail)
      case option :: tail => {
        logger.error("Unknown option " + option)
        throw new Exception("Unknown option " + option)
      }
    }
  }

  private def WriteStringToFile(flName: String, str: String): Unit = {
    val out = new PrintWriter(flName, "UTF-8")
    try {
      out.print(str)
    } catch {
      case e: Exception => throw e;
      case e: Throwable => throw e;
    } finally {
      out.close
    }
  }

  private def getCCParams(cc: Product) : scala.collection.mutable.Map[String,Any] = {          
    val values = cc.productIterator
    val m = cc.getClass.getDeclaredFields.map( _.getName -> values.next ).toMap
    scala.collection.mutable.Map(m.toSeq: _*) 
  }


  def createAdapterMessageBindings(adapters: List[Map[String, Any]]) : Array[adapterMessageBinding] = {
    try{
      var ambs = Array[adapterMessageBinding]()
      adapters.foreach( a => {
	var typeString: String = null
	logger.info("a => " + a)
	val adapter = a.asInstanceOf[Map[String,Any]]
	var am = new adapterMessageBinding(new String(),Array[String]().toList,Map[String,String](),new String())
	adapter.keys.foreach( k => {
	  logger.info(k + " => " + adapter(k))
	  k.toUpperCase match {
	    case "NAME" => am.AdapterName = adapter(k).asInstanceOf[String]
	    case "TYPESTRING" => typeString = adapter(k).asInstanceOf[String]
	    case "ASSOCIATEDMESSAGE" => am.MessageNames = Array(adapter(k).asInstanceOf[String]).toList
	    case "DATAFORMAT" => {
	      adapter(k).asInstanceOf[String].toUpperCase match {
		case "CSV" => am.Serializer = "com.ligadata.kamanja.serializer.CsvSerDeser"
		case "JSON" => am.Serializer = "com.ligadata.kamanja.serializer.JsonSerDeser"
		case "KV" => am.Serializer = "com.ligadata.kamanja.serializer.kvserdeser"
		case _ => am.Serializer = "com.ligadata.kamanja.serializer.JsonSerDeser"
	      }
	    }
	    case "FIELDDELIMITER" => am.Options = am.Options + ("fieldDelimiter" -> adapter(k).asInstanceOf[String])
	    case "LINEDELIMITER" => am.Options = am.Options + ("lineDelimiter" -> adapter(k).asInstanceOf[String])
	    case "VALUEDELIMITER" => am.Options = am.Options + ("valDelimiter" -> adapter(k).asInstanceOf[String])
	    case "VALDELIMITER" => am.Options = am.Options + ("valDelimiter" -> adapter(k).asInstanceOf[String])
	    case _ => logger.info("Ignore the key " + k)
	  }
	})
	// add default options if none exist
	if( am.Options.size == 0 ){
	  am.Options = am.Options + ("alwaysQuotedFields" -> "false")
	}
	if( typeString != null ){
	  if( ( typeString.equalsIgnoreCase("Input") || 
	      typeString.equalsIgnoreCase("Status")) ){
	      if( typeString.equalsIgnoreCase("Status") ){
		if( am.MessageNames == null || am.MessageNames.length == 0 ){
		  // for status adapters, message always defults 
		  // to com.ligadata.KamanjaBase.KamanjaStatusEvent
		  am.MessageNames = Array("com.ligadata.KamanjaBase.KamanjaStatusEvent").toList
		}
		am.Serializer = "com.ligadata.kamanja.serializer.CsvSerDeser"
		if( ! am.Options.contains("fieldDelimiter") ){
		  am.Options = am.Options + ("fieldDelimiter" -> ",")
		}
		ambs = ambs :+ am
	      }
	      else{
		if( am.MessageNames != null && am.MessageNames.length > 0 ){
		  if( am.Serializer == null ){
		    am.Serializer = "com.ligadata.kamanja.serializer.CsvSerDeser"
		  }
		  if( ! am.Options.contains("fieldDelimiter") ){
		    am.Options = am.Options + ("fieldDelimiter" -> ",")
		  }
		  ambs = ambs :+ am
		}
		else{
		  logger.warn("Associated Message is not defined, A adapter-message binding is not generated for the adapter " + am.AdapterName)
		}
	      }
	    }
	    else{
	      logger.warn("The adapterType is Output type, A adapter-message binding is not generated for the adapter " + am.AdapterName)	
	    }
	}
	else{
	  logger.warn("Unable to determine adapterType(Input/output/status), A adapter-message binding is not generated for the adapter " + am.AdapterName)
	}
      })
      ambs
    } catch {
      case e: Exception => throw new Exception("Failed to create adapterMessageBindings", e)
    }
  }

  private def parseClusterConfig(cfgStr: String): Array[adapterMessageBinding] = {
    logger.info("parsing json config: " + cfgStr)
    val cfgmap = parse(cfgStr).values.asInstanceOf[Map[String, Any]]
    logger.info("cfgmap => " + cfgmap)
    var ambs = Array[adapterMessageBinding]()

    var adapters:List[Map[String, Any]] = List[Map[String,Any]]()
    cfgmap.keys.foreach(key => {
      logger.info("key => " + key)
      if ( key.equalsIgnoreCase("clusters") ){
	var clusters = cfgmap(key).asInstanceOf[List[Map[String, Any]]]
	logger.info("Looking for adapters defined within a cluster definition")
	clusters.foreach( cluster => {
	  logger.info("cluster => " + cluster)
	  if ( cluster.contains("Adapters") ){
	    adapters = cluster.get("Adapters").get.asInstanceOf[List[Map[String, Any]]]
	  }
	})
      }
      if ( key.equalsIgnoreCase("adapters") ){
	adapters = adapters ++ cfgmap("Adapters").asInstanceOf[List[Map[String, Any]]]
      }
    })
    ambs = createAdapterMessageBindings(adapters)
    ambs
  }

  private def parseAdapterConfig(cfgStr: String): Array[adapterMessageBinding] = {
    logger.info("parsing json: " + cfgStr)
    var ambs = parseClusterConfig(cfgStr)
    ambs
  }


  private def stripJsonSuffix(jsonFileName: String): String = {
    logger.debug("jsonFileName => " + jsonFileName)

    // get fileName from path
    val fd = new java.io.File(jsonFileName)
    var fname = fd.getName()

    // Fix: Once could use some FileUtils here to check whether the file has a json extension
    val tokens: Array[String] = if (fname != null && fname.contains('.')) fname.split('.') else Array(fname)
    var flNameWithoutSuffix: String = fname
    logger.debug("number of tokens => " + tokens.length)
    if( tokens.length > 1 ){
      val lastToken = tokens(tokens.length - 1)
      logger.debug("last tokens => " + lastToken)
      if( lastToken.equalsIgnoreCase("json") ){
	// construct a file name without json suffix
	val buffer: StringBuilder = new StringBuilder
	if( tokens.length > 2 ) {
	  flNameWithoutSuffix = tokens.slice(0,tokens.length-2).addString(buffer, ".").toString
	}
	else{
	  flNameWithoutSuffix = tokens(0)
	}	  
      }
    }
    logger.debug("flNameWithoutSuffix => " + flNameWithoutSuffix)
    flNameWithoutSuffix
  }

  def run(args: Array[String]): Int = {

    if (args.length == 0) {
      PrintUsage()
      return -1
    }

    val options = nextOption(Map(), args.toList)

    val cfgfile = options.getOrElse('config, null)
    if (cfgfile == null) {
      logger.error("Need configuration file as parameter")
      return -1
    }

    val clusterCfgFile = cfgfile.asInstanceOf[String]
    val iFile = new File(clusterCfgFile)
    if (false == iFile.exists){
      logger.error("Unknown config file " + clusterCfgFile)
      return -1
    }
 
    val outfile = options.getOrElse('outfile, null)
    var outFileName:String = null
    if (outfile == null) {
      // construct a default value for outfileName
      outFileName = stripJsonSuffix(clusterCfgFile) + ".adapterbindings.json"
      logger.info("outputFile is not supplied, It defaults to " + outFileName)
    }
    else{
      outFileName = outfile.asInstanceOf[String]
    }
   

    try{
      var cfgStr = Source.fromFile(iFile).mkString
      var ambs = parseClusterConfig(cfgStr)
      logger.info("ambs => " + ambs)
      implicit val formats = Serialization.formats(NoTypeHints)
      val ambsAsJson = writePretty(ambs)
      WriteStringToFile(outFileName,ambsAsJson)
      return 0
    } catch {
      case e: Exception => throw new Exception("Failed to create AdapterMessageBindings", e)
    }
  }
}

object GenerateAdapterBindings {
  private val logger = LogManager.getLogger(getClass);

  def main(args: Array[String]): Unit = {
    val gab:GenerateAdapterBindings = new GenerateAdapterBindings
    sys.exit(gab.run(args))
  }
}
