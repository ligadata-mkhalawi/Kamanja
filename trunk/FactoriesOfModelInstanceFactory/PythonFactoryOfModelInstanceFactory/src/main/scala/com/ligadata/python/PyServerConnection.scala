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

import java.io._
import java.net.{InetAddress, Socket}
import java.nio.ByteBuffer
import java.util.regex.{Matcher, Pattern}
import java.net.{SocketException, SocketTimeoutException, ConnectException}

import scala.collection.mutable.ArrayBuffer
import scala.sys.process._
import scala.util.control.Breaks._
import scala.collection.immutable.Map

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.native.Json
import org.json4s.DefaultFormats

import org.apache.logging.log4j.LogManager


object PyServerConnGlobalLogger {
    val loggerName = this.getClass.getName
    val logger = LogManager.getLogger(loggerName)
}

trait LogTrait {
    val logger = PyServerConnGlobalLogger.logger
}

/**
  * A PyServerConnection instance establishes an IP connection to a Kamanja python server, by first starting
  * the server and then creating a Socket connection between it and the newly started server.
  * There is but one such Socket connection with the started python server.
  *
  * On the surface of it, this seems like a bad idea in that communication is pinched through this sole socket.
  * That issue is mitigated in the Kamanja usage by creating a unique server for each thread in the thread pool
  * that manages the model execution.  If there are dozen threads dedicated to processing models, there will be a
  * dozen python servers running and a dozen PyServerConnection objects communicating with them on behalf of
  * the Kamanja node.
  *
  * The python factory responsible for instantiating a given python model
  * will be given one of the connections for the thread it will service.
  *
  * @param host the host upon which the python server will run (typically the same host as the kamanja node making
  *             the request.
  * @param port a unique port unused by any other service or python server that is to be run
  * @param user the user account that will be running the server
  * @param log4jConfigPath a file system path that contains the log4j configuration used on the python server to be
  *                        started.
  * @param fileLogPath the name of the log file to be used to capture logging information for the server to be started.
  *                    It is possible to share the same file name for all of the servers started for the model execution
  *                    threads in the pool or use a unique name to separate the logs.  When logging to separate files, it
  *                    is recommended that the port be part of the filename in the path and place all logs in the same
  *                    directory.  Or do it some other way if you like.
  * @param pyPath the Kamanja python path ... part of the Kamanja installation.  This path contains the the Kamanja
  *               python server elements including the modules, config files, working models, and server itself.
  */
class PyServerConnection(val host : String
                         , val port : Int
                         , val user : String
  , val log4jConfigPath : String,
  val fileLogPath : String,
  val pyPath : String,
  val pyBinPath : String,
  val pKey : String
  ) extends LogTrait {

    private var _sock : Socket = null
    private var _in : DataInputStream = null
    private var _out : DataOutputStream = null
    private val _decoder = new Decoder
    private val _buffer: Array[Byte] = new Array[Byte](2 ^ 16)
    /**
      * Start the server and prepare a connection to the server for users of this thread.
      *
      * @return two result strings in the tuple where _1 is the start server return status and _2 is whether a
      *         connection was created to that server.
      */
    def initialize : (String,String) = {

        /** start the server */
      val startServerResult : String = startServer
      if (logger.isDebugEnabled()) {
        logger.debug(s"PyServerConnection.initialize ... start server result = $startServerResult")
     }
        implicit val formats = org.json4s.DefaultFormats
        val startResultsMap : Map[String,Any] = parse(startServerResult).values.asInstanceOf[Map[String,Any]]
        val pid : Int = startResultsMap.getOrElse("pid", -1).asInstanceOf[scala.math.BigInt].toInt
      var attempts : Int  = 0
      if (logger.isDebugEnabled()) {
         logger.debug (" THe value of host is " + host + " in the initialize of PyServerConnection ")
	 }
        /** create a connection to the server on the port that it is listening. */
        val inetbyname = InetAddress.getByName(host)
      if (logger.isDebugEnabled()) {
         logger.debug("PyServerConnection.initialize ... connecting to host known as '$inetbyname' " + inetbyname + " " + port.toString)
       }
      var mSecsToSleep : Long = 500
      Thread.sleep(mSecsToSleep)
      _sock = new Socket(inetbyname, port)
      while (_sock == null && attempts < 10) {
        try {
          _sock = new Socket(inetbyname, port)
        }
        catch {
          case ne : NullPointerException => {
          }
          case ste: SocketTimeoutException => {
                                    /** Fixme: The behavior here is to retry, but wait a twice as long... this is no doubt incorrect... consider this a
                                      *  place holder for actual behavior for actual problems....
                                      */
            logger.error(s"Exception encountered processing ... unable to produce result...exception = ${ste.toString}")
          }
          case co : ConnectException => {

            logger.error(s"Connection Exception encountered processing ... unable to connectt...exception = ${co.toString}")
          }
          case e: Exception => {
            /** if it is not one of the supported retry exceptions... we blow out of here */
            logger.error(s"Exception encountered processing ... unable to produce result...exception = ${e.toString}")
            break
          }
        }
        if (_sock == null) {
          mSecsToSleep *= 2
          Thread.sleep(mSecsToSleep)
          attempts = attempts + 1
	  if (logger.isDebugEnabled()) {
            logger.debug ("Continuting to connect for attempt " + attempts.toString)
          }
        }
        else {
          break
        }

      }
        _in = new DataInputStream(_sock.getInputStream)
        _out = new DataOutputStream(_sock.getOutputStream)

        val (rc, result) : (Int,String) = if (pid >  0 && _sock != null && _in != null && _out != null) {
          (0, "connection created")

        } else {
            (-1 ,"connection creation failed")
        }
      val connResult : String = s"{ ${'"'}code${'"'} : $rc,  ${'"'}result${'"'} : ${'"'}$result${'"'}}"
      if (logger.isDebugEnabled()) {
        logger.debug(s"PyServerConnection.initialize ... conection result = $connResult")
       }
        (startServerResult, connResult)
    }

    /**
      * Using the constructor arguments start a python server. Mark this instance as usable.
      *
      * @return JSON result string that is a dictionary with the "pid" and "result" as keys. For example,
      *         '''
      *         {
      *             "code" : 0
      *             "result": "Server started successfully",
      *             "pid": 22537
      *         }
      *         '''
      */
    private def startServer : String = {
        val useSSH : Boolean = host != "localhost"

      val pyProcess : PyProcess = new PyProcess(host, port, pyPath, pyBinPath, pKey)
//        val pythonCmdStr = s"python $pyPath/pythonserver.py --host $host --port ${port.toString} --pythonPath $pyPath --log4jConfig $log4jConfigPath --fileLogPath $fileLogPath"
      pyProcess.initPyProcess()

      val (rc, result) : (Int, String) = if (pyProcess.pid != null) (0, "Server started successfully") else (-1, "Server start failed")
      val pidStr : String = if (pyProcess.pid != null) pyProcess.pid.toString else s"${'"'}----${'"'}"

      val resultStr : String = s"{ ${'"'}code${'"'} : $rc,  ${'"'}result${'"'} : ${'"'}$result${'"'},  ${'"'}pid${'"'} : $pidStr }"

        resultStr
    }

    /** Process one message, sending the cmdMsg to the DataOutputStream and collecting the answer from the DataInputStream.
      *
      * @param in bytes are received from server here
      * @param out bytes are sent to the server with this
      * @param cmd visual id as to which command is being executed
      * @param cmdMsg the command to send
      * @param buffer working buffer for the bytes received from the DataInputStream
      * @return Unit
      */
    private def processMsg(in : DataInputStream, out : DataOutputStream, cmd : String, cmdMsg : Array[Byte], buffer : Array[Byte]) : String = {

        val cmdLen: Int = cmdMsg.length
        out.write(cmdMsg, 0, cmdLen)
        out.flush()
      logger.warn (" In Process Msg ----- port number ---------- " + port.toString)
        /** Contend with multiple messages results returned */
        val answeredBytes: ArrayBuffer[Byte] = ArrayBuffer[Byte]()
	if (logger.isDebugEnabled()) {
	   logger.debug (" In Process Msg going to read " + port.toString)
	}
        var bytesReceived = in.read(buffer)
	if (logger.isDebugEnabled()) {
           logger.debug (" In Process Msg did i get here  " + port.toString)
	}
        var result : String = ""
        breakable {
            while (bytesReceived > 0) {
                answeredBytes ++= buffer.slice(0, bytesReceived)
                /** print one result each loop... and then the remaining (if any) after bytesReceived == 0) */
		if (logger.isDebugEnabled()) {
		   logger.debug ("The size of bytes received is " +  bytesReceived) 
		   logger.debug ("The content  of bytes received is " +  answeredBytes.toString)
		}
                val endMarkerIdx: Int = answeredBytes.indexOfSlice(CmdConstants.endMarkerArray)
		if (logger.isDebugEnabled()) {
		   logger.debug ("The endMarkerIdx value is  " +  endMarkerIdx.toString)
		}
                if (endMarkerIdx >= 0) {
                  val endMarkerIncludedIdx: Int = endMarkerIdx + CmdConstants.endMarkerArray.length
                  if (logger.isDebugEnabled()) {
                    logger.debug (" The value of endMarkerIncludedIdx is  " + endMarkerIncludedIdx.toString)
                  }
                    val responseBytes: Array[Byte] = answeredBytes.slice(0, endMarkerIncludedIdx).toArray
                    result =  _decoder.unpack(responseBytes)
		    if (logger.isDebugEnabled()) {
                       logger.debug(s"$cmd reply = \n$result")
		    }
                    answeredBytes.remove(0, endMarkerIncludedIdx)
		    break 
                }
		if (logger.isDebugEnabled()) {
		logger.debug("bytes received = " + bytesReceived.toString) 
		}
                bytesReceived = in.read(buffer)
            }
        }
	if (logger.isDebugEnabled()) {
           logger.debug (" In the end of Process Msg ----- port number ---------- " + port.toString + ", bytes left = " + answeredBytes.nonEmpty.toString)
	}
        if (answeredBytes.nonEmpty) {
            logger.error("*****************************************************************************************************************************")
          logger.error("... in processMsg, there are resisdual bytes remaining suggesting multiple commands were dispatched with no intervening receipt of response bytes... some component is sending multiple commands or commands are being sent to this connection from multiple threads... a violation of the supposed contract. ")
          logger.error (" this is the result at that time " + result)
            logger.error("*****************************************************************************************************************************")
        }

        /** When there is a cmd followed by a response, the remove above always takes out all of the bytes.
          * However, if multiple commands are sent at once, then the additional responses are handled here
          * for those subsequent commands.  SINCE WE ARE NOT GOING TO BURST MESSAGES AT THIS JUNCTURE, THIS
          * IS COMMENTED OUT.
          **
          *val lenOfRemainingAnsweredBytes: Int = answeredBytes.length
          *while (lenOfRemainingAnsweredBytes > 0) {
          *val endMarkerIdx: Int = answeredBytes.indexOfSlice(CmdConstants.endMarkerArray)
          *if (endMarkerIdx >= 0) {
          *val endMarkerIncludedIdx: Int = endMarkerIdx + CmdConstants.endMarkerArray.length
          *val responseBytes: Array[Byte] = answeredBytes.slice(0, endMarkerIncludedIdx).toArray
          *val response: String = _decoder.unpack(responseBytes)
          *logger.info(response)
          *answeredBytes.remove(0, endMarkerIncludedIdx)
          *} else {
          *if (answeredBytes.nonEmpty) {
          *logger.error("There were residual bytes remaining in the answer buffer suggesting that the connection went down")
          *logger.error(s"Bytes were '${answeredBytes.toString}'")
          *}
          *}
          *}
          */

        result
    }

    /**
      * Stop the server.  Mark this instance as unusable.
      *
      * @return JSON result string that describes the result of the operation.  For example,
      *         '''
      *         {
      *             "Cmd": "stopServer",
      *             "Port": "9998",
      *             "Result": "Host pepper.botanical.com listening on port 9998 to be stopped by user command",
      *             "Server": "pepper.botanical.com"
      *         }
      *         '''
      */
    def stopServer : String = {
        val payloadStr : String = s"{${'"'}Cmd${'"'}: ${'"'}stopServer${'"'}, ${'"'}CmdVer${'"'}: 1 }"
        val result : String = encodeAndProcess("stopServer", payloadStr)
        result
    }

    /**
      * Add the supplied model to the python server found at the other end of this PyServerConnection's socket.
      *
      * @param moduleName the name of the module (i.e, the file name) to be installed on the server
      * @param modelName the name of the model in that module file to be sent executeModel commands
      * @param moduleSrc the python source for this module
      * @param modelOptions (optional) a Map[String,Any] that will be serialized and sent has part of the addModel
      *                     request to the server.  The model options are given to the python model instance being
      *                     added.  Information contained therein is model specific and unused by the server.
      * @return JSON string result for the addModel operation.  For example,
      *         '''
      *         {
      *           "InputFields": {
      *             "a": "Int",
      *             "b": "Int"
      *           },
      *           "Cmd": "addModel",
      *           "Server": "pepper.botanical.com",
      *           "OutputFields": {
      *             "a": "Int",
      *             "b": "Int",
      *             "result": "Int"
      *           },
      *           "Result": "model add.AddTuple added",
      *           "Port": "9998"
      *         }
      *         '''
      *
      * Note: In this release, modules cannot have namespaces.  All modules are stored in the pypath/models directory.
      * No provisions at the moment for creating module directory trees from names like mammal.swimming.whale
      */
    def addModel(moduleName : String, modelName : String, moduleSrc : String, modelOptions : Map[String, Any] = Map[String, Any]()) : String = {

        /** serialize the modelOptions */
        val modelOpts : String = Json(DefaultFormats).write(modelOptions)
        /** copy the file into a tmp file from string for either local copy to models or possibly remote copy to other server */
      if (logger.isDebugEnabled()) {
         logger.debug("Before add model in PyServerConnection moduleName " + moduleName + " moduleSrc " + moduleSrc)
      }
      //  cpSrcFile(moduleName, moduleSrc)
      if (logger.isDebugEnabled()) {
         logger.debug("After  add model in PyServerConnection moduleName " + moduleName + " moduleSrc " + moduleSrc)
      }
      val addMsg : String = s"{${'"'}Cmd${'"'}: ${'"'}addModel${'"'}, ${'"'}CmdVer${'"'}: 1, ${'"'}CmdOptions${'"'}: {${'"'}Module${'"'}: ${'"'}$moduleName${'"'}, ${'"'}ModelName${'"'}: ${'"'}$modelName${'"'} }, ${'"'}ModelOptions${'"'}: ${'"'}{OPTIONS_KEY}${'"'} }"
         if (logger.isDebugEnabled()) {
            logger.debug("The add msg is " + addMsg)
	}
        val subMap : Map[String,String] = Map[String,String]("{OPTIONS_KEY}" -> modelOpts)
        val sub = new MapSubstitution(addMsg, subMap)
        val payloadStr : String = sub.makeSubstitutions

        if (logger.isDebugEnabled()) {
	   logger.debug("payloadStr  is " + payloadStr)
	}
        val result : String = encodeAndProcess("addModel", payloadStr)
	if (logger.isDebugEnabled()) {
	   logger.debug("result of encode and process is  " + result)
	}
        result
    }

    private def cpSrcFile(moduleName : String, moduleSrc : String) : Unit = {

      val srcTargetPath : String = s"$pyPath/tmp/$moduleName.py"
      if (logger.isDebugEnabled() ) {
        logger.debug(s"create disk file for supplied moduleSrc ... srcTargetPath = $srcTargetPath")
      }
        writeSrcFile(moduleSrc, srcTargetPath)

        /** copy the python model source file to $pyPath/models */
        val useSSH : Boolean = host != "localhost" && host != "127.0.0.1"
        val slash : String = if (pyPath != null && pyPath.endsWith("/")) "" else "/"
        val fromCpArgsStr : String = s"$srcTargetPath"
        val toCpArgsStr : String = s"$pyPath${slash}models/"
        val cmdSeq : Seq[String] = if (useSSH) {
            val userMachine : String = s"$user@$host"
            Seq[String]("scp", userMachine, fromCpArgsStr, toCpArgsStr)
        } else {
	  if (logger.isDebugEnabled()) {
            logger.debug(s"copy model $srcTargetPath locally to $pyPath${slash}models/")
	  }
            Seq[String]("cp", fromCpArgsStr, toCpArgsStr)
        }
        val (result, stdoutStr, stderrStr) : (Int, String, String) = PyServerHelpers.runCmdCollectOutput(cmdSeq)
        if (result != 0) {
            logger.error(s"AddModel failed... unable to copy $srcTargetPath to $pyPath${slash}models/")
            logger.error(s"copy error message(s):\n\t$stderrStr")
        }
    }

    /** Write the source file string to the supplied target path.
      *
      * @param srcCode
      * @param srcTargetPath
      */
    private def writeSrcFile(srcCode: String, srcTargetPath: String) {
        val file = new File(srcTargetPath)
        val bufferedWriter = new BufferedWriter(new FileWriter(file))
        bufferedWriter.write(srcCode)
        bufferedWriter.close
    }


    /**
      * Remove the model from the python server's working set found at the other end of this PyServerConnection's socket.
      *
      * @param modelName the name of the model to remove
      * @return JSON string describing the remove operation.  For example,
      *         '''
      *         {
      *           "Cmd": "removeModel",
      *           "Port": "9998",
      *           "Result": "model AddTuple removed",
      *           "Server": "pepper.botanical.com"
      *         }
      *         '''
      */
    def removeModel(moduleName : String, modelName : String) : String = {
        val json : org.json4s.JValue = (
            ("Cmd" -> "removeModel") ~
                ("CmdVer" -> 1) ~
                ("CmdOptions" -> (
                    ("ModelName" -> modelName) //~
                    //("InputMsgs" -> msg)
                    ))
            //("ModelOptions" -> List[String]())
            )
        val payloadStr : String = s"{${'"'}Cmd${'"'}: ${'"'}removeModel${'"'}, ${'"'}CmdVer${'"'}: 1, ${'"'}CmdOptions${'"'}: {${'"'}Module${'"'}: ${'"'}$moduleName${'"'},${'"'}ModelName${'"'}: ${'"'}$modelName${'"'} } }"
        val result : String = encodeAndProcess("removeModel", payloadStr)
        result
    }

    /**
      * Answer with the python server's status.
      *
      * @return JSON string describing the remove operation.  For example,
      *         '''
      *         {
      *           "Cmd": "serverStatus",
      *           "Port": "9998",
      *           "Result": "Active models are: ['MultiplyTuple', 'SubtractTuple', 'DivideTuple', 'PythonInstallPath']",
      *           "Server": "pepper.botanical.com"
      *         }
      *         '''
      */
    def serverStatus : String = {
        val payloadStr : String = s"{${'"'}Cmd${'"'}: ${'"'}serverStatus${'"'}, ${'"'}CmdVer${'"'}: 1 }"
        val result : String = encodeAndProcess("serverStatus", payloadStr)
        result
    }

    /**
      * Execute the model given the supplied message.  The input message map is transformed to json before sending. For
      * example, a map with two integer fields, a and b, would be transformed to:
      *
      *     '''{"a": 2, "b": 2 }'''
      *
      * @param modelName the name of the model to execute
      * @param msg the msg map to send to the python server.  The preparation of this map is done by the python
      *            model's proxy that selects the required fields from the incoming message.
      * @return JSON string describing the remove operation.  For example, the result from an "AddTuple" model:
      *         '''
      *         {
      *           "a": 2,
      *           "b": 2,
      *           "result": 4
      *         }
      *         '''
      *
      * In the case of the executeModel, the json returned is strictly the output map to be supplied for disposition
      * to the engine.
      */
    def executeModel(moduleName: String, modelName: String, msg : Map[String, Any]) : String = {

        val msgFieldMap : String = Json(DefaultFormats).write(msg)

        val jsonCmdTemplate: String = s"{${'"'}Cmd${'"'}: ${'"'}executeModel${'"'}, ${'"'}CmdVer${'"'}: 1, ${'"'}CmdOptions${'"'}: { ${'"'}Module${'"'}: ${'"'}$moduleName${'"'},${'"'}ModelName${'"'}: ${'"'}$modelName${'"'}, ${'"'}InputDictionary${'"'}: ${'"'}{DATA.KEY}${'"'}}}"

        val subMap : Map[String,String] = Map[String,String]("{DATA.KEY}" -> msgFieldMap)
        val sub = new MapSubstitution(jsonCmdTemplate, subMap)
        val payloadStr : String = sub.makeSubstitutions
	if (logger.isDebugEnabled()) {
           logger.debug(s"executeModel msg='$payloadStr'")
	}

        val result : String = encodeAndProcess("executeModel", payloadStr)
//	  val  result : String  = payloadStr
	if (logger.isDebugEnabled()) {
           logger.debug(s"executeModel result='$result'")
	}

        result
    }

    /**
      * Encode the supplied JSON command string as a byte array, wrapping it with the necessary begin, crc, len, and end
      * bytes.
      *
      * @param cmdName visual identifier for command
      * @param cmdStr the JSON command string with the command specifics in it
      * @return cmd result (a JSON string)
      */
    private def encodeAndProcess(cmdName : String, cmdStr : String) : String = {

      if (logger.isDebugEnabled()) {
        logger.debug("cmdName = " + cmdName + " , cmdStr = " + cmdStr)
      }

      val payload : Array[Byte] = cmdStr.getBytes
        val checksumBytes : ByteBuffer = ByteBuffer.allocate(CmdConstants.lenOfCheckSum)
        checksumBytes.putLong(0L)
        val chkBytesArray : scala.Array[Byte] = checksumBytes.array()
        val lenBytes : ByteBuffer = ByteBuffer.allocate(CmdConstants.lenOfInt)
        lenBytes.putInt(payload.length)
        val payloadLenAsBytes : Array[Byte] = lenBytes.array()
        val cmdBytes : Array[Byte] = CmdConstants.startMarkerArray ++
            chkBytesArray ++
            payloadLenAsBytes ++
            payload ++
            CmdConstants.endMarkerArray

      if (logger.isDebugEnabled()) {
        logger.debug(s"$cmdName msg = ${CmdConstants.startMarkerValue} 0L ${payload.length} $cmdStr ${CmdConstants.endMarkerValue}")
        logger.debug(s"$cmdName msg len = ${cmdBytes.length}")
      }

        val result : String = if (cmdBytes.length == 0) {
            logger.error(s"there were no commands formed for cmdName = $cmdName... abandoning processing")
            ""
        } else {
            processMsg(_in, _out, cmdName, cmdBytes, _buffer)
        }
        result

    }
}

object CmdConstants {
    /** start and end message demarcation (marker) values as strings and arrays */
    val startMarkerValue : String = "_S_T_A_R_T_"
    val endMarkerValue : String = "_F_I_N_I_"
    val startMarkerArray : Array[Byte] = Array[Byte]('_','S','_','T','_','A','_','R','_','T','_')
    val endMarkerArray : Array[Byte] = Array[Byte]('_','F','_','I','_','N','_','I','_')
    /** at some point a crc or digest will be calculated on the cmd message (when
      * the python server is perhaps not located on the local machine) */
    val crcDefaultValue : Long = 0L

    /** lengths of the two fixed fields (scalars) that follow the startMarkerValue */
    val lenOfCheckSum : Int = 8
    val lenOfInt : Int = 4
}

/**
  * Class Decoder interprets the returned results, unpacking the payload message from
  * it and returning it.
  */
class Decoder extends LogTrait {
    /**
      * Unpack the returned message:
      * startMarkerValue ("_S_T_A_R_T_")
      * checksum (value is 0L ...  unused/unchecked)
      * result length (an int)
      * cmd result (some json string)
      * endMarkerValue ("_F_I_N_I_")
      *
      * If all is well, reconstitute the json string value from the payload portion.
      *
      * @param answeredBytes an ArrayBuffer containing the reply from the py server
      * @return the string result if successfully transmitted.  When result integrity
      *         an issue, issue error message as the result revealing the finding.
      *
      */
    def unpack(answeredBytes : Array[Byte]) : String = {
        val lenOfCheckSum : Int = CmdConstants.lenOfCheckSum
        val lenOfInt : Int = CmdConstants.lenOfInt
        val startMarkerValueLen : Int = CmdConstants.startMarkerValue.length
        val endMarkerValueLen : Int = CmdConstants.endMarkerValue.length

      if (logger.isDebugEnabled()) {
        logger.debug ("in unpack answeredBytes = " + answeredBytes.length.toString)
        logger.debug ("in unpack content of answeredBytes is " + answeredBytes.toString)
      }
        val reasonable : Boolean = answeredBytes != null &&
            answeredBytes.length > (startMarkerValueLen + lenOfCheckSum + lenOfInt + endMarkerValueLen)
        val answer : String = if (reasonable) {
            val byteBuffer :  ByteBuffer = ByteBuffer.wrap(answeredBytes)
            val startMark : scala.Array[Byte] = new scala.Array[Byte](startMarkerValueLen)
            val endMark : scala.Array[Byte] = new scala.Array[Byte](endMarkerValueLen)
            /** unpack the byte array into md5 digest, payload len, payload, md5 digest */
            byteBuffer.get(startMark,0,startMarkerValueLen)
            val crc : Long = byteBuffer.getLong()
            val payloadLen : Int = byteBuffer.getInt()
          val startMarkStr : String = new String(startMark)
          if (logger.isDebugEnabled()) {
          logger.debug(s"startMark = $startMarkStr, crc = $crc, payload len = $payloadLen")
          logger.debug(s"startMark = $startMarkStr, crc = $crc, endMarkerLoan = $endMarkerValueLen")
          }
            val payloadArray : scala.Array[Byte] = new scala.Array[Byte](payloadLen)
            byteBuffer.get(payloadArray,0,payloadLen)
            byteBuffer.get(endMark,0,endMarkerValueLen)
            val endMarkStr : String = new String(endMark)
          val payloadStr : String = new String(payloadArray)
          if ( logger.isDebugEnabled()) {
            logger.debug(s"payload = $payloadStr")
            logger.debug(s"endMark = $endMarkStr")
          }

            payloadStr
        } else {
            "unreasonable bytes returned... either null or insufficient bytes in the supplied result"
        }
        answer
    }

}

object PyServerHelpers extends LogTrait {
    /**
      * Execute the supplied command sequence. Answer with the rc, the stdOut, and stdErr outputs from
      * the external command represented in the sequence.
      *
      * Warning: This function will wait for the process to end.  It is **_not_** to be used to launch a daemon. Use
      * cmd.run instead. If this application is itself a server, you can run it with the ProcessLogger as done
      * here ... possibly with a different kind of underlying stream that writes to a log file or in some fashion
      * consumable with the program.
      *
      * @param cmd external command sequence
      * @return (rc, stdout, stderr)
      */
  def runCmdCollectOutput(cmd: Seq[String]): (Int, String, String) = {
    if (logger.isDebugEnabled()) {
      logger.debug ("in runCmdCollectOutput cmd string is  " + cmd.mkString (" ") )
    }
        val stdoutStream = new ByteArrayOutputStream
        val stderrStream = new ByteArrayOutputStream
        val stdoutWriter = new PrintWriter(stdoutStream)
        val stderrWriter = new PrintWriter(stderrStream)
        val exitValue = cmd.!(ProcessLogger(stdoutWriter.println, stderrWriter.println))
        stdoutWriter.close()
        stderrWriter.close()
    if (logger.isDebugEnabled()) {
      logger.debug ("in runCmdCollectOutput result is   " + exitValue.toString + " stdout = " + stdoutStream.toString + ", stderr = " + stderrStream.toString )
    }
    (exitValue, stdoutStream.toString, stderrStream.toString)
    }

}

/**
  * MapSubstitution used to stitch in home grown json strings possibly incorporated
  * into one of the messages destined for the pythonserver.  See executeModel for use.
  *
  * Embedded keys look like this regexp: val patStr = """(\{[A-Za-z0-9_.-]+\})"""
  *
  * For example:
  *     {This_is_.my-1.key}
  *
  * @param template the string to have its embedded keys substituted
  * @param subMap a map of substutitions to make.
  */
class MapSubstitution(template: String, subMap: scala.collection.immutable.Map[String, String]) extends LogTrait {

    def findAndReplace(m: Matcher)(callback: String => String): String = {
        val sb = new StringBuffer
        while (m.find) {
            val replStr = subMap(m.group(1))
            m.appendReplacement(sb, callback(replStr))
        }
        m.appendTail(sb)
        sb.toString
    }

    def makeSubstitutions: String = {
        var retrStr = ""
        try {
            val patStr = """\"(\{[A-Za-z0-9_.-]+\})\""""
            val m = Pattern.compile(patStr).matcher(template)
            retrStr = findAndReplace(m) { x => x }
        } catch {
            case e: Exception => retrStr = ""
            case e: Throwable => retrStr = ""
        }
        retrStr
    }

}
