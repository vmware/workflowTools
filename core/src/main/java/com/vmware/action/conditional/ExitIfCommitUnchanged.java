package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Exit if the commit details in memory are not different to the last commit.")
public class ExitIfCommitUnchanged extends BaseCommitAction {
    public ExitIfCommitUnchanged(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        String existingCommitText = git.lastCommitText(true).trim();
        String updatedCommitText = draft.toGitText(config.getCommitConfiguration()).trim();

        if (!existingCommitText.equals(updatedCommitText)) {
            return;
        }

        log.info("");
        log.info("Commit does not have any changes, exiting");
        System.exit(0);
    }
}
