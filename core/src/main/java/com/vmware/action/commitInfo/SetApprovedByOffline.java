package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseSetUsersList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.input.InputUtils;

@ActionDescription("Set the approved by users offline for a commit.")
public class SetApprovedByOffline extends BaseSetUsersList {
    public SetApprovedByOffline(WorkflowConfig config) {
        super(config, "approvedBy", true, false);
    }

    @Override
    public void process() {
        log.info("Enter users to approve commit");
        draft.approvedBy = InputUtils.readValueUntilNotBlank("Users");
        log.info("Approved by user list: {}", draft.approvedBy);
    }
}
