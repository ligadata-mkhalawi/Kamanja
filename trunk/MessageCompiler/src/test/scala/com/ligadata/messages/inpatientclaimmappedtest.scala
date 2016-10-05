
package com.ligadata.messages.V1000000;

import org.json4s.jackson.JsonMethods._;
import org.json4s.DefaultFormats;
import org.json4s.Formats;
import com.ligadata.KamanjaBase._;
import com.ligadata.BaseTypes._;
import com.ligadata.Exceptions.StackTrace;
import org.apache.logging.log4j.{ Logger, LogManager }
import java.util.Date;
import java.io.{ DataInputStream, DataOutputStream, ByteArrayOutputStream }

object InpatientClaimMappedTest extends RDDObject[InpatientClaimMappedTest] with ContainerFactoryInterface {

  val log = LogManager.getLogger(getClass)
  type T = InpatientClaimMappedTest;
  override def getFullTypeName: String = "com.ligadata.messages.InpatientClaimMappedTest";
  override def getTypeNameSpace: String = "com.ligadata.messages";
  override def getTypeName: String = "InpatientClaimMappedTest";
  override def getTypeVersion: String = "000000.000001.000000";
  override def getSchemaId: Int = 0;
  override def getTenantId: String = "";
  override def createInstance: InpatientClaimMappedTest = new InpatientClaimMappedTest(InpatientClaimMappedTest);
  override def isFixed: Boolean = false;
  override def getContainerType: ContainerTypes.ContainerType = ContainerTypes.ContainerType.CONTAINER
  override def getFullName = getFullTypeName;
  override def getRddTenantId = getTenantId;
  override def toJavaRDDObject: JavaRDDObject[T] = JavaRDDObject.fromRDDObject[T](this);

  def build = new T(this)
  def build(from: T) = new T(from)
  override def getPartitionKeyNames: Array[String] = Array("desynpuf_id");

  override def getPrimaryKeyNames: Array[String] = Array("desynpuf_id", "clm_id");

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

  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "inpatientclaimmappedtest" , "fields":[{ "name" : "desynpuf_id" , "type" : "string"},{ "name" : "clm_id" , "type" : "long"},{ "name" : "icd9_dgns_cds" , "type" :  {"type" : "array", "items" : "string"}},{ "name" : "icd9_prcdr_cds" , "type" :  {"type" : "array", "items" : "int"}},{ "name" : "icd9_check" , "type" : "boolean"},{ "name" : "hcpcs_cds" , "type" : {"type" : "map", "values" : "double"}},{ "name" : "idcodedimmap" , "type" : "map", "values" : { "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "idcodedimmappedtest" , "fields":[{ "name" : "id" , "type" : "int"},{ "name" : "code" , "type" : "int"},{ "name" : "description" , "type" : "string"},{ "name" : "codes" , "type" : {"type" : "map", "values" : "int"}}]}},{ "name" : "idcodedim" ,"type" : { "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "idcodedimmappedtest" , "fields":[{ "name" : "id" , "type" : "int"},{ "name" : "code" , "type" : "int"},{ "name" : "description" , "type" : "string"},{ "name" : "codes" , "type" : {"type" : "map", "values" : "int"}}]}},{ "name" : "idcodedims" ,"type": {"type": "array", "items": { "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "idcodedimmappedtest" , "fields":[{ "name" : "id" , "type" : "int"},{ "name" : "code" , "type" : "int"},{ "name" : "description" , "type" : "string"},{ "name" : "codes" , "type" : {"type" : "map", "values" : "int"}}]}}}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.messages.V1000000.InpatientClaimMappedTest => { return convertToVer1000000(oldVerobj); }
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

  private def convertToVer1000000(oldVerobj: com.ligadata.messages.V1000000.InpatientClaimMappedTest): com.ligadata.messages.V1000000.InpatientClaimMappedTest = {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj InpatientClaimMappedTest") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj InpatientClaimMappedTest");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj InpatientClaimMappedTest");
  override def NeedToTransformData: Boolean = false
}

class InpatientClaimMappedTest(factory: ContainerFactoryInterface, other: InpatientClaimMappedTest) extends ContainerInterface(factory) {

  val log = InpatientClaimMappedTest.log

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

  override def save: Unit = { /* InpatientClaimMappedTest.saveOne(this) */ }

  def Clone(): ContainerOrConcept = { InpatientClaimMappedTest.build(this) }

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

  override def getOrElse(keyName: String, defaultVal: Any): AnyRef = { // Return (value)
    var attributeValue: AttributeValue = new AttributeValue();
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = keyName.toLowerCase;
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
      if (getTimePartitionInfo.getFieldName != null && getTimePartitionInfo.getFieldName.trim().size > 0 && getTimePartitionInfo.getFieldName.equalsIgnoreCase(key)) {
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
      if (getTimePartitionInfo.getFieldName != null && getTimePartitionInfo.getFieldName.trim().size > 0 && getTimePartitionInfo.getFieldName.equalsIgnoreCase(key)) {
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

  private def fromFunc(other: InpatientClaimMappedTest): InpatientClaimMappedTest = {

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
      if (other.valuesMap.contains("icd9_dgns_cds")) {
        val fld = other.valuesMap("icd9_dgns_cds").getValue;
        if (fld == null) valuesMap.put("icd9_dgns_cds", null);
        else {
          val o = fld.asInstanceOf[scala.Array[String]]
          var icd9_dgns_cds = new scala.Array[String](o.size)
          for (i <- 0 until o.length) {
            icd9_dgns_cds(i) = com.ligadata.BaseTypes.StringImpl.Clone(o(i))
          }
          valuesMap.put("icd9_dgns_cds", new AttributeValue(icd9_dgns_cds, other.valuesMap("icd9_dgns_cds").getValueType))
        }
      } else valuesMap.put("icd9_dgns_cds", null);
    };
    {
      if (other.valuesMap.contains("icd9_prcdr_cds")) {
        val fld = other.valuesMap("icd9_prcdr_cds").getValue;
        if (fld == null) valuesMap.put("icd9_prcdr_cds", null);
        else {
          val o = fld.asInstanceOf[scala.Array[Int]]
          var icd9_prcdr_cds = new scala.Array[Int](o.size)
          for (i <- 0 until o.length) {
            icd9_prcdr_cds(i) = com.ligadata.BaseTypes.IntImpl.Clone(o(i))
          }
          valuesMap.put("icd9_prcdr_cds", new AttributeValue(icd9_prcdr_cds, other.valuesMap("icd9_prcdr_cds").getValueType))
        }
      } else valuesMap.put("icd9_prcdr_cds", null);
    };
    {
      if (other.valuesMap.contains("hcpcs_cds")) {
        val hcpcs_cds = other.valuesMap("hcpcs_cds").getValue;
        if (hcpcs_cds == null) valuesMap.put("hcpcs_cds", null);
        else {
          if (hcpcs_cds.isInstanceOf[scala.collection.immutable.Map[String, Double]]) {
            val mapmap = hcpcs_cds.asInstanceOf[scala.collection.immutable.Map[String, Double]].map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), com.ligadata.BaseTypes.DoubleImpl.Clone(a._2)) }.toMap
            valuesMap.put("hcpcs_cds", new AttributeValue(mapmap, other.valuesMap("hcpcs_cds").getValueType));
          }
        }
      } else valuesMap.put("hcpcs_cds", null);
    }
    {
      if (other.valuesMap.contains("idcodedim")) {
        val idcodedim = other.valuesMap("idcodedim").getValue;
        if (idcodedim == null) valuesMap.put("idcodedim", null);
        else {
          valuesMap.put("idcodedim", new AttributeValue(idcodedim.asInstanceOf[com.ligadata.messages.V10000001000000.IdCodeDimMappedTest].Clone.asInstanceOf[com.ligadata.messages.V10000001000000.IdCodeDimMappedTest], other.valuesMap("idcodedim").getValueType));
        }
      } else valuesMap.put("idcodedim", null);
    };
    {
      if (other.valuesMap.contains("idcodedims")) {
        val fld = other.valuesMap("idcodedims").getValue;
        if (fld == null) valuesMap.put("idcodedims", null);
        else {
          val o = fld.asInstanceOf[scala.Array[com.ligadata.messages.V10000001000000.IdCodeDimMappedTest]];
          var idcodedims = new scala.Array[com.ligadata.messages.V10000001000000.IdCodeDimMappedTest](o.size);
          for (i <- 0 until o.length) {
            idcodedims(i) = o(i).Clone.asInstanceOf[com.ligadata.messages.V10000001000000.IdCodeDimMappedTest];
          }
          valuesMap.put("idcodedims", new AttributeValue(idcodedims, other.valuesMap("idcodedims").getValueType));
        }
      } else valuesMap.put("idcodedims", null);
    };

    this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
    return this;
  }

  def withdesynpuf_id(value: String): InpatientClaimMappedTest = {
    valuesMap("desynpuf_id") = new AttributeValue(value, keyTypes("desynpuf_id"))
    return this
  }
  def withclm_id(value: Long): InpatientClaimMappedTest = {
    valuesMap("clm_id") = new AttributeValue(value, keyTypes("clm_id"))
    return this
  }
  def withicd9_dgns_cds(value: scala.Array[String]): InpatientClaimMappedTest = {
    valuesMap("icd9_dgns_cds") = new AttributeValue(value, keyTypes("icd9_dgns_cds"))
    return this
  }
  def withicd9_prcdr_cds(value: scala.Array[Int]): InpatientClaimMappedTest = {
    valuesMap("icd9_prcdr_cds") = new AttributeValue(value, keyTypes("icd9_prcdr_cds"))
    return this
  }
  def withicd9_check(value: Boolean): InpatientClaimMappedTest = {
    valuesMap("icd9_check") = new AttributeValue(value, keyTypes("icd9_check"))
    return this
  }
  def withhcpcs_cds(value: scala.collection.immutable.Map[String, Double]): InpatientClaimMappedTest = {
    valuesMap("hcpcs_cds") = new AttributeValue(value, keyTypes("hcpcs_cds"))
    return this
  }
  def withidcodedimmap(value: scala.collection.immutable.Map[String, com.ligadata.messages.V10000001000000.IdCodeDimMappedTest]): InpatientClaimMappedTest = {
    valuesMap("idcodedimmap") = new AttributeValue(value, keyTypes("idcodedimmap"))
    return this
  }
  def withidcodedim(value: com.ligadata.messages.V10000001000000.IdCodeDimMappedTest): InpatientClaimMappedTest = {
    valuesMap("idcodedim") = new AttributeValue(value, keyTypes("idcodedim"))
    return this
  }
  def withidcodedims(value: scala.Array[com.ligadata.messages.V10000001000000.IdCodeDimMappedTest]): InpatientClaimMappedTest = {
    valuesMap("idcodedims") = new AttributeValue(value, keyTypes("idcodedims"))
    return this
  }

  def this(factory: ContainerFactoryInterface) = {
    this(factory, null)
  }

  def this(other: InpatientClaimMappedTest) = {
    this(other.getFactory.asInstanceOf[ContainerFactoryInterface], other)
  }

}