import sbt.Keys._
import sbt._

sbtPlugin := true

version := "1.6.2"

version in ThisBuild := "1.6.2"

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
  (packageBin in Compile in KafkaAdapters_v10).value
  (assembly in Test in ExtDependencyLibs).value
  (assembly in Test in ExtDependencyLibs2).value
  (assembly in Test in KamanjaInternalDeps).value
}

// sbt-site scaladoc plugin for generating api documentation
//enablePlugins(SiteScaladocPlugin)

enablePlugins(SphinxPlugin)

sourceDirectory in Sphinx := baseDirectory.value / "docs" / "source"

lazy val docSettings = Seq(
  target in Compile in doc := (baseDirectory in ThisBuild).value / "target" / "sphinx" / "docs" / "api" / name.value
)

//newly added
lazy val ExtDependencyLibs = project.in(file("ExtDependencyLibs")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild)

lazy val ExtDependencyLibs2 = project.in(file("ExtDependencyLibs2")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild)

lazy val KamanjaInternalDeps = project.in(file("KamanjaInternalDeps")).configs(TestConfigs.all: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", InputOutputAdapterBase, Exceptions, DataDelimiters, Metadata, KamanjaBase, MetadataBootstrap,
  Serialize, ZooKeeperListener, ZooKeeperLeaderLatch, KamanjaUtils, TransactionService, StorageManager, PmmlCompiler, ZooKeeperClient, OutputMsgDef, SecurityAdapterBase, HeartBeat,
  PythonFactoryOfModelInstanceFactory, JpmmlFactoryOfModelInstanceFactory, JarFactoryOfModelInstanceFactory, KamanjaVersion, InstallDriverBase, BaseFunctions, KafkaSimpleInputOutputAdapters, FileSimpleInputOutputAdapters, SimpleEnvContextImpl,
  GenericMsgCompiler, MethodExtractor, JsonDataGen, Controller, AuditAdapters, CustomUdfLib, UtilityService,
  UtilsForModels, MessageCompiler, jtm, Dag, NodeInfoExtract, SmartFileAdapter, Cache, CacheImp, CsvSerDeser, JsonSerDeser, KBinarySerDeser, ContainerDataLoader, EncryptUtils, VelocityMetrics)

////////////////////////

lazy val BaseTypes = project.in(file("BaseTypes")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, Exceptions)

lazy val VelocityMetrics = project.in(file("VelocityMetrics")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase)

lazy val Cache = project.in(file("Utils/Cache")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val CacheImp = project.in(file("Utils/CacheImp")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Cache)

lazy val BaseFunctions = project.in(file("BaseFunctions")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, Exceptions) // has only resolvers , no dependencies

lazy val Serialize = project.in(file("Utils/Serialize")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", Metadata, AuditAdapterBase, Exceptions)

lazy val ZooKeeperClient = project.in(file("Utils/ZooKeeper/CuratorClient")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Serialize, Exceptions)

lazy val ZooKeeperListener = project.in(file("Utils/ZooKeeper/CuratorListener")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", ZooKeeperClient, Serialize, Exceptions)

lazy val KamanjaVersion = project.in(file("KamanjaVersion")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild) //dependsOn(ExtDependencyLibs) ~no external dependencies

lazy val Exceptions = project.in(file("Exceptions")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", KamanjaVersion)

lazy val KamanjaBase = project.in(file("KamanjaBase")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, Exceptions, KamanjaUtils, HeartBeat, KvBase, DataDelimiters, BaseTypes)

lazy val DataDelimiters = project.in(file("DataDelimiters")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val SmartFileAdapter = project.in(file("InputOutputAdapters/SmartFileAdapter")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", InputOutputAdapterBase, Exceptions, DataDelimiters, VelocityMetrics, JsonSerDeser % "test")

lazy val ElasticsearchAdapters = project.in(file("InputOutputAdapters/ElasticsearchAdapters")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", InputOutputAdapterBase, Exceptions, DataDelimiters, JsonSerDeser % "test")

lazy val KamanjaManager = project.in(file("KamanjaManager")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, MetadataBootstrap, MetadataAPI, Serialize, ZooKeeperListener, ZooKeeperLeaderLatch, Exceptions, KamanjaUtils, TransactionService, DataDelimiters, InputOutputAdapterBase, Dag, KamanjaTestUtils, VelocityMetrics)

lazy val MessageCompiler = project.in(file("MessageCompiler")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(Metadata, MetadataBootstrap, Exceptions).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", VelocityMetrics)

lazy val KafkaSimpleInputOutputAdapters = project.in(file("InputOutputAdapters/KafkaSimpleInputOutputAdapters")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", InputOutputAdapterBase, Exceptions, DataDelimiters)
  .settings(
    parallelExecution in Test := false,
    test <<= (test in Test).dependsOn(assembleDependencies)
  )

lazy val InputOutputAdapterBase = project.in(file("InputOutputAdapters/InputOutputAdapterBase")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions, DataDelimiters, HeartBeat, KamanjaBase, ZooKeeperClient, StorageBase, StorageManager, TransactionService, VelocityMetrics)

lazy val FileSimpleInputOutputAdapters = project.in(file("InputOutputAdapters/FileSimpleInputOutputAdapters")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", InputOutputAdapterBase, Exceptions, DataDelimiters, VelocityMetrics)

lazy val SimpleEnvContextImpl = project.in(file("EnvContexts/SimpleEnvContextImpl")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBase, StorageManager, Serialize, Exceptions, Cache, CacheImp)

lazy val StorageBase = project.in(file("Storage/StorageBase")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions, KamanjaUtils, KvBase, KamanjaBase)

lazy val Metadata = project.in(file("Metadata")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(Exceptions, ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val OutputMsgDef = project.in(file("OutputMsgDef")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, BaseTypes)

lazy val MessageDef = project.in(file("MessageDef")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, MetadataBootstrap, Exceptions)

lazy val GenericMsgCompiler = project.in(file("GenericMsgCompiler")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, MetadataBootstrap, Exceptions)

lazy val PmmlRuntime = project.in(file("Pmml/PmmlRuntime")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, Exceptions)

lazy val PmmlCompiler = project.in(file("Pmml/PmmlCompiler")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", PmmlRuntime, PmmlUdfs, Metadata, KamanjaBase, MetadataBootstrap, Exceptions)

lazy val PmmlUdfs = project.in(file("Pmml/PmmlUdfs")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, PmmlRuntime, KamanjaBase, Exceptions)

lazy val MethodExtractor = project.in(file("Pmml/MethodExtractor")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", PmmlUdfs, Metadata, KamanjaBase, Serialize, Exceptions) // added
// no external dependencies

lazy val MetadataAPI = project.in(file("MetadataAPI")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", StorageManager, Metadata, PmmlCompiler, Serialize, ZooKeeperClient, ZooKeeperListener, OutputMsgDef, Exceptions, SecurityAdapterBase, KamanjaUtils, HeartBeat, KamanjaBase, JpmmlFactoryOfModelInstanceFactory, jtm, SimpleApacheShiroAdapter % "test", KamanjaTestUtils % "test")
    .settings(
      parallelExecution in Test := false,
      test <<= (test in Test).dependsOn(assembleDependencies)
    )

//test in MetadataAPI <<= (test in MetadataAPI) dependsOn ((assembly in KamanjaInternalDeps), (assembly in ExtDependencyLibs), (assembly in ExtDependencyLibs2))
//parallelExecution in Test in MetadataAPI := false

lazy val MetadataBootstrap = project.in(file("MetadataBootstrap/Bootstrap")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, BaseTypes, Exceptions)

lazy val MetadataAPIService = project.in(file("MetadataAPIService")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaBase, MetadataAPI, ZooKeeperLeaderLatch, Exceptions)

lazy val MetadataAPIServiceClient = project.in(file("MetadataAPIServiceClient")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Serialize, Exceptions, KamanjaBase)

lazy val ContainersUtility = project.in(file("Utils/ContainersUtility")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, MetadataBootstrap, MetadataAPI, StorageManager, Exceptions, TransactionService)

lazy val JsonChecker = project.in(file("Utils/JsonChecker")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions)

lazy val QueryGenerator = project.in(file("Utils/QueryGenerator")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions, MetadataAPI, Metadata)

lazy val GenerateMessage = project.in(file("Utils/GenerateMessage")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions)

//lazy val InterfacesSamples = project.in(file("SampleApplication/InterfacesSamples")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(Metadata, KamanjaBase, MetadataBootstrap, MetadataAPI, StorageBase, Exceptions)

lazy val SimpleKafkaProducer = project.in(file("Utils/SimpleKafkaProducer")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, KafkaSimpleInputOutputAdapters, KafkaAdapters_v10, KafkaAdapters_v9, KafkaAdapters_v8, Exceptions)

lazy val OutputUtils = project.in(file("Utils/OutputUtils")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(TestSettings.settings: _*).dependsOn(SaveContainerDataComponent, ZooKeeperLeaderLatch % "provided", VelocityMetrics)

lazy val KVInit = project.in(file("Utils/KVInit")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, MetadataBootstrap, MetadataAPI, StorageManager, Exceptions, TransactionService)

lazy val ContainerDataLoader = project.in(file("Utils/ContainerDataLoader")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, Exceptions)

lazy val ZooKeeperLeaderLatch = project.in(file("Utils/ZooKeeper/CuratorLeaderLatch")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", ZooKeeperClient, Exceptions, KamanjaUtils)

lazy val JsonDataGen = project.in(file("Utils/JsonDataGen")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions, KamanjaBase)

lazy val NodeInfoExtract = project.in(file("Utils/NodeInfoExtract")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", MetadataAPI, Exceptions)

lazy val Controller = project.in(file("Utils/Controller")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KafkaAdapters_v8 % "provided", ZooKeeperClient, ZooKeeperListener, KafkaSimpleInputOutputAdapters, Exceptions)

lazy val SimpleApacheShiroAdapter = project.in(file("Utils/Security/SimpleApacheShiroAdapter")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, Exceptions, SecurityAdapterBase)

lazy val EncryptUtils = project.in(file("Utils/EncryptUtils")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val AuditAdapters = project.in(file("Utils/Audit")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", StorageManager, Exceptions, AuditAdapterBase, Serialize)

lazy val CustomUdfLib = project.in(file("SampleApplication/CustomUdfLib")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", PmmlUdfs, Exceptions)
// no external dependencies

lazy val JdbcDataCollector = project.in(file("Utils/JdbcDataCollector")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions)

lazy val ExtractData = project.in(file("Utils/ExtractData")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, MetadataBootstrap, MetadataAPI, StorageManager, Exceptions)

//last commented
//lazy val InterfacesSamples = project.in(file("SampleApplication/InterfacesSamples")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, MetadataBootstrap, MetadataAPI, StorageBase, Exceptions)

lazy val StorageCassandra = project.in(file("Storage/Cassandra")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", StorageBase, Serialize, Exceptions, KamanjaUtils, KvBase)

lazy val StorageH2DB = project.in(file("Storage/H2DB")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", StorageBase, Serialize, Exceptions, KamanjaUtils, KvBase)

lazy val StorageElasticsearch = project.in(file("Storage/Elasticsearch")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", StorageBase, Serialize, Exceptions, KamanjaUtils, KvBase)

lazy val StorageHashMap = project.in(file("Storage/HashMap")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", StorageBase, Serialize, Exceptions, KamanjaUtils, KvBase)

lazy val StorageHBase = project.in(file("Storage/HBase")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", StorageBase, Serialize, Exceptions, KamanjaUtils, KvBase)

lazy val StorageTreeMap = project.in(file("Storage/TreeMap")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", StorageBase, Serialize, Exceptions, KamanjaUtils, KvBase)

lazy val StorageSqlServer = project.in(file("Storage/SqlServer")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", StorageBase, Serialize, Exceptions, KamanjaUtils)

lazy val StorageManager = project.in(file("Storage/StorageManager")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", StorageBase, Exceptions, KamanjaBase, KamanjaUtils, StorageSqlServer, StorageCassandra, StorageHashMap, StorageTreeMap, StorageHBase, StorageH2DB)

lazy val AuditAdapterBase = project.in(file("AuditAdapters/AuditAdapterBase")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions)

lazy val SecurityAdapterBase = project.in(file("SecurityAdapters/SecurityAdapterBase")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions)

lazy val KamanjaUtils = project.in(file("KamanjaUtils")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", Exceptions)

lazy val UtilityService = project.in(file("Utils/UtilitySerivce")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions, KamanjaUtils)

lazy val HeartBeat = project.in(file("HeartBeat")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", ZooKeeperListener, ZooKeeperLeaderLatch, Exceptions)

lazy val TransactionService = project.in(file("TransactionService")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions, KamanjaBase, ZooKeeperClient, StorageBase, StorageManager)

lazy val jtm = project.in(file("GenerateModels/jtm")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, Exceptions, MetadataBootstrap, MessageCompiler, runtime)

lazy val runtime = project.in(file("GenerateModels/Runtime")) dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, Exceptions, MetadataBootstrap, MessageCompiler)

lazy val Dag = project.in(file("Utils/Dag")) dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaUtils, Exceptions)

lazy val JsonSerDeser = project.in(file("Utils/AdapterSerializers/JsonSerDeser")) dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaVersion, KamanjaBase)

lazy val CsvSerDeser = project.in(file("Utils/AdapterSerializers/CsvSerDeser")) dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaVersion, KamanjaBase)

lazy val KBinarySerDeser = project.in(file("Utils/AdapterSerializers/KBinarySerDeser")) dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaVersion, KamanjaBase, BaseTypes)

lazy val KvBase = project.in(file("KvBase")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild)
//no external dependencies

lazy val FileDataConsumer = project.in(file("FileDataConsumer")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Exceptions, MetadataAPI, VelocityMetrics)

lazy val CleanUtil = project.in(file("Utils/CleanUtil")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", MetadataAPI)

lazy val SaveContainerDataComponent = project.in(file("Utils/SaveContainerDataComponent")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, MetadataBootstrap, MetadataAPI, StorageManager, Exceptions, TransactionService)

lazy val UtilsForModels = project.in(file("Utils/UtilsForModels")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val JarFactoryOfModelInstanceFactory = project.in(file("FactoriesOfModelInstanceFactory/JarFactoryOfModelInstanceFactory")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, Exceptions)

lazy val JpmmlFactoryOfModelInstanceFactory = project.in(file("FactoriesOfModelInstanceFactory/JpmmlFactoryOfModelInstanceFactory")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, Exceptions)

lazy val MigrateBase = project.in(file("Utils/Migrate/MigrateBase")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided") // Remove ExtDependencyLibs2 ??

lazy val PythonFactoryOfModelInstanceFactory =  project.in(file("FactoriesOfModelInstanceFactory/PythonFactoryOfModelInstanceFactory")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, Exceptions, JsonSerDeser)

// no external dependencies

lazy val MigrateFrom_V_1_1 = project.in(file("Utils/Migrate/SourceVersion/MigrateFrom_V_1_1")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase)

lazy val MigrateManager = project.in(file("Utils/Migrate/MigrateManager")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase, KamanjaVersion)

lazy val MigrateFrom_V_1_2 = project.in(file("Utils/Migrate/SourceVersion/MigrateFrom_V_1_2")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase)

lazy val MigrateFrom_V_1_3 = project.in(file("Utils/Migrate/SourceVersion/MigrateFrom_V_1_3")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase)

lazy val MigrateFrom_V_1_4 = project.in(file("Utils/Migrate/SourceVersion/MigrateFrom_V_1_4")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase, MetadataAPI)

lazy val MigrateFrom_V_1_4_1 = project.in(file("Utils/Migrate/SourceVersion/MigrateFrom_V_1_4_1")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(MigrateBase, MetadataAPI)

lazy val MigrateFrom_V_1_5 = project.in(file("Utils/Migrate/SourceVersion/MigrateFrom_V_1_5")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(MigrateBase,MetadataAPI)

lazy val MigrateFrom_V_1_6 = project.in(file("Utils/Migrate/SourceVersion/MigrateFrom_V_1_6")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(MigrateBase,MetadataAPI)

lazy val InstallDriver = project.in(file("Utils/ClusterInstaller/InstallDriver")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", InstallDriverBase, Serialize, KamanjaUtils)

lazy val ClusterInstallerDriver = project.in(file("Utils/ClusterInstaller/ClusterInstallerDriver")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(InstallDriverBase, MigrateBase, MigrateManager)

lazy val GetComponent = project.in(file("Utils/ClusterInstaller/GetComponent")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild) //.dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val PmmlTestTool = project.in(file("Utils/PmmlTestTool")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaVersion)

lazy val GenerateAdapterBindings = project.in(file("Utils/Migrate/GenerateAdapterBindings")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided")

lazy val HttpEndpoint = project.in(file("Utils/HttpEndpoint")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild)

lazy val DemoKafkaProducer = project.in(file("Utils/DemoKafkaProducer")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild).dependsOn(KamanjaVersion)

lazy val KamanjaUIREST = project.in(file("KamanjaUI/Rest/KamanjaUIRest")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings(version <<= version in ThisBuild)

lazy val KafkaAdapters_v8 = project.in(file("InputOutputAdapters/KafkaAdapters_v8")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KafkaSimpleInputOutputAdapters % "provided", VelocityMetrics)

lazy val KafkaAdapters_v9 = project.in(file("InputOutputAdapters/KafkaAdapters_v9")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KafkaSimpleInputOutputAdapters % "provided", VelocityMetrics)

lazy val KafkaAdapters_v10 = project.in(file("InputOutputAdapters/KafkaAdapters_v10"))
  .configs(TestConfigs.all: _*)
  .settings(docSettings: _*)
  .settings(TestSettings.settings: _*)
  .dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KafkaSimpleInputOutputAdapters % "provided", VelocityMetrics, KamanjaTestUtils)

lazy val KamanjaAppTester = project.in(file("Utils/KamanjaAppTester"))
  .configs(TestConfigs.all: _*)
  .settings(TestSettings.settings: _*)
  .settings(version <<= version in ThisBuild)
  .settings(
    parallelExecution in Test := false,
    parallelExecution in testOnly := false,
    testOnly <<= (testOnly in Test).dependsOn(assembleDependencies),
    test <<= (test in Test).dependsOn(assembleDependencies)
  )
  .dependsOn(KamanjaManager % "compile->test", MetadataAPI % "compile->test", SimpleKafkaProducer % "compile->test",
    KamanjaTestUtils, KafkaAdapters_v10 % "compile;compile->test",
    KamanjaInternalDeps % "provided", ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided",
    KVInit)

lazy val MigrateTo_V_1_6 = project.in(file("Utils/Migrate/DestinationVersion/MigrateTo_V_1_6")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(MigrateBase, KamanjaManager)

lazy val InstallDriverBase = project.in(file("Utils/ClusterInstaller/InstallDriverBase")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).settings( version <<= version in ThisBuild ).dependsOn(ExtDependencyLibs % "provided")

lazy val SimpleKafkaProducer_v10 = project.in(file("Utils/SimpleKafkaProducer/v10")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, Exceptions)

lazy val SimpleKafkaProducer_v9 = project.in(file("Utils/SimpleKafkaProducer/v9")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, Exceptions)

lazy val SimpleKafkaProducer_v8 = project.in(file("Utils/SimpleKafkaProducer/v8")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", Metadata, KamanjaBase, Exceptions)

lazy val PythonModelPrototype = project.in(file("FactoriesOfModelInstanceFactory/PythonModelPrototype")).configs(TestConfigs.all: _*).settings(docSettings: _*).settings(docSettings: _*).settings(TestSettings.settings: _*).dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaVersion)

// TEST LIBRARIES ONLY TO BE INCLUDED AS PART OF TESTS

lazy val KamanjaTestUtils = project.in(file("Utils/KamanjaTestUtils"))
  .configs(TestConfigs.all: _*)
  .settings(TestSettings.settings: _*)
  .settings(version <<= version in ThisBuild)
  .dependsOn(ExtDependencyLibs % "provided", ExtDependencyLibs2 % "provided", KamanjaVersion)