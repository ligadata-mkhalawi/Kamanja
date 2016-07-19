package com.ligadata.testmsgcompiler

import org.scalatest.FunSuite
import com.ligadata.msgcompiler.MessageCompiler
import com.ligadata.msgcompiler.Message
import scala.io.Source
import java.io.File
import com.ligadata.kamanja.metadata.MdMgr
import com.ligadata.kamanja.metadata.ContainerDef
import com.ligadata.kamanja.metadata.MessageDef
import com.ligadata.kamanja.metadataload.MetadataLoad
import com.ligadata.messages.V1000000._;
import com.ligadata.messages.V1000000000000._;
import com.ligadata.messages.V10000001000000._;
import scala.collection.mutable._
import java.io.File
import java.io.PrintWriter
import org.apache.logging.log4j.{ Logger, LogManager }
import com.ligadata.KamanjaBase._;

class MessageCompilerTestSuite extends FunSuite {
  val log = LogManager.getLogger(getClass)

  test("Process Message Compiler") {
    val mdLoader: MetadataLoad = new MetadataLoad(MdMgr.GetMdMgr, "", "", "", "")
    mdLoader.initialize
    var msgDfType: String = "JSON"

    var messageCompiler = new MessageCompiler;
    val tid: Option[String] = null
    val jsonstr: String = Source.fromFile("./MessageCompiler/src/test/resources/fixedmsgs/product.json").getLines.mkString
    val ((verScalaMsg, verJavaMsg), containerDef, (nonVerScalaMsg, nonVerJavaMsg), rawMsg) = messageCompiler.processMsgDef(jsonstr, msgDfType, MdMgr.GetMdMgr, 0, null)
    //createScalaFile(verScalaMsg, containerDef.Version.toString, containerDef.FullName, ".scala")

    assert(verScalaMsg === verScalaMsg)
    assert(containerDef.Name === "product")
  }

  test("Test Generated Message - Time Partition Data ") {
    var hl7Fixed: HL7Fixed = new HL7Fixed(HL7Fixed);
    hl7Fixed.set(0, "120024000")
    hl7Fixed.setTimePartitionData();
    assert(hl7Fixed.getOrElse("abc", "123") === "123")
    assert(hl7Fixed.getOrElse(200, "4556") === "4556")
    assert(hl7Fixed.getTimePartitionData === 126230400000L)
  }

  test("Test Fixed Message - InpatientClaimFixedTest") {
    var inpatientClaimFixedTest: InpatientClaimFixedTest = new InpatientClaimFixedTest(InpatientClaimFixedTest);
    var idCodeDimFixedTest = new IdCodeDimFixedTest(IdCodeDimFixedTest);

    var idCodeDimFixedTestArray = new Array[IdCodeDimFixedTest](1)
    idCodeDimFixedTest.set(0, 1)
    idCodeDimFixedTest.set(1, 2)
    idCodeDimFixedTest.set(2, "Test")
    idCodeDimFixedTest.set(3, scala.collection.immutable.Map("test" -> 1))
    idCodeDimFixedTestArray(0) = idCodeDimFixedTest

    inpatientClaimFixedTest.set(0, "100")
    inpatientClaimFixedTest.set(1, 1000L)
    inpatientClaimFixedTest.set(2, Array("icddgs"))
    inpatientClaimFixedTest.set(3, Array(1))
    inpatientClaimFixedTest.set(4, true)
    inpatientClaimFixedTest.set(5, scala.collection.immutable.Map("testInp" -> 10))
    inpatientClaimFixedTest.set(6, scala.collection.immutable.Map("icd" -> idCodeDimFixedTest))
    inpatientClaimFixedTest.set(7, idCodeDimFixedTest)
    inpatientClaimFixedTest.set(8, idCodeDimFixedTestArray)

    log.info("inpatientClaimFixedTest  " + inpatientClaimFixedTest.get(5))

    assert(inpatientClaimFixedTest.get(0) === "100")
    assert(inpatientClaimFixedTest.get(1) === 1000)
    assert(inpatientClaimFixedTest.get(2) === Array("icddgs"))
    assert(inpatientClaimFixedTest.get(4) === true)
    assert(inpatientClaimFixedTest.getOrElse(10, "defaultVal") === "defaultVal")
    assert(inpatientClaimFixedTest.getOrElse("abc", "text") === "text")
    log.info("inpatientClaimFixedTest getOrElse index " + inpatientClaimFixedTest.getOrElse(10, "defaultVal"))
    log.info("inpatientClaimFixedTest getOrElse  " + inpatientClaimFixedTest.getOrElse("abc", "text"))

    val idCodeDim = inpatientClaimFixedTest.get(7)

    if (idCodeDim.isInstanceOf[IdCodeDimFixedTest]) {
      assert(idCodeDim.asInstanceOf[ContainerInterface].get(0) === 1)
      assert(idCodeDim.asInstanceOf[IdCodeDimFixedTest].code === 2)
      log.info("inpatientClaimFixedTest  " + idCodeDim.asInstanceOf[ContainerInterface].get(0))
    }
  }

  test("Test Mapped Message - InpatientClaimMappedTest") {
    var idCodeDimMappedTest = new IdCodeDimMappedTest(IdCodeDimMappedTest);
    idCodeDimMappedTest.set("id", 1)
    idCodeDimMappedTest.set("code", 2)
    idCodeDimMappedTest.set("description", "Test")
    idCodeDimMappedTest.set("codes", scala.collection.immutable.Map("test" -> 1))
    assert(idCodeDimMappedTest.getOrElse("id1", "hello") === "hello")
    assert(idCodeDimMappedTest.get("id") === 1)
    log.info("inpatientClaim Mapped " + idCodeDimMappedTest.getOrElse("id1", "hello"))
  }

  private def createScalaFile(scalaClass: String, version: String, className: String, clstype: String): Unit = {
    try {
      val writer = new PrintWriter(new File("./MessageCompiler/src/test/resources/GeneratedMsgs/" + className + "_" + version + clstype))
      // val writer = new PrintWriter(new File("src/test/resources/GeneratedMsgs/" + className + "_" + version + clstype))
      writer.write(scalaClass.toString)
      writer.close()
      println("Done")
    } catch {
      case e: Exception => {
        e.printStackTrace()
        throw e
      }
    }
  }
}
