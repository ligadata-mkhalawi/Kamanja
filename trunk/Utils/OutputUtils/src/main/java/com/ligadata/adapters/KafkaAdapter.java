package com.ligadata.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import sun.misc.Signal;

public class KafkaAdapter implements Observer {
    static Logger logger = LogManager.getLogger(KafkaAdapter.class);

    private AdapterConfiguration configuration;
    private ArrayList<MessageConsumer> consumers;
    private ExecutorService executor;
    AtomicInteger shutdownTriggerCounter = new AtomicInteger(0);

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

    public void shutdown() {
        for (MessageConsumer c : consumers)
            c.shutdown();

        if (executor != null) executor.shutdown();
        try {
            if (!executor.awaitTermination(30000, TimeUnit.MILLISECONDS)) {
                logger.info("Timed out waiting for consumer threads to shut down, exiting uncleanly");
            }
        } catch (InterruptedException e) {
            logger.info("Interrupted during shutdown, exiting uncleanly");
        }

        logger.info("Shutdown complete.");
    }

    public void run() {
        int numThreads = Integer.parseInt(configuration.getProperty(AdapterConfiguration.COUNSUMER_THREADS, "2"));
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

            adapter.run();
        } catch (Exception e) {
            logger.error("Error starting the adapater.\n", e);
            adapter.shutdownTriggerCounter.incrementAndGet();
            System.exit(1);
        }

        while (adapter.shutdownTriggerCounter.get() == 0) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                logger.info("Main thread is interrupted.\n", e);
                adapter.shutdownTriggerCounter.incrementAndGet();
            } catch (Throwable t) {
                logger.info("Main thread is interrupted.\n", t);
                adapter.shutdownTriggerCounter.incrementAndGet();
            }
        }

        adapter.shutdown();
    }
}
