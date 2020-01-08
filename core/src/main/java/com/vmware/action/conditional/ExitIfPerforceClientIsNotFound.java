package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Exit if perforce client is not found.")
public class ExitIfPerforceClientIsNotFound extends BaseCommitAction {
    public ExitIfPerforceClientIsNotFound(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String reasonForFailing = perforceClientCannotBeUsed();
        if (StringUtils.isEmpty(reasonForFailing) && StringUtils.isEmpty(perforceClientConfig.perforceClientName)) {
            reasonForFailing = "perforceClientName config value is not set, can also be set by git-p4.client git config value.";
        }
        if (StringUtils.isNotEmpty(reasonForFailing)) {
            cancelWithMessage(reasonForFailing);
        }
    }
}
