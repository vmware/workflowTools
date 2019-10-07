package com.vmware.action.jenkins;

import java.util.ArrayList;
import java.util.List;

import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.BrowserUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Opens the console page for a Jenkins Build")
public class OpenBuildConsolePage extends BaseCommitAction {

    public OpenBuildConsolePage(WorkflowConfig config) {
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

        List<String> choices = new ArrayList<>();
        matchingBuilds.forEach(jobBuild -> choices.add(jobBuild.buildDisplayName));
        int selection = InputUtils.readSelection(choices, "Select jenkins builds to remove from commit");

        String consoleUrl = matchingBuilds.get(selection).url + "/console";
        BrowserUtils.openUrl(consoleUrl);
    }
}
