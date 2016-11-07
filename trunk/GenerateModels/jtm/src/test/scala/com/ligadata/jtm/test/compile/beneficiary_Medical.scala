
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

    
 
object Beneficiary extends RDDObject[Beneficiary] with MessageFactoryInterface { 
 
  val log = LogManager.getLogger(getClass)
	type T = Beneficiary ;
	override def getFullTypeName: String = "com.ligadata.kamanja.samples.messages.Beneficiary"; 
	override def getTypeNameSpace: String = "com.ligadata.kamanja.samples.messages"; 
	override def getTypeName: String = "Beneficiary"; 
	override def getTypeVersion: String = "000000.000001.000000"; 
	override def getSchemaId: Int = 0; 
	override def getTenantId: String = ""; 
	override def createInstance: Beneficiary = new Beneficiary(Beneficiary); 
	override def isFixed: Boolean = true; 
	def isCaseSensitive(): Boolean = false; 
	override def getContainerType: ContainerTypes.ContainerType = ContainerTypes.ContainerType.MESSAGE
	override def getFullName = getFullTypeName; 
	override def getRddTenantId = getTenantId; 
	override def toJavaRDDObject: JavaRDDObject[T] = JavaRDDObject.fromRDDObject[T](this); 

    def build = new T(this)
    def build(from: T) = new T(from)
   override def getPartitionKeyNames: Array[String] = Array("desynpuf_id"); 

  override def getPrimaryKeyNames: Array[String] = Array("desynpuf_id"); 
   
  
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
  
    override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanja.samples.messages" , "name" : "beneficiary" , "fields":[{ "name" : "desynpuf_id" , "type" : "string"},{ "name" : "bene_birth_dt" , "type" : "int"},{ "name" : "bene_death_dt" , "type" : "int"},{ "name" : "bene_sex_ident_cd" , "type" : "int"},{ "name" : "bene_race_cd" , "type" : "int"},{ "name" : "bene_esrd_ind" , "type" : "string"},{ "name" : "sp_state_code" , "type" : "int"},{ "name" : "bene_county_cd" , "type" : "int"},{ "name" : "bene_hi_cvrage_tot_mons" , "type" : "int"},{ "name" : "bene_smi_cvrage_tot_mons" , "type" : "int"},{ "name" : "bene_hmo_cvrage_tot_mons" , "type" : "int"},{ "name" : "plan_cvrg_mos_num" , "type" : "int"},{ "name" : "sp_alzhdmta" , "type" : "int"},{ "name" : "sp_chf" , "type" : "int"},{ "name" : "sp_chrnkidn" , "type" : "int"},{ "name" : "sp_cncr" , "type" : "int"},{ "name" : "sp_copd" , "type" : "int"},{ "name" : "sp_depressn" , "type" : "int"},{ "name" : "sp_diabetes" , "type" : "int"},{ "name" : "sp_ischmcht" , "type" : "int"},{ "name" : "sp_osteoprs" , "type" : "int"},{ "name" : "sp_ra_oa" , "type" : "int"},{ "name" : "sp_strketia" , "type" : "int"},{ "name" : "medreimb_ip" , "type" : "double"},{ "name" : "benres_ip" , "type" : "double"},{ "name" : "pppymt_ip" , "type" : "double"},{ "name" : "medreimb_op" , "type" : "double"},{ "name" : "benres_op" , "type" : "double"},{ "name" : "pppymt_op" , "type" : "double"},{ "name" : "medreimb_car" , "type" : "double"},{ "name" : "benres_car" , "type" : "double"},{ "name" : "pppymt_car" , "type" : "double"}]}""";  

    final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);
      
    override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
      try {
        if (oldVerobj == null) return null;
        oldVerobj match {
          
      case oldVerobj: com.ligadata.kamanja.samples.messages.V1000000.Beneficiary => { return  convertToVer1000000(oldVerobj); } 
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
  
    private def convertToVer1000000(oldVerobj: com.ligadata.kamanja.samples.messages.V1000000.Beneficiary): com.ligadata.kamanja.samples.messages.V1000000.Beneficiary= {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj Beneficiary") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj Beneficiary");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj Beneficiary");
 override def NeedToTransformData: Boolean = false
    }

class Beneficiary(factory: MessageFactoryInterface, other: Beneficiary) extends MessageInterface(factory) { 
 
  val log = Beneficiary.log

      var attributeTypes = generateAttributeTypes;
      
    private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
      var attributeTypes = new Array[AttributeTypeInfo](32);
   		 attributeTypes(0) = new AttributeTypeInfo("desynpuf_id", 0, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(1) = new AttributeTypeInfo("bene_birth_dt", 1, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(2) = new AttributeTypeInfo("bene_death_dt", 2, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(3) = new AttributeTypeInfo("bene_sex_ident_cd", 3, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(4) = new AttributeTypeInfo("bene_race_cd", 4, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(5) = new AttributeTypeInfo("bene_esrd_ind", 5, AttributeTypeInfo.TypeCategory.CHAR, -1, -1, 0)
		 attributeTypes(6) = new AttributeTypeInfo("sp_state_code", 6, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(7) = new AttributeTypeInfo("bene_county_cd", 7, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(8) = new AttributeTypeInfo("bene_hi_cvrage_tot_mons", 8, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(9) = new AttributeTypeInfo("bene_smi_cvrage_tot_mons", 9, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(10) = new AttributeTypeInfo("bene_hmo_cvrage_tot_mons", 10, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(11) = new AttributeTypeInfo("plan_cvrg_mos_num", 11, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(12) = new AttributeTypeInfo("sp_alzhdmta", 12, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(13) = new AttributeTypeInfo("sp_chf", 13, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(14) = new AttributeTypeInfo("sp_chrnkidn", 14, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(15) = new AttributeTypeInfo("sp_cncr", 15, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(16) = new AttributeTypeInfo("sp_copd", 16, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(17) = new AttributeTypeInfo("sp_depressn", 17, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(18) = new AttributeTypeInfo("sp_diabetes", 18, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(19) = new AttributeTypeInfo("sp_ischmcht", 19, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(20) = new AttributeTypeInfo("sp_osteoprs", 20, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(21) = new AttributeTypeInfo("sp_ra_oa", 21, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(22) = new AttributeTypeInfo("sp_strketia", 22, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(23) = new AttributeTypeInfo("medreimb_ip", 23, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(24) = new AttributeTypeInfo("benres_ip", 24, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(25) = new AttributeTypeInfo("pppymt_ip", 25, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(26) = new AttributeTypeInfo("medreimb_op", 26, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(27) = new AttributeTypeInfo("benres_op", 27, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(28) = new AttributeTypeInfo("pppymt_op", 28, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(29) = new AttributeTypeInfo("medreimb_car", 29, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(30) = new AttributeTypeInfo("benres_car", 30, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(31) = new AttributeTypeInfo("pppymt_car", 31, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)

     
      return attributeTypes
    } 
    
		 var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;
    
     if (other != null && other != this) {
      // call copying fields from other to local variables
      fromFunc(other)
    }
    
    override def save: Unit = { Beneficiary.saveOne(this) }
  
    def Clone(): ContainerOrConcept = { Beneficiary.build(this) }

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
 		var bene_birth_dt: Int = _; 
 		var bene_death_dt: Int = _; 
 		var bene_sex_ident_cd: Int = _; 
 		var bene_race_cd: Int = _; 
 		var bene_esrd_ind: Char = _; 
 		var sp_state_code: Int = _; 
 		var bene_county_cd: Int = _; 
 		var bene_hi_cvrage_tot_mons: Int = _; 
 		var bene_smi_cvrage_tot_mons: Int = _; 
 		var bene_hmo_cvrage_tot_mons: Int = _; 
 		var plan_cvrg_mos_num: Int = _; 
 		var sp_alzhdmta: Int = _; 
 		var sp_chf: Int = _; 
 		var sp_chrnkidn: Int = _; 
 		var sp_cncr: Int = _; 
 		var sp_copd: Int = _; 
 		var sp_depressn: Int = _; 
 		var sp_diabetes: Int = _; 
 		var sp_ischmcht: Int = _; 
 		var sp_osteoprs: Int = _; 
 		var sp_ra_oa: Int = _; 
 		var sp_strketia: Int = _; 
 		var medreimb_ip: Double = _; 
 		var benres_ip: Double = _; 
 		var pppymt_ip: Double = _; 
 		var medreimb_op: Double = _; 
 		var benres_op: Double = _; 
 		var pppymt_op: Double = _; 
 		var medreimb_car: Double = _; 
 		var benres_car: Double = _; 
 		var pppymt_car: Double = _; 

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
      val fieldX = ru.typeOf[Beneficiary].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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
   
      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container Beneficiary", null);
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
		case 1 => return this.bene_birth_dt.asInstanceOf[AnyRef]; 
		case 2 => return this.bene_death_dt.asInstanceOf[AnyRef]; 
		case 3 => return this.bene_sex_ident_cd.asInstanceOf[AnyRef]; 
		case 4 => return this.bene_race_cd.asInstanceOf[AnyRef]; 
		case 5 => return this.bene_esrd_ind.asInstanceOf[AnyRef]; 
		case 6 => return this.sp_state_code.asInstanceOf[AnyRef]; 
		case 7 => return this.bene_county_cd.asInstanceOf[AnyRef]; 
		case 8 => return this.bene_hi_cvrage_tot_mons.asInstanceOf[AnyRef]; 
		case 9 => return this.bene_smi_cvrage_tot_mons.asInstanceOf[AnyRef]; 
		case 10 => return this.bene_hmo_cvrage_tot_mons.asInstanceOf[AnyRef]; 
		case 11 => return this.plan_cvrg_mos_num.asInstanceOf[AnyRef]; 
		case 12 => return this.sp_alzhdmta.asInstanceOf[AnyRef]; 
		case 13 => return this.sp_chf.asInstanceOf[AnyRef]; 
		case 14 => return this.sp_chrnkidn.asInstanceOf[AnyRef]; 
		case 15 => return this.sp_cncr.asInstanceOf[AnyRef]; 
		case 16 => return this.sp_copd.asInstanceOf[AnyRef]; 
		case 17 => return this.sp_depressn.asInstanceOf[AnyRef]; 
		case 18 => return this.sp_diabetes.asInstanceOf[AnyRef]; 
		case 19 => return this.sp_ischmcht.asInstanceOf[AnyRef]; 
		case 20 => return this.sp_osteoprs.asInstanceOf[AnyRef]; 
		case 21 => return this.sp_ra_oa.asInstanceOf[AnyRef]; 
		case 22 => return this.sp_strketia.asInstanceOf[AnyRef]; 
		case 23 => return this.medreimb_ip.asInstanceOf[AnyRef]; 
		case 24 => return this.benres_ip.asInstanceOf[AnyRef]; 
		case 25 => return this.pppymt_ip.asInstanceOf[AnyRef]; 
		case 26 => return this.medreimb_op.asInstanceOf[AnyRef]; 
		case 27 => return this.benres_op.asInstanceOf[AnyRef]; 
		case 28 => return this.pppymt_op.asInstanceOf[AnyRef]; 
		case 29 => return this.medreimb_car.asInstanceOf[AnyRef]; 
		case 30 => return this.benres_car.asInstanceOf[AnyRef]; 
		case 31 => return this.pppymt_car.asInstanceOf[AnyRef]; 

      	 case _ => throw new Exception(s"$index is a bad index for message Beneficiary");
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
      var attributeVals = new Array[AttributeValue](32);
      try{
 				attributeVals(0) = new AttributeValue(this.desynpuf_id, keyTypes("desynpuf_id")) 
				attributeVals(1) = new AttributeValue(this.bene_birth_dt, keyTypes("bene_birth_dt")) 
				attributeVals(2) = new AttributeValue(this.bene_death_dt, keyTypes("bene_death_dt")) 
				attributeVals(3) = new AttributeValue(this.bene_sex_ident_cd, keyTypes("bene_sex_ident_cd")) 
				attributeVals(4) = new AttributeValue(this.bene_race_cd, keyTypes("bene_race_cd")) 
				attributeVals(5) = new AttributeValue(this.bene_esrd_ind, keyTypes("bene_esrd_ind")) 
				attributeVals(6) = new AttributeValue(this.sp_state_code, keyTypes("sp_state_code")) 
				attributeVals(7) = new AttributeValue(this.bene_county_cd, keyTypes("bene_county_cd")) 
				attributeVals(8) = new AttributeValue(this.bene_hi_cvrage_tot_mons, keyTypes("bene_hi_cvrage_tot_mons")) 
				attributeVals(9) = new AttributeValue(this.bene_smi_cvrage_tot_mons, keyTypes("bene_smi_cvrage_tot_mons")) 
				attributeVals(10) = new AttributeValue(this.bene_hmo_cvrage_tot_mons, keyTypes("bene_hmo_cvrage_tot_mons")) 
				attributeVals(11) = new AttributeValue(this.plan_cvrg_mos_num, keyTypes("plan_cvrg_mos_num")) 
				attributeVals(12) = new AttributeValue(this.sp_alzhdmta, keyTypes("sp_alzhdmta")) 
				attributeVals(13) = new AttributeValue(this.sp_chf, keyTypes("sp_chf")) 
				attributeVals(14) = new AttributeValue(this.sp_chrnkidn, keyTypes("sp_chrnkidn")) 
				attributeVals(15) = new AttributeValue(this.sp_cncr, keyTypes("sp_cncr")) 
				attributeVals(16) = new AttributeValue(this.sp_copd, keyTypes("sp_copd")) 
				attributeVals(17) = new AttributeValue(this.sp_depressn, keyTypes("sp_depressn")) 
				attributeVals(18) = new AttributeValue(this.sp_diabetes, keyTypes("sp_diabetes")) 
				attributeVals(19) = new AttributeValue(this.sp_ischmcht, keyTypes("sp_ischmcht")) 
				attributeVals(20) = new AttributeValue(this.sp_osteoprs, keyTypes("sp_osteoprs")) 
				attributeVals(21) = new AttributeValue(this.sp_ra_oa, keyTypes("sp_ra_oa")) 
				attributeVals(22) = new AttributeValue(this.sp_strketia, keyTypes("sp_strketia")) 
				attributeVals(23) = new AttributeValue(this.medreimb_ip, keyTypes("medreimb_ip")) 
				attributeVals(24) = new AttributeValue(this.benres_ip, keyTypes("benres_ip")) 
				attributeVals(25) = new AttributeValue(this.pppymt_ip, keyTypes("pppymt_ip")) 
				attributeVals(26) = new AttributeValue(this.medreimb_op, keyTypes("medreimb_op")) 
				attributeVals(27) = new AttributeValue(this.benres_op, keyTypes("benres_op")) 
				attributeVals(28) = new AttributeValue(this.pppymt_op, keyTypes("pppymt_op")) 
				attributeVals(29) = new AttributeValue(this.medreimb_car, keyTypes("medreimb_car")) 
				attributeVals(30) = new AttributeValue(this.benres_car, keyTypes("benres_car")) 
				attributeVals(31) = new AttributeValue(this.pppymt_car, keyTypes("pppymt_car")) 
       
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
   
  			 if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message Beneficiary", null)
			 set(keyTypes(key).getIndex, value); 

      }catch {
          case e: Exception => {
          log.debug("", e)
          throw e
        }
      };
      
    }
  
      
    def set(index : Int, value :Any): Unit = {
      if (value == null) throw new Exception(s"Value is null for index $index in message Beneficiary ")
      try{
        index match {
 				case 0 => { 
				if(value.isInstanceOf[String]) 
				  this.desynpuf_id = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field desynpuf_id in message Beneficiary") 
				} 
				case 1 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_birth_dt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_birth_dt in message Beneficiary") 
				} 
				case 2 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_death_dt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_death_dt in message Beneficiary") 
				} 
				case 3 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_sex_ident_cd = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_sex_ident_cd in message Beneficiary") 
				} 
				case 4 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_race_cd = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_race_cd in message Beneficiary") 
				} 
				case 5 => { 
				if(value.isInstanceOf[Char]) 
				  this.bene_esrd_ind = value.asInstanceOf[Char]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_esrd_ind in message Beneficiary") 
				} 
				case 6 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_state_code = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_state_code in message Beneficiary") 
				} 
				case 7 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_county_cd = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_county_cd in message Beneficiary") 
				} 
				case 8 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_hi_cvrage_tot_mons = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_hi_cvrage_tot_mons in message Beneficiary") 
				} 
				case 9 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_smi_cvrage_tot_mons = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_smi_cvrage_tot_mons in message Beneficiary") 
				} 
				case 10 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_hmo_cvrage_tot_mons = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_hmo_cvrage_tot_mons in message Beneficiary") 
				} 
				case 11 => { 
				if(value.isInstanceOf[Int]) 
				  this.plan_cvrg_mos_num = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field plan_cvrg_mos_num in message Beneficiary") 
				} 
				case 12 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_alzhdmta = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_alzhdmta in message Beneficiary") 
				} 
				case 13 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_chf = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_chf in message Beneficiary") 
				} 
				case 14 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_chrnkidn = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_chrnkidn in message Beneficiary") 
				} 
				case 15 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_cncr = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_cncr in message Beneficiary") 
				} 
				case 16 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_copd = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_copd in message Beneficiary") 
				} 
				case 17 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_depressn = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_depressn in message Beneficiary") 
				} 
				case 18 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_diabetes = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_diabetes in message Beneficiary") 
				} 
				case 19 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_ischmcht = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_ischmcht in message Beneficiary") 
				} 
				case 20 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_osteoprs = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_osteoprs in message Beneficiary") 
				} 
				case 21 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_ra_oa = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_ra_oa in message Beneficiary") 
				} 
				case 22 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_strketia = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_strketia in message Beneficiary") 
				} 
				case 23 => { 
				if(value.isInstanceOf[Double]) 
				  this.medreimb_ip = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field medreimb_ip in message Beneficiary") 
				} 
				case 24 => { 
				if(value.isInstanceOf[Double]) 
				  this.benres_ip = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field benres_ip in message Beneficiary") 
				} 
				case 25 => { 
				if(value.isInstanceOf[Double]) 
				  this.pppymt_ip = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field pppymt_ip in message Beneficiary") 
				} 
				case 26 => { 
				if(value.isInstanceOf[Double]) 
				  this.medreimb_op = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field medreimb_op in message Beneficiary") 
				} 
				case 27 => { 
				if(value.isInstanceOf[Double]) 
				  this.benres_op = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field benres_op in message Beneficiary") 
				} 
				case 28 => { 
				if(value.isInstanceOf[Double]) 
				  this.pppymt_op = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field pppymt_op in message Beneficiary") 
				} 
				case 29 => { 
				if(value.isInstanceOf[Double]) 
				  this.medreimb_car = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field medreimb_car in message Beneficiary") 
				} 
				case 30 => { 
				if(value.isInstanceOf[Double]) 
				  this.benres_car = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field benres_car in message Beneficiary") 
				} 
				case 31 => { 
				if(value.isInstanceOf[Double]) 
				  this.pppymt_car = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field pppymt_car in message Beneficiary") 
				} 

        case _ => throw new Exception(s"$index is a bad index for message Beneficiary");
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
  
    private def fromFunc(other: Beneficiary): Beneficiary = {  
   			this.desynpuf_id = com.ligadata.BaseTypes.StringImpl.Clone(other.desynpuf_id);
			this.bene_birth_dt = com.ligadata.BaseTypes.IntImpl.Clone(other.bene_birth_dt);
			this.bene_death_dt = com.ligadata.BaseTypes.IntImpl.Clone(other.bene_death_dt);
			this.bene_sex_ident_cd = com.ligadata.BaseTypes.IntImpl.Clone(other.bene_sex_ident_cd);
			this.bene_race_cd = com.ligadata.BaseTypes.IntImpl.Clone(other.bene_race_cd);
			this.bene_esrd_ind = com.ligadata.BaseTypes.CharImpl.Clone(other.bene_esrd_ind);
			this.sp_state_code = com.ligadata.BaseTypes.IntImpl.Clone(other.sp_state_code);
			this.bene_county_cd = com.ligadata.BaseTypes.IntImpl.Clone(other.bene_county_cd);
			this.bene_hi_cvrage_tot_mons = com.ligadata.BaseTypes.IntImpl.Clone(other.bene_hi_cvrage_tot_mons);
			this.bene_smi_cvrage_tot_mons = com.ligadata.BaseTypes.IntImpl.Clone(other.bene_smi_cvrage_tot_mons);
			this.bene_hmo_cvrage_tot_mons = com.ligadata.BaseTypes.IntImpl.Clone(other.bene_hmo_cvrage_tot_mons);
			this.plan_cvrg_mos_num = com.ligadata.BaseTypes.IntImpl.Clone(other.plan_cvrg_mos_num);
			this.sp_alzhdmta = com.ligadata.BaseTypes.IntImpl.Clone(other.sp_alzhdmta);
			this.sp_chf = com.ligadata.BaseTypes.IntImpl.Clone(other.sp_chf);
			this.sp_chrnkidn = com.ligadata.BaseTypes.IntImpl.Clone(other.sp_chrnkidn);
			this.sp_cncr = com.ligadata.BaseTypes.IntImpl.Clone(other.sp_cncr);
			this.sp_copd = com.ligadata.BaseTypes.IntImpl.Clone(other.sp_copd);
			this.sp_depressn = com.ligadata.BaseTypes.IntImpl.Clone(other.sp_depressn);
			this.sp_diabetes = com.ligadata.BaseTypes.IntImpl.Clone(other.sp_diabetes);
			this.sp_ischmcht = com.ligadata.BaseTypes.IntImpl.Clone(other.sp_ischmcht);
			this.sp_osteoprs = com.ligadata.BaseTypes.IntImpl.Clone(other.sp_osteoprs);
			this.sp_ra_oa = com.ligadata.BaseTypes.IntImpl.Clone(other.sp_ra_oa);
			this.sp_strketia = com.ligadata.BaseTypes.IntImpl.Clone(other.sp_strketia);
			this.medreimb_ip = com.ligadata.BaseTypes.DoubleImpl.Clone(other.medreimb_ip);
			this.benres_ip = com.ligadata.BaseTypes.DoubleImpl.Clone(other.benres_ip);
			this.pppymt_ip = com.ligadata.BaseTypes.DoubleImpl.Clone(other.pppymt_ip);
			this.medreimb_op = com.ligadata.BaseTypes.DoubleImpl.Clone(other.medreimb_op);
			this.benres_op = com.ligadata.BaseTypes.DoubleImpl.Clone(other.benres_op);
			this.pppymt_op = com.ligadata.BaseTypes.DoubleImpl.Clone(other.pppymt_op);
			this.medreimb_car = com.ligadata.BaseTypes.DoubleImpl.Clone(other.medreimb_car);
			this.benres_car = com.ligadata.BaseTypes.DoubleImpl.Clone(other.benres_car);
			this.pppymt_car = com.ligadata.BaseTypes.DoubleImpl.Clone(other.pppymt_car);

      this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
      return this;
    }
    
	 def withdesynpuf_id(value: String) : Beneficiary = {
		 this.desynpuf_id = value 
		 return this 
 	 } 
	 def withbene_birth_dt(value: Int) : Beneficiary = {
		 this.bene_birth_dt = value 
		 return this 
 	 } 
	 def withbene_death_dt(value: Int) : Beneficiary = {
		 this.bene_death_dt = value 
		 return this 
 	 } 
	 def withbene_sex_ident_cd(value: Int) : Beneficiary = {
		 this.bene_sex_ident_cd = value 
		 return this 
 	 } 
	 def withbene_race_cd(value: Int) : Beneficiary = {
		 this.bene_race_cd = value 
		 return this 
 	 } 
	 def withbene_esrd_ind(value: Char) : Beneficiary = {
		 this.bene_esrd_ind = value 
		 return this 
 	 } 
	 def withsp_state_code(value: Int) : Beneficiary = {
		 this.sp_state_code = value 
		 return this 
 	 } 
	 def withbene_county_cd(value: Int) : Beneficiary = {
		 this.bene_county_cd = value 
		 return this 
 	 } 
	 def withbene_hi_cvrage_tot_mons(value: Int) : Beneficiary = {
		 this.bene_hi_cvrage_tot_mons = value 
		 return this 
 	 } 
	 def withbene_smi_cvrage_tot_mons(value: Int) : Beneficiary = {
		 this.bene_smi_cvrage_tot_mons = value 
		 return this 
 	 } 
	 def withbene_hmo_cvrage_tot_mons(value: Int) : Beneficiary = {
		 this.bene_hmo_cvrage_tot_mons = value 
		 return this 
 	 } 
	 def withplan_cvrg_mos_num(value: Int) : Beneficiary = {
		 this.plan_cvrg_mos_num = value 
		 return this 
 	 } 
	 def withsp_alzhdmta(value: Int) : Beneficiary = {
		 this.sp_alzhdmta = value 
		 return this 
 	 } 
	 def withsp_chf(value: Int) : Beneficiary = {
		 this.sp_chf = value 
		 return this 
 	 } 
	 def withsp_chrnkidn(value: Int) : Beneficiary = {
		 this.sp_chrnkidn = value 
		 return this 
 	 } 
	 def withsp_cncr(value: Int) : Beneficiary = {
		 this.sp_cncr = value 
		 return this 
 	 } 
	 def withsp_copd(value: Int) : Beneficiary = {
		 this.sp_copd = value 
		 return this 
 	 } 
	 def withsp_depressn(value: Int) : Beneficiary = {
		 this.sp_depressn = value 
		 return this 
 	 } 
	 def withsp_diabetes(value: Int) : Beneficiary = {
		 this.sp_diabetes = value 
		 return this 
 	 } 
	 def withsp_ischmcht(value: Int) : Beneficiary = {
		 this.sp_ischmcht = value 
		 return this 
 	 } 
	 def withsp_osteoprs(value: Int) : Beneficiary = {
		 this.sp_osteoprs = value 
		 return this 
 	 } 
	 def withsp_ra_oa(value: Int) : Beneficiary = {
		 this.sp_ra_oa = value 
		 return this 
 	 } 
	 def withsp_strketia(value: Int) : Beneficiary = {
		 this.sp_strketia = value 
		 return this 
 	 } 
	 def withmedreimb_ip(value: Double) : Beneficiary = {
		 this.medreimb_ip = value 
		 return this 
 	 } 
	 def withbenres_ip(value: Double) : Beneficiary = {
		 this.benres_ip = value 
		 return this 
 	 } 
	 def withpppymt_ip(value: Double) : Beneficiary = {
		 this.pppymt_ip = value 
		 return this 
 	 } 
	 def withmedreimb_op(value: Double) : Beneficiary = {
		 this.medreimb_op = value 
		 return this 
 	 } 
	 def withbenres_op(value: Double) : Beneficiary = {
		 this.benres_op = value 
		 return this 
 	 } 
	 def withpppymt_op(value: Double) : Beneficiary = {
		 this.pppymt_op = value 
		 return this 
 	 } 
	 def withmedreimb_car(value: Double) : Beneficiary = {
		 this.medreimb_car = value 
		 return this 
 	 } 
	 def withbenres_car(value: Double) : Beneficiary = {
		 this.benres_car = value 
		 return this 
 	 } 
	 def withpppymt_car(value: Double) : Beneficiary = {
		 this.pppymt_car = value 
		 return this 
 	 } 
    def isCaseSensitive(): Boolean = Beneficiary.isCaseSensitive(); 
    def caseSensitiveKey(keyName: String): String = {
      if(isCaseSensitive)
        return keyName;
      else return keyName.toLowerCase;
    }


    
    def this(factory:MessageFactoryInterface) = {
      this(factory, null)
     }
    
    def this(other: Beneficiary) = {
      this(other.getFactory.asInstanceOf[MessageFactoryInterface], other)
    }

}