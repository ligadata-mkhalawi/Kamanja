package com.ligadata.MetadataAPI

import com.ligadata.AuditAdapterInfo.AuditConstants
import com.ligadata.kamanja.metadata.MdMgr
import org.apache.logging.log4j.LogManager

/**
  * Created by Saleh on 8/25/2016.
  */
object ScheduleUtils {

  lazy val serializerType = "json4s"
  lazy val sysNS = "System"
  // system name space
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI

  def addSchedule(text: String, format: String, userid: Option[String], tenantId: Option[String] = None, pStr: Option[String]): String = {
    val key = "ClusterInfo.testSchedule"
    try {
      val value = MetadataAPISerialization.serializeObjectToJson(text).getBytes
      getMetadataAPI.SaveObject(key.toLowerCase, value, "schedules", serializerType)

      val apiResult = new ApiResult(ErrorCodeConstants.Success, "AddSchedule", null, ErrorCodeConstants.Add_Schedule_Successful + ": " + key)

      apiResult.toString()
    } catch {
      case e: Exception => {
        logger.error("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "AddSchedule", null, "Error :" + e.toString() + ErrorCodeConstants.Add_Schedule_Failed + ": " + key)
        apiResult.toString()
      }
    }
  }
}
