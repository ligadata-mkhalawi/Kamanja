package com.ligadata.tool.generatemessage

/**
  * Created by Yousef on 5/24/2016.
  */

import org.dmg.pmml._

import scala.collection.JavaConverters._
import scala.collection.mutable
import java.io.{ByteArrayInputStream, PushbackInputStream, InputStream}
import java.nio.charset.StandardCharsets
import javax.xml.bind.{ValidationEvent, ValidationEventHandler}
import javax.xml.transform.sax.SAXSource
import java.util.{List => JList}

import org.jpmml.model.{JAXBUtil, ImportFilter}
import org.jpmml.evaluator._
import org.xml.sax.InputSource
import org.xml.sax.helpers.XMLReaderFactory
//import scala.collection.JavaConversions._

class PMMLUtility extends LogTrait{
  def XMLReader(pmmlText: String): ModelEvaluator[_ <: Model]={
    val inputStream: InputStream = new ByteArrayInputStream(pmmlText.getBytes(StandardCharsets.UTF_8))

    val is = new PushbackInputStream(inputStream)

    val reader = XMLReaderFactory.createXMLReader()
    reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    val filter = new ImportFilter(reader)
    val source = new SAXSource(filter, new InputSource(is))
    val unmarshaller = JAXBUtil.createUnmarshaller
    unmarshaller.setEventHandler(SimpleValidationEventHandler)

    val pmml: PMML = unmarshaller.unmarshal(source).asInstanceOf[PMML]
    val modelEvaluatorFactory = ModelEvaluatorFactory.newInstance()
    val modelEvaluator = modelEvaluatorFactory.newModelManager(pmml)

    if (modelEvaluator == null){
      logger.error("no fields found in pmml file")
      sys.exit(1)
    }

    return modelEvaluator
  }

  def ActiveFields(modelEvaluator: ModelEvaluator[_ <: Model]): scala.Array[(String,String)] ={
    val dataTypeBean: DataTypeUtility = new DataTypeUtility
    val activeFieldNames: JList[FieldName] = modelEvaluator.getActiveFields
    val activeFields: scala.Array[DataField] = {
      activeFieldNames.asScala.map(nm => modelEvaluator.getDataField(nm))
    }.toArray
    val activeFieldContent : scala.Array[(String,String)] = activeFields.map(fld => {
      if(fld.getDataType == null && fld.getOpType != null){
        if(fld.getOpType.toString.trim.equalsIgnoreCase("continuous"))
          (fld.getName.getValue, "Double")
        else (fld.getName.getValue, "String")
      } else if(fld.getDataType != null)
      (fld.getName.getValue, dataTypeBean.FindPMMLFieldType(fld.getDataType.value))
      else (fld.getName.getValue, "String")
    })
    return activeFieldContent
  }

  def OutputFields(modelEvaluator: ModelEvaluator[_ <: Model]): scala.Array[(String,String)] ={
    val dataTypeBean: DataTypeUtility = new DataTypeUtility
    val outputFieldNames: JList[FieldName] = modelEvaluator.getOutputFields
    val outputFields: scala.Array[OutputField] = {
      outputFieldNames.asScala.filter(nm => modelEvaluator.getOutputField(nm) != null).map(nm => modelEvaluator.getOutputField(nm))
    }.toArray
    val outputFieldContent : scala.Array[(String,String)] = if (outputFields != null && outputFields.length > 0) {
      outputFields.map(fld => {
        if(fld.getDataType == null && fld.getOpType != null){
          if(fld.getOpType.toString.trim.equalsIgnoreCase("continuous"))
            (fld.getName.getValue, "Double")
          else (fld.getName.getValue, "String")
        } else if(fld.getDataType != null) {
        (fld.getName.getValue, dataTypeBean.FindPMMLFieldType(fld.getDataType.value))
      }else (fld.getName.getValue, "String")
      })
    } else {
      scala.Array[(String,String)]()
    }

    return outputFieldContent
  }

  def TargetFields(modelEvaluator: ModelEvaluator[_ <: Model]): scala.Array[(String,String)] ={
    val dataTypeBean: DataTypeUtility = new DataTypeUtility
    val targetFieldNames: JList[FieldName] = modelEvaluator.getTargetFields
    //val x = modelEvaluator.getMiningField(targetFieldNames.get(0))
    val targetFields: scala.Array[FieldName] = {
      targetFieldNames.asScala.map(nm => nm)
    }.toArray
    val targetFieldContent : scala.Array[(String,String)] = if (targetFieldNames != null && targetFieldNames.size() > 0) {
      //for( i <- 0 until targetFieldNames.size()){
        //println(targetFieldNames.get(i))
      targetFields.map(fld => {
        val field : FieldName = fld
        var optype: OpType = null
        val miningField: MiningField = modelEvaluator.getMiningField(field)
        val datafield : DataField =  modelEvaluator.getDataField(field)
        if(miningField != null && miningField.getOpType != null) {
          optype = miningField.getOpType
        }
        if(datafield != null && datafield.getOpType != null){
          optype = datafield.getOpType
        }
        if(datafield.getDataType != null){
          (datafield.getName.getValue, dataTypeBean.FindPMMLFieldType(datafield.getDataType.value))
        } else if(optype != null){
          if(optype.toString.trim.equalsIgnoreCase("continuous"))
            (datafield.getName.getValue, "Double")
          else (datafield.getName.getValue, "String")
        } else (datafield.getName.getValue,"String")
      })
    } else {
      scala.Array[(String,String)]()
    }
//    val targetFields: scala.Array[Target] = {
//      targetFieldNames.asScala.filter(nm => modelEvaluator.getTarget(nm) != null).map(nm => modelEvaluator.getTarget(nm))
//    }.toArray
//    val targetFieldContent : scala.Array[(String,String)] = if (targetFields != null && targetFields.length > 0) {
//      targetFields.map(fld => {
//        val field : FieldName = fld.getField
//        var optype: OpType = null
//        val miningField: MiningField = modelEvaluator.getMiningField(field)
//        val datafield : DataField =  modelEvaluator.getDataField(field)
//        if(miningField != null && miningField.getOpType != null) {
//           optype = miningField.getOpType
//        }
//        if(datafield != null && datafield.getOpType != null){
//          optype = datafield.getOpType
//        }
//        if(datafield.getDataType != null){
//        (datafield.getName.getValue, dataTypeBean.FindPMMLFieldType(datafield.getDataType.value))
//        } else if(optype != null){
//          if(optype.toString.trim.equalsIgnoreCase("continuous"))
//            (datafield.getName.getValue, "Double")
//          else (datafield.getName.getValue, "String")
//        } else (datafield.getName.getValue,"String")
//
//      })
//    } else {
//      scala.Array[(String,String)]()
//    }
    return targetFieldContent
  }

  def OutputMessageFields(outputFields: scala.Array[(String,String)], targetFields: scala.Array[(String,String)]): mutable.LinkedHashMap[String, String] ={
    var outputMap =  mutable.LinkedHashMap[String, String]()
    if(outputFields.length == 0 && targetFields.length == 0)
      return  outputMap
    else if(outputFields.length == 0 && targetFields.length != 0){
      for(target <- targetFields)
        outputMap += (target._1 -> target._2)
      return outputMap
    }
    else if(outputFields.length != 0 && targetFields.length == 0){
      for(output <- outputFields)
        outputMap += (output._1 -> output._2)
      return outputMap
    }
    else {
      for(output <- outputFields)
        outputMap += (output._1 -> output._2)
      for(target <- targetFields) {
        if(!outputMap.contains(target._1))
          outputMap += (target._1 -> target._2)
      }
      return outputMap
    }

  }
}

private object SimpleValidationEventHandler extends ValidationEventHandler {
  /**
    * Answer false whenever the validation event severity is ERROR or FATAL_ERROR.
    *
    * @param event a ValidationEvent issued by the JAXB SAX utility that is parsing the PMML source text.
    * @return flag to indicate whether to continue with the parse or not.
    */
  def handleEvent(event: ValidationEvent): Boolean = {
    val severity: Int = event.getSeverity
    severity match {
      case ValidationEvent.ERROR => false
      case ValidationEvent.FATAL_ERROR => false
      case _ => true
    }
  }
}
