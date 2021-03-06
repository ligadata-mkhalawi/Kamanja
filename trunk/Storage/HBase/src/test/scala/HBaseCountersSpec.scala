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

import com.ligadata.Exceptions._

class HBaseCountersSpec extends FunSpec with BeforeAndAfter with BeforeAndAfterAll with GivenWhenThen {
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
  private val containerName = "sys.customer1"
  private var readCount = 0
  private var exists = false
  private var getCount:scala.collection.mutable.Map[String,Int] = scala.collection.mutable.HashMap()
  private var putCount:scala.collection.mutable.Map[String,Int] = scala.collection.mutable.HashMap()

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
  }

  def readKeyCallBack(key: Key) {
    logger.info("timePartition => " + key.timePartition)
    logger.info("bucketKey => " + key.bucketKey.mkString(","))
    logger.info("transactionId => " + key.transactionId)
    logger.info("rowId => " + key.rowId)
    logger.info("----------------------------------------------------")
    readCount = readCount + 1
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
    while (connectionAttempts < maxConnectionAttempts) {
      try {
        adapter = HBaseAdapter.CreateStorageAdapter(kvManagerLoader, dataStoreInfo, null, null)
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
      val containerName = "sys.customer1"

      hbaseAdapter = adapter.asInstanceOf[HBaseAdapter]
      val tableName = hbaseAdapter.toTableName(containerName)

      And("Create container")
      noException should be thrownBy {
        var containers = new Array[String](0)
        containers = containers :+ containerName
        adapter.CreateContainer(containers)
      }

      And("Test truncate container")
      noException should be thrownBy {
        var containers = new Array[String](0)
        containers = containers :+ containerName
        adapter.TruncateContainer(containers)
	getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      }
      assert(adapter._getOps(tableName) == getCount(tableName))

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
	  putCount(tableName) = putCount.getOrElse(tableName,0) + 1
        }
      }

      assert(adapter._putOps(tableName) == putCount(tableName))

      And("Get all the rows that were just added")
      noException should be thrownBy {
        adapter.get(containerName, readCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      }

      assert(adapter._getOps(tableName) == getCount(tableName))
      hbaseAdapter = adapter.asInstanceOf[HBaseAdapter]

      And("Check the row count after adding a bunch")
      cnt = hbaseAdapter.getRowCount(containerName)
      getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      assert(cnt == 10)
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Get all the keys for the rows that were just added")
      noException should be thrownBy {
        adapter.getKeys(containerName, readKeyCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      }
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Test Del api")
      noException should be thrownBy {
        adapter.del(containerName, keys)
      }

      And("Check the row count after deleting a bunch")
      cnt = hbaseAdapter.getRowCount(containerName)
      getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      assert(cnt == 0)
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Add rows with same bucketKey and different timestamp")
      keys = new Array[Key](0) // to be used by a delete operation later on
      var custName = "customer0"
      for (i <- 1 to 10) {
        var currentTime = new Date()
        var keyArray = new Array[String](0)
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
	  putCount(tableName) = putCount.getOrElse(tableName,0) + 1
        }
      }

      assert(adapter._putOps(tableName) == putCount(tableName))
      And("Get all the rows that were just added")
      noException should be thrownBy {
        adapter.get(containerName, readCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      }
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Test Delete same bucketKey and different timestamp")
      noException should be thrownBy {
        adapter.del(containerName, keys)
      }


      And("Check the row count after deleting all the rows with same bucketKey and different timestamp")
      cnt = hbaseAdapter.getRowCount(containerName)
      assert(cnt == 0)
      getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      assert(adapter._getOps(tableName) == getCount(tableName))


      And("Adding hundred rows for testing truncate")
      for (i <- 1 to 100) {
        var currentTime = new Date()
        var keyArray = new Array[String](0)
        var custName = "customer-" + i
        keyArray = keyArray :+ custName
        var key = new Key(currentTime.getTime(), keyArray, i, i)
        var custAddress = "1000" + i + ",Main St, Redmond WA 98052"
        var custNumber = "425666777" + i
        var obj = new Customer(custName, custAddress, custNumber)
        var v = serializer.SerializeObjectToByteArray(obj)
        var value = new Value(1,"kryo",v)
        noException should be thrownBy {
          adapter.put(containerName, key, value)
	  putCount(tableName) = putCount.getOrElse(tableName,0) + 1
        }
      }
      assert(adapter._putOps(tableName) == putCount(tableName))

      And("Check the row count after adding a hundred rows")
      cnt = hbaseAdapter.getRowCount(containerName)
      getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      assert(cnt == 100)
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Test truncate container")
      noException should be thrownBy {
        var containers = new Array[String](0)
        containers = containers :+ containerName
        adapter.TruncateContainer(containers)
	getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      }
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Check the row count after truncating the container")
      cnt = hbaseAdapter.getRowCount(containerName)
      assert(cnt == 0)
      getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      assert(adapter._getOps(tableName) == getCount(tableName))

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
      }
      var dataList = new Array[(String, Array[(Key, Value)])](0)
      dataList = dataList :+ (containerName, keyValueList)
      noException should be thrownBy {
        adapter.put(dataList)
	putCount(tableName) = putCount.getOrElse(tableName,0) + 1
      }
      assert(adapter._putOps(tableName) == putCount(tableName))

      And("Get all the rows that were just added")
      noException should be thrownBy {
        adapter.get(containerName, readCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      }
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Check the row count after adding a bunch")
      cnt = hbaseAdapter.getRowCount(containerName)
      assert(cnt == 10)
      getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Get all the keys for the rows that were just added")
      noException should be thrownBy {
        adapter.getKeys(containerName, readKeyCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      }
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Set time range for 2 days ")
      var cal = Calendar.getInstance();
      cal.add(Calendar.DATE, -10);
      var beginTime = cal.getTime()
      logger.info("begin time => " + dateFormat.format(beginTime))
      cal = Calendar.getInstance();
      cal.add(Calendar.DATE, -8);
      var endTime = cal.getTime()
      logger.info("end time => " + dateFormat.format(endTime))
      var timeRange = new TimeRange(beginTime.getTime(), endTime.getTime())

      And("Test Delete for a time range")
      noException should be thrownBy {
        adapter.del(containerName, timeRange, keyStringList)
	// this delete will do a get for each key in keyStringList
	getCount(tableName) = getCount.getOrElse(tableName,0) + keyStringList.length
      }
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Check the row count after deleting a bunch based on time range")
      cnt = hbaseAdapter.getRowCount(containerName)
      assert(cnt == 8)
      getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Test the count by Setting time range for all days ")
      cal = Calendar.getInstance();
      cal.add(Calendar.DATE, -11);
      beginTime = cal.getTime()
      logger.info("begin time => " + dateFormat.format(beginTime))
      cal = Calendar.getInstance();
      cal.add(Calendar.DATE, +1);
      endTime = cal.getTime()
      logger.info("end time => " + dateFormat.format(endTime))
      timeRange = new TimeRange(beginTime.getTime(), endTime.getTime())
      var timeRanges = new Array[TimeRange](0)
      timeRanges = timeRanges :+ timeRange
      readCount = 0
      noException should be thrownBy {
        adapter.getKeys(containerName, timeRanges, readKeyCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + timeRanges.length
      }
      assert(readCount == 8)
      assert(adapter._getOps(tableName) == getCount(tableName))


      logger.info("getCount(tableName) => " + getCount(tableName) + ",_getOps(tableName) => " + adapter._getOps(tableName))

      And("Test the count by Setting time range to Long.MinValue to Long.MaxValue  ")
      timeRange = new TimeRange(Long.MinValue, Long.MaxValue)
      timeRanges = new Array[TimeRange](0)
      timeRanges = timeRanges :+ timeRange
      readCount = 0
      noException should be thrownBy {
        adapter.getKeys(containerName, timeRanges, readKeyCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + 2
      }
      assert(readCount == 8)
      logger.info("getCount(tableName) => " + getCount(tableName) + ",_getOps(tableName) => " + adapter._getOps(tableName))
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Test the count by Setting time range to 0 to Long.MaxValue  ")
      timeRange = new TimeRange(0, Long.MaxValue)
      timeRanges = new Array[TimeRange](0)
      timeRanges = timeRanges :+ timeRange
      readCount = 0
      noException should be thrownBy {
        adapter.getKeys(containerName, timeRanges, readKeyCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + timeRanges.length
      }
      assert(readCount == 8)
      assert(adapter._getOps(tableName) == getCount(tableName))


      And("Test the count by Setting time range to some negative value to  another  negative value ")
      timeRange = new TimeRange(-1000000000, -900000000)
      timeRanges = new Array[TimeRange](0)
      timeRanges = timeRanges :+ timeRange
      readCount = 0
      noException should be thrownBy {
        adapter.getKeys(containerName, timeRanges, readKeyCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + timeRanges.length
      }
      assert(readCount == 0)
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Test the count by Setting time range to some radom negative value to  some good value ")
      cal = Calendar.getInstance();
      cal.add(Calendar.DATE, -6);
      endTime = cal.getTime()
      logger.info("end time => " + dateFormat.format(endTime))
      timeRange = new TimeRange(-1000000000, endTime.getTime())
      timeRanges = new Array[TimeRange](0)
      timeRanges = timeRanges :+ timeRange
      readCount = 0
      noException should be thrownBy {
        adapter.getKeys(containerName, timeRanges, readKeyCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + 2
      }
      assert(readCount == 3)
      assert(adapter._getOps(tableName) == getCount(tableName))


      And("Test Get for a time range")
      cal = Calendar.getInstance();
      cal.add(Calendar.DATE, -7);
      beginTime = cal.getTime()
      logger.info("begin time => " + dateFormat.format(beginTime))
      cal = Calendar.getInstance();
      cal.add(Calendar.DATE, -6);
      endTime = cal.getTime()
      logger.info("end time => " + dateFormat.format(endTime))

      timeRange = new TimeRange(beginTime.getTime(), endTime.getTime())
      timeRanges = new Array[TimeRange](0)
      timeRanges = timeRanges :+ timeRange

      noException should be thrownBy {
        adapter.get(containerName, timeRanges, readCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + timeRanges.length
      }
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Test GetKeys for a time range")
      noException should be thrownBy {
        adapter.getKeys(containerName, timeRanges, readKeyCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + timeRanges.length
      }
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Test Get for a given keyString Arrays")
      keyStringList = new Array[Array[String]](0)
      for (i <- 1 to 5) {
        var keyArray = new Array[String](0)
        var custName = "customer-" + i
        keyArray = keyArray :+ custName
        keyStringList = keyStringList :+ keyArray
      }
      noException should be thrownBy {
        adapter.get(containerName, keyStringList, readCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      }
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Test GetKeys for a given keyString Arrays")
      noException should be thrownBy {
        adapter.getKeys(containerName, keyStringList, readKeyCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + 1
      }
      assert(adapter._getOps(tableName) == getCount(tableName))


      And("Test Get for a given set of keyStrings and also an array of time ranges")
      keyStringList = new Array[Array[String]](0)
      for (i <- 1 to 5) {
        var keyArray = new Array[String](0)
        var custName = "customer-" + i
        keyArray = keyArray :+ custName
        keyStringList = keyStringList :+ keyArray
      }
      cal = Calendar.getInstance();
      cal.add(Calendar.DATE, -3);
      beginTime = cal.getTime()
      logger.info("begin time => " + dateFormat.format(beginTime))
      cal = Calendar.getInstance();
      cal.add(Calendar.DATE, -2);
      endTime = cal.getTime()
      logger.info("end time => " + dateFormat.format(endTime))

      timeRange = new TimeRange(beginTime.getTime(), endTime.getTime())
      timeRanges = new Array[TimeRange](0)
      timeRanges = timeRanges :+ timeRange

      noException should be thrownBy {
        adapter.get(containerName, timeRanges, keyStringList, readCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + timeRanges.length * keyStringList.length
      }
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Test GetKeys for a given set of keyStrings and also an array of time ranges")
      noException should be thrownBy {
        adapter.getKeys(containerName, timeRanges, keyStringList, readKeyCallBack _)
	getCount(tableName) = getCount.getOrElse(tableName,0) + timeRanges.length * keyStringList.length
      }
      assert(adapter._getOps(tableName) == getCount(tableName))

      And("Print component stats")
      logger.info("Count of gets => " + adapter._getOps(tableName));
      logger.info("Count of puts => " + adapter._putOps(tableName));
      logger.info("Count of getBytes => " + adapter._getBytes);
      logger.info("Count of putBytes => " + adapter._putBytes);
      logger.info("Simple Stats => " + adapter.getComponentSimpleStats);

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
