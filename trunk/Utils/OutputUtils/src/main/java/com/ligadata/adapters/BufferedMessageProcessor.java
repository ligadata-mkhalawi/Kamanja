package com.ligadata.adapters;

public interface BufferedMessageProcessor {
	public void init(AdapterConfiguration config) throws Exception;

	public boolean addMessage(String message);
	
	public void processAll() throws Exception;

	public void clearAll();

	void close();
}
