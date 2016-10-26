package com.ligadata.kamanja.test.application.configuration

/**
  * This object will copy over the dependency jars into a resource dir called kamanjaInstall/lib/system/. This will require the fat jars to be assembled prior to running tests,
  * which should occur when KamanjaAppTester/test is called. If it doesn't occur, check the top level build.sbt to ensure the settings are setup properly for KamanjaAppTester.
  */
object TestSetup {
  val kamanjaInstallDir = getClass.getResource("/kamanjaInstall").getPath
  val systemLib = s"$kamanjaInstallDir/lib/system/"
  val applicationLib = s"$kamanjaInstallDir/lib/application/"
  val scalaVersionFull = scala.util.Properties.versionNumberString
  val scalaVersion = scalaVersionFull.substring(0, scalaVersionFull.lastIndexOf('.'))
}
