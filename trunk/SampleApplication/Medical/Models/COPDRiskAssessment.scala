/*
 * Copyright 2015 ligaDATA
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

package com.ligadata.kamanja.samples.models

import com.ligadata.KamanjaBase._
import RddUtils._
import RddDate._
import com.ligadata.KamanjaBase.MinVarType._
import java.text.SimpleDateFormat
import java.util.Calendar
import scala.io.Source
import scala.collection.JavaConversions._
import java.util._
import org.joda.time._
import com.ligadata.kamanja.metadata.ModelDef;

class COPDRiskAssessmentFactory(modelDef: ModelDef, nodeContext: NodeContext) extends ModelInstanceFactory(modelDef, nodeContext) {
  override def createModelInstance(): ModelInstance = return new COPDRiskAssessment(this)
  override def getModelName: String = "com.ligadata.kamanja.samples.models.COPDRisk"
  override def getVersion: String = "0.0.1"
}

class COPDRiskAssessment(factory: ModelInstanceFactory) extends ModelInstance(factory) {
  override def execute(txnCtxt: TransactionContext, execMsgsSet: Array[ContainerOrConcept], triggerdSetIndex: Int, outputDefault: Boolean): Array[ContainerOrConcept] = {
    var msgBeneficiary: Beneficiary = execMsgsSet(0).asInstanceOf[Beneficiary]
    val smokingCodeSet: Array[String] = SmokeCodes.getRDD.map { x => (x.icd9code) }.toArray
    val sputumCodeSet: Array[String] = SputumCodes.getRDD.map { x => (x.icd9code) }.toArray
    val envExposureCodeSet: Array[String] = EnvCodes.getRDD.map { x => (x.icd9code) }.toArray
    val coughCodeSet: Array[String] = CoughCodes.getRDD.map { x => (x.icd9code) }.toArray
    val dyspnoeaCodeSet: Array[String] = DyspnoeaCodes.getRDD.map { x => (x.icd9code) }.toArray
    var age: Int = 0
    val cal: Calendar = Calendar.getInstance
    cal.add(Calendar.YEAR, -3)
    var today: Date = Calendar.getInstance.getTime
    var threeYearsBeforeDate = cal.getTime
    var originalFormat: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
    val inPatientInfoThisLastyear: RDD[InpatientClaim] = InpatientClaim.getRDD(msgBeneficiary.getPartitionKey()).filter { x =>
      originalFormat.parse(x.clm_thru_dt.toString()).before(today) && originalFormat.parse(x.clm_thru_dt.toString()).after(threeYearsBeforeDate)
    }

    val outPatientInfoThisLastYear: RDD[OutpatientClaim] = OutpatientClaim.getRDD(msgBeneficiary.getPartitionKey()).filter { x =>
      originalFormat.parse(x.clm_thru_dt.toString()).before(today) && originalFormat.parse(x.clm_thru_dt.toString()).after(threeYearsBeforeDate)
    }

    def getOverSmokingCodesInLastYear(): Boolean = {
      for (x <- inPatientInfoThisLastyear) {
        if (smokingCodeSet.contains(x.admtng_icd9_dgns_cd)) {
          return true
        }

        for (s <- x.icd9_dgns_cds) {
          if (smokingCodeSet.contains(s)) {
            return true
          }
        }
      }
      for (x <- outPatientInfoThisLastYear) {
        if (smokingCodeSet.contains(x.admtng_icd9_dgns_cd)) {
          return true
        }

        for (s <- x.icd9_dgns_cds) {
          if (smokingCodeSet.contains(s)) {
            return true
          }
        }
      }

      return false
    }

    def getEnvironmentalExposuresInLastYear(): Boolean = {
      for (x <- inPatientInfoThisLastyear) {
        if (envExposureCodeSet.contains(x.admtng_icd9_dgns_cd)) {
          return true
        }

        for (s <- x.icd9_dgns_cds) {
          if (envExposureCodeSet.contains(s)) {
            return true
          }
        }
      }
      for (x <- outPatientInfoThisLastYear) {
        if (envExposureCodeSet.contains(x.admtng_icd9_dgns_cd)) {
          return true
        }

        for (s <- x.icd9_dgns_cds) {
          if (envExposureCodeSet.contains(s)) {
            return true
          }
        }
      }

      return false
    }

    def getDyspnoeaInLastYear(): Boolean = {
      for (x <- inPatientInfoThisLastyear) {
        if (dyspnoeaCodeSet.contains(x.admtng_icd9_dgns_cd)) {
          return true
        }

        for (s <- x.icd9_dgns_cds) {
          if (dyspnoeaCodeSet.contains(s)) {
            return true
          }
        }
      }
      for (x <- outPatientInfoThisLastYear) {

        if (dyspnoeaCodeSet.contains(x.admtng_icd9_dgns_cd)) {
          return true
        }

        for (s <- x.icd9_dgns_cds) {
          if (dyspnoeaCodeSet.contains(s)) {
            return true
          }
        }

      }

      return false
    }

    def getChronicCoughInLastYear(): Boolean = {
      for (x <- inPatientInfoThisLastyear) {
        if (coughCodeSet.contains(x.admtng_icd9_dgns_cd)) {
          return true
        }

        for (s <- x.icd9_dgns_cds) {
          if (coughCodeSet.contains(s)) {
            return true
          }
        }

      }

      for (x <- outPatientInfoThisLastYear) {

        if (coughCodeSet.contains(x.admtng_icd9_dgns_cd)) {
          return true
        }

        for (s <- x.icd9_dgns_cds) {
          if (coughCodeSet.contains(s)) {
            return true
          }
        }
      }

      return false
    }

    def getChronicSputumInLastYear(): Boolean = {
      for (x <- inPatientInfoThisLastyear) {

        if (sputumCodeSet.contains(x.admtng_icd9_dgns_cd)) {
          return true
        }

        for (s <- x.icd9_dgns_cds) {
          if (sputumCodeSet.contains(s)) {
            return true
          }
        }
      }
      for (x <- outPatientInfoThisLastYear) {

        if (sputumCodeSet.contains(x.admtng_icd9_dgns_cd)) {
          return true
        }

        for (s <- x.icd9_dgns_cds) {
          if (sputumCodeSet.contains(s)) {
            return true
          }
        }
      }

      return false
    }

    def getHL7InfoThisLastYear(): Boolean = {

      val hl7info = HL7.getRDD(msgBeneficiary.getPartitionKey()).filter { x =>
        originalFormat.parse(x.clm_thru_dt.toString()).before(today) && originalFormat.parse(x.clm_thru_dt.toString()).after(threeYearsBeforeDate)
      }
      for (x <- hl7info) {

        if (x.chroniccough > 0 || x.sp_copd > 0 || x.shortnessofbreath > 0 || x.chronicsputum > 0) {
          return true
        }

      }
      return false
    }

    def getAATDeficiencyInLastYear(): Boolean = {
      val hl7info = HL7.getRDD(msgBeneficiary.getPartitionKey()).filter { x =>
        originalFormat.parse(x.clm_thru_dt.toString()).before(today) && originalFormat.parse(x.clm_thru_dt.toString()).after(threeYearsBeforeDate)
      }
      for (x <- hl7info) {

        if (x.aatdeficiency == 1)
          return true

      }
      return false
    }

    def getCopdSymptoms(): Boolean = {
      if (getChronicSputumInLastYear || getChronicCoughInLastYear || getDyspnoeaInLastYear)
        return true

      return false
    }

    def getFamilyHistory(): Boolean = {

      if (msgBeneficiary.sp_copd == 1 || getHL7InfoThisLastYear)
        return true

      return false
    }

    def getInPatientClaimCostsByDate: Map[Int, Double] = {
      var inPatientClaimCostTuples = new ArrayList[Tuple2[Int, Double]]()
      for (x <- inPatientInfoThisLastyear) {
        inPatientClaimCostTuples.add((x.clm_thru_dt, x.clm_pmt_amt + x.nch_prmry_pyr_clm_pd_amt + x.clm_pass_thru_per_diem_amt + x.nch_bene_ip_ddctbl_amt + x.nch_bene_pta_coinsrnc_lblty_am + x.nch_bene_blood_ddctbl_lblty_am))
      }
      var inPatientClaimTotalCostEachDate = inPatientClaimCostTuples.groupBy(_._1).map { case (k, v) => (k, v.map(_._2)) }
      inPatientClaimTotalCostEachDate.map { case (k, v) => (k, v.sum) }
    }

    def getOutPatientClaimCostsByDate: Map[Int, Double] = {
      var outPatientClaimCostTuples = new ArrayList[Tuple2[Int, Double]]()
      for (x <- outPatientInfoThisLastYear) {
        outPatientClaimCostTuples.add((x.clm_thru_dt, x.clm_pmt_amt + x.nch_prmry_pyr_clm_pd_amt + x.nch_bene_blood_ddctbl_lblty_am + x.nch_bene_ptb_ddctbl_amt + x.nch_bene_ptb_coinsrnc_amt))
      }
      var outPatientClaimTotalCostEachDate = outPatientClaimCostTuples.groupBy(_._1).map { case (k, v) => (k, v.map(_._2)) }
      outPatientClaimTotalCostEachDate.map { case (k, v) => (k, v.sum) }
    }

    def getMaterializeOutputs: Boolean = {
      if (getInPatientClaimCostsByDate.size > 0 || getOutPatientClaimCostsByDate.size > 0)
        return true

      return false
    }

    def getCATII_Rule2: Boolean = {
      val birthDate = originalFormat.parse(msgBeneficiary.bene_birth_dt.toString())
      val ageInYears = Years.yearsBetween(new LocalDate(birthDate), new LocalDate(today)).getYears
      age = ageInYears
      if ((ageInYears > 40 && (getCopdSymptoms || getAATDeficiencyInLastYear || getFamilyHistory))) {
        return true
      }
      return false
    }

    def getCATI_Rule1b: Boolean = {
      val birthDate = originalFormat.parse(msgBeneficiary.bene_birth_dt.toString())
      val ageInYears = Years.yearsBetween(new LocalDate(birthDate), new LocalDate(today)).getYears
      age = ageInYears
      if (ageInYears > 40 && getOverSmokingCodesInLastYear && getAATDeficiencyInLastYear && getEnvironmentalExposuresInLastYear && getCopdSymptoms) {
        return true
      }
      return false
    }

    def getCATI_Rule1a: Boolean = {
      val birthDate = originalFormat.parse(msgBeneficiary.bene_birth_dt.toString())
      val ageInYears = Years.yearsBetween(new LocalDate(birthDate), new LocalDate(today)).getYears
      age = ageInYears
      if (ageInYears > 40 && getOverSmokingCodesInLastYear && (getAATDeficiencyInLastYear || getEnvironmentalExposuresInLastYear || getCopdSymptoms)) {
        return true
      }
      return false
    }

    println("Executing COPD Risk Assessment against message:");
    println("Message Type: " + msgBeneficiary.getFullTypeName())
    println("Message Name: " + msgBeneficiary.getTypeName());
    println("Message Desynpuf ID: " + msgBeneficiary.desynpuf_id);

	val output = COPDOutputMessage.createInstance().asInstanceOf[COPDOutputMessage];
	output.desynpuf_id = msgBeneficiary.desynpuf_id;
	output.ageofthebenificiary = age;
	output.ageover40 = age > 40;
	output.hascopdsymptoms = getCopdSymptoms;
	output.hasaatdeficiency = getAATDeficiencyInLastYear;
	output.hasfamilyhistory = getFamilyHistory;
	output.hassmokinghistory = getOverSmokingCodesInLastYear;
	output.hasdyspnea = false
	output.haschroniccough = false
	output.haschronicsputum = false
	output.hasdyspnea = false
	output.hasenvironmentalexposure = getEnvironmentalExposuresInLastYear;
	output.inpatientclaimcosts = 0;
	output.outpatientclaimcosts = 0;

    if (getCATI_Rule1b) {
		output.risklevel = "1b";
        return Array(output);
    } else if (getCATI_Rule1a) {
		output.risklevel = "1a";
        return Array(output);
    } else if (getCATII_Rule2) {
		output.risklevel = "2";
        return Array(output);
    } else {
      return null
    }

  }
}
