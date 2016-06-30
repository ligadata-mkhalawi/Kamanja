name := "KamanjaUIRest"

version := "1.5.0"

// Enables publishing to maven repo
// publishMavenStyle := true

// Do not append Scala versions to the generated artifacts
crossPaths := false

// This forbids including Scala related libraries into the dependency
autoScalaLibrary := false

libraryDependencies ++= {
  Seq(
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "provided",
	  "com.sun.jersey" % "jersey-server" % "1.8", 
	  "com.orientechnologies" % "orientdb-core" % "2.1.19",
	  "com.orientechnologies" % "orientdb-jdbc" % "2.1.19",
	  "com.googlecode.json-simple" % "json-simple" % "1.1",
	  "javax.ws.rs" % "jsr311-api" % "1.1.1"
  )
}

tomcat() 