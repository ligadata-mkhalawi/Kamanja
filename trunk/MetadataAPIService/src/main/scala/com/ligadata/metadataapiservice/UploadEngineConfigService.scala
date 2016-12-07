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
import org.json4s.jackson.JsonMethods._
import scala.util.{ Success, Failure }
import com.ligadata.MetadataAPI.MetadataAPI

import com.ligadata.MetadataAPI._
import com.ligadata.AuditAdapterInfo.AuditConstants

object UploadEngineConfigService {
  case class Process(cfgJson:String)
}

class UploadEngineConfigService(requestContext: RequestContext, userid:Option[String], password:Option[String], cert:Option[String]) extends Actor {

  import UploadEngineConfigService._

  implicit val system = context.system
  import system.dispatcher
  val log = Logging(system, getClass)
  val APIName = "UploadEngineConfigService"
  // 646 - 676 Change begins - replace MetadataAPIImpl with MetadataAPI
  val getMetadataAPI = MetadataAPI.getMetadataApiInterface()
  // 646 - 676 Change ends

  def receive = {
    case Process(cfgJson) =>
      process(cfgJson)
      context.stop(self)
  }

  def process(cfgJson:String) = {

    log.debug("Requesting UploadEngineConfig {}",cfgJson)

    var objectList: List[String] = List[String]()

    var inParm: Map[String,Any] = parse(cfgJson).values.asInstanceOf[Map[String,Any]]
    var args: List[Map[String,String]] = inParm.getOrElse("Clusters",null).asInstanceOf[List[Map[String,String]]]   //.asInstanceOf[List[Map[String,String]]
    args.foreach(elem => {
      objectList :::= List(elem.getOrElse("ClusterId",""))
    })

    if (!getMetadataAPI.checkAuth(userid,password,cert, getMetadataAPI.getPrivilegeName("update","configuration"))) {
      getMetadataAPI.logAuditRec(userid,Some(AuditConstants.WRITE),AuditConstants.INSERTCONFIG,cfgJson,AuditConstants.FAIL,"",objectList.mkString(","))
      requestContext.complete(new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Error:UPDATE not allowed for this user").toString )
    } else {
      val apiResult = getMetadataAPI.UploadConfig(cfgJson, userid, objectList.mkString(","))
      requestContext.complete(apiResult)
    }
  }
}
