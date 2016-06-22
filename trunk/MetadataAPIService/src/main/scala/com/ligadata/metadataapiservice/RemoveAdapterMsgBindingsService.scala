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

object RemoveAdapterMsgBindingsService {
  case class Process(objectKey: String)
}

class RemoveAdapterMsgBindingsService(requestContext: RequestContext, userid:Option[String], password:Option[String], cert:Option[String]) extends Actor {

  import RemoveAdapterMsgBindingsService._
  implicit val system = context.system
  val log = Logging(system, getClass)

  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)
  // logger.setLevel(Level.TRACE);
  // 646 - 676 Change begins - replace MetadataAPIImpl with MetadataAPI
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Change ends

  val APIName = "GetAdapterMessageBindigsService"

  def removeAdapterMessageBindingObjects(objectKey: String): String = {
    return AdapterMessageBindingUtils.RemoveAdapterMessageBinding(objectKey, userid)
  }


  def receive = {
    case Process(objectKey) =>
      process(objectKey)
      context.stop(self)
  }

  def process(objectKey: String) = {
    log.debug("Removing AdapterBindingsObjects {}", objectKey)

    if (!getMetadataAPI.checkAuth(userid,password,cert, getMetadataAPI.getPrivilegeName("delete","adaptermessagebinding"))) {
      getMetadataAPI.logAuditRec(userid,Some(AuditConstants.READ),AuditConstants.GETCONFIG,AuditConstants.CONFIG,AuditConstants.FAIL,"","AdapterMessageBindings")
      requestContext.complete(new ApiResult(ErrorCodeConstants.Failure,APIName, null, "Error: READ not allowed for this user").toString )
    } else {
      val apiResult = removeAdapterMessageBindingObjects(objectKey)
      requestContext.complete(apiResult)
    }
  }

}
