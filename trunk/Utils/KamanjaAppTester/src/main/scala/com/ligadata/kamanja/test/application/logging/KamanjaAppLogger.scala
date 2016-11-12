package com.ligadata.kamanja.test.application.logging

import java.io.{File, PrintWriter, StringWriter}
import java.text.SimpleDateFormat
import java.util.Calendar

import com.ligadata.kamanja.test.application.EmbeddedServicesManager

import scala.io._

case class KamanjaAppLoggerException(message: String, cause: Throwable = null) extends Exception(message, cause)

object KamanjaAppLogger {
  private var logger: KamanjaAppLogger = _

  def createKamanjaAppLogger(kamanjaInstallDir: String): KamanjaAppLogger = {
    if(logger != null)
      logger.close
    logger = new KamanjaAppLogger(kamanjaInstallDir)
    return logger
  }

  def getKamanjaAppLogger: KamanjaAppLogger = {
    if(logger == null)
      throw new KamanjaAppLoggerException("KamanjaAppLogger hasn't been created. Call createKamanjaAppLogger first.")
    return logger
  }

  def closeLogger: Unit ={
    if(logger == null)
      throw new KamanjaAppLoggerException("KamanjaAppLogger hasn't been created. Call createKamanjaAppLogger first.")
    logger.close
  }
}

class KamanjaAppLogger(kamanjaInstallDir: String) {
  private var logFile: File = _
  private lazy val pw: PrintWriter = new PrintWriter(logFile)
  init(new File(kamanjaInstallDir))

  private def init(kamanjaInstallDir: File): Unit = {
    val logDir = new File(s"$kamanjaInstallDir/logs")
    val testLogDir = new File(logDir, "test")
    if (!logDir.exists())
      if (!logDir.mkdir())
        throw new KamanjaAppLoggerException(s"***ERROR*** Failed to create directory ${logDir.getAbsolutePath}")

    if(!testLogDir.exists())
      if(!testLogDir.mkdir())
        throw new KamanjaAppLoggerException(s"***ERROR*** Failed to create directory ${logDir.getAbsolutePath}")

    val time = Calendar.getInstance().getTime()
    val yearMonthDayFormat = new SimpleDateFormat("yyyy-MM-dd")
    val currentDate = yearMonthDayFormat.format(time)

    var count = 1
    logFile = new File(s"$testLogDir", s"KamanjaAppTestResults-$currentDate-$count.html")

    while (logFile.exists()) {
      count += 1
      logFile = new File(s"$testLogDir", s"KamanjaAppTestResults-$currentDate-$count.html")
    }
    if (!logFile.createNewFile())
      throw new KamanjaAppLoggerException(s"***ERROR*** Failed to create log file ${logFile.getAbsolutePath}")

    pw.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n")
    pw.append("<html>\n")
    pw.append("<head>\n")
    pw.append("<meta charset=\"UTF-8\"/>\n")
    pw.append("<style type=\"text/css\"\n")
    pw.append("<!--\n")
    pw.append("body, table {font-family:arial,sans-serif; font-size: medium;}\n")
    pw.append("th {background: #336699; color: #FFFFFF; text-align: left;}\n")
    pw.append("-->\n")
    pw.append("</style>\n")
    pw.append("<title>Kamanja Application Test Results</title>\n")
    pw.append("</head>\n")
    pw.append("<body bgcolor=\"E5E5E5\" topmargin=\"6\" leftmargin=\"6\">\n")
    pw.append("<h1>Kamanja Application Tests</h1>\n")
    pw.append("<table cellspacing=\"0\" cellpadding=\"1\" border=\"1\" bordercolor=\"#224466\" width=\"100%\"\n")
    pw.append("<tr>\n")
    pw.append("<th bgcolor=\"E5E5E5\">Time</th>\n")
    pw.append("<th bgcolor=\"E5E5E5\">Message</th>\n")
    pw.append("</tr>\n")
  }

  private def log(message: String, foregroundColor: String): Unit = {
    val time = Calendar.getInstance().getTime()
    val dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    val currentDateTime = dateTimeFormat.format(time)

    println(message)
    pw.append(s"<tr>\n")
    pw.append(s"""<td bgcolor="B9B9B9"><font color="$foregroundColor">$currentDateTime</font></td>""" + "\n")
    pw.append(s"""<td bgcolor="B9B9B9"><font color="$foregroundColor">$message</font></td>""" + "\n")
    pw.append("</tr>\n")
  }

  def close: Unit ={
    if(pw != null)
      pw.close()
  }

  def info(message: String): Unit = {
    log(message, "000000")
  }

  def error(message:String): Unit = {
    log(message, "#FF0000")
  }

  def warn(message:String): Unit = {
    log(message, "FFFF00")
  }

  def getStackTraceAsString(cause: Throwable): String = {
    val sw = new StringWriter
    cause.printStackTrace(new PrintWriter(sw))
    sw.toString
  }
}