
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

object IdCodeDimFixedTest extends RDDObject[IdCodeDimFixedTest] with ContainerFactoryInterface {

  val log = LogManager.getLogger(getClass)
  type T = IdCodeDimFixedTest;
  override def getFullTypeName: String = "com.ligadata.messages.IdCodeDimFixedTest";
  override def getTypeNameSpace: String = "com.ligadata.messages";
  override def getTypeName: String = "IdCodeDimFixedTest";
  override def getTypeVersion: String = "000001.000000.000000";
  override def getSchemaId: Int = 0;
  override def getTenantId: String = "";
  override def createInstance: IdCodeDimFixedTest = new IdCodeDimFixedTest(IdCodeDimFixedTest);
  override def isFixed: Boolean = true;
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

  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "idcodedimfixedtest" , "fields":[{ "name" : "id" , "type" : "int"},{ "name" : "code" , "type" : "int"},{ "name" : "description" , "type" : "string"},{ "name" : "codes" , "type" : {"type" : "map", "values" : "int"}}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.messages.V1000000000000.IdCodeDimFixedTest => { return convertToVer1000000000000(oldVerobj); }
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

  private def convertToVer1000000000000(oldVerobj: com.ligadata.messages.V1000000000000.IdCodeDimFixedTest): com.ligadata.messages.V1000000000000.IdCodeDimFixedTest = {
    return oldVerobj
  }

  /****   DEPRECATED METHODS ***/
  override def FullName: String = getFullTypeName
  override def NameSpace: String = getTypeNameSpace
  override def Name: String = getTypeName
  override def Version: String = getTypeVersion
  override def CreateNewMessage: BaseMsg = null;
  override def CreateNewContainer: BaseContainer = createInstance.asInstanceOf[BaseContainer];
  override def IsFixed: Boolean = true
  override def IsKv: Boolean = false
  override def CanPersist: Boolean = false
  override def isMessage: Boolean = false
  override def isContainer: Boolean = true
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj IdCodeDimFixedTest") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj IdCodeDimFixedTest");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj IdCodeDimFixedTest");
  override def NeedToTransformData: Boolean = false
}

class IdCodeDimFixedTest(factory: ContainerFactoryInterface, other: IdCodeDimFixedTest) extends ContainerInterface(factory) {

  val log = IdCodeDimFixedTest.log

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

  override def save: Unit = { /* IdCodeDimFixedTest.saveOne(this) */ }

  def Clone(): ContainerOrConcept = { IdCodeDimFixedTest.build(this) }

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

  var id: Int = _;
  var code: Int = _;
  var description: String = _;
  var codes: scala.collection.immutable.Map[String, Int] = _;

  override def getAttributeTypes(): Array[AttributeTypeInfo] = {
    if (attributeTypes == null) return null;
    return attributeTypes
  }

  private def getWithReflection(keyName: String): AnyRef = {
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = keyName.toLowerCase;
    val ru = scala.reflect.runtime.universe
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(this)
    val fieldX = ru.typeOf[IdCodeDimFixedTest].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
    val fmX = im.reflectField(fieldX)
    return fmX.get.asInstanceOf[AnyRef];
  }

  override def get(key: String): AnyRef = {
    try {
      // Try with reflection
      return getByName(key.toLowerCase())
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        // Call By Name
        return getWithReflection(key.toLowerCase())
      }
    }
  }

  private def getByName(keyName: String): AnyRef = {
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = keyName.toLowerCase;

    if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container IdCodeDimFixedTest", null);
    return get(keyTypes(key).getIndex)
  }

  override def getOrElse(keyName: String, defaultVal: Any): AnyRef = { // Return (value, type)
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = keyName.toLowerCase;
    try {
      val value = get(key.toLowerCase())
      if (value == null) return defaultVal.asInstanceOf[AnyRef]; else return value;
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }
    return null;
  }

  override def get(index: Int): AnyRef = { // Return (value, type)
    try {
      index match {
        case 0 => return this.id.asInstanceOf[AnyRef];
        case 1 => return this.code.asInstanceOf[AnyRef];
        case 2 => return this.description.asInstanceOf[AnyRef];
        case 3 => return this.codes.asInstanceOf[AnyRef];

        case _ => throw new Exception(s"$index is a bad index for message IdCodeDimFixedTest");
      }
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

  }

  override def getOrElse(index: Int, defaultVal: Any): AnyRef = { // Return (value,  type)
    try {
      val value = get(index)
      if (value == null) return defaultVal.asInstanceOf[AnyRef]; else return value;
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }
    return null;
  }

  override def getAttributeNames(): Array[String] = {
    try {
      if (keyTypes.isEmpty) {
        return null;
      } else {
        return keyTypes.keySet.toArray;
      }
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    }
    return null;
  }

  override def getAllAttributeValues(): Array[AttributeValue] = { // Has ( value, attributetypeinfo))
    var attributeVals = new Array[AttributeValue](4);
    try {
      attributeVals(0) = new AttributeValue(this.id, keyTypes("id"))
      attributeVals(1) = new AttributeValue(this.code, keyTypes("code"))
      attributeVals(2) = new AttributeValue(this.description, keyTypes("description"))
      attributeVals(3) = new AttributeValue(this.codes, keyTypes("codes"))

    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

    return attributeVals;
  }

 
  override def set(keyName: String, value: Any) = {
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = keyName.toLowerCase;
    try {

      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message IdCodeDimFixedTest", null)
      set(keyTypes(key).getIndex, value);

    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

  }

  def set(index: Int, value: Any): Unit = {
    if (value == null) throw new Exception(s"Value is null for index $index in message IdCodeDimFixedTest ")
    try {
      index match {
        case 0 => {
          if (value.isInstanceOf[Int]) {
            this.id = value.asInstanceOf[Int];
            setTimePartitionData;
          } else throw new Exception(s"Value is the not the correct type for field id in message IdCodeDimFixedTest")
        }
        case 1 => {
          if (value.isInstanceOf[Int])
            this.code = value.asInstanceOf[Int];
          else throw new Exception(s"Value is the not the correct type for field code in message IdCodeDimFixedTest")
        }
        case 2 => {
          if (value.isInstanceOf[String])
            this.description = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field description in message IdCodeDimFixedTest")
        }
        case 3 => {
          if (value.isInstanceOf[scala.collection.immutable.Map[String, Int]])
            this.codes = value.asInstanceOf[scala.collection.immutable.Map[String, Int]];
          else throw new Exception(s"Value is the not the correct type for field codes in message IdCodeDimFixedTest")
        }

        case _ => throw new Exception(s"$index is a bad index for message IdCodeDimFixedTest");
      }
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

  }

  override def set(key: String, value: Any, valTyp: String) = {
    throw new Exception("Set Func for Value and ValueType By Key is not supported for Fixed Messages")
  }

  private def fromFunc(other: IdCodeDimFixedTest): IdCodeDimFixedTest = {
    this.id = com.ligadata.BaseTypes.IntImpl.Clone(other.id);
    this.code = com.ligadata.BaseTypes.IntImpl.Clone(other.code);
    this.description = com.ligadata.BaseTypes.StringImpl.Clone(other.description);
    if (other.codes != null) {
      this.codes = other.codes.map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), com.ligadata.BaseTypes.IntImpl.Clone(a._2)) }.toMap
    } else this.codes = null;

    this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
    return this;
  }

  def withid(value: Int): IdCodeDimFixedTest = {
    this.id = value
    return this
  }
  def withcode(value: Int): IdCodeDimFixedTest = {
    this.code = value
    return this
  }
  def withdescription(value: String): IdCodeDimFixedTest = {
    this.description = value
    return this
  }
  def withcodes(value: scala.collection.immutable.Map[String, Int]): IdCodeDimFixedTest = {
    this.codes = value
    return this
  }

  def this(factory: ContainerFactoryInterface) = {
    this(factory, null)
  }

  def this(other: IdCodeDimFixedTest) = {
    this(other.getFactory.asInstanceOf[ContainerFactoryInterface], other)
  }

}