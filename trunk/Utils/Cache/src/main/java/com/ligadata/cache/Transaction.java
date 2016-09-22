package com.ligadata.cache;

import java.util.Map;

abstract public class Transaction implements DataCacheOperations {
    private DataCache _cache = null;

    public Transaction(DataCache cache) {
        _cache = cache;
    }

    final public DataCache getDataCache() {
        return _cache;
    }

    final public void resetDataCache() {
        _cache = null;
    }

    final public boolean isValidDataCache() {
        return (_cache != null);
    }

    private void CheckForValidTxn() throws Exception {
        if (!isValidDataCache())
            throw new Exception("Not found valid DataCache in transaction");
    }

    // Default implementations for DataCacheOperations. Derivers will override if they need it. -- Begin
    @Override
    public boolean isKeyInCache(String key) throws Exception, Throwable {
        CheckForValidTxn();
        return _cache.isKeyInCache(key);
    }

    @Override
    public void put(String key, Object value) throws Exception, Throwable {
        CheckForValidTxn();
        _cache.put(key, value);
    }

    @Override
    public void put(Map map) throws Exception, Throwable {
        CheckForValidTxn();
        _cache.put(map);
    }

    @Override
    public Object get(String key) throws Exception, Throwable {
        CheckForValidTxn();
        return _cache.get(key);
    }

    @Override
    public Map<String, Object> get(String[] keys) throws Exception, Throwable {
        CheckForValidTxn();
        return _cache.get(keys);
    }

    @Override
    public String[] getKeys() throws Exception, Throwable {
        CheckForValidTxn();
        return _cache.getKeys();
    }

    @Override
    public Map<String, Object> getAll() throws Exception, Throwable {
        CheckForValidTxn();
        return _cache.getAll();
    }

    @Override
    public void remove(String key) throws Exception, Throwable {
        CheckForValidTxn();
        _cache.remove(key);
    }

    @Override
    public void clear() throws Exception, Throwable {
        CheckForValidTxn();
        _cache.clear();
    }

    @Override
    public int size() throws Exception, Throwable {
        CheckForValidTxn();
        return _cache.size();
    }

    //tree cache
    @Override
    public void put(String containerName, String timestamp, String key, Object value) throws Exception, Throwable {
        CheckForValidTxn();
        _cache.put(containerName, timestamp, key, value);
    }

    @Override
    public void put(String containerName, String key, Object value) throws Exception, Throwable {
        CheckForValidTxn();
        _cache.put(containerName, key, value);
    }

    @Override
    public void get(String containerName, Map<String, Map<String, Object>> map) throws Exception, Throwable {
        CheckForValidTxn();
        _cache.get(containerName, map);
    }

    @Override
    public Map<String, Object> get(String containerName, String timestamp) throws Exception, Throwable {
        CheckForValidTxn();
        return _cache.get(containerName, timestamp);
    }

    @Override
    public Object get(String containerName, String timestamp, String key) throws Exception, Throwable {
        CheckForValidTxn();
        return _cache.get(containerName, timestamp, key);
    }

    @Override
    public Map<String, Object> getFromRoot(String rootNode, String key) throws Exception, Throwable {
        CheckForValidTxn();
        return _cache.getFromRoot(rootNode, key);
    }

    @Override
    public void del(String containerName) throws Exception, Throwable {
        CheckForValidTxn();
        _cache.del(containerName);
    }

    @Override
    public void del(String containerName, String timestamp) throws Exception, Throwable {
        CheckForValidTxn();
        _cache.del(containerName, timestamp);
    }

    @Override
    public void del(String containerName, String timestamp, String key) throws Exception, Throwable {
        CheckForValidTxn();
        _cache.del(containerName, timestamp, key);
    }
    // Default implementations for DataCacheOperations. Derivers will override if they need it. -- End

    // Same as commit
    final public void end() throws Exception, Throwable {
        commit();
    }

    abstract public void commit() throws Exception, Throwable;

    abstract public void rollback() throws Exception, Throwable;
}
