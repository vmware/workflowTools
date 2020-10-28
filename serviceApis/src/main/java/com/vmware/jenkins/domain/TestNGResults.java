package com.vmware.jenkins.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;

import static com.vmware.util.StringUtils.pluralize;

public class TestNGResults {
    public String name;
    @Expose(serialize = false, deserialize = false)
    public String jobName;
    @Expose(serialize = false, deserialize = false)
    public String buildNumber;
    public String uiUrl;
    @SerializedName("package")
    public Package[] packages;

    public int failCount;
    public int skipCount;
    @SerializedName("fail-config")
    public int failConfig;
    @SerializedName("skip-config")
    public int skipConfig;
    public int total;

    public List<TestMethod> testMethods() {
        List<TestMethod> testMethods = new ArrayList<>();
        for (Package pkg : packages) {
            for (Class clazz : pkg.classs) {
                for (TestMethod testMethod : clazz.testMethods) {
                    testMethod.url = UrlUtils.addRelativePaths(uiUrl, pkg.name, clazz.name, testMethod.name);
                    testMethods.add(testMethod);
                }
            }
        }
        return testMethods;
    }


    public class Package {
        public String name;
        public Class[] classs;
        public int fail;
        public int skip;
        public int totalCount;
        public double duration;
    }

    public class Class {
        public String name;
        public int fail;
        public int skip;
        @SerializedName("test-method")
        public TestMethod[] testMethods;
        public int totalCount;
        public double duration;
    }

    public class TestMethod {
        public String name;
        public String url;
        public TestResult status;
        public String className;
        public String exception;
        public double duration;
        public String[] parameters;

        @Expose(serialize = false, deserialize = false)
        public long failureCount;
        @Expose(serialize = false, deserialize = false)
        public long successCount;

        public String fullTestName() {
            String testName = className + "." + name;
            if (parameters != null && parameters.length > 0) {
                String testParams = StringUtils.join(Arrays.asList(parameters), ",");
                testName += " (" + testParams + ")";
            }
            return testName;
        }

        @Override
        public String toString() {
            String text = fullTestName() + " " + status;
            if (exception != null) {
                text += " " + exception;
            }
            return text;
        }

        public String testResultsDescription() {
            if (failureCount == 0) {
                return "no failures";
            } else if (successCount == 0) {
                return "test failed in all builds";
            } return "test failed in " + pluralize(failureCount, "build") + ", passed in " + pluralize(successCount, "build");
        }
    }

    public enum TestResult {
        PASS, ABORTED, FAIL
    }
}
