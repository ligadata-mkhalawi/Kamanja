package com.ligadata.listener;

import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.ligadata.util.InitProp;

/**
 * Application Lifecycle Listener implementation class AppContextListener
 */
public class AppContextListener implements ServletContextListener {
    /**
     * Default constructor.
     */
    public AppContextListener() {
        // TODO Auto-generated constructor stub
    }

    //    @Override
    public void contextInitialized(ServletContextEvent sce) {

        InitProp inti = new InitProp();
        System.out.println(sce.getServletContext().getRealPath(System.getProperty("file.separator")) + System.getProperty("file.separator") + sce.getServletContext().getInitParameter("PropFile"));

        InitProp.setConfFilePath(sce.getServletContext().getRealPath(System.getProperty("file.separator")) + System.getProperty("file.separator") + sce.getServletContext().getInitParameter("PropFile"));
        Properties prop = inti.properties();

        sce.getServletContext().setAttribute("prop", prop);

    }

    //    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // TODO Auto-generated method stub

    }

}
