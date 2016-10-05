// put this at the top of the file
import sbt._
import sbtassembly.AssemblyPlugin.defaultShellScript
import sbt.Keys._

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
 // case x if x endsWith "com/ligadata/kamanja/metadataload/MetadataLoad.class" => MergeStrategy.first
  case x if x endsWith "org/apache/commons/beanutils/BasicDynaBean.class" => MergeStrategy.last
  case x if x endsWith "com\\esotericsoftware\\minlog\\Log.class" => MergeStrategy.first
  case x if x endsWith "com\\esotericsoftware\\minlog\\Log$Logger.class" => MergeStrategy.first
  case x if x endsWith "com/esotericsoftware/minlog/Log.class" => MergeStrategy.first
  case x if x endsWith "com/esotericsoftware/minlog/Log$Logger.class" => MergeStrategy.first
  case x if x endsWith "com\\esotericsoftware\\minlog\\pom.properties" => MergeStrategy.first
  case x if x endsWith "com/esotericsoftware/minlog/pom.properties" => MergeStrategy.first
  case x if x endsWith "com/ligadata/keyvaluestore/DriverShim.class" => MergeStrategy.first
  case x if x endsWith "com/ligadata/keyvaluestore/JdbcClassLoader.class" => MergeStrategy.first
  case x if x contains "com.esotericsoftware.minlog\\minlog\\pom.properties" => MergeStrategy.first
  case x if x contains "com.esotericsoftware.minlog/minlog/pom.properties" => MergeStrategy.first
  case x if x contains "org\\objectweb\\asm\\" => MergeStrategy.last
  case x if x contains "org/objectweb/asm/" => MergeStrategy.last
  case x if x contains "org/apache/commons/collections" => MergeStrategy.last
  case x if x contains "org\\apache\\commons\\collections" => MergeStrategy.last
  case x if x contains "com.fasterxml.jackson.core" => MergeStrategy.first
  case x if x contains "com/fasterxml/jackson/core" => MergeStrategy.first
  case x if x contains "com\\fasterxml\\jackson\\core" => MergeStrategy.first
  case x if x contains "commons-logging" => MergeStrategy.first
  case x if x contains "org/w3c/dom/html" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.first
  case "unwanted.txt" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)

}

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set("commons-beanutils-1.7.0.jar", "google-collections-1.0.jar","commons-collections4-4.0.jar", "log4j-1.2.17.jar", "log4j-1.2.16.jar")
  cp filter { jar => excludes(jar.data.getName) }
}


name := "FileDataConsumer"

//libraryDependencies ++= {
//  val sprayVersion = "1.3.3"
//  val akkaVersion = "2.3.9"
//  Seq(
//    "org.apache.kafka" %% "kafka" % "0.8.2.2",
//    "org.scala-lang" % "scala-actors" % scalaVersion.value
//  )
//}

libraryDependencies ++= {
  val sprayVersion = "1.3.1"
  val akkaVersion = "2.3.2"
  val scalaVersion = "2.10.4"
  Seq(
   // "io.spray" % "spray-can" % sprayVersion,
   // "io.spray" % "spray-routing" % sprayVersion,
   // "io.spray" % "spray-testkit" % sprayVersion,
   // "io.spray" % "spray-client" % sprayVersion,
   // "io.spray" %%  "spray-json" % "1.2.5",
    //"org.apache.kafka" % "kafka_2.10" % "0.8.1.1",
    "org.apache.kafka" %% "kafka" % "0.10.0.0",
   // "org.apache.kafka" % "kafka_2.10" % "0.8.2.0",
  //  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  //  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
   // "ch.qos.logback" % "logback-classic" % "1.0.12",
  //  "org.apache.camel" % "camel-core" % "2.9.2",
    "org.scala-lang" % "scala-actors" % scalaVersion,
    "org.apache.commons" % "commons-lang3" % "3.4",
	  "org.apache.tika" % "tika-core" % "1.11",
	  "org.apache.logging.log4j" % "log4j-core" % "2.5",
	  "org.apache.logging.log4j" % "log4j-api" % "2.5",
	  "jmimemagic" % "jmimemagic" % "0.1.2"
	)
}
coverageMinimum := 80

coverageFailOnMinimum := false
