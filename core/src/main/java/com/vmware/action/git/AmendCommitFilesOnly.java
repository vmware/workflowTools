package com.vmware.action.git;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Performs a git commit --amend --all without modifying any part of the commit message. Uses the existing commit message.")
public class AmendCommitFilesOnly extends AbstractCommitAction {

    public AmendCommitFilesOnly(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        git.amendCommitWithAllFileChanges(git.lastCommitText(true));
    }

}
