package com.ligadata.test.utils

import java.io.{File, FileNotFoundException, IOException}
import java.net.ServerSocket

import scala.util.Random

import com.ligadata.KamanjaVersion.KamanjaVersion

/**
 * Created by wtarver on 4/23/15.
 */
object TestUtils extends KamanjaTestLogger {
  val scalaVersionFull = scala.util.Properties.versionNumberString
  val scalaVersion = scalaVersionFull.substring(0, scalaVersionFull.lastIndexOf('.'))
  val kamanjaVersion = s"${KamanjaVersion.getMajorVersion()}.${KamanjaVersion.getMinorVersion()}.${KamanjaVersion.getMicroVersion()}"
  private val random: Random = new Random()

  def constructTempDir(dirPrefix:String):File = {
    val file:File = new File(System.getProperty("java.io.tmpdir"), dirPrefix + random.nextInt(10000000))
    if(!file.mkdirs()){
      throw new RuntimeException("Could not create temp directory: " + file.getAbsolutePath)
    }
    file.deleteOnExit()
    return file
  }

  def getAvailablePort:Int = {
    try {
      val socket:ServerSocket = new ServerSocket(0)
      try {
        return socket.getLocalPort
      }
      finally {
        socket.close();
      }
    }
    catch {
      case e:IOException => throw new IllegalStateException("Cannot find available port: " + e.getMessage, e)
    }
  }

  @throws(classOf[FileNotFoundException])
  def deleteFile(path:File):Boolean = {
    if(!path.exists()){
      throw new FileNotFoundException(path.getAbsolutePath)
    }
    var ret = true
    if (path.isDirectory){
      for(f <- path.listFiles) {
        ret = ret && deleteFile(f)
      }
    }
    logger.debug("[Kamanja Test Utils]: Deleting file '" + path + "'")
    return ret && path.delete()
  }

  def retry[T](n: Int)(fn : => T): T = {
    util.Try { fn } match {
      case util.Success(x) => x
      case _ if n > 1 => retry(n - 1)(fn)
      case util.Failure(e) => throw e
    }
  }
}
