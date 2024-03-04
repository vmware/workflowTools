package com.vmware.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

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
        StopwatchUtils.Stopwatch stopwatch = StopwatchUtils.start();
        try {
            boolean callableSucceeded = condition.call();
            while (!callableSucceeded && stopwatch.elapsedTime(TimeUnit.SECONDS) < totalTimeToWaitInSeconds) {
                ThreadUtils.sleep(timeBetweenRetriesInSeconds, TimeUnit.SECONDS);
                log.debug("Retrying after {} seconds", stopwatch.elapsedTime(TimeUnit.SECONDS));
                callableSucceeded = condition.call();
            }
            if (!callableSucceeded) {
                throw new RuntimeException("Timed out after " + stopwatch.elapsedTime(TimeUnit.SECONDS) + " seconds. " + reason);
            } else {
                log.info("Completed after {} seconds", stopwatch.elapsedTime(TimeUnit.SECONDS));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <R> R retryFunctionUntilSucceeds(IntFunction<R> function, String description, int retryCount) {
        int counter = 0;
        RuntimeException exceptionToThrow = null;
        while (counter <= retryCount) {
            if (counter > 0) {
                log.info("Retry {} of {} for {}", counter, retryCount, description);
            }
            try {
                return function.apply(counter);
            } catch (RuntimeException e) {
                log.error("Failed " + description, e);
                exceptionToThrow = e;
                counter++;
            }
        }
        log.error("Failed {} after {} retries", description, retryCount);
        throw exceptionToThrow;
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
