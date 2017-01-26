/*
 * Copyright 2016 ligaDATA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ligadata.VelocityMetrics

import org.scalatest._
import org.apache.log4j.LogManager

class PrintVelocityMetrics extends VelocityMetricsCallback {
  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)

  override def call(metrics: Metrics): Unit = {
    if (metrics == null) {
      logger.error("Got null Metrics in callback")
    } else {
      val sb = new StringBuilder()
      val compMetrics = if (metrics.compMetrics == null) Array[ComponentMetrics]() else metrics.compMetrics
      logger.error("Metrics => MetricsGeneratedTimeInMs:%d, UUID:%s, ComponentMetrics:%d".format(metrics.metricsGeneratedTimeInMs, metrics.uuid, compMetrics.size))
      for (i <- 0 until compMetrics.length) {
        val keyMetrics = if (compMetrics(i).keyMetrics == null) Array[ComponentKeyMetrics]() else compMetrics(i).keyMetrics
        logger.error("\tComponentMetrics(%d) => ComponentKey:%s, NodeId:%s, KeyMetrics:%d".format(i, compMetrics(i).componentKey, compMetrics(i).nodeId, keyMetrics.size))
        for (j <- 0 until keyMetrics.length) {
          val metricValues = if (keyMetrics(j).metricValues == null) Array[MetricValue]() else keyMetrics(j).metricValues
          sb.clear
          for (k <- 0 until metricValues.size) {
            if (k > 0)
              sb.append(",");
            sb.append(metricValues(k).Key())
            sb.append(":");
            sb.append(metricValues(k).Value());
          }
          logger.error("\t\tComponentKeyMetrics(%d) => Key:%s, MetricsTime:%d, FirstOccured:%d, LastOccured:%d, MetricValue:{%s}".format(j, keyMetrics(j).key, keyMetrics(j).metricsTime, keyMetrics(j).firstOccured, keyMetrics(j).lastOccured, sb.toString))
        }
      }
    }
  }
}

class VelocityMetricsSpec extends FunSpec {
  private val loggerName = this.getClass.getName
  private val logger = LogManager.getLogger(loggerName)

  describe("Unit Tests for VelocityMetrics") {
    it("Metrics Tests") {
      val nodeId: String = "1"
      val counterNames1 = Array("Counter1", "Counter2")
      val counterNames2 = Array("Counter1", "Counter2", "Counter3")

      val printVelocityMetrics = new PrintVelocityMetrics

      val factory = VelocityMetrics.GetVelocityMetricsFactory(30, 5)

      factory.addEmitListener(printVelocityMetrics)

      val instance1 = factory.GetVelocityMetricsInstance(nodeId, "TestMetrics1", 5, counterNames1)

      val instance2 = factory.GetVelocityMetricsInstance(nodeId, "TestMetrics1", 10, counterNames1)

      val instance3 = factory.GetVelocityMetricsInstance(nodeId, "TestMetrics1", 10, counterNames1)

      instance1.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), true, false)
      instance1.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), true, true)
      instance1.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), false, false)
      instance1.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), false, true)

      instance2.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), true, false)
      instance2.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), true, true)
      instance2.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), false, false)
      instance2.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), false, true)

      instance3.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), true, false)
      instance3.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), true, true)
      instance3.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), false, false)
      instance3.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), false, true)

      // Sleeping for 45secs
      logger.info("Sleeping for 45secs")
      Thread.sleep(45000)

      instance1.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), true, false)
      instance1.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), true, true)
      instance1.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), false, false)
      instance1.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), false, true)

      instance2.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), true, false)
      instance2.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), true, true)
      instance2.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), false, false)
      instance2.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), false, true)

      instance3.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), true, false)
      instance3.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), true, true)
      instance3.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), false, false)
      instance3.increment(System.currentTimeMillis(), "Metrics1", System.currentTimeMillis(), false, true)

      val instance4 = factory.GetVelocityMetricsInstance(nodeId, "TestMetrics2", 5, counterNames2)

      val instance5 = factory.GetVelocityMetricsInstance(nodeId, "TestMetrics2", 10, counterNames2)

      val instance6 = factory.GetVelocityMetricsInstance(nodeId, "TestMetrics2", 10, counterNames2)

      instance4.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), true)
      instance4.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), true, true, true)
      instance4.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), false, true)
      instance4.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), false, true)
      instance4.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), false, false, true)
      instance4.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), false, false, true)
      instance4.Add(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), Array[Int](0, 2), Array[Int](100, 100))
      instance4.Add(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), Array[Int](0, 1), Array[Int](50, 50))

      instance5.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), true)
      instance5.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), true, true, true)
      instance5.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), false, true)
      instance5.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), false, true)
      instance5.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), false, false, true)
      instance5.Add(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), Array[Int](0, 2), Array[Int](100, 100))
      instance5.Add(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), Array[Int](0, 1), Array[Int](50, 50))

      instance6.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), true)
      instance6.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), true, true, true)
      instance6.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), false, true)
      instance6.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), false, true)
      instance6.increment(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), false, false, true)
      instance6.Add(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), Array[Int](0, 2), Array[Int](100, 100))
      instance6.Add(System.currentTimeMillis(), "Metrics2", System.currentTimeMillis(), Array[Int](0, 1), Array[Int](50, 50))

      // Sleeping for 45secs
      logger.info("Sleeping for 45secs")
      Thread.sleep(45000)

      factory.shutdown()
    }
  }
}
