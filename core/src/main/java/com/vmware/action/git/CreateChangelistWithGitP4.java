package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Runs git p4 --prepare-p4-only, then moves changes to the default changelist")
public class CreateChangelistWithGitP4 extends BaseCommitAction {
    public CreateChangelistWithGitP4(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Using git p4 to add matching changes to default changelist in perforce");
        git.addChangesToDefaultChangelist();
        log.info("Using p4 to create new pending changelist with commit text");
        boolean FILES_EXPECTED = true;
        String changelistId = perforce.createPendingChangelist(draft.toGitText(config.getCommitConfiguration()),
                FILES_EXPECTED);
        if (changelistId == null) {
            log.warn("Reverting default changelist as create failed");
            perforce.revertChangesInPendingChangelist("default");
        } else {
            log.info("Created pending changelist with id {}", changelistId);
            draft.perforceChangelistId = changelistId;
        }
    }
}
