package com.ligadata.KamanjaUI;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
//import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
//import java.io.InputStream;
//import java.io.FileInputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Iterator;
//import java.util.concurrent.ThreadFactory;

public abstract class RestBase {
    static String failureFormat = "{\"errors\":[{\"code\":%d,\"reason\":%d,\"content\":\"%s\"}]}";

    // Expecting only one column
    public String getJsonArray(Connection conn, String qry) throws Throwable {
        if (conn == null) return null;
        if (qry == null || qry.length() == 0) return null;
        Statement stmt = null;
        ResultSet rs = null;
        StringBuffer sb = new StringBuffer();

        try {
            stmt = conn.createStatement();

            System.out.println("Executing Query:" + qry);
            rs = stmt.executeQuery(qry);

            ResultSetMetaData columns = rs.getMetaData();

            int columnCnt = columns.getColumnCount();
            String[] columnNames = new String[columnCnt];

            int i = 0;
            while (i < columnCnt) {
                String colNm = "";
                try {
                    colNm = columns.getColumnName(i + 1);
                } catch (Throwable e) {
                    try {
                        colNm = columns.getColumnLabel(i + 1);
                    } catch (Throwable e1) {
                        e.printStackTrace();
                        throw e;
                    } finally {
                        try {
                            if (rs != null)
                                rs.close();
                        } catch (Throwable e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                columnNames[i] = colNm;
                i += 1;
            }

            int invalidRows = 0;
            int validRows = 0;

            sb.append("{\"result\":[");

            while (rs.next()) {
                i = 0;
                while (i < columnNames.length) {
                    Object obj = rs.getObject(columnNames[i]);
                    String objStr = "";
                    if (obj != null && obj instanceof com.orientechnologies.orient.core.record.impl.ODocument) {
                        objStr = ((com.orientechnologies.orient.core.record.impl.ODocument) obj).getIdentity().toString();
                    } else if (obj != null) {
                        objStr = obj.toString();
                    }

                    // if objStr is empty, ignore it for now
                    if (objStr.isEmpty()) {
                        invalidRows += 1;
                    } else {
                        if (validRows > 0)
                            sb.append(",");
                        sb.append(objStr);
                        validRows += 1;
                    }
                    i += 1;
                }
            }
            sb.append("]}");
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        } finally {
            Throwable t = null;
            try {
                if (rs != null)
                    rs.close();
            } catch (Throwable e) {
                e.printStackTrace();
                t = e;
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Throwable e) {
                e.printStackTrace();
                t = e;
            }
            if (t != null)
                throw t;
        }

        String results = sb.toString();
        System.out.println("Executing Query Results:" + results);
        return results;
    }

    //BUGBUG:: We may need to cache the connection. Need to check in connection pool whether it is properly pooling or not.
    public Connection getDbConnection(Properties prop) throws Throwable {
        Connection conn = null;
        try {
            Class.forName(prop.get("driverClassName").toString());
            Properties info = new Properties();
            info.put("user", prop.get("username").toString());
            info.put("password", prop.get("password").toString());
            info.put("db.usePool", "true"); // USE THE POOL
            info.put("db.pool.min", "1");   // MINIMUM POOL SIZE
            info.put("db.pool.max", "16");  // MAXIMUM POOL SIZE

            conn = DriverManager.getConnection(prop.get("url").toString(), info);
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }

        return conn;
    }

    public String prepareErrorString(int errorCode, Throwable error) {
        String str = String.format(failureFormat, errorCode, errorCode, ThrowableTraceString(error));
        return str;
    }

    private String ThrowableTraceString(Throwable t) {
        if (t == null) return "";
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    public String substituteQueryParamsAndGetJsonResult(Connection conn, String qry, String params) throws Throwable {
        String tmpqry = qry;

        if (params != null && params.trim().length() > 0) {
            JSONParser jsonParser = new JSONParser();
            JSONObject json;
            try {
                json = (JSONObject) jsonParser.parse(params);
            } catch (ParseException e) {
                throw e;
            } catch (Exception e) {
                throw e;
            } catch (Throwable e) {
                throw e;
            }
            
            if (json != null && json.containsKey("parameters")) {
                JSONObject parameters = (JSONObject) json.get("parameters");
                if (parameters != null) {
                    for (Object entry : parameters.entrySet()) {
                        Map.Entry<String, Object> obj = (Map.Entry<String, Object>) entry;
                        String key = "${" + obj.getKey() + "}";
                        tmpqry = tmpqry.replace(key, obj.getValue().toString());
                        System.out.println("Replaced all Keys : " + key + " with Value : " + obj.getValue().toString());
                    }
                }
            }
        }

        return getJsonArray(conn, tmpqry);
    }

    public String getViewResults(Connection conn, String qryToGetViewQuery, String params) throws Throwable {
        String retStr = null;
        String mainQryStr = null;
        int queriesExecuted = 0;
        try {
            mainQryStr = substituteQueryParamsAndGetJsonResult(conn, qryToGetViewQuery, params);
            if (mainQryStr == null || mainQryStr.length() == 0) {
                throw new Exception("Failed to get results for View query.\n ViewQuery:" + qryToGetViewQuery + "\nparams:" + params);
            }
            JSONParser jsonParser = new JSONParser();
            JSONObject json;
            try {
                json = (JSONObject) jsonParser.parse(mainQryStr);
                if (json != null && json.containsKey("result")) {
                    JSONArray arrJson = (JSONArray) json.get("result");
                    if (arrJson != null) {
                        Iterator<Object> iterator = arrJson.iterator();
                        // Running all, but getting the result from the last command
                        while (iterator.hasNext()) {
                            JSONObject oneRow = (JSONObject) iterator.next();
                            if (oneRow != null && oneRow.containsKey("ViewQueryCmds")) {
                                JSONArray cmds = (JSONArray) oneRow.get("ViewQueryCmds");
                                if (cmds != null) {
                                    Iterator<Object> cmdsIt = cmds.iterator();

                                    // Running all, but getting the result from the last command
                                    while (cmdsIt.hasNext()) {
                                        String jstr = (String) cmdsIt.next();
                                        retStr = substituteQueryParamsAndGetJsonResult(conn, jstr, params);
                                        queriesExecuted += 1;
                                    }
                                }                            }
                        }
                    }                }
            } catch (ParseException e) {
                throw e;
            } catch (Exception e) {
                throw e;
            } catch (Throwable e) {
                throw e;
            }
        } catch (Exception e) {
            throw e;
        } catch (Throwable e) {
            throw e;
        }

        if (retStr == null || retStr.length() == 0) {
            throw new Exception("Failed to get final results from View results:" + mainQryStr + "\nQueriesExecuted:" + queriesExecuted + "\nViewQuery:" + qryToGetViewQuery + "\nparams:" + params);
        }

        return retStr;
    }
}
