package com.ligadata.test;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData.Record;

import com.ligadata.adapters.hdfs.PartitionStrategy;

public class PartitionStrategyTest {

	public static void main(String[] args) {
		PartitionStrategy ps = new PartitionStrategy("year=${timestamp:yyyy}/month=${timestamp:MM}/day=${timestamp:dd}", "yyyy-MM-dd");
		
		String str = "{\"type\": \"record\",\"name\": \"InstrumentationLog\",\"namespace\": \"cops.datalake\",\"fields\": [ { \"name\": \"Id\",\"type\": \"string\"},{\"name\": \"timestamp\",\"type\": \"string\"}]}";
		Schema schema = new Schema.Parser().parse(str);
		Record rec = new Record(schema);
		rec.put("Id", "123");
		rec.put("timestamp", "2016-03-24T13:26:45");
		System.out.println(ps.getPartition(rec));
	}

}
