package io.syndesis.tools;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import io.syndesis.tools.cmd.Command;
import io.syndesis.tools.cmd.PullRequestClosed;
import io.syndesis.tools.cmd.PullRequestOpened;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class PullRequestHandler implements EventHandler {

    static final Map<EventType, Command> commands = new EnumMap<EventType, Command>(EventType.class) {{
        put(EventType.PULL_REQUEST_OPENED, new PullRequestOpened());
        put(EventType.PULL_REQUEST_REOPENED, new PullRequestOpened());
        put(EventType.PULL_REQUEST_CLOSED, new PullRequestClosed());
    }};

    static Logger LOG = LoggerFactory.getLogger(WebHookHandler.class);

    private final GitHub github;

    public PullRequestHandler() {
        this.github = Util.createGithubClient();
    }

    @Override
    public void handle(RequestHandle request, ResponseHandle response, String event) {

        Object document = Configuration.defaultConfiguration()
                .jsonProvider()
                .parse(request.getPayload());

        String action = JsonPath.read(document, "$.action");
        EventType eventType = EventType.UNKNOWN;
        try {
            eventType = EventType.valueOf((event + "_" + action).toUpperCase());
        } catch (IllegalArgumentException e) {
            //
        }

        String repo = JsonPath.read(document, "$.repository.full_name");

        JiraRestClient jira = Util.createJiraClient();
        try {
            LOG.info("Received EventType: {} from repo {}", eventType, repo);
            Command command = commands.getOrDefault(eventType, new Command() {
                @Override
                public void execute(String repo, EventType eventType, Object jsonDocument, GitHub github, JiraRestClient jira, Logger logger) {
                    LOG.error("No command could be found be found for event `{}` and action `{}`", event, action);
                    response.setStatus(200);
                    response.setMessage("Unknown event type: "+ eventType);
                }
            });

            command.execute(repo, eventType, document, github, jira, LOG);

        } catch (RuntimeException e) {
            response.setStatus(500);
            response.setMessage("Failed to process "+ eventType);
            LOG.error("Failed to process {}: {}", eventType, e.getMessage());
        } finally {
            try {
                jira.close();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
    }
}
