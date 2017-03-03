package com.ligadata.cache;

/**
 * Created by Saleh on 3/15/2016.
 */
public interface DataCache extends DataCacheOperations{
    public void init(String jsonString, CacheCallback listenCallback);
    public void start();
    public void shutdown();
    public Transaction beginTransaction();
}
