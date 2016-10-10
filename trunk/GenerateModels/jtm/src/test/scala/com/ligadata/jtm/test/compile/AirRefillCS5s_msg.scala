
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

    
 
object AirRefillCS5s extends RDDObject[AirRefillCS5s] with MessageFactoryInterface { 
 
  val log = LogManager.getLogger(getClass)
	type T = AirRefillCS5s ;
	override def getFullTypeName: String = "com.ligadata.messages.AirRefillCS5s"; 
	override def getTypeNameSpace: String = "com.ligadata.messages"; 
	override def getTypeName: String = "AirRefillCS5s"; 
	override def getTypeVersion: String = "000000.000001.000000"; 
	override def getSchemaId: Int = 0; 
	override def getTenantId: String = ""; 
	override def createInstance: AirRefillCS5s = new AirRefillCS5s(AirRefillCS5s); 
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
  
    override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "airrefillcs5s" , "fields":[{ "name" : "f1" , "type" : "double"},{ "name" : "f2" , "type" : "double"},{ "name" : "originnodetype" , "type" : "string"},{ "name" : "originhostname" , "type" : "string"}]}""";  

    final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);
      
    override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
      try {
        if (oldVerobj == null) return null;
        oldVerobj match {
          
      case oldVerobj: com.ligadata.messages.V1000000.AirRefillCS5s => { return  convertToVer1000000(oldVerobj); } 
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
  
    private def convertToVer1000000(oldVerobj: com.ligadata.messages.V1000000.AirRefillCS5s): com.ligadata.messages.V1000000.AirRefillCS5s= {
      return oldVerobj
    }
  
      
  /****   DEPRECATED METHODS ***/
  override def FullName: String = getFullTypeName
  override def NameSpace: String = getTypeNameSpace
  override def Name: String = getTypeName
  override def Version: String = getTypeVersion
  override def CreateNewMessage: BaseMsg= createInstance.asInstanceOf[BaseMsg];
  override def CreateNewContainer: BaseContainer= null;
  override def IsFixed: Boolean = true
  override def IsKv: Boolean = false
  override def CanPersist: Boolean = false
  override def isMessage: Boolean = true
  override def isContainer: Boolean = false
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj AirRefillCS5s") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj AirRefillCS5s");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj AirRefillCS5s");
 override def NeedToTransformData: Boolean = false
    }

class AirRefillCS5s(factory: MessageFactoryInterface, other: AirRefillCS5s) extends MessageInterface(factory) { 
 
  val log = AirRefillCS5s.log

      var attributeTypes = generateAttributeTypes;
      
    private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
      var attributeTypes = new Array[AttributeTypeInfo](4);
   		 attributeTypes(0) = new AttributeTypeInfo("f1", 0, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(1) = new AttributeTypeInfo("f2", 1, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(2) = new AttributeTypeInfo("originnodetype", 2, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(3) = new AttributeTypeInfo("originhostname", 3, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)

     
      return attributeTypes
    } 
    
		 var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;
    
     if (other != null && other != this) {
      // call copying fields from other to local variables
      fromFunc(other)
    }
    
    override def save: Unit = { AirRefillCS5s.saveOne(this) }
  
    def Clone(): ContainerOrConcept = { AirRefillCS5s.build(this) }

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
  
  
 		var f1: Double = _; 
 		var f2: Double = _; 
 		var originnodetype: String = _; 
 		var originhostname: String = _; 

    override def getAttributeTypes(): Array[AttributeTypeInfo] = {
      if (attributeTypes == null) return null;
      return attributeTypes
    }
    
    private def getWithReflection(keyName: String): AnyRef = {
      if(keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = caseSensitiveKey(keyName);
      val ru = scala.reflect.runtime.universe
      val m = ru.runtimeMirror(getClass.getClassLoader)
      val im = m.reflect(this)
      val fieldX = ru.typeOf[AirRefillCS5s].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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
     if(keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = caseSensitiveKey(keyName);
   
      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container AirRefillCS5s", null);
      return get(keyTypes(key).getIndex)
  }
  
    override def getOrElse(keyName: String, defaultVal: Any): AnyRef = { // Return (value)
      if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = caseSensitiveKey(keyName);
      try {
        return get(key)
       } catch {
        case e: Exception => {
          log.debug("", e)
          if(defaultVal == null) return null;
          return defaultVal.asInstanceOf[AnyRef];
        }
      }
      return null;
    }
   
      
    override def get(index : Int) : AnyRef = { // Return (value, type)
      try{
        index match {
   		case 0 => return this.f1.asInstanceOf[AnyRef]; 
		case 1 => return this.f2.asInstanceOf[AnyRef]; 
		case 2 => return this.originnodetype.asInstanceOf[AnyRef]; 
		case 3 => return this.originhostname.asInstanceOf[AnyRef]; 

      	 case _ => throw new Exception(s"$index is a bad index for message AirRefillCS5s");
    	  }       
     }catch {
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
          if(defaultVal == null) return null;
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
      try{
 				attributeVals(0) = new AttributeValue(this.f1, keyTypes("f1")) 
				attributeVals(1) = new AttributeValue(this.f2, keyTypes("f2")) 
				attributeVals(2) = new AttributeValue(this.originnodetype, keyTypes("originnodetype")) 
				attributeVals(3) = new AttributeValue(this.originhostname, keyTypes("originhostname")) 
       
      }catch {
          case e: Exception => {
          log.debug("", e)
          throw e
        }
      };
      
      return attributeVals;
    }      
    
    override def set(keyName: String, value: Any) = {
      if(keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = caseSensitiveKey(keyName);
      try {
   
  			 if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message AirRefillCS5s", null)
			 set(keyTypes(key).getIndex, value); 

      }catch {
          case e: Exception => {
          log.debug("", e)
          throw e
        }
      };
      
    }
  
      
    def set(index : Int, value :Any): Unit = {
      if (value == null) throw new Exception(s"Value is null for index $index in message AirRefillCS5s ")
      try{
        index match {
 				case 0 => { 
				if(value.isInstanceOf[Double]) 
				  this.f1 = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field f1 in message AirRefillCS5s") 
				} 
				case 1 => { 
				if(value.isInstanceOf[Double]) 
				  this.f2 = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field f2 in message AirRefillCS5s") 
				} 
				case 2 => { 
				if(value.isInstanceOf[String]) 
				  this.originnodetype = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field originnodetype in message AirRefillCS5s") 
				} 
				case 3 => { 
				if(value.isInstanceOf[String]) 
				  this.originhostname = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field originhostname in message AirRefillCS5s") 
				} 

        case _ => throw new Exception(s"$index is a bad index for message AirRefillCS5s");
        }
    	}catch {
          case e: Exception => {
          log.debug("", e)
          throw e
        }
      };
      
    }      
    
    override def set(key: String, value: Any, valTyp: String) = {
      throw new Exception ("Set Func for Value and ValueType By Key is not supported for Fixed Messages" )
    }
  
    private def fromFunc(other: AirRefillCS5s): AirRefillCS5s = {  
   			this.f1 = com.ligadata.BaseTypes.DoubleImpl.Clone(other.f1);
			this.f2 = com.ligadata.BaseTypes.DoubleImpl.Clone(other.f2);
			this.originnodetype = com.ligadata.BaseTypes.StringImpl.Clone(other.originnodetype);
			this.originhostname = com.ligadata.BaseTypes.StringImpl.Clone(other.originhostname);

      this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
      return this;
    }
    
	 def withf1(value: Double) : AirRefillCS5s = {
		 this.f1 = value 
		 return this 
 	 } 
	 def withf2(value: Double) : AirRefillCS5s = {
		 this.f2 = value 
		 return this 
 	 } 
	 def withoriginnodetype(value: String) : AirRefillCS5s = {
		 this.originnodetype = value 
		 return this 
 	 } 
	 def withoriginhostname(value: String) : AirRefillCS5s = {
		 this.originhostname = value 
		 return this 
 	 } 
    def isCaseSensitive(): Boolean = AirRefillCS5s.isCaseSensitive(); 
    def caseSensitiveKey(keyName: String): String = {
      if(isCaseSensitive)
        return keyName;
      else return keyName.toLowerCase;
    }


    
    def this(factory:MessageFactoryInterface) = {
      this(factory, null)
     }
    
    def this(other: AirRefillCS5s) = {
      this(other.getFactory.asInstanceOf[MessageFactoryInterface], other)
    }

}