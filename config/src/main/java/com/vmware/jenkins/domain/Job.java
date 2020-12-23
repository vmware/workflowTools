package com.vmware.jenkins.domain;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.util.CollectionUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.DbSaveIgnore;
import com.vmware.util.db.DbUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.vmware.BuildStatus.SUCCESS;
import static com.vmware.BuildStatus.UNSTABLE;
import static com.vmware.jenkins.domain.TestResult.TestStatus.PASS;
import static com.vmware.jenkins.domain.TestResult.TestStatus.PRESUMED_PASS;
import static java.util.stream.Collectors.toList;

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

    public String getFullInfoUrl() {
        return UrlUtils.addRelativePaths(url, "api/json?depth=1");
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
        return Stream.of(lastStableBuild, lastUnstableBuild).filter(Objects::nonNull).mapToInt(build -> build.buildNumber).max().orElse(-1);
    }

    public void setDbUtils(DbUtils dbUtils) {
        this.dbUtils = dbUtils;
    }

    public void updateBuilds(JobBuild[] builds, String commitIdInDescriptionPattern) {
        this.builds = builds;
        this.usefulBuilds = Arrays.stream(builds).filter(build -> build.status == SUCCESS || build.status == UNSTABLE)
                .sorted((first, second) -> Integer.compare(second.buildNumber, first.buildNumber))
                .peek(build -> build.setCommitIdForBuild(commitIdInDescriptionPattern)).collect(toList());
    }

    public boolean addTestResultsToMasterList() {
        List<TestResult> newTestResultsAdded = new ArrayList<>();

        fetchedResults.forEach(results -> {
            List<TestResult> resultsToAdd = results.testResults();
            for (TestResult resultToAdd : resultsToAdd) {
                resultToAdd.jobBuildId = results.getBuild().id;
                Optional<TestResult> matchingResult = testResults.stream()
                        .filter(result  -> result.fullTestNameWithPackage().equals(resultToAdd.fullTestNameWithPackage())).findFirst();
                if (matchingResult.isPresent()) {
                    matchingResult.get().addTestResult(resultToAdd);
                } else {
                    newTestResultsAdded.add(resultToAdd);
                    resultToAdd.addTestResult(resultToAdd);
                    testResults.add(resultToAdd);
                }
            }
        });

        testResults.removeIf(TestResult::noFailureInfo);

        if (CollectionUtils.isEmpty(savedBuilds) || CollectionUtils.isEmpty(newTestResultsAdded)) {
            return false;
        }

        AtomicBoolean presumedPassResultsAdded = new AtomicBoolean(false);

        final int MAX_PRESUMED_PASS_RESULTS_TO_ADD = 10;
        newTestResultsAdded.stream().filter(result -> result.status != PASS)
                .forEach(result -> savedBuilds.stream().limit(MAX_PRESUMED_PASS_RESULTS_TO_ADD)
                .filter(build -> !result.containsBuildNumbers(build.buildNumber))
                .forEach(build -> {
                    presumedPassResultsAdded.set(true);
                    log.info("Adding presumed pass result for test {} in build {}", result.fullTestName(), build.buildNumber);
                    result.addTestResult(new TestResult(result, build, PRESUMED_PASS));
                }));
        return presumedPassResultsAdded.get();
    }

    public void saveTestResultsToDb(boolean presumedPassedResultsAdded) {
        if (dbUtils == null || (CollectionUtils.isEmpty(fetchedResults) && !presumedPassedResultsAdded)) {
            return;
        }

        try (Connection connection = dbUtils.createConnection()) {
            connection.setAutoCommit(false);
            dbUtils.insertIfNeeded(connection, this, "SELECT * FROM JOB WHERE URL = ?", url);
            if (usefulBuilds == null) {
                usefulBuilds = savedBuilds;
            } else {
                usefulBuilds.forEach(build -> {
                    build.jobId = this.id;
                    dbUtils.insertIfNeeded(connection, build, "SELECT * FROM JOB_BUILD WHERE url = ?", build.url);
                });
            }

            testResults.forEach(result -> {
                result.jobBuildId = usefulBuilds.stream().filter(build -> build.buildNumber.equals(result.buildNumber)).map(build -> build.id)
                        .findFirst().orElse(result.jobBuildId);
                if (result.id != null) {
                    dbUtils.update(connection, result);
                } else {
                    dbUtils.insert(connection, result);
                }
            });

            connection.commit();
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    public void loadTestResultsFromDb() {
        if (dbUtils == null) {
            return;
        }
        savedBuilds = dbUtils.query(JobBuild.class, "SELECT * from JOB_BUILD WHERE JOB_ID = ? ORDER BY BUILD_NUMBER DESC", id);
        try (Connection connection = dbUtils.createConnection()) {
            this.testResults = dbUtils.query(connection, TestResult.class, "SELECT tr.* from TEST_RESULT tr"
                    + " JOIN JOB_BUILD jb ON tr.job_build_id = jb.id WHERE jb.JOB_ID = ? ORDER BY BUILD_NUMBER ASC", id);
            Set<String> usedUrls = new HashSet<>();
            savedBuilds.forEach(build -> testResults.stream().filter(result -> result.jobBuildId.equals(build.id)).forEach(result -> {
                result.commitId = build.commitId;
                result.buildNumber = build.buildNumber;
                result.setUrlForTestMethod(build.getTestReportsUIUrl(), usedUrls);
                usedUrls.add(result.url);
            }));
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    public boolean hasSavedBuild(int buildNumber) {
        return savedBuilds != null && savedBuilds.stream().anyMatch(build -> build.buildNumber == buildNumber);
    }

    public void removeOldBuilds(int maxJenkinsBuildsToCheck) {
        if (dbUtils == null) {
            return;
        }
        usefulBuilds = dbUtils.query(JobBuild.class, "SELECT * FROM JOB_BUILD WHERE JOB_ID = ? ORDER BY BUILD_NUMBER DESC", id);
        int lastBuildIndexPerConfig = Math.max(maxJenkinsBuildsToCheck - 1, 19); // keep a min of 20 builds
        JobBuild lastBuildToKeep = lastBuildIndexPerConfig >= usefulBuilds.size() ? usefulBuilds.get(usefulBuilds.size() - 1) : usefulBuilds.get(lastBuildIndexPerConfig);
        try (Connection connection = dbUtils.createConnection()) {
            List<JobBuild> existingJobBuildsToCheck = dbUtils.query(connection, JobBuild.class,
                    "SELECT * FROM JOB_BUILD WHERE JOB_ID = ? AND BUILD_NUMBER < ?", id, lastBuildToKeep.buildNumber);
            List<JobBuild> buildsToRemove = existingJobBuildsToCheck.stream().filter(build -> testResults.stream()
                    .allMatch(result -> result.removeUnimportantTestResultsForBuild(build))).collect(toList());
            buildsToRemove.forEach(build -> dbUtils.delete(connection, build));
            if (!buildsToRemove.isEmpty()) {
                log.info("Removing old builds {} for {}", buildsToRemove.stream().map(JobBuild::buildNumber).collect(Collectors.joining(",")), name);
                testResults.forEach(result -> dbUtils.update(connection, result));
            } else {
                log.debug("No old job builds to removed for {}", name);
            }
            connection.commit();
            usefulBuilds.removeAll(buildsToRemove);
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    public List<TestResult> createFailingTestsList(int maxJenkinsBuildsToCheck) {
        return testResults.stream().filter(result -> !TestResult.TestStatus.isPass(result.status))
                .filter(result -> result.containsBuildNumbers(latestUsableBuildNumber()))
                .peek(result -> {
                    List<Map.Entry<Integer, TestResult.TestStatus>> applicableBuilds = result.buildsToUse(maxJenkinsBuildsToCheck);
                    if (applicableBuilds.stream().filter(entry -> !TestResult.TestStatus.isPass(entry.getValue())).count() > 1) {
                        result.testRuns = applicableBuilds.stream().map(entry -> {
                            JobBuild matchingBuild = usefulBuilds.stream().filter(build -> build.buildNumber.equals(entry.getKey())).findFirst()
                                    .orElseThrow(() -> new RuntimeException("Failed to find build with number " + entry.getKey() + " for job " + name));
                            return new TestResult(result, matchingBuild, entry.getValue());
                        }).collect(toList());
                    }
                }).filter(result -> CollectionUtils.isNotEmpty(result.testRuns)).collect(toList());
    }
}
