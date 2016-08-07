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

object KamanjaExecutionFailureEvent extends RDDObject[KamanjaExecutionFailureEvent] with MessageFactoryInterface {

  val log = LogManager.getLogger(getClass)
  type T = KamanjaExecutionFailureEvent;
  override def getFullTypeName: String = "com.ligadata.KamanjaBase.KamanjaExecutionFailureEvent";
  override def getTypeNameSpace: String = "com.ligadata.KamanjaBase";
  override def getTypeName: String = "KamanjaExecutionFailureEvent";
  override def getTypeVersion: String = "000001.000000.000000";
  override def getSchemaId: Int = 1000004;
  override def getTenantId: String = "System";
  override def createInstance: KamanjaExecutionFailureEvent = new KamanjaExecutionFailureEvent(KamanjaExecutionFailureEvent);
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

  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanjabase" , "name" : "kamanjaexecutionfailureevent" , "fields":[{ "name" : "msgid" , "type" : "long"},{ "name" : "timeoferrorepochms" , "type" : "long"},{ "name" : "msgcontent" , "type" : "string"},{ "name" : "msgadapterkey" , "type" : "string"},{ "name" : "msgadaptervalue" , "type" : "string"},{ "name" : "sourceadapter" , "type" : "string"},{ "name" : "deserializer" , "type" : "string"},{ "name" : "errordetail" , "type" : "string"}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.KamanjaBase.KamanjaExecutionFailureEvent => { return convertToVer1000000000000(oldVerobj); }
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

  private def convertToVer1000000000000(oldVerobj: com.ligadata.KamanjaBase.KamanjaExecutionFailureEvent): com.ligadata.KamanjaBase.KamanjaExecutionFailureEvent = {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj KamanjaExecutionFailureEvent") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj KamanjaExecutionFailureEvent");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj KamanjaExecutionFailureEvent");
  override def NeedToTransformData: Boolean = false
}

class KamanjaExecutionFailureEvent(factory: MessageFactoryInterface, other: KamanjaExecutionFailureEvent) extends MessageInterface(factory) {
  def isCaseSensitive(): Boolean = KamanjaExecutionFailureEvent.isCaseSensitive();

  val log = KamanjaExecutionFailureEvent.log

  var attributeTypes = generateAttributeTypes;

  private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
    var attributeTypes = new Array[AttributeTypeInfo](8);
    attributeTypes(0) = new AttributeTypeInfo("msgid", 0, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
    attributeTypes(1) = new AttributeTypeInfo("timeoferrorepochms", 1, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
    attributeTypes(2) = new AttributeTypeInfo("msgcontent", 2, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(3) = new AttributeTypeInfo("msgadapterkey", 3, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(4) = new AttributeTypeInfo("msgadaptervalue", 4, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(5) = new AttributeTypeInfo("sourceadapter", 5, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(6) = new AttributeTypeInfo("deserializer", 6, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(7) = new AttributeTypeInfo("errordetail", 7, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)

    return attributeTypes
  }

  var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;

  if (other != null && other != this) {
    // call copying fields from other to local variables
    fromFunc(other)
  }

  override def save: Unit = { /* KamanjaExecutionFailureEvent.saveOne(this) */ }

  def Clone(): ContainerOrConcept = { KamanjaExecutionFailureEvent.build(this) }

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

  var msgid: Long = _;
  var timeoferrorepochms: Long = _;
  var msgcontent: String = _;
  var msgadapterkey: String = _;
  var msgadaptervalue: String = _;
  var sourceadapter: String = _;
  var deserializer: String = _;
  var errordetail: String = _;

  override def getAttributeTypes(): Array[AttributeTypeInfo] = {
    if (attributeTypes == null) return null;
    return attributeTypes
  }

  private def getWithReflection(key: String): AnyRef = {
    val ru = scala.reflect.runtime.universe
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val im = m.reflect(this)
    val fieldX = ru.typeOf[KamanjaExecutionFailureEvent].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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
    if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container KamanjaExecutionFailureEvent", null);
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


  override def get(index : Int) : AnyRef = { // Return (value, type)
    try{
      index match {
        case 0 => return this.msgid.asInstanceOf[AnyRef];
        case 1 => return this.timeoferrorepochms.asInstanceOf[AnyRef];
        case 2 => return this.msgcontent.asInstanceOf[AnyRef];
        case 3 => return this.msgadapterkey.asInstanceOf[AnyRef];
        case 4 => return this.msgadaptervalue.asInstanceOf[AnyRef];
        case 5 => return this.sourceadapter.asInstanceOf[AnyRef];
        case 6 => return this.deserializer.asInstanceOf[AnyRef];
        case 7 => return this.errordetail.asInstanceOf[AnyRef];

        case _ => throw new Exception(s"$index is a bad index for message KamanjaExecutionFailureEvent");
      }
    }catch {
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
    var attributeVals = new Array[AttributeValue](8);
    try {
      attributeVals(0) = new AttributeValue(this.msgid, keyTypes("msgid"))
      attributeVals(1) = new AttributeValue(this.timeoferrorepochms, keyTypes("timeoferrorepochms"))
      attributeVals(2) = new AttributeValue(this.msgcontent, keyTypes("msgcontent"))
      attributeVals(3) = new AttributeValue(this.msgadapterkey, keyTypes("msgadapterkey"))
      attributeVals(4) = new AttributeValue(this.msgadaptervalue, keyTypes("msgadaptervalue"))
      attributeVals(5) = new AttributeValue(this.sourceadapter, keyTypes("sourceadapter"))
      attributeVals(6) = new AttributeValue(this.deserializer, keyTypes("deserializer"))
      attributeVals(7) = new AttributeValue(this.errordetail, keyTypes("errordetail"))

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

      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message KamanjaExecutionFailureEvent", null)
      set(keyTypes(key).getIndex, value);

    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

  }

  def set(index: Int, value: Any): Unit = {
    if (value == null) throw new Exception(s"Value is null for index $index in message KamanjaExecutionFailureEvent ")
    try {
      index match {
        case 0 => {
          if (value.isInstanceOf[Long])
            this.msgid = value.asInstanceOf[Long];
          else throw new Exception(s"Value is the not the correct type for field msgid in message KamanjaExecutionFailureEvent")
        }
        case 1 => {
          if (value.isInstanceOf[Long])
            this.timeoferrorepochms = value.asInstanceOf[Long];
          else throw new Exception(s"Value is the not the correct type for field timeoferrorepochms in message KamanjaExecutionFailureEvent")
        }
        case 2 => {
          if (value.isInstanceOf[String])
            this.msgcontent = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field msgcontent in message KamanjaExecutionFailureEvent")
        }
        case 3 => {
          if (value.isInstanceOf[String])
            this.msgadapterkey = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field msgadapterkey in message KamanjaExecutionFailureEvent")
        }
        case 4 => {
          if (value.isInstanceOf[String])
            this.msgadaptervalue = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field msgadaptervalue in message KamanjaExecutionFailureEvent")
        }
        case 5 => {
          if (value.isInstanceOf[String])
            this.sourceadapter = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field sourceadapter in message KamanjaExecutionFailureEvent")
        }
        case 6 => {
          if (value.isInstanceOf[String])
            this.deserializer = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field deserializer in message KamanjaExecutionFailureEvent")
        }
        case 7 => {
          if (value.isInstanceOf[String])
            this.errordetail = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field errordetail in message KamanjaExecutionFailureEvent")
        }

        case _ => throw new Exception(s"$index is a bad index for message KamanjaExecutionFailureEvent");
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

  private def fromFunc(other: KamanjaExecutionFailureEvent): KamanjaExecutionFailureEvent = {
    this.msgid = com.ligadata.BaseTypes.LongImpl.Clone(other.msgid);
    this.timeoferrorepochms = com.ligadata.BaseTypes.LongImpl.Clone(other.timeoferrorepochms);
    this.msgcontent = com.ligadata.BaseTypes.StringImpl.Clone(other.msgcontent);
    this.msgadapterkey = com.ligadata.BaseTypes.StringImpl.Clone(other.msgadapterkey);
    this.msgadaptervalue = com.ligadata.BaseTypes.StringImpl.Clone(other.msgadaptervalue);
    this.sourceadapter = com.ligadata.BaseTypes.StringImpl.Clone(other.sourceadapter);
    this.deserializer = com.ligadata.BaseTypes.StringImpl.Clone(other.deserializer);
    this.errordetail = com.ligadata.BaseTypes.StringImpl.Clone(other.errordetail);

    this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
    return this;
  }

  def withmsgid(value: Long): KamanjaExecutionFailureEvent = {
    this.msgid = value
    return this
  }
  def withtimeoferrorepochms(value: Long): KamanjaExecutionFailureEvent = {
    this.timeoferrorepochms = value
    return this
  }
  def withmsgcontent(value: String): KamanjaExecutionFailureEvent = {
    this.msgcontent = value
    return this
  }
  def withmsgadapterkey(value: String): KamanjaExecutionFailureEvent = {
    this.msgadapterkey = value
    return this
  }
  def withmsgadaptervalue(value: String): KamanjaExecutionFailureEvent = {
    this.msgadaptervalue = value
    return this
  }
  def withsourceadapter(value: String): KamanjaExecutionFailureEvent = {
    this.sourceadapter = value
    return this
  }
  def withdeserializer(value: String): KamanjaExecutionFailureEvent = {
    this.deserializer = value
    return this
  }
  def witherrordetail(value: String): KamanjaExecutionFailureEvent = {
    this.errordetail = value
    return this
  }

  def this(factory: MessageFactoryInterface) = {
    this(factory, null)
  }

  def this(other: KamanjaExecutionFailureEvent) = {
    this(other.getFactory.asInstanceOf[MessageFactoryInterface], other)
  }

}