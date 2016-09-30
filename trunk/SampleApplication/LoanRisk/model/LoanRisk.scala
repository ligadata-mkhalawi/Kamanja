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

package com.ligadata.samples.models

import com.ligadata.KamanjaBase._
import com.ligadata.kamanja.metadata.ModelDef

import scala.math


class LoanRiskFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {
  override def createModelInstance(): ModelInstance = return new LoanRisk(this)
  override def getModelName: String = "com.ligadata.kamanja.samples.models.LoanRisk" 
  override def getVersion: String = "0.0.1"
}

class LoanRisk(factory: ModelInstanceFactory) extends ModelInstance(factory) {
   override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
       var loanRisk : msg1 =  execMsgsSet(0).asInstanceOf[msg1] // This run should trigger when we have only msg1
       var pred : Float = -1.49786052275209
       pred += loanRisk.TIN_IMO_REASON_DebtCon *  -0.239668692936379
       pred += loanRisk.TIN_IZR_JOB_Mgr *  1.37932908138322 
       pred += loanRisk.TIN_IZR_JOB_Office * 0.775391491255473
       pred += loanRisk.TIN_IZR_JOB_Other * 1.44907154775426
       pred += loanRisk.TIN_IZR_JOB_ProfExe * 1.40707813621254
       pred += loanRisk.TIN_IZR_JOB_Sales * 2.38272021895632
       pred += loanRisk.TIN_IZR_JOB_Self * 1.97738454381451
       pred += loanRisk.R01_LOAN * -1.98016770987421
       pred += loanRisk.IMN_R01_MORTDUE * -1.70893083156384
       pred += loanRisk.IMN_R01_VALUE * 46.8834972899893
       pred += loanRisk.IMN_R01_YOJ * -0.610183852644824
       pred += loanRisk.IMN_R01_DEROG * 4.47856184348009
       pred += loanRisk.IMN_R01_DELINQ * 7.37620839328494
       pred += loanRisk.IMN_R01_CLAGE * -6.85708989347486
       pred += loanRisk.IMN_R01_NINQ * 2.89418521536115
       pred += loanRisk.IMN_R01_CLNO * -0.296037079316927
       pred += loanRisk.LOG6_DEROG * 0.390988196099627
       pred += loanRisk.LOG5_DELINQ * 1.8255237489947
       pred += loanRisk.LOG_VALUE * -35.3593065292401
       pred = (math.exp(pred) / (1 + math.exp(pred)) 
       output.PYTHON_RISK_SCORE = pred
       output.rec_id  = loanRisk.rec_id 
       return Array(output);
   }
}
