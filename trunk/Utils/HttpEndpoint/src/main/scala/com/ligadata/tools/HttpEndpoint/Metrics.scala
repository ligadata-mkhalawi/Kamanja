package com.ligadata.tools.HttpEndpoint

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicLong
import org.json4s._

object Metrics {
  var startTime: String = _
  var serviceMetrics: scala.collection.mutable.Map[String, Metrics] = _
  
  def reset() = {
    startTime = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z").format(new Date(System.currentTimeMillis()))
    serviceMetrics = new scala.collection.mutable.HashMap[String, Metrics]()      
    Configuration.values.services.keys.foreach( s => serviceMetrics(s) = new Metrics() )
  }
  
  def totals(): Metrics = {
    var ar: Long = 0
    var am: Long = 0
    var er: Long = 0
    serviceMetrics.values.foreach( m => {
      ar += m.acceptedRequests.get
      am += m.acceptedMessages.get
      er += m.errorRequests.get
    })
    
    new Metrics(ar, am, er)
  }
}

class Metrics(ar: Long, am: Long, er: Long) {
  val acceptedRequests: AtomicLong = new AtomicLong(ar)
  val acceptedMessages: AtomicLong = new AtomicLong(am)
  val errorRequests: AtomicLong = new AtomicLong(er)
  
  def this() = this(0, 0, 0)
}

class MetricsSerializer extends CustomSerializer[Metrics](format => (
  {
    case JObject(JField("acceptedRequests", JInt(ar)) :: JField("acceptedMessages", JInt(am)) :: JField("errorRequests", JInt(er)) :: Nil) =>
          new Metrics(ar.longValue, am.longValue, er.longValue)    
  },
  {
    case x: Metrics =>
      JObject(JField("acceptedRequests", JInt(BigInt(x.acceptedRequests.get))) ::
        JField("acceptedMessages", JInt(BigInt(x.acceptedMessages.get))) :: 
        JField("errorRequests", JInt(BigInt(x.errorRequests.get))) :: Nil)
  }
))
