package com.ligadata.adapters.jdbc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ligadata.adapters.AdapterConfiguration;

public class SqlServerBulkInsertSink extends AbstractJDBCSink {
	static Logger logger = LogManager.getLogger(SqlServerBulkInsertSink.class);

	private List<ParameterMapping> fields;
	private String fieldSeperator;
	private String recordSeperator;
	private String workingDir;
	private String sqlserverShare;
	private String dataFileName;
	private String sqlDataFileName;
	private String errorFileName;
	private String tableName;
	private String formatFile;
	private BufferedWriter out;

	public SqlServerBulkInsertSink() {
	}

	@Override
	public void init(AdapterConfiguration config) throws Exception {
		super.init(config);

		fieldSeperator = config.getProperty(AdapterConfiguration.FILE_FIELD_SEPERATOR, "\u0000");
		recordSeperator = config.getProperty(AdapterConfiguration.FILE_RECORD_SEPERATOR, "\n");
		
		tableName = config.getProperty(AdapterConfiguration.INSERT_TABLE_NAME);
		if (tableName == null)
			throw new Exception("Tabel name not specified in the properties file.");
		
		formatFile = config.getProperty(AdapterConfiguration.INSERT_FORMAT_FILE);
		if (formatFile == null)
			throw new Exception("Format file not specified in the properties file.");
		
		fields = new ArrayList<ParameterMapping>();
		String fieldOrderStr = config.getProperty(AdapterConfiguration.FILE_FIELD_ORDER);
		if (fieldOrderStr == null)
			throw new Exception("Field order not specified in the properties file.");
		int i = 1;
		for (String param : fieldOrderStr.split(",")) {
			fields.add(new ParameterMapping(""+(i++), 0, param.split("\\."))) ;
		}

		workingDir = config.getProperty(AdapterConfiguration.WORKING_DIRECTORY);
		if (workingDir == null)
			throw new Exception("Working Directory not specified in the properties file.");
		if(!workingDir.endsWith("/"))
			workingDir = workingDir + "/";
		
		sqlserverShare = config.getProperty(AdapterConfiguration.SQLSERVER_SHARE);
		if (sqlserverShare == null)
			throw new Exception("SqlServer share directory not specified in the properties file.");
		if(!sqlserverShare.endsWith("/"))
			sqlserverShare = sqlserverShare + "/";

		long suffix = System.currentTimeMillis();
		dataFileName = workingDir + "data" + suffix + ".dat";
		sqlDataFileName = sqlserverShare + "data" + suffix + ".dat";
		errorFileName = sqlserverShare + "error" + suffix + ".dat";
		
		if (logger.isInfoEnabled()) {
			logger.info("Field Seperator: " + fieldSeperator);
			logger.info("Field count: " + fields.size());
			for (ParameterMapping param : fields)
				logger.info("Field " + param.typeName + " : path=" + Arrays.toString(param.path));
			logger.info("Using temporary data file: " + dataFileName);
			logger.info("Insert into" + tableName + " using format file " + formatFile);
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

	private boolean checkDataErrors() {
		File file = new File(errorFileName);
		boolean errors = file.exists();

		if (errors) {
			file.delete();
			file = new File(errorFileName + ".Error.Txt");
			file.delete();
		}

		return errors;
	}
	
	@Override
	public void processAll() throws Exception {
		
		try {
			if(out!= null)
				out.close(); 
		} catch(Exception e) {}
		
		checkDataErrors();
		
		Connection connection = dataSource.getConnection();
		String insertStatement = "BULK INSERT " + tableName + " FROM '" + sqlDataFileName + "' WITH ( FORMATFILE = '" + formatFile + "', ERRORFILE = '"+ errorFileName +"')";
		logger.debug("Bulk insert: " + insertStatement);
		
		Statement statement = connection.createStatement();
		try {
			statement.execute(insertStatement);
		} catch (SQLException e) {
			if(checkDataErrors())
				logger.debug("Found some data errors during bulk insert.");
			else
				throw e;
		} finally {
			try {
				connection.commit();
			} catch (Exception e) {
				logger.error("Exception committing messages : "+ e.getMessage(), e);
			}
			try {
				statement.close();
			}catch (Exception e){
				logger.error("Exception closing statement : "+e.getMessage(), e);
			}
			try {
				connection.close();
			}
			catch (Exception e){
				logger.error("Exception closing connection : "+ e.getMessage(),e);
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
