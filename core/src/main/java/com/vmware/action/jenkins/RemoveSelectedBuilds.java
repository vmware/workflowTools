package com.vmware.action.jenkins;

import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ActionDescription("Removes selected jenkins builds from testing done section of commit.")
public class RemoveSelectedBuilds extends BaseCommitAction {

    public RemoveSelectedBuilds(WorkflowConfig config) {
        super(config);
    }

    @Override
    public String cannotRunAction() {
        if (draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl).isEmpty()) {
            return "commit has no Jenkins builds";
        }
        return super.cannotRunAction();
    }

    @Override
    public void process() {
        List<JobBuild> matchingBuilds = draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl);
        log.info("");
        List<String> choices = new ArrayList<>();
        matchingBuilds.forEach(jobBuild -> choices.add(jobBuild.buildDisplayName));
        choices.add("None");
        List<Integer> selections = InputUtils.readSelections(choices, "Select jenkins builds to remove from commit", false);
        // check selection doesn't contain none value
        if (!selections.contains(choices.size() - 1)) {
            selections.forEach(selection -> draft.jobBuilds.remove(matchingBuilds.get(selection)));
        }
    }
}
