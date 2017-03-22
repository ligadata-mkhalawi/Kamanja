// Project name (artifact name in Maven)
name := "JDBCAdapters"

// orgnization name (e.g., the package name of the project)
organization := "com.ligadata"

version := "1.0-SNAPSHOT"

// project description
description := "JDBC Adapters for creating data pipelines"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.9" 

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.9"

libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.4.1"

libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.4.1"

libraryDependencies += "org.apache.commons" % "commons-dbcp2" % "2.1.1"
       
libraryDependencies += "org.scala-lang" % "scala-actors" % "2.11.7"

libraryDependencies += "com.googlecode.json-simple" % "json-simple" % "1.1.1"

libraryDependencies += "org.apache.commons" % "commons-csv" % "1.2"

libraryDependencies += "com.thoughtworks.xstream" % "xstream" % "1.4.8"

libraryDependencies += "org.mariadb.jdbc" % "mariadb-java-client" % "1.3.4"

libraryDependencies += "org.projectlombok" % "lombok" % "1.16.6"