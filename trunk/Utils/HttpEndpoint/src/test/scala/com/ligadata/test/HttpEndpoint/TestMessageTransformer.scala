package com.ligadata.test.HttpEndpoint

import org.scalatest._
import java.io.FileOutputStream
import com.ligadata.tools.HttpEndpoint._

class TestMessageTransformer extends FunSpec with BeforeAndAfter with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen {

  describe("Test Message Transformer") {

    val fileName = getClass.getResource("/good.conf").getPath
    Configuration.load(fileName)
      
    it("should transform a valid message") {
      val message = "stringField=Test String,numberField=1234,dateField=2016-05-19,datetimeField=2016-05-19T11:30:00"
      val xformer = new MessageProcessor(Configuration.values.services("TestEvent1"))
      val (keys, result) = xformer.transform("keyvalue", message)
      result.size shouldEqual 1
      //val f =  new FileOutputStream("avrotest.data")
      //f.write(result(0))
      //f.close()
      //assert(1 ==1)
      val str = new String(result(0), "UTF-8")
      println(str)
      str shouldEqual "Test String,1234,2016-05-19,2016-05-19T11:30:00"
    }
    
    it("should transform with configured message format") {
      val message = "field1:1234;field2:value2;field3:7890;field4:value4"
      val xformer = new MessageProcessor(Configuration.values.services("TestEvent2"))
      val (keys, result) = xformer.transform("keyvalue", message)
      result.size shouldEqual 1
      val str = new String(result(0), "UTF-8")
      println(str)
      str shouldEqual "1234;value2;7890;value4"
    }
    
    it("should transform a message with missing optional field") {
      val message = "stringField=Test String,numberField=1234,dateField=2016-05-19"
      val xformer = new MessageProcessor(Configuration.values.services("TestEvent1"))
      val (keys, result) = xformer.transform("keyvalue", message)
      result.size shouldEqual 1
      val str = new String(result(0), "UTF-8")
      println(str)
      str shouldEqual "Test String,1234,2016-05-19,"
    }
    
    it("should throw exception on a message with missing required field") {
      val message = "numberField=1234,dateField=2016-05-19,datetimeField=2016-05-19T11:30:00"
      val xformer = new MessageProcessor(Configuration.values.services("TestEvent1"))
      a [DataException] should be thrownBy {
        xformer.transform("keyvalue", message)
      }
    }
    
    it("should throw exception on a message with invalid number") {
      val message = "stringField=Test String,numberField=abc,dateField=2016-05-19,datetimeField=2016-05-19T11:30:00"
      val xformer = new MessageProcessor(Configuration.values.services("TestEvent1"))
      a [DataException] should be thrownBy {
        xformer.transform("keyvalue", message)
      }
    }

    it("should throw exception on a message with invalid date") {
      val message = "stringField=Test String,numberField=1234,dateField=2016,datetimeField=2016-05-19T11:30:00"
      val xformer = new MessageProcessor(Configuration.values.services("TestEvent1"))
      a [DataException] should be thrownBy {
        xformer.transform("keyvalue", message)
      }
    }
    
    it("should throw exception on a message with invalid datetime") {
      val message = "stringField=Test String,numberField=1234,dateField=2016-05-19,datetimeField=2016-05-19T11"
      val xformer = new MessageProcessor(Configuration.values.services("TestEvent1"))
      a [DataException] should be thrownBy {
        xformer.transform("keyvalue", message)
      }
    }

    it("should throw exception on a message with missing field seperator") {
      val message = "stringField=Test StringnumberField=1234,dateField=2016-05-19,datetimeField=2016-05-19T11:30:00"
      val xformer = new MessageProcessor(Configuration.values.services("TestEvent1"))
      a [FormatException] should be thrownBy {
        xformer.transform("keyvalue", message)
      }
    }
    
    it("should throw exception on a message with missing key value seperator") {
      val message = "stringField=Test String,numberField=1234,dateField2016-05-19,datetimeField=2016-05-19T11:30:00"
      val xformer = new MessageProcessor(Configuration.values.services("TestEvent1"))
      a [FormatException] should be thrownBy {
        xformer.transform("keyvalue", message)
      }
    }
  }

}
