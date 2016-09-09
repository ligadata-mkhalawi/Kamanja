import sbtassembly.AssemblyPlugin.defaultShellScript
import sbt._
import Keys._

shellPrompt := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

//crossScalaVersions := Seq("2.11.7", "2.10.4")

assemblyOption in assembly ~= {
  _.copy(prependShellScript = Some(defaultShellScript))
}

val kamanjaVersion = "1.5.1"

assemblyJarName in assembly := {
  s"${name.value}_${scalaBinaryVersion.value}-${kamanjaVersion}.jar"
}


name := "KamanjaKafkaAdapters_0_9"

version := "1.6.0"


libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.9.0.1"

//libraryDependencies += "org.scala-lang" %% "scala-library" % scalaVersion.value

//libraryDependencies += "org.scala-lang" %% "scala-actors" % scalaVersion.value

//libraryDependencies += "org.apache.logging.log4j" % "log4j-1.2-api" % "2.4.1"

//libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.4.1"

//libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.4.1"

//libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.9"

//libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.9"

coverageMinimum := 80

coverageFailOnMinimum := false