package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.vmware.jenkins.domain.TestResult.TestStatus.PASS;

public class TestResults {

    public String name;
    @Expose(serialize = false, deserialize = false)
    protected JobBuild build;

    public int failCount;
    public int skipCount;
    @SerializedName("fail-config")
    public int failConfig;
    @SerializedName("skip-config")
    public int skipConfig;
    public int total;

    @Expose(serialize = false, deserialize = false)
    protected List<TestResult> loadedTestResults;

    public TestResults() {
    }

    public TestResults(JobBuild build) {
        this.build = build;
    }

    public JobBuild getBuild() {
        return build;
    }

    public void combineTestResults(TestResults testResults) {
        if (loadedTestResults == null) {
            loadedTestResults = new ArrayList<>();
        }
        this.failCount += testResults.failCount;
        this.skipCount += testResults.skipCount;
        this.failConfig += testResults.failConfig;
        this.loadedTestResults.addAll(testResults.testResults());
    }

    public List<TestResult> testResults() {
        if (loadedTestResults != null) {
            return loadedTestResults;
        }
        loadedTestResults = new ArrayList<>();

        return loadedTestResults;
    }

    public List<TestResult> failedTestResults() {
        return testResults().stream().filter(result -> result.status != PASS).sorted(Comparator.comparing(TestResult::getStartedAt)).collect(Collectors.toList());
    }
}
