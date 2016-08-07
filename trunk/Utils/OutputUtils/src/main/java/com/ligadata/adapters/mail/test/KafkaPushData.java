package com.ligadata.adapters.mail.test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;



public class KafkaPushData {
	public static void main(String[] args) throws Exception{
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());

		KafkaProducer<String,String> producer = new KafkaProducer<String,String>(props);
		
		boolean sync = false;
		String topic="mailout_1";
		String key = "mykey";
		
		InputStream is = null;
		BufferedReader reader = null;
		is = StringTemplateTester.class.getResourceAsStream("/mail-message.json");
		reader = new BufferedReader(new InputStreamReader(is));
				
		StringBuilder b = new StringBuilder();
		String line = reader.readLine();
		while(line != null){
			b.append(line);
			line = reader.readLine();
		}
		reader.close();
		
		String value = b.toString();
		
		ProducerRecord<String,String> producerRecord = new ProducerRecord<String,String>(topic, key, value);
		if (sync) {
			producer.send(producerRecord).get();
		} else {
			producer.send(producerRecord);
		}
		producer.close();
	}
}
