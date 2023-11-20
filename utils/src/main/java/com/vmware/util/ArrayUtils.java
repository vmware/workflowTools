package com.vmware.util;

import java.util.Arrays;
import java.util.stream.Stream;

public class ArrayUtils {

    public static <T> Stream<T> stream(T[] values) {
        if (values == null) {
            return Stream.empty();
        }
        return Arrays.stream(values);
    }

    public static <T> boolean contains(final T[] values, final T valueToFind) {
        if (values == null) {
            return false;
        }
        return Arrays.asList(values).contains(valueToFind);
    }

    public static Integer[] add(Integer[] existingValues, Integer[] values) {
        if (values == null) {
            return existingValues;
        }
        if (existingValues == null) {
            return values;
        }

        for (Integer value : values) {
            existingValues = add(existingValues, value);
        }
        return existingValues;
    }

    public static Integer[] add(Integer[] existingValues, int value) {
        if (existingValues == null) {
            return new Integer[] { value } ;
        } else if (Arrays.binarySearch(existingValues, value) >= 0) {
            return existingValues;
        } else {
            int length = existingValues.length + 1;
            Integer[] result = new Integer[length];
            System.arraycopy(existingValues, 0, result, 0, existingValues.length);
            result[result.length - 1] = value;
            Arrays.sort(result);
            return result;
        }
    }

    public static Integer[] remove(Integer[] existingValues, int value) {
        if (existingValues == null || Arrays.stream(existingValues).noneMatch(i -> i == value)) {
            return existingValues;
        } else {
            return Arrays.stream(existingValues).filter(i -> i != value).toArray(Integer[]::new);
        }
    }

    public static String[] join(String[] firstArray, String[] secondArray) {
        int length = firstArray.length + secondArray.length;
        String[] result = new String[length];
        System.arraycopy(firstArray, 0, result, 0, firstArray.length);
        System.arraycopy(secondArray, 0, result, firstArray.length, secondArray.length);
        return result;
    }

    public static byte[] join(byte[] first, byte[] second) {
        byte[] combinedArray = new byte[first.length + second.length];
        System.arraycopy(first,0, combinedArray,0, first.length);
        System.arraycopy(second,0, combinedArray, first.length, second.length);
        return combinedArray;
    }

    public static byte[] subSection(byte[] data, int startPos, int length) {
        if (data.length < length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        byte[] result = new byte[length];
        System.arraycopy(data, startPos, result, 0, length);
        return result;
    }
}
