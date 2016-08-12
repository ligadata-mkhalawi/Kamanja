
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

object IdCodeDimFixed extends RDDObject[IdCodeDimFixed] with ContainerFactoryInterface {

  val log = LogManager.getLogger(getClass)
  type T = IdCodeDimFixed;
  override def getFullTypeName: String = "com.ligadata.messages.IdCodeDimFixed";
  override def getTypeNameSpace: String = "com.ligadata.messages";
  override def getTypeName: String = "IdCodeDimFixed";
  override def getTypeVersion: String = "000001.000000.000000";
  override def getSchemaId: Int = 0;
  override def getTenantId: String = "";
  override def createInstance: IdCodeDimFixed = new IdCodeDimFixed(IdCodeDimFixed);
  override def isFixed: Boolean = true;
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

  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "IdCodeDimFixed" , "fields":[{ "name" : "id" , "type" : "int"},{ "name" : "code1" , "type" : "float"},{ "name" : "code2" , "type" : "double"},{ "name" : "code3" , "type" : "long"},{ "name" : "code4" , "type" : "string"},{ "name" : "code5" ,},{ "name" : "arrcode1" , "type" :  {"type" : "array", "items" : "float"}},{ "name" : "arrcode2" , "type" :  {"type" : "array", "items" : "double"}},{ "name" : "arrcode3" , "type" :  {"type" : "array", "items" : "int"}},{ "name" : "arrcode4" , "type" :  {"type" : "array", "items" : "boolean"}},{ "name" : "arrcode5" , "type" :  {"type" : "array", "items" : "long"}},{ "name" : "description" , "type" :  {"type" : "array", "items" : "string"}},{ "name" : "mapcodes1" , "type" : {"type" : "map", "values" : "int"}},{ "name" : "mapcodes2" , "type" : {"type" : "map", "values" : "float"}},{ "name" : "mapcodes3" , "type" : {"type" : "map", "values" : "double"}}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.messages.V1000000000000.IdCodeDimFixed => { return convertToVer1000000000000(oldVerobj); }
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

  private def convertToVer1000000000000(oldVerobj: com.ligadata.messages.V1000000000000.IdCodeDimFixed): com.ligadata.messages.V1000000000000.IdCodeDimFixed = {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj IdCodeDimFixed") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj IdCodeDimFixed");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj IdCodeDimFixed");
  override def NeedToTransformData: Boolean = false
}

class IdCodeDimFixed(factory: ContainerFactoryInterface, other: IdCodeDimFixed) extends ContainerInterface(factory) {

  val log = IdCodeDimFixed.log

  var attributeTypes = generateAttributeTypes;

  private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
    var attributeTypes = new Array[AttributeTypeInfo](15);
    attributeTypes(0) = new AttributeTypeInfo("id", 0, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
    attributeTypes(1) = new AttributeTypeInfo("code1", 1, AttributeTypeInfo.TypeCategory.FLOAT, -1, -1, 0)
    attributeTypes(2) = new AttributeTypeInfo("code2", 2, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
    attributeTypes(3) = new AttributeTypeInfo("code3", 3, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
    attributeTypes(4) = new AttributeTypeInfo("code4", 4, AttributeTypeInfo.TypeCategory.CHAR, -1, -1, 0)
    attributeTypes(5) = new AttributeTypeInfo("code5", 5, AttributeTypeInfo.TypeCategory.BOOLEAN, -1, -1, 0)
    attributeTypes(6) = new AttributeTypeInfo("arrcode1", 6, AttributeTypeInfo.TypeCategory.ARRAY, 2, -1, 0)
    attributeTypes(7) = new AttributeTypeInfo("arrcode2", 7, AttributeTypeInfo.TypeCategory.ARRAY, 3, -1, 0)
    attributeTypes(8) = new AttributeTypeInfo("arrcode3", 8, AttributeTypeInfo.TypeCategory.ARRAY, 0, -1, 0)
    attributeTypes(9) = new AttributeTypeInfo("arrcode4", 9, AttributeTypeInfo.TypeCategory.ARRAY, 7, -1, 0)
    attributeTypes(10) = new AttributeTypeInfo("arrcode5", 10, AttributeTypeInfo.TypeCategory.ARRAY, 4, -1, 0)
    attributeTypes(11) = new AttributeTypeInfo("description", 11, AttributeTypeInfo.TypeCategory.ARRAY, 1, -1, 0)
    attributeTypes(12) = new AttributeTypeInfo("mapcodes1", 12, AttributeTypeInfo.TypeCategory.MAP, 0, 1, 0)
    attributeTypes(13) = new AttributeTypeInfo("mapcodes2", 13, AttributeTypeInfo.TypeCategory.MAP, 2, 1, 0)
    attributeTypes(14) = new AttributeTypeInfo("mapcodes3", 14, AttributeTypeInfo.TypeCategory.MAP, 3, 1, 0)

    return attributeTypes
  }

  var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;

  if (other != null && other != this) {
    // call copying fields from other to local variables
    fromFunc(other)
  }

  override def save: Unit = { IdCodeDimFixed.saveOne(this) }

  def Clone(): ContainerOrConcept = { IdCodeDimFixed.build(this) }

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

  var id: java.lang.Integer = _;
  var code1: java.lang.Float = _;
  var code2: java.lang.Double = _;
  var code3: java.lang.Long = _;
  var code4: java.lang.Character = _;
  var code5: java.lang.Boolean = _;
  var arrcode1: scala.Array[java.lang.Float] = _;
  var arrcode2: scala.Array[java.lang.Double] = _;
  var arrcode3: scala.Array[java.lang.Integer] = _;
  var arrcode4: scala.Array[java.lang.Boolean] = _;
  var arrcode5: scala.Array[java.lang.Long] = _;
  var description: scala.Array[String] = _;
  var mapcodes1: scala.collection.immutable.Map[String, java.lang.Integer] = _;
  var mapcodes2: scala.collection.immutable.Map[String, java.lang.Float] = _;
  var mapcodes3: scala.collection.immutable.Map[String, java.lang.Double] = _;

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
    val fieldX = ru.typeOf[IdCodeDimFixed].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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

    if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container IdCodeDimFixed", null);
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
        case 0  => return this.id.asInstanceOf[AnyRef];
        case 1  => return this.code1.asInstanceOf[AnyRef];
        case 2  => return this.code2.asInstanceOf[AnyRef];
        case 3  => return this.code3.asInstanceOf[AnyRef];
        case 4  => return this.code4.asInstanceOf[AnyRef];
        case 5  => return this.code5.asInstanceOf[AnyRef];
        case 6  => return this.arrcode1.asInstanceOf[AnyRef];
        case 7  => return this.arrcode2.asInstanceOf[AnyRef];
        case 8  => return this.arrcode3.asInstanceOf[AnyRef];
        case 9  => return this.arrcode4.asInstanceOf[AnyRef];
        case 10 => return this.arrcode5.asInstanceOf[AnyRef];
        case 11 => return this.description.asInstanceOf[AnyRef];
        case 12 => return this.mapcodes1.asInstanceOf[AnyRef];
        case 13 => return this.mapcodes2.asInstanceOf[AnyRef];
        case 14 => return this.mapcodes3.asInstanceOf[AnyRef];

        case _  => throw new Exception(s"$index is a bad index for message IdCodeDimFixed");
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
    var attributeVals = new Array[AttributeValue](15);
    try {
      attributeVals(0) = new AttributeValue(this.id, keyTypes("id"))
      attributeVals(1) = new AttributeValue(this.code1, keyTypes("code1"))
      attributeVals(2) = new AttributeValue(this.code2, keyTypes("code2"))
      attributeVals(3) = new AttributeValue(this.code3, keyTypes("code3"))
      attributeVals(4) = new AttributeValue(this.code4, keyTypes("code4"))
      attributeVals(5) = new AttributeValue(this.code5, keyTypes("code5"))
      attributeVals(6) = new AttributeValue(this.arrcode1, keyTypes("arrcode1"))
      attributeVals(7) = new AttributeValue(this.arrcode2, keyTypes("arrcode2"))
      attributeVals(8) = new AttributeValue(this.arrcode3, keyTypes("arrcode3"))
      attributeVals(9) = new AttributeValue(this.arrcode4, keyTypes("arrcode4"))
      attributeVals(10) = new AttributeValue(this.arrcode5, keyTypes("arrcode5"))
      attributeVals(11) = new AttributeValue(this.description, keyTypes("description"))
      attributeVals(12) = new AttributeValue(this.mapcodes1, keyTypes("mapcodes1"))
      attributeVals(13) = new AttributeValue(this.mapcodes2, keyTypes("mapcodes2"))
      attributeVals(14) = new AttributeValue(this.mapcodes3, keyTypes("mapcodes3"))

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

      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message IdCodeDimFixed", null)
      set(keyTypes(key).getIndex, value);

    } catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };

  }

  def set(index: Int, value: Any): Unit = {
    // if (value == null) throw new Exception(s"Value is null for index $index in message IdCodeDimFixed ")
    try {
      index match {
        case 0 => {
          if (value == null) this.id = null;
          else {
            if (value.isInstanceOf[java.lang.Integer] || value.isInstanceOf[Int]) {
              this.id = value.asInstanceOf[java.lang.Integer];
              setTimePartitionData;
            } else throw new Exception(s"Value is the not the correct type for field id in message IdCodeDimFixed")
          }
        }
        case 1 => {
          if (value == null) this.code1 = null;
          else {
            if (value.isInstanceOf[java.lang.Float] || value.isInstanceOf[Float])
              this.code1 = value.asInstanceOf[java.lang.Float];
            else throw new Exception(s"Value is the not the correct type for field code1 in message IdCodeDimFixed")
          }
        }
        case 2 => {
          if (value == null) this.code2 = null;
          else {
            if (value.isInstanceOf[java.lang.Double] || value.isInstanceOf[Double])
              this.code2 = value.asInstanceOf[java.lang.Double];
            else throw new Exception(s"Value is the not the correct type for field code2 in message IdCodeDimFixed")
          }
        }
        case 3 => {
          if (value == null) this.code3 = null;
          else {
            if (value.isInstanceOf[java.lang.Long] || value.isInstanceOf[Long])
              this.code3 = value.asInstanceOf[java.lang.Long];
            else throw new Exception(s"Value is the not the correct type for field code3 in message IdCodeDimFixed")
          }
        }
        case 4 => {
          if (value == null) this.code4 = null;
          else {
            if (value.isInstanceOf[java.lang.Character] || value.isInstanceOf[Char])
              this.code4 = value.asInstanceOf[java.lang.Character];
            else throw new Exception(s"Value is the not the correct type for field code4 in message IdCodeDimFixed")
          }
        }
        case 5 => {
          if (value == null) this.code5 = null;
          else {
            if (value.isInstanceOf[java.lang.Boolean] || value.isInstanceOf[Boolean])
              this.code5 = value.asInstanceOf[java.lang.Boolean];
            else throw new Exception(s"Value is the not the correct type for field code5 in message IdCodeDimFixed")
          }
        }
        case 6 => {
          if (value == null) this.arrcode1 = null;
          else {
            if (value.isInstanceOf[scala.Array[java.lang.Float]])
              this.arrcode1 = value.asInstanceOf[scala.Array[java.lang.Float]];
            else if (value.isInstanceOf[scala.Array[_]])
              this.arrcode1 = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[java.lang.Float]);
            else throw new Exception(s"Value is the not the correct type for field arrcode1 in message IdCodeDimFixed")
          }
        }
        case 7 => {
          if (value == null) this.arrcode2 = null;
          else {
            if (value.isInstanceOf[scala.Array[java.lang.Double]])
              this.arrcode2 = value.asInstanceOf[scala.Array[java.lang.Double]];
            else if (value.isInstanceOf[scala.Array[_]])
              this.arrcode2 = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[java.lang.Double]);
            else throw new Exception(s"Value is the not the correct type for field arrcode2 in message IdCodeDimFixed")
          }
        }
        case 8 => {
          if (value == null) this.arrcode3 = null;
          else {
            if (value.isInstanceOf[scala.Array[java.lang.Integer]])
              this.arrcode3 = value.asInstanceOf[scala.Array[java.lang.Integer]];
            else if (value.isInstanceOf[scala.Array[_]])
              this.arrcode3 = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[java.lang.Integer]);
            else throw new Exception(s"Value is the not the correct type for field arrcode3 in message IdCodeDimFixed")
          }
        }
        case 9 => {
          if (value == null) this.arrcode4 = null;
          else {
            if (value.isInstanceOf[scala.Array[java.lang.Boolean]])
              this.arrcode4 = value.asInstanceOf[scala.Array[java.lang.Boolean]];
            else if (value.isInstanceOf[scala.Array[_]])
              this.arrcode4 = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[java.lang.Boolean]);
            else throw new Exception(s"Value is the not the correct type for field arrcode4 in message IdCodeDimFixed")
          }
        }
        case 10 => {
          if (value == null) this.arrcode5 = null;
          else {
            if (value.isInstanceOf[scala.Array[java.lang.Long]])
              this.arrcode5 = value.asInstanceOf[scala.Array[java.lang.Long]];
            else if (value.isInstanceOf[scala.Array[_]])
              this.arrcode5 = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[java.lang.Long]);
            else throw new Exception(s"Value is the not the correct type for field arrcode5 in message IdCodeDimFixed")
          }
        }
        case 11 => {
          if (value == null) this.description = null;
          else {
            if (value.isInstanceOf[scala.Array[String]])
              this.description = value.asInstanceOf[scala.Array[String]];
            else if (value.isInstanceOf[scala.Array[_]])
              this.description = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[String]);
            else throw new Exception(s"Value is the not the correct type for field description in message IdCodeDimFixed")
          }
        }
        case 12 => {
          if (value == null) this.mapcodes1 = null;
          else {
            if (value.isInstanceOf[scala.collection.immutable.Map[String, java.lang.Integer]])
              this.mapcodes1 = value.asInstanceOf[scala.collection.immutable.Map[String, java.lang.Integer]];
            else throw new Exception(s"Value is the not the correct type for field mapcodes1 in message IdCodeDimFixed")
          }
        }
        case 13 => {
          if (value == null) this.mapcodes2 = null;
          else {
            if (value.isInstanceOf[scala.collection.immutable.Map[String, java.lang.Float]])
              this.mapcodes2 = value.asInstanceOf[scala.collection.immutable.Map[String, java.lang.Float]];
            else throw new Exception(s"Value is the not the correct type for field mapcodes2 in message IdCodeDimFixed")
          }
        }
        case 14 => {
          if (value == null) this.mapcodes3 = null;
          else {
            if (value.isInstanceOf[scala.collection.immutable.Map[String, java.lang.Double]])
              this.mapcodes3 = value.asInstanceOf[scala.collection.immutable.Map[String, java.lang.Double]];
            else throw new Exception(s"Value is the not the correct type for field mapcodes3 in message IdCodeDimFixed")
          }
        }

        case _ => throw new Exception(s"$index is a bad index for message IdCodeDimFixed");
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

  private def fromFunc(other: IdCodeDimFixed): IdCodeDimFixed = {
    this.id = com.ligadata.BaseTypes.IntImpl.Clone(other.id);
    this.code1 = com.ligadata.BaseTypes.FloatImpl.Clone(other.code1);
    this.code2 = com.ligadata.BaseTypes.DoubleImpl.Clone(other.code2);
    this.code3 = com.ligadata.BaseTypes.LongImpl.Clone(other.code3);
    this.code4 = com.ligadata.BaseTypes.CharImpl.Clone(other.code4);
    this.code5 = com.ligadata.BaseTypes.BoolImpl.Clone(other.code5);
    if (other.arrcode1 != null) {
      arrcode1 = new scala.Array[java.lang.Float](other.arrcode1.length);
      arrcode1 = other.arrcode1.map(v => com.ligadata.BaseTypes.FloatImpl.Clone(v).asInstanceOf[java.lang.Float]);
    } else this.arrcode1 = null;
    if (other.arrcode2 != null) {
      arrcode2 = new scala.Array[java.lang.Double](other.arrcode2.length);
      arrcode2 = other.arrcode2.map(v => com.ligadata.BaseTypes.DoubleImpl.Clone(v).asInstanceOf[java.lang.Double]);
    } else this.arrcode2 = null;
    if (other.arrcode3 != null) {
      arrcode3 = new scala.Array[java.lang.Integer](other.arrcode3.length);
      arrcode3 = other.arrcode3.map(v => com.ligadata.BaseTypes.IntImpl.Clone(v).asInstanceOf[java.lang.Integer]);
    } else this.arrcode3 = null;
    if (other.arrcode4 != null) {
      arrcode4 = new scala.Array[java.lang.Boolean](other.arrcode4.length);
      arrcode4 = other.arrcode4.map(v => com.ligadata.BaseTypes.BoolImpl.Clone(v).asInstanceOf[java.lang.Boolean]);
    } else this.arrcode4 = null;
    if (other.arrcode5 != null) {
      arrcode5 = new scala.Array[java.lang.Long](other.arrcode5.length);
      arrcode5 = other.arrcode5.map(v => com.ligadata.BaseTypes.LongImpl.Clone(v).asInstanceOf[java.lang.Long]);
    } else this.arrcode5 = null;
    if (other.description != null) {
      description = new scala.Array[String](other.description.length);
      description = other.description.map(v => com.ligadata.BaseTypes.StringImpl.Clone(v).asInstanceOf[String]);
    } else this.description = null;
    if (other.mapcodes1 != null) {
      this.mapcodes1 = other.mapcodes1.map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), com.ligadata.BaseTypes.IntImpl.Clone(a._2).asInstanceOf[java.lang.Integer]) }.toMap
    } else this.mapcodes1 = null;
    if (other.mapcodes2 != null) {
      this.mapcodes2 = other.mapcodes2.map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), com.ligadata.BaseTypes.FloatImpl.Clone(a._2).asInstanceOf[java.lang.Float]) }.toMap
    } else this.mapcodes2 = null;
    if (other.mapcodes3 != null) {
      this.mapcodes3 = other.mapcodes3.map { a => (com.ligadata.BaseTypes.StringImpl.Clone(a._1), com.ligadata.BaseTypes.DoubleImpl.Clone(a._2).asInstanceOf[java.lang.Double]) }.toMap
    } else this.mapcodes3 = null;

    this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
    return this;
  }

  def withid(value: java.lang.Integer): IdCodeDimFixed = {
    this.id = value
    return this
  }
  def withcode1(value: java.lang.Float): IdCodeDimFixed = {
    this.code1 = value
    return this
  }
  def withcode2(value: java.lang.Double): IdCodeDimFixed = {
    this.code2 = value
    return this
  }
  def withcode3(value: java.lang.Long): IdCodeDimFixed = {
    this.code3 = value
    return this
  }
  def withcode4(value: java.lang.Character): IdCodeDimFixed = {
    this.code4 = value
    return this
  }
  def withcode5(value: java.lang.Boolean): IdCodeDimFixed = {
    this.code5 = value
    return this
  }
  def witharrcode1(value: scala.Array[java.lang.Float]): IdCodeDimFixed = {
    this.arrcode1 = value
    return this
  }
  def witharrcode2(value: scala.Array[java.lang.Double]): IdCodeDimFixed = {
    this.arrcode2 = value
    return this
  }
  def witharrcode3(value: scala.Array[java.lang.Integer]): IdCodeDimFixed = {
    this.arrcode3 = value
    return this
  }
  def witharrcode4(value: scala.Array[java.lang.Boolean]): IdCodeDimFixed = {
    this.arrcode4 = value
    return this
  }
  def witharrcode5(value: scala.Array[java.lang.Long]): IdCodeDimFixed = {
    this.arrcode5 = value
    return this
  }
  def withdescription(value: scala.Array[String]): IdCodeDimFixed = {
    this.description = value
    return this
  }
  def withmapcodes1(value: scala.collection.immutable.Map[String, java.lang.Integer]): IdCodeDimFixed = {
    this.mapcodes1 = value
    return this
  }
  def withmapcodes2(value: scala.collection.immutable.Map[String, java.lang.Float]): IdCodeDimFixed = {
    this.mapcodes2 = value
    return this
  }
  def withmapcodes3(value: scala.collection.immutable.Map[String, java.lang.Double]): IdCodeDimFixed = {
    this.mapcodes3 = value
    return this
  }
  def isCaseSensitive(): Boolean = IdCodeDimFixed.isCaseSensitive();
  def caseSensitiveKey(keyName: String): String = {
    if (isCaseSensitive)
      return keyName;
    else return keyName.toLowerCase;
  }

  def this(factory: ContainerFactoryInterface) = {
    this(factory, null)
  }

  def this(other: IdCodeDimFixed) = {
    this(other.getFactory.asInstanceOf[ContainerFactoryInterface], other)
  }

}