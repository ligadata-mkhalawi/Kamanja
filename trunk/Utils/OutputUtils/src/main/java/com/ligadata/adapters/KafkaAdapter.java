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

import com.ligadata.VelocityMetrics.*;

import sun.misc.Signal;

public class KafkaAdapter implements Observer {
    static Logger logger = LogManager.getLogger(KafkaAdapter.class);

    private AdapterConfiguration configuration;
    private ArrayList<MessageConsumer> consumers;
    private ExecutorService executor;
    AtomicInteger shutdownTriggerCounter = new AtomicInteger(0);
    boolean gotSignalToShutdown = false;

    private boolean prevIsThisNodeToProcess = false;
    private ProcessComponentByWeight pcbw;
    private boolean isLockAcquired = false;
    private KafkaVelocityMetrics velocitymetricsStats;
    private VelocityMetricsFactoryInterface VMFactory = null;

    public KafkaAdapter(AdapterConfiguration config) {
	this.configuration = config;
	velocitymetricsStats = createVelocityMetricsStats(config);
	VMFactory = getVMFactory(config);
	config.VMInstances = getVelocityMetricsInstances(config);
    }

    @Override
    public void update(Observable o, Object arg) {
	String sig = arg.toString();
	logger.info("Received signal: " + sig);
	if (sig.compareToIgnoreCase("SIGTERM") == 0
		|| sig.compareToIgnoreCase("SIGINT") == 0
		|| sig.compareToIgnoreCase("SIGABRT") == 0) {
	    logger.info("Got " + sig + " signal. Shutting down the process");
	    shutdownTriggerCounter.incrementAndGet();
	    gotSignalToShutdown = true;
	    // shutdown();
	    // System.exit(0);
	}
    }

    private void PriorityNodeSetup(AdapterConfiguration config)
	    throws Exception {
	logger.info("Setup for node failure events..");
	String zkConnectStr = config
		.getProperty(AdapterConfiguration.ZOOKEEPER_CONNECT);
	if (zkConnectStr == null) {
	    throw new Exception(
		    "AdapterConfiguration should have the property "
			    + AdapterConfiguration.ZOOKEEPER_CONNECT);
	}
	if (zkConnectStr.length() == 0) {
	    throw new Exception(
		    "AdapterConfiguration should have the non null property "
			    + AdapterConfiguration.ZOOKEEPER_CONNECT);
	}
	int zkSessionTimeOut = 30000;
	String zkSessionTimeOutStr = config
		.getProperty(AdapterConfiguration.ZOOKEEPER_SESSION_TIMEOUT_MS);
	if (zkSessionTimeOutStr != null) {
	    zkSessionTimeOut = Integer.parseInt(zkSessionTimeOutStr);
	}
	int zkConnectionTimeOut = 30000;
	String zkConnectionTimeOutStr = config
		.getProperty(AdapterConfiguration.ZOOKEEPER_CONNECTION_TIMEOUT_MS);
	if (zkConnectionTimeOutStr != null) {
	    zkConnectionTimeOut = Integer.parseInt(zkConnectionTimeOutStr);
	}
	String zkLeaderPath = config
		.getProperty(AdapterConfiguration.ZOOKEEPER_LEADER_PATH);
	if (zkLeaderPath == null) {
	    zkLeaderPath = "/kamanja/adapters/leader";
	}
	logger.info("Create an instnce of ProcessComponentByWeight ...");

	String component_name = config
		.getProperty(AdapterConfiguration.COMPONENT_NAME);
	if (component_name == null) {
	    component_name = "JDBCSink";
	}

	String node_id_prefix = config
		.getProperty(AdapterConfiguration.NODE_ID_PREFIX);
	if (node_id_prefix == null) {
	    node_id_prefix = "Node1";
	}

	int adapter_weight = 10;
	String adapter_weight_str = config
		.getProperty(AdapterConfiguration.ADAPTER_WEIGHT);
	if (adapter_weight_str != null) {
	    adapter_weight = Integer.parseInt(adapter_weight_str);
	}

	logger.info("Create an instnce of ProcessComponentByWeight ...");
	try {
	    pcbw = new ProcessComponentByWeight(component_name, node_id_prefix,
		    adapter_weight);
	} catch (Throwable e) {
	    logger.error("Error : " + e.getMessage() + e);
	}

	logger.info("Call ProcessComponentByWeight.Init..");
	pcbw.Init(zkConnectStr, zkLeaderPath, zkSessionTimeOut,
		zkConnectionTimeOut);
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
	    isLockAcquired = false;
	    logger.info("Released the lock ..");
	}
    }

    public void stopBeforeShutdown() {
	for (MessageConsumer c : consumers)
	    c.stopBeforeShutdown();
    }

    public void shutdown(boolean triggerShutdownCntr) {
	for (MessageConsumer c : consumers)
	    c.shutdown(triggerShutdownCntr);
	consumers.clear();

	ExecutorService localExecutor = executor;
	executor = null;

	if (localExecutor != null) {
	    localExecutor.shutdown();
	    try {
		if (!localExecutor.awaitTermination(30000,
			TimeUnit.MILLISECONDS)) {
		    logger.info("Timed out waiting for consumer threads to shut down, exiting uncleanly");
		}
	    } catch (InterruptedException e) {
		logger.info("Interrupted during shutdown, exiting uncleanly");
	    }
	}

	logger.info("Shutdown complete.");
    }

    public void run() {
	int numThreads = Integer.parseInt(configuration.getProperty(
		AdapterConfiguration.COUNSUMER_THREADS, "1"));
	executor = Executors.newFixedThreadPool(numThreads);
	consumers = new ArrayList<MessageConsumer>();
	try {
	    for (int threadNumber = 0; threadNumber < numThreads; threadNumber++) {
		MessageConsumer c = new MessageConsumer(configuration,
			shutdownTriggerCounter);
		executor.submit(c);
		consumers.add(c);
	    }
	} catch (Exception e) {
	    logger.error("Error: " + e.getMessage(), e);
	    shutdownTriggerCounter.incrementAndGet();
	    // shutdown();
	    // System.exit(1);
	}
    }

    public static class AdapterSignalHandler extends Observable implements
	    sun.misc.SignalHandler {

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

	while (adapter.shutdownTriggerCounter.get() == 0
		&& !adapter.gotSignalToShutdown) {
	    boolean curIsThisNodeToProcess = adapter.pcbw.IsThisNodeToProcess();
	    if (curIsThisNodeToProcess) {
		try {
		    if (!adapter.prevIsThisNodeToProcess) { // status flipped
							    // from false to
							    // true
			adapter.AcquireLock();
			adapter.prevIsThisNodeToProcess = curIsThisNodeToProcess;
			adapter.run();
		    }
		    Thread.sleep(1000);
		} catch (Exception e) {
		    logger.info("Main thread is interrupted.\n", e);
		    adapter.shutdownTriggerCounter.incrementAndGet();
		} catch (Throwable t) {
		    logger.info("Main thread is interrupted.\n", t);
		    adapter.shutdownTriggerCounter.incrementAndGet();
		}
	    } else {
		try {
		    if (adapter.prevIsThisNodeToProcess) { // status flipped
							   // from true to
							   // false
			// If adapter.shutdownTriggerCounter == 0 at this
			// moment, just reset the counter at the end
			boolean canResetCounter = (adapter.shutdownTriggerCounter
				.get() == 0);
			logger.info("Stopping before shutdown for switch to another node.");
			adapter.stopBeforeShutdown();
			logger.info("Sleeping 15000 ms before shutdown for switch to another node.");
			try {
			    Thread.sleep(15000);
			} catch (Exception e) {
			    logger.info("Adpater shutdown failed.\n", e);
			} catch (Throwable t) {
			    logger.info("Adpater shutdown failed.\n", t);
			}
			try {
			    adapter.shutdown(false);
			} catch (Exception e) {
			    logger.info("Adpater shutdown failed.\n", e);
			} catch (Throwable t) {
			    logger.info("Adpater shutdown failed.\n", t);
			}
			adapter.ReleaseLock();
			adapter.prevIsThisNodeToProcess = curIsThisNodeToProcess;
			if (canResetCounter
				&& adapter.shutdownTriggerCounter.get() > 0) {
			    adapter.shutdownTriggerCounter.addAndGet(-1
				    * adapter.shutdownTriggerCounter.get());
			}
		    }
		    Thread.sleep(1000);
		} catch (Exception e) {
		    logger.info("Main thread is interrupted.\n", e);
		    adapter.shutdownTriggerCounter.incrementAndGet();
		} catch (Throwable t) {
		    logger.info("Main thread is interrupted.\n", t);
		    adapter.shutdownTriggerCounter.incrementAndGet();
		}
	    }
	    adapter.VMFactory.addEmitListener(adapter.velocitymetricsStats);
	}
	adapter.VMFactory.shutdown();
	adapter.shutdown(true);
    }

    private VelocityMetricsFactoryInterface getVMFactory(
	    AdapterConfiguration config) {

	Integer rotationTimeInSecs = 30;
	Integer emitTimeInSecs = 15;

	VelocityMetricsFactoryInterface existingFactory = VelocityMetrics
		.GetExistingVelocityMetricsFactory();
	if (existingFactory != null)
	    return existingFactory;

	return VelocityMetrics.GetVelocityMetricsFactory(rotationTimeInSecs,
		emitTimeInSecs); // (rotationTimeInSecs, emitTimeInSecs)
    }

    private KafkaVelocityMetrics createVelocityMetricsStats(
	    AdapterConfiguration config) {
	// What impl do they want to use
	KafkaVelocityMetrics k_vm = new KafkaVelocityMetrics();
	try {
	    String classname = configuration
		    .getProperty(AdapterConfiguration.MESSAGE_PROCESSOR);

	    String velocitymetricskafkatopic = config.getProperty(
		    AdapterConfiguration.VELOCITYMETRICS_KAFKA_TOPIC, "");

	    String bootstrapserver = config.getProperty(
		    AdapterConfiguration.BOOTSTRAP_SERVERS, "");

	    if (classname == null || classname.trim().length() == 0)
		throw new Exception(
			"Topic for Velocity Metrics Stats is not provided in properties file");
	    k_vm.init(velocitymetricskafkatopic, bootstrapserver, classname);

	    logger.info("Initializing Velocity Metrics Kafka Recorder");
	} catch (Exception e) {
	    logger.error(e.getMessage());
	}
	return k_vm;
    }

    private InstanceRuntimeInfo[] getVelocityMetricsInstances(
	    AdapterConfiguration config) {
	VelocityMetricsInfo vm = new VelocityMetricsInfo();
	try {
	    String vmCategory = configuration.getProperty(
		    AdapterConfiguration.VM_CATEGORY, "outpututils");
	    String vmComponentName = configuration.getProperty(
		    AdapterConfiguration.VM_COMPONENT_NAME, "");

	    String vmFullConfig = configuration.getProperty(
		    AdapterConfiguration.VM_CONFIG, "");
	    if (vmFullConfig == null || vmFullConfig.trim().length() == 0)
		throw new Exception(
			"velocity metrics config info should be given in properties file");
	    String nodeId = configuration.getProperty(
		    AdapterConfiguration.VM_NODEID, "");
	    if (vmFullConfig == null || vmFullConfig.trim().length() == 0)
		throw new Exception(
			"velocity metrics config info should be given in properties file");

	    return vm.getOutputUtilsVelocityInstances(VMFactory, vmCategory,
		    vmComponentName, vmFullConfig, nodeId);
	} catch (Exception e) {
	    logger.error(e.getMessage());
	}
	return null;
    }
}
