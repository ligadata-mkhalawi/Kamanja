package com.ligadata.adapters.container;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ligadata.KamanjaBase.JsonData;
import com.ligadata.KamanjaBase.MessageContainerBase;
import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.BufferedMessageProcessor;
import com.ligadata.tools.SaveContainerDataComponent;

import scala.Option;
import scala.collection.immutable.Map;
import scala.collection.mutable.ArrayBuffer;

public class AITUserDailyProfileSink implements BufferedMessageProcessor {
	static Logger logger = LogManager.getLogger(AITUserDailyProfileSink.class);

	private HashMap<String, HashMap<String, Object[]>> buffer = new HashMap<String, HashMap<String, Object[]>>();
	private String containerName = null;
	private String fieldDelimiter = ",";
	private String[] fieldNames;	
	private String[] collectionFieldNames;	
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
		
		containerName = config.getProperty(AdapterConfiguration.METADATA_CONTAINER_NAME);
		if(containerName == null)
			throw new Exception("Container name not specified in the configuration file.");

		fieldDelimiter = config.getProperty(AdapterConfiguration.MESSAGE_FIELD_DELIMITER, ",");

		String fieldNamesStr = config.getProperty(AdapterConfiguration.MESSAGE_FIELD_NAMES);
		if(fieldNamesStr == null)
			throw new Exception("Field namess not specified for container " + containerName);
		fieldNames = fieldNamesStr.split(",");

		String collectionFieldNamesStr = config.getProperty(AdapterConfiguration.COLLECTION_FIELD_NAMES);
		if(collectionFieldNamesStr == null)
			throw new Exception("Collection field names not specified for container " + containerName);
		collectionFieldNames = collectionFieldNamesStr.split(",");

		String sumFieldsStr = config.getProperty(AdapterConfiguration.MESSAGE_SUM_FIELDS);
		if(sumFieldsStr == null)
			throw new Exception("Summation fields not specified for container " + containerName);
		sumFields = csvToArrayOfInt(sumFieldsStr);

		writer.Init(configFile);
	}

	@Override
	public boolean addMessage(String message) {
		try {
			String[] fields = message.split(fieldDelimiter);
			if(fields.length < fieldNames.length) {
				logger.error("Incorrect message. Expecting " + fieldNames.length + " fields. Message: " + message);
				return false;
			}

			String partitionKey = fields[0]+":"+fields[1];
			String groupKey = fields[2]+":"+fields[3];
			Object[] newRecord = new Object[fields.length];
			
			for(int i = 0; i < fields.length; i++) {
				if(checkIfExists(sumFields, i))
					newRecord[i] = new Long(fields[i]);
				else
					newRecord[i] = fields[i];
			}
			
			HashMap<String, Object[]> partitionData = buffer.get(partitionKey);
			if (partitionData == null) {
				partitionData = new HashMap<String, Object[]>();
				buffer.put(partitionKey, partitionData);
			}
			
			Object[] oldRecord = partitionData.put(groupKey, newRecord);
			if(oldRecord != null) {
				for(int i : sumFields) {
					Long sum = (Long)oldRecord[i] + (Long)newRecord[i];
					newRecord[i] = sum;
				}
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
			for( HashMap<String, Object[]> record : buffer.values()) {
				ArrayBuffer<Map<String, Object>> uaList = new ArrayBuffer<Map<String, Object>>();

				Object[] rec = null;
				for(Object[] fields : record.values()) {
			       	rec = fields;
			       	scala.collection.mutable.HashMap<String, Object> uaRec = new scala.collection.mutable.HashMap<String, Object>();
			       	uaRec.put(fieldNames[2], fields[2]); // user
			       	uaRec.put(fieldNames[3], fields[3]); // ait
			       	uaRec.put(fieldNames[4], fields[4]); // jobcode

			       	scala.collection.mutable.HashMap<String, Object> counters = new scala.collection.mutable.HashMap<String, Object>();
			       	for(int i = 5; i < fields.length; i++) {
						if(fields[i] instanceof String)
							counters.put(fieldNames[i], fields[i]);
						else
							counters.put(fieldNames[i], fields[i].toString());
					}
			       	uaRec.put(collectionFieldNames[1], (new scala.collection.immutable.HashMap<String, Object>()).$plus$plus(counters));
			       	uaList.$plus$eq((new scala.collection.immutable.HashMap<String, Object>()).$plus$plus(uaRec));
				}
				
				scala.collection.mutable.HashMap<String, Object> json = new scala.collection.mutable.HashMap<String, Object>();
		       	json.put(fieldNames[0], rec[0]); // hashkey
		       	json.put(fieldNames[1], rec[1]); // date
		       	json.put(collectionFieldNames[0], uaList.toList());
							
				MessageContainerBase container = writer.GetMessageContainerBase(containerName);
						
				JsonData jd = new JsonData("");
		        jd.root_json_$eq(Option.apply((Object)(new scala.collection.immutable.HashMap<String, Object>()).$plus$plus(json)));
		       	jd.cur_json_$eq(Option.apply((Object)( new scala.collection.immutable.HashMap<String, Object>()).$plus$plus(json)));
				container.populate(jd);
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
