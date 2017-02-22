
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

object KamanjaVelocityMetrics extends RDDObject[KamanjaVelocityMetrics] with MessageFactoryInterface {

  val log = LogManager.getLogger(getClass)
  type T = KamanjaVelocityMetrics;
  override def getFullTypeName: String = "com.ligadata.KamanjaBase.KamanjaVelocityMetrics";
  override def getTypeNameSpace: String = "com.ligadata.KamanjaBase";
  override def getTypeName: String = "KamanjaVelocityMetrics";
  override def getTypeVersion: String = "000000.000000.000001";
  override def getSchemaId: Int = 1000006;
  override def getTenantId: String = "system";
  override def createInstance: KamanjaVelocityMetrics = new KamanjaVelocityMetrics(KamanjaVelocityMetrics);
  override def isFixed: Boolean = true;
  def isCaseSensitive(): Boolean = false;
  override def getContainerType: ContainerTypes.ContainerType = ContainerTypes.ContainerType.MESSAGE
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

  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanjabase" , "name" : "kamanjavelocitymetrics" , "fields":[{ "name" : "uuid" , "type" : "string"},{ "name" : "componentkey" , "type" : "string"},{ "name" : "nodeid" , "type" : "string"},{ "name" : "componentkeymetrics" ,"type": {"type": "array", "items": { "type": "record",  "namespace" : "com.ligadata.kamanjabase" , "name" : "componentkeymetrics" , "fields":[{ "name" : "key" , "type" : "string"},{ "name" : "metricstime" , "type" : "long"},{ "name" : "roundintervaltimeinsec" , "type" : "int"},{ "name" : "firstoccured" , "type" : "long"},{ "name" : "lastoccured" , "type" : "long"},{ "name" : "metricsvalue" ,"type": {"type": "array", "items": { "type": "record",  "namespace" : "com.ligadata.kamanjabase" , "name" : "metricsvalue" , "fields":[{ "name" : "metrickey" , "type" : "string"},{ "name" : "metricsvalue" , "type" : "long"}]}}}]}}}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.KamanjaBase.KamanjaVelocityMetrics => { return convertToVer1(oldVerobj); }
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

  private def convertToVer1(oldVerobj: com.ligadata.KamanjaBase.KamanjaVelocityMetrics): com.ligadata.KamanjaBase.KamanjaVelocityMetrics = {
    return oldVerobj
  }

  /****   DEPRECATED METHODS ***/
  override def FullName: String = getFullTypeName
  override def NameSpace: String = getTypeNameSpace
  override def Name: String = getTypeName
  override def Version: String = getTypeVersion
  override def CreateNewMessage: BaseMsg = createInstance.asInstanceOf[BaseMsg];
  override def CreateNewContainer: BaseContainer = null;
  override def IsFixed: Boolean = true
  override def IsKv: Boolean = false
  override def CanPersist: Boolean = false
  override def isMessage: Boolean = true
  override def isContainer: Boolean = false
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj KamanjaVelocityMetrics") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj KamanjaVelocityMetrics");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj KamanjaVelocityMetrics");
  override def NeedToTransformData: Boolean = false
}

class KamanjaVelocityMetrics(factory: MessageFactoryInterface, other: KamanjaVelocityMetrics) extends MessageInterface(factory) {

  val log = KamanjaVelocityMetrics.log

  var attributeTypes = generateAttributeTypes;

  private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
    var attributeTypes = new Array[AttributeTypeInfo](4);
    attributeTypes(0) = new AttributeTypeInfo("uuid", 0, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(1) = new AttributeTypeInfo("componentkey", 1, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(2) = new AttributeTypeInfo("nodeid", 2, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(3) = new AttributeTypeInfo("componentkeymetrics", 3, AttributeTypeInfo.TypeCategory.ARRAY, 1001, -1, 2000002)

    return attributeTypes
  }

  var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;

  if (other != null && other != this) {
    // call copying fields from other to local variables
    fromFunc(other)
  }

  override def save: Unit = { KamanjaVelocityMetrics.saveOne(this) }

  def Clone(): ContainerOrConcept = { KamanjaVelocityMetrics.build(this) }

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

  var uuid: String = _;
  var componentkey: String = _;
  var nodeid: String = _;
  var componentkeymetrics: scala.Array[com.ligadata.KamanjaBase.ComponentKeyMetrics] = _;

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
    val fieldX = ru.typeOf[KamanjaVelocityMetrics].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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

    if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container KamanjaVelocityMetrics", null);
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
        case 0 => return this.uuid.asInstanceOf[AnyRef];
        case 1 => return this.componentkey.asInstanceOf[AnyRef];
        case 2 => return this.nodeid.asInstanceOf[AnyRef];
        case 3 => return this.componentkeymetrics.asInstanceOf[AnyRef];

        case _ => throw new Exception(s"$index is a bad index for message KamanjaVelocityMetrics");
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
    var attributeVals = new Array[AttributeValue](4);
    try {
      attributeVals(0) = new AttributeValue(this.uuid, keyTypes("uuid"))
      attributeVals(1) = new AttributeValue(this.componentkey, keyTypes("componentkey"))
      attributeVals(2) = new AttributeValue(this.nodeid, keyTypes("nodeid"))
      attributeVals(3) = new AttributeValue(this.componentkeymetrics, keyTypes("componentkeymetrics"))

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

      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message KamanjaVelocityMetrics", null)
      set(keyTypes(key).getIndex, value);

    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

  }

  def set(index: Int, value: Any): Unit = {
    if (value == null) throw new Exception(s"Value is null for index $index in message KamanjaVelocityMetrics ")
    try {
      index match {
        case 0 => {
          if (value.isInstanceOf[String])
            this.uuid = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field uuid in message KamanjaVelocityMetrics")
        }
        case 1 => {
          if (value.isInstanceOf[String])
            this.componentkey = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field componentkey in message KamanjaVelocityMetrics")
        }
        case 2 => {
          if (value.isInstanceOf[String])
            this.nodeid = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field nodeid in message KamanjaVelocityMetrics")
        }
        case 3 => {
          if (value.isInstanceOf[scala.Array[com.ligadata.KamanjaBase.ComponentKeyMetrics]])
            this.componentkeymetrics = value.asInstanceOf[scala.Array[com.ligadata.KamanjaBase.ComponentKeyMetrics]];
          else if (value.isInstanceOf[scala.Array[ContainerInterface]])
            this.componentkeymetrics = value.asInstanceOf[scala.Array[ContainerInterface]].map(v => v.asInstanceOf[com.ligadata.KamanjaBase.ComponentKeyMetrics]);
          else throw new Exception(s"Value is the not the correct type for field componentkeymetrics in message KamanjaVelocityMetrics")
        }

        case _ => throw new Exception(s"$index is a bad index for message KamanjaVelocityMetrics");
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

  private def fromFunc(other: KamanjaVelocityMetrics): KamanjaVelocityMetrics = {
    this.uuid = com.ligadata.BaseTypes.StringImpl.Clone(other.uuid);
    this.componentkey = com.ligadata.BaseTypes.StringImpl.Clone(other.componentkey);
    this.nodeid = com.ligadata.BaseTypes.StringImpl.Clone(other.nodeid);
    if (other.componentkeymetrics != null) {
      componentkeymetrics = new scala.Array[com.ligadata.KamanjaBase.ComponentKeyMetrics](other.componentkeymetrics.length)
      componentkeymetrics = other.componentkeymetrics.map(f => f.Clone.asInstanceOf[com.ligadata.KamanjaBase.ComponentKeyMetrics]);
    } else componentkeymetrics = null;

    this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
    return this;
  }

  def withuuid(value: String): KamanjaVelocityMetrics = {
    this.uuid = value
    return this
  }
  def withcomponentkey(value: String): KamanjaVelocityMetrics = {
    this.componentkey = value
    return this
  }
  def withnodeid(value: String): KamanjaVelocityMetrics = {
    this.nodeid = value
    return this
  }
  def withcomponentkeymetrics(value: scala.Array[com.ligadata.KamanjaBase.ComponentKeyMetrics]): KamanjaVelocityMetrics = {
    this.componentkeymetrics = value
    return this
  }
  def isCaseSensitive(): Boolean = KamanjaVelocityMetrics.isCaseSensitive();
  def caseSensitiveKey(keyName: String): String = {
    if (isCaseSensitive)
      return keyName;
    else return keyName.toLowerCase;
  }

  def this(factory: MessageFactoryInterface) = {
    this(factory, null)
  }

  def this(other: KamanjaVelocityMetrics) = {
    this(other.getFactory.asInstanceOf[MessageFactoryInterface], other)
  }

}