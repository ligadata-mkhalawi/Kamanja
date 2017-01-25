
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

    
 
object AirRefillCS5 extends RDDObject[AirRefillCS5] with MessageFactoryInterface { 
 
  val log = LogManager.getLogger(getClass)
	type T = AirRefillCS5 ;
	override def getFullTypeName: String = "com.ligadata.messages.AirRefillCS5"; 
	override def getTypeNameSpace: String = "com.ligadata.messages"; 
	override def getTypeName: String = "AirRefillCS5"; 
	override def getTypeVersion: String = "000000.000001.000000"; 
	override def getSchemaId: Int = 0; 
	override def getTenantId: String = ""; 
	override def createInstance: AirRefillCS5 = new AirRefillCS5(AirRefillCS5); 
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
  
    override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.messages" , "name" : "airrefillcs5" , "fields":[{ "name" : "originnodetype" , "type" : "string"},{ "name" : "originhostname" , "type" : "string"},{ "name" : "originfileid" , "type" : "string"},{ "name" : "origintransactionid" , "type" : "string"},{ "name" : "originoperatorid" , "type" : "string"},{ "name" : "origintimestamp" , "type" : "string"},{ "name" : "hostname" , "type" : "string"},{ "name" : "localsequencenumber" , "type" : "double"},{ "name" : "timestamp" , "type" : "string"},{ "name" : "currentserviceclass" , "type" : "double"},{ "name" : "voucherbasedrefill" , "type" : "string"},{ "name" : "transactiontype" , "type" : "string"},{ "name" : "transactioncode" , "type" : "string"},{ "name" : "transactionamount" , "type" : "double"},{ "name" : "transactioncurrency" , "type" : "string"},{ "name" : "refillamountconverted" , "type" : "double"},{ "name" : "refilldivisionamount" , "type" : "double"},{ "name" : "refilltype" , "type" : "double"},{ "name" : "refillprofileid" , "type" : "string"},{ "name" : "segmentationid" , "type" : "string"},{ "name" : "voucherserialnumber" , "type" : "string"},{ "name" : "vouchergroupid" , "type" : "string"},{ "name" : "accountnumber" , "type" : "string"},{ "name" : "accountcurrency" , "type" : "string"},{ "name" : "subscribernumber" , "type" : "string"},{ "name" : "promotionannouncementcode" , "type" : "string"},{ "name" : "accountflagsbef" , "type" : "string"},{ "name" : "accountbalancebef" , "type" : "double"},{ "name" : "accumulatedrefillvaluebef" , "type" : "double"},{ "name" : "accumulatedrefillcounterbef" , "type" : "double"},{ "name" : "accumulatedprogressionvaluebef" , "type" : "double"},{ "name" : "accumulatedprogrcounterbef" , "type" : "double"},{ "name" : "creditclearanceperiodbef" , "type" : "double"},{ "name" : "dedicatedaccount1stidbef" , "type" : "double"},{ "name" : "account1stcampaignidentbef" , "type" : "double"},{ "name" : "account1strefilldivamountbef" , "type" : "double"},{ "name" : "account1strefillpromdivamntbef" , "type" : "double"},{ "name" : "account1stbalancebef" , "type" : "double"},{ "name" : "clearedaccount1stvaluebef" , "type" : "double"},{ "name" : "dedicatedaccount2ndidbef" , "type" : "double"},{ "name" : "account2ndcampaignidentifbef" , "type" : "double"},{ "name" : "account2ndrefilldivamountbef" , "type" : "double"},{ "name" : "account2ndrefilpromodivamntbef" , "type" : "double"},{ "name" : "account2ndbalancebef" , "type" : "double"},{ "name" : "clearedaccount2ndvaluebef" , "type" : "double"},{ "name" : "dedicatedaccount3rdidbef" , "type" : "double"},{ "name" : "account3rdcampaignidentbef" , "type" : "double"},{ "name" : "account3rdrefilldivamountbef" , "type" : "double"},{ "name" : "account3rdrefilpromodivamntbef" , "type" : "double"},{ "name" : "account3rdbalancebef" , "type" : "double"},{ "name" : "clearedaccount3rdvaluebef" , "type" : "double"},{ "name" : "dedicatedaccount4thidbef" , "type" : "double"},{ "name" : "account4thcampaignidentbef" , "type" : "double"},{ "name" : "account4threfilldivamountbef" , "type" : "double"},{ "name" : "account4threfilpromodivamntbef" , "type" : "double"},{ "name" : "account4thbalancebef" , "type" : "double"},{ "name" : "clearedaccount4thvaluebef" , "type" : "double"},{ "name" : "dedicatedaccount5thidbef" , "type" : "double"},{ "name" : "account5thcampaignidentbef" , "type" : "double"},{ "name" : "account5threfilldivamountbef" , "type" : "double"},{ "name" : "account5threfilpromodivamntbef" , "type" : "double"},{ "name" : "account5thbalancebef" , "type" : "double"},{ "name" : "clearedaccount5thvaluebef" , "type" : "double"},{ "name" : "dedicatedaccount6thidbef" , "type" : "double"},{ "name" : "account6thcampaignidentbef" , "type" : "double"},{ "name" : "account6threfilldivamountbef" , "type" : "double"},{ "name" : "account6threfilpromodivamntbef" , "type" : "double"},{ "name" : "account6thbalancebef" , "type" : "double"},{ "name" : "clearedaccount6thvaluebef" , "type" : "double"},{ "name" : "dedicatedaccount7thidbef" , "type" : "double"},{ "name" : "account7thcampaignidentbef" , "type" : "double"},{ "name" : "account7threfilldivamountbef" , "type" : "double"},{ "name" : "account7threfilpromodivamntbef" , "type" : "double"},{ "name" : "account7thbalancebef" , "type" : "double"},{ "name" : "clearedaccount7thvaluebef" , "type" : "double"},{ "name" : "dedicatedaccount8thidbef" , "type" : "double"},{ "name" : "account8thcampaignidentbef" , "type" : "double"},{ "name" : "account8threfilldivamountbef" , "type" : "double"},{ "name" : "account8threfilpromodivamntbef" , "type" : "double"},{ "name" : "account8thbalancebef" , "type" : "double"},{ "name" : "clearedaccount8thvaluebef" , "type" : "double"},{ "name" : "dedicatedaccount9thidbef" , "type" : "double"},{ "name" : "account9thcampaignidentbef" , "type" : "double"},{ "name" : "account9threfilldivamountbef" , "type" : "double"},{ "name" : "account9threfilpromodivamntbef" , "type" : "double"},{ "name" : "account9thbalancebef" , "type" : "double"},{ "name" : "clearedaccount9thvaluebef" , "type" : "double"},{ "name" : "dedicatedaccount10thidbef" , "type" : "double"},{ "name" : "account10thcampaignidentbef" , "type" : "double"},{ "name" : "account10threfilldivamountbef" , "type" : "double"},{ "name" : "account10threfilpromdivamntbef" , "type" : "double"},{ "name" : "account10thbalancebef" , "type" : "double"},{ "name" : "clearedaccount10thvaluebef" , "type" : "double"},{ "name" : "promotionplan" , "type" : "string"},{ "name" : "permanentserviceclassbef" , "type" : "double"},{ "name" : "temporaryserviceclassbef" , "type" : "double"},{ "name" : "temporaryservclassexpdatebef" , "type" : "string"},{ "name" : "refilloptionbef" , "type" : "string"},{ "name" : "servicefeeexpirydatebef" , "type" : "string"},{ "name" : "serviceremovalgraceperiodbef" , "type" : "double"},{ "name" : "serviceofferingbef" , "type" : "string"},{ "name" : "supervisionexpirydatebef" , "type" : "string"},{ "name" : "usageaccumulator1stidbef" , "type" : "double"},{ "name" : "usageaccumulator1stvaluebef" , "type" : "double"},{ "name" : "usageaccumulator2ndidbef" , "type" : "double"},{ "name" : "usageaccumulator2ndvaluebef" , "type" : "double"},{ "name" : "usageaccumulator3rdidbef" , "type" : "double"},{ "name" : "usageaccumulator3rdvaluebef" , "type" : "double"},{ "name" : "usageaccumulator4thidbef" , "type" : "double"},{ "name" : "usageaccumulator4thvaluebef" , "type" : "double"},{ "name" : "usageaccumulator5thidbef" , "type" : "double"},{ "name" : "usageaccumulator5thvaluebef" , "type" : "double"},{ "name" : "usageaccumulator6thidbef" , "type" : "double"},{ "name" : "usageaccumulator6thvaluebef" , "type" : "double"},{ "name" : "usageaccumulator7thidbef" , "type" : "double"},{ "name" : "usageaccumulator7thvaluebef" , "type" : "double"},{ "name" : "usageaccumulator8thidbef" , "type" : "double"},{ "name" : "usageaccumulator8thvaluebef" , "type" : "double"},{ "name" : "usageaccumulator9thidbef" , "type" : "double"},{ "name" : "usageaccumulator9thvaluebef" , "type" : "double"},{ "name" : "usageaccumulator10thidbef" , "type" : "double"},{ "name" : "usageaccumulator10thvaluebef" , "type" : "double"},{ "name" : "communityidbef1" , "type" : "string"},{ "name" : "communityidbef2" , "type" : "string"},{ "name" : "communityidbef3" , "type" : "string"},{ "name" : "accountflagsaft" , "type" : "string"},{ "name" : "accountbalanceaft" , "type" : "double"},{ "name" : "accumulatedrefillvalueaft" , "type" : "double"},{ "name" : "accumulatedrefillcounteraft" , "type" : "double"},{ "name" : "accumulatedprogressionvalueaft" , "type" : "double"},{ "name" : "accumulatedprogconteraft" , "type" : "double"},{ "name" : "creditclearanceperiodaft" , "type" : "double"},{ "name" : "dedicatedaccount1stidaft" , "type" : "double"},{ "name" : "account1stcampaignidentaft" , "type" : "double"},{ "name" : "account1strefilldivamountaft" , "type" : "double"},{ "name" : "account1strefilpromodivamntaft" , "type" : "double"},{ "name" : "account1stbalanceaft" , "type" : "double"},{ "name" : "clearedaccount1stvalueaft" , "type" : "double"},{ "name" : "dedicatedaccount2ndidaft" , "type" : "double"},{ "name" : "account2ndcampaignidentaft" , "type" : "double"},{ "name" : "account2ndrefilldivamountaft" , "type" : "double"},{ "name" : "account2ndrefilpromodivamntaft" , "type" : "double"},{ "name" : "account2ndbalanceaft" , "type" : "double"},{ "name" : "clearedaccount2ndvalueaft" , "type" : "double"},{ "name" : "dedicatedaccount3rdidaft" , "type" : "double"},{ "name" : "account3rdcampaignidentaft" , "type" : "double"},{ "name" : "account3rdrefilldivamountaft" , "type" : "double"},{ "name" : "account3rdrefilpromodivamntaft" , "type" : "double"},{ "name" : "account3rdbalanceaft" , "type" : "double"},{ "name" : "clearedaccount3rdvalueaft" , "type" : "double"},{ "name" : "dedicatedaccount4thidaft" , "type" : "double"},{ "name" : "account4thcampaignidentaft" , "type" : "double"},{ "name" : "account4threfilldivamountaft" , "type" : "double"},{ "name" : "account4threfilpromodivamntaft" , "type" : "double"},{ "name" : "account4thbalanceaft" , "type" : "double"},{ "name" : "clearedaccount4thvalueaft" , "type" : "double"},{ "name" : "dedicatedaccount5thidaft" , "type" : "double"},{ "name" : "account5thcampaignidentaft" , "type" : "double"},{ "name" : "account5threfilldivamountaft" , "type" : "double"},{ "name" : "account5threfilpromodivamntaft" , "type" : "double"},{ "name" : "account5thbalanceaft" , "type" : "double"},{ "name" : "clearedaccount5thvalueaft" , "type" : "double"},{ "name" : "dedicatedaccount6thidaft" , "type" : "double"},{ "name" : "account6thcampaignidentaft" , "type" : "double"},{ "name" : "account6threfilldivamountaft" , "type" : "double"},{ "name" : "account6threfilpromodivamntaft" , "type" : "double"},{ "name" : "account6thbalanceaft" , "type" : "double"},{ "name" : "clearedaccount6thvalueaft" , "type" : "double"},{ "name" : "dedicatedaccount7thidaft" , "type" : "double"},{ "name" : "account7thcampaignidentaft" , "type" : "double"},{ "name" : "account7threfilldivamountaft" , "type" : "double"},{ "name" : "account7threfilpromodivamntaft" , "type" : "double"},{ "name" : "account7thbalanceaft" , "type" : "double"},{ "name" : "clearedaccount7thvalueaft" , "type" : "double"},{ "name" : "dedicatedaccount8thidaft" , "type" : "double"},{ "name" : "account8thcampaignidentaft" , "type" : "double"},{ "name" : "account8threfilldivamountaft" , "type" : "double"},{ "name" : "account8threfilpromodivamntaft" , "type" : "double"},{ "name" : "account8thbalanceaft" , "type" : "double"},{ "name" : "clearedaccount8thvalueaft" , "type" : "double"},{ "name" : "dedicatedaccount9thidaft" , "type" : "double"},{ "name" : "account9thcampaignidentaft" , "type" : "double"},{ "name" : "account9threfilldivamountaft" , "type" : "double"},{ "name" : "account9threfilpromodivamntaft" , "type" : "double"},{ "name" : "account9thbalanceaft" , "type" : "double"},{ "name" : "clearedaccount9thvalueaft" , "type" : "double"},{ "name" : "dedicatedaccount10thidaft" , "type" : "double"},{ "name" : "account10thcampaignidentaft" , "type" : "double"},{ "name" : "account10threfilldivamountaft" , "type" : "double"},{ "name" : "account10threfilpromdivamntaft" , "type" : "double"},{ "name" : "account10thbalanceaft" , "type" : "double"},{ "name" : "clearedaccount10thvalueaft" , "type" : "double"},{ "name" : "promotionplanaft" , "type" : "string"},{ "name" : "permanentserviceclassaft" , "type" : "double"},{ "name" : "temporaryserviceclassaft" , "type" : "double"},{ "name" : "temporaryservclasexpirydateaft" , "type" : "double"},{ "name" : "refilloptionaft" , "type" : "string"},{ "name" : "servicefeeexpirydateaft" , "type" : "string"},{ "name" : "serviceremovalgraceperiodaft" , "type" : "double"},{ "name" : "serviceofferingaft" , "type" : "string"},{ "name" : "supervisionexpirydateaft" , "type" : "string"},{ "name" : "usageaccumulator1stidaft" , "type" : "double"},{ "name" : "usageaccumulator1stvalueaft" , "type" : "double"},{ "name" : "usageaccumulator2ndidaft" , "type" : "double"},{ "name" : "usageaccumulator2ndvalueaft" , "type" : "double"},{ "name" : "usageaccumulator3rdidaft" , "type" : "double"},{ "name" : "usageaccumulator3rdvalueaft" , "type" : "double"},{ "name" : "usageaccumulator4thidaft" , "type" : "double"},{ "name" : "usageaccumulator4thvalueaft" , "type" : "double"},{ "name" : "usageaccumulator5thidaft" , "type" : "double"},{ "name" : "usageaccumulator5thvalueaft" , "type" : "double"},{ "name" : "usageaccumulator6thidaft" , "type" : "double"},{ "name" : "usageaccumulator6thvalueaft" , "type" : "double"},{ "name" : "usageaccumulator7thidaft" , "type" : "double"},{ "name" : "usageaccumulator7thvalueaft" , "type" : "double"},{ "name" : "usageaccumulator8thidaft" , "type" : "double"},{ "name" : "usageaccumulator8thvalueaft" , "type" : "double"},{ "name" : "usageaccumulator9thidaft" , "type" : "double"},{ "name" : "usageaccumulator9thvalueaft" , "type" : "double"},{ "name" : "usageaccumulator10thidaft" , "type" : "double"},{ "name" : "usageaccumulator10thvalueaft" , "type" : "double"},{ "name" : "communityidaft1" , "type" : "string"},{ "name" : "communityidaft2" , "type" : "string"},{ "name" : "communityidaft3" , "type" : "string"},{ "name" : "refillpromodivisionamount" , "type" : "double"},{ "name" : "supervisiondayspromopart" , "type" : "double"},{ "name" : "supervisiondayssurplus" , "type" : "double"},{ "name" : "servicefeedayspromopart" , "type" : "double"},{ "name" : "servicefeedayssurplus" , "type" : "double"},{ "name" : "maximumservicefeeperiod" , "type" : "double"},{ "name" : "maximumsupervisionperiod" , "type" : "double"},{ "name" : "activationdate" , "type" : "string"},{ "name" : "welcomestatus" , "type" : "double"},{ "name" : "voucheragent" , "type" : "string"},{ "name" : "promotionplanallocstartdate" , "type" : "string"},{ "name" : "accountgroupid" , "type" : "string"},{ "name" : "externaldata1" , "type" : "string"},{ "name" : "externaldata2" , "type" : "string"},{ "name" : "externaldata3" , "type" : "string"},{ "name" : "externaldata4" , "type" : "string"},{ "name" : "locationnumber" , "type" : "string"},{ "name" : "voucheractivationcode" , "type" : "string"},{ "name" : "accountcurrencycleared" , "type" : "string"},{ "name" : "ignoreserviceclasshierarchy" , "type" : "string"},{ "name" : "accounthomeregion" , "type" : "string"},{ "name" : "subscriberregion" , "type" : "string"},{ "name" : "voucherregion" , "type" : "string"},{ "name" : "promotionplanallocenddate" , "type" : "string"},{ "name" : "requestedrefilltype" , "type" : "string"},{ "name" : "accountexpiry1stdatebef" , "type" : "string"},{ "name" : "accountexpiry1stdateaft" , "type" : "string"},{ "name" : "accountexpiry2nddatebef" , "type" : "string"},{ "name" : "accountexpiry2nddateaft" , "type" : "string"},{ "name" : "accountexpiry3rddatebef" , "type" : "string"},{ "name" : "accountexpiry3rddateaft" , "type" : "string"},{ "name" : "accountexpiry4thdatebef" , "type" : "string"},{ "name" : "accountexpiry4thdateaft" , "type" : "string"},{ "name" : "accountexpiry5thdatebef" , "type" : "string"},{ "name" : "accountexpiry5thdateaft" , "type" : "string"},{ "name" : "accountexpiry6thdatebef" , "type" : "string"},{ "name" : "accountexpiry6thdateaft" , "type" : "string"},{ "name" : "accountexpiry7thdatebef" , "type" : "string"},{ "name" : "accountexpiry7thdateaft" , "type" : "string"},{ "name" : "accountexpiry8thdatebef" , "type" : "string"},{ "name" : "accountexpiry8thdateaft" , "type" : "string"},{ "name" : "accountexpiry9thdatebef" , "type" : "string"},{ "name" : "accountexpiry9thdateaft" , "type" : "string"},{ "name" : "accountexpiry10thdatebef" , "type" : "string"},{ "name" : "accountexpiry10thdateaft" , "type" : "string"},{ "name" : "rechargedivpartmain" , "type" : "double"},{ "name" : "rechargedivpartda1st" , "type" : "double"},{ "name" : "rechargedivpartda2nd" , "type" : "double"},{ "name" : "rechargedivpartda3rd" , "type" : "double"},{ "name" : "rechargedivpartda4th" , "type" : "double"},{ "name" : "rechargedivpartda5th" , "type" : "double"},{ "name" : "accumulatedprogressionvalueres" , "type" : "double"},{ "name" : "rechargedivpartpmain" , "type" : "double"},{ "name" : "rechargedivpartpda1st" , "type" : "double"},{ "name" : "rechargedivpartpda2nd" , "type" : "double"},{ "name" : "rechargedivpartpda3rd" , "type" : "double"},{ "name" : "rechargedivpartpda4th" , "type" : "double"},{ "name" : "rechargedivpartpda5th" , "type" : "double"},{ "name" : "rechargedivpartda6th" , "type" : "double"},{ "name" : "rechargedivpartda7th" , "type" : "double"},{ "name" : "rechargedivpartda8th" , "type" : "double"},{ "name" : "rechargedivpartda9th" , "type" : "double"},{ "name" : "rechargedivpartda10th" , "type" : "double"},{ "name" : "rechargedivpartpda6th" , "type" : "double"},{ "name" : "rechargedivpartpda7th" , "type" : "double"},{ "name" : "rechargedivpartpda8th" , "type" : "double"},{ "name" : "rechargedivpartpda9th" , "type" : "double"},{ "name" : "rechargedivpartpda10th" , "type" : "double"},{ "name" : "dedicatedaccountunit1stbef" , "type" : "double"},{ "name" : "accountstartdate1stbef" , "type" : "string"},{ "name" : "refilldivunits1stbef" , "type" : "double"},{ "name" : "refillpromodivunits1stbef" , "type" : "double"},{ "name" : "unitbalance1stbef" , "type" : "double"},{ "name" : "clearedunits1stbef" , "type" : "double"},{ "name" : "realmoneyflag1stbef" , "type" : "double"},{ "name" : "dedicatedaccountunit2ndbef" , "type" : "double"},{ "name" : "accountstartdate2ndbef" , "type" : "string"},{ "name" : "refilldivunits2ndbef" , "type" : "double"},{ "name" : "refillpromodivunits2ndbef" , "type" : "double"},{ "name" : "unitbalance2ndbef" , "type" : "double"},{ "name" : "clearedunits2ndbef" , "type" : "double"},{ "name" : "realmoneyflag2ndbef" , "type" : "double"},{ "name" : "dedicatedaccountunit3rdbef" , "type" : "double"},{ "name" : "accountstartdate3rdbef" , "type" : "string"},{ "name" : "refilldivunits3rdbef" , "type" : "double"},{ "name" : "refillpromodivunits3rdbef" , "type" : "double"},{ "name" : "unitbalance3rdbef" , "type" : "double"},{ "name" : "clearedunits3rdbef" , "type" : "double"},{ "name" : "realmoneyflag3rdbef" , "type" : "double"},{ "name" : "dedicatedaccountunit4thbef" , "type" : "double"},{ "name" : "accountstartdate4thbef" , "type" : "string"},{ "name" : "refilldivunits4thbef" , "type" : "double"},{ "name" : "refillpromodivunits4thbef" , "type" : "double"},{ "name" : "unitbalance4thbef" , "type" : "double"},{ "name" : "clearedunits4thbef" , "type" : "double"},{ "name" : "realmoneyflag4thbef" , "type" : "double"},{ "name" : "dedicatedaccountunit5thbef" , "type" : "double"},{ "name" : "accountstartdate5thbef" , "type" : "string"},{ "name" : "refilldivunits5thbef" , "type" : "double"},{ "name" : "refillpromodivunits5thbef" , "type" : "double"},{ "name" : "unitbalance5thbef" , "type" : "double"},{ "name" : "clearedunits5thbef" , "type" : "double"},{ "name" : "realmoneyflag5thbef" , "type" : "double"},{ "name" : "dedicatedaccountunit6thbef" , "type" : "double"},{ "name" : "accountstartdate6thbef" , "type" : "string"},{ "name" : "refilldivunits6thbef" , "type" : "double"},{ "name" : "refillpromodivunits6thbef" , "type" : "double"},{ "name" : "unitbalance6thbef" , "type" : "double"},{ "name" : "clearedunits6thbef" , "type" : "double"},{ "name" : "realmoneyflag6thbef" , "type" : "double"},{ "name" : "dedicatedaccountunit7thbef" , "type" : "double"},{ "name" : "accountstartdate7thbef" , "type" : "string"},{ "name" : "refilldivunits7thbef" , "type" : "double"},{ "name" : "refillpromodivunits7thbef" , "type" : "double"},{ "name" : "unitbalance7thbef" , "type" : "double"},{ "name" : "clearedunits7thbef" , "type" : "double"},{ "name" : "realmoneyflag7thbef" , "type" : "double"},{ "name" : "dedicatedaccountunit8thbef" , "type" : "double"},{ "name" : "accountstartdate8thbef" , "type" : "string"},{ "name" : "refilldivunits8thbef" , "type" : "double"},{ "name" : "refillpromodivunits8thbef" , "type" : "double"},{ "name" : "unitbalance8thbef" , "type" : "double"},{ "name" : "clearedunits8thbef" , "type" : "double"},{ "name" : "realmoneyflag8thbef" , "type" : "double"},{ "name" : "dedicatedaccountunit9thbef" , "type" : "double"},{ "name" : "accountstartdate9thbef" , "type" : "string"},{ "name" : "refilldivunits9thbef" , "type" : "double"},{ "name" : "refillpromodivunits9thbef" , "type" : "double"},{ "name" : "unitbalance9thbef" , "type" : "double"},{ "name" : "clearedunits9thbef" , "type" : "double"},{ "name" : "realmoneyflag9thbef" , "type" : "double"},{ "name" : "dedicatedaccountunit10thbef" , "type" : "double"},{ "name" : "accountstartdate10thbef" , "type" : "string"},{ "name" : "refilldivunits10thbef" , "type" : "double"},{ "name" : "refillpromodivunits10thbef" , "type" : "double"},{ "name" : "unitbalance10thbef" , "type" : "double"},{ "name" : "clearedunits10thbef" , "type" : "double"},{ "name" : "realmoneyflag10thbef" , "type" : "double"},{ "name" : "offer1stidentifierbef" , "type" : "double"},{ "name" : "offerstartdate1stbef" , "type" : "string"},{ "name" : "offerexpirydate1stbef" , "type" : "string"},{ "name" : "offertype1stbef" , "type" : "string"},{ "name" : "offerproductidentifier1stbef" , "type" : "double"},{ "name" : "offerstartdatetime1stbef" , "type" : "string"},{ "name" : "offerexpirydatetime1stbef" , "type" : "string"},{ "name" : "offer2ndidentifierbef" , "type" : "double"},{ "name" : "offerstartdate2ndbef" , "type" : "string"},{ "name" : "offerexpirydate2ndbef" , "type" : "string"},{ "name" : "offertype2ndbef" , "type" : "string"},{ "name" : "offerproductidentifier2ndbef" , "type" : "double"},{ "name" : "offerstartdatetime2ndbef" , "type" : "string"},{ "name" : "offerexpirydatetime2ndbef" , "type" : "string"},{ "name" : "offer3rdidentifierbef" , "type" : "double"},{ "name" : "offerstartdate3rdbef" , "type" : "string"},{ "name" : "offerexpirydate3rdbef" , "type" : "string"},{ "name" : "offertype3rdbef" , "type" : "string"},{ "name" : "offerproductidentifier3rdbef" , "type" : "double"},{ "name" : "offerstartdatetime3rdbef" , "type" : "string"},{ "name" : "offerexpirydatetime3rdbef" , "type" : "string"},{ "name" : "offer4thidentifierbef" , "type" : "double"},{ "name" : "offerstartdate4thbef" , "type" : "string"},{ "name" : "offerexpirydate4thbef" , "type" : "string"},{ "name" : "offertype4thbef" , "type" : "string"},{ "name" : "offerproductidentifier4thbef" , "type" : "double"},{ "name" : "offerstartdatetime4thbef" , "type" : "string"},{ "name" : "offerexpirydatetime4thbef" , "type" : "string"},{ "name" : "offer5thidentifierbef" , "type" : "double"},{ "name" : "offerstartdate5thbef" , "type" : "string"},{ "name" : "offerexpirydate5thbef" , "type" : "string"},{ "name" : "offertype5thbef" , "type" : "string"},{ "name" : "offerproductidentifier5thbef" , "type" : "double"},{ "name" : "offerstartdatetime5thbef" , "type" : "string"},{ "name" : "offerexpirydatetime5thbef" , "type" : "string"},{ "name" : "offer6thidentifierbef" , "type" : "double"},{ "name" : "offerstartdate6thbef" , "type" : "string"},{ "name" : "offerexpirydate6thbef" , "type" : "string"},{ "name" : "offertype6thbef" , "type" : "string"},{ "name" : "offerproductidentifier6thbef" , "type" : "double"},{ "name" : "offerstartdatetime6thbef" , "type" : "string"},{ "name" : "offerexpirydatetime6thbef" , "type" : "string"},{ "name" : "offer7thidentifierbef" , "type" : "double"},{ "name" : "offerstartdate7thbef" , "type" : "string"},{ "name" : "offerexpirydate7thbef" , "type" : "string"},{ "name" : "offertype7thbef" , "type" : "string"},{ "name" : "offerproductidentifier7thbef" , "type" : "double"},{ "name" : "offerstartdatetime7thbef" , "type" : "string"},{ "name" : "offerexpirydatetime7thbef" , "type" : "string"},{ "name" : "offer8thidentifierbef" , "type" : "double"},{ "name" : "offerstartdate8thbef" , "type" : "string"},{ "name" : "offerexpirydate8thbef" , "type" : "string"},{ "name" : "offertype8thbef" , "type" : "string"},{ "name" : "offerproductidentifier8thbef" , "type" : "double"},{ "name" : "offerstartdatetime8thbef" , "type" : "string"},{ "name" : "offerexpirydatetime8thbef" , "type" : "string"},{ "name" : "offer9thidentifierbef" , "type" : "double"},{ "name" : "offerstartdate9thbef" , "type" : "string"},{ "name" : "offerexpirydate9thbef" , "type" : "string"},{ "name" : "offertype9thbef" , "type" : "string"},{ "name" : "offerproductidentifier9thbef" , "type" : "double"},{ "name" : "offerstartdatetime9thbef" , "type" : "string"},{ "name" : "offerexpirydatetime9thbef" , "type" : "string"},{ "name" : "offer10thidentifierbef" , "type" : "double"},{ "name" : "offerstartdate10thbef" , "type" : "string"},{ "name" : "offerexpirydate10thbef" , "type" : "string"},{ "name" : "offertype10thbef" , "type" : "string"},{ "name" : "offerproductidentifier10thbef" , "type" : "double"},{ "name" : "offerstartdatetime10thbef" , "type" : "string"},{ "name" : "offerexpirydatetime10thbef" , "type" : "string"},{ "name" : "aggregatedbalancebef" , "type" : "double"},{ "name" : "dedicatedaccountunit1staft" , "type" : "double"},{ "name" : "accountstartdate1staft" , "type" : "string"},{ "name" : "refilldivunits1staft" , "type" : "double"},{ "name" : "refillpromodivunits1staft" , "type" : "double"},{ "name" : "unitbalance1staft" , "type" : "double"},{ "name" : "clearedunits1staft" , "type" : "double"},{ "name" : "realmoneyflag1staft" , "type" : "double"},{ "name" : "dedicatedaccountunit2ndaft" , "type" : "double"},{ "name" : "accountstartdate2ndaft" , "type" : "string"},{ "name" : "refilldivunits2ndaft" , "type" : "double"},{ "name" : "refillpromodivunits2ndaft" , "type" : "double"},{ "name" : "unitbalance2ndaft" , "type" : "double"},{ "name" : "clearedunits2ndaft" , "type" : "double"},{ "name" : "realmoneyflag2ndaft" , "type" : "double"},{ "name" : "dedicatedaccountunit3rdaft" , "type" : "double"},{ "name" : "accountstartdate3rdaft" , "type" : "string"},{ "name" : "refilldivunits3rdaft" , "type" : "double"},{ "name" : "refillpromodivunits3rdaft" , "type" : "double"},{ "name" : "unitbalance3rdaft" , "type" : "double"},{ "name" : "clearedunits3rdaft" , "type" : "double"},{ "name" : "realmoneyflag3rdaft" , "type" : "double"},{ "name" : "dedicatedaccountunit4thaft" , "type" : "double"},{ "name" : "accountstartdate4thaft" , "type" : "string"},{ "name" : "refilldivunits4thaft" , "type" : "double"},{ "name" : "refillpromodivunits4thaft" , "type" : "double"},{ "name" : "unitbalance4thaft" , "type" : "double"},{ "name" : "clearedunits4thaft" , "type" : "double"},{ "name" : "realmoneyflag4thaft" , "type" : "double"},{ "name" : "dedicatedaccountunit5thaft" , "type" : "double"},{ "name" : "accountstartdate5thaft" , "type" : "string"},{ "name" : "refilldivunits5thaft" , "type" : "double"},{ "name" : "refillpromodivunits5thaft" , "type" : "double"},{ "name" : "unitbalance5thaft" , "type" : "double"},{ "name" : "clearedunits5thaft" , "type" : "double"},{ "name" : "realmoneyflag5thaft" , "type" : "double"},{ "name" : "dedicatedaccountunit6thaft" , "type" : "double"},{ "name" : "accountstartdate6thaft" , "type" : "string"},{ "name" : "refilldivunits6thaft" , "type" : "double"},{ "name" : "refillpromodivunits6thaft" , "type" : "double"},{ "name" : "unitbalance6thaft" , "type" : "double"},{ "name" : "clearedunits6thaft" , "type" : "double"},{ "name" : "realmoneyflag6thaft" , "type" : "double"},{ "name" : "dedicatedaccountunit7thaft" , "type" : "double"},{ "name" : "accountstartdate7thaft" , "type" : "string"},{ "name" : "refilldivunits7thaft" , "type" : "double"},{ "name" : "refillpromodivunits7thaft" , "type" : "double"},{ "name" : "unitbalance7thaft" , "type" : "double"},{ "name" : "clearedunits7thaft" , "type" : "double"},{ "name" : "realmoneyflag7thaft" , "type" : "double"},{ "name" : "dedicatedaccountunit8thaft" , "type" : "double"},{ "name" : "accountstartdate8thaft" , "type" : "string"},{ "name" : "refilldivunits8thaft" , "type" : "double"},{ "name" : "refillpromodivunits8thaft" , "type" : "double"},{ "name" : "unitbalance8thaft" , "type" : "double"},{ "name" : "clearedunits8thaft" , "type" : "double"},{ "name" : "realmoneyflag8thaft" , "type" : "double"},{ "name" : "dedicatedaccountunit9thaft" , "type" : "double"},{ "name" : "accountstartdate9thaft" , "type" : "string"},{ "name" : "refilldivunits9thaft" , "type" : "double"},{ "name" : "refillpromodivunits9thaft" , "type" : "double"},{ "name" : "unitbalance9thaft" , "type" : "double"},{ "name" : "clearedunits9thaft" , "type" : "double"},{ "name" : "realmoneyflag9thaft" , "type" : "double"},{ "name" : "dedicatedaccountunit10thaft" , "type" : "double"},{ "name" : "accountstartdate10thaft" , "type" : "string"},{ "name" : "refilldivunits10thaft" , "type" : "double"},{ "name" : "refillpromodivunits10thaft" , "type" : "double"},{ "name" : "unitbalance10thaft" , "type" : "double"},{ "name" : "clearedunits10thaft" , "type" : "double"},{ "name" : "realmoneyflag10thaft" , "type" : "double"},{ "name" : "offer1stidentifieraft" , "type" : "double"},{ "name" : "offerstartdate1staft" , "type" : "string"},{ "name" : "offerexpirydate1staft" , "type" : "string"},{ "name" : "offertype1staft" , "type" : "string"},{ "name" : "offerproductidentifier1staft" , "type" : "double"},{ "name" : "offerstartdatetime1staft" , "type" : "string"},{ "name" : "offerexpirydatetime1staft" , "type" : "string"},{ "name" : "offer2ndidentifieraft" , "type" : "double"},{ "name" : "offerstartdate2ndaft" , "type" : "string"},{ "name" : "offerexpirydate2ndaft" , "type" : "string"},{ "name" : "offertype2ndaft" , "type" : "string"},{ "name" : "offerproductidentifier2ndaft" , "type" : "double"},{ "name" : "offerstartdatetime2ndaft" , "type" : "string"},{ "name" : "offerexpirydatetime2ndaft" , "type" : "string"},{ "name" : "offer3rdidentifieraft" , "type" : "double"},{ "name" : "offerstartdate3rdaft" , "type" : "string"},{ "name" : "offerexpirydate3rdaft" , "type" : "string"},{ "name" : "offertype3rdaft" , "type" : "string"},{ "name" : "offerproductidentifier3rdaft" , "type" : "double"},{ "name" : "offerstartdatetime3rdaft" , "type" : "string"},{ "name" : "offerexpirydatetime3rdaft" , "type" : "string"},{ "name" : "offer4thidentifieraft" , "type" : "double"},{ "name" : "offerstartdate4thaft" , "type" : "string"},{ "name" : "offerexpirydate4thaft" , "type" : "string"},{ "name" : "offertype4thaft" , "type" : "string"},{ "name" : "offerproductidentifier4thaft" , "type" : "double"},{ "name" : "offerstartdatetime4thaft" , "type" : "string"},{ "name" : "offerexpirydatetime4thaft" , "type" : "string"},{ "name" : "offer5thidentifieraft" , "type" : "double"},{ "name" : "offerstartdate5thaft" , "type" : "string"},{ "name" : "offerexpirydate5thaft" , "type" : "string"},{ "name" : "offertype5thaft" , "type" : "string"},{ "name" : "offerproductidentifier5thaft" , "type" : "double"},{ "name" : "offerstartdatetime5thaft" , "type" : "string"},{ "name" : "offerexpirydatetime5thaft" , "type" : "string"},{ "name" : "offer6thidentifieraft" , "type" : "double"},{ "name" : "offerstartdate6thaft" , "type" : "string"},{ "name" : "offerexpirydate6thaft" , "type" : "string"},{ "name" : "offertype6thaft" , "type" : "string"},{ "name" : "offerproductidentifier6thaft" , "type" : "double"},{ "name" : "offerstartdatetime6thaft" , "type" : "string"},{ "name" : "offerexpirydatetime6thaft" , "type" : "string"},{ "name" : "offer7thidentifieraft" , "type" : "double"},{ "name" : "offerstartdate7thaft" , "type" : "string"},{ "name" : "offerexpirydate7thaft" , "type" : "string"},{ "name" : "offertype7thaft" , "type" : "string"},{ "name" : "offerproductidentifier7thaft" , "type" : "double"},{ "name" : "offerstartdatetime7thaft" , "type" : "string"},{ "name" : "offerexpirydatetime7thaft" , "type" : "string"},{ "name" : "offer8thidentifieraft" , "type" : "double"},{ "name" : "offerstartdate8thaft" , "type" : "string"},{ "name" : "offerexpirydate8thaft" , "type" : "string"},{ "name" : "offertype8thaft" , "type" : "string"},{ "name" : "offerproductidentifier8thaft" , "type" : "double"},{ "name" : "offerstartdatetime8thaft" , "type" : "string"},{ "name" : "offerexpirydatetime8thaft" , "type" : "string"},{ "name" : "offer9thidentifieraft" , "type" : "double"},{ "name" : "offerstartdate9thaft" , "type" : "string"},{ "name" : "offerexpirydate9thaft" , "type" : "string"},{ "name" : "offertype9thaft" , "type" : "string"},{ "name" : "offerproductidentifier9thaft" , "type" : "double"},{ "name" : "offerstartdatetime9thaft" , "type" : "string"},{ "name" : "offerexpirydatetime9thaft" , "type" : "string"},{ "name" : "offer10thidentifieraft" , "type" : "double"},{ "name" : "offerstartdate10thaft" , "type" : "string"},{ "name" : "offerexpirydate10thaft" , "type" : "string"},{ "name" : "offertype10thaft" , "type" : "string"},{ "name" : "offerproductidentifier10thaft" , "type" : "double"},{ "name" : "offerstartdatetime10thaft" , "type" : "string"},{ "name" : "offerexpirydatetime10thaft" , "type" : "string"},{ "name" : "aggregatedbalanceaft" , "type" : "double"},{ "name" : "cellidentifier" , "type" : "string"},{ "name" : "market_id" , "type" : "int"},{ "name" : "hub_id" , "type" : "int"},{ "name" : "filename" , "type" : "string"}]}""";  

    final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);
      
    override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
      try {
        if (oldVerobj == null) return null;
        oldVerobj match {
          
      case oldVerobj: com.ligadata.messages.V1000000.AirRefillCS5 => { return  convertToVer1000000(oldVerobj); } 
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
  
    private def convertToVer1000000(oldVerobj: com.ligadata.messages.V1000000.AirRefillCS5): com.ligadata.messages.V1000000.AirRefillCS5= {
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
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj AirRefillCS5") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj AirRefillCS5");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj AirRefillCS5");
 override def NeedToTransformData: Boolean = false
    }

class AirRefillCS5(factory: MessageFactoryInterface, other: AirRefillCS5) extends MessageInterface(factory) { 
 
  val log = AirRefillCS5.log

      var attributeTypes = generateAttributeTypes;
      
    private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
      var attributeTypes = new Array[AttributeTypeInfo](578);
   		 attributeTypes(0) = new AttributeTypeInfo("originnodetype", 0, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(1) = new AttributeTypeInfo("originhostname", 1, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(2) = new AttributeTypeInfo("originfileid", 2, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(3) = new AttributeTypeInfo("origintransactionid", 3, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(4) = new AttributeTypeInfo("originoperatorid", 4, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(5) = new AttributeTypeInfo("origintimestamp", 5, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(6) = new AttributeTypeInfo("hostname", 6, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(7) = new AttributeTypeInfo("localsequencenumber", 7, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(8) = new AttributeTypeInfo("timestamp", 8, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(9) = new AttributeTypeInfo("currentserviceclass", 9, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(10) = new AttributeTypeInfo("voucherbasedrefill", 10, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(11) = new AttributeTypeInfo("transactiontype", 11, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(12) = new AttributeTypeInfo("transactioncode", 12, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(13) = new AttributeTypeInfo("transactionamount", 13, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(14) = new AttributeTypeInfo("transactioncurrency", 14, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(15) = new AttributeTypeInfo("refillamountconverted", 15, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(16) = new AttributeTypeInfo("refilldivisionamount", 16, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(17) = new AttributeTypeInfo("refilltype", 17, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(18) = new AttributeTypeInfo("refillprofileid", 18, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(19) = new AttributeTypeInfo("segmentationid", 19, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(20) = new AttributeTypeInfo("voucherserialnumber", 20, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(21) = new AttributeTypeInfo("vouchergroupid", 21, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(22) = new AttributeTypeInfo("accountnumber", 22, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(23) = new AttributeTypeInfo("accountcurrency", 23, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(24) = new AttributeTypeInfo("subscribernumber", 24, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(25) = new AttributeTypeInfo("promotionannouncementcode", 25, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(26) = new AttributeTypeInfo("accountflagsbef", 26, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(27) = new AttributeTypeInfo("accountbalancebef", 27, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(28) = new AttributeTypeInfo("accumulatedrefillvaluebef", 28, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(29) = new AttributeTypeInfo("accumulatedrefillcounterbef", 29, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(30) = new AttributeTypeInfo("accumulatedprogressionvaluebef", 30, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(31) = new AttributeTypeInfo("accumulatedprogrcounterbef", 31, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(32) = new AttributeTypeInfo("creditclearanceperiodbef", 32, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(33) = new AttributeTypeInfo("dedicatedaccount1stidbef", 33, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(34) = new AttributeTypeInfo("account1stcampaignidentbef", 34, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(35) = new AttributeTypeInfo("account1strefilldivamountbef", 35, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(36) = new AttributeTypeInfo("account1strefillpromdivamntbef", 36, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(37) = new AttributeTypeInfo("account1stbalancebef", 37, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(38) = new AttributeTypeInfo("clearedaccount1stvaluebef", 38, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(39) = new AttributeTypeInfo("dedicatedaccount2ndidbef", 39, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(40) = new AttributeTypeInfo("account2ndcampaignidentifbef", 40, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(41) = new AttributeTypeInfo("account2ndrefilldivamountbef", 41, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(42) = new AttributeTypeInfo("account2ndrefilpromodivamntbef", 42, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(43) = new AttributeTypeInfo("account2ndbalancebef", 43, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(44) = new AttributeTypeInfo("clearedaccount2ndvaluebef", 44, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(45) = new AttributeTypeInfo("dedicatedaccount3rdidbef", 45, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(46) = new AttributeTypeInfo("account3rdcampaignidentbef", 46, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(47) = new AttributeTypeInfo("account3rdrefilldivamountbef", 47, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(48) = new AttributeTypeInfo("account3rdrefilpromodivamntbef", 48, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(49) = new AttributeTypeInfo("account3rdbalancebef", 49, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(50) = new AttributeTypeInfo("clearedaccount3rdvaluebef", 50, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(51) = new AttributeTypeInfo("dedicatedaccount4thidbef", 51, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(52) = new AttributeTypeInfo("account4thcampaignidentbef", 52, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(53) = new AttributeTypeInfo("account4threfilldivamountbef", 53, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(54) = new AttributeTypeInfo("account4threfilpromodivamntbef", 54, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(55) = new AttributeTypeInfo("account4thbalancebef", 55, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(56) = new AttributeTypeInfo("clearedaccount4thvaluebef", 56, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(57) = new AttributeTypeInfo("dedicatedaccount5thidbef", 57, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(58) = new AttributeTypeInfo("account5thcampaignidentbef", 58, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(59) = new AttributeTypeInfo("account5threfilldivamountbef", 59, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(60) = new AttributeTypeInfo("account5threfilpromodivamntbef", 60, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(61) = new AttributeTypeInfo("account5thbalancebef", 61, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(62) = new AttributeTypeInfo("clearedaccount5thvaluebef", 62, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(63) = new AttributeTypeInfo("dedicatedaccount6thidbef", 63, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(64) = new AttributeTypeInfo("account6thcampaignidentbef", 64, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(65) = new AttributeTypeInfo("account6threfilldivamountbef", 65, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(66) = new AttributeTypeInfo("account6threfilpromodivamntbef", 66, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(67) = new AttributeTypeInfo("account6thbalancebef", 67, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(68) = new AttributeTypeInfo("clearedaccount6thvaluebef", 68, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(69) = new AttributeTypeInfo("dedicatedaccount7thidbef", 69, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(70) = new AttributeTypeInfo("account7thcampaignidentbef", 70, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(71) = new AttributeTypeInfo("account7threfilldivamountbef", 71, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(72) = new AttributeTypeInfo("account7threfilpromodivamntbef", 72, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(73) = new AttributeTypeInfo("account7thbalancebef", 73, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(74) = new AttributeTypeInfo("clearedaccount7thvaluebef", 74, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(75) = new AttributeTypeInfo("dedicatedaccount8thidbef", 75, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(76) = new AttributeTypeInfo("account8thcampaignidentbef", 76, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(77) = new AttributeTypeInfo("account8threfilldivamountbef", 77, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(78) = new AttributeTypeInfo("account8threfilpromodivamntbef", 78, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(79) = new AttributeTypeInfo("account8thbalancebef", 79, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(80) = new AttributeTypeInfo("clearedaccount8thvaluebef", 80, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(81) = new AttributeTypeInfo("dedicatedaccount9thidbef", 81, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(82) = new AttributeTypeInfo("account9thcampaignidentbef", 82, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(83) = new AttributeTypeInfo("account9threfilldivamountbef", 83, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(84) = new AttributeTypeInfo("account9threfilpromodivamntbef", 84, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(85) = new AttributeTypeInfo("account9thbalancebef", 85, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(86) = new AttributeTypeInfo("clearedaccount9thvaluebef", 86, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(87) = new AttributeTypeInfo("dedicatedaccount10thidbef", 87, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(88) = new AttributeTypeInfo("account10thcampaignidentbef", 88, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(89) = new AttributeTypeInfo("account10threfilldivamountbef", 89, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(90) = new AttributeTypeInfo("account10threfilpromdivamntbef", 90, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(91) = new AttributeTypeInfo("account10thbalancebef", 91, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(92) = new AttributeTypeInfo("clearedaccount10thvaluebef", 92, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(93) = new AttributeTypeInfo("promotionplan", 93, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(94) = new AttributeTypeInfo("permanentserviceclassbef", 94, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(95) = new AttributeTypeInfo("temporaryserviceclassbef", 95, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(96) = new AttributeTypeInfo("temporaryservclassexpdatebef", 96, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(97) = new AttributeTypeInfo("refilloptionbef", 97, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(98) = new AttributeTypeInfo("servicefeeexpirydatebef", 98, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(99) = new AttributeTypeInfo("serviceremovalgraceperiodbef", 99, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(100) = new AttributeTypeInfo("serviceofferingbef", 100, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(101) = new AttributeTypeInfo("supervisionexpirydatebef", 101, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(102) = new AttributeTypeInfo("usageaccumulator1stidbef", 102, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(103) = new AttributeTypeInfo("usageaccumulator1stvaluebef", 103, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(104) = new AttributeTypeInfo("usageaccumulator2ndidbef", 104, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(105) = new AttributeTypeInfo("usageaccumulator2ndvaluebef", 105, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(106) = new AttributeTypeInfo("usageaccumulator3rdidbef", 106, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(107) = new AttributeTypeInfo("usageaccumulator3rdvaluebef", 107, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(108) = new AttributeTypeInfo("usageaccumulator4thidbef", 108, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(109) = new AttributeTypeInfo("usageaccumulator4thvaluebef", 109, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(110) = new AttributeTypeInfo("usageaccumulator5thidbef", 110, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(111) = new AttributeTypeInfo("usageaccumulator5thvaluebef", 111, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(112) = new AttributeTypeInfo("usageaccumulator6thidbef", 112, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(113) = new AttributeTypeInfo("usageaccumulator6thvaluebef", 113, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(114) = new AttributeTypeInfo("usageaccumulator7thidbef", 114, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(115) = new AttributeTypeInfo("usageaccumulator7thvaluebef", 115, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(116) = new AttributeTypeInfo("usageaccumulator8thidbef", 116, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(117) = new AttributeTypeInfo("usageaccumulator8thvaluebef", 117, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(118) = new AttributeTypeInfo("usageaccumulator9thidbef", 118, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(119) = new AttributeTypeInfo("usageaccumulator9thvaluebef", 119, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(120) = new AttributeTypeInfo("usageaccumulator10thidbef", 120, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(121) = new AttributeTypeInfo("usageaccumulator10thvaluebef", 121, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(122) = new AttributeTypeInfo("communityidbef1", 122, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(123) = new AttributeTypeInfo("communityidbef2", 123, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(124) = new AttributeTypeInfo("communityidbef3", 124, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(125) = new AttributeTypeInfo("accountflagsaft", 125, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(126) = new AttributeTypeInfo("accountbalanceaft", 126, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(127) = new AttributeTypeInfo("accumulatedrefillvalueaft", 127, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(128) = new AttributeTypeInfo("accumulatedrefillcounteraft", 128, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(129) = new AttributeTypeInfo("accumulatedprogressionvalueaft", 129, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(130) = new AttributeTypeInfo("accumulatedprogconteraft", 130, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(131) = new AttributeTypeInfo("creditclearanceperiodaft", 131, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(132) = new AttributeTypeInfo("dedicatedaccount1stidaft", 132, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(133) = new AttributeTypeInfo("account1stcampaignidentaft", 133, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(134) = new AttributeTypeInfo("account1strefilldivamountaft", 134, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(135) = new AttributeTypeInfo("account1strefilpromodivamntaft", 135, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(136) = new AttributeTypeInfo("account1stbalanceaft", 136, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(137) = new AttributeTypeInfo("clearedaccount1stvalueaft", 137, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(138) = new AttributeTypeInfo("dedicatedaccount2ndidaft", 138, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(139) = new AttributeTypeInfo("account2ndcampaignidentaft", 139, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(140) = new AttributeTypeInfo("account2ndrefilldivamountaft", 140, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(141) = new AttributeTypeInfo("account2ndrefilpromodivamntaft", 141, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(142) = new AttributeTypeInfo("account2ndbalanceaft", 142, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(143) = new AttributeTypeInfo("clearedaccount2ndvalueaft", 143, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(144) = new AttributeTypeInfo("dedicatedaccount3rdidaft", 144, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(145) = new AttributeTypeInfo("account3rdcampaignidentaft", 145, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(146) = new AttributeTypeInfo("account3rdrefilldivamountaft", 146, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(147) = new AttributeTypeInfo("account3rdrefilpromodivamntaft", 147, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(148) = new AttributeTypeInfo("account3rdbalanceaft", 148, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(149) = new AttributeTypeInfo("clearedaccount3rdvalueaft", 149, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(150) = new AttributeTypeInfo("dedicatedaccount4thidaft", 150, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(151) = new AttributeTypeInfo("account4thcampaignidentaft", 151, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(152) = new AttributeTypeInfo("account4threfilldivamountaft", 152, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(153) = new AttributeTypeInfo("account4threfilpromodivamntaft", 153, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(154) = new AttributeTypeInfo("account4thbalanceaft", 154, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(155) = new AttributeTypeInfo("clearedaccount4thvalueaft", 155, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(156) = new AttributeTypeInfo("dedicatedaccount5thidaft", 156, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(157) = new AttributeTypeInfo("account5thcampaignidentaft", 157, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(158) = new AttributeTypeInfo("account5threfilldivamountaft", 158, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(159) = new AttributeTypeInfo("account5threfilpromodivamntaft", 159, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(160) = new AttributeTypeInfo("account5thbalanceaft", 160, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(161) = new AttributeTypeInfo("clearedaccount5thvalueaft", 161, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(162) = new AttributeTypeInfo("dedicatedaccount6thidaft", 162, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(163) = new AttributeTypeInfo("account6thcampaignidentaft", 163, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(164) = new AttributeTypeInfo("account6threfilldivamountaft", 164, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(165) = new AttributeTypeInfo("account6threfilpromodivamntaft", 165, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(166) = new AttributeTypeInfo("account6thbalanceaft", 166, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(167) = new AttributeTypeInfo("clearedaccount6thvalueaft", 167, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(168) = new AttributeTypeInfo("dedicatedaccount7thidaft", 168, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(169) = new AttributeTypeInfo("account7thcampaignidentaft", 169, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(170) = new AttributeTypeInfo("account7threfilldivamountaft", 170, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(171) = new AttributeTypeInfo("account7threfilpromodivamntaft", 171, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(172) = new AttributeTypeInfo("account7thbalanceaft", 172, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(173) = new AttributeTypeInfo("clearedaccount7thvalueaft", 173, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(174) = new AttributeTypeInfo("dedicatedaccount8thidaft", 174, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(175) = new AttributeTypeInfo("account8thcampaignidentaft", 175, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(176) = new AttributeTypeInfo("account8threfilldivamountaft", 176, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(177) = new AttributeTypeInfo("account8threfilpromodivamntaft", 177, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(178) = new AttributeTypeInfo("account8thbalanceaft", 178, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(179) = new AttributeTypeInfo("clearedaccount8thvalueaft", 179, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(180) = new AttributeTypeInfo("dedicatedaccount9thidaft", 180, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(181) = new AttributeTypeInfo("account9thcampaignidentaft", 181, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(182) = new AttributeTypeInfo("account9threfilldivamountaft", 182, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(183) = new AttributeTypeInfo("account9threfilpromodivamntaft", 183, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(184) = new AttributeTypeInfo("account9thbalanceaft", 184, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(185) = new AttributeTypeInfo("clearedaccount9thvalueaft", 185, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(186) = new AttributeTypeInfo("dedicatedaccount10thidaft", 186, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(187) = new AttributeTypeInfo("account10thcampaignidentaft", 187, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(188) = new AttributeTypeInfo("account10threfilldivamountaft", 188, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(189) = new AttributeTypeInfo("account10threfilpromdivamntaft", 189, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(190) = new AttributeTypeInfo("account10thbalanceaft", 190, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(191) = new AttributeTypeInfo("clearedaccount10thvalueaft", 191, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(192) = new AttributeTypeInfo("promotionplanaft", 192, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(193) = new AttributeTypeInfo("permanentserviceclassaft", 193, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(194) = new AttributeTypeInfo("temporaryserviceclassaft", 194, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(195) = new AttributeTypeInfo("temporaryservclasexpirydateaft", 195, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(196) = new AttributeTypeInfo("refilloptionaft", 196, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(197) = new AttributeTypeInfo("servicefeeexpirydateaft", 197, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(198) = new AttributeTypeInfo("serviceremovalgraceperiodaft", 198, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(199) = new AttributeTypeInfo("serviceofferingaft", 199, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(200) = new AttributeTypeInfo("supervisionexpirydateaft", 200, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(201) = new AttributeTypeInfo("usageaccumulator1stidaft", 201, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(202) = new AttributeTypeInfo("usageaccumulator1stvalueaft", 202, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(203) = new AttributeTypeInfo("usageaccumulator2ndidaft", 203, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(204) = new AttributeTypeInfo("usageaccumulator2ndvalueaft", 204, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(205) = new AttributeTypeInfo("usageaccumulator3rdidaft", 205, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(206) = new AttributeTypeInfo("usageaccumulator3rdvalueaft", 206, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(207) = new AttributeTypeInfo("usageaccumulator4thidaft", 207, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(208) = new AttributeTypeInfo("usageaccumulator4thvalueaft", 208, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(209) = new AttributeTypeInfo("usageaccumulator5thidaft", 209, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(210) = new AttributeTypeInfo("usageaccumulator5thvalueaft", 210, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(211) = new AttributeTypeInfo("usageaccumulator6thidaft", 211, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(212) = new AttributeTypeInfo("usageaccumulator6thvalueaft", 212, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(213) = new AttributeTypeInfo("usageaccumulator7thidaft", 213, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(214) = new AttributeTypeInfo("usageaccumulator7thvalueaft", 214, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(215) = new AttributeTypeInfo("usageaccumulator8thidaft", 215, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(216) = new AttributeTypeInfo("usageaccumulator8thvalueaft", 216, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(217) = new AttributeTypeInfo("usageaccumulator9thidaft", 217, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(218) = new AttributeTypeInfo("usageaccumulator9thvalueaft", 218, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(219) = new AttributeTypeInfo("usageaccumulator10thidaft", 219, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(220) = new AttributeTypeInfo("usageaccumulator10thvalueaft", 220, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(221) = new AttributeTypeInfo("communityidaft1", 221, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(222) = new AttributeTypeInfo("communityidaft2", 222, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(223) = new AttributeTypeInfo("communityidaft3", 223, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(224) = new AttributeTypeInfo("refillpromodivisionamount", 224, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(225) = new AttributeTypeInfo("supervisiondayspromopart", 225, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(226) = new AttributeTypeInfo("supervisiondayssurplus", 226, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(227) = new AttributeTypeInfo("servicefeedayspromopart", 227, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(228) = new AttributeTypeInfo("servicefeedayssurplus", 228, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(229) = new AttributeTypeInfo("maximumservicefeeperiod", 229, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(230) = new AttributeTypeInfo("maximumsupervisionperiod", 230, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(231) = new AttributeTypeInfo("activationdate", 231, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(232) = new AttributeTypeInfo("welcomestatus", 232, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(233) = new AttributeTypeInfo("voucheragent", 233, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(234) = new AttributeTypeInfo("promotionplanallocstartdate", 234, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(235) = new AttributeTypeInfo("accountgroupid", 235, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(236) = new AttributeTypeInfo("externaldata1", 236, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(237) = new AttributeTypeInfo("externaldata2", 237, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(238) = new AttributeTypeInfo("externaldata3", 238, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(239) = new AttributeTypeInfo("externaldata4", 239, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(240) = new AttributeTypeInfo("locationnumber", 240, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(241) = new AttributeTypeInfo("voucheractivationcode", 241, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(242) = new AttributeTypeInfo("accountcurrencycleared", 242, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(243) = new AttributeTypeInfo("ignoreserviceclasshierarchy", 243, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(244) = new AttributeTypeInfo("accounthomeregion", 244, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(245) = new AttributeTypeInfo("subscriberregion", 245, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(246) = new AttributeTypeInfo("voucherregion", 246, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(247) = new AttributeTypeInfo("promotionplanallocenddate", 247, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(248) = new AttributeTypeInfo("requestedrefilltype", 248, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(249) = new AttributeTypeInfo("accountexpiry1stdatebef", 249, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(250) = new AttributeTypeInfo("accountexpiry1stdateaft", 250, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(251) = new AttributeTypeInfo("accountexpiry2nddatebef", 251, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(252) = new AttributeTypeInfo("accountexpiry2nddateaft", 252, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(253) = new AttributeTypeInfo("accountexpiry3rddatebef", 253, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(254) = new AttributeTypeInfo("accountexpiry3rddateaft", 254, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(255) = new AttributeTypeInfo("accountexpiry4thdatebef", 255, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(256) = new AttributeTypeInfo("accountexpiry4thdateaft", 256, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(257) = new AttributeTypeInfo("accountexpiry5thdatebef", 257, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(258) = new AttributeTypeInfo("accountexpiry5thdateaft", 258, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(259) = new AttributeTypeInfo("accountexpiry6thdatebef", 259, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(260) = new AttributeTypeInfo("accountexpiry6thdateaft", 260, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(261) = new AttributeTypeInfo("accountexpiry7thdatebef", 261, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(262) = new AttributeTypeInfo("accountexpiry7thdateaft", 262, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(263) = new AttributeTypeInfo("accountexpiry8thdatebef", 263, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(264) = new AttributeTypeInfo("accountexpiry8thdateaft", 264, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(265) = new AttributeTypeInfo("accountexpiry9thdatebef", 265, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(266) = new AttributeTypeInfo("accountexpiry9thdateaft", 266, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(267) = new AttributeTypeInfo("accountexpiry10thdatebef", 267, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(268) = new AttributeTypeInfo("accountexpiry10thdateaft", 268, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(269) = new AttributeTypeInfo("rechargedivpartmain", 269, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(270) = new AttributeTypeInfo("rechargedivpartda1st", 270, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(271) = new AttributeTypeInfo("rechargedivpartda2nd", 271, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(272) = new AttributeTypeInfo("rechargedivpartda3rd", 272, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(273) = new AttributeTypeInfo("rechargedivpartda4th", 273, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(274) = new AttributeTypeInfo("rechargedivpartda5th", 274, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(275) = new AttributeTypeInfo("accumulatedprogressionvalueres", 275, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(276) = new AttributeTypeInfo("rechargedivpartpmain", 276, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(277) = new AttributeTypeInfo("rechargedivpartpda1st", 277, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(278) = new AttributeTypeInfo("rechargedivpartpda2nd", 278, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(279) = new AttributeTypeInfo("rechargedivpartpda3rd", 279, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(280) = new AttributeTypeInfo("rechargedivpartpda4th", 280, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(281) = new AttributeTypeInfo("rechargedivpartpda5th", 281, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(282) = new AttributeTypeInfo("rechargedivpartda6th", 282, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(283) = new AttributeTypeInfo("rechargedivpartda7th", 283, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(284) = new AttributeTypeInfo("rechargedivpartda8th", 284, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(285) = new AttributeTypeInfo("rechargedivpartda9th", 285, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(286) = new AttributeTypeInfo("rechargedivpartda10th", 286, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(287) = new AttributeTypeInfo("rechargedivpartpda6th", 287, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(288) = new AttributeTypeInfo("rechargedivpartpda7th", 288, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(289) = new AttributeTypeInfo("rechargedivpartpda8th", 289, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(290) = new AttributeTypeInfo("rechargedivpartpda9th", 290, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(291) = new AttributeTypeInfo("rechargedivpartpda10th", 291, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(292) = new AttributeTypeInfo("dedicatedaccountunit1stbef", 292, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(293) = new AttributeTypeInfo("accountstartdate1stbef", 293, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(294) = new AttributeTypeInfo("refilldivunits1stbef", 294, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(295) = new AttributeTypeInfo("refillpromodivunits1stbef", 295, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(296) = new AttributeTypeInfo("unitbalance1stbef", 296, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(297) = new AttributeTypeInfo("clearedunits1stbef", 297, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(298) = new AttributeTypeInfo("realmoneyflag1stbef", 298, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(299) = new AttributeTypeInfo("dedicatedaccountunit2ndbef", 299, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(300) = new AttributeTypeInfo("accountstartdate2ndbef", 300, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(301) = new AttributeTypeInfo("refilldivunits2ndbef", 301, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(302) = new AttributeTypeInfo("refillpromodivunits2ndbef", 302, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(303) = new AttributeTypeInfo("unitbalance2ndbef", 303, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(304) = new AttributeTypeInfo("clearedunits2ndbef", 304, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(305) = new AttributeTypeInfo("realmoneyflag2ndbef", 305, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(306) = new AttributeTypeInfo("dedicatedaccountunit3rdbef", 306, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(307) = new AttributeTypeInfo("accountstartdate3rdbef", 307, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(308) = new AttributeTypeInfo("refilldivunits3rdbef", 308, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(309) = new AttributeTypeInfo("refillpromodivunits3rdbef", 309, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(310) = new AttributeTypeInfo("unitbalance3rdbef", 310, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(311) = new AttributeTypeInfo("clearedunits3rdbef", 311, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(312) = new AttributeTypeInfo("realmoneyflag3rdbef", 312, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(313) = new AttributeTypeInfo("dedicatedaccountunit4thbef", 313, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(314) = new AttributeTypeInfo("accountstartdate4thbef", 314, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(315) = new AttributeTypeInfo("refilldivunits4thbef", 315, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(316) = new AttributeTypeInfo("refillpromodivunits4thbef", 316, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(317) = new AttributeTypeInfo("unitbalance4thbef", 317, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(318) = new AttributeTypeInfo("clearedunits4thbef", 318, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(319) = new AttributeTypeInfo("realmoneyflag4thbef", 319, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(320) = new AttributeTypeInfo("dedicatedaccountunit5thbef", 320, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(321) = new AttributeTypeInfo("accountstartdate5thbef", 321, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(322) = new AttributeTypeInfo("refilldivunits5thbef", 322, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(323) = new AttributeTypeInfo("refillpromodivunits5thbef", 323, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(324) = new AttributeTypeInfo("unitbalance5thbef", 324, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(325) = new AttributeTypeInfo("clearedunits5thbef", 325, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(326) = new AttributeTypeInfo("realmoneyflag5thbef", 326, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(327) = new AttributeTypeInfo("dedicatedaccountunit6thbef", 327, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(328) = new AttributeTypeInfo("accountstartdate6thbef", 328, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(329) = new AttributeTypeInfo("refilldivunits6thbef", 329, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(330) = new AttributeTypeInfo("refillpromodivunits6thbef", 330, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(331) = new AttributeTypeInfo("unitbalance6thbef", 331, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(332) = new AttributeTypeInfo("clearedunits6thbef", 332, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(333) = new AttributeTypeInfo("realmoneyflag6thbef", 333, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(334) = new AttributeTypeInfo("dedicatedaccountunit7thbef", 334, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(335) = new AttributeTypeInfo("accountstartdate7thbef", 335, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(336) = new AttributeTypeInfo("refilldivunits7thbef", 336, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(337) = new AttributeTypeInfo("refillpromodivunits7thbef", 337, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(338) = new AttributeTypeInfo("unitbalance7thbef", 338, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(339) = new AttributeTypeInfo("clearedunits7thbef", 339, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(340) = new AttributeTypeInfo("realmoneyflag7thbef", 340, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(341) = new AttributeTypeInfo("dedicatedaccountunit8thbef", 341, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(342) = new AttributeTypeInfo("accountstartdate8thbef", 342, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(343) = new AttributeTypeInfo("refilldivunits8thbef", 343, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(344) = new AttributeTypeInfo("refillpromodivunits8thbef", 344, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(345) = new AttributeTypeInfo("unitbalance8thbef", 345, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(346) = new AttributeTypeInfo("clearedunits8thbef", 346, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(347) = new AttributeTypeInfo("realmoneyflag8thbef", 347, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(348) = new AttributeTypeInfo("dedicatedaccountunit9thbef", 348, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(349) = new AttributeTypeInfo("accountstartdate9thbef", 349, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(350) = new AttributeTypeInfo("refilldivunits9thbef", 350, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(351) = new AttributeTypeInfo("refillpromodivunits9thbef", 351, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(352) = new AttributeTypeInfo("unitbalance9thbef", 352, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(353) = new AttributeTypeInfo("clearedunits9thbef", 353, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(354) = new AttributeTypeInfo("realmoneyflag9thbef", 354, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(355) = new AttributeTypeInfo("dedicatedaccountunit10thbef", 355, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(356) = new AttributeTypeInfo("accountstartdate10thbef", 356, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(357) = new AttributeTypeInfo("refilldivunits10thbef", 357, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(358) = new AttributeTypeInfo("refillpromodivunits10thbef", 358, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(359) = new AttributeTypeInfo("unitbalance10thbef", 359, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(360) = new AttributeTypeInfo("clearedunits10thbef", 360, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(361) = new AttributeTypeInfo("realmoneyflag10thbef", 361, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(362) = new AttributeTypeInfo("offer1stidentifierbef", 362, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(363) = new AttributeTypeInfo("offerstartdate1stbef", 363, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(364) = new AttributeTypeInfo("offerexpirydate1stbef", 364, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(365) = new AttributeTypeInfo("offertype1stbef", 365, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(366) = new AttributeTypeInfo("offerproductidentifier1stbef", 366, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(367) = new AttributeTypeInfo("offerstartdatetime1stbef", 367, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(368) = new AttributeTypeInfo("offerexpirydatetime1stbef", 368, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(369) = new AttributeTypeInfo("offer2ndidentifierbef", 369, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(370) = new AttributeTypeInfo("offerstartdate2ndbef", 370, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(371) = new AttributeTypeInfo("offerexpirydate2ndbef", 371, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(372) = new AttributeTypeInfo("offertype2ndbef", 372, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(373) = new AttributeTypeInfo("offerproductidentifier2ndbef", 373, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(374) = new AttributeTypeInfo("offerstartdatetime2ndbef", 374, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(375) = new AttributeTypeInfo("offerexpirydatetime2ndbef", 375, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(376) = new AttributeTypeInfo("offer3rdidentifierbef", 376, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(377) = new AttributeTypeInfo("offerstartdate3rdbef", 377, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(378) = new AttributeTypeInfo("offerexpirydate3rdbef", 378, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(379) = new AttributeTypeInfo("offertype3rdbef", 379, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(380) = new AttributeTypeInfo("offerproductidentifier3rdbef", 380, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(381) = new AttributeTypeInfo("offerstartdatetime3rdbef", 381, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(382) = new AttributeTypeInfo("offerexpirydatetime3rdbef", 382, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(383) = new AttributeTypeInfo("offer4thidentifierbef", 383, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(384) = new AttributeTypeInfo("offerstartdate4thbef", 384, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(385) = new AttributeTypeInfo("offerexpirydate4thbef", 385, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(386) = new AttributeTypeInfo("offertype4thbef", 386, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(387) = new AttributeTypeInfo("offerproductidentifier4thbef", 387, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(388) = new AttributeTypeInfo("offerstartdatetime4thbef", 388, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(389) = new AttributeTypeInfo("offerexpirydatetime4thbef", 389, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(390) = new AttributeTypeInfo("offer5thidentifierbef", 390, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(391) = new AttributeTypeInfo("offerstartdate5thbef", 391, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(392) = new AttributeTypeInfo("offerexpirydate5thbef", 392, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(393) = new AttributeTypeInfo("offertype5thbef", 393, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(394) = new AttributeTypeInfo("offerproductidentifier5thbef", 394, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(395) = new AttributeTypeInfo("offerstartdatetime5thbef", 395, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(396) = new AttributeTypeInfo("offerexpirydatetime5thbef", 396, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(397) = new AttributeTypeInfo("offer6thidentifierbef", 397, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(398) = new AttributeTypeInfo("offerstartdate6thbef", 398, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(399) = new AttributeTypeInfo("offerexpirydate6thbef", 399, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(400) = new AttributeTypeInfo("offertype6thbef", 400, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(401) = new AttributeTypeInfo("offerproductidentifier6thbef", 401, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(402) = new AttributeTypeInfo("offerstartdatetime6thbef", 402, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(403) = new AttributeTypeInfo("offerexpirydatetime6thbef", 403, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(404) = new AttributeTypeInfo("offer7thidentifierbef", 404, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(405) = new AttributeTypeInfo("offerstartdate7thbef", 405, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(406) = new AttributeTypeInfo("offerexpirydate7thbef", 406, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(407) = new AttributeTypeInfo("offertype7thbef", 407, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(408) = new AttributeTypeInfo("offerproductidentifier7thbef", 408, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(409) = new AttributeTypeInfo("offerstartdatetime7thbef", 409, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(410) = new AttributeTypeInfo("offerexpirydatetime7thbef", 410, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(411) = new AttributeTypeInfo("offer8thidentifierbef", 411, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(412) = new AttributeTypeInfo("offerstartdate8thbef", 412, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(413) = new AttributeTypeInfo("offerexpirydate8thbef", 413, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(414) = new AttributeTypeInfo("offertype8thbef", 414, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(415) = new AttributeTypeInfo("offerproductidentifier8thbef", 415, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(416) = new AttributeTypeInfo("offerstartdatetime8thbef", 416, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(417) = new AttributeTypeInfo("offerexpirydatetime8thbef", 417, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(418) = new AttributeTypeInfo("offer9thidentifierbef", 418, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(419) = new AttributeTypeInfo("offerstartdate9thbef", 419, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(420) = new AttributeTypeInfo("offerexpirydate9thbef", 420, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(421) = new AttributeTypeInfo("offertype9thbef", 421, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(422) = new AttributeTypeInfo("offerproductidentifier9thbef", 422, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(423) = new AttributeTypeInfo("offerstartdatetime9thbef", 423, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(424) = new AttributeTypeInfo("offerexpirydatetime9thbef", 424, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(425) = new AttributeTypeInfo("offer10thidentifierbef", 425, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(426) = new AttributeTypeInfo("offerstartdate10thbef", 426, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(427) = new AttributeTypeInfo("offerexpirydate10thbef", 427, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(428) = new AttributeTypeInfo("offertype10thbef", 428, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(429) = new AttributeTypeInfo("offerproductidentifier10thbef", 429, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(430) = new AttributeTypeInfo("offerstartdatetime10thbef", 430, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(431) = new AttributeTypeInfo("offerexpirydatetime10thbef", 431, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(432) = new AttributeTypeInfo("aggregatedbalancebef", 432, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(433) = new AttributeTypeInfo("dedicatedaccountunit1staft", 433, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(434) = new AttributeTypeInfo("accountstartdate1staft", 434, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(435) = new AttributeTypeInfo("refilldivunits1staft", 435, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(436) = new AttributeTypeInfo("refillpromodivunits1staft", 436, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(437) = new AttributeTypeInfo("unitbalance1staft", 437, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(438) = new AttributeTypeInfo("clearedunits1staft", 438, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(439) = new AttributeTypeInfo("realmoneyflag1staft", 439, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(440) = new AttributeTypeInfo("dedicatedaccountunit2ndaft", 440, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(441) = new AttributeTypeInfo("accountstartdate2ndaft", 441, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(442) = new AttributeTypeInfo("refilldivunits2ndaft", 442, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(443) = new AttributeTypeInfo("refillpromodivunits2ndaft", 443, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(444) = new AttributeTypeInfo("unitbalance2ndaft", 444, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(445) = new AttributeTypeInfo("clearedunits2ndaft", 445, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(446) = new AttributeTypeInfo("realmoneyflag2ndaft", 446, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(447) = new AttributeTypeInfo("dedicatedaccountunit3rdaft", 447, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(448) = new AttributeTypeInfo("accountstartdate3rdaft", 448, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(449) = new AttributeTypeInfo("refilldivunits3rdaft", 449, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(450) = new AttributeTypeInfo("refillpromodivunits3rdaft", 450, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(451) = new AttributeTypeInfo("unitbalance3rdaft", 451, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(452) = new AttributeTypeInfo("clearedunits3rdaft", 452, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(453) = new AttributeTypeInfo("realmoneyflag3rdaft", 453, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(454) = new AttributeTypeInfo("dedicatedaccountunit4thaft", 454, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(455) = new AttributeTypeInfo("accountstartdate4thaft", 455, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(456) = new AttributeTypeInfo("refilldivunits4thaft", 456, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(457) = new AttributeTypeInfo("refillpromodivunits4thaft", 457, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(458) = new AttributeTypeInfo("unitbalance4thaft", 458, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(459) = new AttributeTypeInfo("clearedunits4thaft", 459, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(460) = new AttributeTypeInfo("realmoneyflag4thaft", 460, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(461) = new AttributeTypeInfo("dedicatedaccountunit5thaft", 461, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(462) = new AttributeTypeInfo("accountstartdate5thaft", 462, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(463) = new AttributeTypeInfo("refilldivunits5thaft", 463, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(464) = new AttributeTypeInfo("refillpromodivunits5thaft", 464, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(465) = new AttributeTypeInfo("unitbalance5thaft", 465, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(466) = new AttributeTypeInfo("clearedunits5thaft", 466, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(467) = new AttributeTypeInfo("realmoneyflag5thaft", 467, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(468) = new AttributeTypeInfo("dedicatedaccountunit6thaft", 468, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(469) = new AttributeTypeInfo("accountstartdate6thaft", 469, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(470) = new AttributeTypeInfo("refilldivunits6thaft", 470, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(471) = new AttributeTypeInfo("refillpromodivunits6thaft", 471, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(472) = new AttributeTypeInfo("unitbalance6thaft", 472, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(473) = new AttributeTypeInfo("clearedunits6thaft", 473, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(474) = new AttributeTypeInfo("realmoneyflag6thaft", 474, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(475) = new AttributeTypeInfo("dedicatedaccountunit7thaft", 475, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(476) = new AttributeTypeInfo("accountstartdate7thaft", 476, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(477) = new AttributeTypeInfo("refilldivunits7thaft", 477, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(478) = new AttributeTypeInfo("refillpromodivunits7thaft", 478, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(479) = new AttributeTypeInfo("unitbalance7thaft", 479, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(480) = new AttributeTypeInfo("clearedunits7thaft", 480, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(481) = new AttributeTypeInfo("realmoneyflag7thaft", 481, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(482) = new AttributeTypeInfo("dedicatedaccountunit8thaft", 482, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(483) = new AttributeTypeInfo("accountstartdate8thaft", 483, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(484) = new AttributeTypeInfo("refilldivunits8thaft", 484, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(485) = new AttributeTypeInfo("refillpromodivunits8thaft", 485, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(486) = new AttributeTypeInfo("unitbalance8thaft", 486, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(487) = new AttributeTypeInfo("clearedunits8thaft", 487, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(488) = new AttributeTypeInfo("realmoneyflag8thaft", 488, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(489) = new AttributeTypeInfo("dedicatedaccountunit9thaft", 489, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(490) = new AttributeTypeInfo("accountstartdate9thaft", 490, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(491) = new AttributeTypeInfo("refilldivunits9thaft", 491, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(492) = new AttributeTypeInfo("refillpromodivunits9thaft", 492, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(493) = new AttributeTypeInfo("unitbalance9thaft", 493, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(494) = new AttributeTypeInfo("clearedunits9thaft", 494, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(495) = new AttributeTypeInfo("realmoneyflag9thaft", 495, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(496) = new AttributeTypeInfo("dedicatedaccountunit10thaft", 496, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(497) = new AttributeTypeInfo("accountstartdate10thaft", 497, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(498) = new AttributeTypeInfo("refilldivunits10thaft", 498, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(499) = new AttributeTypeInfo("refillpromodivunits10thaft", 499, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(500) = new AttributeTypeInfo("unitbalance10thaft", 500, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(501) = new AttributeTypeInfo("clearedunits10thaft", 501, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(502) = new AttributeTypeInfo("realmoneyflag10thaft", 502, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(503) = new AttributeTypeInfo("offer1stidentifieraft", 503, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(504) = new AttributeTypeInfo("offerstartdate1staft", 504, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(505) = new AttributeTypeInfo("offerexpirydate1staft", 505, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(506) = new AttributeTypeInfo("offertype1staft", 506, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(507) = new AttributeTypeInfo("offerproductidentifier1staft", 507, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(508) = new AttributeTypeInfo("offerstartdatetime1staft", 508, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(509) = new AttributeTypeInfo("offerexpirydatetime1staft", 509, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(510) = new AttributeTypeInfo("offer2ndidentifieraft", 510, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(511) = new AttributeTypeInfo("offerstartdate2ndaft", 511, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(512) = new AttributeTypeInfo("offerexpirydate2ndaft", 512, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(513) = new AttributeTypeInfo("offertype2ndaft", 513, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(514) = new AttributeTypeInfo("offerproductidentifier2ndaft", 514, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(515) = new AttributeTypeInfo("offerstartdatetime2ndaft", 515, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(516) = new AttributeTypeInfo("offerexpirydatetime2ndaft", 516, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(517) = new AttributeTypeInfo("offer3rdidentifieraft", 517, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(518) = new AttributeTypeInfo("offerstartdate3rdaft", 518, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(519) = new AttributeTypeInfo("offerexpirydate3rdaft", 519, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(520) = new AttributeTypeInfo("offertype3rdaft", 520, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(521) = new AttributeTypeInfo("offerproductidentifier3rdaft", 521, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(522) = new AttributeTypeInfo("offerstartdatetime3rdaft", 522, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(523) = new AttributeTypeInfo("offerexpirydatetime3rdaft", 523, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(524) = new AttributeTypeInfo("offer4thidentifieraft", 524, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(525) = new AttributeTypeInfo("offerstartdate4thaft", 525, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(526) = new AttributeTypeInfo("offerexpirydate4thaft", 526, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(527) = new AttributeTypeInfo("offertype4thaft", 527, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(528) = new AttributeTypeInfo("offerproductidentifier4thaft", 528, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(529) = new AttributeTypeInfo("offerstartdatetime4thaft", 529, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(530) = new AttributeTypeInfo("offerexpirydatetime4thaft", 530, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(531) = new AttributeTypeInfo("offer5thidentifieraft", 531, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(532) = new AttributeTypeInfo("offerstartdate5thaft", 532, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(533) = new AttributeTypeInfo("offerexpirydate5thaft", 533, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(534) = new AttributeTypeInfo("offertype5thaft", 534, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(535) = new AttributeTypeInfo("offerproductidentifier5thaft", 535, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(536) = new AttributeTypeInfo("offerstartdatetime5thaft", 536, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(537) = new AttributeTypeInfo("offerexpirydatetime5thaft", 537, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(538) = new AttributeTypeInfo("offer6thidentifieraft", 538, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(539) = new AttributeTypeInfo("offerstartdate6thaft", 539, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(540) = new AttributeTypeInfo("offerexpirydate6thaft", 540, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(541) = new AttributeTypeInfo("offertype6thaft", 541, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(542) = new AttributeTypeInfo("offerproductidentifier6thaft", 542, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(543) = new AttributeTypeInfo("offerstartdatetime6thaft", 543, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(544) = new AttributeTypeInfo("offerexpirydatetime6thaft", 544, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(545) = new AttributeTypeInfo("offer7thidentifieraft", 545, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(546) = new AttributeTypeInfo("offerstartdate7thaft", 546, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(547) = new AttributeTypeInfo("offerexpirydate7thaft", 547, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(548) = new AttributeTypeInfo("offertype7thaft", 548, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(549) = new AttributeTypeInfo("offerproductidentifier7thaft", 549, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(550) = new AttributeTypeInfo("offerstartdatetime7thaft", 550, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(551) = new AttributeTypeInfo("offerexpirydatetime7thaft", 551, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(552) = new AttributeTypeInfo("offer8thidentifieraft", 552, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(553) = new AttributeTypeInfo("offerstartdate8thaft", 553, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(554) = new AttributeTypeInfo("offerexpirydate8thaft", 554, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(555) = new AttributeTypeInfo("offertype8thaft", 555, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(556) = new AttributeTypeInfo("offerproductidentifier8thaft", 556, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(557) = new AttributeTypeInfo("offerstartdatetime8thaft", 557, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(558) = new AttributeTypeInfo("offerexpirydatetime8thaft", 558, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(559) = new AttributeTypeInfo("offer9thidentifieraft", 559, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(560) = new AttributeTypeInfo("offerstartdate9thaft", 560, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(561) = new AttributeTypeInfo("offerexpirydate9thaft", 561, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(562) = new AttributeTypeInfo("offertype9thaft", 562, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(563) = new AttributeTypeInfo("offerproductidentifier9thaft", 563, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(564) = new AttributeTypeInfo("offerstartdatetime9thaft", 564, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(565) = new AttributeTypeInfo("offerexpirydatetime9thaft", 565, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(566) = new AttributeTypeInfo("offer10thidentifieraft", 566, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(567) = new AttributeTypeInfo("offerstartdate10thaft", 567, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(568) = new AttributeTypeInfo("offerexpirydate10thaft", 568, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(569) = new AttributeTypeInfo("offertype10thaft", 569, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(570) = new AttributeTypeInfo("offerproductidentifier10thaft", 570, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(571) = new AttributeTypeInfo("offerstartdatetime10thaft", 571, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(572) = new AttributeTypeInfo("offerexpirydatetime10thaft", 572, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(573) = new AttributeTypeInfo("aggregatedbalanceaft", 573, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)
		 attributeTypes(574) = new AttributeTypeInfo("cellidentifier", 574, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
		 attributeTypes(575) = new AttributeTypeInfo("market_id", 575, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(576) = new AttributeTypeInfo("hub_id", 576, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
		 attributeTypes(577) = new AttributeTypeInfo("filename", 577, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)

     
      return attributeTypes
    } 
    
		 var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;
    
     if (other != null && other != this) {
      // call copying fields from other to local variables
      fromFunc(other)
    }
    
    override def save: Unit = { AirRefillCS5.saveOne(this) }
  
    def Clone(): ContainerOrConcept = { AirRefillCS5.build(this) }

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
  
  
 		var originnodetype: String = _; 
 		var originhostname: String = _; 
 		var originfileid: String = _; 
 		var origintransactionid: String = _; 
 		var originoperatorid: String = _; 
 		var origintimestamp: String = _; 
 		var hostname: String = _; 
 		var localsequencenumber: Double = _; 
 		var timestamp: String = _; 
 		var currentserviceclass: Double = _; 
 		var voucherbasedrefill: String = _; 
 		var transactiontype: String = _; 
 		var transactioncode: String = _; 
 		var transactionamount: Double = _; 
 		var transactioncurrency: String = _; 
 		var refillamountconverted: Double = _; 
 		var refilldivisionamount: Double = _; 
 		var refilltype: Double = _; 
 		var refillprofileid: String = _; 
 		var segmentationid: String = _; 
 		var voucherserialnumber: String = _; 
 		var vouchergroupid: String = _; 
 		var accountnumber: String = _; 
 		var accountcurrency: String = _; 
 		var subscribernumber: String = _; 
 		var promotionannouncementcode: String = _; 
 		var accountflagsbef: String = _; 
 		var accountbalancebef: Double = _; 
 		var accumulatedrefillvaluebef: Double = _; 
 		var accumulatedrefillcounterbef: Double = _; 
 		var accumulatedprogressionvaluebef: Double = _; 
 		var accumulatedprogrcounterbef: Double = _; 
 		var creditclearanceperiodbef: Double = _; 
 		var dedicatedaccount1stidbef: Double = _; 
 		var account1stcampaignidentbef: Double = _; 
 		var account1strefilldivamountbef: Double = _; 
 		var account1strefillpromdivamntbef: Double = _; 
 		var account1stbalancebef: Double = _; 
 		var clearedaccount1stvaluebef: Double = _; 
 		var dedicatedaccount2ndidbef: Double = _; 
 		var account2ndcampaignidentifbef: Double = _; 
 		var account2ndrefilldivamountbef: Double = _; 
 		var account2ndrefilpromodivamntbef: Double = _; 
 		var account2ndbalancebef: Double = _; 
 		var clearedaccount2ndvaluebef: Double = _; 
 		var dedicatedaccount3rdidbef: Double = _; 
 		var account3rdcampaignidentbef: Double = _; 
 		var account3rdrefilldivamountbef: Double = _; 
 		var account3rdrefilpromodivamntbef: Double = _; 
 		var account3rdbalancebef: Double = _; 
 		var clearedaccount3rdvaluebef: Double = _; 
 		var dedicatedaccount4thidbef: Double = _; 
 		var account4thcampaignidentbef: Double = _; 
 		var account4threfilldivamountbef: Double = _; 
 		var account4threfilpromodivamntbef: Double = _; 
 		var account4thbalancebef: Double = _; 
 		var clearedaccount4thvaluebef: Double = _; 
 		var dedicatedaccount5thidbef: Double = _; 
 		var account5thcampaignidentbef: Double = _; 
 		var account5threfilldivamountbef: Double = _; 
 		var account5threfilpromodivamntbef: Double = _; 
 		var account5thbalancebef: Double = _; 
 		var clearedaccount5thvaluebef: Double = _; 
 		var dedicatedaccount6thidbef: Double = _; 
 		var account6thcampaignidentbef: Double = _; 
 		var account6threfilldivamountbef: Double = _; 
 		var account6threfilpromodivamntbef: Double = _; 
 		var account6thbalancebef: Double = _; 
 		var clearedaccount6thvaluebef: Double = _; 
 		var dedicatedaccount7thidbef: Double = _; 
 		var account7thcampaignidentbef: Double = _; 
 		var account7threfilldivamountbef: Double = _; 
 		var account7threfilpromodivamntbef: Double = _; 
 		var account7thbalancebef: Double = _; 
 		var clearedaccount7thvaluebef: Double = _; 
 		var dedicatedaccount8thidbef: Double = _; 
 		var account8thcampaignidentbef: Double = _; 
 		var account8threfilldivamountbef: Double = _; 
 		var account8threfilpromodivamntbef: Double = _; 
 		var account8thbalancebef: Double = _; 
 		var clearedaccount8thvaluebef: Double = _; 
 		var dedicatedaccount9thidbef: Double = _; 
 		var account9thcampaignidentbef: Double = _; 
 		var account9threfilldivamountbef: Double = _; 
 		var account9threfilpromodivamntbef: Double = _; 
 		var account9thbalancebef: Double = _; 
 		var clearedaccount9thvaluebef: Double = _; 
 		var dedicatedaccount10thidbef: Double = _; 
 		var account10thcampaignidentbef: Double = _; 
 		var account10threfilldivamountbef: Double = _; 
 		var account10threfilpromdivamntbef: Double = _; 
 		var account10thbalancebef: Double = _; 
 		var clearedaccount10thvaluebef: Double = _; 
 		var promotionplan: String = _; 
 		var permanentserviceclassbef: Double = _; 
 		var temporaryserviceclassbef: Double = _; 
 		var temporaryservclassexpdatebef: String = _; 
 		var refilloptionbef: String = _; 
 		var servicefeeexpirydatebef: String = _; 
 		var serviceremovalgraceperiodbef: Double = _; 
 		var serviceofferingbef: String = _; 
 		var supervisionexpirydatebef: String = _; 
 		var usageaccumulator1stidbef: Double = _; 
 		var usageaccumulator1stvaluebef: Double = _; 
 		var usageaccumulator2ndidbef: Double = _; 
 		var usageaccumulator2ndvaluebef: Double = _; 
 		var usageaccumulator3rdidbef: Double = _; 
 		var usageaccumulator3rdvaluebef: Double = _; 
 		var usageaccumulator4thidbef: Double = _; 
 		var usageaccumulator4thvaluebef: Double = _; 
 		var usageaccumulator5thidbef: Double = _; 
 		var usageaccumulator5thvaluebef: Double = _; 
 		var usageaccumulator6thidbef: Double = _; 
 		var usageaccumulator6thvaluebef: Double = _; 
 		var usageaccumulator7thidbef: Double = _; 
 		var usageaccumulator7thvaluebef: Double = _; 
 		var usageaccumulator8thidbef: Double = _; 
 		var usageaccumulator8thvaluebef: Double = _; 
 		var usageaccumulator9thidbef: Double = _; 
 		var usageaccumulator9thvaluebef: Double = _; 
 		var usageaccumulator10thidbef: Double = _; 
 		var usageaccumulator10thvaluebef: Double = _; 
 		var communityidbef1: String = _; 
 		var communityidbef2: String = _; 
 		var communityidbef3: String = _; 
 		var accountflagsaft: String = _; 
 		var accountbalanceaft: Double = _; 
 		var accumulatedrefillvalueaft: Double = _; 
 		var accumulatedrefillcounteraft: Double = _; 
 		var accumulatedprogressionvalueaft: Double = _; 
 		var accumulatedprogconteraft: Double = _; 
 		var creditclearanceperiodaft: Double = _; 
 		var dedicatedaccount1stidaft: Double = _; 
 		var account1stcampaignidentaft: Double = _; 
 		var account1strefilldivamountaft: Double = _; 
 		var account1strefilpromodivamntaft: Double = _; 
 		var account1stbalanceaft: Double = _; 
 		var clearedaccount1stvalueaft: Double = _; 
 		var dedicatedaccount2ndidaft: Double = _; 
 		var account2ndcampaignidentaft: Double = _; 
 		var account2ndrefilldivamountaft: Double = _; 
 		var account2ndrefilpromodivamntaft: Double = _; 
 		var account2ndbalanceaft: Double = _; 
 		var clearedaccount2ndvalueaft: Double = _; 
 		var dedicatedaccount3rdidaft: Double = _; 
 		var account3rdcampaignidentaft: Double = _; 
 		var account3rdrefilldivamountaft: Double = _; 
 		var account3rdrefilpromodivamntaft: Double = _; 
 		var account3rdbalanceaft: Double = _; 
 		var clearedaccount3rdvalueaft: Double = _; 
 		var dedicatedaccount4thidaft: Double = _; 
 		var account4thcampaignidentaft: Double = _; 
 		var account4threfilldivamountaft: Double = _; 
 		var account4threfilpromodivamntaft: Double = _; 
 		var account4thbalanceaft: Double = _; 
 		var clearedaccount4thvalueaft: Double = _; 
 		var dedicatedaccount5thidaft: Double = _; 
 		var account5thcampaignidentaft: Double = _; 
 		var account5threfilldivamountaft: Double = _; 
 		var account5threfilpromodivamntaft: Double = _; 
 		var account5thbalanceaft: Double = _; 
 		var clearedaccount5thvalueaft: Double = _; 
 		var dedicatedaccount6thidaft: Double = _; 
 		var account6thcampaignidentaft: Double = _; 
 		var account6threfilldivamountaft: Double = _; 
 		var account6threfilpromodivamntaft: Double = _; 
 		var account6thbalanceaft: Double = _; 
 		var clearedaccount6thvalueaft: Double = _; 
 		var dedicatedaccount7thidaft: Double = _; 
 		var account7thcampaignidentaft: Double = _; 
 		var account7threfilldivamountaft: Double = _; 
 		var account7threfilpromodivamntaft: Double = _; 
 		var account7thbalanceaft: Double = _; 
 		var clearedaccount7thvalueaft: Double = _; 
 		var dedicatedaccount8thidaft: Double = _; 
 		var account8thcampaignidentaft: Double = _; 
 		var account8threfilldivamountaft: Double = _; 
 		var account8threfilpromodivamntaft: Double = _; 
 		var account8thbalanceaft: Double = _; 
 		var clearedaccount8thvalueaft: Double = _; 
 		var dedicatedaccount9thidaft: Double = _; 
 		var account9thcampaignidentaft: Double = _; 
 		var account9threfilldivamountaft: Double = _; 
 		var account9threfilpromodivamntaft: Double = _; 
 		var account9thbalanceaft: Double = _; 
 		var clearedaccount9thvalueaft: Double = _; 
 		var dedicatedaccount10thidaft: Double = _; 
 		var account10thcampaignidentaft: Double = _; 
 		var account10threfilldivamountaft: Double = _; 
 		var account10threfilpromdivamntaft: Double = _; 
 		var account10thbalanceaft: Double = _; 
 		var clearedaccount10thvalueaft: Double = _; 
 		var promotionplanaft: String = _; 
 		var permanentserviceclassaft: Double = _; 
 		var temporaryserviceclassaft: Double = _; 
 		var temporaryservclasexpirydateaft: Double = _; 
 		var refilloptionaft: String = _; 
 		var servicefeeexpirydateaft: String = _; 
 		var serviceremovalgraceperiodaft: Double = _; 
 		var serviceofferingaft: String = _; 
 		var supervisionexpirydateaft: String = _; 
 		var usageaccumulator1stidaft: Double = _; 
 		var usageaccumulator1stvalueaft: Double = _; 
 		var usageaccumulator2ndidaft: Double = _; 
 		var usageaccumulator2ndvalueaft: Double = _; 
 		var usageaccumulator3rdidaft: Double = _; 
 		var usageaccumulator3rdvalueaft: Double = _; 
 		var usageaccumulator4thidaft: Double = _; 
 		var usageaccumulator4thvalueaft: Double = _; 
 		var usageaccumulator5thidaft: Double = _; 
 		var usageaccumulator5thvalueaft: Double = _; 
 		var usageaccumulator6thidaft: Double = _; 
 		var usageaccumulator6thvalueaft: Double = _; 
 		var usageaccumulator7thidaft: Double = _; 
 		var usageaccumulator7thvalueaft: Double = _; 
 		var usageaccumulator8thidaft: Double = _; 
 		var usageaccumulator8thvalueaft: Double = _; 
 		var usageaccumulator9thidaft: Double = _; 
 		var usageaccumulator9thvalueaft: Double = _; 
 		var usageaccumulator10thidaft: Double = _; 
 		var usageaccumulator10thvalueaft: Double = _; 
 		var communityidaft1: String = _; 
 		var communityidaft2: String = _; 
 		var communityidaft3: String = _; 
 		var refillpromodivisionamount: Double = _; 
 		var supervisiondayspromopart: Double = _; 
 		var supervisiondayssurplus: Double = _; 
 		var servicefeedayspromopart: Double = _; 
 		var servicefeedayssurplus: Double = _; 
 		var maximumservicefeeperiod: Double = _; 
 		var maximumsupervisionperiod: Double = _; 
 		var activationdate: String = _; 
 		var welcomestatus: Double = _; 
 		var voucheragent: String = _; 
 		var promotionplanallocstartdate: String = _; 
 		var accountgroupid: String = _; 
 		var externaldata1: String = _; 
 		var externaldata2: String = _; 
 		var externaldata3: String = _; 
 		var externaldata4: String = _; 
 		var locationnumber: String = _; 
 		var voucheractivationcode: String = _; 
 		var accountcurrencycleared: String = _; 
 		var ignoreserviceclasshierarchy: String = _; 
 		var accounthomeregion: String = _; 
 		var subscriberregion: String = _; 
 		var voucherregion: String = _; 
 		var promotionplanallocenddate: String = _; 
 		var requestedrefilltype: String = _; 
 		var accountexpiry1stdatebef: String = _; 
 		var accountexpiry1stdateaft: String = _; 
 		var accountexpiry2nddatebef: String = _; 
 		var accountexpiry2nddateaft: String = _; 
 		var accountexpiry3rddatebef: String = _; 
 		var accountexpiry3rddateaft: String = _; 
 		var accountexpiry4thdatebef: String = _; 
 		var accountexpiry4thdateaft: String = _; 
 		var accountexpiry5thdatebef: String = _; 
 		var accountexpiry5thdateaft: String = _; 
 		var accountexpiry6thdatebef: String = _; 
 		var accountexpiry6thdateaft: String = _; 
 		var accountexpiry7thdatebef: String = _; 
 		var accountexpiry7thdateaft: String = _; 
 		var accountexpiry8thdatebef: String = _; 
 		var accountexpiry8thdateaft: String = _; 
 		var accountexpiry9thdatebef: String = _; 
 		var accountexpiry9thdateaft: String = _; 
 		var accountexpiry10thdatebef: String = _; 
 		var accountexpiry10thdateaft: String = _; 
 		var rechargedivpartmain: Double = _; 
 		var rechargedivpartda1st: Double = _; 
 		var rechargedivpartda2nd: Double = _; 
 		var rechargedivpartda3rd: Double = _; 
 		var rechargedivpartda4th: Double = _; 
 		var rechargedivpartda5th: Double = _; 
 		var accumulatedprogressionvalueres: Double = _; 
 		var rechargedivpartpmain: Double = _; 
 		var rechargedivpartpda1st: Double = _; 
 		var rechargedivpartpda2nd: Double = _; 
 		var rechargedivpartpda3rd: Double = _; 
 		var rechargedivpartpda4th: Double = _; 
 		var rechargedivpartpda5th: Double = _; 
 		var rechargedivpartda6th: Double = _; 
 		var rechargedivpartda7th: Double = _; 
 		var rechargedivpartda8th: Double = _; 
 		var rechargedivpartda9th: Double = _; 
 		var rechargedivpartda10th: Double = _; 
 		var rechargedivpartpda6th: Double = _; 
 		var rechargedivpartpda7th: Double = _; 
 		var rechargedivpartpda8th: Double = _; 
 		var rechargedivpartpda9th: Double = _; 
 		var rechargedivpartpda10th: Double = _; 
 		var dedicatedaccountunit1stbef: Double = _; 
 		var accountstartdate1stbef: String = _; 
 		var refilldivunits1stbef: Double = _; 
 		var refillpromodivunits1stbef: Double = _; 
 		var unitbalance1stbef: Double = _; 
 		var clearedunits1stbef: Double = _; 
 		var realmoneyflag1stbef: Double = _; 
 		var dedicatedaccountunit2ndbef: Double = _; 
 		var accountstartdate2ndbef: String = _; 
 		var refilldivunits2ndbef: Double = _; 
 		var refillpromodivunits2ndbef: Double = _; 
 		var unitbalance2ndbef: Double = _; 
 		var clearedunits2ndbef: Double = _; 
 		var realmoneyflag2ndbef: Double = _; 
 		var dedicatedaccountunit3rdbef: Double = _; 
 		var accountstartdate3rdbef: String = _; 
 		var refilldivunits3rdbef: Double = _; 
 		var refillpromodivunits3rdbef: Double = _; 
 		var unitbalance3rdbef: Double = _; 
 		var clearedunits3rdbef: Double = _; 
 		var realmoneyflag3rdbef: Double = _; 
 		var dedicatedaccountunit4thbef: Double = _; 
 		var accountstartdate4thbef: String = _; 
 		var refilldivunits4thbef: Double = _; 
 		var refillpromodivunits4thbef: Double = _; 
 		var unitbalance4thbef: Double = _; 
 		var clearedunits4thbef: Double = _; 
 		var realmoneyflag4thbef: Double = _; 
 		var dedicatedaccountunit5thbef: Double = _; 
 		var accountstartdate5thbef: String = _; 
 		var refilldivunits5thbef: Double = _; 
 		var refillpromodivunits5thbef: Double = _; 
 		var unitbalance5thbef: Double = _; 
 		var clearedunits5thbef: Double = _; 
 		var realmoneyflag5thbef: Double = _; 
 		var dedicatedaccountunit6thbef: Double = _; 
 		var accountstartdate6thbef: String = _; 
 		var refilldivunits6thbef: Double = _; 
 		var refillpromodivunits6thbef: Double = _; 
 		var unitbalance6thbef: Double = _; 
 		var clearedunits6thbef: Double = _; 
 		var realmoneyflag6thbef: Double = _; 
 		var dedicatedaccountunit7thbef: Double = _; 
 		var accountstartdate7thbef: String = _; 
 		var refilldivunits7thbef: Double = _; 
 		var refillpromodivunits7thbef: Double = _; 
 		var unitbalance7thbef: Double = _; 
 		var clearedunits7thbef: Double = _; 
 		var realmoneyflag7thbef: Double = _; 
 		var dedicatedaccountunit8thbef: Double = _; 
 		var accountstartdate8thbef: String = _; 
 		var refilldivunits8thbef: Double = _; 
 		var refillpromodivunits8thbef: Double = _; 
 		var unitbalance8thbef: Double = _; 
 		var clearedunits8thbef: Double = _; 
 		var realmoneyflag8thbef: Double = _; 
 		var dedicatedaccountunit9thbef: Double = _; 
 		var accountstartdate9thbef: String = _; 
 		var refilldivunits9thbef: Double = _; 
 		var refillpromodivunits9thbef: Double = _; 
 		var unitbalance9thbef: Double = _; 
 		var clearedunits9thbef: Double = _; 
 		var realmoneyflag9thbef: Double = _; 
 		var dedicatedaccountunit10thbef: Double = _; 
 		var accountstartdate10thbef: String = _; 
 		var refilldivunits10thbef: Double = _; 
 		var refillpromodivunits10thbef: Double = _; 
 		var unitbalance10thbef: Double = _; 
 		var clearedunits10thbef: Double = _; 
 		var realmoneyflag10thbef: Double = _; 
 		var offer1stidentifierbef: Double = _; 
 		var offerstartdate1stbef: String = _; 
 		var offerexpirydate1stbef: String = _; 
 		var offertype1stbef: String = _; 
 		var offerproductidentifier1stbef: Double = _; 
 		var offerstartdatetime1stbef: String = _; 
 		var offerexpirydatetime1stbef: String = _; 
 		var offer2ndidentifierbef: Double = _; 
 		var offerstartdate2ndbef: String = _; 
 		var offerexpirydate2ndbef: String = _; 
 		var offertype2ndbef: String = _; 
 		var offerproductidentifier2ndbef: Double = _; 
 		var offerstartdatetime2ndbef: String = _; 
 		var offerexpirydatetime2ndbef: String = _; 
 		var offer3rdidentifierbef: Double = _; 
 		var offerstartdate3rdbef: String = _; 
 		var offerexpirydate3rdbef: String = _; 
 		var offertype3rdbef: String = _; 
 		var offerproductidentifier3rdbef: Double = _; 
 		var offerstartdatetime3rdbef: String = _; 
 		var offerexpirydatetime3rdbef: String = _; 
 		var offer4thidentifierbef: Double = _; 
 		var offerstartdate4thbef: String = _; 
 		var offerexpirydate4thbef: String = _; 
 		var offertype4thbef: String = _; 
 		var offerproductidentifier4thbef: Double = _; 
 		var offerstartdatetime4thbef: String = _; 
 		var offerexpirydatetime4thbef: String = _; 
 		var offer5thidentifierbef: Double = _; 
 		var offerstartdate5thbef: String = _; 
 		var offerexpirydate5thbef: String = _; 
 		var offertype5thbef: String = _; 
 		var offerproductidentifier5thbef: Double = _; 
 		var offerstartdatetime5thbef: String = _; 
 		var offerexpirydatetime5thbef: String = _; 
 		var offer6thidentifierbef: Double = _; 
 		var offerstartdate6thbef: String = _; 
 		var offerexpirydate6thbef: String = _; 
 		var offertype6thbef: String = _; 
 		var offerproductidentifier6thbef: Double = _; 
 		var offerstartdatetime6thbef: String = _; 
 		var offerexpirydatetime6thbef: String = _; 
 		var offer7thidentifierbef: Double = _; 
 		var offerstartdate7thbef: String = _; 
 		var offerexpirydate7thbef: String = _; 
 		var offertype7thbef: String = _; 
 		var offerproductidentifier7thbef: Double = _; 
 		var offerstartdatetime7thbef: String = _; 
 		var offerexpirydatetime7thbef: String = _; 
 		var offer8thidentifierbef: Double = _; 
 		var offerstartdate8thbef: String = _; 
 		var offerexpirydate8thbef: String = _; 
 		var offertype8thbef: String = _; 
 		var offerproductidentifier8thbef: Double = _; 
 		var offerstartdatetime8thbef: String = _; 
 		var offerexpirydatetime8thbef: String = _; 
 		var offer9thidentifierbef: Double = _; 
 		var offerstartdate9thbef: String = _; 
 		var offerexpirydate9thbef: String = _; 
 		var offertype9thbef: String = _; 
 		var offerproductidentifier9thbef: Double = _; 
 		var offerstartdatetime9thbef: String = _; 
 		var offerexpirydatetime9thbef: String = _; 
 		var offer10thidentifierbef: Double = _; 
 		var offerstartdate10thbef: String = _; 
 		var offerexpirydate10thbef: String = _; 
 		var offertype10thbef: String = _; 
 		var offerproductidentifier10thbef: Double = _; 
 		var offerstartdatetime10thbef: String = _; 
 		var offerexpirydatetime10thbef: String = _; 
 		var aggregatedbalancebef: Double = _; 
 		var dedicatedaccountunit1staft: Double = _; 
 		var accountstartdate1staft: String = _; 
 		var refilldivunits1staft: Double = _; 
 		var refillpromodivunits1staft: Double = _; 
 		var unitbalance1staft: Double = _; 
 		var clearedunits1staft: Double = _; 
 		var realmoneyflag1staft: Double = _; 
 		var dedicatedaccountunit2ndaft: Double = _; 
 		var accountstartdate2ndaft: String = _; 
 		var refilldivunits2ndaft: Double = _; 
 		var refillpromodivunits2ndaft: Double = _; 
 		var unitbalance2ndaft: Double = _; 
 		var clearedunits2ndaft: Double = _; 
 		var realmoneyflag2ndaft: Double = _; 
 		var dedicatedaccountunit3rdaft: Double = _; 
 		var accountstartdate3rdaft: String = _; 
 		var refilldivunits3rdaft: Double = _; 
 		var refillpromodivunits3rdaft: Double = _; 
 		var unitbalance3rdaft: Double = _; 
 		var clearedunits3rdaft: Double = _; 
 		var realmoneyflag3rdaft: Double = _; 
 		var dedicatedaccountunit4thaft: Double = _; 
 		var accountstartdate4thaft: String = _; 
 		var refilldivunits4thaft: Double = _; 
 		var refillpromodivunits4thaft: Double = _; 
 		var unitbalance4thaft: Double = _; 
 		var clearedunits4thaft: Double = _; 
 		var realmoneyflag4thaft: Double = _; 
 		var dedicatedaccountunit5thaft: Double = _; 
 		var accountstartdate5thaft: String = _; 
 		var refilldivunits5thaft: Double = _; 
 		var refillpromodivunits5thaft: Double = _; 
 		var unitbalance5thaft: Double = _; 
 		var clearedunits5thaft: Double = _; 
 		var realmoneyflag5thaft: Double = _; 
 		var dedicatedaccountunit6thaft: Double = _; 
 		var accountstartdate6thaft: String = _; 
 		var refilldivunits6thaft: Double = _; 
 		var refillpromodivunits6thaft: Double = _; 
 		var unitbalance6thaft: Double = _; 
 		var clearedunits6thaft: Double = _; 
 		var realmoneyflag6thaft: Double = _; 
 		var dedicatedaccountunit7thaft: Double = _; 
 		var accountstartdate7thaft: String = _; 
 		var refilldivunits7thaft: Double = _; 
 		var refillpromodivunits7thaft: Double = _; 
 		var unitbalance7thaft: Double = _; 
 		var clearedunits7thaft: Double = _; 
 		var realmoneyflag7thaft: Double = _; 
 		var dedicatedaccountunit8thaft: Double = _; 
 		var accountstartdate8thaft: String = _; 
 		var refilldivunits8thaft: Double = _; 
 		var refillpromodivunits8thaft: Double = _; 
 		var unitbalance8thaft: Double = _; 
 		var clearedunits8thaft: Double = _; 
 		var realmoneyflag8thaft: Double = _; 
 		var dedicatedaccountunit9thaft: Double = _; 
 		var accountstartdate9thaft: String = _; 
 		var refilldivunits9thaft: Double = _; 
 		var refillpromodivunits9thaft: Double = _; 
 		var unitbalance9thaft: Double = _; 
 		var clearedunits9thaft: Double = _; 
 		var realmoneyflag9thaft: Double = _; 
 		var dedicatedaccountunit10thaft: Double = _; 
 		var accountstartdate10thaft: String = _; 
 		var refilldivunits10thaft: Double = _; 
 		var refillpromodivunits10thaft: Double = _; 
 		var unitbalance10thaft: Double = _; 
 		var clearedunits10thaft: Double = _; 
 		var realmoneyflag10thaft: Double = _; 
 		var offer1stidentifieraft: Double = _; 
 		var offerstartdate1staft: String = _; 
 		var offerexpirydate1staft: String = _; 
 		var offertype1staft: String = _; 
 		var offerproductidentifier1staft: Double = _; 
 		var offerstartdatetime1staft: String = _; 
 		var offerexpirydatetime1staft: String = _; 
 		var offer2ndidentifieraft: Double = _; 
 		var offerstartdate2ndaft: String = _; 
 		var offerexpirydate2ndaft: String = _; 
 		var offertype2ndaft: String = _; 
 		var offerproductidentifier2ndaft: Double = _; 
 		var offerstartdatetime2ndaft: String = _; 
 		var offerexpirydatetime2ndaft: String = _; 
 		var offer3rdidentifieraft: Double = _; 
 		var offerstartdate3rdaft: String = _; 
 		var offerexpirydate3rdaft: String = _; 
 		var offertype3rdaft: String = _; 
 		var offerproductidentifier3rdaft: Double = _; 
 		var offerstartdatetime3rdaft: String = _; 
 		var offerexpirydatetime3rdaft: String = _; 
 		var offer4thidentifieraft: Double = _; 
 		var offerstartdate4thaft: String = _; 
 		var offerexpirydate4thaft: String = _; 
 		var offertype4thaft: String = _; 
 		var offerproductidentifier4thaft: Double = _; 
 		var offerstartdatetime4thaft: String = _; 
 		var offerexpirydatetime4thaft: String = _; 
 		var offer5thidentifieraft: Double = _; 
 		var offerstartdate5thaft: String = _; 
 		var offerexpirydate5thaft: String = _; 
 		var offertype5thaft: String = _; 
 		var offerproductidentifier5thaft: Double = _; 
 		var offerstartdatetime5thaft: String = _; 
 		var offerexpirydatetime5thaft: String = _; 
 		var offer6thidentifieraft: Double = _; 
 		var offerstartdate6thaft: String = _; 
 		var offerexpirydate6thaft: String = _; 
 		var offertype6thaft: String = _; 
 		var offerproductidentifier6thaft: Double = _; 
 		var offerstartdatetime6thaft: String = _; 
 		var offerexpirydatetime6thaft: String = _; 
 		var offer7thidentifieraft: Double = _; 
 		var offerstartdate7thaft: String = _; 
 		var offerexpirydate7thaft: String = _; 
 		var offertype7thaft: String = _; 
 		var offerproductidentifier7thaft: Double = _; 
 		var offerstartdatetime7thaft: String = _; 
 		var offerexpirydatetime7thaft: String = _; 
 		var offer8thidentifieraft: Double = _; 
 		var offerstartdate8thaft: String = _; 
 		var offerexpirydate8thaft: String = _; 
 		var offertype8thaft: String = _; 
 		var offerproductidentifier8thaft: Double = _; 
 		var offerstartdatetime8thaft: String = _; 
 		var offerexpirydatetime8thaft: String = _; 
 		var offer9thidentifieraft: Double = _; 
 		var offerstartdate9thaft: String = _; 
 		var offerexpirydate9thaft: String = _; 
 		var offertype9thaft: String = _; 
 		var offerproductidentifier9thaft: Double = _; 
 		var offerstartdatetime9thaft: String = _; 
 		var offerexpirydatetime9thaft: String = _; 
 		var offer10thidentifieraft: Double = _; 
 		var offerstartdate10thaft: String = _; 
 		var offerexpirydate10thaft: String = _; 
 		var offertype10thaft: String = _; 
 		var offerproductidentifier10thaft: Double = _; 
 		var offerstartdatetime10thaft: String = _; 
 		var offerexpirydatetime10thaft: String = _; 
 		var aggregatedbalanceaft: Double = _; 
 		var cellidentifier: String = _; 
 		var market_id: Int = _; 
 		var hub_id: Int = _; 
 		var filename: String = _; 

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
      val fieldX = ru.typeOf[AirRefillCS5].declaration(ru.newTermName(key)).asTerm.accessed.asTerm
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
   
      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container AirRefillCS5", null);
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
   		case 0 => return this.originnodetype.asInstanceOf[AnyRef]; 
		case 1 => return this.originhostname.asInstanceOf[AnyRef]; 
		case 2 => return this.originfileid.asInstanceOf[AnyRef]; 
		case 3 => return this.origintransactionid.asInstanceOf[AnyRef]; 
		case 4 => return this.originoperatorid.asInstanceOf[AnyRef]; 
		case 5 => return this.origintimestamp.asInstanceOf[AnyRef]; 
		case 6 => return this.hostname.asInstanceOf[AnyRef]; 
		case 7 => return this.localsequencenumber.asInstanceOf[AnyRef]; 
		case 8 => return this.timestamp.asInstanceOf[AnyRef]; 
		case 9 => return this.currentserviceclass.asInstanceOf[AnyRef]; 
		case 10 => return this.voucherbasedrefill.asInstanceOf[AnyRef]; 
		case 11 => return this.transactiontype.asInstanceOf[AnyRef]; 
		case 12 => return this.transactioncode.asInstanceOf[AnyRef]; 
		case 13 => return this.transactionamount.asInstanceOf[AnyRef]; 
		case 14 => return this.transactioncurrency.asInstanceOf[AnyRef]; 
		case 15 => return this.refillamountconverted.asInstanceOf[AnyRef]; 
		case 16 => return this.refilldivisionamount.asInstanceOf[AnyRef]; 
		case 17 => return this.refilltype.asInstanceOf[AnyRef]; 
		case 18 => return this.refillprofileid.asInstanceOf[AnyRef]; 
		case 19 => return this.segmentationid.asInstanceOf[AnyRef]; 
		case 20 => return this.voucherserialnumber.asInstanceOf[AnyRef]; 
		case 21 => return this.vouchergroupid.asInstanceOf[AnyRef]; 
		case 22 => return this.accountnumber.asInstanceOf[AnyRef]; 
		case 23 => return this.accountcurrency.asInstanceOf[AnyRef]; 
		case 24 => return this.subscribernumber.asInstanceOf[AnyRef]; 
		case 25 => return this.promotionannouncementcode.asInstanceOf[AnyRef]; 
		case 26 => return this.accountflagsbef.asInstanceOf[AnyRef]; 
		case 27 => return this.accountbalancebef.asInstanceOf[AnyRef]; 
		case 28 => return this.accumulatedrefillvaluebef.asInstanceOf[AnyRef]; 
		case 29 => return this.accumulatedrefillcounterbef.asInstanceOf[AnyRef]; 
		case 30 => return this.accumulatedprogressionvaluebef.asInstanceOf[AnyRef]; 
		case 31 => return this.accumulatedprogrcounterbef.asInstanceOf[AnyRef]; 
		case 32 => return this.creditclearanceperiodbef.asInstanceOf[AnyRef]; 
		case 33 => return this.dedicatedaccount1stidbef.asInstanceOf[AnyRef]; 
		case 34 => return this.account1stcampaignidentbef.asInstanceOf[AnyRef]; 
		case 35 => return this.account1strefilldivamountbef.asInstanceOf[AnyRef]; 
		case 36 => return this.account1strefillpromdivamntbef.asInstanceOf[AnyRef]; 
		case 37 => return this.account1stbalancebef.asInstanceOf[AnyRef]; 
		case 38 => return this.clearedaccount1stvaluebef.asInstanceOf[AnyRef]; 
		case 39 => return this.dedicatedaccount2ndidbef.asInstanceOf[AnyRef]; 
		case 40 => return this.account2ndcampaignidentifbef.asInstanceOf[AnyRef]; 
		case 41 => return this.account2ndrefilldivamountbef.asInstanceOf[AnyRef]; 
		case 42 => return this.account2ndrefilpromodivamntbef.asInstanceOf[AnyRef]; 
		case 43 => return this.account2ndbalancebef.asInstanceOf[AnyRef]; 
		case 44 => return this.clearedaccount2ndvaluebef.asInstanceOf[AnyRef]; 
		case 45 => return this.dedicatedaccount3rdidbef.asInstanceOf[AnyRef]; 
		case 46 => return this.account3rdcampaignidentbef.asInstanceOf[AnyRef]; 
		case 47 => return this.account3rdrefilldivamountbef.asInstanceOf[AnyRef]; 
		case 48 => return this.account3rdrefilpromodivamntbef.asInstanceOf[AnyRef]; 
		case 49 => return this.account3rdbalancebef.asInstanceOf[AnyRef]; 
		case 50 => return this.clearedaccount3rdvaluebef.asInstanceOf[AnyRef]; 
		case 51 => return this.dedicatedaccount4thidbef.asInstanceOf[AnyRef]; 
		case 52 => return this.account4thcampaignidentbef.asInstanceOf[AnyRef]; 
		case 53 => return this.account4threfilldivamountbef.asInstanceOf[AnyRef]; 
		case 54 => return this.account4threfilpromodivamntbef.asInstanceOf[AnyRef]; 
		case 55 => return this.account4thbalancebef.asInstanceOf[AnyRef]; 
		case 56 => return this.clearedaccount4thvaluebef.asInstanceOf[AnyRef]; 
		case 57 => return this.dedicatedaccount5thidbef.asInstanceOf[AnyRef]; 
		case 58 => return this.account5thcampaignidentbef.asInstanceOf[AnyRef]; 
		case 59 => return this.account5threfilldivamountbef.asInstanceOf[AnyRef]; 
		case 60 => return this.account5threfilpromodivamntbef.asInstanceOf[AnyRef]; 
		case 61 => return this.account5thbalancebef.asInstanceOf[AnyRef]; 
		case 62 => return this.clearedaccount5thvaluebef.asInstanceOf[AnyRef]; 
		case 63 => return this.dedicatedaccount6thidbef.asInstanceOf[AnyRef]; 
		case 64 => return this.account6thcampaignidentbef.asInstanceOf[AnyRef]; 
		case 65 => return this.account6threfilldivamountbef.asInstanceOf[AnyRef]; 
		case 66 => return this.account6threfilpromodivamntbef.asInstanceOf[AnyRef]; 
		case 67 => return this.account6thbalancebef.asInstanceOf[AnyRef]; 
		case 68 => return this.clearedaccount6thvaluebef.asInstanceOf[AnyRef]; 
		case 69 => return this.dedicatedaccount7thidbef.asInstanceOf[AnyRef]; 
		case 70 => return this.account7thcampaignidentbef.asInstanceOf[AnyRef]; 
		case 71 => return this.account7threfilldivamountbef.asInstanceOf[AnyRef]; 
		case 72 => return this.account7threfilpromodivamntbef.asInstanceOf[AnyRef]; 
		case 73 => return this.account7thbalancebef.asInstanceOf[AnyRef]; 
		case 74 => return this.clearedaccount7thvaluebef.asInstanceOf[AnyRef]; 
		case 75 => return this.dedicatedaccount8thidbef.asInstanceOf[AnyRef]; 
		case 76 => return this.account8thcampaignidentbef.asInstanceOf[AnyRef]; 
		case 77 => return this.account8threfilldivamountbef.asInstanceOf[AnyRef]; 
		case 78 => return this.account8threfilpromodivamntbef.asInstanceOf[AnyRef]; 
		case 79 => return this.account8thbalancebef.asInstanceOf[AnyRef]; 
		case 80 => return this.clearedaccount8thvaluebef.asInstanceOf[AnyRef]; 
		case 81 => return this.dedicatedaccount9thidbef.asInstanceOf[AnyRef]; 
		case 82 => return this.account9thcampaignidentbef.asInstanceOf[AnyRef]; 
		case 83 => return this.account9threfilldivamountbef.asInstanceOf[AnyRef]; 
		case 84 => return this.account9threfilpromodivamntbef.asInstanceOf[AnyRef]; 
		case 85 => return this.account9thbalancebef.asInstanceOf[AnyRef]; 
		case 86 => return this.clearedaccount9thvaluebef.asInstanceOf[AnyRef]; 
		case 87 => return this.dedicatedaccount10thidbef.asInstanceOf[AnyRef]; 
		case 88 => return this.account10thcampaignidentbef.asInstanceOf[AnyRef]; 
		case 89 => return this.account10threfilldivamountbef.asInstanceOf[AnyRef]; 
		case 90 => return this.account10threfilpromdivamntbef.asInstanceOf[AnyRef]; 
		case 91 => return this.account10thbalancebef.asInstanceOf[AnyRef]; 
		case 92 => return this.clearedaccount10thvaluebef.asInstanceOf[AnyRef]; 
		case 93 => return this.promotionplan.asInstanceOf[AnyRef]; 
		case 94 => return this.permanentserviceclassbef.asInstanceOf[AnyRef]; 
		case 95 => return this.temporaryserviceclassbef.asInstanceOf[AnyRef]; 
		case 96 => return this.temporaryservclassexpdatebef.asInstanceOf[AnyRef]; 
		case 97 => return this.refilloptionbef.asInstanceOf[AnyRef]; 
		case 98 => return this.servicefeeexpirydatebef.asInstanceOf[AnyRef]; 
		case 99 => return this.serviceremovalgraceperiodbef.asInstanceOf[AnyRef]; 
		case 100 => return this.serviceofferingbef.asInstanceOf[AnyRef]; 
		case 101 => return this.supervisionexpirydatebef.asInstanceOf[AnyRef]; 
		case 102 => return this.usageaccumulator1stidbef.asInstanceOf[AnyRef]; 
		case 103 => return this.usageaccumulator1stvaluebef.asInstanceOf[AnyRef]; 
		case 104 => return this.usageaccumulator2ndidbef.asInstanceOf[AnyRef]; 
		case 105 => return this.usageaccumulator2ndvaluebef.asInstanceOf[AnyRef]; 
		case 106 => return this.usageaccumulator3rdidbef.asInstanceOf[AnyRef]; 
		case 107 => return this.usageaccumulator3rdvaluebef.asInstanceOf[AnyRef]; 
		case 108 => return this.usageaccumulator4thidbef.asInstanceOf[AnyRef]; 
		case 109 => return this.usageaccumulator4thvaluebef.asInstanceOf[AnyRef]; 
		case 110 => return this.usageaccumulator5thidbef.asInstanceOf[AnyRef]; 
		case 111 => return this.usageaccumulator5thvaluebef.asInstanceOf[AnyRef]; 
		case 112 => return this.usageaccumulator6thidbef.asInstanceOf[AnyRef]; 
		case 113 => return this.usageaccumulator6thvaluebef.asInstanceOf[AnyRef]; 
		case 114 => return this.usageaccumulator7thidbef.asInstanceOf[AnyRef]; 
		case 115 => return this.usageaccumulator7thvaluebef.asInstanceOf[AnyRef]; 
		case 116 => return this.usageaccumulator8thidbef.asInstanceOf[AnyRef]; 
		case 117 => return this.usageaccumulator8thvaluebef.asInstanceOf[AnyRef]; 
		case 118 => return this.usageaccumulator9thidbef.asInstanceOf[AnyRef]; 
		case 119 => return this.usageaccumulator9thvaluebef.asInstanceOf[AnyRef]; 
		case 120 => return this.usageaccumulator10thidbef.asInstanceOf[AnyRef]; 
		case 121 => return this.usageaccumulator10thvaluebef.asInstanceOf[AnyRef]; 
		case 122 => return this.communityidbef1.asInstanceOf[AnyRef]; 
		case 123 => return this.communityidbef2.asInstanceOf[AnyRef]; 
		case 124 => return this.communityidbef3.asInstanceOf[AnyRef]; 
		case 125 => return this.accountflagsaft.asInstanceOf[AnyRef]; 
		case 126 => return this.accountbalanceaft.asInstanceOf[AnyRef]; 
		case 127 => return this.accumulatedrefillvalueaft.asInstanceOf[AnyRef]; 
		case 128 => return this.accumulatedrefillcounteraft.asInstanceOf[AnyRef]; 
		case 129 => return this.accumulatedprogressionvalueaft.asInstanceOf[AnyRef]; 
		case 130 => return this.accumulatedprogconteraft.asInstanceOf[AnyRef]; 
		case 131 => return this.creditclearanceperiodaft.asInstanceOf[AnyRef]; 
		case 132 => return this.dedicatedaccount1stidaft.asInstanceOf[AnyRef]; 
		case 133 => return this.account1stcampaignidentaft.asInstanceOf[AnyRef]; 
		case 134 => return this.account1strefilldivamountaft.asInstanceOf[AnyRef]; 
		case 135 => return this.account1strefilpromodivamntaft.asInstanceOf[AnyRef]; 
		case 136 => return this.account1stbalanceaft.asInstanceOf[AnyRef]; 
		case 137 => return this.clearedaccount1stvalueaft.asInstanceOf[AnyRef]; 
		case 138 => return this.dedicatedaccount2ndidaft.asInstanceOf[AnyRef]; 
		case 139 => return this.account2ndcampaignidentaft.asInstanceOf[AnyRef]; 
		case 140 => return this.account2ndrefilldivamountaft.asInstanceOf[AnyRef]; 
		case 141 => return this.account2ndrefilpromodivamntaft.asInstanceOf[AnyRef]; 
		case 142 => return this.account2ndbalanceaft.asInstanceOf[AnyRef]; 
		case 143 => return this.clearedaccount2ndvalueaft.asInstanceOf[AnyRef]; 
		case 144 => return this.dedicatedaccount3rdidaft.asInstanceOf[AnyRef]; 
		case 145 => return this.account3rdcampaignidentaft.asInstanceOf[AnyRef]; 
		case 146 => return this.account3rdrefilldivamountaft.asInstanceOf[AnyRef]; 
		case 147 => return this.account3rdrefilpromodivamntaft.asInstanceOf[AnyRef]; 
		case 148 => return this.account3rdbalanceaft.asInstanceOf[AnyRef]; 
		case 149 => return this.clearedaccount3rdvalueaft.asInstanceOf[AnyRef]; 
		case 150 => return this.dedicatedaccount4thidaft.asInstanceOf[AnyRef]; 
		case 151 => return this.account4thcampaignidentaft.asInstanceOf[AnyRef]; 
		case 152 => return this.account4threfilldivamountaft.asInstanceOf[AnyRef]; 
		case 153 => return this.account4threfilpromodivamntaft.asInstanceOf[AnyRef]; 
		case 154 => return this.account4thbalanceaft.asInstanceOf[AnyRef]; 
		case 155 => return this.clearedaccount4thvalueaft.asInstanceOf[AnyRef]; 
		case 156 => return this.dedicatedaccount5thidaft.asInstanceOf[AnyRef]; 
		case 157 => return this.account5thcampaignidentaft.asInstanceOf[AnyRef]; 
		case 158 => return this.account5threfilldivamountaft.asInstanceOf[AnyRef]; 
		case 159 => return this.account5threfilpromodivamntaft.asInstanceOf[AnyRef]; 
		case 160 => return this.account5thbalanceaft.asInstanceOf[AnyRef]; 
		case 161 => return this.clearedaccount5thvalueaft.asInstanceOf[AnyRef]; 
		case 162 => return this.dedicatedaccount6thidaft.asInstanceOf[AnyRef]; 
		case 163 => return this.account6thcampaignidentaft.asInstanceOf[AnyRef]; 
		case 164 => return this.account6threfilldivamountaft.asInstanceOf[AnyRef]; 
		case 165 => return this.account6threfilpromodivamntaft.asInstanceOf[AnyRef]; 
		case 166 => return this.account6thbalanceaft.asInstanceOf[AnyRef]; 
		case 167 => return this.clearedaccount6thvalueaft.asInstanceOf[AnyRef]; 
		case 168 => return this.dedicatedaccount7thidaft.asInstanceOf[AnyRef]; 
		case 169 => return this.account7thcampaignidentaft.asInstanceOf[AnyRef]; 
		case 170 => return this.account7threfilldivamountaft.asInstanceOf[AnyRef]; 
		case 171 => return this.account7threfilpromodivamntaft.asInstanceOf[AnyRef]; 
		case 172 => return this.account7thbalanceaft.asInstanceOf[AnyRef]; 
		case 173 => return this.clearedaccount7thvalueaft.asInstanceOf[AnyRef]; 
		case 174 => return this.dedicatedaccount8thidaft.asInstanceOf[AnyRef]; 
		case 175 => return this.account8thcampaignidentaft.asInstanceOf[AnyRef]; 
		case 176 => return this.account8threfilldivamountaft.asInstanceOf[AnyRef]; 
		case 177 => return this.account8threfilpromodivamntaft.asInstanceOf[AnyRef]; 
		case 178 => return this.account8thbalanceaft.asInstanceOf[AnyRef]; 
		case 179 => return this.clearedaccount8thvalueaft.asInstanceOf[AnyRef]; 
		case 180 => return this.dedicatedaccount9thidaft.asInstanceOf[AnyRef]; 
		case 181 => return this.account9thcampaignidentaft.asInstanceOf[AnyRef]; 
		case 182 => return this.account9threfilldivamountaft.asInstanceOf[AnyRef]; 
		case 183 => return this.account9threfilpromodivamntaft.asInstanceOf[AnyRef]; 
		case 184 => return this.account9thbalanceaft.asInstanceOf[AnyRef]; 
		case 185 => return this.clearedaccount9thvalueaft.asInstanceOf[AnyRef]; 
		case 186 => return this.dedicatedaccount10thidaft.asInstanceOf[AnyRef]; 
		case 187 => return this.account10thcampaignidentaft.asInstanceOf[AnyRef]; 
		case 188 => return this.account10threfilldivamountaft.asInstanceOf[AnyRef]; 
		case 189 => return this.account10threfilpromdivamntaft.asInstanceOf[AnyRef]; 
		case 190 => return this.account10thbalanceaft.asInstanceOf[AnyRef]; 
		case 191 => return this.clearedaccount10thvalueaft.asInstanceOf[AnyRef]; 
		case 192 => return this.promotionplanaft.asInstanceOf[AnyRef]; 
		case 193 => return this.permanentserviceclassaft.asInstanceOf[AnyRef]; 
		case 194 => return this.temporaryserviceclassaft.asInstanceOf[AnyRef]; 
		case 195 => return this.temporaryservclasexpirydateaft.asInstanceOf[AnyRef]; 
		case 196 => return this.refilloptionaft.asInstanceOf[AnyRef]; 
		case 197 => return this.servicefeeexpirydateaft.asInstanceOf[AnyRef]; 
		case 198 => return this.serviceremovalgraceperiodaft.asInstanceOf[AnyRef]; 
		case 199 => return this.serviceofferingaft.asInstanceOf[AnyRef]; 
		case 200 => return this.supervisionexpirydateaft.asInstanceOf[AnyRef]; 
		case 201 => return this.usageaccumulator1stidaft.asInstanceOf[AnyRef]; 
		case 202 => return this.usageaccumulator1stvalueaft.asInstanceOf[AnyRef]; 
		case 203 => return this.usageaccumulator2ndidaft.asInstanceOf[AnyRef]; 
		case 204 => return this.usageaccumulator2ndvalueaft.asInstanceOf[AnyRef]; 
		case 205 => return this.usageaccumulator3rdidaft.asInstanceOf[AnyRef]; 
		case 206 => return this.usageaccumulator3rdvalueaft.asInstanceOf[AnyRef]; 
		case 207 => return this.usageaccumulator4thidaft.asInstanceOf[AnyRef]; 
		case 208 => return this.usageaccumulator4thvalueaft.asInstanceOf[AnyRef]; 
		case 209 => return this.usageaccumulator5thidaft.asInstanceOf[AnyRef]; 
		case 210 => return this.usageaccumulator5thvalueaft.asInstanceOf[AnyRef]; 
		case 211 => return this.usageaccumulator6thidaft.asInstanceOf[AnyRef]; 
		case 212 => return this.usageaccumulator6thvalueaft.asInstanceOf[AnyRef]; 
		case 213 => return this.usageaccumulator7thidaft.asInstanceOf[AnyRef]; 
		case 214 => return this.usageaccumulator7thvalueaft.asInstanceOf[AnyRef]; 
		case 215 => return this.usageaccumulator8thidaft.asInstanceOf[AnyRef]; 
		case 216 => return this.usageaccumulator8thvalueaft.asInstanceOf[AnyRef]; 
		case 217 => return this.usageaccumulator9thidaft.asInstanceOf[AnyRef]; 
		case 218 => return this.usageaccumulator9thvalueaft.asInstanceOf[AnyRef]; 
		case 219 => return this.usageaccumulator10thidaft.asInstanceOf[AnyRef]; 
		case 220 => return this.usageaccumulator10thvalueaft.asInstanceOf[AnyRef]; 
		case 221 => return this.communityidaft1.asInstanceOf[AnyRef]; 
		case 222 => return this.communityidaft2.asInstanceOf[AnyRef]; 
		case 223 => return this.communityidaft3.asInstanceOf[AnyRef]; 
		case 224 => return this.refillpromodivisionamount.asInstanceOf[AnyRef]; 
		case 225 => return this.supervisiondayspromopart.asInstanceOf[AnyRef]; 
		case 226 => return this.supervisiondayssurplus.asInstanceOf[AnyRef]; 
		case 227 => return this.servicefeedayspromopart.asInstanceOf[AnyRef]; 
		case 228 => return this.servicefeedayssurplus.asInstanceOf[AnyRef]; 
		case 229 => return this.maximumservicefeeperiod.asInstanceOf[AnyRef]; 
		case 230 => return this.maximumsupervisionperiod.asInstanceOf[AnyRef]; 
		case 231 => return this.activationdate.asInstanceOf[AnyRef]; 
		case 232 => return this.welcomestatus.asInstanceOf[AnyRef]; 
		case 233 => return this.voucheragent.asInstanceOf[AnyRef]; 
		case 234 => return this.promotionplanallocstartdate.asInstanceOf[AnyRef]; 
		case 235 => return this.accountgroupid.asInstanceOf[AnyRef]; 
		case 236 => return this.externaldata1.asInstanceOf[AnyRef]; 
		case 237 => return this.externaldata2.asInstanceOf[AnyRef]; 
		case 238 => return this.externaldata3.asInstanceOf[AnyRef]; 
		case 239 => return this.externaldata4.asInstanceOf[AnyRef]; 
		case 240 => return this.locationnumber.asInstanceOf[AnyRef]; 
		case 241 => return this.voucheractivationcode.asInstanceOf[AnyRef]; 
		case 242 => return this.accountcurrencycleared.asInstanceOf[AnyRef]; 
		case 243 => return this.ignoreserviceclasshierarchy.asInstanceOf[AnyRef]; 
		case 244 => return this.accounthomeregion.asInstanceOf[AnyRef]; 
		case 245 => return this.subscriberregion.asInstanceOf[AnyRef]; 
		case 246 => return this.voucherregion.asInstanceOf[AnyRef]; 
		case 247 => return this.promotionplanallocenddate.asInstanceOf[AnyRef]; 
		case 248 => return this.requestedrefilltype.asInstanceOf[AnyRef]; 
		case 249 => return this.accountexpiry1stdatebef.asInstanceOf[AnyRef]; 
		case 250 => return this.accountexpiry1stdateaft.asInstanceOf[AnyRef]; 
		case 251 => return this.accountexpiry2nddatebef.asInstanceOf[AnyRef]; 
		case 252 => return this.accountexpiry2nddateaft.asInstanceOf[AnyRef]; 
		case 253 => return this.accountexpiry3rddatebef.asInstanceOf[AnyRef]; 
		case 254 => return this.accountexpiry3rddateaft.asInstanceOf[AnyRef]; 
		case 255 => return this.accountexpiry4thdatebef.asInstanceOf[AnyRef]; 
		case 256 => return this.accountexpiry4thdateaft.asInstanceOf[AnyRef]; 
		case 257 => return this.accountexpiry5thdatebef.asInstanceOf[AnyRef]; 
		case 258 => return this.accountexpiry5thdateaft.asInstanceOf[AnyRef]; 
		case 259 => return this.accountexpiry6thdatebef.asInstanceOf[AnyRef]; 
		case 260 => return this.accountexpiry6thdateaft.asInstanceOf[AnyRef]; 
		case 261 => return this.accountexpiry7thdatebef.asInstanceOf[AnyRef]; 
		case 262 => return this.accountexpiry7thdateaft.asInstanceOf[AnyRef]; 
		case 263 => return this.accountexpiry8thdatebef.asInstanceOf[AnyRef]; 
		case 264 => return this.accountexpiry8thdateaft.asInstanceOf[AnyRef]; 
		case 265 => return this.accountexpiry9thdatebef.asInstanceOf[AnyRef]; 
		case 266 => return this.accountexpiry9thdateaft.asInstanceOf[AnyRef]; 
		case 267 => return this.accountexpiry10thdatebef.asInstanceOf[AnyRef]; 
		case 268 => return this.accountexpiry10thdateaft.asInstanceOf[AnyRef]; 
		case 269 => return this.rechargedivpartmain.asInstanceOf[AnyRef]; 
		case 270 => return this.rechargedivpartda1st.asInstanceOf[AnyRef]; 
		case 271 => return this.rechargedivpartda2nd.asInstanceOf[AnyRef]; 
		case 272 => return this.rechargedivpartda3rd.asInstanceOf[AnyRef]; 
		case 273 => return this.rechargedivpartda4th.asInstanceOf[AnyRef]; 
		case 274 => return this.rechargedivpartda5th.asInstanceOf[AnyRef]; 
		case 275 => return this.accumulatedprogressionvalueres.asInstanceOf[AnyRef]; 
		case 276 => return this.rechargedivpartpmain.asInstanceOf[AnyRef]; 
		case 277 => return this.rechargedivpartpda1st.asInstanceOf[AnyRef]; 
		case 278 => return this.rechargedivpartpda2nd.asInstanceOf[AnyRef]; 
		case 279 => return this.rechargedivpartpda3rd.asInstanceOf[AnyRef]; 
		case 280 => return this.rechargedivpartpda4th.asInstanceOf[AnyRef]; 
		case 281 => return this.rechargedivpartpda5th.asInstanceOf[AnyRef]; 
		case 282 => return this.rechargedivpartda6th.asInstanceOf[AnyRef]; 
		case 283 => return this.rechargedivpartda7th.asInstanceOf[AnyRef]; 
		case 284 => return this.rechargedivpartda8th.asInstanceOf[AnyRef]; 
		case 285 => return this.rechargedivpartda9th.asInstanceOf[AnyRef]; 
		case 286 => return this.rechargedivpartda10th.asInstanceOf[AnyRef]; 
		case 287 => return this.rechargedivpartpda6th.asInstanceOf[AnyRef]; 
		case 288 => return this.rechargedivpartpda7th.asInstanceOf[AnyRef]; 
		case 289 => return this.rechargedivpartpda8th.asInstanceOf[AnyRef]; 
		case 290 => return this.rechargedivpartpda9th.asInstanceOf[AnyRef]; 
		case 291 => return this.rechargedivpartpda10th.asInstanceOf[AnyRef]; 
		case 292 => return this.dedicatedaccountunit1stbef.asInstanceOf[AnyRef]; 
		case 293 => return this.accountstartdate1stbef.asInstanceOf[AnyRef]; 
		case 294 => return this.refilldivunits1stbef.asInstanceOf[AnyRef]; 
		case 295 => return this.refillpromodivunits1stbef.asInstanceOf[AnyRef]; 
		case 296 => return this.unitbalance1stbef.asInstanceOf[AnyRef]; 
		case 297 => return this.clearedunits1stbef.asInstanceOf[AnyRef]; 
		case 298 => return this.realmoneyflag1stbef.asInstanceOf[AnyRef]; 
		case 299 => return this.dedicatedaccountunit2ndbef.asInstanceOf[AnyRef]; 
		case 300 => return this.accountstartdate2ndbef.asInstanceOf[AnyRef]; 
		case 301 => return this.refilldivunits2ndbef.asInstanceOf[AnyRef]; 
		case 302 => return this.refillpromodivunits2ndbef.asInstanceOf[AnyRef]; 
		case 303 => return this.unitbalance2ndbef.asInstanceOf[AnyRef]; 
		case 304 => return this.clearedunits2ndbef.asInstanceOf[AnyRef]; 
		case 305 => return this.realmoneyflag2ndbef.asInstanceOf[AnyRef]; 
		case 306 => return this.dedicatedaccountunit3rdbef.asInstanceOf[AnyRef]; 
		case 307 => return this.accountstartdate3rdbef.asInstanceOf[AnyRef]; 
		case 308 => return this.refilldivunits3rdbef.asInstanceOf[AnyRef]; 
		case 309 => return this.refillpromodivunits3rdbef.asInstanceOf[AnyRef]; 
		case 310 => return this.unitbalance3rdbef.asInstanceOf[AnyRef]; 
		case 311 => return this.clearedunits3rdbef.asInstanceOf[AnyRef]; 
		case 312 => return this.realmoneyflag3rdbef.asInstanceOf[AnyRef]; 
		case 313 => return this.dedicatedaccountunit4thbef.asInstanceOf[AnyRef]; 
		case 314 => return this.accountstartdate4thbef.asInstanceOf[AnyRef]; 
		case 315 => return this.refilldivunits4thbef.asInstanceOf[AnyRef]; 
		case 316 => return this.refillpromodivunits4thbef.asInstanceOf[AnyRef]; 
		case 317 => return this.unitbalance4thbef.asInstanceOf[AnyRef]; 
		case 318 => return this.clearedunits4thbef.asInstanceOf[AnyRef]; 
		case 319 => return this.realmoneyflag4thbef.asInstanceOf[AnyRef]; 
		case 320 => return this.dedicatedaccountunit5thbef.asInstanceOf[AnyRef]; 
		case 321 => return this.accountstartdate5thbef.asInstanceOf[AnyRef]; 
		case 322 => return this.refilldivunits5thbef.asInstanceOf[AnyRef]; 
		case 323 => return this.refillpromodivunits5thbef.asInstanceOf[AnyRef]; 
		case 324 => return this.unitbalance5thbef.asInstanceOf[AnyRef]; 
		case 325 => return this.clearedunits5thbef.asInstanceOf[AnyRef]; 
		case 326 => return this.realmoneyflag5thbef.asInstanceOf[AnyRef]; 
		case 327 => return this.dedicatedaccountunit6thbef.asInstanceOf[AnyRef]; 
		case 328 => return this.accountstartdate6thbef.asInstanceOf[AnyRef]; 
		case 329 => return this.refilldivunits6thbef.asInstanceOf[AnyRef]; 
		case 330 => return this.refillpromodivunits6thbef.asInstanceOf[AnyRef]; 
		case 331 => return this.unitbalance6thbef.asInstanceOf[AnyRef]; 
		case 332 => return this.clearedunits6thbef.asInstanceOf[AnyRef]; 
		case 333 => return this.realmoneyflag6thbef.asInstanceOf[AnyRef]; 
		case 334 => return this.dedicatedaccountunit7thbef.asInstanceOf[AnyRef]; 
		case 335 => return this.accountstartdate7thbef.asInstanceOf[AnyRef]; 
		case 336 => return this.refilldivunits7thbef.asInstanceOf[AnyRef]; 
		case 337 => return this.refillpromodivunits7thbef.asInstanceOf[AnyRef]; 
		case 338 => return this.unitbalance7thbef.asInstanceOf[AnyRef]; 
		case 339 => return this.clearedunits7thbef.asInstanceOf[AnyRef]; 
		case 340 => return this.realmoneyflag7thbef.asInstanceOf[AnyRef]; 
		case 341 => return this.dedicatedaccountunit8thbef.asInstanceOf[AnyRef]; 
		case 342 => return this.accountstartdate8thbef.asInstanceOf[AnyRef]; 
		case 343 => return this.refilldivunits8thbef.asInstanceOf[AnyRef]; 
		case 344 => return this.refillpromodivunits8thbef.asInstanceOf[AnyRef]; 
		case 345 => return this.unitbalance8thbef.asInstanceOf[AnyRef]; 
		case 346 => return this.clearedunits8thbef.asInstanceOf[AnyRef]; 
		case 347 => return this.realmoneyflag8thbef.asInstanceOf[AnyRef]; 
		case 348 => return this.dedicatedaccountunit9thbef.asInstanceOf[AnyRef]; 
		case 349 => return this.accountstartdate9thbef.asInstanceOf[AnyRef]; 
		case 350 => return this.refilldivunits9thbef.asInstanceOf[AnyRef]; 
		case 351 => return this.refillpromodivunits9thbef.asInstanceOf[AnyRef]; 
		case 352 => return this.unitbalance9thbef.asInstanceOf[AnyRef]; 
		case 353 => return this.clearedunits9thbef.asInstanceOf[AnyRef]; 
		case 354 => return this.realmoneyflag9thbef.asInstanceOf[AnyRef]; 
		case 355 => return this.dedicatedaccountunit10thbef.asInstanceOf[AnyRef]; 
		case 356 => return this.accountstartdate10thbef.asInstanceOf[AnyRef]; 
		case 357 => return this.refilldivunits10thbef.asInstanceOf[AnyRef]; 
		case 358 => return this.refillpromodivunits10thbef.asInstanceOf[AnyRef]; 
		case 359 => return this.unitbalance10thbef.asInstanceOf[AnyRef]; 
		case 360 => return this.clearedunits10thbef.asInstanceOf[AnyRef]; 
		case 361 => return this.realmoneyflag10thbef.asInstanceOf[AnyRef]; 
		case 362 => return this.offer1stidentifierbef.asInstanceOf[AnyRef]; 
		case 363 => return this.offerstartdate1stbef.asInstanceOf[AnyRef]; 
		case 364 => return this.offerexpirydate1stbef.asInstanceOf[AnyRef]; 
		case 365 => return this.offertype1stbef.asInstanceOf[AnyRef]; 
		case 366 => return this.offerproductidentifier1stbef.asInstanceOf[AnyRef]; 
		case 367 => return this.offerstartdatetime1stbef.asInstanceOf[AnyRef]; 
		case 368 => return this.offerexpirydatetime1stbef.asInstanceOf[AnyRef]; 
		case 369 => return this.offer2ndidentifierbef.asInstanceOf[AnyRef]; 
		case 370 => return this.offerstartdate2ndbef.asInstanceOf[AnyRef]; 
		case 371 => return this.offerexpirydate2ndbef.asInstanceOf[AnyRef]; 
		case 372 => return this.offertype2ndbef.asInstanceOf[AnyRef]; 
		case 373 => return this.offerproductidentifier2ndbef.asInstanceOf[AnyRef]; 
		case 374 => return this.offerstartdatetime2ndbef.asInstanceOf[AnyRef]; 
		case 375 => return this.offerexpirydatetime2ndbef.asInstanceOf[AnyRef]; 
		case 376 => return this.offer3rdidentifierbef.asInstanceOf[AnyRef]; 
		case 377 => return this.offerstartdate3rdbef.asInstanceOf[AnyRef]; 
		case 378 => return this.offerexpirydate3rdbef.asInstanceOf[AnyRef]; 
		case 379 => return this.offertype3rdbef.asInstanceOf[AnyRef]; 
		case 380 => return this.offerproductidentifier3rdbef.asInstanceOf[AnyRef]; 
		case 381 => return this.offerstartdatetime3rdbef.asInstanceOf[AnyRef]; 
		case 382 => return this.offerexpirydatetime3rdbef.asInstanceOf[AnyRef]; 
		case 383 => return this.offer4thidentifierbef.asInstanceOf[AnyRef]; 
		case 384 => return this.offerstartdate4thbef.asInstanceOf[AnyRef]; 
		case 385 => return this.offerexpirydate4thbef.asInstanceOf[AnyRef]; 
		case 386 => return this.offertype4thbef.asInstanceOf[AnyRef]; 
		case 387 => return this.offerproductidentifier4thbef.asInstanceOf[AnyRef]; 
		case 388 => return this.offerstartdatetime4thbef.asInstanceOf[AnyRef]; 
		case 389 => return this.offerexpirydatetime4thbef.asInstanceOf[AnyRef]; 
		case 390 => return this.offer5thidentifierbef.asInstanceOf[AnyRef]; 
		case 391 => return this.offerstartdate5thbef.asInstanceOf[AnyRef]; 
		case 392 => return this.offerexpirydate5thbef.asInstanceOf[AnyRef]; 
		case 393 => return this.offertype5thbef.asInstanceOf[AnyRef]; 
		case 394 => return this.offerproductidentifier5thbef.asInstanceOf[AnyRef]; 
		case 395 => return this.offerstartdatetime5thbef.asInstanceOf[AnyRef]; 
		case 396 => return this.offerexpirydatetime5thbef.asInstanceOf[AnyRef]; 
		case 397 => return this.offer6thidentifierbef.asInstanceOf[AnyRef]; 
		case 398 => return this.offerstartdate6thbef.asInstanceOf[AnyRef]; 
		case 399 => return this.offerexpirydate6thbef.asInstanceOf[AnyRef]; 
		case 400 => return this.offertype6thbef.asInstanceOf[AnyRef]; 
		case 401 => return this.offerproductidentifier6thbef.asInstanceOf[AnyRef]; 
		case 402 => return this.offerstartdatetime6thbef.asInstanceOf[AnyRef]; 
		case 403 => return this.offerexpirydatetime6thbef.asInstanceOf[AnyRef]; 
		case 404 => return this.offer7thidentifierbef.asInstanceOf[AnyRef]; 
		case 405 => return this.offerstartdate7thbef.asInstanceOf[AnyRef]; 
		case 406 => return this.offerexpirydate7thbef.asInstanceOf[AnyRef]; 
		case 407 => return this.offertype7thbef.asInstanceOf[AnyRef]; 
		case 408 => return this.offerproductidentifier7thbef.asInstanceOf[AnyRef]; 
		case 409 => return this.offerstartdatetime7thbef.asInstanceOf[AnyRef]; 
		case 410 => return this.offerexpirydatetime7thbef.asInstanceOf[AnyRef]; 
		case 411 => return this.offer8thidentifierbef.asInstanceOf[AnyRef]; 
		case 412 => return this.offerstartdate8thbef.asInstanceOf[AnyRef]; 
		case 413 => return this.offerexpirydate8thbef.asInstanceOf[AnyRef]; 
		case 414 => return this.offertype8thbef.asInstanceOf[AnyRef]; 
		case 415 => return this.offerproductidentifier8thbef.asInstanceOf[AnyRef]; 
		case 416 => return this.offerstartdatetime8thbef.asInstanceOf[AnyRef]; 
		case 417 => return this.offerexpirydatetime8thbef.asInstanceOf[AnyRef]; 
		case 418 => return this.offer9thidentifierbef.asInstanceOf[AnyRef]; 
		case 419 => return this.offerstartdate9thbef.asInstanceOf[AnyRef]; 
		case 420 => return this.offerexpirydate9thbef.asInstanceOf[AnyRef]; 
		case 421 => return this.offertype9thbef.asInstanceOf[AnyRef]; 
		case 422 => return this.offerproductidentifier9thbef.asInstanceOf[AnyRef]; 
		case 423 => return this.offerstartdatetime9thbef.asInstanceOf[AnyRef]; 
		case 424 => return this.offerexpirydatetime9thbef.asInstanceOf[AnyRef]; 
		case 425 => return this.offer10thidentifierbef.asInstanceOf[AnyRef]; 
		case 426 => return this.offerstartdate10thbef.asInstanceOf[AnyRef]; 
		case 427 => return this.offerexpirydate10thbef.asInstanceOf[AnyRef]; 
		case 428 => return this.offertype10thbef.asInstanceOf[AnyRef]; 
		case 429 => return this.offerproductidentifier10thbef.asInstanceOf[AnyRef]; 
		case 430 => return this.offerstartdatetime10thbef.asInstanceOf[AnyRef]; 
		case 431 => return this.offerexpirydatetime10thbef.asInstanceOf[AnyRef]; 
		case 432 => return this.aggregatedbalancebef.asInstanceOf[AnyRef]; 
		case 433 => return this.dedicatedaccountunit1staft.asInstanceOf[AnyRef]; 
		case 434 => return this.accountstartdate1staft.asInstanceOf[AnyRef]; 
		case 435 => return this.refilldivunits1staft.asInstanceOf[AnyRef]; 
		case 436 => return this.refillpromodivunits1staft.asInstanceOf[AnyRef]; 
		case 437 => return this.unitbalance1staft.asInstanceOf[AnyRef]; 
		case 438 => return this.clearedunits1staft.asInstanceOf[AnyRef]; 
		case 439 => return this.realmoneyflag1staft.asInstanceOf[AnyRef]; 
		case 440 => return this.dedicatedaccountunit2ndaft.asInstanceOf[AnyRef]; 
		case 441 => return this.accountstartdate2ndaft.asInstanceOf[AnyRef]; 
		case 442 => return this.refilldivunits2ndaft.asInstanceOf[AnyRef]; 
		case 443 => return this.refillpromodivunits2ndaft.asInstanceOf[AnyRef]; 
		case 444 => return this.unitbalance2ndaft.asInstanceOf[AnyRef]; 
		case 445 => return this.clearedunits2ndaft.asInstanceOf[AnyRef]; 
		case 446 => return this.realmoneyflag2ndaft.asInstanceOf[AnyRef]; 
		case 447 => return this.dedicatedaccountunit3rdaft.asInstanceOf[AnyRef]; 
		case 448 => return this.accountstartdate3rdaft.asInstanceOf[AnyRef]; 
		case 449 => return this.refilldivunits3rdaft.asInstanceOf[AnyRef]; 
		case 450 => return this.refillpromodivunits3rdaft.asInstanceOf[AnyRef]; 
		case 451 => return this.unitbalance3rdaft.asInstanceOf[AnyRef]; 
		case 452 => return this.clearedunits3rdaft.asInstanceOf[AnyRef]; 
		case 453 => return this.realmoneyflag3rdaft.asInstanceOf[AnyRef]; 
		case 454 => return this.dedicatedaccountunit4thaft.asInstanceOf[AnyRef]; 
		case 455 => return this.accountstartdate4thaft.asInstanceOf[AnyRef]; 
		case 456 => return this.refilldivunits4thaft.asInstanceOf[AnyRef]; 
		case 457 => return this.refillpromodivunits4thaft.asInstanceOf[AnyRef]; 
		case 458 => return this.unitbalance4thaft.asInstanceOf[AnyRef]; 
		case 459 => return this.clearedunits4thaft.asInstanceOf[AnyRef]; 
		case 460 => return this.realmoneyflag4thaft.asInstanceOf[AnyRef]; 
		case 461 => return this.dedicatedaccountunit5thaft.asInstanceOf[AnyRef]; 
		case 462 => return this.accountstartdate5thaft.asInstanceOf[AnyRef]; 
		case 463 => return this.refilldivunits5thaft.asInstanceOf[AnyRef]; 
		case 464 => return this.refillpromodivunits5thaft.asInstanceOf[AnyRef]; 
		case 465 => return this.unitbalance5thaft.asInstanceOf[AnyRef]; 
		case 466 => return this.clearedunits5thaft.asInstanceOf[AnyRef]; 
		case 467 => return this.realmoneyflag5thaft.asInstanceOf[AnyRef]; 
		case 468 => return this.dedicatedaccountunit6thaft.asInstanceOf[AnyRef]; 
		case 469 => return this.accountstartdate6thaft.asInstanceOf[AnyRef]; 
		case 470 => return this.refilldivunits6thaft.asInstanceOf[AnyRef]; 
		case 471 => return this.refillpromodivunits6thaft.asInstanceOf[AnyRef]; 
		case 472 => return this.unitbalance6thaft.asInstanceOf[AnyRef]; 
		case 473 => return this.clearedunits6thaft.asInstanceOf[AnyRef]; 
		case 474 => return this.realmoneyflag6thaft.asInstanceOf[AnyRef]; 
		case 475 => return this.dedicatedaccountunit7thaft.asInstanceOf[AnyRef]; 
		case 476 => return this.accountstartdate7thaft.asInstanceOf[AnyRef]; 
		case 477 => return this.refilldivunits7thaft.asInstanceOf[AnyRef]; 
		case 478 => return this.refillpromodivunits7thaft.asInstanceOf[AnyRef]; 
		case 479 => return this.unitbalance7thaft.asInstanceOf[AnyRef]; 
		case 480 => return this.clearedunits7thaft.asInstanceOf[AnyRef]; 
		case 481 => return this.realmoneyflag7thaft.asInstanceOf[AnyRef]; 
		case 482 => return this.dedicatedaccountunit8thaft.asInstanceOf[AnyRef]; 
		case 483 => return this.accountstartdate8thaft.asInstanceOf[AnyRef]; 
		case 484 => return this.refilldivunits8thaft.asInstanceOf[AnyRef]; 
		case 485 => return this.refillpromodivunits8thaft.asInstanceOf[AnyRef]; 
		case 486 => return this.unitbalance8thaft.asInstanceOf[AnyRef]; 
		case 487 => return this.clearedunits8thaft.asInstanceOf[AnyRef]; 
		case 488 => return this.realmoneyflag8thaft.asInstanceOf[AnyRef]; 
		case 489 => return this.dedicatedaccountunit9thaft.asInstanceOf[AnyRef]; 
		case 490 => return this.accountstartdate9thaft.asInstanceOf[AnyRef]; 
		case 491 => return this.refilldivunits9thaft.asInstanceOf[AnyRef]; 
		case 492 => return this.refillpromodivunits9thaft.asInstanceOf[AnyRef]; 
		case 493 => return this.unitbalance9thaft.asInstanceOf[AnyRef]; 
		case 494 => return this.clearedunits9thaft.asInstanceOf[AnyRef]; 
		case 495 => return this.realmoneyflag9thaft.asInstanceOf[AnyRef]; 
		case 496 => return this.dedicatedaccountunit10thaft.asInstanceOf[AnyRef]; 
		case 497 => return this.accountstartdate10thaft.asInstanceOf[AnyRef]; 
		case 498 => return this.refilldivunits10thaft.asInstanceOf[AnyRef]; 
		case 499 => return this.refillpromodivunits10thaft.asInstanceOf[AnyRef]; 
		case 500 => return this.unitbalance10thaft.asInstanceOf[AnyRef]; 
		case 501 => return this.clearedunits10thaft.asInstanceOf[AnyRef]; 
		case 502 => return this.realmoneyflag10thaft.asInstanceOf[AnyRef]; 
		case 503 => return this.offer1stidentifieraft.asInstanceOf[AnyRef]; 
		case 504 => return this.offerstartdate1staft.asInstanceOf[AnyRef]; 
		case 505 => return this.offerexpirydate1staft.asInstanceOf[AnyRef]; 
		case 506 => return this.offertype1staft.asInstanceOf[AnyRef]; 
		case 507 => return this.offerproductidentifier1staft.asInstanceOf[AnyRef]; 
		case 508 => return this.offerstartdatetime1staft.asInstanceOf[AnyRef]; 
		case 509 => return this.offerexpirydatetime1staft.asInstanceOf[AnyRef]; 
		case 510 => return this.offer2ndidentifieraft.asInstanceOf[AnyRef]; 
		case 511 => return this.offerstartdate2ndaft.asInstanceOf[AnyRef]; 
		case 512 => return this.offerexpirydate2ndaft.asInstanceOf[AnyRef]; 
		case 513 => return this.offertype2ndaft.asInstanceOf[AnyRef]; 
		case 514 => return this.offerproductidentifier2ndaft.asInstanceOf[AnyRef]; 
		case 515 => return this.offerstartdatetime2ndaft.asInstanceOf[AnyRef]; 
		case 516 => return this.offerexpirydatetime2ndaft.asInstanceOf[AnyRef]; 
		case 517 => return this.offer3rdidentifieraft.asInstanceOf[AnyRef]; 
		case 518 => return this.offerstartdate3rdaft.asInstanceOf[AnyRef]; 
		case 519 => return this.offerexpirydate3rdaft.asInstanceOf[AnyRef]; 
		case 520 => return this.offertype3rdaft.asInstanceOf[AnyRef]; 
		case 521 => return this.offerproductidentifier3rdaft.asInstanceOf[AnyRef]; 
		case 522 => return this.offerstartdatetime3rdaft.asInstanceOf[AnyRef]; 
		case 523 => return this.offerexpirydatetime3rdaft.asInstanceOf[AnyRef]; 
		case 524 => return this.offer4thidentifieraft.asInstanceOf[AnyRef]; 
		case 525 => return this.offerstartdate4thaft.asInstanceOf[AnyRef]; 
		case 526 => return this.offerexpirydate4thaft.asInstanceOf[AnyRef]; 
		case 527 => return this.offertype4thaft.asInstanceOf[AnyRef]; 
		case 528 => return this.offerproductidentifier4thaft.asInstanceOf[AnyRef]; 
		case 529 => return this.offerstartdatetime4thaft.asInstanceOf[AnyRef]; 
		case 530 => return this.offerexpirydatetime4thaft.asInstanceOf[AnyRef]; 
		case 531 => return this.offer5thidentifieraft.asInstanceOf[AnyRef]; 
		case 532 => return this.offerstartdate5thaft.asInstanceOf[AnyRef]; 
		case 533 => return this.offerexpirydate5thaft.asInstanceOf[AnyRef]; 
		case 534 => return this.offertype5thaft.asInstanceOf[AnyRef]; 
		case 535 => return this.offerproductidentifier5thaft.asInstanceOf[AnyRef]; 
		case 536 => return this.offerstartdatetime5thaft.asInstanceOf[AnyRef]; 
		case 537 => return this.offerexpirydatetime5thaft.asInstanceOf[AnyRef]; 
		case 538 => return this.offer6thidentifieraft.asInstanceOf[AnyRef]; 
		case 539 => return this.offerstartdate6thaft.asInstanceOf[AnyRef]; 
		case 540 => return this.offerexpirydate6thaft.asInstanceOf[AnyRef]; 
		case 541 => return this.offertype6thaft.asInstanceOf[AnyRef]; 
		case 542 => return this.offerproductidentifier6thaft.asInstanceOf[AnyRef]; 
		case 543 => return this.offerstartdatetime6thaft.asInstanceOf[AnyRef]; 
		case 544 => return this.offerexpirydatetime6thaft.asInstanceOf[AnyRef]; 
		case 545 => return this.offer7thidentifieraft.asInstanceOf[AnyRef]; 
		case 546 => return this.offerstartdate7thaft.asInstanceOf[AnyRef]; 
		case 547 => return this.offerexpirydate7thaft.asInstanceOf[AnyRef]; 
		case 548 => return this.offertype7thaft.asInstanceOf[AnyRef]; 
		case 549 => return this.offerproductidentifier7thaft.asInstanceOf[AnyRef]; 
		case 550 => return this.offerstartdatetime7thaft.asInstanceOf[AnyRef]; 
		case 551 => return this.offerexpirydatetime7thaft.asInstanceOf[AnyRef]; 
		case 552 => return this.offer8thidentifieraft.asInstanceOf[AnyRef]; 
		case 553 => return this.offerstartdate8thaft.asInstanceOf[AnyRef]; 
		case 554 => return this.offerexpirydate8thaft.asInstanceOf[AnyRef]; 
		case 555 => return this.offertype8thaft.asInstanceOf[AnyRef]; 
		case 556 => return this.offerproductidentifier8thaft.asInstanceOf[AnyRef]; 
		case 557 => return this.offerstartdatetime8thaft.asInstanceOf[AnyRef]; 
		case 558 => return this.offerexpirydatetime8thaft.asInstanceOf[AnyRef]; 
		case 559 => return this.offer9thidentifieraft.asInstanceOf[AnyRef]; 
		case 560 => return this.offerstartdate9thaft.asInstanceOf[AnyRef]; 
		case 561 => return this.offerexpirydate9thaft.asInstanceOf[AnyRef]; 
		case 562 => return this.offertype9thaft.asInstanceOf[AnyRef]; 
		case 563 => return this.offerproductidentifier9thaft.asInstanceOf[AnyRef]; 
		case 564 => return this.offerstartdatetime9thaft.asInstanceOf[AnyRef]; 
		case 565 => return this.offerexpirydatetime9thaft.asInstanceOf[AnyRef]; 
		case 566 => return this.offer10thidentifieraft.asInstanceOf[AnyRef]; 
		case 567 => return this.offerstartdate10thaft.asInstanceOf[AnyRef]; 
		case 568 => return this.offerexpirydate10thaft.asInstanceOf[AnyRef]; 
		case 569 => return this.offertype10thaft.asInstanceOf[AnyRef]; 
		case 570 => return this.offerproductidentifier10thaft.asInstanceOf[AnyRef]; 
		case 571 => return this.offerstartdatetime10thaft.asInstanceOf[AnyRef]; 
		case 572 => return this.offerexpirydatetime10thaft.asInstanceOf[AnyRef]; 
		case 573 => return this.aggregatedbalanceaft.asInstanceOf[AnyRef]; 
		case 574 => return this.cellidentifier.asInstanceOf[AnyRef]; 
		case 575 => return this.market_id.asInstanceOf[AnyRef]; 
		case 576 => return this.hub_id.asInstanceOf[AnyRef]; 
		case 577 => return this.filename.asInstanceOf[AnyRef]; 

      	 case _ => throw new Exception(s"$index is a bad index for message AirRefillCS5");
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
      var attributeVals = new Array[AttributeValue](578);
      try{
 				attributeVals(0) = new AttributeValue(this.originnodetype, keyTypes("originnodetype")) 
				attributeVals(1) = new AttributeValue(this.originhostname, keyTypes("originhostname")) 
				attributeVals(2) = new AttributeValue(this.originfileid, keyTypes("originfileid")) 
				attributeVals(3) = new AttributeValue(this.origintransactionid, keyTypes("origintransactionid")) 
				attributeVals(4) = new AttributeValue(this.originoperatorid, keyTypes("originoperatorid")) 
				attributeVals(5) = new AttributeValue(this.origintimestamp, keyTypes("origintimestamp")) 
				attributeVals(6) = new AttributeValue(this.hostname, keyTypes("hostname")) 
				attributeVals(7) = new AttributeValue(this.localsequencenumber, keyTypes("localsequencenumber")) 
				attributeVals(8) = new AttributeValue(this.timestamp, keyTypes("timestamp")) 
				attributeVals(9) = new AttributeValue(this.currentserviceclass, keyTypes("currentserviceclass")) 
				attributeVals(10) = new AttributeValue(this.voucherbasedrefill, keyTypes("voucherbasedrefill")) 
				attributeVals(11) = new AttributeValue(this.transactiontype, keyTypes("transactiontype")) 
				attributeVals(12) = new AttributeValue(this.transactioncode, keyTypes("transactioncode")) 
				attributeVals(13) = new AttributeValue(this.transactionamount, keyTypes("transactionamount")) 
				attributeVals(14) = new AttributeValue(this.transactioncurrency, keyTypes("transactioncurrency")) 
				attributeVals(15) = new AttributeValue(this.refillamountconverted, keyTypes("refillamountconverted")) 
				attributeVals(16) = new AttributeValue(this.refilldivisionamount, keyTypes("refilldivisionamount")) 
				attributeVals(17) = new AttributeValue(this.refilltype, keyTypes("refilltype")) 
				attributeVals(18) = new AttributeValue(this.refillprofileid, keyTypes("refillprofileid")) 
				attributeVals(19) = new AttributeValue(this.segmentationid, keyTypes("segmentationid")) 
				attributeVals(20) = new AttributeValue(this.voucherserialnumber, keyTypes("voucherserialnumber")) 
				attributeVals(21) = new AttributeValue(this.vouchergroupid, keyTypes("vouchergroupid")) 
				attributeVals(22) = new AttributeValue(this.accountnumber, keyTypes("accountnumber")) 
				attributeVals(23) = new AttributeValue(this.accountcurrency, keyTypes("accountcurrency")) 
				attributeVals(24) = new AttributeValue(this.subscribernumber, keyTypes("subscribernumber")) 
				attributeVals(25) = new AttributeValue(this.promotionannouncementcode, keyTypes("promotionannouncementcode")) 
				attributeVals(26) = new AttributeValue(this.accountflagsbef, keyTypes("accountflagsbef")) 
				attributeVals(27) = new AttributeValue(this.accountbalancebef, keyTypes("accountbalancebef")) 
				attributeVals(28) = new AttributeValue(this.accumulatedrefillvaluebef, keyTypes("accumulatedrefillvaluebef")) 
				attributeVals(29) = new AttributeValue(this.accumulatedrefillcounterbef, keyTypes("accumulatedrefillcounterbef")) 
				attributeVals(30) = new AttributeValue(this.accumulatedprogressionvaluebef, keyTypes("accumulatedprogressionvaluebef")) 
				attributeVals(31) = new AttributeValue(this.accumulatedprogrcounterbef, keyTypes("accumulatedprogrcounterbef")) 
				attributeVals(32) = new AttributeValue(this.creditclearanceperiodbef, keyTypes("creditclearanceperiodbef")) 
				attributeVals(33) = new AttributeValue(this.dedicatedaccount1stidbef, keyTypes("dedicatedaccount1stidbef")) 
				attributeVals(34) = new AttributeValue(this.account1stcampaignidentbef, keyTypes("account1stcampaignidentbef")) 
				attributeVals(35) = new AttributeValue(this.account1strefilldivamountbef, keyTypes("account1strefilldivamountbef")) 
				attributeVals(36) = new AttributeValue(this.account1strefillpromdivamntbef, keyTypes("account1strefillpromdivamntbef")) 
				attributeVals(37) = new AttributeValue(this.account1stbalancebef, keyTypes("account1stbalancebef")) 
				attributeVals(38) = new AttributeValue(this.clearedaccount1stvaluebef, keyTypes("clearedaccount1stvaluebef")) 
				attributeVals(39) = new AttributeValue(this.dedicatedaccount2ndidbef, keyTypes("dedicatedaccount2ndidbef")) 
				attributeVals(40) = new AttributeValue(this.account2ndcampaignidentifbef, keyTypes("account2ndcampaignidentifbef")) 
				attributeVals(41) = new AttributeValue(this.account2ndrefilldivamountbef, keyTypes("account2ndrefilldivamountbef")) 
				attributeVals(42) = new AttributeValue(this.account2ndrefilpromodivamntbef, keyTypes("account2ndrefilpromodivamntbef")) 
				attributeVals(43) = new AttributeValue(this.account2ndbalancebef, keyTypes("account2ndbalancebef")) 
				attributeVals(44) = new AttributeValue(this.clearedaccount2ndvaluebef, keyTypes("clearedaccount2ndvaluebef")) 
				attributeVals(45) = new AttributeValue(this.dedicatedaccount3rdidbef, keyTypes("dedicatedaccount3rdidbef")) 
				attributeVals(46) = new AttributeValue(this.account3rdcampaignidentbef, keyTypes("account3rdcampaignidentbef")) 
				attributeVals(47) = new AttributeValue(this.account3rdrefilldivamountbef, keyTypes("account3rdrefilldivamountbef")) 
				attributeVals(48) = new AttributeValue(this.account3rdrefilpromodivamntbef, keyTypes("account3rdrefilpromodivamntbef")) 
				attributeVals(49) = new AttributeValue(this.account3rdbalancebef, keyTypes("account3rdbalancebef")) 
				attributeVals(50) = new AttributeValue(this.clearedaccount3rdvaluebef, keyTypes("clearedaccount3rdvaluebef")) 
				attributeVals(51) = new AttributeValue(this.dedicatedaccount4thidbef, keyTypes("dedicatedaccount4thidbef")) 
				attributeVals(52) = new AttributeValue(this.account4thcampaignidentbef, keyTypes("account4thcampaignidentbef")) 
				attributeVals(53) = new AttributeValue(this.account4threfilldivamountbef, keyTypes("account4threfilldivamountbef")) 
				attributeVals(54) = new AttributeValue(this.account4threfilpromodivamntbef, keyTypes("account4threfilpromodivamntbef")) 
				attributeVals(55) = new AttributeValue(this.account4thbalancebef, keyTypes("account4thbalancebef")) 
				attributeVals(56) = new AttributeValue(this.clearedaccount4thvaluebef, keyTypes("clearedaccount4thvaluebef")) 
				attributeVals(57) = new AttributeValue(this.dedicatedaccount5thidbef, keyTypes("dedicatedaccount5thidbef")) 
				attributeVals(58) = new AttributeValue(this.account5thcampaignidentbef, keyTypes("account5thcampaignidentbef")) 
				attributeVals(59) = new AttributeValue(this.account5threfilldivamountbef, keyTypes("account5threfilldivamountbef")) 
				attributeVals(60) = new AttributeValue(this.account5threfilpromodivamntbef, keyTypes("account5threfilpromodivamntbef")) 
				attributeVals(61) = new AttributeValue(this.account5thbalancebef, keyTypes("account5thbalancebef")) 
				attributeVals(62) = new AttributeValue(this.clearedaccount5thvaluebef, keyTypes("clearedaccount5thvaluebef")) 
				attributeVals(63) = new AttributeValue(this.dedicatedaccount6thidbef, keyTypes("dedicatedaccount6thidbef")) 
				attributeVals(64) = new AttributeValue(this.account6thcampaignidentbef, keyTypes("account6thcampaignidentbef")) 
				attributeVals(65) = new AttributeValue(this.account6threfilldivamountbef, keyTypes("account6threfilldivamountbef")) 
				attributeVals(66) = new AttributeValue(this.account6threfilpromodivamntbef, keyTypes("account6threfilpromodivamntbef")) 
				attributeVals(67) = new AttributeValue(this.account6thbalancebef, keyTypes("account6thbalancebef")) 
				attributeVals(68) = new AttributeValue(this.clearedaccount6thvaluebef, keyTypes("clearedaccount6thvaluebef")) 
				attributeVals(69) = new AttributeValue(this.dedicatedaccount7thidbef, keyTypes("dedicatedaccount7thidbef")) 
				attributeVals(70) = new AttributeValue(this.account7thcampaignidentbef, keyTypes("account7thcampaignidentbef")) 
				attributeVals(71) = new AttributeValue(this.account7threfilldivamountbef, keyTypes("account7threfilldivamountbef")) 
				attributeVals(72) = new AttributeValue(this.account7threfilpromodivamntbef, keyTypes("account7threfilpromodivamntbef")) 
				attributeVals(73) = new AttributeValue(this.account7thbalancebef, keyTypes("account7thbalancebef")) 
				attributeVals(74) = new AttributeValue(this.clearedaccount7thvaluebef, keyTypes("clearedaccount7thvaluebef")) 
				attributeVals(75) = new AttributeValue(this.dedicatedaccount8thidbef, keyTypes("dedicatedaccount8thidbef")) 
				attributeVals(76) = new AttributeValue(this.account8thcampaignidentbef, keyTypes("account8thcampaignidentbef")) 
				attributeVals(77) = new AttributeValue(this.account8threfilldivamountbef, keyTypes("account8threfilldivamountbef")) 
				attributeVals(78) = new AttributeValue(this.account8threfilpromodivamntbef, keyTypes("account8threfilpromodivamntbef")) 
				attributeVals(79) = new AttributeValue(this.account8thbalancebef, keyTypes("account8thbalancebef")) 
				attributeVals(80) = new AttributeValue(this.clearedaccount8thvaluebef, keyTypes("clearedaccount8thvaluebef")) 
				attributeVals(81) = new AttributeValue(this.dedicatedaccount9thidbef, keyTypes("dedicatedaccount9thidbef")) 
				attributeVals(82) = new AttributeValue(this.account9thcampaignidentbef, keyTypes("account9thcampaignidentbef")) 
				attributeVals(83) = new AttributeValue(this.account9threfilldivamountbef, keyTypes("account9threfilldivamountbef")) 
				attributeVals(84) = new AttributeValue(this.account9threfilpromodivamntbef, keyTypes("account9threfilpromodivamntbef")) 
				attributeVals(85) = new AttributeValue(this.account9thbalancebef, keyTypes("account9thbalancebef")) 
				attributeVals(86) = new AttributeValue(this.clearedaccount9thvaluebef, keyTypes("clearedaccount9thvaluebef")) 
				attributeVals(87) = new AttributeValue(this.dedicatedaccount10thidbef, keyTypes("dedicatedaccount10thidbef")) 
				attributeVals(88) = new AttributeValue(this.account10thcampaignidentbef, keyTypes("account10thcampaignidentbef")) 
				attributeVals(89) = new AttributeValue(this.account10threfilldivamountbef, keyTypes("account10threfilldivamountbef")) 
				attributeVals(90) = new AttributeValue(this.account10threfilpromdivamntbef, keyTypes("account10threfilpromdivamntbef")) 
				attributeVals(91) = new AttributeValue(this.account10thbalancebef, keyTypes("account10thbalancebef")) 
				attributeVals(92) = new AttributeValue(this.clearedaccount10thvaluebef, keyTypes("clearedaccount10thvaluebef")) 
				attributeVals(93) = new AttributeValue(this.promotionplan, keyTypes("promotionplan")) 
				attributeVals(94) = new AttributeValue(this.permanentserviceclassbef, keyTypes("permanentserviceclassbef")) 
				attributeVals(95) = new AttributeValue(this.temporaryserviceclassbef, keyTypes("temporaryserviceclassbef")) 
				attributeVals(96) = new AttributeValue(this.temporaryservclassexpdatebef, keyTypes("temporaryservclassexpdatebef")) 
				attributeVals(97) = new AttributeValue(this.refilloptionbef, keyTypes("refilloptionbef")) 
				attributeVals(98) = new AttributeValue(this.servicefeeexpirydatebef, keyTypes("servicefeeexpirydatebef")) 
				attributeVals(99) = new AttributeValue(this.serviceremovalgraceperiodbef, keyTypes("serviceremovalgraceperiodbef")) 
				attributeVals(100) = new AttributeValue(this.serviceofferingbef, keyTypes("serviceofferingbef")) 
				attributeVals(101) = new AttributeValue(this.supervisionexpirydatebef, keyTypes("supervisionexpirydatebef")) 
				attributeVals(102) = new AttributeValue(this.usageaccumulator1stidbef, keyTypes("usageaccumulator1stidbef")) 
				attributeVals(103) = new AttributeValue(this.usageaccumulator1stvaluebef, keyTypes("usageaccumulator1stvaluebef")) 
				attributeVals(104) = new AttributeValue(this.usageaccumulator2ndidbef, keyTypes("usageaccumulator2ndidbef")) 
				attributeVals(105) = new AttributeValue(this.usageaccumulator2ndvaluebef, keyTypes("usageaccumulator2ndvaluebef")) 
				attributeVals(106) = new AttributeValue(this.usageaccumulator3rdidbef, keyTypes("usageaccumulator3rdidbef")) 
				attributeVals(107) = new AttributeValue(this.usageaccumulator3rdvaluebef, keyTypes("usageaccumulator3rdvaluebef")) 
				attributeVals(108) = new AttributeValue(this.usageaccumulator4thidbef, keyTypes("usageaccumulator4thidbef")) 
				attributeVals(109) = new AttributeValue(this.usageaccumulator4thvaluebef, keyTypes("usageaccumulator4thvaluebef")) 
				attributeVals(110) = new AttributeValue(this.usageaccumulator5thidbef, keyTypes("usageaccumulator5thidbef")) 
				attributeVals(111) = new AttributeValue(this.usageaccumulator5thvaluebef, keyTypes("usageaccumulator5thvaluebef")) 
				attributeVals(112) = new AttributeValue(this.usageaccumulator6thidbef, keyTypes("usageaccumulator6thidbef")) 
				attributeVals(113) = new AttributeValue(this.usageaccumulator6thvaluebef, keyTypes("usageaccumulator6thvaluebef")) 
				attributeVals(114) = new AttributeValue(this.usageaccumulator7thidbef, keyTypes("usageaccumulator7thidbef")) 
				attributeVals(115) = new AttributeValue(this.usageaccumulator7thvaluebef, keyTypes("usageaccumulator7thvaluebef")) 
				attributeVals(116) = new AttributeValue(this.usageaccumulator8thidbef, keyTypes("usageaccumulator8thidbef")) 
				attributeVals(117) = new AttributeValue(this.usageaccumulator8thvaluebef, keyTypes("usageaccumulator8thvaluebef")) 
				attributeVals(118) = new AttributeValue(this.usageaccumulator9thidbef, keyTypes("usageaccumulator9thidbef")) 
				attributeVals(119) = new AttributeValue(this.usageaccumulator9thvaluebef, keyTypes("usageaccumulator9thvaluebef")) 
				attributeVals(120) = new AttributeValue(this.usageaccumulator10thidbef, keyTypes("usageaccumulator10thidbef")) 
				attributeVals(121) = new AttributeValue(this.usageaccumulator10thvaluebef, keyTypes("usageaccumulator10thvaluebef")) 
				attributeVals(122) = new AttributeValue(this.communityidbef1, keyTypes("communityidbef1")) 
				attributeVals(123) = new AttributeValue(this.communityidbef2, keyTypes("communityidbef2")) 
				attributeVals(124) = new AttributeValue(this.communityidbef3, keyTypes("communityidbef3")) 
				attributeVals(125) = new AttributeValue(this.accountflagsaft, keyTypes("accountflagsaft")) 
				attributeVals(126) = new AttributeValue(this.accountbalanceaft, keyTypes("accountbalanceaft")) 
				attributeVals(127) = new AttributeValue(this.accumulatedrefillvalueaft, keyTypes("accumulatedrefillvalueaft")) 
				attributeVals(128) = new AttributeValue(this.accumulatedrefillcounteraft, keyTypes("accumulatedrefillcounteraft")) 
				attributeVals(129) = new AttributeValue(this.accumulatedprogressionvalueaft, keyTypes("accumulatedprogressionvalueaft")) 
				attributeVals(130) = new AttributeValue(this.accumulatedprogconteraft, keyTypes("accumulatedprogconteraft")) 
				attributeVals(131) = new AttributeValue(this.creditclearanceperiodaft, keyTypes("creditclearanceperiodaft")) 
				attributeVals(132) = new AttributeValue(this.dedicatedaccount1stidaft, keyTypes("dedicatedaccount1stidaft")) 
				attributeVals(133) = new AttributeValue(this.account1stcampaignidentaft, keyTypes("account1stcampaignidentaft")) 
				attributeVals(134) = new AttributeValue(this.account1strefilldivamountaft, keyTypes("account1strefilldivamountaft")) 
				attributeVals(135) = new AttributeValue(this.account1strefilpromodivamntaft, keyTypes("account1strefilpromodivamntaft")) 
				attributeVals(136) = new AttributeValue(this.account1stbalanceaft, keyTypes("account1stbalanceaft")) 
				attributeVals(137) = new AttributeValue(this.clearedaccount1stvalueaft, keyTypes("clearedaccount1stvalueaft")) 
				attributeVals(138) = new AttributeValue(this.dedicatedaccount2ndidaft, keyTypes("dedicatedaccount2ndidaft")) 
				attributeVals(139) = new AttributeValue(this.account2ndcampaignidentaft, keyTypes("account2ndcampaignidentaft")) 
				attributeVals(140) = new AttributeValue(this.account2ndrefilldivamountaft, keyTypes("account2ndrefilldivamountaft")) 
				attributeVals(141) = new AttributeValue(this.account2ndrefilpromodivamntaft, keyTypes("account2ndrefilpromodivamntaft")) 
				attributeVals(142) = new AttributeValue(this.account2ndbalanceaft, keyTypes("account2ndbalanceaft")) 
				attributeVals(143) = new AttributeValue(this.clearedaccount2ndvalueaft, keyTypes("clearedaccount2ndvalueaft")) 
				attributeVals(144) = new AttributeValue(this.dedicatedaccount3rdidaft, keyTypes("dedicatedaccount3rdidaft")) 
				attributeVals(145) = new AttributeValue(this.account3rdcampaignidentaft, keyTypes("account3rdcampaignidentaft")) 
				attributeVals(146) = new AttributeValue(this.account3rdrefilldivamountaft, keyTypes("account3rdrefilldivamountaft")) 
				attributeVals(147) = new AttributeValue(this.account3rdrefilpromodivamntaft, keyTypes("account3rdrefilpromodivamntaft")) 
				attributeVals(148) = new AttributeValue(this.account3rdbalanceaft, keyTypes("account3rdbalanceaft")) 
				attributeVals(149) = new AttributeValue(this.clearedaccount3rdvalueaft, keyTypes("clearedaccount3rdvalueaft")) 
				attributeVals(150) = new AttributeValue(this.dedicatedaccount4thidaft, keyTypes("dedicatedaccount4thidaft")) 
				attributeVals(151) = new AttributeValue(this.account4thcampaignidentaft, keyTypes("account4thcampaignidentaft")) 
				attributeVals(152) = new AttributeValue(this.account4threfilldivamountaft, keyTypes("account4threfilldivamountaft")) 
				attributeVals(153) = new AttributeValue(this.account4threfilpromodivamntaft, keyTypes("account4threfilpromodivamntaft")) 
				attributeVals(154) = new AttributeValue(this.account4thbalanceaft, keyTypes("account4thbalanceaft")) 
				attributeVals(155) = new AttributeValue(this.clearedaccount4thvalueaft, keyTypes("clearedaccount4thvalueaft")) 
				attributeVals(156) = new AttributeValue(this.dedicatedaccount5thidaft, keyTypes("dedicatedaccount5thidaft")) 
				attributeVals(157) = new AttributeValue(this.account5thcampaignidentaft, keyTypes("account5thcampaignidentaft")) 
				attributeVals(158) = new AttributeValue(this.account5threfilldivamountaft, keyTypes("account5threfilldivamountaft")) 
				attributeVals(159) = new AttributeValue(this.account5threfilpromodivamntaft, keyTypes("account5threfilpromodivamntaft")) 
				attributeVals(160) = new AttributeValue(this.account5thbalanceaft, keyTypes("account5thbalanceaft")) 
				attributeVals(161) = new AttributeValue(this.clearedaccount5thvalueaft, keyTypes("clearedaccount5thvalueaft")) 
				attributeVals(162) = new AttributeValue(this.dedicatedaccount6thidaft, keyTypes("dedicatedaccount6thidaft")) 
				attributeVals(163) = new AttributeValue(this.account6thcampaignidentaft, keyTypes("account6thcampaignidentaft")) 
				attributeVals(164) = new AttributeValue(this.account6threfilldivamountaft, keyTypes("account6threfilldivamountaft")) 
				attributeVals(165) = new AttributeValue(this.account6threfilpromodivamntaft, keyTypes("account6threfilpromodivamntaft")) 
				attributeVals(166) = new AttributeValue(this.account6thbalanceaft, keyTypes("account6thbalanceaft")) 
				attributeVals(167) = new AttributeValue(this.clearedaccount6thvalueaft, keyTypes("clearedaccount6thvalueaft")) 
				attributeVals(168) = new AttributeValue(this.dedicatedaccount7thidaft, keyTypes("dedicatedaccount7thidaft")) 
				attributeVals(169) = new AttributeValue(this.account7thcampaignidentaft, keyTypes("account7thcampaignidentaft")) 
				attributeVals(170) = new AttributeValue(this.account7threfilldivamountaft, keyTypes("account7threfilldivamountaft")) 
				attributeVals(171) = new AttributeValue(this.account7threfilpromodivamntaft, keyTypes("account7threfilpromodivamntaft")) 
				attributeVals(172) = new AttributeValue(this.account7thbalanceaft, keyTypes("account7thbalanceaft")) 
				attributeVals(173) = new AttributeValue(this.clearedaccount7thvalueaft, keyTypes("clearedaccount7thvalueaft")) 
				attributeVals(174) = new AttributeValue(this.dedicatedaccount8thidaft, keyTypes("dedicatedaccount8thidaft")) 
				attributeVals(175) = new AttributeValue(this.account8thcampaignidentaft, keyTypes("account8thcampaignidentaft")) 
				attributeVals(176) = new AttributeValue(this.account8threfilldivamountaft, keyTypes("account8threfilldivamountaft")) 
				attributeVals(177) = new AttributeValue(this.account8threfilpromodivamntaft, keyTypes("account8threfilpromodivamntaft")) 
				attributeVals(178) = new AttributeValue(this.account8thbalanceaft, keyTypes("account8thbalanceaft")) 
				attributeVals(179) = new AttributeValue(this.clearedaccount8thvalueaft, keyTypes("clearedaccount8thvalueaft")) 
				attributeVals(180) = new AttributeValue(this.dedicatedaccount9thidaft, keyTypes("dedicatedaccount9thidaft")) 
				attributeVals(181) = new AttributeValue(this.account9thcampaignidentaft, keyTypes("account9thcampaignidentaft")) 
				attributeVals(182) = new AttributeValue(this.account9threfilldivamountaft, keyTypes("account9threfilldivamountaft")) 
				attributeVals(183) = new AttributeValue(this.account9threfilpromodivamntaft, keyTypes("account9threfilpromodivamntaft")) 
				attributeVals(184) = new AttributeValue(this.account9thbalanceaft, keyTypes("account9thbalanceaft")) 
				attributeVals(185) = new AttributeValue(this.clearedaccount9thvalueaft, keyTypes("clearedaccount9thvalueaft")) 
				attributeVals(186) = new AttributeValue(this.dedicatedaccount10thidaft, keyTypes("dedicatedaccount10thidaft")) 
				attributeVals(187) = new AttributeValue(this.account10thcampaignidentaft, keyTypes("account10thcampaignidentaft")) 
				attributeVals(188) = new AttributeValue(this.account10threfilldivamountaft, keyTypes("account10threfilldivamountaft")) 
				attributeVals(189) = new AttributeValue(this.account10threfilpromdivamntaft, keyTypes("account10threfilpromdivamntaft")) 
				attributeVals(190) = new AttributeValue(this.account10thbalanceaft, keyTypes("account10thbalanceaft")) 
				attributeVals(191) = new AttributeValue(this.clearedaccount10thvalueaft, keyTypes("clearedaccount10thvalueaft")) 
				attributeVals(192) = new AttributeValue(this.promotionplanaft, keyTypes("promotionplanaft")) 
				attributeVals(193) = new AttributeValue(this.permanentserviceclassaft, keyTypes("permanentserviceclassaft")) 
				attributeVals(194) = new AttributeValue(this.temporaryserviceclassaft, keyTypes("temporaryserviceclassaft")) 
				attributeVals(195) = new AttributeValue(this.temporaryservclasexpirydateaft, keyTypes("temporaryservclasexpirydateaft")) 
				attributeVals(196) = new AttributeValue(this.refilloptionaft, keyTypes("refilloptionaft")) 
				attributeVals(197) = new AttributeValue(this.servicefeeexpirydateaft, keyTypes("servicefeeexpirydateaft")) 
				attributeVals(198) = new AttributeValue(this.serviceremovalgraceperiodaft, keyTypes("serviceremovalgraceperiodaft")) 
				attributeVals(199) = new AttributeValue(this.serviceofferingaft, keyTypes("serviceofferingaft")) 
				attributeVals(200) = new AttributeValue(this.supervisionexpirydateaft, keyTypes("supervisionexpirydateaft")) 
				attributeVals(201) = new AttributeValue(this.usageaccumulator1stidaft, keyTypes("usageaccumulator1stidaft")) 
				attributeVals(202) = new AttributeValue(this.usageaccumulator1stvalueaft, keyTypes("usageaccumulator1stvalueaft")) 
				attributeVals(203) = new AttributeValue(this.usageaccumulator2ndidaft, keyTypes("usageaccumulator2ndidaft")) 
				attributeVals(204) = new AttributeValue(this.usageaccumulator2ndvalueaft, keyTypes("usageaccumulator2ndvalueaft")) 
				attributeVals(205) = new AttributeValue(this.usageaccumulator3rdidaft, keyTypes("usageaccumulator3rdidaft")) 
				attributeVals(206) = new AttributeValue(this.usageaccumulator3rdvalueaft, keyTypes("usageaccumulator3rdvalueaft")) 
				attributeVals(207) = new AttributeValue(this.usageaccumulator4thidaft, keyTypes("usageaccumulator4thidaft")) 
				attributeVals(208) = new AttributeValue(this.usageaccumulator4thvalueaft, keyTypes("usageaccumulator4thvalueaft")) 
				attributeVals(209) = new AttributeValue(this.usageaccumulator5thidaft, keyTypes("usageaccumulator5thidaft")) 
				attributeVals(210) = new AttributeValue(this.usageaccumulator5thvalueaft, keyTypes("usageaccumulator5thvalueaft")) 
				attributeVals(211) = new AttributeValue(this.usageaccumulator6thidaft, keyTypes("usageaccumulator6thidaft")) 
				attributeVals(212) = new AttributeValue(this.usageaccumulator6thvalueaft, keyTypes("usageaccumulator6thvalueaft")) 
				attributeVals(213) = new AttributeValue(this.usageaccumulator7thidaft, keyTypes("usageaccumulator7thidaft")) 
				attributeVals(214) = new AttributeValue(this.usageaccumulator7thvalueaft, keyTypes("usageaccumulator7thvalueaft")) 
				attributeVals(215) = new AttributeValue(this.usageaccumulator8thidaft, keyTypes("usageaccumulator8thidaft")) 
				attributeVals(216) = new AttributeValue(this.usageaccumulator8thvalueaft, keyTypes("usageaccumulator8thvalueaft")) 
				attributeVals(217) = new AttributeValue(this.usageaccumulator9thidaft, keyTypes("usageaccumulator9thidaft")) 
				attributeVals(218) = new AttributeValue(this.usageaccumulator9thvalueaft, keyTypes("usageaccumulator9thvalueaft")) 
				attributeVals(219) = new AttributeValue(this.usageaccumulator10thidaft, keyTypes("usageaccumulator10thidaft")) 
				attributeVals(220) = new AttributeValue(this.usageaccumulator10thvalueaft, keyTypes("usageaccumulator10thvalueaft")) 
				attributeVals(221) = new AttributeValue(this.communityidaft1, keyTypes("communityidaft1")) 
				attributeVals(222) = new AttributeValue(this.communityidaft2, keyTypes("communityidaft2")) 
				attributeVals(223) = new AttributeValue(this.communityidaft3, keyTypes("communityidaft3")) 
				attributeVals(224) = new AttributeValue(this.refillpromodivisionamount, keyTypes("refillpromodivisionamount")) 
				attributeVals(225) = new AttributeValue(this.supervisiondayspromopart, keyTypes("supervisiondayspromopart")) 
				attributeVals(226) = new AttributeValue(this.supervisiondayssurplus, keyTypes("supervisiondayssurplus")) 
				attributeVals(227) = new AttributeValue(this.servicefeedayspromopart, keyTypes("servicefeedayspromopart")) 
				attributeVals(228) = new AttributeValue(this.servicefeedayssurplus, keyTypes("servicefeedayssurplus")) 
				attributeVals(229) = new AttributeValue(this.maximumservicefeeperiod, keyTypes("maximumservicefeeperiod")) 
				attributeVals(230) = new AttributeValue(this.maximumsupervisionperiod, keyTypes("maximumsupervisionperiod")) 
				attributeVals(231) = new AttributeValue(this.activationdate, keyTypes("activationdate")) 
				attributeVals(232) = new AttributeValue(this.welcomestatus, keyTypes("welcomestatus")) 
				attributeVals(233) = new AttributeValue(this.voucheragent, keyTypes("voucheragent")) 
				attributeVals(234) = new AttributeValue(this.promotionplanallocstartdate, keyTypes("promotionplanallocstartdate")) 
				attributeVals(235) = new AttributeValue(this.accountgroupid, keyTypes("accountgroupid")) 
				attributeVals(236) = new AttributeValue(this.externaldata1, keyTypes("externaldata1")) 
				attributeVals(237) = new AttributeValue(this.externaldata2, keyTypes("externaldata2")) 
				attributeVals(238) = new AttributeValue(this.externaldata3, keyTypes("externaldata3")) 
				attributeVals(239) = new AttributeValue(this.externaldata4, keyTypes("externaldata4")) 
				attributeVals(240) = new AttributeValue(this.locationnumber, keyTypes("locationnumber")) 
				attributeVals(241) = new AttributeValue(this.voucheractivationcode, keyTypes("voucheractivationcode")) 
				attributeVals(242) = new AttributeValue(this.accountcurrencycleared, keyTypes("accountcurrencycleared")) 
				attributeVals(243) = new AttributeValue(this.ignoreserviceclasshierarchy, keyTypes("ignoreserviceclasshierarchy")) 
				attributeVals(244) = new AttributeValue(this.accounthomeregion, keyTypes("accounthomeregion")) 
				attributeVals(245) = new AttributeValue(this.subscriberregion, keyTypes("subscriberregion")) 
				attributeVals(246) = new AttributeValue(this.voucherregion, keyTypes("voucherregion")) 
				attributeVals(247) = new AttributeValue(this.promotionplanallocenddate, keyTypes("promotionplanallocenddate")) 
				attributeVals(248) = new AttributeValue(this.requestedrefilltype, keyTypes("requestedrefilltype")) 
				attributeVals(249) = new AttributeValue(this.accountexpiry1stdatebef, keyTypes("accountexpiry1stdatebef")) 
				attributeVals(250) = new AttributeValue(this.accountexpiry1stdateaft, keyTypes("accountexpiry1stdateaft")) 
				attributeVals(251) = new AttributeValue(this.accountexpiry2nddatebef, keyTypes("accountexpiry2nddatebef")) 
				attributeVals(252) = new AttributeValue(this.accountexpiry2nddateaft, keyTypes("accountexpiry2nddateaft")) 
				attributeVals(253) = new AttributeValue(this.accountexpiry3rddatebef, keyTypes("accountexpiry3rddatebef")) 
				attributeVals(254) = new AttributeValue(this.accountexpiry3rddateaft, keyTypes("accountexpiry3rddateaft")) 
				attributeVals(255) = new AttributeValue(this.accountexpiry4thdatebef, keyTypes("accountexpiry4thdatebef")) 
				attributeVals(256) = new AttributeValue(this.accountexpiry4thdateaft, keyTypes("accountexpiry4thdateaft")) 
				attributeVals(257) = new AttributeValue(this.accountexpiry5thdatebef, keyTypes("accountexpiry5thdatebef")) 
				attributeVals(258) = new AttributeValue(this.accountexpiry5thdateaft, keyTypes("accountexpiry5thdateaft")) 
				attributeVals(259) = new AttributeValue(this.accountexpiry6thdatebef, keyTypes("accountexpiry6thdatebef")) 
				attributeVals(260) = new AttributeValue(this.accountexpiry6thdateaft, keyTypes("accountexpiry6thdateaft")) 
				attributeVals(261) = new AttributeValue(this.accountexpiry7thdatebef, keyTypes("accountexpiry7thdatebef")) 
				attributeVals(262) = new AttributeValue(this.accountexpiry7thdateaft, keyTypes("accountexpiry7thdateaft")) 
				attributeVals(263) = new AttributeValue(this.accountexpiry8thdatebef, keyTypes("accountexpiry8thdatebef")) 
				attributeVals(264) = new AttributeValue(this.accountexpiry8thdateaft, keyTypes("accountexpiry8thdateaft")) 
				attributeVals(265) = new AttributeValue(this.accountexpiry9thdatebef, keyTypes("accountexpiry9thdatebef")) 
				attributeVals(266) = new AttributeValue(this.accountexpiry9thdateaft, keyTypes("accountexpiry9thdateaft")) 
				attributeVals(267) = new AttributeValue(this.accountexpiry10thdatebef, keyTypes("accountexpiry10thdatebef")) 
				attributeVals(268) = new AttributeValue(this.accountexpiry10thdateaft, keyTypes("accountexpiry10thdateaft")) 
				attributeVals(269) = new AttributeValue(this.rechargedivpartmain, keyTypes("rechargedivpartmain")) 
				attributeVals(270) = new AttributeValue(this.rechargedivpartda1st, keyTypes("rechargedivpartda1st")) 
				attributeVals(271) = new AttributeValue(this.rechargedivpartda2nd, keyTypes("rechargedivpartda2nd")) 
				attributeVals(272) = new AttributeValue(this.rechargedivpartda3rd, keyTypes("rechargedivpartda3rd")) 
				attributeVals(273) = new AttributeValue(this.rechargedivpartda4th, keyTypes("rechargedivpartda4th")) 
				attributeVals(274) = new AttributeValue(this.rechargedivpartda5th, keyTypes("rechargedivpartda5th")) 
				attributeVals(275) = new AttributeValue(this.accumulatedprogressionvalueres, keyTypes("accumulatedprogressionvalueres")) 
				attributeVals(276) = new AttributeValue(this.rechargedivpartpmain, keyTypes("rechargedivpartpmain")) 
				attributeVals(277) = new AttributeValue(this.rechargedivpartpda1st, keyTypes("rechargedivpartpda1st")) 
				attributeVals(278) = new AttributeValue(this.rechargedivpartpda2nd, keyTypes("rechargedivpartpda2nd")) 
				attributeVals(279) = new AttributeValue(this.rechargedivpartpda3rd, keyTypes("rechargedivpartpda3rd")) 
				attributeVals(280) = new AttributeValue(this.rechargedivpartpda4th, keyTypes("rechargedivpartpda4th")) 
				attributeVals(281) = new AttributeValue(this.rechargedivpartpda5th, keyTypes("rechargedivpartpda5th")) 
				attributeVals(282) = new AttributeValue(this.rechargedivpartda6th, keyTypes("rechargedivpartda6th")) 
				attributeVals(283) = new AttributeValue(this.rechargedivpartda7th, keyTypes("rechargedivpartda7th")) 
				attributeVals(284) = new AttributeValue(this.rechargedivpartda8th, keyTypes("rechargedivpartda8th")) 
				attributeVals(285) = new AttributeValue(this.rechargedivpartda9th, keyTypes("rechargedivpartda9th")) 
				attributeVals(286) = new AttributeValue(this.rechargedivpartda10th, keyTypes("rechargedivpartda10th")) 
				attributeVals(287) = new AttributeValue(this.rechargedivpartpda6th, keyTypes("rechargedivpartpda6th")) 
				attributeVals(288) = new AttributeValue(this.rechargedivpartpda7th, keyTypes("rechargedivpartpda7th")) 
				attributeVals(289) = new AttributeValue(this.rechargedivpartpda8th, keyTypes("rechargedivpartpda8th")) 
				attributeVals(290) = new AttributeValue(this.rechargedivpartpda9th, keyTypes("rechargedivpartpda9th")) 
				attributeVals(291) = new AttributeValue(this.rechargedivpartpda10th, keyTypes("rechargedivpartpda10th")) 
				attributeVals(292) = new AttributeValue(this.dedicatedaccountunit1stbef, keyTypes("dedicatedaccountunit1stbef")) 
				attributeVals(293) = new AttributeValue(this.accountstartdate1stbef, keyTypes("accountstartdate1stbef")) 
				attributeVals(294) = new AttributeValue(this.refilldivunits1stbef, keyTypes("refilldivunits1stbef")) 
				attributeVals(295) = new AttributeValue(this.refillpromodivunits1stbef, keyTypes("refillpromodivunits1stbef")) 
				attributeVals(296) = new AttributeValue(this.unitbalance1stbef, keyTypes("unitbalance1stbef")) 
				attributeVals(297) = new AttributeValue(this.clearedunits1stbef, keyTypes("clearedunits1stbef")) 
				attributeVals(298) = new AttributeValue(this.realmoneyflag1stbef, keyTypes("realmoneyflag1stbef")) 
				attributeVals(299) = new AttributeValue(this.dedicatedaccountunit2ndbef, keyTypes("dedicatedaccountunit2ndbef")) 
				attributeVals(300) = new AttributeValue(this.accountstartdate2ndbef, keyTypes("accountstartdate2ndbef")) 
				attributeVals(301) = new AttributeValue(this.refilldivunits2ndbef, keyTypes("refilldivunits2ndbef")) 
				attributeVals(302) = new AttributeValue(this.refillpromodivunits2ndbef, keyTypes("refillpromodivunits2ndbef")) 
				attributeVals(303) = new AttributeValue(this.unitbalance2ndbef, keyTypes("unitbalance2ndbef")) 
				attributeVals(304) = new AttributeValue(this.clearedunits2ndbef, keyTypes("clearedunits2ndbef")) 
				attributeVals(305) = new AttributeValue(this.realmoneyflag2ndbef, keyTypes("realmoneyflag2ndbef")) 
				attributeVals(306) = new AttributeValue(this.dedicatedaccountunit3rdbef, keyTypes("dedicatedaccountunit3rdbef")) 
				attributeVals(307) = new AttributeValue(this.accountstartdate3rdbef, keyTypes("accountstartdate3rdbef")) 
				attributeVals(308) = new AttributeValue(this.refilldivunits3rdbef, keyTypes("refilldivunits3rdbef")) 
				attributeVals(309) = new AttributeValue(this.refillpromodivunits3rdbef, keyTypes("refillpromodivunits3rdbef")) 
				attributeVals(310) = new AttributeValue(this.unitbalance3rdbef, keyTypes("unitbalance3rdbef")) 
				attributeVals(311) = new AttributeValue(this.clearedunits3rdbef, keyTypes("clearedunits3rdbef")) 
				attributeVals(312) = new AttributeValue(this.realmoneyflag3rdbef, keyTypes("realmoneyflag3rdbef")) 
				attributeVals(313) = new AttributeValue(this.dedicatedaccountunit4thbef, keyTypes("dedicatedaccountunit4thbef")) 
				attributeVals(314) = new AttributeValue(this.accountstartdate4thbef, keyTypes("accountstartdate4thbef")) 
				attributeVals(315) = new AttributeValue(this.refilldivunits4thbef, keyTypes("refilldivunits4thbef")) 
				attributeVals(316) = new AttributeValue(this.refillpromodivunits4thbef, keyTypes("refillpromodivunits4thbef")) 
				attributeVals(317) = new AttributeValue(this.unitbalance4thbef, keyTypes("unitbalance4thbef")) 
				attributeVals(318) = new AttributeValue(this.clearedunits4thbef, keyTypes("clearedunits4thbef")) 
				attributeVals(319) = new AttributeValue(this.realmoneyflag4thbef, keyTypes("realmoneyflag4thbef")) 
				attributeVals(320) = new AttributeValue(this.dedicatedaccountunit5thbef, keyTypes("dedicatedaccountunit5thbef")) 
				attributeVals(321) = new AttributeValue(this.accountstartdate5thbef, keyTypes("accountstartdate5thbef")) 
				attributeVals(322) = new AttributeValue(this.refilldivunits5thbef, keyTypes("refilldivunits5thbef")) 
				attributeVals(323) = new AttributeValue(this.refillpromodivunits5thbef, keyTypes("refillpromodivunits5thbef")) 
				attributeVals(324) = new AttributeValue(this.unitbalance5thbef, keyTypes("unitbalance5thbef")) 
				attributeVals(325) = new AttributeValue(this.clearedunits5thbef, keyTypes("clearedunits5thbef")) 
				attributeVals(326) = new AttributeValue(this.realmoneyflag5thbef, keyTypes("realmoneyflag5thbef")) 
				attributeVals(327) = new AttributeValue(this.dedicatedaccountunit6thbef, keyTypes("dedicatedaccountunit6thbef")) 
				attributeVals(328) = new AttributeValue(this.accountstartdate6thbef, keyTypes("accountstartdate6thbef")) 
				attributeVals(329) = new AttributeValue(this.refilldivunits6thbef, keyTypes("refilldivunits6thbef")) 
				attributeVals(330) = new AttributeValue(this.refillpromodivunits6thbef, keyTypes("refillpromodivunits6thbef")) 
				attributeVals(331) = new AttributeValue(this.unitbalance6thbef, keyTypes("unitbalance6thbef")) 
				attributeVals(332) = new AttributeValue(this.clearedunits6thbef, keyTypes("clearedunits6thbef")) 
				attributeVals(333) = new AttributeValue(this.realmoneyflag6thbef, keyTypes("realmoneyflag6thbef")) 
				attributeVals(334) = new AttributeValue(this.dedicatedaccountunit7thbef, keyTypes("dedicatedaccountunit7thbef")) 
				attributeVals(335) = new AttributeValue(this.accountstartdate7thbef, keyTypes("accountstartdate7thbef")) 
				attributeVals(336) = new AttributeValue(this.refilldivunits7thbef, keyTypes("refilldivunits7thbef")) 
				attributeVals(337) = new AttributeValue(this.refillpromodivunits7thbef, keyTypes("refillpromodivunits7thbef")) 
				attributeVals(338) = new AttributeValue(this.unitbalance7thbef, keyTypes("unitbalance7thbef")) 
				attributeVals(339) = new AttributeValue(this.clearedunits7thbef, keyTypes("clearedunits7thbef")) 
				attributeVals(340) = new AttributeValue(this.realmoneyflag7thbef, keyTypes("realmoneyflag7thbef")) 
				attributeVals(341) = new AttributeValue(this.dedicatedaccountunit8thbef, keyTypes("dedicatedaccountunit8thbef")) 
				attributeVals(342) = new AttributeValue(this.accountstartdate8thbef, keyTypes("accountstartdate8thbef")) 
				attributeVals(343) = new AttributeValue(this.refilldivunits8thbef, keyTypes("refilldivunits8thbef")) 
				attributeVals(344) = new AttributeValue(this.refillpromodivunits8thbef, keyTypes("refillpromodivunits8thbef")) 
				attributeVals(345) = new AttributeValue(this.unitbalance8thbef, keyTypes("unitbalance8thbef")) 
				attributeVals(346) = new AttributeValue(this.clearedunits8thbef, keyTypes("clearedunits8thbef")) 
				attributeVals(347) = new AttributeValue(this.realmoneyflag8thbef, keyTypes("realmoneyflag8thbef")) 
				attributeVals(348) = new AttributeValue(this.dedicatedaccountunit9thbef, keyTypes("dedicatedaccountunit9thbef")) 
				attributeVals(349) = new AttributeValue(this.accountstartdate9thbef, keyTypes("accountstartdate9thbef")) 
				attributeVals(350) = new AttributeValue(this.refilldivunits9thbef, keyTypes("refilldivunits9thbef")) 
				attributeVals(351) = new AttributeValue(this.refillpromodivunits9thbef, keyTypes("refillpromodivunits9thbef")) 
				attributeVals(352) = new AttributeValue(this.unitbalance9thbef, keyTypes("unitbalance9thbef")) 
				attributeVals(353) = new AttributeValue(this.clearedunits9thbef, keyTypes("clearedunits9thbef")) 
				attributeVals(354) = new AttributeValue(this.realmoneyflag9thbef, keyTypes("realmoneyflag9thbef")) 
				attributeVals(355) = new AttributeValue(this.dedicatedaccountunit10thbef, keyTypes("dedicatedaccountunit10thbef")) 
				attributeVals(356) = new AttributeValue(this.accountstartdate10thbef, keyTypes("accountstartdate10thbef")) 
				attributeVals(357) = new AttributeValue(this.refilldivunits10thbef, keyTypes("refilldivunits10thbef")) 
				attributeVals(358) = new AttributeValue(this.refillpromodivunits10thbef, keyTypes("refillpromodivunits10thbef")) 
				attributeVals(359) = new AttributeValue(this.unitbalance10thbef, keyTypes("unitbalance10thbef")) 
				attributeVals(360) = new AttributeValue(this.clearedunits10thbef, keyTypes("clearedunits10thbef")) 
				attributeVals(361) = new AttributeValue(this.realmoneyflag10thbef, keyTypes("realmoneyflag10thbef")) 
				attributeVals(362) = new AttributeValue(this.offer1stidentifierbef, keyTypes("offer1stidentifierbef")) 
				attributeVals(363) = new AttributeValue(this.offerstartdate1stbef, keyTypes("offerstartdate1stbef")) 
				attributeVals(364) = new AttributeValue(this.offerexpirydate1stbef, keyTypes("offerexpirydate1stbef")) 
				attributeVals(365) = new AttributeValue(this.offertype1stbef, keyTypes("offertype1stbef")) 
				attributeVals(366) = new AttributeValue(this.offerproductidentifier1stbef, keyTypes("offerproductidentifier1stbef")) 
				attributeVals(367) = new AttributeValue(this.offerstartdatetime1stbef, keyTypes("offerstartdatetime1stbef")) 
				attributeVals(368) = new AttributeValue(this.offerexpirydatetime1stbef, keyTypes("offerexpirydatetime1stbef")) 
				attributeVals(369) = new AttributeValue(this.offer2ndidentifierbef, keyTypes("offer2ndidentifierbef")) 
				attributeVals(370) = new AttributeValue(this.offerstartdate2ndbef, keyTypes("offerstartdate2ndbef")) 
				attributeVals(371) = new AttributeValue(this.offerexpirydate2ndbef, keyTypes("offerexpirydate2ndbef")) 
				attributeVals(372) = new AttributeValue(this.offertype2ndbef, keyTypes("offertype2ndbef")) 
				attributeVals(373) = new AttributeValue(this.offerproductidentifier2ndbef, keyTypes("offerproductidentifier2ndbef")) 
				attributeVals(374) = new AttributeValue(this.offerstartdatetime2ndbef, keyTypes("offerstartdatetime2ndbef")) 
				attributeVals(375) = new AttributeValue(this.offerexpirydatetime2ndbef, keyTypes("offerexpirydatetime2ndbef")) 
				attributeVals(376) = new AttributeValue(this.offer3rdidentifierbef, keyTypes("offer3rdidentifierbef")) 
				attributeVals(377) = new AttributeValue(this.offerstartdate3rdbef, keyTypes("offerstartdate3rdbef")) 
				attributeVals(378) = new AttributeValue(this.offerexpirydate3rdbef, keyTypes("offerexpirydate3rdbef")) 
				attributeVals(379) = new AttributeValue(this.offertype3rdbef, keyTypes("offertype3rdbef")) 
				attributeVals(380) = new AttributeValue(this.offerproductidentifier3rdbef, keyTypes("offerproductidentifier3rdbef")) 
				attributeVals(381) = new AttributeValue(this.offerstartdatetime3rdbef, keyTypes("offerstartdatetime3rdbef")) 
				attributeVals(382) = new AttributeValue(this.offerexpirydatetime3rdbef, keyTypes("offerexpirydatetime3rdbef")) 
				attributeVals(383) = new AttributeValue(this.offer4thidentifierbef, keyTypes("offer4thidentifierbef")) 
				attributeVals(384) = new AttributeValue(this.offerstartdate4thbef, keyTypes("offerstartdate4thbef")) 
				attributeVals(385) = new AttributeValue(this.offerexpirydate4thbef, keyTypes("offerexpirydate4thbef")) 
				attributeVals(386) = new AttributeValue(this.offertype4thbef, keyTypes("offertype4thbef")) 
				attributeVals(387) = new AttributeValue(this.offerproductidentifier4thbef, keyTypes("offerproductidentifier4thbef")) 
				attributeVals(388) = new AttributeValue(this.offerstartdatetime4thbef, keyTypes("offerstartdatetime4thbef")) 
				attributeVals(389) = new AttributeValue(this.offerexpirydatetime4thbef, keyTypes("offerexpirydatetime4thbef")) 
				attributeVals(390) = new AttributeValue(this.offer5thidentifierbef, keyTypes("offer5thidentifierbef")) 
				attributeVals(391) = new AttributeValue(this.offerstartdate5thbef, keyTypes("offerstartdate5thbef")) 
				attributeVals(392) = new AttributeValue(this.offerexpirydate5thbef, keyTypes("offerexpirydate5thbef")) 
				attributeVals(393) = new AttributeValue(this.offertype5thbef, keyTypes("offertype5thbef")) 
				attributeVals(394) = new AttributeValue(this.offerproductidentifier5thbef, keyTypes("offerproductidentifier5thbef")) 
				attributeVals(395) = new AttributeValue(this.offerstartdatetime5thbef, keyTypes("offerstartdatetime5thbef")) 
				attributeVals(396) = new AttributeValue(this.offerexpirydatetime5thbef, keyTypes("offerexpirydatetime5thbef")) 
				attributeVals(397) = new AttributeValue(this.offer6thidentifierbef, keyTypes("offer6thidentifierbef")) 
				attributeVals(398) = new AttributeValue(this.offerstartdate6thbef, keyTypes("offerstartdate6thbef")) 
				attributeVals(399) = new AttributeValue(this.offerexpirydate6thbef, keyTypes("offerexpirydate6thbef")) 
				attributeVals(400) = new AttributeValue(this.offertype6thbef, keyTypes("offertype6thbef")) 
				attributeVals(401) = new AttributeValue(this.offerproductidentifier6thbef, keyTypes("offerproductidentifier6thbef")) 
				attributeVals(402) = new AttributeValue(this.offerstartdatetime6thbef, keyTypes("offerstartdatetime6thbef")) 
				attributeVals(403) = new AttributeValue(this.offerexpirydatetime6thbef, keyTypes("offerexpirydatetime6thbef")) 
				attributeVals(404) = new AttributeValue(this.offer7thidentifierbef, keyTypes("offer7thidentifierbef")) 
				attributeVals(405) = new AttributeValue(this.offerstartdate7thbef, keyTypes("offerstartdate7thbef")) 
				attributeVals(406) = new AttributeValue(this.offerexpirydate7thbef, keyTypes("offerexpirydate7thbef")) 
				attributeVals(407) = new AttributeValue(this.offertype7thbef, keyTypes("offertype7thbef")) 
				attributeVals(408) = new AttributeValue(this.offerproductidentifier7thbef, keyTypes("offerproductidentifier7thbef")) 
				attributeVals(409) = new AttributeValue(this.offerstartdatetime7thbef, keyTypes("offerstartdatetime7thbef")) 
				attributeVals(410) = new AttributeValue(this.offerexpirydatetime7thbef, keyTypes("offerexpirydatetime7thbef")) 
				attributeVals(411) = new AttributeValue(this.offer8thidentifierbef, keyTypes("offer8thidentifierbef")) 
				attributeVals(412) = new AttributeValue(this.offerstartdate8thbef, keyTypes("offerstartdate8thbef")) 
				attributeVals(413) = new AttributeValue(this.offerexpirydate8thbef, keyTypes("offerexpirydate8thbef")) 
				attributeVals(414) = new AttributeValue(this.offertype8thbef, keyTypes("offertype8thbef")) 
				attributeVals(415) = new AttributeValue(this.offerproductidentifier8thbef, keyTypes("offerproductidentifier8thbef")) 
				attributeVals(416) = new AttributeValue(this.offerstartdatetime8thbef, keyTypes("offerstartdatetime8thbef")) 
				attributeVals(417) = new AttributeValue(this.offerexpirydatetime8thbef, keyTypes("offerexpirydatetime8thbef")) 
				attributeVals(418) = new AttributeValue(this.offer9thidentifierbef, keyTypes("offer9thidentifierbef")) 
				attributeVals(419) = new AttributeValue(this.offerstartdate9thbef, keyTypes("offerstartdate9thbef")) 
				attributeVals(420) = new AttributeValue(this.offerexpirydate9thbef, keyTypes("offerexpirydate9thbef")) 
				attributeVals(421) = new AttributeValue(this.offertype9thbef, keyTypes("offertype9thbef")) 
				attributeVals(422) = new AttributeValue(this.offerproductidentifier9thbef, keyTypes("offerproductidentifier9thbef")) 
				attributeVals(423) = new AttributeValue(this.offerstartdatetime9thbef, keyTypes("offerstartdatetime9thbef")) 
				attributeVals(424) = new AttributeValue(this.offerexpirydatetime9thbef, keyTypes("offerexpirydatetime9thbef")) 
				attributeVals(425) = new AttributeValue(this.offer10thidentifierbef, keyTypes("offer10thidentifierbef")) 
				attributeVals(426) = new AttributeValue(this.offerstartdate10thbef, keyTypes("offerstartdate10thbef")) 
				attributeVals(427) = new AttributeValue(this.offerexpirydate10thbef, keyTypes("offerexpirydate10thbef")) 
				attributeVals(428) = new AttributeValue(this.offertype10thbef, keyTypes("offertype10thbef")) 
				attributeVals(429) = new AttributeValue(this.offerproductidentifier10thbef, keyTypes("offerproductidentifier10thbef")) 
				attributeVals(430) = new AttributeValue(this.offerstartdatetime10thbef, keyTypes("offerstartdatetime10thbef")) 
				attributeVals(431) = new AttributeValue(this.offerexpirydatetime10thbef, keyTypes("offerexpirydatetime10thbef")) 
				attributeVals(432) = new AttributeValue(this.aggregatedbalancebef, keyTypes("aggregatedbalancebef")) 
				attributeVals(433) = new AttributeValue(this.dedicatedaccountunit1staft, keyTypes("dedicatedaccountunit1staft")) 
				attributeVals(434) = new AttributeValue(this.accountstartdate1staft, keyTypes("accountstartdate1staft")) 
				attributeVals(435) = new AttributeValue(this.refilldivunits1staft, keyTypes("refilldivunits1staft")) 
				attributeVals(436) = new AttributeValue(this.refillpromodivunits1staft, keyTypes("refillpromodivunits1staft")) 
				attributeVals(437) = new AttributeValue(this.unitbalance1staft, keyTypes("unitbalance1staft")) 
				attributeVals(438) = new AttributeValue(this.clearedunits1staft, keyTypes("clearedunits1staft")) 
				attributeVals(439) = new AttributeValue(this.realmoneyflag1staft, keyTypes("realmoneyflag1staft")) 
				attributeVals(440) = new AttributeValue(this.dedicatedaccountunit2ndaft, keyTypes("dedicatedaccountunit2ndaft")) 
				attributeVals(441) = new AttributeValue(this.accountstartdate2ndaft, keyTypes("accountstartdate2ndaft")) 
				attributeVals(442) = new AttributeValue(this.refilldivunits2ndaft, keyTypes("refilldivunits2ndaft")) 
				attributeVals(443) = new AttributeValue(this.refillpromodivunits2ndaft, keyTypes("refillpromodivunits2ndaft")) 
				attributeVals(444) = new AttributeValue(this.unitbalance2ndaft, keyTypes("unitbalance2ndaft")) 
				attributeVals(445) = new AttributeValue(this.clearedunits2ndaft, keyTypes("clearedunits2ndaft")) 
				attributeVals(446) = new AttributeValue(this.realmoneyflag2ndaft, keyTypes("realmoneyflag2ndaft")) 
				attributeVals(447) = new AttributeValue(this.dedicatedaccountunit3rdaft, keyTypes("dedicatedaccountunit3rdaft")) 
				attributeVals(448) = new AttributeValue(this.accountstartdate3rdaft, keyTypes("accountstartdate3rdaft")) 
				attributeVals(449) = new AttributeValue(this.refilldivunits3rdaft, keyTypes("refilldivunits3rdaft")) 
				attributeVals(450) = new AttributeValue(this.refillpromodivunits3rdaft, keyTypes("refillpromodivunits3rdaft")) 
				attributeVals(451) = new AttributeValue(this.unitbalance3rdaft, keyTypes("unitbalance3rdaft")) 
				attributeVals(452) = new AttributeValue(this.clearedunits3rdaft, keyTypes("clearedunits3rdaft")) 
				attributeVals(453) = new AttributeValue(this.realmoneyflag3rdaft, keyTypes("realmoneyflag3rdaft")) 
				attributeVals(454) = new AttributeValue(this.dedicatedaccountunit4thaft, keyTypes("dedicatedaccountunit4thaft")) 
				attributeVals(455) = new AttributeValue(this.accountstartdate4thaft, keyTypes("accountstartdate4thaft")) 
				attributeVals(456) = new AttributeValue(this.refilldivunits4thaft, keyTypes("refilldivunits4thaft")) 
				attributeVals(457) = new AttributeValue(this.refillpromodivunits4thaft, keyTypes("refillpromodivunits4thaft")) 
				attributeVals(458) = new AttributeValue(this.unitbalance4thaft, keyTypes("unitbalance4thaft")) 
				attributeVals(459) = new AttributeValue(this.clearedunits4thaft, keyTypes("clearedunits4thaft")) 
				attributeVals(460) = new AttributeValue(this.realmoneyflag4thaft, keyTypes("realmoneyflag4thaft")) 
				attributeVals(461) = new AttributeValue(this.dedicatedaccountunit5thaft, keyTypes("dedicatedaccountunit5thaft")) 
				attributeVals(462) = new AttributeValue(this.accountstartdate5thaft, keyTypes("accountstartdate5thaft")) 
				attributeVals(463) = new AttributeValue(this.refilldivunits5thaft, keyTypes("refilldivunits5thaft")) 
				attributeVals(464) = new AttributeValue(this.refillpromodivunits5thaft, keyTypes("refillpromodivunits5thaft")) 
				attributeVals(465) = new AttributeValue(this.unitbalance5thaft, keyTypes("unitbalance5thaft")) 
				attributeVals(466) = new AttributeValue(this.clearedunits5thaft, keyTypes("clearedunits5thaft")) 
				attributeVals(467) = new AttributeValue(this.realmoneyflag5thaft, keyTypes("realmoneyflag5thaft")) 
				attributeVals(468) = new AttributeValue(this.dedicatedaccountunit6thaft, keyTypes("dedicatedaccountunit6thaft")) 
				attributeVals(469) = new AttributeValue(this.accountstartdate6thaft, keyTypes("accountstartdate6thaft")) 
				attributeVals(470) = new AttributeValue(this.refilldivunits6thaft, keyTypes("refilldivunits6thaft")) 
				attributeVals(471) = new AttributeValue(this.refillpromodivunits6thaft, keyTypes("refillpromodivunits6thaft")) 
				attributeVals(472) = new AttributeValue(this.unitbalance6thaft, keyTypes("unitbalance6thaft")) 
				attributeVals(473) = new AttributeValue(this.clearedunits6thaft, keyTypes("clearedunits6thaft")) 
				attributeVals(474) = new AttributeValue(this.realmoneyflag6thaft, keyTypes("realmoneyflag6thaft")) 
				attributeVals(475) = new AttributeValue(this.dedicatedaccountunit7thaft, keyTypes("dedicatedaccountunit7thaft")) 
				attributeVals(476) = new AttributeValue(this.accountstartdate7thaft, keyTypes("accountstartdate7thaft")) 
				attributeVals(477) = new AttributeValue(this.refilldivunits7thaft, keyTypes("refilldivunits7thaft")) 
				attributeVals(478) = new AttributeValue(this.refillpromodivunits7thaft, keyTypes("refillpromodivunits7thaft")) 
				attributeVals(479) = new AttributeValue(this.unitbalance7thaft, keyTypes("unitbalance7thaft")) 
				attributeVals(480) = new AttributeValue(this.clearedunits7thaft, keyTypes("clearedunits7thaft")) 
				attributeVals(481) = new AttributeValue(this.realmoneyflag7thaft, keyTypes("realmoneyflag7thaft")) 
				attributeVals(482) = new AttributeValue(this.dedicatedaccountunit8thaft, keyTypes("dedicatedaccountunit8thaft")) 
				attributeVals(483) = new AttributeValue(this.accountstartdate8thaft, keyTypes("accountstartdate8thaft")) 
				attributeVals(484) = new AttributeValue(this.refilldivunits8thaft, keyTypes("refilldivunits8thaft")) 
				attributeVals(485) = new AttributeValue(this.refillpromodivunits8thaft, keyTypes("refillpromodivunits8thaft")) 
				attributeVals(486) = new AttributeValue(this.unitbalance8thaft, keyTypes("unitbalance8thaft")) 
				attributeVals(487) = new AttributeValue(this.clearedunits8thaft, keyTypes("clearedunits8thaft")) 
				attributeVals(488) = new AttributeValue(this.realmoneyflag8thaft, keyTypes("realmoneyflag8thaft")) 
				attributeVals(489) = new AttributeValue(this.dedicatedaccountunit9thaft, keyTypes("dedicatedaccountunit9thaft")) 
				attributeVals(490) = new AttributeValue(this.accountstartdate9thaft, keyTypes("accountstartdate9thaft")) 
				attributeVals(491) = new AttributeValue(this.refilldivunits9thaft, keyTypes("refilldivunits9thaft")) 
				attributeVals(492) = new AttributeValue(this.refillpromodivunits9thaft, keyTypes("refillpromodivunits9thaft")) 
				attributeVals(493) = new AttributeValue(this.unitbalance9thaft, keyTypes("unitbalance9thaft")) 
				attributeVals(494) = new AttributeValue(this.clearedunits9thaft, keyTypes("clearedunits9thaft")) 
				attributeVals(495) = new AttributeValue(this.realmoneyflag9thaft, keyTypes("realmoneyflag9thaft")) 
				attributeVals(496) = new AttributeValue(this.dedicatedaccountunit10thaft, keyTypes("dedicatedaccountunit10thaft")) 
				attributeVals(497) = new AttributeValue(this.accountstartdate10thaft, keyTypes("accountstartdate10thaft")) 
				attributeVals(498) = new AttributeValue(this.refilldivunits10thaft, keyTypes("refilldivunits10thaft")) 
				attributeVals(499) = new AttributeValue(this.refillpromodivunits10thaft, keyTypes("refillpromodivunits10thaft")) 
				attributeVals(500) = new AttributeValue(this.unitbalance10thaft, keyTypes("unitbalance10thaft")) 
				attributeVals(501) = new AttributeValue(this.clearedunits10thaft, keyTypes("clearedunits10thaft")) 
				attributeVals(502) = new AttributeValue(this.realmoneyflag10thaft, keyTypes("realmoneyflag10thaft")) 
				attributeVals(503) = new AttributeValue(this.offer1stidentifieraft, keyTypes("offer1stidentifieraft")) 
				attributeVals(504) = new AttributeValue(this.offerstartdate1staft, keyTypes("offerstartdate1staft")) 
				attributeVals(505) = new AttributeValue(this.offerexpirydate1staft, keyTypes("offerexpirydate1staft")) 
				attributeVals(506) = new AttributeValue(this.offertype1staft, keyTypes("offertype1staft")) 
				attributeVals(507) = new AttributeValue(this.offerproductidentifier1staft, keyTypes("offerproductidentifier1staft")) 
				attributeVals(508) = new AttributeValue(this.offerstartdatetime1staft, keyTypes("offerstartdatetime1staft")) 
				attributeVals(509) = new AttributeValue(this.offerexpirydatetime1staft, keyTypes("offerexpirydatetime1staft")) 
				attributeVals(510) = new AttributeValue(this.offer2ndidentifieraft, keyTypes("offer2ndidentifieraft")) 
				attributeVals(511) = new AttributeValue(this.offerstartdate2ndaft, keyTypes("offerstartdate2ndaft")) 
				attributeVals(512) = new AttributeValue(this.offerexpirydate2ndaft, keyTypes("offerexpirydate2ndaft")) 
				attributeVals(513) = new AttributeValue(this.offertype2ndaft, keyTypes("offertype2ndaft")) 
				attributeVals(514) = new AttributeValue(this.offerproductidentifier2ndaft, keyTypes("offerproductidentifier2ndaft")) 
				attributeVals(515) = new AttributeValue(this.offerstartdatetime2ndaft, keyTypes("offerstartdatetime2ndaft")) 
				attributeVals(516) = new AttributeValue(this.offerexpirydatetime2ndaft, keyTypes("offerexpirydatetime2ndaft")) 
				attributeVals(517) = new AttributeValue(this.offer3rdidentifieraft, keyTypes("offer3rdidentifieraft")) 
				attributeVals(518) = new AttributeValue(this.offerstartdate3rdaft, keyTypes("offerstartdate3rdaft")) 
				attributeVals(519) = new AttributeValue(this.offerexpirydate3rdaft, keyTypes("offerexpirydate3rdaft")) 
				attributeVals(520) = new AttributeValue(this.offertype3rdaft, keyTypes("offertype3rdaft")) 
				attributeVals(521) = new AttributeValue(this.offerproductidentifier3rdaft, keyTypes("offerproductidentifier3rdaft")) 
				attributeVals(522) = new AttributeValue(this.offerstartdatetime3rdaft, keyTypes("offerstartdatetime3rdaft")) 
				attributeVals(523) = new AttributeValue(this.offerexpirydatetime3rdaft, keyTypes("offerexpirydatetime3rdaft")) 
				attributeVals(524) = new AttributeValue(this.offer4thidentifieraft, keyTypes("offer4thidentifieraft")) 
				attributeVals(525) = new AttributeValue(this.offerstartdate4thaft, keyTypes("offerstartdate4thaft")) 
				attributeVals(526) = new AttributeValue(this.offerexpirydate4thaft, keyTypes("offerexpirydate4thaft")) 
				attributeVals(527) = new AttributeValue(this.offertype4thaft, keyTypes("offertype4thaft")) 
				attributeVals(528) = new AttributeValue(this.offerproductidentifier4thaft, keyTypes("offerproductidentifier4thaft")) 
				attributeVals(529) = new AttributeValue(this.offerstartdatetime4thaft, keyTypes("offerstartdatetime4thaft")) 
				attributeVals(530) = new AttributeValue(this.offerexpirydatetime4thaft, keyTypes("offerexpirydatetime4thaft")) 
				attributeVals(531) = new AttributeValue(this.offer5thidentifieraft, keyTypes("offer5thidentifieraft")) 
				attributeVals(532) = new AttributeValue(this.offerstartdate5thaft, keyTypes("offerstartdate5thaft")) 
				attributeVals(533) = new AttributeValue(this.offerexpirydate5thaft, keyTypes("offerexpirydate5thaft")) 
				attributeVals(534) = new AttributeValue(this.offertype5thaft, keyTypes("offertype5thaft")) 
				attributeVals(535) = new AttributeValue(this.offerproductidentifier5thaft, keyTypes("offerproductidentifier5thaft")) 
				attributeVals(536) = new AttributeValue(this.offerstartdatetime5thaft, keyTypes("offerstartdatetime5thaft")) 
				attributeVals(537) = new AttributeValue(this.offerexpirydatetime5thaft, keyTypes("offerexpirydatetime5thaft")) 
				attributeVals(538) = new AttributeValue(this.offer6thidentifieraft, keyTypes("offer6thidentifieraft")) 
				attributeVals(539) = new AttributeValue(this.offerstartdate6thaft, keyTypes("offerstartdate6thaft")) 
				attributeVals(540) = new AttributeValue(this.offerexpirydate6thaft, keyTypes("offerexpirydate6thaft")) 
				attributeVals(541) = new AttributeValue(this.offertype6thaft, keyTypes("offertype6thaft")) 
				attributeVals(542) = new AttributeValue(this.offerproductidentifier6thaft, keyTypes("offerproductidentifier6thaft")) 
				attributeVals(543) = new AttributeValue(this.offerstartdatetime6thaft, keyTypes("offerstartdatetime6thaft")) 
				attributeVals(544) = new AttributeValue(this.offerexpirydatetime6thaft, keyTypes("offerexpirydatetime6thaft")) 
				attributeVals(545) = new AttributeValue(this.offer7thidentifieraft, keyTypes("offer7thidentifieraft")) 
				attributeVals(546) = new AttributeValue(this.offerstartdate7thaft, keyTypes("offerstartdate7thaft")) 
				attributeVals(547) = new AttributeValue(this.offerexpirydate7thaft, keyTypes("offerexpirydate7thaft")) 
				attributeVals(548) = new AttributeValue(this.offertype7thaft, keyTypes("offertype7thaft")) 
				attributeVals(549) = new AttributeValue(this.offerproductidentifier7thaft, keyTypes("offerproductidentifier7thaft")) 
				attributeVals(550) = new AttributeValue(this.offerstartdatetime7thaft, keyTypes("offerstartdatetime7thaft")) 
				attributeVals(551) = new AttributeValue(this.offerexpirydatetime7thaft, keyTypes("offerexpirydatetime7thaft")) 
				attributeVals(552) = new AttributeValue(this.offer8thidentifieraft, keyTypes("offer8thidentifieraft")) 
				attributeVals(553) = new AttributeValue(this.offerstartdate8thaft, keyTypes("offerstartdate8thaft")) 
				attributeVals(554) = new AttributeValue(this.offerexpirydate8thaft, keyTypes("offerexpirydate8thaft")) 
				attributeVals(555) = new AttributeValue(this.offertype8thaft, keyTypes("offertype8thaft")) 
				attributeVals(556) = new AttributeValue(this.offerproductidentifier8thaft, keyTypes("offerproductidentifier8thaft")) 
				attributeVals(557) = new AttributeValue(this.offerstartdatetime8thaft, keyTypes("offerstartdatetime8thaft")) 
				attributeVals(558) = new AttributeValue(this.offerexpirydatetime8thaft, keyTypes("offerexpirydatetime8thaft")) 
				attributeVals(559) = new AttributeValue(this.offer9thidentifieraft, keyTypes("offer9thidentifieraft")) 
				attributeVals(560) = new AttributeValue(this.offerstartdate9thaft, keyTypes("offerstartdate9thaft")) 
				attributeVals(561) = new AttributeValue(this.offerexpirydate9thaft, keyTypes("offerexpirydate9thaft")) 
				attributeVals(562) = new AttributeValue(this.offertype9thaft, keyTypes("offertype9thaft")) 
				attributeVals(563) = new AttributeValue(this.offerproductidentifier9thaft, keyTypes("offerproductidentifier9thaft")) 
				attributeVals(564) = new AttributeValue(this.offerstartdatetime9thaft, keyTypes("offerstartdatetime9thaft")) 
				attributeVals(565) = new AttributeValue(this.offerexpirydatetime9thaft, keyTypes("offerexpirydatetime9thaft")) 
				attributeVals(566) = new AttributeValue(this.offer10thidentifieraft, keyTypes("offer10thidentifieraft")) 
				attributeVals(567) = new AttributeValue(this.offerstartdate10thaft, keyTypes("offerstartdate10thaft")) 
				attributeVals(568) = new AttributeValue(this.offerexpirydate10thaft, keyTypes("offerexpirydate10thaft")) 
				attributeVals(569) = new AttributeValue(this.offertype10thaft, keyTypes("offertype10thaft")) 
				attributeVals(570) = new AttributeValue(this.offerproductidentifier10thaft, keyTypes("offerproductidentifier10thaft")) 
				attributeVals(571) = new AttributeValue(this.offerstartdatetime10thaft, keyTypes("offerstartdatetime10thaft")) 
				attributeVals(572) = new AttributeValue(this.offerexpirydatetime10thaft, keyTypes("offerexpirydatetime10thaft")) 
				attributeVals(573) = new AttributeValue(this.aggregatedbalanceaft, keyTypes("aggregatedbalanceaft")) 
				attributeVals(574) = new AttributeValue(this.cellidentifier, keyTypes("cellidentifier")) 
				attributeVals(575) = new AttributeValue(this.market_id, keyTypes("market_id")) 
				attributeVals(576) = new AttributeValue(this.hub_id, keyTypes("hub_id")) 
				attributeVals(577) = new AttributeValue(this.filename, keyTypes("filename")) 
       
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
   
  			 if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message AirRefillCS5", null)
			 set(keyTypes(key).getIndex, value); 

      }catch {
          case e: Exception => {
          log.debug("", e)
          throw e
        }
      };
      
    }
  
      
    def set(index : Int, value :Any): Unit = {
      if (value == null) throw new Exception(s"Value is null for index $index in message AirRefillCS5 ")
      try{
        index match {
 				case 0 => { 
				if(value.isInstanceOf[String]) 
				  this.originnodetype = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field originnodetype in message AirRefillCS5") 
				} 
				case 1 => { 
				if(value.isInstanceOf[String]) 
				  this.originhostname = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field originhostname in message AirRefillCS5") 
				} 
				case 2 => { 
				if(value.isInstanceOf[String]) 
				  this.originfileid = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field originfileid in message AirRefillCS5") 
				} 
				case 3 => { 
				if(value.isInstanceOf[String]) 
				  this.origintransactionid = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field origintransactionid in message AirRefillCS5") 
				} 
				case 4 => { 
				if(value.isInstanceOf[String]) 
				  this.originoperatorid = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field originoperatorid in message AirRefillCS5") 
				} 
				case 5 => { 
				if(value.isInstanceOf[String]) 
				  this.origintimestamp = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field origintimestamp in message AirRefillCS5") 
				} 
				case 6 => { 
				if(value.isInstanceOf[String]) 
				  this.hostname = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field hostname in message AirRefillCS5") 
				} 
				case 7 => { 
				if(value.isInstanceOf[Double]) 
				  this.localsequencenumber = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field localsequencenumber in message AirRefillCS5") 
				} 
				case 8 => { 
				if(value.isInstanceOf[String]) 
				  this.timestamp = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field timestamp in message AirRefillCS5") 
				} 
				case 9 => { 
				if(value.isInstanceOf[Double]) 
				  this.currentserviceclass = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field currentserviceclass in message AirRefillCS5") 
				} 
				case 10 => { 
				if(value.isInstanceOf[String]) 
				  this.voucherbasedrefill = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field voucherbasedrefill in message AirRefillCS5") 
				} 
				case 11 => { 
				if(value.isInstanceOf[String]) 
				  this.transactiontype = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field transactiontype in message AirRefillCS5") 
				} 
				case 12 => { 
				if(value.isInstanceOf[String]) 
				  this.transactioncode = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field transactioncode in message AirRefillCS5") 
				} 
				case 13 => { 
				if(value.isInstanceOf[Double]) 
				  this.transactionamount = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field transactionamount in message AirRefillCS5") 
				} 
				case 14 => { 
				if(value.isInstanceOf[String]) 
				  this.transactioncurrency = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field transactioncurrency in message AirRefillCS5") 
				} 
				case 15 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillamountconverted = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillamountconverted in message AirRefillCS5") 
				} 
				case 16 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivisionamount = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivisionamount in message AirRefillCS5") 
				} 
				case 17 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilltype = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilltype in message AirRefillCS5") 
				} 
				case 18 => { 
				if(value.isInstanceOf[String]) 
				  this.refillprofileid = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field refillprofileid in message AirRefillCS5") 
				} 
				case 19 => { 
				if(value.isInstanceOf[String]) 
				  this.segmentationid = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field segmentationid in message AirRefillCS5") 
				} 
				case 20 => { 
				if(value.isInstanceOf[String]) 
				  this.voucherserialnumber = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field voucherserialnumber in message AirRefillCS5") 
				} 
				case 21 => { 
				if(value.isInstanceOf[String]) 
				  this.vouchergroupid = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field vouchergroupid in message AirRefillCS5") 
				} 
				case 22 => { 
				if(value.isInstanceOf[String]) 
				  this.accountnumber = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountnumber in message AirRefillCS5") 
				} 
				case 23 => { 
				if(value.isInstanceOf[String]) 
				  this.accountcurrency = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountcurrency in message AirRefillCS5") 
				} 
				case 24 => { 
				if(value.isInstanceOf[String]) 
				  this.subscribernumber = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field subscribernumber in message AirRefillCS5") 
				} 
				case 25 => { 
				if(value.isInstanceOf[String]) 
				  this.promotionannouncementcode = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field promotionannouncementcode in message AirRefillCS5") 
				} 
				case 26 => { 
				if(value.isInstanceOf[String]) 
				  this.accountflagsbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountflagsbef in message AirRefillCS5") 
				} 
				case 27 => { 
				if(value.isInstanceOf[Double]) 
				  this.accountbalancebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field accountbalancebef in message AirRefillCS5") 
				} 
				case 28 => { 
				if(value.isInstanceOf[Double]) 
				  this.accumulatedrefillvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field accumulatedrefillvaluebef in message AirRefillCS5") 
				} 
				case 29 => { 
				if(value.isInstanceOf[Double]) 
				  this.accumulatedrefillcounterbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field accumulatedrefillcounterbef in message AirRefillCS5") 
				} 
				case 30 => { 
				if(value.isInstanceOf[Double]) 
				  this.accumulatedprogressionvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field accumulatedprogressionvaluebef in message AirRefillCS5") 
				} 
				case 31 => { 
				if(value.isInstanceOf[Double]) 
				  this.accumulatedprogrcounterbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field accumulatedprogrcounterbef in message AirRefillCS5") 
				} 
				case 32 => { 
				if(value.isInstanceOf[Double]) 
				  this.creditclearanceperiodbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field creditclearanceperiodbef in message AirRefillCS5") 
				} 
				case 33 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount1stidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount1stidbef in message AirRefillCS5") 
				} 
				case 34 => { 
				if(value.isInstanceOf[Double]) 
				  this.account1stcampaignidentbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account1stcampaignidentbef in message AirRefillCS5") 
				} 
				case 35 => { 
				if(value.isInstanceOf[Double]) 
				  this.account1strefilldivamountbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account1strefilldivamountbef in message AirRefillCS5") 
				} 
				case 36 => { 
				if(value.isInstanceOf[Double]) 
				  this.account1strefillpromdivamntbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account1strefillpromdivamntbef in message AirRefillCS5") 
				} 
				case 37 => { 
				if(value.isInstanceOf[Double]) 
				  this.account1stbalancebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account1stbalancebef in message AirRefillCS5") 
				} 
				case 38 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount1stvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount1stvaluebef in message AirRefillCS5") 
				} 
				case 39 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount2ndidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount2ndidbef in message AirRefillCS5") 
				} 
				case 40 => { 
				if(value.isInstanceOf[Double]) 
				  this.account2ndcampaignidentifbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account2ndcampaignidentifbef in message AirRefillCS5") 
				} 
				case 41 => { 
				if(value.isInstanceOf[Double]) 
				  this.account2ndrefilldivamountbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account2ndrefilldivamountbef in message AirRefillCS5") 
				} 
				case 42 => { 
				if(value.isInstanceOf[Double]) 
				  this.account2ndrefilpromodivamntbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account2ndrefilpromodivamntbef in message AirRefillCS5") 
				} 
				case 43 => { 
				if(value.isInstanceOf[Double]) 
				  this.account2ndbalancebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account2ndbalancebef in message AirRefillCS5") 
				} 
				case 44 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount2ndvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount2ndvaluebef in message AirRefillCS5") 
				} 
				case 45 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount3rdidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount3rdidbef in message AirRefillCS5") 
				} 
				case 46 => { 
				if(value.isInstanceOf[Double]) 
				  this.account3rdcampaignidentbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account3rdcampaignidentbef in message AirRefillCS5") 
				} 
				case 47 => { 
				if(value.isInstanceOf[Double]) 
				  this.account3rdrefilldivamountbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account3rdrefilldivamountbef in message AirRefillCS5") 
				} 
				case 48 => { 
				if(value.isInstanceOf[Double]) 
				  this.account3rdrefilpromodivamntbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account3rdrefilpromodivamntbef in message AirRefillCS5") 
				} 
				case 49 => { 
				if(value.isInstanceOf[Double]) 
				  this.account3rdbalancebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account3rdbalancebef in message AirRefillCS5") 
				} 
				case 50 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount3rdvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount3rdvaluebef in message AirRefillCS5") 
				} 
				case 51 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount4thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount4thidbef in message AirRefillCS5") 
				} 
				case 52 => { 
				if(value.isInstanceOf[Double]) 
				  this.account4thcampaignidentbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account4thcampaignidentbef in message AirRefillCS5") 
				} 
				case 53 => { 
				if(value.isInstanceOf[Double]) 
				  this.account4threfilldivamountbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account4threfilldivamountbef in message AirRefillCS5") 
				} 
				case 54 => { 
				if(value.isInstanceOf[Double]) 
				  this.account4threfilpromodivamntbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account4threfilpromodivamntbef in message AirRefillCS5") 
				} 
				case 55 => { 
				if(value.isInstanceOf[Double]) 
				  this.account4thbalancebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account4thbalancebef in message AirRefillCS5") 
				} 
				case 56 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount4thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount4thvaluebef in message AirRefillCS5") 
				} 
				case 57 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount5thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount5thidbef in message AirRefillCS5") 
				} 
				case 58 => { 
				if(value.isInstanceOf[Double]) 
				  this.account5thcampaignidentbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account5thcampaignidentbef in message AirRefillCS5") 
				} 
				case 59 => { 
				if(value.isInstanceOf[Double]) 
				  this.account5threfilldivamountbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account5threfilldivamountbef in message AirRefillCS5") 
				} 
				case 60 => { 
				if(value.isInstanceOf[Double]) 
				  this.account5threfilpromodivamntbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account5threfilpromodivamntbef in message AirRefillCS5") 
				} 
				case 61 => { 
				if(value.isInstanceOf[Double]) 
				  this.account5thbalancebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account5thbalancebef in message AirRefillCS5") 
				} 
				case 62 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount5thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount5thvaluebef in message AirRefillCS5") 
				} 
				case 63 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount6thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount6thidbef in message AirRefillCS5") 
				} 
				case 64 => { 
				if(value.isInstanceOf[Double]) 
				  this.account6thcampaignidentbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account6thcampaignidentbef in message AirRefillCS5") 
				} 
				case 65 => { 
				if(value.isInstanceOf[Double]) 
				  this.account6threfilldivamountbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account6threfilldivamountbef in message AirRefillCS5") 
				} 
				case 66 => { 
				if(value.isInstanceOf[Double]) 
				  this.account6threfilpromodivamntbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account6threfilpromodivamntbef in message AirRefillCS5") 
				} 
				case 67 => { 
				if(value.isInstanceOf[Double]) 
				  this.account6thbalancebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account6thbalancebef in message AirRefillCS5") 
				} 
				case 68 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount6thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount6thvaluebef in message AirRefillCS5") 
				} 
				case 69 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount7thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount7thidbef in message AirRefillCS5") 
				} 
				case 70 => { 
				if(value.isInstanceOf[Double]) 
				  this.account7thcampaignidentbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account7thcampaignidentbef in message AirRefillCS5") 
				} 
				case 71 => { 
				if(value.isInstanceOf[Double]) 
				  this.account7threfilldivamountbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account7threfilldivamountbef in message AirRefillCS5") 
				} 
				case 72 => { 
				if(value.isInstanceOf[Double]) 
				  this.account7threfilpromodivamntbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account7threfilpromodivamntbef in message AirRefillCS5") 
				} 
				case 73 => { 
				if(value.isInstanceOf[Double]) 
				  this.account7thbalancebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account7thbalancebef in message AirRefillCS5") 
				} 
				case 74 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount7thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount7thvaluebef in message AirRefillCS5") 
				} 
				case 75 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount8thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount8thidbef in message AirRefillCS5") 
				} 
				case 76 => { 
				if(value.isInstanceOf[Double]) 
				  this.account8thcampaignidentbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account8thcampaignidentbef in message AirRefillCS5") 
				} 
				case 77 => { 
				if(value.isInstanceOf[Double]) 
				  this.account8threfilldivamountbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account8threfilldivamountbef in message AirRefillCS5") 
				} 
				case 78 => { 
				if(value.isInstanceOf[Double]) 
				  this.account8threfilpromodivamntbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account8threfilpromodivamntbef in message AirRefillCS5") 
				} 
				case 79 => { 
				if(value.isInstanceOf[Double]) 
				  this.account8thbalancebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account8thbalancebef in message AirRefillCS5") 
				} 
				case 80 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount8thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount8thvaluebef in message AirRefillCS5") 
				} 
				case 81 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount9thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount9thidbef in message AirRefillCS5") 
				} 
				case 82 => { 
				if(value.isInstanceOf[Double]) 
				  this.account9thcampaignidentbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account9thcampaignidentbef in message AirRefillCS5") 
				} 
				case 83 => { 
				if(value.isInstanceOf[Double]) 
				  this.account9threfilldivamountbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account9threfilldivamountbef in message AirRefillCS5") 
				} 
				case 84 => { 
				if(value.isInstanceOf[Double]) 
				  this.account9threfilpromodivamntbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account9threfilpromodivamntbef in message AirRefillCS5") 
				} 
				case 85 => { 
				if(value.isInstanceOf[Double]) 
				  this.account9thbalancebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account9thbalancebef in message AirRefillCS5") 
				} 
				case 86 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount9thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount9thvaluebef in message AirRefillCS5") 
				} 
				case 87 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount10thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount10thidbef in message AirRefillCS5") 
				} 
				case 88 => { 
				if(value.isInstanceOf[Double]) 
				  this.account10thcampaignidentbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account10thcampaignidentbef in message AirRefillCS5") 
				} 
				case 89 => { 
				if(value.isInstanceOf[Double]) 
				  this.account10threfilldivamountbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account10threfilldivamountbef in message AirRefillCS5") 
				} 
				case 90 => { 
				if(value.isInstanceOf[Double]) 
				  this.account10threfilpromdivamntbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account10threfilpromdivamntbef in message AirRefillCS5") 
				} 
				case 91 => { 
				if(value.isInstanceOf[Double]) 
				  this.account10thbalancebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account10thbalancebef in message AirRefillCS5") 
				} 
				case 92 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount10thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount10thvaluebef in message AirRefillCS5") 
				} 
				case 93 => { 
				if(value.isInstanceOf[String]) 
				  this.promotionplan = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field promotionplan in message AirRefillCS5") 
				} 
				case 94 => { 
				if(value.isInstanceOf[Double]) 
				  this.permanentserviceclassbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field permanentserviceclassbef in message AirRefillCS5") 
				} 
				case 95 => { 
				if(value.isInstanceOf[Double]) 
				  this.temporaryserviceclassbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field temporaryserviceclassbef in message AirRefillCS5") 
				} 
				case 96 => { 
				if(value.isInstanceOf[String]) 
				  this.temporaryservclassexpdatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field temporaryservclassexpdatebef in message AirRefillCS5") 
				} 
				case 97 => { 
				if(value.isInstanceOf[String]) 
				  this.refilloptionbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field refilloptionbef in message AirRefillCS5") 
				} 
				case 98 => { 
				if(value.isInstanceOf[String]) 
				  this.servicefeeexpirydatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field servicefeeexpirydatebef in message AirRefillCS5") 
				} 
				case 99 => { 
				if(value.isInstanceOf[Double]) 
				  this.serviceremovalgraceperiodbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field serviceremovalgraceperiodbef in message AirRefillCS5") 
				} 
				case 100 => { 
				if(value.isInstanceOf[String]) 
				  this.serviceofferingbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field serviceofferingbef in message AirRefillCS5") 
				} 
				case 101 => { 
				if(value.isInstanceOf[String]) 
				  this.supervisionexpirydatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field supervisionexpirydatebef in message AirRefillCS5") 
				} 
				case 102 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator1stidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator1stidbef in message AirRefillCS5") 
				} 
				case 103 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator1stvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator1stvaluebef in message AirRefillCS5") 
				} 
				case 104 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator2ndidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator2ndidbef in message AirRefillCS5") 
				} 
				case 105 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator2ndvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator2ndvaluebef in message AirRefillCS5") 
				} 
				case 106 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator3rdidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator3rdidbef in message AirRefillCS5") 
				} 
				case 107 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator3rdvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator3rdvaluebef in message AirRefillCS5") 
				} 
				case 108 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator4thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator4thidbef in message AirRefillCS5") 
				} 
				case 109 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator4thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator4thvaluebef in message AirRefillCS5") 
				} 
				case 110 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator5thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator5thidbef in message AirRefillCS5") 
				} 
				case 111 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator5thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator5thvaluebef in message AirRefillCS5") 
				} 
				case 112 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator6thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator6thidbef in message AirRefillCS5") 
				} 
				case 113 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator6thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator6thvaluebef in message AirRefillCS5") 
				} 
				case 114 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator7thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator7thidbef in message AirRefillCS5") 
				} 
				case 115 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator7thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator7thvaluebef in message AirRefillCS5") 
				} 
				case 116 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator8thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator8thidbef in message AirRefillCS5") 
				} 
				case 117 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator8thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator8thvaluebef in message AirRefillCS5") 
				} 
				case 118 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator9thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator9thidbef in message AirRefillCS5") 
				} 
				case 119 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator9thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator9thvaluebef in message AirRefillCS5") 
				} 
				case 120 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator10thidbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator10thidbef in message AirRefillCS5") 
				} 
				case 121 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator10thvaluebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator10thvaluebef in message AirRefillCS5") 
				} 
				case 122 => { 
				if(value.isInstanceOf[String]) 
				  this.communityidbef1 = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field communityidbef1 in message AirRefillCS5") 
				} 
				case 123 => { 
				if(value.isInstanceOf[String]) 
				  this.communityidbef2 = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field communityidbef2 in message AirRefillCS5") 
				} 
				case 124 => { 
				if(value.isInstanceOf[String]) 
				  this.communityidbef3 = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field communityidbef3 in message AirRefillCS5") 
				} 
				case 125 => { 
				if(value.isInstanceOf[String]) 
				  this.accountflagsaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountflagsaft in message AirRefillCS5") 
				} 
				case 126 => { 
				if(value.isInstanceOf[Double]) 
				  this.accountbalanceaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field accountbalanceaft in message AirRefillCS5") 
				} 
				case 127 => { 
				if(value.isInstanceOf[Double]) 
				  this.accumulatedrefillvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field accumulatedrefillvalueaft in message AirRefillCS5") 
				} 
				case 128 => { 
				if(value.isInstanceOf[Double]) 
				  this.accumulatedrefillcounteraft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field accumulatedrefillcounteraft in message AirRefillCS5") 
				} 
				case 129 => { 
				if(value.isInstanceOf[Double]) 
				  this.accumulatedprogressionvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field accumulatedprogressionvalueaft in message AirRefillCS5") 
				} 
				case 130 => { 
				if(value.isInstanceOf[Double]) 
				  this.accumulatedprogconteraft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field accumulatedprogconteraft in message AirRefillCS5") 
				} 
				case 131 => { 
				if(value.isInstanceOf[Double]) 
				  this.creditclearanceperiodaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field creditclearanceperiodaft in message AirRefillCS5") 
				} 
				case 132 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount1stidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount1stidaft in message AirRefillCS5") 
				} 
				case 133 => { 
				if(value.isInstanceOf[Double]) 
				  this.account1stcampaignidentaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account1stcampaignidentaft in message AirRefillCS5") 
				} 
				case 134 => { 
				if(value.isInstanceOf[Double]) 
				  this.account1strefilldivamountaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account1strefilldivamountaft in message AirRefillCS5") 
				} 
				case 135 => { 
				if(value.isInstanceOf[Double]) 
				  this.account1strefilpromodivamntaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account1strefilpromodivamntaft in message AirRefillCS5") 
				} 
				case 136 => { 
				if(value.isInstanceOf[Double]) 
				  this.account1stbalanceaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account1stbalanceaft in message AirRefillCS5") 
				} 
				case 137 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount1stvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount1stvalueaft in message AirRefillCS5") 
				} 
				case 138 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount2ndidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount2ndidaft in message AirRefillCS5") 
				} 
				case 139 => { 
				if(value.isInstanceOf[Double]) 
				  this.account2ndcampaignidentaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account2ndcampaignidentaft in message AirRefillCS5") 
				} 
				case 140 => { 
				if(value.isInstanceOf[Double]) 
				  this.account2ndrefilldivamountaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account2ndrefilldivamountaft in message AirRefillCS5") 
				} 
				case 141 => { 
				if(value.isInstanceOf[Double]) 
				  this.account2ndrefilpromodivamntaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account2ndrefilpromodivamntaft in message AirRefillCS5") 
				} 
				case 142 => { 
				if(value.isInstanceOf[Double]) 
				  this.account2ndbalanceaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account2ndbalanceaft in message AirRefillCS5") 
				} 
				case 143 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount2ndvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount2ndvalueaft in message AirRefillCS5") 
				} 
				case 144 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount3rdidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount3rdidaft in message AirRefillCS5") 
				} 
				case 145 => { 
				if(value.isInstanceOf[Double]) 
				  this.account3rdcampaignidentaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account3rdcampaignidentaft in message AirRefillCS5") 
				} 
				case 146 => { 
				if(value.isInstanceOf[Double]) 
				  this.account3rdrefilldivamountaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account3rdrefilldivamountaft in message AirRefillCS5") 
				} 
				case 147 => { 
				if(value.isInstanceOf[Double]) 
				  this.account3rdrefilpromodivamntaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account3rdrefilpromodivamntaft in message AirRefillCS5") 
				} 
				case 148 => { 
				if(value.isInstanceOf[Double]) 
				  this.account3rdbalanceaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account3rdbalanceaft in message AirRefillCS5") 
				} 
				case 149 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount3rdvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount3rdvalueaft in message AirRefillCS5") 
				} 
				case 150 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount4thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount4thidaft in message AirRefillCS5") 
				} 
				case 151 => { 
				if(value.isInstanceOf[Double]) 
				  this.account4thcampaignidentaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account4thcampaignidentaft in message AirRefillCS5") 
				} 
				case 152 => { 
				if(value.isInstanceOf[Double]) 
				  this.account4threfilldivamountaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account4threfilldivamountaft in message AirRefillCS5") 
				} 
				case 153 => { 
				if(value.isInstanceOf[Double]) 
				  this.account4threfilpromodivamntaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account4threfilpromodivamntaft in message AirRefillCS5") 
				} 
				case 154 => { 
				if(value.isInstanceOf[Double]) 
				  this.account4thbalanceaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account4thbalanceaft in message AirRefillCS5") 
				} 
				case 155 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount4thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount4thvalueaft in message AirRefillCS5") 
				} 
				case 156 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount5thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount5thidaft in message AirRefillCS5") 
				} 
				case 157 => { 
				if(value.isInstanceOf[Double]) 
				  this.account5thcampaignidentaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account5thcampaignidentaft in message AirRefillCS5") 
				} 
				case 158 => { 
				if(value.isInstanceOf[Double]) 
				  this.account5threfilldivamountaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account5threfilldivamountaft in message AirRefillCS5") 
				} 
				case 159 => { 
				if(value.isInstanceOf[Double]) 
				  this.account5threfilpromodivamntaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account5threfilpromodivamntaft in message AirRefillCS5") 
				} 
				case 160 => { 
				if(value.isInstanceOf[Double]) 
				  this.account5thbalanceaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account5thbalanceaft in message AirRefillCS5") 
				} 
				case 161 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount5thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount5thvalueaft in message AirRefillCS5") 
				} 
				case 162 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount6thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount6thidaft in message AirRefillCS5") 
				} 
				case 163 => { 
				if(value.isInstanceOf[Double]) 
				  this.account6thcampaignidentaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account6thcampaignidentaft in message AirRefillCS5") 
				} 
				case 164 => { 
				if(value.isInstanceOf[Double]) 
				  this.account6threfilldivamountaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account6threfilldivamountaft in message AirRefillCS5") 
				} 
				case 165 => { 
				if(value.isInstanceOf[Double]) 
				  this.account6threfilpromodivamntaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account6threfilpromodivamntaft in message AirRefillCS5") 
				} 
				case 166 => { 
				if(value.isInstanceOf[Double]) 
				  this.account6thbalanceaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account6thbalanceaft in message AirRefillCS5") 
				} 
				case 167 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount6thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount6thvalueaft in message AirRefillCS5") 
				} 
				case 168 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount7thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount7thidaft in message AirRefillCS5") 
				} 
				case 169 => { 
				if(value.isInstanceOf[Double]) 
				  this.account7thcampaignidentaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account7thcampaignidentaft in message AirRefillCS5") 
				} 
				case 170 => { 
				if(value.isInstanceOf[Double]) 
				  this.account7threfilldivamountaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account7threfilldivamountaft in message AirRefillCS5") 
				} 
				case 171 => { 
				if(value.isInstanceOf[Double]) 
				  this.account7threfilpromodivamntaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account7threfilpromodivamntaft in message AirRefillCS5") 
				} 
				case 172 => { 
				if(value.isInstanceOf[Double]) 
				  this.account7thbalanceaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account7thbalanceaft in message AirRefillCS5") 
				} 
				case 173 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount7thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount7thvalueaft in message AirRefillCS5") 
				} 
				case 174 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount8thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount8thidaft in message AirRefillCS5") 
				} 
				case 175 => { 
				if(value.isInstanceOf[Double]) 
				  this.account8thcampaignidentaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account8thcampaignidentaft in message AirRefillCS5") 
				} 
				case 176 => { 
				if(value.isInstanceOf[Double]) 
				  this.account8threfilldivamountaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account8threfilldivamountaft in message AirRefillCS5") 
				} 
				case 177 => { 
				if(value.isInstanceOf[Double]) 
				  this.account8threfilpromodivamntaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account8threfilpromodivamntaft in message AirRefillCS5") 
				} 
				case 178 => { 
				if(value.isInstanceOf[Double]) 
				  this.account8thbalanceaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account8thbalanceaft in message AirRefillCS5") 
				} 
				case 179 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount8thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount8thvalueaft in message AirRefillCS5") 
				} 
				case 180 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount9thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount9thidaft in message AirRefillCS5") 
				} 
				case 181 => { 
				if(value.isInstanceOf[Double]) 
				  this.account9thcampaignidentaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account9thcampaignidentaft in message AirRefillCS5") 
				} 
				case 182 => { 
				if(value.isInstanceOf[Double]) 
				  this.account9threfilldivamountaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account9threfilldivamountaft in message AirRefillCS5") 
				} 
				case 183 => { 
				if(value.isInstanceOf[Double]) 
				  this.account9threfilpromodivamntaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account9threfilpromodivamntaft in message AirRefillCS5") 
				} 
				case 184 => { 
				if(value.isInstanceOf[Double]) 
				  this.account9thbalanceaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account9thbalanceaft in message AirRefillCS5") 
				} 
				case 185 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount9thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount9thvalueaft in message AirRefillCS5") 
				} 
				case 186 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccount10thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccount10thidaft in message AirRefillCS5") 
				} 
				case 187 => { 
				if(value.isInstanceOf[Double]) 
				  this.account10thcampaignidentaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account10thcampaignidentaft in message AirRefillCS5") 
				} 
				case 188 => { 
				if(value.isInstanceOf[Double]) 
				  this.account10threfilldivamountaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account10threfilldivamountaft in message AirRefillCS5") 
				} 
				case 189 => { 
				if(value.isInstanceOf[Double]) 
				  this.account10threfilpromdivamntaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account10threfilpromdivamntaft in message AirRefillCS5") 
				} 
				case 190 => { 
				if(value.isInstanceOf[Double]) 
				  this.account10thbalanceaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field account10thbalanceaft in message AirRefillCS5") 
				} 
				case 191 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedaccount10thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedaccount10thvalueaft in message AirRefillCS5") 
				} 
				case 192 => { 
				if(value.isInstanceOf[String]) 
				  this.promotionplanaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field promotionplanaft in message AirRefillCS5") 
				} 
				case 193 => { 
				if(value.isInstanceOf[Double]) 
				  this.permanentserviceclassaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field permanentserviceclassaft in message AirRefillCS5") 
				} 
				case 194 => { 
				if(value.isInstanceOf[Double]) 
				  this.temporaryserviceclassaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field temporaryserviceclassaft in message AirRefillCS5") 
				} 
				case 195 => { 
				if(value.isInstanceOf[Double]) 
				  this.temporaryservclasexpirydateaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field temporaryservclasexpirydateaft in message AirRefillCS5") 
				} 
				case 196 => { 
				if(value.isInstanceOf[String]) 
				  this.refilloptionaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field refilloptionaft in message AirRefillCS5") 
				} 
				case 197 => { 
				if(value.isInstanceOf[String]) 
				  this.servicefeeexpirydateaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field servicefeeexpirydateaft in message AirRefillCS5") 
				} 
				case 198 => { 
				if(value.isInstanceOf[Double]) 
				  this.serviceremovalgraceperiodaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field serviceremovalgraceperiodaft in message AirRefillCS5") 
				} 
				case 199 => { 
				if(value.isInstanceOf[String]) 
				  this.serviceofferingaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field serviceofferingaft in message AirRefillCS5") 
				} 
				case 200 => { 
				if(value.isInstanceOf[String]) 
				  this.supervisionexpirydateaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field supervisionexpirydateaft in message AirRefillCS5") 
				} 
				case 201 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator1stidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator1stidaft in message AirRefillCS5") 
				} 
				case 202 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator1stvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator1stvalueaft in message AirRefillCS5") 
				} 
				case 203 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator2ndidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator2ndidaft in message AirRefillCS5") 
				} 
				case 204 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator2ndvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator2ndvalueaft in message AirRefillCS5") 
				} 
				case 205 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator3rdidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator3rdidaft in message AirRefillCS5") 
				} 
				case 206 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator3rdvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator3rdvalueaft in message AirRefillCS5") 
				} 
				case 207 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator4thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator4thidaft in message AirRefillCS5") 
				} 
				case 208 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator4thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator4thvalueaft in message AirRefillCS5") 
				} 
				case 209 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator5thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator5thidaft in message AirRefillCS5") 
				} 
				case 210 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator5thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator5thvalueaft in message AirRefillCS5") 
				} 
				case 211 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator6thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator6thidaft in message AirRefillCS5") 
				} 
				case 212 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator6thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator6thvalueaft in message AirRefillCS5") 
				} 
				case 213 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator7thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator7thidaft in message AirRefillCS5") 
				} 
				case 214 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator7thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator7thvalueaft in message AirRefillCS5") 
				} 
				case 215 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator8thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator8thidaft in message AirRefillCS5") 
				} 
				case 216 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator8thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator8thvalueaft in message AirRefillCS5") 
				} 
				case 217 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator9thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator9thidaft in message AirRefillCS5") 
				} 
				case 218 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator9thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator9thvalueaft in message AirRefillCS5") 
				} 
				case 219 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator10thidaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator10thidaft in message AirRefillCS5") 
				} 
				case 220 => { 
				if(value.isInstanceOf[Double]) 
				  this.usageaccumulator10thvalueaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field usageaccumulator10thvalueaft in message AirRefillCS5") 
				} 
				case 221 => { 
				if(value.isInstanceOf[String]) 
				  this.communityidaft1 = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field communityidaft1 in message AirRefillCS5") 
				} 
				case 222 => { 
				if(value.isInstanceOf[String]) 
				  this.communityidaft2 = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field communityidaft2 in message AirRefillCS5") 
				} 
				case 223 => { 
				if(value.isInstanceOf[String]) 
				  this.communityidaft3 = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field communityidaft3 in message AirRefillCS5") 
				} 
				case 224 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivisionamount = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivisionamount in message AirRefillCS5") 
				} 
				case 225 => { 
				if(value.isInstanceOf[Double]) 
				  this.supervisiondayspromopart = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field supervisiondayspromopart in message AirRefillCS5") 
				} 
				case 226 => { 
				if(value.isInstanceOf[Double]) 
				  this.supervisiondayssurplus = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field supervisiondayssurplus in message AirRefillCS5") 
				} 
				case 227 => { 
				if(value.isInstanceOf[Double]) 
				  this.servicefeedayspromopart = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field servicefeedayspromopart in message AirRefillCS5") 
				} 
				case 228 => { 
				if(value.isInstanceOf[Double]) 
				  this.servicefeedayssurplus = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field servicefeedayssurplus in message AirRefillCS5") 
				} 
				case 229 => { 
				if(value.isInstanceOf[Double]) 
				  this.maximumservicefeeperiod = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field maximumservicefeeperiod in message AirRefillCS5") 
				} 
				case 230 => { 
				if(value.isInstanceOf[Double]) 
				  this.maximumsupervisionperiod = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field maximumsupervisionperiod in message AirRefillCS5") 
				} 
				case 231 => { 
				if(value.isInstanceOf[String]) 
				  this.activationdate = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field activationdate in message AirRefillCS5") 
				} 
				case 232 => { 
				if(value.isInstanceOf[Double]) 
				  this.welcomestatus = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field welcomestatus in message AirRefillCS5") 
				} 
				case 233 => { 
				if(value.isInstanceOf[String]) 
				  this.voucheragent = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field voucheragent in message AirRefillCS5") 
				} 
				case 234 => { 
				if(value.isInstanceOf[String]) 
				  this.promotionplanallocstartdate = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field promotionplanallocstartdate in message AirRefillCS5") 
				} 
				case 235 => { 
				if(value.isInstanceOf[String]) 
				  this.accountgroupid = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountgroupid in message AirRefillCS5") 
				} 
				case 236 => { 
				if(value.isInstanceOf[String]) 
				  this.externaldata1 = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field externaldata1 in message AirRefillCS5") 
				} 
				case 237 => { 
				if(value.isInstanceOf[String]) 
				  this.externaldata2 = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field externaldata2 in message AirRefillCS5") 
				} 
				case 238 => { 
				if(value.isInstanceOf[String]) 
				  this.externaldata3 = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field externaldata3 in message AirRefillCS5") 
				} 
				case 239 => { 
				if(value.isInstanceOf[String]) 
				  this.externaldata4 = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field externaldata4 in message AirRefillCS5") 
				} 
				case 240 => { 
				if(value.isInstanceOf[String]) 
				  this.locationnumber = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field locationnumber in message AirRefillCS5") 
				} 
				case 241 => { 
				if(value.isInstanceOf[String]) 
				  this.voucheractivationcode = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field voucheractivationcode in message AirRefillCS5") 
				} 
				case 242 => { 
				if(value.isInstanceOf[String]) 
				  this.accountcurrencycleared = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountcurrencycleared in message AirRefillCS5") 
				} 
				case 243 => { 
				if(value.isInstanceOf[String]) 
				  this.ignoreserviceclasshierarchy = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field ignoreserviceclasshierarchy in message AirRefillCS5") 
				} 
				case 244 => { 
				if(value.isInstanceOf[String]) 
				  this.accounthomeregion = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accounthomeregion in message AirRefillCS5") 
				} 
				case 245 => { 
				if(value.isInstanceOf[String]) 
				  this.subscriberregion = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field subscriberregion in message AirRefillCS5") 
				} 
				case 246 => { 
				if(value.isInstanceOf[String]) 
				  this.voucherregion = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field voucherregion in message AirRefillCS5") 
				} 
				case 247 => { 
				if(value.isInstanceOf[String]) 
				  this.promotionplanallocenddate = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field promotionplanallocenddate in message AirRefillCS5") 
				} 
				case 248 => { 
				if(value.isInstanceOf[String]) 
				  this.requestedrefilltype = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field requestedrefilltype in message AirRefillCS5") 
				} 
				case 249 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry1stdatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry1stdatebef in message AirRefillCS5") 
				} 
				case 250 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry1stdateaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry1stdateaft in message AirRefillCS5") 
				} 
				case 251 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry2nddatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry2nddatebef in message AirRefillCS5") 
				} 
				case 252 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry2nddateaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry2nddateaft in message AirRefillCS5") 
				} 
				case 253 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry3rddatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry3rddatebef in message AirRefillCS5") 
				} 
				case 254 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry3rddateaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry3rddateaft in message AirRefillCS5") 
				} 
				case 255 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry4thdatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry4thdatebef in message AirRefillCS5") 
				} 
				case 256 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry4thdateaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry4thdateaft in message AirRefillCS5") 
				} 
				case 257 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry5thdatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry5thdatebef in message AirRefillCS5") 
				} 
				case 258 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry5thdateaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry5thdateaft in message AirRefillCS5") 
				} 
				case 259 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry6thdatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry6thdatebef in message AirRefillCS5") 
				} 
				case 260 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry6thdateaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry6thdateaft in message AirRefillCS5") 
				} 
				case 261 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry7thdatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry7thdatebef in message AirRefillCS5") 
				} 
				case 262 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry7thdateaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry7thdateaft in message AirRefillCS5") 
				} 
				case 263 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry8thdatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry8thdatebef in message AirRefillCS5") 
				} 
				case 264 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry8thdateaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry8thdateaft in message AirRefillCS5") 
				} 
				case 265 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry9thdatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry9thdatebef in message AirRefillCS5") 
				} 
				case 266 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry9thdateaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry9thdateaft in message AirRefillCS5") 
				} 
				case 267 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry10thdatebef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry10thdatebef in message AirRefillCS5") 
				} 
				case 268 => { 
				if(value.isInstanceOf[String]) 
				  this.accountexpiry10thdateaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountexpiry10thdateaft in message AirRefillCS5") 
				} 
				case 269 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartmain = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartmain in message AirRefillCS5") 
				} 
				case 270 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartda1st = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartda1st in message AirRefillCS5") 
				} 
				case 271 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartda2nd = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartda2nd in message AirRefillCS5") 
				} 
				case 272 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartda3rd = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartda3rd in message AirRefillCS5") 
				} 
				case 273 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartda4th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartda4th in message AirRefillCS5") 
				} 
				case 274 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartda5th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartda5th in message AirRefillCS5") 
				} 
				case 275 => { 
				if(value.isInstanceOf[Double]) 
				  this.accumulatedprogressionvalueres = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field accumulatedprogressionvalueres in message AirRefillCS5") 
				} 
				case 276 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartpmain = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartpmain in message AirRefillCS5") 
				} 
				case 277 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartpda1st = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartpda1st in message AirRefillCS5") 
				} 
				case 278 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartpda2nd = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartpda2nd in message AirRefillCS5") 
				} 
				case 279 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartpda3rd = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartpda3rd in message AirRefillCS5") 
				} 
				case 280 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartpda4th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartpda4th in message AirRefillCS5") 
				} 
				case 281 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartpda5th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartpda5th in message AirRefillCS5") 
				} 
				case 282 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartda6th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartda6th in message AirRefillCS5") 
				} 
				case 283 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartda7th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartda7th in message AirRefillCS5") 
				} 
				case 284 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartda8th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartda8th in message AirRefillCS5") 
				} 
				case 285 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartda9th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartda9th in message AirRefillCS5") 
				} 
				case 286 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartda10th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartda10th in message AirRefillCS5") 
				} 
				case 287 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartpda6th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartpda6th in message AirRefillCS5") 
				} 
				case 288 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartpda7th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartpda7th in message AirRefillCS5") 
				} 
				case 289 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartpda8th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartpda8th in message AirRefillCS5") 
				} 
				case 290 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartpda9th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartpda9th in message AirRefillCS5") 
				} 
				case 291 => { 
				if(value.isInstanceOf[Double]) 
				  this.rechargedivpartpda10th = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field rechargedivpartpda10th in message AirRefillCS5") 
				} 
				case 292 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit1stbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit1stbef in message AirRefillCS5") 
				} 
				case 293 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate1stbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate1stbef in message AirRefillCS5") 
				} 
				case 294 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits1stbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits1stbef in message AirRefillCS5") 
				} 
				case 295 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits1stbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits1stbef in message AirRefillCS5") 
				} 
				case 296 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance1stbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance1stbef in message AirRefillCS5") 
				} 
				case 297 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits1stbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits1stbef in message AirRefillCS5") 
				} 
				case 298 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag1stbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag1stbef in message AirRefillCS5") 
				} 
				case 299 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit2ndbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit2ndbef in message AirRefillCS5") 
				} 
				case 300 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate2ndbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate2ndbef in message AirRefillCS5") 
				} 
				case 301 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits2ndbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits2ndbef in message AirRefillCS5") 
				} 
				case 302 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits2ndbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits2ndbef in message AirRefillCS5") 
				} 
				case 303 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance2ndbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance2ndbef in message AirRefillCS5") 
				} 
				case 304 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits2ndbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits2ndbef in message AirRefillCS5") 
				} 
				case 305 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag2ndbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag2ndbef in message AirRefillCS5") 
				} 
				case 306 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit3rdbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit3rdbef in message AirRefillCS5") 
				} 
				case 307 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate3rdbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate3rdbef in message AirRefillCS5") 
				} 
				case 308 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits3rdbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits3rdbef in message AirRefillCS5") 
				} 
				case 309 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits3rdbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits3rdbef in message AirRefillCS5") 
				} 
				case 310 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance3rdbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance3rdbef in message AirRefillCS5") 
				} 
				case 311 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits3rdbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits3rdbef in message AirRefillCS5") 
				} 
				case 312 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag3rdbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag3rdbef in message AirRefillCS5") 
				} 
				case 313 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit4thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit4thbef in message AirRefillCS5") 
				} 
				case 314 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate4thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate4thbef in message AirRefillCS5") 
				} 
				case 315 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits4thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits4thbef in message AirRefillCS5") 
				} 
				case 316 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits4thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits4thbef in message AirRefillCS5") 
				} 
				case 317 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance4thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance4thbef in message AirRefillCS5") 
				} 
				case 318 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits4thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits4thbef in message AirRefillCS5") 
				} 
				case 319 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag4thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag4thbef in message AirRefillCS5") 
				} 
				case 320 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit5thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit5thbef in message AirRefillCS5") 
				} 
				case 321 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate5thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate5thbef in message AirRefillCS5") 
				} 
				case 322 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits5thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits5thbef in message AirRefillCS5") 
				} 
				case 323 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits5thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits5thbef in message AirRefillCS5") 
				} 
				case 324 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance5thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance5thbef in message AirRefillCS5") 
				} 
				case 325 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits5thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits5thbef in message AirRefillCS5") 
				} 
				case 326 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag5thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag5thbef in message AirRefillCS5") 
				} 
				case 327 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit6thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit6thbef in message AirRefillCS5") 
				} 
				case 328 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate6thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate6thbef in message AirRefillCS5") 
				} 
				case 329 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits6thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits6thbef in message AirRefillCS5") 
				} 
				case 330 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits6thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits6thbef in message AirRefillCS5") 
				} 
				case 331 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance6thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance6thbef in message AirRefillCS5") 
				} 
				case 332 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits6thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits6thbef in message AirRefillCS5") 
				} 
				case 333 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag6thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag6thbef in message AirRefillCS5") 
				} 
				case 334 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit7thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit7thbef in message AirRefillCS5") 
				} 
				case 335 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate7thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate7thbef in message AirRefillCS5") 
				} 
				case 336 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits7thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits7thbef in message AirRefillCS5") 
				} 
				case 337 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits7thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits7thbef in message AirRefillCS5") 
				} 
				case 338 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance7thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance7thbef in message AirRefillCS5") 
				} 
				case 339 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits7thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits7thbef in message AirRefillCS5") 
				} 
				case 340 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag7thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag7thbef in message AirRefillCS5") 
				} 
				case 341 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit8thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit8thbef in message AirRefillCS5") 
				} 
				case 342 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate8thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate8thbef in message AirRefillCS5") 
				} 
				case 343 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits8thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits8thbef in message AirRefillCS5") 
				} 
				case 344 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits8thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits8thbef in message AirRefillCS5") 
				} 
				case 345 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance8thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance8thbef in message AirRefillCS5") 
				} 
				case 346 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits8thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits8thbef in message AirRefillCS5") 
				} 
				case 347 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag8thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag8thbef in message AirRefillCS5") 
				} 
				case 348 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit9thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit9thbef in message AirRefillCS5") 
				} 
				case 349 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate9thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate9thbef in message AirRefillCS5") 
				} 
				case 350 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits9thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits9thbef in message AirRefillCS5") 
				} 
				case 351 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits9thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits9thbef in message AirRefillCS5") 
				} 
				case 352 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance9thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance9thbef in message AirRefillCS5") 
				} 
				case 353 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits9thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits9thbef in message AirRefillCS5") 
				} 
				case 354 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag9thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag9thbef in message AirRefillCS5") 
				} 
				case 355 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit10thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit10thbef in message AirRefillCS5") 
				} 
				case 356 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate10thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate10thbef in message AirRefillCS5") 
				} 
				case 357 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits10thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits10thbef in message AirRefillCS5") 
				} 
				case 358 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits10thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits10thbef in message AirRefillCS5") 
				} 
				case 359 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance10thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance10thbef in message AirRefillCS5") 
				} 
				case 360 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits10thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits10thbef in message AirRefillCS5") 
				} 
				case 361 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag10thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag10thbef in message AirRefillCS5") 
				} 
				case 362 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer1stidentifierbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer1stidentifierbef in message AirRefillCS5") 
				} 
				case 363 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate1stbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate1stbef in message AirRefillCS5") 
				} 
				case 364 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate1stbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate1stbef in message AirRefillCS5") 
				} 
				case 365 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype1stbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype1stbef in message AirRefillCS5") 
				} 
				case 366 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier1stbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier1stbef in message AirRefillCS5") 
				} 
				case 367 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime1stbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime1stbef in message AirRefillCS5") 
				} 
				case 368 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime1stbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime1stbef in message AirRefillCS5") 
				} 
				case 369 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer2ndidentifierbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer2ndidentifierbef in message AirRefillCS5") 
				} 
				case 370 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate2ndbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate2ndbef in message AirRefillCS5") 
				} 
				case 371 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate2ndbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate2ndbef in message AirRefillCS5") 
				} 
				case 372 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype2ndbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype2ndbef in message AirRefillCS5") 
				} 
				case 373 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier2ndbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier2ndbef in message AirRefillCS5") 
				} 
				case 374 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime2ndbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime2ndbef in message AirRefillCS5") 
				} 
				case 375 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime2ndbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime2ndbef in message AirRefillCS5") 
				} 
				case 376 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer3rdidentifierbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer3rdidentifierbef in message AirRefillCS5") 
				} 
				case 377 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate3rdbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate3rdbef in message AirRefillCS5") 
				} 
				case 378 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate3rdbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate3rdbef in message AirRefillCS5") 
				} 
				case 379 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype3rdbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype3rdbef in message AirRefillCS5") 
				} 
				case 380 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier3rdbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier3rdbef in message AirRefillCS5") 
				} 
				case 381 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime3rdbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime3rdbef in message AirRefillCS5") 
				} 
				case 382 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime3rdbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime3rdbef in message AirRefillCS5") 
				} 
				case 383 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer4thidentifierbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer4thidentifierbef in message AirRefillCS5") 
				} 
				case 384 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate4thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate4thbef in message AirRefillCS5") 
				} 
				case 385 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate4thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate4thbef in message AirRefillCS5") 
				} 
				case 386 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype4thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype4thbef in message AirRefillCS5") 
				} 
				case 387 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier4thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier4thbef in message AirRefillCS5") 
				} 
				case 388 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime4thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime4thbef in message AirRefillCS5") 
				} 
				case 389 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime4thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime4thbef in message AirRefillCS5") 
				} 
				case 390 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer5thidentifierbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer5thidentifierbef in message AirRefillCS5") 
				} 
				case 391 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate5thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate5thbef in message AirRefillCS5") 
				} 
				case 392 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate5thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate5thbef in message AirRefillCS5") 
				} 
				case 393 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype5thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype5thbef in message AirRefillCS5") 
				} 
				case 394 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier5thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier5thbef in message AirRefillCS5") 
				} 
				case 395 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime5thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime5thbef in message AirRefillCS5") 
				} 
				case 396 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime5thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime5thbef in message AirRefillCS5") 
				} 
				case 397 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer6thidentifierbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer6thidentifierbef in message AirRefillCS5") 
				} 
				case 398 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate6thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate6thbef in message AirRefillCS5") 
				} 
				case 399 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate6thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate6thbef in message AirRefillCS5") 
				} 
				case 400 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype6thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype6thbef in message AirRefillCS5") 
				} 
				case 401 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier6thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier6thbef in message AirRefillCS5") 
				} 
				case 402 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime6thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime6thbef in message AirRefillCS5") 
				} 
				case 403 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime6thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime6thbef in message AirRefillCS5") 
				} 
				case 404 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer7thidentifierbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer7thidentifierbef in message AirRefillCS5") 
				} 
				case 405 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate7thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate7thbef in message AirRefillCS5") 
				} 
				case 406 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate7thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate7thbef in message AirRefillCS5") 
				} 
				case 407 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype7thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype7thbef in message AirRefillCS5") 
				} 
				case 408 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier7thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier7thbef in message AirRefillCS5") 
				} 
				case 409 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime7thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime7thbef in message AirRefillCS5") 
				} 
				case 410 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime7thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime7thbef in message AirRefillCS5") 
				} 
				case 411 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer8thidentifierbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer8thidentifierbef in message AirRefillCS5") 
				} 
				case 412 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate8thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate8thbef in message AirRefillCS5") 
				} 
				case 413 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate8thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate8thbef in message AirRefillCS5") 
				} 
				case 414 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype8thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype8thbef in message AirRefillCS5") 
				} 
				case 415 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier8thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier8thbef in message AirRefillCS5") 
				} 
				case 416 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime8thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime8thbef in message AirRefillCS5") 
				} 
				case 417 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime8thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime8thbef in message AirRefillCS5") 
				} 
				case 418 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer9thidentifierbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer9thidentifierbef in message AirRefillCS5") 
				} 
				case 419 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate9thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate9thbef in message AirRefillCS5") 
				} 
				case 420 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate9thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate9thbef in message AirRefillCS5") 
				} 
				case 421 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype9thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype9thbef in message AirRefillCS5") 
				} 
				case 422 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier9thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier9thbef in message AirRefillCS5") 
				} 
				case 423 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime9thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime9thbef in message AirRefillCS5") 
				} 
				case 424 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime9thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime9thbef in message AirRefillCS5") 
				} 
				case 425 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer10thidentifierbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer10thidentifierbef in message AirRefillCS5") 
				} 
				case 426 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate10thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate10thbef in message AirRefillCS5") 
				} 
				case 427 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate10thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate10thbef in message AirRefillCS5") 
				} 
				case 428 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype10thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype10thbef in message AirRefillCS5") 
				} 
				case 429 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier10thbef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier10thbef in message AirRefillCS5") 
				} 
				case 430 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime10thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime10thbef in message AirRefillCS5") 
				} 
				case 431 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime10thbef = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime10thbef in message AirRefillCS5") 
				} 
				case 432 => { 
				if(value.isInstanceOf[Double]) 
				  this.aggregatedbalancebef = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field aggregatedbalancebef in message AirRefillCS5") 
				} 
				case 433 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit1staft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit1staft in message AirRefillCS5") 
				} 
				case 434 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate1staft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate1staft in message AirRefillCS5") 
				} 
				case 435 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits1staft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits1staft in message AirRefillCS5") 
				} 
				case 436 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits1staft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits1staft in message AirRefillCS5") 
				} 
				case 437 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance1staft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance1staft in message AirRefillCS5") 
				} 
				case 438 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits1staft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits1staft in message AirRefillCS5") 
				} 
				case 439 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag1staft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag1staft in message AirRefillCS5") 
				} 
				case 440 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit2ndaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit2ndaft in message AirRefillCS5") 
				} 
				case 441 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate2ndaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate2ndaft in message AirRefillCS5") 
				} 
				case 442 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits2ndaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits2ndaft in message AirRefillCS5") 
				} 
				case 443 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits2ndaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits2ndaft in message AirRefillCS5") 
				} 
				case 444 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance2ndaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance2ndaft in message AirRefillCS5") 
				} 
				case 445 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits2ndaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits2ndaft in message AirRefillCS5") 
				} 
				case 446 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag2ndaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag2ndaft in message AirRefillCS5") 
				} 
				case 447 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit3rdaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit3rdaft in message AirRefillCS5") 
				} 
				case 448 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate3rdaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate3rdaft in message AirRefillCS5") 
				} 
				case 449 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits3rdaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits3rdaft in message AirRefillCS5") 
				} 
				case 450 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits3rdaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits3rdaft in message AirRefillCS5") 
				} 
				case 451 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance3rdaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance3rdaft in message AirRefillCS5") 
				} 
				case 452 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits3rdaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits3rdaft in message AirRefillCS5") 
				} 
				case 453 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag3rdaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag3rdaft in message AirRefillCS5") 
				} 
				case 454 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit4thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit4thaft in message AirRefillCS5") 
				} 
				case 455 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate4thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate4thaft in message AirRefillCS5") 
				} 
				case 456 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits4thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits4thaft in message AirRefillCS5") 
				} 
				case 457 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits4thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits4thaft in message AirRefillCS5") 
				} 
				case 458 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance4thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance4thaft in message AirRefillCS5") 
				} 
				case 459 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits4thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits4thaft in message AirRefillCS5") 
				} 
				case 460 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag4thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag4thaft in message AirRefillCS5") 
				} 
				case 461 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit5thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit5thaft in message AirRefillCS5") 
				} 
				case 462 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate5thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate5thaft in message AirRefillCS5") 
				} 
				case 463 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits5thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits5thaft in message AirRefillCS5") 
				} 
				case 464 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits5thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits5thaft in message AirRefillCS5") 
				} 
				case 465 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance5thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance5thaft in message AirRefillCS5") 
				} 
				case 466 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits5thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits5thaft in message AirRefillCS5") 
				} 
				case 467 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag5thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag5thaft in message AirRefillCS5") 
				} 
				case 468 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit6thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit6thaft in message AirRefillCS5") 
				} 
				case 469 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate6thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate6thaft in message AirRefillCS5") 
				} 
				case 470 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits6thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits6thaft in message AirRefillCS5") 
				} 
				case 471 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits6thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits6thaft in message AirRefillCS5") 
				} 
				case 472 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance6thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance6thaft in message AirRefillCS5") 
				} 
				case 473 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits6thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits6thaft in message AirRefillCS5") 
				} 
				case 474 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag6thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag6thaft in message AirRefillCS5") 
				} 
				case 475 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit7thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit7thaft in message AirRefillCS5") 
				} 
				case 476 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate7thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate7thaft in message AirRefillCS5") 
				} 
				case 477 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits7thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits7thaft in message AirRefillCS5") 
				} 
				case 478 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits7thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits7thaft in message AirRefillCS5") 
				} 
				case 479 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance7thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance7thaft in message AirRefillCS5") 
				} 
				case 480 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits7thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits7thaft in message AirRefillCS5") 
				} 
				case 481 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag7thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag7thaft in message AirRefillCS5") 
				} 
				case 482 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit8thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit8thaft in message AirRefillCS5") 
				} 
				case 483 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate8thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate8thaft in message AirRefillCS5") 
				} 
				case 484 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits8thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits8thaft in message AirRefillCS5") 
				} 
				case 485 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits8thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits8thaft in message AirRefillCS5") 
				} 
				case 486 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance8thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance8thaft in message AirRefillCS5") 
				} 
				case 487 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits8thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits8thaft in message AirRefillCS5") 
				} 
				case 488 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag8thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag8thaft in message AirRefillCS5") 
				} 
				case 489 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit9thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit9thaft in message AirRefillCS5") 
				} 
				case 490 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate9thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate9thaft in message AirRefillCS5") 
				} 
				case 491 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits9thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits9thaft in message AirRefillCS5") 
				} 
				case 492 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits9thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits9thaft in message AirRefillCS5") 
				} 
				case 493 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance9thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance9thaft in message AirRefillCS5") 
				} 
				case 494 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits9thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits9thaft in message AirRefillCS5") 
				} 
				case 495 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag9thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag9thaft in message AirRefillCS5") 
				} 
				case 496 => { 
				if(value.isInstanceOf[Double]) 
				  this.dedicatedaccountunit10thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field dedicatedaccountunit10thaft in message AirRefillCS5") 
				} 
				case 497 => { 
				if(value.isInstanceOf[String]) 
				  this.accountstartdate10thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field accountstartdate10thaft in message AirRefillCS5") 
				} 
				case 498 => { 
				if(value.isInstanceOf[Double]) 
				  this.refilldivunits10thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refilldivunits10thaft in message AirRefillCS5") 
				} 
				case 499 => { 
				if(value.isInstanceOf[Double]) 
				  this.refillpromodivunits10thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field refillpromodivunits10thaft in message AirRefillCS5") 
				} 
				case 500 => { 
				if(value.isInstanceOf[Double]) 
				  this.unitbalance10thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field unitbalance10thaft in message AirRefillCS5") 
				} 
				case 501 => { 
				if(value.isInstanceOf[Double]) 
				  this.clearedunits10thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field clearedunits10thaft in message AirRefillCS5") 
				} 
				case 502 => { 
				if(value.isInstanceOf[Double]) 
				  this.realmoneyflag10thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field realmoneyflag10thaft in message AirRefillCS5") 
				} 
				case 503 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer1stidentifieraft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer1stidentifieraft in message AirRefillCS5") 
				} 
				case 504 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate1staft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate1staft in message AirRefillCS5") 
				} 
				case 505 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate1staft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate1staft in message AirRefillCS5") 
				} 
				case 506 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype1staft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype1staft in message AirRefillCS5") 
				} 
				case 507 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier1staft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier1staft in message AirRefillCS5") 
				} 
				case 508 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime1staft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime1staft in message AirRefillCS5") 
				} 
				case 509 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime1staft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime1staft in message AirRefillCS5") 
				} 
				case 510 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer2ndidentifieraft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer2ndidentifieraft in message AirRefillCS5") 
				} 
				case 511 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate2ndaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate2ndaft in message AirRefillCS5") 
				} 
				case 512 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate2ndaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate2ndaft in message AirRefillCS5") 
				} 
				case 513 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype2ndaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype2ndaft in message AirRefillCS5") 
				} 
				case 514 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier2ndaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier2ndaft in message AirRefillCS5") 
				} 
				case 515 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime2ndaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime2ndaft in message AirRefillCS5") 
				} 
				case 516 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime2ndaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime2ndaft in message AirRefillCS5") 
				} 
				case 517 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer3rdidentifieraft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer3rdidentifieraft in message AirRefillCS5") 
				} 
				case 518 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate3rdaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate3rdaft in message AirRefillCS5") 
				} 
				case 519 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate3rdaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate3rdaft in message AirRefillCS5") 
				} 
				case 520 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype3rdaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype3rdaft in message AirRefillCS5") 
				} 
				case 521 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier3rdaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier3rdaft in message AirRefillCS5") 
				} 
				case 522 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime3rdaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime3rdaft in message AirRefillCS5") 
				} 
				case 523 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime3rdaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime3rdaft in message AirRefillCS5") 
				} 
				case 524 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer4thidentifieraft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer4thidentifieraft in message AirRefillCS5") 
				} 
				case 525 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate4thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate4thaft in message AirRefillCS5") 
				} 
				case 526 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate4thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate4thaft in message AirRefillCS5") 
				} 
				case 527 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype4thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype4thaft in message AirRefillCS5") 
				} 
				case 528 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier4thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier4thaft in message AirRefillCS5") 
				} 
				case 529 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime4thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime4thaft in message AirRefillCS5") 
				} 
				case 530 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime4thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime4thaft in message AirRefillCS5") 
				} 
				case 531 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer5thidentifieraft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer5thidentifieraft in message AirRefillCS5") 
				} 
				case 532 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate5thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate5thaft in message AirRefillCS5") 
				} 
				case 533 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate5thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate5thaft in message AirRefillCS5") 
				} 
				case 534 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype5thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype5thaft in message AirRefillCS5") 
				} 
				case 535 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier5thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier5thaft in message AirRefillCS5") 
				} 
				case 536 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime5thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime5thaft in message AirRefillCS5") 
				} 
				case 537 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime5thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime5thaft in message AirRefillCS5") 
				} 
				case 538 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer6thidentifieraft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer6thidentifieraft in message AirRefillCS5") 
				} 
				case 539 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate6thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate6thaft in message AirRefillCS5") 
				} 
				case 540 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate6thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate6thaft in message AirRefillCS5") 
				} 
				case 541 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype6thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype6thaft in message AirRefillCS5") 
				} 
				case 542 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier6thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier6thaft in message AirRefillCS5") 
				} 
				case 543 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime6thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime6thaft in message AirRefillCS5") 
				} 
				case 544 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime6thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime6thaft in message AirRefillCS5") 
				} 
				case 545 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer7thidentifieraft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer7thidentifieraft in message AirRefillCS5") 
				} 
				case 546 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate7thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate7thaft in message AirRefillCS5") 
				} 
				case 547 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate7thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate7thaft in message AirRefillCS5") 
				} 
				case 548 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype7thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype7thaft in message AirRefillCS5") 
				} 
				case 549 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier7thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier7thaft in message AirRefillCS5") 
				} 
				case 550 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime7thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime7thaft in message AirRefillCS5") 
				} 
				case 551 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime7thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime7thaft in message AirRefillCS5") 
				} 
				case 552 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer8thidentifieraft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer8thidentifieraft in message AirRefillCS5") 
				} 
				case 553 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate8thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate8thaft in message AirRefillCS5") 
				} 
				case 554 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate8thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate8thaft in message AirRefillCS5") 
				} 
				case 555 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype8thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype8thaft in message AirRefillCS5") 
				} 
				case 556 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier8thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier8thaft in message AirRefillCS5") 
				} 
				case 557 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime8thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime8thaft in message AirRefillCS5") 
				} 
				case 558 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime8thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime8thaft in message AirRefillCS5") 
				} 
				case 559 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer9thidentifieraft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer9thidentifieraft in message AirRefillCS5") 
				} 
				case 560 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate9thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate9thaft in message AirRefillCS5") 
				} 
				case 561 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate9thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate9thaft in message AirRefillCS5") 
				} 
				case 562 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype9thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype9thaft in message AirRefillCS5") 
				} 
				case 563 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier9thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier9thaft in message AirRefillCS5") 
				} 
				case 564 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime9thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime9thaft in message AirRefillCS5") 
				} 
				case 565 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime9thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime9thaft in message AirRefillCS5") 
				} 
				case 566 => { 
				if(value.isInstanceOf[Double]) 
				  this.offer10thidentifieraft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offer10thidentifieraft in message AirRefillCS5") 
				} 
				case 567 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdate10thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdate10thaft in message AirRefillCS5") 
				} 
				case 568 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydate10thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydate10thaft in message AirRefillCS5") 
				} 
				case 569 => { 
				if(value.isInstanceOf[String]) 
				  this.offertype10thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offertype10thaft in message AirRefillCS5") 
				} 
				case 570 => { 
				if(value.isInstanceOf[Double]) 
				  this.offerproductidentifier10thaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field offerproductidentifier10thaft in message AirRefillCS5") 
				} 
				case 571 => { 
				if(value.isInstanceOf[String]) 
				  this.offerstartdatetime10thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerstartdatetime10thaft in message AirRefillCS5") 
				} 
				case 572 => { 
				if(value.isInstanceOf[String]) 
				  this.offerexpirydatetime10thaft = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field offerexpirydatetime10thaft in message AirRefillCS5") 
				} 
				case 573 => { 
				if(value.isInstanceOf[Double]) 
				  this.aggregatedbalanceaft = value.asInstanceOf[Double]; 
				 else throw new Exception(s"Value is the not the correct type for field aggregatedbalanceaft in message AirRefillCS5") 
				} 
				case 574 => { 
				if(value.isInstanceOf[String]) 
				  this.cellidentifier = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field cellidentifier in message AirRefillCS5") 
				} 
				case 575 => { 
				if(value.isInstanceOf[Int]) 
				  this.market_id = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field market_id in message AirRefillCS5") 
				} 
				case 576 => { 
				if(value.isInstanceOf[Int]) 
				  this.hub_id = value.asInstanceOf[Int]; 
				 else throw new Exception(s"Value is the not the correct type for field hub_id in message AirRefillCS5") 
				} 
				case 577 => { 
				if(value.isInstanceOf[String]) 
				  this.filename = value.asInstanceOf[String]; 
				 else throw new Exception(s"Value is the not the correct type for field filename in message AirRefillCS5") 
				} 

        case _ => throw new Exception(s"$index is a bad index for message AirRefillCS5");
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
  
    private def fromFunc(other: AirRefillCS5): AirRefillCS5 = {  
   			this.originnodetype = com.ligadata.BaseTypes.StringImpl.Clone(other.originnodetype);
			this.originhostname = com.ligadata.BaseTypes.StringImpl.Clone(other.originhostname);
			this.originfileid = com.ligadata.BaseTypes.StringImpl.Clone(other.originfileid);
			this.origintransactionid = com.ligadata.BaseTypes.StringImpl.Clone(other.origintransactionid);
			this.originoperatorid = com.ligadata.BaseTypes.StringImpl.Clone(other.originoperatorid);
			this.origintimestamp = com.ligadata.BaseTypes.StringImpl.Clone(other.origintimestamp);
			this.hostname = com.ligadata.BaseTypes.StringImpl.Clone(other.hostname);
			this.localsequencenumber = com.ligadata.BaseTypes.DoubleImpl.Clone(other.localsequencenumber);
			this.timestamp = com.ligadata.BaseTypes.StringImpl.Clone(other.timestamp);
			this.currentserviceclass = com.ligadata.BaseTypes.DoubleImpl.Clone(other.currentserviceclass);
			this.voucherbasedrefill = com.ligadata.BaseTypes.StringImpl.Clone(other.voucherbasedrefill);
			this.transactiontype = com.ligadata.BaseTypes.StringImpl.Clone(other.transactiontype);
			this.transactioncode = com.ligadata.BaseTypes.StringImpl.Clone(other.transactioncode);
			this.transactionamount = com.ligadata.BaseTypes.DoubleImpl.Clone(other.transactionamount);
			this.transactioncurrency = com.ligadata.BaseTypes.StringImpl.Clone(other.transactioncurrency);
			this.refillamountconverted = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillamountconverted);
			this.refilldivisionamount = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivisionamount);
			this.refilltype = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilltype);
			this.refillprofileid = com.ligadata.BaseTypes.StringImpl.Clone(other.refillprofileid);
			this.segmentationid = com.ligadata.BaseTypes.StringImpl.Clone(other.segmentationid);
			this.voucherserialnumber = com.ligadata.BaseTypes.StringImpl.Clone(other.voucherserialnumber);
			this.vouchergroupid = com.ligadata.BaseTypes.StringImpl.Clone(other.vouchergroupid);
			this.accountnumber = com.ligadata.BaseTypes.StringImpl.Clone(other.accountnumber);
			this.accountcurrency = com.ligadata.BaseTypes.StringImpl.Clone(other.accountcurrency);
			this.subscribernumber = com.ligadata.BaseTypes.StringImpl.Clone(other.subscribernumber);
			this.promotionannouncementcode = com.ligadata.BaseTypes.StringImpl.Clone(other.promotionannouncementcode);
			this.accountflagsbef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountflagsbef);
			this.accountbalancebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.accountbalancebef);
			this.accumulatedrefillvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.accumulatedrefillvaluebef);
			this.accumulatedrefillcounterbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.accumulatedrefillcounterbef);
			this.accumulatedprogressionvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.accumulatedprogressionvaluebef);
			this.accumulatedprogrcounterbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.accumulatedprogrcounterbef);
			this.creditclearanceperiodbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.creditclearanceperiodbef);
			this.dedicatedaccount1stidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount1stidbef);
			this.account1stcampaignidentbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account1stcampaignidentbef);
			this.account1strefilldivamountbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account1strefilldivamountbef);
			this.account1strefillpromdivamntbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account1strefillpromdivamntbef);
			this.account1stbalancebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account1stbalancebef);
			this.clearedaccount1stvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount1stvaluebef);
			this.dedicatedaccount2ndidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount2ndidbef);
			this.account2ndcampaignidentifbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account2ndcampaignidentifbef);
			this.account2ndrefilldivamountbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account2ndrefilldivamountbef);
			this.account2ndrefilpromodivamntbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account2ndrefilpromodivamntbef);
			this.account2ndbalancebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account2ndbalancebef);
			this.clearedaccount2ndvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount2ndvaluebef);
			this.dedicatedaccount3rdidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount3rdidbef);
			this.account3rdcampaignidentbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account3rdcampaignidentbef);
			this.account3rdrefilldivamountbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account3rdrefilldivamountbef);
			this.account3rdrefilpromodivamntbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account3rdrefilpromodivamntbef);
			this.account3rdbalancebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account3rdbalancebef);
			this.clearedaccount3rdvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount3rdvaluebef);
			this.dedicatedaccount4thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount4thidbef);
			this.account4thcampaignidentbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account4thcampaignidentbef);
			this.account4threfilldivamountbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account4threfilldivamountbef);
			this.account4threfilpromodivamntbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account4threfilpromodivamntbef);
			this.account4thbalancebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account4thbalancebef);
			this.clearedaccount4thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount4thvaluebef);
			this.dedicatedaccount5thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount5thidbef);
			this.account5thcampaignidentbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account5thcampaignidentbef);
			this.account5threfilldivamountbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account5threfilldivamountbef);
			this.account5threfilpromodivamntbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account5threfilpromodivamntbef);
			this.account5thbalancebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account5thbalancebef);
			this.clearedaccount5thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount5thvaluebef);
			this.dedicatedaccount6thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount6thidbef);
			this.account6thcampaignidentbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account6thcampaignidentbef);
			this.account6threfilldivamountbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account6threfilldivamountbef);
			this.account6threfilpromodivamntbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account6threfilpromodivamntbef);
			this.account6thbalancebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account6thbalancebef);
			this.clearedaccount6thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount6thvaluebef);
			this.dedicatedaccount7thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount7thidbef);
			this.account7thcampaignidentbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account7thcampaignidentbef);
			this.account7threfilldivamountbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account7threfilldivamountbef);
			this.account7threfilpromodivamntbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account7threfilpromodivamntbef);
			this.account7thbalancebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account7thbalancebef);
			this.clearedaccount7thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount7thvaluebef);
			this.dedicatedaccount8thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount8thidbef);
			this.account8thcampaignidentbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account8thcampaignidentbef);
			this.account8threfilldivamountbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account8threfilldivamountbef);
			this.account8threfilpromodivamntbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account8threfilpromodivamntbef);
			this.account8thbalancebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account8thbalancebef);
			this.clearedaccount8thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount8thvaluebef);
			this.dedicatedaccount9thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount9thidbef);
			this.account9thcampaignidentbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account9thcampaignidentbef);
			this.account9threfilldivamountbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account9threfilldivamountbef);
			this.account9threfilpromodivamntbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account9threfilpromodivamntbef);
			this.account9thbalancebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account9thbalancebef);
			this.clearedaccount9thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount9thvaluebef);
			this.dedicatedaccount10thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount10thidbef);
			this.account10thcampaignidentbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account10thcampaignidentbef);
			this.account10threfilldivamountbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account10threfilldivamountbef);
			this.account10threfilpromdivamntbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account10threfilpromdivamntbef);
			this.account10thbalancebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account10thbalancebef);
			this.clearedaccount10thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount10thvaluebef);
			this.promotionplan = com.ligadata.BaseTypes.StringImpl.Clone(other.promotionplan);
			this.permanentserviceclassbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.permanentserviceclassbef);
			this.temporaryserviceclassbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.temporaryserviceclassbef);
			this.temporaryservclassexpdatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.temporaryservclassexpdatebef);
			this.refilloptionbef = com.ligadata.BaseTypes.StringImpl.Clone(other.refilloptionbef);
			this.servicefeeexpirydatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.servicefeeexpirydatebef);
			this.serviceremovalgraceperiodbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.serviceremovalgraceperiodbef);
			this.serviceofferingbef = com.ligadata.BaseTypes.StringImpl.Clone(other.serviceofferingbef);
			this.supervisionexpirydatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.supervisionexpirydatebef);
			this.usageaccumulator1stidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator1stidbef);
			this.usageaccumulator1stvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator1stvaluebef);
			this.usageaccumulator2ndidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator2ndidbef);
			this.usageaccumulator2ndvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator2ndvaluebef);
			this.usageaccumulator3rdidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator3rdidbef);
			this.usageaccumulator3rdvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator3rdvaluebef);
			this.usageaccumulator4thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator4thidbef);
			this.usageaccumulator4thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator4thvaluebef);
			this.usageaccumulator5thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator5thidbef);
			this.usageaccumulator5thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator5thvaluebef);
			this.usageaccumulator6thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator6thidbef);
			this.usageaccumulator6thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator6thvaluebef);
			this.usageaccumulator7thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator7thidbef);
			this.usageaccumulator7thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator7thvaluebef);
			this.usageaccumulator8thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator8thidbef);
			this.usageaccumulator8thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator8thvaluebef);
			this.usageaccumulator9thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator9thidbef);
			this.usageaccumulator9thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator9thvaluebef);
			this.usageaccumulator10thidbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator10thidbef);
			this.usageaccumulator10thvaluebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator10thvaluebef);
			this.communityidbef1 = com.ligadata.BaseTypes.StringImpl.Clone(other.communityidbef1);
			this.communityidbef2 = com.ligadata.BaseTypes.StringImpl.Clone(other.communityidbef2);
			this.communityidbef3 = com.ligadata.BaseTypes.StringImpl.Clone(other.communityidbef3);
			this.accountflagsaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountflagsaft);
			this.accountbalanceaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.accountbalanceaft);
			this.accumulatedrefillvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.accumulatedrefillvalueaft);
			this.accumulatedrefillcounteraft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.accumulatedrefillcounteraft);
			this.accumulatedprogressionvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.accumulatedprogressionvalueaft);
			this.accumulatedprogconteraft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.accumulatedprogconteraft);
			this.creditclearanceperiodaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.creditclearanceperiodaft);
			this.dedicatedaccount1stidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount1stidaft);
			this.account1stcampaignidentaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account1stcampaignidentaft);
			this.account1strefilldivamountaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account1strefilldivamountaft);
			this.account1strefilpromodivamntaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account1strefilpromodivamntaft);
			this.account1stbalanceaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account1stbalanceaft);
			this.clearedaccount1stvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount1stvalueaft);
			this.dedicatedaccount2ndidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount2ndidaft);
			this.account2ndcampaignidentaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account2ndcampaignidentaft);
			this.account2ndrefilldivamountaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account2ndrefilldivamountaft);
			this.account2ndrefilpromodivamntaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account2ndrefilpromodivamntaft);
			this.account2ndbalanceaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account2ndbalanceaft);
			this.clearedaccount2ndvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount2ndvalueaft);
			this.dedicatedaccount3rdidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount3rdidaft);
			this.account3rdcampaignidentaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account3rdcampaignidentaft);
			this.account3rdrefilldivamountaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account3rdrefilldivamountaft);
			this.account3rdrefilpromodivamntaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account3rdrefilpromodivamntaft);
			this.account3rdbalanceaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account3rdbalanceaft);
			this.clearedaccount3rdvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount3rdvalueaft);
			this.dedicatedaccount4thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount4thidaft);
			this.account4thcampaignidentaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account4thcampaignidentaft);
			this.account4threfilldivamountaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account4threfilldivamountaft);
			this.account4threfilpromodivamntaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account4threfilpromodivamntaft);
			this.account4thbalanceaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account4thbalanceaft);
			this.clearedaccount4thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount4thvalueaft);
			this.dedicatedaccount5thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount5thidaft);
			this.account5thcampaignidentaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account5thcampaignidentaft);
			this.account5threfilldivamountaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account5threfilldivamountaft);
			this.account5threfilpromodivamntaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account5threfilpromodivamntaft);
			this.account5thbalanceaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account5thbalanceaft);
			this.clearedaccount5thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount5thvalueaft);
			this.dedicatedaccount6thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount6thidaft);
			this.account6thcampaignidentaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account6thcampaignidentaft);
			this.account6threfilldivamountaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account6threfilldivamountaft);
			this.account6threfilpromodivamntaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account6threfilpromodivamntaft);
			this.account6thbalanceaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account6thbalanceaft);
			this.clearedaccount6thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount6thvalueaft);
			this.dedicatedaccount7thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount7thidaft);
			this.account7thcampaignidentaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account7thcampaignidentaft);
			this.account7threfilldivamountaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account7threfilldivamountaft);
			this.account7threfilpromodivamntaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account7threfilpromodivamntaft);
			this.account7thbalanceaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account7thbalanceaft);
			this.clearedaccount7thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount7thvalueaft);
			this.dedicatedaccount8thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount8thidaft);
			this.account8thcampaignidentaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account8thcampaignidentaft);
			this.account8threfilldivamountaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account8threfilldivamountaft);
			this.account8threfilpromodivamntaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account8threfilpromodivamntaft);
			this.account8thbalanceaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account8thbalanceaft);
			this.clearedaccount8thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount8thvalueaft);
			this.dedicatedaccount9thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount9thidaft);
			this.account9thcampaignidentaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account9thcampaignidentaft);
			this.account9threfilldivamountaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account9threfilldivamountaft);
			this.account9threfilpromodivamntaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account9threfilpromodivamntaft);
			this.account9thbalanceaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account9thbalanceaft);
			this.clearedaccount9thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount9thvalueaft);
			this.dedicatedaccount10thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccount10thidaft);
			this.account10thcampaignidentaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account10thcampaignidentaft);
			this.account10threfilldivamountaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account10threfilldivamountaft);
			this.account10threfilpromdivamntaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account10threfilpromdivamntaft);
			this.account10thbalanceaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.account10thbalanceaft);
			this.clearedaccount10thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedaccount10thvalueaft);
			this.promotionplanaft = com.ligadata.BaseTypes.StringImpl.Clone(other.promotionplanaft);
			this.permanentserviceclassaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.permanentserviceclassaft);
			this.temporaryserviceclassaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.temporaryserviceclassaft);
			this.temporaryservclasexpirydateaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.temporaryservclasexpirydateaft);
			this.refilloptionaft = com.ligadata.BaseTypes.StringImpl.Clone(other.refilloptionaft);
			this.servicefeeexpirydateaft = com.ligadata.BaseTypes.StringImpl.Clone(other.servicefeeexpirydateaft);
			this.serviceremovalgraceperiodaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.serviceremovalgraceperiodaft);
			this.serviceofferingaft = com.ligadata.BaseTypes.StringImpl.Clone(other.serviceofferingaft);
			this.supervisionexpirydateaft = com.ligadata.BaseTypes.StringImpl.Clone(other.supervisionexpirydateaft);
			this.usageaccumulator1stidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator1stidaft);
			this.usageaccumulator1stvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator1stvalueaft);
			this.usageaccumulator2ndidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator2ndidaft);
			this.usageaccumulator2ndvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator2ndvalueaft);
			this.usageaccumulator3rdidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator3rdidaft);
			this.usageaccumulator3rdvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator3rdvalueaft);
			this.usageaccumulator4thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator4thidaft);
			this.usageaccumulator4thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator4thvalueaft);
			this.usageaccumulator5thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator5thidaft);
			this.usageaccumulator5thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator5thvalueaft);
			this.usageaccumulator6thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator6thidaft);
			this.usageaccumulator6thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator6thvalueaft);
			this.usageaccumulator7thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator7thidaft);
			this.usageaccumulator7thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator7thvalueaft);
			this.usageaccumulator8thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator8thidaft);
			this.usageaccumulator8thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator8thvalueaft);
			this.usageaccumulator9thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator9thidaft);
			this.usageaccumulator9thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator9thvalueaft);
			this.usageaccumulator10thidaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator10thidaft);
			this.usageaccumulator10thvalueaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.usageaccumulator10thvalueaft);
			this.communityidaft1 = com.ligadata.BaseTypes.StringImpl.Clone(other.communityidaft1);
			this.communityidaft2 = com.ligadata.BaseTypes.StringImpl.Clone(other.communityidaft2);
			this.communityidaft3 = com.ligadata.BaseTypes.StringImpl.Clone(other.communityidaft3);
			this.refillpromodivisionamount = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivisionamount);
			this.supervisiondayspromopart = com.ligadata.BaseTypes.DoubleImpl.Clone(other.supervisiondayspromopart);
			this.supervisiondayssurplus = com.ligadata.BaseTypes.DoubleImpl.Clone(other.supervisiondayssurplus);
			this.servicefeedayspromopart = com.ligadata.BaseTypes.DoubleImpl.Clone(other.servicefeedayspromopart);
			this.servicefeedayssurplus = com.ligadata.BaseTypes.DoubleImpl.Clone(other.servicefeedayssurplus);
			this.maximumservicefeeperiod = com.ligadata.BaseTypes.DoubleImpl.Clone(other.maximumservicefeeperiod);
			this.maximumsupervisionperiod = com.ligadata.BaseTypes.DoubleImpl.Clone(other.maximumsupervisionperiod);
			this.activationdate = com.ligadata.BaseTypes.StringImpl.Clone(other.activationdate);
			this.welcomestatus = com.ligadata.BaseTypes.DoubleImpl.Clone(other.welcomestatus);
			this.voucheragent = com.ligadata.BaseTypes.StringImpl.Clone(other.voucheragent);
			this.promotionplanallocstartdate = com.ligadata.BaseTypes.StringImpl.Clone(other.promotionplanallocstartdate);
			this.accountgroupid = com.ligadata.BaseTypes.StringImpl.Clone(other.accountgroupid);
			this.externaldata1 = com.ligadata.BaseTypes.StringImpl.Clone(other.externaldata1);
			this.externaldata2 = com.ligadata.BaseTypes.StringImpl.Clone(other.externaldata2);
			this.externaldata3 = com.ligadata.BaseTypes.StringImpl.Clone(other.externaldata3);
			this.externaldata4 = com.ligadata.BaseTypes.StringImpl.Clone(other.externaldata4);
			this.locationnumber = com.ligadata.BaseTypes.StringImpl.Clone(other.locationnumber);
			this.voucheractivationcode = com.ligadata.BaseTypes.StringImpl.Clone(other.voucheractivationcode);
			this.accountcurrencycleared = com.ligadata.BaseTypes.StringImpl.Clone(other.accountcurrencycleared);
			this.ignoreserviceclasshierarchy = com.ligadata.BaseTypes.StringImpl.Clone(other.ignoreserviceclasshierarchy);
			this.accounthomeregion = com.ligadata.BaseTypes.StringImpl.Clone(other.accounthomeregion);
			this.subscriberregion = com.ligadata.BaseTypes.StringImpl.Clone(other.subscriberregion);
			this.voucherregion = com.ligadata.BaseTypes.StringImpl.Clone(other.voucherregion);
			this.promotionplanallocenddate = com.ligadata.BaseTypes.StringImpl.Clone(other.promotionplanallocenddate);
			this.requestedrefilltype = com.ligadata.BaseTypes.StringImpl.Clone(other.requestedrefilltype);
			this.accountexpiry1stdatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry1stdatebef);
			this.accountexpiry1stdateaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry1stdateaft);
			this.accountexpiry2nddatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry2nddatebef);
			this.accountexpiry2nddateaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry2nddateaft);
			this.accountexpiry3rddatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry3rddatebef);
			this.accountexpiry3rddateaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry3rddateaft);
			this.accountexpiry4thdatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry4thdatebef);
			this.accountexpiry4thdateaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry4thdateaft);
			this.accountexpiry5thdatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry5thdatebef);
			this.accountexpiry5thdateaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry5thdateaft);
			this.accountexpiry6thdatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry6thdatebef);
			this.accountexpiry6thdateaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry6thdateaft);
			this.accountexpiry7thdatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry7thdatebef);
			this.accountexpiry7thdateaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry7thdateaft);
			this.accountexpiry8thdatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry8thdatebef);
			this.accountexpiry8thdateaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry8thdateaft);
			this.accountexpiry9thdatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry9thdatebef);
			this.accountexpiry9thdateaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry9thdateaft);
			this.accountexpiry10thdatebef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry10thdatebef);
			this.accountexpiry10thdateaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountexpiry10thdateaft);
			this.rechargedivpartmain = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartmain);
			this.rechargedivpartda1st = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartda1st);
			this.rechargedivpartda2nd = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartda2nd);
			this.rechargedivpartda3rd = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartda3rd);
			this.rechargedivpartda4th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartda4th);
			this.rechargedivpartda5th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartda5th);
			this.accumulatedprogressionvalueres = com.ligadata.BaseTypes.DoubleImpl.Clone(other.accumulatedprogressionvalueres);
			this.rechargedivpartpmain = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartpmain);
			this.rechargedivpartpda1st = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartpda1st);
			this.rechargedivpartpda2nd = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartpda2nd);
			this.rechargedivpartpda3rd = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartpda3rd);
			this.rechargedivpartpda4th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartpda4th);
			this.rechargedivpartpda5th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartpda5th);
			this.rechargedivpartda6th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartda6th);
			this.rechargedivpartda7th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartda7th);
			this.rechargedivpartda8th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartda8th);
			this.rechargedivpartda9th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartda9th);
			this.rechargedivpartda10th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartda10th);
			this.rechargedivpartpda6th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartpda6th);
			this.rechargedivpartpda7th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartpda7th);
			this.rechargedivpartpda8th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartpda8th);
			this.rechargedivpartpda9th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartpda9th);
			this.rechargedivpartpda10th = com.ligadata.BaseTypes.DoubleImpl.Clone(other.rechargedivpartpda10th);
			this.dedicatedaccountunit1stbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit1stbef);
			this.accountstartdate1stbef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate1stbef);
			this.refilldivunits1stbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits1stbef);
			this.refillpromodivunits1stbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits1stbef);
			this.unitbalance1stbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance1stbef);
			this.clearedunits1stbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits1stbef);
			this.realmoneyflag1stbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag1stbef);
			this.dedicatedaccountunit2ndbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit2ndbef);
			this.accountstartdate2ndbef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate2ndbef);
			this.refilldivunits2ndbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits2ndbef);
			this.refillpromodivunits2ndbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits2ndbef);
			this.unitbalance2ndbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance2ndbef);
			this.clearedunits2ndbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits2ndbef);
			this.realmoneyflag2ndbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag2ndbef);
			this.dedicatedaccountunit3rdbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit3rdbef);
			this.accountstartdate3rdbef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate3rdbef);
			this.refilldivunits3rdbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits3rdbef);
			this.refillpromodivunits3rdbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits3rdbef);
			this.unitbalance3rdbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance3rdbef);
			this.clearedunits3rdbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits3rdbef);
			this.realmoneyflag3rdbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag3rdbef);
			this.dedicatedaccountunit4thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit4thbef);
			this.accountstartdate4thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate4thbef);
			this.refilldivunits4thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits4thbef);
			this.refillpromodivunits4thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits4thbef);
			this.unitbalance4thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance4thbef);
			this.clearedunits4thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits4thbef);
			this.realmoneyflag4thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag4thbef);
			this.dedicatedaccountunit5thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit5thbef);
			this.accountstartdate5thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate5thbef);
			this.refilldivunits5thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits5thbef);
			this.refillpromodivunits5thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits5thbef);
			this.unitbalance5thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance5thbef);
			this.clearedunits5thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits5thbef);
			this.realmoneyflag5thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag5thbef);
			this.dedicatedaccountunit6thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit6thbef);
			this.accountstartdate6thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate6thbef);
			this.refilldivunits6thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits6thbef);
			this.refillpromodivunits6thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits6thbef);
			this.unitbalance6thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance6thbef);
			this.clearedunits6thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits6thbef);
			this.realmoneyflag6thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag6thbef);
			this.dedicatedaccountunit7thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit7thbef);
			this.accountstartdate7thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate7thbef);
			this.refilldivunits7thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits7thbef);
			this.refillpromodivunits7thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits7thbef);
			this.unitbalance7thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance7thbef);
			this.clearedunits7thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits7thbef);
			this.realmoneyflag7thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag7thbef);
			this.dedicatedaccountunit8thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit8thbef);
			this.accountstartdate8thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate8thbef);
			this.refilldivunits8thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits8thbef);
			this.refillpromodivunits8thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits8thbef);
			this.unitbalance8thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance8thbef);
			this.clearedunits8thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits8thbef);
			this.realmoneyflag8thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag8thbef);
			this.dedicatedaccountunit9thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit9thbef);
			this.accountstartdate9thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate9thbef);
			this.refilldivunits9thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits9thbef);
			this.refillpromodivunits9thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits9thbef);
			this.unitbalance9thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance9thbef);
			this.clearedunits9thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits9thbef);
			this.realmoneyflag9thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag9thbef);
			this.dedicatedaccountunit10thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit10thbef);
			this.accountstartdate10thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate10thbef);
			this.refilldivunits10thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits10thbef);
			this.refillpromodivunits10thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits10thbef);
			this.unitbalance10thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance10thbef);
			this.clearedunits10thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits10thbef);
			this.realmoneyflag10thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag10thbef);
			this.offer1stidentifierbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer1stidentifierbef);
			this.offerstartdate1stbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate1stbef);
			this.offerexpirydate1stbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate1stbef);
			this.offertype1stbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype1stbef);
			this.offerproductidentifier1stbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier1stbef);
			this.offerstartdatetime1stbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime1stbef);
			this.offerexpirydatetime1stbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime1stbef);
			this.offer2ndidentifierbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer2ndidentifierbef);
			this.offerstartdate2ndbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate2ndbef);
			this.offerexpirydate2ndbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate2ndbef);
			this.offertype2ndbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype2ndbef);
			this.offerproductidentifier2ndbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier2ndbef);
			this.offerstartdatetime2ndbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime2ndbef);
			this.offerexpirydatetime2ndbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime2ndbef);
			this.offer3rdidentifierbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer3rdidentifierbef);
			this.offerstartdate3rdbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate3rdbef);
			this.offerexpirydate3rdbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate3rdbef);
			this.offertype3rdbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype3rdbef);
			this.offerproductidentifier3rdbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier3rdbef);
			this.offerstartdatetime3rdbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime3rdbef);
			this.offerexpirydatetime3rdbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime3rdbef);
			this.offer4thidentifierbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer4thidentifierbef);
			this.offerstartdate4thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate4thbef);
			this.offerexpirydate4thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate4thbef);
			this.offertype4thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype4thbef);
			this.offerproductidentifier4thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier4thbef);
			this.offerstartdatetime4thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime4thbef);
			this.offerexpirydatetime4thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime4thbef);
			this.offer5thidentifierbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer5thidentifierbef);
			this.offerstartdate5thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate5thbef);
			this.offerexpirydate5thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate5thbef);
			this.offertype5thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype5thbef);
			this.offerproductidentifier5thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier5thbef);
			this.offerstartdatetime5thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime5thbef);
			this.offerexpirydatetime5thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime5thbef);
			this.offer6thidentifierbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer6thidentifierbef);
			this.offerstartdate6thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate6thbef);
			this.offerexpirydate6thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate6thbef);
			this.offertype6thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype6thbef);
			this.offerproductidentifier6thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier6thbef);
			this.offerstartdatetime6thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime6thbef);
			this.offerexpirydatetime6thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime6thbef);
			this.offer7thidentifierbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer7thidentifierbef);
			this.offerstartdate7thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate7thbef);
			this.offerexpirydate7thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate7thbef);
			this.offertype7thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype7thbef);
			this.offerproductidentifier7thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier7thbef);
			this.offerstartdatetime7thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime7thbef);
			this.offerexpirydatetime7thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime7thbef);
			this.offer8thidentifierbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer8thidentifierbef);
			this.offerstartdate8thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate8thbef);
			this.offerexpirydate8thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate8thbef);
			this.offertype8thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype8thbef);
			this.offerproductidentifier8thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier8thbef);
			this.offerstartdatetime8thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime8thbef);
			this.offerexpirydatetime8thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime8thbef);
			this.offer9thidentifierbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer9thidentifierbef);
			this.offerstartdate9thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate9thbef);
			this.offerexpirydate9thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate9thbef);
			this.offertype9thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype9thbef);
			this.offerproductidentifier9thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier9thbef);
			this.offerstartdatetime9thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime9thbef);
			this.offerexpirydatetime9thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime9thbef);
			this.offer10thidentifierbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer10thidentifierbef);
			this.offerstartdate10thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate10thbef);
			this.offerexpirydate10thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate10thbef);
			this.offertype10thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype10thbef);
			this.offerproductidentifier10thbef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier10thbef);
			this.offerstartdatetime10thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime10thbef);
			this.offerexpirydatetime10thbef = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime10thbef);
			this.aggregatedbalancebef = com.ligadata.BaseTypes.DoubleImpl.Clone(other.aggregatedbalancebef);
			this.dedicatedaccountunit1staft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit1staft);
			this.accountstartdate1staft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate1staft);
			this.refilldivunits1staft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits1staft);
			this.refillpromodivunits1staft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits1staft);
			this.unitbalance1staft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance1staft);
			this.clearedunits1staft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits1staft);
			this.realmoneyflag1staft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag1staft);
			this.dedicatedaccountunit2ndaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit2ndaft);
			this.accountstartdate2ndaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate2ndaft);
			this.refilldivunits2ndaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits2ndaft);
			this.refillpromodivunits2ndaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits2ndaft);
			this.unitbalance2ndaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance2ndaft);
			this.clearedunits2ndaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits2ndaft);
			this.realmoneyflag2ndaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag2ndaft);
			this.dedicatedaccountunit3rdaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit3rdaft);
			this.accountstartdate3rdaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate3rdaft);
			this.refilldivunits3rdaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits3rdaft);
			this.refillpromodivunits3rdaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits3rdaft);
			this.unitbalance3rdaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance3rdaft);
			this.clearedunits3rdaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits3rdaft);
			this.realmoneyflag3rdaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag3rdaft);
			this.dedicatedaccountunit4thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit4thaft);
			this.accountstartdate4thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate4thaft);
			this.refilldivunits4thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits4thaft);
			this.refillpromodivunits4thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits4thaft);
			this.unitbalance4thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance4thaft);
			this.clearedunits4thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits4thaft);
			this.realmoneyflag4thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag4thaft);
			this.dedicatedaccountunit5thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit5thaft);
			this.accountstartdate5thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate5thaft);
			this.refilldivunits5thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits5thaft);
			this.refillpromodivunits5thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits5thaft);
			this.unitbalance5thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance5thaft);
			this.clearedunits5thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits5thaft);
			this.realmoneyflag5thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag5thaft);
			this.dedicatedaccountunit6thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit6thaft);
			this.accountstartdate6thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate6thaft);
			this.refilldivunits6thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits6thaft);
			this.refillpromodivunits6thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits6thaft);
			this.unitbalance6thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance6thaft);
			this.clearedunits6thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits6thaft);
			this.realmoneyflag6thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag6thaft);
			this.dedicatedaccountunit7thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit7thaft);
			this.accountstartdate7thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate7thaft);
			this.refilldivunits7thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits7thaft);
			this.refillpromodivunits7thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits7thaft);
			this.unitbalance7thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance7thaft);
			this.clearedunits7thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits7thaft);
			this.realmoneyflag7thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag7thaft);
			this.dedicatedaccountunit8thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit8thaft);
			this.accountstartdate8thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate8thaft);
			this.refilldivunits8thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits8thaft);
			this.refillpromodivunits8thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits8thaft);
			this.unitbalance8thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance8thaft);
			this.clearedunits8thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits8thaft);
			this.realmoneyflag8thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag8thaft);
			this.dedicatedaccountunit9thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit9thaft);
			this.accountstartdate9thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate9thaft);
			this.refilldivunits9thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits9thaft);
			this.refillpromodivunits9thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits9thaft);
			this.unitbalance9thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance9thaft);
			this.clearedunits9thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits9thaft);
			this.realmoneyflag9thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag9thaft);
			this.dedicatedaccountunit10thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.dedicatedaccountunit10thaft);
			this.accountstartdate10thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.accountstartdate10thaft);
			this.refilldivunits10thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refilldivunits10thaft);
			this.refillpromodivunits10thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.refillpromodivunits10thaft);
			this.unitbalance10thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.unitbalance10thaft);
			this.clearedunits10thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.clearedunits10thaft);
			this.realmoneyflag10thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.realmoneyflag10thaft);
			this.offer1stidentifieraft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer1stidentifieraft);
			this.offerstartdate1staft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate1staft);
			this.offerexpirydate1staft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate1staft);
			this.offertype1staft = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype1staft);
			this.offerproductidentifier1staft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier1staft);
			this.offerstartdatetime1staft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime1staft);
			this.offerexpirydatetime1staft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime1staft);
			this.offer2ndidentifieraft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer2ndidentifieraft);
			this.offerstartdate2ndaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate2ndaft);
			this.offerexpirydate2ndaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate2ndaft);
			this.offertype2ndaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype2ndaft);
			this.offerproductidentifier2ndaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier2ndaft);
			this.offerstartdatetime2ndaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime2ndaft);
			this.offerexpirydatetime2ndaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime2ndaft);
			this.offer3rdidentifieraft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer3rdidentifieraft);
			this.offerstartdate3rdaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate3rdaft);
			this.offerexpirydate3rdaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate3rdaft);
			this.offertype3rdaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype3rdaft);
			this.offerproductidentifier3rdaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier3rdaft);
			this.offerstartdatetime3rdaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime3rdaft);
			this.offerexpirydatetime3rdaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime3rdaft);
			this.offer4thidentifieraft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer4thidentifieraft);
			this.offerstartdate4thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate4thaft);
			this.offerexpirydate4thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate4thaft);
			this.offertype4thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype4thaft);
			this.offerproductidentifier4thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier4thaft);
			this.offerstartdatetime4thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime4thaft);
			this.offerexpirydatetime4thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime4thaft);
			this.offer5thidentifieraft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer5thidentifieraft);
			this.offerstartdate5thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate5thaft);
			this.offerexpirydate5thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate5thaft);
			this.offertype5thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype5thaft);
			this.offerproductidentifier5thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier5thaft);
			this.offerstartdatetime5thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime5thaft);
			this.offerexpirydatetime5thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime5thaft);
			this.offer6thidentifieraft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer6thidentifieraft);
			this.offerstartdate6thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate6thaft);
			this.offerexpirydate6thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate6thaft);
			this.offertype6thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype6thaft);
			this.offerproductidentifier6thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier6thaft);
			this.offerstartdatetime6thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime6thaft);
			this.offerexpirydatetime6thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime6thaft);
			this.offer7thidentifieraft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer7thidentifieraft);
			this.offerstartdate7thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate7thaft);
			this.offerexpirydate7thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate7thaft);
			this.offertype7thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype7thaft);
			this.offerproductidentifier7thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier7thaft);
			this.offerstartdatetime7thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime7thaft);
			this.offerexpirydatetime7thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime7thaft);
			this.offer8thidentifieraft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer8thidentifieraft);
			this.offerstartdate8thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate8thaft);
			this.offerexpirydate8thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate8thaft);
			this.offertype8thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype8thaft);
			this.offerproductidentifier8thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier8thaft);
			this.offerstartdatetime8thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime8thaft);
			this.offerexpirydatetime8thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime8thaft);
			this.offer9thidentifieraft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer9thidentifieraft);
			this.offerstartdate9thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate9thaft);
			this.offerexpirydate9thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate9thaft);
			this.offertype9thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype9thaft);
			this.offerproductidentifier9thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier9thaft);
			this.offerstartdatetime9thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime9thaft);
			this.offerexpirydatetime9thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime9thaft);
			this.offer10thidentifieraft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offer10thidentifieraft);
			this.offerstartdate10thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdate10thaft);
			this.offerexpirydate10thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydate10thaft);
			this.offertype10thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offertype10thaft);
			this.offerproductidentifier10thaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.offerproductidentifier10thaft);
			this.offerstartdatetime10thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerstartdatetime10thaft);
			this.offerexpirydatetime10thaft = com.ligadata.BaseTypes.StringImpl.Clone(other.offerexpirydatetime10thaft);
			this.aggregatedbalanceaft = com.ligadata.BaseTypes.DoubleImpl.Clone(other.aggregatedbalanceaft);
			this.cellidentifier = com.ligadata.BaseTypes.StringImpl.Clone(other.cellidentifier);
			this.market_id = com.ligadata.BaseTypes.IntImpl.Clone(other.market_id);
			this.hub_id = com.ligadata.BaseTypes.IntImpl.Clone(other.hub_id);
			this.filename = com.ligadata.BaseTypes.StringImpl.Clone(other.filename);

      this.setTimePartitionData(com.ligadata.BaseTypes.LongImpl.Clone(other.getTimePartitionData));
      return this;
    }
    
	 def withoriginnodetype(value: String) : AirRefillCS5 = {
		 this.originnodetype = value 
		 return this 
 	 } 
	 def withoriginhostname(value: String) : AirRefillCS5 = {
		 this.originhostname = value 
		 return this 
 	 } 
	 def withoriginfileid(value: String) : AirRefillCS5 = {
		 this.originfileid = value 
		 return this 
 	 } 
	 def withorigintransactionid(value: String) : AirRefillCS5 = {
		 this.origintransactionid = value 
		 return this 
 	 } 
	 def withoriginoperatorid(value: String) : AirRefillCS5 = {
		 this.originoperatorid = value 
		 return this 
 	 } 
	 def withorigintimestamp(value: String) : AirRefillCS5 = {
		 this.origintimestamp = value 
		 return this 
 	 } 
	 def withhostname(value: String) : AirRefillCS5 = {
		 this.hostname = value 
		 return this 
 	 } 
	 def withlocalsequencenumber(value: Double) : AirRefillCS5 = {
		 this.localsequencenumber = value 
		 return this 
 	 } 
	 def withtimestamp(value: String) : AirRefillCS5 = {
		 this.timestamp = value 
		 return this 
 	 } 
	 def withcurrentserviceclass(value: Double) : AirRefillCS5 = {
		 this.currentserviceclass = value 
		 return this 
 	 } 
	 def withvoucherbasedrefill(value: String) : AirRefillCS5 = {
		 this.voucherbasedrefill = value 
		 return this 
 	 } 
	 def withtransactiontype(value: String) : AirRefillCS5 = {
		 this.transactiontype = value 
		 return this 
 	 } 
	 def withtransactioncode(value: String) : AirRefillCS5 = {
		 this.transactioncode = value 
		 return this 
 	 } 
	 def withtransactionamount(value: Double) : AirRefillCS5 = {
		 this.transactionamount = value 
		 return this 
 	 } 
	 def withtransactioncurrency(value: String) : AirRefillCS5 = {
		 this.transactioncurrency = value 
		 return this 
 	 } 
	 def withrefillamountconverted(value: Double) : AirRefillCS5 = {
		 this.refillamountconverted = value 
		 return this 
 	 } 
	 def withrefilldivisionamount(value: Double) : AirRefillCS5 = {
		 this.refilldivisionamount = value 
		 return this 
 	 } 
	 def withrefilltype(value: Double) : AirRefillCS5 = {
		 this.refilltype = value 
		 return this 
 	 } 
	 def withrefillprofileid(value: String) : AirRefillCS5 = {
		 this.refillprofileid = value 
		 return this 
 	 } 
	 def withsegmentationid(value: String) : AirRefillCS5 = {
		 this.segmentationid = value 
		 return this 
 	 } 
	 def withvoucherserialnumber(value: String) : AirRefillCS5 = {
		 this.voucherserialnumber = value 
		 return this 
 	 } 
	 def withvouchergroupid(value: String) : AirRefillCS5 = {
		 this.vouchergroupid = value 
		 return this 
 	 } 
	 def withaccountnumber(value: String) : AirRefillCS5 = {
		 this.accountnumber = value 
		 return this 
 	 } 
	 def withaccountcurrency(value: String) : AirRefillCS5 = {
		 this.accountcurrency = value 
		 return this 
 	 } 
	 def withsubscribernumber(value: String) : AirRefillCS5 = {
		 this.subscribernumber = value 
		 return this 
 	 } 
	 def withpromotionannouncementcode(value: String) : AirRefillCS5 = {
		 this.promotionannouncementcode = value 
		 return this 
 	 } 
	 def withaccountflagsbef(value: String) : AirRefillCS5 = {
		 this.accountflagsbef = value 
		 return this 
 	 } 
	 def withaccountbalancebef(value: Double) : AirRefillCS5 = {
		 this.accountbalancebef = value 
		 return this 
 	 } 
	 def withaccumulatedrefillvaluebef(value: Double) : AirRefillCS5 = {
		 this.accumulatedrefillvaluebef = value 
		 return this 
 	 } 
	 def withaccumulatedrefillcounterbef(value: Double) : AirRefillCS5 = {
		 this.accumulatedrefillcounterbef = value 
		 return this 
 	 } 
	 def withaccumulatedprogressionvaluebef(value: Double) : AirRefillCS5 = {
		 this.accumulatedprogressionvaluebef = value 
		 return this 
 	 } 
	 def withaccumulatedprogrcounterbef(value: Double) : AirRefillCS5 = {
		 this.accumulatedprogrcounterbef = value 
		 return this 
 	 } 
	 def withcreditclearanceperiodbef(value: Double) : AirRefillCS5 = {
		 this.creditclearanceperiodbef = value 
		 return this 
 	 } 
	 def withdedicatedaccount1stidbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount1stidbef = value 
		 return this 
 	 } 
	 def withaccount1stcampaignidentbef(value: Double) : AirRefillCS5 = {
		 this.account1stcampaignidentbef = value 
		 return this 
 	 } 
	 def withaccount1strefilldivamountbef(value: Double) : AirRefillCS5 = {
		 this.account1strefilldivamountbef = value 
		 return this 
 	 } 
	 def withaccount1strefillpromdivamntbef(value: Double) : AirRefillCS5 = {
		 this.account1strefillpromdivamntbef = value 
		 return this 
 	 } 
	 def withaccount1stbalancebef(value: Double) : AirRefillCS5 = {
		 this.account1stbalancebef = value 
		 return this 
 	 } 
	 def withclearedaccount1stvaluebef(value: Double) : AirRefillCS5 = {
		 this.clearedaccount1stvaluebef = value 
		 return this 
 	 } 
	 def withdedicatedaccount2ndidbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount2ndidbef = value 
		 return this 
 	 } 
	 def withaccount2ndcampaignidentifbef(value: Double) : AirRefillCS5 = {
		 this.account2ndcampaignidentifbef = value 
		 return this 
 	 } 
	 def withaccount2ndrefilldivamountbef(value: Double) : AirRefillCS5 = {
		 this.account2ndrefilldivamountbef = value 
		 return this 
 	 } 
	 def withaccount2ndrefilpromodivamntbef(value: Double) : AirRefillCS5 = {
		 this.account2ndrefilpromodivamntbef = value 
		 return this 
 	 } 
	 def withaccount2ndbalancebef(value: Double) : AirRefillCS5 = {
		 this.account2ndbalancebef = value 
		 return this 
 	 } 
	 def withclearedaccount2ndvaluebef(value: Double) : AirRefillCS5 = {
		 this.clearedaccount2ndvaluebef = value 
		 return this 
 	 } 
	 def withdedicatedaccount3rdidbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount3rdidbef = value 
		 return this 
 	 } 
	 def withaccount3rdcampaignidentbef(value: Double) : AirRefillCS5 = {
		 this.account3rdcampaignidentbef = value 
		 return this 
 	 } 
	 def withaccount3rdrefilldivamountbef(value: Double) : AirRefillCS5 = {
		 this.account3rdrefilldivamountbef = value 
		 return this 
 	 } 
	 def withaccount3rdrefilpromodivamntbef(value: Double) : AirRefillCS5 = {
		 this.account3rdrefilpromodivamntbef = value 
		 return this 
 	 } 
	 def withaccount3rdbalancebef(value: Double) : AirRefillCS5 = {
		 this.account3rdbalancebef = value 
		 return this 
 	 } 
	 def withclearedaccount3rdvaluebef(value: Double) : AirRefillCS5 = {
		 this.clearedaccount3rdvaluebef = value 
		 return this 
 	 } 
	 def withdedicatedaccount4thidbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount4thidbef = value 
		 return this 
 	 } 
	 def withaccount4thcampaignidentbef(value: Double) : AirRefillCS5 = {
		 this.account4thcampaignidentbef = value 
		 return this 
 	 } 
	 def withaccount4threfilldivamountbef(value: Double) : AirRefillCS5 = {
		 this.account4threfilldivamountbef = value 
		 return this 
 	 } 
	 def withaccount4threfilpromodivamntbef(value: Double) : AirRefillCS5 = {
		 this.account4threfilpromodivamntbef = value 
		 return this 
 	 } 
	 def withaccount4thbalancebef(value: Double) : AirRefillCS5 = {
		 this.account4thbalancebef = value 
		 return this 
 	 } 
	 def withclearedaccount4thvaluebef(value: Double) : AirRefillCS5 = {
		 this.clearedaccount4thvaluebef = value 
		 return this 
 	 } 
	 def withdedicatedaccount5thidbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount5thidbef = value 
		 return this 
 	 } 
	 def withaccount5thcampaignidentbef(value: Double) : AirRefillCS5 = {
		 this.account5thcampaignidentbef = value 
		 return this 
 	 } 
	 def withaccount5threfilldivamountbef(value: Double) : AirRefillCS5 = {
		 this.account5threfilldivamountbef = value 
		 return this 
 	 } 
	 def withaccount5threfilpromodivamntbef(value: Double) : AirRefillCS5 = {
		 this.account5threfilpromodivamntbef = value 
		 return this 
 	 } 
	 def withaccount5thbalancebef(value: Double) : AirRefillCS5 = {
		 this.account5thbalancebef = value 
		 return this 
 	 } 
	 def withclearedaccount5thvaluebef(value: Double) : AirRefillCS5 = {
		 this.clearedaccount5thvaluebef = value 
		 return this 
 	 } 
	 def withdedicatedaccount6thidbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount6thidbef = value 
		 return this 
 	 } 
	 def withaccount6thcampaignidentbef(value: Double) : AirRefillCS5 = {
		 this.account6thcampaignidentbef = value 
		 return this 
 	 } 
	 def withaccount6threfilldivamountbef(value: Double) : AirRefillCS5 = {
		 this.account6threfilldivamountbef = value 
		 return this 
 	 } 
	 def withaccount6threfilpromodivamntbef(value: Double) : AirRefillCS5 = {
		 this.account6threfilpromodivamntbef = value 
		 return this 
 	 } 
	 def withaccount6thbalancebef(value: Double) : AirRefillCS5 = {
		 this.account6thbalancebef = value 
		 return this 
 	 } 
	 def withclearedaccount6thvaluebef(value: Double) : AirRefillCS5 = {
		 this.clearedaccount6thvaluebef = value 
		 return this 
 	 } 
	 def withdedicatedaccount7thidbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount7thidbef = value 
		 return this 
 	 } 
	 def withaccount7thcampaignidentbef(value: Double) : AirRefillCS5 = {
		 this.account7thcampaignidentbef = value 
		 return this 
 	 } 
	 def withaccount7threfilldivamountbef(value: Double) : AirRefillCS5 = {
		 this.account7threfilldivamountbef = value 
		 return this 
 	 } 
	 def withaccount7threfilpromodivamntbef(value: Double) : AirRefillCS5 = {
		 this.account7threfilpromodivamntbef = value 
		 return this 
 	 } 
	 def withaccount7thbalancebef(value: Double) : AirRefillCS5 = {
		 this.account7thbalancebef = value 
		 return this 
 	 } 
	 def withclearedaccount7thvaluebef(value: Double) : AirRefillCS5 = {
		 this.clearedaccount7thvaluebef = value 
		 return this 
 	 } 
	 def withdedicatedaccount8thidbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount8thidbef = value 
		 return this 
 	 } 
	 def withaccount8thcampaignidentbef(value: Double) : AirRefillCS5 = {
		 this.account8thcampaignidentbef = value 
		 return this 
 	 } 
	 def withaccount8threfilldivamountbef(value: Double) : AirRefillCS5 = {
		 this.account8threfilldivamountbef = value 
		 return this 
 	 } 
	 def withaccount8threfilpromodivamntbef(value: Double) : AirRefillCS5 = {
		 this.account8threfilpromodivamntbef = value 
		 return this 
 	 } 
	 def withaccount8thbalancebef(value: Double) : AirRefillCS5 = {
		 this.account8thbalancebef = value 
		 return this 
 	 } 
	 def withclearedaccount8thvaluebef(value: Double) : AirRefillCS5 = {
		 this.clearedaccount8thvaluebef = value 
		 return this 
 	 } 
	 def withdedicatedaccount9thidbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount9thidbef = value 
		 return this 
 	 } 
	 def withaccount9thcampaignidentbef(value: Double) : AirRefillCS5 = {
		 this.account9thcampaignidentbef = value 
		 return this 
 	 } 
	 def withaccount9threfilldivamountbef(value: Double) : AirRefillCS5 = {
		 this.account9threfilldivamountbef = value 
		 return this 
 	 } 
	 def withaccount9threfilpromodivamntbef(value: Double) : AirRefillCS5 = {
		 this.account9threfilpromodivamntbef = value 
		 return this 
 	 } 
	 def withaccount9thbalancebef(value: Double) : AirRefillCS5 = {
		 this.account9thbalancebef = value 
		 return this 
 	 } 
	 def withclearedaccount9thvaluebef(value: Double) : AirRefillCS5 = {
		 this.clearedaccount9thvaluebef = value 
		 return this 
 	 } 
	 def withdedicatedaccount10thidbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount10thidbef = value 
		 return this 
 	 } 
	 def withaccount10thcampaignidentbef(value: Double) : AirRefillCS5 = {
		 this.account10thcampaignidentbef = value 
		 return this 
 	 } 
	 def withaccount10threfilldivamountbef(value: Double) : AirRefillCS5 = {
		 this.account10threfilldivamountbef = value 
		 return this 
 	 } 
	 def withaccount10threfilpromdivamntbef(value: Double) : AirRefillCS5 = {
		 this.account10threfilpromdivamntbef = value 
		 return this 
 	 } 
	 def withaccount10thbalancebef(value: Double) : AirRefillCS5 = {
		 this.account10thbalancebef = value 
		 return this 
 	 } 
	 def withclearedaccount10thvaluebef(value: Double) : AirRefillCS5 = {
		 this.clearedaccount10thvaluebef = value 
		 return this 
 	 } 
	 def withpromotionplan(value: String) : AirRefillCS5 = {
		 this.promotionplan = value 
		 return this 
 	 } 
	 def withpermanentserviceclassbef(value: Double) : AirRefillCS5 = {
		 this.permanentserviceclassbef = value 
		 return this 
 	 } 
	 def withtemporaryserviceclassbef(value: Double) : AirRefillCS5 = {
		 this.temporaryserviceclassbef = value 
		 return this 
 	 } 
	 def withtemporaryservclassexpdatebef(value: String) : AirRefillCS5 = {
		 this.temporaryservclassexpdatebef = value 
		 return this 
 	 } 
	 def withrefilloptionbef(value: String) : AirRefillCS5 = {
		 this.refilloptionbef = value 
		 return this 
 	 } 
	 def withservicefeeexpirydatebef(value: String) : AirRefillCS5 = {
		 this.servicefeeexpirydatebef = value 
		 return this 
 	 } 
	 def withserviceremovalgraceperiodbef(value: Double) : AirRefillCS5 = {
		 this.serviceremovalgraceperiodbef = value 
		 return this 
 	 } 
	 def withserviceofferingbef(value: String) : AirRefillCS5 = {
		 this.serviceofferingbef = value 
		 return this 
 	 } 
	 def withsupervisionexpirydatebef(value: String) : AirRefillCS5 = {
		 this.supervisionexpirydatebef = value 
		 return this 
 	 } 
	 def withusageaccumulator1stidbef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator1stidbef = value 
		 return this 
 	 } 
	 def withusageaccumulator1stvaluebef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator1stvaluebef = value 
		 return this 
 	 } 
	 def withusageaccumulator2ndidbef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator2ndidbef = value 
		 return this 
 	 } 
	 def withusageaccumulator2ndvaluebef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator2ndvaluebef = value 
		 return this 
 	 } 
	 def withusageaccumulator3rdidbef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator3rdidbef = value 
		 return this 
 	 } 
	 def withusageaccumulator3rdvaluebef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator3rdvaluebef = value 
		 return this 
 	 } 
	 def withusageaccumulator4thidbef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator4thidbef = value 
		 return this 
 	 } 
	 def withusageaccumulator4thvaluebef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator4thvaluebef = value 
		 return this 
 	 } 
	 def withusageaccumulator5thidbef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator5thidbef = value 
		 return this 
 	 } 
	 def withusageaccumulator5thvaluebef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator5thvaluebef = value 
		 return this 
 	 } 
	 def withusageaccumulator6thidbef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator6thidbef = value 
		 return this 
 	 } 
	 def withusageaccumulator6thvaluebef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator6thvaluebef = value 
		 return this 
 	 } 
	 def withusageaccumulator7thidbef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator7thidbef = value 
		 return this 
 	 } 
	 def withusageaccumulator7thvaluebef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator7thvaluebef = value 
		 return this 
 	 } 
	 def withusageaccumulator8thidbef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator8thidbef = value 
		 return this 
 	 } 
	 def withusageaccumulator8thvaluebef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator8thvaluebef = value 
		 return this 
 	 } 
	 def withusageaccumulator9thidbef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator9thidbef = value 
		 return this 
 	 } 
	 def withusageaccumulator9thvaluebef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator9thvaluebef = value 
		 return this 
 	 } 
	 def withusageaccumulator10thidbef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator10thidbef = value 
		 return this 
 	 } 
	 def withusageaccumulator10thvaluebef(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator10thvaluebef = value 
		 return this 
 	 } 
	 def withcommunityidbef1(value: String) : AirRefillCS5 = {
		 this.communityidbef1 = value 
		 return this 
 	 } 
	 def withcommunityidbef2(value: String) : AirRefillCS5 = {
		 this.communityidbef2 = value 
		 return this 
 	 } 
	 def withcommunityidbef3(value: String) : AirRefillCS5 = {
		 this.communityidbef3 = value 
		 return this 
 	 } 
	 def withaccountflagsaft(value: String) : AirRefillCS5 = {
		 this.accountflagsaft = value 
		 return this 
 	 } 
	 def withaccountbalanceaft(value: Double) : AirRefillCS5 = {
		 this.accountbalanceaft = value 
		 return this 
 	 } 
	 def withaccumulatedrefillvalueaft(value: Double) : AirRefillCS5 = {
		 this.accumulatedrefillvalueaft = value 
		 return this 
 	 } 
	 def withaccumulatedrefillcounteraft(value: Double) : AirRefillCS5 = {
		 this.accumulatedrefillcounteraft = value 
		 return this 
 	 } 
	 def withaccumulatedprogressionvalueaft(value: Double) : AirRefillCS5 = {
		 this.accumulatedprogressionvalueaft = value 
		 return this 
 	 } 
	 def withaccumulatedprogconteraft(value: Double) : AirRefillCS5 = {
		 this.accumulatedprogconteraft = value 
		 return this 
 	 } 
	 def withcreditclearanceperiodaft(value: Double) : AirRefillCS5 = {
		 this.creditclearanceperiodaft = value 
		 return this 
 	 } 
	 def withdedicatedaccount1stidaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount1stidaft = value 
		 return this 
 	 } 
	 def withaccount1stcampaignidentaft(value: Double) : AirRefillCS5 = {
		 this.account1stcampaignidentaft = value 
		 return this 
 	 } 
	 def withaccount1strefilldivamountaft(value: Double) : AirRefillCS5 = {
		 this.account1strefilldivamountaft = value 
		 return this 
 	 } 
	 def withaccount1strefilpromodivamntaft(value: Double) : AirRefillCS5 = {
		 this.account1strefilpromodivamntaft = value 
		 return this 
 	 } 
	 def withaccount1stbalanceaft(value: Double) : AirRefillCS5 = {
		 this.account1stbalanceaft = value 
		 return this 
 	 } 
	 def withclearedaccount1stvalueaft(value: Double) : AirRefillCS5 = {
		 this.clearedaccount1stvalueaft = value 
		 return this 
 	 } 
	 def withdedicatedaccount2ndidaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount2ndidaft = value 
		 return this 
 	 } 
	 def withaccount2ndcampaignidentaft(value: Double) : AirRefillCS5 = {
		 this.account2ndcampaignidentaft = value 
		 return this 
 	 } 
	 def withaccount2ndrefilldivamountaft(value: Double) : AirRefillCS5 = {
		 this.account2ndrefilldivamountaft = value 
		 return this 
 	 } 
	 def withaccount2ndrefilpromodivamntaft(value: Double) : AirRefillCS5 = {
		 this.account2ndrefilpromodivamntaft = value 
		 return this 
 	 } 
	 def withaccount2ndbalanceaft(value: Double) : AirRefillCS5 = {
		 this.account2ndbalanceaft = value 
		 return this 
 	 } 
	 def withclearedaccount2ndvalueaft(value: Double) : AirRefillCS5 = {
		 this.clearedaccount2ndvalueaft = value 
		 return this 
 	 } 
	 def withdedicatedaccount3rdidaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount3rdidaft = value 
		 return this 
 	 } 
	 def withaccount3rdcampaignidentaft(value: Double) : AirRefillCS5 = {
		 this.account3rdcampaignidentaft = value 
		 return this 
 	 } 
	 def withaccount3rdrefilldivamountaft(value: Double) : AirRefillCS5 = {
		 this.account3rdrefilldivamountaft = value 
		 return this 
 	 } 
	 def withaccount3rdrefilpromodivamntaft(value: Double) : AirRefillCS5 = {
		 this.account3rdrefilpromodivamntaft = value 
		 return this 
 	 } 
	 def withaccount3rdbalanceaft(value: Double) : AirRefillCS5 = {
		 this.account3rdbalanceaft = value 
		 return this 
 	 } 
	 def withclearedaccount3rdvalueaft(value: Double) : AirRefillCS5 = {
		 this.clearedaccount3rdvalueaft = value 
		 return this 
 	 } 
	 def withdedicatedaccount4thidaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount4thidaft = value 
		 return this 
 	 } 
	 def withaccount4thcampaignidentaft(value: Double) : AirRefillCS5 = {
		 this.account4thcampaignidentaft = value 
		 return this 
 	 } 
	 def withaccount4threfilldivamountaft(value: Double) : AirRefillCS5 = {
		 this.account4threfilldivamountaft = value 
		 return this 
 	 } 
	 def withaccount4threfilpromodivamntaft(value: Double) : AirRefillCS5 = {
		 this.account4threfilpromodivamntaft = value 
		 return this 
 	 } 
	 def withaccount4thbalanceaft(value: Double) : AirRefillCS5 = {
		 this.account4thbalanceaft = value 
		 return this 
 	 } 
	 def withclearedaccount4thvalueaft(value: Double) : AirRefillCS5 = {
		 this.clearedaccount4thvalueaft = value 
		 return this 
 	 } 
	 def withdedicatedaccount5thidaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount5thidaft = value 
		 return this 
 	 } 
	 def withaccount5thcampaignidentaft(value: Double) : AirRefillCS5 = {
		 this.account5thcampaignidentaft = value 
		 return this 
 	 } 
	 def withaccount5threfilldivamountaft(value: Double) : AirRefillCS5 = {
		 this.account5threfilldivamountaft = value 
		 return this 
 	 } 
	 def withaccount5threfilpromodivamntaft(value: Double) : AirRefillCS5 = {
		 this.account5threfilpromodivamntaft = value 
		 return this 
 	 } 
	 def withaccount5thbalanceaft(value: Double) : AirRefillCS5 = {
		 this.account5thbalanceaft = value 
		 return this 
 	 } 
	 def withclearedaccount5thvalueaft(value: Double) : AirRefillCS5 = {
		 this.clearedaccount5thvalueaft = value 
		 return this 
 	 } 
	 def withdedicatedaccount6thidaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount6thidaft = value 
		 return this 
 	 } 
	 def withaccount6thcampaignidentaft(value: Double) : AirRefillCS5 = {
		 this.account6thcampaignidentaft = value 
		 return this 
 	 } 
	 def withaccount6threfilldivamountaft(value: Double) : AirRefillCS5 = {
		 this.account6threfilldivamountaft = value 
		 return this 
 	 } 
	 def withaccount6threfilpromodivamntaft(value: Double) : AirRefillCS5 = {
		 this.account6threfilpromodivamntaft = value 
		 return this 
 	 } 
	 def withaccount6thbalanceaft(value: Double) : AirRefillCS5 = {
		 this.account6thbalanceaft = value 
		 return this 
 	 } 
	 def withclearedaccount6thvalueaft(value: Double) : AirRefillCS5 = {
		 this.clearedaccount6thvalueaft = value 
		 return this 
 	 } 
	 def withdedicatedaccount7thidaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount7thidaft = value 
		 return this 
 	 } 
	 def withaccount7thcampaignidentaft(value: Double) : AirRefillCS5 = {
		 this.account7thcampaignidentaft = value 
		 return this 
 	 } 
	 def withaccount7threfilldivamountaft(value: Double) : AirRefillCS5 = {
		 this.account7threfilldivamountaft = value 
		 return this 
 	 } 
	 def withaccount7threfilpromodivamntaft(value: Double) : AirRefillCS5 = {
		 this.account7threfilpromodivamntaft = value 
		 return this 
 	 } 
	 def withaccount7thbalanceaft(value: Double) : AirRefillCS5 = {
		 this.account7thbalanceaft = value 
		 return this 
 	 } 
	 def withclearedaccount7thvalueaft(value: Double) : AirRefillCS5 = {
		 this.clearedaccount7thvalueaft = value 
		 return this 
 	 } 
	 def withdedicatedaccount8thidaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount8thidaft = value 
		 return this 
 	 } 
	 def withaccount8thcampaignidentaft(value: Double) : AirRefillCS5 = {
		 this.account8thcampaignidentaft = value 
		 return this 
 	 } 
	 def withaccount8threfilldivamountaft(value: Double) : AirRefillCS5 = {
		 this.account8threfilldivamountaft = value 
		 return this 
 	 } 
	 def withaccount8threfilpromodivamntaft(value: Double) : AirRefillCS5 = {
		 this.account8threfilpromodivamntaft = value 
		 return this 
 	 } 
	 def withaccount8thbalanceaft(value: Double) : AirRefillCS5 = {
		 this.account8thbalanceaft = value 
		 return this 
 	 } 
	 def withclearedaccount8thvalueaft(value: Double) : AirRefillCS5 = {
		 this.clearedaccount8thvalueaft = value 
		 return this 
 	 } 
	 def withdedicatedaccount9thidaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount9thidaft = value 
		 return this 
 	 } 
	 def withaccount9thcampaignidentaft(value: Double) : AirRefillCS5 = {
		 this.account9thcampaignidentaft = value 
		 return this 
 	 } 
	 def withaccount9threfilldivamountaft(value: Double) : AirRefillCS5 = {
		 this.account9threfilldivamountaft = value 
		 return this 
 	 } 
	 def withaccount9threfilpromodivamntaft(value: Double) : AirRefillCS5 = {
		 this.account9threfilpromodivamntaft = value 
		 return this 
 	 } 
	 def withaccount9thbalanceaft(value: Double) : AirRefillCS5 = {
		 this.account9thbalanceaft = value 
		 return this 
 	 } 
	 def withclearedaccount9thvalueaft(value: Double) : AirRefillCS5 = {
		 this.clearedaccount9thvalueaft = value 
		 return this 
 	 } 
	 def withdedicatedaccount10thidaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccount10thidaft = value 
		 return this 
 	 } 
	 def withaccount10thcampaignidentaft(value: Double) : AirRefillCS5 = {
		 this.account10thcampaignidentaft = value 
		 return this 
 	 } 
	 def withaccount10threfilldivamountaft(value: Double) : AirRefillCS5 = {
		 this.account10threfilldivamountaft = value 
		 return this 
 	 } 
	 def withaccount10threfilpromdivamntaft(value: Double) : AirRefillCS5 = {
		 this.account10threfilpromdivamntaft = value 
		 return this 
 	 } 
	 def withaccount10thbalanceaft(value: Double) : AirRefillCS5 = {
		 this.account10thbalanceaft = value 
		 return this 
 	 } 
	 def withclearedaccount10thvalueaft(value: Double) : AirRefillCS5 = {
		 this.clearedaccount10thvalueaft = value 
		 return this 
 	 } 
	 def withpromotionplanaft(value: String) : AirRefillCS5 = {
		 this.promotionplanaft = value 
		 return this 
 	 } 
	 def withpermanentserviceclassaft(value: Double) : AirRefillCS5 = {
		 this.permanentserviceclassaft = value 
		 return this 
 	 } 
	 def withtemporaryserviceclassaft(value: Double) : AirRefillCS5 = {
		 this.temporaryserviceclassaft = value 
		 return this 
 	 } 
	 def withtemporaryservclasexpirydateaft(value: Double) : AirRefillCS5 = {
		 this.temporaryservclasexpirydateaft = value 
		 return this 
 	 } 
	 def withrefilloptionaft(value: String) : AirRefillCS5 = {
		 this.refilloptionaft = value 
		 return this 
 	 } 
	 def withservicefeeexpirydateaft(value: String) : AirRefillCS5 = {
		 this.servicefeeexpirydateaft = value 
		 return this 
 	 } 
	 def withserviceremovalgraceperiodaft(value: Double) : AirRefillCS5 = {
		 this.serviceremovalgraceperiodaft = value 
		 return this 
 	 } 
	 def withserviceofferingaft(value: String) : AirRefillCS5 = {
		 this.serviceofferingaft = value 
		 return this 
 	 } 
	 def withsupervisionexpirydateaft(value: String) : AirRefillCS5 = {
		 this.supervisionexpirydateaft = value 
		 return this 
 	 } 
	 def withusageaccumulator1stidaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator1stidaft = value 
		 return this 
 	 } 
	 def withusageaccumulator1stvalueaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator1stvalueaft = value 
		 return this 
 	 } 
	 def withusageaccumulator2ndidaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator2ndidaft = value 
		 return this 
 	 } 
	 def withusageaccumulator2ndvalueaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator2ndvalueaft = value 
		 return this 
 	 } 
	 def withusageaccumulator3rdidaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator3rdidaft = value 
		 return this 
 	 } 
	 def withusageaccumulator3rdvalueaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator3rdvalueaft = value 
		 return this 
 	 } 
	 def withusageaccumulator4thidaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator4thidaft = value 
		 return this 
 	 } 
	 def withusageaccumulator4thvalueaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator4thvalueaft = value 
		 return this 
 	 } 
	 def withusageaccumulator5thidaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator5thidaft = value 
		 return this 
 	 } 
	 def withusageaccumulator5thvalueaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator5thvalueaft = value 
		 return this 
 	 } 
	 def withusageaccumulator6thidaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator6thidaft = value 
		 return this 
 	 } 
	 def withusageaccumulator6thvalueaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator6thvalueaft = value 
		 return this 
 	 } 
	 def withusageaccumulator7thidaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator7thidaft = value 
		 return this 
 	 } 
	 def withusageaccumulator7thvalueaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator7thvalueaft = value 
		 return this 
 	 } 
	 def withusageaccumulator8thidaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator8thidaft = value 
		 return this 
 	 } 
	 def withusageaccumulator8thvalueaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator8thvalueaft = value 
		 return this 
 	 } 
	 def withusageaccumulator9thidaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator9thidaft = value 
		 return this 
 	 } 
	 def withusageaccumulator9thvalueaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator9thvalueaft = value 
		 return this 
 	 } 
	 def withusageaccumulator10thidaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator10thidaft = value 
		 return this 
 	 } 
	 def withusageaccumulator10thvalueaft(value: Double) : AirRefillCS5 = {
		 this.usageaccumulator10thvalueaft = value 
		 return this 
 	 } 
	 def withcommunityidaft1(value: String) : AirRefillCS5 = {
		 this.communityidaft1 = value 
		 return this 
 	 } 
	 def withcommunityidaft2(value: String) : AirRefillCS5 = {
		 this.communityidaft2 = value 
		 return this 
 	 } 
	 def withcommunityidaft3(value: String) : AirRefillCS5 = {
		 this.communityidaft3 = value 
		 return this 
 	 } 
	 def withrefillpromodivisionamount(value: Double) : AirRefillCS5 = {
		 this.refillpromodivisionamount = value 
		 return this 
 	 } 
	 def withsupervisiondayspromopart(value: Double) : AirRefillCS5 = {
		 this.supervisiondayspromopart = value 
		 return this 
 	 } 
	 def withsupervisiondayssurplus(value: Double) : AirRefillCS5 = {
		 this.supervisiondayssurplus = value 
		 return this 
 	 } 
	 def withservicefeedayspromopart(value: Double) : AirRefillCS5 = {
		 this.servicefeedayspromopart = value 
		 return this 
 	 } 
	 def withservicefeedayssurplus(value: Double) : AirRefillCS5 = {
		 this.servicefeedayssurplus = value 
		 return this 
 	 } 
	 def withmaximumservicefeeperiod(value: Double) : AirRefillCS5 = {
		 this.maximumservicefeeperiod = value 
		 return this 
 	 } 
	 def withmaximumsupervisionperiod(value: Double) : AirRefillCS5 = {
		 this.maximumsupervisionperiod = value 
		 return this 
 	 } 
	 def withactivationdate(value: String) : AirRefillCS5 = {
		 this.activationdate = value 
		 return this 
 	 } 
	 def withwelcomestatus(value: Double) : AirRefillCS5 = {
		 this.welcomestatus = value 
		 return this 
 	 } 
	 def withvoucheragent(value: String) : AirRefillCS5 = {
		 this.voucheragent = value 
		 return this 
 	 } 
	 def withpromotionplanallocstartdate(value: String) : AirRefillCS5 = {
		 this.promotionplanallocstartdate = value 
		 return this 
 	 } 
	 def withaccountgroupid(value: String) : AirRefillCS5 = {
		 this.accountgroupid = value 
		 return this 
 	 } 
	 def withexternaldata1(value: String) : AirRefillCS5 = {
		 this.externaldata1 = value 
		 return this 
 	 } 
	 def withexternaldata2(value: String) : AirRefillCS5 = {
		 this.externaldata2 = value 
		 return this 
 	 } 
	 def withexternaldata3(value: String) : AirRefillCS5 = {
		 this.externaldata3 = value 
		 return this 
 	 } 
	 def withexternaldata4(value: String) : AirRefillCS5 = {
		 this.externaldata4 = value 
		 return this 
 	 } 
	 def withlocationnumber(value: String) : AirRefillCS5 = {
		 this.locationnumber = value 
		 return this 
 	 } 
	 def withvoucheractivationcode(value: String) : AirRefillCS5 = {
		 this.voucheractivationcode = value 
		 return this 
 	 } 
	 def withaccountcurrencycleared(value: String) : AirRefillCS5 = {
		 this.accountcurrencycleared = value 
		 return this 
 	 } 
	 def withignoreserviceclasshierarchy(value: String) : AirRefillCS5 = {
		 this.ignoreserviceclasshierarchy = value 
		 return this 
 	 } 
	 def withaccounthomeregion(value: String) : AirRefillCS5 = {
		 this.accounthomeregion = value 
		 return this 
 	 } 
	 def withsubscriberregion(value: String) : AirRefillCS5 = {
		 this.subscriberregion = value 
		 return this 
 	 } 
	 def withvoucherregion(value: String) : AirRefillCS5 = {
		 this.voucherregion = value 
		 return this 
 	 } 
	 def withpromotionplanallocenddate(value: String) : AirRefillCS5 = {
		 this.promotionplanallocenddate = value 
		 return this 
 	 } 
	 def withrequestedrefilltype(value: String) : AirRefillCS5 = {
		 this.requestedrefilltype = value 
		 return this 
 	 } 
	 def withaccountexpiry1stdatebef(value: String) : AirRefillCS5 = {
		 this.accountexpiry1stdatebef = value 
		 return this 
 	 } 
	 def withaccountexpiry1stdateaft(value: String) : AirRefillCS5 = {
		 this.accountexpiry1stdateaft = value 
		 return this 
 	 } 
	 def withaccountexpiry2nddatebef(value: String) : AirRefillCS5 = {
		 this.accountexpiry2nddatebef = value 
		 return this 
 	 } 
	 def withaccountexpiry2nddateaft(value: String) : AirRefillCS5 = {
		 this.accountexpiry2nddateaft = value 
		 return this 
 	 } 
	 def withaccountexpiry3rddatebef(value: String) : AirRefillCS5 = {
		 this.accountexpiry3rddatebef = value 
		 return this 
 	 } 
	 def withaccountexpiry3rddateaft(value: String) : AirRefillCS5 = {
		 this.accountexpiry3rddateaft = value 
		 return this 
 	 } 
	 def withaccountexpiry4thdatebef(value: String) : AirRefillCS5 = {
		 this.accountexpiry4thdatebef = value 
		 return this 
 	 } 
	 def withaccountexpiry4thdateaft(value: String) : AirRefillCS5 = {
		 this.accountexpiry4thdateaft = value 
		 return this 
 	 } 
	 def withaccountexpiry5thdatebef(value: String) : AirRefillCS5 = {
		 this.accountexpiry5thdatebef = value 
		 return this 
 	 } 
	 def withaccountexpiry5thdateaft(value: String) : AirRefillCS5 = {
		 this.accountexpiry5thdateaft = value 
		 return this 
 	 } 
	 def withaccountexpiry6thdatebef(value: String) : AirRefillCS5 = {
		 this.accountexpiry6thdatebef = value 
		 return this 
 	 } 
	 def withaccountexpiry6thdateaft(value: String) : AirRefillCS5 = {
		 this.accountexpiry6thdateaft = value 
		 return this 
 	 } 
	 def withaccountexpiry7thdatebef(value: String) : AirRefillCS5 = {
		 this.accountexpiry7thdatebef = value 
		 return this 
 	 } 
	 def withaccountexpiry7thdateaft(value: String) : AirRefillCS5 = {
		 this.accountexpiry7thdateaft = value 
		 return this 
 	 } 
	 def withaccountexpiry8thdatebef(value: String) : AirRefillCS5 = {
		 this.accountexpiry8thdatebef = value 
		 return this 
 	 } 
	 def withaccountexpiry8thdateaft(value: String) : AirRefillCS5 = {
		 this.accountexpiry8thdateaft = value 
		 return this 
 	 } 
	 def withaccountexpiry9thdatebef(value: String) : AirRefillCS5 = {
		 this.accountexpiry9thdatebef = value 
		 return this 
 	 } 
	 def withaccountexpiry9thdateaft(value: String) : AirRefillCS5 = {
		 this.accountexpiry9thdateaft = value 
		 return this 
 	 } 
	 def withaccountexpiry10thdatebef(value: String) : AirRefillCS5 = {
		 this.accountexpiry10thdatebef = value 
		 return this 
 	 } 
	 def withaccountexpiry10thdateaft(value: String) : AirRefillCS5 = {
		 this.accountexpiry10thdateaft = value 
		 return this 
 	 } 
	 def withrechargedivpartmain(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartmain = value 
		 return this 
 	 } 
	 def withrechargedivpartda1st(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartda1st = value 
		 return this 
 	 } 
	 def withrechargedivpartda2nd(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartda2nd = value 
		 return this 
 	 } 
	 def withrechargedivpartda3rd(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartda3rd = value 
		 return this 
 	 } 
	 def withrechargedivpartda4th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartda4th = value 
		 return this 
 	 } 
	 def withrechargedivpartda5th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartda5th = value 
		 return this 
 	 } 
	 def withaccumulatedprogressionvalueres(value: Double) : AirRefillCS5 = {
		 this.accumulatedprogressionvalueres = value 
		 return this 
 	 } 
	 def withrechargedivpartpmain(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartpmain = value 
		 return this 
 	 } 
	 def withrechargedivpartpda1st(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartpda1st = value 
		 return this 
 	 } 
	 def withrechargedivpartpda2nd(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartpda2nd = value 
		 return this 
 	 } 
	 def withrechargedivpartpda3rd(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartpda3rd = value 
		 return this 
 	 } 
	 def withrechargedivpartpda4th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartpda4th = value 
		 return this 
 	 } 
	 def withrechargedivpartpda5th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartpda5th = value 
		 return this 
 	 } 
	 def withrechargedivpartda6th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartda6th = value 
		 return this 
 	 } 
	 def withrechargedivpartda7th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartda7th = value 
		 return this 
 	 } 
	 def withrechargedivpartda8th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartda8th = value 
		 return this 
 	 } 
	 def withrechargedivpartda9th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartda9th = value 
		 return this 
 	 } 
	 def withrechargedivpartda10th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartda10th = value 
		 return this 
 	 } 
	 def withrechargedivpartpda6th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartpda6th = value 
		 return this 
 	 } 
	 def withrechargedivpartpda7th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartpda7th = value 
		 return this 
 	 } 
	 def withrechargedivpartpda8th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartpda8th = value 
		 return this 
 	 } 
	 def withrechargedivpartpda9th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartpda9th = value 
		 return this 
 	 } 
	 def withrechargedivpartpda10th(value: Double) : AirRefillCS5 = {
		 this.rechargedivpartpda10th = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit1stbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit1stbef = value 
		 return this 
 	 } 
	 def withaccountstartdate1stbef(value: String) : AirRefillCS5 = {
		 this.accountstartdate1stbef = value 
		 return this 
 	 } 
	 def withrefilldivunits1stbef(value: Double) : AirRefillCS5 = {
		 this.refilldivunits1stbef = value 
		 return this 
 	 } 
	 def withrefillpromodivunits1stbef(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits1stbef = value 
		 return this 
 	 } 
	 def withunitbalance1stbef(value: Double) : AirRefillCS5 = {
		 this.unitbalance1stbef = value 
		 return this 
 	 } 
	 def withclearedunits1stbef(value: Double) : AirRefillCS5 = {
		 this.clearedunits1stbef = value 
		 return this 
 	 } 
	 def withrealmoneyflag1stbef(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag1stbef = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit2ndbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit2ndbef = value 
		 return this 
 	 } 
	 def withaccountstartdate2ndbef(value: String) : AirRefillCS5 = {
		 this.accountstartdate2ndbef = value 
		 return this 
 	 } 
	 def withrefilldivunits2ndbef(value: Double) : AirRefillCS5 = {
		 this.refilldivunits2ndbef = value 
		 return this 
 	 } 
	 def withrefillpromodivunits2ndbef(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits2ndbef = value 
		 return this 
 	 } 
	 def withunitbalance2ndbef(value: Double) : AirRefillCS5 = {
		 this.unitbalance2ndbef = value 
		 return this 
 	 } 
	 def withclearedunits2ndbef(value: Double) : AirRefillCS5 = {
		 this.clearedunits2ndbef = value 
		 return this 
 	 } 
	 def withrealmoneyflag2ndbef(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag2ndbef = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit3rdbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit3rdbef = value 
		 return this 
 	 } 
	 def withaccountstartdate3rdbef(value: String) : AirRefillCS5 = {
		 this.accountstartdate3rdbef = value 
		 return this 
 	 } 
	 def withrefilldivunits3rdbef(value: Double) : AirRefillCS5 = {
		 this.refilldivunits3rdbef = value 
		 return this 
 	 } 
	 def withrefillpromodivunits3rdbef(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits3rdbef = value 
		 return this 
 	 } 
	 def withunitbalance3rdbef(value: Double) : AirRefillCS5 = {
		 this.unitbalance3rdbef = value 
		 return this 
 	 } 
	 def withclearedunits3rdbef(value: Double) : AirRefillCS5 = {
		 this.clearedunits3rdbef = value 
		 return this 
 	 } 
	 def withrealmoneyflag3rdbef(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag3rdbef = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit4thbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit4thbef = value 
		 return this 
 	 } 
	 def withaccountstartdate4thbef(value: String) : AirRefillCS5 = {
		 this.accountstartdate4thbef = value 
		 return this 
 	 } 
	 def withrefilldivunits4thbef(value: Double) : AirRefillCS5 = {
		 this.refilldivunits4thbef = value 
		 return this 
 	 } 
	 def withrefillpromodivunits4thbef(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits4thbef = value 
		 return this 
 	 } 
	 def withunitbalance4thbef(value: Double) : AirRefillCS5 = {
		 this.unitbalance4thbef = value 
		 return this 
 	 } 
	 def withclearedunits4thbef(value: Double) : AirRefillCS5 = {
		 this.clearedunits4thbef = value 
		 return this 
 	 } 
	 def withrealmoneyflag4thbef(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag4thbef = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit5thbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit5thbef = value 
		 return this 
 	 } 
	 def withaccountstartdate5thbef(value: String) : AirRefillCS5 = {
		 this.accountstartdate5thbef = value 
		 return this 
 	 } 
	 def withrefilldivunits5thbef(value: Double) : AirRefillCS5 = {
		 this.refilldivunits5thbef = value 
		 return this 
 	 } 
	 def withrefillpromodivunits5thbef(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits5thbef = value 
		 return this 
 	 } 
	 def withunitbalance5thbef(value: Double) : AirRefillCS5 = {
		 this.unitbalance5thbef = value 
		 return this 
 	 } 
	 def withclearedunits5thbef(value: Double) : AirRefillCS5 = {
		 this.clearedunits5thbef = value 
		 return this 
 	 } 
	 def withrealmoneyflag5thbef(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag5thbef = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit6thbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit6thbef = value 
		 return this 
 	 } 
	 def withaccountstartdate6thbef(value: String) : AirRefillCS5 = {
		 this.accountstartdate6thbef = value 
		 return this 
 	 } 
	 def withrefilldivunits6thbef(value: Double) : AirRefillCS5 = {
		 this.refilldivunits6thbef = value 
		 return this 
 	 } 
	 def withrefillpromodivunits6thbef(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits6thbef = value 
		 return this 
 	 } 
	 def withunitbalance6thbef(value: Double) : AirRefillCS5 = {
		 this.unitbalance6thbef = value 
		 return this 
 	 } 
	 def withclearedunits6thbef(value: Double) : AirRefillCS5 = {
		 this.clearedunits6thbef = value 
		 return this 
 	 } 
	 def withrealmoneyflag6thbef(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag6thbef = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit7thbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit7thbef = value 
		 return this 
 	 } 
	 def withaccountstartdate7thbef(value: String) : AirRefillCS5 = {
		 this.accountstartdate7thbef = value 
		 return this 
 	 } 
	 def withrefilldivunits7thbef(value: Double) : AirRefillCS5 = {
		 this.refilldivunits7thbef = value 
		 return this 
 	 } 
	 def withrefillpromodivunits7thbef(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits7thbef = value 
		 return this 
 	 } 
	 def withunitbalance7thbef(value: Double) : AirRefillCS5 = {
		 this.unitbalance7thbef = value 
		 return this 
 	 } 
	 def withclearedunits7thbef(value: Double) : AirRefillCS5 = {
		 this.clearedunits7thbef = value 
		 return this 
 	 } 
	 def withrealmoneyflag7thbef(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag7thbef = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit8thbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit8thbef = value 
		 return this 
 	 } 
	 def withaccountstartdate8thbef(value: String) : AirRefillCS5 = {
		 this.accountstartdate8thbef = value 
		 return this 
 	 } 
	 def withrefilldivunits8thbef(value: Double) : AirRefillCS5 = {
		 this.refilldivunits8thbef = value 
		 return this 
 	 } 
	 def withrefillpromodivunits8thbef(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits8thbef = value 
		 return this 
 	 } 
	 def withunitbalance8thbef(value: Double) : AirRefillCS5 = {
		 this.unitbalance8thbef = value 
		 return this 
 	 } 
	 def withclearedunits8thbef(value: Double) : AirRefillCS5 = {
		 this.clearedunits8thbef = value 
		 return this 
 	 } 
	 def withrealmoneyflag8thbef(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag8thbef = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit9thbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit9thbef = value 
		 return this 
 	 } 
	 def withaccountstartdate9thbef(value: String) : AirRefillCS5 = {
		 this.accountstartdate9thbef = value 
		 return this 
 	 } 
	 def withrefilldivunits9thbef(value: Double) : AirRefillCS5 = {
		 this.refilldivunits9thbef = value 
		 return this 
 	 } 
	 def withrefillpromodivunits9thbef(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits9thbef = value 
		 return this 
 	 } 
	 def withunitbalance9thbef(value: Double) : AirRefillCS5 = {
		 this.unitbalance9thbef = value 
		 return this 
 	 } 
	 def withclearedunits9thbef(value: Double) : AirRefillCS5 = {
		 this.clearedunits9thbef = value 
		 return this 
 	 } 
	 def withrealmoneyflag9thbef(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag9thbef = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit10thbef(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit10thbef = value 
		 return this 
 	 } 
	 def withaccountstartdate10thbef(value: String) : AirRefillCS5 = {
		 this.accountstartdate10thbef = value 
		 return this 
 	 } 
	 def withrefilldivunits10thbef(value: Double) : AirRefillCS5 = {
		 this.refilldivunits10thbef = value 
		 return this 
 	 } 
	 def withrefillpromodivunits10thbef(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits10thbef = value 
		 return this 
 	 } 
	 def withunitbalance10thbef(value: Double) : AirRefillCS5 = {
		 this.unitbalance10thbef = value 
		 return this 
 	 } 
	 def withclearedunits10thbef(value: Double) : AirRefillCS5 = {
		 this.clearedunits10thbef = value 
		 return this 
 	 } 
	 def withrealmoneyflag10thbef(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag10thbef = value 
		 return this 
 	 } 
	 def withoffer1stidentifierbef(value: Double) : AirRefillCS5 = {
		 this.offer1stidentifierbef = value 
		 return this 
 	 } 
	 def withofferstartdate1stbef(value: String) : AirRefillCS5 = {
		 this.offerstartdate1stbef = value 
		 return this 
 	 } 
	 def withofferexpirydate1stbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydate1stbef = value 
		 return this 
 	 } 
	 def withoffertype1stbef(value: String) : AirRefillCS5 = {
		 this.offertype1stbef = value 
		 return this 
 	 } 
	 def withofferproductidentifier1stbef(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier1stbef = value 
		 return this 
 	 } 
	 def withofferstartdatetime1stbef(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime1stbef = value 
		 return this 
 	 } 
	 def withofferexpirydatetime1stbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime1stbef = value 
		 return this 
 	 } 
	 def withoffer2ndidentifierbef(value: Double) : AirRefillCS5 = {
		 this.offer2ndidentifierbef = value 
		 return this 
 	 } 
	 def withofferstartdate2ndbef(value: String) : AirRefillCS5 = {
		 this.offerstartdate2ndbef = value 
		 return this 
 	 } 
	 def withofferexpirydate2ndbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydate2ndbef = value 
		 return this 
 	 } 
	 def withoffertype2ndbef(value: String) : AirRefillCS5 = {
		 this.offertype2ndbef = value 
		 return this 
 	 } 
	 def withofferproductidentifier2ndbef(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier2ndbef = value 
		 return this 
 	 } 
	 def withofferstartdatetime2ndbef(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime2ndbef = value 
		 return this 
 	 } 
	 def withofferexpirydatetime2ndbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime2ndbef = value 
		 return this 
 	 } 
	 def withoffer3rdidentifierbef(value: Double) : AirRefillCS5 = {
		 this.offer3rdidentifierbef = value 
		 return this 
 	 } 
	 def withofferstartdate3rdbef(value: String) : AirRefillCS5 = {
		 this.offerstartdate3rdbef = value 
		 return this 
 	 } 
	 def withofferexpirydate3rdbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydate3rdbef = value 
		 return this 
 	 } 
	 def withoffertype3rdbef(value: String) : AirRefillCS5 = {
		 this.offertype3rdbef = value 
		 return this 
 	 } 
	 def withofferproductidentifier3rdbef(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier3rdbef = value 
		 return this 
 	 } 
	 def withofferstartdatetime3rdbef(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime3rdbef = value 
		 return this 
 	 } 
	 def withofferexpirydatetime3rdbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime3rdbef = value 
		 return this 
 	 } 
	 def withoffer4thidentifierbef(value: Double) : AirRefillCS5 = {
		 this.offer4thidentifierbef = value 
		 return this 
 	 } 
	 def withofferstartdate4thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdate4thbef = value 
		 return this 
 	 } 
	 def withofferexpirydate4thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydate4thbef = value 
		 return this 
 	 } 
	 def withoffertype4thbef(value: String) : AirRefillCS5 = {
		 this.offertype4thbef = value 
		 return this 
 	 } 
	 def withofferproductidentifier4thbef(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier4thbef = value 
		 return this 
 	 } 
	 def withofferstartdatetime4thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime4thbef = value 
		 return this 
 	 } 
	 def withofferexpirydatetime4thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime4thbef = value 
		 return this 
 	 } 
	 def withoffer5thidentifierbef(value: Double) : AirRefillCS5 = {
		 this.offer5thidentifierbef = value 
		 return this 
 	 } 
	 def withofferstartdate5thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdate5thbef = value 
		 return this 
 	 } 
	 def withofferexpirydate5thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydate5thbef = value 
		 return this 
 	 } 
	 def withoffertype5thbef(value: String) : AirRefillCS5 = {
		 this.offertype5thbef = value 
		 return this 
 	 } 
	 def withofferproductidentifier5thbef(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier5thbef = value 
		 return this 
 	 } 
	 def withofferstartdatetime5thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime5thbef = value 
		 return this 
 	 } 
	 def withofferexpirydatetime5thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime5thbef = value 
		 return this 
 	 } 
	 def withoffer6thidentifierbef(value: Double) : AirRefillCS5 = {
		 this.offer6thidentifierbef = value 
		 return this 
 	 } 
	 def withofferstartdate6thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdate6thbef = value 
		 return this 
 	 } 
	 def withofferexpirydate6thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydate6thbef = value 
		 return this 
 	 } 
	 def withoffertype6thbef(value: String) : AirRefillCS5 = {
		 this.offertype6thbef = value 
		 return this 
 	 } 
	 def withofferproductidentifier6thbef(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier6thbef = value 
		 return this 
 	 } 
	 def withofferstartdatetime6thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime6thbef = value 
		 return this 
 	 } 
	 def withofferexpirydatetime6thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime6thbef = value 
		 return this 
 	 } 
	 def withoffer7thidentifierbef(value: Double) : AirRefillCS5 = {
		 this.offer7thidentifierbef = value 
		 return this 
 	 } 
	 def withofferstartdate7thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdate7thbef = value 
		 return this 
 	 } 
	 def withofferexpirydate7thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydate7thbef = value 
		 return this 
 	 } 
	 def withoffertype7thbef(value: String) : AirRefillCS5 = {
		 this.offertype7thbef = value 
		 return this 
 	 } 
	 def withofferproductidentifier7thbef(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier7thbef = value 
		 return this 
 	 } 
	 def withofferstartdatetime7thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime7thbef = value 
		 return this 
 	 } 
	 def withofferexpirydatetime7thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime7thbef = value 
		 return this 
 	 } 
	 def withoffer8thidentifierbef(value: Double) : AirRefillCS5 = {
		 this.offer8thidentifierbef = value 
		 return this 
 	 } 
	 def withofferstartdate8thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdate8thbef = value 
		 return this 
 	 } 
	 def withofferexpirydate8thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydate8thbef = value 
		 return this 
 	 } 
	 def withoffertype8thbef(value: String) : AirRefillCS5 = {
		 this.offertype8thbef = value 
		 return this 
 	 } 
	 def withofferproductidentifier8thbef(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier8thbef = value 
		 return this 
 	 } 
	 def withofferstartdatetime8thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime8thbef = value 
		 return this 
 	 } 
	 def withofferexpirydatetime8thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime8thbef = value 
		 return this 
 	 } 
	 def withoffer9thidentifierbef(value: Double) : AirRefillCS5 = {
		 this.offer9thidentifierbef = value 
		 return this 
 	 } 
	 def withofferstartdate9thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdate9thbef = value 
		 return this 
 	 } 
	 def withofferexpirydate9thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydate9thbef = value 
		 return this 
 	 } 
	 def withoffertype9thbef(value: String) : AirRefillCS5 = {
		 this.offertype9thbef = value 
		 return this 
 	 } 
	 def withofferproductidentifier9thbef(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier9thbef = value 
		 return this 
 	 } 
	 def withofferstartdatetime9thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime9thbef = value 
		 return this 
 	 } 
	 def withofferexpirydatetime9thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime9thbef = value 
		 return this 
 	 } 
	 def withoffer10thidentifierbef(value: Double) : AirRefillCS5 = {
		 this.offer10thidentifierbef = value 
		 return this 
 	 } 
	 def withofferstartdate10thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdate10thbef = value 
		 return this 
 	 } 
	 def withofferexpirydate10thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydate10thbef = value 
		 return this 
 	 } 
	 def withoffertype10thbef(value: String) : AirRefillCS5 = {
		 this.offertype10thbef = value 
		 return this 
 	 } 
	 def withofferproductidentifier10thbef(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier10thbef = value 
		 return this 
 	 } 
	 def withofferstartdatetime10thbef(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime10thbef = value 
		 return this 
 	 } 
	 def withofferexpirydatetime10thbef(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime10thbef = value 
		 return this 
 	 } 
	 def withaggregatedbalancebef(value: Double) : AirRefillCS5 = {
		 this.aggregatedbalancebef = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit1staft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit1staft = value 
		 return this 
 	 } 
	 def withaccountstartdate1staft(value: String) : AirRefillCS5 = {
		 this.accountstartdate1staft = value 
		 return this 
 	 } 
	 def withrefilldivunits1staft(value: Double) : AirRefillCS5 = {
		 this.refilldivunits1staft = value 
		 return this 
 	 } 
	 def withrefillpromodivunits1staft(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits1staft = value 
		 return this 
 	 } 
	 def withunitbalance1staft(value: Double) : AirRefillCS5 = {
		 this.unitbalance1staft = value 
		 return this 
 	 } 
	 def withclearedunits1staft(value: Double) : AirRefillCS5 = {
		 this.clearedunits1staft = value 
		 return this 
 	 } 
	 def withrealmoneyflag1staft(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag1staft = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit2ndaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit2ndaft = value 
		 return this 
 	 } 
	 def withaccountstartdate2ndaft(value: String) : AirRefillCS5 = {
		 this.accountstartdate2ndaft = value 
		 return this 
 	 } 
	 def withrefilldivunits2ndaft(value: Double) : AirRefillCS5 = {
		 this.refilldivunits2ndaft = value 
		 return this 
 	 } 
	 def withrefillpromodivunits2ndaft(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits2ndaft = value 
		 return this 
 	 } 
	 def withunitbalance2ndaft(value: Double) : AirRefillCS5 = {
		 this.unitbalance2ndaft = value 
		 return this 
 	 } 
	 def withclearedunits2ndaft(value: Double) : AirRefillCS5 = {
		 this.clearedunits2ndaft = value 
		 return this 
 	 } 
	 def withrealmoneyflag2ndaft(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag2ndaft = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit3rdaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit3rdaft = value 
		 return this 
 	 } 
	 def withaccountstartdate3rdaft(value: String) : AirRefillCS5 = {
		 this.accountstartdate3rdaft = value 
		 return this 
 	 } 
	 def withrefilldivunits3rdaft(value: Double) : AirRefillCS5 = {
		 this.refilldivunits3rdaft = value 
		 return this 
 	 } 
	 def withrefillpromodivunits3rdaft(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits3rdaft = value 
		 return this 
 	 } 
	 def withunitbalance3rdaft(value: Double) : AirRefillCS5 = {
		 this.unitbalance3rdaft = value 
		 return this 
 	 } 
	 def withclearedunits3rdaft(value: Double) : AirRefillCS5 = {
		 this.clearedunits3rdaft = value 
		 return this 
 	 } 
	 def withrealmoneyflag3rdaft(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag3rdaft = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit4thaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit4thaft = value 
		 return this 
 	 } 
	 def withaccountstartdate4thaft(value: String) : AirRefillCS5 = {
		 this.accountstartdate4thaft = value 
		 return this 
 	 } 
	 def withrefilldivunits4thaft(value: Double) : AirRefillCS5 = {
		 this.refilldivunits4thaft = value 
		 return this 
 	 } 
	 def withrefillpromodivunits4thaft(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits4thaft = value 
		 return this 
 	 } 
	 def withunitbalance4thaft(value: Double) : AirRefillCS5 = {
		 this.unitbalance4thaft = value 
		 return this 
 	 } 
	 def withclearedunits4thaft(value: Double) : AirRefillCS5 = {
		 this.clearedunits4thaft = value 
		 return this 
 	 } 
	 def withrealmoneyflag4thaft(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag4thaft = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit5thaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit5thaft = value 
		 return this 
 	 } 
	 def withaccountstartdate5thaft(value: String) : AirRefillCS5 = {
		 this.accountstartdate5thaft = value 
		 return this 
 	 } 
	 def withrefilldivunits5thaft(value: Double) : AirRefillCS5 = {
		 this.refilldivunits5thaft = value 
		 return this 
 	 } 
	 def withrefillpromodivunits5thaft(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits5thaft = value 
		 return this 
 	 } 
	 def withunitbalance5thaft(value: Double) : AirRefillCS5 = {
		 this.unitbalance5thaft = value 
		 return this 
 	 } 
	 def withclearedunits5thaft(value: Double) : AirRefillCS5 = {
		 this.clearedunits5thaft = value 
		 return this 
 	 } 
	 def withrealmoneyflag5thaft(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag5thaft = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit6thaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit6thaft = value 
		 return this 
 	 } 
	 def withaccountstartdate6thaft(value: String) : AirRefillCS5 = {
		 this.accountstartdate6thaft = value 
		 return this 
 	 } 
	 def withrefilldivunits6thaft(value: Double) : AirRefillCS5 = {
		 this.refilldivunits6thaft = value 
		 return this 
 	 } 
	 def withrefillpromodivunits6thaft(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits6thaft = value 
		 return this 
 	 } 
	 def withunitbalance6thaft(value: Double) : AirRefillCS5 = {
		 this.unitbalance6thaft = value 
		 return this 
 	 } 
	 def withclearedunits6thaft(value: Double) : AirRefillCS5 = {
		 this.clearedunits6thaft = value 
		 return this 
 	 } 
	 def withrealmoneyflag6thaft(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag6thaft = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit7thaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit7thaft = value 
		 return this 
 	 } 
	 def withaccountstartdate7thaft(value: String) : AirRefillCS5 = {
		 this.accountstartdate7thaft = value 
		 return this 
 	 } 
	 def withrefilldivunits7thaft(value: Double) : AirRefillCS5 = {
		 this.refilldivunits7thaft = value 
		 return this 
 	 } 
	 def withrefillpromodivunits7thaft(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits7thaft = value 
		 return this 
 	 } 
	 def withunitbalance7thaft(value: Double) : AirRefillCS5 = {
		 this.unitbalance7thaft = value 
		 return this 
 	 } 
	 def withclearedunits7thaft(value: Double) : AirRefillCS5 = {
		 this.clearedunits7thaft = value 
		 return this 
 	 } 
	 def withrealmoneyflag7thaft(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag7thaft = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit8thaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit8thaft = value 
		 return this 
 	 } 
	 def withaccountstartdate8thaft(value: String) : AirRefillCS5 = {
		 this.accountstartdate8thaft = value 
		 return this 
 	 } 
	 def withrefilldivunits8thaft(value: Double) : AirRefillCS5 = {
		 this.refilldivunits8thaft = value 
		 return this 
 	 } 
	 def withrefillpromodivunits8thaft(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits8thaft = value 
		 return this 
 	 } 
	 def withunitbalance8thaft(value: Double) : AirRefillCS5 = {
		 this.unitbalance8thaft = value 
		 return this 
 	 } 
	 def withclearedunits8thaft(value: Double) : AirRefillCS5 = {
		 this.clearedunits8thaft = value 
		 return this 
 	 } 
	 def withrealmoneyflag8thaft(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag8thaft = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit9thaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit9thaft = value 
		 return this 
 	 } 
	 def withaccountstartdate9thaft(value: String) : AirRefillCS5 = {
		 this.accountstartdate9thaft = value 
		 return this 
 	 } 
	 def withrefilldivunits9thaft(value: Double) : AirRefillCS5 = {
		 this.refilldivunits9thaft = value 
		 return this 
 	 } 
	 def withrefillpromodivunits9thaft(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits9thaft = value 
		 return this 
 	 } 
	 def withunitbalance9thaft(value: Double) : AirRefillCS5 = {
		 this.unitbalance9thaft = value 
		 return this 
 	 } 
	 def withclearedunits9thaft(value: Double) : AirRefillCS5 = {
		 this.clearedunits9thaft = value 
		 return this 
 	 } 
	 def withrealmoneyflag9thaft(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag9thaft = value 
		 return this 
 	 } 
	 def withdedicatedaccountunit10thaft(value: Double) : AirRefillCS5 = {
		 this.dedicatedaccountunit10thaft = value 
		 return this 
 	 } 
	 def withaccountstartdate10thaft(value: String) : AirRefillCS5 = {
		 this.accountstartdate10thaft = value 
		 return this 
 	 } 
	 def withrefilldivunits10thaft(value: Double) : AirRefillCS5 = {
		 this.refilldivunits10thaft = value 
		 return this 
 	 } 
	 def withrefillpromodivunits10thaft(value: Double) : AirRefillCS5 = {
		 this.refillpromodivunits10thaft = value 
		 return this 
 	 } 
	 def withunitbalance10thaft(value: Double) : AirRefillCS5 = {
		 this.unitbalance10thaft = value 
		 return this 
 	 } 
	 def withclearedunits10thaft(value: Double) : AirRefillCS5 = {
		 this.clearedunits10thaft = value 
		 return this 
 	 } 
	 def withrealmoneyflag10thaft(value: Double) : AirRefillCS5 = {
		 this.realmoneyflag10thaft = value 
		 return this 
 	 } 
	 def withoffer1stidentifieraft(value: Double) : AirRefillCS5 = {
		 this.offer1stidentifieraft = value 
		 return this 
 	 } 
	 def withofferstartdate1staft(value: String) : AirRefillCS5 = {
		 this.offerstartdate1staft = value 
		 return this 
 	 } 
	 def withofferexpirydate1staft(value: String) : AirRefillCS5 = {
		 this.offerexpirydate1staft = value 
		 return this 
 	 } 
	 def withoffertype1staft(value: String) : AirRefillCS5 = {
		 this.offertype1staft = value 
		 return this 
 	 } 
	 def withofferproductidentifier1staft(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier1staft = value 
		 return this 
 	 } 
	 def withofferstartdatetime1staft(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime1staft = value 
		 return this 
 	 } 
	 def withofferexpirydatetime1staft(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime1staft = value 
		 return this 
 	 } 
	 def withoffer2ndidentifieraft(value: Double) : AirRefillCS5 = {
		 this.offer2ndidentifieraft = value 
		 return this 
 	 } 
	 def withofferstartdate2ndaft(value: String) : AirRefillCS5 = {
		 this.offerstartdate2ndaft = value 
		 return this 
 	 } 
	 def withofferexpirydate2ndaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydate2ndaft = value 
		 return this 
 	 } 
	 def withoffertype2ndaft(value: String) : AirRefillCS5 = {
		 this.offertype2ndaft = value 
		 return this 
 	 } 
	 def withofferproductidentifier2ndaft(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier2ndaft = value 
		 return this 
 	 } 
	 def withofferstartdatetime2ndaft(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime2ndaft = value 
		 return this 
 	 } 
	 def withofferexpirydatetime2ndaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime2ndaft = value 
		 return this 
 	 } 
	 def withoffer3rdidentifieraft(value: Double) : AirRefillCS5 = {
		 this.offer3rdidentifieraft = value 
		 return this 
 	 } 
	 def withofferstartdate3rdaft(value: String) : AirRefillCS5 = {
		 this.offerstartdate3rdaft = value 
		 return this 
 	 } 
	 def withofferexpirydate3rdaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydate3rdaft = value 
		 return this 
 	 } 
	 def withoffertype3rdaft(value: String) : AirRefillCS5 = {
		 this.offertype3rdaft = value 
		 return this 
 	 } 
	 def withofferproductidentifier3rdaft(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier3rdaft = value 
		 return this 
 	 } 
	 def withofferstartdatetime3rdaft(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime3rdaft = value 
		 return this 
 	 } 
	 def withofferexpirydatetime3rdaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime3rdaft = value 
		 return this 
 	 } 
	 def withoffer4thidentifieraft(value: Double) : AirRefillCS5 = {
		 this.offer4thidentifieraft = value 
		 return this 
 	 } 
	 def withofferstartdate4thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdate4thaft = value 
		 return this 
 	 } 
	 def withofferexpirydate4thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydate4thaft = value 
		 return this 
 	 } 
	 def withoffertype4thaft(value: String) : AirRefillCS5 = {
		 this.offertype4thaft = value 
		 return this 
 	 } 
	 def withofferproductidentifier4thaft(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier4thaft = value 
		 return this 
 	 } 
	 def withofferstartdatetime4thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime4thaft = value 
		 return this 
 	 } 
	 def withofferexpirydatetime4thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime4thaft = value 
		 return this 
 	 } 
	 def withoffer5thidentifieraft(value: Double) : AirRefillCS5 = {
		 this.offer5thidentifieraft = value 
		 return this 
 	 } 
	 def withofferstartdate5thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdate5thaft = value 
		 return this 
 	 } 
	 def withofferexpirydate5thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydate5thaft = value 
		 return this 
 	 } 
	 def withoffertype5thaft(value: String) : AirRefillCS5 = {
		 this.offertype5thaft = value 
		 return this 
 	 } 
	 def withofferproductidentifier5thaft(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier5thaft = value 
		 return this 
 	 } 
	 def withofferstartdatetime5thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime5thaft = value 
		 return this 
 	 } 
	 def withofferexpirydatetime5thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime5thaft = value 
		 return this 
 	 } 
	 def withoffer6thidentifieraft(value: Double) : AirRefillCS5 = {
		 this.offer6thidentifieraft = value 
		 return this 
 	 } 
	 def withofferstartdate6thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdate6thaft = value 
		 return this 
 	 } 
	 def withofferexpirydate6thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydate6thaft = value 
		 return this 
 	 } 
	 def withoffertype6thaft(value: String) : AirRefillCS5 = {
		 this.offertype6thaft = value 
		 return this 
 	 } 
	 def withofferproductidentifier6thaft(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier6thaft = value 
		 return this 
 	 } 
	 def withofferstartdatetime6thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime6thaft = value 
		 return this 
 	 } 
	 def withofferexpirydatetime6thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime6thaft = value 
		 return this 
 	 } 
	 def withoffer7thidentifieraft(value: Double) : AirRefillCS5 = {
		 this.offer7thidentifieraft = value 
		 return this 
 	 } 
	 def withofferstartdate7thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdate7thaft = value 
		 return this 
 	 } 
	 def withofferexpirydate7thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydate7thaft = value 
		 return this 
 	 } 
	 def withoffertype7thaft(value: String) : AirRefillCS5 = {
		 this.offertype7thaft = value 
		 return this 
 	 } 
	 def withofferproductidentifier7thaft(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier7thaft = value 
		 return this 
 	 } 
	 def withofferstartdatetime7thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime7thaft = value 
		 return this 
 	 } 
	 def withofferexpirydatetime7thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime7thaft = value 
		 return this 
 	 } 
	 def withoffer8thidentifieraft(value: Double) : AirRefillCS5 = {
		 this.offer8thidentifieraft = value 
		 return this 
 	 } 
	 def withofferstartdate8thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdate8thaft = value 
		 return this 
 	 } 
	 def withofferexpirydate8thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydate8thaft = value 
		 return this 
 	 } 
	 def withoffertype8thaft(value: String) : AirRefillCS5 = {
		 this.offertype8thaft = value 
		 return this 
 	 } 
	 def withofferproductidentifier8thaft(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier8thaft = value 
		 return this 
 	 } 
	 def withofferstartdatetime8thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime8thaft = value 
		 return this 
 	 } 
	 def withofferexpirydatetime8thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime8thaft = value 
		 return this 
 	 } 
	 def withoffer9thidentifieraft(value: Double) : AirRefillCS5 = {
		 this.offer9thidentifieraft = value 
		 return this 
 	 } 
	 def withofferstartdate9thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdate9thaft = value 
		 return this 
 	 } 
	 def withofferexpirydate9thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydate9thaft = value 
		 return this 
 	 } 
	 def withoffertype9thaft(value: String) : AirRefillCS5 = {
		 this.offertype9thaft = value 
		 return this 
 	 } 
	 def withofferproductidentifier9thaft(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier9thaft = value 
		 return this 
 	 } 
	 def withofferstartdatetime9thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime9thaft = value 
		 return this 
 	 } 
	 def withofferexpirydatetime9thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime9thaft = value 
		 return this 
 	 } 
	 def withoffer10thidentifieraft(value: Double) : AirRefillCS5 = {
		 this.offer10thidentifieraft = value 
		 return this 
 	 } 
	 def withofferstartdate10thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdate10thaft = value 
		 return this 
 	 } 
	 def withofferexpirydate10thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydate10thaft = value 
		 return this 
 	 } 
	 def withoffertype10thaft(value: String) : AirRefillCS5 = {
		 this.offertype10thaft = value 
		 return this 
 	 } 
	 def withofferproductidentifier10thaft(value: Double) : AirRefillCS5 = {
		 this.offerproductidentifier10thaft = value 
		 return this 
 	 } 
	 def withofferstartdatetime10thaft(value: String) : AirRefillCS5 = {
		 this.offerstartdatetime10thaft = value 
		 return this 
 	 } 
	 def withofferexpirydatetime10thaft(value: String) : AirRefillCS5 = {
		 this.offerexpirydatetime10thaft = value 
		 return this 
 	 } 
	 def withaggregatedbalanceaft(value: Double) : AirRefillCS5 = {
		 this.aggregatedbalanceaft = value 
		 return this 
 	 } 
	 def withcellidentifier(value: String) : AirRefillCS5 = {
		 this.cellidentifier = value 
		 return this 
 	 } 
	 def withmarket_id(value: Int) : AirRefillCS5 = {
		 this.market_id = value 
		 return this 
 	 } 
	 def withhub_id(value: Int) : AirRefillCS5 = {
		 this.hub_id = value 
		 return this 
 	 } 
	 def withfilename(value: String) : AirRefillCS5 = {
		 this.filename = value 
		 return this 
 	 } 
    def isCaseSensitive(): Boolean = AirRefillCS5.isCaseSensitive(); 
    def caseSensitiveKey(keyName: String): String = {
      if(isCaseSensitive)
        return keyName;
      else return keyName.toLowerCase;
    }


    
    def this(factory:MessageFactoryInterface) = {
      this(factory, null)
     }
    
    def this(other: AirRefillCS5) = {
      this(other.getFactory.asInstanceOf[MessageFactoryInterface], other)
    }

}