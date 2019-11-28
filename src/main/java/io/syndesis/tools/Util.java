package io.syndesis.tools;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import okhttp3.*;
import org.kohsuke.github.GitHub;
import sun.misc.BASE64Encoder;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Util {

    final static String USER = System.getenv("JIRA_USER");
    final static String PASS = System.getenv("JIRA_PASS");

    public final static Locale LOCALE = Locale.UK;
    public final static DateFormat DF = new SimpleDateFormat("yyyy-MM-dd", LOCALE);

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

    public static List<Week> buildCalendar(String startDate) throws Exception {

        LinkedList<Week> weeks = new LinkedList<>();

        Calendar releaseStart = new GregorianCalendar(LOCALE);
        releaseStart.setTime(DF.parse(startDate));

        Calendar cal = new GregorianCalendar(LOCALE);

        cal.setTime(new Date());
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.add(Calendar.WEEK_OF_MONTH, 1);

        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());

        for (int i = 0; i < 15; i++) {

            if(cal.before(releaseStart))
                break;

            cal.add(Calendar.WEEK_OF_MONTH, -1);

            Calendar tmp = (Calendar) cal.clone();
            tmp.add(Calendar.DAY_OF_WEEK, 7);
            weeks.add(0, new Week(cal.getTime(), tmp.getTime()));
        }

        return weeks;
    }



}
