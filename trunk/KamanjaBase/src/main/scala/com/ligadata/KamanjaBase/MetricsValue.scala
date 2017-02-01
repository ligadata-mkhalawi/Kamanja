
package com.ligadata.KamanjaBase;

import org.json4s.jackson.JsonMethods._;
import org.json4s.DefaultFormats;
import org.json4s.Formats;
import com.ligadata.KamanjaBase._;
import com.ligadata.BaseTypes._;
import com.ligadata.Exceptions._;
import org.apache.logging.log4j.{ Logger, LogManager }
import java.util.Date;
import java.io.{ DataInputStream, DataOutputStream, ByteArrayOutputStream }

object MetricsValue extends RDDObject[MetricsValue] with ContainerFactoryInterface {

  val log = LogManager.getLogger(getClass)
  type T = MetricsValue;
  override def getFullTypeName: String = "com.ligadata.KamanjaBase.MetricsValue";
  override def getTypeNameSpace: String = "com.ligadata.KamanjaBase";
  override def getTypeName: String = "MetricsValue";
  override def getTypeVersion: String = "000000.000000.000001";
  override def getSchemaId: Int = 6;
  override def getTenantId: String = "system";
  override def createInstance: MetricsValue = new MetricsValue(MetricsValue);
  override def isFixed: Boolean = true;
  def isCaseSensitive(): Boolean = false;
  override def getContainerType: ContainerTypes.ContainerType = ContainerTypes.ContainerType.CONTAINER
  override def getFullName = getFullTypeName;
  override def getRddTenantId = getTenantId;
  override def toJavaRDDObject: JavaRDDObject[T] = JavaRDDObject.fromRDDObject[T](this);

  def build = new T(this)
  def build(from: T) = new T(from)
  override def getPartitionKeyNames: Array[String] = Array[String]();

  override def getPrimaryKeyNames: Array[String] = Array[String]();

  override def getTimePartitionInfo: TimePartitionInfo = { return null; } // FieldName, Format & Time Partition Types(Daily/Monthly/Yearly)

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

  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanjabase" , "name" : "metricsvalue" , "fields":[{ "name" : "metrickey" , "type" : "string"},{ "name" : "metricskeyvalue" , "type" : "long"}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.KamanjaBase.MetricsValue => { return convertToVer1(oldVerobj); }
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

  private def convertToVer1(oldVerobj: com.ligadata.KamanjaBase.MetricsValue): com.ligadata.KamanjaBase.MetricsValue = {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj MetricsValue") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj MetricsValue");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj MetricsValue");
  override def NeedToTransformData: Boolean = false
}

class MetricsValue(factory: ContainerFactoryInterface, other: MetricsValue) extends ContainerInterface(factory) {

  val log = MetricsValue.log

  var attributeTypes = generateAttributeTypes;

  private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
    var attributeTypes = new Array[AttributeTypeInfo](2);
    attributeTypes(0) = new AttributeTypeInfo("metrickey", 0, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(1) = new AttributeTypeInfo("metricskeyvalue", 1, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)

    return attributeTypes
  }

  var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;

  if (other != null && other != this) {
    // call copying fields from other to local variables
    fromFunc(other)
  }

  override def save: Unit = { MetricsValue.saveOne(this) }

  def Clone(): ContainerOrConcept = { MetricsValue.build(this) }

  override def getPartitionKey: Array[String] = Array[String]()

  override def getPrimaryKey: Array[String] = Array[String]()

  override def getAttributeType(name: String): AttributeTypeInfo = {
    if (name == null || name.trim() == "") return null;
    attributeTypes.foreach(attributeType => {
      if (attributeType.getName == caseSensitiveKey(name))
        return attributeType
    })
    return null;
  }

  var metrickey: String = _;
  var metricskeyvalue: Long = _;

  override def getAttributeTypes(): Array[AttributeTypeInfo] = {
    if (attributeTypes == null) return null;
    return attributeTypes
  }

  private def getWithReflection(keyName: String): AnyRef = {
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = caseSensitiveKey(keyName);
    val ru = scala.reflect.runtime.universe
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(this)
    val fieldX = ru.typeOf[MetricsValue].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
    val fmX = im.reflectField(fieldX)
    return fmX.get.asInstanceOf[AnyRef];
  }

  override def get(key: String): AnyRef = {
    try {
      // Try with reflection
      return getByName(caseSensitiveKey(key))
    } catch {
      case e: Exception => {
        val stackTrace = StackTrace.ThrowableTraceString(e)
        log.debug("StackTrace:" + stackTrace)
        // Call By Name
        return getWithReflection(caseSensitiveKey(key))
      }
    }
  }

  private def getByName(keyName: String): AnyRef = {
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = caseSensitiveKey(keyName);

    if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container MetricsValue", null);
    return get(keyTypes(key).getIndex)
  }

  override def getOrElse(keyName: String, defaultVal: Any): AnyRef = { // Return (value)
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = caseSensitiveKey(keyName);
    try {
      return get(key)
    } catch {
      case e: Exception => {
        log.debug("", e)
        if (defaultVal == null) return null;
        return defaultVal.asInstanceOf[AnyRef];
      }
    }
    return null;
  }

  override def get(index: Int): AnyRef = { // Return (value, type)
    try {
      index match {
        case 0 => return this.metrickey.asInstanceOf[AnyRef];
        case 1 => return this.metricskeyvalue.asInstanceOf[AnyRef];

        case _ => throw new Exception(s"$index is a bad index for message MetricsValue");
      }
    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

  }

  override def getOrElse(index: Int, defaultVal: Any): AnyRef = { // Return (value)
    try {
      return get(index);
    } catch {
      case e: Exception => {
        log.debug("", e)
        if (defaultVal == null) return null;
        return defaultVal.asInstanceOf[AnyRef];
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
    var attributeVals = new Array[AttributeValue](2);
    try {
      attributeVals(0) = new AttributeValue(this.metrickey, keyTypes("metrickey"))
      attributeVals(1) = new AttributeValue(this.metricskeyvalue, keyTypes("metricskeyvalue"))

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
    val key = caseSensitiveKey(keyName);
    try {

      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message MetricsValue", null)
      set(keyTypes(key).getIndex, value);

    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

  }

  def set(index: Int, value: Any): Unit = {
    if (value == null) throw new Exception(s"Value is null for index $index in message MetricsValue ")
    try {
      index match {
        case 0 => {
          if (value.isInstanceOf[String])
            this.metrickey = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field metrickey in message MetricsValue")
        }
        case 1 => {
          if (value.isInstanceOf[Long])
            this.metricskeyvalue = value.asInstanceOf[Long];
          else throw new Exception(s"Value is the not the correct type for field metricskeyvalue in message MetricsValue")
        }

        case _ => throw new Exception(s"$index is a bad index for message MetricsValue");
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

  private def fromFunc(other: MetricsValue): MetricsValue = {
    this.metrickey = com.ligadata.BaseTypes.StringImpl.Clone(other.metrickey);
    this.metricskeyvalue = com.ligadata.BaseTypes.LongImpl.Clone(other.metricskeyvalue);

    this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
    return this;
  }

  def withmetrickey(value: String): MetricsValue = {
    this.metrickey = value
    return this
  }
  def withmetricskeyvalue(value: Long): MetricsValue = {
    this.metricskeyvalue = value
    return this
  }
  def isCaseSensitive(): Boolean = MetricsValue.isCaseSensitive();
  def caseSensitiveKey(keyName: String): String = {
    if (isCaseSensitive)
      return keyName;
    else return keyName.toLowerCase;
  }

  def this(factory: ContainerFactoryInterface) = {
    this(factory, null)
  }

  def this(other: MetricsValue) = {
    this(other.getFactory.asInstanceOf[ContainerFactoryInterface], other)
  }

}