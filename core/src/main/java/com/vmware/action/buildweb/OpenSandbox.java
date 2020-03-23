package com.vmware.action.buildweb;

import java.util.ArrayList;
import java.util.List;

import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitWithBuildwebBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Opens the page for a Buildweb sandbox build")
public class OpenSandbox extends BaseCommitWithBuildwebBuildsAction {

    public OpenSandbox(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<JobBuild> matchingBuilds = draft.jobBuildsMatchingUrl(buildwebConfig.buildwebUrl);

        if (matchingBuilds.size() == 1) {
            log.info("Opening build {} as it is the only Buildweb build", matchingBuilds.get(0).buildDisplayName);
            SystemUtils.openUrl(matchingBuilds.get(0).url);
        } else {
            List<String> choices = new ArrayList<>();
            matchingBuilds.forEach(jobBuild -> choices.add(jobBuild.buildDisplayName));
            int selection = InputUtils.readSelection(choices, "Select sandbox build to open");
            SystemUtils.openUrl(matchingBuilds.get(selection).url);
        }
    }
}
