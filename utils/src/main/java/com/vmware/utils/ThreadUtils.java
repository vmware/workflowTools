package com.vmware.utils;

import java.util.concurrent.TimeUnit;

public class ThreadUtils {

    public static void sleep(long amount, TimeUnit timeUnit) {
        try {
            Thread.sleep(timeUnit.toMillis(amount));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
