
package com.ligadata.KamanjaManager

import com.ligadata.KamanjaBase._
import com.ligadata.Utils.Utils
import java.util.Map
import com.ligadata.outputmsg.OutputMsgGenerator
import org.apache.log4j.Logger
import java.io.{ PrintWriter, File }
import scala.xml.XML
import scala.xml.Elem
import scala.collection.mutable.ArrayBuffer
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import com.ligadata.outputmsg.OutputMsgGenerator
import com.ligadata.InputOutputAdapterInfo.{ ExecContext, InputAdapter, PartitionUniqueRecordKey, PartitionUniqueRecordValue }
import com.ligadata.Exceptions.StackTrace

class LearningEngine(val input: InputAdapter, val curPartitionKey: PartitionUniqueRecordKey) {
  val LOG = Logger.getLogger(getClass);

  private def RunAllModels(transId: Long, finalTopMsgOrContainer: MessageContainerBase, envContext: EnvContext, uk: String, uv: String, xformedMsgCntr: Int, totalXformedMsgs: Int): Array[SavedMdlResult] = {
    var results: ArrayBuffer[SavedMdlResult] = new ArrayBuffer[SavedMdlResult]()
    LOG.debug("Processing uniqueKey:%s, uniqueVal:%s".format(uk, uv))

    if (finalTopMsgOrContainer != null) {

      val models: Array[MdlInfo] = KamanjaMetadata.getAllModels.map(mdl => mdl._2).toArray

      val outputAlways: Boolean = false;

      // Execute all modes here
      models.foreach(md => {
        try {
          if (md.mdl.IsValidMessage(finalTopMsgOrContainer)) {
            LOG.debug("Processing uniqueKey:%s, uniqueVal:%s, model:%s".format(uk, uv, md.mdl.ModelName))
            // Checking whether this message has any fields/concepts to execute in this model
            val mdlCtxt = new ModelContext(new TransactionContext(transId, envContext, md.tenantId), finalTopMsgOrContainer)
            ThreadLocalStorage.modelContextInfo.set(mdlCtxt)
            val curMd = md.mdl.CreateNewModel(mdlCtxt)
            if (curMd != null) {
              val res = curMd.execute(outputAlways)
              if (res != null) {
                results += new SavedMdlResult().withMdlName(md.mdl.ModelName).withMdlVersion(md.mdl.Version).withUniqKey(uk).withUniqVal(uv).withTxnId(transId).withXformedMsgCntr(xformedMsgCntr).withTotalXformedMsgs(totalXformedMsgs).withMdlResult(res)
              } else {
                // Nothing to output
              }
            } else {
              LOG.error("Failed to create model " + md.mdl.ModelName())
            }
          } else {
          }
        } catch {
          case e: Exception => {
            LOG.error("Model Failed => " + md.mdl.ModelName() + ". Reason: " + e.getCause + ". Message: " + e.getMessage)
          }
          case t: Throwable => {
            LOG.error("Model Failed => " + md.mdl.ModelName() + ". Reason: " + t.getCause + ". Message: " + t.getMessage)
          }
        } finally {
          ThreadLocalStorage.modelContextInfo.remove
        }
      })
    }
    return results.toArray
  }

  private def GetTopMsgName(msgName: String): (String, Boolean, MsgContainerObjAndTransformInfo) = {
    val topMsgInfo = KamanjaMetadata.getMessgeInfo(msgName)
    if (topMsgInfo == null || topMsgInfo.parents.size == 0) return (msgName, false, null)
    (topMsgInfo.parents(0)._1, true, topMsgInfo)
  }

  def execute(transId: Long, msgType: String, msgInfo: MsgContainerObjAndTransformInfo, inputdata: InputData, envContext: EnvContext, readTmNs: Long, rdTmMs: Long, uk: String, uv: String, xformedMsgCntr: Int, totalXformedMsgs: Int, ignoreOutput: Boolean): Array[(String, String)] = {
    // LOG.debug("LE => " + msgData)
    LOG.debug("Processing uniqueKey:%s, uniqueVal:%s".format(uk, uv))
    val returnOutput = ArrayBuffer[(String, String)]() // Adapter/Queue name & output message 

    try {
      if (msgInfo != null && inputdata != null) {
        val partKeyData = if (msgInfo.contmsgobj.asInstanceOf[BaseMsgObj].CanPersist) msgInfo.contmsgobj.asInstanceOf[BaseMsgObj].PartitionKeyData(inputdata) else null
        val isValidPartitionKey = (partKeyData != null && partKeyData.size > 0)
        val partKeyDataList = if (isValidPartitionKey) partKeyData.toList else null
        val primaryKey = if (isValidPartitionKey) msgInfo.contmsgobj.asInstanceOf[BaseMsgObj].PrimaryKeyData(inputdata) else null
        val primaryKeyList = if (primaryKey != null && primaryKey.size > 0) primaryKey.toList else null

        var msg: BaseMsg = null
        if (isValidPartitionKey && primaryKeyList != null) {
          val fndmsg = envContext.getObject(transId, msgType, partKeyDataList, primaryKeyList)
          if (fndmsg != null) {
            msg = fndmsg.asInstanceOf[BaseMsg]
          }
        }
        var createdNewMsg = false
        if (msg == null) {
          createdNewMsg = true
          msg = msgInfo.contmsgobj.asInstanceOf[BaseMsgObj].CreateNewMessage
        }
        msg.populate(inputdata)
        var allMdlsResults: scala.collection.mutable.Map[String, SavedMdlResult] = null
        if (isValidPartitionKey) {
          envContext.setObject(transId, msgType, partKeyDataList, msg) // Whether it is newmsg or oldmsg, we are still doing createdNewMsg
          allMdlsResults = envContext.getModelsResult(transId, partKeyDataList)
        }
        if (allMdlsResults == null)
          allMdlsResults = scala.collection.mutable.Map[String, SavedMdlResult]()
        // Run all models
        val mdlsStartTime = System.nanoTime
        val results = RunAllModels(transId, msg, envContext, uk, uv, xformedMsgCntr, totalXformedMsgs)
        LOG.debug(ManagerUtils.getComponentElapsedTimeStr("Models", uv, readTmNs, mdlsStartTime))

        if (results.size > 0) {
          var elapseTmFromRead = (System.nanoTime - readTmNs) / 1000

          if (elapseTmFromRead < 0)
            elapseTmFromRead = 1

          try {
            // Prepare final output and update the models persistance map
            results.foreach(res => {
              allMdlsResults(res.mdlName) = res
            })
          } catch {
            case e: Exception => {
              LOG.error("Failed to get Model results. Reason:%s Message:%s".format(e.getCause, e.getMessage))

            }
          }
          val resMap = scala.collection.mutable.Map[String, Array[(String, Any)]]()

          results.map(res => {
            resMap(res.mdlName) = res.mdlRes.asKeyValuesMap.map(r => { (r._1, r._2) }).toArray
          })

          var resStr = "Not found any output."
          val outputMsgs = KamanjaMetadata.getMdMgr.OutputMessages(true, true)
          if (outputMsgs != None && outputMsgs != null && outputMsgs.get.size > 0) {
            LOG.info("msg " + msg.FullName)
            LOG.info(" outputMsgs.size" + outputMsgs.get.size)
            var outputGen = new OutputMsgGenerator()
            val resultedoutput = outputGen.generateOutputMsg(msg, resMap, outputMsgs.get.toArray)
            resStr = resultedoutput.map(resout => resout._3).mkString("\n")
          } else {
            val json = ("ModelsResult" -> results.toList.map(res => res.toJson))
            resStr = compact(render(json))
          }
          returnOutput += (("", resStr))

          if (isValidPartitionKey) {
            envContext.saveModelsResult(transId, partKeyDataList, allMdlsResults)
          }
        }
      } else {
        LOG.error("Recieved null message object for input:" + inputdata.dataInput)
      }
      return returnOutput.toArray
    } catch {
      case e: Exception => {
        LOG.error("Failed to create and run message. Reason:%s Message:%s".format(e.getCause, e.getMessage))

      }
    }
    return Array[(String, String)]()
  }
}
