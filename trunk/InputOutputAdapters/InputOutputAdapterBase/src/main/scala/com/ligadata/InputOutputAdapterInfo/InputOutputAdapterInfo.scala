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

package com.ligadata.InputOutputAdapterInfo

import com.ligadata.Exceptions.{ KamanjaException, StackTrace }
import com.ligadata.KamanjaBase._
import com.ligadata.HeartBeat._
import com.ligadata.kamanja.metadata.MdMgr
import com.ligadata.transactions.{ NodeLevelTransService, SimpleTransService }
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import com.ligadata.VelocityMetrics._

//import org.json4s._
//import org.json4s.JsonDSL._
//import org.json4s.jackson.JsonMethods._
import org.apache.logging.log4j.{ Logger, LogManager }

//import scala.collection.mutable.ArrayBuffer

object AdapterConfiguration {
  // Strings to be used for the Metrics descriptions
  val TYPE_INPUT = "Input_Adapter"
  val TYPE_OUTPUT = "Output_Adapter"
}

class AdapterConfiguration {
  // Name of the Adapter, KafkaQueue Name/MQ Name/File Adapter Logical Name/etc
  var Name: String = _
  // Class where the Adapter can be loaded (Object derived from InputAdapterObj)
  var className: String = _
  // Jar where the className can be found
  var jarName: String = _
  // All dependency Jars for jarName
  var dependencyJars: Set[String] = _
  // adapter specific (mostly json) string
  var adapterSpecificCfg: String = _
  var fullAdapterConfig: String = _
  var tenantId: String = _
}

// Input Adapter Object to create Adapter
trait InputAdapterFactory {
  def CreateInputAdapter(inputConfig: AdapterConfiguration, execCtxtObj: ExecContextFactory, nodeContext: NodeContext): InputAdapter
}

class StartProcPartInfo {
  var _key: PartitionUniqueRecordKey = null
  var _val: PartitionUniqueRecordValue = null
  var _validateInfoVal: PartitionUniqueRecordValue = null
}

// Input Adapter
trait InputAdapter extends AdaptersSerializeDeserializers with Monitorable {
  val nodeContext: NodeContext
  // NodeContext
  val inputConfig: AdapterConfiguration // Configuration
  var vm: VelocityMetricsInfo = new VelocityMetricsInfo

  def UniqueName: String = {
    // Making String from key
    return "{\"Name\" : \"%s\"}".format(inputConfig.Name)
  }

  override final def getAdapterName = inputConfig.Name

  def Category = "Input"

  def Shutdown: Unit

  def StopProcessing: Unit

  def StartProcessing(partitionInfo: Array[StartProcPartInfo], ignoreFirstMsg: Boolean): Unit

  // each value in partitionInfo is (PartitionUniqueRecordKey, PartitionUniqueRecordValue, Long, PartitionUniqueRecordValue). // key, processed value, Start transactionid, Ignore Output Till given Value (Which is written into Output Adapter) & processing Transformed messages (processing & total)
  def GetAllPartitionUniqueRecordKey: Array[PartitionUniqueRecordKey]

  def DeserializeKey(k: String): PartitionUniqueRecordKey

  def DeserializeValue(v: String): PartitionUniqueRecordValue

  def getAllPartitionBeginValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]

  def getAllPartitionEndValues: Array[(PartitionUniqueRecordKey, PartitionUniqueRecordValue)]

  def externalizeExceptionEvent(cause: Throwable): Unit = {
    try {
      val exceptionEvent = nodeContext.getEnvCtxt.getContainerInstance("com.ligadata.KamanjaBase.KamanjaExceptionEvent").asInstanceOf[com.ligadata.KamanjaBase.KamanjaExceptionEvent]
      exceptionEvent.timeoferrorepochms = System.currentTimeMillis()
      exceptionEvent.componentname = inputConfig.Name
      exceptionEvent.errortype = "exception"
      exceptionEvent.errorstring = StackTrace.ThrowableTraceString(cause)
      nodeContext.getEnvCtxt.postMessages(Array[ContainerInterface](exceptionEvent))
    } catch {
      case t: Throwable => { return }
    }
  }

  def VMFactory = vm.getVMFactory(nodeContext)

}

// Output Adapter Object to create Adapter
trait OutputAdapterFactory {
  def CreateOutputAdapter(inputConfig: AdapterConfiguration, nodeContext: NodeContext): OutputAdapter
}

// Output Adapter
trait OutputAdapter extends AdaptersSerializeDeserializers with Monitorable {
  // NodeContext
  val nodeContext: NodeContext

  var vm: VelocityMetricsInfo = new VelocityMetricsInfo
  // Configuration
  val inputConfig: AdapterConfiguration

  override final def getAdapterName = inputConfig.Name

  def send(message: Array[Array[Byte]], partitionKey: Array[Array[Byte]]): Unit

  def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): Unit

  def Shutdown: Unit

  def Category = "Output"

  def externalizeExceptionEvent(cause: Throwable): Unit = {
    try {
      val exceptionEvent = nodeContext.getEnvCtxt.getContainerInstance("com.ligadata.KamanjaBase.KamanjaExceptionEvent").asInstanceOf[com.ligadata.KamanjaBase.KamanjaExceptionEvent]
      exceptionEvent.timeoferrorepochms = System.currentTimeMillis()
      exceptionEvent.componentname = inputConfig.Name
      exceptionEvent.errortype = "exception"
      exceptionEvent.errorstring = StackTrace.ThrowableTraceString(cause)
      nodeContext.getEnvCtxt.postMessages(Array[ContainerInterface](exceptionEvent))
    } catch {
      case t: Throwable => {
        return
      }
    }
  }

  def VMFactory = vm.getVMFactory(nodeContext)

}

trait ExecContext {
  val input: InputAdapter
  val curPartitionKey: PartitionUniqueRecordKey
  val nodeContext: NodeContext

  private val LOG = LogManager.getLogger(getClass);
  private val failedEventDtFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  if (nodeContext != null && nodeContext.getEnvCtxt() != null) {
    if (!nodeContext.getEnvCtxt().hasZkConnectionString)
      throw new KamanjaException("Zookeeper information is not yet set", null)
  } else {
    throw new KamanjaException("Not found NodeContext or EnvContext", null)
  }

  val excludedMsgs = scala.collection.mutable.Set[String]()

  final def LoadExcludedMessages: Unit = {
    // For now ignoring all standard messages
    excludedMsgs += "com.ligadata.kamanjabase.kamanjastatusevent"
    excludedMsgs += "com.ligadata.kamanjabase.kamanjastatisticsevent"
    excludedMsgs += "com.ligadata.kamanjabase.kamanjaexceptionevent"
    excludedMsgs += "com.ligadata.kamanjabase.kamanjaexecutionfailureevent"
    excludedMsgs += "com.ligadata.kamanjabase.kamanjamessageevent"
  }

  LoadExcludedMessages

  val (zkConnectString, zkNodeBasePath, zkSessionTimeoutMs, zkConnectionTimeoutMs) = nodeContext.getEnvCtxt().getZookeeperInfo
  val (txnIdsRangeForPartition, txnIdsRangeForNode) = nodeContext.getEnvCtxt().getTransactionRanges

  NodeLevelTransService.init(zkConnectString, zkSessionTimeoutMs, zkConnectionTimeoutMs, zkNodeBasePath, txnIdsRangeForNode,
    nodeContext.getEnvCtxt().getSystemCatalogDatastore(), nodeContext.getEnvCtxt().getJarPaths())
  private val transService = new SimpleTransService
  transService.init(txnIdsRangeForPartition)

  final def SendFailedEvent(data: Array[Byte], deserializer: String, failedMsg: String, uniqueKey: PartitionUniqueRecordKey, uniqueVal: PartitionUniqueRecordValue, e: Throwable, omsg: ContainerInterface): Unit = {
    if (nodeContext != null && nodeContext.getEnvCtxt() != null) {
      var uk = ""
      var uv = ""

      try {
        uk = if (uniqueKey != null) uniqueKey.Serialize else ""
        uv = if (uniqueVal != null) uniqueVal.Serialize else ""
      } catch {
        case e: Throwable => {
          LOG.error("Failed to serialize PartitionUniqueRecordKey and/or PartitionUniqueRecordValue", e)
        }
      }

      try {
        val msg = nodeContext.getEnvCtxt().getContainerInstance("com.ligadata.KamanjaBase.KamanjaExecutionFailureEvent").asInstanceOf[com.ligadata.KamanjaBase.KamanjaExecutionFailureEvent]
        if (msg != null) {
          val failedTm: String = failedEventDtFormat.format(new java.util.Date(System.currentTimeMillis))
          val evntData = new String(data)

          val failMsg = if (e != null) e.getMessage else ""
          val stackTrace = if (e != null) StackTrace.ThrowableTraceString(e) else ""

          try {
            if (omsg != null)
              msg.msgid = MdMgr.GetMdMgr.ElementIdForSchemaId(omsg.asInstanceOf[ContainerInterface].getSchemaId)
            else
              msg.msgid = -1

            msg.timeoferrorepochms = System.currentTimeMillis
            msg.msgcontent = evntData
            msg.msgadapterkey = uk
            msg.msgadaptervalue = uv
            msg.sourceadapter = input.inputConfig.Name
            msg.deserializer = deserializer
            msg.errordetail = stackTrace

            nodeContext.getEnvCtxt().postMessages(Array(msg))
          } catch {
            case e: Throwable => {
              LOG.error("Failed to post message of type:%s, UK:%s, UV:%s".format(omsg.Name, uk, uv), e)
            }
          }
        } else {
          LOG.error(s"Failed to get message com.ligadata.KamanjaBase.KamanjaExecutionFailureEvent from EnvContext. UK:${uk}, UV:${uv}")
        }
      } catch {
        case e: Throwable => {
          LOG.error("Failed to create message for type:" + omsg.Name, e)
        }
      }
    } else {
      LOG.error("EnvContext not found to send failed event")
    }
  }

  final def execute(msg: ContainerInterface, data: Array[Byte], uniqueKey: PartitionUniqueRecordKey, uniqueVal: PartitionUniqueRecordValue, readTmMilliSecs: Long): Unit = {
    if (msg == null) {
      SendFailedEvent(data, "", "", uniqueKey, uniqueVal, null, msg)
      return
    }

    var uk = ""
    var uv = ""

    try {
      uk = if (uniqueKey != null) uniqueKey.Serialize else ""
      uv = if (uniqueVal != null) uniqueVal.Serialize else ""
    } catch {
      case e: Throwable => {
        LOG.error("Failed to serialize PartitionUniqueRecordKey and/or PartitionUniqueRecordValue", e)
      }
    }

    var txnCtxt: TransactionContext = null
    try {
      val transId = transService.getNextTransId
      if (LOG.isTraceEnabled) {
        LOG.trace("CurrentMsg:%s, KamanjaMessageEvent excluded Messages:%s".format(msg.getFullTypeName.toLowerCase(), excludedMsgs.mkString(",")))
      }
      val msgEvent: KamanjaMessageEvent =
        if (excludedMsgs.contains(msg.getFullTypeName.toLowerCase())) {
          val tmpMsgEvent: KamanjaMessageEvent = null
          tmpMsgEvent
        } else {
          val tmpMsgEvent: KamanjaMessageEvent = nodeContext.getEnvCtxt().getContainerInstance("com.ligadata.KamanjaBase.KamanjaMessageEvent").asInstanceOf[KamanjaMessageEvent]
          if (tmpMsgEvent == null) {
            LOG.warn("Not able to get com.ligadata.KamanjaBase.KamanjaMessageEvent")
          }
          tmpMsgEvent
        }

      if (msgEvent != null) {
        msgEvent.messagekey = uk
        msgEvent.messagevalue = uv
        msgEvent.error = ""
      }
      txnCtxt = new TransactionContext(transId, nodeContext, data, EventOriginInfo(uk, uv), readTmMilliSecs, msgEvent)
      txnCtxt.setInitialMessage("", msg)
      ThreadLocalStorage.txnContextInfo.set(txnCtxt)
      executeMessage(txnCtxt)
    } catch {
      case e: Throwable => {
        LOG.error("Failed to execute message : " + msg.getFullTypeName, e)
      }
    } finally {
      try {
        commitData(txnCtxt);
      } catch {
        case e: Throwable => {
          LOG.error("Failed to commit data for executed message : " + msg.getFullTypeName, e)
        }
      } finally {
        ThreadLocalStorage.txnContextInfo.remove
      }
    }
  }

  // Raw data deserialized and send to another send method which takes msg
  final def execute(data: Array[Byte], uniqueKey: PartitionUniqueRecordKey, uniqueVal: PartitionUniqueRecordValue, readTmMilliSecs: Long): Unit = {
    val deserializer = ""
    val failedMsg = ""
    var messageName = ""
    var tempMsgInterface: ContainerInterface = null

    var uk = ""
    var uv = ""

    try {
      uk = if (uniqueKey != null) uniqueKey.Serialize else ""
      uv = if (uniqueVal != null) uniqueVal.Serialize else ""
    } catch {
      case e: Throwable => {
        LOG.error("Failed to serialize PartitionUniqueRecordKey and/or PartitionUniqueRecordValue", e)
      }
    }

    try {
      val (tMsg, tDeserializerName, msgName) = input.deserialize(data)
      tempMsgInterface = tMsg
      messageName = if (msgName != null) msgName else ""
      LOG.debug("Called Deserialize and got msg:" + (if (tMsg == null) "" else tMsg.getFullTypeName))
      val deserializer = if (tDeserializerName != null) tDeserializerName else ""
      val failedMsg = if (msgName != null) msgName else ""
      if (tMsg != null) {
        execute(tMsg, data, uniqueKey, uniqueVal, readTmMilliSecs)
        val nodeId = input.nodeContext.getEnvCtxt().getNodeId()
        getIAVelocityMetrics(input.VMFactory, nodeId, tMsg, input.inputConfig, true)
      } else {
        if (LOG.isDebugEnabled) {
          LOG.debug("Not able to deserialize data:%s at UK:%s, UV:%s".format((if (data != null) new String(data) else ""), uk, uv))
        }
        val ex = new Exception("Unable to deserialize messageName:%s from data:%s".format(messageName, (if (data != null) new String(data) else "")))
        SendFailedEvent(data, tDeserializerName, failedMsg, uniqueKey, uniqueVal, ex, tempMsgInterface)
        val nodeId = input.nodeContext.getEnvCtxt().getNodeId()
        getIAVelocityMetrics(input.VMFactory, nodeId, tMsg, input.inputConfig, false)
      }
    } catch {
      case e: Throwable => {
        LOG.error("Failed to Deserialize/Execute. MessageName:%s at UK:%s, UV:%s".format(messageName, uk, uv), e)
        SendFailedEvent(data, deserializer, failedMsg, uniqueKey, uniqueVal, e, tempMsgInterface)
      }
    }
  }

  private def getIAVelocityMetrics(VMFactory: VelocityMetricsFactoryInterface, nodeId: String, message: ContainerInterface, adapConfig: AdapterConfiguration, processed: Boolean) = {
    var vm = new VelocityMetricsInfo
    val IACompName = "InputAdapter"
    vm.incrementVelocityMetrics(VMFactory, IACompName, nodeId, message, adapConfig, true)

  }

  protected def executeMessage(txnCtxt: TransactionContext): Unit

  protected def commitData(txnCtxt: TransactionContext): Unit

}

trait ExecContextFactory {
  def CreateExecContext(input: InputAdapter, curPartitionKey: PartitionUniqueRecordKey, nodeContext: NodeContext): ExecContext
}

trait PartitionUniqueRecordKey {
  val Type: String

  // Type of the Key -- For now putting File/Kafka like that. This is mostly for readable purpose (for which adapter etc)
  def Serialize: String

  // Making String from key
  def Deserialize(key: String): Unit // Making Key from Serialized String
}

trait PartitionUniqueRecordValue {
  def Serialize: String

  // Making String from Value
  def Deserialize(key: String): Unit // Making Value from Serialized String
}


