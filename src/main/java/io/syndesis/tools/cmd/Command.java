package io.syndesis.tools.cmd;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import io.syndesis.tools.EventType;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;

public interface Command {

    void execute(String repo, EventType eventType, Object jsonDocument, GitHub github, JiraRestClient jira, Logger logger);
}
