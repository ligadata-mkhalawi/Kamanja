package com.ligadata.tools.HttpEndpoint

import akka.actor.Actor
import spray.http._
import spray.httpx.unmarshalling._
import spray.routing._
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization._
import org.apache.logging.log4j.{ Logger, LogManager }

class HttpEndpointServiceActor extends Actor with HttpEndpointService {
  def actorRefFactory = context
  def receive = runRoute(endpointRoute)
}

trait HttpEndpointService extends HttpService {
  lazy val log = LogManager.getLogger(this.getClass.getName)

  val statusResponse = "Kamanja Http Endpoint service running since " + Metrics.startTime
  implicit val formats = Serialization.formats(NoTypeHints) + new MetricsSerializer
  
  implicit val customRejectionHandler = RejectionHandler {
    case rejections => mapHttpResponse {
      response =>
        response.withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),
          "{\"code\": 0, \"error\": \""+response.entity.asString+"\"}"))
    } {
      RejectionHandler.Default(rejections)
    }
  }

  val endpointRoute = respondWithMediaType(MediaTypes.`application/json`) {

    get {
      pathSingleSlash {    
        complete(write(Map("status" -> "online",
            "message" -> statusResponse,
            "services" -> Configuration.values.services.keys.mkString(","))))
      } ~
      pathPrefix("status") {
        pathEndOrSingleSlash {
          complete(write(Map("status" -> "online", "message" -> statusResponse)))
        }
      } ~ 
      pathPrefix("stats") {
        pathEndOrSingleSlash {
          complete(write(Metrics.totals))
        }
      } 
    } ~
    pathPrefix(Configuration.values.services) { svc =>
      pathEndOrSingleSlash {
        get {
          complete(write(Map("name" -> svc.name, "schema" -> svc.schema, "message" -> svc.message)))
        } ~
        post {
          parameter("format" ? "keyvalue") {format =>
            entity(as[String]) { message => 
              ctx: RequestContext => processMessage(ctx, svc, format, message) 
            }
          }
        }
      } ~
      pathPrefix("avro") {
        pathEndOrSingleSlash {
          get {
            complete(svc.avroSchema.toString())
          }
        }
      } ~
      pathPrefix("status") {
        pathEndOrSingleSlash {
          get {
            complete(write(Map("status" -> "online", "message" -> statusResponse)))
          }
        }
      } ~
      pathPrefix("stats") {
        pathEndOrSingleSlash {
          get {
            complete(write(Metrics.serviceMetrics(svc.name)))
          }
        }
      }
    }
    
  }
  
  def processMessage(ctx: RequestContext, svc: Service, format: String, message: String) = {
    val processor = new MessageProcessor(svc)
    val m = Metrics.serviceMetrics(svc.name)
    try {
      val result = processor.process(format, message)
      m.acceptedRequests.incrementAndGet()
      m.acceptedMessages.addAndGet(result)
      //ctx.complete("{ \"count\": " + result + "}")
      ctx.complete(write(Map("count" -> result)))
    } catch {
      case e: FormatException => {
        m.errorRequests.incrementAndGet()
        log.error("Error processing request: ", e)
        //ctx.complete(400, "{\"code\": " + (1000+e.code) + ", \"error\": \"" + e.getMessage + "\"}")
        ctx.complete(400, write(Map("code" -> (1000+e.code), "error" -> JString(e.getMessage))))
      }
      case e: DataException => {
        m.errorRequests.incrementAndGet()
        log.error("Error processing request: ", e)
        //ctx.complete(400, "{\"code\": " + (2000+e.code) + ", \"error\": \"" + e.getMessage + "\"}")
        ctx.complete(400, write(Map("code" -> (2000+e.code), "error" -> JString(e.getMessage))))
      }
      case e: ServerException => {
        m.errorRequests.incrementAndGet()
        log.error("Error processing request: ", e)
        //ctx.complete(400, "{\"code\": " + (3000+e.code) + ", \"error\": \"" + e.getMessage + "\"}")
        ctx.complete(400, write(Map("code" -> (3000+e.code), "error" -> JString(e.getMessage))))
      }
      case e: Exception => {
        m.errorRequests.incrementAndGet()
        log.error("Error processing request: ", e)
        //ctx.complete(400, "{\"code\": 3999, \"error\": \"" + e.getMessage + "\"}")
        ctx.complete(400, write(Map("code" -> 3999, "error" -> JString(e.getMessage))))
      }
    }
  }
    
  
}