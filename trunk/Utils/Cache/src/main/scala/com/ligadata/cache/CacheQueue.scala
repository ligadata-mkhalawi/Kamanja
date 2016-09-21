package com.ligadata.cache

import org.apache.hadoop.hbase.protobuf.generated.MasterProtos.ExecProcedureRequest
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

// BUGBUG:: We can optimize this with one look ahead extra item. That way we don't need to replace the key in enQueue.  Which saves one put operation & manupulating data

*/


// NOTENOTE: Must to serialize link value at the begining either using base trait serialize or CacheQueue.linkValueToSerializeCacheQueueElementInfo(link)
trait CacheQueueElement {
  def serialize(): Array[Byte] = CacheQueue.linkValueToSerializeCacheQueueElementInfo(link)
  var link: String
}

object CacheQueue {
  final def extractLinkValueLengthFromSerializedCacheQueueElementInfo(buf: Array[Byte]): Int = {
    if (buf.length < 2)
      throw new Exception("Not enough lenght to get key from Serialized buffer")
    val byte0 = buf(0).toInt
    val byte1 = buf(1).toInt
    val len = (((byte0 & 0xFF) << 8) + ((byte1 & 0xFF) << 0))

    len
  }

  final def extractLinkValueFromSerializedCacheQueueElementInfo(buf: Array[Byte]): String = {
    val len = extractLinkValueLengthFromSerializedCacheQueueElementInfo(buf)
    new String(buf, 2, len)
  }

  final def linkValueToSerializeCacheQueueElementInfo(linkVal: String): Array[Byte] = {
    val bytes = linkVal.getBytes("UTF-8")
    val len = bytes.length.toShort

    val lenArrBuf = Array[Byte](0, 0)

    lenArrBuf(0) = ((len >>> 8) & 0xFF).toByte
    lenArrBuf(1) = ((len >>> 0) & 0xFF).toByte

    (lenArrBuf ++ bytes)
  }

  final def replaceLinkValueInSerializeCacheQueueElementInfo(newLinkVal: String, serInfo: Array[Byte]): Array[Byte] = {
    val prevLinkValLen = extractLinkValueLengthFromSerializedCacheQueueElementInfo(serInfo)

    val newBytes = newLinkVal.getBytes("UTF-8")
    val curLinkValLen = newBytes.length

    if (prevLinkValLen == curLinkValLen) {
      var i = 2
      newBytes.foreach(b => {
        serInfo(i) = b
      })
      serInfo
    } else {
      linkValueToSerializeCacheQueueElementInfo(newLinkVal) ++ serInfo.takeRight(serInfo.size - prevLinkValLen - 2)
    }
  }
}

class CacheQueue(val cache: DataCache, val kQueueHead: String, val kQueueTail: String, val deserializeCacheQueueElement: (Array[Byte]) => CacheQueueElement) {
  if (deserializeCacheQueueElement == null)
    throw new Exception("Required deserializeCacheQueueElement")

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
        val v = element.serialize()
        cache.put(key, v)
        cache.put(kQueueTail, key)
      } else {
        val pred = cache.get(tailPred).asInstanceOf[Array[Byte]]
        // Replacing link with key
        cache.put(tailPred, CacheQueue.replaceLinkValueInSerializeCacheQueueElementInfo(key, pred))
        val v = element.serialize()
        cache.put(key, v)
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
      returnElmt = deserializeCacheQueueElement(cache.get(key).asInstanceOf[Array[Byte]])
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
      returnElmt = deserializeCacheQueueElement(cache.get(key).asInstanceOf[Array[Byte]])
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
