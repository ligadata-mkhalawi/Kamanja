package com.ligadata.kamanja.utils.scheduler;

import java.util.List;
import java.util.Map;

/**
 * Created by Saleh on 8/21/2016.
 */
public interface KamanjaScheduler {
    void add(String name, SchedulerCallback callback);

    void remove(String jobName, String groupName);

    void update(String jobName);

    Map<String, Object> getAll();

    void shutdown();

}
