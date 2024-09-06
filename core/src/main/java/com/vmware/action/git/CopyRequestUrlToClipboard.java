package com.vmware.action.git;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.SystemUtils;

@ActionDescription("Copies merge or pull request url to clipboard.")
public class CopyRequestUrlToClipboard extends BaseCommitAction {
    public CopyRequestUrlToClipboard(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        log.info("Copying request url {} to clipboard", draft.requestUrl);
        SystemUtils.copyTextToClipboard(draft.requestUrl);
    }
}
