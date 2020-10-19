package com.vmware.util.collection;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockingExecutorService<T> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final BlockingDeque<Supplier<T>> availableObjects;
    private final ConcurrentLinkedQueue<T> usedObjects = new ConcurrentLinkedQueue<>();

    public BlockingExecutorService(int maxSize, Supplier<T> createFunction) {
        this.availableObjects = new LinkedBlockingDeque<>(maxSize);
        IntStream.range(0, maxSize).forEach(i -> {
            try {
                availableObjects.put(createFunction);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <V> V execute(Function<T, V> function) {
        T value = null;
        try {
            value = availableObjects.take().get();
            usedObjects.add(value);
            return function.apply(value);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (value != null) {
                final T valueToReAdd = value;
                usedObjects.remove(valueToReAdd);
                availableObjects.addFirst(() -> {
                    log.debug("Reusing already created service");
                    return valueToReAdd;
                });
            }
        }
    }
}
