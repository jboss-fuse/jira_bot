package io.syndesis.tools;


import com.sun.net.httpserver.HttpServer;
import io.syndesis.tools.job.PrepareReleaseReportData;
import io.syndesis.tools.job.SprintStatusToCurrentSprint;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class JiraBot {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel",
                System.getProperty("logLevel", "info"));
    }

    static Logger LOG = LoggerFactory.getLogger(JiraBot.class);

    public static File tempDirectory = Util.createTempDirectory();

    public static void main(String[] args) throws Exception {

        final String user = System.getenv("JIRA_USER");
        final String pass = System.getenv("JIRA_PASS");

        if(null==user || null==pass) {
            LOG.error("Missing username and password properties, exiting.");
            System.exit(-1);
        }


        LOG.info("Using data dir: {}", tempDirectory.getAbsolutePath());

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new WebHookHandler());
        server.createContext(DirectoryHandler.WEBCONTEXT, new DirectoryHandler(tempDirectory.getAbsolutePath()));
        server.setExecutor(null); // creates a default executor
        server.start();

        LOG.info("Server started at port 8080 ...");

        // Grab the Scheduler instance from the Factory
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        // schedule jobs
        //scheduleSprintCleanup(scheduler);
        scheduleReleaseCriteria(scheduler);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                scheduler.shutdown();
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }));

    }

    private static void scheduleSprintCleanup(Scheduler scheduler) throws Exception {
        // define the job and tie it to our HelloJob class
        JobDetail job = newJob(SprintStatusToCurrentSprint.class)
                .withIdentity("Sprint Cleanup", "jira")
                .build();

        // Trigger the job to run now, and then repeat every 40 seconds
        Trigger trigger = newTrigger()
                .withIdentity("15min", "jira")
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInMinutes(15)
                        .repeatForever())
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
    }

    private static void scheduleReleaseCriteria(Scheduler scheduler) throws Exception {
        // define the job and tie it to our HelloJob class
        JobDetail job = newJob(PrepareReleaseReportData.class)
                .withIdentity("Release Criteria Report", "jira")
                .build();

        // Trigger the job to run now, and then repeat every 40 seconds
        Trigger trigger = newTrigger()
                .withIdentity("daily", "jira")
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInHours(24)
                        .repeatForever())
                .build();

        // Tell quartz to schedule the job using our trigger
        scheduler.scheduleJob(job, trigger);
    }

}