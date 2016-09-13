package com.ligadata.test;

import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.jdbc.SqlServerBulkCopySink;

public class SqlServerBulkTest {

	static String message1 = "{\"Id\": \"8134b9cb-446c-4bbf-9dd0-74f97b9ec79b\", \"ait\": \"11111\", \"createdAt\": \"2015-12-04T19:38:21.041\", \"timestamp\": \"2015-10-21T19:38:06.0000122Z\", \"timezone\": \"-07:00\", \"corrId\": \"\", \"corrIdParent\": \"\", \"dqscore\": 100, \"details\": {\"resource\": \"/uri\", \"processname\": \"http-bio-8080-exec-13\", \"aitName\": \"NL\", \"copsId\": \"65204265\", \"type\": \"userActivityEvent\", \"resourceport\": \"3233\", \"personNumber\": \"NL\", \"eventsource\": \"com.test.logging.MyLoggerTest\", \"resourcetype\": \"webapp\", \"transactionId\": \"0\", \"result\": \"0\", \"confidentialrecordcount\": \"10\", \"application\": \"BAC1111\", \"processid\": \"47\", \"resourcehost\": \"serverName2\", \"resourceprotocol\": \"http\", \"clientip\": \"1.1.1.13\", \"proprietaryrecordcount\": \"10\", \"action\": \"read1\", \"systemuser\": \"NBDJ5PG\", \"user\": \"userid\", \"device\": \"FD89D67D43EC1\", \"timePartitionData\": \"0\"}}";
	static String message2 = "{\"Id\": \"8134b9cb-446c-4bbf-9dd0-74f97b9ec79b\", \"ait\": \"21111\", \"createdAt\": \"2015-12-04T19:38:21.041\", \"timestamp\": \"201-10-21T19:38:06.0000122Z\", \"timezone\": \"-07:00\", \"corrId\": \"\", \"corrIdParent\": \"\", \"dqscore\": 100, \"details\": {\"resource\": \"/uri\", \"processname\": \"http-bio-8080-exec-13\", \"aitName\": \"NL\", \"copsId\": \"75204265\", \"type\": \"userActivityEvent\", \"resourceport\": \"3233\", \"personNumber\": \"NL\", \"eventsource\": \"com.test.logging.MyLoggerTest\", \"resourcetype\": \"webapp\", \"transactionId\": \"0\", \"result\": \"0\", \"confidentialrecordcount\": \"10\", \"application\": \"BAC1111\", \"processid\": \"47\", \"resourcehost\": \"serverName2\", \"resourceprotocol\": \"http\", \"clientip\": \"1.1.1.13\", \"proprietaryrecordcount\": \"10\", \"action\": \"read2\", \"systemuser\": \"NBDJ5PG\", \"user\": \"userid\", \"device\": \"FD89D67D43EC1\", \"timePartitionData\": \"0\"}}";
	static String message3 = "{\"Id\": \"8134b9cb-446c-4bbf-9dd0-74f97b9ec79b\", \"ait\": \"31111\", \"createdAt\": \"2015-12-04T19:38:21.041\", \"timestamp\": \"2015-10-21T19:38:06.0000122Z\", \"timezone\": \"-07:00\", \"corrId\": \"\", \"corrIdParent\": \"\", \"dqscore\": 100, \"details\": {\"resource\": \"/uri\", \"processname\": \"http-bio-8080-exec-13\", \"aitName\": \"NL\", \"copsId\": \"85204265\", \"type\": \"userActivityEvent\", \"resourceport\": \"3233\", \"personNumber\": \"NL\", \"eventsource\": \"com.test.logging.MyLoggerTest\", \"resourcetype\": \"webapp\", \"transactionId\": \"0\", \"result\": \"0\", \"confidentialrecordcount\": \"10\", \"application\": \"BAC1111\", \"processid\": \"47\", \"resourcehost\": \"serverName2\", \"resourceprotocol\": \"http\", \"clientip\": \"1.1.1.13\", \"proprietaryrecordcount\": \"10\", \"action\": \"read3\", \"systemuser\": \"NBDJ5PG\", \"user\": \"userid\", \"device\": \"FD89D67D43EC1\", \"timePartitionData\": \"0\"}}";
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		AdapterConfiguration config;
		try {
			config = new AdapterConfiguration("src/main/resources/copsbulkinsert.properties");
			SqlServerBulkCopySink processor = new SqlServerBulkCopySink();
			processor.init(config);
			processor.addMessage(message1);
			processor.addMessage(message2);
			processor.addMessage(message3);
			processor.processAll();
			processor.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

}
