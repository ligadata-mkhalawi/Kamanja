package com.ligadata.MetadataAPI

import com.ligadata.KamanjaBase.FactoryOfModelInstanceFactory
import com.ligadata.kamanja.metadata.MiningModelType._

import com.ligadata.Serialize._
//import org.dmg.pmml.{DataField, FieldName, PMML}

import scala.collection.JavaConverters._

import java.io.{ ByteArrayInputStream, PushbackInputStream, InputStream }
import java.nio.charset.StandardCharsets
import javax.xml.bind.{ ValidationEvent, ValidationEventHandler }
import javax.xml.transform.sax.SAXSource
import java.util.{ List => JList }

import com.ligadata.kamanja.metadata._
//import com.ligadata.jpmml.JpmmlAdapter
////import org.jpmml.model.{JAXBUtil, ImportFilter}
//import org.jpmml.evaluator._
import org.xml.sax.InputSource
import org.xml.sax.helpers.XMLReaderFactory

/**
 * PythonMdlSupport - Add, rebuild, and remove of Python based models from the Kamanja metadata store.
 *
 * It builds an instance of the python model with a Python Model evaluator appropriate for the supplied InputStream
 * containing the python model text.
 *
 * @param mgr            the active metadata manager instance
 * @param modelNamespace the namespace for the model
 * @param modelName      the name of the model
 * @param version        the version of the model in the form "MMMMMM.NNNNNN.mmmmmmm"
 * @param msgNamespace   the message namespace of the message that will be consumed by this model
 * @param msgName        the message name
 * @param msgVersion     the version of the message to be used for this model
 * @param pythonMdlText       the python model to be ingested.
 */

class PythonMdlSupport(mgr: MdMgr, val modelNamespace: String, val modelName: String, val version: String, val msgNamespace: String, val msgName: String, val msgVersion: String, val pythonMdlText: String, val ownerId: String, val tenantId: String) extends LogTrait {

  /**
   * Answer a ModelDef based upon the arguments supplied to the class constructor.
   *
   * @param recompile certain callers are creating a model to recompile the model when the message it consumes changes.
   *                  pass this flag as true in those cases to avoid com.ligadata.Exceptions.AlreadyExistsException
   * @return a ModelDef
   */
  def CreateModel(recompile: Boolean = false, isPython: Boolean): ModelDef = {
    val reasonable: Boolean = (
      mgr != null &&
      modelNamespace != null && modelNamespace.nonEmpty &&
      modelName != null && modelName.nonEmpty &&
      version != null && version.nonEmpty &&
      msgNamespace != null && msgNamespace.nonEmpty &&
      msgName != null && msgName.nonEmpty &&
      pythonMdlText != null && pythonMdlText.nonEmpty)
    /**
     * ******************************************************************
     * TODO - Call the Python Model Evealuater and Generate the Model Def
     *
     *
     *
     * ***************************************************************
     */

    val modelDef: ModelDef = CreateModelDef(recompile, isPython)

    return modelDef

  }

  /**
   * Create ModelDef
   */

  private def CreateModelDef(recompile: Boolean, isPython: Boolean): ModelDef = {

    if (isPython)
      CreatePythonModelDef(recompile)
    else
      CreateJythonModelDef(recompile)

  }

  /**
   * Create Pythin Model Def
   */

  private def CreatePythonModelDef(recompile: Boolean): ModelDef = {
    val onlyActive: Boolean = true
    val latestVersion: Boolean = true
    val facFacDefs: scala.collection.immutable.Set[FactoryOfModelInstanceFactoryDef] = mgr.ActiveFactoryOfMdlInstFactories
    val optPythonMdlFacFac: Option[FactoryOfModelInstanceFactoryDef] = facFacDefs.filter(facfac => {
      facfac.ModelRepSupported == ModelRepresentation.PYTHON
    }).headOption
    val pythonMdlFacFac: FactoryOfModelInstanceFactoryDef = optPythonMdlFacFac.orNull

    val modelDefinition: ModelDef = if (pythonMdlFacFac == null) {
      logger.error(s"While building model metadata for $modelNamespace.$modelName, it was discovered that there is no factory for this model representation (${ModelRepresentation.PYTHON}")
      null
    } else {
      val jarName: String = pythonMdlFacFac.jarName
      val jarDeps: scala.Array[String] = pythonMdlFacFac.dependencyJarNames
      val phyName: String = pythonMdlFacFac.physicalName

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

        val withDots: Boolean = true
        val msgVersionFormatted: String = MdMgr.ConvertLongVersionToString(inputMsg.Version, !withDots)
        val model: ModelDef = mgr.MakeModelDef(modelNamespace, modelName, phyName, ownerId, tenantId, 0, 0, ModelRepresentation.PYTHON, Array(inpMsgs), Array[String](), isReusable, pythonMdlText, null, MdMgr.ConvertVersionToLong(version), jarName, jarDeps, recompile, supportsInstanceSerialization)

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

  /**
   * Create Jython Model
   */
  private def CreateJythonModelDef(recompile: Boolean): ModelDef = {

    val onlyActive: Boolean = true
    val latestVersion: Boolean = true
    val facFacDefs: scala.collection.immutable.Set[FactoryOfModelInstanceFactoryDef] = mgr.ActiveFactoryOfMdlInstFactories
    val optJythonMdlFacFac: Option[FactoryOfModelInstanceFactoryDef] = facFacDefs.filter(facfac => {
      facfac.ModelRepSupported == ModelRepresentation.JYTHON
    }).headOption
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

        val withDots: Boolean = true
        val msgVersionFormatted: String = MdMgr.ConvertLongVersionToString(inputMsg.Version, !withDots)
        val model: ModelDef = mgr.MakeModelDef(modelNamespace, modelName, phyName, ownerId, tenantId, 0, 0, ModelRepresentation.JYTHON, Array(inpMsgs), Array[String](), isReusable, pythonMdlText, null, MdMgr.ConvertVersionToLong(version), jarName, jarDeps, recompile, supportsInstanceSerialization)

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

}