package com.ligadata.kamanja.utils.scheduler;

/**
 * Created by Saleh on 8/22/2016.
 */
public interface CacheCallback {
    void call(String SchedulerName, String TriggerTime, String[] Payload) throws Exception;
}
