package io.syndesis.tools;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.syndesis.tools.cmd.PullRequestOpenedCmd;
import org.apache.http.Consts;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.EnumMap;
import java.util.Map;

public class WebHookHandler implements HttpHandler {

    static Logger LOG = LoggerFactory.getLogger(WebHookHandler.class);
    private final GitHub github;

    Map<EventType, Command> commands = new EnumMap<EventType, Command>(EventType.class) {{
        put(EventType.PULL_REQUEST_OPENED, new PullRequestOpenedCmd());
        put(EventType.PULL_REQUEST_REOPENED, new PullRequestOpenedCmd());
    }};

    public WebHookHandler() {
        this.github = Util.createGithubClient();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // request
        StringBuilder body = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(
                exchange.getRequestBody(), Consts.UTF_8)) {
            char[] buffer = new char[256];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                body.append(buffer, 0, read);
            }
        }

        Object document = Configuration.defaultConfiguration()
                .jsonProvider()
                .parse(body.toString()
                );

        String action = JsonPath.read(document, "$.action");
        String typeHeader = exchange.getRequestHeaders().getFirst("X-GitHub-Event");
        EventType eventType = EventType.valueOf((typeHeader + "_" + action).toUpperCase());
        String repo = JsonPath.read(document, "$.repository.full_name");

        int status = 200;
        JiraRestClient jira = Util.createJiraClient();
        try {
            LOG.info("Received EventType: {} from repo {}", eventType, repo);
            Command command = commands.getOrDefault(eventType, new Command() {
                @Override
                public void execute(EventType eventType, String payload, GitHub github, JiraRestClient jira, Logger logger) {
                    LOG.error("Unknown event type: {}. No handler could be found.", eventType);
                }
            });

            long start = System.currentTimeMillis();
            command.execute(eventType, body.toString(), github, jira, LOG);
            long end = System.currentTimeMillis();

            LOG.info("Processing time: {} ms", end-start);

        } catch (RuntimeException e) {
            status = 500;
            LOG.error("Failed to process {}: {}", eventType, e.getMessage());
        } finally {
            jira.close();

        }

        // response
        String response = "ok";
        exchange.sendResponseHeaders(status, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
