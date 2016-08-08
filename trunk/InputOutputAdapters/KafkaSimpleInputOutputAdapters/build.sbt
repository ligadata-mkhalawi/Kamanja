name := "KafkaSimpleInputOutputAdapters"

//
//resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
//
//resolvers += "Apache repo" at "https://repository.apache.org/content/repositories/releases"
//
//libraryDependencies += "org.apache.logging.log4j" % "log4j-1.2-api" % "2.4.1"
//
//libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.4.1"
//
//libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.4.1"
//
//libraryDependencies ++= Seq("org.apache.kafka" %% "kafka" % "0.8.2.2"
//                              exclude("javax.jms", "jms")
//                              exclude("com.sun.jdmk", "jmxtools")
//                              exclude("com.sun.jmx", "jmxri")
//)
//
//libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-actors" % _)
//
//libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.9"
//
//libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.9"

// Adding test libs for testing purposes
//libraryDependencies += "org.apache.kafka" %% "kafka" % "0.9.0.1" % "test" classifier "test"
//libraryDependencies += "org.apache.kafka" % "kafka-clients" % "0.9.0.1" % "test"

coverageMinimum := 80

coverageFailOnMinimum := false
