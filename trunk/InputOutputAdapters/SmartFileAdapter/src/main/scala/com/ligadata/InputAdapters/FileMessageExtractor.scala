package com.ligadata.InputAdapters

import java.io.IOException
import com.ligadata.AdaptersConfiguration.SmartFileAdapterConfiguration
import com.ligadata.Utils.Utils
import org.apache.logging.log4j.LogManager
import scala.util.control.Breaks._

import scala.actors.threadpool.{ExecutorService, Executors}

/**
  *
  *
  * @param adapterConfig
  * @param fileHandler          file to read messages from
  * @param startOffset          offset in the file to start with
  * @param consumerContext      has required params
  * @param messageFoundCallback to call for every read message
  * @param finishCallback       call when finished reading
  */
class FileMessageExtractor(parentSmartFileConsumer: SmartFileConsumer,
                           parentExecutor: ExecutorService,
                           adapterConfig: SmartFileAdapterConfiguration,
                           fileHandler: SmartFileHandler,
                           startOffset: Long,
                           consumerContext: SmartFileConsumerContext,
                           messageFoundCallback: (SmartFileMessage, SmartFileConsumerContext) => Unit,
                           finishCallback: (SmartFileHandler, SmartFileConsumerContext, Int, InputAdapterStatus) => Unit) {

  private val maxlen: Int = adapterConfig.monitoringConfig.workerBufferSize * 1024 * 1024 //in MB

  val srcDirLocInfo = parentSmartFileConsumer.getDirLocationInfo(fileHandler.getParentDir)
  private val message_separator: Char =
    if (srcDirLocInfo == null) adapterConfig.monitoringConfig.messageSeparator
    else srcDirLocInfo.messageSeparator
  private val message_separator_len = 1 // since separator is a char

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  private var currentMsgNum = 0
  private var globalOffset = 0L
  private var totalReadLen = 0L

  private val extractExecutor = Executors.newFixedThreadPool(1)
  private val updatExecutor = Executors.newFixedThreadPool(1)


  private var finished = false
  private var processingInterrupted = false
  private var isFileCorrupted = false

  private var fileProcessingStartTs: Long = 0L
  private var fileProcessingStartTime: String = ""

  def getFileStats(status: Int): InputAdapterStatus = {
    val fileProcessingEndTime = Utils.GetCurDtTmStrWithTZ
    val statusStr = status match {
      case SmartFileConsumer.FILE_STATUS_CORRUPT => "Corrupt"
      case SmartFileConsumer.FILE_STATUS_FINISHED => "Success"
      case SmartFileConsumer.FILE_STATUS_NOT_FOUND => "NotFound"
      case SmartFileConsumer.FILE_STATUS_ProcessingInterrupted => "Interrupted"
      case _ => throw new Exception("Unsupported file processing status " + status)
    }

    InputAdapterStatus(fileHandler.getFullPath, currentMsgNum, fileProcessingStartTime, fileProcessingEndTime,
      totalReadLen, consumerContext.nodeId, statusStr)
  }

  def extractMessages(): Unit = {
    fileProcessingStartTime = Utils.GetCurDtTmStrWithTZ

    if (!fileHandler.exists()) {
      sendFinishFlag(SmartFileConsumer.FILE_STATUS_NOT_FOUND)
    }
    else {
      //keep updating status so leader knows participant is working fine
      //TODO : find a way to send the update in same reading thread
      val statusUpdateThread = new Runnable() {
        override def run(): Unit = {
          try {
            while (!finished && parentExecutor != null && !parentExecutor.isShutdown &&
              !parentExecutor.isTerminated && !parentSmartFileConsumer.isConsumerShutdown) {
              //put filename~offset~timestamp
              val data = fileHandler.getFullPath + "~" + currentMsgNum + "~" + System.nanoTime + "~in-progress"
              logger.debug("SMART FILE CONSUMER - Node {} with partition {} is updating status to value {}",
                consumerContext.nodeId, consumerContext.partitionId.toString, data)
              if(!finished) {
                consumerContext.envContext.saveConfigInClusterCache(consumerContext.statusUpdateCacheKey, data.getBytes)
                Thread.sleep(consumerContext.statusUpdateInterval)
              }
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
          try {
            readBytesChunksFromFile()
          } catch {
            case e: Throwable => {
              logger.error("", e)
              sendFinishFlag(SmartFileConsumer.FILE_STATUS_CORRUPT)
              shutdownThreads
            }
          }
        }
      }
      extractExecutor.execute(extractorThread)
    }
  }

  private def readBytesChunksFromFile(): Unit = {
    try {
      val byteBuffer = new Array[Byte](maxlen)

      var readlen = 0
      var len: Int = 0

      val fileName = fileHandler.getFullPath

      fileProcessingStartTs = System.nanoTime
      logger.warn("Smart File Consumer - Starting reading messages from file {} , on Node {} , PartitionId {}",
        fileName, consumerContext.nodeId, consumerContext.partitionId.toString)

      try {
        fileHandler.openForRead()
      } catch {

        case fio: java.io.FileNotFoundException => {
          logger.error("SMART_FILE_CONSUMER Exception accessing the file for processing the file - File is missing", fio)
          sendFinishFlag(SmartFileConsumer.FILE_STATUS_NOT_FOUND)
          shutdownThreads
          return
        }
        case fio: IOException => {
          logger.error("SMART_FILE_CONSUMER Exception accessing the file for processing ", fio)
          sendFinishFlag(SmartFileConsumer.FILE_STATUS_CORRUPT)
          shutdownThreads
          return
        }
        case ex: Exception => {
          logger.error("", ex)
          sendFinishFlag(SmartFileConsumer.FILE_STATUS_CORRUPT)
          shutdownThreads
          return
        }
        case ex: Throwable => {
          logger.error("", ex)
          sendFinishFlag(SmartFileConsumer.FILE_STATUS_CORRUPT)
          shutdownThreads
          return
        }
      }

      var curReadLen = 0
      var lastReadLen = 0

      //skip to startOffset
      //TODO : modify to use seek whenever possible
      if (startOffset > 0)
        logger.debug("SMART FILE CONSUMER - skipping into offset {} while reading file {}", startOffset.toString, fileName)

      var lengthToRead: Int = 0
      do {
        lengthToRead = Math.min(maxlen, startOffset - totalReadLen).toInt
        curReadLen = fileHandler.read(byteBuffer, 0, lengthToRead)
        if (curReadLen > 0)
          totalReadLen += curReadLen
        logger.debug("SMART FILE CONSUMER - reading {} bytes from file {} but got only {} bytes",
          lengthToRead.toString, fileHandler.getFullPath, curReadLen.toString)
      } while (totalReadLen < startOffset && curReadLen > 0)

      logger.debug("SMART FILE CONSUMER - totalReadLen from file {} is {}", fileHandler.getFullPath, totalReadLen.toString)

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
              if (parentExecutor == null) {
                logger.warn("SMART FILE CONSUMER (FileMessageExtractor) - (parentExecutor = null) while reading file {}", fileHandler.getFullPath)
                processingInterrupted = true
                //break
              }
              if (parentExecutor.isShutdown) {
                logger.warn("SMART FILE CONSUMER (FileMessageExtractor) - parentExecutor is shutdown while reading file {}", fileHandler.getFullPath)
                processingInterrupted = true
                //break
              }
              if (parentExecutor.isTerminated) {
                logger.warn("SMART FILE CONSUMER (FileMessageExtractor) - parentExecutor is terminated while reading file {}", fileHandler.getFullPath)
                processingInterrupted = true
                //break
              }

              if (!processingInterrupted) {
                var curReadLen = fileHandler.read(byteBuffer, readlen, maxlen - readlen - 1)
                lastReadLen = curReadLen

                logger.debug("SMART FILE CONSUMER - reading {} bytes from file {}. got actually {} bytes ",
                  (maxlen - readlen - 1).toString, fileHandler.getFullPath, curReadLen.toString)

                if (curReadLen > 0) {
                  readlen += curReadLen
                  totalReadLen += curReadLen
                }
                else // First time reading into buffer triggered end of file (< 0)
                  readlen = curReadLen
                val minBuf = maxlen / 3; // We are expecting at least 1/3 of the buffer need to fill before
                while (readlen < minBuf && curReadLen > 0) {
                  // Re-reading some more data
                  try {
                    curReadLen = fileHandler.read(byteBuffer, readlen, maxlen - readlen - 1)
                  }
                  catch {
                    case e: Throwable => {
                      logger.error("SMART FILE CONSUMER - " + adapterConfig.Name + "Failed to read file " + fileName +
                        ". only read " + totalReadLen + " bytes", e)
                      isFileCorrupted = true
                      curReadLen = -1
                    }
                  }
                  logger.debug("SMART FILE CONSUMER - not enough read. reading more {} bytes from file {} . got actually {} bytes",
                    (maxlen - readlen - 1).toString, fileHandler.getFullPath, curReadLen.toString)
                  if (curReadLen > 0) {
                    readlen += curReadLen
                    totalReadLen += curReadLen
                  }
                  lastReadLen = curReadLen
                }
              }

            } catch {

              case ioe: IOException => {
                logger.error("Failed to read file " + fileName, ioe)
                sendFinishFlag(SmartFileConsumer.FILE_STATUS_CORRUPT)
                shutdownThreads
                return
              }
              case e: Throwable => {
                logger.error("Failed to read file, file corrupted " + fileName, e)
                sendFinishFlag(SmartFileConsumer.FILE_STATUS_CORRUPT)
                shutdownThreads
                return
              }
            }

            if (!processingInterrupted) {
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
        if (readlen > 0 && !processingInterrupted) {
          val lastMsg: Array[Byte] = byteBuffer.slice(0, readlen)
          if (lastMsg.length == 1 && lastMsg(0).asInstanceOf[Char] == message_separator) {

          }
          else {
            //println(">>>>>>>>>>>>>msg found:" + new String(lastMsg))
            currentMsgNum += 1
            val msgOffset = globalOffset + lastMsg.length + message_separator_len //byte offset of next message in the file
            val smartFileMessage = new SmartFileMessage(lastMsg, msgOffset, fileHandler, currentMsgNum, globalOffset)
            messageFoundCallback(smartFileMessage, consumerContext)
          }
        }


      }
      catch {
        case ioe: IOException => {
          logger.error("SMART FILE CONSUMER: Exception while accessing the file for processing " + fileName, ioe)
          sendFinishFlag(SmartFileConsumer.FILE_STATUS_CORRUPT)
          shutdownThreads
          return
        }
        case et: Throwable => {
          logger.error("SMART FILE CONSUMER: Throwable while accessing the file for processing " + fileName, et)
          sendFinishFlag(SmartFileConsumer.FILE_STATUS_CORRUPT)
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
      finally {
        val endTm = System.nanoTime
        val elapsedTm = endTm - fileProcessingStartTs

        if (processingInterrupted) {
          logger.debug("SMART FILE CONSUMER (FileMessageExtractor) - sending interrupting flag for file {}", fileName)
          logger.warn("SMART FILE CONSUMER - %s - finished reading file %s. Operation took %fms on Node %s, PartitionId %s. StartTime:%d, EndTime:%d.".format(
            adapterConfig.Name, fileName, elapsedTm / 1000000.0, consumerContext.nodeId, consumerContext.partitionId.toString, fileProcessingStartTs, endTm))
          sendFinishFlag(SmartFileConsumer.FILE_STATUS_ProcessingInterrupted)
        }
        else if (isFileCorrupted) {
          logger.warn("SMART FILE CONSUMER - %s - finished reading file %s. The file is corrupt, could only read %s bytes. Operation took %fms on Node %s, PartitionId %s. StartTime:%d, EndTime:%d.".format(
            adapterConfig.Name, fileName, totalReadLen, elapsedTm / 1000000.0, consumerContext.nodeId, consumerContext.partitionId.toString, fileProcessingStartTs, endTm))
          sendFinishFlag(SmartFileConsumer.FILE_STATUS_CORRUPT)
        }
        else {
          logger.warn("SMART FILE CONSUMER - %s - finished reading file %s. Operation took %fms on Node %s, PartitionId %s. StartTime:%d, EndTime:%d.".format(
            adapterConfig.Name, fileName, elapsedTm / 1000000.0, consumerContext.nodeId, consumerContext.partitionId.toString, fileProcessingStartTs, endTm))
          sendFinishFlag(SmartFileConsumer.FILE_STATUS_FINISHED)
        }

        shutdownThreads()
      }
    }
    catch {
      case e: Throwable => {
        logger.error("", e)
        sendFinishFlag(SmartFileConsumer.FILE_STATUS_CORRUPT)
        shutdownThreads
        return
      }
    }
  }


  private def shutdownThreads(): Unit = {
    finished = true

    try {
      if (fileHandler != null) fileHandler.close()

    } catch {
      case et: Throwable =>
    }

    logger.debug("File message Extractor - shutting down updatExecutor")
    MonitorUtils.shutdownAndAwaitTermination(updatExecutor, "file message extracting status updator", 1)

    logger.debug("File message Extractor - shutting down extractExecutor")
    MonitorUtils.shutdownAndAwaitTermination(extractExecutor, "file message extractor", 1)
  }

  private def extractMessages(chunk: Array[Byte], len: Int): Int = {
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
            //println(">>>>>>>>>>>>>msg found:" + new String(newMsg))

            val smartFileMessage = new SmartFileMessage(newMsg, msgOffset, fileHandler, currentMsgNum, globalOffset)
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

  var finishFlagSent = false
  val finishFlagSent_Lock = new Object()

  def sendFinishFlag(status: Int): Unit = {
    finished = true

    val data = fileHandler.getFullPath + "~" + currentMsgNum + "~" + System.nanoTime + "~done"
    logger.warn("Node {} before sending done status for file processing key={} , value={}",
      consumerContext.nodeId, consumerContext.statusUpdateCacheKey, data)
    consumerContext.envContext.saveConfigInClusterCache(consumerContext.statusUpdateCacheKey, data.getBytes)
    logger.warn("Node {} after sending done status for file processing key={} , value={}",
      consumerContext.nodeId, consumerContext.statusUpdateCacheKey, data)


    val savedStatusData = consumerContext.envContext.getConfigFromClusterCache(consumerContext.statusUpdateCacheKey)
    val statusDataStr: String = if (savedStatusData == null) null else new String(savedStatusData)
    logger.warn("Node {} checking saved done status for file processing key={} ,saved value={}",
      consumerContext.nodeId, consumerContext.statusUpdateCacheKey, statusDataStr)


    finishFlagSent_Lock.synchronized {
      if (!finishFlagSent) {
        if (finishCallback != null)
          finishCallback(fileHandler, consumerContext, status, getFileStats(status))
        finishFlagSent = true
      }
    }

  }
}
