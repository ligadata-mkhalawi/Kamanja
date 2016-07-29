package com.ligadata.tools.HttpEndpoint

import java.io.ByteArrayOutputStream
import org.apache.logging.log4j.{ Logger, LogManager }
import org.apache.avro.{ Schema }
import org.apache.avro.io.{ Encoder, EncoderFactory}
import org.apache.avro.generic.{ GenericData, GenericDatumWriter, GenericRecord }
import org.joda.time.format.DateTimeFormatter

case class FormatException(code: Int, msg: String, cause: Throwable = null) extends Exception("Format error: " + msg, cause)
case class DataException(code: Int, msg: String, cause: Throwable = null) extends Exception("Data error: " + msg, cause)
case class ServerException(code: Int, msg: String, cause: Throwable = null) extends Exception("Server error: "+ msg, cause)

class MessageProcessor(service: Service) {
  lazy val log = LogManager.getLogger(this.getClass.getName)

  def process(format: String, message: String): Int = {
    val (keys, values) = transform(format, message) 
     
     val publisher = KafkaPublisher.get(service.name)
     publisher.send(keys, values)
     
     values.size
  }

  def transform(format: String, message: String): (Array[String], Array[Array[Byte]]) = {
    val msgs = message.split(service.message.recordSeparator)
    log.debug("Found " + msgs.size + " messages in the payload")
    val keys = new Array[String](msgs.size) 
    val values = new Array[Array[Byte]](msgs.size) 
    var i = 0
    msgs.foreach ( m => {
      val data = deserialize(format, m)
      if (validate(data)) {
        keys(i) = getPartitionKey(data)
        values(i) = serialize(data)
      }
      i += 1
    })
    (keys, values) 
  }

  private def deserialize(format: String, message: String): Map[String, String] = {
    if (format == null || format.equalsIgnoreCase("keyvalue"))
      return deserializeKeyValue(message)
    else if (format.equalsIgnoreCase("delimited"))
      return deserializeDelimited(message)
    else
      throw new FormatException(1, "Unsupported message format " + format)
  }

  private def deserializeDelimited(message: String): Map[String, String] = {
    if (message == null || message.isEmpty) {
      log.error("Error parsing delimited message. Message should not be null or empty for " + service.name)
      throw new FormatException(2, "Message should not be empty for " + service.name)
    }
    log.debug("Processing delimited message with [" + service.message.fieldSeparator + "] as separator")

    val fields = message.split(service.message.fieldSeparator, -1)
    log.debug("Found " + fields.size + " fields in the message")

    if (fields.isEmpty) {
      log.error("Error parsing delimited message. Message should not be empty for " + service.name)
      throw new FormatException(2, "Message should not be empty for " + service.name)
    }
    
    if (fields.length != service.schema.length) {
      log.error("Error parsing delimited message. Message should have " + service.name.length + " fields for " + service.name + " but found " + fields.length)
      throw new FormatException(3, "Message should have " + service.name.length + " fields for " + service.name)
    }

    val record = scala.collection.mutable.Map[String, String]()
    for (i <- 0 to service.schema.length - 1) {
      record(service.schema(i).name) = fields(i)
    }

    record.toMap
  }

  private def deserializeKeyValue(message: String): Map[String, String] = {
    if (message == null || message.isEmpty) {
      log.error("Error parsing keyvalue message. Message should not be null or empty for " + service.name)
      throw new FormatException(2, "Message should not be empty for " + service.name)
    }
    log.debug("Processing keyvalue message with [" + service.message.fieldSeparator + "] as field separator and [" + 
        service.message.keyAndValueSeparator + "] as key and value separator")

    val fields = message.split(service.message.fieldSeparator)
    log.debug("Found " + fields.size + " tokens in the message")

    if (fields.isEmpty) {
      log.error("Error parsing keyvalue message. Message should not be empty for " + service.name)
      throw new FormatException(2, "Format error: Message should not be empty for " + service.name)
    }

    val isSameDelimiter = service.message.fieldSeparator.equalsIgnoreCase(service.message.keyAndValueSeparator)
    if (isSameDelimiter && (fields.length % 2) != 0) {
      log.error("Incorrect format for keyvalue message. Odd number of tokens, could be missing a key, value or separator. Found " + fields.length + "tokens.")
      throw new FormatException(4, "Incorrect format for keyvalue message. Odd number of tokens, could be missing a key, value or separator")
    }

    var fldIdx = 0
    val numFields = fields.length
    val record = scala.collection.mutable.Map[String, String]()

    if (log.isDebugEnabled) {
      log.debug("InputData in fields:" + fields.map(fld => if (fld == null) "<null>" else fld).mkString(","))
    }

    while (fldIdx < numFields) {
      var k = ""
      var v = ""
      if (isSameDelimiter) {
        k = fields(fldIdx)
        v = fields(fldIdx + 1)
        fldIdx += 2
      } else {
        val kv = fields(fldIdx)
        val parts = kv.split(service.message.keyAndValueSeparator, -1)
        if (parts.length != 2) {
          log.error("Incorrect format for keyvalue message. Missing key value separator for field " + (fldIdx+1) + " at ..." + kv + "...")
          throw new FormatException(5, "Incorrect format for keyvalue message. Missing key value separator for field " + (fldIdx+1))
        }
        k = parts(0)
        v = parts(1)
        fldIdx += 1
      }
      record(k) = v
    }

    record.toMap
  }

  private def validate(kvdata: Map[String, String]): Boolean = {
    service.schema.foreach(field => {
      val value: String = kvdata.getOrElse(field.name, "").trim
      if (value.isEmpty) {
        if(field.required)
          throw new DataException(1, "Required field " + field.name + " is missing.")
      } else {
        field.typeId match {
          case 1 => try { value.toDouble }
            catch { case e: Exception => throw new DataException(2, field.name + " should be a number." + ". " + e.getMessage) }
          case 3 => try {
              org.joda.time.format.DateTimeFormat.forPattern(field.format).parseDateTime(value) 
            }
            catch { case e: Exception => throw new DataException(3, field.name + " should be a " + field.datatype + " with format " + field.format + ". " + e.getMessage) }
          case _ =>
        }
      }
    })
    
    true
  }

  private def serialize(kvdata: Map[String, String]): Array[Byte] = {
    service.serializationType match {
      case 1 => return serializeDelimited(kvdata)
      case 2 => return serializeKeyValue(kvdata)
      case 3 => return serializeAvro(kvdata)
      case _ => throw new ServerException(1, "Unsupported serialization type")
    }
  }
  
  private def truncateValue(value: String, index: Int): String = {
    if(index < 0)
      return value
      
    if(service.schema(index).typeId == 2 && service.schema(index).maxlength > 0 && value.length > service.schema(index).maxlength) 
      return value.substring(0, service.schema(index).maxlength)
    else     
      value
  }

  private def serializeDelimited(kvdata: Map[String, String]): Array[Byte] = {
    log.debug("Serializing message to delimited format.")
    val sb = new StringBuilder()
    sb.append(truncateValue(kvdata.getOrElse(service.schema(0).name, ""), 0))
    for (i <- 1 to service.schema.length - 1) {
      sb.append(service.message.fieldSeparator)
      sb.append(truncateValue(kvdata.getOrElse(service.schema(i).name, ""), i))
    }

    sb.toString.getBytes
  }

  private def serializeKeyValue(kvdata: Map[String, String]): Array[Byte] = {
    log.debug("Serializing message to keyvalue format.")
    
    val sb = new StringBuilder()
    if (service.message.allowFieldsNotInSchema) {
      var i: Int = 0;
      for ((key, value) <- kvdata) {
        if (i > 0)
          sb.append(service.message.fieldSeparator)

        val j = service.schema.indexWhere(f => f.name.equals(key))
        sb.append(key).append(service.message.keyAndValueSeparator).append(truncateValue(value, j))

        i += 1
      }
    } else {
      sb.append(service.schema(0).name).append(service.message.keyAndValueSeparator)
        .append(truncateValue(kvdata.getOrElse(service.schema(0).name, ""), 0))

      for (i <- 1 to service.schema.length - 1) {
        sb.append(service.message.fieldSeparator)
        sb.append(service.schema(i).name).append(service.message.keyAndValueSeparator)
          .append(truncateValue(kvdata.getOrElse(service.schema(i).name, ""), i))
      }
    }

    sb.toString.getBytes
  }

  private def serializeAvro(kvdata: Map[String, String]): Array[Byte] = {
    log.debug("Serializing message to Avro format.")
    
    val datum: GenericRecord = new GenericData.Record(service.avroSchema)
    for (i <- 0 to service.schema.length - 1) {
      val f = service.schema(i)
      if(f.typeId == 1) 
        datum.put(f.name, kvdata(f.name).toDouble) 
      else 
        datum.put(f.name, truncateValue(kvdata.getOrElse(f.name, ""), i))
    }

    val writer = new GenericDatumWriter[GenericRecord](service.avroSchema);
    val os: ByteArrayOutputStream = new ByteArrayOutputStream();
    try {
      val e: Encoder = EncoderFactory.get().binaryEncoder(os, null);
      writer.write(datum, e);
      e.flush();
      return os.toByteArray()
    } finally {
      os.close();
    }
  }
  
  private def getPartitionKey(record: Map[String, String]): String = {
    val key = new StringBuffer()
    service.partitionKeys.foreach( k => key.append(record(k)) )
    key.toString
  }
}