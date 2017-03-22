

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
import scala.actors.threadpool.{ Executors, ExecutorService }
import scala.util.control.Breaks.{break, breakable}
import org.apache.logging.log4j.LogManager
import com.ligadata.AdaptersConfiguration.{DbAdapterConfiguration, DbPartitionUniqueRecordKey,DbPartitionUniqueRecordValue}
import com.ligadata.InputOutputAdapterInfo.AdapterConfiguration
import com.ligadata.InputOutputAdapterInfo.CountersAdapter
import com.ligadata.InputOutputAdapterInfo.ExecContextObj
import com.ligadata.InputOutputAdapterInfo.InputAdapter
import com.ligadata.InputOutputAdapterInfo.InputAdapterCallerContext
import com.ligadata.InputOutputAdapterInfo.InputAdapterObj
import com.ligadata.InputOutputAdapterInfo.PartitionUniqueRecordKey
import com.ligadata.InputOutputAdapterInfo.PartitionUniqueRecordValue
import com.ligadata.InputOutputAdapterInfo.StartProcPartInfo
import com.ligadata.KamanjaBase.DataDelimiters
import javax.sql.DataSource
import org.apache.commons.dbcp2.BasicDataSource
import java.util.HashMap
import org.apache.commons.csv.CSVFormat
import java.io.StringWriter
import org.apache.commons.csv.CSVPrinter
import java.util.ArrayList
import org.json.simple.JSONObject
import org.json.simple.JSONValue
import com.thoughtworks.xstream.XStream
import com.ligadata.adapters.xstream.CustomMapConverter
import java.util.Arrays


object DbConsumer extends InputAdapterObj {
  val ADAPTER_DESCRIPTION = "JDBC Consumer"
  def CreateInputAdapter(inputConfig: AdapterConfiguration, callerCtxt: InputAdapterCallerContext, execCtxtObj: ExecContextObj, cntrAdapter: CountersAdapter): InputAdapter = new DbConsumer(inputConfig, callerCtxt, execCtxtObj, cntrAdapter)
}

class DbConsumer (val inputConfig: AdapterConfiguration, val callerCtxt: InputAdapterCallerContext, val execCtxtObj: ExecContextObj, cntrAdapter: CountersAdapter) extends InputAdapter {
  private[this] val LOG = LogManager.getLogger(getClass);
  private[this] val dcConf = DbAdapterConfiguration.getAdapterConfig(inputConfig)
  private[this] val lock = new Object()
  
  private[this] val XML_HEADER:String = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
  
  private var startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var metrics: scala.collection.mutable.Map[String,Any] = scala.collection.mutable.Map[String,Any]()
  
  private[this] val kvs = scala.collection.mutable.Map[String, (DbPartitionUniqueRecordKey, DbPartitionUniqueRecordValue, DbPartitionUniqueRecordValue)]()
  
  private[this] var uniqueKey: DbPartitionUniqueRecordKey = new DbPartitionUniqueRecordKey
  uniqueKey.DBUrl = dcConf.dbURL
  uniqueKey.DBName = dcConf.dbName
  uniqueKey.TableName = dcConf.table
  uniqueKey.WhereClause = dcConf.where
  uniqueKey.Columns = dcConf.columns
  uniqueKey.Query = dcConf.query
  
  //DataSource for the connection Pool
  private var dataSource: BasicDataSource = _
  var executor: ExecutorService = _
  val input = this
   
  val execThread = execCtxtObj.CreateExecContext(input, uniqueKey, callerCtxt)
  
  override def Shutdown: Unit = lock.synchronized {
    StopProcessing
  }
  
  override def StopProcessing: Unit = lock.synchronized {
    LOG.debug("Initiating Stop Processing...")
    
    //Shutdown the executor
    if (executor != null){
      executor.shutdownNow
      while (executor.isTerminated == false) {
        Thread.sleep(100) // sleep 100ms and then check
      }
      executor = null
    }else return
    
    
    if(dataSource != null){
      dataSource.close
      dataSource = null
    }else return
    
  }
  
  override def StartProcessing(partitionInfo: Array[StartProcPartInfo], ignoreFirstMsg: Boolean): Unit = lock.synchronized {
    
     
    
    LOG.debug("Initiating Start Processing...")
    
    LOG.debug("Configuration data - "+dcConf);
    
    if (partitionInfo == null || partitionInfo.size == 0)
      return
    
    val delimiters = new DataDelimiters
    delimiters.keyAndValueDelimiter = dcConf.keyAndValueDelimiter
    delimiters.fieldDelimiter = dcConf.fieldDelimiter
    delimiters.valueDelimiter = dcConf.valueDelimiter
    
    // Get the data about the request and set the instancePartition list
    
    val partInfo = partitionInfo.map(quad => { (
         quad._key.asInstanceOf[DbPartitionUniqueRecordKey], 
         quad._val.asInstanceOf[DbPartitionUniqueRecordValue], 
         quad._validateInfoVal.asInstanceOf[DbPartitionUniqueRecordValue]) })

     
    kvs.clear

    partInfo.foreach(quad => {
      kvs(quad._1.DBName) = quad
    })

    LOG.debug("KV Map =>")
    kvs.foreach(kv => {
      LOG.debug("Key:%s => Val:%s".format(kv._2._1.Serialize, kv._2._2.Serialize))
    })
    
    var threads: Int = 1
    
    //TODO Need to see if we can run a thread pool with timeInterval and timeUnits 
    executor = Executors.newFixedThreadPool(threads)
    
    //Create a DBCP based Connection Pool Here
    dataSource = new BasicDataSource
    
    //Force a load of the DB Driver Class
    Class.forName(dcConf.dbDriver)
    LOG.debug("Loaded the DB Driver..."+dcConf.dbDriver)
		
		dataSource.setDriverClassName(dcConf.dbDriver)
		if(dcConf.dbName!=null && !dcConf.dbName.isEmpty())
		  dataSource.setUrl(dcConf.dbURL+"/"+dcConf.dbName)
		else
		  dataSource.setUrl(dcConf.dbURL)
		dataSource.setUsername(dcConf.dbUser);
		dataSource.setPassword(dcConf.dbPwd);
		
		
		dataSource.setTestWhileIdle(false);
		dataSource.setTestOnBorrow(true);
		dataSource.setValidationQuery("Select 1");
		dataSource.setTestOnReturn(false);
		
		dataSource.setMaxTotal(100);
		dataSource.setMaxIdle(5);
		dataSource.setMinIdle(0);
		dataSource.setInitialSize(5);
		dataSource.setMaxWaitMillis(5000);
		
		//var conn:Connection = DriverManager.getConnection(dcConf.dbURL+"/"+dcConf.dbName,dcConf.dbUser,dcConf.dbPwd)
		//var conn:Connection = dataSource.getConnection
		//LOG.debug("Created Connection..."+conn.toString())
				
		LOG.debug("Created DataSource..."+dataSource.toString())
    
    val uniqueValue = new DbPartitionUniqueRecordValue
    
    //Record the last run time on this counter value
    val runIntervalKey:String = Category concat "/" concat  dcConf.Name concat dcConf.dbName
    val lastRun:Long = cntrAdapter.getCntr(runIntervalKey);
    
    try{
      executor.execute(new Runnable() {
        override def run() {
          
          //For CSV Format
          var csvFormat = CSVFormat.DEFAULT.withSkipHeaderRecord().withIgnoreSurroundingSpaces()
          var stringWriter = new StringWriter
          var csvPrinter = new CSVPrinter(stringWriter,csvFormat)
         
          //For XML Format
          var xStream = new XStream
          xStream.registerConverter(new CustomMapConverter(xStream.getMapper))
          
          LOG.debug("Started the executor..."+Thread.currentThread().getName);
          
          var connection:Connection = null
          var statement:Statement = null
          var preparedStatement: PreparedStatement = null
          var resultset:ResultSet = null
          var resultSetMetaData: ResultSetMetaData = null
          
          LOG.debug("Before starting....");
          
          //connection = dataSource.getConnection
          connection = DriverManager.getConnection(dcConf.dbURL+"/"+dcConf.dbName,dcConf.dbUser,dcConf.dbPwd)
          LOG.debug("Got the connection from the datasource");
          
          statement = connection.createStatement
          LOG.debug("Created the statement");
          
          //TODO Dynamic insertion of where clause to include the 
          if(dcConf.query != null && !dcConf.query.isEmpty()){
             statement.execute(dcConf.query)
          }else{
            if(dcConf.where != null && !dcConf.where.isEmpty()){
              statement.execute("Select ".concat(dcConf.columns)
                   .concat(" from ").concat(dcConf.table)
                   .concat(" ").concat(dcConf.where))
            }else{
              statement.execute("Select ".concat(dcConf.columns)
                   .concat(" from ").concat(dcConf.table))
            }  
          }
          
          LOG.debug("Executed Query....");
          
           
          resultset = statement.getResultSet
          resultSetMetaData = resultset.getMetaData
          
          var cntr: Long = 0
          
          breakable{
            
            LOG.debug("Start execution at "+new Date)
            
            while(resultset.next){
              val readTmNs = System.nanoTime
              val readTmMs = System.currentTimeMillis
              
              var cols:Int = 0
              
              var listData = new ArrayList[Object]
              var map :Map[String,Object] = Map()
              
              for(cols <- 1 to resultSetMetaData.getColumnCount){
                 if(resultSetMetaData.getColumnName(cols).equalsIgnoreCase(dcConf.partitionColumn))
                   uniqueValue.PrimaryKeyValue = resultset.getObject(cols).toString()
                 
                 if(resultSetMetaData.getColumnName(cols).equalsIgnoreCase(dcConf.temporalColumn))
                   uniqueValue.AddedDate = resultset.getTimestamp(cols)
                 
                 if(dcConf.formatOrInputAdapterName.equalsIgnoreCase("CSV")){
                    listData.add(resultset.getObject(cols))
                  }else if (dcConf.formatOrInputAdapterName.equalsIgnoreCase("JSON") || dcConf.formatOrInputAdapterName.equalsIgnoreCase("XML")){
                    map + (resultSetMetaData.getColumnName(cols) ->  resultset.getObject(cols))
                  }else{ //Handle other formats
                    map + (resultSetMetaData.getColumnName(cols) ->  resultset.getObject(cols))
                  } 
              }
              
              var sb = new StringBuilder;
              
              if(dcConf.formatOrInputAdapterName.equalsIgnoreCase("CSV")){
                csvPrinter.printRecord(listData)
                sb.append(stringWriter.getBuffer.toString())
                LOG.debug("CSV Message - "+sb.toString())
                listData.clear()
                stringWriter.getBuffer.setLength(0)
              }else if(dcConf.formatOrInputAdapterName.equalsIgnoreCase("JSON")){
                sb.append(JSONValue.toJSONString(map))
                LOG.debug("JSON Message - "+sb.toString())
                map.empty
              }else if(dcConf.formatOrInputAdapterName.equalsIgnoreCase("XML")){
                sb.append(XML_HEADER + xStream.toXML(map))
                LOG.debug("XML Message - "+sb.toString())
                map.empty
              }else{ //Handle other types
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
                  LOG.debug("Delimited/KV Message - "+sb.toString())
                }
              }
              
              execThread.execute(sb.toString().getBytes, dcConf.formatOrInputAdapterName, uniqueKey, uniqueValue, readTmNs, readTmMs, false, dcConf.associatedMsg, delimiters)
                          
              cntr += 1
              LOG.debug("Counter value "+cntr)
              val key = Category concat "/" concat dcConf.Name concat "/evtCnt"
              cntrAdapter.addCntr(key, 1) // for now adding each row
              
              
               if (executor.isShutdown) {
                 break
               }
            }
            
            //Capture the last run time here and push into a counter
            val currentTime:Long = new Date().getTime;
            if(lastRun == 0)
              cntrAdapter.addCntr(runIntervalKey, currentTime)
            else 
              cntrAdapter.addCntr(runIntervalKey, (currentTime - lastRun))
          }
          //breakable ends here
          LOG.debug("Complete execution at "+new Date)
          
          try{
            if(resultset != null){
               resultset.close
              resultset = null
            }
            if(statement != null){
              statement.close
              statement = null
            }
            if(preparedStatement != null){
              preparedStatement.close
              preparedStatement = null
            }
            if(connection != null){
              connection.close
              connection = null
            }
          }catch{
            case exc:SQLException =>  LOG.error("Error while closing resources ".concat(exc.getMessage))
          }finally{
            try{
               if(resultset != null){
                  resultset.close
                  resultset = null
                }
                if(statement != null){
                  statement.close
                  statement = null
                }
                if(preparedStatement != null){
                  preparedStatement.close
                  preparedStatement = null
                }
                if(connection != null){
                  connection.close
                  connection = null
                }
            }catch{
              case exc:SQLException =>  LOG.error("Error while closing resources ".concat(exc.getMessage))
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
      uniqueKey.DBName = dcConf.dbName
      uniqueKey.DBUrl = dcConf.dbURL
      if(dcConf.table != null && !dcConf.table.isEmpty())
        uniqueKey.TableName = dcConf.table
      if(dcConf.query != null && !dcConf.query.isEmpty())
        uniqueKey.Query = dcConf.query
      if(dcConf.columns != null && !dcConf.columns.isEmpty())
        uniqueKey.Columns = dcConf.columns
      if(dcConf.where != null && !dcConf.where.isEmpty())
        uniqueKey.WhereClause = dcConf.where
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
  
  // Not yet implemented
  override def getAllPartitionBeginValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = {
    return Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]()
  }

  // Not yet implemented
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