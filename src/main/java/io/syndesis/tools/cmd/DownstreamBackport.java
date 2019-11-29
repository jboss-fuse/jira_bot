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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import io.syndesis.tools.EventType;
import io.syndesis.tools.IssueKey;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.jayway.jsonpath.JsonPath;

public class DownstreamBackport implements Command {

    private static GitHub GITHUB;

    private static final String JIRA_BASE = "https://issues.jboss.org/browse/";

    static {
        final String username = System.getenv("github_login");
        final String password = System.getenv("github_password");

        final UsernamePasswordCredentialsProvider jboss = new UsernamePasswordCredentialsProvider(username, password);
        CredentialsProvider.setDefault(jboss);

        try {
            GITHUB = GitHub.connect(username, password);
        } catch (final IOException e) {
            throw new UncheckedIOException("GitHub Java API threw a IOException from code that doesn't throw it, hmm", e);
        }
    }

    private final String downstream;

    private GHRepository repository;

    private final URIish upstream;

    public DownstreamBackport() {
        this("https://github.com/syndesisio/syndesis.git", "https://github.com/jboss-fuse/syndesis.git");
    }

    public DownstreamBackport(final String upstream, final String downstream) {
        try {
            this.upstream = new URIish(upstream);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("upstream", e);
        }
        this.downstream = downstream;

        final String repositoryName = downstream.replaceFirst("^https://github.com/(.+)\\.git", "$1");
        try {
            repository = GITHUB.getRepository(repositoryName);
        } catch (final IOException e) {
            throw new IllegalArgumentException("downstream GitHub repository: " + repositoryName, e);
        }
    }

    @Override
    public void execute(final String repo, final EventType eventType, final Object document, final GitHub github, final JiraRestClient jira,
        final Logger logger) {

        final Boolean pullRequestIsMerged = JsonPath.read(document, "$.pull_request.merged");

        if (Boolean.FALSE.equals(pullRequestIsMerged)) {
            return;
        }

        final List<IssueKey> issueKeys = parseIssueKeys(document);

        if (issueKeys.isEmpty()) {
            logger.info("No issue keys found, no automatic backport pull request will be created");
            return;
        }

        // TODO should we check in JIRA if the PR needs backporting?

        final String base = "master"; // TODO replace with correct product
        // branch based on the JIRA issue and the mapping of product versions to
        // product branches

        final String issues = issueKeys.stream()
            .map(IssueKey::getFullyQualifiedIssueKey)
            .collect(Collectors.joining(", "));

        final File workdir;
        try {
            workdir = Files.createTempDirectory("backport").toFile();
            workdir.deleteOnExit();
        } catch (final IOException e) {
            throw new UncheckedIOException("Unable to create work directory", e);
        }

        final String pr = JsonPath.read(document, "$.number");
        final String backportBranchName = "backport-" + pr + "-to-" + base;
        try (Git git = Git.cloneRepository()
            .setURI(downstream)
            .setDirectory(workdir)
            .setBranch(base)
            .setRemote("downstream")
            .call()) {

            git.remoteAdd()
                .setName("upstream")
                .setUri(upstream)
                .call();

            git.fetch()
                .setRemote("upstream")
                .call();

            git.branchCreate()
                .setName(backportBranchName)
                .setStartPoint(base)
                .call();

            git.checkout()
                .setName(backportBranchName)
                .call();

            final String mergeId = JsonPath.read(document, "$.pull_request.merge_commit_sha");
            git.cherryPick()
                .setMainlineParentNumber(1) // handle merge commits
                .include(ObjectId.fromString(mergeId))
                .call();

            git.push()
                .setRemote("downstream")
                .call();
        } catch (final GitAPIException e) {
            // TODO notify on JIRA
            throw new IllegalStateException("Unable to create automatic backport pull request", e);
        } finally {
            workdir.delete();
        }

        // TODO refine?
        final String upstreamTitle = JsonPath.read(document, "$.pull_request.title");
        final String downstreamTitle = "[Backport " + base + "] " + issues + " " + upstreamTitle;

        // TODO refine?
        final String body = "Automatic backport of syndesisio/syndesis#" + pr + "\n" + issueKeys.stream()
            .map(IssueKey::getFullyQualifiedIssueKey)
            .collect(Collectors.joining("\n", "Ref. " + JIRA_BASE, ""));

        try {
            repository.createPullRequest(downstreamTitle, backportBranchName, base, body);
        } catch (final IOException e) {
            // TODO notify on JIRA
            throw new UncheckedIOException("Unable to create downstream pull request", e);
        }
    }

    // TODO move to IssueKey?
    private static List<IssueKey> parseIssueKeys(final Object document) {
        final String title = JsonPath.read(document, "$.pull_request.title");
        final List<IssueKey> issueKeys = IssueKey.parseIssueKeys(title);
        if (!issueKeys.isEmpty()) {
            return issueKeys;
        }

        final String body = JsonPath.read(document, "$.pull_request.body");
        issueKeys.addAll(IssueKey.parseIssueKeys(body));

        return issueKeys;
    }

}
