package com.ligadata.adapters.jdbc;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ligadata.adapters.AdapterConfiguration;

public class BufferedJDBCSink extends AbstractJDBCSink {
	static Logger logger = LogManager.getLogger(BufferedJDBCSink.class);

	private String insertStatement;
	private List<ParameterMapping> insertParams;
	private ArrayList<JSONObject> buffer;

	public BufferedJDBCSink() {
	}

	@Override
	public void init(AdapterConfiguration config) throws Exception {
		super.init(config);

		insertParams = new ArrayList<ParameterMapping>();
		buffer = new ArrayList<JSONObject>();
		String insertStr = config.getProperty(AdapterConfiguration.JDBC_INSERT_STATEMENT);
		if (insertStr == null)
			throw new Exception("Insert statement not specified in the properties file.");

		logger.info("Insert statement: " + insertStr);
		insertStatement = buildStatementAndParameters(insertStr, insertParams);
		if (logger.isInfoEnabled()) {
			logger.info(insertParams.size() + " parameters found.");
			int i = 1;
			for (ParameterMapping param : insertParams)
				logger.info("Parameter " + (i++) + ": path=" + Arrays.toString(param.path) + " type=" + param.type
						+ " typeName=" + param.typeName);
		}
	}

	@Override
	public boolean addMessage(String message) {
		try {
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(message);

			if (jsonObject.get("dedup") != null && "1".equals(jsonObject.get("dedup").toString())) {
				logger.debug("ignoring duplicate message.");
				return false;
			}

			buffer.add(jsonObject);
		} catch (Exception e) {
			logger.error("Error processing message: " + e.getMessage() + " - ignoring message : " + message, e);
			return false;
		}

		return true;
	}

	@Override
	public void processAll() throws Exception {
		Connection connection = dataSource.getConnection();
		PreparedStatement statement = connection.prepareStatement(insertStatement);

		try {
			for (JSONObject jsonObject : buffer) {
				if (bindParameters(statement, insertParams, jsonObject))
					statement.addBatch();
			}

			statement.executeBatch();
		} catch (BatchUpdateException e) {
			logger.error("Error saving messages : " + e.getMessage(), e);
		} finally {
			try {
				connection.commit();
			} catch (Exception e) {
				logger.error("Error committing messages : " + e.getMessage(),e );
			}
			try{
				statement.close();
			}catch (Exception e){
				logger.error("Error closing  Statement  : " + e.getMessage(),e);

			}
			try {
				connection.close();
			}catch (Exception e){
				logger.error("Error closing  Connection  : " + e.getMessage(),e);
			}
		}
	}

	@Override
	public void clearAll() {
		buffer.clear();
	}
}
