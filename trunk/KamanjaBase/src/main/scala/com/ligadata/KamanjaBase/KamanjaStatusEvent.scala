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

object KamanjaStatusEvent extends RDDObject[KamanjaStatusEvent] with MessageFactoryInterface {

  val log = LogManager.getLogger(getClass)
  type T = KamanjaStatusEvent;
  override def getFullTypeName: String = "com.ligadata.KamanjaBase.KamanjaStatusEvent";
  override def getTypeNameSpace: String = "com.ligadata.KamanjaBase";
  override def getTypeName: String = "KamanjaStatusEvent";
  override def getTypeVersion: String = "000001.000002.000000";
  override def getSchemaId: Int = 1000001;
  override def getTenantId: String = "System";
  override def createInstance: KamanjaStatusEvent = new KamanjaStatusEvent(KamanjaStatusEvent);
  override def isFixed: Boolean = true;
  def isCaseSensitive: Boolean = false;
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

  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanjabase" , "name" : "KamanjaStatusEvent" , "fields":[{ "name" : "statustype" , "type" : "string"},{ "name" : "nodeid" , "type" : "string"},{ "name" : "eventtime" , "type" : "string"},{ "name" : "statusstring" , "type" : "string"}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.KamanjaBase.KamanjaStatusEvent => { return convertToVer1000002000000(oldVerobj); }
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

  private def convertToVer1000002000000(oldVerobj: com.ligadata.KamanjaBase.KamanjaStatusEvent): com.ligadata.KamanjaBase.KamanjaStatusEvent = {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj KamanjaStatusEvent") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj KamanjaStatusEvent");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj KamanjaStatusEvent");
  override def NeedToTransformData: Boolean = false
}

class KamanjaStatusEvent(factory: MessageFactoryInterface, other: KamanjaStatusEvent) extends MessageInterface(factory) {
  def isCaseSensitive: Boolean = KamanjaStatusEvent.isCaseSensitive;

  val log = KamanjaStatusEvent.log

  var attributeTypes = generateAttributeTypes;

  private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
    var attributeTypes = new Array[AttributeTypeInfo](4);
    attributeTypes(0) = new AttributeTypeInfo("statustype", 0, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(1) = new AttributeTypeInfo("nodeid", 1, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(2) = new AttributeTypeInfo("eventtime", 2, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(3) = new AttributeTypeInfo("statusstring", 3, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)

    return attributeTypes
  }

  var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;

  if (other != null && other != this) {
    // call copying fields from other to local variables
    fromFunc(other)
  }

  override def save: Unit = { /* KamanjaStatusEvent.saveOne(this) */ }

  def Clone(): ContainerOrConcept = { KamanjaStatusEvent.build(this) }

  override def getPartitionKey: Array[String] = Array[String]()

  override def getPrimaryKey: Array[String] = Array[String]()

  override def getAttributeType(name: String): AttributeTypeInfo = {
    if (name == null || name.trim() == "") return null;
    attributeTypes.foreach(attributeType => {
      if (attributeType.getName == name.toLowerCase())
        return attributeType
    })
    return null;
  }

  var statustype: String = _;
  var nodeid: String = _;
  var eventtime: String = _;
  var statusstring: String = _;

  override def getAttributeTypes(): Array[AttributeTypeInfo] = {
    if (attributeTypes == null) return null;
    return attributeTypes
  }

  private def getWithReflection(key: String): AnyRef = {
    val ru = scala.reflect.runtime.universe
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(this)
    val fieldX = ru.typeOf[KamanjaStatusEvent].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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

  private def getByName(key: String): AnyRef = {
    if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container KamanjaStatusEvent", null);
    return get(keyTypes(key).getIndex)
  }

  override def getOrElse(key: String, defaultVal: Any): AnyRef = { // Return (value, type)
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
        case 0 => return this.statustype.asInstanceOf[AnyRef];
        case 1 => return this.nodeid.asInstanceOf[AnyRef];
        case 2 => return this.eventtime.asInstanceOf[AnyRef];
        case 3 => return this.statusstring.asInstanceOf[AnyRef];

        case _ => throw new Exception(s"$index is a bad index for message KamanjaStatusEvent");
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
      attributeVals(0) = new AttributeValue(this.statustype, keyTypes("statustype"))
      attributeVals(1) = new AttributeValue(this.nodeid, keyTypes("nodeid"))
      attributeVals(2) = new AttributeValue(this.eventtime, keyTypes("eventtime"))
      attributeVals(3) = new AttributeValue(this.statusstring, keyTypes("statusstring"))

    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

    return attributeVals;
  }

  override def set(key: String, value: Any) = {
    try {

      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message KamanjaStatusEvent", null)
      set(keyTypes(key).getIndex, value);

    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

  }

  def set(index: Int, value: Any): Unit = {
    if (value == null) throw new Exception(s"Value is null for index $index in message KamanjaStatusEvent ")
    try {
      index match {
        case 0 => {
          if (value.isInstanceOf[String])
            this.statustype = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field statustype in message KamanjaStatusEvent")
        }
        case 1 => {
          if (value.isInstanceOf[String])
            this.nodeid = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field nodeid in message KamanjaStatusEvent")
        }
        case 2 => {
          if (value.isInstanceOf[Long])
            this.eventtime = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field eventtime in message KamanjaStatusEvent")
        }
        case 3 => {
          if (value.isInstanceOf[String])
            this.statusstring = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field statusstring in message KamanjaStatusEvent")
        }

        case _ => throw new Exception(s"$index is a bad index for message KamanjaStatusEvent");
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

  private def fromFunc(other: KamanjaStatusEvent): KamanjaStatusEvent = {
    this.statustype = com.ligadata.BaseTypes.StringImpl.Clone(other.statustype);
    this.nodeid = com.ligadata.BaseTypes.StringImpl.Clone(other.nodeid);
    this.eventtime = com.ligadata.BaseTypes.StringImpl.Clone(other.eventtime);
    this.statusstring = com.ligadata.BaseTypes.StringImpl.Clone(other.statusstring);

    this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
    return this;
  }

  def withstatustype(value: String): KamanjaStatusEvent = {
    this.statustype = value
    return this
  }
  def withnodeid(value: String): KamanjaStatusEvent = {
    this.nodeid = value
    return this
  }
  def witheventtime(value: String): KamanjaStatusEvent = {
    this.eventtime = value
    return this
  }
  def withstatusstring(value: String): KamanjaStatusEvent = {
    this.statusstring = value
    return this
  }

  def this(factory: MessageFactoryInterface) = {
    this(factory, null)
  }

  def this(other: KamanjaStatusEvent) = {
    this(other.getFactory.asInstanceOf[MessageFactoryInterface], other)
  }

}