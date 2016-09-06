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

package com.ligadata.InputOutputAdapterInfo

import com.ligadata.HeartBeat.MonitorComponentInfo
import com.ligadata.KamanjaBase.{ContainerInterface, EnvContext, NodeContext, TransactionContext}
import org.scalatest._
import org.scalamock._
import org.scalamock.scalatest.MockFactory

import scala.collection.mutable.ArrayBuffer

/**
  * Created by will on 2/9/16.
  */

private class MockOutputAdapterFactory extends OutputAdapterFactory {
  override def CreateOutputAdapter(inputConfig: AdapterConfiguration, nodeContext: NodeContext): MockOutputAdapter = new MockOutputAdapter
}

private class MockOutputAdapter extends OutputAdapter {
  var testMessages: Seq[String] = Seq()
  var testPartitionKeys: Seq[String] = Seq()

  override val inputConfig: AdapterConfiguration = null

  override def Shutdown: Unit = ???

  override def getComponentStatusAndMetrics: MonitorComponentInfo = null

  override def getComponentSimpleStats: String = ""

  override val nodeContext: NodeContext = null

  // This is protected override method. After applying serialization, pass original messages, Serialized data & Serializer names
  //def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface], serializedContainerData: Array[Array[Byte]], serializerNames: Array[String]): Unit = {
  //  this.send(serializedContainerData)
  //}

  override def send(tnxCtxt: TransactionContext, outputContainers: Array[ContainerInterface]): Unit = {
    val (containers, serBuf, serializers) = serialize(tnxCtxt, outputContainers)
    val partitionKeys = ArrayBuffer[Array[Byte]]()

    for (i <- 0 until containers.size) {
      partitionKeys += containers(i).getPartitionKey.mkString(",").getBytes()
    }
    send(serBuf, partitionKeys.toArray)
  }

  override def send(message: Array[Array[Byte]], partitionKey: Array[Array[Byte]]): Unit = {
    message.foreach(msg => {
      testMessages = testMessages :+ new String(msg)
    })
  }
}

class OutputAdapterTests extends FlatSpec with BeforeAndAfter with Matchers with MockFactory {
  private var outputAdapter: MockOutputAdapter = null

  before {
    outputAdapter = new MockOutputAdapterFactory().CreateOutputAdapter(null, null)
  }

  // TODO: Need to find a way to use Container Interface more intelligently for these tests. May be better used in an actual output adapter integration test.

  "OutputAdapter" should "send a single message and a single partition key" in {
    val messages = Array("This is a message".getBytes, "This is another message".getBytes)
    //outputAdapter.send(null, null, messages, null)
    outputAdapter.send(messages, null)
    assert(new String(outputAdapter.testMessages(0)) == "This is a message")
    assert(new String(outputAdapter.testMessages(1)) == "This is another message")
  }

  it should "be instantiated with Category set at Output" in {
    assert(outputAdapter.Category == "Output")
  }
}
