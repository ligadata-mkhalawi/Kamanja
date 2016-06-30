name := "KamanjaUIRest"

version := "1.0"

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