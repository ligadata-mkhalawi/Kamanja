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
import  com.ligadata.Utilities.JDBCUtility
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

//  //Create a DBCP based Connection Pool Here
//  val dataSource = new BasicDataSource
//
//  //Force a load of the DB Driver Class
//  Class.forName(adapterConfig.dbDriver)
//  LOG.debug("Loaded the DB Driver..."+adapterConfig.dbDriver)
//
//  dataSource.setDriverClassName(adapterConfig.dbDriver)
//  if(adapterConfig.dbName!=null && !adapterConfig.dbName.isEmpty())
//    dataSource.setUrl(adapterConfig.dbURL+"/"+adapterConfig.dbName)
//  else
//    dataSource.setUrl(adapterConfig.dbURL)
//  dataSource.setUsername(adapterConfig.dbUser);
//  dataSource.setPassword(adapterConfig.dbPwd);
//
//
//  dataSource.setTestWhileIdle(false);
//  dataSource.setTestOnBorrow(true);
//  dataSource.setValidationQuery("Select 1");
//  dataSource.setTestOnReturn(false);
//
//  dataSource.setMaxTotal(100);
//  dataSource.setMaxIdle(5);
//  dataSource.setMinIdle(0);
//  dataSource.setInitialSize(5);
//  dataSource.setMaxWaitMillis(5000);

  private val connector = new JDBCUtility
  connector.createConnection(adapterConfig)

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


   if (putData.size > 0)
     connector.put(putData)  //you should create put command
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

    if (connector != null)
      connector.shutDown()
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
}