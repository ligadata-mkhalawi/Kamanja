package com.ligadata.VelocityMetrics

// import java.util.HashMap
import java.util.UUID
import java.util.concurrent.{ScheduledExecutorService, Executors, TimeUnit}
import scala.collection.mutable.ArrayBuffer
import org.apache.log4j.Logger

object VelocityMetrics {
  private val logger = Logger.getLogger(getClass)

  private[VelocityMetrics] case class IntervalAndKey(val key: String, val metricsTime: Long) {
    override def hashCode() = (key + "," + metricsTime).hashCode()

    override def equals(other: Any): Boolean = {
      if (!other.isInstanceOf[IntervalAndKey]) return false
      val o = other.asInstanceOf[IntervalAndKey]
      if (metricsTime != o.metricsTime) return false
      (key == o.key)
    }
  }

  private[VelocityMetrics] class IntervalAndKeyVelocityCounts(intervalAndKey: IntervalAndKey, countersNames: Array[String]) {
    val counters: Array[Long] = new Array[Long](countersNames.size)
    var firstOccured: Long = 0L
    var lastOccured: Long = 0L

    def Add(currentTime: Long, addCntrIdxs: Array[Int], addCntrValuesForIdxs: Array[Int]): Unit = {
      addCntrIdxs.foreach(idx => {
        if (idx < countersNames.size)
          counters(idx) += addCntrValuesForIdxs(idx)
      })
      if (firstOccured == 0) firstOccured = currentTime
      lastOccured = currentTime
    }

    def duplicate(): IntervalAndKeyVelocityCounts = {
      val c = new IntervalAndKeyVelocityCounts(intervalAndKey, countersNames)
      counters.copyToArray(c.counters)
      c.firstOccured = firstOccured
      c.lastOccured = lastOccured
      c
    }

    def addTo(counts: IntervalAndKeyVelocityCounts): Unit = {
      for (i <- 0 until countersNames.size) {
        counts.counters(i) += counters(i)
      }
      if (counts.firstOccured > this.firstOccured) counts.firstOccured = this.firstOccured
      if (counts.lastOccured < this.lastOccured) counts.lastOccured = this.lastOccured
    }
  }

  private[VelocityMetrics] class VelocityMetricsInstanceImpl(intervalRoundingInMs: Long, countersNames: Array[String]) extends VelocityMetricsInstanceInterface {
    var firstOccured: Long = 0L
    var lastOccured: Long = 0L
    var intervalAndKeyVelocityCounts = scala.collection.mutable.Map[IntervalAndKey, IntervalAndKeyVelocityCounts]()

    private def AddMetrics(intervalAndKey: IntervalAndKey, currentTime: Long, addCntrIdxs: Array[Int], addCntrValuesForIdxs: Array[Int]): Unit = synchronized {
      try {
        val metricsCounts = intervalAndKeyVelocityCounts.getOrElse(intervalAndKey, null)
        if (metricsCounts != null) {
          if (firstOccured == 0) firstOccured = currentTime
          lastOccured = currentTime
          metricsCounts.Add(currentTime, addCntrIdxs, addCntrValuesForIdxs)
        } else {
          val tmpMetricsCounts = new IntervalAndKeyVelocityCounts(intervalAndKey, countersNames)
          if (firstOccured == 0) firstOccured = currentTime
          lastOccured = currentTime
          intervalAndKeyVelocityCounts(intervalAndKey) = tmpMetricsCounts
          tmpMetricsCounts.Add(currentTime, addCntrIdxs, addCntrValuesForIdxs)
        }
      } catch {
        case e1: Exception => logger.error("Failed to add metrics", e1)
        case e2: Throwable => logger.error("Failed to add metrics", e2)
      }
    }

    override def Add(metricsTime: Long, key: String, currentTime: Long, addCntrIdxs: Array[Int], addCntrValuesForIdxs: Array[Int]): Unit = {
      if (addCntrIdxs.size == 0) return
      val roundedMetricsTime = metricsTime - (metricsTime % intervalRoundingInMs)
      val intervalAndKey = IntervalAndKey(key, roundedMetricsTime)
      AddMetrics(intervalAndKey, currentTime, addCntrIdxs, addCntrValuesForIdxs)
    }

    override def increment(metricsTime: Long, key: String, currentTime: Long, addCntrIdxs: Array[Int]): Unit = {
      if (addCntrIdxs.size == 0) return
      val roundedMetricsTime = metricsTime - (metricsTime % intervalRoundingInMs)
      val intervalAndKey = IntervalAndKey(key, roundedMetricsTime)
      val addCntrValuesForIdxs = addCntrIdxs.map(v => 1)
      AddMetrics(intervalAndKey, currentTime, addCntrIdxs, addCntrValuesForIdxs)
    }

    override def increment(metricsTime: Long, key: String, currentTime: Long, addCntrIdxFlags: Boolean*): Unit = {
      if (addCntrIdxFlags.size == 0) return
      val roundedMetricsTime = metricsTime - (metricsTime % intervalRoundingInMs)
      val intervalAndKey = IntervalAndKey(key, roundedMetricsTime)

      val idxs = ArrayBuffer[Int]()
      var idxNumber = 0
      addCntrIdxFlags.foreach(flg => {
        if (flg)
          idxs += idxNumber
        idxNumber += 1
      })
      val idxArr = idxs.toArray
      val addCntrValuesForIdxs = idxArr.map(v => 1)
      AddMetrics(intervalAndKey, currentTime, idxArr, addCntrValuesForIdxs)
    }

    override def duplicate(): VelocityMetricsInstanceInterface = synchronized {
      val m = new VelocityMetricsInstanceImpl(intervalRoundingInMs, countersNames)
      try {
        m.firstOccured = this.firstOccured
        m.lastOccured = this.lastOccured
        intervalAndKeyVelocityCounts.foreach(kv => {
          m.intervalAndKeyVelocityCounts(kv._1) = kv._2.duplicate
        })
      } catch {
        case e1: Exception => logger.error("Failed to duplicate", e1)
        case e2: Throwable => logger.error("Failed to duplicate", e2)
      }
      m
    }

    override def addTo(metrics: VelocityMetricsInstanceInterface, reset: Boolean): Unit = synchronized {
      if (!metrics.isInstanceOf[VelocityMetricsInstanceImpl]) return
      val m = metrics.asInstanceOf[VelocityMetricsInstanceImpl]
      try {
        if (m.firstOccured > this.firstOccured) m.firstOccured = this.firstOccured
        if (m.lastOccured < this.lastOccured) m.lastOccured = this.lastOccured

        intervalAndKeyVelocityCounts.foreach(kv => {
          val key = kv._1
          val value = kv._2
          val metricCounts = m.intervalAndKeyVelocityCounts.getOrElse(key, null)
          if (metricCounts != null) {
            value.addTo(metricCounts)
          } else {
            m.intervalAndKeyVelocityCounts(key) = value.duplicate
          }
        })

        if (reset) {
          firstOccured = 0L
          lastOccured = 0L
          // loop thru intervalAndKeyVelocityCounts and clear. Do we really need it?
          intervalAndKeyVelocityCounts.clear()
        }
      } catch {
        case e1: Exception => logger.error("Failed to addTo", e1)
        case e2: Throwable => logger.error("Failed to addTo", e2)
      }
      return
    }
  }

  private[VelocityMetrics] class ComponentVelocityMetrics(nodeId: String, componentKey: String, intervalRoundingInMs: Long, countersNames: Array[String]) {
    private val instanceMetrics = ArrayBuffer[VelocityMetricsInstanceImpl]()

    def GetVelocityMetricsInstance(): VelocityMetricsInstanceInterface = synchronized {
      val m = new VelocityMetricsInstanceImpl(intervalRoundingInMs, countersNames)
      instanceMetrics += m
      m
    }

    def duplicate(): ComponentVelocityMetrics = synchronized {
      val comp = new ComponentVelocityMetrics(nodeId, componentKey, intervalRoundingInMs, countersNames)
      comp.instanceMetrics ++= instanceMetrics.map(inst => inst.duplicate().asInstanceOf[VelocityMetricsInstanceImpl])
      comp
    }

    def getVelocityMetricsInstanceInterfaces(): Array[VelocityMetricsInstanceImpl] = synchronized {
      instanceMetrics.toArray
    }

    def clear(): Unit = {
      instanceMetrics.clear()
    }
  }

  private[VelocityMetrics] class VelocityMetricsFactoryImpl(rotationTimeInSecs: Long, emitTimeInSecs: Long) extends VelocityMetricsFactoryInterface {
    private var guid: String = ""
    private var guidCreatedTime: Long = 0L
    private val factoryStartTime = System.currentTimeMillis()
    private var minRotationTimeInSecs: Long = 30L
    private var minEmitTimeInSecs: Long = 5L

    private val emitTmInSecs =
      if (emitTimeInSecs < minEmitTimeInSecs) {
        logger.warn("Minimam emit time is " + minEmitTimeInSecs + "  secs. Given values (in secs) " + emitTimeInSecs + " is reset to " + minEmitTimeInSecs + " secs")
        minEmitTimeInSecs
      } else {
        emitTimeInSecs
      }

    private val tmpRotationTmInSecs =
      if (rotationTimeInSecs < minRotationTimeInSecs) {
        logger.warn("Minimam rotation time is " + minRotationTimeInSecs + "  secs. Given values (in secs) " + rotationTimeInSecs + " is reset to " + minRotationTimeInSecs + " secs")
        minRotationTimeInSecs
      } else {
        rotationTimeInSecs
      }

    private val rotationTmInSecs =
      if (tmpRotationTmInSecs < emitTmInSecs) {
        logger.warn("Emit time is " + emitTmInSecs + "  secs. Rotation time is reset to " + emitTmInSecs + " secs")
        emitTmInSecs
      } else {
        tmpRotationTmInSecs
      }

    private var rotationTimeInMilliSecs: Long = rotationTmInSecs * 1000
    private var curSendRotationIdx: Long = factoryStartTime / rotationTimeInMilliSecs

    private var metricsComponents = scala.collection.mutable.Map[String, ComponentVelocityMetrics]()

    private def setGuid(curTime: Long) {
      guid = UUID.randomUUID().toString
      guidCreatedTime = curTime
    }

    setGuid(factoryStartTime)

    private val callbackListeners = ArrayBuffer[VelocityMetricsCallback]()
    private var scheduledThreadPool: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var isTerminated = false

    private def EmitMetrics(reset: Boolean): Unit = synchronized {
      val allComponents = GetAllMetricsComponents
/*
      val allInstaceInterfaces = ArrayBuffer[VelocityMetricsInstanceImpl]()
      allComponents.foreach(comp => {
        allInstaceInterfaces ++= comp.getVelocityMetricsInstanceInterfaces()
      })
*/
      // allInstaceInterfaces.groupBy(inst => inst.)


      /*
                  allComponents.map(comp => {
                    // val dup = comp.
                  })

                  for (m <- mdlInstanceMetrics) {
                    m.addTo(metrics, reset)
                  }
                  var dailyMetrics = Array.ofDim[ContainerInterface](metrics.dailyVelocityCounts.size)
                  var idx = 0
                  for ((key, value) <- metrics.dailyVelocityCounts) {
                    var v = value
                    var m = DailyVelocityMetrics.createInstance()
                    m.set("event_date", v.dataDate)
                    m.set("msg_in_cnt", v.input)
                    m.set("msg_out_cnt", v.output)
                    m.set("msg_dedup_cnt", v.dedup)
                    dailyMetrics(idx) = m
                    idx += 1
                  }
                  var finalMetrics = VelocityMetrics.createInstance()
                  finalMetrics.set("log_id", local_guid)
                  finalMetrics.set("node_id", nodeId)
                  finalMetrics.set("eventtype", eventtype)
                  finalMetrics.set("timestampinmilliseconds", curTime)
                  finalMetrics.set("msg_in_cnt", metrics.totalInput)
                  finalMetrics.set("msg_out_cnt", metrics.totaloutput)
                  finalMetrics.set("msg_dedup_cnt", metrics.totaldedup)
                  finalMetrics.set("msg_error_cnt", metrics.exceptions)
                  finalMetrics.set("dailymetrics", dailyMetrics)
                  var msg = Array.ofDim[ContainerInterface](1)
                  msg(0) = finalMetrics
      */
      val metrics: Metrics = null
      val callbacks = callbackListeners.toArray
      callbacks.foreach(cb => {
        try {
          if (cb != null)
            cb.call(metrics)
        } catch {
          case e: Exception => {
            logger.error("Failed to callback with velocity metrics", e)
          }
          case e: Throwable => {
            logger.error("Failed to callback with velocity metrics", e)
          }
        }
      })
    }

    val externalizeMetrics = new Runnable() {
      def run() {
        try {
          var local_guid = guid
          var local_guidCreatedTime = guidCreatedTime
          var curTime = System.currentTimeMillis()
          var nextSendRotationIdx = (curTime + emitTmInSecs * 1000) / rotationTimeInMilliSecs
          var reset = (curSendRotationIdx != nextSendRotationIdx)
          if (logger.isDebugEnabled) logger.debug("About to send metrics message. guid:" + local_guid + ", guidCreatedTime:" +
            local_guidCreatedTime + ", curTime:" + curTime + ", nextSendRotationIdx:" +
            nextSendRotationIdx + ", curSendRotationIdx:" + curSendRotationIdx + ", reset:" + reset)
          EmitMetrics(reset)
          curSendRotationIdx = nextSendRotationIdx
          if (reset) setGuid(curTime)
        } catch {
          case e1: Exception => logger.error("Failed to send metrics", e1)
          case e2: Throwable => logger.error("Failed to send metrics", e2)
        }
      }
    }

    private val initalDelay = (((System.currentTimeMillis() + emitTmInSecs * 1000) / rotationTimeInMilliSecs) * rotationTimeInMilliSecs) - System.currentTimeMillis()

    scheduledThreadPool.scheduleWithFixedDelay(externalizeMetrics, initalDelay, emitTmInSecs * 1000, TimeUnit.MILLISECONDS)

    private def GetAllMetricsComponents: Array[ComponentVelocityMetrics] = synchronized {
      metricsComponents.values.toArray
    }

    private def GetMetricsComponent(componentKey: String, nodeId: String, intervalRoundingInMs: Long, countersNames: Array[String], addIfMissing: Boolean): ComponentVelocityMetrics = synchronized {
      val compKey = componentKey.trim.toLowerCase
      var comp = metricsComponents.getOrElse(compKey, null)
      if (comp == null && addIfMissing) {
        comp = new ComponentVelocityMetrics(nodeId.trim.toLowerCase, compKey, intervalRoundingInMs: Long, countersNames: Array[String])
      }
      comp
    }

    override def GetVelocityMetricsInstance(nodeId: String, componentKey: String, intervalRoundingInMs: Long, countersNames: Array[String]): VelocityMetricsInstanceInterface = {
      if (isTerminated) {
        throw new Exception("VelocityMetricsFactoryInterface is already terminated.")
      }
      val comp = GetMetricsComponent(componentKey, nodeId, intervalRoundingInMs, countersNames, true)
      comp.GetVelocityMetricsInstance()
    }

    override def shutdown(): Unit = synchronized {
      isTerminated = true
      scheduledThreadPool.shutdownNow()
      EmitMetrics(false)
      metricsComponents.foreach(kv => kv._2.clear())
      metricsComponents.clear()
    }

    override def addEmitListener(velocityMetricsCallback: VelocityMetricsCallback): Unit = synchronized {
      if (velocityMetricsCallback != null)
        callbackListeners += velocityMetricsCallback
    }
  }

  private var factory: VelocityMetricsFactoryImpl = null

  // We instantiate Factory instance with the first given rotationTimeInSecs & emitTimeInSecs
  def GetVelocityMetricsFactory(rotationTimeInSecs: Long, emitTimeInSecs: Long): VelocityMetricsFactoryInterface = {
    if (factory != null) return factory
    this.synchronized {
      if (factory == null) {
        factory = new VelocityMetricsFactoryImpl(rotationTimeInSecs, emitTimeInSecs)
      }
    }
    return factory
  }

  def shutdown(): Unit = synchronized {
    if (factory != null)
      factory.shutdown()
    factory = null
  }

  def addEmitListener(velocityMetricsCallback: VelocityMetricsCallback): Unit = synchronized {
    if (factory != null)
      factory.addEmitListener(velocityMetricsCallback)
    factory = null
  }

}


