package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.util.SystemUtils;

@ActionDescription("Opens the merge request matching the commit.")
public class OpenMergeRequest extends BaseCommitWithMergeRequestAction {

    public OpenMergeRequest(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        SystemUtils.openUrl(draft.mergeRequestUrl());
    }
}
