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



object KamanjaModelEvent extends RDDObject[KamanjaModelEvent] with ContainerFactoryInterface { 
  
  val log = LogManager.getLogger(getClass)
  type T = KamanjaModelEvent ;
  override def getFullTypeName: String = "com.ligadata.KamanjaBase.KamanjaModelEvent"; 
  override def getTypeNameSpace: String = "com.ligadata.KamanjaBase"; 
  override def getTypeName: String = "KamanjaModelEvent"; 
  override def getTypeVersion: String = "000001.000000.000000"; 
  override def getSchemaId: Int = 5; 
  override def getTenantId: String = "system"; 
  override def createInstance: KamanjaModelEvent = new KamanjaModelEvent(KamanjaModelEvent); 
  override def isFixed: Boolean = true; 
  override def getContainerType: ContainerTypes.ContainerType = ContainerTypes.ContainerType.CONTAINER
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
  
  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanjabase" , "name" : "kamanjamodelevent" , "fields":[{ "name" : "modelid" , "type" : "long"},{ "name" : "elapsedtimeinms" , "type" : "float"},{ "name" : "eventepochtime" , "type" : "long"},{ "name" : "isresultproduced" , "type" : "boolean"},{ "name" : "consumedmessages" , "type" :  {"type" : "array", "items" : "long"}},{ "name" : "producedmessages" , "type" :  {"type" : "array", "items" : "long"}},{ "name" : "consumedcontainers" , "type" :  {"type" : "array", "items" : "long"}},{ "name" : "producedcontainers" , "type" :  {"type" : "array", "items" : "long"}},{ "name" : "error" , "type" : "string"}]}""";  

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);
  
  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {
          
	case oldVerobj: com.ligadata.KamanjaBase.KamanjaModelEvent => { return  convertToVer1000000000000(oldVerobj); } 
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
  
  private def convertToVer1000000000000(oldVerobj: com.ligadata.KamanjaBase.KamanjaModelEvent): com.ligadata.KamanjaBase.KamanjaModelEvent= {
    return oldVerobj
  }
  
  /****   DEPRECATED METHODS ***/
  override def FullName: String = getFullTypeName
  override def NameSpace: String = getTypeNameSpace
  override def Name: String = getTypeName
  override def Version: String = getTypeVersion
  override def CreateNewMessage: BaseMsg= null;
  override def CreateNewContainer: BaseContainer= createInstance.asInstanceOf[BaseContainer];
  override def IsFixed: Boolean = true
  override def IsKv: Boolean = false
  override def CanPersist: Boolean = false
  override def isMessage: Boolean = false
  override def isContainer: Boolean = true
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj KamanjaModelEvent") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj KamanjaModelEvent");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj KamanjaModelEvent");
  override def NeedToTransformData: Boolean = false
}

class KamanjaModelEvent(factory: ContainerFactoryInterface, other: KamanjaModelEvent) extends ContainerInterface(factory) { 
  
  val log = KamanjaModelEvent.log

  var attributeTypes = generateAttributeTypes;
  
  private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
    var attributeTypes = new Array[AttributeTypeInfo](9);
    attributeTypes(0) = new AttributeTypeInfo("modelid", 0, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
    attributeTypes(1) = new AttributeTypeInfo("elapsedtimeinms", 1, AttributeTypeInfo.TypeCategory.FLOAT, -1, -1, 0)
    attributeTypes(2) = new AttributeTypeInfo("eventepochtime", 2, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
    attributeTypes(3) = new AttributeTypeInfo("isresultproduced", 3, AttributeTypeInfo.TypeCategory.BOOLEAN, -1, -1, 0)
    attributeTypes(4) = new AttributeTypeInfo("consumedmessages", 4, AttributeTypeInfo.TypeCategory.ARRAY, 4, -1, 0)
    attributeTypes(5) = new AttributeTypeInfo("producedmessages", 5, AttributeTypeInfo.TypeCategory.ARRAY, 4, -1, 0)
    attributeTypes(6) = new AttributeTypeInfo("consumedcontainers", 6, AttributeTypeInfo.TypeCategory.ARRAY, 4, -1, 0)
    attributeTypes(7) = new AttributeTypeInfo("producedcontainers", 7, AttributeTypeInfo.TypeCategory.ARRAY, 4, -1, 0)
    attributeTypes(8) = new AttributeTypeInfo("error", 8, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)

    
    return attributeTypes
  } 
  
  var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;
  
  if (other != null && other != this) {
    // call copying fields from other to local variables
    fromFunc(other)
  }
  
  override def save: Unit = { /* KamanjaModelEvent.saveOne(this) */ }
  
  def Clone(): ContainerOrConcept = { KamanjaModelEvent.build(this) }

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
  
  
  var modelid: Long = _; 
  var elapsedtimeinms: Float = _; 
  var eventepochtime: Long = _; 
  var isresultproduced: Boolean = _; 
  var consumedmessages: scala.Array[Long] = _; 
  var producedmessages: scala.Array[Long] = _; 
  var consumedcontainers: scala.Array[Long] = _; 
  var producedcontainers: scala.Array[Long] = _; 
  var error: String = _; 

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
    val fieldX = ru.typeOf[KamanjaModelEvent].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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
    
    if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container KamanjaModelEvent", null);
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
   	case 0 => return this.modelid.asInstanceOf[AnyRef]; 
	case 1 => return this.elapsedtimeinms.asInstanceOf[AnyRef]; 
	case 2 => return this.eventepochtime.asInstanceOf[AnyRef]; 
	case 3 => return this.isresultproduced.asInstanceOf[AnyRef]; 
	case 4 => return this.consumedmessages.asInstanceOf[AnyRef]; 
	case 5 => return this.producedmessages.asInstanceOf[AnyRef]; 
	case 6 => return this.consumedcontainers.asInstanceOf[AnyRef]; 
	case 7 => return this.producedcontainers.asInstanceOf[AnyRef]; 
	case 8 => return this.error.asInstanceOf[AnyRef]; 

      	case _ => throw new Exception(s"$index is a bad index for message KamanjaModelEvent");
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
    var attributeVals = new Array[AttributeValue](9);
    try{
      attributeVals(0) = new AttributeValue(this.modelid, keyTypes("modelid")) 
      attributeVals(1) = new AttributeValue(this.elapsedtimeinms, keyTypes("elapsedtimeinms")) 
      attributeVals(2) = new AttributeValue(this.eventepochtime, keyTypes("eventepochtime")) 
      attributeVals(3) = new AttributeValue(this.isresultproduced, keyTypes("isresultproduced")) 
      attributeVals(4) = new AttributeValue(this.consumedmessages, keyTypes("consumedmessages")) 
      attributeVals(5) = new AttributeValue(this.producedmessages, keyTypes("producedmessages")) 
      attributeVals(6) = new AttributeValue(this.consumedcontainers, keyTypes("consumedcontainers")) 
      attributeVals(7) = new AttributeValue(this.producedcontainers, keyTypes("producedcontainers")) 
      attributeVals(8) = new AttributeValue(this.error, keyTypes("error")) 
      
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
      
      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message KamanjaModelEvent", null)
      set(keyTypes(key).getIndex, value); 

    }catch {
      case e: Exception => {
        log.debug("", e)
        throw e
      }
    };
    
  }
  
  
  def set(index : Int, value :Any): Unit = {
    if (value == null) throw new Exception(s"Value is null for index $index in message KamanjaModelEvent ")
    try{
      index match {
 	case 0 => { 
	  if(value.isInstanceOf[Long]) 
	    this.modelid = value.asInstanceOf[Long]; 
	  else throw new Exception(s"Value is the not the correct type for field modelid in message KamanjaModelEvent") 
	} 
	case 1 => { 
	  if(value.isInstanceOf[Float]) 
	    this.elapsedtimeinms = value.asInstanceOf[Float]; 
	  else throw new Exception(s"Value is the not the correct type for field elapsedtimeinms in message KamanjaModelEvent") 
	} 
	case 2 => { 
	  if(value.isInstanceOf[Long]) 
	    this.eventepochtime = value.asInstanceOf[Long]; 
	  else throw new Exception(s"Value is the not the correct type for field eventepochtime in message KamanjaModelEvent") 
	} 
	case 3 => { 
	  if(value.isInstanceOf[Boolean]) 
	    this.isresultproduced = value.asInstanceOf[Boolean]; 
	  else throw new Exception(s"Value is the not the correct type for field isresultproduced in message KamanjaModelEvent") 
	} 
	case 4 => { 
	  if(value.isInstanceOf[scala.Array[Long]]) 
	    this.consumedmessages = value.asInstanceOf[scala.Array[Long]]; 
	  else if(value.isInstanceOf[scala.Array[_]]) 
		 this.consumedmessages = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[Long]); 
	  else throw new Exception(s"Value is the not the correct type for field consumedmessages in message KamanjaModelEvent") 
	} 
	case 5 => { 
	  if(value.isInstanceOf[scala.Array[Long]]) 
	    this.producedmessages = value.asInstanceOf[scala.Array[Long]]; 
	  else if(value.isInstanceOf[scala.Array[_]]) 
		 this.producedmessages = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[Long]); 
	  else throw new Exception(s"Value is the not the correct type for field producedmessages in message KamanjaModelEvent") 
	} 
	case 6 => { 
	  if(value.isInstanceOf[scala.Array[Long]]) 
	    this.consumedcontainers = value.asInstanceOf[scala.Array[Long]]; 
	  else if(value.isInstanceOf[scala.Array[_]]) 
		 this.consumedcontainers = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[Long]); 
	  else throw new Exception(s"Value is the not the correct type for field consumedcontainers in message KamanjaModelEvent") 
	} 
	case 7 => { 
	  if(value.isInstanceOf[scala.Array[Long]]) 
	    this.producedcontainers = value.asInstanceOf[scala.Array[Long]]; 
	  else if(value.isInstanceOf[scala.Array[_]]) 
		 this.producedcontainers = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[Long]); 
	  else throw new Exception(s"Value is the not the correct type for field producedcontainers in message KamanjaModelEvent") 
	} 
	case 8 => { 
	  if(value.isInstanceOf[String]) 
	    this.error = value.asInstanceOf[String]; 
	  else throw new Exception(s"Value is the not the correct type for field error in message KamanjaModelEvent") 
	} 

        case _ => throw new Exception(s"$index is a bad index for message KamanjaModelEvent");
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
  
  private def fromFunc(other: KamanjaModelEvent): KamanjaModelEvent = {  
    this.modelid = com.ligadata.BaseTypes.LongImpl.Clone(other.modelid);
    this.elapsedtimeinms = com.ligadata.BaseTypes.FloatImpl.Clone(other.elapsedtimeinms);
    this.eventepochtime = com.ligadata.BaseTypes.LongImpl.Clone(other.eventepochtime);
    this.isresultproduced = com.ligadata.BaseTypes.BoolImpl.Clone(other.isresultproduced);
    if (other.consumedmessages != null ) { 
      consumedmessages = new scala.Array[Long](other.consumedmessages.length); 
      consumedmessages = other.consumedmessages.map(v => com.ligadata.BaseTypes.LongImpl.Clone(v)); 
    } 
    else this.consumedmessages = null; 
    if (other.producedmessages != null ) { 
      producedmessages = new scala.Array[Long](other.producedmessages.length); 
      producedmessages = other.producedmessages.map(v => com.ligadata.BaseTypes.LongImpl.Clone(v)); 
    } 
    else this.producedmessages = null; 
    if (other.consumedcontainers != null ) { 
      consumedcontainers = new scala.Array[Long](other.consumedcontainers.length); 
      consumedcontainers = other.consumedcontainers.map(v => com.ligadata.BaseTypes.LongImpl.Clone(v)); 
    } 
    else this.consumedcontainers = null; 
    if (other.producedcontainers != null ) { 
      producedcontainers = new scala.Array[Long](other.producedcontainers.length); 
      producedcontainers = other.producedcontainers.map(v => com.ligadata.BaseTypes.LongImpl.Clone(v)); 
    } 
    else this.producedcontainers = null; 
    this.error = com.ligadata.BaseTypes.StringImpl.Clone(other.error);

    this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
    return this;
  }
  
  def withmodelid(value: Long) : KamanjaModelEvent = {
    this.modelid = value 
    return this 
  } 
  def withelapsedtimeinms(value: Float) : KamanjaModelEvent = {
    this.elapsedtimeinms = value 
    return this 
  } 
  def witheventepochtime(value: Long) : KamanjaModelEvent = {
    this.eventepochtime = value 
    return this 
  } 
  def withisresultproduced(value: Boolean) : KamanjaModelEvent = {
    this.isresultproduced = value 
    return this 
  } 
  def withconsumedmessages(value: scala.Array[Long]) : KamanjaModelEvent = {
    this.consumedmessages = value 
    return this 
  } 
  def withproducedmessages(value: scala.Array[Long]) : KamanjaModelEvent = {
    this.producedmessages = value 
    return this 
  } 
  def withconsumedcontainers(value: scala.Array[Long]) : KamanjaModelEvent = {
    this.consumedcontainers = value 
    return this 
  } 
  def withproducedcontainers(value: scala.Array[Long]) : KamanjaModelEvent = {
    this.producedcontainers = value 
    return this 
  } 
  def witherror(value: String) : KamanjaModelEvent = {
    this.error = value 
    return this 
  } 

  def this(factory:ContainerFactoryInterface) = {
    this(factory, null)
  }
  
  def this(other: KamanjaModelEvent) = {
    this(other.getFactory.asInstanceOf[ContainerFactoryInterface], other)
  }

}
