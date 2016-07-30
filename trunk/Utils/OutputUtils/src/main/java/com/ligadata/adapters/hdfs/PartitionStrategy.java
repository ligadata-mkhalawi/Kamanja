package com.ligadata.adapters.hdfs;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.avro.generic.GenericData.Record;

public class PartitionStrategy {
	private class PartitionKey {
		String attribute;
		DateFormat format;
		
		private PartitionKey(String attribute, String formatStr) {
			this.attribute = attribute;
			this.format = new SimpleDateFormat(formatStr);
		}

		private PartitionKey(String attribute) {
			this.attribute = attribute;
			this.format = null;
		}
	}
	
	private ArrayList<PartitionKey> keys = null;
	private String partitionString = null;
	private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	public PartitionStrategy() {
	}
	
	public PartitionStrategy(String format, String inputDateFormat) {
		if(format == null || "".equals(format))
			return;
		
		keys = new ArrayList<PartitionKey>();
				
		// extract parameters between ${..}
		Matcher matcher = Pattern.compile("\\$\\{([^\\}]+)").matcher(format);
		int pos = -1;
		while (matcher.find(pos + 1)) {
			pos = matcher.start();
			String token = matcher.group(1);
			String[] attr = token.split(":");
			PartitionKey key;
			if(attr.length > 1) 
				key = new PartitionKey(attr[0], attr[1]);
			else
				key= new PartitionKey(attr[0]);
			
			keys.add(key);
		}

		partitionString = format.replaceAll("\\$\\{[^\\}]+\\}", "%s");

//		for(String token: format.split(",")) {
//			String[] attr = token.split(":");
//			PartitionKey key;
//			if(attr.length > 1) 
//				key = new PartitionKey(attr[0], attr[1]);
//			else
//				key= new PartitionKey(attr[0]);
//			
//			keys.add(key);
//		}
		
		inputFormat = new SimpleDateFormat(inputDateFormat);
	}

	public String getPartition(Record rec) {
		String partition = "";
		if(keys != null) {
			Object[] values = new Object[keys.size()];
			for (int i = 0; i < keys.size(); i++) {
				PartitionKey key = keys.get(i);
				String value = "";
				if(key.format == null)
					value = rec.get(key.attribute).toString();
				else {
					try {
						Date dateValue = inputFormat.parse(rec.get(key.attribute).toString());
						value = key.format.format(dateValue);
					} catch (ParseException e) {
					}
				}
				values[i] = (value == null) ? "" : value;
			}
			//partition = partition.substring(1);
			partition = String.format(partitionString, values);
		}
		
		return partition;
	}

}
