package io.syndesis.tools;


import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class JiraBot {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel",
                System.getProperty("logLevel", "info"));
    }

    static Logger LOG = LoggerFactory.getLogger(JiraBot.class);

    public static void main(String[] args) throws Exception {

        final String user = System.getProperty("JIRA_USER");
        final String pass = System.getProperty("JIRA_PASS");

        if(null==user || null==pass) {
            LOG.error("Missing username and password properties, exiting.");
            System.exit(-1);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new WebHookHandler());
        server.setExecutor(null); // creates a default executor
        server.start();

        LOG.info("Server started at port 8080 ...");
    }


}