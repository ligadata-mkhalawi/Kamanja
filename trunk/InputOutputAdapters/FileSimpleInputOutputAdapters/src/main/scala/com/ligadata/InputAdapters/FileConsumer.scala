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

package com.ligadata.InputAdapters

import org.json4s.jackson.Serialization

import scala.actors.threadpool.{ Executors, ExecutorService }
import org.apache.logging.log4j.{ Logger, LogManager }
import java.io.{ InputStream, FileInputStream }
import java.util.zip.GZIPInputStream
import java.nio.file.{ Paths, Files }
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.AdaptersConfiguration.{ FileAdapterConfiguration, FilePartitionUniqueRecordKey, FilePartitionUniqueRecordValue }
import scala.util.control.Breaks._
import com.ligadata.KamanjaBase.{NodeContext, DataDelimiters}
import com.ligadata.HeartBeat.{Monitorable, MonitorComponentInfo}
import com.ligadata.Utils.Utils

object FileConsumer extends InputAdapterFactory {
  val ADAPTER_DESCRIPTION = "File Consumer"
  def CreateInputAdapter(inputConfig: AdapterConfiguration, execCtxtObj: ExecContextFactory, nodeContext: NodeContext): InputAdapter = new FileConsumer(inputConfig, execCtxtObj, nodeContext)
}

class FileConsumer(val inputConfig: AdapterConfiguration, val execCtxtObj: ExecContextFactory, val nodeContext: NodeContext) extends InputAdapter {
  private[this] val LOG = LogManager.getLogger(getClass);

  private[this] val fc = FileAdapterConfiguration.GetAdapterConfig(inputConfig)
  private[this] var uniqueKey: FilePartitionUniqueRecordKey = new FilePartitionUniqueRecordKey
  private[this] val lock = new Object()
  private var startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var metrics: scala.collection.mutable.Map[String,Any] = scala.collection.mutable.Map[String,Any]()

  uniqueKey.Name = "File"

  var executor: ExecutorService = _

  val input = this

  val execThread = execCtxtObj.CreateExecContext(input, uniqueKey, nodeContext)

  class Stats {
    var totalLines: Long = 0;
    var totalSent: Long = 0
  }

  override  def getComponentStatusAndMetrics: MonitorComponentInfo = {
    implicit val formats = org.json4s.DefaultFormats
    return new MonitorComponentInfo(AdapterConfiguration.TYPE_OUTPUT, fc.Name, FileConsumer.ADAPTER_DESCRIPTION, startTime, lastSeen,  Serialization.write(metrics).toString)
  }

  override def getComponentSimpleStats: String = {
    ""
  }

  private def ProcessFile(sFileName: String, msg: String, st: Stats, ignorelines: Int, AddTS2MsgFlag: Boolean, isGz: Boolean): Unit = {
    var is: InputStream = null

    LOG.debug("FileConsumer Processing File:" + sFileName)

    try {
      if (isGz)
        is = new GZIPInputStream(new FileInputStream(sFileName))
      else
        is = new FileInputStream(sFileName)
    } catch {
      case e: Exception =>
        LOG.error("Failed to open FileConsumer for %s.".format(sFileName), e)
        throw e
        return
    }

    val uniqueVal = new FilePartitionUniqueRecordValue
    uniqueVal.FileFullPath = sFileName

    val trimMsg = if (msg != null) msg.trim else null
    var len = 0
    var readlen = 0
    var totalLen: Int = 0
    var locallinecntr: Int = 0
    val maxlen = 1024 * 1024
    val buffer = new Array[Byte](maxlen)
    var tm = System.nanoTime
    var ignoredlines = 0
    try {
      breakable {
        do {
          readlen = is.read(buffer, len, maxlen - 1 - len)
          if (readlen > 0) {
            totalLen += readlen
            len += readlen
            var startidx: Int = 0
            var isrn: Boolean = false
            for (idx <- 0 until len) {
              if ((isrn == false && buffer(idx) == '\n') || (buffer(idx) == '\r' && idx + 1 < len && buffer(idx + 1) == '\n')) {
                locallinecntr += 1
                var strlen = idx - startidx
                if (ignoredlines < ignorelines) {
                  ignoredlines += 1
                } else {
                  if (strlen > 0) {
                    var readTmMs = System.currentTimeMillis

                    val ln = new String(buffer, startidx, idx - startidx)
                    val sendmsg = (if (trimMsg != null && trimMsg.isEmpty() == false) (trimMsg + ",") else "") + (if (AddTS2MsgFlag) (readTmMs.toString + ",") else "") + ln

                    try {
                      // Creating new string to convert from Byte Array to string
                      uniqueVal.Offset = 0 //BUGBUG:: yet to fill this information
                      execThread.execute(sendmsg.getBytes, uniqueKey, uniqueVal, readTmMs)
                    } catch {
                      case e: Exception => {
                        LOG.error("", e)
                      }
                    }

                    st.totalSent += sendmsg.size
                  }
                }

                if (executor.isShutdown)
                  break

                startidx = idx + 1;
                if (buffer(idx) == '\r') // Inc one more char in case of \r \n
                {
                  startidx += 1;
                  isrn = true
                }
                st.totalLines += 1;

                // val key = Category + "/" + fc.Name + "/evtCnt"
                // cntrAdapter.addCntr(key, 1)

                val curTm = System.nanoTime
                if ((curTm - tm) > 1000000000L) {
                  tm = curTm
                  LOG.debug("Time:%10dms, Lns:%8d, Sent:%15d".format(curTm / 1000000, st.totalLines, st.totalSent))
                }
              } else {
                isrn = false
              }
            }

            var destidx: Int = 0
            // move rest of the data left to starting of the buffer
            for (idx <- startidx until len) {
              buffer(destidx) = buffer(idx)
              destidx += 1
            }
            len = destidx
          }
        } while (readlen > 0)

        if (len > 0 && ignoredlines >= ignorelines) {
          var readTmMs = System.currentTimeMillis

          val ln = new String(buffer, 0, len)
          val sendmsg = (if (trimMsg != null && trimMsg.isEmpty() == false) (trimMsg + ",") else "") + (if (AddTS2MsgFlag) (readTmMs.toString + ",") else "") + ln

          try {
            // Creating new string to convert from Byte Array to string
            uniqueVal.Offset = 0 //BUGBUG:: yet to fill this information
            execThread.execute(sendmsg.getBytes, uniqueKey, uniqueVal, readTmMs)
          } catch {
            case e: Exception => {
              LOG.error("", e)
            }
          }

          st.totalSent += sendmsg.size
          st.totalLines += 1;
          val key = Category + "/" + fc.Name + "/evtCnt"
          // cntrAdapter.addCntr(key, 1)
        }
      }
    } catch {
      case e: Exception => {
        LOG.error("", e)
      }
    }

    val curTm = System.nanoTime
    LOG.debug("Time:%10dms, Lns:%8d, Sent:%15d, Last, file:%s".format(curTm / 1000000, st.totalLines, st.totalSent, sFileName))
    is.close();
  }

  private def elapsedTm[A](f: => A): Long = {
    val s = System.nanoTime
    f
    (System.nanoTime - s)
  }

  override def Shutdown: Unit = lock.synchronized {
    StopProcessing
  }

  override def StopProcessing: Unit = lock.synchronized {
    if (executor == null) return

    executor.shutdown

    while (executor.isTerminated == false) {
      Thread.sleep(100) // sleep 100ms and then check
    }

    executor = null
  }

  // Each value in partitionInfo is (PartitionUniqueRecordKey, PartitionUniqueRecordValue, Long, PartitionUniqueRecordValue) key, processed value, Start transactionid, Ignore Output Till given Value (Which is written into Output Adapter)
  override def StartProcessing(partitionInfo: Array[StartProcPartInfo], ignoreFirstMsg: Boolean): Unit = lock.synchronized {
    if (partitionInfo == null || partitionInfo.size == 0)
      return

    executor = Executors.newFixedThreadPool(1, Utils.GetScalaThreadFactory(inputConfig.Name + "-executor-%d"))
    executor.execute(new Runnable() {
      override def run() {

        val s = System.nanoTime

        var tm: Long = 0
        val st: Stats = new Stats
        val compString = if (fc.CompressionString == null) null else fc.CompressionString.trim
        val isTxt = (compString == null || compString.size == 0)
        val isGz = (compString != null && compString.compareToIgnoreCase("gz") == 0)
        fc.Files.foreach(fl => {
          if (isTxt || isGz) {
            tm = tm + elapsedTm(ProcessFile(fl, fc.MessagePrefix, st, fc.IgnoreLines, fc.AddTS2MsgFlag, isGz))
          } else {
            throw new Exception("Not yet handled other than text & GZ files")
          }
          if (executor.isShutdown) {
            LOG.debug("Stop processing File:%s in the middle ElapsedTime:%.02fms".format(fl, tm / 1000000.0))
            break
          } else {
            LOG.debug("File:%s ElapsedTime:%.02fms".format(fl, tm / 1000000.0))
          }
        })
        LOG.debug("Done. ElapsedTime:%.02fms".format((System.nanoTime - s) / 1000000.0))
      }
    });

  }

  override def GetAllPartitionUniqueRecordKey: Array[PartitionUniqueRecordKey] = lock.synchronized {
    if (uniqueKey != null) {
      return Array(uniqueKey)
    }
    null
  }

  override def DeserializeKey(k: String): PartitionUniqueRecordKey = {
    val key = new FilePartitionUniqueRecordKey
    try {
      LOG.debug("Deserializing Key:" + k)
      key.Deserialize(k)
    } catch {
      case e: Exception => {
        LOG.error("Failed to deserialize Key:%s.".format(k), e)
        throw e
      }
    }
    key
  }

  override def DeserializeValue(v: String): PartitionUniqueRecordValue = {
    val vl = new FilePartitionUniqueRecordValue
    if (v != null) {
      try {
        LOG.debug("Deserializing Value:" + v)
        vl.Deserialize(v)
      } catch {
        case e: Exception => {

          LOG.error("Failed to deserialize Value:%s.".format(v), e)
          throw e
        }
      }
    }
    vl
  }

  // Not yet implemented
  override def getAllPartitionBeginValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = {
    return Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]()
  }

  // Not yet implemented
  override def getAllPartitionEndValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)] = {
    return Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]()
  }
}

