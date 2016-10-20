import java.io.File

import sbt._
import Keys._
import Tests._

name := "KamanjaAppTester"

shellPrompt := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

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
  sbt.IO.copyFile(new File(s"lib_managed/jars/org.apache.kafka/kafka-clients/kafka-clients-0.10.0.0.jar"), new File(s"Utils/KamanjaAppTester/target/scala-$scalaV/test-classes/kamanjaInstall/lib/system/kafka-clients-0.10.0.0.jar"))
})

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