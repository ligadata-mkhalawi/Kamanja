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

import com.ligadata.MetadataAPI._

object UploadModelConfigService {
  case class Process(cfgJson:String)
}

class UploadModelConfigService(requestContext: RequestContext, userid:Option[String], password:Option[String], cert:Option[String]) extends Actor {

  import UploadModelConfigService._

  implicit val system = context.system
  import system.dispatcher
  val log = Logging(system, getClass)
  val APIName = "UploadModelConfigService"
  // 646 - 676 Change begins - replace MetadataAPIImpl with MetadataAPI
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Change ends

  def receive = {
    case Process(cfgJson) =>
      log.debug("Received a upload config request by the actor")
      process(cfgJson)
      context.stop(self)
  }

  def process(cfgJson:String) = {
    log.debug("Requesting UploadModelConfig {}", cfgJson)
    val apiResult = getMetadataAPI.UploadModelsConfig(cfgJson, userid, null)
    requestContext.complete(apiResult)
  }

}
