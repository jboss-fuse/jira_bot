/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.syndesis.tools.cmd;

import io.syndesis.tools.EventType;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.rest.client.api.JiraRestClient;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CommandTest {

    final Logger logger = LoggerFactory.getLogger(CommandTest.class);
    final GitHub github = mock(GitHub.class);
    final JiraRestClient jira = mock(JiraRestClient.class);

    @Test
    public void shouldExecuteCombinedCommands() {
        final Command command1 = mock(Command.class);
        final Command command2 = mock(Command.class);

        final Command combined = Command.combined(command1, command2);

        combined.execute("repo", EventType.UNKNOWN, "json", github, jira, logger);

        verify(command1).execute("repo", EventType.UNKNOWN, "json", github, jira, logger);
        verify(command2).execute("repo", EventType.UNKNOWN, "json", github, jira, logger);
    }

    @Test
    public void shouldExecuteAllCommandsRegardlessOfIndividualFailures() {
        final Command command1 = mock(Command.class);
        final Command command2 = mock(Command.class);

        final Command combined = Command.combined(command1, command2);

        doThrow(new IllegalStateException("should be logged and not propagated")).when(command1).execute("repo", EventType.UNKNOWN, "json", github, jira,
            logger);

        combined.execute("repo", EventType.UNKNOWN, "json", github, jira, logger);

        verify(command1).execute("repo", EventType.UNKNOWN, "json", github, jira, logger);
        verify(command2).execute("repo", EventType.UNKNOWN, "json", github, jira, logger);
    }
}
