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
        String remoteMasterBranch = gitRepoConfig.defaultGitRemote + "/master";
        String remoteMasterRef = git.revParseWithoutException(remoteMasterBranch);
        String p4MasterRef = git.revParseWithoutException("p4/master");
        String rebaseOutput;
        if (!remoteMasterRef.contains("unknown revision or path")) {
            log.info("Rebasing against {} after fetching", remoteMasterBranch);
            git.fetch();
            rebaseOutput = git.rebase(remoteMasterBranch);
        } else if (!p4MasterRef.contains("unknown revision or path")) {
            log.info("Rebasing using git p4");
            rebaseOutput = git.p4Rebase();
        } else {
            log.error("Neither {} or p4/master were found as remote branches, can't rebase", remoteMasterBranch);
            return;
        }

        if (rebaseOutput.contains("Cannot rebase")
                || rebaseOutput.contains("Some files in your working directory are modified and different")) {
            log.error("Failed to rebase\n{}", rebaseOutput);
        }
    }
}
