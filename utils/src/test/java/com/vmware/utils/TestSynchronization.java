package com.vmware.utils;

import com.vmware.util.ThreadUtils;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class TestSynchronization {

    @Test
    public void print() {
        Syncer syncer = new Syncer();
        IntStream.range(1, 100).parallel().forEach(i -> {
            if (i % 2 == 0) {
                syncer.runFirst(i);
            } else {
                syncer.runSecond(i);
            }
        });
    }


    private class Syncer {
        private String message;
        private boolean inMethod;

        public synchronized void clear(int index) {
            message = "";
            ThreadUtils.sleep(30, TimeUnit.MILLISECONDS);
            System.out.println("cleared " + index + " " + message);
        }

        public synchronized void runFirst(int index) {
            if (inMethod) {
                throw new RuntimeException("failed on " + index);
            }
            inMethod = true;
            message += "first " + index;
            System.out.println(message);
            clear(index);
            inMethod = false;
        }

        public synchronized void runSecond(int index) {
            if (inMethod) {
                throw new RuntimeException("failed on " + index);
            }
            inMethod = true;
            message += "second " + index;
            System.out.println(message);
            clear(index);
            inMethod = false;
        }
    }
}
