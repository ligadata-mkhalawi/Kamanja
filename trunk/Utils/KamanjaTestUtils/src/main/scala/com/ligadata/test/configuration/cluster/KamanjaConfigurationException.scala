package com.ligadata.test.configuration.cluster

/**
  * Created by william on 4/18/16.
  */
case class KamanjaConfigurationException(message: String, cause: Throwable = null) extends Exception(message, cause)
