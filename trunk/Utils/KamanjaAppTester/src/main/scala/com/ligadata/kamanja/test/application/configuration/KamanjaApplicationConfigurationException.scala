package com.ligadata.kamanja.test.application.configuration

case class KamanjaApplicationConfigurationException(message: String, cause: Throwable = null) extends Exception(message, cause)
