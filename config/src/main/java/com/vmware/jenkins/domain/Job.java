package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.BuildStatus;
import com.vmware.util.CollectionUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.DbSaveIgnore;
import com.vmware.util.db.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLIntegrityConstraintViolationException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.vmware.jenkins.domain.TestResult.TestStatusOnBuildRemoval.DELETEABLE;
import static com.vmware.jenkins.domain.TestResult.TestStatusOnBuildRemoval.UPDATEABLE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class Job extends BaseDbClass {

    private static final SimpleDateFormat START_TIME_TOOLTIP_FORMATTER = new SimpleDateFormat("MMM dd hh:mm aa");

    private static final SimpleDateFormat START_TIME_FORMATTER = new SimpleDateFormat("MMM dd");
    private static final SimpleDateFormat START_TIME_WITH_YEAR_FORMATTER = new SimpleDateFormat("MMM dd yyyy");
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public String name;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public String buildDisplayName;

    public String url;

    @DbSaveIgnore
    public ActionDefinition[] actions;

    @DbSaveIgnore
    @SerializedName("property")
    public PropertyDefinition[] properties;

    @DbSaveIgnore
    public JobBuild[] builds;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public List<JobBuild> usefulBuilds;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    private List<JobBuild> savedBuilds;

    @DbSaveIgnore
    public List<TestResults> fetchedResults;

    @DbSaveIgnore
    public List<TestResult> testResults;

    @DbSaveIgnore
    public JobBuild lastBuild;

    @DbSaveIgnore
    public JobBuild lastCompletedBuild;

    @DbSaveIgnore
    public JobBuild lastStableBuild;

    @DbSaveIgnore
    public JobBuild lastFailedBuild;

    @DbSaveIgnore
    public JobBuild lastUnstableBuild;

    @DbSaveIgnore
    public int nextBuildNumber;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public List<JobParameter> parameters = Collections.emptyList();

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    private DbUtils dbUtils;

    /**
     * Buildweb jobs can be either sandbox or official builds
     */
    public static Job buildwebJob(String url, String jobDisplayName) {
        Job job = new Job();
        job.url = url;
        job.buildDisplayName = jobDisplayName;
        return job;
    }

    public String getInfoUrl() {
        return url + "api/json";
    }

    public void constructUrl(String baseUrl, String jobName) {
        this.name = jobName;
        baseUrl = UrlUtils.addTrailingSlash(baseUrl);
        this.url = baseUrl + "job/" + jobName + "/";
    }

    public String getBuildWithParametersUrl() {
        return UrlUtils.addRelativePaths(url, "buildWithParameters");
    }

    public List<ParameterDefinition> getParameterDefinitions() {
        if (actions == null && properties == null) {
            return Collections.emptyList();
        }
        if (actions != null) {
            for (ActionDefinition actionDefinition : actions) {
                if (actionDefinition.parameterDefinitions != null && actionDefinition.parameterDefinitions.length > 0) {
                    return Arrays.asList(actionDefinition.parameterDefinitions);
                }
            }
        }

        if (properties != null) {
            for (PropertyDefinition propertyDefinition : properties) {
                if (propertyDefinition.parameterDefinitions != null && propertyDefinition.parameterDefinitions.length > 0) {
                    return Arrays.asList(propertyDefinition.parameterDefinitions);
                }
            }
        }

        return Collections.emptyList();
    }

    public List<TestResult> failures(int lastFetchAmount, int numberOfFailuresNeededToBeConsistentlyFailing) {
        if (testResults == null) {
            return Collections.emptyList();
        }
        return createFailingTestsList(lastFetchAmount, numberOfFailuresNeededToBeConsistentlyFailing);
    }


    public long totalTestMethodCount() {
        if (testResults == null) {
            return 0;
        }
        return testResults.stream().filter(testResult -> !Boolean.TRUE.equals(testResult.configMethod)).count();
    }

    public boolean lastBuildWasSuccessful() {
        if (CollectionUtils.isNotEmpty(savedBuilds)) {
            return savedBuilds.get(0).status == BuildStatus.SUCCESS;
        }

        if (lastStableBuild == null) {
            return false;
        }
        return lastStableBuild.buildNumber.equals(lastCompletedBuild.buildNumber)
                || lastUnstableBuild == null || lastStableBuild.buildNumber > lastUnstableBuild.buildNumber;
    }

    public int lastUnstableBuildAge() {
        return lastCompletedBuild.buildNumber - lastUnstableBuild.buildNumber;
    }

    public int latestUsableBuildNumber() {
        int latestBuild = Stream.of(lastStableBuild, lastUnstableBuild).filter(Objects::nonNull).mapToInt(build -> build.buildNumber).max().orElse(-1);
        if (lastFailedBuild != null && lastFailedBuild.buildNumber - latestBuild > 2) {
            latestBuild = lastFailedBuild.buildNumber;
        }
        if (latestBuild == -1 && CollectionUtils.isNotEmpty(savedBuilds)) {
            return savedBuilds.stream().mapToInt(JobBuild::getBuildNumber).max().orElse(-1);
        } else {
            return latestBuild;
        }
    }

    public void setDbUtils(DbUtils dbUtils) {
        this.dbUtils = dbUtils;
    }

    public void addTestResultsToMasterList() {
        fetchedResults.forEach(results -> {
            List<TestResult> resultsToAdd = results.testResults();
            for (TestResult resultToAdd : resultsToAdd) {
                resultToAdd.jobBuildId = results.getBuild().id;
                resultToAdd.jobId = results.getBuild().jobId;
                Optional<TestResult> matchingResult = testResults.stream()
                        .filter(result  -> result.matchesByUrlPath(resultToAdd)).findFirst();
                if (matchingResult.isPresent()) {
                    matchingResult.get().addTestResult(resultToAdd);
                } else {
                    log.info("Found new test {} in build {}", resultToAdd.classAndTestName(), results.getBuild().name);
                    resultToAdd.addTestResult(resultToAdd);
                    testResults.add(resultToAdd);
                }
            }
        });
    }

    public void saveFetchedBuildsInfo() {
        if (dbUtils == null) {
            return;
        }

        dbUtils.insertIfNeeded(this, "SELECT * FROM JOB WHERE URL = ?", url);
        usefulBuilds.forEach(build -> {
            build.jobId = this.id;
            dbUtils.insertIfNeeded(build, "SELECT * FROM JOB_BUILD WHERE url = ?", build.url);
        });

        savedBuilds = dbUtils.query(JobBuild.class, "SELECT * from JOB_BUILD WHERE JOB_ID = ? ORDER BY BUILD_NUMBER DESC", id);
    }

    public void saveTestResultsToDb() {
        if (dbUtils == null) {
            return;
        }

        List<TestResult> duplicateTestResults = new ArrayList<>();

        testResults.forEach(result -> {
            if (result.id == null) {
                return;
            }

            Optional<JobBuild> matchingBuild = savedBuilds.stream().filter(build -> build.buildNumber.equals(result.buildNumber)).findFirst();
            matchingBuild.ifPresent(build -> {
                result.jobBuildId = build.id;
                result.jobId = build.jobId;
            });

            List<TestResult> resultsForSameUrlPath = testResults.stream()
                    .filter(testResult -> testResult.name.equals(result.name))
                    .filter(result::matchesByUrlPath)
                    .filter(resultToCheck -> resultToCheck != result && !duplicateTestResults.contains(resultToCheck)).collect(toList());
            if (resultsForSameUrlPath.isEmpty()) {
                return;
            }
            resultsForSameUrlPath.forEach(duplicateResult -> {
                log.info("Merging duplicate test result with url path {} for {}", result.testPath(), result.fullPackageAndTestName());
                result.addTestResult(duplicateResult);
                duplicateTestResults.add(duplicateResult);
            });
        });

        duplicateTestResults.stream().filter(result -> result.id != null).forEach(result -> {
            log.info("Removing duplicate result {}", result.fullPackageAndTestName());
            dbUtils.delete(result);
            testResults.remove(result);
        });


        testResults.forEach(result -> {
            if (result.id != null) {
                dbUtils.update(result);
            } else {
                dbUtils.insert(result);
            }
        });

    }

    public void loadTestResultsFromDb() {
        if (dbUtils == null) {
            this.testResults = new ArrayList<>();
            return;
        }
        savedBuilds = dbUtils.query(JobBuild.class, "SELECT * from JOB_BUILD WHERE JOB_ID = ? ORDER BY BUILD_NUMBER DESC", id);
        testResults = new ArrayList<>();
        for (JobBuild savedBuild : savedBuilds) {
            List<TestResult> testResultsForBuild = dbUtils.query(TestResult.class, "SELECT * from TEST_RESULT WHERE JOB_BUILD_ID = ?", savedBuild.id);
            Map<String, String[]> usedUrls = new HashMap<>();

            testResultsForBuild.forEach(result -> {
                Optional<JobBuild> matchingBuild = savedBuilds.stream().filter(build -> result.jobBuildId.equals(build.id)).findFirst();
                matchingBuild.ifPresent(build -> {
                    result.refreshFromMatchingBuild(build);

                    String testReportsUIUrl;
                    if (result.packagePath.equals(JenkinsTestResults.JUNIT_ROOT)) {
                        testReportsUIUrl = UrlUtils.addRelativePaths(build.url, "testReport/");
                    } else {
                        testReportsUIUrl = build.getTestReportsUIUrl();
                    }
                    result.setUrlForTestMethod(testReportsUIUrl, usedUrls);
                    usedUrls.put(result.url, result.parameters);
                });
            });
            testResults.addAll(testResultsForBuild);
        }

        savedBuilds.forEach(build -> {
            boolean buildHasTestResultsSaved = testResults.stream().filter(result -> build.jobId.equals(result.jobId)).anyMatch(result -> result.containsBuildNumbers(build.buildNumber));
            build.setHasSavedTestResults(buildHasTestResultsSaved);
        });
    }

    public boolean buildIsTooOld(int buildNumber, int maxBuildsToCheck) {
        if (savedBuilds == null) {
            return false;
        }

        if (savedBuilds.stream().anyMatch(build -> build.buildNumber == buildNumber)) {
            return true;
        }

        List<JobBuild> usableBuilds = savedBuilds.stream().filter(JobBuild::hasSavedTestResults).collect(toList());
        long numberOfNewerBuilds = usableBuilds.stream().filter(build -> build.buildNumber > buildNumber).count();
        return numberOfNewerBuilds >= maxBuildsToCheck;
    }

    public void removeOldBuilds(int maxJenkinsBuildsToKeep) {
        if (dbUtils == null) {
            return;
        }
        if (CollectionUtils.isEmpty(savedBuilds)) {
            return;
        }
        List<JobBuild> usableBuilds = savedBuilds.stream().filter(JobBuild::hasSavedTestResults).collect(toList());

        JobBuild lastSavedBuildToKeep = maxJenkinsBuildsToKeep >= savedBuilds.size() ? savedBuilds.get(savedBuilds.size() - 1) : savedBuilds.get(maxJenkinsBuildsToKeep - 1);
        JobBuild lastUsableBuildToKeep = usableBuilds.isEmpty() ? null
                : maxJenkinsBuildsToKeep >= usableBuilds.size() ? usableBuilds.get(usableBuilds.size() - 1) : usableBuilds.get(maxJenkinsBuildsToKeep -1);

        JobBuild lastBuildToKeep = lastUsableBuildToKeep != null && lastUsableBuildToKeep.buildNumber < lastSavedBuildToKeep.buildNumber
                ? lastUsableBuildToKeep : lastSavedBuildToKeep;

        List<JobBuild> existingJobBuildsToCheck = dbUtils.query(JobBuild.class,
                "SELECT * FROM JOB_BUILD WHERE JOB_ID = ? AND BUILD_NUMBER < ? ORDER BY BUILD_NUMBER ASC", id, lastBuildToKeep.buildNumber);
        existingJobBuildsToCheck.forEach(build -> {
            log.debug("Removing any unimportant test results for build {}", build.name);
            Map<TestResult, TestResult.TestStatusOnBuildRemoval> testResultsWithBuildsRemoved = testResults.stream()
                    .collect(toMap(result -> result, result -> result.removeUnimportantTestResultsForBuild(build, lastBuildToKeep.buildNumber)));
            testResultsWithBuildsRemoved.entrySet().stream().filter(entry -> UPDATEABLE == entry.getValue())
                    .forEach(entry -> dbUtils.update(entry.getKey()));
            testResultsWithBuildsRemoved.entrySet().stream().filter(entry -> DELETEABLE == entry.getValue())
                    .forEach(entry -> {
                        TestResult testResult = entry.getKey();
                        log.info("Deleting test {} {} with id {}", testResult.classAndTestName(), testResult.status, testResult.id);
                        dbUtils.delete(testResult);
                        testResults.remove(testResult);
                    } );
            if (testResults.stream().noneMatch(testResult -> testResult.containsBuildNumbers(build.buildNumber))) {
                log.info("Removing build {}", build.name);
                try {
                    dbUtils.delete(build);
                } catch (RuntimeException re) {
                    if (re.getCause() instanceof SQLIntegrityConstraintViolationException) {
                        log.error("Failed to delete build {} with id {} due to existing tests", build.name, build.id);
                        List<TestResult> existingTests = dbUtils.query(TestResult.class, "SELECT * FROM TEST_RESULT WHERE JOB_BUILD_ID = ?", build.id);
                        existingTests.forEach(testResult -> log.info("Existing test: {}", testResult.classAndTestName()));
                    } else {
                        throw re;
                    }
                }

                usefulBuilds.remove(build);
            }
        });
    }

    public List<TestResult> createFailingTestsList(int maxJenkinsBuildsToCheck, int numberOfFailuresNeededToBeConsistentlyFailing) {
        return createFailingTestsList(result -> {
            List<Map.Entry<Integer, TestResult.TestStatus>> builds = result.buildsToUse(maxJenkinsBuildsToCheck);
            if (builds.stream().filter(entry -> !TestResult.TestStatus.isPass(entry.getValue())).count() >= numberOfFailuresNeededToBeConsistentlyFailing) {
                return builds;
            } else {
                return Collections.emptyList();
            }
        });
    }

    private List<TestResult> createFailingTestsList(Function<TestResult, List<Map.Entry<Integer, TestResult.TestStatus>>> buildsToUseFunction) {
        List<JobBuild> buildsToCheck = savedBuilds != null ? savedBuilds : usefulBuilds;
        int buildNumberToUse = latestUsableBuildNumber();
        return testResults.stream().filter(result -> !TestResult.TestStatus.isPass(result.status))
                .filter(result -> result.containsBuildNumbers(buildNumberToUse))
                .filter(result -> !result.isSkippedConfigMethod())
                .peek(result -> {
                    List<Map.Entry<Integer, TestResult.TestStatus>> applicableBuilds = buildsToUseFunction.apply(result);
                    result.testRuns = applicableBuilds.stream().map(entry -> {
                        JobBuild matchingBuild = buildsToCheck.stream().filter(build -> build.buildNumber.equals(entry.getKey())).findFirst()
                                .orElse(null);
                        if (matchingBuild == null) {
                            log.warn("Failed to find build with number {} for job {}", entry.getKey(), name);
                            return null;
                        } else {
                            return new TestResult(result, matchingBuild, entry.getValue());
                        }
                    }).filter(Objects::nonNull).collect(toList());
                }).filter(result -> CollectionUtils.isNotEmpty(result.testRuns)).collect(toList());
    }

    public String runningBuildLink(String viewUrl) {
        if (lastBuild != null && lastCompletedBuild != null && lastBuild.buildNumber > lastCompletedBuild.buildNumber) {
            String consolePath = StringUtils.substringAfterLast(lastBuild.consoleUrl(), "/job/");
            String consolePathWithViewName = UrlUtils.addRelativePaths(viewUrl, "job", consolePath);
            return "Running <a href=\"" + consolePathWithViewName + "\">" + lastBuild.buildNumber +"</a>";
        } else {
            return "";
        }
    }

    public String latestBuildLink(String viewUrl, int daysOldToIncludeDate) {
        JobBuild build = CollectionUtils.isNotEmpty(savedBuilds) ? savedBuilds.get(0) : lastCompletedBuild;
        if (build != null) {
            String buildPath = StringUtils.substringAfterLast(build.getTestReportsUIUrl(), "/job/");
            String buildPathWithViewName = UrlUtils.addRelativePaths(viewUrl, "job", buildPath);
            Date startDate = new Date(build.buildTimestamp);
            String titleAttribute = build.buildTimestamp > 0 ? String.format(" title=\"%s with commit %s\"",
                    START_TIME_TOOLTIP_FORMATTER.format(startDate), build.commitId) : "";
            String buildDateAndDuration;
            SimpleDateFormat startTimeFormatterToUse = new Date().getTime() > (build.buildTimestamp + TimeUnit.DAYS.toMillis(90))
                    ? START_TIME_WITH_YEAR_FORMATTER : START_TIME_FORMATTER;
            String jobDate = new Date().getTime() > (build.buildTimestamp + TimeUnit.DAYS.toMillis(daysOldToIncludeDate))
                    ? "<b>" + startTimeFormatterToUse.format(startDate) + "</b>": "";
            if (build.duration != null && build.duration > 0) {
                long durationInHours = TimeUnit.MILLISECONDS.toHours(build.duration);
                long durationInMinutes = TimeUnit.MILLISECONDS.toMinutes(build.duration - TimeUnit.HOURS.toMillis(durationInHours));
                buildDateAndDuration = " (" + jobDate + (jobDate.isEmpty() ? "" : " ")  + durationInHours + " hrs " + durationInMinutes + " mins)";
            } else {
                buildDateAndDuration = jobDate.isEmpty() ? "" : " (" + jobDate + ")";
            }
            return "Latest <a href=\"" + buildPathWithViewName + "\"" + titleAttribute + ">" + build.buildNumber + "</a>" + buildDateAndDuration;
        } else {
            return "";
        }
    }

    public String latestBuildDateTime() {
        JobBuild build = CollectionUtils.isNotEmpty(savedBuilds) ? savedBuilds.get(0) : lastCompletedBuild;
        return build != null ? String.valueOf(build.buildTimestamp) : "";
    }
}
