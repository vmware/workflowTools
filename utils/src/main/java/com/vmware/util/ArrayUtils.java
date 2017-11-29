package com.vmware.util;

public class ArrayUtils {

    public static <T> boolean contains(final T[] values, final T valueToFind) {
        if (values == null) {
            return false;
        }
        for (final T value : values)
            if (value == valueToFind || valueToFind != null && valueToFind.equals(value))
                return true;

        return false;
    }

    public static String[] join(String[] firstArray, String[] secondArray) {
        int length = firstArray.length + secondArray.length;
        String[] result = new String[length];
        System.arraycopy(firstArray, 0, result, 0, firstArray.length);
        System.arraycopy(secondArray, 0, result, firstArray.length, secondArray.length);
        return result;
    }
}
