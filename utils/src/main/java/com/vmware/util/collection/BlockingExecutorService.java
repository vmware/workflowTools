package com.vmware.util.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class BlockingExecutorService<T> {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final BlockingDeque<Supplier<T>> availableObjects;

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
            return function.apply(value);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (value != null) {
                final T valueToReAdd = value;
                availableObjects.addFirst(() -> {
                    log.debug("Reusing already created service");
                    return valueToReAdd;
                });
            }
        }
    }
}
