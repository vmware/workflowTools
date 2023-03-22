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
        } catch (RuntimeException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw e;
            }
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
            return timeUnit.convert(elapsedTime(), TimeUnit.MILLISECONDS);
        }
    }
}
