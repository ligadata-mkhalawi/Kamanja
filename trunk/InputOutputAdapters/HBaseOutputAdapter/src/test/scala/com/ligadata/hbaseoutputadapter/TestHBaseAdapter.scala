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

package com.ligadata.OutputAdapters;

import org.scalatest._
import Matchers._

import java.util.{Date, Calendar, TimeZone}
import java.text.{SimpleDateFormat}
import java.io._

import org.apache.logging.log4j._
import com.ligadata.Utils.KamanjaLoaderInfo
import com.ligadata.Exceptions._

case class ReadResult(key: String,columnFamily: String,columnName: String,columnValue: String);

class TestHBaseAdapter extends FunSpec with BeforeAndAfter with BeforeAndAfterAll with GivenWhenThen {
  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)

  val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
  val dateFormat1 = new SimpleDateFormat("yyyy/MM/dd")
  // set the timezone to UTC for all time values
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  private val kvManagerLoader = new KamanjaLoaderInfo
  private var hbaseAdapter: HBaseAdapter = null
  val dataStoreInfo = s"""{"StoreType": "hbase","SchemaName": "unit_tests","Location":"localhost","autoCreateTables":"YES"}"""

  private val maxConnectionAttempts = 10;
  var cnt: Long = 0
  private val containerName = "sys.customer1"
  private var readCount = 0
  private var exists = false
  private var getCount:scala.collection.mutable.Map[String,Int] = scala.collection.mutable.HashMap()
  private var putCount:scala.collection.mutable.Map[String,Int] = scala.collection.mutable.HashMap()

  private var readResults = new Array[ReadResult](0)

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

  def readCallBack(key: String, columnFamily: String, columnName: String, columnValue: String) {
    logger.info("key => " + key)
    logger.info("columnFamily => " + columnFamily)
    logger.info("columnName   => " + columnName)
    logger.info("columnValue  => " + columnValue)
    logger.info("----------------------------------------------------")
    readCount = readCount + 1
    val rr = new ReadResult(key,columnFamily,columnName,columnValue);
    readResults = readResults :+ rr
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

  private def CreateAdapter: HBaseAdapter = {
    var connectionAttempts = 0
    while (connectionAttempts < maxConnectionAttempts) {
      try {
        hbaseAdapter = new HBaseAdapter(kvManagerLoader, dataStoreInfo, null, null)
        return hbaseAdapter
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
      hbaseAdapter = CreateAdapter
    }
    catch {
      case e: Exception => throw new Exception("Failed to execute set up properly", e)
    }
  }

  describe("Unit Tests To Create a Random Table and do fetch operations") {

    it("Validate api operations in simple case of columnFamily contains only one column") {
      val containerName = "customer1"

      val tableName = hbaseAdapter.toTableName(containerName)
      val columnList = Array("name","address","cellNumber")

      And("Create Random Table " + containerName)
      noException should be thrownBy {
        hbaseAdapter.createAnyTable(containerName,columnList,"ddl")
      }


      var keyColumns = Array("name");
      for (i <- 1 to 10) {
        var custName = "customer-" + i
        var custAddress = "1000" + i + ",Main St, Redmond WA 98052"
        var custNumber = "425666777" + i
	var columnValues = new Array[(String,String,String)](0)
	columnValues = columnValues :+ ("name","name",custName)
	columnValues = columnValues :+ ("address","address",custAddress)
	columnValues = columnValues :+ ("cellNumber","cellNumber",custNumber)
        noException should be thrownBy {
	  hbaseAdapter.put(containerName,keyColumns,columnValues);
        }
      }

      And("Get the rows that were just added with a fliter applied ")
      readResults = new Array[ReadResult](0)
      var selectList = Array(("address","address"))
      var filterColumns = new Array[(String,String,String)](0)
      filterColumns = filterColumns :+ ("name","name","customer-1");
      noException should be thrownBy {
        hbaseAdapter.get(containerName, selectList, filterColumns, keyColumns,readCallBack _)
      }

      assert(readResults.length == 1)
      assert(readResults(0).key.equals("customer-1"))
      assert(readResults(0).columnFamily.equals("address"))
      assert(readResults(0).columnName.equals("address"))
      assert(readResults(0).columnValue.equals("10001,Main St, Redmond WA 98052"))
    }

    it("Validate api operations in case of columnFamily contains more than one column") {
      val containerName = "customer2"

      val tableName = hbaseAdapter.toTableName(containerName)
      val columnList = Array("ssid","name","address")

      And("Create Random Table " + containerName)
      noException should be thrownBy {
        hbaseAdapter.createAnyTable(containerName,columnList,"ddl")
      }

      var keyColumns = Array("ssid");
      for (i <- 1 to 10) {
	var ssid = "11122333" + i;
        var firstName = "bill" + i;
        var lastName = "king" + i;
        var custAddress = "1000" + i + ",Main St, Redmond WA 98052"
        var custNumber = "425666777" + i
	var columnValues = new Array[(String,String,String)](0)
	columnValues = columnValues :+ ("ssid","ssid",ssid)
	columnValues = columnValues :+ ("address","streetAddress",custAddress)
	columnValues = columnValues :+ ("address","cellNumber",custNumber)
        noException should be thrownBy {
	  hbaseAdapter.put(containerName,keyColumns,columnValues);
        }
	
	columnValues = new Array[(String,String,String)](0)
	columnValues = columnValues :+ ("ssid","ssid",ssid)
	columnValues = columnValues :+ ("name","firstName",firstName)
	columnValues = columnValues :+ ("name","lastName",lastName)
        noException should be thrownBy {
	  hbaseAdapter.put(containerName,keyColumns,columnValues);
        }
      }

      And("Get the rows that were just added with a fliter applied ")
      readResults = new Array[ReadResult](0)
      var selectList = Array(("address","streetAddress"),("name","lastName"))
      var filterColumns = new Array[(String,String,String)](0)
      filterColumns = filterColumns :+ ("ssid","ssid","111223331");
      noException should be thrownBy {
        hbaseAdapter.get(containerName, selectList, filterColumns, keyColumns,readCallBack _)
      }

      assert(readResults.length == 2)
      assert(readResults(0).key.equals("111223331"))

      assert(readResults(0).columnFamily.equals("address"))
      assert(readResults(0).columnName.equals("streetAddress"))
      assert(readResults(0).columnValue.equals("10001,Main St, Redmond WA 98052"))

      assert(readResults(1).columnFamily.equals("name"))
      assert(readResults(1).columnName.equals("lastName"))
      assert(readResults(1).columnValue.equals("king1"))
    }

    it("Validate api operations in case of keyColumnFamily contains more than one column") {
      val containerName = "customer3"

      val tableName = hbaseAdapter.toTableName(containerName)
      val columnList = Array("serialNum","ssid","name","address")

      And("Create Random Table " + containerName)
      noException should be thrownBy {
        hbaseAdapter.createAnyTable(containerName,columnList,"ddl")
      }

      var keyColumns = Array("ssid","serialNum");
      for (i <- 1 to 10) {
	var ssid = "11122333" + i;
	var serialNum = i.toString();
        var firstName = "bill" + i;
        var lastName = "king" + i;
        var custAddress = "1000" + i + ",Main St, Redmond WA 98052"
        var custNumber = "425666777" + i
	var columnValues = new Array[(String,String,String)](0)
	columnValues = columnValues :+ ("ssid","ssid",ssid)
	columnValues = columnValues :+ ("serialNum","serialNum",serialNum)
	columnValues = columnValues :+ ("address","streetAddress",custAddress)
	columnValues = columnValues :+ ("address","cellNumber",custNumber)
        noException should be thrownBy {
	  hbaseAdapter.put(containerName,keyColumns,columnValues);
        }
	
	columnValues = new Array[(String,String,String)](0)
	columnValues = columnValues :+ ("ssid","ssid",ssid)
	columnValues = columnValues :+ ("serialNum","serialNum",serialNum)
	columnValues = columnValues :+ ("name","firstName",firstName)
	columnValues = columnValues :+ ("name","lastName",lastName)
        noException should be thrownBy {
	  hbaseAdapter.put(containerName,keyColumns,columnValues);
        }
      }

      And("Get the rows that were just added with a fliter applied ")
      readResults = new Array[ReadResult](0)
      var selectList = Array(("address","streetAddress"),("name","lastName"))
      var filterColumns = new Array[(String,String,String)](0)
      filterColumns = filterColumns :+ ("ssid","ssid","111223331");
      filterColumns = filterColumns :+ ("serialNum","serialNum","1");
      noException should be thrownBy {
        hbaseAdapter.get(containerName, selectList, filterColumns, keyColumns,readCallBack _)
      }

      assert(readResults.length == 2)
      assert(readResults(0).key.equals("111223331.1"))

      assert(readResults(0).columnFamily.equals("address"))
      assert(readResults(0).columnName.equals("streetAddress"))
      assert(readResults(0).columnValue.equals("10001,Main St, Redmond WA 98052"))

      assert(readResults(1).columnFamily.equals("name"))
      assert(readResults(1).columnName.equals("lastName"))
      assert(readResults(1).columnValue.equals("king1"))
    }

  }

  override def afterAll = {
    logger.info("Shutdown hbase session")
    noException should be thrownBy {
      hbaseAdapter.Shutdown
    }
    var logFile = new java.io.File("logs")
    if (logFile != null) {
      deleteFile(logFile)
    }
  }
}
