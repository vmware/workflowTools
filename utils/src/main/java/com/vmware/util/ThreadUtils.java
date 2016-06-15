package com.vmware.util;

import com.vmware.util.exception.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {

    private static Logger log = LoggerFactory.getLogger(ThreadUtils.class);

    public static void sleep(long amount, TimeUnit timeUnit) {
        try {
            Thread.sleep(timeUnit.toMillis(amount));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sleepUntilCallableReturnsTrue(Callable<Boolean> condition, long totalTimeToWait, TimeUnit timeUnit) {
        long totalTimeToWaitInSeconds = timeUnit.toSeconds(totalTimeToWait);
        long timeBetweenRetriesInSeconds = determineRetryWaitPeriod(totalTimeToWaitInSeconds);
        Date startTime = new Date();
        long elapsedTimeInSeconds = 0;
        try {
            boolean callableSucceeded = condition.call();
            while (!callableSucceeded && elapsedTimeInSeconds < totalTimeToWaitInSeconds) {
                ThreadUtils.sleep(timeBetweenRetriesInSeconds, TimeUnit.SECONDS);
                elapsedTimeInSeconds = determineElapsedTime(startTime);
                log.info("Retrying after {} seconds", elapsedTimeInSeconds);
                callableSucceeded = condition.call();
            }
            if (!callableSucceeded) {
                throw new RuntimeException("Timed out after " + elapsedTimeInSeconds + " seconds");
            } else {
                log.info("Completed after {} seconds", elapsedTimeInSeconds);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static long determineElapsedTime(Date startTime) {
        long elapsedTimeInSeconds;
        long elapsedTimeInMilliseconds = new Date().getTime() - startTime.getTime();
        elapsedTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeInMilliseconds);
        return elapsedTimeInSeconds;
    }

    private static long determineRetryWaitPeriod(long totalSeconds) {
        long timeBetweenRetries = 60;
        if (totalSeconds < 30) {
            timeBetweenRetries = 10;
        } else if (totalSeconds < 120) {
            timeBetweenRetries = 30;
        }
        return timeBetweenRetries;
    }
}
