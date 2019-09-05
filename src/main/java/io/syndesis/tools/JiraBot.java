package io.syndesis.tools;


import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Project;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.sun.net.httpserver.HttpServer;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.URI;

public class JiraBot {

    static Logger LOG = LoggerFactory.getLogger(JiraBot.class);

    public static final String JIRA_PROJECT = "ENTESB";

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
        server.createContext("/", new WebHookHandler(jira,github));
        server.setExecutor(null); // creates a default executor
        server.start();

        LOG.info("Server started at port 8080 ...");
    }
}