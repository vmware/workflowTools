package com.vmware.jenkins.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.util.UrlUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JobDetails {

    public String name;

    public String url;

    public ActionDefinition[] actions;

    @SerializedName("property")
    public PropertyDefinition[] properties;

    public JobBuildDetails[] builds;

    public JobBuildDetails lastBuild;

    public JobBuildDetails lastCompletedBuild;

    public JobBuildDetails lastStableBuild;

    public JobBuildDetails lastUnstableBuild;

    public int nextBuildNumber;

    public String getFullInfoUrl() {
        return UrlUtils.addRelativePaths(url, "api/json?depth=1");
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
        return lastStableBuild.number() == lastCompletedBuild.number()
                || lastUnstableBuild == null || lastStableBuild.number() > lastUnstableBuild.number();
    }

    public int lastUnstableBuildAge() {
        return lastCompletedBuild.number() - lastUnstableBuild.number();
    }
}
