
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

package com.ligadata.tools

import java.text.SimpleDateFormat

import org.apache.kafka.clients.producer.{RecordMetadata, Callback, ProducerRecord}

import scala.actors.threadpool.{Executors, TimeUnit}
import scala.collection.mutable.ArrayBuffer
import java.util.{TimeZone, Arrays, Properties}
import java.io.{InputStream, ByteArrayInputStream}
import java.util.zip.GZIPInputStream
import java.nio.file.{Files, Paths}
import org.apache.logging.log4j._
import com.ligadata.KamanjaVersion.KamanjaVersion

/**
  * Object used to insert messages from a specified source (files for now) to a specified Kafka queues
  */
object DemoKafkaProducer {
  private val dtFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  private val dtFormatWithMS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
  private var adjustTime: Long = 0

  class DataToPush {
    var prevTime: Long = -1
    var curTime: Long = -1
    var lastSentTime: Long = -1
    var curMsgs = ArrayBuffer[ProducerRecord[Array[Byte], Array[Byte]]]()
    var dataStartProcessing: Long = -1
    var originalTimeInData: Long = 0
  }

  class Stats {
    var totalLines: Long = 0;
    var totalRead: Long = 0;
    var totalSent: Long = 0
  }

  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)

  val randomPartitionCntr = new java.util.Random

  def send(producer: org.apache.kafka.clients.producer.KafkaProducer[Array[Byte], Array[Byte]], topic: String, curMsgs: Array[ProducerRecord[Array[Byte], Array[Byte]]]): Unit = {
    try {
      // Send the request to Kafka
      val resps = curMsgs.map(msg => {
        // Send the request to Kafka
        val response = producer.send(msg, new Callback {
          override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit = {
            if (exception != null) {
              logger.error("DEMO Producer - Kafka has detected a problem with pushing a message into the " + msg.topic, exception)
              sys.exit(1)
            }
          }
        })
        response
      })

      // Waiting to push
      resps.foreach(r => r.get)
    } catch {
      case e: Exception =>
        logger.error("Failed to send data to Kafka topic:" + topic, e)
        sys.exit(1)
    }
  }

  /**
    * ProcessFile - Process a file specified by the below parameters
    *
    * @param producer         - Producer[AnyRef, AnyRef]
    * @param topics           - Array[String]
    * @param threadId         - Int
    * @param sFileName        - String
    * @param partitionkeyidxs Any
    * @param st               - Stats
    * @param ignorelines      - Int
    * @param format           - String
    * @param isGzip           Boolean
    * @param isVerbose        Boolean
    */
  def ProcessFile(producer: org.apache.kafka.clients.producer.KafkaProducer[Array[Byte], Array[Byte]], topics: Array[String], threadId: Int, sFileName: String, partitionkeyidxs: Any, st: Stats, ignorelines: Int, format: String, isGzip: Boolean, isVerbose: Boolean): Unit = {

    var bis: InputStream = null

    // If from the Gzip, wrap around a GZIPInput Stream, else... use the ByteArrayInputStream
    if (isGzip) {
      bis = new GZIPInputStream(new ByteArrayInputStream(Files.readAllBytes(Paths.get(sFileName))))
    } else {
      bis = new ByteArrayInputStream(Files.readAllBytes(Paths.get(sFileName)))
    }

    try {
      if (format.equalsIgnoreCase("csv")) {
        processCSVFile(producer, topics, threadId, sFileName, partitionkeyidxs.asInstanceOf[Array[Int]], st, ignorelines, bis, isVerbose)
      } else {
        throw new Exception("Only following formats are supported: CSV")
      }
    } catch {
      case e: Exception => logger.error("Error reading from a file", e)
    }
    finally {
      if (bis != null) bis.close
    }
  }

  private def getPartition(key: Array[Byte], numPartitions: Int): Int = {
    if (numPartitions == 0) return 0
    if (key != null) {
      try {
        return (scala.math.abs(Arrays.hashCode(key)) % numPartitions)
      } catch {
        case e: Exception => {
          throw e
        }
        case e: Throwable => {
          throw e
        }
      }
    }
    return randomPartitionCntr.nextInt(numPartitions)
  }

  private def CheckTimeAndSend(producer: org.apache.kafka.clients.producer.KafkaProducer[Array[Byte], Array[Byte]], topics: Array[String], sendmsg: String, key: String, tmVal: String, curData: DataToPush, topicPartitionsCount: Int, tmInMs: Long): Unit = {
    // Get current time
    val topic = topics(0)
    val tmInMsRoundedToSec = (tmInMs / 1000) * 1000
    if (tmInMsRoundedToSec != 0 && curData.curTime == -1)
      curData.curTime = tmInMsRoundedToSec
    if (tmInMsRoundedToSec == 0 || curData.curTime == tmInMsRoundedToSec) {
      curData.curMsgs += new ProducerRecord(topic, getPartition(key.getBytes(), topicPartitionsCount), key.getBytes(), sendmsg.getBytes())
    } else {
      if (curData.prevTime != -1) {
        val sysTm = System.currentTimeMillis
        val waitTmInMs = (curData.curTime - curData.prevTime) - (sysTm - curData.lastSentTime)
        if (waitTmInMs > 0) {
          try {
            Thread.sleep(waitTmInMs)
          } catch {
            case e: Throwable => {}
          }
        }
      }

      val sysTm1 = System.currentTimeMillis
      logger.debug("Sending %d messages".format(curData.curMsgs.size))
      println("Sending %d messages for time %d at %d".format(curData.curMsgs.size, curData.curTime, sysTm1))
      send(producer, topics(0), curData.curMsgs.toArray) // BUGBUG:: Always using topics(0)
      curData.prevTime = curData.curTime
      curData.curTime = tmInMsRoundedToSec
      curData.lastSentTime = sysTm1
      curData.curMsgs.clear()
      curData.curMsgs += new ProducerRecord(topic, getPartition(key.getBytes(), topicPartitionsCount), key.getBytes(), sendmsg.getBytes())
    }
  }

  // Returns => key, original time column, orginal time in MS, time replaced new line
  private def extractKeyTmAndReplaceTime(inputData: String, partitionkeyidxs: Array[Int], timeKeyIdx: Int, keyPartDelim: String, curData: DataToPush): (String, String, Long, String) = {
    val str_arr = inputData.split(",", -1)
    if (str_arr.size == 0)
      throw new Exception("Not found any values in message")
    var key: String = ""
    var tmVal: String = ""
    var tmInMs: Long = 0
    var tmReplacedStr = inputData
    if (partitionkeyidxs.size > 0)
      key = partitionkeyidxs.map(idx => str_arr(idx)).mkString(keyPartDelim)

    if (timeKeyIdx >= 0) {
      tmVal = str_arr(timeKeyIdx).trim
      try {
        val hasMS = (tmVal.contains("."))
        // println("Parsing DateTime:" + tmVal + ", hasMS:" + hasMS)
        tmInMs = (if (hasMS) dtFormatWithMS.parse(tmVal) else dtFormat.parse(tmVal)).getTime
        if (curData.dataStartProcessing == -1) {
          // Rounding these values to sec level roundings
          curData.dataStartProcessing = (System.currentTimeMillis / 1000) * 1000
          curData.originalTimeInData = (tmInMs / 1000) * 1000
        }

        val newTm = curData.dataStartProcessing + (tmInMs - curData.originalTimeInData) + adjustTime
        val newTmStr = {
          val str = if (hasMS) dtFormatWithMS.format(new java.util.Date(newTm)) else dtFormat.format(new java.util.Date(newTm))
          if (tmVal.charAt(tmVal.size - 1) == 'Z')
            str + "Z"
          else if (tmVal.charAt(tmVal.size - 1) == 'z')
            str + "z"
          else
            str
        }
        str_arr(timeKeyIdx) = newTmStr
        tmReplacedStr = str_arr.mkString(",")
        // println("OldStr:%s, NewStr:%s, NewTimeStr:%s".format(inputData,tmReplacedStr, newTmStr))
      } catch {
        case e: Throwable => {
          logger.warn("Failed to parse datetime:" + tmVal, e)
        }
      }
    }

    (key, tmVal, tmInMs, tmReplacedStr)
  }

  /*
* processCSVFile - dealing with CSV File
 */
  private def processCSVFile(producer: org.apache.kafka.clients.producer.KafkaProducer[Array[Byte], Array[Byte]], topics: Array[String], threadId: Int, sFileName: String, partitionkeyidxs: Array[Int], st: Stats, ignorelines: Int, bis: InputStream, isVerbose: Boolean): Unit = {
    var len = 0
    var readlen = 0
    var totalLen: Int = 0
    var locallinecntr: Int = 0
    val maxlen = 1024 * 1024
    val buffer = new Array[Byte](maxlen)
    var tm = System.nanoTime
    var ignoredlines = 0
    var curTime = System.currentTimeMillis
    var curData = new DataToPush
    val topicPartitionsCount = producer.partitionsFor(topics(0)).size()

    do {
      readlen = bis.read(buffer, len, maxlen - 1 - len)
      if (readlen > 0) {
        totalLen += readlen
        len += readlen
        var startidx: Int = 0
        var isrn: Boolean = false
        for (idx <- 0 until len) {
          if ((isrn == false && buffer(idx) == '\n') || (buffer(idx) == '\r' && idx + 1 < len && buffer(idx + 1) == '\n')) {
            if ((locallinecntr % 20) == 0)
              curTime = System.currentTimeMillis
            locallinecntr += 1
            val strlen = idx - startidx
            if (ignoredlines < ignorelines) {
              ignoredlines += 1
            } else {
              if (strlen > 0) {
                val ln = new String(buffer, startidx, idx - startidx)
                val defPartKey = st.totalLines.toString
                val (key, tmVal, tmInMs, tmReplacedStr) = extractKeyTmAndReplaceTime(ln, partitionkeyidxs, 0, ".", curData)
                // Check time And send
                CheckTimeAndSend(producer, topics, tmReplacedStr, key, tmVal, curData, topicPartitionsCount, tmInMs)
                st.totalRead += ln.size
                st.totalSent += tmReplacedStr.size
              }
            }
            startidx = idx + 1;
            if (buffer(idx) == '\r') // Inc one more char in case of \r \n
            {
              startidx += 1;
              isrn = true
            }
            st.totalLines += 1;

            val curTm = System.nanoTime
            if ((curTm - tm) > 10 * 1000000000L) {
              tm = curTm
              println("Tid:%2d, Time:%10dms, Lines:%8d, Read:%15d, Sent:%15d".format(threadId, curTm / 1000000, st.totalLines, st.totalRead, st.totalSent))
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
      val ln = new String(buffer, 0, len)
      val defPartKey = st.totalLines.toString
      val (key, tmVal, tmInMs, tmReplacedStr) = extractKeyTmAndReplaceTime(ln, partitionkeyidxs, 0, ".", curData)
      CheckTimeAndSend(producer, topics, tmReplacedStr, key, tmVal, curData, topicPartitionsCount, tmInMs)
      st.totalRead += ln.size
      st.totalSent += tmReplacedStr.size
      st.totalLines += 1;
    }

    val curTm = System.nanoTime
    println("Tid:%2d, Time:%10dms, Lines:%8d, Read:%15d, Sent:%15d, Last, file:%s".format(threadId, curTm / 1000000, st.totalLines, st.totalRead, st.totalSent, sFileName))
  }

  /*
  * elapsed
   */
  private def elapsed[A](f: => A): Long = {
    val s = System.nanoTime
    f
    (System.nanoTime - s)
  }

  type OptionMap = Map[Symbol, Any]

  /*
  * nextOption - parsing input options
   */
  private def nextOption(map: OptionMap, list: List[String]): OptionMap = {
    def isSwitch(s: String) = (s(0) == '-')
    list match {
      case Nil => map
      case "--files" :: value :: tail =>
        nextOption(map ++ Map('files -> value), tail)
      case "--gz" :: value :: tail =>
        nextOption(map ++ Map('gz -> value), tail)
      case "--topics" :: value :: tail =>
        nextOption(map ++ Map('topics -> value), tail)
      case "--threads" :: value :: tail =>
        nextOption(map ++ Map('threads -> value), tail)
      case "--adjusttimezone" :: value :: tail =>
        nextOption(map ++ Map('adjusttimezone -> value), tail)
      case "--partitionkeyidxs" :: value :: tail =>
        nextOption(map ++ Map('partitionkeyidxs -> value), tail)
      case "--format" :: value :: tail =>
        nextOption(map ++ Map('format -> value), tail)
      case "--ignorelines" :: value :: tail =>
        nextOption(map ++ Map('ignorelines -> value), tail)
      case "--brokerlist" :: value :: tail =>
        nextOption(map ++ Map('brokerlist -> value), tail)
      case "--verbose" :: value :: tail =>
        nextOption(map ++ Map('verbose -> value), tail)
      case "--version" :: tail =>
        nextOption(map ++ Map('version -> "true"), tail)
      case option :: tail => {
        println("Unknown option " + option)
        sys.exit(1)
      }
    }
  }

  /*
  * Just a local debug method
   */
  private def printline(inString: String, isVerbose: Boolean): Unit = {
    if (!isVerbose) return
    println(inString)
  }

  /**
    * Entry point for this tool
    *
    * @param args - Array[Strings]
    */
  def main(args: Array[String]): Unit = {
    val options = nextOption(Map(), args.toList)
    val version = options.getOrElse('version, "false").toString
    if (version.equalsIgnoreCase("true")) {
      KamanjaVersion.print
      return
    }
    val sFilesNames = options.getOrElse('files, null).asInstanceOf[String]
    if (sFilesNames == null) {
      println("Need input files as parameter")
      sys.exit(1)
    }

    val sAllFls = sFilesNames.replace("\"", "").trim.split(",")
    val sAllTrimFls = sAllFls.map(flnm => flnm.trim)
    val sAllValidTrimFls = sAllTrimFls.filter(flnm => flnm.size > 0)

    val tmptopics = options.getOrElse('topics, "").toString.replace("\"", "").trim.toLowerCase.split(",").map(t => t.trim).filter(t => t.size > 0)

    if (tmptopics.size == 0) {
      println("Need queue(s)")
      sys.exit(1)
    }

    val topics = tmptopics.toList.sorted.toArray // Sort topics by names

    if (topics.size != 1) {
      println("Supporting only one topic")
      sys.exit(1)
    }

    val brokerlist = options.getOrElse('brokerlist, "").toString.replace("\"", "").trim // .toLowerCase

    if (brokerlist.size == 0) {
      println("Need Brokers list (brokerlist) in the format of HOST:PORT,HOST:PORT")
      sys.exit(1)
    }

    val gz = options.getOrElse('gz, "false").toString

    val adjusttimezone = options.getOrElse('adjusttimezone, "").toString.trim.toUpperCase

    if (adjusttimezone.size > 0) {
      if (adjusttimezone.startsWith("GMT") || adjusttimezone.startsWith("UTC")) {
        try {
          val adjTmZn = TimeZone.getTimeZone(adjusttimezone.replace("UTC", "GMT"))
          adjustTime = adjTmZn.getOffset(System.currentTimeMillis)
        } catch {
          case e: Throwable => {
            logger.error("Supported adjusttimezone are GMT & UTC related ones. Ignoring given adjusttimezone:" + adjusttimezone, e)
          }
        }
      } else {
        logger.error("Supported adjusttimezone are GMT & UTC related ones. Ignoring given adjusttimezone:" + adjusttimezone)
      }
    }

    println("AdjustmentOffset:" + adjustTime + " for given adjusttimezone String:" + adjusttimezone)
    val threads = options.getOrElse('threads, "0").toString.toInt

    if (threads <= 0) {
      println("Threads must be more than 0")
      sys.exit(1)
    }

    val ignorelines = options.getOrElse('ignorelines, "0").toString.toInt

    val format = options.getOrElse('format, "").toString

    var partitionkeyidxs: Any = null

    // This is the CSV path, partitionkeyidx is in the Array[Int] format
    // If this is old path... keep as before for now....
    partitionkeyidxs = options.getOrElse('partitionkeyidxs, "").toString.replace("\"", "").trim.split(",").map(part => part.trim).filter(part => part.size > 0).map(part => part.toInt)

    val isVerbose = options.getOrElse('verbose, "false").toString

    val props = new Properties()

    val default_compression_type = "none" // Valida values at this moment are none, gzip, or snappy.
    val default_value_serializer = "org.apache.kafka.common.serialization.ByteArraySerializer"
    val default_key_serializer = "org.apache.kafka.common.serialization.ByteArraySerializer"
    val default_batch_size = "1024"
    val default_linger_ms = "50" // 50ms
    // val default_retries = "0"
    val default_block_on_buffer_full = "true" // true or false
    val default_buffer_memory = "16777216" // 16MB
    val default_client_id = "Client1"
    val default_request_timeout_ms = "10000"
    val default_timeout_ms = "10000"
    val default_metadata_fetch_timeout_ms = "10000"
    val defrault_metadata_max_age_ms = "20000"
    val default_max_block_ms = "20000"
    val default_max_buffer_full_block_ms = "100"
    val default_network_request_timeout_ms = "20000"
    val default_outstanding_messages = "2048"

    props.put("bootstrap.servers", brokerlist); // ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
    props.put("compression.type", default_compression_type.toString.trim()); // ProducerConfig.COMPRESSION_TYPE_CONFIG
    props.put("value.serializer", default_value_serializer.toString.trim()); // ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
    props.put("key.serializer", default_key_serializer.toString.trim()); // ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
    props.put("batch.size", default_batch_size.toString.trim()); // ProducerConfig.BATCH_SIZE_CONFIG
    props.put("linger.ms", default_linger_ms) // ProducerConfig.LINGER_MS_CONFIG
    props.put("block.on.buffer.full", default_block_on_buffer_full.toString.trim()) // ProducerConfig.BLOCK_ON_BUFFER_FULL_CONFIG
    props.put("buffer.memory", default_buffer_memory.toString.trim()) // ProducerConfig.BUFFER_MEMORY_CONFIG
    props.put("client.id", default_client_id.toString.trim()) // ProducerConfig.CLIENT_ID_CONFIG
    props.put("request.timeout.ms", default_request_timeout_ms.toString.trim())
    props.put("timeout.ms", default_timeout_ms)
    props.put("metadata.fetch.timeout.ms", default_metadata_fetch_timeout_ms)
    props.put("metadata.max.age.ms", defrault_metadata_max_age_ms.toString.trim())
    props.put("max.block.ms", default_max_block_ms.toString.trim())
    props.put("max.buffer.full.block.ms", default_max_buffer_full_block_ms.toString.trim())
    props.put("network.request.timeout.ms", default_network_request_timeout_ms.toString.trim())

    val s = System.nanoTime

    if (sAllValidTrimFls.size > 0) {
      var idx = 0

      val flsLists = if (sAllValidTrimFls.size > threads) threads else sAllValidTrimFls.size

      val executor = Executors.newFixedThreadPool(flsLists)
      val FilesForThreads = new Array[ArrayBuffer[String]](flsLists)
      sAllValidTrimFls.foreach(fl => {
        val index = idx % flsLists
        if (FilesForThreads(index) == null)
          FilesForThreads(index) = new ArrayBuffer[String];
        FilesForThreads(index) += fl
        idx = idx + 1
      })

      idx = 0
      val runContinueously = true
      FilesForThreads.foreach(fls => {
        if (fls.size > 0) {
          executor.execute(new Runnable() {
            val threadNo = idx
            val flNames = fls.toArray
            var isGzip: Boolean = false

            override def run() {
              val producer = new org.apache.kafka.clients.producer.KafkaProducer[Array[Byte], Array[Byte]](props)
              var tm: Long = 0
              val st: Stats = new Stats
              try {
                do {
                  flNames.foreach(fl => {
                    if (gz.trim.compareToIgnoreCase("true") == 0) {
                      isGzip = true
                    }
                    tm = tm + elapsed(ProcessFile(producer, topics, threadNo, fl, partitionkeyidxs, st, ignorelines, format, isGzip, isVerbose.equalsIgnoreCase("true")))
                    println("%02d. File:%s ElapsedTime:%.02fms".format(threadNo, fl, tm / 1000000.0))
                  })
                } while (runContinueously)
              } catch {
                case e: Throwable => {
                  logger.error("Got Throwable", e)
                }
              } finally {
                producer.close
              }
            }
          })
        }
        idx = idx + 1
      })
      executor.shutdown();
      try {
        executor.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS);
      } catch {
        case e: Exception => {
          logger.debug("", e)
        }
      }
    }

    println("Done. ElapsedTime:%.02fms".format((System.nanoTime - s) / 1000000.0))
    sys.exit(0)

  }
}

