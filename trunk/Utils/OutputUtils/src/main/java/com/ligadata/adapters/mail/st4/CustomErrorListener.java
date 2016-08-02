package com.ligadata.adapters.mail.st4;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.STMessage;

import com.ligadata.adapters.mail.pojo.Constants;

public class CustomErrorListener implements STErrorListener {
	
	static Logger logger = LogManager.getLogger(CustomErrorListener.class);

	@Override
	public void IOError(STMessage msg) {
		logError("xxx io error");
        report(Constants.IO_ERROR, msg);
	}

	@Override
	public void compileTimeError(STMessage msg) {
		logError("xxx compile time error");
        report(Constants.COMPILE_TIME_ERROR, msg);
	}

	@Override
	public void internalError(STMessage msg) {
		logError("xxx internal error");
        report(Constants.INTERNAL_ERROR, msg);

	}

	@Override
	public void runTimeError(STMessage msg) {
		report(Constants.RUNTIME_ERROR, msg);
	}
	
	private void report(String msgType, STMessage msg){
        logError(msgType + " " + String.format(msg.error.message, msg.arg, msg.arg2, msg.arg3) );
    }
	
	private void logError(String message){
		logger.error(message);
    }

}
