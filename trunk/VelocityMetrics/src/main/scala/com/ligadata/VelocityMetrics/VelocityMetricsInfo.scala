package com.ligadata.VelocityMetrics

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

case class VelocityMetricsCfg(var KeyType: String, var Keys: Array[String], var KeyStrings: Array[String], var ValidMsgTypes: Array[String], var TimeIntervalInSecs: Int, var MetricsTime: MetricsTime)
case class MetricsTime(var MType: String, var Field: String, var Format: String)

case class InstanceRuntimeInfo(typ: String, typId: Int, instance: VelocityMetricsInstanceInterface, validMsgs: Array[String], allMsgsValid: Boolean, MsgKeys: Array[String], var KeyStrings: Array[String], isLocalTime: Boolean, timeKey: String, timeFormat: SimpleDateFormat)

object VelocityMetricsInfo {
  private val LOG = LogManager.getLogger(getClass);
  val velocityStatsInfo = "VelocityStatsInfo"
  /*
   * Create VelocityMetricsFactoryInterface 
   */
  def getVMFactory(nodeContext: NodeContext): VelocityMetricsFactoryInterface = synchronized {
    var rotationTimeInSecs: Int = 30
    var emitTimeInSecs: Int = 15
    try {
      if (nodeContext != null) {

        val clusterCfg = MdMgr.GetMdMgr.GetClusterCfg(nodeContext.getEnvCtxt().getClusterId())
        LOG.info("clusterCfg  Keys" + clusterCfg.cfgMap.keySet.toList)
        val velocityStats = clusterCfg.cfgMap.getOrElse(velocityStatsInfo, null)
        if (velocityStats != null) {
          val vstatsJson = parse(velocityStats)
          if (vstatsJson == null || vstatsJson.values == null) {
            LOG.warn("Failed to parse velocityStstsInfo JSON configuration string:" + vstatsJson)
            throw new Exception("Failed to parse velocityStstsInfo JSON configuration string:" + vstatsJson)

          }
          val vstats = vstatsJson.values.asInstanceOf[Map[String, String]]
          if (vstats.contains("RotationTimeInSecs")) {
            rotationTimeInSecs = vstats.getOrElse("RotationTimeInSecs", 30).asInstanceOf[scala.math.BigInt].toInt
          }
          if (vstats.contains("EmitTimeInSecs")) {
            emitTimeInSecs = vstats.getOrElse("EmitTimeInSecs", 15).asInstanceOf[scala.math.BigInt].toInt
          }

          LOG.info("VelocityMetrics Stats Info - RotationTimeInSecs  " + rotationTimeInSecs)
          LOG.info("VelocityMetrics Stats Info EmitTimeInSecs  " + emitTimeInSecs)
        }
      }
    } catch {
      case e: Exception => LOG.error("VelocityMetrics Factory Instance" + e.getMessage)
    }

    val existingFactory = VelocityMetrics.GetExistingVelocityMetricsFactory
    if (existingFactory != null) return existingFactory

    VelocityMetrics.GetVelocityMetricsFactory(rotationTimeInSecs, emitTimeInSecs) //(rotationTimeInSecs, emitTimeInSecs)
  }

  /*
   * Create VelocityMetricsFactoryInterface 
   */
  def getVMFactory(rotationTmeInSecs: Int, emitTmeInSecs: Int): VelocityMetricsFactoryInterface = synchronized {
    var rotationTimeInSecs: Int = 30
    var emitTimeInSecs: Int = 15
    try {
      if (rotationTmeInSecs == 0)
        rotationTimeInSecs = rotationTmeInSecs
      if (emitTmeInSecs == 0)
        emitTimeInSecs = emitTmeInSecs
      LOG.info("VelocityMetrics Stats Info - RotationTimeInSecs  " + rotationTimeInSecs)
      LOG.info("VelocityMetrics Stats Info EmitTimeInSecs  " + emitTimeInSecs)
      val existingFactory = VelocityMetrics.GetExistingVelocityMetricsFactory
      if (existingFactory != null) return existingFactory
    } catch {
      case e: Exception => LOG.error("VelocityMetrics Factory Instance" + e.getMessage)
    }
    return VelocityMetrics.GetVelocityMetricsFactory(rotationTimeInSecs, emitTimeInSecs) //(rotationTimeInSecs, emitTimeInSecs)

  }

}

class VelocityMetricsInfo {
  private val LOG = LogManager.getLogger(getClass);

  val metricsbymsgtype = "metricsbymsgtype"
  val metricsbymsgkeys = "metricsbymsgkeys"
  val metricsbyfilename = "metricsbyfilename"
  val velocitymetrics = "VelocityMetrics"
  val metricsbymsgfixedstring = "metricsbymsgfixedstring"
  val input = "input"
  val counterNames = Array("processed", "exception")
  val uscore = "_"

  /* 
   * Get all the VM Instances and metrics info for all msgtype keys
   */
  def getMsgVelocityInstances(VMFactory: VelocityMetricsFactoryInterface, adapCategory: String, adapName: String, adapFullConfig: String, nodeId: String): Array[InstanceRuntimeInfo] = {
    val allMsgInstances = ArrayBuffer[InstanceRuntimeInfo]()
    try {
      val vmetrics = getVelocityMetricsConfig(adapFullConfig)
      val compName = adapCategory + uscore + adapName
      // val compName = getComponentTypeName(adapCategory.toLowerCase(), adapConfig.adapterSpecificCfg)

      if (nodeId == null) throw new Exception("Node Id is null")
      val allinstances = getVelocityMetricsInstances(VMFactory, nodeId, adapFullConfig, compName)
      if (allinstances != null && allinstances.length > 0) {
        for (i <- 0 until allinstances.length) {
          if (allinstances(i).typId > 1 && allinstances(i).typId < 5) {
            allMsgInstances += allinstances(i)
          }
        }
      }
    } catch {
      case e: Exception => LOG.error(e.getMessage)
    }
    return allMsgInstances.toArray
  }
  /* 
   * Get all the VM Instances and metrics info for all msgtype keys
   */
  def getMsgVelocityInstances(VMFactory: VelocityMetricsFactoryInterface, adapCategory: String, adapName: String, adapFullConfig: String, nodeContext: NodeContext): Array[InstanceRuntimeInfo] = {
    val allMsgInstances = ArrayBuffer[InstanceRuntimeInfo]()
    try {
      var nodeId: String = null
      val vmetrics = getVelocityMetricsConfig(adapFullConfig)
      val compName = adapCategory + uscore + adapName
      // val compName = getComponentTypeName(adapCategory.toLowerCase(), adapConfig.adapterSpecificCfg)
      if (nodeContext != null) {
        nodeId = nodeContext.getEnvCtxt().getNodeId()
      }
      if (nodeId == null) throw new Exception("Node Id is null")
      val allinstances = getVelocityMetricsInstances(VMFactory, nodeId, adapFullConfig, compName)
      if (allinstances != null && allinstances.length > 0) {
        for (i <- 0 until allinstances.length) {
          if (allinstances(i).typId > 1 && allinstances(i).typId < 5) {
            allMsgInstances += allinstances(i)
          }
        }
      }
    } catch {
      case e: Exception => LOG.error(e.getMessage)
    }
    return allMsgInstances.toArray
  }

  def getFileVelocityInstances(VMFactory: VelocityMetricsFactoryInterface, adapCategory: String, adapName: String, adapFullConfig: String, nodeId: String): Array[InstanceRuntimeInfo] = {
    val allMsgInstances = ArrayBuffer[InstanceRuntimeInfo]()
    try {
      val vmetrics = getVelocityMetricsConfig(adapFullConfig)
      val compName = adapCategory + uscore + adapName
      // val compName = getComponentTypeName(adapCategory.toLowerCase(), adapConfig.adapterSpecificCfg)

      if (nodeId == null) throw new Exception("Node Id is null")
      val allinstances = getVelocityMetricsInstances(VMFactory, nodeId, adapFullConfig, compName)
      if (allinstances != null && allinstances.length > 0) {
        for (i <- 0 until allinstances.length) {
          if (allinstances(i).typId == 1) {
            allMsgInstances += allinstances(i)
          }
        }
      }
    } catch {
      case e: Exception => LOG.error(e.getMessage)
    }
    return allMsgInstances.toArray
  }

  def getFileVelocityInstances(VMFactory: VelocityMetricsFactoryInterface, adapCategory: String, adapName: String, adapFullConfig: String, nodeContext: NodeContext): Array[InstanceRuntimeInfo] = {
    val allMsgInstances = ArrayBuffer[InstanceRuntimeInfo]()
    try {
      var nodeId: String = null
      val vmetrics = getVelocityMetricsConfig(adapFullConfig)
      val compName = adapCategory + uscore + adapName
      // val compName = getComponentTypeName(adapCategory.toLowerCase(), adapConfig.adapterSpecificCfg)
      if (nodeContext != null) {
        nodeId = nodeContext.getEnvCtxt().getNodeId()
      }
      if (nodeId == null) throw new Exception("Node Id is null")
      val allinstances = getVelocityMetricsInstances(VMFactory, nodeId, adapFullConfig, compName)
      if (allinstances != null && allinstances.length > 0) {
        for (i <- 0 until allinstances.length) {
          if (allinstances(i).typId == 1) {
            allMsgInstances += allinstances(i)
          }
        }
      }
    } catch {
      case e: Exception => LOG.error(e.getMessage)
    }
    return allMsgInstances.toArray
  }

  def getOutputUtilsVelocityInstances(VMFactory: VelocityMetricsFactoryInterface, adapCategory: String, adapName: String, adapFullConfig: String, nodeId: String): Array[InstanceRuntimeInfo] = {
    val allMsgInstances = ArrayBuffer[InstanceRuntimeInfo]()
    try {
      val vmetrics = getVelocityMetricsConfig(adapFullConfig)
      val compName = adapCategory + uscore + adapName
      // val compName = getComponentTypeName(adapCategory.toLowerCase(), adapConfig.adapterSpecificCfg)
      if (nodeId == null) throw new Exception("Node Id is null")
      val allinstances = getVelocityMetricsInstances(VMFactory, nodeId, adapFullConfig, compName)
      if (allinstances != null && allinstances.length > 0) {
        for (i <- 0 until allinstances.length) {
          if (allinstances(i).typId == 3 || allinstances(i).typId == 4) {
            allMsgInstances += allinstances(i)
          }
        }
      }
    } catch {
      case e: Exception => LOG.error(e.getMessage)
    }
    return allMsgInstances.toArray
  }
  /*
   * Create the VelocityMetricsInbstances for the VelocityMetricsInfo key types
   */

  def getVelocityMetricsInstances(VMFactory: VelocityMetricsFactoryInterface, nodeId: String, adapFullConfig: String, compName: String): Array[InstanceRuntimeInfo] = {

    var velocityMetricsInstBuf = new scala.collection.mutable.ArrayBuffer[(VelocityMetricsInstanceInterface, VelocityMetricsCfg)]()

    val allInstances = ArrayBuffer[InstanceRuntimeInfo]()

    try {
      val vmetrics = getVelocityMetricsConfig(adapFullConfig)
      var componentName: String = ""
      LOG.info("vmetrics.length  " + vmetrics.length)
      vmetrics.foreach(vm => {
        val typ = vm.KeyType
        if (typ == null || typ.size == 0) throw new Exception("Metrics Type does not exist")
        val typId = GetTypeId(typ)
        val validMsgTypes = vm.ValidMsgTypes
        var islocaltime: Boolean = false
        var allMsgsValid: Boolean = true
        val intervalRoundingInSecs = vm.TimeIntervalInSecs
        var metricsTime: Long = System.currentTimeMillis()
        var metricsTimeKey = ""
        var metricsTimeKeyFormat: SimpleDateFormat = null
        var metricstimeformat = vm.MetricsTime.Format.trim()
        if (metricstimeformat == null || metricstimeformat.trim().size == 0) {
          metricsTimeKeyFormat = new SimpleDateFormat();
        } else {
          try {
            metricsTimeKeyFormat = new SimpleDateFormat(metricstimeformat)
            //  println("good format: " + metricsTimeKeyFormat.getTimeZone);
            // good format
          } catch {
            // bad format
            case e: Exception => {
              metricsTimeKeyFormat = new SimpleDateFormat();
              // println("bad format: " + metricsTimeKeyFormat.getTimeZone);
            }
          }
        }

        val metricsType = vm.MetricsTime.MType
        if (metricsType.equalsIgnoreCase("field")) {
          metricsTimeKey = vm.MetricsTime.Field
          val frmat = vm.MetricsTime.Format
        } else islocaltime = true

        val keys = vm.Keys
        var msgkeys = new Array[String](keys.length)
        componentName = compName + uscore + vm.KeyType + uscore + vm.TimeIntervalInSecs

        var vmInstance = VMFactory.GetVelocityMetricsInstance(nodeId, componentName, intervalRoundingInSecs, counterNames)
        if (vmInstance == null) LOG.info("VMInstance is null")

        val instanceRuntimeInfo = new InstanceRuntimeInfo(typ, typId, vmInstance, validMsgTypes, allMsgsValid, keys, vm.KeyStrings, islocaltime, metricsTimeKey, metricsTimeKeyFormat)
        allInstances += instanceRuntimeInfo

        LOG.info("vm.TimeIntervalInSecs  " + vm.TimeIntervalInSecs)
        LOG.info("vm.Keys  " + vm.Keys.toList)
        LOG.info("vm.MetricsTime.MType  " + vm.MetricsTime.MType)
        LOG.info("nodeId " + nodeId)
        LOG.info("componentName  " + componentName)
        LOG.info("intervalRoundingInMs  " + intervalRoundingInSecs)
        LOG.info("counterNames  " + counterNames.toList)

      })

    } catch {
      case e: Exception => LOG.error("increment Velocity Metrics " + e.getMessage)
    }

    allInstances.toArray
  }

  def getComponentTypeName(adapterType: String, adapCfgStr: String): String = {
    if (adapterType == null || adapterType.trim().size == 0) LOG.info("Adapter category does not exist")

    if (adapCfgStr == null || adapCfgStr.trim().size == 0) {
      LOG.info("Adapter Specific Config does not exist")
    }
    //println("adapCfg.values " + adapCfgStr)
    if (adapCfgStr != null) {
      val adapCfgJson = parse(adapCfgStr)
      if (adapCfgJson == null || adapCfgJson.values == null) {
        LOG.warn("Failed to parse AdapterSpecific JSON configuration string:" + adapCfgStr)
        throw new Exception("Failed to parse AdapterSpecific JSON configuration string:" + adapCfgStr)

      }
      val adapCfgVals = adapCfgJson.values.asInstanceOf[Map[String, Any]]
      //println("adapCfgVals.values " + adapCfgVals)

      val adapCfgValues: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map[String, Any]()
      adapCfgVals.foreach(kv => { adapCfgValues(kv._1.trim().toLowerCase()) = kv._2 })

      //println("adapCfgValues.values " + adapCfgValues)

      val typ = adapCfgValues.getOrElse("type", null)
      val topicname = adapCfgValues.getOrElse("topicname", null)
      val compressionString = adapCfgValues.getOrElse("compressionstring", null)
      if (topicname != null) return adapterType + "_" + "kafka";
      else if (typ != null) {
        if (typ.toString().equalsIgnoreCase("nas/das"))
          return adapterType + "_" + "smartfilenas";
        else if (typ.toString().equalsIgnoreCase("hdfs"))
          return adapterType + "_" + "smartfilehdfs";
        else if (typ.toString().equalsIgnoreCase("sft"))
          return adapterType + "_" + "smartfilesft";
      } else if (compressionString != null)
        return adapterType + "_" + "fileconsumer";
    }
    return ""

  }

  def GetTypeId(typ: String): Int = {
    if (typ.equalsIgnoreCase(metricsbyfilename)) return 1
    if (typ.equalsIgnoreCase(metricsbymsgtype)) return 2
    if (typ.equalsIgnoreCase(metricsbymsgkeys)) return 3
    if (typ.equalsIgnoreCase(metricsbymsgfixedstring)) return 4
    return 5
  }

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

  def incrementIAVelocityMetrics(VMInstance: InstanceRuntimeInfo, message: ContainerInterface, processed: Boolean): Unit = {
    LOG.info("Start Increment Velocity Metrics")
    try {
      var metricsTime: Long = System.currentTimeMillis()
      val typId = VMInstance.typId
      if (typId >= 5) throw new Exception("The metrics type key is not valid")

      if (typId > 1 && typId < 5) {

        if (!VMInstance.isLocalTime) {
          var field = VMInstance.timeKey
          if (field != null && field.trim().size > 0) {
            val fieldVal = message.getOrElse(field, null)
            metricsTime = extractTime(field, VMInstance.timeFormat)
          }
        }

        if (typId == 2) {
          val metricsKey = message.getTypeName
          if (processed) {
            VMInstance.instance.increment(metricsTime, metricsKey, System.currentTimeMillis(), true, false)
          } else {
            VMInstance.instance.increment(metricsTime, metricsKey, System.currentTimeMillis(), false, true)
          }
        } else if (typId == 3) {
          val mkeys = VMInstance.MsgKeys
          if (mkeys != null && mkeys.length > 0) {
            var msgkeys = new Array[String](mkeys.length)
            if (mkeys != null && mkeys.length > 0) {
              LOG.info("Keys length " + mkeys.length)
              for (j <- 0 until mkeys.length) {
                msgkeys(j) = message.getOrElse(mkeys(j).trim, "").toString
                LOG.info("keys(j) " + mkeys(j))
                LOG.info("msgkeys(j) " + msgkeys(j))
              }
            }
            if (msgkeys != null && msgkeys.length > 0) {
              val metricsKey = msgkeys.mkString(",")
              if (processed) {
                VMInstance.instance.increment(metricsTime, metricsKey, System.currentTimeMillis(), true, false)
              } else {
                VMInstance.instance.increment(metricsTime, metricsKey, System.currentTimeMillis(), false, true)
              }
            }
          } else throw new Exception("Keys for the metricsbymsgkeys need to be provided in the velocity metrics config")

        } else if (typId == 4) {
          if (VMInstance.KeyStrings != null && VMInstance.KeyStrings.length > 0) {
            val metricsKey = VMInstance.KeyStrings.mkString(",")
            if (processed) {
              VMInstance.instance.increment(metricsTime, metricsKey, System.currentTimeMillis(), true, false)
            } else {
              VMInstance.instance.increment(metricsTime, metricsKey, System.currentTimeMillis(), false, true)
            }
          } else throw new Exception("KeyStrings for the metricsbymsgfixedstring need to be provided in the velocity metrics config")
        }

      }

    } catch {
      case e: Exception => LOG.error("increment Velocity Metrics " + e.getMessage)
    }

  }

  /*
   * Increment the velocity metrics - get the VelocityMetricsInstance Factory and call increment for metrics by msgType and metrics by msg keys
   */

  def incrementFileVMetrics(VMInstance: InstanceRuntimeInfo, filename: String, processed: Boolean): Unit = {
    LOG.info("Start Increment Velocity Metrics")
    try {
      var metricsTime: Long = System.currentTimeMillis()
      val typId = VMInstance.typId
      if (typId >= 5) throw new Exception("The metrics type key is not valid")

      if (typId == 1) {
        val metricsKey = filename
        if (processed) {
          VMInstance.instance.increment(metricsTime, metricsKey, System.currentTimeMillis(), true, false)
        } else {
          VMInstance.instance.increment(metricsTime, metricsKey, System.currentTimeMillis(), false, true)
        }
      }

    } catch {
      case e: Exception => LOG.error("increment Velocity Metrics " + e.getMessage)
    }

  }

  /*
   * Increment the velocity metrics - get the VelocityMetricsInstance Factory and call increment for metrics by msgType and metrics by msg keys
   */

  def incrementOutputUtilsVMetricsByKey(VMInstance: InstanceRuntimeInfo, keys: Array[String], keyStrings: Array[String], processed: Boolean): Unit = {
    LOG.info("Start Increment Velocity Metrics")
    try {
      var metricsTime: Long = System.currentTimeMillis()
      val typId = VMInstance.typId
      if (typId >= 5) throw new Exception("The metrics type key is not valid")
      var metricsKey = ""
      if (typId == 3 || typId == 4) {
        if (typId == 3) {
          if (keys != null && keys.length > 0)
            metricsKey = keys.mkString(",")
        } else if (typId == 4) {
          if (keyStrings != null && keyStrings.length > 0)
            metricsKey = keyStrings.mkString(",")
        }

        if (processed) {
          VMInstance.instance.increment(metricsTime, metricsKey, System.currentTimeMillis(), true, false)
        } else {
          VMInstance.instance.increment(metricsTime, metricsKey, System.currentTimeMillis(), false, true)
        }
      }

    } catch {
      case e: Exception => LOG.error("increment Velocity Metrics " + e.getMessage)
    }

  }

  /*
   * Increment the velocity metrics - get the VelocityMetricsInstance Factory and call increment for metrics by msgType and metrics by msg keys
   */

  /* def incrementVelocityMetrics(VMFactory: VelocityMetricsFactoryInterface, componentName: String, nodeId: String, message: ContainerInterface, adapConfig: AdapterConfiguration, processed: Boolean): Unit = {
    LOG.info("Start Increment Velocity Metrics")
    try {
      val vm = getIAVelocityMetricsInstances(VMFactory, nodeId, adapConfig, componentName)
      var Key: String = ""
      if (vm == null || vm.size == 0) return LOG.info("Velocity Metrics does not exists")
      for (i <- 0 until vm.size) {
        val keyType = vm(i)._2.KeyType
        val validMsgType = vm(i)._2.ValidMsgTypes
        if (keyType.equalsIgnoreCase(metricsbymsgtype) || keyType.equalsIgnoreCase(metricsbymsgkeys)) {
          LOG.info("validMsgType size " + validMsgType.size)
          LOG.info("validMsgType List " + validMsgType.toList)
          LOG.info("validMsgType keyType " + keyType)

          if (validMsgType != null && validMsgType.size > 0 && validateMsgType(message.FullName(), validMsgType)) {

            var metricsTime: Long = System.currentTimeMillis()
            val metricsType = vm(i)._2.MetricsTime.MType
            if (metricsType.equalsIgnoreCase("field")) {
              val field = vm(i)._2.MetricsTime.Field
              val frmat = vm(i)._2.MetricsTime.Format
              if (field != null && field.trim().size > 0) {
                val fieldVal = message.getOrElse(field, null)
                // metricsTime = extractTime(field, frmat)
              }
            }
            val keys = vm(i)._2.Keys
            var msgkeys = new Array[String](keys.length)
            if (keys != null && keys.length > 0) {
              LOG.info("Keys length " + keys.length)
              for (j <- 0 until keys.length) {

                msgkeys(j) = message.getOrElse(keys(j).trim, "").toString
                LOG.info("keys(j) " + keys(j))
                LOG.info("msgkeys(j) " + msgkeys(j))
              }
            }

            if (msgkeys != null && msgkeys.length > 0) {
              Key = keyType + "_" + message.Name().toLowerCase() + "_" + msgkeys.mkString("_")
            } else
              Key = keyType + "_" + message.Name().toLowerCase()

            if (processed) {
              vm(i)._1.increment(metricsTime, Key, System.currentTimeMillis(), true, false)
            } else {
              vm(i)._1.increment(metricsTime, Key, System.currentTimeMillis(), false, true)
            }
          }
        }
      }

      LOG.info("End Increment-Velocity Metrics")
    } catch {
      case e: Exception => LOG.error("increment Velocity Metrics " + e.getMessage)
    }

  }

  
   * Increment the velocity metrics - get the VelocityMetricsInstance Factory and call increment of velocity metrics by file name
   

  def incrementFileVelocityMetrics(VMFactory: VelocityMetricsFactoryInterface, componentName: String, fileName: String, nodeId: String, adapConfig: AdapterConfiguration) = {
    LOG.info("Start Increment")
    try {
      val vm = getIAVelocityMetricsInstances(VMFactory, nodeId, adapConfig, componentName)
      if (vm == null || vm.size == 0) LOG.info("Velocity Metrics does not exists")
      for (i <- 0 until vm.size) {
        var metricsTime: Long = System.currentTimeMillis() //0L
        val metricsType = vm(i)._2.MetricsTime.MType
        metricsTime = System.currentTimeMillis()

        val keyType = vm(i)._2.KeyType
        var Key: String = keyType + "_" + fileName
        var msgkeys = Array[String]()
        if (keyType.equalsIgnoreCase(metricsbyfilename)) {
          vm(i)._1.increment(metricsTime, Key, System.currentTimeMillis(), true, false)
        }
        LOG.info("End Increment")
      }
    } catch {
      case e: Exception => LOG.error("increment Velocity Metrics " + e.getMessage)
    }

  }

  
   * Create the VelocityMetricsInbstances for the VelocityMetricsInfo key types
   

  private def getIAVelocityMetricsInstances(VMFactory: VelocityMetricsFactoryInterface, nodeId: String, adapConfig: AdapterConfiguration, componentName: String): Array[(VelocityMetricsInstanceInterface, VelocityMetricsCfg)] = {

    var velocityMetricsInstBuf = new scala.collection.mutable.ArrayBuffer[(VelocityMetricsInstanceInterface, VelocityMetricsCfg)]()
    try {
      val vmetrics = getVelocityMetricsConfig(adapConfig.fullAdapterConfig)
      LOG.info("vmetrics.length  " + vmetrics.length)
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

        LOG.info("vm.TimeIntervalInSecs  " + vm.TimeIntervalInSecs)
        LOG.info("vm.Keys  " + vm.Keys.toList)
        LOG.info("vm.MetricsTime.MType  " + vm.MetricsTime.MType)
        LOG.info("nodeId " + nodeId)
        LOG.info("componentName  " + componentName)
        LOG.info("intervalRoundingInMs  " + intervalRoundingInSecs)
        LOG.info("counterNames  " + counterNames.toList)

      })
    } catch {
      case e: Exception => LOG.error("increment Velocity Metrics " + e.getMessage)
    }

    velocityMetricsInstBuf.toArray
  }

*/ /*
   * Parse the adapter json and create the Array of VelocityMetricsCfg for the metrics keys types
   */
  def getVelocityMetricsConfig(fullAdapterConfig: String): Array[VelocityMetricsCfg] = {
    if (fullAdapterConfig == null && fullAdapterConfig.trim().size == 0) {
      LOG.info("fullAdapterConfig is null")
      return null
    }
    parseVelocityMetrics(fullAdapterConfig)
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
        LOG.warn("Failed to parse VelocityMetrics JSON configuration string")
        throw new Exception("Failed to parse VelocityMetrics JSON configuration string")

      }
      LOG.info("Start Parser Velocity Metrics ")
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
          var keysString = new ArrayBuffer[String]()
          var validMsgTypes = new ArrayBuffer[String]()
          var keyType: String = ""
          var timeIntervalInSecs: Int = 0
          var metricsTimeType: String = "LocalTime"
          var metricsTimeField: String = ""
          var metricsTimeFormat: String = ""

          LOG.info("vm Map   " + vm)
          if (vm.contains(metricsbyfilename) || vm.contains(metricsbymsgtype) || (vm.contains(metricsbymsgkeys)) || (vm.contains(metricsbymsgfixedstring))) {
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
            } else if (vm.contains(metricsbymsgfixedstring)) {
              keyType = metricsbymsgfixedstring

              val metricsByType = vm.getOrElse(metricsbymsgfixedstring, null)
              if (metricsByType != null)
                vmetrics = metricsByType.asInstanceOf[Map[String, Any]]
            }

            val metrics: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map[String, Any]()
            vmetrics.foreach(kv => { metrics(kv._1.trim().toLowerCase()) = kv._2 })

            if (metrics != null) {
              //    val mByFileName = metricsByFileName.asInstanceOf[Map[String, Any]]

              val timeIntrvlInSecs = metrics.getOrElse("timeintervalinsecs", 0)
              if (timeIntrvlInSecs != 0) {
                timeIntervalInSecs = timeIntrvlInSecs.toString().toInt
              }

              val keysLst = metrics.getOrElse("keys", null)
              if (keysLst != null) {
                val keysList = keysLst.asInstanceOf[List[String]]
                keysList.foreach(key => {
                  keys += key.trim()
                })
              }

              val keyStringLst = metrics.getOrElse("keystring", null)
              if (keyStringLst != null) {
                val keysStringList = keyStringLst.asInstanceOf[List[String]]
                keysStringList.foreach(key => {
                  keysString += key.trim()
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
              velocityMtrcsVar = new VelocityMetricsCfg(keyType, keys.toArray, keysString.toArray, validMsgTypes.toArray, timeIntervalInSecs, mTime)
            }
            velocityMetricsBuf += velocityMtrcsVar
          }
        })
      }
      if (LOG.isDebugEnabled()) {
        velocityMetricsBuf.foreach(vm => {
          LOG.info("vm keytype " + vm.KeyType)
          LOG.info("vm key " + vm.Keys.size)
          LOG.info("vm ValidMsgTypes length " + vm.ValidMsgTypes.length)
          vm.ValidMsgTypes.foreach { msg => LOG.info("vm msg" + msg) }
          LOG.info("vm " + vm.TimeIntervalInSecs)
          LOG.info("vm " + vm.MetricsTime.MType)
          LOG.info("vm " + vm.MetricsTime.Field)
          LOG.info("vm " + vm.MetricsTime.Format)

          LOG.info("vm keytype" + vm.KeyType)
          LOG.info("vm key" + vm.Keys.size)
          LOG.info("vm " + vm.TimeIntervalInSecs)
          LOG.info("vm " + vm.MetricsTime.MType)
          LOG.info("vm " + vm.MetricsTime.Field)
          LOG.info("vm " + vm.MetricsTime.Format)

        })
      }

      LOG.info("End Parse Velocity Metrics ")
    } catch {
      case e: Exception => LOG.error("VelocityMetrics - Parse Velocity Metrics" + e.getMessage)
    }
    velocityMetricsBuf.toArray
  }

  private def extractTime(fieldData: String, dtFormat: SimpleDateFormat): Long = {
    val tm =
      if (fieldData == null || fieldData.trim.size == 0 || dtFormat == null) {
        new Date(0)
      } else {
        dtFormat.parse(fieldData)
      }
    tm.getTime
  }

}