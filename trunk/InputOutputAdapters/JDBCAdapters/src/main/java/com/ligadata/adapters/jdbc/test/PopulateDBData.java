package com.ligadata.adapters.jdbc.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import lombok.extern.log4j.Log4j2;

import org.apache.commons.dbcp2.BasicDataSource;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.ligadata.adapters.utility.WrappedScheduleExecutor;

@Log4j2
public class PopulateDBData {
	private static JSONObject localConfigs;
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
	
	public static void main(String args[]){
		
		try {
			
			InputStream is = null;
			BufferedReader reader = null;
			is = PopulateDBData.class.getResourceAsStream("/testDBProperties.json");
			reader = new BufferedReader(new InputStreamReader(is));
					
			StringBuilder b = new StringBuilder();
			String line = reader.readLine();
			while(line != null){
				b.append(line);
				line = reader.readLine();
			}
			reader.close();
			
			JSONParser parser = new JSONParser();
			JSONObject config = null;
			config =  (JSONObject)parser.parse(b.toString());
			if(config != null && config.size() >0){
				localConfigs = config;
				Long interval = (Long)config.get("interval");
				String timeUnits = (String)config.get("intervalUnits");
				TimeUnit unit = TimeUnit.valueOf(timeUnits.toUpperCase());
				
				WrappedScheduleExecutor scheduler = new WrappedScheduleExecutor(1);
				scheduler.scheduleAtFixedRate(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						
						BasicDataSource dataSource = new BasicDataSource();
						
						
						String dbUser = (String)localConfigs.get("dbUser");
						String dbPwd = (String)localConfigs.get("dbPwd");
						String dbName = (String)localConfigs.get("dbName");
						String dbDriver = (String)localConfigs.get("dbDriver");
						String dbURL = (String)localConfigs.get("dbURL");
						String tableName = (String)localConfigs.get("tableName");
						
						dataSource.setDriverClassName(dbDriver);
						if(dbName!=null && !dbName.isEmpty())
						  dataSource.setUrl(dbURL+"/"+dbName);
						else
						  dataSource.setUrl(dbURL);
						dataSource.setUsername(dbUser);
						dataSource.setPassword(dbPwd);
						
						
						dataSource.setTestWhileIdle(false);
						dataSource.setTestOnBorrow(true);
						dataSource.setValidationQuery("Select 1");
						dataSource.setTestOnReturn(false);
						
						dataSource.setMaxTotal(100);
						dataSource.setMaxIdle(5);
						dataSource.setMinIdle(0);
						dataSource.setInitialSize(5);
						dataSource.setMaxWaitMillis(5000);
						
						
						String tableScript = "CREATE TABLE IF NOT EXISTS "+tableName
								+ " ( id bigint auto_increment primary key,"
								+ " emp_id bigint not null,"
								+ " emp_first_name varchar(256) ,"
								+ " emp_last_name varchar(256) ,"
								+ " score int,"
								+ " added_date datetime,"
								+ " txn_date datetime"
								+ ") engine=InnoDB default charset latin1";
						
						boolean tableExists = false;
						Connection con = null;
						PreparedStatement pstmt = null;
						ResultSet rs = null;
						
						System.out.println("Inserting data at "+sdf.format(new Date()));
						
						try {
							
							
							con = dataSource.getConnection();
							
							if(con!=null){
								System.out.println("Connected to the DB");
								DatabaseMetaData dbmd = con.getMetaData();
								rs = dbmd.getTables(null, null, "%", null);
								
								//Look up for the tableName
								while(rs!=null && rs.next()){
									if(rs.getString(3).equalsIgnoreCase(tableName)){
										tableExists = true;
										break;
									}
								}
								
								//Create Table if it doesnt exist
								if(!tableExists){
									Statement stmt = con.createStatement();
									stmt.execute(tableScript);
									if(stmt.getUpdateCount() == 0 ){
										System.out.println("Created table "+tableName+" successfully...");
									}
								}else{
									System.out.println("Table "+tableName+" exists, skipping creation processs...");
								}
								
								pstmt = con.prepareStatement("INSERT INTO "+tableName+" (emp_id, emp_first_name, emp_last_name, score, added_date, txn_date  ) "
										+ "VALUES (?, ?, ? , ? , CURRENT_TIMESTAMP , ? ) ");
								con.setAutoCommit(false);
								//Now pump in 50k Records
								Random randomGenerator1 = new Random();
								Random randomGenerator2 = new Random();
								Random randomGenerator3 = new Random();
								for(int j=0;j<10000; j++){
									int randomId = randomGenerator1.nextInt(1000);
									int randomScore = randomGenerator2.nextInt(200);
									long randomTime = (long)(randomGenerator3.nextDouble()*System.currentTimeMillis());
									
									pstmt.setLong(1, randomId);
									pstmt.setString(2, "fname"+randomId);
									pstmt.setString(3, "lname"+randomId);
									pstmt.setInt(4, randomScore);
									pstmt.setTimestamp(5, new Timestamp(randomTime));
									
									int rows  = pstmt.executeUpdate();
									
									if(j % 1000 == 0)
										System.out.println("Inserting record "+j);
									
									if(rows == 1)
										continue;
									else
										break;
								}
								con.commit();
								
								rs.close();
								rs = null;
								pstmt.close();
								pstmt = null;
								con.close();
								con = null;
								
							}else{
								System.out.println("Unable to connect to the DB");
							}
						}catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						finally{
							try{
								if(rs != null) rs.close();
								if(pstmt != null) pstmt.close();
								if(con != null) con.close();
							}catch(SQLException exc){
								exc.printStackTrace();
								
							}
						}
					}
				}, 0, interval, unit);
			}
			
		} catch ( IOException | org.json.simple.parser.ParseException e1) {
			// TODO Auto-generated catch block
			log.error("Error while processing...."+e1.getMessage());
			
		}
	}
}
