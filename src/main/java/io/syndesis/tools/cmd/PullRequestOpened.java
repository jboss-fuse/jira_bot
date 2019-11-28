package io.syndesis.tools.cmd;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.IssueField;
import com.atlassian.jira.rest.client.api.domain.Transition;
import com.atlassian.jira.rest.client.api.domain.input.IssueInputBuilder;
import com.atlassian.jira.rest.client.api.domain.input.TransitionInput;
import com.jayway.jsonpath.JsonPath;
import io.syndesis.tools.EventType;
import io.syndesis.tools.IssueKey;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

public class PullRequestOpened implements Command {

    public static final String IN_DEVELOPMENT = "In Development";
    public static final String TO_REVIEW = "To Review";
    public static final String IN_REVIEW = "In Review";

    @Override
    public void execute(String repo, EventType eventType, Object document, GitHub github, JiraRestClient jira, Logger logger) {
        logger.info("Processing {}", eventType);

        // try title first
        String title = JsonPath.read(document, "$.pull_request.title");
        Integer pullRequestId = JsonPath.read(document, "$.pull_request.number");
        String pullRequestHref = JsonPath.read(document, "$.pull_request._links.html.href");
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
        for (IssueKey key : issueKeys) {
            logger.info("Found {}", key.getFullyQualifiedIssueKey());

            IssueRestClient issueClient = jira.getIssueClient();
            Issue issue = issueClient
                    .getIssue(key.getFullyQualifiedIssueKey())
                    .claim();

            Iterable<Transition> transitions = issueClient.getTransitions(issue).claim();

            logger.info("Current status: {}", issue.getStatus().getName());
            logger.debug("Possible transitions:");
            for (Transition t : transitions) {
                logger.debug("> {}", t.getName());
            }

            if (issue.getStatus().getName().equals(IN_DEVELOPMENT)) {
                for (Transition t : transitions) {

                    if (t.getName().equals(TO_REVIEW)) {
                        try {
                            logger.info("Moving issue to `{}`", t.getName());
                            issueClient.transition(issue, new TransitionInput(t.getId())).claim();
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to move issue: "+ e.getMessage());
                        }
                    }
                }
            } else if (issue.getStatus().getName().equals(IN_REVIEW)) {
                logger.info("Ticket {} already in correct state {}", issue.getKey(), IN_REVIEW);
            } else {
                logger.warn("Cannot auto transition {} from {} to {}"
                        , issue.getKey()
                        , issue.getStatus().getName(),
                        "In Review");

                // check if a comment about this issue already exists
                try {

                    boolean remarkExists = false;
                    GHPullRequest pullRequest = github.getRepository(repo).getPullRequest(pullRequestId);
                    String github_login = System.getenv("github_login");

                    search: {
                        for(GHIssueComment comment : pullRequest.getComments()) {

                            if(comment.getUser().getLogin().equals(github_login)) {

                                List<IssueKey> commentKeys = IssueKey.parseIssueKeys(comment.getBody());
                                for(IssueKey commentKey : commentKeys) {
                                    if(commentKey.getFullyQualifiedIssueKey().equals(issue.getKey())) {
                                        logger.info("I have already bragged about {} on {}", issue.getKey(), pullRequest.getNumber());
                                        remarkExists = true;
                                        break search;
                                    }
                                }

                            }
                        }
                    }

                    if(!remarkExists) {
                        pullRequest
                                .comment("@"+pullRequest.getUser().getLogin()+" The bot could not transition the ticket automatically, please update this Jira ticket manually: "
                                        + "https://issues.jboss.org/browse/" + issue.getKey());
                    }

                } catch (IOException e) {
                    throw new RuntimeException("Failed to leave comment: "+e.getMessage());
                }

            }

            // at least, link the PR
            IssueField pullRequestField = issue.getFieldByName("Git Pull Request");

            // append PR info
            StringBuilder pullRequests = new StringBuilder();

            if (pullRequestField.getValue() != null) {
                JSONArray jsonArray = (JSONArray) pullRequestField.getValue();
                jsonArray.put(pullRequestHref.toString());

                if (jsonArray.length() > 0) {

                    try {
                        for (int idx = 0; idx < jsonArray.length(); idx++) {
                            if (idx > 0)
                                pullRequests.append(",");
                            pullRequests.append(jsonArray.get(idx).toString());
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                } else {
                    pullRequests.append(pullRequestHref.toString());
                }
            } else {
                pullRequests.append(pullRequestHref.toString());
            }

            IssueInputBuilder issueBuilder = new IssueInputBuilder();
            issueBuilder.setFieldValue(pullRequestField.getId(), pullRequests.toString());
            issueClient.updateIssue(issue.getKey(), issueBuilder.build());
        }

    }
}
