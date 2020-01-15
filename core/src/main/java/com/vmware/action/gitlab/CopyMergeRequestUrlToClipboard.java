package com.vmware.action.gitlab;

import com.vmware.action.base.BaseCommitWithMergeRequestAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;

@ActionDescription("Copies merge request url to clipboard.")
public class CopyMergeRequestUrlToClipboard extends BaseCommitWithMergeRequestAction {
    public CopyMergeRequestUrlToClipboard(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Copying merge request url {} to clipboard", draft.mergeRequestUrl());
        SystemUtils.copyTextToClipboard(draft.mergeRequestUrl());
    }
}
