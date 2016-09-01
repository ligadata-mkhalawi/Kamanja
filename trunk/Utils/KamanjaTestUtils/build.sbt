import sbt.Project

name := "KamanjaTestUtils"

version := "1.0"

shellPrompt := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

//libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.10"

//libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.10"

libraryDependencies += "org.apache.kafka" %% "kafka" % "0.10.0.0" exclude("log4j", "log4j") exclude("org.slf4j","slf4j-log4j12")

