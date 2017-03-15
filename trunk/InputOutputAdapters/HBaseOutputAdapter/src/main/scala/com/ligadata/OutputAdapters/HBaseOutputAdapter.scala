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

package com.ligadata.OutputAdapters

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
import com.ligadata.KamanjaBase.{ContainerInterface, TransactionContext, NodeContext}
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.Exceptions.{UnsupportedOperationException, FatalAdapterException}
import com.ligadata.HeartBeat.{Monitorable, MonitorComponentInfo}
import org.json4s.jackson.Serialization
import org.apache.hadoop.hdfs.DFSOutputStream
import org.apache.hadoop.hdfs.client.HdfsDataOutputStream.SyncFlag
import org.apache.hadoop.fs.{FileSystem, FSDataOutputStream, Path}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.security.UserGroupInformation
import org.apache.commons.lang3.time.FastDateFormat
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.compressors.CompressorOutputStream
import parquet.avro.AvroParquetWriter
import org.apache.avro.generic.GenericRecordBuilder
import parquet.hadoop.metadata.CompressionCodecName
import parquet.schema.MessageTypeParser
import scala.collection.mutable.ArrayBuffer
import scala.actors.threadpool.{ExecutorService, Executors}
import scala.util.control.Breaks._
import com.ligadata.VelocityMetrics._
import java.util.{Date, Calendar, TimeZone}
import java.text.{SimpleDateFormat}

import parquet.hadoop._
import parquet.hadoop.api.WriteSupport
import com.ligadata.Utils.KamanjaLoaderInfo

/*
import com.ligadata.KvBase._
import com.ligadata.StorageBase._
import com.ligadata.Serialize._
import com.ligadata.keyvaluestore.HBaseAdapter
*/

object HBaseOutputAdapter extends OutputAdapterFactory {
  def CreateOutputAdapter(inputConfig: AdapterConfiguration, nodeContext: NodeContext): OutputAdapter = new HBaseOutputAdapter(inputConfig, nodeContext)
}

class HBaseOutputAdapter(val inputConfig: AdapterConfiguration, val nodeContext: NodeContext) extends OutputAdapter {
  private[this] val logger = LogManager.getLogger(getClass.getName);
  private val kvManagerLoader = new KamanjaLoaderInfo
  private var hbaseAdapter: HBaseAdapter = null;
  private val maxConnectionAttempts = 10;
  private var isShutdown = false
  
  // 1. Write function(s) to create HBASE connectivity
  private def CreateHbaseAdapter(dataStoreInfo: String): HBaseAdapter = {
    var connectionAttempts = 0

    while (connectionAttempts < maxConnectionAttempts) {
      try {
	logger.info("Creating HBase connection...")
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

  private def getCurrentTimeAsString: String = {
    val df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    // Get the date today using Calendar object.
    val today = Calendar.getInstance().getTime();        
    // Using DateFormat format method we can create a string 
    // representation of a date with the defined format.
    val reportDate = df.format(today);
    return reportDate;
  }

  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    val lastSeen = getCurrentTimeAsString;
    val startTime = getCurrentTimeAsString;
    return new MonitorComponentInfo(AdapterConfiguration.TYPE_OUTPUT, inputConfig.Name, "", startTime, lastSeen, "")
  }

  override def getComponentSimpleStats: String = {
    return ""
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

    if( hbaseAdapter == null ){
      hbaseAdapter = CreateHbaseAdapter(inputConfig.adapterSpecificCfg);
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
	  keyColumns = container.getPrimaryKeyNames();
	  hbaseAdapter.createAnyTable(container.FullName(),columns,"ddl");
	}	  
	// populate rows
	containers.foreach(cont => {
	  // gather fieldName and corresponding values
	  val fields = cont.getAllAttributeValues();
	  var fieldValues = new Array[(String,String,String)](0)
	  fields.foreach(fld => {
	      val fn = fld.getValueType().getName();
	      /* No special processing based on the field type at this time
	      val fv = fn match {
		case BOOLEAN => fld.getValue().toString()
		case BYTE => fld.getValue().toString()
		case CHAR => fld.getValue().toString()
		case LONG => fld.getValue().toString()
		case INT => fld.getValue().toString()
		case FLOAT => fld.getValue().toString()
		case DOUBLE => fld.getValue().toString()
		case STRING => fld.getValue().toString()
		case _ => throw new UnsupportedObjectException(s"HBaseOutputAdapter got unsupported type, typeId: $fn", null)
	      }
	      */
	      val fv = fld.getValue().toString()
	      fieldValues = fieldValues :+ (fn,fn,fv)
	  });
	  hbaseAdapter.put(containerName, keyColumns, fieldValues);
	});
	//put(containerName, containersValues, schema)
      })
    }
  }

  override def send(messages: Array[Array[Byte]], partitionKeys: Array[Array[Byte]]): Unit = {
    // throw error not implemented
  }

  override def Shutdown(): Unit = {
    if (logger.isWarnEnabled) logger.warn(inputConfig.Name + " Shutdown detected")

    // Shutdown HB
    isShutdown = true

    try {
      if (hbaseAdapter != null)
        hbaseAdapter.Shutdown;
    } catch {
      case e: Exception => {}
      case e: Throwable => {}
    }
    hbaseAdapter = null
  }
}

