package com.ligadata.adapters.jdbc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ligadata.adapters.AdapterConfiguration;
import com.microsoft.sqlserver.jdbc.SQLServerBulkCSVFileRecord;
import com.microsoft.sqlserver.jdbc.SQLServerBulkCopy;
import com.ligadata.adapters.StatusCollectable;

public class SqlServerBulkCopySink extends AbstractJDBCSink {
	static Logger logger = LogManager.getLogger(SqlServerBulkCopySink.class);

	private String connectionStr;
	private String dataTimeFormatStr;
	private List<ParameterMapping> fields;
	private String [] columns;
	private String fieldSeperator;
	private String recordSeperator;
	private String workingDir;
	private String dataFileName;
	private String tableName;
	private BufferedWriter out;

	public SqlServerBulkCopySink() {
	}

	@Override
	public void init(AdapterConfiguration config, StatusCollectable sw) throws Exception {
		super.init(config, sw);

		connectionStr = config.getProperty(AdapterConfiguration.JDBC_URL) 
				+ ";user=" + config.getProperty(AdapterConfiguration.JDBC_USER) 
				+ ";password=" + config.getProperty(AdapterConfiguration.JDBC_PASSWORD);

		dataTimeFormatStr = config.getProperty(AdapterConfiguration.INPUT_DATE_FORMAT, "yyyy-MM-dd'T'HH:mm:ss.SSS");
		fieldSeperator = config.getProperty(AdapterConfiguration.FILE_FIELD_SEPERATOR, "\u0000");
		recordSeperator = config.getProperty(AdapterConfiguration.FILE_RECORD_SEPERATOR, "\n");
		
		fields = new ArrayList<ParameterMapping>();
		String insertStr = config.getProperty(AdapterConfiguration.JDBC_INSERT_STATEMENT);
		if (insertStr == null)
			throw new Exception("Insert statement not specified in the properties file.");
		logger.info("Insert statement: " + insertStr);

		buildStatementAndParameters(insertStr, fields);

		int begin = insertStr.toUpperCase().indexOf("INTO")+4;
		int end = insertStr.indexOf('(');
		tableName = insertStr.substring(begin, end).trim();
		tableName = tableName.replace("[", "");
		tableName = tableName.replace("]", "");
		logger.info("Table name: " + tableName);
		
		begin = insertStr.indexOf('(')+1;
		end = insertStr.indexOf(')');
		String colStr = insertStr.substring(begin, end);
		columns = colStr.split(",", -1);
		for(int i = 0; i < columns.length; i++) {
			columns[i] = columns[i].trim();
			columns[i] = columns[i].replace("[", "");
			columns[i] = columns[i].replace("]", "");
		}

		workingDir = config.getProperty(AdapterConfiguration.WORKING_DIRECTORY);
		if (workingDir == null)
			throw new Exception("Working Directory not specified in the properties file.");
		if(!workingDir.endsWith("/"))
			workingDir = workingDir + "/";
		
		long suffix = System.currentTimeMillis();
		dataFileName = workingDir + "data" + suffix + ".dat";
		
		if (logger.isInfoEnabled()) {
			logger.info("Field Seperator: " + fieldSeperator);
			logger.info("Field count: " + fields.size());
			int i = 0;
			for (ParameterMapping param : fields) {
				logger.info("Field " + (i+1) + ": column=" + columns[i] + " path=" + Arrays.toString(param.path)
					+ " type=" + param.type + " typeName=" + param.typeName);
				i++;
			}
			logger.info("Using temporary data file: " + dataFileName);
		}
		
		out = new BufferedWriter(new FileWriter(dataFileName));
	}

	@Override
	public boolean addMessage(String message) {
		StringBuffer part1 = new StringBuffer();
		StringBuffer part2 = new StringBuffer();
		StringBuffer record = part1;
		try {
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonObject = (JSONObject) jsonParser.parse(message);

			if (jsonObject.get("dedup") != null && "1".equals(jsonObject.get("dedup").toString())) {
				logger.debug("ignoring duplicate message.");
				return false;
			}

			jsonObject.remove("dedup");
			for (ParameterMapping param : fields) {
				// Defer processing of special parameter _Remaining_Attributes_ to end
				if (param.path[0].equalsIgnoreCase("_Remaining_Attributes_")) {
					record = part2;
					continue;
				}

				// traverse JSON tree to get the value
				JSONObject subobject = jsonObject;
				for (int i = 0; i < param.path.length - 1; i++) {
					if (subobject != null)
						subobject = ((JSONObject) subobject.get(param.path[i]));
				}
				String value = "";
				if (subobject != null && subobject.get(param.path[param.path.length - 1]) != null)
					value = subobject.remove(param.path[param.path.length - 1]).toString();

				logger.debug("Field=[" + param.typeName + "] value=[" + value + "]");
				record.append(value).append(fieldSeperator);				
			}
			
			record = part1;
			// set letfover attributes to _Remaining_Attributes_ parameter
			if (!jsonObject.isEmpty())
				record.append(jsonObject.toJSONString()).append(fieldSeperator);
			
			if(part2.length() > 0)
				record.append(part2);

			out.write(record.substring(0, record.length()-1) + recordSeperator);
		} catch (Exception e) {
			logger.error("Error processing message - ignoring message : " + e.getMessage(), e);
			return false;
		}

		return true;
	}
	
	@Override
	public void processAll(long batchId) throws Exception {
		
		try {
			if(out!= null)
				out.close(); 
		} catch(Exception e) {}

		Connection connection = DriverManager.getConnection(connectionStr);
		logger.info("Connection: " + connection.getClass().getName());
		
		SQLServerBulkCSVFileRecord fileRecord = null;
		SQLServerBulkCopy bulkCopy = null;
		try {
			fileRecord = new SQLServerBulkCSVFileRecord(dataFileName, null, fieldSeperator, false);
			fileRecord.setTimestampWithTimezoneFormat(dataTimeFormatStr);
			fileRecord.setTimeWithTimezoneFormat(dataTimeFormatStr);

			int i = 1;
			for (ParameterMapping field : fields) {
				if(field.type == java.sql.Types.VARCHAR || field.type == java.sql.Types.LONGVARCHAR)
					fileRecord.addColumnMetadata(i, null, field.type, 2048, 0);
				else
					fileRecord.addColumnMetadata(i, null, field.type, 0, 0);	
				i++;
			}
			
			bulkCopy = new SQLServerBulkCopy(connection);
			bulkCopy.setDestinationTableName(tableName);
			
			for (i = 0; i < columns.length; i++) {
				bulkCopy.addColumnMapping(i+1, columns[i]);	
			}

			bulkCopy.writeToServer(fileRecord);
		//} catch (SQLException e) {
		} finally {
			try {
				connection.commit();
				if(bulkCopy != null)
					bulkCopy.close();
				if(fileRecord != null)
					fileRecord.close();
				connection.close();
			} catch (SQLException e) {
			}
		}
		
		try {
			File file = new File(dataFileName);
			file.delete();
		} catch(Exception e) {
			logger.warn("Could not delete temporary file " + dataFileName + ". Cause: " + e.getMessage());
		}
	}

	@Override
	public void clearAll() {
		try {
			if(out!= null)
				out.close(); 
		} catch(Exception e) {}

		try {
			File file = new File(dataFileName);
			if(file.exists())
				file.delete();
		} catch(Exception e) {}
	}
	
	@Override
	public void close() {
		clearAll();
		super.close();		
	}
}
