package com.ligadata.smartfileadapter

import org.scalatest._
import com.ligadata.KamanjaBase._ // { AttributeTypeInfo, ContainerFactoryInterface, ContainerInterface, ContainerOrConcept }
import com.ligadata.AdaptersConfiguration.SmartFileProducerConfiguration
import com.ligadata.OutputAdapters.SmartFileProducer
import com.ligadata.InputOutputAdapterInfo.AdapterConfiguration
import com.ligadata.Exceptions.{ KamanjaException, FatalAdapterException, KeyNotFoundException }
import scala.meta.tokens.Token.`override`
import scala.collection.JavaConversions._
import java.io.File
import org.apache.commons.io.FileUtils

object ParameterContainer extends RDDObject[ParameterContainer] with ContainerFactoryInterface {
  type T = ParameterContainer;
  override def getFullTypeName: String = "com.ligadata.smartfileadapter.test.ParameterContainer";
  override def getTypeNameSpace: String = "com.ligadata.smartfileadapter.test";
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

  override def getPrimaryKeyNames: Array[String] = Array[String]();

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
  override def getAvroSchema: String = """{ "type": "record",  "namespace" : "com.ligadata.smartfileadapter.test" , "name" : "ParameterContainer" , "fields":[{ "name" : "id" , "type" : "int"},{ "name" : "name" , "type" : "string"},{ "name" : "value" , "type" : "string"}]}""";

  final override def convertFrom(srcObj: Any): T = convertFrom(createInstance(), srcObj);

  override def convertFrom(newVerObj: Any, oldVerobj: Any): ContainerInterface = {
    try {
      if (oldVerobj == null) return null;
      oldVerobj match {

        case oldVerobj: com.ligadata.smartfileadapter.ParameterContainer => { return oldVerobj; }
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
    var attributeTypes = new Array[AttributeTypeInfo](3);
    attributeTypes(0) = new AttributeTypeInfo("id", 0, AttributeTypeInfo.TypeCategory.INT, -1, -1, 0)
    attributeTypes(1) = new AttributeTypeInfo("name", 1, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)
    attributeTypes(2) = new AttributeTypeInfo("value", 2, AttributeTypeInfo.TypeCategory.STRING, -1, -1, 0)

    return attributeTypes
  }
  
  var keyTypes: Map[String, AttributeTypeInfo] = attributeTypes.map { a => (a.getName, a) }.toMap;
    
  def Clone(): ContainerOrConcept = { ParameterContainer.build(this) }
  override def save: Unit = { }
  override def getPartitionKey: Array[String] = Array[String](id.toString)
  override def getPrimaryKey: Array[String] = Array[String]()
  
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
  
  override def get(index: Int): AnyRef = { 
    try {
      index match {
        case 0 => return this.id.asInstanceOf[AnyRef];
        case 1 => return this.name.asInstanceOf[AnyRef];
        case 2 => return this.value.asInstanceOf[AnyRef];

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
    var attributeVals = new Array[AttributeValue](3);
    try {
      attributeVals(0) = new AttributeValue(this.id, keyTypes("id"))
      attributeVals(1) = new AttributeValue(this.name, keyTypes("name"))
      attributeVals(2) = new AttributeValue(this.value, keyTypes("value"))
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

trait JsonDefaultSerializerDeserializer extends AdaptersSerializeDeserializers{
  override def getDefaultSerializerDeserializer(): MsgBindingInfo = { 
    var ser = new com.ligadata.kamanja.serializer.JSONSerDes();
    ser.configure(null, Map("emitSystemColumns" -> "true"))
    new MsgBindingInfo("com.ligadata.kamanja.serializer.jsonserdes", scala.collection.immutable.Map[String, Any](), "", ser)
  }
}

class TestLocalFileProducer extends FunSpec with BeforeAndAfter with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen {

  val location = getClass.getResource("/producer").getPath
  println(location)
  val dir = new File(location + "/com.ligadata.smartfileadapter.test.ParameterContainer")

  before {
    println("Before Test Deleting directory " + dir.getAbsolutePath)
    FileUtils.deleteDirectory(dir)
  }
  
  after {
    println("After Test Deleting directory " + dir.getAbsolutePath)
    FileUtils.deleteDirectory(dir)
  }

  describe("Test Smart File Producer") {

    val inputConfig = new AdapterConfiguration()
    inputConfig.Name = "TestOutput"
    inputConfig.className = "com.ligadata.InputAdapters.SamrtFileProducer$"
    inputConfig.jarName = "smartfileinputoutputadapters_2.10-1.0.jar"

    val data = new Array[ContainerInterface](3)
    data(0) = new ParameterContainer(ParameterContainer)
    data(0).set(0, 10)
    data(0).set(1, "parameter1")
    data(0).set(2, "value1")
    data(0).setTimePartitionData(1462912843000L)  // May 10, 2016
    data(1) = new ParameterContainer(ParameterContainer)
    data(1).set(0, 20)
    data(1).set(1, "parameter2")
    data(1).set(2, "value2")
    data(1).setTimePartitionData(1462912843000L)  // May 10, 2016
    data(2) = new ParameterContainer(ParameterContainer)
    data(2).set(0, 30)
    data(2).set(1, "parameter3")
    data(2).set(2, "value3")
    data(2).setTimePartitionData(1462999243000L)  // May 11, 2016

    val data2 = new Array[ContainerInterface](6)
    data.copyToArray(data2)
    data2(3) = new ParameterContainer(ParameterContainer)
    data2(3).set(0, 40)
    data2(3).set(1, "parameter1")
    data2(3).set(2, "value11")
    data2(3).setTimePartitionData(1462999243000L)  // May 11, 2016
    data2(4) = new ParameterContainer(ParameterContainer)
    data2(4).set(0, 50)
    data2(4).set(1, "parameter2")
    data2(4).set(2, "value21")
    data2(4).setTimePartitionData(1462999243000L)  // May 11, 2016
    data2(5) = new ParameterContainer(ParameterContainer)
    data2(5).set(0, 60)
    data2(5).set(1, "parameter3")
    data2(5).set(2, "value31")
    data2(5).setTimePartitionData(1462912843000L)  // May 10, 2016

    it("should produce a text file with json output") {
      
      inputConfig.adapterSpecificCfg =
        "{\"Uri\": \"file://" + location + "\",\"FileNamePrefix\": \"Data-\", \"MessageSeparator\": \"\\n\"}"

      val sfp = new SmartFileProducer(inputConfig, null) with JsonDefaultSerializerDeserializer
      sfp.send(null, data)
      sfp.Shutdown()

      val outfiles = FileUtils.listFiles(dir, Array("dat"), true)
      println("Number of file produced: " + outfiles.size())
      assert( outfiles.size() > 0)
      
      val file = outfiles.iterator().next()
      println("File " + file.getAbsolutePath)
      val actual = FileUtils.readFileToString(file)
      val expected = FileUtils.readFileToString(new File(location + "/unpartionedjson.expected"))
      assert(actual == expected)
    }

    it("should produce a .gz file when Compression is gz") {
      
      inputConfig.adapterSpecificCfg =
        "{\"Uri\": \"file://" + location + "\",\"FileNamePrefix\": \"Data-\", \"Compression\": \"gz\"}"

      val sfp = new SmartFileProducer(inputConfig, null) with JsonDefaultSerializerDeserializer
      sfp.send(null, data)
      sfp.Shutdown()

      val outfiles = FileUtils.listFiles(dir, Array("dat.gz"), true)
      println("Number of file produced: " + outfiles.size())
      assert( outfiles.size() > 0)
      
      val file = outfiles.iterator().next()
      println("File " + file.getAbsolutePath)
      val actual = FileUtils.readFileToString(file)
      val expected = FileUtils.readFileToString(new File(location + "/unpartionedjson.expected.gz"))
      assert(actual == expected)
    }
    
    it("should produce a .bz2 file when Compression is bzip2") {
      
      inputConfig.adapterSpecificCfg =
        "{\"Uri\": \"file://" + location + "\",\"FileNamePrefix\": \"Data-\", \"Compression\": \"bzip2\"}"

      val sfp = new SmartFileProducer(inputConfig, null) with JsonDefaultSerializerDeserializer
      sfp.send(null, data)
      sfp.Shutdown()

      val outfiles = FileUtils.listFiles(dir, Array("dat.bz2"), true)
      println("Number of file produced: " + outfiles.size())
      assert( outfiles.size() > 0)
      
      val file = outfiles.iterator().next()
      println("File " + file.getAbsolutePath)
      val actual = FileUtils.readFileToString(file)
      val expected = FileUtils.readFileToString(new File(location + "/unpartionedjson.expected.bz2"))
      assert(actual == expected)
    }

    it("should produce a .xz file when Compression is xz") {
      
      inputConfig.adapterSpecificCfg =
        "{\"Uri\": \"file://" + location + "\",\"FileNamePrefix\": \"Data-\", \"Compression\": \"xz\"}"

      val sfp = new SmartFileProducer(inputConfig, null) with JsonDefaultSerializerDeserializer
      sfp.send(null, data)
      sfp.Shutdown()

      val outfiles = FileUtils.listFiles(dir, Array("dat.xz"), true)
      println("Number of file produced: " + outfiles.size())
      assert( outfiles.size() > 0)
      
      val file = outfiles.iterator().next()
      println("File " + file.getAbsolutePath)
      val actual = FileUtils.readFileToString(file)
      val expected = FileUtils.readFileToString(new File(location + "/unpartionedjson.expected.xz"))
      assert(actual == expected)
    }
    
    it("should partition using time patition when Partition is given") {
      
      inputConfig.adapterSpecificCfg =
        "{\"Uri\": \"file://" + location + "\",\"FileNamePrefix\": \"Data-\", \"TimePartitionFormat\": \"year=${yyyy}/month=${MM}/day=${dd}\"}"

      val sfp = new SmartFileProducer(inputConfig, null) with JsonDefaultSerializerDeserializer
      sfp.send(null, data)
      sfp.Shutdown()

      val outfiles = FileUtils.listFiles(dir, Array("dat"), true)
      println("Number of file produced: " + outfiles.size())
      assert(outfiles.size() > 1)
      
      outfiles.foreach(file => {
        println("File " + file.getAbsolutePath)
        val expected = if (file.getAbsolutePath.contains("year=2016/month=05/day=10"))
          FileUtils.readFileToString(new File(location + "/may10partitionjson.expected"))
        else if (file.getAbsolutePath.contains("year=2016/month=05/day=11"))
          FileUtils.readFileToString(new File(location + "/may11partitionjson.expected"))
        else ""
        val actual = FileUtils.readFileToString(file)
        assert(actual == expected)
      })
    }

    it("should partition using any given field when PartitionFormat is given") {
      
      inputConfig.adapterSpecificCfg =
        "{\"Uri\": \"file://" + location + "\",\"FileNamePrefix\": \"Data-\", \"PartitionFormat\": \"name=${name}\"}"

      val sfp = new SmartFileProducer(inputConfig, null) with JsonDefaultSerializerDeserializer
      sfp.send(null, data)
      sfp.Shutdown()

      val outfiles = FileUtils.listFiles(dir, Array("dat"), true)
      println("Number of file produced: " + outfiles.size())
      assert(outfiles.size() > 1)
      
      outfiles.foreach(file => {
        println("File " + file.getAbsolutePath)
        val expected = if (file.getAbsolutePath.contains("name=parameter1"))
          FileUtils.readFileToString(new File(location + "/parameter1partitionjson.expected"))
        else if (file.getAbsolutePath.contains("name=parameter2"))
          FileUtils.readFileToString(new File(location + "/parameter2partitionjson.expected"))
        else if (file.getAbsolutePath.contains("name=parameter3"))
          FileUtils.readFileToString(new File(location + "/parameter3partitionjson.expected"))
        else ""
        val actual = FileUtils.readFileToString(file)
        assert(actual == expected)
      })
    }

    it("should partition using both message fields and time partion data") {
      
      inputConfig.adapterSpecificCfg =
        "{\"Uri\": \"file://" + location + "\",\"FileNamePrefix\": \"Data-\", \"PartitionFormat\": \"name=${name}/year=${time:yyyy}/month=${time:MM}/day=${time:dd}\"}"

      val sfp = new SmartFileProducer(inputConfig, null) with JsonDefaultSerializerDeserializer
      sfp.send(null, data2)
      sfp.Shutdown()

      val outfiles = FileUtils.listFiles(dir, Array("dat"), true)
      println("Number of file produced: " + outfiles.size())
      assert(outfiles.size() > 1)
      
      outfiles.foreach(file => {
        println("File " + file.getAbsolutePath)
        val expected = if (file.getAbsolutePath.contains("name=parameter1/year=2016/month=05/day=10"))
          FileUtils.readFileToString(new File(location + "/parameter1may10json.expected"))
        else if (file.getAbsolutePath.contains("name=parameter1/year=2016/month=05/day=11"))
          FileUtils.readFileToString(new File(location + "/parameter1may11json.expected"))
        else if (file.getAbsolutePath.contains("name=parameter2/year=2016/month=05/day=10"))
          FileUtils.readFileToString(new File(location + "/parameter2may10json.expected"))
        else if (file.getAbsolutePath.contains("name=parameter2/year=2016/month=05/day=11"))
          FileUtils.readFileToString(new File(location + "/parameter2may11json.expected"))
        else if (file.getAbsolutePath.contains("name=parameter3/year=2016/month=05/day=10"))
          FileUtils.readFileToString(new File(location + "/parameter3may10json.expected"))
        else if (file.getAbsolutePath.contains("name=parameter3/year=2016/month=05/day=11"))
          FileUtils.readFileToString(new File(location + "/parameter3may11json.expected"))
        else ""
        val actual = FileUtils.readFileToString(file)
        assert(actual == expected)
      })
    }

    it("should partition using message level partition overrides") {
      
      inputConfig.adapterSpecificCfg =
        "{\"Uri\": \"file://" + location + "\",\"FileNamePrefix\": \"Data-\", " + 
        "\"PartitionFormat\": \"name=${name}\", \"typeLevelConfig\": "  + 
        "[{\"type\": \"com.ligadata.smartfileadapter.test.ParameterContainer\", " + 
        "\"PartitionFormat\": \"name=${name}/year=${time:yyyy}/month=${time:MM}/day=${time:dd}\"}]}"

      val sfp = new SmartFileProducer(inputConfig, null) with JsonDefaultSerializerDeserializer
      sfp.send(null, data2)
      sfp.Shutdown()

      val outfiles = FileUtils.listFiles(dir, Array("dat"), true)
      println("Number of file produced: " + outfiles.size())
      assert(outfiles.size() > 1)
      
      outfiles.foreach(file => {
        println("File " + file.getAbsolutePath)
        val expected = if (file.getAbsolutePath.contains("name=parameter1/year=2016/month=05/day=10"))
          FileUtils.readFileToString(new File(location + "/parameter1may10json.expected"))
        else if (file.getAbsolutePath.contains("name=parameter1/year=2016/month=05/day=11"))
          FileUtils.readFileToString(new File(location + "/parameter1may11json.expected"))
        else if (file.getAbsolutePath.contains("name=parameter2/year=2016/month=05/day=10"))
          FileUtils.readFileToString(new File(location + "/parameter2may10json.expected"))
        else if (file.getAbsolutePath.contains("name=parameter2/year=2016/month=05/day=11"))
          FileUtils.readFileToString(new File(location + "/parameter2may11json.expected"))
        else if (file.getAbsolutePath.contains("name=parameter3/year=2016/month=05/day=10"))
          FileUtils.readFileToString(new File(location + "/parameter3may10json.expected"))
        else if (file.getAbsolutePath.contains("name=parameter3/year=2016/month=05/day=11"))
          FileUtils.readFileToString(new File(location + "/parameter3may11json.expected"))
        else ""
        val actual = FileUtils.readFileToString(file)
        assert(actual == expected)
      })
    }

    it("should partition using patition key when PartitionBuckets is given") {
      
      inputConfig.adapterSpecificCfg =
        "{\"Uri\": \"file://" + location + "\",\"FileNamePrefix\": \"Data-\", \"PartitionBuckets\": \"2\"}"

      val sfp = new SmartFileProducer(inputConfig, null) with JsonDefaultSerializerDeserializer
      sfp.send(null, data)
      sfp.Shutdown()

      val outfiles = FileUtils.listFiles(dir, Array("dat"), true)
      println("Number of file produced: " + outfiles.size())
      assert(outfiles.size() > 1)
      
      outfiles.foreach(file => {
        println("File " + file.getAbsolutePath)
        val expected = if (file.getAbsolutePath.contains("Data-1-0"))
          FileUtils.readFileToString(new File(location + "/bucket0json.expected"))
        else if (file.getAbsolutePath.contains("Data-1-1"))
          FileUtils.readFileToString(new File(location + "/bucket1json.expected"))
        else ""
        val actual = FileUtils.readFileToString(file)
        assert(actual == expected)
      })
    }

    it("should rollover file when RolloverInterval is given") {
      
      inputConfig.adapterSpecificCfg =
        "{\"Uri\": \"file://" + location + "\",\"FileNamePrefix\": \"Data-\", \"MessageSeparator\": \"\\n\", \"RolloverInterval\": \"1\"}"

      val sfp = new SmartFileProducer(inputConfig, null) with JsonDefaultSerializerDeserializer
      sfp.send(null, data)
      Thread.sleep(60000)
      sfp.send(null, data)
      sfp.Shutdown()

      val outfiles = FileUtils.listFiles(dir, Array("dat"), true)
      println("Number of file produced: " + outfiles.size())
      assert(outfiles.size() > 1)
      
      outfiles.foreach(file => {
        println("File " + file.getAbsolutePath)
        val actual = FileUtils.readFileToString(file)
        val expected = FileUtils.readFileToString(new File(location + "/unpartionedjson.expected"))
        assert(actual == expected)
      })
    }

  }

}
