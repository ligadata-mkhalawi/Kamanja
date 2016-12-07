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

package com.ligadata.metadataapiservice

import akka.actor.{ Actor, ActorRef }
import akka.event.Logging
import akka.io.IO
import spray.routing.RequestContext
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import scala.util.{ Success, Failure }
import com.ligadata.MetadataAPI._
import com.ligadata.kamanja.metadata._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.util.control._
import org.apache.logging.log4j._
import com.ligadata.AuditAdapterInfo.AuditConstants

object GetTypeByIdService {
  case class Process(formatType: String, objectKey: String)
}

class GetTypeByIdService(requestContext: RequestContext, userid: Option[String], password: Option[String], cert: Option[String]) extends Actor {

  import GetTypeByIdService._

  implicit val system = context.system
  import system.dispatcher
  val log = Logging(system, getClass)

  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)
  // logger.setLevel(Level.TRACE);
  // 646 - 676 Change begins - replace MetadataAPIImpl with MetadataAPI
  val getMetadataAPI = MetadataAPI.getMetadataApiInterface()
  // 646 - 676 Change ends

  val APIName = "GetTypeById"

  def GetTypeById(objectType: String, objectKey: String): String = {
    var apiResult: String = ""

    try {
      objectType match {
        case "typebyschemaid" => {
          if (!scala.util.Try(objectKey.toInt).isSuccess) {
            val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetTypeBySchemaId", null, "Please provide proper schema id :" + objectKey)
            return apiResult.toString()
          }
          apiResult = getMetadataAPI.GetTypeBySchemaId(objectKey.toInt, userid)
        }
        case "typebyelementid" => {
          if (!scala.util.Try(objectKey.toLong).isSuccess) {
            val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetTypeByElementId", null, "Please provide proper element id :" + objectKey)
            return apiResult.toString()
          }
          apiResult = getMetadataAPI.GetTypeByElementId(objectKey.toLong, userid)
        }
      }
    } catch {
      case nfe: NumberFormatException => {
        logger.debug("", nfe)
        val apiResult = new ApiResult(ErrorCodeConstants.Failure, "GetTypeById", null, "Error :" + nfe.toString() + "Please provide proper id :" + objectKey)
        apiResult.toString()
      }
    }
    apiResult
  }

  def receive = {
    case Process(objectType, objectKey) =>
      process(objectType, objectKey)
      context.stop(self)
  }

  def process(objectType: String, objectKey: String) = {
    log.debug("Requesting GetTypeByIdObjects {}", objectType)

    if (!getMetadataAPI.checkAuth(userid, password, cert, getMetadataAPI.getPrivilegeName("get", "config"))) {
      getMetadataAPI.logAuditRec(userid, Some(AuditConstants.READ), AuditConstants.GETCONFIG, AuditConstants.CONFIG, AuditConstants.FAIL, "", objectType)
      requestContext.complete(new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Error: READ not allowed for this user").toString)
    } else {
      if (objectKey == null || objectKey.trim() == "") requestContext.complete(new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Error: Please provide the proper id").toString)

      val apiResult = GetTypeById(objectType, objectKey)
      requestContext.complete(apiResult)
    }
  }
}
