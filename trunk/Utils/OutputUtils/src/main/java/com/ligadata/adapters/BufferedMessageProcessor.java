package com.ligadata.adapters;

public interface BufferedMessageProcessor {
	public void init(AdapterConfiguration config, StatusCollectable stats) throws Exception;

	public boolean addMessage(String message);

	public void processAll(long batchId) throws Exception;

	public void clearAll();

	void close();
}
