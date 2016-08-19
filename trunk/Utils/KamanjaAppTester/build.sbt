import java.io.File

name := "KamanjaAppTester"

version := "1.0"

shellPrompt := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

testOptions in Test += Tests.Setup( () => {
  val scalaMajor = scalaVersion.value.substring(0, scalaVersion.value.lastIndexOf('.'))

  def copy(path:File) {
    val path = new File("../../Kamanja")
    if (path.isDirectory) {
      Option(path.listFiles).map(_.toList).getOrElse(Nil).foreach(f => {
        if (f.isDirectory) {
          copy(f)
        }
        else if(f.getName == s"ExtDependencyLibs_$scalaMajor-1.5.3.jar" || f.getName == s"ExtDependencyLibs2_$scalaMajor-1.5.3.jar" || f.getName == s"KamanjaInternalDeps_$scalaMajor-1.5.3.jar")
          try {
            sbt.IO.copyFile(f, new File("Utils/KamanjaAppTester/src/test/resources/kamanjaInstall/jars/lib/system/" + f.getName))
          }
          catch {
            case e: Exception => throw new Exception("Failed to copy file: " + f, e)
          }
        }
      )
    }
  copy(new File("../../Kamanja"))
}
})