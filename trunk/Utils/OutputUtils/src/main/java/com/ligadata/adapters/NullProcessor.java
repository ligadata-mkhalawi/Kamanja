package com.ligadata.adapters;

public class NullProcessor implements BufferedMessageProcessor {

	@Override
	public void init(AdapterConfiguration config) throws Exception {

	}

	@Override
	public boolean addMessage(String message) {
		return true;
	}

	@Override
	public void processAll() throws Exception {

	}

	@Override
	public void clearAll() {

	}

	@Override
	public void close() {

	}

}
