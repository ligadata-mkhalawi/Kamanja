
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

object ComponentKeyMetrics extends RDDObject[ComponentKeyMetrics] with ContainerFactoryInterface {

  val log = LogManager.getLogger(getClass)
  type T = ComponentKeyMetrics;
  override def getFullTypeName: String = "com.ligadata.KamanjaBase.ComponentKeyMetrics";
  override def getTypeNameSpace: String = "com.ligadata.KamanjaBase";
  override def getTypeName: String = "ComponentKeyMetrics";
  override def getTypeVersion: String = "000000.000000.000001";
  override def getSchemaId: Int = 7;
  override def getTenantId: String = "system";
  override def createInstance: ComponentKeyMetrics = new ComponentKeyMetrics(ComponentKeyMetrics);
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

  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanjabase" , "name" : "componentkeymetrics" , "fields":[{ "name" : "key" , "type" : "string"},{ "name" : "metricstime" , "type" : "long"},{ "name" : "roundintervaltimeinsec" , "type" : "int"},{ "name" : "firstoccured" , "type" : "long"},{ "name" : "lastoccured" , "type" : "long"},{ "name" : "metricsvalue" ,"type": {"type": "array", "items": { "type": "record",  "namespace" : "com.ligadata.kamanjabase" , "name" : "metricsvalue" , "fields":[{ "name" : "metrickey" , "type" : "string"},{ "name" : "metricsvalue" , "type" : "long"}]}}}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.KamanjaBase.ComponentKeyMetrics => { return convertToVer1(oldVerobj); }
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

  private def convertToVer1(oldVerobj: com.ligadata.KamanjaBase.ComponentKeyMetrics): com.ligadata.KamanjaBase.ComponentKeyMetrics = {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj ComponentKeyMetrics") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj ComponentKeyMetrics");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj ComponentKeyMetrics");
  override def NeedToTransformData: Boolean = false
}

class ComponentKeyMetrics(factory: ContainerFactoryInterface, other: ComponentKeyMetrics) extends ContainerInterface(factory) {

  val log = ComponentKeyMetrics.log

  var attributeTypes = generateAttributeTypes;

  private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
    var attributeTypes = new Array[AttributeTypeInfo](6);
    attributeTypes(0) = new AttributeTypeInfo("key", 0, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(1) = new AttributeTypeInfo("metricstime", 1, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
    attributeTypes(2) = new AttributeTypeInfo("roundintervaltimeinsec", 2, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
    attributeTypes(3) = new AttributeTypeInfo("firstoccured", 3, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
    attributeTypes(4) = new AttributeTypeInfo("lastoccured", 4, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
    attributeTypes(5) = new AttributeTypeInfo("metricsvalue", 5, AttributeTypeInfo.TypeCategory.ARRAY, 1001, -1, 2000001)

    return attributeTypes
  }

  var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;

  if (other != null && other != this) {
    // call copying fields from other to local variables
    fromFunc(other)
  }

  override def save: Unit = { ComponentKeyMetrics.saveOne(this) }

  def Clone(): ContainerOrConcept = { ComponentKeyMetrics.build(this) }

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

  var key: String = _;
  var metricstime: Long = _;
  var roundintervaltimeinsec: Int = _;
  var firstoccured: Long = _;
  var lastoccured: Long = _;
  var metricsvalue: scala.Array[com.ligadata.KamanjaBase.MetricsValue] = _;

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
    val fieldX = ru.typeOf[ComponentKeyMetrics].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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

    if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container ComponentKeyMetrics", null);
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
        case 0 => return this.key.asInstanceOf[AnyRef];
        case 1 => return this.metricstime.asInstanceOf[AnyRef];
        case 2 => return this.roundintervaltimeinsec.asInstanceOf[AnyRef];
        case 3 => return this.firstoccured.asInstanceOf[AnyRef];
        case 4 => return this.lastoccured.asInstanceOf[AnyRef];
        case 5 => return this.metricsvalue.asInstanceOf[AnyRef];

        case _ => throw new Exception(s"$index is a bad index for message ComponentKeyMetrics");
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
    var attributeVals = new Array[AttributeValue](6);
    try {
      attributeVals(0) = new AttributeValue(this.key, keyTypes("key"))
      attributeVals(1) = new AttributeValue(this.metricstime, keyTypes("metricstime"))
      attributeVals(2) = new AttributeValue(this.roundintervaltimeinsec, keyTypes("roundintervaltimeinsec"))
      attributeVals(3) = new AttributeValue(this.firstoccured, keyTypes("firstoccured"))
      attributeVals(4) = new AttributeValue(this.lastoccured, keyTypes("lastoccured"))
      attributeVals(5) = new AttributeValue(this.metricsvalue, keyTypes("metricsvalue"))

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

      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message ComponentKeyMetrics", null)
      set(keyTypes(key).getIndex, value);

    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

  }

  def set(index: Int, value: Any): Unit = {
    if (value == null) throw new Exception(s"Value is null for index $index in message ComponentKeyMetrics ")
    try {
      index match {
        case 0 => {
          if (value.isInstanceOf[String])
            this.key = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field key in message ComponentKeyMetrics")
        }
        case 1 => {
          if (value.isInstanceOf[Long])
            this.metricstime = value.asInstanceOf[Long];
          else throw new Exception(s"Value is the not the correct type for field metricstime in message ComponentKeyMetrics")
        }
        case 2 => {
          if (value.isInstanceOf[Int])
            this.roundintervaltimeinsec = value.asInstanceOf[Int];
          else throw new Exception(s"Value is the not the correct type for field roundintervaltimeinsec in message ComponentKeyMetrics")
        }
        case 3 => {
          if (value.isInstanceOf[Long])
            this.firstoccured = value.asInstanceOf[Long];
          else throw new Exception(s"Value is the not the correct type for field firstoccured in message ComponentKeyMetrics")
        }
        case 4 => {
          if (value.isInstanceOf[Long])
            this.lastoccured = value.asInstanceOf[Long];
          else throw new Exception(s"Value is the not the correct type for field lastoccured in message ComponentKeyMetrics")
        }
        case 5 => {
          if (value.isInstanceOf[scala.Array[com.ligadata.KamanjaBase.MetricsValue]])
            this.metricsvalue = value.asInstanceOf[scala.Array[com.ligadata.KamanjaBase.MetricsValue]];
          else if (value.isInstanceOf[scala.Array[ContainerInterface]])
            this.metricsvalue = value.asInstanceOf[scala.Array[ContainerInterface]].map(v => v.asInstanceOf[com.ligadata.KamanjaBase.MetricsValue]);
          else throw new Exception(s"Value is the not the correct type for field metricsvalue in message ComponentKeyMetrics")
        }

        case _ => throw new Exception(s"$index is a bad index for message ComponentKeyMetrics");
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

  private def fromFunc(other: ComponentKeyMetrics): ComponentKeyMetrics = {
    this.key = com.ligadata.BaseTypes.StringImpl.Clone(other.key);
    this.metricstime = com.ligadata.BaseTypes.LongImpl.Clone(other.metricstime);
    this.roundintervaltimeinsec = com.ligadata.BaseTypes.IntImpl.Clone(other.roundintervaltimeinsec);
    this.firstoccured = com.ligadata.BaseTypes.LongImpl.Clone(other.firstoccured);
    this.lastoccured = com.ligadata.BaseTypes.LongImpl.Clone(other.lastoccured);
    if (other.metricsvalue != null) {
      metricsvalue = new scala.Array[com.ligadata.KamanjaBase.MetricsValue](other.metricsvalue.length)
      metricsvalue = other.metricsvalue.map(f => f.Clone.asInstanceOf[com.ligadata.KamanjaBase.MetricsValue]);
    } else metricsvalue = null;

    this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
    return this;
  }

  def withkey(value: String): ComponentKeyMetrics = {
    this.key = value
    return this
  }
  def withmetricstime(value: Long): ComponentKeyMetrics = {
    this.metricstime = value
    return this
  }
  def withroundintervaltimeinsec(value: Int): ComponentKeyMetrics = {
    this.roundintervaltimeinsec = value
    return this
  }
  def withfirstoccured(value: Long): ComponentKeyMetrics = {
    this.firstoccured = value
    return this
  }
  def withlastoccured(value: Long): ComponentKeyMetrics = {
    this.lastoccured = value
    return this
  }
  def withmetricsvalue(value: scala.Array[com.ligadata.KamanjaBase.MetricsValue]): ComponentKeyMetrics = {
    this.metricsvalue = value
    return this
  }
  def isCaseSensitive(): Boolean = ComponentKeyMetrics.isCaseSensitive();
  def caseSensitiveKey(keyName: String): String = {
    if (isCaseSensitive)
      return keyName;
    else return keyName.toLowerCase;
  }

  def this(factory: ContainerFactoryInterface) = {
    this(factory, null)
  }

  def this(other: ComponentKeyMetrics) = {
    this(other.getFactory.asInstanceOf[ContainerFactoryInterface], other)
  }

}