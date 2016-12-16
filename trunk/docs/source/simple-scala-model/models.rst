

.. simp_scala_models:

Create Scala models
===================

Write the models that actually execute good logic.
First, DAGAddition. Be surprised to learn at this point,
that DAGAddition performs (*gasp*) addition.
The interfaces used in this model are required
to compile this model for use in Kamanja.

Note: Models may also be written in Java, PMML
or Kamanja’s own special brand of PMML called kPMML.
Read more about model development in the models section.

The logic of these models is straightforward.
DAGAddition takes NumberMessage, adds its values together,
then produces the AddedMessage message with the sum stored as the third number.
DAGMultiplication takes AddedMessage, multiplies its values together,
then produces the MultipliedMessage message
with the product stored as the fourth number.
Finally, DAGDivision takes MultipliedMessage, divides its values,
then produces the DividedMessage message
with the dividend stored as the fifth number.
Once DividedMessage is produced, it is sent to the output topic.

DAGAddition.scala
-----------------

Here are the contents of DAGAddition.scala:

::

  package com.ligadata.test.dag
 
  import com.ligadata.KamanjaBase._
  import com.ligadata.kamanja.metadata.ModelDef

  class DAGAdditionFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {
  override def createModelInstance(): ModelInstance = return new DAGAddition(this)
  override def getModelName: String = "DAGAddition"
  override def getVersion: String = "0.0.1"
  override def createResultObject(): ModelResultBase = new MappedModelResults()
  }
 
  class DAGAddition(factory: ModelInstanceFactory) extends ModelInstance(factory) {
  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
 
  //Here we are expecting a single message of type NumberMessage
  val additionMsg: NumberMessage = execMsgsSet(0).asInstanceOf[NumberMessage]
 
  val result = additionMsg.integer1 + additionMsg.integer2
 
  // Here we are creating output, which is a type of AddedMessage, which we want to present
  //as the output of this model
  val output = AddedMessage.createInstance().asInstanceOf[AddedMessage]
 
  output.integer1 = additionMsg.integer1
  output.integer2 = additionMsg.integer2
  output.integer3 = result
 
  return Array(output)
  }
}

DAGMultiplication.scala
-----------------------

Here are the contents of DATMultiplication.scala:

::

  package com.ligadata.test.dag
 
  import com.ligadata.KamanjaBase._
  import com.ligadata.kamanja.metadata.ModelDef
 
  class DAGMultiplicationFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {
  override def createModelInstance(): ModelInstance = return new DAGMultiplication(this)
  override def getModelName: String = "DAGMultiplication"
  override def getVersion: String = "0.0.1"
  override def createResultObject(): ModelResultBase = new MappedModelResults()
  }
 
  class DAGMultiplication(factory: ModelInstanceFactory) extends ModelInstance(factory) {
  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
  val msg: AddedMessage = execMsgsSet(0).asInstanceOf[AddedMessage]
 
  val result = msg.integer1 * msg.integer2 * msg.integer3
 
  val output = MultipliedMessage.createInstance().asInstanceOf[MultipliedMessage]
 
  output.integer1 = msg.integer1
  output.integer2 = msg.integer2
  output.integer3 = msg.integer3
  output.integer4 = result
 
  return Array(output)
  }
  }

DAGDivision.scala
-----------------

Here are the contents of the DAGDivision.scala model:

::

  package com.ligadata.test.dag
 
  import com.ligadata.KamanjaBase._
  import com.ligadata.kamanja.metadata.ModelDef
 
  class DAGDivisionFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {
  override def createModelInstance(): ModelInstance = return new DAGDivision(this)
  override def getModelName: String = "DAGDivision"
  override def getVersion: String = "0.0.1"
  override def createResultObject(): ModelResultBase = new MappedModelResults()
  }
 
  class DAGDivision(factory: ModelInstanceFactory) extends ModelInstance(factory) {
  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
  val msg: MultipliedMessage = execMsgsSet(0).asInstanceOf[MultipliedMessage]
 
  val result = msg.integer1.toFloat / msg.integer2.toFloat / msg.integer3.toFloat / msg.integer4.toFloat
 
  val output = DividedMessage.createInstance().asInstanceOf[DividedMessage]
 
  output.integer1 = msg.integer1
  output.integer2 = msg.integer2
  output.integer3 = msg.integer3
  output.integer4 = msg.integer4
  output.float = result
 
  return Array(output)
  }
  }


