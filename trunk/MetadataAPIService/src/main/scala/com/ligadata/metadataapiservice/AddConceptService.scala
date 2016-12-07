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

import com.ligadata.MetadataAPI._
import com.ligadata.AuditAdapterInfo.AuditConstants
import com.ligadata.MetadataAPI.MetadataAPI

object AddConceptService {
	case class Process(conceptJson:String, formatType:String)
}

class AddConceptService(requestContext: RequestContext, userid:Option[String], password:Option[String], cert:Option[String]) extends Actor {

	import AddConceptService._

	implicit val system = context.system
	import system.dispatcher
	val log = Logging(system, getClass)
  val APIName = "AddConceptService"
  // 646 - 676 Change begins - replace MetadataAPIImpl with MetadataAPI
  val getMetadataAPI = MetadataAPI.getMetadataApiInterface()
  // 646 - 676 Change ends

	def receive = {
	  case Process(conceptJson, formatType) =>
	    process(conceptJson, formatType)
	    context.stop(self)
	}

	def process(conceptJson:String, formatType:String): Unit = {
	  log.debug("Requesting AddConcept {},{}",conceptJson,formatType)

    var nameVal: String = null
    if (formatType.equalsIgnoreCase("json")) {
      nameVal = APIService.extractNameFromJson(conceptJson,AuditConstants.CONCEPT)
    } else {
      requestContext.complete(new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Error:Unsupported format: "+formatType).toString )
      return
    }

	  if (!getMetadataAPI.checkAuth(userid, password, cert, getMetadataAPI.getPrivilegeName("insert","concept"))) {
	    getMetadataAPI.logAuditRec(userid,Some(AuditConstants.WRITE),AuditConstants.INSERTOBJECT,conceptJson,AuditConstants.FAIL,"",nameVal)
	    requestContext.complete(new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Error:UPDATE not allowed for this user").toString )
	  } else {
	    val apiResult = getMetadataAPI.AddConcepts(conceptJson,formatType,userid)
	    requestContext.complete(apiResult)
    }
	}
}
