package com.ligadata.OutputAdapters

import java.util.Arrays

import com.ligadata.KamanjaBase.{ContainerInterface, NodeContext, TransactionContext}
import org.apache.logging.log4j.{LogManager, Logger}
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.Exceptions.{FatalAdapterException, InvalidArgumentException, KamanjaException}
import com.ligadata.HeartBeat.{MonitorComponentInfo, Monitorable}
import org.json4s.jackson.Serialization
import com.ligadata.AdaptersConfiguration.{DbAdapterConfiguration, JDBCConstants, LogTrait}
import scala.collection.mutable.ArrayBuffer
import java.util.concurrent.{Future, TimeUnit}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import scala.actors.threadpool.{ExecutorService, Executors, TimeUnit}
import java.util.concurrent.locks.ReentrantReadWriteLock

import com.ligadata.KvBase.{Key, Value}
import com.ligadata.Utils.KamanjaLoaderInfo
import org.apache.commons.dbcp2.BasicDataSource

/**
  * Created by Yousef on 8/28/2016.
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
  private val adapterConfig = DbAdapterConfiguration.getAdapterConfig(inputConfig)
  Thread.currentThread().setContextClassLoader(tempContext);
  //////////val hbaseutil: HbaseUtility = new HbaseUtility
  private val kvManagerLoader = new KamanjaLoaderInfo
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

  //Create a DBCP based Connection Pool Here
  val dataSource = new BasicDataSource

  //Force a load of the DB Driver Class
  Class.forName(adapterConfig.dbDriver)
  LOG.debug("Loaded the DB Driver..."+adapterConfig.dbDriver)

  dataSource.setDriverClassName(adapterConfig.dbDriver)
  if(adapterConfig.dbName!=null && !adapterConfig.dbName.isEmpty())
    dataSource.setUrl(adapterConfig.dbURL+"/"+adapterConfig.dbName)
  else
    dataSource.setUrl(adapterConfig.dbURL)
  dataSource.setUsername(adapterConfig.dbUser);
  dataSource.setPassword(adapterConfig.dbPwd);


  dataSource.setTestWhileIdle(false);
  dataSource.setTestOnBorrow(true);
  dataSource.setValidationQuery("Select 1");
  dataSource.setTestOnReturn(false);

  dataSource.setMaxTotal(100);
  dataSource.setMaxIdle(5);
  dataSource.setMinIdle(0);
  dataSource.setInitialSize(5);
  dataSource.setMaxWaitMillis(5000);
  override def send(messages: Array[Array[Byte]], partitionKeys: Array[Array[Byte]]): Unit = {
  } ///////not implemented yet

  override def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): Unit = {
    if (outputContainers.size == 0) return

    val dt = System.currentTimeMillis
    lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(dt))
    // Sanity checks
    if (isShutdown) {
      val szMsg = adapterConfig.Name + " Hbase PRODUCER: Producer is not available for processing"
      LOG.error(szMsg)
      throw new Exception(szMsg)
    }

    for (i <- 0 until outputContainers.size) {
      metrics("MessagesProcessed").asInstanceOf[AtomicLong].incrementAndGet()
    }

    val data_list = outputContainers.groupBy(_.getFullTypeName.toLowerCase).map(oneContainerData => {
      (oneContainerData._1, oneContainerData._2.map(container => {
        (Key(container.TimePartitionData(), container.PartitionKeyData(), 0, 0), "", container.asInstanceOf[Any])
      }))
    }).toArray

    if (data_list == null)
      throw new InvalidArgumentException("Data should not be null", null)

    val putData = data_list.map(oneContainerData => {
      val containerData: Array[(com.ligadata.KvBase.Key, com.ligadata.KvBase.Value)] = oneContainerData._2.map(row => {
        if (row._3.isInstanceOf[ContainerInterface]) {
          val cont = row._3.asInstanceOf[ContainerInterface]
          val arrayOfCont = Array(cont)
          val (containers, serData, serializers) = serialize(tnxCtxt, arrayOfCont)
          // println("++container fullname++" + cont.getFullTypeName)
          if (containers == null || containers.size == 0) {
            (null, Value(0, null, null))
          } else {
            (row._1, Value(cont.getSchemaId, serializers(0), serData(0)))
          }
        } else {
          (row._1, Value(0, row._2, row._3.asInstanceOf[Array[Byte]]))
        }
      }).filter(row => row._1 != null || row._2.serializedInfo != null || row._2.serializerType != null || row._2.schemaId != 0)
      (oneContainerData._1, containerData)
    }).filter(oneContainerData => oneContainerData._2.size > 0)


//    if (putData.size > 0)
//      dataStore.put(putData)  you should create put command
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
    return getAdapterName + "->" + metrics("MessagesProcessed").asInstanceOf[AtomicLong].get
  }

  override def Shutdown(): Unit = {

    LOG.info(adapterConfig.Name +" Shutdown detected")

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

    if (dataSource != null)
      dataSource.close()
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
              LOG.warn(adapterConfig.Name + " Heartbeat Interrupt detected", e)
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
    var sendStatus = JDBCConstants.JDBC_NOT_SEND
    var retryCount = 0
    var waitTm = 15000

    // We keep on retry until we succeed on this thread
    while (sendStatus != JDBCConstants.JDBC_SEND_SUCCESS && isShutdown == false) {
      try {
        //     sendStatus = doSend(keyMessages, removeFromFailedMap)   //check this
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          LOG.error(adapterConfig.Name + " Hbase PRODUCER: Error sending to kafka, Retrying after %dms. Retry count:%d".format(waitTm, retryCount), e)
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
}