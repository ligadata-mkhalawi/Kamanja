import sbt.Project

name := "KamanjaTestUtils"

version := "1.0"

shellPrompt := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.10"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.10"
