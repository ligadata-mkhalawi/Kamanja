package com.ligadata.test.embedded.zookeeper

case class EmbeddedZookeeperException(message: String, cause: Throwable = null) extends Exception(message, cause)
