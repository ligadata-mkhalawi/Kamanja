package com.ligadata.adapters;

public interface StatusCollectable {

    /**
     * Add a message to the underlying statistics gathering
     * @param message
     * @param soureOfStatus
     * @return true if recorded
     */
    public boolean externalizeStatusMessage(String batchId, String retryNumber, String sourceOfStatus);

    /**
     * Add the status message to the underlying status sturcutre. the whole thing will be externalized when
     * externalizeStatusMessage(String message, String sourceOfStatus) is called
     * @param key
     * @param value
     */
    public void addStatus(String key, String success, String failure);

    /**
     * Add a message to appear under the Messages[] array in the Status message
     * @param msg
     */
    public void addStatusMessage(String key, String msg);

    /**
     * Set the completion code for this batch
     * @param status
     */
    public void setCompletionCode(String key, String status);

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