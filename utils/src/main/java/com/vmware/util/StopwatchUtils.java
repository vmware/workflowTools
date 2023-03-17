package com.vmware.util;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class StopwatchUtils {

    public static Stopwatch start() {
        return new Stopwatch();
    }

    public static long timeRunnable(Runnable callable) {
        Stopwatch stopwatch = start();
        try {
            callable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return stopwatch.elapsedTime(TimeUnit.MILLISECONDS);
    }

    public static class Stopwatch {
        private Date startDate;

        public Stopwatch() {
            this.startDate = new Date();
        }

        public long elapsedTime() {
            return System.currentTimeMillis() - startDate.getTime();
        }

        public long elapsedTime(TimeUnit timeUnit) {
            return TimeUnit.MILLISECONDS.convert(elapsedTime(), timeUnit);
        }
    }
}
