import sbt.Keys._
import sbt._

shellPrompt := { state =>  "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

name := "EncryptUtils"

coverageMinimum := 80

coverageFailOnMinimum := false
