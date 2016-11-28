import sbtassembly.AssemblyPlugin.defaultShellScript
import sbt._
import Keys._

shellPrompt := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

assemblyOption in assembly ~= {
  _.copy(prependShellScript = Some(defaultShellScript))
}

assemblyJarName in assembly := {
  s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar"
}

// for some reason the merge strategy for non ligadata classes are not working and thus added those conflicting jars in exclusions
// this may result some run time errors

assemblyMergeStrategy in assembly := {
  // case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  // case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList("META-INF", "maven", "jline", "jline", ps) if ps.startsWith("pom") => MergeStrategy.discard
  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
  case x if x endsWith "google/common/annotations/GwtCompatible.class" => MergeStrategy.first
  case x if x endsWith "google/common/annotations/GwtIncompatible.class" => MergeStrategy.first
  case x if x endsWith "/apache/commons/beanutils/BasicDynaBean.class" => MergeStrategy.first
  case x if x endsWith "com\\ligadata\\kamanja\\metadataload\\MetadataLoad.class" => MergeStrategy.first
  case x if x endsWith "com/ligadata/kamanja/metadataload/MetadataLoad.class" => MergeStrategy.first
  case x if x endsWith "com\\ligadata\\keyvaluestore\\DriverShim.class" => MergeStrategy.first
  case x if x endsWith "com/ligadata/keyvaluestore/DriverShim.class" => MergeStrategy.first
  case x if x endsWith "com\\ligadata\\keyvaluestore\\JdbcClassLoader.class" => MergeStrategy.first
  case x if x endsWith "com/ligadata/keyvaluestore/JdbcClassLoader.class" => MergeStrategy.first
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
  case x if x endsWith "StaticLoggerBinder.class" => MergeStrategy.first
  case x if x endsWith "StaticMDCBinder.class" => MergeStrategy.first
  case x if x endsWith "StaticMarkerBinder.class" => MergeStrategy.first
  case x if x contains "com.fasterxml.jackson.core" => MergeStrategy.first
  case x if x contains "com/fasterxml/jackson/core" => MergeStrategy.first
  case x if x contains "com\\fasterxml\\jackson\\core" => MergeStrategy.first
  case x if x contains "commons-logging" => MergeStrategy.first
  case "log4j.properties" => MergeStrategy.first
  case "unwanted.txt" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)

}

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set("commons-beanutils-1.7.0.jar", "google-collections-1.0.jar", "commons-collections-4-4.0.jar", "scalatest_2.11-2.2.0.jar", "scala-reflect-2.11.0.jar", "akka-actor_2.11-2.3.2.jar", "scala-reflect-2.11.2.jar", "scalatest_2.11-2.2.4.jar", "joda-time-2.9.1-javadoc.jar", "voldemort-0.96.jar", "scala-compiler-2.11.0.jar", "guava-16.0.1.jar", "log4j-1.2.17.jar","log4j-1.2.16.jar","jsp-2.1-6.1.14.jar","stax-api-1.0-2.jar","jersey-server-1.9.jar","jersey-json-1.8.jar","servlet-api-2.5-20081211.jar","servlet-api-2.5.jar","jsp-api-2.1-6.1.14.jar","stax-api-1.0.1.jar","hadoop-core-1.2.1.jar")
  cp filter { jar => excludes(jar.data.getName) }
}

name := "KamanjaEndpointServices"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

shellPrompt := { state =>  "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

//unmanagedJars in Compile += file("sqljdbc42.jar")

/////////////////////// OutputUtils
libraryDependencies += "commons-codec" % "commons-codec" % "1.6"
libraryDependencies += "commons-cli" % "commons-cli" % "1.3"
libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.9.0.1"
libraryDependencies += "com.googlecode.json-simple" % "json-simple" % "1.1"
libraryDependencies += "org.apache.commons" % "commons-dbcp2" % "2.1.1"
//libraryDependencies += "junit" % "junit" % "4.11"
libraryDependencies += "org.apache.avro" % "avro" % "1.7.7"
libraryDependencies += "org.apache.avro" % "avro-mapred" % "1.7.7"
libraryDependencies += "org.apache.hadoop" % "hadoop-hdfs" % "2.7.1"
//libraryDependencies += "org.apache.hadoop" % "hadoop-auth" % "2.7.1"
libraryDependencies += "org.apache.hadoop" % "hadoop-core" % "1.2.1"
libraryDependencies += "javax.json" % "javax.json-api" % "1.0"
//libraryDependencies += "org.glassfish" % "javax.json" % "1.0.4"
libraryDependencies += "org.codemonkey.simplejavamail" % "simple-java-mail" % "2.5.1"
libraryDependencies += "org.antlr" % "ST4" % "4.0.8"
libraryDependencies += "org.projectlombok" % "lombok" % "1.16.6"
//libraryDependencies += "org.codehaus.jackson" % "jackson-mapper-asl" % "1.8.3"
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.4.1"
libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.4.1"

coverageMinimum := 80

coverageFailOnMinimum := false
