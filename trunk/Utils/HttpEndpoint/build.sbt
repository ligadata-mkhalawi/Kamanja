
name := "HttpEndpoint"

//scalaVersion := "2.11.7"

//resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

//resolvers += Resolver.typesafeRepo("releases")

//resolvers += Resolver.sonatypeRepo("releases")

//scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.4.1"
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.4.1"
libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.4.1"
libraryDependencies += "commons-io" % "commons-io" % "2.4"
libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.9"
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.9"
libraryDependencies += "joda-time" % "joda-time" % "2.9.4"

// Kafka
resolvers += "Apache repo" at "https://repository.apache.org/content/repositories/releases"
libraryDependencies ++= Seq("org.apache.kafka" %% "kafka" % "0.9.0.1"
  exclude("javax.jms", "jms")
  exclude("com.sun.jdmk", "jmxtools")
  exclude("com.sun.jmx", "jmxri")
  exclude("org.slf4j", "slf4j-log4j12")
)

// Avro
libraryDependencies += "org.apache.avro" % "avro" % "1.7.7"

// Spray
resolvers ++= Seq(
  "spray repo" at "http://repo.spray.io/"
)
libraryDependencies ++= {
  val sprayVersion = "1.3.3"
  val akkaVersion = "2.3.9"
  Seq(
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    //"io.spray" %% "spray-testkit" % sprayVersion,
    //"io.spray" %% "spray-client" % sprayVersion,
    "io.spray" %% "spray-json" % "1.3.2",
    "com.typesafe.akka" %% "akka-actor" % akkaVersion
    //"com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    //    "ch.qos.logback" % "logback-classic" % "1.0.12",
    //"org.apache.camel" % "camel-core" % "2.9.2"
  )
}

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test->default"

test in assembly := {}

assemblyJarName in assembly := s"${name.value}-${version.value}"
