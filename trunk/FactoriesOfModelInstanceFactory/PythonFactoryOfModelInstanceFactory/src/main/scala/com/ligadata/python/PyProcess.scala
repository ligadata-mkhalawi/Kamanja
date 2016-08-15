/*
 * Copyright 2016 ligaDATA
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

package com.ligadata.python

import java.lang.reflect.Field
import scala.actors._
import scala.actors.Actor._
import org.apache.logging.log4j.LogManager


object PyProcessGlobalLogger {
  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)
}

trait LogProcessTrait {
  val logger = PyProcessGlobalLogger.logger
}


class PyProcess(host: String,
                port: Int,
//  PyPath: String)  {
  PyPath: String) extends LogProcessTrait {

  val HostText: String = "--host"
  val Host: String = "localhost"
  val PortText: String = "--port"
  val PyPathText : String = "--pythonPath"
  val LogConfigText: String = "--log4jConfig"
  val LogConfigFileName: String = "pythonlog4j.cfg"
  val LogFilePathText:String = "--fileLogPath"
  val LogFileName: String = "pythonserver.log"
  val SingleSpace : String = " "

  val cHost: String = host
  val cPort: Int = port
  val cPyPath: String = PyPath
  var processBuilder: ProcessBuilder = _
  var proc: Process = _
  var pid: Long = _

  private val caller = self
  private val WAIT_TIME = 2000

  private val reader = actor {
    logger.debug("created actor: " + Thread.currentThread)
    var continue = true
    loopWhile(continue) {
      reactWithin(WAIT_TIME) {
        case TIMEOUT =>
          //caller ! "react timeout"
        case proc: Process =>
          logger.debug("PyProcess : entering first actor " + Thread.currentThread)
          try {
          val streamReader = new java.io.InputStreamReader(proc.getInputStream)
            logger.debug("PyProcess : came after stream reader " )
          val bufferedReader = new java.io.BufferedReader(streamReader)
            logger.debug("PyProcess : came after buffered reader " )
          val stringBuilder = new java.lang.StringBuilder()
          var line: String = null
          while ( {
            line = bufferedReader.readLine;
            line != null
          }) {
            stringBuilder.append(line)
            stringBuilder.append("\n")
          }
          bufferedReader.close
//          caller ! stringBuilder.toString
          logger.debug("PyProcess : The process pid in reader " + pid.toString + stringBuilder.toString)
      }
      catch {

        case e: Exception => {
          logger.debug("PyProcess : The process pid in reader has Exception " + pid.toString)
        }
      }

      }
    }
  }

  def run(command: String) {
    logger.debug(s"going to run the command: " + Thread.currentThread + " " + command)
    val args = command.split(" ")
    processBuilder = new ProcessBuilder(args: _*)

    proc = processBuilder.start()
    Thread.sleep(2000)
    try {
      if (proc.getClass().getName().equals("java.lang.UNIXProcess")) {
        proc.getClass().getDeclaredField("pid").setAccessible(true)
        var f: Field = proc.getClass().getDeclaredField("pid")
        f.setAccessible(true)
        pid = f.getLong(proc)
        logger.debug("Py Process The server started at host " + cHost + " at port " + cPort + " and the processor id is " + pid)
        f.setAccessible(false)
      }
    }
    catch {

      case e : Exception => {
        pid = - 1
        logger.debug("Problem in starting the python server " + cHost + " at " + cPort)
      }
    }
    processBuilder.redirectErrorStream(true)

    processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

    reader ! proc

  }


  def initPyProcess(): Unit = {
    var cmdString: String =
      cPyPath + "/pythonserver.py " +
      HostText + SingleSpace + cHost + SingleSpace +
      PortText + SingleSpace + cPort + SingleSpace +
      PyPathText + SingleSpace + cPyPath + SingleSpace +
      LogConfigText + SingleSpace + cPyPath + "/config/" + LogConfigFileName + SingleSpace +
      LogFilePathText + SingleSpace + cPyPath + "/logs/" + LogFileName
    logger.debug("THis is pyprocess scala  must see this if logger here works " + cmdString)

    run (cmdString)

//    while (true) {
      //Receive the console output from the actor.
      //receiveWithin(2000) {
//        case TIMEOUT => //println("receiving Timeout")
        //case result: String => logger.debug("PyProcess : The process pid " + pid.toString + " " + result)
      //}
    //}
  }

  def killSubProcess(): Unit = {

    proc.destroy()

  }


}
