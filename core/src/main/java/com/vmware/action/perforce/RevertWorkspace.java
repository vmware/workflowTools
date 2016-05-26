package com.vmware.action.perforce;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

import java.util.List;

@ActionDescription("Used to revert all pending changelists in a perforce workspace")
public class RevertWorkspace extends BaseAction {
    public RevertWorkspace(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        List<String> changelistIds = perforce.getPendingChangelists(config.perforceClientName);

        for (String changelistId : changelistIds) {
            revertAndDeleteChangelist(changelistId);
        }

        log.info("Reverting open files in default changelist for client {}", config.perforceClientName);
        perforce.revertChangesInPendingChangelist("default");
    }

    private void revertAndDeleteChangelist(String changelistId) {
        log.info("Reverting open files in changelist {} for client {}", changelistId, config.perforceClientName);
        perforce.revertChangesInPendingChangelist(changelistId);
        log.info("Deleting pending changelist {} in client {}", changelistId, config.perforceClientName);
        perforce.deletePendingChangelist(changelistId);
    }


}
