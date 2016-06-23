package com.ligadata.test.HttpEndpoint

import org.scalatest._
import com.ligadata.tools.HttpEndpoint.Configuration

class TestEndpointConfiguration extends FunSpec with BeforeAndAfter with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen {

  describe("Test Http Endpoint configuration") {

    val fileName = getClass.getResource("/good.conf").getPath
    println(fileName)
    Configuration.load(fileName)

    it("should read configuration correctly from a valid JSON") {
      Configuration.values.host shouldEqual "localhost"
      Configuration.values.port shouldEqual 8085
    }
      
    it("should read multiple services from a valid JSON") {
      Configuration.values.services.size shouldEqual 2
      assert(Configuration.values.services("TestEvent1") != null)
      assert(Configuration.values.services("TestEvent2") != null)
    }
    
    it("should read service schema from a valid JSON") {
      val svc = Configuration.values.services("TestEvent1")
      
      svc.schema.size shouldEqual 4
      svc.schema(0).id shouldEqual 1
      svc.schema(1).id shouldEqual 2
      svc.schema(2).id shouldEqual 3
      svc.schema(3).id shouldEqual 4
    }

    it("should read service schema fields specified out of order") {
      val svc = Configuration.values.services("TestEvent2")
      
      svc.schema.size shouldEqual 4
      svc.schema(0).id shouldEqual 1
      svc.schema(1).id shouldEqual 2
      svc.schema(2).id shouldEqual 3
      svc.schema(3).id shouldEqual 4
    }
    
    it("should read string field from a valid JSON") {
      var svc = Configuration.values.services("TestEvent1")
      svc.schema(0).name shouldEqual "stringField"
      svc.schema(0).datatype shouldEqual "string"
      svc.schema(0).typeId shouldEqual 2
      svc.schema(0).required shouldEqual true
      svc.schema(0).maxlength shouldEqual 20

      svc = Configuration.values.services("TestEvent2")
      svc.schema(1).name shouldEqual "field2"
      svc.schema(1).datatype shouldEqual "string"
      svc.schema(1).typeId shouldEqual 2
      svc.schema(1).required shouldEqual true
      svc.schema(1).maxlength shouldEqual 20

      svc.schema(3).name shouldEqual "field4"
      svc.schema(3).datatype shouldEqual "string"
      svc.schema(3).typeId shouldEqual 2
      svc.schema(3).required shouldEqual false
      svc.schema(3).maxlength shouldEqual 256
    }

    it("should read number field from a valid JSON") {
      var svc = Configuration.values.services("TestEvent1")

      svc.schema(1).id shouldEqual 2
      svc.schema(1).name shouldEqual "numberField"
      svc.schema(1).datatype shouldEqual "number"
      svc.schema(1).typeId shouldEqual 1
      svc.schema(1).required shouldEqual true

      svc = Configuration.values.services("TestEvent2")
      svc.schema(0).id shouldEqual 1
      svc.schema(0).name shouldEqual "field1"
      svc.schema(0).datatype shouldEqual "number"
      svc.schema(0).typeId shouldEqual 1
      svc.schema(0).required shouldEqual true

      svc.schema(2).id shouldEqual 3
      svc.schema(2).name shouldEqual "field3"
      svc.schema(2).datatype shouldEqual "number"
      svc.schema(2).typeId shouldEqual 1
      svc.schema(2).required shouldEqual false
    }

    it("should read date field from a valid JSON") {
      var svc = Configuration.values.services("TestEvent1")
      svc.schema(2).id shouldEqual 3
      svc.schema(2).name shouldEqual "dateField"
      svc.schema(2).datatype shouldEqual "date"
      svc.schema(2).typeId shouldEqual 3
      svc.schema(2).required shouldEqual true
      svc.schema(2).format shouldEqual "yyyy-MM-dd"
      
    }

    it("should read dateTime field from a valid JSON") {
      var svc = Configuration.values.services("TestEvent1")
      svc.schema(3).id shouldEqual 4
      svc.schema(3).name shouldEqual "datetimeField"
      svc.schema(3).datatype shouldEqual "datetime"
      svc.schema(3).typeId shouldEqual 3
      svc.schema(3).required shouldEqual false
      svc.schema(3).format shouldEqual "yyyy-MM-dd'T'HH:mm:SS"
    }
      
    it("should read service message definition from a valid JSON") {
      var svc = Configuration.values.services("TestEvent1")
      svc.message.keyAndValueSeparator shouldEqual "="
      svc.message.fieldSeparator shouldEqual ","
      svc.message.recordSeparator shouldEqual "\n"
      svc.message.allowFieldsNotInSchema shouldEqual false

      svc = Configuration.values.services("TestEvent2")
      svc.message.keyAndValueSeparator shouldEqual ":"
      svc.message.fieldSeparator shouldEqual ";"
      svc.message.recordSeparator shouldEqual "\n"
      svc.message.allowFieldsNotInSchema shouldEqual true
    }
      
    it("should read service output configuration from a valid JSON") {
      var svc = Configuration.values.services("TestEvent1")
      svc.output.getOrElse("kafkaHost", "") shouldEqual "localhost:9092"
      svc.output.getOrElse("kafkaTopic", "") shouldEqual "topic_1"
      svc.output.getOrElse("partitionKey", "") shouldEqual "stringField"
      svc.output.getOrElse("outputFormat", "") shouldEqual "delimited"
      svc.serializationType shouldEqual 1

      svc = Configuration.values.services("TestEvent2")
      svc.output.getOrElse("kafkaHost", "") shouldEqual "localhost:9092"
      svc.output.getOrElse("kafkaTopic", "") shouldEqual "topic_2"
      svc.output.getOrElse("partitionKey", "") shouldEqual "field1"
      svc.output.getOrElse("outputFormat", "") shouldEqual "delimited"
      svc.serializationType shouldEqual 1
    }
    
  }

}
