package com.ligadata.dataaccessapi

import org.apache.logging.log4j.{ Logger, LogManager }
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import com.google.common.collect.Synchronized

//import org.json4s.native.Serialization
//import org.json4s.native.Serialization._

object KafkaDataAccessAdapter {
  var api: KafkaDataAccessAdapter = null

  def Init(configJson: String) = synchronized {
    if(api == null) {
      api = new KafkaDataAccessAdapter(configJson)
      api.start
    }
  }
  
  def Shutdown() = { 
    if(api != null) {
      api.stop 
      api = null
    }
  } 
}

class KafkaDataAccessAdapter(configJson: String) extends DataAccessAPI {
  lazy val log = LogManager.getLogger(this.getClass.getName)
  //implicit val formats = DefaultFormats
  
  val pendingRequests = scala.collection.mutable.Map[String, (Any, (Any, Any, String) => Unit)]()
  val config = parse(configJson).values.asInstanceOf[Map[String, Any]]
  
  val requestConfig = config.getOrElse("requestTopic", null)
  if(requestConfig == null)
    throw new Exception("Error in configuration: requestTopic needs to be specified.")

  val responseConfig = config.getOrElse("responseTopic", null)
  if(responseConfig == null)
    throw new Exception("Error in configuration: responseTopic needs to be specified.")
  
  var requestTopic: KafkaRequestSender = null
  var responseTopic: KafkaResponseReader = null
  
  def start() = {
    log.debug("Starting Kafka producer ")
    requestTopic = new KafkaRequestSender(requestConfig.asInstanceOf[Map[String, String]])
    requestTopic.start
    
    log.debug("Starting Kafka consumer ")
    responseTopic = new KafkaResponseReader(responseConfig.asInstanceOf[Map[String, String]])
    responseTopic.start(recieveResponse)
  }
  
  def stop() = {    
    if(requestTopic != null) requestTopic.stop
    if(responseTopic != null) responseTopic.stop
  }
  
  def addRequest(reqid: String, reqCtx: (Any, (Any, Any, String) => Unit)) = synchronized {
    pendingRequests(reqid) = reqCtx
  }
  
  def removeRequest(reqid: String): (Any, (Any, Any, String) => Unit) = synchronized {
    val ctx = pendingRequests.remove(reqid)
    return ctx.getOrElse(null)
  }

  override def retrieve(fullContainerName: String, select : Array[AttributeDef], keys: Array[String], filter: String, context: Any, callback: (Any, Any, String) => Unit) = {
    val reqId = java.util.UUID.randomUUID().toString
    addRequest(reqId, (context, callback))
    val projections = select.map( s => s.name )

    //    val reqJson = compact(render(("id" -> reqId) ~ ("containerName" -> fullContainerName) ~ ("key" -> keys.toList) ~ ("projections" -> projections.toList)))
    var req = ("id" -> reqId) ~ ("containerName" -> fullContainerName) ~ ("key" -> keys.toList)
    if(projections.length > 0)
      req = req ~ ("projections" -> projections.toList)
    if(filter != null && filter.length > 0)
      req = req ~ ("filter" -> filter)
    val reqJson = compact(render(req))
    requestTopic.send(Array(reqId), Array(reqJson.getBytes("UTF8")))
  }

  def recieveResponse(responseJson: String) = {
    try {
      val response = responseJson.replace("\\", "")
      val res = parse(response).values.asInstanceOf[Map[String, Any]]
      val reqId = res.getOrElse("id", null)
      if (reqId == null)
        log.error("Missing id in response message: " + response)
      else {
        val reqCtx = removeRequest(reqId.toString)
        if (reqCtx == null)
          log.error("Unknown request id in response message: " + response)
        else {
          val context = reqCtx._1
          val callback = reqCtx._2
          callback(context, response, res.getOrElse("statusCode", "1000").toString)
        }
      }
    } catch {
      case e: Throwable => {
        log.warn("Error processing response: " + e.getMessage, e)
      }
    }
  }

  def sendToKafka(key: Array[String], message: String, context: Any, callback: (Any, Any, String) => Unit) ={
    val reqId = java.util.UUID.randomUUID().toString
    addRequest(reqId, (context, callback))
    requestTopic.send(key, Array(message.getBytes("UTF8")))
  }
}