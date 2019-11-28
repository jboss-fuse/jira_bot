package io.syndesis.tools.job;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import io.syndesis.tools.JiraBot;
import io.syndesis.tools.Util;
import io.syndesis.tools.Week;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class PrepareReleaseReportData implements Job {

    static Logger LOG = LoggerFactory.getLogger(PrepareReleaseReportData.class);
    static String DEFAULT_WEEL_JQL = " DURING (startOfWeek(), endOfWeek())";
    static String RELEASE_START_DATE = System.getenv("RELEASE_START_DATE");
    static String RELEASE_VERSION = System.getenv("RELEASE_VERSION");

    @Override
    public void execute(JobExecutionContext jobExecutionContext)
            throws JobExecutionException {

        try (JiraRestClient jiraClient = Util.createJiraClient()) {

            // build calendar
            List<Week> weeks = Util.buildCalendar(RELEASE_START_DATE);

            StringBuilder sb;
            for(ReleaseCriteria criteria : ReleaseCriteria.values()) {

                sb = new StringBuilder();
                sb.append("start").append(";");
                sb.append("week").append(";");
                sb.append("issues").append("\n");

                LOG.info("Criteria: {}", criteria.name);
                LOG.info("Query: {}", criteria.query + DEFAULT_WEEL_JQL);

                TimeSeries series = new TimeSeries("Number of Issues");
                for (Week week : weeks) {
                    LOG.info("Calculating week {}", week);

                    String query = criteria.query
                            + " DURING (\"" + week.start() + "\", \"" + week.end() + "\")"
                            + " AND \"Target Release\" = \""+RELEASE_VERSION+"\"";

                    LOG.info("{}", query);
                    SearchResult results = jiraClient.getSearchClient().searchJql(query).claim();
                    LOG.info("Number of issues: {}", results.getTotal());

                    sb.append(week.start()).append(";");
                    sb.append(week.end()).append(";");
                    sb.append(results.getTotal()).append("\n");

                    series.add( new org.jfree.data.time.Week(week.endDate()), results.getTotal());
                }

                File file = new File(JiraBot.tempDirectory, criteria.filename+".txt");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(sb.toString());
                }

                // chart generation
                TimeSeriesCollection dataset = new TimeSeriesCollection();
                dataset.addSeries(series);
                JFreeChart chart = ChartFactory.createTimeSeriesChart(
                        criteria.name,
                        "Week",
                        "Count",
                        dataset,
                        true, false, false);

                int width = 640;    /* Width of the image */
                int height = 480;   /* Height of the image */
                File lineChart = new File( JiraBot.tempDirectory, criteria.filename+".jpg" );
                ChartUtilities.saveChartAsJPEG(lineChart ,chart, width ,height);
            }

            LOG.info("Done generating release criteria report");

        } catch(Exception e)  {
            e.printStackTrace();
            throw new JobExecutionException(e.getMessage());
        }
    }


}
