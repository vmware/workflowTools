package com.vmware.action.perforce;

import com.vmware.action.base.BasePerforceCommitAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.Padder;

@ActionDescription("Reads a pending changelist for the specified perforce client.")
public class ReadPendingChangelist extends BasePerforceCommitAction {

    public ReadPendingChangelist(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        String changelistId = perforce.selectPendingChangelist();
        String changelistText = perforce.readChangelist(changelistId);
        if (StringUtils.isBlank(changelistText) || !changelistText.contains("\n")) {
            throw new RuntimeException("No pending changelist exists for user " + config.username);
        }

        draft.fillValuesFromCommitText(changelistText, config.getCommitConfiguration());
        if (StringUtils.isBlank(draft.perforceChangelistId)) {
            throw new RuntimeException("Failed to parse changelist id from text\n" + changelistText);
        }

        Padder titlePadder = new Padder("Parsed Values");
        titlePadder.debugTitle();
        log.debug(draft.toText(config.getCommitConfiguration()));
        titlePadder.debugTitle();
    }
}
