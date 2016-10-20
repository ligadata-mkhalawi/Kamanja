package com.ligadata.test.models

import com.ligadata.KamanjaBase._
import com.ligadata.kamanja.metadata.ModelDef

class TestApp1ModelFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {
  override def createModelInstance(): ModelInstance = return new TestApp1Model(this)
  override def getModelName: String = "com.ligadata.kamanja.test.models.TestApp1Model"
  override def getVersion: String = "0.0.1"
}

class TestApp1Model(factory: ModelInstanceFactory) extends ModelInstance(factory) {
  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggeredSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
    val customer: TestApp1Message = execMsgsSet(0).asInstanceOf[TestApp1Message]
    val customerName: String = customer.name
    val customerId: String = customer.id
    val customerShoppingList: Array[String] = customer.shoppinglist
    var cost: Double = 0d

    println("Customer Name: " + customerName)
    println("Customer ID: " + customerId)
    println("Customer Shopping List: ")
    customerShoppingList.foreach(item => {
      println("\t" + item)
      item.toLowerCase match {
        case "food" => cost = cost + 39.95
        case "games" => cost = cost + 59.99
        case "music" => cost = cost + 14.80
      }
    })

    println("Customer Total Cost: " + String.format("%.2f", cost: java.lang.Double))

    val output = TestApp1OutputMessage.createInstance.asInstanceOf[TestApp1OutputMessage]
    output.id = customerId
    output.name = customerName
    output.shoppinglist = customerShoppingList
    output.totalcost = String.format("%.2f", cost: java.lang.Double)
    return Array(output)
  }
}