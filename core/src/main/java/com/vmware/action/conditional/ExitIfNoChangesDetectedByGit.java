package com.vmware.action.conditional;

import java.util.List;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.logging.Padder;
import com.vmware.util.scm.FileChange;

@ActionDescription("Exits if git status does not detect any changes.")
public class ExitIfNoChangesDetectedByGit extends BaseCommitAction {

    public ExitIfNoChangesDetectedByGit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<FileChange> changes = git.getAllChanges();
        if (changes.isEmpty()) {
            exitWithMessage("no changes detected by git!");
        }

        Padder titlePadder = new Padder("Changes");
        titlePadder.infoTitle();
        for (FileChange change : changes) {
            log.info(change.toString());
        }
        titlePadder.infoTitle();
    }

}
