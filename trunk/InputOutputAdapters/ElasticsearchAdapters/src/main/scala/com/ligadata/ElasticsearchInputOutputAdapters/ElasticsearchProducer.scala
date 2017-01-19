package com.ligadata.ElasticsearchInputOutputAdapters

import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.{Arrays, Calendar}

import org.json4s._
import org.json4s.jackson.JsonMethods._
import com.ligadata.AdapterConfiguration.{ElasticsearchAdapterConfiguration, ElasticsearchConstants}
import com.ligadata.HeartBeat.MonitorComponentInfo
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.KamanjaBase.{ContainerInterface, NodeContext, TransactionContext}
import com.ligadata.Utils.KamanjaLoaderInfo
import com.ligadata.keyvaluestore.ElasticsearchAdapter
import org.apache.logging.log4j.LogManager
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.VersionType
import org.json4s.jackson.Serialization

import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.ArrayBuffer
import org.json.JSONObject;
import org.elasticsearch.shield.ShieldPlugin


object ElasticsearchProducer extends OutputAdapterFactory {
  def CreateOutputAdapter(inputConfig: AdapterConfiguration, nodeContext: NodeContext): OutputAdapter = new ElasticsearchProducer(inputConfig, nodeContext)

  val HB_PERIOD = 5000

  // Statistics Keys
  val ADAPTER_DESCRIPTION = "Elasticsearch Producer Client"
  val SEND_MESSAGE_COUNT_KEY = "Messages Sent"
  val SEND_CALL_COUNT_KEY = "Send Call Count"
  val LAST_FAILURE_TIME = "Last_Failure"
  val LAST_RECOVERY_TIME = "Last_Recovery"
}

class ElasticsearchProducer(val inputConfig: AdapterConfiguration, val nodeContext: NodeContext) extends OutputAdapter {

  private[this] val LOG = LogManager.getLogger(getClass);

  private var shutDown: Boolean = false
  private val nodeId = if (nodeContext == null || nodeContext.getEnvCtxt() == null) "1" else nodeContext.getEnvCtxt().getNodeId()
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
  private val adapterConfig = ElasticsearchAdapterConfiguration.getAdapterConfig(inputConfig, "output")
  Thread.currentThread().setContextClassLoader(tempContext);
  val elaticsearchutil: ElasticsearchUtility = new ElasticsearchUtility
  private val kvManagerLoader = new KamanjaLoaderInfo
  //  var producer: Connection = null
  //  hbaseutil.createConnection(adapterConfig)
  //  hbaseutil.initilizeVariable(adapterConfig)
  //  producer = hbaseutil.getConnection()
  //  hbaseutil.setConnection(producer)
  //  hbaseutil.initilizeVariable(adapterConfig)
  var transId: Long = 0
  val key = Category + "/" + adapterConfig.Name + "/evtCnt"
  val randomPartitionCntr = new java.util.Random
  var partitionsGetTm = System.currentTimeMillis
  val refreshPartitionTime = 60 * 1000
  // 60 secs
  var timePartition = System.currentTimeMillis()
  var transService = new com.ligadata.transactions.SimpleTransService
  val sendData = scala.collection.mutable.Map[String, ArrayBuffer[String]]()
  var recsToWrite = 0
  // For now hard coded to 60secs
  val timeToWriteRecs = 60000
  val writeRecsBatch = 1000
  var nextWrite = System.currentTimeMillis + timeToWriteRecs

  transService.init(1)
  transId = transService.getNextTransId

  // Getting first transaction. It may get wasted if we don't have any lines to save.


  case class MsgDataRecievedCnt(cntrToOrder: Long, msg: Array[(Array[Byte], Array[Byte])])

  val partitionsMap = new ConcurrentHashMap[Int, ConcurrentHashMap[Long, MsgDataRecievedCnt]](128);
  val failedMsgsMap = new ConcurrentHashMap[Int, ConcurrentHashMap[Long, MsgDataRecievedCnt]](128);
  // We just need Array Buffer as Innser value. But the only issue is we need to make sure we handle it for multiple threads.

  var dataStoreInfo = elaticsearchutil.createDataStorageInfo(adapterConfig)
  var dataStore = elaticsearchutil.GetDataStoreHandle(dataStoreInfo).asInstanceOf[ElasticsearchAdapter]

  override def send(messages: Array[Array[Byte]], partitionKeys: Array[Array[Byte]]): Unit = {
    throw new Exception("send with data is not yet implemented")
  }

  private def addData(data: Map[String, Array[String]]): Unit = synchronized {
    data.foreach(kv => {
      val existingData = sendData.getOrElse(kv._1, null)
      if (existingData != null) {
        existingData ++= kv._2
      } else {
        val ab = ArrayBuffer[String]()
        ab ++= kv._2
        sendData(kv._1) = ab
      }
      recsToWrite += kv._2.size
    })
  }

  override def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): Unit = {
    if (outputContainers.size == 0) return

    val dt = System.currentTimeMillis
    var indexName = adapterConfig.TableName
    lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(dt))

    this.synchronized {
      if (dt > nextWrite || recsToWrite > writeRecsBatch) {
        LOG.warn("WriteTest=>Going to write records now. Current time:%d, next write time:%d, current records to write:%d, batch size:%d".format(dt, nextWrite, recsToWrite, writeRecsBatch))
        putJsonsWithMetadata(true)
        nextWrite = System.currentTimeMillis + timeToWriteRecs
      } else {
        LOG.warn("WriteTest=>Not yet writing. Current time:%d, next write time:%d, current records to write:%d, batch size:%d".format(dt, nextWrite, recsToWrite, writeRecsBatch))
      }
    }

    // Sanity checks
    if (isShutdown) {
      val szMsg = adapterConfig.Name + " Elasticsearch Adapter: Adapter is not available for processing"
      LOG.error(szMsg)
      throw new Exception(szMsg)
    }
    val (outContainers, serializedContainerData, serializerNames) = serialize(tnxCtxt, outputContainers)

    // BUGBUG::Just make sure all serializerNames must be JSONs. For now we support only JSON output here.

    // check if we need the indexName to be change according to the current date
    if (adapterConfig.rollIndexNameByCurrentDate) {
      val dateFormat: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
      val currentDate = dateFormat.format(Calendar.getInstance().getTime())
      indexName = indexName + "-" + currentDate
    }

    val strDataRecords = serializedContainerData.map(data => new String(data))

    var addedData = false
    if (adapterConfig.rollIndexNameByDataDate) {
      if (adapterConfig.dateFiledNameInOutputMessage.isEmpty) {
        LOG.error("Elasticsearch OutputAdapter : dateFiledNameInOutputMessage filed is empty")
      } else {
        val indexBaseName = indexName
        // BUGBUG:: Why are we using jsonData even though we have outContainers corresponding to them. The is very less expensive
        val idxNameAndData = strDataRecords.map(jsonData => {
          var idxName = indexBaseName + "-unknown"
          try {
            val jsonObj: JSONObject = new JSONObject(jsonData)
            // assuming format is yyyy-MM-dd'T'hh:mm'Z'
            val dateFiled: String = jsonObj.getString(adapterConfig.dateFiledNameInOutputMessage)
            val dateFormatString: String = adapterConfig.dateFiledFormat
            val sourceDateFormat: SimpleDateFormat = new SimpleDateFormat(dateFormatString)
            val targetDateFormat: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
            val targetDate: String = targetDateFormat.format(sourceDateFormat.parse(dateFiled))
            idxName = indexBaseName + "-" + targetDate
          } catch {
            case e: Exception => LOG.error("Elasticsearch output adapter : error while retrieving date field from output message - ", e)
          }
          ((idxName, jsonData))
        })
        val map = idxNameAndData.groupBy(idxAndData => idxAndData._1).map(kv => (kv._1, kv._2.map(idxData => idxData._2)))
        addData(map)
        LOG.warn("WriteTest=>Added %d records to cached data in %d indices".format(strDataRecords.size, map.size))
        addedData = true
      }
    }

    if (!addedData) {
      val map = Map[String, Array[String]](indexName -> strDataRecords)
      addData(map)
      LOG.warn("WriteTest=>Added %d records to cached data in %d indices".format(strDataRecords.size, map.size))
    }
  }

  private def canConsiderShutdown(considerShutdown: Boolean): Boolean = {
    isShutdown && considerShutdown
  }

  private def putJsonsWithMetadata(considerShutdown: Boolean): Unit = synchronized {
    if (sendData.size == 0) return
    //   containerName: String, data_list: Array[String]
    var client: TransportClient = null
    var connectedTime = 0L
    //    CheckTableExists(tableName)
    try {
      client = getConnection
      connectedTime = System.currentTimeMillis

      var exec = true
      var curWaitTm = 5000

      // BUGBUG:: Do we need to do every time?
      // check if we need to cteate the indexMapping beforehand.
      if (adapterConfig.manuallyCreateIndexMapping && adapterConfig.indexMapping.length > 0) {
        val allKeys = sendData.map(kv => kv._1).toArray
        LOG.warn("WriteTest=>Validating whether indices {%s} exists or not".format(allKeys.mkString(",")))
        exec = true
        curWaitTm = 5000
        while (!canConsiderShutdown(considerShutdown) && exec) {
          exec = false
          try {
            if (client == null) {
              client = getConnection
              connectedTime = System.currentTimeMillis
            }
            dataStore.createIndexForOutputAdapter(client, allKeys, adapterConfig.indexMapping, true)
          } catch {
            case e: Throwable => {
              if ((System.currentTimeMillis - connectedTime) > 10000) {
                try {
                  if (client != null)
                    client.close
                } catch {
                  case e: Throwable => {
                    LOG.error("Failed to close connection", e)
                  }
                }
                client = null
                connectedTime = 0L
              }
              exec = true
              LOG.error("Failed to create indices. Going to retry after %dms".format(curWaitTm), e)
              val nSecs = curWaitTm / 1000
              for (i <- 0 until nSecs) {
                try {
                  if (!canConsiderShutdown(considerShutdown))
                    Thread.sleep(1000)
                } catch {
                  case e: Throwable => {}
                }
              }
              curWaitTm = curWaitTm * 2
              if (curWaitTm > 60000)
                curWaitTm = 60000
            }
          }
        }
      }

      LOG.warn("WriteTest=>About to write %d records".format(recsToWrite))
      exec = true
      curWaitTm = 5000
      while (!canConsiderShutdown(considerShutdown) && exec) {
        exec = false
        try {
          if (client == null) {
            client = getConnection
            connectedTime = System.currentTimeMillis
          }
          var gotException: Throwable = null
          var addedKeys = ArrayBuffer[String]()
          sendData.foreach(kv => {
            if (gotException == null) {
              val containerName = kv._1
              val data_list = kv._2
              try {
                val tableName = toFullTableName(containerName)
                var bulkRequest = client.prepareBulk()
                data_list.foreach({ jsonData =>
                  //added by saleh 15/12/2016
                  val root = parse(jsonData).values.asInstanceOf[Map[String, String]]
                  //if (root.get("metadata") != None) {
                  val metadata = if (root.get("metadata") == None) Map[String, Any]() else root.get("metadata").get.asInstanceOf[Map[String, Any]]

                  val index = if (metadata.get("index") == None || metadata.get("index").get.toString.trim.length == 0) tableName else metadata.get("index").get.toString
                  val metadata_type = if (metadata.get("_type") == None || metadata.get("_type").get.toString.trim.length == 0) "type1" else metadata.get("_type").get.toString

                  val bulk = client.prepareIndex(index, metadata_type)

                  if (metadata.get("id") != None && metadata.get("id").get.toString.trim.length > 0) bulk.setId(metadata.get("id").get.toString)
                  if (metadata.get("version") != None) bulk.setVersionType(VersionType.FORCE).setVersion(metadata.get("version").get.asInstanceOf[BigInt].toLong)

                  //}
                  bulk.setSource(jsonData)
                  bulkRequest.add(bulk)
                })
                LOG.debug("Executing bulk indexing...")
                val bulkResponse = bulkRequest.execute().actionGet()

                // BUGBUG:: If we have errors do we treat this data is added ???????
                //added by saleh 15/12/2016
                if (bulkResponse.hasFailures) {
                  LOG.warn(bulkResponse.buildFailureMessage())
                }
                recsToWrite -= kv._2.size
                kv._2.clear
                addedKeys += containerName
              } catch {
                case e: Throwable => {
                  LOG.error("Failed to add data to index:" + containerName, e)
                  gotException = e
                }
              }
            }
          })
          addedKeys.foreach(key => {
            sendData.remove(key)
          })
          if (gotException != null) {
            throw gotException
          }
          // No Exceptions and everything is written
          if (sendData.size == 0)
            recsToWrite = 0
        } catch {
          case e: Throwable => {
            if ((System.currentTimeMillis - connectedTime) > 10000) {
              try {
                if (client != null)
                  client.close
              } catch {
                case e: Throwable => {
                  LOG.error("Failed to close connection", e)
                }
              }
              connectedTime = 0L
              client = null
            }
            exec = true
            LOG.error("Failed to create indices. Going to retry after %dms".format(curWaitTm), e)
            val nSecs = curWaitTm / 1000
            for (i <- 0 until nSecs) {
              try {
                if (!canConsiderShutdown(considerShutdown))
                  Thread.sleep(1000)
              } catch {
                case e: Throwable => {}
              }
            }
            curWaitTm = curWaitTm * 2
            if (curWaitTm > 60000)
              curWaitTm = 60000
          }
        }
      }
    }
    catch {
      case e: Exception => {
        LOG.error("Failed to save an object in the table", e)
      }
    } finally {
      if (client != null) {
        client.close
        client = null
      }
    }

    if (client != null)
      client.close
  }

  private def getConnection: TransportClient = {
    try {
      var settings = Settings.settingsBuilder()
      settings.put("cluster.name", adapterConfig.clusterName)

      // add by saleh 15/12/2016
      val it = adapterConfig.properties.keySet.iterator
      while (it.hasNext) {
        val key = it.next()
        LOG.info("ElasticSearch - properties [%s] - [%s]".format(key, adapterConfig.properties.get(key).get.toString))
        settings.put(key, adapterConfig.properties.get(key).get.toString)
      }

      settings.build()
      val client = TransportClient.builder().addPlugin(classOf[ShieldPlugin]).settings(settings).build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(adapterConfig.location), adapterConfig.portNumber.toInt))
      return client
    } catch {
      case ex: Exception => LOG.error("Adapter getConnection ", ex)
    }

    return null
  }

  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    implicit val formats = org.json4s.DefaultFormats
    return new MonitorComponentInfo(AdapterConfiguration.TYPE_OUTPUT, adapterConfig.Name, ElasticsearchProducer.ADAPTER_DESCRIPTION, startTime, lastSeen, Serialization.write(metrics).toString)
  }

  def toFullTableName(containerName: String): String = {
    (adapterConfig.scehmaName + "." + containerName).toLowerCase()
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

    LOG.info(adapterConfig.Name + " Shutdown detected")

    // Shutdown HB
    isShutdown = true

    putJsonsWithMetadata(false)

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

    if (dataStore != null)
      dataStore.Shutdown()
  }

  private def updateMetricValue(key: String, value: Any): Unit = {
    counterLock.synchronized {
      if (key.equalsIgnoreCase(ElasticsearchProducer.LAST_FAILURE_TIME) ||
        key.equalsIgnoreCase(ElasticsearchProducer.LAST_RECOVERY_TIME)) {
        metrics(key) = value.toString
      } else {
        // This is an aggregated Long value
        val cur = metrics.getOrElse(key, "0").toString
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
            Thread.sleep(ElasticsearchProducer.HB_PERIOD)
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
            if (!isShutdown) LOG.warn("", e)
          }
          case e: Throwable => {
            externalizeExceptionEvent(e)
            if (!isShutdown) LOG.warn("", e)
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
    var sendStatus = ElasticsearchConstants.ELASTICSEARCH_NOT_SEND
    var retryCount = 0
    var waitTm = 15000

    // We keep on retry until we succeed on this thread
    while (sendStatus != ElasticsearchConstants.ELASTICSEARCH_SEND_SUCCESS && isShutdown == false) {
      try {
        //     sendStatus = doSend(keyMessages, removeFromFailedMap)   //check this
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          LOG.error(adapterConfig.Name + " Elasticsearch PRODUCER: Error sending to kafka, Retrying after %dms. Retry count:%d".format(waitTm, retryCount), e)
          try {
            Thread.sleep(waitTm)
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


}
