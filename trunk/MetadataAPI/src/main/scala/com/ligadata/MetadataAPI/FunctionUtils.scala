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

package com.ligadata.MetadataAPI

import java.util.Properties
import java.io._
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.text.ParseException
import scala.Enumeration
import scala.io._
import scala.collection.mutable.ArrayBuffer

import scala.collection.mutable._
import scala.reflect.runtime.{ universe => ru }

import com.ligadata.kamanja.metadata.ObjType._
import com.ligadata.kamanja.metadata._
import com.ligadata.kamanja.metadata.MdMgr._

import com.ligadata.kamanja.metadataload.MetadataLoad

// import com.ligadata.keyvaluestore._
import com.ligadata.HeartBeat.HeartBeatUtil
import com.ligadata.StorageBase.{ DataStore, Transaction }
import com.ligadata.KvBase.{ Key, TimeRange }

import scala.util.parsing.json.JSON
import scala.util.parsing.json.{ JSONObject, JSONArray }
import scala.collection.immutable.Map
import scala.collection.immutable.HashMap
import scala.collection.mutable.HashMap

import com.google.common.base.Throwables

import com.ligadata.msgcompiler._
import com.ligadata.Exceptions._

import scala.xml.XML
import org.apache.logging.log4j._

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import com.ligadata.ZooKeeper._
import org.apache.curator.framework.CuratorFramework
import org.apache.zookeeper.CreateMode

import com.ligadata.keyvaluestore._
import com.ligadata.Serialize._
import com.ligadata.Utils._
import com.ligadata.AuditAdapterInfo._
import com.ligadata.SecurityAdapterInfo.SecurityAdapter
import com.ligadata.keyvaluestore.KeyValueManager

import java.util.Date
import org.json4s.jackson.Serialization

// The implementation class for metadata object "function"
object FunctionUtils {

  lazy val sysNS = "System"
  // system name space
  lazy val loggerName = this.getClass.getName
  // 646 - 676 Change begins - replace MetadataAPIImpl
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Change ends
lazy val logger = LogManager.getLogger(loggerName)
  //lazy val serializer = SerializerManager.GetSerializer("kryo")

  def AddFunction(functionDef: FunctionDef): String = {
    val key = functionDef.FullNameWithVer
    val dispkey = functionDef.FullName + "." + MdMgr.Pad0s2Version(functionDef.Version)
    try {
      val value = JsonSerializer.SerializeObjectToJson(functionDef)

      logger.debug("key => " + key + ",value =>" + value);
      getMetadataAPI.SaveObject(functionDef, MdMgr.GetMdMgr)
      logger.debug("Added function " + key + " successfully ")
      val apiResult = new ApiResult(ErrorCodeConstants.Success, "AddFunction", null, ErrorCodeConstants.Add_Function_Successful + ":" + dispkey)
      apiResult.toString()
    } catch {
      case e: Exception => {
        logger.debug("", e)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "AddFunction", null, ErrorCodeConstants.Add_Function_Failed + ":" + dispkey)
        apiResult.toString()
      }
    }
  }

  def RemoveFunction(nameSpace: String, functionName: String, version: Long, userid: Option[String]): String = {
    var key = nameSpace + "." + functionName + "." + version
    val dispkey =  nameSpace + "." + functionName + "." + MdMgr.Pad0s2Version(version)
    var newTranId = getMetadataAPI.GetNewTranId
    if (userid != None) getMetadataAPI.logAuditRec(userid,Some(AuditConstants.WRITE),AuditConstants.DELETEOBJECT,AuditConstants.FUNCTION,AuditConstants.SUCCESS,"",nameSpace+"."+key)
    try {
      val o = MdMgr.GetMdMgr.Functions(nameSpace.toLowerCase, functionName.toLowerCase, true, true)
      o match {
        case None =>
          logger.warn("Function not found => " + key)
          var apiResult = new ApiResult(ErrorCodeConstants.Failure, "RemoveFunction", null, ErrorCodeConstants.Remove_Function_Failed_Not_Found + ": " + dispkey)
          return apiResult.toString()
        case Some(m)   =>
          // Found a function, the Functions returns a set, but since we asked for only the latest, there can be only 1 in the returned set.
          // so grab the last one.
          val fDef = m.last.asInstanceOf[FunctionDef]
          logger.debug("function found => " + fDef.FullName + "." + MdMgr.Pad0s2Version(fDef.Version))

          // Mark the transactionId for this transaction and delete object
          fDef.tranId = newTranId
          getMetadataAPI.DeleteObject(fDef)

          // Notify everyone who cares about this change.
          var allObjectsArray =  Array[BaseElemDef](fDef)
          val operations = for (op <- allObjectsArray) yield "Remove"
          getMetadataAPI.NotifyEngine(allObjectsArray, operations)

          // 'Saul Good'man
          var apiResult = new ApiResult(ErrorCodeConstants.Success, "RemoveFunction", null, ErrorCodeConstants.Remove_Function_Successfully + ":" + dispkey)
          return apiResult.toString()
      }
    } catch {
      case e: Exception => {
        logger.debug("", e)
        var apiResult = new ApiResult(ErrorCodeConstants.Failure, "RemoveFunction", null, "Error :" + e.toString() + ErrorCodeConstants.Remove_Function_Failed + ":" + dispkey)
        return apiResult.toString()
      }
    }
  }

  def DumpFunctionDef(funcDef: FunctionDef) {
    logger.debug("Name => " + funcDef.Name)
    for (arg <- funcDef.args) {
      logger.debug("arg_name => " + arg.name)
      logger.debug("arg_type => " + arg.Type.tType)
    }
    logger.debug("Json string => " + JsonSerializer.SerializeObjectToJson(funcDef))
  }

  def IsFunctionAlreadyExists(funcDef: FunctionDef): Boolean = {
    try {
      var key = funcDef.typeString
      val dispkey = key // No version in this string
      val o = MdMgr.GetMdMgr.Function(funcDef.nameSpace,
        funcDef.name,
        funcDef.args.toList.map(a => (a.aType.nameSpace, a.aType.name)),
        funcDef.ver,
        false)
      o match {
        case None =>
          None
          logger.debug("function not in the cache => " + dispkey)
          return false;
        case Some(m) =>
          logger.debug("function found => " + m.asInstanceOf[FunctionDef].typeString)
          return true
      }
    } catch {
      case e: Exception => {
        logger.debug("", e)
        throw UnexpectedMetadataAPIException(e.getMessage(), e)
      }
    }
  }

  def UpdateFunction(functionDef: FunctionDef): String = {
    val key = functionDef.typeString
    val dispkey = key // This does not have version at this moment
    try {
      if (IsFunctionAlreadyExists(functionDef)) {
        functionDef.ver = functionDef.ver + 1
      }
      AddFunction(functionDef)
      var apiResult = new ApiResult(ErrorCodeConstants.Success, "UpdateFunction", null, ErrorCodeConstants.Update_Function_Successful + ":" + dispkey)
      apiResult.toString()
    } catch {
      case e: AlreadyExistsException => {
        logger.error("Failed to update the function, key => " + key, e)
        var apiResult = new ApiResult(ErrorCodeConstants.Failure, "UpdateFunction", null, "Error :" + e.toString() + ErrorCodeConstants.Update_Function_Failed + ":" + dispkey)
        apiResult.toString()
      }
      case e: Exception => {
        logger.error("Failed to up the type, json => " + key, e)
        var apiResult = new ApiResult(ErrorCodeConstants.Failure, "UpdateFunction", null, "Error :" + e.toString() + ErrorCodeConstants.Update_Function_Failed + ":" + dispkey)
        apiResult.toString()
      }
    }
  }

  private def CheckForMissingJar(obj: BaseElemDef): Array[String] = {
    val missingJars = scala.collection.mutable.Set[String]()

    var allJars = getMetadataAPI.GetDependantJars(obj)
    if (allJars.length > 0) {
      val tmpJarPaths = getMetadataAPI.GetMetadataAPIConfig.getProperty("JAR_PATHS")
      val jarPaths = if (tmpJarPaths != null) tmpJarPaths.split(",").toSet else scala.collection.immutable.Set[String]()
      jarPaths.foreach(jardir => {
        val dir = new File(jardir)
        if (!dir.exists()) {
          // attempt to create the missing directory
          dir.mkdir();
        }
      })

      val dirPath = getMetadataAPI.GetMetadataAPIConfig.getProperty("JAR_TARGET_DIR")
      val dir = new File(dirPath)
      if (!dir.exists()) {
        // attempt to create the missing directory
        dir.mkdir();
      }

      allJars.foreach(jar => {
        val jarName = com.ligadata.Utils.Utils.GetValidJarFile(jarPaths, jar)
        val f = new File(jarName)
        if (!f.exists()) {
          try {
            val mObj = getMetadataAPI.GetObject(jar, "jar_store")
            // Nothing to do after getting the object.
          } catch {
            case e: Exception => {
              logger.error("", e)
              missingJars += jar
            }
          }
        }
      })
    }

    missingJars.toArray
  }

  def AddFunctions(functionsText: String, format: String, userid: Option[String]): String = {
    logger.debug("Started AddFunctions => ")
    var aggFailures: String = ""
    val tenantId = "" // For functions we will take empty for now
    try {
      if (format != "JSON") {
        var apiResult = new ApiResult(ErrorCodeConstants.Not_Implemented_Yet, "AddFunctions", functionsText, ErrorCodeConstants.Not_Implemented_Yet_Msg)
        apiResult.toString()
      } else {
        val ownerId: String = if (userid == None) "kamanja" else userid.get
        val uniqueId = getMetadataAPI.GetUniqueId
        val mdElementId = 0L //FIXME:- Not yet handled this
        var funcList= JsonSerializer.parseFunctionList(functionsText, "JSON", ownerId, tenantId, uniqueId, mdElementId)
        // Check for the Jars
        val missingJars = scala.collection.mutable.Set[String]()
        funcList.foreach(func => {
          getMetadataAPI.logAuditRec(userid,Some(AuditConstants.WRITE),AuditConstants.INSERTOBJECT,functionsText,AuditConstants.SUCCESS,"",func.FullNameWithVer)
          if (getMetadataAPI.SaveObject(func, MdMgr.GetMdMgr))
            missingJars ++= CheckForMissingJar(func)
          else {
            if (!aggFailures.equalsIgnoreCase("")) aggFailures = aggFailures + ","
            aggFailures = aggFailures + func.FullNameWithVer
          }
        })

        if (missingJars.size > 0) {
          var apiResult = new ApiResult(ErrorCodeConstants.Failure, "AddFunctions", null, "Error : Not found required jars " + missingJars.mkString(",") + "\n" + ErrorCodeConstants.Add_Function_Failed + ":" + functionsText)
          return apiResult.toString()
        }

        val alreadyCheckedJars = scala.collection.mutable.Set[String]()
        funcList.foreach(func => { getMetadataAPI.UploadJarsToDB(func, false, alreadyCheckedJars) })

        if (funcList.size > 0)
          getMetadataAPI.PutTranId(funcList(0).tranId)
        if (!aggFailures.equalsIgnoreCase("")) {
          (new ApiResult(ErrorCodeConstants.Warning, "AddFunctions", aggFailures, ErrorCodeConstants.Add_Function_Warning)).toString()
        } else {
          (new ApiResult(ErrorCodeConstants.Success, "AddFunctions", functionsText, ErrorCodeConstants.Add_Function_Successful)).toString()
        }
      }
    } catch {
      case e: Exception => {
        logger.error("", e)
        var apiResult = new ApiResult(ErrorCodeConstants.Failure, "AddFunctions", functionsText, "Error :" + e.toString() + ErrorCodeConstants.Add_Function_Failed)
        apiResult.toString()
      }
    }
  }

  def UpdateFunctions(functionsText: String, format: String, userid: Option[String]): String = {
    logger.debug("Started UpdateFunctions => ")
    val tenantId = "" // For functions we will take empty for now
    try {
      if (format != "JSON") {
        var apiResult = new ApiResult(ErrorCodeConstants.Not_Implemented_Yet, "UpdateFunctions", null, ErrorCodeConstants.Not_Implemented_Yet_Msg + ":" + functionsText + ".Format not JSON.")
        apiResult.toString()
      } else {
        val ownerId: String = if (userid == None) "kamanja" else userid.get
        val uniqueId = getMetadataAPI.GetUniqueId
        val mdElementId = 0L //FIXME:- Not yet handled this
        var funcList = JsonSerializer.parseFunctionList(functionsText, "JSON", ownerId, tenantId, uniqueId, mdElementId)
        // Check for the Jars
        val missingJars = scala.collection.mutable.Set[String]()
        funcList.foreach(func => {
          missingJars ++= CheckForMissingJar(func)
        })
        if (missingJars.size > 0) {
          var apiResult = new ApiResult(ErrorCodeConstants.Failure, "UpdateFunctions", null, "Error : Not found required jars " + missingJars.mkString(",") + "\n" + ErrorCodeConstants.Update_Function_Failed + ":" + functionsText)
          return apiResult.toString()
        }
        val alreadyCheckedJars = scala.collection.mutable.Set[String]()
        funcList.foreach(func => {
          getMetadataAPI.logAuditRec(userid, Some(AuditConstants.WRITE), AuditConstants.UPDATEOBJECT, functionsText, AuditConstants.SUCCESS, "", func.FullNameWithVer)
          getMetadataAPI.UploadJarsToDB(func, false, alreadyCheckedJars)
          UpdateFunction(func)
        })
        if (funcList.size > 0)
          getMetadataAPI.PutTranId(funcList(0).tranId)
        var apiResult = new ApiResult(ErrorCodeConstants.Success, "UpdateFunctions", null, ErrorCodeConstants.Update_Function_Successful + ":" + functionsText)
        apiResult.toString()
      }
    } catch {
      case e: MappingException => {
        logger.error("Failed to parse the function, json => " + functionsText, e)
        var apiResult = new ApiResult(ErrorCodeConstants.Failure, "UpdateFunctions", null, "Error :" + e.toString() + ErrorCodeConstants.Update_Function_Failed + ":" + functionsText)
        apiResult.toString()
      }
      case e: AlreadyExistsException => {
        logger.error("Failed to add the function, json => " + functionsText, e)
        var apiResult = new ApiResult(ErrorCodeConstants.Failure, "UpdateFunctions", null, "Error :" + e.toString() + ErrorCodeConstants.Update_Function_Failed + ":" + functionsText)
        apiResult.toString()
      }
      case e: Exception => {
        logger.error("Failed to up the function, json => " + functionsText, e)
        var apiResult = new ApiResult(ErrorCodeConstants.Failure, "UpdateFunctions", null, "Error :" + e.toString() + ErrorCodeConstants.Update_Function_Failed + ":" + functionsText)
        apiResult.toString()
      }
    }
  }

  def LoadFunctionIntoCache(key: String) {
    try {
      val obj = getMetadataAPI.GetObject(key.toLowerCase, "functions")
      val cont: FunctionDef = MetadataAPISerialization.deserializeMetadata(new String(obj._2.asInstanceOf[Array[Byte]])).asInstanceOf[FunctionDef]//serializer.DeserializeObjectFromByteArray(obj._2.asInstanceOf[Array[Byte]]).asInstanceOf[FunctionDef]
      getMetadataAPI.AddObjectToCache(cont.asInstanceOf[FunctionDef], MdMgr.GetMdMgr)
    } catch {
      case e: Exception => {
        logger.debug("", e)
      }
    }
  }

  def GetFunctionDef(nameSpace: String, objectName: String, formatType: String, userid: Option[String]): String = {
    try {
      if (userid != None) getMetadataAPI.logAuditRec(userid, Some(AuditConstants.READ), AuditConstants.GETOBJECT, AuditConstants.FUNCTION, AuditConstants.SUCCESS, "", nameSpace + "." + objectName + "LATEST")
      val funcDefs = MdMgr.GetMdMgr.FunctionsWithName(nameSpace, objectName)
      if (funcDefs == null) {
        logger.debug("No Functions found ")
        var apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetFunctionDef", null, ErrorCodeConstants.Get_Function_Failed + ":" + nameSpace + "." + objectName)
        apiResult.toString()
      } else {
        val fsa = funcDefs.toArray
        var apiResult = new ApiResult(ErrorCodeConstants.Success, "GetFunctionDef", JsonSerializer.SerializeObjectListToJson("Functions", fsa), ErrorCodeConstants.Get_Function_Successful)
        apiResult.toString()
      }
    } catch {
      case e: Exception => {
        logger.debug("", e)
        var apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetFunctionDef", null, "Error :" + e.toString() + ErrorCodeConstants.Get_Function_Failed + ":" + nameSpace + "." + objectName)
        apiResult.toString()
      }
    }
  }

  def GetFunctionDef(nameSpace: String, objectName: String, formatType: String, version: String, userid: Option[String]): String = {
    getMetadataAPI.logAuditRec(userid, Some(AuditConstants.READ), AuditConstants.GETOBJECT, AuditConstants.FUNCTION, AuditConstants.SUCCESS, "", nameSpace + "." + objectName + "." + version)
    GetFunctionDef(nameSpace, objectName, formatType, None)
  }

  // Specific messages (format JSON or XML) as a String using messageName(without version) as the key
  def GetFunctionDef(objectName: String, formatType: String, userid: Option[String]): String = {
    val nameSpace = MdMgr.sysNS
    GetFunctionDef(nameSpace, objectName, formatType, userid)
  }

    /**
     * GetAllFunctionsFromCache
     * @param active
     * @param userid the identity to be used by the security adapter to ascertain if this user has access permissions for this
     *               method. If Security and/or Audit are configured, this value must be a value other than None.
     * @return
     */
  def GetAllFunctionsFromCache(active: Boolean, userid: Option[String] = None): Array[String] = {
    var functionList: Array[String] = new Array[String](0)
    getMetadataAPI.logAuditRec(userid, Some(AuditConstants.READ), AuditConstants.GETKEYS, AuditConstants.FUNCTION, AuditConstants.SUCCESS, "", AuditConstants.FUNCTION)
    try {
      val contDefs = MdMgr.GetMdMgr.Functions(active, true)
      contDefs match {
        case None =>
          None
          logger.debug("No Functions found ")
          functionList
        case Some(ms) =>
          val msa = ms.toArray
          val contCount = msa.length
          functionList = new Array[String](contCount)
          for (i <- 0 to contCount - 1) {
            functionList(i) = msa(i).FullName + "." + MdMgr.Pad0s2Version(msa(i).Version)
          }
          functionList
      }
    } catch {
      case e: Exception => {

        logger.debug("", e)
        throw UnexpectedMetadataAPIException("Failed to fetch all the functions:" + e.toString, e)
      }
    }
  }

    /**
     * GetLatestFunction
     * @param fDef
     * @return
     */
  def GetLatestFunction(fDef: FunctionDef): Option[FunctionDef] = {
    try {
      var key = fDef.nameSpace + "." + fDef.name + "." + fDef.ver
      val dispkey = fDef.nameSpace + "." + fDef.name + "." + MdMgr.Pad0s2Version(fDef.ver)
      val o = MdMgr.GetMdMgr.Messages(fDef.nameSpace.toLowerCase,
        fDef.name.toLowerCase,
        false,
        true)
      o match {
        case None =>
          None
          logger.debug("message not in the cache => " + dispkey)
          None
        case Some(m) =>
          // We can get called from the Add Message path, and M could be empty.
          if (m.size == 0) return None
          logger.debug("message found => " + m.head.asInstanceOf[MessageDef].FullName + "." + MdMgr.Pad0s2Version(m.head.asInstanceOf[MessageDef].ver))
          Some(m.head.asInstanceOf[FunctionDef])
      }
    } catch {
      case e: Exception => {

        logger.debug("", e)
        throw UnexpectedMetadataAPIException(e.getMessage(), e)
      }
    }
  }

  // Answer count and dump of all available functions(format JSON or XML) as a String
  def GetAllFunctionDefs(formatType: String, userid: Option[String]): (Int, String) = {
    try {
      val funcDefs = MdMgr.GetMdMgr.Functions(true, true)
      if (userid != None) getMetadataAPI.logAuditRec(userid, Some(AuditConstants.READ), AuditConstants.GETOBJECT, AuditConstants.FUNCTION, AuditConstants.SUCCESS, "", "ALL")
      funcDefs match {
        case None =>
          None
          logger.debug("No Functions found ")
          var apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetAllFunctionDefs", null, ErrorCodeConstants.Get_All_Functions_Failed_Not_Available)
          (0, apiResult.toString())
        case Some(fs) =>
          val fsa: Array[FunctionDef] = fs.toArray
          var apiResult = new ApiResult(ErrorCodeConstants.Success, "GetAllFunctionDefs", JsonSerializer.SerializeObjectListToJson("Functions", fsa), ErrorCodeConstants.Get_All_Functions_Successful)
          (fsa.size, apiResult.toString())
      }
    } catch {
      case e: Exception => {
        logger.debug("", e)
        var apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetAllFunctionDefs", null, "Error :" + e.toString() + ErrorCodeConstants.Get_All_Functions_Failed)
        (0, apiResult.toString())
      }
    }
  }
}
