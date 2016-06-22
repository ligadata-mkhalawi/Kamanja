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

import akka.actor.{Actor, ActorRef}
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

object GetAdapterMessageBindigsService {
  case class Process(objectType: String, objectKey: String)
}

class GetAdapterMessageBindigsService(requestContext: RequestContext, userid:Option[String], password:Option[String], cert:Option[String]) extends Actor {

  import GetAdapterMessageBindigsService._
  implicit val system = context.system
  val log = Logging(system, getClass)

  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)
  // logger.setLevel(Level.TRACE);

  val APIName = "GetAdapterMessageBindigsService"

  def GetAdapterMessageBindingObjects(objectType:String, objectKey: String): String = {
    if (objectType.equalsIgnoreCase("adapter")) {
      val bindings = AdapterMessageBindingUtils.ListBindingsForAdapter(objectKey)
      return new ApiResult (ErrorCodeConstants.Success, "ListAllAdapterMessageBindings", constructAdaperMsgBindingsResults(bindings), "").toString
    } else if (objectType.equalsIgnoreCase("message")) {
      val bindings = AdapterMessageBindingUtils.ListBindingsForMessage(objectKey)
      return new ApiResult (ErrorCodeConstants.Success, "ListAllAdapterMessageBindings", constructAdaperMsgBindingsResults(bindings), "").toString
    } else if (objectType.equalsIgnoreCase("serializer")) {
      val bindings = AdapterMessageBindingUtils.ListBindingsUsingSerializer(objectKey)
      return new ApiResult (ErrorCodeConstants.Success, "ListAllAdapterMessageBindings", constructAdaperMsgBindingsResults(bindings), "").toString
    } else {
      val myResults = Array[String]("The " + objectType + " is not supported yet ")
      return new ApiResult(ErrorCodeConstants.Failure, APIName, null,  "Invalid URL:" + myResults.mkString).toString
    }
  }


  def receive = {
    case Process(objectType, objectKey) =>
      process(objectType, objectKey)
      context.stop(self)
  }

  def process(objectType:String, objectKey: String) = {
    log.debug("Requesting GetAdapterBindingsObjects {}",objectType)

    if (!MetadataAPIImpl.checkAuth(userid,password,cert, MetadataAPIImpl.getPrivilegeName("get","adaptermessagebinding"))) {
      MetadataAPIImpl.logAuditRec(userid,Some(AuditConstants.READ),AuditConstants.GETCONFIG,AuditConstants.CONFIG,AuditConstants.FAIL,"",objectType)
      requestContext.complete(new ApiResult(ErrorCodeConstants.Failure,APIName, null, "Error: READ not allowed for this user").toString )
    } else {
      val apiResult = GetAdapterMessageBindingObjects(objectType, objectKey)
      requestContext.complete(apiResult)
    }
  }

  private def constructAdaperMsgBindingsResults(bindings: scala.collection.immutable.Map[String,AdapterMessageBinding]): String = {
    if (bindings == null || bindings.size == 0) {return ""}
    val result = bindings.values.map(binding => {
      binding.FullBindingName
    }).toArray.mkString(":")
    return result
  }


}