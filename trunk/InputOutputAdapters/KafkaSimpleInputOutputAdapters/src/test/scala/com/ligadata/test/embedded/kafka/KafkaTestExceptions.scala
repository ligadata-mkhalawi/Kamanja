package com.ligadata.test.embedded.kafka

case class EmbeddedKafkaException(message: String, cause: Throwable = null) extends Exception(message, cause)
case class KafkaTestClientException(message: String, cause: Throwable = null) extends Exception(message, cause)
case class KafkaMsgHandlerException(message: String, cause: Throwable = null) extends Exception(message, cause)
