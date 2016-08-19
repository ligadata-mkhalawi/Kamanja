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
package com.ligadata.jtm

/**
  * Created by joerg on 1/20/16.
  */
object Parts {

  val header =
    """|/*
       | * Copyright 2016 ligaDATA
       | *
       | * Licensed under the Apache License, Version 2.0 (the "License");
       | * you may not use this file except in compliance with the License.
       | * You may obtain a copy of the License at
       | *
       | *     http://www.apache.org/licenses/LICENSE-2.0
       | *
       | * Unless required by applicable law or agreed to in writing, software
       | * distributed under the License is distributed on an "AS IS" BASIS,
       | * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
       | * See the License for the specific language governing permissions and
       | * limitations under the License.
       | */""".stripMargin

  val imports =
    """|import com.ligadata.KamanjaBase._
       |import com.ligadata.KvBase.TimeRange
       |import com.ligadata.kamanja.metadata.ModelDef
       |import com.ligadata.runtime._
       |""".stripMargin

  val factory =
    """|// Package code start
       |{external.packagecode}
       |// Package code end
       |
       |class {factoryclass.name}(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {
       |  // Factory code start
       |  {external.factorycode}
       |  // Factory code end
       |  override def createModelInstance(): ModelInstance = return new {modelclass.name}(this)
       |  override def getModelName: String = "{model.name}"
       |  override def getVersion: String = "{model.version}"
       |
       |  override def createResultObject(): ModelResultBase = new MappedModelResults()
       |
       |  override def isModelInstanceReusable(): Boolean = true;
       |}""".stripMargin

  val model =
    """|class {modelclass.name}(factory: ModelInstanceFactory) extends ModelInstance(factory) {
       |  val conversion = new com.ligadata.runtime.Conversion
       |  val log = new com.ligadata.runtime.Log(this.getClass.getName)
       |  val context = new com.ligadata.runtime.JtmContext
       |  import log._
       |  // Model code start
       |  {external.modelcode}
       |  // Model code end
       |
       |  private def DumpState(action: String, txnCtxt: TransactionContext): Unit = {
       |    if (isWarnEnabled) { Warning("==================> " + action + " =====> this:" + this + " ====================>Key: " + txnCtxt.getPartitionKey()) }
       |  }
       |
       |
       |  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
       |    context.Reset(); // Resetting the JtmContext before executing the model
       |    DumpState("Enter", txnCtxt)
       |    if (isTraceEnabled)
       |      Trace(s"Model::execute transid=%d triggeredset=%d outputdefault=%s".format(txnCtxt.transId, triggerdSetIndex, outputDefault.toString))
       |    if(isDebugEnabled)
       |    {
       |      execMsgsSet.foreach(m => Debug( s"Input: %s -> %s".format(m.getFullTypeName, m.toString())))
       |    }
       |    // Grok parts
       |    {model.grok}
       |    // Model methods
       |    {model.methods}
       |    // Evaluate messages
       |    {model.message}
       |    // Main dependency -> execution check
       |    // Create result object
       |    val results: Array[MessageInterface] =
       |    {model.code}
       |    if(isDebugEnabled)
       |    {
       |      results.foreach(m => Debug( s"Output: %s -> %s".format(m.getFullTypeName, m.toString())))
       |    }
       |    DumpState("Exit", txnCtxt)
       |    results.asInstanceOf[Array[ContainerOrConcept]]
       |  }
       |}""".stripMargin
}
