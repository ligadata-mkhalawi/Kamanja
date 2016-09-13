package com.ligadata.cache

import org.apache.logging.log4j.LogManager

/*
Algorithem:

=====================

enQ =>

newElem (x, tailValue)

       get          get           put
tail -------> prev -------> next ===== newElem


      get           put
tail -------> prev ===== newElem

------------------------

-1 => -2

-1 <= -2

------------------------

-1 => 1

1 => -2

1 <= -2

------------------------

-1 => 1

1 => 2

2 => -2

2 <= -2

=====================


deQ =>

          get
if (head-------> next == tailValue)
	return null;

                get
retVal = head -------> next


       get          put         get          get
head -------> next ===== head -------> next -------> next

==========================

lookAheadQ =>


          get
if (head-------> next == tailValue)
	return null;

                get
retVal = head -------> next

==========================



*/


trait CacheQueueElement {
  def serialize(): Array[Byte]

  def deserialize(data: Array[Byte]): Unit

  var link: String
}


class CacheQueue(val cache: DataCache, val kQueueHead: String, val kQueueTail: String, val createCacheQueueElement: CacheQueueElement) {
  private val LOG = LogManager.getLogger(getClass);

  // For tail we are setting predecessor key
  cache.put(kQueueTail, kQueueHead)
  // For head we are setting successor key
  cache.put(kQueueHead, kQueueTail)

  def enQ(key: String, element: CacheQueueElement): Unit = {
    element.link = kQueueTail
    var exception: Throwable = null

    // Update this new element under transaction
    val tm = cache.beginTransaction
    try {
      val tailPred = cache.get(kQueueTail).asInstanceOf[String]

      if (tailPred.equals(kQueueHead)) {
        cache.put(kQueueHead, key)
        cache.put(key, element)
        cache.put(kQueueTail, key)
      } else {
        val pred = cache.get(tailPred).asInstanceOf[CacheQueueElement]
        pred.link = key
        cache.put(tailPred, pred)
        cache.put(key, element)
        cache.put(kQueueTail, key)
      }
      tm.commit()
    } catch {
      case e: Throwable => {
        exception = e
        if (tm != null) {
          try {
            tm.rollback()
          } catch {
            case e: Throwable => {
              LOG.error("Failed to rollback transaction", e)
            }
          }
        }
      }
    }

    // Rethrow exception in case if we get any
    if (exception != null)
      throw exception
  }

  def deQ(): CacheQueueElement = {
    var returnElmt: CacheQueueElement = null
    var exception: Throwable = null

    // Get this key under transaction
    val tm = cache.beginTransaction
    try {
      val key = cache.get(kQueueHead).asInstanceOf[String]
      if (key.equals(kQueueTail))
        return null
      returnElmt = cache.get(key).asInstanceOf[CacheQueueElement]
      cache.put(kQueueHead, returnElmt.link)
      cache.remove(key)
      tm.commit()
    } catch {
      case e: Throwable => {
        exception = e
        if (tm != null) {
          try {
            tm.rollback()
          } catch {
            case e: Throwable => {
              LOG.error("Failed to rollback transaction", e)
            }
          }
        }
      }
    }
    returnElmt
  }

  def lookAheadQ(): CacheQueueElement = {
    var returnElmt: CacheQueueElement = null
    var exception: Throwable = null

    // Get this key under transaction
    val tm = cache.beginTransaction
    try {
      val key = cache.get(kQueueHead).asInstanceOf[String]
      if (key.equals(kQueueTail))
        return null
      returnElmt = cache.get(key).asInstanceOf[CacheQueueElement]
      tm.commit()
    } catch {
      case e: Throwable => {
        exception = e
        if (tm != null) {
          try {
            tm.rollback()
          } catch {
            case e: Throwable => {
              LOG.error("Failed to rollback transaction", e)
            }
          }
        }
      }
    }
    returnElmt
  }
}
