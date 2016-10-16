
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

    
 
object HL7 extends RDDObject[HL7] with MessageFactoryInterface { 
 
  val log = LogManager.getLogger(getClass)
	type T = HL7 ;
	override def getFullTypeName: String = "com.ligadata.kamanja.samples.messages.HL7"; 
	override def getTypeNameSpace: String = "com.ligadata.kamanja.samples.messages"; 
	override def getTypeName: String = "HL7"; 
	override def getTypeVersion: String = "000000.000001.000000"; 
	override def getSchemaId: Int = 0; 
	override def getTenantId: String = ""; 
	override def createInstance: HL7 = new HL7(HL7); 
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
  
    override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.kamanja.samples.messages" , "name" : "hl7" , "fields":[{ "name" : "desynpuf_id" , "type" : "string"},{ "name" : "clm_id" , "type" : "long"},{ "name" : "clm_from_dt" , "type" : "int"},{ "name" : "clm_thru_dt" , "type" : "int"},{ "name" : "bene_birth_dt" , "type" : "int"},{ "name" : "bene_death_dt" , "type" : "int"},{ "name" : "bene_sex_ident_cd" , "type" : "int"},{ "name" : "bene_race_cd" , "type" : "int"},{ "name" : "bene_esrd_ind" , "type" : "string"},{ "name" : "sp_state_code" , "type" : "int"},{ "name" : "bene_county_cd" , "type" : "int"},{ "name" : "bene_hi_cvrage_tot_mons" , "type" : "int"},{ "name" : "bene_smi_cvrage_tot_mons" , "type" : "int"},{ "name" : "bene_hmo_cvrage_tot_mons" , "type" : "int"},{ "name" : "plan_cvrg_mos_num" , "type" : "int"},{ "name" : "sp_alzhdmta" , "type" : "int"},{ "name" : "sp_chf" , "type" : "int"},{ "name" : "sp_chrnkidn" , "type" : "int"},{ "name" : "sp_cncr" , "type" : "int"},{ "name" : "sp_copd" , "type" : "int"},{ "name" : "sp_depressn" , "type" : "int"},{ "name" : "sp_diabetes" , "type" : "int"},{ "name" : "sp_ischmcht" , "type" : "int"},{ "name" : "sp_osteoprs" , "type" : "int"},{ "name" : "sp_ra_oa" , "type" : "int"},{ "name" : "sp_strketia" , "type" : "int"},{ "name" : "age" , "type" : "int"},{ "name" : "infectious_parasitic_diseases" , "type" : "int"},{ "name" : "neoplasms" , "type" : "int"},{ "name" : "endocrine_nutritional_metabolic_diseases_immunity_disorders" , "type" : "int"},{ "name" : "diseases_blood_blood_forming_organs" , "type" : "int"},{ "name" : "mental_disorders" , "type" : "int"},{ "name" : "diseases_nervous_system_sense_organs" , "type" : "int"},{ "name" : "diseases_circulatory_system" , "type" : "int"},{ "name" : "diseases_respiratory_system" , "type" : "int"},{ "name" : "diseases_digestive_system" , "type" : "int"},{ "name" : "diseases_genitourinary_system" , "type" : "int"},{ "name" : "complications_of_pregnancy_childbirth_the_puerperium" , "type" : "int"},{ "name" : "diseases_skin_subcutaneous_tissue" , "type" : "int"},{ "name" : "diseases_musculoskeletal_system_connective_tissue" , "type" : "int"},{ "name" : "congenital_anomalies" , "type" : "int"},{ "name" : "certain_conditions_originating_in_the_perinatal_period" , "type" : "int"},{ "name" : "symptoms_signs_ill_defined_conditions" , "type" : "int"},{ "name" : "injury_poisoning" , "type" : "int"},{ "name" : "factors_influencing_health_status_contact_with_health_services" , "type" : "int"},{ "name" : "external_causes_of_injury_poisoning" , "type" : "int"},{ "name" : "hypothyroidism" , "type" : "int"},{ "name" : "infarction" , "type" : "int"},{ "name" : "alzheimer" , "type" : "int"},{ "name" : "alzheimer_related" , "type" : "int"},{ "name" : "anemia" , "type" : "int"},{ "name" : "asthma" , "type" : "int"},{ "name" : "atrial_fibrillation" , "type" : "int"},{ "name" : "hyperplasia" , "type" : "int"},{ "name" : "cataract" , "type" : "int"},{ "name" : "kidney_disease" , "type" : "int"},{ "name" : "pulmonary_disease" , "type" : "int"},{ "name" : "depression" , "type" : "int"},{ "name" : "diabetes" , "type" : "int"},{ "name" : "glaucoma" , "type" : "int"},{ "name" : "heart_failure" , "type" : "int"},{ "name" : "hip_pelvic_fracture" , "type" : "int"},{ "name" : "hyperlipidemia" , "type" : "int"},{ "name" : "hypertension" , "type" : "int"},{ "name" : "ischemic_heart_disease" , "type" : "int"},{ "name" : "osteoporosis" , "type" : "int"},{ "name" : "ra_oa" , "type" : "int"},{ "name" : "stroke" , "type" : "int"},{ "name" : "breast_cancer" , "type" : "int"},{ "name" : "colorectal_cancer" , "type" : "int"},{ "name" : "prostate_cancer" , "type" : "int"},{ "name" : "lung_cancer" , "type" : "int"},{ "name" : "endometrial_cancer" , "type" : "int"},{ "name" : "tobacco" , "type" : "int"},{ "name" : "height" , "type" : "double"},{ "name" : "weight" , "type" : "double"},{ "name" : "systolic" , "type" : "double"},{ "name" : "diastolic" , "type" : "double"},{ "name" : "totalcholesterol" , "type" : "double"},{ "name" : "ldl" , "type" : "double"},{ "name" : "triglycerides" , "type" : "double"},{ "name" : "shortnessofbreath" , "type" : "int"},{ "name" : "chestpain" , "type" : "string"},{ "name" : "aatdeficiency" , "type" : "int"},{ "name" : "chroniccough" , "type" : "int"},{ "name" : "chronicsputum" , "type" : "int"}]}""";  

    final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);
      
    override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
      try {
        if (oldVerobj == null) return null;
        oldVerobj match {
          
      case oldVerobj: com.ligadata.kamanja.samples.messages.V1000000.HL7 => { return  convertToVer1000000(oldVerobj); } 
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
  
    private def convertToVer1000000(oldVerobj: com.ligadata.kamanja.samples.messages.V1000000.HL7): com.ligadata.kamanja.samples.messages.V1000000.HL7= {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj HL7") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj HL7");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj HL7");
 override def NeedToTransformData: Boolean = false
    }

class HL7(factory: MessageFactoryInterface, other: HL7) extends MessageInterface(factory) { 
 
  val log = HL7.log

      var attributeTypes = generateAttributeTypes;
      
    private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
      var attributeTypes = new Array[AttributeTypeInfo](86);
   		 attributeTypes(0) = new AttributeTypeInfo("desynpuf_id", 0, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(1) = new AttributeTypeInfo("clm_id", 1, AttributeTypeInfo.TypeCategory.LONG, -1, -1, 0)
		 attributeTypes(2) = new AttributeTypeInfo("clm_from_dt", 2, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(3) = new AttributeTypeInfo("clm_thru_dt", 3, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(4) = new AttributeTypeInfo("bene_birth_dt", 4, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(5) = new AttributeTypeInfo("bene_death_dt", 5, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(6) = new AttributeTypeInfo("bene_sex_ident_cd", 6, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(7) = new AttributeTypeInfo("bene_race_cd", 7, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(8) = new AttributeTypeInfo("bene_esrd_ind", 8, AttributeTypeInfo.TypeCategory.CHAR, -1, -1, 0)
		 attributeTypes(9) = new AttributeTypeInfo("sp_state_code", 9, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(10) = new AttributeTypeInfo("bene_county_cd", 10, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(11) = new AttributeTypeInfo("bene_hi_cvrage_tot_mons", 11, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(12) = new AttributeTypeInfo("bene_smi_cvrage_tot_mons", 12, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(13) = new AttributeTypeInfo("bene_hmo_cvrage_tot_mons", 13, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(14) = new AttributeTypeInfo("plan_cvrg_mos_num", 14, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(15) = new AttributeTypeInfo("sp_alzhdmta", 15, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(16) = new AttributeTypeInfo("sp_chf", 16, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(17) = new AttributeTypeInfo("sp_chrnkidn", 17, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(18) = new AttributeTypeInfo("sp_cncr", 18, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(19) = new AttributeTypeInfo("sp_copd", 19, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(20) = new AttributeTypeInfo("sp_depressn", 20, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(21) = new AttributeTypeInfo("sp_diabetes", 21, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(22) = new AttributeTypeInfo("sp_ischmcht", 22, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(23) = new AttributeTypeInfo("sp_osteoprs", 23, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(24) = new AttributeTypeInfo("sp_ra_oa", 24, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(25) = new AttributeTypeInfo("sp_strketia", 25, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(26) = new AttributeTypeInfo("age", 26, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(27) = new AttributeTypeInfo("infectious_parasitic_diseases", 27, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(28) = new AttributeTypeInfo("neoplasms", 28, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(29) = new AttributeTypeInfo("endocrine_nutritional_metabolic_diseases_immunity_disorders", 29, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(30) = new AttributeTypeInfo("diseases_blood_blood_forming_organs", 30, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(31) = new AttributeTypeInfo("mental_disorders", 31, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(32) = new AttributeTypeInfo("diseases_nervous_system_sense_organs", 32, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(33) = new AttributeTypeInfo("diseases_circulatory_system", 33, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(34) = new AttributeTypeInfo("diseases_respiratory_system", 34, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(35) = new AttributeTypeInfo("diseases_digestive_system", 35, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(36) = new AttributeTypeInfo("diseases_genitourinary_system", 36, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(37) = new AttributeTypeInfo("complications_of_pregnancy_childbirth_the_puerperium", 37, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(38) = new AttributeTypeInfo("diseases_skin_subcutaneous_tissue", 38, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(39) = new AttributeTypeInfo("diseases_musculoskeletal_system_connective_tissue", 39, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(40) = new AttributeTypeInfo("congenital_anomalies", 40, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(41) = new AttributeTypeInfo("certain_conditions_originating_in_the_perinatal_period", 41, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(42) = new AttributeTypeInfo("symptoms_signs_ill_defined_conditions", 42, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(43) = new AttributeTypeInfo("injury_poisoning", 43, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(44) = new AttributeTypeInfo("factors_influencing_health_status_contact_with_health_services", 44, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(45) = new AttributeTypeInfo("external_causes_of_injury_poisoning", 45, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(46) = new AttributeTypeInfo("hypothyroidism", 46, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(47) = new AttributeTypeInfo("infarction", 47, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(48) = new AttributeTypeInfo("alzheimer", 48, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(49) = new AttributeTypeInfo("alzheimer_related", 49, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(50) = new AttributeTypeInfo("anemia", 50, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(51) = new AttributeTypeInfo("asthma", 51, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(52) = new AttributeTypeInfo("atrial_fibrillation", 52, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(53) = new AttributeTypeInfo("hyperplasia", 53, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(54) = new AttributeTypeInfo("cataract", 54, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(55) = new AttributeTypeInfo("kidney_disease", 55, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(56) = new AttributeTypeInfo("pulmonary_disease", 56, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(57) = new AttributeTypeInfo("depression", 57, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(58) = new AttributeTypeInfo("diabetes", 58, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(59) = new AttributeTypeInfo("glaucoma", 59, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(60) = new AttributeTypeInfo("heart_failure", 60, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(61) = new AttributeTypeInfo("hip_pelvic_fracture", 61, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(62) = new AttributeTypeInfo("hyperlipidemia", 62, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(63) = new AttributeTypeInfo("hypertension", 63, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(64) = new AttributeTypeInfo("ischemic_heart_disease", 64, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(65) = new AttributeTypeInfo("osteoporosis", 65, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(66) = new AttributeTypeInfo("ra_oa", 66, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(67) = new AttributeTypeInfo("stroke", 67, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(68) = new AttributeTypeInfo("breast_cancer", 68, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(69) = new AttributeTypeInfo("colorectal_cancer", 69, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(70) = new AttributeTypeInfo("prostate_cancer", 70, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(71) = new AttributeTypeInfo("lung_cancer", 71, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(72) = new AttributeTypeInfo("endometrial_cancer", 72, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(73) = new AttributeTypeInfo("tobacco", 73, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(74) = new AttributeTypeInfo("height", 74, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(75) = new AttributeTypeInfo("weight", 75, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(76) = new AttributeTypeInfo("systolic", 76, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(77) = new AttributeTypeInfo("diastolic", 77, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(78) = new AttributeTypeInfo("totalcholesterol", 78, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(79) = new AttributeTypeInfo("ldl", 79, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(80) = new AttributeTypeInfo("triglycerides", 80, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(81) = new AttributeTypeInfo("shortnessofbreath", 81, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(82) = new AttributeTypeInfo("chestpain", 82, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(83) = new AttributeTypeInfo("aatdeficiency", 83, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(84) = new AttributeTypeInfo("chroniccough", 84, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(85) = new AttributeTypeInfo("chronicsputum", 85, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)

     
      return attributeTypes
    } 
    
		 var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;
    
     if (other != null && other != this) {
      // call copying fields from other to local variables
      fromFunc(other)
    }
    
    override def save: Unit = { HL7.saveOne(this) }
  
    def Clone(): ContainerOrConcept = { HL7.build(this) }

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
 		var clm_from_dt: Int = _; 
 		var clm_thru_dt: Int = _; 
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
 		var age: Int = _; 
 		var infectious_parasitic_diseases: Int = _; 
 		var neoplasms: Int = _; 
 		var endocrine_nutritional_metabolic_diseases_immunity_disorders: Int = _; 
 		var diseases_blood_blood_forming_organs: Int = _; 
 		var mental_disorders: Int = _; 
 		var diseases_nervous_system_sense_organs: Int = _; 
 		var diseases_circulatory_system: Int = _; 
 		var diseases_respiratory_system: Int = _; 
 		var diseases_digestive_system: Int = _; 
 		var diseases_genitourinary_system: Int = _; 
 		var complications_of_pregnancy_childbirth_the_puerperium: Int = _; 
 		var diseases_skin_subcutaneous_tissue: Int = _; 
 		var diseases_musculoskeletal_system_connective_tissue: Int = _; 
 		var congenital_anomalies: Int = _; 
 		var certain_conditions_originating_in_the_perinatal_period: Int = _; 
 		var symptoms_signs_ill_defined_conditions: Int = _; 
 		var injury_poisoning: Int = _; 
 		var factors_influencing_health_status_contact_with_health_services: Int = _; 
 		var external_causes_of_injury_poisoning: Int = _; 
 		var hypothyroidism: Int = _; 
 		var infarction: Int = _; 
 		var alzheimer: Int = _; 
 		var alzheimer_related: Int = _; 
 		var anemia: Int = _; 
 		var asthma: Int = _; 
 		var atrial_fibrillation: Int = _; 
 		var hyperplasia: Int = _; 
 		var cataract: Int = _; 
 		var kidney_disease: Int = _; 
 		var pulmonary_disease: Int = _; 
 		var depression: Int = _; 
 		var diabetes: Int = _; 
 		var glaucoma: Int = _; 
 		var heart_failure: Int = _; 
 		var hip_pelvic_fracture: Int = _; 
 		var hyperlipidemia: Int = _; 
 		var hypertension: Int = _; 
 		var ischemic_heart_disease: Int = _; 
 		var osteoporosis: Int = _; 
 		var ra_oa: Int = _; 
 		var stroke: Int = _; 
 		var breast_cancer: Int = _; 
 		var colorectal_cancer: Int = _; 
 		var prostate_cancer: Int = _; 
 		var lung_cancer: Int = _; 
 		var endometrial_cancer: Int = _; 
 		var tobacco: Int = _; 
 		var height: Double = _; 
 		var weight: Double = _; 
 		var systolic: Double = _; 
 		var diastolic: Double = _; 
 		var totalcholesterol: Double = _; 
 		var ldl: Double = _; 
 		var triglycerides: Double = _; 
 		var shortnessofbreath: Int = _; 
 		var chestpain: String = _; 
 		var aatdeficiency: Int = _; 
 		var chroniccough: Int = _; 
 		var chronicsputum: Int = _; 

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
      val fieldX = ru.typeOf[HL7].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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
   
      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container HL7", null);
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
		case 2 => return this.clm_from_dt.asInstanceOf[AnyRef]; 
		case 3 => return this.clm_thru_dt.asInstanceOf[AnyRef]; 
		case 4 => return this.bene_birth_dt.asInstanceOf[AnyRef]; 
		case 5 => return this.bene_death_dt.asInstanceOf[AnyRef]; 
		case 6 => return this.bene_sex_ident_cd.asInstanceOf[AnyRef]; 
		case 7 => return this.bene_race_cd.asInstanceOf[AnyRef]; 
		case 8 => return this.bene_esrd_ind.asInstanceOf[AnyRef]; 
		case 9 => return this.sp_state_code.asInstanceOf[AnyRef]; 
		case 10 => return this.bene_county_cd.asInstanceOf[AnyRef]; 
		case 11 => return this.bene_hi_cvrage_tot_mons.asInstanceOf[AnyRef]; 
		case 12 => return this.bene_smi_cvrage_tot_mons.asInstanceOf[AnyRef]; 
		case 13 => return this.bene_hmo_cvrage_tot_mons.asInstanceOf[AnyRef]; 
		case 14 => return this.plan_cvrg_mos_num.asInstanceOf[AnyRef]; 
		case 15 => return this.sp_alzhdmta.asInstanceOf[AnyRef]; 
		case 16 => return this.sp_chf.asInstanceOf[AnyRef]; 
		case 17 => return this.sp_chrnkidn.asInstanceOf[AnyRef]; 
		case 18 => return this.sp_cncr.asInstanceOf[AnyRef]; 
		case 19 => return this.sp_copd.asInstanceOf[AnyRef]; 
		case 20 => return this.sp_depressn.asInstanceOf[AnyRef]; 
		case 21 => return this.sp_diabetes.asInstanceOf[AnyRef]; 
		case 22 => return this.sp_ischmcht.asInstanceOf[AnyRef]; 
		case 23 => return this.sp_osteoprs.asInstanceOf[AnyRef]; 
		case 24 => return this.sp_ra_oa.asInstanceOf[AnyRef]; 
		case 25 => return this.sp_strketia.asInstanceOf[AnyRef]; 
		case 26 => return this.age.asInstanceOf[AnyRef]; 
		case 27 => return this.infectious_parasitic_diseases.asInstanceOf[AnyRef]; 
		case 28 => return this.neoplasms.asInstanceOf[AnyRef]; 
		case 29 => return this.endocrine_nutritional_metabolic_diseases_immunity_disorders.asInstanceOf[AnyRef]; 
		case 30 => return this.diseases_blood_blood_forming_organs.asInstanceOf[AnyRef]; 
		case 31 => return this.mental_disorders.asInstanceOf[AnyRef]; 
		case 32 => return this.diseases_nervous_system_sense_organs.asInstanceOf[AnyRef]; 
		case 33 => return this.diseases_circulatory_system.asInstanceOf[AnyRef]; 
		case 34 => return this.diseases_respiratory_system.asInstanceOf[AnyRef]; 
		case 35 => return this.diseases_digestive_system.asInstanceOf[AnyRef]; 
		case 36 => return this.diseases_genitourinary_system.asInstanceOf[AnyRef]; 
		case 37 => return this.complications_of_pregnancy_childbirth_the_puerperium.asInstanceOf[AnyRef]; 
		case 38 => return this.diseases_skin_subcutaneous_tissue.asInstanceOf[AnyRef]; 
		case 39 => return this.diseases_musculoskeletal_system_connective_tissue.asInstanceOf[AnyRef]; 
		case 40 => return this.congenital_anomalies.asInstanceOf[AnyRef]; 
		case 41 => return this.certain_conditions_originating_in_the_perinatal_period.asInstanceOf[AnyRef]; 
		case 42 => return this.symptoms_signs_ill_defined_conditions.asInstanceOf[AnyRef]; 
		case 43 => return this.injury_poisoning.asInstanceOf[AnyRef]; 
		case 44 => return this.factors_influencing_health_status_contact_with_health_services.asInstanceOf[AnyRef]; 
		case 45 => return this.external_causes_of_injury_poisoning.asInstanceOf[AnyRef]; 
		case 46 => return this.hypothyroidism.asInstanceOf[AnyRef]; 
		case 47 => return this.infarction.asInstanceOf[AnyRef]; 
		case 48 => return this.alzheimer.asInstanceOf[AnyRef]; 
		case 49 => return this.alzheimer_related.asInstanceOf[AnyRef]; 
		case 50 => return this.anemia.asInstanceOf[AnyRef]; 
		case 51 => return this.asthma.asInstanceOf[AnyRef]; 
		case 52 => return this.atrial_fibrillation.asInstanceOf[AnyRef]; 
		case 53 => return this.hyperplasia.asInstanceOf[AnyRef]; 
		case 54 => return this.cataract.asInstanceOf[AnyRef]; 
		case 55 => return this.kidney_disease.asInstanceOf[AnyRef]; 
		case 56 => return this.pulmonary_disease.asInstanceOf[AnyRef]; 
		case 57 => return this.depression.asInstanceOf[AnyRef]; 
		case 58 => return this.diabetes.asInstanceOf[AnyRef]; 
		case 59 => return this.glaucoma.asInstanceOf[AnyRef]; 
		case 60 => return this.heart_failure.asInstanceOf[AnyRef]; 
		case 61 => return this.hip_pelvic_fracture.asInstanceOf[AnyRef]; 
		case 62 => return this.hyperlipidemia.asInstanceOf[AnyRef]; 
		case 63 => return this.hypertension.asInstanceOf[AnyRef]; 
		case 64 => return this.ischemic_heart_disease.asInstanceOf[AnyRef]; 
		case 65 => return this.osteoporosis.asInstanceOf[AnyRef]; 
		case 66 => return this.ra_oa.asInstanceOf[AnyRef]; 
		case 67 => return this.stroke.asInstanceOf[AnyRef]; 
		case 68 => return this.breast_cancer.asInstanceOf[AnyRef]; 
		case 69 => return this.colorectal_cancer.asInstanceOf[AnyRef]; 
		case 70 => return this.prostate_cancer.asInstanceOf[AnyRef]; 
		case 71 => return this.lung_cancer.asInstanceOf[AnyRef]; 
		case 72 => return this.endometrial_cancer.asInstanceOf[AnyRef]; 
		case 73 => return this.tobacco.asInstanceOf[AnyRef]; 
		case 74 => return this.height.asInstanceOf[AnyRef]; 
		case 75 => return this.weight.asInstanceOf[AnyRef]; 
		case 76 => return this.systolic.asInstanceOf[AnyRef]; 
		case 77 => return this.diastolic.asInstanceOf[AnyRef]; 
		case 78 => return this.totalcholesterol.asInstanceOf[AnyRef]; 
		case 79 => return this.ldl.asInstanceOf[AnyRef]; 
		case 80 => return this.triglycerides.asInstanceOf[AnyRef]; 
		case 81 => return this.shortnessofbreath.asInstanceOf[AnyRef]; 
		case 82 => return this.chestpain.asInstanceOf[AnyRef]; 
		case 83 => return this.aatdeficiency.asInstanceOf[AnyRef]; 
		case 84 => return this.chroniccough.asInstanceOf[AnyRef]; 
		case 85 => return this.chronicsputum.asInstanceOf[AnyRef]; 

      	 case _ => throw new Exception(s"$index is a bad index for message HL7");
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
      var attributeVals = new Array[AttributeValue](86);
      try{
 				attributeVals(0) = new AttributeValue(this.desynpuf_id, keyTypes("desynpuf_id")) 
				attributeVals(1) = new AttributeValue(this.clm_id, keyTypes("clm_id")) 
				attributeVals(2) = new AttributeValue(this.clm_from_dt, keyTypes("clm_from_dt")) 
				attributeVals(3) = new AttributeValue(this.clm_thru_dt, keyTypes("clm_thru_dt")) 
				attributeVals(4) = new AttributeValue(this.bene_birth_dt, keyTypes("bene_birth_dt")) 
				attributeVals(5) = new AttributeValue(this.bene_death_dt, keyTypes("bene_death_dt")) 
				attributeVals(6) = new AttributeValue(this.bene_sex_ident_cd, keyTypes("bene_sex_ident_cd")) 
				attributeVals(7) = new AttributeValue(this.bene_race_cd, keyTypes("bene_race_cd")) 
				attributeVals(8) = new AttributeValue(this.bene_esrd_ind, keyTypes("bene_esrd_ind")) 
				attributeVals(9) = new AttributeValue(this.sp_state_code, keyTypes("sp_state_code")) 
				attributeVals(10) = new AttributeValue(this.bene_county_cd, keyTypes("bene_county_cd")) 
				attributeVals(11) = new AttributeValue(this.bene_hi_cvrage_tot_mons, keyTypes("bene_hi_cvrage_tot_mons")) 
				attributeVals(12) = new AttributeValue(this.bene_smi_cvrage_tot_mons, keyTypes("bene_smi_cvrage_tot_mons")) 
				attributeVals(13) = new AttributeValue(this.bene_hmo_cvrage_tot_mons, keyTypes("bene_hmo_cvrage_tot_mons")) 
				attributeVals(14) = new AttributeValue(this.plan_cvrg_mos_num, keyTypes("plan_cvrg_mos_num")) 
				attributeVals(15) = new AttributeValue(this.sp_alzhdmta, keyTypes("sp_alzhdmta")) 
				attributeVals(16) = new AttributeValue(this.sp_chf, keyTypes("sp_chf")) 
				attributeVals(17) = new AttributeValue(this.sp_chrnkidn, keyTypes("sp_chrnkidn")) 
				attributeVals(18) = new AttributeValue(this.sp_cncr, keyTypes("sp_cncr")) 
				attributeVals(19) = new AttributeValue(this.sp_copd, keyTypes("sp_copd")) 
				attributeVals(20) = new AttributeValue(this.sp_depressn, keyTypes("sp_depressn")) 
				attributeVals(21) = new AttributeValue(this.sp_diabetes, keyTypes("sp_diabetes")) 
				attributeVals(22) = new AttributeValue(this.sp_ischmcht, keyTypes("sp_ischmcht")) 
				attributeVals(23) = new AttributeValue(this.sp_osteoprs, keyTypes("sp_osteoprs")) 
				attributeVals(24) = new AttributeValue(this.sp_ra_oa, keyTypes("sp_ra_oa")) 
				attributeVals(25) = new AttributeValue(this.sp_strketia, keyTypes("sp_strketia")) 
				attributeVals(26) = new AttributeValue(this.age, keyTypes("age")) 
				attributeVals(27) = new AttributeValue(this.infectious_parasitic_diseases, keyTypes("infectious_parasitic_diseases")) 
				attributeVals(28) = new AttributeValue(this.neoplasms, keyTypes("neoplasms")) 
				attributeVals(29) = new AttributeValue(this.endocrine_nutritional_metabolic_diseases_immunity_disorders, keyTypes("endocrine_nutritional_metabolic_diseases_immunity_disorders")) 
				attributeVals(30) = new AttributeValue(this.diseases_blood_blood_forming_organs, keyTypes("diseases_blood_blood_forming_organs")) 
				attributeVals(31) = new AttributeValue(this.mental_disorders, keyTypes("mental_disorders")) 
				attributeVals(32) = new AttributeValue(this.diseases_nervous_system_sense_organs, keyTypes("diseases_nervous_system_sense_organs")) 
				attributeVals(33) = new AttributeValue(this.diseases_circulatory_system, keyTypes("diseases_circulatory_system")) 
				attributeVals(34) = new AttributeValue(this.diseases_respiratory_system, keyTypes("diseases_respiratory_system")) 
				attributeVals(35) = new AttributeValue(this.diseases_digestive_system, keyTypes("diseases_digestive_system")) 
				attributeVals(36) = new AttributeValue(this.diseases_genitourinary_system, keyTypes("diseases_genitourinary_system")) 
				attributeVals(37) = new AttributeValue(this.complications_of_pregnancy_childbirth_the_puerperium, keyTypes("complications_of_pregnancy_childbirth_the_puerperium")) 
				attributeVals(38) = new AttributeValue(this.diseases_skin_subcutaneous_tissue, keyTypes("diseases_skin_subcutaneous_tissue")) 
				attributeVals(39) = new AttributeValue(this.diseases_musculoskeletal_system_connective_tissue, keyTypes("diseases_musculoskeletal_system_connective_tissue")) 
				attributeVals(40) = new AttributeValue(this.congenital_anomalies, keyTypes("congenital_anomalies")) 
				attributeVals(41) = new AttributeValue(this.certain_conditions_originating_in_the_perinatal_period, keyTypes("certain_conditions_originating_in_the_perinatal_period")) 
				attributeVals(42) = new AttributeValue(this.symptoms_signs_ill_defined_conditions, keyTypes("symptoms_signs_ill_defined_conditions")) 
				attributeVals(43) = new AttributeValue(this.injury_poisoning, keyTypes("injury_poisoning")) 
				attributeVals(44) = new AttributeValue(this.factors_influencing_health_status_contact_with_health_services, keyTypes("factors_influencing_health_status_contact_with_health_services")) 
				attributeVals(45) = new AttributeValue(this.external_causes_of_injury_poisoning, keyTypes("external_causes_of_injury_poisoning")) 
				attributeVals(46) = new AttributeValue(this.hypothyroidism, keyTypes("hypothyroidism")) 
				attributeVals(47) = new AttributeValue(this.infarction, keyTypes("infarction")) 
				attributeVals(48) = new AttributeValue(this.alzheimer, keyTypes("alzheimer")) 
				attributeVals(49) = new AttributeValue(this.alzheimer_related, keyTypes("alzheimer_related")) 
				attributeVals(50) = new AttributeValue(this.anemia, keyTypes("anemia")) 
				attributeVals(51) = new AttributeValue(this.asthma, keyTypes("asthma")) 
				attributeVals(52) = new AttributeValue(this.atrial_fibrillation, keyTypes("atrial_fibrillation")) 
				attributeVals(53) = new AttributeValue(this.hyperplasia, keyTypes("hyperplasia")) 
				attributeVals(54) = new AttributeValue(this.cataract, keyTypes("cataract")) 
				attributeVals(55) = new AttributeValue(this.kidney_disease, keyTypes("kidney_disease")) 
				attributeVals(56) = new AttributeValue(this.pulmonary_disease, keyTypes("pulmonary_disease")) 
				attributeVals(57) = new AttributeValue(this.depression, keyTypes("depression")) 
				attributeVals(58) = new AttributeValue(this.diabetes, keyTypes("diabetes")) 
				attributeVals(59) = new AttributeValue(this.glaucoma, keyTypes("glaucoma")) 
				attributeVals(60) = new AttributeValue(this.heart_failure, keyTypes("heart_failure")) 
				attributeVals(61) = new AttributeValue(this.hip_pelvic_fracture, keyTypes("hip_pelvic_fracture")) 
				attributeVals(62) = new AttributeValue(this.hyperlipidemia, keyTypes("hyperlipidemia")) 
				attributeVals(63) = new AttributeValue(this.hypertension, keyTypes("hypertension")) 
				attributeVals(64) = new AttributeValue(this.ischemic_heart_disease, keyTypes("ischemic_heart_disease")) 
				attributeVals(65) = new AttributeValue(this.osteoporosis, keyTypes("osteoporosis")) 
				attributeVals(66) = new AttributeValue(this.ra_oa, keyTypes("ra_oa")) 
				attributeVals(67) = new AttributeValue(this.stroke, keyTypes("stroke")) 
				attributeVals(68) = new AttributeValue(this.breast_cancer, keyTypes("breast_cancer")) 
				attributeVals(69) = new AttributeValue(this.colorectal_cancer, keyTypes("colorectal_cancer")) 
				attributeVals(70) = new AttributeValue(this.prostate_cancer, keyTypes("prostate_cancer")) 
				attributeVals(71) = new AttributeValue(this.lung_cancer, keyTypes("lung_cancer")) 
				attributeVals(72) = new AttributeValue(this.endometrial_cancer, keyTypes("endometrial_cancer")) 
				attributeVals(73) = new AttributeValue(this.tobacco, keyTypes("tobacco")) 
				attributeVals(74) = new AttributeValue(this.height, keyTypes("height")) 
				attributeVals(75) = new AttributeValue(this.weight, keyTypes("weight")) 
				attributeVals(76) = new AttributeValue(this.systolic, keyTypes("systolic")) 
				attributeVals(77) = new AttributeValue(this.diastolic, keyTypes("diastolic")) 
				attributeVals(78) = new AttributeValue(this.totalcholesterol, keyTypes("totalcholesterol")) 
				attributeVals(79) = new AttributeValue(this.ldl, keyTypes("ldl")) 
				attributeVals(80) = new AttributeValue(this.triglycerides, keyTypes("triglycerides")) 
				attributeVals(81) = new AttributeValue(this.shortnessofbreath, keyTypes("shortnessofbreath")) 
				attributeVals(82) = new AttributeValue(this.chestpain, keyTypes("chestpain")) 
				attributeVals(83) = new AttributeValue(this.aatdeficiency, keyTypes("aatdeficiency")) 
				attributeVals(84) = new AttributeValue(this.chroniccough, keyTypes("chroniccough")) 
				attributeVals(85) = new AttributeValue(this.chronicsputum, keyTypes("chronicsputum")) 
       
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
   
  			 if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message HL7", null)
			 set(keyTypes(key).getIndex, value); 

      }catch {
          case e: Exception => {
          log.debug("", e)
          throw e
        }
      };
      
    }
  
      
    def set(index : Int, value :Any): Unit = {
      if (value == null) throw new Exception(s"Value is null for index $index in message HL7 ")
      try{
        index match {
 				case 0 => { 
				if(value.isInstanceOf[String]) 
				  this.desynpuf_id = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field desynpuf_id in message HL7") 
				} 
				case 1 => { 
				if(value.isInstanceOf[Long]) 
				  this.clm_id = value.asInstanceOf[Long]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_id in message HL7") 
				} 
				case 2 => { 
				if(value.isInstanceOf[Int]) 
				  this.clm_from_dt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_from_dt in message HL7") 
				} 
				case 3 => { 
				if(value.isInstanceOf[Int]) 
				  this.clm_thru_dt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field clm_thru_dt in message HL7") 
				} 
				case 4 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_birth_dt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_birth_dt in message HL7") 
				} 
				case 5 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_death_dt = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_death_dt in message HL7") 
				} 
				case 6 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_sex_ident_cd = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_sex_ident_cd in message HL7") 
				} 
				case 7 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_race_cd = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_race_cd in message HL7") 
				} 
				case 8 => { 
				if(value.isInstanceOf[Char]) 
				  this.bene_esrd_ind = value.asInstanceOf[Char]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_esrd_ind in message HL7") 
				} 
				case 9 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_state_code = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_state_code in message HL7") 
				} 
				case 10 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_county_cd = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_county_cd in message HL7") 
				} 
				case 11 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_hi_cvrage_tot_mons = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_hi_cvrage_tot_mons in message HL7") 
				} 
				case 12 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_smi_cvrage_tot_mons = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_smi_cvrage_tot_mons in message HL7") 
				} 
				case 13 => { 
				if(value.isInstanceOf[Int]) 
				  this.bene_hmo_cvrage_tot_mons = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field bene_hmo_cvrage_tot_mons in message HL7") 
				} 
				case 14 => { 
				if(value.isInstanceOf[Int]) 
				  this.plan_cvrg_mos_num = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field plan_cvrg_mos_num in message HL7") 
				} 
				case 15 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_alzhdmta = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_alzhdmta in message HL7") 
				} 
				case 16 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_chf = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_chf in message HL7") 
				} 
				case 17 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_chrnkidn = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_chrnkidn in message HL7") 
				} 
				case 18 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_cncr = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_cncr in message HL7") 
				} 
				case 19 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_copd = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_copd in message HL7") 
				} 
				case 20 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_depressn = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_depressn in message HL7") 
				} 
				case 21 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_diabetes = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_diabetes in message HL7") 
				} 
				case 22 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_ischmcht = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_ischmcht in message HL7") 
				} 
				case 23 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_osteoprs = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_osteoprs in message HL7") 
				} 
				case 24 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_ra_oa = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_ra_oa in message HL7") 
				} 
				case 25 => { 
				if(value.isInstanceOf[Int]) 
				  this.sp_strketia = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field sp_strketia in message HL7") 
				} 
				case 26 => { 
				if(value.isInstanceOf[Int]) 
				  this.age = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field age in message HL7") 
				} 
				case 27 => { 
				if(value.isInstanceOf[Int]) 
				  this.infectious_parasitic_diseases = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field infectious_parasitic_diseases in message HL7") 
				} 
				case 28 => { 
				if(value.isInstanceOf[Int]) 
				  this.neoplasms = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field neoplasms in message HL7") 
				} 
				case 29 => { 
				if(value.isInstanceOf[Int]) 
				  this.endocrine_nutritional_metabolic_diseases_immunity_disorders = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field endocrine_nutritional_metabolic_diseases_immunity_disorders in message HL7") 
				} 
				case 30 => { 
				if(value.isInstanceOf[Int]) 
				  this.diseases_blood_blood_forming_organs = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field diseases_blood_blood_forming_organs in message HL7") 
				} 
				case 31 => { 
				if(value.isInstanceOf[Int]) 
				  this.mental_disorders = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field mental_disorders in message HL7") 
				} 
				case 32 => { 
				if(value.isInstanceOf[Int]) 
				  this.diseases_nervous_system_sense_organs = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field diseases_nervous_system_sense_organs in message HL7") 
				} 
				case 33 => { 
				if(value.isInstanceOf[Int]) 
				  this.diseases_circulatory_system = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field diseases_circulatory_system in message HL7") 
				} 
				case 34 => { 
				if(value.isInstanceOf[Int]) 
				  this.diseases_respiratory_system = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field diseases_respiratory_system in message HL7") 
				} 
				case 35 => { 
				if(value.isInstanceOf[Int]) 
				  this.diseases_digestive_system = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field diseases_digestive_system in message HL7") 
				} 
				case 36 => { 
				if(value.isInstanceOf[Int]) 
				  this.diseases_genitourinary_system = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field diseases_genitourinary_system in message HL7") 
				} 
				case 37 => { 
				if(value.isInstanceOf[Int]) 
				  this.complications_of_pregnancy_childbirth_the_puerperium = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field complications_of_pregnancy_childbirth_the_puerperium in message HL7") 
				} 
				case 38 => { 
				if(value.isInstanceOf[Int]) 
				  this.diseases_skin_subcutaneous_tissue = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field diseases_skin_subcutaneous_tissue in message HL7") 
				} 
				case 39 => { 
				if(value.isInstanceOf[Int]) 
				  this.diseases_musculoskeletal_system_connective_tissue = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field diseases_musculoskeletal_system_connective_tissue in message HL7") 
				} 
				case 40 => { 
				if(value.isInstanceOf[Int]) 
				  this.congenital_anomalies = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field congenital_anomalies in message HL7") 
				} 
				case 41 => { 
				if(value.isInstanceOf[Int]) 
				  this.certain_conditions_originating_in_the_perinatal_period = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field certain_conditions_originating_in_the_perinatal_period in message HL7") 
				} 
				case 42 => { 
				if(value.isInstanceOf[Int]) 
				  this.symptoms_signs_ill_defined_conditions = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field symptoms_signs_ill_defined_conditions in message HL7") 
				} 
				case 43 => { 
				if(value.isInstanceOf[Int]) 
				  this.injury_poisoning = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field injury_poisoning in message HL7") 
				} 
				case 44 => { 
				if(value.isInstanceOf[Int]) 
				  this.factors_influencing_health_status_contact_with_health_services = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field factors_influencing_health_status_contact_with_health_services in message HL7") 
				} 
				case 45 => { 
				if(value.isInstanceOf[Int]) 
				  this.external_causes_of_injury_poisoning = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field external_causes_of_injury_poisoning in message HL7") 
				} 
				case 46 => { 
				if(value.isInstanceOf[Int]) 
				  this.hypothyroidism = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field hypothyroidism in message HL7") 
				} 
				case 47 => { 
				if(value.isInstanceOf[Int]) 
				  this.infarction = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field infarction in message HL7") 
				} 
				case 48 => { 
				if(value.isInstanceOf[Int]) 
				  this.alzheimer = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field alzheimer in message HL7") 
				} 
				case 49 => { 
				if(value.isInstanceOf[Int]) 
				  this.alzheimer_related = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field alzheimer_related in message HL7") 
				} 
				case 50 => { 
				if(value.isInstanceOf[Int]) 
				  this.anemia = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field anemia in message HL7") 
				} 
				case 51 => { 
				if(value.isInstanceOf[Int]) 
				  this.asthma = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field asthma in message HL7") 
				} 
				case 52 => { 
				if(value.isInstanceOf[Int]) 
				  this.atrial_fibrillation = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field atrial_fibrillation in message HL7") 
				} 
				case 53 => { 
				if(value.isInstanceOf[Int]) 
				  this.hyperplasia = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field hyperplasia in message HL7") 
				} 
				case 54 => { 
				if(value.isInstanceOf[Int]) 
				  this.cataract = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field cataract in message HL7") 
				} 
				case 55 => { 
				if(value.isInstanceOf[Int]) 
				  this.kidney_disease = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field kidney_disease in message HL7") 
				} 
				case 56 => { 
				if(value.isInstanceOf[Int]) 
				  this.pulmonary_disease = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field pulmonary_disease in message HL7") 
				} 
				case 57 => { 
				if(value.isInstanceOf[Int]) 
				  this.depression = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field depression in message HL7") 
				} 
				case 58 => { 
				if(value.isInstanceOf[Int]) 
				  this.diabetes = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field diabetes in message HL7") 
				} 
				case 59 => { 
				if(value.isInstanceOf[Int]) 
				  this.glaucoma = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field glaucoma in message HL7") 
				} 
				case 60 => { 
				if(value.isInstanceOf[Int]) 
				  this.heart_failure = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field heart_failure in message HL7") 
				} 
				case 61 => { 
				if(value.isInstanceOf[Int]) 
				  this.hip_pelvic_fracture = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field hip_pelvic_fracture in message HL7") 
				} 
				case 62 => { 
				if(value.isInstanceOf[Int]) 
				  this.hyperlipidemia = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field hyperlipidemia in message HL7") 
				} 
				case 63 => { 
				if(value.isInstanceOf[Int]) 
				  this.hypertension = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field hypertension in message HL7") 
				} 
				case 64 => { 
				if(value.isInstanceOf[Int]) 
				  this.ischemic_heart_disease = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field ischemic_heart_disease in message HL7") 
				} 
				case 65 => { 
				if(value.isInstanceOf[Int]) 
				  this.osteoporosis = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field osteoporosis in message HL7") 
				} 
				case 66 => { 
				if(value.isInstanceOf[Int]) 
				  this.ra_oa = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field ra_oa in message HL7") 
				} 
				case 67 => { 
				if(value.isInstanceOf[Int]) 
				  this.stroke = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field stroke in message HL7") 
				} 
				case 68 => { 
				if(value.isInstanceOf[Int]) 
				  this.breast_cancer = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field breast_cancer in message HL7") 
				} 
				case 69 => { 
				if(value.isInstanceOf[Int]) 
				  this.colorectal_cancer = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field colorectal_cancer in message HL7") 
				} 
				case 70 => { 
				if(value.isInstanceOf[Int]) 
				  this.prostate_cancer = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field prostate_cancer in message HL7") 
				} 
				case 71 => { 
				if(value.isInstanceOf[Int]) 
				  this.lung_cancer = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field lung_cancer in message HL7") 
				} 
				case 72 => { 
				if(value.isInstanceOf[Int]) 
				  this.endometrial_cancer = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field endometrial_cancer in message HL7") 
				} 
				case 73 => { 
				if(value.isInstanceOf[Int]) 
				  this.tobacco = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field tobacco in message HL7") 
				} 
				case 74 => { 
				if(value.isInstanceOf[Double]) 
				  this.height = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field height in message HL7") 
				} 
				case 75 => { 
				if(value.isInstanceOf[Double]) 
				  this.weight = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field weight in message HL7") 
				} 
				case 76 => { 
				if(value.isInstanceOf[Double]) 
				  this.systolic = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field systolic in message HL7") 
				} 
				case 77 => { 
				if(value.isInstanceOf[Double]) 
				  this.diastolic = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field diastolic in message HL7") 
				} 
				case 78 => { 
				if(value.isInstanceOf[Double]) 
				  this.totalcholesterol = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field totalcholesterol in message HL7") 
				} 
				case 79 => { 
				if(value.isInstanceOf[Double]) 
				  this.ldl = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field ldl in message HL7") 
				} 
				case 80 => { 
				if(value.isInstanceOf[Double]) 
				  this.triglycerides = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field triglycerides in message HL7") 
				} 
				case 81 => { 
				if(value.isInstanceOf[Int]) 
				  this.shortnessofbreath = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field shortnessofbreath in message HL7") 
				} 
				case 82 => { 
				if(value.isInstanceOf[String]) 
				  this.chestpain = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field chestpain in message HL7") 
				} 
				case 83 => { 
				if(value.isInstanceOf[Int]) 
				  this.aatdeficiency = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field aatdeficiency in message HL7") 
				} 
				case 84 => { 
				if(value.isInstanceOf[Int]) 
				  this.chroniccough = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field chroniccough in message HL7") 
				} 
				case 85 => { 
				if(value.isInstanceOf[Int]) 
				  this.chronicsputum = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field chronicsputum in message HL7") 
				} 

        case _ => throw new Exception(s"$index is a bad index for message HL7");
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
  
    private def fromFunc(other: HL7): HL7 = {  
   			this.desynpuf_id = com.ligadata.BaseTypes.StringImpl.Clone(other.desynpuf_id);
			this.clm_id = com.ligadata.BaseTypes.LongImpl.Clone(other.clm_id);
			this.clm_from_dt = com.ligadata.BaseTypes.IntImpl.Clone(other.clm_from_dt);
			this.clm_thru_dt = com.ligadata.BaseTypes.IntImpl.Clone(other.clm_thru_dt);
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
			this.age = com.ligadata.BaseTypes.IntImpl.Clone(other.age);
			this.infectious_parasitic_diseases = com.ligadata.BaseTypes.IntImpl.Clone(other.infectious_parasitic_diseases);
			this.neoplasms = com.ligadata.BaseTypes.IntImpl.Clone(other.neoplasms);
			this.endocrine_nutritional_metabolic_diseases_immunity_disorders = com.ligadata.BaseTypes.IntImpl.Clone(other.endocrine_nutritional_metabolic_diseases_immunity_disorders);
			this.diseases_blood_blood_forming_organs = com.ligadata.BaseTypes.IntImpl.Clone(other.diseases_blood_blood_forming_organs);
			this.mental_disorders = com.ligadata.BaseTypes.IntImpl.Clone(other.mental_disorders);
			this.diseases_nervous_system_sense_organs = com.ligadata.BaseTypes.IntImpl.Clone(other.diseases_nervous_system_sense_organs);
			this.diseases_circulatory_system = com.ligadata.BaseTypes.IntImpl.Clone(other.diseases_circulatory_system);
			this.diseases_respiratory_system = com.ligadata.BaseTypes.IntImpl.Clone(other.diseases_respiratory_system);
			this.diseases_digestive_system = com.ligadata.BaseTypes.IntImpl.Clone(other.diseases_digestive_system);
			this.diseases_genitourinary_system = com.ligadata.BaseTypes.IntImpl.Clone(other.diseases_genitourinary_system);
			this.complications_of_pregnancy_childbirth_the_puerperium = com.ligadata.BaseTypes.IntImpl.Clone(other.complications_of_pregnancy_childbirth_the_puerperium);
			this.diseases_skin_subcutaneous_tissue = com.ligadata.BaseTypes.IntImpl.Clone(other.diseases_skin_subcutaneous_tissue);
			this.diseases_musculoskeletal_system_connective_tissue = com.ligadata.BaseTypes.IntImpl.Clone(other.diseases_musculoskeletal_system_connective_tissue);
			this.congenital_anomalies = com.ligadata.BaseTypes.IntImpl.Clone(other.congenital_anomalies);
			this.certain_conditions_originating_in_the_perinatal_period = com.ligadata.BaseTypes.IntImpl.Clone(other.certain_conditions_originating_in_the_perinatal_period);
			this.symptoms_signs_ill_defined_conditions = com.ligadata.BaseTypes.IntImpl.Clone(other.symptoms_signs_ill_defined_conditions);
			this.injury_poisoning = com.ligadata.BaseTypes.IntImpl.Clone(other.injury_poisoning);
			this.factors_influencing_health_status_contact_with_health_services = com.ligadata.BaseTypes.IntImpl.Clone(other.factors_influencing_health_status_contact_with_health_services);
			this.external_causes_of_injury_poisoning = com.ligadata.BaseTypes.IntImpl.Clone(other.external_causes_of_injury_poisoning);
			this.hypothyroidism = com.ligadata.BaseTypes.IntImpl.Clone(other.hypothyroidism);
			this.infarction = com.ligadata.BaseTypes.IntImpl.Clone(other.infarction);
			this.alzheimer = com.ligadata.BaseTypes.IntImpl.Clone(other.alzheimer);
			this.alzheimer_related = com.ligadata.BaseTypes.IntImpl.Clone(other.alzheimer_related);
			this.anemia = com.ligadata.BaseTypes.IntImpl.Clone(other.anemia);
			this.asthma = com.ligadata.BaseTypes.IntImpl.Clone(other.asthma);
			this.atrial_fibrillation = com.ligadata.BaseTypes.IntImpl.Clone(other.atrial_fibrillation);
			this.hyperplasia = com.ligadata.BaseTypes.IntImpl.Clone(other.hyperplasia);
			this.cataract = com.ligadata.BaseTypes.IntImpl.Clone(other.cataract);
			this.kidney_disease = com.ligadata.BaseTypes.IntImpl.Clone(other.kidney_disease);
			this.pulmonary_disease = com.ligadata.BaseTypes.IntImpl.Clone(other.pulmonary_disease);
			this.depression = com.ligadata.BaseTypes.IntImpl.Clone(other.depression);
			this.diabetes = com.ligadata.BaseTypes.IntImpl.Clone(other.diabetes);
			this.glaucoma = com.ligadata.BaseTypes.IntImpl.Clone(other.glaucoma);
			this.heart_failure = com.ligadata.BaseTypes.IntImpl.Clone(other.heart_failure);
			this.hip_pelvic_fracture = com.ligadata.BaseTypes.IntImpl.Clone(other.hip_pelvic_fracture);
			this.hyperlipidemia = com.ligadata.BaseTypes.IntImpl.Clone(other.hyperlipidemia);
			this.hypertension = com.ligadata.BaseTypes.IntImpl.Clone(other.hypertension);
			this.ischemic_heart_disease = com.ligadata.BaseTypes.IntImpl.Clone(other.ischemic_heart_disease);
			this.osteoporosis = com.ligadata.BaseTypes.IntImpl.Clone(other.osteoporosis);
			this.ra_oa = com.ligadata.BaseTypes.IntImpl.Clone(other.ra_oa);
			this.stroke = com.ligadata.BaseTypes.IntImpl.Clone(other.stroke);
			this.breast_cancer = com.ligadata.BaseTypes.IntImpl.Clone(other.breast_cancer);
			this.colorectal_cancer = com.ligadata.BaseTypes.IntImpl.Clone(other.colorectal_cancer);
			this.prostate_cancer = com.ligadata.BaseTypes.IntImpl.Clone(other.prostate_cancer);
			this.lung_cancer = com.ligadata.BaseTypes.IntImpl.Clone(other.lung_cancer);
			this.endometrial_cancer = com.ligadata.BaseTypes.IntImpl.Clone(other.endometrial_cancer);
			this.tobacco = com.ligadata.BaseTypes.IntImpl.Clone(other.tobacco);
			this.height = com.ligadata.BaseTypes.DoubleImpl.Clone(other.height);
			this.weight = com.ligadata.BaseTypes.DoubleImpl.Clone(other.weight);
			this.systolic = com.ligadata.BaseTypes.DoubleImpl.Clone(other.systolic);
			this.diastolic = com.ligadata.BaseTypes.DoubleImpl.Clone(other.diastolic);
			this.totalcholesterol = com.ligadata.BaseTypes.DoubleImpl.Clone(other.totalcholesterol);
			this.ldl = com.ligadata.BaseTypes.DoubleImpl.Clone(other.ldl);
			this.triglycerides = com.ligadata.BaseTypes.DoubleImpl.Clone(other.triglycerides);
			this.shortnessofbreath = com.ligadata.BaseTypes.IntImpl.Clone(other.shortnessofbreath);
			this.chestpain = com.ligadata.BaseTypes.StringImpl.Clone(other.chestpain);
			this.aatdeficiency = com.ligadata.BaseTypes.IntImpl.Clone(other.aatdeficiency);
			this.chroniccough = com.ligadata.BaseTypes.IntImpl.Clone(other.chroniccough);
			this.chronicsputum = com.ligadata.BaseTypes.IntImpl.Clone(other.chronicsputum);

      this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
      return this;
    }
    
	 def withdesynpuf_id(value: String) : HL7 = {
		 this.desynpuf_id = value 
		 return this 
 	 } 
	 def withclm_id(value: Long) : HL7 = {
		 this.clm_id = value 
		 return this 
 	 } 
	 def withclm_from_dt(value: Int) : HL7 = {
		 this.clm_from_dt = value 
		 return this 
 	 } 
	 def withclm_thru_dt(value: Int) : HL7 = {
		 this.clm_thru_dt = value 
		 return this 
 	 } 
	 def withbene_birth_dt(value: Int) : HL7 = {
		 this.bene_birth_dt = value 
		 return this 
 	 } 
	 def withbene_death_dt(value: Int) : HL7 = {
		 this.bene_death_dt = value 
		 return this 
 	 } 
	 def withbene_sex_ident_cd(value: Int) : HL7 = {
		 this.bene_sex_ident_cd = value 
		 return this 
 	 } 
	 def withbene_race_cd(value: Int) : HL7 = {
		 this.bene_race_cd = value 
		 return this 
 	 } 
	 def withbene_esrd_ind(value: Char) : HL7 = {
		 this.bene_esrd_ind = value 
		 return this 
 	 } 
	 def withsp_state_code(value: Int) : HL7 = {
		 this.sp_state_code = value 
		 return this 
 	 } 
	 def withbene_county_cd(value: Int) : HL7 = {
		 this.bene_county_cd = value 
		 return this 
 	 } 
	 def withbene_hi_cvrage_tot_mons(value: Int) : HL7 = {
		 this.bene_hi_cvrage_tot_mons = value 
		 return this 
 	 } 
	 def withbene_smi_cvrage_tot_mons(value: Int) : HL7 = {
		 this.bene_smi_cvrage_tot_mons = value 
		 return this 
 	 } 
	 def withbene_hmo_cvrage_tot_mons(value: Int) : HL7 = {
		 this.bene_hmo_cvrage_tot_mons = value 
		 return this 
 	 } 
	 def withplan_cvrg_mos_num(value: Int) : HL7 = {
		 this.plan_cvrg_mos_num = value 
		 return this 
 	 } 
	 def withsp_alzhdmta(value: Int) : HL7 = {
		 this.sp_alzhdmta = value 
		 return this 
 	 } 
	 def withsp_chf(value: Int) : HL7 = {
		 this.sp_chf = value 
		 return this 
 	 } 
	 def withsp_chrnkidn(value: Int) : HL7 = {
		 this.sp_chrnkidn = value 
		 return this 
 	 } 
	 def withsp_cncr(value: Int) : HL7 = {
		 this.sp_cncr = value 
		 return this 
 	 } 
	 def withsp_copd(value: Int) : HL7 = {
		 this.sp_copd = value 
		 return this 
 	 } 
	 def withsp_depressn(value: Int) : HL7 = {
		 this.sp_depressn = value 
		 return this 
 	 } 
	 def withsp_diabetes(value: Int) : HL7 = {
		 this.sp_diabetes = value 
		 return this 
 	 } 
	 def withsp_ischmcht(value: Int) : HL7 = {
		 this.sp_ischmcht = value 
		 return this 
 	 } 
	 def withsp_osteoprs(value: Int) : HL7 = {
		 this.sp_osteoprs = value 
		 return this 
 	 } 
	 def withsp_ra_oa(value: Int) : HL7 = {
		 this.sp_ra_oa = value 
		 return this 
 	 } 
	 def withsp_strketia(value: Int) : HL7 = {
		 this.sp_strketia = value 
		 return this 
 	 } 
	 def withage(value: Int) : HL7 = {
		 this.age = value 
		 return this 
 	 } 
	 def withinfectious_parasitic_diseases(value: Int) : HL7 = {
		 this.infectious_parasitic_diseases = value 
		 return this 
 	 } 
	 def withneoplasms(value: Int) : HL7 = {
		 this.neoplasms = value 
		 return this 
 	 } 
	 def withendocrine_nutritional_metabolic_diseases_immunity_disorders(value: Int) : HL7 = {
		 this.endocrine_nutritional_metabolic_diseases_immunity_disorders = value 
		 return this 
 	 } 
	 def withdiseases_blood_blood_forming_organs(value: Int) : HL7 = {
		 this.diseases_blood_blood_forming_organs = value 
		 return this 
 	 } 
	 def withmental_disorders(value: Int) : HL7 = {
		 this.mental_disorders = value 
		 return this 
 	 } 
	 def withdiseases_nervous_system_sense_organs(value: Int) : HL7 = {
		 this.diseases_nervous_system_sense_organs = value 
		 return this 
 	 } 
	 def withdiseases_circulatory_system(value: Int) : HL7 = {
		 this.diseases_circulatory_system = value 
		 return this 
 	 } 
	 def withdiseases_respiratory_system(value: Int) : HL7 = {
		 this.diseases_respiratory_system = value 
		 return this 
 	 } 
	 def withdiseases_digestive_system(value: Int) : HL7 = {
		 this.diseases_digestive_system = value 
		 return this 
 	 } 
	 def withdiseases_genitourinary_system(value: Int) : HL7 = {
		 this.diseases_genitourinary_system = value 
		 return this 
 	 } 
	 def withcomplications_of_pregnancy_childbirth_the_puerperium(value: Int) : HL7 = {
		 this.complications_of_pregnancy_childbirth_the_puerperium = value 
		 return this 
 	 } 
	 def withdiseases_skin_subcutaneous_tissue(value: Int) : HL7 = {
		 this.diseases_skin_subcutaneous_tissue = value 
		 return this 
 	 } 
	 def withdiseases_musculoskeletal_system_connective_tissue(value: Int) : HL7 = {
		 this.diseases_musculoskeletal_system_connective_tissue = value 
		 return this 
 	 } 
	 def withcongenital_anomalies(value: Int) : HL7 = {
		 this.congenital_anomalies = value 
		 return this 
 	 } 
	 def withcertain_conditions_originating_in_the_perinatal_period(value: Int) : HL7 = {
		 this.certain_conditions_originating_in_the_perinatal_period = value 
		 return this 
 	 } 
	 def withsymptoms_signs_ill_defined_conditions(value: Int) : HL7 = {
		 this.symptoms_signs_ill_defined_conditions = value 
		 return this 
 	 } 
	 def withinjury_poisoning(value: Int) : HL7 = {
		 this.injury_poisoning = value 
		 return this 
 	 } 
	 def withfactors_influencing_health_status_contact_with_health_services(value: Int) : HL7 = {
		 this.factors_influencing_health_status_contact_with_health_services = value 
		 return this 
 	 } 
	 def withexternal_causes_of_injury_poisoning(value: Int) : HL7 = {
		 this.external_causes_of_injury_poisoning = value 
		 return this 
 	 } 
	 def withhypothyroidism(value: Int) : HL7 = {
		 this.hypothyroidism = value 
		 return this 
 	 } 
	 def withinfarction(value: Int) : HL7 = {
		 this.infarction = value 
		 return this 
 	 } 
	 def withalzheimer(value: Int) : HL7 = {
		 this.alzheimer = value 
		 return this 
 	 } 
	 def withalzheimer_related(value: Int) : HL7 = {
		 this.alzheimer_related = value 
		 return this 
 	 } 
	 def withanemia(value: Int) : HL7 = {
		 this.anemia = value 
		 return this 
 	 } 
	 def withasthma(value: Int) : HL7 = {
		 this.asthma = value 
		 return this 
 	 } 
	 def withatrial_fibrillation(value: Int) : HL7 = {
		 this.atrial_fibrillation = value 
		 return this 
 	 } 
	 def withhyperplasia(value: Int) : HL7 = {
		 this.hyperplasia = value 
		 return this 
 	 } 
	 def withcataract(value: Int) : HL7 = {
		 this.cataract = value 
		 return this 
 	 } 
	 def withkidney_disease(value: Int) : HL7 = {
		 this.kidney_disease = value 
		 return this 
 	 } 
	 def withpulmonary_disease(value: Int) : HL7 = {
		 this.pulmonary_disease = value 
		 return this 
 	 } 
	 def withdepression(value: Int) : HL7 = {
		 this.depression = value 
		 return this 
 	 } 
	 def withdiabetes(value: Int) : HL7 = {
		 this.diabetes = value 
		 return this 
 	 } 
	 def withglaucoma(value: Int) : HL7 = {
		 this.glaucoma = value 
		 return this 
 	 } 
	 def withheart_failure(value: Int) : HL7 = {
		 this.heart_failure = value 
		 return this 
 	 } 
	 def withhip_pelvic_fracture(value: Int) : HL7 = {
		 this.hip_pelvic_fracture = value 
		 return this 
 	 } 
	 def withhyperlipidemia(value: Int) : HL7 = {
		 this.hyperlipidemia = value 
		 return this 
 	 } 
	 def withhypertension(value: Int) : HL7 = {
		 this.hypertension = value 
		 return this 
 	 } 
	 def withischemic_heart_disease(value: Int) : HL7 = {
		 this.ischemic_heart_disease = value 
		 return this 
 	 } 
	 def withosteoporosis(value: Int) : HL7 = {
		 this.osteoporosis = value 
		 return this 
 	 } 
	 def withra_oa(value: Int) : HL7 = {
		 this.ra_oa = value 
		 return this 
 	 } 
	 def withstroke(value: Int) : HL7 = {
		 this.stroke = value 
		 return this 
 	 } 
	 def withbreast_cancer(value: Int) : HL7 = {
		 this.breast_cancer = value 
		 return this 
 	 } 
	 def withcolorectal_cancer(value: Int) : HL7 = {
		 this.colorectal_cancer = value 
		 return this 
 	 } 
	 def withprostate_cancer(value: Int) : HL7 = {
		 this.prostate_cancer = value 
		 return this 
 	 } 
	 def withlung_cancer(value: Int) : HL7 = {
		 this.lung_cancer = value 
		 return this 
 	 } 
	 def withendometrial_cancer(value: Int) : HL7 = {
		 this.endometrial_cancer = value 
		 return this 
 	 } 
	 def withtobacco(value: Int) : HL7 = {
		 this.tobacco = value 
		 return this 
 	 } 
	 def withheight(value: Double) : HL7 = {
		 this.height = value 
		 return this 
 	 } 
	 def withweight(value: Double) : HL7 = {
		 this.weight = value 
		 return this 
 	 } 
	 def withsystolic(value: Double) : HL7 = {
		 this.systolic = value 
		 return this 
 	 } 
	 def withdiastolic(value: Double) : HL7 = {
		 this.diastolic = value 
		 return this 
 	 } 
	 def withtotalcholesterol(value: Double) : HL7 = {
		 this.totalcholesterol = value 
		 return this 
 	 } 
	 def withldl(value: Double) : HL7 = {
		 this.ldl = value 
		 return this 
 	 } 
	 def withtriglycerides(value: Double) : HL7 = {
		 this.triglycerides = value 
		 return this 
 	 } 
	 def withshortnessofbreath(value: Int) : HL7 = {
		 this.shortnessofbreath = value 
		 return this 
 	 } 
	 def withchestpain(value: String) : HL7 = {
		 this.chestpain = value 
		 return this 
 	 } 
	 def withaatdeficiency(value: Int) : HL7 = {
		 this.aatdeficiency = value 
		 return this 
 	 } 
	 def withchroniccough(value: Int) : HL7 = {
		 this.chroniccough = value 
		 return this 
 	 } 
	 def withchronicsputum(value: Int) : HL7 = {
		 this.chronicsputum = value 
		 return this 
 	 } 
    def isCaseSensitive(): Boolean = HL7.isCaseSensitive(); 
    def caseSensitiveKey(keyName: String): String = {
      if(isCaseSensitive)
        return keyName;
      else return keyName.toLowerCase;
    }


    
    def this(factory:MessageFactoryInterface) = {
      this(factory, null)
     }
    
    def this(other: HL7) = {
      this(other.getFactory.asInstanceOf[MessageFactoryInterface], other)
    }

}