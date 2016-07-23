import sbtassembly.AssemblyPlugin.defaultShellScript
import sbt._
import Keys._

shellPrompt := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

crossScalaVersions := Seq("2.11.7", "2.10.4")

assemblyOption in assembly ~= {
  _.copy(prependShellScript = Some(defaultShellScript))
}

val kamanjaVersion = "1.5.1"

assemblyJarName in assembly := {
  s"${name.value}_${scalaBinaryVersion.value}-${kamanjaVersion}.jar"
}



name := "KafkaVersionValidator"


version := "1.5.1"


libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.10.0.0"

libraryDependencies += "org.scala-lang" % "scala-library" % "2.11.7"

libraryDependencies += "org.scala-lang" % "scala-actors" % "2.11.7"

coverageMinimum := 80

coverageFailOnMinimum := false
