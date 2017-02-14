package com.ligadata.VelocityMetrics

// import java.util.HashMap
import java.util.UUID
import java.util.concurrent.{ ScheduledExecutorService, Executors, TimeUnit }
import scala.collection.mutable.ArrayBuffer
import org.apache.log4j.Logger

object VelocityMetrics {
  private val logger = Logger.getLogger(getClass)

  private[VelocityMetrics] case class IntervalAndKey(val key: String, val metricsTime: Long, val intervalRoundingInSecs: Int) {
    override def hashCode() = (key + "," + metricsTime + "," + intervalRoundingInSecs).hashCode()

    override def equals(other: Any): Boolean = {
      if (!other.isInstanceOf[IntervalAndKey]) return false
      val o = other.asInstanceOf[IntervalAndKey]
      if (metricsTime != o.metricsTime) return false
      if (intervalRoundingInSecs != o.intervalRoundingInSecs) return false
      (key == o.key)
    }
  }

  private[VelocityMetrics] class IntervalAndKeyVelocityCounts(val intervalAndKey: IntervalAndKey, val countersNames: Array[String]) {
    val counters: Array[Long] = new Array[Long](countersNames.size)
    var firstOccured: Long = 0L
    var lastOccured: Long = 0L

    def Add(currentTime: Long, addCntrIdxs: Array[Int], addCntrValuesForIdxs: Array[Int]): Unit = {
      var curIdx = 0
      addCntrIdxs.foreach(idx => {
        if (idx < countersNames.size)
          counters(idx) += addCntrValuesForIdxs(curIdx)
        curIdx += 1
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

  private[VelocityMetrics] class VelocityMetricsInstanceImpl(val intervalRoundingInSecs: Int, val countersNames: Array[String]) extends VelocityMetricsInstanceInterface {
    var firstOccured: Long = 0L
    var lastOccured: Long = 0L
    val intervalRoundingInMs = intervalRoundingInSecs * 1000
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
      val intervalAndKey = IntervalAndKey(key, roundedMetricsTime, intervalRoundingInSecs)
      AddMetrics(intervalAndKey, currentTime, addCntrIdxs, addCntrValuesForIdxs)
    }

    override def increment(metricsTime: Long, key: String, currentTime: Long, addCntrIdxs: Array[Int]): Unit = {
      if (addCntrIdxs.size == 0) return
      val roundedMetricsTime = metricsTime - (metricsTime % intervalRoundingInMs)
      val intervalAndKey = IntervalAndKey(key, roundedMetricsTime, intervalRoundingInSecs)
      val addCntrValuesForIdxs = addCntrIdxs.map(v => 1)
      AddMetrics(intervalAndKey, currentTime, addCntrIdxs, addCntrValuesForIdxs)
    }

    override def increment(metricsTime: Long, key: String, currentTime: Long, addCntrIdxFlags: Boolean*): Unit = {
      if (addCntrIdxFlags.size == 0) return
      val roundedMetricsTime = metricsTime - (metricsTime % intervalRoundingInMs)
      val intervalAndKey = IntervalAndKey(key, roundedMetricsTime, intervalRoundingInSecs)

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
      val m = new VelocityMetricsInstanceImpl(intervalRoundingInSecs, countersNames)
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

    private def resetCounter(): Unit = {
      firstOccured = 0L
      lastOccured = 0L
      // loop thru intervalAndKeyVelocityCounts and clear. Do we really need it?
      intervalAndKeyVelocityCounts.clear()
    }

    def duplicate(reset: Boolean): VelocityMetricsInstanceInterface = synchronized {
      val m = new VelocityMetricsInstanceImpl(intervalRoundingInSecs, countersNames)
      try {
        m.firstOccured = this.firstOccured
        m.lastOccured = this.lastOccured
        intervalAndKeyVelocityCounts.foreach(kv => {
          m.intervalAndKeyVelocityCounts(kv._1) = kv._2.duplicate
        })

        if (reset) {
          resetCounter
        }
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
        if (m.firstOccured == 0 || m.firstOccured > this.firstOccured) m.firstOccured = this.firstOccured
        if (m.lastOccured == 0 || m.lastOccured < this.lastOccured) m.lastOccured = this.lastOccured

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
          resetCounter
        }
      } catch {
        case e1: Exception => logger.error("Failed to addTo", e1)
        case e2: Throwable => logger.error("Failed to addTo", e2)
      }
      return
    }
  }

  private[VelocityMetrics] class ComponentVelocityMetrics(val nodeId: String, val componentKey: String, intervalRoundingInSecs: Int, countersNames: Array[String]) {
    private val instanceMetrics = ArrayBuffer[VelocityMetricsInstanceImpl]()

    def GetVelocityMetricsInstance(): VelocityMetricsInstanceInterface = synchronized {
      val m = new VelocityMetricsInstanceImpl(intervalRoundingInSecs, countersNames)
      instanceMetrics += m
      m
    }

    def duplicate(): ComponentVelocityMetrics = synchronized {
      val comp = new ComponentVelocityMetrics(nodeId, componentKey, intervalRoundingInSecs, countersNames)
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
      try {
        val allComponents = GetAllMetricsComponents(reset)
        val uuid = guid

        val finalComponentsMetrics = ArrayBuffer[ComponentMetrics]()
        allComponents.foreach(comp => {
          val allMetricInstances = comp.getVelocityMetricsInstanceInterfaces()
          if (allMetricInstances.size > 0) {
            val finalMetrics = allMetricInstances(0).duplicate(reset)
            for (i <- 1 until allMetricInstances.size) {
              allMetricInstances(i).addTo(finalMetrics, reset)
            }

            val compMetrics = new ComponentMetrics
            compMetrics.componentKey = comp.componentKey
            compMetrics.nodeId = comp.nodeId
            compMetrics.keyMetrics = finalMetrics.asInstanceOf[VelocityMetricsInstanceImpl].intervalAndKeyVelocityCounts.values.map(velocityCounts => {
              val keyMetrics = new ComponentKeyMetrics
              keyMetrics.key = velocityCounts.intervalAndKey.key
              keyMetrics.metricsTime = velocityCounts.intervalAndKey.metricsTime
              keyMetrics.roundIntervalTimeInSec = velocityCounts.intervalAndKey.intervalRoundingInSecs
              keyMetrics.firstOccured = velocityCounts.firstOccured
              keyMetrics.lastOccured = velocityCounts.lastOccured
              keyMetrics.metricValues = new Array[MetricValue](velocityCounts.counters.size)

              for (i <- 0 until velocityCounts.counters.size) {
                keyMetrics.metricValues(i) = new MetricValue(velocityCounts.countersNames(i), velocityCounts.counters(i))
              }
              keyMetrics
            }).toArray

            finalComponentsMetrics += compMetrics
          }
        })

        val metrics = new Metrics
        metrics.metricsGeneratedTimeInMs = System.currentTimeMillis()
        metrics.uuid = uuid
        metrics.compMetrics = finalComponentsMetrics.toArray

        val callbacks = getAllEmitListeners
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
      } catch {
        case e: Exception => {
          logger.error("Failed to send velocity metrics", e)
        }
        case e: Throwable => {
          logger.error("Failed to send velocity metrics", e)
        }
      }
    }

    val externalizeMetrics = new Runnable() {
      def run() {
        try {
          if (!isTerminated) {
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
          }
        } catch {
          case e1: Exception => logger.error("Failed to send metrics", e1)
          case e2: Throwable => logger.error("Failed to send metrics", e2)
        }
      }
    }

    private val initalDelay = (((System.currentTimeMillis() + emitTmInSecs * 1000) / rotationTimeInMilliSecs) * rotationTimeInMilliSecs) - System.currentTimeMillis()

    scheduledThreadPool.scheduleWithFixedDelay(externalizeMetrics, initalDelay, emitTmInSecs * 1000, TimeUnit.MILLISECONDS)

    private def GetAllMetricsComponents(reset: Boolean): Array[ComponentVelocityMetrics] = synchronized {
      val retVal = metricsComponents.values.toArray
      retVal
    }

    private def GetMetricsComponent(componentKey: String, nodeId: String, intervalRoundingInSecs: Int, countersNames: Array[String], addIfMissing: Boolean): ComponentVelocityMetrics = synchronized {
      val compKey = componentKey.trim.toLowerCase
      var comp = metricsComponents.getOrElse(compKey, null)
      if (comp == null && addIfMissing) {
        comp = new ComponentVelocityMetrics(nodeId.trim.toLowerCase, compKey, intervalRoundingInSecs: Int, countersNames: Array[String])
        if (comp != null)
          metricsComponents(compKey) = comp
      }
      comp
    }

    override def GetVelocityMetricsInstance(nodeId: String, componentKey: String, intervalRoundingInSecs: Int, countersNames: Array[String]): VelocityMetricsInstanceInterface = {
      if (isTerminated) {
        throw new Exception("VelocityMetricsFactoryInterface is already terminated.")
      }
      val comp = GetMetricsComponent(componentKey, nodeId, intervalRoundingInSecs, countersNames, true)
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

    private def getAllEmitListeners(): Array[VelocityMetricsCallback] = synchronized {
      callbackListeners.toArray
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

  def GetExistingVelocityMetricsFactory(): VelocityMetricsFactoryInterface = {
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
  }
}


