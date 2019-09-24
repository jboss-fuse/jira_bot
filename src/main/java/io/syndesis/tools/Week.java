package io.syndesis.tools;

import java.util.Date;

public class Week {
    Date start;
    Date end;

    public Week(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    public String start() {
        return Util.DF.format(start);
    }

    public String end() {
        return Util.DF.format(end);
    }

    public Date startDate() {
        return start;
    }

    public Date endDate() {
        return end;
    }

    @Override
    public String toString() {
        return start()+" ... "+end();
    }
}
