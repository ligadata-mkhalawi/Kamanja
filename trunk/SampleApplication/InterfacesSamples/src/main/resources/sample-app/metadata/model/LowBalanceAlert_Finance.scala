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

package com.ligadata.models.samples.models

import com.ligadata.KamanjaBase._
import RddUtils._
import RddDate._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import java.io.{ DataInputStream, DataOutputStream }
import org.apache.logging.log4j.{ Logger, LogManager }
import com.ligadata.kamanja.metadata.ModelDef;

class LowBalanceAlertFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {
  // override def isValidMessage(msg: MessageContainerBase): Boolean = return msg.isInstanceOf[TransactionMsg]
  override def createModelInstance(): ModelInstance = return new LowBalanceAlert(this)
  override def getModelName(): String = "LowBalanceAlert" // Model Name
  override def getVersion(): String = "0.0.1" // Model Version
  override def createResultObject(): ModelResultBase = new MappedModelResults()
}

class LowBalanceAlert(factory: ModelInstanceFactory) extends ModelInstance(factory) {
  // private[this] val LOG = LogManager.getLogger(getClass);
  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
    // Make sure current transaction has some data
    if (execMsgsSet.size == 0) {
      return Array[ContainerOrConcept]()
    }
    val curMsg = execMsgsSet(0).asInstanceOf[TransactionMsg]

    // First check the preferences and decide whether to continue or not
    val gPref = GlobalPreferences.getRecentOrNew(Array("Type1"))
    val pref = CustPreferences.getRecentOrNew
    if (pref.minbalancealertoptout == true) {
      return null
    }

    // Check if at least min number of hours elapsed since last alert  
    val curDtTmInMs = RddDate.currentGmtDateTime
    val alertHistory = CustAlertHistory.getRecentOrNew
    if (curDtTmInMs.timeDiffInHrs(RddDate(alertHistory.alertdttminms)) < gPref.minalertdurationinhrs) {
      return null
    }

    if (curMsg.balance >= gPref.minalertbalance) {
      return null
    }

    val curTmInMs = curDtTmInMs.getDateTimeInMs
    // create new alert history record and persist (if policy is to keep only one, this will replace existing one)
    val ah = CustAlertHistory.build;
    ah.set("alertdttminms",curTmInMs);
    ah.set("alerttype","lowbalancealert");
    ah.save;

    //CustAlertHistory.build.withalertdttminms(curTmInMs).withalerttype("lowbalancealert").Save
    // results
	val msg = LowBalanceAlertOutputMsg.createInstance().asInstanceOf[LowBalanceAlertOutputMsg]
	msg.custid = curMsg.custid;
	msg.branchid = curMsg.branchid;
	msg.accno = curMsg.accno;
	msg.curbalance = curMsg.balance;
	msg.alerttype = "lowBalanceAlert";
	msg.triggertime = curTmInMs;
	return Array(msg)
  }
}

