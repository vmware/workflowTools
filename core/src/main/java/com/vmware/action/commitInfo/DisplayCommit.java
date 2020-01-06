package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

@ActionDescription("Displays the commit details in memory.")
public class DisplayCommit extends BaseCommitAction {

    public DisplayCommit(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        Padder titlePadder = new Padder("Commit Details");
        titlePadder.infoTitle();
        log.info(draft.toText(commitConfig));
        if (StringUtils.isNotEmpty(draft.perforceChangelistId)) {
            log.info("Perforce Changelist Id: {}", draft.perforceChangelistId);
        }
        titlePadder.infoTitle();
    }
}
