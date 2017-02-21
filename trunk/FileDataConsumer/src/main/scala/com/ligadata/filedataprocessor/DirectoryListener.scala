package com.ligadata.filedataprocessor

import java.io.{ File, IOException, PrintWriter }
import java.nio.file.{ FileSystems, Path }
import java.util.{ Observable, Observer }

import com.ligadata.Exceptions.{ InternalErrorException, MissingArgumentException }
import org.apache.logging.log4j.{ LogManager, Logger }
import com.ligadata.KamanjaVersion.KamanjaVersion

import scala.collection.mutable.ArrayBuffer
import scala.actors.threadpool.ExecutorService
import com.ligadata.VelocityMetrics._

/**
 * Created by danielkozin on 9/24/15.
 */
class DirectoryListener {

}

object LocationWatcher extends Observer {

  private class SignalHandler extends Observable with sun.misc.SignalHandler {
    def handleSignal(signalName: String) {
      sun.misc.Signal.handle(new sun.misc.Signal(signalName), this)
    }

    def handle(signal: sun.misc.Signal) {
      setChanged()
      notifyObservers(signal)
    }
  }

  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  var shutdown = false

  def update(o: Observable, arg: AnyRef): Unit = {
    val sig = arg.toString
    logger.debug("Received signal: " + sig)
    if (sig.compareToIgnoreCase("SIGTERM") == 0 || sig.compareToIgnoreCase("SIGINT") == 0 || sig.compareToIgnoreCase("SIGABRT") == 0) {
      logger.warn("Got " + sig + " signal. Shutting down the process")
      shutdown = true
    }
  }

  def main(args: Array[String]): Unit = {

    if (args.size == 0 || args.size > 1) {
      logger.error("Smart File Consumer requires a configuration file as its argument")
      return
    }

    if (args(0).equalsIgnoreCase("--version")) {
      KamanjaVersion.print
      return
    }

    //Read the velocity metrics config

    // Read the config and figure out how many consumers to start
    var config = args(0)
    var properties = scala.collection.mutable.Map[String, String]()

    val lines = scala.io.Source.fromFile(config).getLines.toList
    lines.foreach(line => {
      //Handle empty lines also 
      if (!line.isEmpty() && !line.startsWith("#")) {
        val lProp = line.split("=")
        try {
          logger.info("SMART FILE CONSUMER " + lProp(0) + " = " + lProp(1))
          properties(lProp(0)) = lProp(1)
        } catch {
          case iobe: IndexOutOfBoundsException => {
            logger.error("SMART FILE CONSUMER: Invalid format in the configuration file " + config, iobe)
            logger.error("SMART FILE CONSUMER: unable to determine the value for property " + lProp(0), iobe)
            return
          }
          case e: Throwable => {
            logger.error("SMART FILE CONSUMER: Invalid format in the configuration file " + config)
            logger.error("SMART FILE CONSUMER: unable to determine the value for property " + lProp(0), e)
            return
          }
        }
      }
    })
    //Velocity Metrics Properties
    // var rotationtimeinsecs = 30
    var rotationtimeinsecs = properties.getOrElse(SmartFileAdapterConstants.VM_ROTATIONTIMEINSECS, "30")
    var emittimeinsecs = properties.getOrElse(SmartFileAdapterConstants.VM_EMITTIMEINSECS, "15")
    var velocitymetricsInfo = properties.getOrElse(SmartFileAdapterConstants.VELOCITYMETRICSINFO, null)
    var nodeId = properties.getOrElse(SmartFileAdapterConstants.NODE_ID_PREFIX, null)
    var vmCategory = properties.getOrElse(SmartFileAdapterConstants.VM_CATEGORY, null)
    var vmComponentName = properties.getOrElse(SmartFileAdapterConstants.VM_COMPONENTNAME, null)
    logger.info("rotationtimeinsecs " + rotationtimeinsecs)
    logger.info("emittimeinsecs " + emittimeinsecs)
    logger.info("velocitymetricsInfo " + velocitymetricsInfo)
    logger.info("nodeId " + nodeId)
    logger.info("vmCategory " + vmCategory)
    logger.info("vmComponentName " + vmComponentName)

    var VMFactory: VelocityMetricsFactoryInterface = null
    if (velocitymetricsInfo != null && velocitymetricsInfo.trim.length() > 0) {

      // create factory here
      var vm = new VelocityMetricsInfo();
      FileProcessor.vm = vm
      VMFactory = VelocityMetricsInfo.getVMFactory(rotationtimeinsecs.toInt, emittimeinsecs.toInt)
      FileProcessor.VMFactory = VMFactory
      val msgVMInstances = vm.getMsgVelocityInstances(VMFactory, vmCategory, vmComponentName, velocitymetricsInfo, nodeId)
      logger.warn("msgVMInstances length" + msgVMInstances.length)
      FileProcessor.msgVMInstances = msgVMInstances
      val fileVMInstances = vm.getFileVelocityInstances(VMFactory, vmCategory, vmComponentName, velocitymetricsInfo, nodeId)
      FileProcessor.fileVMInstances = fileVMInstances
      logger.warn("fileVMInstances length" + fileVMInstances.length)
      var kafkaVelocityMetrics = new KafkaVelocityMetrics(properties)

      VMFactory.addEmitListener(kafkaVelocityMetrics)
    }
    // FileConsumer is a special case we need to default to 1, but also have it present in the properties since
    // it is used later for memory managemnt
    var numberOfProcessorsRaw = properties.getOrElse(SmartFileAdapterConstants.NUMBER_OF_FILE_CONSUMERS, null)
    var numberOfProcessors: Int = 1
    if (numberOfProcessorsRaw == null) {
      properties(SmartFileAdapterConstants.NUMBER_OF_FILE_CONSUMERS) = "1"
      logger.info("SMART FILE CONSUMER: Defaulting the number of file consumers to 1")
    } else {
      numberOfProcessors = numberOfProcessorsRaw.toInt
    }

    //var path: Path= null
    //Create an array of paths
    var path = new ArrayBuffer[Path]()

    try {
      val dirName = properties.getOrElse(SmartFileAdapterConstants.DIRECTORY_TO_WATCH, null)
      if (dirName == null) {
        logger.error("SMART FILE CONSUMER: Directory to watch is missing, must be specified")
        return
      }

      //path = FileSystems.getDefault().getPath(dirName)
      var p: Int = 0;
      for (x <- dirName.split(System.getProperty("path.separator"))) {
        path += FileSystems.getDefault().getPath(x)
      }

    } catch {
      case e: IOException => {
        logger.error("Unable to find the directory to watch", e)
        return
      }
      case e: Throwable => {
        logger.error("Unable to find the directory to watch", e)
        return
      }
    }

    for (dir <- path)
      logger.info("SMART FILE CONSUMER: Starting " + numberOfProcessors + " file consumers, reading from " + dir)

    var processors: Array[FileProcessor] = new Array[FileProcessor](numberOfProcessors)

    try {
      for (i <- 0 until numberOfProcessors) {
        try {
          val processor = new FileProcessor(path, i + 1)
          processor.init(properties)
          processors(i) = processor
        } catch {
          case e: Exception => {
            logger.error("Failure", e)
            return
          }
          case e: Throwable => {
            logger.error("Failure", e)
            return
          }
        }
      }
    } catch {
      case e: Exception => {
        logger.error("SMART FILE CONSUMER:  ERROR in starting SMART FILE CONSUMER ", e)
        return
      }
      case e: Throwable => {
        logger.error("SMART FILE CONSUMER:  ERROR in starting SMART FILE CONSUMER ", e)
        return
      }
    }

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

    var watchThreads: ExecutorService = scala.actors.threadpool.Executors.newFixedThreadPool(numberOfProcessors + 1)
    FileProcessor.PriorityNodeSetup(properties)

    while (!shutdown) {
      val curIsThisNodeToProcess = FileProcessor.pcbw.IsThisNodeToProcess();
      if (curIsThisNodeToProcess) {
        if (!FileProcessor.prevIsThisNodeToProcess) {
          // status flipped from false to true
          FileProcessor.AcquireLock();
          // Cleanup all files from buffered queue & enqued files
          FileProcessor.removeBufferedFilesAndEnqedFiles
          FileProcessor.isThisNodeReadyToProcess = true
          FileProcessor.prevIsThisNodeToProcess = curIsThisNodeToProcess;

          try {
            watchThreads = scala.actors.threadpool.Executors.newFixedThreadPool(numberOfProcessors + 1)
            for (i <- 0 until numberOfProcessors) {
              try {
                watchThreads.execute(new FileProcessorThread(processors(i)))
              } catch {
                case e: Exception => {
                  logger.error("Failure", e)
                }
                case e: Throwable => {
                  logger.error("Failure", e)
                }
              }
            }
          } catch {
            case e: Exception => {
              logger.error("SMART FILE CONSUMER:  ERROR in starting SMART FILE CONSUMER ", e)
              return
            }
            case e: Throwable => {
              logger.error("SMART FILE CONSUMER:  ERROR in starting SMART FILE CONSUMER ", e)
              return
            }
          }
        }
      } else {
        FileProcessor.isThisNodeReadyToProcess = false
        if (FileProcessor.prevIsThisNodeToProcess) {
          watchThreads.shutdown()
          // 1. Wait for all threads to come out
          var cntr = 0
          while (!watchThreads.isTerminated) {
            try {
              Thread.sleep(1000)
            } catch {
              case e: Throwable => {}
            }
            cntr += 1
            if (cntr % 30 == 0)
              logger.warn("SMART FILE CONSUMER:  Still waiting for threads to come out to release lock. Current counter:" + cntr)
          }
          // Cleanup all files from buffered queue & enqued files
          FileProcessor.removeBufferedFilesAndEnqedFiles
          // status flipped from true to false
          FileProcessor.ReleaseLock();
          FileProcessor.prevIsThisNodeToProcess = curIsThisNodeToProcess;
        }
      }

      logger.warn("End in Directory Listener");
      try {
        Thread.sleep(1000)
      } catch {
        case e: Throwable => {}
      }

    }

    // Release lock in case if it is holding
    if (FileProcessor.prevIsThisNodeToProcess) {
      watchThreads.shutdownNow()
      FileProcessor.ReleaseLock();
    }

    val pcbw = FileProcessor.pcbw
    FileProcessor.pcbw = null
    if (pcbw != null)
      pcbw.Shutdown

    for (i <- 0 until numberOfProcessors) {
      try {
        processors(i).shutdown
      } catch {
        case e: Exception => {
          logger.error("Failure", e)
        }
        case e: Throwable => {
          logger.error("Failure", e)
        }
      }
    }

    logger.warn("Waiting for final shutdown")
    try {
      Thread.sleep(1000)
    } catch {
      case e: Throwable => {}
    }
    VMFactory.shutdown()
  }
}
