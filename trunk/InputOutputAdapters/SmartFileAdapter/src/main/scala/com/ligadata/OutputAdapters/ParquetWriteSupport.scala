package com.ligadata.OutputAdapters

import java.util.HashMap

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import parquet.column.ColumnDescriptor
import parquet.hadoop.api.WriteSupport
import parquet.hadoop.api.WriteSupport.WriteContext
import parquet.io.ParquetEncodingException
import parquet.io.api._
import parquet.schema.MessageType
import parquet.schema.PrimitiveType.PrimitiveTypeName

class ParquetWriteSupport extends WriteSupport[Array[Any]] {
  var schema: MessageType = null
  var recordConsumer: RecordConsumer = null
  var cols: Array[ColumnDescriptor] = null

  // TODO: support specifying encodings and compression
  def this(schema: MessageType) {
    this()
    this.schema = schema
    if (schema.getColumns == null)
      throw new Exception("columns in schema are null")


    this.cols = new Array[ColumnDescriptor](schema.getColumns.size)
    schema.getColumns.toArray[parquet.column.ColumnDescriptor](this.cols)
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

    if (values.length != cols.length) {
      throw new ParquetEncodingException("Invalid input data. Expecting " +
        cols.length + " columns. Input had " + values.length + " columns (" + cols + ") : " + values);
    }

    recordConsumer.startMessage()
    for (i <- 0 to cols.length - 1) {

      val value = values(i);
      // val.length() == 0 indicates a NULL value.
      if (value != null) {
        recordConsumer.startField(cols(i).getPath()(0), i)
        cols(i).getType() match {
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
            throw new ParquetEncodingException("Unsupported column type: " + cols(i).getType)
        }

        recordConsumer.endField(cols(i).getPath()(0), i)
      }
    }
    recordConsumer.endMessage()
  }

  //TODO : consider complex types: need to convert into strings first
  private def stringToBinary(value: Any): Binary = {
    Binary.fromString(value.toString)
  }
}
