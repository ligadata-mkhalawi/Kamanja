package com.ligadata.Utilities

import java.sql.{Connection, Statement, ResultSet, DriverManager,DatabaseMetaData, PreparedStatement}

import com.ligadata.AdaptersConfiguration.DbAdapterConfiguration
import com.ligadata.KvBase.{Key, Value}
import com.ligadata.AdaptersConfiguration.LogTrait
import org.apache.commons.dbcp2.BasicDataSource
/**
  * Created by Yousef on 8/29/2016.
  */
class JDBCUtility extends LogTrait {

  private[this] val lock = new Object
  var connection: Connection = null
  val statement: Statement = connection.createStatement()
  var rs: ResultSet = null;
  var schemaName: String = ""
  var _getOps:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _getObjs:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _getBytes:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putObjs:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putOps:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()
  var _putBytes:scala.collection.mutable.Map[String,Long] = new scala.collection.mutable.HashMap()

  def createConnection(AdapterConfig: DbAdapterConfiguration): Unit = {
    if(AdapterConfig.dbName != null && !AdapterConfig.dbName.isEmpty())
      connection = DriverManager.getConnection(AdapterConfig.dbURL+"/"+AdapterConfig.dbName, AdapterConfig.dbUser, AdapterConfig.dbPwd)
    else
      connection = DriverManager.getConnection(AdapterConfig.dbURL, AdapterConfig.dbUser, AdapterConfig.dbPwd)

  }

  def createSchema(schemaName: String): Unit = {
    logger.info("Check if database/schema exists and create it if not..")
    println("Check if database/schema exists and create it if not..")
    val query: String = "create schame if not exists %s".format(schemaName)
    try {
      statement.execute(query)
      rs = statement.getResultSet
      logger.info("Done from check database/schema exists..")
      println("Done from check database/schema exists..")
    } catch {
      case e: Exception => logger.error("Error while creating schema query results..".concat(e.getMessage))
    }
  }

  def CheckTableExists(tblName: String): Unit = {
    logger.info("Check if table exists..")
    println("Check if table exists..")
    val dbmd: DatabaseMetaData = connection.getMetaData
    val tableName = toTableName(tblName)
    rs = dbmd.getTables(null, null, tableName, Array("TABLE"))
    if (rs != null && rs.next()) {
      logger.info("Done from check table exists..")
      println("Done from check table exists..")
    } else{
      val fullTableName = toFullTableName(tableName)
      val query = "create table " + fullTableName + "(timePartition bigint,bucketKey varchar(1024), transactionId bigint, rowId Int, schemaId Int, serializerType varchar(128), serializedInfo BINARY)"
      statement.executeUpdate(query)
      logger.info("table does not exists..")
      logger.info("Creating %s table..".format(fullTableName))
    }
  }

  def executeQuery(query: String): Unit = {
    statement.execute(query)
  }

  private def IsSingleRowPut(data_list: Array[(String, Array[(Key, Value)])]): Boolean = {
    if (data_list.length == 1) {
      if (data_list(0)._2.length == 1) {
        return true
      }
    }
    return false
  }

   def toTableName(tableName: String): String = {
    tableName.toLowerCase.replace('.', '_').replace('-', '_')
  }

  def toFullTableName(tableName: String): String = {
    schemaName + "." + toTableName(tableName)
  }

  private def getKeySize(k: Key): Int = {
    var bucketKeySize = 0
    k.bucketKey.foreach(bk => {
      bucketKeySize = bucketKeySize + bk.length
    })
    8 + bucketKeySize + 8 + 4
  }

  private def getValueSize(v: Value): Int = {
    v.serializedInfo.length
  }

  def put(containerName: String, key: Key, value: Value): Unit = {
    var pstmt: PreparedStatement = null
    val tableName = toFullTableName(containerName)
    var sql = ""
    try {
      CheckTableExists(containerName)
      sql = "if ( not exists(select 1 from " + tableName +
        " where timePartition = ? and bucketKey = ?  and transactionId = ?  and rowId = ? ) ) " +
        " begin " +
        " insert into " + tableName + "(timePartition,bucketKey,transactionId,rowId,schemaId,serializerType,serializedInfo)" +
        " values(?,?,?,?,?,?,?)" +
        " end " +
        " else " +
        " begin " +
        " update " + tableName + " set schemaId = ?,serializerType = ?, serializedInfo = ? where timePartition = ? and bucketKey = ?  and transactionId = ?  and rowId = ?  " +
        " end ";
      logger.debug("sql => " + sql)
      pstmt = connection.prepareStatement(sql)
      pstmt.setLong(1, key.timePartition)
      pstmt.setString(2, key.bucketKey.mkString(","))
      pstmt.setLong(3, key.transactionId)
      pstmt.setInt(4, key.rowId)
      pstmt.setLong(5, key.timePartition)
      pstmt.setString(6, key.bucketKey.mkString(","))
      pstmt.setLong(7, key.transactionId)
      pstmt.setInt(8, key.rowId)
      pstmt.setInt(9, value.schemaId)
      pstmt.setString(10, value.serializerType)
      pstmt.setBinaryStream(11, new java.io.ByteArrayInputStream(value.serializedInfo), value.serializedInfo.length)
      pstmt.setInt(12, value.schemaId)
      pstmt.setString(13, value.serializerType)
      pstmt.setBinaryStream(14, new java.io.ByteArrayInputStream(value.serializedInfo), value.serializedInfo.length)
      pstmt.setLong(15, key.timePartition)
      pstmt.setString(16, key.bucketKey.mkString(","))
      pstmt.setLong(17, key.transactionId)
      pstmt.setInt(18, key.rowId)
      pstmt.executeUpdate();
      updateOpStats("put", tableName, 1)
      updateObjStats("put", tableName, 1)
      updateByteStats("put", tableName, getKeySize(key) + getValueSize(value))
    } catch {
      case e: Exception => {
        if (connection != null) {
          try {
            // rollback has thrown exception in some special scenarios, capture it
            connection.rollback()
          } catch {
            case ie: Exception => {
              logger.error("", ie)
            }
          }
        }
        logger.error("Failed to save an object in the table " + tableName + ":" + "sql => " + sql, e)
      }
    } finally {
      if (pstmt != null) {
        pstmt.close
      }
      if (connection != null) {
        connection.close
      }
    }
  }

  def put(data_list: Array[(String, Array[(Key, Value)])]): Unit = {
    var pstmt: PreparedStatement = null
    var sql: String = null
    var totalRowsUpdated = 0;
    try {
      if (IsSingleRowPut(data_list)) {
        var containerName = data_list(0)._1
        var isMetadataContainer = data_list(0)._2
        var keyValuePairs = data_list(0)._2
        var key = keyValuePairs(0)._1
        var value = keyValuePairs(0)._2
        put(containerName, key, value)
      } else {
        logger.debug("Get a new connection...")
        // we need to commit entire batch
        connection.setAutoCommit(false)
        var byteCount = 0
        data_list.foreach(li => {
          var containerName = li._1
          var tableName = toFullTableName(containerName)
          var keyValuePairs = li._2
          logger.info("Input row count for the table " + tableName + " => " + keyValuePairs.length)
          sql = "if ( not exists(select 1 from " + tableName +
            " where timePartition = ? and bucketKey = ?  and transactionId = ?  and rowId = ? ) ) " +
            " begin " +
            " insert into " + tableName + "(timePartition,bucketKey,transactionId,rowId,schemaId,serializerType,serializedInfo)" +
            " values(?,?,?,?,?,?,?)" +
            " end " +
            " else " +
            " begin " +
            " update " + tableName + " set schemaId = ?,serializerType = ?, serializedInfo = ? where timePartition = ? and bucketKey = ?  and transactionId = ?  and rowId = ?  " +
            " end ";
          logger.debug("sql => " + sql)
          pstmt = connection.prepareStatement(sql)
          keyValuePairs.foreach(keyValuePair => {
            var key = keyValuePair._1
            var value = keyValuePair._2
            pstmt.setLong(1, key.timePartition)
            pstmt.setString(2, key.bucketKey.mkString(","))
            pstmt.setLong(3, key.transactionId)
            pstmt.setInt(4, key.rowId)
            pstmt.setLong(5, key.timePartition)
            pstmt.setString(6, key.bucketKey.mkString(","))
            pstmt.setLong(7, key.transactionId)
            pstmt.setInt(8, key.rowId)
            pstmt.setInt(9, value.schemaId)
            pstmt.setString(10, value.serializerType)
            pstmt.setBinaryStream(11, new java.io.ByteArrayInputStream(value.serializedInfo), value.serializedInfo.length)
            pstmt.setInt(12, value.schemaId)
            pstmt.setString(13, value.serializerType)
            pstmt.setBinaryStream(14, new java.io.ByteArrayInputStream(value.serializedInfo), value.serializedInfo.length)
            pstmt.setLong(15, key.timePartition)
            pstmt.setString(16, key.bucketKey.mkString(","))
            pstmt.setLong(17, key.transactionId)
            pstmt.setInt(18, key.rowId)
            pstmt.addBatch()
            byteCount = byteCount + getKeySize(key) + getValueSize(value)
          })
          logger.debug("Executing bulk upsert...")
          var updateCount = pstmt.executeBatch();
          updateCount.foreach(cnt => {
            totalRowsUpdated += cnt
          });
          if (pstmt != null) {
            pstmt.clearBatch();
            pstmt.close
            pstmt = null;
          }
          updateOpStats("put", tableName, 1)
          updateObjStats("put", tableName, totalRowsUpdated)
          updateByteStats("put", tableName, byteCount)
          logger.info("Inserted/Updated " + totalRowsUpdated + " rows for " + tableName)
        })
        connection.commit()
        connection.close
        connection = null
      }
    } catch {
      case e: Exception => {
        if (connection != null) {
          try {
            // rollback has thrown exception in some special scenarios, capture it
            connection.rollback()
          } catch {
            case ie: Exception => {
              logger.error("", ie)
            }
          }
        }
        logger.error("Failed to save a batch of objects into the table :" + "sql => " + sql, e)
      }
    } finally {
      if (pstmt != null) {
        pstmt.close
        pstmt = null
      }
      if (connection != null) {
        connection.close
      }
    }
  }

   def updateOpStats(operation: String, tableName: String, opCount: Int) : Unit = lock.synchronized{
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
        logger.error("Internal Error: Failed to Update Op-stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

   def updateObjStats(operation: String, tableName: String, objCount: Int) : Unit = lock.synchronized{
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
        logger.error("Internal Error: Failed to Update Obj-stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

   def updateByteStats(operation: String, tableName: String, byteCount: Int) : Unit = lock.synchronized{
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
        logger.error("Internal Error: Failed to Update Byte Stats for " + tableName, new Exception("Invalid operation " + operation))
      }
    }
  }

  def shutDown(): Unit ={
    connection.close()
    statement.close()
    rs.close()
  }
}
