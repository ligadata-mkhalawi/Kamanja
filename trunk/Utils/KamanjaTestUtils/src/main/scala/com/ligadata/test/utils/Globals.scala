package com.ligadata.test.utils

import com.ligadata.test.configuration.cluster.adapters.KafkaAdapterConfig

import scala.collection.mutable

object Globals extends KamanjaTestLogger {

  //var modelOutputResult: Option[String] = None
  //var modelOutputResult = new mutable.HashMap[String, String]()
  var modelOutputResult = new mutable.HashMap[String, List[String]]()

  val kamanjaTestTenant = "KamanjaTestTenant"

  def waitForOutputResults(adapterConfig: KafkaAdapterConfig, timeout: Int = 15, msgCount: Int = 1): Option[List[String]] = {
    logger.info(s"[Test Globals]: Waiting for $msgCount output results...")
    println(s"[Test Globals]: Waiting for $msgCount output results...")

    val topicName = adapterConfig.adapterSpecificConfig.topicName
    var timeoutCnt = 0
    var currentOutput: Option[List[String]] = None

    while (timeoutCnt < timeout * 100) {
      currentOutput = Globals.modelOutputResult.get(topicName)
      currentOutput match {
        case Some(output) => {
          val outputCnt: Int = output.length
          var innerCnt = 0
          if(outputCnt < msgCount) {
            logger.info(s"[Test Globals]: Found $outputCnt out of $msgCount messages. Waiting for more mesages...")
            Thread sleep 10
            timeoutCnt += 1
          }
          else if(outputCnt > msgCount) {
            Globals.modelOutputResult -= topicName
            logger.error(s"[Test Globals]: Found $outputCnt output messages but was only expecting $msgCount messages")
            return currentOutput
            //throw new Exception(s"[Test Globals]: Found $outputCnt output messages but was only excepting $msgCount messages")
          }
          else {
            logger.info(s"[Test Globals]: Found all $outputCnt output messages")
            println(s"[Test Globals]: Found all $outputCnt output messages")
            while(Globals.modelOutputResult.exists(_._1 == topicName)) {
              innerCnt += 1
              Globals.modelOutputResult = Globals.modelOutputResult - topicName
              if (innerCnt >= 30) {
                throw new Exception("[Test Globals]: Failed to remove model output results")
              }
            }
            return currentOutput
          }
        }
        case None => {
          logger.warn("[Test Globals]: No output found. Trying again...")
          Thread sleep 10
          timeoutCnt += 1
        }
      }
    }

    currentOutput match {
      case Some(x) =>
        if (x.length < msgCount) {
          logger.error(s"[Test Globals]: Timed out waiting for results. Found ${x.length} but expected $msgCount")
        }
      case None =>
        logger.error("[Test Globals]: Failed to retrieve output.")
    }
    return currentOutput
  }
}
