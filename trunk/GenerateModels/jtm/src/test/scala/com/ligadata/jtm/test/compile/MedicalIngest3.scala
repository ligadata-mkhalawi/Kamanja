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
package com.ligadata.models.samples.medical3.V1
import com.ligadata.KamanjaBase._
import com.ligadata.KvBase.TimeRange
import com.ligadata.kamanja.metadata.ModelDef
import com.ligadata.runtime._
import com.ligadata.runtime.Conversion
// Package code start
// Package code end
class TransactionIngestFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {
  // Factory code start
  // Factory code end
  override def createModelInstance(): ModelInstance = return new TransactionIngest(this)
  override def getModelName: String = "com.ligadata.models.samples.medical3.TransactionIngest"
  override def getVersion: String = "0.0.1"
  override def createResultObject(): ModelResultBase = new MappedModelResults()
}
class TransactionIngest(factory: ModelInstanceFactory) extends ModelInstance(factory) {
  val conversion = new com.ligadata.runtime.Conversion
  val log = new com.ligadata.runtime.Log(this.getClass.getName)
  val context = new com.ligadata.runtime.JtmContext
  import log._
  // Model code start
  // Model code end
  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
    if (isTraceEnabled)
      Trace(s"Model::execute transid=%d triggeredset=%d outputdefault=%s".format(txnCtxt.transId, triggerdSetIndex, outputDefault.toString))
    if(isDebugEnabled)
    {
      execMsgsSet.foreach(m => Debug( s"Input: %s -> %s".format(m.getFullTypeName, m.toString())))
    }
    // Grok parts
    // Model methods
    def exeGenerated_transactionmsg_1(msg1: com.ligadata.kamanja.samples.messages.V1000000.TransactionMsgIn): Array[MessageInterface] = {
      Debug("exeGenerated_transactionmsg_1")
      context.SetSection("transactionmsg")
      // Split the incoming data
      val arraydata: Array[String] = msg1.data.split(",")
      // extract the type
      val typeName: String = arraydata(0)
      def process_o2(): Array[MessageInterface] = {
        Debug("exeGenerated_transactionmsg_1::process_o2")
        context.SetScope("o2")
        try {
          if (!("com.ligadata.kamanja.samples.messages.HL7" == typeName)) {
            Debug("Filtered: transactionmsg@o2")
            return Array.empty[MessageInterface]
          }
          val result = com.ligadata.kamanja.samples.messages.V1000000.HL71.createInstance
          result.set("desynpuf_id", arraydata(1))
          result.set("clm_id", arraydata(2))
          result.set("clm_from_dt", arraydata(3))
          result.set("clm_thru_dt", arraydata(4))
          if(context.CurrentErrors()==0) {
            Array(result)
          } else {
            Array.empty[MessageInterface]
          }
        } catch {
          case e: AbortOutputException => {
            context.AddError(e.getMessage)
            return Array.empty[MessageInterface]
          }
          case e: Exception => {
            Debug("Exception: o2:" + e.getMessage)
            throw e
          }
        }
      }
      try {
        process_o2()
      } catch {
        case e: AbortTransformationException => {
          return Array.empty[MessageInterface]
        }
      }
    }
    // Evaluate messages
    val msgs = execMsgsSet.map(m => m.getFullTypeName -> m).toMap
    val msg1 = msgs.getOrElse("com.ligadata.kamanja.samples.messages.TransactionMsgIn", null).asInstanceOf[com.ligadata.kamanja.samples.messages.V1000000.TransactionMsgIn]
    // Main dependency -> execution check
    // Create result object
    val results: Array[MessageInterface] =
      try {
        (if(msg1!=null) {
          exeGenerated_transactionmsg_1(msg1)
        } else {
          Array.empty[MessageInterface]
        }) ++
          Array.empty[MessageInterface]
      } catch {
        case e: AbortExecuteException => {
          Array.empty[MessageInterface]
        }
      }
    if(isDebugEnabled)
    {
      results.foreach(m => Debug( s"Output: %s -> %s".format(m.getFullTypeName, m.toString())))
    }
    results.asInstanceOf[Array[ContainerOrConcept]]
  }
}
