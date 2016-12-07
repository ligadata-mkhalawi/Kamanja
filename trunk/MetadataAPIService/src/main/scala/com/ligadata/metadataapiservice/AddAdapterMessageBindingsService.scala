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
import com.ligadata.Exceptions.{Json4sParsingException, InvalidArgumentException}
import spray.routing.RequestContext
import spray.httpx.SprayJsonSupport
import spray.client.pipelining._
import scala.collection.mutable.Map
import scala.util.{ Success, Failure }
import com.ligadata.MetadataAPI._
import com.ligadata.kamanja.metadata._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.util.control._
import org.apache.logging.log4j._
import com.ligadata.AuditAdapterInfo.AuditConstants
import com.ligadata.MetadataAPI.MetadataAPI

object AddAdapterMessageBindingsService {
  case class Process(objectKey: String)
}

class AddAdapterMessageBindingsService(requestContext: RequestContext, userid:Option[String], password:Option[String], cert:Option[String]) extends Actor {

  import AddAdapterMessageBindingsService._
  implicit val system = context.system
  val log = Logging(system, getClass)

  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)
  // logger.setLevel(Level.TRACE);

  // 646 - 676 Change begins - replace MetadataAPIImpl with MetadataAPI
  val getMetadataAPI = MetadataAPI.getMetadataApiInterface()
  // 646 - 676 Change ends
  val APIName = "AddAdapterMessageBindigsService"

  def addAdapterMessageBindingObjects(source: String): String = {

    //TODO we need to move this logic into MetadataAPI... this is identical as the Kamanja Command LIne
    val trimmedInput : String = source.trim
    val isMap : Boolean = trimmedInput.startsWith("{")
    val isList : Boolean = trimmedInput.startsWith("[")

    try {
      val reasonable : Boolean = isMap || isList
      if (! reasonable) {
        throw InvalidArgumentException("the adapter string specified must be either a json map or json array.", null)
      }
      val result : String = if (isMap) {
        val bindingSpec : scala.collection.immutable.Map[String,Any] = jsonStringAsColl(trimmedInput).asInstanceOf[scala.collection.immutable.Map[String,Any]]
        val mutableBindingSpec : Map[String,Any] = Map[String,Any]()
        bindingSpec.foreach(pair => mutableBindingSpec(pair._1) = pair._2)
        AdapterMessageBindingUtils.AddAdapterMessageBinding(mutableBindingSpec, userid)
      } else {
        val bindingSpecList : List[scala.collection.immutable.Map[String,Any]] = jsonStringAsColl(trimmedInput).asInstanceOf[List[scala.collection.immutable.Map[String,Any]]]
        val mutableMapsList : List[Map[String,Any]] = bindingSpecList.map(aMap => {
          val mutableBindingSpec : Map[String,Any] = Map[String,Any]()
          aMap.foreach(pair => mutableBindingSpec(pair._1) = pair._2)
          mutableBindingSpec
        })
        AdapterMessageBindingUtils.AddAdapterMessageBinding(mutableMapsList, userid)
      }
      return result
    } catch {
      case e: Exception => {
        return (new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Error: Unable to add adapter " + e.getMessage).toString)
      }
      case e: Throwable => {
        return (new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Error: Unable to add adapter " + e.getMessage).toString)
      }
    }
  }


  def receive = {
    case Process(source) =>
      process(source)
      context.stop(self)
  }

  def process(source: String) = {
    log.debug("Adding AdapterBindingsObjects {}", "JSON")

    if (!getMetadataAPI.checkAuth(userid,password,cert, getMetadataAPI.getPrivilegeName("add","adaptermessagebinding"))) {
      getMetadataAPI.logAuditRec(userid,Some(AuditConstants.READ),AuditConstants.GETCONFIG,AuditConstants.CONFIG,AuditConstants.FAIL,"","AdapterMessageBindings")
      requestContext.complete(new ApiResult(ErrorCodeConstants.Failure,APIName, null, "Error: READ not allowed for this user").toString )
    } else {
      val apiResult = addAdapterMessageBindingObjects(source)
      requestContext.complete(apiResult)
    }
  }


  @throws(classOf[com.ligadata.Exceptions.Json4sParsingException])
  @throws(classOf[com.ligadata.Exceptions.InvalidArgumentException])
  private def jsonStringAsColl(configJson: String): Any = {
    val jsonObjs : Any = try {
      implicit val jsonFormats: Formats = DefaultFormats
      val json = parse(configJson)
      logger.debug("Parsed the json : " + configJson)
      json.values
    } catch {
      case e: MappingException => {
        logger.debug("", e)
        throw Json4sParsingException(e.getMessage, e)
      }
      case e: Exception => {
        logger.debug("", e)
        throw InvalidArgumentException(e.getMessage, e)
      }
    }
    jsonObjs
  }
}
