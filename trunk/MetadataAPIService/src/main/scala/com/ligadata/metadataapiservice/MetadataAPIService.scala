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

import akka.actor._
import akka.event.Logging
import com.ligadata.MetadataAPI.MetadataAPI.ModelType
import com.ligadata.metadataapiservice.DetailsLevel.DetailsLevel
import com.ligadata.metadataapiservice._
import spray.routing._
import spray.http._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import MediaTypes._
import com.ligadata.KamanjaBase.{MsgBindingInfo}
import org.apache.logging.log4j._
import com.ligadata.MetadataAPI._
import com.ligadata.Serialize._
import com.ligadata.dataaccessapi.KafkaDataAccessAdapter
import com.ligadata.dataaccessapi.{AttributeDef, AttributeGroupDef, DataContainerDef}
import com.ligadata.dataaccessapi.SearchUtil
import com.ligadata.kamanja.metadata._

class MetadataAPIServiceActor extends Actor with MetadataAPIService {

  def actorRefFactory = context

  def receive = runRoute(metadataAPIRoute)
}

class DataApiResult(var status:String, var statusCode:String, var statusDescription:String, var resultCount:Option[Int], var result: Option[Array[Map[String, Any]]])

trait MetadataAPIService extends HttpService {

  APIInit.Init
  val KEY_TOKN = "keys"
  val ADAPTER_MSG_BINDING = "adaptermessagebinding"
  val AUDIT_LOG_TOKN = "audit_log"
  val LEADER_TOKN = "leader"
  val APIName = "MetadataAPIService"
  val GET_HEALTH = "heartbeat"

  val loggerName = this.getClass.getName
  val logger = LogManager.getLogger(loggerName)
  // logger.setLevel(Level.TRACE);
  // 646 - 676 Change begins - replace MetadataAPIImpl with MetadataAPI
  val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
  // 646 - 676 Change ends

  val daasApi = KafkaDataAccessAdapter.api
  implicit val formats = Serialization.formats(NoTypeHints)
    
  val dataContainers: Map[String, DataContainerDef] = getDataContainerDefinitions
  //Map("customer" -> Map("userdata" -> List("HS_Make", "HS_Model")))

  val metadataAPIRoute = respondWithMediaType(MediaTypes.`application/json`) {
    optionalHeaderValueByName("userid") { userId =>
      {
        optionalHeaderValueByName("password") { password =>
          {
            optionalHeaderValueByName("role") { role =>
              optionalHeaderValueByName("tenantid") { tid =>
                optionalHeaderValueByName("modelconfig") { modelcofniginfo =>
                  var user: Option[String] = None

                  // Make sure that the Audit knows the difference between No User specified and an None (request originates within the engine)
                  if (userId == None) user = Some("kamanja")
                  logger.debug("userid => " + user.get + ",password => xxxxx" + ",role => " + role + ",modelname => " + modelcofniginfo)
                  get {
                    path("api" / Rest) { str =>
                      {
                        val toknRoute = str.split("/")
                        logger.debug("GET reqeust : api/" + str)

		                if (!isUrlSuffix(str)) {
		                  requestContext => requestContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Unknown GET route")).toString)
		                } else if (toknRoute.size == 1) {
		                  if (toknRoute(0).equalsIgnoreCase(AUDIT_LOG_TOKN)) {
		                    requestContext => processGetAuditLogRequest(null, requestContext, user, password, role)
		                  }
		                  else if (toknRoute(0).equalsIgnoreCase(LEADER_TOKN)) {
		                    requestContext => processGetLeaderRequest(null, requestContext, user, password, role)
		                  }
		                  else {
		                    requestContext => processGetObjectRequest(toknRoute(0), "", requestContext, user, password, role)
		                  }
		                }
		                else if (toknRoute(0).equalsIgnoreCase(KEY_TOKN)) {
		                  requestContext => processGetKeysRequest(toknRoute(1).toLowerCase, requestContext, user, password, role)
		                }
		                else if (toknRoute(0).equalsIgnoreCase(AUDIT_LOG_TOKN)) {
		                  // strip the first token and send the rest
		                  val filterParameters = toknRoute.slice(1, toknRoute.size)
		                  requestContext => processGetAuditLogRequest(filterParameters, requestContext, user, password, role)
		                }
		                else if (toknRoute(0).equalsIgnoreCase(ADAPTER_MSG_BINDING)) {
		                  requestContext => processGetAdapterMessageBindingObjectRequest(toknRoute(1).toLowerCase, toknRoute(2).toLowerCase, requestContext, user, password, role)
		                }
		                else {
		                  requestContext => processGetObjectRequest(toknRoute(0).toLowerCase, toknRoute(1).toLowerCase, requestContext, user, password, role)
		             	}
                      }
                    }~
                    pathPrefix("data") {
                      pathEndOrSingleSlash {
                        complete(write(new DataApiResult("success" , "0", "Containers in DaaS API", Some(1), Some(Array(dataContainers)))))
                      } ~
                      pathPrefix(dataContainers) { dcDef =>
                        pathEndOrSingleSlash {
                          parameterMap { params =>
                            requestContext => processGetDataRequest(params, dcDef, dcDef.attributes, requestContext, userId, password, role)
                          }
                        } ~
                        pathPrefix(dcDef.attributeGroups) { agDef =>
                          parameterMap { params =>
                            requestContext => processGetDataRequest(params, dcDef, agDef.attributes, requestContext, userId, password, role)
                          }
                        }
                      }
                    }
                  } ~
                    put {
                      path("api" / Rest) { str =>
                        {
                          logger.debug("PUT reqeust : api/" + str)
                          val toknRoute = str.split("/")
                          if (toknRoute(0).equalsIgnoreCase("UploadJars")) {
                            entity(as[Array[Byte]]) {
                              reqBody =>
                                {
                                  parameters('name) { jarName =>
                                    {
                                      logger.debug("Uploading jar " + jarName)
                                      requestContext =>
                                        val uploadJarService = actorRefFactory.actorOf(Props(new UploadJarService(requestContext, user, password, role)))
                                        uploadJarService ! UploadJarService.Process(jarName, reqBody)
                                    }
                                  }
                                }
                            }
                          } else if (toknRoute(0).equalsIgnoreCase("Activate") || toknRoute(0).equalsIgnoreCase("Deactivate")) {
                            entity(as[String]) { reqBody => requestContext => processPutRequest(toknRoute(0), toknRoute(1).toLowerCase, toknRoute(2), requestContext, user, password, role, modelcofniginfo, tid) }
                          } else {
                            entity(as[String]) { reqBody =>
                              {
                                if (toknRoute.size == 1) { requestContext => processPutRequest(toknRoute(0), reqBody, requestContext, user, password, role, modelcofniginfo, tid) }
                                else if (toknRoute.size == 2 && toknRoute(0) == "model") {
                                  ModelType.withName(toknRoute(1).toString) match {
                                    case ModelType.KPMML => {
                                      val objectType = toknRoute(0) + toknRoute(1)
                                      entity(as[String]) { reqBody => { requestContext => processPutRequest(objectType, reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }

                                    }
                                    case ModelType.JAVA => {
                                      val objectType = toknRoute(0) + toknRoute(1)
                                      entity(as[String]) { reqBody => { requestContext => processPutRequest(objectType, reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }
                                    }

                                    case ModelType.SCALA => {
                                      val objectType = toknRoute(0) + toknRoute(1)
                                      entity(as[String]) { reqBody => { requestContext => processPutRequest(objectType, reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }
                                    }
                                    case ModelType.PMML =>{
                                      val objectType = toknRoute(0) + toknRoute(1)
                                      entity(as[String]) { reqBody => { requestContext => processPutRequest(objectType, reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }
                                  }
                                   case ModelType.PYTHON =>{
                                      val objectType = toknRoute(0) + toknRoute(1)
                                      entity(as[String]) { reqBody => { requestContext => processPutRequest(objectType, reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }
                                  }
                                 case ModelType.JYTHON =>
                                      val objectType = toknRoute(0) + toknRoute(1)
                                      entity(as[String]) { reqBody => { requestContext => processPutRequest(objectType, reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }
                                  }
                                } else { requestContext => requestContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Unknown PUT route")).toString) }
                              }
                            }
                          }
                        }
                      }
                    } ~
                    post {
                      entity(as[String]) { reqBody =>
                        path("api" / Rest) { str =>
                          {
                            val toknRoute = str.split("/")
                            logger.debug("POST reqeust : api/" + str)
                            if (toknRoute.size == 1) {
                              if (toknRoute(0).equalsIgnoreCase(GET_HEALTH))
                                requestContext => processHBRequest(DetailsLevel.ALL, reqBody, requestContext, user, password, role)
                              else {
                                if (toknRoute(0).equalsIgnoreCase("model"))
                                  logger.warn("MetadataAPI Http Service: URL of type https://hostname:port/api/model is deprecated")
                                entity(as[String]) { reqBody => { requestContext => processPostRequest(toknRoute(0), reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }
                              }
                            } else if (toknRoute.size == 2 && toknRoute(0).equalsIgnoreCase(GET_HEALTH)) {
                              val detailsLevel = DetailsLevel.withName(toknRoute(1))
                              requestContext => processHBRequest(detailsLevel, reqBody, requestContext, user, password, role)
                            } else if (toknRoute.size == 2 && toknRoute(0).equalsIgnoreCase("model")) {
                              ModelType.withName(toknRoute(1).toString) match {
                                case ModelType.KPMML => {
                                  val objectType = toknRoute(0) + toknRoute(1)
                                  entity(as[String]) { reqBody => { requestContext => processPostRequest(objectType, reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }
                                }

                                case ModelType.JAVA => {
                                  val objectType = toknRoute(0) + toknRoute(1)
                                  entity(as[String]) { reqBody => { requestContext => processPostRequest(objectType, reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }
                                }

                                case ModelType.SCALA => {
                                  val objectType = toknRoute(0) + toknRoute(1)
                                  entity(as[String]) { reqBody => { requestContext => processPostRequest(objectType, reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }
                                }
                                case ModelType.PMML => {
                                  val objectType = toknRoute(0) + toknRoute(1)
                                  entity(as[String]) { reqBody => { requestContext => processPostRequest(objectType, reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }
                              }
                               case ModelType.PYTHON => {
                                  val objectType = toknRoute(0) + toknRoute(1)
                                  entity(as[String]) { reqBody => { requestContext => processPostRequest(objectType, reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }
                              }
                             case ModelType.JYTHON =>
                                  val objectType = toknRoute(0) + toknRoute(1)
                                  entity(as[String]) { reqBody => { requestContext => processPostRequest(objectType, reqBody, requestContext, user, password, role, modelcofniginfo, tid) } }
                              }
                            } else { requestContext => requestContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Unknown POST route")).toString) }
                          }
                        }
                      }~
                        entity(as[String]) { reqBody =>
                          pathPrefix("data" /Rest) {
                                  str => {
                                    logger.debug("POST reqeust : data" + str)
                                    val toknRoute = str.split("/")
                                    if (toknRoute.size == 0 || toknRoute(0) == null) {
                                      complete(write((new DataApiResult("-1", APIName, "Unknown POST route", None, None))))
                                    } else {
                                      val searchObj = new SearchUtil(toknRoute(0))
                                      if (!searchObj.checkMessageExists(toknRoute(0))) {
                                        complete(write(new DataApiResult("-1" , APIName, "did not find %s message in metadata".format(toknRoute(0)), None, None)))
                                      } else {
                                        parameterMap { params =>
                                          val DeserializerFormat = searchObj.getDeserializerType(params.getOrElse("format", "").toString)
                                          val options = searchObj.getDeserializeOption(params.getOrElse("alwaysQuoteFields", ""),params.getOrElse("fieldDelimiter", ""),params.getOrElse("valueDelimiter", ""), params.getOrElse("options", ""))
                                          val msgBindingInfo = searchObj.ResolveDeserializer(toknRoute(0), DeserializerFormat,  options)
                                          val partitionKey = searchObj.getMessageKey(toknRoute(0), reqBody, msgBindingInfo)
                                          val optionsWithFormatType = searchObj.getDeserializeOptionWithFormatType(params.getOrElse("alwaysQuoteFields", ""),params.getOrElse("fieldDelimiter", ""),params.getOrElse("valueDelimiter", ""), DeserializerFormat)
                                          val message = searchObj.makeMessage(toknRoute(0), optionsWithFormatType, reqBody)
                                          requestContext => processSetDataRequest(toknRoute(0), message, partitionKey)
                                            requestContext.complete(write(new DataApiResult("success" , "0", "Sent Data", None, None)))
                                        }
                                      }
                                    }
                                  }
                          }
                        }
                    } ~
                    delete {
                      entity(as[String]) { reqBody =>
                        path("api" / Rest) { str =>
                          {

                            val toknRoute = str.split("/")
                            logger.debug("DELETE reqeust : api/" + str)
                            if (toknRoute.size == 2) { requestContext => processDeleteRequest(toknRoute(0).toLowerCase, toknRoute(1).toLowerCase, requestContext, user, password, role) }
                            else { requestContext => requestContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Unknown DELETE route")).toString) }
                          }
                        }
                      }
                    }
                } //modelname
              } // TennantId
            } // Role
          }
        } //password 2x
      }
    } //userid
  } // Routes

  /**
   * Modify Existing objects in the Metadata
   */
  private def processPutRequest(objtype: String, body: String, rContext: RequestContext, userid: Option[String], password: Option[String], role: Option[String], modelcompileinfo: Option[String], tid: Option[String]): Unit = {
    val action = "Update" + objtype
    val notes = "Invoked " + action + " API "
    if (objtype.equalsIgnoreCase("AdapterMessageBinding")) {
      // Not Supported yet
      rContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Operatin not supported")).toString)
    } else if (objtype.equalsIgnoreCase("Container")) {
      val updateContainerDefsService = actorRefFactory.actorOf(Props(new UpdateContainerService(rContext, userid, password, role, tid)))
      updateContainerDefsService ! UpdateContainerService.Process(body)
    } else if (objtype.equalsIgnoreCase("Model")) {
      val updateModelService: ActorRef = actorRefFactory.actorOf(Props(new UpdateModelService(rContext, userid, password, role, None, tid)))
      updateModelService ! UpdateModelService.Process(body)
    } else if (objtype.equalsIgnoreCase("Message")) {
      val updateMessageDefsService = actorRefFactory.actorOf(Props(new UpdateMessageService(rContext, userid, password, role, tid)))
      updateMessageDefsService ! UpdateMessageService.Process(body, "JSON")
    } else if (objtype.equalsIgnoreCase("Type")) {
      val updateTypeDefsService = actorRefFactory.actorOf(Props(new UpdateTypeService(rContext, userid, password, role)))
      updateTypeDefsService ! UpdateTypeService.Process(body, "JSON")
    } else if (objtype.equalsIgnoreCase("Concept")) {
      val updateConceptDefsService = actorRefFactory.actorOf(Props(new UpdateConceptService(rContext, userid, password, role)))
      updateConceptDefsService ! UpdateConceptService.Process(body)
    } else if (objtype.equalsIgnoreCase("Function")) {
      val updateFunctionDefsService = actorRefFactory.actorOf(Props(new UpdateFunctionService(rContext, userid, password, role)))
      updateFunctionDefsService ! UpdateFunctionService.Process(body)
    } else if (objtype.equalsIgnoreCase("RemoveConfig")) {
      val removeConfigService = actorRefFactory.actorOf(Props(new RemoveEngineConfigService(rContext, userid, password, role)))
      removeConfigService ! RemoveEngineConfigService.Process(body)
    } else if (objtype.equalsIgnoreCase("UploadConfig")) {
      val uploadConfigService = actorRefFactory.actorOf(Props(new UploadEngineConfigService(rContext, userid, password, role)))
      uploadConfigService ! UploadEngineConfigService.Process(body)
    } else if (objtype.equalsIgnoreCase("UploadModelConfig")) {
      logger.debug("In put request process of UploadModelConfig")
      val addModelDefsService = actorRefFactory.actorOf(Props(new UploadModelConfigService(rContext, userid, password, role)))
      addModelDefsService ! UploadModelConfigService.Process(body)
    } else if (objtype.equalsIgnoreCase("modeljava")) {
      logger.debug("In put request process of model java")
      val updateSourceModelService: ActorRef = actorRefFactory.actorOf(Props(new UpdateSourceModelService(rContext, userid, password, role, modelcompileinfo, tid)))
      updateSourceModelService ! UpdateSourceModelService.UpdateJava(body)

    } else if (objtype.equalsIgnoreCase("modelscala")) {
      try {
        val updateSourceModelService: ActorRef = actorRefFactory.actorOf(Props(new UpdateSourceModelService(rContext, userid, password, role, modelcompileinfo, tid)))
        updateSourceModelService ! UpdateSourceModelService.UpdateScala(body)
      } catch {
        case e: Exception => {
          logger.debug("Exception updating scala model", e)
        }
      }

    } else if (objtype.equalsIgnoreCase("modelkpmml")) {
      val addModelService: ActorRef = actorRefFactory.actorOf(Props(new UpdateModelService(rContext, userid, password, role, None, tid)))
      addModelService ! UpdateModelService.Process(body)
    } else if (objtype.equalsIgnoreCase("modelpmml")) {
      val addModelService: ActorRef = actorRefFactory.actorOf(Props(new UpdateModelService(rContext, userid, password, role, modelcompileinfo, tid)))
      addModelService ! UpdateModelService.Process(body)
    }else if (objtype.equalsIgnoreCase("modelpython")) {
      val addModelService: ActorRef = actorRefFactory.actorOf(Props(new UpdateModelService(rContext, userid, password, role, modelcompileinfo, tid)))
      addModelService ! UpdateModelService.Process(body)
    }else if (objtype.equalsIgnoreCase("modeljython")) {
      val addModelService: ActorRef = actorRefFactory.actorOf(Props(new UpdateModelService(rContext, userid, password, role, modelcompileinfo, tid)))
      addModelService ! UpdateModelService.Process(body)
    } else {
      rContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Unknown PUT route")).toString)
    }
  }

  /**
   * Modify Existing objects in the Metadata
   * Modify Existing objects in the Metadata
   */
  private def processPutRequest(action: String, objtype: String, objKey: String, rContext: RequestContext, userid: Option[String], password: Option[String], role: Option[String], modelname: Option[String], tid: Option[String]): Unit = {
    var argParm: String = verifyInput(objKey, objtype, rContext)
    if (argParm == null) return

    if (action.equalsIgnoreCase("Activate")) {
      val activateObjectsService = actorRefFactory.actorOf(Props(new ActivateObjectsService(rContext, userid, password, role)))
      activateObjectsService ! ActivateObjectsService.Process(argParm)
    } else if (action.equalsIgnoreCase("Deactivate")) {
      val deactivateObjectsService = actorRefFactory.actorOf(Props(new DeactivateObjectsService(rContext, userid, password, role)))
      deactivateObjectsService ! DeactivateObjectsService.Process(argParm)
    } else {
      rContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Unknown PUT route")).toString)
    }
  }

  /**
   * Create new Objects in the Metadata
   */
  private def processPostRequest(objtype: String, body: String, rContext: RequestContext, userid: Option[String], password: Option[String], role: Option[String], modelcompileinfo: Option[String], tenantId: Option[String]): Unit = {
    val action = "Add" + objtype
    val notes = "Invoked " + action + " API "

    if (objtype.equalsIgnoreCase("AdapterMessageBinding")) {
      val addAdapterMsgBindingsService = actorRefFactory.actorOf(Props(new AddAdapterMessageBindingsService(rContext, userid, password, role)))
      addAdapterMsgBindingsService ! AddAdapterMessageBindingsService.Process(body)
    } else if (objtype.equalsIgnoreCase("Container")) {
      val addContainerDefsService = actorRefFactory.actorOf(Props(new AddContainerService(rContext, userid, password, role, tenantId)))
      addContainerDefsService ! AddContainerService.Process(body)
    } else if (objtype.equalsIgnoreCase("Model")) {
      val addModelService: ActorRef = actorRefFactory.actorOf(Props(new AddModelService(rContext, userid, password, role, None, tenantId)))
      addModelService ! AddModelService.Process(body)
    } else if (objtype.equalsIgnoreCase("Message")) {
      val addMessageDefsService = actorRefFactory.actorOf(Props(new AddMessageService(rContext, userid, password, role, tenantId)))
      addMessageDefsService ! AddMessageService.Process(body)
    } else if (objtype.equalsIgnoreCase("Type")) {
      val addTypeDefsService = actorRefFactory.actorOf(Props(new AddTypeService(rContext, userid, password, role)))
      addTypeDefsService ! AddTypeService.Process(body, "JSON")
    } else if (objtype.equalsIgnoreCase("Concept")) {
      val addConceptDefsService = actorRefFactory.actorOf(Props(new AddConceptService(rContext, userid, password, role)))
      addConceptDefsService ! AddConceptService.Process(body, "JSON")
    } else if (objtype.equalsIgnoreCase("Function")) {
      val addFunctionDefsService = actorRefFactory.actorOf(Props(new AddFunctionService(rContext, userid, password, role)))
      addFunctionDefsService ! AddFunctionService.Process(body, "JSON")
    } else if (objtype.equalsIgnoreCase("UploadModelConfig")) {
      //TODO
      //call the UploadModelConfig in the MetadataAPIImpl
      //UploadModelsConfig (cfgStr: String,userid:Option[String], objectList: String): String = {
      logger.debug("In post request process of UploadModelConfig")
      val addModelDefsService = actorRefFactory.actorOf(Props(new UploadModelConfigService(rContext, userid, password, role)))
      addModelDefsService ! UploadModelConfigService.Process(body)
    } else if (objtype.equalsIgnoreCase("modeljava")) {
      //TODO
      logger.debug("In post request process of model java")

      val addSourceModelService: ActorRef = actorRefFactory.actorOf(Props(new AddSourceModelService(rContext, userid, password, role, modelcompileinfo, tenantId)))
      addSourceModelService ! AddSourceModelService.ProcessJava(body)

    } else if (objtype.equalsIgnoreCase("modelscala")) {

      logger.debug("In post request process of model scala")
      // rContext.complete(new ApiResult(ErrorCodeConstants.Success, "AddModelFromScalaSource",body.toString, "Upload of java model successful").toString)
      val addSourceModelService: ActorRef = actorRefFactory.actorOf(Props(new AddSourceModelService(rContext, userid, password, role, modelcompileinfo, tenantId)))
      addSourceModelService ! AddSourceModelService.ProcessScala(body)
    }else if (objtype.equalsIgnoreCase("modelkpmml")) {
      val addModelService: ActorRef = actorRefFactory.actorOf(Props(new AddModelService(rContext, userid, password, role, None, tenantId)))
      addModelService ! AddModelService.Process(body)
    } else if (objtype.equalsIgnoreCase("modelpmml")) {
      val addModelService: ActorRef = actorRefFactory.actorOf(Props(new AddModelService(rContext, userid, password, role, modelcompileinfo, tenantId)))
      addModelService ! AddModelService.Process(body)
    } else if (objtype.equalsIgnoreCase("modelpython")) {
      val addModelService: ActorRef = actorRefFactory.actorOf(Props(new AddModelService(rContext, userid, password, role, modelcompileinfo, tenantId)))
      addModelService ! AddModelService.Process(body)
    }  else if (objtype.equalsIgnoreCase("modeljython")) {
      val addModelService: ActorRef = actorRefFactory.actorOf(Props(new AddModelService(rContext, userid, password, role, modelcompileinfo, tenantId)))
      addModelService ! AddModelService.Process(body)
    }
    else {
      rContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Unknown POST route")).toString)
    }
  }

  /**
   *
   */
  private def processGetAuditLogRequest(filterParameters: Array[String], rContext: RequestContext, userid: Option[String], password: Option[String], role: Option[String]): Unit = {
    val auditLogService = actorRefFactory.actorOf(Props(new GetAuditLogService(rContext, userid, password, role)))
    auditLogService ! GetAuditLogService.Process(filterParameters)
  }

  /**
   *
   */
  private def processGetLeaderRequest(nodeList: Array[String], rContext: RequestContext, userid: Option[String], password: Option[String], role: Option[String]): Unit = {
    val auditLogService = actorRefFactory.actorOf(Props(new GetLeaderService(rContext, userid, password, role)))
    auditLogService ! GetLeaderService.Process(nodeList)
  }

  /**
   *
   */

  private def processGetKeysRequest(objtype: String, rContext: RequestContext, userid: Option[String], password: Option[String], role: Option[String]): Unit = {
    if (objtype.equalsIgnoreCase("Container") || objtype.equalsIgnoreCase("Model") || objtype.equalsIgnoreCase("Message") || objtype.equalsIgnoreCase("Function") ||
      objtype.equalsIgnoreCase("Concept") || objtype.equalsIgnoreCase("Type") || objtype.equalsIgnoreCase("OutputMsg") || objtype.equalsIgnoreCase("adaptermessagebinding")) {
      val allObjectKeysService = actorRefFactory.actorOf(Props(new GetAllObjectKeysService(rContext, userid, password, role)))
      allObjectKeysService ! GetAllObjectKeysService.Process(objtype)
    } else {
      rContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Unknown GET route")).toString)
    }
  }

  /**
   */
  private def processHBRequest(detailsLevel: DetailsLevel, nodeIds: String, rContext: RequestContext, userid: Option[String], password: Option[String], role: Option[String]): Unit = {
    val heartBeatSerivce = actorRefFactory.actorOf(Props(new GetHeartbeatService(rContext, userid, password, role)))
    heartBeatSerivce ! GetHeartbeatService.Process(nodeIds, detailsLevel)
  }


  /**
    *
    */
  private def processGetAdapterMessageBindingObjectRequest(objType: String, objKey: String, rContext: RequestContext, userid: Option[String], password: Option[String], role: Option[String]): Unit = {
    val action = "Get" + objType
    val notes = "Invoked " + action + " API "
    var argParm: String = null

    val allObjectsService = actorRefFactory.actorOf(Props(new GetAdapterMessageBindigsService(rContext, userid, password, role)))
    allObjectsService ! GetAdapterMessageBindigsService.Process(objType, objKey)

  }


  /**
   *
   */
  private def processGetObjectRequest(objtype: String, objKey: String, rContext: RequestContext, userid: Option[String], password: Option[String], role: Option[String]): Unit = {
    val action = "Get" + objtype
    val notes = "Invoked " + action + " API "
    var argParm: String = null

    if (!(objtype.equalsIgnoreCase("config") || objtype.equalsIgnoreCase("TypeBySchemaId") || objtype.equalsIgnoreCase("TypeByElementId"))) {
      argParm = verifyInput(objKey, objtype, rContext)
      if (argParm == null) return
    }
    if (objtype.equalsIgnoreCase("Config")) {
      val allObjectsService = actorRefFactory.actorOf(Props(new GetConfigObjectsService(rContext, userid, password, role)))
      allObjectsService ! GetConfigObjectsService.Process(objKey)
    } else if (objtype.equalsIgnoreCase("Container") || objtype.equalsIgnoreCase("Model") || objtype.equalsIgnoreCase("Message") ||
      objtype.equalsIgnoreCase("Function") || objtype.equalsIgnoreCase("Concept") || objtype.equalsIgnoreCase("Type") || objtype.equalsIgnoreCase("OutputMsg")) {
      val getObjectsService = actorRefFactory.actorOf(Props(new GetObjectsService(rContext, userid, password, role)))
      getObjectsService ! GetObjectsService.Process(argParm)
    } else if (objtype.equalsIgnoreCase("TypeBySchemaId") || objtype.equalsIgnoreCase("TypeByElementId")) {
      val typeByIdsService = actorRefFactory.actorOf(Props(new GetTypeByIdService(rContext, userid, password, role)))
      typeByIdsService ! GetTypeByIdService.Process(objtype, objKey)

    } else {
      rContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Unknown GET route")).toString)
    }
  }

  /**
   *
   */
  private def processDeleteRequest(objtype: String, objKey: String, rContext: RequestContext, userid: Option[String], password: Option[String], role: Option[String]): Unit = {
    val action = "Remove" + objtype
    val notes = "Invoked " + action + " API "
    var argParm: String = null
    if (!objtype.equalsIgnoreCase("Config") &&
        !objtype.equalsIgnoreCase("AdapterMessageBinding"))
      argParm = verifyInput(objKey, objtype, rContext)
    else
      argParm = objKey

    if (argParm == null) return

    if (objtype.equalsIgnoreCase("AdapterMessageBinding")) {
      val removeObjectsService = actorRefFactory.actorOf(Props(new RemoveAdapterMsgBindingsService(rContext, userid, password, role)))
      removeObjectsService ! RemoveAdapterMsgBindingsService.Process(argParm)
    }
    else if (objtype.equalsIgnoreCase("Container") || objtype.equalsIgnoreCase("Model") || objtype.equalsIgnoreCase("Message") ||
      objtype.equalsIgnoreCase("Function") || objtype.equalsIgnoreCase("Concept") || objtype.equalsIgnoreCase("Type") || objtype.equalsIgnoreCase("OutputMsg")) {
      val removeObjectsService = actorRefFactory.actorOf(Props(new RemoveObjectsService(rContext, userid, password, role)))
      removeObjectsService ! RemoveObjectsService.Process(argParm)
    } else if (objtype.equalsIgnoreCase("Config")) {
      val removeConfigService = actorRefFactory.actorOf(Props(new RemoveEngineConfigService(rContext, userid, password, role)))
      removeConfigService ! RemoveEngineConfigService.Process(argParm)
    } else {
      rContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Unknown DELETE route")).toString)
    }
  }

  private def verifyInput(objKey: String, objType: String, rContext: RequestContext): String = {
    // Verify that the a 3 part name is the key, an Out Of Bounds exception will be thrown if name is not XXX.XXX.XXX
    try {
      return createGetArg(objKey, objType)
    } catch {
      case aobe: ArrayIndexOutOfBoundsException => {
        logger.debug("METADATASERVICE: Invalid key " + objKey, aobe)
        rContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Invalid key: " + objKey)).toString)
        return null
      }
      case nfe: java.lang.NumberFormatException => {
        logger.debug("METADATASERVICE: Invalid key " + objKey, nfe)
        rContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Invalid key: " + objKey)).toString)
        return null
      }
      case iae: com.ligadata.Exceptions.InvalidArgumentException => {
        logger.debug("METADATASERVICE: Invalid key " + objKey, iae)
        rContext.complete((new ApiResult(ErrorCodeConstants.Failure, APIName, null, "Invalid key: " + objKey)).toString)
        return null
      }
    }
  }

  private def isUrlSuffix(str: String): Boolean = {
    if ((str == null) ||
      ((str != null) && (str.size == 0))) {
      return false
    }
    return true
  }
  /**
   * MakeJsonStrForArgList
   */
  private def createGetArg(objKey: String, objectType: String): String = {
    /* val keyTokens = objKey.split("\\.")
    val nameSpace = keyTokens(0)
    val name = keyTokens(1)
    val version = keyTokens(2)
   */
    val (ns, name, ver) = com.ligadata.kamanja.metadata.Utils.parseNameToken(objKey)
    val lVersion = ver.toLong
    val mdArg = new MetadataApiArg(objectType, ns, name, ver, "JSON")
    val argList = new Array[MetadataApiArg](1)
    argList(0) = mdArg
    val mdArgList = new MetadataApiArgList(argList.toList)
    val apiArgJson = JsonSerializer.SerializeApiArgListToJson(mdArgList)
    apiArgJson
  }

  private def processGetDataRequest(params: Map[String, String], dcDef: DataContainerDef, attr: Array[AttributeDef], rContext: RequestContext, userid: Option[String], password: Option[String], role: Option[String]): Unit = {
    try {
      val missing = dcDef.key.filter(k => !params.contains(k))
      if (missing != null && missing.length > 0)
        rContext.complete(400, write(new DataApiResult("error", "9002", "Missing key " + missing.mkString(","), None, None)))
      else {
        val key = dcDef.key.map(k => params.getOrElse(k, null))

        val fields = params.getOrElse("fields", "").toLowerCase.split(",")
        val projectList = if (fields.length == 0) attr else attr.filter(a => fields.contains(a.name))
        val filter = params.getOrElse("filter", null)

        daasApi.retrieve(dcDef.fullContainerName, projectList, key, filter, rContext, (ctx, result, status) => {
          val context = ctx.asInstanceOf[RequestContext]
          if(status == "1000")
            context.complete(result.asInstanceOf[String])
          else
            context.complete(500, result.asInstanceOf[String])
        })
      }
    } catch {
      case e: Throwable => {
        logger.error("Error processing request: " + e.getMessage, e)
        rContext.complete(500, write(new DataApiResult("error", "9000", "Internal Error: " + e.getMessage, None, None)))
      }
    }
  }
  
  private def getDataContainerDefinitions(): Map[String, DataContainerDef] = {
    val results = scala.collection.mutable.Map[String, DataContainerDef]()
    try {
      val contDefs = MdMgr.mdMgr.Containers(true, true)
      contDefs match {
        case None =>
          //None
          logger.debug("No Containers found ")
        case Some(cs) =>
          cs.foreach(c => {
            val containerDef = c.cType.asInstanceOf[ContainerTypeDef]
            val attribs =
              if (containerDef.IsFixed) {
                containerDef.asInstanceOf[StructTypeDef].memberDefs.toList
              } else {
                containerDef.asInstanceOf[MappedMsgTypeDef].attrMap.map(kv => kv._2).toList
              }

            val attributes = attribs.map(a => new AttributeDef(a.name, a.typeDef.Name))
            c.cType.partitionKey
            results.put(c.name, new DataContainerDef(c.name, c.FullName, attributes.toArray, Map[String, AttributeGroupDef](), c.cType.partitionKey))
          })
      }
    } catch {
      case e: Throwable => logger.error("Error retrieving container definitions ", e)
    }
    return results.toMap
  }

  /**
    * used to push data in kafka topic
    * @param messageFullName message name
    * @param data record that pushed to kafka
    * @param key partition key for message
    */
  private def processSetDataRequest(messageFullName: String, data: String,key: Array[String]): Unit = {
    daasApi.sendToKafka(key, data)
  }
  }

