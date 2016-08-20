package com.ligadata.test.application

import com.ligadata.MetadataAPI.test._
import com.ligadata.test.application.metadata._
import com.ligadata.test.application.metadata.interfaces.ModelElement
import com.ligadata.test.utils.Globals

case class TestExecutorException(message: String, cause: Throwable = null) extends Exception(message, cause)

object TestExecutor {

  private val mdMan: MetadataManager = new MetadataManager

  private type OptionMap = Map[Symbol, Any]

  private def addApplicationMetadata(kamanjaApp: KamanjaApplication): Unit = {
    kamanjaApp.metadataElements.foreach(element => {
      element match {
        case e: MessageElement => mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant))
        case e: ContainerElement => mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant))
        case e: JavaModelElement => mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType), Some(e.modelCfg))
        case e: ScalaModelElement => mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType), Some(e.modelCfg))
        case e: KPmmlModelElement => mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType))
        case e: PmmlModelElement => mdMan.add(e.elementType, e.filename, Some(Globals.kamanjaTestTenant), Some(e.modelType), None, Some("0.0.1"), Some(e.msgConsumed), None, e.msgProduced)
        case e: AdapterMessageBindingElement => mdMan.addBindings(e.filename)
        case e: ModelConfigurationElement => mdMan.add(e.elementType, e.filename)
        case _ => throw new TestExecutorException("[Kamanja Application Tester] - ***ERROR*** Unknown element type: '" + element.elementType)
      }
    })
  }

  //TODO: Need to determine if we want the user to specify the kamanja install directory.
  def main(args: Array[String]): Unit = {
    if (args.length == 0) {
      println("[Kamanja Application Tester] - ***ERROR*** Kamanja installation directory must be specified. --kamanja-dir /path/to/Kamanja/install/directory")
      return
    } else {
      val options = nextOption(Map(), args.toList)
      if(options == null) {
        return
      }
      val installDir: String = options('kamanjadir).asInstanceOf[String]
      val appManager = new KamanjaApplicationManager(installDir + "/test")
      appManager.kamanjaApplications.foreach(app => {
        println(app.name)
      })
    }
  }

  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    def isSwitch(s: String) = (s(0) == '-')
    list match {
      case Nil => map
      case "--kamanja-dir" :: value :: tail =>
        nextOption(map ++ Map('kamanjadir -> value), tail)
      case option :: tail => {
        println("[Kamanja Application Tester] - ***ERROR*** Unknown option " + option)
        return null
      }
    }
  }
}
