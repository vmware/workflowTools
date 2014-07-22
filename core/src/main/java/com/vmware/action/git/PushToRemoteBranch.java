package com.vmware.action.git;

import com.vmware.action.AbstractAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Performs a git push origin HEAD:[remote branch path] -f.")
public class PushToRemoteBranch extends AbstractAction {

    public PushToRemoteBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        String remoteBranchName = config.remoteBranchToUse;
        String remoteBranchPath = config.remoteBranches.get(remoteBranchName);
        if (StringUtils.isBlank(remoteBranchPath)) {
            log.info("{} did not match any predefined staging branch names {}.", remoteBranchName, config.remoteBranches.keySet().toString());
            log.info("Assuming that it is a valid staging branch path.");
            remoteBranchPath = remoteBranchName;
        }

        remoteBranchPath = remoteBranchPath.replace(":username", config.username);
        log.info("Updating remote branch " + remoteBranchPath);

        git.pushToRemoteBranch(remoteBranchPath);
    }
}
