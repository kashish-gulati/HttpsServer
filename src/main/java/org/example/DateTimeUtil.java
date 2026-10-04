package org.example;

import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.util.HashMap;
import java.util.Map;

import static java.time.temporal.ChronoField.*;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;
import static java.util.Map.entry;

public final class DateTimeUtil {

    private static final Map<Long,String> DAYS_OF_WEEK = Map.of(
            1L, "Mon",
            2L, "Tue",
            3L, "Wed",
            4L, "Thu",
            5L, "Fri",
            6L, "Sat",
            7L, "Sun"
    );

    private static final Map<Long,String> MONTHS_OF_YEAR = Map.ofEntries(
            entry(1L, "Jan"),
            entry(2L, "Feb"),
            entry(3L, "Mar"),
            entry(4L, "Apr"),
            entry(5L, "May"),
            entry(6L, "Jun"),
            entry(7L, "Jul"),
            entry(8L, "Aug"),
            entry(9L, "Sep"),
            entry(10L, "Oct"),
            entry(11L, "Nov"),
            entry(12L, "Dec"));

    public static final DateTimeFormatter RFC_1123_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .parseLenient()
                .optionalStart()
                .appendText(DAY_OF_WEEK, DAYS_OF_WEEK)
                .appendLiteral(", ")
                .optionalEnd()
                .appendValue(DAY_OF_MONTH, 2, 2, SignStyle.NOT_NEGATIVE)
                .appendLiteral(' ')
                .appendText(MONTH_OF_YEAR, MONTHS_OF_YEAR)
                .appendLiteral(' ')
                .appendValue(YEAR, 4)  // 2 digit year not handled
                .appendLiteral(' ')
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .optionalEnd()
                .appendLiteral(' ')
                .appendOffset("+HHMM", "GMT")  // should handle UT/Z/EST/EDT/CST/CDT/MST/MDT/PST/MDT
            .toFormatter().withResolverStyle(ResolverStyle.SMART)
            .withChronology(IsoChronology.INSTANCE);
}
