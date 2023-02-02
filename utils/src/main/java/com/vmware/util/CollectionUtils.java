package com.vmware.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class CollectionUtils {
    public static boolean isEmpty(Object collection) {
        return collection == null || Array.getLength(collection) == 0;
    }

    public static boolean isNotEmpty(int[] collection) {
        return collection != null && collection.length > 0;
    }

    public static boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection collection) {
        return collection != null && !collection.isEmpty();
    }

    public static <T> Stream<T> stream(List<T> values) {
        if (values == null) {
            return Stream.empty();
        }
        return values.stream();
    }
}
