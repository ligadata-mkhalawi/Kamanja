package com.ligadata.InputOutputAdapterInfo

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import scala.io.Source;
import scala.collection.mutable.ArrayBuffer
import org.apache.logging.log4j.{ Logger, LogManager }
import com.ligadata.VelocityMetrics._
import com.ligadata.kamanja.metadata.MdMgr
import com.ligadata.KamanjaBase._

case class VelocityMetricsCfg(var KeyType: String, var Keys: Array[String], var ValidMsgTypes: Array[String], var TimeIntervalInSecs: Int, var MetricsTime: MetricsTime)
case class MetricsTime(var MType: String, var Field: String, var Format: String)

class VelocityMetricsInfo {
  private val LOG = LogManager.getLogger(getClass);

  def incrementVelocityMetrics(VMFactory: VelocityMetricsFactoryInterface, componentName: String, nodeContext: NodeContext, message: ContainerInterface, adapConfig: AdapterConfiguration, processed: Boolean) = {
    LOG.info("*********Start Increment********************")

    val vm = getIAVelocityMetricsInstances(VMFactory, nodeContext, adapConfig, componentName)
    var Key: String = ""
    for (i <- 0 until vm.size) {
      var metricsTime: Long = 0L
      val metricsType = vm(i)._2.MetricsTime.MType
      if (metricsType.equalsIgnoreCase("localtime")) {
        metricsTime = System.currentTimeMillis()
      } else if (metricsType.equalsIgnoreCase("field")) {
        val field = vm(i)._2.MetricsTime.Field
        val frmat = vm(i)._2.MetricsTime.Format
      }
      val keyType = vm(i)._2.KeyType
      var msgkeys = Array[String]()
      if (keyType.equalsIgnoreCase("metricsbymsgtype") || keyType.equalsIgnoreCase("metricsbymsgkeys")) {
        val keys = vm(i)._2.Keys
        if (keys != null && keys.length > 0) {
          for (j <- 0 until keys.length) {
            msgkeys(j) = message.getOrElse(keys(j), "").toString
          }
        }

        if (msgkeys != null && msgkeys.length > 0) {
          Key = keyType + "_" + msgkeys.mkString("_")
        }
      }
      if (processed)
        vm(i)._1.increment(metricsTime, Key, System.currentTimeMillis(), true, false)
      else
        vm(i)._1.increment(metricsTime, Key, System.currentTimeMillis(), false, true)

    }
    LOG.info("**********End Increment*******************")

  }

  def incrementFileVelocityMetrics(VMFactory: VelocityMetricsFactoryInterface, componentName: String, fileName: String, nodeContext: NodeContext, adapConfig: AdapterConfiguration) = {
    LOG.info("*********Start Increment********************")

    val vm = getIAVelocityMetricsInstances(VMFactory, nodeContext, adapConfig, componentName)
    var Key: String = ""
    for (i <- 0 until vm.size) {
      var metricsTime: Long = 0L
      val metricsType = vm(i)._2.MetricsTime.MType
      metricsTime = System.currentTimeMillis()

      val keyType = vm(i)._2.KeyType
      var msgkeys = Array[String]()
      if (keyType.equalsIgnoreCase("metricsbyfilename")) {

        vm(i)._1.increment(metricsTime, Key, System.currentTimeMillis(), true, false)

      }
      LOG.info("**********End Increment*******************")
    }
  }

  private def getIAVelocityMetricsInstances(VMFactory: VelocityMetricsFactoryInterface, nodeContext: NodeContext, adapConfig: AdapterConfiguration, componentName: String): Array[(VelocityMetricsInstanceInterface, VelocityMetricsCfg)] = {

    LOG.info("*****************Start getVelocityMetricsInstances in InputOutputAdapterInfo **********************")

    var velocityMetricsInstBuf = new scala.collection.mutable.ArrayBuffer[(VelocityMetricsInstanceInterface, VelocityMetricsCfg)]()
    val vmetrics = getVelocityMetricsConfig(adapConfig.fullAdapterConfig)
    val nodeId = nodeContext.getEnvCtxt().getNodeId()
    val componentNam = componentName
    val counterNames = Array("exception", "processed")
    vmetrics.foreach(vm => {
      val intervalRoundingInMs = vm.TimeIntervalInSecs
      var vmInstance = VMFactory.GetVelocityMetricsInstance(nodeId, componentNam, intervalRoundingInMs, counterNames)
      val v = ((vmInstance, vm))
      velocityMetricsInstBuf += v
    })
    LOG.info("*****************End getVelocityMetricsInstances in InputOutputAdapterInfo **********************")

    velocityMetricsInstBuf.toArray
  }

  def getVelocityMetricsConfig(fullAdapterConfig: String): Array[VelocityMetricsCfg] = {
    if (fullAdapterConfig == null && fullAdapterConfig.trim().size == 0) {
      println("======================fullAdapterConfig is null===========")
      return null
    }
    parseVelocityMetrics(fullAdapterConfig)
  }

  def getVMFactory(nodeContext: NodeContext): VelocityMetricsFactoryInterface = {
    var rotationTimeInSecs: Long = 36000
    var emitTimeInSecs: Long = 600
    try {
      val clusterCfg = MdMgr.GetMdMgr.GetClusterCfg(nodeContext.getEnvCtxt().getClusterId())
      val velocityStats = clusterCfg.cfgMap.getOrElse("VelocityStatsInfo", null)
      if (velocityStats != null) {
        val vstats = velocityStats.asInstanceOf[Map[String, Long]]
        if (vstats.contains("RotationTimeInSecs")) {
          val vrtime = vstats.getOrElse("RotationTimeInSecs", null)
          if (vrtime != null) rotationTimeInSecs = vrtime.asInstanceOf[Long]
        }
        if (vstats.contains("EmitTimeInSecs")) {
          val vetime = vstats.getOrElse("EmitTimeInSecs", null)
          if (vetime != null) emitTimeInSecs = vetime.asInstanceOf[Long]
        }

        LOG.info("VelocityMetrics Stats Info - RotationTimeInSecs  " + rotationTimeInSecs)
        LOG.info("VelocityMetrics Stats Info EmitTimeInSecs  " + emitTimeInSecs)
      }
    } catch {
      case e: Exception => LOG.error("ValocityMetrics Factory Instace" + e.getMessage)
    }

    VelocityMetrics.GetVelocityMetricsFactory(rotationTimeInSecs, emitTimeInSecs) //(rotationTimeInSecs, emitTimeInSecs)
  }

  def parseVelocityMetrics(fullAdapterConfig: String): Array[VelocityMetricsCfg] = {
    var velocityMetricsBuf = new ArrayBuffer[VelocityMetricsCfg]()
    val json = parse(fullAdapterConfig)
    var parsed_json: Map[String, Any] = null

    if (json == null || json.values == null) {
      LOG.warn("Failed to parse Storage JSON configuration string:" + json)
      throw new Exception("Failed to parse Storage JSON configuration string:" + json)

    }
    LOG.info("*******************Start Parser Velocity Metrics ***************")
    parsed_json = json.values.asInstanceOf[Map[String, Any]]

    val velocityMetrics = parsed_json.getOrElse("VelocityMetrics", null)

    if (velocityMetrics != null && velocityMetrics.isInstanceOf[List[Map[String, Any]]]) {
      val velocityMetricsList = velocityMetrics.asInstanceOf[List[Map[String, Any]]]
      velocityMetricsList.foreach(vm => {
        var velocityMtrcsVar: VelocityMetricsCfg = null
        var keys = new ArrayBuffer[String]()
        var validMsgTypes = new ArrayBuffer[String]()
        var keyType: String = ""
        var timeIntervalInSecs: Int = 30
        var metricsTimeType: String = "LocalTime"
        var metricsTimeField: String = ""
        var metricsTimeFormat: String = ""

        if (vm.contains("MetricsByFileName") || vm.contains("MetricsByMsgType") || (vm.contains("MetricsByMsgKeys"))) {
          var vmetrics: Map[String, Any] = null
          if (vm.contains("MetricsByFileName")) {
            keyType = "MetricsByFileName"
            val metricsByType = vm.getOrElse("MetricsByFileName", null)
            if (metricsByType != null)
              vmetrics = metricsByType.asInstanceOf[Map[String, Any]]
          } else if (vm.contains("MetricsByMsgType")) {
            keyType = "MetricsByMsgType"
            val metricsByType = vm.getOrElse("MetricsByMsgType", null)
            if (metricsByType != null)
              vmetrics = metricsByType.asInstanceOf[Map[String, Any]]
          } else if (vm.contains("MetricsByMsgKeys")) {
            keyType = "MetricsByMsgKeys"

            val metricsByType = vm.getOrElse("MetricsByMsgKeys", null)
            if (metricsByType != null)
              vmetrics = metricsByType.asInstanceOf[Map[String, Any]]
          }

          if (vmetrics != null) {
            //    val mByFileName = metricsByFileName.asInstanceOf[Map[String, Any]]

            val timeIntrvlInSecs = vmetrics.getOrElse("TimeIntervalInSecs", null)
            if (timeIntrvlInSecs != null) {
              timeIntervalInSecs = timeIntervalInSecs.asInstanceOf[Int]
            }

            val keysLst = vmetrics.getOrElse("Keys", null)
            if (keysLst != null) {
              val keysList = keysLst.asInstanceOf[List[String]]
              keysList.foreach(key => {
                keys += key.trim()
              })
            }

            val vMsgTypes = vmetrics.getOrElse("ValidMsgTypes", null)
            if (vMsgTypes != null) {
              val validMTypes = vMsgTypes.asInstanceOf[List[String]]
              validMTypes.foreach(msgTypes => {
                validMsgTypes += msgTypes
              })
            }

            val metricsTime = vmetrics.getOrElse("MetricsTime", null)

            if (metricsTime != null) {
              val metricsTimeMap = metricsTime.asInstanceOf[Map[String, String]]
              val metricsTimeTyp = metricsTimeMap.getOrElse("MetricsTimeType", null).asInstanceOf[String].trim()
              if (metricsTimeTyp != null && metricsTimeTyp.size == 0)
                metricsTimeType = metricsTimeTyp

              val field = metricsTimeMap.getOrElse("Field", null)
              if (field != null) {
                val fld = field.asInstanceOf[String].trim()
                metricsTimeField = field
              }
              val frmt = metricsTimeMap.getOrElse("Format", null)
              if (frmt != null) {
                val format = frmt.asInstanceOf[String].trim()
                if (format != null && format.size != 0)
                  metricsTimeFormat = format
              }
            }
            val mTime = new MetricsTime(metricsTimeType, metricsTimeField, metricsTimeFormat)
            println("keyType " + keyType)
            velocityMtrcsVar = new VelocityMetricsCfg(keyType, keys.toArray, validMsgTypes.toArray, timeIntervalInSecs, mTime)
          }
          velocityMetricsBuf += velocityMtrcsVar
        }
      })
    }
    velocityMetricsBuf.foreach(vm => {
      LOG.info("vm keytype" + vm.KeyType)
      LOG.info("vm key" + vm.Keys.size)
      LOG.info("vm " + vm.TimeIntervalInSecs)
      LOG.info("vm " + vm.MetricsTime.MType)
      LOG.info("vm " + vm.MetricsTime.Field)
      LOG.info("vm " + vm.MetricsTime.Format)
    })
    println(velocityMetrics)
    LOG.info("*******************End Parse Velocity Metrics ***************")

    velocityMetricsBuf.toArray
  }
}