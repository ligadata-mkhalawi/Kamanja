package com.ligadata.MetadataAPI

import com.ligadata.AuditAdapterInfo.AuditConstants
import com.ligadata.Serialize.JsonSerializer
import com.ligadata.kamanja.metadata.MdMgr
import org.apache.logging.log4j.LogManager
import com.ligadata.kamanja.metadata._

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
    var key = None:Option[String]

    try {
      val sch =  JsonSerializer.parseScheduleDef(text)
      key = Some("%s.%s.%d".format(sch.nameSpace.trim.toLowerCase,sch.name.trim.toLowerCase,sch.Version))

      val value = MetadataAPISerialization.serializeObjectToJson(sch).getBytes
      getMetadataAPI.SaveObject(key.get.toLowerCase, value, "schedules", serializerType)
      val (objtype, jsonBytes) : (String, Any) = PersistenceUtils.GetObject(key.get.toLowerCase, "schedules")
      println(">>>>>>>>> " + MdMgr.GetMdMgr.AddSchedule(sch))
      println(">>>>>>>>> "+new String(jsonBytes.asInstanceOf[Array[Byte]]))
      val ss = MetadataAPISerialization.deserializeMetadata(new String(jsonBytes.asInstanceOf[Array[Byte]])).asInstanceOf[ScheduleDef]
      println(">>>>>>>>> " + ss.name)
      println(">>>>>>>>> " + MdMgr.GetMdMgr.GetSchedule("test.test.0").name)

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
