package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.vmware.util.UrlUtils;
import com.vmware.util.db.BaseDbClass;
import com.vmware.util.db.DbSaveIgnore;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class Job extends BaseDbClass {

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
}
