package com.vmware.action.conditional;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

@ActionDescription("Exits if git status does not detect any staged changes.")
public class ExitIfNoStagedChangesDetectedByGit extends AbstractCommitAction {

    public ExitIfNoStagedChangesDetectedByGit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        List<String> changes = git.getStagedChanges();
        if (changes.isEmpty()) {
            log.info("No staged changes detected by git!");
            System.exit(0);
        }

        Padder titlePadder = new Padder("Staged Changes");
        titlePadder.infoTitle();
        for (String change : changes) {
            log.info(change);
        }
        titlePadder.infoTitle();
    }

}
