
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

    
 
object InpatientClaim extends RDDObject[InpatientClaim] with MessageFactoryInterface { 
 
  val log = LogManager.getLogger(getClass)
	type T = InpatientClaim ;
	override def getFullTypeName: String = "com.ligadata.kamanja.samples.messages.InpatientClaim"; 
	override def getTypeNameSpace: String = "com.ligadata.kamanja.samples.messages"; 
	override def getTypeName: String = "InpatientClaim"; 
	override def getTypeVersion: String = "000000.000001.000000"; 
	override def getSchemaId: Int = 0; 
	override def getTenantId: String = ""; 
	override def createInstance: InpatientClaim = new InpatientClaim(InpatientClaim); 
	override def isFixed: Boolean = true; 
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
  
    override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanja.samples.messages" , "name" : "inpatientclaim" , "fields":[{ "name" : "desynpuf_id" , "type" : "string"},{ "name" : "clm_id" , "type" : "long"},{ "name" : "segment" , "type" : "int"},{ "name" : "clm_from_dt" , "type" : "int"},{ "name" : "clm_thru_dt" , "type" : "int"},{ "name" : "prvdr_num" , "type" : "string"},{ "name" : "clm_pmt_amt" , "type" : "double"},{ "name" : "nch_prmry_pyr_clm_pd_amt" , "type" : "double"},{ "name" : "at_physn_npi" , "type" : "long"},{ "name" : "op_physn_npi" , "type" : "long"},{ "name" : "ot_physn_npi" , "type" : "long"},{ "name" : "clm_admsn_dt" , "type" : "int"},{ "name" : "admtng_icd9_dgns_cd" , "type" : "string"},{ "name" : "clm_pass_thru_per_diem_amt" , "type" : "double"},{ "name" : "nch_bene_ip_ddctbl_amt" , "type" : "double"},{ "name" : "nch_bene_pta_coinsrnc_lblty_am" , "type" : "double"},{ "name" : "nch_bene_blood_ddctbl_lblty_am" , "type" : "double"},{ "name" : "clm_utlztn_day_cnt" , "type" : "int"},{ "name" : "nch_bene_dschrg_dt" , "type" : "int"},{ "name" : "clm_drg_cd" , "type" : "int"},{ "name" : "icd9_dgns_cds" , "type" :  {"type" : "array", "items" : "string"}},{ "name" : "icd9_prcdr_cds" , "type" :  {"type" : "array", "items" : "int"}},{ "name" : "hcpcs_cds" , "type" :  {"type" : "array", "items" : "int"}}]}""";  

    final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);
      
    override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
      try {
        if (oldVerobj == null) return null;
        oldVerobj match {
          
      case oldVerobj: com.ligadata.kamanja.samples.messages.V1000000.InpatientClaim => { return  convertToVer1000000(oldVerobj); } 
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
  
    private def convertToVer1000000(oldVerobj: com.ligadata.kamanja.samples.messages.V1000000.InpatientClaim): com.ligadata.kamanja.samples.messages.V1000000.InpatientClaim= {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj InpatientClaim") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj InpatientClaim");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj InpatientClaim");
 override def NeedToTransformData: Boolean = false
    }

class InpatientClaim(factory: MessageFactoryInterface, other: InpatientClaim) extends MessageInterface(factory) { 
 
  val log = InpatientClaim.log

      var attributeTypes = generateAttributeTypes;
      
    private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
      var attributeTypes = new Array[AttributeTypeInfo](23);
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
		 attributeTypes(11) = new AttributeTypeInfo("clm_admsn_dt", 11, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(12) = new AttributeTypeInfo("admtng_icd9_dgns_cd", 12, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(13) = new AttributeTypeInfo("clm_pass_thru_per_diem_amt", 13, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(14) = new AttributeTypeInfo("nch_bene_ip_ddctbl_amt", 14, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(15) = new AttributeTypeInfo("nch_bene_pta_coinsrnc_lblty_am", 15, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(16) = new AttributeTypeInfo("nch_bene_blood_ddctbl_lblty_am", 16, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(17) = new AttributeTypeInfo("clm_utlztn_day_cnt", 17, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(18) = new AttributeTypeInfo("nch_bene_dschrg_dt", 18, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(19) = new AttributeTypeInfo("clm_drg_cd", 19, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(20) = new AttributeTypeInfo("icd9_dgns_cds", 20, AttributeTypeInfo.TypeCategory.ARRAY, 1, -1, 0)
		 attributeTypes(21) = new AttributeTypeInfo("icd9_prcdr_cds", 21, AttributeTypeInfo.TypeCategory.ARRAY, 0, -1, 0)
		 attributeTypes(22) = new AttributeTypeInfo("hcpcs_cds", 22, AttributeTypeInfo.TypeCategory.ARRAY, 0, -1, 0)

     
      return attributeTypes
    } 
    
		 var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;
    
     if (other != null && other != this) {
      // call copying fields from other to local variables
      fromFunc(other)
    }
    
    override def save: Unit = { /* InpatientClaim.saveOne(this) */}
  
    def Clone(): ContainerOrConcept = { InpatientClaim.build(this) }

		override def getPartitionKey: Array[String] = {
		var partitionKeys: scala.collection.mutable.ArrayBuffer[String] = scala.collection.mutable.ArrayBuffer[String]();
		try {
		 partitionKeys += com.ligadata.BaseTypes.StringImpl.toString(get("desynpuf_id").asInstanceOf[String]);
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
		 primaryKeys += com.ligadata.BaseTypes.StringImpl.toString(get("desynpuf_id").asInstanceOf[String]);
		 primaryKeys += com.ligadata.BaseTypes.LongImpl.toString(get("clm_id").asInstanceOf[Long]);
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
        if(attributeType.getName == name.toLowerCase())
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
 		var clm_admsn_dt: Int = _; 
 		var admtng_icd9_dgns_cd: String = _; 
 		var clm_pass_thru_per_diem_amt: Double = _; 
 		var nch_bene_ip_ddctbl_amt: Double = _; 
 		var nch_bene_pta_coinsrnc_lblty_am: Double = _; 
 		var nch_bene_blood_ddctbl_lblty_am: Double = _; 
 		var clm_utlztn_day_cnt: Int = _; 
 		var nch_bene_dschrg_dt: Int = _; 
 		var clm_drg_cd: Int = _; 
 		var icd9_dgns_cds: scala.Array[String] = _; 
 		var icd9_prcdr_cds: scala.Array[Int] = _; 
 		var hcpcs_cds: scala.Array[Int] = _; 

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
      val fieldX = ru.typeOf[InpatientClaim].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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
   
      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container InpatientClaim", null);
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
		case 11 => return this.clm_admsn_dt.asInstanceOf[AnyRef]; 
		case 12 => return this.admtng_icd9_dgns_cd.asInstanceOf[AnyRef]; 
		case 13 => return this.clm_pass_thru_per_diem_amt.asInstanceOf[AnyRef]; 
		case 14 => return this.nch_bene_ip_ddctbl_amt.asInstanceOf[AnyRef]; 
		case 15 => return this.nch_bene_pta_coinsrnc_lblty_am.asInstanceOf[AnyRef]; 
		case 16 => return this.nch_bene_blood_ddctbl_lblty_am.asInstanceOf[AnyRef]; 
		case 17 => return this.clm_utlztn_day_cnt.asInstanceOf[AnyRef]; 
		case 18 => return this.nch_bene_dschrg_dt.asInstanceOf[AnyRef]; 
		case 19 => return this.clm_drg_cd.asInstanceOf[AnyRef]; 
		case 20 => return this.icd9_dgns_cds.asInstanceOf[AnyRef]; 
		case 21 => return this.icd9_prcdr_cds.asInstanceOf[AnyRef]; 
		case 22 => return this.hcpcs_cds.asInstanceOf[AnyRef]; 

      	 case _ => throw new Exception(s"$index is a bad index for message InpatientClaim");
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
      var attributeVals = new Array[AttributeValue](23);
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
				attributeVals(11) = new AttributeValue(this.clm_admsn_dt, keyTypes("clm_admsn_dt")) 
				attributeVals(12) = new AttributeValue(this.admtng_icd9_dgns_cd, keyTypes("admtng_icd9_dgns_cd")) 
				attributeVals(13) = new AttributeValue(this.clm_pass_thru_per_diem_amt, keyTypes("clm_pass_thru_per_diem_amt")) 
				attributeVals(14) = new AttributeValue(this.nch_bene_ip_ddctbl_amt, keyTypes("nch_bene_ip_ddctbl_amt")) 
				attributeVals(15) = new AttributeValue(this.nch_bene_pta_coinsrnc_lblty_am, keyTypes("nch_bene_pta_coinsrnc_lblty_am")) 
				attributeVals(16) = new AttributeValue(this.nch_bene_blood_ddctbl_lblty_am, keyTypes("nch_bene_blood_ddctbl_lblty_am")) 
				attributeVals(17) = new AttributeValue(this.clm_utlztn_day_cnt, keyTypes("clm_utlztn_day_cnt")) 
				attributeVals(18) = new AttributeValue(this.nch_bene_dschrg_dt, keyTypes("nch_bene_dschrg_dt")) 
				attributeVals(19) = new AttributeValue(this.clm_drg_cd, keyTypes("clm_drg_cd")) 
				attributeVals(20) = new AttributeValue(this.icd9_dgns_cds, keyTypes("icd9_dgns_cds")) 
				attributeVals(21) = new AttributeValue(this.icd9_prcdr_cds, keyTypes("icd9_prcdr_cds")) 
				attributeVals(22) = new AttributeValue(this.hcpcs_cds, keyTypes("hcpcs_cds")) 
       
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
   
  			 if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message InpatientClaim", null)
			 set(keyTypes(key).getIndex, value); 

      }catch {
          case e: Exception => {
          log.debug("", e)
          throw e
        }
      };
      
    }
  
      
    def set(index : Int, value :Any): Unit = {
      if (value == null) throw new Exception(s"Value is null for index $index in message InpatientClaim ")
      try{
        index match {
 				case 0 => { 
				if(value.isInstanceOf[String]) 
				  this.desynpuf_id = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field desynpuf_id in message InpatientClaim") 
				} 
				case 1 => { 
				if(value.isInstanceOf[Long]) 
				  this.clm_id = value.asInstanceOf[Long]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_id in message InpatientClaim") 
				} 
				case 2 => { 
				if(value.isInstanceOf[Int]) 
				  this.segment = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field segment in message InpatientClaim") 
				} 
				case 3 => { 
				if(value.isInstanceOf[Int]) 
				  this.clm_from_dt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_from_dt in message InpatientClaim") 
				} 
				case 4 => { 
				if(value.isInstanceOf[Int]) 
				  this.clm_thru_dt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_thru_dt in message InpatientClaim") 
				} 
				case 5 => { 
				if(value.isInstanceOf[String]) 
				  this.prvdr_num = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field prvdr_num in message InpatientClaim") 
				} 
				case 6 => { 
				if(value.isInstanceOf[Double]) 
				  this.clm_pmt_amt = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_pmt_amt in message InpatientClaim") 
				} 
				case 7 => { 
				if(value.isInstanceOf[Double]) 
				  this.nch_prmry_pyr_clm_pd_amt = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field nch_prmry_pyr_clm_pd_amt in message InpatientClaim") 
				} 
				case 8 => { 
				if(value.isInstanceOf[Long]) 
				  this.at_physn_npi = value.asInstanceOf[Long]; 
				 else throw new Exception(s"Value is the not the correct type for field at_physn_npi in message InpatientClaim") 
				} 
				case 9 => { 
				if(value.isInstanceOf[Long]) 
				  this.op_physn_npi = value.asInstanceOf[Long]; 
				 else throw new Exception(s"Value is the not the correct type for field op_physn_npi in message InpatientClaim") 
				} 
				case 10 => { 
				if(value.isInstanceOf[Long]) 
				  this.ot_physn_npi = value.asInstanceOf[Long]; 
				 else throw new Exception(s"Value is the not the correct type for field ot_physn_npi in message InpatientClaim") 
				} 
				case 11 => { 
				if(value.isInstanceOf[Int]) 
				  this.clm_admsn_dt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_admsn_dt in message InpatientClaim") 
				} 
				case 12 => { 
				if(value.isInstanceOf[String]) 
				  this.admtng_icd9_dgns_cd = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field admtng_icd9_dgns_cd in message InpatientClaim") 
				} 
				case 13 => { 
				if(value.isInstanceOf[Double]) 
				  this.clm_pass_thru_per_diem_amt = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_pass_thru_per_diem_amt in message InpatientClaim") 
				} 
				case 14 => { 
				if(value.isInstanceOf[Double]) 
				  this.nch_bene_ip_ddctbl_amt = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field nch_bene_ip_ddctbl_amt in message InpatientClaim") 
				} 
				case 15 => { 
				if(value.isInstanceOf[Double]) 
				  this.nch_bene_pta_coinsrnc_lblty_am = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field nch_bene_pta_coinsrnc_lblty_am in message InpatientClaim") 
				} 
				case 16 => { 
				if(value.isInstanceOf[Double]) 
				  this.nch_bene_blood_ddctbl_lblty_am = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field nch_bene_blood_ddctbl_lblty_am in message InpatientClaim") 
				} 
				case 17 => { 
				if(value.isInstanceOf[Int]) 
				  this.clm_utlztn_day_cnt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_utlztn_day_cnt in message InpatientClaim") 
				} 
				case 18 => { 
				if(value.isInstanceOf[Int]) 
				  this.nch_bene_dschrg_dt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field nch_bene_dschrg_dt in message InpatientClaim") 
				} 
				case 19 => { 
				if(value.isInstanceOf[Int]) 
				  this.clm_drg_cd = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_drg_cd in message InpatientClaim") 
				} 
				case 20 => { 
				if(value.isInstanceOf[scala.Array[String]]) 
				  this.icd9_dgns_cds = value.asInstanceOf[scala.Array[String]]; 
				else if(value.isInstanceOf[scala.Array[_]]) 
				  this.icd9_dgns_cds = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[String]); 
				 else throw new Exception(s"Value is the not the correct type for field icd9_dgns_cds in message InpatientClaim") 
				} 
				case 21 => { 
				if(value.isInstanceOf[scala.Array[Int]]) 
				  this.icd9_prcdr_cds = value.asInstanceOf[scala.Array[Int]]; 
				else if(value.isInstanceOf[scala.Array[_]]) 
				  this.icd9_prcdr_cds = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[Int]); 
				 else throw new Exception(s"Value is the not the correct type for field icd9_prcdr_cds in message InpatientClaim") 
				} 
				case 22 => { 
				if(value.isInstanceOf[scala.Array[Int]]) 
				  this.hcpcs_cds = value.asInstanceOf[scala.Array[Int]]; 
				else if(value.isInstanceOf[scala.Array[_]]) 
				  this.hcpcs_cds = value.asInstanceOf[scala.Array[_]].map(v => v.asInstanceOf[Int]); 
				 else throw new Exception(s"Value is the not the correct type for field hcpcs_cds in message InpatientClaim") 
				} 

        case _ => throw new Exception(s"$index is a bad index for message InpatientClaim");
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
  
    private def fromFunc(other: InpatientClaim): InpatientClaim = {  
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
			this.clm_admsn_dt = com.ligadata.BaseTypes.IntImpl.Clone(other.clm_admsn_dt);
			this.admtng_icd9_dgns_cd = com.ligadata.BaseTypes.StringImpl.Clone(other.admtng_icd9_dgns_cd);
			this.clm_pass_thru_per_diem_amt = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clm_pass_thru_per_diem_amt);
			this.nch_bene_ip_ddctbl_amt = com.ligadata.BaseTypes.DoubleImpl.Clone(other.nch_bene_ip_ddctbl_amt);
			this.nch_bene_pta_coinsrnc_lblty_am = com.ligadata.BaseTypes.DoubleImpl.Clone(other.nch_bene_pta_coinsrnc_lblty_am);
			this.nch_bene_blood_ddctbl_lblty_am = com.ligadata.BaseTypes.DoubleImpl.Clone(other.nch_bene_blood_ddctbl_lblty_am);
			this.clm_utlztn_day_cnt = com.ligadata.BaseTypes.IntImpl.Clone(other.clm_utlztn_day_cnt);
			this.nch_bene_dschrg_dt = com.ligadata.BaseTypes.IntImpl.Clone(other.nch_bene_dschrg_dt);
			this.clm_drg_cd = com.ligadata.BaseTypes.IntImpl.Clone(other.clm_drg_cd);
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
		 if (other.hcpcs_cds != null ) { 
		 hcpcs_cds = new scala.Array[Int](other.hcpcs_cds.length); 
		 hcpcs_cds = other.hcpcs_cds.map(v => com.ligadata.BaseTypes.IntImpl.Clone(v)); 
		 } 
		 else this.hcpcs_cds = null; 

      this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
      return this;
    }
    
	 def withdesynpuf_id(value: String) : InpatientClaim = {
		 this.desynpuf_id = value 
		 return this 
 	 } 
	 def withclm_id(value: Long) : InpatientClaim = {
		 this.clm_id = value 
		 return this 
 	 } 
	 def withsegment(value: Int) : InpatientClaim = {
		 this.segment = value 
		 return this 
 	 } 
	 def withclm_from_dt(value: Int) : InpatientClaim = {
		 this.clm_from_dt = value 
		 return this 
 	 } 
	 def withclm_thru_dt(value: Int) : InpatientClaim = {
		 this.clm_thru_dt = value 
		 return this 
 	 } 
	 def withprvdr_num(value: String) : InpatientClaim = {
		 this.prvdr_num = value 
		 return this 
 	 } 
	 def withclm_pmt_amt(value: Double) : InpatientClaim = {
		 this.clm_pmt_amt = value 
		 return this 
 	 } 
	 def withnch_prmry_pyr_clm_pd_amt(value: Double) : InpatientClaim = {
		 this.nch_prmry_pyr_clm_pd_amt = value 
		 return this 
 	 } 
	 def withat_physn_npi(value: Long) : InpatientClaim = {
		 this.at_physn_npi = value 
		 return this 
 	 } 
	 def withop_physn_npi(value: Long) : InpatientClaim = {
		 this.op_physn_npi = value 
		 return this 
 	 } 
	 def withot_physn_npi(value: Long) : InpatientClaim = {
		 this.ot_physn_npi = value 
		 return this 
 	 } 
	 def withclm_admsn_dt(value: Int) : InpatientClaim = {
		 this.clm_admsn_dt = value 
		 return this 
 	 } 
	 def withadmtng_icd9_dgns_cd(value: String) : InpatientClaim = {
		 this.admtng_icd9_dgns_cd = value 
		 return this 
 	 } 
	 def withclm_pass_thru_per_diem_amt(value: Double) : InpatientClaim = {
		 this.clm_pass_thru_per_diem_amt = value 
		 return this 
 	 } 
	 def withnch_bene_ip_ddctbl_amt(value: Double) : InpatientClaim = {
		 this.nch_bene_ip_ddctbl_amt = value 
		 return this 
 	 } 
	 def withnch_bene_pta_coinsrnc_lblty_am(value: Double) : InpatientClaim = {
		 this.nch_bene_pta_coinsrnc_lblty_am = value 
		 return this 
 	 } 
	 def withnch_bene_blood_ddctbl_lblty_am(value: Double) : InpatientClaim = {
		 this.nch_bene_blood_ddctbl_lblty_am = value 
		 return this 
 	 } 
	 def withclm_utlztn_day_cnt(value: Int) : InpatientClaim = {
		 this.clm_utlztn_day_cnt = value 
		 return this 
 	 } 
	 def withnch_bene_dschrg_dt(value: Int) : InpatientClaim = {
		 this.nch_bene_dschrg_dt = value 
		 return this 
 	 } 
	 def withclm_drg_cd(value: Int) : InpatientClaim = {
		 this.clm_drg_cd = value 
		 return this 
 	 } 
	 def withicd9_dgns_cds(value: scala.Array[String]) : InpatientClaim = {
		 this.icd9_dgns_cds = value 
		 return this 
 	 } 
	 def withicd9_prcdr_cds(value: scala.Array[Int]) : InpatientClaim = {
		 this.icd9_prcdr_cds = value 
		 return this 
 	 } 
	 def withhcpcs_cds(value: scala.Array[Int]) : InpatientClaim = {
		 this.hcpcs_cds = value 
		 return this 
 	 } 

    def this(factory:MessageFactoryInterface) = {
      this(factory, null)
     }
    
    def this(other: InpatientClaim) = {
      this(other.getFactory.asInstanceOf[MessageFactoryInterface], other)
    }

}