package com.ligadata.InputOutputAdapterInfo

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import scala.io.Source;
import scala.collection.mutable.ArrayBuffer

case class VelocityMetricsCfg(var KeyType: String, var Keys: Array[String], var ValidMsgTypes: Array[String], var TimeIntervalInSecs: Int, var MetricsTime: MetricsTime)
case class MetricsTime(var MType: String, var Field: String, var Format: String)

class VelocityMetricsInfo {

  def parseVelocityMetrics(fullAdapterConfig: String): Array[VelocityMetricsCfg] = {
    var velocityMetricsBuf = new ArrayBuffer[VelocityMetricsCfg]()
    val json = parse(fullAdapterConfig)
    var parsed_json: Map[String, Any] = null

    if (json == null || json.values == null) {
      throw new Exception("Failed to parse Storage JSON configuration string:" + json)
    }
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
      println("vm keytype" + vm.KeyType)
      println("vm key" + vm.Keys.size)
      println("vm " + vm.TimeIntervalInSecs)
      println("vm " + vm.MetricsTime.MType)
      println("vm " + vm.MetricsTime.Field)
      println("vm " + vm.MetricsTime.Format)
    })
    println(velocityMetrics)
    velocityMetricsBuf.toArray
  }
}