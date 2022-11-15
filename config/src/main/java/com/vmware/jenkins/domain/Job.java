package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.util.CollectionUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.DbSaveIgnore;
import com.vmware.util.db.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.vmware.jenkins.domain.TestResult.TestStatusOnBuildRemoval.DELETEABLE;
import static com.vmware.jenkins.domain.TestResult.TestStatusOnBuildRemoval.UPDATEABLE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class Job extends BaseDbClass {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public String name;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public String buildDisplayName;

    @Expose(serialize = false, deserialize = false)
    public Long viewId;

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

    public long failingTestCount(int lastFetchAmount) {
        if (testResults == null) {
            return 0;
        }
        return createFailingTestsList(lastFetchAmount).size();
    }

    public boolean lastBuildWasSuccessful() {
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

        testResults.forEach(result -> {
            Optional<JobBuild> matchingBuild = savedBuilds.stream().filter(build -> build.buildNumber.equals(result.buildNumber)).findFirst();
            matchingBuild.ifPresent(build -> {
                result.jobBuildId = build.id;
                result.jobId = build.jobId;
            });
        });

        List<TestResult> duplicateTestResults = new ArrayList<>();

        testResults.forEach(result -> {
            if (result.id == null || result.parameters == null || result.parameters.length == 0) {
                return;
            }

            List<TestResult> resultsForSameUrlPath = testResults.stream()
                    .filter(resultToCheck -> resultToCheck != result && !duplicateTestResults.contains(resultToCheck))
                    .filter(result::matchesByUrlPath).collect(toList());
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
        this.testResults = dbUtils.query(TestResult.class, "SELECT tr.* from TEST_RESULT tr"
                + " JOIN JOB_BUILD jb ON tr.job_build_id = jb.id WHERE jb.JOB_ID = ? ORDER BY tr.NAME ASC, tr.PARAMETERS ASC", id);
        Set<String> usedUrls = new HashSet<>();
        testResults.forEach(result -> {
            Optional<JobBuild> matchingBuild = savedBuilds.stream().filter(build -> result.jobBuildId.equals(build.id)).findFirst();
            matchingBuild.ifPresent(build -> {
                result.commitId = build.commitId;
                result.buildNumber = build.buildNumber;
                result.jobId = build.jobId;
                String testReportsUIUrl;
                if (result.packagePath.equals(TestResults.JUNIT_ROOT)) {
                    testReportsUIUrl = UrlUtils.addRelativePaths(build.url, "testReport");
                } else {
                    testReportsUIUrl = build.getTestReportsUIUrl();
                }
                result.setUrlForTestMethod(testReportsUIUrl, usedUrls);
                usedUrls.add(result.url);
            });
        });
    }

    public boolean buildIsTooOld(int buildNumber, int maxBuildsToCheck) {
        if (savedBuilds == null) {
            return false;
        }

        if (savedBuilds.stream().anyMatch(build -> build.buildNumber == buildNumber)) {
            return true;
        }

        List<JobBuild> usableBuilds = savedBuilds.stream().filter(JobBuild::hasTestResults).collect(toList());
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
        List<JobBuild> usableBuilds = savedBuilds.stream().filter(JobBuild::hasTestResults).collect(toList());
        JobBuild lastSavedBuildToKeep = maxJenkinsBuildsToKeep >= savedBuilds.size() ? savedBuilds.get(savedBuilds.size() - 1) : savedBuilds.get(maxJenkinsBuildsToKeep - 1);
        JobBuild lastUsableBuildToKeep = maxJenkinsBuildsToKeep >= usableBuilds.size() ? usableBuilds.get(usableBuilds.size() - 1) : usableBuilds.get(maxJenkinsBuildsToKeep -1);

        JobBuild lastBuildToKeep = lastUsableBuildToKeep != null && lastUsableBuildToKeep.buildNumber < lastSavedBuildToKeep.buildNumber
                ? lastUsableBuildToKeep : lastSavedBuildToKeep;

        List<JobBuild> existingJobBuildsToCheck = dbUtils.query(JobBuild.class,
                "SELECT * FROM JOB_BUILD WHERE JOB_ID = ? AND BUILD_NUMBER < ?", id, lastBuildToKeep.buildNumber);
        existingJobBuildsToCheck.forEach(build -> {
            log.info("Removing any unimportant test results for build {}", build.name);
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
                dbUtils.delete(build);
                usefulBuilds.remove(build);
            }
        });
    }

    public List<TestResult> createFailingTestsList(int maxJenkinsBuildsToCheck) {
        List<JobBuild> buildsToCheck = savedBuilds != null ? savedBuilds : usefulBuilds;
        return testResults.stream().filter(result -> !TestResult.TestStatus.isPass(result.status))
                .filter(result -> result.containsBuildNumbers(latestUsableBuildNumber()))
                .peek(result -> {
                    List<Map.Entry<Integer, TestResult.TestStatus>> applicableBuilds = result.buildsToUse(maxJenkinsBuildsToCheck);
                    if (applicableBuilds.stream().filter(entry -> !TestResult.TestStatus.isPass(entry.getValue())).count() > 1) {

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
                    }
                }).filter(result -> CollectionUtils.isNotEmpty(result.testRuns)).collect(toList());
    }

    public String runningBuildLink() {
        if (lastBuild != null && lastCompletedBuild != null && lastBuild.buildNumber > lastCompletedBuild.buildNumber) {
            return "Current build <a href=\"" + lastBuild.consoleUrl() + "\">" + lastBuild.buildNumber +"</a>";
        } else {
            return "";
        }
    }
}
