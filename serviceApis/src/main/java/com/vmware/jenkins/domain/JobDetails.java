package com.vmware.jenkins.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vmware.JobBuild;

public class JobDetails {

    public String name;

    public ActionDefinition[] actions;

    public JobBuild[] builds;

    public JobBuild lastBuild;

    public int nextBuildNumber;

    public List<ParameterDefinition> getParameterDefinitions() {
        if (actions == null) {
            return Collections.emptyList();
        }
        for (ActionDefinition actionDefinition : actions) {
            if (actionDefinition.parameterDefinitions != null && actionDefinition.parameterDefinitions.length > 0) {
                return Arrays.asList(actionDefinition.parameterDefinitions);
            }
        }
        return Collections.emptyList();
    }


}
