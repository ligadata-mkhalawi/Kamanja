package com.ligadata.filedataprocessor

import java.io.{IOException, File, PrintWriter}
import java.nio.file.{Path, FileSystems}
import com.ligadata.Exceptions.{InternalErrorException, MissingArgumentException}
import org.apache.logging.log4j.{ Logger, LogManager }
import scala.collection.mutable.ArrayBuffer

/**
 * Created by danielkozin on 9/24/15.
 */
class DirectoryListener {

}

class ConnectionConfig(var Type: String, var Host : String, var Locations : Array[String], var UserId : String, var Password : String)
class MonitoringConfig(var WaitingTimeMS : Int)

object LocationWatcher {
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  def main (args: Array[String]) : Unit = {

    /*val json1 = "{ \"Type\": \"sftp\", \"Host\": \"10.20.30.40:2000\", \"Locations\": [\"folder1\", \"folder2\"], \"UserId\": \"user\", \"Password\": \"passwd\"}"
    val connectionConf = JsonHelper.getConnectionConfigObj(json1)
    println(connectionConf.Type)
    println(connectionConf.Host)
    connectionConf.Locations.foreach(println)
    println(connectionConf.UserId)
    println(connectionConf.Password)*/

    /*val json2 = "{ \"WaitingTimeMS\": 300}"
    val conf = JsonHelper.getMonitoringConfigObj(json2)
    println(conf.WaitingTimeMS)
    return;*/

      if (args.size == 0 || args.size > 1) {
        logger.error("Smart File Consumer requires a configuration file as its argument")
        return
      }

      // Read the config and figure out how many consumers to start
      var config = args(0)
      var properties = scala.collection.mutable.Map[String,String]()

      val lines = scala.io.Source.fromFile(config).getLines.toList
      lines.foreach(line => {
        //Handle empty lines also 
        if (!line.isEmpty() && !line.startsWith("#")) {
          val lProp = line.split("=")
          try {
            logger.info("SMART FILE CONSUMER "+lProp(0) + " = "+lProp(1))
            properties(lProp(0)) = lProp(1)
          } catch {
            case iobe: IndexOutOfBoundsException => {
              logger.error("SMART FILE CONSUMER: Invalid format in the configuration file " + config, iobe)
              logger.error("SMART FILE CONSUMER: unable to determine the value for property " + lProp(0))
              return
            }
          }
        }
      })

      // FileConsumer is a special case we need to default to 1, but also have it present in the properties since
      // it is used later for memory managemnt
      var numberOfProcessorsRaw = properties.getOrElse(SmartFileAdapterConstants.NUMBER_OF_FILE_CONSUMERS,null)
      var numberOfProcessors: Int = 1
      if (numberOfProcessorsRaw == null) {
        properties(SmartFileAdapterConstants.NUMBER_OF_FILE_CONSUMERS) = "1"
        logger.info("SMART FILE CONSUMER: Defaulting the number of file consumers to 1")
      } else  {
        numberOfProcessors = numberOfProcessorsRaw.toInt
      }

      //var processors: Array[FileProcessor] = new Array[FileProcessor](numberOfProcessors)
      var threads: Array[Thread] = new Array[Thread](numberOfProcessors)
      
      //var path: Path= null
      //Create an array of paths
      var path = new ArrayBuffer[Path]()
      
      try {
         val dirName = properties.getOrElse(SmartFileAdapterConstants.DIRECTORY_TO_WATCH, null)
         if (dirName == null) {
           logger.error("SMART FILE CONSUMER: Directory to watch is missing, must be specified")
           return
         }
         
         //path = FileSystems.getDefault().getPath(dirName)
         var p:Int = 0;
         for(x <- dirName.split(System.getProperty("path.separator"))){
           path += FileSystems.getDefault().getPath(x)
         }
         
      } catch {
        case e: IOException => {
          logger.error ("Unable to find the directory to watch", e)
          return
        }
      }

      for(dir <- path)
        logger.info("SMART FILE CONSUMER: Starting "+ numberOfProcessors+" file consumers, reading from "+ dir)

      try {
        for (i <- 1 to numberOfProcessors) {
          var processor = new FileProcessor(path,i)
          processor.init(properties)
          val watch_thread = new Thread(processor)
          watch_thread.start
        }
      } catch {
        case e: Exception => {
          logger.error("SMART FILE CONSUMER:  ERROR in starting SMART FILE CONSUMER ", e)
          return
        }
      }
  }
}
