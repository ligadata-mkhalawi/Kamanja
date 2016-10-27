package com.ligadata.adapters.container;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ligadata.adapters.StatusCollectable;
import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.jdbc.AbstractJDBCSink;


public class FileStatusSink extends AbstractJDBCSink {
	static Logger logger = LogManager.getLogger(FileStatusSink.class);
	
	private String sqlStr;
	private ArrayList<String[]> buffer = new ArrayList<String[]>();

	@Override
	public void init(AdapterConfiguration config,  StatusCollectable sw) throws Exception {
		super.init(config, sw);

		sqlStr = config.getProperty(AdapterConfiguration.JDBC_INSERT_STATEMENT);
		if(sqlStr == null)
			throw new Exception("Sql statement not specified in the properties file.");
		logger.info("Sql statement: " + sqlStr);
		
		// Make sure database properties and sql statement are correct
		Connection connection = null;
		PreparedStatement statement = null;
		try {
			connection = dataSource.getConnection();
			statement = connection.prepareStatement(sqlStr);
		} finally {
			try {
				if(statement != null)
					statement.close();
				if (connection != null)
					connection.close();
			} catch(Exception e) {}	
		}
	}

	@Override
	public boolean addMessage(String message) {
		String[] fields = message.split(",");
		if(fields.length < 5) {
			logger.error("Incorrect message. Expecting atleast 5 fields. Message: " + message);
			return false;
		}

		if("File_Total_Result".equalsIgnoreCase(fields[0]))
			buffer.add(fields);
		
		return true;
	}

	@Override
	public void processAll(long batchid, long retryNumber) throws Exception {
		Connection connection = dataSource.getConnection();
		PreparedStatement statement = connection.prepareStatement(sqlStr);

	    for(String[] fields : buffer) {
			try {
				statement.setString(1, fields[3]);
				statement.setString(2, fields[2]);
				java.util.Date dt = inputFormat.parse(fields[1]);
				statement.setTimestamp(3, new Timestamp(dt.getTime()));
				statement.setLong(4, Long.parseLong(fields[4]));
				statement.addBatch();
			} catch (Exception e) {
				logger.error("Error: " + e.getMessage(), e);
				try { statement.clearParameters(); } catch (SQLException e1) {}
			}
	    }
	    
		try {
	        statement.executeBatch();
	        connection.commit();
		} catch (BatchUpdateException e) {
		} finally {
			try { 
				connection.commit();
				statement.close();
				connection.close();
			} catch (SQLException e) {}
		}
	}

	@Override
	public void clearAll() {
		buffer.clear();
	}
}
