package com.ligadata.InputAdapters

import java.io.IOException
import com.ligadata.AdaptersConfiguration.SmartFileAdapterConfiguration
import org.apache.logging.log4j.LogManager
import scala.util.control.Breaks._

import scala.actors.threadpool.{ExecutorService, Executors}

/**
  *
  *
  * @param adapterConfig
  * @param fileHandler file to read messages from
  * @param startOffset offset in the file to start with
  * @param consumerContext has required params
  * @param messageFoundCallback to call for every read message
  * @param finishCallback call when finished reading
  */
class FileMessageExtractor(parentSmartFileConsumer : SmartFileConsumer,
                            parentExecutor: ExecutorService,
                           adapterConfig : SmartFileAdapterConfiguration,
                           fileHandler: SmartFileHandler,
                           startOffset : Long,
                           consumerContext : SmartFileConsumerContext,
                           messageFoundCallback : (SmartFileMessage, SmartFileConsumerContext) => Unit,
                           finishCallback : (SmartFileHandler, SmartFileConsumerContext, Int) => Unit ) {

  private val maxlen: Int = adapterConfig.monitoringConfig.workerBufferSize * 1024 * 1024 //in MB

  val srcDirLocInfo = parentSmartFileConsumer.getDirLocationInfo(fileHandler.getParentDir)
  private val message_separator : Char =
    if(srcDirLocInfo == null) adapterConfig.monitoringConfig.messageSeparator
    else srcDirLocInfo.messageSeparator
  private val message_separator_len = 1// since separator is a char

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  private var currentMsgNum = 0
  private var globalOffset = 0L

  private val extractExecutor = Executors.newFixedThreadPool(1)
  private val updatExecutor = Executors.newFixedThreadPool(1)



  private var finished = false
  private var processingInterrupted = false

  private var fileProcessingStartTm : Long = 0L

  def extractMessages() : Unit = {

    if(!fileHandler.exists()){
      finishCallback(fileHandler, consumerContext, SmartFileConsumer.FILE_STATUS_NOT_FOUND)
    }

    else {

      //keep updating status so leader knows participant is working fine
      //TODO : find a way to send the update in same reading thread
      val statusUpdateThread = new Runnable() {
        override def run(): Unit = {
          try {
            while (!finished) {
              //put filename~offset~timestamp
              val data = fileHandler.getFullPath + "~" + currentMsgNum + "~" + System.nanoTime
              logger.debug("SMART FILE CONSUMER - Node {} with partition {} is updating status to value {}",
                consumerContext.nodeId, consumerContext.partitionId.toString, data)
              consumerContext.envContext.saveConfigInClusterCache(consumerContext.statusUpdateCacheKey, data.getBytes)


              Thread.sleep(consumerContext.statusUpdateInterval)

            }
          }
          catch {
            case ie: InterruptedException => {}
            case e: Exception => logger.error("", e)
            case e: Throwable => logger.error("", e)
          }
        }
      }
      updatExecutor.execute(statusUpdateThread)

      //just run it in a separate thread
      val extractorThread = new Runnable() {
        override def run(): Unit = {
          readBytesChunksFromFile()
        }
      }
      extractExecutor.execute(extractorThread)
    }
  }

  private def readBytesChunksFromFile(): Unit = {

    val byteBuffer = new Array[Byte](maxlen)

    var readlen = 0
    var len: Int = 0

    val fileName = fileHandler.getFullPath

    fileProcessingStartTm = System.nanoTime
    logger.warn("Smart File Consumer - Starting reading messages from file {} , on Node {} , PartitionId {}",
      fileName, consumerContext.nodeId, consumerContext.partitionId.toString)

    try {
      fileHandler.openForRead()
    } catch {

      case fio: java.io.FileNotFoundException => {
        logger.error("SMART_FILE_CONSUMER Exception accessing the file for processing the file - File is missing",fio)
        finishCallback(fileHandler, consumerContext, SmartFileConsumer.FILE_STATUS_NOT_FOUND)
        shutdownThreads
        return
      }
      case fio: IOException => {
        logger.error("SMART_FILE_CONSUMER Exception accessing the file for processing ",fio)
        finishCallback(fileHandler, consumerContext, SmartFileConsumer.FILE_STATUS_CORRUPT)
        shutdownThreads
        return
      }
      case ex : Exception => {
        logger.error("", ex)
        finishCallback(fileHandler, consumerContext, SmartFileConsumer.FILE_STATUS_CORRUPT)
        shutdownThreads
        return
      }
      case ex : Throwable => {
        logger.error("", ex)
        finishCallback(fileHandler, consumerContext, SmartFileConsumer.FILE_STATUS_CORRUPT)
        shutdownThreads
        return
      }
    }

    var curReadLen = 0
    var lastReadLen = 0

    //skip to startOffset
    //TODO : modify to use seek whenever possible
    if(startOffset > 0)
      logger.debug("SMART FILE CONSUMER - skipping into offset {} while reading file {}", startOffset.toString, fileName)
    var totalReadLen = 0
    var lengthToRead : Int = 0
    do{
      lengthToRead = Math.min(maxlen, startOffset - totalReadLen).toInt
      curReadLen = fileHandler.read(byteBuffer, 0, lengthToRead)
      totalReadLen += curReadLen
      logger.debug("SMART FILE CONSUMER - reading {} bytes from file {} but got only {} bytes",
        lengthToRead.toString, fileHandler.getFullPath, curReadLen.toString)
    }while(totalReadLen < startOffset && curReadLen >0)

    logger.debug("SMART FILE CONSUMER - totalReadLen from file {} is {}", fileHandler.getFullPath,totalReadLen.toString)

    globalOffset = totalReadLen

    curReadLen = 0

    try {

      breakable {
        do {
          try {

            if (Thread.currentThread().isInterrupted) {
              logger.warn("SMART FILE CONSUMER (FileMessageExtractor) - interrupted while reading file {}", fileHandler.getFullPath)
              processingInterrupted = true
              //break
            }
            if(parentExecutor == null){
              logger.warn("SMART FILE CONSUMER (FileMessageExtractor) - (parentExecutor = null) while reading file {}", fileHandler.getFullPath)
              processingInterrupted = true
              //break
            }
            if(parentExecutor.isShutdown){
              logger.warn("SMART FILE CONSUMER (FileMessageExtractor) - parentExecutor is shutdown while reading file {}", fileHandler.getFullPath)
              processingInterrupted = true
              //break
            }
            if(parentExecutor.isTerminated){
              logger.warn("SMART FILE CONSUMER (FileMessageExtractor) - parentExecutor is terminated while reading file {}", fileHandler.getFullPath)
              processingInterrupted = true
              //break
            }

            if(!processingInterrupted) {
              var curReadLen = fileHandler.read(byteBuffer, readlen, maxlen - readlen - 1)
              lastReadLen = curReadLen

              logger.debug("SMART FILE CONSUMER - reading {} bytes from file {}. got actually {} bytes ",
                (maxlen - readlen - 1).toString, fileHandler.getFullPath, curReadLen.toString)

              if (curReadLen > 0) {
                readlen += curReadLen
              }
              else // First time reading into buffer triggered end of file (< 0)
                readlen = curReadLen
              val minBuf = maxlen / 3; // We are expecting at least 1/3 of the buffer need to fill before
              while (readlen < minBuf && curReadLen > 0) {
                // Re-reading some more data
                curReadLen = fileHandler.read(byteBuffer, readlen, maxlen - readlen - 1)
                logger.debug("SMART FILE CONSUMER - not enough read. reading more {} bytes from file {} . got actually {} bytes",
                  (maxlen - readlen - 1).toString, fileHandler.getFullPath, curReadLen.toString)
                if (curReadLen > 0) {
                  readlen += curReadLen
                }
                lastReadLen = curReadLen
              }
            }

          } catch {

            case ioe: IOException => {
              logger.error("Failed to read file " + fileName, ioe)
              finishCallback(fileHandler, consumerContext, SmartFileConsumer.FILE_STATUS_CORRUPT)
              shutdownThreads
              return
            }
            case e: Throwable => {
              logger.error("Failed to read file, file corrupted " + fileName, e)
              finishCallback(fileHandler, consumerContext, SmartFileConsumer.FILE_STATUS_CORRUPT)
              shutdownThreads
              return
            }
          }

          if(!processingInterrupted) {
            logger.debug("SMART FILE CONSUMER (FileMessageExtractor) - readlen1={}", readlen.toString)
            if (readlen > 0) {
              len += readlen

              //e.g we have 1024, but 1000 is consumeByte
              val consumedBytes = extractMessages(byteBuffer, readlen)
              if (consumedBytes < readlen) {
                val remainigBytes = readlen - consumedBytes
                val newByteBuffer = new Array[Byte](maxlen)
                // copy reaming from byteBuffer to byteBuffer
                /*System.arraycopy(byteBuffer, consumedBytes + 1, newByteBuffer, 0, remainigBytes)
            byteBuffer = newByteBuffer*/
                for (i <- 0 to readlen - consumedBytes) {
                  byteBuffer(i) = byteBuffer(consumedBytes + i)
                }

                readlen = readlen - consumedBytes
              }
              else {
                readlen = 0
              }
            }
          }
        } while (lastReadLen > 0 && !processingInterrupted)
      }

      logger.debug("SMART FILE CONSUMER (FileMessageExtractor) - readlen2={}", readlen.toString)
      //now if readlen>0 means there is one last message.
      //most likely this happens if last message is not followed by the separator
      if(readlen > 0 && !processingInterrupted){
        val lastMsg: Array[Byte] = byteBuffer.slice(0, readlen)
        if(lastMsg.length == 1 && lastMsg(0).asInstanceOf[Char] == message_separator){

        }
        else {
          currentMsgNum += 1
          val msgOffset = globalOffset + lastMsg.length + message_separator_len //byte offset of next message in the file
          val smartFileMessage = new SmartFileMessage(lastMsg, msgOffset, fileHandler, currentMsgNum)
          messageFoundCallback(smartFileMessage, consumerContext)
        }
      }


    }
    catch {
      case ioe: IOException => {
        logger.error("SMART FILE CONSUMER: Exception while accessing the file for processing " + fileName, ioe)
        finishCallback(fileHandler, consumerContext, SmartFileConsumer.FILE_STATUS_CORRUPT)
        shutdownThreads
        return
      }
      case et: Throwable => {
        logger.error("SMART FILE CONSUMER: Throwable while accessing the file for processing " + fileName, et)
        finishCallback(fileHandler, consumerContext, SmartFileConsumer.FILE_STATUS_CORRUPT)
        shutdownThreads
        return
      }
    }

    // Done with this file... mark is as closed
    try {
      if (fileHandler != null) fileHandler.close

    } catch {
      case ioe: IOException => {
        logger.error("SMART FILE CONSUMER: Exception while closing file " + fileName, ioe)
      }
      case et: Throwable => {
        logger.error("SMART FILE CONSUMER: Throwable while closing file " + fileName, et)
      }
    }
    finally{
      if(finishCallback != null) {

        val endTm = System.nanoTime
        val elapsedTm = endTm - fileProcessingStartTm

        if(processingInterrupted) {
          logger.debug("SMART FILE CONSUMER (FileMessageExtractor) - sending interrupting flag for file {}", fileName)
          logger.warn("SMART FILE CONSUMER - finished reading file %s. Operation took %fms on Node %s, PartitionId %s. StartTime:%d, EndTime:%d.".format(fileName, elapsedTm/1000000.0, consumerContext.nodeId, consumerContext.partitionId.toString, fileProcessingStartTm, endTm))
          finishCallback(fileHandler, consumerContext, SmartFileConsumer.FILE_STATUS_ProcessingInterrupted)
        }
        else {
          logger.warn("SMART FILE CONSUMER - finished reading file %s. Operation took %fms on Node %s, PartitionId %s. StartTime:%d, EndTime:%d.".format(fileName, elapsedTm/1000000.0, consumerContext.nodeId, consumerContext.partitionId.toString, fileProcessingStartTm, endTm))
          finishCallback(fileHandler, consumerContext, SmartFileConsumer.FILE_STATUS_FINISHED)
        }
      }

      shutdownThreads()
    }

  }

  private def shutdownThreads(): Unit ={
    finished = true

    try {
      if (fileHandler != null) fileHandler.close()

    } catch {
      case et: Throwable =>
    }
    
    logger.debug("File message Extractor - shutting down updatExecutor")
    MonitorUtils.shutdownAndAwaitTermination(updatExecutor, "file message extracting status updator")

    logger.debug("File message Extractor - shutting down extractExecutor")
    MonitorUtils.shutdownAndAwaitTermination(extractExecutor, "file message extractor")
  }

  private def extractMessages(chunk : Array[Byte], len : Int) : Int = {
    var indx = 0
    var prevIndx = indx

    breakable {
      for (i <- 0 to len - 1) {

        if (Thread.currentThread().isInterrupted) {
          logger.info("SMART FILE CONSUMER (FileMessageExtractor) - interrupted while extracting messages from file {}", fileHandler.getFullPath)
          processingInterrupted = true
          break
        }
        if (parentExecutor == null) {
          logger.info("SMART FILE CONSUMER (FileMessageExtractor) - (parentExecutor = null) while extracting messages from file {}", fileHandler.getFullPath)
          processingInterrupted = true
          break
        }
        if (parentExecutor.isShutdown) {
          logger.info("SMART FILE CONSUMER (FileMessageExtractor) - parentExecutor is shutdown while extracting messages from file {}", fileHandler.getFullPath)
          processingInterrupted = true
          break
        }
        if (parentExecutor.isTerminated) {
          logger.info("SMART FILE CONSUMER (FileMessageExtractor) - parentExecutor is terminated while extracting messages from file {}", fileHandler.getFullPath)
          processingInterrupted = true
          break
        }

        if (chunk(i).asInstanceOf[Char] == message_separator) {
          val newMsg: Array[Byte] = chunk.slice(prevIndx, indx)
          if (newMsg.length > 0) {
            currentMsgNum += 1
            //if(globalOffset >= startOffset) {//send messages that are only after startOffset
            val msgOffset = globalOffset + newMsg.length + message_separator_len //byte offset of next message in the file
            val smartFileMessage = new SmartFileMessage(newMsg, msgOffset, fileHandler, currentMsgNum)
            messageFoundCallback(smartFileMessage, consumerContext)

            //}
            prevIndx = indx + 1
            globalOffset = globalOffset + newMsg.length + message_separator_len
          }
        }
        indx = indx + 1
      }
    }



    /*if(prevIndx == chunk.length)
      Array()
    else
      chunk.slice(prevIndx, chunk.length)*/
    prevIndx
  }
}
