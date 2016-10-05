
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

    
 
object arrin1 extends RDDObject[arrin1] with MessageFactoryInterface { 
 
  val log = LogManager.getLogger(getClass)
	type T = arrin1 ;
	override def getFullTypeName: String = "com.ligadata.kamanja.test.arrin1"; 
	override def getTypeNameSpace: String = "com.ligadata.kamanja.test"; 
	override def getTypeName: String = "arrin1"; 
	override def getTypeVersion: String = "000000.000001.000000"; 
	override def getSchemaId: Int = 0; 
	override def getTenantId: String = ""; 
	override def createInstance: arrin1 = new arrin1(arrin1); 
	override def isFixed: Boolean = true; 
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
  
    override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanja.test" , "name" : "arrin1" , "fields":[{ "name" : "ext_type_id" , "type" : "string"},{ "name" : "mydata" , "type" :  {"type" : "array", "items" : "string"}}]}""";  

    final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);
      
    override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
      try {
        if (oldVerobj == null) return null;
        oldVerobj match {
          
      case oldVerobj: com.ligadata.kamanja.test.V1000000.arrin1 => { return  convertToVer1000000(oldVerobj); } 
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
  
    private def convertToVer1000000(oldVerobj: com.ligadata.kamanja.test.V1000000.arrin1): com.ligadata.kamanja.test.V1000000.arrin1= {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj arrin1") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj arrin1");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj arrin1");
 override def NeedToTransformData: Boolean = false
    }

class arrin1(factory: MessageFactoryInterface, other: arrin1) extends MessageInterface(factory) { 
 
  val log = arrin1.log

      var attributeTypes = generateAttributeTypes;
      
    private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
      var attributeTypes = new Array[AttributeTypeInfo](2);
   		 attributeTypes(0) = new AttributeTypeInfo("ext_type_id", 0, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(1) = new AttributeTypeInfo("mydata", 1, AttributeTypeInfo.TypeCategory.ARRAY, 1, -1, 0)

     
      return attributeTypes
    } 
    
		 var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;
    
     if (other != null && other != this) {
      // call copying fields from other to local variables
      fromFunc(other)
    }
    
    override def save: Unit = { /* arrin1.saveOne(this) */}
  
    def Clone(): ContainerOrConcept = { arrin1.build(this) }

		override def getPartitionKey: Array[String] = Array[String]() 

		override def getPrimaryKey: Array[String] = Array[String]() 

    override def getAttributeType(name: String): AttributeTypeInfo = {
      if (name == null || name.trim() == "") return null;
      attributeTypes.foreach(attributeType => {
        if(attributeType.getName == name.toLowerCase())
          return attributeType
      }) 
      return null;
    }
  
  
 		var ext_type_id: String = _; 
 		var mydata: scala.Array[String] = _; 

    override def getAttributeTypes(): Array[AttributeTypeInfo] = {
      if (attributeTypes == null) return null;
      return attributeTypes
    }
    
    private def getWithReflection(keyName: String): AnyRef = {
      if(keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = keyName.toLowerCase;
      val ru = scala.reflect.runtime.universe
      val m = ru.runtimeMirror(getClass.getClassLoader)
      val im = m.reflect(this)
      val fieldX = ru.typeOf[arrin1].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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
     if(keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = keyName.toLowerCase;
   
      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container arrin1", null);
      return get(keyTypes(key).getIndex)
  }
  
    override def getOrElse(keyName: String, defaultVal: Any): AnyRef = { // Return (value, type)
      if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name "+keyName);
      val key = keyName.toLowerCase;
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
   		case 0 => return this.ext_type_id.asInstanceOf[AnyRef]; 
		case 1 => return this.mydata.asInstanceOf[AnyRef]; 

      	 case _ => throw new Exception(s"$index is a bad index for message arrin1");
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
      var attributeVals = new Array[AttributeValue](2);
      try{
 				attributeVals(0) = new AttributeValue(this.ext_type_id, keyTypes("ext_type_id")) 
				attributeVals(1) = new AttributeValue(this.mydata, keyTypes("mydata")) 
       
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
      val key = keyName.toLowerCase;
      try {
   
  			 if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message arrin1", null)
			 set(keyTypes(key).getIndex, value); 

      }catch {
          case e: Exception => {
          log.debug("", e)
          throw e
        }
      };
      
    }
  
      
    def set(index : Int, value :Any): Unit = {
      if (value == null) throw new Exception(s"Value is null for index $index in message arrin1 ")
      try{
        index match {
 				case 0 => { 
				if(value.isInstanceOf[String]) 
				  this.ext_type_id = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field ext_type_id in message arrin1") 
				} 
				case 1 => { 
				if(value.isInstanceOf[scala.Array[String]]) 
				  this.mydata = value.asInstanceOf[scala.Array[String]]; 
				else if(value.isInstanceOf[scala.Array[_]]) 
				  this.mydata = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[String]); 
				 else throw new Exception(s"Value is the not the correct type for field mydata in message arrin1") 
				} 

        case _ => throw new Exception(s"$index is a bad index for message arrin1");
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
  
    private def fromFunc(other: arrin1): arrin1 = {  
   			this.ext_type_id = com.ligadata.BaseTypes.StringImpl.Clone(other.ext_type_id);
		 if (other.mydata != null ) { 
		 mydata = new scala.Array[String](other.mydata.length); 
		 mydata = other.mydata.map(v => com.ligadata.BaseTypes.StringImpl.Clone(v)); 
		 } 
		 else this.mydata = null; 

      this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
      return this;
    }
    
	 def withext_type_id(value: String) : arrin1 = {
		 this.ext_type_id = value 
		 return this 
 	 } 
	 def withmydata(value: scala.Array[String]) : arrin1 = {
		 this.mydata = value 
		 return this 
 	 } 

    def this(factory:MessageFactoryInterface) = {
      this(factory, null)
     }
    
    def this(other: arrin1) = {
      this(other.getFactory.asInstanceOf[MessageFactoryInterface], other)
    }

}