package com.ligadata.test.utils

import java.io.{File, FileNotFoundException, IOException, FileWriter, BufferedWriter}
import java.net.ServerSocket

import scala.util.Random

import com.ligadata.KamanjaVersion.KamanjaVersion

object  TestUtils extends KamanjaTestLogger {
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
      return true
      //throw new FileNotFoundException(path.getAbsolutePath)
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

  def deleteFile(path:String):Unit = {
    val file = new File(path);
    deleteFile(file)
  }

  private def dumpStrTextToFile(strText: String, filePath: String): Unit = {
    val file = new File(filePath);
    val bufferedWriter = new BufferedWriter(new FileWriter(file,true))
    bufferedWriter.write(strText)
    bufferedWriter.close
  }

  def createAuditParamsFile(paramFile:String, tableName: String = null): String = {
    val file = new File(paramFile);
    deleteFile(file)
    var tbl = tableName
    if( tbl == null ){
      //extract tableName as last token of file path
      val fileNamePathTokens = paramFile.split("/")
      tbl = fileNamePathTokens(fileNamePathTokens.length-1)
    }
    dumpStrTextToFile("schema=" + tbl + "\n",paramFile)
    dumpStrTextToFile("table=" + tbl + "\n",paramFile)
    paramFile
  }


  def retry[T](n: Int)(fn : => T): T = {
    util.Try { fn } match {
      case util.Success(x) => x
      case _ if n > 1 => retry(n - 1)(fn)
      case util.Failure(e) => throw e
    }
  }
}
