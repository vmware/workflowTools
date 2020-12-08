package com.vmware.jenkins.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.BuildStatus;
import com.vmware.util.StringUtils;

import static com.vmware.jenkins.domain.TestResult.TestStatus.SKIP;
import static java.util.Arrays.stream;

public class TestResults {
    public String name;
    @Expose(serialize = false, deserialize = false)
    private JobBuild build;
    @SerializedName("package")
    public Package[] packages;

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
        return loadedTestResults;
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
}
