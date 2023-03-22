package com.vmware.utils;

import com.vmware.util.StopwatchUtils;
import org.junit.Before;
import org.junit.Test;

import com.vmware.util.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class TestDateUtils {

    private Calendar cal;
    private Date fridayDate;

    @Before
    public void init() {
        // Friday 16th May 2014
        cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 16);
        cal.set(Calendar.MONTH, 4);
        cal.set(Calendar.YEAR, 2014);
        fridayDate = cal.getTime();
    }

    @Test
    public void checkFridayToMonday() {
        cal.add(Calendar.DAY_OF_MONTH, 3);
        Date mondayDate = cal.getTime();

        long weekendMinutes = DateUtils.weekendMinutesBetween(fridayDate, mondayDate);
        assertEquals(TimeUnit.DAYS.toMinutes(2), weekendMinutes);
    }

    @Test
    public void checkFridayToSunday() {
        cal.add(Calendar.DAY_OF_MONTH, 2);
        cal.add(Calendar.MINUTE, 10);
        Date sundayDate = cal.getTime();

        long weekendMinutes = DateUtils.weekendMinutesBetween(fridayDate, sundayDate);
        assertEquals(TimeUnit.DAYS.toMinutes(2), weekendMinutes);
    }

    @Test
    public void checkFridayToWednesdayWeek() {
        cal.add(Calendar.DAY_OF_MONTH, 12);
        Date wednesdayDate = cal.getTime();

        long weekendMinutes = DateUtils.weekendMinutesBetween(fridayDate, wednesdayDate);
        assertEquals(TimeUnit.DAYS.toMinutes(4), weekendMinutes);
    }

    @Test
    public void testStopwatch() throws InterruptedException {
        StopwatchUtils.Stopwatch stopwatch = StopwatchUtils.start();
        Thread.sleep(1000);
        assertEquals(1, stopwatch.elapsedTime(TimeUnit.SECONDS));
    }
}
