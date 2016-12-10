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

public class Add extends ModelInstance {
	public Add(ModelInstanceFactory factory) {
    	super(factory);
    }

	public ContainerOrConcept[] execute(TransactionContext txnCtxt, ContainerOrConcept[] execMsgsSet, int matchedInputSetIndex, boolean outputDefault) {
		msg1 add = (msg1) execMsgsSet[0];  // This run should trigger when we have only msg1
		outmsg1 output = (outmsg1) outmsg1.createInstance();
		output.set(0, add.a());
		output.set(1, add.b());
		ContainerInterface[] returnArr = new ContainerInterface[1];
		returnArr[0] = output;
        return returnArr;
  }

    /**
     * @param inTxnContext
     */

    public static class AddFactory extends ModelInstanceFactory {
		public AddFactory(ModelDef modelDef, NodeContext nodeContext) {
			super(modelDef, nodeContext);
		}

		public ModelInstance createModelInstance() {
			return new Add(this);
		}

		public String getModelName() {
			return "com.ligadata.kamanja.samples.models.Add";
		}

		public String getVersion() {
			return "0.0.1";
		}
	}

}










