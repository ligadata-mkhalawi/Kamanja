package com.ligadata.smartfileadapter

import org.scalatest._
import com.ligadata.AdaptersConfiguration._
import com.ligadata.InputAdapters._
import org.apache.commons.lang.StringUtils

/**
  * Created by Yasser on 7/21/2016.
  */
class TestOrdering extends FunSpec with BeforeAndAfter with ShouldMatchers with BeforeAndAfterAll with GivenWhenThen {
/*
  val file1 = "/data/input/emm/20160715.INTEC_DIGICELY_EMM_JA.38"
  //val file2 = "/data/input/emm/20160715.INTEC_DIGICELY_EMM_JA.38"

  val pattern = "([0-9]+)\\.([A-Za-z]+)_([A-Z]+)_([A-Z]+)_([A-Z]+)\\.([0-9]+)"
  val orderingInfo = new  FileOrderingInfo
  orderingInfo.orderBy = "components"
  orderingInfo.fileComponents = new FileComponents
  orderingInfo.fileComponents.regex = pattern
  orderingInfo.fileComponents.components = Array("date", "source_type", "phy_switch", "proc_source", "region", "serial")
  orderingInfo.fileComponents.orderFieldValueFromComponents = "date-region-serial"

  describe("Test order by file name components"){
    it(""){
      //val file1Alt = MonitorUtils.getFileAltNameFromComponents(file1, orderingInfo)
      //println("file1Alt="+file1Alt)
      println (StringUtils.leftPad("123", 2, "0"))
      println (StringUtils.leftPad("123", 10, "0"))
      println (StringUtils.rightPad("123", 2, "0"))
      println (StringUtils.rightPad("123", 10, "0"))

      println("123".padTo(2, "0").mkString(""))
      println("123".padTo(10, "0").mkString(""))
    }
  }*/
}
