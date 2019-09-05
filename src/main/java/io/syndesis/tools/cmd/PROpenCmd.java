package io.syndesis.tools.cmd;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import io.syndesis.tools.Command;
import io.syndesis.tools.EventType;
import io.syndesis.tools.IssueKey;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;

import java.util.List;

public class PROpenCmd implements Command {
    @Override
    public void execute(EventType eventType, String payload, GitHub github, JiraRestClient jira, Logger logger) {
        logger.info("Processing {}", eventType);

        Object document = Configuration.defaultConfiguration()
                .jsonProvider()
                .parse(payload);

        // try title first
        String title = JsonPath.read(document, "$.pull_request.title");
        List<IssueKey> issueKeys = IssueKey.parseIssueKeys(title);

        if(issueKeys.isEmpty()) {
            // look at body
            String body = JsonPath.read(document, "$.pull_request.body");
            issueKeys = IssueKey.parseIssueKeys(body);
        }

        if(issueKeys.isEmpty()) {
            logger.info("No issue keys found");
            return;
        }

        // proceed with issue keys
        for(IssueKey key : issueKeys) {
            logger.info("Found {}", key.getFullyQualifiedIssueKey());
        }
    }
}
