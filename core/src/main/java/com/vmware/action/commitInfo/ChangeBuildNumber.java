package com.vmware.action.commitInfo;

import java.util.ArrayList;
import java.util.List;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.util.input.InputUtils;

@ActionDescription("Change the build number for the selected build")
public class ChangeBuildNumber extends BaseCommitAction {

    public ChangeBuildNumber(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<JobBuild> matchingBuilds = new ArrayList<>(draft.jobBuildsMatchingUrl(buildwebConfig.buildwebUrl));
        matchingBuilds.addAll(draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl));
        log.info("");
        List<String> choices = new ArrayList<>();
        matchingBuilds.forEach(jobBuild -> choices.add(jobBuild.name));
        choices.add("None");

        int selection = InputUtils.readSelection(choices, "Select jenkins builds to change build number for");
        if (selection >= matchingBuilds.size()) {
            return;
        }

        JobBuild selectedBuild = matchingBuilds.get(selection);
        log.info("Existing url {} and build number {}", selectedBuild.url, selectedBuild.number());
        selectedBuild.updateBuildNumber(InputUtils.readValueUntilValidInt("Enter new build number"));
    }
}
