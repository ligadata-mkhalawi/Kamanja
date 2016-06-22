package com.ligadata.tools.HttpEndpoint

import java.util.{ Observer, Observable }
import akka.event.Logging
import akka.actor._
import akka.actor.ActorDSL._
import akka.io.IO
import scala.concurrent.duration._
import spray.can.Http
import org.apache.logging.log4j.{ Logger, LogManager }

object Stop

object Main {
  def main(args: Array[String]): Unit = {

    if (args.size == 0 || args.size > 1) {
      println("Missing configuration file for HttpEndpoint service")
      println("Usage: com.ligadata.tools.HttpEndpoint.Main <configuration_file_name>")
      sys.exit(1)
    }

    try {
      Configuration.load(args(0))
    } catch {
      case e: Exception => {
        println("Error loading configuration file: " + e.getMessage)
        e.printStackTrace()
        sys.exit(1)
      }
    }

    Metrics.reset()

    // create an actor system for application
    val system = ActorSystem("HttpEndpointActorSystem")
    
    // create and start server
    new Server(system).start()
  }
}

class Server(val system: ActorSystem) extends Observer {

  lazy val logger = LogManager.getLogger(this.getClass.getName)

  var sh: SignalHandler = null
  try {
    sh = new SignalHandler()
    sh.addObserver(this)
    sh.handleSignal("TERM")
    sh.handleSignal("INT")
    sh.handleSignal("ABRT")
  } catch {
    case e: Throwable => {
      logger.error("Failed to add signal handler.", e)
    }
  }

  val serverManager = system.actorOf(Props[ServerManagerActor], "ServerManager")

  def start() = {
    // create and start rest service actor
    val endpointService = system.actorOf(Props[HttpEndpointServiceActor], "HttpEndpointService")

    // start HTTP server with rest service actor as a handler
    IO(Http)(system).tell(Http.Bind(endpointService, Configuration.values.host, Configuration.values.port), serverManager)
  }

  def stop() = {
    serverManager ! Stop
  }
  
  def update(o: Observable, arg: AnyRef): Unit = {
    val sig = arg.toString
    logger.debug("Received signal: " + sig)
    if (sig.compareToIgnoreCase("SIGTERM") == 0 || sig.compareToIgnoreCase("SIGINT") == 0 || sig.compareToIgnoreCase("SIGABRT") == 0) {
      logger.warn("Received " + sig + " signal. Shutting down the process")
      stop()
    }
  }

}

class ServerManagerActor extends Actor {
  lazy val logger = LogManager.getLogger(this.getClass.getName)
  
  var httpListener: ActorRef = _

  def receive = {
    case b: Http.Bound => {
      logger.info("HttpEndpoint started and bound to " + b.toString)
      httpListener = sender()
      context.watch(httpListener)
    }
    case f: Http.CommandFailed => {
      logger.info("HttpEndpoint failed " + f.toString)
      context.system.shutdown()
    }
    case Stop => {
      logger.info("Stopping HttpEndpoint with a grace period of 10 secs")
      httpListener ! Http.Unbind(10.second)
    }
    case Http.Unbound => {
      logger.info("HttpEndpoint unbound and stopped accepting new requests")
    }
    case Terminated(t) if t.equals(httpListener) => {
      logger.info("HttpEndpoint stopped")
      KafkaPublisher.close()
      context.system.shutdown()
    }
    case all => println("ServerManager Received unhandled message " + all.toString)
  }
}

class SignalHandler extends Observable with sun.misc.SignalHandler {
  def handleSignal(signalName: String) {
    sun.misc.Signal.handle(new sun.misc.Signal(signalName), this)
  }
  def handle(signal: sun.misc.Signal) {
    setChanged()
    notifyObservers(signal)
  }
}
