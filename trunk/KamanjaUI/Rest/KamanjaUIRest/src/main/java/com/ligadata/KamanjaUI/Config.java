package com.ligadata.KamanjaUI;

import java.util.Properties;

import javax.servlet.ServletContext;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.ligadata.util.InitProp;


@Path("/config")
public class Config {

    @Context
    private ServletContext context;

    @POST
    @Path("/reload")
    public Response reload() {

        InitProp inti = new InitProp();
        InitProp.setConfFilePath(context.getRealPath(System.getProperty("file.separator")) + System.getProperty("file.separator") + context.getInitParameter("PropFile"));
        Properties prop = inti.properties();

        context.setAttribute("prop", prop);

        return Response.status(200).entity("Config File Reloaded").build();

    }

}
