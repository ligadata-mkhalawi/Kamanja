package com.ligadata.adapters.container;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ligadata.KamanjaBase.DataDelimiters;
import com.ligadata.KamanjaBase.DelimitedData;
import com.ligadata.KamanjaBase.MessageContainerBase;
import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.BufferedMessageProcessor;
import com.ligadata.tools.SaveContainerDataComponent;

public class DedupContainerSink implements BufferedMessageProcessor { //extends AbstractJDBCSink {
	static Logger logger = LogManager.getLogger(DedupContainerSink.class);

	protected class DedupPartition {
		String key1;
		String key2;
		StringBuffer hashValues;

		private DedupPartition(String key1, String key2) {
			this.key1 = key1;
			this.key2 = key2;
			this.hashValues = new StringBuffer();
		}
	}
	
	private HashMap<String, DedupPartition> buffer = new HashMap<String, DedupPartition>();
	private String containerName = null;
	private SaveContainerDataComponent writer = null;
	
	@Override
	public void init(AdapterConfiguration config) throws Exception {
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
				dedup = new DedupPartition(fields[0], fields[1]);
				dedup.hashValues.append(fields[2]);
				buffer.put(key, dedup);
			} else {
				dedup.hashValues.append("~").append(fields[2]);
			}
		} catch (Exception e) {
			logger.error("Error: " + e.getMessage(), e);
			return false;
		}
		
		return true;
	}

	@Override
	public void processAll() throws Exception {
		ArrayList<MessageContainerBase> data = new ArrayList<MessageContainerBase>();
		//logger.info("Container name is " + containerName);
		for (String key : buffer.keySet()) {
			DedupPartition dedup = buffer.get(key);
			MessageContainerBase container = writer.GetMessageContainerBase(containerName);
			
			String[] tokens = new String[3];
			tokens[0] = dedup.key1;
			tokens[1] = dedup.key2;
			tokens[2] = dedup.hashValues.toString();
			
			DataDelimiters delimiters = new DataDelimiters();
			delimiters.keyAndValueDelimiter_$eq(",");
			delimiters.fieldDelimiter_$eq(",");
			delimiters.valueDelimiter_$eq("~");
			
			DelimitedData id = new DelimitedData("", delimiters);
			id.tokens_$eq(tokens);
			id.curPos_$eq(0);
			
			container.populate(id);
			data.add(container);
		}
		writer.SaveMessageContainerBase(containerName, data.toArray(new MessageContainerBase[0]), true, true);
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
