package com.ligadata.throttler

import org.scalatest._

import com.ligadata.throttler.ThrottleController;
import com.ligadata.cache.DataCache
import com.ligadata.cache.{ CacheCallbackData, CacheCallback, DataCache }

class ThrottleControllerTest extends FlatSpec with BeforeAndAfter with Matchers {
  var tc = new ThrottleControllerCache("10.0.0.83", "7800", true)

  tc.Init();

  "add" should "put data in memory" in {
    tc.AddKey("1", 1)
    tc.AddKey("2", 2)
    tc.AddKey("3", 3)
    tc.AddKey("4", 4)
    tc.AddKey("5", 5)
    tc.AddKey("6", 6)
    assert(tc.Size() === 6)
  }

  "size" should "get keys set size from memory" in {
    println("size 1 : "+tc.Size())
    assert(tc.Size() === 6)
  }

  "remove" should "get data from memory" in {
    tc.RemoveKey("1")
    println("size 2 : "+tc.Size())
    assert(tc.Size() === 5)
  }

  "size again" should "get keys set size from memory" in {
    tc.Size()
    assert(tc.Size() === 5)
  }

}
