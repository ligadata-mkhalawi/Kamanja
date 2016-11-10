package com.ligadata.kamanja.test.application.logging

import java.io.{File, PrintWriter, StringWriter}
import java.text.SimpleDateFormat
import java.util.Calendar

import scala.io._

case class KamanjaAppLoggerException(message: String, cause: Throwable = null) extends Exception(message, cause)

object KamanjaAppLogger {
  private val logger: KamanjaAppLogger = new KamanjaAppLogger("logs")

  def info(message: String): Unit = {
    logger.log(message, "000000")
  }

  def error(message:String): Unit = {
    logger.log(message, "#FF0000")
  }

  def warn(message:String): Unit = {
    logger.log(message, "FFFF00")
  }

  def getStackTraceAsString(cause: Throwable): String = {
    val sw = new StringWriter
    cause.printStackTrace(new PrintWriter(sw))
    sw.toString
  }

  def close: Unit = {
    logger.close
  }
}

class KamanjaAppLogger(logDirectory: String) {
  private var logFile: File = _
  private lazy val pw: PrintWriter = new PrintWriter(logFile)
  init(new File(logDirectory))

  private def init(logDir: File): Unit = {
    if (!logDir.exists()) {
      if (!logDir.mkdir()) {
        throw new KamanjaAppLoggerException(s"[Kamanja Application Tester] - ***ERROR*** Failed to create log directory ${logDir.getAbsolutePath}")
      }
    }

    val time = Calendar.getInstance().getTime()
    val yearMonthDayFormat = new SimpleDateFormat("yyyy-MM-dd")
    val currentDate = yearMonthDayFormat.format(time)

    var count = 1
    logFile = new File(logDir, s"KamanjaAppTestResults-$currentDate-$count.html")

    while (logFile.exists()) {
      count += 1
      logFile = new File(logDir, s"KamanjaAppTestResults-$currentDate-$count.html")
    }
    if (!logFile.createNewFile())
      throw new KamanjaAppLoggerException(s"[Kamanja Application Tester] - ***ERROR*** Failed to create log file ${logFile.getAbsolutePath}")

    pw.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">")
    pw.append("<html>")
    pw.append("<head>")
    pw.append("<meta charset=\"UTF-8\"/>")
    pw.append("<style type=\"text/css\"")
    pw.append("<!--")
    pw.append("body, table {font-family:arial,sans-serif; font-size: medium;}")
    pw.append("th {background: #336699; color: #FFFFFF; text-align: left;}")
    pw.append("-->")
    pw.append("</style>")
    pw.append("<title>Kamanja Application Test Results</title>")
    pw.append("</head>")
    pw.append("<body bgcolor=\"#FFFFFF\" topmargin=\"6\" leftmargin=\"6\">")
    pw.append("<h1>Kamanja Application Tests</h1>")
    pw.append("<table cellspacing=\"0\" cellpadding=\"2\" border=\"1\" bordercolor=\"#224466\" width=\"100%\"")
    pw.append("<tr>")
    pw.append("<th>Time</th>")
    pw.append("<th>Message</th>")
    pw.append("</tr>")
  }

  private def log(message: String, foregroundColor: String): Unit = {
    val time = Calendar.getInstance().getTime()
    val dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    val currentDateTime = dateTimeFormat.format(time)

    println(message)
    pw.append(s"""<tr>""")
    pw.append(s"""<td bgcolor="B9B9B9"><font color="$foregroundColor">$currentDateTime</font></td>""")
    pw.append(s"""<td bgcolor="B9B9B9"><font color="$foregroundColor">$message</font></td>""")
    pw.append("</tr>")
  }

  private def close: Unit ={
    if(pw != null)
      pw.close()
  }
}