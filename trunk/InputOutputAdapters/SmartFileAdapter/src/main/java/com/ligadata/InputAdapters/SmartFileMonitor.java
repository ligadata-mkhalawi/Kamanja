package com.ligadata.InputAdapters;

/**
 * Created by Yasser on 3/10/2016.
 */
public interface SmartFileMonitor {
    void init(String adapterSpecificCfgJson);
    void monitor();
    void shutdown();

    //basically will remove the file from monitor's map so that it could be processed and moved again if added again
    void markFileAsProcessed(String filePath);

    //when passing true => monitor should stop listing files in folders
    void setMonitoringStatus(boolean status);

    // List files for the given path
    String[] listFiles(String path);
}
