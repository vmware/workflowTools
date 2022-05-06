package com.vmware.action.commitInfo;

import java.util.ArrayList;
import java.util.List;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.util.input.InputUtils;

@ActionDescription("Removes selected builds from testing done section of commit.")
public class RemoveSelectedBuilds extends BaseCommitAction {

    public RemoveSelectedBuilds(WorkflowConfig config) {
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
        List<Integer> selections = InputUtils.readSelections(choices, "Select builds to remove from commit", false);
        // check selection doesn't contain none value
        if (!selections.contains(choices.size() - 1)) {
            selections.forEach(selection -> draft.jobBuilds.removeIf(build -> build.url.equals(matchingBuilds.get(selection).url)));
        }
    }
}
