import sbt.Keys._
import sbt._

sbtPlugin := true

version := "1.5.3"

version in ThisBuild := "1.5.3"

//scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.11.7", "2.10.4")

shellPrompt := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

net.virtualvoid.sbt.graph.Plugin.graphSettings

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

resolvers += Resolver.typesafeRepo("releases")

resolvers += Resolver.sonatypeRepo("releases")

coverageEnabled in ThisBuild := true

val Organization = "com.ligadata"

// This is a task that is defined to assemble the 3 fat jars all at once
// This may be used as part of a build/install script to reduce commands
// or to include as a dependency for another task such as what is done in the MetadataAPI project
val assembleDependencies = TaskKey[Unit]("assembleDependencies")

assembleDependencies in Global := {
  (assembly in ExtDependencyLibs).value
  (assembly in ExtDependencyLibs2).value
  (assembly in HBaseExtDependencyLibs).value
  (assembly in KamanjaBaseDeps).value
  (assembly in KamanjaInternalDeps).value
  (assembly in StorageDeps).value
}

//newly added
lazy val ExtDependencyLibs = project.in(file("ExtDependencyLibs")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild)

lazy val ExtDependencyLibs2 = project.in(file("ExtDependencyLibs2")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided")

lazy val HBaseExtDependencyLibs = project.in(file("HBaseExtDependencyLibs")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

// ------ 1st level Kamanja Dependencies ------ 

lazy val KamanjaVersion = project.in(file("KamanjaVersion")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild) //dependsOn(ExtDependencyLibs)

// ------ 2nd level Kamanja Dependencies ------ 

lazy val Exceptions = project.in(file("Exceptions")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", KamanjaVersion % "provided")

lazy val Metadata = project.in(file("Metadata")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(Exceptions % "provided", ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val BaseTypes = project.in(file("BaseTypes")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata % "provided", Exceptions % "provided")

lazy val Cache = project.in(file("Utils/Cache")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val DataDelimiters = project.in(file("DataDelimiters")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val KamanjaUtils = project.in(file("KamanjaUtils")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", Exceptions % "provided")

lazy val KvBase = project.in(file("KvBase")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild)

lazy val ZooKeeperClient = project.in(file("Utils/ZooKeeper/CuratorClient")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions % "provided")

lazy val ZooKeeperLeaderLatch = project.in(file("Utils/ZooKeeper/CuratorLeaderLatch")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", ZooKeeperClient % "provided", Exceptions % "provided", KamanjaUtils % "provided", KamanjaVersion % "provided")

lazy val ZooKeeperListener = project.in(file("Utils/ZooKeeper/CuratorListener")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", ZooKeeperClient % "provided", Exceptions % "provided")

lazy val HeartBeat = project.in(file("HeartBeat")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", ZooKeeperClient % "provided", ZooKeeperLeaderLatch % "provided", Exceptions % "provided")

lazy val KamanjaBase = project.in(file("KamanjaBase")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata % "provided", Exceptions % "provided", KamanjaUtils % "provided", HeartBeat % "provided", KvBase % "provided", DataDelimiters % "provided", BaseTypes % "provided")

lazy val MetadataBootstrap = project.in(file("MetadataBootstrap/Bootstrap")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata % "provided", KamanjaBase % "provided", BaseTypes % "provided", Exceptions % "provided")

lazy val SecurityAdapterBase = project.in(file("SecurityAdapters/SecurityAdapterBase")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions % "provided")

lazy val StorageBase = project.in(file("Storage/StorageBase")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions % "provided", KamanjaUtils % "provided", KvBase % "provided", KamanjaBase % "provided", HeartBeat % "provided", Metadata % "provided")

lazy val StorageManager = project.in(file("Storage/StorageManager")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", StorageBase % "provided", Exceptions % "provided", KamanjaBase % "provided", KamanjaUtils % "provided", Metadata % "provided")

lazy val TransactionService = project.in(file("TransactionService")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions % "provided", KamanjaBase % "provided", ZooKeeperClient % "provided", StorageBase % "provided", StorageManager % "provided", KvBase % "provided", KamanjaUtils % "provided")

lazy val Dag = project.in(file("Utils/Dag")) dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaUtils % "provided", Exceptions % "provided")

lazy val AuditAdapterBase = project.in(file("AuditAdapters/AuditAdapterBase")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions % "provided")

lazy val MetadataAPIBase = project.in(file("MetadataAPIBase")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", StorageBase % "provided", StorageManager % "provided", Metadata % "provided", ZooKeeperListener % "provided", Exceptions % "provided", KamanjaUtils % "provided", HeartBeat % "provided", KamanjaBase % "provided", AuditAdapterBase % "provided")

lazy val InputOutputAdapterBase = project.in(file("InputOutputAdapters/InputOutputAdapterBase")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions % "provided", DataDelimiters % "provided", HeartBeat % "provided", KamanjaBase % "provided", ZooKeeperClient % "provided", StorageManager % "provided", TransactionService % "provided", Metadata % "provided")

lazy val KamanjaBaseDeps = project.in(file("KamanjaBaseDeps")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", BaseTypes, Cache, DataDelimiters, Exceptions, HeartBeat, InputOutputAdapterBase, KamanjaBase, KamanjaUtils, KvBase, Metadata, MetadataBootstrap, SecurityAdapterBase, StorageBase, StorageManager, TransactionService, ZooKeeperClient, ZooKeeperLeaderLatch, ZooKeeperListener, Dag, MetadataAPIBase, AuditAdapterBase, KamanjaVersion)

// ------ 3rd level Kamanja Dependencies ------ 
lazy val KafkaSimpleInputOutputAdapters = project.in(file("InputOutputAdapters/KafkaSimpleInputOutputAdapters")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val KafkaAdapters_v8 = project.in(file("InputOutputAdapters/KafkaAdapters_v8")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KafkaSimpleInputOutputAdapters % "provided", KamanjaBaseDeps % "provided")

lazy val KafkaAdapters_v9 = project.in(file("InputOutputAdapters/KafkaAdapters_v9")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KafkaSimpleInputOutputAdapters % "provided", KamanjaBaseDeps % "provided")

lazy val KafkaAdapters_v10 = project.in(file("InputOutputAdapters/KafkaAdapters_v10")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KafkaSimpleInputOutputAdapters % "provided", KamanjaBaseDeps % "provided")

// ------ 4th level Kamanja Dependencies ------ 

lazy val Serialize = project.in(file("Utils/Serialize")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", KamanjaBaseDeps % "provided")

// ------ 5th level Kamanja Dependencies ------ 

lazy val StorageCassandra = project.in(file("Storage/Cassandra")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", Serialize)

lazy val StorageH2DB = project.in(file("Storage/H2DB")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", Serialize)

lazy val StorageElasticsearch = project.in(file("Storage/Elasticsearch")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", Serialize)

lazy val StorageHashMap = project.in(file("Storage/HashMap")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", Serialize)

lazy val StorageHBase = project.in(file("Storage/HBase")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", HBaseExtDependencyLibs % "provided", KamanjaBaseDeps % "provided", Serialize)

lazy val StorageTreeMap = project.in(file("Storage/TreeMap")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", Serialize)

lazy val StorageSqlServer = project.in(file("Storage/SqlServer")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", Serialize)

lazy val StorageDeps = project.in(file("Storage/StorageDeps")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", StorageSqlServer, StorageCassandra, StorageHashMap, StorageTreeMap, StorageHBase, StorageH2DB, StorageElasticsearch)

// ------ 6th level Kamanja Dependencies ------ 

lazy val CacheImp = project.in(file("Utils/CacheImp")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val BaseFunctions = project.in(file("BaseFunctions")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided") // has only resolvers , no dependencies

lazy val JsonSerDeser = project.in(file("Utils/AdapterSerializers/JsonSerDeser")) dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val CsvSerDeser = project.in(file("Utils/AdapterSerializers/CsvSerDeser")) dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val KBinarySerDeser = project.in(file("Utils/AdapterSerializers/KBinarySerDeser")) dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val MessageCompiler = project.in(file("MessageCompiler")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val PmmlRuntime = project.in(file("Pmml/PmmlRuntime")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val PmmlUdfs = project.in(file("Pmml/PmmlUdfs")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", PmmlRuntime % "provided", KamanjaBaseDeps % "provided")

lazy val PmmlCompiler = project.in(file("Pmml/PmmlCompiler")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", PmmlRuntime % "provided", PmmlUdfs % "provided", KamanjaBaseDeps % "provided")

lazy val JarFactoryOfModelInstanceFactory = project.in(file("FactoriesOfModelInstanceFactory/JarFactoryOfModelInstanceFactory")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val JpmmlFactoryOfModelInstanceFactory = project.in(file("FactoriesOfModelInstanceFactory/JpmmlFactoryOfModelInstanceFactory")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided",KamanjaBaseDeps % "provided")

lazy val PythonFactoryOfModelInstanceFactory =  project.in(file("FactoriesOfModelInstanceFactory/PythonFactoryOfModelInstanceFactory")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val FileSimpleInputOutputAdapters = project.in(file("InputOutputAdapters/FileSimpleInputOutputAdapters")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val SimpleEnvContextImpl = project.in(file("EnvContexts/SimpleEnvContextImpl")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaBaseDeps % "provided", Serialize % "provided", CacheImp % "provided")

lazy val JsonDataGen = project.in(file("Utils/JsonDataGen")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val Controller = project.in(file("Utils/Controller")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KafkaAdapters_v8 % "provided", KamanjaBaseDeps % "provided", KafkaSimpleInputOutputAdapters % "provided")

lazy val AuditAdapters = project.in(file("Utils/Audit")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", HBaseExtDependencyLibs % "provided", KamanjaBaseDeps % "provided", Serialize % "provided")

lazy val CustomUdfLib = project.in(file("SampleApplication/CustomUdfLib")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", PmmlUdfs % "provided", KamanjaBaseDeps % "provided")

lazy val UtilityService = project.in(file("Utils/UtilitySerivce")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val UtilsForModels = project.in(file("Utils/UtilsForModels")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val runtime = project.in(file("GenerateModels/Runtime")) dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", MessageCompiler % "provided")

lazy val jtm = project.in(file("GenerateModels/jtm")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", MessageCompiler % "provided", runtime % "provided")

lazy val NodeInfoExtract = project.in(file("Utils/NodeInfoExtract")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", Serialize % "provided")

lazy val SmartFileAdapter = project.in(file("InputOutputAdapters/SmartFileAdapter")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", JsonSerDeser % "test")

lazy val ElasticsearchAdapters = project.in(file("InputOutputAdapters/ElasticsearchAdapters")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", JsonSerDeser % "test", StorageElasticsearch % "provided")

lazy val MethodExtractor = project.in(file("Pmml/MethodExtractor")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", PmmlUdfs % "provided", Serialize % "provided", KamanjaBaseDeps % "provided") 

lazy val SimpleApacheShiroAdapter = project.in(file("Utils/Security/SimpleApacheShiroAdapter")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided")

lazy val KamanjaInternalDeps = project.in(file("KamanjaInternalDeps")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", 
  Serialize, PmmlCompiler, 
  JpmmlFactoryOfModelInstanceFactory, JarFactoryOfModelInstanceFactory, InstallDriverBase, BaseFunctions, FileSimpleInputOutputAdapters, SimpleEnvContextImpl,
  MethodExtractor, JsonDataGen, Controller, AuditAdapters, CustomUdfLib, UtilityService,
  UtilsForModels, MessageCompiler, jtm, NodeInfoExtract, SmartFileAdapter, ElasticsearchAdapters, CacheImp, CsvSerDeser, JsonSerDeser, KBinarySerDeser, SimpleApacheShiroAdapter)

// ------ Final (fat) Jars ------ 

lazy val KamanjaManager = project.in(file("KamanjaManager")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

lazy val MetadataAPI = project.in(file("MetadataAPI")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")
  .settings(
    parallelExecution in Test := false,
    test <<= (test in Test).dependsOn(assembleDependencies)
  )

//test in MetadataAPI <<= (test in MetadataAPI) dependsOn ((assembly in KamanjaInternalDeps), (assembly in ExtDependencyLibs), (assembly in ExtDependencyLibs2))
//parallelExecution in Test in MetadataAPI := false

lazy val MetadataAPIService = project.in(file("MetadataAPIService")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided", MetadataAPI % "provided")

lazy val MetadataAPIServiceClient = project.in(file("MetadataAPIServiceClient")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

lazy val ContainersUtility = project.in(file("Utils/ContainersUtility")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

lazy val JsonChecker = project.in(file("Utils/JsonChecker")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

lazy val QueryGenerator = project.in(file("Utils/QueryGenerator")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

lazy val GenerateMessage = project.in(file("Utils/GenerateMessage")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

// lazy val InterfacesSamples = project.in(file("SampleApplication/InterfacesSamples")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(KamanjaBaseDeps % "provided")

lazy val SimpleKafkaProducer = project.in(file("Utils/SimpleKafkaProducer")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided", KafkaSimpleInputOutputAdapters % "provided", KafkaAdapters_v10 % "provided", KafkaAdapters_v9 % "provided", KafkaAdapters_v8 % "provided")

lazy val KVInit = project.in(file("Utils/KVInit")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

lazy val JdbcDataCollector = project.in(file("Utils/JdbcDataCollector")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

lazy val ExtractData = project.in(file("Utils/ExtractData")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

//last commented
//lazy val InterfacesSamples = project.in(file("SampleApplication/InterfacesSamples")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, MetadataBootstrap, MetadataAPIBase, StorageBase, Exceptions)

lazy val FileDataConsumer = project.in(file("FileDataConsumer")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

lazy val CleanUtil = project.in(file("Utils/CleanUtil")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

lazy val SaveContainerDataComponent = project.in(file("Utils/SaveContainerDataComponent")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

lazy val MigrateBase = project.in(file("Utils/Migrate/MigrateBase")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val MigrateFrom_V_1_1 = project.in(file("Utils/Migrate/SourceVersion/MigrateFrom_V_1_1")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase)

lazy val MigrateManager = project.in(file("Utils/Migrate/MigrateManager")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase, KamanjaVersion)

lazy val MigrateFrom_V_1_2 = project.in(file("Utils/Migrate/SourceVersion/MigrateFrom_V_1_2")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase)

lazy val MigrateFrom_V_1_3 = project.in(file("Utils/Migrate/SourceVersion/MigrateFrom_V_1_3")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase)

lazy val MigrateFrom_V_1_4 = project.in(file("Utils/Migrate/SourceVersion/MigrateFrom_V_1_4")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase, KamanjaBaseDeps % "provided")

lazy val MigrateFrom_V_1_4_1 = project.in(file("Utils/Migrate/SourceVersion/MigrateFrom_V_1_4_1")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase, KamanjaBaseDeps % "provided")

lazy val MigrateTo_V_1_5_0 = project.in(file("Utils/Migrate/DestinationVersion/MigrateTo_V_1_5_0")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase, KamanjaBaseDeps % "provided", KamanjaManager % "provided", MetadataAPI % "provided", KamanjaInternalDeps % "provided")

lazy val InstallDriverBase = project.in(file("Utils/ClusterInstaller/InstallDriverBase")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided")

lazy val InstallDriver = project.in(file("Utils/ClusterInstaller/InstallDriver")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", InstallDriverBase, KamanjaInternalDeps % "provided", KamanjaBaseDeps % "provided")

lazy val ClusterInstallerDriver = project.in(file("Utils/ClusterInstaller/ClusterInstallerDriver")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(InstallDriverBase, MigrateBase, MigrateManager)

lazy val GetComponent = project.in(file("Utils/ClusterInstaller/GetComponent")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild) //.dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val PmmlTestTool = project.in(file("Utils/PmmlTestTool")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBaseDeps % "provided", KamanjaInternalDeps % "provided")

lazy val GenerateAdapterBindings = project.in(file("Utils/Migrate/GenerateAdapterBindings")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val HttpEndpoint = project.in(file("Utils/HttpEndpoint")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild)

lazy val DemoKafkaProducer = project.in(file("Utils/DemoKafkaProducer")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(KamanjaBaseDeps % "provided")

lazy val KamanjaUIREST = project.in(file("KamanjaUI/Rest/KamanjaUIRest")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild)

