package com.ligadata.kamanja.utils.scheduler;

import java.util.Map;

/**
 * Created by Saleh on 8/21/2016.
 */
public interface KamanjaScheduler {
    void add(String name, CacheCallback callback);

    void remove(String jobName);

    void update(String jobName);

    Map<String, String> getAll();

    void shutdown();

}
