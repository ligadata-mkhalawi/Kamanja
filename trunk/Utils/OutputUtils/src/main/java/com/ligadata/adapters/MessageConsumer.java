package com.ligadata.adapters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ligadata.VelocityMetrics.VelocityMetricsInfo;

public class MessageConsumer implements Runnable {
    // Implement Kafka heart beat to get around session timeout that causes
    // rebalance
    // see issue: https://issues.apache.org/jira/browse/KAFKA-2985
    class SimpleKafkaPoll implements Runnable {
	private AtomicInteger _breaker;
	private KafkaConsumer<String, String> _consumer;

	public SimpleKafkaPoll(KafkaConsumer<String, String> consumer,
		AtomicInteger breaker) {
	    _consumer = consumer;
	    _breaker = breaker;
	}

	public void run() {
	    TopicPartition[] partitions = null;
	    try {
		partitions = _consumer.assignment().toArray(
			new TopicPartition[0]);
		_consumer.pause(partitions);
		try {
		    while (_breaker.get() == 0) {
			try {
			    Thread.sleep(50);
			} catch (Exception e) {
			    //
			} catch (Throwable t) {
			    //
			}
			_consumer.poll(0);
		    }
		} catch (Exception e) {
		    //
		} catch (Throwable t) {
		    //
		}
	    } catch (Exception e) {
		//
	    } catch (Throwable t) {
		//
	    } finally {
		if (partitions != null)
		    _consumer.resume(partitions);
	    }
	}
    }

    static Logger logger = LogManager.getLogger(MessageConsumer.class);
    private volatile boolean stop = false;

    private final AdapterConfiguration configuration;
    private KafkaConsumer<String, String> consumer;
    private BufferedMessageProcessor processor;
    private HashMap<Integer, Long> partitionOffsets = new HashMap<Integer, Long>();
    private Thread thisThread;
    private AtomicInteger shutdownTriggerCounter;
    private StatusCollectable stats;

    public MessageConsumer(AdapterConfiguration config,
	    AtomicInteger shutdownTriggerCounter) throws Exception {
	this.configuration = config;
	this.shutdownTriggerCounter = shutdownTriggerCounter;
	String classname = configuration
		.getProperty(AdapterConfiguration.MESSAGE_PROCESSOR);
	stats = createStatusRecorder(config, classname);
	// create the kafka velocity metrics object here, call init,

	if (classname == null || "".equals(classname)
		|| "null".equalsIgnoreCase(classname)) {
	    logger.info("Message prcessor not specified for processing messages.");
	    processor = new NullProcessor();
	} else {
	    logger.info("Loading class " + classname
		    + " for processing messages.");
	    processor = (BufferedMessageProcessor) Class.forName(classname)
		    .newInstance();
	}
    }

    public void stopBeforeShutdown() {
	stop = true;
    }

    public synchronized void shutdown(boolean triggerShutdownCntr) {
	if (triggerShutdownCntr)
	    shutdownTriggerCounter.incrementAndGet();
	stop = true;

	if (stats != null)
	    stats.close();
	if (processor != null)
	    processor.close();
	processor = null;
	if (consumer != null)
	    consumer.wakeup();

	if (thisThread != null
		&& thisThread.getState() == Thread.State.TIMED_WAITING)
	    thisThread.interrupt();
    }

    public synchronized void close() {
	shutdownTriggerCounter.incrementAndGet();
	if (consumer != null)
	    consumer.close();
	if (processor != null)
	    processor.close();
	// processor = null;
    }

    private StatusCollectable createStatusRecorder(AdapterConfiguration config,
	    String componentName) {
	// What impl do they want to use
	String implName = config.getProperty(AdapterConfiguration.STATUS_IMPL,
		"");
	if (implName.length() > 0) {
	    // Set up Kafka stuff
	    logger.info("Initializing Kafka Status Recorder");
	    try {
		StatusCollectable sc = (StatusCollectable) Class.forName(
			implName).newInstance();
		sc.init(config.getProperty(
			AdapterConfiguration.STATUS_IMPL_INIT_PARMS, ""),
			componentName);
		return sc;
	    } catch (Exception e) {
		logger.warn(
			"Error creating Status Recorder, unable to create status recorder due to ",
			e);
		// Unknown value for Impl.. just return null and
		return null;
	    }
	}

	logger.info("Unkown Status Recorder, desired implementation was not provided, using the dafault log4j");
	// Unknown value for Impl.. just return null and
	return null;
    }

    private void createKafkaConsumer() {
	Properties props = new Properties();

	props.put("bootstrap.servers", configuration
		.getProperty(AdapterConfiguration.BOOTSTRAP_SERVERS));
	props.put("group.id",
		configuration.getProperty(AdapterConfiguration.KAFKA_GROUP_ID));
	props.put("key.deserializer",
		"org.apache.kafka.common.serialization.StringDeserializer");
	props.put("value.deserializer",
		"org.apache.kafka.common.serialization.StringDeserializer");

	// Add any additional properties specified for Kafka
	for (String key : configuration.getProperties().stringPropertyNames()) {
	    if (key.startsWith(AdapterConfiguration.KAFKA_PROPERTY_PREFIX)) {
		logger.debug("Adding kafka configuration: " + key + "="
			+ configuration.getProperty(key));
		props.put(key
			.substring(AdapterConfiguration.KAFKA_PROPERTY_PREFIX
				.length()), configuration.getProperty(key));
	    }
	}

	props.put("enable.auto.commit", "false");

	consumer = new KafkaConsumer<String, String>(props);

	String topic = configuration
		.getProperty(AdapterConfiguration.KAFKA_TOPIC);
	logger.info("Connecting to kafka topic " + topic);
	try {
	    consumer.subscribe(Arrays.asList(topic));
	} catch (Exception e) {

	    logger.error("Error  : " + e.getMessage(), e);
	}
    }

    private void createKafkaConsumerRetry() {
	long retry = 0;
	long retryInterval = 5000;
	while (!stop) {
	    try {
		createKafkaConsumer();
		if (retry > 0)
		    logger.info("Successfully connected after " + retry
			    + " retries.");
		return;
	    } catch (Exception e) {
		retry++;
		logger.error(
			"Error after " + retry + " retries : " + e.getMessage(),
			e);
		try {
		    long tmpMaxRetry = retry;
		    if (tmpMaxRetry > 10000)
			tmpMaxRetry = 10000;
		    long waitInterval = retryInterval * tmpMaxRetry;
		    if (waitInterval > 60000)
			waitInterval = 60000;
		    Thread.sleep(waitInterval);
		} catch (InterruptedException e1) {
		}
	    }
	}

	// return null;
    }

    private void processWithRetry(long batchId) {
	long retry = 0;
	long retryInterval = 5000;
	while (!stop) {
	    try {
		processor.processAll(batchId, retry);
		if (retry > 0)
		    logger.info("Successfully processed messages after "
			    + retry + " retries.");
		return;
	    } catch (Exception e) {
		retry++;
		logger.error(
			"Error after " + retry + " retries : " + e.getMessage(),
			e);
		try {
		    long tmpMaxRetry = retry;
		    if (tmpMaxRetry > 10000)
			tmpMaxRetry = 10000;
		    long waitInterval = retryInterval * tmpMaxRetry;
		    if (waitInterval > 60000)
			waitInterval = 60000;
		    Thread.sleep(waitInterval);
		} catch (InterruptedException e1) {
		}
	    }
	}
    }

    @Override
    public void run() {
	logger.info("Kafka consumer started processing.");
	thisThread = Thread.currentThread();
	java.util.concurrent.atomic.AtomicLong batchid = new java.util.concurrent.atomic.AtomicLong(
		1);
	VelocityMetricsInfo vm = new VelocityMetricsInfo();
	long totalMessageCount = 0;
	long errorMessageCount = 0;
	try {
	    logger.info("Using " + processor.getClass().getName()
		    + " for processing messages.");
	    processor.init(configuration, stats);

	} catch (Exception e) {
	    logger.error("Error initializing processor: " + e.getMessage(), e);
	    if (configuration.VMInstances != null
		    && configuration.VMInstances.length > 0) {
		System.out.println("1 increment "
			+ configuration.VMInstances.length);
		for (int i = 0; i < configuration.VMInstances.length; i++) {
		    if (configuration.VMInstances[i].typId() == 4) {
			System.out.println("2 increment "
				+ configuration.VMInstances.length);
			vm.incrementOutputUtilsVMetricsByKey(
				configuration.VMInstances[i], null,
				configuration.VMInstances[i].KeyStrings(),
				false);
		    }
		}
	    }
	    throw new RuntimeException(e);
	}

	createKafkaConsumerRetry();

	long syncMessageCount = Long.parseLong(configuration.getProperty(
		AdapterConfiguration.SYNC_MESSAGE_COUNT, "10000"));
	long syncInterval = Long.parseLong(configuration.getProperty(
		AdapterConfiguration.SYNC_INTERVAL_SECONDS, "120")) * 1000;
	long pollInterval = Long.parseLong(configuration.getProperty(
		AdapterConfiguration.KAFKA_POLL_INTERVAL, "100"));

	long messageCount = 0;
	long nextSyncTime = System.currentTimeMillis() + syncInterval;
	long start = System.currentTimeMillis();

	while (!stop) {
	    try {
		try {
		    ConsumerRecords<String, String> records = consumer
			    .poll(pollInterval);
		    for (ConsumerRecord<String, String> record : records) {
			Long lastOffset = partitionOffsets.get(record
				.partition());
			if (logger.isDebugEnabled())
			    logger.debug("lastOffset => " + lastOffset
				    + ",partition => " + record.partition());
			if (lastOffset == null || record.offset() > lastOffset) {
			    logger.debug("Message from partition Id :"
				    + record.partition() + " Message: "
				    + record.value());
			    if (processor.addMessage(record.value())) {
				messageCount++;
			    } else {
				errorMessageCount++;
			    }

			    partitionOffsets.put(record.partition(),
				    record.offset());
			}
		    }
		} catch (WakeupException e) {
		    logger.debug("Came out of kafka queue polling...");
		} catch (Exception e) {
		    logger.error("Error reading from kafka: " + e.getMessage(),
			    e);
		    if (configuration.VMInstances != null
			    && configuration.VMInstances.length > 0) {
			System.out.println("1 increment "
				+ configuration.VMInstances.length);
			for (int i = 0; i < configuration.VMInstances.length; i++) {
			    if (configuration.VMInstances[i].typId() == 4) {
				System.out.println("2 increment "
					+ configuration.VMInstances.length);
				vm.incrementOutputUtilsVMetricsByKey(
					configuration.VMInstances[i], null,
					configuration.VMInstances[i]
						.KeyStrings(), false);
			    }
			}
		    }
		    createKafkaConsumerRetry();
		}

		if (!stop
			&& messageCount > 0
			&& (messageCount >= syncMessageCount || System
				.currentTimeMillis() >= nextSyncTime)) {
		    ExecutorService executor = null;
		    try {
			long endRead = System.currentTimeMillis();
			logger.info("Saving " + messageCount
				+ " messages. Read time " + (endRead - start)
				+ " msecs.");

			AtomicInteger breaker = new AtomicInteger(0);
			executor = Executors.newFixedThreadPool(1);

			executor.execute(new SimpleKafkaPoll(consumer, breaker));

			// Save data here
			processWithRetry(batchid.getAndIncrement());

			breaker.incrementAndGet();
			executor.shutdown();
			try {
			    executor.awaitTermination(86400, TimeUnit.SECONDS);
			} catch (InterruptedException e) {

			}
			executor = null;

			if (processor != null)
			    processor.clearAll();
			long endWrite = System.currentTimeMillis();
			if (consumer != null)
			    consumer.commitSync();
			totalMessageCount += messageCount;
			logger.info("Saved " + messageCount
				+ " messages (Total:" + totalMessageCount
				+ "). Write time " + (endWrite - endRead)
				+ " msecs.");

			messageCount = 0;
			nextSyncTime = System.currentTimeMillis()
				+ syncInterval;
			start = System.currentTimeMillis();
		    } catch (Exception e) {
			if (executor != null)
			    executor.shutdownNow();
			executor = null;
			logger.error("Failed with: " + e.getMessage(), e);
			shutdownTriggerCounter.incrementAndGet();
			stop = true;
		    } catch (Throwable t) {
			if (configuration.VMInstances != null
				&& configuration.VMInstances.length > 0) {
			    System.out.println("1 increment "
				    + configuration.VMInstances.length);
			    for (int i = 0; i < configuration.VMInstances.length; i++) {
				if (configuration.VMInstances[i].typId() == 4) {
				    System.out.println("2 increment "
					    + configuration.VMInstances.length);
				    vm.incrementOutputUtilsVMetricsByKey(
					    configuration.VMInstances[i], null,
					    configuration.VMInstances[i]
						    .KeyStrings(), false);
				}
			    }
			}
			if (executor != null)
			    executor.shutdownNow();
			executor = null;
			logger.error("Failed with: " + t.getMessage(), t);
			shutdownTriggerCounter.incrementAndGet();
			stop = true;
		    }
		}
	    } catch (Exception e) {
		logger.error("Unexpected Exception: " + e.getMessage(), e);
		throw new RuntimeException(e);
	    }
	}
	close();
	logger.info("Shutting down after processing " + totalMessageCount
		+ " messages with " + errorMessageCount + " error messages.");
    }
}
