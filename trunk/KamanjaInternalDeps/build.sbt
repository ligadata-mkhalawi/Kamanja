import sbt.Keys._
import sbt._


name := "KamanjaInternalDeps"

assemblyJarName in assembly := {
  s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar"
}

assemblyMergeStrategy in assembly := {
  case x if x contains "com/ligadata/keyvaluestore/DriverShim.class" => MergeStrategy.first
  case x if x contains "com/ligadata/keyvaluestore/JdbcClassLoader.class" => MergeStrategy.first
  case "shiro.ini" => MergeStrategy.first
  case "log4j2.xml" => MergeStrategy.discard
  case "logback.xml" => MergeStrategy.discard
  case x if x contains "Function1.class" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)

}

val excludesSet = Set("commons-beanutils-1.7.0.jar", "google-collections-1.0.jar", "commons-collections4-4.0.jar", "log4j-1.2.17.jar", "log4j-1.2.16.jar", "commons-collections-4-4.0.jar", "scalatest_2.11-2.2.0.jar"
  , "scala-reflect-2.11.0.jar", "akka-actor_2.11-2.3.2.jar", "scala-reflect-2.11.2.jar", "scalatest_2.11-2.2.4.jar", "joda-time-2.9.1-javadoc.jar", "voldemort-0.96.jar", "scala-compiler-2.11.0.jar", "guava-16.0.1.jar"
  , "guava-14.0.1.jar", "guava-r03.jar", "minlog-1.2.jar", "elasticsearch-2.3.5.jar", "shield-2.3.5.jar")

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  cp filter { jar => (excludesSet(jar.data.getName)  || (jar.data.getName.startsWith("KamanjaInternalDeps_2.10-") || jar.data.getName.startsWith("KamanjaInternalDeps_2.11-") || jar.data.getName.startsWith("elasticsearchinputoutputadapters_2.10-") || jar.data.getName.startsWith("elasticsearchinputoutputadapters_2.11-")))  }
}

unmanagedBase <<= baseDirectory { base => base / "custom_lib" }

unmanagedJars in Compile <<= baseDirectory map { base => (base ** "*.jar").classpath }

excludeFilter in unmanagedJars := s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar"
