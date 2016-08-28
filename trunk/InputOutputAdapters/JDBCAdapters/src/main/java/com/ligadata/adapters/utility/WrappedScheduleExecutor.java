package com.ligadata.adapters.utility;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//@Log4j
public class WrappedScheduleExecutor extends ScheduledThreadPoolExecutor {

    final static Logger log = Logger.getLogger(WrappedScheduleExecutor.class);

	public WrappedScheduleExecutor(int corePoolSize) {
		super(corePoolSize);
	}
	
	@Override
    public ScheduledFuture scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return super.scheduleAtFixedRate(wrapRunnable(command), initialDelay, period, unit);
    }
	
	@Override
    public ScheduledFuture scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return super.scheduleWithFixedDelay(wrapRunnable(command), initialDelay, delay, unit);
    }
	
	private Runnable wrapRunnable(Runnable command) {
        return new LogOnExceptionRunnable(command);
	}
	
	private class LogOnExceptionRunnable implements Runnable {
        private Runnable runnable;

        public LogOnExceptionRunnable(Runnable command) {
                super();
                this.runnable = command;
        }

        @Override
        public void run() {
                try {
                        runnable.run();
                } catch (Exception e) {
                        // Log the exception
                        log.error("Execution error : " + runnable + ". Future executions will not happen...");
                        e.printStackTrace();
                        // Re-throw so that	the Executor also gets this error 
                        throw new RuntimeException(e);
                }
        }
	}
	
}
