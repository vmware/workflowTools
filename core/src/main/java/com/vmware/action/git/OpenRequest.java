package com.vmware.action.git;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;

@ActionDescription("Opens the merge or pull request matching the commit.")
public class OpenRequest extends BaseCommitWithMergeRequestAction {

    public OpenRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        SystemUtils.openUrl(draft.requestUrl);
    }
}
