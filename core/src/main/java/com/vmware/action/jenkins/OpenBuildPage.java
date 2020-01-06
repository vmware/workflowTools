package com.vmware.action.jenkins;

import java.util.ArrayList;
import java.util.List;

import com.vmware.JobBuild;
import com.vmware.action.base.BaseCommitWithJenkinsBuildsAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;
import com.vmware.util.input.InputUtils;

@ActionDescription("Opens the web page for a Jenkins Build")
public class OpenBuildPage extends BaseCommitWithJenkinsBuildsAction {

    public OpenBuildPage(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<JobBuild> matchingBuilds = draft.jobBuildsMatchingUrl(jenkinsConfig.jenkinsUrl);

        if (matchingBuilds.size() == 1) {
            log.info("Opening build {} as it is the only Jenkins build", matchingBuilds.get(0).buildDisplayName);
            String consoleUrl = matchingBuilds.get(0).url;
            SystemUtils.openUrl(consoleUrl);
        } else {
            List<String> choices = new ArrayList<>();
            matchingBuilds.forEach(jobBuild -> choices.add(jobBuild.buildDisplayName));
            int selection = InputUtils.readSelection(choices, "Select jenkins builds to open web page for");

            String consoleUrl = matchingBuilds.get(selection).url;
            SystemUtils.openUrl(consoleUrl);
        }
    }
}
