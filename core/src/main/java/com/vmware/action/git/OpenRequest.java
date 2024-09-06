package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.SystemUtils;

@ActionDescription("Opens the merge or pull request matching the commit.")
public class OpenRequest extends BaseCommitAction {

    public OpenRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        failIfTrue(StringUtils.isEmpty(draft.requestUrl), "no request url found");
    }

    @Override
    public void process() {
        SystemUtils.openUrl(draft.requestUrl);
    }
}
