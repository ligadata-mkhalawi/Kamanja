package com.ligadata.kamanja.serializer

import scala.collection.mutable.Map
import scala.collection.JavaConverters._
import java.io.{ByteArrayOutputStream, DataOutputStream, StringReader}
import scala.collection.JavaConverters._
import org.apache.commons.csv.CSVFormat

import org.apache.logging.log4j.LogManager
// import org.apache.commons.lang.StringEscapeUtils

import com.ligadata.Exceptions._
import com.ligadata.KamanjaBase._
import com.ligadata.KamanjaBase.AttributeTypeInfo.TypeCategory._

/**
  * Meta fields found at the beginning of each JSON representation of a ContainerInterface object
  */
//@@TODO: move this into utils and use for all logging
object CSVLog {
    private val log = LogManager.getLogger(getClass)

    def Trace(str: String) = if(log.isTraceEnabled())  log.trace(str)
    def Warning(str: String) = if(log.isWarnEnabled()) log.warn(str)
    def Info(str: String) = if(log.isInfoEnabled())    log.info(str)
    def Error(str: String) = if(log.isErrorEnabled())  log.error(str)
    def Debug(str: String) = if(log.isDebugEnabled())  log.debug(str)

    def Trace(str: String, e: Throwable) = if(log.isTraceEnabled())  log.trace(str, e)
    def Warning(str: String, e: Throwable) = if(log.isWarnEnabled()) log.warn(str, e)
    def Info(str: String, e: Throwable) = if(log.isInfoEnabled())    log.info(str, e)
    def Error(str: String, e: Throwable) = if(log.isErrorEnabled())  log.error(str, e)
    def Debug(str: String, e: Throwable) = if(log.isDebugEnabled())  log.debug(str, e)

    def Trace(e: Throwable) = if(log.isTraceEnabled())  log.trace("", e)
    def Warning(e: Throwable) = if(log.isWarnEnabled()) log.warn("", e)
    def Info(e: Throwable) = if(log.isInfoEnabled())    log.info("", e)
    def Error(e: Throwable) = if(log.isErrorEnabled())  log.error("", e)
    def Debug(e: Throwable) = if(log.isDebugEnabled())  log.debug("", e)

    def isTraceEnabled = log.isTraceEnabled()
    def isWarnEnabled = log.isWarnEnabled()
    def isInfoEnabled = log.isInfoEnabled()
    def isErrorEnabled = log.isErrorEnabled()
    def isDebugEnabled = log.isDebugEnabled()
}

import CSVLog._
/**
  * CsvSerDeser instance can serialize a ContainerInterface to a byte array and deserialize a byte array to form
  * an instance of the ContainerInterface encoded in its bytes.
  *
  * Pre-condition: The JSONSerDes must be initialized with the metadata manager, object resolver and class loader
  * before it can be used.
  *
  * Pre-condition: The configuration object is an important part of the behavior of the CsvSerDeser.  It must have values for
  * "fieldDelimiter" (e.g., ','), "alwaysQuoteField" (e.g., false), and "lineDelimiter" (e.g., "\r\n")
  *
  * Csv also supports emitting a "header" record.  To generate one, use the emitHeaderOnce method just before calling
  * the serialize(container) method.  The behavior is to emit the header and then immediately turn the state off.  That is, the
  * behavior is "one-shot".
  */

class CsvSerDeser extends SerializeDeserialize {

    var _objResolver : ObjectResolver = null
    var _isReady : Boolean = false
    var _config = Map[String,String]()
    var _emitHeaderFirst : Boolean = false
    var _fieldDelimiter  = ","
    // add by saleh 24/1/2017
    var _fieldDelimiterAsChar  = ','
    var _valDelimiter = "~"
    var _keyDelimiter = "@"
    var _lineDelimiter = "\n"
    var _nullValue = ""
    var _alwaysQuoteField = false
    var _escapeChar = "\\"
    // add by saleh 24/1/2017
    var _csvParser  = false

    val _nullFlagsFieldName = "kamanja_system_null_flags"

    /**
      * Serialize the supplied container to a byte array using these CSV rules:
      *
      * @param v a ContainerInterface (describes a standard kamanja container)
      */
    @throws(classOf[com.ligadata.Exceptions.ObjectNotFoundException])
    override def serialize(v : ContainerInterface) : Array[Byte] = {
        val sb = new StringBuilder(8 * 1024)

        val containerName : String = v.getFullTypeName
        val containerType = v.getContainerType

        /** The Csv implementation of the SerializeDeserialize interface will not support the mapped message type.  Instead there will be another
          * implementation that supports the Kamanja Variable Comma Separated Value (VCSV) format.  That one deals with sparse data as does the
          * JSON implementation.  Either of those should be chosen
          */
        if (! v.isFixed) {
            throw new UnsupportedObjectException(s"CsvSerDeser::serialize, type name $containerName is a mapped message container type... Csv emcodings of mapped messages are not currently supported...choose JSON or (when available) Kamanja VCSV serialize/deserialize... deserialize fails.",null)
        }

        /* The check for empty container should be done at adapter binding level rather than here.
           For now, keep it here for debugging purpose, but needs to be removed as it is too low level to have this check and fail.
         */
        val fields = v.getAllAttributeValues
        if (fields.isEmpty) {
            throw new ObjectNotFoundException(s"CsvSerDeser::serialize, The container $containerName surprisingly has no fields...serialize fails", null)
        }


        //check if message has a field (kamanja_system_null_flags), any field whose corresponding flag value is true
        //means it is null not 0
        val nullFlagsAny = v.getOrElse(_nullFlagsFieldName, null)
        val nullsFlagExists = nullFlagsAny != null
        val nullFlags =
            try {
                if (nullFlagsAny == null) null
                else nullFlagsAny.asInstanceOf[Array[Boolean]]
            }
            catch{
                case ex : Exception => null
            }

        Debug("nullFlags array="+ (if(nullFlags == null) "null" else nullFlags.map(f => f.toString).mkString("~") ))

        var processCnt : Int = 0
        val fieldCnt = fields.size
        //actual count: is field count after removing null flags field
        val actualFieldCnt = if(nullsFlagExists) fieldCnt - 1 else fieldCnt
        val emitKeyName = false
        var idx = 0
        fields.foreach(attr => {
            processCnt += 1
            val fld = attr.getValue
            val fldName = attr.getValueType.getName
            if(!fldName.equalsIgnoreCase(_nullFlagsFieldName)) {
                val nullFlag = if(nullFlags == null || nullFlags.length <= idx ) false else nullFlags(idx)
                if (fld != null) {
                    val doTrailingComma: Boolean = processCnt < actualFieldCnt
                    emitField(sb, attr, doTrailingComma, emitKeyName, nullFlag)
                } else {
                    // right thing to do is to emit NULL as special value - either as empty in output or some special indication such as -
                    throw new ObjectNotFoundException(s"CsvSerDeser::serialize, during serialize()...attribute ${attr.getValueType.getName} could not be found in the container... mismatch", null)
                }
            }

            idx += 1
        })
        val strRep = sb.toString()
        if (isDebugEnabled) {
            Debug(s"Serialized as CSV, #flds: $fieldCnt, data: $strRep")
        }

        Debug(s"Serialized as CSV, #flds: $actualFieldCnt , data: $strRep" +
          (if(nullsFlagExists) " . after ignoring null flags field - " + _nullFlagsFieldName else ""))
        strRep.getBytes
    }

    /**
      * Write the field data type names to the supplied stream.  This is called whenever the _emitHeaderFirst instance
      * variable is true... usually just once in a given stream creation.
      *
      * @param sb string builder to emit header
      * @param containerFieldsInOrder array of field definitions
      */
    def emitHeaderRecord(sb : StringBuilder, containerFieldsInOrder : Array[AttributeValue]): Unit = {
        val quote : String = s"${'"'}"
        val fieldCnt : Int = containerFieldsInOrder.length
        var cnt : Int = 0

        containerFieldsInOrder.foreach(av => {
            cnt += 1
            val delim : String = if (cnt < fieldCnt) _fieldDelimiter else ""
            val value : String = s"$quote${av.getValueType.getName}$quote"
            sb.append(s"$value$delim")
        })
    }

    /**
      * Emit the supplied attribute's data value, possibly decorating it with commas or newline, escaped quotes, etc.
      *
      * CSV rules supported:
      * 1) CSV is a delimited data format that has fields/columns separated by the comma character and records/rows terminated by newlines.
      * 2) A CSV file does not require a specific character encoding, byte order, or line terminator format (some software does not support all line-end variations).
      * 3) A record ends at a line terminator. However, line-terminators can be embedded as data within fields.  For fields with embedded line terminators
      * the field will be automatically enclosed in double quotes
      * 4) All records should have the same number of fields, in the same order.
      * 5) Numeric quantities are expressed in decimal.  Perhaps we will support other text representations (e.g., hex) at a later time.
      * 6) Field delimiters can be any char but usually {,;:\t}
      * 7) A field with embedded field delimiters must be enclosed in double quotes.
      * 8) The first record may be a "header", which contains column names in each of the fields. A header record's presence is
      * specified in the CSV serializer metadata. Once a file or stream has been produced with the CSV serializer, there is no
      * identifying markings in the file that there is a header.  Apriori, one most know.
      * 9) Fields with embedded double quote marks are quoted and each occurrence of a double quote mark is escaped with a double quote mark.
      * For example, a field value
      *     This is not a "joke"
      * will be encoded as
      *     "This is not a ""joke"""
      * 10) A CSV serializer may be configured to always quote a field.  If not so configured, only fields that require them (embedded new
      * lines, quote marks)
      * 11) Record (line) delimiters can be configured.  By default, the delimiter is the MS-DOS delimiter (\r\n).  The typical *nix line
      * (\n) is also supported.
      *
      * @param sb the data output stream to receive the emissions of the decorated value
      * @param attr the attribute value that contains the data value
      * @param doTrailingComma when true follow data emission with comma; when false, emit the line delimiter configured
      */
    def emitField( sb: StringBuilder
                   ,attr : com.ligadata.KamanjaBase.AttributeValue
                   ,doTrailingComma : Boolean, emitKeyName: Boolean, nullFlag : Boolean = false) = {
        val valueType  = attr.getValueType
        val rawValue : Any = if(nullFlag) _nullValue else attr.getValue
        val typeName : String = valueType.getName
        Debug(s"emit field $typeName with original value = ${rawValue.toString}")

        var fld = _nullValue
        if(rawValue != null) {
            fld = valueType.getTypeCategory match {
                case (BOOLEAN | BYTE | LONG | DOUBLE | FLOAT | INT) => rawValue.toString
                case STRING => escapeQuotes(rawValue.asInstanceOf[String])
                case ARRAY => { emitArray(sb, valueType, rawValue.asInstanceOf[Array[_]]); "" }
                case (MAP | CONTAINER | MESSAGE ) => throw new NotImplementedFunctionException(s"emitField: complex type: ${valueType.getKeyTypeCategory.getValue}, fldName: ${valueType.getName}, not supported in standard csv format", null);
                case _ => throw new ObjectNotFoundException(s"emitField: invalid value type: ${valueType.getKeyTypeCategory.getValue}, fldName: ${valueType.getName} could not be resolved", null)
            }
            Debug(s"CsvSerDeser::emitField $typeName with value possibly quoted and escaped = $fld")
        }
        if(emitKeyName) {
            sb.append(typeName)
            sb.append(_keyDelimiter)
        }
        sb.append(fld)
        if(doTrailingComma)
            sb.append(_fieldDelimiter)
    }

    def emitArray(sb: StringBuilder, valueType: AttributeTypeInfo, array: Array[_]) = {
        var idx = 0
        if (array != null && array.size > 0) {
            val itemType = valueType.getValTypeCategory
            array.foreach(itm => {
                if (idx > 0) sb.append(_valDelimiter)
                idx += 1
                val itemStr = itemType match {
                    case (BOOLEAN | BYTE | LONG | DOUBLE | FLOAT | INT | CHAR) => if(itm != null) itm.toString else ""
                    case STRING => escapeQuotes(itm.asInstanceOf[String])
                    case _ => throw new UnsupportedObjectException("CsvSerDeser::emitArray - Not yet handled itemType: " + itemType.getValue, null)
                }
                sb.append(itemStr)
            })
        }
    }

    /**
      * Escape all double quotes found in this string by prefixing the double quote with another double quote.  If
      * none are found, the original string is returned.
      *
      * @param valueStr string presumably that has embedded double quotes.
      * @return adjusted string
      */
    def escapeQuotes(valueStr : String) : String = {
        val len : Int = if (valueStr != null) valueStr.length else 0
        if(len == 0) return ""
        val buffer : StringBuilder = new StringBuilder
        val fieldDelimiter = _fieldDelimiter(0)
        val escapeChar = _escapeChar(0)
        val valueDelimiter = _valDelimiter(0)

        valueStr.foreach(ch => {
            val esc = ch match {
                case `escapeChar` => _escapeChar
                case `fieldDelimiter` => _escapeChar
//              case `valueDelimiter` => _escapeChar
                case '"' => _escapeChar
                case '\\' => _escapeChar
                case _ => ""
            }
            buffer.append(esc)
            buffer.append(ch)
        })
        buffer.toString
    }

    /**
      * Set the object resolver to be used for this serializer
      *
      * @param objRes an ObjectResolver
      */
    override def setObjectResolver(objRes : ObjectResolver) : Unit = {
        _objResolver = objRes
    }

    /**
      * Configure the SerializeDeserialize adapter.  This must be done before the adapter implementation can be used.
      *
      * @param objResolver the ObjectResolver instance that can instantiate ContainerInterface instances
      * @param configProperties a map of options that might be used to configure the execution of the CsvSerDeser instance.
      */
    override def configure(objResolver: ObjectResolver
                           , configProperties : java.util.Map[String,String]): Unit = {
        _objResolver = objResolver
        _config = configProperties.asScala
        _fieldDelimiter = _config.getOrElse("fieldDelimiter", ",")
        //added by saleh 24/1/2017
        _fieldDelimiterAsChar = _fieldDelimiter.charAt(0)
        _valDelimiter = _config.getOrElse("valDelimiter", "~")
        _keyDelimiter = _config.getOrElse("keyDelimiter", "@")
        _lineDelimiter =  _config.getOrElse("lineDelimiter", "\n")
        _nullValue =  _config.getOrElse("nullValue", "")
        _escapeChar = _config.getOrElse("escChar", "\\")
        //added by saleh 24/1/2017
        _csvParser = _config.getOrElse("csvParser","false").toBoolean
        val alwaysQuoteFieldStr = _config.getOrElse("alwaysQuoteField", "F")
        _alwaysQuoteField = alwaysQuoteFieldStr.toLowerCase.startsWith("t")

        _isReady = (_objResolver != null && _config != null)
    }


    def deserializeArray(fldStr: String, attr: AttributeTypeInfo): Any = {
        val rawValFields : Array[String] = if (fldStr != null) {
            fldStr.split(_valDelimiter, -1)
        } else {
            Array[String]()
        }
        val itemType = attr.getValTypeCategory
        var retVal: Any = Array[Any]()

        val fld = itemType match {
            case LONG =>    retVal = rawValFields.map(itm => if(itm != null && itm.length > 0) itm.toLong else 0.toLong).toArray
            case INT =>     retVal = rawValFields.map(itm => if(itm != null && itm.length > 0) itm.toInt else 0).toArray
            case BYTE =>    retVal = rawValFields.map(itm => if(itm != null && itm.length > 0) itm.toByte else 0.toByte).toArray
            case BOOLEAN => retVal = rawValFields.map(itm => if(itm != null && itm.length > 0) itm.toBoolean else false).toArray
            case DOUBLE =>  retVal = rawValFields.map(itm => if(itm != null && itm.length > 0) itm.toDouble else 0.toDouble).toArray
            case FLOAT =>   retVal = rawValFields.map(itm => if(itm != null && itm.length > 0) itm.toFloat else 0.toFloat).toArray
            case STRING =>  retVal = rawValFields
            case CHAR =>    retVal = rawValFields.map(itm => if (itm != null && itm.length > 0) itm.charAt(0) else ' ').toArray
            case _ => throw new ObjectNotFoundException(s"CsvSerDeser::deserializeArray, invalid value type: ${itemType.getValue}, fldName: ${itemType.name} could not be resolved", null)
        }
        retVal
    }

    def resolveValue(fldStr: String, attr: AttributeTypeInfo): Any = {
        val fld = if(fldStr == null) "" else fldStr
        var returnVal: Any = null
        val tc = attr.getTypeCategory

        tc match {
            case CHAR =>   returnVal = if( fld.length > 0) fld(0) else ' '
            case STRING => returnVal = fld
            case ARRAY =>  returnVal = deserializeArray(fldStr, attr)
            case _ => {
                val f1 = fld.trim
                tc match {
                    case INT =>     returnVal = if (f1.length > 0) f1.toInt else 0.toInt
                    case FLOAT =>   returnVal = if (f1.length > 0) f1.toFloat else 0.toFloat
                    case DOUBLE =>  returnVal = if (f1.length > 0) f1.toDouble else 0.toDouble
                    case LONG =>    returnVal = if (f1.length > 0) f1.toLong else 0.toLong
                    case BYTE =>    returnVal = if (f1.length > 0) f1.toByte else 0.toByte
                    case BOOLEAN => returnVal = if (f1.length > 0) f1.toBoolean else false
                    case _ => {
                        // Unhandled type
                        try
                            Error("For CsvSerDeser::resolveValue:%s we did not find valid Category Type in attribute info(Name:%s, Index:%d, getTypeCategory:%s)".format(fld, attr.getName, attr.getIndex, attr.getTypeCategory.toString))
                        catch {
                            case e: Throwable => {
                                Error(e)
                            }
                        }
                    }
                }
            }
        }
        if(isDebugEnabled) {
            val dumpStr = if(returnVal != null) returnVal.toString else "null"
            Debug(s"CsvSerDeser::resolveValue, name: ${attr.getName} value: $dumpStr\n")
        }
        returnVal
    }

    //add by saleh 24/1/2017
    /**
      * This method will use the Apache Parser commons-csv 1.4 this will work only if csvParser option is true
      * csvParser is false by default
      *
      * @param  rawCsvContainerStr the CSV string line
      * @return a Array[String]
      */
    def csvApache(rawCsvContainerStr: String) : Array[String] = {
        val in = new StringReader(rawCsvContainerStr)
        val records = CSVFormat.DEFAULT.withDelimiter(_fieldDelimiterAsChar).parse(in).iterator
        if (records.hasNext) {
            records.next().iterator().asScala.toArray
        }else{
            Array[String]()
        }
    }

    /**
      * Deserialize the supplied byte array into some ContainerInterface instance.  Note that the header
      * record is not handled.  The CSV stream of multiple container interface records will just stumble over
      * such header records when they don't match the
      *
      * @param b the byte array containing the serialized ContainerInterface instance
      * @return a ContainerInterface
      */
    @throws(classOf[com.ligadata.Exceptions.MissingPropertyException])
    @throws(classOf[com.ligadata.Exceptions.ObjectNotFoundException])
    @throws(classOf[com.ligadata.Exceptions.UnsupportedObjectException])
    def deserialize(b: Array[Byte], containerName: String) : ContainerInterface = {

        val rawCsvContainerStr : String = new String(b)

        val rawCsvFields : Array[String] = if (rawCsvContainerStr != null) {
            //add by saleh 24/1/2017
            if(_csvParser){
              csvApache(rawCsvContainerStr)
            }else{
              rawCsvContainerStr.split(_fieldDelimiter, -1)
            }
        } else {
            Array[String]()
        }
        if (rawCsvFields.isEmpty) {
            Error("The supplied CSV record is empty...abandoning processing")
            throw new ObjectNotFoundException("The supplied CSV record is empty...abandoning processing", null)
        }
        /** get an empty ContainerInterface instance for this type name from the _objResolver */
        val ci : ContainerInterface = _objResolver.getInstance(containerName)
        if (ci == null) {
            throw new ObjectNotFoundException(s"type name $containerName could not be resolved and built for deserialize",null)
        }
        val containerType = ci.getContainerType
        /** get the fields information */
        if (containerType == null) {
            throw new ObjectNotFoundException(s"type name $containerName is not a container type... deserialize fails.",null)
        }
        if (! ci.isFixed) {
            throw new UnsupportedObjectException(s"type name $containerName has a mapped message container type...these are not supported in CSV... use either JSON or VCSV (when available) instead... deserialize fails.",null)
        }

        val fieldsToConsider = ci.getAttributeTypes
        if (fieldsToConsider.isEmpty) {
            throw new ObjectNotFoundException(s"The container $containerName surprisingly has no fields...deserialize fails", null)
        }
        var fldIdx = 0
        val numFields = rawCsvFields.length

        if (isDebugEnabled) {
            Debug("InputData in fields:" + rawCsvFields.map(fld => if (fld == null) "<null>" else fld).mkString(","))
        }

        fieldsToConsider.foreach(attr => {
            if (attr.IsContainer) {
                Error(s"field type name ${attr.getName} is a container type... containers are not supported by the CSV deserializer at this time... deserialization fails.")
                throw new UnsupportedObjectException(s"field type name ${attr.getName} is a container type... containers are not supported by the CSV deserializer at this time... deserialization fails.",null)
            }
            /** currently assumed to be one of the scalars or simple types supported by json/avro **/
            if(fldIdx >= numFields) {
                Error(s"input contains less number of fields than expected in container - attribute name: ${attr.getName}, fieldIndex: $fldIdx, numFields: $numFields")
                throw new UnsupportedObjectException(s"field type name ${attr.getName} is a container type... containers are not supported by the CSV deserializer at this time... deserialization fails.",null)
            }
            val fld = rawCsvFields(fldIdx)
            // @TODO: need to handle failure condition for set - string is not in expected format?

            ci.set(fldIdx, resolveValue(fld, attr))
            fldIdx += 1
        })
        ci
    }
}

class KVSerDeser extends CsvSerDeser {
    /**
      * Serialize the supplied container to a byte array using these CSV rules:
      *
      * @param v a ContainerInterface (describes a standard kamanja container)
      */
    @throws(classOf[com.ligadata.Exceptions.ObjectNotFoundException])
    override def serialize(v : ContainerInterface) : Array[Byte] = {
        val sb = new StringBuilder(8 * 1024)

        val containerName : String = v.getFullTypeName
        val containerType = v.getContainerType

        /* The check for empty container should be done at adapter binding level rather than here.
           For now, keep it here for debugging purpose, but needs to be removed as it is too low level to have this check and fail.
         */
        val fields = v.getAllAttributeValues
        if (fields.isEmpty) {
            throw new ObjectNotFoundException(s"KVSerDeser::serialize, The container $containerName surprisingly has no fields...serialize fails", null)
        }

        var processCnt : Int = 0
        val fieldCnt = fields.size
        val emitKeyName = true
        fields.foreach(attr => {
            processCnt += 1
            val fld = attr.getValue
            if (fld != null) {
                val doTrailingComma : Boolean = processCnt < fieldCnt
                emitField(sb, attr, doTrailingComma, emitKeyName)
            } else {
                // right thing to do is to emit NULL as special value - either as empty in output or some special indication such as -
                throw new ObjectNotFoundException(s"KVSerDeser::serialize, during serialize()...attribute ${attr.getValueType.getName} could not be found in the container... mismatch", null)
            }
        })
        val strRep = sb.toString()
        if (isDebugEnabled) {
            Debug(s"Serialized as KV, #flds: $fieldCnt, data: $strRep")
        }
        strRep.getBytes
    }

    /**
      * Write the field data type names to the supplied stream.  This is called whenever the _emitHeaderFirst instance
      * variable is true... usually just once in a given stream creation.
      *
      * @param sb string builder to emit header
      * @param containerFieldsInOrder array of field definitions
      */
    override def emitHeaderRecord(sb : StringBuilder, containerFieldsInOrder : Array[AttributeValue]): Unit = { }

    /**
      * Deserialize the supplied byte array into some ContainerInterface instance.  Note that the header
      * record is not handled.  The CSV stream of multiple container interface records will just stumble over
      * such header records when they don't match the
      *
      * @param b the byte array containing the serialized ContainerInterface instance
      * @return a ContainerInterface
      */
    @throws(classOf[com.ligadata.Exceptions.MissingPropertyException])
    @throws(classOf[com.ligadata.Exceptions.ObjectNotFoundException])
    @throws(classOf[com.ligadata.Exceptions.UnsupportedObjectException])
    override def deserialize(b: Array[Byte], containerName: String) : ContainerInterface = {
        /** get an empty ContainerInterface instance for this type name from the _objResolver */
        val ci : ContainerInterface = _objResolver.getInstance(containerName)
        if (ci == null) {
            throw new ObjectNotFoundException(s"type name $containerName could not be resolved and built for deserialize",null)
        }
        val rawCsvContainerStr : String = new String(b)

        val rawCsvFields : Array[String] = if (rawCsvContainerStr != null) {
            rawCsvContainerStr.split(_fieldDelimiter, -1)
        } else {
            Array[String]()
        }
        if (rawCsvFields.isEmpty) {
            Error("The supplied KV record is empty...abandoning processing")
            throw new ObjectNotFoundException("The supplied KV record is empty...abandoning processing", null)
        }
        val isFieldAndKeyDelimSame = (_fieldDelimiter == _keyDelimiter)
        if(isFieldAndKeyDelimSame && (rawCsvFields.length % 2) != 0) {
            Error(s"The supplied KV record has same key and field delimiters and has odd number of parts, record: $rawCsvContainerStr")
            throw new ObjectNotFoundException("The supplied KV record format is not correct...abandoning processing", null)
        }

        var fldIdx = 0
        val numFields = rawCsvFields.length

        if (isDebugEnabled) {
            Debug("InputData in fields:" + rawCsvFields.map(fld => if (fld == null) "<null>" else fld).mkString(","))
        }

        while (fldIdx < numFields){
            var k = ""
            var v = ""
            if(isFieldAndKeyDelimSame) {
                k = rawCsvFields(fldIdx)
                v = rawCsvFields(fldIdx+1)
                fldIdx += 2
            } else {
                val kv = rawCsvFields(fldIdx)
                val parts = kv.split(_keyDelimiter, -1)
                if(parts.length != 2) {
                    Error(s"The supplied KV record has field ($fldIdx) without key delimiter or more than one delimiter, record: $rawCsvContainerStr")
                    throw new ObjectNotFoundException("The supplied KV record format is not correct...abandoning processing", null)
                }
                k = parts(0)
                v = parts(1)
                fldIdx += 1
            }
            val at = ci.getAttributeType(k)
            if (at == null) {
                if (!ci.isFixed)
                    ci.set(k, v)
            }
            else {
                ci.set(k, resolveValue(v, at))
            }
        }
        ci
    }
}

