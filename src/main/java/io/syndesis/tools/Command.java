package io.syndesis.tools;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;

public interface Command {

    void execute(String repo, EventType eventType, String payload, GitHub github, JiraRestClient jira, Logger logger);
}
