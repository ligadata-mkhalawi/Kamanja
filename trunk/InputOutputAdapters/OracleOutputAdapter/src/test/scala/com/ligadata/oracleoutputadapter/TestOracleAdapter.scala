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

package com.ligadata.outputadapters;

import org.scalatest._
import Matchers._

import java.util.{Date, Calendar, TimeZone}
import java.text.{SimpleDateFormat}
import java.io._

import org.apache.logging.log4j._
import com.ligadata.Utils.KamanjaLoaderInfo
import com.ligadata.Exceptions._

case class ReadResult(tableName: String,columnName: String,columnValue: String);

class TestOracleAdapter extends FunSpec with BeforeAndAfter with BeforeAndAfterAll with GivenWhenThen {
  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)

  val dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
  val dateFormat1 = new SimpleDateFormat("yyyy/MM/dd")
  // set the timezone to UTC for all time values
  TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

  private val kvManagerLoader = new KamanjaLoaderInfo
  private var oracleAdapter: OracleAdapter = null

  val dataStoreInfo = """{"hostname": "192.168.1.23","instancename":"KAMANJA","portnumber":"1521","user":"digicell","SchemaName":"digicell","password":"Carribean2","jarpaths":"/media/home2/jdbc","jdbcJar":"ojdbc6.jar","autoCreateTables":"YES","appendOnly":"YES"}"""


  private val maxConnectionAttempts = 10;
  var cnt: Long = 0
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

  def readCallBack(tableName: String, columnName: String, columnValue: String) {
    logger.info("tableName => %s,columnName => %s, columnValue => %s".format(tableName,columnName,columnValue));
    readCount = readCount + 1
    val rr = new ReadResult(tableName,columnName,columnValue);
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

  private def CreateAdapter: OracleAdapter = {
    var connectionAttempts = 0
    while (connectionAttempts < maxConnectionAttempts) {
      try {
        oracleAdapter = new OracleAdapter(kvManagerLoader, dataStoreInfo, null, null)
        return oracleAdapter
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
      logger.info("Initialize OracleAdapter")
      oracleAdapter = CreateAdapter
    }
    catch {
      case e: Exception => throw new Exception("Failed to execute set up properly", e)
    }
  }

  describe("Unit Tests To Create a Random Table and do fetch operations") {

    it("Validate api operations in simple case of selectList contains only one column") {
      And("create a sample table");
      val containerName = "customer1"

      val tableName = oracleAdapter.toTableName(containerName)
      val columnNames = Array("name","address","cellNumber")
      val columnTypes = Array("varchar2(30)","varchar2(100)","number")
      val keyColumns = Array("name");

      And("Create Random Table " + containerName)
      noException should be thrownBy {
        oracleAdapter.createAnyTable(containerName,columnNames,columnTypes,keyColumns,"ddl")
      }

      And("truncate the table");
      noException should be thrownBy {
	var containerList = new Array[String](0)
	containerList = containerList :+ containerName
	oracleAdapter.TruncateContainer(containerList)
      }

      And("insert few sample rows");
      var rowColumnValues = new Array[Array[(String,String)]](0)
      for (i <- 1 to 10) {
        var custName = "customer-" + i
        var custAddress = "1000" + i + ",Main St, Redmond WA 98052"
        var custNumber = "425666777" + i
	var columnValues = new Array[(String,String)](0)

	columnValues = columnValues :+ ("name",custName)
	columnValues = columnValues :+ ("address",custAddress)
	columnValues = columnValues :+ ("cellNumber",custNumber)
	rowColumnValues = rowColumnValues :+ columnValues
      }
      noException should be thrownBy {
	oracleAdapter.put(containerName,columnNames,rowColumnValues);
      }

      And("Get the rows that were just added with a fliter applied ")
      readResults = new Array[ReadResult](0);
      var selectList = Array("address");
      var filterColumns = new Array[(String,String)](0)
      filterColumns = filterColumns :+ ("name","customer-1");
      noException should be thrownBy {
        oracleAdapter.get(containerName, selectList, filterColumns, readCallBack _)
      }

      assert(readResults.length == 1)
      assert(readResults(0).tableName.equalsIgnoreCase("customer1"))
      assert(readResults(0).columnName.equalsIgnoreCase("address"))
      assert(readResults(0).columnValue.equals("10001,Main St, Redmond WA 98052"))
    }

    it("Test dropContainer ..."){
      val containerName = "customer1";
      noException should be thrownBy {
	var containerList = new Array[String](0)
	containerList = containerList :+ containerName
	oracleAdapter.DropContainer(containerList)
      }
    }      
  }

  override def afterAll = {
    logger.info("Shutdown oracle session")
    noException should be thrownBy {
      oracleAdapter.Shutdown
    }
    var logFile = new java.io.File("logs")
    if (logFile != null) {
      deleteFile(logFile)
    }
  }
}
