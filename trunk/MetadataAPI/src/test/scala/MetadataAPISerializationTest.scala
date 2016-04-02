/**
  * Created by Ahmed-Work on 3/17/2016.
  */

package com.ligadata.MetadataAPI

import java.util.Date

import com.ligadata.kamanja.metadata.{ObjFormatType, _}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpec}
import com.ligadata.MetadataAPI.MetadataAPISerialization
import com.ligadata.kamanja.metadataload.MetadataLoad

class MetadataAPISerializationTest extends FlatSpec with BeforeAndAfterAll {

  val nameSpace = "com.ligadata.namespace"
  val name = "name"
  val physicalNameString = "/opt/kamanja/obj"
  val jarName = "JarName"
  val origDef = "OrigDef"
  val objectDefinition = "ObjectDefinition"
  val description = "Description"
  val author = "Author"
  val ownerId = "ownerId"
  val dependencyJar = Array("Jar1", "Jar2")
  val version = 123456789L
  val transId = 123123123123L
  val uniqID = 987654321L
  val creationTime = 2222222222L
  val modTime = 33333333333L
  val mdElemStructVer = 1
  val mdElementId = 1L
  val isActive = true
  val isDeleted = false
  val persist = true
  val supportsInstanceSerialization = true
  val recompile = true
  val isReusable = true

  override def beforeAll() {
    try {
      val mdLoader = new MetadataLoad(MdMgr.mdMgr, "", "", "", "")
      mdLoader.initialize
    }
    catch {
      case e: Exception => throw new Exception("Failed to add messagedef", e)
    }
  }

  "serializeObjectToJson" should "return serialized modelDefJson" in {
    //input
    val modelDef = getModlDef
    //expected
    val expected: String =
      """{"Model":{"Name":"name","PhysicalName":"/opt/kamanja/obj","JarName":"JarName","NameSpace":"com.ligadata.namespace","ModelType":"SCALA","DependencyJars":["Jar1","Jar2"],"ModelRep":"PMML","OrigDef":"OrigDef","OwnerId":"ownerId","ObjectDefinition":"ObjectDefinition","ObjectFormat":"SCALA","Description":"Description","ModelConfig":"modelConfig","Author":"Author","inputMsgSets":[[{"Origin":"origin","Message":"msg","Attributes":["attrebute1","attrebute2"]}]],"OutputMsgs":["outputMessage"],"NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"BooleanTypes":{"IsActive":true,"IsReusable":true,"IsDeleted":false,"Recompile":false,"SupportsInstanceSerialization":true}}}"""
    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(modelDef)
    assert(expected === actual)
  }

  "It" should "return serialized messageDefJson" in {
    //input
    val mssgDef = getMsgDef
    //expected
    val expected: String =
      """{"Message":{"Name":"name","PhysicalName":"/opt/kamanja/obj","JarName":"JarName","NameSpace":"com.ligadata.namespace","DependencyJars":["Jar1","Jar2"],"OrigDef":"OrigDef","ObjectDefinition":"ObjectDefinition","ObjectFormat":"JSON","CreationTime":2222222222,"Author":"Author","OwnerId":"ownerId","SchemaId":1,"AvroSchema":"avroSchema","PartitionKey":["key1","key2"],"IsActive":true,"IsDeleted":false,"Recompile":false,"Persist":true,"Description":"Description","NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"PrimaryKeys":[{"constraintName":"prim","key":["key1","key2"]}],"ForeignKeys":[{"constraintName":"foreign","key":["key1","key2"],"forignContainerName":"forr","forignKey":["key1","key2"]}]}}"""
    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(mssgDef)

    assert(expected === actual)
  }

  "It" should "return serialized containerDefJson" in {
    //input
    var conDef = getContainerDef
    //expected
    val expected: String =
      """{"Container":{"Name":"name","PhysicalName":"/opt/kamanja/obj","JarName":"JarName","NameSpace":"com.ligadata.namespace","DependencyJars":["Jar1","Jar2"],"OrigDef":"OrigDef","ObjectDefinition":"ObjectDefinition","ObjectFormat":"JSON","CreationTime":2222222222,"Author":"Author","OwnerId":"ownerId","SchemaId":2,"AvroSchema":"avroSchema","Persist":true,"PartitionKey":["key1","key2"],"IsActive":true,"IsDeleted":false,"Recompile":false,"Description":"Description","NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"PrimaryKeys":[{"constraintName":"prim","key":["key1","key2"]}],"ForeignKeys":[{"constraintName":"foreign","key":["key1","key2"],"forignContainerName":"forr","forignKey":["key1","key2"]}]}}"""
    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(conDef)

    assert(expected === actual)
  }

  "It" should "return serialized functionDefJson" in {
    //input
    val funDef = getFunctionDef
    //expected
    val expected: String =
      """{"Function":{"Name":"name","PhysicalName":"/opt/kamanja/obj","JarName":"JarName","NameSpace":"com.ligadata.namespace","DependencyJars":["Jar1","basetypes_2.10-0.1.0.jar","metadata_2.10-1.0.jar","Jar2"],"OrigDef":"OrigDef","ObjectDefinition":"ObjectDefinition","ObjectFormat":"JSON","Author":"Author","OwnerId":"ownerId","Arguments":[{"ArgName":"type1","ArgTypeNameSpace":"system","ArgTypeName":"int"},{"ArgName":"type2","ArgTypeNameSpace":"system","ArgTypeName":"int"}],"Features":["CLASSUPDATE","HAS_INDEFINITE_ARITY"],"ReturnTypeNameSpace":"system","ReturnTypeName":"int","ClassName":"className","Recompile":false,"Description":"Description","NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"IsActive":true,"IsDeleted":false}}"""
    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(funDef)

    assert(expected === actual)
  }

  "It" should "return serialized MapTypeDefJson" in {
    //input
    val mapTypeDef = getMapTypeDef
    //expected
    val expected: String =
      """{"MapType":{"Name":"name","NameSpace":"com.ligadata.namespace","PhysicalName":"/opt/kamanja/obj","TypeTypeName":"tContainer","JarName":"JarName","ObjectFormat":"JSON","DependencyJars":["Jar1","Jar2"],"Implementation":"implementationName","KeyTypeNameSpace":"system","KeyTypeName":"String","ValueTypeNameSpace":"system","ValueTypeName":"String","ObjectDefinition":"ObjectDefinition","OrigDef":"OrigDef","Author":"Author","Recompile":false,"OwnerId":"ownerId","Description":"Description","NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"IsActive":true,"IsFixed":false,"IsDeleted":false}}"""
    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(mapTypeDef)

    assert(expected === actual)
  }

  "It" should "return serialized ArrayTypeDefJson" in {
    //input
    val arrayType = getArrayTypeDef
    //expected
    val expected: String =
      """{"ArrayType":{"Name":"name","NameSpace":"com.ligadata.namespace","PhysicalName":"/opt/kamanja/obj","TypeTypeName":"tScalar","TypeName":"Int","TypeNameSpace":"system","NumberOfDimensions":2,"JarName":"JarName","ObjectFormat":"JSON","DependencyJars":["Jar1","Jar2"],"Implementation":"implementationName","ObjectDefinition":"ObjectDefinition","OrigDef":"OrigDef","OwnerId":"ownerId","Author":"Author","Recompile":false,"Description":"Description","NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"IsActive":true,"IsFixed":false,"IsDeleted":false}}"""
    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(arrayType)
    assert(expected === actual)
  }

  "It" should "return serialized JarDefJson" in {
    //input
    val jar = getJarDef
    //expected
    val expected: String =
      """{"Jar":{"IsActive":true,"IsDeleted":false,"OrigDef":"OrigDef","ObjectDefinition":"ObjectDefinition","ObjectFormat":"JSON","NameSpace":"com.ligadata.namespace","Name":"name","Author":"Author","OwnerId":"ownerId","PhysicalName":"/opt/kamanja/obj","JarName":"JarName","DependencyJars":["Jar1","Jar2"],"NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"Description":"Description"}}""".stripMargin
    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(jar)

    assert(expected === actual)
  }

  "It" should "return serialized ConfigDefJson" in {
    //input
    val config = getConfigDef
    //expected
    val expected: String =
      """{"Config":{"IsActive":true,"IsDeleted":false,"OrigDef":"OrigDef","ObjectDefinition":"ObjectDefinition","ObjectFormat":"JSON","NameSpace":"com.ligadata.namespace","Contents":"Contents","OwnerId":"ownerId","Name":"name","Author":"Author","PhysicalName":"/opt/kamanja/obj","JarName":"JarName","DependencyJars":["Jar1","Jar2"],"NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"Description":"Description"}}"""
    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(config)

    assert(expected === actual)
  }

  "It" should "return serialized NodeInfoJson" in {
    //input
    val nodeInfo = getNodeInfo
    //expected
    val expected: String =
      """{"Node":{"NodeId":"1","NodePort":2021,"NodeIpAddr":"localhost","JarPaths":["path1","path2"],"Scala_home":"/usr/bin","Java_home":"/usr/bin","Classpath":"/class/path","ClusterId":"1","Power":2,"Roles":["role1","role2"],"Description":"description"}}"""

    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(nodeInfo)

    assert(expected === actual)
  }

  "It" should "return serialized ClusterInfoJson" in {
    //input
    val clusterInfo = getClusterInfo
    //expected
    val expected: String =
      """{"Cluster":{"ClusterId":"1","Description":"description","Privileges":"privilges"}}"""

    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(clusterInfo)
    assert(expected === actual)
  }

  "It" should "return serialized ClusterCfgInfoJson" in {
    //input
    val clusterCfgInfo = getClusterCfgInfo
    //expected
    val expected: String =
      """{"ClusterCfg":{"ClusterId":"clusterId","CfgMap":[{"Key":"key2","Value":"value2"},{"Key":"key1","Value":"value1"}],"ModifiedTime":1458952764,"CreatedTime":1459039164,"UsrConfigs":[{"Key":"key2","Value":"value2"},{"Key":"key1","Value":"value1"}]}}"""

    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(clusterCfgInfo)
    assert(expected === actual)
  }

  "It" should "return serialized AdapterInfoJson" in {
    //input
    val adapterInfo = getAdapterInfo
    //expected
    val expected: String =
      """{"Adapter":{"Name":"name","TypeString":"typeString","DataFormat":"dataformat","ClassName":"ClassName","JarName":"JarName","DependencyJars":["Jar1","Jar2"],"AdapterSpecificCfg":"AdapterSpecificCfg"}}"""

    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(adapterInfo)

    assert(expected === actual)
  }

  "It" should "return serialized UserPropertiesInfoJson" in {
    //input
    val userPropertiesInfo = getUserPropertiesInfo
    //expected
    val expected: String =
      """{"UserProperties":{"ClusterId":"ClusterId","Props":[{"Key":"key2","Value":"value2"},{"Key":"key1","Value":"value1"}]}}"""

    //actual
    val actual = MetadataAPISerialization.serializeObjectToJson(userPropertiesInfo)

    assert(expected === actual)
  }

  "deserializeMetadata" should "return serialized ModelDef" in {

    //input
    val input: String =
      """{"Model":{"Name":"name","PhysicalName":"/opt/kamanja/obj","JarName":"JarName","NameSpace":"com.ligadata.namespace","ModelType":"SCALA","DependencyJars":["Jar1","Jar2"],"ModelRep":"PMML","OrigDef":"OrigDef","OwnerId":"ownerId","ObjectDefinition":"ObjectDefinition","ObjectFormat":"SCALA","Description":"Description","ModelConfig":"modelConfig","Author":"Author","inputMsgSets":[[{"Origin":"origin","Message":"msg","Attributes":["attrebute1","attrebute2"]}]],"OutputMsgs":["outputMessage"],"NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"BooleanTypes":{"IsActive":true,"IsReusable":true,"IsDeleted":false,"Recompile":false,"SupportsInstanceSerialization":true}}}"""
    //expected
    val expected = getModlDef

    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[ModelDef]
    assert(expected.Name === actual.Name)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.NameSpace === actual.NameSpace)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.miningModelType.toString === actual.miningModelType.toString)
    assert(expected.modelRepresentation.toString === actual.modelRepresentation.toString)
    assert(expected.OrigDef === actual.OrigDef)
    assert(expected.OwnerId === actual.OwnerId)
    assert(expected.Author === actual.Author)
    assert(expected.UniqId === actual.UniqId)
    assert(expected.MdElementId === actual.MdElementId)
    assert(expected.IsDeleted === actual.IsDeleted)
    assert(expected.isReusable === actual.isReusable)
    assert(true === isInputMsgSetsEqual(expected.inputMsgSets, actual.inputMsgSets))
    assert(expected.outputMsgs.sameElements(actual.outputMsgs))
    assert(expected.dependencyJarNames.sameElements(actual.dependencyJarNames))
  }
  "It" should "return serialized MessageDef" in {

    //input
    val input: String =
      """{"Message":{"Name":"name","PhysicalName":"/opt/kamanja/obj","JarName":"JarName","NameSpace":"com.ligadata.namespace","DependencyJars":["Jar1","Jar2"],"OrigDef":"OrigDef","ObjectDefinition":"ObjectDefinition","ObjectFormat":"JSON","CreationTime":2222222222,"Author":"Author","OwnerId":"ownerId","SchemaId":1,"AvroSchema":"avroSchema","PartitionKey":["key1","key2"],"IsActive":true,"IsDeleted":false,"Recompile":false,"Persist":true,"Description":"Description","NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"PrimaryKeys":[{"constraintName":"prim","key":["key1","key2"]}],"ForeignKeys":[{"constraintName":"foreign","key":["key1","key2"],"forignContainerName":"forr","forignKey":["key1","key2"]}]}}"""

    //expected
    val expected = getMsgDef

    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[MessageDef]
    assert(expected.Name === actual.Name)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.NameSpace === actual.NameSpace)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.cType.schemaId === actual.cType.schemaId)
    assert(expected.OrigDef === actual.OrigDef)
    assert(expected.OwnerId === actual.OwnerId)
    assert(expected.Author === actual.Author)
    assert(expected.UniqId === actual.UniqId)
    assert(expected.MdElementId === actual.MdElementId)
    assert(expected.IsDeleted === actual.IsDeleted)
    assert(expected.cType.avroSchema === actual.cType.avroSchema)
    assert(expected.dependencyJarNames.sameElements(actual.dependencyJarNames))

  }
  "It" should "return serialized ContainerDef" in {

    //input
    val input: String =
      """{"Container":{"Name":"name","PhysicalName":"/opt/kamanja/obj","JarName":"JarName","NameSpace":"com.ligadata.namespace","DependencyJars":["Jar1","Jar2"],"OrigDef":"OrigDef","ObjectDefinition":"ObjectDefinition","ObjectFormat":"JSON","CreationTime":2222222222,"Author":"Author","OwnerId":"ownerId","SchemaId":2,"AvroSchema":"avroSchema","Persist":true,"PartitionKey":["key1","key2"],"IsActive":true,"IsDeleted":false,"Recompile":false,"Description":"Description","NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"PrimaryKeys":[{"constraintName":"prim","key":["key1","key2"]}],"ForeignKeys":[{"constraintName":"foreign","key":["key1","key2"],"forignContainerName":"forr","forignKey":["key1","key2"]}]}}"""
    //expected
    val expected = getContainerDef
    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[ContainerDef]
    assert(expected.Name === actual.Name)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.NameSpace === actual.NameSpace)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.cType.schemaId === actual.cType.schemaId)
    assert(expected.OrigDef === actual.OrigDef)
    assert(expected.OwnerId === actual.OwnerId)
    assert(expected.Author === actual.Author)
    assert(expected.UniqId === actual.UniqId)
    assert(expected.MdElementId === actual.MdElementId)
    assert(expected.IsDeleted === actual.IsDeleted)
    assert(expected.cType.avroSchema === actual.cType.avroSchema)
  }
  "It" should "return serialized FunctionDef" in {

    //input
    val input: String =
      """{"Function":{"Name":"name","PhysicalName":"/opt/kamanja/obj","JarName":"JarName","NameSpace":"com.ligadata.namespace","DependencyJars":["Jar1","basetypes_2.10-0.1.0.jar","metadata_2.10-1.0.jar","Jar2"],"OrigDef":"OrigDef","ObjectDefinition":"ObjectDefinition","ObjectFormat":"JSON","Author":"Author","OwnerId":"ownerId","Arguments":[{"ArgName":"type1","ArgTypeNameSpace":"system","ArgTypeName":"int"},{"ArgName":"type2","ArgTypeNameSpace":"system","ArgTypeName":"int"}],"Features":["CLASSUPDATE","HAS_INDEFINITE_ARITY"],"ReturnTypeNameSpace":"system","ReturnTypeName":"int","ClassName":"className","Recompile":false,"Description":"Description","NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"IsActive":true,"IsDeleted":false}}"""
    //expected
    val expected = getFunctionDef
    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[FunctionDef]
    assert(expected.Name === actual.Name)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.NameSpace === actual.NameSpace)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.retType.nameSpace === actual.retType.nameSpace)
    assert(expected.retType.name === actual.retType.name)
    assert(expected.OrigDef === actual.OrigDef)
    assert(expected.OwnerId === actual.OwnerId)
    assert(expected.Author === actual.Author)
    assert(expected.UniqId === actual.UniqId)
    assert(expected.MdElementId === actual.MdElementId)
    assert(expected.IsDeleted === actual.IsDeleted)
    assert(expected.className === actual.className)
    assert(expected.IsDeleted === actual.IsDeleted)

  }
  "It" should "return serialized MapTypeDef" in {

    //input
    val input: String =
      """{"MapType":{"Name":"name","NameSpace":"com.ligadata.namespace","PhysicalName":"/opt/kamanja/obj","TypeTypeName":"tContainer","JarName":"JarName","ObjectFormat":"JSON","DependencyJars":["Jar1","Jar2"],"Implementation":"implementationName","KeyTypeNameSpace":"system","KeyTypeName":"String","ValueTypeNameSpace":"system","ValueTypeName":"String","ObjectDefinition":"ObjectDefinition","OrigDef":"OrigDef","Author":"Author","Recompile":false,"OwnerId":"ownerId","Description":"Description","NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"IsActive":true,"IsFixed":false,"IsDeleted":false}}"""
    //expected
    val expected = getMapTypeDef
    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[MapTypeDef]
    assert(expected.Name === actual.Name)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.NameSpace === actual.NameSpace)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.implementationName === actual.implementationName)
    assert(expected.keyDef.nameSpace === actual.keyDef.nameSpace)
    assert(expected.OrigDef === actual.OrigDef)
    assert(expected.OwnerId === actual.OwnerId)
    assert(expected.Author === actual.Author)
    assert(expected.UniqId === actual.UniqId)
    assert(expected.MdElementId === actual.MdElementId)
    assert(expected.IsDeleted === actual.IsDeleted)
    assert(expected.valDef.nameSpace === actual.valDef.nameSpace)
    assert(expected.IsDeleted === actual.IsDeleted)

  }
  "It" should "return serialized ArrayTypeDef" in {

    //input
    val input: String =
      """{"ArrayType":{"Name":"name","NameSpace":"com.ligadata.namespace","PhysicalName":"/opt/kamanja/obj","TypeTypeName":"tScalar","TypeName":"Int","TypeNameSpace":"system","NumberOfDimensions":2,"JarName":"JarName","ObjectFormat":"JSON","DependencyJars":["Jar1","Jar2"],"Implementation":"implementationName","ObjectDefinition":"ObjectDefinition","OrigDef":"OrigDef","OwnerId":"ownerId","Author":"Author","Recompile":false,"Description":"Description","NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"IsActive":true,"IsFixed":false,"IsDeleted":false}}"""
    //expected
    val expected = getArrayTypeDef
    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[ArrayTypeDef]
    assert(expected.Name === actual.Name)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.NameSpace === actual.NameSpace)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.arrayDims === actual.arrayDims)
    assert(expected.implementationName === actual.implementationName)
    assert(expected.OrigDef === actual.OrigDef)
    assert(expected.OwnerId === actual.OwnerId)
    assert(expected.Author === actual.Author)
    assert(expected.UniqId === actual.UniqId)
    assert(expected.MdElementId === actual.MdElementId)
    assert(expected.IsDeleted === actual.IsDeleted)
    assert(expected.IsFixed === actual.IsFixed)
  }
  "It" should "return serialized JarDef" in {

    //input
    val input: String =
      """{"Jar":{"IsActive":true,"IsDeleted":false,"OrigDef":"OrigDef","ObjectDefinition":"ObjectDefinition","ObjectFormat":"JSON","NameSpace":"com.ligadata.namespace","Name":"name","Author":"Author","OwnerId":"ownerId","PhysicalName":"/opt/kamanja/obj","JarName":"JarName","DependencyJars":["Jar1","Jar2"],"NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"Description":"Description"}}""".stripMargin
    //expected
    val expected = getJarDef
    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[JarDef]
    assert(expected.Name === actual.Name)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.NameSpace === actual.NameSpace)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.OrigDef === actual.OrigDef)
    assert(expected.OwnerId === actual.OwnerId)
    assert(expected.Author === actual.Author)
    assert(expected.UniqId === actual.UniqId)
    assert(expected.MdElementId === actual.MdElementId)
    assert(expected.IsDeleted === actual.IsDeleted)
  }
  "It" should "return serialized ConfigDef" in {

    //input
    val input: String =
      """{"Config":{"IsActive":true,"IsDeleted":false,"OrigDef":"OrigDef","ObjectDefinition":"ObjectDefinition","ObjectFormat":"JSON","NameSpace":"com.ligadata.namespace","Contents":"Contents","OwnerId":"ownerId","Name":"name","Author":"Author","PhysicalName":"/opt/kamanja/obj","JarName":"JarName","DependencyJars":["Jar1","Jar2"],"NumericTypes":{"Version":123456789,"TransId":123123123123,"UniqId":987654321,"CreationTime":2222222222,"ModTime":33333333333,"MdElemStructVer":1,"MdElementId":1},"Description":"Description"}}"""
    //expected
    val expected = getConfigDef
    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[ConfigDef]
    assert(expected.Name === actual.Name)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.NameSpace === actual.NameSpace)
    assert(expected.PhysicalName === actual.PhysicalName)
    assert(expected.OrigDef === actual.OrigDef)
    assert(expected.OwnerId === actual.OwnerId)
    assert(expected.Author === actual.Author)
    assert(expected.UniqId === actual.UniqId)
    assert(expected.MdElementId === actual.MdElementId)
    assert(expected.IsDeleted === actual.IsDeleted)
  }
  "It" should "return serialized NodeInfo" in {

    //input
    val input: String =
      """{"Node":{"NodeId":"1","NodePort":2021,"NodeIpAddr":"localhost","JarPaths":["path1","path2"],"Scala_home":"/usr/bin","Java_home":"/usr/bin","Classpath":"/class/path","ClusterId":"1","Power":2,"Roles":["role1","role2"],"Description":"description"}}"""
    //expected
    val expected = getNodeInfo
    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[NodeInfo]
    assert(expected.NodeId === actual.NodeId)
    assert(expected.NodePort === actual.NodePort)
    assert(expected.NodeIpAddr === actual.NodeIpAddr)
    assert(expected.Scala_home === actual.Scala_home)
    assert(expected.Java_home === actual.Java_home)
    assert(expected.Classpath === actual.Classpath)
    assert(expected.ClusterId === actual.ClusterId)
    assert(expected.Power === actual.Power)
    assert(expected.Description === actual.Description)

  }
  "It" should "return serialized ClusterInfo" in {

    //input
    val input: String =
      """{"Cluster":{"ClusterId":"1","Description":"description","Privileges":"privilges"}}"""
    //expected
    val expected = getClusterInfo
    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[ClusterInfo]
    assert(expected.ClusterId === actual.ClusterId)
    assert(expected.Description === actual.Description)
    assert(expected.Privileges === actual.Privileges)
  }
  "It" should "return serialized ClusterCfgInfo" in {

    //input
    val input: String =
      """{"ClusterCfg":{"ClusterId":"clusterId","CfgMap":[{"Key":"key2","Value":"value2"},{"Key":"key1","Value":"value1"}],"ModifiedTime":1458952764,"CreatedTime":1459039164,"UsrConfigs":[{"Key":"key2","Value":"value2"},{"Key":"key1","Value":"value1"}]}}"""
    //expected
    val expected = getClusterCfgInfo
    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[ClusterCfgInfo]
    assert(expected.ClusterId === actual.ClusterId)
    assert(expected.ModifiedTime.getTime === actual.ModifiedTime.getTime)
    assert(expected.CreatedTime.getTime === actual.CreatedTime.getTime)
  }
  "It" should "return serialized AdapterInfo" in {

    //input
    val input: String =
      """{"Adapter":{"Name":"name","TypeString":"typeString","DataFormat":"dataformat","ClassName":"ClassName","JarName":"JarName","DependencyJars":["Jar1","Jar2"],"AdapterSpecificCfg":"AdapterSpecificCfg"}}"""
    //expected
    val expected = getAdapterInfo
    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[AdapterInfo]
    assert(expected.Name === actual.Name)
    assert(expected.TypeString === actual.TypeString)
    assert(expected.DataFormat === actual.DataFormat)
    assert(expected.ClassName === actual.ClassName)
    assert(expected.AdapterSpecificCfg === actual.AdapterSpecificCfg)
  }
  "It" should "return serialized UserPropertiesInfo" in {

    //input
    val input: String =
      """{"UserProperties":{"ClusterId":"ClusterId","Props":[{"Key":"key2","Value":"value2"},{"Key":"key1","Value":"value1"}]}}"""
    //expected
    val expected = getUserPropertiesInfo
    //actual
    val actual = MetadataAPISerialization.deserializeMetadata(input).asInstanceOf[UserPropertiesInfo]
    assert(expected.ClusterId === actual.ClusterId)
  }


  private def getModlDef: ModelDef = {
    val modelType = "SCALA"
    val objectFormat = "SCALA"
    val partitionKey = new Array[String](2)
    partitionKey(0) = "key1"
    partitionKey(1) = "key2"

    val inputMsgSets = new Array[Array[MessageAndAttributes]](1)
    val attrebutes = Array("attrebute1", "attrebute2")
    val msgAndAttrib = new MessageAndAttributes()
    msgAndAttrib.attributes = attrebutes
    msgAndAttrib.origin = "origin"
    msgAndAttrib.message = "msg"
    val msgAndAttribs = new Array[MessageAndAttributes](1)
    msgAndAttribs(0) = msgAndAttrib
    inputMsgSets(0) = msgAndAttribs
    val modelConfig = "modelConfig"

    val outputMessages = Array("outputMessage")
    val modelDef = MdMgr.GetMdMgr.MakeModelDef(nameSpace, name, physicalNameString, ownerId, uniqID,
      mdElementId, ModelRepresentation.modelRep("PMML"), inputMsgSets, outputMessages, isReusable, objectDefinition, MiningModelType.modelType(modelType),
      version, jarName, dependencyJar, recompile, supportsInstanceSerialization, modelConfig)

    modelDef.ObjectDefinition(objectDefinition)
    val objFmt: ObjFormatType.FormatType = ObjFormatType.fromString(objectFormat)
    modelDef.ObjectFormat(objFmt)
    modelDef.tranId = transId
    modelDef.origDef = origDef
    modelDef.uniqueId = uniqID
    modelDef.creationTime = creationTime
    modelDef.modTime = modTime
    modelDef.description = description
    modelDef.author = author
    modelDef.mdElemStructVer = mdElemStructVer
    modelDef.active = isActive
    modelDef.deleted = isDeleted
    modelDef
  }

  private def getMsgDef: MessageDef = {

    val objectFormat = "JSON"
    val schemaId = 1
    val avroSchema = "avroSchema"
    val mdElemStructVer = 1
    val attrList1 = Array.empty[(String, String, String, String, Boolean, String)]
    val partitionKey = Array("key1", "key2")
    val key = List("key1", "key2")
    val primaryKeys = List(("prim", key))
    val foreignKeys = List(("foreign", key, "forr", key))

    val msgDef = MdMgr.GetMdMgr.MakeFixedMsg(
      nameSpace, name, physicalNameString, attrList1.toList, ownerId,
      uniqID, mdElementId, schemaId, avroSchema,
      version, jarName, dependencyJar, primaryKeys, foreignKeys, partitionKey, recompile)

    msgDef.tranId = transId
    msgDef.origDef = origDef
    msgDef.ObjectDefinition(objectDefinition)
    val objFmt: ObjFormatType.FormatType = ObjFormatType.fromString(objectFormat)
    msgDef.ObjectFormat(objFmt)
    msgDef.uniqueId = uniqID
    msgDef.creationTime = creationTime
    msgDef.modTime = modTime
    msgDef.description = description
    msgDef.author = author
    msgDef.mdElemStructVer = mdElemStructVer
    msgDef.cType.persist = persist
    msgDef.active = isActive
    msgDef.deleted = isDeleted
    msgDef
  }

  private def getContainerDef: ContainerDef = {

    val objectFormat = "JSON"
    val attrList1 = Array.empty[(String, String, String, String, Boolean, String)]
    val partitionKey = Array("key1", "key2")
    val key = List("key1", "key2")
    val primaryKeys = List(("prim", key))
    val foreignKeys = List(("foreign", key, "forr", key))
    val schemaId = 2
    val avroSchema = "avroSchema"
    val contDef = MdMgr.GetMdMgr.MakeFixedContainer(nameSpace, name, physicalNameString, attrList1.toList, ownerId,
      uniqID, mdElementId, schemaId, avroSchema, version, jarName, dependencyJar, primaryKeys, foreignKeys, partitionKey, recompile)

    contDef.tranId = transId
    contDef.origDef = origDef
    contDef.ObjectDefinition(objectDefinition)
    val objFmt: ObjFormatType.FormatType = ObjFormatType.fromString(objectFormat)
    contDef.ObjectFormat(objFmt)
    contDef.creationTime = creationTime
    contDef.modTime = modTime
    contDef.description = description
    contDef.author = author
    contDef.mdElemStructVer = mdElemStructVer
    contDef.cType.persist = persist
    contDef.active = isActive
    contDef.deleted = isDeleted
    contDef
  }

  private def getFunctionDef: FunctionDef = {

    val objectFormat = "JSON"
    val className = "className"
    val returnTypeNameSpace = "system"
    val returnTypeName = "int"
    val argList = List(("type1", "system", "int"), ("type2", "system", "int"))
    var featureSet: scala.collection.mutable.Set[FcnMacroAttr.Feature] = scala.collection.mutable.Set[FcnMacroAttr.Feature]()
    featureSet += FcnMacroAttr.fromString("CLASSUPDATE")
    featureSet += FcnMacroAttr.fromString("HAS_INDEFINITE_ARITY")



    val functionDef = MdMgr.GetMdMgr.MakeFunc(nameSpace, name, physicalNameString, (returnTypeNameSpace, returnTypeName),
      argList, featureSet, ownerId, uniqID, mdElementId, version, jarName, dependencyJar)

    functionDef.tranId = transId
    functionDef.origDef = origDef
    functionDef.ObjectDefinition(objectDefinition)
    val objFmt: ObjFormatType.FormatType = ObjFormatType.fromString(objectFormat)
    functionDef.ObjectFormat(objFmt)
    functionDef.creationTime = creationTime
    functionDef.modTime = modTime
    functionDef.description = description
    functionDef.author = author
    functionDef.mdElemStructVer = mdElemStructVer
    functionDef.className = className
    functionDef.active = isActive
    functionDef.deleted = isDeleted
    functionDef
  }

  private def getMapTypeDef: MapTypeDef = {


    val objectFormat = "JSON"
    val implementationName = "implementationName"
    val key = ("system", "string")
    val value = ("system", "string")

    val mapType = MdMgr.GetMdMgr.MakeMap(nameSpace, name, key, value, version, ownerId, uniqID, mdElementId, recompile)

    mapType.tranId = transId
    mapType.origDef = origDef
    mapType.ObjectDefinition(objectDefinition)
    val objFmt: ObjFormatType.FormatType = ObjFormatType.fromString(objectFormat)
    mapType.ObjectFormat(objFmt)
    mapType.uniqueId = uniqID
    mapType.creationTime = creationTime
    mapType.modTime = modTime
    mapType.description = description
    mapType.author = author
    mapType.mdElemStructVer = mdElemStructVer
    mapType.implementationName(implementationName)
    mapType.active = isActive
    mapType.deleted = isDeleted
    mapType.dependencyJarNames = dependencyJar
    mapType.jarName = jarName
    mapType.physicalName = physicalNameString

    mapType
  }

  private def getArrayTypeDef: ArrayTypeDef = {

    val objectFormat = "JSON"
    val implementationName = "implementationName"
    val numberOfDimention = 2
    val typeNameSpace = "system"
    val typeName = "int"
    val arrayType = MdMgr.GetMdMgr.MakeArray(nameSpace, name, typeNameSpace, typeName, numberOfDimention, ownerId, uniqID, mdElementId, version, recompile)

    arrayType.tranId = transId
    arrayType.origDef = origDef
    arrayType.ObjectDefinition(objectDefinition)
    val objFmt: ObjFormatType.FormatType = ObjFormatType.fromString(objectFormat)
    arrayType.ObjectFormat(objFmt)
    arrayType.uniqueId = uniqID
    arrayType.creationTime = creationTime
    arrayType.modTime = modTime
    arrayType.description = description
    arrayType.author = author
    arrayType.mdElemStructVer = mdElemStructVer
    arrayType.implementationName(implementationName)
    arrayType.active = isActive
    arrayType.deleted = isDeleted
    arrayType.dependencyJarNames = dependencyJar
    arrayType.jarName = jarName
    arrayType.physicalName = physicalNameString

    arrayType
  }

  private def getJarDef: JarDef = {

    val objectFormat = "JSON"
    val isActive = true
    val isDeleted = false


    val jar = MdMgr.GetMdMgr.MakeJarDef(nameSpace, name, version.toString, ownerId, uniqID, mdElementId)

    jar.tranId = transId
    jar.origDef = origDef
    jar.ObjectDefinition(objectDefinition)
    val objFmt: ObjFormatType.FormatType = ObjFormatType.fromString(objectFormat)
    jar.ObjectFormat(objFmt)
    jar.uniqueId = uniqID
    jar.creationTime = creationTime
    jar.modTime = modTime
    jar.description = description
    jar.author = author
    jar.mdElemStructVer = mdElemStructVer
    jar.active = isActive
    jar.deleted = isDeleted
    jar.dependencyJarNames = dependencyJar
    jar.jarName = jarName
    jar.physicalName = physicalNameString

    jar
  }

  private def getConfigDef: ConfigDef = {

    val objectFormat = "JSON"
    val contents = "Contents"
    val config = new ConfigDef

    config.nameSpace = nameSpace;
    config.name = name;
    config.ver = version;
    config.tranId = transId
    config.origDef = origDef
    config.ObjectDefinition(objectDefinition)
    val objFmt: ObjFormatType.FormatType = ObjFormatType.fromString(objectFormat)
    config.ObjectFormat(objFmt)
    config.uniqueId = uniqID
    config.creationTime = creationTime
    config.modTime = modTime
    config.description = description
    config.author = author
    config.mdElemStructVer = mdElemStructVer
    config.active = isActive
    config.deleted = isDeleted
    config.dependencyJarNames = dependencyJar
    config.jarName = jarName
    config.physicalName = physicalNameString
    config.contents = contents
    config.ownerId = ownerId
    config.mdElementId = mdElementId
    config
  }

  private def getNodeInfo: NodeInfo = {

    val nodeId = "1"
    val nodePort = 2021
    val nodeIpAddr = "localhost"
    val jarPaths = new Array[String](2)
    jarPaths(0) = "path1"
    jarPaths(1) = "path2"

    val scala_home = "/usr/bin"
    val java_home = "/usr/bin"
    val classpath = "/class/path"
    val clusterId = "1"
    val power = 2
    val roles = new Array[String](2)
    roles(0) = "role1"
    roles(1) = "role2"
    val description = "description"

    val nodeInfo = MdMgr.GetMdMgr.MakeNode(
      nodeId,
      nodePort,
      nodeIpAddr,
      jarPaths.toList,
      scala_home,
      java_home,
      classpath,
      clusterId,
      power,
      roles,
      description)
    nodeInfo
  }

  private def getClusterInfo: ClusterInfo = {

    val clusterId = "1"
    val description = "description"
    val privilges = "privilges"

    val clusterInfo = MdMgr.GetMdMgr.MakeCluster(
      clusterId,
      description,
      privilges
    )
    clusterInfo
  }

  private def getClusterCfgInfo: ClusterCfgInfo = {

    val clusterId = "clusterId"

    val cfgMap = new scala.collection.mutable.HashMap[String, String]
    cfgMap.put("key1", "value1")
    cfgMap.put("key2", "value2")

    val clusterCfgInfo = MdMgr.GetMdMgr.MakeClusterCfg(
      clusterId,
      cfgMap,
      new Date(1458952764),
      new Date(1459039164)
    )
    val usrConfigs = new scala.collection.mutable.HashMap[String, String]

    usrConfigs.put("key1", "value1")
    usrConfigs.put("key2", "value2")
    clusterCfgInfo.usrConfigs = usrConfigs

    clusterCfgInfo
  }

  private def getAdapterInfo: AdapterInfo = {
    val typeString = "typeString"
    val dataFormat = "dataformat"
    val className = "ClassName"
    val adapterSpecificCfg = "AdapterSpecificCfg"

    val adapterInfo = MdMgr.GetMdMgr.MakeAdapter(
      name,
      typeString,
      dataFormat,
      className,
      jarName,
      dependencyJar.toList,
      adapterSpecificCfg
    )
    adapterInfo
  }

  private def getUserPropertiesInfo: UserPropertiesInfo = {
    val clusterId = "ClusterId"

    val props = new scala.collection.mutable.HashMap[String, String]
    props.put("key1", "value1")
    props.put("key2", "value2")

    val upi = new UserPropertiesInfo
    upi.clusterId = clusterId
    upi.props = props
    upi

  }

  private def isInputMsgSetsEqual(actual: Array[Array[MessageAndAttributes]], expected: Array[Array[MessageAndAttributes]]): Boolean = {

    if (actual.length != expected.length)
      return false

    var count = 0
    actual.foreach(m => {
      if (m.length != expected(count).length)
        return false

      var count2 = 0
      m.foreach(f => {
        if (f.message != expected(count)(count2).message || f.origin != expected(count)(count2).origin || !f.attributes.sameElements(expected(count)(count2).attributes))
          return false
        count2 += 1
      })
      count += 1
    })
    true
  }

}
