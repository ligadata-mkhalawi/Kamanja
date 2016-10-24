package com.ligadata.adapters.statusRecorders;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.StatusCollectable;
import java.util.Properties;
import java.util.concurrent.Future;


/**
 * Provides implementation of functionality to externalize status messages to Kafka.
 *
 * After the class is instantiated, init method must be called and provided an com.ligadata.adapters.AdapterConfiguration
 * object must be passed.  For now, we define a minimum set of values that this object must posses
 *
 * Example (a config file will have the following values):
 * #Status Reporter
 * status.impl=com.ligadata.adapters.statRecorders.KafkaStatusRecorder
 * status.bootstrap.servers=localhost:9092
 * status.kafka.topic=statusTopic
 *
 *  @author dankozin
 */
public class KafkaStatusRecorder implements StatusCollectable {

    private Properties props = null;
    private KafkaProducer<String, String> producer = null;
    private String statusTopic = null;
    private java.util.HashMap<String, String> currentStatus = new java.util.HashMap<String, String>();
    private String componentName = "unknown";
    private java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");

    static Logger logger = LogManager.getLogger(KafkaStatusRecorder.class);

    /**
     * Create an instance of KafkaStatsRecorder
     * @throws Exception
     */
    public KafkaStatusRecorder() {
        props = new Properties();
    }

    /**
     * Record the given message in the Kafka storage.
     * @param message
     * @return
     * @throws Exception
     */
    public boolean externalizeStatusMessage(String batchId, String sourceOfStatus) {
        logger.debug("Externalizing status from " + sourceOfStatus + " for batchId " + batchId);


        // Test Test Test
        java.util.HashMap<String,String> newMessage = new java.util.HashMap<String,String>();
        newMessage.put("ComponentName",componentName);
        newMessage.put("BatchId",batchId);
        newMessage.put("TimeStamp", sdf.format(new java.util.Date()));
        newMessage.put("Status",JSONObject.toJSONString(currentStatus));
        String statusOutput = JSONObject.toJSONString(newMessage);
        logger.debug("message=" + statusOutput);

        // TODO: Do we need to handle the failure differently? Will we have a problem if a lot of addStatus Calls are made
        // and then time out?
        Future f = producer.send(new ProducerRecord<String, String>(statusTopic, "PKey_"+sourceOfStatus, statusOutput),
                                 new Callback () {
                                     public void onCompletion(RecordMetadata metadata, Exception e) {
                                         if(e != null)
                                             logger.warn("Unable to sent status message to kafka", e);
                                         else {
                                             logger.debug("The offset of the record we just sent is: " + metadata.offset());
                                         }
                                     }
                                 });
        // TODO: for now we do not retry failed sends.  May need to introduce retry logic
        currentStatus.clear();
        return true;
    }

    /**
     *  Initialize the kafka stats recorder
     * @param config - values to initialize the underlying Kafka Producer
     * @throws Exception
     */
    public void init (String config, String destinationComponentName) throws Exception {
        logger.info ("Initializing KafkaStatsRecorder");
        componentName = destinationComponentName;

        JSONParser jsonParser = new JSONParser();
        JSONObject parms = (JSONObject) jsonParser.parse(config);

        logger.debug("Initializing status recorder with property bootstrap.servers = " + getOrElse(parms, "bootsrap.servers", ""));
        logger.debug("Initializing status recorder with property kafka.topic = " + getOrElse(parms, "kafka.topic", ""));


        java.util.Iterator it = parms.entrySet().iterator();
        while(it.hasNext()) {
            java.util.Map.Entry<String, String> pair =  (java.util.Map.Entry<String, String>) it.next();
            // Handle Special case for Status Topic
            if (pair.getKey().trim().equalsIgnoreCase("kafka.topic")) {
                statusTopic =  pair.getValue();
            } else {
                props.put(pair.getKey(), pair.getValue());
            }
        }

        // Initialize producer properties - these 3 are required:
        // TODO: Hardcode serializers now
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getOrElse(parms, ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ""));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        // Verify Topic
        if (statusTopic.length() == 0) {
            throw new Exception("Valid topic name is required for KafkaStatusReporter");
        }

        // Initialize the producer
        try {
            producer = new KafkaProducer<String,String>(props);
        } catch (Exception e) {
            // If something went wrong, assume no status recorder provided and go forth.
            // TODO:  Maybe need to do some retries here???
            logger.info("Unable to create Status Producer",e);
            throw e;
        }
    }

    /**
     * clean up all kafka resources
     */
    public void close() {
        logger.info ("Sutting down KafkaStatsRecorder");
        producer.close();
    }

    /**
     * Add the status message to the underlying status sturcutre. the whole thing will be externalized when
     * externalizeStatusMessage(String message, String sourceOfStatus) is called
     * @param key
     * @param value
     */
    public void addStatus(String key, String value) {
        currentStatus.put(key, value);
    }


    private String getOrElse(java.util.HashMap map, String key, String defaultValue)  {
        String value = (String) map.get(key);
        if (value == null)
            return defaultValue;
        else
            return value;
    }

}