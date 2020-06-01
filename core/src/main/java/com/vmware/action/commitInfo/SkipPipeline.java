package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Skips pipeline if skipPipeline is set to true")
public class SkipPipeline extends BaseCommitAction {
    public SkipPipeline(WorkflowConfig config) {
        super(config);
    }

    @Override
    protected void skipActionDueTo(String reason, Object... arguments) {
        super.skipActionDueTo(reason, arguments);
        skipActionIfTrue(!commitConfig.skipPipeline, "as skipPipeline is set to false");

    }

    @Override
    public void process() {
        log.info("Setting pipeline to {}", commitConfig.noPipelineLabel);
        draft.pipeline = commitConfig.noPipelineLabel;
    }
}
