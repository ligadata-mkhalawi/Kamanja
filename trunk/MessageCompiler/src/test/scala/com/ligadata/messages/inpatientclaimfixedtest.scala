
package com.ligadata.messages.V1000000;

import org.json4s.jackson.JsonMethods._;
import org.json4s.DefaultFormats;
import org.json4s.Formats;
import com.ligadata.KamanjaBase._;
import com.ligadata.BaseTypes._;
import com.ligadata.Exceptions._;
import org.apache.logging.log4j.{ Logger, LogManager }
import java.util.Date;
import java.io.{ DataInputStream, DataOutputStream, ByteArrayOutputStream }

object InpatientClaimFixedTest extends RDDObject[InpatientClaimFixedTest] with ContainerFactoryInterface {

  val log = LogManager.getLogger(getClass)
  type T = InpatientClaimFixedTest;
  override def getFullTypeName: String = "com.ligadata.messages.InpatientClaimFixedTest";
  override def getTypeNameSpace: String = "com.ligadata.messages";
  override def getTypeName: String = "InpatientClaimFixedTest";
  override def getTypeVersion: String = "000000.000001.000000";
  override def getSchemaId: Int = 0;
  override def getTenantId: String = "";
  override def createInstance: InpatientClaimFixedTest = new InpatientClaimFixedTest(InpatientClaimFixedTest);
  override def isFixed: Boolean = true;
  override def getContainerType: ContainerTypes.ContainerType = ContainerTypes.ContainerType.CONTAINER
  override def getFullName = getFullTypeName;
  override def getRddTenantId = getTenantId;
  override def toJavaRDDObject: JavaRDDObject[T] = JavaRDDObject.fromRDDObject[T](this);

  def build = new T(this)
  def build(from: T) = new T(from)
  override def getPartitionKeyNames: Array[String] = Array("desynpuf_id");

  override def getPrimaryKeyNames: Array[String] = Array("desynpuf_id", "clm_id");

  override def getTimePartitionInfo: TimePartitionInfo = {
    var timePartitionInfo: TimePartitionInfo = new TimePartitionInfo();
    timePartitionInfo.setFieldName("desynpuf_id");
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

  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "inpatientclaimfixedtest" , "fields":[{ "name" : "desynpuf_id" , "type" : "string"},{ "name" : "clm_id" , "type" : "long"},{ "name" : "icd9_dgns_cds" , "type" :  {"type" : "array", "items" : "string"}},{ "name" : "icd9_prcdr_cds" , "type" :  {"type" : "array", "items" : "int"}},{ "name" : "icd9_check" , "type" : "boolean"},{ "name" : "hcpcs_cds" , "type" : {"type" : "map", "values" : "double"}},{ "name" : "idcodedimmap" , "type" : "map", "values" : { "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "idcodedimfixedtest" , "fields":[{ "name" : "id" , "type" : "int"},{ "name" : "code" , "type" : "int"},{ "name" : "description" , "type" : "string"},{ "name" : "codes" , "type" : {"type" : "map", "values" : "int"}}]}},{ "name" : "idcodedim" , "type": { "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "idcodedimfixedtest" , "fields":[{ "name" : "id" , "type" : "int"},{ "name" : "code" , "type" : "int"},{ "name" : "description" , "type" : "string"},{ "name" : "codes" , "type" : {"type" : "map", "values" : "int"}}]}},{ "name" : "idcodedims" ,"type": {"type": "array", "items": { "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "idcodedimfixedtest" , "fields":[{ "name" : "id" , "type" : "int"},{ "name" : "code" , "type" : "int"},{ "name" : "description" , "type" : "string"},{ "name" : "codes" , "type" : {"type" : "map", "values" : "int"}}]}}}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.messages.V1000000.InpatientClaimFixedTest => { return convertToVer1000000(oldVerobj); }
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

  private def convertToVer1000000(oldVerobj: com.ligadata.messages.V1000000.InpatientClaimFixedTest): com.ligadata.messages.V1000000.InpatientClaimFixedTest = {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj InpatientClaimFixedTest") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj InpatientClaimFixedTest");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj InpatientClaimFixedTest");
  override def NeedToTransformData: Boolean = false
}

class InpatientClaimFixedTest(factory: ContainerFactoryInterface, other: InpatientClaimFixedTest) extends ContainerInterface(factory) {

  val log = InpatientClaimFixedTest.log

  var attributeTypes = generateAttributeTypes;

  private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
    var attributeTypes = new Array[AttributeTypeInfo](9);
    attributeTypes(0) = new AttributeTypeInfo("desynpuf_id", 0, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(1) = new AttributeTypeInfo("clm_id", 1, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
    attributeTypes(2) = new AttributeTypeInfo("icd9_dgns_cds", 2, AttributeTypeInfo.TypeCategory.ARRAY, 1, -1, 0)
    attributeTypes(3) = new AttributeTypeInfo("icd9_prcdr_cds", 3, AttributeTypeInfo.TypeCategory.ARRAY, 0, -1, 0)
    attributeTypes(4) = new AttributeTypeInfo("icd9_check", 4, AttributeTypeInfo.TypeCategory.BOOLEAN, -1, -1, 0)
    attributeTypes(5) = new AttributeTypeInfo("hcpcs_cds", 5, AttributeTypeInfo.TypeCategory.MAP, 3, 1, 0)
    attributeTypes(6) = new AttributeTypeInfo("idcodedimmap", 6, AttributeTypeInfo.TypeCategory.MAP, 1001, 1, 0)
    attributeTypes(7) = new AttributeTypeInfo("idcodedim", 7, AttributeTypeInfo.TypeCategory.CONTAINER, -1, -1, 0)
    attributeTypes(8) = new AttributeTypeInfo("idcodedims", 8, AttributeTypeInfo.TypeCategory.ARRAY, 1001, -1, 0)

    return attributeTypes
  }

  var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;

  if (other != null && other != this) {
    // call copying fields from other to local variables
    fromFunc(other)
  }

  override def save: Unit = { /* InpatientClaimFixedTest.saveOne(this) */ }

  def Clone(): ContainerOrConcept = { InpatientClaimFixedTest.build(this) }

  override def getPartitionKey: Array[String] = {
    var partitionKeys: scala.collection.mutable.ArrayBuffer[String] = scala.collection.mutable.ArrayBuffer[String]();
    try {
      partitionKeys += com.ligadata.BaseTypes.StringImpl.toString(get("desynpuf_id").asInstanceOf[String]);
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
      primaryKeys += com.ligadata.BaseTypes.StringImpl.toString(get("desynpuf_id").asInstanceOf[String]);
      primaryKeys += com.ligadata.BaseTypes.LongImpl.toString(get("clm_id").asInstanceOf[Long]);
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

  var desynpuf_id: String = _;
  var clm_id: Long = _;
  var icd9_dgns_cds: scala.Array[String] = _;
  var icd9_prcdr_cds: scala.Array[Int] = _;
  var icd9_check: Boolean = _;
  var hcpcs_cds: scala.collection.immutable.Map[String, Double] = _;
  var idcodedimmap: scala.collection.immutable.Map[String, com.ligadata.messages.V1000000000000.IdCodeDimFixedTest] = _;
  var idcodedim: com.ligadata.messages.V1000000000000.IdCodeDimFixedTest = _;
  var idcodedims: scala.Array[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest] = _;

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
    val fieldX = ru.typeOf[InpatientClaimFixedTest].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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

    if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container InpatientClaimFixedTest", null);
    return get(keyTypes(key).getIndex)
  }

  override def getOrElse(keyName: String, defaultVal: Any): AnyRef = { // Return (value, type)
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = keyName.toLowerCase;
    try {
      return get(key.toLowerCase())
    } catch {
      case e: Exception => {
        log.debug("", e)
        return defaultVal.asInstanceOf[AnyRef];
      }
    }
    return null;
  }

  override def get(index: Int): AnyRef = { // Return (value, type)
    try {
      index match {
        case 0 => return this.desynpuf_id.asInstanceOf[AnyRef];
        case 1 => return this.clm_id.asInstanceOf[AnyRef];
        case 2 => return this.icd9_dgns_cds.asInstanceOf[AnyRef];
        case 3 => return this.icd9_prcdr_cds.asInstanceOf[AnyRef];
        case 4 => return this.icd9_check.asInstanceOf[AnyRef];
        case 5 => return this.hcpcs_cds.asInstanceOf[AnyRef];
        case 6 => return this.idcodedimmap.asInstanceOf[AnyRef];
        case 7 => return this.idcodedim.asInstanceOf[AnyRef];
        case 8 => return this.idcodedims.asInstanceOf[AnyRef];

        case _ => throw new Exception(s"$index is a bad index for message InpatientClaimFixedTest");
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
      return get(index);
    } catch {
      case e: Exception => {
        log.debug("", e)
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
    var attributeVals = new Array[AttributeValue](9);
    try {
      attributeVals(0) = new AttributeValue(this.desynpuf_id, keyTypes("desynpuf_id"))
      attributeVals(1) = new AttributeValue(this.clm_id, keyTypes("clm_id"))
      attributeVals(2) = new AttributeValue(this.icd9_dgns_cds, keyTypes("icd9_dgns_cds"))
      attributeVals(3) = new AttributeValue(this.icd9_prcdr_cds, keyTypes("icd9_prcdr_cds"))
      attributeVals(4) = new AttributeValue(this.icd9_check, keyTypes("icd9_check"))
      attributeVals(5) = new AttributeValue(this.hcpcs_cds, keyTypes("hcpcs_cds"))
      attributeVals(6) = new AttributeValue(this.idcodedimmap, keyTypes("idcodedimmap"))
      attributeVals(7) = new AttributeValue(this.idcodedim, keyTypes("idcodedim"))
      attributeVals(8) = new AttributeValue(this.idcodedims, keyTypes("idcodedims"))

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

      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message InpatientClaimFixedTest", null)
      set(keyTypes(key).getIndex, value);

    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

  }

  def set(index: Int, value: Any): Unit = {
    if (value == null) throw new Exception(s"Value is null for index $index in message InpatientClaimFixedTest ")
    try {
      index match {
        case 0 => {
          if (value.isInstanceOf[String]) {
            this.desynpuf_id = value.asInstanceOf[String];
            setTimePartitionData;
          } else throw new Exception(s"Value is the not the correct type for field desynpuf_id in message InpatientClaimFixedTest")
        }
        case 1 => {
          if (value.isInstanceOf[Long])
            this.clm_id = value.asInstanceOf[Long];
          else throw new Exception(s"Value is the not the correct type for field clm_id in message InpatientClaimFixedTest")
        }
        case 2 => {
          if (value.isInstanceOf[scala.Array[String]])
            this.icd9_dgns_cds = value.asInstanceOf[scala.Array[String]];
          else if (value.isInstanceOf[scala.Array[_]])
            this.icd9_dgns_cds = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[String]);
          else throw new Exception(s"Value is the not the correct type for field icd9_dgns_cds in message InpatientClaimFixedTest")
        }
        case 3 => {
          if (value.isInstanceOf[scala.Array[Int]])
            this.icd9_prcdr_cds = value.asInstanceOf[scala.Array[Int]];
          else if (value.isInstanceOf[scala.Array[_]])
            this.icd9_prcdr_cds = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[Int]);
          else throw new Exception(s"Value is the not the correct type for field icd9_prcdr_cds in message InpatientClaimFixedTest")
        }
        case 4 => {
          if (value.isInstanceOf[Boolean])
            this.icd9_check = value.asInstanceOf[Boolean];
          else throw new Exception(s"Value is the not the correct type for field icd9_check in message InpatientClaimFixedTest")
        }
        case 5 => {
          if (value.isInstanceOf[scala.collection.immutable.Map[String, Double]])
            this.hcpcs_cds = value.asInstanceOf[scala.collection.immutable.Map[String, Double]];
          else throw new Exception(s"Value is the not the correct type for field hcpcs_cds in message InpatientClaimFixedTest")
        }
        case 6 => {
          if (value.isInstanceOf[scala.collection.immutable.Map[String, com.ligadata.messages.V1000000000000.IdCodeDimFixedTest]])
            this.idcodedimmap = value.asInstanceOf[scala.collection.immutable.Map[String, com.ligadata.messages.V1000000000000.IdCodeDimFixedTest]];
          else if (value.isInstanceOf[scala.collection.immutable.Map[String, ContainerInterface]])
            this.idcodedimmap = value.asInstanceOf[scala.collection.immutable.Map[String, ContainerInterface]].map(v => (v._1, v._2.asInstanceOf[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest]));
          else throw new Exception(s"Value is the not the correct type for field idcodedimmap in message InpatientClaimFixedTest")
        }
        case 7 => {
          if (value.isInstanceOf[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest])
            this.idcodedim = value.asInstanceOf[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest];
          else if (value.isInstanceOf[ContainerInterface])
            this.idcodedim = value.asInstanceOf[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest];
          else throw new Exception(s"Value is the not the correct type for field idcodedim in message InpatientClaimFixedTest")
        }
        case 8 => {
          if (value.isInstanceOf[scala.Array[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest]])
            this.idcodedims = value.asInstanceOf[scala.Array[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest]];
          else if (value.isInstanceOf[scala.Array[ContainerInterface]])
            this.idcodedims = value.asInstanceOf[scala.Array[ContainerInterface]].map(v => v.asInstanceOf[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest]);
          else throw new Exception(s"Value is the not the correct type for field idcodedims in message InpatientClaimFixedTest")
        }

        case _ => throw new Exception(s"$index is a bad index for message InpatientClaimFixedTest");
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

  private def fromFunc(other: InpatientClaimFixedTest): InpatientClaimFixedTest = {
    this.desynpuf_id = com.ligadata.BaseTypes.StringImpl.Clone(other.desynpuf_id);
    this.clm_id = com.ligadata.BaseTypes.LongImpl.Clone(other.clm_id);
    if (other.icd9_dgns_cds != null) {
      icd9_dgns_cds = new scala.Array[String](other.icd9_dgns_cds.length);
      icd9_dgns_cds = other.icd9_dgns_cds.map(v => com.ligadata.BaseTypes.StringImpl.Clone(v));
    } else this.icd9_dgns_cds = null;
    if (other.icd9_prcdr_cds != null) {
      icd9_prcdr_cds = new scala.Array[Int](other.icd9_prcdr_cds.length);
      icd9_prcdr_cds = other.icd9_prcdr_cds.map(v => com.ligadata.BaseTypes.IntImpl.Clone(v));
    } else this.icd9_prcdr_cds = null;
    this.icd9_check = com.ligadata.BaseTypes.BoolImpl.Clone(other.icd9_check);
    if (other.hcpcs_cds != null) {
      this.hcpcs_cds = other.hcpcs_cds.map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), com.ligadata.BaseTypes.DoubleImpl.Clone(a._2)) }.toMap
    } else this.hcpcs_cds = null;
    if (other.idcodedimmap != null) {
      this.idcodedimmap = other.idcodedimmap.map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), a._2.Clone.asInstanceOf[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest]) }.toMap
    } else this.idcodedimmap = null;
    if (other.idcodedim != null) {
      idcodedim = other.idcodedim.Clone.asInstanceOf[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest]
    } else idcodedim = null;
    if (other.idcodedims != null) {
      idcodedims = new scala.Array[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest](other.idcodedims.length)
      idcodedims = other.idcodedims.map(f => f.Clone.asInstanceOf[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest]);
    } else idcodedims = null;

    this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
    return this;
  }

  def withdesynpuf_id(value: String): InpatientClaimFixedTest = {
    this.desynpuf_id = value
    return this
  }
  def withclm_id(value: Long): InpatientClaimFixedTest = {
    this.clm_id = value
    return this
  }
  def withicd9_dgns_cds(value: scala.Array[String]): InpatientClaimFixedTest = {
    this.icd9_dgns_cds = value
    return this
  }
  def withicd9_prcdr_cds(value: scala.Array[Int]): InpatientClaimFixedTest = {
    this.icd9_prcdr_cds = value
    return this
  }
  def withicd9_check(value: Boolean): InpatientClaimFixedTest = {
    this.icd9_check = value
    return this
  }
  def withhcpcs_cds(value: scala.collection.immutable.Map[String, Double]): InpatientClaimFixedTest = {
    this.hcpcs_cds = value
    return this
  }
  def withidcodedimmap(value: scala.collection.immutable.Map[String, com.ligadata.messages.V1000000000000.IdCodeDimFixedTest]): InpatientClaimFixedTest = {
    this.idcodedimmap = value
    return this
  }
  def withidcodedim(value: com.ligadata.messages.V1000000000000.IdCodeDimFixedTest): InpatientClaimFixedTest = {
    this.idcodedim = value
    return this
  }
  def withidcodedims(value: scala.Array[com.ligadata.messages.V1000000000000.IdCodeDimFixedTest]): InpatientClaimFixedTest = {
    this.idcodedims = value
    return this
  }

  def this(factory: ContainerFactoryInterface) = {
    this(factory, null)
  }

  def this(other: InpatientClaimFixedTest) = {
    this(other.getFactory.asInstanceOf[ContainerFactoryInterface], other)
  }

}