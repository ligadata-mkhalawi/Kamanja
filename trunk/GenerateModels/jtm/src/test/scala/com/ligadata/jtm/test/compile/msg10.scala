
package com.ligadata.kamanja.test.V1000000; 

import org.json4s.jackson.JsonMethods._;
import org.json4s.DefaultFormats;
import org.json4s.Formats;
import com.ligadata.KamanjaBase._;
import com.ligadata.BaseTypes._;
import com.ligadata.Exceptions._;
import org.apache.logging.log4j.{ Logger, LogManager }
import java.util.Date;
import java.io.{ DataInputStream, DataOutputStream, ByteArrayOutputStream }

    
 
object varin1 extends RDDObject[varin1] with MessageFactoryInterface { 
 
  val log = LogManager.getLogger(getClass)
	type T = varin1 ;
	override def getFullTypeName: String = "com.ligadata.kamanja.test.varin1"; 
	override def getTypeNameSpace: String = "com.ligadata.kamanja.test"; 
	override def getTypeName: String = "varin1"; 
	override def getTypeVersion: String = "000000.000001.000000"; 
	override def getSchemaId: Int = 0; 
	override def getTenantId: String = ""; 
	override def createInstance: varin1 = new varin1(varin1); 
	override def isFixed: Boolean = false; 
	def isCaseSensitive(): Boolean = false; 
	override def getContainerType: ContainerTypes.ContainerType = ContainerTypes.ContainerType.MESSAGE
	override def getFullName = getFullTypeName; 
	override def getRddTenantId = getTenantId; 
	override def toJavaRDDObject: JavaRDDObject[T] = JavaRDDObject.fromRDDObject[T](this); 

    def build = new T(this)
    def build(from: T) = new T(from)
   override def getPartitionKeyNames: Array[String] = Array[String](); 

  override def getPrimaryKeyNames: Array[String] = Array[String](); 
   
  
  override def getTimePartitionInfo: TimePartitionInfo = { return null;}  // FieldName, Format & Time Partition Types(Daily/Monthly/Yearly)
  
       
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
  
    override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanja.test" , "name" : "varin1" , "fields":[{ "name" : "in1" , "type" : "string"},{ "name" : "in2" , "type" : "int"}]}""";  

    final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);
      
    override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
      try {
        if (oldVerobj == null) return null;
        oldVerobj match {
          
      case oldVerobj: com.ligadata.kamanja.test.V1000000.varin1 => { return  convertToVer1000000(oldVerobj); } 
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
  
    private def convertToVer1000000(oldVerobj: com.ligadata.kamanja.test.V1000000.varin1): com.ligadata.kamanja.test.V1000000.varin1= {
      return oldVerobj
    }
  
      
  /****   DEPRECATED METHODS ***/
  override def FullName: String = getFullTypeName
  override def NameSpace: String = getTypeNameSpace
  override def Name: String = getTypeName
  override def Version: String = getTypeVersion
  override def CreateNewMessage: BaseMsg= createInstance.asInstanceOf[BaseMsg];
  override def CreateNewContainer: BaseContainer= null;
  override def IsFixed: Boolean = false
  override def IsKv: Boolean = true
  override def CanPersist: Boolean = false
  override def isMessage: Boolean = true
  override def isContainer: Boolean = false
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj varin1") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj varin1");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj varin1");
 override def NeedToTransformData: Boolean = false
    }

class varin1(factory: MessageFactoryInterface, other: varin1) extends MessageInterface(factory) { 
 
  val log = varin1.log

      var attributeTypes = generateAttributeTypes;
      
    private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
      var attributeTypes = new Array[AttributeTypeInfo](2);
   		 attributeTypes(0) = new AttributeTypeInfo("in1", 0, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(1) = new AttributeTypeInfo("in2", 1, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)

     
      return attributeTypes
    } 
    
		 var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;
    
     if (other != null && other != this) {
      // call copying fields from other to local variables
      fromFunc(other)
    }
    
    override def save: Unit = { varin1.saveOne(this) }
  
    def Clone(): ContainerOrConcept = { varin1.build(this) }

		override def getPartitionKey: Array[String] = Array[String]() 

		override def getPrimaryKey: Array[String] = Array[String]() 

    override def getAttributeType(name: String): AttributeTypeInfo = {
      if (name == null || name.trim() == "") return null;
      attributeTypes.foreach(attributeType => {
        if(attributeType.getName == caseSensitiveKey(name))
          return attributeType
      }) 
      return null;
    }
  
  
    var valuesMap = scala.collection.mutable.Map[String, AttributeValue]()
 
    override def getAttributeTypes(): Array[AttributeTypeInfo] = {
      val attributeTyps = valuesMap.map(f => f._2.getValueType).toArray;
      if (attributeTyps == null) return null else return attributeTyps
    }   
 
    override def get(keyName: String): AnyRef = { // Return (value)
      if(keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " +keyName);
      val key = caseSensitiveKey(keyName);
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
      if(keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = caseSensitiveKey(keyName);
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

    override def get(index: Int): AnyRef = { // Return (value)
      throw new Exception("Get By Index is not supported in mapped messages");
    }

    override def getOrElse(index: Int, defaultVal: Any): AnyRef = { // Return (value,  type)
      throw new Exception("Get By Index is not supported in mapped messages");
    }  
    
    override def getAllAttributeValues(): Array[AttributeValue] = { // Has (name, value, type))
      return valuesMap.map(f => f._2).toArray;
    }  
    
    override def set(keyName: String, value: Any) = {
      if(keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = caseSensitiveKey(keyName);
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
       if(keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
       val key = caseSensitiveKey(keyName);      
       try{
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
    
    private def fromFunc(other: varin1): varin1 = {  
      
     if (other.valuesMap != null) {
      other.valuesMap.foreach(vMap => {
        val key = caseSensitiveKey(vMap._1)
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
        
       
      this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));   
      return this;
    }
    
	 def within1(value: String) : varin1 = {
		 valuesMap("in1") = new AttributeValue(value, keyTypes("in1")) 
	 return this 
 	 } 
	 def within2(value: Int) : varin1 = {
		 valuesMap("in2") = new AttributeValue(value, keyTypes("in2")) 
	 return this 
 	 } 
    def isCaseSensitive(): Boolean = varin1.isCaseSensitive(); 
    def caseSensitiveKey(keyName: String): String = {
      if(isCaseSensitive)
        return keyName;
      else return keyName.toLowerCase;
    }


    
    def this(factory:MessageFactoryInterface) = {
      this(factory, null)
     }
    
    def this(other: varin1) = {
      this(other.getFactory.asInstanceOf[MessageFactoryInterface], other)
    }

}