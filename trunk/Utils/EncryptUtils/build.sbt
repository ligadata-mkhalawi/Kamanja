import sbt.Keys._
import sbt._

shellPrompt := { state =>  "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

name := "EncryptUtils"

libraryDependencies += "commons-codec" % "commons-codec" % "1.6"

libraryDependencies += "commons-cli" % "commons-cli" % "1.3"

coverageMinimum := 80

coverageFailOnMinimum := false
