package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.scm.FileChange;
import com.vmware.util.logging.Padder;

import java.util.List;

@ActionDescription("Exits if git status does not detect any staged changes.")
public class ExitIfNoStagedChangesDetectedByGit extends BaseCommitAction {

    public ExitIfNoStagedChangesDetectedByGit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<FileChange> changes = git.getStagedChanges();
        if (changes.isEmpty()) {
            cancelWithMessage("no staged changes detected by git!");
        }

        Padder titlePadder = new Padder("Staged Changes");
        titlePadder.infoTitle();
        for (FileChange change : changes) {
            log.info(change.toString());
        }
        titlePadder.infoTitle();
    }

}
