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

package com.ligadata.automation.unittests.hbaseadapter

import org.scalatest._
import Matchers._

import java.util.{Date, Calendar, TimeZone}
import java.text.{SimpleDateFormat}
import java.io._

import org.apache.logging.log4j._

import com.ligadata.KvBase._
import com.ligadata.StorageBase._
import com.ligadata.Serialize._
import com.ligadata.Utils.KamanjaLoaderInfo
import com.ligadata.keyvaluestore.HBaseAdapter
import com.ligadata.kamanja.metadata.AdapterInfo

import com.ligadata.Exceptions._

@Ignore
class GetByteCountsSpec extends FunSpec with BeforeAndAfter with BeforeAndAfterAll with GivenWhenThen {
  var adapter: DataStore = null
  var serializer: Serializer = null

  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)

  val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
  val dateFormat1 = new SimpleDateFormat("yyyy/MM/dd")
  // set the timezone to UTC for all time values
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  private val kvManagerLoader = new KamanjaLoaderInfo
  private var hbaseAdapter: HBaseAdapter = null
  serializer = SerializerManager.GetSerializer("kryo")
  val dataStoreInfo = s"""{"StoreType": "hbase","SchemaName": "unit_tests","Location":"localhost","autoCreateTables":"YES"}"""

  private val maxConnectionAttempts = 10;
  var cnt: Long = 0
  private var readCount = 0
  private var byteCount = 0
  private var exists = false
  private var getOpCount:scala.collection.mutable.Map[String,Int] = new scala.collection.mutable.HashMap()
  private var putOpCount:scala.collection.mutable.Map[String,Int] = new scala.collection.mutable.HashMap()
  private var getObjCount:scala.collection.mutable.Map[String,Int] = new scala.collection.mutable.HashMap()
  private var putObjCount:scala.collection.mutable.Map[String,Int] = new scala.collection.mutable.HashMap()
  private var getBytCount:scala.collection.mutable.Map[String,Int] = new scala.collection.mutable.HashMap()
  private var putBytCount:scala.collection.mutable.Map[String,Int] = new scala.collection.mutable.HashMap()

  private def RoundDateToSecs(d: Date): Date = {
    var c = Calendar.getInstance()
    if (d == null) {
      c.setTime(new Date(0))
      c.getTime
    }
    else {
      c.setTime(d)
      c.set(Calendar.MILLISECOND, 0)
      c.getTime
    }
  }

  def readCallBack(key: Key, value: Value) {
    logger.info("timePartition => " + key.timePartition)
    logger.info("bucketKey => " + key.bucketKey.mkString(","))
    logger.info("transactionId => " + key.transactionId)
    logger.info("rowId => " + key.rowId)
    logger.info("serializerType => " + value.serializerType)
    logger.info("serializedInfo length => " + value.serializedInfo.length)
    val cust = serializer.DeserializeObjectFromByteArray(value.serializedInfo).asInstanceOf[Customer]
    logger.info("serializedObject => " + cust)
    logger.info("----------------------------------------------------")
    readCount = readCount + 1
    byteCount = byteCount + getKeySize(key) + getValueSize(value) + 3
  }

  def readKeyCallBack(key: Key) {
    logger.info("timePartition => " + key.timePartition)
    logger.info("bucketKey => " + key.bucketKey.mkString(","))
    logger.info("transactionId => " + key.transactionId)
    logger.info("rowId => " + key.rowId)
    logger.info("----------------------------------------------------")
    readCount = readCount + 1
    byteCount = byteCount + getKeySize(key)
  }

  def deleteFile(path: File): Unit = {
    if (path.exists()) {
      if (path.isDirectory) {
        for (f <- path.listFiles) {
          deleteFile(f)
        }
      }
      path.delete()
    }
  }

  private def CreateAdapter: DataStore = {
    var connectionAttempts = 0
    val adapterInfo = new AdapterInfo
    adapterInfo.name = "testHBaseAdapter"
    while (connectionAttempts < maxConnectionAttempts) {
      try {
        adapter = HBaseAdapter.CreateStorageAdapter(kvManagerLoader, dataStoreInfo, null, adapterInfo)
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


  private def getKeySize(k: Key): Int = {
    var bucketKeySize = 0
    k.bucketKey.foreach(bk => { bucketKeySize = bucketKeySize + bk.length })
    8 + bucketKeySize + 8 + 4
  }

  private def getValueSize(v: Value): Int = {
    //v.serializerType.length + v.serializedInfo.length + 4 + columnFamilyNamesLen
    v.serializedInfo.length
  }

  override def beforeAll = {
    try {
      logger.info("starting...");
      logger.info("Initialize HBaseAdapter")
      adapter = CreateAdapter
    }
    catch {
      case e: Exception => throw new Exception("Failed to execute set up properly", e)
    }
  }

  describe("Unit Tests for all hbaseadapter operations") {

    // validate property setup
    it("Validate api operations") {
      val containerName1 = "sys.customer1"
      hbaseAdapter = adapter.asInstanceOf[HBaseAdapter]
      val tableName1 = hbaseAdapter.toTableName(containerName1)

      And("Drop container")
      noException should be thrownBy {
        var containers = new Array[String](0)
        containers = containers :+ containerName1
        adapter.DropContainer(containers)
      }

      noException should be thrownBy {
        var containers = new Array[String](0)
        containers = containers :+ containerName1
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
          adapter.put(containerName1, key, value)
	  putBytCount(tableName1) = putBytCount.getOrElse(tableName1,0) + getKeySize(key) + getValueSize(value)
	  putOpCount(tableName1) = putOpCount.getOrElse(tableName1,0) + 1
	  putObjCount(tableName1) = putObjCount.getOrElse(tableName1,0) + 1
        }
      }
      assert(adapter._putOps(tableName1) == putOpCount(tableName1))
      assert(adapter._putBytes(tableName1) == putBytCount(tableName1))

      And("Get all the rows that were just added")
      noException should be thrownBy {
	readCount = 0
	byteCount = 0
        adapter.get(containerName1, readCallBack _)
	getOpCount(tableName1) = getOpCount.getOrElse(tableName1,0) + 1
	getObjCount(tableName1) = getObjCount.getOrElse(tableName1,0) + readCount
	getBytCount(tableName1) = getBytCount.getOrElse(tableName1,0) + byteCount
      }
      assert(adapter._getOps(tableName1) == getOpCount(tableName1))
      assert(adapter._getObjs(tableName1) == getObjCount(tableName1))
      assert(adapter._getBytes(tableName1) == getBytCount(tableName1))

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
	putBytCount(tableName1) = putBytCount.getOrElse(tableName1,0) + getKeySize(key) + getValueSize(value)
      }
      var dataList = new Array[(String, Array[(Key, Value)])](0)
      dataList = dataList :+ (containerName1, keyValueList)
      putObjCount(tableName1) = putObjCount.getOrElse(tableName1,0) + keyValueList.length

      noException should be thrownBy {
        adapter.put(dataList)
	putOpCount(tableName1) = putOpCount.getOrElse(tableName1,0) + 1
      }
      assert(adapter._putOps(tableName1) == putOpCount(tableName1))
      assert(adapter._putObjs(tableName1) == putObjCount(tableName1))
      assert(adapter._putBytes(tableName1) == putBytCount(tableName1))

      noException should be thrownBy {
	readCount = 0
	byteCount = 0
        adapter.get(containerName1, readCallBack _)
	getOpCount(tableName1) = getOpCount.getOrElse(tableName1,0) + 1
	getObjCount(tableName1) = getObjCount.getOrElse(tableName1,0) + readCount
	getBytCount(tableName1) = getBytCount.getOrElse(tableName1,0) + byteCount
      }
      assert(adapter._getOps(tableName1) == getOpCount(tableName1))
      assert(adapter._getObjs(tableName1) == getObjCount(tableName1))
      assert(adapter._getBytes(tableName1) == getBytCount(tableName1))

      And("Print component stats")
      logger.info("Count of getOps   => " + adapter._getOps(tableName1));
      logger.info("Count of putOps   => " + adapter._putOps(tableName1));
      logger.info("Count of getObjs  => " + adapter._getObjs(tableName1));
      logger.info("Count of putObjs  => " + adapter._putObjs(tableName1));
      logger.info("Count of getBytes => " + adapter._getBytes(tableName1));
      logger.info("Count of putBytes => " + adapter._putBytes(tableName1));

      val containerName2 = "sys.customer2"
      val tableName2 = hbaseAdapter.toTableName(containerName2)

      And("Drop container")
      noException should be thrownBy {
        var containers = new Array[String](0)
        containers = containers :+ containerName2
        adapter.DropContainer(containers)
      }

      noException should be thrownBy {
        var containers = new Array[String](0)
        containers = containers :+ containerName2
        adapter.CreateContainer(containers)
      }

      And("Test Put api")
      keys = new Array[Key](0) // to be used by a delete operation later on
      for (i <- 1 to 20) {
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
          adapter.put(containerName2, key, value)
	  putBytCount(tableName2) = putBytCount.getOrElse(tableName2,0) + getKeySize(key) + getValueSize(value)
	  putOpCount(tableName2) = putOpCount.getOrElse(tableName2,0) + 1
	  putObjCount(tableName2) = putObjCount.getOrElse(tableName2,0) + 1
        }
      }
      assert(adapter._putOps(tableName2) == putOpCount(tableName2))
      assert(adapter._putBytes(tableName2) == putBytCount(tableName2))

      And("Get all the rows that were just added")
      noException should be thrownBy {
	readCount = 0
	byteCount = 0
        adapter.get(containerName2, readCallBack _)
	getOpCount(tableName2) = getOpCount.getOrElse(tableName2,0) + 1
	getObjCount(tableName2) = getObjCount.getOrElse(tableName2,0) + readCount
	getBytCount(tableName2) = getBytCount.getOrElse(tableName2,0) + byteCount
      }
      assert(adapter._getOps(tableName2) == getOpCount(tableName2))
      assert(adapter._getObjs(tableName2) == getObjCount(tableName2))
      assert(adapter._getBytes(tableName2) == getBytCount(tableName2))

      And("Test Bulk Put api")
      keyValueList = new Array[(Key, Value)](0)
      keyStringList = new Array[Array[String]](0)
      for (i <- 1 to 20) {
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
	putBytCount(tableName2) = putBytCount.getOrElse(tableName2,0) + getKeySize(key) + getValueSize(value)
      }
      dataList = new Array[(String, Array[(Key, Value)])](0)
      dataList = dataList :+ (containerName2, keyValueList)
      putObjCount(tableName2) = putObjCount.getOrElse(tableName2,0) + keyValueList.length

      noException should be thrownBy {
        adapter.put(dataList)
	putOpCount(tableName2) = putOpCount.getOrElse(tableName2,0) + 1
      }
      assert(adapter._putOps(tableName2) == putOpCount(tableName2))
      assert(adapter._putObjs(tableName2) == putObjCount(tableName2))
      assert(adapter._putBytes(tableName2) == putBytCount(tableName2))

      noException should be thrownBy {
	readCount = 0
	byteCount = 0
        adapter.get(containerName2, readCallBack _)
	getOpCount(tableName2) = getOpCount.getOrElse(tableName2,0) + 1
	getObjCount(tableName2) = getObjCount.getOrElse(tableName2,0) + readCount
	getBytCount(tableName2) = getBytCount.getOrElse(tableName2,0) + byteCount
      }
      assert(adapter._getOps(tableName2) == getOpCount(tableName2))
      assert(adapter._getObjs(tableName2) == getObjCount(tableName2))
      assert(adapter._getBytes(tableName2) == getBytCount(tableName2))

      And("Print component stats")
      logger.info("Count of getOps   => " + adapter._getOps(tableName2));
      logger.info("Count of putOps   => " + adapter._putOps(tableName2));
      logger.info("Count of getObjs  => " + adapter._getObjs(tableName2));
      logger.info("Count of putObjs  => " + adapter._putObjs(tableName2));
      logger.info("Count of getBytes => " + adapter._getBytes(tableName2));
      logger.info("Count of putBytes => " + adapter._putBytes(tableName2));

      logger.info("getComponentSimpleStats  => " + adapter.getComponentSimpleStats);
      logger.info("getComponentStatusAndMetrics => " + adapter.getComponentStatusAndMetrics);

      And("Shutdown hbase session")
      noException should be thrownBy {
        adapter.Shutdown
      }

    }
  }

  override def afterAll = {
    var logFile = new java.io.File("logs")
    if (logFile != null) {
      deleteFile(logFile)
    }
  }
}
