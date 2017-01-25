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
  * Created by Yousef on 1/24/2017.
  */
class FileSearch {

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  val stringBuilderObj: StringBuilder = new StringBuilder

  /**
    *
    * @param fileName filename for message
    * @param msgBody  email body
    * @param attachments attachments list (attachment name, attachment content)
    * @return an input message for model in json string
    */
  def createMessage(fileName: String, msgBody: String, attachments: List[(String, String)]): String = {//change the fileName to take PST file name
  var attachmentsJson: String = ""
    var jsonString: String = ""
    if(attachments.size > 0) {
      attachmentsJson = buildAttachmentJson(attachments)
      jsonString = "{\"filename\":\"%s\",\"messageBody\": \"%s\" %s}".format(fileName, msgBody, attachmentsJson)
    } else
      jsonString = "{\"filename\":\"%s\",\"messageBody\": \"%s\"}".format(fileName, msgBody)
    jsonString
  }

  /**
    * This method used to build an attachment part in json format
    * @param attachments list of attachment (attachment name, attachment content)
    * @return attachment part in json structure for input message
    */
  def buildAttachmentJson(attachments: List[(String, String)]): String = {// change the fileName to take PST file name
  val attachmentsJson = new java.lang.StringBuilder()
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

  /**
    * This method used to create message from emails as MIME format
    * @param email take a pst email
    * @return a string with MIME email format
    */
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

  /**
    * this method used to create a file of emails where each line is a json String that belong to email
    * @param values list of json emails
    * @return a string that includes all emails
    */
  def makeString(values: ListBuffer[String]): String = {
    for(value <- values){
      stringBuilderObj.append(value)
      stringBuilderObj.append(System.getProperty("line.separator"))
    }
    stringBuilderObj.toString()
  }
}
