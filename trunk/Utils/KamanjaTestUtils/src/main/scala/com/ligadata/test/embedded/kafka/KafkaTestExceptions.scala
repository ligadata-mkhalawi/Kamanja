package com.ligadata.test.embedded.kafka

case class KafkaTestClientException(message: String, cause: Throwable = null) extends Exception(message, cause)
