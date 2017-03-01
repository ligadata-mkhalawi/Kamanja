package com.ligadata.adapters;

public interface StatusCollectable {

    /**
     * Add a message to the underlying statistics gathering
     * @param batchId
     * @param retryNumber
     * @param sourceOfStatus
     * @return true if recorded
     */
    public boolean externalizeStatusMessage(long batchId, long retryNumber, String sourceOfStatus);

    /**
     * Add the status message to the underlying status sturcutre. the whole thing will be externalized when
     * externalizeStatusMessage(String message, String sourceOfStatus) is called
     * @param key
     * @param success
     * @param failure
     */
    public void addStatus(String key, String success, String failure);

    /**
     * Add a message to appear under the Messages[] array in the Status message
     * @param key
     * @param msg
     * @param isRequired
     */
    public void addStatusMessage(String key, String msg, boolean isRequired);

    /**
     * Set the completion code for this batch
     * @param key
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