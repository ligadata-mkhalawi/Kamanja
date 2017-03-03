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

package com.ligadata.kamanja.logicalpartition.test.models;

import com.google.common.base.Optional;
import com.ligadata.KamanjaBase.*;
import com.ligadata.kamanja.metadata.ModelDef;

public class MarathonModel extends ModelInstance {
	public MarathonModel(ModelInstanceFactory factory) {
    	super(factory);
    }

	public ContainerOrConcept[] execute(TransactionContext txnCtxt, ContainerOrConcept[] execMsgsSet, int matchedInputSetIndex, boolean outputDefault) {
		marathon_in_msg input = (marathon_in_msg) execMsgsSet[0];  // This run should trigger when we have only msg1
		if(input.ranking()>4)
			return null;
		marathon_out_msg output = (marathon_out_msg) marathon_out_msg.createInstance();
		output.set(0, input.ranking());
		output.set(1, input.name());
		ContainerInterface[] returnArr = new ContainerInterface[1];
		returnArr[0] = output;
        return returnArr;
  }

    /**
     * @param inTxnContext
     */

    public static class MarathonModelFactory extends ModelInstanceFactory {
		public MarathonModelFactory(ModelDef modelDef, NodeContext nodeContext) {
			super(modelDef, nodeContext);
		}

		public ModelInstance createModelInstance() {
			return new MarathonModel(this);
		}

		public String getModelName() {
			return "com.ligadata.kamanja.logicalpartition.test.models.MarathonModel";
		}

		public String getVersion() {
			return "0.0.5";
		}
	}

}
