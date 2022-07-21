package com.vmware.jenkins.domain;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;

import static com.vmware.jenkins.domain.TestResult.TestStatus.FAIL;
import static com.vmware.jenkins.domain.TestResult.TestStatus.FAILED;
import static com.vmware.jenkins.domain.TestResult.TestStatus.FIXED;
import static com.vmware.jenkins.domain.TestResult.TestStatus.PASS;
import static com.vmware.jenkins.domain.TestResult.TestStatus.PASSED;
import static com.vmware.jenkins.domain.TestResult.TestStatus.REGRESSION;
import static com.vmware.jenkins.domain.TestResult.TestStatus.SKIP;
import static java.util.Arrays.stream;

public class TestResults {
    public static final String JUNIT_ROOT = "junit/(root)";

    public String name;
    @Expose(serialize = false, deserialize = false)
    private JobBuild build;
    @SerializedName("package")
    public Package[] packages;

    public Suite[] suites;

    public int failCount;
    public int skipCount;
    @SerializedName("fail-config")
    public int failConfig;
    @SerializedName("skip-config")
    public int skipConfig;
    public int total;

    @Expose(serialize = false, deserialize = false)
    private List<TestResult> loadedTestResults;

    public TestResults() {
    }

    public TestResults(JobBuild build) {
        this.setBuild(build);
    }

    public void setBuild(JobBuild build) {
        this.build = build;
        this.build.failedCount = failCount;
        this.build.skippedCount = skipCount;
    }

    public JobBuild getBuild() {
        return build;
    }

    public int totalFailures() {
        return failConfig + failCount;
    }

    public List<TestResult> testResults() {
        if (loadedTestResults != null) {
            return loadedTestResults;
        }
        loadedTestResults = new ArrayList<>();
        if (packages != null) {
            for (Package pkg : packages) {
                for (Class clazz : pkg.classs) {
                    Set<String> usedUrls = new HashSet<>();
                    Set<String> usedSkipExceptions = new HashSet<>();
                    for (TestResult testResult : clazz.testResults) {
                        testResult.packagePath = pkg.name;
                        testResult.buildNumber = build.buildNumber;
                        testResult.commitId = build.commitId;
                        testResult.setUrlForTestMethod(build.getTestReportsUIUrl(), usedUrls);
                        if (testResult.status == SKIP && StringUtils.isNotBlank(testResult.exception) && !usedSkipExceptions.contains(testResult.exception)) {
                            testResult.similarSkips = Math.toIntExact(
                                    stream(clazz.testResults).filter(method -> method.status == SKIP
                                            && StringUtils.equals(method.exception, testResult.exception)).count() - 1);
                            usedSkipExceptions.add(testResult.exception);
                        }
                        usedUrls.add(testResult.url);
                        loadedTestResults.add(testResult);
                    }
                }
            }
        }
        if (suites != null) {
            for (Suite suite : suites) {
                Set<String> usedUrls = new HashSet<>();
                Set<String> usedSkipExceptions = new HashSet<>();
                for (TestResult testResult : suite.testResults) {
                    testResult.packagePath = JUNIT_ROOT;
                    testResult.buildNumber = build.buildNumber;
                    testResult.commitId = build.commitId;
                    testResult.setUrlForTestMethod(UrlUtils.addRelativePaths(build.url, "testReport"), usedUrls);
                    if (testResult.skipped) {
                        testResult.status = SKIP;
                    }
                    if (testResult.status == REGRESSION || testResult.status == FAILED) {
                        testResult.status = FAIL;
                    }
                    if (testResult.status == PASSED || testResult.status == FIXED) {
                        testResult.status = PASS;
                    }
                    if (StringUtils.isNotBlank(testResult.errorStackTrace)) {
                        testResult.exception = testResult.errorStackTrace;
                    }
                    if (testResult.status == SKIP && StringUtils.isNotBlank(testResult.exception) && !usedSkipExceptions.contains(testResult.exception)) {
                        testResult.similarSkips = Math.toIntExact(
                                stream(suite.testResults).filter(method -> method.status == SKIP
                                        && StringUtils.equals(method.exception, testResult.exception)).count() - 1);
                        usedSkipExceptions.add(testResult.exception);
                    }
                    usedUrls.add(testResult.url);
                    loadedTestResults.add(testResult);
                }
            }

        }

        return loadedTestResults;
    }

    public List<TestResult> failedTestResults() {
        return testResults().stream().filter(result -> result.status != PASS).sorted(Comparator.comparing(TestResult::getStartedAt)).collect(Collectors.toList());
    }

    public void setLoadedTestResults(List<TestResult> loadedTestResults) {
        this.loadedTestResults = loadedTestResults;
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
        public TestResult[] testResults;
        public int totalCount;
        public double duration;
    }

    public static class Suite {
        public String name;
        @SerializedName("cases")
        public TestResult[] testResults;
        public int failCount;
        public int skipCount;
        public int passCount;
        public double duration;
    }
}
