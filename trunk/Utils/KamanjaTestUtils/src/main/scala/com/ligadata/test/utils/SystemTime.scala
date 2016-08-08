package com.ligadata.test.utils

/**
 * Created by wtarver on 4/23/15.
 */
class SystemTime {

  def milliseconds:Long = {
    return System.currentTimeMillis()
  }

  def nanoseconds:Long = {
    return System.nanoTime()
  }

  def sleep(ms: Long) = {
    try{
      Thread.sleep(ms)
    }
    catch {
      case e:InterruptedException =>
    }
  }
}
