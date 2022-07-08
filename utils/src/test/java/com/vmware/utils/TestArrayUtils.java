package com.vmware.utils;

import java.util.Arrays;
import com.vmware.util.ArrayUtils;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestArrayUtils {

    @Test
    public void addToArray() {
        Integer[] startingArray = new Integer[] {56, 23, 34};
        Integer[] updatedArray = ArrayUtils.add(startingArray, 29);
        assertEquals(Arrays.toString(new Integer[] {23, 29, 34, 56}), Arrays.toString(updatedArray));
    }

    @Test
    public void removeFromArray() {
        Integer[] startingArray = new Integer[] {56, 23, 34};
        Integer[] updatedArray = ArrayUtils.remove(startingArray, 23);
        assertEquals(Arrays.toString(new Integer[] {56, 34}), Arrays.toString(updatedArray));
    }

    @Test
    public void findInArray() {
        int[] values = new int[] {23, 34, 56};
        assertEquals(0, Arrays.binarySearch(values, 23));
        assertEquals(-2, Arrays.binarySearch(values, 24));
        assertEquals(-3, Arrays.binarySearch(values, 37));
        assertEquals(2, Arrays.binarySearch(values, 56));
        assertEquals(-4, Arrays.binarySearch(values, 600));
    }
}
