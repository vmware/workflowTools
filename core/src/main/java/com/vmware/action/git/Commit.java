package com.vmware.action.git;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;

@ActionDescription("Performs a git commit.")
public class Commit extends AbstractCommitAction {

    public Commit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        if (!draft.hasData()) {
            log.error("Not committing as there no information set for the commit message");
            return;
        }

        git.commit(draft.toGitText(config.getCommitConfiguration()));
    }
}
