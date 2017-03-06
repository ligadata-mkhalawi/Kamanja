package com.ligadata.kamanja.test.application

import java.io.File

import com.ligadata.MetadataAPI.{APIResultInfo, ApiResult, MetadataAPIImpl}
import com.ligadata.kamanja.test.application.logging.{KamanjaAppLogger, KamanjaAppLoggerException}
import com.ligadata.kamanja.test.application.configuration._
import com.ligadata.kamanja.test.application.metadata._
import com.ligadata.kamanja.test.application.metadata.interfaces.MetadataElement
import com.ligadata.tools.kvinit.KVInit

import scala.collection.mutable.ListBuffer
import org.json4s._
import org.json4s.native.JsonMethods._

class KamanjaApplicationManager(baseDir: String) {

  lazy val kamanjaApplications = initializeApplications(baseDir)

  private var logger: KamanjaAppLogger = {
    try {
      KamanjaAppLogger.getKamanjaAppLogger
    }
    catch {
      case e: KamanjaAppLoggerException => throw new Exception("Kamanja App Logger has not been created. Please call createKamanjaAppLogger first.")
    }
  }

  def addApplicationMetadata(kamanjaApp: KamanjaApplication): Boolean = {

    def setMetadataElementName(element: MetadataElement, apiResult: ApiResult): Unit = {
      val mdNameArray = apiResult.description.split(":")(1).split('.')
      val arrayLen = mdNameArray.length
      element.version = mdNameArray(mdNameArray.length - 1)
      element.name = mdNameArray(mdNameArray.length - 2)
      element.namespace = mdNameArray.reverse.dropWhile(_ == element.version).dropWhile(_ == element.name).reverse.mkString(".")
    }

    var statusCode: Int = -10
    kamanjaApp.metadataElements.foreach(element => {
      var apiResult: ApiResult = null
      try {
        element match {
          case e: MessageElement => {
            logger.info(s"Adding message from file '${e.filename}'")
            apiResult = KamanjaEnvironmentManager.mdMan.add(e.elementType, e.filename, Some(KamanjaEnvironmentManager.getAllTenants(0).tenantId))
            setMetadataElementName(element, apiResult)
          }
          case e: ContainerElement => {
            logger.info(s"Adding container from file '${e.filename}'")
            apiResult = KamanjaEnvironmentManager.mdMan.add(e.elementType, e.filename, Some(KamanjaEnvironmentManager.getAllTenants(0).tenantId))
            setMetadataElementName(element, apiResult)
            e.kvFilename match {
              case Some(file) =>
                logger.info(s"Key-Value filename associated with container ${e.namespace}.${e.name} found. Adding data from $file")
                if (KVInit.run(Array("--typename", s"${e.namespace}.${e.name}",
                  "--config", KamanjaEnvironmentManager.metadataConfigFile,
                  "--datafiles", file,
                  "--ignorerecords", "1",
                  "--deserializer", "com.ligadata.kamanja.serializer.csvserdeser",
                  "--optionsjson",
                  """{"alwaysQuoteFields": false, "fieldDelimiter":",", "valueDelimiter":"~"}"""
                )) != 0)
                  throw new TestExecutorException(s"***ERROR*** Failed to upload data from Key-Value file")
                else {
                  logger.info(s"Successfully added Key-Value data")
                  // KVInit calls MetadataAPIImpl.CloseDB after it loads container data.
                  // Therefore, we need to call OpenDBStore in order to reopen it to avoid a nullpointer expceiont on future MetadataAPI calls.
                  MetadataAPIImpl.OpenDbStore(MetadataAPIImpl.GetMetadataAPIConfig.getProperty("JAR_PATHS").split(",").toSet, MetadataAPIImpl.GetMetadataAPIConfig.getProperty("METADATA_DATASTORE"))
                }
              case None =>
            }
          }
          case e: JavaModelElement => {
            logger.info(s"Adding java model from file '${e.filename}' with model configuration '${e.modelCfg}'")
            apiResult = KamanjaEnvironmentManager.mdMan.add(e.elementType, e.filename, Some(KamanjaEnvironmentManager.getAllTenants(0).tenantId), Some(e.modelType), Some(e.modelCfg))
            setMetadataElementName(element, apiResult)
          }
          case e: ScalaModelElement => {
            logger.info(s"Adding scala model from file '${e.filename}' with model configuration '${e.modelCfg}'")
            apiResult = KamanjaEnvironmentManager.mdMan.add(e.elementType, e.filename, Some(KamanjaEnvironmentManager.getAllTenants(0).tenantId), Some(e.modelType), Some(e.modelCfg))
            setMetadataElementName(element, apiResult)
          }
          case e: KPmmlModelElement => {
            logger.info(s"Adding KPMML model from file '${e.filename}'")
            apiResult = KamanjaEnvironmentManager.mdMan.add(e.elementType, e.filename, Some(KamanjaEnvironmentManager.getAllTenants(0).tenantId), Some(e.modelType))
            setMetadataElementName(element, apiResult)
          }
          case e: PmmlModelElement => {
            logger.info(s"Adding PMML model from file '${e.filename}' with message consumed '${e.msgConsumed}'")
            apiResult = KamanjaEnvironmentManager.mdMan.add(e.elementType, e.filename, Some(KamanjaEnvironmentManager.getAllTenants(0).tenantId), Some(e.modelType), None, Some(e.modelName), Some("0.0.1"), Some(e.msgConsumed), None, e.msgProduced)
          }
          case e: PythonModelElement => {
            logger.info(s"Adding PYTHON model from file '${e.filename}' with message consumed '${e.msgConsumed}'")
            apiResult = KamanjaEnvironmentManager.mdMan.add(e.elementType, e.filename, Some(KamanjaEnvironmentManager.getAllTenants(0).tenantId), Some(e.modelType), None, Some(e.modelName), Some("0.0.1"), Some(e.msgConsumed), None, e.msgProduced,Some(e.modelOptions))
          }
          case e: AdapterMessageBindingElement => {
            logger.info(s"Adding adapter message bindings from file '${e.filename}'")
            statusCode = KamanjaEnvironmentManager.mdMan.addBindings(e.filename)
          }
          case e: ModelConfigurationElement => {
            logger.info(s"Adding model configuration from file '${e.filename}'")
            apiResult = KamanjaEnvironmentManager.mdMan.add(e.elementType, e.filename)
          }
          case _ => throw new TestExecutorException("***ERROR*** Unknown element type: '" + element.elementType)
        }
      }
      catch {
        case e: com.ligadata.MetadataAPI.test.MetadataManagerException =>
          logger.error(s"Failed to add '${element.elementType}' from file '${element.filename}' with result '$apiResult' and exception:\n$e")
          return false
        case e: Exception =>
          logger.error(s"***ERROR*** Failed to add '${element.elementType}' from file '${element.filename}' with result '$apiResult' and exception:\n$e")
          return false
      }

      if (element.isInstanceOf[AdapterMessageBindingElement]) {
        if (statusCode != 0) {
          logger.error(s"***ERROR*** Failed to add '${element.elementType}' from file '${element.filename}' with result '$statusCode'")
          return false
        }
        else
          logger.info(s"'${element.elementType}' successfully added")
      }
      else {
        if (apiResult.statusCode != 0) {
          logger.error(s"***ERROR*** Failed to add '${element.elementType}' from file '${element.filename}' with result '$apiResult'")
          return false
        }
        else
          logger.info(s"'${element.elementType}' successfully added")
      }
    })
    true
  }

  def removeApplicationMetadata(kamanjaApp: KamanjaApplication): Boolean = {
      kamanjaApp.metadataElements.foreach(element => {
        var apiResult: ApiResult = null
        try {
          element match {
            case e: MessageElement => apiResult = KamanjaEnvironmentManager.mdMan.remove(element.elementType, element.namespace, element.name, element.version)
            case e: ContainerElement => apiResult = KamanjaEnvironmentManager.mdMan.remove(element.elementType, element.namespace, element.name, element.version)
            case e: ScalaModelElement => apiResult = KamanjaEnvironmentManager.mdMan.remove(element.elementType, element.namespace, element.name, element.version)
            case e: JavaModelElement => apiResult = KamanjaEnvironmentManager.mdMan.remove(element.elementType, element.namespace, element.name, element.version)
            case e: KPmmlModelElement => apiResult = KamanjaEnvironmentManager.mdMan.remove(element.elementType, element.namespace, element.name, element.version)
            case e: PmmlModelElement => apiResult = KamanjaEnvironmentManager.mdMan.remove(element.elementType, element.namespace, element.name, element.version)
            case e: ModelConfigurationElement =>
            case e: AdapterMessageBindingElement =>
          }
        }
        catch {
          case e: com.ligadata.MetadataAPI.test.MetadataManagerException =>
            logger.error(s"***ERROR*** Failed to remove '${element.elementType}' from file '${element.filename}' with result '$apiResult' and exception:\n$e")
            return false
          case e: Exception =>
            logger.error(s"***ERROR*** Failed to remove '${element.elementType}' from file '${element.filename}' with result '$apiResult' and exception:\n$e")
            return false
        }
        if(apiResult != null) {
          if (apiResult.statusCode != 0) {
            logger.error(s"***ERROR*** Failed to remove '${element.elementType}' from file '${element.filename}' with result '$apiResult'")
            return false
          }
          else
            logger.info(s"${element.elementType} '${element.namespace}.${element.name}.${element.version}' successfully removed")
        }
      })
    return true
  }

  /** Given a directory, this will return a list of directories contained with the baseDir/tests
    *
    * baseDir should be Kamanja's installation directory. A basic directory scraping will be performed
    * to return a list of directories under ${KamanjaInstallDirectory}/tests. Each directory found will be considered
    * a "KamanjaApplication".
    *
    */
  private def getApplicationDirectories(dir: String): List[File] = {
    val d = new File(dir)
    if(d.exists && d.isDirectory) {
      return d.listFiles.filter(_.isDirectory).toList
    }
    else
      return List[File]()
  }

  /** Given a List of Application Directories, this will search each directory for an applicable configuration file.
    *
    * @param applicationDirs
    * @return
    */
  private def getApplicationConfigFiles(applicationDirs: List[File]): List[File] = {
    var applicationConfigFiles: ListBuffer[File] = ListBuffer[File]()
    applicationDirs.foreach(d => {
      if (d.exists && d.isDirectory) {
        var dirFiles: ListBuffer[File] = ListBuffer[File]()
        val files = d.listFiles.filter(_.isFile).toList

        files.foreach(file => {
          if (file.getName.toLowerCase != "applicationconfiguration.json" && file.getName.toLowerCase != "appconfig.json") {
            logger.warn(s"***WARN*** File '${file.getName}' is an unaccepted name for a configuration file, please use either 'ApplicationConfiguration.json' or 'AppConfig.json'")
          }
          else {
            dirFiles = dirFiles :+ file
          }
        })

        if (dirFiles.length == 0) {
          logger.warn(s"***WARN*** Failed to discover any configuration files in application directory '${d.getName}'. This application will not be tested.")
        }
        else if (dirFiles.length > 1) {
          logger.warn(s"***WARN*** Multiple configuration files found. Using the first file found '${dirFiles(0)}'")
        }
        else {
          applicationConfigFiles = applicationConfigFiles :+ dirFiles(0)
        }
      }
    })
    return applicationConfigFiles.toList
  }

  /** Returns a list of KamanjaApplication given a test directory by getting a list of application folders within the test directory and creating a KamanjaApplication instance for each config file.
    *
    * @param testDir
    * @return
    */
  private def initializeApplications(testDir: String): List[KamanjaApplication] = {
    val dir = new File(testDir)
    var applicationConfigFiles: List[File] = List[File]()
    var apps: ListBuffer[KamanjaApplication] = ListBuffer[KamanjaApplication]()
    if(dir.exists && dir.isDirectory) {
      val appDirs = getApplicationDirectories(dir.getAbsolutePath)
      var count = 0
      applicationConfigFiles = getApplicationConfigFiles(appDirs)
      applicationConfigFiles.foreach(appConfigFile => {
        val appConfig = new KamanjaApplicationConfiguration
        apps = apps :+ appConfig.initializeApplication(appDirs(count).getAbsolutePath, appConfigFile.getAbsolutePath)
        count = count + 1
      })
      return apps.toList
    }
    else {
      logger.error("***ERROR*** Test Directory '$testDir' either doesn't exist or isn't a directory.")
      throw new KamanjaApplicationException(s"***ERROR*** Test Directory '$testDir' either doesn't exist or isn't a directory.")
    }
  }
}
