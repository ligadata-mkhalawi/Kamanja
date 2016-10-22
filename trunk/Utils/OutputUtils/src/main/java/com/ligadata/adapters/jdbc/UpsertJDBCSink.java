package com.ligadata.adapters.jdbc;

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
import com.ligadata.adapters.StatusCollectable;

public class UpsertJDBCSink extends AbstractJDBCSink {
	static Logger logger = LogManager.getLogger(UpsertJDBCSink.class);

	private String insertSql;
	private List<ParameterMapping> insertParams;
	private String updateSql;
	private List<ParameterMapping> updateParams;
	private ArrayList<JSONObject> buffer;

	public UpsertJDBCSink() {
	}

	@Override
	public void init(AdapterConfiguration config,  StatusCollectable sw) throws Exception {
		super.init(config, sw);

		buffer = new ArrayList<JSONObject>();
		insertParams = new ArrayList<ParameterMapping>();
		updateParams = new ArrayList<ParameterMapping>();
		String insertStr = config.getProperty(AdapterConfiguration.JDBC_INSERT_STATEMENT);
		if (insertStr == null)
			throw new Exception("Insert statement not specified in the properties file.");

		String updateStr = config.getProperty(AdapterConfiguration.JDBC_UPDATE_STATEMENT);
		if (updateStr == null)
			throw new Exception("Update statement not specified in the properties file.");

		logger.info("Insert statement: " + insertStr);
		insertSql = buildStatementAndParameters(insertStr, insertParams);
		if (logger.isInfoEnabled()) {
			logger.info(insertParams.size() + " parameters found.");
			int i = 1;
			for (ParameterMapping param : insertParams)
				logger.info("Parameter " + (i++) + ": path=" + Arrays.toString(param.path) + " type=" + param.type
						+ " typeName=" + param.typeName);
		}

		logger.info("Update statement: " + updateStr);
		updateSql = buildStatementAndParameters(updateStr, updateParams);
		if (logger.isInfoEnabled()) {
			logger.info(updateParams.size() + " parameters found.");
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
			logger.error("Error processing message - ignoring message : " + e.getMessage(), e);
			return false;
		}

		return true;
	}

	@Override
	public void processAll(long batchId) throws Exception {
		Connection connection = dataSource.getConnection();
		PreparedStatement updateStatement = connection.prepareStatement(updateSql);
		PreparedStatement insertStatement = connection.prepareStatement(insertSql);

		try {
			for (JSONObject jsonObject : buffer) {
				if (bindParameters(updateStatement, updateParams, jsonObject)) {
					updateStatement.execute();
					if (updateStatement.getUpdateCount() == 0) {
						if (bindParameters(insertStatement, insertParams, jsonObject))
							insertStatement.execute();
					}

					connection.commit();
				}
			}
		} catch (SQLException e) {
		} finally {
			try {
				connection.commit();
			} catch (Exception e) {
				logger.error("Error committing messages : " + e.getMessage(),e );
			}
			try{
				updateStatement.close();
			}
			catch (Exception e){
				logger.error("Error updating messages : " + e.getMessage(),e );
			}
			try{
				insertStatement.close();
			}
			catch (Exception e){
				logger.error("Error inserting messages : " + e.getMessage(),e );
			}
			try{
				connection.close();
			}
			catch (Exception e){
				logger.error("Error closing connection : " + e.getMessage(),e );

			}
		}

	}
}
