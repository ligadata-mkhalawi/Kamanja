import sbt.Project

name := "KamanjaTestUtils"

version := "1.0"

shellPrompt := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

//libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.10"

//libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.10"

libraryDependencies += "org.apache.kafka" %% "kafka" % "0.9.0.1" exclude("javax.jms", "jms") exclude("com.sun.jdmk", "jmxtools") exclude("com.sun.jmx", "jmxri")

//libraryDependencies += "org.apache.kafka" %% "kafka" % "0.9.0.1" classifier "test" exclude("javax.jms", "jms") exclude("com.sun.jdmk", "jmxtools") exclude("com.sun.jmx", "jmxri")

libraryDependencies += "com.101tec" % "zkclient" % "0.7" exclude("javax.jms", "jms") exclude("com.sun.jdmk", "jmxtools") exclude("com.sun.jmx", "jmxri")