package com.ligadata.InputAdapters

import com.ligadata.AdaptersConfiguration.LocationInfo
import org.apache.logging.log4j.LogManager

class MonitoredDirsQueue {

  val dirsQueue = scala.collection.mutable.Queue[(LocationInfo, Long, Boolean)]() //(location, last time scanned MS, is first scan)
  private val qLock = new Object
  private var dirWaitingTime : Int = 0

  lazy val logger = LogManager.getLogger(this.getClass.getName)

  def init(dirs : Array[LocationInfo], waitingTime : Int) : Unit = {
    dirWaitingTime = waitingTime

    qLock.synchronized{
      dirs.foreach(loc => dirsQueue.enqueue((loc, 0, true)))
    }
  }

  def getNextDir(re_enqueue : Boolean = true) : (LocationInfo, Long, Boolean) = {

    logger.warn("dirsQueue before calling getNextDir = {}", dirsQueue)
    qLock.synchronized{
      if(dirsQueue.isEmpty)
        null
      else{
        val currentTimeMs = System.nanoTime() / 1000
        val dir = dirsQueue.head
        if(currentTimeMs - dir._2 < dirWaitingTime)
          null
        else {
          dirsQueue.dequeue()
          dir
        }
      }
    }
  }

  def reEnqueue(dirInfo : (LocationInfo, Long, Boolean)) : Unit = {
    qLock.synchronized {
      try {
        val newDirInfo = (dirInfo._1, System.nanoTime() / 1000, false)
        dirsQueue.enqueue(newDirInfo)

        logger.warn("dirsQueue reEnqueue calling getNextDir = {}", dirsQueue)
      }
      catch{
        case ex : Throwable => logger.error("Error while enqueuing folder for monitoring " + dirInfo._1.srcDir,ex)
      }
    }
  }
}
