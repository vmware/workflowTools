package com.vmware.utils;

import java.util.Arrays;
import com.vmware.util.ArrayUtils;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TestArrayUtils {

    @Test
    public void addToArray() {
        int[] startingArray = new int[] {56, 23, 34};
        int[] updatedArray = ArrayUtils.add(startingArray, 29);
        assertEquals(Arrays.toString(new int[] {56, 23, 34, 29}), Arrays.toString(updatedArray));
    }

    @Test
    public void removeFromArray() {
        int[] startingArray = new int[] {56, 23, 34};
        int[] updatedArray = ArrayUtils.remove(startingArray, 23);
        assertEquals(Arrays.toString(new int[] {56, 34}), Arrays.toString(updatedArray));
    }
}
