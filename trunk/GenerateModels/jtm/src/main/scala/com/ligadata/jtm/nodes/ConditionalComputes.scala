package com.ligadata.jtm.nodes

/**
  * Created by Yasser on 1/16/2017.
  */
class ConditionalComputesGroup {

  /** Map with computations
    *
    */
  val computes: scala.collection.mutable.LinkedHashMap[String, Compute] = scala.collection.mutable.LinkedHashMap[String, Compute]()

  /** condition, calculate the computes only if condition is true
    *
    */
  val condition: String = ""
}
