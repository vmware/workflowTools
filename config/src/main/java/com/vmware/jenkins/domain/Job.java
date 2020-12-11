package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.util.UrlUtils;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.DbSaveIgnore;
import com.vmware.util.db.DbUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

public class Job extends BaseDbClass {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public String name;

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

    public void addTestResultsToMasterList() {
        if (fetchedResults == null) {
            return;
        }

        fetchedResults.forEach(results -> {
            List<TestResult> resultsToAdd = results.testResults();
            for (TestResult resultToAdd : resultsToAdd) {
                resultToAdd.jobBuildId = results.getBuild().id;
                Optional<TestResult> matchingResult = testResults.stream()
                        .filter(result  -> result.fullTestNameWithPackage().equals(resultToAdd.fullTestNameWithPackage())).findFirst();
                if (matchingResult.isPresent()) {
                    matchingResult.get().addTestResult(resultToAdd);
                } else {
                    resultToAdd.addTestResult(resultToAdd);
                    testResults.add(resultToAdd);
                }
            }
        });
    }

    public void saveTestResultsToDb(DbUtils dbUtils) {
        if (dbUtils == null || fetchedResults == null || fetchedResults.isEmpty()) {
            return;
        }


        try (Connection connection = dbUtils.createConnection()) {
            connection.setAutoCommit(false);
            dbUtils.insertIfNeeded(connection, this, "SELECT * FROM JOB WHERE URL = ?", url);
            usefulBuilds.forEach(build -> {
                build.jobId = this.id;
                dbUtils.insertIfNeeded(connection, build, "SELECT * FROM JOB_BUILD WHERE url = ?", build.url);
            });
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

    public void removeOldBuilds(DbUtils dbUtils, int maxJenkinsBuildsToCheck) {
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
            buildsToRemove.forEach(build -> dbUtils.delete(connection, "DELETE FROM JOB_BUILD WHERE ID = ?", build.id));
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
        return testResults.stream().filter(result -> result.status != TestResult.TestStatus.PASS)
                .filter(result -> result.containsBuildNumbers(latestUsableBuildNumber()))
                .peek(result -> {
                    Map<Integer, TestResult.TestStatus> applicableBuilds = result.buildsToUse(maxJenkinsBuildsToCheck);
                    if (applicableBuilds.values().stream().filter(status -> status != TestResult.TestStatus.PASS).count() > 1) {
                        result.testRuns = applicableBuilds.keySet().stream().sorted(Collections.reverseOrder()).map(buildNumber -> {
                            JobBuild matchingBuild = usefulBuilds.stream().filter(build -> build.buildNumber.equals(buildNumber)).findFirst()
                                    .orElseThrow(() -> new RuntimeException("Failed to find build with number " + buildNumber + " for job " + name));
                            return new TestResult(result, matchingBuild, applicableBuilds.get(buildNumber));
                        }).collect(toList());
                    }
                }).filter(result -> result.testRuns != null && !result.testRuns.isEmpty()).collect(toList());
    }

}
