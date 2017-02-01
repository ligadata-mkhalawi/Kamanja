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
import java.util.Date
import java.util.Calendar
import java.text.SimpleDateFormat

case class VelocityMetricsCfg(var KeyType: String, var Keys: Array[String], var ValidMsgTypes: Array[String], var TimeIntervalInSecs: Int, var MetricsTime: MetricsTime)
case class MetricsTime(var MType: String, var Field: String, var Format: String)

class VelocityMetricsInfo {
  private val LOG = LogManager.getLogger(getClass);

  val metricsbymsgtype = "metricsbymsgtype"
  val metricsbymsgkeys = "metricsbymsgkeys"
  val metricsbyfilename = "metricsbyfilename"
  val velocityStatsInfo = "VelocityStatsInfo"
  val velocitymetrics = "VelocityMetrics"

  def validateMsgType(msgFullName: String, validMsgTyps: Array[String]): Boolean = {

    if (validMsgTyps != null && validMsgTyps.length > 0) {
      for (i <- 0 until validMsgTyps.length) {
        if (validMsgTyps(i).equalsIgnoreCase(msgFullName)) return true
      }
    }

    return false
  }

  /*
   * Increment the velocity metrics - get the VelocityMetricsInstance Factory and call increment for metrics by msgType and metrics by msg keys
   */

  def incrementVelocityMetrics(VMFactory: VelocityMetricsFactoryInterface, componentName: String, nodeId: String, message: ContainerInterface, adapConfig: AdapterConfiguration, processed: Boolean): Unit = {
    LOG.info("*********Start Increment********************")
    LOG.info("*********componentNamet********************" + componentName)
    LOG.info("*********nodeId********************" + nodeId)
    LOG.info("*********message.FullName()********************" + message.FullName())
    try {
      val vm = getIAVelocityMetricsInstances(VMFactory, nodeId, adapConfig, componentName)
      var Key: String = ""

      for (i <- 0 until vm.size) {
        val keyType = vm(i)._2.KeyType
        val validMsgType = vm(i)._2.ValidMsgTypes
        if (keyType.equalsIgnoreCase(metricsbymsgtype) || keyType.equalsIgnoreCase(metricsbymsgkeys)) {
          LOG.info("**************metricsbymsgtype*************")
          LOG.info("**************validateMsgType(message.FullName(), validMsgType)*************" + validateMsgType(message.FullName(), validMsgType))
          LOG.info("**************validMsgType size*************" + validMsgType.size)
          LOG.info("**************validMsgType List*************" + validMsgType.toList)
          LOG.info("**************validMsgType keyType*************" + keyType)

          if (validMsgType != null && validMsgType.size > 0 && validateMsgType(message.FullName(), validMsgType)) {
            LOG.info("**************validateMsgType(message.FullName(), validMsgType)*************")

            var metricsTime: Long = System.currentTimeMillis()
            val metricsType = vm(i)._2.MetricsTime.MType
            if (metricsType.equalsIgnoreCase("field")) {
              val field = vm(i)._2.MetricsTime.Field
              val frmat = vm(i)._2.MetricsTime.Format
              if (field != null && field.trim().size > 0) {
                val fieldVal = message.getOrElse(field, null)
                metricsTime = extractTime(field, frmat)
              }
            }
            val keys = vm(i)._2.Keys
            var msgkeys = new Array[String](keys.length)
            if (keys != null && keys.length > 0) {
              LOG.info("**************Keys length *************" + keys.length)
              for (j <- 0 until keys.length) {
                LOG.info("**************keys(j)*************" + keys(j))

                msgkeys(j) = message.getOrElse(keys(j).trim, "").toString
                LOG.info("**************keys(j)*************" + keys(j))
                LOG.info("**************msgkeys(j)*************" + msgkeys(j))
              }
              LOG.info("**************validMsgType keys*************" + msgkeys.length)
            }

            if (msgkeys != null && msgkeys.length > 0) {
              Key = keyType + "_" + message.Name().toLowerCase() + "_" + msgkeys.mkString("_")
            } else
              Key = keyType + "_" + message.Name().toLowerCase()

            if (processed) {
              vm(i)._1.increment(metricsTime, Key, System.currentTimeMillis(), true, false)
              LOG.info("*************metricsTime****************" + metricsTime)
              LOG.info("*************Key****************" + Key)
            } else {
              vm(i)._1.increment(metricsTime, Key, System.currentTimeMillis(), false, true)
              LOG.info("*************metricsTime****************" + metricsTime)
              LOG.info("*************Key****************" + Key)

            }
          }
        }
      }
      LOG.info("**********End Increment*******************")
    } catch {
      case e: Exception => LOG.error("increment Velocity Metrics " + e.getMessage)
    }

  }

  /*
   * Increment the velocity metrics - get the VelocityMetricsInstance Factory and call increment of velocity metrics by file name
   */

  def incrementFileVelocityMetrics(VMFactory: VelocityMetricsFactoryInterface, componentName: String, fileName: String, nodeId: String, adapConfig: AdapterConfiguration) = {
    LOG.info("*********Start Increment********************")
    try {
      val vm = getIAVelocityMetricsInstances(VMFactory, nodeId, adapConfig, componentName)      
      for (i <- 0 until vm.size) {
        var metricsTime: Long = System.currentTimeMillis() //0L
        val metricsType = vm(i)._2.MetricsTime.MType
        metricsTime = System.currentTimeMillis()

        val keyType = vm(i)._2.KeyType
        var Key: String = keyType+"_"+fileName
        var msgkeys = Array[String]()
        if (keyType.equalsIgnoreCase(metricsbyfilename)) {
          vm(i)._1.increment(metricsTime, Key, System.currentTimeMillis(), true, false)
        }
        LOG.info("**********End Increment*******************")
      }
    } catch {
      case e: Exception => LOG.error("increment Velocity Metrics " + e.getMessage)
    }

  }

  /*
   * Create the VelocityMetricsInbstances for the VelocityMetricsInfo key types
   */

  private def getIAVelocityMetricsInstances(VMFactory: VelocityMetricsFactoryInterface, nodeId: String, adapConfig: AdapterConfiguration, componentName: String): Array[(VelocityMetricsInstanceInterface, VelocityMetricsCfg)] = {

    LOG.info("*****************Start getVelocityMetricsInstances in InputOutputAdapterInfo **********************")

    var velocityMetricsInstBuf = new scala.collection.mutable.ArrayBuffer[(VelocityMetricsInstanceInterface, VelocityMetricsCfg)]()
    try {
      val vmetrics = getVelocityMetricsConfig(adapConfig.fullAdapterConfig)
      println("vmetrics.length  " + vmetrics.length)
      // var nodeId: String = "1"
      // val nid = nodeContext.getEnvCtxt().getNodeId()
      //  if(nodeContext.getEnvCtxt().getNodeId() != null) nodeId = nodeContext.getEnvCtxt().getNodeId()
      val componentNam = componentName
      val counterNames = Array("processed", "exception")
      vmetrics.foreach(vm => {

        val intervalRoundingInSecs = vm.TimeIntervalInSecs
        var vmInstance = VMFactory.GetVelocityMetricsInstance(nodeId, componentName, intervalRoundingInSecs, counterNames)
        if (vmInstance == null) LOG.info("VMInstance is null")
        val v = (vmInstance, vm)
        velocityMetricsInstBuf += v

        println("vm.TimeIntervalInSecs  " + vm.TimeIntervalInSecs)
        println("vm.Keys  " + vm.Keys.toList)
        println("vm.MetricsTime.MType  " + vm.MetricsTime.MType)
        println("nodeId " + nodeId)
        println("componentName  " + componentName)
        println("intervalRoundingInMs  " + intervalRoundingInSecs)
        println("counterNames  " + counterNames.toList)

      })
      LOG.info("*****************velocityMetricsInstBuf **********************" + velocityMetricsInstBuf.size)

      LOG.info("*****************End getVelocityMetricsInstances in InputOutputAdapterInfo **********************")
    } catch {
      case e: Exception => LOG.error("increment Velocity Metrics " + e.getMessage)
    }

    velocityMetricsInstBuf.toArray
  }

  /*
   * Parse the adapter json and create the Array of VelocityMetricsCfg for the metrics keys types
   */
  def getVelocityMetricsConfig(fullAdapterConfig: String): Array[VelocityMetricsCfg] = {
    if (fullAdapterConfig == null && fullAdapterConfig.trim().size == 0) {
      println("======================fullAdapterConfig is null===========")
      return null
    }
    parseVelocityMetrics(fullAdapterConfig)
  }

  /*
   * Create VelocityMetricsFactoryInterface 
   */
  def getVMFactory(nodeContext: NodeContext): VelocityMetricsFactoryInterface = {
    var rotationTimeInSecs: Int = 30
    var emitTimeInSecs: Int = 15
    try {
      val clusterCfg = MdMgr.GetMdMgr.GetClusterCfg(nodeContext.getEnvCtxt().getClusterId())
      println("=============== clusterCfg  Keys" + clusterCfg.cfgMap.keySet.toList)
      val velocityStats = clusterCfg.cfgMap.getOrElse(velocityStatsInfo, null)
      if (velocityStats != null) {
        val vstatsJson = parse(velocityStats)
        if (vstatsJson == null || vstatsJson.values == null) {
          LOG.warn("Failed to parse velocityStstsInfo JSON configuration string:" + vstatsJson)
          throw new Exception("Failed to parse velocityStstsInfo JSON configuration string:" + vstatsJson)

        }
        val vstats = vstatsJson.values.asInstanceOf[Map[String, String]]
        if (vstats.contains("RotationTimeInSecs")) {
          LOG.info("VelocityMetrics Stats Info 11111111111111111");
          rotationTimeInSecs = vstats.getOrElse("RotationTimeInSecs", 30).asInstanceOf[scala.math.BigInt].toInt
        }
        if (vstats.contains("EmitTimeInSecs")) {
          LOG.info("VelocityMetrics Stats Info 2222222222222");
          emitTimeInSecs = vstats.getOrElse("EmitTimeInSecs", 15).asInstanceOf[scala.math.BigInt].toInt
        }

        LOG.info("VelocityMetrics Stats Info - RotationTimeInSecs  " + rotationTimeInSecs)
        LOG.info("VelocityMetrics Stats Info EmitTimeInSecs  " + emitTimeInSecs)
      }
    } catch {
      case e: Exception => LOG.error("VelocityMetrics Factory Instance" + e.getMessage)
    }

    VelocityMetrics.GetVelocityMetricsFactory(rotationTimeInSecs, emitTimeInSecs) //(rotationTimeInSecs, emitTimeInSecs)
  }

  /*
   * Parse the FullAdapterConfig and get the VelocityMetricsCfg
   */

  def parseVelocityMetrics(fullAdapterConfig: String): Array[VelocityMetricsCfg] = {
    var velocityMetricsBuf = new ArrayBuffer[VelocityMetricsCfg]()
    try {
      val json = parse(fullAdapterConfig)
      var parsed_json: Map[String, Any] = null

      if (json == null || json.values == null) {
        LOG.warn("Failed to parse VelocityMetrics JSON configuration string:" + json)
        throw new Exception("Failed to parse VelocityMetrics JSON configuration string:" + json)

      }
      println("*******************Start Parser Velocity Metrics ***************")
      parsed_json = json.values.asInstanceOf[Map[String, Any]]

      val velocityMetrics = parsed_json.getOrElse(velocitymetrics, null)

      if (velocityMetrics != null) {
        val velocityMetricsList = velocityMetrics.asInstanceOf[List[Map[String, Any]]]
        velocityMetricsList.foreach(vm1 => {

          if (vm1 == null)
            throw new Exception("Invalid json data")

          val vm: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map[String, Any]()
          vm1.foreach(kv => { vm(kv._1.trim().toLowerCase()) = kv._2 })

          var velocityMtrcsVar: VelocityMetricsCfg = null
          var keys = new ArrayBuffer[String]()
          var validMsgTypes = new ArrayBuffer[String]()
          var keyType: String = ""
          var timeIntervalInSecs: Int = 30
          var metricsTimeType: String = "LocalTime"
          var metricsTimeField: String = ""
          var metricsTimeFormat: String = ""

          LOG.info("vm Map 1111111111  " + vm)
          if (vm.contains(metricsbyfilename) || vm.contains(metricsbymsgtype) || (vm.contains(metricsbymsgkeys))) {
            LOG.info("1111111111")
            var vmetrics: Map[String, Any] = null
            if (vm.contains(metricsbyfilename)) {
              keyType = metricsbyfilename
              val metricsByType = vm.getOrElse(metricsbyfilename, null)
              if (metricsByType != null)
                vmetrics = metricsByType.asInstanceOf[Map[String, Any]]
            } else if (vm.contains(metricsbymsgtype)) {
              keyType = metricsbymsgtype
              val metricsByType = vm.getOrElse(metricsbymsgtype, null)
              if (metricsByType != null)
                vmetrics = metricsByType.asInstanceOf[Map[String, Any]]
            } else if (vm.contains(metricsbymsgkeys)) {
              keyType = metricsbymsgkeys

              val metricsByType = vm.getOrElse(metricsbymsgkeys, null)
              if (metricsByType != null)
                vmetrics = metricsByType.asInstanceOf[Map[String, Any]]
            }

            val metrics: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map[String, Any]()
            vmetrics.foreach(kv => { metrics(kv._1.trim().toLowerCase()) = kv._2 })

            if (metrics != null) {
              //    val mByFileName = metricsByFileName.asInstanceOf[Map[String, Any]]

              val timeIntrvlInSecs = metrics.getOrElse("timeintervalinsecs", null)
              if (timeIntrvlInSecs != null) {
                timeIntervalInSecs = timeIntervalInSecs.asInstanceOf[Int]
              }

              val keysLst = metrics.getOrElse("keys", null)
              if (keysLst != null) {
                val keysList = keysLst.asInstanceOf[List[String]]
                keysList.foreach(key => {
                  keys += key.trim()
                })
              }

              val vMsgTypes = metrics.getOrElse("validmsgtypes", null)
              if (vMsgTypes != null) {
                val validMTypes = vMsgTypes.asInstanceOf[List[String]]
                validMTypes.foreach(msgTypes => {
                  validMsgTypes += msgTypes
                })
              }

              val metricsTime = metrics.getOrElse("metricstime", null)

              if (metricsTime != null) {
                val metricsTMap = metricsTime.asInstanceOf[Map[String, String]]

                val metricsTimeMap: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map[String, String]()
                metricsTMap.foreach(kv => { metricsTimeMap(kv._1.trim().toLowerCase()) = kv._2 })

                val metricsTimeTyp = metricsTimeMap.getOrElse("metricstimetype", null).asInstanceOf[String].trim()
                if (metricsTimeTyp != null && metricsTimeTyp.size != 0)
                  metricsTimeType = metricsTimeTyp

                val field = metricsTimeMap.getOrElse("field", null)
                if (field != null) {
                  val fld = field.asInstanceOf[String].trim()
                  metricsTimeField = field
                }
                val frmt = metricsTimeMap.getOrElse("format", null)
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
        println("vm keytype " + vm.KeyType)
        println("vm key " + vm.Keys.size)
        println("vm ValidMsgTypes length " + vm.ValidMsgTypes.length)
        vm.ValidMsgTypes.foreach { msg => println("vm msg" + msg) }
        println("vm " + vm.TimeIntervalInSecs)
        println("vm " + vm.MetricsTime.MType)
        println("vm " + vm.MetricsTime.Field)
        println("vm " + vm.MetricsTime.Format)

        LOG.info("vm keytype" + vm.KeyType)
        LOG.info("vm key" + vm.Keys.size)
        LOG.info("vm " + vm.TimeIntervalInSecs)
        LOG.info("vm " + vm.MetricsTime.MType)
        LOG.info("vm " + vm.MetricsTime.Field)
        LOG.info("vm " + vm.MetricsTime.Format)
      })
      println(velocityMetrics)
      LOG.info("*******************End Parse Velocity Metrics ***************")
    } catch {
      case e: Exception => LOG.error("VelocityMetrics - Parse Velocity Metrics" + e.getMessage)
    }
    velocityMetricsBuf.toArray
  }

  private def extractTime(fieldData: String, format: String): Long = {

    var timeFormat = "epochtime"
    var formatTypes = new ArrayBuffer[String]
    formatTypes += "MM/dd/yyyy"
    formatTypes += "yyyy/mm/dd"
    formatTypes += "yyyy-mm-dd"
    formatTypes += "yyyy-MM-dd'T'HH:mm:ssz"

    if (format != null && format.trim() != "" && formatTypes.contains(format)) timeFormat = format

    if (timeFormat.compareToIgnoreCase("epochtimeInMillis") == 0)
      return fieldData.toLong

    if (timeFormat.compareToIgnoreCase("epochtimeInSeconds") == 0 || timeFormat.compareToIgnoreCase("epochtime") == 0)
      return fieldData.toLong * 1000

    // Now assuming Date partition format exists.
    val dtFormat = new SimpleDateFormat(timeFormat);
    val tm =
      if (fieldData == null || fieldData.trim.size == 0) {
        new Date(0)
      } else {
        dtFormat.parse(fieldData)
      }
    tm.getTime
  }

}