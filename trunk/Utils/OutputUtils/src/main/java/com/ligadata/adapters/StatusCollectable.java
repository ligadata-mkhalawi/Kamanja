package com.ligadata.adapters;

public interface StatusCollectable {

    /**
     * Add a message to the underlying statistics gathering
     * @param batchId batch ID
     * @param retryNumber Number of times to retry
     * @param sourceOfStatus Source of the status
     * @return true if recorded
     */
    public boolean externalizeStatusMessage(long batchId, long retryNumber, String sourceOfStatus);

    /**
     * Add the status message to the underlying status sturcutre. the whole thing will be externalized when
     * externalizeStatusMessage(String message, String sourceOfStatus) is called
     * @param key the key
     * @param success the success message
     * @param failure the failure message
     */
    public void addStatus(String key, String success, String failure);

    /**
     * Add a message to appear under the Messages[] array in the Status message
     * @param key the key
     * @param msg the message
     * @param isRequired is it required
     */
    public void addStatusMessage(String key, String msg, boolean isRequired);

    /**
     * Set the completion code for this batch
     * @param key the key
     * @param status the status message
     */
    public void setCompletionCode(String key, String status);

    /**
     *
     * @param config configuration
     * @param destinationComponentName component data is sent to
     * @throws Exception some exception
     */
    void init (String config, String destinationComponentName) throws Exception;

    /**
     * Clean up and close the recorder
     */
    void close();
}