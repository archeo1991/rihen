package cn.archeo.rihen;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Milestones {
    private static final int[] SPECIAL_DAYS = {
            100, 365, 500, 777, 1000, 1111, 1234, 1500, 2000, 2222, 3000, 3333,
            4000, 4444, 5000, 5555, 6000, 6666, 7000, 7777, 8000, 8888, 9000,
            9999, 10000, 11111, 12345, 15000, 20000, 22222, 25000, 30000
    };

    private Milestones() {
    }

    static long livedDays(LocalDate birthday, LocalDate today) {
        return ChronoUnit.DAYS.between(birthday, today);
    }

    static List<Integer> upcomingSpecialDays(LocalDate birthday, LocalDate today, int limit) {
        long lived = livedDays(birthday, today);
        List<Integer> result = new ArrayList<>();
        for (int day : SPECIAL_DAYS) {
            if (day > lived) {
                result.add(day);
                if (result.size() == limit) break;
            }
        }
        return Collections.unmodifiableList(result);
    }

    static LocalDate dateForMilestone(LocalDate birthday, int milestoneDay) {
        return birthday.plusDays(milestoneDay);
    }
}
