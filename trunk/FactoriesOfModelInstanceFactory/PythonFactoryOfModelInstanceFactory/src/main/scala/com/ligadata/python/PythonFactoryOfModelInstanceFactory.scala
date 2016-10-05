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

import scala.collection.mutable.{Map => MutableMap}
import com.ligadata.kamanja.metadata.{BaseElem, MdMgr, ModelDef}
import com.ligadata.KamanjaBase._
import com.ligadata.Utils.{KamanjaLoaderInfo, Utils}
import org.apache.logging.log4j.LogManager
import java.io.{ByteArrayInputStream, InputStream, PushbackInputStream}
import java.nio.charset.StandardCharsets

import com.ligadata.Exceptions.KamanjaException
import org.json4s.jackson.JsonMethods._
import org.json4s.native.Json
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.{parse => _, _}

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

        LoadJarIfNeeded(loaderInfo, jarPaths, modelDef)

        val isReasonable : Boolean = modelDef != null && modelDef.FullNameWithVer != null && modelDef.FullNameWithVer.nonEmpty
        val mdlInstanceFactory : ModelInstanceFactory = if (isReasonable) {

            val factory: PythonAdapterFactory = new PythonAdapterFactory(modelDef, nodeContext)

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
  * @param pyServerConnection the connection object used to communicate with the python server that the actual model
  *                           runs on.
  */

class PythonAdapter(factory : ModelInstanceFactory
                    , val pyServerConnection : PyServerConnection
                   ) extends ModelInstance(factory) with LogTrait {

    /** the input and output fields that are desired by the model instance (this info filled in after an addModel request) */
    private var _inputFields : Map[String,Any] = Map[String,Any]()
    private var _outputFields : Map[String,Any] = Map[String,Any]()

    /**
     *	One time initialization of the model instance.
     *
     *	@param instanceMetadata model metadata ...
     *
     */
    override def init(instanceMetadata: String): Unit = {
        if (factory == null) {
            logger.error("PythonAdapter initialization failed... adapter constructed with a null factory ")
            throw new KamanjaException("PythonAdapter initialization failed... adapter constructed with a null factory",null)
        }
        if (factory.getModelDef() == null) {
            logger.error("PythonAdapter initialization failed... factory's model def is null ")
            throw new KamanjaException("PythonAdapter initialization failed... factory's model def is null",null)
        }
        if (pyServerConnection == null) {
            logger.error("PythonAdapter initialization failed... adapter constructed with null python server connection")
            throw new KamanjaException("PythonAdapter initialization failed... adapter constructed with null python server connection",null)
        }

        implicit val formats = org.json4s.DefaultFormats
        val modelDef : ModelDef = factory.getModelDef()
        val modelOptStr : String = modelDef.modelConfig
        val trimmedOptionStr : String = modelOptStr.trim
        val modelOptions : Map[String,Any] = parse(trimmedOptionStr).values.asInstanceOf[Map[String,Any]]

        val moduleName : String = modelDef.moduleName
        val resultStr : String = pyServerConnection.addModel(moduleName, modelDef.Name, modelDef.objectDefinition, modelOptions)
        val resultMap : Map[String,Any] = parse(resultStr).values.asInstanceOf[Map[String,Any]]
        val host : String = pyServerConnection.host
        val port : String = pyServerConnection.port.toString
        val rc = resultMap.getOrElse("Code", -1)
        if (rc == 0) {
            _inputFields = resultMap.getOrElse("InputFields", Map[String,Any]()).asInstanceOf[Map[String,Any]]
            _outputFields = resultMap.getOrElse("OutputFields", Map[String,Any]()).asInstanceOf[Map[String,Any]]
            logger.debug(s"Module '$moduleName.${modelDef.Name} successfully added to python server at $host($port")
        } else {
            logger.error(s"Module '$moduleName.${modelDef.Name} could not be added to python server at $host($port")
            logger.error(s"error details: '$resultStr'")
        }
    }

    /** This calls when the instance is shutting down. There is no guarantee */
    override def shutdown(): Unit = {
        /** FIXME: the model COULD BE REMOVED from the python server here*/
    }

    /**
      * The engine will call this method to have the model evaluate the input message and produce a ModelResultBase with results.
      *
      * @param txnCtxt the transaction context (contains message, transaction id, partition key, raw data, et al)
      * @param outputDefault when true, a model result will always be produced with default values.  If false (ordinary case), output is
      *                      emitted only when the model deems this message worthy of report.  If desired the model may return a 'null'
      *                      for the execute's return value and the engine will not proceed with output processing
      * @return a ModelResultBase derivative or null (if there is nothing to report and outputDefault is false).
      */
    override def execute(txnCtxt: TransactionContext, outputDefault: Boolean): ModelResultBase = {
        val msg = txnCtxt.getMessage()
        evaluateModel(msg)
    }

    /**
      * Prepare the active fields, call the model evaluator, and emit results.
      *
      * @param msg the incoming message containing the values of interest to be assigned to the active fields in the
      *            model's data dictionary.
      * @return
      */
    private def evaluateModel(msg : ContainerInterface): ModelResultBase = {

        val modelDef : ModelDef = factory.getModelDef()

        val resultStr : String = pyServerConnection.executeModel(modelDef.Name, prepareFields(msg))
        implicit val formats = org.json4s.DefaultFormats
        val resultsMap : Map[String,Any] = parse(resultStr).values.asInstanceOf[Map[String,Any]]
        val results : Array[(String, Any)] = resultsMap.toArray
        new MappedModelResults().withResults(results)
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
        logger.debug(s"model ${factory.getModelDef().Name} msg = $msgDict")
        inputDictionary
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

class PythonAdapterFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) with LogTrait {

    val CONNECTION_KEY : String = "PYTHON_CONNECTIONS"
   /**
    * Common initialization for all Model Instances. This gets called once per node during the metadata load or corresponding model def change.
    *
    * @param txnContext Transaction context to do get operations on this transactionid. But this transaction will be rolledback
    *                   once the initialization is done.
    */
    override def init(txnContext: TransactionContext): Unit = {
        val partitionKey : String = txnContext.getPartitionKey()

       if (nodeContext.getValue(partitionKey) != null) {
           val pyCon: PyServerConnection = nodeContext.getValue(partitionKey).asInstanceOf[PyServerConnection]
           val connObjStr: String = if (pyCon != null) "valid PyServerConnection" else "<bad PyServerConnection>"
           logger.info(s"PythonAdapterFactory.init($partitionKey) ...values are:\nhost = ${pyCon.host}\nport = ${pyCon.port.toString}\npyPath = ${pyCon.pyPath}\nconnection object = $connObjStr")
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
            val connMap : mutable.HashMap[String,Any] = nodeContext.getValue(CONNECTION_KEY).asInstanceOf[mutable.HashMap[String,Any]]
            val pyPropMap : Map[String,Any] = nodeContext.getValue("pyPropertyMap").asInstanceOf[Map[String,Any]]

            val pyConn : PyServerConnection = new PyServerConnection(host
                                                                    ,port
                                                                    ,"kamanja"
                                                                    ,pyLogConfigPath
                                                                    ,pyLogPath
                                                                    ,pyPath)
            val (startValid, connValid) : (Boolean, Boolean) = if (pyConn != null) {
                /** setup server and connection to it */
                val (startServerResult, connResult) : (String,String) = pyConn.initialize

                /** how did it go? */
                implicit val formats = org.json4s.DefaultFormats
                val startResultsMap : Map[String,Any] = parse(startServerResult).values.asInstanceOf[Map[String,Any]]
                val startRc : Int = startResultsMap.getOrElse("code", -1).asInstanceOf[Int]

                val connResultsMap : Map[String,Any] = parse(connResult).values.asInstanceOf[Map[String,Any]]
                val connRc : Int = connResultsMap.getOrElse("code", -1).asInstanceOf[Int]

                if (startRc == 0 && connRc == 0) {
                    connMap(partitionKey) = pyConn
                    nodeContext.putValue(partitionKey, connMap)
                } else {
                    logger.error(s"Problems starting py server on behalf of models running on ${partitionKey}... in PythonAdapterFactory.init for ${modelDef.FullName}")
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
        }

    }

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
    override def createModelInstance(): ModelInstance = {
      throw new Exception("Unhandled Python createModelInstance")
/*
        val useThisModel : ModelInstance = if (modelDef != null) {
            val partitionKey : String = txnContext.getPartitionKey()
            val isInstanceReusable : Boolean = true
            val connMap : mutable.HashMap[String,Any] = nodeContext.getValue(CONNECTION_KEY).asInstanceOf[mutable.HashMap[String,Any]]
            val pyServerConnection : PyServerConnection = connMap.getOrElse(partitionKey,null).asInstanceOf[PyServerConnection]
            val builtModel : ModelInstance = new PythonAdapter(this, pyServerConnection)
            builtModel
        } else {
            logger.error("ModelDef in ctor was null...instance could not be built")
            null
        }
        useThisModel
*/
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



