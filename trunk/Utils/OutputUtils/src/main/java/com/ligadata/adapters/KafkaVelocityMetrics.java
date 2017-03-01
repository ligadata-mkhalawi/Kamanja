package com.ligadata.adapters;

import java.util.Properties;
import java.util.concurrent.Future;

import com.ligadata.VelocityMetrics.VelocityMetricsCallback;
import com.ligadata.VelocityMetrics.Metrics;
import com.ligadata.VelocityMetrics.ComponentMetrics;
import com.ligadata.VelocityMetrics.ComponentKeyMetrics;
import com.ligadata.VelocityMetrics.MetricValue;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.json.*;

public class KafkaVelocityMetrics implements VelocityMetricsCallback {

    private Properties props = null;
    private KafkaProducer<String, String> producer = null;
    private String velocitymetricsTopic = null;
    // private JSONObject currentStatus = new JSONObject();
    private String componentName = "unknown";
    private java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
	    "yyyy-MM-dd HH:mm:ss.SSS z");

    static Logger logger = LogManager.getLogger(KafkaVelocityMetrics.class);

    /**
     * Create an instance of KafkaStatsRecorder
     */
    public KafkaVelocityMetrics() {
	props = new Properties();
    }

    public void call(Metrics metrics) {
	logger.info("metrics " + metrics.metricsGeneratedTimeInMs);
	ComponentMetrics[] compMetrics = metrics.compMetrics;

	logger.info("compMetrics length  " + compMetrics.length);

	if (compMetrics.length > 0) {
	    javax.json.JsonArrayBuilder componentArr = Json
		    .createArrayBuilder();
	    javax.json.JsonArrayBuilder componentValsArr = Json
		    .createArrayBuilder();
	    JsonObjectBuilder componentObj = Json.createObjectBuilder();
	    for (int i = 0; i < compMetrics.length; i++) {
		logger.info("compMetrics componentKey  "
			+ compMetrics[i].componentKey);
		logger.info("compMetrics nodeId " + compMetrics[i].nodeId);
		javax.json.JsonArrayBuilder componentMetricsArr = Json
			.createArrayBuilder();
		ComponentKeyMetrics[] componentMetrics = compMetrics[i].keyMetrics;
		logger.info("componentMetrics " + componentMetrics.length);
		for (int j = 0; j < componentMetrics.length; j++) {
		    JsonObjectBuilder componentJsonObj = Json
			    .createObjectBuilder();
		    logger.info("componentMetrics "
			    + componentMetrics[j].firstOccured);
		    logger.info("componentMetrics metrics time "
			    + componentMetrics[j].metricsTime);
		    logger.info("componentMetrics key "
			    + componentMetrics[j].key);
		    logger.info("componentMetrics "
			    + componentMetrics[j].lastOccured);
		    MetricValue[] metricValues = componentMetrics[j].metricValues;
		    logger.info("metricValues " + metricValues.length);
		    javax.json.JsonArrayBuilder metricsValsArr = Json
			    .createArrayBuilder();
		    for (int k = 0; k < metricValues.length; k++) {
			JsonObjectBuilder metricsValueJsonObj = Json
				.createObjectBuilder();
			metricsValueJsonObj.add("metrickey",
				metricValues[k].Key());
			metricsValueJsonObj.add("metricskeyvalue",
				metricValues[k].Value());
			metricsValsArr.add(metricsValueJsonObj);
			logger.info("metricValues Key " + metricValues[k].Key());
			logger.info("metricValues value "
				+ metricValues[k].Value());
		    }
		    JsonArray marr = metricsValsArr.build();
		    componentJsonObj.add("key", componentMetrics[j].key);
		    componentJsonObj.add("metricstime",
			    componentMetrics[j].metricsTime);
		    componentJsonObj.add("roundintervaltimeinsec",
			    componentMetrics[j].roundIntervalTimeInSec);
		    componentJsonObj.add("firstoccured",
			    componentMetrics[j].firstOccured);
		    componentJsonObj.add("lastoccured",
			    componentMetrics[j].lastOccured);
		    componentJsonObj.add("key", componentMetrics[j].key);
		    componentJsonObj.add("metricsvalue", marr);
		    componentValsArr.add(componentJsonObj);
		}
		JsonArray componentMetricsarr = componentValsArr.build();
		componentObj.add("componentkey", compMetrics[i].componentKey);
		componentObj.add("nodeid", compMetrics[i].nodeId);
		componentObj.add("componentmetrics", componentMetricsarr);
		componentArr.add(componentObj);
	    }
	    JsonArray componentsarr = componentValsArr.build();
	    JsonObjectBuilder metricObj = Json.createObjectBuilder();
	    metricObj.add("uuid", metrics.uuid);
	    metricObj.add("metricsgeneratedtime",
		    metrics.metricsGeneratedTimeInMs);
	    metricObj.add("Metrics", componentArr.build());
	    JsonObject json = metricObj.build();
	    logger.info("json: " + json);

	    // TODO: Do we need to handle the failure differently? Will we have
	    // a problem if a lot of addStatus Calls are made
	    // and then time out?
	    Future f = producer.send(new ProducerRecord<String, String>(
		    velocitymetricsTopic, json.toString()), new Callback() {
		public void onCompletion(RecordMetadata metadata, Exception e) {
		    if (e != null)
			logger.warn("Unable to sent status message to kafka", e);
		    else {
			logger.debug("The offset of the record we just sent is: "
				+ metadata.offset());
		    }
		}
	    });

	}

    }

    /**
     * Initialize the kafka stats recorder
     * 
     * @param topic kafka topic name
     * @param destinationComponentName type of component
	 * @param bootstrapserver kafka servers
     * @throws Exception an exception is thrown
     */
    public void init(String topic, String bootstrapserver,
	    String destinationComponentName) throws Exception {
	try {
	    logger.info("Initializing Velocity Metrics KafkaStatsRecorder"
		    + topic);
	    if (topic == null || topic.trim().length() == 0)
		throw new Exception(
			"Topic for Velocity Metrics Stats is not provided in properties file");
	    if (destinationComponentName != null
		    && destinationComponentName.trim().length() != 0)
		componentName = destinationComponentName;
	    velocitymetricsTopic = topic;

	    // Initialize producer properties - these 3 are required:
	    // TODO: Hardcode serializers now
	    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapserver);
	    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
		    "org.apache.kafka.common.serialization.StringSerializer");
	    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
		    "org.apache.kafka.common.serialization.StringSerializer");
	} catch (Throwable t) {
	    logger.warn("Exception during status collection ", t);
	}

	// Verify Topic
	if (velocitymetricsTopic.length() == 0) {
	    logger.warn("Valid topic name is required for KafkaStatusReporter");
	    return;
	}

	// Initialize the producer
	try {
	    producer = new KafkaProducer<String, String>(props);
	} catch (Exception e) {
	    // If something went wrong, assume no status recorder provided and
	    // go forth.
	    // TODO: Maybe need to do some retries here???
	    logger.info("Unable to create Status Producer", e);
	    throw e;
	}
    }

    /**
     * clean up all kafka resources
     */
    public void close() {
	try {
	    logger.info("Sutting down KafkaStatsRecorder");
	    producer.close();
	} catch (Throwable t) {
	    logger.warn("Exception during status collection ", t);
	}
    }

    private String getOrElse(java.util.HashMap map, String key,
	    String defaultValue) {
	String value = (String) map.get(key);
	if (value == null)
	    return defaultValue;
	else
	    return value;
    }

}
