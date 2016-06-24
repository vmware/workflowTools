package com.vmware.action.conditional;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;

@ActionDescription("Exit if git-changeset is not installed.")
public class ExitIfGitChangesetNotInstalled extends BaseAction {
    public ExitIfGitChangesetNotInstalled(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (CommandLineUtils.isCommandAvailable("git-changeset")) {
            return;
        }

        log.info("");
        log.info("Exiting as git-changeset is not installed.");
        System.exit(0);
    }
}
