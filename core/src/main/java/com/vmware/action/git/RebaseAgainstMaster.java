package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Rebases the current branch against the remote master.")
public class RebaseAgainstMaster extends BaseAction {
    public RebaseAgainstMaster(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String originMasterRef = git.revParse("origin/master");
        String p4MasterRef = git.revParse("p4/master");
        String rebaseOutput;
        if (!originMasterRef.contains("unknown revision or path")) {
            log.info("Rebasing against origin/master after fetching");
            git.fetch();
            rebaseOutput = git.rebase("origin/master");
        } else if (!p4MasterRef.contains("unknown revision or path")) {
            log.info("Rebasing using git p4");
            rebaseOutput = git.p4Rebase();
        } else {
            log.error("Neither origin/master or p4/master were found as remote branches, can't rebase");
            return;
        }

        if (rebaseOutput.contains("Cannot rebase")
                || rebaseOutput.contains("Some files in your working directory are modified and different")) {
            log.error("Failed to rebase\n{}", rebaseOutput);
        }
    }
}
