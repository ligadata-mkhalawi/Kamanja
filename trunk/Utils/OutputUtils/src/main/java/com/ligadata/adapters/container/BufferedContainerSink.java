package com.ligadata.adapters.container;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ligadata.KamanjaBase.DataDelimiters;
import com.ligadata.KamanjaBase.DelimitedData;
import com.ligadata.KamanjaBase.MessageContainerBase;
import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.BufferedMessageProcessor;
import com.ligadata.tools.SaveContainerDataComponent;

public class BufferedContainerSink implements BufferedMessageProcessor {
	static Logger logger = LogManager.getLogger(BufferedContainerSink.class);

	protected class PartitionData {
		ArrayList<String> keys;
		HashMap<String, ArrayList<Object>> records;

		private PartitionData(ArrayList<String> keys) {
			this.keys = keys;
			this.records = new HashMap<String, ArrayList<Object>>();
		}
	}

	private HashMap<String, HashMap<String, PartitionData>> buffer = new HashMap<String, HashMap<String, PartitionData>>();
	private String containerName = null;
	private String fieldDelimiter = ",";
	private String valueDelimiter = "~";
	private String kvDelimiter = ":";
	
	private int fixedFields = 0;
	private int[] groupByFields = null;
	private int[] sumFields = null;
	
	private SaveContainerDataComponent writer = null;
	
	private int[] csvToArrayOfInt(String str) {
		String[] stringArray = str.split(",");
		int[] intArray = new int[stringArray.length];
		for (int i = 0; i < stringArray.length; i++) {
			intArray[i] = Integer.parseInt(stringArray[i]);
		}

		return intArray;
	}
	
	private boolean checkIfExists(int[] array, int value) {
		for(int i : array)
			if(i == value)	return true;
		
		return false;
	}

	@Override
	public void init(AdapterConfiguration config) throws Exception {
		writer = new SaveContainerDataComponent();
		String configFile = config.getProperty(AdapterConfiguration.METADATA_CONFIG_FILE);
		if(configFile == null)
			throw new Exception("Metadata config file not specified.");
		
		fieldDelimiter = config.getProperty(AdapterConfiguration.MESSAGE_FIELD_DELIMITER, ",");
		valueDelimiter = config.getProperty(AdapterConfiguration.MESSAGE_VALUE_DELIMITER, "~");
		kvDelimiter = config.getProperty(AdapterConfiguration.MESSAGE_KEYVALUE_DELIMITER, ":");

		containerName = config.getProperty(AdapterConfiguration.METADATA_CONTAINER_NAME);
		if(containerName == null)
			logger.warn("Container name not specified in the configuration file, will be expecting container name as the first field in the message.");

		String groupByFieldsStr = config.getProperty(AdapterConfiguration.MESSAGE_GROUP_BY_FIELDS);
		if(groupByFieldsStr == null)
			throw new Exception("Partition keys not specified for container " + containerName);
		groupByFields = csvToArrayOfInt(groupByFieldsStr);

		String sumFieldsStr = config.getProperty(AdapterConfiguration.MESSAGE_SUM_FIELDS);
		if(sumFieldsStr == null)
			throw new Exception("Primary keys not specified for container " + containerName);
		sumFields = csvToArrayOfInt(sumFieldsStr);

		writer.Init(configFile);
	}

	@Override
	public boolean addMessage(String message) {
		try {
			String containerKey = containerName;
			String[] fields = message.split(fieldDelimiter);
			if(containerName == null) {
				containerKey = fields[0];
				fields = Arrays.copyOfRange(fields, 1, fields.length);
			}
			
			ArrayList<String> keyData = new ArrayList<String>();
			ArrayList<Object> recordData = new ArrayList<Object>();
			StringBuffer partitionKey = new StringBuffer();
			StringBuffer groupByKey = new StringBuffer();
			
			for(int i = 0; i < fields.length; i++) {
				if(i < fixedFields) {
					keyData.add(fields[i]);
					partitionKey.append(fields[i]);
				} else
					recordData.add(fields[i]);

				if(checkIfExists(groupByFields, i))
					groupByKey.append(fields[i]);
			}
			
			HashMap<String, PartitionData> partitions = buffer.get(containerKey);
			if (partitions == null) {
				partitions = new HashMap<String, PartitionData>();
				buffer.put(containerKey, partitions);
			}
			
			PartitionData data = partitions.get(partitionKey.toString());
			if(data == null) {
				data = new PartitionData(keyData);
				partitions.put(partitionKey.toString(), data);
			}
			
			data.records.put(groupByKey.toString(), recordData);

		} catch (Exception e) {
			logger.error("Error: " + e.getMessage(), e);
			return false;
		}
		
		return true;
	}

	@Override
	public void processAll() throws Exception {
		DataDelimiters delimiters = new DataDelimiters();
		delimiters.keyAndValueDelimiter_$eq(kvDelimiter);
		delimiters.fieldDelimiter_$eq(fieldDelimiter);
		delimiters.valueDelimiter_$eq(valueDelimiter);
	
		for (String containerKey : buffer.keySet()) {
			logger.debug("Container name is " + containerKey);
			ArrayList<MessageContainerBase> data = new ArrayList<MessageContainerBase>();
			HashMap<String, PartitionData> partitions = buffer.get(containerKey);
			for(PartitionData record : partitions.values()) {
				ArrayList<String> tokens = new ArrayList<String>();
				tokens.addAll(record.keys);
				for(ArrayList<Object> fields : record.records.values()) {
					for(Object f : fields) {
						if(f instanceof String)
							tokens.add((String)f);
						else
							tokens.add(f.toString());
					}
				}
			
				MessageContainerBase container = writer.GetMessageContainerBase(containerKey);
						
				DelimitedData id = new DelimitedData("", delimiters);
				id.tokens_$eq(tokens.toArray(new String[0]));
				id.curPos_$eq(0);
			
				container.populate(id);
				data.add(container);
			}
			writer.SaveMessageContainerBase(containerKey, data.toArray(new MessageContainerBase[0]), true, true);
		}
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
