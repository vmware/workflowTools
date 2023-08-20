package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;

@ActionDescription("Sets Review URL to none if no id is specified")
public class SetEmptyReviewUrlIfNeeded extends BaseCommitAction {
    public SetEmptyReviewUrlIfNeeded(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        if (StringUtils.isEmpty(draft.id)) {
            log.info("Setting review url to {}", commitConfig.noReviewNumberLabel);
            draft.id = commitConfig.noReviewNumberLabel;
        }
    }
}
