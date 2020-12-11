package com.vmware.jenkins.domain;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.gson.annotations.Expose;
import com.vmware.util.ArrayUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.db.AfterDbLoad;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.DbSaveIgnore;

public class TestResult extends BaseDbClass {
    public String name;
    @Expose(serialize = false, deserialize = false)
    public Long jobBuildId;

    public TestStatus status;
    public String packagePath;
    public String className;
    public String exception;
    public double duration;
    public long startedAt;
    public String[] parameters;

    public Integer similarSkips;

    @Expose(serialize = false, deserialize = false)
    public int[] failedBuilds;

    @Expose(serialize = false, deserialize = false)
    public int[] skippedBuilds;

    @Expose(serialize = false, deserialize = false)
    public int[] passedBuilds;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public String url;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public int buildNumber;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public String commitId;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public List<TestResult> testRuns;

    public TestResult() {
    }

    public TestResult(TestResult methodToClone, JobBuild build, TestStatus status) {
        this.name = methodToClone.name;
        this.buildNumber = build.buildNumber;
        this.url = methodToClone.url.replace(String.valueOf(methodToClone.buildNumber), String.valueOf(build.buildNumber));
        this.packagePath = methodToClone.packagePath;
        this.className = methodToClone.className;
        this.parameters = methodToClone.parameters;
        this.jobBuildId = methodToClone.jobBuildId;
        this.status = status;
        this.commitId = build.commitId;
    }

    @AfterDbLoad
    public void sortArrays() {
        Stream.of(passedBuilds, skippedBuilds, failedBuilds).filter(Objects::nonNull).forEach(Arrays::sort);
    }


    public String fullTestNameWithPackage() {
        return packagePath + "." + fullTestName();
    }

    public long getStartedAt() {
        return startedAt;
    }

    public String fullTestName() {
        String testName = className + "." + name;
        if (parameters != null && parameters.length > 0) {
            String testParams = StringUtils.join(Arrays.asList(parameters), ",");
            testName += " (" + testParams + ")";
        }
        return testName;
    }

    public String fullTestNameWithSkipInfo() {
        String fullTestName = fullTestName();
        if (status == TestStatus.SKIP && similarSkips != null && similarSkips > 0) {
            fullTestName = fullTestName + " (" + StringUtils.pluralize(similarSkips, "similar skip") + ")";
        }
        return fullTestName;
    }

    public void setUrlForTestMethod(String uiUrl, Set<String> usedUrls) {
        String urlToUse = UrlUtils.addRelativePaths(uiUrl, packagePath, className, name);
        int counter = 1;
        while (usedUrls.contains(urlToUse)) {
            urlToUse = UrlUtils.addRelativePaths(uiUrl, packagePath, className, name + "_" + counter++);
        }
        this.url = urlToUse;
    }

    public String testLinks(String commitComparisonUrl) {
        List<TestResult> sortedTestRuns = testRuns.stream().sorted(Comparator.comparing(TestResult::buildNumber)).collect(Collectors.toList());
        String resultLinks = sortedTestRuns.stream().map(this::testResultLink).collect(Collectors.joining(System.lineSeparator()));

        sortedTestRuns.sort(Comparator.comparing(TestResult::buildNumber).reversed());
        Optional<TestResult> lastPassingResult =
                IntStream.range(1, sortedTestRuns.size()).mapToObj(sortedTestRuns::get).filter(method -> method.status == TestStatus.PASS).findFirst();

        if (lastPassingResult.isPresent() && StringUtils.isNotBlank(commitComparisonUrl) && StringUtils.isNotBlank(lastPassingResult.get().commitId)) {
            TestResult firstFailure = sortedTestRuns.get(sortedTestRuns.indexOf(lastPassingResult.get()) - 1);
            String suspectsUrl = commitComparisonUrl.replace("(first)", lastPassingResult.get().commitId)
                    .replace("(second)", firstFailure.commitId);
            String suspectsLink = "<a class =\"suspects\" href=\"" + suspectsUrl + "\""
                    + "title=\"Commits between last pass and first failure, guilty until proven innocent\">SUSPECTS</a>";
            return suspectsLink + System.lineSeparator() + resultLinks;
        } else {
            return resultLinks;
        }
    }

    public boolean containsBuildNumbers(int... buildNumber) {
        return Stream.of(passedBuilds, skippedBuilds, failedBuilds).anyMatch(values -> containsBuildNumbers(values, buildNumber));
    }

    public Map<Integer, TestStatus> buildsToUse(int limit) {
        Map<Integer, TestStatus> testStatusMap = new HashMap<>();
        Stream.of(passedBuilds).filter(Objects::nonNull).flatMapToInt(IntStream::of).forEach(value -> testStatusMap.put(value, TestStatus.PASS));
        Stream.of(failedBuilds).filter(Objects::nonNull).flatMapToInt(IntStream::of).forEach(value -> testStatusMap.put(value, TestStatus.FAIL));
        Stream.of(skippedBuilds).filter(Objects::nonNull).flatMapToInt(IntStream::of).forEach(value -> testStatusMap.put(value, TestStatus.SKIP));

        Comparator<Map.Entry<Integer, TestStatus>> keyComparator = Map.Entry.<Integer, TestStatus>comparingByKey().reversed();

        Map<Integer, TestStatus> buildsToUse = testStatusMap.entrySet().stream().sorted(keyComparator).limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (buildsToUse.values().stream().noneMatch(value -> value == TestStatus.PASS) && passedBuilds != null && passedBuilds.length > 0) {
            int lastPassingBuildNumber = passedBuilds[passedBuilds.length - 1];
            buildsToUse.put(lastPassingBuildNumber, TestStatus.PASS);
            // get next failing / skipped test
            for (int i = lastPassingBuildNumber + 1; i < buildNumber; i ++) {
                if (testStatusMap.containsKey(i) && testStatusMap.get(i) == TestStatus.FAIL) {
                    buildsToUse.put(i, testStatusMap.get(i));
                    return buildsToUse;
                }
            }

            for (int i = lastPassingBuildNumber + 1; i < buildNumber; i ++) {
                if (testStatusMap.containsKey(i)) {
                    buildsToUse.put(i, testStatusMap.get(i));
                    return buildsToUse;
                }
            }
        }
        return buildsToUse;
    }

    public void addTestResult(TestResult testResult) {
        if (testResult.buildNumber == 0) {
            throw new RuntimeException("Bad build number for test " + testResult.name);
        }
        switch (testResult.status) {
        case PASS:
            passedBuilds = ArrayUtils.add(passedBuilds, testResult.buildNumber);
            break;
        case FAIL:
            failedBuilds = ArrayUtils.add(failedBuilds, testResult.buildNumber);
            break;
        case SKIP:
            skippedBuilds = ArrayUtils.add(skippedBuilds, testResult.buildNumber);
            break;
        }
        if (testResult.buildNumber <= buildNumber) {
            return;
        }

        if (testResult.status == TestStatus.SKIP && testResult.hasStandardSkipException()) {
            return;
        }

        // update to latest build result
        this.url = testResult.url;
        this.buildNumber = testResult.buildNumber;
        this.status = testResult.status;
        this.similarSkips = testResult.similarSkips;
        this.exception = testResult.exception;
        this.startedAt = testResult.startedAt;
        this.duration = testResult.duration;
    }

    public boolean removeUnimportantTestResultsForBuild(JobBuild build) {
        if (this.jobBuildId != null && this.jobBuildId.equals(build.id)) {
            return false;
        }
        int newestPass = passedBuilds != null ? Arrays.stream(passedBuilds).max().orElse(-1) : -1;
        int firstFailureAfterPass = failedBuilds != null ? Arrays.stream(failedBuilds).filter(failure -> failure > newestPass).findFirst().orElse(-1) : -1;
        int firstSkipAfterPass = skippedBuilds != null ? Arrays.stream(skippedBuilds).filter(skip -> skip > newestPass).findFirst().orElse(-1) : -1;
        int firstFailureOrSkipAfterPass = Stream.of(firstFailureAfterPass, firstSkipAfterPass).filter(val -> val != -1).mapToInt(val -> val).min().orElse(-1);
        if (newestPass == build.buildNumber || firstFailureOrSkipAfterPass == build.buildNumber) {
            return false;
        }

        passedBuilds = ArrayUtils.remove(passedBuilds, build.buildNumber);
        failedBuilds = ArrayUtils.remove(failedBuilds, build.buildNumber);
        skippedBuilds = ArrayUtils.remove(skippedBuilds, build.buildNumber);
        return true;

    }

    @Override
    public String toString() {
        String text = fullTestName() + " " + status;
        if (exception != null) {
            text += " " + exception;
        }
        return text;
    }

    private String testResultLink(TestResult testResult) {
        String commitIdSuffix = StringUtils.isNotBlank(testResult.commitId) ? " with commit " + testResult.commitId : "";
        String title = testResult.status.getDescription() + " in build " + testResult.buildNumber + commitIdSuffix;
        return String.format("<a class =\"%s\" href = \"%s\" title=\"%s\">%s</a>", testResult.status.cssClass, testResult.url, title, testResult.buildNumber);
    }

    private boolean containsBuildNumbers(int[] values, int... buildNumbers) {
        if (values == null) {
            return false;
        }
        return Arrays.stream(buildNumbers).allMatch(buildNumber -> Arrays.stream(values).anyMatch(value -> value == buildNumber));
    }

    private int buildNumber() {
        return buildNumber;
    }

    public enum TestStatus {
        PASS("Passed", "testPass"), ABORTED("Aborted", "testFail"),
        FAIL("Failed", "testFail"), SKIP("Skipped", "testSkip");
        private final String description;
        private final String cssClass;

        TestStatus(String description, String cssClass) {
            this.description = description;
            this.cssClass = cssClass;
        }

        public String getDescription() {
            return description;
        }
    }

    private boolean hasStandardSkipException() {
        return exception == null || exception.contains("depends on not successfully finished methods");
    }
}

