package com.ligadata.adapters.container;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ligadata.KamanjaBase.ContainerInterface;
import com.ligadata.KamanjaBase.MessageContainerBase;
import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.BufferedMessageProcessor;
import com.ligadata.tools.SaveContainerDataComponent;
import com.ligadata.adapters.StatusCollectable;

public class DedupContainerSink implements BufferedMessageProcessor { //extends AbstractJDBCSink {
	static Logger logger = LogManager.getLogger(DedupContainerSink.class);
	protected StatusCollectable statusWriter = null;

	protected class DedupPartition {
		Integer key1;
		Integer key2;
		ArrayList<Long> hashValues;

		private DedupPartition(Integer key1, Integer key2) {
			this.key1 = key1;
			this.key2 = key2;
			this.hashValues = new ArrayList<Long>();
		}
	}
	
	private HashMap<String, DedupPartition> buffer = new HashMap<String, DedupPartition>();
	private String containerName = null;
	private SaveContainerDataComponent writer = null;
	
	@Override
	public void init(AdapterConfiguration config, StatusCollectable sw) throws Exception {
		statusWriter = sw;
		writer = new SaveContainerDataComponent();
		String configFile = config.getProperty(AdapterConfiguration.METADATA_CONFIG_FILE);
		if(configFile == null)
			throw new Exception("Metadata config file not specified.");
		
		containerName = config.getProperty(AdapterConfiguration.METADATA_CONTAINER_NAME);
		if(containerName == null)
			throw new Exception("Container name not specified.");

		writer.Init(configFile);
	}

	@Override
	public boolean addMessage(String message) {
		String[] fields = message.split(",");
		if(fields.length < 3) {
			logger.error("Incorrect message. Expecting atleast 3 fields. Message: " + message);
			return false;
		}
		
		try {
			String key = fields[0] + ":" + fields[1];
			DedupPartition dedup = buffer.get(key);
			if (dedup == null) {
				dedup = new DedupPartition(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]));
				dedup.hashValues.add(Long.parseLong(fields[2]));
				buffer.put(key, dedup);
			} else {
				dedup.hashValues.add(Long.parseLong(fields[2]));
			}
		} catch (Exception e) {
			logger.error("Error: " + e.getMessage(), e);
			return false;
		}
		
		return true;
	}

	@Override
	public void processAll(long batchid, long retryNumber) throws Exception {
		ArrayList<MessageContainerBase> data = new ArrayList<MessageContainerBase>();
		//logger.info("Container name is " + containerName);
		for (String key : buffer.keySet()) {
			DedupPartition dedup = buffer.get(key);
			ContainerInterface container = writer.GetContainerInterface(containerName);
			
			container.set(0, dedup.key1);
			container.set(1, dedup.key2);
			container.set(2, dedup.hashValues.toArray(new Long[0]));
			data.add(container);
		}
		writer.SaveContainerInterface(containerName, data.toArray(new ContainerInterface[0]), true, true);
	}

	@Override
	public void clearAll() {
		buffer.clear();
	}

	@Override
	public void close() {
		writer.Shutdown();
	}

}
