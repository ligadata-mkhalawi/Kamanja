package com.ligadata.InputAdapters.pst

import org.apache.logging.log4j.LogManager
import java.util.{UUID, Vector}
import com.ligadata.InputAdapters.{SmartFileHandler, SmartFileMessage}
import scala.collection.mutable.ListBuffer
import com.pff._
import org.apache.tika
import org.apache.tika.Tika

/**
  * Created by Yousef on 1/24/2017.
  */
class PSTReader {
  var depth = -1
  var alertID = 1
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  val emailsList = ListBuffer[String]()
  def readPSTFile(PSTfile: String, outputPath: String, smartFile: SmartFileHandler): ListBuffer[String] = {
    val pstFile: PSTFile = new PSTFile(PSTfile)
    logger.info(pstFile.getMessageStore.getDisplayName)
    processFolder(pstFile.getRootFolder, outputPath, smartFile)
    pstFile.getFileHandle.close
    emailsList
  }

  def processFolder(folder: PSTFolder, outputPath: String, smartFile: SmartFileHandler): Unit = {
    depth = depth + 1
    if(depth > 0){
      printDepth
      logger.info(folder.getDisplayName)
    }

    if (folder.hasSubfolders) {
      val childFolders: Vector[PSTFolder] = folder.getSubFolders
      for ( childFolder <- childFolders) {
        processFolder(childFolder, outputPath, smartFile)
      }
    }

    if (folder.getContentCount > 0) {
      depth = depth + 1

      var email: PSTMessage = folder.getNextChild.asInstanceOf[PSTMessage]
      while (email != null) {
        printDepth
        logger.info("Email: " + email.getSubject)
        val jsonString = createObject(email, outputPath);///change to return message to engine
        emailsList.append(jsonString)
        //val smartFileMessage = new SmartFileMessage(jsonString.getBytes(), 0, smartFile, 0)//change ofset
        email = folder.getNextChild.asInstanceOf[PSTMessage]
      }
      depth = depth - 1
    }
    depth = depth - 1
  }

  def printDepth(): Unit = {
    for(x <- 0 until depth - 1 ){
      logger.info(" | ")
    }
    logger.info(" | ")
  }

  def createObject(email: PSTMessage, outputPath: String): String = {
    logger.info("Extracting header from email...");
    val tika: Tika = new Tika
    var attachmentsListTuple = List[(String, String)] ()
    val  numberOfAttachmentFile: Int = email.getNumberOfAttachments
    for(i <- 0 until numberOfAttachmentFile){
      val attach: PSTAttachment = email.getAttachment(i)
      attachmentsListTuple :+= (attach.getFilename, tika.parseToString(attach.getFileInputStream))
    }

    val sender: String = email.getSenderEmailAddress
    val reciever: String = email.getDisplayTo
    if (reciever.length() > 0 && sender.length() > 0) {
      logger.info("Extracting email body..")
      val fs: FileSearch = new FileSearch
      val content: String = fs.createMessageContent(email)
      var attachment: ListBuffer[String] = ListBuffer[String]()
      val message: String= fs.createMessage(outputPath + ".txt", content,  attachmentsListTuple)
      logger.info("Done from extracting email body.")
      message
    } else {
      logger.info("This message have not header.")
      null
    }
  }
}