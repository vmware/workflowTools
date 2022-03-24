package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.action.base.BaseCommitCreateAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Performs a git commit --all, -all will automatically add tracked file changes to the commit.")
public class CommitAll extends BaseCommitCreateAction {

    public CommitAll(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void commitUsingGit(String description) {
        git.commitWithAllFileChanges(description, gitRepoConfig.noVerify);
    }
}
