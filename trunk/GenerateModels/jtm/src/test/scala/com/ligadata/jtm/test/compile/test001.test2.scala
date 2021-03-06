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
package com.ligadata.jtm.test.filter2.V1
import com.ligadata.KamanjaBase._
import com.ligadata.KvBase.TimeRange
import com.ligadata.kamanja.metadata.ModelDef
import com.ligadata.runtime._
import com.ligadata.Utils._
// Package code start
// Package code end
class ModelFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {
  // Factory code start
  // Factory code end
  override def createModelInstance(): ModelInstance = return new Model(this)
  override def getModelName: String = "com.ligadata.jtm.test.filter2.Model"
  override def getVersion: String = "0.0.1"
  override def createResultObject(): ModelResultBase = new MappedModelResults()
  override def isModelInstanceReusable(): Boolean = true;
}
class common_exeGenerated_test1_1(conversion: com.ligadata.runtime.Conversion,
                                  log: com.ligadata.runtime.Log,
                                  context: com.ligadata.runtime.JtmContext,
                                  msg1: com.ligadata.kamanja.test.V1000000.msg1) {
  import log._
  // in scala, type could be optional
  val out3: Int = msg1.in1 + 1000
}
class common_exeGenerated_test1_1_process_o1(conversion: com.ligadata.runtime.Conversion,
                                             log : com.ligadata.runtime.Log,
                                             context: com.ligadata.runtime.JtmContext,
                                             common: common_exeGenerated_test1_1,
                                             msg1: com.ligadata.kamanja.test.V1000000.msg1) {
  import log._
  import common._
  val result: Array[MessageInterface]= try {
    if (!(!(msg1.in2 != -1 && msg1.in2 < 100))) {
      Debug("Filtered: test1@o1")
      Array.empty[MessageInterface]
    } else {
      val t1: String = "s:" + msg1.in2.toString()
      val result = com.ligadata.kamanja.test.V1000000.msg2.createInstance
      result.out4 = msg1.in3
      result.out3 = msg1.in2
      result.out2 = t1
      result.out1 = msg1.in1
      if (result.hasTimePartitionInfo) result.setTimePartitionData ;
      if(context.CurrentErrors()==0) {
        Array(result)
      } else {
        Array.empty[MessageInterface]
      }
    }
  } catch {
    case e: AbortOutputException => {
      context.AddError(e.getMessage)
      Array.empty[MessageInterface]
    }
    case e: Exception => {
      Debug("Exception: o1:" + e.getMessage)
      throw e
    }
  }
}
class common_exeGenerated_test1_2(conversion: com.ligadata.runtime.Conversion,
                                  log: com.ligadata.runtime.Log,
                                  context: com.ligadata.runtime.JtmContext,
                                  msg2: com.ligadata.kamanja.test.V1000000.msg3) {
  import log._
  // in scala, type could be optional
  val out3: Int = msg2.in1 + 2000
}
class common_exeGenerated_test1_2_process_o1(conversion: com.ligadata.runtime.Conversion,
                                             log : com.ligadata.runtime.Log,
                                             context: com.ligadata.runtime.JtmContext,
                                             common: common_exeGenerated_test1_2,
                                             msg2: com.ligadata.kamanja.test.V1000000.msg3) {
  import log._
  import common._
  val result: Array[MessageInterface]= try {
    if (!(!(msg2.in2 != -1 && msg2.in2 < 100))) {
      Debug("Filtered: test1@o1")
      Array.empty[MessageInterface]
    } else {
      val t1: String = "s:" + msg2.in2.toString()
      val result = com.ligadata.kamanja.test.V1000000.msg2.createInstance
      result.out4 = msg2.in3
      result.out3 = msg2.in2
      result.out2 = t1
      result.out1 = msg2.in1
      if (result.hasTimePartitionInfo) result.setTimePartitionData ;
      if(context.CurrentErrors()==0) {
        Array(result)
      } else {
        Array.empty[MessageInterface]
      }
    }
  } catch {
    case e: AbortOutputException => {
      context.AddError(e.getMessage)
      Array.empty[MessageInterface]
    }
    case e: Exception => {
      Debug("Exception: o1:" + e.getMessage)
      throw e
    }
  }
}
class Model(factory: ModelInstanceFactory) extends ModelInstance(factory) {
  val conversion = new com.ligadata.runtime.Conversion
  val log = new com.ligadata.runtime.Log(this.getClass.getName)
  val context = new com.ligadata.runtime.JtmContext
  import log._
  // Model code start
  // Model code end
  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
    context.Reset(); // Resetting the JtmContext before executing the model
    if (isTraceEnabled)
      Trace(s"Model::execute transid=%d triggeredset=%d outputdefault=%s".format(txnCtxt.transId, triggerdSetIndex, outputDefault.toString))
    if(isDebugEnabled)
    {
      execMsgsSet.foreach(m => Debug( s"Input: %s -> %s".format(m.getFullTypeName, m.toString())))
    }
    // Grok parts
    // Model methods
    def exeGenerated_test1_1(msg1: com.ligadata.kamanja.test.V1000000.msg1): Array[MessageInterface] = {
      Debug("exeGenerated_test1_1")
      context.SetSection("test1")
      val common = new common_exeGenerated_test1_1(conversion, log, context, msg1)
      def process_o1(): Array[MessageInterface] = {
        Debug("exeGenerated_test1_1::process_o1")
        context.SetScope("o1")
        val result = new common_exeGenerated_test1_1_process_o1(conversion, log, context, common, msg1)
        result.result
      }
      try {
        process_o1()
      } catch {
        case e: AbortTransformationException => {
          return Array.empty[MessageInterface]
        }
      }
    }
    def exeGenerated_test1_2(msg2: com.ligadata.kamanja.test.V1000000.msg3): Array[MessageInterface] = {
      Debug("exeGenerated_test1_2")
      context.SetSection("test1")
      val common = new common_exeGenerated_test1_2(conversion, log, context, msg2)
      def process_o1(): Array[MessageInterface] = {
        Debug("exeGenerated_test1_2::process_o1")
        context.SetScope("o1")
        val result = new common_exeGenerated_test1_2_process_o1(conversion, log, context, common, msg2)
        result.result
      }
      try {
        process_o1()
      } catch {
        case e: AbortTransformationException => {
          return Array.empty[MessageInterface]
        }
      }
    }
    // Evaluate messages
    val msgs = execMsgsSet.map(m => m.getFullTypeName -> m).toMap
    val msg1 = msgs.getOrElse("com.ligadata.kamanja.test.msg1", null).asInstanceOf[com.ligadata.kamanja.test.V1000000.msg1]
    val msg2 = msgs.getOrElse("com.ligadata.kamanja.test.msg3", null).asInstanceOf[com.ligadata.kamanja.test.V1000000.msg3]
    // Main dependency -> execution check
    // Create result object
    val results: Array[MessageInterface] =
    try {
      (if(msg1!=null) {
        exeGenerated_test1_1(msg1)
      } else {
        Array.empty[MessageInterface]
      }) ++
        (if(msg2!=null) {
          exeGenerated_test1_2(msg2)
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
