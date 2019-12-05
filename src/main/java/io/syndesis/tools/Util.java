package io.syndesis.tools;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import org.kohsuke.github.GitHub;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class Util {

    final static String USER = System.getenv("JIRA_USER");
    final static String PASS = System.getenv("JIRA_PASS");

    public static HttpURLConnection createAuthenticatedUrlConnection(URL url, String requestMethod)
            throws IOException {
        String userpassword = USER + ":" + PASS;
        String encodedAuthorization = Base64.getEncoder().encodeToString(userpassword.getBytes());

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(requestMethod);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Basic "+ encodedAuthorization);

        return con;
    }

    public static OkHttpClient createAuthenticatedClient() {
        OkHttpClient httpClient = new OkHttpClient.Builder().authenticator(new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                String credential = Credentials.basic(USER, PASS);
                return response.request().newBuilder().header("Authorization", credential).build();
            }
        }).build();
        return httpClient;
    }

    public static File createTempDirectory(){
        try {
            final File temp;

            temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

            if(!(temp.delete()))
            {
                throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
            }

            if(!(temp.mkdir()))
            {
                throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
            }

            return (temp);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp dir: "+ e.getMessage());
        }
    }

    public static JiraRestClient createJiraClient() {
        // clients
        JiraRestClient jira = new AsynchronousJiraRestClientFactory()
                .createWithBasicHttpAuthentication(
                        URI.create("http://issues.redhat.com"),
                        USER, PASS
                );

        return jira;
    }

    public static GitHub createGithubClient() {
        try {
            GitHub github = GitHub.connect();
            return github;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Github client: "+ e.getMessage());
        }
    }

    public static List<Week> buildCalendar(String startDate) throws Exception {
        final LocalDate releaseStart = LocalDate.parse(startDate); // Mon Jul 29 00:00:00 CEST 2019

        final LocalDate nextMonday = LocalDate.now().plusDays(7).with(WeekFields.ISO.dayOfWeek(), 1); // Mon Dec 02 01:15:10 CET 2019

        LocalDate weekEnd = nextMonday;

        LinkedList<Week> weeks = new LinkedList<>();
        for (int i = 0; i < 15 && weekEnd.isAfter(releaseStart); i++) {
            LocalDate weekStart = weekEnd.minusDays(7);
            weeks.add(0, new Week(weekStart, weekEnd));
            weekEnd = weekStart;
        }

        return weeks;
    }



}
