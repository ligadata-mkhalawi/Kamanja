package com.ligadata.outputadapters;

import org.scalatest._
import Matchers._
import org.apache.logging.log4j._

import com.ligadata.KamanjaBase._ // { AttributeTypeInfo, ContainerFactoryInterface, ContainerInterface, ContainerOrConcept }
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.Exceptions.{ KamanjaException, FatalAdapterException, KeyNotFoundException }

import scala.collection.JavaConversions._
import java.io.File
import org.apache.commons.io.FileUtils

import com.ligadata.kamanja.metadata._
import com.ligadata.kamanja.metadata.MdMgr._

import java.util.{Date, Calendar, TimeZone}
import java.text.{SimpleDateFormat}
import scala.collection.mutable.ArrayBuffer;

import com.ligadata.kamanja.metadataload.MetadataLoad

object ParameterContainer extends RDDObject[ParameterContainer] with ContainerFactoryInterface {
  type T = ParameterContainer;
  override def getFullTypeName: String = "com.ligadata.oracleoutputadapter.test.ParameterContainer";
  override def getTypeNameSpace: String = "com.ligadata.oracleoutputadapter.test";
  override def getTypeName: String = "ParameterContainer";
  override def getTypeVersion: String = "000001.000000.000000";
  override def getSchemaId: Int = 5;
  override def getTenantId: String = "system";
  override def createInstance: ParameterContainer = new ParameterContainer(ParameterContainer);
  override def isFixed: Boolean = true;
  override def getContainerType: ContainerTypes.ContainerType = ContainerTypes.ContainerType.CONTAINER
  override def getFullName = getFullTypeName;
  override def getRddTenantId = getTenantId;
  override def toJavaRDDObject: JavaRDDObject[T] = JavaRDDObject.fromRDDObject[T](this);  
  
  override def FullName: String = getFullTypeName
  override def NameSpace: String = getTypeNameSpace
  override def Name: String = getTypeName
  override def Version: String = getTypeVersion
  override def CreateNewMessage: BaseMsg = null;
  override def CreateNewContainer: BaseContainer = createInstance.asInstanceOf[BaseContainer];
  override def IsFixed: Boolean = true
  override def IsKv: Boolean = false
  override def CanPersist: Boolean = false
  override def isMessage: Boolean = false
  override def isContainer: Boolean = true
  override def PartitionKeyData(inputdata: InputData): Array[String] = { throw new Exception("Deprecated method PartitionKeyData in obj ParameterContainer") };
  override def PrimaryKeyData(inputdata: InputData): Array[String] = throw new Exception("Deprecated method PrimaryKeyData in obj ParameterContainer");
  override def TimePartitionData(inputdata: InputData): Long = throw new Exception("Deprecated method TimePartitionData in obj ParameterContainer");
  override def NeedToTransformData: Boolean = false
  
  def build = new T(this)
  def build(from: T) = null
  
  override def getPartitionKeyNames: Array[String] = Array[String]("id");

  override def getPrimaryKeyNames: Array[String] = Array[String]("id");

  override def getTimePartitionInfo: TimePartitionInfo = { return null; } // FieldName, Format & Time Partition Types(Daily/Monthly/Yearly)

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
  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.oracleoutputadapter.test" , "name" : "ParameterContainer" , "fields":[{ "name" : "id" , "type" : "int"},{ "name" : "name" , "type" : "string"},{ "name" : "value" , "type" : "string"}, { "name" : "altername" , "type" : "string"}, { "name" : "alterid" , "type" : "double"}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.outputadapters.ParameterContainer => { return oldVerobj; }
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
}

class ParameterContainer(factory: ContainerFactoryInterface) extends ContainerInterface(factory) {

  var attributeTypes = generateAttributeTypes;

  private def generateAttributeTypes(): Array[AttributeTypeInfo] = {
    var attributeTypes = new Array[AttributeTypeInfo](5);
    attributeTypes(0) = new AttributeTypeInfo("id", 0, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
    attributeTypes(1) = new AttributeTypeInfo("name", 1, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(2) = new AttributeTypeInfo("value", 2, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(3) = new AttributeTypeInfo("altername", 3, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(4) = new AttributeTypeInfo("alterid", 4, AttributeTypeInfo.TypeCategory.DOUBLE, -1, -1, 0)

    return attributeTypes
  }
  
  var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;
    
  def Clone(): ContainerOrConcept = { ParameterContainer.build(this) }
  override def save: Unit = { }
  override def getPartitionKey: Array[String] = Array[String](id.toString)
  override def getPrimaryKey: Array[String] = Array[String](id.toString)

  override def getAttributeTypes(): Array[AttributeTypeInfo] = {
    if (attributeTypes == null) return null;
    return attributeTypes
  }
  
  override def getAttributeType(name: String): AttributeTypeInfo = {
    if (name == null || name.trim() == "") return null;
    attributeTypes.foreach(attributeType => {
      if (attributeType.getName == name.toLowerCase())
        return attributeType
    })
    return null;
  }
  
  var id: Int = _;
  var name: String = _;
  var value: String = _;
  var altername: String = _;
  var alterid: Double = _;
  
  override def get(index: Int): AnyRef = { 
    try {
      index match {
        case 0 => return this.id.asInstanceOf[AnyRef];
        case 1 => return this.name.asInstanceOf[AnyRef];
        case 2 => return this.value.asInstanceOf[AnyRef];
        case 3 => return this.altername.asInstanceOf[AnyRef];
        case 4 => return this.alterid.asInstanceOf[AnyRef];

        case _ => throw new Exception(s"$index is a bad index for message ParameterContainer");
      }
    } catch {
      case e: Exception => {
        throw e
      }
    };

  }
  
  override def get(keyName: String): AnyRef = {
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = keyName.toLowerCase;

    if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message/container ParameterContainer", null);
    return get(keyTypes(key).getIndex)     
  }
  
  override def getOrElse(keyName: String, defaultVal: Any): AnyRef = {
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = keyName.toLowerCase;
    try {
      val value = get(key.toLowerCase())
      if (value == null) return defaultVal.asInstanceOf[AnyRef]; else return value;
    } catch {
      case e: Exception => {
        throw e
      }
    }
    return null;
  }
  
  override def getOrElse(index: Int, defaultVal: Any): AnyRef = { // Return (value,  type)
    try {
      val value = get(index)
      if (value == null) return defaultVal.asInstanceOf[AnyRef]; else return value;
    } catch {
      case e: Exception => {
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
        throw e
      }
    }
    return null;
  }

  override def getAllAttributeValues(): Array[AttributeValue] = { 
    var attributeVals = new Array[AttributeValue](5);
    try {
      attributeVals(0) = new AttributeValue(this.id, keyTypes("id"))
      attributeVals(1) = new AttributeValue(this.name, keyTypes("name"))
      attributeVals(2) = new AttributeValue(this.value, keyTypes("value"))
      attributeVals(3) = new AttributeValue(this.altername, keyTypes("altername"))
      attributeVals(4) = new AttributeValue(this.alterid, keyTypes("alterid"))
    } catch {
      case e: Exception => {
        throw e
      }
    };

    return attributeVals;
  }
    override def set(keyName: String, value: Any) = {
    if (keyName == null || keyName.trim.size == 0) throw new Exception("Please provide proper key name " + keyName);
    val key = keyName.toLowerCase;
    try {

      if (!keyTypes.contains(key)) throw new KeyNotFoundException(s"Key $key does not exists in message ParameterContainer", null)
      set(keyTypes(key).getIndex, value);

    } catch {
      case e: Exception => {
        throw e
      }
    };

  }

  def set(index: Int, value: Any): Unit = {
    if (value == null) throw new Exception(s"Value is null for index $index in message ParameterContainer ")
    try {
      index match {
        case 0 => {
          if (value.isInstanceOf[Int])
            this.id = value.asInstanceOf[Int];
          else throw new Exception(s"Value is the not the correct type for field modelid in message ParameterContainer")
        }
        case 1 => {
          if (value.isInstanceOf[String])
            this.name = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field elapsedtimeinms in message ParameterContainer")
        }
        case 2 => {
          if (value.isInstanceOf[String])
            this.value = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field eventepochtime in message ParameterContainer")
        }
        case 3 => {
          if (value.isInstanceOf[String])
            this.altername = value.asInstanceOf[String];
          else throw new Exception(s"Value is the not the correct type for field eventepochtime in message ParameterContainer")
        }
        case 4 => {
          if (value.isInstanceOf[Double])
            this.alterid = value.asInstanceOf[Double];
          else throw new Exception(s"Value is the not the correct type for field eventepochtime in message ParameterContainer")
        }

        case _ => throw new Exception(s"$index is a bad index for message ParameterContainer");
      }
    } catch {
      case e: Exception => {
        throw e
      }
    };

  }

  override def set(key: String, value: Any, valTyp: String) = {
    throw new Exception("Set Func for Value and ValueType By Key is not supported for Fixed Messages")
  }
}

class TestOracleOutputAdapter extends FunSpec with BeforeAndAfter with BeforeAndAfterAll with GivenWhenThen {

  private var adapterConfig: AdapterConfiguration = null
  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)
  private var oa:OutputAdapter = null;
  
  override def beforeAll = {
    logger.warn("starting...");
    logger.warn("resource dir => " + getClass.getResource("/").getPath)

    val mdLoader = new MetadataLoad(MdMgr.mdMgr, "", "", "", "")
    mdLoader.initialize    
    
    // setup AdapterConfiguration object
    var adapterConfig = new AdapterConfiguration;
    adapterConfig.Name = "OracleOutputAdapter";
    adapterConfig.adapterSpecificCfg = s"""{"jdbcUrl":"jdbc:oracle:thin:@vm002.ligadata.com:1521:KAMANJA","user":"digicell","password":"Carribean2","jarpaths":"/media/home2/jdbc","jdbcJar":"ojdbc6.jar","autoCreateTables":"YES","appendOnly":"YES"}"""
    oa = OracleOutputAdapter.CreateOutputAdapter(adapterConfig, null);
  }

  private def getCurrentTimeAsString: String = {
    val df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    // Get the date today using Calendar object.
    val today = Calendar.getInstance().getTime();        
    // Using DateFormat format method we can create a string 
    // representation of a date with the defined format.
    val reportDate = df.format(today);
    return reportDate;
  }

  describe("Test oracleoutputadapter operations") {
    it("create an array of ContainerInterface and send them to oracle"){
      val instances = ArrayBuffer[ContainerInterface]()
      for (i <- 1 to 10){
	var inst = new ParameterContainer(ParameterContainer)
	inst.set("id",i)
	inst.set("name","parameter" + i)
	inst.set("value","value" + i)
	inst.set("altername","altername" + i)
	inst.set("alterid",i.asInstanceOf[Double])
	instances += inst
      }
      oa.send(null, instances.toArray)
    }
  }
  override def afterAll = {
    logger.warn("shutdown...");
    oa.Shutdown;
  }
}
