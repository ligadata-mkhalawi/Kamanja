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
import com.ligadata.kamanja.metadata._
import spray.routing.RequestContext
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._

import scala.util.{ Success, Failure }

import com.ligadata.MetadataAPI._
import com.ligadata.AuditAdapterInfo.AuditConstants

object UploadJarService {
  case class Process(jarName:String,byteArray:Array[Byte])
}

class UploadJarService(requestContext: RequestContext, userid:Option[String], password:Option[String], cert:Option[String]) extends Actor {

  import UploadJarService._

  implicit val system = context.system
  import system.dispatcher
  val log = Logging(system, getClass)
  val APIName = "UploadJarService"
  // 646 - 676 Change begins - replace MetadataAPIImpl with MetadataAPI
  val getMetadataAPI = MetadataAPI.getMetadataApiInterface()
  // 646 - 676 Change ends

  def receive = {
    case Process(jarName,byteArray) =>
      process(jarName,byteArray)
      context.stop(self)
  }

  def process(jarName:String,byteArray:Array[Byte]) = {

    log.debug("Requesting UploadJar {}",jarName)

    if (!getMetadataAPI.checkAuth(userid,password,cert, getMetadataAPI.getPrivilegeName("update","jars"))) {
      getMetadataAPI.logAuditRec(userid,Some(AuditConstants.WRITE),AuditConstants.INSERTJAR,jarName,AuditConstants.FAIL,"",jarName)
      requestContext.complete(new ApiResult(ErrorCodeConstants.Failure,"Security", null,"UPDATE not allowed for this user").toString )
    } else {
      val apiResult = getMetadataAPI.UploadJarToDB(jarName,byteArray,userid)
      requestContext.complete(apiResult.toString)
    }
  }
}
