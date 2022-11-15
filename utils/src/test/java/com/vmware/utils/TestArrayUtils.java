package com.vmware.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vmware.util.ArrayUtils;
import com.vmware.util.IOUtils;
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
    public void maxCheck() {
        Integer[] passedBuilds = new Integer[] {183, 184};
        Integer[] presumedPassedBuilds = new Integer[] {181};
        int newestPass = Stream.of(passedBuilds, presumedPassedBuilds).filter(Objects::nonNull).flatMap(Arrays::stream).mapToInt(Integer::intValue).max().orElse(-1);
        assertEquals(184, newestPass);
        Integer firstValue = 108;
        int secondValue = 109;
        assertTrue(secondValue > firstValue);
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

    @Test
    public void updateJwfToken() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Map<String, String> stfConfig = gson.fromJson(new FileReader("/Users/dbiggs/cloud/dev-integration/system-tests/SystemTests/nsxTGlobalTest.json"), Map.class);
        String jwtToken = IOUtils.read(new File("/Users/dbiggs/.development-org-vcd-api-token.txt"));
        stfConfig.put("jwtToken", jwtToken);
        IOUtils.write(new File("/Users/dbiggs/cloud/dev-integration/system-tests/SystemTests/nsxTGlobalTest.json"), gson.toJson(stfConfig));
        IOUtils.write(new File("/Users/dbiggs/cloud/dev-integration/system-tests/SystemTests/populated-bindings.properties"), "{}");
    }
}
