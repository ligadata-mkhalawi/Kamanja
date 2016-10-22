package com.ligadata.adapters;

public class NullProcessor implements BufferedMessageProcessor {

	@Override
	public void init(AdapterConfiguration config, StatusCollectable stats) throws Exception {

	}

	@Override
	public boolean addMessage(String message) {
		return true;
	}

	@Override
	public void processAll(long batchId) throws Exception {

	}

	@Override
	public void clearAll() {

	}

	@Override
	public void close() {

	}

}
