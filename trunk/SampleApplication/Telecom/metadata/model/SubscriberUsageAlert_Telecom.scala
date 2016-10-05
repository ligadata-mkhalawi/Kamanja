/*
 * Copyright 2015 ligaDATA
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

// The following model generates an alert when an individual subscriber or account with sharedplan exceed usage
// above threshold as defined by a rate plan
// The following containers are used for lookup
// SubscriberGlobalPreferences : defines the thresholds in terms of percentage values
//                               at which we choose to call the message a warning or alert
// SubscriberInfo: individual subscriber information such as his phone number(msisdn), account number
//                 kind of ratePlan, activation date, preference to be notified or not
// SubscriberPlans: rate plan information such as kind of plan(shared or individual), plan limit, individual limit if any
// SubscriberAggregatedUsage: An object that maintains the aggregated usage for a given subscriber and for the current month
//                            Current Month is defined as whatever the month of the day, irrespective of activation date
//                            require enhancements if we aggregate the usage over each 30 days after activation
// AccountAggregatedUsage: An object that maintains the aggregated usage for a given account(could have
//                             multiple subscribers) and for the current month
//                            Current Month is defined as whatever the month of the day, irrespective of activation date
//                            require enhancements if we aggregate the usage over each 30 days after activation
// AccountInfo: individual account information such as account number, preference to be notified or not
//               Every subscriber is associated with an account
//     
package com.ligadata.models.samples.models

import com.ligadata.KamanjaBase._
import RddUtils._
import RddDate._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.apache.logging.log4j.{Logger, LogManager}
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import java.util.Locale
import java.io._
import com.ligadata.kamanja.metadata.ModelDef;

class SubscriberUsageAlertFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {
  // override def isValidMessage(msg: MessageContainerBase): Boolean = return msg.isInstanceOf[SubscriberUsage]
  override def createModelInstance(): ModelInstance = return new SubscriberUsageAlert(this)

  override def getModelName(): String = "com.ligadata.models.samples.models.SubscriberUsageAlert"

  // Model Name
  override def getVersion(): String = "0.0.1" // Model Version
  // override def createResultObject(): ModelResultBase = new SubscriberUsageAlertResult()
}

class SubscriberUsageAlert(factory: ModelInstanceFactory) extends ModelInstance(factory) {
  lazy val loggerName = this.getClass.getName
  lazy val logger = LogManager.getLogger(loggerName)
  val df = DateTimeFormat.forPattern("yyyyMMdd").withLocale(Locale.US)

  private def getMonth(dt: String): Int = {
    val jdt = DateTime.parse(dt, df)
    jdt.monthOfYear().get()
  }

  private def getCurrentMonth: Int = {
    val jdt = new DateTime()
    jdt.monthOfYear().get()
  }

  private def dumpAppLog(logStr: String) = {
    val fw = new FileWriter("SubscriberUsageAlertAppLog.txt", true)
    try {
      fw.write(logStr + "\n")
    }
    finally fw.close()
  }

  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
    // Make sure current transaction has some data
    if (execMsgsSet.size == 0) {
      return Array[ContainerOrConcept]()
    }
    val curMsg = execMsgsSet(0).asInstanceOf[SubscriberUsage]

    // Get the current subscriber, account info and global preferences
    val gPref = SubscriberGlobalPreferences.getRecentOrNew(Array("Type1"))
    val subInfo = SubscriberInfo.getRecentOrNew(Array(curMsg.msisdn.toString))
    val actInfo = AccountInfo.getRecentOrNew(Array(subInfo.actno))
    val planInfo = SubscriberPlans.getRecentOrNew(Array(subInfo.planname))

    var logTag = "SubscriberUsageAlertApp(" + subInfo.msisdn + "," + actInfo.actno + "): "

    // Get current values of aggregatedUsage
    val subAggrUsage = SubscriberAggregatedUsage.getRecentOrNew(Array(subInfo.msisdn.toString))
    val actAggrUsage = AccountAggregatedUsage.getRecentOrNew(Array(actInfo.actno))

    //dumpAppLog(logTag + "Before: Subscriber current month usage => " + subAggrUsage.thismonthusage + ",Account current month usage => " + actAggrUsage.thismonthusage)

    // Get current month
    val curDtTmInMs = RddDate.currentGmtDateTime
    val txnMonth = getMonth(curMsg.date.toString)
    val currentMonth = getCurrentMonth

    // planLimit values are supplied as GB. But SubscriberUsage record contains the usage as MB
    // So convert planLimit to MB
    val planLimit = planInfo.planlimit * 1000
    val indLimit = planInfo.individuallimit * 1000


    //dumpAppLog(logTag + "Subscriber plan name => " + subInfo.planname + ",plan type => " + planInfo.plantype + ",plan limit => " + planLimit + ",individual limit => " + indLimit)
    dumpAppLog(logTag + "Subscriber usage in the current transaction  => " + curMsg.usage)

    // we are supposed to check whether the usage belongs to current month
    // if the usage doesn't belong to this month, we are supposed to ignore it
    // Here we let all the data pass through just to generate sample alerts no matter
    // what the actual usage data is
    //if( txnMonth != currentMonth ){
    //dumpAppLog(logTag + "The month value " + txnMonth + " is either older than current month " + currentMonth + " or incorrect,transaction ignored " + txnMonth)
    //  return null
    //}

    // aggregate account usage
    val actMonthlyUsage = actAggrUsage.thismonthusage + curMsg.usage
    actAggrUsage.set("thismonthusage", actMonthlyUsage);
    actAggrUsage.save;

    // aggregate the usage 
    // aggregate individual subscriber usage
    val subMonthlyUsage = subAggrUsage.thismonthusage + curMsg.usage
    subAggrUsage.set("thismonthusage", subMonthlyUsage);
    subAggrUsage.save


    dumpAppLog(logTag + "After Aggregation: Subscriber current month usage => " + subMonthlyUsage + ",Account current month usage => " + actMonthlyUsage)

    val curTmInMs = curDtTmInMs.getDateTimeInMs

    // generate alerts if plan limits are exceeded based on planType
    planInfo.plantype match {
      case 1 => {
        // shared plans
        // exceeded plan limit
        if (actMonthlyUsage > planLimit) {
          if (actInfo.thresholdalertoptout == false) {
            dumpAppLog(logTag + "Creating Alert for a shared plan account " + actInfo.actno)
            dumpAppLog(logTag + "---------------------------")
            val msg = AccountUsageAlertMessage.createInstance()
            msg.actno = actInfo.actno;
            msg.curusage = actMonthlyUsage;
            msg.alerttype = "pastThresholdAlert";
            msg.triggertime = curTmInMs;
            return Array(msg)
          }
        }
      }
      case 2 => {
        // individual plans
        // individual plan,  individual limit may have been exceeded
        if (subMonthlyUsage > indLimit) {
          if (subInfo.thresholdalertoptout == false) {
            dumpAppLog(logTag + "Creating alert for individual subscriber account " + curMsg.msisdn)
            dumpAppLog(logTag + "---------------------------")
            val msg = SubscriberUsageAlertMessage.createInstance()
            msg.msisdn = curMsg.msisdn;
            msg.curusage = subMonthlyUsage;
            msg.alerttype = "pastThresholdAlert";
            msg.triggertime = curTmInMs;
            return Array(msg)
          }
        }
      }
      case _ => {
        // unsupported plan type
        //dumpAppLog("Unknown planType => " + planInfo.plantype)
      }
    }
    return Array[ContainerOrConcept]()
  }
}
