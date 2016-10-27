package com.ligadata.adapters.container;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ligadata.KamanjaBase.ContainerInterface;
import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.BufferedMessageProcessor;
import com.ligadata.tools.SaveContainerDataComponent;
import com.ligadata.adapters.StatusCollectable;

public class AITUserDailyProfileSink implements BufferedMessageProcessor {
	static Logger logger = LogManager.getLogger(AITUserDailyProfileSink.class);

	private HashMap<String, HashMap<String, Object[]>> buffer = new HashMap<String, HashMap<String, Object[]>>();
	private String containerName = null;
	private String[] collectionContainerNames;	
	private String fieldDelimiter = ",";
	private String[] fieldNames;	
	private String[] collectionFieldNames;	
	private int[] sumFields = null;

	protected StatusCollectable statusWriter = null;
	
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
	public void init(AdapterConfiguration config, StatusCollectable sw) throws Exception {
		statusWriter = sw;
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
			throw new Exception("Field names not specified for container " + containerName);
		fieldNames = fieldNamesStr.split(",");

		String collectionFieldNamesStr = config.getProperty(AdapterConfiguration.COLLECTION_FIELD_NAMES);
		if(collectionFieldNamesStr == null)
			throw new Exception("Collection field names not specified for container " + containerName);
		collectionFieldNames = collectionFieldNamesStr.split(",");

		String collectionContainerNamesStr = config.getProperty(AdapterConfiguration.COLLECTION_CONTAINER_NAMES);
		if(collectionContainerNamesStr == null)
			throw new Exception("Collection container names not specified for container " + containerName);
		collectionContainerNames = collectionContainerNamesStr.split(",");

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
			
			newRecord[0] = new Integer(fields[0]);
			newRecord[1] = new Long(fields[1]);
			for(int i = 2; i < fields.length; i++) {
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
	public void processAll(long batchId, long retryNumber) throws Exception {
			ArrayList<ContainerInterface> data = new ArrayList<ContainerInterface>();
			for( HashMap<String, Object[]> record : buffer.values()) {
				ArrayList<ContainerInterface> uaList = new ArrayList<ContainerInterface>();

				Object[] rec = null;
				for(Object[] fields : record.values()) {
			       	rec = fields;
			       	ContainerInterface profileRec = writer.GetContainerInterface(collectionContainerNames[0]);
			       	profileRec.set(fieldNames[2], fields[2]); // user
			       	profileRec.set(fieldNames[3], fields[3]); // ait
			       	profileRec.set(fieldNames[4], fields[4]); // jobcode

			       	ContainerInterface counters = writer.GetContainerInterface(collectionContainerNames[1]);
			       	for(int i = 5; i < fields.length; i++) {
						counters.set(fieldNames[i], fields[i]);
					}
			       	profileRec.set(collectionFieldNames[1], counters);
			       	uaList.add(profileRec);
				}
				
		       	ContainerInterface container = writer.GetContainerInterface(containerName);
				container.set(fieldNames[0], rec[0]); // hashkey
				container.set(fieldNames[1], rec[1]); // date
				container.set(collectionFieldNames[0], uaList.toArray(new ContainerInterface[0]));
						
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
