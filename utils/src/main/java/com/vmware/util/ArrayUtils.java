package com.vmware.util;

import java.util.Arrays;

public class ArrayUtils {

    public static <T> boolean contains(final T[] values, final T valueToFind) {
        if (values == null) {
            return false;
        }
        return Arrays.asList(values).contains(valueToFind);
    }

    public static int[] add(int[] existingValues, int value) {
        if (existingValues == null) {
            return new int[] { value } ;
        } else if (Arrays.binarySearch(existingValues, value) >= 0) {
            return existingValues;
        } else {
            int length = existingValues.length + 1;
            int[] result = new int[length];
            System.arraycopy(existingValues, 0, result, 0, existingValues.length);
            result[result.length - 1] = value;
            Arrays.sort(result);
            return result;
        }
    }

    public static int[] remove(int[] existingValues, int value) {
        if (existingValues == null || Arrays.stream(existingValues).noneMatch(i -> i == value)) {
            return existingValues;
        } else {
            return Arrays.stream(existingValues).filter(i -> i != value).toArray();
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
