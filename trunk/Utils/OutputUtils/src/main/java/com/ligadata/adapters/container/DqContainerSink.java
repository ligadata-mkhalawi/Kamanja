package com.ligadata.adapters.container;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.jdbc.AbstractJDBCSink;
import com.ligadata.adapters.StatusCollectable;

public class DqContainerSink extends AbstractJDBCSink {
	static Logger logger = LogManager.getLogger(DqContainerSink.class);

	protected class DqAggregation {
		String ait;
		String aitName;
		String date;
		long count;
		double dqSum;
		
		private DqAggregation(String ait, String aitName, String date) {
			this.ait = ait;
			this.aitName = aitName;
			this.date = date;
			this.count = 0;
			this.dqSum = 0;
		}
	}
	
	private HashMap<String, DqAggregation> buffer = new HashMap<String, DqAggregation>();
	private String sqlStr;
	
	@Override
	public void init(AdapterConfiguration config, StatusCollectable sw) throws Exception {
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
		if(fields.length < 4) {
			logger.error("Incorrect message. Expecting atleast 4 fields. Message: " + message);
			return false;
		}
		
		try {
			String key = fields[0] + ":" + fields[2];
			double dqScore = Double.parseDouble(fields[3]);
			DqAggregation agg = buffer.get(key);
			if (agg == null) {
				agg = new DqAggregation(fields[0], fields[1], fields[2]);
				buffer.put(key, agg);
			}
			
			agg.count++;
			agg.dqSum += dqScore;			
		} catch (Exception e) {
			logger.error("Error: " + e.getMessage(), e);
			return false;
		}
		
		return true;
	}

	@Override
	public void processAll(long batchid) throws Exception {
		Connection connection = dataSource.getConnection();
		PreparedStatement statement = connection.prepareStatement(sqlStr);

		for (String key : buffer.keySet()) {
			DqAggregation agg = buffer.get(key);
			
			try {
				statement.setString(1, agg.ait);
				statement.setString(2, agg.aitName);
				java.util.Date date = inputFormat.parse(agg.date);
				java.sql.Date dt = new java.sql.Date(date.getTime());
				statement.setDate(3, dt);
				statement.setLong(4, agg.count);
				double dqAvg = agg.dqSum/agg.count;
				statement.setDouble(5, dqAvg);
				
				logger.debug("Save DQ record: " + agg.ait + "," + agg.aitName + "," + agg.date + "," + agg.count + "," + dqAvg);
				statement.addBatch();
			} catch (Exception e) {
				logger.error("Error: " + e.getMessage(), e);
				try { statement.clearParameters(); } catch (SQLException e1) {}
			}
		}
		
		try {
			statement.executeBatch();
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
