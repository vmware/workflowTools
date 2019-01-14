package com.vmware.jenkins.domain;

import com.google.gson.annotations.SerializedName;
import com.vmware.JobBuild;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JobDetails {

    public String name;

    public ActionDefinition[] actions;

    @SerializedName("property")
    public PropertyDefinition[] properties;

    public JobBuild[] builds;

    public JobBuild lastBuild;

    public int nextBuildNumber;

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


}
