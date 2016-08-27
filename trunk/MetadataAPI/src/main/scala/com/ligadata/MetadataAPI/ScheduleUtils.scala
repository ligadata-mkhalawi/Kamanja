package com.ligadata.MetadataAPI

import com.ligadata.AuditAdapterInfo.AuditConstants
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
    try {
      //      getMetadataAPI.AddObjectToCache(msgDef, MdMgr.GetMdMgr)
      //      getMetadataAPI.UploadJarsToDB(msgDef)
      //      var objectsAdded = AddMessageTypes(msgDef, MdMgr.GetMdMgr, recompile)
      //      objectsAdded = msgDef +: objectsAdded
      PersistenceUtils.SaveSchemaInformation(1, "test", "test", 1, "test", "test", "Schedule")
      PersistenceUtils.SaveElementInformation(1, "Schedule", "test", "test")
      val (objtype, jsonBytes) : (String, Any) = PersistenceUtils.GetObject("test.test.1", "schedules")
      //      getMetadataAPI.SaveObjectList(objectsAdded, "schedule")
      //      val operations = for (op <- objectsAdded) yield "Add"
      //      getMetadataAPI.NotifyEngine(objectsAdded, operations)
      val apiResult = new ApiResult(ErrorCodeConstants.Success, "AddSchedule", null, ErrorCodeConstants.Add_Message_Successful + ": test")

      apiResult.toString()
    } catch {
      case e: Exception => {
        logger.error("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "AddSchedule", null, "Error :" + e.toString() + ErrorCodeConstants.Add_Message_Failed + ":test")
        apiResult.toString()
      }
    }
  }
}
