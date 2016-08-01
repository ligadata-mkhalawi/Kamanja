package com.ligadata.queryutility

import com.ligadata.scalabilityutility.LogTrait

/**
  * Created by Yousef on 8/1/2016.
  */
class TimeUtility  extends LogTrait{

  def RunDurationTime(timeInSec: Long): Long = {
    val currentTime: Long = System.currentTimeMillis
    val loopEndTime: Long = currentTime + (1000 * timeInSec)
    return loopEndTime
  }
}
