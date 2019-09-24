package io.syndesis.tools.job;

public enum ReleaseCriteria {


    MAJOR_ENHANCEMENTS("enhancements", "# of Blocker, Critical, Major enhancements not Done",
            "project = ENTESB AND Type IN (Enhancement) AND priority IN (Blocker,Critical,Major) AND (labels is empty or (labels is not empty and labels not in (QA_IGNORE))) and status was not in (Done, Closed, \"Triage Backlog\", \"Planning  Backlog\", \"Productization Backlog\", \"Validation Backlog\")\n"),

    CUSTOMER_BUGS("customer", "# of Customer bugs targeted for the release not Done",
            "project = ENTESB AND \"GSS Priority\"  is not EMPTY and status was not in (Done, Closed, \"Triage Backlog\", \"Planning  Backlog\", \"Productization Backlog\", \"Validation Backlog\")"),

    UNWAIVED("unwaived", "# of Minor, Optional, Trivial unwaived enhancements not Done",
            "project = ENTESB AND Type IN (Enhancement) AND priority IN (Minor,Optional,Trivial) and status was not in (Done, Closed, \"Triage Backlog\", \"Planning  Backlog\", \"Productization Backlog\", \"Validation Backlog\")"),

    REGRESSIONS("regressions", "# of regressions not Done",
        "project = ENTESB AND Type IN (Bug) AND labels IN (regression, Regression) and status was not in (Done, Closed, \"Productization Backlog\", \"Validation Backlog\")"),

    CRITICAL("critical", "# of Critical issues not Done",
            "project = ENTESB AND Type IN (Bug) AND priority = Critical and status was not in (Done, Closed, \"Triage Backlog\", \"Planning  Backlog\", \"Productization Backlog\", \"Validation Backlog\")"),

    BLOCKERS("blockers", "# of Blockers not Done",
            "project = ENTESB AND Type IN (Bug) AND priority = Blocker and status was not in (Done, Closed, \"Triage Backlog\", \"Planning  Backlog\", \"Productization Backlog\", \"Validation Backlog\")");


    ReleaseCriteria(String file, String name, String query) {
        this.query = query;
        this.name = name;
        this.filename = file;
    }

    String name;
    String query;
    String filename;

}
