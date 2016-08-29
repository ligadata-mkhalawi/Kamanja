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

import scala.collection.mutable.{ArrayBuffer, Map => MutableMap}
import com.ligadata.kamanja.metadata.{BaseElem, MdMgr, ModelDef}
import com.ligadata.KamanjaBase._
import com.ligadata.Utils.{KamanjaLoaderInfo, Utils}
import org.apache.logging.log4j.LogManager
import java.io.{ByteArrayInputStream, InputStream, PushbackInputStream}
import java.net.{SocketException, SocketTimeoutException, ConnectException}
import java.nio.charset.StandardCharsets

import scala.util.control.Breaks._
import com.ligadata.Exceptions.{KamanjaException, NotImplementedFunctionException}
import com.ligadata.kamanja.serializer.JSONSerDes
//import org.json4s.jackson.JsonMethods._
import org.json4s.native.Json
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._

import scala.collection.immutable.Map
import scala.collection.mutable

object GlobalLogger {
    val loggerName = this.getClass.getName
    val logger = LogManager.getLogger(loggerName)
}


/**
  * An implementation of the FactoryOfModelInstanceFactory trait that supports all of the JPMML models.
  */

object PythonFactoryOfModelInstanceFactory extends FactoryOfModelInstanceFactory {
    private[this] val loggerName = this.getClass.getName
    private[this] val LOG = LogManager.getLogger(loggerName)
    private[this] var jser : JSONSerDes = null

    /**
      * As part of the creation of the model instance factory, see to it that there any jars that it needs (either its
      * own or those upon which it depends) are loaded.
      *
      * @param metadataLoader the engine's custom loader to use if/when instances of the model and the dependent jars are to be loaded
      * @param jarPaths  a Set of Paths that contain the dependency jars required by this factory instance to be created
      * @param elem a BaseElem (actually the ModelDef in this case) with an implementation jar and possible dependent jars
      * @return true if all the jar loading was successful.
      */
    private[this] def LoadJarIfNeeded(metadataLoader: KamanjaLoaderInfo, jarPaths: collection.immutable.Set[String], elem: BaseElem): Boolean = {
        val allJars = GetAllJarsFromElem(jarPaths, elem)
        val loadAcceptable : Boolean = if (allJars.nonEmpty) {
            Utils.LoadJars(allJars.toArray, metadataLoader.loadedJars, metadataLoader.loader)
        } else {
            true
        }
        loadAcceptable
    }

    /**
      * Answer a set of jars that contain the implementation jar and its dependent jars for the supplied BaseElem.
      * The list is checked for valid jar paths before returning.
      *
      * @param jarPaths where jars are located in the cluster node.
      * @param elem the model definition that has a jar implementation and a list of dependency jars
      * @return an Array[String] containing the valid jar paths for the supplied element.
      */
    private[this] def GetAllJarsFromElem(jarPaths: collection.immutable.Set[String], elem: BaseElem): Set[String] = {
        var allJars: Array[String] = null

        val jarname = if (elem.JarName == null) "" else elem.JarName.trim

        if (elem.DependencyJarNames != null && elem.DependencyJarNames.nonEmpty && jarname.nonEmpty) {
            allJars = elem.DependencyJarNames :+ jarname
        } else if (elem.DependencyJarNames != null && elem.DependencyJarNames.nonEmpty) {
            allJars = elem.DependencyJarNames
        } else if (jarname.nonEmpty) {
            allJars = Array(jarname)
        } else {
            return Set[String]()
        }

        allJars.map(j => Utils.GetValidJarFile(jarPaths, j)).toSet
    }


    /**
      * Instantiate a model instance factory for the supplied ''modelDef''.  The model instance factory can instantiate
      * instances of the model described by the ModelDef.
      *
      * @param modelDef the metadatata object for which a model factory instance will be created.
      * @param nodeContext the NodeContext that provides access to model state and kamanja engine services.
      * @param loaderInfo the engine's custom loader to use if/when instances of the model and the dependent jars are to be loaded
      * @param jarPaths a Set of Paths that contain the dependency jars required by this factory instance to be created
      * @return a ModelInstanceFactory or null if bogus information has been supplied.
      *
      * Fixme: Passing the jarPaths as a Set suggests that there is no search order in the path.  Should we have an ordered list
      * instead to allow for alternate implementations where the lib/application directory is first followed by the lib/system
      * directory?  This could be fruitfully used to drop in a fixed implementation ahead of a broken one... this all assumes
      * there is a command to force reload of jars for a given model.
      */
    override def getModelInstanceFactory(modelDef: ModelDef
                                         , nodeContext: NodeContext
                                         , loaderInfo: KamanjaLoaderInfo
                                         , jarPaths: collection.immutable.Set[String]): ModelInstanceFactory = {
      val logSupInfo : String = "In getModelInstanceFactory in PythonFactoryOfModelInstanceFactory "
      if (LOG.isDebugEnabled()) {
        LOG.debug("In getModelInstanceFactory in PythonFactoryOfModelInstanceFactory ")
      }


        LoadJarIfNeeded(loaderInfo, jarPaths, modelDef)

        if (jser == null) {
            jser = new JSONSerDes
            val serOpts : java.util.Map[String,String] = null
            val resolver : ObjectResolver = nodeContext.getEnvCtxt().getObjectResolver
            jser.configure(resolver, serOpts)
        }

        val isReasonable : Boolean = modelDef != null && modelDef.FullNameWithVer != null && modelDef.FullNameWithVer.nonEmpty



        val mdlInstanceFactory : ModelInstanceFactory = if (isReasonable) {

            val factory: PythonAdapterFactory = new PythonAdapterFactory(modelDef, nodeContext, jser)

            if (factory == null) {
                LOG.error(s"Failed to instantiate ModelInstanceFactory... name = $modelDef.FullNameWithVer")
            }
            factory
        }  else {
            null
        }

        mdlInstanceFactory
    }

    /**
      * Answer a model definition for the supplied model string, input message, output message and jarPaths.
      *
      * NOTE: Currently not used.
      *
      * @param nodeContext the NodeContext that provides access to model state and kamanja engine services.
      * @param modelString the model source (for those models that supply source)
      * @param inputMessage the namespace.name.version of the input message this model consumes
      * @param outputMessage the namespace.name.version of the output message this model produces (if any)
      * @param loaderInfo the engine's custom loader to use if/when instances of the model and the dependent jars are to be loaded
      * @param jarPaths a Set of Paths that contain the dependency jars required by this factory instance to be created
      * @return a ModelDef instance
      */
    override def prepareModel(nodeContext: NodeContext
                              , modelString: String
                              , inputMessage: String
                              , outputMessage: String
                              , loaderInfo: KamanjaLoaderInfo
                              , jarPaths: collection.immutable.Set[String]): ModelDef = {
        null
    }
}

/**
  * PythonAdapter serves a "shim" between the engine and the python server that will perform the actual message
  * scoring. It exhibits the "adapter" pattern as discussed in "Design Patterns" by Gamma, Helm, Johnson, and Vlissitudes.
  *
  * Kamanja messages are presented to the adapter and transformed to a Map[FieldName, FieldValue] for consumption by
  * the PMML evaluator associated with the PythonAdapter instance. The target fields (or predictions) and the output fields
  * are returned in the MappedModelResults instance to the engine for further transformation and dispatch.
  *
  * @param factory This model's factory object
  * @param nodeContext the nodeContext that contains the latest connection map values for the node's python servers..
  */

class PythonAdapter(factory : PythonAdapterFactory
                   ,nodeContext : NodeContext
                   ) extends ModelInstance(factory) with LogTrait {

    /** the input and output fields that are desired by the model instance (this info filled in after an addModel request) */
    private var _inputFields : Map[String,Any] = Map[String,Any]()
    private var _outputFields : Map[String,Any] = Map[String,Any]()
    val CONNECTION_KEY : String = "PYTHON_CONNECTIONS"
    private var _partitionKey : String = ""

    /**
     *	One time initialization of the model instance.  For python, the model text is installed on the python server
     *	associated with this partitionKey.  The input and output fields required/produced by the model are retained from
     *	the addModel result.  The input fields in particular are used to optimize what is sent to the python server in
     *	the way of content.
     *
     *	@param partitionKey the partition key that is assigned to this model.
     *
     */
    override def init(partitionKey: String): Unit = {
      _partitionKey = partitionKey
      if (logger.isDebugEnabled()) {
        logger.debug("In init - file PythonFactoryOfModelInstanaceFactory - The partition key value is  -----------" + _partitionKey)
      }


      /** preserve the partition key ... it will be used to get the latest server connection
        * for this model's python server */

      if (factory == null) {
            logger.error("PythonAdapter initialization failed... adapter constructed with a null factory ")
            throw new KamanjaException("PythonAdapter initialization failed... adapter constructed with a null factory",null)
        }
        if (factory.getModelDef() == null) {
          logger.error("PythonAdapter initialization failed... factory's model def is null ")
            throw new KamanjaException("PythonAdapter initialization failed... factory's model def is null",null)
        }

        val pyServerConnection : PyServerConnection = getServerConnection

      if (logger.isDebugEnabled()) {
        logger.debug ()
      }
        if (pyServerConnection == null) {
          val modelName: String = factory.getModelName()
          logger.error(s"Python initialization for model '$modelName' failed... the python server connection could not be established")
          throw new KamanjaException(s"Python initialization for model '$modelName' failed... the python server connection could not be established", null)
        }



      /** retrieve the model options */
        implicit val formats = org.json4s.DefaultFormats
        val modelDef : ModelDef = factory.getModelDef()
        val modelOptStr : String = modelDef.modelConfig
        val trimmedOptionStr : String = modelOptStr.trim
        val modelOptions : Map[String,Any] = parse(trimmedOptionStr).values.asInstanceOf[Map[String,Any]]

        /** add this model to the server's working set */
        val (moduleName, modelName) : (String,String) = ModuleNModelNames

        val resultStr : String = pyServerConnection.addModel(moduleName, modelName, modelDef.objectDefinition, modelOptions)

      if (logger.isDebugEnabled() ){
      logger.debug("PythonAdapter getting called for ... ModuleName :  "
        + moduleName +
        " , ModelName : "
        + modelName +
        ", PartitionID : "
        + _partitionKey)
        }

      val resultMap : Map[String,Any] = parse(resultStr).values.asInstanceOf[Map[String,Any]]
        val host : String = pyServerConnection.host
        val port : String = pyServerConnection.port.toString
        val rc = resultMap.getOrElse("Code", -1)
        if (rc == 0) {
            _inputFields = resultMap.getOrElse("InputFields", Map[String,Any]()).asInstanceOf[Map[String,Any]]
          _outputFields = resultMap.getOrElse("OutputFields", Map[String,Any]()).asInstanceOf[Map[String,Any]]
          if (logger.isDebugEnabled()) {
            logger.debug(s"Module '$moduleName.${modelDef.Name} successfully added to python server at $host($port")
          }
        } else {
            logger.error(s"Module '$moduleName.${modelDef.Name} could not be added to python server at $host($port")
            logger.error(s"error details: '$resultStr'")
        }
    }

    /** Shared function to fetch the current python connection ... The connection is always fetched from the node context
      * value map.  This permits the various players to try to build a new connection should it turn to crap.  The
      * nodeContext contains the necessary information to even change to a new port if necessary.
      *
      * @return the current PyServerConnection
      */
    private def getServerConnection : PyServerConnection = {
        val connectionMap : scala.collection.mutable.HashMap[String,Any] =
            nodeContext.getValue(CONNECTION_KEY).asInstanceOf[scala.collection.mutable.HashMap[String,Any]]
        val pySrvConn : PyServerConnection = connectionMap.getOrElse(_partitionKey, null).asInstanceOf[PyServerConnection]

      val pyServerConnection : PyServerConnection = if (pySrvConn == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("PythonAdapter found no python server connection ... building one now...")
          }
            createConnection(_partitionKey)
        } else {
            pySrvConn
        }

        connectionMap foreach
          ((conn) => logger.debug ("Connection map key values are " +
            conn._1.toString + " = " + conn._2.toString))

        pyServerConnection
    }

    private def ModuleNModelNames : (String,String) = {
        val modelDef : ModelDef = factory.getModelDef()
        val moduleModelNms : Array[String] = modelDef.PhysicalName.split('.')
        val moduleName : String = moduleModelNms.head
        val modelName : String = moduleModelNms.last
        (moduleName,modelName)
    }

    /** Replace a presumably broken python connection with a new one.
      *
      * @return the current PyServerConnection
      */
    private def replaceServerConnection : PyServerConnection = {
        val pyServerConnection : PyServerConnection = createConnection(_partitionKey)

        val connMap : scala.collection.mutable.HashMap[String,Any] =
            nodeContext.getValue(CONNECTION_KEY).asInstanceOf[scala.collection.mutable.HashMap[String,Any]]
        connMap(_partitionKey) = pyServerConnection
        nodeContext.putValue(CONNECTION_KEY, connMap)

        pyServerConnection
    }

    /** This calls when the instance is shutting down. There is no guarantee */
    override def shutdown(): Unit = {
        /** FIXME: the model COULD BE REMOVED from the python server here*/
    }

    /** Process the input messages and produce the modelDefs output messages.
      *
      * @param txnCtxt contains the message and other things
      * @param execMsgsSet the messages to process
      * @param triggerdSetIndex FIXME: There is no explanation of what this is
      * @param outputDefault when true, always produce output, when false return null if the model returns nothing.
      * @return a Array[ContainerOrConcept] ... one or more Containers, concepts, output messages
      */
    override def execute(txnCtxt: TransactionContext
                         , execMsgsSet: Array[ContainerOrConcept]
                         , triggerdSetIndex: Int
                         , outputDefault: Boolean): Array[ContainerOrConcept] = {
        // ... only one input message/one output message supported for now.
        // Holding current transaction information original message and set the new information. Because the model will pull the message from transaction context
        val (origin, orgInputMsg) = txnCtxt.getInitialMessage
        var returnValues : ArrayBuffer[ContainerOrConcept] = ArrayBuffer[ContainerOrConcept]()
        try {
            txnCtxt.setInitialMessage("", execMsgsSet(0).asInstanceOf[ContainerInterface], false)
            val msg = txnCtxt.getMessage()
            val jsonMdlResults : String = evaluateModel(msg)
            if (jsonMdlResults != null) {
                val outMsgName = factory.getModelDef().outputMsgs(0)
                val deser : SerializeDeserialize = factory.serDeserializer
                val outMsg : ContainerInterface = deser.deserialize(jsonMdlResults.getBytes, outMsgName)
                returnValues += outMsg
            } else {
                null
            }
        } catch {
            case e: Exception => throw e
        } finally {
            txnCtxt.setInitialMessage(origin, orgInputMsg, false)
        }
        returnValues.toArray
    }

    /**
      * Prepare the active fields, call the model evaluator, and emit results.
      **
      * @param msg the incoming message containing the values of interest to be assigned to the active fields in the
      *            model's data dictionary.
      * @return
      */
    private def evaluateModel(msg : ContainerInterface): String = {

        val (moduleName, modelName) : (String,String) = ModuleNModelNames
      /** get the current connection for this model's server */
      if (logger.isDebugEnabled()){
        logger.debug (s"Evaluate Model for (moduleName = $moduleName, modelName = '$modelName')")
      }
        val pyServerConnection : PyServerConnection = getServerConnection
        if (pyServerConnection == null) {
            logger.error(s"Python evaluateModel (moduleName = $moduleName, modelName = '$modelName') ... the python server connection could not be obtained... giving up")
            throw new KamanjaException(s"Python initialization for model '$moduleName.$modelName' failed... the python server connection could not be established", null)
        }

        val jsonResults : String = evaluateModel1(pyServerConnection, moduleName, modelName, msg)
        jsonResults
    }

    /**
      * Evaluate the supplied message with the supplied model name.  The connection is the current python connection
      * object in use for this partitionKey.
      *
      * Modest attempt is made to deal with broken connections, busy systems that have caused timeout.
      *
      * It is anticipated there will be issues with kamanja <=> python server communications.  Things go down.
      * Things fall apart.  Indeed the world is passing away and only those who do the will of God will remain.
      *
      * To make this slightly more robust, we are going to catch at least some of the Socket exceptions.  Socket.java
      * can throw these:
      *
      *     IllegalArgumentException
      *     IllegalBlockingModeException
      *     InterruptedIOException
      *     IOException
      *     NullPointerException
      *     PrivilegedActionException
      *     PrivilegedException
      *     SecurityException
      *     SocketException
      *     SocketTimeoutException
      *     UnknownHostException
      *     UnsupportedOperationException
      *
      * A real robust implementation would manage a number of these.  For example an InterruptedIOException might
      * be retried.  An UnknownHostException could be an issue if the python server were being run on a different
      * host than the kamanja node.
      *
      * For now we focus on the SocketException and SocketTimeoutException.  We will try to create a new connection for
      * the first and sleep and retry on the second.  I am not convinced that this is the right behavior, but it will
      * suffice for now.
      *
      * The initial implementation will get a new port.  It is likely that the port is hammered ... at least until
      * the linux has had a chance to clean up the dead process of the failed python server.
      *
      * Fixme: It is possible to run out of ports.  We might want to have a contigency plan of getting an additional
      * band of ports.  This is doable.  The current iterator and band information is found in the nodeContext map.
      *
      * There is going to be many iterations on this as we make "robustness" improvements.
      *
      * @param pyServerConnection a PyServerConnection
      * @param modelName the model that is to evaluate the message
      * @param msg the message from the engine that is to be evaluated
      * @return a message result (the json reply from the executed model on the python server)
      */
    private def evaluateModel1(pyServerConnection: PyServerConnection, modulelName : String, modelName : String, msg : ContainerInterface) : String = {

        var connObj : PyServerConnection = pyServerConnection
        var msgReturned : String = ""
        var attempts : Int = 0
        val attemptsLimit = 10
        var mSecsToSleep : Long = 500

      if (logger.isDebugEnabled()) {
        logger.debug(s"In evaluateModel1 for moduleName = $modulelName, modelName = $modelName")
      }
        breakable {
            while (true) {
                try {
                    attempts += 1
                    if (attempts > attemptsLimit) {
                        logger.error(s"model '$modelName' could not be evaluated...consult prior log entries for what has been tried.")
                        logger.error(s"there are likely communication issues with the python server for the model '$modelName'.")
                        break
                    }
                    msgReturned = pyServerConnection.executeModel(modulelName, modelName, prepareFields(msg))
                  if (logger.isDebugEnabled()) {
                    logger.debug(s"In evaluateModel1 after executeModel for moduleName = $modulelName, modelName = $modelName")
                  }
                    break
                } catch {
                    case se: SocketException => {
                        logger.error(s"SocketException encountered processing $modelName... unable to produce result...exception = ${se.toString}")
                        logger.error(s"obtaining a new connection ... will retry")
                        connObj = replaceServerConnection
                    }
                    case ste: SocketTimeoutException => {
                        /** Fixme: The behavior here is to retry, but wait a twice as long... this is no doubt incorrect... consider this a
                          * place holder for actual behavior for actual problems....
                          */
                      logger.error(s"Exception encountered processing $modelName... unable to produce result...exception = ${ste.toString}")
                      mSecsToSleep *= 2
                      Thread.sleep(mSecsToSleep)
                    }
                  case co : ConnectException => {

                    logger.error(s"Connection Exception encountered processing $modelName... unable to connectt...exception = ${co.toString}")
                    mSecsToSleep *= 2
                    Thread.sleep(mSecsToSleep)
                  }
                    case e: Exception => {
                        /** if it is not one of the supported retry exceptions... we blow out of here */
                        logger.error(s"Exception encountered processing $modelName... unable to produce result...exception = ${e.toString}")
                        break
                    }
                    //case t : Throwable => null  let the engine take this one..
                }
            }
        }
        msgReturned
    }
    /**
      * Send only those fields that are specified in the inputField state retrieved from the addModel result.
      *
      * @param msg the incoming message instance
      * @return a JSON map string suitable for submission to the server.
      */
    private def prepareFields(msg: ContainerInterface) : Map[String,Any] = {
        val inputDictionary : Map[String,Any] = _inputFields.foldLeft(Map.empty[String, Any])((map, fld) => {
            val key = fld._1.toLowerCase
            Option(msg.get(key)).fold(map)(value => {
                map.updated(key, value)
            })
        })

      val msgDict : String = Json(DefaultFormats).write(inputDictionary)
      if (logger.isDebugEnabled()) {
        logger.debug(s"model ${factory.getModelDef().Name} msg = $msgDict")
      }
        inputDictionary
    }

    /**
      * It there is no python connection object available in the node context with this partition key, this function is called to establish a server and
      * also build a connection to it.
      *
      * @param partitionKey supplied at initialization and also available in the txnContext supplied to execute. This is the
      *                     key used to lookup the python server connection object for this model to forward commands to the server.
      */
    private def createConnection(partitionKey : String): PyServerConnection = {

      if (logger.isDebugEnabled()) {
        logger.debug("in createConnection partionKey = " + partitionKey.toString)
      }
        val modelDef : ModelDef = factory.getModelDef()
        if (partitionKey == null) return null // first time engine case... there is no valid key yet ... wait until we got a partition key that is valid

        val connMap : scala.collection.mutable.HashMap[String,Any] =
            nodeContext.getValue(CONNECTION_KEY).asInstanceOf[scala.collection.mutable.HashMap[String,Any]]
      val pyServCon: PyServerConnection = if (connMap != null && connMap.contains(partitionKey)) connMap(partitionKey).asInstanceOf[PyServerConnection] else null

      if (logger.isDebugEnabled()) {
        logger.debug("Checking Python Server  connection ----")
      }
        val pySerververConnection : PyServerConnection = if (pyServCon != null) {

            val connObjStr: String = "valid PyServerConnection previously prepared"
            logger.info(s"PythonAdapterFactory.init($partitionKey) ...values are:\nhost = ${pyServCon.host}\nport = ${pyServCon.port.toString}\npyPath = ${pyServCon.pyPath}\nconnection object = $connObjStr")
            pyServCon
        } else {
            /**
              * The factory doesn't have a connection for this partition key ... make one now
              * FixMe: user, the log config location and the log dir should be added to the nodeContext dict as well and initialized
              * FixMe: from config files
              */
            val host : String = if (nodeContext.getValue("HOST") != null)
                nodeContext.getValue("HOST").asInstanceOf[String]
            else
                null
            val pyPath : String = if (nodeContext.getValue("PYTHON_PATH") != null)
                nodeContext.getValue("PYTHON_PATH").asInstanceOf[String]
            else
                null
            val it : Iterator[Int] = nodeContext.getValue("PYTHON_CONNECTION_PORTS").asInstanceOf[Iterator[Int]]
            val port : Int = it.next()

            val pyLogConfigPath : String = if (nodeContext.getValue("PYTHON_LOG_CONFIG_PATH") != null)
                nodeContext.getValue("PYTHON_LOG_CONFIG_PATH").asInstanceOf[String]
            else
                s"${pyPath}/bin/pythonlog4j.cfg"
            val pyLogPath : String = if (nodeContext.getValue("PYTHON_LOG_PATH") != null)
                nodeContext.getValue("PYTHON_LOG_PATH").asInstanceOf[String]
            else
              s"$pyPath/logs/pythonserver.log"

          val pyBinPath : String = if (nodeContext.getValue("PYTHON_BIN_DIR") != null)
            nodeContext.getValue("PYTHON_BIN_DIR").asInstanceOf[String]
          else
            s"/usr/local/bin/"
            /** peer inside the pyPropMap to see if things are correct when debuging needed */
            val pyPropMap : Map[String,Any] = nodeContext.getValue("pyPropertyMap").asInstanceOf[Map[String,Any]]
	val params : Map[String, Any] =  parse(_partitionKey).values.asInstanceOf[Map[String, Any]]

            val pyConn : PyServerConnection = new PyServerConnection(host
                ,port
                ,"kamanja"
                ,pyLogConfigPath
                ,pyLogPath
              ,pyPath,
              pyBinPath,
	      params("PartitionId").toString)

          if (logger.isDebugEnabled()) {
            logger.debug("Creating connection ---- " + host + ", port = " + port.toString + ", logConfigPath = " + pyLogConfigPath +
              ", LogPath = " + pyLogPath + ", pyPath = " + pyPath + ", pyBinPath " + pyBinPath)
          }

            val (startValid, connValid) : (Boolean, Boolean) = if (pyConn != null) {
                /** setup server and connection to it */
                val (startServerResult, connResult) : (String,String) = pyConn.initialize

                /** how did it go? */
                implicit val formats = org.json4s.DefaultFormats
                val startResultsMap : Map[String,Any] = parse(startServerResult).values.asInstanceOf[Map[String,Any]]
                val startRc : Int = startResultsMap.getOrElse("code", -1).asInstanceOf[scala.math.BigInt].toInt

                val connResultsMap : Map[String,Any] = parse(connResult).values.asInstanceOf[Map[String,Any]]
                val connRc : Int = connResultsMap.getOrElse("code", -1).asInstanceOf[scala.math.BigInt].toInt

                if (startRc == 0 && connRc == 0) {
                    connMap(partitionKey) = pyConn
                    nodeContext.putValue(CONNECTION_KEY, connMap)
                    logger.debug(s"node context's python connection map (key '$CONNECTION_KEY') updated with new server connection (host = $host, port = ${port.toString})... \nstart server result = $startServerResult, \nconnection to server result = $connResult")
                } else {
                    logger.error(s"Problems starting py server on behalf of models running on thread with partition key = ${partitionKey}... in PythonAdapterFactory.init for ${modelDef.FullName}")
                    logger.error(s"startServer result = $startServerResult")
                    logger.error(s"connection result = $connResult")
                }
                (startRc == 0, connRc == 0)
            } else {
                (false, false)
            }
            val isReasonable : Boolean = pyConn != null && startValid && connValid

            if (! isReasonable) {
                logger.error(s"The PythonAdapterFactory for model ${modelDef.FullName} could not be initialized...")
                val hostStr : String = if (host != null) host else "<no host given>"
                val portStr : String = if (port != -1) port.toString else "<no port given>"
                val pyPathStr : String = if (pyPath != null) pyPath else "<no PYTHON_PATH given>"
                val connObjStr : String = if (pyConn != null) "valid PyServerConnection" else "<bad PyServerConnection>"

                logger.error(s"The values are:\nhost = $hostStr\nport = $portStr\npyPath = $pyPathStr\nconnection object = $connObjStr")
            }
          if (logger.isDebugEnabled()) {
            logger.debug("Connection  created ----")
          }
            pyConn
        }
        pySerververConnection
    }
}

/**
  * The PythonAdapterFactory instantiates Python proxy model instance when asked by caller.  Its main function is to
  * instantiate a new python proxy model whenever asked (createModelInstance) and assess whether the current message being processed
  * by the engine is consumable by this model (isValidMessage).
  *
  * The ModelInstanceFactory derivatives are created from FactoryOfModelInstanceFactory at engine startup and whenever
  * a new model is added or changed when the engine is running.
  *
  * @param modelDef the model definition that describes the model that this factory will prepare
  * @param nodeContext the NodeContext object can be used by the model instances to put/get model state needed to
  *                    execute the model.
  */

class PythonAdapterFactory(modelDef: ModelDef, nodeContext: NodeContext, val serDeserializer : SerializeDeserialize) extends ModelInstanceFactory(modelDef, nodeContext) with LogTrait {

   /**
    * Common initialization for all Model Instances. This gets called once per partition id used for each kamanja server node
    *
    * @param txnContext Transaction context to do get operations on this transactionid. But this transaction will be rolledback
    *                   once the initialization is done.
    */
    override def init(txnContext: TransactionContext): Unit = {}

    /**
      * Called when the system is being shutdown, do any needed cleanup ...
      */
    override def shutdown(): Unit = {}

    /**
      * Answer the model name.
      *
      * @return the model namespace.name.version
      */
    override def getModelName(): String = {
        val name : String = if (getModelDef() != null) {
            getModelDef().FullName
        } else {
            val msg : String =  "PythonAdapterFactory: model has no name and no version"
            logger.error("ModelDef in ctor was null...instance could not be built")
            logger.error(msg)
            msg
        }
        name
    }

    /**
      * Answer the model version.
      *
      * @return the model version
      */
    override def getVersion(): String = {
        val withDots: Boolean = true
        if (modelDef != null) {
            MdMgr.ConvertLongVersionToString(modelDef.Version, withDots)
        } else {
            if (withDots) "000000.000001.000000" else "000000000001000000"
        }
    }

    /**
      * Determine if the supplied message can be consumed by the model mentioned in the argument list.  The engine will
      * call this method when a new messages has arrived and been prepared.  It is passed to each of the active models
      * in the working set.  Each model has the opportunity to indicate its interest in the message.
      *
      * @param msg  - the message instance that is currently being processed
      * @return true if this model can process the message.
      */
    override def isValidMessage(msg: MessageContainerBase): Boolean = {
        val msgFullName : String = msg.getFullTypeName
        val msgVersionDots : String = msg.getTypeVersion
        val msgVersion : String = msgVersionDots.filter(_ != '.').toString
        val msgNameKey : String = s"$msgFullName.$msgVersion".toLowerCase()
        val yum : Boolean = if (modelDef != null && modelDef.inputMsgSets != null) {
            val filter = modelDef.inputMsgSets.filter(s => s.length == 1 && s(0).message.toLowerCase == msgNameKey)
            return filter.length > 0
        } else {
            false
        }
        yum
    }

    /**
      * Answer a python proxy model instance.
      *
      * @param txnContext the model instance will be created on behalf of the partition with the key in this object.
      * @return - a ModelInstance that can process the message found in the TransactionContext supplied at execution time
      */
    override def createModelInstance(txnContext: TransactionContext): ModelInstance = {

        val useThisModel : ModelInstance = if (modelDef != null) {
            val partitionKey : String = txnContext.getPartitionKey()
            val isInstanceReusable : Boolean = true
            val builtModel : ModelInstance = new PythonAdapter(this, nodeContext)
            builtModel
        } else {
            logger.error("ModelDef in ctor was null...instance could not be built")
            null
        }
        useThisModel
    }

    /**
      * Answer a ModelResultBase from which to give the model results.
      *
      * @return - a ModelResultBase derivative appropriate for the model
      */
    override def createResultObject(): ModelResultBase = new MappedModelResults

    /**
      *  Is the ModelInstance created by this ModelInstanceFactory is reusable? NOTE: All Python models are resusable.
      *
      *  @return true
      */
    override def isModelInstanceReusable(): Boolean = true


}
