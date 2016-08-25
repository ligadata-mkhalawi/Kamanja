package com.ligadata.MetadataAPI

import com.ligadata.kamanja.metadata.MdMgr
import org.apache.logging.log4j.LogManager

/**
  * Created by Saleh on 8/25/2016.
  */
object ScheduleUtils {

  lazy val sysNS = "System"
  // system name space
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI

  def addSchedule(text: String, format: String, userid: Option[String], tenantId: Option[String] = None, pStr: Option[String]): String = {
//    val dispkey = msgDef.FullName + "." + MdMgr.Pad0s2Version(msgDef.Version)
//    try {
//      PersistenceUtils.SaveSchemaInformation(msgDef.cType.SchemaId, msgDef.NameSpace, msgDef.Name, msgDef.Version, msgDef.PhysicalName, msgDef.cType.AvroSchema, "Message")
//      PersistenceUtils.SaveElementInformation(msgDef.MdElementId, "Message", msgDef.NameSpace, msgDef.Name)
//      val operations = for (op <- objectsAdded) yield "Add"
//      getMetadataAPI.NotifyEngine(objectsAdded, operations)
//      val apiResult = new ApiResult(ErrorCodeConstants.Success, "AddMessageDef", null, ErrorCodeConstants.Add_Message_Successful + ":" + dispkey)
//      apiResult.toString()
//    } catch {
//      case e: Exception => {
//        logger.error("", e)
//        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "AddMessageDef", null, "Error :" + e.toString() + ErrorCodeConstants.Add_Message_Failed + ":" + dispkey)
//        apiResult.toString()
//      }
//    }
    "test"
  }
}
