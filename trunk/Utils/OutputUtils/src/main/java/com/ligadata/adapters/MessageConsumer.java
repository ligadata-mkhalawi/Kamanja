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

public class MessageConsumer implements Runnable {
    // Implement Kafka heart beat to get around session timeout that causes rebalance
    // see issue: https://issues.apache.org/jira/browse/KAFKA-2985
    class SimpleKafkaPoll implements Runnable {
        private AtomicInteger _breaker;
        private KafkaConsumer<String, String> _consumer;

        public SimpleKafkaPoll(KafkaConsumer<String, String> consumer, AtomicInteger breaker) {
            _consumer = consumer;
            _breaker = breaker;
        }

        public void run() {
            TopicPartition[] partitions = null;
            try {
                partitions = _consumer.assignment().toArray(new TopicPartition[0]);
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

    public MessageConsumer(AdapterConfiguration config, AtomicInteger shutdownTriggerCounter) throws Exception {
        this.configuration = config;
        this.shutdownTriggerCounter = shutdownTriggerCounter;
        String classname = configuration.getProperty(AdapterConfiguration.MESSAGE_PROCESSOR);
        if (classname == null || "".equals(classname) || "null".equalsIgnoreCase(classname)) {
            logger.info("Message prcessor not specified for processing messages.");
            processor = new NullProcessor();
        } else {
            logger.info("Loading class " + classname + " for processing messages.");
            processor = (BufferedMessageProcessor) Class.forName(classname).newInstance();
        }
    }

    public synchronized void shutdown() {
        shutdownTriggerCounter.incrementAndGet();
        stop = true;

        if (processor != null)
            processor.close();
        processor = null;
        if (consumer != null)
            consumer.wakeup();

        if (thisThread != null && thisThread.getState() == Thread.State.TIMED_WAITING)
            thisThread.interrupt();
    }

    public synchronized void close() {
        shutdownTriggerCounter.incrementAndGet();
        if (consumer != null)
            consumer.close();
        consumer = null;
        if (processor != null)
            processor.close();
        processor = null;
    }

    private void createKafkaConsumer() {
        Properties props = new Properties();

        props.put("bootstrap.servers", configuration.getProperty(AdapterConfiguration.BOOTSTRAP_SERVERS));
        props.put("group.id", configuration.getProperty(AdapterConfiguration.KAFKA_GROUP_ID));
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        // Add any additional properties specified for Kafka
        for (String key : configuration.getProperties().stringPropertyNames()) {
            if (key.startsWith(AdapterConfiguration.KAFKA_PROPERTY_PREFIX)) {
                logger.debug("Adding kafka configuration: " + key + "=" + configuration.getProperty(key));
                props.put(key.substring(AdapterConfiguration.KAFKA_PROPERTY_PREFIX.length()), configuration.getProperty(key));
            }
        }

        props.put("enable.auto.commit", "false");

        consumer = new KafkaConsumer<String, String>(props);

        String topic = configuration.getProperty(AdapterConfiguration.KAFKA_TOPIC);
        logger.info("Connecting to kafka topic " + topic);
        consumer.subscribe(Arrays.asList(topic));
    }

    private void createKafkaConsumerRetry() {
        long retry = 0;
        long retryInterval = 5000;
        while (!stop) {
            try {
                createKafkaConsumer();
                if (retry > 0)
                    logger.info("Successfully connected after " + retry + " retries.");
                return;
            } catch (Exception e) {
                retry++;
                if (retry <= 12) {
                    logger.error("Error after " + retry + " retries : " + e.getMessage(), e);
                    try {
                        Thread.sleep(retryInterval * retry);
                    } catch (InterruptedException e1) {
                    }
                } else
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e1) {
                    }
            }
        }

        //return null;
    }

    private void processWithRetry() {
        long retry = 0;
        long retryInterval = 5000;
        while (!stop) {
            try {
                processor.processAll();
                if (retry > 0)
                    logger.info("Successfully processed messages after " + retry + " retries.");
                return;
            } catch (Exception e) {
                retry++;
                if (retry <= 12) {
                    logger.error("Error after " + retry + " retries : " + e.getMessage(), e);
                    try {
                        Thread.sleep(retryInterval * retry);
                    } catch (InterruptedException e1) {
                    }
                } else
                    try {
                        Thread.sleep(60000);
                    } catch (InterruptedException e1) {
                    }
            }
        }
    }

    @Override
    public void run() {
        logger.info("Kafka consumer started processing.");
        thisThread = Thread.currentThread();

        long totalMessageCount = 0;
        long errorMessageCount = 0;
        try {
            logger.info("Using " + processor.getClass().getName() + " for processing messages.");
            processor.init(configuration);

        } catch (Exception e) {
            logger.error("Error initializing processor: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }

        createKafkaConsumerRetry();

        long syncMessageCount = Long.parseLong(configuration.getProperty(AdapterConfiguration.SYNC_MESSAGE_COUNT, "10000"));
        long syncInterval = Long.parseLong(configuration.getProperty(AdapterConfiguration.SYNC_INTERVAL_SECONDS, "120")) * 1000;
        long pollInterval = Long.parseLong(configuration.getProperty(AdapterConfiguration.KAFKA_POLL_INTERVAL, "100"));

        long messageCount = 0;
        long nextSyncTime = System.currentTimeMillis() + syncInterval;
        long start = System.currentTimeMillis();

        while (!stop) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(pollInterval);
                for (ConsumerRecord<String, String> record : records) {
                    Long lastOffset = partitionOffsets.get(record.partition());
                    if (lastOffset == null || record.offset() > lastOffset) {
                        logger.debug("Message from partition Id :" + record.partition() + " Message: " + record.value());
                        if (processor.addMessage(record.value()))
                            messageCount++;
                        else
                            errorMessageCount++;

                        partitionOffsets.put(record.partition(), record.offset());
                    }
                }
            } catch (WakeupException e) {
            } catch (Exception e) {
                logger.error("Error reading from kafka: " + e.getMessage(), e);
                createKafkaConsumerRetry();
            }

            if (messageCount > 0 && (messageCount >= syncMessageCount || System.currentTimeMillis() >= nextSyncTime)) {
                try {
                    long endRead = System.currentTimeMillis();
                    logger.info("Saving " + messageCount + " messages. Read time " + (endRead - start) + " msecs.");

                    AtomicInteger breaker = new AtomicInteger(0);
                    ExecutorService executor = Executors.newFixedThreadPool(1);

                    executor.execute(new SimpleKafkaPoll(consumer, breaker));

                    // Save data here
                    processWithRetry();

                    breaker.incrementAndGet();
                    executor.shutdown();
                    try {
                        executor.awaitTermination(86400, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                    }

                    processor.clearAll();
                    long endWrite = System.currentTimeMillis();
                    consumer.commitSync();
                    totalMessageCount += messageCount;
                    logger.info("Saved " + messageCount + " messages. Write time " + (endWrite - endRead) + " msecs.");

                    messageCount = 0;
                    nextSyncTime = System.currentTimeMillis() + syncInterval;
                    start = System.currentTimeMillis();
                } catch (Exception e) {
                    logger.error("Failed with: " + e.getMessage(), e);
                    stop = true;
                } catch (Throwable t) {
                    logger.error("Failed with: " + t.getMessage(), t);
                    stop = true;
                }
            }
        }

        close();

        logger.info("Shutting down after processing " + totalMessageCount + " messages with " + errorMessageCount
                + " error messages.");
    }
}


