package com.vmware.util;

import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DateUtils {

    public static Date parseDate(String dateValue) {
        Date date = parseDate(dateValue, "EEE MMM dd HH:mm:ss yyyy zzzzz", false);
        if (date == null) {
            date = parseDate(dateValue, "EEE MMM dd HH:mm:ss yyyy", true);
        }
        return date;
    }

    public static long workWeekMinutesBetween(Date date1, Date date2) {
        long duration = minutesBetween(date1, date2);
        long weekendDuration = weekendMinutesBetween(date1, date2);
        return duration - weekendDuration;
    }

    public static long minutesBetween(Date date1, Date date2) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return TimeUnit.MINUTES.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    public static long weekendMinutesBetween(Date date1, Date date2) {
        Calendar date1Calendar = Calendar.getInstance();
        date1Calendar.setTime(date1);

        long weekendMinutes = 0;
        date1Calendar.add(Calendar.DAY_OF_MONTH, 1);
        while (date2.after(date1Calendar.getTime())) {
            if (date1Calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                    || date1Calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                weekendMinutes += TimeUnit.DAYS.toMinutes(1);
            }
            date1Calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return weekendMinutes;
    }

    private static Date parseDate(String dateValue, String pattern, boolean logError) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        try {
            return dateFormat.parse(dateValue);
        } catch (ParseException e) {
            if (logError) {
                LoggerFactory.getLogger(DateUtils.class)
                        .debug("Failed to parse {} from {}: {}", dateValue, pattern, e.getMessage());
            } else {
                LoggerFactory.getLogger(DateUtils.class)
                        .trace("Failed to parse {} from {}: {}", dateValue, pattern, e.getMessage());
            }
            return null;
        }
    }
}
