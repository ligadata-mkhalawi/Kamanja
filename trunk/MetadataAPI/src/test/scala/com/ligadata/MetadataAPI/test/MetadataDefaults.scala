package com.ligadata.MetadataAPI.test

import com.ligadata.test.utils.TestUtils

object MetadataDefaults {
  lazy val jarResourceDir = getClass.getResource("/jars/lib/system").getPath
  lazy val rootDir = getClass.getResource("/").getPath

  val nodeClassPath: String = {
    List(
      s"ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      s"ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      s"KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
    ).mkString(s".:$jarResourceDir/", s":$jarResourceDir/", "")
  }

  val metadataClasspath: String = {
    List(
      s"ExtDependencyLibs_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      s"ExtDependencyLibs2_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar",
      s"KamanjaInternalDeps_${TestUtils.scalaVersion}-${TestUtils.kamanjaVersion}.jar"
    ).mkString(s"$jarResourceDir/", s":$jarResourceDir/", "")
  }
}