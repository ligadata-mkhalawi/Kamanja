
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

    
 
object msg2 extends RDDObject[msg2] with MessageFactoryInterface { 
 
  val log = LogManager.getLogger(getClass)
	type T = msg2 ;
	override def getFullTypeName: String = "com.ligadata.kamanja.test.msg2"; 
	override def getTypeNameSpace: String = "com.ligadata.kamanja.test"; 
	override def getTypeName: String = "msg2"; 
	override def getTypeVersion: String = "000000.000001.000000"; 
	override def getSchemaId: Int = 0; 
	override def getTenantId: String = ""; 
	override def createInstance: msg2 = new msg2(msg2); 
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
  
    override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanja.test" , "name" : "msg2" , "fields":[{ "name" : "out1" , "type" : "int"},{ "name" : "out2" , "type" : "string"},{ "name" : "out3" , "type" : "int"},{ "name" : "out4" , "type" : "int"}]}""";  

    final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);
      
    override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
      try {
        if (oldVerobj == null) return null;
        oldVerobj match {
          
      case oldVerobj: com.ligadata.kamanja.test.V1000000.msg2 => { return  convertToVer1000000(oldVerobj); } 
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
  
    private def convertToVer1000000(oldVerobj: com.ligadata.kamanja.test.V1000000.msg2): com.ligadata.kamanja.test.V1000000.msg2= {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj msg2") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj msg2");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj msg2");
 override def NeedToTransformData: Boolean = false
    }

class msg2(factory: MessageFactoryInterface, other: msg2) extends MessageInterface(factory) { 
 
  val log = msg2.log

      var attributeTypes = generateAttributeTypes;
      
    private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
      var attributeTypes = new Array[AttributeTypeInfo](4);
   		 attributeTypes(0) = new AttributeTypeInfo("out1", 0, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(1) = new AttributeTypeInfo("out2", 1, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(2) = new AttributeTypeInfo("out3", 2, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(3) = new AttributeTypeInfo("out4", 3, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)

     
      return attributeTypes
    } 
    
		 var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;
    
     if (other != null && other != this) {
      // call copying fields from other to local variables
      fromFunc(other)
    }
    
    override def save: Unit = { msg2.saveOne(this) }
  
    def Clone(): ContainerOrConcept = { msg2.build(this) }

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
  
  
 		var out1: Int = _; 
 		var out2: String = _; 
 		var out3: Int = _; 
 		var out4: Int = _; 

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
      val fieldX = ru.typeOf[msg2].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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
   
      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container msg2", null);
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
   		case 0 => return this.out1.asInstanceOf[AnyRef]; 
		case 1 => return this.out2.asInstanceOf[AnyRef]; 
		case 2 => return this.out3.asInstanceOf[AnyRef]; 
		case 3 => return this.out4.asInstanceOf[AnyRef]; 

      	 case _ => throw new Exception(s"$index is a bad index for message msg2");
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
 				attributeVals(0) = new AttributeValue(this.out1, keyTypes("out1")) 
				attributeVals(1) = new AttributeValue(this.out2, keyTypes("out2")) 
				attributeVals(2) = new AttributeValue(this.out3, keyTypes("out3")) 
				attributeVals(3) = new AttributeValue(this.out4, keyTypes("out4")) 
       
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
   
  			 if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message msg2", null)
			 set(keyTypes(key).getIndex, value); 

      }catch {
          case e: Exception => {
          log.debug("", e)
          throw e
        }
      };
      
    }
  
      
    def set(index : Int, value :Any): Unit = {
      if (value == null) throw new Exception(s"Value is null for index $index in message msg2 ")
      try{
        index match {
 				case 0 => { 
				if(value.isInstanceOf[Int]) 
				  this.out1 = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field out1 in message msg2") 
				} 
				case 1 => { 
				if(value.isInstanceOf[String]) 
				  this.out2 = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field out2 in message msg2") 
				} 
				case 2 => { 
				if(value.isInstanceOf[Int]) 
				  this.out3 = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field out3 in message msg2") 
				} 
				case 3 => { 
				if(value.isInstanceOf[Int]) 
				  this.out4 = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field out4 in message msg2") 
				} 

        case _ => throw new Exception(s"$index is a bad index for message msg2");
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
  
    private def fromFunc(other: msg2): msg2 = {  
   			this.out1 = com.ligadata.BaseTypes.IntImpl.Clone(other.out1);
			this.out2 = com.ligadata.BaseTypes.StringImpl.Clone(other.out2);
			this.out3 = com.ligadata.BaseTypes.IntImpl.Clone(other.out3);
			this.out4 = com.ligadata.BaseTypes.IntImpl.Clone(other.out4);

      this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
      return this;
    }
    
	 def without1(value: Int) : msg2 = {
		 this.out1 = value 
		 return this 
 	 } 
	 def without2(value: String) : msg2 = {
		 this.out2 = value 
		 return this 
 	 } 
	 def without3(value: Int) : msg2 = {
		 this.out3 = value 
		 return this 
 	 } 
	 def without4(value: Int) : msg2 = {
		 this.out4 = value 
		 return this 
 	 } 
    def isCaseSensitive(): Boolean = msg2.isCaseSensitive(); 
    def caseSensitiveKey(keyName: String): String = {
      if(isCaseSensitive)
        return keyName;
      else return keyName.toLowerCase;
    }


    
    def this(factory:MessageFactoryInterface) = {
      this(factory, null)
     }
    
    def this(other: msg2) = {
      this(other.getFactory.asInstanceOf[MessageFactoryInterface], other)
    }

}