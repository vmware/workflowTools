package com.vmware.action.git;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Performs a git push origin HEAD:[remote branch path] -f.")
public class PushToRemoteBranch extends BaseAction {

    public PushToRemoteBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String remoteBranchName = gitRepoConfig.remoteBranchToUse;
        String remoteBranchPath = gitRepoConfig.remoteBranches.get(remoteBranchName);
        if (StringUtils.isEmpty(remoteBranchPath)) {
            log.info("{} did not match any predefined remote branch names {}.", remoteBranchName, gitRepoConfig.remoteBranches.keySet().toString());
            log.info("Assuming that it is a valid remote branch path.");
            remoteBranchPath = remoteBranchName;
        }

        remoteBranchPath = remoteBranchPath.replace("$USERNAME", config.username);
        log.info("Updating remote branch " + remoteBranchPath);

        git.forcePushToRemoteBranch("origin", remoteBranchPath);
    }
}
