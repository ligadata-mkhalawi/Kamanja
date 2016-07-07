package com.ligadata.kamanja.CheckerComponent;

import java.util.ArrayList;
import java.util.List;

import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.consumer.SimpleConsumer;
import org.apache.log4j.*;
import org.codehaus.jettison.json.JSONObject;
import org.apache.kafka.clients.*;

public class KafkaHelper {
    // KafkaProducer pro = new KafkaProducer();

    String errorMessage = null;
    String status = null;
    private Logger LOG = Logger.getLogger(getClass());

    public void CheckVersion() {
//		System.out.println(kafka.api.OffsetRequest.CurrentVersion());
//		System.out.println(kafka.api.TopicMetadataRequest.CurrentVersion());
    }

    public void GetTopics(String hostslist, java.util.HashMap<String, Object> kafkaMap) {
        String[] hlists = hostslist.split(",");
        boolean gotHosts = false;
        int i = 0;
        Throwable error = null; // Getting one error is enough to send back.
        while (!gotHosts && i < hlists.length) {
            String broker = hlists[i];
            try {
                String[] brokerNameAndPort = broker.split(":");
                String brokerName = brokerNameAndPort[0].trim();
                String portStr = brokerNameAndPort[1].trim();

                java.util.Properties props = new java.util.Properties();
                System.out.println("Kafka Validator, checking on host " + broker);
                org.json.simple.JSONObject kafkaInfo = (org.json.simple.JSONObject) kafkaMap.get(broker);
                java.util.Iterator iter = kafkaInfo.keySet().iterator();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    if (key.equalsIgnoreCase("HostList")) {
                        System.out.println("Add Property  bootstrap.servers=" + kafkaInfo.get(key));
                        props.put("bootstrap.servers", kafkaInfo.get(key));
                    }
                    else {
                        if (!key.equalsIgnoreCase("TopicName")) {
                            props.put(key, kafkaInfo.get(key));
                            System.out.println("Add Property " + key + "=" + kafkaInfo.get(key) );
                        }
                    }
                }
                props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
                props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

                org.apache.kafka.clients.consumer.KafkaConsumer<String, String> consumer = new  org.apache.kafka.clients.consumer.KafkaConsumer<>(props);
                java.util.Map<java.lang.String,java.util.List<org.apache.kafka.common.PartitionInfo>> parts = consumer.listTopics();

                String statusStr = new String();
                java.util.Iterator iter2 = parts.keySet().iterator();
                while (iter2.hasNext()) {
                    String thisTopic = (String) iter2.next();
                    statusStr = statusStr + thisTopic + "\n";
                    java.util.List<org.apache.kafka.common.PartitionInfo> thisParts = parts.get(thisTopic);
                    java.util.Iterator iter3 = thisParts.iterator();
                    while (iter3.hasNext()) {
                        System.out.println(thisPart.toString());
                      }
                }
                gotHosts = true;
            } catch (Exception e) {
                LOG.error("Failed to get partitions from host: " + broker, e);
                error = e;
            } catch (Throwable t) {
                LOG.error("Failed to get partitions from host: " + broker, t);
                error = t;
            }
            i += 1;
        }

        if (error != null) {
            errorMessage = new StringUtility().getStackTrace(error);
            status = "Fail";
        } else {
            status = "Success";
        }
    }

    public void AskKafka(String hostslist, java.util.HashMap<String, Object> kafkaMap) {
        //pro.initialize(host);
        GetTopics(hostslist, kafkaMap);
        //pro.CreateMessage();;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getStatus() {
        return status;
    }
}
