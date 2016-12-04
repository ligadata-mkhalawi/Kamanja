package com.ligadata.InputAdapters.pst

import org.apache.logging.log4j.LogManager
import java.util.{UUID, Vector}

import scala.collection.mutable.ListBuffer
import com.pff._
import org.apache.tika
import org.apache.tika.Tika
/**
  * Created by Yousef on 11/30/2016.
  */
class PSTReader {
  var depth = -1
  var alertID = 1
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)

  def readPSTFile(PSTfile: String, outputPath: String): Unit = {
    val pstFile: PSTFile = new PSTFile(PSTfile)
    logger.info(pstFile.getMessageStore.getDisplayName)
    processFolder(pstFile.getRootFolder, outputPath)
    pstFile.getFileHandle.close
  }

  def processFolder(folder: PSTFolder, outputPath: String): Unit = {
    depth = depth + 1
    if(depth > 0){
      printDepth
      logger.info(folder.getDisplayName)
    }

    if (folder.hasSubfolders) {
      val childFolders: Vector[PSTFolder] = folder.getSubFolders
      for ( childFolder <- childFolders) {
        processFolder(childFolder, outputPath)
      }
    }

    if (folder.getContentCount > 0) {
      depth = depth + 1

      var email: PSTMessage = folder.getNextChild.asInstanceOf[PSTMessage]
      while (email != null) {
        printDepth
        logger.info("Email: " + email.getSubject)
        createObject(email, outputPath);///change to return message to engine
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

  def createObject(email: PSTMessage, outputPath: String): Unit = {
    logger.info("Extracting header from email...");
    val tika: Tika = new Tika
 //   var  attachmentFilesList: ListBuffer[String] =  ListBuffer[String]()
    var attachmentsListTuple = List[(String, String)] ()
    val  numberOfAttachmentFile: Int = email.getNumberOfAttachments
    for(i <- 0 until numberOfAttachmentFile){
      val attach: PSTAttachment = email.getAttachment(i)
     // attachmentFilesList
//      attachmentFilesList += tika.parseToString(attach.getFileInputStream)
//      attach.getFilename
      attachmentsListTuple :+= (attach.getFilename, tika.parseToString(attach.getFileInputStream))
    }

    val sender: String = email.getSenderEmailAddress
    val reciever: String = email.getDisplayTo
   // var uniqueKey: UUID = UUID.randomUUID
    if (reciever.length() > 0 && sender.length() > 0) {
      logger.info("Extracting email body..");
      //outputPath = outputPath + uniqueKey.toString().replaceAll("-", "").replaceAll("_", "") + "a";
      val fs: FileSearch = new FileSearch
      val content: String = fs.createMessageContent(email);
      var attachment: ListBuffer[String] = ListBuffer[String]()
//      for (j <- 0 until attachmentFilesList.size ){
//        attachment += (outputPath + "." + (j + 1) + ".txt")//  fs.createmessage(outputPath + "." + (j + 1) + ".txt", attachmentFilesList(j));
//      }
      fs.createMessage(outputPath + ".txt", content,  attachmentsListTuple);
      logger.info("Done from extracting email body.");
    } else {
      logger.info("This message have not header.");
    }
  }
}
