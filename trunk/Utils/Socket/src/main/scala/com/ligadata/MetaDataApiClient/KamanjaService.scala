package com.ligadata.MetaDataApiClient

import com.ligadata.MetadataAPI.StartMetadataAPI.MetadataAPIManager
import com.ligadata.metadataapiservice.APIService
import com.ligadata.metadataapiservice
import com.ligadata.KamanjaManager._
import org.apache.logging.log4j.LogManager
import scala.actors.threadpool.{TimeUnit, Executors}
//import sys.process._
import java.io.File
import scala.io.Source
import com.ligadata.kamanja.metadata.MdMgr._
import com.ligadata.MetadataAPI.MetadataAPIImpl
/**
  * Created by Yasser on 5/24/2016.
  */
object KamanjaService {

  private val LOG = LogManager.getLogger(getClass)
  private val executor = Executors.newFixedThreadPool(3)


  def main(args : Array[String]) : Unit = {
    println("--------------------------") 
val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
        val filePath = "/opt/KAM/Kamanja-1.5.3_2.11/config/MetadataAPIConfig.properties"
        val config = Source.fromFile(filePath).mkString
	getMetadataAPI.InitMdMgrFromBootStrap(filePath, false)
	val mdMgr = GetMdMgr
	val tenatInfo = mdMgr.GetAllTenantInfos
    println("--------------------------")
    if (tenatInfo.isEmpty) {
      LOG.warn("Uploading cluster config")
//      val uploadClusterConfig="sudo kamanja upload cluster config /opt/KAM/Kamanja-1.5.3_2.11/config/ClusterConfig.json".!!
//      println(uploadClusterConfig)
        //LOG.warn("Returned value="+uploadClusterConfig)
	val filePath = new File("/opt/KAM/Kamanja-1.5.3_2.11/config/ClusterConfig.json")
 	val cfgDef = Source.fromFile(filePath).mkString
      // getMetadataAPI.shutdown
//    val mdMgr = GetMdMgr
 //   val tenatInfo = mdMgr.GetAllTenantInfos

//	val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
//        getMetadataAPI.InitMdMgrFromBootStrap(cfgDef, false)
//    val mdMgr = GetMdMgr
//    val tenatInfo = mdMgr.GetAllTenantInfos

      val response=MetadataAPIImpl.UploadConfig(cfgDef, None, "configuration")
    }
    else {
      LOG.warn("cluster config already uploaded")
    }

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
      //  Thread.sleep(60 * 1000)

        LOG.warn("starting KAMANJA-MANAGER")

        //TODO : pass engine config file path (hardcoded temporarily)
        val engineCfg = Array[String]("--config", "/opt/KAM/Kamanja-1.5.3_2.11/config/Engine1Config.properties")
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
        val msgApiCfg = Array[String]("--config", "/opt/KAM/Kamanja-1.5.3_2.11/config/MetadataAPIConfig.properties",
          "--port", "9000")
        MetadataAPIManager.start(msgApiCfg)
      }
    }
    executor.execute(metadataApiThread)

    //run rest api
    val apiServiceThread = new Runnable() {
      override def run(): Unit = {
        LOG.warn("starting Rest APIService")

        //TODO : pass config file path (hardcoded temporarily)
        val msgApiCfg = Array[String]("--config", "/opt/KAM/Kamanja-1.5.3_2.11/config/MetadataAPIConfig.properties")

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
