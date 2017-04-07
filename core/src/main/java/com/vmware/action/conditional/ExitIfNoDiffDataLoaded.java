package com.vmware.action.conditional;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Exit if no diff data has been loaded. Load via review or from a file.")
public class ExitIfNoDiffDataLoaded extends BaseCommitAction {

    public ExitIfNoDiffDataLoaded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (StringUtils.isBlank(draft.draftDiffData)) {
            log.info("");
            log.info("Exiting as no diff data has been loaded.");
            System.exit(0);
        }
    }
}
