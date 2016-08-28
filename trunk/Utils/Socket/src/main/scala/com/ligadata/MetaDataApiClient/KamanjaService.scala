package com.ligadata.MetaDataApiClient

import com.ligadata.MetadataAPI.StartMetadataAPI.MetadataAPIManager
import com.ligadata.metadataapiservice.APIService
import com.ligadata.metadataapiservice
import com.ligadata.KamanjaManager._
import org.apache.logging.log4j.LogManager
import scala.actors.threadpool.{TimeUnit, Executors}
//import sys.process._
import java.io.File
import java.io.FileInputStream
import scala.io.Source
import com.ligadata.kamanja.metadata.MdMgr._
import com.ligadata.MetadataAPI.MetadataAPIImpl
/**
  * Created by Yasser on 5/24/2016.
  */
import java.util.Properties
object KamanjaService {

  private val LOG = LogManager.getLogger(getClass)
  private val executor = Executors.newFixedThreadPool(3)


  def main(args : Array[String]) : Unit = {
	if(args.length<1){
          LOG.error("KAMANJA-Service: Sending shutdown request due to user not providing components Properties file")
          KamanjaConfiguration.shutdown = true // Setting the global shutdown
          shutdown()
	  sys.exit(1)
	}
	 val (metadataFile:String,clusterConfigFile:String,engineConfigFile:String,port:String)=loadPropertiesFile(args(0))
	println(metadataFile+"\n"+clusterConfigFile+"\n"+engineConfigFile+"\n"+port)
          verifyClusterCfgLoaded(metadataFile,clusterConfigFile)
	


    //run engine
    val kamanjaEngineThread = new Runnable() {
      override def run(): Unit = {

        //TODO : use a better way to wait until cluster config is uploaded
      //  Thread.sleep(60 * 1000)

        LOG.warn("starting KAMANJA-MANAGER")

        //TODO : pass engine config file path (hardcoded temporarily)
        val engineCfg = Array[String]("--config", engineConfigFile)
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
        val msgApiCfg = Array[String]("--config", metadataFile,"--port",port)
        MetadataAPIManager.start(msgApiCfg)
      }
    }
    executor.execute(metadataApiThread)

    //run rest api
    val apiServiceThread = new Runnable() {
      override def run(): Unit = {
        LOG.warn("starting Rest APIService")

        //TODO : pass config file path (hardcoded temporarily)
        val msgApiCfg = Array[String]("--config", metadataFile)

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
def loadPropertiesFile(propFile:String)={
          try{
	    val prop=new Properties()
            prop.load(new FileInputStream(propFile))
            (
                (prop.getProperty("MetadataFile"),
                prop.getProperty("ClusterConfigFile"),
                prop.getProperty("EngineConfigFile"),
                prop.getProperty("Port")
            )
)
          }
          catch{ case e: Exception =>
            e.printStackTrace()
            KamanjaConfiguration.shutdown = true // Setting the global shutdown
            shutdown()

         }
}
 
def verifyClusterCfgLoaded( metadataCfgFile:String,clusterConfigFile:String)={
     val getMetadataAPI = MetadataAPIImpl.getMetadataAPI
        val filePath = "/opt/KAM/Kamanja-1.5.3_2.11/config/MetadataAPIConfig.properties"
        val config = Source.fromFile(filePath).mkString
        getMetadataAPI.InitMdMgrFromBootStrap(filePath, false)
        val mdMgr = GetMdMgr
        val tenantInfo = mdMgr.GetAllTenantInfos

        if (tenantInfo.length<2) {
          LOG.warn("Uploading cluster config")
          val ccFilePath = new File(clusterConfigFile)
          val cfgDef = Source.fromFile(ccFilePath).mkString
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
	
}
}
