package com.vmware.util;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadUtils {

    private static Logger log = LoggerFactory.getLogger(ThreadUtils.class);

    public static long sleep(long amount, TimeUnit timeUnit) {
        try {
            long sleepTime = timeUnit.toMillis(amount);
            Thread.sleep(sleepTime);
            return sleepTime;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void waitForCallable(Callable<Boolean> condition, long totalTimeToWait, TimeUnit timeUnit, String reason) {
        long totalTimeToWaitInSeconds = timeUnit.toSeconds(totalTimeToWait);
        waitForCallable(condition, totalTimeToWaitInSeconds, determineRetryWaitPeriod(totalTimeToWaitInSeconds), reason);
    }

    public static void waitForCallable(Callable<Boolean> condition, long totalTimeToWaitInSeconds, long timeBetweenRetriesInSeconds, String reason) {
        Date startTime = new Date();
        long elapsedTimeInSeconds = 0;
        try {
            boolean callableSucceeded = condition.call();
            while (!callableSucceeded && elapsedTimeInSeconds < totalTimeToWaitInSeconds) {
                ThreadUtils.sleep(timeBetweenRetriesInSeconds, TimeUnit.SECONDS);
                elapsedTimeInSeconds = determineElapsedTime(startTime);
                log.debug("Retrying after {} seconds", elapsedTimeInSeconds);
                callableSucceeded = condition.call();
            }
            if (!callableSucceeded) {
                throw new RuntimeException("Timed out after " + elapsedTimeInSeconds + " seconds. " + reason);
            } else {
                log.info("Completed after {} seconds", elapsedTimeInSeconds);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static long determineElapsedTime(Date startTime) {
        long elapsedTimeInSeconds;
        long elapsedTimeInMilliseconds = System.currentTimeMillis() - startTime.getTime();
        elapsedTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeInMilliseconds);
        return elapsedTimeInSeconds;
    }

    private static long determineRetryWaitPeriod(long totalSeconds) {
        long timeBetweenRetries = 60;
        if (totalSeconds <= 10) {
            timeBetweenRetries = 2;
        } else if (totalSeconds <= 20) {
            timeBetweenRetries = 3;
        } else if (totalSeconds <= 30) {
            timeBetweenRetries = 5;
        } else if (totalSeconds <= 120) {
            timeBetweenRetries = 30;
        }
        return timeBetweenRetries;
    }
}
