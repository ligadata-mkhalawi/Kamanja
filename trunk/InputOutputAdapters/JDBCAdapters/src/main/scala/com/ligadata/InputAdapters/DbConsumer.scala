

package com.ligadata.InputAdapters

import org.json4s.jackson.Serialization
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.SQLException
import java.sql.Statement
import java.util.Date

import scala.actors.threadpool.{ExecutorService, Executors}
import scala.util.control.Breaks.{break, breakable}
import org.apache.logging.log4j.LogManager
import com.ligadata.AdaptersConfiguration.{DbAdapterConfiguration, DbPartitionUniqueRecordKey, DbPartitionUniqueRecordValue}
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.KamanjaBase.NodeContext
// import javax.sql.DataSource

import org.apache.commons.dbcp2.BasicDataSource
import org.apache.commons.csv.CSVFormat
import java.io.StringWriter

import org.apache.commons.csv.CSVPrinter
import java.util.ArrayList

import com.thoughtworks.xstream.XStream
import com.ligadata.adapters.xstream.CustomMapConverter
import java.util.concurrent.atomic.AtomicLong

import com.ligadata.HeartBeat.MonitorComponentInfo


object DbConsumer extends InputAdapterFactory {
  val ADAPTER_DESCRIPTION = "JDBC Consumer"

  def CreateInputAdapter(inputConfig: AdapterConfiguration, execCtxtObj: ExecContextFactory, nodeContext: NodeContext): InputAdapter = new DbConsumer(inputConfig, execCtxtObj, nodeContext)
}

class DbConsumer(val inputConfig: AdapterConfiguration, val execCtxtObj: ExecContextFactory, val nodeContext: NodeContext) extends InputAdapter {
  private[this] val LOG = LogManager.getLogger(getClass);
  private[this] val dcConf = DbAdapterConfiguration.getAdapterConfig(inputConfig)
  private[this] val lock = new Object()

  private var startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var lastSeen = System.currentTimeMillis
  private val msgCount = new AtomicLong(0)
  private var metrics: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map[String, Any]()

  private[this] val kvs = scala.collection.mutable.Map[String, (DbPartitionUniqueRecordKey, DbPartitionUniqueRecordValue, DbPartitionUniqueRecordValue)]()

  private[this] var uniqueKey: DbPartitionUniqueRecordKey = new DbPartitionUniqueRecordKey

  private[this] var maxActiveConnections = 128
  private[this] var maxIdleConnections = 4
  private[this] var initialSize = 5
  private[this] var maxWaitMillis = 5000

  //DataSource for the connection Pool
  private var dataSource: BasicDataSource = _
  private var executor: ExecutorService = _
  private val input = this

  private val execThread = execCtxtObj.CreateExecContext(input, uniqueKey, nodeContext)

  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    val lastSeenStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(lastSeen))
    return new MonitorComponentInfo(AdapterConfiguration.TYPE_INPUT, inputConfig.Name, DbConsumer.ADAPTER_DESCRIPTION,
      startTime, lastSeenStr, Serialization.write(metrics).toString)
  }

  override def getComponentSimpleStats: String = {
    return "Input/" + inputConfig.Name + "/evtCnt" + "->" + msgCount.get()
  }

  override def Shutdown: Unit = lock.synchronized {
    StopProcessing
  }

  override def StopProcessing: Unit = lock.synchronized {
    LOG.debug("Initiating Stop Processing...")

    //Shutdown the executor
    if (executor != null) {
      executor.shutdownNow
      while (executor.isTerminated == false) {
        Thread.sleep(100) // sleep 100ms and then check
      }
      executor = null
    } else return


    if (dataSource != null) {
      dataSource.close
      dataSource = null
    } else return

  }

  override def StartProcessing(partitionInfo: Array[StartProcPartInfo], ignoreFirstMsg: Boolean): Unit = lock.synchronized {
    LOG.debug("Initiating Start Processing...")

    LOG.debug("Configuration data - " + dcConf);

    if (partitionInfo == null || partitionInfo.size == 0)
      return

    val partInfo = partitionInfo.map(quad => {
      (
        quad._key.asInstanceOf[DbPartitionUniqueRecordKey],
        quad._val.asInstanceOf[DbPartitionUniqueRecordValue],
        quad._validateInfoVal.asInstanceOf[DbPartitionUniqueRecordValue])
    })

    kvs.clear

/*
    partInfo.foreach(quad => {
      kvs(quad._1.DBName) = quad
    })
*/

    LOG.debug("KV Map =>")
    kvs.foreach(kv => {
      LOG.debug("Key:%s => Val:%s".format(kv._2._1.Serialize, kv._2._2.Serialize))
    })

    executor = Executors.newFixedThreadPool(partitionInfo.length)

    //Create a DBCP based Connection Pool Here
    dataSource = new BasicDataSource

    //Force a load of the DB Driver Class
    Class.forName(dcConf.DriverName)
    LOG.debug("Loaded the DB Driver..." + dcConf.DriverName)

    dataSource.setDriverClassName(dcConf.DriverName)
    if (dcConf.dbName != null && !dcConf.dbName.isEmpty())
      dataSource.setUrl(dcConf.URLString + "/" + dcConf.dbName)
    else
      dataSource.setUrl(dcConf.URLString)
    dataSource.setUsername(dcConf.UserId);
    dataSource.setPassword(dcConf.Password);

    dataSource.setTestWhileIdle(false);
    dataSource.setTestOnBorrow(true);
    dataSource.setValidationQuery("Select 1");
    dataSource.setTestOnReturn(false);

    dataSource.setMaxTotal(maxActiveConnections);
    dataSource.setMaxIdle(maxIdleConnections);
    dataSource.setMinIdle(0);
    dataSource.setInitialSize(initialSize);
    dataSource.setMaxWaitMillis(maxWaitMillis);

    //var conn:Connection = DriverManager.getConnection(dcConf.URLString+"/"+dcConf.dbName,dcConf.UserId,dcConf.Password)
    //var conn:Connection = dataSource.getConnection
    //LOG.debug("Created Connection..."+conn.toString())

    LOG.debug("Created DataSource..." + dataSource.toString())

    val uniqueValue = new DbPartitionUniqueRecordValue

    //Record the last run time on this counter value
    val runIntervalKey: String = Category concat "/" concat dcConf.Name concat dcConf.dbName
    // val lastRun:Long = cntrAdapter.getCntr(runIntervalKey);

    try {
      executor.execute(new Runnable() {
        override def run() {

          //For CSV Format
          var csvFormat = CSVFormat.DEFAULT.withSkipHeaderRecord().withIgnoreSurroundingSpaces()
          var stringWriter = new StringWriter
          var csvPrinter = new CSVPrinter(stringWriter, csvFormat)

          //For XML Format
          var xStream = new XStream
          xStream.registerConverter(new CustomMapConverter(xStream.getMapper))

          LOG.debug("Started the executor..." + Thread.currentThread().getName);

          var connection: Connection = null
          var statement: Statement = null
          var preparedStatement: PreparedStatement = null
          var resultset: ResultSet = null
          var resultSetMetaData: ResultSetMetaData = null

          LOG.debug("Before starting....");

          //connection = dataSource.getConnection
          connection = DriverManager.getConnection(dcConf.URLString + "/" + dcConf.dbName, dcConf.UserId, dcConf.Password)
          LOG.debug("Got the connection from the datasource");

          statement = connection.createStatement
          LOG.debug("Created the statement");

          var queryToExec = ""
          // BUGBUG:: Yet to insert PartitionId &
          if (dcConf.query != null && !dcConf.query.isEmpty()) {
            queryToExec = dcConf.query
          } else {
            if (dcConf.where != null && !dcConf.where.isEmpty()) {
              queryToExec = ("Select ".concat(dcConf.columns)
                .concat(" from ").concat(dcConf.table)
                .concat(" ").concat(dcConf.where))
            } else {
              queryToExec = ("Select ".concat(dcConf.columns)
                .concat(" from ").concat(dcConf.table))
            }
          }

          LOG.debug("Executing Query:" + queryToExec);
          statement.execute(queryToExec)

          LOG.debug("Executed Query....");

          resultset = statement.getResultSet
          resultSetMetaData = resultset.getMetaData

          var cntr: Long = 0

          breakable {
            LOG.debug("Start execution at " + new Date)

            while (resultset.next) {
              val readTmNs = System.nanoTime
              val readTmMs = System.currentTimeMillis

              var cols: Int = 0

              var listData = new ArrayList[Object]
              var map  = scala.collection.mutable.Map[String, Object]

              for (cols <- 1 to resultSetMetaData.getColumnCount) {
                if (resultSetMetaData.getColumnName(cols).equalsIgnoreCase(dcConf.partitionColumn))
                  uniqueValue.PrimaryKeyValue = resultset.getObject(cols).toString()

                if (resultSetMetaData.getColumnName(cols).equalsIgnoreCase(dcConf.temporalColumn))
                  uniqueValue.AddedDate = resultset.getTimestamp(cols)

                /*
                if (dcConf.formatOrInputAdapterName.equalsIgnoreCase("CSV")) {
                  listData.add(resultset.getObject(cols))
                } else if (dcConf.formatOrInputAdapterName.equalsIgnoreCase("JSON") || dcConf.formatOrInputAdapterName.equalsIgnoreCase("XML")) {
                  map + (resultSetMetaData.getColumnName(cols) -> resultset.getObject(cols))
                } else {
                  //Handle other formats
                  map + (resultSetMetaData.getColumnName(cols) -> resultset.getObject(cols))
                }
                */
              }

              var sb = new StringBuilder;
              /*
              if (dcConf.formatOrInputAdapterName.equalsIgnoreCase("CSV")) {
                csvPrinter.printRecord(listData)
                sb.append(stringWriter.getBuffer.toString())
                LOG.debug("CSV Message - " + sb.toString())
                listData.clear()
                stringWriter.getBuffer.setLength(0)
              } else if (dcConf.formatOrInputAdapterName.equalsIgnoreCase("JSON")) {
                sb.append(JSONValue.toJSONString(map))
                LOG.debug("JSON Message - " + sb.toString())
                map.empty
              } else {
                //Handle other types
                if (dcConf.formatOrInputAdapterName.equalsIgnoreCase("KV") || dcConf.formatOrInputAdapterName.equalsIgnoreCase("Delimited")) {
                  //Need to see if the same logic applies for both KV and Delimited
                  var i: Int = 0;
                  for ((k, v) <- map) {
                    sb.append(k)
                    sb.append(dcConf.keyAndValueDelimiter)
                    sb.append(v)
                    i += 1
                    if (i != map.size) {
                      sb.append(dcConf.fieldDelimiter)
                    }
                  }
                  LOG.debug("Delimited/KV Message - " + sb.toString())
                }
              }
              */
              execThread.execute(sb.toString().getBytes, uniqueKey, uniqueValue, readTmNs)

              cntr += 1

              if (executor.isShutdown) {
                break
              }
            }
          }
          //breakable ends here
          LOG.debug("Complete execution at " + new Date)

          try {
            if (resultset != null) {
              resultset.close
              resultset = null
            }
            if (statement != null) {
              statement.close
              statement = null
            }
            if (preparedStatement != null) {
              preparedStatement.close
              preparedStatement = null
            }
            if (connection != null) {
              connection.close
              connection = null
            }
          } catch {
            case exc: SQLException => LOG.error("Error while closing resources ".concat(exc.getMessage))
          } finally {
            try {
              if (resultset != null) {
                resultset.close
                resultset = null
              }
              if (statement != null) {
                statement.close
                statement = null
              }
              if (preparedStatement != null) {
                preparedStatement.close
                preparedStatement = null
              }
              if (connection != null) {
                connection.close
                connection = null
              }
            } catch {
              case exc: SQLException => LOG.error("Error while closing resources ".concat(exc.getMessage))
            }
          }

        }
      })
    } catch {
      case e: Exception => {
        printFailure(e)
        LOG.error("Failed to setup Streams. Reason:%s Message:%s".format(e.getCause, e.getMessage))
      }
    }
  }

  override def GetAllPartitionUniqueRecordKey: Array[PartitionUniqueRecordKey] = lock.synchronized {
    GetAllPartitionsUniqueKeys
  }

  private def GetAllPartitionsUniqueKeys: Array[PartitionUniqueRecordKey] = lock.synchronized {
    val uniqueKey = new DbPartitionUniqueRecordKey
    Array[PartitionUniqueRecordKey](uniqueKey)
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