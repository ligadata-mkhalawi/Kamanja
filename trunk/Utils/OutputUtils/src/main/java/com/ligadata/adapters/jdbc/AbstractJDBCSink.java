package com.ligadata.adapters.jdbc;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.simple.JSONObject;

import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.adapters.BufferedMessageProcessor;

public abstract class AbstractJDBCSink implements BufferedMessageProcessor {
	static Logger logger = LogManager.getLogger(AbstractJDBCSink.class);

	protected BasicDataSource dataSource;
	protected SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

	protected class ParameterMapping {
		String typeName;
		int type;
		String[] path;

		protected ParameterMapping(String name, int type, String[] path) {
			this.typeName = name;
			this.type = type;
			this.path = path;
		}
	}

	public AbstractJDBCSink() {
	}

	protected String buildStatementAndParameters(String sqlStr, List<ParameterMapping> paramArray) throws SQLException {
		// replace parameters specified as {$..} with ?
		String sql = sqlStr.replaceAll("\\{\\$[^\\}]+\\}", "?");
		logger.debug("SQL: " + sql);

		Connection connection = null;
		PreparedStatement statement = null;
		ParameterMetaData metadata = null;
		try {
			connection = dataSource.getConnection();
			statement = connection.prepareStatement(sql);
			metadata = statement.getParameterMetaData();

			// extract parameters between {$..}
			Matcher matcher = Pattern.compile("\\{\\$([^\\}]+)").matcher(sqlStr);
			int pos = -1;
			int param = 1;
			while (matcher.find(pos + 1)) {
				pos = matcher.start();
				String path = matcher.group(1);
				paramArray.add(new ParameterMapping(metadata.getParameterTypeName(param),
						metadata.getParameterType(param), path.split("\\.")));
				param++;
			}
		} finally {
			try {
				if (statement != null)
					statement.close();
				if (connection != null)
					connection.close();
			} catch (SQLException e) {
			}

		}

		return sql;
	}

	protected boolean bindParameters(PreparedStatement statement, List<ParameterMapping> paramArray,
			JSONObject jsonObject) {
		int paramIndex = 1;
		String key = null;
		String value = null;
		int remainingParamIndex = -1;
		boolean success = true;
		JSONObject jo = (JSONObject) jsonObject.clone();
		jo.remove("dedup");

		try {
			for (ParameterMapping param : paramArray) {
				key = Arrays.toString(param.path);

				// Defer processing of special parameter _Remaining_Attributes_
				// to end
				if (param.path[0].equalsIgnoreCase("_Remaining_Attributes_")) {
					remainingParamIndex = paramIndex;
					paramIndex++;
					continue;
				}

				// traverse JSON tree to get the value
				JSONObject subobject = jo;
				for (int i = 0; i < param.path.length - 1; i++) {
					if (subobject != null)
						subobject = ((JSONObject) subobject.get(param.path[i]));
				}
				value = null;
				if (subobject != null && subobject.get(param.path[param.path.length - 1]) != null)
					value = subobject.remove(param.path[param.path.length - 1]).toString();

				logger.debug("key=[" + key + "] value=[" + value + "] type=[" + param.type + "] typeName=["
						+ param.typeName + "]");

				if (param.type == java.sql.Types.VARCHAR || param.type == java.sql.Types.LONGVARCHAR
						|| param.type == java.sql.Types.CHAR) {
					// String
					statement.setString(paramIndex, value);
				} else if (param.type == java.sql.Types.BIGINT || param.type == java.sql.Types.INTEGER
						|| param.type == java.sql.Types.SMALLINT) {
					// Integer
					if (value == null || "".equals(value))
						statement.setNull(paramIndex, java.sql.Types.BIGINT);
					else
						statement.setLong(paramIndex, Long.parseLong(value));
				} else if (param.type == java.sql.Types.DOUBLE || param.type == java.sql.Types.FLOAT
						|| param.type == java.sql.Types.REAL || param.type == java.sql.Types.DECIMAL
						|| param.type == java.sql.Types.NUMERIC) {
					// Double
					if (value == null || "".equals(value))
						statement.setNull(paramIndex, java.sql.Types.DOUBLE);
					else
						statement.setDouble(paramIndex, Double.parseDouble(value));
				} else if (param.type == java.sql.Types.DATE) {
					// Date
					java.sql.Date dt = null;
					if (value == null || "".equals(value))
						statement.setNull(paramIndex, java.sql.Types.DATE);
					else {
						java.util.Date date = inputFormat.parse(value);
						dt = new java.sql.Date(date.getTime());
						statement.setDate(paramIndex, dt);
					}
				} else if (param.type == java.sql.Types.TIMESTAMP) {
					// timestamp
					Timestamp ts = null;
					if (value == null || "".equals(value))
						statement.setNull(paramIndex, java.sql.Types.TIMESTAMP);
					else {
						java.util.Date date = inputFormat.parse(value);
						ts = new Timestamp(date.getTime());
						statement.setTimestamp(paramIndex, ts);
					}
				} else {
					throw new Exception("Unsupported sql data type " + param.typeName + " [" + param.type + "]");
				}
				paramIndex++;
			}

			// set letfover attributes to _Remaining_Attributes_ parameter
			if (remainingParamIndex > 0 &&  jo.size() > 0) {
				statement.setString(remainingParamIndex, jo.toJSONString());
			}

		} catch (Exception e) {
			logger.error("Error binding parameters: " + e.getMessage() + " for Parameter index : [" + paramIndex +
					"] Key : [" + key + "] value : [" + value + "] - ignoring message : " + jsonObject.toJSONString(), e);
			try {
				statement.clearParameters();
			} catch (SQLException e1) {
			}
			success = false;
		}

		return success;
	}

	@Override
	public void init(AdapterConfiguration config) throws Exception {
		dataSource = new BasicDataSource();
		dataSource.setDriverClassName(config.getProperty(AdapterConfiguration.JDBC_DRIVER));
		dataSource.setUrl(config.getProperty(AdapterConfiguration.JDBC_URL));
		dataSource.setUsername(config.getProperty(AdapterConfiguration.JDBC_USER));
		dataSource.setPassword(config.getProperty(AdapterConfiguration.JDBC_PASSWORD));
		dataSource.setDefaultAutoCommit(false);

		dataSource.setMaxTotal(Integer.parseInt(config.getProperty(AdapterConfiguration.DBCP_MAX_TOTAL, "2")));
		dataSource.setMaxIdle(Integer.parseInt(config.getProperty(AdapterConfiguration.DBCP_MAX_IDLE, "2")));
		dataSource.setMaxWaitMillis(Long.parseLong(config.getProperty(AdapterConfiguration.DBCP_MAX_WAIT_MILLIS, "10000")));
		dataSource.setTestOnBorrow(Boolean.parseBoolean(config.getProperty(AdapterConfiguration.DBCP_TEST_ON_BORROW, "true")));
		dataSource.setValidationQuery(config.getProperty(AdapterConfiguration.DBCP_VALIDATION_QUERY, "SELECT 1"));

		inputFormat = new SimpleDateFormat(
				config.getProperty(AdapterConfiguration.INPUT_DATE_FORMAT, "yyyy-MM-dd'T'HH:mm:ss.SSS"));
	}

	@Override
	public abstract boolean addMessage(String message);

	@Override
	public abstract void processAll() throws Exception;

	@Override
	public void clearAll() {
	}

	@Override
	public void close() {
		try {
			if (dataSource != null)
				dataSource.close();
		} catch (SQLException e) {
		}
	}
}
