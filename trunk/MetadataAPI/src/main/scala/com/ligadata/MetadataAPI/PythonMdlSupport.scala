package com.ligadata.MetadataAPI

import java.io._
import java.util.Properties

import scala.sys.process._
import scala.util.Random
import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.ligadata.Serialize._
import com.ligadata.kamanja.metadata._
import org.json4s.native.JsonMethods.{parse => _, _}

import scala.collection.immutable.Map


/**
 * PythonMdlSupport - Add, rebuild, and remove of Python based models from the Kamanja metadata store.
 *
 * It builds an instance of the python model with a Python Model evaluator appropriate for the supplied InputStream
 * containing the python model text.
 *
 * @param mgr            the active metadata manager instance
 * @param moduleName     the python module name stem from the file name (as in moduleName.py)
 * @param modelNamespace the namespace for the model (the moduleName)
 * @param modelName      the name of the model
 * @param version        the version of the model in the form "MMMMMM.NNNNNN.mmmmmmm"
 * @param msgNamespace   the message namespace of the message that will be consumed by this model
 * @param msgName        the message name
 * @param msgVersion     the version of the message to be used for this model
 * @param pythonMdlText  the python model to be ingested.
 * @param ownerId  the owner of this cluster
 * @param tenantId the tenant for whom this model is being added/updated.
 * @param optMsgProduced the output message this model should produce
 * @param pStr global options (JSON) that may be supplied to control certain ingestion mechanisms
 * @param modelOptions  model specific options to be saved with the model definition produced here... elements used
 *                      at model instance construction time complete initialization... do things peculiar to this model.
 * @param metadataAPIConfig the Properties file for the metadata api application that contains certain information needed
 *                          to complete the production of the model definition.
 */

class PythonMdlSupport ( val mgr: MdMgr
                       , val moduleName: String
                       , val modelNamespace: String
                       , val modelName: String
                       , val version: String
                       , val msgNamespace: String
                       , val msgName: String
                       , val msgVersion: String
                       , val pythonMdlText: String
                       , val ownerId: String
                       , val tenantId: String
                       , val optMsgProduced : Option[String]
                       , val pStr : Option[String]
                       , val modelOptions : String
                       , val metadataAPIConfig: Properties ) extends LogTrait {

    /** Kamanja's PYTHONPATH key where a) server code is located and b) where the Python Module directories to support
      * the python server are found (i.e., common, commands, models sub-directories)
      */
  val PYTHON_PATH : String = "PYTHON_PATH"
  val PYTHON_BIN_DIR : String = "PYTHON_BIN_DIR"
    val PYTHON_CONFIG : String = "PYTHON_CONFIG"
    val ROOT_DIR : String = "ROOT_DIR"

    /** generate a random string for portion of python compile file name */
    val random : Random = new Random(java.lang.System.currentTimeMillis())
    val randomFileNameStemSuffix : String = Random.alphanumeric.take(8).mkString

    /**
    * Answer a ModelDef based upon the arguments supplied to the class constructor.
    *
    * @param recompile certain callers are creating a model to recompile the model when the message it consumes changes.
    *                  pass this flag as true in those cases to avoid com.ligadata.Exceptions.AlreadyExistsException
    * @return a ModelDef
    *
    */
  def CreateModel(recompile: Boolean = false, isPython: Boolean): ModelDef = {
    val reasonable: Boolean = mgr != null &&
                              moduleName != null && moduleName.nonEmpty &&
                              modelNamespace != null && // empty ok for python ... moduleName really is namespace for python modules.
                                                        // modelNamespace.nonEmpty &&
                              modelName != null && modelName.nonEmpty &&
                              version != null && version.nonEmpty &&
                              msgNamespace != null && msgNamespace.nonEmpty &&
                              msgName != null && msgName.nonEmpty &&
                              pythonMdlText != null && pythonMdlText.nonEmpty &&
                              modelOptions != null && modelOptions.nonEmpty &&
                              metadataAPIConfig != null && metadataAPIConfig.size() > 0
    val modelDef: ModelDef = if (reasonable) {
        CreateModelDef(recompile, isPython)
    } else {
        null
    }

    return modelDef

  }

  /**
   * Create ModelDef
   */
  private def CreateModelDef(recompile: Boolean, isPython: Boolean): ModelDef = {

    /** if properties have been supplied, then give them to the to the model def makers */
    val pythonPropertiesStr : String = metadataAPIConfig.getProperty(PYTHON_CONFIG)
    implicit val formats = org.json4s.DefaultFormats
    val pyPropertyMap : Map[String,Any] = parse(pythonPropertiesStr).values.asInstanceOf[Map[String,Any]]
    val modelDef : ModelDef = if (isPython) {
        val pypathExists : Boolean = pyPropertyMap != null && pyPropertyMap.contains(PYTHON_PATH)
        val mdlD : ModelDef = if (pypathExists) {
            CreatePythonModelDef(recompile, pyPropertyMap)
        } else {
            /** fail this ... absolutely must have python config and location of the python path */
            val pyCfgIsThere : Boolean = pyPropertyMap != null
            val pyPathIsNotThere: Boolean = pyCfgIsThere
            logger.error(s"In the api configuration there is either no python config (key = ${PYTHON_CONFIG.toUpperCase}) or the python path is missing in it... gotta have it... cfg present? $pyCfgIsThere pypath present? $pyPathIsNotThere")
            null
        }
        mdlD
    } else {
        /** FixMe: Will config info be needed for Jython? Only time will tell */
        CreateJythonModelDef(recompile, pyPropertyMap)
    }
    modelDef
  }

  /**
   * Create Python ModelDef
   *
   * @param recompile when true, an update context...
   * @return
   */
  private def CreatePythonModelDef(recompile: Boolean, pyPropertyMap : Map[String,Any]): ModelDef = {
    val onlyActive: Boolean = true
    val latestVersion: Boolean = true
    val facFacDefs: scala.collection.immutable.Set[FactoryOfModelInstanceFactoryDef] = mgr.ActiveFactoryOfMdlInstFactories
    val optPythonMdlFacFac: Option[FactoryOfModelInstanceFactoryDef] = facFacDefs.find(ff => ff.ModelRepSupported == ModelRepresentation.PYTHON)
    val pythonMdlFacFac: FactoryOfModelInstanceFactoryDef = optPythonMdlFacFac.orNull

    val modelDefinition: ModelDef = if (pythonMdlFacFac == null) {
      logger.error(s"While building model metadata for $modelNamespace.$modelName, it was discovered that there is no factory for this model representation (${ModelRepresentation.PYTHON}")
      null
    } else {
      val jarName: String = pythonMdlFacFac.jarName
      val jarDeps: scala.Array[String] = pythonMdlFacFac.dependencyJarNames
        /** for python, we want to preserve the case of the module and moduleName.  It is put into the phyName which the
          * metadata manager will not fold (unlike namespace and name) to lowercase.
          */
      val phyName: String = s"$moduleName.$modelName"

      /** make sure new msg is there. */
      val msgver: Long = MdMgr.ConvertVersionToLong(msgVersion)
      val optInputMsg: Option[MessageDef] = mgr.Message(msgNamespace, msgName, msgver, onlyActive)
      val inputMsg: MessageDef = optInputMsg.orNull

      val modelD: ModelDef = if (inputMsg != null) {

        val inpMsgs = if (inputMsg != null) {
          val t = new MessageAndAttributes
          t.origin = "" //FIXME:- Fill this if looking for specific input
          t.message = inputMsg.FullName
          t.attributes = Array[String]()
          Array(t)
        } else {
          Array[MessageAndAttributes]()
        }

      /**
        * reasonable python text should be compilable... let's do so now.  Reject if non 0 return code.
        */
        val pyPath : String = pyPropertyMap(PYTHON_PATH).toString
          //val modelDir : String = if (pyPath.endsWith("/")) s"${pyPath.dropRight(1)}/models" else s"$pyPath/models"
          //val pyFileName : String = s"${modelName}_$randomFileNameStemSuffix.py"
        val modelDir : String = if (pyPath.endsWith("/")) s"${pyPath.dropRight(1)}/tmp" else s"$pyPath/tmp"
        val pyFileName : String = s"${moduleName}.py"
        val pyFilePath : String = s"$modelDir/$pyFileName"
        val pyFileCmd : String = s"-m py_compile $pyFilePath"
        val exportcmd : String = s"export PYTHONPATH=$pyPath"
        val pybinpath : String = pyPropertyMap(PYTHON_BIN_DIR).toString
        val pybindir : String = if (pybinpath.endsWith("/")) pybinpath else pybinpath + "/"

        writeSrcFile(pythonMdlText, pyFilePath)
        val cmdSeq : Seq[String] = Seq[String](pybindir + "python", "-m", "compileall", modelDir)
        val (rc, stdoutResult, stderrResult) : (Int, String, String) = runCmdCollectOutput(cmdSeq)
        //val rmCompileFiles : String = pyFilePath.dropRight(3) + "*" /** rm the .py and .pyc */
        val rmCompileFiles : String = s"$modelDir/*"
        val rmFileCmd : String = s"rm -f $rmCompileFiles"
        val killDirRc = Process(rmFileCmd).!  // clean up the placed py src file... regardless of compile outcome.

        logger.debug(s"result of python src file for $modelNamespace.$modelName ($pyFilePath) compilation = $rc\nstdout=$stdoutResult\nstderr=$stderrResult")
        logger.debug(s"python file cleanup of $pyFilePath following compile = $killDirRc")

        /** Can the JSON modelOptions string be tranformed into a map or list? */
        val optionsReasonable : Boolean = try {
            implicit val formats = org.json4s.DefaultFormats
            val trimmedOptionStr: String = modelOptions.trim
            val mOrL = if (trimmedOptionStr.startsWith("{")) {
                parse(trimmedOptionStr).values.asInstanceOf[Map[String, Any]]
                true
            } else if (trimmedOptionStr.startsWith("[")) {
                parse(trimmedOptionStr).values.asInstanceOf[List[Map[String, Any]]]
                true
            } else {
                logger.error("The supplied python options must be expressed as a JSON string... ")
                false
            }
            mOrL
        }  catch {
            case _ : Throwable => {
                false
            }
        }

        val model : ModelDef = if (rc != 0 || ! optionsReasonable) {
            if (rc != 0)
                logger.error(s"The supplied python text failed to compile... model name = $modelName... model catalog fails")
            if (! optionsReasonable)
                logger.error(s"The supplied modelOptions JSON string could not be parsed... modelOptions = '$modelOptions'")
            null
        } else {
            val isReusable: Boolean = true
            val supportsInstanceSerialization: Boolean = false // FIXME: not yet

            val withDots: Boolean = true
            val msgVersionFormatted: String = MdMgr.ConvertLongVersionToString(inputMsg.Version, !withDots)
            val outMsg : String = optMsgProduced.orNull
            val outMsgs : Array[String] = if (outMsg != null) Array[String](outMsg) else Array[String]()
            val mdl: ModelDef = mgr.MakeModelDef(modelNamespace
                , modelName
                , phyName
                , ownerId
                , tenantId
                , 0
                , 0
                , ModelRepresentation.PYTHON
                , Array(inpMsgs)
                , outMsgs
                , isReusable
                , pythonMdlText
                , MiningModelType.PYTHON
                , MdMgr.ConvertVersionToLong(version)
                , jarName
                , jarDeps
                , recompile
                , supportsInstanceSerialization
                , modelOptions
                , moduleName)

            /** dump the model def to the log for time being */
            logger.debug(modelDefToString(mdl))
            mdl
        }
        model
      } else {
        logger.error(s"The supplied message def is not available in the metadata... msgName=$msgNamespace.$msgName.$msgVersion ... a model definition will not be created for model name=$modelNamespace.$modelName.$version")
        null
      }
      modelD
    }
    modelDefinition
  }

  /**
   * Create Jython ModelDef
   */
  private def CreateJythonModelDef(recompile: Boolean, pyPropertyMap : Map[String,Any]): ModelDef = {

    val onlyActive: Boolean = true
    val latestVersion: Boolean = true
    val facFacDefs: scala.collection.immutable.Set[FactoryOfModelInstanceFactoryDef] = mgr.ActiveFactoryOfMdlInstFactories
    val optJythonMdlFacFac: Option[FactoryOfModelInstanceFactoryDef] = facFacDefs.find(ff => ff.ModelRepSupported == ModelRepresentation.JYTHON)
    val jythonMdlFacFac: FactoryOfModelInstanceFactoryDef = optJythonMdlFacFac.orNull

    val modelDefinition: ModelDef = if (jythonMdlFacFac == null) {
      logger.error(s"While building model metadata for $modelNamespace.$modelName, it was discovered that there is no factory for this model representation (${ModelRepresentation.JYTHON}")
      null
    } else {
      val jarName: String = jythonMdlFacFac.jarName
      val jarDeps: scala.Array[String] = jythonMdlFacFac.dependencyJarNames
      val phyName: String = jythonMdlFacFac.physicalName

      /** make sure new msg is there. */
      val msgver: Long = MdMgr.ConvertVersionToLong(msgVersion)
      val optInputMsg: Option[MessageDef] = mgr.Message(msgNamespace, msgName, msgver, onlyActive)
      val inputMsg: MessageDef = optInputMsg.orNull

      val modelD: ModelDef = if (inputMsg != null) {

        val inpMsgs = if (inputMsg != null) {
          val t = new MessageAndAttributes
          t.origin = "" //FIXME:- Fill this if looking for specific input
          t.message = inputMsg.FullName
          t.attributes = Array[String]()
          Array(t)
        } else {
          Array[MessageAndAttributes]()
        }

        val isReusable: Boolean = true
        val supportsInstanceSerialization: Boolean = false // FIXME: not yet

      /**
        * Fixme: Reasonable jython model src text can be compiled... do so now and reject if there are stderr messages
        * Fixme: something like the c python check above needs to be done here.
        */

        val withDots: Boolean = true
        val msgVersionFormatted: String = MdMgr.ConvertLongVersionToString(inputMsg.Version, !withDots)
        val outMsg : String = optMsgProduced.orNull
        val outMsgs : Array[String] = if (outMsg != null) Array[String](outMsg) else Array[String]()
        val model: ModelDef = mgr.MakeModelDef(modelNamespace
            , modelName
            , phyName
            , ownerId
            , tenantId
            , 0
            , 0
            , ModelRepresentation.JYTHON
            , Array(inpMsgs)
            , outMsgs
            , isReusable
            , pythonMdlText
            , MiningModelType.JYTHON
            , MdMgr.ConvertVersionToLong(version)
            , jarName
            , jarDeps
            , recompile
            , supportsInstanceSerialization
            , modelOptions
            , moduleName)

        /** dump the model def to the log for time being */
        logger.debug(modelDefToString(model))
        model
      } else {
        logger.error(s"The supplied message def is not available in the metadata... msgName=$msgNamespace.$msgName.$msgVersion ... a model definition will not be created for model name=$modelNamespace.$modelName.$version")
        null
      }
      modelD
    }
    modelDefinition
  }

   /** Prepare a new model with the new python/jython source supplied in the constructor.
    *
    * @return a newly constructed model def that reflects the new PMML source
    */
  def UpdateModel(isPython: Boolean): ModelDef = {
    logger.debug("UpdateModel is a synonym for CreateModel")
    val recompile: Boolean = false
    CreateModel(recompile, isPython)
  }

  /**
   * diagnostic... generate a JSON string to print to the log for the supplied ModelDef.
   *
   * @param modelDef the model def of interest
   * @return a JSON string representation of the ModelDef almost suitable for printing to log or console.
   */
  def modelDefToString(modelDef: ModelDef): String = {
    val abbreviatedModelSrc: String = if (modelDef.objectDefinition != null && modelDef.objectDefinition.length > 100) {
      modelDef.objectDefinition.take(99)
    } else {
      if (modelDef.objectDefinition != null) {
        modelDef.objectDefinition
      } else {
        "no source"
      }
    }

    var jsonStr: String = JsonSerializer.SerializeObjectToJson(modelDef)
    jsonStr
  }

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
    private def runCmdCollectOutput(cmd: Seq[String]): (Int, String, String) = {
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
      * Write the supplied source text to the target path (path and file name)
      *
      * @param srcTxt
      * @param targetPath
      */
    private def writeSrcFile(srcTxt: String, targetPath: String) {
        val file = new File(targetPath);
        val bufferedWriter = new BufferedWriter(new FileWriter(file))
        bufferedWriter.write(srcTxt)
        bufferedWriter.close
    }

}
