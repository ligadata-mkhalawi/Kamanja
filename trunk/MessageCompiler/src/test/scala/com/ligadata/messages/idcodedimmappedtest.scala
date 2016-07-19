
package com.ligadata.messages.V10000001000000;

import org.json4s.jackson.JsonMethods._;
import org.json4s.DefaultFormats;
import org.json4s.Formats;
import com.ligadata.KamanjaBase._;
import com.ligadata.BaseTypes._;
import com.ligadata.Exceptions.StackTrace;
import org.apache.logging.log4j.{ Logger, LogManager }
import java.util.Date;
import java.io.{ DataInputStream, DataOutputStream, ByteArrayOutputStream }

object IdCodeDimMappedTest extends RDDObject[IdCodeDimMappedTest] with ContainerFactoryInterface {

  val log = LogManager.getLogger(getClass)
  type T = IdCodeDimMappedTest;
  override def getFullTypeName: String = "com.ligadata.messages.IdCodeDimMappedTest";
  override def getTypeNameSpace: String = "com.ligadata.messages";
  override def getTypeName: String = "IdCodeDimMappedTest";
  override def getTypeVersion: String = "000010.000001.000000";
  override def getSchemaId: Int = 0;
  override def getTenantId: String = "";
  override def createInstance: IdCodeDimMappedTest = new IdCodeDimMappedTest(IdCodeDimMappedTest);
  override def isFixed: Boolean = false;
  override def getContainerType: ContainerTypes.ContainerType = ContainerTypes.ContainerType.CONTAINER
  override def getFullName = getFullTypeName;
  override def getRddTenantId = getTenantId;
  override def toJavaRDDObject: JavaRDDObject[T] = JavaRDDObject.fromRDDObject[T](this);

  def build = new T(this)
  def build(from: T) = new T(from)
  override def getPartitionKeyNames: Array[String] = Array("id");

  override def getPrimaryKeyNames: Array[String] = Array("id");

  // override def getTimePartitionInfo: TimePartitionInfo = { return null; } // FieldName, Format & Time Partition Types(Daily/Monthly/Yearly)

  override def getTimePartitionInfo: TimePartitionInfo = {
    var timePartitionInfo: TimePartitionInfo = new TimePartitionInfo();
    timePartitionInfo.setFieldName("id");
    timePartitionInfo.setFormat("epochtime");
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

  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "idcodedimmappedtest" , "fields":[{ "name" : "id" , "type" : "int"},{ "name" : "code" , "type" : "int"},{ "name" : "description" , "type" : "string"},{ "name" : "codes" , "type" : {"type" : "map", "values" : "int"}}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.messages.V10000001000000.IdCodeDimMappedTest => { return convertToVer10000001000000(oldVerobj); }
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

  private def convertToVer10000001000000(oldVerobj: com.ligadata.messages.V10000001000000.IdCodeDimMappedTest): com.ligadata.messages.V10000001000000.IdCodeDimMappedTest = {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj IdCodeDimMappedTest") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj IdCodeDimMappedTest");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj IdCodeDimMappedTest");
  override def NeedToTransformData: Boolean = false
}

class IdCodeDimMappedTest(factory: ContainerFactoryInterface, other: IdCodeDimMappedTest) extends ContainerInterface(factory) {

  val log = IdCodeDimMappedTest.log

  var attributeTypes = generateAttributeTypes;

  private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
    var attributeTypes = new Array[AttributeTypeInfo](4);
    attributeTypes(0) = new AttributeTypeInfo("id", 0, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
    attributeTypes(1) = new AttributeTypeInfo("code", 1, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
    attributeTypes(2) = new AttributeTypeInfo("description", 2, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(3) = new AttributeTypeInfo("codes", 3, AttributeTypeInfo.TypeCategory.MAP, 0, 1, 0)

    return attributeTypes
  }

  var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;

  if (other != null && other != this) {
    // call copying fields from other to local variables
    fromFunc(other)
  }

  override def save: Unit = { /* IdCodeDimMappedTest.saveOne(this) */ }

  def Clone(): ContainerOrConcept = { IdCodeDimMappedTest.build(this) }

  override def getPartitionKey: Array[String] = {
    var partitionKeys: scala.collection.mutable.ArrayBuffer[String] = scala.collection.mutable.ArrayBuffer[String]();
    try {
      partitionKeys += com.ligadata.BaseTypes.IntImpl.toString(get("id").asInstanceOf[Int]);
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
      primaryKeys += com.ligadata.BaseTypes.IntImpl.toString(get("id").asInstanceOf[Int]);
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
      if (attributeType.getName == name.toLowerCase())
        return attributeType
    })
    return null;
  }

  var valuesMap = scala.collection.mutable.Map[String, AttributeValue]()

  override def getAttributeTypes(): Array[AttributeTypeInfo] = {
    val attributeTyps = valuesMap.map(f => f._2.getValueType).toArray;
    if (attributeTyps == null) return null else return attributeTyps
  }

  override def get(keyName: String): AnyRef = { // Return (value, type)
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = keyName.toLowerCase;
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

  override def getOrElse(keyName: String, defaultVal: Any): AnyRef = { // Return (value, type)
    var attributeValue: AttributeValue = new AttributeValue();
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = keyName.toLowerCase;
    try {
      return valuesMap.getOrElse(key, defaultVal.asInstanceOf[AnyRef])
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

  override def get(index: Int): AnyRef = { // Return (value, type)
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
    val key = keyName.toLowerCase;
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
    val key = keyName.toLowerCase;
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

  private def fromFunc(other: IdCodeDimMappedTest): IdCodeDimMappedTest = {

    if (other.valuesMap != null) {
      other.valuesMap.foreach(vMap => {
        val key = vMap._1.toLowerCase
        val attribVal = vMap._2
        val valType = attribVal.getValueType.getTypeCategory.getValue
        if (attribVal.getValue != null && attribVal.getValueType != null) {
          var attributeValue: AttributeValue = null
          valType match {
            case 1 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.StringImpl.Clone(attribVal.getValue.asInstanceOf[String]), attribVal.getValueType) }
            case 0 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.IntImpl.Clone(attribVal.getValue.asInstanceOf[Int]), attribVal.getValueType) }
            case 2 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.FloatImpl.Clone(attribVal.getValue.asInstanceOf[Float]), attribVal.getValueType) }
            case 3 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.DoubleImpl.Clone(attribVal.getValue.asInstanceOf[Double]), attribVal.getValueType) }
            case 7 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.BoolImpl.Clone(attribVal.getValue.asInstanceOf[Boolean]), attribVal.getValueType) }
            case 4 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.LongImpl.Clone(attribVal.getValue.asInstanceOf[Long]), attribVal.getValueType) }
            case 6 => { attributeValue = new AttributeValue(com.ligadata.BaseTypes.CharImpl.Clone(attribVal.getValue.asInstanceOf[Char]), attribVal.getValueType) }
            case _ => {} // do nothhing
          }
          valuesMap.put(key, attributeValue);
        };
      })
    }

    {
      if (other.valuesMap.contains("codes")) {
        val codes = other.valuesMap("codes").getValue;
        if (codes == null) valuesMap.put("codes", null);
        else {
          if (codes.isInstanceOf[scala.collection.immutable.Map[String, Int]]) {
            val mapmap = codes.asInstanceOf[scala.collection.immutable.Map[String, Int]].map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), com.ligadata.BaseTypes.IntImpl.Clone(a._2)) }.toMap
            valuesMap.put("codes", new AttributeValue(mapmap, other.valuesMap("codes").getValueType));
          }
        }
      } else valuesMap.put("codes", null);
    }

    this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
    return this;
  }

  def withid(value: Int): IdCodeDimMappedTest = {
    valuesMap("id") = new AttributeValue(value, keyTypes("id"))
    return this
  }
  def withcode(value: Int): IdCodeDimMappedTest = {
    valuesMap("code") = new AttributeValue(value, keyTypes("code"))
    return this
  }
  def withdescription(value: String): IdCodeDimMappedTest = {
    valuesMap("description") = new AttributeValue(value, keyTypes("description"))
    return this
  }
  def withcodes(value: scala.collection.immutable.Map[String, Int]): IdCodeDimMappedTest = {
    valuesMap("codes") = new AttributeValue(value, keyTypes("codes"))
    return this
  }

  def this(factory: ContainerFactoryInterface) = {
    this(factory, null)
  }

  def this(other: IdCodeDimMappedTest) = {
    this(other.getFactory.asInstanceOf[ContainerFactoryInterface], other)
  }

}