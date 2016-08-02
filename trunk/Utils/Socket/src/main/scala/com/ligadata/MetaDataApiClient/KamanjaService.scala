package com.ligadata.MetaDataApiClient

import com.ligadata.MetadataAPI.StartMetadataAPI.MetadataAPIManager
import com.ligadata.metadataapiservice.APIService
import com.ligadata.metadataapiservice
import com.ligadata.KamanjaManager._
import org.apache.logging.log4j.LogManager
import scala.actors.threadpool.{TimeUnit, Executors}

/**
  * Created by Yasser on 5/24/2016.
  */
object KamanjaService {

  private val LOG = LogManager.getLogger(getClass)
  private val executor = Executors.newFixedThreadPool(3)


  def main(args : Array[String]) : Unit = {

    scala.sys.addShutdownHook({
      if (!KamanjaConfiguration.shutdown) {
        LOG.warn("KAMANJA-Service: Received shutdown request")
        KamanjaConfiguration.shutdown = true // Setting the global shutdown

        shutdown()
      }
    })


    //run engine
    val kamanjaEngineThread = new Runnable() {
      override def run(): Unit = {

        //TODO : use a better way to wait until cluster config is uploaded
        Thread.sleep(60 * 1000)

        LOG.warn("starting KAMANJA-MANAGER")

        //TODO : pass engine config file path (hardcoded temporarily)
        val engineCfg = Array[String]("--config", "/opt/Kamanja_1.5.0.Test/Kamanja-1.4.1_2.11/config/Engine1Config.properties")
        LOG.warn("KamanjaService - main() : engineCfg.length="+engineCfg.length)
        KamanjaManager.startKamanjaManager(engineCfg)
      }
    }
    executor.execute(kamanjaEngineThread)

    //run metadata api
    val metadataApiThread = new Runnable() {
      override def run(): Unit = {
        LOG.warn("starting Metadata API")

        //TODO : pass config file path, and port (hardcoded temporarily)
        val msgApiCfg = Array[String]("--config", "/opt/Kamanja_1.5.0.Test/Kamanja-1.4.1_2.11/config/MetadataAPIConfig.properties",
          "--port", "444")
        MetadataAPIManager.start(msgApiCfg)
      }
    }
    executor.execute(metadataApiThread)

    //run rest api
    val apiServiceThread = new Runnable() {
      override def run(): Unit = {
        LOG.warn("starting Rest APIService")

        //TODO : pass config file path (hardcoded temporarily)
        val msgApiCfg = Array[String]("--config", "/opt/Kamanja_1.5.0.Test/Kamanja-1.4.1_2.11/config/MetadataAPIConfig.properties")

        APIService.startAPISevrice(msgApiCfg)
      }
    }
    executor.execute(apiServiceThread)
  }

  def shutdown(): Unit ={

    APIService.shutdownAPISevrice()
    MetadataAPIManager.shutdown()

    executor.shutdown()
    try {
      if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
        executor.shutdownNow()
        if (!executor.awaitTermination(1, TimeUnit.SECONDS))
          LOG.warn("Pool did not terminate ")
      }
    } catch  {
      case ie : InterruptedException =>
        LOG.warn("InterruptedException", ie)
        executor.shutdownNow()
        Thread.currentThread().interrupt()

      case th : Throwable =>
        LOG.warn("Throwable", th)

    }
  }
}