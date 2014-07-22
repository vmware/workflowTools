package com.vmware.action.conditional;

import com.vmware.action.base.AbstractCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.utils.Padder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

@ActionDescription("Exits if git status does not detect any changes.")
public class ExitIfNoChangesDetectedByGit extends AbstractCommitAction {

    public ExitIfNoChangesDetectedByGit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException {
        List<String> changes = git.getAllChanges();
        if (changes.isEmpty()) {
            log.info("No changes detected by git!");
            System.exit(0);
        }

        Padder titlePadder = new Padder("Changes");
        titlePadder.infoTitle();
        for (String change : changes) {
            log.info(change);
        }
        titlePadder.infoTitle();
    }

}
