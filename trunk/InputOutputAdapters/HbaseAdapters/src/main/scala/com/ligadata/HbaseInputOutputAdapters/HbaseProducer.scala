package com.ligadata.HbaseInputOutputAdapters

import java.util.Arrays

import com.ligadata.KamanjaBase.{ContainerInterface, NodeContext, TransactionContext}
import org.apache.logging.log4j.{LogManager, Logger}
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.Exceptions.{FatalAdapterException, KamanjaException}
import com.ligadata.HeartBeat.{MonitorComponentInfo, Monitorable}
import org.json4s.jackson.Serialization

import scala.collection.mutable.ArrayBuffer
import java.util.concurrent.{Future, TimeUnit}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

import scala.actors.threadpool.{ExecutorService, Executors, TimeUnit}
import java.util.concurrent.locks.ReentrantReadWriteLock

import com.ligadata.adapterconfiguration.{HbaseAdapterConfiguration, HbaseConstants}
import org.apache.hadoop.hbase.client.Connection

/**
  * Created by Yousef on 8/16/2016.
  */
object HbaseProducer extends OutputAdapterFactory {
  def CreateOutputAdapter(inputConfig: AdapterConfiguration, nodeContext: NodeContext): OutputAdapter = new HbaseProducer(inputConfig, nodeContext)
  val HB_PERIOD = 5000

  // Statistics Keys
  val ADAPTER_DESCRIPTION = "Hbase Producer Client"
  val SEND_MESSAGE_COUNT_KEY = "Messages Sent"
  val SEND_CALL_COUNT_KEY = "Send Call Count"
  val LAST_FAILURE_TIME = "Last_Failure"
  val LAST_RECOVERY_TIME = "Last_Recovery"
}

class HbaseProducer(val inputConfig: AdapterConfiguration, val nodeContext: NodeContext) extends OutputAdapter {

  private[this] val LOG = LogManager.getLogger(getClass);

  private var shutDown:Boolean = false
  private val nodeId = if(nodeContext==null || nodeContext.getEnvCtxt()==null) "1" else nodeContext.getEnvCtxt().getNodeId()
  private val FAIL_WAIT = 2000
  private var numOfRetries = 0
  private var MAX_RETRIES = 3
  private var startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var metrics: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map[String, Any]()
  metrics("MessagesProcessed") = new AtomicLong(0)
  private var isShutdown = false
  private var isHeartBeating = false
  private var isInError = false
  private var msgCount = 0
  val default_outstanding_messages = "2048"
  val max_outstanding_messages = default_outstanding_messages.toString.trim().toInt
  private var retryExecutor: ExecutorService = Executors.newFixedThreadPool(1)
  private var heartBeatThread: ExecutorService = Executors.newFixedThreadPool(1)
  val counterLock = new Object
  var tempContext = Thread.currentThread().getContextClassLoader
  Thread.currentThread().setContextClassLoader(null);
  private val adapterConfig = HbaseAdapterConfiguration.getAdapterConfig(inputConfig)
  Thread.currentThread().setContextClassLoader(tempContext);
  val hbaseutil: HbaseUtility = new HbaseUtility
  var producer: Connection = null
  hbaseutil.createConnection(adapterConfig)
  hbaseutil.initilizeVariable(adapterConfig)
  producer = hbaseutil.getConnection()
  hbaseutil.setConnection(producer)
  hbaseutil.initilizeVariable(adapterConfig)
  var transId: Long = 0
  val key = Category + "/" + adapterConfig.Name + "/evtCnt"
  val randomPartitionCntr = new java.util.Random
  var partitionsGetTm = System.currentTimeMillis
  val refreshPartitionTime = 60 * 1000 // 60 secs
  var timePartition = System.currentTimeMillis()
  var transService = new com.ligadata.transactions.SimpleTransService
  transService.init(1)
  transId = transService.getNextTransId // Getting first transaction. It may get wasted if we don't have any lines to save.


  case class MsgDataRecievedCnt(cntrToOrder: Long, msg: Array[(Array[Byte], Array[Byte])])

  val partitionsMap = new ConcurrentHashMap[Int, ConcurrentHashMap[Long, MsgDataRecievedCnt]](128);
  val failedMsgsMap = new ConcurrentHashMap[Int, ConcurrentHashMap[Long, MsgDataRecievedCnt]](128); // We just need Array Buffer as Innser value. But the only issue is we need to make sure we handle it for multiple threads.

  override def send(messages: Array[Array[Byte]], partitionKeys: Array[Array[Byte]]): Unit = {
    if (!isHeartBeating) runHeartBeat

    // Refreshing Partitions for every refreshPartitionTime.
    // BUGBUG:: This may execute multiple times from multiple threads. For now it does not hard too much.
    if ((System.currentTimeMillis - partitionsGetTm) > refreshPartitionTime) {
      //topicPartitionsCount = producer.partitionsFor(qc.topic).size()
      partitionsGetTm = System.currentTimeMillis
    }

    try {
      var partitionsMsgMap = scala.collection.mutable.Map[Int, ArrayBuffer[MsgDataRecievedCnt]]();
      timePartition = System.currentTimeMillis()
      transId = transService.getNextTransId

      for (i <- 0 until messages.size) {
      /////////////////////  hbaseutil.put("","","")
//        val partId = getPartition(partitionKeys(i), topicPartitionsCount)
//        var ab = partitionsMsgMap.getOrElse(partId, null)
//        if (ab == null) {
//          ab = new ArrayBuffer[MsgDataRecievedCnt](256)
//          partitionsMsgMap(partId) = ab
//        }
//        val pr = new ProducerRecord(qc.topic, partId, partitionKeys(i), messages(i))
//        ab += MsgDataRecievedCnt(msgInOrder.getAndIncrement, pr)
      }

      var outstandingMsgs = outstandingMsgCount
      // LOG.debug("KAFKA PRODUCER: current outstanding messages for topic %s are %d".format(qc.topic, outstandingMsgs))

      var osRetryCount = 0
      var osWaitTm = 5000
      while (outstandingMsgs > max_outstanding_messages) {
        LOG.warn(adapterConfig.Name + " Hbase PRODUCER: %d outstanding messages in queue to write. Waiting for them to flush before we write new messages. Retrying after %dms. Retry count:%d".format(outstandingMsgs, osWaitTm, osRetryCount))
        try {
          Thread.sleep(osWaitTm)
        } catch {
          case e: Exception => {
            externalizeExceptionEvent(e)
            throw e
          }
          case e: Throwable => {
            externalizeExceptionEvent(e)
            throw e
          }
        }
        outstandingMsgs = outstandingMsgCount
      }

      partitionsMsgMap.foreach(partIdAndRecs => {
        val partId = partIdAndRecs._1
        val keyMessages = partIdAndRecs._2

        // first push all messages to partitionsMap before we really send. So that callback is guaranteed to find the message in partitionsMap
        addMsgsToMap(partId, keyMessages)
        sendInfinitely(keyMessages, false)
      })

    } catch {
      case e: java.lang.InterruptedException => {
        // Not doing anythign for now
        LOG.warn(adapterConfig.Name + " Hbase PRODUCER: Got java.lang.InterruptedException. isShutdown:" + isShutdown)
      }
      case fae: FatalAdapterException => {
        externalizeExceptionEvent(fae)
        throw fae
      }
      case e: Exception               => {
        externalizeExceptionEvent(e)
        throw FatalAdapterException("Unknown exception", e)
      }
      case e: Throwable               => {
        externalizeExceptionEvent(e)
        throw FatalAdapterException("Unknown exception", e)
      }
    }
  }

  override def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): Unit = {
    if (outputContainers.size == 0) return

    // Sanity checks
    if (isShutdown) {
      val szMsg = adapterConfig.Name + " KAFKA PRODUCER: Producer is not available for processing"
      LOG.error(szMsg)
      throw new Exception(szMsg)
    }

    val (outContainers, serializedContainerData, serializerNames) = serialize(tnxCtxt, outputContainers)

    if (outContainers.size != serializedContainerData.size || outContainers.size != serializerNames.size) {
      val szMsg = adapterConfig.Name + " Hbase PRODUCER: Messages, messages serialized data & serializer names should has same number of elements. Messages:%d, Messages Serialized data:%d, serializerNames:%d".format(outContainers.size, serializedContainerData.size, serializerNames.size)
      LOG.error(szMsg)
      throw new Exception(szMsg)
    }

    if (serializedContainerData.size == 0) return

    val partitionKeys = ArrayBuffer[Array[Byte]]()

    for (i <- 0 until serializedContainerData.size) {
      partitionKeys += outContainers(i).getPartitionKey.mkString(",").getBytes()
    }

    send(serializedContainerData, partitionKeys.toArray)
  }

  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    implicit val formats = org.json4s.DefaultFormats
    return new MonitorComponentInfo( AdapterConfiguration.TYPE_OUTPUT, adapterConfig.Name, HbaseProducer.ADAPTER_DESCRIPTION, startTime, lastSeen,  Serialization.write(metrics).toString)
  }
  /**
    * this is a very simple string to be externalized on a Status timer for the adapter.
    *
    * @return String
    */
  override def getComponentSimpleStats: String = {
    return key + "->" + msgCount
  }

  override def Shutdown(): Unit = {

    LOG.info(/*qc.Name + */" Shutdown detected")

    // Shutdown HB
    isShutdown = true

    heartBeatThread.shutdownNow
    while (!heartBeatThread.isTerminated) {
      try {
        Thread.sleep(100)
      } catch {
        case e: Exception => {
          // Don't do anything, because it is shutting down
        }
        case e: Throwable => {
          // Don't do anything, because it is shutting down
        }
      }
    }

    // First shutdown retry executor
    if (retryExecutor != null) {
      retryExecutor.shutdownNow
      while (!retryExecutor.isTerminated) {
        try {
          Thread.sleep(100)
        } catch {
          case e: Exception => {
            // Don't do anything, because it is shutting down
          }
          case e: Throwable => {
            // Don't do anything, because it is shutting down
          }
        }
      }
    }

    if (producer != null)
      producer.close
  }

  private def updateMetricValue(key: String, value: Any): Unit = {
    counterLock.synchronized {
      if (key.equalsIgnoreCase(HbaseProducer.LAST_FAILURE_TIME) ||
        key.equalsIgnoreCase(HbaseProducer.LAST_RECOVERY_TIME)) {
        metrics(key) = value.toString
      } else {
        // This is an aggregated Long value
        val cur = metrics.getOrElse(key,"0").toString
        val longCur = cur.toLong
        metrics(key) = longCur + value.toString.toLong
      }
    }
  }

  private def runHeartBeat: Unit = {
    heartBeatThread.execute(new Runnable() {
      override def run(): Unit = {
        try {
          isHeartBeating = true
          while (!isShutdown) {
            if (!isInError) {
              lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
            }
            Thread.sleep(HbaseProducer.HB_PERIOD)
          }
          isHeartBeating = false
        } catch {
          case e: Exception => {
            externalizeExceptionEvent(e)
            isHeartBeating = false
            if (isShutdown == false)
              LOG.warn(/*HbaseProducer.Name + */" Heartbeat Interrupt detected", e)
          }
        }
        LOG.info(adapterConfig.Name + " Heartbeat is shutting down")
        LOG.info(adapterConfig.Name + " Heartbeat is shutting down")
      }
    })
  }

  private def getPartition(key: Array[Byte], numPartitions: Int): Int = {
    if (numPartitions == 0) return 0
    if (key != null) {
      try {
        return (scala.math.abs(Arrays.hashCode(key)) % numPartitions)
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          throw e
        }
        case e: Throwable => {
          externalizeExceptionEvent(e)
          throw e
        }
      }
    }
    return randomPartitionCntr.nextInt(numPartitions)
  }

  private def outstandingMsgCount: Int = {
    var outstandingMsgs = 0
    val allPartitions = partitionsMap.elements()
    while (allPartitions.hasMoreElements()) {
      val nxt = allPartitions.nextElement();
      outstandingMsgs += nxt.size()
    }
    outstandingMsgs
  }

  private def addMsgsToMap(partId: Int, keyMessages: ArrayBuffer[MsgDataRecievedCnt]): Unit = {
    var msgMap = partitionsMap.get(partId)
    if (msgMap == null) {
      partitionsMap.synchronized {
        msgMap = partitionsMap.get(partId)
        if (msgMap == null) {
          val tmpMsgMap = new ConcurrentHashMap[Long, MsgDataRecievedCnt](1024);
          partitionsMap.put(partId, tmpMsgMap)
          msgMap = tmpMsgMap
        }
      }
    }

    if (msgMap != null) {
      try {
        val allKeys = new java.util.HashMap[Long, MsgDataRecievedCnt]()
        keyMessages.foreach(m => {
          allKeys.put(m.cntrToOrder, m)
        })
        msgMap.putAll(allKeys)
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          // Failed to insert into Map
          throw e
        }
      }
    }
  }

  private def failedMsgCount: Int = {
    var failedMsgs = 0

    val allFailedPartitions = failedMsgsMap.elements()
    while (allFailedPartitions.hasMoreElements()) {
      val nxt = allFailedPartitions.nextElement();
      failedMsgs += nxt.size()
    }
    failedMsgs
  }

  class RetryFailedMessages extends Runnable {
    def run() {
      val statusPrintTm = 60000 // for every 1 min
      var nextPrintTimeCheck = System.currentTimeMillis + statusPrintTm
      while (isShutdown == false) {
        try {
          Thread.sleep(5000) // Sleeping for 5Sec
        } catch {
          case e: Exception => {
            externalizeExceptionEvent(e)
            if (! isShutdown) LOG.warn("", e)
          }
          case e: Throwable => {
            externalizeExceptionEvent(e)
            if (! isShutdown) LOG.warn("", e)
          }
        }
        if (isShutdown == false) {
          var outstandingMsgs = outstandingMsgCount
          var allFailedMsgs = failedMsgCount
          if (outstandingMsgs > 0 || allFailedMsgs > 0 || nextPrintTimeCheck < System.currentTimeMillis) {
       //     LOG.warn("KAFKA PRODUCER: Topic: %s - current outstanding messages:%d & failed messages:%d".format(adapterConfig.topic, outstandingMsgs, allFailedMsgs))
            nextPrintTimeCheck = System.currentTimeMillis + statusPrintTm
          }
          // Get all failed records and resend for each partitions
          val keysIt = failedMsgsMap.keySet().iterator()

          while (keysIt.hasNext() && isShutdown == false) {
            val partId = keysIt.next();

            val failedMsgs = failedMsgsMap.get(partId)
            val sz = failedMsgs.size()
            if (sz > 0) {
              val keyMessages = new ArrayBuffer[MsgDataRecievedCnt](sz)

              val allmsgsit = failedMsgs.entrySet().iterator()
              while (allmsgsit.hasNext() && isShutdown == false) {
                val ent = allmsgsit.next();
                keyMessages += ent.getValue
              }
              if (isShutdown == false) {
                val km = keyMessages.sortWith(_.cntrToOrder < _.cntrToOrder) // Sending in the same order as inserted before.
                sendInfinitely(km, true)
              }
            }
          }
        }
      }
    }
  }

  private def sendInfinitely(keyMessages: ArrayBuffer[MsgDataRecievedCnt], removeFromFailedMap: Boolean): Unit = {
    var sendStatus = HbaseConstants.HBASE_NOT_SEND
    var retryCount = 0
    var waitTm = 15000

    // We keep on retry until we succeed on this thread
    while (sendStatus != HbaseConstants.HBASE_SEND_SUCCESS && isShutdown == false) {
      try {
   //     sendStatus = doSend(keyMessages, removeFromFailedMap)   //check this
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          LOG.error(adapterConfig.Name + " KAFKA PRODUCER: Error sending to kafka, Retrying after %dms. Retry count:%d".format(waitTm, retryCount), e)
          try {
            Thread.sleep(waitTm)
          } catch {
            case e: Exception =>  {
              externalizeExceptionEvent(e)
              throw e
            }
            case e: Throwable => {
              externalizeExceptionEvent(e)
              throw e
            }
          }
          if (waitTm < 60000) {
            waitTm = waitTm * 2
            if (waitTm > 60000)
              waitTm = 60000
          }
        }
      }
    }
  }

//  private def doSend(keyMessages: ArrayBuffer[MsgDataRecievedCnt], removeFromFailedMap: Boolean): Int = {
//    var sentMsgsCntr = 0
//    var lastAccessRec: MsgDataRecievedCnt = null
//    try {
//      updateMetricValue(HbaseProducer.SEND_MESSAGE_COUNT_KEY,keyMessages.size)
//      updateMetricValue(HbaseProducer.SEND_CALL_COUNT_KEY,1)
//
//      // We already populated partitionsMap before we really send. So that callback is guaranteed to find the message in partitionsMap
//      keyMessages.map(msgAndCntr => {
//        if (isShutdown)
//          throw new Exception(adapterConfig.Name + " is shutting down")
//        lastAccessRec = msgAndCntr
//        if (removeFromFailedMap)
//          removeMsgFromFailedMap(lastAccessRec)
//        // Send the request to Kafka
//        producer.send(msgAndCntr.msg, new Callback {
//          override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
//            val localMsgAndCntr = msgAndCntr
//            msgCount += 1
//            if (exception != null) {
//              LOG.warn(adapterConfig.Name + " Failed to send message into " + localMsgAndCntr.msg.topic, exception)
//              addToFailedMap(localMsgAndCntr)
//              if (!isInError) updateMetricValue(HbaseProducer.LAST_FAILURE_TIME, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis)))
//              isInError = true
//            } else {
//              // Succeed - also click the heartbeat here... just to be more accurate.
//              if (isInError) updateMetricValue(HbaseProducer.LAST_RECOVERY_TIME, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis)))
//              lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
//              removeMsgFromMap(localMsgAndCntr)
//              isInError = false
//
//            }
//          }
//        })
//        lastAccessRec = null
//        sentMsgsCntr += 1
//      })
//
//      keyMessages.clear()
//    } catch {
//      case e: Exception  => {
//        externalizeExceptionEvent(e)
//        if (sentMsgsCntr > 0) keyMessages.remove(0, sentMsgsCntr)
//        addBackFailedToSendRec(lastAccessRec)
//        LOG.warn(adapterConfig.Name + " unknown exception encountered ", e)
//        throw new FatalAdapterException("Unknown exception", e)
//      }
//      case e: Throwable => {
//        externalizeExceptionEvent(e)
//        if (sentMsgsCntr > 0) keyMessages.remove(0, sentMsgsCntr)
//        addBackFailedToSendRec(lastAccessRec)
//        LOG.warn(adapterConfig.Name + " unknown exception encountered ", e)
//        throw new FatalAdapterException("Unknown exception", e) }
//    }
//    return HbaseConstants.HBASE_SEND_SUCCESS
//  }

  private def addBackFailedToSendRec(lastAccessRec: MsgDataRecievedCnt): Unit = {
    if (lastAccessRec != null)
      addToFailedMap(lastAccessRec)
  }

  private def addToFailedMap(msgAndCntr: MsgDataRecievedCnt): Unit = {
//    if (msgAndCntr == null) return
//    val partId = msgAndCntr.msg.partition()
//    var msgMap = failedMsgsMap.get(partId)
//    if (msgMap == null) {
//      failedMsgsMap.synchronized {
//        msgMap = failedMsgsMap.get(partId)
//        if (msgMap == null) {
//          val tmpMsgMap = new ConcurrentHashMap[Long, MsgDataRecievedCnt](1024);
//          failedMsgsMap.put(partId, tmpMsgMap)
//          msgMap = tmpMsgMap
//        }
//      }
//    }
//
//    if (msgMap != null) {
//      try {
//        msgMap.put(msgAndCntr.cntrToOrder, msgAndCntr)
//      } catch {
//        case e: Exception => {
//          externalizeExceptionEvent(e)
//          // Failed to insert into Map
//          throw e
//        }
//      }
//    }
  } //check this

  private def removeMsgFromFailedMap(msgAndCntr: MsgDataRecievedCnt): Unit = {
//     if (msgAndCntr == null) return
//     val partId = msgAndCntr.msg.partition()
//     val msgMap = failedMsgsMap.get(partId)
//     if (msgMap != null) {
//       try {
//         msgMap.remove(msgAndCntr.cntrToOrder)
//       } catch {
//         case e: Exception => {
//           externalizeExceptionEvent(e)
//           LOG.warn("", e)
//         }
//         case e: Throwable => {
//           externalizeExceptionEvent(e)
//           LOG.warn("", e)
//         }
//       }
//     }
   }    ///check this

  private def removeMsgFromMap(msgAndCntr: MsgDataRecievedCnt): Unit = {
//    if (msgAndCntr == null) return
//    val partId = msgAndCntr.msg.partition()
//    val msgMap = partitionsMap.get(partId)
//    if (msgMap != null) {
//      try {
//        msgMap.remove(msgAndCntr.cntrToOrder) // This must present. Because we are adding the records into partitionsMap before we send messages. If it does not present we simply ignore it.
//      } catch {
//        case e: Exception => {
//          externalizeExceptionEvent(e)
//          LOG.warn("", e)
//        }
//        case e: Throwable => {
//          externalizeExceptionEvent(e)
//          LOG.warn("", e)
//        }
//      }
//    }
  }  ///check this

}