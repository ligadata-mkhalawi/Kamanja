
package com.ligadata.kamanja.samples.messages.V1000000; 

import org.json4s.jackson.JsonMethods._;
import org.json4s.DefaultFormats;
import org.json4s.Formats;
import com.ligadata.KamanjaBase._;
import com.ligadata.BaseTypes._;
import com.ligadata.Exceptions._;
import org.apache.logging.log4j.{ Logger, LogManager }
import java.util.Date;
import java.io.{ DataInputStream, DataOutputStream, ByteArrayOutputStream }

    
 
object OutpatientClaim extends RDDObject[OutpatientClaim] with MessageFactoryInterface { 
 
  val log = LogManager.getLogger(getClass)
	type T = OutpatientClaim ;
	override def getFullTypeName: String = "com.ligadata.kamanja.samples.messages.OutpatientClaim"; 
	override def getTypeNameSpace: String = "com.ligadata.kamanja.samples.messages"; 
	override def getTypeName: String = "OutpatientClaim"; 
	override def getTypeVersion: String = "000000.000001.000000"; 
	override def getSchemaId: Int = 0; 
	override def getTenantId: String = ""; 
	override def createInstance: OutpatientClaim = new OutpatientClaim(OutpatientClaim); 
	override def isFixed: Boolean = true; 
	def isCaseSensitive(): Boolean = false; 
	override def getContainerType: ContainerTypes.ContainerType = ContainerTypes.ContainerType.MESSAGE
	override def getFullName = getFullTypeName; 
	override def getRddTenantId = getTenantId; 
	override def toJavaRDDObject: JavaRDDObject[T] = JavaRDDObject.fromRDDObject[T](this); 

    def build = new T(this)
    def build(from: T) = new T(from)
   override def getPartitionKeyNames: Array[String] = Array("desynpuf_id"); 

  override def getPrimaryKeyNames: Array[String] = Array("desynpuf_id", "clm_id"); 
   
  
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
  
    override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanja.samples.messages" , "name" : "outpatientclaim" , "fields":[{ "name" : "desynpuf_id" , "type" : "string"},{ "name" : "clm_id" , "type" : "long"},{ "name" : "segment" , "type" : "int"},{ "name" : "clm_from_dt" , "type" : "int"},{ "name" : "clm_thru_dt" , "type" : "int"},{ "name" : "prvdr_num" , "type" : "string"},{ "name" : "clm_pmt_amt" , "type" : "double"},{ "name" : "nch_prmry_pyr_clm_pd_amt" , "type" : "double"},{ "name" : "at_physn_npi" , "type" : "long"},{ "name" : "op_physn_npi" , "type" : "long"},{ "name" : "ot_physn_npi" , "type" : "long"},{ "name" : "nch_bene_blood_ddctbl_lblty_am" , "type" : "double"},{ "name" : "icd9_dgns_cds" , "type" :  {"type" : "array", "items" : "string"}},{ "name" : "icd9_prcdr_cds" , "type" :  {"type" : "array", "items" : "int"}},{ "name" : "nch_bene_ptb_ddctbl_amt" , "type" : "double"},{ "name" : "nch_bene_ptb_coinsrnc_amt" , "type" : "double"},{ "name" : "admtng_icd9_dgns_cd" , "type" : "string"},{ "name" : "hcpcs_cds" , "type" :  {"type" : "array", "items" : "int"}}]}""";  

    final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);
      
    override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
      try {
        if (oldVerobj == null) return null;
        oldVerobj match {
          
      case oldVerobj: com.ligadata.kamanja.samples.messages.V1000000.OutpatientClaim => { return  convertToVer1000000(oldVerobj); } 
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
  
    private def convertToVer1000000(oldVerobj: com.ligadata.kamanja.samples.messages.V1000000.OutpatientClaim): com.ligadata.kamanja.samples.messages.V1000000.OutpatientClaim= {
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
  override def CanPersist: Boolean = true
  override def isMessage: Boolean = true
  override def isContainer: Boolean = false
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj OutpatientClaim") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj OutpatientClaim");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj OutpatientClaim");
 override def NeedToTransformData: Boolean = false
    }

class OutpatientClaim(factory: MessageFactoryInterface, other: OutpatientClaim) extends MessageInterface(factory) { 
 
  val log = OutpatientClaim.log

      var attributeTypes = generateAttributeTypes;
      
    private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
      var attributeTypes = new Array[AttributeTypeInfo](18);
   		 attributeTypes(0) = new AttributeTypeInfo("desynpuf_id", 0, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(1) = new AttributeTypeInfo("clm_id", 1, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
		 attributeTypes(2) = new AttributeTypeInfo("segment", 2, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(3) = new AttributeTypeInfo("clm_from_dt", 3, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(4) = new AttributeTypeInfo("clm_thru_dt", 4, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(5) = new AttributeTypeInfo("prvdr_num", 5, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(6) = new AttributeTypeInfo("clm_pmt_amt", 6, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(7) = new AttributeTypeInfo("nch_prmry_pyr_clm_pd_amt", 7, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(8) = new AttributeTypeInfo("at_physn_npi", 8, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
		 attributeTypes(9) = new AttributeTypeInfo("op_physn_npi", 9, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
		 attributeTypes(10) = new AttributeTypeInfo("ot_physn_npi", 10, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
		 attributeTypes(11) = new AttributeTypeInfo("nch_bene_blood_ddctbl_lblty_am", 11, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(12) = new AttributeTypeInfo("icd9_dgns_cds", 12, AttributeTypeInfo.TypeCategory.ARRAY, 1, -1, 0)
		 attributeTypes(13) = new AttributeTypeInfo("icd9_prcdr_cds", 13, AttributeTypeInfo.TypeCategory.ARRAY, 0, -1, 0)
		 attributeTypes(14) = new AttributeTypeInfo("nch_bene_ptb_ddctbl_amt", 14, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(15) = new AttributeTypeInfo("nch_bene_ptb_coinsrnc_amt", 15, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(16) = new AttributeTypeInfo("admtng_icd9_dgns_cd", 16, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(17) = new AttributeTypeInfo("hcpcs_cds", 17, AttributeTypeInfo.TypeCategory.ARRAY, 0, -1, 0)

     
      return attributeTypes
    } 
    
		 var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;
    
     if (other != null && other != this) {
      // call copying fields from other to local variables
      fromFunc(other)
    }
    
    override def save: Unit = { OutpatientClaim.saveOne(this) }
  
    def Clone(): ContainerOrConcept = { OutpatientClaim.build(this) }

		override def getPartitionKey: Array[String] = {
		var partitionKeys: scala.collection.mutable.ArrayBuffer[String] = scala.collection.mutable.ArrayBuffer[String]();
		try {
		 partitionKeys += com.ligadata.BaseTypes.StringImpl.toString(get(caseSensitiveKey("desynpuf_id")).asInstanceOf[String]);
		 }catch {
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
		 primaryKeys += com.ligadata.BaseTypes.StringImpl.toString(get(caseSensitiveKey("desynpuf_id")).asInstanceOf[String]);
		 primaryKeys += com.ligadata.BaseTypes.LongImpl.toString(get(caseSensitiveKey("clm_id")).asInstanceOf[Long]);
		 }catch {
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
        if(attributeType.getName == caseSensitiveKey(name))
          return attributeType
      }) 
      return null;
    }
  
  
 		var desynpuf_id: String = _; 
 		var clm_id: Long = _; 
 		var segment: Int = _; 
 		var clm_from_dt: Int = _; 
 		var clm_thru_dt: Int = _; 
 		var prvdr_num: String = _; 
 		var clm_pmt_amt: Double = _; 
 		var nch_prmry_pyr_clm_pd_amt: Double = _; 
 		var at_physn_npi: Long = _; 
 		var op_physn_npi: Long = _; 
 		var ot_physn_npi: Long = _; 
 		var nch_bene_blood_ddctbl_lblty_am: Double = _; 
 		var icd9_dgns_cds: scala.Array[String] = _; 
 		var icd9_prcdr_cds: scala.Array[Int] = _; 
 		var nch_bene_ptb_ddctbl_amt: Double = _; 
 		var nch_bene_ptb_coinsrnc_amt: Double = _; 
 		var admtng_icd9_dgns_cd: String = _; 
 		var hcpcs_cds: scala.Array[Int] = _; 

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
      val fieldX = ru.typeOf[OutpatientClaim].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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
   
      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container OutpatientClaim", null);
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
   		case 0 => return this.desynpuf_id.asInstanceOf[AnyRef]; 
		case 1 => return this.clm_id.asInstanceOf[AnyRef]; 
		case 2 => return this.segment.asInstanceOf[AnyRef]; 
		case 3 => return this.clm_from_dt.asInstanceOf[AnyRef]; 
		case 4 => return this.clm_thru_dt.asInstanceOf[AnyRef]; 
		case 5 => return this.prvdr_num.asInstanceOf[AnyRef]; 
		case 6 => return this.clm_pmt_amt.asInstanceOf[AnyRef]; 
		case 7 => return this.nch_prmry_pyr_clm_pd_amt.asInstanceOf[AnyRef]; 
		case 8 => return this.at_physn_npi.asInstanceOf[AnyRef]; 
		case 9 => return this.op_physn_npi.asInstanceOf[AnyRef]; 
		case 10 => return this.ot_physn_npi.asInstanceOf[AnyRef]; 
		case 11 => return this.nch_bene_blood_ddctbl_lblty_am.asInstanceOf[AnyRef]; 
		case 12 => return this.icd9_dgns_cds.asInstanceOf[AnyRef]; 
		case 13 => return this.icd9_prcdr_cds.asInstanceOf[AnyRef]; 
		case 14 => return this.nch_bene_ptb_ddctbl_amt.asInstanceOf[AnyRef]; 
		case 15 => return this.nch_bene_ptb_coinsrnc_amt.asInstanceOf[AnyRef]; 
		case 16 => return this.admtng_icd9_dgns_cd.asInstanceOf[AnyRef]; 
		case 17 => return this.hcpcs_cds.asInstanceOf[AnyRef]; 

      	 case _ => throw new Exception(s"$index is a bad index for message OutpatientClaim");
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
      var attributeVals = new Array[AttributeValue](18);
      try{
 				attributeVals(0) = new AttributeValue(this.desynpuf_id, keyTypes("desynpuf_id")) 
				attributeVals(1) = new AttributeValue(this.clm_id, keyTypes("clm_id")) 
				attributeVals(2) = new AttributeValue(this.segment, keyTypes("segment")) 
				attributeVals(3) = new AttributeValue(this.clm_from_dt, keyTypes("clm_from_dt")) 
				attributeVals(4) = new AttributeValue(this.clm_thru_dt, keyTypes("clm_thru_dt")) 
				attributeVals(5) = new AttributeValue(this.prvdr_num, keyTypes("prvdr_num")) 
				attributeVals(6) = new AttributeValue(this.clm_pmt_amt, keyTypes("clm_pmt_amt")) 
				attributeVals(7) = new AttributeValue(this.nch_prmry_pyr_clm_pd_amt, keyTypes("nch_prmry_pyr_clm_pd_amt")) 
				attributeVals(8) = new AttributeValue(this.at_physn_npi, keyTypes("at_physn_npi")) 
				attributeVals(9) = new AttributeValue(this.op_physn_npi, keyTypes("op_physn_npi")) 
				attributeVals(10) = new AttributeValue(this.ot_physn_npi, keyTypes("ot_physn_npi")) 
				attributeVals(11) = new AttributeValue(this.nch_bene_blood_ddctbl_lblty_am, keyTypes("nch_bene_blood_ddctbl_lblty_am")) 
				attributeVals(12) = new AttributeValue(this.icd9_dgns_cds, keyTypes("icd9_dgns_cds")) 
				attributeVals(13) = new AttributeValue(this.icd9_prcdr_cds, keyTypes("icd9_prcdr_cds")) 
				attributeVals(14) = new AttributeValue(this.nch_bene_ptb_ddctbl_amt, keyTypes("nch_bene_ptb_ddctbl_amt")) 
				attributeVals(15) = new AttributeValue(this.nch_bene_ptb_coinsrnc_amt, keyTypes("nch_bene_ptb_coinsrnc_amt")) 
				attributeVals(16) = new AttributeValue(this.admtng_icd9_dgns_cd, keyTypes("admtng_icd9_dgns_cd")) 
				attributeVals(17) = new AttributeValue(this.hcpcs_cds, keyTypes("hcpcs_cds")) 
       
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
   
  			 if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message OutpatientClaim", null)
			 set(keyTypes(key).getIndex, value); 

      }catch {
          case e: Exception => {
          log.debug("", e)
          throw e
        }
      };
      
    }
  
      
    def set(index : Int, value :Any): Unit = {
      if (value == null) throw new Exception(s"Value is null for index $index in message OutpatientClaim ")
      try{
        index match {
 				case 0 => { 
				if(value.isInstanceOf[String]) 
				  this.desynpuf_id = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field desynpuf_id in message OutpatientClaim") 
				} 
				case 1 => { 
				if(value.isInstanceOf[Long]) 
				  this.clm_id = value.asInstanceOf[Long]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_id in message OutpatientClaim") 
				} 
				case 2 => { 
				if(value.isInstanceOf[Int]) 
				  this.segment = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field segment in message OutpatientClaim") 
				} 
				case 3 => { 
				if(value.isInstanceOf[Int]) 
				  this.clm_from_dt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_from_dt in message OutpatientClaim") 
				} 
				case 4 => { 
				if(value.isInstanceOf[Int]) 
				  this.clm_thru_dt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_thru_dt in message OutpatientClaim") 
				} 
				case 5 => { 
				if(value.isInstanceOf[String]) 
				  this.prvdr_num = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field prvdr_num in message OutpatientClaim") 
				} 
				case 6 => { 
				if(value.isInstanceOf[Double]) 
				  this.clm_pmt_amt = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_pmt_amt in message OutpatientClaim") 
				} 
				case 7 => { 
				if(value.isInstanceOf[Double]) 
				  this.nch_prmry_pyr_clm_pd_amt = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field nch_prmry_pyr_clm_pd_amt in message OutpatientClaim") 
				} 
				case 8 => { 
				if(value.isInstanceOf[Long]) 
				  this.at_physn_npi = value.asInstanceOf[Long]; 
				 else throw new Exception(s"Value is the not the correct type for field at_physn_npi in message OutpatientClaim") 
				} 
				case 9 => { 
				if(value.isInstanceOf[Long]) 
				  this.op_physn_npi = value.asInstanceOf[Long]; 
				 else throw new Exception(s"Value is the not the correct type for field op_physn_npi in message OutpatientClaim") 
				} 
				case 10 => { 
				if(value.isInstanceOf[Long]) 
				  this.ot_physn_npi = value.asInstanceOf[Long]; 
				 else throw new Exception(s"Value is the not the correct type for field ot_physn_npi in message OutpatientClaim") 
				} 
				case 11 => { 
				if(value.isInstanceOf[Double]) 
				  this.nch_bene_blood_ddctbl_lblty_am = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field nch_bene_blood_ddctbl_lblty_am in message OutpatientClaim") 
				} 
				case 12 => { 
				if(value.isInstanceOf[scala.Array[String]]) 
				  this.icd9_dgns_cds = value.asInstanceOf[scala.Array[String]]; 
				else if(value.isInstanceOf[scala.Array[_]]) 
				  this.icd9_dgns_cds = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[String]); 
				 else throw new Exception(s"Value is the not the correct type for field icd9_dgns_cds in message OutpatientClaim") 
				} 
				case 13 => { 
				if(value.isInstanceOf[scala.Array[Int]]) 
				  this.icd9_prcdr_cds = value.asInstanceOf[scala.Array[Int]]; 
				else if(value.isInstanceOf[scala.Array[_]]) 
				  this.icd9_prcdr_cds = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[Int]); 
				 else throw new Exception(s"Value is the not the correct type for field icd9_prcdr_cds in message OutpatientClaim") 
				} 
				case 14 => { 
				if(value.isInstanceOf[Double]) 
				  this.nch_bene_ptb_ddctbl_amt = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field nch_bene_ptb_ddctbl_amt in message OutpatientClaim") 
				} 
				case 15 => { 
				if(value.isInstanceOf[Double]) 
				  this.nch_bene_ptb_coinsrnc_amt = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field nch_bene_ptb_coinsrnc_amt in message OutpatientClaim") 
				} 
				case 16 => { 
				if(value.isInstanceOf[String]) 
				  this.admtng_icd9_dgns_cd = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field admtng_icd9_dgns_cd in message OutpatientClaim") 
				} 
				case 17 => { 
				if(value.isInstanceOf[scala.Array[Int]]) 
				  this.hcpcs_cds = value.asInstanceOf[scala.Array[Int]]; 
				else if(value.isInstanceOf[scala.Array[_]]) 
				  this.hcpcs_cds = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[Int]); 
				 else throw new Exception(s"Value is the not the correct type for field hcpcs_cds in message OutpatientClaim") 
				} 

        case _ => throw new Exception(s"$index is a bad index for message OutpatientClaim");
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
  
    private def fromFunc(other: OutpatientClaim): OutpatientClaim = {  
   			this.desynpuf_id = com.ligadata.BaseTypes.StringImpl.Clone(other.desynpuf_id);
			this.clm_id = com.ligadata.BaseTypes.LongImpl.Clone(other.clm_id);
			this.segment = com.ligadata.BaseTypes.IntImpl.Clone(other.segment);
			this.clm_from_dt = com.ligadata.BaseTypes.IntImpl.Clone(other.clm_from_dt);
			this.clm_thru_dt = com.ligadata.BaseTypes.IntImpl.Clone(other.clm_thru_dt);
			this.prvdr_num = com.ligadata.BaseTypes.StringImpl.Clone(other.prvdr_num);
			this.clm_pmt_amt = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clm_pmt_amt);
			this.nch_prmry_pyr_clm_pd_amt = com.ligadata.BaseTypes.DoubleImpl.Clone(other.nch_prmry_pyr_clm_pd_amt);
			this.at_physn_npi = com.ligadata.BaseTypes.LongImpl.Clone(other.at_physn_npi);
			this.op_physn_npi = com.ligadata.BaseTypes.LongImpl.Clone(other.op_physn_npi);
			this.ot_physn_npi = com.ligadata.BaseTypes.LongImpl.Clone(other.ot_physn_npi);
			this.nch_bene_blood_ddctbl_lblty_am = com.ligadata.BaseTypes.DoubleImpl.Clone(other.nch_bene_blood_ddctbl_lblty_am);
		 if (other.icd9_dgns_cds != null ) { 
		 icd9_dgns_cds = new scala.Array[String](other.icd9_dgns_cds.length); 
		 icd9_dgns_cds = other.icd9_dgns_cds.map(v => com.ligadata.BaseTypes.StringImpl.Clone(v)); 
		 } 
		 else this.icd9_dgns_cds = null; 
		 if (other.icd9_prcdr_cds != null ) { 
		 icd9_prcdr_cds = new scala.Array[Int](other.icd9_prcdr_cds.length); 
		 icd9_prcdr_cds = other.icd9_prcdr_cds.map(v => com.ligadata.BaseTypes.IntImpl.Clone(v)); 
		 } 
		 else this.icd9_prcdr_cds = null; 
			this.nch_bene_ptb_ddctbl_amt = com.ligadata.BaseTypes.DoubleImpl.Clone(other.nch_bene_ptb_ddctbl_amt);
			this.nch_bene_ptb_coinsrnc_amt = com.ligadata.BaseTypes.DoubleImpl.Clone(other.nch_bene_ptb_coinsrnc_amt);
			this.admtng_icd9_dgns_cd = com.ligadata.BaseTypes.StringImpl.Clone(other.admtng_icd9_dgns_cd);
		 if (other.hcpcs_cds != null ) { 
		 hcpcs_cds = new scala.Array[Int](other.hcpcs_cds.length); 
		 hcpcs_cds = other.hcpcs_cds.map(v => com.ligadata.BaseTypes.IntImpl.Clone(v)); 
		 } 
		 else this.hcpcs_cds = null; 

      this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
      return this;
    }
    
	 def withdesynpuf_id(value: String) : OutpatientClaim = {
		 this.desynpuf_id = value 
		 return this 
 	 } 
	 def withclm_id(value: Long) : OutpatientClaim = {
		 this.clm_id = value 
		 return this 
 	 } 
	 def withsegment(value: Int) : OutpatientClaim = {
		 this.segment = value 
		 return this 
 	 } 
	 def withclm_from_dt(value: Int) : OutpatientClaim = {
		 this.clm_from_dt = value 
		 return this 
 	 } 
	 def withclm_thru_dt(value: Int) : OutpatientClaim = {
		 this.clm_thru_dt = value 
		 return this 
 	 } 
	 def withprvdr_num(value: String) : OutpatientClaim = {
		 this.prvdr_num = value 
		 return this 
 	 } 
	 def withclm_pmt_amt(value: Double) : OutpatientClaim = {
		 this.clm_pmt_amt = value 
		 return this 
 	 } 
	 def withnch_prmry_pyr_clm_pd_amt(value: Double) : OutpatientClaim = {
		 this.nch_prmry_pyr_clm_pd_amt = value 
		 return this 
 	 } 
	 def withat_physn_npi(value: Long) : OutpatientClaim = {
		 this.at_physn_npi = value 
		 return this 
 	 } 
	 def withop_physn_npi(value: Long) : OutpatientClaim = {
		 this.op_physn_npi = value 
		 return this 
 	 } 
	 def withot_physn_npi(value: Long) : OutpatientClaim = {
		 this.ot_physn_npi = value 
		 return this 
 	 } 
	 def withnch_bene_blood_ddctbl_lblty_am(value: Double) : OutpatientClaim = {
		 this.nch_bene_blood_ddctbl_lblty_am = value 
		 return this 
 	 } 
	 def withicd9_dgns_cds(value: scala.Array[String]) : OutpatientClaim = {
		 this.icd9_dgns_cds = value 
		 return this 
 	 } 
	 def withicd9_prcdr_cds(value: scala.Array[Int]) : OutpatientClaim = {
		 this.icd9_prcdr_cds = value 
		 return this 
 	 } 
	 def withnch_bene_ptb_ddctbl_amt(value: Double) : OutpatientClaim = {
		 this.nch_bene_ptb_ddctbl_amt = value 
		 return this 
 	 } 
	 def withnch_bene_ptb_coinsrnc_amt(value: Double) : OutpatientClaim = {
		 this.nch_bene_ptb_coinsrnc_amt = value 
		 return this 
 	 } 
	 def withadmtng_icd9_dgns_cd(value: String) : OutpatientClaim = {
		 this.admtng_icd9_dgns_cd = value 
		 return this 
 	 } 
	 def withhcpcs_cds(value: scala.Array[Int]) : OutpatientClaim = {
		 this.hcpcs_cds = value 
		 return this 
 	 } 
    def isCaseSensitive(): Boolean = OutpatientClaim.isCaseSensitive(); 
    def caseSensitiveKey(keyName: String): String = {
      if(isCaseSensitive)
        return keyName;
      else return keyName.toLowerCase;
    }


    
    def this(factory:MessageFactoryInterface) = {
      this(factory, null)
     }
    
    def this(other: OutpatientClaim) = {
      this(other.getFactory.asInstanceOf[MessageFactoryInterface], other)
    }

}