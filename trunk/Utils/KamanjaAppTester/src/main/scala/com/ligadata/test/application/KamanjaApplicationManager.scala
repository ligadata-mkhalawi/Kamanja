package com.ligadata.test.application

import java.io.{File, FilenameFilter}

import com.ligadata.test.application.configuration.KamanjaApplicationConfiguration
import com.ligadata.test.utils.KamanjaTestLogger

import scala.collection.mutable.ListBuffer

class KamanjaApplicationManager(baseDir: String) extends KamanjaTestLogger {

  lazy val kamanjaApplications = initializeApplications(baseDir)

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
            logger.warn(s"[Kamanja Application Tester - ApplicationManager]: File '${file.getName}' is an unaccepted name for a configuration file, please use either 'ApplicationConfiguration.json' or 'AppConfig.json'")
          }
          else {
            dirFiles = dirFiles :+ file
          }
        })

        if (dirFiles.length == 0) {
          logger.warn(s"[Kamanja ApplicationTester - ApplicationManager]: Failed to discover any configuration files in application directory '${d.getName}'. This application will not be tested.")
        }
        else if (dirFiles.length > 1) {
          logger.warn(s"[Kamanja Application Tester - ApplicationManager]: Multiple configuration files found. Using the first file found '${dirFiles(0)}'")
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
      throw new KamanjaApplicationException(s"[Kamanja Application Tester - ApplicationManager]: Test Directory '$testDir' either doesn't exist or isn't a directory.")
    }
  }
}
