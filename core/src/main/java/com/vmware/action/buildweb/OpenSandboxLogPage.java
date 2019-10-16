package com.vmware.action.buildweb;

import java.util.ArrayList;
import java.util.List;

import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitWithBuildwebBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.BrowserUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Opens the log page for a Buildweb sandbox build")
public class OpenSandboxLogPage extends BaseCommitWithBuildwebBuildsAction {

    public OpenSandboxLogPage(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<JobBuild> matchingBuilds = draft.jobBuildsMatchingUrl(buildwebConfig.buildwebUrl);

        if (matchingBuilds.size() == 1) {
            log.info("Opening build {} as it is the only Buildweb build", matchingBuilds.get(0).buildDisplayName);
            BrowserUtils.openUrl(buildweb.getLogsUrl(matchingBuilds.get(0).id()));
        } else {
            List<String> choices = new ArrayList<>();
            matchingBuilds.forEach(jobBuild -> choices.add(jobBuild.buildDisplayName));
            int selection = InputUtils.readSelection(choices, "Select sandbox build to open");
            BrowserUtils.openUrl(buildweb.getLogsUrl(matchingBuilds.get(selection).id()));
        }



    }
}
