package com.ligadata.adapters;

public interface StatusCollectable {

    /**
     * Add a message to the underlying statistics gathering
     * @param message
     * @param soureOfStatus
     * @return true if recorded
     */
    public boolean externalizeStatusMessage(String batchId, String sourceOfStatus);

    /**
     * Add the status message to the underlying status sturcutre. the whole thing will be externalized when
     * externalizeStatusMessage(String message, String sourceOfStatus) is called
     * @param key
     * @param value
     */
    public void addStatus(String key, String value);

    /**
     *
     * @param config
     * @param destinationComponentName
     * @throws Exception
     */
    void init (String config, String destinationComponentName) throws Exception;

    /**
     * Clean up and close the recorder
     */
    void close();
}