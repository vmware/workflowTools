package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Performs a git commit --amend if changes are detected.")
public class AmendCommit extends BaseCommitAction {

    public AmendCommit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        if (!draft.hasData()) {
            log.error("Not amending commit as there no information set for the commit message");
            return;
        }

        String existingCommitText = git.lastCommitText(true).trim();
        String updatedCommitText = draft.toGitText(config.getCommitConfiguration()).trim();

        if (git.getStagedChanges().isEmpty() && existingCommitText.equals(updatedCommitText)) {
            log.info("");
            log.info("Not amending commit as it does not have any changes");
            return;
        }

        git.amendCommit(updatedCommitText);
    }
}
