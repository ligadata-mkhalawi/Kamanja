package com.ligadata.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ligadata.ZooKeeper.ProcessComponentByWeight;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import sun.misc.Signal;

public class KafkaAdapter implements Observer {
    static Logger logger = LogManager.getLogger(KafkaAdapter.class);

    private AdapterConfiguration configuration;
    private ArrayList<MessageConsumer> consumers;
    private ExecutorService executor;
    AtomicInteger shutdownTriggerCounter = new AtomicInteger(0);

    private boolean prevIsThisNodeToProcess = false;
    private ProcessComponentByWeight pcbw;
    private boolean isLockAcquired = false;

    public KafkaAdapter(AdapterConfiguration config) {
        this.configuration = config;
    }

    @Override
    public void update(Observable o, Object arg) {
        String sig = arg.toString();
        logger.info("Received signal: " + sig);
        if (sig.compareToIgnoreCase("SIGTERM") == 0 || sig.compareToIgnoreCase("SIGINT") == 0
                || sig.compareToIgnoreCase("SIGABRT") == 0) {
            logger.info("Got " + sig + " signal. Shutting down the process");
            shutdownTriggerCounter.incrementAndGet();
//            shutdown();
//            System.exit(0);
        }
    }

    private void PriorityNodeSetup(AdapterConfiguration config) throws Exception {
        logger.info("Setup for node failure events..");
        String zkConnectStr = config.getProperty(AdapterConfiguration.ZOOKEEPER_CONNECT);
        if (zkConnectStr == null) {
            throw new Exception("AdapterConfiguration should have the property " + AdapterConfiguration.ZOOKEEPER_CONNECT);
        }
        if (zkConnectStr.length() == 0) {
            throw new Exception("AdapterConfiguration should have the non null property " + AdapterConfiguration.ZOOKEEPER_CONNECT);
        }
        int zkSessionTimeOut = 30000;
        String zkSessionTimeOutStr = config.getProperty(AdapterConfiguration.ZOOKEEPER_SESSION_TIMEOUT_MS);
        if (zkSessionTimeOutStr != null) {
            zkSessionTimeOut = Integer.parseInt(zkSessionTimeOutStr);
        }
        int zkConnectionTimeOut = 30000;
        String zkConnectionTimeOutStr = config.getProperty(AdapterConfiguration.ZOOKEEPER_CONNECTION_TIMEOUT_MS);
        if (zkConnectionTimeOutStr != null) {
            zkConnectionTimeOut = Integer.parseInt(zkConnectionTimeOutStr);
        }
        String zkLeaderPath = config.getProperty(AdapterConfiguration.ZOOKEEPER_LEADER_PATH);
        if (zkLeaderPath == null) {
            zkLeaderPath = "/kamanja/adapters/leader";
        }
        logger.info("Create an instnce of ProcessComponentByWeight ...");

        String component_name = config.getProperty(AdapterConfiguration.COMPONENT_NAME);
        if (component_name == null) {
            component_name = "JDBCSink";
        }

        String node_id_prefix = config.getProperty(AdapterConfiguration.NODE_ID_PREFIX);
        if (node_id_prefix == null) {
            node_id_prefix = "Node1";
        }

        int adapter_weight = 10;
        String adapter_weight_str = config.getProperty(AdapterConfiguration.ADAPTER_WEIGHT);
        if (adapter_weight_str != null) {
            adapter_weight = Integer.parseInt(adapter_weight_str);
        }

        logger.info("Create an instnce of ProcessComponentByWeight ...");
        try {
            pcbw = new ProcessComponentByWeight(component_name, node_id_prefix, adapter_weight);
        } catch (Throwable e) {
            logger.error("Error : " + e.getMessage() + e);
        }

        logger.info("Call ProcessComponentByWeight.Init..");
        pcbw.Init(zkConnectStr, zkLeaderPath, zkSessionTimeOut, zkConnectionTimeOut);
    }

    private void AcquireLock() throws Exception {
        try {
            if (!isLockAcquired) {
                logger.info("Acquiring the lock ..");
                pcbw.Acquire();
                isLockAcquired = true;
                logger.info("Acquired the lock ..");
            }
        } catch (Exception e) {
            logger.error("Error : " + e.getMessage() + e);
            throw e;
        }
    }

    private void ReleaseLock() {
        if (isLockAcquired) {
            logger.info("Releasing the lock ..");
            if (pcbw != null)
                pcbw.Release();
            pcbw = null;
            isLockAcquired = false;
            logger.info("Released the lock ..");
        }
    }

    public void shutdown(boolean triggerShutdownCntr) {
        for (MessageConsumer c : consumers)
            c.shutdown(triggerShutdownCntr);
        consumers.clear();

        if (executor != null) executor.shutdown();
        try {
            if (!executor.awaitTermination(30000, TimeUnit.MILLISECONDS)) {
                logger.info("Timed out waiting for consumer threads to shut down, exiting uncleanly");
            }
        } catch (InterruptedException e) {
            logger.info("Interrupted during shutdown, exiting uncleanly");
        }
        executor = null;

        logger.info("Shutdown complete.");
    }

    public void run() {
        int numThreads = Integer.parseInt(configuration.getProperty(AdapterConfiguration.COUNSUMER_THREADS, "1"));
        executor = Executors.newFixedThreadPool(numThreads);
        consumers = new ArrayList<MessageConsumer>();
        try {
            for (int threadNumber = 0; threadNumber < numThreads; threadNumber++) {
                MessageConsumer c = new MessageConsumer(configuration, shutdownTriggerCounter);
                executor.submit(c);
                consumers.add(c);
            }
        } catch (Exception e) {
            logger.error("Error: " + e.getMessage(), e);
            shutdownTriggerCounter.incrementAndGet();
//            shutdown();
//            System.exit(1);
        }
    }

    public static class AdapterSignalHandler extends Observable implements sun.misc.SignalHandler {

        @Override
        public void handle(Signal sig) {
            setChanged();
            notifyObservers(sig);
        }

        public void handleSignal(String signalName) {
            sun.misc.Signal.handle(new sun.misc.Signal(signalName), this);
        }
    }

    public static void main(String[] args) {
        AdapterConfiguration config = null;
        try {
            if (args.length == 0)
                config = new AdapterConfiguration();
            else if (args.length == 1)
                config = new AdapterConfiguration(args[0]);
            else {
                logger.error("Incorrect number of arguments. ");
                logger.error("Usage: KafkaAdapter [configfilename]");
                System.exit(1);
            }
        } catch (IOException e) {
            logger.error("Error loading configuration properties.", e);
            System.exit(1);
        }

        KafkaAdapter adapter = null;
        try {
            adapter = new KafkaAdapter(config);
            KafkaAdapter.AdapterSignalHandler sh = new KafkaAdapter.AdapterSignalHandler();
            sh.addObserver(adapter);
            sh.handleSignal("TERM");
            sh.handleSignal("INT");
            sh.handleSignal("ABRT");

            adapter.PriorityNodeSetup(adapter.configuration);
        } catch (Exception e) {
            logger.error("Error starting the adapater.\n", e);
            adapter.shutdownTriggerCounter.incrementAndGet();
            System.exit(1);
        }

        while (adapter.shutdownTriggerCounter.get() == 0) {
            boolean curIsThisNodeToProcess = adapter.pcbw.IsThisNodeToProcess();
            if (curIsThisNodeToProcess) {
                try {
                    if (!adapter.prevIsThisNodeToProcess) { // status flipped from false to true
                        adapter.AcquireLock();
                        adapter.prevIsThisNodeToProcess = curIsThisNodeToProcess;
                        adapter.run();
                    }
                    Thread.sleep(100);
                } catch (Exception e) {
                    logger.info("Main thread is interrupted.\n", e);
                    adapter.shutdownTriggerCounter.incrementAndGet();
                } catch (Throwable t) {
                    logger.info("Main thread is interrupted.\n", t);
                    adapter.shutdownTriggerCounter.incrementAndGet();
                }
            } else {
                try {
                    if (adapter.prevIsThisNodeToProcess) { // status flipped from true to false
                        try {
                            adapter.shutdown(false);
                        } catch (Exception e) {
                            logger.info("Adpater shutdown failed.\n", e);
                        } catch (Throwable t) {
                            logger.info("Adpater shutdown failed.\n", t);
                        }
                        adapter.ReleaseLock();
                        adapter.prevIsThisNodeToProcess = curIsThisNodeToProcess;
                    }
                } catch (Exception e) {
                    logger.info("Main thread is interrupted.\n", e);
                    adapter.shutdownTriggerCounter.incrementAndGet();
                } catch (Throwable t) {
                    logger.info("Main thread is interrupted.\n", t);
                    adapter.shutdownTriggerCounter.incrementAndGet();
                }
            }
        }

        adapter.shutdown(true);
    }
}
