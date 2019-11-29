package io.syndesis.tools;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class Week {
    LocalDate start;
    LocalDate end;

    public Week(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    public String start() {
        return start.toString();
    }

    public String end() {
        return end.toString();
    }

    public LocalDate startDate() {
        return start;
    }

    public LocalDate endDate() {
        return end;
    }

    @Override
    public String toString() {
        return start() + " ... " + end();
    }

    public Date endDateVintage() {
        return Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
