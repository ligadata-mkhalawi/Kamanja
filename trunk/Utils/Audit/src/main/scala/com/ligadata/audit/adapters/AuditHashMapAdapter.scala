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

package com.ligadata.audit.adapters

import com.ligadata.AuditAdapterInfo._
import com.ligadata.kamanja.metadata._
import com.ligadata.Serialize._
import com.ligadata.Exceptions._
import org.apache.logging.log4j._
import java.util.Date
import java.util.Calendar
import java.io.File;
import java.nio.ByteBuffer

import org.mapdb._;

class AuditHashMapAdapter extends AuditAdapter {
  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)
  
  var adapterProperties: scala.collection.mutable.Map[String,String] = scala.collection.mutable.Map[String,String]()
  var db: DB = null
  var map: HTreeMap[Array[Byte],Array[Byte]] = null

  var withTransactions:Boolean = true
  lazy val serializer = SerializerManager.GetSerializer("kryo")

  var path = "."
  var keyspace = "default"
  var table = "default"

  /**
   * init - This is a method that must be implemented by the adapter impl.  This method should preform any necessary
   *        steps to set up the destination of the Audit Records 
   * @param String - class name that contains parameters required to initialize the Datastore connection
   * @return Unit
   */
  override def init (parms: String): Unit = {
    
    if (parms != null) {
      logger.info("HASHMAP AUDIT: Initializing to "+parms)
      initPropertiesFromFile(parms)   
    }

    path = adapterProperties.getOrElse("path", ".")
    keyspace = adapterProperties.getOrElse("schema", "default")
    table = adapterProperties.getOrElse("table", "default")

    val InMemory = adapterProperties.getOrElse("inmemory", "false").toBoolean
    withTransactions = adapterProperties.getOrElse("withtransaction", "false").toBoolean

    if (InMemory == true) {
      db = DBMaker.newMemoryDB()
	.make()
    } else {
      val dir = new File(path);
      if (!dir.exists()){
	// attempt to create the directory here
	dir.mkdir();
      }
      db = DBMaker.newFileDB(new File(path + "/" + keyspace + ".hdb"))
	.closeOnJvmShutdown()
	.mmapFileEnable()
	.transactionDisable()
	.commitFileSyncDisable()
	.make()
    }

    map = db.createHashMap(table)
      .hasher(Hasher.BYTE_ARRAY)
      .makeOrGet[Array[Byte], Array[Byte]]()

    logger.info("HASHMAP AUDIT: Initialized with "+keyspace+"."+path+"."+table)
  }
  
  
  /**
   * getYear - get the 4 digit format of the current year
   */
  private def getYear(dt: Long) : Int = {
    val cal = Calendar.getInstance();
    cal.setTime(new Date(dt));
    val year = cal.get(Calendar.YEAR);
    year
  }    

  private def Commit(tableName: String): Unit = {
    if (withTransactions) {
      db.commit();
    }
  }

  private def Commit: Unit = {
    Commit(table)
  }

  /**
   * addAuditRecord - adds the auditRecord to the audit table.
   */
  def addAuditRecord(rec: AuditRecord) = {
    try{
      logger.debug("Audit Event occured")
      var at:java.lang.Long = rec.actionTime.toLong
      val key = at.toString.getBytes()
      var value = serializer.SerializeObjectToByteArray(rec)
      map.put(key, value)
      // Commit(table)
    }catch {
      case e: Exception => 
        logger.error("", e)
        throw e
    }
  }
  
  /**
   * getAuditRecords - gets the array of Audit Records that exist in the Cassandra audit tables.
   *                   Filters:
   *                       - startTime (if none specified, defaults to 10 minutes prior to the call
   *                       - endTime (if none specified, current time is sued
   *                       - user
   *                       - action
   *                       - objectAccessed
   *                       
   */
  def getAuditRecord(startTime: Date, endTime: Date, userOrRole: String, action: String, objectAccessed: String): Array[AuditRecord] = {
    var auditRecords = new Array[AuditRecord](0)
    var stime = (new Date().getTime() - 10 * 60 * 1000L)
    if( startTime != null ){
      stime = startTime.getTime()
    }

    var etime = new Date().getTime()
    if( endTime != null ){
      etime = endTime.getTime()
    }

    try{
      var iter = map.keySet().iterator()
      while (iter.hasNext()) {
        val key = iter.next()
	val at = new String(key).toLong

	if ( at >= stime && at <= etime ){
	  val value = map.get(key)
	  val ar = serializer.DeserializeObjectFromByteArray(value).asInstanceOf[AuditRecord]
          auditRecords = auditRecords :+ ar
	}
      }
      auditRecords
    } catch {
      case e:Exception => {
        throw new Exception("Failed to fetch audit records", e)
      }
    }
  }

  /**
   * Shutdown - clean up all the resources used by this class.
   */
  override def Shutdown() = {
    if( db != null && db.isClosed() == false ){
      logger.debug("Trying to shutdown hashmap db")
      try{
	db.commit(); //persist changes into disk
	db = null
	map.close();
      }catch{
	case e:NullPointerException =>{
	  logger.error("Unexpected Null pointer exception when closing hashmap, seems like internal bug related to mapdb ", e)
	}
	case e:Exception =>{
	  logger.error("Unexpected error when closing hashmap ", e)
	}
      }
    }
  }

  override def TruncateStore: Unit = {
    try{
      map.clear()
      Commit
    } catch {
      case e: Exception => {
        logger.debug("", e)
        throw new Exception("Failed to truncate Audit Store", e)
      }
    }
  }

  private def deleteFile(file: File): Unit = {
    if (file.exists()) {
      var ret = true
      if (file.isDirectory) {
        for (f <- file.listFiles) {
          deleteFile(f)
        }
      }
      logger.info("cleanup: Deleting file '" + file + "'")
      file.delete()
    }
  }

  override def dropStore: Unit = {
    logger.info("Dropping audit store..")
    Shutdown()
    var f = new File(path + "/" + keyspace + ".hdb")
    deleteFile(f)
    f = new File(path + "/" + keyspace + ".hdb.p")
    deleteFile(f)
  }
  
  private def initPropertiesFromFile(paramFile: String): Unit = {
    logger.info("paramFile = " + paramFile)
    try {
       scala.io.Source.fromFile(paramFile).getLines.foreach(line => {
         var parsedLine = line.split('=')
	 logger.info(parsedLine(0).trim + "=" + parsedLine(1).trim)
         adapterProperties(parsedLine(0).trim) = parsedLine(1).trim      
       })
    } catch {
      case e:Exception => {
        throw new Exception("Failed to read Audit Configuration", e)
      }     
    }
  }
}
