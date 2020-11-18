package com.vmware.jenkins.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.BuildResult;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;

public class TestNGResults {
    public String name;
    @Expose(serialize = false, deserialize = false)
    public BuildResult buildResult;
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
                Set<String> usedUrls = new HashSet<>();
                for (TestMethod testMethod : clazz.testMethods) {
                    testMethod.packagePath = pkg.name;
                    testMethod.buildNumber = buildNumber;
                    testMethod.setUrlForTestMethod(uiUrl, usedUrls);
                    usedUrls.add(testMethod.url);
                    testMethods.add(testMethod);
                }
            }
        }
        return testMethods;
    }


    public static class Package {
        public String name;
        public Class[] classs;
        public int fail;
        public int skip;
        public int totalCount;
        public double duration;
    }

    public static class Class {
        public String name;
        public int fail;
        public int skip;
        @SerializedName("test-method")
        public TestMethod[] testMethods;
        public int totalCount;
        public double duration;
    }

    public static class TestMethod {
        public String name;
        public TestResult status;
        public String packagePath;
        public String className;
        public String exception;
        public double duration;
        public String[] parameters;

        @Expose(serialize = false, deserialize = false)
        public String url;
        @Expose(serialize = false, deserialize = false)
        public String buildNumber;
        @Expose(serialize = false, deserialize = false)
        public List<TestMethod> testRuns;

        public TestMethod() {
        }

        public TestMethod(TestMethod methodToClone, TestResult status, String buildNumber) {
            this.name = methodToClone.name;
            this.packagePath = methodToClone.packagePath;
            this.className = methodToClone.className;
            this.parameters = methodToClone.parameters;
            this.status = status;
            this.buildNumber = buildNumber;
        }

        public String fullTestNameWithPackage() {
            return packagePath + "." + fullTestName();
        }

        public String fullTestName() {
            String testName = className + "." + name;
            if (parameters != null && parameters.length > 0) {
                String testParams = StringUtils.join(Arrays.asList(parameters), ",");
                testName += " (" + testParams + ")";
            }
            return testName;
        }

        public void setUrlForTestMethod(String uiUrl, Set<String> usedUrls) {
            String urlToUse = UrlUtils.addRelativePaths(uiUrl, packagePath, className, name);
            int counter = 1;
            while (usedUrls.contains(urlToUse)) {
                urlToUse = UrlUtils.addRelativePaths(uiUrl, packagePath, className, name + "_" + counter++);
            }
            this.url = urlToUse;
        }

        public String testResultLink(TestMethod testMethod) {
            String linkClass = testMethod.status == TestResult.PASS ? "testPass" : "testFail";
            return String.format("<a class =\"%s\" href = \"%s\">%s</a>", linkClass, testMethod.url, testMethod.buildNumber);
        }

        public String testResultsLinks() {
            return testRuns.stream().sorted(Comparator.comparing(TestMethod::buildNumberAsInt))
                    .map(this::testResultLink).collect(Collectors.joining(" "));
        }

        @Override
        public String toString() {
            String text = fullTestName() + " " + status;
            if (exception != null) {
                text += " " + exception;
            }
            return text;
        }

        private int buildNumberAsInt() {
            return Integer.parseInt(buildNumber);
        }
    }

    public enum TestResult {
        PASS, ABORTED, FAIL
    }
}
