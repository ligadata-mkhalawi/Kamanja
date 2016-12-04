package com.ligadata.InputAdapters.pst

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.apache.logging.log4j.LogManager
import javax.mail.Session
import javax.mail.internet.MimeMessage
import javax.mail.Header
import com.pff._
import java.util.Properties
import scala.collection.mutable.ListBuffer
import java.io._
/**
  * Created by Yousef on 11/30/2016.
  */
class FileSearch {

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  def createMessage(fileName: String, msgBody: String, attachments: List[(String, String)]): String = {//change the fileName to take PST file name
    var attachmentsJson: String = ""
    var jsonString: String = ""
    if(attachments.size > 0) {
      attachmentsJson = buildAttachmentJson(fileName, attachments)
      jsonString = "{\"filename\":\"%s\",\"messageBody\": \"%s\" %s}".format(fileName, msgBody, attachmentsJson)
    } else
      jsonString = "{\"filename\":\"%s\",\"messageBody\": \"%s\"}".format(fileName, msgBody)
    jsonString
  }

  def buildAttachmentJson(fileName: String, attachments: List[(String, String)]): String = {// change the fileName to take PST file name
    var attachmentsJson = new java.lang.StringBuilder()
    attachmentsJson.append(",\"attachments\": {")
    //var attachmentfilename = ""
    for (i <- 0 until attachments.size) {
      //attachmentfilename = fileName + "." + i + ".txt"
      if (i > 0)
        attachmentsJson.append(",\"%s\":\"%s\"".format(attachments(i)._1, attachments(i)._2))
      else
        attachmentsJson.append("\"%s\":\"%s\"".format(attachments(i)._1, attachments(i)._2))
    }
    attachmentsJson.append("}")
    attachmentsJson.toString
  }
  def createMessageContent(email: PSTMessage): String ={
    val body: String = email.getBody
    var content: String = ""
    logger.info("Creating a MIME email from PST...")
    val decodedString: Array[Byte] = email.getTransportMessageHeaders.getBytes
    val is: InputStream  = new ByteArrayInputStream(decodedString)
    var message: MimeMessage = null
    val s: Session = Session.getDefaultInstance(new Properties)
    message = new MimeMessage(s, is)
    message.getAllHeaderLines
    is.close
    val e/*: Enumeration[Header]*/ = message.getAllHeaders
    //for (Enumeration[Header] e = message.getAllHeaders(); e.hasMoreElements();) { ///this code in java
    while (e.hasMoreElements){
      val h/*: Header*/ = e.nextElement.asInstanceOf[Header]
      if (!h.getName().equalsIgnoreCase("content-type"))
        content = content + h.getName() + ": " + h.getValue() + "\n"
    }
    content = content + "\n" + body + "\n"
    logger.info("MIME email creating successfully")
    content
  }
}
