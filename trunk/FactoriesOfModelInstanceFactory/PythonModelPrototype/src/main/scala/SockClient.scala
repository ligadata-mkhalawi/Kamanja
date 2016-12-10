//#!/bin/bash
//exec scala "$0" "$@"
//!#

import java.net._
import java.io._
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.regex.{Matcher, Pattern}

import org.json4s.{DefaultFormats, Formats, JsonAST, MappingException}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.collection.JavaConverters._
import scala.io._
import scala.collection.immutable.Map
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.List
import scala.sys.process._
import scala.util.control.Breaks._

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

object SockClientTools {
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
        val stdoutStream = new ByteArrayOutputStream
        val stderrStream = new ByteArrayOutputStream
        val stdoutWriter = new PrintWriter(stdoutStream)
        val stderrWriter = new PrintWriter(stderrStream)
        val exitValue = cmd.!(ProcessLogger(stdoutWriter.println, stderrWriter.println))
        stdoutWriter.close()
        stderrWriter.close()
        (exitValue, stdoutStream.toString, stderrStream.toString)
    }

    /**
      * Translate the supplied JSON string.
      *
      * @param jsonStr ...a JSON map, a JSON list, or a JSON list of JSON maps
      * @return Any ... the caller is expected to know how to cast this value
      */

    def jsonStringAsColl(jsonStr: String): Any = {
        val jsonObjs : Any = try {
            implicit val jsonFormats: Formats = DefaultFormats
            val json = parse(jsonStr)
            println(s"Parsed the json : \n$jsonStr")
            json.values
        } catch {
            case e: MappingException => {
                println(s"mapping exception...\n${e.toString}")
                throw e
            }
            case e: Exception => {
                println(s"exception encountered...\n${e.toString}")
                throw e
            }
        }
        jsonObjs
    }

    /** Serialize a simple name value map to a suitable json string fragment for inclusion in the
      * server messages.
      *
      * @param msgMap a Map[String,Any] ... e.g., input values
      * @return json string rep
      */
    def SerializeSimpleMapToJson(msgMap: Map[String, Any]): String = {

        val buffer : StringBuilder = new StringBuilder
        buffer.append("{ ")
        msgMap.toList.foreach(m => {
            buffer.append(s"${'"'}${m._1}${'"'} : ${m._2.toString}, ")
        })

        val jsonStr : String = buffer.toString.dropRight(2) // scrape off trailing comma and space
        val enclosedJsonStr : String = s"$jsonStr }"
        enclosedJsonStr
    }

}

/**
  * MapSubstitution used to stitch in home grown json strings possibly passed as argument to SockClient
  * into one of the messages destined for the pythonserver.  See executeModel for example.
  *
  * Embedded keys look like this regexp: val patStr = """(\{[A-Za-z0-9_.-]+\})"""
  *
  * That is:
  *     {This_is_.my.1.key}
  *
  * @param template the string to have its embedded keys substituted
  * @param subMap a map of substutitions to make.
  */
class MapSubstitution(template: String, subMap: scala.collection.immutable.Map[String, String]) {

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


object SockClient {


    /** cmdMap contains command preparation objects for each command type */
    val cmdMap : Map[String, PyCmd] =
        List[(String,PyCmd)](
            ("addModel" , new AddModelCmd("addModel"))
            ,("removeModel" , new RemoveModelCmd("removeModel"))
            ,("serverStatus" , new ServerStatusCmd("serverStatus"))
            ,("executeModel" , new ExecuteModelCmd("executeModel"))
            ,("startServer" , new StartServerCmd("startServer"))
            ,("stopServer" , new StopServerCmd("stopServer"))
        ).toMap

    /** Usage message for console display */
    def usage: String = {
        """
  One of the following must be supplied. Currently only one command at a time is permitted.

  SocketClient.scala <named args...>
  where <named args> are:

    --cmd startServer   [--host <hostname or ip> ... default = localhost]
                        [--port <user port no.> ... default=9999]
                        [--user <userId.> ... default="kamanja"]
                        --pyPath <location of Kamanja python installation>
                        --log4jConfig <path to the log4j configuration>
                        [--fileLogPath <log file path> ... default $pyPath/logs/pythonserver.log]
    --cmd stopServer    [--host <hostname or ip> ... default = localhost]
                        [--port <user port no.> ... default=9999]
                        [--user <userId.> ... default="kamanja"]
    --cmd addModel      [--host <hostname or ip> ... default = localhost]
                        [--port <user port no.> ... default=9999]
                        [--user <userId.> ... default="kamanja"]
                        --filePath <filePath>
                        --modelOptions <JSON string (map) defining with input and output messages.  Keys are InputMsgs
                        and OutputMsgs respectively. InputMsgs and OutputMsgs values are arrays (there can be multiple
                        messages ingested by a model or produced by one).  Other values can be presented in the map
                        as necessary to be utilized by the model instance that will run on the server.
                        --pyPath <location of Kamanja python installation>
    --cmd removeModel   [--host <hostname or ip> ... default = localhost]
                        [--port <user port no.> ... default=9999]
                        [--user <userId.> ... default="kamanja"]
                        --modelName <modelName>
    --cmd serverStatus  [--host <hostname or ip> ... default = localhost]
                        [--port <user port no.> ... default=9999]
                        [--user <userId.> ... default="kamanja"]
    --cmd executeModel  [--host <hostname or ip> ... default = localhost]
                        [--port <user port no.> ... default=9999]
                        [--user <userId.> ... default="kamanja"]
                        --modelName '<modelName>'
                        --msg '<msg data expressed as json map string>'
    --cmd executeModel  [--host <hostname or ip> ... default = localhost]
                        [--port <user port no.> ... default=9999]
                        [--user <userId.> ... default="kamanja"]
                        --modelName '<modelName>'
                        --filePath '<msg file path>'


  It is possible to run multiple servers on same host simply by varying 
  the port number.
        """
    }


    def main (args : Array[String]) {
        val arglist = args.toList
        if (args.isEmpty) {
            println(usage)
            sys.exit(0)
        }

        type OptionMap = Map[Symbol, String]
        def nextOption(map: OptionMap, list: List[String]): OptionMap = {
            list match {
                case Nil => map
                case "--cmd" :: value :: tail =>
                    nextOption(map ++ Map('cmd -> value), tail)
                case "--filePath" :: value :: tail =>
                    nextOption(map ++ Map('filePath -> value), tail)
                case "--modelName" :: value :: tail =>
                    nextOption(map ++ Map('modelName -> value), tail)
                case "--msg" :: value :: tail =>
                    nextOption(map ++ Map('msg -> value), tail)
                case "--host" :: value :: tail =>
                    nextOption(map ++ Map('host -> value), tail)
                case "--port" :: value :: tail =>
                    nextOption(map ++ Map('port -> value), tail)
                case "--user" :: value :: tail =>
                    nextOption(map ++ Map('user -> value), tail)
                case "--modelOptions" :: value :: tail =>
                    nextOption(map ++ Map('modelOptions -> value), tail)
                case "--pyPath" :: value :: tail =>
                    nextOption(map ++ Map('pyPath -> value), tail)
                case "--log4jConfig" :: value :: tail =>
                    nextOption(map ++ Map('log4jConfig -> value), tail)
                case "--fileLogPath" :: value :: tail =>
                    nextOption(map ++ Map('fileLogPath -> value), tail)
                case option :: tail => println("Unknown option " + option)
                    sys.exit(1)
            }
        }
        val options = nextOption(Map(), arglist)
        val cmd : String = if (options.contains('cmd)) options.apply('cmd) else null
        val filePath : String = if (options.contains('filePath)) options.apply('filePath) else null
        val modelName : String = if (options.contains('modelName)) options.apply('modelName) else null
        val msg : String = if (options.contains('msg)) options.apply('msg) else null
        val user : String = if (options.contains('user)) options.apply('user) else "rich"
        val host : String = if (options.contains('host)) options.apply('host) else "localhost"
        val portNo : Int = if (options.contains('port)) options.apply('port).toInt else 9999
        val pyPath : String = if (options.contains('pyPath)) options.apply('pyPath) else null
        val log4jConfig : String = if (options.contains('log4jConfig)) options.apply('log4jConfig) else null
        val fileLogPath : String = if (options.contains('fileLogPath)) options.apply('fileLogPath) else null

        val modelOptions : String = if (options.contains('modelOptions)) options.apply('modelOptions) else null

        val fileLogPathToBeUsed : String = if (fileLogPath != null) {
            fileLogPath
        } else {
            if (pyPath != null) {
                s"$pyPath/logs/pythonserver.log"
            } else {
                ""
            }
        }

        val ok : Boolean = cmd != null
        if (! ok) {
            println("\nInvalid command..\n")
            println(usage)
            sys.exit(1)
        }

        /** known command? */
        val cmdObj : PyCmd = cmdMap.getOrElse(cmd, null)
        if (cmdObj == null) {
            println("\nCommand not recognized...\n")
            println(usage)
            sys.exit(1)
        }

        /** command specific checks */
        val (cmdOk,errMsg) : (Boolean,String) =
            cmdObj.semanticCheck(filePath
                , modelName
                , msg
                , user
                , host
                , portNo
                , modelOptions
                , pyPath)
        if (! cmdOk) {
            println(s"\nCommand $cmd parameters are invalid...$errMsg\n")
            println(usage)
            sys.exit(1)
        }


        if (cmd == "startServer") {
            val startServerCmd : StartServerCmd = cmdObj.asInstanceOf[StartServerCmd]
            val result : String = startServerCmd.startServer(filePath, user, host, portNo, pyPath, log4jConfig, fileLogPathToBeUsed)
            println(s"Server started? $result")
        } else {
	        /** Prepare the command message for transport (except for the case of the
              * StartServerCmd instance... NOTE: multiple cmds may be prepared */
	        val cmdMsgs : Array[Array[Byte]] = cmdObj.prepareCmdMsg(filePath
	            , modelName
	            , msg
	            , user
	            , host
	            , portNo
                , modelOptions
                , pyPath)
            /** send the command to the server for execution */
            val inetbyname = InetAddress.getByName(host)
            println("inetbyname = " + inetbyname)
            val sock : Socket = new Socket(inetbyname, portNo)
            lazy val in = new DataInputStream(sock.getInputStream)
            //lazy val in = new BufferedSource(s.getInputStream).getLines()
            val out = new DataOutputStream(sock.getOutputStream)
            //val out = new PrintStream(s.getOutputStream)

            if (cmdMsgs.length == 0) {
                println("there were no commands formed... abandoning processing")
            } else {
                val buffer: Array[Byte] = new Array[Byte](2 ^ 16) // 64k
                cmdMsgs.foreach( msg => {
                    processOneMsg(in, out, cmd, msg, buffer)
                })
            }
            sock.close()
        }
        sys.exit(0)
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
    def processOneMsg(in : DataInputStream, out : DataOutputStream, cmd : String, cmdMsg : Array[Byte], buffer : Array[Byte]) : Unit = {
        //out.println(cmdMsg)
        val cmdLen: Int = cmdMsg.length
        out.write(cmdMsg, 0, cmdLen)
        out.flush()

        /** Contend with multiple messages results returned */
        val answeredBytes: ArrayBuffer[Byte] = ArrayBuffer[Byte]()
        var bytesReceived = in.read(buffer)
        breakable {
            while (bytesReceived > 0) {
                answeredBytes ++= buffer.slice(0, bytesReceived)
                /** print one result each loop... and then the remaining (if any) after bytesReceived == 0) */
                val endMarkerIdx: Int = answeredBytes.indexOfSlice(CmdConstants.endMarkerArray)
                if (endMarkerIdx >= 0) {
                    val endMarkerIncludedIdx: Int = endMarkerIdx + CmdConstants.endMarkerArray.length
                    val responseBytes: Array[Byte] = answeredBytes.slice(0, endMarkerIncludedIdx).toArray
                    val response: String = unpack(responseBytes)
                    println(s"$cmd reply = \n$response")
                    answeredBytes.remove(0, endMarkerIncludedIdx)
                    break
                }
                bytesReceived = in.read(buffer)
            }
        }

        val lenOfRemainingAnsweredBytes: Int = answeredBytes.length
        while (lenOfRemainingAnsweredBytes > 0) {
            val endMarkerIdx: Int = answeredBytes.indexOfSlice(CmdConstants.endMarkerArray)
            if (endMarkerIdx >= 0) {
                val endMarkerIncludedIdx: Int = endMarkerIdx + CmdConstants.endMarkerArray.length
                val responseBytes: Array[Byte] = answeredBytes.slice(0, endMarkerIncludedIdx).toArray
                val response: String = unpack(responseBytes)
                println(response)
                answeredBytes.remove(0, endMarkerIncludedIdx)
            } else {
                if (answeredBytes.nonEmpty) {
                    println("There were residual bytes remaining in the answer buffer suggesting that the connection went down")
                    println(s"Bytes were '${answeredBytes.toString}'")
                }
            }
        }
    }

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
            //println(s"startMark = $startMarkStr, crc = $crc, payload len = $payloadLen")
    		val payloadArray : scala.Array[Byte] = new scala.Array[Byte](payloadLen)
			byteBuffer.get(payloadArray,0,payloadLen)
			byteBuffer.get(endMark,0,endMarkerValueLen)
            val endMarkStr : String = new String(endMark)
			val payloadStr : String = new String(payloadArray)
            //println(s"payload = $payloadStr")
            //println(s"endMark = $endMarkStr")
            payloadStr
    	} else {
    		"unreasonable bytes returned... either null or insufficient bytes in the supplied result"
    	}
 		answer
    }
}

/** Abstract class for objects that handle semantic checks and server command preparation
  */
abstract class PyCmd (val cmd : String) {
    def semanticCheck(filePath : String, modelName : String, msg : String, user : String, host : String, portNo : Int, modelOptions : String, pyPath : String) : (Boolean,String)
    def prepareCmdMsg(filePath : String, modelName : String, msg : String, user : String, host : String, portNo : Int, modelOptions : String, pyPath : String) : Array[Array[Byte]]
}

/** Commands: StartServerCmd, StopServerCmd, AddModelCmd, RemoveModelCmd, ServerStatusCmd, ExecuteModelCmd */

/**
  * StartServerCmd
  */
class StartServerCmd(cmd : String) extends PyCmd(cmd) {

    override def semanticCheck(filePath : String
                               , modelName : String
                               , msg : String
                               , user : String
                               , host : String
                               , portNo : Int
                               , modelOptions : String
                               , pyPath : String) : (Boolean,String) = {

        val isLocalHost : Boolean = host == "localhost"
        val errMsg : String = if (isLocalHost) "" else "only local host supported for this build"
        (isLocalHost, errMsg)
    }

    /**
      * the server command is a local command ... no remote command prep... this is a no op
      */
    override def prepareCmdMsg(filePath : String
                               , modelName : String
                               , msg : String
                               , user : String
                               , host : String
                               , portNo : Int
                               , modelOptions : String
                               , pyPath : String) : Array[Array[Byte]] = {
        val fauxcmd : Array[Byte] = Array[Byte](0)
        new Array[Array[Byte]](0) :+ fauxcmd
    }

    /**
      * Start the pythonserver.
      *
      * @param filePath (not used)
      * @param user the user that issued this command (tenant)
      * @param host the host that will be used for the pythonserver
      * @param portNo the port on which the server will listen
      * @param pyPath the kamanja python module path where the server lives and the models will be copied
      * @param log4jConfigPath the log4j config file to be used by the python server being started
      * @param fileLogPath the log file path for the python server
      * @return the result of the start server command
      *
      */
    def startServer(filePath : String
                    , user : String
                    , host : String
                    , portNo : Int
                    , pyPath : String
                    , log4jConfigPath : String
                    , fileLogPath : String) : String = {

        val useSSH : Boolean = host != "localhost"

        val pythonCmdStr = s"python $pyPath/pythonserver.py --host $host --port ${portNo.toString} --pythonPath $pyPath --log4jConfig $log4jConfigPath --fileLogPath $fileLogPath"
        val cmdSeq : Seq[String] = if (useSSH) {
            val userMachine : String = s"$user@$host"
            val remoteCmd : String = s"python $pythonCmdStr"
            Seq[String]("ssh", userMachine, remoteCmd)
        } else {
            println(s"Start the python server... $pythonCmdStr")
            Seq[String]("bash", "-c", pythonCmdStr)
        }


        /**
          * Note that if we ask for the result, the call will block.  This is not a good idea for the start server.
          * We really want it to be put in background.  We might add a process logging here to get the output from
          * the pythonserver via ProcessLogger (see runCmdCollectOutput for example that waits for completion with
          * the .! invocation.
          */

        val pySrvCmd = Process(cmdSeq)
        pySrvCmd.run
        val startResult : Int = 0  /** if there are no exceptions, it succeeds */

        val processInfo = ("ps aux" #| "grep python" #| s"grep ${portNo.toString}").!!.trim
        val re = """[A-Za-z0-9]+[\t ]+([0-9]+).*""".r
        val allMatches = re.findAllMatchIn(processInfo)

        val pids : ArrayBuffer[String] = ArrayBuffer[String]()
        allMatches.foreach ( m =>
            pids += m.group(1)
        )

        //println(pids.toString)

        val pid : String = if (pids != null) pids.last else null
        val result : String = if (pid != null) "Server started successfully" else "Server start failed"
        val pidStr : String = if (pid != null) pid else s"${'"'}----${'"'}"

        val resultStr : String = s"{ ${'"'}result${'"'} : ${'"'}$result${'"'},  ${'"'}pid${'"'} : $pidStr }"

        resultStr
    }
}

/** StopServerCmd */
class StopServerCmd(cmd : String) extends PyCmd(cmd) {

    /** FIXME: do meaningful checks here */
    override def semanticCheck(filePath : String
                               , modelName : String
                               , msg : String
                               , user : String
                               , host : String
                               , portNo : Int
                               , modelOptions : String
                               , pyPath : String) : (Boolean,String) = {
        (true,"")
    }

    /**
      * Example stopServer command:
      * '''
      * {
      *     "Cmd": "stopServer",
      *     "CmdVer" : 1,
      *     "CmdOptions": {},
      *     "ModelOptions": {}
      * }
      * '''
      *
      * @param filePath (unused for serverStatus)
      * @param modelName (unused for serverStatus)
      * @param msg (unused for serverStatus)
      * @param user the user that submitted this addModel command ... the 'tenant'
      * @param host the host that has the target server
      * @param portNo the port with which the server is listening for commands
      * @param modelOptions (unused for stopServer
      * @param pyPath (unused for stopServer)
      * @return
      */
    override def prepareCmdMsg(filePath: String
                               , modelName: String
                               , msg: String
                               , user: String
                               , host: String
                               , portNo: Int
                               , modelOptions : String
                               , pyPath : String) : Array[Array[Byte]] = {
        val json = (
            ("Cmd" -> cmd) ~
            ("CmdVer" -> 1) // ~
            //("CmdOptions" -> List[String]() ~
            //("ModelOptions" -> List[String]())
            )
        val cmdJson : String = compact(render(json))

        val payloadStr : String = cmdJson
        val payload : Array[Byte] = payloadStr.getBytes
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

        //Array[Array[Byte]] = {
        new Array[Array[Byte]](0) :+ cmdBytes

    }
}

/**
  * AddModelCmd
  *
  *  Required: filePath, modelNm, host, port, user, and pyPath.
  *
  *  The modelOptions can supply the input message layout as an unnamed dictionary if desired.  The result
  *  or score field will be automatically added to these fields for the output message requirement.
  *  This is just for testing.
  *
  */
class AddModelCmd(cmd : String) extends PyCmd(cmd) {

    /** FIXME: do meaningful checks here */
    override def semanticCheck(filePath : String
                               , modelNm : String
                               , msg : String
                               , user : String
                               , host : String
                               , portNo : Int
                               , modelOptions : String
                               , pyPath : String) : (Boolean,String) = {
        (true,"")
    }

    /**
      * AddModelCmd prep consists of copying the supplied filePath to the pyPath on the
      * supplied machine followed by preparing the command message for it...
      *
      * Sample message:
      *
      *
      * {
      *     "Cmd": "addModel",
      *     "CmdVer" : 1,
      *     "CmdOptions": {
      *         "ModelFile": "a.py",
      *         "ModelName": "a"
      *     },
      *     "ModelOptions": {}
      * }
      *
      * Multiple input and output messages are possible, assuming the model can handle that.
      *
      * @param filePath the python model program file to copy to the pyPath
      * @param modelName the name of the model (the class name) found in that file
      * @param msgs (unused for addModel)
      * @param user the user that submitted this addModel command ... the 'tenant'
      * @param host the host that has the target server
      * @param portNo the port with which the server is listening for commands
      * @param modelOptions the options for (principally) addModel that describe the messages the model will ingest/emit
      * @param pyPath Kamanja's python sys.path entrant on the server machine that was supplied to the python startServer
      *               command.  Currently models are added to the $pypath/models.  NOTE: In a future release, the
      *               modelNm parameter is a namespace.name and the nodes of the namespace provide the directory structure
      *               within the pyPath (i.e., no more models forced into the models subdirectory).
      * @return an Array[Byte] representing the addModel command
      *
      */
    override def prepareCmdMsg(filePath : String
                               , modelName : String
                               , msgs : String
                               , user : String
                               , host : String
                               , portNo : Int
                               , modelOptions : String
                               , pyPath : String) : Array[Array[Byte]] = {


        /** copy the python model source file to $pyPath/models */
        val useSSH : Boolean = host != "localhost"
        val slash : String = if (pyPath != null && pyPath.endsWith("/")) "" else "/"
        val fromCpArgsStr : String = s"$filePath"
        val toCpArgsStr : String = s"$pyPath${slash}models/"
        val cmdSeq : Seq[String] = if (useSSH) {
            val userMachine : String = s"$user@$host"
            Seq[String]("scp", userMachine, fromCpArgsStr, toCpArgsStr)
        } else {
            println(s"copy model $filePath locally to $pyPath${slash}models/")
            Seq[String]("cp", fromCpArgsStr, toCpArgsStr)
        }
        val (result, stdoutStr, stderrStr) : (Int, String, String) = SockClientTools.runCmdCollectOutput(cmdSeq)
        if (result != 0) {
            println(s"AddModel failed... unable to copy $filePath to $pyPath${slash}models/")
            println(s"copy error message(s):\n\t$stderrStr")
            return new Array[Array[Byte]](0)
        }

        /** prepare the message */
        val modelFile : String = filePath.split('/').last.trim /** just the file name */
        val json = (
            ("Cmd" -> cmd) ~
            ("CmdVer" -> 1) ~
            ("CmdOptions" -> (
                ("ModelFile" -> modelFile) ~
                ("ModelName" -> modelName)
            )) ~
            ("ModelOptions" -> (
                ("TypeInfo" -> "{TYPEINFO_KEY}")
            ))
        )
        val addMsg : String = compact(render(json))

        val modOpts : String = if (modelOptions != null && modelOptions.length > 0) modelOptions else "{}"
        val subMap : Map[String,String] = Map[String,String]("{TYPEINFO_KEY}" -> modelOptions)
        val sub = new MapSubstitution(addMsg, subMap)
        val jsonCmdMsg : String = sub.makeSubstitutions


        val payloadStr : String = jsonCmdMsg
        val payload : Array[Byte] = payloadStr.getBytes
        val checksumBytes : ByteBuffer = ByteBuffer.allocate(CmdConstants.lenOfCheckSum)
        checksumBytes.putLong(0L)
        val chkBytesArray : scala.Array[Byte] = checksumBytes.array()
        val lenBytes : ByteBuffer = ByteBuffer.allocate(CmdConstants.lenOfInt)
        lenBytes.putInt(payload.length)
        val payloadLenAsBytes : Array[Byte] = lenBytes.array()

        println(s"addModel msg = ${CmdConstants.startMarkerValue} 0L ${payload.length} $jsonCmdMsg ${CmdConstants.endMarkerValue}")

        val cmdBytes : Array[Byte] = CmdConstants.startMarkerArray ++
            chkBytesArray ++
            payloadLenAsBytes ++
            payload ++
            CmdConstants.endMarkerArray

        println(s"addModel msg len = ${cmdBytes.length}")

        //Array[Array[Byte]] = {
        new Array[Array[Byte]](0) :+ cmdBytes
    }
}

/** RemoveModelCmd */
class RemoveModelCmd(cmd : String) extends PyCmd(cmd) {

    /** FIXME: do meaningful checks here */
    override def semanticCheck(filePath: String
                               , modelName: String
                               , msg: String
                               , user: String
                               , host: String
                               , portNo: Int
                               , modelOptions : String
                               , pyPath : String) : (Boolean,String) = {
        (true,"")
    }

    /**
      * Example removeModel command message:
      *
      * '''
      * {
      *     "Cmd": "removeModel",
      *     "CmdVer" : 1,
      *     "CmdOptions": {
      *         "ModelName": "a"
      *     },
      *     "ModelOptions": {}
      * }
      * '''
      *
      * @param filePath (unused for removeModel)
      * @param modelName (the name of the model to be removed from the server
      * @param msg (unused for removeModel)
      * @param user the user that submitted this addModel command ... the 'tenant'
      * @param host the host that has the target server
      * @param portNo the port with which the server is listening for commands
      * @param modelOptions (unused for removeModel)
      * @param pyPath (unused for removeModel)
      * @return
      */
    override def prepareCmdMsg(filePath: String
                               , modelName: String
                               , msg: String
                               , user: String
                               , host: String
                               , portNo: Int
                               , modelOptions : String
                               , pyPath : String): Array[Array[Byte]] = {

        val json = (
            ("Cmd" -> cmd) ~
            ("CmdVer" -> 1) ~
            ("CmdOptions" -> (
                ("ModelName" -> modelName) //~
                //("InputMsgs" -> msg)
            ))
            //("ModelOptions" -> List[String]())
        )
        val cmdJson : String = compact(render(json))

        val payloadStr : String = cmdJson
        val payload : Array[Byte] = payloadStr.getBytes
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

        //Array[Array[Byte]] = {
        new Array[Array[Byte]](0) :+ cmdBytes
    }
}

/** ServerStatusCmd */
class ServerStatusCmd(cmd: String) extends PyCmd(cmd) {

    /** FIXME: do meaningful checks here */
    override def semanticCheck(filePath: String
                               , modelName: String
                               , msg: String
                               , user: String
                               , host: String
                               , portNo: Int
                               , modelOptions : String
                               , pyPath : String) : (Boolean,String) = {
        (true,"")
    }

    /**
      * Example serverStatus command:
      *
      * '''
      * {
      *     "Cmd": "serverStatus",
      *     "CmdVer" : 1,
      *     "CmdOptions": {},
      *     "ModelOptions": {}
      * }
      * '''
 *
      * @param filePath (unused for serverStatus)
      * @param modelName (unused for serverStatus)
      * @param msg (unused for serverStatus)
      * @param user the user that submitted this addModel command ... the 'tenant'
      * @param host the host that has the target server
      * @param portNo the port with which the server is listening for commands
      * @param modelOptions (unused for serverStatus)
      * @param pyPath (unused for serverStatus)
      * @return
      */
    override def prepareCmdMsg(filePath: String
                               , modelName: String
                               , msg: String
                               , user: String
                               , host: String
                               , portNo: Int
                               , modelOptions : String
                               , pyPath : String) : Array[Array[Byte]] = {
        val json = (
            ("Cmd" -> cmd) ~
            ("CmdVer" -> 1) //~
            //("CmdOptions" -> List[String]() ~
            //("ModelOptions" -> List[String]())
            )
        val cmdJson : String = compact(render(json))

        val payloadStr : String = cmdJson
        val payload : Array[Byte] = payloadStr.getBytes
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

        //Array[Array[Byte]] = {
        new Array[Array[Byte]](0) :+ cmdBytes
    }
}

/** ExecuteModelCmd */
class ExecuteModelCmd(cmd : String) extends PyCmd(cmd) {

    /** FIXME: do meaningful checks here */
    override def semanticCheck(filePath : String
                               , modelName : String
                               , msg : String
                               , user : String
                               , host : String
                               , portNo : Int
                               , modelOptions : String
                               , pyPath : String) : (Boolean,String) = {
        (true,"")
    }

    /**
      * Prepare an one or more executeModel commands.  When the filePath is not null, the content of the
      * file is expected to be csv input with a header record that describes the field names.
      *
      * Recall that the input message field types are either apriori known by the model or have been sent
      * to the model with the key of TypeInfo in the ModelOptions dictionary.  See AddModelCmd for more details.
      * In any event, the data items had jolly well better match up with the expectations of the model.
      *
      * A single message can also be supplied with inline JSON data.
      *
      * @param filePath if supplied, a CSV file with header line is expected. Multiple elements appear
      *                 in the InputMsgs list.  The header names had better match the _sole_ input message.
      *                 This is just a hack to test out the multiple command
      * @param modelName the name of the model
      * @param msg one message ... a flattened version of the InputDictionary dictionary with the field name/value pairs
      *            (e.g., {"a": 1, "b": 2 } for the InputDictionary above)
      * @param user the user that submitted this addModel command ... the 'tenant'
      * @param host the host that has the target server
      * @param portNo the port with which the server is listening for commands
      * @param modelOptions conceivably used for executeModel, but not currently
      * @param pyPath (unused by executeModel).
      * @return an Array[Byte] containing the command message ready for transmission to the server
      *
      * Pre-conditions:
      *    For this release, only one message can be ingested
      */
    override def prepareCmdMsg(filePath : String
                               , modelName : String
                               , msg : String
                               , user : String
                               , host : String
                               , portNo : Int
                               , modelOptions : String
                               , pyPath : String) : Array[Array[Byte]] = {

        val oneOrMoreCmds : Array[Array[Byte]] =  if (filePath != null) {
            prepareCmdMsgs(filePath, modelName, msg, user, host, portNo, modelOptions, pyPath)
        } else {
            prepareOneCmdMsg(filePath, modelName, msg, user, host, portNo, modelOptions, pyPath)
        }

        oneOrMoreCmds
    }

    /**
      * Prepare one executeModel message for transmission to the server.
      *
      * Sample executeModel command prepared:
      *
      * '''
      * {
      *     "Cmd": "executeModel",
      *     "CmdVer" : 1,
      *     "CmdOptions": {
      *         "ModelName": "a",
      *         "InputDictionary": {
      *             "a": 1,
      *             "b": 2
      *         }
      *     },
      *     "ModelOptions": {}
      * }
      * '''
      *
      * @param filePath if supplied, a CSV file with header line is expected. Multiple elements appear
      *                 in the InputMsgs list.  The header names had better match the _sole_ input message.
      *                 This is just a hack to test out the multiple command
      * @param modelName the name of the model
      * @param msg a flattened version of the InputDictionary dictionary with the field name/value pairs
      *            (e.g., {"a": 1, "b": 2 } for the InputDictionary above)
      * @param user the user that submitted this addModel command ... the 'tenant'
      * @param host the host that has the target server
      * @param portNo the port with which the server is listening for commands
      * @param modelOptions conceivably used for executeModel, but not currently
      * @param pyPath (unused by executeModel).
      * @return an Array[Byte] containing the command message ready for transmission to the server
      *
      * Pre-conditions:
      *    For this release, only one message can be ingested
      */
    private def prepareOneCmdMsg(filePath : String
                               , modelName : String
                               , msg : String
                               , user : String
                               , host : String
                               , portNo : Int
                               , modelOptions : String
                               , pyPath : String) : Array[Array[Byte]] = {

        if (filePath != null) {
            println("executeModel failure!  Method prepareCmdMsg does not accept file input... method prepareCmdMsgs should have been used... logic error....")
            return new Array[Array[Byte]](0)
        }

        val msgJson : scala.collection.immutable.Map[String,Any] =
            SockClientTools.jsonStringAsColl(msg).asInstanceOf[scala.collection.immutable.Map[String,Any]]
        val msgFieldMap : String =  SockClientTools.SerializeSimpleMapToJson(msgJson)

        val json = (
            ("Cmd" -> cmd) ~
                ("CmdVer" -> 1) ~
                ("CmdOptions" -> (
                    ("ModelName" -> modelName) ~
                        ("InputDictionary" -> "{DATA.KEY}")
                    )) //~
            //("ModelOptions" -> List[String]())
            )
        val jsonCmdTemplate : String = compact(render(json))

        val subMap : Map[String,String] = Map[String,String]("{DATA.KEY}" -> msgFieldMap)
        val sub = new MapSubstitution(jsonCmdTemplate, subMap)
        val jsonCmdMsg : String = sub.makeSubstitutions

        val subMap1 : Map[String,String] = Map[String,String]("{DATA.KEY}" -> msg)
        val sub1 = new MapSubstitution(jsonCmdTemplate, subMap1)
        val jsonCmdMsg1 : String = sub1.makeSubstitutions

        val payload : Array[Byte] = jsonCmdMsg1.getBytes
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

        //Array[Array[Byte]] = {
        new Array[Array[Byte]](0) :+ cmdBytes
    }

    /**
      * Prepare multiple executeModel commands from the content of the filePath.
      *
      * @param filePath if supplied, a CSV file with header line is expected. The header names had better match
      *                 the _sole_ input message in the InputMsgs list of the model when it was added.  This is principally
      *                 to test the ability of the server to manage multiple incoming messages before an output response
      *                 is prepared for the first and subsequent messages.  Incoming messages can be burst to the server.
      * @param modelName the name of the model
      * @param msg the name of the message that has field names found in the first record of the filePath file.
      * @param user the user that submitted this addModel command ... the 'tenant'
      * @param host the host that has the target server
      * @param portNo the port with which the server is listening for commands
      * @param modelOptions conceivably used for executeModel, but not currently
      * @param pyPath (unused by executeModel).
      * @return an array of commands (byte arrays) prepared from the records found in the filePath
      */
    def prepareCmdMsgs(filePath : String
                       , modelName : String
                       , msg : String
                       , user : String
                       , host : String
                       , portNo : Int
                       , modelOptions : String
                       , pyPath : String) : Array[Array[Byte]]  = {

        if (filePath == null) {
            println("executeModel failure!  There was no file sent to the prepareCmdMsgs... logic error....")
            return Array[Array[Byte]]()
        }

        val cmdStr : String = Source.fromFile(filePath).mkString
        val cmdContent : Array[String] = cmdStr.split('\n')
        val cmdContentCnt : Int = cmdContent.size
        val reasonable : Boolean = cmdContentCnt > 1
        val cmds : Array[Array[Byte]] = if (reasonable) {
            val (msgHeader,dataContent) : (Array[String], Array[String]) = cmdContent.splitAt(1)
            val msgNames : Array[String] = msgHeader.head.split(',').map(_.trim)

            val cmdStrings : Array[String] = dataContent.map(msgValues => {
                val values : Array[String] = msgValues.split(',').map(fieldVal => sansQuotes(fieldVal.trim))
                val pairs : Array[(String,String)] = msgNames.zip(values)

                val msgFieldMap : String =  SockClientTools.SerializeSimpleMapToJson(pairs.toMap)
                val json = (
                    ("Cmd" -> cmd) ~
                        ("CmdVer" -> 1) ~
                        ("CmdOptions" -> (
                            ("ModelName" -> modelName) ~
                                ("InputDictionary" -> "{DATA.KEY}")
                            )) //~
                    //("ModelOptions" -> List[String]())
                    )
                val jsonCmdTemplate : String = compact(render(json))

                val subMap1 : Map[String,String] = Map[String,String]("{DATA.KEY}" -> msgFieldMap)
                val sub1 = new MapSubstitution(jsonCmdTemplate, subMap1)
                val cmdMsg : String = sub1.makeSubstitutions
                cmdMsg
            })

            val arrayOfCmds : Array[Array[Byte]] = cmdStrings.map(jsonCmd => {
                val payload : Array[Byte] = jsonCmd.getBytes
                val checksumBytes : ByteBuffer = ByteBuffer.allocate(CmdConstants.lenOfCheckSum)
                checksumBytes.putLong(0L)
                val chkBytesArray : scala.Array[Byte] = checksumBytes.array()
                val lenBytes : ByteBuffer = ByteBuffer.allocate(CmdConstants.lenOfInt)
                lenBytes.putInt(payload.length)
                val payloadLenAsBytes : Array[Byte] = lenBytes.array()
                val cmdsAsBytes : Array[Byte] = CmdConstants.startMarkerArray ++
                    chkBytesArray ++
                    payloadLenAsBytes ++
                    payload ++
                    CmdConstants.endMarkerArray
                cmdsAsBytes
            })
            arrayOfCmds

        } else {
            println(s"executeModel failure!  \nThe message input file sent to the prepareCmdMsgs is malformed... \nIt contains only one (or no) records... \nThere must be a CSV header that gives field names (the names should match those found in the InputMsgs message supplied to the addModel command)...\n...and at least one input data record of csv values corresponding to the names in the header.")
            new Array[Array[Byte]](0)
        }


        cmds
    }

    /**
      * Use org.json4s.jackson.JsonMethods to serialize an executeCommand for single msg
      *
      * @param cmd
      * @param modelName
      * @param inputMsgName
      * @param msgFieldValuePairs
      * @return
      */
    private def SerializeCmdToJson(cmd : String
                           ,modelName: String
                           ,inputMsgName: String
                           ,msgFieldValuePairs : Array[(String,String)]
                          ): String = {

        val msgJson : String = SerializeMsgToJson(inputMsgName, msgFieldValuePairs)
        val msgs : List[String] = List[String](msgJson)
        val json = (
            ("Cmd" -> cmd) ~
            ("CmdVer" -> 1) ~
            ("CmdOptions" -> (
                ("ModelName" -> modelName) ~
                ("InputDictionary" -> msgs)
            )) //~
            //("ModelOptions" -> List[String]())
        )
        val cmdMsg : String = compact(render(json))
        cmdMsg
    }

    /**
      * Use org.json4s.jackson.JsonMethods to serialize an executeCommand
      *
      *
      *         val json = (
      * ("Cmd" -> cmd) ~
      * ("CmdVer" -> 1) ~
      * ("CmdOptions" -> (
      * ("ModelName" -> modelName) ~
      * ("InputDictionary" -> "{DATA.KEY}")
      * )) //~
      * //("ModelOptions" -> List[String]())
      * )
      *
      * @param inputMsgName the name of the message
      * @param msgFieldValuePairs field name/value pairs
      * @return json string for the message to be processed
      */
    private def SerializeMsgToJson(inputMsgName: String,msgFieldValuePairs : Array[(String,String)]): String = {
        val json = (
            ("InputDictionary" -> msgFieldValuePairs.toList.map ( pair => {
                val name : String = pair._1
                (s"${'"'}$name${'"'}" -> pair._2)
            }))
        )
        val msgJson : String = compact(render(json))
        msgJson
    }



    /**
      * Trim off csv quote marks if they are both ends of the value...only double quote supported
      *
      * @param value a value from the csv array of the record being processed.
      * @return a cleaned up string sans quotes as needed.
      */
    def sansQuotes(value : String) : String = {
        val hasStartQuote : Boolean = value.startsWith(s"${'"'}")
        val hasEndQuote : Boolean = value.endsWith(s"${'"'}")
        val answer : String = if (hasStartQuote && hasEndQuote) {
            value.dropRight(1).drop(1)
        } else {
            value
        }
        answer
    }
}

//SockClient.main(args)

