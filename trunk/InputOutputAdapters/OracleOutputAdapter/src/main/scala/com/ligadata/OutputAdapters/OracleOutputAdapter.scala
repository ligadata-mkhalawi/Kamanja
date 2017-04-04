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

import org.apache.avro.generic.GenericRecord
import org.apache.logging.log4j.{Logger, LogManager}
import java.io._
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.zip.{ZipException, GZIPOutputStream}
import java.nio.file.{Paths, Files}
import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import com.ligadata.KamanjaBase._ // { AttributeTypeInfo, ContainerFactoryInterface, ContainerInterface, ContainerOrConcept }
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.Exceptions.{UnsupportedOperationException, FatalAdapterException}
import com.ligadata.HeartBeat.{Monitorable, MonitorComponentInfo}
import org.json4s.jackson.Serialization
import org.apache.commons.lang3.time.FastDateFormat
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.compressors.CompressorOutputStream

import scala.collection.mutable.ArrayBuffer
import scala.actors.threadpool.{ExecutorService, Executors}
import scala.util.control.Breaks._

import java.util.{Date, Calendar, TimeZone}
import java.text.{SimpleDateFormat}

import parquet.hadoop._
import parquet.hadoop.api.WriteSupport
import com.ligadata.Utils.KamanjaLoaderInfo

object OracleOutputAdapter extends OutputAdapterFactory {
  def CreateOutputAdapter(inputConfig: AdapterConfiguration, nodeContext: NodeContext): OutputAdapter = new OracleOutputAdapter(inputConfig, nodeContext)
}

class OracleOutputAdapter(val inputConfig: AdapterConfiguration, val nodeContext: NodeContext) extends OutputAdapter {
  private[this] val logger = LogManager.getLogger(getClass.getName);
  private val kvManagerLoader = new KamanjaLoaderInfo
  private var oracleAdapter: OracleAdapter = null;
  private val maxConnectionAttempts = 10;
  private var isShutdown = false

  val _TYPE_STORAGE = "Storage_Adapter"
  val _startTime: String = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  val adapterName = if (inputConfig.Name != null) inputConfig.Name else "OracleOutputAdapter";
  
  // 1. Write function(s) to create ORACLE connectivity
  private def CreateOracleAdapter(dataStoreInfo: String): OracleAdapter = {
    var connectionAttempts = 0

    while (connectionAttempts < maxConnectionAttempts) {
      try {
	logger.info("Creating Oracle connection...")
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

  private def getCurrentTimeAsString: String = {
    val df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    // Get the date today using Calendar object.
    val today = Calendar.getInstance().getTime();        
    // Using DateFormat format method we can create a string 
    // representation of a date with the defined format.
    val reportDate = df.format(today);
    return reportDate;
  }

  override def getComponentSimpleStats: String = {
    if( oracleAdapter != null ){
      oracleAdapter.getComponentSimpleStats(adapterName);
    }
    else{
      return " ";
    }
  }

  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    val lastSeen = getCurrentTimeAsString;
    logger.info("component stats => " + getComponentSimpleStats)
    MonitorComponentInfo(_TYPE_STORAGE, adapterName,adapterName, _startTime, lastSeen, "{" + getComponentSimpleStats + "}")
  }


  /*
   case 0: TypeCategory.INT;
   case 1: TypeCategory.STRING;
   case 2: TypeCategory.FLOAT;
   case 3: TypeCategory.DOUBLE;
   case 4: TypeCategory.LONG;
   case 5: TypeCategory.BYTE;
   case 6: TypeCategory.CHAR;
   case 7: TypeCategory.BOOLEAN;
   case 1001: TypeCategory.CONTAINER;
   case 1002: TypeCategory.MESSAGE;
   case 1003: TypeCategory.ARRAY;
   default: TypeCategory.NONE;
  */

  private def typeCategoryToOracleType(typeCategoryValue: Int) : String = {
    typeCategoryValue match {
      case 0 => "NUMBER"
      case 1 => "VARCHAR2(4000)" // oracle doesn't support a varchar2 without specifying an upper bound to length of the string. 
      case 2 => "NUMBER"
      case 3 => "NUMBER"
      case 4 => "NUMBER"
      case 5 => "NUMBER"
      case 6 => "CHAR(1)"
      case 7 => "VARCHAR2(5)" // A Boolean attribute translates to a string 'true' or 'false'
      case 1001 => "CONTAINER"
      case 1002 => "MESSAGE"
      case 1003 => "ARRAY"
      case _ => null
    }
  }

  /**
    *
    * @param tnxCtxt
    * @param outputContainers
    */
  override def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): Unit = {
    if (outputContainers.size == 0){
      logger.info("No data to send");
      return;
    }

    if( oracleAdapter == null ){
      oracleAdapter = CreateOracleAdapter(inputConfig.adapterSpecificCfg);
    }

    val containersByType = outputContainers.groupBy(c => c.FullName().toLowerCase)
    logger.debug("containersByType => " + containersByType);

    var keyColumns = new Array[String](0);
    breakable {
      containersByType.foreach(kv => {
	val containerName = kv._1
	val containers = kv._2
	if (! kv._2(0).IsFixed()) {
          logger.error("The container %s is not fixed type. Not supported by this adapter".format(kv._2(0).FullName()));
	  break;
	}
	else{
	  // create table, one call for each containerType
	  val container = kv._2(0);
	  val columns   = container.getAttributeNames();
	  val attrTypes   = container.getAttributeTypes();
	  var colNamesAndTypes = new Array[(String,String)](0);
	  attrTypes.foreach(a => {
	    val ati = container.getAttributeType(a.getName);
	    val typeCategoryValue = ati.getTypeCategory().getValue();
	    var colType = typeCategoryToOracleType(typeCategoryValue);
	    if( colType == null ){
	      throw new Exception("The typeCategory %d for the column %s is not supported ".format(typeCategoryValue,a));
	    }
	    colNamesAndTypes = colNamesAndTypes :+ (a.getName,colType);
	  })
	  
	  keyColumns = container.getPrimaryKeyNames();
	  keyColumns.foreach(k => { logger.info("key column => %s".format(k)) })
	  oracleAdapter.createTable(container.getTypeName(),colNamesAndTypes, keyColumns,"ddl");

	  // populate rows
	  var rowColumnValues = new Array[Array[(String,String)]](0)
	  containers.foreach(cont => {
	    // gather fieldName and corresponding values
	    val fields = cont.getAllAttributeValues();
	    var fieldValues = new Array[(String,String)](0)
	    fields.foreach(fld => {
	      val fn = fld.getValueType().getName();
	      val fv = fld.getValue().toString();
	      logger.info("fn => %s,fv => %s".format(fn,fv));
	      fieldValues = fieldValues :+ (fn,fv);
	    });
	    rowColumnValues = rowColumnValues :+ fieldValues;
	  })
	  oracleAdapter.put(container.getTypeName(), columns, rowColumnValues);
	}
      })
    }
  }
  
  override def send(messages: Array[Array[Byte]], partitionKeys: Array[Array[Byte]]): Unit = {
    throw new Exception("send non-fixed messages is not yet implemented")  // throw error not implemented
  }

  override def Shutdown(): Unit = {
    if (logger.isWarnEnabled) logger.warn(inputConfig.Name + " Shutdown detected")

    // Shutdown HB
    isShutdown = true

    try {
      if (oracleAdapter != null)
        oracleAdapter.Shutdown;
    } catch {
      case e: Exception => {}
      case e: Throwable => {}
    }
    oracleAdapter = null
  }
}

