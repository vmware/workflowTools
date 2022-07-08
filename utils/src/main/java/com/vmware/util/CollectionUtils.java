package com.vmware.util;

import java.lang.reflect.Array;
import java.util.Collection;

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
}
