package com.ligadata.adapters.statusRecorders;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import javax.json.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.StatusCollectable;
import java.util.Properties;
import java.util.ArrayList;
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
    private int completionStatus = -1;
    private long maxMessageSize = (1024 * 32) - 1024;
    private long msgAreaInUse = 0;
    private boolean isTruncated = false;

    private java.util.HashMap<String, String> currentStatus = new java.util.HashMap<String, String>();
    private java.util.HashMap<String, String> currentFailedStatus = new java.util.HashMap<String, String>();
    private java.util.HashMap<String, ArrayList<String>> currentStatusMsg = new java.util.HashMap<String, ArrayList<String>>();
    private java.util.HashMap<String, String> currentStatusCompletionCode = new java.util.HashMap<String, String>();

   // private JSONObject currentStatus = new JSONObject();
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
    public boolean externalizeStatusMessage(long batchId, long retryNumber, String sourceOfStatus) {
        try {
            logger.debug("Externalizing status from " + sourceOfStatus + " for batchId " + batchId);

            JsonObjectBuilder status_obldr =  Json.createObjectBuilder();

            java.util.Iterator statusIter = currentStatus.entrySet().iterator();
            while(statusIter.hasNext()) {
                java.util.Map.Entry<String, String> pair =  (java.util.Map.Entry<String, String>) statusIter.next();
                logger.info ("Processing key " + pair.getKey());

                JsonObjectBuilder subStatus_obldr =  Json.createObjectBuilder();
                // Create Messages
                javax.json.JsonArrayBuilder abldr = Json.createArrayBuilder();
                // If we have any status for this key to report.. the message List is not null
                logger.info("Building Status Message, msgSetSize for " + pair.getKey() +" is " + currentStatusMsg.get(pair.getKey()));
                java.util.Iterator<String> msgIter = currentStatusMsg.get(pair.getKey()).iterator();
                while(msgIter.hasNext()) {
                    abldr = abldr.add(msgIter.next());
                }
                JsonArray tarray = abldr.build();

                JsonObject subStatus = subStatus_obldr.add("Code", currentStatusCompletionCode.get(pair.getKey()))
                        .add("BatchSize",  Integer.parseInt(currentFailedStatus.get(pair.getKey())) +   Integer.parseInt(currentStatus.get(pair.getKey())))
                        .add("Succeded", Integer.parseInt(currentStatus.get(pair.getKey())))
                        .add("Failed", Integer.parseInt(currentFailedStatus.get(pair.getKey())))
                        .add("Messages", tarray).build();


                status_obldr = status_obldr.add(pair.getKey(),subStatus);
            }

            JsonObject finalStatus = status_obldr.build();

            JsonObject msg = Json.createObjectBuilder()
                    .add("ComponentName",componentName).add("BatchId",batchId).add("AttemptNumber",(retryNumber + 1)).add("TimeStamp", sdf.format(new java.util.Date()))
                    .add("Status",finalStatus).build();


            // TODO: Do we need to handle the failure differently? Will we have a problem if a lot of addStatus Calls are made
            // and then time out?
            Future f = producer.send(new ProducerRecord<String, String>(statusTopic, "PKey_"+sourceOfStatus, msg.toString()),
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
            currentFailedStatus.clear();
            currentStatusMsg.clear();
            currentStatusCompletionCode.clear();
            msgAreaInUse = 0;
            isTruncated = false;
            return true;
        } catch (Throwable t) {
            logger.warn("Exception during status collection ",t);
            return false;
        }
    }

    /**
     *  Initialize the kafka stats recorder
     * @param config - values to initialize the underlying Kafka Producer
     * @throws Exception
     */
    public void init (String config, String destinationComponentName) throws Exception {
        try {
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
                }
                else if ( (pair.getKey().trim().equalsIgnoreCase("kafka.maxMessageAreaBytes")) ) {
                    maxMessageSize = java.lang.Math.max(Long.parseLong(pair.getValue()) - 1024, 0);
                    logger.info("Total usable area for optional messages is " + maxMessageSize);
                }
                else if ((pair.getKey().trim().equalsIgnoreCase("kafka.displayComponentName")) ) {
                    componentName = pair.getValue();
                }
                else {
                    props.put(pair.getKey(), pair.getValue());
                }
            }

            // Initialize producer properties - these 3 are required:
            // TODO: Hardcode serializers now
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getOrElse(parms, ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ""));
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        } catch (Throwable t) {
            logger.warn("Exception during status collection ",t);
        }

        // Verify Topic
        if (statusTopic.length() == 0) {
            logger.warn("Valid topic name is required for KafkaStatusReporter");
            return;
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
        try {
            logger.info ("Sutting down KafkaStatsRecorder");
            producer.close();
        } catch (Throwable t) {
            logger.warn("Exception during status collection ",t);
        }
    }

    /**
     * Add the status message to the underlying status sturcutre. the whole thing will be externalized when
     * externalizeStatusMessage(String message, String sourceOfStatus) is called
     * @param key
     * @param value
     */
    public void addStatus(String key, String successValue, String failedValue) {
        try {
            // Make sure the completion code is initialized to true
            if (currentStatusCompletionCode.get(key) == null)
                currentStatusCompletionCode.put(key, "0");

            // Make sure the messages are initialized
            if(currentStatusMsg.get(key) == null)
                currentStatusMsg.put(key, new ArrayList<String>());

            currentStatus.put(key, successValue);
            currentFailedStatus.put(key, failedValue);
        } catch (Throwable t) {
            logger.warn("Exception during status collection ",t);
        }
    }

    /**
     * Add a message to appear under the Messages[] array in the Status message
     * @param msg
     */
    public void addStatusMessage(String key, String msg, boolean isRequired) {
        try {
            // Make sure the messages are initialized
            logger.debug("Adding message for " + key + " message: " + msg );
            if(currentStatusMsg.get(key) == null)
                currentStatusMsg.put(key, new ArrayList<String>());

            // TODO: Limit the size here.
            if (!isRequired) {
                long msgSize = msg.getBytes().length;
                if ((msgAreaInUse + msgSize) > maxMessageSize) {
                    if (!isTruncated) {
                        currentStatusMsg.get(key).add("error messages trucated... see logs");
                    }
                    isTruncated = true;
                    return;
                }
                msgAreaInUse = msgAreaInUse + msgSize;
            }
            currentStatusMsg.get(key).add(msg);
        } catch (Throwable t) {
            logger.warn("Exception during status collection ",t);
        }
    }

    /**
     * Set the completion code for this batch
     * @param status
     */
    public void setCompletionCode(String key, String status) {
        try {
            // Dont care if it exists.. set to whatever the user wants
            currentStatusCompletionCode.put(key, status);
        } catch (Throwable t) {
            logger.warn("Exception during status collection ",t);
        }
    }


    private String getOrElse(java.util.HashMap map, String key, String defaultValue)  {
        String value = (String) map.get(key);
        if (value == null)
            return defaultValue;
        else
            return value;
    }

}