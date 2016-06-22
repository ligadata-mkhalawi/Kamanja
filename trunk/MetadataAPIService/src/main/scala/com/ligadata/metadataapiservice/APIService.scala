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

package com.ligadata.metadataapiservice

import java.io.File
import java.nio.file.{Path, Files}

import akka.actor.{ActorSystem, Props}
import akka.actor.ActorDSL._
import akka.event.Logging
import akka.io.IO
import akka.io.Tcp._
import com.typesafe.config.ConfigFactory
import spray.can.Http

import org.json4s.jackson.JsonMethods._
import com.ligadata.kamanja.metadata.ObjType._
import com.ligadata.kamanja.metadata._
import com.ligadata.kamanja.metadataload.MetadataLoad
import com.ligadata.MetadataAPI.MetadataAPIImpl
import org.apache.logging.log4j._
import com.ligadata.Utils._
import spray.can.server.ServerSettings
import scala.util.control.Breaks._
import com.ligadata.Exceptions._
import com.ligadata.KamanjaVersion.KamanjaVersion

// $COVERAGE-OFF$
class APIService extends LigadataSSLConfiguration with Runnable{

  private type OptionMap = Map[Symbol, Any]
  var inArgs: Array[String] = null

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("metadata-api-service")
  val log = Logging(system, getClass)

  val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  var databaseOpen = false
  // 646 - 676 Change begins - replace MetadataAPIImpl with MetadataAPI
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Change ends

  /**
   *
   */
  def this(args: Array[String]) = {
    this
    inArgs = args
  }

  /**
   *
   */
  def run() {
    logger.warn("running service")
    StartService(inArgs)
  }


  private def PrintUsage(): Unit = {
    logger.warn("    --config <configfilename>")
    logger.warn("    --version")
  }

  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    def isSwitch(s: String) = (s(0) == '-')
    list match {
      case Nil => map
      case "--config" :: value :: tail =>
        nextOption(map ++ Map('config -> value), tail)
      case "--version" :: tail =>
        nextOption(map ++ Map('version -> "true"), tail)
      case option :: tail => {
        logger.error("Unknown option " + option)
        sys.exit(1)
      }
    }
  }

  private def Shutdown(exitCode: Int): Unit = {
    if(system != null)
      system.shutdown()

    APIInit.Shutdown(0)
    MetadataAPIImpl.shutdown
    //System.exit(0)
  }

  private def StartService(args: Array[String]) : Unit = {
    try{

      var configFile = ""
      if (args.length == 0) {
        try {
          configFile = scala.util.Properties.envOrElse("KAMANJA_HOME", scala.util.Properties.envOrElse("HOME", "~" )) + "/config/MetadataAPIConfig.properties"
        } catch {
          case nsee: java.util.NoSuchElementException => {
            logger.warn("Either a CONFIG FILE parameter must be passed to start this service or KAMANJA_HOME must be set")
            return
          }
          case e: Exception => {
            logger.error("", e)
            return
          }
        }

        logger.warn("Config File defaults to " + configFile)
        logger.warn("One Could optionally pass a config file as a command line argument:  --config myConfig.properties")
        logger.warn("The config file supplied is a complete path name of a  json file similar to one in github/Kamanja/trunk/MetadataAPI/src/main/resources/MetadataAPIConfig.properties")
      } else {
        val options = nextOption(Map(), args.toList)
        val version = options.getOrElse('version, "false").toString
        if (version.equalsIgnoreCase("true")) {
          KamanjaVersion.print
          sys.exit(0)
        }
        val cfgfile = options.getOrElse('config, null)
        if (cfgfile == null) {
          logger.error("Need configuration file as parameter")
          throw MissingArgumentException("Usage: configFile  supplied as --config myConfig.properties", null)
        }
        configFile = cfgfile.asInstanceOf[String]
      }

      val (loadConfigs, failStr) = com.ligadata.Utils.Utils.loadConfiguration(configFile.toString, true)
      if (failStr != null && failStr.size > 0) {
        logger.error(failStr)
        Shutdown(1)
        return
      }

      if (loadConfigs == null) {
        Shutdown(1)
        return
      }

      APIInit.SetConfigFile(configFile.toString)

      // Read properties file and Open db connection
      getMetadataAPI.InitMdMgrFromBootStrap(configFile, true)
      // APIInit deals with shutdown activity and it needs to know
      // that database connections were successfully made
      APIInit.SetDbOpen

      logger.debug("API Properties => " + getMetadataAPI.GetMetadataAPIConfig)

      // We will allow access to this web service from all the servers on the PORT # defined in the config file
      val serviceHost = "0.0.0.0"
      val servicePort = getMetadataAPI.GetMetadataAPIConfig.getProperty("SERVICE_PORT").toInt

      // create and start our service actor
      val callbackActor = actor(new Act {
        become {
          case b @ Bound(connection) => logger.debug(b.toString)
          case cf @ CommandFailed(command) => logger.error(cf.toString)
          case all => logger.debug("ApiService Received a message from Akka.IO: " + all.toString)
        }
      })
      val service = system.actorOf(Props[MetadataAPIServiceActor], "metadata-api-service")

      val sslEnabled = getIsSslEnabledFromConfig
      logger.warn("Setting ssl enabled to: "+sslEnabled)
      MetadataAPIImpl.setSslEnabled(sslEnabled)

      val isValid = {
        if(MetadataAPIImpl.isSslEnabled){
          val keyStoreResource = MetadataAPIImpl.getSSLCertificatePath
          val kspass = MetadataAPIImpl.getSSLCertificatePasswd

          if(keyStoreResource == null || keyStoreResource.trim.length==0){
            logger.error("SSL is enabled. please provide a value for SSL_CERTIFICATE file path in Metadata Api config ")
            false
          }
          else if(!new File(keyStoreResource.trim).exists){
            logger.error("SSL_CERTIFICATE file ({}) does not exist ", keyStoreResource.trim)
            false
          }
          else if(kspass == null) {
            //TODO : is it allowed to be empty???
            logger.error("SSL is enabled. please provide a value for SSL_PASSWD in Metadata Api config ")
            false
          }
          else true
        }
        else true
      }

      if(!isValid){
        logger.warn("Hence shutting down")
        Shutdown(0)
        //System.exit(0)
        return
      }
      else {
        //Open db connection
        // APIInit deals with shutdown activity and it needs to know
        // that database connections were successfully made
        APIInit.SetDbOpen

        logger.debug("API Properties => " + MetadataAPIImpl.GetMetadataAPIConfig)

        // We will allow access to this web service from all the servers on the PORT # defined in the config file
        val serviceHost = "0.0.0.0"
        val servicePort = MetadataAPIImpl.GetMetadataAPIConfig.getProperty("SERVICE_PORT").toInt

        // create and start our service actor
        val callbackActor = actor(new Act {
          become {
            case b@Bound(connection) => logger.debug(b.toString)
            case cf@CommandFailed(command) => logger.error(cf.toString)
            case all => logger.debug("ApiService Received a message from Akka.IO: " + all.toString)
          }
        })
        val service = system.actorOf(Props[MetadataAPIServiceActor], "metadata-api-service")

        logger.info("Starting MetadataAPIService")
        // start a new HTTP server on a specified port with our service actor as the handler
        IO(Http).tell(Http.Bind(service, serviceHost, servicePort), callbackActor)

        logger.debug("MetadataAPIService started, listening on (%s,%s)".format(serviceHost, servicePort))

        sys.addShutdownHook({
          logger.warn("ShutdownHook called")
          Shutdown(0)
        })

        Thread.sleep(365 * 24 * 60 * 60 * 1000L)
      }
    } catch {
      case e: InterruptedException => {
        logger.info("Unexpected Interrupt", e)
      }
      case e: Exception => {
              logger.error("Exception: ", e)
      }
      case e: Throwable => {
        logger.error("Throwable: ", e)
      }
    } finally {
      logger.warn("Shutting down")
      Shutdown(0)
    }
  }

  def getIsSslEnabledFromConfig(): Boolean ={
    var sslEnabled = true // consider this default?
    val config = ConfigFactory.load()
    if(config != null){
      val sprayServerConfig = config.getConfig("spray.can.server")
      if(sprayServerConfig != null){
        val sslEncryptionConfigVAl = sprayServerConfig.getString("ssl-encryption")
        if(sslEncryptionConfigVAl != null)
          sslEnabled = sslEncryptionConfigVAl.toLowerCase.equals("on")
      }
    }
    sslEnabled
  }

}

object APIService {
  val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  def main(args: Array[String]): Unit = {
    val mgr = new APIService

    /*logger.warn("shutting down for testing")
    mgr.Shutdown(0)
    return*/

    mgr.StartService(args)
  }


  /**
   * extractNameFromJson - applies to a simple Kamanja object
   */
  def extractNameFromJson (jsonObj: String, objType: String): String = {
    var inParm: Map[String,Any] = null
    try {
      inParm = parse(jsonObj).values.asInstanceOf[Map[String,Any]]
    } catch {
      case e: Exception => {
        logger.warn("Unknown:NameParsingError", e)
        return "Unknown:NameParsingError"
      }
    }
    var vals: Map[String,String] = inParm.getOrElse(objType,null).asInstanceOf[Map[String,String]]
    if (vals == null) {
      return "unknown "+ objType
    }
    return vals.getOrElse("NameSpace","system")+"."+vals.getOrElse("Name","")+"."+vals.getOrElse("Version","-1")
  }


  /**
   * extractNameFromPMML - pull the Application name="xxx" version="xxx.xx.xx" from the PMML doc and construct
   *                       a name  string from it
   */
  def extractNameFromPMML (pmmlObj: String): String = {
    var firstOccurence: String = "unknownModel"
    val pattern = """Application[ ]*name="([^ ]*)"[ ]*version="([^ ]*)"""".r
    val allMatches = pattern.findAllMatchIn(pmmlObj)
    allMatches.foreach( m => {
      if (firstOccurence.equalsIgnoreCase("unknownModel")) {
      firstOccurence = (m.group(1)+"."+m.group(2))
      }
    })
    return firstOccurence
  }
}
// $COVERAGE-ON$
