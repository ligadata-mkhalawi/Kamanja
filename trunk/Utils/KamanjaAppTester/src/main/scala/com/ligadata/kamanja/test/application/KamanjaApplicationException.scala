package com.ligadata.kamanja.test.application

case class KamanjaApplicationException(message: String, cause: Throwable = null) extends Exception(message, cause)
