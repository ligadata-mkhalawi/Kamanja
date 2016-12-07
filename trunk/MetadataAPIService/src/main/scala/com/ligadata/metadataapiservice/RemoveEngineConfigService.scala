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
import com.ligadata.kamanja.metadata._
import scala.util.{ Success, Failure }
import org.json4s.jackson.JsonMethods._
import com.ligadata.MetadataAPI._
import com.ligadata.AuditAdapterInfo.AuditConstants

object RemoveEngineConfigService {
  case class Process(cfgJson:String)
}

class RemoveEngineConfigService(requestContext: RequestContext, userid:Option[String], password:Option[String], cert:Option[String]) extends Actor {

  import RemoveEngineConfigService._

  implicit val system = context.system
  import system.dispatcher
  val log = Logging(system, getClass)
  val APIName = "RemoveEngineConfigService"
  // 646 - 676 Change begins - replace MetadataAPIImpl with MetadataAPI
  val getMetadataAPI = MetadataAPI.getMetadataApiInterface()
  // 646 - 676 Change ends

  def receive = {
    case Process(cfgJson) =>
      process(cfgJson)
      context.stop(self)
  }

  def process(cfgJson:String) = {
    log.debug("Requesting RemoveEngineConfig {}",cfgJson)

    var objectList: List[String] = List[String]()


    try {

//      var inParm: Map[String, Any] = parse(cfgJson).values.asInstanceOf[Map[String, Any]]
      //var args: List[Map[String, String]] = inParm.getOrElse("ArgList", null).asInstanceOf[List[Map[String, String]]] //.asInstanceOf[List[Map[String,String]]
//      args.foreach(elem => {
//        objectList :::= List(elem.getOrElse("NameSpace", "system") + "." + elem.getOrElse("Name", "") + "." + elem.getOrElse("Version", "-1"))
//      })


      if (!getMetadataAPI.checkAuth(userid, password, cert, "write")) {
        getMetadataAPI.logAuditRec(userid, Some(AuditConstants.WRITE), AuditConstants.REMOVECONFIG, cfgJson, AuditConstants.FAIL, "", objectList.mkString(","))
        requestContext.complete(new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Error:UPDATE not allowed for this user").toString)
      } else {
        //        val apiResult = getMetadataAPI.RemoveConfig(cfgJson, userid, objectList.mkString(","))
        val apiResult = getMetadataAPI.RemoveConfig(cfgJson, userid, "adapter")
        requestContext.complete(apiResult)
      }
    }
  catch {
    case e: Exception =>
      log.error("Unable to get tenantid info, please add it to ClusterConfig before using. ", e.getStackTraceString)
      requestContext.complete(new ApiResult(ErrorCodeConstants.Failure, APIName, null, e.toString).toString + " for ArgList" )
  }
  }
}
