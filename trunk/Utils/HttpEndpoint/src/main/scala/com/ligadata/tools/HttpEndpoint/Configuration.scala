package com.ligadata.tools.HttpEndpoint

import java.io.File
import org.json4s._
import org.json4s.native.JsonMethods._
import org.apache.commons.io.FileUtils
import org.apache.avro.{Schema, SchemaBuilder}

case class Configuration(host: String, port: Int, services: Map[String, Service])
case class Service(var schema: List[Field], message: Message, output: Map[String, String], kafkaConfig: Map[String, String]) {
  var name: String = _
  var serializationType: Int = _
  var avroSchema: Schema = _
  output("outputFormat") match {
    case s if s matches "(?i)delimited" => serializationType = 1
    case s if s matches "(?i)keyvalue" => serializationType = 2
    case s if s matches "(?i)avro" => serializationType = 3
    case _ => throw new Exception("outputFormat should be one of delimited, keyvalue or avro.")
  }
  schema = schema.sortWith(_.id < _.id)
  val partitionKeys = output.getOrElse("partitionKey", "").split(",")
  if(partitionKeys.length == 0)
    throw new Exception("partitionKey should be specified for " + name)  
  
  def this(schema: List[Field], message: Message, output: Map[String, String]) = this(schema, message, output, null)
}
case class Field(id: Int, name: String, datatype: String, required: Boolean, maxlength: Int, format: String) {
  var typeId: Int = _
  datatype match {
    case s if s matches "(?i)number" => typeId = 1
    case s if s matches "(?i)string" => typeId = 2
    case s if s matches "(?i)date" => { 
      typeId = 3
      if(format.isEmpty) throw new Exception("Format needed for date field " + name)
    }
    case s if s matches "(?i)datetime" => { 
      typeId = 3
      if(format.isEmpty) throw new Exception("Format needed for datetime field " + name)
    }
    case _ => throw new Exception("Unsupported data type " + datatype + " for field " + name)
  }
  def this(id: Int, name: String, datatype: String, required: Boolean) = this(id, name, datatype, required, 0, "")
  def this(id: Int, name: String, datatype: String, required: Boolean, maxlength: Int) = this(id, name, datatype, required, maxlength, "")
  def this(id: Int, name: String, datatype: String, required: Boolean, format: String) = this(id, name, datatype, required, 0, format)
}
case class Message(keyAndValueSeparator: String, fieldSeparator: String, recordSeparator: String, allowFieldsNotInSchema: Boolean)

object Configuration {

  var configFilename: String = _
  var values: Configuration = _

  def load(fileName: String) {
    implicit val formats = DefaultFormats
    configFilename = fileName
    val json = FileUtils.readFileToString(new File(configFilename))
    values = parse(json).extract[Configuration]
    
    if(values.services.isEmpty)
      throw new Exception("Atleast one service should be specified.")

    for ((name, svc) <- values.services) {
      svc.name = name
      if (svc.schema.size == 0)
        throw new Exception("Atleast one field should be specified in schema for " + name)
      if (svc.message.keyAndValueSeparator.isEmpty)
        throw new Exception("keyAndValueSeparator should be specified for " + name)
      if (svc.message.fieldSeparator.isEmpty)
        throw new Exception("fieldSeparator should be specified for " + name)
      if (svc.message.recordSeparator.isEmpty)
        throw new Exception("recordSeparator should be specified for " + name)
      if (svc.output.getOrElse("kafkaHost", "").isEmpty)
        throw new Exception("kafkaHost should be specified for " + name)
      if (svc.output.getOrElse("kafkaTopic", "").isEmpty)
        throw new Exception("kafkaTopic should be specified for " + name)
      if (svc.output.getOrElse("partitionKey", "").isEmpty)
        throw new Exception("partitionKey should be specified for " + name)
      if (svc.output.getOrElse("outputFormat", "").isEmpty)
        throw new Exception("outputFormat should be specified for " + name)

      //if(svc.serializationType == 3) {
      var builder = SchemaBuilder.record(name).fields()
      svc.schema.foreach(f => {
        builder =
          if (f.typeId == 1)
            if (f.required) builder.requiredDouble(f.name) else builder.optionalDouble(f.name)
          else if (f.required) builder.requiredString(f.name) else builder.optionalString(f.name)
      })

      svc.avroSchema = builder.endRecord()
      //}
    }

  
  }
}