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

package com.ligadata.hbaseoutputadapter;

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

class TestHBaseAdapter extends FunSpec with BeforeAndAfter with BeforeAndAfterAll with GivenWhenThen {
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

  def readCallBack(key: String, columnFamily: String, columnName: String, columnValue: String) {
    logger.info("key => " + key)
    logger.info("columnFamily => " + columnFamily)
    logger.info("columnName   => " + columnName)
    logger.info("columnValue  => " + columnValue)
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
        adapter = new HBaseAdapter(kvManagerLoader, dataStoreInfo, null, null)
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

  describe("Unit Tests To Create a Random Table and do fetch operations") {

    // validate property setup
    it("Validate api operations") {
      val containerName = "customer1"

      hbaseAdapter = adapter.asInstanceOf[HBaseAdapter]
      val tableName = hbaseAdapter.toTableName(containerName)

      val columnList = Array("name","address","cellNumber")

      And("Create Random Table ")
      noException should be thrownBy {
        adapter.createAnyTable(containerName,columnList,"ddl")
      }

      And("Add sample rows to the container")
      var columnValues = new Array[(String,String,String)](0)

      var keyColumns = Array("name");
      for (i <- 1 to 10) {
        var custName = "customer-" + i
        var custAddress = "1000" + i + ",Main St, Redmond WA 98052"
        var custNumber = "425666777" + i
	columnValues = columnValues :+ ("name","name",custName)
	columnValues = columnValues :+ ("address","address",custAddress)
	columnValues = columnValues :+ ("cellNumber","cellNumber",custNumber)
	//columnValues("name") = custName
	//columnValues("address") = custAddress
	//columnValues("cellNumber") = custNumber
        noException should be thrownBy {
	  adapter.put(containerName,keyColumns,columnValues);
        }
      }

      And("Get the rows that were just added with a fliter applied ")
      var selectList = Array(("address","address"))
      var filterColumns = new Array[(String,String,String)](0)
      filterColumns = filterColumns :+ ("name","name","customer-1");
      //filterColumns("cellNumber") = "4256667775";
      noException should be thrownBy {
        adapter.get(containerName, selectList, filterColumns, keyColumns,readCallBack _)
      }

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
