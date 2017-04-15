import java.io.File

import sbt._
import Keys._
import Tests._
import sbtassembly.AssemblyPlugin.defaultShellScript
import sbtassembly.AssemblyPlugin.autoImport.{assembly => _, assemblyOption => _, jarName => _, _}

name := "KamanjaAppTester"

shellPrompt := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

assemblyOption in assembly ~= {
  _.copy(prependShellScript = Some(defaultShellScript))
}

test in assembly := {}

assemblyJarName := s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar"

excludeFilter in unmanagedJars := s"${name.value}_${scalaBinaryVersion.value}-${version.value}.jar"

excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
  val excludes = Set("commons-beanutils-1.7.0.jar", "google-collections-1.0.jar", "commons-collections4-4.0.jar", "log4j-1.2.17.jar", "commons-beanutils-1.8.3.jar", "log4j-1.2.16.jar")
  cp filter { jar => excludes(jar.data.getName) }
}

// Test Configuration

// Copying jars into the test-classes resource directory for proper classpath location of embedded services during test.
testOptions in Test += Tests.Setup( () => {
  val scalaV = scalaVersion.value.substring(0, scalaVersion.value.lastIndexOf('.'))

  def copy(path: File): Unit = {
    if(path.isDirectory){
      Option(path.listFiles).map(_.toList).getOrElse(Nil).foreach(f => {
        if (f.isDirectory && f.getName != "lib_managed" && f.getName != "streams" && f.getName != "test-classes")
          copy(f)
        else if (f.getPath.endsWith(".jar")) {
          try {
            //println("Copying File: " + f)
            //sbt.IO.copyFile(f, new File(s"Utils/KamanjaAppTester/src/test/resources/kamanjaInstall/lib/system/${f.getName}"))
            sbt.IO.copyFile(f, new File(s"Utils/KamanjaAppTester/target/scala-$scalaV/test-classes/kamanjaInstall/lib/system/${f.getName}"))
          }
          catch {
            case e: Exception => throw new Exception("Failed to copy file: " + f, e)
          }
        }
      })
    }
  }

  copy(new File("."))
  sbt.IO.copyFile(new File(s"lib_managed/jars/org.apache.kafka/kafka-clients/kafka-clients-0.9.0.1.jar"), new File(s"Utils/KamanjaAppTester/target/scala-$scalaV/test-classes/kamanjaInstall/lib/system/kafka-clients-0.9.0.1.jar"))
  sbt.IO.write(new File(s"Utils/KamanjaAppTester/target/scala-$scalaV/test-classes/kamanjaInstall/config/library_list"), s"ExtDependencyLibs_$scalaV-${version.value}.jar\nKamanjaInternalDeps_$scalaV-${version.value}.jar\nExtDependencyLibs2_$scalaV-${version.value}.jar")
})

assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter { x => x.data.getName == "kafka-clients-0.10.0.1.jar" || x.data.getName == "log4j-1.2.17.jar" }
}

assemblyMergeStrategy in assembly := {
  case PathList("javax", "servlet", xs@_*) => MergeStrategy.first
  // case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList("META-INF", "maven", "jline", "jline", ps) if ps.startsWith("pom") => MergeStrategy.discard
  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
  case x if x endsWith "google/common/annotations/GwtCompatible.class" => MergeStrategy.first
  case x if x endsWith "google/common/annotations/GwtIncompatible.class" => MergeStrategy.first
  case x if x endsWith "/apache/commons/beanutils/BasicDynaBean.class" => MergeStrategy.first
  //added from : JdbcDataCollector
  case x if x endsWith "com\\ligadata\\olep\\metadataload\\MetadataLoad.class" => MergeStrategy.first
  case x if x endsWith "com/ligadata/olep/metadataload/MetadataLoad.class" => MergeStrategy.first
  //
  case x if x endsWith "com/ligadata/keyvaluestore/JdbcClassLoader.class" => MergeStrategy.first
  case x if x endsWith "com/ligadata/keyvaluestore/DriverShim.class" => MergeStrategy.first
  case x if x endsWith "com\\ligadata\\kamanja\\metadataload\\MetadataLoad.class" => MergeStrategy.first
  case x if x endsWith "com/ligadata/kamanja/metadataload/MetadataLoad.class" => MergeStrategy.first
  case x if x endsWith "org/apache/commons/beanutils/BasicDynaBean.class" => MergeStrategy.last
  case x if x endsWith "com\\esotericsoftware\\minlog\\Log.class" => MergeStrategy.first
  case x if x endsWith "com\\esotericsoftware\\minlog\\Log$Logger.class" => MergeStrategy.first
  case x if x endsWith "com/esotericsoftware/minlog/Log.class" => MergeStrategy.first
  case x if x endsWith "com/esotericsoftware/minlog/Log$Logger.class" => MergeStrategy.first
  case x if x endsWith "com\\esotericsoftware\\minlog\\pom.properties" => MergeStrategy.first
  case x if x endsWith "com/esotericsoftware/minlog/pom.properties" => MergeStrategy.first
  case x if x contains "com.esotericsoftware.minlog\\minlog\\pom.properties" => MergeStrategy.first
  case x if x contains "com.esotericsoftware.minlog/minlog/pom.properties" => MergeStrategy.first
  case x if x contains "org\\objectweb\\asm\\" => MergeStrategy.last
  case x if x contains "org/objectweb/asm/" => MergeStrategy.last
  case x if x contains "org/apache/commons/collections" => MergeStrategy.last
  case x if x contains "org\\apache\\commons\\collections" => MergeStrategy.last
  case x if x contains "com.fasterxml.jackson.core" => MergeStrategy.first
  case x if x contains "com/fasterxml/jackson/core" => MergeStrategy.first
  case x if x contains "com\\fasterxml\\jackson\\core" => MergeStrategy.first
  // newly added
  case x if x contains "StaticLoggerBinder.class" => MergeStrategy.first
  case x if x contains "StaticMDCBinder.class" => MergeStrategy.first
  case x if x contains "StaticMarkerBinder.class" => MergeStrategy.first
  case x if x contains "package-info.class" => MergeStrategy.first
  case x if x contains "HTMLDOMImplementation.class" => MergeStrategy.first
  //
  case x if x contains "commons-logging" => MergeStrategy.first
  case "log4j.properties" => MergeStrategy.discard
  case "log4j2.xml" => MergeStrategy.first
  case "logback.xml" => MergeStrategy.first
  case "shiro.ini" => MergeStrategy.first
  case "unwanted.txt" => MergeStrategy.discard
  case "DEPENDENCIES.txt" => MergeStrategy.discard
  case "META-INF/DEPENDENCIES.txt" => MergeStrategy.discard
  case "blueprint.xml" => MergeStrategy.discard
  case "OSGI-INF/blueprint/blueprint.xml" => MergeStrategy.discard
  case "features.xml" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}


// This allows "provided" libraries to be included in compile/run commands when executed through sbt.
run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))

// Forking each test suite into a new JVM
def singleTests(tests: Seq[TestDefinition]): Seq[Group] =
  tests map { test =>
    new Group(name = test.name,
      tests = Seq(test),
      runPolicy = SubProcess(javaOptions = Seq.empty[String]))
  }

testGrouping in Test <<= definedTests in Test map singleTests