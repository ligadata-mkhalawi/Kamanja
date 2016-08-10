
package com.ligadata.messages.V1000000000000;

import org.json4s.jackson.JsonMethods._;
import org.json4s.DefaultFormats;
import org.json4s.Formats;
import com.ligadata.KamanjaBase._;
import com.ligadata.BaseTypes._;
import com.ligadata.Exceptions._;
import org.apache.logging.log4j.{ Logger, LogManager }
import java.util.Date;
import java.io.{ DataInputStream, DataOutputStream, ByteArrayOutputStream }

object IdCodeDimMapped extends RDDObject[IdCodeDimMapped] with ContainerFactoryInterface {

  val log = LogManager.getLogger(getClass)
  type T = IdCodeDimMapped;
  override def getFullTypeName: String = "com.ligadata.messages.IdCodeDimMapped";
  override def getTypeNameSpace: String = "com.ligadata.messages";
  override def getTypeName: String = "IdCodeDimMapped";
  override def getTypeVersion: String = "000001.000000.000000";
  override def getSchemaId: Int = 0;
  override def getTenantId: String = "";
  override def createInstance: IdCodeDimMapped = new IdCodeDimMapped(IdCodeDimMapped);
  override def isFixed: Boolean = false;
  def isCaseSensitive(): Boolean = false;
  override def getContainerType: ContainerTypes.ContainerType = ContainerTypes.ContainerType.CONTAINER
  override def getFullName = getFullTypeName;
  override def getRddTenantId = getTenantId;
  override def toJavaRDDObject: JavaRDDObject[T] = JavaRDDObject.fromRDDObject[T](this);

  def build = new T(this)
  def build(from: T) = new T(from)
  override def getPartitionKeyNames: Array[String] = Array("id");

  override def getPrimaryKeyNames: Array[String] = Array("id");

  override def getTimePartitionInfo: TimePartitionInfo = {
    var timePartitionInfo: TimePartitionInfo = new TimePartitionInfo();
    timePartitionInfo.setFieldName("id");
    timePartitionInfo.setFormat("epochTime");
    timePartitionInfo.setTimePartitionType(TimePartitionInfo.TimePartitionType.DAILY);
    return timePartitionInfo
  }

  override def hasPrimaryKey(): Boolean = {
    val pKeys = getPrimaryKeyNames();
    return (pKeys != null && pKeys.length > 0);
  }

  override def hasPartitionKey(): Boolean = {
    val pKeys = getPartitionKeyNames();
    return (pKeys != null && pKeys.length > 0);
  }

  override def hasTimePartitionInfo(): Boolean = {
    val tmInfo = getTimePartitionInfo();
    return (tmInfo != null && tmInfo.getTimePartitionType != TimePartitionInfo.TimePartitionType.NONE);
  }

  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "IdCodeDimMapped" , "fields":[{ "name" : "id" , "type" : "int"},{ "name" : "code1" , "type" : "float"},{ "name" : "code2" , "type" : "double"},{ "name" : "code3" , "type" : "long"},{ "name" : "code4" , "type" : "string"},{ "name" : "code5" ,}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.messages.V1000000000000.IdCodeDimMapped => { return convertToVer1000000000000(oldVerobj); }
        case _ => {
          throw new Exception("Unhandled Version Found");
        }
      }
    } catch {
      case e: Exception => {
        throw e
      }
    }
    return null;
  }

  private def convertToVer1000000000000(oldVerobj: com.ligadata.messages.V1000000000000.IdCodeDimMapped): com.ligadata.messages.V1000000000000.IdCodeDimMapped = {
    return oldVerobj
  }

  /****   DEPRECATED METHODS ***/
  override def FullName: String = getFullTypeName
  override def NameSpace: String = getTypeNameSpace
  override def Name: String = getTypeName
  override def Version: String = getTypeVersion
  override def CreateNewMessage: BaseMsg = null;
  override def CreateNewContainer: BaseContainer = createInstance.asInstanceOf[BaseContainer];
  override def IsFixed: Boolean = false
  override def IsKv: Boolean = true
  override def CanPersist: Boolean = false
  override def isMessage: Boolean = false
  override def isContainer: Boolean = true
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj IdCodeDimMapped") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj IdCodeDimMapped");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj IdCodeDimMapped");
  override def NeedToTransformData: Boolean = false
}

class IdCodeDimMapped(factory: ContainerFactoryInterface, other: IdCodeDimMapped) extends ContainerInterface(factory) {

  val log = IdCodeDimMapped.log

  var attributeTypes = generateAttributeTypes;

  private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
    var attributeTypes = new Array[AttributeTypeInfo](6);
    attributeTypes(0) = new AttributeTypeInfo("id", 0, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
    attributeTypes(1) = new AttributeTypeInfo("code1", 1, AttributeTypeInfo.TypeCategory.FLOAT, -1, -1, 0)
    attributeTypes(2) = new AttributeTypeInfo("code2", 2, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
    attributeTypes(3) = new AttributeTypeInfo("code3", 3, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
    attributeTypes(4) = new AttributeTypeInfo("code4", 4, AttributeTypeInfo.TypeCategory.CHAR, -1, -1, 0)
    attributeTypes(5) = new AttributeTypeInfo("code5", 5, AttributeTypeInfo.TypeCategory.BOOLEAN, -1, -1, 0)

    return attributeTypes
  }

  var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;

  if (other != null && other != this) {
    // call copying fields from other to local variables
    fromFunc(other)
  }

  override def save: Unit = { IdCodeDimMapped.saveOne(this) }

  def Clone(): ContainerOrConcept = { IdCodeDimMapped.build(this) }

  override def getPartitionKey: Array[String] = {
    var partitionKeys: scala.collection.mutable.ArrayBuffer[String] = scala.collection.mutable.ArrayBuffer[String]();
    try {
      partitionKeys += com.ligadata.BaseTypes.IntImpl.toString(get(caseSensitiveKey("id")).asInstanceOf[java.lang.Integer]);
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };
    partitionKeys.toArray;

  }

  override def getPrimaryKey: Array[String] = {
    var primaryKeys: scala.collection.mutable.ArrayBuffer[String] = scala.collection.mutable.ArrayBuffer[String]();
    try {
      primaryKeys += com.ligadata.BaseTypes.IntImpl.toString(get(caseSensitiveKey("id")).asInstanceOf[java.lang.Integer]);
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };
    primaryKeys.toArray;

  }

  override def getAttributeType(name: String): AttributeTypeInfo = {
    if (name == null || name.trim() == "") return null;
    attributeTypes.foreach(attributeType => {
      if (attributeType.getName == caseSensitiveKey(name))
        return attributeType
    })
    return null;
  }

  var valuesMap = scala.collection.mutable.Map[String, AttributeValue]()

  override def getAttributeTypes(): Array[AttributeTypeInfo] = {
    val attributeTyps = valuesMap.map(f => f._2.getValueType).toArray;
    if (attributeTyps == null) return null else return attributeTyps
  }

  override def get(keyName: String): AnyRef = { // Return (value)
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = caseSensitiveKey(keyName);
    try {
      val value = valuesMap(key).getValue
      if (value == null) return null; else return value.asInstanceOf[AnyRef];
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }
  }

  override def getOrElse(keyName: String, defaultVal: Any): AnyRef = { // Return (value)
    var attributeValue: AttributeValue = new AttributeValue();
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = caseSensitiveKey(keyName);
    try {
      if (valuesMap.contains(key)) return get(key)
      else {
        if (defaultVal == null) return null;
        return defaultVal.asInstanceOf[AnyRef];
      }
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }
  }

  override def getAttributeNames(): Array[String] = {
    try {
      if (valuesMap.isEmpty) {
        return null;
      } else {
        return valuesMap.keySet.toArray;
      }
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }
  }

  override def get(index: Int): AnyRef = { // Return (value)
    throw new Exception("Get By Index is not supported in mapped messages");
  }

  override def getOrElse(index: Int, defaultVal: Any): AnyRef = { // Return (value,  type)
    throw new Exception("Get By Index is not supported in mapped messages");
  }

  override def getAllAttributeValues(): Array[AttributeValue] = { // Has (name, value, type))
    return valuesMap.map(f => f._2).toArray;
  }

  override def set(keyName: String, value: Any) = {
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = caseSensitiveKey(keyName);
    try {
      if (keyTypes.contains(key)) {
        valuesMap(key) = new AttributeValue(value, keyTypes(key))
      } else {
        valuesMap(key) = new AttributeValue(ValueToString(value), new AttributeTypeInfo(key, -1, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0))
      }
      if (getTimePartitionInfo != null && getTimePartitionInfo.getFieldName != null && getTimePartitionInfo.getFieldName.trim().size > 0 && getTimePartitionInfo.getFieldName.equalsIgnoreCase(key)) {
        setTimePartitionData;
      }
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }
  }

  override def set(keyName: String, value: Any, valTyp: String) = {
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = caseSensitiveKey(keyName);
    try {
      if (keyTypes.contains(key)) {
        valuesMap(key) = new AttributeValue(value, keyTypes(key))
      } else {
        val typeCategory = AttributeTypeInfo.TypeCategory.valueOf(valTyp.toUpperCase())
        val keytypeId = typeCategory.getValue.toShort
        val valtypeId = typeCategory.getValue.toShort
        valuesMap(key) = new AttributeValue(value, new AttributeTypeInfo(key, -1, typeCategory, valtypeId, keytypeId, 0))
      }
      if (getTimePartitionInfo != null && getTimePartitionInfo.getFieldName != null && getTimePartitionInfo.getFieldName.trim().size > 0 && getTimePartitionInfo.getFieldName.equalsIgnoreCase(key)) {
        setTimePartitionData;
      }
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }
  }

  override def set(index: Int, value: Any) = {
    throw new Exception("Set By Index is not supported in mapped messages");
  }

  private def ValueToString(v: Any): String = {
    if (v == null) {
      return "null"
    }
    if (v.isInstanceOf[Set[_]]) {
      return v.asInstanceOf[Set[_]].mkString(",")
    }
    if (v.isInstanceOf[List[_]]) {
      return v.asInstanceOf[List[_]].mkString(",")
    }
    if (v.isInstanceOf[Array[_]]) {
      return v.asInstanceOf[Array[_]].mkString(",")
    }
    v.toString
  }

  private def fromFunc(other: IdCodeDimMapped): IdCodeDimMapped = {

    if (other.valuesMap != null) {
      other.valuesMap.foreach(vMap => {
        val key = caseSensitiveKey(vMap._1)
        val attribVal = vMap._2
        val valType = attribVal.getValueType.getTypeCategory.getValue
        if (attribVal.getValue != null && attribVal.getValueType != null) {
          var attributeValue: AttributeValue = null
          valType match {
            case 1 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.StringImpl.Clone(attribVal.getValue.asInstanceOf[String]), attribVal.getValueType) }
            case 0 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.IntImpl.Clone(attribVal.getValue.asInstanceOf[java.lang.Integer]), attribVal.getValueType) }
            case 2 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.FloatImpl.Clone(attribVal.getValue.asInstanceOf[java.lang.Float]), attribVal.getValueType) }
            case 3 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.DoubleImpl.Clone(attribVal.getValue.asInstanceOf[java.lang.Double]), attribVal.getValueType) }
            case 7 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.BoolImpl.Clone(attribVal.getValue.asInstanceOf[java.lang.Boolean]), attribVal.getValueType) }
            case 4 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.LongImpl.Clone(attribVal.getValue.asInstanceOf[java.lang.Long]), attribVal.getValueType) }
            case 6 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.CharImpl.Clone(attribVal.getValue.asInstanceOf[java.lang.Character]), attribVal.getValueType) }
            case _ => {} // do nothhing
          }
          valuesMap.put(key, attributeValue);
        };
      })
    }

    this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
    return this;
  }

  def withid(value: java.lang.Integer): IdCodeDimMapped = {
    valuesMap("id") = new AttributeValue(value, keyTypes("id"))
    return this
  }
  def withcode1(value: java.lang.Float): IdCodeDimMapped = {
    valuesMap("code1") = new AttributeValue(value, keyTypes("code1"))
    return this
  }
  def withcode2(value: java.lang.Double): IdCodeDimMapped = {
    valuesMap("code2") = new AttributeValue(value, keyTypes("code2"))
    return this
  }
  def withcode3(value: java.lang.Long): IdCodeDimMapped = {
    valuesMap("code3") = new AttributeValue(value, keyTypes("code3"))
    return this
  }
  def withcode4(value: java.lang.Character): IdCodeDimMapped = {
    valuesMap("code4") = new AttributeValue(value, keyTypes("code4"))
    return this
  }
  def withcode5(value: Boolean): IdCodeDimMapped = {
    valuesMap("code5") = new AttributeValue(value, keyTypes("code5"))
    return this
  }
  def isCaseSensitive(): Boolean = IdCodeDimMapped.isCaseSensitive();
  def caseSensitiveKey(keyName: String): String = {
    if (isCaseSensitive)
      return keyName;
    else return keyName.toLowerCase;
  }

  def this(factory: ContainerFactoryInterface) = {
    this(factory, null)
  }

  def this(other: IdCodeDimMapped) = {
    this(other.getFactory.asInstanceOf[ContainerFactoryInterface], other)
  }

}