import sbtassembly.AssemblyPlugin._

name := "ExtDependencyLibs2"

shellPrompt := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

assemblyOption in assembly ~= {
  _.copy(prependShellScript = Some(defaultShellScript))
}

assemblyJarName in assembly := {
  s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar"
}

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
  // case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList("META-INF", "maven", "jline", "jline", ps) if ps.startsWith("pom") => MergeStrategy.discard
  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
  case x if x endsWith ".class" => MergeStrategy.last
  case x if x endsWith "google/common/annotations/GwtCompatible.class" => MergeStrategy.first
  case x if x endsWith "google/common/annotations/GwtIncompatible.class" => MergeStrategy.first
  case x if x endsWith "/apache/commons/beanutils/BasicDynaBean.class" => MergeStrategy.first
  //added from : JdbcDataCollector
  case x if x endsWith "com\\ligadata\\olep\\metadataload\\MetadataLoad.class" => MergeStrategy.first
  case x if x endsWith "com/ligadata/olep/metadataload/MetadataLoad.class" => MergeStrategy.first
  //
  case x if x endsWith "com\\ligadata\\kamanja\\metadataload\\MetadataLoad.class" => MergeStrategy.first
  case x if x endsWith "com/ligadata/kamanja/metadataload/MetadataLoad.class" => MergeStrategy.first
  case x if x endsWith "org/apache/commons/beanutils/BasicDynaBean.class" => MergeStrategy.last
  case x if x endsWith "com\\esotericsoftware\\minlog\\Log.class" => MergeStrategy.first
  case x if x endsWith "com\\esotericsoftware\\minlog\\Log$Logger.class" => MergeStrategy.first
  case x if x endsWith "com/esotericsoftware/minlog/Log.class" => MergeStrategy.first
  case x if x endsWith "com/esotericsoftware/minlog/Log$Logger.class" => MergeStrategy.first
  case x if x endsWith "com\\esotericsoftware\\minlog\\pom.properties" => MergeStrategy.first
  case x if x endsWith "com/esotericsoftware/minlog/pom.properties" => MergeStrategy.first
  case x if x contains "com.esotericsoftware.minlog\\minlog\\pom.properties" => MergeStrategy.first
  case x if x contains "com.esotericsoftware.minlog/minlog/pom.properties" => MergeStrategy.first
  case x if x contains "org\\objectweb\\asm\\" => MergeStrategy.last
  case x if x contains "org/objectweb/asm/" => MergeStrategy.last
  case x if x contains "org/apache/commons/collections" => MergeStrategy.last
  case x if x contains "org\\apache\\commons\\collections" => MergeStrategy.last
  case x if x contains "com.fasterxml.jackson.core" => MergeStrategy.first
  case x if x contains "com/fasterxml/jackson/core" => MergeStrategy.first
  // newly added
  case x if x contains "StaticLoggerBinder.class" => MergeStrategy.first
  case x if x contains "StaticMDCBinder.class" => MergeStrategy.first
  case x if x contains "StaticMarkerBinder.class" => MergeStrategy.first
  case x if x contains "package-info.class" => MergeStrategy.first
  case x if x contains "HTMLDOMImplementation.class" => MergeStrategy.first
  ////  
  case x if x contains "io/netty" => MergeStrategy.first
  case x if x contains "org/apache/commons/logging" => MergeStrategy.first
  case x if x contains "io.netty.versions.properties" => MergeStrategy.first
  //
  case x if x contains "com\\fasterxml\\jackson\\core" => MergeStrategy.first
  case x if x contains "commons-logging" => MergeStrategy.first
  case "log4j.properties" => MergeStrategy.first
  case "ScalaTestBundle.properties" => MergeStrategy.first
  case "unwanted.txt" => MergeStrategy.discard
  case "DEPENDENCIES.txt" => MergeStrategy.discard
  case "META-INF/DEPENDENCIES.txt" => MergeStrategy.discard
  case "blueprint.xml" => MergeStrategy.discard
  case "OSGI-INF/blueprint/blueprint.xml" => MergeStrategy.discard
  case "features.xml" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)

}

excludeFilter in unmanagedJars := s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar"

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set("commons-beanutils-1.7.0.jar", "google-collections-1.0.jar", "commons-collections4-4.0.jar", "log4j-1.2.17.jar", "commons-beanutils-1.8.3.jar", "log4j-1.2.16.jar", "log4j-over-slf4j-1.7.7.jar")
  cp filter { jar => excludes(jar.data.getName) }
}

/////////////////////////////////// from /trunk/build.sbt
libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    // if scala 2.11+ is used, quasiquotes are merged into scala-reflect
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ Seq("org.scalameta" %% "scalameta" % "0.0.3")
    // libraryDependencies.value
    // in Scala 2.10, quasiquotes are provided by macro paradise
    case Some((2, 10)) =>
      libraryDependencies.value ++ Seq("org.scalamacros" %% "quasiquotes" % "2.1.0")
    //libraryDependencies.value ++ Seq(
    //compilerPlugin("org.scalamacros" % "paradise" % "2.0.0" cross CrossVersion.full),
    //"org.scalamacros" %% "quasiquotes" % "2.1.0" cross CrossVersion.binary)
  }
}


/////////////////////// GetComponent
libraryDependencies += "org.apache.hbase" % "hbase-client" % "1.0.2"
libraryDependencies += "org.apache.hbase" % "hbase-common" % "1.0.2"
libraryDependencies += "org.apache.hadoop" % "hadoop-common" % "2.7.1"
libraryDependencies += "com.googlecode.json-simple" % "json-simple" % "1.1"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.3"
libraryDependencies += "log4j" % "log4j" % "1.2.17"


////////////////////// ZooKeeperListener
libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-collections4" % "4.0",
  "commons-configuration" % "commons-configuration" % "1.7",
  "commons-logging" % "commons-logging" % "1.1.1",
  "org.apache.curator" % "curator-client" % "2.6.0",
  "org.apache.curator" % "curator-framework" % "2.6.0",
  "org.apache.curator" % "curator-recipes" % "2.6.0"
)



////////////////////// CleanUtil
libraryDependencies ++= Seq(
  "com.101tec" % "zkclient" % "0.6",
  "org.apache.curator" % "curator-test" % "2.8.0"
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri")
)



////////////////////// OutputMsgDef
libraryDependencies ++= Seq("junit" % "junit" % "4.8.1" % "test")



////////////////////// MetadataAPIServiceClient
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")
libraryDependencies ++= Seq(
  //    "org.slf4j" % "slf4j-api" % "1.7.10",
  "ch.qos.logback" % "logback-core" % "1.0.13",
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "uk.co.bigbeeconsultants" %% "bee-client" % "0.28.0",
  "org.apache.httpcomponents" % "httpclient" % "4.1.2"
)
resolvers += "Big Bee Consultants" at "http://repo.bigbeeconsultants.co.uk/"

////////////////////// H2DB / StorageH2DB
// http://mvnrepository.com/artifact/com.h2database/h2
libraryDependencies += "com.h2database" % "h2" % "1.4.192"
libraryDependencies += "commons-dbcp" % "commons-dbcp" % "1.4"


///////////////////// New Cassandra
libraryDependencies += "com.datastax.cassandra" % "cassandra-driver-parent" % "3.0.1" pomOnly()
libraryDependencies += "com.datastax.cassandra" % "cassandra-driver-core" % "3.0.1"
libraryDependencies += "org.apache.cassandra" % "cassandra-thrift" % "3.5"
libraryDependencies += "org.apache.thrift" % "libthrift" % "0.9.3" pomOnly()


////////////////////// HashMap
libraryDependencies += "org.mapdb" % "mapdb" % "1.0.6"


////////////////////// SqlServer
libraryDependencies += "org.apache.commons" % "commons-dbcp2" % "2.1.1" // one


//////////////////////  SmartFileAdapter
libraryDependencies ++= {
  val sprayVersion = "1.3.3"
  val akkaVersion = "2.3.9"
  Seq(
    //"org.apache.kafka" %% "kafka" % "0.8.2.2",
    //"org.scala-lang" % "scala-actors" % scalaVersion,
    "org.apache.commons" % "commons-lang3" % "3.4",
    "org.apache.tika" % "tika-core" % "1.11",
    "jmimemagic" % "jmimemagic" % "0.1.2"
  )
}
libraryDependencies += "org.apache.hadoop" % "hadoop-client" % "2.7.1"
libraryDependencies += "org.apache.hadoop" % "hadoop-hdfs" % "2.7.1"
libraryDependencies += "org.apache.commons" % "commons-vfs2" % "2.0"
libraryDependencies += "org.apache.commons" % "commons-compress" % "1.5"
libraryDependencies += "org.anarres.lzo" % "lzo-core" % "1.0.0"
libraryDependencies += "com.jcraft" % "jsch" % "0.1.53"

//////////////////////  Cache & CacheImp
//libraryDependencies += "org.infinispan" % "infinispan-core" % "7.2.5.Final"
//libraryDependencies += "org.infinispan" % "infinispan-tree" % "7.2.5.Final"
//libraryDependencies += "net.jcip" % "jcip-annotations" % "1.0"
libraryDependencies += "net.sf.ehcache" % "ehcache-core" % "2.6.5"
libraryDependencies += "net.sf.ehcache" % "ehcache-jgroupsreplication" % "1.7"
libraryDependencies += "org.jgroups" % "jgroups" % "3.6.7.Final"
// libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.9"
// libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.9"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.12"
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.12"
libraryDependencies += "org.slf4j" % "slf4j-nop" % "1.7.12"
// libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.0"
