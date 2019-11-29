package io.syndesis.tools.cmd;

import java.util.stream.Stream;

import io.syndesis.tools.EventType;

import org.kohsuke.github.GitHub;
import org.slf4j.Logger;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.machinezoo.noexception.Exceptions;

@FunctionalInterface
public interface Command {

    void execute(String repo, EventType eventType, Object jsonDocument, GitHub github, JiraRestClient jira, Logger logger);

    /**
     * Creates a combined command. Each command in provided will be
     * invoked independently of other commands specified. Any exceptions
     * that might occur while executing one of the commands will be
     * logged to the provided logger and will not prevent other commands
     * from executing.
     *
     * @param commands commands to combine
     */
    static Command combined(Command... commands) {
        return (repo, eventType, jsonDocument, github, jira, logger) -> Stream.of(commands)
            .parallel()
            .forEach(Exceptions.log(logger)
                .consumer(c -> c.execute(repo, eventType, jsonDocument, github, jira, logger)));
    }
}
