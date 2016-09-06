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

package com.ligadata.kamanja.samples.models;

import com.google.common.base.Optional;
import com.ligadata.KamanjaBase.*;
import com.ligadata.kamanja.metadata.ModelDef;

public class LoanRisk extends ModelInstance {

	
	public LoanRisk(ModelInstanceFactory factory) {
    	super(factory);
    }

	public ContainerOrConcept[] execute(TransactionContext txnCtxt, ContainerOrConcept[] execMsgsSet, int matchedInputSetIndex, boolean outputDefault) {
		msg1 LoanRisk = (msg1) execMsgsSet[0];  // This run should trigger when we have only msg1
		float pred = -1.49786052275209 ;
                pred += LoanRisk.TIN_IMO_REASON_DebtCon() * -0.239668692936379
                pred += LoanRisk.TIN_IZR_JOB_Mgr() * 1.37932908138322
                pred += LoanRisk.TIN_IZR_JOB_Office() * 0.775391491255473
                pred += LoanRisk.TIN_IZR_JOB_Other() * 1.44907154775426
                pred += LoanRisk.TIN_IZR_JOB_ProfExe() * 1.40707813621254
                pred += LoanRisk.TIN_IZR_JOB_Sales() * 2.38272021895632
                pred += LoanRisk.TIN_IZR_JOB_Self() * 1.97738454381451
                pred += LoanRisk.R01_LOAN() * -1.98016770987421
                pred += LoanRisk.IMN_R01_MORTDUE() * -1.70893083156384
                pred += LoanRisk.IMN_R01_VALUE() * 46.8834972899893
                pred += LoanRisk.IMN_R01_YOJ() * -0.610183852644824
                pred += LoanRisk.IMN_R01_DEROG() * 4.47856184348009
                pred += LoanRisk.IMN_R01_DELINQ() * 7.37620839328494
                pred += LoanRisk.IMN_R01_CLAGE() * -6.85708989347486
                pred += LoanRisk.IMN_R01_NINQ() * 2.89418521536115
                pred += LoanRisk.IMN_R01_CLNO() * -0.296037079316927
                pred += LoanRisk.LOG6_DEROG() * 0.390988196099627
                pred += LoanRisk.LOG5_DELINQ() * 1.8255237489947
                pred += LoanRisk.LOG_VALUE() * -35.3593065292401
		
		outmsg1 output = (outmsg1) outmsg1.createInstance();
		output.set(0, LoanRisk.rec_ID());
		output.set(pred, LoanRisk.PYTHON_RISK_SCORE());
		returnArr[0] = output;
        return returnArr;
  }

    /**
     * @param inTxnContext
     */

    public static class LoanRiskFactory extends ModelInstanceFactory {
		public LoanRiskFactory(ModelDef modelDef, NodeContext nodeContext) {
			super(modelDef, nodeContext);
		}

		public ModelInstance createModelInstance() {
			return new LoanRisk(this);
		}

		public String getModelName() {
			return "com.ligadata.kamanja.samples.models.LoanRisk";
		}

		public String getVersion() {
			return "0.0.1";
		}
	}

}










