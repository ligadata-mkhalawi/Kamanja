package com.ligadata.cache;

abstract public class Transaction implements DataCacheOperations {
    private DataCache _cache = null;

    public Transaction(DataCache cache) {
        _cache = cache;
    }

    public DataCache getDataCache() {
        return _cache;
    }

    abstract public void end(); // Same as commit
    abstract public void commit();
    abstract public void rollback();
}
