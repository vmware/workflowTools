package com.vmware.action.jenkins;

import java.util.ArrayList;
import java.util.List;

import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;

@ActionDescription("Change the build number for the selected build")
public class ChangeBuildNumber extends BaseCommitWithJenkinsBuildsAction {

    public ChangeBuildNumber(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<JobBuild> matchingBuilds = draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl);
        log.info("");
        List<String> choices = new ArrayList<>();
        matchingBuilds.forEach(jobBuild -> choices.add(jobBuild.buildDisplayName));
        choices.add("None");

        int selection = InputUtils.readSelection(choices, "Select jenkins builds to change build number for");
        if (selection >= choices.size()) {
            return;
        }

        JobBuild selectedBuild = matchingBuilds.get(selection);
        log.info("Existing url {} and build number {}", selectedBuild.url, selectedBuild.getNumber());
        selectedBuild.updateBuildNumber(InputUtils.readValueUntilValidInt("Enter new build number"));
    }
}
