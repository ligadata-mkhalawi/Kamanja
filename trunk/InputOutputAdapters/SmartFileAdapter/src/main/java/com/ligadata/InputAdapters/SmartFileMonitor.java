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
}
