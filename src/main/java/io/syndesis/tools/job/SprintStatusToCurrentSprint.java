package io.syndesis.tools.job;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.syndesis.tools.Util;

import org.joda.time.Instant;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SprintStatusToCurrentSprint implements Job {

    static Logger LOG = LoggerFactory.getLogger(SprintStatusToCurrentSprint.class);
    public static final MediaType APPLICATION_JSON
            = MediaType.parse("application/json");

    private static final String JQL = "project = ENTESB AND status in (\"Sprint Backlog\",\"Validation Failed\", \"In Review\", \"In Development\") and (sprint is EMPTY OR sprint not in openSprints())";

    @Override
    public void execute(JobExecutionContext jobExecutionContext)
            throws JobExecutionException {


        // Development Service board: 4934
        // https://issues.jboss.org/rest/agile/1.0/board?projectKeyOrId=ENTESB
        // https://issues.jboss.org/rest/agile/1.0/board/4934/sprint

        OkHttpClient okHttpClient = Util.createAuthenticatedClient();

        Request request = new Request.Builder()
            .url("https://issues.jboss.org/rest/agile/1.0/board/4934/sprint")
            .get()
            .build();

        try (Response fetchResponse = okHttpClient.newCall(request).execute()) {
            Object document = Configuration.defaultConfiguration()
                    .jsonProvider()
                    .parse(fetchResponse.body().string());
            List<Object> sprints = JsonPath.read(document, "$..values[?(@.startDate)]");

            Calendar cur = Calendar.getInstance();
            cur.setTime(new Date());

            Integer activeSprintId = null;
            for(Object sprint : sprints) {

                String name = JsonPath.read(sprint, "$.name");
                Integer id = JsonPath.read(sprint, "$.id");
                String startDate = JsonPath.read(sprint, "$.startDate");
                String endDateDate = JsonPath.read(sprint, "$.endDate");

                Calendar cal1 = Calendar.getInstance();
                cal1.setTime(Instant.parse(startDate).toDate());
                Calendar cal2 = Calendar.getInstance();
                cal2.setTime(Instant.parse(endDateDate).toDate());

                if(cur.after(cal1) && cur.before(cal2)) {
                    LOG.info("Active sprint {}", name);
                    activeSprintId = id;
                    break;
                }
            }

            if (activeSprintId!=null) {

                final SearchResult result;
                try (JiraRestClient jiraClient = Util.createJiraClient()) {
                    result = jiraClient.getSearchClient().searchJql(JQL).claim();
                }

                if (result.getTotal()==0) {
                    LOG.info("No inconsistencies found ...");
                }

                for(Issue issue : result.getIssues()) {
                    LOG.info("Found issue in inconsistent state: {} ", issue.getKey());

                    // see https://docs.atlassian.com/jira-software/REST/7.0.4/#agile/1.0/sprint-moveIssuesToSprint

                    Request updateSprint = new Request.Builder()
                            .url("https://issues.jboss.org/rest/agile/1.0/sprint/"+activeSprintId+"/issue")
                            .method("POST",
                                    RequestBody.create(
                                        APPLICATION_JSON,
                                "{\"issues\": [\""+issue.getKey()   +"\"]}")
                                    )
                            .build();
                    try (Response updateResponse = okHttpClient.newCall(updateSprint).execute()) {
                        if(updateResponse.code() >= 200 && updateResponse.code() < 300) {
                            LOG.info("Added {} to current sprint: {} ", issue.getKey(), activeSprintId);
                        } else {
                            LOG.error("Failed ot modify sprint contents: HTTP {}", updateResponse.code());
                        }
                    }
                }

            } else {
                LOG.warn("No active sprint could be identified");
            }


        } catch (Exception e) {
            throw new JobExecutionException(e.getMessage());
        }
    }
}
