package com.ligadata.cache.infinispan

import java.util

import com.ligadata.cache.{CacheCallbackData, CacheCallback}
import org.infinispan.atomic.impl.AtomicHashMap
import org.infinispan.notifications.Listener
import org.infinispan.notifications.cachelistener.annotation.{TopologyChanged, CacheEntryRemoved, CacheEntryModified, CacheEntryCreated}
import org.infinispan.notifications.cachelistener.event.{CacheEntryRemovedEvent, CacheEntryModifiedEvent, TopologyChangedEvent, CacheEntryCreatedEvent}
import org.infinispan.tree.impl.NodeKey

/**
  * Created by Saleh on 6/14/2016.
  */

@Listener
class EventCacheListener(val listenCallback: CacheCallback) {

  @CacheEntryCreated
  def observeAdd(event: CacheEntryCreatedEvent[String, String]): Unit = {
    if (event.isPre())
      return;

    //    if (listenCallback != null) {
    //      val key: NodeKey = event.getKey().asInstanceOf[NodeKey]
    //      if (key.getContents == NodeKey.Type.DATA) {
    //        val map: AtomicHashMap[String, String] = event.getValue.asInstanceOf[AtomicHashMap[String, String]]
    //        val it: util.Iterator[String] = map.keySet().iterator()
    //        while (it.hasNext) {
    //          val k: String = it.next()
    //          listenCallback.call(new CacheCallbackData("Put", k, map.get(k)));
    //        }
    //      }
    //    }
    if (listenCallback != null)
      listenCallback.call(new CacheCallbackData("Put", event.getKey(), event.getValue));
    //    System.out.println("Cache entry " + key.toString + map.get("1") + " added in cache " + event.getCache());
  }

  @CacheEntryModified
  def observeUpdate(event: CacheEntryModifiedEvent[String, String]) {
    if (event.isPre())
      return;

    //    if (listenCallback != null) {
    //      val key: NodeKey = event.getKey().asInstanceOf[NodeKey]
    //      if (key.getContents == NodeKey.Type.DATA) {
    //        val map: AtomicHashMap[String, String] = event.getValue.asInstanceOf[AtomicHashMap[String, String]]
    //        val it: util.Iterator[String] = map.keySet().iterator()
    //        while (it.hasNext) {
    //          val k: String = it.next()
    //          listenCallback.call(new CacheCallbackData("Update", k, map.get(k)));
    //        }
    //      }
    //    }
    if (listenCallback != null)
      listenCallback.call(new CacheCallbackData("Update", event.getKey(), event.getValue));

    //    System.out.println("Cache entry " + key.toString + map.get(key.toString) + " modified in cache " + event.getCache());
  }

  @CacheEntryRemoved
  def observeRemove(event: CacheEntryRemovedEvent[String, String]) {
    if (event.isPre())
      return;

    //    if (listenCallback != null) {
    //      val key: NodeKey = event.getKey().asInstanceOf[NodeKey]
    //      if (key.getContents == NodeKey.Type.DATA) {
    //        val map: AtomicHashMap[String, String] = event.getValue.asInstanceOf[AtomicHashMap[String, String]]
    //        val it: util.Iterator[String] = map.keySet().iterator()
    //        while (it.hasNext) {
    //          val k: String = it.next()
    //          listenCallback.call(new CacheCallbackData("Remove", k, map.get(k)));
    //        }
    //      }
    //    }
    if (listenCallback != null)
      listenCallback.call(new CacheCallbackData("Remove", event.getKey(), event.getValue));

    //    System.out.println("Cache entry " + key.toString + map.get(key.toString) + " removed in cache " + event.getCache());

  }

  @TopologyChanged
  def observeTopologyChange(event: TopologyChangedEvent[String, String]) {
    if (event.isPre())
      return;

    //    System.out.println("Cache entry " + event.getCache().getName() + " topology changed, new membership is " + event.getConsistentHashAtEnd().getMembers());
  }

}
