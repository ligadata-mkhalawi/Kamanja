import java.io.File

name := "KamanjaAppTester"

shellPrompt := { state => "sbt (%s)> ".format(Project.extract(state).currentProject.id) }

testOptions in Test += Tests.Setup( () => {

  def copy(path: File): Unit = {
    if(path.isDirectory){
      Option(path.listFiles).map(_.toList).getOrElse(Nil).foreach(f => {
        if (f.isDirectory)
          copy(f)
        else if (f.getPath.endsWith(".jar")) {
          try {
            sbt.IO.copyFile(f, new File(s"Utils/KamanjaAppTester/src/test/resources/kamanjaInstall/lib/system/${f.getName}"))
          }
          catch {
            case e: Exception => throw new Exception("Failed to copy file: " + f, e)
          }
        }
      })
    }
  }

  copy(new File("."))
})