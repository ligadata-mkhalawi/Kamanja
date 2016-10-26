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

public class BufferedJDBCSink extends AbstractJDBCSink {
    static Logger logger = LogManager.getLogger(BufferedJDBCSink.class);
    static final String STATUS_KEY = new String("SqlBatch");
    private String insertStatement;
    private List<ParameterMapping> insertParams;
    private ArrayList<JSONObject> buffer;

    public BufferedJDBCSink() {
    }

    @Override
    public void init(AdapterConfiguration config, StatusCollectable sw) throws Exception {
        super.init(config, sw);

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
    public void processAll(long batchId, long retryNumber) throws Exception {
        Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(insertStatement);
        int totalStatements = buffer.size();
        int failedStatements = 0;

        try {
            for (JSONObject jsonObject : buffer) {
                if (bindParameters(statement, insertParams, jsonObject))
                    statement.addBatch();
            }

            statement.executeBatch();
        } catch (BatchUpdateException e) {
            logger.error("Error saving messages : " + e.getMessage(), e);
            int[] updateCounts = e.getUpdateCounts();

            for (int i = 0; i < updateCounts.length; i++) {
                if (updateCounts[i] == Statement.EXECUTE_FAILED) {
                    logger.error("failed to execute this statement : " + buffer.get(i));
                    statusWriter.addStatusMessage(this.STATUS_KEY, "failed to execute this statement : " + buffer.get(i));
                    statusWriter.setCompletionCode(this.STATUS_KEY,"1");
                    failedStatements++;
                }
            }
            statusWriter.addStatusMessage(this.STATUS_KEY, "BatchUpdateException encountered " + getCauseForDisplay(e));
        } finally {
            try {
                if (totalStatements == failedStatements )
                    statusWriter.setCompletionCode(this.STATUS_KEY,"-1");
                connection.commit();
                statusWriter.addStatus(this.STATUS_KEY, String.valueOf(totalStatements - failedStatements), String.valueOf(failedStatements));
                statusWriter.externalizeStatusMessage(String.valueOf(batchId), String.valueOf(retryNumber), "BufferedJDBCSink");
            } catch (Exception e) {
                logger.error("Error committing messages : " + e.getMessage(), e);
                statusWriter.addStatusMessage(this.STATUS_KEY, "Error committing messages : " + getCauseForDisplay(e));
                statusWriter.addStatus(this.STATUS_KEY, String.valueOf(0), String.valueOf(totalStatements));
                statusWriter.setCompletionCode(this.STATUS_KEY,"-1");
                statusWriter.externalizeStatusMessage(String.valueOf(batchId), String.valueOf(retryNumber), "BufferedJDBCSink");
            }
            try {
                statement.close();
            } catch (Exception e) {
                logger.error("Error closing  Statement  : " + e.getMessage(), e);

            }
            try {
                connection.close();
            } catch (Exception e) {
                logger.error("Error closing  Connection  : " + e.getMessage(), e);
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
