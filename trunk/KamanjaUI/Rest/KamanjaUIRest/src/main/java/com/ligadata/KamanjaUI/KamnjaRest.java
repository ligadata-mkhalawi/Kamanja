package com.ligadata.KamanjaUI;

import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.Iterator;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/kamanjarest")
public class KamnjaRest extends RestBase {
    @Context
    private ServletContext context;

    @GET
    @Path("/Login")
    public Response Login() throws InterruptedException {
        //BUGBUG:: Yet to fix this
        return Response.status(200).entity("Success").build();
    }

  /*
   * Input:
   * Output: JSON text returned from Query if successful, otherwise return failure code & failure string
   * Other: We also need to cache all the views
   * Query: select @this.toJSON() as jsonrec from (select from KamanjaViews)
   */

    @GET
    @Path("/GetViews")
    public Response GetViews(@Context HttpServletRequest request) throws InterruptedException {
        // HttpSession session = request.getSession();

        Connection conn = null;
        try {
            conn = getDbConnection((Properties) context.getAttribute("prop"));
            // Always get new views and replace them in cache if we have old values
            String qry = "select @this.toJSON() as jsonrec from (select ViewName, MainQuery.parameters as MainQuery_parameters, DepthQuery.parameters as DepthQuery_parameters, PropertiesQuery.parameters as PropertiesQuery_parameters, SymbolClasses, isDefault from KamanjaViews where isActive = true)";
            String retStr = substituteQueryParamsAndGetJsonResult(conn, qry, null);
            // String retStr = getJsonArray(conn, qry);
//            synchronized (session) {
//                session.setAttribute("Views", retStr);
//            }
            // Cache these results and send back
            return Response.status(200).entity(retStr).build();
        } catch (Throwable e) {
            return Response.status(500).entity(prepareErrorString(500, e)).build();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (Throwable e) {
                // What to do????
            }
        }
    }

  /*
   * Input: ViewName
   * Output: JSON text returned from Query if successful, otherwise return failure code
   * Query: This depends on MainQuery of the Selected View
   * Process:
   */

    @POST
    @Path("/GetView")
    public Response GetView(@Context HttpServletRequest request, String jsonBody) throws InterruptedException {
        Connection conn = null;
        try {
            String qry = "select @this.toJSON() as jsonrec from (select MainQuery.commands as ViewQueryCmds from KamanjaViews where ViewName = '${ViewName}')";
            conn = getDbConnection((Properties) context.getAttribute("prop"));
            try {
                String retStr = getViewResults(conn, qry, jsonBody);
                return Response.status(200).entity(retStr).build();
            } catch (Exception e) {
                return Response.status(500).entity(prepareErrorString(500, e)).build();
            }
        } catch (Throwable e) {
            return Response.status(500).entity(prepareErrorString(500, e)).build();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (Throwable e) {
                // What to do????
            }
        }
    }

  /*
   * Input: ViewName, RID
   * Output: JSON text returned from Query if successful, otherwise return failure code
   * Query: This depends on DepthNTraverseQuery of the Selected View
   * Process: Replace ${RID} with given RID in the given ViewName DepthNTraverseQuery and execute the query
   */

    @POST
    @Path("/DepthTraverse")
    public Response DepthTraverse(@Context HttpServletRequest request, String jsonBody) throws InterruptedException {
        Connection conn = null;
        try {
            String qry = "select @this.toJSON() as jsonrec from (select DepthQuery.commands as ViewQueryCmds from KamanjaViews where ViewName = '${ViewName}')";
            conn = getDbConnection((Properties) context.getAttribute("prop"));
            try {
                String retStr = getViewResults(conn, qry, jsonBody);
                return Response.status(200).entity(retStr).build();
            } catch (Exception e) {
                return Response.status(500).entity(prepareErrorString(500, e)).build();
            }
        } catch (Throwable e) {
            return Response.status(500).entity(prepareErrorString(500, e)).build();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (Throwable e) {
                // What to do????
            }
        }
    }

  /*
   * Input: ViewName, RID
   * Output: JSON text returned from Query if successful, otherwise return failure code
   * Query: This depends on PropertiesQuery of the Selected View
   * Process: Replace ${RID} with given RID in the given ViewName PropertiesQuery and execute the query
   */

    @POST
    @Path("/Properties")
    public Response Properties(@Context HttpServletRequest request, String jsonBody) throws InterruptedException {
        Connection conn = null;
        try {
            String qry = "select @this.toJSON() as jsonrec from (select PropertiesQuery.commands as ViewQueryCmds from KamanjaViews where ViewName = '${ViewName}')";
            conn = getDbConnection((Properties) context.getAttribute("prop"));
            try {
                String retStr = getViewResults(conn, qry, jsonBody);
                return Response.status(200).entity(retStr).build();
            } catch (Exception e) {
                return Response.status(500).entity(prepareErrorString(500, e)).build();
            }
        } catch (Throwable e) {
            return Response.status(500).entity(prepareErrorString(500, e)).build();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (Throwable e) {
                conn = null;
            }
        }
    }
}

