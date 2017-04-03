/*
 * Copyright 2015 ligaDATA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ligadata.outputadapters

import java.sql.DriverManager
import java.sql.{ Statement, PreparedStatement, CallableStatement, DatabaseMetaData, ResultSet }
import java.sql.Connection
import java.sql.DatabaseMetaData;
import com.ligadata.KamanjaBase.NodeContext
import java.nio.ByteBuffer
import com.ligadata.kamanja.metadata.AdapterInfo
import org.apache.logging.log4j._
import com.ligadata.Exceptions._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import com.ligadata.Utils.{ KamanjaLoaderInfo }
import java.util.{ Date, Calendar, TimeZone }
import java.text.SimpleDateFormat
import java.io.File
import java.net.{ URL, URLClassLoader }
import scala.collection.mutable.TreeSet
import java.sql.{ Driver, DriverPropertyInfo, SQLException }
import java.sql.Timestamp
import java.util.Properties
import org.apache.commons.dbcp2.BasicDataSource
import java.sql.Types

import com.ligadata.Utils.EncryptDecryptUtils

class JdbcClassLoader(urls: Array[URL], parent: ClassLoader) extends URLClassLoader(urls, parent) {
  override def addURL(url: URL) {
    super.addURL(url)
  }
}

class OraDriverShim(d: Driver) extends Driver {
  private var driver: Driver = d

  def connect(u: String, p: Properties): Connection = this.driver.connect(u, p)

  def acceptsURL(u: String): Boolean = this.driver.acceptsURL(u)

  def getPropertyInfo(u: String, p: Properties): Array[DriverPropertyInfo] = this.driver.getPropertyInfo(u, p)

  def getMajorVersion(): Int = this.driver.getMajorVersion

  def getMinorVersion(): Int = this.driver.getMinorVersion

  def jdbcCompliant(): Boolean = this.driver.jdbcCompliant()

  def getParentLogger(): java.util.logging.Logger = this.driver.getParentLogger()
}

class OracleAdapter(val kvManagerLoader: KamanjaLoaderInfo, val datastoreConfig: String, val nodeCtxt: NodeContext, val adapterInfo: AdapterInfo) {

  private[this] val lock = new Object

  val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

  val adapterConfig = if (datastoreConfig != null) datastoreConfig.trim else ""
  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)

  var _getOps:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _getObjs:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _getBytes:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putObjs:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putOps:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putBytes:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()

  var columnTypes: scala.collection.mutable.Map[String, String] = new scala.collection.mutable.HashMap()
  var columnLists: scala.collection.mutable.Map[String, Array[String]] = new scala.collection.mutable.HashMap()
  var columnTypesFetched: scala.collection.mutable.Map[String, Int] = new scala.collection.mutable.HashMap()


  private val loadedJars: TreeSet[String] = new TreeSet[String];
  private val clsLoader = new JdbcClassLoader(ClassLoader.getSystemClassLoader().asInstanceOf[URLClassLoader].getURLs(), getClass().getClassLoader())

  private var containerList: scala.collection.mutable.Set[String] = scala.collection.mutable.Set[String]()
  private var msg: String = ""

  private def CreateConnectionException(msg: String, ie: Exception): StorageConnectionException = {
    logger.error(msg, ie)
    val ex = new StorageConnectionException("Failed to connect to Database", ie)
    ex
  }

  private def CreateDMLException(msg: String, ie: Exception): StorageDMLException = {
    logger.error(msg, ie)
    val ex = new StorageDMLException("Failed to execute select/insert/delete/update operation on Database", ie)
    ex
  }

  private def CreateDDLException(msg: String, ie: Exception): StorageDDLException = {
    logger.error(msg, ie)
    val ex = new StorageDDLException("Failed to execute create/drop operations on Database", ie)
    ex
  }

  if (adapterConfig.size == 0) {
    msg = "Invalid Oracle Json Configuration string:" + adapterConfig
    throw CreateConnectionException(msg, new Exception("Invalid Configuration"))
  }

  logger.debug("Oracle configuration:" + adapterConfig)
  var parsed_json: Map[String, Any] = null
  try {
    val json = parse(adapterConfig)
    if (json == null || json.values == null) {
      msg = "Failed to parse Oracle JSON configuration string:" + adapterConfig
      throw CreateConnectionException(msg, new Exception("Invalid Configuration"))
    }
    parsed_json = json.values.asInstanceOf[Map[String, Any]]
  } catch {
    case e: Exception => {
      var msg = "Failed to parse Oracle JSON configuration string:%s.".format(adapterConfig)
      throw CreateConnectionException(msg, e)
    }
  }

  // Getting AdapterSpecificConfig if it has
  var adapterSpecificConfig_json: Map[String, Any] = null

  if (parsed_json.contains("AdapterSpecificConfig")) {
    val adapterSpecificStr = parsed_json.getOrElse("AdapterSpecificConfig", "").toString.trim
    if (adapterSpecificStr.size > 0) {
      try {
        val json = parse(adapterSpecificStr)
        if (json == null || json.values == null) {
          msg = "Failed to parse Oracle Adapter Specific JSON configuration string:" + adapterSpecificStr
          throw CreateConnectionException(msg, new Exception("Invalid Configuration"))
        }
        adapterSpecificConfig_json = json.values.asInstanceOf[Map[String, Any]]
      } catch {
        case e: Exception => {
          msg = "Failed to parse Oracle Adapter Specific JSON configuration string:%s.".format(adapterSpecificStr)
          throw CreateConnectionException(msg, e)
        }
      }
    }
  }

  // Read all oracle parameters
  var hostname: String = null;
  if (parsed_json.contains("hostname")) {
    hostname = parsed_json.get("hostname").get.toString.trim
  } else {
    throw CreateConnectionException("Unable to find hostname in adapterConfig ", new Exception("Invalid adapterConfig"))
  }

  var user: String = null;
  if (parsed_json.contains("user")) {
    user = parsed_json.get("user").get.toString.trim
  } else {
    throw CreateConnectionException("Unable to find user in adapterConfig ", new Exception("Invalid adapterConfig"))
  }

  var sid: String = null;
  if (parsed_json.contains("instancename")) {
    sid = parsed_json.get("instancename").get.toString.trim
  }
  else {
    throw CreateConnectionException("Unable to find instancename in adapterConfig ", new Exception("Invalid adapterConfig"))
  }

  var portNumber: String = null;
  if (parsed_json.contains("portnumber")) {
    portNumber = parsed_json.get("portnumber").get.toString.trim
  }
  else{
    portNumber = "1521";
  }

  var schemaName: String = null;
  if (parsed_json.contains("schemaName")) {
    schemaName = parsed_json.get("schemaName").get.toString.trim
  } else {
    logger.info("The schemaName is not supplied in adapterConfig, defaults to " + user)
    schemaName = user
  }

  var password: String = null;
  if ( parsed_json.contains("encryptedEncodedPassword")) {
      val encryptedPassword = parsed_json.get("encryptedEncodedPassword").get.toString.trim
      if ( parsed_json.contains("privateKeyFile")) {
	val privateKeyFile = parsed_json.get("privateKeyFile").get.toString.trim
	if ( parsed_json.contains("encryptDecryptAlgorithm")) {
	  val algorithm = parsed_json.get("encryptDecryptAlgorithm").get.toString.trim
	  password = EncryptDecryptUtils.getDecryptedPassword(encryptedPassword,privateKeyFile,algorithm);
	}
	else{
	  throw CreateConnectionException("Unable to find encryptDecryptAlgorithm in adapterConfig ", new Exception("Invalid adapterConfig"))
	}
      }
      else{
	throw CreateConnectionException("Unable to find privateKeyFile in adapterConfig ", new Exception("Invalid adapterConfig"))
      }
  }
  else if ( parsed_json.contains("encodedPassword")) {
    val encodedPassword = parsed_json.get("encodedPassword").get.toString.trim
    password = EncryptDecryptUtils.decode(encodedPassword.getBytes);
  }
  else if (parsed_json.contains("password")) {
    password = parsed_json.get("password").get.toString.trim
  } 
  else{
    throw CreateConnectionException("Unable to find encrypted or encoded or text password in adapterConfig ", new Exception("Invalid adapterConfig"))
  }

  var jarpaths: String = null;
  if (parsed_json.contains("jarpaths")) {
    jarpaths = parsed_json.get("jarpaths").get.toString.trim
  } else {
    throw CreateConnectionException("Unable to find jarpaths in adapterConfig ", new Exception("Invalid adapterConfig"))
  }

  var jdbcJar: String = null;
  if (parsed_json.contains("jdbcJar")) {
    jdbcJar = parsed_json.get("jdbcJar").get.toString.trim
  } else {
    throw CreateConnectionException("Unable to find jdbcJar in adapterConfig ", new Exception("Invalid adapterConfig"))
  }

  // The following three properties are used for connection pooling
  var maxActiveConnections = 1000
  if (parsed_json.contains("maxActiveConnections")) {
    maxActiveConnections = parsed_json.get("maxActiveConnections").get.toString.trim.toInt
  }

  var maxIdleConnections = 10
  if (parsed_json.contains("maxIdleConnections")) {
    maxIdleConnections = parsed_json.get("maxIdleConnections").get.toString.trim.toInt
  }

  var initialSize = 1
  if (parsed_json.contains("initialSize")) {
    initialSize = parsed_json.get("initialSize").get.toString.trim.toInt
  }

  var maxWaitMillis = 10000
  if (parsed_json.contains("maxWaitMillis")) {
    maxWaitMillis = parsed_json.get("maxWaitMillis").get.toString.trim.toInt
  }

  var autoCreateTables = "YES"
  if (parsed_json.contains("autoCreateTables")) {
    autoCreateTables = parsed_json.get("autoCreateTables").get.toString.trim
  }

  var externalDDLOnly = "NO"
  if (parsed_json.contains("externalDDLOnly")) {
    externalDDLOnly = parsed_json.get("externalDDLOnly").get.toString.trim
  }

  // some misc optional parameters
  var appendOnly = "YES"
  if (parsed_json.contains("appendOnly")) {
    appendOnly = parsed_json.get("appendOnly").get.toString.trim
  }

  var jdbcUrl = "jdbc:oracle:thin:@" + hostname + ":" + portNumber + ":" + sid;


  logger.info("hostname => " + hostname)
  logger.info("username => " + user)
  logger.info("schemaName => " + schemaName)
  logger.info("jarpaths => " + jarpaths)
  logger.info("jdbcJar  => " + jdbcJar)
  logger.info("jdbcUrl  => " + jdbcUrl)
  logger.info("autoCreateTables  => " + autoCreateTables)
  logger.info("externalDDLOnly  => " + externalDDLOnly)
  logger.info("appendOnly  => " + appendOnly)


  var jars = new Array[String](0)
  var jar = jarpaths + "/" + jdbcJar
  jars = jars :+ jar

  val driverType = "oracle.jdbc.OracleDriver";
  try {
    logger.info("Loading the Driver..")
    LoadJars(jars)
    val d = Class.forName(driverType, true, clsLoader).newInstance.asInstanceOf[Driver]
    logger.info("Registering Driver.." + d)
    DriverManager.registerDriver(new OraDriverShim(d));
  } catch {
    case e: Exception => {
      msg = "Failed to load/register jdbc driver name:%s.".format(driverType)
      throw CreateConnectionException(msg, e)
    }
  }

  val userprops = new Properties();
  userprops.setProperty("user", user);
  userprops.setProperty("password", password);

  // setup connection pooling using apache-commons-dbcp2
  logger.info("Setting up jdbc connection pool ..")
  var dataSource: BasicDataSource = null
  try {
    dataSource = new BasicDataSource
    dataSource.setDriverClassName(driverType);
    dataSource.setUrl(jdbcUrl)
    dataSource.setUsername(user)
    dataSource.setPassword(password)
    dataSource.setMaxTotal(maxActiveConnections);
    dataSource.setMaxIdle(maxIdleConnections);
    dataSource.setInitialSize(initialSize);
    dataSource.setMaxWaitMillis(maxWaitMillis);
    dataSource.setTestOnBorrow(true);
    dataSource.setValidationQuery("SELECT 1 from dual");
    //dataSource.getConnection();
  } catch {
    case e: Exception => {
      msg = "Failed to setup connection pooling using apache-commons-dbcp."
      throw CreateConnectionException(msg, e)
    }
  }
  logger.info("Done Setting up jdbc connection pool ..")

  // set the timezone to UTC for all time values
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

  // return the date time in the format yyyy-MM-dd HH:mm:ss.SSS
  private def GetCurDtTmStr: String = {
    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date(System.currentTimeMillis))
  }

  private def updateOpStats(operation: String, tableName: String, opCount: Int) : Unit = lock.synchronized{
    operation match {
      case "get" => {
	if( _getOps.get(tableName) != None ){
	  _getOps(tableName) = _getOps(tableName) + opCount
	}
	else{
	  _getOps(tableName) = + opCount
	}
      }
      case "put" => {
	if( _putOps.get(tableName) != None ){
	  _putOps(tableName) = _putOps(tableName) + opCount
	}
	else{
	  _putOps(tableName) = opCount
	}	  
      }
      case _ => {
        throw CreateDMLException("Internal Error: Failed to Update Op-stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

  private def updateObjStats(operation: String, tableName: String, objCount: Int) : Unit = lock.synchronized{
    operation match {
      case "get" => {
	if( _getObjs.get(tableName) != None ){
	  _getObjs(tableName) = _getObjs(tableName) + objCount
	}
	else{
	  _getObjs(tableName) = + objCount
	}
      }
      case "put" => {
	if( _putObjs.get(tableName) != None ){
	  _putObjs(tableName) = _putObjs(tableName) + objCount
	}
	else{
	  _putObjs(tableName) = objCount
	}	  
      }
      case _ => {
        throw CreateDMLException("Internal Error: Failed to Update Obj-stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

  private def updateByteStats(operation: String, tableName: String, byteCount: Int) : Unit = lock.synchronized{
    operation match {
      case "get" => {
	if( _getBytes.get(tableName) != None ){
	  _getBytes(tableName) = _getBytes(tableName) + byteCount
	}
	else{
	  _getBytes(tableName) = byteCount
	}
      }
      case "put" => {
	if( _putBytes.get(tableName) != None ){
	  _putBytes(tableName) = _putBytes(tableName) + byteCount
	}
	else{
	  _putBytes(tableName) = byteCount
	}
      }
      case _ => {
        throw CreateDMLException("Internal Error: Failed to Update Byte Stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

  private def getConnection: Connection = {
    try {
      //var con = dataSource.getConnection();
      // Pool Creation using dbcp2 library is creating problems
      // So obtaining the connection without using the pool
      val con = DriverManager.getConnection(jdbcUrl, userprops);
      con
    } catch {
      case e: Exception => {
        var msg = "Message:%s".format(e.getMessage)
        throw CreateConnectionException(msg, e)
      }
    }
  }

  /**
   * loadJar - load the specified jar into the classLoader
   */

  private def LoadJars(jars: Array[String]): Unit = {
    // Loading all jars
    for (j <- jars) {
      val jarNm = j.trim
      logger.debug("%s:Processing Jar: %s".format(GetCurDtTmStr, jarNm))
      val fl = new File(jarNm)
      if (fl.exists) {
        try {
          if (loadedJars(fl.getPath())) {
            logger.info("%s:Jar %s already loaded to class path.".format(GetCurDtTmStr, jarNm))
          } else {
            clsLoader.addURL(fl.toURI().toURL())
            logger.info("%s:Jar %s added to class path.".format(GetCurDtTmStr, jarNm))
            loadedJars += fl.getPath()
          }
        } catch {
          case e: Exception => {
            val errMsg = "Jar " + jarNm + " failed added to class path."
            throw CreateConnectionException(errMsg, e)
          }
        }
      } else {
        val errMsg = "Jar " + jarNm + " not found"
        throw new Exception(errMsg)
      }
    }
  }

  def toTableName(containerName: String): String = {
    // Ideally, we need to check for all restrictions for naming a table
    // such as length of the table, special characters etc
    // containerName.replace('.','_')
    containerName.toLowerCase.replace('.', '_').replace('-', '_')
  }

  def toFullTableName(containerName: String): String = {
    schemaName + "." + toTableName(containerName)
  }

  // accessor used for testing
  def getTableName(containerName: String): String = {
    toTableName(containerName)
  }


  private def IsTableExists(schemaName: String, tableName:String): Boolean = {
    var con: Connection = null
    var pstmt: PreparedStatement = null
    var rs: ResultSet = null
    var rowCount = 0
    var query = ""
    try {
      con = getConnection
      query = "SELECT count(*) FROM dba_tables WHERE owner = upper(?) and table_name = upper(?)"
      pstmt = con.prepareStatement(query)
      pstmt.setString(1, schemaName)
      pstmt.setString(2, tableName)
      rs = pstmt.executeQuery();
      while (rs.next()) {
        rowCount = rs.getInt(1)
      }
      if (rowCount > 0) {
        return true
      } else {
        return false
      }
    } catch {
      case e: StorageConnectionException => {
        throw e
      }
      case e: Exception => {
        throw new Exception("Failed to verify schema existence for the schema " + schemaName + ":" + "query => " + query, e)
      }
    } finally {
      if (rs != null) {
        rs.close
      }
      if (pstmt != null) {
        pstmt.close
      }
      if (con != null) {
        con.close
      }
    }
  }

  private def IsIndexExists(schemaName: String, tableName:String, keyColumns:Array[String]): Boolean = {
    var con: Connection = null
    var pstmt: PreparedStatement = null
    var rs: ResultSet = null
    var rowCount = 0;
    var indexName = "";
    var query = ""
    var colStr = new StringBuilder();
    keyColumns.foreach(k => { 
      colStr.append("'");
      colStr.append(k.toUpperCase())
      colStr.append("',");
    })
    colStr = colStr.dropRight(1);

    try {
      con = getConnection
      query = "SELECT index_name, count(*) FROM dba_ind_columns WHERE table_owner = upper(?) and table_name = upper(?) and column_name in ( " + colStr + ") group by index_name having count(*) = ? "
      pstmt = con.prepareStatement(query)
      pstmt.setString(1, schemaName)
      pstmt.setString(2, tableName)
      pstmt.setLong(3, keyColumns.length)
      rs = pstmt.executeQuery();
      while (rs.next()) {
	rowCount += 1;
        indexName = rs.getString(1)
      }
      if (rowCount > 0) {
	logger.info("The index %s with the columns %s already exist".format(indexName,colStr));
        return true
      } else {
        return false
      }
    } catch {
      case e: StorageConnectionException => {
        throw e
      }
      case e: Exception => {
        throw new Exception("Failed to verify schema existence for the schema " + schemaName + ":" + "query => " + query, e)
      }
    } finally {
      if (rs != null) {
        rs.close
      }
      if (pstmt != null) {
        pstmt.close
      }
      if (con != null) {
        con.close
      }
    }
  }

  def createIndex(containerName: String, keyColumns: Array[String], apiType:String): Unit = {
    var con: Connection = null
    var stmt: Statement = null
    var tableName = toTableName(containerName)
    val indexName = tableName + "_pk";
    var fullTableName = toTableName(containerName)
    var query = new StringBuilder();
    try {
      con = getConnection
      // check if the container already exists
      if( IsIndexExists(schemaName,tableName,keyColumns) ){
        logger.debug("A primary key on " + tableName + " already exists ")
      } else {
        if (autoCreateTables.equalsIgnoreCase("NO")) {
          apiType match {
            case "dml" => {
              throw new Exception("The option autoCreateTables is set to NO, So Can't create non-existent index automatically to support the requested DML operation")
            }
            case _ => {
              logger.info("proceed with creating index..")
            }
          }
        }
	if( keyColumns.length == 0 ){
          throw new Exception("The keyColumns must have atleast one entry");
	}
        query.append("create unique index ");
	query.append(indexName);
	query.append(" on ");
	query.append(tableName);
	query.append("(");
	var i = 0;
	for( i  <-  0 to  keyColumns.length - 1){
	  val col = keyColumns(i);
	  query.append(col);
	  query.append(",");
	}
	query = query.dropRight(1);
	query.append(")");
	
	logger.info("query => %s".format(query.toString()));

        stmt = con.createStatement()
        stmt.executeUpdate(query.toString());
        stmt.close
      }
    } catch {
      case e: Exception => {
        throw CreateDDLException("Failed to create index " + indexName + ": ddl = " + query.toString(), e)
      }
    } finally {
      if (stmt != null) {
        stmt.close
      }
      if (con != null) {
        con.close
      }
    }
  }

  def createTable(containerName: String, columnNamesAndTypes: Array[(String,String)], keyColumns: Array[String], apiType:String): Unit = {
    var con: Connection = null
    var stmt: Statement = null
    var tableName = toTableName(containerName)
    var fullTableName = toTableName(containerName)
    var query = new StringBuilder();
    try {
      logger.debug("setup database connection...");
      con = getConnection
      logger.debug("done setting up database connection");
      // check if the container already exists
      if( IsTableExists(schemaName,tableName) ){
        logger.debug("The table " + tableName + " already exists ")
      } else {
        if (autoCreateTables.equalsIgnoreCase("NO")) {
          apiType match {
            case "dml" => {
              throw new Exception("The option autoCreateTables is set to NO, So Can't create non-existent table automatically to support the requested DML operation")
            }
            case _ => {
              logger.info("proceed with creating table..")
            }
          }
        }
	if( columnNamesAndTypes.length == 0 ){
          throw new Exception("The parameters columnNamesAndTypes should have atleast one entry");
	}
        query.append("create table ");
	query.append(fullTableName)
	query.append("(");
	var i = 0;
	for( i  <-  0 to  columnNamesAndTypes.length - 1){
	  val col = columnNamesAndTypes(i)._1;
	  val typ = columnNamesAndTypes(i)._2;
	  query.append(col);
	  query.append(" ");
	  query.append(typ);
	  query.append(",");
	}
	query = query.dropRight(1);
	query.append(")");
	
	logger.info("query => %s".format(query.toString()));

        stmt = con.createStatement()
        stmt.executeUpdate(query.toString());
        stmt.close;

	// create primary key as well
	if( keyColumns != null && keyColumns.length > 0 ){
	  createIndex(containerName, keyColumns, apiType);
	}
      }
    } catch {
      case e: Exception => {
        throw CreateDDLException("Failed to create table or index " + tableName + ": ddl = " + query.toString(), e)
      }
    } finally {
      if (stmt != null) {
        stmt.close
      }
      if (con != null) {
        con.close
      }
    }
  }

  private def getColumnTypes(tableName: String) : Unit =  {
    var con:Connection = null;
    var rs: ResultSet = null
    if( columnTypesFetched.contains(tableName) ){
      logger.debug("The columnTypes are already fetched from database");
      return;
    }
    try{
      con = getConnection;
      val meta = con.getMetaData();
      rs = meta.getColumns(null,null,tableName.toUpperCase(),null);
      var colCount = 0;
      var colNames = new Array[String](0);
      while( rs.next() ){
	val colName = rs.getString("COLUMN_NAME");
	colNames = colNames :+ colName;
	val colType = rs.getString("TYPE_NAME");
	val key = tableName.toUpperCase() + "." + colName.toUpperCase();
	columnTypes(key) = colType;
	colCount = colCount + 1;
      }
      columnLists(tableName.toUpperCase()) = colNames;
      logger.debug("Found %d columns in the table %s".format(colCount,tableName));
      if( colCount > 0 ){
	columnTypesFetched(tableName) = 1;
      }
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to fetch column types for the  " + tableName , e)
      }
    } finally {
      if (rs != null) {
        rs.close
      }
      if (con != null) {
        con.close
      }
    }
  }

  // delete can be improved to pass only key values
  // Assumption: rowColumnValues contains values for all columns of the table
  // in the same order as the columns in the table
  private def delete(con: Connection, containerName: String, columnNames:Array[String], 
		     rowColumnValues: Array[Array[(String,String)]]): Unit = {
    var pstmt: PreparedStatement = null
    var tableName = toTableName(containerName)
    var query = new StringBuilder();
    var totalRowsDeleted = 0;

    try {
      query.append("delete from ");
      query.append(tableName);
      val whereClauseColumns = columnLists(tableName.toUpperCase());
      query.append(" where ");
      whereClauseColumns.foreach( x => {
	query.append(x);
	query.append(" = ");
	query.append("? and ");
      })
      query = query.dropRight(5);
      logger.info("query => %s".format(query.toString()));
      pstmt = con.prepareStatement(query.toString())
      getColumnTypes(tableName);

      rowColumnValues.foreach( row => {
	var colIndex = 0;
	row.foreach( x => {
	  val colName = x._1;
	  val colValue = x._2;
	  val colType = columnTypes(tableName.toUpperCase() + "." + x._1.toUpperCase());
	  colIndex += 1;
	  logger.debug("colName => %s, colValue => %s, colType => %s, colIndex => %d".format(colName,colValue,colType,colIndex));
          colType match{
	    case "NUMBER" => {
	      val f = java.lang.Double.parseDouble(colValue);
	      pstmt.setDouble(colIndex,f);
	    }
	    case "VARCHAR2" => {
	      pstmt.setString(colIndex,colValue);
	    }
	    case _ => {
	      // more types can be supported without throwing error
	      throw new Exception("The column type %s is not supported by this adapter".format(colType))
	    }
	  }
	})
	pstmt.addBatch();
      })

      logger.debug("Executing bulk delete...")
      var deleteCount = pstmt.executeBatch();
      deleteCount.foreach(cnt => { totalRowsDeleted += cnt });
      if (pstmt != null) {
        pstmt.clearBatch();
        pstmt.close
        pstmt = null;
      }
    } catch {
      case e: Exception => {
        if (con != null) {
          try {
            // rollback has thrown exception in some special scenarios, capture it
            con.rollback()
          } catch {
            case ie: Exception => {
              logger.error("", ie)
            }
          }
        }
        throw CreateDMLException("Failed to save an object in the table " + tableName + ":" + "query => " + query.toString(), e)
      }
    } finally {
      if (pstmt != null) {
        pstmt.close
      }
    }
  }

  // Assumption: rowColumnValues contains values for all columns of the table
  // in the same order as the columns in the table
  // Need to be improved..
  def put(containerName: String, columnNames:Array[String],
	  rowColumnValues: Array[Array[(String,String)]]): Unit = {
    var con: Connection = null
    var pstmt: PreparedStatement = null
    var tableName = toTableName(containerName)
    var query = new StringBuilder();
    var values_str = new StringBuilder("values(");
    var totalRowsInserted = 0;

    try {
      con = getConnection
      // we need to commit entire batch
      con.setAutoCommit(false)
      getColumnTypes(tableName);

      val insertColumns = columnLists(tableName.toUpperCase());

      query.append("insert into ");
      query.append(tableName);
      query.append("(");
      insertColumns.foreach( x => {
	query.append(x);
	query.append(",");
	values_str.append("?,");
      })
      query = query.dropRight(1);
      query.append(")")
      values_str = values_str.dropRight(1);
      query.append(values_str);
      query.append(")");

      logger.info("query => %s".format(query.toString()));
      pstmt = con.prepareStatement(query.toString())


      rowColumnValues.foreach( row => {
	var colIndex = 0;
	row.foreach( x => {
	  val colName = x._1;
	  val colValue = x._2;
	  val colType = columnTypes(tableName.toUpperCase() + "." + x._1.toUpperCase());
	  colIndex += 1;
	  logger.debug("colName => %s, colValue => %s, colType => %s, colIndex => %d".format(colName,colValue,colType,colIndex));
          colType match{
	    case "NUMBER" => {
	      val f = java.lang.Double.parseDouble(colValue);
	      pstmt.setDouble(colIndex,f);
	    }
	    case "VARCHAR2" => {
	      pstmt.setString(colIndex,colValue);
	    }
	    case _ => {
	      // more types can be supported without throwing error
	      throw new Exception("The column type %s is not supported by this adapter".format(colType))
	    }
	  }
	})
	pstmt.addBatch();
      })

      if( appendOnly.equalsIgnoreCase("NO") ){
	// delete the rows before inserting
	delete(con,containerName,columnNames,rowColumnValues);
      }

      logger.debug("Executing bulk insert...")
      var insertCount = pstmt.executeBatch();
      insertCount.foreach(cnt => { totalRowsInserted += cnt });
      if (pstmt != null) {
        pstmt.clearBatch();
        pstmt.close
        pstmt = null;
      }
      con.commit();
    } catch {
      case e: Exception => {
        if (con != null) {
          try {
            // rollback has thrown exception in some special scenarios, capture it
            con.rollback()
          } catch {
            case ie: Exception => {
              logger.error("", ie)
            }
          }
        }
        throw CreateDMLException("Failed to save an object in the table " + tableName + ":" + "query => " + query.toString(), e)
      }
    } finally {
      if (pstmt != null) {
        pstmt.close
      }
      if (con != null) {
        con.close
      }
    }
  }

  private def processCell(tableName:String, rowNumber:Int,columnName: String, columnValue: String,callbackFunction: (String, Int, String, String) => Unit) {
    try {
      if (callbackFunction != null)
        (callbackFunction)(tableName, rowNumber, columnName,columnValue)
    } catch {
      case e: Exception => {
        throw e
      }
    }
  }

  def get(containerName: String, selectList: Array[String], filterColumns:Array[(String,String)], callbackFunction: (String, Int, String, String) => Unit): Unit = {
    var con: Connection = null
    var pstmt: PreparedStatement = null
    var rs: ResultSet = null
    var tableName = toTableName(containerName)
    var query = new StringBuilder();
    query = new StringBuilder("select ");
    try{
      getColumnTypes(tableName);
      var selectColumns = selectList;
      if( selectList == null ){
	selectColumns = columnLists(tableName.toUpperCase());
      }
      selectColumns.foreach(s => {
	query.append(s);
	query.append(",");
      })
      query = query.dropRight(1);// drop last comma
      query.append(" from ")
      query.append(tableName)
      if( filterColumns != null ){
	query.append(" where ");
	filterColumns.foreach(f => {
	  val colName = f._1;
	  query.append(colName);
	  query.append(" = ? and ");
	})
	query = query.dropRight(4); // drop last "and "
      }
      con = getConnection
      pstmt = con.prepareStatement(query.toString());
      var colIndex = 0;
      if( filterColumns != null ){
	filterColumns.foreach(f => {
	  colIndex += 1;
	  val colName = f._1;
	  val colValue = f._2;
	  val colType = columnTypes(tableName.toUpperCase() + "." + colName.toUpperCase());
	  logger.debug("Setting up where clause: colName => %s, colValue => %s, colType => %s, colIndex => %d".format(colName,colValue,colType,colIndex));
          colType match{
	    case "NUMBER" => {
	      val f = java.lang.Double.parseDouble(colValue);
	      pstmt.setDouble(colIndex,f);
	    }
	    case "VARCHAR2" => {
	      pstmt.setString(colIndex,colValue);
	    }
	    case _ => {
	      // more types can be supported without throwing error
	      throw new Exception("The column type %s is not supported by this adapter".format(colType))
	    }
	  }
	})
      }
      rs = pstmt.executeQuery();
      var rowCount = 1;
      while (rs.next()) {
	logger.debug("Fetched a row rowCount => %d. process it...".format(rowCount));
	colIndex = 0;
	for( i <- 0 to selectColumns.length - 1 ){
	  val colName = selectColumns(i);
	  val colType = columnTypes(tableName.toUpperCase() + "." + colName.toUpperCase());
	  var colValue: String = "";
	  colIndex += 1;
	  logger.debug("Fetching values from result set: colName => %s, colType => %s, colIndex => %d".format(colName,colType,colIndex));
          colType match{
	    case "NUMBER" => {
	      colValue = rs.getLong(colIndex).toString();
	    }
	    case "VARCHAR2" => {
	      colValue = rs.getString(colIndex).toString();
	    }
	    case _ => {
	      // more types can be supported without throwing error
	      throw new Exception("The column type %s is not supported by this adapter".format(colType))
	    }
	  }
	  logger.debug("Fetched a value from result set: colName => %s, colType => %s, colIndex => %d".format(colName,colType,colIndex));
	  processCell(tableName,rowCount,colName,colValue,callbackFunction);
	}
	rowCount += 1;
      }
    } catch {
      case e: Exception => {
        throw CreateDMLException("Failed to create a query the table " + tableName + ":" + "query => " + query.toString(), e)
      }
    }
  }

  def Shutdown(): Unit = {
    logger.info("close the connection pool")
  }

  private def TruncateContainer(containerName: String): Unit = {
    var con: Connection = null
    var stmt: Statement = null
    var tableName = toFullTableName(containerName)
    var query = new StringBuilder();
    try {
      con = getConnection

      query.append("truncate table ")
      query.append(tableName);
      stmt = con.createStatement()
      stmt.executeUpdate(query.toString());
    } catch {
      case e: Exception => {
        throw CreateDDLException("Failed to fetch data from the table " + tableName + ":" + "query => " + query.toString(), e)
      }
    } finally {
      if (stmt != null) {
        stmt.close
      }
      if (con != null) {
        con.close
      }
    }
  }

  def TruncateContainer(containerNames: Array[String]): Unit = {
    logger.info("truncate the container tables")
    containerNames.foreach(cont => {
      logger.info("truncate the container " + cont)
      TruncateContainer(cont)
    })
  }

  private def dropTable(tableName: String): Unit = lock.synchronized {
    var con: Connection = null
    var stmt: Statement = null
    var rs: ResultSet = null
    var fullTableName = schemaName + "." + tableName
    var query = new StringBuilder();
    try {
      con = getConnection
      // check if the container already dropped
      val dbm = con.getMetaData();
      rs = dbm.getTables(null, schemaName.toUpperCase(), tableName.toUpperCase(), null);
      if (!rs.next()) {
        logger.info("The table " + tableName + " doesn't exist in the schema " + schemaName + "  may have beem dropped already ")
      } else {
        query.append("drop table ");
	query.append(tableName);
        stmt = con.createStatement()
        stmt.executeUpdate(query.toString());
      }
    } catch {
      case e: Exception => {
        throw CreateDDLException("Failed to drop the table " + fullTableName + ":" + "query => " + query.toString(), e)
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

  private def DropContainer(containerName: String): Unit = lock.synchronized {
    var tableName = toTableName(containerName)
    dropTable(tableName)
  }

  def DropContainer(containerNames: Array[String]): Unit = {
    logger.info("drop the container tables")
    containerNames.foreach(cont => {
      logger.info("drop the container " + cont)
      DropContainer(cont)
    })
  }
}
