package com.ligadata.test.application.configuration

case class ApplicationConfigurationException(message: String, cause: Throwable = null) extends Exception(message, cause)
