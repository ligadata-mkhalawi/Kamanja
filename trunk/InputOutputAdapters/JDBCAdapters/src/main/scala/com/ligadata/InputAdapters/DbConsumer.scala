package com.ligadata.InputAdapters

import java.sql.{Connection, Driver, DriverManager, DriverPropertyInfo, ResultSet, SQLException, Statement}
import java.util.Properties

import scala.actors.threadpool.{ExecutorService, Executors}
import org.apache.logging.log4j.LogManager
import com.ligadata.AdaptersConfiguration.{DbAdapterConfiguration, DbPartitionUniqueRecordKey, DbPartitionUniqueRecordValue, DbSerializeFormat, QueryInfo}
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.KamanjaBase.NodeContext
import org.apache.commons.dbcp2.BasicDataSource
import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable.ArrayBuffer
import com.ligadata.HeartBeat.MonitorComponentInfo
import com.ligadata.Utils.{KamanjaLoaderInfo, Utils}

object DbConsumer extends InputAdapterFactory {
  val ADAPTER_DESCRIPTION = "JDBC_Consumer"

  def CreateInputAdapter(inputConfig: AdapterConfiguration, execCtxtObj: ExecContextFactory, nodeContext: NodeContext): InputAdapter = new DbConsumer(inputConfig, execCtxtObj, nodeContext)
}

class DbConsumerDriverShim(d: Driver) extends Driver {
  private var driver: Driver = d

  def connect(u: String, p: Properties): Connection = this.driver.connect(u, p)

  def acceptsURL(u: String): Boolean = this.driver.acceptsURL(u)

  def getPropertyInfo(u: String, p: Properties): Array[DriverPropertyInfo] = this.driver.getPropertyInfo(u, p)

  def getMajorVersion(): Int = this.driver.getMajorVersion

  def getMinorVersion(): Int = this.driver.getMinorVersion

  def jdbcCompliant(): Boolean = this.driver.jdbcCompliant()

  def getParentLogger(): java.util.logging.Logger = this.driver.getParentLogger()
}

class DbConsumer(val inputConfig: AdapterConfiguration, val execCtxtObj: ExecContextFactory, val nodeContext: NodeContext) extends InputAdapter {
  private[this] val LOG = LogManager.getLogger(getClass);
  private[this] val dcConf = DbAdapterConfiguration.getAdapterConfig(inputConfig)
  private[this] val lock = new Object()

  private var startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var lastSeen = System.currentTimeMillis
  private val msgCount = new AtomicLong(0)
  private var metrics: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map[String, Any]()

  // private[this] val kvs = scala.collection.mutable.Map[String, (DbPartitionUniqueRecordKey, DbPartitionUniqueRecordValue, DbPartitionUniqueRecordValue)]()

  private[this] var maxActiveConnections = 256
  private[this] var maxIdleConnections = 8
  private[this] var initialSize = 5
  private[this] var maxWaitMillis = 10000

  private[this] var isShutdown = false
  private[this] var isStopProcessing = false

  //DataSource for the connection Pool
  private var dataSource: BasicDataSource = _
  private var executor: ExecutorService = _
  private val input = this
  private var driver: DbConsumerDriverShim = null
  private val clsLoader = new KamanjaLoaderInfo()

  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    val lastSeenStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(lastSeen))
    return new MonitorComponentInfo(AdapterConfiguration.TYPE_INPUT, inputConfig.Name, DbConsumer.ADAPTER_DESCRIPTION,
      startTime, lastSeenStr, "{}" /*Serialization.write(metrics).toString */)
  }

  override def getComponentSimpleStats: String = {
    return "Input/" + inputConfig.Name + "/evtCnt" + "->" + msgCount.get()
  }

  override def Shutdown: Unit = lock.synchronized {
    isShutdown = true
    StopProcessing
  }

  override def StopProcessing: Unit = lock.synchronized {
    isStopProcessing = true
    LOG.debug("Initiating Stop Processing...")

    //Shutdown the executor
    if (executor != null) {
      executor.shutdownNow
      while (executor.isTerminated == false) {
        // sleep 100ms and then check
        Thread.sleep(100)
      }
    }
    executor = null

    if (dataSource != null) {
      dataSource.close
      dataSource = null
    }
  }

  private def loadDriver: Unit = {
    try {
      if (LOG.isInfoEnabled) LOG.info("Loading the Driver..")
      val d = Class.forName(dcConf.DriverName, true, clsLoader.loader).newInstance.asInstanceOf[Driver]
      if (LOG.isInfoEnabled) LOG.info("Registering Driver..")
      driver = new DbConsumerDriverShim(d)
      DriverManager.registerDriver(driver);
    } catch {
      case e: Exception => {
        LOG.error("Failed to load/register jdbc driver name:%s.".format(dcConf.DriverName), e)
        throw e
      }
    }
  }

  private def getBasicDataSource: BasicDataSource = {
    try {
      //Create a DBCP based Connection Pool Here
      var dataSrc = new BasicDataSource

      if (driver != null)
        dataSrc.setDriver(driver)
      dataSrc.setDriverClassName(dcConf.DriverName)
      // dataSrc.setDriverClassLoader()
      dataSrc.setUrl(dcConf.URLString)
      dataSrc.setUsername(dcConf.UserId);
      dataSrc.setPassword(dcConf.Password);
      dataSrc.setMaxTotal(maxActiveConnections);
      dataSrc.setMaxIdle(maxIdleConnections);
      dataSrc.setMinIdle(0);
      dataSrc.setInitialSize(initialSize);
      dataSrc.setMaxWaitMillis(maxWaitMillis);

      dataSrc.setTestWhileIdle(false);

      if (dcConf.validationQuery != null && !dcConf.validationQuery.trim.isEmpty) {
        dataSrc.setTestOnBorrow(true);
        dataSrc.setValidationQuery(dcConf.validationQuery);
      }
      dataSrc.setTestOnReturn(false);

      return dataSrc
    } catch {
      case e: Exception => {
        LOG.error("Failed to create BasicDataSource using DriverName:%s, URLString:%s, UserId:%s, Password:%s, maxActiveConnections:%d, maxIdleConnections:%d, initialSize:%d, maxWaitMillis:%d.".format(dcConf.DriverName, dcConf.URLString, dcConf.UserId, dcConf.Password, maxActiveConnections, maxIdleConnections, initialSize, maxWaitMillis), e)
        throw e
      }
    }
  }

  private def getConnection: Connection = {
    try {
      var con = dataSource.getConnection
      con
    } catch {
      case e: Exception => {
        LOG.error("Failed to get connection", e)
        throw e
      }
    }
  }

  private def serializeData(rowData: ArrayBuffer[String], dataBuf: StringBuilder, quotedType: ArrayBuffer[Boolean], columnNames: ArrayBuffer[String]): Unit = {
    var addFldDelimiter = false
    val sz = rowData.length
    if (dcConf.format == DbSerializeFormat.kv) {
      for (i <- 0 until sz) {
        if (addFldDelimiter)
          dataBuf.append(dcConf.fieldDelimiter)
        dataBuf.append(columnNames(i))
        dataBuf.append(dcConf.keyDelimiter)
        dataBuf.append(rowData(i))
        addFldDelimiter = true
      }
    } else if (dcConf.format == DbSerializeFormat.json) {
      dataBuf.append("{")
      for (i <- 0 until sz) {
        if (addFldDelimiter)
          dataBuf.append(",")
        if (quotedType(i))
          dataBuf.append(""""%s":"%s"""".format(columnNames(i), rowData(i)))
        else
          dataBuf.append(""""%s":%s""".format(columnNames(i), rowData(i)))
        addFldDelimiter = true
      }
      dataBuf.append("}")
    } else if (dcConf.format == DbSerializeFormat.delimited) {
      for (i <- 0 until sz) {
        if (addFldDelimiter)
          dataBuf.append(dcConf.fieldDelimiter)
        dataBuf.append(rowData(i))
        addFldDelimiter = true
      }
    } else {
      throw new Exception("Unhandled format:" + dcConf.format.toString)
    }
  }

  private def isQuotedType(columnType: Int): Boolean = {
    // BUGBUG:: java.sql.Types.DECIMAL & java.sql.Types.NUMERIC are also treaded quoted data for now. Is it correct?
    return (
      columnType != java.sql.Types.TINYINT &&
        columnType != java.sql.Types.SMALLINT &&
        columnType != java.sql.Types.INTEGER &&
        columnType != java.sql.Types.ROWID &&
        columnType != java.sql.Types.BIGINT &&
        columnType != java.sql.Types.FLOAT &&
        columnType != java.sql.Types.REAL &&
        columnType != java.sql.Types.DOUBLE &&
        columnType != java.sql.Types.NUMERIC &&
        columnType != java.sql.Types.DECIMAL
      )
  }

  private def readData(partitionKey: DbPartitionUniqueRecordKey, partitionVal: DbPartitionUniqueRecordValue,
                       execThread: ExecContext, qryUniqId: String, queryInfo: QueryInfo, execNo: Long, queryNo: Int): Unit = {
    var con: Connection = null
    var stmt: Statement = null
    var rs: ResultSet = null
    // BUGBUG:: Yet to prepare query

    val prevVal = partitionVal.QueryId2Primary.getOrElse(qryUniqId, null)

    var primaryKeyExpression: String = null
    if (prevVal != null)
      primaryKeyExpression = queryInfo.PrimaryKeyExpression.replace("${PreviousPrimaryKeyValue}", prevVal)

    val partitionExpression0 = queryInfo.PartitionExpression.replace("${MaxPartitions}", dcConf.Consumers.toString)
    val partitionExpression = partitionExpression0.replace("${PartitionId}", partitionKey.PartitionId.toString)

    val primaryAndPartitionSubstitution =
      if (primaryKeyExpression != null)
        " ((%s) AND (%s)) ".format(primaryKeyExpression, partitionExpression)
      else
        " (%s) ".format(partitionExpression)

    val query = queryInfo.Query.replace("${PrimaryAndPartitionSubstitution}", primaryAndPartitionSubstitution)

    if (LOG.isDebugEnabled()) LOG.debug("ExecNo:%d, QueryNo:%d. Fetch the results of query:%s".format(execNo, queryNo, query))
    try {
      con = getConnection
      if (isStopProcessing || isShutdown) throw new Exception("Adapter is shutting down")
      stmt = con.createStatement()
      if (isStopProcessing || isShutdown) throw new Exception("Adapter is shutting down")
      stmt.setQueryTimeout(dcConf.Timeout)
      rs = stmt.executeQuery(query);

      // This is for JSON type
      val quotedType = ArrayBuffer[Boolean]()
      val columnNames = ArrayBuffer[String]()
      val columns = rs.getMetaData
      var primaryKeyOrd = -1
      for (i <- 1 to columns.getColumnCount) {
        val colNm = columns.getColumnName(i)
        // val colNm = columns.getColumnLabel(i)
        columnNames += colNm
        if (primaryKeyOrd <= 0 && colNm.equalsIgnoreCase(queryInfo.PrimaryKeyColumn)) {
          primaryKeyOrd = i
        }
        if (dcConf.format == DbSerializeFormat.json) {
          val colType = columns.getColumnType(i)
          if (LOG.isTraceEnabled) LOG.trace("ExecNo:%d, QueryNo:%d. Columns %s, ColumnType:%d in query: %s".format(execNo, queryNo, colNm, colType, query))
          quotedType += isQuotedType(colType)
        }
      }

      val colsSize = columnNames.size
      val rowData = ArrayBuffer[String]()
      val dataBuf = new StringBuilder()

      if (LOG.isDebugEnabled()) LOG.debug("ExecNo:%d, QueryNo:%d. Found columns {%s} (QuoteTypes:{%s}) for query:%s and primary key ordinal:%d".format(execNo, queryNo, columnNames.mkString(","), quotedType.map(v => v.toString).mkString(","), query, primaryKeyOrd))

      if (primaryKeyOrd <= 0) {
        LOG.error("ExecNo:%d, QueryNo:%d. Primary key: %s is not found in columns:{%s} for QueryUniqueId:%s. So, every time query will be restarted from beginning".format(execNo, queryNo, queryInfo.PrimaryKeyColumn, columnNames.mkString(","), qryUniqId))
      }

      var foundFailure = false
      var rows = 0
      var hasNext = rs.next()

      if (LOG.isTraceEnabled()) LOG.trace("ExecNo:%d, QueryNo:%d. rows:%d, isStopProcessing:%s, isShutdown: %s, foundFailure: %s, hasNext:%s".format(execNo, queryNo, rows, isStopProcessing.toString, isShutdown.toString, foundFailure.toString, hasNext.toString))

      while (!isStopProcessing && !isShutdown && !foundFailure && hasNext) {
        try {
          val readTmMs = System.currentTimeMillis
          rowData.clear
          var primaryKeyValue: String = null
          for (i <- 1 to colsSize) {
            val fld = rs.getString(i)
            var fldVal: String = "" // Default value if value is null/empty
            if (fld != null && !fld.isEmpty)
              fldVal = fld.toString
            if (primaryKeyOrd == i)
              primaryKeyValue = fldVal
            rowData += fldVal
          }

          if (LOG.isTraceEnabled()) LOG.trace("ExecNo:%d, QueryNo:%d. Found row data {%s} of columns {%s} for query:%s and primary key ordinal:%d".format(execNo, queryNo, rowData.mkString(","), columnNames.mkString(","), query, primaryKeyOrd))

          if (!isStopProcessing && !isShutdown) {
            dataBuf.clear()
            serializeData(rowData, dataBuf, quotedType, columnNames)
            val message = dataBuf.toString()
            if (LOG.isDebugEnabled) LOG.debug("Processing message:" + message)
            var prevVal: String = null
            if (primaryKeyValue != null) {
              prevVal = partitionVal.QueryId2Primary.getOrElse(qryUniqId, null)
              partitionVal.QueryId2Primary(qryUniqId) = primaryKeyValue
            }
            try {
              if (LOG.isTraceEnabled()) LOG.trace("ExecNo:%d, QueryNo:%d. sending message %s".format(execNo, queryNo, message))
              execThread.execute(message.getBytes("UTF-8"), partitionKey, partitionVal, readTmMs)
            } catch {
              case e: Throwable => {
                if (primaryKeyValue != null) {
                  if (prevVal != null)
                    partitionVal.QueryId2Primary(qryUniqId) = prevVal
                  else
                    partitionVal.QueryId2Primary.remove(qryUniqId)
                }
                throw e
              }
            }
          }
          rows += 1
        } catch {
          case e: Throwable => {
            LOG.error("Failed to process messages. Existing current rowset and going to start again from previous point", e)
            foundFailure = true
          }
        }
        hasNext = rs.next()
        if (LOG.isTraceEnabled()) LOG.trace("ExecNo:%d, QueryNo:%d. rows:%d, isStopProcessing:%s, isShutdown: %s, foundFailure: %s, hasNext:%s".format(execNo, queryNo, rows, isStopProcessing.toString, isShutdown.toString, foundFailure.toString, hasNext.toString))
      }

      if (LOG.isDebugEnabled()) LOG.debug("ExecNo:%d, QueryNo:%d. Processed %d rows for query:%s".format(execNo, queryNo, rows, query))

    } catch {
      case e: Exception => {
        LOG.error("ExecNo:%d, QueryNo:%d. Failed to fetch data using query:%s for QueryUniqueId:%s".format(execNo, queryNo, query, qryUniqId), e)
        printFailure(e)
        throw e
      }
    } finally {
      if (rs != null) {
        rs.close
      }
      if (stmt != null) {
        stmt.close
      }
      if (con != null) {
        con.close
      }
    }
  }

  override def StartProcessing(partitionInfo: Array[StartProcPartInfo], ignoreFirstMsg: Boolean): Unit = lock.synchronized {
    isShutdown = false
    isStopProcessing = false

    if (LOG.isDebugEnabled) LOG.debug("Initiating Start Processing...")

    if (LOG.isDebugEnabled) LOG.debug("Configuration data - " + dcConf);

    if (partitionInfo == null || partitionInfo.size == 0)
      return

    val jars = ArrayBuffer[String]()

    if (dcConf.jarName != null && !dcConf.jarName.trim.isEmpty)
      jars += dcConf.jarName.trim

    if (dcConf.dependencyJars != null && !dcConf.dependencyJars.isEmpty)
      jars ++= dcConf.dependencyJars

    if (!jars.isEmpty) {
      val jarPaths = nodeContext.getEnvCtxt().getJarPaths()
      if (jarPaths != null && !jarPaths.isEmpty)
        Utils.LoadJars(jars.map(j => Utils.GetValidJarFile(jarPaths, j)).toArray, clsLoader.loadedJars, clsLoader.loader)
    }

    var startActionTime = System.currentTimeMillis
    var waitTime = 5000
    var tryAction = true

    while (tryAction && !isShutdown && !isStopProcessing) {
      try {
        loadDriver
        tryAction = false
      } catch {
        case e: Throwable => {
          val timeDiff = System.currentTimeMillis - startActionTime
          LOG.error("Failed to load driver. Waiting for %d ms and try again. Trying to load driver from past %d ms".format(waitTime, timeDiff), e);
          try {
            Thread.sleep(waitTime)
          } catch {
            case e: Throwable => {}
          }
          if (waitTime < 60000) {
            waitTime *= 2
            if (waitTime > 60000)
              waitTime = 60000
          }
        }
      }
    }

    if (isShutdown || isStopProcessing)
      return

    startActionTime = System.currentTimeMillis
    waitTime = 5000
    tryAction = true

    while (tryAction && !isShutdown && !isStopProcessing) {
      try {
        dataSource = getBasicDataSource
        tryAction = false
      } catch {
        case e: Throwable => {
          val timeDiff = System.currentTimeMillis - startActionTime
          LOG.error("Failed to create BasicDataSource using DriverName:%s, URLString:%s, UserId:%s, Password:%s, maxActiveConnections:%d, maxIdleConnections:%d, initialSize:%d, maxWaitMillis:%d. Waiting for %d ms and try again. Trying to create BasicDataSource from past %d ms".format(dcConf.DriverName, dcConf.URLString, dcConf.UserId, dcConf.Password, maxActiveConnections, maxIdleConnections, initialSize, maxWaitMillis, waitTime, timeDiff), e)
          try {
            Thread.sleep(waitTime)
          } catch {
            case e: Throwable => {}
          }
          if (waitTime < 60000) {
            waitTime *= 2
            if (waitTime > 60000)
              waitTime = 60000
          }
        }
      }
    }

    if (isShutdown || isStopProcessing)
      return

    executor = Executors.newFixedThreadPool(partitionInfo.length)

    var failedToCreateTasks = false
    partitionInfo.foreach(pInfo => {
      try {
        val k = if (pInfo._key != null) pInfo._key.asInstanceOf[DbPartitionUniqueRecordKey] else null
        val v = if (pInfo._val != null) pInfo._val.asInstanceOf[DbPartitionUniqueRecordValue] else null

        if (k != null) {
          executor.execute(new Runnable() {
            private val partitionKey = k
            private val partitionVal = if (v != null) v else new DbPartitionUniqueRecordValue
            private val execThread = execCtxtObj.CreateExecContext(input, partitionKey, nodeContext)
            private val threadName = "Adapter:%s-PartitionId:%d".format(inputConfig.Name, partitionKey.PartitionId)

            override def run(): Unit = {
              Utils.SetThreadName(Thread.currentThread(), threadName)
              try {
                var canContinueToSend = true
                var execNo = 0L
                while (canContinueToSend && !isStopProcessing && !isShutdown) {
                  canContinueToSend = dcConf.RefreshInterval > 0
                  execNo += 1
                  var queryNo = 0
                  dcConf.queriesInfo.foreach(kv => {
                    try {
                      queryNo += 1
                      if (!isStopProcessing && !isShutdown)
                        readData(partitionKey, partitionVal, execThread, kv._1, kv._2, execNo, queryNo)
                    } catch {
                      case e: Throwable => {
                        LOG.error("Failed to execute query for queryid :" + kv._1, e)
                      }
                    }
                  })

                  if (!isStopProcessing && !isShutdown && dcConf.RefreshInterval > 0) {
                    try {
                      Thread.sleep(dcConf.RefreshInterval)
                    } catch {
                      case e: Throwable => {}
                    }
                  }
                }
              } catch {
                case e: Throwable => {
                  LOG.error("Failed to execute in partition:" + partitionKey.Serialize + ". Terminating this partition execution", e)
                }
              }
            }
          })
        }
      } catch {
        case e: Exception => {
          failedToCreateTasks = true
          LOG.error("Failed to create tasks", e)
        }
      }
    })

    if (failedToCreateTasks) {
      LOG.error("Failed to create tasks. Shutting down the adapter")
      Shutdown
    }
  }

  override def GetAllPartitionUniqueRecordKey: Array[PartitionUniqueRecordKey] = lock.synchronized {
    GetAllPartitionsUniqueKeys
  }

  private def GetAllPartitionsUniqueKeys: Array[PartitionUniqueRecordKey] = lock.synchronized {
    val consumers = if (dcConf.Consumers <= 0) 0 else dcConf.Consumers
    val retKeys = new Array[PartitionUniqueRecordKey](consumers)
    for (i <- 0 until consumers) {
      val key = new DbPartitionUniqueRecordKey
      key.Name = inputConfig.Name
      key.PartitionId = i
      retKeys(i) = key
    }
    retKeys
  }

  override def DeserializeKey(k: String): PartitionUniqueRecordKey = {
    val key = new DbPartitionUniqueRecordKey
    try {
      LOG.debug("Deserializing Key:" + k)
      key.Deserialize(k)
    } catch {
      case e: Exception => {
        LOG.error("Failed to deserialize Key:%s. Reason:%s Message:%s".format(k, e.getCause, e.getMessage))
        throw e
      }
    }
    key
  }

  override def DeserializeValue(v: String): PartitionUniqueRecordValue = {
    val vl = new DbPartitionUniqueRecordValue
    if (v != null) {
      try {
        LOG.debug("Deserializing Value:" + v)
        vl.Deserialize(v)
      } catch {
        case e: Exception => {
          LOG.error("Failed to deserialize Value:%s. Reason:%s Message:%s".format(v, e.getCause, e.getMessage))
          throw e
        }
      }
    }
    vl
  }

  // BUGBUG:: Not yet implemented
  override def getAllPartitionBeginValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = {
    return Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]()
  }

  // BUGBUG:: Not yet implemented
  override def getAllPartitionEndValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = {
    return Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]()
  }

  private def printFailure(ex: Exception) {
    if (ex != null) {
      if (ex.isInstanceOf[SQLException]) {
        processSQLException(ex.asInstanceOf[SQLException])
      } else {
        LOG.error(ex)
      }
    }
  }

  private def processSQLException(sqlex: SQLException) {
    LOG.error(sqlex)
    var innerException: Throwable = sqlex.getNextException
    if (innerException != null) {
      LOG.error("Inner exception(s):")
    }
    while (innerException != null) {
      LOG.error(innerException)
      innerException = innerException.getCause
    }
  }
}