package io.syndesis.tools;


import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.*;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import com.atlassian.util.concurrent.Promise;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class JiraBot {


    // TODO
    public static final String JIRA_PROJECT = "ENTESB";

    static Logger LOG = LoggerFactory.getLogger(JiraBot.class);

    public static void main(String[] args) throws Exception {

        final String user = System.getProperty("JIRA_USER");
        final String pass = System.getProperty("JIRA_PASS");

        if(null==user || null==pass) {
            LOG.error("Missing username and password properties, exiting.");
            System.exit(-1);
        }

        // clients
        JiraRestClient jira = new AsynchronousJiraRestClientFactory()
                .createWithBasicHttpAuthentication(
                        URI.create("http://issues.jboss.org"),
                        user, pass
                );

        Project project = jira.getProjectClient().getProject(JIRA_PROJECT)
                .claim();

        LOG.info("Connected to {} ...", project.getName());

        GitHub github = GitHub.connect();
        GHRepository repo = github.getRepository("syndesisio/syndesis");
        LOG.info("Connected to {} ... ", repo.getName());

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/github", new WebHookHandler());
        server.setExecutor(null); // creates a default executor
        server.start();

        LOG.info("Server started at port 8080 ...");
    }
}