package com.ligadata.HbaseInputOutputAdapters

import java.lang

import com.ligadata.Exceptions.KamanjaException
import com.ligadata.HeartBeat.MonitorComponentInfo
import com.ligadata.InputOutputAdapterInfo.{PartitionUniqueRecordValue, StartProcPartInfo, _}
import com.ligadata.KamanjaBase.{EnvContext, NodeContext}
import com.ligadata.KvBase.TimeRange
import com.ligadata.Utils.ClusterStatus
import com.ligadata.adapterconfiguration.{HbaseAdapterConfiguration, HbasePartitionUniqueRecordKey, HbasePartitionUniqueRecordValue}
import com.ligadata.keyvaluestore.HBaseAdapter
import org.apache.hadoop.hbase.client.Connection
import org.apache.hadoop.hbase.util.Bytes
import org.json4s.jackson.Serialization

import scala.actors.threadpool.{ExecutorService, Executors}
import scala.collection.mutable.ArrayBuffer

/**
  * Created by Yousef on 8/14/2016.
  */
case class HbaseExceptionInfo (Last_Failure: String, Last_Recovery: String)

object KamanjaHbaseConsumer  extends InputAdapterFactory {
  val INITIAL_SLEEP = 500
  val MAX_SLEEP = 30000
  val POLLING_INTERVAL = 100

  // Statistics Keys
  val ADAPTER_DESCRIPTION = "Hbase Consumer Client"

  def CreateInputAdapter(inputConfig: AdapterConfiguration, execCtxtObj: ExecContextFactory, nodeContext: NodeContext): InputAdapter = new KamanjaHbaseConsumer(inputConfig, execCtxtObj, nodeContext)
}

class KamanjaHbaseConsumer(val inputConfig: AdapterConfiguration, val execCtxtObj: ExecContextFactory, val nodeContext: NodeContext) extends InputAdapter{
  val input = this
  //  lazy val loggerName = this.getClass.getName
  private var isQuiese = false
  private var sleepDuration = 500
  lazy val LOG = logger
  private var lastSeen: String = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private val lock = new Object()
  private var readExecutor: ExecutorService = _
  private var metrics: collection.mutable.Map[String,Any] = collection.mutable.Map[String,Any]()
  private val adapterConfig = HbaseAdapterConfiguration.getAdapterConfig(inputConfig, "input")
  private var isShutdown = false
  private var isQuiesced = false
  private var startTime: Long = 0
  private var msgCount = 0
  private var _ignoreFirstMsg : Boolean = _
  private var startHeartBeat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var partitonCounts: collection.mutable.Map[String,Long] = collection.mutable.Map[String,Long]()
  private var partitonDepths: collection.mutable.Map[String,Long] = collection.mutable.Map[String,Long]()
  private var partitionExceptions: collection.mutable.Map[String,HbaseExceptionInfo] = collection.mutable.Map[String,HbaseExceptionInfo]()
  private var initialFilesHandled = false
  private var envContext : EnvContext = nodeContext.getEnvCtxt()
  private var clusterStatus : ClusterStatus = null
  metrics(com.ligadata.adapterconfiguration.KamanjaHbaseAdapterConstants.PARTITION_COUNT_KEYS) = partitonCounts
  metrics(com.ligadata.adapterconfiguration.KamanjaHbaseAdapterConstants.EXCEPTION_SUMMARY) = partitionExceptions
  metrics(com.ligadata.adapterconfiguration.KamanjaHbaseAdapterConstants.PARTITION_DEPTH_KEYS) = partitonDepths
  private var startTimestamp: Long = 0 /// take data from this timestamp
  private var endTimeStamp: Long = 0 /// take data to this timestamp

  val hbaseutil: HbaseUtility = new HbaseUtility
  var dataStoreInfo = hbaseutil.createDataStorageInfo(adapterConfig)
  var dataStore = hbaseutil.GetDataStoreHandle(dataStoreInfo).asInstanceOf[HBaseAdapter]
  //dataStore.setDefaultSerializerDeserializer("com.ligadata.kamanja.serializer.kbinaryserdeser", scala.collection.immutable.Map[String, Any]())

  override def StartProcessing(partitionIds: Array[StartProcPartInfo], ignoreFirstMsg: Boolean): Unit = {  //////create all code hereeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee
    isShutdown = false
    var maxPartNumber = -1
    _ignoreFirstMsg = ignoreFirstMsg
    var lastHb: Long = 0
    startHeartBeat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
    var numberOfThreads = partitionIds.size
    readExecutor = Executors.newFixedThreadPool(numberOfThreads)
    LOG.info("Hbase_ADAPTER - START_PROCESSING CALLED")

    // Check to see if this already started
    if (startTime > 0) {
      LOG.error("Hbase_ADAPTER: already started, or in the process of shutting down")
    }
    startTime = System.nanoTime

    if (partitionIds == null || partitionIds.size == 0) {
      LOG.error("Hbase_ADAPTER: Cannot process the file adapter request, invalid parameters - number")
      return
    }

    partitionIds.foreach(part => {
      val partitionId = part._key.asInstanceOf[HbasePartitionUniqueRecordKey].PartitionId
      // Initialize the monitoring status
      partitonCounts(partitionId.toString) = 0
      partitonDepths(partitionId.toString) = 0
      partitionExceptions(partitionId.toString) = new HbaseExceptionInfo("n/a","n/a")
    })

    val partitionInfo = partitionIds.map(quad => {
      (quad._key.asInstanceOf[HbasePartitionUniqueRecordKey],
        quad._val.asInstanceOf[HbasePartitionUniqueRecordValue],
        quad._validateInfoVal.asInstanceOf[HbasePartitionUniqueRecordValue])
    })

    val myPartitionInfo = partitionIds.map(pid => (pid._key.asInstanceOf[HbasePartitionUniqueRecordKey].PartitionId,
      pid._val.asInstanceOf[HbasePartitionUniqueRecordValue].TableName,
      pid._val.asInstanceOf[HbasePartitionUniqueRecordValue].TimeStamp, ignoreFirstMsg)) //////add key

    val myPartitionInfoMap = partitionIds.map(pid =>
      pid._key.asInstanceOf[HbasePartitionUniqueRecordKey].PartitionId ->
        (pid._val.asInstanceOf[HbasePartitionUniqueRecordValue].TimeStamp, ignoreFirstMsg)
    ).toMap

   // myPartitionInfoMap

    val partitionListDisplay = partitionIds.map(x => {
      x._key.asInstanceOf[HbasePartitionUniqueRecordKey].PartitionId
    }).mkString(",")

    //data is comma separated partition ids
    val currentNodePartitions = partitionListDisplay.split(",").map(idStr => idStr.toInt).toList


    currentNodePartitions.foreach(partitionid => {
      val initialTimestamp = myPartitionInfoMap(partitionid)._1

      readExecutor.execute(new Runnable() {
        var intSleepTimer = KamanjaHbaseConsumer.INITIAL_SLEEP

        var execContexts: ExecContext = null
        var uniqueKey = new HbasePartitionUniqueRecordKey
        //////////////////////// var topicPartitions: Array[org.apache.kafka.common.TopicPartition] = new Array[org.apache.kafka.common.TopicPartition](maxPartNumber + 1)
        //      var initialOffsets: Array[Long] = new Array[Long](maxPartNumber + 1)
        //      var ignoreUntilOffsets: Array[Long] = new Array[Long](maxPartNumber + 1)

        // Create a new EngineMessage and call the engine.
        if (execContexts == null) {
          execContexts = execCtxtObj.CreateExecContext(input, uniqueKey, nodeContext)
        }

        var timerange: TimeRange = _

        override def run(): Unit = {
          var readTmMs = System.currentTimeMillis
          var isRecordSentToKamanja = false
          val uniqueVal = new HbasePartitionUniqueRecordValue
          while ( /*!isRecordSentToKamanja &&*/ !isQuiese) {
            val retrievedata = (data: Array[Byte]) => {
              readTmMs = System.currentTimeMillis
              if (data != null) {
                val uniqueVal = new HbasePartitionUniqueRecordValue
                uniqueVal.TimeStamp = readTmMs
                uniqueVal.TableName = adapterConfig.TableName
                uniqueVal.Key = new String(data).substring(0, new String(data).indexOf(',')).toInt
                msgCount += 1
                execContexts.execute(data, uniqueKey, uniqueVal, readTmMs)
              }
            }
            endTimeStamp = System.currentTimeMillis
            val fulltablename = adapterConfig.scehmaName + ":" + adapterConfig.TableName
            timerange = TimeRange(initialTimestamp+1, endTimeStamp) ///filter data based on time range
            dataStore.getAllRecords(fulltablename, timerange, retrievedata)
            Thread.sleep(1000)
          }
        }
      })
    })
  }

  override def Shutdown: Unit = lock.synchronized {
    isQuiese = true
    StopProcessing
  }

  override def DeserializeKey(k: String): PartitionUniqueRecordKey = {
    val key = new HbasePartitionUniqueRecordKey
    try {
      LOG.debug("Deserializing Key:" + k)
      key.Deserialize(k)
    } catch {
      case e: Exception => {
        externalizeExceptionEvent(e)
        LOG.error("Failed to deserialize Key:%s.".format(k), e)
        throw e
      }
      case e: Throwable => {
        externalizeExceptionEvent(e)
        LOG.error("Failed to deserialize Key:%s.".format(k), e)
        throw e
      }
    }
    key
  }

  override def DeserializeValue(v: String): PartitionUniqueRecordValue = {
    val vl = new HbasePartitionUniqueRecordValue
    if (v != null) {
      try {
        LOG.debug("Deserializing Value:" + v)
        vl.Deserialize(v)
      } catch {
        case e: Exception => {
          externalizeExceptionEvent(e)
          LOG.error("Failed to deserialize Value:%s.".format(v), e)
          throw e
        }
        case e: Throwable => {
          externalizeExceptionEvent(e)
          LOG.error("Failed to deserialize Value:%s.".format(v), e)
          throw e
        }
      }
    }
    vl
  }

  private def getKeyValuePairs(): Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = {
    val infoBuffer = ArrayBuffer[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]()
    for(partitionId <- 1 to adapterConfig.numberOfThread) {
      val rKey = new HbasePartitionUniqueRecordKey
      val rValue = new HbasePartitionUniqueRecordValue
      rKey.PartitionId = partitionId
      rKey.Name = adapterConfig.Name
      rValue.TimeStamp = 0
      rValue.Key = -1
      rValue.TableName = adapterConfig.TableName
      infoBuffer.append((rKey, rValue))
    }
    infoBuffer.toArray
  }

  override def getAllPartitionBeginValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = lock.synchronized {
    getKeyValuePairs()
  }

  override def getAllPartitionEndValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = lock.synchronized {
    getKeyValuePairs()
  }

  private def resetSleepTimer(): Unit = {
    sleepDuration = KamanjaHbaseConsumer.INITIAL_SLEEP
  }

  override def GetAllPartitionUniqueRecordKey: Array[PartitionUniqueRecordKey] = lock.synchronized {
    val infoBuffer = ArrayBuffer[PartitionUniqueRecordKey]()
    for(partitionId <- 1 to adapterConfig.numberOfThread){
      var newkey = new HbasePartitionUniqueRecordKey
      newkey.PartitionId = partitionId
      newkey.Name = adapterConfig.Name
      infoBuffer.append(newkey)
    }
    infoBuffer.toArray
  }

  private def getSleepTimer() : Int = {
    var thisSleep = sleepDuration
    sleepDuration = scala.math.max(KamanjaHbaseConsumer.MAX_SLEEP, thisSleep *2)
    return thisSleep
  }

  override def StopProcessing(): Unit = {
    isShutdown = true
    terminateReaderTasks
  }

  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    implicit val formats = org.json4s.DefaultFormats

    var depths:  Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = null

    try {
      depths = getAllPartitionEndValues
    } catch {
      case e: KamanjaException => {
        return new MonitorComponentInfo(AdapterConfiguration.TYPE_INPUT, adapterConfig.Name, KamanjaHbaseConsumer.ADAPTER_DESCRIPTION, startHeartBeat, lastSeen, Serialization.write(metrics).toString)
      }
      case e: Exception => {
        LOG.error ("SMART-FILE-ADAPTER: Unexpected exception determining depths for smart file input adapter " + adapterConfig.Name, e)
        return new MonitorComponentInfo(AdapterConfiguration.TYPE_INPUT, adapterConfig.Name, KamanjaHbaseConsumer.ADAPTER_DESCRIPTION, startHeartBeat, lastSeen, Serialization.write(metrics).toString)
      }
    }

    return new MonitorComponentInfo(AdapterConfiguration.TYPE_INPUT, adapterConfig.Name, KamanjaHbaseConsumer.ADAPTER_DESCRIPTION,
      startHeartBeat, lastSeen,  Serialization.write(metrics).toString)
  } ///get status with more details for each node monitoring rest api

  override def getComponentSimpleStats: String = {
    return "Input/"+ adapterConfig.Name +"/evtCnt" + "->" + msgCount
  } /////// return status on status queue

  private def terminateReaderTasks(): Unit = {
    if (readExecutor == null) return

    // Give the threads to gracefully stop their reading cycles, and then execute them with extreme prejudice.
    Thread.sleep(adapterConfig.noDataSleepTimeInMs + 1)
    if (readExecutor != null) readExecutor.shutdownNow
    while (readExecutor != null && readExecutor.isTerminated == false) {
      Thread.sleep(100)
    }

    LOG.debug("Hbase_ADAPTER - Shutdown Complete")
    readExecutor = null
  }
}
