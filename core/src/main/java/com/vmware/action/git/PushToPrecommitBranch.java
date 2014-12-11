package com.vmware.action.git;

import com.vmware.action.AbstractAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Performs a git push origin HEAD:topic/[username config property]/pre-commit -f.")
public class PushToPrecommitBranch extends AbstractAction {

    public PushToPrecommitBranch(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        String remoteBranchName = "precommit";
        String remoteBranchPath = config.remoteBranches.get(remoteBranchName);
        if (StringUtils.isBlank(remoteBranchPath)) {
            remoteBranchPath = "topic/:username/pre-commit";
        }

        remoteBranchPath = remoteBranchPath.replace(":username", config.username);
        log.info("Updating remote branch " + remoteBranchPath);

        git.pushToRemoteBranch(remoteBranchPath, true);
    }
}
