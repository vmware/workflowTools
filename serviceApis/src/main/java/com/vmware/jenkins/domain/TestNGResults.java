package com.vmware.jenkins.domain;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.vmware.util.UrlUtils;

public class TestNGResults {
    public String name;
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

        public String fullTestName() {
            return className + "." + name;
        }

        @Override
        public String toString() {
            String text = fullTestName() + " " + status;
            if (exception != null) {
                text += " " + exception;
            }
            return text;
        }
    }

    public enum TestResult {
        PASS, ABORTED, FAIL
    }
}
