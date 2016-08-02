name := "PmmlUdfs"

scalacOptions += "-deprecation"

net.virtualvoid.sbt.graph.Plugin.graphSettings

//libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4"

coverageMinimum := 80

coverageFailOnMinimum := false

coverageEnabled := false

