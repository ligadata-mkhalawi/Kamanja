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
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse
import org.elasticsearch.client.IndicesAdminClient
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse
import com.ligadata.keyvaluestore.ElasticsearchAdapter
import org.apache.logging.log4j.LogManager
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.VersionType
import org.json4s.jackson.Serialization


import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.ArrayBuffer
import org.json.JSONObject
import org.elasticsearch.shield.ShieldPlugin

object ElasticsearchProducer extends OutputAdapterFactory {
  def CreateOutputAdapter(inputConfig: AdapterConfiguration, nodeContext: NodeContext): OutputAdapter = new ElasticsearchProducer(inputConfig, nodeContext)

  // Statistics Keys
  val ADAPTER_DESCRIPTION = "Elasticsearch Producer Client"
  val SEND_MESSAGE_COUNT_KEY = "Messages Sent"
  val SEND_CALL_COUNT_KEY = "Send Call Count"
  val LAST_FAILURE_TIME = "Last_Failure"
  val LAST_RECOVERY_TIME = "Last_Recovery"
}

class ElasticsearchProducer(val inputConfig: AdapterConfiguration, val nodeContext: NodeContext) extends OutputAdapter {
  private[this] val LOG = LogManager.getLogger(getClass);
  private val nodeId = if (nodeContext == null || nodeContext.getEnvCtxt() == null) "1" else nodeContext.getEnvCtxt().getNodeId()
  private val FAIL_WAIT = 2000
  private var numOfRetries = 0
  private var MAX_RETRIES = 3
  private var startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var lastSeenTime = System.currentTimeMillis
  private var metrics: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map[String, Any]()
  metrics("MessagesProcessed") = new AtomicLong(0)
  private var isShutdown = false
  private var isHeartBeating = false
  private var isInError = false
  private var msgCount = 0
  val default_outstanding_messages = "2048"
  val max_outstanding_messages = default_outstanding_messages.toString.trim().toInt
  private var commitExecutor: ExecutorService = Executors.newFixedThreadPool(1)
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
  val timeToWriteRecs = adapterConfig.timeToWriteRecsInSec
  val writeRecsBatch = adapterConfig.writeRecsBatch
  var nextWrite = System.currentTimeMillis + timeToWriteRecs
  var flagindex = false
  transService.init(1)
  transId = transService.getNextTransId

  // Getting first transaction. It may get wasted if we don't have any lines to save.

  case class MsgDataRecievedCnt(cntrToOrder: Long, msg: Array[(Array[Byte], Array[Byte])])

  val partitionsMap = new ConcurrentHashMap[Int, ConcurrentHashMap[Long, MsgDataRecievedCnt]](128);
  val failedMsgsMap = new ConcurrentHashMap[Int, ConcurrentHashMap[Long, MsgDataRecievedCnt]](128);
  // We just need Array Buffer as Innser value. But the only issue is we need to make sure we handle it for multiple threads.

  private val producer = this

  // Start the heartbeat.
  commitExecutor.execute(new Runnable() {
    override def run(): Unit = {
      while (!isShutdown) {
        try {
          val dt = System.currentTimeMillis
          if (IsTimeToWrite(dt)) {
            producer.synchronized {
              if (IsTimeToWrite(dt)) {
                if (LOG.isInfoEnabled) LOG.info("Going to write records now. Current time:%d, next write time:%d, current records to write:%d, batch size:%d".format(dt, nextWrite, recsToWrite, writeRecsBatch))
                putJsonsWithMetadata(true)
                nextWrite = System.currentTimeMillis + timeToWriteRecs
              }
            }
          }
        } catch {
          case e: Throwable => {
            if (!isShutdown)
              logger.warn("Failed to commit. ClassLoader:" + getClass().getClassLoader(), e)
          }
        }

        try {
          Thread.sleep(1000)
        } catch {
          case e: Throwable => {}
        }
      }
    }
  })

  override def send(messages: Array[Array[Byte]], partitionKeys: Array[Array[Byte]]): Unit = {
    throw new Exception("send with data is not yet implemented")
  }

  private def addData(data: Map[String, Array[String]]): Unit = producer.synchronized {
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

  private def IsTimeToWrite(dt: Long): Boolean = {
    (dt > nextWrite || recsToWrite > writeRecsBatch)
  }

  override def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): Unit = {
    if (outputContainers.size == 0) return

    val dt = System.currentTimeMillis
    var indexName = adapterConfig.TableName
    lastSeenTime = dt

    if (IsTimeToWrite(dt)) {
      producer.synchronized {
        if (IsTimeToWrite(dt)) {
          if (LOG.isInfoEnabled) LOG.info("Going to write records now. Current time:%d, next write time:%d, current records to write:%d, batch size:%d".format(dt, nextWrite, recsToWrite, writeRecsBatch))
          putJsonsWithMetadata(true)
          nextWrite = System.currentTimeMillis + timeToWriteRecs
        }
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


    val strDataRecords = serializedContainerData.map(data => new String(data))

    val map = Map[String, Array[String]](indexName -> strDataRecords)
    addData(map)
    if (LOG.isDebugEnabled) LOG.debug("Added %d records to cached data in %d indices".format(strDataRecords.size, map.size))

    metrics("MessagesProcessed").asInstanceOf[AtomicLong].addAndGet(strDataRecords.size)
  }

  private def canConsiderShutdown(considerShutdown: Boolean): Boolean = {
    isShutdown && considerShutdown
  }

  private def putJsonsWithMetadata(considerShutdown: Boolean): Unit = producer.synchronized {
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
      if (!flagindex) {
        if (adapterConfig.manuallyCreateIndexMapping && adapterConfig.indexMapping.length > 0) {
          val allKeys = sendData.map(kv => kv._1).toArray
          if (LOG.isInfoEnabled) LOG.info("Validating whether indices {%s} exists or not".format(allKeys.mkString(",")))
          exec = true
          curWaitTm = 5000
          while (!canConsiderShutdown(considerShutdown) && exec) {
            exec = false
            try {
              if (client == null) {
                client = getConnection
                connectedTime = System.currentTimeMillis
              }
              createIndexForOutputAdapter(client, allKeys, adapterConfig.indexMapping, true)
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
        flagindex = true
      }

      if (LOG.isInfoEnabled) LOG.info("About to write %d records".format(recsToWrite))
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
                if (LOG.isDebugEnabled) LOG.debug("Executing bulk indexing...")
                val bulkResponse = bulkRequest.execute().actionGet()

                // BUGBUG:: If we have errors do we treat this data is added ???????
                //added by saleh 15/12/2016
                if (bulkResponse.hasFailures && LOG.isWarnEnabled) {
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
        if (LOG.isInfoEnabled) LOG.info("ElasticSearch - properties [%s] - [%s]".format(key, adapterConfig.properties.get(key).get.toString))
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
    val lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(lastSeenTime))
    return new MonitorComponentInfo(AdapterConfiguration.TYPE_OUTPUT, adapterConfig.Name, ElasticsearchProducer.ADAPTER_DESCRIPTION, startTime, lastSeen, Serialization.write(metrics).toString)
  }

  def toFullTableName(containerName: String): String = {
    (adapterConfig.scehmaName + "." + containerName).toLowerCase()
  }

  private def createIndexForOutputAdapter(indexName: String, indexMapping: String): Unit = {
    createIndexForOutputAdapter(Array(indexName), indexMapping, false)
  }

  private def createIndexForOutputAdapter(client: TransportClient, indexNames: Array[String], indexMapping: String, onlyNonExistIndices: Boolean): Unit = {
    var exp: Throwable = null
    try {
      indexNames.foreach(indexName => {
        val fullIndexName = toFullTableName(indexName)
        var tryNo = 0
        var haveValidIndex = false

        while (tryNo < 2 && !haveValidIndex) {
          tryNo += 1
          val createIndex =
            if (onlyNonExistIndices) {
              (!(checkIndexExsists(indexName, client)))
            } else {
              true
            }
          if (createIndex) {
            try {
              val putMappingResponse = client.admin().indices().prepareCreate(fullIndexName)
                .setSource(indexMapping)
                .execute().actionGet()
              val tmp: RefreshResponse = client.admin().indices().prepareRefresh(fullIndexName).get()
              haveValidIndex = true
            } catch {
              case e: Throwable => {
                if (exp != null && tryNo == 2)
                  exp = new Exception("Failed to create Index " + fullIndexName, e)
              }
            }
          } else {
            haveValidIndex = true
          }
        }
      })
    }
    catch {
      case e: Exception => {
        LOG.error("Failed to create Index", e)
      }
    }

    if (exp != null)
      throw exp
  }

  private def createIndexForOutputAdapter(indexNames: Array[String], indexMapping: String, onlyNonExistIndices: Boolean): Unit = {
    var client: TransportClient = null
    var exp: Throwable = null
    try {
      client = getConnection
      createIndexForOutputAdapter(client, indexNames, indexMapping, onlyNonExistIndices)
    }
    catch {
      case e: Exception => {
        LOG.error("Failed to create Index", e)
      }
    } finally {
      if (client != null) {
        client.close
      }
    }
  }

  private def checkIndexExsists(indexName: String, client: TransportClient): Boolean = {
    val fullIndexName = toFullTableName(indexName)
    try {
      val indices: IndicesAdminClient = client.admin().indices()
      val res: IndicesExistsResponse = indices.prepareExists(fullIndexName.toLowerCase).execute().actionGet()
      if (res.isExists) {
        return true
      } else {
        return false
      }
    } catch {
      case e: Exception => {
        LOG.error("Failed to check if Index exists " + fullIndexName, e)
        return false
      }
    }
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

    if (LOG.isWarnEnabled) LOG.warn(adapterConfig.Name + " Shutdown detected")

    isShutdown = true

    if (commitExecutor != null)
      commitExecutor.shutdownNow

    putJsonsWithMetadata(false)
  }
}
