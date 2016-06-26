package com.ligadata.kamanja.serializer

import org.json4s.jackson.JsonMethods._
import org.json4s.{DefaultFormats, Formats, MappingException}

import scala.collection.mutable.{ArrayBuffer}
import scala.collection.JavaConverters._
import java.io.{ByteArrayOutputStream, DataOutputStream}

import com.ligadata.kamanja.metadata._
import com.ligadata.Exceptions._
import com.ligadata.KamanjaBase.AttributeTypeInfo.TypeCategory
import com.ligadata.KamanjaBase.AttributeTypeInfo.TypeCategory._
import com.ligadata.KamanjaBase._
import org.apache.logging.log4j.LogManager
import org.apache.commons.lang3.StringEscapeUtils;

/**
  * Meta fields found at the beginning of each JSON representation of a ContainerInterface object
  */
//@@TODO: move this into utils and use for all logging
object JSonLog {
  private val log = LogManager.getLogger(getClass);

  def Trace(str: String) = if (log.isTraceEnabled()) log.trace(str)

  def Warning(str: String) = if (log.isWarnEnabled()) log.warn(str)

  def Info(str: String) = if (log.isInfoEnabled()) log.info(str)

  def Error(str: String) = if (log.isErrorEnabled()) log.error(str)

  def Debug(str: String) = if (log.isDebugEnabled()) log.debug(str)

  def Trace(str: String, e: Throwable) = if (log.isTraceEnabled()) log.trace(str, e)

  def Warning(str: String, e: Throwable) = if (log.isWarnEnabled()) log.warn(str, e)

  def Info(str: String, e: Throwable) = if (log.isInfoEnabled()) log.info(str, e)

  def Error(str: String, e: Throwable) = if (log.isErrorEnabled()) log.error(str, e)

  def Debug(str: String, e: Throwable) = if (log.isDebugEnabled()) log.debug(str, e)

  def Trace(e: Throwable) = if (log.isTraceEnabled()) log.trace("", e)

  def Warning(e: Throwable) = if (log.isWarnEnabled()) log.warn("", e)

  def Info(e: Throwable) = if (log.isInfoEnabled()) log.info("", e)

  def Error(e: Throwable) = if (log.isErrorEnabled()) log.error("", e)

  def Debug(e: Throwable) = if (log.isDebugEnabled()) log.debug("", e)

  def isTraceEnabled = log.isTraceEnabled()

  def isWarnEnabled = log.isWarnEnabled()

  def isInfoEnabled = log.isInfoEnabled()

  def isErrorEnabled = log.isErrorEnabled()

  def isDebugEnabled = log.isDebugEnabled()
}


object JSONSerDes {
  val indents = ComputeIndents
  val strLF = "\n"
  val maxIndentLevel = 64

  def getIndentStr(indentLevel: Int): String = {
    if (indentLevel < 0 || indents.size == 0)
      return ""
    if (indentLevel >= indents.size)
      indents(indents.size - 1)
    else
      indents(indentLevel)
  }

  private
  def ComputeIndents: Array[String] = {
    val indentsTemp = ArrayBuffer[String]()
    indentsTemp += ""
    val indent = "  "
    for (idx <- 1 to maxIndentLevel) indentsTemp += (indent + indentsTemp(idx - 1))
    indentsTemp.toArray
  }
}

import JSONSerDes._
import JSonLog._

/**
  * JSONSerDeser instance can serialize a ContainerInterface to a byte array and deserialize a byte array to form
  * an instance of the ContainerInterface encoded in its bytes.
  *
  * Pre-condition: The JSONSerDes must be initialized with the metadata manager, object resolver and class loader
  * before it can be used.
  */

class JSONSerDes extends SerializeDeserialize {
  var _objResolver: ObjectResolver = null
  var _config = Map[String, String]()
  var _isReady: Boolean = false
  var _emitSchemaId = true
  var _schemaIdKeyPrefix = "@@"

  def SchemaIDKeyName = _schemaIdKeyPrefix + "SchemaId"
  def TransactionIDKeyName = _schemaIdKeyPrefix + "TransactionId"
  def TimePartitionIDKeyName = _schemaIdKeyPrefix + "TimePartitionValue"
  def RowNumberIDKeyName = _schemaIdKeyPrefix + "RowNumber"

  /**
    * Serialize the supplied container to a byte array
    *
    * @param v a ContainerInterface (describes a standard kamanja container)
    */
  @throws(classOf[com.ligadata.Exceptions.ObjectNotFoundException])
  @throws(classOf[com.ligadata.Exceptions.UnsupportedObjectException])
  def serialize(v: ContainerInterface): Array[Byte] = {
    val sb = new StringBuilder(8 * 1024)
    containerAsJson(sb, 0, v)
    val strRep = sb.toString()
    if (isDebugEnabled) {
      Debug(s"Serialized as JSON, data: $strRep")
    }
    strRep.getBytes
  }

  /**
    * Serialize the supplied container to a byte array
    *
    * @param v a ContainerInterface (describes a standard kamanja container)
    */
  @throws(classOf[com.ligadata.Exceptions.UnsupportedObjectException])
  def containerAsJson(sb: StringBuilder, indentLevel: Int, v: ContainerInterface): Unit = {
    if (v == null) return
    val fields = v.getAllAttributeValues
    val fieldCnt: Int = fields.length

    val indentStr = getIndentStr(indentLevel)
    val schemaId = v.getSchemaId
    val txnId = v.getTransactionId
    val tmPartVal = v.getTimePartitionData
    val rowNum = v.getRowNumber
    val containerJsonHead = indentStr + "{ "
    val containerJsonTail = indentStr + " }"
    sb.append(containerJsonHead)
    if (_emitSchemaId) {
      // sb.append(strLF)
      nameValueAsJson(sb, indentLevel + 1, SchemaIDKeyName, schemaId.toString, false)
      sb.append(", ")
      // Save TransactionId, TimePartitioinKey & RowNumber
      nameValueAsJson(sb, indentLevel + 1, TransactionIDKeyName, txnId.toString, false)
      sb.append(", ")
      nameValueAsJson(sb, indentLevel + 1, TimePartitionIDKeyName, tmPartVal.toString, false)
      sb.append(", ")
      nameValueAsJson(sb, indentLevel + 1, RowNumberIDKeyName, rowNum.toString, false)
      if (fieldCnt > 0)
        sb.append(", ")
    }
    var processCnt: Int = 0
    fields.foreach(fld => {
      processCnt += 1
      val valueType = fld.getValueType
      val rawValue: Any = fld.getValue
      val commaSuffix = if (processCnt < fieldCnt) "," else ""
      val quoteValue = useQuotesOnValue(valueType)
      valueType.getTypeCategory match {
        case MAP => {
          keyAsJson(sb, indentLevel + 1, valueType.getName); mapAsJson(sb, indentLevel + 1, valueType, rawValue.asInstanceOf[Map[_, _]])
        }
        case ARRAY => {
          keyAsJson(sb, indentLevel + 1, valueType.getName); arrayAsJson(sb, indentLevel + 1, valueType, rawValue.asInstanceOf[Array[_]])
        }
        case (MESSAGE | CONTAINER) => {
          keyAsJson(sb, indentLevel + 1, valueType.getName); containerAsJson(sb, indentLevel + 1, rawValue.asInstanceOf[ContainerInterface])
        }
        case (BOOLEAN | BYTE | LONG | DOUBLE | FLOAT | INT | STRING | CHAR) => nameValueAsJson(sb, indentLevel + 1, valueType.getName, rawValue, quoteValue)
        case _ => throw new UnsupportedObjectException(s"container type ${valueType.getName} not currently serializable", null)
      }
      sb.append(commaSuffix)
    })
    sb.append(containerJsonTail)
  }

  /**
    * Answer a string consisting of "name" : "value" with/without comma suffix.  When quoteValue parameter is false
    * the value is not quoted (for the scalars and boolean
    *
    * @param name       json key
    * @param value      json value
    * @param quoteValue when true value is quoted
    * @return decorated map element string suitable for including in json map string
    */
  private def nameValueAsJson(sb: StringBuilder, indentLevel: Int, name: String, value: Any, quoteValue: Boolean) = {
    keyAsJson(sb, indentLevel, name)
    valueAsJson(sb, indentLevel, value, quoteValue)
  }

  private def valueAsJson(sb: StringBuilder, indentLevel: Int, value: Any, quoteValue: Boolean) = {
    if (quoteValue) {
      val strVal = if (value != null) StringEscapeUtils.escapeJson(value.toString) else ""
      sb.append('\"' + strVal + '\"')
    } else {
      if (value != null)
        sb.append(value)
      else
        sb.append("")
    }
  }

  /**
    * Answer a string consisting of "name" : "value" with/without comma suffix.  When quoteValue parameter is false
    * the value is not quoted (for the scalars and boolean
    *
    * @param key json key
    */
  private def keyAsJson(sb: StringBuilder, indentLevel: Int, key: String) = sb.append(getIndentStr(indentLevel) + "\"" + key + "\": ")


  /**
    * Ascertain if the supplied type is one that does not require quotes. The scalars and boolean do not.
    *
    * @param fieldTypeDef a BaseTypeDef
    * @return true or false if quotes should be used on the json value
    */
  private def useQuotesOnValue(fieldTypeDef: AttributeTypeInfo): Boolean = if (fieldTypeDef == null) true else useQuotesOnValue(fieldTypeDef.getTypeCategory)

  private def useQuotesOnValue(typeCategory: TypeCategory): Boolean = {
    typeCategory match {
      case (MAP | ARRAY | MESSAGE | CONTAINER | BOOLEAN | BYTE | LONG | DOUBLE | FLOAT | INT) => false
      case _ => true
    }
  }

  /**
    * Create a Json string from the supplied immutable map.  Note that either/both map elements can in turn be
    * containers.
    *
    * @param attribType The container type def for the supplied map
    * @param map        the map instance
    * @return a Json string representation
    */
  private def mapAsJson(sb: StringBuilder, indentLevel: Int, attribType: AttributeTypeInfo, map: Map[_, _]) = {
    val keyType = attribType.getKeyTypeCategory
    val valType = attribType.getValTypeCategory
    val quoteValue = useQuotesOnValue(valType)

    keyType match {
      case (BOOLEAN | BYTE | LONG | DOUBLE | FLOAT | INT | STRING | CHAR) => ;
      case _ => throw new UnsupportedObjectException(s"json serialize doesn't support maps as with complex key types, keyType: ${keyType.name}", null)
    }
    val indentStr = getIndentStr(indentLevel)

    // @TODO: for now, write entire map as a single line.. later it can be done in multi line using the passed in indentation as basis
    val mapJsonHead = "{ "
    val mapJsonTail = " }"
    sb.append(mapJsonHead)
    var idx = 0
    if (map != null) {
      map.foreach(pair => {
        val k = pair._1
        val v = pair._2
        if (idx > 0) sb.append(", ")
        idx += 1
        sb.append(mapJsonHead)
        keyAsJson(sb, 0, k.toString)
        valType match {
          case (BOOLEAN | BYTE | LONG | DOUBLE | FLOAT | INT | STRING | CHAR) => valueAsJson(sb, 0, v, quoteValue);
          case MAP => mapGenericAsJson(sb, indentLevel, v.asInstanceOf[scala.collection.mutable.Map[_, _]])
          case ARRAY => arrayGenericAsJson(sb, indentLevel, v.asInstanceOf[Array[_]])
          case (CONTAINER | MESSAGE) => containerAsJson(sb, 0, v.asInstanceOf[ContainerInterface])
          case _ => throw new UnsupportedObjectException("Not yet handled valType:" + valType, null)
        }
        sb.append(mapJsonTail)
      })
    }
    sb.append(mapJsonTail)
  }

  private def mapGenericAsJson(sb: StringBuilder, indentLevel: Int, map: scala.collection.mutable.Map[_, _]) = {
    val indentStr = getIndentStr(indentLevel)
    // @TODO: for now, write entire map as a single line.. later it can be done in multi line using the passed in indentation as basis
    val mapJsonHead = "{ "
    val mapJsonTail = " }"
    sb.append(mapJsonHead)
    var idx = 0
    map.foreach(pair => {
      val k = pair._1
      val v = pair._2
      if (idx > 0) sb.append(", ")
      idx += 1
      sb.append(mapJsonHead)
      keyAsJson(sb, 0, k.toString)
      valueAsJson(sb, 0, v, v.isInstanceOf[String])
      sb.append(mapJsonTail)
    })
    sb.append(mapJsonTail)
  }

  private def arrayGenericAsJson(sb: StringBuilder, indentLevel: Int, array: Array[_]) = {
    val indentStr = getIndentStr(indentLevel)
    // @TODO: for now, write entire map as a single line.. later it can be done in multi line using the passed in indentation as basis
    val mapJsonHead = "[ "
    val mapJsonTail = " ]"
    sb.append(mapJsonHead)
    if (array != null && array.size > 0) {
      var idx = 0
      array.foreach(elem => {
        if (idx > 0) sb.append(", ")
        idx += 1
        valueAsJson(sb, 0, elem, elem.isInstanceOf[String])
      })
    }
    sb.append(mapJsonTail)
  }

  /**
    * Create a Json string from the supplied array.  Note that the array elements can themselves
    * be containers.
    *
    * @param attribType The container type def for the supplied array
    * @param array      the array instance
    * @return a Json string representation
    */
  private def arrayAsJson(sb: StringBuilder, indentLevel: Int, attribType: AttributeTypeInfo, array: Array[_]) = {
    val mapJsonHead = "[ "
    val mapJsonTail = " ]"
    sb.append(mapJsonHead)
    var idx = 0
    if (array != null && array.size > 0) {
      val itemType = attribType.getValTypeCategory
      val quoteValue = useQuotesOnValue(itemType)
      array.foreach(itm => {
        if (idx > 0) sb.append(", ")
        idx += 1
        itemType match {
          case (BOOLEAN | BYTE | LONG | DOUBLE | FLOAT | INT | STRING | CHAR) => valueAsJson(sb, 0, itm, quoteValue);
          case MAP => mapGenericAsJson(sb, indentLevel, itm.asInstanceOf[scala.collection.mutable.Map[_, _]])
          case ARRAY => arrayGenericAsJson(sb, indentLevel, itm.asInstanceOf[Array[_]])
          case (CONTAINER | MESSAGE) => containerAsJson(sb, 0, itm.asInstanceOf[ContainerInterface])
          case _ => throw new UnsupportedObjectException("Not yet handled itemType:" + itemType, null)
        }
      })
    }
    sb.append(mapJsonTail)
  }


  /**
    * Set the object resolver to be used for this serializer
    *
    * @param objRes an ObjectResolver
    */
  def setObjectResolver(objRes: ObjectResolver): Unit = {
    _objResolver = objRes
  }

  /**
    * Configure the SerializeDeserialize adapter.  This must be done before the adapter implementation can be used.
    *
    * @param objResolver the ObjectResolver instance that can instantiate ContainerInterface instances
    * @param config      a map of options that might be used to configure the execution of this SerializeDeserialize instance. This may
    *                    be null if it is not necessary for the SerializeDeserialize implementation
    */
  def configure(objResolver: ObjectResolver, config: java.util.Map[String, String]): Unit = {
    _objResolver = objResolver
    _config = if (config != null) config.asScala.toMap else Map[String, String]()
    _isReady = _objResolver != null && _config != null
  }

  /**
    * Deserialize the supplied byte array into some ContainerInterface instance.
    *
    * @param b the byte array containing the serialized ContainerInterface instance
    * @return a ContainerInterface
    */
  @throws(classOf[com.ligadata.Exceptions.ObjectNotFoundException])
  def deserialize(b: Array[Byte], containerName: String): ContainerInterface = {
    val rawJsonContainerStr: String = new String(b)
    try {
      val containerInstanceMap: Map[String, Any] = jsonStringAsMap(rawJsonContainerStr)
      val container = deserializeContainerFromJsonMap(containerInstanceMap, containerName)

      val txnId = toLong(containerInstanceMap.getOrElse(TransactionIDKeyName, -1))
      val tmPartVal = toLong(containerInstanceMap.getOrElse(TimePartitionIDKeyName, -1))
      val rowNum = toInt(containerInstanceMap.getOrElse(RowNumberIDKeyName, -1))

      if (txnId >= 0)
        container.setTransactionId(txnId)
      if (tmPartVal >= 0)
        container.setTimePartitionData(tmPartVal)
      if (rowNum >= 0)
        container.setRowNumber(rowNum)

      container
    } catch {
      case e: Throwable => {
        throw new KamanjaException("Failed to deserialize JSON:" + rawJsonContainerStr, e)
      }
    }
  }

  @throws(classOf[com.ligadata.Exceptions.ObjectNotFoundException])
  private def deserializeContainerFromJsonMap(containerInstanceMap: Map[String, Any], containerName: String): ContainerInterface = {
    /** Decode the map to produce an instance of ContainerInterface */

    val schemaIdJson = toLong(containerInstanceMap.getOrElse(SchemaIDKeyName, -1))

    if (schemaIdJson == -1) {
      if (containerName == null || containerName.trim.size == 0)
        throw new MissingPropertyException(s"the supplied map (from json) to deserialize does not have a known schemaid, id: $schemaIdJson", null)
    }
    /** get an empty ContainerInterface instance for this type name from the _objResolver */

    var ci: ContainerInterface = null
      if (schemaIdJson != -1) {
      ci = _objResolver.getInstance(schemaIdJson)
      if (ci == null) {
        throw new ObjectNotFoundException(s"container interface with schema id: $schemaIdJson could not be resolved and built for deserialize", null)
      }
    } else {
      ci = _objResolver.getInstance(containerName.trim)
      if (ci == null) {
        throw new ObjectNotFoundException(s"container interface with schema id: $containerName could not be resolved and built for deserialize", null)
      }
    }

    containerInstanceMap.foreach(pair => {
      val k = pair._1
      val v = pair._2

      val at = ci.getAttributeType(k)
      if (at == null) {
        if (!ci.isFixed)
          ci.set(k, v)
      }
      else {
        // @@TODO: check the type compatibility between "value" field v with the target field
        val valType = at.getTypeCategory
        val fld = valType match {
          case LONG => toLong(v)
          case INT => toInt(v)
          case BYTE => toByte(v)
          case FLOAT => toFloat(v)
          case (BOOLEAN | DOUBLE | STRING) => v
          case CHAR => {
            if (v != null && v.isInstanceOf[String] && v.asInstanceOf[String].size > 0) v.asInstanceOf[String].charAt(0) else ' '
          }
          case MAP => jsonAsMap(at, v.asInstanceOf[Map[String, Any]])
          case (CONTAINER | MESSAGE) => {
            val containerTypeName: String = null //BUGBUG:: Fix this to make the container object properly
            deserializeContainerFromJsonMap(v.asInstanceOf[Map[String, Any]], containerTypeName)
          }
          case ARRAY => jsonAsArray(at, v.asInstanceOf[List[Any]])
          case _ => throw new UnsupportedObjectException("Not yet handled valType:" + valType, null)
        }
        // Warning("Key:%s, Idx:%d, valType:%d, Value:%s. Value Class:%s".format(k, at.getIndex, valType.getValue, fld.toString, fld.getClass.getName))
        ci.set(k, fld)
      }
    })
    ci
  }

  private def toLong(itm: Any): Long = {
    if (itm.isInstanceOf[BigInt])
      itm.asInstanceOf[BigInt].toLong
    else if (itm.isInstanceOf[Long])
      itm.asInstanceOf[Long]
    else if (itm.isInstanceOf[Int])
      itm.asInstanceOf[Int].toLong
    else
      throw new UnsupportedObjectException("Convert to long. Parameter is neither BigInt, Long or Int", null)
  }

  private def toInt(itm: Any): Int = {
    if (itm.isInstanceOf[BigInt])
      itm.asInstanceOf[BigInt].toInt
    else if (itm.isInstanceOf[Long])
      itm.asInstanceOf[Long].toInt
    else if (itm.isInstanceOf[Int])
      itm.asInstanceOf[Int]
    else
      throw new UnsupportedObjectException("Convert to int. Parameter is neither BigInt, Long or Int", null)
  }

  private def toByte(itm: Any): Byte = {
    if (itm.isInstanceOf[BigInt])
      itm.asInstanceOf[BigInt].toByte
    else if (itm.isInstanceOf[Long])
      itm.asInstanceOf[Long].toByte
    else if (itm.isInstanceOf[Int])
      itm.asInstanceOf[Int].toByte
    else
      throw new UnsupportedObjectException("Convert to byte. Parameter is neither BigInt, Long or Int", null)
  }

  private def toFloat(itm: Any): Float = {
    if (itm.isInstanceOf[BigInt])
      itm.asInstanceOf[BigInt].toFloat
    else if (itm.isInstanceOf[Long])
      itm.asInstanceOf[Long].toFloat
    else if (itm.isInstanceOf[Int])
      itm.asInstanceOf[Int].toFloat
    else if (itm.isInstanceOf[Float])
      itm.asInstanceOf[Float]
    else if (itm.isInstanceOf[Double])
      itm.asInstanceOf[Double].toFloat
    else
      throw new UnsupportedObjectException("Convert to float. Parameter is neither BigInt, Long, Int, Float or Double", null)
  }

  /**
    * Coerce the list of mapped elements to an array of the mapped elements' values
    *
    * @param arrayTypeInfo the metadata that describes the array
    * @param collElements  the list of json elements for the array buffer
    * @return an array instance
    */
  private def jsonAsArray(arrayTypeInfo: AttributeTypeInfo, collElements: List[Any]): Any = {
    var retVal: Any = Array[Any]()

    val itmType = arrayTypeInfo.getValTypeCategory
    val fld = itmType match {
      case LONG => {
        retVal = collElements.map(itm => toLong(itm)).toArray
      }
      case INT => {
        retVal = collElements.map(itm => toInt(itm)).toArray
      }
      case BYTE => {
        retVal = collElements.map(itm => toByte(itm)).toArray
      }
      case BOOLEAN => {
        retVal = collElements.map(itm => itm.asInstanceOf[Boolean]).toArray
      }
      case DOUBLE => {
        retVal = collElements.map(itm => itm.asInstanceOf[Double]).toArray
      }
      case FLOAT => {
        retVal = collElements.map(itm => toFloat(itm)).toArray
      }
      case STRING => {
        retVal = collElements.map(itm => itm.asInstanceOf[String]).toArray
      }
      case CHAR => {
        retVal = collElements.map(itm => if (itm != null && itm.isInstanceOf[String] && itm.asInstanceOf[String].size > 0) itm.asInstanceOf[String].charAt(0) else ' ').toArray
      }
      case MAP => {
        retVal = collElements.map(itm => itm.asInstanceOf[Map[String, Any]]).toArray
      }
      case (CONTAINER | MESSAGE) => {
        retVal = collElements.map(itm => {
          val containerTypeName: String = null //BUGBUG:: Fix this to make the container object properly
          deserializeContainerFromJsonMap(itm.asInstanceOf[Map[String, Any]], containerTypeName)
        }).toArray
      }
      case ARRAY => {
        retVal = collElements.map(itm => itm.asInstanceOf[List[Any]].toArray).toArray
      }
      case _ => throw new ObjectNotFoundException(s"jsonAsArray: invalid value type: ${itmType.getValue}, fldName: ${itmType.name} could not be resolved", null)
    }
    retVal
  }

  /**
    * Coerce the list of mapped elements to an immutable map of the mapped elements' values
    *
    * @param mapTypeInfo
    * @param collElements
    * @return
    */
  def jsonAsMap(mapTypeInfo: AttributeTypeInfo, collElements: Map[String, Any]): scala.collection.immutable.Map[Any, Any] = {
    val keyType = mapTypeInfo.getKeyTypeCategory
    // check if keyType is STRING or other, for now, only STRING is supported
    val valType = mapTypeInfo.getValTypeCategory
    val map: scala.collection.immutable.Map[Any, Any] = collElements.map(pair => {
      val key: String = pair._1
      val value: Any = pair._2
      val fld = valType match {
        case LONG => toLong(value)
        case INT => toInt(value)
        case BYTE => toByte(value)
        case FLOAT => toFloat(value)
        case (BOOLEAN | DOUBLE | STRING) => value
        case CHAR => {
          if (value != null && value.isInstanceOf[String] && value.asInstanceOf[String].size > 0) value.asInstanceOf[String].charAt(0) else ' '
        }
        case MAP => value.asInstanceOf[Map[String, Any]]
        case (CONTAINER | MESSAGE) => {
          val containerTypeName: String = null //BUGBUG:: Fix this to make the container object properly
          deserializeContainerFromJsonMap(value.asInstanceOf[Map[String, Any]], containerTypeName)
        }
        case ARRAY => value.asInstanceOf[List[Any]].toArray
        case _ => throw new ObjectNotFoundException(s"jsonAsMap: invalid value type: ${valType.getValue}, fldName: ${valType.name} could not be resolved", null)
      }
      (key, fld)
    }).toMap

    map
  }

  /**
    * Translate the supplied json string to a Map[String, Any]
    *
    * @param configJson
    * @return Map[String, Any]
    */

  @throws(classOf[com.ligadata.Exceptions.Json4sParsingException])
  @throws(classOf[com.ligadata.Exceptions.EngineConfigParsingException])
  def jsonStringAsMap(configJson: String): Map[String, Any] = {
    try {
      implicit val jsonFormats: Formats = DefaultFormats
      val json = parse(configJson)
      Debug("Parsed the json : " + configJson)

      val fullmap = json.values.asInstanceOf[Map[String, Any]]

      fullmap
    } catch {
      case e: MappingException => {
        Debug("", e)
        throw Json4sParsingException(e.getMessage, e)
      }
      case e: Exception => {
        Debug("", e)
        throw EngineConfigParsingException(e.getMessage, e)
      }
    }
  }
}
