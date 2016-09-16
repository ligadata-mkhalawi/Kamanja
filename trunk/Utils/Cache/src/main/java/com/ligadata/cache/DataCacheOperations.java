package com.ligadata.cache;

import java.util.Map;
import java.util.List;

public interface DataCacheOperations {
    public boolean isKeyInCache(String key) throws Exception, Throwable;
    public void put(String key, Object value) throws Exception, Throwable;
    public void put(Map map) throws Exception, Throwable;
    public Object get(String key) throws Exception, Throwable;
    public Map<String, Object> get(String[] keys) throws Exception, Throwable;
    public String[] getKeys() throws Exception, Throwable;
    public Map<String, Object> getAll() throws Exception, Throwable;
    public void remove(String key) throws Exception, Throwable;

    //tree cache
    public void put(String containerName, String timestamp, String key, Object value) throws Exception, Throwable;
    public void put(String containerName, String key, Object value) throws Exception, Throwable;
    public void get(String containerName, Map<String, Map<String, Object>> map) throws Exception, Throwable;
    public Map<String, Object> get(String containerName, String timestamp) throws Exception, Throwable;
    public Object get(String containerName, String timestamp, String key) throws Exception, Throwable;
    public Map<String, Object> getFromRoot(String rootNode, String key) throws Exception, Throwable;
    public void del(String containerName) throws Exception, Throwable;
    public void del(String containerName, String timestamp) throws Exception, Throwable;
    public void del(String containerName, String timestamp, String key) throws Exception, Throwable;
    public Transaction beginTx();
    public void endTx(Transaction tx);
    public void commitTx(Transaction tx);    
    public void rollbackTx(Transaction tx);
}
