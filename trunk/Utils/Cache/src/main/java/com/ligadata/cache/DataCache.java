package com.ligadata.cache;

import java.util.Map;
import java.util.List;

/**
 * Created by Saleh on 3/15/2016.
 */
public interface DataCache {
    public void init(String jsonString, CacheCallback listenCallback);
    public void start();
    public boolean isKeyInCache(String key);
    public void put(String key, Object value);
    public void put(Map map);
    public Object get(String key);
    public Map<String, Object> get(String[] keys);
    public String[] getKeys();
    public Map<String, Object> getAll();
    public void shutdown();
    //tree cache
    public void put(String containerName, String timestamp, String key, Object value);
    public void put(String containerName, String key, Object value);
    public void get(String containerName, Map<String, Map<String, Object>> map);
    public Map<String, Object> get(String containerName, String timestamp);
    public Object get(String containerName, String timestamp, String key);
    public Map<String, Object> getFromRoot(String rootNode, String key);
    public void del(String containerName);
    public void del(String containerName, String timestamp);
    public void del(String containerName, String timestamp, String key);

}
