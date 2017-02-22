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

import com.ligadata.KamanjaBase.{ ContainerInterface, TransactionContext, NodeContext }
import org.apache.logging.log4j.{ Logger, LogManager }
import java.io._
import java.util.zip.{ ZipException, GZIPOutputStream }
import java.nio.file.{ Paths, Files }
import com.ligadata.InputOutputAdapterInfo._
import com.ligadata.AdaptersConfiguration.FileAdapterConfiguration
import com.ligadata.Exceptions.{ FatalAdapterException }
import com.ligadata.HeartBeat.{ Monitorable, MonitorComponentInfo }
import org.json4s.jackson.Serialization
import com.ligadata.VelocityMetrics._

object FileProducer extends OutputAdapterFactory {
  val ADAPTER_DESCRIPTION = "File Producer"
  def CreateOutputAdapter(inputConfig: AdapterConfiguration, nodeContext: NodeContext): OutputAdapter = new FileProducer(inputConfig, nodeContext)
}

class FileProducer(val inputConfig: AdapterConfiguration, val nodeContext: NodeContext) extends OutputAdapter {
  private[this] val _lock = new Object()
  private[this] val LOG = LogManager.getLogger(getClass);

  private[this] val fc = FileAdapterConfiguration.GetAdapterConfig(inputConfig)
  private var os: OutputStream = null
  private val NEW_LINE = "\n".getBytes("UTF8")
  private val FAIL_WAIT = 2000
  private var numOfRetries = 0
  private var MAX_RETRIES = 3
  private val GZ = "gz"
  private var startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var lastSeen = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis))
  private var metrics: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map[String, Any]()

  var getFileVelocityInstances = vm.getFileVelocityInstances(VMFactory, Category, inputConfig.Name, inputConfig.fullAdapterConfig, nodeContext)

  //BUGBUG:: Not validating the values in FileAdapterConfiguration 

  //BUGBUG:: Open file to write the data
  // Taking only first file, if exists
  val sFileName = if (fc.Files != null && fc.Files.size > 0) fc.Files(0).trim else null
  if (sFileName == null || sFileName.size == 0)
    throw FatalAdapterException("First File Name should not be NULL or empty", new Exception("Invalid Parameters"))

  // Initialize the type of a file to Write to... Currently handles only TXT and GZ formats.
  val compString = if (fc.CompressionString == null) null else fc.CompressionString.trim

  while (os == null) {
    try {
      if (compString == null || compString.size == 0) {
        os = new FileOutputStream(sFileName, fc.append);
      } else if (compString.compareToIgnoreCase(GZ) == 0) {
        os = new GZIPOutputStream(new FileOutputStream(sFileName, fc.append)) // fc.append make sense here??
      } else {
        throw new Exception("Invalid Parameters")
      }
    } catch {
      case zio: ZipException => { throw FatalAdapterException("File Corruption (bad compression)", zio) }
      case fio: IOException => {
        LOG.warn("File input adapter " + fc.Name + ": Unable to create a file destination " + sFileName + " due to an IOException", fio)
        if (numOfRetries > MAX_RETRIES) {
          LOG.error("File input adapter " + fc.Name + ":Unable to create a file destination after " + MAX_RETRIES + " tries.  Aborting.")
          throw FatalAdapterException("Unable to open connection to specified file after " + MAX_RETRIES + " retries", fio)
        }
        numOfRetries += 1
        LOG.warn("File input adapter " + fc.Name + ": Retyring " + numOfRetries + "/" + MAX_RETRIES)
        Thread.sleep(FAIL_WAIT)
      }
      case e: Exception => { throw FatalAdapterException("Unable to open connection to specified file ", e) }
    }
    LOG.info("File input adapter " + fc.Name + ": Output adapter file destination is " + sFileName)
    numOfRetries = 0
  }

  override def getComponentStatusAndMetrics: MonitorComponentInfo = {
    implicit val formats = org.json4s.DefaultFormats
    return new MonitorComponentInfo(AdapterConfiguration.TYPE_OUTPUT, fc.Name, FileProducer.ADAPTER_DESCRIPTION, startTime, lastSeen, Serialization.write(metrics).toString)
  }

  override def getComponentSimpleStats: String = {
    ""
  }

  override def send(messages: Array[Array[Byte]], partitionKey: Array[Array[Byte]]): Unit = {
    try {
      // Op is not atomic
      messages.foreach(message => {
        var isSuccess = false
        numOfRetries = 0
        while (!isSuccess) {
          try {
            os.write(message ++ NEW_LINE);
            isSuccess = true
          } catch {
            case zio: ZipException => {
              LOG.error("File input adapter " + fc.Name + ": File Corruption (bad compression)", zio)
              throw zio
            }
            case fio: IOException => {
              LOG.warn("File input adapter " + fc.Name + ": Unable to write to file " + sFileName)
              if (numOfRetries >= MAX_RETRIES) {
                LOG.error("File input adapter " + fc.Name + ": Unable to create a file destination after " + MAX_RETRIES + " tries.  Aborting.", fio)
                throw FatalAdapterException("Unable to open connection to specified file after " + MAX_RETRIES + " retries", fio)
              }
              numOfRetries += 1
              LOG.warn("File input adapter " + fc.Name + ": Retyring " + numOfRetries + "/" + MAX_RETRIES)
              Thread.sleep(FAIL_WAIT)
            }
            case e: Exception => {
              LOG.error("File input adapter " + fc.Name + ": Unable to write output message: " + new String(message), e)
              throw e
            }
          }
        }
      })
      // val key = Category + "/" + fc.Name + "/evtCnt"
      // cntrAdapter.addCntr(key, messages.size) // for now adding rows
    } catch {
      case e: Exception => {
        LOG.error("File input adapter " + fc.Name + ": Failed to send", e)
        throw FatalAdapterException("Unable to send message", e)
      }
    }
  }

  // Locking before we write into file
  // To send an array of messages. messages.size should be same as partKeys.size
  override def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): Unit = _lock.synchronized {
    if (outputContainers.size == 0) return

    val (outContainers, serializedContainerData, serializerNames) = serialize(tnxCtxt, outputContainers)

    if (outContainers.size != serializedContainerData.size || outContainers.size != serializerNames.size) {
      LOG.error("File input adapter " + fc.Name + ": Messages, messages serialized data & serializer names should has same number of elements. Messages:%d, Messages Serialized data:%d, serializerNames:%d".format(outContainers.size, serializedContainerData.size, serializerNames.size))
      //TODO Need to record an error here... is this a job for the ERROR Q?
      return
    }

    if (serializedContainerData.size == 0) return

    try {
      // Op is not atomic
      serializedContainerData.foreach(message => {
        var isSuccess = false
        numOfRetries = 0
        while (!isSuccess) {
          try {
            os.write(message ++ NEW_LINE);
            isSuccess = true
          } catch {
            case zio: ZipException => {
              LOG.error("File input adapter " + fc.Name + ": File Corruption (bad compression)", zio)
              throw zio
            }
            case fio: IOException => {
              LOG.warn("File input adapter " + fc.Name + ": Unable to write to file " + sFileName)
              if (numOfRetries >= MAX_RETRIES) {
                LOG.error("File input adapter " + fc.Name + ": Unable to create a file destination after " + MAX_RETRIES + " tries.  Aborting.", fio)
                throw FatalAdapterException("Unable to open connection to specified file after " + MAX_RETRIES + " retries", fio)
              }
              numOfRetries += 1
              LOG.warn("File input adapter " + fc.Name + ": Retyring " + numOfRetries + "/" + MAX_RETRIES)
              Thread.sleep(FAIL_WAIT)
            }
            case e: Exception => {
              LOG.error("File input adapter " + fc.Name + ": Unable to write output message: " + new String(message), e)
              throw e
            }
          }
        }
      })
      // val key = Category + "/" + fc.Name + "/evtCnt"
      // cntrAdapter.addCntr(key, messages.size) // for now adding rows
      /****VelocityMetrics****/
      if (outContainers != null && outContainers.size > 0) {
        val nodeId = nodeContext.getEnvCtxt().getNodeId()
        for (i <- 0 until outContainers.size) {
          getOAVelocityMetrics(this, outContainers(i), true)
        }
      }
      getFileVelocityMetrics(this, fc.Name, true)

    } catch {
      case e: Exception => {
        LOG.error("File input adapter " + fc.Name + ": Failed to send", e)
        getFileVelocityMetrics(this, fc.Name, false)
        throw FatalAdapterException("Unable to send message", e)
      }
    }
  }

  override def Shutdown(): Unit = _lock.synchronized {
    if (os != null)
      os.close
  }

  private def getOAVelocityMetrics(output: OutputAdapter, message: ContainerInterface, processed: Boolean) = {
    val vmInstances = output.getVelocityInstances
    if (vmInstances != null && vmInstances.length > 0) {
      for (i <- 0 until vmInstances.length) {
        output.vm.incrementIAVelocityMetrics(vmInstances(i), message, processed)
      }
    }
  }

  /* Get Velocity Metrics for Output Adapter   */
  private def getFileVelocityMetrics(output: OutputAdapter, fileName: String, processed: Boolean) = {
    val vmInstances = this.getFileVelocityInstances
    if (vmInstances != null && vmInstances.length > 0) {
      for (i <- 0 until vmInstances.length) {
        output.vm.incrementFileVMetrics(vmInstances(i), fileName, processed)
      }
    }
  }

}

