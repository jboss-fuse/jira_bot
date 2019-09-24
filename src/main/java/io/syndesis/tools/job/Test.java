package io.syndesis.tools.job;

import io.syndesis.tools.Util;
import io.syndesis.tools.Week;

import java.util.List;

public class Test {

    public static void main (String[] args) throws Exception {
        List<Week> weeks = Util.buildCalendar("2019-07-29");

        for(Week w : weeks) {
            System.out.println(w);
        }
    }
}
