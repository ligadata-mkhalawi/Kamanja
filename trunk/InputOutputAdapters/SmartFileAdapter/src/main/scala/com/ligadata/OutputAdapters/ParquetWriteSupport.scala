package com.ligadata.OutputAdapters

import java.util.HashMap

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.logging.log4j.LogManager
import parquet.column.ColumnDescriptor
import parquet.hadoop.api.WriteSupport
import parquet.hadoop.api.WriteSupport.WriteContext
import parquet.io.ParquetEncodingException
import parquet.io.api._
import parquet.schema.MessageType
import parquet.schema.PrimitiveType.PrimitiveTypeName

class ParquetWriteSupport extends WriteSupport[Array[Any]] {
  private[this] val logger = LogManager.getLogger(getClass);

  val nullFlagsFieldName = "kamanja_system_null_flags"
  val systemFields = Set(nullFlagsFieldName)

  var schema: MessageType = null
  var recordConsumer: RecordConsumer = null
  var cols: Array[ColumnDescriptor] = null

  private var nullFlagsFieldIdx = -1

  private val writeLock = new Object

  def this(schema: MessageType) {
    this()
    this.schema = schema
    if (schema.getColumns == null)
      throw new Exception("columns in schema are null")


    this.cols = new Array[ColumnDescriptor](schema.getColumns.size)
    schema.getColumns.toArray[parquet.column.ColumnDescriptor](this.cols)

    this.cols.foldLeft(0)((counter, cd) => {
      if(cd.getPath()(0).equalsIgnoreCase(nullFlagsFieldName))
        nullFlagsFieldIdx = counter
      counter + 1
    })
  }

  @Override
  def init(config: Configuration): WriteContext = {
    new WriteContext(schema, new HashMap[String, String]())
  }

  @Override
  def prepareForWrite(r: RecordConsumer): Unit = {
    recordConsumer = r
  }

  @Override
  def write(values: Array[Any]): Unit = {
    writeLock.synchronized {

      if (values.length != cols.length) {
        throw new ParquetEncodingException("Invalid input data. Expecting " +
          cols.length + " columns. Input had " + values.length + " columns (" + cols + ") : " + values);
      }

      val nullFlags =
        if (nullFlagsFieldIdx >= 0) {
          val nullFlagsAny = values(nullFlagsFieldIdx)
          try {
            if (nullFlagsAny == null) null
            else nullFlagsAny.asInstanceOf[Array[Boolean]]
          }
          catch {
            case ex: Exception => null
          }
        }
        else null

      recordConsumer.startMessage()
      for (i <- 0 to cols.length - 1) {

        val colName = cols(i).getPath()(0)
        logger.debug("parquet serializing. col name: %s, value: %s".format(colName, (if (values(i) == null) null else values(i).toString)))
        if (!systemFields.contains(colName.toLowerCase)) {

          val colName = cols(i).getPath()(0)
          val value = values(i)
          // val.length() == 0 indicates a NULL value.

          if (value != null) {
            if ((nullFlags == null || nullFlags.length <= i || !nullFlags(i))) {
              recordConsumer.startField(colName, i)

              if (cols(i).getPath().length == 1)
                addSimpleValue(recordConsumer, value, cols(i).getType())
              else if (cols(i).getPath()(1) == "array")
                addArrayValue(recordConsumer, value, cols(i).getType(), cols(i).getPath())
              else
                throw new ParquetEncodingException("Unsupported complex column type: " + cols(i).getPath()(1))

              recordConsumer.endField(cols(i).getPath()(0), i)
            }
          }
        }
      }
      recordConsumer.endMessage()
    }
  }

  private def stringToBinary(value: Any): Binary = {
    Binary.fromString(value.toString)
  }

  private def addSimpleValue(recordConsumer : RecordConsumer, value : Any, typeName : PrimitiveTypeName){
    typeName match {
      case PrimitiveTypeName.BOOLEAN =>
        recordConsumer.addBoolean(value.asInstanceOf[Boolean])

      case PrimitiveTypeName.FLOAT =>
        recordConsumer.addFloat(value.asInstanceOf[Float])

      case PrimitiveTypeName.DOUBLE =>
        recordConsumer.addDouble(value.asInstanceOf[Double])

      case PrimitiveTypeName.INT32 =>
        recordConsumer.addInteger(value.asInstanceOf[Int])

      case PrimitiveTypeName.INT64 =>
        recordConsumer.addLong(value.asInstanceOf[Long])

      case PrimitiveTypeName.BINARY =>
        recordConsumer.addBinary(stringToBinary(value))
      case _ =>
        throw new ParquetEncodingException("Unsupported column type: " + typeName)
    }
  }

  private def addArrayValue(recordConsumer : RecordConsumer, value : Any, typeName : PrimitiveTypeName, path : Array[String]){
    typeName match {
      case PrimitiveTypeName.BOOLEAN =>
        val valueAr = value.asInstanceOf[Array[Boolean]]
        if(valueAr != null){
          recordConsumer.startGroup();
          recordConsumer.startField(path(1), 0)
          valueAr.foreach(v => if(v!=null) recordConsumer.addBoolean(v))
          recordConsumer.endField(path(1), 0)
          recordConsumer.endGroup()
        }

      case PrimitiveTypeName.FLOAT =>
        val valueAr = value.asInstanceOf[Array[Float]]
        if(valueAr != null){
          recordConsumer.startGroup();
          recordConsumer.startField(path(1), 0)
          valueAr.foreach(v => if(v!=null) recordConsumer.addFloat(v))
          recordConsumer.endField(path(1), 0)
          recordConsumer.endGroup()
        }


      case PrimitiveTypeName.DOUBLE =>
        val valueAr = value.asInstanceOf[Array[Double]]
        if(valueAr != null){
          recordConsumer.startGroup();
          recordConsumer.startField(path(1), 0)
          valueAr.foreach(v => if(v!=null) recordConsumer.addDouble(v))
          recordConsumer.endField(path(1), 0)
          recordConsumer.endGroup()
        }


      case PrimitiveTypeName.INT32 =>
        val valueAr = value.asInstanceOf[Array[Int]]
        if(valueAr != null){
          recordConsumer.startGroup();
          recordConsumer.startField(path(1), 0)
          valueAr.foreach(v => if(v!=null) recordConsumer.addInteger(v))
          recordConsumer.endField(path(1), 0)
          recordConsumer.endGroup()
        }

      case PrimitiveTypeName.INT64 =>
        val valueAr = value.asInstanceOf[Array[Long]]
        if(valueAr != null){
          recordConsumer.startGroup();
          recordConsumer.startField(path(1), 0)
          valueAr.foreach(v => if(v!=null) recordConsumer.addLong(v))
          recordConsumer.endField(path(1), 0)
          recordConsumer.endGroup()
        }

      case PrimitiveTypeName.BINARY =>
        val valueAr = value.asInstanceOf[Array[String]]
        if(valueAr != null){
          recordConsumer.startGroup();
          recordConsumer.startField(path(1), 0)
          valueAr.foreach(v => if(v!=null) recordConsumer.addBinary(stringToBinary(v)))
          recordConsumer.endField(path(1), 0)
          recordConsumer.endGroup()
        }
      case _ =>
        throw new ParquetEncodingException("Unsupported column type: " + typeName)
    }
  }
}
