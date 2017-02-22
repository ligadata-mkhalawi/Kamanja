package com.ligadata.adapters.jdbc;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ligadata.adapters.StatusCollectable;
import com.ligadata.adapters.AdapterConfiguration;
import com.ligadata.VelocityMetrics.InstanceRuntimeInfo;
import com.ligadata.VelocityMetrics.VelocityMetricsInfo;

public class BufferedJDBCSink extends AbstractJDBCSink {
    static Logger logger = LogManager.getLogger(BufferedJDBCSink.class);
    private String insertStatement;
    private List<ParameterMapping> insertParams;
    private ArrayList<JSONObject> buffer;
    private InstanceRuntimeInfo[] VMInstances;

    public BufferedJDBCSink() {
    }

    @Override
    public void init(AdapterConfiguration config, StatusCollectable sw)
	    throws Exception {
	super.init(config, sw);

	insertParams = new ArrayList<ParameterMapping>();
	buffer = new ArrayList<JSONObject>();
	String insertStr = config
		.getProperty(AdapterConfiguration.JDBC_INSERT_STATEMENT);
	if (insertStr == null)
	    throw new Exception(
		    "Insert statement not specified in the properties file.");

	logger.info("Insert statement: " + insertStr);
	insertStatement = buildStatementAndParameters(insertStr, insertParams);
	if (logger.isInfoEnabled()) {
	    logger.info(insertParams.size() + " parameters found.");
	    int i = 1;
	    for (ParameterMapping param : insertParams)
		logger.info("Parameter " + (i++) + ": path="
			+ Arrays.toString(param.path) + " type=" + param.type
			+ " typeName=" + param.typeName);
	}
	this.VMInstances = config.VMInstances;

	}

    @Override
    public boolean addMessage(String message) {
	try {
	    JSONParser jsonParser = new JSONParser();
	    JSONObject jsonObject = (JSONObject) jsonParser.parse(message);

	    if (jsonObject.get("dedup") != null
		    && "1".equals(jsonObject.get("dedup").toString())) {
		logger.debug("ignoring duplicate message.");
		return false;
	    }

	    buffer.add(jsonObject);
	} catch (Exception e) {
	    logger.error("Error processing message: " + e.getMessage()
		    + " - ignoring message : " + message, e);
	    return false;
	}

	return true;
    }

    @Override
    public void processAll(long batchId, long retryNumber) throws Exception {
	Connection connection = dataSource.getConnection();
	PreparedStatement statement = connection
		.prepareStatement(insertStatement);
	int totalStatements = buffer.size();
	int failedStatements = 0;
	VelocityMetricsInfo vm = new VelocityMetricsInfo();

	try {
	    for (JSONObject jsonObject : buffer) {
		if (bindParameters(statement, insertParams, jsonObject))
		    statement.addBatch();
		// Taking Velocity Metrics stats here
		if (this.VMInstances != null && this.VMInstances.length > 0) {
		    logger.info("BufferedJDBSSink vm increment "
			    + this.VMInstances.length);
		    for (int i = 0; i < this.VMInstances.length; i++) {
			if (this.VMInstances[i].typId() == 4) {
			    logger.info("BufferedJDBSSink vm increment typid 4 "
				    + this.VMInstances.length);
			    vm.incrementOutputUtilsVMetricsByKey(
				    this.VMInstances[i], null,
				    this.VMInstances[i].KeyStrings(), true);
			}
			if (this.VMInstances[i].typId() == 3) {
			    logger.info("BufferedJDBSSink vm increment typid 3 "
				    + this.VMInstances.length);

			    if (this.VMInstances[i].MsgKeys() != null
				    && this.VMInstances[i].MsgKeys().length > 0) {
				String[] myStringArray = new String[this.VMInstances[i]
					.MsgKeys().length];
				for (int k = 0; k < this.VMInstances[i]
					.MsgKeys().length; k++) {
				    myStringArray[k] = jsonObject.get(
					    this.VMInstances[i].MsgKeys()[k])
					    .toString();
				}
				vm.incrementOutputUtilsVMetricsByKey(
					this.VMInstances[i], myStringArray,
					this.VMInstances[i].KeyStrings(), true);
			    }
			}
		    }
		}
	    }

	    statement.executeBatch();

	} catch (BatchUpdateException e) {
	    logger.error("Error saving messages : " + e.getMessage(), e);
	    int[] updateCounts = e.getUpdateCounts();

	    for (int i = 0; i < updateCounts.length; i++) {
		if (updateCounts[i] == Statement.EXECUTE_FAILED) {
		    logger.error("failed to execute this statement : "
			    + buffer.get(i));
		    if (statusWriter != null) {
			statusWriter.addStatusMessage(
				this.STATUS_KEY,
				"failed to execute this statement : "
					+ buffer.get(i), false);
			statusWriter.setCompletionCode(this.STATUS_KEY, "1");

			JSONObject jsonObject = buffer.get(i);

			if (this.VMInstances != null
				&& this.VMInstances.length > 0) {
			    System.out.println("1 increment "
				    + this.VMInstances.length);
			    for (int j = 0; j < this.VMInstances.length; j++) {
				if (this.VMInstances[j].typId() == 4) {
				    System.out.println("2 increment "
					    + this.VMInstances.length);
				    vm.incrementOutputUtilsVMetricsByKey(
					    this.VMInstances[j], null,
					    this.VMInstances[j].KeyStrings(),
					    false);
				}
				if (this.VMInstances[j].typId() == 3) {
				    System.out.println("2 increment "
					    + this.VMInstances.length);

				    if (this.VMInstances[j].MsgKeys() != null
					    && this.VMInstances[j].MsgKeys().length > 0) {
					String[] myStringArray = new String[this.VMInstances[j]
						.MsgKeys().length];
					for (int k = 0; k < this.VMInstances[j]
						.MsgKeys().length; k++) {
					    myStringArray[k] = jsonObject.get(
						    this.VMInstances[j]
							    .MsgKeys()[k])
						    .toString();
					}
					vm.incrementOutputUtilsVMetricsByKey(
						this.VMInstances[j],
						myStringArray,
						this.VMInstances[j]
							.KeyStrings(), false);
				    }
				}
			    }
			}
		    }
		    failedStatements++;
		}
	    }
	    if (statusWriter != null)
		statusWriter.addStatusMessage(this.STATUS_KEY,
			"BatchUpdateException encountered "
				+ getCauseForDisplay(e), true);
	} finally {
	    try {
		if ((totalStatements == failedStatements && statusWriter != null))
		    statusWriter.setCompletionCode(this.STATUS_KEY, "-1");
		connection.commit();
		if (statusWriter != null) {
		    statusWriter.addStatus(this.STATUS_KEY,
			    String.valueOf(totalStatements - failedStatements),
			    String.valueOf(failedStatements));
		    statusWriter.externalizeStatusMessage(batchId, retryNumber,
			    "BufferedJDBCSink");
		}
	    } catch (Exception e) {
		logger.error("Error committing messages : " + e.getMessage(), e);
		if (statusWriter != null) {
		    statusWriter.addStatusMessage(this.STATUS_KEY,
			    "Error committing messages : "
				    + getCauseForDisplay(e), true);
		    statusWriter.addStatus(this.STATUS_KEY, String.valueOf(0),
			    String.valueOf(totalStatements));
		    statusWriter.setCompletionCode(this.STATUS_KEY, "-1");
		    statusWriter.externalizeStatusMessage(batchId, retryNumber,
			    "BufferedJDBCSink");
		}
	    }
	    try {
		statement.close();
	    } catch (Exception e) {
		logger.error("Error closing  Statement  : " + e.getMessage(), e);

	    }
	    try {
		connection.close();
	    } catch (Exception e) {
		logger.error("Error closing  Connection  : " + e.getMessage(),
			e);
	    }
	}
    }

    @Override
    public void clearAll() {
	buffer.clear();
    }

    private String getCauseForDisplay(Exception e) {
	java.io.StringWriter sw = new java.io.StringWriter();
	java.io.PrintWriter pw = new java.io.PrintWriter(sw);
	e.printStackTrace(pw);
	return sw.toString(); // stack trace as a string
    }
}
