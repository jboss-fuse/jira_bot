package io.syndesis.tools;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import okhttp3.*;
import org.kohsuke.github.GitHub;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class Util {

    final static String USER = System.getenv("JIRA_USER");
    final static String PASS = System.getenv("JIRA_PASS");

    public static HttpURLConnection createAuthenticatedUrlConnection(URL url, String requestMethod)
            throws IOException {
        BASE64Encoder enc = new BASE64Encoder();
        String userpassword = USER + ":" + PASS;
        String encodedAuthorization = enc.encode( userpassword.getBytes() );

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(requestMethod);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Basic "+ encodedAuthorization);

        return con;
    }

    public static OkHttpClient createAuthenticatedClient() {
        OkHttpClient httpClient = new OkHttpClient.Builder().authenticator(new Authenticator() {
            public Request authenticate(Route route, Response response) throws IOException {
                String credential = Credentials.basic(USER, PASS);
                return response.request().newBuilder().header("Authorization", credential).build();
            }
        }).build();
        return httpClient;
    }

    public static JiraRestClient createJiraClient() {
        // clients
        JiraRestClient jira = new AsynchronousJiraRestClientFactory()
                .createWithBasicHttpAuthentication(
                        URI.create("http://issues.jboss.org"),
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


}
