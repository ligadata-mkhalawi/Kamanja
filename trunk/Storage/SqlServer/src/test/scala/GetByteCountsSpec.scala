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

package com.ligadata.automation.unittests.sqlserveradapter

import org.scalatest._
import Matchers._

import com.ligadata.Utils._
import util.control.Breaks._
import scala.io._
import java.util.{Date,Calendar,TimeZone}
import java.text.{SimpleDateFormat}
import java.io._

import sys.process._
import org.apache.logging.log4j._

import com.ligadata.keyvaluestore._
import com.ligadata.KvBase._
import com.ligadata.StorageBase._
import com.ligadata.Serialize._
import com.ligadata.Utils.Utils._
import com.ligadata.Utils.{ KamanjaClassLoader, KamanjaLoaderInfo }
import com.ligadata.StorageBase.StorageAdapterFactory
import com.ligadata.keyvaluestore.SqlServerAdapter

import com.ligadata.Exceptions._

class GetByteCountsSpec extends FunSpec with BeforeAndAfter with BeforeAndAfterAll with GivenWhenThen {
  var res : String = null;
  var statusCode: Int = -1;
  var adapter:DataStore = null
  var serializer:Serializer = null

  private var readCount = 0
  private var byteCount = 0
  private var getOpCount:scala.collection.mutable.Map[String,Int] = new scala.collection.mutable.HashMap()
  private var putOpCount:scala.collection.mutable.Map[String,Int] = new scala.collection.mutable.HashMap()
  private var getObjCount:scala.collection.mutable.Map[String,Int] = new scala.collection.mutable.HashMap()
  private var putObjCount:scala.collection.mutable.Map[String,Int] = new scala.collection.mutable.HashMap()
  private var getBytCount:scala.collection.mutable.Map[String,Int] = new scala.collection.mutable.HashMap()
  private var putBytCount:scala.collection.mutable.Map[String,Int] = new scala.collection.mutable.HashMap()

  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)

  val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  val dateFormat1 = new SimpleDateFormat("yyyy/MM/dd")
  // set the timezone to UTC for all time values
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  var containerName = ""
  private val kvManagerLoader = new KamanjaLoaderInfo
  private val maxConnectionAttempts = 10

  serializer = SerializerManager.GetSerializer("kryo")
  logger.info("Initialize SqlServerAdapter")
  val dataStoreInfo = """{"StoreType": "sqlserver","hostname": "192.168.56.1","instancename":"KAMANJA","portnumber":"1433","database": "bofa","user":"bofauser","SchemaName":"bofauser","password":"bofauser","jarpaths":"/media/home2/jdbc","jdbcJar":"sqljdbc4-2.0.jar","clusteredIndex":"YES","autoCreateTables":"YES"}"""
  private var sqlServerAdapter: SqlServerAdapter = null

  private def CreateAdapter: DataStore = {
    var connectionAttempts = 0
    while (connectionAttempts < maxConnectionAttempts) {
      try {
        adapter = SqlServerAdapter.CreateStorageAdapter(kvManagerLoader, dataStoreInfo, null, null)
        return adapter
      } catch {
        case e: Exception => {
          logger.error("will retry after one minute ...", e)
          Thread.sleep(60 * 1000L)
          connectionAttempts = connectionAttempts + 1
        }
      }
    }
    return null;
  }

  override def beforeAll = {
    logger.info("starting...");
    CreateAdapter
  }

  private def RoundDateToSecs(d:Date): Date = {
    var c = Calendar.getInstance()
    if( d == null ){
      c.setTime(new Date(0))
      c.getTime
    }
    else{
      c.setTime(d)
      c.set(Calendar.MILLISECOND,0)
      c.getTime
    }
  }

  private def getKeySize(k: Key): Int = {
    var bucketKeySize = 0
    k.bucketKey.foreach(bk => { bucketKeySize = bucketKeySize + bk.length })
    8 + bucketKeySize + 8 + 4
  }

  private def getValueSize(v: Value): Int = {
    //v.serializerType.length + v.serializedInfo.length + 4 + columnFamilyNamesLen
    v.serializedInfo.length
  }

  def readCallBack(key:Key, value: Value) {
    logger.info("timePartition => " + key.timePartition)
    logger.info("bucketKey => " + key.bucketKey.mkString(","))
    logger.info("transactionId => " + key.transactionId)
    logger.info("rowId => " + key.rowId)
    logger.info("serializerType => " + value.serializerType)
    logger.info("serializedInfo length => " + value.serializedInfo.length)
    logger.info("----------------------------------------------------")
    readCount = readCount + 1
    byteCount = byteCount + getKeySize(key) + getValueSize(value)
  }

  def readKeyCallBack(key:Key) {
    logger.info("timePartition => " + key.timePartition)
    logger.info("bucketKey => " + key.bucketKey.mkString(","))
    logger.info("transactionId => " + key.transactionId)
    logger.info("rowId => " + key.rowId)
    logger.info("----------------------------------------------------")
    readCount = readCount + 1
    byteCount = byteCount + getKeySize(key)
  }

  def deleteFile(path:File):Unit = {
    if(path.exists()){
      if (path.isDirectory){
	for(f <- path.listFiles) {
          deleteFile(f)
	}
      }
      path.delete()
    }
  }


  describe("Unit Tests for all sqlserveradapter operations") {

    // validate property setup
    it("Validate api operations") {
      val containerName = "sys.customer1"
      sqlServerAdapter = adapter.asInstanceOf[SqlServerAdapter]
      val tableName = sqlServerAdapter.toFullTableName(containerName)

      And("Drop container")
      noException should be thrownBy {
        var containers = new Array[String](0)
        containers = containers :+ containerName
        adapter.DropContainer(containers)
      }

      noException should be thrownBy {
        var containers = new Array[String](0)
        containers = containers :+ containerName
        adapter.CreateContainer(containers)
      }

      And("Test Put api")
      var keys = new Array[Key](0) // to be used by a delete operation later on
      for (i <- 1 to 10) {
        var currentTime = new Date()
        //var currentTime = null
        var keyArray = new Array[String](0)
        var custName = "customer-" + i
        keyArray = keyArray :+ custName
        var key = new Key(currentTime.getTime(), keyArray, i, i)
        keys = keys :+ key
        var custAddress = "1000" + i + ",Main St, Redmond WA 98052"
        var custNumber = "425666777" + i
        var obj = new Customer(custName, custAddress, custNumber)
        var v = serializer.SerializeObjectToByteArray(obj)
        var value = new Value(1,"kryo",v)
        noException should be thrownBy {
          adapter.put(containerName, key, value)
	  putBytCount(tableName) = putBytCount.getOrElse(tableName,0) + getKeySize(key) + getValueSize(value)
	  putOpCount(tableName) = putOpCount.getOrElse(tableName,0) + 1
	  putObjCount(tableName) = putObjCount.getOrElse(tableName,0) + 1
        }
      }
      assert(adapter._putOps(tableName) == putOpCount(tableName))
      assert(adapter._putBytes(tableName) == putBytCount(tableName))

      And("Get all the rows that were just added")
      noException should be thrownBy {
	readCount = 0
	byteCount = 0
        adapter.get(containerName, readCallBack _)
	getOpCount(tableName) = getOpCount.getOrElse(tableName,0) + 1
	getObjCount(tableName) = getObjCount.getOrElse(tableName,0) + readCount
	getBytCount(tableName) = getBytCount.getOrElse(tableName,0) + byteCount
      }
      assert(adapter._getOps(tableName) == getOpCount(tableName))
      assert(adapter._getObjs(tableName) == getObjCount(tableName))
      assert(adapter._getBytes(tableName) == getBytCount(tableName))

      And("Test Bulk Put api")
      var keyValueList = new Array[(Key, Value)](0)
      var keyStringList = new Array[Array[String]](0)
      for (i <- 1 to 10) {
        var cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -i);
        var currentTime = cal.getTime()
        var keyArray = new Array[String](0)
        var custName = "customer-" + i
        keyArray = keyArray :+ custName
        // keyStringList is only used to test a del operation later
        keyStringList = keyStringList :+ keyArray
        var key = new Key(currentTime.getTime(), keyArray, i, i)
        var custAddress = "1000" + i + ",Main St, Redmond WA 98052"
        var custNumber = "4256667777" + i
        var obj = new Customer(custName, custAddress, custNumber)
        var v = serializer.SerializeObjectToByteArray(obj)
        var value = new Value(1,"kryo",v)
        keyValueList = keyValueList :+(key, value)
	putBytCount(tableName) = putBytCount.getOrElse(tableName,0) + getKeySize(key) + getValueSize(value)
      }
      var dataList = new Array[(String, Array[(Key, Value)])](0)
      dataList = dataList :+ (containerName, keyValueList)
      putObjCount(tableName) = putObjCount.getOrElse(tableName,0) + keyValueList.length

      noException should be thrownBy {
        adapter.put(dataList)
	putOpCount(tableName) = putOpCount.getOrElse(tableName,0) + 1
      }
      assert(adapter._putOps(tableName) == putOpCount(tableName))
      assert(adapter._putObjs(tableName) == putObjCount(tableName))
      assert(adapter._putBytes(tableName) == putBytCount(tableName))

      noException should be thrownBy {
	readCount = 0
	byteCount = 0
        adapter.get(containerName, readCallBack _)
	getOpCount(tableName) = getOpCount.getOrElse(tableName,0) + 1
	getObjCount(tableName) = getObjCount.getOrElse(tableName,0) + readCount
	getBytCount(tableName) = getBytCount.getOrElse(tableName,0) + byteCount
      }
      assert(adapter._getOps(tableName) == getOpCount(tableName))
      assert(adapter._getObjs(tableName) == getObjCount(tableName))
      assert(adapter._getBytes(tableName) == getBytCount(tableName))

      And("Print component stats")
      logger.info("Count of getOps   => " + adapter._getOps(tableName));
      logger.info("Count of putOps   => " + adapter._putOps(tableName));
      logger.info("Count of getObjs  => " + adapter._getObjs(tableName));
      logger.info("Count of putObjs  => " + adapter._putObjs(tableName));
      logger.info("Count of getBytes => " + adapter._getBytes(tableName));
      logger.info("Count of putBytes => " + adapter._putBytes(tableName));
      logger.info("Simple Stats      => " + adapter.getComponentSimpleStats);

      And("Shutdown sqlServer session")
      noException should be thrownBy {
        adapter.Shutdown
      }
    }
  }
  override def afterAll = {
    var logFile = new java.io.File("logs")
    if( logFile != null ){
      deleteFile(logFile)
    }
  }
}
