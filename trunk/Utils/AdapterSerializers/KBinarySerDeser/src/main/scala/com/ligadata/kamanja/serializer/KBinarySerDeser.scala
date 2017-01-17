package com.ligadata.kamanja.serializer

import com.ligadata.BaseTypes._

import scala.collection.immutable.Map
import scala.collection.JavaConverters._
import java.io._
import java.util.zip.{CRC32, Checksum}

import scala.reflect.runtime.universe._
import org.apache.logging.log4j._
import com.ligadata.Exceptions._
import com.ligadata.KamanjaBase._
import com.ligadata.KamanjaBase.AttributeTypeInfo.TypeCategory
import com.ligadata.KamanjaBase.AttributeTypeInfo.TypeCategory._

//@@TODO: move this into utils and use for all logging
object KBinLog {
  private val log = LogManager.getLogger(getClass)

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

import KBinLog._

/**
  * KBinarySerDeser instance can serialize a ContainerInterface to a byte array and deserialize a byte array to form
  * an instance of the ContainerInterface encoded in its bytes.
  *
  * Pre-condition: The JSONSerDes must be initialized with the metadata manager, object resolver and class loader
  * before it can be used.
  */
class KBinarySerDeser extends SerializeDeserialize {

  var _objResolver: ObjectResolver = null
  var _config: Map[String, String] = Map[String, String]()
  var _isReady: Boolean = false


  case class ContainerHeader(schemaId: Long, fieldCnt: Int, isFixed: Boolean, dataSize: Int, checksumVal: Int)

  case class ArrayHeader(itmType: TypeCategory, cnt: Int)

  case class MapHeader(keyType: TypeCategory, valType: TypeCategory, valSchemaId: Long, cnt: Int)

  // write container header:
  // header: schemaId: Int, fieldCnt: Int, isFixed: Bool, serializedSize:Int, checksum: Int
  private def WriteContainerHeader(dos: DataOutputStream, h: ContainerHeader) = {
    dos.writeLong(h.schemaId)
    dos.writeInt(h.fieldCnt)
    dos.writeBoolean(h.isFixed)
    dos.writeInt(h.dataSize)
    dos.writeInt(h.checksumVal)
  }

  // Read container header:
  private def ReadContainerHeader(dis: DataInputStream): ContainerHeader = {
    val schemaId = dis.readLong
    val fieldCnt = dis.readInt
    val isFixed = dis.readBoolean
    val dataSize = dis.readInt
    val checksumValue = dis.readInt
    ContainerHeader(schemaId, fieldCnt, isFixed, dataSize, checksumValue)
  }

  private def ReadArrayHeader(dis: DataInputStream): ArrayHeader = {
    val vt = AttributeTypeInfo.toTypeCategory(dis.readShort)
    val cnt = dis.readInt
    ArrayHeader(vt, cnt)
  }

  private def WriteArrayHeader(dos: DataOutputStream, ah: ArrayHeader) = {
    dos.writeShort(ah.itmType.getValue.toShort)
    dos.writeInt(ah.cnt)
  }

  private def WriteMapHeader(dos: DataOutputStream, mh: MapHeader) = {
    dos.writeShort(mh.keyType.getValue.toShort)
    dos.writeShort(mh.valType.getValue.toShort)
    dos.writeLong(mh.valSchemaId)
    dos.writeInt(mh.cnt)
  }

  private def ReadMapHeader(dis: DataInputStream): MapHeader = {
    val kt = AttributeTypeInfo.toTypeCategory(dis.readShort)
    val vt = AttributeTypeInfo.toTypeCategory(dis.readShort)
    val valSchemaId = dis.readLong
    val cnt = dis.readInt
    MapHeader(kt, vt, valSchemaId, cnt)
  }

  private def WriteFldInfo(dos: DataOutputStream, tc: TypeCategory, name: String): Unit = {
    dos.writeShort(tc.getValue.toShort)
    dos.writeUTF(name)
  }

  private def WriteFldInfo(dos: DataOutputStream, fld: AttributeValue): Unit =
    WriteFldInfo(dos, fld.getValueType.getTypeCategory, fld.getValueType.getName)

  private def ReadFldInfo(dis: DataInputStream): (TypeCategory, String) = {
    val tc = dis.readShort()
    val name = dis.readUTF()
    val typeCategory = AttributeTypeInfo.toTypeCategory(tc)
    (typeCategory, name)
  }

  /**
    * Serialize the supplied container to a byte array using Binary format
    *
    * @param v a ContainerInterface (describes a standard kamanja container)
    */
  @throws(classOf[com.ligadata.Exceptions.ObjectNotFoundException])
  @throws(classOf[com.ligadata.Exceptions.UnsupportedObjectException])
  def serialize(v: ContainerInterface): Array[Byte] = {
    if (v == null)
      return null
    val bos: ByteArrayOutputStream = new ByteArrayOutputStream(8 * 1024)
    val dos = new DataOutputStream(bos)

    serializeContainer(dos, v.getTypeName, v)

    // Save TransactionId, TimePartitioinKey & RowNumber
    dos.writeLong(v.getTransactionId)
    dos.writeLong(v.getTimePartitionData)
    dos.writeInt(v.getRowNumber)

    dos.close()
    bos.toByteArray
  }

  private def serializeContainer(dosFinal: DataOutputStream, name: String, v: ContainerInterface): Unit = {
    Debug(s"KBinarySerDeser:serializeContainer ->, name: $name, typeName:${v.getTypeName}")
    val bos: ByteArrayOutputStream = new ByteArrayOutputStream(8 * 1024)
    val dos = new DataOutputStream(bos)

    val fields = v.getAllAttributeValues
    val fieldCnt = fields.length
    val isFixed = v.isFixed
    var processCnt: Int = 0
    fields.foreach(fld => {
      processCnt += 1
      val valueType = fld.getValueType
      val rawValue: Any = fld.getValue
      if (!isFixed)
        WriteFldInfo(dos, fld)
      valueType.getTypeCategory match {
        case MAP => mapAsKBinary(dos, valueType, rawValue.asInstanceOf[scala.collection.immutable.Map[Any, Any]])
        case ARRAY => arrayAsKBinary(dos, valueType, rawValue.asInstanceOf[Array[_]])
        case (MESSAGE | CONTAINER) => serializeContainer(dos, valueType.getName, rawValue.asInstanceOf[ContainerInterface])
        case (BOOLEAN | BYTE | CHAR | LONG | DOUBLE | FLOAT | INT | STRING) => WriteVal(dos, valueType.getTypeCategory, rawValue)
        case _ => throw new UnsupportedObjectException(s"container type ${valueType.getName} not currently serializable", null)
      }
    })
    val dataSize = dos.size
    dos.close()
    val bytes = bos.toByteArray

    // prepare header..
    // header: schemaId: Int, fieldCnt: Int, isFixed: Bool, serializedSize:Int, checksum: Int
    val schemaId = v.getSchemaId
    val checksum = new CRC32
    checksum.update(bytes, 0, bytes.length)
    val checksumValue = checksum.getValue.toInt

    WriteContainerHeader(dosFinal, ContainerHeader(schemaId, fieldCnt, isFixed, dataSize, checksumValue))
    // data: byte array
    dosFinal.write(bytes)
    Debug(s"KBinarySerDeser:serializeContainer <-, name: $name, typeName:${v.getTypeName}, cnt: $fieldCnt, isFixed: $isFixed, schemaId: $schemaId, dataSize: $dataSize")
  }

  // DataOutputStream - doesn't handle strings greater than 64k, hence these two specialized functions to write and read
  // strings.
  // WriteStrVal -
  //  Write INT length of byte array derived from given string
  //  Write UTF-8 byte array representation
  //
  private def WriteStrVal(dos: DataOutputStream, str: String) = {
    val bytes = if(str.length > 0) str.getBytes("UTF-8") else Array[Byte]()
    val len = bytes.length
    dos.writeInt(len)
    if(len > 0)
      dos.write(bytes)
  }

  // ReadStrVal does exact opposite of WriteStrVal -
  //  Read integer that represent length of array
  //  Allocate byte array of that size
  //  Read UTF-8 byte buffer and convert that into a string
  private def ReadStrVal(dos: DataInputStream) : String = {
    val len = dos.readInt
    val bytes = if(len > 0) new Array[Byte](len) else Array[Byte]()
    if(len > 0) {
      dos.readFully(bytes)
      new String(bytes, "UTF-8")
    }
    else ""
  }

  private def WriteVal(dos: DataOutputStream, typeCategory: TypeCategory, v: Any) =
    typeCategory match {
      case BOOLEAN => dos.writeBoolean(v.asInstanceOf[Boolean])
      case BYTE => dos.writeByte(v.asInstanceOf[Byte])
      case CHAR => dos.writeChar(v.asInstanceOf[Char])
      case LONG => dos.writeLong(v.asInstanceOf[Long])
      case INT => dos.writeInt(v.asInstanceOf[Int])
      case FLOAT => dos.writeFloat(v.asInstanceOf[Float])
      case DOUBLE => dos.writeDouble(v.asInstanceOf[Double])
      case STRING => WriteStrVal(dos, if (v !=null) v.asInstanceOf[String] else "")
      case _ => throw new UnsupportedObjectException(s"KBinary WriteVal got unsupported type, typeId: ${typeCategory.name}", null)
    }

  private def ReadVal(dis: DataInputStream, typeCategory: TypeCategory): Any =
    typeCategory match {
      case BOOLEAN => dis.readBoolean
      case BYTE => dis.readByte
      case CHAR => dis.readChar
      case LONG => dis.readLong
      case INT => dis.readInt
      case FLOAT => dis.readFloat
      case DOUBLE => dis.readDouble
      case STRING => ReadStrVal(dis)
      case _ => throw new UnsupportedObjectException(s"KBinary ReadVal got unsupported type, typeId: ${typeCategory.name}", null)
    }

  /**
    * Create a kbinary from the supplied immutable map.  Note that either/both map elements can in turn be
    * containers.  As a pre-condition, the map cannot be null and can have zero to N key/value pairs.
    *
    * @param dos the output stream to receive the value
    * @param map the map instance
    * @return Unit
    */
  @throws(classOf[IOException])
  private def mapAsKBinary(dos: DataOutputStream, attribType: AttributeTypeInfo, map: scala.collection.immutable.Map[Any, Any]): Unit = {
    mapAsKBinary(dos, attribType.getKeyTypeCategory, attribType.getValTypeCategory, attribType.getValSchemaId, map)
  }

  @throws(classOf[IOException])
  private def mapGenericAsKBinary(dos: DataOutputStream, map: scala.collection.immutable.Map[Any, Any]): Unit = {
    val (keyType, valType) = mapElemTypes(map)
    mapAsKBinary(dos, keyType, valType, 0, map)
  }

  private def mapAsKBinary(dos: DataOutputStream, keyType: TypeCategory, valType: TypeCategory, valSchemaId: Long, map: scala.collection.immutable.Map[Any, Any]): Unit = {
    Debug(s"KBinarySerDeser:mapAsKBinary ->, keyType: ${keyType.getValue}, valType: ${valType.getValue}, schemaId:$valSchemaId, map.size: ${map.size}")
    if (map != null) {
      keyType match {
        case (BYTE | LONG | CHAR | INT | STRING) => ;
        case _ => throw new UnsupportedObjectException(s"KBinary serialize doesn't support maps as with complex key types, keyType: ${keyType.name}", null)
      }
      if (valType == NONE)
        throw new UnsupportedObjectException(s"KBinary serialize doesn't support nested maps other than simple value types", null)
    }

    WriteMapHeader(dos, MapHeader(keyType, valType, valSchemaId, if (map == null) 0 else map.size))
    if (map == null)
      return
    var idx = 0
    map.foreach(pair => {
      val k = pair._1
      val v = pair._2
      idx += 1
      WriteVal(dos, keyType, k)
      valType match {
        case (BOOLEAN | BYTE | CHAR | LONG | INT | FLOAT | DOUBLE | STRING) => WriteVal(dos, valType, v)
        case MAP => mapGenericAsKBinary(dos, v.asInstanceOf[scala.collection.immutable.Map[Any, Any]])
        case ARRAY => arrayGenericAsKBinary(dos, v.asInstanceOf[Array[_]])
        case (CONTAINER | MESSAGE) => serializeContainer(dos, "", v.asInstanceOf[ContainerInterface])
        case NONE => ; // shouldn't match this
      }
    })
    Debug(s"KBinarySerDeser:mapAsKBinary <-, keyType: ${keyType.getValue}, valType: ${valType.getValue}, schemaId:$valSchemaId, map.size: ${map.size}")
  }

  /**
    * Create a Json string from the supplied array.  Note that the array elements can themselves
    * be containers.  As a pre-condition, the array cannot be null and can have zero to N items.
    *
    * @param dos   the output stream to receive the value
    * @param array the array instance
    * @return Unit
    */
  @throws(classOf[IOException])
  private def arrayAsKBinary(dos: DataOutputStream, attribType: AttributeTypeInfo, array: Array[_]): Unit = {
    val itmType = attribType.getValTypeCategory
    val memberCnt = if (array == null) 0 else array.size
    Debug(s"KBinarySerDeser:arrayAsKBinary ->, itmType: ${itmType.getValue}, array.size: $memberCnt")
    WriteArrayHeader(dos, ArrayHeader(itmType, memberCnt))

    if (array != null && memberCnt > 0) {
      array.foreach(itm => {
        itmType match {
          case (BOOLEAN | BYTE | LONG | CHAR | INT | FLOAT | DOUBLE | STRING) => WriteVal(dos, itmType, itm)
          case MAP => mapGenericAsKBinary(dos, itm.asInstanceOf[scala.collection.immutable.Map[Any, Any]])
          case ARRAY => arrayGenericAsKBinary(dos, itm.asInstanceOf[Array[_]])
          case (CONTAINER | MESSAGE) => serializeContainer(dos, "", itm.asInstanceOf[ContainerInterface])
          case NONE => ; // shouldn't match this
        }
      })
    }
    Debug(s"KBinarySerDeser:arrayAsKBinary <-, itmType: ${itmType.getValue}, array.size: $memberCnt")
  }

  private def mapElemTypes[T: TypeTag](map: T): (TypeCategory, TypeCategory) = {
    if (map == null) (NONE, NONE)
    else typeTag[T].tpe match {
      case t if t =:= typeOf[scala.collection.immutable.Map[String, String]] => (STRING, STRING)
      case t if t =:= typeOf[scala.collection.immutable.Map[String, Long]] => (STRING, LONG)
      case t if t =:= typeOf[scala.collection.immutable.Map[String, Int]] => (STRING, INT)
      case t if t =:= typeOf[scala.collection.immutable.Map[String, Float]] => (STRING, FLOAT)
      case t if t =:= typeOf[scala.collection.immutable.Map[String, Double]] => (STRING, DOUBLE)

      case t if t =:= typeOf[scala.collection.immutable.Map[Int, String]] => (INT, STRING)
      case t if t =:= typeOf[scala.collection.immutable.Map[Int, Int]] => (INT, INT)
      case t if t =:= typeOf[scala.collection.immutable.Map[Int, Long]] => (INT, LONG)
      case t if t =:= typeOf[scala.collection.immutable.Map[Int, Float]] => (INT, FLOAT)
      case t if t =:= typeOf[scala.collection.immutable.Map[Int, Double]] => (INT, DOUBLE)

      case t if t =:= typeOf[scala.collection.immutable.Map[Long, String]] => (LONG, STRING)
      case t if t =:= typeOf[scala.collection.immutable.Map[Long, Long]] => (LONG, LONG)
      case t if t =:= typeOf[scala.collection.immutable.Map[Long, Int]] => (LONG, INT)
      case t if t =:= typeOf[scala.collection.immutable.Map[Long, Float]] => (LONG, FLOAT)
      case t if t =:= typeOf[scala.collection.immutable.Map[Long, Double]] => (LONG, DOUBLE)
      case _ => throw new UnsupportedObjectException(s"mapElemTypes are not currently serializable", null)
    }
  }

  private def arrayElemType[T: TypeTag](array: T): TypeCategory = {
    if (array == null) NONE
    else typeTag[T].tpe match {
      case t if t =:= typeOf[Array[String]] => STRING
      case t if t =:= typeOf[Array[Boolean]] => BOOLEAN
      case t if t =:= typeOf[Array[Char]] => CHAR
      case t if t =:= typeOf[Array[Byte]] => BYTE
      case t if t =:= typeOf[Array[Int]] => INT
      case t if t =:= typeOf[Array[Long]] => LONG
      case t if t =:= typeOf[Array[Float]] => FLOAT
      case t if t =:= typeOf[Array[Double]] => DOUBLE
      case t if t =:= typeOf[Array[ContainerInterface]] => CONTAINER
      case _ => NONE
    }
  }

  private def arrayGenericAsKBinary(dos: DataOutputStream, array: Array[_]): Unit = {
    val memberCnt = if (array == null) 0 else array.size
    val itmType = arrayElemType(array)
    if ((array != null) && (itmType == NONE))
      throw new UnsupportedObjectException(s"container type ${itmType.name()} not currently serializable", null)
    WriteArrayHeader(dos, ArrayHeader(itmType, memberCnt))
    if (array != null)
      array.foreach(itm => WriteVal(dos, itmType, itm))
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
  def deserialize(b: Array[Byte], containerName: String): ContainerInterface = {
    var dis = new DataInputStream(new ByteArrayInputStream(b));
    val container = ReadContainer(dis, containerName)
    // Set TransactionId, TimePartitioinKey & RowNumber
    if (container != null) {
      container.setTransactionId(dis.readLong)
      container.setTimePartitionData(dis.readLong)
      container.setRowNumber(dis.readInt)
    }
    container
  }

  /**
    * Deserialize the supplied byte array into some ContainerInterface instance.
    *
    * @param dis the DataInputStream that contains the container
    * @return a ContainerInterface
    */
  @throws(classOf[com.ligadata.Exceptions.ObjectNotFoundException])
  @throws(classOf[com.ligadata.Exceptions.MissingPropertyException])
  @throws(classOf[com.ligadata.Exceptions.UnsupportedObjectException])
  @throws(classOf[IOException])
  private def ReadContainer(dis: DataInputStream, containerName: String): ContainerInterface = {
    val hdr = ReadContainerHeader(dis)
    Debug(s"ReadContainer -> cnt: ${hdr.fieldCnt}, isFixed: ${hdr.isFixed}, schemaId: ${hdr.schemaId}, dataSize: ${hdr.dataSize}")
    var ci: ContainerInterface = null
    val nonNullContainerName = if (containerName != null) containerName else ""
    var possibleException: Throwable = null
    try {
      ci = _objResolver.getInstance(hdr.schemaId)
      if (ci == null) {
        possibleException = new ObjectNotFoundException(s"KBinary ReadContainer - schemaid ${hdr.schemaId} & ContainerName ${nonNullContainerName} could not be resolved for deserialize", null)
      }
    } catch {
      case e: Exception => {
        possibleException = e
      }
      case e: Throwable => {
        possibleException = e
      }
    }

    if (ci == null && containerName != null) {
      ci = _objResolver.getInstance(containerName)
    }

    if (ci == null) {
      if (possibleException != null)
        throw possibleException
      throw new ObjectNotFoundException(s"KBinary ReadContainer - schemaid ${hdr.schemaId} & ContainerName ${nonNullContainerName} could not be resolved for deserialize", null)
    }

    val attribs = ci.getAttributeTypes
    for (idx <- 0 until hdr.fieldCnt) {
      var attribType: TypeCategory = NONE
      var attribName = ""

      if (hdr.isFixed) {
        attribType = attribs(idx).getTypeCategory
        attribName = attribs(idx).getName
      }
      else {
        val (at, an) = ReadFldInfo(dis)
        attribType = at;
        attribName = an
      }
      val fld = attribType match {
        case MAP => ReadMap(dis)
        case ARRAY => ReadArray(dis)
        case (MESSAGE | CONTAINER) => ReadContainer(dis, null) // BUGBUG:: Get container type and pass it instead of NULL
        case (BOOLEAN | BYTE | CHAR | LONG | DOUBLE | FLOAT | INT | STRING) => ReadVal(dis, attribType)
        case _ => throw new UnsupportedObjectException(s"container type ${attribType.name} not currently serializable", null)
      }
      if (hdr.isFixed) ci.set(idx, fld)
      else ci.set(attribName, fld)
    }
    Debug(s"ReadContainer <- cnt: ${hdr.fieldCnt}, isFixed: ${hdr.isFixed}, schemaId: ${hdr.schemaId}, dataSize: ${hdr.dataSize}")
    ci
  }

  private def ReadMapT[K, V](dis: DataInputStream, cnt: Int, fn: DataInputStream => (K, V)): Map[K, V] = {
    val map = scala.collection.mutable.HashMap.empty[K, V]
    for (idx <- 0 until cnt) {
      val rslt = fn(dis); map(rslt._1) = rslt._2;
    }
    map.toMap
  }

  private def ReadArrayT[T](dis: DataInputStream, array: Array[T], cnt: Int, fn: DataInputStream => T) = {
    for (idx <- 0 until cnt) array(idx) = fn(dis)
    array
  }

  private def ReadMap(dis: DataInputStream): Any = {
    val hdr = ReadMapHeader(dis)
    Debug(s"ReadMap ->, cnt: ${hdr.cnt}, keyType: ${hdr.keyType.getValue}, valType: ${hdr.valType.getValue}")

    val map = hdr.valType match {
      case BOOLEAN => ReadMapT(dis, hdr.cnt, { d: DataInputStream => (ReadVal(d, hdr.keyType), d.readBoolean) })
      case BYTE => ReadMapT(dis, hdr.cnt, { d: DataInputStream => (ReadVal(d, hdr.keyType), d.readByte) })
      case CHAR => ReadMapT(dis, hdr.cnt, { d: DataInputStream => (ReadVal(d, hdr.keyType), d.readChar) })
      case LONG => ReadMapT(dis, hdr.cnt, { d: DataInputStream => (ReadVal(d, hdr.keyType), d.readLong) })
      case DOUBLE => ReadMapT(dis, hdr.cnt, { d: DataInputStream => (ReadVal(d, hdr.keyType), d.readDouble) })
      case FLOAT => ReadMapT(dis, hdr.cnt, { d: DataInputStream => (ReadVal(d, hdr.keyType), d.readFloat) })
      case INT => ReadMapT(dis, hdr.cnt, { d: DataInputStream => (ReadVal(d, hdr.keyType), d.readInt) })
      case STRING => ReadMapT(dis, hdr.cnt, { d: DataInputStream => (ReadVal(d, hdr.keyType), ReadStrVal(d)) })
      case MAP => ReadMapT(dis, hdr.cnt, { d: DataInputStream => (ReadVal(d, hdr.keyType), ReadMap(d)) })
      case (MESSAGE | CONTAINER) => ReadMapT(dis, hdr.cnt, { d: DataInputStream => (ReadVal(d, hdr.keyType), ReadContainer(d, null)) }) // BUGBUG:: Get Type and pass it here instead of null
      case ARRAY => ReadMapT(dis, hdr.cnt, { d: DataInputStream => (ReadVal(d, hdr.keyType), ReadArray(d)) })
      case _ => throw new UnsupportedObjectException(s"Unsupported value type while loading array, valType: ${hdr.valType}", null)
    }
    Debug(s"ReadMap <-, cnt: ${hdr.cnt}, keyType: ${hdr.keyType.getValue}, valType: ${hdr.valType.getValue}")
    map
  }

  private def ReadArray(dis: DataInputStream): Any = {
    val hdr = ReadArrayHeader(dis)
    Debug(s"ReadArray ->, cnt: ${hdr.cnt}, valType: ${hdr.itmType.getValue}")

    val array = hdr.itmType match {
      case BOOLEAN => ReadArrayT(dis, new Array[Boolean](hdr.cnt), hdr.cnt, { d: DataInputStream => d.readBoolean })
      case BYTE => ReadArrayT(dis, new Array[Byte](hdr.cnt), hdr.cnt, { d: DataInputStream => d.readByte })
      case CHAR => ReadArrayT(dis, new Array[Char](hdr.cnt), hdr.cnt, { d: DataInputStream => d.readChar })
      case LONG => ReadArrayT(dis, new Array[Long](hdr.cnt), hdr.cnt, { d: DataInputStream => d.readLong })
      case DOUBLE => ReadArrayT(dis, new Array[Double](hdr.cnt), hdr.cnt, { d: DataInputStream => d.readDouble })
      case FLOAT => ReadArrayT(dis, new Array[Float](hdr.cnt), hdr.cnt, { d: DataInputStream => d.readFloat })
      case INT => ReadArrayT(dis, new Array[Int](hdr.cnt), hdr.cnt, { d: DataInputStream => d.readInt })
      case STRING => ReadArrayT(dis, new Array[String](hdr.cnt), hdr.cnt, { d: DataInputStream => ReadStrVal(d) })
      case (MESSAGE | CONTAINER) => ReadArrayT(dis, new Array[ContainerInterface](hdr.cnt), hdr.cnt, { d: DataInputStream => ReadContainer(d, null) }) // BUGBUG:: Get Container Name from type and pass it here instead of null
      case ARRAY => ReadArrayT(dis, new Array[Any](hdr.cnt), hdr.cnt, { d: DataInputStream => ReadArray(d) })
      case _ => throw new UnsupportedObjectException(s"Unsupported value type while loading array, itmType: ${hdr.itmType}", null)
    }
    Debug(s"ReadArray <-, cnt: ${hdr.cnt}, valType: ${hdr.itmType.getValue}")
    array
  }
}
